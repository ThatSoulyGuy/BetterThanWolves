package btw.modern;

import java.nio.ByteBuffer;

public class Texture {

    private int glTextureId = -1;
    private int textureId;
    private final int width;
    private final int height;
    private final String textureName;
    private ByteBuffer textureData;

    public Texture(String name, int width, int height) {
        this.textureName = name;
        this.width = width;
        this.height = height;
    }

    public String getTextureName() {
        return this.textureName;
    }

    public int getTextureId() {
        return this.textureId;
    }

    public int getGlTextureId() {
        return this.glTextureId;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public ByteBuffer getTextureData() {
        return this.textureData;
    }

    public void bindTexture(int unit) {}

    public void uploadTexture() {}

    public void func_104062_b(int x, int y, Texture texture) {}

    public void UploadByteBufferToGPU(int destX, int destY, ByteBuffer buffer, int bufferWidth, int bufferHeight) {}
}
