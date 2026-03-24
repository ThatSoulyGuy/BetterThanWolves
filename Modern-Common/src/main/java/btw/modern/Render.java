package btw.modern;

public abstract class Render {

    protected RenderManager renderManager;
    protected RenderBlocks renderBlocks = new RenderBlocks();
    protected float shadowSize = 0.0F;
    protected float shadowOpaque = 1.0F;

    /**
     * Actually renders the given argument.
     */
    public abstract void doRender(Entity entity, double x, double y, double z, float yaw, float partialTickTime);

    /**
     * Loads the specified texture.
     */
    protected void loadTexture(String textureName) {
        // Always record texture on Tessellator for the capture pipeline,
        // even if renderManager is null (FC renderers created standalone).
        Tessellator.instance.setCurrentTextureName(textureName);
        if (this.renderManager != null && this.renderManager.renderEngine != null) {
            this.renderManager.renderEngine.bindTexture(textureName);
        }
    }

    protected boolean loadDownloadableImageTexture(String url, String fallback) {
        return false;
    }

    public void setRenderManager(RenderManager renderManager) {
        this.renderManager = renderManager;
    }

    public void doRenderShadowAndFire(Entity entity, double x, double y, double z, float yaw, float partialTickTime) {}

    public FontRenderer getFontRendererFromRenderManager() {
        return this.renderManager.getFontRenderer();
    }

    public void updateIcons(IconRegister iconRegister) {}

    public static void renderOffsetAABB(AxisAlignedBB aabb, double x, double y, double z) {}

    public static void renderAABB(AxisAlignedBB aabb) {}
}
