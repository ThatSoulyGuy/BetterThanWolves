package btw.forge.mixin;

import btw.forge.PlayerBridge;
import btw.forge.ProxyRegistry;
import btw.forge.WorldBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Intercepts {@link LivingEntity} movement calculations to apply FC overrides:
 * <ul>
 *   <li>Block speed factor (movement modifiers)</li>
 *   <li>Movement penalty modifier</li>
 *   <li>Jump gating (too hungry/weak to jump)</li>
 *   <li>Jump exhaustion (custom FC values)</li>
 *   <li>Ladder gating (extreme health penalty)</li>
 *   <li>Swim gating (#12): prevent swimming up when CanSwim() is false</li>
 *   <li>Air supply decrease hook for soulforged helm override</li>
 *   <li>Gradual air recovery (FC replaces vanilla instant recovery)</li>
 * </ul>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow protected boolean jumping;

    // NO getBlockSpeedFactor hook here — DELIBERATELY removed. Vanilla
    // Entity.getBlockSpeedFactor already calls Block.getSpeedFactor() on the feet/below
    // blocks, which BlockMixin (vanilla-with-FC-counterpart) and ProxyBlock (FC blocks)
    // already multiply by FC's GetMovementModifier. Applying GetMovementModifier AGAIN at
    // the LivingEntity level double-counted it — ×1.44 on hard surfaces (the "moving super
    // fast" bug). The block-level hook is the single, correct application point; the
    // old "safety net" here was wrong (vanilla does NOT bypass Block.getSpeedFactor).

    // Suppress vanilla FoodProperties effect application for any food with an FC counterpart:
    // FC's ItemFood.onFoodEaten already applies the FC effect via ItemStackMixin.finishUsingItem,
    // so without this the modern effect (rotten flesh hunger, pufferfish poison, spider eye
    // poison, ...) would apply a SECOND time. Pattern-E foods (no FC counterpart) fall through.
    @Inject(method = "addEatEffect", at = @At("HEAD"), cancellable = true)
    private void btw$addEatEffect(net.minecraft.world.item.ItemStack stack,
                                  net.minecraft.world.level.Level level,
                                  LivingEntity entity, CallbackInfo ci) {
        int legacyId;
        if (stack.getItem() instanceof net.minecraft.world.item.BlockItem bi) {
            legacyId = ProxyRegistry.getBlockId(bi.getBlock());
        } else {
            legacyId = ProxyRegistry.getItemId(stack.getItem());
        }
        if (legacyId > 0 && legacyId < btw.modern.Item.itemsList.length
                && btw.modern.Item.itemsList[legacyId] != null) {
            ci.cancel();
        }
    }

    // ================================================================
    // FC movement penalty modifier — applies to ALL movement
    // ================================================================

    @Inject(method = "getSpeed", at = @At("RETURN"), cancellable = true)
    private void btw$applyMovementPenalty(CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof ServerPlayer sp) {
            PlayerBridge pb = PlayerBridge.getOrCreate(sp);
            float modifier = pb.GetLandMovementModifier();
            if (modifier < 1.0F) {
                cir.setReturnValue(cir.getReturnValue() * modifier);
            }
        }
    }

    // ================================================================
    // FC jump gating — cannot jump when too hungry/weak
    // ================================================================

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void btw$gateJump(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof ServerPlayer sp) {
            PlayerBridge pb = PlayerBridge.getOrCreate(sp);
            pb.syncFromReal();
            if (!pb.CanJump()) {
                ci.cancel();
            }
        }
    }

    // ================================================================
    // FC jump exhaustion — custom values (0.2F normal, 1.0F sprinting)
    // ================================================================

    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    private void btw$jumpExhaustion(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof ServerPlayer sp) {
            PlayerBridge pb = PlayerBridge.getOrCreate(sp);
            pb.AddExhaustionForJump();
        }
    }

    // ================================================================
    // FC ladder gating — cannot climb at extreme health penalty
    // ================================================================

    @Inject(method = "onClimbable", at = @At("HEAD"), cancellable = true)
    private void btw$gateLadder(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof ServerPlayer sp) {
            PlayerBridge pb = PlayerBridge.getOrCreate(sp);
            pb.syncFromReal();
            if (pb.GetHealthPenaltyLevel() >= 4) {
                cir.setReturnValue(false);
            }
        }
    }

    // ================================================================
    // FC swim gating (#12) — prevent swimming up when CanSwim() is false
    // ================================================================

    /**
     * Prevents the player from swimming upward when {@code CanSwim()} returns false.
     * In MC 1.20.1, swimming upward happens in {@code aiStep()} when the player
     * presses jump while in water (the {@code jumping} field is true and the entity
     * is in water). When FC says the player cannot swim (too heavy from armor or
     * health too low), we suppress the upward motion by zeroing the Y component
     * of the entity's delta movement when the player is trying to jump in water.
     *
     * @see btw.modern.EntityPlayer#CanSwim()
     */
    @Inject(method = "aiStep", at = @At("HEAD"))
    private void btw$gateSwimming(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof ServerPlayer sp) {
            // Only act when the player is in water and trying to jump (swim up)
            if (sp.isInWater() && this.jumping) {
                PlayerBridge pb = PlayerBridge.getOrCreate(sp);
                pb.syncFromReal();
                if (!pb.CanSwim()) {
                    // Suppress upward motion — the player sinks instead of swimming up.
                    // We zero out any positive Y velocity so they cannot ascend in water.
                    Vec3 motion = sp.getDeltaMovement();
                    if (motion.y > 0) {
                        sp.setDeltaMovement(motion.x, 0.0, motion.z);
                    }
                } else {
                    // FC extra swimming exhaustion: when the player CAN swim and is
                    // actively moving upward in water, add 0.025F exhaustion per tick.
                    // This makes swimming up more costly than horizontal swimming.
                    Vec3 motion = sp.getDeltaMovement();
                    if (motion.y > 0) {
                        pb.AddExhaustionWithoutVisualFeedback(0.025F);
                    }
                }
            }
        }
    }

    // ================================================================
    // FC air supply decrease — soulforged helm hook
    // ================================================================

    /**
     * Hook: route player air-supply decrease to FC. The behavior (soulforged-helm
     * Respiration bonus, etc.) lives in {@link btw.modern.EntityPlayer#decreaseAirSupply(int)};
     * this mixin only forwards the call and returns FC's result.
     */
    @Inject(method = "decreaseAirSupply", at = @At("HEAD"), cancellable = true)
    private void btw$decreaseAirSupply(int currentAir, CallbackInfoReturnable<Integer> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayer sp)) return;

        PlayerBridge pb = PlayerBridge.getOrCreate(sp);
        pb.syncFromReal();
        cir.setReturnValue(pb.decreaseAirSupply(currentAir));
    }

    // ================================================================
    // FC gradual air recovery — replaces vanilla instant recovery
    // ================================================================

    /**
     * Tracks whether this entity was submerged in the previous tick,
     * so we can detect the moment the player surfaces.
     */
    @Unique
    private boolean btw$wasInWaterLastTick = false;

    /**
     * FC replaces vanilla's instant air recovery with a gradual system:
     * when the player surfaces, a 20-tick countdown starts. After the
     * countdown expires, air recovers at 10 units per tick until full.
     * This is tracked via {@code PlayerBridge.m_iAirRecoveryCountdown}.
     *
     * <p>Injected at the tail of {@code baseTick()} so it runs after
     * vanilla's own air supply logic. When the player is out of water
     * and air is below max, vanilla would set air to {@code air + 1}
     * per tick. We override that by clamping air to the FC-calculated
     * value instead.</p>
     */
    @Inject(method = "baseTick", at = @At("TAIL"))
    private void btw$gradualAirRecovery(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayer sp)) {
            btw$wasInWaterLastTick = self.isInWater();
            return;
        }

        PlayerBridge pb = PlayerBridge.getOrCreate(sp);
        int maxAir = sp.getMaxAirSupply(); // typically 300
        int currentAir = sp.getAirSupply();
        boolean inWaterNow = sp.isInWater();

        if (inWaterNow) {
            // Player is submerged — reset recovery state
            btw$wasInWaterLastTick = true;
            // Reset countdown so it starts fresh when they surface
            pb.m_iAirRecoveryCountdown = 20;
            return;
        }

        // Player is NOT in water
        if (btw$wasInWaterLastTick && !inWaterNow) {
            // Just surfaced — start the 20-tick delay
            pb.m_iAirRecoveryCountdown = 20;
        }
        btw$wasInWaterLastTick = false;

        if (currentAir >= maxAir) {
            // Air is full, nothing to do
            pb.m_iAirRecoveryCountdown = 0;
            return;
        }

        // Air is below max and player is out of water — apply FC's
        // gradual recovery instead of vanilla's per-tick +1.
        if (pb.m_iAirRecoveryCountdown > 0) {
            // Still in the delay window — undo vanilla's +1 recovery
            // by clamping air back down. Vanilla incremented it by 1
            // in baseTick(); we reverse that.
            pb.m_iAirRecoveryCountdown--;
            // Prevent air from recovering during the countdown
            // (vanilla may have already added some this tick)
            int preVanillaAir = currentAir - 1;
            if (preVanillaAir < 0) preVanillaAir = 0;
            sp.setAirSupply(preVanillaAir);
        } else {
            // Countdown expired — recover 10 units per tick (FC rate)
            // Vanilla already added 1, so we add 9 more to reach 10 total
            int newAir = currentAir + 9;
            if (newAir > maxAir) newAir = maxAir;
            sp.setAirSupply(newAir);
        }
    }

    // ================================================================
    // DEBUG: Instrument causeFallDamage to diagnose missing fall damage
    // ================================================================

    @Inject(method = "causeFallDamage", at = @At("HEAD"))
    private void btw$debugFallDamage(float fallDistance, float multiplier,
            net.minecraft.world.damagesource.DamageSource source,
            CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof net.minecraft.server.level.ServerPlayer sp) {
            org.apache.logging.log4j.LogManager.getLogger("BTW-Fall").info(
                    "LivingEntity.causeFallDamage: dist={} mult={} health={} mayfly={}",
                    fallDistance, multiplier, self.getHealth(), sp.getAbilities().mayfly);
        }
    }

    @Inject(method = "checkFallDamage", at = @At("HEAD"))
    private void btw$debugCheckFall(double y, boolean onGround,
            BlockState state, BlockPos pos, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof net.minecraft.server.level.ServerPlayer && self.fallDistance > 3.0F && onGround) {
            org.apache.logging.log4j.LogManager.getLogger("BTW-Fall").info(
                    "checkFallDamage: onGround={} fallDist={} y={} block={}",
                    onGround, self.fallDistance, y, state.getBlock());
        }
    }
}
