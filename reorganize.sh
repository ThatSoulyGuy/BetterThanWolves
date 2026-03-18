#!/usr/bin/env bash
set -euo pipefail
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

# === Subpackage mapping ===
get_subpackage() {
    case "$1" in
        FCBlock*|AaaFCBlock*)         echo "block" ;;
        FCItem*|AaaFCItem*)           echo "item" ;;
        FCEntity*|FCSpawn*)           echo "entity" ;;
        FCTileEntity*)                echo "tileentity" ;;
        FCClient*)                    echo "client" ;;
        FCCraftingManager*|FCRecipes*|FCContainer*|FCInventory*) echo "crafting" ;;
        FCI*)                         echo "api" ;;
        FCUtils*|FCClosest*|FCMagnetic*|FCMap*|FCPacket*|FCChunk*) echo "util" ;;
        FCBiome*|FCWorld*|FCExplosion*|FCBeacon*) echo "world" ;;
        FCBehavior*)                  echo "behavior" ;;
        FCMaterial*|FCEnum*|FCDamage*|FCStep*) echo "properties" ;;
        FCModel*)                     echo "model" ;;
        FCCommand*)                   echo "command" ;;
        FCAdd*|FCBetter*)             echo "core" ;;
        FC*)                          echo "core" ;;
    esac
}

SUBPACKAGES="core block item entity tileentity client crafting api util world behavior properties model command"

# === Step 1: Move FC files to subpackage directories ===
process_module() {
    local mod_dir="$1"
    local src_dir="$mod_dir/src/main/java/net/minecraft/src"
    [ ! -d "$src_dir" ] && return

    local count=0
    for f in "$src_dir"/FC*.java "$src_dir"/Aaa*.java; do
        [ ! -f "$f" ] && continue
        local base=$(basename "$f" .java)
        local subpkg=$(get_subpackage "$base")
        local target_dir="$mod_dir/src/main/java/net/minecraft/src/btw/$subpkg"
        mkdir -p "$target_dir"

        # Replace package line + add imports
        local new_pkg="package net.minecraft.src.btw.${subpkg};"
        local imports="import net.minecraft.src.*;"
        for pkg in $SUBPACKAGES; do
            imports="$imports
import net.minecraft.src.btw.${pkg}.*;"
        done

        # Write new file: new package, imports, then original content minus old package line
        {
            echo "$new_pkg"
            echo ""
            echo "$imports"
            echo ""
            sed '/^package net\.minecraft\.src/d' "$f"
        } > "$target_dir/$(basename "$f")"

        rm "$f"
        count=$((count + 1))
    done
    echo "  $mod_dir: moved $count files into subpackages"
}

for mod in Common Client Server; do
    process_module "$PROJECT_DIR/$mod"
done

# === Step 2: Remove btw.client import from Common and Server (no client classes there) ===
echo "Removing btw.client imports from Common/Server..."
find "$PROJECT_DIR/Common/src" "$PROJECT_DIR/Server/src" -name "*.java" -exec sed -i '/btw\.client/d' {} +

# === Step 3: Add btw imports to vanilla source files ===
echo "Adding btw imports to vanilla source files..."
add_imports() {
    local dir="$1"
    local skip_client="$2"
    find "$dir" -name "*.java" | while read f; do
        grep -q "btw.core" "$f" && continue  # already done
        local imports="import net.minecraft.src.btw.core.*;\nimport net.minecraft.src.btw.block.*;\nimport net.minecraft.src.btw.item.*;\nimport net.minecraft.src.btw.entity.*;\nimport net.minecraft.src.btw.tileentity.*;\nimport net.minecraft.src.btw.crafting.*;\nimport net.minecraft.src.btw.api.*;\nimport net.minecraft.src.btw.util.*;\nimport net.minecraft.src.btw.world.*;\nimport net.minecraft.src.btw.behavior.*;\nimport net.minecraft.src.btw.properties.*;\nimport net.minecraft.src.btw.model.*;\nimport net.minecraft.src.btw.command.*;"
        if [ "$skip_client" = "false" ]; then
            imports="$imports\nimport net.minecraft.src.btw.client.*;"
        fi
        sed -i "/^package /a\\$imports" "$f"
    done
}

add_imports "$PROJECT_DIR/vanilla/client" "false"
add_imports "$PROJECT_DIR/vanilla/server" "true"

# === Step 4: Widen access modifiers in FC files (protected/package-private → public) ===
# FC classes extensively use cross-class protected access that breaks across packages.
echo "Widening FC access modifiers for cross-package compatibility..."
find "$PROJECT_DIR/Common/src" "$PROJECT_DIR/Client/src" "$PROJECT_DIR/Server/src" \
    -name "FC*.java" -o -name "Aaa*.java" | while read f; do
    # Change "protected " to "public " for fields, methods, constructors (indented lines only)
    sed -i 's/^\(    \)protected /\1public /g' "$f"
done

echo "Done!"
