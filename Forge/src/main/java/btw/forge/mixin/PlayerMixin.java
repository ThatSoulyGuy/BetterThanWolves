package btw.forge.mixin;

import btw.forge.PlayerBridge;
import btw.forge.ProxyRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Intercepts {@link Player} methods to apply FC overrides:
 * <ul>
 *   <li>Dig speed / tool effectiveness (mining)</li>
 *   <li>Bed sleeping disabled</li>
 *   <li>Mid-air block placement restriction</li>
 *   <li>Eating restriction (hunger potion)</li>
 *   <li>Armor exhaustion modifier</li>
 *   <li>Blasting oil detonation on damage (combat #18)</li>
 *   <li>Melee damage modifier from penalties (combat #20)</li>
 * </ul>
 */
@Mixin(Player.class)
public abstract class PlayerMixin {

    // NOTE: getDestroySpeed is NOT intercepted here.
    // FC's complete mining speed calculation runs through
    // fcBlock.getPlayerRelativeBlockHardness() called from BlockBehaviorMixin,
    // which internally calls player.getCurrentPlayerStrVsBlock() →
    // inventory.getStrVsBlock() → item.getStrVsBlock() — all FC code.

    // ================================================================
    // FC mining speed modifier (penalty-based slowdown)
    // ================================================================

    // ================================================================
    // FC bed sleeping disabled
    // ================================================================

    @Inject(method = "startSleepInBed", at = @At("HEAD"), cancellable = true)
    private void btw$disableBeds(BlockPos pos,
                                  CallbackInfoReturnable<Player.BedSleepingProblem> cir) {
        // FC completely disables bed sleeping — always returns OTHER_PROBLEM
        cir.setReturnValue(Player.BedSleepingProblem.OTHER_PROBLEM);
    }

    // ================================================================
    // FC mid-air block placement restriction
    // ================================================================

    @Inject(method = "mayUseItemAt", at = @At("HEAD"), cancellable = true)
    private void btw$restrictMidAirPlacement(BlockPos pos, net.minecraft.core.Direction facing,
                                              net.minecraft.world.item.ItemStack stack,
                                              CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) this;
        if (self instanceof ServerPlayer sp) {
            // Creative mode bypasses
            if (sp.getAbilities().instabuild) return;

            // FC requires: onGround OR inWater OR on ladder OR riding OR in lava
            boolean canPlace = sp.onGround()
                    || sp.isInWater()
                    || sp.onClimbable()
                    || sp.isPassenger()
                    || sp.isInLava();

            if (!canPlace) {
                cir.setReturnValue(false);
            }
        }
    }

    // ================================================================
    // FC eating restriction — block when Hunger potion active
    // ================================================================

    @Inject(method = "canEat", at = @At("HEAD"), cancellable = true)
    private void btw$restrictEating(boolean canAlwaysEat, CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) this;
        if (self.hasEffect(net.minecraft.world.effect.MobEffects.HUNGER)) {
            cir.setReturnValue(false);
        }
    }

    // ================================================================
    // FC armor exhaustion modifier
    // ================================================================

    @Inject(method = "causeFoodExhaustion", at = @At("HEAD"), cancellable = true)
    private void btw$armorExhaustion(float amount, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (self instanceof ServerPlayer sp) {
            PlayerBridge pb = PlayerBridge.getOrCreate(sp);
            // Apply FC armor weight exhaustion modifier
            float modified = amount * pb.GetArmorExhaustionModifier();
            // Route to FC food stats instead of vanilla
            pb.foodStats.addExhaustion(modified);
            ci.cancel();
        }
    }

    // ================================================================
    // DEBUG: catch fall damage at Player level
    // ================================================================

    @Inject(method = "causeFallDamage", at = @At("HEAD"))
    private void btw$debugPlayerFall(float fallDistance, float multiplier,
            net.minecraft.world.damagesource.DamageSource source,
            CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) this;
        org.apache.logging.log4j.LogManager.getLogger("BTW-Fall").info(
                "Player.causeFallDamage: dist={} mayfly={} creative={}",
                fallDistance, self.getAbilities().mayfly, self.getAbilities().instabuild);
    }

    // ================================================================
    // FC combat: blasting oil detonation on damage (#18)
    // ================================================================

    /**
     * After a player takes damage, check if they are carrying blasting oil.
     * If so, detonate it. Only applies to ServerPlayer (server-side).
     *
     * @see btw.modern.EntityPlayer#IsCarryingBlastingOil()
     * @see btw.modern.EntityPlayer#DetonateCarriedBlastingOil()
     */
    @Inject(method = "hurt", at = @At("RETURN"))
    private void btw$checkBlastingOilOnDamage(DamageSource source, float amount,
                                               CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) this;
        if (self instanceof ServerPlayer sp) {
            // Only proceed if damage was actually applied (return value true)
            if (cir.getReturnValue()) {
                PlayerBridge pb = PlayerBridge.getOrCreate(sp);
                pb.syncFromReal();
                if (pb.IsCarryingBlastingOil()) {
                    pb.DetonateCarriedBlastingOil();
                }
            }
        }
    }

    // ================================================================
    // FC combat: melee damage modifier (#20)
    // ================================================================

    /**
     * Before a player attacks an entity, store the current melee damage
     * modifier on the PlayerBridge so FC code can reference it.
     * <p>
     * In vanilla, {@code Player.attack(Entity)} calculates damage internally
     * using attribute values. FC applies a penalty-based modifier to melee
     * damage via {@code GetMeleeDamageModifier()}. Since we cannot easily
     * redirect the internal damage calculation, we sync the modifier to
     * the bridge before the attack method runs, and apply it in a
     * secondary injection at the damage application point.
     * <p>
     * The modifier is stored as {@link PlayerBridge#pendingMeleeDamageModifier}
     * so it can be applied by downstream logic.
     */
    @Inject(method = "attack", at = @At("HEAD"))
    private void btw$storeMeleeDamageModifier(Entity target, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (self instanceof ServerPlayer sp) {
            PlayerBridge pb = PlayerBridge.getOrCreate(sp);
            pb.syncFromReal();
            pb.pendingMeleeDamageModifier = pb.GetMeleeDamageModifier();
        }
    }

    /**
     * Applies the FC melee damage penalty to the BASE attack damage — the first float local
     * in Player.attack, `getAttributeValue(ATTACK_DAMAGE)`, before enchantment damage is
     * added. Mirrors 1.5.2 EntityPlayer.attackTargetEntityWithCurrentItem:1226-1230
     * (`if (fModifier < 0.99F) var2 = (int)(var2 * fModifier)` applied to base, pre-enchant).
     * Modifier comes from btw$storeMeleeDamageModifier (@HEAD, already synced). This is the
     * consumer the stored value never had — FC weight/health/exhaustion now actually weakens
     * hits. Server-only (client attack() also runs but the server value is authoritative).
     */
    @ModifyVariable(method = "attack", at = @At("STORE"), ordinal = 0)
    private float btw$applyMeleeDamageModifier(float baseDamage) {
        Player self = (Player) (Object) this;
        if (self instanceof ServerPlayer sp) {
            float modifier = PlayerBridge.getOrCreate(sp).pendingMeleeDamageModifier;
            if (modifier < 0.99F) {
                return baseDamage * modifier;
            }
        }
        return baseDamage;
    }
}
