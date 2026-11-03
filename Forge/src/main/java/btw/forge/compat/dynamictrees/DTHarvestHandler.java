package btw.forge.compat.dynamictrees;

import btw.forge.FCLogRules;
import btw.forge.FCMiningSpeed;
import com.ferreusveritas.dynamictrees.api.TreeHelper;
import com.ferreusveritas.dynamictrees.block.branch.BranchBlock;
import com.ferreusveritas.dynamictrees.entity.FallingTreeEntity;
import com.ferreusveritas.dynamictrees.init.DTConfigs;
import com.ferreusveritas.dynamictrees.tree.family.Family;
import com.ferreusveritas.dynamictrees.util.BranchDestructionData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * FC's log rules on living Dynamic Trees (INTEGRATIONS #027): FCBlockLog's chew stages, the axe fell with FC's per-log
 * hunger and wear, the BTW stump, the work stump, FC's dig rate for a chew, and FC bark for axe stripping. Registered
 * once on the Forge bus by {@link DynamicTreesCompat#init}; server side unless stated.
 *
 * <p>Timing: DT never breaks a branch inside {@code destroyBlock}. {@code BranchBlock.removedByEntity} queues a
 * FutureBreak (BranchBlock.java:618-624) that runs on the next level tick (CommonEventHandler.java:18-20), and
 * {@code futureBreak} reads Fortune from the main hand of THAT tick, drops the falling tree (whose join is
 * {@link #onJoin}) and only then applies {@code damageAxe} to that main-hand stack (BranchBlock.java:597-613). A fell
 * or chew-through is therefore recorded at the break ({@link PendingFell}, keyed by dimension and trunk core),
 * settled when its falling tree joins, and its wear is written as an absolute target at ServerTickEvent END, after
 * DT's own wear: FC's count lands on the stack that made the break, and a stack swapped into the main hand in between
 * gets DT's wear compensated back to its earlier damage.</p>
 */
// FCMOD-INTEGRATION(1.20.1): dynamic_trees: Dynamic Trees branches follow FCBlockLog's harvest rules: chew stages, axe fell, stump, work stump, FC wear and hunger
public final class DTHarvestHandler {

    /** A pending entry older than this many server ticks is dropped (its FutureBreak or strip never came). */
    private static final int EXPIRY_TICKS = 5;

    /** A trunk core in one dimension. */
    record Key(ResourceKey<Level> dim, BlockPos pos) {}

    /**
     * A break DT will fell on the next level tick: an axe harvest ({@code harvest}) or the last chew that cuts
     * through. {@code stack} is the main-hand stack that made the break (a reference: vanilla's mineBlock acted on it).
     * {@code woodMeta} is the family's FC wood type ({@link DTTrees#fcWoodMeta}), for FC's harvest hunger.
     */
    record PendingFell(ServerPlayer p, ItemStack stack, int damageBefore, boolean harvest, int woodMeta, long tick) {}

    /**
     * An axe strip (right click) DT performs inside its {@code use()}, after the event; {@code barkSpecies} is the bark
     * it pays ({@link DTTrees#barkSpecies}).
     */
    record PendingStrip(ServerPlayer p, ItemStack stack, int damageBefore, Direction face, int barkSpecies,
                        long tick) {}

    /** An absolute damage {@code target} for {@code stack}, applied at ServerTickEvent END. */
    record Wear(ServerPlayer p, ItemStack stack, int target) {}

    private final Map<Key, PendingFell> fells = new HashMap<>();
    private final Map<Key, PendingStrip> strips = new HashMap<>();
    private final List<Wear> wears = new ArrayList<>();
    private boolean payloadWarned;

    // ------------------------------------------------------------------ (a) breaking a branch

    /**
     * A survival player breaks a DT block. LOWEST and not receiving cancelled events, so protection mods and adventure
     * pre-cancels act first. Explosions and non-players keep DT's own path.
     * <ol>
     *   <li>Rooty soil under a log is not dug (RootyBlock.playerWillDestroy would delete the tree with no drops).</li>
     *   <li>An unchewed dead stump and an FC work-stump tool: FC's work stump of the family's wood by the shared log
     *       rule ({@link DTTrees#workStumpSpecies}: FC's four 0-3, acacia, dark oak, mangrove and cherry 5-8, a
     *       modded family's bark tag or oak; none for the fungi, which are chewed), with FC's convert wear for it (10
     *       on the iron chisel). A chewed one is chewed on, as FC's.</li>
     *   <li>Not a stump, and an FC-harvesting axe: the break proceeds; DT fells on the next tick
     *       ({@link #onJoin}).</li>
     *   <li>Otherwise FC chews: hit 1 strips and gives the family's bark ({@link DTTrees#barkSpecies}); down to radius
     *       4 each hit takes one radius and pops the stage item; the last chew pops sawdust and lets DT fell what
     *       stands above, with no logs.</li>
     * </ol>
     */
    // FCMOD-INTEGRATION(1.20.1): dynamic_trees: a DT branch hit without an FC-harvesting axe is chewed as FCBlockLog.ConvertBlock, never felled with its logs
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBreak(BlockEvent.BreakEvent e) {
        if (!DynamicTreesCompat.active) return;
        if (!(e.getLevel() instanceof ServerLevel level) || !(e.getPlayer() instanceof ServerPlayer p)) return;
        if (p.isCreative() || p.isSpectator()) return;
        try {
            BlockPos pos = e.getPos();
            BlockState state = e.getState();
            // 1. A stump or a living tree must go before its rooty soil.
            if (TreeHelper.isRooty(state) && DTTrees.isLog(level.getBlockState(pos.above()))) {
                e.setCanceled(true);
                return;
            }
            // 2. The trunk core (a trunk shell's muse) and its facts.
            BlockPos core = DTTrees.resolveCore(level, pos);
            BlockState cs = level.getBlockState(core);
            if (!DTTrees.isLog(cs)) return;
            BranchBlock b = (BranchBlock) cs.getBlock();
            int r = b.getRadius(cs);
            boolean stump = DTTrees.isStump(level, core);
            // FCMOD-INTEGRATION(1.20.1): dynamic_trees: the family's FC wood type (FC's hunger) and the bark and work stump of the shared log rule (FCLogRules), so a DT dark oak is wood type 6 like the vanilla log
            int meta = DTTrees.fcWoodMeta(b);
            int barkSpecies = DTTrees.barkSpecies(b);
            ItemStack held = p.getMainHandItem();
            Direction face = hitFace(p, pos);
            long now = level.getServer().getTickCount();
            Key key = key(level, core);

            // 3. Work stump, from an unchewed dead stump only: FCBlockLogDamaged (a stripped branch) has no work-stump
            //    convert (Server FCBlockLogDamaged.java:164-207). setBlockStateIgnored avoids BranchBlock.onRemove's
            //    sloppy break. The wear hands FC the log's id 17 at a cell that now holds the work stump, as
            //    ItemInWorldManager does (Modern-Common :85-108), so FCItemChiselIron charges its 10 (Server :44-54);
            //    the vanilla oak log reads as 17. A family with no work stump (the fungi) is chewed instead.
            int stumpWood = DTTrees.workStumpSpecies(b);
            if (stump && stumpWood >= 0 && !b.isStrippedBranch() && DTTrees.isDeadStump(level, core)
                    && FCLogRules.isWorkStumpTool(p, held, core)) {
                BlockState ws = FCLogRules.workStumpState(stumpWood);
                if (ws != null) {
                    e.setCanceled(true);
                    b.setBlockStateIgnored(level, core, ws, 3);
                    wearOne(level, p, held, Blocks.OAK_LOG.defaultBlockState(), core);
                    DynamicTreesCompat.trace("work stump at {} (wood {})", core, stumpWood);
                    return;
                }
            }
            // 4. Fell: the break proceeds and DT fells on the next level tick (FutureBreak).
            if (!stump && FCLogRules.canHarvestLog(p, core)) {
                fells.put(key, new PendingFell(p, held, damage(held), true, meta, now));
                DynamicTreesCompat.trace("fell queued at {} (radius {})", core, r);
                return;
            }
            // 5. Strip chew: FC's first chew hit always gives bark (no DT wear: the LevelAccessor overload).
            Family fam = b.getFamily();
            int minStrip = fam.getMinRadiusForStripping();
            if (!b.isStrippedBranch() && fam.hasStrippedBranch() && minStrip != 0 && r >= minStrip) {
                e.setCanceled(true);
                strips.remove(key);
                b.stripBranch(cs, level, core);
                popToward(level, core, face, FCLogRules.bark(barkSpecies));
                wearOne(level, p, held, cs, core);
                DynamicTreesCompat.trace("chew at {}: strip, bark of wood {}", core, barkSpecies);
                return;
            }
            // 6. Last chew (FCBlockLogDamaged's last ConvertBlock, Server :196-206): sawdust; DT fells, no logs.
            if (r <= 4) {
                pop(level, core, FCLogRules.sawdust(stump ? 2 : 1));
                fells.put(key, new PendingFell(p, held, damage(held), false, meta, now));
                DynamicTreesCompat.trace("chew at {}: last chew, cut through (stump {})", core, stump);
                return;
            }
            // 7. Chew one radius.
            e.setCanceled(true);
            b.setRadius(level, core, r - 1, null, 3);
            ItemStack stage = DTTrees.stageItem(stump, r - 1);
            popToward(level, core, face, stage);
            wearOne(level, p, held, cs, core);
            DynamicTreesCompat.trace("chew at {}: radius {} -> {}, {} (stump {})", core, r, r - 1, stage, stump);
        } catch (RuntimeException | LinkageError t) {
            DynamicTreesCompat.fail("the branch break handler", t);
        }
    }

    // ------------------------------------------------------------------ (b) dig speed of a chew

    /**
     * Both sides. A chew hit (a stump, or no AXE_DIG tool) digs at FC's log rate
     * ({@link FCMiningSpeed#fcLogDigProgress}, the C1 contract): {@code newSpeed = progress * H * divisor * newSpeed /
     * originalSpeed}, so BTW's penalty and other listeners' factors apply exactly once; a chewed (stripped) branch
     * digs as FCBlockLogDamaged. An axe fell keeps DT's radius-scaled time. A trunk shell already arrives as its
     * core's state and position.
     */
    // FCMOD-INTEGRATION(1.20.1): dynamic_trees: a chew hit on a DT branch takes FC's log dig time, a stump three times as long
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBreakSpeed(PlayerEvent.BreakSpeed e) {
        if (!DynamicTreesCompat.active) return;
        try {
            BlockState s = e.getState();
            if (!DTTrees.isLog(s)) return;
            Optional<BlockPos> at = e.getPosition();
            if (at.isEmpty()) return;
            BlockPos pos = at.get();
            Player p = e.getEntity();
            Level level = p.level();
            boolean stump = DTTrees.isStump(level, pos);
            boolean chew = stump || !p.getMainHandItem().canPerformAction(ToolActions.AXE_DIG);
            if (!chew) return;
            float orig = e.getOriginalSpeed();
            if (!(orig > 0F)) return;
            float prog = FCMiningSpeed.fcLogDigProgress(p, level, pos, stump);
            if (!(prog > 0F)) return;
            if (((BranchBlock) s.getBlock()).isStrippedBranch()) prog *= DTTrees.damagedLogFactor();
            float h = s.getDestroySpeed(level, pos);
            float div = ForgeHooks.isCorrectToolForDrops(s, p) ? 30F : 100F;
            e.setNewSpeed(prog * h * div * (e.getNewSpeed() / orig));
        } catch (RuntimeException | LinkageError t) {
            DynamicTreesCompat.fail("the branch dig-speed handler", t);
        }
    }

    // ------------------------------------------------------------------ (c) axe stripping

    /**
     * An axe strip by right click: recorded here, settled at END once DT stripped the branch inside its
     * {@code use()} (after this event): FC bark, and wear normalized to FC's 1 per convert.
     */
    // FCMOD-INTEGRATION(1.20.1): dynamic_trees: stripping a DT branch with an axe pays FC bark and 1 durability, as FC's log convert
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onStrip(PlayerInteractEvent.RightClickBlock e) {
        if (!DynamicTreesCompat.active) return;
        if (!(e.getLevel() instanceof ServerLevel level) || !(e.getEntity() instanceof ServerPlayer p)) return;
        try {
            if (e.getUseBlock() == Event.Result.DENY) return;
            ItemStack held = e.getItemStack();
            if (held.isEmpty() || !held.canPerformAction(ToolActions.AXE_STRIP)) return;
            BlockPos core = DTTrees.resolveCore(level, e.getPos());
            BlockState cs = level.getBlockState(core);
            if (!DTTrees.isLog(cs)) return;
            BranchBlock b = (BranchBlock) cs.getBlock();
            if (b.isStrippedBranch() || !b.getFamily().canStripBranch(cs, level, core, p, held)) return;
            Direction face = e.getFace() != null ? e.getFace() : Direction.UP;
            strips.put(key(level, core), new PendingStrip(p, held, damage(held), face, DTTrees.barkSpecies(b),
                    level.getServer().getTickCount()));
        } catch (RuntimeException | LinkageError t) {
            DynamicTreesCompat.fail("the axe strip handler", t);
        }
    }

    // ------------------------------------------------------------------ (d) the falling tree joins

    /**
     * Fires inside {@code BranchBlock.futureBreak}, after it read the main hand and its Fortune and multiplied the wood
     * volume by that Fortune factor (BranchBlock.java:597-601), and before its {@code damageAxe} on that stack (:613).
     * <ul>
     *   <li>Axe fell: n = max(1, floor(volume / (Fortune factor x DT's loot multipliers))) real logs; FC's harvest
     *       hunger for n logs, and wear target damageBefore + n on the stack that made the break. DT's Fortune and
     *       loot-multiplier bonus logs stay (decision D1 (b)).</li>
     *   <li>Chew-through: no logs in the payload (sticks and leaf drops stay), wear target damageBefore + 1.</li>
     *   <li>DT's deferred axe wear: a stack swapped into the main hand since the break is returned to its current
     *       damage at END (compensation); the stack DT is about to damage gets headroom so DT's hurtAndBreak cannot
     *       break it before END writes its target.</li>
     * </ul>
     */
    // FCMOD-INTEGRATION(1.20.1): dynamic_trees: a DT fell costs FC's harvest hunger and 1 durability per real log; a chewed-through tree drops no logs
    @SubscribeEvent
    public void onJoin(EntityJoinLevelEvent e) {
        if (!DynamicTreesCompat.active || fells.isEmpty()) return;
        if (e.loadedFromDisk() || !(e.getEntity() instanceof FallingTreeEntity fte)) return;
        if (!(e.getLevel() instanceof ServerLevel level)) return;
        try {
            if (fte.destroyType != FallingTreeEntity.DestroyType.HARVEST) return;
            BranchDestructionData d = fte.getDestroyData();
            if (d == null || d.cutPos == null) return;
            PendingFell pf = fells.remove(key(level, d.cutPos));
            if (pf == null) return;
            ItemStack dt = pf.p().getMainHandItem();
            int target;
            if (pf.harvest()) {
                int fortune = EnchantmentHelper.getTagEnchantmentLevel(Enchantments.BLOCK_FORTUNE, dt);
                float factor = 1F + 0.25F * fortune;
                // getBranchesDrops (BranchBlock.java:602) also scaled the same volume in place by DT's loot
                // multipliers (Species.processVolume: harvestMultiplier, the family's loot_volume_multiplier).
                double loot = DTConfigs.TREE_HARVEST_MULTIPLIER.get() * d.species.getFamily().getLootVolumeMultiplier();
                if (loot > 0D) factor *= (float) loot;
                float volume = d.woodVolume.getVolume();
                int n = Math.max(1, (int) Math.floor(volume / factor));
                FCLogRules.chargeLogHarvest(pf.p(), n, pf.woodMeta(), d.cutPos);
                target = pf.damageBefore() + n;
                DynamicTreesCompat.trace("fell at {}: n={} (volume {}, fortune {}), wear target {}", d.cutPos, n,
                        volume, fortune, target);
            } else {
                try {
                    fte.getPayload().removeIf(st -> st.is(ItemTags.LOGS));
                } catch (UnsupportedOperationException x) {
                    if (!payloadWarned) {
                        payloadWarned = true;
                        DynamicTreesCompat.LOGGER.warn("[BTW-Compat] dynamictrees: a falling tree's payload is "
                                + "immutable; a chewed-through tree keeps its logs (logged once)");
                    }
                }
                target = pf.damageBefore() + 1;
                DynamicTreesCompat.trace("chew-through at {}: logs removed, wear target {}", d.cutPos, target);
            }
            wears.add(new Wear(pf.p(), pf.stack(), target));
            // DT's damageAxe precondition (ItemUtils.java:63): only an AXE_DIG stack takes DT's wear.
            if (!dt.isEmpty() && dt.isDamageableItem() && dt.canPerformAction(ToolActions.AXE_DIG)) {
                if (dt != pf.stack()) {
                    wears.add(new Wear(pf.p(), dt, dt.getDamageValue()));
                }
                dt.setDamageValue(0);
            }
        } catch (RuntimeException | LinkageError t) {
            DynamicTreesCompat.fail("the falling-tree join handler", t);
        }
    }

    // ------------------------------------------------------------------ (e) settle at END

    /**
     * After DT's FutureBreaks and strips of this tick: strip bark and wear, then every absolute wear target (both
     * directions; the last point through hurtAndBreak, so the break event, stat and Unbreaking behave as vanilla
     * wear). Queued wear is applied even after a switch-off, so a stack given headroom in {@link #onJoin} always gets
     * its real damage back.
     */
    // FCMOD-INTEGRATION(1.20.1): dynamic_trees: FC's wear count replaces DT's axe wear on the stack that made the break
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (fells.isEmpty() && strips.isEmpty() && wears.isEmpty()) return;
        try {
            MinecraftServer server = e.getServer();
            long now = server.getTickCount();
            if (DynamicTreesCompat.active) settleStrips(server, now);
            applyWears();
            fells.values().removeIf(pf -> now - pf.tick() > EXPIRY_TICKS);
            if (!DynamicTreesCompat.active) {
                fells.clear();
                strips.clear();
            }
        } catch (RuntimeException | LinkageError t) {
            wears.clear();
            fells.clear();
            strips.clear();
            DynamicTreesCompat.fail("the server-tick wear handler", t);
        }
    }

    private void settleStrips(MinecraftServer server, long now) {
        Iterator<Map.Entry<Key, PendingStrip>> it = strips.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Key, PendingStrip> entry = it.next();
            Key k = entry.getKey();
            PendingStrip ps = entry.getValue();
            ServerLevel level = server.getLevel(k.dim());
            if (level != null && level.isLoaded(k.pos())
                    && level.getBlockState(k.pos()).getBlock() instanceof BranchBlock bb && bb.isStrippedBranch()) {
                popToward(level, k.pos(), ps.face(), FCLogRules.bark(ps.barkSpecies()));
                if (!ps.p().getAbilities().instabuild) {
                    wears.add(new Wear(ps.p(), ps.stack(), ps.damageBefore() + 1));
                }
                DynamicTreesCompat.trace("strip bark at {} (wood {}), wear target {}", k.pos(), ps.barkSpecies(),
                        ps.damageBefore() + 1);
                it.remove();
            } else if (now - ps.tick() > EXPIRY_TICKS) {
                it.remove();
            }
        }
    }

    private void applyWears() {
        for (Wear w : wears) {
            ItemStack st = w.stack();
            if (st.isEmpty() || !st.isDamageableItem()) continue;
            int max = st.getMaxDamage();
            if (w.target() >= max) {
                st.setDamageValue(max - 1);
                InteractionHand hand = w.p().getOffhandItem() == st
                        ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
                st.hurtAndBreak(1, w.p(), pl -> pl.broadcastBreakEvent(hand));
            } else {
                st.setDamageValue(Math.max(0, w.target()));
            }
        }
        wears.clear();
    }

    // ------------------------------------------------------------------ (f) server stop

    @SubscribeEvent
    public void onStopped(ServerStoppedEvent e) {
        fells.clear();
        strips.clear();
        wears.clear();
    }

    // ------------------------------------------------------------------ helpers

    private static Key key(ServerLevel level, BlockPos pos) {
        return new Key(level.dimension(), pos.immutable());
    }

    private static int damage(ItemStack stack) {
        return stack.isEmpty() ? 0 : stack.getDamageValue();
    }

    /**
     * FC's per-hit convert wear (Modern-Common ItemInWorldManager onBlockDestroyed): the held item's mineBlock, which
     * ProxyItem routes to FC's onBlockDestroyed for FC tools; a stack used up fires Forge's destroy-item event. The
     * log rule's own helper ({@link FCLogRules#wearConvert}).
     */
    private static void wearOne(ServerLevel level, ServerPlayer p, ItemStack held, BlockState state, BlockPos pos) {
        FCLogRules.wearConvert(level, p, held, state, pos);
    }

    /** The face the player breaks {@code pos} from: the dig packet's, else the look ray's, else UP ({@link FCLogRules#hitFace}). */
    private static Direction hitFace(ServerPlayer p, BlockPos pos) {
        return FCLogRules.hitFace(p, pos);
    }

    private static void popToward(ServerLevel level, BlockPos pos, Direction face, ItemStack stack) {
        FCLogRules.popToward(level, pos, face, stack);
    }

    private static void pop(ServerLevel level, BlockPos pos, ItemStack stack) {
        if (!stack.isEmpty()) Block.popResource(level, pos, stack);
    }
}
