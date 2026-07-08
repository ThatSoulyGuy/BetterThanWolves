package btw.forge.mixin;

import net.minecraft.world.item.enchantment.FrostWalkerEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * FCMOD-INTEGRATION(1.20.1): frost_walker
 *
 * <p><b>Feature:</b> Frost Walker enchantment (MC 1.9+). Freezes water
 * blocks into Frosted Ice in a radius around the wearer when they walk.</p>
 *
 * <p><b>Conflict with FC:</b> Vanilla Frost Walker at level 1 freezes a
 * 5×5 area under the player — effectively converting any body of water
 * into walkable terrain. FC's worldgen tunes lake/ocean crossings as
 * deliberate traversal obstacles (swimming exhausts the hunger bar, water
 * slows the player, ice-breaking is limited to FC tools). Free water
 * crossings defeat that design.</p>
 *
 * <p><b>Integration pattern:</b> C (rebalance, preserve feature).</p>
 *
 * <p><b>Rebalance:</b> Clamp the freeze radius formula from
 * {@code 2 + enchantLevel} to {@code 0 + enchantLevel}. Net effect:</p>
 * <ul>
 *   <li>Level 1 → 1×1 freeze (just the block under the player)</li>
 *   <li>Level 2 → 3×3 freeze (small puddle, not a crossing)</li>
 *   <li>Enchantment stays available — no removal from loot/treasure RNG.</li>
 * </ul>
 *
 * <p><b>Alternative considered and rejected:</b> Remove Frost Walker from
 * treasure enchant pools. Rejected under Protocol #5 (dual-sourcing) —
 * the enchantment should still appear in the enchanted-book loot pool
 * so other mods querying by ID still see it.</p>
 *
 * <p><b>Injection strategy:</b> {@link ModifyConstant} on the {@code 2}
 * literal inside {@code onEntityMoved} — that's the base radius added to
 * the enchantment level. Changing it to {@code 0} drops the radius by 2
 * at every level.</p>
 */
@Mixin(FrostWalkerEnchantment.class)
public abstract class FrostWalkerRadiusMixin {

    @ModifyConstant(
        method = "onEntityMoved",
        constant = @Constant(intValue = 2)
    )
    private static int btw$clampFrostWalkerBaseRadius(int vanillaBase) {
        return 0;
    }
}
