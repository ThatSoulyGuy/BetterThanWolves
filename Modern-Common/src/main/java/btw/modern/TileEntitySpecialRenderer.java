package btw.modern;

public abstract class TileEntitySpecialRenderer {

    public TileEntityRenderer tileEntityRenderer;

    public abstract void renderTileEntityAt(TileEntity tileEntity, double x, double y, double z, float partialTickTime);

    public void bindTextureByName(String textureName) {
        if (this.tileEntityRenderer != null && this.tileEntityRenderer.renderEngine != null) {
            this.tileEntityRenderer.renderEngine.bindTexture(textureName);
        }
    }

    public void bindTextureByURL(String url, String fallback) {}

    public void setTileEntityRenderer(TileEntityRenderer renderer) {
        this.tileEntityRenderer = renderer;
    }

    public void onWorldChange(World world) {}

    public FontRenderer getFontRenderer() {
        return this.tileEntityRenderer != null ? this.tileEntityRenderer.getFontRenderer() : null;
    }
}
