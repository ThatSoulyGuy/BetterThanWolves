package btw.forge;

import btw.forge.recipes.ModernCookingImport;
import btw.forge.recipes.ModernLogChoppingRecipe;
import btw.forge.recipes.RawCraftedFoods;
import btw.forge.recipes.RecipeReplacements;
import btw.forge.recipes.TagMachineRecipes;
import btw.forge.recipes.WorkbenchFallback;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.ref.WeakReference;
import java.util.*;

/**
 * Converts FC recipes (from btw.modern.CraftingManager and btw.modern.FurnaceRecipes)
 * into MC 1.20.1 recipes and injects them into the server's RecipeManager.
 *
 * <p>Called after FC initialization completes and the server's recipe manager is available,
 * and again after a datapack reload (/reload builds a fresh RecipeManager without them).
 * FC recipes are keyed {@code betterthanwolves:fc_recipe_<index>} /
 * {@code betterthanwolves:fc_smelting_<inputId>}; an existing recipe with the same id is
 * replaced.</p>
 *
 * <p>Injection goes through {@link RecipeManager#replaceRecipes}, which rebuilds BOTH of the
 * manager's indexes ({@code recipes} by type and {@code byName} by id, RecipeManager.java:37-38).
 * Writing only the by-type map left {@code byKey()} blind to FC recipes, so every login
 * dropped them from the player's recipe book ("Tried to load unrecognized recipe ...
 * removed now", ServerRecipeBook.java:97-105) and recipe-book placement did nothing
 * (triage RE6).</p>
 *
 * <p>Before the map is installed, {@link FcRecipeRemovals} removes what BTW 4.B removed: FC's
 * vanilla-recipe removals (behind {@link #MIRROR_FC_VANILLA_REMOVALS}) and, always, every recipe
 * whose result is a {@link btw.forge.removed.RemovedContent} id ("blocks that 4.B explicitly
 * removed must stay removed"). {@link RetiredIngredientWidening} then lets recipes that consume a
 * removed id accept its BTW successor. Working on the same map keeps both across /reload.</p>
 *
 * <p><b>Recipe bridge (INTEGRATIONS #025):</b> 1.20.1 recipes and other mods' recipes follow FC's
 * recipe rules. {@link #injectRecipes} runs these steps in order, each in its own try/catch that
 * logs one ERROR and leaves the recipes as they were:</p>
 * <ol>
 *   <li>clear the previous cooking and millstone imports (FC statics hold no stale headless ids);</li>
 *   <li>collect the loaded non-FC recipes and inject FC's crafting and smelting recipes, then 4.B's stair and
 *       slab rules for the newer woods ({@link ModernWoodSubBlocks#contributeRecipes});</li>
 *   <li>{@link RecipeReplacements}: data-driven replacements by id ({@code btw_recipe_replacements});</li>
 *   <li>{@link FcRecipeRemovals}: 4.B's removals, the result-only fence rule and the stays-removed rule;</li>
 *   <li>{@link RetiredIngredientWidening}: removed ingredients also accept their BTW successor (furnace to
 *       brick oven and tripwire hook to FC iron nugget included);</li>
 *   <li>install the map ({@code replaceRecipes}) and index its {@link ModernLogChoppingRecipe}s for FC's saw;</li>
 *   <li>{@link ModernCookingImport}: edible unmapped cooking inputs join FC's furnace and campfire tables;</li>
 *   <li>{@link TagMachineRecipes}: item tags feed the FC millstone ({@code btw_machine_recipes});</li>
 *   <li>{@link RawCraftedFoods}: crafted foods made from poisonous raw items are marked for food-core;</li>
 *   <li>{@link WorkbenchFallback}: the FC workbench, work stump and soulforge fall back to every non-FC
 *       RecipeManager crafting recipe after FC's own lists (installed with Part A's switch, so one switch
 *       reverts A, the fence rule and the fallback together).</li>
 * </ol>
 * <p>It runs at ServerStarting, after the overworld's LevelEvent.Load restored the world's headless ids
 * (FcWorldSavedData), and again after every /reload ({@link #onDatapackSync}), never earlier: steps 7 and 8
 * hand out headless ids.</p>
 */
@Mod.EventBusSubscriber(modid = BTWForgeMod.MOD_ID)
public class RecipeBridge {

    private static final Logger LOGGER = LogManager.getLogger("BTW-RecipeBridge");

    /**
     * Compat workstream "recipes" Part A: mirror FC's vanilla-recipe removals (FCRecipes.RemoveVanillaRecipes,
     * its 14 furnace removals and the dropped special recipes) onto the 1.20.1 RecipeManager. Maintainer decision
     * "Recipe bridge" (a): on. The stays-removed result rule (RemovedContent) is not behind this switch.
     */
    // FCMOD-INTEGRATION(1.20.1): stays-removed: one switch reverts Part A without touching the removed-id rule
    static final boolean MIRROR_FC_VANILLA_REMOVALS = true;

    /** 1.5.2 "any damage" ingredient marker: CraftingManager.java:206 / ShapedRecipes.java:134. */
    private static final int FC_WILDCARD_DAMAGE = 32767;

    private static final String CRAFTING_PREFIX = "fc_recipe_";
    private static final String SMELTING_PREFIX = "fc_smelting_";

    /** The RecipeManager most recently injected, and one id injected into it (reload probe). */
    private static WeakReference<RecipeManager> lastInjected = new WeakReference<>(null);
    private static ResourceLocation injectedProbe;

    /**
     * Injects all FC recipes into the MC RecipeManager.
     * Called from BTWForgeMod.onServerStarting() after the server loads data packs.
     */
    public static synchronized void injectRecipes(MinecraftServer server) {
        try {
            RecipeManager recipeManager = server.getRecipeManager();
            RegistryAccess access = server.registryAccess();

            // FCMOD-INTEGRATION(1.20.1): recipes: the previous load's imports leave FC's static tables first, so
            // FC's own smelting list is exported below without them and no stale headless id survives a reload.
            runStep("clear the previous cooking and millstone imports", () -> {
                ModernCookingImport.clear();
                TagMachineRecipes.clear();
                return null;
            }, null);

            // Everything currently loaded (vanilla + datapacks), minus FC entries left over from
            // an earlier injection; FC recipes are then (re)added under their stable ids.
            Map<ResourceLocation, Recipe<?>> all = new LinkedHashMap<>();
            for (Recipe<?> recipe : recipeManager.getRecipes()) {
                if (!isFcRecipeId(recipe.getId())) {
                    all.put(recipe.getId(), recipe);
                }
            }

            int[] crafting = injectCraftingRecipes(all);
            int smeltingCount = injectSmeltingRecipes(all);

            // FCMOD-INTEGRATION(1.20.1): modern_wood_subblocks: 4.B's stair and slab rules for the four newer woods
            // (FCRecipes.java:877-901, :2396-2420, :2923-2929); before the replacements, so a pack's replacement by id
            // still wins, and before 4.B's removals, the widening and the workbench fallback, which see the result.
            int[] woods = mapStep(all, "Failed to add the newer woods' stair and slab recipes; they stay as loaded",
                    () -> ModernWoodSubBlocks.contributeRecipes(all, access), new int[3]);

            // FCMOD-INTEGRATION(1.20.1): recipes: replacements by id first, so the stays-removed rule and the
            // widening below also govern replacement recipes; after every pack loaded, so pack order is irrelevant.
            int[] replacements = mapStep(all, "Failed to apply BTW recipe replacements; recipes stay as loaded",
                    () -> RecipeReplacements.applyTo(all), new int[3]);

            // FCMOD-INTEGRATION(1.20.1): stays-removed: 4.B's recipe removals, applied to the same map so a
            // /reload re-injection (onDatapackSync) re-applies them; a failure leaves the recipes as before.
            int beforeRemovals = all.size();
            mapStep(all, "Failed to apply BTW recipe removals; vanilla recipes stay as loaded",
                    () -> FcRecipeRemovals.apply(all, access, MIRROR_FC_VANILLA_REMOVALS), null);
            int removedCount = beforeRemovals - all.size();

            // FCMOD-INTEGRATION(1.20.1): stays-removed: integrate, don't disable -- recipes that consume a removed
            // id (torch, chest, clay, nether brick, furnace, tripwire hook, ...) also accept its BTW successor; after
            // the removals, so only surviving recipes are widened, and on the same map, so /reload re-applies it.
            Map<ResourceLocation, Recipe<?>> beforeWidening = new HashMap<>(all);
            mapStep(all, "Failed to widen recipes that take removed ids; they stay as loaded",
                    () -> RetiredIngredientWidening.apply(all, access), null);
            int widenedCount = 0;
            for (Map.Entry<ResourceLocation, Recipe<?>> e : all.entrySet()) {
                if (beforeWidening.get(e.getKey()) != e.getValue()) widenedCount++;
            }

            recipeManager.replaceRecipes(all.values());

            // FCMOD-INTEGRATION(1.20.1): bamboo_planks: FC's saw follows the installed modern_log_chopping recipes
            // (ModernWoodSawHooks), so one data file drives the saw and the axe, and a /reload re-indexes it.
            runStep("index the modern log chopping recipes for the saw",
                    () -> ModernLogChoppingRecipe.rebuildSawIndex(all.values()), 0);

            lastInjected = new WeakReference<>(recipeManager);
            injectedProbe = all.keySet().stream().filter(RecipeBridge::isFcRecipeId).findFirst().orElse(null);

            LOGGER.info("Injected {} crafting ({} code-driven) + {} smelting FC recipes into RecipeManager"
                            + " ({} FC crafting recipes skipped: unconvertible)",
                    crafting[0], crafting[1], smeltingCount, crafting[2]);

            // FCMOD-INTEGRATION(1.20.1): recipes: FC's heat machines, millstone and food values follow the
            // installed map; these hand out headless ids, so they run only here, after the world's table loaded.
            int[] cooking = runStep("import modern cooking recipes into FC's furnace and campfire",
                    () -> ModernCookingImport.apply(all.values(), access), new int[2]);
            int millstone = runStep("apply tag-driven millstone recipes", TagMachineRecipes::apply, 0);
            int rawCrafted = runStep("mark raw-crafted foods", () -> RawCraftedFoods.apply(all.values(), access),
                    -1);

            // FCMOD-INTEGRATION(1.20.1): recipes: the FC workbench, work stump and soulforge fall back to this map
            // after FC's own lists; behind Part A's switch, so one switch reverts A, A2 and the fallback together.
            boolean fallback = runStep("install the FC workbench fallback", () -> {
                WorkbenchFallback.onRecipesReplaced();
                btw.modern.CraftingManager.setExternalResolver(
                        MIRROR_FC_VANILLA_REMOVALS ? WorkbenchFallback.INSTANCE : null);
                return MIRROR_FC_VANILLA_REMOVALS;
            }, false);

            LOGGER.info("[BTW-Recipes] {} recipes installed: FC {} crafting + {} smelting; replacements {} replaced,"
                            + " {} removed; 4.B removals {} recipes; widened {}; cooking import {} furnace + {}"
                            + " campfire; millstone tag recipes {}; raw-crafted foods {}; FC workbench fallback {};"
                            + " newer woods {} replaced, {} added",
                    all.size(), crafting[0], smeltingCount, replacements[0], replacements[1], removedCount,
                    widenedCount, cooking[0], cooking[1], millstone, rawCrafted < 0 ? "n/a" : rawCrafted,
                    fallback ? "on" : "OFF", woods[0], woods[1]);
        } catch (Throwable e) {
            LOGGER.error("Failed to inject FC recipes into the RecipeManager", e);
        }
    }

    /** One orchestration step: its own try/catch, one ERROR on failure, {@code fallback} returned instead. */
    private static <T> T runStep(String what, java.util.function.Supplier<T> step, T fallback) {
        try {
            return step.get();
        } catch (Throwable t) {
            LOGGER.error("Failed to {}; skipped", what, t);
            return fallback;
        }
    }

    /** A step that edits the recipe map in place: on failure, one ERROR and the map is restored as it was. */
    private static <T> T mapStep(Map<ResourceLocation, Recipe<?>> all, String error,
                                 java.util.function.Supplier<T> step, T fallback) {
        Map<ResourceLocation, Recipe<?>> snapshot = new LinkedHashMap<>(all);
        try {
            return step.get();
        } catch (Throwable t) {
            all.clear();
            all.putAll(snapshot);
            LOGGER.error(error, t);
            return fallback;
        }
    }

    /**
     * Registers the recipe bridge's data loaders (btw_recipe_replacements, btw_machine_recipes) with every server
     * data load; {@link #injectRecipes} applies what they read.
     */
    // FCMOD-INTEGRATION(1.20.1): recipes: data-driven replacements and machine recipes reload with the datapacks
    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new RecipeReplacements.Loader(event.getConditionContext()));
        event.addListener(new TagMachineRecipes.Loader(event.getConditionContext()));
    }

    /**
     * The world is gone: FC's static tables drop every import keyed to its headless ids, the FC grids stop asking the
     * previous RecipeManager, and the raw-crafted marks are cleared, so the next world in this JVM starts clean.
     */
    // FCMOD-INTEGRATION(1.20.1): recipes: no FC static keeps a previous world's imports after ServerStopped
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        runStep("remove the FC workbench fallback", () -> {
            btw.modern.CraftingManager.setExternalResolver(null);
            return null;
        }, null);
        runStep("clear the cooking imports", () -> {
            ModernCookingImport.clear();
            return null;
        }, null);
        runStep("clear the millstone tag recipes", () -> {
            TagMachineRecipes.clear();
            return null;
        }, null);
        runStep("clear the raw-crafted foods", () -> {
            btw.forge.food.FcFoodValues.setRawCrafted(Set.of());
            return null;
        }, null);
        runStep("reset the FC workbench fallback cache", () -> {
            WorkbenchFallback.onRecipesReplaced();
            return null;
        }, null);
        // FCMOD-INTEGRATION(1.20.1): bamboo_planks: the saw follows no previous world's chopping recipes
        runStep("clear the modern log chopping saw index", () -> {
            ModernLogChoppingRecipe.clearSawIndex();
            return null;
        }, null);
    }

    /**
     * /reload replaces the server's RecipeManager with one built from JSON only, which would
     * silently drop every FC recipe until restart. {@code OnDatapackSyncEvent} with no player
     * fires after the reload and before the recipes are sent to clients
     * (PlayerList.reloadResources); a join (player != null) needs nothing.
     */
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) return;
        try {
            MinecraftServer server = event.getPlayerList().getServer();
            RecipeManager current = server.getRecipeManager();
            boolean missing = lastInjected.get() != current
                    || (injectedProbe != null && current.byKey(injectedProbe).isEmpty());
            if (missing) {
                LOGGER.info("Recipe reload detected; re-injecting FC recipes");
                injectRecipes(server);
            }
        } catch (Throwable e) {
            LOGGER.error("Failed to re-inject FC recipes after a reload", e);
        }
    }

    /** True for the ids this bridge injects for FC ({@code fc_recipe_*}, {@code fc_smelting_*}). */
    // FCMOD-INTEGRATION(1.20.1): recipes: public for btw.forge.recipes (the fallback skips FC's own copies)
    public static boolean isFcRecipeId(ResourceLocation id) {
        return id != null && BTWForgeMod.MOD_ID.equals(id.getNamespace())
                && (id.getPath().startsWith(CRAFTING_PREFIX) || id.getPath().startsWith(SMELTING_PREFIX));
    }

    /** canonical 1.20.1 item -> its modern spelling (same 1.5.2 subtype, one-way); built once, after FC init. */
    private static volatile Map<net.minecraft.world.item.Item, net.minecraft.world.item.Item> legacyAliases;

    /**
     * The modern spelling 1.20.1 recipes want for the 4.B subtype whose FC stacks convert back to {@code canonical}
     * (ItemStackHelper.toMcStack), or null; one-way. These are LegacyVariants' modern-only spellings of a 1.5.2
     * subtype: the black, brown, blue and white dye items for ink sac, cocoa beans, lapis and bone meal (1.5.2 dye
     * 0/3/4/15, ItemDye.java:19; each dye keeps its own FC stack, 351:256/259/260/271, and never reads as the 4.B item,
     * which still stands in for it, as in 4.B), the stone slab for the smooth stone slab (44:0) and smooth sandstone
     * for cut sandstone (24:2). Lossy collapses (the 16 beds onto one) are not aliases; the modern flowers are their
     * own subtypes 38:1-10 (flower_subtypes).
     */
    // FCMOD-INTEGRATION(1.20.1): recipes: the FC workbench fallback retries a grid with these spellings (INTEGRATIONS #025)
    public static net.minecraft.world.item.Item legacyAlias(net.minecraft.world.item.Item canonical) {
        if (canonical == null) return null;
        Map<net.minecraft.world.item.Item, net.minecraft.world.item.Item> map = legacyAliases;
        if (map == null) {
            if (!BTWLifecycle.isFcReady()) return null; // the canonical pick needs FC's tables; never cache early
            map = buildLegacyAliases();
            legacyAliases = map;
        }
        return map.get(canonical);
    }

    private static Map<net.minecraft.world.item.Item, net.minecraft.world.item.Item> buildLegacyAliases() {
        Map<net.minecraft.world.item.Item, net.minecraft.world.item.Item> out = new IdentityHashMap<>();
        try {
            for (net.minecraft.world.item.Item item : BuiltInRegistries.ITEM) {
                int[] legacy = LegacyVariants.toLegacy(item);
                if (legacy == null || legacy[0] <= 0 || legacy[0] == LegacyVariants.SPAWN_EGG_ID) continue;
                // FCMOD-INTEGRATION(1.20.1): raw_ore_chunks: raw iron counts as the chunk one way; it is not a spelling of the chunk (no chunk -> raw iron alias in BTW grids)
                if (LegacyVariants.isCountsAs(item)) continue;
                // the base subtype's FC -> MC pick (white dye 351:271 -> bone meal); toMcStack makes no headless id
                net.minecraft.world.item.Item canonical = ItemStackHelper.toMcStack(
                        new btw.modern.ItemStack(legacy[0], 1, LegacyVariants.baseSubtype(legacy[1]))).getItem();
                if (canonical == null || canonical == net.minecraft.world.item.Items.AIR || canonical == item) continue;
                out.putIfAbsent(canonical, item); // registry order: deterministic
            }
        } catch (Throwable t) {
            LOGGER.warn("Could not list FC's legacy item aliases; the FC workbench fallback matches canonical items only",
                    t);
        }
        LOGGER.debug("FC legacy item aliases: {}", out);
        return Collections.unmodifiableMap(out);
    }

    /**
     * Converts FC crafting recipes to MC recipes: shaped + shapeless as
     * {@link FCCraftingRecipes.Shaped}/{@link FCCraftingRecipes.Shapeless}, and FC's code-driven
     * IRecipe classes (FCRecipesLogChopping / FCRecipesKnitting / FCRecipesFishingRodBaiting,
     * added at FCRecipes.java:5108-5110) as {@link FCCraftingRecipes.Custom}.
     *
     * @return {converted, of which code-driven, skipped}
     */
    private static int[] injectCraftingRecipes(Map<ResourceLocation, Recipe<?>> all) {
        List fcRecipes = btw.modern.CraftingManager.getInstance().getRecipeList();
        int count = 0;
        int customCount = 0;
        int skipped = 0;

        // Recipes are sent to clients by serializer registry key (ClientboundUpdateRecipesPacket
        // .toNetwork); a Custom recipe without its registered serializer would break that packet.
        boolean customSerializerRegistered =
                BuiltInRegistries.RECIPE_SERIALIZER.getKey(FCCraftingRecipes.CUSTOM_SERIALIZER) != null;
        if (!customSerializerRegistered) {
            LOGGER.error("Serializer {} is not registered; FC code-driven crafting recipes are not injected",
                    FCCraftingRecipes.CUSTOM_SERIALIZER_ID);
        }

        for (int i = 0; i < fcRecipes.size(); i++) {
            Object fcRecipe = fcRecipes.get(i);
            ResourceLocation id = new ResourceLocation(BTWForgeMod.MOD_ID, CRAFTING_PREFIX + i);

            try {
                Recipe<?> mcRecipe = null;

                if (fcRecipe instanceof btw.modern.ShapedRecipes shaped) {
                    mcRecipe = convertShaped(id, shaped);
                } else if (fcRecipe instanceof btw.modern.ShapelessRecipes shapeless) {
                    mcRecipe = convertShapeless(id, shapeless);
                } else if (fcRecipe instanceof btw.modern.IRecipe custom && customSerializerRegistered) {
                    mcRecipe = FCCraftingRecipes.custom(id, i, custom);
                    customCount++;
                }

                if (mcRecipe != null) {
                    all.put(id, mcRecipe);
                    count++;
                } else {
                    skipped++;
                }
            } catch (Throwable e) {
                skipped++;
                LOGGER.debug("Failed to convert FC recipe {}: {}", i, e.toString());
            }
        }

        return new int[] { count, customCount, skipped };
    }

    /**
     * Converts FC furnace recipes to MC smelting recipes.
     */
    @SuppressWarnings("unchecked")
    private static int injectSmeltingRecipes(Map<ResourceLocation, Recipe<?>> all) {
        Map<Integer, btw.modern.ItemStack> fcSmelting =
                btw.modern.FurnaceRecipes.smelting().getSmeltingList();
        int count = 0;

        for (Map.Entry<Integer, btw.modern.ItemStack> entry : fcSmelting.entrySet()) {
            int inputId = entry.getKey();
            btw.modern.ItemStack fcOutput = entry.getValue();

            // FCMOD-INTEGRATION(1.20.1): recipes: a headless input is a modern item's own recipe imported into FC's
            // table (ModernCookingImport); exporting it back would duplicate it as fc_smelting_25xxx
            if (inputId >= ProxyRegistry.HEADLESS_BASE_ID) continue;

            try {
                // Convert input ID to Ingredient
                Ingredient input = fcIdToIngredient(inputId);
                if (input.isEmpty()) continue;
                // FCMOD-INTEGRATION(1.20.1): raw_ore_chunks: an item that counts as the FC input cooks the same way, so a furnace and JEI show raw iron -> BTW iron nugget
                List<net.minecraft.world.item.Item> aliases = LegacyVariants.countsAsItems(inputId, 0);
                if (!aliases.isEmpty()) {
                    List<ItemStack> stacks = new ArrayList<>(Arrays.asList(input.getItems()));
                    for (net.minecraft.world.item.Item alias : aliases) stacks.add(new ItemStack(alias));
                    input = Ingredient.of(stacks.toArray(new ItemStack[0]));
                }

                // Convert output
                ItemStack output = ItemStackHelper.toMcStack(fcOutput);
                if (output.isEmpty()) continue;

                float xp = btw.modern.FurnaceRecipes.smelting().getExperience(inputId);

                ResourceLocation id = new ResourceLocation(
                        BTWForgeMod.MOD_ID, SMELTING_PREFIX + inputId);

                SmeltingRecipe recipe = new SmeltingRecipe(
                        id, "", CookingBookCategory.MISC,
                        input, output, xp, 200);

                all.put(id, recipe);
                count++;
            } catch (Exception e) {
                LOGGER.debug("Failed to convert FC smelting recipe for input {}: {}",
                        inputId, e.getMessage());
            }
        }

        return count;
    }

    /**
     * Converts an FC ShapedRecipes to an MC ShapedRecipe. Returns null when the output or any
     * ingredient has no MC mapping: an EMPTY ingredient in a shaped pattern means "this cell
     * must be empty", so keeping the recipe would let it be crafted without that ingredient.
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
                Ingredient ing = fcStackToIngredient(fcStack);
                if (ing.isEmpty()) {
                    LOGGER.debug("Skipping FC recipe {}: ingredient {} has no MC mapping", id, fcStack.itemID);
                    return null;
                }
                ingredients.set(i, ing);
            }
        }

        return new FCCraftingRecipes.Shaped(id, w, h, ingredients, output, fcRecipe);
    }

    /**
     * Converts an FC ShapelessRecipes to an MC ShapelessRecipe. Returns null when the output
     * or any ingredient has no MC mapping (dropping just that ingredient would make the
     * recipe cheaper than in FC).
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
                if (ing.isEmpty()) {
                    LOGGER.debug("Skipping FC recipe {}: ingredient {} has no MC mapping", id, fcStack.itemID);
                    return null;
                }
                ingredients.add(ing);
            }
        }

        if (ingredients.isEmpty()) return null;

        return new FCCraftingRecipes.Shapeless(id, output, ingredients, fcRecipe);
    }

    /**
     * Converts an FC ItemStack to an MC Ingredient (for recipe inputs).
     */
    private static Ingredient fcStackToIngredient(btw.modern.ItemStack fcStack) {
        List<ItemStack> alternatives = fcStackAlternatives(fcStack);
        if (alternatives.isEmpty()) return Ingredient.EMPTY;
        return Ingredient.of(alternatives.toArray(new ItemStack[0]));
    }

    /**
     * The MC stacks an FC recipe ingredient stands for. A plain stack converts to one stack.
     * The 1.5.2 wildcard damage (32767, "any metadata") stands for every subtype: for ids whose
     * subtypes are separate 1.20.1 items (wood species, wool colours, dyes, ...) one stack per
     * distinct item, otherwise the item at damage 0. Converting the wildcard as a literal
     * damage resolved e.g. "any planks" (FCRecipes.java:2838-2840) to jungle planks only
     * (32767 &amp; 3 == 3) and gave tools a nonsense durability. Also used by the JEI plugin.
     */
    public static List<ItemStack> fcStackAlternatives(btw.modern.ItemStack fcStack) {
        if (fcStack == null) return List.of();
        if (fcStack.getItemDamage() != FC_WILDCARD_DAMAGE) {
            ItemStack mc = ItemStackHelper.toMcStack(fcStack);
            return mc.isEmpty() ? List.of() : List.of(mc);
        }

        List<ItemStack> out = new ArrayList<>();
        ItemStack base = ItemStackHelper.toMcStack(
                new btw.modern.ItemStack(fcStack.itemID, fcStack.stackSize, 0));
        if (base.isEmpty()) return out;
        out.add(base);
        // 1.5.2 metadata subtypes span 0-15 (wool/dye colours, wood species in bits 0-1).
        for (int meta = 1; meta < 16; meta++) {
            ItemStack variant = ItemStackHelper.toMcStack(
                    new btw.modern.ItemStack(fcStack.itemID, fcStack.stackSize, meta));
            if (variant.isEmpty()) continue;
            boolean known = false;
            for (ItemStack seen : out) {
                if (seen.getItem() == variant.getItem()) {
                    known = true;
                    break;
                }
            }
            if (!known) out.add(variant);
        }
        return out;
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
