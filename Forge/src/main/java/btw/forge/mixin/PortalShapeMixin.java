package btw.forge.mixin;

import btw.forge.NetherPortalShapes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * FCMOD-INTEGRATION(1.20.1): nether_portal (INTEGRATIONS #058)
 *
 * <p>1.20.1's frame interior test, PortalShape.isEmpty (PortalShape.java:160-162: air, fire, nether_portal), also takes
 * FC's four campfire blocks, lit or not, which 4.B's frame test counted as open interior (Common
 * FCBlockPortal.java:106-111; {@link NetherPortalShapes#isFcPortalInterior}). Every portal search reads it: fire's
 * lighting (BaseFireBlock.onPlace), the fire placement test of flint and steel, fire charges and dispensers
 * (BaseFireBlock.canBePlacedAt, through isPortal), FC's campfire lighting (NetherPortalShapes.tryToCreatePortal, where
 * the lit campfire stands in the frame) and NetherPortalBlock.updateShape's completeness test, which counts only portal
 * cells, so a campfire never keeps a broken portal standing. createPortalBlocks fills the campfire's cell with portal,
 * as 4.B's {@code setBlock(90)} did. Feature, conflict, pattern, rebalance and alternatives:
 * btw.forge.NetherPortalShapes.</p>
 *
 * <p><b>Injection strategy:</b> {@link Inject} at RETURN of the private static {@code isEmpty(BlockState)}
 * (SRG {@code m_77717_}; one {@code ireturn}), cancellable, static handler: a false answer becomes true for an FC
 * campfire. Common side: the client's updateShape asks the same question as the server's.</p>
 */
// FCMOD-INTEGRATION(1.20.1): nether_portal: 1.20.1's portal frame test counts FC's campfires as open interior, as 4.B's did
@Mixin(PortalShape.class)
public abstract class PortalShapeMixin {

    @Inject(method = "isEmpty(Lnet/minecraft/world/level/block/state/BlockState;)Z", at = @At("RETURN"),
            cancellable = true)
    private static void btw$fcPortalInterior(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && NetherPortalShapes.isFcPortalInterior(state)) {
            cir.setReturnValue(true);
        }
    }
}
