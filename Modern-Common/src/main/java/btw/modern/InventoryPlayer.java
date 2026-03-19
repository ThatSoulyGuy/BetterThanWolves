package btw.modern;

public class InventoryPlayer implements IInventory {
    public ItemStack[] mainInventory = new ItemStack[36];
    public ItemStack[] armorInventory = new ItemStack[4];
    public int currentItem = 0;
    public EntityPlayer player;
    private ItemStack itemStack;
    public boolean inventoryChanged = false;

    public InventoryPlayer(EntityPlayer player) {
        this.player = player;
    }

    public ItemStack getCurrentItem() {
        return this.currentItem < 9 && this.currentItem >= 0 ? this.mainInventory[this.currentItem] : null;
    }

    public static int getHotbarSize() { return 9; }
    public int getSizeInventory() { return mainInventory.length + 4; }
    public ItemStack getStackInSlot(int slot) { return null; }
    public ItemStack decrStackSize(int slot, int amount) { return null; }
    public ItemStack getStackInSlotOnClosing(int slot) { return null; }
    public void setInventorySlotContents(int slot, ItemStack stack) {}
    public String getInvName() { return "container.inventory"; }
    public boolean isInvNameLocalized() { return false; }
    public int getInventoryStackLimit() { return 64; }
    public void onInventoryChanged() {}
    public boolean isUseableByPlayer(EntityPlayer player) { return true; }
    public void openChest() {}
    public void closeChest() {}
    public boolean isItemValidForSlot(int slot, ItemStack stack) { return true; }
    public boolean addItemStackToInventory(ItemStack stack) { return false; }
    public boolean consumeInventoryItem(int itemId) { return false; }
    public boolean hasItem(int itemId) { return false; }
    public ItemStack getItemStack() { return itemStack; }
    public void setItemStack(ItemStack stack) { this.itemStack = stack; }
    public int clearInventory(int itemId, int metadata) { return 0; }
    public void dropAllItems() {}
    public int getTotalArmorValue() { return 0; }
    public void damageArmor(int damage) {}

    /**
     * FC's tool speed calculation. Gets the held item's strength vs the block.
     */
    public float getStrVsBlock(Block block) {
        float str = 1.0F;
        if (this.currentItem >= 0 && this.currentItem < this.mainInventory.length
                && this.mainInventory[this.currentItem] != null) {
            str = this.mainInventory[this.currentItem].getStrVsBlock(block);
        }
        return str;
    }

    /**
     * FC's position-aware tool speed calculation.
     */
    public float getStrVsBlock(World world, Block block, int i, int j, int k) {
        float str = 1.0F;
        if (this.currentItem >= 0 && this.currentItem < this.mainInventory.length
                && this.mainInventory[this.currentItem] != null) {
            str = this.mainInventory[this.currentItem].getStrVsBlock(world, block, i, j, k);
        }
        return str;
    }

    /**
     * FC's harvest check. Can the current tool harvest this block?
     */
    public boolean canHarvestBlock(World world, Block block, int i, int j, int k) {
        if (this.currentItem >= 0 && this.currentItem < this.mainInventory.length
                && this.mainInventory[this.currentItem] != null) {
            return this.mainInventory[this.currentItem].canHarvestBlock(world, block, i, j, k);
        }
        return false;
    }
}
