package btw.forge.mixin;

import net.minecraft.world.level.block.RespawnAnchorBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * FCMOD-INTEGRATION(1.20.1): respawn_anchor
 *
 * <p><b>Feature:</b> Respawn Anchor (MC 1.16+). A block placed in The
 * Nether that stores up to 4 charges of glowstone; each charge allows
 * one respawn at the anchor's location.</p>
 *
 * <p><b>Conflict with FC:</b> FC's HC Spawn mechanic (see integration
 * {@code #002} Gamerule defaults) makes the initial respawn location
 * load-bearing for the early-game progression curve. The respawn anchor
 * is effectively a portable, Nether-side second spawn — one that stacks
 * 4 deep, making death in the Nether free for a long stretch of play.</p>
 *
 * <p><b>Integration pattern:</b> C (rebalance, preserve feature).</p>
 *
 * <p><b>Rebalance:</b> Cap the anchor at a single charge. Each Nether
 * death now costs a fresh charge to recoup — the anchor becomes a
 * "one escape route" commitment rather than a routine safety net. The
 * reagent for that single charge is gated behind Concentrated Hellfire
 * per {@code docs/audit/alchemy.md} §"Respawn Anchor".</p>
 *
 * <p><b>Alternative considered and rejected:</b> Delete the respawn
 * anchor recipe entirely. Rejected under Protocol #5 — removes a
 * vanilla block rather than rebalancing it, breaks player autonomy.</p>
 *
 * <p><b>Injection strategy:</b> {@link ModifyConstant} on the {@code 4}
 * literal inside {@code canBeCharged(BlockState)}. The vanilla method
 * is a one-liner: {@code return state.getValue(CHARGE) < 4}. Changing
 * the comparison to {@code < 1} makes the anchor refuse any charge
 * beyond the first.</p>
 */
@Mixin(RespawnAnchorBlock.class)
public abstract class RespawnAnchorChargeMixin {

    @ModifyConstant(
        method = "canBeCharged",
        constant = @Constant(intValue = 4)
    )
    private static int btw$capRespawnAnchorCharges(int vanillaMaxCharges) {
        return 1;
    }
}
