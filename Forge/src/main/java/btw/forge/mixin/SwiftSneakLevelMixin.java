package btw.forge.mixin;

import net.minecraft.world.item.enchantment.SwiftSneakEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * FCMOD-INTEGRATION(1.20.1): swift_sneak
 *
 * <p><b>Feature:</b> Swift Sneak enchantment (MC 1.19+). Legging-slot
 * enchant that reduces the sneak-speed penalty, approaching full walking
 * speed while crouched at level 3.</p>
 *
 * <p><b>Conflict with FC:</b> FC's stealth and mining-inside-caves loop
 * depend on crouch being a real speed trade-off. Swift Sneak III
 * eliminates the trade-off entirely, making crouched mining strictly
 * better than upright mining (no food-spam from moving slowly, no
 * ledge-fall risk). That collapses the design decision FC expects the
 * player to make moment-to-moment in caves.</p>
 *
 * <p><b>Integration pattern:</b> C (rebalance, preserve feature).</p>
 *
 * <p><b>Rebalance:</b> Cap the enchantment at level 1 — still provides
 * a partial mitigation of the crouch penalty (useful for long mining
 * sessions) without eliminating it.</p>
 *
 * <p><b>Known limitation:</b> Pre-generated ancient-city loot that rolls
 * Swift Sneak III books still functions at level 3 — the level is stored
 * on the ItemStack NBT and consulted at consumption time. New items
 * generated from the enchanting table or Forge's enchantment RNG will
 * cap at 1. A consumption-time clamp (mixin on
 * {@code EnchantmentHelper#getEnchantmentLevel}) is deferred as a
 * follow-up if pre-generated level-3 books are found to be a problem
 * in playtest.</p>
 *
 * <p><b>Alternative considered and rejected:</b> Removing Swift Sneak
 * from the ancient city chest loot table. Rejected under Protocol #5 —
 * dual-sourcing preserves the enchant's presence for other mods.</p>
 *
 * <p><b>Injection strategy:</b> {@link Inject} on {@code getMaxLevel}
 * with {@code cancellable = true}. Replaces the returned max level
 * with {@code 1}. Using Inject rather than Overwrite leaves room for
 * other mods to layer on top.</p>
 */
@Mixin(SwiftSneakEnchantment.class)
public abstract class SwiftSneakLevelMixin {

    @Inject(method = "getMaxLevel", at = @At("HEAD"), cancellable = true)
    private void btw$capSwiftSneakMaxLevel(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(1);
    }
}
