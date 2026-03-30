package btw.modern;

import java.util.HashMap;
import java.util.Map;

public class FurnaceRecipes {
    private static final FurnaceRecipes instance = new FurnaceRecipes();
    private Map smeltingList = new HashMap();
    private Map metaSmeltingList = new HashMap();
    private Map experienceList = new HashMap();
    private boolean vanillaRecipesRegistered = false;

    public static FurnaceRecipes smelting() {
        instance.ensureVanillaRecipes();
        return instance;
    }

    private void ensureVanillaRecipes() {
        if (vanillaRecipesRegistered) return;
        // Only register once Block/Item statics are initialized
        if (Block.oreIron == null || Item.ingotIron == null) return;
        vanillaRecipesRegistered = true;

        addSmelting(Block.oreIron.blockID, new ItemStack(Item.ingotIron), 0.7F);
        addSmelting(Block.oreGold.blockID, new ItemStack(Item.ingotGold), 1.0F);
        addSmelting(Block.oreDiamond.blockID, new ItemStack(Item.diamond), 1.0F);
        addSmelting(Block.sand.blockID, new ItemStack(Block.glass), 0.1F);
        addSmelting(Item.porkRaw.itemID, new ItemStack(Item.porkCooked), 0.35F);
        addSmelting(Item.beefRaw.itemID, new ItemStack(Item.beefCooked), 0.35F);
        addSmelting(Item.chickenRaw.itemID, new ItemStack(Item.chickenCooked), 0.35F);
        addSmelting(Item.fishRaw.itemID, new ItemStack(Item.fishCooked), 0.35F);
        addSmelting(Block.cobblestone.blockID, new ItemStack(Block.stone), 0.1F);
        addSmelting(Item.clay.itemID, new ItemStack(Item.brick), 0.3F);
        addSmelting(Block.cactus.blockID, new ItemStack(Item.dyePowder, 1, 2), 0.2F);
        addSmelting(Block.wood.blockID, new ItemStack(Item.coal, 1, 1), 0.15F);
        addSmelting(Block.oreEmerald.blockID, new ItemStack(Item.emerald), 1.0F);
        addSmelting(Item.potato.itemID, new ItemStack(Item.bakedPotato), 0.35F);
        addSmelting(Block.oreCoal.blockID, new ItemStack(Item.coal), 0.1F);
        addSmelting(Block.oreRedstone.blockID, new ItemStack(Item.redstone), 0.7F);
        addSmelting(Block.oreLapis.blockID, new ItemStack(Item.dyePowder, 1, 4), 0.2F);
        addSmelting(Block.oreNetherQuartz.blockID, new ItemStack(Item.netherQuartz), 0.2F);
    }

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
