package btw.api;

public class MapData extends WorldSavedData {
    public int xCenter;
    public int zCenter;
    public byte dimension;
    public byte scale;
    public byte[] colors = new byte[16384];

    public MapData(String name) { super(name); }

    public void readFromNBT(NBTTagCompound compound) {}
    public void writeToNBT(NBTTagCompound compound) {}

    public void updateVisiblePlayers(EntityPlayer player, ItemStack stack) {}
    public void updateMPData(byte[] data) {}
    public void markDirty() {}
    public boolean IsEntityLocationVisibleOnMap(Entity entity) { return false; }
}
