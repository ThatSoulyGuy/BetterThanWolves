package btw.api;

public class RenderEngine {

    public void bindTexture(String textureName) {}

    public void resetBoundTexture() {}

    public int getTextureForDownloadableImage(String url, String fallback) {
        return -1;
    }

    public int[] getTextureContents(String textureName) {
        return new int[0];
    }

    public int allocateAndSetupTexture(Object bufferedImage) {
        return 0;
    }

    public void deleteTexture(int textureId) {}

    public void refreshTextures() {}

    public void updateDynamicTextures() {}

    public Icon getMissingIcon(int type) {
        return null;
    }
}
