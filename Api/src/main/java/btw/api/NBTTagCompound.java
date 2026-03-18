package btw.api;

import java.util.HashMap;
import java.util.Map;

public class NBTTagCompound extends NBTBase {

    private Map tagMap = new HashMap();

    public NBTTagCompound() {}

    public void setInteger(String key, int value) {
        tagMap.put(key, value);
    }

    public int getInteger(String key) {
        Object val = tagMap.get(key);
        return val instanceof Integer ? (Integer) val : 0;
    }

    public void setString(String key, String value) {
        tagMap.put(key, value);
    }

    public String getString(String key) {
        Object val = tagMap.get(key);
        return val instanceof String ? (String) val : "";
    }

    public void setBoolean(String key, boolean value) {
        tagMap.put(key, value);
    }

    public boolean getBoolean(String key) {
        Object val = tagMap.get(key);
        return val instanceof Boolean ? (Boolean) val : false;
    }

    public boolean hasKey(String key) {
        return tagMap.containsKey(key);
    }

    public void setTag(String key, Object value) {
        tagMap.put(key, value);
    }

    public Object getTag(String key) {
        return tagMap.get(key);
    }

    public NBTTagCompound getCompoundTag(String key) {
        Object val = tagMap.get(key);
        return val instanceof NBTTagCompound ? (NBTTagCompound) val : new NBTTagCompound();
    }

    public NBTTagList getTagList(String key) {
        Object val = tagMap.get(key);
        return val instanceof NBTTagList ? (NBTTagList) val : new NBTTagList();
    }

    public void setByte(String key, byte value) {
        tagMap.put(key, value);
    }

    public byte getByte(String key) {
        Object val = tagMap.get(key);
        return val instanceof Byte ? (Byte) val : 0;
    }

    public void setFloat(String key, float value) {
        tagMap.put(key, value);
    }

    public float getFloat(String key) {
        Object val = tagMap.get(key);
        return val instanceof Float ? (Float) val : 0.0F;
    }

    public void setLong(String key, long value) {
        tagMap.put(key, value);
    }

    public long getLong(String key) {
        Object val = tagMap.get(key);
        return val instanceof Long ? (Long) val : 0L;
    }

    public void setShort(String key, short value) {
        tagMap.put(key, value);
    }

    public short getShort(String key) {
        Object val = tagMap.get(key);
        return val instanceof Short ? (Short) val : 0;
    }

    public void setDouble(String key, double value) {
        tagMap.put(key, value);
    }

    public double getDouble(String key) {
        Object val = tagMap.get(key);
        return val instanceof Double ? (Double) val : 0.0D;
    }

    public void setIntArray(String key, int[] value) {
        tagMap.put(key, value);
    }

    public int[] getIntArray(String key) {
        Object val = tagMap.get(key);
        return val instanceof int[] ? (int[]) val : new int[0];
    }

    public void setByteArray(String key, byte[] value) {
        tagMap.put(key, value);
    }

    public byte[] getByteArray(String key) {
        Object val = tagMap.get(key);
        return val instanceof byte[] ? (byte[]) val : new byte[0];
    }

    public void removeTag(String key) {
        tagMap.remove(key);
    }

    public void setCompoundTag(String key, NBTTagCompound value) {
        tagMap.put(key, value);
    }

    public NBTTagCompound copy() {
        NBTTagCompound copy = new NBTTagCompound();
        copy.tagMap.putAll(this.tagMap);
        return copy;
    }
}
