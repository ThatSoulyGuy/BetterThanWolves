package btw.api;

public class ChunkCoordIntPair {
    public final int chunkXPos;
    public final int chunkZPos;

    public ChunkCoordIntPair(int x, int z) {
        this.chunkXPos = x;
        this.chunkZPos = z;
    }

    public static long chunkXZ2Int(int x, int z) {
        return (long) x & 0xFFFFFFFFL | ((long) z & 0xFFFFFFFFL) << 32;
    }

    public int hashCode() {
        long val = chunkXZ2Int(this.chunkXPos, this.chunkZPos);
        int hash = (int) val;
        return hash ^ (int)(val >> 32);
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ChunkCoordIntPair)) return false;
        ChunkCoordIntPair other = (ChunkCoordIntPair) obj;
        return this.chunkXPos == other.chunkXPos && this.chunkZPos == other.chunkZPos;
    }

    public int getCenterXPos() { return (this.chunkXPos << 4) + 8; }
    public int getCenterZPosition() { return (this.chunkZPos << 4) + 8; }
}
