package btw.modern;

/**
 * Vanilla 1.5.2 SlotMerchantResult — output slot for merchant trading.
 * Full implementation for dev environment compatibility.
 */
public class SlotMerchantResult extends Slot {
    private final InventoryMerchant theMerchantInventory;
    private EntityPlayer thePlayer;
    private int field_75231_g;
    private final IMerchant theMerchant;

    public SlotMerchantResult(EntityPlayer player, IMerchant merchant, InventoryMerchant inv, int slotIndex, int x, int y) {
        super(inv, slotIndex, x, y);
        this.thePlayer = player;
        this.theMerchant = merchant;
        this.theMerchantInventory = inv;
    }

    public boolean isItemValid(ItemStack stack) { return false; }

    public ItemStack decrStackSize(int amount) {
        if (getHasStack()) field_75231_g += Math.min(amount, getStack().stackSize);
        return super.decrStackSize(amount);
    }

    public void onCrafting(ItemStack stack, int amount) {
        field_75231_g += amount;
        onCrafting(stack);
    }

    public void onCrafting(ItemStack stack) {
        stack.onCrafting(thePlayer.worldObj, thePlayer, field_75231_g);
        field_75231_g = 0;
    }

    private static final org.apache.logging.log4j.Logger TRADE_LOG =
            org.apache.logging.log4j.LogManager.getLogger("BTW-Trade");

    public void onPickupFromSlot(EntityPlayer player, ItemStack stack) {
        onCrafting(stack);
        MerchantRecipe recipe = theMerchantInventory.getCurrentRecipe();
        ItemStack a = theMerchantInventory.getStackInSlot(0);
        ItemStack b = theMerchantInventory.getStackInSlot(1);
        TRADE_LOG.info("[TRADE] onPickupFromSlot: recipe={} slot0={} slot1={} class={}",
                recipe != null ? recipe.getItemToBuy().itemID + "->" + recipe.getItemToSell().itemID : "NULL",
                a != null ? a.itemID + "x" + a.stackSize : "null",
                b != null ? b.itemID + "x" + b.stackSize : "null",
                this.getClass().getName());
        if (recipe != null) {
            boolean traded = doTrade(recipe, a, b) || doTrade(recipe, b, a);
            TRADE_LOG.info("[TRADE] doTrade result={} slot0After={} slot1After={}",
                    traded,
                    a != null ? a.itemID + "x" + a.stackSize : "null",
                    b != null ? b.itemID + "x" + b.stackSize : "null");
            if (traded) {
                if (a != null && a.stackSize <= 0) a = null;
                if (b != null && b.stackSize <= 0) b = null;
                theMerchantInventory.setInventorySlotContents(0, a);
                theMerchantInventory.setInventorySlotContents(1, b);
                theMerchant.useRecipe(recipe);
            }
        }
    }

    private boolean doTrade(MerchantRecipe recipe, ItemStack offer1, ItemStack offer2) {
        ItemStack cost1 = recipe.getItemToBuy();
        ItemStack cost2 = recipe.getSecondItemToBuy();
        if (offer1 != null && offer1.itemID == cost1.itemID) {
            if (cost2 != null && offer2 != null && cost2.itemID == offer2.itemID) {
                offer1.stackSize -= cost1.stackSize;
                offer2.stackSize -= cost2.stackSize;
                return true;
            }
            if (cost2 == null && offer2 == null) {
                offer1.stackSize -= cost1.stackSize;
                return true;
            }
        }
        return false;
    }
}
