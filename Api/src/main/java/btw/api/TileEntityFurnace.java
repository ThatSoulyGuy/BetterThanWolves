package btw.api;

public class TileEntityFurnace extends TileEntity {

    public ItemStack[] furnaceItemStacks = new ItemStack[3];
    public int furnaceBurnTime = 0;
    public int currentItemBurnTime = 0;
    public int furnaceCookTime = 0;
    public boolean keepFurnaceInventory = false;
    public static int m_iBaseBurnTimeMultiplier = 1;
    public static int m_iDefaultCookTime = 200;

    public boolean isBurning() {
        return this.furnaceBurnTime > 0;
    }

    public int getSizeInventory() {
        return this.furnaceItemStacks.length;
    }

    public String getInvName() { return ""; }
    public int getItemBurnTime(ItemStack stack) { return 0; }
    public int GetCookTimeForCurrentItem() { return 200; }
    public void smeltItem() {}
    public void setInventorySlotContents(int slot, ItemStack stack) {}
    public ItemStack getStackInSlot(int slot) { return null; }
    public boolean canSmelt() { return false; }
}
