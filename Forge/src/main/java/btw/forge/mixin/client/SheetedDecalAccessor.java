package btw.forge.mixin.client;

import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Read access to the crumbling buffer a SheetedDecalTextureGenerator writes into (its private final {@code delegate},
 * SheetedDecalTextureGenerator.java:13), so FCBreakingCrack can draw 4.B's crack into the stage's own buffer
 * (R3-CRACK Part C). Client mixin config only.
 */
// FCMOD-INTEGRATION(1.20.1): fc_crack_4b: 4.B's crack goes into the crumbling buffer of 1.20.1's decal for the stage
@Mixin(SheetedDecalTextureGenerator.class)
public interface SheetedDecalAccessor {

    @Accessor("delegate")
    VertexConsumer btw$delegate();
}
