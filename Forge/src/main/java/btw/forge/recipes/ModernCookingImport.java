package btw.forge.recipes;

import btw.forge.ItemStackHelper;
import btw.forge.ProxyRegistry;
import btw.forge.RecipeBridge;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Modern raw foods cook in FC's heat machines (INTEGRATIONS #025, part D).
 *
 * <p>FC cooks from two static tables: Modern-Common FurnaceRecipes (read by the FC furnace, the brick oven,
 * FCBlockFurnaceBrick.java:291-298, and the cauldron, FCTileEntityCauldron.java:157-202, which also needs the input
 * to be an FC food: food-core's ModernFoodItem) and FCCraftingManagerCampfire (the campfire spit). Post-1.5.2 and
 * modded foods (mutton, salmon, rabbit, any mod's raw meat) were in neither, so they could only be cooked in the
 * stubbed vanilla furnace and smoker.</p>
 *
 * <p>Rule: every SMELTING, SMOKING and CAMPFIRE_COOKING recipe of the final RecipeManager map with exactly one
 * ingredient offers its result for each edible, unmapped input item (one with no 1.5.2 or FC id; mapped foods such as
 * the potato, beef, pork, chicken and fish keep 4.B's own tables). One result per input: SMELTING first, else SMOKING,
 * else CAMPFIRE_COOKING; within a type the first recipe by id. The input gets its headless id
 * ({@link ProxyRegistry#getOrCreateLegacyId}, after the world's headless table was restored) and the result its FC
 * form. An existing FC entry is never overwritten.</p>
 *
 * <p>The imported entries are tracked and removed again by {@link #clear} before every re-injection and at server
 * stop, so no FC static keeps a previous world's headless ids.</p>
 */
// FCMOD-INTEGRATION(1.20.1): recipes: edible unmapped cooking inputs join FC's furnace and campfire tables
public final class ModernCookingImport {

    private static final Logger LOGGER = LogManager.getLogger("BTW-RecipeBridge");

    /** Type priority for the one result each input gets. */
    private static final List<RecipeType<?>> TYPES =
            List.of(RecipeType.SMELTING, RecipeType.SMOKING, RecipeType.CAMPFIRE_COOKING);

    /** Imported FurnaceRecipes entries: input id -> the exact stack this class put there. */
    private static final Map<Integer, btw.modern.ItemStack> SMELTING = new LinkedHashMap<>();
    /** Imported campfire entries: input id -> the exact stack this class put there. */
    private static final Map<Integer, btw.modern.ItemStack> CAMPFIRE = new LinkedHashMap<>();

    private ModernCookingImport() {}

    /**
     * True if the item already has a 1.5.2 or FC id (an FC item, a vanilla 1.5.2 item, a flattened subtype, or the
     * item form of a mapped block). Never allocates a headless id.
     */
    public static boolean isMapped(Item item) {
        if (item == null || item == Items.AIR) return false;
        if (ProxyRegistry.getItemId(item) > 0) return true;
        // FCMOD-INTEGRATION(1.20.1): strata: item identity, so a deepslate form's item stays unmapped (its own recipes may import)
        return item instanceof BlockItem bi && ProxyRegistry.getBlockFormItemId(bi.getBlock()) > 0;
    }

    /**
     * Imports the cooking results of {@code recipes} (the map just installed) into FC's tables.
     *
     * @return {furnace entries added, campfire entries added}
     */
    public static synchronized int[] apply(Iterable<Recipe<?>> recipes, RegistryAccess access) {
        // input item -> {recipe, type index}; the best recipe per input by type priority, then id
        Map<Item, Recipe<?>> best = new IdentityHashMap<>();
        Map<Item, Integer> bestType = new IdentityHashMap<>();
        Map<Item, Boolean> eligible = new IdentityHashMap<>();

        for (Recipe<?> r : recipes) {
            if (r == null || r.getId() == null || RecipeBridge.isFcRecipeId(r.getId())) continue;
            int type = TYPES.indexOf(r.getType());
            if (type < 0 || !(r instanceof AbstractCookingRecipe)) continue;
            Ingredient ingredient = singleIngredient(r);
            if (ingredient == null) continue;
            ItemStack[] stacks;
            try {
                stacks = ingredient.getItems();
            } catch (Throwable t) {
                continue;
            }
            for (ItemStack s : stacks) {
                if (s == null || s.isEmpty()) continue;
                Item item = s.getItem();
                if (!eligible.computeIfAbsent(item, ModernCookingImport::isEligible)) continue;
                Recipe<?> current = best.get(item);
                int currentType = bestType.getOrDefault(item, Integer.MAX_VALUE);
                if (current == null || type < currentType
                        || (type == currentType && r.getId().toString().compareTo(current.getId().toString()) < 0)) {
                    best.put(item, r);
                    bestType.put(item, type);
                }
            }
        }

        // Deterministic headless-id allocation order: by registry name.
        List<Item> inputs = new ArrayList<>(best.keySet());
        inputs.sort(Comparator.comparing(ModernCookingImport::registryName));

        int furnace = 0;
        int campfire = 0;
        btw.modern.FurnaceRecipes furnaceRecipes = btw.modern.FurnaceRecipes.smelting();
        for (Item input : inputs) {
            AbstractCookingRecipe recipe = (AbstractCookingRecipe) best.get(input);
            ItemStack result;
            try {
                result = recipe.getResultItem(access);
            } catch (Throwable t) {
                continue;
            }
            if (result == null || result.isEmpty()) continue;
            int id = ProxyRegistry.getOrCreateLegacyId(input);
            if (id <= 0) continue;
            btw.modern.ItemStack fcResult = ItemStackHelper.toFcStack(result.copy());
            if (fcResult == null) continue;

            if (furnaceRecipes.getSmeltingResult(id) == null) {
                btw.modern.ItemStack stack = fcResult.copy();
                furnaceRecipes.addSmelting(id, stack, recipe.getExperience());
                SMELTING.put(id, stack);
                furnace++;
            }
            if (FcCraftingManagers.campfireGet(id) == null) {
                btw.modern.ItemStack stack = fcResult.copy();
                if (FcCraftingManagers.campfireAdd(id, stack)) {
                    CAMPFIRE.put(id, stack);
                    campfire++;
                }
            }
            LOGGER.debug("Cooking import: {} (fc {}) -> {} from {}", registryName(input), id,
                    registryName(result.getItem()), recipe.getId());
        }

        LOGGER.info("Modern cooking import: {} furnace / brick-oven and {} campfire entries for {}"
                + " edible unmapped input(s)", furnace, campfire, inputs.size());
        return new int[]{furnace, campfire};
    }

    /** Removes every entry {@link #apply} added that is still the one it put there. */
    @SuppressWarnings("rawtypes")
    public static synchronized void clear() {
        if (!SMELTING.isEmpty()) {
            Map list = btw.modern.FurnaceRecipes.smelting().getSmeltingList();
            for (Map.Entry<Integer, btw.modern.ItemStack> e : SMELTING.entrySet()) {
                if (list.get(e.getKey()) == e.getValue()) list.remove(e.getKey());
            }
            SMELTING.clear();
        }
        if (!CAMPFIRE.isEmpty()) {
            for (Map.Entry<Integer, btw.modern.ItemStack> e : CAMPFIRE.entrySet()) {
                if (FcCraftingManagers.campfireGet(e.getKey()) == e.getValue()) {
                    FcCraftingManagers.campfireRemove(e.getKey());
                }
            }
            CAMPFIRE.clear();
        }
    }

    private static boolean isEligible(Item item) {
        try {
            return !isMapped(item) && new ItemStack(item).isEdible();
        } catch (Throwable t) {
            return false;
        }
    }

    /** The recipe's one non-empty ingredient, or null when it has none or several. */
    private static Ingredient singleIngredient(Recipe<?> r) {
        Ingredient found = null;
        try {
            for (Ingredient ing : r.getIngredients()) {
                if (ing == null || ing.isEmpty()) continue;
                if (found != null) return null;
                found = ing;
            }
        } catch (Throwable t) {
            return null;
        }
        return found;
    }

    private static String registryName(Item item) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        return key != null ? key.toString() : String.valueOf(item);
    }
}
