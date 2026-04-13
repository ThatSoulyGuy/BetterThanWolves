package btw.modern;

/**
 * Compile-time stub for vanilla 1.5.2 IMerchant interface.
 * Real version loaded via remap at runtime.
 */
public interface IMerchant {
    void setCustomer(EntityPlayer player);
    EntityPlayer getCustomer();
    MerchantRecipeList getRecipes(EntityPlayer player);
    void useRecipe(MerchantRecipe recipe);
    int GetCurrentTradeLevel();
    int GetCurrentTradeXP();
    int GetCurrentTradeMaxXP();
}
