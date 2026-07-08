package btw.forge.mixin;

import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.CrossbowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * FCMOD-INTEGRATION(1.20.1): crossbow_piercing
 *
 * <p><b>Feature:</b> Crossbow {@code PIERCING} enchantment (MC 1.14+).
 * Each level allows a fired projectile to penetrate one additional
 * entity before losing momentum. Crossbow rolls {@code PIERCING IV}
 * at treasure enchant drops, giving 4-entity penetration.</p>
 *
 * <p><b>Conflict with FC:</b> FC's ranged combat uses
 * {@code FCItemArrowBroadhead} as its designed armor-bypass mechanism.
 * Piercing III+ effectively fires through walls of mobs, turning FC's
 * spider swarms and zombie hordes (which FC tunes to threaten the
 * player through sheer number) into a one-shot cleanup.</p>
 *
 * <p><b>Integration pattern:</b> C (rebalance, preserve feature).</p>
 *
 * <p><b>Rebalance:</b> Clamp the piercing level applied to fired
 * projectiles to at most 1. Enchantment still rolls at higher levels
 * in loot; the NBT still stores the level. The clamp happens at
 * projectile-fire time, so no existing enchanted crossbow is
 * invalidated — it just fires at the capped effectiveness.</p>
 *
 * <p><b>Alternative considered and rejected:</b> Overwrite
 * {@code PIERCING.getMaxLevel()} to 1. Rejected because it affects
 * enchanting-table RNG only; pre-generated loot with Piercing IV
 * crossbows would keep their level and fire at level IV via the
 * consumption path (NBT-stored). Clamping at consumption is more
 * robust and doesn't affect other mods that query the enchant max.</p>
 *
 * <p><b>Injection strategy:</b> {@link Redirect} on the
 * {@code AbstractArrow#setPierceLevel(byte)} call inside
 * {@code CrossbowItem#getArrow} — the private static helper that builds
 * the projectile and stamps the crossbow's Piercing level onto it.
 * (Note: this is {@code getArrow}, NOT {@code shootProjectile}; the
 * pierce transfer lives in the arrow-construction helper, verified
 * against the 1.20.1 official mappings — {@code m_40914_}.) Trident
 * piercing goes through a separate path and is unaffected.</p>
 */
@Mixin(CrossbowItem.class)
public abstract class CrossbowPiercingMixin {

    @Redirect(
        method = "getArrow",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/projectile/AbstractArrow;setPierceLevel(B)V"
        )
    )
    private static void btw$clampCrossbowPierce(AbstractArrow arrow, byte requestedPierce) {
        byte clamped = (byte) Math.min(1, (int) requestedPierce);
        arrow.setPierceLevel(clamped);
    }
}
