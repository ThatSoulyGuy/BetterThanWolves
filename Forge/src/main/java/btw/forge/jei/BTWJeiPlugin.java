package btw.forge.jei;

import btw.forge.BTWForgeMod;
import btw.forge.ProxyRegistry;
import btw.forge.jei.categories.BulkRecipeCategory;
import btw.forge.jei.categories.CampfireRecipeCategory;
import btw.forge.jei.categories.SoulforgeRecipeCategory;
import btw.forge.recipes.ModernLogChoppingRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.block.Block;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI entry point for Better Than Wolves.
 *
 * <p>Exposes FC's private recipe managers as browsable JEI recipe categories
 * (cauldron / stoked cauldron / crucible / stoked crucible / mill stone /
 * soulforge / campfire), adds FC's code-driven crafting recipes (log
 * chopping, knitting, fishing-rod baiting) to the crafting category, and
 * adds information pages for FC processes that have no recipe list (kiln,
 * saw, turntable, hopper filters, hibachi / stoked fire, cooking-vessel fire
 * rules, campfire). The FC blocks are wired as catalysts.</p>
 *
 * <p>This class is only loaded by JEI itself — it is safe to ship when JEI
 * is absent because JVM classloading is lazy. No other BTW code references
 * it or anything else in {@code btw.forge.jei}.</p>
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
    private static final ResourceLocation SOULFORGE_ID =
            new ResourceLocation(BTWForgeMod.MOD_ID, "soulforge");
    private static final ResourceLocation CAMPFIRE_ID =
            new ResourceLocation(BTWForgeMod.MOD_ID, "campfire");

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

    // FC block/item field names on FCBetterThanWolves (catalysts and info pages)
    private static final String FIELD_CAULDRON = "fcCauldron";
    private static final String FIELD_CRUCIBLE = "fcCrucible";
    private static final String FIELD_MILLSTONE = "fcMillStone";
    /** fcAnvil is the soulforge (FCBetterThanWolves.java:1030, FCBlockSoulforge). */
    private static final String FIELD_SOULFORGE = "fcAnvil";
    private static final String FIELD_CAMPFIRE = "fcBlockCampfireUnlit";

    private static final String INFO_PREFIX = "jei.betterthanwolves.info.";

    // Category instances, stored so registerRecipes / registerCatalysts can
    // retrieve the RecipeType handles created in registerCategories.
    private BulkRecipeCategory cauldronCat;
    private BulkRecipeCategory cauldronStokedCat;
    private BulkRecipeCategory crucibleCat;
    private BulkRecipeCategory crucibleStokedCat;
    private BulkRecipeCategory millstoneCat;
    private SoulforgeRecipeCategory soulforgeCat;
    private CampfireRecipeCategory campfireCat;

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    /**
     * One rule for every item this mod registered for FC (each {@code betterthanwolves} item ProxyRegistry maps a
     * legacy id to: the block items of FC blocks and FC's standalone items): the FC metadata a non-damageable stack
     * carries in its Damage tag (ItemStackHelper.toMcStack) is its JEI subtype, so FC's subtypes are separate
     * entries -- the 22 arcane scrolls, 16 wool / candle / knit wool colours, the aesthetic blocks, the wool slab
     * colours -- instead of collapsing into the first one JEI saw (it de-duplicates by registry name plus subtype).
     * Which subtypes exist is still FC's: JEI lists the BTW creative tab, built from FC's getSubItems. A damageable
     * item's Damage is durability, never a subtype.
     */
    @Override
    public void registerItemSubtypes(ISubtypeRegistration reg) {
        int registered = 0;
        try {
            java.util.Set<net.minecraft.world.item.Item> seen =
                    java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
            for (int id = 1; id < btw.modern.Item.itemsList.length; id++) {
                net.minecraft.world.item.Item item = ProxyRegistry.getModernItem(id);
                if (item == null || item == Items.AIR || !seen.add(item)) continue;
                ResourceLocation key = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item);
                if (key == null || !BTWForgeMod.MOD_ID.equals(key.getNamespace())) continue;
                reg.registerSubtypeInterpreter(item, BTWJeiPlugin::fcMetadataSubtype);
                registered++;
            }
        } catch (Throwable e) {
            LOGGER.warn("FC subtype interpreters not (all) registered: {}", e.toString());
        }
        LOGGER.debug("Registered the FC metadata subtype interpreter for {} items", registered);
    }

    /**
     * The FC metadata of a non-damageable btw stack ("" for metadata 0, which carries no Damage tag), for the
     * ingredient list only. Recipe lookups keep one uid per item: JEI indexes and looks recipes up by the exact
     * UidContext.Recipe uid (InternalRecipeManagerPlugin.getRecipes, RecipeMap.addRecipe), and
     * RecipeBridge.fcStackAlternatives turns FC's "any metadata" wildcard on a btw item into its metadata-0 stack, so a
     * subtype uid there hid every wildcard recipe from a non-zero subtype's uses (red knit wool -> the wool armour,
     * FCRecipes.java:913-931). Recipe lookups therefore merge an item's subtypes, as they did before this interpreter.
     */
    private static String fcMetadataSubtype(ItemStack stack, UidContext context) {
        if (context == UidContext.Recipe) return IIngredientSubtypeInterpreter.NONE;
        if (stack.isEmpty() || stack.getItem().isDamageable(stack)) return IIngredientSubtypeInterpreter.NONE;
        int metadata = stack.getDamageValue();
        return metadata == 0 ? IIngredientSubtypeInterpreter.NONE : Integer.toString(metadata);
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
        soulforgeCat = new SoulforgeRecipeCategory(gui, SOULFORGE_ID,
                "jei.betterthanwolves.category.soulforge",
                iconFor(FIELD_SOULFORGE, Items.ANVIL));
        campfireCat = new CampfireRecipeCategory(gui, CAMPFIRE_ID,
                "jei.betterthanwolves.category.campfire",
                "jei.betterthanwolves.campfire.note",
                iconFor(FIELD_CAMPFIRE, Items.CAMPFIRE));

        reg.addRecipeCategories(cauldronCat, cauldronStokedCat,
                crucibleCat, crucibleStokedCat, millstoneCat, soulforgeCat, campfireCat);
    }

    @Override
    public void registerRecipes(IRecipeRegistration reg) {
        addAll(reg, cauldronCat,        MGR_CAULDRON);
        addAll(reg, cauldronStokedCat,  MGR_CAULDRON_STOKED);
        addAll(reg, crucibleCat,        MGR_CRUCIBLE);
        addAll(reg, crucibleStokedCat,  MGR_CRUCIBLE_STOKED);
        addAll(reg, millstoneCat,       MGR_MILLSTONE);

        try {
            List<SoulforgeRecipeWrapper> soulforge = FCRecipeExtractor.extractSoulforge();
            if (!soulforge.isEmpty()) reg.addRecipes(soulforgeCat.getRecipeType(), soulforge);
        } catch (Throwable e) {
            LOGGER.warn("Soulforge recipes not registered: {}", e.toString());
        }
        try {
            List<CampfireRecipeWrapper> campfire = FCRecipeExtractor.extractCampfire();
            if (!campfire.isEmpty()) reg.addRecipes(campfireCat.getRecipeType(), campfire);
        } catch (Throwable e) {
            LOGGER.warn("Campfire recipes not registered: {}", e.toString());
        }
        try {
            // FC's code-driven IRecipes run in-game as special recipes JEI does not list
            // (FCCraftingRecipes.Custom); show what they make in the crafting category.
            List<CraftingRecipe> custom = FCCustomCraftingDisplays.build();
            if (!custom.isEmpty()) reg.addRecipes(RecipeTypes.CRAFTING, custom);
        } catch (Throwable e) {
            LOGGER.warn("FC custom crafting displays not registered: {}", e.toString());
        }
        try {
            // FCMOD-INTEGRATION(1.20.1): bamboo_planks: the modern log chopping recipes are special recipes too (an
            // FC axe and a bamboo block); show their planks and sticks, read from the client's synced recipes.
            net.minecraft.client.multiplayer.ClientLevel level = net.minecraft.client.Minecraft.getInstance().level;
            List<CraftingRecipe> chopping = level == null ? List.of()
                    : ModernLogChoppingRecipe.jeiDisplays(level.getRecipeManager());
            if (!chopping.isEmpty()) reg.addRecipes(RecipeTypes.CRAFTING, chopping);
        } catch (Throwable e) {
            LOGGER.warn("Modern log chopping displays not registered: {}", e.toString());
        }
        try {
            registerInfoPages(reg);
        } catch (Throwable e) {
            LOGGER.warn("FC information pages not registered: {}", e.toString());
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration reg) {
        ItemStack cauldronCatalyst = iconFor(FIELD_CAULDRON, Items.CAULDRON);
        ItemStack crucibleCatalyst = iconFor(FIELD_CRUCIBLE, Items.BLAST_FURNACE);
        ItemStack millstoneCatalyst = iconFor(FIELD_MILLSTONE, Items.GRINDSTONE);
        ItemStack soulforgeCatalyst = iconFor(FIELD_SOULFORGE, Items.ANVIL);
        ItemStack campfireCatalyst = iconFor(FIELD_CAMPFIRE, Items.CAMPFIRE);

        reg.addRecipeCatalyst(cauldronCatalyst,  cauldronCat.getRecipeType());
        reg.addRecipeCatalyst(cauldronCatalyst,  cauldronStokedCat.getRecipeType());
        reg.addRecipeCatalyst(crucibleCatalyst,  crucibleCat.getRecipeType());
        reg.addRecipeCatalyst(crucibleCatalyst,  crucibleStokedCat.getRecipeType());
        reg.addRecipeCatalyst(millstoneCatalyst, millstoneCat.getRecipeType());
        reg.addRecipeCatalyst(soulforgeCatalyst, soulforgeCat.getRecipeType());
        reg.addRecipeCatalyst(campfireCatalyst,  campfireCat.getRecipeType());
    }

    // ------------------------------------------------------------------
    // Information pages (FC processes that are code, not recipe lists)
    // ------------------------------------------------------------------

    /**
     * Each page (text in en_us.json, {@code jei.betterthanwolves.info.<key>.<n>}) restates
     * the rule in FC's block code so a player does not have to look it up. Sources:
     * <ul>
     *   <li>kiln: FCBlockFireStoked.java:59-66, FCBlockKiln.java:53-90/200-262, the
     *       GetCookTimeMultiplierInKiln / GetItemIndexDroppedWhenCookedByKiln overrides;</li>
     *   <li>saw: FCBlockSaw.java:31-32/431-451/585-729, Block.java:2155-2215 (vanilla/server);</li>
     *   <li>turntable: FCTileEntityTurntable.java:34-41/151-153, FCBlockTurntable.java:104-116,
     *       FCBlockUnfiredClay.java:102-116, FCBlockUnfiredPottery.java:28-46/541-619;</li>
     *   <li>hopper filters: FCBlockHopper.java:418-535, FCTileEntityHopper.java:34/184-233,
     *       FCBlockSoulSand.java:41-53, FCBlockWickerPane.java:64-75;</li>
     *   <li>hibachi / stoked fire: FCBlockBBQ.java:150-170, FCBlockBellows.java:547-570,
     *       FCBlockFireStoked.java:59-158;</li>
     *   <li>cauldron / crucible: FCTileEntityCookingVessel.java:30-40/175-230/395-520/566-578;</li>
     *   <li>campfire: FCBlockCampfire.java:275-320, FCTileEntityCampfire.java:26-44/415-452.</li>
     * </ul>
     */
    private static void registerInfoPages(IRecipeRegistration reg) {
        addInfo(reg, "kiln", 3, new ItemStack(Items.BRICKS),
                fcStack("fcUnfiredPottery"), fcStack("fcBlockUnfiredBrick"));
        addInfo(reg, "saw", 2, fcStack("fcSaw"));
        addInfo(reg, "turntable", 2, fcStack("fcTurntable"), fcStack("fcBlockUnfiredClay"));
        addInfo(reg, "hopper", 3, fcStack("fcHopper"), new ItemStack(Items.SOUL_SAND),
                fcStack("fcItemSoulUrn"), fcStack("fcItemUrn"), fcStack("fcBlockWickerPane"));
        addInfo(reg, "hibachi", 2, fcStack("fcBBQ"), fcStack("fcBellows"));
        addInfo(reg, "cooking_vessel", 2, fcStack(FIELD_CAULDRON), fcStack(FIELD_CRUCIBLE));
        addInfo(reg, "campfire", 2, fcStack(FIELD_CAMPFIRE), fcStack("fcItemChiselWood"));
    }

    private static void addInfo(IRecipeRegistration reg, String key, int paragraphs, ItemStack... stacks) {
        List<ItemStack> targets = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) continue;
            boolean known = false;
            for (ItemStack seen : targets) {
                if (seen.getItem() == stack.getItem()) {
                    known = true;
                    break;
                }
            }
            if (!known) targets.add(stack);
        }
        if (targets.isEmpty()) {
            LOGGER.debug("No items for the '{}' information page", key);
            return;
        }
        Component[] text = new Component[paragraphs];
        for (int i = 0; i < paragraphs; i++) {
            text[i] = Component.translatable(INFO_PREFIX + key + "." + (i + 1));
        }
        reg.addItemStackInfo(targets, text);
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
        ItemStack stack = fcStack(fcFieldName);
        return stack.isEmpty() ? new ItemStack(vanillaFallback) : stack;
    }

    /**
     * The MC ItemStack for an FC block or item field on {@code FCBetterThanWolves}, or
     * EMPTY when FC is not loaded or the field has no registered MC counterpart.
     */
    private static ItemStack fcStack(String fcFieldName) {
        try {
            Class<?> btwClass = Class.forName("net.minecraft.src.btw.core.FCBetterThanWolves");
            Object value = btwClass.getField(fcFieldName).get(null);
            if (value instanceof btw.modern.Block fcModernBlock) {
                int legacyId = fcModernBlock.blockID;
                Block modernBlock = ProxyRegistry.getModernBlock(legacyId);
                if (modernBlock != null) {
                    ItemStack stack = new ItemStack(modernBlock);
                    if (!stack.isEmpty()) return stack;
                }
                net.minecraft.world.item.Item item = ProxyRegistry.getModernItem(legacyId);
                if (item != null) {
                    ItemStack stack = new ItemStack(item);
                    if (!stack.isEmpty()) return stack;
                }
            } else if (value instanceof btw.modern.Item fcModernItem) {
                net.minecraft.world.item.Item item = ProxyRegistry.getModernItem(fcModernItem.itemID);
                if (item != null) {
                    ItemStack stack = new ItemStack(item);
                    if (!stack.isEmpty()) return stack;
                }
            }
        } catch (Throwable e) {
            LOGGER.debug("fcStack({}) failed: {}", fcFieldName, e.getMessage());
        }
        return ItemStack.EMPTY;
    }
}
