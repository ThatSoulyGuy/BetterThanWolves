package btw.modern;

import java.util.HashMap;
import java.util.Map;

public class FurnaceRecipes {
    private static final FurnaceRecipes instance = new FurnaceRecipes();
    private Map smeltingList = new HashMap();
    private Map metaSmeltingList = new HashMap();
    private Map experienceList = new HashMap();

    public static FurnaceRecipes smelting() { return instance; }

    public void addSmelting(int inputId, ItemStack output, float xp) {
        smeltingList.put(inputId, output);
        experienceList.put(inputId, xp);
    }

    public void addSmelting(int inputId, ItemStack output, float xp, int cookTime) {
        addSmelting(inputId, output, xp);
    }

    public ItemStack getSmeltingResult(int inputId) {
        return (ItemStack) smeltingList.get(inputId);
    }

    public ItemStack getSmeltingResult(ItemStack input) {
        return getSmeltingResult(input.itemID);
    }

    public Map getSmeltingList() { return smeltingList; }
    public Map getMetaSmeltingList() { return metaSmeltingList; }
    public float getExperience(int itemId) {
        Float xp = (Float) experienceList.get(itemId);
        return xp != null ? xp : 0.0F;
    }
}
