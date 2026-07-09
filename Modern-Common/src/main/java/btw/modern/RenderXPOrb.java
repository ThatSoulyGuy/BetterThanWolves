package btw.modern;

/**
 * 1.5.2 RenderXPOrb (vanilla/client RenderXPOrb.java) — the experience-orb renderer.
 *
 * <p>Registered in the {@link RenderManager} shim ctor for the frozen vanilla
 * {@code EntityXPOrb} (the {@code fc_xp_orb} entity). FC's
 * {@code ClientAddEntityRenderers} does NOT register it — vanilla 1.5.2 relied on the
 * base {@code RenderManager} ctor, which this shim only partially replicated (it
 * registered {@code RenderItem} alone). Without this, {@code FCEntityRenderer} found no
 * FC {@link Render} for XP orbs and fell back to the debug box, so orbs were invisible.</p>
 *
 * <p>Adaptations from vanilla, matching the {@link RenderFallingSand} port: the
 * {@code getBrightnessForRender}/{@code OpenGlHelper} lightmap calls are dropped
 * (lighting comes from MC 1.20.1's pipeline via packedLight), and GL state toggles
 * ({@code GL_BLEND}, {@code RESCALE_NORMAL}) are shim no-ops. The billboard uses
 * {@code RenderManager.playerViewY/X}, which the capture pipeline leaves at 0, so the
 * quad faces a fixed direction rather than tracking the camera — it renders correctly
 * placed and textured; true camera-billboarding is a follow-up if desired.</p>
 */
public class RenderXPOrb extends Render {

    public RenderXPOrb() {
        this.shadowSize = 0.15F;
        this.shadowOpaque = 0.75F;
    }

    public void renderTheXPOrb(EntityXPOrb orb, double x, double y, double z, float yaw, float partialTick) {
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x, (float) y, (float) z);

        int frame = orb.getTextureByXP();
        this.loadTexture("/item/xporb.png");

        Tessellator tess = Tessellator.instance;
        float u0 = (float) (frame % 4 * 16) / 64.0F;
        float u1 = (float) (frame % 4 * 16 + 16) / 64.0F;
        float v0 = (float) (frame / 4 * 16) / 64.0F;
        float v1 = (float) (frame / 4 * 16 + 16) / 64.0F;
        float size = 1.0F;
        float offX = 0.5F;
        float offY = 0.25F;

        float colorPhase = ((float) orb.xpColor + partialTick) / 2.0F;
        float scale = 255.0F;
        int red = (int) ((MathHelper.sin(colorPhase) + 1.0F) * 0.5F * scale);
        int green = (int) scale;
        int blue = (int) ((MathHelper.sin(colorPhase + 4.1887903F) + 1.0F) * 0.1F * scale);

        // FCMOD: soul-bound (not-player-owned) orbs pulse dim blue instead of green/yellow
        if (orb.m_bNotPlayerOwned) {
            red = (int) ((MathHelper.sin(colorPhase / 10.0F) + 1.0F) * 0.5F * scale);
            green = 0;
            blue = 0;
            red = (red >> 2) + 64;
        }

        int color = red << 16 | green << 8 | blue;

        float viewY = this.renderManager != null ? this.renderManager.playerViewY : 0.0F;
        float viewX = this.renderManager != null ? this.renderManager.playerViewX : 0.0F;
        GL11.glRotatef(180.0F - viewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-viewX, 1.0F, 0.0F, 0.0F);

        float s = 0.3F;
        GL11.glScalef(s, s, s);

        tess.startDrawingQuads();
        tess.setColorRGBA_I(color, 128);
        tess.setNormal(0.0F, 1.0F, 0.0F);
        tess.addVertexWithUV(0.0F - offX, 0.0F - offY, 0.0D, u0, v1);
        tess.addVertexWithUV(size - offX, 0.0F - offY, 0.0D, u1, v1);
        tess.addVertexWithUV(size - offX, 1.0F - offY, 0.0D, u1, v0);
        tess.addVertexWithUV(0.0F - offX, 1.0F - offY, 0.0D, u0, v0);
        tess.draw();

        GL11.glPopMatrix();
    }

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTick) {
        this.renderTheXPOrb((EntityXPOrb) entity, x, y, z, yaw, partialTick);
    }
}
