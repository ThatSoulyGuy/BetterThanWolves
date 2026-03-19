package btw.modern;

import java.util.List;

public class TextureStitched implements Icon {

    private final String textureName;
    public Texture textureSheet;
    public List textureList;
    public boolean rotated;
    public int originX;
    public int originY;
    private int width;
    private int height;
    private float minU;
    private float maxU;
    private float minV;
    private float maxV;
    public int frameCounter = 0;
    public int tickCounter = 0;

    public static TextureStitched makeTextureStitched(String name) {
        return new TextureStitched(name);
    }

    public TextureStitched(String name) {
        this.textureName = name;
    }

    public void init(Texture texture, List textures, int originX, int originY, int width, int height, boolean rotated) {
        this.textureSheet = texture;
        this.textureList = textures;
        this.originX = originX;
        this.originY = originY;
        this.width = width;
        this.height = height;
        this.rotated = rotated;
    }

    public void copyFrom(TextureStitched other) {
        this.init(other.textureSheet, other.textureList, other.originX, other.originY, other.width, other.height, other.rotated);
    }

    public int getOriginX() { return this.originX; }
    public int getOriginY() { return this.originY; }
    public float getMinU() { return this.minU; }
    public float getMaxU() { return this.maxU; }
    public float getInterpolatedU(double d) { return this.minU + (this.maxU - this.minU) * ((float) d / 16.0F); }
    public float getMinV() { return this.minV; }
    public float getMaxV() { return this.maxV; }
    public float getInterpolatedV(double d) { return this.minV + (this.maxV - this.minV) * ((float) d / 16.0F); }
    public String getIconName() { return this.textureName; }
    public int getSheetWidth() { return this.textureSheet != null ? this.textureSheet.getWidth() : 0; }
    public int getSheetHeight() { return this.textureSheet != null ? this.textureSheet.getHeight() : 0; }
    public int getIconWidth() { return this.width; }
    public int getIconHeight() { return this.height; }

    public void updateAnimation() {}

    public boolean IsProcedurallyAnimated() {
        return false;
    }
}
