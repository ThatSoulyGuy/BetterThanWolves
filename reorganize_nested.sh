#!/usr/bin/env bash
set -euo pipefail
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Nested subpackage mapping: block.X, item.X, entity.X
get_nested_subpackage() {
    local name="$1"
    local parent="$2"  # current subpackage (block, item, entity, etc.)

    case "$parent" in
    block)
        case "$name" in
            *Slab*|*Siding*|*Moulding*|*Stairs*|*Corner*|*Decorative*|*Column*|*Table*|*Pedestal*|*Slat*|*Grate*|*Bone*Slab*) echo "block/decoration" ;;
            *Ore*|*ChunkOre*|*Gravel*|*Sand*Stone*|*Stone*|*Cobble*|*Bedrock*|*Clay*|*Brick*|*Mortar*|*Loose*|*Dirt*|*Dung*|*Cement*|*Soulforged*Steel*|*BlackStone*|*WhiteStone*|*Cooked*|*Block*Bone*) echo "block/terrain" ;;
            *Furnace*|*Anvil*|*Crucible*|*Kiln*|*Vessel*|*Cauldron*|*Hopper*|*BBQ*|*Campfire*|*BrewingStand*|*Dispenser*|*Workbench*|*Infernal*|*Basket*|*Hamper*|*Barrel*|*Chest*|*Bucket*|*Arcane*) echo "block/container" ;;
            *Axle*|*Gear*|*Pulley*|*Bellows*|*Saw*|*Turntable*|*Mill*|*Crank*|*Platform*|*Anchor*|*Rope*|*ScrewPump*|*Shaft*|*Power*) echo "block/mechanical" ;;
            *Redstone*|*Detector*|*Buddy*|*Button*|*Pressure*|*Lever*|*Clutch*|*Comparator*|*Repeater*|*Light*Bulb*) echo "block/redstone" ;;
            *Crop*|*Wheat*|*Hemp*|*Planter*|*Sapling*|*Mushroom*|*Pumpkin*|*Melon*|*Cactus*|*Vine*|*Plant*|*Weed*|*BloodWood*|*BloodLeaves*|*BloodMoss*|*NetherGrowth*|*Log*|*Leaves*|*Flower*|*Reed*|*TallGrass*|*Cocoa*|*Carrot*|*Potato*|*Blight*|*Vegetation*) echo "block/nature" ;;
            *Torch*|*Fire*|*Candle*|*Magma*|*Ember*|*Hibachi*|*Smouldering*|*Ash*) echo "block/fire" ;;
            *Water*|*Lava*|*Flowing*|*Stationary*|*Ice*|*Snow*|*Sponge*|*Fluid*) echo "block/fluid" ;;
            *) echo "block/misc" ;;
        esac ;;
    item)
        case "$name" in
            *ItemBlock*|*ItemSlab*|*ItemSiding*|*PlacesAsBlock*) echo "item/itemblock" ;;
            *Food*|*Stew*|*Soup*|*Dough*|*Flour*|*Bread*|*Cookie*|*Meat*|*Pork*|*Chicken*|*Beef*|*Fish*|*Egg*|*Mushroom*Omelet*|*Seeds*|*Cocoa*|*Carrot*|*Potato*|*Apple*|*Sugar*|*Chocolate*|*Kibble*|*Chowder*|*Creeper*Oyster*|*RawMutton*|*CookedMutton*|*RawWolf*|*CookedWolf*) echo "item/food" ;;
            *Tool*|*Sword*|*Axe*|*Pickaxe*|*Shovel*|*Hoe*|*Shears*|*Chisel*|*Mattock*) echo "item/tool" ;;
            *Armor*|*Boots*|*Helmet*|*Chest*plate*|*Legging*) echo "item/armor" ;;
            *Bow*|*Arrow*|*Dynamite*) echo "item/weapon" ;;
            *) echo "item/misc" ;;
        esac ;;
    entity)
        case "$name" in
            *EntityAI*) echo "entity/ai" ;;
            *Spawn*) echo "entity/spawn" ;;
            *) echo "entity/mob" ;;
        esac ;;
    crafting)
        case "$name" in
            *Container*|*Inventory*) echo "crafting/container" ;;
            *) echo "crafting/recipe" ;;
        esac ;;
    *)
        echo "$parent" ;;  # no nesting for other packages
    esac
}

ALL_SUBPACKAGES="core block/decoration block/terrain block/container block/mechanical block/redstone block/nature block/fire block/fluid block/misc item/itemblock item/food item/tool item/armor item/weapon item/misc entity/ai entity/mob entity/spawn crafting/recipe crafting/container tileentity client api util world behavior properties model command"

# Build import block with all nested packages
build_imports() {
    echo "import net.minecraft.src.*;"
    for pkg in $ALL_SUBPACKAGES; do
        local dotpkg=$(echo "$pkg" | tr '/' '.')
        echo "import net.minecraft.src.btw.${dotpkg}.*;"
    done
}

IMPORT_BLOCK=$(build_imports)

process_file() {
    local f="$1"
    local base=$(basename "$f" .java)

    # Determine current parent subpackage from path
    local parent=$(echo "$f" | grep -oP 'btw/\K[^/]+')
    local nested=$(get_nested_subpackage "$base" "$parent")
    local dotpkg=$(echo "$nested" | tr '/' '.')
    local target_dir=$(dirname "$f" | sed "s|btw/$parent|btw/$nested|")

    # Skip if already in nested package
    [[ "$f" == *"$nested"* ]] && return 0

    mkdir -p "$target_dir"

    {
        echo "package net.minecraft.src.btw.${dotpkg};"
        echo ""
        echo "$IMPORT_BLOCK"
        echo ""
        # Remove old package line, keep rest
        sed '/^package net\.minecraft\.src\.btw\./d' "$f"
    } > "$target_dir/$(basename "$f")"

    rm "$f"
    return 0
}

# Process all FC files in btw subdirs
echo "Moving files into nested subpackages..."
count=0
for mod in Common Client Server; do
    for f in $(find "$PROJECT_DIR/$mod/src/main/java/net/minecraft/src/btw" -name "FC*.java" -o -name "Aaa*.java" 2>/dev/null); do
        process_file "$f" && count=$((count+1))
    done
done
echo "Moved $count files"

# Remove btw.client imports from Common/Server
echo "Cleaning client imports from Common/Server..."
find "$PROJECT_DIR/Common/src" "$PROJECT_DIR/Server/src" -name "*.java" -exec sed -i '/btw\.client/d' {} +

# Update vanilla imports with all nested packages
echo "Updating vanilla imports..."
for f in $(find "$PROJECT_DIR/vanilla" -name "*.java"); do
    # Remove old btw imports
    sed -i '/^import net\.minecraft\.src\.btw\./d' "$f"
    # Add new ones after package line
    local skip_client="true"
    [[ "$f" == *"/client/"* ]] && skip_client="false"

    local imports=""
    for pkg in $ALL_SUBPACKAGES; do
        [[ "$skip_client" == "true" && "$pkg" == "client" ]] && continue
        local dotpkg=$(echo "$pkg" | tr '/' '.')
        imports="${imports}import net.minecraft.src.btw.${dotpkg}.*;\n"
    done
    sed -i "/^package /a\\$imports" "$f"
done

# Fix Minecraft.java FQN
sed -i 's/net\.minecraft\.src\.FCAddOnHandler/net.minecraft.src.btw.core.FCAddOnHandler/' "$PROJECT_DIR/vanilla/client/net/minecraft/client/Minecraft.java" 2>/dev/null

echo "Done!"
