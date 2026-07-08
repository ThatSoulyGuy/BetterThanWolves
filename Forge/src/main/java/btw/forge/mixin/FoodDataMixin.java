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
     * Vanilla items with FC counterparts already get their nutrition from FC's
     * ItemFood.onEaten (ItemStackMixin.finishUsingItem → FoodStats.addStats with
     * FC's tuned heal values). Vanilla's FoodProperties path then calls
     * eat(Item, ItemStack) → eat(int, float), which applied the nutrition a
     * SECOND time (6x instead of 3x). Cancel this overload when an FC
     * counterpart exists — the item lookup mirrors ItemStackMixin.btw$getFcItem
     * exactly, so precisely one of the two paths applies. Modern-only foods
     * (no FC counterpart, Pattern-E) fall through to the eat(IF) conversion.
     */
    @Inject(method = "eat(Lnet/minecraft/world/item/Item;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"), cancellable = true)
    private void btw$eatItem(net.minecraft.world.item.Item item,
                             net.minecraft.world.item.ItemStack stack, CallbackInfo ci) {
        int legacyId;
        if (item instanceof net.minecraft.world.item.BlockItem bi) {
            legacyId = btw.forge.ProxyRegistry.getBlockId(bi.getBlock());
        } else {
            legacyId = btw.forge.ProxyRegistry.getItemId(item);
        }
        if (legacyId > 0 && legacyId < btw.modern.Item.itemsList.length
                && btw.modern.Item.itemsList[legacyId] != null) {
            ci.cancel();
        }
    }

    /**
     * Redirect vanilla food eating to FC food system.
     * Vanilla items call FoodData.eat(nutrition, saturation) — we convert
     * to FC's 3x scale and forward to the FC FoodStats.
     */
    @Inject(method = "eat(IF)V", at = @At("HEAD"), cancellable = true)
    private void btw$eat(int nutrition, float saturationModifier, CallbackInfo ci) {
        // Find the owning player via Minecraft's internals
        // FoodData doesn't store a player reference, but the mixin can
        // intercept and route through PlayerBridge
        try {
            // Find the player that owns this FoodData by checking all server players.
            net.minecraft.server.MinecraftServer server =
                    net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
                    if (sp.getFoodData() == (Object) this) {
                        PlayerBridge pb = PlayerBridge.getOrCreate(sp);
                        // Convert vanilla nutrition to FC's 3x scale
                        int fcNutrition = nutrition * 3;
                        pb.foodStats.addStats(fcNutrition, saturationModifier);
                        // Sync back to vanilla
                        this.foodLevel = pb.foodStats.getFoodLevel() / 3;
                        this.saturationLevel = pb.foodStats.getSaturationLevel();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            // Fall through silently if we can't find the player
        }
        ci.cancel();
    }
}
