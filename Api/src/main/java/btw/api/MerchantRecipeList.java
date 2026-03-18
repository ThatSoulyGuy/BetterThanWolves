package btw.api;

import java.util.ArrayList;

public class MerchantRecipeList extends ArrayList {
    public MerchantRecipeList() {}

    public MerchantRecipe canRecipeBeUsed(ItemStack buy1, ItemStack buy2, int index) { return null; }
    public void addToListWithCheck(MerchantRecipe recipe) { this.add(recipe); }
    public void readRecipiesFromTags(NBTTagCompound tag) {}
    public NBTTagCompound getRecipiesAsTags() { return new NBTTagCompound(); }

    public static MerchantRecipeList readRecipiesFromStream(java.io.DataInputStream stream) throws java.io.IOException { return new MerchantRecipeList(); }
}
