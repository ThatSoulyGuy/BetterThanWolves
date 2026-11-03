package btw.forge.mixin.fcext;

import btw.forge.ModernWoodSubBlocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * FCMOD-INTEGRATION(1.20.1): modern_wood_subblocks (W4, the siding and corner stub items' texture)
 *
 * <p><b>Feature:</b> siding and corner stubs of wood types 5-8 draw their own wood's planks in inventories, hands and
 * item frames.</p>
 *
 * <p><b>Conflict with FC:</b> FCBlockSidingAndCorner.RenderBlockAsItem picks the stub's texture with a switch over
 * the item damage 1-4 whose default is oak (Client :516-571, default :561), so the newer woods' stubs drew as oak.</p>
 *
 * <p><b>Integration pattern:</b> D (FC-extend): FC's item renderer, with four more woods.</p>
 *
 * <p><b>Rebalance:</b> none (client cosmetics).</p>
 *
 * <p><b>Injection strategy:</b> load-time mixin on the FC class ({@code @Pseudo}, {@code remap = false}; javap -c of
 * Forge/build/classes/java/fc): RenderBlockAsItem HEAD keeps the item damage in a thread-local (renders run one per
 * call under FCBakedModel.CAPTURE_LOCK), and {@code @ModifyArg} index 5 of its only
 * FCClientUtilsRender.RenderInvBlockWithTexture call (offset 178; the call at 193 is RenderInvBlockWithMetadata)
 * answers the wood's block icon (ModernWoodSubBlocks.stubIcon). Client-only in effect: FC's renderers never run on a
 * dedicated server, and the Client copy of the class ships on both sides.</p>
 *
 * <p><b>Alternative considered and rejected:</b> rewriting the renderer (FC's bounds and draw call stay FC's).</p>
 */
// FCMOD-INTEGRATION(1.20.1): modern_wood_subblocks: the siding and corner stubs of wood types 5-8 draw their own planks
@Pseudo
@Mixin(targets = "net.minecraft.src.btw.block.FCBlockSidingAndCorner", remap = false)
public abstract class FCBlockSidingStubRenderMixin {

    /** The item damage RenderBlockAsItem was called with on this thread. */
    @Unique
    private static final ThreadLocal<Integer> btw$damage = new ThreadLocal<>();

    /** RenderBlockAsItem HEAD (:516): keep the stub's damage for the texture choice. */
    @Inject(method = "RenderBlockAsItem(Lbtw/modern/RenderBlocks;IF)V", at = @At("HEAD"), require = 1, remap = false)
    private void btw$keepDamage(btw.modern.RenderBlocks renderBlocks, int damage, float brightness, CallbackInfo ci) {
        btw$damage.set(damage);
    }

    /** The texture of FC's stub draw (:566): the wood's own planks for 5-8, FC's choice otherwise. */
    @ModifyArg(method = "RenderBlockAsItem(Lbtw/modern/RenderBlocks;IF)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/src/btw/client/FCClientUtilsRender;RenderInvBlockWithTexture(Lbtw/modern/RenderBlocks;Lbtw/modern/Block;FFFLbtw/modern/Icon;)V",
            remap = false), index = 5, require = 1, remap = false)
    private btw.modern.Icon btw$modernWoodTexture(btw.modern.Icon icon) {
        Integer damage = btw$damage.get();
        btw.modern.Icon mine = damage == null ? null
                : ModernWoodSubBlocks.stubIcon(((btw.modern.Block) (Object) this).blockID, damage);
        return mine != null ? mine : icon;
    }
}
