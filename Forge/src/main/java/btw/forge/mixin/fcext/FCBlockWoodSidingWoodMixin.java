package btw.forge.mixin.fcext;

import btw.forge.ModernWoodSubBlocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * FCMOD-INTEGRATION(1.20.1): modern_wood_subblocks (W2, the siding-and-corner blocks)
 *
 * <p><b>Feature:</b> the acacia, dark oak, mangrove and cherry siding-and-corner blocks (1093, 1095, 1097, 1099;
 * ModernWoodSubBlocks) drop and saw into stubs of their own wood type 5-8.</p>
 *
 * <p><b>Conflict with FC:</b> FCBlockWoodSidingAndCornerAndDecorative.GetWoodTypeFromBlockID compares its id with FC's
 * five blocks and answers blood wood (4) for any other (Client :207-233); it feeds damageDropped (siding, corner, bench
 * and fence, :56-79) and the fence's saw damage (:142-166).</p>
 *
 * <p><b>Integration pattern:</b> D (FC-extend): FC's class, keyed on four more ids.</p>
 *
 * <p><b>Rebalance:</b> none.</p>
 *
 * <p><b>Injection strategy:</b> load-time mixin on the FC class ({@code @Pseudo}, {@code remap = false}; javap of
 * Forge/build/classes/java/fc): {@code GetWoodTypeFromBlockID()I} HEAD, cancellable, answers 5-8 for the new ids, so
 * every FC caller of that one method follows.</p>
 *
 * <p><b>Alternative considered and rejected:</b> patching damageDropped and the saw methods one by one (three callers of
 * the one method FC already routes them through).</p>
 */
// FCMOD-INTEGRATION(1.20.1): modern_wood_subblocks: blocks 1093/1095/1097/1099 are wood types 5-8, not blood wood
@Pseudo
@Mixin(targets = "net.minecraft.src.btw.block.FCBlockWoodSidingAndCornerAndDecorative", remap = false)
public abstract class FCBlockWoodSidingWoodMixin {

    /** GetWoodTypeFromBlockID (:207-233): wood type 5-8 for the newer woods' blocks. */
    @Inject(method = "GetWoodTypeFromBlockID()I", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private void btw$modernWoodType(CallbackInfoReturnable<Integer> cir) {
        int wood = ModernWoodSubBlocks.woodTypeOfBlock(((btw.modern.Block) (Object) this).blockID);
        if (wood >= 0) {
            cir.setReturnValue(wood);
        }
    }
}
