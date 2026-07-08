package btw.forge.data;

import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.function.Consumer;

/**
 * Datapack provider for BTW recipe overrides.
 *
 * Corresponds to INTEGRATIONS.md {@code #005 — Recipe overrides}.
 *
 * Scope (this revision):
 *   - Stonecutter slab output halved (2 → 1) across vanilla stone-like
 *     materials. Per Pattern C (rebalance): stonecutter stays available,
 *     but it's no longer a 2× efficiency multiplier over the crafting
 *     table, which currently lets players skip FC's stone-tool economy
 *     entirely.
 *
 * Out of scope (tracked separately):
 *   - FC-ingredient-gated recipes (lodestone, recovery compass, conduit)
 *     — these need stable resource locations for FC items, which is
 *     blocked on the tag-remediation work under #006. Promoted to the
 *     alchemy audit's "recipe-file anchors" list for follow-up.
 */
public class FCRecipeOverrides extends RecipeProvider {

    public FCRecipeOverrides(PackOutput output) {
        super(output);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> consumer) {
        // Stonecutter slab 2× → 1× across the major stone material set.
        // Each entry overrides the vanilla recipe file under
        // data/minecraft/recipes/<id>_from_<input>_stonecutting.json.
        halveStonecutterSlab(consumer, Items.STONE,                  Items.STONE_SLAB,                  "stone_slab_from_stone_stonecutting");
        halveStonecutterSlab(consumer, Items.COBBLESTONE,            Items.COBBLESTONE_SLAB,            "cobblestone_slab_from_cobblestone_stonecutting");
        halveStonecutterSlab(consumer, Items.MOSSY_COBBLESTONE,      Items.MOSSY_COBBLESTONE_SLAB,      "mossy_cobblestone_slab_from_mossy_cobblestone_stonecutting");
        halveStonecutterSlab(consumer, Items.SMOOTH_STONE,           Items.SMOOTH_STONE_SLAB,           "smooth_stone_slab_from_smooth_stone_stonecutting");
        halveStonecutterSlab(consumer, Items.STONE_BRICKS,           Items.STONE_BRICK_SLAB,            "stone_brick_slab_from_stone_bricks_stonecutting");
        halveStonecutterSlab(consumer, Items.MOSSY_STONE_BRICKS,     Items.MOSSY_STONE_BRICK_SLAB,      "mossy_stone_brick_slab_from_mossy_stone_bricks_stonecutting");
        halveStonecutterSlab(consumer, Items.GRANITE,                Items.GRANITE_SLAB,                "granite_slab_from_granite_stonecutting");
        halveStonecutterSlab(consumer, Items.POLISHED_GRANITE,       Items.POLISHED_GRANITE_SLAB,       "polished_granite_slab_from_polished_granite_stonecutting");
        halveStonecutterSlab(consumer, Items.DIORITE,                Items.DIORITE_SLAB,                "diorite_slab_from_diorite_stonecutting");
        halveStonecutterSlab(consumer, Items.POLISHED_DIORITE,       Items.POLISHED_DIORITE_SLAB,       "polished_diorite_slab_from_polished_diorite_stonecutting");
        halveStonecutterSlab(consumer, Items.ANDESITE,               Items.ANDESITE_SLAB,               "andesite_slab_from_andesite_stonecutting");
        halveStonecutterSlab(consumer, Items.POLISHED_ANDESITE,      Items.POLISHED_ANDESITE_SLAB,      "polished_andesite_slab_from_polished_andesite_stonecutting");
        halveStonecutterSlab(consumer, Items.SANDSTONE,              Items.SANDSTONE_SLAB,              "sandstone_slab_from_sandstone_stonecutting");
        halveStonecutterSlab(consumer, Items.SMOOTH_SANDSTONE,       Items.SMOOTH_SANDSTONE_SLAB,       "smooth_sandstone_slab_from_smooth_sandstone_stonecutting");
        halveStonecutterSlab(consumer, Items.RED_SANDSTONE,          Items.RED_SANDSTONE_SLAB,          "red_sandstone_slab_from_red_sandstone_stonecutting");
        halveStonecutterSlab(consumer, Items.SMOOTH_RED_SANDSTONE,   Items.SMOOTH_RED_SANDSTONE_SLAB,   "smooth_red_sandstone_slab_from_smooth_red_sandstone_stonecutting");
        halveStonecutterSlab(consumer, Items.PRISMARINE,             Items.PRISMARINE_SLAB,             "prismarine_slab_from_prismarine_stonecutting");
        halveStonecutterSlab(consumer, Items.PRISMARINE_BRICKS,      Items.PRISMARINE_BRICK_SLAB,       "prismarine_brick_slab_from_prismarine_bricks_stonecutting");
        halveStonecutterSlab(consumer, Items.DARK_PRISMARINE,        Items.DARK_PRISMARINE_SLAB,        "dark_prismarine_slab_from_dark_prismarine_stonecutting");
        halveStonecutterSlab(consumer, Items.NETHER_BRICKS,          Items.NETHER_BRICK_SLAB,           "nether_brick_slab_from_nether_bricks_stonecutting");
        halveStonecutterSlab(consumer, Items.RED_NETHER_BRICKS,      Items.RED_NETHER_BRICK_SLAB,       "red_nether_brick_slab_from_red_nether_bricks_stonecutting");
        halveStonecutterSlab(consumer, Items.END_STONE_BRICKS,       Items.END_STONE_BRICK_SLAB,        "end_stone_brick_slab_from_end_stone_bricks_stonecutting");
        halveStonecutterSlab(consumer, Items.BLACKSTONE,             Items.BLACKSTONE_SLAB,             "blackstone_slab_from_blackstone_stonecutting");
        halveStonecutterSlab(consumer, Items.POLISHED_BLACKSTONE,    Items.POLISHED_BLACKSTONE_SLAB,    "polished_blackstone_slab_from_polished_blackstone_stonecutting");
        halveStonecutterSlab(consumer, Items.POLISHED_BLACKSTONE_BRICKS, Items.POLISHED_BLACKSTONE_BRICK_SLAB, "polished_blackstone_brick_slab_from_polished_blackstone_bricks_stonecutting");
        halveStonecutterSlab(consumer, Items.COBBLED_DEEPSLATE,      Items.COBBLED_DEEPSLATE_SLAB,      "cobbled_deepslate_slab_from_cobbled_deepslate_stonecutting");
        halveStonecutterSlab(consumer, Items.POLISHED_DEEPSLATE,     Items.POLISHED_DEEPSLATE_SLAB,     "polished_deepslate_slab_from_polished_deepslate_stonecutting");
        halveStonecutterSlab(consumer, Items.DEEPSLATE_BRICKS,       Items.DEEPSLATE_BRICK_SLAB,        "deepslate_brick_slab_from_deepslate_bricks_stonecutting");
        halveStonecutterSlab(consumer, Items.DEEPSLATE_TILES,        Items.DEEPSLATE_TILE_SLAB,         "deepslate_tile_slab_from_deepslate_tiles_stonecutting");
        halveStonecutterSlab(consumer, Items.MUD_BRICKS,             Items.MUD_BRICK_SLAB,              "mud_brick_slab_from_mud_bricks_stonecutting");
    }

    /**
     * Emits a stonecutter recipe with output count 1 (vanilla's is 2)
     * at the exact vanilla resource location, overriding it.
     */
    private void halveStonecutterSlab(Consumer<FinishedRecipe> consumer,
                                      ItemLike input, ItemLike output,
                                      String vanillaRecipeId) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(input),
                        RecipeCategory.BUILDING_BLOCKS, output, 1)
                .unlockedBy("has_" + getItemName(input), has(input))
                .save(consumer, new ResourceLocation("minecraft", vanillaRecipeId));
    }
}
