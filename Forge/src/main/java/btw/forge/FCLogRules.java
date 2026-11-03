package btw.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.ForgeEventFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FC's log rules (1.5.2 FCBlockLog: the log 17, and the port's log2 165 for acacia, dark oak, mangrove and cherry,
 * ModernWoods) for 1.20.1 blocks that look like logs but are not FC's log: Dynamic Trees branches and stumps
 * (INTEGRATIONS #027), and the 1.20.1 logs FC has no identity for (ModernLogRules, ModernLogHooks). FC's own logs keep
 * FC's code (ServerPlayerGameModeMixin, FCMiningSpeed); these helpers answer the same questions for a block FC never
 * knew, from FC's own data, so a caller reproduces FC's harvest without copying FC logic:
 * <ul>
 *   <li>{@link #canHarvestLog}: does the held item harvest a log whole (FC's log material needs a tool,
 *       FCBetterThanWolves.java:50)? Otherwise FC chews it (ConvertBlock: bark, then the chewed stages).</li>
 *   <li>{@link #isWorkStumpTool}: does the held item turn a stump into a work stump (an FC chisel of harvest level
 *       2 or more, Client FCBlockLog.java:219-229)?</li>
 *   <li>{@link #fcWoodMeta}, {@link #bark}, {@link #sawdust}, {@link #stick}, {@link #workStumpState}: FC's wood type
 *       and the items and block FC's log produces.</li>
 *   <li>{@link #chargeLogHarvest}: FC's harvest exhaustion for each log taken.</li>
 *   <li>The log rule's two classes of 1.20.1 log ({@link #isLogRuleTarget}): class F ({@link #isFcReadLog}), the bark
 *       and stripped forms FC reads as log 17 or 165 although they have no FC identity, and class S
 *       ({@link #isForeignLog}), every other log FC has no identity for (crimson and warped stems, modded logs).
 *       Their stump ({@link #isStump}), bark ({@link #barked}, {@link #barkSpecies}), work stump
 *       ({@link #workStumpSpecies}), chewed log ({@link #chewedLogMeta}) and saw planks ({@link #sawPlanks}).</li>
 * </ul>
 * <p>Wood types are FC's one number per wood (the bark, planks and work-stump subtype): 0-3 FC's oak, spruce, birch and
 * jungle, 4 blood wood, 5-8 acacia, dark oak, mangrove and cherry (ModernWoods). A wood with none, the stems
 * (maintainer ruling 2, 2026-10-19) and a modded log a pack tagged {@code betterthanwolves:bark/none}, is -1.</p>
 * <p>Server side unless stated. Nothing here throws: every failure logs once and degrades to {@link ItemStack#EMPTY},
 * null, -1 or false. FC's statics are read by reflection (the Forge source set cannot name FC classes), only once
 * BTWLifecycle.isFcReady(), and cached.</p>
 */
// FCMOD-INTEGRATION(1.20.1): fc_log_rules: trunks FC never knew (Dynamic Trees branches, modern logs) follow FC's log rules through FC's own data, not a copy of FCBlockLog
public final class FCLogRules {
    private static final Logger LOGGER = LogManager.getLogger("BTW-LogRules");

    private static final String FC_CORE = "net.minecraft.src.btw.core.FCBetterThanWolves";
    private static final String FC_LOG = "net.minecraft.src.btw.block.FCBlockLog";
    /** 1.5.2 Block.wood, the log (FCBlockLog replaces it in blocksList). */
    private static final int LEGACY_LOG_ID = 17;
    /** 1.5.2 Item.stick (256 + 24). */
    private static final int LEGACY_STICK_ID = 280;

    /** FC wood type of blood wood (FCItemBark subtype 4, FCItemBark.java:25-32): bark, but no work stump. */
    private static final int BLOOD_WOOD = 4;
    /** The first and last FC wood types of ModernWoods (acacia 5 ... cherry 8, species = wood type - 5 in log2). */
    private static final int FIRST_MODERN_WOOD = 5;
    private static final int LAST_MODERN_WOOD = 8;
    /** Decision 6 (a) of the logs design: a barked log with no bark tag pays oak bark and makes an oak work stump. */
    public static final int UNTAGGED_BARK = 0;

    /**
     * The block tags {@code betterthanwolves:bark/<name>} that name a log's bark by wood type (the index): written by
     * LogBarkTagProvider, read by {@link #barkSpecies} after FC's own wood type, so a pack maps a modded log by tag.
     */
    public static final List<String> BARK_TAG_NAMES = List.of("oak", "spruce", "birch", "jungle", "blood_wood",
            "acacia", "dark_oak", "mangrove", "cherry");
    private static final List<TagKey<Block>> BARK_TAGS = barkTags();
    /** {@code betterthanwolves:bark/none}: a log with no bark and no work stump (the stems, maintainer ruling 2). */
    public static final TagKey<Block> BARK_NONE = barkTag("none");

    /** The 1.20.1 strip result per state ({@link #strippedState}); empty when it does not strip. */
    private static final Map<BlockState, Optional<BlockState>> STRIPPED = new ConcurrentHashMap<>();
    /** The planks item per log block ({@link #sawPlanks}); EMPTY when it has no one-log planks recipe. */
    private static final Map<Block, ItemStack> SAW_PLANKS = new ConcurrentHashMap<>();
    /** FCBlockLog / FCBlockLogDamaged hardness (1.25 / 2.0); NaN until resolved after FC init. */
    private static volatile float damagedLogFactor = Float.NaN;

    /** FCBetterThanWolves statics by field name; empty when missing (logged once). Filled only after FC init. */
    private static final Map<String, Optional<Object>> FC_STATICS = new ConcurrentHashMap<>();
    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();
    /** FCBlockLog.IsWorkStumpItemConversionTool (Lbtw/modern/ItemStack;Lbtw/modern/World;III)Z; null until resolved. */
    private static volatile Optional<Method> isWorkStumpTool;

    private FCLogRules() {}

    /**
     * True if {@code p}'s held item harvests an FC log whole at {@code pos}: FC's canHarvestBlock against
     * blocksList[17] (Modern-Common EntityPlayer.canHarvestBlock; FCItemAxe.canHarvestBlock, Common
     * FCItemAxe.java:50-68). A held item with no FC item at all (another mod's axe) counts when it declares AXE_DIG.
     * A stump is the caller's decision: FC's axe refuses a stump only through its world-read GetIsProblemToRemove.
     */
    public static boolean canHarvestLog(ServerPlayer p, BlockPos pos) {
        if (p == null || pos == null) return false;
        try {
            btw.modern.Block log = fcLog();
            if (log != null) {
                PlayerBridge pb = PlayerBridge.getOrCreate(p); // syncs from the ServerPlayer
                if (pb.canHarvestBlock(log, pos.getX(), pos.getY(), pos.getZ())) return true;
            }
            ItemStack held = p.getMainHandItem();
            return !held.isEmpty() && ProxyItem.fcItemFor(held.getItem()) == null
                    && held.canPerformAction(ToolActions.AXE_DIG);
        } catch (Throwable t) {
            failOnce("canHarvestLog", t);
            return false;
        }
    }

    /**
     * True if {@code held} turns an FC stump into a work stump: FC's FCBlockLog.IsWorkStumpItemConversionTool on
     * blocksList[17] (an FC chisel of harvest level 2 or more, Client FCBlockLog.java:219-229), given FC's view of the
     * stack and {@code p}'s world.
     */
    public static boolean isWorkStumpTool(ServerPlayer p, ItemStack held, BlockPos pos) {
        if (p == null || held == null || held.isEmpty() || pos == null) return false;
        try {
            btw.modern.Block log = fcLog();
            Method m = workStumpToolMethod();
            if (log == null || m == null || !m.getDeclaringClass().isInstance(log)) return false;
            btw.modern.ItemStack fcStack = ItemStackHelper.toFcStack(held);
            if (fcStack == null) return false;
            Object r = m.invoke(log, fcStack, WorldBridge.getOrCreate(p.serverLevel()),
                    pos.getX(), pos.getY(), pos.getZ());
            return Boolean.TRUE.equals(r);
        } catch (Throwable t) {
            failOnce("isWorkStumpTool", t);
            return false;
        }
    }

    /**
     * FC's wood type of {@code log} (contract C1 of the 2026-10-19 follow-ups): 0-3 (oak, spruce, birch, jungle) for
     * log 17's four logs and their bark and stripped forms, 5-8 (acacia, dark oak, mangrove, cherry) for log2's
     * (ProxyRegistry.getWoodType, ModernWoods), -1 for every other block, the stems and modded logs included. Never a
     * log2-local species (cherry is log2 species 3 and would read as jungle). Callers pass the unstripped log when they
     * mean its bark. Both sides. The name is kept for its callers (Dynamic Trees, leaves); {@link #fcWoodType} is the
     * same value.
     */
    public static int fcWoodMeta(Block log) {
        if (log == null) return -1;
        try {
            return ProxyRegistry.getWoodType(log);
        } catch (Throwable t) {
            failOnce("fcWoodMeta", t);
            return -1;
        }
    }

    /** {@link #fcWoodMeta} under its meaning: FC's wood type of {@code log} (0-3, 5-8), -1 when it has none. */
    public static int fcWoodType(Block log) {
        return fcWoodMeta(log);
    }

    /**
     * One FC bark of wood type {@code woodType} (FCBlockLog.ConvertBlock ejects bark with damage {@code meta & 3},
     * Client FCBlockLog.java:112; log2 ejects its wood type, FCBlockLogWoodMixin): FC's subtypes 0-4 and ModernWoods'
     * 5-8 (FCItemBarkWoodMixin names and draws them). EMPTY outside 0-8, so a wood with no bark (-1) gives none.
     */
    public static ItemStack bark(int woodType) {
        if (woodType < 0 || woodType > LAST_MODERN_WOOD) return ItemStack.EMPTY;
        btw.modern.Item bark = fcItem("fcItemBark");
        return bark != null ? stack(bark.itemID, 1, woodType) : ItemStack.EMPTY;
    }

    /** {@code n} FC sawdust (FCBlockLog's improper-tool drop, Client FCBlockLog.java:125-131); EMPTY for n <= 0. */
    public static ItemStack sawdust(int n) {
        if (n <= 0) return ItemStack.EMPTY;
        btw.modern.Item sawDust = fcItem("fcItemSawDust");
        return sawDust != null ? stack(sawDust.itemID, n, 0) : ItemStack.EMPTY;
    }

    /** {@code n} sticks (1.5.2 Item.stick, 280); EMPTY for n <= 0. */
    public static ItemStack stick(int n) {
        if (n <= 0) return ItemStack.EMPTY;
        return stack(LEGACY_STICK_ID, n, 0);
    }

    /** True for a wood type FC makes a work stump of: FC's four (0-3) and ModernWoods' four (5-8), not blood wood. */
    public static boolean isWorkStumpWood(int woodType) {
        return (woodType >= 0 && woodType < BLOOD_WOOD)
                || (woodType >= FIRST_MODERN_WOOD && woodType <= LAST_MODERN_WOOD);
    }

    /**
     * The FC work stump of wood type {@code woodType} as a 1.20.1 state: FCBetterThanWolves.fcBlockWorkStump's block
     * with META {@code woodType}, as FCBlockLog.ConvertBlock places it (Client FCBlockLog.java:92: {@code meta & 3} for
     * log 17; log2 writes its wood type 5-8, FCBlockLogWoodMixin). Not masked: META 5-8 are the ModernWoods stumps
     * (FCBlockWorkStumpWoodMixin draws them). Null outside {@link #isWorkStumpWood} or when the block cannot be
     * resolved.
     */
    @Nullable
    public static BlockState workStumpState(int woodType) {
        if (!isWorkStumpWood(woodType)) return null;
        try {
            btw.modern.Block stump = fcBlock("fcBlockWorkStump");
            if (stump == null) return null;
            Block modern = ProxyRegistry.getModernBlock(stump.blockID);
            if (modern == null) return null;
            BlockState state = modern.defaultBlockState();
            return state.hasProperty(ProxyBlock.META) ? state.setValue(ProxyBlock.META, woodType) : null;
        } catch (Throwable t) {
            failOnce("workStumpState", t);
            return null;
        }
    }

    /**
     * FC's harvest exhaustion for {@code logs} logs of wood type {@code woodType} taken at {@code pos}: Modern-Common
     * EntityPlayer.AddHarvestBlockExhaustion once per log for the cell's FC log id and {@code meta & 3} (log 17 with
     * species 0-3, log2 165 with wood type - 5), as FC charges each block it harvests (the held item's
     * GetExhaustionOnUsedToHarvestBlock, 0.025 by default). A wood type with no FC log (-1) is charged as log 17
     * species 0; FC's exhaustion does not depend on it.
     */
    public static void chargeLogHarvest(ServerPlayer p, int logs, int woodType, BlockPos pos) {
        if (p == null || pos == null || logs <= 0) return;
        try {
            int logId = logIdFor(woodType);
            if (fcLogBlock(logId) == null) return;
            int meta = logId == LEGACY_LOG_ID ? Math.max(woodType, 0) & 3 : (woodType - FIRST_MODERN_WOOD) & 3;
            PlayerBridge pb = PlayerBridge.getOrCreate(p); // syncs from the ServerPlayer
            for (int n = 0; n < logs; n++) {
                pb.AddHarvestBlockExhaustion(logId, pos.getX(), pos.getY(), pos.getZ(), meta);
            }
        } catch (Throwable t) {
            failOnce("chargeLogHarvest", t);
        }
    }

    // ------------------------------------------------------------------ the log rule's two classes (both sides)

    /**
     * Class F: a block with no FC identity that FC's world reads as a log (ProxyRegistry.isLogForm: the bark and
     * stripped forms of log 17's and log2's species, world-read as 17 or 165), with that FC log present. FC's machines
     * already treat it as FC's log (the saw, the stump remover, the kiln); the log rule gives it FC's dig rate and
     * FC's own chew ({@link #fcReadLog}.ConvertBlock at the cell), and keeps the vanilla harvest drop (the block drops
     * itself). Both sides.
     */
    public static boolean isFcReadLog(BlockState state) {
        if (state == null) return false;
        try {
            return fcReadLog(state) != null;
        } catch (Throwable t) {
            failOnce("isFcReadLog", t);
            return false;
        }
    }

    /**
     * The FC log a class-F state reads as (blocksList[17] or blocksList[165], FCBlockLog), or null when the state is
     * not class F or FC has no block there.
     */
    @Nullable
    public static btw.modern.Block fcReadLog(BlockState state) {
        if (state == null || !BTWLifecycle.isFcReady()) return null;
        Block block = state.getBlock();
        if (!ProxyRegistry.isLogForm(block)) return null;
        return fcLogBlock(ProxyRegistry.getTreeLogId(block));
    }

    /**
     * The class-S structural test without its hook clause (the {@link ModernLogHooks} predicate): in
     * {@code #minecraft:logs}, no FC identity and not class F, an AXIS pillar, a full collision cube and no block
     * entity. The AXIS and full-cube guards keep Dynamic Trees branches out (no AXIS: BasicBranchBlock.java:54,
     * :119-120) even when #027 is switched off (DTTrees.isHookTarget answers false then, while the branches stay in
     * {@code #minecraft:logs}); the block-entity guard keeps special modded cores out. Both sides; never throws.
     */
    public static boolean isForeignLogCandidate(BlockState state) {
        if (state == null || !BTWLifecycle.isFcReady()) return false;
        try {
            if (!state.is(BlockTags.LOGS) || !state.hasProperty(BlockStateProperties.AXIS) || state.hasBlockEntity()) {
                return false;
            }
            Block block = state.getBlock();
            if (ProxyRegistry.getFcBlock(block) != null || ProxyRegistry.isLogForm(block)) return false;
            return state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        } catch (Throwable t) {
            failOnce("isForeignLogCandidate", t);
            return false;
        }
    }

    /**
     * Class S: a 1.20.1 log FC has no identity for and does not read as its log (the crimson and warped stems and
     * hyphae, modded logs): {@link #isForeignLogCandidate} and no earlier integration owns it (the first FC machine
     * hook for the state is the log rule's, {@link ModernLogHooks}). Both sides.
     */
    public static boolean isForeignLog(BlockState state) {
        return isForeignLogCandidate(state) && ModernBlockHooks.find(state) == ModernLogHooks.INSTANCE;
    }

    /** A log the log rule acts on: class F ({@link #isFcReadLog}) or class S ({@link #isForeignLog}). Both sides. */
    public static boolean isLogRuleTarget(BlockState state) {
        return isFcReadLog(state) || isForeignLog(state);
    }

    /**
     * The port's one stump rule (contract C2), shared by WorldBridge.deriveVanillaMetadata (the stump bit FC's own logs
     * read, FCBlockLog.java:182-202) and the log rule: a vertical log ({@code AXIS == Y}) standing on a block that is
     * not air, renders solid ({@code isSolidRender}) and is not in {@code #minecraft:logs}. Geometric, as the port's
     * stump has always been (logs design decision 9 (a)); 4.B marks only a generated trunk base (Common
     * FCUtilsTrees.java:483-495). A state with no AXIS is never a stump. Side-agnostic (client dig prediction and
     * ClientFcWorld read it through WorldBridge); never throws.
     */
    public static boolean isStump(BlockGetter level, BlockPos pos, BlockState state) {
        if (level == null || pos == null || state == null) return false;
        try {
            if (!state.hasProperty(BlockStateProperties.AXIS)
                    || state.getValue(BlockStateProperties.AXIS) != Direction.Axis.Y) {
                return false;
            }
            BlockPos below = pos.below();
            BlockState belowState = level.getBlockState(below);
            return !belowState.isAir() && belowState.isSolidRender(level, below) && !belowState.is(BlockTags.LOGS);
        } catch (Throwable t) {
            failOnce("isStump", t);
            return false;
        }
    }

    /**
     * True if the log at {@code pos} still has its bark: class F always (FC's saw and FC's chew pay a class-F log's
     * bark from its metadata, FCBlockSaw.java:618-639, FCBlockLog.java:112, so its stripped forms keep their bark for
     * them); class S when it strips ({@link #strippedState} differs from it). Both sides.
     */
    public static boolean barked(BlockGetter level, BlockPos pos, BlockState state) {
        if (state == null) return false;
        if (isFcReadLog(state)) return true;
        BlockState stripped = strippedState(level, pos, state);
        return stripped != null && stripped != state;
    }

    /**
     * The state an axe strip turns {@code state} into (its axis kept), or null when it does not strip:
     * {@code IForgeBlock.getToolModifiedState(state, ctx, AXE_STRIP, simulate = true)} with a synthetic iron-axe
     * context and no player ({@code UseOnContext(Level, Player, InteractionHand, ItemStack, BlockHitResult)},
     * UseOnContext.java:25). The block's own method posts no BlockToolModificationEvent (IForgeBlock.java:758-766 vs
     * IForgeBlockState.java:659-663), so it never recurses into ModernLogRules, and it sees modded logs that strip
     * through their own override (AxeItem.STRIPPABLES alone does not). Cached per state; a block that throws (for
     * example one that needs the player) falls back to AxeItem.getAxeStrippingState (AxeItem.java:73-76), logged once.
     * Both sides.
     */
    @Nullable
    public static BlockState strippedState(BlockGetter level, BlockPos pos, BlockState state) {
        if (state == null) return null;
        Optional<BlockState> cached = STRIPPED.get(state);
        if (cached == null) {
            cached = Optional.ofNullable(computeStripped(level, pos, state));
            STRIPPED.put(state, cached);
        }
        return cached.orElse(null);
    }

    @Nullable
    private static BlockState computeStripped(BlockGetter level, BlockPos pos, BlockState state) {
        if (level instanceof Level lvl && pos != null) {
            try {
                UseOnContext ctx = new UseOnContext(lvl, null, InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE),
                        new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
                BlockState result = state.getBlock().getToolModifiedState(state, ctx, ToolActions.AXE_STRIP, true);
                return result != state ? result : null;
            } catch (Throwable t) {
                failOnce("strippedState " + state.getBlock(), t);
            }
        }
        try {
            return AxeItem.getAxeStrippingState(state);
        } catch (Throwable t) {
            failOnce("getAxeStrippingState " + state.getBlock(), t);
            return null;
        }
    }

    /**
     * The FC wood type of the bark a log pays (FC bark subtype): FC's own wood type first ({@link #fcWoodMeta}: log
     * 17's and log2's forms), then the first matching {@code betterthanwolves:bark/<wood>} tag
     * ({@link #BARK_TAG_NAMES}: oak 0, spruce 1, birch 2, jungle 3, blood_wood 4, acacia 5, dark_oak 6, mangrove 7,
     * cherry 8), -1 for
     * {@code betterthanwolves:bark/none} (the stems: no bark and no work stump, maintainer ruling 2), else
     * {@link #UNTAGGED_BARK} (oak, logs design decision 6 (a)). Callers pass the unstripped log when they mean the
     * species' bark; whether it still has bark is {@link #barked}. Both sides.
     */
    public static int barkSpecies(BlockState state) {
        if (state == null) return -1;
        int wood = fcWoodMeta(state.getBlock());
        if (wood >= 0) return wood;
        try {
            for (int w = 0; w < BARK_TAGS.size(); w++) {
                if (state.is(BARK_TAGS.get(w))) return w;
            }
            if (state.is(BARK_NONE)) return -1;
        } catch (Throwable t) {
            failOnce("barkSpecies", t);
        }
        return UNTAGGED_BARK;
    }

    /** The work stump's wood type for a log: {@link #barkSpecies} when FC makes a work stump of it, else -1. */
    public static int workStumpSpecies(BlockState state) {
        int wood = barkSpecies(state);
        return isWorkStumpWood(wood) ? wood : -1;
    }

    /**
     * The FCBlockLogDamaged metadata of a log's first chew, as FCBlockLog.ConvertBlock writes it (Client
     * FCBlockLog.java:97-108; FCBlockLogDamaged.SetIsStump / SetOrientation, :433-458): 12 for a stump, else
     * {@code orientation << 2} (Y 0, X 1, Z 2, BlockLog's order), damage level 0.
     */
    public static int chewedLogMeta(BlockState state, boolean stump) {
        if (stump) return 12;
        if (state == null || !state.hasProperty(BlockStateProperties.AXIS)) return 0;
        return switch (state.getValue(BlockStateProperties.AXIS)) {
            case X -> 1 << 2;
            case Z -> 2 << 2;
            default -> 0;
        };
    }

    /**
     * One plank of the log's own wood, for the saw (4 per log, FCBlockSaw.java:618-639): the result item of a
     * CRAFTING recipe with exactly one ingredient that accepts the log's item and whose result is in
     * {@code #minecraft:planks} (crimson stem -> crimson planks; a modded log -> its planks). Cached per block and
     * cleared by {@link #clearCaches} (tags, datapacks, server stop); EMPTY when there is none. Server side.
     */
    public static ItemStack sawPlanks(ServerLevel level, BlockState state) {
        if (level == null || state == null) return ItemStack.EMPTY;
        Block block = state.getBlock();
        ItemStack planks = SAW_PLANKS.get(block);
        if (planks == null) {
            planks = findPlanks(level, block);
            SAW_PLANKS.put(block, planks);
        }
        return planks.isEmpty() ? ItemStack.EMPTY : planks.copy();
    }

    private static ItemStack findPlanks(ServerLevel level, Block block) {
        try {
            ItemStack log = new ItemStack(block);
            if (log.isEmpty()) return ItemStack.EMPTY;
            for (CraftingRecipe recipe : level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
                Ingredient only = null;
                int ingredients = 0;
                for (Ingredient ingredient : recipe.getIngredients()) {
                    if (!ingredient.isEmpty()) {
                        only = ingredient;
                        ingredients++;
                    }
                }
                if (ingredients != 1 || !only.test(log)) continue;
                ItemStack result = recipe.getResultItem(level.registryAccess());
                if (!result.isEmpty() && result.is(ItemTags.PLANKS)) return result.copyWithCount(1);
            }
        } catch (Throwable t) {
            failOnce("sawPlanks", t);
        }
        return ItemStack.EMPTY;
    }

    /** Drops the per-state strip and per-block planks caches (tags or recipes changed, or the server stopped). */
    public static void clearCaches() {
        STRIPPED.clear();
        SAW_PLANKS.clear();
    }

    /**
     * The dig factor of a barkless log, which the log rule chews as FC's chewed log: FCBlockLog's hardness over
     * FCBlockLogDamaged's (1.25 / 2.0 = 0.625 with FC's values, Server FCBlockLog.java:26, FCBlockLogDamaged.java:25).
     * Read from FC's blocks once FC is ready and cached; 1.0 before FC init and on failure (logged once). Both sides.
     */
    public static float damagedLogFactor() {
        float f = damagedLogFactor;
        if (!Float.isNaN(f)) return f;
        if (!BTWLifecycle.isFcReady()) return 1F;
        f = 1F;
        try {
            btw.modern.Block log = fcLogBlock(LEGACY_LOG_ID);
            btw.modern.Block damaged = chewedLog();
            if (log != null && damaged != null && log.blockHardness > 0F && damaged.blockHardness > 0F) {
                f = log.blockHardness / damaged.blockHardness;
            } else {
                LOGGER.warn("FC's chewed-log hardness could not be read; a barkless log digs at the fresh log's rate");
            }
        } catch (Throwable t) {
            failOnce("damagedLogFactor", t);
        }
        damagedLogFactor = f;
        return f;
    }

    /** FCBetterThanWolves.fcBlockLogDamaged (FC's chewed log, 1010); null before FC init or when missing. */
    @Nullable
    public static btw.modern.Block chewedLog() {
        return fcBlock("fcBlockLogDamaged");
    }

    // ------------------------------------------------------------------ a chew in the world (server)

    /**
     * The face the player breaks {@code pos} from: the side FC ejects a chew's items toward. {@link DigFace}'s rule,
     * shared with FC's own harvest: the dig packet's face (kept for a break the server finishes later), else the face
     * the player's look ray hits, else UP.
     */
    // FCMOD-INTEGRATION(1.20.1): fc_dig_face: the log rule and Dynamic Trees' chews eject toward the dug face, the rule FC's harvest uses
    public static Direction hitFace(ServerPlayer p, BlockPos pos) {
        return DigFace.of(p, pos);
    }

    /**
     * FC's per-hit convert wear (Modern-Common ItemInWorldManager onBlockDestroyed, :106-113): the held item's
     * mineBlock for {@code state}, which ProxyItem routes to FC's onBlockDestroyed with {@code state}'s world-read id
     * (ProxyItem.minedBlockIdForFc: a log state gives 17 or 165, so FCItemChiselIron charges 10 on a cell that now
     * holds the work stump, FCItemChiselIron.java:43-55); a stack used up fires Forge's destroy-item event.
     */
    public static void wearConvert(ServerLevel level, ServerPlayer p, ItemStack held, BlockState state, BlockPos pos) {
        if (held == null || held.isEmpty()) return;
        try {
            ItemStack copy = held.copy();
            held.mineBlock(level, state, pos, p);
            if (held.isEmpty()) {
                ForgeEventFactory.onPlayerDestroyItem(p, copy, InteractionHand.MAIN_HAND);
            }
        } catch (Throwable t) {
            failOnce("wearConvert", t);
        }
    }

    /** Pops {@code stack} out of {@code face} of {@code pos} (FCUtilsItem.EjectStackFromBlockTowardsFacing's role). */
    public static void popToward(ServerLevel level, BlockPos pos, Direction face, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        try {
            Block.popResourceFromFace(level, pos, face, stack);
        } catch (Throwable t) {
            failOnce("popToward", t);
        }
    }

    // ------------------------------------------------------------------ FC lookups

    @Nullable
    private static btw.modern.Block fcLog() {
        return fcLogBlock(LEGACY_LOG_ID);
    }

    /** blocksList[{@code id}] for an FC log id (17, or ModernWoods.LOG2_ID); null when out of range or empty. */
    @Nullable
    private static btw.modern.Block fcLogBlock(int id) {
        return id > 0 && id < btw.modern.Block.blocksList.length ? btw.modern.Block.blocksList[id] : null;
    }

    /** The FC log id of a wood type: log2 (165) for 5-8 when FC has it, else log 17. */
    private static int logIdFor(int woodType) {
        if (woodType >= FIRST_MODERN_WOOD && woodType <= LAST_MODERN_WOOD
                && fcLogBlock(ModernWoods.LOG2_ID) != null) {
            return ModernWoods.LOG2_ID;
        }
        return LEGACY_LOG_ID;
    }

    private static TagKey<Block> barkTag(String wood) {
        return BlockTags.create(new ResourceLocation(BTWForgeMod.MOD_ID, "bark/" + wood));
    }

    private static List<TagKey<Block>> barkTags() {
        List<TagKey<Block>> tags = new ArrayList<>(BARK_TAG_NAMES.size());
        for (String wood : BARK_TAG_NAMES) tags.add(barkTag(wood));
        return List.copyOf(tags);
    }

    private static ItemStack stack(int fcItemId, int count, int damage) {
        try {
            return ItemStackHelper.fcIdToMcStack(fcItemId, count, damage);
        } catch (Throwable t) {
            failOnce("item " + fcItemId, t);
            return ItemStack.EMPTY;
        }
    }

    @Nullable
    private static btw.modern.Item fcItem(String field) {
        return fcStatic(field) instanceof btw.modern.Item item ? item : null;
    }

    @Nullable
    private static btw.modern.Block fcBlock(String field) {
        return fcStatic(field) instanceof btw.modern.Block block ? block : null;
    }

    /** An FCBetterThanWolves static; null before FC init (not cached then) or when missing (logged once). */
    @Nullable
    private static Object fcStatic(String field) {
        if (!BTWLifecycle.isFcReady()) return null;
        return FC_STATICS.computeIfAbsent(field, name -> {
            try {
                Object value = Class.forName(FC_CORE).getField(name).get(null);
                if (value == null) LOGGER.warn("FCBetterThanWolves.{} is null after FC init", name);
                return Optional.ofNullable(value);
            } catch (Throwable t) {
                LOGGER.warn("FCBetterThanWolves.{} could not be read: {}", name, t.toString());
                return Optional.empty();
            }
        }).orElse(null);
    }

    @Nullable
    private static Method workStumpToolMethod() {
        Optional<Method> m = isWorkStumpTool;
        if (m == null) {
            if (!BTWLifecycle.isFcReady()) return null;
            try {
                m = Optional.of(Class.forName(FC_LOG).getMethod("IsWorkStumpItemConversionTool",
                        btw.modern.ItemStack.class, btw.modern.World.class, int.class, int.class, int.class));
            } catch (Throwable t) {
                LOGGER.warn("FCBlockLog.IsWorkStumpItemConversionTool could not be resolved: {}", t.toString());
                m = Optional.empty();
            }
            isWorkStumpTool = m;
        }
        return m.orElse(null);
    }

    private static void failOnce(String what, Throwable t) {
        if (LOGGED.add(what)) {
            LOGGER.warn("FC log rule {} failed (further failures are not logged): {}", what, t.toString(), t);
        }
    }
}
