package btw.modern;

/**
 * Vanilla 1.5.2 AABB pool. Used by FC's vanilla AxisAlignedBB to recycle
 * AABB instances. The (int, int) constructor is what vanilla AABBLocalPool
 * calls — its absence used to throw NoSuchMethodError on AABB class init.
 */
public class AABBPool {
    public AABBPool() {}

    /**
     * Vanilla constructor: maxNumCleans, numEntriesToRemove. We don't
     * actually pool — every getAABB returns a fresh box — so the args
     * are ignored, but the constructor must exist for vanilla AABB's
     * static initializer chain.
     */
    public AABBPool(int maxNumCleans, int numEntriesToRemove) {}

    public AxisAlignedBB getAABB(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public int getlistAABBsize() { return 0; }
    public int getnextPoolIndex() { return 0; }
    public void cleanPool() {}
    public void clearPool() {}
}
