package btw.modern;

public class Packet53BlockChange extends Packet {
    public int xPosition;
    public int yPosition;
    public int zPosition;
    public int type;
    public int metadata;

    public Packet53BlockChange() {}
    public Packet53BlockChange(int x, int y, int z, World world) {
        this.xPosition = x;
        this.yPosition = y;
        this.zPosition = z;
    }

    public int getPacketSize() { return 0; }
}
