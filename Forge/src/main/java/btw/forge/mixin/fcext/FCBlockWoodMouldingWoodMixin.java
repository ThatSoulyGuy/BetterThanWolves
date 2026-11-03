package btw.forge.mixin.fcext;

import btw.forge.ModernWoodSubBlocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * FCMOD-INTEGRATION(1.20.1): modern_wood_subblocks (W3, the moulding-and-decorative blocks)
 *
 * <p><b>Feature:</b> the acacia, dark oak, mangrove and cherry moulding-and-decorative blocks (1094, 1096, 1098, 1100;
 * ModernWoodSubBlocks) drop and saw into stubs of their own wood type 5-8.</p>
 *
 * <p><b>Conflict with FC:</b> FCBlockWoodMouldingAndDecorative.damageDropped turns its id into a wood type inline,
 * blood wood (4) for any id but FC's five (Client :56-80), then packs the decorative stub damage (:81-101). It also
 * feeds GetItemDamageDroppedOnSaw (:127-137): moulding to corners, and the decorative saw chain.</p>
 *
 * <p><b>Integration pattern:</b> D (FC-extend): FC's class, keyed on four more ids.</p>
 *
 * <p><b>Rebalance:</b> none.</p>
 *
 * <p><b>Injection strategy:</b> load-time mixin on the FC class ({@code @Pseudo}, {@code remap = false}; javap of
 * Forge/build/classes/java/fc): {@code damageDropped(I)I} HEAD, cancellable, for the new ids answers FC's own result
 * for that wood type (ModernWoodSubBlocks.mouldingDamageDropped re-expresses :56-102 for one wood; FC is frozen).</p>
 *
 * <p><b>Alternative considered and rejected:</b> {@code @ModifyVariable} on the inline wood-type local (it needs the
 * local variable table or {@code ordinal} guessing across five stores).</p>
 */
// FCMOD-INTEGRATION(1.20.1): modern_wood_subblocks: blocks 1094/1096/1098/1100 drop wood types 5-8, not blood wood
@Pseudo
@Mixin(targets = "net.minecraft.src.btw.block.FCBlockWoodMouldingAndDecorative", remap = false)
public abstract class FCBlockWoodMouldingWoodMixin {

    /** damageDropped (:56-102): the moulding or decorative stub damage of wood type 5-8 for the newer woods' blocks. */
    @Inject(method = "damageDropped(I)I", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
    private void btw$modernWoodDrop(int metadata, CallbackInfoReturnable<Integer> cir) {
        int wood = ModernWoodSubBlocks.woodTypeOfBlock(((btw.modern.Block) (Object) this).blockID);
        if (wood >= 0) {
            cir.setReturnValue(ModernWoodSubBlocks.mouldingDamageDropped(wood, metadata));
        }
    }
}
