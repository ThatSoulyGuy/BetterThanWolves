package btw.modern;

public class TileEntityBeacon extends TileEntity implements IInventory {

    public int getLevels() { return 0; }
    public int getPrimaryEffect() { return 0; }
    public int getSecondaryEffect() { return 0; }

    public void SetPrimaryEffect(int effect) {}
    public void setLevelsServerSafe(int levels) {}
    public boolean IsOn() { return false; }
    public void SetIsOn(boolean on) {}

    // IInventory implementation
    public int getSizeInventory() { return 1; }
    public ItemStack getStackInSlot(int slot) { return null; }
    public ItemStack decrStackSize(int slot, int amount) { return null; }
    public ItemStack getStackInSlotOnClosing(int slot) { return null; }
    public void setInventorySlotContents(int slot, ItemStack stack) {}
    public String getInvName() { return "container.beacon"; }
    public boolean isInvNameLocalized() { return false; }
    public int getInventoryStackLimit() { return 1; }
    public void onInventoryChanged() {}
    public boolean isUseableByPlayer(EntityPlayer player) { return true; }
    public void openChest() {}
    public void closeChest() {}
    public boolean isItemValidForSlot(int slot, ItemStack stack) { return true; }
}
