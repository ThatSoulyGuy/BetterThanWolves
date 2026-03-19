package btw.modern;

public abstract class WorldSavedData {
    public String mapName;
    public boolean dirty;

    public WorldSavedData(String name) {
        this.mapName = name;
    }

    public abstract void readFromNBT(NBTTagCompound compound);
    public abstract void writeToNBT(NBTTagCompound compound);
    public void markDirty() { this.dirty = true; }
    public void setDirty(boolean dirty) { this.dirty = dirty; }
    public boolean isDirty() { return this.dirty; }
}
