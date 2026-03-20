package btw.modern;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FC's crafting recipe manager. Stores shaped and shapeless recipes
 * registered during FC initialization.
 *
 * FC code calls addRecipe/AddShapelessRecipe with the MC 1.5.2 format:
 *   addRecipe(output, "###", "# #", "###", '#', ingredient)
 *
 * The recipes are parsed and stored as ShapedRecipes/ShapelessRecipes.
 * The Forge bridge reads getRecipeList() after FC init and converts
 * them to modern MC recipes.
 */
public class CraftingManager {
    private static final CraftingManager instance = new CraftingManager();
    private List recipes = new ArrayList();

    public static CraftingManager getInstance() { return instance; }
    public List getRecipeList() { return recipes; }

    /**
     * Adds a shaped recipe. FC format:
     *   addRecipe(output, "AB", "CD", 'A', item1, 'B', item2, ...)
     */
    @SuppressWarnings("unchecked")
    public ShapedRecipes addRecipe(ItemStack output, Object... params) {
        String pattern = "";
        int i = 0;
        int width = 0;
        int height = 0;

        // Parse pattern strings
        List<String> rows = new ArrayList<>();
        while (i < params.length && params[i] instanceof String) {
            String row = (String) params[i];
            rows.add(row);
            width = Math.max(width, row.length());
            i++;
        }
        height = rows.size();

        // Build padded pattern
        StringBuilder sb = new StringBuilder();
        for (String row : rows) {
            while (row.length() < width) row = row + " ";
            sb.append(row);
        }
        pattern = sb.toString();

        // Parse character → ingredient map
        Map<Character, ItemStack> charMap = new HashMap<>();
        while (i < params.length) {
            if (params[i] instanceof Character) {
                char c = (Character) params[i];
                i++;
                ItemStack ingredient = toItemStack(params[i]);
                charMap.put(c, ingredient);
                i++;
            } else {
                i++;
            }
        }

        // Build recipe items array
        ItemStack[] recipeItems = new ItemStack[width * height];
        for (int j = 0; j < pattern.length() && j < recipeItems.length; j++) {
            char c = pattern.charAt(j);
            if (c == ' ') {
                recipeItems[j] = null;
            } else {
                ItemStack mapped = charMap.get(c);
                recipeItems[j] = mapped != null ? mapped.copy() : null;
            }
        }

        ShapedRecipes recipe = new ShapedRecipes(width, height, recipeItems, output);
        recipes.add(recipe);
        return recipe;
    }

    /**
     * Adds a shapeless recipe. FC format:
     *   addShapelessRecipe(output, ingredient1, ingredient2, ...)
     */
    @SuppressWarnings("unchecked")
    public void addShapelessRecipe(ItemStack output, Object... params) {
        AddShapelessRecipe(output, params);
    }

    @SuppressWarnings("unchecked")
    public ShapelessRecipes AddShapelessRecipe(ItemStack output, Object... params) {
        List<ItemStack> items = new ArrayList<>();
        for (Object param : params) {
            ItemStack stack = toItemStack(param);
            if (stack != null) {
                items.add(stack);
            }
        }
        ShapelessRecipes recipe = new ShapelessRecipes(output, items);
        recipes.add(recipe);
        return recipe;
    }

    @SuppressWarnings("unchecked")
    public ShapedRecipes AddShapedRecipeWithCustomClass(Class clazz, ItemStack output, Object... params) {
        // Parse same as addRecipe, but use a custom class
        // For now, fall back to standard ShapedRecipes
        return addRecipe(output, params);
    }

    public boolean RemoveRecipe(ItemStack output, Object... params) {
        // Mark a recipe for removal — the bridge will handle vanilla recipe removal
        return true;
    }

    public boolean RemoveShapelessRecipe(ItemStack output, Object... params) {
        return true;
    }

    public ItemStack findMatchingRecipe(InventoryCrafting inv, World world) { return null; }
    public IRecipe FindMatchingRecipe(InventoryCrafting inv, World world) { return null; }

    /**
     * Converts an Object parameter to an ItemStack.
     * FC passes Item, Block, or ItemStack as recipe ingredients.
     */
    private static ItemStack toItemStack(Object obj) {
        if (obj instanceof ItemStack) {
            return (ItemStack) obj;
        } else if (obj instanceof Item) {
            return new ItemStack((Item) obj);
        } else if (obj instanceof Block) {
            return new ItemStack((Block) obj);
        } else if (obj instanceof Integer) {
            return new ItemStack((Integer) obj, 1, 0);
        }
        return null;
    }
}
