package btw.forge.mixin.fcext;

import btw.forge.NetherPortalShapes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * FCMOD-INTEGRATION(1.20.1): nether_portal (INTEGRATIONS #058)
 *
 * <p>FC's portal lighting entry, FCBlockPortal.tryToCreatePortal (Common FCBlockPortal.java:56-131; called by
 * FCBlockCampfire.SetOnFireDirectly, Server FCBlockCampfire.java:237), goes through 1.20.1's PortalShape and Forge's
 * PortalSpawnEvent ({@link NetherPortalShapes#tryToCreatePortal}): the frame's own axis and any size 1.20.1 fire
 * lights, where FC's 1.5.2 test lit only 2x3 frames and wrote every cell as {@code nether_portal[axis=x]}. Feature,
 * conflict, pattern, rebalance and alternatives: btw.forge.NetherPortalShapes.</p>
 *
 * <p><b>Injection strategy:</b> load-time mixin on the FC class ({@code @Pseudo}, {@code remap = false}; javap of the
 * remapped FC output: {@code public boolean tryToCreatePortal(btw.modern.World, int, int, int)}, declared by
 * FCBlockPortal over btw.modern.BlockPortal's {@code return false}). {@code @Inject} at HEAD, cancellable: the bridge's
 * answer is the return value, so FC's frame test, its {@code setBlock(90, 0, 2)} writes and its own Nether flag call
 * (:127) do not run; the flag comes from the PortalSpawnEvent listener NetherPortalProgress. updateTick (possession,
 * the Nether flag) and the bounds methods are untouched.</p>
 */
// FCMOD-INTEGRATION(1.20.1): nether_portal: FC's portal lighting entry (the campfire path) goes through 1.20.1's PortalShape and Forge's PortalSpawnEvent
@Pseudo
@Mixin(targets = "net.minecraft.src.btw.block.FCBlockPortal", remap = false)
public abstract class FCBlockPortalShapeMixin {

    /** FCBlockPortal.tryToCreatePortal (Common FCBlockPortal.java:57): 1.20.1's PortalShape answers. */
    @Inject(method = "tryToCreatePortal(Lbtw/modern/World;III)Z", at = @At("HEAD"), cancellable = true, require = 1,
            remap = false)
    private void btw$portalShape(btw.modern.World world, int i, int j, int k, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(NetherPortalShapes.tryToCreatePortal(world, i, j, k));
    }
}
