package btw.forge;

import btw.forge.removed.RemovedContent;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Applies BTW 4.B's recipe removals to the 1.20.1 recipe map that {@link RecipeBridge} is about to install
 * (compat workstream "recipes", Part A, generalised; research report removed_blocks.md section 4.3).
 *
 * <p><b>Maintainer ruling:</b> "blocks that 4.B explicitly removed must stay removed." Recipe decision 3 (a): FC's
 * vanilla-recipe removals are mirrored into {@code minecraft:*} recipes, which also fixed the former CHANGELOG known
 * issue "BTW recipe removals are not applied" (now Changed: "Minecraft recipes that BTW 4.B removed ... are gone").</p>
 *
 * <p>Four passes, all on the map before {@code RecipeManager.replaceRecipes}, so they survive {@code /reload} through
 * {@link RecipeBridge#onDatapackSync} and reach clients (and JEI) through the normal recipe sync:</p>
 * <ol>
 *   <li><b>FC records</b> (switch {@link RecipeBridge#MIRROR_FC_VANILLA_REMOVALS}): every
 *       {@code RemoveVanillaRecipe} / {@code RemoveVanillaShapelessRecipe} call (Server FCRecipes.java:5112-5760,
 *       recorded by Modern-Common CraftingManager) removes the {@code minecraft:} CRAFTING recipes whose result and
 *       ingredient multiset match. Shape is ignored (seven records are shaped in 1.5.2 and shapeless in 1.20.1).
 *       An FC ingredient (legacy id, metadata, 32767 = any) accepts a 1.20.1 {@link Ingredient} if ANY of its items
 *       (tags resolved) maps back to that id and metadata. A result count that changed in 1.20.1 (dyes, iron door)
 *       matches only when no exact-count recipe does, and is logged. A record whose recipe an earlier record already
 *       removed (the charcoal torch) counts as matched. Results are equivalent across a 1.20.1 recipe group whose
 *       results all map to one legacy block (the 16 bed colours, all block 26); new wood species are unmapped, so
 *       planks, stairs, buttons and trapdoors never form such a class. A record that matches nothing by ingredients
 *       but whose result FC's own list makes (the 6-stick fence, FCRecipes.java:5651, against FC's fences at
 *       :4987-4996) removes every {@code minecraft:} CRAFTING recipe of that result instead, counted "result-only"
 *       (A2): the FC workbench falls back to every RecipeManager recipe, so a surviving {@code minecraft:oak_fence}
 *       would bypass FC's own route.</li>
 *   <li><b>Special recipes</b> (same switch): FC dropped the map-extending recipe (vanilla/server
 *       CraftingManager.java:49-51) and the two-tool repair (:283-382). Every recipe with the
 *       {@code MAP_EXTENDING} or {@code REPAIR_ITEM} serializer is removed.</li>
 *   <li><b>Cooking</b> (same switch): FC's 14 {@code getSmeltingList().remove} calls (FCRecipes.java:5763-5776)
 *       are read as the difference between the 1.5.2 vanilla furnace table (vanilla/server FurnaceRecipes.java:37-55)
 *       and FC's live furnace list: an input FC dropped, or now cooks into something else, is removed. Clay keeps its
 *       recipe (FC re-adds the same brick, FCRecipes.java:3028) and glass was never a 1.5.2 input. Every
 *       {@code minecraft:} SMELTING, BLASTING, SMOKING and CAMPFIRE_COOKING recipe whose ingredient maps to a removed
 *       input is removed, and so is one whose ingredient FC's own furnace cooks into a different result than the
 *       recipe's (raw iron counts as BTW's iron ore chunk, which FC cooks into a nugget, so 1.20.1's raw iron to iron
 *       ingot goes). A recipe whose result has no legacy key is never compared on the second rule.</li>
 *   <li><b>Stays removed</b> (always on, by ruling): every recipe of any type and any namespace whose result is a
 *       {@link RemovedContent} item (T1, T2, T3) is removed; removals outside {@code minecraft} are logged at INFO.
 *       FC's own injected recipes are never removed (FC is the authority on what 4.B makes); one that produces a
 *       removed id is reported as a WARN instead.</li>
 * </ol>
 *
 * <p>Lives in {@code btw.forge} for the package-private legacy-id helpers ({@link LegacyVariants#toLegacy},
 * {@link ProxyRegistry#getLegacyVariantMeta}): the MC-to-legacy mapping here must never allocate a headless id,
 * which {@link ItemStackHelper#toFcStack} would for every unmapped modded item.</p>
 */
// FCMOD-INTEGRATION(1.20.1): stays-removed: FC's vanilla-recipe removals and the removed-id table reach the RecipeManager
final class FcRecipeRemovals {

    private static final Logger LOGGER = LogManager.getLogger("BTW-RecipeRemovals");

    /** 1.5.2 "any metadata" (CraftingManager.java:206). */
    private static final int WILDCARD = 32767;

    /**
     * The 1.5.2 vanilla furnace table, {input id, output id, output damage} (vanilla/server FurnaceRecipes.java:37-55).
     * Literal ids: Modern-Common's own copy (FurnaceRecipes.ensureVanillaRecipes) lacks the netherrack line, and
     * btw.modern.Item has no static for item 405.
     */
    private static final int[][] VANILLA_SMELTING = {
            {15, 265, 0},   // iron ore -> iron ingot
            {14, 266, 0},   // gold ore -> gold ingot
            {56, 264, 0},   // diamond ore -> diamond
            {12, 20, 0},    // sand -> glass
            {319, 320, 0},  // raw pork -> cooked pork
            {363, 364, 0},  // raw beef -> steak
            {365, 366, 0},  // raw chicken -> cooked chicken
            {349, 350, 0},  // raw fish -> cooked fish
            {4, 1, 0},      // cobblestone -> stone
            {337, 336, 0},  // clay ball -> brick
            {81, 351, 2},   // cactus -> cactus green
            {17, 263, 1},   // log -> charcoal
            {129, 388, 0},  // emerald ore -> emerald
            {392, 393, 0},  // potato -> baked potato
            {87, 405, 0},   // netherrack -> nether brick (item)
            {16, 263, 0},   // coal ore -> coal
            {73, 331, 0},   // redstone ore -> redstone
            {21, 351, 4},   // lapis ore -> lapis
            {153, 406, 0},  // nether quartz ore -> quartz
    };

    private static final Set<RecipeType<?>> COOKING_TYPES = Set.of(
            RecipeType.SMELTING, RecipeType.BLASTING, RecipeType.SMOKING, RecipeType.CAMPFIRE_COOKING);

    /** FC-injected recipe ids already reported for producing a removed id (once per id per JVM). */
    private static final Set<ResourceLocation> FC_RESULT_WARNED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private FcRecipeRemovals() {}

    /**
     * Removes, in place, every recipe the rules above select from {@code all} (id -> recipe, FC recipes included).
     *
     * @param mirrorFcRemovals passes 1-3 (Part A, maintainer decision "Recipe bridge" (a)); pass 4 always runs
     * @return a one-line summary (also logged at INFO)
     */
    static String apply(Map<ResourceLocation, Recipe<?>> all, RegistryAccess access, boolean mirrorFcRemovals) {
        Pass pass = new Pass(all, access);
        if (mirrorFcRemovals) {
            pass.mirrorCraftingRecords();
            pass.removeSpecialRecipes();
            pass.removeCookingRecipes();
        }
        pass.removeRemovedResults();
        for (ResourceLocation id : pass.removed) all.remove(id);

        String summary = String.format(
                "FC vanilla-recipe removals %s: %d/%d records matched (%d count-insensitive, %d result-only,"
                        + " %d unmatched),"
                        + " %d crafting + %d special + %d cooking recipes removed (removed furnace inputs %s);"
                        + " stays-removed rule removed %d more (%d outside minecraft); %d recipes removed in total",
                mirrorFcRemovals ? "on" : "OFF", pass.recordsMatched, pass.recordsTotal, pass.recordsCountInsensitive,
                pass.recordsResultOnly, pass.recordsUnmatched, pass.craftingRemoved, pass.specialRemoved,
                pass.cookingRemoved, pass.removedFurnaceInputs, pass.resultRuleRemoved, pass.resultRuleForeign,
                pass.removed.size());
        LOGGER.info(summary);
        return summary;
    }

    /** Packs a 1.5.2 (id, metadata) pair. */
    private static long key(int id, int meta) {
        return ((long) id << 16) | (meta & 0xFFFF);
    }

    private static int idOf(long key) {
        return (int) (key >>> 16);
    }

    private static int metaOf(long key) {
        return (int) (key & 0xFFFF);
    }

    /** One removal pass over one recipe map (caches are per pass: /reload builds fresh Ingredient objects). */
    private static final class Pass {
        final Map<ResourceLocation, Recipe<?>> all;
        final RegistryAccess access;
        final Set<ResourceLocation> removed = new LinkedHashSet<>();

        final Map<Item, Long> keyCache = new IdentityHashMap<>();
        final Map<Ingredient, long[]> ingredientKeys = new IdentityHashMap<>();
        final Map<Recipe<?>, ItemStack> results = new IdentityHashMap<>();
        Map<Item, Set<Item>> equivalents;
        /** 1.5.2 (id, metadata) keys of every result FC's own crafting list makes (A2), built on first use. */
        Set<Long> fcOutputs;

        int recordsTotal;
        int recordsMatched;
        int recordsCountInsensitive;
        int recordsResultOnly;
        int recordsUnmatched;
        int craftingRemoved;
        int specialRemoved;
        int cookingRemoved;
        int resultRuleRemoved;
        int resultRuleForeign;
        Set<Integer> removedFurnaceInputs = Collections.emptySet();

        Pass(Map<ResourceLocation, Recipe<?>> all, RegistryAccess access) {
            this.all = all;
            this.access = access;
        }

        // ---- pass 1: FC records ----------------------------------------------------------------------

        void mirrorCraftingRecords() {
            List<btw.modern.CraftingManager.RemovedVanillaRecipe> records =
                    btw.modern.CraftingManager.getRemovedVanillaRecipes();
            recordsTotal = records.size();

            List<Recipe<?>> pool = new ArrayList<>();
            for (Recipe<?> r : all.values()) {
                if (r.getType() == RecipeType.CRAFTING && isMinecraft(r.getId())) pool.add(r);
            }

            List<String> unmatched = new ArrayList<>();
            for (btw.modern.CraftingManager.RemovedVanillaRecipe record : records) {
                if (record.output == null || record.ingredients.isEmpty()) {
                    recordsUnmatched++;
                    unmatched.add(describe(record));
                    continue;
                }
                long fcResult = key(record.output.itemID, record.output.getItemDamage());
                List<Recipe<?>> exact = new ArrayList<>();
                List<Recipe<?>> otherCount = new ArrayList<>();
                for (Recipe<?> r : pool) {
                    ItemStack result = result(r);
                    if (result.isEmpty() || !resultMatches(result.getItem(), fcResult)) continue;
                    if (!ingredientsMatch(record.ingredients, r)) continue;
                    (result.getCount() == record.output.stackSize ? exact : otherCount).add(r);
                }
                List<Recipe<?>> hits = exact.isEmpty() ? otherCount : exact;
                if (hits.isEmpty()) {
                    // FCMOD-INTEGRATION(1.20.1): recipes: A2, a record with no 1.20.1 counterpart still removes the
                    // minecraft: recipes of its result when FC's own list makes that result (the FC workbench offers
                    // every RecipeManager recipe, so a surviving one would bypass FC's route)
                    List<Recipe<?>> byResult = fcOwnListMakes(fcResult) ? resultOnlyHits(pool, fcResult) : List.of();
                    if (byResult.isEmpty()) {
                        recordsUnmatched++;
                        unmatched.add(describe(record));
                        continue;
                    }
                    recordsMatched++;
                    recordsResultOnly++;
                    for (Recipe<?> r : byResult) {
                        if (removed.add(r.getId())) {
                            craftingRemoved++;
                            LOGGER.info("Removed {} by result only: FC removal {} has no 1.20.1 counterpart and FC's"
                                    + " own recipes make {}:{}", r.getId(), describe(record), idOf(fcResult),
                                    metaOf(fcResult));
                        }
                    }
                    continue;
                }
                recordsMatched++;
                if (exact.isEmpty()) {
                    recordsCountInsensitive++;
                    LOGGER.info("FC removal {} matched {} only with a different 1.20.1 result count", describe(record),
                            ids(hits));
                }
                for (Recipe<?> r : hits) {
                    if (removed.add(r.getId())) {
                        craftingRemoved++;
                        LOGGER.debug("Removed {} (FC removal {})", r.getId(), describe(record));
                    }
                }
            }
            // Expected: none. The one record without a 1.20.1 counterpart, the 6-stick fence (85:0 x2,
            // FCRecipes.java:5651), is matched by result only above (FC makes fences its own way, :4987-4996).
            for (String u : unmatched) {
                LOGGER.warn("FC removal {} matched no minecraft: crafting recipe", u);
            }
        }

        /** True if FC's own crafting list (shaped or shapeless) makes the 1.5.2 result (id, metadata). */
        boolean fcOwnListMakes(long fcResult) {
            if (fcOutputs == null) {
                Set<Long> outputs = new java.util.HashSet<>();
                try {
                    for (Object o : btw.modern.CraftingManager.getInstance().getRecipeList()) {
                        btw.modern.ItemStack out = null;
                        if (o instanceof btw.modern.ShapedRecipes shaped) {
                            out = shaped.getRecipeOutput();
                        } else if (o instanceof btw.modern.ShapelessRecipes shapeless) {
                            out = shapeless.getRecipeOutput();
                        }
                        if (out != null) outputs.add(key(out.itemID, out.getItemDamage()));
                    }
                } catch (Throwable t) {
                    LOGGER.debug("Could not list FC's own crafting outputs: {}", t.toString());
                }
                fcOutputs = outputs;
            }
            return fcOutputs.contains(fcResult);
        }

        /** The minecraft: CRAFTING recipes whose result is the 1.5.2 (id, metadata), equivalents included. */
        List<Recipe<?>> resultOnlyHits(List<Recipe<?>> pool, long fcResult) {
            List<Recipe<?>> out = new ArrayList<>();
            for (Recipe<?> r : pool) {
                ItemStack result = result(r);
                if (!result.isEmpty() && resultMatches(result.getItem(), fcResult)) out.add(r);
            }
            return out;
        }

        boolean resultMatches(Item item, long fcResult) {
            if (legacyKey(item) == fcResult) return true;
            Set<Item> eq = equivalents().get(item);
            if (eq != null) {
                for (Item other : eq) {
                    if (legacyKey(other) == fcResult) return true;
                }
            }
            return false;
        }

        /**
         * 1.20.1 crafting groups whose results all map to ONE legacy block (same id and subtype): one 1.5.2 result
         * split into many 1.20.1 items. Today: the 16 bed colours (group "bed", ProxyRegistry.java:450-466).
         */
        Map<Item, Set<Item>> equivalents() {
            if (equivalents != null) return equivalents;
            Map<String, Set<Item>> groups = new HashMap<>();
            for (Recipe<?> r : all.values()) {
                if (r.getType() != RecipeType.CRAFTING || !isMinecraft(r.getId())) continue;
                String group = safeGroup(r);
                if (group.isEmpty()) continue;
                ItemStack result = result(r);
                if (result.isEmpty()) continue;
                groups.computeIfAbsent(group, g -> new LinkedHashSet<>()).add(result.getItem());
            }
            equivalents = new IdentityHashMap<>();
            for (Set<Item> members : groups.values()) {
                if (members.size() < 2 || !sameLegacyBlock(members)) continue;
                for (Item member : members) equivalents.put(member, members);
            }
            return equivalents;
        }

        private static boolean sameLegacyBlock(Set<Item> members) {
            int id = -1;
            int meta = Integer.MIN_VALUE;
            for (Item item : members) {
                if (!(item instanceof BlockItem bi)) return false;
                Block block = bi.getBlock();
                int blockId = ProxyRegistry.getBlockId(block);
                if (blockId <= 0) return false;
                int blockMeta = ProxyRegistry.getLegacyVariantMeta(block);
                if (id == -1) {
                    id = blockId;
                    meta = blockMeta;
                } else if (id != blockId || meta != blockMeta) {
                    return false;
                }
            }
            return id > 0;
        }

        /** Multiset match, shape ignored: a perfect bipartite matching of FC ingredients onto 1.20.1 ingredients. */
        boolean ingredientsMatch(List<btw.modern.ItemStack> fc, Recipe<?> recipe) {
            List<Ingredient> mc = new ArrayList<>();
            try {
                for (Ingredient ing : recipe.getIngredients()) {
                    if (ing != null && !ing.isEmpty()) mc.add(ing);
                }
            } catch (Throwable t) {
                return false;
            }
            int n = fc.size();
            if (mc.size() != n) return false;
            boolean[][] accepts = new boolean[n][n];
            for (int i = 0; i < n; i++) {
                boolean any = false;
                for (int j = 0; j < n; j++) {
                    accepts[i][j] = accepts(mc.get(j), fc.get(i));
                    any |= accepts[i][j];
                }
                if (!any) return false;
            }
            int[] owner = new int[n];
            java.util.Arrays.fill(owner, -1);
            for (int i = 0; i < n; i++) {
                if (!augment(i, accepts, new boolean[n], owner)) return false;
            }
            return true;
        }

        private static boolean augment(int i, boolean[][] accepts, boolean[] seen, int[] owner) {
            for (int j = 0; j < owner.length; j++) {
                if (!accepts[i][j] || seen[j]) continue;
                seen[j] = true;
                if (owner[j] < 0 || augment(owner[j], accepts, seen, owner)) {
                    owner[j] = i;
                    return true;
                }
            }
            return false;
        }

        /** True if any item of the 1.20.1 ingredient is the FC ingredient's (id, metadata); 32767 = any metadata. */
        boolean accepts(Ingredient ingredient, btw.modern.ItemStack fc) {
            if (fc == null) return false;
            int id = fc.itemID;
            int meta = fc.getItemDamage();
            for (long k : keysOf(ingredient)) {
                if (k >= 0 && idOf(k) == id && (meta == WILDCARD || metaOf(k) == meta)) return true;
            }
            return false;
        }

        // ---- pass 2: special recipes -----------------------------------------------------------------

        void removeSpecialRecipes() {
            for (Recipe<?> r : all.values()) {
                if (RecipeBridge.isFcRecipeId(r.getId())) continue;
                RecipeSerializer<?> serializer;
                try {
                    serializer = r.getSerializer();
                } catch (Throwable t) {
                    continue;
                }
                if (serializer == RecipeSerializer.MAP_EXTENDING || serializer == RecipeSerializer.REPAIR_ITEM) {
                    if (removed.add(r.getId())) {
                        specialRemoved++;
                        LOGGER.debug("Removed special recipe {} (FC dropped map extending and the two-tool repair)",
                                r.getId());
                    }
                }
            }
        }

        // ---- pass 3: cooking -------------------------------------------------------------------------

        // FCMOD-INTEGRATION(1.20.1): raw_ore_chunks: a minecraft: cooking recipe goes when FC removed its 1.5.2 input or when FC's own furnace cooks that input into something else (raw iron, a BTW iron ore chunk, cooks to a nugget)
        @SuppressWarnings("rawtypes")
        void removeCookingRecipes() {
            removedFurnaceInputs = removedFurnaceInputs();
            Map fcList = btw.modern.FurnaceRecipes.smelting().getSmeltingList();
            for (Recipe<?> r : all.values()) {
                if (!COOKING_TYPES.contains(r.getType()) || !isMinecraft(r.getId()) || removed.contains(r.getId())) {
                    continue;
                }
                String why = disownedByFc(r, fcList);
                if (why != null && removed.add(r.getId())) {
                    cookingRemoved++;
                    LOGGER.debug("Removed cooking recipe {} ({})", r.getId(), why);
                }
            }
        }

        /** Why 4.B's furnace disowns {@code r}, or null. A result with no legacy key is never compared (kept). */
        @SuppressWarnings("rawtypes")
        private String disownedByFc(Recipe<?> r, Map fcList) {
            try {
                long resultKey = legacyKey(result(r).getItem());
                for (Ingredient ing : r.getIngredients()) {
                    if (ing == null || ing.isEmpty()) continue;
                    for (long k : keysOf(ing)) {
                        if (k < 0) continue;
                        int id = idOf(k);
                        if (removedFurnaceInputs.contains(id)) return "FC removed its 1.5.2 furnace input " + id;
                        if (resultKey >= 0 && fcList.get(id) instanceof btw.modern.ItemStack out
                                && resultKey != key(out.itemID, out.getItemDamage())) {
                            return "FC cooks " + id + " into " + out.itemID + ":" + out.getItemDamage();
                        }
                    }
                }
            } catch (Throwable ignored) {
                // a recipe that cannot list its ingredients is left alone
            }
            return null;
        }

        /** 1.5.2 vanilla furnace inputs that FC's live furnace list no longer cooks into the vanilla result. */
        @SuppressWarnings("rawtypes")
        private static Set<Integer> removedFurnaceInputs() {
            Set<Integer> out = new TreeSet<>();
            Map fcList = btw.modern.FurnaceRecipes.smelting().getSmeltingList();
            for (int[] v : VANILLA_SMELTING) {
                Object current = fcList.get(v[0]);
                if (!(current instanceof btw.modern.ItemStack stack)
                        || stack.itemID != v[1] || stack.getItemDamage() != v[2]) {
                    out.add(v[0]);
                }
            }
            return out;
        }

        // ---- pass 4: stays removed -------------------------------------------------------------------

        void removeRemovedResults() {
            for (Recipe<?> r : all.values()) {
                ItemStack result = result(r);
                if (result.isEmpty() || !RemovedContent.isRemovedRecipeResult(result.getItem())) continue;
                ResourceLocation id = r.getId();
                if (RecipeBridge.isFcRecipeId(id)) {
                    if (FC_RESULT_WARNED.add(id)) {
                        LOGGER.warn("FC recipe {} produces removed id {}; kept (FC decides what 4.B makes)", id,
                                result.getItem());
                    }
                    continue;
                }
                if (!removed.add(id)) continue;
                resultRuleRemoved++;
                if (isMinecraft(id)) {
                    LOGGER.debug("Removed {} (result {} stays removed)", id, result.getItem());
                } else {
                    resultRuleForeign++;
                    LOGGER.info("Removed {} from another namespace: its result {} stays removed (4.B removed it)", id,
                            result.getItem());
                }
            }
        }

        // ---- legacy-id helpers -----------------------------------------------------------------------

        ItemStack result(Recipe<?> r) {
            ItemStack cached = results.get(r);
            if (cached != null) return cached;
            ItemStack result;
            try {
                result = r.getResultItem(access);
                if (result == null) result = ItemStack.EMPTY;
            } catch (Throwable t) {
                result = ItemStack.EMPTY;
            }
            results.put(r, result);
            return result;
        }

        long[] keysOf(Ingredient ingredient) {
            long[] cached = ingredientKeys.get(ingredient);
            if (cached != null) return cached;
            long[] keys;
            try {
                ItemStack[] stacks = ingredient.getItems();
                keys = new long[stacks.length];
                for (int i = 0; i < stacks.length; i++) {
                    keys[i] = stacks[i] == null || stacks[i].isEmpty() ? -1 : legacyKey(stacks[i].getItem());
                }
            } catch (Throwable t) {
                keys = new long[0];
            }
            ingredientKeys.put(ingredient, keys);
            return keys;
        }

        /**
         * The 1.5.2 (id, metadata) of a 1.20.1 item as {@link #key}, or -1: the flattened-subtype table first, then the
         * 1.5.2 item table / FC items, then the block form with its subtype. Mirrors ItemStackHelper.toFcStack without
         * ever creating a headless id.
         */
        long legacyKey(Item item) {
            if (item == null || item == Items.AIR) return -1;
            Long cached = keyCache.get(item);
            if (cached != null) return cached;
            long k = -1;
            try {
                int[] variant = LegacyVariants.toLegacy(item);
                if (variant != null && variant[0] > 0) {
                    k = key(variant[0], variant[1]);
                } else {
                    int id = ProxyRegistry.getItemId(item);
                    if (id > 0) {
                        k = key(id, 0);
                    } else if (item instanceof BlockItem bi) {
                        // FCMOD-INTEGRATION(1.20.1): strata: world identity on purpose (not getBlockFormItemId): a deepslate ore item is that ore for 4.B's cooking rule
                        int blockId = ProxyRegistry.getBlockId(bi.getBlock());
                        if (blockId > 0) k = key(blockId, Math.max(0, ProxyRegistry.getLegacyVariantMeta(bi.getBlock())));
                    }
                }
            } catch (Throwable ignored) {
                k = -1;
            }
            keyCache.put(item, k);
            return k;
        }

        private static String safeGroup(Recipe<?> r) {
            try {
                String g = r.getGroup();
                return g == null ? "" : g;
            } catch (Throwable t) {
                return "";
            }
        }

        private static boolean isMinecraft(ResourceLocation id) {
            return id != null && "minecraft".equals(id.getNamespace());
        }

        private static List<ResourceLocation> ids(List<Recipe<?>> recipes) {
            List<ResourceLocation> out = new ArrayList<>();
            for (Recipe<?> r : recipes) out.add(r.getId());
            return out;
        }

        private static String describe(btw.modern.CraftingManager.RemovedVanillaRecipe record) {
            StringBuilder sb = new StringBuilder(record.shaped ? "shaped " : "shapeless ");
            btw.modern.ItemStack out = record.output;
            sb.append(out == null ? "null" : out.itemID + ":" + out.getItemDamage() + "x" + out.stackSize);
            sb.append(" <- [");
            for (int i = 0; i < record.ingredients.size(); i++) {
                btw.modern.ItemStack in = record.ingredients.get(i);
                if (i > 0) sb.append(", ");
                sb.append(in == null ? "null" : in.itemID + ":" + (in.getItemDamage() == WILDCARD ? "*" : in.getItemDamage()));
            }
            return sb.append(']').toString();
        }
    }
}
