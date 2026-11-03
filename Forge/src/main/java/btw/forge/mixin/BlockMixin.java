package btw.forge.mixin;

import btw.forge.HookPolicy;
import btw.forge.ProxyBlock;
import btw.forge.ProxyRegistry;
import btw.forge.WorldBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
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
 *
 * FCMOD-INTEGRATION(1.20.1): hook_ownership. stepOn and fallOn are NOTIFY executors of
 * {@link btw.forge.HookPolicy} (Block.stepOn / Block.fallOn are called on the Block,
 * Entity.java:676, :1128, so there is no BlockStateBase forwarder): each runs FC only when the
 * cached owner of its state allows it (farmland FALL is ADAPTER(D9b): one trample roll on
 * 1.20.1's path, with FC's threshold, FarmBlockMixin). Every FC call is caught and logged once
 * per throw site; a throw answers the 1.20.1 result. getExplosionResistance is FC's position-less
 * value (HookPolicy RESIST NOTIFY); explosions read FC's positional value where FC's class has one
 * (ExplosionDamageCalculatorMixin, fc_blast_rules).
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

    /**
     * Wraps a vanilla MC entity as a btw.modern.Entity for FC block callbacks (onEntityWalking / onFallenUpon): the one
     * contact wrapper, {@link btw.forge.VanillaEntityViews#forContact} (FCMOD-INTEGRATION(1.20.1): fc_entity_views),
     * shared with ProxyBlock and BlockBehaviorMixin. Null when the entity has no FC object to show (an item without a
     * view, a puppet without its FC entity) -- the caller skips the FC callback.
     */
    private btw.modern.Entity btw$wrapEntity(Entity entity) {
        return btw.forge.VanillaEntityViews.forContact(entity);
    }

    // NOTE: playerDestroy is NOT intercepted here. FC's harvest pipeline runs
    // from ServerPlayerGameModeMixin.destroyBlock() at HEAD — BEFORE vanilla
    // removes the block. This is critical because FC's tryHarvestBlock queries
    // the world for the block, and it must still be there. The beds and doors of
    // vanilla_pair_drops take 1.20.1's playerDestroy instead; BlockPlayerDestroyMixin
    // charges FC's harvest exhaustion there.

    // ================================================================
    // stepOn -> FC onEntityWalking
    // ================================================================

    @Inject(method = "stepOn", at = @At("HEAD"), cancellable = true)
    private void btw$stepOn(Level level, BlockPos pos, BlockState state, Entity entity,
                             CallbackInfo ci) {
        // FCMOD-INTEGRATION(1.20.1): hook_ownership: owner-gated and caught; a throw keeps the 1.20.1 body
        if (!HookPolicy.notifyRuns(state, HookPolicy.STEP)) return;
        btw.modern.Block fcBlock = btw$getFcBlock();
        if (fcBlock == null) return;

        if (level instanceof ServerLevel sl) {
            // MC calls stepOn for every grounded entity; 1.5.2 Entity.moveEntity gates onEntityWalking on
            // canTriggerWalking() (vanilla/server Entity.java:841), false for EntityItem (EntityItem.java:59).
            // Checked before wrapping so every item resting on grass/dirt/stone is not re-synced per move;
            // the vanilla stepOn stays replaced by FC's, as for any other entity.
            if (entity instanceof ItemEntity) {
                ci.cancel();
                return;
            }
            try {
                btw.modern.World world = WorldBridge.getOrCreate(sl);
                btw.modern.Entity fcEntity = btw$wrapEntity(entity);
                if (fcEntity != null) {
                    fcBlock.onEntityWalking(world, pos.getX(), pos.getY(), pos.getZ(), fcEntity);
                }
                ci.cancel();
            } catch (Throwable t) {
                HookPolicy.warnOnce("STEP", fcBlock, pos, t);
            }
        }
    }

    // ================================================================
    // fallOn -> FC onFallenUpon
    // ================================================================

    @Inject(method = "fallOn", at = @At("HEAD"))
    private void btw$fallOn(Level level, BlockState state, BlockPos pos, Entity entity,
                             float fallDistance, CallbackInfo ci) {
        // FCMOD-INTEGRATION(1.20.1): hook_ownership: owner-gated and caught. Farmland's FALL is ADAPTER(D9b): FarmBlock.fallOn
        // (FarmBlock.java:86-91) already rolled the trample on 1.20.1's path with FC's threshold (FarmBlockMixin), so FC's
        // second roll (Server FCBlockFarmlandBase.java:80-89) is skipped here
        if (!HookPolicy.notifyRuns(state, HookPolicy.FALL)) return;
        btw.modern.Block fcBlock = btw$getFcBlock();
        if (fcBlock == null) return;

        if (level instanceof ServerLevel sl) {
            try {
                btw.modern.World world = WorldBridge.getOrCreate(sl);
                btw.modern.Entity fcEntity = btw$wrapEntity(entity);
                if (fcEntity != null) {
                    fcBlock.onFallenUpon(world, pos.getX(), pos.getY(), pos.getZ(), fcEntity, fallDistance);
                }
            } catch (Throwable t) {
                HookPolicy.warnOnce("FALL", fcBlock, pos, t);
            }
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

        // FCMOD-INTEGRATION(1.20.1): hook_ownership: caught; the position-less value (explosion rays read the positional one where HookPolicy RESIST is FC, ExplosionDamageCalculatorMixin); a throw answers 1.20.1's resistance
        try {
            cir.setReturnValue(fcBlock.getExplosionResistance(null));
        } catch (Throwable t) {
            HookPolicy.warnOnce("RESIST", fcBlock, null, t);
        }
    }

    // ================================================================
    // getSpeedFactor -> FC GetMovementModifier
    // ================================================================

    // NO getSpeedFactor hook here — DELIBERATELY removed. FC's GetMovementModifier (the +20%
    // hard-surface bonus) must NOT be applied through getSpeedFactor: MC 1.20.1 multiplies
    // getSpeedFactor into carried deltaMovement every tick in Entity.move(), so a factor > 1.0
    // compounds into momentum and, while airborne (sprint-jumping), the retention product
    // 0.91 × 1.2 = 1.092 > 1 makes speed grow unbounded ("practically flying"). FC applied it
    // to the INPUT speed (moveEntityWithHeading), so the bridge now does the same in
    // PlayerMixin.btw$applyBlockMovementModifier (Player.getSpeed, via FCMovementBonus). Slowdown
    // blocks (soul sand, honey) keep working via their own vanilla getSpeedFactor + getFriction.

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
