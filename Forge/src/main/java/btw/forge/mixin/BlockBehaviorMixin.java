package btw.forge.mixin;

import btw.forge.FcAttachSupport;
import btw.forge.HookPolicy;
import btw.forge.PlayerBridge;
import btw.forge.ProxyBlock;
import btw.forge.ProxyRegistry;
import btw.forge.WorldBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Intercepts vanilla {@link BlockBehaviour} callbacks and redirects them
 * to FC block behavior when a matching FC block exists for the vanilla block
 * at the given position.
 *
 * ProxyBlocks already override these methods directly, so these injections
 * only fire for vanilla blocks that have FC replacements in
 * {@code btw.modern.Block.blocksList[]}.
 *
 * FCMOD-INTEGRATION(1.20.1): hook_ownership. These are the NOTIFY executors of
 * {@link btw.forge.HookPolicy}: they run inside the 1.20.1 base bodies, so they fire only
 * where the 1.20.1 class does not override the hook or calls super, and each runs FC only
 * when the cached owner of the state it runs for allows it ({@link btw.forge.HookPolicy#notifyRuns}:
 * NOTIFY, UNDECIDED, never VANILLA). StairBlock's base delegation therefore gets the base
 * block's owner. The FC-owned rows (the anvils' use, the pumpkins' onPlace) are enforced at
 * the BlockStateBase forwarders by {@link BlockStateHookMixin}. Every FC call is caught and
 * logged once per throw site ({@link btw.forge.HookPolicy#warnOnce}): an FC failure answers
 * the 1.20.1 result and never escapes LevelChunk.setBlockState or a packet handler; a throw
 * with the player's FC inventory in play discards its pending edits (INV-RESYNC). USE (Stage
 * 1, FC's onBlockActivated) and REMOVE never run FC's block for a state whose FC block is a
 * btw.modern.BlockContainer: its block entity is 1.20.1's, and FC's casts of it crashed beacon
 * and ender-chest removals ({@link btw.forge.HookPolicy#isTeGuarded}). A VANILLA USE owner
 * drops Stage 1 only: the held FC item (Stage 2) still runs, as in 4.B.
 *
 * Ticks are not routed here. Random and scheduled ticks of FC-mapped states are
 * dispatched at the BlockState level by {@link BlockStateRandomTickMixin} and
 * {@link BlockStateBaseTickMixin} under the rule in {@link btw.forge.TickPolicy}. Do not add tick or randomTick injects to this
 * class: a BlockBehaviour-level hook keys on the wrong block (StairBlock passes the
 * stair state to {@code base.tick / randomTick}, so FCBlockStoneBrick.updateTick
 * would run at a stair position), never sees the 1.20.1 classes that override
 * tick or randomTick, and would be a second routing path that ignores the policy.
 */
@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviorMixin {


    // ================================================================
    // Helper: get FC block for this BlockBehaviour instance
    // ================================================================

    private btw.modern.Block btw$getFcBlock() {
        // 'this' is a BlockBehaviour, which all Block instances extend.
        // Skip ProxyBlocks — they already handle FC forwarding directly.
        if ((Object) this instanceof ProxyBlock) {
            return null;
        }
        if ((Object) this instanceof Block block) {
            return ProxyRegistry.getFcBlock(block);
        }
        return null;
    }

    /**
     * The FC twin of a post-1.5.2 plant state (btw.forge.TwinRegistry: modern saplings, azaleas, beetroots, the
     * torchflower and pitcher crops, fungi), or null. Identity routing of these plants stays 0 (btw$getFcBlock), but
     * FC's weeds grow under a twin crop (BlockCrops.CanWeedsGrowInBlock, read by FCBlockFarmland through the twin's
     * world id) and stop it (BlockCrops.AttemptToGrow), and 4.B pulls weeds only through the plant's
     * onBlockActivated (Client FCBlockPlants.java:84-102, Modern-Common BlockFlower.onBlockActivated), which runs
     * before the held item: bone meal on a weedy crop pulls the weeds and is kept. Used by btw$use, Stage 1 only.
     */
    private static btw.modern.Block btw$getTwinFcBlock(BlockState state) {
        int id = btw.forge.TwinRegistry.worldIdFor(state);
        return id > 0 && id < btw.modern.Block.blocksList.length ? btw.modern.Block.blocksList[id] : null;
    }

    /**
     * Wraps a Minecraft entity into an FC entity for use in FC block callbacks (onEntityCollidedWithBlock): the one
     * contact wrapper, {@link btw.forge.VanillaEntityViews#forContact} (FCMOD-INTEGRATION(1.20.1): fc_entity_views),
     * shared with ProxyBlock and BlockMixin: a synced PlayerBridge, a puppet's own FC object, an item's FC EntityItem
     * view (`instanceof EntityItem` callbacks: FCBlockCampfire:442, FCBlockFarmlandBase:153, FCBlockPlanterBase:61,
     * FCBlockNetherGrowth:276), a LivingEntityBridge, or the class or generic view of any other 1.20.1 entity. Null when
     * the entity has no FC object to show -- the caller skips the FC callback.
     */
    private btw.modern.Entity btw$wrapEntity(Entity entity) {
        return btw.forge.VanillaEntityViews.forContact(entity);
    }

    // ================================================================
    // getDestroyProgress -> FC getPlayerRelativeBlockHardness
    // ================================================================

    // FC's getPlayerRelativeBlockHardness (vanilla/server Block.java:2340-2361) folds in block hardness,
    // tool effectiveness/speed, potions, water/airborne and FC penalties with the 30/200 divisor. Routed
    // through FCMiningSpeed so the client predicts with its own LocalPlayer bridge and client world (which
    // derives the same legacy metadata as the server, e.g. the log stump bit) and the server uses the
    // ServerPlayer bridge + WorldBridge - never the server bridge from the render thread.
    @Inject(method = "getDestroyProgress", at = @At("HEAD"), cancellable = true)
    private void btw$getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos,
                                         CallbackInfoReturnable<Float> cir) {
        btw.modern.Block fcBlock = btw$getFcBlock();
        if (fcBlock == null) return;

        float result = btw.forge.FCMiningSpeed.relativeHardness(fcBlock, player, level, pos);
        if (!Float.isNaN(result)) {
            cir.setReturnValue(result);
        }
        // NaN: FC threw (logged by FCMiningSpeed) -> vanilla formula instead of failing the dig.
    }

    // ================================================================
    // neighborChanged -> FC onNeighborBlockChange
    // ================================================================

    @Inject(method = "neighborChanged", at = @At("HEAD"), cancellable = true)
    private void btw$neighborChanged(BlockState state, Level level, BlockPos pos,
                                      Block neighborBlock, BlockPos neighborPos, boolean moving,
                                      CallbackInfo ci) {
        // FCMOD-INTEGRATION(1.20.1): hook_ownership: FC runs only where the state's NEIGHBOR owner allows it (RULED(4) door, trapdoor and gate redstone, RULED(5) pair blocks), and an FC throw answers the 1.20.1 result
        if (!HookPolicy.notifyRuns(state, HookPolicy.NEIGHBOR)) return;
        btw.modern.Block fcBlock = btw$getFcBlock();
        if (fcBlock == null) {
            // FCMOD-INTEGRATION(1.20.1): attach_support_rules: a 1.20.1 block that takes an FC attachment rule through
            // FcAttachSupport's support twins (soul torch, soul wall torch, the modern pressure plates; rulings 2026-10-23,
            // 2026-10-25, 2026-10-31) drops at any neighbour change on a support 4.B refuses, as
            // FCBlockTorchBase.ValidateState drops the torch and FCBlockPressurePlate's neighbour check the plate; the pop
            // is 1.20.1's own pop (Block.updateOrDestroy -> destroyBlock: break effect, its loot, flag 3)
            if (level instanceof ServerLevel twinLevel && FcAttachSupport.isSupportTwin(state.getBlock())
                    && twinLevel.getBlockState(pos) == state && FcAttachSupport.refuses(state, twinLevel, pos)) {
                twinLevel.destroyBlock(pos, true);
            }
            return;
        }

        if (level instanceof ServerLevel sl) {
            try {
                btw.modern.World world = WorldBridge.getOrCreate(sl);
                int neighborId = ProxyRegistry.getNeighbourSourceId(neighborBlock, level, neighborPos);
                fcBlock.onNeighborBlockChange(world, pos.getX(), pos.getY(), pos.getZ(), neighborId);
            } catch (Throwable t) {
                HookPolicy.warnOnce("NEIGHBOR", fcBlock, pos, t);
            }
            // Don't cancel neighborChanged for vanilla blocks — MC needs its own
            // neighbor logic (redstone, dropper triggering, etc.) to run.
        }
    }

    // ================================================================
    // onPlace -> FC onBlockAdded
    // ================================================================

    @Inject(method = "onPlace", at = @At("HEAD"))
    private void btw$onPlace(BlockState state, Level level, BlockPos pos,
                              BlockState oldState, boolean moving, CallbackInfo ci) {
        // FCMOD-INTEGRATION(1.20.1): hook_ownership: owner-gated and caught; an FC-owned PLACE (the pumpkins) ran at the forwarder (BlockStateHookMixin)
        if (!HookPolicy.notifyRuns(state, HookPolicy.PLACE)) return;
        btw.modern.Block fcBlock = btw$getFcBlock();
        if (fcBlock == null) return;

        if (!state.is(oldState.getBlock()) && level instanceof ServerLevel sl && !WorldBridge.isMetadataOnlyWrite(pos)) {
            try {
                btw.modern.World world = WorldBridge.getOrCreate(sl);
                fcBlock.onBlockAdded(world, pos.getX(), pos.getY(), pos.getZ());
            } catch (Throwable t) {
                HookPolicy.warnOnce("PLACE", fcBlock, pos, t);
            }
        }
    }

    // ================================================================
    // onRemove -> FC breakBlock
    // ================================================================

    @Inject(method = "onRemove", at = @At("HEAD"), cancellable = true)
    private void btw$onRemove(BlockState state, Level level, BlockPos pos,
                               BlockState newState, boolean moving, CallbackInfo ci) {
        // The FC block FC's world reads see here: the identity mapping, or the log for a bark / stripped log form
        // (ProxyRegistry.getRemovalLegacyId), so cutting a stripped trunk runs FC's log removal (leaf decay check).
        if ((Object) this instanceof ProxyBlock || !((Object) this instanceof Block self)) return;
        // FCMOD-INTEGRATION(1.20.1): hook_ownership: the old state's REMOVE owner gates FC (VANILLA for removed blocks and block-entity blocks)
        if (!HookPolicy.notifyRuns(state, HookPolicy.REMOVE)) return;
        int legacyId = ProxyRegistry.getRemovalLegacyId(self);
        if (legacyId <= 0 || legacyId >= btw.modern.Block.blocksList.length) return;
        btw.modern.Block fcBlock = btw.modern.Block.blocksList[legacyId];
        if (fcBlock == null) return;
        // The block-entity guard, also for states without a cached owner: the block entity here is 1.20.1's, and
        // WorldBridge.getBlockTileEntity hands FC a TileEntityBridge for it, which FCBlockBeacon.breakBlock /
        // FCBlockEnderChest.breakBlock cast (Client FCBlockBeacon.java:42, FCBlockEnderChest.java:104): the
        // ClassCastException left an orphan block entity, lost the drop and crashed explosions. BlockBehaviour's own
        // onRemove (block entity removal) runs either way.
        if (HookPolicy.isTeGuarded(self, fcBlock)) return;

        if (!state.is(newState.getBlock()) && level instanceof ServerLevel sl && !WorldBridge.isMetadataOnlyWrite(pos)) {
            try {
                btw.modern.World world = WorldBridge.getOrCreate(sl);
                // 1.5.2 passes the removed block's metadata (vanilla/server Chunk.java:581): the old state's derived
                // metadata (WorldBridge.deriveVanillaMetadata, WorldBridge.java:307; never throws, 0 on a failure)
                int meta = WorldBridge.deriveVanillaMetadata(level, state, pos);
                fcBlock.breakBlock(world, pos.getX(), pos.getY(), pos.getZ(), legacyId, meta);
            } catch (Throwable t) {
                HookPolicy.warnOnce("REMOVE", fcBlock, pos, t);
            }
            // Do NOT cancel — let vanilla handle block entity removal etc.
        }
    }

    // ================================================================
    // use -> FC onBlockActivated
    // ================================================================

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void btw$use(BlockState state, Level level, BlockPos pos,
                          Player player, InteractionHand hand, BlockHitResult hit,
                          CallbackInfoReturnable<InteractionResult> cir) {
        // Skip for ProxyBlocks — they handle FC delegation in ProxyBlock.use()
        if (state.getBlock() instanceof btw.forge.ProxyBlock) return;
        // FCMOD-INTEGRATION(1.20.1): fc_item_use_order: Stage 1 (twins' too) and Stage 2 run for the main hand only; the off-hand pass gets 1.20.1's use
        // The 1.20.1 client sends an OFF_HAND useItemOn after a main-hand pass that did not consume (Minecraft.java:1662,
        // :1698-1720). FC here would re-run Stage 1 and use the MAIN-hand FC item (the FC player has only the main hand),
        // sticking the main-hand tool into the block on an off-hand click. Returning without a cancel leaves the block's
        // own use to run.
        if (hand != InteractionHand.MAIN_HAND) return;
        btw.modern.Block fcBlock = btw$getFcBlock();
        // FCMOD-INTEGRATION(1.20.1): modern_plant_growth: a twin plant gets FC's activation (Stage 1 only), so the weeds FC grows under twin crops can be pulled as under FC's own crops
        boolean twin = false;
        if (fcBlock == null) {
            fcBlock = btw$getTwinFcBlock(state);
            twin = fcBlock != null;
        }
        if (fcBlock == null) return;
        // FCMOD-INTEGRATION(1.20.1): hook_ownership: the USE owner gates Stage 1 only (RULED(3) beds, the pending door / cake / workbench / pot rows, removed and block-entity blocks keep 1.20.1's activation); Stage 2, the held FC item, still runs, as 4.B ran it whatever the block did (vanilla/server ItemInWorldManager.java:376-407); an FC-owned USE (the anvils) ran at the forwarder (BlockStateHookMixin)
        // The block-entity guard (see btw$onRemove) also covers states without a cached owner: FC's activation of a
        // container block casts the 1.20.1 block entity (Client FCBlockEnderChest.java:39, FCBlockBeacon.java:69)
        boolean stage1 = HookPolicy.notifyRuns(state, HookPolicy.USE) && !HookPolicy.isTeGuarded(state.getBlock(), fcBlock);
        // A twin has no Stage 2, so nothing is left to run
        if (!stage1 && twin) return;

        if (level instanceof ServerLevel sl) {
            // Stage 1 (when the owner allows it), then Stage 2 (never for a twin: it stands for a 1.20.1 plant whose
            // items stay 1.20.1's, identity id 0, so the held item's own use follows; bone meal: BoneMealItemMixin's
            // soil relay), the menu, the write-back and the INV-RESYNC resend: HookPolicy.activate, shared with the
            // FC-owned executor. The client got PASS from this block's use (the base body) and predicted the held
            // BlockItem's placement, place sound included, so FC's place sound leaves this player out
            // (WorldBridge.PREDICTING_ACTOR). ProxyBlock.use needs no such scope: its client returns SUCCESS and
            // predicts nothing. A Stage 1 throw is logged once, discards FC's pending inventory edits and answers the
            // 1.20.1 result (null).
            if (HookPolicy.activate(sl, pos, player, hand, hit, fcBlock, stage1, twin, true)
                    == InteractionResult.SUCCESS) {
                cir.setReturnValue(InteractionResult.SUCCESS);
            }
        }
    }

    // ================================================================
    // entityInside -> FC onEntityCollidedWithBlock
    // ================================================================

    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
    private void btw$entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                   CallbackInfo ci) {
        // FCMOD-INTEGRATION(1.20.1): hook_ownership: owner-gated (detector rails: equivalent, FC's cart rule runs inside 1.20.1's own check, DetectorRailBlockMixin) and caught; a throw keeps the 1.20.1 body
        if (!HookPolicy.notifyRuns(state, HookPolicy.INSIDE)) return;
        btw.modern.Block fcBlock = btw$getFcBlock();
        if (fcBlock == null) return;

        if (level instanceof ServerLevel sl) {
            try {
                btw.modern.World world = WorldBridge.getOrCreate(sl);
                btw.modern.Entity fcEntity = btw$wrapEntity(entity);
                if (fcEntity != null) {
                    fcBlock.onEntityCollidedWithBlock(world, pos.getX(), pos.getY(), pos.getZ(), fcEntity);
                }
                ci.cancel();
            } catch (Throwable t) {
                HookPolicy.warnOnce("INSIDE", fcBlock, pos, t);
            }
        }
    }

    // ================================================================
    // getSignal -> FC isProvidingWeakPower
    // ================================================================

    @Inject(method = "getSignal", at = @At("HEAD"), cancellable = true)
    private void btw$getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction,
                                CallbackInfoReturnable<Integer> cir) {
        // FCMOD-INTEGRATION(1.20.1): hook_ownership: owner-gated and caught; a throw answers 1.20.1's signal
        if (!HookPolicy.notifyRuns(state, HookPolicy.SIGNAL)) return;
        btw.modern.Block fcBlock = btw$getFcBlock();
        if (fcBlock == null) return;

        if (level instanceof ServerLevel sl) {
            try {
                btw.modern.World world = WorldBridge.getOrCreate(sl);
                int result = fcBlock.isProvidingWeakPower(world, pos.getX(), pos.getY(), pos.getZ(),
                        direction.get3DDataValue());
                cir.setReturnValue(result);
            } catch (Throwable t) {
                HookPolicy.warnOnce("SIGNAL", fcBlock, pos, t);
            }
        }
    }

    // ================================================================
    // getDirectSignal -> FC isProvidingStrongPower
    // ================================================================

    @Inject(method = "getDirectSignal", at = @At("HEAD"), cancellable = true)
    private void btw$getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction,
                                      CallbackInfoReturnable<Integer> cir) {
        // FCMOD-INTEGRATION(1.20.1): hook_ownership: owner-gated and caught; a throw answers 1.20.1's signal
        if (!HookPolicy.notifyRuns(state, HookPolicy.SIGNAL)) return;
        btw.modern.Block fcBlock = btw$getFcBlock();
        if (fcBlock == null) return;

        if (level instanceof ServerLevel sl) {
            try {
                btw.modern.World world = WorldBridge.getOrCreate(sl);
                int result = fcBlock.isProvidingStrongPower(world, pos.getX(), pos.getY(), pos.getZ(),
                        direction.get3DDataValue());
                cir.setReturnValue(result);
            } catch (Throwable t) {
                HookPolicy.warnOnce("SIGNAL", fcBlock, pos, t);
            }
        }
    }

    // NOTE: getDrops is NOT intercepted. FC's complete drop system runs through
    // Block.harvestBlock() or Block.OnBlockDestroyedWithImproperTool(), called from FC's
    // harvest (ServerPlayerGameModeMixin.btw$destroyBlock). FC code internally calls dropBlockAsItem() →
    // dropBlockAsItemWithChance() → dropBlockAsItem_do() → world.spawnEntityInWorld()
    // which creates real MC ItemEntities via EntityProxyFactory. Exception (vanilla_pair_drops):
    // beds and doors take 1.20.1's own break and loot, and an FC drop of their id at a live
    // cell is that cell's loot (World.UsesModernBlockLoot).

    // ================================================================
    // isSignalSource -> FC canProvidePower
    // ================================================================

    @Inject(method = "isSignalSource", at = @At("HEAD"), cancellable = true)
    private void btw$isSignalSource(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        // FCMOD-INTEGRATION(1.20.1): hook_ownership: owner-gated and caught; a throw answers 1.20.1's flag
        if (!HookPolicy.notifyRuns(state, HookPolicy.SIGNAL)) return;
        btw.modern.Block fcBlock = btw$getFcBlock();
        if (fcBlock == null) return;

        try {
            cir.setReturnValue(fcBlock.canProvidePower());
        } catch (Throwable t) {
            HookPolicy.warnOnce("SIGNAL", fcBlock, null, t);
        }
    }

    // ================================================================
    // attack -> FC onBlockClicked
    // ================================================================

    @Inject(method = "attack", at = @At("HEAD"))
    private void btw$attack(BlockState state, Level level, BlockPos pos, Player player,
                             CallbackInfo ci) {
        // FCMOD-INTEGRATION(1.20.1): hook_ownership: owner-gated and caught
        if (!HookPolicy.notifyRuns(state, HookPolicy.ATTACK)) return;
        btw.modern.Block fcBlock = btw$getFcBlock();
        if (fcBlock == null) return;

        if (level instanceof ServerLevel sl) {
            PlayerBridge fcPlayer = null;
            try {
                btw.modern.World world = WorldBridge.getOrCreate(sl);
                // 1.5.2 passes the clicking player (ItemInWorldManager.onBlockClicked); a synced PlayerBridge, as
                // ProxyBlock.attack does, never null
                fcPlayer = PlayerBridge.getOrCreate(player);
                fcPlayer.syncFromReal();
                fcBlock.onBlockClicked(world, pos.getX(), pos.getY(), pos.getZ(), fcPlayer);
            } catch (Throwable t) {
                // INV-RESYNC: a failed FC call lands nothing; the next sync dropped its edits, it would now write them
                if (fcPlayer != null) fcPlayer.discardInventoryEdits();
                HookPolicy.warnOnce("ATTACK", fcBlock, pos, t);
            }
        }
    }
}
