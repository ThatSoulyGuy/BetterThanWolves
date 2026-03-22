package btw.forge.mixin;

import btw.forge.ProxyBlock;
import btw.forge.ProxyRegistry;
import btw.forge.WorldBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Intercepts vanilla {@link Block} callbacks and redirects them to FC block
 * behavior when a matching FC block exists for the vanilla block.
 *
 * ProxyBlocks already override these methods, so these injections only fire
 * for vanilla blocks that have FC counterparts.
 */
@Mixin(Block.class)
public abstract class BlockMixin {

    // ================================================================
    // Helper: get FC block for this Block instance
    // ================================================================

    private btw.modern.Block btw$getFcBlock() {
        if ((Object) this instanceof ProxyBlock) {
            return null;
        }
        return ProxyRegistry.getFcBlock((Block) (Object) this);
    }

    // NOTE: playerDestroy is NOT intercepted here. FC's harvest pipeline runs
    // from ServerPlayerGameModeMixin.destroyBlock() at HEAD — BEFORE vanilla
    // removes the block. This is critical because FC's tryHarvestBlock queries
    // the world for the block, and it must still be there.

    // ================================================================
    // stepOn -> FC onEntityWalking
    // ================================================================

    @Inject(method = "stepOn", at = @At("HEAD"), cancellable = true)
    private void btw$stepOn(Level level, BlockPos pos, BlockState state, Entity entity,
                             CallbackInfo ci) {
        btw.modern.Block fcBlock = btw$getFcBlock();
        if (fcBlock == null) return;

        if (level instanceof ServerLevel sl) {
            btw.modern.World world = WorldBridge.getOrCreate(sl);
            // Entity wrapping not yet implemented — pass null
            fcBlock.onEntityWalking(world, pos.getX(), pos.getY(), pos.getZ(), null);
            ci.cancel();
        }
    }

    // ================================================================
    // fallOn -> FC onFallenUpon
    // ================================================================

    @Inject(method = "fallOn", at = @At("HEAD"))
    private void btw$fallOn(Level level, BlockState state, BlockPos pos, Entity entity,
                             float fallDistance, CallbackInfo ci) {
        btw.modern.Block fcBlock = btw$getFcBlock();
        if (fcBlock == null) return;

        if (level instanceof ServerLevel sl) {
            btw.modern.World world = WorldBridge.getOrCreate(sl);
            fcBlock.onFallenUpon(world, pos.getX(), pos.getY(), pos.getZ(), null, fallDistance);
            // Do NOT cancel — vanilla fallOn must still call entity.causeFallDamage().
            // FC's onFallenUpon is a notification hook, not a damage replacement.
        }
    }

    // ================================================================
    // getExplosionResistance -> FC getExplosionResistance
    // ================================================================

    @Inject(method = "getExplosionResistance", at = @At("HEAD"), cancellable = true)
    private void btw$getExplosionResistance(CallbackInfoReturnable<Float> cir) {
        btw.modern.Block fcBlock = btw$getFcBlock();
        if (fcBlock == null) return;

        cir.setReturnValue(fcBlock.getExplosionResistance(null));
    }

    // ================================================================
    // getSpeedFactor -> FC GetMovementModifier
    // ================================================================

    @Inject(method = "getSpeedFactor", at = @At("HEAD"), cancellable = true)
    private void btw$getSpeedFactor(CallbackInfoReturnable<Float> cir) {
        btw.modern.Block fcBlock = btw$getFcBlock();
        if (fcBlock == null) return;

        // Bridge to FC's GetMovementModifier — pass through whatever FC returns.
        try {
            float modifier = fcBlock.GetMovementModifier(null, 0, 0, 0);
            if (modifier > 0) {
                cir.setReturnValue(modifier);
            }
        } catch (Exception ignored) {}
    }

    // ================================================================
    // getFriction -> FC slipperiness
    // ================================================================

    @Inject(method = "getFriction", at = @At("HEAD"), cancellable = true)
    private void btw$getFriction(CallbackInfoReturnable<Float> cir) {
        btw.modern.Block fcBlock = btw$getFcBlock();
        if (fcBlock == null) return;

        float slip = fcBlock.slipperiness;
        if (slip != 0.6F && slip > 0) {
            cir.setReturnValue(slip);
        }
    }

}
