package btw.api;

public class TileEntity {

    public World worldObj;
    public int xCoord;
    public int yCoord;
    public int zCoord;
    public boolean tileEntityInvalid;
    public int blockMetadata = -1;
    public Block blockType;

    public void readFromNBT(NBTTagCompound compound) {}

    public void writeToNBT(NBTTagCompound compound) {}

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
        return this.blockType;
    }

    public int getBlockMetadata() {
        return this.blockMetadata;
    }

    public void onInventoryChanged() {}

    public boolean isStackValidForSlot(int slot, ItemStack stack) { return true; }
    public int getSizeInventory() { return 0; }
    public void setInventorySlotContents(int slot, ItemStack stack) {}
    public ItemStack getStackInSlot(int slot) { return null; }

    public static void addMapping(Class clazz, String name) {}
    public static void ReplaceVanillaMapping(Class oldClass, Class newClass, String name) {}
    public void markDirty() { onInventoryChanged(); }
}
