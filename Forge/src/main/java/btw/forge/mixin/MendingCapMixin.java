package btw.forge.mixin;

import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * FCMOD-INTEGRATION(1.20.1): mending
 *
 * <p><b>Feature:</b> Mending enchantment (MC 1.9+). Picking up an XP orb
 * while wearing/holding a mending-enchanted item converts XP into durability
 * repair at a 1 XP : 2 durability ratio.</p>
 *
 * <p><b>Conflict with FC:</b> FC's gameplay loop assumes tool durability
 * matters — the anvil, steel, and soulforge progression are all motivated
 * by the need to repair/replace tools. Vanilla Mending, on a single
 * enchanted tool, turns that progression into an optional nicety: one
 * pickaxe with Mending is infinite.</p>
 *
 * <p><b>Integration pattern:</b> C (rebalance, preserve feature).</p>
 *
 * <p><b>Rebalance:</b> Halve the physical durability restored per XP orb
 * while leaving XP consumption identical. Net effect:</p>
 * <ul>
 *   <li>Mending still works — players can't be surprised by feature deletion.</li>
 *   <li>A pickaxe with Mending now takes 2× as many XP orbs to fully
 *       repair, keeping the anvil loop relevant.</li>
 *   <li>XP economy is untouched: the same XP is consumed by the orb as
 *       in vanilla; nothing else (levels, enchanting) is affected.</li>
 * </ul>
 *
 * <p><b>Alternative considered and rejected:</b> Gating mending-book
 * drops behind FC soulforge crafting. Rejected under Protocol #5
 * (dual-source items) — removing mending from vanilla enchanting-table
 * RNG or loot tables would break any other mod that queries for
 * mending-enchanted items through vanilla channels.</p>
 *
 * <p><b>Injection strategy:</b> {@link Redirect} on the {@code setDamageValue}
 * call inside {@code ExperienceOrb.repairPlayerItems}. This is the one
 * site where vanilla converts XP into item healing. We intercept the
 * call, read the proposed damage delta, halve it, and apply. The
 * surrounding logic — XP consumption, enchantment lookup, recursion into
 * remaining orbs — runs vanilla and is untouched.</p>
 */
@Mixin(ExperienceOrb.class)
public abstract class MendingCapMixin {

    /**
     * Intercepts vanilla's durability restoration and halves the amount
     * actually applied to the item. XP consumption stays identical.
     */
    @Redirect(
        method = "repairPlayerItems",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;setDamageValue(I)V"
        )
    )
    private void btw$cappedMending(ItemStack stack, int proposedDamage) {
        int currentDamage = stack.getDamageValue();
        int vanillaRepair = currentDamage - proposedDamage;
        // Halve: floor-div keeps 1-durability orbs from still repairing
        // fully (0 restored if vanillaRepair == 1).
        int cappedRepair = vanillaRepair / 2;
        stack.setDamageValue(currentDamage - cappedRepair);
    }
}
