package btw.forge;

import btw.forge.removed.RemovedContent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FCMOD-INTEGRATION(1.20.1): fc_blast_rules
 * <p><b>Feature:</b> 1.20.1 explosions (Explosion.explode / finalizeExplosion): TNT, creepers, ghasts, the wither, beds
 * and respawn anchors, other mods' blasts, and BTW's dynamite, blasting oil and vases through Level.explode.</p>
 * <p><b>Conflict with FC:</b> finalizeExplosion dropped every block's 1.20.1 loot and ran 1.20.1's reaction, so FC's
 * per-block blast rules (canDropFromExplosion, 1 / size drops, the 15 onBlockDestroyedByExplosion overrides, the
 * secondary explosions) never ran; the rays read FC's position-less resistance, so strata stone and ores resisted
 * like surface stone (triage CF2-DR4).</p>
 * <p><b>Integration pattern:</b> D (FC-extend). Every affected cell whose block FC has a blast rule for (rules 0-8,
 * {@link #decide}) runs 1.5.2 Explosion.doExplosionB's per-block body at the HEAD of finalizeExplosion and is left as
 * air for 1.20.1's own loop, which handles every other cell as before; FC's drops are merged as 1.20.1 merges its own
 * (16 per stack); FC's queued secondary explosions run at the TAIL. A block whose FC class reads its position for
 * blast resistance (HookPolicy RESIST = FC) answers 1.20.1's rays with that value
 * ({@link #positionalResistance}).</p>
 * <p><b>Rebalance:</b> FC's drop chance is 1 / radius when the blast decays and 1 otherwise (the decay gamerules,
 * decision D1).</p>
 * <p><b>Injection strategy:</b> ExplosionMixin (finalizeExplosion HEAD and TAIL) and ExplosionDamageCalculatorMixin
 * (getBlockExplosionResistance HEAD, cancellable). Server thread only; KEEP blasts never reach FC.</p>
 * <p><b>Alternatives rejected:</b> ExplosionEvent.Detonate (runs before the entity damage and in listener order, so a
 * protection mod's own list edit could come after BTW destroyed the cells); redirects of canDropFromExplosion /
 * getDrops / onBlockExploded (exclusive, two coordinated redirects per cell); a loot modifier on EXPLOSION_RADIUS
 * (misses every non-decaying blast, cannot run FC's reactions); routing every FC-identity block (Modern-Common
 * placeholders and FC classes with vanilla drops would lose behaviour 4.B shares with 1.20.1, and TNT would stop
 * chaining).</p>
 * <p><b>Known limitations:</b> TNT (46) keeps 1.20.1's blast until BTW's primed TNT is ported; blocks with a 1.20.1
 * block entity, beds, doors and collapsed variants keep 1.20.1's blast; loot modifiers do not see FC-owned blasts.</p>
 */
// FCMOD-INTEGRATION(1.20.1): fc_blast_rules: every 1.20.1 explosion runs 1.5.2 Explosion.doExplosionB's per-block body for the blocks FC has a blast rule for, and reads FC's positional blast resistance
public final class BlastRules {

    private static final Logger LOGGER = LogManager.getLogger("BTW-Blast");

    /** Why a state's blast is FC's or 1.20.1's (design section 2.1; the self-audit tallies them). */
    public enum Rule { FC, NO_IDENTITY, REMOVED, PAIR, BLOCK_ENTITY, COLLAPSED, PENDING, NO_FC_RULE }

    /** Legacy ids whose FC blast body needs an unported prerequisite: 1.20.1 keeps their blast until it lands. */
    static final Map<Integer, String> PENDING = Map.of(46,
            "FCBlockPowderKeg primes Modern-Common EntityTNTPrimed, a stub (fuse 0, no tick); TntBlock.wasExploded primes a 1.20.1 PrimedTnt");

    /** Self-audit expectations: ids FC must own, and ids that must stay 1.20.1's for the given rule. */
    static final Set<Integer> EXPECTED_FC = Set.of(1, 2, 3, 4, 13, 18, 78, 110);
    static final Map<Integer, Rule> EXPECTED_OTHER = Map.of(26, Rule.PAIR, 64, Rule.PAIR, 71, Rule.PAIR,
            54, Rule.BLOCK_ENTITY, 46, Rule.PENDING);

    /**
     * Legacy ids whose FC class reads its position for blast resistance (Server FCBlockStone.java:65-85,
     * FCBlockOre.java:65-85 and FCBlockOreStaged's subclasses: the strata). 74, the glowing redstone ore, has no 1.20.1
     * block of its own (REDSTONE_ORE[lit] reads 73).
     */
    static final Set<Integer> EXPECTED_POSITIONAL = Set.of(1, 14, 15, 16, 21, 56, 73, 129);

    /** FC's 1.5.2 blast and drop methods on btw.modern.Block (rule 7). */
    private static final Object[][] BLAST_METHODS = {
        {"canDropFromExplosion", new Class<?>[]{btw.modern.Explosion.class}},
        {"onBlockDestroyedByExplosion", new Class<?>[]{btw.modern.World.class, int.class, int.class, int.class,
                btw.modern.Explosion.class}},
        {"dropBlockAsItemWithChance", new Class<?>[]{btw.modern.World.class, int.class, int.class, int.class,
                int.class, float.class, int.class}},
        {"idDropped", new Class<?>[]{int.class, Random.class, int.class}},
        {"quantityDropped", new Class<?>[]{Random.class}},
        {"quantityDroppedWithBonus", new Class<?>[]{int.class, Random.class}},
        {"damageDropped", new Class<?>[]{int.class}},
    };

    /** 1.20.1 Explosion.addBlockDrops merges a blast's drops into stacks of at most 16 (Explosion.java:292-308). */
    private static final int MERGED_STACK = 16;

    private static final Map<BlockState, Boolean> OWNS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Boolean> DECLARES = new ConcurrentHashMap<>();
    /** BTW blocks whose FC class declares the positional resistance (HookPolicy leaves ProxyBlocks unmanaged). */
    private static final Map<Block, Boolean> PROXY_POSITIONAL = new ConcurrentHashMap<>();
    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();

    private BlastRules() {}

    // ================================================================
    // finalizeExplosion: HEAD and TAIL (ExplosionMixin)
    // ================================================================

    /**
     * ExplosionMixin HEAD: 1.5.2 doExplosionB's per-block body for every FC-owned cell of the explosion, in toBlow
     * order; the cells are air afterwards and 1.20.1's loop skips them (Explosion.java:248). Returns the FC Explosion
     * handed to FC (TAIL runs its secondaries), or null when no cell was FC's, the blast keeps blocks, or this is not
     * the server thread. Never throws.
     */
    public static btw.modern.Explosion beforeFinalize(Explosion explosion, ServerLevel level, float radius,
                                                      Explosion.BlockInteraction interaction, boolean fire) {
        if (interaction == Explosion.BlockInteraction.KEEP || !BTWLifecycle.isFcReady()
                || !level.getServer().isSameThread()) {
            return null;
        }
        btw.modern.Explosion fcExplosion = null;
        WorldBridge world = null;
        List<ItemEntity> drops = null;
        List<ItemEntity> outer = null;
        try {
            for (BlockPos pos : new ArrayList<>(explosion.getToBlow())) {
                BlockState state = level.getBlockState(pos);
                if (state.isAir() || !fcOwnsBlast(state)) continue;
                if (fcExplosion == null) {
                    world = WorldBridge.getOrCreate(level);
                    drops = new ArrayList<>();
                    outer = world.swapBlastDropCapture(drops);
                    Vec3 c = explosion.getPosition();
                    fcExplosion = new btw.modern.Explosion(world, fcExploder(world, explosion.getIndirectSourceEntity()),
                            c.x, c.y, c.z, dropRadius(radius, interaction));
                    fcExplosion.isFlaming = fire;
                    fcExplosion.isSmoking = true;
                }
                destroy(world, level, fcExplosion, pos);
            }
        } catch (Throwable t) {
            failOnce("beforeFinalize", t);
        } finally {
            if (world != null) {
                world.swapBlastDropCapture(outer);
                addMerged(level, drops);
            }
        }
        return fcExplosion;
    }

    /** ExplosionMixin TAIL: 1.5.2 PerformSecondaryExplosions (vanilla/server Explosion.java:284) as NoFX blasts. */
    public static void afterFinalize(ServerLevel level, btw.modern.Explosion fcExplosion) {
        if (!level.getServer().isSameThread()) return;
        try {
            List<?> queued = fcExplosion.m_SecondaryExplosionList;
            if (queued == null || queued.isEmpty()) return;
            fcExplosion.m_SecondaryExplosionList = null;
            WorldBridge world = WorldBridge.getOrCreate(level);
            for (Object o : new ArrayList<>(queued)) {
                if (o instanceof btw.modern.Explosion s) {
                    world.NewExplosionNoFX(null, s.explosionX, s.explosionY, s.explosionZ, s.explosionSize,
                            s.isFlaming, s.isSmoking);
                }
            }
        } catch (Throwable t) {
            failOnce("afterFinalize", t);
        }
    }

    /** D1: the size FC divides its drop chances by (1.5.2: 1 / explosionSize for every blast). Options B/C. */
    static float dropRadius(float radius, Explosion.BlockInteraction interaction) {
        return interaction == Explosion.BlockInteraction.DESTROY_WITH_DECAY ? radius : 1.0F;
    }

    /**
     * 1.5.2 Explosion.doExplosionB for one cell (vanilla/server Explosion.java:241-247), in 1.5.2's order: drop,
     * remove, react. Each step is caught on its own and the cell is always removed, so an FC drop never pairs with
     * 1.20.1's loot for the same cell.
     */
    private static void destroy(WorldBridge world, ServerLevel level, btw.modern.Explosion fcExplosion, BlockPos pos) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        int id = world.getBlockId(x, y, z);
        btw.modern.Block fc = id > 0 && id < btw.modern.Block.blocksList.length ? btw.modern.Block.blocksList[id] : null;
        if (fc == null) return; // never for an FC-owned state (getBlockId never answers an empty slot): 1.20.1's loop
        try {
            if (fc.canDropFromExplosion(fcExplosion)) {
                fc.dropBlockAsItemWithChance(world, x, y, z, world.getBlockMetadata(x, y, z),
                        1.0F / fcExplosion.explosionSize, 0);
            }
        } catch (Throwable t) {
            warnOnce("drop", fc, pos, t);
        }
        boolean removed = false;
        try {
            world.setBlock(x, y, z, 0, 0, 3);
            removed = true;
        } catch (Throwable t) {
            warnOnce("remove", fc, pos, t);
        }
        if (!removed && !level.getBlockState(pos).isAir()) {
            try {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            } catch (Throwable t) {
                warnOnce("remove", fc, pos, t);
            }
        }
        try {
            fc.onBlockDestroyedByExplosion(world, x, y, z, fcExplosion);
        } catch (Throwable t) {
            warnOnce("react", fc, pos, t);
        }
    }

    /**
     * 1.20.1 Explosion.addBlockDrops (Explosion.java:292-308) on the ItemEntities FC dropped during the HEAD pass
     * (WorldBridge held them back): each joins the first earlier drop it can merge with, up to 16 per stack; the drops
     * left keep FC's position, motion and pickup delay and are added to the level.
     */
    static void addMerged(ServerLevel level, List<ItemEntity> drops) {
        if (drops == null || drops.isEmpty()) return;
        List<ItemEntity> kept = new ArrayList<>(drops.size());
        for (ItemEntity drop : drops) {
            ItemStack stack = drop.getItem().copy();
            for (ItemEntity k : kept) {
                ItemStack ks = k.getItem();
                if (ItemEntity.areMergable(ks, stack)) {
                    k.setItem(ItemEntity.merge(ks, stack, MERGED_STACK));
                    if (stack.isEmpty()) break;
                }
            }
            if (!stack.isEmpty()) {
                drop.setItem(stack);
                kept.add(drop);
            }
        }
        for (ItemEntity k : kept) {
            try {
                level.addFreshEntity(k);
            } catch (Throwable t) {
                failOnce("addDrop", t);
            }
        }
    }

    /**
     * FC's view of the explosion's living source (1.5.2 Explosion.func_94613_c reads the exploder): the proxy's FC
     * entity or the player's PlayerBridge, through WorldBridge's own lookup; null where FC has no view of it.
     */
    private static btw.modern.Entity fcExploder(WorldBridge world, LivingEntity source) {
        return source == null ? null : world.getEntityByID(source.getId());
    }

    // ================================================================
    // Positional blast resistance (ExplosionDamageCalculatorMixin)
    // ================================================================

    /**
     * ExplosionDamageCalculatorMixin HEAD, the resistance of each step of 1.20.1's explosion rays: FC's positional
     * getExplosionResistance(entity, world, i, j, k) where FC's class reads the position (4.B: doExplosionA asks it,
     * vanilla/server Explosion.java:107-108, Entity.java:2244-2252), maxed with the fluid's as 1.20.1 does. Null (the
     * 1.20.1 path, BlockMixin's position-less FC lookup) for every other state, off the server thread and on an FC
     * throw. Hot path: every other state costs one cached int read.
     */
    public static Float positionalResistance(BlockGetter getter, BlockPos pos, BlockState state, FluidState fluid,
                                             Explosion explosion) {
        if (!readsPosition(state)) return null;
        if (!(getter instanceof ServerLevel level) || !level.getServer().isSameThread()) return null;
        btw.modern.Block fc = ProxyRegistry.getFcBlock(state.getBlock());
        if (fc == null) return null;
        try {
            float r = fc.getExplosionResistance(null, WorldBridge.getOrCreate(level), pos.getX(), pos.getY(), pos.getZ());
            return fluid.isEmpty() ? r : Math.max(r, fluid.getExplosionResistance(getter, pos, explosion));
        } catch (Throwable t) {
            warnOnce("resist", fc, pos, t);
            return null;
        }
    }

    /** HookPolicy's RESIST owner is FC (cached on the state), or a BTW block whose FC class declares the overload. */
    static boolean readsPosition(BlockState state) {
        if (HookPolicy.owner(HookPolicy.cachedOwners(state), HookPolicy.RESIST) == HookPolicy.FC) return true;
        Block block = state.getBlock();
        if (!(block instanceof ProxyBlock proxy)) return false;
        Boolean cached = PROXY_POSITIONAL.get(block);
        if (cached == null) {
            btw.modern.Block fc = proxy.getFcBlock();
            cached = fc != null && HookPolicy.fcDeclaresPositionalResistance(fc.getClass());
            PROXY_POSITIONAL.put(block, cached);
        }
        return cached;
    }

    // ================================================================
    // Ownership rule
    // ================================================================

    /** True when FC owns the blast of this state (design section 2.1). Pure; cached per state once FC is ready. */
    public static boolean fcOwnsBlast(BlockState state) {
        Boolean cached = OWNS.get(state);
        if (cached != null) return cached;
        if (!BTWLifecycle.isFcReady()) return false;
        boolean owns;
        try {
            owns = decide(state) == Rule.FC;
        } catch (Throwable t) {
            failOnce("decide", t);
            owns = false;
        }
        OWNS.put(state, owns);
        return owns;
    }

    public static Rule decide(BlockState state) {
        Block block = state.getBlock();
        int id = ProxyRegistry.getBlockId(block);
        if (id <= 0) return Rule.NO_IDENTITY;
        // identity blocks read their own id, a double slab 43/125; level and pos are only read for id 0
        int worldId = ProxyRegistry.getWorldBlockId(state, EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        btw.modern.Block fc = worldId > 0 && worldId < btw.modern.Block.blocksList.length
                ? btw.modern.Block.blocksList[worldId] : null;
        if (fc == null) return Rule.NO_IDENTITY;
        if (block instanceof ProxyBlock) return Rule.FC;
        RemovedContent.Entry removed = RemovedContent.forBlock(block);
        if (removed != null && removed.tier == RemovedContent.Tier.T1_STUBBED) return Rule.REMOVED;
        if (VanillaPairDrops.isRuled(block)) return Rule.PAIR;
        if (state.hasBlockEntity() || HookPolicy.isTeGuarded(fc)) return Rule.BLOCK_ENTITY;
        if (!isCanonical(block, id)) return Rule.COLLAPSED;
        if (PENDING.containsKey(id)) return Rule.PENDING;
        return declaresBlastRule(fc.getClass()) ? Rule.FC : Rule.NO_FC_RULE;
    }

    /** The 1.20.1 block FC recreates for its (id, subtype) is this block (ProxyRegistry: first listed = canonical). */
    static boolean isCanonical(Block block, int id) {
        int variantMeta = ProxyRegistry.getLegacyVariantMeta(block);       // package-private, same package
        if (variantMeta >= 0) return ProxyRegistry.getModernBlockVariant(id, variantMeta) == block;
        Block canonical = ProxyRegistry.getModernBlock(id);
        return canonical == null || canonical == block;
    }

    /** Rule 7: the class declares a blast or drop method in FC code (outside btw.modern), as HookPolicy.declareMask. */
    static boolean declaresBlastRule(Class<?> fcClass) {
        return DECLARES.computeIfAbsent(fcClass, c -> {
            for (Object[] m : BLAST_METHODS) {
                try {
                    if (!c.getMethod((String) m[0], (Class<?>[]) m[1]).getDeclaringClass().getName()
                            .startsWith("btw.modern.")) {
                        return true;
                    }
                } catch (NoSuchMethodException e) {
                    failOnce("signature " + m[0], e);
                }
            }
            return false;
        });
    }

    // ================================================================
    // Self-audit
    // ================================================================

    /** [SELF-AUDIT/blast], from BridgeSelfAudit.runServerStart (after Blocks.rebuildCache: HookPolicy's cache is set). */
    public static void audit() {
        if (!BTWLifecycle.isFcReady()) {
            LOGGER.warn("[SELF-AUDIT/blast] FC is not ready; skipped");
            return;
        }
        Map<Rule, Integer> tally = new EnumMap<>(Rule.class);
        int total = 0, proxies = 0, warnings = 0, positionalBlocks = 0, positionalProxies = 0;
        Set<Integer> positionalIds = new TreeSet<>();
        for (Block b : ForgeRegistries.BLOCKS) {
            if (ProxyRegistry.getBlockId(b) <= 0) continue;
            total++;
            BlockState s = b.defaultBlockState();
            Rule r = decide(s);
            tally.merge(r, 1, Integer::sum);
            if (r == Rule.FC && b instanceof ProxyBlock) proxies++;
            if (readsPosition(s)) {
                if (b instanceof ProxyBlock) {
                    positionalProxies++;
                } else {
                    positionalBlocks++;
                    positionalIds.add(ProxyRegistry.getBlockId(b));
                }
            }
        }
        for (Object[] m : BLAST_METHODS) {
            try {
                btw.modern.Block.class.getMethod((String) m[0], (Class<?>[]) m[1]);
            } catch (NoSuchMethodException e) {
                warnings++;
                LOGGER.warn("[SELF-AUDIT/blast] btw.modern.Block has no {}: rule 7 cannot see it", m[0]);
            }
        }
        for (int id : EXPECTED_FC) {
            Block b = ProxyRegistry.getModernBlock(id);
            Rule r = b == null ? null : decide(b.defaultBlockState());
            if (r != Rule.FC) {
                warnings++;
                LOGGER.warn("[SELF-AUDIT/blast] legacy {} ({}) should follow FC's blast rules; rule {}", id, b, r);
            }
        }
        for (Map.Entry<Integer, Rule> e : EXPECTED_OTHER.entrySet()) {
            Block b = ProxyRegistry.getModernBlock(e.getKey());
            Rule r = b == null ? null : decide(b.defaultBlockState());
            if (r != e.getValue()) {
                warnings++;
                LOGGER.warn("[SELF-AUDIT/blast] legacy {} ({}) should keep 1.20.1's blast by {}; rule {}", e.getKey(),
                        b, e.getValue(), r);
            }
        }
        if (!positionalIds.equals(EXPECTED_POSITIONAL)) {
            warnings++;
            LOGGER.warn("[SELF-AUDIT/blast] positional blast resistance on legacy ids {}, expected {}: an FC class gained"
                    + " or lost getExplosionResistance(Entity, World, int, int, int), its HookPolicy signature drifted, or"
                    + " BlockStateHookMixin's cache is missing", positionalIds, EXPECTED_POSITIONAL);
        }
        int fc = tally.getOrDefault(Rule.FC, 0);
        tally.remove(Rule.FC);
        LOGGER.info("[SELF-AUDIT/blast] {} FC-identity blocks: {} follow FC's blast rules ({} BTW blocks), the rest keep"
                + " 1.20.1's {}; pending {}; positional resistance on {} blocks of ids {} and {} BTW blocks; {} warnings"
                + " (0 expected)", total, fc, proxies, tally, PENDING.keySet(), positionalBlocks, positionalIds,
                positionalProxies, warnings);
    }

    // ================================================================
    // Logging
    // ================================================================

    private static void warnOnce(String step, btw.modern.Block fc, BlockPos pos, Throwable t) {
        StackTraceElement[] frames = t.getStackTrace();
        String site = step + '|' + fc.getClass().getSimpleName() + '|' + t.getClass().getName() + '@'
                + (frames.length > 0 ? frames[0] : "?");
        if (LOGGED.add(site)) {
            LOGGER.warn("[BLAST] {} of {} at {} threw {} (logged once per throw site)", step,
                    fc.getClass().getSimpleName(), pos, t.toString(), t);
        }
    }

    private static void failOnce(String what, Throwable t) {
        if (LOGGED.add(what)) LOGGER.warn("[BLAST] {} failed: {} (logged once)", what, t.toString(), t);
    }
}
