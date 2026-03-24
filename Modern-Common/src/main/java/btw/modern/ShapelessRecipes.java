package btw.modern;

import java.util.ArrayList;
import java.util.List;

public class ShapelessRecipes implements IRecipe {
    private final ItemStack recipeOutput;
    public final List recipeItems;

    public ShapelessRecipes(ItemStack output, List items) {
        this.recipeOutput = output;
        this.recipeItems = items;
    }

    public ItemStack getRecipeOutput() { return recipeOutput; }

    @SuppressWarnings("unchecked")
    public boolean matches(InventoryCrafting inv, World world) {
        List<ItemStack> remaining = new ArrayList<>(recipeItems);

        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack slot = inv.getStackInSlot(i);
            if (slot != null) {
                boolean matched = false;
                for (int j = 0; j < remaining.size(); j++) {
                    ItemStack expected = (ItemStack) remaining.get(j);
                    if (slot.itemID == expected.itemID
                            && (expected.getItemDamage() == -1 || expected.getItemDamage() == slot.getItemDamage())) {
                        remaining.remove(j);
                        matched = true;
                        break;
                    }
                }
                if (!matched) return false;
            }
        }

        return remaining.isEmpty();
    }

    public ItemStack getCraftingResult(InventoryCrafting inv) {
        return recipeOutput != null ? recipeOutput.copy() : null;
    }

    public int getRecipeSize() { return recipeItems.size(); }

    public void SetHasSecondaryOutput(boolean hasSecondary) {}
}
