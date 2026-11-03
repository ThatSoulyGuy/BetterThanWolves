package btw.forge.mixin;

import btw.forge.BlastRules;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * FCMOD-INTEGRATION(1.20.1): fc_blast_rules
 * <p><b>Conflict:</b> 1.20.1's explosion rays (Explosion.explode, Explosion.java:130-170) ask
 * ExplosionDamageCalculator.getBlockExplosionResistance for every step, which reaches FC only through the
 * position-less Block.getExplosionResistance (BlockMixin). 4.B's doExplosionA asked FC's positional overload
 * (vanilla/server Explosion.java:107-108, Entity.java:2244-2252), where FCBlockStone and FCBlockOre read the strata:
 * mid stone 7.8, deep stone 12, mid ore 4.2, deep ore 6 (Server FCBlockStone.java:65-85, FCBlockOre.java:65-85).</p>
 * <p><b>Pattern:</b> D. A state whose HookPolicy RESIST owner is FC (its FC class declares the positional overload in
 * FC code), or a BTW block whose FC class does, answers FC's positional value, maxed with the fluid's as 1.20.1 does;
 * every other state keeps 1.20.1's body. EntityBasedExplosionDamageCalculator calls this body (super), so the source's
 * own adjustment (the wither skull's 0.8 cap) still applies on top. Server thread only.</p>
 */
@Mixin(ExplosionDamageCalculator.class)
public abstract class ExplosionDamageCalculatorMixin {

    // FCMOD-INTEGRATION(1.20.1): fc_blast_rules: blocks whose FC class reads its position for blast resistance (the strata) resist 1.20.1's rays with FC's positional value, as 4.B's doExplosionA asked it
    @Inject(method = "getBlockExplosionResistance", at = @At("HEAD"), cancellable = true)
    private void btw$fcPositionalResistance(Explosion explosion, BlockGetter getter, BlockPos pos, BlockState state,
                                            FluidState fluid, CallbackInfoReturnable<Optional<Float>> cir) {
        Float resistance = BlastRules.positionalResistance(getter, pos, state, fluid, explosion);
        if (resistance != null) cir.setReturnValue(Optional.of(resistance));
    }
}
