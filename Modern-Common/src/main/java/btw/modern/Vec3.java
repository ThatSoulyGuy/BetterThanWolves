package btw.modern;

public class Vec3 {

    public double xCoord;
    public double yCoord;
    public double zCoord;

    public Vec3(double x, double y, double z) {
        this.xCoord = x;
        this.yCoord = y;
        this.zCoord = z;
    }

    public static Vec3 createVectorHelper(double x, double y, double z) {
        return new Vec3(x, y, z);
    }

    public static Vec3 createVectorHelper(Vec3 copyVector) {
        return new Vec3(copyVector.xCoord, copyVector.yCoord, copyVector.zCoord);
    }

    public Vec3 normalize() {
        double len = Math.sqrt(xCoord * xCoord + yCoord * yCoord + zCoord * zCoord);
        if (len < 1.0E-4D) {
            return new Vec3(0.0D, 0.0D, 0.0D);
        }
        return new Vec3(xCoord / len, yCoord / len, zCoord / len);
    }

    public double lengthVector() {
        return Math.sqrt(xCoord * xCoord + yCoord * yCoord + zCoord * zCoord);
    }

    public double dotProduct(Vec3 other) {
        return this.xCoord * other.xCoord + this.yCoord * other.yCoord + this.zCoord * other.zCoord;
    }

    public Vec3 crossProduct(Vec3 other) {
        return new Vec3(
            this.yCoord * other.zCoord - this.zCoord * other.yCoord,
            this.zCoord * other.xCoord - this.xCoord * other.zCoord,
            this.xCoord * other.yCoord - this.yCoord * other.xCoord
        );
    }

    public Vec3 addVector(double x, double y, double z) {
        return new Vec3(this.xCoord + x, this.yCoord + y, this.zCoord + z);
    }

    public double distanceTo(Vec3 other) {
        double dx = other.xCoord - this.xCoord;
        double dy = other.yCoord - this.yCoord;
        double dz = other.zCoord - this.zCoord;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public double squareDistanceTo(Vec3 other) {
        double dx = other.xCoord - this.xCoord;
        double dy = other.yCoord - this.yCoord;
        double dz = other.zCoord - this.zCoord;
        return dx * dx + dy * dy + dz * dz;
    }

    public Vec3 setComponents(double x, double y, double z) {
        this.xCoord = x;
        this.yCoord = y;
        this.zCoord = z;
        return this;
    }

    public void rotateAroundX(float angle) {
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        double newY = this.yCoord * cos + this.zCoord * sin;
        double newZ = this.zCoord * cos - this.yCoord * sin;
        this.yCoord = newY;
        this.zCoord = newZ;
    }

    public void rotateAroundY(float angle) {
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        double newX = this.xCoord * cos + this.zCoord * sin;
        double newZ = this.zCoord * cos - this.xCoord * sin;
        this.xCoord = newX;
        this.zCoord = newZ;
    }

    public Vec3 getIntermediateWithXValue(Vec3 other, double x) {
        double dx = other.xCoord - this.xCoord;
        double dy = other.yCoord - this.yCoord;
        double dz = other.zCoord - this.zCoord;
        if (dx * dx < 1.0E-7D) return null;
        double ratio = (x - this.xCoord) / dx;
        if (ratio < 0.0D || ratio > 1.0D) return null;
        return new Vec3(this.xCoord + dx * ratio, this.yCoord + dy * ratio, this.zCoord + dz * ratio);
    }

    public Vec3 getIntermediateWithYValue(Vec3 other, double y) {
        double dx = other.xCoord - this.xCoord;
        double dy = other.yCoord - this.yCoord;
        double dz = other.zCoord - this.zCoord;
        if (dy * dy < 1.0E-7D) return null;
        double ratio = (y - this.yCoord) / dy;
        if (ratio < 0.0D || ratio > 1.0D) return null;
        return new Vec3(this.xCoord + dx * ratio, this.yCoord + dy * ratio, this.zCoord + dz * ratio);
    }

    public Vec3 getIntermediateWithZValue(Vec3 other, double z) {
        double dx = other.xCoord - this.xCoord;
        double dy = other.yCoord - this.yCoord;
        double dz = other.zCoord - this.zCoord;
        if (dz * dz < 1.0E-7D) return null;
        double ratio = (z - this.zCoord) / dz;
        if (ratio < 0.0D || ratio > 1.0D) return null;
        return new Vec3(this.xCoord + dx * ratio, this.yCoord + dy * ratio, this.zCoord + dz * ratio);
    }

    public Vec3 SubtractFrom(Vec3 other) {
        return new Vec3(other.xCoord - this.xCoord, other.yCoord - this.yCoord, other.zCoord - this.zCoord);
    }

    public Vec3 AddVector(Vec3 other) {
        return new Vec3(this.xCoord + other.xCoord, this.yCoord + other.yCoord, this.zCoord + other.zCoord);
    }

    public Vec3 MakeTemporaryCopy() {
        return new Vec3(this.xCoord, this.yCoord, this.zCoord);
    }

    public Vec3 RotateAroundJToFacing(int facing) {
        // BTW rotation around J (Y) axis to facing
        return new Vec3(this.xCoord, this.yCoord, this.zCoord);
    }

    public void RotateAsBlockPosAroundJToFacing(int facing) {
    }

    public void RotateAsVectorAroundJToFacing(int facing) {
    }

    public void TiltAsBlockPosToFacingAlongJ(int facing) {
    }

    public Vec3 TiltToFacingAlongJ(int facing) {
        return new Vec3(this.xCoord, this.yCoord, this.zCoord);
    }

    public static Vec3 getVecFromPool(double x, double y, double z) {
        return new Vec3(x, y, z);
    }

    @Override
    public String toString() {
        return "(" + this.xCoord + ", " + this.yCoord + ", " + this.zCoord + ")";
    }
}
