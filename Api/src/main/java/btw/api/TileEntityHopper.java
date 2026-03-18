package btw.api;

public class TileEntityHopper extends TileEntity implements IInventory {
    private ItemStack[] stacks = new ItemStack[5];
    private int transferCooldown = -1;

    public int getSizeInventory() { return 5; }
    public ItemStack getStackInSlot(int slot) { return stacks[slot]; }
    public ItemStack decrStackSize(int slot, int amount) { return null; }
    public ItemStack getStackInSlotOnClosing(int slot) { return null; }
    public void setInventorySlotContents(int slot, ItemStack stack) { stacks[slot] = stack; }
    public String getInvName() { return "container.hopper"; }
    public boolean isInvNameLocalized() { return false; }
    public int getInventoryStackLimit() { return 64; }
    public void onInventoryChanged() {}
    public boolean isUseableByPlayer(EntityPlayer player) { return true; }
    public void openChest() {}
    public void closeChest() {}
    public boolean isItemValidForSlot(int slot, ItemStack stack) { return true; }
    public void setTransferCooldown(int cooldown) { this.transferCooldown = cooldown; }
    public boolean isCoolingDown() { return this.transferCooldown > 0; }
}
