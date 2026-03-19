package btw.modern;

/**
 * Pool for Vec3 instances (optimizes allocation).
 * Mirrors net.minecraft.src.Vec3Pool.
 */
public class Vec3Pool {

    public Vec3 getVecFromPool(double x, double y, double z) {
        return Vec3.createVectorHelper(x, y, z);
    }
}
