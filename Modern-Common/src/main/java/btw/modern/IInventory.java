package btw.modern;

public interface IInventory {
    int getSizeInventory();
    ItemStack getStackInSlot(int slot);
    ItemStack decrStackSize(int slot, int amount);
    ItemStack getStackInSlotOnClosing(int slot);
    void setInventorySlotContents(int slot, ItemStack stack);
    String getInvName();
    boolean isInvNameLocalized();
    int getInventoryStackLimit();
    void onInventoryChanged();
    boolean isUseableByPlayer(EntityPlayer player);
    void openChest();
    void closeChest();
    default boolean isItemValidForSlot(int slot, ItemStack stack) { return true; }
}
