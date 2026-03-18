#!/bin/bash
cd "$(dirname "$0")"
OUT="BTW_API_Surface.md"

{
echo "# Better Than Wolves - Complete API Surface Catalog"
echo ""

echo "## 1. Vanilla Classes Extended"
echo '```'
grep -rh "extends " Common/src Client/src Server/src --include="*.java" | grep -oE "extends [A-Z][a-zA-Z]+" | sort -u | sed 's/extends //'
echo '```'
echo ""

echo "## 2. Vanilla Interfaces Implemented"
echo '```'
grep -rh "implements " Common/src Client/src Server/src --include="*.java" | grep -oE "implements [A-Z][a-zA-Z]+" | sort -u | sed 's/implements //'
echo '```'
echo ""

echo "## 3. FC Class Hierarchy (class extends parent)"
echo '```'
grep -rh "class FC\|class Aaa" Common/src Client/src Server/src --include="*.java" | grep -oE "class [A-Za-z]+ extends [A-Za-z]+" | sort -u
echo '```'
echo ""

echo "## 4. Vanilla Classes Patched"
echo "### Src/patch.txt (shared)"
echo '```'
grep "^--- " Src/patch.txt | grep -oE "[A-Z][a-zA-Z0-9_]+\.java" | sort -u
echo '```'
echo "### SrcClient/patch.txt"
echo '```'
grep "^--- " SrcClient/patch.txt | grep -oE "[A-Z][a-zA-Z0-9_]+\.java" | sort -u
echo '```'
echo "### SrcServer/patch.txt"
echo '```'
grep "^--- " SrcServer/patch.txt | grep -oE "[A-Z][a-zA-Z0-9_]+\.java" | sort -u
echo '```'
echo ""

echo "## 5. @Override Method Signatures"
echo '```'
grep -rh -A1 "@Override" Common/src Client/src Server/src --include="*.java" | grep -v "@Override" | grep -v "^--$" | sed 's/^[[:space:]]*//' | sed 's/{.*//' | grep -v "^$" | sort -u
echo '```'
echo ""

echo "## 6. BTW API Interfaces"
for f in $(find Common/src -name "FCI*.java" -path "*/btw/api/*" 2>/dev/null | sort); do
  echo "### $(basename $f .java)"
  echo '```java'
  cat "$f"
  echo '```'
  echo ""
done

echo "## 7. FCAddOn.java"
echo '```java'
find Common/src -name "FCAddOn.java" -exec cat {} \;
echo '```'
echo ""

echo "## 8. FCAddOnHandler.java"
echo '```java'
find Common/src -name "FCAddOnHandler.java" -exec cat {} \;
echo '```'
echo ""

echo "## 9. External Imports"
echo '```'
grep -rh "^import " Common/src Client/src Server/src --include="*.java" | grep -v "net\.minecraft" | sort -u
echo '```'
echo ""

echo "## 10. Vanilla Static Member Access"
echo '```'
grep -rohE "(Block|Item|World|Entity|EntityList|Material|CreativeTabs|Potion|Enchantment|EnumAction|DamageSource|CraftingManager|FurnaceRecipes|EnumCreatureType|EnumToolMaterial|EnumArmorMaterial)\.[a-zA-Z_][a-zA-Z0-9_]*" Common/src Client/src Server/src --include="*.java" | sort -u
echo '```'
echo ""

echo "## 11. Access Modifiers Widened in Vanilla"
echo '```'
echo "Block.blockHardness (protected -> public)"
echo "Block.blockResistance (protected -> public)"
echo "Block.blockIcon (protected -> public)"
echo "Block.setHardness/setResistance/setStepSound/setLightValue (protected -> public)"
echo "Block.dropBlockAsItem_do, CanFallIntoBlockAtPos, OnFallingUpdate, ScheduleCheckForFall, RenderCrossHatch (protected -> public)"
echo "Item(int) constructor (protected -> public)"
echo "Item.setPotionEffect, itemIcon (protected -> public)"
echo "Entity.rand, inWater, dealFireDamage, setBeenAttacked, setSize (protected -> public)"
echo "EntityLiving.health, isLivingDead, moveSpeed, jump() (protected -> public)"
echo "EntityCreature.entityToAttack (protected -> public)"
echo "EntityPlayer.foodStats (protected -> public)"
echo "EntityAnimal.m_iGrazeProgressCounter (protected -> public)"
echo "World.worldInfo (protected -> public)"
echo "TileEntity.worldObj (protected -> public)"
echo "Material.setRequiresTool/setBurning/setNoPushMobility/setTranslucent (protected -> public)"
echo "AxisAlignedBB constructor, Vec3.setComponents, DamageSource.setDamageBypassesArmor (protected -> public)"
echo "Potion constructor, Potion.setIconIndex (protected -> public)"
echo "BlockLeavesBase.graphicsLevel, BiomeDecorator.reedGen, StructureStart.boundingBox, EntityFX.particleScale (protected -> public)"
echo "RenderBlocks.RenderStandardFullBlockMovedByPiston (protected -> public)"
echo "BlockLeaves.adjacentTreeBlocks, WorldGenBigTree.basePos/heightLimit/worldObj/otherCoordPairs (package-private -> public)"
echo "WorldGenBigTree methods: genTreeLayer/placeBlockLine/checkBlockLine/generateLeafNodeBases/validTreeLocation (pkg -> public)"
echo "EntityAIAttackOnCollide.attacker, EntityAICreeperSwell.creeperAttackTarget, EntityVillager.villageObj (pkg -> public)"
echo "ModelSquid.squidBody/squidTentacles (package-private -> public)"
echo "CraftingManager.addRecipe/AddShapelessRecipe/AddShapedRecipeWithCustomClass (package-private -> public)"
echo "Packet.addIdClassMapping, ChunkProviderServer.GetCurrentProvider (package-private -> public)"
echo "DispenserBehaviorFilledBucket/Potion/EmptyBucket classes (package-private -> public)"
echo '```'
echo ""

BTWFILE=$(find Client/src Common/src -name "FCBetterThanWolves.java" 2>/dev/null | head -1)
echo "## 12. Registered Block Fields"
echo '```'
grep -oE "public static (Block |FCBlock[A-Za-z]* )fc[A-Za-z]+" "$BTWFILE" | sort -u
echo '```'
echo ""

echo "## 13. Registered Item Fields"
echo '```'
grep -oE "public static (Item |FCItem[A-Za-z]* )fc[A-Za-z]+" "$BTWFILE" | sort -u
echo '```'
echo ""

echo "## 14. Custom Materials"
echo '```'
grep "public static.*Material " "$BTWFILE" | sed 's/^[[:space:]]*//' | sort -u
echo '```'
echo ""

echo "## 15. Custom Damage Sources"
echo '```'
grep -rh "class FCDamage" Common/src Client/src Server/src --include="*.java" | sort -u
echo '```'
echo ""

echo "## 16. Custom Enchantments"
echo '```'
grep -rh "class FCEnchant" Common/src Client/src Server/src --include="*.java" | sort -u
echo '```'
echo ""

echo "## 17. Packet Classes"
echo '```'
grep -rh "class FCPacket" Common/src Client/src Server/src --include="*.java" | sort -u
echo '```'
echo ""

echo "## 18. Statistics"
echo '```'
echo "FC source files: $(find Common/src Client/src Server/src -name 'FC*.java' -o -name 'Aaa*.java' | wc -l)"
echo "Common: $(find Common/src -name 'FC*.java' -o -name 'Aaa*.java' | wc -l)"
echo "Client: $(find Client/src -name 'FC*.java' -o -name 'Aaa*.java' | wc -l)"
echo "Server: $(find Server/src -name 'FC*.java' -o -name 'Aaa*.java' | wc -l)"
echo "Vanilla patched: $(grep -h '^--- ' Src/patch.txt SrcClient/patch.txt SrcServer/patch.txt | grep -oE '[A-Z][a-zA-Z0-9_]+\.java' | sort -u | wc -l)"
echo '```'
} > "$OUT"

echo "Generated $OUT ($(wc -l < "$OUT") lines)"
