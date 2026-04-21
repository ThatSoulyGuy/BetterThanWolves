package btw.modern;

import java.util.ArrayList;

public class MerchantRecipeList extends ArrayList {
    public MerchantRecipeList() {}
    public MerchantRecipeList(NBTTagCompound tag) { this.readRecipiesFromTags(tag); }

    public MerchantRecipe canRecipeBeUsed(ItemStack buy1, ItemStack buy2, int index) {
        if (index > 0 && index < this.size()) {
            MerchantRecipe r = (MerchantRecipe) this.get(index);
            if (buy1.itemID == r.getItemToBuy().itemID
                    && (buy2 == null && !r.hasSecondItemToBuy()
                        || r.hasSecondItemToBuy() && buy2 != null && r.getSecondItemToBuy().itemID == buy2.itemID)
                    && buy1.stackSize >= r.getItemToBuy().stackSize
                    && (!r.hasSecondItemToBuy() || buy2.stackSize >= r.getSecondItemToBuy().stackSize)) {
                return r;
            }
        }
        for (int i = 0; i < this.size(); i++) {
            MerchantRecipe r = (MerchantRecipe) this.get(i);
            if (buy1.itemID == r.getItemToBuy().itemID
                    && buy1.stackSize >= r.getItemToBuy().stackSize
                    && (!r.hasSecondItemToBuy() && buy2 == null
                        || r.hasSecondItemToBuy() && buy2 != null
                           && r.getSecondItemToBuy().itemID == buy2.itemID
                           && buy2.stackSize >= r.getSecondItemToBuy().stackSize)) {
                return r;
            }
        }
        return null;
    }

    public void addToListWithCheck(MerchantRecipe recipe) {
        for (int i = 0; i < this.size(); i++) {
            MerchantRecipe existing = (MerchantRecipe) this.get(i);
            if (recipe.hasSameIDsAs(existing)) {
                return;
            }
        }
        this.add(recipe);
    }

    public void readRecipiesFromTags(NBTTagCompound tag) {
        NBTTagList list = tag.getTagList("Recipes");
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound recipeTag = (NBTTagCompound) list.tagAt(i);
            this.add(new MerchantRecipe(recipeTag));
        }
    }

    public NBTTagCompound getRecipiesAsTags() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList list = new NBTTagList("Recipes");
        for (int i = 0; i < this.size(); i++) {
            MerchantRecipe r = (MerchantRecipe) this.get(i);
            list.appendTag(r.writeToTags());
        }
        tag.setTag("Recipes", list);
        return tag;
    }

    public static MerchantRecipeList readRecipiesFromStream(java.io.DataInputStream stream) throws java.io.IOException {
        return new MerchantRecipeList();
    }
}
