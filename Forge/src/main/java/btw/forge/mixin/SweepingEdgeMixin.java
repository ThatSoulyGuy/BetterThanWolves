package btw.forge.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collections;
import java.util.List;

/**
 * FCMOD-INTEGRATION(1.20.1): sweeping_edge
 *
 * <p><b>Feature:</b> Sweeping Edge enchantment (MC 1.11+, sword-only).
 * When the player attacks with full cooldown, non-critical, non-sprint,
 * on-ground, the sword's strike deals reduced damage (1.0 + ratio ×
 * primary damage) to every other {@code LivingEntity} inside an
 * inflated AABB around the target.</p>
 *
 * <p><b>Conflict with FC:</b> FC's combat is per-target-hit — every
 * swing costs exhaustion, and damage is reconciled against individual
 * entities. Area-of-effect melee breaks that accounting: a single
 * swing can kill multiple mobs without paying the per-mob exhaustion
 * cost. FC's spider swarms and husk clusters are tuned as
 * numerically-threatening encounters, not as loot-bundles.</p>
 *
 * <p><b>Integration pattern:</b> C (rebalance, preserve feature).</p>
 *
 * <p><b>Rebalance:</b> Sweeping animation, sound, and particle still
 * fire (visible feedback is preserved). The list of peripheral
 * entities eligible for sweep damage is replaced with an empty list,
 * so no secondary target takes damage. Net effect: Sweeping Edge is a
 * cosmetic flourish, not a damage multiplier.</p>
 *
 * <p><b>Alternative considered and rejected:</b> Redirect the
 * {@code EnchantmentHelper.getSweepingDamageRatio} call to return 0.
 * Rejected because the vanilla formula is
 * {@code sweepMul = 1.0f + ratio * damage}; returning ratio=0 still
 * yields {@code sweepMul = 1.0f}, so peripheral entities still take
 * 1 damage per sweep. Zeroing the entity-target list is cleaner and
 * cannot interact with future vanilla changes to the sweep formula.</p>
 *
 * <p><b>Alternative considered and rejected (2):</b> Overwriting the
 * whole {@code Player#attack} sweep block. Rejected — very high
 * mixin-conflict risk with mods that inject into attack for stat/damage
 * reasons. A surgical {@link Redirect} on the entity-lookup is the
 * smallest viable change.</p>
 *
 * <p><b>Injection strategy:</b> {@link Redirect} on
 * {@code Level#getEntitiesOfClass(Class, AABB)} within
 * {@code Player#attack}. The only place attack uses this API is for
 * the sweep target lookup, so scoping the redirect to this method is
 * safe. We return an empty list so vanilla's subsequent forEach loop
 * iterates zero times.</p>
 */
@Mixin(Player.class)
public abstract class SweepingEdgeMixin {

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Redirect(
        method = "attack",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"
        )
    )
    private List btw$zeroSweepTargets(Level level, Class type, AABB bounds) {
        return Collections.emptyList();
    }
}
