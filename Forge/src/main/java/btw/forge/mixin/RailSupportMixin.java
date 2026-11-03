package btw.forge.mixin;

import btw.forge.FcAttachSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * FCMOD-INTEGRATION(1.20.1): attach_support_rules (INTEGRATIONS #055)
 *
 * <p><b>Feature:</b> 1.20.1's rail survival. BaseRailBlock.neighborChanged (BaseRailBlock.java:70-81) drops a rail on
 * the server when shouldBeRemoved (:83-100) finds no rigid support below it or, for an ascending shape, under the end it
 * climbs onto. Neither it nor BaseRailBlock.updateShape (:145-151) asks canSurvive, so AttachableSupportMixin's
 * canSurvive veto reaches a rail's placement only. A straight rail (powered, detector, activator) also runs this check
 * from its own onPlace (updateState, :61-68).</p>
 *
 * <p><b>Conflict with FC:</b> FC's rails (66 FCBlockRailRegular, 28 FCBlockDetectorRail, BTW's detector rails 235 and
 * 236, and the powered rail through FcAttachSupport's support twin) drop by 4.B's rule in FC's neighbour check (1.5.2
 * BlockRailBase.onNeighborBlockChange, vanilla/server BlockRailBase.java:169-208: a solid top below, and under an
 * ascending rail's climb). That check never runs in the port: BaseRailBlock overrides neighborChanged without the base
 * body, so BlockBehaviorMixin's NOTIFY executor is never reached. A rail on glass, glowstone, ice, TNT, pistons, a
 * redstone lamp, a beacon, a hopper, a cauldron or a closed upper oak trapdoor (rigid in 1.20.1, no solid top in 4.B),
 * placed before the support changed, by /setblock, or (the powered rail, whose FC block at 27 is a plain block) by FC's
 * own route, stayed.</p>
 *
 * <p><b>Integration pattern:</b> D (FC-extend): 1.20.1's neighbour check and its drop (dropResources, removeBlock) stay;
 * its answer is vetoed by FC's rail rule ({@link FcAttachSupport#railDropped}). Maintainer ruling 2026-10-31: the paths
 * that never ask FC follow 4.B through the veto torches and ladders get, and survival ends too.</p>
 *
 * <p><b>Rebalance:</b> none (4.B parity). Only a veto: a rail 1.20.1 drops still drops. The activator rail (T1 removed
 * content, FC's stub 157, a rock cube with no rail rule) and a rail over a modern block keep 1.20.1's rule.</p>
 *
 * <p><b>Injection strategy:</b> {@link Inject} at RETURN of the private static
 * {@code shouldBeRemoved(BlockPos, Level, RailShape)} (SRG {@code m_49398_}; its one caller, neighborChanged, has
 * checked that the cell still holds this rail), cancellable: when 1.20.1 keeps the rail and FC's rule drops it, the
 * answer is true. A mixin of its own: AttachableSupportMixin's one inject targets canSurvive in six classes, and this
 * method exists in BaseRailBlock alone.</p>
 *
 * <p><b>Alternatives considered and rejected:</b> a HEAD inject on neighborChanged repeating 1.20.1's drop; asking
 * canSurvive there (it does not test the climb); FC's onNeighborBlockChange through a NOTIFY hook (Modern-Common's
 * BlockRailBase tests only below, and FC's rail bodies would run beside 1.20.1's).</p>
 */
// FCMOD-INTEGRATION(1.20.1): attach_support_rules: 1.20.1's rail neighbour check drops a rail wherever 4.B's rail neighbour check drops it
@Mixin(BaseRailBlock.class)
public abstract class RailSupportMixin {

    @Inject(method = "shouldBeRemoved(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/properties/RailShape;)Z",
            at = @At("RETURN"), cancellable = true)
    private static void btw$fcRailSupport(BlockPos pos, Level level, RailShape shape, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && FcAttachSupport.railDropped(level, pos, shape)) {
            cir.setReturnValue(true);
        }
    }
}
