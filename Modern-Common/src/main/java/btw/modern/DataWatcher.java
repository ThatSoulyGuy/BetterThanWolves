package btw.modern;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores watched data values on entities.
 *
 * <p>In vanilla MC 1.5.2, DataWatcher synced values to clients via
 * entity metadata packets. In the Forge 1.20.1 bridge, FC penalty
 * levels are synced via a dedicated BTWNetwork packet instead.
 * But FC code still reads/writes penalty levels through DataWatcher
 * getters/setters, so storage must work correctly.</p>
 */
public class DataWatcher {

    private final Map<Integer, Object> data = new HashMap<>();

    public DataWatcher() {}

    public void addObject(int id, Object value) {
        data.put(id, value);
    }

    public void updateObject(int id, Object value) {
        data.put(id, value);
    }

    public byte getWatchableObjectByte(int id) {
        Object val = data.get(id);
        if (val instanceof Byte b) return b;
        if (val instanceof Number n) return n.byteValue();
        return 0;
    }

    public short getWatchableObjectShort(int id) {
        Object val = data.get(id);
        if (val instanceof Short s) return s;
        if (val instanceof Number n) return n.shortValue();
        return 0;
    }

    public int getWatchableObjectInt(int id) {
        Object val = data.get(id);
        if (val instanceof Integer i) return i;
        if (val instanceof Number n) return n.intValue();
        return 0;
    }

    public float getWatchableObjectFloat(int id) {
        Object val = data.get(id);
        if (val instanceof Float f) return f;
        if (val instanceof Number n) return n.floatValue();
        return 0.0F;
    }

    public String getWatchableObjectString(int id) {
        Object val = data.get(id);
        return val instanceof String s ? s : "";
    }

    public ItemStack getWatchableObjectItemStack(int id) {
        Object val = data.get(id);
        return val instanceof ItemStack is ? is : null;
    }

    public void setObjectWatched(int id) {}

    public boolean hasChanges() { return false; }

    public java.util.List getChanged() { return null; }

    public java.util.List getAllWatched() { return null; }
}
