package btw.modern;

/**
 * Vanilla 1.5.2 InventoryMerchant — 3-slot trading inventory.
 * Full implementation for dev environment compatibility.
 */
public class InventoryMerchant implements IInventory {
    private final IMerchant theMerchant;
    private ItemStack[] theInventory = new ItemStack[3];
    private final EntityPlayer thePlayer;
    private MerchantRecipe currentRecipe;
    private int currentRecipeIndex;

    public InventoryMerchant(EntityPlayer player, IMerchant merchant) {
        this.thePlayer = player;
        this.theMerchant = merchant;
    }

    public int getSizeInventory() { return theInventory.length; }
    public ItemStack getStackInSlot(int i) { return theInventory[i]; }

    public ItemStack decrStackSize(int slot, int amount) {
        if (theInventory[slot] == null) return null;
        if (slot == 2) {
            ItemStack r = theInventory[slot];
            theInventory[slot] = null;
            return r;
        }
        if (theInventory[slot].stackSize <= amount) {
            ItemStack r = theInventory[slot];
            theInventory[slot] = null;
            if (slot == 0 || slot == 1) resetRecipeAndSlots();
            return r;
        }
        ItemStack r = theInventory[slot].splitStack(amount);
        if (theInventory[slot].stackSize == 0) theInventory[slot] = null;
        if (slot == 0 || slot == 1) resetRecipeAndSlots();
        return r;
    }

    public ItemStack getStackInSlotOnClosing(int slot) {
        if (theInventory[slot] != null) {
            ItemStack r = theInventory[slot];
            theInventory[slot] = null;
            return r;
        }
        return null;
    }

    public void setInventorySlotContents(int slot, ItemStack stack) {
        theInventory[slot] = stack;
        if (stack != null && stack.stackSize > getInventoryStackLimit())
            stack.stackSize = getInventoryStackLimit();
        if (slot == 0 || slot == 1) resetRecipeAndSlots();
    }

    public String getInvName() { return "mob.villager"; }
    public boolean isInvNameLocalized() { return false; }
    public int getInventoryStackLimit() { return 64; }
    public boolean isUseableByPlayer(EntityPlayer player) { return theMerchant.getCustomer() == player; }
    public void openChest() {}
    public void closeChest() {}
    public boolean isItemValidForSlot(int slot, ItemStack stack) { return true; }
    public boolean isStackValidForSlot(int slot, ItemStack stack) { return true; }
    public void onInventoryChanged() { resetRecipeAndSlots(); }

    public void resetRecipeAndSlots() {
        currentRecipe = null;
        ItemStack a = theInventory[0];
        ItemStack b = theInventory[1];
        if (a == null) { a = b; b = null; }
        if (a == null) { setInventorySlotContents(2, null); return; }
        MerchantRecipeList recipes = theMerchant.getRecipes(thePlayer);
        if (recipes != null) {
            MerchantRecipe r = recipes.canRecipeBeUsed(a, b, currentRecipeIndex);
            if (r != null && !r.func_82784_g()) {
                currentRecipe = r;
                setInventorySlotContents(2, r.getItemToSell().copy());
            } else if (b != null) {
                r = recipes.canRecipeBeUsed(b, a, currentRecipeIndex);
                if (r != null && !r.func_82784_g()) {
                    currentRecipe = r;
                    setInventorySlotContents(2, r.getItemToSell().copy());
                } else {
                    setInventorySlotContents(2, null);
                }
            } else {
                setInventorySlotContents(2, null);
            }
        }
    }

    public MerchantRecipe getCurrentRecipe() { return currentRecipe; }

    public void setCurrentRecipeIndex(int index) {
        currentRecipeIndex = index;
        resetRecipeAndSlots();
    }
}
