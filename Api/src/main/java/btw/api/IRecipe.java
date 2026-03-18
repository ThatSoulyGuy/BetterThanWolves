package btw.api;

public interface IRecipe {
    boolean matches(InventoryCrafting inv, World world);
    ItemStack getCraftingResult(InventoryCrafting inv);
    int getRecipeSize();
    ItemStack getRecipeOutput();

    default boolean matches(IRecipe recipe) { return false; }
    default boolean HasSecondaryOutput() { return false; }
}
