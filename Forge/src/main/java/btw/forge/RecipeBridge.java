package btw.forge;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Converts FC recipes (from btw.modern.CraftingManager and btw.modern.FurnaceRecipes)
 * into MC 1.20.1 recipes and injects them into the server's RecipeManager.
 *
 * Called after FC initialization completes and the server's recipe manager is available.
 * FC recipes take precedence over vanilla — vanilla recipes with conflicting outputs
 * are replaced.
 */
public class RecipeBridge {

    private static final Logger LOGGER = LogManager.getLogger("BTW-RecipeBridge");

    /**
     * Injects all FC recipes into the MC RecipeManager.
     * Called from BTWLifecycle.onServerStarting() after the server loads data packs.
     */
    @SuppressWarnings("unchecked")
    public static void injectRecipes(net.minecraft.server.MinecraftServer server) {
        RecipeManager recipeManager = server.getRecipeManager();

        // Get the mutable recipe map. `ObfuscationReflectionHelper` expects
        // the SRG name and resolves it to the runtime name via Forge's
        // naming service. RecipeManager's
        // Map<RecipeType, Map<ResourceLocation, Recipe>> field is SRG
        // `f_44007_` (Mojang name `recipes`). Passing the Mojang name
        // works in dev (mojmap-named classes) but throws
        // NoSuchFieldException in the reobfuscated production JAR — which
        // silently aborts ALL FC recipe injection (nothing craftable).
        // Always pass the SRG name; it resolves correctly in both
        // environments (dev: f_44007_ -> recipes; jar: f_44007_ -> f_44007_).
        final String RECIPES_FIELD_SRG = "f_44007_";
        Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipesByType;
        try {
            Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> immutableMap =
                    net.minecraftforge.fml.util.ObfuscationReflectionHelper.getPrivateValue(
                            RecipeManager.class, recipeManager, RECIPES_FIELD_SRG);
            if (immutableMap == null) {
                LOGGER.error("RecipeManager.{} was null — FC recipes will not be injected",
                        RECIPES_FIELD_SRG);
                return;
            }

            // Make mutable copies
            recipesByType = new HashMap<>();
            for (var entry : immutableMap.entrySet()) {
                recipesByType.put(entry.getKey(), new HashMap<>(entry.getValue()));
            }
            net.minecraftforge.fml.util.ObfuscationReflectionHelper.setPrivateValue(
                    RecipeManager.class, recipeManager, recipesByType, RECIPES_FIELD_SRG);
        } catch (Exception e) {
            LOGGER.error("Failed to access RecipeManager.recipes — FC recipes will not be injected", e);
            return;
        }

        int craftingCount = injectCraftingRecipes(recipesByType);
        int smeltingCount = injectSmeltingRecipes(recipesByType);

        LOGGER.info("Injected {} crafting + {} smelting FC recipes into RecipeManager",
                craftingCount, smeltingCount);
    }

    /**
     * Converts FC crafting recipes (shaped + shapeless) to MC recipes.
     */
    private static int injectCraftingRecipes(
            Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipesByType) {

        Map<ResourceLocation, Recipe<?>> craftingMap =
                recipesByType.computeIfAbsent(RecipeType.CRAFTING, k -> new HashMap<>());

        List fcRecipes = btw.modern.CraftingManager.getInstance().getRecipeList();
        int count = 0;

        for (int i = 0; i < fcRecipes.size(); i++) {
            Object fcRecipe = fcRecipes.get(i);
            ResourceLocation id = new ResourceLocation(BTWForgeMod.MOD_ID, "fc_recipe_" + i);

            try {
                Recipe<?> mcRecipe = null;

                if (fcRecipe instanceof btw.modern.ShapedRecipes shaped) {
                    mcRecipe = convertShaped(id, shaped);
                } else if (fcRecipe instanceof btw.modern.ShapelessRecipes shapeless) {
                    mcRecipe = convertShapeless(id, shapeless);
                }

                if (mcRecipe != null) {
                    craftingMap.put(id, mcRecipe);
                    count++;
                }
            } catch (Exception e) {
                LOGGER.debug("Failed to convert FC recipe {}: {}", i, e.getMessage());
            }
        }

        return count;
    }

    /**
     * Converts FC furnace recipes to MC smelting recipes.
     */
    @SuppressWarnings("unchecked")
    private static int injectSmeltingRecipes(
            Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipesByType) {

        Map<ResourceLocation, Recipe<?>> smeltingMap =
                recipesByType.computeIfAbsent(RecipeType.SMELTING, k -> new HashMap<>());

        Map<Integer, btw.modern.ItemStack> fcSmelting =
                btw.modern.FurnaceRecipes.smelting().getSmeltingList();
        int count = 0;

        for (Map.Entry<Integer, btw.modern.ItemStack> entry : fcSmelting.entrySet()) {
            int inputId = entry.getKey();
            btw.modern.ItemStack fcOutput = entry.getValue();

            try {
                // Convert input ID to Ingredient
                Ingredient input = fcIdToIngredient(inputId);
                if (input.isEmpty()) continue;

                // Convert output
                ItemStack output = ItemStackHelper.toMcStack(fcOutput);
                if (output.isEmpty()) continue;

                float xp = btw.modern.FurnaceRecipes.smelting().getExperience(inputId);

                ResourceLocation id = new ResourceLocation(
                        BTWForgeMod.MOD_ID, "fc_smelting_" + inputId);

                SmeltingRecipe recipe = new SmeltingRecipe(
                        id, "", CookingBookCategory.MISC,
                        input, output, xp, 200);

                smeltingMap.put(id, recipe);
                count++;
            } catch (Exception e) {
                LOGGER.debug("Failed to convert FC smelting recipe for input {}: {}",
                        inputId, e.getMessage());
            }
        }

        return count;
    }

    /**
     * Converts an FC ShapedRecipes to an MC ShapedRecipe.
     */
    private static ShapedRecipe convertShaped(ResourceLocation id,
                                               btw.modern.ShapedRecipes fcRecipe) {
        int w = fcRecipe.recipeWidth;
        int h = fcRecipe.recipeHeight;

        // Convert output
        ItemStack output = ItemStackHelper.toMcStack(fcRecipe.getRecipeOutput());
        if (output.isEmpty()) return null;

        // Convert ingredients
        NonNullList<Ingredient> ingredients = NonNullList.withSize(w * h, Ingredient.EMPTY);
        for (int i = 0; i < fcRecipe.recipeItems.length && i < ingredients.size(); i++) {
            btw.modern.ItemStack fcStack = fcRecipe.recipeItems[i];
            if (fcStack != null) {
                ingredients.set(i, fcStackToIngredient(fcStack));
            }
        }

        return new ShapedRecipe(id, "", CraftingBookCategory.MISC, w, h, ingredients, output);
    }

    /**
     * Converts an FC ShapelessRecipes to an MC ShapelessRecipe.
     */
    @SuppressWarnings("unchecked")
    private static ShapelessRecipe convertShapeless(ResourceLocation id,
                                                     btw.modern.ShapelessRecipes fcRecipe) {
        // Convert output
        ItemStack output = ItemStackHelper.toMcStack(fcRecipe.getRecipeOutput());
        if (output.isEmpty()) return null;

        // Convert ingredients
        NonNullList<Ingredient> ingredients = NonNullList.create();
        List<btw.modern.ItemStack> fcItems = fcRecipe.recipeItems;
        for (Object obj : fcItems) {
            btw.modern.ItemStack fcStack = (btw.modern.ItemStack) obj;
            if (fcStack != null) {
                Ingredient ing = fcStackToIngredient(fcStack);
                if (!ing.isEmpty()) {
                    ingredients.add(ing);
                }
            }
        }

        if (ingredients.isEmpty()) return null;

        return new ShapelessRecipe(id, "", CraftingBookCategory.MISC, output, ingredients);
    }

    /**
     * Converts an FC ItemStack to an MC Ingredient (for recipe inputs).
     */
    private static Ingredient fcStackToIngredient(btw.modern.ItemStack fcStack) {
        ItemStack mcStack = ItemStackHelper.toMcStack(fcStack);
        if (mcStack.isEmpty()) return Ingredient.EMPTY;
        return Ingredient.of(mcStack);
    }

    /**
     * Converts a legacy item/block ID to an Ingredient.
     */
    private static Ingredient fcIdToIngredient(int legacyId) {
        // Try as item first
        net.minecraft.world.item.Item modernItem = ProxyRegistry.getModernItem(legacyId);
        if (modernItem != null) {
            return Ingredient.of(modernItem);
        }
        // Try as block
        net.minecraft.world.level.block.Block modernBlock = ProxyRegistry.getModernBlock(legacyId);
        if (modernBlock != null) {
            return Ingredient.of(modernBlock.asItem());
        }
        return Ingredient.EMPTY;
    }
}
