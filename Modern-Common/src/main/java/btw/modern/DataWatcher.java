package btw.modern;

/**
 * Watches data values on entities and syncs them to clients.
 * Mirrors net.minecraft.src.DataWatcher.
 */
public class DataWatcher {

    public DataWatcher() {}

    public void addObject(int id, Object value) {}

    public void updateObject(int id, Object value) {}

    public byte getWatchableObjectByte(int id) { return 0; }

    public short getWatchableObjectShort(int id) { return 0; }

    public int getWatchableObjectInt(int id) { return 0; }

    public float getWatchableObjectFloat(int id) { return 0.0F; }

    public String getWatchableObjectString(int id) { return ""; }

    public ItemStack getWatchableObjectItemStack(int id) { return null; }

    public void setObjectWatched(int id) {}

    public boolean hasChanges() { return false; }

    public java.util.List getChanged() { return null; }

    public java.util.List getAllWatched() { return null; }
}
