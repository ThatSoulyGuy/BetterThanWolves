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

    public boolean matches(InventoryCrafting inv, World world) {
        int gridWidth = (int) Math.sqrt(inv.getSizeInventory());
        for (int xOffset = 0; xOffset <= gridWidth - recipeWidth; xOffset++) {
            for (int yOffset = 0; yOffset <= gridWidth - recipeHeight; yOffset++) {
                if (checkMatch(inv, xOffset, yOffset, false, gridWidth)) return true;
                if (checkMatch(inv, xOffset, yOffset, true, gridWidth)) return true;
            }
        }
        return false;
    }

    private boolean checkMatch(InventoryCrafting inv, int xOffset, int yOffset, boolean mirror, int gridWidth) {
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridWidth; y++) {
                int recipeX = x - xOffset;
                int recipeY = y - yOffset;
                ItemStack expected = null;

                if (recipeX >= 0 && recipeY >= 0 && recipeX < recipeWidth && recipeY < recipeHeight) {
                    if (mirror) {
                        expected = recipeItems[recipeWidth - recipeX - 1 + recipeY * recipeWidth];
                    } else {
                        expected = recipeItems[recipeX + recipeY * recipeWidth];
                    }
                }

                ItemStack actual = inv.getStackInRowAndColumn(x, y);

                if (expected == null && actual != null) return false;
                if (expected != null && actual == null) return false;
                if (expected == null) continue;

                if (actual.itemID != expected.itemID) return false;
                if (expected.getItemDamage() != -1 && expected.getItemDamage() != actual.getItemDamage()) return false;
            }
        }
        return true;
    }

    public ItemStack getCraftingResult(InventoryCrafting inv) {
        return this.recipeOutput.copy();
    }
}
