package btw.api;

public class ChunkCoordinates implements Comparable {
    public int posX;
    public int posY;
    public int posZ;

    public ChunkCoordinates() {}

    public ChunkCoordinates(int x, int y, int z) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
    }

    public ChunkCoordinates(ChunkCoordinates other) {
        this.posX = other.posX;
        this.posY = other.posY;
        this.posZ = other.posZ;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ChunkCoordinates)) return false;
        ChunkCoordinates other = (ChunkCoordinates) obj;
        return this.posX == other.posX && this.posY == other.posY && this.posZ == other.posZ;
    }

    public int hashCode() {
        return this.posX + this.posZ << 8 + this.posY << 16;
    }

    public int compareTo(Object obj) {
        return compareTo((ChunkCoordinates) obj);
    }

    public int compareTo(ChunkCoordinates other) {
        if (this.posY == other.posY) {
            return this.posZ == other.posZ ? this.posX - other.posX : this.posZ - other.posZ;
        }
        return this.posY - other.posY;
    }

    public double getDistanceSquared(int x, int y, int z) {
        double dx = this.posX - x;
        double dy = this.posY - y;
        double dz = this.posZ - z;
        return dx * dx + dy * dy + dz * dz;
    }

    public double getDistanceSquaredToChunkCoordinates(ChunkCoordinates other) {
        return getDistanceSquared(other.posX, other.posY, other.posZ);
    }
}
