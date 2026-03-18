#!/usr/bin/env bash
#
# apply_migration.sh — Applies all migration transformations from MIGRATION_GUIDE.md
# Run from project root: bash apply_migration.sh
#
set -euo pipefail
cd "$(dirname "$0")"

FC_DIRS="Common/src Client/src Server/src"

echo "============================================="
echo " BTW Migration Script"
echo "============================================="
echo ""

# =============================================
# STEP 1: extends VanillaClass → extends AbstractVanillaClass
# =============================================
echo "STEP 1: Changing extends to Abstract* classes..."

# Build vanilla class set and FC class set
VANILLA_CLASSES=$(find Adapter/src -name "Abstract*.java" -exec basename {} .java \; | sed 's/Abstract//')
FC_CLASSES=$(grep -roh "class FC[A-Za-z]*\|class Aaa[A-Za-z]*" $FC_DIRS --include="*.java" 2>/dev/null | sed 's/class //' | sort -u)

step1_count=0
# Process longest class names first to avoid partial matches
for cls in $(echo "$VANILLA_CLASSES" | awk '{print length, $0}' | sort -rn | awk '{print $2}'); do
  # Find FC files that extend this vanilla class
  find $FC_DIRS -name "*.java" | while read f; do
    if grep -q "extends ${cls}$\|extends ${cls} \|extends ${cls}{" "$f" 2>/dev/null; then
      # Verify the parent is the vanilla class, not an FC class with same name
      # (FC classes are in fc_classes list — skip if parent is FC)
      if echo "$FC_CLASSES" | grep -qx "$cls" 2>/dev/null; then
        continue
      fi
      sed -i "s/extends ${cls}\b/extends Abstract${cls}/g" "$f"
    fi
  done
  changed=$(grep -rl "Abstract${cls}" $FC_DIRS --include="*.java" 2>/dev/null | wc -l)
  if [ "$changed" -gt 0 ]; then
    echo "  extends $cls → extends Abstract${cls} ($changed files)"
    step1_count=$((step1_count + changed))
  fi
done
echo "  Total: $step1_count files changed"
echo ""

# =============================================
# STEP 1b: Add adapter imports to changed files
# =============================================
echo "STEP 1b: Adding adapter imports..."
import_count=0
find $FC_DIRS -name "*.java" | while read f; do
  if grep -q "Abstract" "$f" && ! grep -q "btw.adapter" "$f"; then
    sed -i '/^package /a\
import btw.adapter.block.*;\
import btw.adapter.entity.*;\
import btw.adapter.item.*;\
import btw.adapter.tileentity.*;\
import btw.adapter.crafting.*;\
import btw.adapter.misc.*;' "$f"
  fi
done
echo "  Done"
echo ""

# =============================================
# STEP 2: Rename @Override methods to _btw suffix
# =============================================
echo "STEP 2: Renaming bridged @Override methods to _btw..."

# Collect all bridge method names from Abstract classes
BRIDGE_METHODS=$(find Adapter/src -name "Abstract*.java" -exec grep -oh "\w\+_btw" {} \; 2>/dev/null | sed 's/_btw//' | sort -u)

step2_count=0
for method in $BRIDGE_METHODS; do
  # Find files that have @Override followed by this method with vanilla-typed params
  # Only rename if the method has World/Entity/EntityPlayer/ItemStack/Block params
  find $FC_DIRS -name "*.java" | while read f; do
    if grep -q "@Override" "$f" && grep -q "public.*${method}(" "$f"; then
      # Check if the method has vanilla-typed params (World, Entity, EntityPlayer, etc.)
      if grep "public.*${method}(" "$f" | grep -qE "\b(World|Entity|EntityPlayer|EntityLiving|EntityAnimal|EntityFallingSand|ItemStack|Block)\b"; then
        # Rename method to _btw
        sed -i "s/\b${method}(/&/; s/${method}(/${method}_btw(/" "$f"
        # But that doubled it. Use a cleaner approach:
        # Only replace in method declarations (lines with public/protected and the method name followed by paren)
        # Revert the bad sed and do it right
        sed -i "s/${method}_btw_btw/${method}_btw/g" "$f"
      fi
    fi
  done
done

# Cleaner approach: process each file, find @Override + method declaration pattern
find $FC_DIRS -name "*.java" | while read f; do
  changed=false
  for method in $BRIDGE_METHODS; do
    # Match: public [type] methodName(... World|Entity|EntityPlayer|ItemStack|Block ...)
    if grep -q "public.*\b${method}\b.*(" "$f" 2>/dev/null; then
      if grep "public.*\b${method}\b.*(" "$f" | grep -qE "\b(World|Entity|EntityPlayer|EntityLiving|EntityAnimal|EntityFallingSand|ItemStack)\b" 2>/dev/null; then
        # Only rename if not already _btw
        if ! grep -q "${method}_btw" "$f" 2>/dev/null; then
          sed -i "s/\bpublic \(.*\)\b${method}\b(/public \1${method}_btw(/g" "$f"
          changed=true
        fi
      fi
    fi
  done
  # Also rename super.method() calls for bridged methods
  for method in $BRIDGE_METHODS; do
    if grep -q "super\.${method}(" "$f" 2>/dev/null; then
      if ! grep -q "super\.${method}_btw(" "$f" 2>/dev/null; then
        sed -i "s/super\.${method}(/super.${method}_btw(/g" "$f"
        changed=true
      fi
    fi
  done
  if [ "$changed" = true ]; then
    step2_count=$((step2_count + 1))
  fi
done
echo "  Renamed methods in ~$step2_count files"
echo ""

# =============================================
# STEP 3: Change parameter types to interfaces
# =============================================
echo "STEP 3: Changing parameter types to interface types..."

# Type replacements (in _btw method signatures only)
# Process longest names first
declare -A TYPE_MAP
TYPE_MAP=(
  ["EntityFallingSand"]="IEntityFallingSand"
  ["EntityPlayer"]="IEntityPlayer"
  ["EntityLiving"]="IEntityLiving"
  ["EntityAnimal"]="IEntityAnimal"
  ["ItemStack"]="IItemStack"
  ["Entity"]="IEntity"
  ["World"]="IWorld"
  ["Block"]="IBlock"
)

# Only replace in lines that contain _btw (method declarations)
step3_count=0
for vanilla in EntityFallingSand EntityPlayer EntityLiving EntityAnimal ItemStack Entity World Block; do
  iface="${TYPE_MAP[$vanilla]}"
  find $FC_DIRS -name "*.java" -exec grep -l "_btw" {} \; 2>/dev/null | while read f; do
    # Only replace on lines containing _btw (the method signature)
    if grep "_btw.*\b${vanilla}\b" "$f" >/dev/null 2>&1; then
      sed -i "/_btw/s/\b${vanilla}\b/${iface}/g" "$f"
    fi
  done
done
echo "  Done"
echo ""

# =============================================
# STEP 4: Convert field access to getters
# =============================================
echo "STEP 4: Converting field access to getters..."

# Only convert on non-this, non-super references
# Pattern: variable.field → variable.getField()
# Skip: this.field, super.field (FC inherits these from Abstract*)

step4_count=0
find $FC_DIRS -name "*.java" | while read f; do
  changed=false

  # entity/player/living field access (but not this. or super.)
  for pair in "posX:getPosX()" "posY:getPosY()" "posZ:getPosZ()" \
              "motionX:getMotionX()" "motionY:getMotionY()" "motionZ:getMotionZ()"; do
    field="${pair%%:*}"
    getter="${pair#*:}"
    # Match: word.field but NOT this.field or super.field
    if grep -q "\.${field}\b" "$f" 2>/dev/null; then
      # Negative lookbehind isn't available in sed, so use a two-pass approach
      # Replace all .field with .getter, then revert this.getter and super.getter
      sed -i "s/\.${field}\b/.${getter}/g" "$f"
      sed -i "s/this\.${getter}/this.${field}/g" "$f"
      sed -i "s/super\.${getter}/super.${field}/g" "$f"
      changed=true
    fi
  done

  # world.rand → world.getRandom()
  if grep -q '\.rand\b' "$f" 2>/dev/null; then
    sed -i 's/\.rand\b/.getRandom()/g' "$f"
    sed -i 's/this\.getRandom()/this.rand/g' "$f"
    sed -i 's/super\.getRandom()/super.rand/g' "$f"
    # Fix java.util.Random import (getRandom() is not a field of Random)
    sed -i 's/Random\.getRandom()/Random.rand/g' "$f"
    changed=true
  fi

  # world.isRemote → world.getIsRemote()
  if grep -q '\.isRemote\b' "$f" 2>/dev/null; then
    sed -i 's/\.isRemote\b/.getIsRemote()/g' "$f"
    sed -i 's/this\.getIsRemote()/this.isRemote/g' "$f"
    changed=true
  fi

  # block.blockID → block.getBlockID()
  if grep -q '\.blockID\b' "$f" 2>/dev/null; then
    sed -i 's/\.blockID\b/.getBlockID()/g' "$f"
    sed -i 's/this\.getBlockID()/this.blockID/g' "$f"
    changed=true
  fi

  # stack.stackSize → stack.getStackSize()
  if grep -q '\.stackSize\b' "$f" 2>/dev/null; then
    sed -i 's/\.stackSize\b/.getStackSize()/g' "$f"
    sed -i 's/this\.getStackSize()/this.stackSize/g' "$f"
    changed=true
  fi

  # stack.itemID → stack.getItemID()
  if grep -q '\.itemID\b' "$f" 2>/dev/null; then
    sed -i 's/\.itemID\b/.getItemID()/g' "$f"
    sed -i 's/this\.getItemID()/this.itemID/g' "$f"
    changed=true
  fi

  # tile.worldObj → tile.getWorldObj()
  if grep -q '\.worldObj\b' "$f" 2>/dev/null; then
    sed -i 's/\.worldObj\b/.getWorldObj()/g' "$f"
    sed -i 's/this\.getWorldObj()/this.worldObj/g' "$f"
    changed=true
  fi

  # tile.xCoord/yCoord/zCoord
  for pair in "xCoord:getXCoord()" "yCoord:getYCoord()" "zCoord:getZCoord()"; do
    field="${pair%%:*}"
    getter="${pair#*:}"
    if grep -q "\.${field}\b" "$f" 2>/dev/null; then
      sed -i "s/\.${field}\b/.${getter}/g" "$f"
      sed -i "s/this\.${getter}/this.${field}/g" "$f"
      changed=true
    fi
  done

  if [ "$changed" = true ]; then
    step4_count=$((step4_count + 1))
  fi
done
echo "  Converted field access in ~$step4_count files"
echo ""

# =============================================
# STEP 5: Convert static access to registries
# =============================================
echo "STEP 5: Converting static access to registries..."

find $FC_DIRS -name "*.java" | while read f; do
  # Block.blocksList[expr] → BlockRegistry.get(expr)
  sed -i 's/Block\.blocksList\[\([^]]*\)\]/BlockRegistry.get(\1)/g' "$f"

  # Item.itemsList[expr] → ItemRegistry.get(expr)
  sed -i 's/Item\.itemsList\[\([^]]*\)\]/ItemRegistry.get(\1)/g' "$f"
done

# Block.stone → Blocks.stone (for all block constants)
BLOCK_CONSTANTS=$(grep -oE "public static final (Block|BlockFluid|BlockGrass|BlockLeaves) \w+" vanilla/server/net/minecraft/src/Block.java 2>/dev/null | awk '{print $NF}' | grep -v "blocksList\|opaque\|lightO\|lightV\|canBlock\|useNeigh\|sound" | sort -u)

for const in $BLOCK_CONSTANTS; do
  find $FC_DIRS -name "*.java" -exec sed -i "s/Block\.${const}\b/Blocks.${const}/g" {} +
done

# Add registry import
find $FC_DIRS -name "*.java" | while read f; do
  if grep -q "BlockRegistry\|ItemRegistry\|Blocks\." "$f" 2>/dev/null; then
    if ! grep -q "btw.api.registry" "$f" 2>/dev/null; then
      sed -i '/^package /a\import btw.api.registry.*;' "$f"
    fi
  fi
done

echo "  Done"
echo ""

# =============================================
# STEP 6: Add API imports
# =============================================
echo "STEP 6: Adding API imports..."
find $FC_DIRS -name "*.java" | while read f; do
  if ! grep -q "btw.api.block.IBlock" "$f" 2>/dev/null; then
    sed -i '/^package /a\
import btw.api.block.*;\
import btw.api.item.*;\
import btw.api.entity.*;\
import btw.api.world.*;\
import btw.api.material.*;\
import btw.api.tileentity.*;\
import btw.api.util.*;\
import btw.api.registry.*;' "$f"
  fi
done
echo "  Done"
echo ""

# =============================================
# STEP 7: instanceof changes
# =============================================
echo "STEP 7: Changing instanceof to interface types..."
find $FC_DIRS -name "*.java" -exec sed -i \
  -e 's/instanceof World\b/instanceof IWorld/g' \
  -e 's/instanceof Block\b/instanceof IBlock/g' \
  -e 's/instanceof Entity\b/instanceof IEntity/g' \
  -e 's/instanceof EntityLiving\b/instanceof IEntityLiving/g' \
  -e 's/instanceof EntityPlayer\b/instanceof IEntityPlayer/g' \
  -e 's/instanceof Material\b/instanceof IMaterial/g' \
  -e 's/instanceof ItemStack\b/instanceof IItemStack/g' \
  {} +
echo "  Done"
echo ""

echo "============================================="
echo " Migration complete!"
echo " Run: ./gradlew buildAll"
echo " Fix any remaining errors manually in IntelliJ"
echo "============================================="
