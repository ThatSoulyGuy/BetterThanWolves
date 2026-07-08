package btw.modern;

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

    // 1.5.2 ChunkCoordinates.set — frozen EntityLiving.setHomeArea calls it
    public void set(int x, int y, int z) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
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
        ChunkCoordinates other = (ChunkCoordinates) obj;
        if (this.posY == other.posY) {
            return this.posZ == other.posZ ? this.posX - other.posX : this.posZ - other.posZ;
        }
        return this.posY - other.posY;
    }

    public float getDistanceSquared(int x, int y, int z) {
        float dx = this.posX - x;
        float dy = this.posY - y;
        float dz = this.posZ - z;
        return dx * dx + dy * dy + dz * dz;
    }

    public float getDistanceSquaredToChunkCoordinates(ChunkCoordinates other) {
        return getDistanceSquared(other.posX, other.posY, other.posZ);
    }
}
