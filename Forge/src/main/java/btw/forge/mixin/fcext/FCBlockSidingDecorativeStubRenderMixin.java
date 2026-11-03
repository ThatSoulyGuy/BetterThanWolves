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
 * FCMOD-INTEGRATION(1.20.1): modern_wood_subblocks (W5, the bench and fence stub items)
 *
 * <p><b>Feature:</b> bench and fence stubs of wood types 5-8 draw as their own wood's bench and fence.</p>
 *
 * <p><b>Conflict with FC:</b> FCBlockSidingAndCornerAndDecorative.RenderBlockAsItem picks the block it draws the
 * siding decorative stub as with a chain over wood types 0-3 whose default is blood wood (Client :601-656, :638-641).</p>
 *
 * <p><b>Integration pattern:</b> D (FC-extend): FC's item renderer, with four more woods.</p>
 *
 * <p><b>Rebalance:</b> none (client cosmetics).</p>
 *
 * <p><b>Injection strategy:</b> load-time mixin on the FC class ({@code @Pseudo}, {@code remap = false}; javap -c of
 * Forge/build/classes/java/fc): RenderBlockAsItem HEAD keeps the item damage in a thread-local, and {@code @ModifyArg}
 * index 1 (the Block) of its only RenderBenchInvBlock call (offset 116) and its only RenderFenceInvBlock call (134),
 * both invokestatic on this class, answers the wood's own siding block (ModernWoodSubBlocks.decorativeRenderBlock).</p>
 *
 * <p><b>Alternative considered and rejected:</b> rewriting the renderer (FC's bench and fence drawing stays FC's).</p>
 */
// FCMOD-INTEGRATION(1.20.1): modern_wood_subblocks: the bench and fence stubs of wood types 5-8 draw as their own wood
@Pseudo
@Mixin(targets = "net.minecraft.src.btw.block.FCBlockSidingAndCornerAndDecorative", remap = false)
public abstract class FCBlockSidingDecorativeStubRenderMixin {

    /** The item damage RenderBlockAsItem was called with on this thread. */
    @Unique
    private static final ThreadLocal<Integer> btw$damage = new ThreadLocal<>();

    /** RenderBlockAsItem HEAD (:601): keep the stub's damage for the block choice. */
    @Inject(method = "RenderBlockAsItem(Lbtw/modern/RenderBlocks;IF)V", at = @At("HEAD"), require = 1, remap = false)
    private void btw$keepDamage(btw.modern.RenderBlocks renderBlocks, int damage, float brightness, CallbackInfo ci) {
        btw$damage.set(damage);
    }

    /** The bench FC draws (:646): the wood's own siding block for 5-8. */
    @ModifyArg(method = "RenderBlockAsItem(Lbtw/modern/RenderBlocks;IF)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/src/btw/block/FCBlockSidingAndCornerAndDecorative;RenderBenchInvBlock(Lbtw/modern/RenderBlocks;Lbtw/modern/Block;I)V",
            remap = false), index = 1, require = 1, remap = false)
    private btw.modern.Block btw$modernWoodBench(btw.modern.Block block) {
        return btw$woodBlock(block);
    }

    /** The fence FC draws (:650): the wood's own siding block for 5-8. */
    @ModifyArg(method = "RenderBlockAsItem(Lbtw/modern/RenderBlocks;IF)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/src/btw/block/FCBlockSidingAndCornerAndDecorative;RenderFenceInvBlock(Lbtw/modern/RenderBlocks;Lbtw/modern/Block;I)V",
            remap = false), index = 1, require = 1, remap = false)
    private btw.modern.Block btw$modernWoodFence(btw.modern.Block block) {
        return btw$woodBlock(block);
    }

    @Unique
    private btw.modern.Block btw$woodBlock(btw.modern.Block block) {
        Integer damage = btw$damage.get();
        btw.modern.Block mine = damage == null ? null
                : ModernWoodSubBlocks.decorativeRenderBlock(((btw.modern.Block) (Object) this).blockID, damage);
        return mine != null ? mine : block;
    }
}
