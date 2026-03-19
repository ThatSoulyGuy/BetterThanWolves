package btw.modern;

public class InventoryCrafting implements IInventory {
    private ItemStack[] stackList;
    private int inventoryWidth;
    private Container eventHandler;

    public InventoryCrafting(Container container, int width, int height) {
        this.stackList = new ItemStack[width * height];
        this.eventHandler = container;
        this.inventoryWidth = width;
    }

    public int getSizeInventory() { return stackList.length; }
    public ItemStack getStackInSlot(int slot) { return slot >= 0 && slot < stackList.length ? stackList[slot] : null; }
    public ItemStack getStackInRowAndColumn(int row, int col) {
        if (row >= 0 && row < inventoryWidth) {
            int index = row + col * inventoryWidth;
            return getStackInSlot(index);
        }
        return null;
    }
    public ItemStack decrStackSize(int slot, int amount) { return null; }
    public ItemStack getStackInSlotOnClosing(int slot) { return null; }
    public void setInventorySlotContents(int slot, ItemStack stack) {
        if (slot >= 0 && slot < stackList.length) stackList[slot] = stack;
    }
    public String getInvName() { return "container.crafting"; }
    public boolean isInvNameLocalized() { return false; }
    public int getInventoryStackLimit() { return 64; }
    public void onInventoryChanged() {}
    public boolean isUseableByPlayer(EntityPlayer player) { return true; }
    public void openChest() {}
    public void closeChest() {}
    public boolean isItemValidForSlot(int slot, ItemStack stack) { return true; }
}
