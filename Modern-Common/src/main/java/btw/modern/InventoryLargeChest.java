package btw.modern;

public class InventoryLargeChest implements IInventory {

    private String name;
    private IInventory upperChest;
    private IInventory lowerChest;

    public InventoryLargeChest(String name, IInventory upper, IInventory lower) {
        this.name = name;
        this.upperChest = upper;
        this.lowerChest = lower;
    }

    public int getSizeInventory() {
        return upperChest.getSizeInventory() + lowerChest.getSizeInventory();
    }

    public ItemStack getStackInSlot(int slot) {
        return slot >= upperChest.getSizeInventory()
                ? lowerChest.getStackInSlot(slot - upperChest.getSizeInventory())
                : upperChest.getStackInSlot(slot);
    }

    public ItemStack decrStackSize(int slot, int amount) {
        return slot >= upperChest.getSizeInventory()
                ? lowerChest.decrStackSize(slot - upperChest.getSizeInventory(), amount)
                : upperChest.decrStackSize(slot, amount);
    }

    public ItemStack getStackInSlotOnClosing(int slot) {
        return slot >= upperChest.getSizeInventory()
                ? lowerChest.getStackInSlotOnClosing(slot - upperChest.getSizeInventory())
                : upperChest.getStackInSlotOnClosing(slot);
    }

    public void setInventorySlotContents(int slot, ItemStack stack) {
        if (slot >= upperChest.getSizeInventory()) {
            lowerChest.setInventorySlotContents(slot - upperChest.getSizeInventory(), stack);
        } else {
            upperChest.setInventorySlotContents(slot, stack);
        }
    }

    public String getInvName() { return name; }
    public boolean isInvNameLocalized() { return false; }
    public int getInventoryStackLimit() { return upperChest.getInventoryStackLimit(); }

    public void onInventoryChanged() {
        upperChest.onInventoryChanged();
        lowerChest.onInventoryChanged();
    }

    public boolean isUseableByPlayer(EntityPlayer player) {
        return upperChest.isUseableByPlayer(player) && lowerChest.isUseableByPlayer(player);
    }

    public void openChest() {
        upperChest.openChest();
        lowerChest.openChest();
    }

    public void closeChest() {
        upperChest.closeChest();
        lowerChest.closeChest();
    }

    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot >= upperChest.getSizeInventory()
                ? lowerChest.isItemValidForSlot(slot - upperChest.getSizeInventory(), stack)
                : upperChest.isItemValidForSlot(slot, stack);
    }
}
