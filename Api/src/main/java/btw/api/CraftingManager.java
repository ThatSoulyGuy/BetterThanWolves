package btw.api;

import java.util.List;

public class CraftingManager {
    private static final CraftingManager instance = new CraftingManager();
    private List recipes;

    public static CraftingManager getInstance() { return instance; }
    public List getRecipeList() { return recipes; }
    public ShapedRecipes addRecipe(ItemStack output, Object... params) { return null; }
    public void addShapelessRecipe(ItemStack output, Object... params) {}
    public ItemStack findMatchingRecipe(InventoryCrafting inv, World world) { return null; }
    public IRecipe FindMatchingRecipe(InventoryCrafting inv, World world) { return null; }

    public ShapelessRecipes AddShapelessRecipe(ItemStack output, Object... params) { return null; }
    public ShapedRecipes AddShapedRecipeWithCustomClass(Class clazz, ItemStack output, Object... params) { return null; }
    public boolean RemoveRecipe(ItemStack output, Object... params) { return false; }
    public boolean RemoveShapelessRecipe(ItemStack output, Object... params) { return false; }
}
