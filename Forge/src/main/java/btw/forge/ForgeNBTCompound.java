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
        // Check both the real CompoundTag AND the inherited tagMap.
        // FC's generic setTag(String, NBTBase) stores in tagMap (base class),
        // while typed setters (setInteger, etc.) store in the CompoundTag.
        // Without this, addEnchantment overwrites previous enchantments
        // because it thinks the "ench" key doesn't exist.
        return tag.contains(key) || super.hasKey(key);
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
    public void setTag(String key, btw.modern.NBTBase value) {
        // Store in BOTH the real CompoundTag and the inherited tagMap.
        // FC code reads back via getTagList/getCompoundTag which may
        // check tagMap, while toMcTag reads from the CompoundTag.
        super.setTag(key, value); // tagMap for FC reads
        if (value instanceof ForgeNBTCompound fnbt) {
            tag.put(key, fnbt.getTag());
        } else if (value instanceof btw.modern.NBTTagCompound sub) {
            CompoundTag mcSub = ItemStackHelper.toMcTag(sub);
            if (mcSub != null) tag.put(key, mcSub);
        } else if (value instanceof btw.modern.NBTTagList list) {
            net.minecraft.nbt.ListTag mcList = ItemStackHelper.toMcList(list);
            if (mcList != null) tag.put(key, mcList);
        }
    }

    @Override
    public void removeTag(String key) {
        super.removeTag(key); // also remove from tagMap
        tag.remove(key);
    }

    @Override
    public btw.modern.NBTBase copy() {
        return new ForgeNBTCompound(tag.copy());
    }

    @Override
    public btw.modern.NBTTagList getTagList(String key) {
        // First check FC's tagMap (for data written during this session)
        btw.modern.NBTTagList fromMap = super.getTagList(key);
        if (fromMap != null && fromMap.tagCount() > 0) return fromMap;
        // Fall back to MC CompoundTag (for data loaded from disk)
        if (!tag.contains(key, 9)) return new btw.modern.NBTTagList(); // 9 = list type
        // Auto-detect element type. Vanilla 1.5.2 stores Pos/Motion as
        // double-element lists (type 6) and Rotation as float-element
        // lists (type 5). Compound-element lists (type 10) are used for
        // Equipment, ActiveEffects, etc. Without this dispatch, non-
        // compound lists come back empty and vanilla Entity.readFromNBT
        // throws IndexOutOfBoundsException dereferencing posList.tagAt(0).
        net.minecraft.nbt.Tag rawList = tag.get(key);
        if (!(rawList instanceof net.minecraft.nbt.ListTag mcList)) {
            return new btw.modern.NBTTagList();
        }
        byte elementType = mcList.getElementType();
        btw.modern.NBTTagList result = new btw.modern.NBTTagList();
        for (int i = 0; i < mcList.size(); i++) {
            switch (elementType) {
                case 6 -> { // double
                    double v = mcList.getDouble(i);
                    result.appendTag(new btw.modern.NBTTagDouble("", v));
                }
                case 5 -> { // float
                    float v = mcList.getFloat(i);
                    result.appendTag(new btw.modern.NBTTagFloat("", v));
                }
                case 8 -> { // string
                    String v = mcList.getString(i);
                    result.appendTag(new btw.modern.NBTTagString("", v));
                }
                case 10 -> { // compound
                    CompoundTag mcSub = mcList.getCompound(i);
                    result.appendTag(new ForgeNBTCompound(mcSub));
                }
                default -> {
                    // unsupported element type — skip
                }
            }
        }
        return result;
    }

    @Override
    public btw.modern.NBTTagCompound getCompoundTag(String key) {
        // 1.5.2 NBTTagCompound.getCompoundTag returns a fresh DETACHED
        // compound when the key is absent — FC callers write into it and
        // re-attach via setCompoundTag (FCItemArmorMod.func_82813_b:108-115,
        // removeColor:81-83, wool-armor dyeing). Returning null here NPE'd
        // every such FC path fed a converted stack tag.
        if (!tag.contains(key, 10)) { // 10 = compound tag type
            return new ForgeNBTCompound(new CompoundTag());
        }
        CompoundTag sub = tag.getCompound(key);
        return new ForgeNBTCompound(sub);
    }

    @Override
    public void setCompoundTag(String key, btw.modern.NBTTagCompound value) {
        if (value instanceof ForgeNBTCompound fnbt) {
            tag.put(key, fnbt.getTag());
        } else {
            // Convert plain NBTTagCompound to real CompoundTag by reading its tagMap
            CompoundTag sub = ItemStackHelper.toMcTag(value);
            if (sub == null) sub = new CompoundTag();
            tag.put(key, sub);
        }
    }
}
