package btw.forge.mixin.client;

import btw.forge.client.FCBreakingCrack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The breaking crack of a block FC draws is 4.B's (R3-CRACK Part C; see {@link FCBreakingCrack}). 1.20.1's crumbling
 * pass calls {@link BlockRenderDispatcher#renderBreakingTexture} for every cracking block with the stage's
 * SheetedDecalTextureGenerator (LevelRenderer.java:1314-1316); that method re-tessellates the static model quads into the
 * decal (BlockRenderDispatcher.java:52-58). At its head, a block whose model is FC's is drawn by FC's own
 * RenderBlockWithTexture into the decal's buffer instead, and the call returns. The deprecated five-argument overload
 * delegates to this one (:49-51).
 *
 * <p>The target is the overload Forge adds (it carries the ModelData); it has no SRG name (createMcpToSrg/output.tsrg
 * maps only the vanilla five-argument renderBreakingTexture, to m_110918_) and keeps its name in production, hence
 * {@code remap = false}, as FarmBlockMixin does for its Forge target.</p>
 *
 * <p>Client mixin config only: BlockRenderDispatcher is client-only.</p>
 */
// FCMOD-INTEGRATION(1.20.1): fc_crack_4b: the breaking crack of a block FC draws is FC's own RenderBlockWithTexture, drawn into 1.20.1's crumbling buffer
@Mixin(BlockRenderDispatcher.class)
public abstract class BlockRenderDispatcherCrackMixin {

    @Inject(method = "renderBreakingTexture(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;"
            + "Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraftforge/client/model/data/ModelData;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void btw$fcCrack(BlockState state, BlockPos pos, BlockAndTintGetter level, PoseStack poseStack,
                             VertexConsumer consumer, ModelData modelData, CallbackInfo ci) {
        if (FCBreakingCrack.renderBreakingTexture(state, pos, level, poseStack, consumer)) {
            ci.cancel();
        }
    }
}
