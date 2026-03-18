package btw.api;

public class ChunkPosition {
    public final int x;
    public final int y;
    public final int z;

    public ChunkPosition(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ChunkPosition)) return false;
        ChunkPosition other = (ChunkPosition) obj;
        return this.x == other.x && this.y == other.y && this.z == other.z;
    }

    public int hashCode() {
        return this.x * 8976890 + this.y * 981131 + this.z;
    }
}
