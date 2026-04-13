package btw.modern;

/**
 * Compile-time stub — replaced at runtime by real 1.5.2 class via remap.
 */
public class PositionTextureVertex {
    public Vec3 vector3D;
    public float texturePositionX;
    public float texturePositionY;

    public PositionTextureVertex(float x, float y, float z, float u, float v) {
        this(Vec3.createVectorHelper(x, y, z), u, v);
    }

    public PositionTextureVertex(Vec3 vec, float u, float v) {
        this.vector3D = vec;
        this.texturePositionX = u;
        this.texturePositionY = v;
    }

    public PositionTextureVertex(PositionTextureVertex other, float u, float v) {
        this.vector3D = other.vector3D;
        this.texturePositionX = u;
        this.texturePositionY = v;
    }

    public PositionTextureVertex setTexturePosition(float u, float v) {
        return new PositionTextureVertex(this, u, v);
    }
}
