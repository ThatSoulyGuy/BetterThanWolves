package btw.forge.client;

import btw.forge.mixin.client.SheetedDecalAccessor;
import btw.forge.mixin.client.VertexMultiConsumerDoubleAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The breaking crack of the blocks FC draws, as 4.B drew it (fc_crack_4b; R3-CRACK Part C, the maintainer's ruling of
 * 2026-10-22: the exact 4.B look).
 *
 * <p>4.B: 1.5.2's RenderGlobal.drawBlockDamageTexture (vanilla/client RenderGlobal.java:1567-1624) drew, every frame and
 * for every cracking block within 32 blocks, the block's {@code RenderBlockWithTexture} with the stage's destroy icon,
 * blended DST_COLOR/SRC_COLOR. That is the whole crack: block-entity renderers never drew one.</p>
 *
 * <p>1.20.1: LevelRenderer.renderLevel draws each cracking block's model through a SheetedDecalTextureGenerator over
 * the crumbling buffer of the stage (LevelRenderer.java:1302-1321, the same 32-block reach), which throws the model's
 * UVs away and projects the crack on the axis nearest each quad's normal (SheetedDecalTextureGenerator.java:47-58); and
 * it lays the same decal on every crumbling-capable RenderType a block entity's renderer draws (:1246-1257).</p>
 *
 * <p>The rule: the crack follows the renderer. A block whose world model is FC's ({@link FCBakedModel}, directly or
 * through its per-state view) is cracked by FC's own renderer ({@link FCBakedModel#renderCrack}), into the decal's own
 * buffer, so the stage's texture, blending and batching stay 1.20.1's; FC tile-entity geometry gets no crack. Every
 * other block, vanilla JSON models and FC's 1.5.2 replacements at vanilla ids included, keeps 1.20.1's decal.</p>
 */
public final class FCBreakingCrack {
    private FCBreakingCrack() {}

    // FCMOD-INTEGRATION(1.20.1): fc_crack_4b: the breaking crack of a block FC draws is FC's own RenderBlockWithTexture, drawn into 1.20.1's crumbling buffer
    /**
     * BlockRenderDispatcher.renderBreakingTexture's head (BlockRenderDispatcherCrackMixin). True when the crack was drawn
     * here and 1.20.1's must not be. Only LevelRenderer's crumbling pass hands in a SheetedDecalTextureGenerator
     * (LevelRenderer.java:1314-1316); any other caller, or a block FC does not draw, keeps 1.20.1's path.
     */
    public static boolean renderBreakingTexture(BlockState state, BlockPos pos, BlockAndTintGetter level,
                                                PoseStack poseStack, VertexConsumer consumer) {
        if (!(consumer instanceof SheetedDecalTextureGenerator)) return false;
        FCBakedModel model = FCBakedModel.of(Minecraft.getInstance().getBlockRenderer().getBlockModel(state));
        if (model == null) return false;
        VertexConsumer crumbling = ((SheetedDecalAccessor) consumer).btw$delegate();
        return model.renderCrack(state, level, pos, poseStack, crumbling);
    }

    /**
     * The buffers of an FC tile entity's renderer without 1.20.1's crack. While the block cracks, LevelRenderer hands
     * the renderer a source whose crumbling-capable buffers are VertexMultiConsumer.create(decal, buffer)
     * (LevelRenderer.java:1251-1255); this source answers the buffer alone, exactly what it answers when nothing cracks.
     */
    public static MultiBufferSource withoutCrumbling(MultiBufferSource source) {
        return renderType -> {
            VertexConsumer consumer = source.getBuffer(renderType);
            if (consumer instanceof VertexMultiConsumerDoubleAccessor pair
                    && pair.btw$first() instanceof SheetedDecalTextureGenerator) {
                return pair.btw$second();
            }
            return consumer;
        };
    }
}
