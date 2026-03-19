package btw.modern;

public class MerchantRecipe {
    private ItemStack itemToBuy;
    private ItemStack secondItemToBuy;
    private ItemStack itemToSell;
    private int toolUses;
    private int maxTradeUses;
    public int m_iTradeLevel;

    public MerchantRecipe(ItemStack buy1, ItemStack buy2, ItemStack sell) {
        this.itemToBuy = buy1;
        this.secondItemToBuy = buy2;
        this.itemToSell = sell;
        this.maxTradeUses = 7;
    }

    public MerchantRecipe(ItemStack buy1, ItemStack buy2, ItemStack sell, int maxUses) {
        this.itemToBuy = buy1;
        this.secondItemToBuy = buy2;
        this.itemToSell = sell;
        this.maxTradeUses = maxUses;
    }

    public MerchantRecipe(ItemStack buy1, ItemStack sell) {
        this(buy1, null, sell);
    }

    public MerchantRecipe(ItemStack buy1, Item sell) {
        this(buy1, new ItemStack(sell));
    }

    public MerchantRecipe(ItemStack buy1, ItemStack sell, int maxUses) {
        this(buy1, null, sell);
        this.maxTradeUses = maxUses;
    }

    public ItemStack getItemToBuy() { return itemToBuy; }
    public ItemStack getSecondItemToBuy() { return secondItemToBuy; }
    public boolean hasSecondItemToBuy() { return secondItemToBuy != null; }
    public ItemStack getItemToSell() { return itemToSell; }
    public int getToolUses() { return toolUses; }
    public int getMaxTradeUses() { return maxTradeUses; }
    public void incrementToolUses() { this.toolUses++; }
    public boolean isRecipeDisabled() { return toolUses >= maxTradeUses; }
    public void func_82783_a(int maxTradeUses) {}
    public MerchantRecipe(NBTTagCompound tag) { this(null, null, null); }
    public NBTTagCompound writeToTags() { return new NBTTagCompound(); }
    public boolean hasSameIDsAs(MerchantRecipe other) { return false; }
    public boolean func_82784_g() { return isRecipeDisabled(); }
}
