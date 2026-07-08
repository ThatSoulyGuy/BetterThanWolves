package btw.forge.mixin;

import btw.forge.ProxyRegistry;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * FCMOD-INTEGRATION(1.20.1): totem_of_undying
 *
 * <p><b>Feature:</b> Totem of Undying (MC 1.11+). Held in the main-hand
 * or off-hand, the totem activates on lethal damage to restore the
 * holder to 1 HP plus Regen + Fire Resist + Absorption, consuming the
 * totem.</p>
 *
 * <p><b>Conflict with FC:</b> FC's HC Spawn mechanic (integration
 * {@code #002} Gamerule defaults) makes death load-bearing for the
 * early/mid progression curve. The totem is a free death-save; it
 * short-circuits the penalty entirely. Evokers drop totems at 100%
 * and can be farmed, which turns late-game FC progression into
 * "accumulate totems, become immortal".</p>
 *
 * <p><b>Integration pattern:</b> B (gate behind FC reagent — preserve
 * the feature but attach an FC-side cost that routes through FC's
 * alchemy chain).</p>
 *
 * <p><b>Rebalance:</b> Totem activation requires at least one
 * {@code fcItemSoulFlux} in the player's inventory and consumes it on
 * activation. Per {@code docs/audit/alchemy.md} §Totem of Undying,
 * soul flux is FC's rarest alchemy output — requiring it per save
 * makes totem use an endgame commitment rather than a routine escape
 * hatch. Totem drop rates from evokers are untouched (Protocol #5,
 * dual-source preserved).</p>
 *
 * <p><b>Alternative considered and rejected:</b> Remove totem from
 * evoker drops. Rejected under Protocol #5 — loot-source removal
 * breaks other mods that may rely on the drop, and is a heavier
 * change than an activation cost.</p>
 *
 * <p><b>Injection strategy:</b> {@link Inject} at {@code HEAD} of
 * {@code LivingEntity#checkTotemDeathProtection(DamageSource)} with
 * {@code cancellable = true}. The injection:
 * <ol>
 *   <li>Returns immediately (vanilla flow) if the entity is not a
 *       player — FC's reagent gate is a player-only design decision.
 *       Non-player totem use (e.g., modded entities with totems) is
 *       not in FC's scope.</li>
 *   <li>Returns immediately if the damage source bypasses invulnerability
 *       — vanilla would return {@code false} anyway, and we don't want
 *       to consume soul flux for a save that can't fire.</li>
 *   <li>Checks both hands for a totem; if neither hand has one, returns
 *       immediately so vanilla returns false without soul flux
 *       consumption.</li>
 *   <li>Checks inventory for {@code fcItemSoulFlux} (item ID 2555, the
 *       runtime index of FC's soul flux item — see
 *       {@code FCBetterThanWolves.java:1652}). If absent, cancels with
 *       {@code false} — totem is kept but doesn't save.</li>
 *   <li>If soul flux present, consumes one and falls through to vanilla,
 *       which then consumes the totem and applies the save effects.</li>
 * </ol></p>
 */
@Mixin(LivingEntity.class)
public abstract class TotemOfUndyingMixin {

    /**
     * Runtime itemsList index for {@code fcItemSoulFlux} (ParseID 2299 + 256
     * offset). Matches the convention used by {@code FCGameplayBridge} for
     * other FC-item-ID constants (arcane scroll, etc.).
     */
    private static final int FC_SOUL_FLUX_ITEM_ID = 2555;

    @Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true)
    private void btw$requireSoulFlux(DamageSource source,
                                     CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player player)) return;

        // Vanilla short-circuits this case to false; don't spend soul flux.
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;

        // No totem in either hand? Vanilla will return false — let it.
        boolean hasTotem = false;
        for (InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).is(Items.TOTEM_OF_UNDYING)) {
                hasTotem = true;
                break;
            }
        }
        if (!hasTotem) return;

        // Totem present. Look up soul flux in the player inventory.
        Item soulFluxItem = ProxyRegistry.getModernItem(FC_SOUL_FLUX_ITEM_ID);
        if (soulFluxItem == null) {
            // Soul flux not registered (pre-init / misconfigured): fall
            // through to vanilla rather than crash. This is defensive —
            // in normal operation the item is always registered by the
            // time an entity can take damage.
            return;
        }

        int soulFluxSlot = -1;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(soulFluxItem)) {
                soulFluxSlot = i;
                break;
            }
        }

        if (soulFluxSlot < 0) {
            // Totem but no soul flux: save fails.
            cir.setReturnValue(false);
            return;
        }

        // Both present: consume one soul flux, let vanilla consume the
        // totem and apply the save effects.
        player.getInventory().removeItem(soulFluxSlot, 1);
    }
}
