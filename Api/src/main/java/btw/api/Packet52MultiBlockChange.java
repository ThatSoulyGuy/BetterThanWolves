package btw.api;

public class Packet52MultiBlockChange extends Packet {
    public int xPosition;
    public int zPosition;
    public int size;
    public byte[] metadataArray;

    public Packet52MultiBlockChange() {}
    public Packet52MultiBlockChange(int chunkX, int chunkZ, short[] blockPositions, int size, World world) {}

    public int getPacketSize() { return 0; }
}
