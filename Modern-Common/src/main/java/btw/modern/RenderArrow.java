package btw.modern;

/**
 * 1.5.2 RenderArrow (vanilla/client RenderArrow.java) — the arrow renderer, for the
 * frozen vanilla {@code EntityArrow} ({@code fc_arrow}). Registered via
 * {@code FCEntityRenderer.registerMissingVanillaRenderers} (FC's ClientAddEntityRenderers
 * doesn't cover non-FC-subclass vanilla entities). Six quads (2 head + 4 shaft) captured
 * through the recording Tessellator. Texture {@code /item/arrows.png} recorded via
 * {@link Render#loadTexture} for the capture pipeline. glNormal3f is a shim no-op
 * (lighting comes from MC's pipeline).
 */
public class RenderArrow extends Render {

    public void renderArrow(EntityArrow arrow, double x, double y, double z, float yaw, float partialTick) {
        this.loadTexture("/item/arrows.png");
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x, (float) y, (float) z);
        GL11.glRotatef(arrow.prevRotationYaw + (arrow.rotationYaw - arrow.prevRotationYaw) * partialTick - 90.0F, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(arrow.prevRotationPitch + (arrow.rotationPitch - arrow.prevRotationPitch) * partialTick, 0.0F, 0.0F, 1.0F);

        Tessellator tess = Tessellator.instance;
        float headU0 = 0.0F, headU1 = 0.15625F;
        float headV0 = 5.0F / 32.0F, headV1 = 10.0F / 32.0F;
        float shaftU0 = 0.0F, shaftU1 = 0.5F;
        float shaftV0 = 0.0F, shaftV1 = 5.0F / 32.0F;
        float sc = 0.05625F;

        float shake = (float) arrow.arrowShake - partialTick;
        if (shake > 0.0F) {
            float wobble = -MathHelper.sin(shake * 3.0F) * shake;
            GL11.glRotatef(wobble, 0.0F, 0.0F, 1.0F);
        }

        GL11.glRotatef(45.0F, 1.0F, 0.0F, 0.0F);
        GL11.glScalef(sc, sc, sc);
        GL11.glTranslatef(-4.0F, 0.0F, 0.0F);

        GL11.glNormal3f(sc, 0.0F, 0.0F);
        tess.startDrawingQuads();
        tess.addVertexWithUV(-7.0D, -2.0D, -2.0D, headU0, headV1);
        tess.addVertexWithUV(-7.0D, -2.0D, 2.0D, headU1, headV1);
        tess.addVertexWithUV(-7.0D, 2.0D, 2.0D, headU1, headV0);
        tess.addVertexWithUV(-7.0D, 2.0D, -2.0D, headU0, headV0);
        tess.draw();

        GL11.glNormal3f(-sc, 0.0F, 0.0F);
        tess.startDrawingQuads();
        tess.addVertexWithUV(-7.0D, 2.0D, -2.0D, headU0, headV1);
        tess.addVertexWithUV(-7.0D, 2.0D, 2.0D, headU1, headV1);
        tess.addVertexWithUV(-7.0D, -2.0D, 2.0D, headU1, headV0);
        tess.addVertexWithUV(-7.0D, -2.0D, -2.0D, headU0, headV0);
        tess.draw();

        for (int i = 0; i < 4; ++i) {
            GL11.glRotatef(90.0F, 1.0F, 0.0F, 0.0F);
            GL11.glNormal3f(0.0F, 0.0F, sc);
            tess.startDrawingQuads();
            tess.addVertexWithUV(-8.0D, -2.0D, 0.0D, shaftU0, shaftV0);
            tess.addVertexWithUV(8.0D, -2.0D, 0.0D, shaftU1, shaftV0);
            tess.addVertexWithUV(8.0D, 2.0D, 0.0D, shaftU1, shaftV1);
            tess.addVertexWithUV(-8.0D, 2.0D, 0.0D, shaftU0, shaftV1);
            tess.draw();
        }

        GL11.glPopMatrix();
    }

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTick) {
        this.renderArrow((EntityArrow) entity, x, y, z, yaw, partialTick);
    }
}
