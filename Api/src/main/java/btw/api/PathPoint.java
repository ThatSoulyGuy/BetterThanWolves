package btw.api;

public class PathPoint {
    public final int xCoord;
    public final int yCoord;
    public final int zCoord;
    public int hash;
    public int index = -1;
    public float totalPathDistance;
    public float distanceToNext;
    public float distanceToTarget;
    public PathPoint previous;
    public boolean isFirst;

    public PathPoint(int x, int y, int z) {
        this.xCoord = x;
        this.yCoord = y;
        this.zCoord = z;
        this.hash = makeHash(x, y, z);
    }

    public static int makeHash(int x, int y, int z) {
        return y & 255 | (x & 32767) << 8 | (z & 32767) << 24;
    }

    public float distanceTo(PathPoint point) { return 0.0F; }
    public float distanceToSquared(PathPoint point) { return 0.0F; }

    public boolean equals(Object obj) {
        if (!(obj instanceof PathPoint)) return false;
        PathPoint other = (PathPoint) obj;
        return this.hash == other.hash && this.xCoord == other.xCoord && this.yCoord == other.yCoord && this.zCoord == other.zCoord;
    }

    public int hashCode() { return this.hash; }
    public boolean isAssigned() { return this.index >= 0; }
}
