package btw.modern;

public class InventoryCraftResult implements IInventory {
    private ItemStack[] stackResult = new ItemStack[1];

    public int getSizeInventory() { return 1; }
    public ItemStack getStackInSlot(int slot) { return stackResult[0]; }
    public ItemStack decrStackSize(int slot, int amount) {
        if (stackResult[0] != null) {
            ItemStack stack = stackResult[0];
            stackResult[0] = null;
            return stack;
        }
        return null;
    }
    public ItemStack getStackInSlotOnClosing(int slot) {
        if (stackResult[0] != null) {
            ItemStack stack = stackResult[0];
            stackResult[0] = null;
            return stack;
        }
        return null;
    }
    public void setInventorySlotContents(int slot, ItemStack stack) { stackResult[0] = stack; }
    public String getInvName() { return "Result"; }
    public boolean isInvNameLocalized() { return false; }
    public int getInventoryStackLimit() { return 64; }
    public void onInventoryChanged() {}
    public boolean isUseableByPlayer(EntityPlayer player) { return true; }
    public void openChest() {}
    public void closeChest() {}
    public boolean isItemValidForSlot(int slot, ItemStack stack) { return true; }
}
