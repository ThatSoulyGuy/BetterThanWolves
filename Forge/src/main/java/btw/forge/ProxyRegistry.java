package btw.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

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
    // Concurrent: written at runtime by createHeadlessItem / restoreHeadlessIds and read by both the
    // integrated-server thread and the render thread (singleplayer shares these statics; triage RE4).
    private static final Map<Integer, net.minecraft.world.item.Item> itemsByLegacyId = new ConcurrentHashMap<>();

    /**
     * Maps modern vanilla Block instances to their legacy MC 1.5.2 numeric IDs.
     * Populated lazily on first access via {@link #initVanillaBlockMap()}. Insertion-ordered (MC Blocks
     * use identity equals/hashCode) so the reverse map below picks the FIRST block listed for an id:
     * an IdentityHashMap iterated in identity-hash order, i.e. a different species/colour per JVM run.
     */
    private static final Map<Block, Integer> vanillaBlockToLegacyId = new LinkedHashMap<>();
    /** Reverse mapping: legacy block ID → modern MC Block (first listed wins). Built from vanillaBlockToLegacyId. */
    private static final Map<Integer, Block> legacyIdToVanillaBlock = new HashMap<>();
    /**
     * Deepslate forms (ruling 2026-10-21): BTW's deep-strata stone, its cobblestone and its ores in the world. FC identity
     * for the world only: their items keep their own identity (getBlockFormItemId), and StrataRule reads them as deep.
     * Filled by initVanillaBlockMap under the class lock, published by vanillaBlockMapInitialized.
     */
    private static final java.util.Set<Block> DEEPSLATE_FORMS =
            java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    // volatile: the maps are filled under the class lock, then published by this write (RE4)
    private static volatile boolean vanillaBlockMapInitialized = false;

    /**
     * Maps modern vanilla Item instances to their legacy MC 1.5.2 numeric IDs.
     * Populated lazily on first access via {@link #initVanillaItemMap()}.
     */
    private static final Map<Item, Integer> vanillaItemToLegacyId = new IdentityHashMap<>();
    /** Reverse mapping: legacy item ID → modern MC Item. Built from vanillaItemToLegacyId. */
    private static final Map<Integer, Item> legacyIdToVanillaItem = new HashMap<>();
    private static volatile boolean vanillaItemMapInitialized = false;

    // ---- blocks ----

    public static void registerProxy(int legacyId, ProxyBlock proxy) {
        blocksByLegacyId.put(legacyId, proxy);
    }

    public static ProxyBlock getProxy(int legacyId) {
        return blocksByLegacyId.get(legacyId);
    }

    /**
     * FCMOD-INTEGRATION(1.20.1): fc_redstone: FC blocks of id 175+ that are 1.20.1 block classes rather than ProxyBlocks,
     * registered under the same {@code betterthanwolves:block_<id>} name: BTW's detector rails 235 and 236
     * ({@link FcDetectorRailBlock}), so 1.20.1's minecarts ride them. Each keeps its FC identity here (getBlockId,
     * getModernBlock); FC's world reads, hooks, ticks and drops follow it as for a vanilla-id FC block. Written during
     * block registration only, read everywhere after.
     */
    private static final Map<Integer, Block> fcOwnBlocksByLegacyId = new ConcurrentHashMap<>();

    /**
     * Records {@code block} as the 1.20.1 block of FC id {@code legacyId} (block registration, BTWRegistration). The
     * block knows its own id ({@link FcDetectorRailBlock#getLegacyId}), which getBlockId reads.
     */
    public static void registerFcOwnBlock(int legacyId, Block block) {
        fcOwnBlocksByLegacyId.put(legacyId, block);
    }

    /** The 1.20.1 block class FC id {@code legacyId} is registered as when it is no ProxyBlock, or null. */
    public static Block getFcOwnBlock(int legacyId) {
        return fcOwnBlocksByLegacyId.get(legacyId);
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
        // FCMOD-INTEGRATION(1.20.1): fc_redstone: FC blocks 175+ that are 1.20.1 block classes (detector rails 235, 236)
        Block fcOwn = fcOwnBlocksByLegacyId.get(legacyId);
        if (fcOwn != null) return fcOwn;

        // Fall back to vanilla reverse mapping (IDs 1-158)
        ensureVanillaBlockMapInitialized();
        return legacyIdToVanillaBlock.get(legacyId);
    }

    // ---- items ----

    /**
     * Reverse map: MC Item → FC legacy ID for ProxyItems and headless items. Concurrent for the same
     * reason as itemsByLegacyId (RE4); a plain HashMap read here while createHeadlessItem resized it on
     * the other thread could miss and allocate a second headless id for the same item.
     */
    private static final Map<net.minecraft.world.item.Item, Integer> proxyItemToLegacyId = new ConcurrentHashMap<>();

    public static void registerItem(int legacyId, net.minecraft.world.item.Item item) {
        if (item == null) return;   // ConcurrentHashMap takes no null keys/values
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

    /**
     * 1.5.2 blocks whose metadata subtypes are distinct 1.20.1 blocks (wood species, wool colour,
     * sandstone/stone-brick/slab type, ...). Legacy ids collapse every variant onto one id, so the single
     * getModernBlock() mapping resolves to one canonical block. This table restores the variant from the
     * subtype bits of the metadata ({@code meta & mask}; slab bit 8 = upper half, log bits 2-3 = axis are
     * outside the mask), in the order of the 1.5.2 subtype tables (BlockWood.java:19, BlockSapling.java:20,
     * BlockLog.java:21, BlockLeaves.java:20, BlockSandStone.java:21, BlockTallGrass.java:22, BlockCloth,
     * BlockStep.java:23, BlockSilverfish.java:23, BlockStoneBrick.java:21, BlockWoodSlab.java:23,
     * BlockWall.java:22), matching LegacyVariants' canonical item picks (24:2 = cut sandstone,
     * 44:0 = smooth stone slab). A null entry has no 1.20.1 block under this id.
     */
    private static final Map<Integer, Variants> VARIANT_BLOCKS = new HashMap<>();
    /** Reverse of VARIANT_BLOCKS: block -> its subtype metadata (WorldBridge.deriveVanillaMetadata). */
    private static final Map<Block, Integer> VARIANT_META = new IdentityHashMap<>();

    private static final class Variants {
        final int mask;
        final Block[] blocks;

        Variants(int mask, Block... blocks) {
            this.mask = mask;
            this.blocks = blocks;
        }
    }

    private static void variants(int legacyId, int mask, Block... blocks) {
        VARIANT_BLOCKS.put(legacyId, new Variants(mask, blocks));
        for (int meta = 0; meta < blocks.length; meta++) {
            if (blocks[meta] != null) VARIANT_META.putIfAbsent(blocks[meta], meta);
        }
    }

    static {
        // FCMOD-INTEGRATION(1.20.1): modern_woods: FC planks subtypes are FC wood types: 0-3 FC's, 4 blood wood (no
        // 1.20.1 block: oak, as before), 5-8 acacia, dark oak, mangrove and cherry (ModernWoods). Item and world: the
        // four modern planks answer 5 while their sub-blocks exist (getBlockId, ModernWoodSubBlocks), so FC's planks
        // rule and saw run on them, and an FC-placed 5:5 is acacia planks.
        variants(5, 15, Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS, Blocks.BIRCH_PLANKS, Blocks.JUNGLE_PLANKS, null,
                Blocks.ACACIA_PLANKS, Blocks.DARK_OAK_PLANKS, Blocks.MANGROVE_PLANKS, Blocks.CHERRY_PLANKS);
        variants(6, 3, Blocks.OAK_SAPLING, Blocks.SPRUCE_SAPLING, Blocks.BIRCH_SAPLING, Blocks.JUNGLE_SAPLING);
        variants(17, 3, Blocks.OAK_LOG, Blocks.SPRUCE_LOG, Blocks.BIRCH_LOG, Blocks.JUNGLE_LOG);
        // log2 (ModernWoods): species = metadata bits 0-1 = FC wood type - 5, as log 17's species are FC's 0-3
        variants(ModernWoods.LOG2_ID, 3, Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG, Blocks.MANGROVE_LOG, Blocks.CHERRY_LOG);
        variants(18, 3, Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.BIRCH_LEAVES, Blocks.JUNGLE_LEAVES);
        // {default, chiseled, smooth}: 1.5.2 "smooth" sandstone is 1.20.1 cut sandstone
        variants(24, 3, Blocks.SANDSTONE, Blocks.CHISELED_SANDSTONE, Blocks.CUT_SANDSTONE);
        // {deadbush, tallgrass, fern}: the dead shrub keeps its own id 32 in 1.20.1 terms
        variants(31, 3, null, Blocks.GRASS, Blocks.FERN);
        variants(35, 15, Blocks.WHITE_WOOL, Blocks.ORANGE_WOOL, Blocks.MAGENTA_WOOL, Blocks.LIGHT_BLUE_WOOL,
                Blocks.YELLOW_WOOL, Blocks.LIME_WOOL, Blocks.PINK_WOOL, Blocks.GRAY_WOOL,
                Blocks.LIGHT_GRAY_WOOL, Blocks.CYAN_WOOL, Blocks.PURPLE_WOOL, Blocks.BLUE_WOOL,
                Blocks.BROWN_WOOL, Blocks.GREEN_WOOL, Blocks.RED_WOOL, Blocks.BLACK_WOOL);
        // {stone, sand, wood, cobble, brick, smoothStoneBrick, netherBrick, quartz}; 43 = the double slab
        Block[] stoneSlabs = {Blocks.SMOOTH_STONE_SLAB, Blocks.SANDSTONE_SLAB, Blocks.PETRIFIED_OAK_SLAB,
                Blocks.COBBLESTONE_SLAB, Blocks.BRICK_SLAB, Blocks.STONE_BRICK_SLAB, Blocks.NETHER_BRICK_SLAB,
                Blocks.QUARTZ_SLAB};
        variants(44, 7, stoneSlabs);
        variants(43, 7, stoneSlabs);
        variants(97, 3, Blocks.INFESTED_STONE, Blocks.INFESTED_COBBLESTONE, Blocks.INFESTED_STONE_BRICKS);
        variants(98, 3, Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS,
                Blocks.CHISELED_STONE_BRICKS);
        Block[] woodSlabs = {Blocks.OAK_SLAB, Blocks.SPRUCE_SLAB, Blocks.BIRCH_SLAB, Blocks.JUNGLE_SLAB};
        variants(126, 3, woodSlabs);
        variants(125, 3, woodSlabs);
        variants(139, 1, Blocks.COBBLESTONE_WALL, Blocks.MOSSY_COBBLESTONE_WALL);
        // FCMOD-INTEGRATION(1.20.1): flower_subtypes: the small flowers 1.5.2 never had are port-only subtypes of the rose 38:
        // 1.7.2's red_flower order for 0-8 (0 poppy, 1 blue orchid, 2 allium, 3 azure bluet, 4-7 red, orange, white and
        // pink tulip, 8 oxeye daisy), then 9 cornflower and 10 lily of the valley: FC places, reads and stores each flower
        // as itself (triage CF2-P8c). 11-15 read as the poppy.
        variants(38, 15, Blocks.POPPY, Blocks.BLUE_ORCHID, Blocks.ALLIUM, Blocks.AZURE_BLUET, Blocks.RED_TULIP,
                Blocks.ORANGE_TULIP, Blocks.WHITE_TULIP, Blocks.PINK_TULIP, Blocks.OXEYE_DAISY, Blocks.CORNFLOWER,
                Blocks.LILY_OF_THE_VALLEY);
    }

    /**
     * Returns the 1.20.1 block variant for a legacy id + damage/metadata (the id's subtype bits),
     * or null if the id has no per-metadata variants (or none for this subtype).
     */
    public static net.minecraft.world.level.block.Block getModernBlockVariant(int legacyId, int damage) {
        Variants variants = VARIANT_BLOCKS.get(legacyId);
        if (variants == null) return null;
        int idx = damage & variants.mask;
        return idx < variants.blocks.length && variants.blocks[idx] != null
                ? variants.blocks[idx] : variants.blocks[0];
    }

    /**
     * True if the legacy id's subtype bits pick distinct 1.20.1 blocks (the VARIANT_BLOCKS table). An FC metadata
     * write on such an id may have to switch the 1.20.1 block, as 1.5.2's metadata IS the subtype
     * (WorldBridge.setBlockMetadata: stone bricks 98 turning mossy / cracked).
     */
    static boolean isVariantId(int legacyId) {
        return VARIANT_BLOCKS.containsKey(legacyId);
    }

    /**
     * 1.5.2 subtype metadata of a 1.20.1 block that is one variant of a legacy id (spruce planks -> 1,
     * orange wool -> 1, cut sandstone -> 2, cobblestone slab -> 3), or -1 if it is not a table variant.
     */
    static int getLegacyVariantMeta(net.minecraft.world.level.block.Block block) {
        Integer meta = VARIANT_META.get(block);
        return meta != null ? meta : -1;
    }

    // ================================================================
    // Vanilla Block -> legacy ID mapping
    // ================================================================

    /**
     * Legacy 1.5.2 id for a modern block (identity mapping, used for routing MC callbacks/items to FC:
     * getFcBlock, BlockBehaviorMixin, ItemStackMixin...). Returns 0 when unmapped OR when the mapped id
     * has no entry in btw.modern.Block.blocksList. 1.5.2/BTW code dereferences blocksList[id] unguarded for
     * any id > 0 (vanilla/client PathFinder.java:452, EntityLiving.java:3708, Entity.java:943/1028), and this
     * table maps 95/137/152 (1.5.2 ids with no block created here) and 159/170-173 (1.6+ ids) to empty slots
     * (triage HAI-4: "tempBlock is null" in PathFinder.GetPathWeightAtLocation).
     * Modern-only blocks must keep returning 0 HERE so they never run FC block code; FC reads of the live
     * world use {@link #getWorldBlockId} instead. One intended exception, the modern logs with FC identity: the
     * acacia, dark oak, mangrove and cherry logs answer log2 ({@link ModernWoods#LOG2_ID}, 165, FC wood types 5-8),
     * a plain FCBlockLog, so FC's log code runs on them as it runs on oak (id 17). They answer 0 when log2 was not
     * built ({@link ModernWoods#isBuilt}: id 165 taken at start-up), so another block at 165 never receives them.
     * A second, the deepslate forms (StrataRule): deepslate, cobbled deepslate and the seven deepslate ores answer 1, 4,
     * 14, 15, 16, 21, 56, 73 and 129, so FC's stone, cobblestone and ore code runs on them; their items keep their own
     * identity ({@link #getBlockFormItemId}).
     * A third, the acacia, dark oak, mangrove and cherry planks answer 5 (FC planks 5:5-8) while their sub-blocks exist
     * ({@link ModernWoodSubBlocks#isBuilt}), so FC's planks code and saw run on them as on oak planks.
     */
    public static int getBlockId(Block block) {
        if (block instanceof ProxyBlock pb) {
            return pb.getLegacyId();
        }
        // FCMOD-INTEGRATION(1.20.1): fc_redstone: an FC block 175+ registered as a 1.20.1 block class (detector rails)
        if (block instanceof FcDetectorRailBlock rail) {
            return hasFcBlock(rail.getLegacyId()) ? rail.getLegacyId() : 0;
        }
        ensureVanillaBlockMapInitialized();
        Integer id = vanillaBlockToLegacyId.get(block);
        if (id == null || !hasFcBlock(id)) return 0;
        if (id == ModernWoods.LOG2_ID && !ModernWoods.isBuilt()) return 0;
        // FCMOD-INTEGRATION(1.20.1): modern_wood_subblocks: the four newer planks answer 5 only while their sub-blocks exist
        if (id == 5 && ModernWoodSubBlocks.withholdsPlanksIdentity(block)) return 0;
        return id;
    }

    /** True if btw.modern.Block.blocksList[id] is populated. */
    static boolean hasFcBlock(int id) {
        return id > 0 && id < btw.modern.Block.blocksList.length
                && btw.modern.Block.blocksList[id] != null;
    }

    /** True if any 1.20.1 block maps to this legacy id (ProxyBlock or vanilla table). Stand-in ids must avoid these. */
    static boolean isLegacyIdMapped(int id) {
        ensureVanillaBlockMapInitialized();
        return blocksByLegacyId.containsKey(id) || legacyIdToVanillaBlock.containsKey(id)
                || fcOwnBlocksByLegacyId.containsKey(id);
    }

    /** 1.5.2 Block.waterMoving: what Blocks.WATER reads as (vanilla/client Block.java:98). */
    private static final int LEGACY_WATER_ID = 8;
    /** 1.5.2 Block.wood, the log (vanilla/server Block.java, id 17). */
    private static final int LEGACY_LOG_ID = 17;

    /** A log form FC reads as a log: the log id it reads as (17, or log2 165) and its species within that id (0-3). */
    private record LogForm(int logId, int species) {}

    /**
     * The bark ("wood") and stripped forms of the eight log species with an FC log, by log id and species: log 17's
     * oak 0, spruce 1, birch 2, jungle 3 (the log variant order), and log2's acacia 0, dark oak 1, mangrove 2,
     * cherry 3 (FC wood types 5-8, {@link ModernWoods}). 1.20.1 treats them as the species' logs (BlockTags.LOGS holds
     * up leaves, LeavesBlock.java:99; BlockTags.JUNGLE_LOGS holds cocoa, CocoaBlock.java:60-63), so FC's world reads see
     * them as their species' log ({@link #getWorldBlockId}): FCBlockLeaves counts them as support (Server
     * FCBlockLeaves.java:171-181) and FCBlockCocoa / BlockCocoa as a jungle log, where as stand-ins a stripped trunk let
     * its canopy decay and dropped the pods on it. Read path only: no identity mapping, so no FC callback, drop or
     * harvest runs on them through routing, and FC metadata writes keep the block (WorldBridge.variantStateFor). FC
     * machines that copy a block by id and metadata (pistons, the block dispenser) recreate the plain log.
     */
    // FCMOD-INTEGRATION(1.20.1): modern_woods: the wood and stripped forms of acacia, dark oak, mangrove and cherry read as log2 (165), as oak's read as log 17
    private static final Map<Block, LogForm> LOG_FORMS = new IdentityHashMap<>();

    static {
        logForms(LEGACY_LOG_ID, 0, Blocks.OAK_WOOD, Blocks.STRIPPED_OAK_LOG, Blocks.STRIPPED_OAK_WOOD);
        logForms(LEGACY_LOG_ID, 1, Blocks.SPRUCE_WOOD, Blocks.STRIPPED_SPRUCE_LOG, Blocks.STRIPPED_SPRUCE_WOOD);
        logForms(LEGACY_LOG_ID, 2, Blocks.BIRCH_WOOD, Blocks.STRIPPED_BIRCH_LOG, Blocks.STRIPPED_BIRCH_WOOD);
        logForms(LEGACY_LOG_ID, 3, Blocks.JUNGLE_WOOD, Blocks.STRIPPED_JUNGLE_LOG, Blocks.STRIPPED_JUNGLE_WOOD);
        logForms(ModernWoods.LOG2_ID, 0, Blocks.ACACIA_WOOD, Blocks.STRIPPED_ACACIA_LOG, Blocks.STRIPPED_ACACIA_WOOD);
        logForms(ModernWoods.LOG2_ID, 1, Blocks.DARK_OAK_WOOD, Blocks.STRIPPED_DARK_OAK_LOG,
                Blocks.STRIPPED_DARK_OAK_WOOD);
        logForms(ModernWoods.LOG2_ID, 2, Blocks.MANGROVE_WOOD, Blocks.STRIPPED_MANGROVE_LOG,
                Blocks.STRIPPED_MANGROVE_WOOD);
        logForms(ModernWoods.LOG2_ID, 3, Blocks.CHERRY_WOOD, Blocks.STRIPPED_CHERRY_LOG, Blocks.STRIPPED_CHERRY_WOOD);
    }

    private static void logForms(int logId, int species, Block... blocks) {
        LogForm form = new LogForm(logId, species);
        for (Block b : blocks) LOG_FORMS.put(b, form);
    }

    /** True when FC has the log {@code logId}: blocksList[17], or log2 once ModernWoods built it. */
    private static boolean hasLogBlock(int logId) {
        return hasFcBlock(logId) && (logId != ModernWoods.LOG2_ID || ModernWoods.isBuilt());
    }

    /**
     * True for a wood or stripped form (no FC identity) that FC's world reads see as log 17 or log2 165: the bark and
     * stripped forms of the eight species above, while their log exists. False for the logs themselves (identity),
     * the stems and every modded log.
     */
    public static boolean isLogForm(Block block) {
        LogForm form = block != null ? LOG_FORMS.get(block) : null;
        return form != null && hasLogBlock(form.logId());
    }

    /**
     * The FC log id {@code block} is read as: 17 or 165 ({@link ModernWoods#LOG2_ID}) for the eight logs with FC
     * identity and for their wood and stripped forms ({@link #isLogForm}); -1 for any other block, or when that log
     * does not exist.
     */
    public static int getTreeLogId(Block block) {
        if (block == null) return -1;
        LogForm form = LOG_FORMS.get(block);
        if (form != null) return hasLogBlock(form.logId()) ? form.logId() : -1;
        int id = getBlockId(block);
        return id == LEGACY_LOG_ID || id == ModernWoods.LOG2_ID ? id : -1;
    }

    /**
     * Log species (0-3, metadata bits 0-1) of a block FC reads as a log ({@link #getTreeLogId}), within its log id:
     * oak, spruce, birch, jungle for log 17; acacia, dark oak, mangrove, cherry for log2. -1 for any other block, or
     * when that log does not exist. Not the FC wood type: cherry is species 3 like jungle ({@link #getWoodType}).
     */
    public static int getTreeLogSpecies(Block block) {
        if (block == null) return -1;
        LogForm form = LOG_FORMS.get(block);
        if (form != null) return hasLogBlock(form.logId()) ? form.species() : -1;
        int id = getBlockId(block);
        return id == LEGACY_LOG_ID || id == ModernWoods.LOG2_ID ? Math.max(getLegacyVariantMeta(block), 0) : -1;
    }

    /**
     * FC wood type of a block FC reads as a log: 0-3 (oak, spruce, birch, jungle: log 17's species) or 5-8 (acacia,
     * dark oak, mangrove, cherry: log2 species + 5, {@link ModernWoods#woodType}), the number FC keys bark, planks and
     * the work stump on. -1 for any other block (stems, modded logs), or when its log does not exist.
     */
    public static int getWoodType(Block block) {
        int logId = getTreeLogId(block);
        if (logId < 0) return -1;
        int species = getTreeLogSpecies(block);
        if (species < 0) return -1;
        return logId == ModernWoods.LOG2_ID ? ModernWoods.woodType(species) : species;
    }

    /**
     * Legacy id whose FC block hears the removal of {@code block} (BlockBehaviorMixin.btw$onRemove -> breakBlock): its
     * identity id ({@link #getBlockId}), or the log id (17 or log2 165) for the bark and stripped log forms FC reads
     * as logs, so cutting a stripped trunk runs FC's log removal as cutting a log does (BlockLog.breakBlock: the leaf
     * decay check of every leaf within 4, vanilla/server BlockLog.java:57-85; FCBlockLog adds nothing to it). 0 when
     * FC has no block for it.
     */
    public static int getRemovalLegacyId(Block block) {
        int id = getBlockId(block);
        if (id == 0 && isLogForm(block)) return LOG_FORMS.get(block).logId();
        if (id == 0) {
            // A crop twin (beetroots, torchflower and pitcher crops, TwinRegistry): its FC crop hears the removal like
            // FC's own crops, so FC farmland under it untills (FCBlockFarmland.NotifyOfPlantAboveRemoved, S16).
            int twin = TwinRegistry.removalIdFor(block);
            if (twin > 0 && hasFcBlock(twin)) return twin;
        }
        return id;
    }

    /** 1.5.2 Block.stoneSingleSlab / woodSingleSlab; their double-slab ids are one lower (vanilla/client Block.java:212-213, 301-302). */
    private static final int LEGACY_STONE_SLAB_ID = 44;
    private static final int LEGACY_WOOD_SLAB_ID = 126;

    /**
     * Legacy id FC sees when it reads the LIVE WORLD (WorldBridge.getBlockId -> PathFinder, Entity.moveEntity
     * collisions, isBlockNormalCube, FC neighbour logic). Identity mapping, except:
     * <ul>
     *   <li>an MC double slab reads as the 1.5.2 double-slab id 43 / 125 (BlockStep / BlockWoodSlab
     *       isDoubleSlab; the bottom/top half is reported through metadata bit 8);</li>
     *   <li>the bark and stripped forms of the oak, spruce, birch and jungle logs read as log 17 of their species,
     *       those of the acacia, dark oak, mangrove and cherry logs as log2 165 ({@link #LOG_FORMS});</li>
     *   <li>a post-1.5.2 plant that grows by an FC twin ({@link TwinRegistry}: acacia, cherry and dark oak saplings,
     *       the planted mangrove propagule, azaleas, beetroots, the torchflower crop, the pitcher crop's lower half,
     *       crimson and warped fungi) reads as its twin's id, so the twin's FC tick finds itself at its cell;</li>
     *   <li>a colliding 1.20.1 state with no usable 1.5.2 id reads as a {@link ModernBlockStandIn}
     *       instead of air (design_blockmap U4: puppets fell through tuff, calcite, ...);</li>
     *   <li>a non-colliding unmapped state whose fluid is water (seagrass, kelp, bubble column, coral,
     *       waterlogged plants) reads as water: in 1.5.2 terms that cell IS water (triage D5).</li>
     * </ul>
     * Guaranteed: 0 or an id whose blocksList slot is populated. Not for routing/items: those keep
     * {@link #getBlockId(Block)}.
     */
    public static int getWorldBlockId(BlockState state, BlockGetter level, BlockPos pos) {
        int id = getBlockId(state.getBlock());
        if ((id == LEGACY_STONE_SLAB_ID || id == LEGACY_WOOD_SLAB_ID) && state.hasProperty(SlabBlock.TYPE)
                && state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE) {
            id -= 1;
        } else if (id == 0 && !state.isAir()) {
            int twin = TwinRegistry.worldIdFor(state);
            if (twin > 0) {
                id = twin;
            } else if (isLogForm(state.getBlock())) {
                id = LOG_FORMS.get(state.getBlock()).logId();
            } else {
                id = ModernBlockStandIn.idFor(state, level, pos);
                if (id == 0 && state.getFluidState().is(FluidTags.WATER)) {
                    id = LEGACY_WATER_ID;
                }
            }
        }
        return hasFcBlock(id) ? id : 0;
    }

    /**
     * Legacy id FC's onNeighborBlockChange sees for the block that notified it. 1.20.1 neighborChanged passes the
     * source Block (the old block for a setBlock, as 1.5.2 World.setBlock passes the old id, World.java:461-478);
     * a ProxyBlock or legacy-id vanilla block reports its id, any other solid block its stand-in id (the
     * getWorldBlockId rule), so FCBlockBuddyBlock (which ignores id 0) trips on vanilla and post-1.5.2 changes too.
     */
    public static int getNeighbourSourceId(Block block, BlockGetter level, BlockPos pos) {
        return getWorldBlockId(block.defaultBlockState(), level, pos);
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

    private static void deepslateForm(Block block, int legacyId) {
        vanillaBlockToLegacyId.put(block, legacyId);
        DEEPSLATE_FORMS.add(block);
    }

    /** True for deepslate, cobbled deepslate and the seven deepslate ores (StrataRule: always the deep strata). */
    public static boolean isDeepslateForm(Block block) {
        ensureVanillaBlockMapInitialized();
        return block != null && DEEPSLATE_FORMS.contains(block);
    }

    /**
     * Legacy id of a BlockItem's block form, for item identity (getOrCreateLegacyId, hasRealLegacyId, ProxyItem.fcItemFor,
     * ModernCookingImport.isMapped): getBlockId, but 0 for a deepslate form, whose item stays its own 1.20.1 item (a
     * headless passthrough, as before the forms had FC identity), so BTW containers and BTW's placement never turn it into
     * stone. World identity (FC's reads of the cell, its dig, harvest and hooks) keeps getBlockId.
     */
    // FCMOD-INTEGRATION(1.20.1): strata: deepslate forms are BTW blocks in the world only; their items keep their identity
    public static int getBlockFormItemId(Block block) {
        ensureVanillaBlockMapInitialized();
        return DEEPSLATE_FORMS.contains(block) ? 0 : getBlockId(block);
    }

    // Double-checked: getBlockId runs per block read on the server thread, the render thread and the
    // chunk-mesh threads; only the first call needs the lock (RE4).
    private static void ensureVanillaBlockMapInitialized() {
        if (!vanillaBlockMapInitialized) {
            initVanillaBlockMapOnce();
        }
    }

    private static synchronized void initVanillaBlockMapOnce() {
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
        // FCMOD-INTEGRATION(1.20.1): modern_wood_subblocks: the four newer planks are FC planks 5:5-8 (FC wood types 5-8,
        // ModernWoods), with FC's planks rule and saw, while their sub-blocks exist (ModernWoodSubBlocks.withholdsPlanksIdentity)
        vanillaBlockToLegacyId.put(Blocks.ACACIA_PLANKS, 5);
        vanillaBlockToLegacyId.put(Blocks.DARK_OAK_PLANKS, 5);
        vanillaBlockToLegacyId.put(Blocks.MANGROVE_PLANKS, 5);
        vanillaBlockToLegacyId.put(Blocks.CHERRY_PLANKS, 5);
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
        // FCMOD-INTEGRATION(1.20.1): modern_woods: ID 165 (free in 1.5.2): log2, the port's second FCBlockLog
        // (ModernWoods; FC wood types 5-8). Acacia first: the canonical pick. Identity only once log2 is built
        // (getBlockId); the mapping alone keeps the stand-ins and twins off 165 (isLegacyIdMapped).
        vanillaBlockToLegacyId.put(Blocks.ACACIA_LOG, ModernWoods.LOG2_ID);
        vanillaBlockToLegacyId.put(Blocks.DARK_OAK_LOG, ModernWoods.LOG2_ID);
        vanillaBlockToLegacyId.put(Blocks.MANGROVE_LOG, ModernWoods.LOG2_ID);
        vanillaBlockToLegacyId.put(Blocks.CHERRY_LOG, ModernWoods.LOG2_ID);
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
        vanillaBlockToLegacyId.put(Blocks.CUT_SANDSTONE, 24);       // 1.5.2 "smooth" sandstone (24:2)
        vanillaBlockToLegacyId.put(Blocks.SMOOTH_SANDSTONE, 24);
        // ID 25: Note Block
        vanillaBlockToLegacyId.put(Blocks.NOTE_BLOCK, 25);
        // ID 26: Bed (red first: the 1.5.2 bed, and item 355 -> RED_BED)
        vanillaBlockToLegacyId.put(Blocks.RED_BED, 26);
        vanillaBlockToLegacyId.put(Blocks.WHITE_BED, 26);
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
        // FCMOD-INTEGRATION(1.20.1): flower_subtypes: cornflower and lily of the valley are small flowers whose nearest 1.5.2 relative is the rose (post-1.5.2 plants by lore, ruling 2026-10-15): BTW's flower rules and the port-only subtypes 38:9 and 38:10. The wither rose (Nether soils) and the torchflower (the torchflower crop twin's last stage) stay unmapped
        vanillaBlockToLegacyId.put(Blocks.CORNFLOWER, 38);
        vanillaBlockToLegacyId.put(Blocks.LILY_OF_THE_VALLEY, 38);
        // ID 39: Brown Mushroom
        vanillaBlockToLegacyId.put(Blocks.BROWN_MUSHROOM, 39);
        // ID 40: Red Mushroom
        vanillaBlockToLegacyId.put(Blocks.RED_MUSHROOM, 40);
        // ID 41: Gold Block
        vanillaBlockToLegacyId.put(Blocks.GOLD_BLOCK, 41);
        // ID 42: Iron Block
        vanillaBlockToLegacyId.put(Blocks.IRON_BLOCK, 42);
        // ID 43: Double Stone Slab -- an MC slab with TYPE=DOUBLE: no table entry (identity routing keeps 44),
        // reported by getWorldBlockId and placed through getModernBlockVariant(43, meta).
        // ID 44: Stone Slab. The 1.5.2 stone slab (44:0) is 1.20.1's smooth stone slab (canonical pick,
        // as LegacyVariants); a double slab reads as 43 on the world-read path (getWorldBlockId).
        vanillaBlockToLegacyId.put(Blocks.SMOOTH_STONE_SLAB, 44);
        vanillaBlockToLegacyId.put(Blocks.STONE_SLAB, 44);
        vanillaBlockToLegacyId.put(Blocks.PETRIFIED_OAK_SLAB, 44);
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
        // ID 125: Double Wooden Slab -- like 43: getWorldBlockId / getModernBlockVariant(125, meta)
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

        // FCMOD-INTEGRATION(1.20.1): strata: deepslate, cobbled deepslate and the deepslate ores are BTW's deep-strata stone, cobblestone and ores; listed after the stone forms, so FC's writes of these ids keep placing the stone forms (the reverse map takes the first block listed)
        deepslateForm(Blocks.DEEPSLATE, 1);
        deepslateForm(Blocks.COBBLED_DEEPSLATE, 4);
        deepslateForm(Blocks.DEEPSLATE_GOLD_ORE, 14);
        deepslateForm(Blocks.DEEPSLATE_IRON_ORE, 15);
        deepslateForm(Blocks.DEEPSLATE_COAL_ORE, 16);
        deepslateForm(Blocks.DEEPSLATE_LAPIS_ORE, 21);
        deepslateForm(Blocks.DEEPSLATE_DIAMOND_ORE, 56);
        deepslateForm(Blocks.DEEPSLATE_REDSTONE_ORE, 73);
        deepslateForm(Blocks.DEEPSLATE_EMERALD_ORE, 129);

        // Build reverse mapping: legacy ID → modern Block (insertion order: the first block listed for an
        // id is its canonical pick -- oak, white wool, smooth stone slab, ...)
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
     * Returns the legacy MC 1.5.2 item ID for a modern item instance (routing: FoodDataMixin,
     * ItemStackMixin, LivingEntityMixin, ServerPlayerGameModeMixin index itemsList with it).
     * <ul>
     *   <li>ProxyItems (FC items) and the vanilla table answer their 1.5.2 id.</li>
     *   <li>Anything else answers 0 ("no FC item": vanilla behaviour). It used to answer the MC raw registry
     *       id, which indexes an unrelated FC item or an ItemBlock (itemsList[0..255] are block ids), so
     *       using a modern-only item could run another item's FC code (design_blockmap cross-cluster 1).
     *       Headless passthrough ids (&gt;= 25000) are not FC items either.</li>
     * </ul>
     */
    public static int getItemId(Item item) {
        if (item == null) return 0;
        // Check ProxyItem reverse map first (FC-specific items)
        Integer proxyId = proxyItemToLegacyId.get(item);
        if (proxyId != null && proxyId < HEADLESS_BASE_ID) return proxyId;
        // Check vanilla item map
        ensureVanillaItemMapInitialized();
        Integer id = vanillaItemToLegacyId.get(item);
        if (id != null) return id;
        // Flattened 1.5.2 subtypes (bone meal / dyes 351, charcoal 263, enchanted golden apple 322, skulls 397)
        try {
            int[] legacy = LegacyVariants.toLegacy(item);
            if (legacy != null) return legacy[0];
        } catch (Throwable ignored) {
            // LegacyVariants logs its own init failure
        }
        return 0;
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

    /** First headless id: clear of every FC item id (which max out around ~2000). */
    static final int HEADLESS_BASE_ID = 25000;

    /**
     * Next available ID for headless items. Guarded by the ProxyRegistry class lock (createHeadlessItem,
     * restoreHeadlessIds, resetHeadlessIds).
     */
    private static int nextHeadlessId = HEADLESS_BASE_ID;

    /**
     * Headless id -> item registry name for every headless id of the running world (triage RE5). FC NBT stores
     * the numeric id (Modern-Common ItemStack.writeToNBT), and headless ids are allocated in first-seen order,
     * so FcWorldSavedData persists this table per world and restores it before any chunk loads; ids whose
     * item no longer exists stay reserved so a new item can never inherit their saved stacks.
     */
    private static final Map<Integer, String> headlessNames = new ConcurrentHashMap<>();
    private static volatile boolean headlessOverflowLogged;

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
     * @return a positive legacy ID guaranteed to have an entry in itemsList (0 only if the headless id
     *         space is exhausted)
     */
    public static int getOrCreateLegacyId(Item modernItem) {
        if (modernItem == null) return 0;

        // 1.5.2 item ids first: in 1.20.1 wheat seeds, string, redstone, the cauldron/brewing stand/sign/door
        // items, ... are BlockItems of their crop/wire/block, but 1.5.2 kept separate item ids (seeds 295 vs
        // crops 59, string 287 vs tripwire 132, redstone 331 vs wire 55, cauldron 380 vs 118; vanilla/client
        // Item.java:117/109/212/325), and FC item checks (isBreedingItem(Item.seeds), ...) compare those.
        ensureVanillaItemMapInitialized();
        Integer vanillaId = vanillaItemToLegacyId.get(modernItem);
        if (vanillaId != null && vanillaId > 0) {
            boolean hasItem = vanillaId < btw.modern.Item.itemsList.length
                    && btw.modern.Item.itemsList[vanillaId] != null;
            boolean hasBlock = vanillaId < btw.modern.Block.blocksList.length
                    && btw.modern.Block.blocksList[vanillaId] != null;
            if (hasItem || hasBlock) return vanillaId;
        }

        // Then the block form (getBlockId already filters ids with an empty blocksList slot)
        if (modernItem instanceof net.minecraft.world.item.BlockItem bi) {
            // FCMOD-INTEGRATION(1.20.1): strata: a deepslate form's item keeps its own (headless) identity
            int blockId = getBlockFormItemId(bi.getBlock());
            if (blockId > 0) {
                return blockId;
            }
        }

        // Check existing item mapping (ProxyItems, headless ids)
        Integer existing = proxyItemToLegacyId.get(modernItem);
        if (existing != null && existing > 0) {
            // Verify it has an FC entry
            if (existing < btw.modern.Item.itemsList.length
                    && btw.modern.Item.itemsList[existing] != null) {
                return existing;
            }
        }

        // No valid FC entry — create a headless passthrough item
        return createHeadlessItem(modernItem);
    }

    private static synchronized int createHeadlessItem(Item modernItem) {
        // Double-check in case another thread raced
        Integer raceCheck = proxyItemToLegacyId.get(modernItem);
        if (raceCheck != null && raceCheck >= HEADLESS_BASE_ID) return raceCheck;

        // Skip ids a restored world table reserved for items that no longer exist
        int id = nextHeadlessId;
        while (id < btw.modern.Item.itemsList.length
                && (headlessNames.containsKey(id) || btw.modern.Item.itemsList[id] != null)) {
            id++;
        }
        if (id >= btw.modern.Item.itemsList.length) {
            if (!headlessOverflowLogged) {
                headlessOverflowLogged = true;
                LOGGER.error("Headless item ids exhausted ({}..{}); modern item {} has no FC form",
                        HEADLESS_BASE_ID, btw.modern.Item.itemsList.length - 1, registryName(modernItem));
            }
            return 0;
        }
        nextHeadlessId = id + 1;
        newHeadless(id, modernItem);

        // Register both directions
        itemsByLegacyId.put(id, modernItem);
        proxyItemToLegacyId.put(modernItem, id);
        String name = registryName(modernItem);
        if (name != null) headlessNames.put(id, name);

        LOGGER.debug("Created headless FC item ID {} for modern item {}", id, name);
        return id;
    }

    /**
     * Builds (and, through the FC Item constructor, installs at {@code itemsList[id]}) the FC item behind headless id
     * {@code id} of {@code modernItem}; the one factory of createHeadlessItem and restoreHeadlessIds.
     * <ul>
     *   <li>An edible item, or one with a btw_foods hunger value: a {@link btw.forge.food.ModernFoodItem} (an FC
     *       ItemFood carrying the item's FC food value), stacking to min(its 1.20.1 size, 16) like 4.B ItemFood.</li>
     *   <li>An item the table gives only fodder values: a {@link btw.forge.food.ModernFodderItem}.</li>
     *   <li>Anything else: a plain FC Item with the 1.20.1 stack size, as before.</li>
     * </ul>
     * The class is fixed when the id is created: a /reload that adds a table value for a non-edible item applies to its
     * headless id only from the next world load (resetHeadlessIds clears every id when the server stops).
     * FCMOD-INTEGRATION(1.20.1): food_values: FC code that knows items only by id (wolves, animals, the cauldron) sees
     * modern foods and fodder as FC food (INTEGRATIONS #024).
     */
    private static btw.modern.Item newHeadless(int id, Item modernItem) {
        // Item constructor does: this.itemID = 256 + rawId; itemsList[itemID] = this; so rawId = id - 256
        try {
            btw.forge.food.BtwFoodTable.Entry entry = btw.forge.food.BtwFoodTable.get(modernItem);
            if (modernItem.isEdible() || (entry != null && entry.hunger() != null)) {
                btw.modern.Item food = new btw.forge.food.ModernFoodItem(id, modernItem);
                food.setMaxStackSize(Math.min(modernItem.getMaxStackSize(), 16));
                return food;
            }
            if (entry != null && entry.fodder() != null) {
                btw.modern.Item fodder = new btw.forge.food.ModernFodderItem(id, modernItem);
                fodder.setMaxStackSize(modernItem.getMaxStackSize());
                return fodder;
            }
        } catch (Throwable t) {
            LOGGER.warn("Headless FC food item {} for {} failed; a plain FC item stands in: {}",
                    id, registryName(modernItem), t.toString());
        }
        btw.modern.Item headless = new btw.modern.Item(id - 256);
        headless.setMaxStackSize(modernItem.getMaxStackSize());
        return headless;
    }

    /** Registry name of a modern item, or null if it is not registered. */
    private static String registryName(Item item) {
        ResourceLocation key = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item);
        return key != null ? key.toString() : null;
    }

    /**
     * RE5: every headless id known this session with its item's registry name (restored reservations
     * included), for FcWorldSavedData to persist with the world.
     */
    static synchronized Map<Integer, String> snapshotHeadlessIds() {
        return new TreeMap<>(headlessNames);
    }

    /**
     * RE5: re-creates the headless ids a world saved (id -> item registry name) so the FC stacks stored with
     * them load as the same item. Must run before the world's chunks load (FcWorldSavedData hooks the
     * overworld's LevelEvent.Load). Saved ids are always reserved; an item that no longer exists leaves its
     * id empty (its stacks load as nothing instead of as some other item); an item that has a real legacy
     * id by now keeps it for new stacks while its old headless id still loads as it. The world's numbering
     * always wins over an id this session handed out before the world loaded (see the first pass).
     */
    static synchronized void restoreHeadlessIds(Map<Integer, String> saved) {
        if (saved == null || saved.isEmpty()) return;
        int restored = 0, missing = 0, rehomed = 0;
        // Pass 1: an id this session already gave to a DIFFERENT item, before the world loaded (a client-side
        // conversion while connected to another server, any conversion between the previous world's reset and
        // this load). That allocation is in no NBT of this world, whereas this world's FC stacks hold the saved
        // id: keeping the session item loaded every such stack as it, and the next save made the swap
        // permanent (w2 review BM-R2). Evict it; its next conversion re-homes it at a fresh id
        // (getOrCreateLegacyId -> createHeadlessItem, past every saved id), unless pass 2 restores it at its own
        // saved id. Only FC stacks created this session, before the load, still carry the evicted id.
        for (Map.Entry<Integer, String> e : saved.entrySet()) {
            Integer boxedId = e.getKey();
            String name = e.getValue();
            if (boxedId == null || name == null) continue;
            int id = boxedId;
            if (id < HEADLESS_BASE_ID || id >= btw.modern.Item.itemsList.length) continue;
            String known = headlessNames.get(id);
            if (name.equals(known) || (known == null && btw.modern.Item.itemsList[id] == null)) continue;
            Item live = itemsByLegacyId.remove(id);
            if (live != null) proxyItemToLegacyId.remove(live, id);
            headlessNames.remove(id);
            btw.modern.Item.itemsList[id] = null;
            rehomed++;
            LOGGER.warn("[HEADLESS] saved id {} ({}) was already given to {} in this session; the world's numbering"
                    + " wins and {} moves to a new id", id, name, known, known);
        }
        // Pass 2: install the saved ids.
        for (Map.Entry<Integer, String> e : saved.entrySet()) {
            Integer boxedId = e.getKey();
            String name = e.getValue();
            if (boxedId == null || name == null) continue;
            int id = boxedId;
            if (id < HEADLESS_BASE_ID || id >= btw.modern.Item.itemsList.length) continue;
            nextHeadlessId = Math.max(nextHeadlessId, id + 1);
            if (headlessNames.containsKey(id) || btw.modern.Item.itemsList[id] != null) {
                continue; // the same item already holds this id this session (pass 1 evicted any other)
            }
            headlessNames.put(id, name);
            ResourceLocation rl = ResourceLocation.tryParse(name);
            Item item = rl != null && net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(rl)
                    ? net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(rl) : null;
            if (item == null || item == Items.AIR) {
                missing++;
                continue;
            }
            newHeadless(id, item);
            itemsByLegacyId.put(id, item);
            if (!proxyItemToLegacyId.containsKey(item) && !hasRealLegacyId(item)) {
                proxyItemToLegacyId.put(item, id);
            }
            restored++;
        }
        LOGGER.info("[HEADLESS] restored {} headless item ids ({} items no longer exist, {} session items re-homed);"
                + " next id {}", restored, missing, rehomed, nextHeadlessId);
    }

    /**
     * RE5: forgets the running world's headless ids once its server has stopped, so the next world opened in
     * this JVM restores exactly its own table (two worlds may have numbered the same items differently).
     */
    static synchronized void resetHeadlessIds() {
        for (int id = HEADLESS_BASE_ID; id < nextHeadlessId && id < btw.modern.Item.itemsList.length; id++) {
            btw.modern.Item.itemsList[id] = null;
            itemsByLegacyId.remove(id);
        }
        proxyItemToLegacyId.values().removeIf(v -> v >= HEADLESS_BASE_ID);
        headlessNames.clear();
        nextHeadlessId = HEADLESS_BASE_ID;
    }

    /** True if the item has a non-headless legacy id (vanilla table, block form, or ProxyItem). */
    private static boolean hasRealLegacyId(Item item) {
        ensureVanillaItemMapInitialized();
        if (vanillaItemToLegacyId.containsKey(item)) return true;
        // FCMOD-INTEGRATION(1.20.1): strata: a deepslate form's item has no real legacy id (restoreHeadlessIds keeps mapping its saved headless id)
        if (item instanceof net.minecraft.world.item.BlockItem bi && getBlockFormItemId(bi.getBlock()) > 0) return true;
        Integer proxyId = proxyItemToLegacyId.get(item);
        return proxyId != null && proxyId < HEADLESS_BASE_ID;
    }

    // Double-checked like ensureVanillaBlockMapInitialized (RE4).
    private static void ensureVanillaItemMapInitialized() {
        if (!vanillaItemMapInitialized) {
            initVanillaItemMapOnce();
        }
    }

    private static synchronized void initVanillaItemMapOnce() {
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
        // 383: Spawn Egg — every modern egg of a 1.5.2 mob maps to the single 1.5.2
        // monsterPlacer id so EntityAgeable.interact's held.itemID ==
        // Item.monsterPlacer.itemID (383) baby-spawn check can match. Eggs of mobs 1.5.2 never had
        // (fox, axolotl, modded) stay headless: as (383, 0) they had no FC -> MC form
        // (LegacyVariants.eggFor(0) and getModernItem(383) are null), so one put in an FC
        // container could never be taken out again.
        for (net.minecraft.world.item.Item eggItem : net.minecraftforge.registries.ForgeRegistries.ITEMS) {
            if (eggItem instanceof net.minecraft.world.item.SpawnEggItem egg && isLegacyMobEgg(egg)) {
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

    /** True for a vanilla egg whose mob existed in 1.5.2 (a key of ItemStackHelper.LEGACY_ENTITY_NAMES). */
    private static boolean isLegacyMobEgg(net.minecraft.world.item.SpawnEggItem egg) {
        try {
            net.minecraft.world.entity.EntityType<?> type = egg.getType(null);
            ResourceLocation key = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(type);
            return key != null && "minecraft".equals(key.getNamespace())
                    && ItemStackHelper.LEGACY_ENTITY_NAMES.containsKey(key.getPath());
        } catch (Throwable t) {
            return false;
        }
    }
}
