package btw.forge.mixin;

import btw.forge.EntityBridge;
import btw.forge.PlayerBridge;
import btw.forge.ProxyAnimal;
import btw.forge.ProxyBlock;
import btw.forge.ProxyEntity;
import btw.forge.ProxyMob;
import btw.forge.ProxyPathfinderMob;
import btw.forge.ProxyRegistry;
import btw.forge.WorldBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Intercepts vanilla {@link BlockBehaviour} callbacks and redirects them
 * to FC block behavior when a matching FC block exists for the vanilla block
 * at the given position.
 *
 * ProxyBlocks already override these methods directly, so these injections
 * only fire for vanilla blocks that have FC replacements in
 * {@code btw.modern.Block.blocksList[]}.
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
     * Wraps a Minecraft entity into an FC entity for use in FC block callbacks.
     * Uses EntityBridge as a fallback for vanilla entities without dedicated proxies.
     */
    private btw.modern.Entity btw$wrapEntity(Entity entity) {
        if (entity == null) return null;

        // Player → PlayerBridge
        if (entity instanceof net.minecraft.world.entity.player.Player player) {
            PlayerBridge bridge = PlayerBridge.getOrCreate(player);
            bridge.syncFromReal();
            return bridge;
        }
        // ProxyMob → getFcEntity()
        if (entity instanceof ProxyMob proxy) {
            return proxy.getFcEntity();
        }
        // ProxyAnimal → getFcEntity()
        if (entity instanceof ProxyAnimal proxy) {
            return proxy.getFcEntity();
        }
        // ProxyPathfinderMob → getFcEntity()
        if (entity instanceof ProxyPathfinderMob proxy) {
            return proxy.getFcEntity();
        }
        // ProxyEntity → getFcEntity()
        if (entity instanceof ProxyEntity proxy) {
            return proxy.getFcEntity();
        }
        // Vanilla entities without FC proxy → use generic EntityBridge
        EntityBridge bridge = EntityBridge.getOrCreate(entity);
        bridge.syncFromReal();
        return bridge;
    }

    // ================================================================
    // getDestroyProgress -> FC hardness
    // ================================================================

    @Inject(method = "getDestroyProgress", at = @At("HEAD"), cancellable = true)
    private void btw$getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos,
                                         CallbackInfoReturnable<Float> cir) {
        btw.modern.Block fcBlock = btw$getFcBlock();
        if (fcBlock == null) return;

        // Get the FC player — works for BOTH ServerPlayer and client LocalPlayer.
        // FC's getPlayerRelativeBlockHardness handles EVERYTHING:
        // block hardness, tool effectiveness, tool speed, penalties, 30/200 divisor.
        btw.forge.PlayerBridge fcPlayer = btw.forge.PlayerBridge.getOrCreate(player);
        fcPlayer.syncFromReal();

        // FC method needs a World — may be null on client, but
        // getPlayerRelativeBlockHardness handles null world gracefully
        // (getBlockHardness falls back to blockHardness field)
        float result = fcBlock.getPlayerRelativeBlockHardness(
                fcPlayer, fcPlayer.worldObj, pos.getX(), pos.getY(), pos.getZ());
        cir.setReturnValue(result);
    }

    // ================================================================
    // tick -> FC updateTick
    // ================================================================

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void btw$tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random,
                           CallbackInfo ci) {
        btw.modern.Block fcBlock = btw$getFcBlock();
        if (fcBlock == null) return;

        btw.modern.World world = WorldBridge.getOrCreate(level);
        fcBlock.updateTick(world, pos.getX(), pos.getY(), pos.getZ(),
                new Random(random.nextLong()));
        ci.cancel();
    }

    // ================================================================
    // randomTick -> FC RandomUpdateTick
    // ================================================================

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void btw$randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random,
                                 CallbackInfo ci) {
        btw.modern.Block fcBlock = btw$getFcBlock();
        if (fcBlock == null) return;

        btw.modern.World world = WorldBridge.getOrCreate(level);
        fcBlock.RandomUpdateTick(world, pos.getX(), pos.getY(), pos.getZ(),
                new Random(random.nextLong()));
        ci.cancel();
    }

    // ================================================================
    // neighborChanged -> FC onNeighborBlockChange
    // ================================================================

    @Inject(method = "neighborChanged", at = @At("HEAD"), cancellable = true)
    private void btw$neighborChanged(BlockState state, Level level, BlockPos pos,
                                      Block neighborBlock, BlockPos neighborPos, boolean moving,
                                      CallbackInfo ci) {
        btw.modern.Block fcBlock = btw$getFcBlock();
        if (fcBlock == null) return;

        if (level instanceof ServerLevel sl) {
            btw.modern.World world = WorldBridge.getOrCreate(sl);
            int neighborId = ProxyRegistry.getBlockId(neighborBlock);
            fcBlock.onNeighborBlockChange(world, pos.getX(), pos.getY(), pos.getZ(), neighborId);
            ci.cancel();
        }
    }

    // ================================================================
    // onPlace -> FC onBlockAdded
    // ================================================================

    @Inject(method = "onPlace", at = @At("HEAD"), cancellable = true)
    private void btw$onPlace(BlockState state, Level level, BlockPos pos,
                              BlockState oldState, boolean moving, CallbackInfo ci) {
        btw.modern.Block fcBlock = btw$getFcBlock();
        if (fcBlock == null) return;

        if (!state.is(oldState.getBlock()) && level instanceof ServerLevel sl) {
            btw.modern.World world = WorldBridge.getOrCreate(sl);
            fcBlock.onBlockAdded(world, pos.getX(), pos.getY(), pos.getZ());
            ci.cancel();
        }
    }

    // ================================================================
    // onRemove -> FC breakBlock
    // ================================================================

    @Inject(method = "onRemove", at = @At("HEAD"), cancellable = true)
    private void btw$onRemove(BlockState state, Level level, BlockPos pos,
                               BlockState newState, boolean moving, CallbackInfo ci) {
        btw.modern.Block fcBlock = btw$getFcBlock();
        if (fcBlock == null) return;

        if (!state.is(newState.getBlock()) && level instanceof ServerLevel sl) {
            btw.modern.World world = WorldBridge.getOrCreate(sl);
            int legacyId = ProxyRegistry.getBlockId(state.getBlock());
            int meta = 0; // vanilla blocks don't carry FC metadata
            fcBlock.breakBlock(world, pos.getX(), pos.getY(), pos.getZ(), legacyId, meta);
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
        btw.modern.Block fcBlock = btw$getFcBlock();
        if (fcBlock == null) return;

        if (level instanceof ServerLevel sl) {
            btw.modern.World world = WorldBridge.getOrCreate(sl);
            int side = hit.getDirection().get3DDataValue();
            Vec3 hitLoc = hit.getLocation();
            // Player wrapping not yet implemented — pass null
            boolean result = fcBlock.onBlockActivated(world, pos.getX(), pos.getY(), pos.getZ(),
                    null, side,
                    (float) (hitLoc.x - pos.getX()),
                    (float) (hitLoc.y - pos.getY()),
                    (float) (hitLoc.z - pos.getZ()));
            if (result) {
                cir.setReturnValue(InteractionResult.SUCCESS);
            }
            // If FC returned false, let vanilla handle it (don't cancel)
        }
    }

    // ================================================================
    // entityInside -> FC onEntityCollidedWithBlock
    // ================================================================

    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
    private void btw$entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                   CallbackInfo ci) {
        btw.modern.Block fcBlock = btw$getFcBlock();
        if (fcBlock == null) return;

        if (level instanceof ServerLevel sl) {
            btw.modern.World world = WorldBridge.getOrCreate(sl);
            btw.modern.Entity fcEntity = btw$wrapEntity(entity);
            fcBlock.onEntityCollidedWithBlock(world, pos.getX(), pos.getY(), pos.getZ(), fcEntity);
            ci.cancel();
        }
    }

    // ================================================================
    // getSignal -> FC isProvidingWeakPower
    // ================================================================

    @Inject(method = "getSignal", at = @At("HEAD"), cancellable = true)
    private void btw$getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction,
                                CallbackInfoReturnable<Integer> cir) {
        btw.modern.Block fcBlock = btw$getFcBlock();
        if (fcBlock == null) return;

        if (level instanceof ServerLevel sl) {
            btw.modern.World world = WorldBridge.getOrCreate(sl);
            int result = fcBlock.isProvidingWeakPower(world, pos.getX(), pos.getY(), pos.getZ(),
                    direction.get3DDataValue());
            cir.setReturnValue(result);
        }
    }

    // ================================================================
    // getDirectSignal -> FC isProvidingStrongPower
    // ================================================================

    @Inject(method = "getDirectSignal", at = @At("HEAD"), cancellable = true)
    private void btw$getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction,
                                      CallbackInfoReturnable<Integer> cir) {
        btw.modern.Block fcBlock = btw$getFcBlock();
        if (fcBlock == null) return;

        if (level instanceof ServerLevel sl) {
            btw.modern.World world = WorldBridge.getOrCreate(sl);
            int result = fcBlock.isProvidingStrongPower(world, pos.getX(), pos.getY(), pos.getZ(),
                    direction.get3DDataValue());
            cir.setReturnValue(result);
        }
    }

    // NOTE: getDrops is NOT intercepted. FC's complete drop system runs through
    // Block.harvestBlock() or Block.OnBlockDestroyedWithImproperTool() called from
    // BlockMixin.btw$playerDestroy. FC code internally calls dropBlockAsItem() →
    // dropBlockAsItemWithChance() → dropBlockAsItem_do() → world.spawnEntityInWorld()
    // which creates real MC ItemEntities via EntityProxyFactory.

    // ================================================================
    // isSignalSource -> FC canProvidePower
    // ================================================================

    @Inject(method = "isSignalSource", at = @At("HEAD"), cancellable = true)
    private void btw$isSignalSource(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        btw.modern.Block fcBlock = btw$getFcBlock();
        if (fcBlock == null) return;

        cir.setReturnValue(fcBlock.canProvidePower());
    }

    // ================================================================
    // attack -> FC onBlockClicked
    // ================================================================

    @Inject(method = "attack", at = @At("HEAD"))
    private void btw$attack(BlockState state, Level level, BlockPos pos, Player player,
                             CallbackInfo ci) {
        btw.modern.Block fcBlock = btw$getFcBlock();
        if (fcBlock == null) return;

        if (level instanceof ServerLevel sl) {
            btw.modern.World world = WorldBridge.getOrCreate(sl);
            fcBlock.onBlockClicked(world, pos.getX(), pos.getY(), pos.getZ(), null);
        }
    }
}
