package btw.forge.jei;

import btw.forge.ItemStackHelper;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pulls recipes out of FC's bulk-recipe managers
 * ({@code FCCraftingManagerCauldron}, {@code FCCraftingManagerCrucible}, etc.)
 * and converts them into JEI-friendly {@link BulkRecipeWrapper} instances.
 *
 * <p>The FC crafting managers are private singletons with a private
 * {@code m_recipes} field on the shared {@code FCCraftingManagerBulk} base
 * class. There is no public accessor, so this class uses reflection. If the
 * field layout changes the extractor logs a warning and returns an empty
 * list rather than crashing the plugin.</p>
 */
public final class FCRecipeExtractor {

    private static final Logger LOGGER = LogManager.getLogger("BTW-JEI-Extractor");

    private FCRecipeExtractor() {}

    /**
     * Extracts all recipes from the given FC crafting manager singleton.
     *
     * @param managerClassName fully-qualified name of the FC manager class
     *                         (e.g. {@code net.minecraft.src.btw.crafting.FCCraftingManagerCauldron})
     * @return a list of wrapped recipes in JEI-ready form, or an empty list
     *         if the manager cannot be reached or has no recipes
     */
    public static List<BulkRecipeWrapper> extract(String managerClassName) {
        try {
            Class<?> managerCls = Class.forName(managerClassName);

            // All FC bulk managers expose a static getInstance() returning
            // the singleton.
            Object singleton = managerCls.getMethod("getInstance").invoke(null);
            if (singleton == null) {
                LOGGER.warn("{}.getInstance() returned null", managerClassName);
                return Collections.emptyList();
            }

            // The m_recipes field lives on the FCCraftingManagerBulk base.
            Field recipesField = findRecipesField(managerCls);
            if (recipesField == null) {
                LOGGER.warn("Could not find m_recipes field on {}", managerClassName);
                return Collections.emptyList();
            }
            recipesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Object> fcRecipes = (List<Object>) recipesField.get(singleton);
            if (fcRecipes == null || fcRecipes.isEmpty()) return Collections.emptyList();

            List<BulkRecipeWrapper> result = new ArrayList<>(fcRecipes.size());
            for (Object fcRecipe : fcRecipes) {
                BulkRecipeWrapper wrapper = convertRecipe(fcRecipe);
                if (wrapper != null) result.add(wrapper);
            }
            LOGGER.info("Extracted {} recipes from {}", result.size(), managerClassName);
            return result;
        } catch (ClassNotFoundException e) {
            LOGGER.debug("FC manager {} not present: {}", managerClassName, e.getMessage());
            return Collections.emptyList();
        } catch (Throwable e) {
            LOGGER.warn("Failed to extract recipes from {}: {}", managerClassName, e.toString());
            return Collections.emptyList();
        }
    }

    /** Walks up the class hierarchy to find the {@code m_recipes} field. */
    private static Field findRecipesField(Class<?> cls) {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField("m_recipes");
            } catch (NoSuchFieldException ignored) {}
        }
        return null;
    }

    /**
     * Converts a single {@code FCCraftingManagerBulkRecipe} into an MC
     * ItemStack pair. Uses reflection for the input/output list getters so
     * the extractor does not depend on the FC class at compile time.
     */
    private static BulkRecipeWrapper convertRecipe(Object fcRecipe) {
        try {
            Class<?> cls = fcRecipe.getClass();
            @SuppressWarnings("unchecked")
            List<Object> fcInputs =
                    (List<Object>) cls.getMethod("getCraftingIngrediantList").invoke(fcRecipe);
            @SuppressWarnings("unchecked")
            List<Object> fcOutputs =
                    (List<Object>) cls.getMethod("getCraftingOutputList").invoke(fcRecipe);

            List<ItemStack> inputs = new ArrayList<>();
            List<ItemStack> outputs = new ArrayList<>();
            if (fcInputs != null) {
                for (Object fcStack : fcInputs) {
                    ItemStack mc = ItemStackHelper.toMcStack((btw.modern.ItemStack) fcStack);
                    if (mc != null && !mc.isEmpty()) inputs.add(mc);
                }
            }
            if (fcOutputs != null) {
                for (Object fcStack : fcOutputs) {
                    ItemStack mc = ItemStackHelper.toMcStack((btw.modern.ItemStack) fcStack);
                    if (mc != null && !mc.isEmpty()) outputs.add(mc);
                }
            }
            if (inputs.isEmpty() && outputs.isEmpty()) return null;
            return new BulkRecipeWrapper(inputs, outputs);
        } catch (Throwable e) {
            LOGGER.debug("Skipping recipe: {}", e.getMessage());
            return null;
        }
    }
}
