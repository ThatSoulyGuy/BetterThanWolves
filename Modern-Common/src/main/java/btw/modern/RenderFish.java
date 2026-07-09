package btw.modern;

/**
 * 1.5.2 RenderFish (vanilla/client RenderFish.java) — fishing-bobber renderer for the
 * frozen vanilla {@code EntityFishHook} ({@code fc_fish_hook}). Renders the bobber as a
 * camera-facing quad from {@code /particles.png}, recorded via {@link Render#loadTexture}.
 *
 * <p>Adaptation: the fishing LINE (angler → hook) is omitted. Vanilla draws it with a
 * {@code startDrawing(GL_LINE_STRIP)} pass and needs {@code Minecraft.thePlayer} plus the
 * angler's interpolated position/swing — the capture pipeline records quads, not line
 * strips, and has no client Minecraft handle here. The bobber renders; the line does not.
 * The billboard uses {@code RenderManager.playerViewY/X}, left at 0 by the capture
 * pipeline, so it faces a fixed direction rather than tracking the camera.</p>
 */
public class RenderFish extends Render {

    public void doRenderFishHook(EntityFishHook hook, double x, double y, double z, float yaw, float partialTick) {
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x, (float) y, (float) z);
        GL11.glScalef(0.5F, 0.5F, 0.5F);

        byte col = 1;
        byte row = 2;
        // FC's bobber sprite lives in the particles atlas; BTW ships it at gui/particles.png
        // (resolveEntityTexture -> textures/gui/particles.png). "/particles.png" resolved to a
        // non-shipped path and fell back to the placeholder.
        this.loadTexture("/gui/particles.png");

        Tessellator tess = Tessellator.instance;
        float u0 = (float) (col * 8) / 128.0F;
        float u1 = (float) (col * 8 + 8) / 128.0F;
        float v0 = (float) (row * 8) / 128.0F;
        float v1 = (float) (row * 8 + 8) / 128.0F;
        float size = 1.0F;
        float ox = 0.5F;
        float oy = 0.5F;

        float viewY = this.renderManager != null ? this.renderManager.playerViewY : 0.0F;
        float viewX = this.renderManager != null ? this.renderManager.playerViewX : 0.0F;
        GL11.glRotatef(180.0F - viewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-viewX, 1.0F, 0.0F, 0.0F);

        tess.startDrawingQuads();
        tess.setNormal(0.0F, 1.0F, 0.0F);
        tess.addVertexWithUV(0.0F - ox, 0.0F - oy, 0.0D, u0, v1);
        tess.addVertexWithUV(size - ox, 0.0F - oy, 0.0D, u1, v1);
        tess.addVertexWithUV(size - ox, 1.0F - oy, 0.0D, u1, v0);
        tess.addVertexWithUV(0.0F - ox, 1.0F - oy, 0.0D, u0, v0);
        tess.draw();

        GL11.glPopMatrix();
    }

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTick) {
        this.doRenderFishHook((EntityFishHook) entity, x, y, z, yaw, partialTick);
    }
}
