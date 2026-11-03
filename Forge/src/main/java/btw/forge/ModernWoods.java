package btw.forge;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FCMOD-INTEGRATION(1.20.1): modern_woods
 *
 * <p><b>Feature:</b> the four 1.20.1 woods 1.5.2 never had: acacia, dark oak, mangrove and cherry logs, their wood and
 * stripped forms and their planks, which every overworld biome outside the four FC species' grows.</p>
 *
 * <p><b>Conflict with FC:</b> FC keys a wood on one number, the wood type: the bark subtype, the planks subtype and the
 * work stump metadata (oak 0, spruce 1, birch 2, jungle 3, blood wood 4), read from the log's metadata as
 * {@code meta & 3} (Client FCBlockLog.java:92, :112, :128; FCBlockWorkStump.java:93; FCBlockSaw.java:619-639). Its log
 * is one block, id 17 ({@code Block.wood}), with two species bits, so the four modern woods had no FC wood: no
 * chewing, bark, tanning, work stump, saw planks, chopping, FC fuel or FC harvest hunger.</p>
 *
 * <p><b>Integration pattern:</b> D (FC-extend). The four become FC wood types 5-8 (maintainer ruling 1, 2026-10-19):
 * a second, plain and unmodified {@code FCBlockLog} at the reserved legacy id 165 ("log2"; metadata exactly FC's log
 * layout: bits 0-1 species = wood type - 5, bits 2-3 axis, 12 = stump), built here as TwinRegistry builds its twins,
 * with FC's own {@code FCItemBlockLog} item, as FC builds id 17's (FCBetterThanWolves.java:1880). The four logs map to
 * 165 (ProxyRegistry), so FC's own log code runs on them as on oak (dig, chew, the 0.025 harvest hunger, fire, kiln,
 * explosions); their wood and stripped forms read as 165. Where FC turns {@code meta & 3} into a wood type or tests
 * {@code == Block.wood.blockID}, nine load-time patches keyed on id 165 or on wood types 5-8
 * ({@code btw.forge.mixin.fcext}, M1-M9) give log2 its wood type. No registry entry is added: 165 is below the
 * ProxyBlock range (175), the work stump already carries META 0-15 on block_1030, bark 5-8 rides item_22547, and FC
 * planks 5:5-8 are the 1.20.1 planks items, and in the world the 1.20.1 planks blocks while their sub-blocks exist
 * ({@link ModernWoodSubBlocks}).</p>
 *
 * <p><b>Rebalance:</b> each new wood takes its nearest FC relative's numbers by lore (maintainer ruling "post-1.5.2
 * plants take their nearest FC relative's rules"), see {@link #BARK_PER_LEATHER} and {@link #PLANKS_BURN_TIME}. The
 * vanilla log -> 4 planks recipe goes for the four, as 4.B removes it for oak (FCRecipes.java:5418-5436).</p>
 *
 * <p><b>Injection strategy:</b> reflective construction (the Forge source set cannot name FC classes) in
 * BTWLifecycle step 5e, after the twins and before {@link BTWLifecycle#isFcReady()}, so the table is final at every
 * bake; FC recipes through FC's own public API (FCRecipes.AddCauldronRecipe / RemoveVanillaRecipe, :78-112).</p>
 *
 * <p><b>Alternative considered and rejected:</b> tag-based bark for the four (the logs design's decision 1(a): the
 * nearest species' bark and work stump, no wood of their own); a new FC block id at 175+ (would become a registered
 * ProxyBlock and a second, duplicate log item); FC source edits (FC code is read-only).</p>
 *
 * <p>Id 165 must never change: FC stacks store numeric ids in their NBT, and client and server must agree. A fifth
 * modern species needs its own id: log2's two species bits are full.</p>
 */
// FCMOD-INTEGRATION(1.20.1): modern_woods: acacia, dark oak, mangrove and cherry are FC wood types 5-8 through a plain FCBlockLog at legacy id 165 (log2) and the fcext patches keyed on it
public final class ModernWoods {

    private static final Logger LOGGER = LogManager.getLogger("BTW-Wood");

    /** Legacy id of log2, the second FCBlockLog: the first free id below the twins (ModernBlockStandIn.java:51-61). */
    public static final int LOG2_ID = 165;
    /** FC wood type of log2 species 0 (acacia); species s is wood type {@code FIRST_WOOD_TYPE + s}. */
    public static final int FIRST_WOOD_TYPE = 5;
    /** FC wood types of the four modern woods. */
    public static final int ACACIA = 5, DARK_OAK = 6, MANGROVE = 7, CHERRY = 8;
    /** 1.5.2 Block.planks (FC's FCBlockPlanks): the item id of FC planks 5:w. */
    static final int PLANKS_ID = 5;

    /** FCItemBlockLog type names of log2, by species (the log2 metadata order). */
    private static final String[] SPECIES_NAMES = {"acacia", "dark_oak", "mangrove", "cherry"};
    /** FCItemBark name extensions of wood types 5-8 (item.fcItemBark.&lt;x&gt;.name, /lang/BTW_modern_en_US.lang). */
    private static final String[] BARK_NAMES = {"acacia", "darkoak", "mangrove", "cherry"};
    /**
     * FC icon names of bark 5-8. NamedIcon lower-cases them; FCItemSubtypeModel then finds
     * models/item/fcitembark&lt;x&gt;.json, whose layer0 is the species' 1.20.1 log side: FC's bark icon is its log
     * side (the oak, spruce and jungle bark icons share 220, 209 and 190 of 256 pixels with their trunk side).
     */
    public static final String[] BARK_ICONS =
            {"fcItemBarkAcacia", "fcItemBarkDarkOak", "fcItemBarkMangrove", "fcItemBarkCherry"};
    /** FC icon names of the work stump sides 5-8: textures/block/fcblockworkstump&lt;x&gt;.png (deriveModernWoodTextures). */
    public static final String[] WORK_STUMP_ICONS =
            {"fcBlockWorkStumpAcacia", "fcBlockWorkStumpDarkOak", "fcBlockWorkStumpMangrove", "fcBlockWorkStumpCherry"};
    /** 1.20.1 texture names of the four woods (minecraft:block/&lt;x&gt;_log, _log_top, _planks). */
    private static final String[] TEXTURE_NAMES = {"acacia", "dark_oak", "mangrove", "cherry"};

    /**
     * Bark per leather in the cauldron, by wood type 5-8 (FC: oak 5, spruce 3, birch 8, jungle 2, blood 8,
     * FCRecipes.java:3355-3433). Acacia 2 (wattle bark is among the richest tannin sources), dark oak 5 (an oak),
     * mangrove 2 (mangrove bark, "cutch", is very rich in tannin), cherry 8 (thin, ring-peeling bark like birch). Cut
     * leather takes the same count, as in FC.
     */
    private static final int[] BARK_PER_LEATHER = {2, 5, 2, 8};
    /**
     * FC planks fuel by wood type 5-8 (logs burn x4, FCBlockLog.GetFurnaceBurnTime): acacia the oak tier (dense, dry
     * savanna hardwood), dark oak the oak's, mangrove the jungle tier (waterlogged tropical wood), cherry the birch
     * tier (prized fruitwood firewood). FC: oak 400, spruce 300, birch 500, jungle 200 (FCEnumFurnaceBurnTime).
     */
    private static final int[] PLANKS_BURN_TIME = {
            btw.modern.FCEnumFurnaceBurnTime.PLANKS_OAK.m_iBurnTime,
            btw.modern.FCEnumFurnaceBurnTime.PLANKS_OAK.m_iBurnTime,
            btw.modern.FCEnumFurnaceBurnTime.PLANKS_JUNGLE.m_iBurnTime,
            btw.modern.FCEnumFurnaceBurnTime.PLANKS_BIRCH.m_iBurnTime};

    private static final String FC_CORE = "net.minecraft.src.btw.core.FCBetterThanWolves";
    private static final String FC_LOG = "net.minecraft.src.btw.block.FCBlockLog";
    private static final String FC_ITEM_BLOCK_LOG = "net.minecraft.src.btw.item.FCItemBlockLog";
    private static final String FC_RECIPES = "net.minecraft.src.btw.crafting.FCRecipes";
    private static final String FC_UTILS_ITEM = "net.minecraft.src.btw.util.FCUtilsItem";

    /** True once log2 and its item are in FC's tables. Written once in {@link #create}, before FC is published. */
    private static volatile boolean built;
    /** FC item ids the patches eject (FCBetterThanWolves.fcItemBark / fcItemSawDust); 0 = unresolved. */
    private static int barkId;
    private static int sawDustId;
    /** FCBetterThanWolves.fcBlockWorkStump.blockID (1030 by default, ParseID); -1 = unresolved. */
    private static int workStumpId = -1;
    /** FCUtilsItem.EjectSingleItemWithRandomOffset(World, int, int, int, int, int); null = unresolved. */
    private static volatile MethodHandle eject;

    private static final Set<String> FAILURES_LOGGED = ConcurrentHashMap.newKeySet();

    private ModernWoods() {}

    // ================================================================
    // Wood types (pure; read by the fcext patches on both sides)
    // ================================================================

    /** FC wood type of log2 metadata {@code meta}: 5 + species (bits 0-1). */
    public static int woodType(int meta) {
        return FIRST_WOOD_TYPE + (meta & 3);
    }

    /** True for the wood types of the four modern woods (5-8). */
    public static boolean isWoodType(int woodType) {
        return woodType >= ACACIA && woodType <= CHERRY;
    }

    /** True once log2 exists (FC wood types 5-8 have their log). */
    public static boolean isBuilt() {
        return built;
    }

    /** FCItemBark name extension of wood type 5-8 ("acacia", "darkoak", "mangrove", "cherry"); null otherwise. */
    public static String barkName(int woodType) {
        return isWoodType(woodType) ? BARK_NAMES[woodType - FIRST_WOOD_TYPE] : null;
    }

    /** 1.20.1 texture name of wood type 5-8 ("acacia", "dark_oak", "mangrove", "cherry"); null otherwise. */
    public static String textureName(int woodType) {
        return isWoodType(woodType) ? TEXTURE_NAMES[woodType - FIRST_WOOD_TYPE] : null;
    }

    /** FC planks fuel of wood type 5-8 ({@link #PLANKS_BURN_TIME}); 0 otherwise. */
    public static int planksBurnTime(int woodType) {
        return isWoodType(woodType) ? PLANKS_BURN_TIME[woodType - FIRST_WOOD_TYPE] : 0;
    }

    /** FCBetterThanWolves.fcBlockWorkStump's id, -1 before {@link #create} resolved it. */
    public static int workStumpId() {
        return workStumpId;
    }

    // ================================================================
    // Icons (client cosmetics; NamedIcon resolves through FCBakedModel.lookupSprite)
    // ================================================================

    /** log2's icon: the species' 1.20.1 log side or end, by FC's axis bits (1.5.2 BlockLog.getIcon), stumps included. */
    public static btw.modern.Icon logIcon(int side, int meta) {
        int axis = meta & 12;
        boolean end = axis == 4 ? side == 4 || side == 5
                : axis == 8 ? side == 2 || side == 3
                : side == 0 || side == 1;   // vertical, and a stump (12): FC's trunk-top ends
        String name = TEXTURE_NAMES[meta & 3] + (end ? "_log_top" : "_log");
        return new NamedIcon(name);
    }

    /** FC planks icon of wood type 5-8: the 1.20.1 planks (minecraft:block/&lt;x&gt;_planks). */
    public static btw.modern.Icon planksIcon(int woodType) {
        return new NamedIcon(TEXTURE_NAMES[(woodType - FIRST_WOOD_TYPE) & 3] + "_planks");
    }

    /** Registers {@code names} with FC's icon register, in order (an FC registerIcons body). */
    public static btw.modern.Icon[] registerIcons(btw.modern.IconRegister register, String[] names) {
        btw.modern.Icon[] icons = new btw.modern.Icon[names.length];
        for (int n = 0; n < names.length; n++) icons[n] = register.registerIcon(names[n]);
        return icons;
    }

    // ================================================================
    // Construction (BTWLifecycle step 5e)
    // ================================================================

    /**
     * Builds log2 and its item, then the FC recipes of wood types 5-8. Runs once, after FC's own table and the twins,
     * before {@link BTWLifecycle#isFcReady()}. Never throws. If id 165 is already taken (an add-on or a changed FC id),
     * logs an ERROR and builds nothing: the four logs then keep no FC identity ({@link ProxyRegistry#getBlockId}) and
     * stay on the modern-log rule.
     */
    static void create() {
        if (built) return;
        btw.modern.Block[] blocks = btw.modern.Block.blocksList;
        btw.modern.Item[] items = btw.modern.Item.itemsList;
        if (blocks[LOG2_ID] != null || items[LOG2_ID] != null || ProxyRegistry.getProxy(LOG2_ID) != null) {
            LOGGER.error("[WOOD] legacy id {} is already taken (block {}, item {}): acacia, dark oak, mangrove and"
                            + " cherry are not FC wood types and keep the modern-log rule", LOG2_ID,
                    className(blocks[LOG2_ID]), className(items[LOG2_ID]));
            return;
        }
        btw.modern.Block log2;
        try {
            // FC's own constructor (Client FCBlockLog.java:24-45) fills blocksList[165], opaqueCubeLookup[165] and a
            // placeholder ItemBlock, as for every 1.5.2 block (Modern-Common Block.java:219-245).
            log2 = (btw.modern.Block) Class.forName(FC_LOG).getConstructor(int.class).newInstance(LOG2_ID);
            // a full log, as id 17; BTWLifecycle.fillOpaqueCubeLookup (step 5c) ran before log2 existed
            btw.modern.Block.opaqueCubeLookup[LOG2_ID] = true;
            // FC's log item, as FCBetterThanWolves.java:1880 builds id 17's: FCItemBlockLog(id - 256, block, names)
            btw.modern.Item item = (btw.modern.Item) Class.forName(FC_ITEM_BLOCK_LOG)
                    .getConstructor(int.class, btw.modern.Block.class, String[].class)
                    .newInstance(LOG2_ID - 256, log2, SPECIES_NAMES.clone());
            item.setUnlocalizedName("log");
            btw.modern.Item.itemsList[LOG2_ID] = item;
        } catch (Throwable t) {
            Throwable cause = t instanceof java.lang.reflect.InvocationTargetException ite && ite.getCause() != null
                    ? ite.getCause() : t;
            LOGGER.error("[WOOD] could not build log2 ({} at {}): acacia, dark oak, mangrove and cherry keep the"
                    + " modern-log rule", FC_LOG, LOG2_ID, cause);
            // the FC constructors may have registered themselves before failing: leave the id free again
            btw.modern.Block.blocksList[LOG2_ID] = null;
            btw.modern.Item.itemsList[LOG2_ID] = null;
            return;
        }
        built = true;
        LOGGER.info("[WOOD] log2 {} at {}: acacia, dark oak, mangrove and cherry are FC wood types 5-8",
                log2.getClass().getSimpleName(), LOG2_ID);

        resolveFc();
        addRecipes();
    }

    /** FC's bark, sawdust and work stump ids and the eject helper the patches use. Logs a WARN for what fails. */
    private static void resolveFc() {
        try {
            barkId = ((btw.modern.Item) fcStatic("fcItemBark")).itemID;
            sawDustId = ((btw.modern.Item) fcStatic("fcItemSawDust")).itemID;
            workStumpId = ((btw.modern.Block) fcStatic("fcBlockWorkStump")).blockID;
        } catch (Throwable t) {
            LOGGER.warn("[WOOD] FC's bark, sawdust or work stump could not be read: the log2 saw and chisel patches"
                    + " stay off", t);
        }
        try {
            Method m = Class.forName(FC_UTILS_ITEM).getMethod("EjectSingleItemWithRandomOffset",
                    btw.modern.World.class, int.class, int.class, int.class, int.class, int.class);
            eject = MethodHandles.lookup().unreflect(m);
        } catch (Throwable t) {
            LOGGER.warn("[WOOD] FCUtilsItem.EjectSingleItemWithRandomOffset could not be resolved: the saw does not cut"
                    + " log2", t);
        }
    }

    /**
     * FC's recipes for wood types 5-8 through FC's own API: tanning with bark 5-8 (FCRecipes.java:3355-3433: leather
     * and cut leather, {@link #BARK_PER_LEATHER}), and the removal records of the vanilla log -> 4 planks recipes
     * (as :5418-5436 records oak's), which FcRecipeRemovals pass 1 applies to the 1.20.1 recipes. Log chopping at the
     * work stump is FC's own FCRecipesLogChopping (patched to accept log2).
     */
    private static void addRecipes() {
        try {
            Class<?> recipes = Class.forName(FC_RECIPES);
            Method cauldron = recipes.getMethod("AddCauldronRecipe", btw.modern.ItemStack.class,
                    btw.modern.ItemStack[].class);
            Method removeVanilla = recipes.getMethod("RemoveVanillaRecipe", btw.modern.ItemStack.class,
                    Object[].class);
            btw.modern.Item bark = (btw.modern.Item) fcStatic("fcItemBark");
            btw.modern.Item dung = (btw.modern.Item) fcStatic("fcItemDung");
            btw.modern.Item scoured = (btw.modern.Item) fcStatic("fcItemScouredLeather");
            btw.modern.Item scouredCut = (btw.modern.Item) fcStatic("fcItemScouredLeatherCut");
            btw.modern.Item tanned = (btw.modern.Item) fcStatic("fcItemTannedLeather");
            btw.modern.Item tannedCut = (btw.modern.Item) fcStatic("fcItemTannedLeatherCut");
            for (int species = 0; species < 4; species++) {
                int wood = woodType(species);
                int barkCount = BARK_PER_LEATHER[species];
                cauldron.invoke(null, new btw.modern.ItemStack(tanned, 1), new btw.modern.ItemStack[] {
                        new btw.modern.ItemStack(dung, 1),
                        new btw.modern.ItemStack(scoured, 1),
                        new btw.modern.ItemStack(bark, barkCount, wood)});
                cauldron.invoke(null, new btw.modern.ItemStack(tannedCut, 2), new btw.modern.ItemStack[] {
                        new btw.modern.ItemStack(dung, 1),
                        new btw.modern.ItemStack(scouredCut, 2),
                        new btw.modern.ItemStack(bark, barkCount, wood)});
                removeVanilla.invoke(null, new btw.modern.ItemStack(PLANKS_ID, 4, wood),
                        new Object[] {"#", '#', new btw.modern.ItemStack(LOG2_ID, 1, species)});
            }
            LOGGER.info("[WOOD] tanning with bark 5-8 ({} bark per leather) and the 4 vanilla log -> planks removals"
                    + " added", Arrays.toString(BARK_PER_LEATHER));
        } catch (Throwable t) {
            Throwable cause = t instanceof java.lang.reflect.InvocationTargetException ite && ite.getCause() != null
                    ? ite.getCause() : t;
            LOGGER.warn("[WOOD] the FC recipes of wood types 5-8 could not be added", cause);
        }
    }

    // ================================================================
    // The saw (FCBlockSawWoodMixin, M6)
    // ================================================================

    /**
     * FC's log case of the saw for log2 (Client FCBlockSaw.java:619-639) with wood type {@code woodType}: 4 planks of
     * that wood type, 2 sawdust and its bark, each through FC's FCUtilsItem.EjectSingleItemWithRandomOffset. False
     * (nothing ejected) when FC's helper or ids could not be resolved, so FC's own path runs instead.
     */
    public static boolean ejectSawnLog2(btw.modern.World world, int i, int j, int k, int woodType) {
        MethodHandle handle = eject;
        if (!built || handle == null || barkId <= 0 || sawDustId <= 0) return false;
        try {
            for (int n = 0; n < 4; n++) handle.invoke(world, i, j, k, PLANKS_ID, woodType);
            for (int n = 0; n < 2; n++) handle.invoke(world, i, j, k, sawDustId, 0);
            handle.invoke(world, i, j, k, barkId, woodType);
            return true;
        } catch (Throwable t) {
            failOnce("saw", t);
            return false;
        }
    }

    // ================================================================
    // Self-audit (BridgeSelfAudit.runCommon)
    // ================================================================

    /**
     * Logs one {@code [SELF-AUDIT/wood]} summary, and a WARN per problem: log2's class and item, the block mappings
     * of the four logs, their wood and stripped forms and their planks (ProxyRegistry), the item mappings both ways
     * (LegacyVariants), and the bark models and work-stump textures in the jar; then the sub-blocks of wood types 5-8
     * ({@link ModernWoodSubBlocks#audit}). Never throws.
     */
    public static void audit() {
        try {
            auditUnchecked();
        } catch (Throwable t) {
            LOGGER.warn("[SELF-AUDIT/wood] the modern-woods audit failed", t);
        }
        // FCMOD-INTEGRATION(1.20.1): modern_wood_subblocks: the sidings, mouldings, corners and decorative blocks of wood
        // types 5-8 are audited with log2, so BridgeSelfAudit.runCommon needs no line of its own
        ModernWoodSubBlocks.audit();
    }

    private static void auditUnchecked() {
        if (!built) {
            LOGGER.warn("[SELF-AUDIT/wood] log2 was not built (see the [WOOD] ERROR): acacia, dark oak, mangrove and"
                    + " cherry are not FC wood types");
            return;
        }
        int warnings = 0;
        String blockClass = className(btw.modern.Block.blocksList[LOG2_ID]);
        String itemClass = className(btw.modern.Item.itemsList[LOG2_ID]);
        if (!FC_LOG.equals(blockClass) || !FC_ITEM_BLOCK_LOG.equals(itemClass)) {
            LOGGER.warn("[SELF-AUDIT/wood] id {} holds block {} and item {} (expected {} and {})", LOG2_ID, blockClass,
                    itemClass, FC_LOG, FC_ITEM_BLOCK_LOG);
            warnings++;
        }
        if (barkId <= 0 || sawDustId <= 0 || workStumpId <= 0 || eject == null) {
            LOGGER.warn("[SELF-AUDIT/wood] FC's bark {}, sawdust {}, work stump {} or eject helper {} is unresolved",
                    barkId, sawDustId, workStumpId, eject != null);
            warnings++;
        }
        Block[] logs = {Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG, Blocks.MANGROVE_LOG, Blocks.CHERRY_LOG};
        Block[][] forms = {
                {Blocks.ACACIA_WOOD, Blocks.STRIPPED_ACACIA_LOG, Blocks.STRIPPED_ACACIA_WOOD},
                {Blocks.DARK_OAK_WOOD, Blocks.STRIPPED_DARK_OAK_LOG, Blocks.STRIPPED_DARK_OAK_WOOD},
                {Blocks.MANGROVE_WOOD, Blocks.STRIPPED_MANGROVE_LOG, Blocks.STRIPPED_MANGROVE_WOOD},
                {Blocks.CHERRY_WOOD, Blocks.STRIPPED_CHERRY_LOG, Blocks.STRIPPED_CHERRY_WOOD}};
        Block[] planks = {Blocks.ACACIA_PLANKS, Blocks.DARK_OAK_PLANKS, Blocks.MANGROVE_PLANKS, Blocks.CHERRY_PLANKS};
        for (int s = 0; s < 4; s++) {
            int wood = woodType(s);
            Block log = logs[s];
            if (ProxyRegistry.getBlockId(log) != LOG2_ID || ProxyRegistry.getModernBlockVariant(LOG2_ID, s) != log
                    || ProxyRegistry.getWoodType(log) != wood) {
                LOGGER.warn("[SELF-AUDIT/wood] {}: id {} (expected {}), variant {} = {}, wood type {} (expected {})",
                        log, ProxyRegistry.getBlockId(log), LOG2_ID, s, ProxyRegistry.getModernBlockVariant(LOG2_ID, s),
                        ProxyRegistry.getWoodType(log), wood);
                warnings++;
            }
            for (Block form : forms[s]) {
                if (!ProxyRegistry.isLogForm(form) || ProxyRegistry.getTreeLogId(form) != LOG2_ID
                        || ProxyRegistry.getWoodType(form) != wood || ProxyRegistry.getBlockId(form) != 0) {
                    LOGGER.warn("[SELF-AUDIT/wood] {}: log form {}, log id {}, wood type {} (expected a form of {} with"
                                    + " wood type {} and no identity, id {})", form, ProxyRegistry.isLogForm(form),
                            ProxyRegistry.getTreeLogId(form), ProxyRegistry.getWoodType(form), LOG2_ID, wood,
                            ProxyRegistry.getBlockId(form));
                    warnings++;
                }
            }
            if (ProxyRegistry.getModernBlockVariant(PLANKS_ID, wood) != planks[s]) {
                LOGGER.warn("[SELF-AUDIT/wood] FC planks {}:{} place {} (expected {})", PLANKS_ID, wood,
                        ProxyRegistry.getModernBlockVariant(PLANKS_ID, wood), planks[s]);
                warnings++;
            }
            warnings += auditItem(log.asItem(), LOG2_ID, s);
            warnings += auditItem(planks[s].asItem(), PLANKS_ID, wood);
            String bark = BARK_ICONS[s].toLowerCase(java.util.Locale.ROOT);
            String stump = WORK_STUMP_ICONS[s].toLowerCase(java.util.Locale.ROOT);
            warnings += auditResource("/assets/" + BTWForgeMod.MOD_ID + "/models/item/" + bark + ".json");
            warnings += auditResource("/assets/" + BTWForgeMod.MOD_ID + "/textures/block/" + stump + ".png");
        }
        LOGGER.info("[SELF-AUDIT/wood] log2 {} / {} at {}: 4 woods (wood types 5-8), {} warnings (0 expected)",
                blockClass, itemClass, LOG2_ID, warnings);
    }

    /** One WARN when {@code item} and FC (id, meta) do not convert into each other (LegacyVariants), else 0. */
    private static int auditItem(Item item, int id, int meta) {
        int[] legacy = LegacyVariants.toLegacy(item);
        Item modern = LegacyVariants.toModern(id, meta);
        if (legacy != null && legacy[0] == id && legacy[1] == meta && modern == item) return 0;
        LOGGER.warn("[SELF-AUDIT/wood] {} reads as FC {} and FC {}:{} as {} (expected {}:{} both ways; the"
                + " LegacyVariants modern_woods rows)", item, legacy == null ? "nothing" : legacy[0] + ":" + legacy[1],
                id, meta, modern, id, meta);
        return 1;
    }

    /** One WARN when the jar lacks {@code path}, else 0. */
    private static int auditResource(String path) {
        if (ModernWoods.class.getResource(path) != null) return 0;
        LOGGER.warn("[SELF-AUDIT/wood] {} is missing from the jar (models: checked in; work-stump textures:"
                + " deriveModernWoodTextures)", path);
        return 1;
    }

    // ================================================================
    // FC lookups
    // ================================================================

    /** An FCBetterThanWolves static, read reflectively (the Forge source set cannot name FC classes). */
    private static Object fcStatic(String field) throws ReflectiveOperationException {
        Object value = Class.forName(FC_CORE).getField(field).get(null);
        if (value == null) throw new IllegalStateException("FCBetterThanWolves." + field + " is null");
        return value;
    }

    private static String className(Object o) {
        return o == null ? "null" : o.getClass().getName();
    }

    private static void failOnce(String what, Throwable t) {
        if (FAILURES_LOGGED.add(what)) {
            LOGGER.warn("[WOOD] {} failed (further failures are not logged): {}", what, t.toString(), t);
        }
    }
}
