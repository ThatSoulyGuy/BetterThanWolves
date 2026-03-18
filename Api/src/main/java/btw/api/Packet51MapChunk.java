package btw.api;

public class Packet51MapChunk extends Packet {
    public int xCh;
    public int zCh;
    public int yChMin;
    public int yChMax;
    public byte[] chunkData;
    public boolean includeInitialize;

    public Packet51MapChunk() {}
    public Packet51MapChunk(Chunk chunk, boolean init, int yFilter) {}

    public int getPacketSize() { return 0; }
}
