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

    /**
     * Vanilla 1.5.2 DataWatcher.addObjectByDataType — declares a typed slot
     * with no initial value. Type IDs (from vanilla 1.5.2 Watchable):
     *   0 = byte, 1 = short, 2 = int, 3 = float, 4 = string,
     *   5 = ItemStack, 6 = ChunkCoordinates.
     * Called by vanilla EntityItem.entityInit (slot 10, type 5) and
     * other entity init methods. We back this with a sensible-default
     * value so the typed getters don't NPE.
     */
    public void addObjectByDataType(int id, int type) {
        Object initial;
        switch (type) {
            case 0 -> initial = (byte) 0;
            case 1 -> initial = (short) 0;
            case 2 -> initial = 0;
            case 3 -> initial = 0.0F;
            case 4 -> initial = "";
            case 5 -> initial = null; // ItemStack — caller will set later
            default -> initial = null;
        }
        data.put(id, initial);
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

    /**
     * Returns a snapshot of all stored id → value pairs.
     * Used by the Forge bridge to serialize DataWatcher state
     * for server→client sync.
     */
    public Map<Integer, Object> snapshot() {
        return new HashMap<>(data);
    }

    /**
     * Replaces all stored values with the contents of {@code values}.
     * Existing keys not present in {@code values} are preserved so that
     * partial snapshots do not wipe client-side defaults.
     */
    public void applySnapshot(Map<Integer, Object> values) {
        if (values == null) return;
        for (Map.Entry<Integer, Object> e : values.entrySet()) {
            data.put(e.getKey(), e.getValue());
        }
    }
}
