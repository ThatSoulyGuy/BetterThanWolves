package btw.forge.jei;

import btw.forge.BTWForgeMod;
import btw.forge.ProxyRegistry;
import btw.forge.jei.categories.BulkRecipeCategory;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * JEI entry point for Better Than Wolves.
 *
 * <p>Exposes FC's private bulk-recipe managers (cauldron / stoked cauldron /
 * crucible / stoked crucible / mill stone) as browsable JEI recipe
 * categories, and wires the relevant FC blocks as catalysts.</p>
 *
 * <p>This class is only loaded by JEI itself — it is safe to ship when JEI
 * is absent because JVM classloading is lazy. No other BTW code references
 * it.</p>
 */
@JeiPlugin
public class BTWJeiPlugin implements IModPlugin {

    private static final Logger LOGGER = LogManager.getLogger("BTW-JEI");

    private static final ResourceLocation PLUGIN_ID =
            new ResourceLocation(BTWForgeMod.MOD_ID, "jei_plugin");

    // Category IDs
    private static final ResourceLocation CAULDRON_ID =
            new ResourceLocation(BTWForgeMod.MOD_ID, "cauldron");
    private static final ResourceLocation CAULDRON_STOKED_ID =
            new ResourceLocation(BTWForgeMod.MOD_ID, "cauldron_stoked");
    private static final ResourceLocation CRUCIBLE_ID =
            new ResourceLocation(BTWForgeMod.MOD_ID, "crucible");
    private static final ResourceLocation CRUCIBLE_STOKED_ID =
            new ResourceLocation(BTWForgeMod.MOD_ID, "crucible_stoked");
    private static final ResourceLocation MILLSTONE_ID =
            new ResourceLocation(BTWForgeMod.MOD_ID, "millstone");

    // FC manager class names
    private static final String MGR_CAULDRON =
            "net.minecraft.src.btw.crafting.FCCraftingManagerCauldron";
    private static final String MGR_CAULDRON_STOKED =
            "net.minecraft.src.btw.crafting.FCCraftingManagerCauldronStoked";
    private static final String MGR_CRUCIBLE =
            "net.minecraft.src.btw.crafting.FCCraftingManagerCrucible";
    private static final String MGR_CRUCIBLE_STOKED =
            "net.minecraft.src.btw.crafting.FCCraftingManagerCrucibleStoked";
    private static final String MGR_MILLSTONE =
            "net.minecraft.src.btw.crafting.FCCraftingManagerMillStone";

    // FC block field names on FCBetterThanWolves (used for catalysts)
    private static final String FIELD_CAULDRON = "fcCauldron";
    private static final String FIELD_CRUCIBLE = "fcCrucible";
    private static final String FIELD_MILLSTONE = "fcMillStone";

    // Category instances, stored so registerRecipes / registerCatalysts can
    // retrieve the RecipeType handles created in registerCategories.
    private BulkRecipeCategory cauldronCat;
    private BulkRecipeCategory cauldronStokedCat;
    private BulkRecipeCategory crucibleCat;
    private BulkRecipeCategory crucibleStokedCat;
    private BulkRecipeCategory millstoneCat;

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration reg) {
        IGuiHelper gui = reg.getJeiHelpers().getGuiHelper();

        cauldronCat = new BulkRecipeCategory(gui, CAULDRON_ID,
                "jei.betterthanwolves.category.cauldron",
                iconFor(FIELD_CAULDRON, Items.CAULDRON));
        cauldronStokedCat = new BulkRecipeCategory(gui, CAULDRON_STOKED_ID,
                "jei.betterthanwolves.category.cauldron_stoked",
                iconFor(FIELD_CAULDRON, Items.CAULDRON));
        crucibleCat = new BulkRecipeCategory(gui, CRUCIBLE_ID,
                "jei.betterthanwolves.category.crucible",
                iconFor(FIELD_CRUCIBLE, Items.BLAST_FURNACE));
        crucibleStokedCat = new BulkRecipeCategory(gui, CRUCIBLE_STOKED_ID,
                "jei.betterthanwolves.category.crucible_stoked",
                iconFor(FIELD_CRUCIBLE, Items.BLAST_FURNACE));
        millstoneCat = new BulkRecipeCategory(gui, MILLSTONE_ID,
                "jei.betterthanwolves.category.millstone",
                iconFor(FIELD_MILLSTONE, Items.GRINDSTONE));

        reg.addRecipeCategories(cauldronCat, cauldronStokedCat,
                crucibleCat, crucibleStokedCat, millstoneCat);
    }

    @Override
    public void registerRecipes(IRecipeRegistration reg) {
        addAll(reg, cauldronCat,        MGR_CAULDRON);
        addAll(reg, cauldronStokedCat,  MGR_CAULDRON_STOKED);
        addAll(reg, crucibleCat,        MGR_CRUCIBLE);
        addAll(reg, crucibleStokedCat,  MGR_CRUCIBLE_STOKED);
        addAll(reg, millstoneCat,       MGR_MILLSTONE);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration reg) {
        ItemStack cauldronCatalyst = iconFor(FIELD_CAULDRON, Items.CAULDRON);
        ItemStack crucibleCatalyst = iconFor(FIELD_CRUCIBLE, Items.BLAST_FURNACE);
        ItemStack millstoneCatalyst = iconFor(FIELD_MILLSTONE, Items.GRINDSTONE);

        reg.addRecipeCatalyst(cauldronCatalyst,  cauldronCat.getRecipeType());
        reg.addRecipeCatalyst(cauldronCatalyst,  cauldronStokedCat.getRecipeType());
        reg.addRecipeCatalyst(crucibleCatalyst,  crucibleCat.getRecipeType());
        reg.addRecipeCatalyst(crucibleCatalyst,  crucibleStokedCat.getRecipeType());
        reg.addRecipeCatalyst(millstoneCatalyst, millstoneCat.getRecipeType());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static void addAll(IRecipeRegistration reg, BulkRecipeCategory cat,
                               String managerClass) {
        List<BulkRecipeWrapper> recipes = FCRecipeExtractor.extract(managerClass);
        if (recipes.isEmpty()) {
            LOGGER.debug("No recipes for {}", managerClass);
            return;
        }
        RecipeType<BulkRecipeWrapper> type = cat.getRecipeType();
        reg.addRecipes(type, recipes);
    }

    /**
     * Resolves the MC ItemStack for an FC block field on
     * {@code FCBetterThanWolves}. Falls back to {@code vanillaFallback} if
     * FC is not loaded or the block is not registered.
     */
    private static ItemStack iconFor(String fcFieldName, net.minecraft.world.item.Item vanillaFallback) {
        try {
            Class<?> btwClass = Class.forName("net.minecraft.src.btw.core.FCBetterThanWolves");
            Object fcBlock = btwClass.getField(fcFieldName).get(null);
            if (fcBlock instanceof btw.modern.Block fcModernBlock) {
                int legacyId = fcModernBlock.blockID;
                Block modernBlock = ProxyRegistry.getModernBlock(legacyId);
                if (modernBlock != null) {
                    return new ItemStack(modernBlock);
                }
            }
        } catch (Throwable e) {
            LOGGER.debug("iconFor({}) failed: {}", fcFieldName, e.getMessage());
        }
        return new ItemStack(vanillaFallback);
    }
}
