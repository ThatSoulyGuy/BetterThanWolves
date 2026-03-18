package btw.api;

public class TileEntityDispenser extends TileEntity implements IInventory {
    private ItemStack[] stacks = new ItemStack[9];

    public int getSizeInventory() { return 9; }
    public ItemStack getStackInSlot(int slot) { return stacks[slot]; }
    public ItemStack decrStackSize(int slot, int amount) { return null; }
    public ItemStack getStackInSlotOnClosing(int slot) { return null; }
    public void setInventorySlotContents(int slot, ItemStack stack) { stacks[slot] = stack; }
    public String getInvName() { return "container.dispenser"; }
    public boolean isInvNameLocalized() { return false; }
    public int getInventoryStackLimit() { return 64; }
    public void onInventoryChanged() {}
    public boolean isUseableByPlayer(EntityPlayer player) { return true; }
    public void openChest() {}
    public void closeChest() {}
    public boolean isItemValidForSlot(int slot, ItemStack stack) { return true; }
    public int addItem(ItemStack stack) { return -1; }
    public int getRandomStackFromInventory() { return -1; }
}
