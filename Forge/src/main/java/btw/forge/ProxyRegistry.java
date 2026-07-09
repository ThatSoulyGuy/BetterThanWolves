package btw.forge;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Simple registry mapping legacy int block/item IDs to their Forge proxy
 * counterparts.  Populated during {@link BTWRegistration#registerAllBTWContent()}
 * after FC initialization fills {@code btw.modern.Block.blocksList[]}.
 *
 * Also maintains a reverse mapping from vanilla 1.20.1 Block instances to
 * legacy MC 1.5.2 numeric IDs, so that mixin code can look up the FC block
 * for any vanilla block encountered in the world.
 */
public class ProxyRegistry {

    private static final Logger LOGGER = LogManager.getLogger("BTW-ProxyRegistry");

    private static final Map<Integer, ProxyBlock> blocksByLegacyId = new HashMap<>();
    private static final Map<Integer, net.minecraft.world.item.Item> itemsByLegacyId = new HashMap<>();

    /**
     * Maps modern vanilla Block instances to their legacy MC 1.5.2 numeric IDs.
     * Populated lazily on first access via {@link #initVanillaBlockMap()}.
     */
    private static final Map<Block, Integer> vanillaBlockToLegacyId = new IdentityHashMap<>();
    /** Reverse mapping: legacy block ID → modern MC Block. Built from vanillaBlockToLegacyId. */
    private static final Map<Integer, Block> legacyIdToVanillaBlock = new HashMap<>();
    private static boolean vanillaBlockMapInitialized = false;

    /**
     * Maps modern vanilla Item instances to their legacy MC 1.5.2 numeric IDs.
     * Populated lazily on first access via {@link #initVanillaItemMap()}.
     */
    private static final Map<Item, Integer> vanillaItemToLegacyId = new IdentityHashMap<>();
    /** Reverse mapping: legacy item ID → modern MC Item. Built from vanillaItemToLegacyId. */
    private static final Map<Integer, Item> legacyIdToVanillaItem = new HashMap<>();
    private static boolean vanillaItemMapInitialized = false;

    // ---- blocks ----

    public static void registerProxy(int legacyId, ProxyBlock proxy) {
        blocksByLegacyId.put(legacyId, proxy);
    }

    public static ProxyBlock getProxy(int legacyId) {
        return blocksByLegacyId.get(legacyId);
    }

    /**
     * Returns the Forge {@link net.minecraft.world.level.block.Block} for a
     * legacy block ID. Checks ProxyBlocks first, then falls back to the
     * vanilla reverse mapping (legacy ID → modern MC block).
     */
    public static net.minecraft.world.level.block.Block getModernBlock(int legacyId) {
        // Check ProxyBlocks first (FC blocks 175+)
        ProxyBlock proxy = blocksByLegacyId.get(legacyId);
        if (proxy != null) return proxy;

        // Fall back to vanilla reverse mapping (IDs 1-158)
        ensureVanillaBlockMapInitialized();
        return legacyIdToVanillaBlock.get(legacyId);
    }

    // ---- items ----

    /** Reverse map: MC Item → FC legacy ID for ProxyItems. */
    private static final Map<net.minecraft.world.item.Item, Integer> proxyItemToLegacyId = new java.util.HashMap<>();

    public static void registerItem(int legacyId, net.minecraft.world.item.Item item) {
        itemsByLegacyId.put(legacyId, item);
        proxyItemToLegacyId.put(item, legacyId);
    }

    /**
     * Returns the modern MC Item for a legacy item ID. Checks registered FC
     * items first, then falls back to the vanilla reverse mapping, then
     * tries treating the ID as a block ID and getting its item form.
     */
    public static net.minecraft.world.item.Item getModernItem(int legacyId) {
        // Check registered FC items first
        net.minecraft.world.item.Item item = itemsByLegacyId.get(legacyId);
        if (item != null) return item;

        // Check vanilla item reverse mapping
        ensureVanillaItemMapInitialized();
        item = legacyIdToVanillaItem.get(legacyId);
        if (item != null) return item;

        // If it's a block ID (< 256), try getting the block's item form
        if (legacyId < 256) {
            net.minecraft.world.level.block.Block block = getModernBlock(legacyId);
            if (block != null) {
                return block.asItem();
            }
        }

        return null;
    }

    // ================================================================
    // Vanilla Block -> legacy ID mapping
    // ================================================================

    /**
     * Returns the legacy MC 1.5.2 block ID for a modern block instance.
     * <ul>
     *   <li>If the block is a {@link ProxyBlock}, returns its legacy ID directly.</li>
     *   <li>Otherwise, looks up the vanilla block in the static mapping table.</li>
     *   <li>Returns 0 (air) if the block is unmapped.</li>
     * </ul>
     */
    public static int getBlockId(Block block) {
        if (block instanceof ProxyBlock pb) {
            return pb.getLegacyId();
        }
        ensureVanillaBlockMapInitialized();
        Integer id = vanillaBlockToLegacyId.get(block);
        return id != null ? id : 0;
    }

    /**
     * Returns the FC block for a given modern block, or null if unmapped.
     */
    public static btw.modern.Block getFcBlock(Block block) {
        int id = getBlockId(block);
        if (id > 0 && id < btw.modern.Block.blocksList.length) {
            return btw.modern.Block.blocksList[id];
        }
        return null;
    }

    private static synchronized void ensureVanillaBlockMapInitialized() {
        if (!vanillaBlockMapInitialized) {
            initVanillaBlockMap();
            vanillaBlockMapInitialized = true;
        }
    }

    /**
     * Populates the vanilla block -> legacy ID map. IDs match MC 1.5.2.
     * This covers all blocks that existed in MC 1.5.2 and have a direct
     * counterpart in 1.20.1's {@link Blocks} class.
     */
    private static void initVanillaBlockMap() {
        // ID 1: Stone
        vanillaBlockToLegacyId.put(Blocks.STONE, 1);
        // ID 2: Grass Block
        vanillaBlockToLegacyId.put(Blocks.GRASS_BLOCK, 2);
        // ID 3: Dirt
        vanillaBlockToLegacyId.put(Blocks.DIRT, 3);
        // ID 4: Cobblestone
        vanillaBlockToLegacyId.put(Blocks.COBBLESTONE, 4);
        // ID 5: Planks (oak is default)
        vanillaBlockToLegacyId.put(Blocks.OAK_PLANKS, 5);
        vanillaBlockToLegacyId.put(Blocks.SPRUCE_PLANKS, 5);
        vanillaBlockToLegacyId.put(Blocks.BIRCH_PLANKS, 5);
        vanillaBlockToLegacyId.put(Blocks.JUNGLE_PLANKS, 5);
        // ID 6: Sapling
        vanillaBlockToLegacyId.put(Blocks.OAK_SAPLING, 6);
        vanillaBlockToLegacyId.put(Blocks.SPRUCE_SAPLING, 6);
        vanillaBlockToLegacyId.put(Blocks.BIRCH_SAPLING, 6);
        vanillaBlockToLegacyId.put(Blocks.JUNGLE_SAPLING, 6);
        // ID 7: Bedrock
        vanillaBlockToLegacyId.put(Blocks.BEDROCK, 7);
        // ID 8: Flowing Water
        vanillaBlockToLegacyId.put(Blocks.WATER, 8);
        // ID 9: Still Water (same block in modern MC)
        // ID 10: Flowing Lava
        vanillaBlockToLegacyId.put(Blocks.LAVA, 10);
        // ID 11: Still Lava (same block in modern MC)
        // ID 12: Sand
        vanillaBlockToLegacyId.put(Blocks.SAND, 12);
        // ID 13: Gravel
        vanillaBlockToLegacyId.put(Blocks.GRAVEL, 13);
        // ID 14: Gold Ore
        vanillaBlockToLegacyId.put(Blocks.GOLD_ORE, 14);
        // ID 15: Iron Ore
        vanillaBlockToLegacyId.put(Blocks.IRON_ORE, 15);
        // ID 16: Coal Ore
        vanillaBlockToLegacyId.put(Blocks.COAL_ORE, 16);
        // ID 17: Wood (Log)
        vanillaBlockToLegacyId.put(Blocks.OAK_LOG, 17);
        vanillaBlockToLegacyId.put(Blocks.SPRUCE_LOG, 17);
        vanillaBlockToLegacyId.put(Blocks.BIRCH_LOG, 17);
        vanillaBlockToLegacyId.put(Blocks.JUNGLE_LOG, 17);
        // ID 18: Leaves
        vanillaBlockToLegacyId.put(Blocks.OAK_LEAVES, 18);
        vanillaBlockToLegacyId.put(Blocks.SPRUCE_LEAVES, 18);
        vanillaBlockToLegacyId.put(Blocks.BIRCH_LEAVES, 18);
        vanillaBlockToLegacyId.put(Blocks.JUNGLE_LEAVES, 18);
        // ID 19: Sponge
        vanillaBlockToLegacyId.put(Blocks.SPONGE, 19);
        vanillaBlockToLegacyId.put(Blocks.WET_SPONGE, 19);
        // ID 20: Glass
        vanillaBlockToLegacyId.put(Blocks.GLASS, 20);
        // ID 21: Lapis Ore
        vanillaBlockToLegacyId.put(Blocks.LAPIS_ORE, 21);
        // ID 22: Lapis Block
        vanillaBlockToLegacyId.put(Blocks.LAPIS_BLOCK, 22);
        // ID 23: Dispenser
        vanillaBlockToLegacyId.put(Blocks.DISPENSER, 23);
        // ID 24: Sandstone
        vanillaBlockToLegacyId.put(Blocks.SANDSTONE, 24);
        vanillaBlockToLegacyId.put(Blocks.CHISELED_SANDSTONE, 24);
        vanillaBlockToLegacyId.put(Blocks.SMOOTH_SANDSTONE, 24);
        // ID 25: Note Block
        vanillaBlockToLegacyId.put(Blocks.NOTE_BLOCK, 25);
        // ID 26: Bed
        vanillaBlockToLegacyId.put(Blocks.WHITE_BED, 26);
        vanillaBlockToLegacyId.put(Blocks.RED_BED, 26);
        vanillaBlockToLegacyId.put(Blocks.BLACK_BED, 26);
        vanillaBlockToLegacyId.put(Blocks.BLUE_BED, 26);
        vanillaBlockToLegacyId.put(Blocks.BROWN_BED, 26);
        vanillaBlockToLegacyId.put(Blocks.CYAN_BED, 26);
        vanillaBlockToLegacyId.put(Blocks.GRAY_BED, 26);
        vanillaBlockToLegacyId.put(Blocks.GREEN_BED, 26);
        vanillaBlockToLegacyId.put(Blocks.LIGHT_BLUE_BED, 26);
        vanillaBlockToLegacyId.put(Blocks.LIGHT_GRAY_BED, 26);
        vanillaBlockToLegacyId.put(Blocks.LIME_BED, 26);
        vanillaBlockToLegacyId.put(Blocks.MAGENTA_BED, 26);
        vanillaBlockToLegacyId.put(Blocks.ORANGE_BED, 26);
        vanillaBlockToLegacyId.put(Blocks.PINK_BED, 26);
        vanillaBlockToLegacyId.put(Blocks.PURPLE_BED, 26);
        vanillaBlockToLegacyId.put(Blocks.YELLOW_BED, 26);
        // ID 27: Powered Rail
        vanillaBlockToLegacyId.put(Blocks.POWERED_RAIL, 27);
        // ID 28: Detector Rail
        vanillaBlockToLegacyId.put(Blocks.DETECTOR_RAIL, 28);
        // ID 29: Sticky Piston
        vanillaBlockToLegacyId.put(Blocks.STICKY_PISTON, 29);
        // ID 30: Cobweb
        vanillaBlockToLegacyId.put(Blocks.COBWEB, 30);
        // ID 31: Tall Grass
        vanillaBlockToLegacyId.put(Blocks.GRASS, 31);
        vanillaBlockToLegacyId.put(Blocks.FERN, 31);
        // ID 32: Dead Bush
        vanillaBlockToLegacyId.put(Blocks.DEAD_BUSH, 32);
        // ID 33: Piston
        vanillaBlockToLegacyId.put(Blocks.PISTON, 33);
        // ID 34: Piston Head
        vanillaBlockToLegacyId.put(Blocks.PISTON_HEAD, 34);
        // ID 35: Wool
        vanillaBlockToLegacyId.put(Blocks.WHITE_WOOL, 35);
        vanillaBlockToLegacyId.put(Blocks.ORANGE_WOOL, 35);
        vanillaBlockToLegacyId.put(Blocks.MAGENTA_WOOL, 35);
        vanillaBlockToLegacyId.put(Blocks.LIGHT_BLUE_WOOL, 35);
        vanillaBlockToLegacyId.put(Blocks.YELLOW_WOOL, 35);
        vanillaBlockToLegacyId.put(Blocks.LIME_WOOL, 35);
        vanillaBlockToLegacyId.put(Blocks.PINK_WOOL, 35);
        vanillaBlockToLegacyId.put(Blocks.GRAY_WOOL, 35);
        vanillaBlockToLegacyId.put(Blocks.LIGHT_GRAY_WOOL, 35);
        vanillaBlockToLegacyId.put(Blocks.CYAN_WOOL, 35);
        vanillaBlockToLegacyId.put(Blocks.PURPLE_WOOL, 35);
        vanillaBlockToLegacyId.put(Blocks.BLUE_WOOL, 35);
        vanillaBlockToLegacyId.put(Blocks.BROWN_WOOL, 35);
        vanillaBlockToLegacyId.put(Blocks.GREEN_WOOL, 35);
        vanillaBlockToLegacyId.put(Blocks.RED_WOOL, 35);
        vanillaBlockToLegacyId.put(Blocks.BLACK_WOOL, 35);
        // ID 36: Moving Piston (technical block)
        vanillaBlockToLegacyId.put(Blocks.MOVING_PISTON, 36);
        // ID 37: Dandelion
        vanillaBlockToLegacyId.put(Blocks.DANDELION, 37);
        // ID 38: Poppy / Flowers
        vanillaBlockToLegacyId.put(Blocks.POPPY, 38);
        vanillaBlockToLegacyId.put(Blocks.BLUE_ORCHID, 38);
        vanillaBlockToLegacyId.put(Blocks.ALLIUM, 38);
        vanillaBlockToLegacyId.put(Blocks.AZURE_BLUET, 38);
        vanillaBlockToLegacyId.put(Blocks.RED_TULIP, 38);
        vanillaBlockToLegacyId.put(Blocks.ORANGE_TULIP, 38);
        vanillaBlockToLegacyId.put(Blocks.WHITE_TULIP, 38);
        vanillaBlockToLegacyId.put(Blocks.PINK_TULIP, 38);
        vanillaBlockToLegacyId.put(Blocks.OXEYE_DAISY, 38);
        // ID 39: Brown Mushroom
        vanillaBlockToLegacyId.put(Blocks.BROWN_MUSHROOM, 39);
        // ID 40: Red Mushroom
        vanillaBlockToLegacyId.put(Blocks.RED_MUSHROOM, 40);
        // ID 41: Gold Block
        vanillaBlockToLegacyId.put(Blocks.GOLD_BLOCK, 41);
        // ID 42: Iron Block
        vanillaBlockToLegacyId.put(Blocks.IRON_BLOCK, 42);
        // ID 43: Double Stone Slab
        // (In modern MC, double slabs are separate blocks or don't exist as such)
        // ID 44: Stone Slab
        vanillaBlockToLegacyId.put(Blocks.STONE_SLAB, 44);
        vanillaBlockToLegacyId.put(Blocks.SANDSTONE_SLAB, 44);
        vanillaBlockToLegacyId.put(Blocks.COBBLESTONE_SLAB, 44);
        vanillaBlockToLegacyId.put(Blocks.BRICK_SLAB, 44);
        vanillaBlockToLegacyId.put(Blocks.STONE_BRICK_SLAB, 44);
        vanillaBlockToLegacyId.put(Blocks.NETHER_BRICK_SLAB, 44);
        vanillaBlockToLegacyId.put(Blocks.QUARTZ_SLAB, 44);
        // ID 45: Bricks
        vanillaBlockToLegacyId.put(Blocks.BRICKS, 45);
        // ID 46: TNT
        vanillaBlockToLegacyId.put(Blocks.TNT, 46);
        // ID 47: Bookshelf
        vanillaBlockToLegacyId.put(Blocks.BOOKSHELF, 47);
        // ID 48: Mossy Cobblestone
        vanillaBlockToLegacyId.put(Blocks.MOSSY_COBBLESTONE, 48);
        // ID 49: Obsidian
        vanillaBlockToLegacyId.put(Blocks.OBSIDIAN, 49);
        // ID 50: Torch
        vanillaBlockToLegacyId.put(Blocks.TORCH, 50);
        vanillaBlockToLegacyId.put(Blocks.WALL_TORCH, 50);
        // ID 51: Fire
        vanillaBlockToLegacyId.put(Blocks.FIRE, 51);
        // ID 52: Monster Spawner
        vanillaBlockToLegacyId.put(Blocks.SPAWNER, 52);
        // ID 53: Oak Wood Stairs
        vanillaBlockToLegacyId.put(Blocks.OAK_STAIRS, 53);
        // ID 54: Chest
        vanillaBlockToLegacyId.put(Blocks.CHEST, 54);
        // ID 55: Redstone Wire
        vanillaBlockToLegacyId.put(Blocks.REDSTONE_WIRE, 55);
        // ID 56: Diamond Ore
        vanillaBlockToLegacyId.put(Blocks.DIAMOND_ORE, 56);
        // ID 57: Diamond Block
        vanillaBlockToLegacyId.put(Blocks.DIAMOND_BLOCK, 57);
        // ID 58: Crafting Table
        vanillaBlockToLegacyId.put(Blocks.CRAFTING_TABLE, 58);
        // ID 59: Wheat Crops
        vanillaBlockToLegacyId.put(Blocks.WHEAT, 59);
        // ID 60: Farmland
        vanillaBlockToLegacyId.put(Blocks.FARMLAND, 60);
        // ID 61: Furnace (idle)
        vanillaBlockToLegacyId.put(Blocks.FURNACE, 61);
        // ID 62: Burning Furnace (same block in modern MC)
        // ID 63: Standing Sign
        vanillaBlockToLegacyId.put(Blocks.OAK_SIGN, 63);
        // ID 64: Wooden Door
        vanillaBlockToLegacyId.put(Blocks.OAK_DOOR, 64);
        // ID 65: Ladder
        vanillaBlockToLegacyId.put(Blocks.LADDER, 65);
        // ID 66: Rail
        vanillaBlockToLegacyId.put(Blocks.RAIL, 66);
        // ID 67: Cobblestone Stairs
        vanillaBlockToLegacyId.put(Blocks.COBBLESTONE_STAIRS, 67);
        // ID 68: Wall Sign
        vanillaBlockToLegacyId.put(Blocks.OAK_WALL_SIGN, 68);
        // ID 69: Lever
        vanillaBlockToLegacyId.put(Blocks.LEVER, 69);
        // ID 70: Stone Pressure Plate
        vanillaBlockToLegacyId.put(Blocks.STONE_PRESSURE_PLATE, 70);
        // ID 71: Iron Door
        vanillaBlockToLegacyId.put(Blocks.IRON_DOOR, 71);
        // ID 72: Wooden Pressure Plate
        vanillaBlockToLegacyId.put(Blocks.OAK_PRESSURE_PLATE, 72);
        // ID 73: Redstone Ore
        vanillaBlockToLegacyId.put(Blocks.REDSTONE_ORE, 73);
        // ID 74: Glowing Redstone Ore (same block in modern, uses lit state)
        // ID 75: Redstone Torch (off) — same block with lit property in modern
        // ID 76: Redstone Torch (on)
        vanillaBlockToLegacyId.put(Blocks.REDSTONE_TORCH, 76);
        vanillaBlockToLegacyId.put(Blocks.REDSTONE_WALL_TORCH, 76);
        // ID 77: Stone Button
        vanillaBlockToLegacyId.put(Blocks.STONE_BUTTON, 77);
        // ID 78: Snow Layer
        vanillaBlockToLegacyId.put(Blocks.SNOW, 78);
        // ID 79: Ice
        vanillaBlockToLegacyId.put(Blocks.ICE, 79);
        // ID 80: Snow Block
        vanillaBlockToLegacyId.put(Blocks.SNOW_BLOCK, 80);
        // ID 81: Cactus
        vanillaBlockToLegacyId.put(Blocks.CACTUS, 81);
        // ID 82: Clay
        vanillaBlockToLegacyId.put(Blocks.CLAY, 82);
        // ID 83: Sugar Cane
        vanillaBlockToLegacyId.put(Blocks.SUGAR_CANE, 83);
        // ID 84: Jukebox
        vanillaBlockToLegacyId.put(Blocks.JUKEBOX, 84);
        // ID 85: Oak Fence
        vanillaBlockToLegacyId.put(Blocks.OAK_FENCE, 85);
        // ID 86: Pumpkin
        vanillaBlockToLegacyId.put(Blocks.PUMPKIN, 86);
        vanillaBlockToLegacyId.put(Blocks.CARVED_PUMPKIN, 86);
        // ID 87: Netherrack
        vanillaBlockToLegacyId.put(Blocks.NETHERRACK, 87);
        // ID 88: Soul Sand
        vanillaBlockToLegacyId.put(Blocks.SOUL_SAND, 88);
        // ID 89: Glowstone
        vanillaBlockToLegacyId.put(Blocks.GLOWSTONE, 89);
        // ID 90: Nether Portal
        vanillaBlockToLegacyId.put(Blocks.NETHER_PORTAL, 90);
        // ID 91: Jack o'Lantern
        vanillaBlockToLegacyId.put(Blocks.JACK_O_LANTERN, 91);
        // ID 92: Cake
        vanillaBlockToLegacyId.put(Blocks.CAKE, 92);
        // ID 93: Redstone Repeater (off) — same block in modern
        vanillaBlockToLegacyId.put(Blocks.REPEATER, 93);
        // ID 94: Redstone Repeater (on) — same block in modern
        // ID 95: Stained Glass (post-1.5.2 but maps to white for compatibility)
        vanillaBlockToLegacyId.put(Blocks.WHITE_STAINED_GLASS, 95);
        // ID 96: Trapdoor
        vanillaBlockToLegacyId.put(Blocks.OAK_TRAPDOOR, 96);
        // ID 97: Monster Egg (Silverfish blocks)
        vanillaBlockToLegacyId.put(Blocks.INFESTED_STONE, 97);
        vanillaBlockToLegacyId.put(Blocks.INFESTED_COBBLESTONE, 97);
        vanillaBlockToLegacyId.put(Blocks.INFESTED_STONE_BRICKS, 97);
        vanillaBlockToLegacyId.put(Blocks.INFESTED_MOSSY_STONE_BRICKS, 97);
        vanillaBlockToLegacyId.put(Blocks.INFESTED_CRACKED_STONE_BRICKS, 97);
        vanillaBlockToLegacyId.put(Blocks.INFESTED_CHISELED_STONE_BRICKS, 97);
        // ID 98: Stone Bricks
        vanillaBlockToLegacyId.put(Blocks.STONE_BRICKS, 98);
        vanillaBlockToLegacyId.put(Blocks.MOSSY_STONE_BRICKS, 98);
        vanillaBlockToLegacyId.put(Blocks.CRACKED_STONE_BRICKS, 98);
        vanillaBlockToLegacyId.put(Blocks.CHISELED_STONE_BRICKS, 98);
        // ID 99: Brown Mushroom Block
        vanillaBlockToLegacyId.put(Blocks.BROWN_MUSHROOM_BLOCK, 99);
        // ID 100: Red Mushroom Block
        vanillaBlockToLegacyId.put(Blocks.RED_MUSHROOM_BLOCK, 100);
        // ID 101: Iron Bars
        vanillaBlockToLegacyId.put(Blocks.IRON_BARS, 101);
        // ID 102: Glass Pane
        vanillaBlockToLegacyId.put(Blocks.GLASS_PANE, 102);
        // ID 103: Melon Block
        vanillaBlockToLegacyId.put(Blocks.MELON, 103);
        // ID 104: Pumpkin Stem
        vanillaBlockToLegacyId.put(Blocks.PUMPKIN_STEM, 104);
        vanillaBlockToLegacyId.put(Blocks.ATTACHED_PUMPKIN_STEM, 104);
        // ID 105: Melon Stem
        vanillaBlockToLegacyId.put(Blocks.MELON_STEM, 105);
        vanillaBlockToLegacyId.put(Blocks.ATTACHED_MELON_STEM, 105);
        // ID 106: Vines
        vanillaBlockToLegacyId.put(Blocks.VINE, 106);
        // ID 107: Fence Gate
        vanillaBlockToLegacyId.put(Blocks.OAK_FENCE_GATE, 107);
        // ID 108: Brick Stairs
        vanillaBlockToLegacyId.put(Blocks.BRICK_STAIRS, 108);
        // ID 109: Stone Brick Stairs
        vanillaBlockToLegacyId.put(Blocks.STONE_BRICK_STAIRS, 109);
        // ID 110: Mycelium
        vanillaBlockToLegacyId.put(Blocks.MYCELIUM, 110);
        // ID 111: Lily Pad
        vanillaBlockToLegacyId.put(Blocks.LILY_PAD, 111);
        // ID 112: Nether Bricks
        vanillaBlockToLegacyId.put(Blocks.NETHER_BRICKS, 112);
        // ID 113: Nether Brick Fence
        vanillaBlockToLegacyId.put(Blocks.NETHER_BRICK_FENCE, 113);
        // ID 114: Nether Brick Stairs
        vanillaBlockToLegacyId.put(Blocks.NETHER_BRICK_STAIRS, 114);
        // ID 115: Nether Wart
        vanillaBlockToLegacyId.put(Blocks.NETHER_WART, 115);
        // ID 116: Enchanting Table
        vanillaBlockToLegacyId.put(Blocks.ENCHANTING_TABLE, 116);
        // ID 117: Brewing Stand
        vanillaBlockToLegacyId.put(Blocks.BREWING_STAND, 117);
        // ID 118: Cauldron
        vanillaBlockToLegacyId.put(Blocks.CAULDRON, 118);
        // ID 119: End Portal
        vanillaBlockToLegacyId.put(Blocks.END_PORTAL, 119);
        // ID 120: End Portal Frame
        vanillaBlockToLegacyId.put(Blocks.END_PORTAL_FRAME, 120);
        // ID 121: End Stone
        vanillaBlockToLegacyId.put(Blocks.END_STONE, 121);
        // ID 122: Dragon Egg
        vanillaBlockToLegacyId.put(Blocks.DRAGON_EGG, 122);
        // ID 123: Redstone Lamp (off)
        vanillaBlockToLegacyId.put(Blocks.REDSTONE_LAMP, 123);
        // ID 124: Redstone Lamp (on) — same block in modern with lit property
        // ID 125: Double Wooden Slab (no direct modern equivalent)
        // ID 126: Wooden Slab
        vanillaBlockToLegacyId.put(Blocks.OAK_SLAB, 126);
        vanillaBlockToLegacyId.put(Blocks.SPRUCE_SLAB, 126);
        vanillaBlockToLegacyId.put(Blocks.BIRCH_SLAB, 126);
        vanillaBlockToLegacyId.put(Blocks.JUNGLE_SLAB, 126);
        // ID 127: Cocoa
        vanillaBlockToLegacyId.put(Blocks.COCOA, 127);
        // ID 128: Sandstone Stairs
        vanillaBlockToLegacyId.put(Blocks.SANDSTONE_STAIRS, 128);
        // ID 129: Emerald Ore
        vanillaBlockToLegacyId.put(Blocks.EMERALD_ORE, 129);
        // ID 130: Ender Chest
        vanillaBlockToLegacyId.put(Blocks.ENDER_CHEST, 130);
        // ID 131: Tripwire Hook
        vanillaBlockToLegacyId.put(Blocks.TRIPWIRE_HOOK, 131);
        // ID 132: Tripwire
        vanillaBlockToLegacyId.put(Blocks.TRIPWIRE, 132);
        // ID 133: Emerald Block
        vanillaBlockToLegacyId.put(Blocks.EMERALD_BLOCK, 133);
        // ID 134: Spruce Wood Stairs
        vanillaBlockToLegacyId.put(Blocks.SPRUCE_STAIRS, 134);
        // ID 135: Birch Wood Stairs
        vanillaBlockToLegacyId.put(Blocks.BIRCH_STAIRS, 135);
        // ID 136: Jungle Wood Stairs
        vanillaBlockToLegacyId.put(Blocks.JUNGLE_STAIRS, 136);
        // ID 137: Command Block
        vanillaBlockToLegacyId.put(Blocks.COMMAND_BLOCK, 137);
        // ID 138: Beacon
        vanillaBlockToLegacyId.put(Blocks.BEACON, 138);
        // ID 139: Cobblestone Wall
        vanillaBlockToLegacyId.put(Blocks.COBBLESTONE_WALL, 139);
        vanillaBlockToLegacyId.put(Blocks.MOSSY_COBBLESTONE_WALL, 139);
        // ID 140: Flower Pot
        vanillaBlockToLegacyId.put(Blocks.FLOWER_POT, 140);
        // ID 141: Carrots
        vanillaBlockToLegacyId.put(Blocks.CARROTS, 141);
        // ID 142: Potatoes
        vanillaBlockToLegacyId.put(Blocks.POTATOES, 142);
        // ID 143: Wooden Button
        vanillaBlockToLegacyId.put(Blocks.OAK_BUTTON, 143);
        // ID 144: Mob Head (Skull)
        vanillaBlockToLegacyId.put(Blocks.SKELETON_SKULL, 144);
        vanillaBlockToLegacyId.put(Blocks.SKELETON_WALL_SKULL, 144);
        vanillaBlockToLegacyId.put(Blocks.WITHER_SKELETON_SKULL, 144);
        vanillaBlockToLegacyId.put(Blocks.WITHER_SKELETON_WALL_SKULL, 144);
        vanillaBlockToLegacyId.put(Blocks.ZOMBIE_HEAD, 144);
        vanillaBlockToLegacyId.put(Blocks.ZOMBIE_WALL_HEAD, 144);
        vanillaBlockToLegacyId.put(Blocks.PLAYER_HEAD, 144);
        vanillaBlockToLegacyId.put(Blocks.PLAYER_WALL_HEAD, 144);
        vanillaBlockToLegacyId.put(Blocks.CREEPER_HEAD, 144);
        vanillaBlockToLegacyId.put(Blocks.CREEPER_WALL_HEAD, 144);
        // ID 145: Anvil
        vanillaBlockToLegacyId.put(Blocks.ANVIL, 145);
        vanillaBlockToLegacyId.put(Blocks.CHIPPED_ANVIL, 145);
        vanillaBlockToLegacyId.put(Blocks.DAMAGED_ANVIL, 145);
        // ID 146: Trapped Chest
        vanillaBlockToLegacyId.put(Blocks.TRAPPED_CHEST, 146);
        // ID 147: Light Weighted Pressure Plate (Gold)
        vanillaBlockToLegacyId.put(Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE, 147);
        // ID 148: Heavy Weighted Pressure Plate (Iron)
        vanillaBlockToLegacyId.put(Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE, 148);
        // ID 149: Comparator (off)
        vanillaBlockToLegacyId.put(Blocks.COMPARATOR, 149);
        // ID 150: Comparator (on) — same block in modern
        // ID 151: Daylight Sensor
        vanillaBlockToLegacyId.put(Blocks.DAYLIGHT_DETECTOR, 151);
        // ID 152: Redstone Block
        vanillaBlockToLegacyId.put(Blocks.REDSTONE_BLOCK, 152);
        // ID 153: Nether Quartz Ore
        vanillaBlockToLegacyId.put(Blocks.NETHER_QUARTZ_ORE, 153);
        // ID 154: Hopper
        vanillaBlockToLegacyId.put(Blocks.HOPPER, 154);
        // ID 155: Quartz Block
        vanillaBlockToLegacyId.put(Blocks.QUARTZ_BLOCK, 155);
        vanillaBlockToLegacyId.put(Blocks.CHISELED_QUARTZ_BLOCK, 155);
        vanillaBlockToLegacyId.put(Blocks.QUARTZ_PILLAR, 155);
        // ID 156: Quartz Stairs
        vanillaBlockToLegacyId.put(Blocks.QUARTZ_STAIRS, 156);
        // ID 157: Activator Rail
        vanillaBlockToLegacyId.put(Blocks.ACTIVATOR_RAIL, 157);
        // ID 158: Dropper
        vanillaBlockToLegacyId.put(Blocks.DROPPER, 158);
        // ID 159: Stained Clay (Terracotta)
        vanillaBlockToLegacyId.put(Blocks.WHITE_TERRACOTTA, 159);
        vanillaBlockToLegacyId.put(Blocks.ORANGE_TERRACOTTA, 159);
        vanillaBlockToLegacyId.put(Blocks.MAGENTA_TERRACOTTA, 159);
        vanillaBlockToLegacyId.put(Blocks.LIGHT_BLUE_TERRACOTTA, 159);
        vanillaBlockToLegacyId.put(Blocks.YELLOW_TERRACOTTA, 159);
        vanillaBlockToLegacyId.put(Blocks.LIME_TERRACOTTA, 159);
        vanillaBlockToLegacyId.put(Blocks.PINK_TERRACOTTA, 159);
        vanillaBlockToLegacyId.put(Blocks.GRAY_TERRACOTTA, 159);
        vanillaBlockToLegacyId.put(Blocks.LIGHT_GRAY_TERRACOTTA, 159);
        vanillaBlockToLegacyId.put(Blocks.CYAN_TERRACOTTA, 159);
        vanillaBlockToLegacyId.put(Blocks.PURPLE_TERRACOTTA, 159);
        vanillaBlockToLegacyId.put(Blocks.BLUE_TERRACOTTA, 159);
        vanillaBlockToLegacyId.put(Blocks.BROWN_TERRACOTTA, 159);
        vanillaBlockToLegacyId.put(Blocks.GREEN_TERRACOTTA, 159);
        vanillaBlockToLegacyId.put(Blocks.RED_TERRACOTTA, 159);
        vanillaBlockToLegacyId.put(Blocks.BLACK_TERRACOTTA, 159);
        // ID 170: Hay Bale
        vanillaBlockToLegacyId.put(Blocks.HAY_BLOCK, 170);
        // ID 171: Carpet
        vanillaBlockToLegacyId.put(Blocks.WHITE_CARPET, 171);
        // ID 172: Hardened Clay
        vanillaBlockToLegacyId.put(Blocks.TERRACOTTA, 172);
        // ID 173: Block of Coal
        vanillaBlockToLegacyId.put(Blocks.COAL_BLOCK, 173);

        // Build reverse mapping: legacy ID → modern Block
        for (var entry : vanillaBlockToLegacyId.entrySet()) {
            legacyIdToVanillaBlock.putIfAbsent(entry.getValue(), entry.getKey());
        }

        LOGGER.info("Initialized vanilla block -> legacy ID mapping ({} entries, {} reverse).",
                vanillaBlockToLegacyId.size(), legacyIdToVanillaBlock.size());
    }

    // ================================================================
    // Vanilla Item -> legacy ID mapping
    // ================================================================

    /**
     * Returns the legacy MC 1.5.2 item ID for a modern item instance.
     * <ul>
     *   <li>Looks up the vanilla item in the static mapping table.</li>
     *   <li>Falls back to the MC registry ID if the item is unmapped.</li>
     * </ul>
     */
    public static int getItemId(Item item) {
        // Check ProxyItem reverse map first (FC-specific items)
        Integer proxyId = proxyItemToLegacyId.get(item);
        if (proxyId != null) return proxyId;
        // Check vanilla item map
        ensureVanillaItemMapInitialized();
        Integer id = vanillaItemToLegacyId.get(item);
        return id != null ? id : Item.getId(item);
    }

    /**
     * Returns the FC item for a given modern item, or null if unmapped.
     */
    public static btw.modern.Item getFcItem(Item item) {
        int id = getItemId(item);
        if (id > 0 && id < btw.modern.Item.itemsList.length) {
            return btw.modern.Item.itemsList[id];
        }
        return null;
    }

    // ================================================================
    // Headless item system: passthrough entries for modern-only items
    // ================================================================

    /**
     * Next available ID for headless items. Starts at 25000 to avoid
     * all FC item IDs (which max out around ~2000).
     */
    private static int nextHeadlessId = 25000;

    /**
     * Returns a legacy ID for any modern MC item, creating a headless
     * passthrough entry in {@code Item.itemsList[]} if needed.
     *
     * <p>A "headless" item is a minimal FC Item that exists solely so
     * modern-only items (netherite, bundles, etc.) can pass through FC
     * container logic without being destroyed. FC code sees a valid
     * itemID and doesn't NPE; the item roundtrips losslessly.</p>
     *
     * @param modernItem the MC 1.20.1 Item instance
     * @return a positive legacy ID guaranteed to have an entry in itemsList
     */
    public static int getOrCreateLegacyId(Item modernItem) {
        if (modernItem == null) return 0;

        // Check BlockItem first
        if (modernItem instanceof net.minecraft.world.item.BlockItem bi) {
            int blockId = getBlockId(bi.getBlock());
            if (blockId > 0 && blockId < btw.modern.Block.blocksList.length
                    && btw.modern.Block.blocksList[blockId] != null) {
                return blockId;
            }
        }

        // Check existing item mapping
        Integer existing = proxyItemToLegacyId.get(modernItem);
        if (existing != null && existing > 0) {
            // Verify it has an FC entry
            if (existing < btw.modern.Item.itemsList.length
                    && btw.modern.Item.itemsList[existing] != null) {
                return existing;
            }
        }

        // Check vanilla item map
        ensureVanillaItemMapInitialized();
        Integer vanillaId = vanillaItemToLegacyId.get(modernItem);
        if (vanillaId != null && vanillaId > 0) {
            boolean hasItem = vanillaId < btw.modern.Item.itemsList.length
                    && btw.modern.Item.itemsList[vanillaId] != null;
            boolean hasBlock = vanillaId < btw.modern.Block.blocksList.length
                    && btw.modern.Block.blocksList[vanillaId] != null;
            if (hasItem || hasBlock) return vanillaId;
        }

        // No valid FC entry — create a headless passthrough item
        return createHeadlessItem(modernItem);
    }

    private static synchronized int createHeadlessItem(Item modernItem) {
        // Double-check in case another thread raced
        Integer raceCheck = proxyItemToLegacyId.get(modernItem);
        if (raceCheck != null && raceCheck >= 25000) return raceCheck;

        int id = nextHeadlessId++;
        // Item constructor does: this.itemID = 256 + rawId; itemsList[itemID] = this;
        // So rawId = id - 256
        btw.modern.Item headless = new btw.modern.Item(id - 256);
        headless.setMaxStackSize(modernItem.getMaxStackSize());

        // Register both directions
        itemsByLegacyId.put(id, modernItem);
        proxyItemToLegacyId.put(modernItem, id);

        LOGGER.debug("Created headless FC item ID {} for modern item {}",
                id, net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(modernItem));
        return id;
    }

    private static synchronized void ensureVanillaItemMapInitialized() {
        if (!vanillaItemMapInitialized) {
            initVanillaItemMap();
            vanillaItemMapInitialized = true;
        }
    }

    /**
     * Populates the vanilla item -> legacy ID map. IDs match MC 1.5.2.
     * This covers items that existed in MC 1.5.2 and have a direct
     * counterpart in 1.20.1's {@link Items} class.
     */
    private static void initVanillaItemMap() {
        // Complete MC 1.5.2 item ID mapping (display IDs = constructor arg + 256)
        // Tools & weapons
        vanillaItemToLegacyId.put(Items.IRON_SHOVEL, 256);
        vanillaItemToLegacyId.put(Items.IRON_PICKAXE, 257);
        vanillaItemToLegacyId.put(Items.IRON_AXE, 258);
        vanillaItemToLegacyId.put(Items.FLINT_AND_STEEL, 259);
        vanillaItemToLegacyId.put(Items.APPLE, 260);
        vanillaItemToLegacyId.put(Items.BOW, 261);
        vanillaItemToLegacyId.put(Items.ARROW, 262);
        vanillaItemToLegacyId.put(Items.COAL, 263);
        vanillaItemToLegacyId.put(Items.DIAMOND, 264);
        vanillaItemToLegacyId.put(Items.IRON_INGOT, 265);
        vanillaItemToLegacyId.put(Items.GOLD_INGOT, 266);
        vanillaItemToLegacyId.put(Items.IRON_SWORD, 267);
        vanillaItemToLegacyId.put(Items.WOODEN_SWORD, 268);
        vanillaItemToLegacyId.put(Items.WOODEN_SHOVEL, 269);
        vanillaItemToLegacyId.put(Items.WOODEN_PICKAXE, 270);
        vanillaItemToLegacyId.put(Items.WOODEN_AXE, 271);
        vanillaItemToLegacyId.put(Items.STONE_SWORD, 272);
        vanillaItemToLegacyId.put(Items.STONE_SHOVEL, 273);
        vanillaItemToLegacyId.put(Items.STONE_PICKAXE, 274);
        vanillaItemToLegacyId.put(Items.STONE_AXE, 275);
        vanillaItemToLegacyId.put(Items.DIAMOND_SWORD, 276);
        vanillaItemToLegacyId.put(Items.DIAMOND_SHOVEL, 277);
        vanillaItemToLegacyId.put(Items.DIAMOND_PICKAXE, 278);
        vanillaItemToLegacyId.put(Items.DIAMOND_AXE, 279);
        vanillaItemToLegacyId.put(Items.STICK, 280);
        vanillaItemToLegacyId.put(Items.BOWL, 281);
        vanillaItemToLegacyId.put(Items.MUSHROOM_STEW, 282);
        vanillaItemToLegacyId.put(Items.GOLDEN_SWORD, 283);
        vanillaItemToLegacyId.put(Items.GOLDEN_SHOVEL, 284);
        vanillaItemToLegacyId.put(Items.GOLDEN_PICKAXE, 285);
        vanillaItemToLegacyId.put(Items.GOLDEN_AXE, 286);
        vanillaItemToLegacyId.put(Items.STRING, 287);
        vanillaItemToLegacyId.put(Items.FEATHER, 288);
        vanillaItemToLegacyId.put(Items.GUNPOWDER, 289);
        vanillaItemToLegacyId.put(Items.WOODEN_HOE, 290);
        vanillaItemToLegacyId.put(Items.STONE_HOE, 291);
        vanillaItemToLegacyId.put(Items.IRON_HOE, 292);
        vanillaItemToLegacyId.put(Items.DIAMOND_HOE, 293);
        vanillaItemToLegacyId.put(Items.GOLDEN_HOE, 294);
        vanillaItemToLegacyId.put(Items.WHEAT_SEEDS, 295);
        vanillaItemToLegacyId.put(Items.WHEAT, 296);
        vanillaItemToLegacyId.put(Items.BREAD, 297);
        // Armor
        vanillaItemToLegacyId.put(Items.LEATHER_HELMET, 298);
        vanillaItemToLegacyId.put(Items.LEATHER_CHESTPLATE, 299);
        vanillaItemToLegacyId.put(Items.LEATHER_LEGGINGS, 300);
        vanillaItemToLegacyId.put(Items.LEATHER_BOOTS, 301);
        vanillaItemToLegacyId.put(Items.CHAINMAIL_HELMET, 302);
        vanillaItemToLegacyId.put(Items.CHAINMAIL_CHESTPLATE, 303);
        vanillaItemToLegacyId.put(Items.CHAINMAIL_LEGGINGS, 304);
        vanillaItemToLegacyId.put(Items.CHAINMAIL_BOOTS, 305);
        vanillaItemToLegacyId.put(Items.IRON_HELMET, 306);
        vanillaItemToLegacyId.put(Items.IRON_CHESTPLATE, 307);
        vanillaItemToLegacyId.put(Items.IRON_LEGGINGS, 308);
        vanillaItemToLegacyId.put(Items.IRON_BOOTS, 309);
        vanillaItemToLegacyId.put(Items.DIAMOND_HELMET, 310);
        vanillaItemToLegacyId.put(Items.DIAMOND_CHESTPLATE, 311);
        vanillaItemToLegacyId.put(Items.DIAMOND_LEGGINGS, 312);
        vanillaItemToLegacyId.put(Items.DIAMOND_BOOTS, 313);
        vanillaItemToLegacyId.put(Items.GOLDEN_HELMET, 314);
        vanillaItemToLegacyId.put(Items.GOLDEN_CHESTPLATE, 315);
        vanillaItemToLegacyId.put(Items.GOLDEN_LEGGINGS, 316);
        vanillaItemToLegacyId.put(Items.GOLDEN_BOOTS, 317);
        // Materials & food
        vanillaItemToLegacyId.put(Items.FLINT, 318);
        vanillaItemToLegacyId.put(Items.PORKCHOP, 319);
        vanillaItemToLegacyId.put(Items.COOKED_PORKCHOP, 320);
        vanillaItemToLegacyId.put(Items.PAINTING, 321);
        vanillaItemToLegacyId.put(Items.GOLDEN_APPLE, 322);
        vanillaItemToLegacyId.put(Items.OAK_SIGN, 323);
        vanillaItemToLegacyId.put(Items.OAK_DOOR, 324);
        vanillaItemToLegacyId.put(Items.BUCKET, 325);
        vanillaItemToLegacyId.put(Items.WATER_BUCKET, 326);
        vanillaItemToLegacyId.put(Items.LAVA_BUCKET, 327);
        vanillaItemToLegacyId.put(Items.MINECART, 328);
        vanillaItemToLegacyId.put(Items.SADDLE, 329);
        vanillaItemToLegacyId.put(Items.IRON_DOOR, 330);
        vanillaItemToLegacyId.put(Items.REDSTONE, 331);
        vanillaItemToLegacyId.put(Items.SNOWBALL, 332);
        vanillaItemToLegacyId.put(Items.OAK_BOAT, 333);
        vanillaItemToLegacyId.put(Items.LEATHER, 334);
        vanillaItemToLegacyId.put(Items.MILK_BUCKET, 335);
        vanillaItemToLegacyId.put(Items.BRICK, 336);
        vanillaItemToLegacyId.put(Items.CLAY_BALL, 337);
        vanillaItemToLegacyId.put(Items.SUGAR_CANE, 338);
        vanillaItemToLegacyId.put(Items.PAPER, 339);
        vanillaItemToLegacyId.put(Items.BOOK, 340);
        vanillaItemToLegacyId.put(Items.SLIME_BALL, 341);
        vanillaItemToLegacyId.put(Items.CHEST_MINECART, 342);
        vanillaItemToLegacyId.put(Items.FURNACE_MINECART, 343);
        vanillaItemToLegacyId.put(Items.EGG, 344);
        vanillaItemToLegacyId.put(Items.COMPASS, 345);
        vanillaItemToLegacyId.put(Items.FISHING_ROD, 346);
        vanillaItemToLegacyId.put(Items.CLOCK, 347);
        vanillaItemToLegacyId.put(Items.GLOWSTONE_DUST, 348);
        vanillaItemToLegacyId.put(Items.COD, 349);
        vanillaItemToLegacyId.put(Items.COOKED_COD, 350);
        vanillaItemToLegacyId.put(Items.INK_SAC, 351);
        vanillaItemToLegacyId.put(Items.BONE, 352);
        vanillaItemToLegacyId.put(Items.SUGAR, 353);
        vanillaItemToLegacyId.put(Items.CAKE, 354);
        vanillaItemToLegacyId.put(Items.RED_BED, 355);
        vanillaItemToLegacyId.put(Items.REPEATER, 356);
        vanillaItemToLegacyId.put(Items.COOKIE, 357);
        vanillaItemToLegacyId.put(Items.FILLED_MAP, 358);
        vanillaItemToLegacyId.put(Items.SHEARS, 359);
        vanillaItemToLegacyId.put(Items.MELON_SLICE, 360);
        vanillaItemToLegacyId.put(Items.PUMPKIN_SEEDS, 361);
        vanillaItemToLegacyId.put(Items.MELON_SEEDS, 362);
        vanillaItemToLegacyId.put(Items.BEEF, 363);
        vanillaItemToLegacyId.put(Items.COOKED_BEEF, 364);
        vanillaItemToLegacyId.put(Items.CHICKEN, 365);
        vanillaItemToLegacyId.put(Items.COOKED_CHICKEN, 366);
        vanillaItemToLegacyId.put(Items.ROTTEN_FLESH, 367);
        vanillaItemToLegacyId.put(Items.ENDER_PEARL, 368);
        vanillaItemToLegacyId.put(Items.BLAZE_ROD, 369);
        vanillaItemToLegacyId.put(Items.GHAST_TEAR, 370);
        vanillaItemToLegacyId.put(Items.GOLD_NUGGET, 371);
        vanillaItemToLegacyId.put(Items.NETHER_WART, 372);
        vanillaItemToLegacyId.put(Items.POTION, 373);
        vanillaItemToLegacyId.put(Items.GLASS_BOTTLE, 374);
        vanillaItemToLegacyId.put(Items.SPIDER_EYE, 375);
        vanillaItemToLegacyId.put(Items.FERMENTED_SPIDER_EYE, 376);
        vanillaItemToLegacyId.put(Items.BLAZE_POWDER, 377);
        vanillaItemToLegacyId.put(Items.MAGMA_CREAM, 378);
        vanillaItemToLegacyId.put(Items.BREWING_STAND, 379);
        vanillaItemToLegacyId.put(Items.CAULDRON, 380);
        vanillaItemToLegacyId.put(Items.ENDER_EYE, 381);
        vanillaItemToLegacyId.put(Items.GLISTERING_MELON_SLICE, 382);
        // 383: Spawn Egg — every modern per-mob egg maps to the single 1.5.2
        // monsterPlacer id so EntityAgeable.interact's held.itemID ==
        // Item.monsterPlacer.itemID (383) baby-spawn check can match.
        for (net.minecraft.world.item.Item eggItem : net.minecraftforge.registries.ForgeRegistries.ITEMS) {
            if (eggItem instanceof net.minecraft.world.item.SpawnEggItem) {
                vanillaItemToLegacyId.put(eggItem, 383);
            }
        }
        vanillaItemToLegacyId.put(Items.EXPERIENCE_BOTTLE, 384);
        vanillaItemToLegacyId.put(Items.FIRE_CHARGE, 385);
        vanillaItemToLegacyId.put(Items.WRITABLE_BOOK, 386);
        vanillaItemToLegacyId.put(Items.WRITTEN_BOOK, 387);
        vanillaItemToLegacyId.put(Items.EMERALD, 388);
        vanillaItemToLegacyId.put(Items.ITEM_FRAME, 389);
        vanillaItemToLegacyId.put(Items.FLOWER_POT, 390);
        vanillaItemToLegacyId.put(Items.CARROT, 391);
        vanillaItemToLegacyId.put(Items.POTATO, 392);
        vanillaItemToLegacyId.put(Items.BAKED_POTATO, 393);
        vanillaItemToLegacyId.put(Items.POISONOUS_POTATO, 394);
        vanillaItemToLegacyId.put(Items.MAP, 395);
        vanillaItemToLegacyId.put(Items.GOLDEN_CARROT, 396);
        vanillaItemToLegacyId.put(Items.SKELETON_SKULL, 397);
        vanillaItemToLegacyId.put(Items.CARROT_ON_A_STICK, 398);
        vanillaItemToLegacyId.put(Items.NETHER_STAR, 399);
        vanillaItemToLegacyId.put(Items.PUMPKIN_PIE, 400);
        vanillaItemToLegacyId.put(Items.FIREWORK_ROCKET, 401);
        vanillaItemToLegacyId.put(Items.FIREWORK_STAR, 402);
        vanillaItemToLegacyId.put(Items.ENCHANTED_BOOK, 403);
        vanillaItemToLegacyId.put(Items.COMPARATOR, 404);
        vanillaItemToLegacyId.put(Items.NETHER_BRICK, 405);
        vanillaItemToLegacyId.put(Items.QUARTZ, 406);
        vanillaItemToLegacyId.put(Items.TNT_MINECART, 407);
        vanillaItemToLegacyId.put(Items.HOPPER_MINECART, 408);
        vanillaItemToLegacyId.put(Items.IRON_HORSE_ARMOR, 417);
        vanillaItemToLegacyId.put(Items.GOLDEN_HORSE_ARMOR, 418);
        vanillaItemToLegacyId.put(Items.DIAMOND_HORSE_ARMOR, 419);
        vanillaItemToLegacyId.put(Items.LEAD, 420);
        vanillaItemToLegacyId.put(Items.NAME_TAG, 421);
        // Music discs
        vanillaItemToLegacyId.put(Items.MUSIC_DISC_13, 2256);
        vanillaItemToLegacyId.put(Items.MUSIC_DISC_CAT, 2257);
        vanillaItemToLegacyId.put(Items.MUSIC_DISC_BLOCKS, 2258);
        vanillaItemToLegacyId.put(Items.MUSIC_DISC_CHIRP, 2259);
        vanillaItemToLegacyId.put(Items.MUSIC_DISC_FAR, 2260);
        vanillaItemToLegacyId.put(Items.MUSIC_DISC_MALL, 2261);
        vanillaItemToLegacyId.put(Items.MUSIC_DISC_MELLOHI, 2262);
        vanillaItemToLegacyId.put(Items.MUSIC_DISC_STAL, 2263);
        vanillaItemToLegacyId.put(Items.MUSIC_DISC_STRAD, 2264);
        vanillaItemToLegacyId.put(Items.MUSIC_DISC_WARD, 2265);
        vanillaItemToLegacyId.put(Items.MUSIC_DISC_11, 2266);
        vanillaItemToLegacyId.put(Items.MUSIC_DISC_WAIT, 2267);

        // Build reverse mapping: legacy ID → modern Item. Skip spawn eggs: they all map
        // to 383, so putIfAbsent would pick an arbitrary egg by iteration order (a
        // different mob per JVM run). getModernItem(383) stays null instead.
        for (var entry : vanillaItemToLegacyId.entrySet()) {
            if (entry.getKey() instanceof net.minecraft.world.item.SpawnEggItem) continue;
            legacyIdToVanillaItem.putIfAbsent(entry.getValue(), entry.getKey());
        }

        LOGGER.info("Initialized vanilla item -> legacy ID mapping ({} entries, {} reverse).",
                vanillaItemToLegacyId.size(), legacyIdToVanillaItem.size());
    }
}
