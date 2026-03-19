package btw.forge;

import net.minecraft.nbt.CompoundTag;

/**
 * A {@link btw.modern.NBTTagCompound} backed by a real {@link CompoundTag}.
 * Reads and writes go directly to the MC CompoundTag, allowing FC code
 * to persist data in the real save format.
 */
public class ForgeNBTCompound extends btw.modern.NBTTagCompound {

    private final CompoundTag tag;

    public ForgeNBTCompound(CompoundTag tag) {
        this.tag = tag;
    }

    public CompoundTag getTag() {
        return tag;
    }

    @Override
    public void setInteger(String key, int value) {
        tag.putInt(key, value);
    }

    @Override
    public int getInteger(String key) {
        return tag.getInt(key);
    }

    @Override
    public void setString(String key, String value) {
        tag.putString(key, value);
    }

    @Override
    public String getString(String key) {
        return tag.getString(key);
    }

    @Override
    public void setBoolean(String key, boolean value) {
        tag.putBoolean(key, value);
    }

    @Override
    public boolean getBoolean(String key) {
        return tag.getBoolean(key);
    }

    @Override
    public boolean hasKey(String key) {
        return tag.contains(key);
    }

    @Override
    public void setFloat(String key, float value) {
        tag.putFloat(key, value);
    }

    @Override
    public float getFloat(String key) {
        return tag.getFloat(key);
    }

    @Override
    public void setLong(String key, long value) {
        tag.putLong(key, value);
    }

    @Override
    public long getLong(String key) {
        return tag.getLong(key);
    }

    @Override
    public void setShort(String key, short value) {
        tag.putShort(key, value);
    }

    @Override
    public short getShort(String key) {
        return tag.getShort(key);
    }

    @Override
    public void setDouble(String key, double value) {
        tag.putDouble(key, value);
    }

    @Override
    public double getDouble(String key) {
        return tag.getDouble(key);
    }

    @Override
    public void setByte(String key, byte value) {
        tag.putByte(key, value);
    }

    @Override
    public byte getByte(String key) {
        return tag.getByte(key);
    }

    @Override
    public void setIntArray(String key, int[] value) {
        tag.putIntArray(key, value);
    }

    @Override
    public int[] getIntArray(String key) {
        return tag.getIntArray(key);
    }

    @Override
    public void setByteArray(String key, byte[] value) {
        tag.putByteArray(key, value);
    }

    @Override
    public byte[] getByteArray(String key) {
        return tag.getByteArray(key);
    }

    @Override
    public void removeTag(String key) {
        tag.remove(key);
    }

    @Override
    public btw.modern.NBTTagCompound getCompoundTag(String key) {
        CompoundTag sub = tag.getCompound(key);
        return new ForgeNBTCompound(sub);
    }

    @Override
    public void setCompoundTag(String key, btw.modern.NBTTagCompound value) {
        if (value instanceof ForgeNBTCompound fnbt) {
            tag.put(key, fnbt.getTag());
        } else {
            // Fallback: create a new CompoundTag and copy
            CompoundTag sub = new CompoundTag();
            tag.put(key, sub);
        }
    }
}
