package btw.forge.mixin;

import btw.forge.PlayerBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Disables vanilla food system and redirects to FC's {@link btw.modern.FoodStats}.
 *
 * FC completely replaces the hunger system:
 * - Food level range: 0-60 (3x vanilla's 0-20)
 * - Different exhaustion rates (1.33F/pip vs 4.0F)
 * - Fat-before-hunger burning logic
 * - Healing every 600 ticks (not 80) when food > 24
 * - Starvation only when food ≤ 0 AND saturation ≤ 0.01
 */
@Mixin(FoodData.class)
public abstract class FoodDataMixin {

    @Shadow private int foodLevel;
    @Shadow private float saturationLevel;

    /**
     * Cancel vanilla food tick entirely. FC food system runs from
     * ServerPlayerMixin.btw$tick() via PlayerBridge.
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void btw$tick(Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer sp) {
            PlayerBridge pb = PlayerBridge.getOrCreate(sp);

            // Sync real state into FC layer
            pb.syncFromReal();

            // Run FC food tick
            pb.foodStats.onUpdate(pb);

            // Sync FC food level back to vanilla FoodData for client display
            // Client sees foodLevel/20 pips, FC uses foodLevel/60.
            // We set vanilla food level = FC level / 3 so the HUD shows correctly.
            this.foodLevel = pb.foodStats.getFoodLevel() / 3;
            this.saturationLevel = pb.foodStats.getSaturationLevel();

            // Sync any health changes back
            pb.syncToReal();

            ci.cancel();
        }
    }

    /**
     * Redirect food eating to FC food system.
     */
    @Inject(method = "eat(IF)V", at = @At("HEAD"), cancellable = true)
    private void btw$eat(int foodLevel, float saturationModifier, CallbackInfo ci) {
        // We can't easily get the Player from FoodData here.
        // The FC food system handles eating through block/item activation
        // which goes through the btw.modern layer directly.
        // For safety, cancel the vanilla eat and let FC handle it.
        ci.cancel();
    }
}
