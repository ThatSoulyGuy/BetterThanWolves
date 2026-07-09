package btw.forge.mixin;

import btw.forge.BTWNetwork;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client-side counterpart to {@code LivingEntityMixin.btw$applyMovementPenalty}.
 *
 * <p>The local player's movement is client-authoritative: {@link LocalPlayer} computes
 * its own {@code getSpeed()} and moves itself, then reports the result to the server.
 * The server applies the FC movement penalty (see the ServerPlayer branch in
 * {@code LivingEntityMixin}), but if the client keeps predicting vanilla speed the
 * server only <em>intermittently</em> pulls it back — airborne/jump movement has looser
 * anti-move tolerance — so the debuff appears to "vanish" precisely while running and
 * jumping. Applying the SAME penalty to the local player here keeps client prediction in
 * agreement with the server, so the debuff is felt consistently.</p>
 *
 * <p>This mixin is registered in the {@code "client"} array of the mixin config, so
 * {@link LocalPlayer} is never referenced on a dedicated server. The penalty levels come
 * from the server via {@link BTWNetwork}'s PenaltySync packet (sent every tick), and the
 * modifier is computed by the single shared formula in
 * {@link btw.modern.EntityPlayer#computeHealthAndExhaustionModifier(int, int)} so the
 * client value can never drift from the server's.</p>
 */
@Mixin(LivingEntity.class)
public class ClientPlayerSpeedMixin {

    @Inject(method = "getSpeed", at = @At("RETURN"), cancellable = true)
    private void btw$applyClientMovement(CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof LocalPlayer)) {
            return;
        }

        float original = cir.getReturnValue();
        float value = original;

        // FC health/hunger/fat/gloom penalty (always <= 1.0), from the server-synced levels.
        int maxPenalty = Math.max(BTWNetwork.clientHealthPenalty,
                Math.max(BTWNetwork.clientHungerPenalty, BTWNetwork.clientFatPenalty));
        float penalty = btw.modern.EntityPlayer.computeHealthAndExhaustionModifier(
                maxPenalty, BTWNetwork.clientGloomLevel);
        if (penalty < 1.0F) {
            value *= penalty;
        }

        // FC hard-surface +20% bonus, applied to the INPUT speed — identical to the server
        // (LivingEntityMixin) so client-authoritative prediction agrees and doesn't rubber-band.
        // Applied via getSpeed (bounded) rather than getSpeedFactor (which compounded into
        // airborne momentum → the sprint-jump "flying" bug).
        value *= btw.forge.FCMovementBonus.getBlockBelowSpeedBonus(self);

        if (value != original) {
            cir.setReturnValue(value);
        }
    }
}
