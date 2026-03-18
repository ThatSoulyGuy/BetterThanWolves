package btw.api;

import java.util.Map;

public class FurnaceRecipes {
    private static final FurnaceRecipes instance = new FurnaceRecipes();

    public static FurnaceRecipes smelting() { return instance; }

    public void addSmelting(int inputId, ItemStack output, float xp) {}
    public void addSmelting(int inputId, ItemStack output, float xp, int cookTime) {}
    public ItemStack getSmeltingResult(int inputId) { return null; }
    public ItemStack getSmeltingResult(ItemStack input) { return null; }
    public Map getSmeltingList() { return null; }
    public float getExperience(int itemId) { return 0.0F; }
}
