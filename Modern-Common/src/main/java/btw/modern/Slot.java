package btw.modern;

public class Slot {
    private final int slotIndex;
    public final IInventory inventory;
    public int slotNumber;
    public int xDisplayPosition;
    public int yDisplayPosition;

    public Slot(IInventory inventory, int index, int x, int y) {
        this.inventory = inventory;
        this.slotIndex = index;
        this.xDisplayPosition = x;
        this.yDisplayPosition = y;
    }

    public void onSlotChange(ItemStack oldStack, ItemStack newStack) {}
    protected void onCrafting(ItemStack stack, int amount) {}
    protected void onCrafting(ItemStack stack) {}
    public void onPickupFromSlot(EntityPlayer player, ItemStack stack) {}

    public boolean isItemValid(ItemStack stack) { return true; }
    public ItemStack getStack() { return inventory.getStackInSlot(slotIndex); }
    public boolean getHasStack() { return getStack() != null; }
    public void putStack(ItemStack stack) { inventory.setInventorySlotContents(slotIndex, stack); }
    public void onSlotChanged() { inventory.onInventoryChanged(); }
    public int getSlotStackLimit() { return inventory.getInventoryStackLimit(); }
    public ItemStack decrStackSize(int amount) { return inventory.decrStackSize(slotIndex, amount); }
    public boolean canTakeStack(EntityPlayer player) { return true; }
    public Icon getBackgroundIconIndex() { return null; }
}
