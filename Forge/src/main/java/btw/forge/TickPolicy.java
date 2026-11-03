package btw.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DetectorRailBlock;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.RedStoneOreBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The tick-ownership rule: FC decides the random and scheduled ticks of every FC-mapped vanilla-id
 * {@link BlockState}. One decision per state, made from the FC block at the state's legacy id and enforced at
 * the BlockState level by {@code btw.forge.mixin.BlockStateBaseTickMixin} (initCache / tick) and
 * {@code BlockStateRandomTickMixin} (randomTick), the single pass every server tick goes through
 * (ServerLevel.tickChunk -> state.randomTick, ServerLevel.tickBlock -> state.tick), so no 1.20.1 per-class override
 * can skip it. {@code LavaFluidMixin} covers the one separate path, the lava fluid random tick.
 *
 * <p><b>Other mods.</b> A randomTick HEAD veto of another mod (Serene Seasons 9.1+ cancels crop ticks out of season)
 * runs before FC's dispatch (BlockStateRandomTickMixin's priority). For the plants whose 1.20.1 random tick fires
 * Forge's CropGrowEvent ({@link #firesCropGrowEvents}), the FC random tick fires it too: {@code Pre} before the FC
 * body, which a DENY skips (Serene Seasons 9.0 and other crop-gating mods), and {@code Post} when the tick changed
 * the plant's state.</p>
 *
 * <p><b>Scope.</b> A state is managed when its Block maps to an FC block at a vanilla legacy id
 * ({@link ProxyRegistry#getBlockId} non-zero, blocksList slot populated) and is not a {@link ProxyBlock}.
 * ProxyBlocks keep their own forwarding (ProxyBlock.tick / randomTick / isRandomlyTicking). Unmanaged states, and
 * every state while {@link BTWLifecycle#isFcReady()} is false, keep the 1.20.1 flag and both owners stay
 * {@link #VANILLA}.</p>
 *
 * <p><b>Random ticks.</b> The state random-ticks exactly when 4.B random-ticks that FC block:
 * {@code getTickRandomly()} and the runtime FC class has a tick body of its own ({@code updateTick} or
 * {@code RandomUpdateTick} declared below btw.modern.Block). The gate is written into the cached
 * BlockStateBase.isRandomlyTicking at initCache, so LevelChunkSection's ticking counts and ServerLevel.tickChunk
 * follow it. Each random tick runs FC {@code RandomUpdateTick} and cancels the 1.20.1 body, as the 4.B WorldServer
 * loop does (SrcServer/patch.txt:25931-25938; RandomUpdateTick -> updateTick, vanilla/server Block.java:1496-1499).
 * The gate is keyed on the state's OWN block: StairBlock.isRandomlyTicking delegates to its base block
 * (StairBlock.java:146-148), and stairs get their FC stairs flag (false) instead.</p>
 *
 * <p><b>Scheduled ticks.</b> A scheduled tick runs FC {@code updateTick} and cancels 1.20.1, unless the state's
 * 1.20.1 class overrides tick itself. BlockBehaviour.tick is a no-op (BlockBehaviour.java:359-360), so ticks on
 * a non-overriding class come only from FC (WorldBridge.scheduleBlockUpdate). Overriding classes are 1.20.1
 * machinery FC never scheduled (leaf DISTANCE, cactus / sugar cane / farmland survival, chest lids, redstone
 * timing identical to 1.5.2, bubble columns, dripstone filling) and stay 1.20.1's. Exception {@code FC_SCHED}:
 * FIRE, SAND, GRAVEL, DRAGON_EGG and the three ANVILs, where FC has the same machine (FireBlock.onPlace schedules
 * 30+nextInt(10) = 1.5.2 BlockFire tickRate, vanilla/server BlockFire.java:130-133; FallingBlock schedules at
 * FCBlockFalling's / BlockDragonEgg's delays) or removed the 1.20.1 one on purpose (FCBlockAnvil never falls,
 * so its empty updateTick cancels the 1.20.1 fall). LevelChunkTicks de-duplicates by (pos, block), so an FC and
 * a 1.20.1 schedule for one cell merge into one tick that this rule hands to exactly one engine.</p>
 *
 * <p><b>Exceptions</b> (1.20.1 already implements the identical 4.B rule and the port's FC object cannot):</p>
 * <ul>
 *   <li>WATER (8): random owner NONE, gate false. 4.B's id-8 random tick is the 1.5.2 flow step
 *       (vanilla/server BlockFlowing.java:60-197); the 1.20.1 fluid engine owns flow and the port's
 *       BlockFlowing has no body.</li>
 *   <li>LAVA (10): random owner FC with id 11 (FCBlockLavaStationary), gate forced true. All settled lava in
 *       4.B is id 11 (BlockFlowing.java:46-50); flow belongs to the 1.20.1 engine. Scheduled ticks keep id 10.
 *       LavaFluidMixin cancels the fluid random tick of every lava state whose random owner is FC
 *       ({@link #fcOwnsRandomTick}), so 1.20.1 fire spread does not run a second time.</li>
 *   <li>Redstone ore (73): random owner VANILLA, 1.20.1 flag kept. blocksList[73] is the unlit
 *       FCBlockRedstoneOre, which does not random-tick; 1.20.1 LIT plus unlight-on-random-tick is exactly
 *       4.B id 74 (RedStoneOreBlock.java:64-73).</li>
 *   <li>ICE (79): random owner VANILLA. FCBlockIce.updateTick melts at stored light &gt; 11 - opacity 3
 *       (Common FCBlockIce.java:53-60), which assumes 1.5.2 attenuation; on 1.20.1 light (ice getLightBlock 1)
 *       it would melt two levels early. 1.20.1 IceBlock.randomTick melts at neighbour light &gt;= 12, as 4.B
 *       does.</li>
 *   <li>Detector rails (every DetectorRailBlock: DETECTOR_RAIL 28 and BTW's 235 and 236, FcDetectorRailBlock): random
 *       owner NONE. 1.20.1's DetectorRailBlock presses and re-checks the rail (entityInside, its scheduled tick every
 *       20 ticks), with FC's trigger rule as its cart filter (DetectorRailBlockMixin); FCBlockDetectorRail's random
 *       re-check would only repeat it. Its scheduled owner is VANILLA by the override rule.</li>
 *   <li>Pressure plates (every BasePressurePlateBlock: 70, 72): random owner NONE. FC's plate body
 *       (Modern-Common BlockBasePressurePlate, the 1.5.2 mechanism) gives FCBlockPressurePlate a tick body, but
 *       1.20.1's plate already re-checks a pressed plate on its scheduled tick (BasePressurePlateBlock.tick, every
 *       getPressedTime), which FC's own presses schedule too; the scheduled owner is VANILLA by the override rule.
 *       FC's body still runs from FC's own contact (FC mobs, Entity.doBlockCollisions). FC's soulforged plate 230
 *       is a ProxyBlock and keeps FC's random tick.</li>
 *   <li>DEAD_BUSH (32): random owner NONE (maintainer decision D3). FCBlockDeadBush accepts only id 12 sand
 *       (Common FCBlockDeadBush.java:63-66) and its 4.B random tick is survival-only, so the gate would only
 *       delete 1.20.1 badlands bushes on red sand and terracotta.</li>
 *   <li>Leaves (18): random owner FC, gate = !PERSISTENT, replacing 1.20.1's DISTANCE==7 gate
 *       (LeavesBlock.java:42-44). FCBlockLeaves.updateTick is a no-op for player-placed leaves (bit 4). The
 *       scheduled owner stays VANILLA (DISTANCE).</li>
 *   <li>Stairs: no entry. The gate comes from the stair's own FC block (never random-ticks) and the
 *       scheduled owner is VANILLA because StairBlock overrides tick (its base delegation equals
 *       FCBlockStairs).</li>
 * </ul>
 *
 * <p><b>Nether portal (90)</b> has no entry: it falls under the general rule, which replaces the former
 * NetherPortalBlock class mixin (that hook would never fire behind the state-level dispatch, and both together
 * would roll FCBlockPortal.updateTick twice per selection). The portal random tick needs the 1.5.2 flag on
 * btw.modern.BlockPortal; without it the gate turns portal ticks off and the self-audit warns. BTW cancels the
 * 1.20.1 zombified piglin spawn because FCBlockPortal.updateTick
 * overrides BlockPortal.updateTick without calling super ("override to prevent pigmen spawning around portals
 * in the overworld", Common FCBlockPortal.java:41-54), so 4.B never ran the 1.5.2 pigman spawn
 * (vanilla/server BlockPortal.java:29-52) that 1.20.1's piglin spawn replaces. FCBlockPortal instead possesses
 * a creature near a surface-world portal and sets the nether-accessed progress flag.</p>
 *
 * <p><b>Post-1.5.2 plants</b> ({@link TwinRegistry}) grow at 4.B speeds, following each plant's nearest 1.5.2 / FC
 * relative. Their Blocks have no legacy id, so the general rule never sees them; two entries decide them first:</p>
 * <ul>
 *   <li>Twin states (acacia, cherry, dark oak saplings, the planted mangrove propagule, azaleas, beetroots, the
 *       torchflower crop, the pitcher crop's lower half, crimson and warped fungi): random owner FC with the twin's
 *       id, gate true (azaleas and fungi gain the random tick 1.20.1 never gave them; crops tick at every age as
 *       FC's crops do, their tick being their survival check too). Scheduled owner VANILLA: 1.20.1 keeps survival
 *       from shape updates. The FC body is the twin's: FCBlockSapling, FCBlockCarrot, FCBlockWheatLegacy,
 *       FCBlockMushroom.</li>
 *   <li>Paced states (bamboo shoot and bamboo, kelp, cave / weeping / twisting vines, sweet berry bush, the hanging
 *       mangrove propagule, chorus flower, and modded plants in #betterthanwolves:paced_like_sweet_berries): random owner {@link #PACED}, gate null (1.20.1's flag), with a pace: the
 *       1.20.1 body runs on that fraction of the random ticks, and never in the End unless the plant is exempt
 *       (chorus). The mean growth is FC's relative's.</li>
 * </ul>
 * <p>{@link #isFcGrowthGoverned} names the states whose growth FC governs (these, and FC-owned plants), for the
 * paths that bypass random ticks (bone meal, bees).</p>
 *
 * <p><b>Determinism.</b> {@link #decide} is a pure function of the BlockState and FC's static data. It runs at
 * every registry bake and Blocks.rebuildCache (server start, /reload) while chunk sections exist, and
 * LevelChunkSection counts ticking states with the cached flag (from worldgen threads too), so the gate must never
 * read gamerules, config or world state, and BlockStateBaseTickMixin writes the flag once per bake, with its final
 * value (no interim 1.20.1 value a section could count). doFireTick ({@link #HONOUR_DO_FIRE_TICK}) is applied at
 * dispatch time. Two inputs beyond FC's static table are fixed before any bake that matters: modded saplings an
 * integration registered during mod construction (TwinRegistry.registerModdedSapling, Dynamic Trees #027), and one
 * block tag, {@link TwinRegistry#PACED_LIKE_SWEET_BERRIES}: Holder.is answers false before tags bind, and
 * Blocks.rebuildCache re-runs decide after every tag bind (server start, /reload), so the cached owner always follows
 * the tags in force (INTEGRATIONS #032).</p>
 *
 * <p>FC failures inside a tick are logged once per throw site and never crash the server tick.</p>
 */
public final class TickPolicy {

    private static final Logger LOGGER = LogManager.getLogger("BTW-Tick");

    private TickPolicy() {}

    /** Owner: the 1.20.1 body runs (unmanaged states, exception entries, 1.20.1-owned scheduled ticks). */
    public static final byte VANILLA = 0;
    /** Owner: FC runs (RandomUpdateTick / updateTick) and the 1.20.1 body is cancelled. */
    public static final byte FC = 1;
    /** Owner: nobody random-ticks the state (gate false); a direct call passes through to 1.20.1. */
    public static final byte NONE = 2;
    /**
     * Owner: the 1.20.1 body runs on a fraction of the random ticks ({@link Decision#pace}), never in the End when
     * {@link Decision#paceEndBan} (post-1.5.2 plants FC has no class for, {@link TwinRegistry}).
     */
    public static final byte PACED = 3;

    /**
     * Maintainer decision D1: FC fire and FC lava honour /gamerule doFireTick. With it false, a scheduled FC fire
     * tick only reschedules itself (fire persists without spreading or burning out, as FireBlock.tick:129-130
     * does) and FC lava / fire random ticks are skipped. The default (true) is unchanged 4.B. Set to false for
     * 4.B parity, where FCBlockFire / FCBlockLavaStationary never read the rule.
     *
     * <p>The rule is a property of the FC object ({@link #fireTickSuppressed}): every btw.modern.BlockFire (fire 51
     * and FC's own fires, such as FCBlockFireStoked at 251, a ProxyBlock whose ticks ProxyBlock forwards) and FC
     * lava. Accepted D1 gaps, which still spread fire with the rule off: the burning ladder (FCBlockLadderOnFire
     * 1052, a ladder whose own updateTick calls FCBlockFire.CheckForFireSpreadFromLocation) and the campfire tile
     * entity (FCTileEntityCampfire, a tile-entity tick, not a block tick). Fire they start obeys the rule.</p>
     */
    public static final boolean HONOUR_DO_FIRE_TICK = true;

    static final int LAVA_STILL_ID = 11;
    static final int LEAVES_ID = 18;

    /**
     * Staging: legacy ids whose FC random-tick body or metadata round-trip is not yet faithful keep the 1.20.1
     * random tick (owner VANILLA, flag kept) until their prerequisite lands. Empty: every prerequisite of the
     * tick-ownership change has landed.
     */
    public static final Set<Integer> PENDING = Set.of();

    /**
     * Staging for {@code FC_SCHED}: ids listed here fall back to the override rule (1.20.1 keeps the scheduled
     * tick) until their FC body is faithful. Empty for the same reason as {@link #PENDING}.
     */
    public static final Set<Integer> PENDING_SCHED = Set.of();

    /**
     * One state's tick ownership.
     *
     * @param fcId   legacy id whose FC block runs the random tick (11 for every lava state)
     * @param schedId legacy id whose FC block runs a scheduled tick (the state's own id)
     * @param random random-tick owner ({@link #VANILLA}, {@link #FC}, {@link #NONE} or {@link #PACED})
     * @param sched  scheduled-tick owner ({@link #VANILLA} or {@link #FC})
     * @param gate   the random-tick flag to cache, or null to keep the 1.20.1 flag
     * @param rule   which rule decided (general, an exception name, twin:&lt;family&gt;, paced:&lt;plant&gt; or
     *               pending); for the self-audit
     * @param pace   {@link #PACED} only: the fraction of random ticks on which the 1.20.1 body runs (1 otherwise)
     * @param paceEndBan {@link #PACED} only: no growth in the End (FC's End ban)
     */
    public record Decision(int fcId, int schedId, byte random, byte sched, Boolean gate, String rule,
                           float pace, boolean paceEndBan) {

        public Decision(int fcId, int schedId, byte random, byte sched, Boolean gate, String rule) {
            this(fcId, schedId, random, sched, gate, rule, 1F, false);
        }
    }

    /** Blocks whose scheduled ticks FC owns although their 1.20.1 class overrides tick. Built on first use. */
    private static final class FcSched {
        static final Set<Block> BLOCKS = Set.of(Blocks.FIRE, Blocks.SAND, Blocks.GRAVEL, Blocks.DRAGON_EGG,
                Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL);
    }

    /**
     * The tick ownership of a state, or null when it is unmanaged (FC not ready, a ProxyBlock, an unmapped
     * block or an empty blocksList slot). Pure: reads only the state and FC's static block table.
     */
    public static Decision decide(BlockState s) {
        if (!BTWLifecycle.isFcReady()) return null;
        Block block = s.getBlock();
        if (block instanceof ProxyBlock) return null;

        // Post-1.5.2 plants (no legacy id): FC twins and paced plants, see the class javadoc and TwinRegistry.
        TwinRegistry.Family twin = TwinRegistry.familyOf(s);
        if (twin != null) {
            return new Decision(TwinRegistry.idOf(twin.kind), 0, FC, VANILLA, Boolean.TRUE, "twin:" + twin.label);
        }
        TwinRegistry.Pace pace = TwinRegistry.paceFor(s);
        if (pace != null) {
            return new Decision(0, 0, PACED, VANILLA, null, "paced:" + pace.name(), pace.pace(), pace.endBan());
        }

        int id = ProxyRegistry.getBlockId(block);
        if (id <= 0 || id >= btw.modern.Block.blocksList.length) return null;
        btw.modern.Block fc = btw.modern.Block.blocksList[id];
        if (fc == null) return null;

        byte sched = scheduledOwner(block, id);

        if (block == Blocks.WATER) {
            return new Decision(id, id, NONE, sched, Boolean.FALSE, "water");
        }
        if (block == Blocks.LAVA) {
            if (btw.modern.Block.blocksList[LAVA_STILL_ID] == null) {
                return new Decision(id, id, VANILLA, sched, null, "lava-unbuilt");
            }
            return new Decision(LAVA_STILL_ID, id, FC, sched, Boolean.TRUE, "lava");
        }
        if (block instanceof RedStoneOreBlock) {
            return new Decision(id, id, VANILLA, sched, null, "redstone-ore");
        }
        if (block instanceof IceBlock) {
            return new Decision(id, id, VANILLA, sched, null, "ice");
        }
        // FCMOD-INTEGRATION(1.20.1): fc_redstone: every detector rail (28, 235, 236) and the vanilla-id plates (70, 72)
        if (block instanceof DetectorRailBlock) {
            return new Decision(id, id, NONE, sched, Boolean.FALSE, "detector-rail");
        }
        if (block instanceof BasePressurePlateBlock) {
            return new Decision(id, id, NONE, sched, Boolean.FALSE, "pressure-plate");
        }
        if (block == Blocks.DEAD_BUSH) {
            return new Decision(id, id, NONE, sched, Boolean.FALSE, "dead-bush");
        }
        if (PENDING.contains(id)) {
            return new Decision(id, id, VANILLA, sched, null, "pending");
        }
        if (block instanceof LeavesBlock && id == LEAVES_ID) {
            return new Decision(id, id, FC, sched, !s.getValue(LeavesBlock.PERSISTENT), "leaves");
        }

        boolean gate = fc.getTickRandomly() && hasOwnTickBody(fc.getClass());
        return new Decision(id, id, gate ? FC : NONE, sched, gate, "general");
    }

    /** Scheduled-tick owner of a mapped block (see the class javadoc). */
    private static byte scheduledOwner(Block block, int id) {
        if (isFcScheduled(block) && !PENDING_SCHED.contains(id)) return FC;
        return overridesTick(block.getClass()) ? VANILLA : FC;
    }

    /** True for the FC_SCHED blocks (FC owns their scheduled ticks although the 1.20.1 class overrides tick). */
    static boolean isFcScheduled(Block block) {
        return FcSched.BLOCKS.contains(block);
    }

    // ================================================================
    // Dispatch (called from BlockStateRandomTickMixin / BlockStateBaseTickMixin, D1 also from ProxyBlock)
    // ================================================================

    /**
     * Runs FC's random tick for {@code state}, whose random owner is FC. Returns false only when the FC block is
     * missing, so the caller lets 1.20.1 run instead of dropping the tick.
     */
    public static boolean runRandom(BlockState state, int fcId, ServerLevel level, BlockPos pos, RandomSource random) {
        btw.modern.Block fc = fcBlock(fcId);
        if (fc == null) return false;
        if (fireTickSuppressed(fc, level)) return true;
        LightPolicy.relightIfStale(state, level, pos);
        boolean cropEvents = firesCropGrowEvents(state.getBlock());
        // The 1.20.1 body this tick replaces asks the same event before it grows (CropBlock.java:72-74, StemBlock,
        // CactusBlock, SugarCaneBlock, NetherWartBlock, CocoaBlock): a mod's DENY skips the FC body, as it skips
        // 1.20.1's growth. FC's body is growth and the plant's own upkeep, so the whole tick is the growth attempt.
        if (cropEvents && !ForgeHooks.onCropsGrowPre(level, pos, state, true)) return true;
        try {
            fc.RandomUpdateTick(WorldBridge.getOrCreate(level), pos.getX(), pos.getY(), pos.getZ(),
                    new Random(random.nextLong()));
        } catch (Throwable t) {
            warnOnce("RANDOM-TICK", fcId, fc, pos, t);
        }
        if (cropEvents) {
            BlockState after = level.getBlockState(pos);
            if (after != state && !after.isAir()) {
                ForgeHooks.onCropsGrowPost(level, pos, state);   // the plant grew (or bore fruit), not died
            }
        }
        return true;
    }

    /**
     * Runs FC's scheduled tick for a state whose scheduled owner is FC. Returns false only when the FC block is
     * missing, so the caller lets 1.20.1 run instead of dropping the tick.
     */
    public static boolean runScheduled(int fcId, ServerLevel level, BlockPos pos, RandomSource random) {
        btw.modern.Block fc = fcBlock(fcId);
        if (fc == null) return false;
        if (fireTickSuppressed(fc, level)) {
            rescheduleSuppressedFire(fc, level, pos);
            return true;
        }
        try {
            fc.updateTick(WorldBridge.getOrCreate(level), pos.getX(), pos.getY(), pos.getZ(),
                    new Random(random.nextLong()));
        } catch (Throwable t) {
            warnOnce("TICK", fcId, fc, pos, t);
        }
        return true;
    }

    /**
     * True when FC owns {@code state}'s random tick (its random owner is {@link #FC}). LavaFluidMixin cancels the
     * lava fluid random tick exactly then: FC's lava (id 11) ran from the block path. A state whose decision failed
     * (owner VANILLA) keeps 1.20.1's fluid tick, so its lava still starts fires.
     */
    public static boolean fcOwnsRandomTick(BlockState state) {
        return (Object) state instanceof FcBlockStateAccess access && access.btw$randomOwner() == FC;
    }

    /**
     * The roll of a {@link #PACED} state's random tick: true lets the 1.20.1 body run. False in the End for a plant
     * under FC's End ban, else true on {@code pace} of the ticks (one {@code nextFloat} of the tick's random).
     */
    public static boolean pacedRollPasses(FcPaceAccess paced, ServerLevel level, RandomSource random) {
        if (paced.btw$paceEndBan() && level.dimension() == net.minecraft.world.level.Level.END) return false;
        return random.nextFloat() < paced.btw$growthPace();
    }

    /**
     * True when FC governs the growth of {@code state}: an FC-owned growing plant (a random owner of {@link #FC} whose
     * FC block is a plant, vine or cactus: saplings, crops, stems, mushrooms, reeds, cactus, cocoa, nether wart, vines,
     * tall grass, flowers, lily pads), a ProxyBlock whose FC block is one (FC's own crops, hemp, weeds), and every
     * twin or paced state of a post-1.5.2 plant ({@link TwinRegistry#governs}: pitcher upper halves and hanging
     * propagules included). Growth that bypasses the random tick (bone meal, villagers, bees) must follow FC's rule
     * for these: 4.B bone meal never grows a plant, it fertilizes the soil. A pure function of the state, its cached
     * owners (BlockStateBaseTickMixin) and FC's static data; false before FC is ready.
     */
    public static boolean isFcGrowthGoverned(BlockState state) {
        if (state == null || !BTWLifecycle.isFcReady()) return false;
        Block block = state.getBlock();
        if (block instanceof ProxyBlock) return isGrowingPlant(ProxyRegistry.getFcBlock(block));
        if (TwinRegistry.governs(state)) return true;
        // A body segment follows its head's rule: 1.20.1 bone meal on a body grows the head
        // (GrowingPlantBodyBlock.performBonemeal) and gives cave-vine berries
        Block head = growingPlantHead(block);
        if (head != null) return isFcGrowthGoverned(head.defaultBlockState());
        if (!((Object) state instanceof FcBlockStateAccess access)) return false;
        byte owner = access.btw$randomOwner();
        if (owner == PACED) return true;
        return owner == FC && isGrowingPlant(fcBlock(access.btw$randomFcId()));
    }

    /** The head block of a 1.20.1 growing-plant body (kelp, weeping, twisting and cave vines), else null. */
    private static Block growingPlantHead(Block body) {
        if (body == Blocks.KELP_PLANT) return Blocks.KELP;
        if (body == Blocks.WEEPING_VINES_PLANT) return Blocks.WEEPING_VINES;
        if (body == Blocks.TWISTING_VINES_PLANT) return Blocks.TWISTING_VINES;
        if (body == Blocks.CAVE_VINES_PLANT) return Blocks.CAVE_VINES;
        return null;
    }

    /** FC's plant materials (1.5.2 Material.plants, vine, cactus): the blocks a 4.B growth rule applies to. */
    private static boolean isGrowingPlant(btw.modern.Block fc) {
        if (fc == null) return false;
        btw.modern.Material m = fc.blockMaterial;
        return m == btw.modern.Material.plants || m == btw.modern.Material.vine || m == btw.modern.Material.cactus;
    }

    /**
     * True for the 1.20.1 plants whose own randomTick fires Forge's CropGrowEvent (the onCropsGrowPre callers:
     * CropBlock, StemBlock, CactusBlock, SugarCaneBlock, NetherWartBlock, CocoaBlock, SweetBerryBushBlock,
     * BambooStalkBlock, ChorusFlowerBlock, GrowingPlantHeadBlock). Only FC-owned states reach the check.
     */
    static boolean firesCropGrowEvents(Block block) {
        return block instanceof CropBlock || block instanceof StemBlock || block instanceof CactusBlock
                || block instanceof SugarCaneBlock || block instanceof NetherWartBlock || block instanceof CocoaBlock
                || block instanceof SweetBerryBushBlock || block instanceof BambooStalkBlock
                || block instanceof ChorusFlowerBlock || block instanceof GrowingPlantHeadBlock;
    }

    private static btw.modern.Block fcBlock(int id) {
        return id > 0 && id < btw.modern.Block.blocksList.length ? btw.modern.Block.blocksList[id] : null;
    }

    /**
     * D1 ({@link #HONOUR_DO_FIRE_TICK}): FC fire (every btw.modern.BlockFire: fire 51, stoked fire 251, ...) and FC
     * lava skip their ticks while doFireTick is false. Read at dispatch time, never in the gate. Also used by
     * ProxyBlock.tick / randomTick, the tick path of FC's own fire blocks.
     */
    public static boolean fireTickSuppressed(btw.modern.Block fc, ServerLevel level) {
        return HONOUR_DO_FIRE_TICK && fc != null
                && (fc instanceof btw.modern.BlockFire || fc.blockMaterial == btw.modern.Material.lava)
                && !level.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK);
    }

    /**
     * The scheduled tick a suppressed FC fire skipped rescheduled, so the fire keeps ticking until the rule is back
     * on: FireBlock.tick:129 reschedules before reading the rule, and FCBlockFire.updateTick reschedules itself at
     * tickRate + nextInt(10) as part of the body that is skipped here (Server FCBlockFire.java:79; FCBlockFireStoked
     * the same at its own rate, :93-94). FC lava has no scheduled tick body to keep alive.
     */
    public static void rescheduleSuppressedFire(btw.modern.Block fc, ServerLevel level, BlockPos pos) {
        if (!(fc instanceof btw.modern.BlockFire)) return;
        int rate = 30;   // 1.5.2 BlockFire.tickRate (vanilla/server BlockFire.java:130-133)
        try {
            rate = Math.max(1, fc.tickRate(WorldBridge.getOrCreate(level)));
        } catch (Throwable ignored) {
            // keep the 1.5.2 fire rate
        }
        level.scheduleTick(pos, level.getBlockState(pos).getBlock(), rate + level.random.nextInt(10));
    }

    // ================================================================
    // Class analysis (cached per class)
    // ================================================================

    private static final Map<Class<?>, Boolean> OWN_TICK_BODY = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Boolean> TICK_OVERRIDES = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Boolean> RANDOM_TICK_OVERRIDES = new ConcurrentHashMap<>();

    /**
     * True when the runtime FC class declares {@code updateTick} or {@code RandomUpdateTick} below
     * btw.modern.Block. FC classes are not obfuscated; they are remapped btw.api -> btw.modern, so the
     * signature uses btw.modern.World. A class whose 4.B tick reaches only a Modern-Common stub stays off.
     */
    static boolean hasOwnTickBody(Class<?> c) {
        Boolean cached = OWN_TICK_BODY.get(c);
        if (cached != null) return cached;
        boolean result;
        try {
            result = declaredBelowBase(c, "updateTick") || declaredBelowBase(c, "RandomUpdateTick");
        } catch (Throwable t) {
            // Cannot happen for a loaded FC block class; keep 4.B's flag rather than silently dropping its ticks.
            LOGGER.warn("Could not inspect the tick methods of {}; treating it as having a tick body", c.getName(), t);
            result = true;
        }
        OWN_TICK_BODY.put(c, result);
        return result;
    }

    private static boolean declaredBelowBase(Class<?> c, String name) throws NoSuchMethodException {
        return c.getMethod(name, btw.modern.World.class, int.class, int.class, int.class, Random.class)
                .getDeclaringClass() != btw.modern.Block.class;
    }

    /** True when the 1.20.1 block class (or a superclass below BlockBehaviour) overrides tick (m_213897_). */
    static boolean overridesTick(Class<?> c) {
        return overrides(c, MethodNames.TICK, TICK_OVERRIDES);
    }

    /** True when the 1.20.1 block class (or a superclass below BlockBehaviour) overrides randomTick (m_213898_). */
    static boolean overridesRandomTick(Class<?> c) {
        return overrides(c, MethodNames.RANDOM_TICK, RANDOM_TICK_OVERRIDES);
    }

    /** Runtime names of BlockBehaviour.tick / randomTick (SRG in production, official names in dev). */
    private static final class MethodNames {
        static final String TICK = resolve("m_213897_", "tick");
        static final String RANDOM_TICK = resolve("m_213898_", "randomTick");

        /**
         * The runtime name: Forge's mapping lookup first, then each name declared directly on BlockBehaviour (the
         * official name in dev, the SRG name in production). Null only when neither exists; every mapped class then
         * counts as overriding it, which keeps 1.20.1's scheduled ticks (an unclassifiable class may override tick).
         */
        private static String resolve(String srg, String official) {
            Throwable failure;
            try {
                return ObfuscationReflectionHelper.findMethod(BlockBehaviour.class, srg,
                        BlockState.class, ServerLevel.class, BlockPos.class, RandomSource.class).getName();
            } catch (Throwable t) {
                failure = t;
            }
            for (String name : new String[] {official, srg}) {
                try {
                    BlockBehaviour.class.getDeclaredMethod(name, BlockState.class, ServerLevel.class, BlockPos.class,
                            RandomSource.class);
                    LOGGER.warn("Resolved BlockBehaviour.{} by its declared name {} (mapping lookup failed: {})",
                            srg, name, failure.toString());
                    return name;
                } catch (Throwable ignored) {
                    // try the next name
                }
            }
            LOGGER.error("Could not resolve BlockBehaviour.{}; every mapped class counts as overriding it", srg, failure);
            return null;
        }
    }

    private static boolean overrides(Class<?> start, String name, Map<Class<?>, Boolean> cache) {
        Boolean cached = cache.get(start);
        if (cached != null) return cached;
        boolean result = false;
        if (name == null) {
            result = true;
        } else {
            for (Class<?> c = start; c != null && c != BlockBehaviour.class; c = c.getSuperclass()) {
                try {
                    c.getDeclaredMethod(name, BlockState.class, ServerLevel.class, BlockPos.class, RandomSource.class);
                    result = true;
                    break;
                } catch (NoSuchMethodException ignored) {
                    // not declared here; keep walking up
                } catch (Throwable t) {
                    // Unreadable class: keep the 1.20.1 owner, the conservative choice.
                    LOGGER.warn("Could not inspect {} on {}; treating it as overridden", name, c.getName(), t);
                    result = true;
                    break;
                }
            }
        }
        cache.put(start, result);
        return result;
    }

    // ================================================================
    // Logging
    // ================================================================

    /** Tick failure sites already reported by {@link #warnOnce}. */
    private static final Set<String> LOGGED_FAILURES = ConcurrentHashMap.newKeySet();

    /**
     * One WARN with its stack trace per distinct throw site (stage, legacy id, exception class and top frame),
     * as ProxyBlock.warnTickFailureOnce: a block failing every tick neither floods the log nor hides.
     */
    private static void warnOnce(String stage, int id, btw.modern.Block fc, BlockPos pos, Throwable t) {
        StackTraceElement[] frames = t.getStackTrace();
        String site = stage + '|' + id + '|' + t.getClass().getName() + '@' + (frames.length > 0 ? frames[0] : "?");
        if (LOGGED_FAILURES.add(site)) {
            LOGGER.warn("[{}] {} (id {}) at {} threw {}", stage, fc.getClass().getSimpleName(), id, pos, t.toString(), t);
        }
    }

    /** initCache must never throw from a registry bake: the failing state keeps its 1.20.1 values. */
    public static void warnDecideFailure(BlockState s, Throwable t) {
        String site = "DECIDE|" + s.getBlock().getClass().getName() + '|' + t.getClass().getName();
        if (LOGGED_FAILURES.add(site)) {
            LOGGER.warn("Tick policy decision failed for {}; it keeps its 1.20.1 tick behaviour", s, t);
        }
    }
}
