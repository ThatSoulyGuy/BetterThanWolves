package btw.forge.mixin.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Read access to the two halves of VertexMultiConsumer.Double (package-private, VertexMultiConsumer.java:26-28), the
 * pair LevelRenderer hands a cracking block entity's renderer: the crack decal first, the real buffer second
 * (LevelRenderer.java:1251-1255). FCBreakingCrack.withoutCrumbling keeps the real buffer for FC tile entities, which
 * 4.B never cracked (R3-CRACK Part C). Client mixin config only.
 */
// FCMOD-INTEGRATION(1.20.1): fc_crack_4b: FC tile-entity geometry gets no crack, as in 4.B
@Mixin(targets = "com.mojang.blaze3d.vertex.VertexMultiConsumer$Double")
public interface VertexMultiConsumerDoubleAccessor {

    @Accessor("first")
    VertexConsumer btw$first();

    @Accessor("second")
    VertexConsumer btw$second();
}
