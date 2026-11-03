package btw.forge;

import btw.forge.removed.RemovedContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.DetectorRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.RedStoneOreBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FCMOD-INTEGRATION(1.20.1): hook_ownership
 *
 * <p><b>Feature:</b> 1.20.1 per-class block hooks. 1.20.1 moves block behaviour into overrides of
 * {@code use}, {@code attack}, {@code onPlace}, {@code onRemove}, {@code neighborChanged}, {@code entityInside}, the
 * signal methods, {@code stepOn} and {@code fallOn} on the block class, reached through the BlockStateBase forwarders
 * (BlockBehaviour.java:723, :749, :753, :765, :777, :781) and Block (Entity.java:676, :1128).</p>
 *
 * <p><b>Conflict with FC:</b> the port ran FC's 1.5.2 hooks only from the base bodies (BlockBehaviorMixin,
 * BlockMixin), so whether FC ran depended on 1.20.1's class layout: 231 override-without-super pairs bypass FC, 108
 * of them hooks 4.B changed (hookaudit scan). Nothing could say "FC replaces 1.20.1 here" (anvil, golems) or "1.20.1
 * owns this hook and FC must not run although FC declares it" (FC block-entity bodies on a vanilla block entity,
 * which crashed every beacon and ender chest removal; beds; door, trapdoor and gate redstone by ruling).</p>
 *
 * <p><b>Integration pattern:</b> D (FC-extend), shaped like {@link TickPolicy}: one owner per hook per state,
 * decided once at {@code initCache} from 4.B and cached on the state ({@link Access}, by
 * {@code btw.forge.mixin.BlockStateHookMixin}). Owners, two bits per hook:</p>
 * <ul>
 *   <li>{@link #UNDECIDED}: no decision (FC not ready at the bake, a ProxyBlock, an unmapped block, a failed decision):
 *       the executors keep their dynamic lookup, so nothing regresses on a stale cache;</li>
 *   <li>{@link #NOTIFY}: FC runs where the 1.20.1 chain reaches the base body (non-overriding or super-calling classes),
 *       today's behaviour made explicit;</li>
 *   <li>{@link #FC}: FC replaces 1.20.1 on the server at the BlockStateBase forwarder and the 1.20.1 body is cancelled
 *       (USE and PLACE are enforced there, the only hooks with an FC row; on the other hooks FC runs where the base
 *       body is reached, as NOTIFY);</li>
 *   <li>{@link #VANILLA}: FC's block never runs for that hook, not even through a super call (USE: the held FC item,
 *       Stage 2 of {@link #activate}, still runs where the base body is reached, as 4.B ran it whatever the block
 *       did).</li>
 * </ul>
 * <p>{@link #decide} rules, in order (hook delta B5): (1) not ready, a ProxyBlock or legacy id 0: null; (2) a
 * RemovedContent T1 block: VANILLA (D6); (3) the block-entity guard: USE and REMOVE of a state whose FC block is a
 * {@code btw.modern.BlockContainer} are VANILLA, because its block entity is always 1.20.1's and FC would cast the
 * TileEntityBridge WorldBridge hands it (WorldBridge.getBlockTileEntity, WorldBridge.java:1002); (4) RULED and ADAPTER
 * rows, permanent: VANILLA; (5) PENDING metadata prerequisites: VANILLA until the codec lands (staging like
 * TickPolicy.PENDING); (6) EQUIVALENT: NOTIFY; (7) the general rule: FC iff the 1.20.1 class overrides the hook and
 * the runtime FC class declares it in FC code (below btw.modern), else NOTIFY. RESIST (explosions, fc_blast_rules) is
 * decided before them: FC when the runtime FC class declares the positional getExplosionResistance(Entity, World, int,
 * int, int) in FC code (FCBlockStone, FCBlockOre: the strata; ExplosionDamageCalculatorMixin answers it to 1.20.1's
 * explosion rays), else NOTIFY (BlockMixin's position-less lookup).</p>
 *
 * <p><b>Rebalance:</b> rows, each from a ruling or 4.B:</p>
 * <ul>
 *   <li>RULED(3), ruling 3 (2026-10-19): beds USE. 1.20.1 sleep (night skip, messages, Nether/End explosion);
 *       {@link BedSpawnPolicy} keeps the bed from setting the spawn.</li>
 *   <li>RULED(4), ruling 4: door, trapdoor and fence-gate NEIGHBOR (64, 71, 96, 107): 1.20.1 redstone. The row only
 *       stops rule 7 from picking FC: DoorBlock, TrapDoorBlock and FenceGateBlock.neighborChanged never call the base
 *       body, and FCBlockDoor.onNeighborBlockChange would delete an iron door's upper half (delta A5).</li>
 *   <li>RULED(5), vanilla_pair_drops (ruling 2026-10-20): NEIGHBOR of every {@link VanillaPairDrops#isRuled} block.
 *       1.20.1 owns a bed's and a door's partner handling ({@code updateShape}), which 1.5.2 did in
 *       onNeighborBlockChange; a later 1.5.2 BlockBed port can then never pop and FC-drop beside it (beddoor Q7).
 *       Doors are RULED(4) first.</li>
 *   <li>ADAPTER(D16b): fence-gate USE. FenceGateBlock.use keeps its game event and per-wood sounds
 *       (FenceGateBlock.java:141-142); FenceGateBlockMixin adds 4.B's neighbour notify (Client FCBlockFenceGate.java:57,
 *       :69).</li>
 *   <li>ADAPTER(D9b): farmland FALL. One trample roll, on 1.20.1's path with FC's threshold (FarmBlockMixin), instead
 *       of 1.20.1's roll and then FC's (Entity.java:3426-3430, Server FCBlockFarmlandBase.java:80-89).</li>
 *   <li>FC by the general rule: the three anvils' USE (FC's 3x3 crafting station, D2(c), with an empty-handed sneak
 *       left to AnvilMenu, R15) and the carved pumpkin's and jack o'lantern's PLACE (no golems, D3(a); a jack
 *       o'lantern placed now goes out next to water, Client FCBlockJackOLantern.java:54-64).</li>
 * </ul>
 *
 * <p><b>Injection strategy:</b> no injector here. BlockStateHookMixin caches {@link #decide} at {@code initCache}
 * TAIL and enforces FC rows at the {@code use} and {@code onPlace} forwarders (cancellable HEAD injects at priority
 * 1100, after other mods' HEAD vetoes); BlockBehaviorMixin and BlockMixin, the NOTIFY executors, read the owner of
 * the state they run for ({@link #notifyRuns}), so StairBlock's base delegation gets the base block's owner. Every
 * executor catches FC throws ({@link #warnOnce}): an FC failure answers the 1.20.1 result (an FC-owned USE included:
 * the anvil's AnvilMenu) and never escapes LevelChunk.setBlockState or a packet handler; only an FC-owned PLACE still
 * cancels the 1.20.1 body on a throw, so golems stay blocked. A throw on a click discards FC's pending inventory edits
 * (INV-RESYNC). {@link #decide} is pure, as TickPolicy.decide (TickPolicy.java:131-140):
 * it reads the state, FC's static block table, the 1.20.1 and FC classes and the static RemovedContent table; it
 * never touches Blocks or Items at class init (initCache runs inside Blocks' static init, before FC is ready).</p>
 *
 * <p><b>Alternatives considered and rejected:</b> a table of FC rows per block (every future mapping or override
 * would need a row; the general rule plus the self-audit's WARN for an unclassified pair covers them); cancellable
 * injects on every hook (each allocates a CallbackInfo per call on hot paths such as neighborChanged; injects exist
 * only where a row resolves to FC); a NONE owner (cancel 1.20.1 and run nothing: its only users, wooden-door NEIGHBOR
 * and the barricade twins, were withdrawn by ruling 4).</p>
 */
public final class HookPolicy {

    private static final Logger LOGGER = LogManager.getLogger("BTW-Hook");

    private HookPolicy() {}

    // ---- hooks: two owner bits each, hook h at bits 2h and 2h+1 of the packed int

    /** use -&gt; onBlockActivated. */
    public static final int USE = 0;
    /** attack -&gt; onBlockClicked. */
    public static final int ATTACK = 1;
    /** onPlace -&gt; onBlockAdded. */
    public static final int PLACE = 2;
    /** onRemove -&gt; breakBlock. */
    public static final int REMOVE = 3;
    /** neighborChanged -&gt; onNeighborBlockChange. */
    public static final int NEIGHBOR = 4;
    /** entityInside -&gt; onEntityCollidedWithBlock. */
    public static final int INSIDE = 5;
    /** getSignal / getDirectSignal / isSignalSource -&gt; isProvidingWeakPower / isProvidingStrongPower / canProvidePower. */
    public static final int SIGNAL = 6;
    /** stepOn -&gt; onEntityWalking. */
    public static final int STEP = 7;
    /** fallOn -&gt; onFallenUpon. */
    public static final int FALL = 8;
    /**
     * getExplosionResistance: FC when FC's class reads its position for it (ExplosionDamageCalculatorMixin, via
     * BlastRules.positionalResistance), else NOTIFY (BlockMixin's position-less lookup).
     */
    public static final int RESIST = 9;
    public static final int HOOKS = 10;

    private static final String[] HOOK_NAMES = {"USE", "ATTACK", "PLACE", "REMOVE", "NEIGHBOR", "INSIDE", "SIGNAL",
            "STEP", "FALL", "RESIST"};

    // ---- owners

    /** No decision: the executors keep their dynamic lookup. */
    public static final int UNDECIDED = 0;
    /** FC runs where the 1.20.1 chain reaches the base body. */
    public static final int NOTIFY = 1;
    /** FC replaces 1.20.1 on the server (enforced for USE and PLACE by BlockStateHookMixin). */
    public static final int FC = 2;
    /** FC's block never runs for the hook (USE: the held FC item still runs, {@link #activate} Stage 2). */
    public static final int VANILLA = 3;

    /**
     * The per-state owners cached at {@code BlockStateBase.initCache} by {@code btw.forge.mixin.BlockStateHookMixin},
     * which implements this interface on every BlockState (a mixin duck interface: read it through
     * {@link #cachedOwners}).
     */
    public interface Access {
        /** The packed owners ({@link #owner}), 0 (every hook {@link #UNDECIDED}) before a decision. */
        int btw$hookOwners();
    }

    /**
     * One state's hook ownership.
     *
     * @param fcId   legacy id of the state's FC block
     * @param owners the packed owners, read with {@link #owner(int)}
     * @param rules  the rule that decided each hook (general, removed(T1), te-guard, RULED(n), ADAPTER(..),
     *               pending:.., equivalent:.., positional, lookup); for the self-audit
     */
    public record Decision(int fcId, int owners, String[] rules) {

        public int owner(int hook) {
            return HookPolicy.owner(owners, hook);
        }

        public String rule(int hook) {
            return rules[hook];
        }
    }

    /**
     * Legacy ids each RULED / ADAPTER label stands for today, for the self-audit (delta B9: WARN when a ruled state
     * no longer maps to its id, or an id loses its row). RULED(5) is seen on the beds only: the doors are RULED(4)
     * first.
     */
    public static final Map<String, Set<Integer>> RULED_IDS = Map.of(
            "RULED(3)", Set.of(26),
            "RULED(4)", Set.of(64, 71, 96, 107),
            "RULED(5)", Set.of(26),
            "ADAPTER(D16b)", Set.of(107),
            "ADAPTER(D9b)", Set.of(60));

    /**
     * The pairs the general rule is expected to hand to FC (legacy id -&gt; hook mask), for the self-audit: it WARNs on
     * an FC row not listed here, and on a listed row no block of its id carries (an unresolved 1.20.1 method name or a
     * drifted FC signature would otherwise turn the anvils and pumpkins back to NOTIFY without a word).
     */
    public static final Map<Integer, Integer> EXPECTED_FC = Map.of(
            145, 1 << USE,       // ANVIL, CHIPPED_ANVIL, DAMAGED_ANVIL (Client FCBlockAnvil.java:134-150)
            86, 1 << PLACE,      // CARVED_PUMPKIN (Common FCBlockPumpkinCarved.java:39-43)
            91, 1 << PLACE,      // JACK_O_LANTERN (Client FCBlockJackOLantern.java:54-64)
            138, 1 << USE);      // BEACON: FCBlockBeacon.onBlockActivated (Client FCBlockBeacon.java:69-113), fc_beacon ruling 2026-10-21

    // ================================================================
    // Decision
    // ================================================================

    /**
     * The hook ownership of a state, or null when it is unmanaged (FC not ready, a ProxyBlock, a block without a
     * legacy id, an empty blocksList slot). Pure: reads only the state, its classes and FC's static data.
     */
    public static Decision decide(BlockState s) {
        if (!BTWLifecycle.isFcReady()) return null;
        Block block = s.getBlock();
        if (block instanceof ProxyBlock) return null;
        int id = ProxyRegistry.getBlockId(block);
        if (id <= 0 || id >= btw.modern.Block.blocksList.length) return null;
        btw.modern.Block fc = btw.modern.Block.blocksList[id];
        if (fc == null) return null;

        String removed = removedRule(block);
        int owners = 0;
        String[] rules = new String[HOOKS];
        for (int h = 0; h < HOOKS; h++) {
            int owner;
            String rule;
            if (h == RESIST) {
                // FCMOD-INTEGRATION(1.20.1): fc_blast_rules: 4.B's doExplosionA reads the positional overload (vanilla/server Explosion.java:107-108)
                boolean positional = fcDeclaresPositionalResistance(fc.getClass());
                owner = positional ? FC : NOTIFY;
                rule = positional ? "positional" : "lookup";
            } else if (removed != null) {
                owner = VANILLA;
                rule = removed;
            } else if ((h == USE || h == REMOVE) && isTeGuarded(block, fc)) {
                owner = VANILLA;
                rule = "te-guard";
            } else if ((rule = ruledRule(block, h)) != null) {
                owner = VANILLA;
            } else if ((rule = pendingRule(id, h)) != null) {
                owner = VANILLA;
            } else if ((rule = equivalentRule(block, h)) != null) {
                owner = NOTIFY;
            } else {
                rule = "general";
                owner = generalRuleGivesFc(block.getClass(), fc.getClass(), h) ? FC : NOTIFY;
            }
            owners |= owner << (h << 1);
            rules[h] = rule;
        }
        return new Decision(id, owners, rules);
    }

    /** Rule 2 (D6): a 4.B-stubbed block (RemovedContent T1) keeps every hook 1.20.1's. */
    private static String removedRule(Block block) {
        RemovedContent.Entry e = RemovedContent.forBlock(block);
        return e != null && e.tier == RemovedContent.Tier.T1_STUBBED ? "removed(T1)" : null;
    }

    /**
     * Rule 3, the block-entity guard: a non-ProxyBlock state whose FC block is a btw.modern.BlockContainer (Modern-Common
     * BlockContainer.java:3-9) always holds a 1.20.1 block entity, and WorldBridge hands FC a TileEntityBridge for it, so
     * FC's USE and REMOVE bodies, which cast it, never run (Client FCBlockBeacon.java:42, :69; FCBlockEnderChest.java:39,
     * :104; Common FCBlockNote.java:36). The guard keys on the FC class alone, never on the block entity in the
     * world, so it does not lift by itself: when Phase 4 bridges FC tile entities onto 1.20.1 block entities, this test
     * must narrow with it (for instance to a state whose block entity is FC's). PLACE, NEIGHBOR and INSIDE are not
     * guarded: FCBlockEndPortal.onBlockAdded sets BTW's End-progress flag. Only Stage 1 of a click is guarded: the
     * held FC item still runs (BlockBehaviorMixin.btw$use, {@link #activate}).
     */
    public static boolean isTeGuarded(btw.modern.Block fc) {
        return fc instanceof btw.modern.BlockContainer;
    }

    /**
     * Rule 3 for a cell of {@code mc}: the class guard above, lifted for a block whose 1.20.1 block entity carries FC's
     * tile entity ({@link FcBackedBlockEntities#backs}: the beacon), because WorldBridge then hands FC its own tile
     * entity; rules 4-7 decide. The hook rule (decide, BlockBehaviorMixin's REMOVE and USE executors) uses this one; the
     * one-argument form keeps the class meaning ("FC's block expects a tile entity") for other readers.
     */
    // FCMOD-INTEGRATION(1.20.1): fc_beacon: the block-entity guard lifts for FC-backed block entities
    public static boolean isTeGuarded(Block mc, btw.modern.Block fc) {
        return isTeGuarded(fc) && !FcBackedBlockEntities.backs(mc);
    }

    /** Rule 4, permanent rows (P4 never flips them): rulings 3, 4 and vanilla_pair_drops, and the two adapters. */
    private static String ruledRule(Block block, int hook) {
        if (hook == USE && block instanceof BedBlock) return "RULED(3)";
        if (hook == NEIGHBOR && (block instanceof DoorBlock || block instanceof TrapDoorBlock
                || block instanceof FenceGateBlock)) {
            return "RULED(4)";
        }
        if (hook == NEIGHBOR && VanillaPairDrops.isRuled(block)) return "RULED(5)";
        if (hook == USE && block instanceof FenceGateBlock) return "ADAPTER(D16b)";
        if (hook == FALL && block instanceof FarmBlock) return "ADAPTER(D9b)";
        return null;
    }

    /** Rule 5, staging: FC hooks whose metadata round-trip is not ported yet stay 1.20.1's (P4 flips them). */
    private static String pendingRule(int id, int hook) {
        if (hook == USE) {
            switch (id) {
                case 64, 71: return "pending:door-codec";        // FCBlockDoor reads bit 8 (Client FCBlockDoor.java:33-66)
                case 92: return "pending:cake-bites";            // FCBlockCake eat state (Client FCBlockCake.java:67)
                case 58: return "pending:workbench(D12)";        // Common FCBlockWorkbench.java:43-54
                case 140: return "pending:pot-metadata";         // Client FCBlockFlowerPot.java:38
                default: return null;
            }
        }
        return null;
    }

    /** Rule 6: 1.20.1 already runs 4.B's effect, so FC keeps today's NOTIFY. */
    private static String equivalentRule(Block block, int hook) {
        // FCBlockStairs delegates as StairBlock does (Client FCBlockStairs.java:43-125, StairBlock.java:117-160); the
        // base state's own owner applies at the stair cell through baseState.use / onRemove
        if (block instanceof StairBlock) return "equivalent:stairs";
        // FCBlockRedstoneOre.SetGlowing = RedStoneOreBlock.interact (TickPolicy makes the same call); use differs only
        // in its result (RedStoneOreBlock.java:52-53 against Client FCBlockRedstoneOre.java:55-60)
        if (block instanceof RedStoneOreBlock && (hook == USE || hook == ATTACK || hook == STEP)) {
            return "equivalent:redstone-ore";
        }
        // FCBlockFalling.onBlockAdded only schedules the fall check, as FallingBlock.onPlace does (Common
        // FCBlockFalling.java:39); TickPolicy runs that tick (FC_SCHED)
        if (block instanceof FallingBlock && hook == PLACE) return "equivalent:falling";
        // the lamp's timing is equal; its notify is P3(iv)
        if (block instanceof RedstoneLampBlock && hook == NEIGHBOR) return "equivalent:lamp";
        // FCMOD-INTEGRATION(1.20.1): fc_redstone: FCBlockDetectorRail's contact body (Client FCBlockDetectorRail.java:42-52)
        // is DetectorRailBlock.entityInside with FC's cart rule, which DetectorRailBlockMixin gives 1.20.1's own check; the
        // rail metadata codec (WorldBridge) lets FC read the result (28, and BTW's 235 and 236, FcDetectorRailBlock)
        if (block instanceof DetectorRailBlock && hook == INSIDE) return "equivalent:detector-rail";
        return null;
    }

    // ================================================================
    // Executor helpers (BlockStateHookMixin, BlockBehaviorMixin, BlockMixin)
    // ================================================================

    /** The owner of {@code hook} in a packed owners int. */
    public static int owner(int owners, int hook) {
        return (owners >>> (hook << 1)) & 3;
    }

    /** A state's cached owners, or 0 (every hook {@link #UNDECIDED}) when BlockStateHookMixin is not applied. */
    public static int cachedOwners(BlockState s) {
        return (Object) s instanceof Access access ? access.btw$hookOwners() : 0;
    }

    /**
     * True when a NOTIFY executor (BlockBehaviorMixin, BlockMixin: FC inside the 1.20.1 base body) runs FC for
     * {@code hook} of {@code s}: NOTIFY, UNDECIDED (today's lookup), and FC on a hook without a forwarder inject (the
     * base body is reached only through a super call, where FC ran before). Never for VANILLA, nor for FC on USE or
     * PLACE, which BlockStateHookMixin ran at the forwarder.
     */
    public static boolean notifyRuns(BlockState s, int hook) {
        int o = owner(cachedOwners(s), hook);
        return o == NOTIFY || o == UNDECIDED || (o == FC && hook != USE && hook != PLACE);
    }

    /** True when the general rule's FC row for {@code hook} of legacy id {@code id} is one of {@link #EXPECTED_FC}. */
    public static boolean isExpectedFc(int id, int hook) {
        Integer mask = EXPECTED_FC.get(id);
        return mask != null && (mask & (1 << hook)) != 0;
    }

    public static String hookName(int hook) {
        return HOOK_NAMES[hook];
    }

    public static String ownerName(int owner) {
        return switch (owner) {
            case NOTIFY -> "NOTIFY";
            case FC -> "FC";
            case VANILLA -> "VANILLA";
            default -> "UNDECIDED";
        };
    }

    /**
     * FC's activation of a click on a vanilla-id state, server side, main hand (1.5.2
     * ItemInWorldManager.activateBlockOrUseItem, vanilla/server ItemInWorldManager.java:376-407): Stage 1, FC
     * onBlockActivated, when the state's USE owner lets FC's block run; when it declines or does not run (and never for
     * a twin, whose items stay 1.20.1's), Stage 2, the held FC item through the single FC onItemUse point
     * {@link ProxyBlock#tryPlaceHeldItem}: 4.B runs the item whatever the block's activation does, so a VANILLA USE
     * owner (a removed block, a block-entity block, a ruled or pending row) drops Stage 1 only. A container FC opened
     * becomes its MC menu, every slot FC changed is written back, and a consumed click resends the held slot
     * (INV-RESYNC, as 1.5.2 after every place packet, NetServerHandler.java:551-569). Shared by the NOTIFY executor
     * (BlockBehaviorMixin.btw$use) and the FC-owned one ({@link #runFcOwnedUse}).
     *
     * @param stage1          true when FC's onBlockActivated runs (Stage 1); false for a VANILLA USE owner or a
     *                        block-entity-guarded FC block, where only Stage 2 runs
     * @param twin            true for a twin plant's FC block: Stage 1 only, never Stage 2
     * @param clientPredicted true when the client got PASS from this block's use and predicted the held BlockItem's
     *        placement, place sound included (the base body), so FC's place sound leaves this player out
     *        (WorldBridge.PREDICTING_ACTOR); false when the 1.20.1 class claims the click on the client
     *        (AnvilBlock.java:56-57)
     * @return SUCCESS when FC consumed the click or opened a menu; PASS when it declined or ran nothing; null when
     *         Stage 1 threw (logged once, FC's pending inventory edits discarded and nothing written back, INV-RESYNC:
     *         the 1.20.1 result and the item step follow)
     */
    public static InteractionResult activate(ServerLevel sl, BlockPos pos, Player player, InteractionHand hand,
                                             BlockHitResult hit, btw.modern.Block fcBlock, boolean stage1,
                                             boolean twin, boolean clientPredicted) {
        btw.modern.World world = WorldBridge.getOrCreate(sl);
        PlayerBridge fcPlayer = PlayerBridge.getOrCreate(player);
        fcPlayer.syncFromReal();
        int side = hit.getDirection().get3DDataValue();
        Vec3 hitLoc = hit.getLocation();
        float hitX = (float) (hitLoc.x - pos.getX());
        float hitY = (float) (hitLoc.y - pos.getY());
        float hitZ = (float) (hitLoc.z - pos.getZ());

        // Snapshot openContainer before FC call to detect GUI opens
        btw.modern.Container prevContainer = fcPlayer.openContainer;

        // Stage 1: FC block activation (e.g., opening chests, adding spit to campfire)
        boolean result = false;
        if (stage1) {
            try {
                result = fcBlock.onBlockActivated(world, pos.getX(), pos.getY(), pos.getZ(),
                        fcPlayer, side, hitX, hitY, hitZ);
            } catch (Throwable t) {
                // INV-RESYNC: a failed FC call lands nothing (invresync design section 2); the next sync dropped its
                // edits, the item step's sync would now write them
                fcPlayer.discardInventoryEdits();
                warnOnce("USE", fcBlock, pos, t);
                return null;
            }
        }

        if (!result && !twin) {
            // Stage 2: FC item use on block (e.g., flint & steel igniting campfire); tryPlaceHeldItem catches FC's
            // throws itself (creative: damage/stackSize restored like ItemInWorldManager.java:394-402)
            btw.modern.ItemStack fcHeld = fcPlayer.getCurrentEquippedItem();
            if (clientPredicted && player instanceof ServerPlayer sp
                    && player.getItemInHand(hand).getItem() instanceof BlockItem) {
                final btw.modern.ItemStack held = fcHeld;
                result = WorldBridge.withPredictingActor(sp, () -> ProxyBlock.tryPlaceHeldItem(held, player,
                        fcPlayer, world, pos, side, hitX, hitY, hitZ));
            } else {
                result = ProxyBlock.tryPlaceHeldItem(fcHeld, player, fcPlayer, world, pos,
                        side, hitX, hitY, hitZ);
            }
        }

        // Check if FC opened a container GUI and open the MC menu
        boolean containerOpened = ContainerBridge.checkAndOpenContainer(fcPlayer, prevContainer);

        // FC edits the held FC stack in place (e.g. --stackSize when planting/feeding, flint & steel
        // damage) and nothing applies that natively on this path - land it in the real MC inventory, as
        // ProxyBlock.use does. Selective write-back: only slots FC changed are written (InventoryBridge);
        // a creative Stage 2 was already undone above, so it writes nothing.
        fcPlayer.syncInventoryToReal();
        // INV-RESYNC: the client may have predicted the held BlockItem's placement; resend the held stack FC left
        if (result && fcPlayer.inventory instanceof InventoryBridge invBridge) {
            invBridge.resendHeldSlot();
        }
        return result || containerOpened ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    /**
     * The FC-owned USE executor, at the BlockStateBase.use forwarder on the server (BlockStateHookMixin; today the three
     * anvils, D2(c) and R15). FC replaces 1.20.1's use; the returned result ends the hook, null lets the 1.20.1 use run:
     * <ul>
     *   <li>the off hand: PASS, neither FC nor 1.20.1 (the FC player has only the main hand);</li>
     *   <li>an empty-handed sneak: null, the 1.20.1 use (the anvil's AnvilMenu, R15). FC-owned activation replaces a
     *       1.20.1 one, which stays reachable this way; sneaking with an item never reaches block use
     *       (ServerPlayerGameMode.java:340-343), and 4.B's FC item then runs at the item step, as 4.B did
     *       (vanilla/server ItemInWorldManager.java:380);</li>
     *   <li>otherwise FC's activation ({@link #activate}): SUCCESS when it consumed the click or opened a menu, else
     *       PASS, so the item step follows in 4.B's order;</li>
     *   <li>a Stage 1 throw: null, the 1.20.1 use (P0: an FC failure answers the 1.20.1 result, the anvil's
     *       AnvilMenu), with FC's pending inventory edits discarded.</li>
     * </ul>
     */
    public static InteractionResult runFcOwnedUse(BlockState state, ServerLevel sl, Player player, InteractionHand hand,
                                                  BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        // FCMOD-INTEGRATION(1.20.1): fc_beacon: an FC-backed block has no 1.20.1 activation left to fall back to; 4.B activated it on an empty-handed sneak (vanilla/server ItemInWorldManager.java:380)
        if (player.isSecondaryUseActive() && player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty()
                && !FcBackedBlockEntities.backs(state.getBlock())) {
            return null;
        }
        btw.modern.Block fc = ProxyRegistry.getFcBlock(state.getBlock());
        if (fc == null) return null;
        return activate(sl, hit.getBlockPos(), player, hand, hit, fc, true, false, false);
    }

    /**
     * The FC-owned PLACE executor, at the BlockStateBase.onPlace forwarder on the server (BlockStateHookMixin; today the
     * carved pumpkin and the jack o'lantern, D3(a)): FC onBlockAdded under today's guards (a new block, not a metadata
     * write, WorldBridge.isMetadataOnlyWrite), and the 1.20.1 body, the golem check (CarvedPumpkinBlock.java:44-48), is
     * always replaced. Every placement path reaches it: LevelChunk.setBlockState (LevelChunk.java:257) and Forge's
     * replay of captured placements (ForgeHooks.java:644), so dispensers too.
     *
     * @return true when the 1.20.1 body is to be cancelled (false only when the state has no FC block)
     */
    public static boolean runFcOwnedPlace(BlockState state, ServerLevel sl, BlockPos pos, BlockState oldState) {
        btw.modern.Block fc = ProxyRegistry.getFcBlock(state.getBlock());
        if (fc == null) return false;
        if (!state.is(oldState.getBlock()) && !WorldBridge.isMetadataOnlyWrite(pos)) {
            try {
                fc.onBlockAdded(WorldBridge.getOrCreate(sl), pos.getX(), pos.getY(), pos.getZ());
            } catch (Throwable t) {
                warnOnce("PLACE", fc, pos, t);
            }
        }
        return true;
    }

    // ================================================================
    // Class analysis (cached per class)
    // ================================================================

    /** FC's 1.5.2 hook methods per hook, on btw.modern.Block (SIGNAL: the three signal methods, paired with 1.20.1's). */
    private static final class FcSigs {
        static final Object[][][] BY_HOOK = {
                {{"onBlockActivated", new Class<?>[]{btw.modern.World.class, int.class, int.class, int.class,
                        btw.modern.EntityPlayer.class, int.class, float.class, float.class, float.class}}},
                {{"onBlockClicked", new Class<?>[]{btw.modern.World.class, int.class, int.class, int.class,
                        btw.modern.EntityPlayer.class}}},
                {{"onBlockAdded", new Class<?>[]{btw.modern.World.class, int.class, int.class, int.class}}},
                {{"breakBlock", new Class<?>[]{btw.modern.World.class, int.class, int.class, int.class, int.class,
                        int.class}}},
                {{"onNeighborBlockChange", new Class<?>[]{btw.modern.World.class, int.class, int.class, int.class,
                        int.class}}},
                {{"onEntityCollidedWithBlock", new Class<?>[]{btw.modern.World.class, int.class, int.class, int.class,
                        btw.modern.Entity.class}}},
                {{"isProvidingWeakPower", new Class<?>[]{btw.modern.IBlockAccess.class, int.class, int.class, int.class,
                        int.class}},
                        {"isProvidingStrongPower", new Class<?>[]{btw.modern.IBlockAccess.class, int.class, int.class,
                                int.class, int.class}},
                        {"canProvidePower", new Class<?>[0]}},
                {{"onEntityWalking", new Class<?>[]{btw.modern.World.class, int.class, int.class, int.class,
                        btw.modern.Entity.class}}},
                {{"onFallenUpon", new Class<?>[]{btw.modern.World.class, int.class, int.class, int.class,
                        btw.modern.Entity.class, float.class}}},
                {{"getExplosionResistance", new Class<?>[]{btw.modern.Entity.class}},
                        {"getExplosionResistance", new Class<?>[]{btw.modern.Entity.class, btw.modern.World.class,
                                int.class, int.class, int.class}}},
        };
    }

    /**
     * 1.20.1's hook methods per hook: {base class, runtime name, parameter types}, the SIGNAL entries aligned with
     * {@link FcSigs}. Runtime names: SRG in production, official in dev (srg_to_official_1.20.1.tsrg: m_6227_ use,
     * m_6256_ attack, m_6807_ onPlace, m_6810_ onRemove, m_6861_ neighborChanged, m_7892_ entityInside, m_6378_
     * getSignal, m_6376_ getDirectSignal, m_7899_ isSignalSource, m_141947_ stepOn, m_142072_ fallOn, m_7325_
     * getExplosionResistance). Built on first use, after the registries exist.
     */
    private static final class McSigs {
        static final Object[][][] BY_HOOK = {
                {sig(BlockBehaviour.class, "m_6227_", "use", BlockState.class, Level.class, BlockPos.class,
                        Player.class, InteractionHand.class, BlockHitResult.class)},
                {sig(BlockBehaviour.class, "m_6256_", "attack", BlockState.class, Level.class, BlockPos.class,
                        Player.class)},
                {sig(BlockBehaviour.class, "m_6807_", "onPlace", BlockState.class, Level.class, BlockPos.class,
                        BlockState.class, boolean.class)},
                {sig(BlockBehaviour.class, "m_6810_", "onRemove", BlockState.class, Level.class, BlockPos.class,
                        BlockState.class, boolean.class)},
                {sig(BlockBehaviour.class, "m_6861_", "neighborChanged", BlockState.class, Level.class, BlockPos.class,
                        Block.class, BlockPos.class, boolean.class)},
                {sig(BlockBehaviour.class, "m_7892_", "entityInside", BlockState.class, Level.class, BlockPos.class,
                        Entity.class)},
                {sig(BlockBehaviour.class, "m_6378_", "getSignal", BlockState.class, BlockGetter.class, BlockPos.class,
                        Direction.class),
                        sig(BlockBehaviour.class, "m_6376_", "getDirectSignal", BlockState.class, BlockGetter.class,
                                BlockPos.class, Direction.class),
                        sig(BlockBehaviour.class, "m_7899_", "isSignalSource", BlockState.class)},
                {sig(Block.class, "m_141947_", "stepOn", Level.class, BlockPos.class, BlockState.class, Entity.class)},
                {sig(Block.class, "m_142072_", "fallOn", Level.class, BlockState.class, BlockPos.class, Entity.class,
                        float.class)},
                {sig(Block.class, "m_7325_", "getExplosionResistance")},
        };

        /**
         * {base, runtime name or null, params}. The runtime name: Forge's mapping lookup first, then each name declared
         * on the base (official in dev, SRG in production). Null when neither resolves: no class then counts as
         * overriding the hook, so the general rule keeps NOTIFY (today's behaviour) rather than handing a hook to FC.
         */
        private static Object[] sig(Class<?> base, String srg, String official, Class<?>... params) {
            Throwable failure;
            try {
                return new Object[]{base, ObfuscationReflectionHelper.findMethod(base, srg, params).getName(), params};
            } catch (Throwable t) {
                failure = t;
            }
            for (String name : new String[]{official, srg}) {
                try {
                    base.getDeclaredMethod(name, params);
                    LOGGER.warn("Resolved {}.{} by its declared name {} (mapping lookup failed: {})",
                            base.getSimpleName(), srg, name, failure.toString());
                    return new Object[]{base, name, params};
                } catch (Throwable ignored) {
                    // try the next name
                }
            }
            LOGGER.error("Could not resolve {}.{}; no block counts as overriding it", base.getSimpleName(), srg, failure);
            return new Object[]{base, null, params};
        }
    }

    /** Per 1.20.1 class: bit h*3+i set when sub-method i of hook h is overridden below its base. */
    private static final Map<Class<?>, Long> OVERRIDES = new ConcurrentHashMap<>();
    /** Per FC class: bit h*3+i set when sub-method i of hook h is declared in FC code (below btw.modern). */
    private static final Map<Class<?>, Long> FC_DECLARES = new ConcurrentHashMap<>();

    /** Rule 7: some sub-method of the hook is overridden by the 1.20.1 class and declared by the FC class. */
    private static boolean generalRuleGivesFc(Class<?> mcClass, Class<?> fcClass, int hook) {
        long both = overrideMask(mcClass) & declareMask(fcClass);
        return ((both >>> (hook * 3)) & 7L) != 0;
    }

    /** True when the 1.20.1 class overrides the hook (any of its sub-methods) below BlockBehaviour / Block. */
    public static boolean overrides(Class<?> mcClass, int hook) {
        return ((overrideMask(mcClass) >>> (hook * 3)) & 7L) != 0;
    }

    /** True when the runtime FC class declares the hook (any of its 1.5.2 methods) in FC code, below btw.modern. */
    public static boolean fcDeclares(Class<?> fcClass, int hook) {
        return ((declareMask(fcClass) >>> (hook * 3)) & 7L) != 0;
    }

    /**
     * True when the runtime FC class declares the positional getExplosionResistance(Entity, World, int, int, int) in
     * FC code (RESIST sub-method 1 of {@link FcSigs}): FCBlockStone and FCBlockOre read the strata there.
     */
    public static boolean fcDeclaresPositionalResistance(Class<?> fcClass) {
        return ((declareMask(fcClass) >>> (RESIST * 3 + 1)) & 1L) != 0;
    }

    private static long overrideMask(Class<?> start) {
        Long cached = OVERRIDES.get(start);
        if (cached != null) return cached;
        long mask = 0;
        for (int h = 0; h < HOOKS; h++) {
            Object[][] subs = McSigs.BY_HOOK[h];
            for (int i = 0; i < subs.length; i++) {
                Class<?> base = (Class<?>) subs[i][0];
                String name = (String) subs[i][1];
                Class<?>[] params = (Class<?>[]) subs[i][2];
                if (name == null) continue;
                for (Class<?> c = start; c != null && c != base && c != Object.class; c = c.getSuperclass()) {
                    try {
                        c.getDeclaredMethod(name, params);
                        mask |= 1L << (h * 3 + i);
                        break;
                    } catch (NoSuchMethodException ignored) {
                        // not declared here; keep walking up
                    } catch (Throwable t) {
                        // Unreadable class: not overriding, the conservative choice (NOTIFY is today's behaviour)
                        LOGGER.warn("Could not inspect {} on {}; treating it as not overridden", name, c.getName(), t);
                        break;
                    }
                }
            }
        }
        OVERRIDES.put(start, mask);
        return mask;
    }

    private static long declareMask(Class<?> fcClass) {
        Long cached = FC_DECLARES.get(fcClass);
        if (cached != null) return cached;
        long mask = 0;
        for (int h = 0; h < HOOKS; h++) {
            Object[][] subs = FcSigs.BY_HOOK[h];
            for (int i = 0; i < subs.length; i++) {
                try {
                    Class<?> declaring = fcClass.getMethod((String) subs[i][0], (Class<?>[]) subs[i][1])
                            .getDeclaringClass();
                    if (!declaring.getName().startsWith("btw.modern.")) mask |= 1L << (h * 3 + i);
                } catch (Throwable t) {
                    // not an FC hook here (cannot happen for a btw.modern.Block subclass)
                }
            }
        }
        FC_DECLARES.put(fcClass, mask);
        return mask;
    }

    // ================================================================
    // Logging
    // ================================================================

    /** Hook failure sites already reported by {@link #warnOnce}. */
    private static final Set<String> LOGGED_FAILURES = ConcurrentHashMap.newKeySet();

    /**
     * One WARN with its stack trace per distinct throw site (hook, FC class, exception class and top frame), as
     * TickPolicy.warnOnce (TickPolicy.java:557-563): a block failing on every call neither floods the log nor hides.
     * The caller answers the 1.20.1 result.
     */
    public static void warnOnce(String hook, Object fc, BlockPos pos, Throwable t) {
        StackTraceElement[] frames = t.getStackTrace();
        String cls = fc == null ? "?" : fc.getClass().getSimpleName();
        String site = hook + '|' + cls + '|' + t.getClass().getName() + '@' + (frames.length > 0 ? frames[0] : "?");
        if (LOGGED_FAILURES.add(site)) {
            LOGGER.warn("[{}] {} at {} threw {}; the 1.20.1 result stands (logged once per throw site)", hook, cls,
                    pos, t.toString(), t);
        }
    }

    /** initCache must never throw from a registry bake: the failing state keeps every hook UNDECIDED. */
    public static void warnDecideFailure(BlockState s, Throwable t) {
        String site = "DECIDE|" + s.getBlock().getClass().getName() + '|' + t.getClass().getName();
        if (LOGGED_FAILURES.add(site)) {
            LOGGER.warn("Hook policy decision failed for {}; its hooks keep today's lookup (UNDECIDED)", s, t);
        }
    }
}
