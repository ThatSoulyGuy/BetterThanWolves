package btw.modern;

public class InventoryBasic implements IInventory {
    private String inventoryTitle;
    private int slotsCount;
    private ItemStack[] inventoryContents;
    private boolean field_94051_d;

    public InventoryBasic(String title, boolean localized, int slots) {
        this.inventoryTitle = title;
        this.field_94051_d = localized;
        this.slotsCount = slots;
        this.inventoryContents = new ItemStack[slots];
    }

    public int getSizeInventory() { return slotsCount; }
    public ItemStack getStackInSlot(int slot) { return inventoryContents[slot]; }
    public ItemStack decrStackSize(int slot, int amount) { return null; }
    public ItemStack getStackInSlotOnClosing(int slot) { return null; }
    public void setInventorySlotContents(int slot, ItemStack stack) { inventoryContents[slot] = stack; }
    public String getInvName() { return inventoryTitle; }
    public boolean isInvNameLocalized() { return field_94051_d; }
    public int getInventoryStackLimit() { return 64; }
    public void onInventoryChanged() {}
    public boolean isUseableByPlayer(EntityPlayer player) { return true; }
    public void openChest() {}
    public void closeChest() {}
    public boolean isItemValidForSlot(int slot, ItemStack stack) { return true; }
}
