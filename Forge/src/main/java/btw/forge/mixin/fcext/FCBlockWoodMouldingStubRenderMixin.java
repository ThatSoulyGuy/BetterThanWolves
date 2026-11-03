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
 * FCMOD-INTEGRATION(1.20.1): modern_wood_subblocks (W6, the moulding and column/pedestal/table stub items)
 *
 * <p><b>Feature:</b> moulding stubs of wood types 5-8 draw their own wood's planks, and column, pedestal and table stubs
 * draw as their own wood's column, pedestal and table.</p>
 *
 * <p><b>Conflict with FC:</b> FCBlockWoodMouldingAndDecorative.RenderBlockAsItem picks the decorative stub's block with
 * a chain over wood types 0-3 whose default is blood wood (Client :252-303, :290-293), and the moulding stub's texture
 * with a switch over 1-4 whose default is oak (:304-344, :338-341).</p>
 *
 * <p><b>Integration pattern:</b> D (FC-extend): FC's item renderer, with four more woods.</p>
 *
 * <p><b>Rebalance:</b> none (client cosmetics).</p>
 *
 * <p><b>Injection strategy:</b> load-time mixin on the FC class ({@code @Pseudo}, {@code remap = false}; javap -c of
 * Forge/build/classes/java/fc): RenderBlockAsItem HEAD keeps the item damage in a thread-local; {@code @ModifyArg}
 * index 1 (the Block) of its only RenderDecorativeInvBlock call (offset 146, invokevirtual, owner this class) answers
 * the wood's own moulding block (ModernWoodSubBlocks.decorativeRenderBlock), and index 5 (the Icon) of its only
 * FCClientUtilsRender.RenderInvBlockWithTexture call (261; the calls at :358/:365 are in RenderOakTableInvBlock)
 * answers the wood's own planks (ModernWoodSubBlocks.stubIcon). The oak-table branch never sees 5-8.</p>
 *
 * <p><b>Alternative considered and rejected:</b> rewriting the renderer (FC's bounds and drawing stay FC's).</p>
 */
// FCMOD-INTEGRATION(1.20.1): modern_wood_subblocks: the moulding and column/pedestal/table stubs of wood types 5-8 draw as their own wood
@Pseudo
@Mixin(targets = "net.minecraft.src.btw.block.FCBlockWoodMouldingAndDecorative", remap = false)
public abstract class FCBlockWoodMouldingStubRenderMixin {

    /** The item damage RenderBlockAsItem was called with on this thread. */
    @Unique
    private static final ThreadLocal<Integer> btw$damage = new ThreadLocal<>();

    /** RenderBlockAsItem HEAD (:252): keep the stub's damage (FC reassigns its local before the decorative draw). */
    @Inject(method = "RenderBlockAsItem(Lbtw/modern/RenderBlocks;IF)V", at = @At("HEAD"), require = 1, remap = false)
    private void btw$keepDamage(btw.modern.RenderBlocks renderBlocks, int damage, float brightness, CallbackInfo ci) {
        btw$damage.set(damage);
    }

    /** The column, pedestal or table FC draws (:301): the wood's own moulding block for 5-8. */
    @ModifyArg(method = "RenderBlockAsItem(Lbtw/modern/RenderBlocks;IF)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/src/btw/block/FCBlockWoodMouldingAndDecorative;RenderDecorativeInvBlock(Lbtw/modern/RenderBlocks;Lbtw/modern/Block;IF)V",
            remap = false), index = 1, require = 1, remap = false)
    private btw.modern.Block btw$modernWoodDecorative(btw.modern.Block block) {
        Integer damage = btw$damage.get();
        btw.modern.Block mine = damage == null ? null
                : ModernWoodSubBlocks.decorativeRenderBlock(((btw.modern.Block) (Object) this).blockID, damage);
        return mine != null ? mine : block;
    }

    /** The texture of FC's moulding stub draw (:343): the wood's own planks for 5-8. */
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
