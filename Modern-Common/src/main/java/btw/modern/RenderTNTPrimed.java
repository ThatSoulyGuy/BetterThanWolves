package btw.modern;

/**
 * 1.5.2 RenderTNTPrimed (vanilla/client RenderTNTPrimed.java) — primed-TNT renderer for
 * the frozen vanilla {@code EntityTNTPrimed} ({@code fc_tnt_primed}). Renders the TNT
 * block through {@link RenderBlocks#renderBlockAsItem} (same capture path as
 * {@link RenderFallingSand}); texture {@code /terrain.png} recorded via
 * {@link Render#loadTexture}. The block swells as the fuse runs out.
 *
 * <p>Adaptation: the fuse-blink white overlay (the second renderBlockAsItem under
 * GL_BLEND) is omitted — it's a blended GL-state effect the capture pipeline doesn't
 * reproduce; the TNT block itself renders and swells.</p>
 */
public class RenderTNTPrimed extends Render {

    private final RenderBlocks blockRenderer = new RenderBlocks();

    public RenderTNTPrimed() {
        this.shadowSize = 0.5F;
    }

    public void renderPrimedTNT(EntityTNTPrimed tnt, double x, double y, double z, float yaw, float partialTick) {
        if (Block.tnt == null) return;

        GL11.glPushMatrix();
        GL11.glTranslatef((float) x, (float) y, (float) z);

        if ((float) tnt.fuse - partialTick + 1.0F < 10.0F) {
            float t = 1.0F - ((float) tnt.fuse - partialTick + 1.0F) / 10.0F;
            if (t < 0.0F) t = 0.0F;
            if (t > 1.0F) t = 1.0F;
            t *= t;
            t *= t;
            float s = 1.0F + t * 0.3F;
            GL11.glScalef(s, s, s);
        }

        this.loadTexture("/terrain.png");
        this.blockRenderer.renderBlockAsItem(Block.tnt, 0, tnt.getBrightness(partialTick));

        GL11.glPopMatrix();
    }

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTick) {
        this.renderPrimedTNT((EntityTNTPrimed) entity, x, y, z, yaw, partialTick);
    }
}
