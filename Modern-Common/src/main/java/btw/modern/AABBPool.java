package btw.modern;

public class AABBPool {
    public AxisAlignedBB getAABB(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public int getlistAABBsize() { return 0; }
    public int getnextPoolIndex() { return 0; }
    public void cleanPool() {}
}
