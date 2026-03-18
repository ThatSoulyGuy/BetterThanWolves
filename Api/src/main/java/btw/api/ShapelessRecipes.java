package btw.api;

import java.util.List;

public class ShapelessRecipes implements IRecipe {
    private final ItemStack recipeOutput;
    public final List recipeItems;

    public ShapelessRecipes(ItemStack output, List items) {
        this.recipeOutput = output;
        this.recipeItems = items;
    }

    public ItemStack getRecipeOutput() { return recipeOutput; }
    public boolean matches(InventoryCrafting inv, World world) { return false; }
    public ItemStack getCraftingResult(InventoryCrafting inv) { return null; }
    public int getRecipeSize() { return recipeItems.size(); }

    public void SetHasSecondaryOutput(boolean hasSecondary) {}
}
