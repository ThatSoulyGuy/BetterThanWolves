package btw.modern;

import java.util.HashMap;
import java.util.Map;

public class TileEntity {

    private static Map<String, Class> nameToClassMap = new HashMap<>();
    private static Map<Class, String> classToNameMap = new HashMap<>();

    public World worldObj;
    public int xCoord;
    public int yCoord;
    public int zCoord;
    public boolean tileEntityInvalid;
    public int blockMetadata = -1;
    public Block blockType;

    public void readFromNBT(NBTTagCompound compound) {
        this.xCoord = compound.getInteger("x");
        this.yCoord = compound.getInteger("y");
        this.zCoord = compound.getInteger("z");
    }

    public void writeToNBT(NBTTagCompound compound) {
        String id = classToNameMap.get(this.getClass());
        if (id != null) {
            compound.setString("id", id);
        }
        compound.setInteger("x", this.xCoord);
        compound.setInteger("y", this.yCoord);
        compound.setInteger("z", this.zCoord);
    }

    public void updateEntity() {}

    public Packet getDescriptionPacket() {
        return null;
    }

    public World getWorldObj() {
        return this.worldObj;
    }

    public void setWorldObj(World world) {
        this.worldObj = world;
    }

    public boolean receiveClientEvent(int eventId, int eventParam) {
        return false;
    }

    public void invalidate() {
        this.tileEntityInvalid = true;
    }

    public void validate() {
        this.tileEntityInvalid = false;
    }

    public boolean isInvalid() {
        return this.tileEntityInvalid;
    }

    public Block getBlockType() {
        if (this.worldObj != null) {
            this.blockType = Block.blocksList[this.worldObj.getBlockId(this.xCoord, this.yCoord, this.zCoord)];
        }
        return this.blockType;
    }

    public int getBlockMetadata() {
        if (this.blockMetadata == -1) {
            if (this.worldObj != null) {
                this.blockMetadata = this.worldObj.getBlockMetadata(this.xCoord, this.yCoord, this.zCoord);
            }
        }
        return this.blockMetadata;
    }

    public void onInventoryChanged() {
        if (this.worldObj != null) {
            this.blockMetadata = this.worldObj.getBlockMetadata(this.xCoord, this.yCoord, this.zCoord);
            this.worldObj.markBlockForUpdate(this.xCoord, this.yCoord, this.zCoord);
        }
    }

    public boolean isStackValidForSlot(int slot, ItemStack stack) { return true; }
    public int getSizeInventory() { return 0; }
    public void setInventorySlotContents(int slot, ItemStack stack) {}
    public ItemStack getStackInSlot(int slot) { return null; }

    public static void addMapping(Class clazz, String name) {
        nameToClassMap.put(name, clazz);
        classToNameMap.put(clazz, name);
    }

    public static void ReplaceVanillaMapping(Class oldClass, Class newClass, String name) {
        nameToClassMap.put(name, newClass);
        classToNameMap.remove(oldClass);
        classToNameMap.put(newClass, name);
    }

    public static TileEntity createAndLoadEntity(NBTTagCompound tag) {
        TileEntity tileEntity = null;
        try {
            Class clazz = nameToClassMap.get(tag.getString("id"));
            if (clazz != null) {
                tileEntity = (TileEntity) clazz.newInstance();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (tileEntity != null) {
            tileEntity.readFromNBT(tag);
        }

        return tileEntity;
    }

    public void markDirty() { onInventoryChanged(); }
}
