package btw.api;

public class MapData {
    public int xCenter;
    public int zCenter;
    public byte dimension;
    public byte scale;
    public byte[] colors = new byte[16384];

    public MapData(String name) {}

    public void updateVisiblePlayers(EntityPlayer player, ItemStack stack) {}
    public void updateMPData(byte[] data) {}
    public void markDirty() {}
    public boolean IsEntityLocationVisibleOnMap(Entity entity) { return false; }
}
