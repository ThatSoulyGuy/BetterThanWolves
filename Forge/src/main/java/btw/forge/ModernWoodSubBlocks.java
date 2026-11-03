package btw.forge;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.crafting.PartialNBTIngredient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * FCMOD-INTEGRATION(1.20.1): modern_wood_subblocks
 *
 * <p><b>Feature:</b> BTW's wood sub-blocks for acacia, dark oak, mangrove and cherry (FC wood types 5-8,
 * {@link ModernWoods}): sidings, mouldings, corners, benches, fences, columns, pedestals and tables of each wood, its
 * planks under FC's planks rule and saw, and 4.B's stair and slab rules for its 1.20.1 stairs and slabs.</p>
 *
 * <p><b>Conflict with FC:</b> the four planks had FC identity at item level only, so FC's planks rule (iron-axe harvest,
 * sawdust on a bad break, Client FCBlockPlanks.java:35-87) and its saw output never reached them. Every FC site that
 * turns a wood type into a block, name or texture answers blood wood or oak for 5-8: the five stub items' placement and
 * names (Common FCItemBlockWoodSidingStub.java:31-86 and its four siblings), the two sub-block classes' wood type
 * (FCBlockWoodSidingAndCornerAndDecorative.GetWoodTypeFromBlockID :207-233, FCBlockWoodMouldingAndDecorative
 * .damageDropped :56-102), three item renderers, and FC's sub-block recipe loop (FCRecipes.java:4944-5032, wood types
 * 0-4). Their 1.20.1 stairs and slabs have no legacy id, so 4.B's stair, slab and saw rules (FCRecipes.java:877-901,
 * :2396-2420, :2923-2929; FCBlockSaw.java:640-719) missed them.</p>
 *
 * <p><b>Integration pattern:</b> D (FC-extend). FC's own sub-block classes at four new pairs of ids, built as FC builds
 * blood wood's (FCBetterThanWolves.java:1117-1122, :1821-1822): siding-and-corner 1093 / 1095 / 1097 / 1099 and
 * moulding-and-decorative 1094 / 1096 / 1098 / 1100, with FC's own items. Six load-time patches keyed on those ids
 * ({@code btw.forge.mixin.fcext}, W1-W6) teach the stubs and the two classes wood types 5-8. FC's sub-block recipe loop
 * runs for 5-8 through FC's own AddRecipe; 4.B's stair and slab rules reach the 1.20.1 stairs and slabs as 1.20.1-side
 * recipes ({@link #contributeRecipes}) and an FC machine hook ({@link ModernWoodSawHooks}).</p>
 *
 * <p><b>Rebalance:</b> none: FC's costs and outputs for every wood. FC's planks -&gt; six fences row is not added for
 * 5-8 (maintainer ruling 7.2 (b)): it would take the grid of the four 1.20.1 trapdoors, which stay craftable; the
 * newer woods' BTW fences come from three mouldings, as 4.B also allows.</p>
 *
 * <p><b>Injection strategy:</b> reflective construction (the Forge source set cannot name FC classes) in
 * BTWLifecycle step 5f, after log2 and before {@link BTWLifecycle#isFcReady()}, so registration, the bake and the
 * client icons see the eight blocks as FC blocks ({@code betterthanwolves:block_1093..1100}). FC's 32 recipes are
 * appended at common setup after the modern-dye twins ({@link #addFcRecipes}), so no {@code fc_recipe_<n>} that has
 * shipped moves. The four planks answer 5 in {@link ProxyRegistry#getBlockId} only while the sub-blocks exist
 * ({@link #withholdsPlanksIdentity}), so "planks identity" and "sub-blocks" are one feature, on or off together.</p>
 *
 * <p><b>Alternative considered and rejected:</b> FC stairs blocks at new ids for the four woods (duplicate stairs
 * blocks); species slabs as subtypes of FC's wood slab 126 (three species bits; cherry is 8); planks identity without
 * the sub-blocks (5-8 stubs would place blood wood blocks); {@code @ModifyVariable} on FC's inline wood-type chain;
 * adding the 32 recipes at FC init (it renumbered the modern-dye twins' shipped ids).</p>
 *
 * <p>Ids 1093-1100 must never change: FC NBT stores numeric ids, the registry names are {@code block_<id>}, and client
 * and server must agree. If one is taken at start-up none of this turns on (one ERROR, one audit WARN).</p>
 */
// FCMOD-INTEGRATION(1.20.1): modern_wood_subblocks: the newer woods (FC wood types 5-8) get FC's sidings, mouldings, corners and decorative blocks at 1093-1100, FC's planks rule and 4.B's stair and slab rules
public final class ModernWoodSubBlocks {

    private static final Logger LOGGER = LogManager.getLogger("BTW-Wood");

    /** Siding-and-corner blocks of wood types 5-8 (FC's siding id; the moulding is the next id, as blood's 1007/1008). */
    static final int[] SIDING_IDS = {1093, 1095, 1097, 1099};
    /** Moulding-and-decorative blocks of wood types 5-8. */
    static final int[] MOULDING_IDS = {1094, 1096, 1098, 1100};
    /** The first and last reserved id. */
    private static final int FIRST_ID = 1093, LAST_ID = 1100;
    /** FC name stems of the four woods: fcWood&lt;X&gt;Siding / fcWood&lt;X&gt;Moulding / fcBlockColumnWood&lt;X&gt;_side. */
    private static final String[] CAMEL = {"Acacia", "DarkOak", "Mangrove", "Cherry"};

    /** FC's decorative block types (FCItemBlockWoodMouldingDecorativeStub / FCItemBlockWoodSidingDecorativeStub m_iType*). */
    static final int COLUMN = 0, PEDESTAL = 1, TABLE = 2, BENCH = 0, FENCE = 1;
    /** FCBlockMouldingAndDecorative.m_iSubtypeColumn / m_iSubtypePedestalUp / m_iSubtypePedestalDown (table is 15). */
    private static final int SUBTYPE_COLUMN = 12, SUBTYPE_PEDESTAL_UP = 13, SUBTYPE_PEDESTAL_DOWN = 14;
    /** FCBlockSidingAndCornerAndDecorative.m_iSubtypeBench / m_iSubtypeFence. */
    private static final int SUBTYPE_BENCH = 12, SUBTYPE_FENCE = 14;

    private static final String FC_CORE = "net.minecraft.src.btw.core.FCBetterThanWolves";
    private static final String FC_SIDING = "net.minecraft.src.btw.block.FCBlockWoodSidingAndCornerAndDecorative";
    private static final String FC_MOULDING = "net.minecraft.src.btw.block.FCBlockWoodMouldingAndDecorative";
    private static final String FC_SIDING_ITEM = "net.minecraft.src.btw.item.FCItemBlockSidingAndCorner";
    private static final String FC_MOULDING_ITEM = "net.minecraft.src.btw.item.FCItemBlockMoulding";
    private static final String FC_RECIPES = "net.minecraft.src.btw.crafting.FCRecipes";

    /** True once the eight blocks and their items are in FC's tables. Written last in {@link #create}. */
    private static volatile boolean built;
    /** FC's five wood stub item ids (FCBetterThanWolves.java:1824-1828: 196-200 by default); -1 = unresolved. */
    private static volatile int sidingStubId = -1, mouldingStubId = -1, cornerStubId = -1, mouldingDecoStubId = -1,
            sidingDecoStubId = -1;
    /** True once {@link #addFcRecipes} ran (whatever it decided). */
    private static boolean recipesAdded;

    private ModernWoodSubBlocks() {}

    // ================================================================
    // Construction (BTWLifecycle step 5f)
    // ================================================================

    /**
     * Builds the eight sub-blocks of wood types 5-8 and their items with FC's own classes, as FC builds blood wood's.
     * Runs once, after FC's own table, the twins and log2, before {@link BTWLifecycle#isFcReady()}. All or nothing,
     * never throws: if an id is taken or a constructor fails, one ERROR and nothing is built (the four planks then keep
     * their 1.20.1 identity).
     */
    static void create() {
        if (built) return;
        btw.modern.Block[] blocks = btw.modern.Block.blocksList;
        btw.modern.Item[] items = btw.modern.Item.itemsList;
        for (int s = 0; s < CAMEL.length; s++) {                        // every id free, or nothing is built
            for (int id : new int[] {SIDING_IDS[s], MOULDING_IDS[s]}) {
                if (blocks[id] != null || items[id] != null || ProxyRegistry.getProxy(id) != null) {
                    LOGGER.error("[WOOD] legacy id {} is taken (block {}, item {}): the newer woods get no sidings,"
                            + " mouldings or corners, and their planks stay plain 1.20.1 planks", id,
                            className(blocks[id]), className(items[id]));
                    return;
                }
            }
        }
        int[] stubs = new int[5];
        try {
            stubs[0] = fcInt("fcBlockWoodSidingItemStubID");               // 196 (FCBetterThanWolves.java:1824-1828)
            stubs[1] = fcInt("fcBlockWoodMouldingItemStubID");             // 197
            stubs[2] = fcInt("fcBlockWoodCornerItemStubID");               // 198
            stubs[3] = fcInt("fcBlockWoodMouldingDecorativeItemStubID");   // 199
            stubs[4] = fcInt("fcBlockWoodSidingDecorativeItemStubID");     // 200
            for (int stub : stubs) {
                if (stub <= 0) throw new IllegalStateException("an FC wood stub id is unresolved: " + stub);
            }
            Constructor<?> siding = Class.forName(FC_SIDING).getConstructor(int.class, String.class, String.class);
            Constructor<?> moulding = Class.forName(FC_MOULDING)
                    .getConstructor(int.class, String.class, String.class, int.class, String.class);
            Constructor<?> sidingItem = Class.forName(FC_SIDING_ITEM).getConstructor(int.class);
            Constructor<?> mouldingItem = Class.forName(FC_MOULDING_ITEM).getConstructor(int.class);
            for (int s = 0; s < CAMEL.length; s++) {
                int sid = SIDING_IDS[s], mid = MOULDING_IDS[s];
                String planks = ModernWoods.textureName(ModernWoods.FIRST_WOOD_TYPE + s) + "_planks"; // "acacia_planks"
                siding.newInstance(sid, planks, "fcWood" + CAMEL[s] + "Siding");                     // as :1117-1118
                moulding.newInstance(mid, planks, "fcBlockColumnWood" + CAMEL[s] + "_side", sid,
                        "fcWood" + CAMEL[s] + "Moulding");                                              // as :1120-1122
                // FC's items for blood's pair (:1821-1822); the Block constructor left a placeholder ItemBlock there
                items[sid] = (btw.modern.Item) sidingItem.newInstance(sid - 256);
                items[mid] = (btw.modern.Item) mouldingItem.newInstance(mid - 256);
            }
        } catch (Throwable t) {
            // as ModernWoods.create: the FC constructors may have registered themselves before failing; free every id
            for (int s = 0; s < CAMEL.length; s++) {
                for (int id : new int[] {SIDING_IDS[s], MOULDING_IDS[s]}) {
                    blocks[id] = null;
                    items[id] = null;
                }
            }
            LOGGER.error("[WOOD] could not build the newer woods' sub-blocks; their planks stay plain 1.20.1 planks",
                    cause(t));
            return;
        }
        sidingStubId = stubs[0];
        mouldingStubId = stubs[1];
        cornerStubId = stubs[2];
        mouldingDecoStubId = stubs[3];
        sidingDecoStubId = stubs[4];
        built = true;                                                     // volatile, written last
        LOGGER.info("[WOOD] sub-blocks of wood types 5-8 at {}-{} ({} / {} with {} / {})", FIRST_ID, LAST_ID,
                simpleName(FC_SIDING), simpleName(FC_MOULDING), simpleName(FC_SIDING_ITEM),
                simpleName(FC_MOULDING_ITEM));
    }

    // ================================================================
    // Lookups (pure after create; both sides; "not mine" while not built)
    // ================================================================

    /** True once the eight sub-blocks exist. */
    public static boolean isBuilt() {
        return built;
    }

    /** FC wood type 5-8 of block {@code blockId} (1093-1100), -1 for any other id or when not built. */
    public static int woodTypeOfBlock(int blockId) {
        if (!built || blockId < FIRST_ID || blockId > LAST_ID) return -1;
        return ModernWoods.FIRST_WOOD_TYPE + (blockId - FIRST_ID) / 2;
    }

    /** Siding-and-corner block id of wood type 5-8, -1 otherwise. */
    public static int sidingId(int woodType) {
        return ModernWoods.isWoodType(woodType) ? SIDING_IDS[woodType - ModernWoods.FIRST_WOOD_TYPE] : -1;
    }

    /** Moulding-and-decorative block id of wood type 5-8, -1 otherwise. */
    public static int mouldingId(int woodType) {
        return ModernWoods.isWoodType(woodType) ? MOULDING_IDS[woodType - ModernWoods.FIRST_WOOD_TYPE] : -1;
    }

    /** FC's decorative stub damage (FCItemBlockWood*DecorativeStub.GetItemDamageForType, Client :144-149). */
    public static int decorativeDamage(int woodType, int type) {
        return (woodType & 3) | ((woodType >> 2) << 4) | (type << 2);
    }

    /** FC's decorative stub wood type (GetWoodType, Client :130-135). */
    public static int decorativeWoodType(int damage) {
        return (damage & 3) | ((damage >> 4) << 2);
    }

    /** FC's decorative stub block type (GetBlockType, Client :137-142). */
    public static int decorativeType(int damage) {
        return (damage >> 2) & 3;
    }

    /**
     * FCBlockWoodMouldingAndDecorative.damageDropped (Client :56-102) for wood type {@code woodType}: the moulding's
     * wood type, or the decorative stub damage of a column (12), pedestal (13, 14) or table (15).
     */
    public static int mouldingDamageDropped(int woodType, int metadata) {
        if (metadata < SUBTYPE_COLUMN) return woodType;               // FCBlockMouldingAndDecorative.IsDecorative
        int type = metadata == SUBTYPE_COLUMN ? COLUMN
                : metadata == SUBTYPE_PEDESTAL_UP || metadata == SUBTYPE_PEDESTAL_DOWN ? PEDESTAL : TABLE;
        return decorativeDamage(woodType, type);
    }

    /**
     * The block FC's wood stub {@code stubItemId} places for {@code damage} when it is of wood type 5-8 (its own
     * siding-and-corner or moulding-and-decorative block), else 0 (FC's own answer stands).
     */
    public static int stubBlockToPlace(int stubItemId, int damage) {
        if (!built) return 0;
        if (stubItemId == sidingStubId || stubItemId == cornerStubId) return Math.max(0, sidingId(damage));
        if (stubItemId == mouldingStubId) return Math.max(0, mouldingId(damage));
        if (stubItemId == mouldingDecoStubId) return Math.max(0, mouldingId(decorativeWoodType(damage)));
        if (stubItemId == sidingDecoStubId) return Math.max(0, sidingId(decorativeWoodType(damage)));
        return 0;
    }

    /**
     * The name suffix of FC's wood stub {@code stubItemId} for {@code damage} when it is of wood type 5-8:
     * ".&lt;wood&gt;" for a siding, moulding or corner, ".&lt;wood&gt;.&lt;column|pedestal|table&gt;" and
     * ".&lt;wood&gt;.&lt;bench|fence&gt;" for the decorative stubs (by FC's GetBlockType); null otherwise.
     */
    public static String stubNameSuffix(int stubItemId, int damage) {
        if (!built) return null;
        if (stubItemId == sidingStubId || stubItemId == mouldingStubId || stubItemId == cornerStubId) {
            String wood = ModernWoods.barkName(damage);
            return wood != null ? "." + wood : null;
        }
        if (stubItemId == mouldingDecoStubId || stubItemId == sidingDecoStubId) {
            String wood = ModernWoods.barkName(decorativeWoodType(damage));
            if (wood == null) return null;
            int type = decorativeType(damage);
            String kind = stubItemId == mouldingDecoStubId
                    ? (type == COLUMN ? "column" : type == PEDESTAL ? "pedestal" : "table")
                    : (type == BENCH ? "bench" : "fence");
            return "." + wood + "." + kind;
        }
        return null;
    }

    /**
     * Client: the icon the siding, corner or moulding stub (drawn by its block {@code stubBlockId}, 196 / 198 / 197)
     * of wood type {@code damage} 5-8 is drawn with: its own block's planks; null otherwise or before the icons exist.
     */
    public static btw.modern.Icon stubIcon(int stubBlockId, int damage) {
        if (!built || !ModernWoods.isWoodType(damage)) return null;
        int id = stubBlockId == sidingStubId || stubBlockId == cornerStubId ? sidingId(damage)
                : stubBlockId == mouldingStubId ? mouldingId(damage) : -1;
        btw.modern.Block block = id > 0 ? btw.modern.Block.blocksList[id] : null;
        return block != null ? block.blockIcon : null;
    }

    /**
     * Client: the block a decorative stub (drawn by its block {@code stubBlockId}, 200 bench/fence or 199
     * column/pedestal/table) of wood type 5-8 is drawn as; null otherwise.
     */
    public static btw.modern.Block decorativeRenderBlock(int stubBlockId, int damage) {
        int woodType = decorativeWoodType(damage);
        if (!built || !ModernWoods.isWoodType(woodType)) return null;
        int id = stubBlockId == sidingDecoStubId ? sidingId(woodType)
                : stubBlockId == mouldingDecoStubId ? mouldingId(woodType) : -1;
        return id > 0 ? btw.modern.Block.blocksList[id] : null;
    }

    /** {@code n} siding stubs of wood type {@code woodType} as a 1.20.1 stack; EMPTY when not built. */
    public static ItemStack sidingStack(int woodType, int n) {
        return built ? ItemStackHelper.fcIdToMcStack(sidingStubId, n, woodType) : ItemStack.EMPTY;
    }

    /** {@code n} moulding stubs of wood type {@code woodType} as a 1.20.1 stack; EMPTY when not built. */
    public static ItemStack mouldingStack(int woodType, int n) {
        return built ? ItemStackHelper.fcIdToMcStack(mouldingStubId, n, woodType) : ItemStack.EMPTY;
    }

    /**
     * True for the four newer planks while their sub-blocks are not built (getBlockId then answers 0, as before). On
     * getBlockId's hot path, only for id 5: {@code built} is read first.
     */
    static boolean withholdsPlanksIdentity(Block block) {
        return !built && (block == Blocks.ACACIA_PLANKS || block == Blocks.DARK_OAK_PLANKS
                || block == Blocks.MANGROVE_PLANKS || block == Blocks.CHERRY_PLANKS);
    }

    /** FC wood type 5-8 of the newer woods' 1.20.1 stairs and slabs, -1 for any other block or when not built. */
    public static int stairsOrSlabWoodType(Block block) {
        if (!built || block == null) return -1;
        if (block == Blocks.ACACIA_STAIRS || block == Blocks.ACACIA_SLAB) return ModernWoods.ACACIA;
        if (block == Blocks.DARK_OAK_STAIRS || block == Blocks.DARK_OAK_SLAB) return ModernWoods.DARK_OAK;
        if (block == Blocks.MANGROVE_STAIRS || block == Blocks.MANGROVE_SLAB) return ModernWoods.MANGROVE;
        if (block == Blocks.CHERRY_STAIRS || block == Blocks.CHERRY_SLAB) return ModernWoods.CHERRY;
        return -1;
    }

    // ================================================================
    // FC's sub-block recipes for 5-8 (BTWForgeMod.commonSetup, after the modern-dye twins)
    // ================================================================

    /** One FC recipe row: shaped or shapeless, its result and FC's argument array. */
    private record Row(boolean shapeless, btw.modern.ItemStack result, Object[] args) {}

    /**
     * FC's sub-block recipe loop (Client FCRecipes.java:4948-5031) for wood types 5-8, through FC's own AddRecipe /
     * AddShapelessRecipe: pedestal, column, table, bench, fence from three mouldings, and the three combine rows; FC's
     * planks -&gt; six fences row is left out (ruling 7.2 (b)). 32 recipes, appended after every recipe that has
     * already shipped (the modern-dye twins included), so no {@code fc_recipe_<n>} id moves. Built first, then appended
     * all or nothing (an exact rollback on a throw). Runs once, on both physical sides, before RecipeBridge
     * (ServerStarting) and JEI read FC's list. Never throws.
     */
    // FCMOD-INTEGRATION(1.20.1): modern_wood_subblocks: FC's sub-block recipes for wood types 5-8, appended after every
    // recipe that has already shipped (the modern-dye twins included), so no fc_recipe_<n> id moves
    @SuppressWarnings("rawtypes")
    static synchronized void addFcRecipes() {
        if (recipesAdded) return;
        recipesAdded = true;
        if (!built) {
            LOGGER.warn("[WOOD] the newer woods' sub-blocks were not built (see the [WOOD] ERROR): no FC recipes for them");
            return;
        }
        List<Row> rows = new ArrayList<>();
        Method shaped, shapeless;
        try {
            Class<?> fcRecipes = Class.forName(FC_RECIPES);
            shaped = fcRecipes.getMethod("AddRecipe", btw.modern.ItemStack.class, Object[].class);
            shapeless = fcRecipes.getMethod("AddShapelessRecipe", btw.modern.ItemStack.class, Object[].class);
            for (int w = ModernWoods.ACACIA; w <= ModernWoods.CHERRY; w++) {
                // :4950-4957 pedestal
                rows.add(new Row(false, fc(mouldingDecoStubId, 6, decorativeDamage(w, PEDESTAL)), new Object[] {
                        " S ", "###", "###", '#', fc(ModernWoods.PLANKS_ID, 1, w), 'S', fc(sidingStubId, 1, w)}));
                // :4959-4965 column
                rows.add(new Row(false, fc(mouldingDecoStubId, 1, decorativeDamage(w, COLUMN)), new Object[] {
                        "M", "M", "M", 'M', fc(mouldingStubId, 1, w)}));
                // :4967-4974 table
                rows.add(new Row(false, fc(mouldingDecoStubId, 4, decorativeDamage(w, TABLE)), new Object[] {
                        "###", " X ", " X ", '#', fc(sidingStubId, 1, w), 'X', fc(mouldingStubId, 1, w)}));
                // :4976-4982 bench
                rows.add(new Row(false, fc(sidingDecoStubId, 4, decorativeDamage(w, BENCH)), new Object[] {
                        "###", " X ", '#', fc(sidingStubId, 1, w), 'X', fc(mouldingStubId, 1, w)}));
                // :5008-5012 fence from three mouldings (:5001-5006, six planks -> six fences, is not added: 7.2 (b))
                rows.add(new Row(false, fc(sidingDecoStubId, 2, decorativeDamage(w, FENCE)), new Object[] {
                        "###", '#', fc(mouldingStubId, 1, w)}));
                // :5017-5030 the combine rows
                rows.add(new Row(true, fc(ModernWoods.PLANKS_ID, 1, w), new Object[] {
                        fc(sidingStubId, 1, w), fc(sidingStubId, 1, w)}));
                rows.add(new Row(true, fc(sidingStubId, 1, w), new Object[] {
                        fc(mouldingStubId, 1, w), fc(mouldingStubId, 1, w)}));
                rows.add(new Row(true, fc(mouldingStubId, 1, w), new Object[] {
                        fc(cornerStubId, 1, w), fc(cornerStubId, 1, w)}));
            }
        } catch (Throwable t) {
            LOGGER.error("[WOOD] the newer woods' FC recipes could not be prepared; none added", cause(t));
            return;
        }
        List list = btw.modern.CraftingManager.getInstance().getRecipeList();
        int before = list.size();
        try {
            for (Row r : rows) (r.shapeless() ? shapeless : shaped).invoke(null, r.result(), r.args());
        } catch (Throwable t) {
            list.subList(before, list.size()).clear();                 // all or nothing: no partial id range ever ships
            LOGGER.error("[WOOD] the newer woods' FC recipes could not be added; none added", cause(t));
            return;
        }
        LOGGER.info("[WOOD] FC recipes of wood types 5-8: fc_recipe_{}..fc_recipe_{} ({}), after the modern-dye twins",
                before, list.size() - 1, rows.size());
    }

    // ================================================================
    // 4.B's stair and slab rules as 1.20.1-side recipes (RecipeBridge.injectRecipes)
    // ================================================================

    /**
     * 4.B's stair and slab rules for the four newer woods' 1.20.1 stairs and slabs, in the recipe map RecipeBridge is
     * about to install: {@code minecraft:<x>_stairs} makes 6 (4.B's 6 -&gt; 6, which removed vanilla's 6 -&gt; 4,
     * FCRecipes.java:877-901, :5708-5734), replaced at its own id when it is still vanilla's (exactly a ShapedRecipe
     * making 4); three mouldings make one stairs ({@code betterthanwolves:modern_woods/<x>_stairs_from_mouldings},
     * :2396-2420, only while the sub-blocks exist; Forge's partial-NBT ingredient keeps the moulding's wood exact in
     * every grid); two slabs make one planks ({@code betterthanwolves:modern_woods/<x>_planks_from_slabs}, :2923-2929).
     * Returns {stairs replaced, recipes added, 0}.
     */
    // FCMOD-INTEGRATION(1.20.1): modern_wood_subblocks: 4.B's stair and slab rules for the four newer woods (FCRecipes.java:877-901, :2396-2420, :2923-2929)
    static int[] contributeRecipes(Map<ResourceLocation, Recipe<?>> all, RegistryAccess access) {
        Block[] stairs = {Blocks.ACACIA_STAIRS, Blocks.DARK_OAK_STAIRS, Blocks.MANGROVE_STAIRS, Blocks.CHERRY_STAIRS};
        Block[] slabs = {Blocks.ACACIA_SLAB, Blocks.DARK_OAK_SLAB, Blocks.MANGROVE_SLAB, Blocks.CHERRY_SLAB};
        Block[] planks = {Blocks.ACACIA_PLANKS, Blocks.DARK_OAK_PLANKS, Blocks.MANGROVE_PLANKS, Blocks.CHERRY_PLANKS};
        int replaced = 0, added = 0;
        for (int s = 0; s < stairs.length; s++) {
            int woodType = ModernWoods.FIRST_WOOD_TYPE + s;
            String wood = ModernWoods.textureName(woodType);
            // 1. 4.B's six planks -> six stairs, at the vanilla id (its recipe-book advancement keeps working)
            ResourceLocation stairsId = new ResourceLocation("minecraft", wood + "_stairs");
            if (all.get(stairsId) instanceof ShapedRecipe old && old.getClass() == ShapedRecipe.class) {
                ItemStack result = old.getResultItem(access);
                if (result.is(stairs[s].asItem()) && result.getCount() == 4) {
                    all.put(stairsId, new ShapedRecipe(stairsId, old.getGroup(), old.category(), old.getWidth(),
                            old.getHeight(), old.getIngredients(), new ItemStack(stairs[s], 6), old.showNotification()));
                    replaced++;
                }
            }
            // 2. three mouldings ("# " / "##") -> one stairs, only with the sub-blocks (the stub is theirs)
            if (built) {
                ItemStack stub = mouldingStack(woodType, 1);
                if (!stub.isEmpty() && stub.getTag() != null) {
                    Ingredient m = PartialNBTIngredient.of(stub.getItem(), stub.getTag());
                    ResourceLocation id = new ResourceLocation(BTWForgeMod.MOD_ID,
                            "modern_woods/" + wood + "_stairs_from_mouldings");
                    all.put(id, new ShapedRecipe(id, "", CraftingBookCategory.BUILDING, 2, 2,
                            NonNullList.of(Ingredient.EMPTY, m, Ingredient.EMPTY, m, m), new ItemStack(stairs[s], 1)));
                    added++;
                }
            }
            // 3. two slabs -> one planks
            ResourceLocation slabsId = new ResourceLocation(BTWForgeMod.MOD_ID, "modern_woods/" + wood + "_planks_from_slabs");
            all.put(slabsId, new ShapelessRecipe(slabsId, "", CraftingBookCategory.BUILDING, new ItemStack(planks[s]),
                    NonNullList.of(Ingredient.EMPTY, Ingredient.of(slabs[s]), Ingredient.of(slabs[s]))));
            added++;
        }
        return new int[] {replaced, added, 0};
    }

    // ================================================================
    // Self-audit (ModernWoods.audit, from BridgeSelfAudit.runCommon)
    // ================================================================

    /**
     * Logs one {@code [SELF-AUDIT/wood]} summary of the sub-blocks, and a WARN per problem: the eight block and item
     * classes, the five stub ids, the four planks' FC identity (5:5-8), FC's own placement, names and drops of every
     * stub kind and wood (through the patched FC methods, so W1-W3 are checked as applied), the four column-side
     * textures in the jar, the 52 names in FC's language table and the moulding stub stack the stairs recipe matches.
     * The recipes log their own line ({@link #addFcRecipes} runs after this audit). Never throws.
     */
    static void audit() {
        try {
            auditUnchecked();
        } catch (Throwable t) {
            LOGGER.warn("[SELF-AUDIT/wood] the newer woods' sub-block audit failed", t);
        }
    }

    private static void auditUnchecked() {
        if (!built) {
            LOGGER.warn("[SELF-AUDIT/wood] the sub-blocks of wood types 5-8 were not built (see the [WOOD] ERROR):"
                    + " the newer woods have no sidings, mouldings or corners, and their planks stay 1.20.1 planks");
            return;
        }
        int warnings = 0;
        btw.modern.Block[] blocks = btw.modern.Block.blocksList;
        btw.modern.Item[] items = btw.modern.Item.itemsList;
        int[] stubIds = {sidingStubId, mouldingStubId, cornerStubId, mouldingDecoStubId, sidingDecoStubId};
        for (int stub : stubIds) {
            if (stub <= 0 || items[stub] == null) {
                LOGGER.warn("[SELF-AUDIT/wood] FC wood stub ids {} are not all resolved items", java.util.Arrays.toString(stubIds));
                return;
            }
        }
        Block[] planks = {Blocks.ACACIA_PLANKS, Blocks.DARK_OAK_PLANKS, Blocks.MANGROVE_PLANKS, Blocks.CHERRY_PLANKS};
        btw.modern.StringTranslate lang = btw.modern.StringTranslate.getInstance();
        int names = 0;
        for (int s = 0; s < CAMEL.length; s++) {
            int w = ModernWoods.FIRST_WOOD_TYPE + s;
            int sid = SIDING_IDS[s], mid = MOULDING_IDS[s];
            // the eight block and item classes
            if (!FC_SIDING.equals(className(blocks[sid])) || !FC_MOULDING.equals(className(blocks[mid]))
                    || !FC_SIDING_ITEM.equals(className(items[sid])) || !FC_MOULDING_ITEM.equals(className(items[mid]))) {
                LOGGER.warn("[SELF-AUDIT/wood] ids {}/{} hold blocks {} / {} and items {} / {} (expected {} / {} and {} / {})",
                        sid, mid, className(blocks[sid]), className(blocks[mid]), className(items[sid]),
                        className(items[mid]), FC_SIDING, FC_MOULDING, FC_SIDING_ITEM, FC_MOULDING_ITEM);
                warnings++;
                continue;
            }
            // the planks' FC identity (2.A)
            if (ProxyRegistry.getBlockId(planks[s]) != ModernWoods.PLANKS_ID
                    || ProxyRegistry.getLegacyVariantMeta(planks[s]) != w) {
                LOGGER.warn("[SELF-AUDIT/wood] {} reads as FC {}:{} (expected {}:{})", planks[s],
                        ProxyRegistry.getBlockId(planks[s]), ProxyRegistry.getLegacyVariantMeta(planks[s]),
                        ModernWoods.PLANKS_ID, w);
                warnings++;
            }
            // FC's placement through the stubs (W1): each stub kind of this wood places this wood's block
            int[][] placements = {
                    {sidingStubId, w, sid}, {cornerStubId, w, sid}, {mouldingStubId, w, mid},
                    {mouldingDecoStubId, decorativeDamage(w, COLUMN), mid},
                    {mouldingDecoStubId, decorativeDamage(w, PEDESTAL), mid},
                    {mouldingDecoStubId, decorativeDamage(w, TABLE), mid},
                    {sidingDecoStubId, decorativeDamage(w, BENCH), sid},
                    {sidingDecoStubId, decorativeDamage(w, FENCE), sid}};
            for (int[] p : placements) {
                int placed = items[p[0]].GetBlockIDToPlace(p[1], 1, 0.5F, 0.5F, 0.5F);
                String suffix = stubNameSuffix(p[0], p[1]);
                String name = items[p[0]].getUnlocalizedName(new btw.modern.ItemStack(p[0], 1, p[1]));
                if (placed != p[2] || suffix == null || !name.endsWith(suffix)) {
                    LOGGER.warn("[SELF-AUDIT/wood] FC stub {}:{} places {} named {} (expected {} named ...{}; the"
                            + " FCItemBlockWoodStubWoodMixin patch)", p[0], p[1], placed, name, p[2], suffix);
                    warnings++;
                }
                if (lang.containsTranslateKey(name + ".name")) names++;
            }
            // the blocks' own items (pick-block and /give): siding, corner, bench, fence; moulding
            for (int[] own : new int[][] {{sid, 0}, {sid, 1}, {sid, SUBTYPE_BENCH}, {sid, SUBTYPE_FENCE}, {mid, 0}}) {
                String name = items[own[0]].getUnlocalizedName(new btw.modern.ItemStack(own[0], 1, own[1]));
                if (lang.containsTranslateKey(name + ".name")) names++;
            }
            // FC's drops of this wood (W2 through damageDropped's GetWoodTypeFromBlockID, W3 its own chain)
            int sidingDrop = blocks[sid].damageDropped(0);
            int benchDrop = blocks[sid].damageDropped(SUBTYPE_BENCH);
            int mouldingDrop = blocks[mid].damageDropped(0);
            int tableDrop = blocks[mid].damageDropped(15);
            if (sidingDrop != w || benchDrop != decorativeDamage(w, BENCH) || mouldingDrop != w
                    || tableDrop != decorativeDamage(w, TABLE)) {
                LOGGER.warn("[SELF-AUDIT/wood] blocks {}/{} drop siding {}, bench {}, moulding {}, table {} (expected {},"
                                + " {}, {}, {}; the FCBlockWoodSidingWoodMixin / FCBlockWoodMouldingWoodMixin patches)",
                        sid, mid, sidingDrop, benchDrop, mouldingDrop, tableDrop, w, decorativeDamage(w, BENCH), w,
                        decorativeDamage(w, TABLE));
                warnings++;
            }
            // the derived column side (deriveModernWoodTextures)
            String column = ("fcBlockColumnWood" + CAMEL[s] + "_side").toLowerCase(java.util.Locale.ROOT);
            String path = "/assets/" + BTWForgeMod.MOD_ID + "/textures/block/" + column + ".png";
            if (ModernWoodSubBlocks.class.getResource(path) == null) {
                LOGGER.warn("[SELF-AUDIT/wood] {} is missing from the jar (deriveModernWoodTextures)", path);
                warnings++;
            }
            // the moulding stub the stairs-from-mouldings recipe matches by its Damage tag
            ItemStack stub = mouldingStack(w, 1);
            if (stub.isEmpty() || stub.getTag() == null || stub.getTag().getInt("Damage") != w) {
                LOGGER.warn("[SELF-AUDIT/wood] the moulding stub of wood type {} converts to {} {} (expected a Damage:{}"
                        + " tag)", w, stub, stub.getTag(), w);
                warnings++;
            }
        }
        if (names != 13 * CAMEL.length) {
            LOGGER.warn("[SELF-AUDIT/wood] {} of the {} sub-block names are in FC's language table"
                    + " (/lang/BTW_modern_en_US.lang)", names, 13 * CAMEL.length);
            warnings++;
        }
        LOGGER.info("[SELF-AUDIT/wood] sub-blocks of wood types 5-8 at {}-{}: {} woods, {} names, {} warnings"
                + " (0 expected)", FIRST_ID, LAST_ID, CAMEL.length, names, warnings);
    }

    // ================================================================
    // FC lookups
    // ================================================================

    private static btw.modern.ItemStack fc(int id, int count, int damage) {
        return new btw.modern.ItemStack(id, count, damage);
    }

    /** An int static of FCBetterThanWolves, read reflectively (the Forge source set cannot name FC classes). */
    private static int fcInt(String field) throws ReflectiveOperationException {
        return Class.forName(FC_CORE).getField(field).getInt(null);
    }

    private static Throwable cause(Throwable t) {
        return t instanceof InvocationTargetException ite && ite.getCause() != null ? ite.getCause() : t;
    }

    private static String className(Object o) {
        return o == null ? "null" : o.getClass().getName();
    }

    private static String simpleName(String className) {
        return className.substring(className.lastIndexOf('.') + 1);
    }
}
