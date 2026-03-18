package btw.api;

public class Packet132TileEntityData extends Packet {
    public int xPosition;
    public int yPosition;
    public int zPosition;
    public int actionType;
    public NBTTagCompound customParam1;

    public Packet132TileEntityData() {}
    public Packet132TileEntityData(int x, int y, int z, int action, NBTTagCompound tag) {
        this.xPosition = x;
        this.yPosition = y;
        this.zPosition = z;
        this.actionType = action;
        this.customParam1 = tag;
    }

    public int getPacketSize() { return 0; }
}
