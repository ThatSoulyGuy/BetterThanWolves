package btw.modern;

/**
 * 1.5.2 RenderSnowball (vanilla/client RenderSnowball.java) — renders a thrown item as a
 * camera-facing icon billboard. Used for the frozen vanilla {@code EntitySnowball}
 * ({@code fc_snowball}) and {@code EntityEgg} ({@code fc_egg}); constructed with the item
 * whose icon to draw. Mirrors {@link RenderItem}'s icon path exactly (loadTexture
 * "/gui/items.png" + the item's {@link Icon} UVs), which is already proven to render in
 * the capture pipeline.
 *
 * <p>Adaptation: the splash-potion tint branch is omitted — {@code fc_snowball}/{@code
 * fc_egg} aren't potions, and FC potions render via their own path. The billboard uses
 * {@code RenderManager.playerViewY/X}, left at 0 by the capture pipeline, so it faces a
 * fixed direction rather than tracking the camera.</p>
 */
public class RenderSnowball extends Render {

    private final Item item;
    private final int meta;

    public RenderSnowball(Item item, int meta) {
        this.item = item;
        this.meta = meta;
    }

    public RenderSnowball(Item item) {
        this(item, 0);
    }

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTick) {
        if (this.item == null) return;
        Icon icon = this.item.getIconFromDamage(this.meta);
        if (icon == null) return;

        GL11.glPushMatrix();
        GL11.glTranslatef((float) x, (float) y, (float) z);
        GL11.glScalef(0.5F, 0.5F, 0.5F);
        this.loadTexture("/gui/items.png");
        renderIconBillboard(Tessellator.instance, icon);
        GL11.glPopMatrix();
    }

    private void renderIconBillboard(Tessellator tess, Icon icon) {
        float u0 = icon.getMinU();
        float u1 = icon.getMaxU();
        float v0 = icon.getMinV();
        float v1 = icon.getMaxV();
        float size = 1.0F;
        float ox = 0.5F;
        float oy = 0.25F;

        float viewY = this.renderManager != null ? this.renderManager.playerViewY : 0.0F;
        float viewX = this.renderManager != null ? this.renderManager.playerViewX : 0.0F;
        GL11.glRotatef(180.0F - viewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-viewX, 1.0F, 0.0F, 0.0F);

        tess.startDrawingQuads();
        tess.setNormal(0.0F, 1.0F, 0.0F);
        tess.addVertexWithUV(0.0F - ox, 0.0F - oy, 0.0D, u0, v1);
        tess.addVertexWithUV(size - ox, 0.0F - oy, 0.0D, u1, v1);
        tess.addVertexWithUV(size - ox, size - oy, 0.0D, u1, v0);
        tess.addVertexWithUV(0.0F - ox, size - oy, 0.0D, u0, v0);
        tess.draw();
    }
}
