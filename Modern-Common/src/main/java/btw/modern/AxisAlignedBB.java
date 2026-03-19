package btw.modern;

import java.util.List;

public class AxisAlignedBB extends FCUtilsPrimitiveGeometric {

    public double minX;
    public double minY;
    public double minZ;
    public double maxX;
    public double maxY;
    public double maxZ;

    private static final AABBPool thePool = new AABBPool();

    public static AABBPool getAABBPool() {
        return thePool;
    }

    public AxisAlignedBB(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public static AxisAlignedBB getBoundingBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public AxisAlignedBB setBounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        return this;
    }

    public AxisAlignedBB expand(double x, double y, double z) {
        return new AxisAlignedBB(
            this.minX - x, this.minY - y, this.minZ - z,
            this.maxX + x, this.maxY + y, this.maxZ + z
        );
    }

    public AxisAlignedBB offset(double x, double y, double z) {
        return new AxisAlignedBB(
            this.minX + x, this.minY + y, this.minZ + z,
            this.maxX + x, this.maxY + y, this.maxZ + z
        );
    }

    public boolean intersectsWith(AxisAlignedBB other) {
        return other.maxX > this.minX && other.minX < this.maxX
            && other.maxY > this.minY && other.minY < this.maxY
            && other.maxZ > this.minZ && other.minZ < this.maxZ;
    }

    public AxisAlignedBB copy() {
        return new AxisAlignedBB(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
    }

    public AxisAlignedBB addCoord(double x, double y, double z) {
        double newMinX = this.minX;
        double newMinY = this.minY;
        double newMinZ = this.minZ;
        double newMaxX = this.maxX;
        double newMaxY = this.maxY;
        double newMaxZ = this.maxZ;

        if (x < 0.0D) newMinX += x; else newMaxX += x;
        if (y < 0.0D) newMinY += y; else newMaxY += y;
        if (z < 0.0D) newMinZ += z; else newMaxZ += z;

        return new AxisAlignedBB(newMinX, newMinY, newMinZ, newMaxX, newMaxY, newMaxZ);
    }

    public boolean isVecInside(Vec3 vec) {
        return vec.xCoord > this.minX && vec.xCoord < this.maxX
            && vec.yCoord > this.minY && vec.yCoord < this.maxY
            && vec.zCoord > this.minZ && vec.zCoord < this.maxZ;
    }

    public AxisAlignedBB contract(double x, double y, double z) {
        return new AxisAlignedBB(
            this.minX + x, this.minY + y, this.minZ + z,
            this.maxX - x, this.maxY - y, this.maxZ - z
        );
    }

    public void AddToListIfIntersects(AxisAlignedBB intersectingBox, List list) {
        if (this.intersectsWith(intersectingBox)) {
            list.add(this);
        }
    }

    public double calculateXOffset(AxisAlignedBB other, double offset) { return offset; }
    public double calculateYOffset(AxisAlignedBB other, double offset) { return offset; }
    public double calculateZOffset(AxisAlignedBB other, double offset) { return offset; }

    public AxisAlignedBB getOffsetBoundingBox(double x, double y, double z) {
        return new AxisAlignedBB(
            this.minX + x, this.minY + y, this.minZ + z,
            this.maxX + x, this.maxY + y, this.maxZ + z
        );
    }

    public MovingObjectPosition calculateIntercept(Vec3 start, Vec3 end) {
        return null;
    }

    public void RotateAroundJToFacing(int facing) {
    }

    public void TiltToFacingAlongJ(int facing) {
    }

    public AxisAlignedBB MakeTemporaryCopy() {
        return this.copy();
    }

    public void AddToRayTrace(Object rayTrace) {}

    public void Translate(double dDeltaX, double dDeltaY, double dDeltaZ) {
        this.minX += dDeltaX;
        this.minY += dDeltaY;
        this.minZ += dDeltaZ;
        this.maxX += dDeltaX;
        this.maxY += dDeltaY;
        this.maxZ += dDeltaZ;
    }

    public void ExpandToInclude(AxisAlignedBB other) {
        if (other.minX < this.minX) this.minX = other.minX;
        if (other.minY < this.minY) this.minY = other.minY;
        if (other.minZ < this.minZ) this.minZ = other.minZ;
        if (other.maxX > this.maxX) this.maxX = other.maxX;
        if (other.maxY > this.maxY) this.maxY = other.maxY;
        if (other.maxZ > this.maxZ) this.maxZ = other.maxZ;
    }
}
