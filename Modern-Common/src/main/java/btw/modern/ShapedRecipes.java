package btw.modern;

public class ShapedRecipes implements IRecipe {

    public final int recipeWidth;
    public final int recipeHeight;
    public final ItemStack[] recipeItems;
    private ItemStack recipeOutput;

    public ShapedRecipes(int width, int height, ItemStack[] items, ItemStack output) {
        this.recipeWidth = width;
        this.recipeHeight = height;
        this.recipeItems = items;
        this.recipeOutput = output;
    }

    public ItemStack getRecipeOutput() {
        return this.recipeOutput;
    }

    public int getRecipeSize() {
        return this.recipeWidth * this.recipeHeight;
    }

    public boolean matches(InventoryCrafting inv, World world) { return false; }
    public ItemStack getCraftingResult(InventoryCrafting inv) { return this.recipeOutput; }
}
