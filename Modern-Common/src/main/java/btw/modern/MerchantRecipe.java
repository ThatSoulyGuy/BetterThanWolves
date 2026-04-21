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
        this.maxTradeUses = 1;
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

    public MerchantRecipe(NBTTagCompound tag) {
        this.readFromTags(tag);
    }

    public ItemStack getItemToBuy() { return itemToBuy; }
    public ItemStack getSecondItemToBuy() { return secondItemToBuy; }
    public boolean hasSecondItemToBuy() { return secondItemToBuy != null; }
    public ItemStack getItemToSell() { return itemToSell; }
    public int getToolUses() { return toolUses; }
    public int getMaxTradeUses() { return maxTradeUses; }
    public void incrementToolUses() { this.toolUses++; }
    public boolean isRecipeDisabled() { return toolUses >= maxTradeUses; }
    public boolean func_82784_g() { return isRecipeDisabled(); }

    public void func_82783_a(int extra) {
        this.maxTradeUses += extra;
    }

    public boolean hasSameIDsAs(MerchantRecipe other) {
        if (other == null) return false;
        if (this.itemToBuy.itemID != other.itemToBuy.itemID) return false;
        if (this.itemToSell.itemID != other.itemToSell.itemID) return false;
        if (this.secondItemToBuy != null && other.secondItemToBuy != null) {
            return this.secondItemToBuy.itemID == other.secondItemToBuy.itemID;
        }
        return this.secondItemToBuy == null && other.secondItemToBuy == null;
    }

    public void readFromTags(NBTTagCompound tag) {
        NBTTagCompound buyTag = tag.getCompoundTag("buy");
        this.itemToBuy = ItemStack.loadItemStackFromNBT(buyTag);
        NBTTagCompound sellTag = tag.getCompoundTag("sell");
        this.itemToSell = ItemStack.loadItemStackFromNBT(sellTag);
        if (tag.hasKey("buyB")) {
            this.secondItemToBuy = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("buyB"));
        }
        if (tag.hasKey("uses")) {
            this.toolUses = tag.getInteger("uses");
        }
        if (tag.hasKey("maxUses")) {
            this.maxTradeUses = tag.getInteger("maxUses");
        } else {
            this.maxTradeUses = 1;
        }
        if (tag.hasKey("fcTradeLevel")) {
            this.m_iTradeLevel = tag.getInteger("fcTradeLevel");
        } else {
            this.m_iTradeLevel = 1;
        }
    }

    public void writeToTags(NBTTagCompound into) {
        into.setCompoundTag("buy", this.itemToBuy.writeToNBT(new NBTTagCompound()));
        into.setCompoundTag("sell", this.itemToSell.writeToNBT(new NBTTagCompound()));
        if (this.secondItemToBuy != null) {
            into.setCompoundTag("buyB", this.secondItemToBuy.writeToNBT(new NBTTagCompound()));
        }
        into.setInteger("uses", this.toolUses);
        into.setInteger("maxUses", this.maxTradeUses);
        into.setInteger("fcTradeLevel", this.m_iTradeLevel);
    }

    public NBTTagCompound writeToTags() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setCompoundTag("buy", this.itemToBuy.writeToNBT(new NBTTagCompound()));
        tag.setCompoundTag("sell", this.itemToSell.writeToNBT(new NBTTagCompound()));
        if (this.secondItemToBuy != null) {
            tag.setCompoundTag("buyB", this.secondItemToBuy.writeToNBT(new NBTTagCompound()));
        }
        tag.setInteger("uses", this.toolUses);
        tag.setInteger("maxUses", this.maxTradeUses);
        tag.setInteger("fcTradeLevel", this.m_iTradeLevel);
        return tag;
    }
}
