package btw.forge;

import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MC 1.20.1 crafting recipes backed by FC's {@code btw.modern.CraftingManager} entries.
 *
 * <p>The FC workbench (and the vanilla crafting table, whose block is FC's FCBlockWorkbench)
 * opens an FC container that resolves recipes through FC's own CraftingManager and FC's
 * SlotCrafting, so FC crafting rules apply there as far as the Modern-Common stand-ins
 * (ShapedRecipes/ShapelessRecipes matching, Item.OnUsedInCrafting) implement them; nothing
 * here affects that path. The player's 2x2 inventory grid is MC's InventoryMenu, which only
 * consults the MC RecipeManager -- these classes make that grid behave like 1.5.2's
 * ContainerPlayer, which also used FC's CraftingManager + SlotCrafting:</p>
 * <ul>
 *   <li>{@link Shaped}/{@link Shapeless}: FC shaped/shapeless recipes converted to MC
 *       recipes (vanilla serializers, so they sync, show in the recipe book and in JEI), with
 *       {@code getRemainingItems} following 1.5.2 SlotCrafting.onPickupFromSlot so tools such
 *       as shears/chisels/axes are damaged rather than consumed. Their {@code matches} also asks
 *       FC's own recipe on the FC reading of the grid, so a square grid matches them exactly as
 *       FC's CraftingManager does (id and damage, 32767 = any).</li>
 *   <li>{@link Custom}: FC's code-driven IRecipe classes (FCRecipesLogChopping,
 *       FCRecipesKnitting, FCRecipesFishingRodBaiting) wrapped as a special recipe whose
 *       matches/assemble/remaining items delegate to the FC IRecipe through an
 *       {@code InventoryCrafting} adapter.</li>
 * </ul>
 */
public final class FCCraftingRecipes {

    private static final Logger LOGGER = LogManager.getLogger("BTW-FCCrafting");

    /** Registry name of the serializer used to sync {@link Custom} recipes to clients. */
    public static final ResourceLocation CUSTOM_SERIALIZER_ID =
            new ResourceLocation(BTWForgeMod.MOD_ID, "fc_custom_crafting");

    public static final CustomSerializer CUSTOM_SERIALIZER = new CustomSerializer();

    /** Failure keys already logged (once per key, never per craft). */
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    /** FC item class -> whether it (or an FC superclass) overrides the 3-arg OnUsedInCrafting. */
    private static final Map<Class<?>, Boolean> OVERRIDES_DAMAGE_HOOK = new ConcurrentHashMap<>();

    private FCCraftingRecipes() {}

    // ================================================================
    // Recipe types
    // ================================================================

    /** FC ShapedRecipes as an MC ShapedRecipe with FC crafting-grid remainders. */
    public static final class Shaped extends ShapedRecipe {
        private final btw.modern.ShapedRecipes fcRecipe;

        public Shaped(ResourceLocation id, int width, int height, NonNullList<Ingredient> ingredients,
                      ItemStack result, btw.modern.ShapedRecipes fcRecipe) {
            super(id, "", CraftingBookCategory.MISC, width, height, ingredients, result);
            this.fcRecipe = fcRecipe;
        }

        @Override
        public ItemStack assemble(CraftingContainer inv, RegistryAccess access) {
            // A ShapedRecipes subclass (FCRecipesWoolBlock/FCRecipesArmorWool colour the result from
            // the grid) computes its own result; the plain class just copies its output.
            if (fcRecipe.getClass() != btw.modern.ShapedRecipes.class) {
                ItemStack custom = fcAssemble(fcRecipe, inv);
                if (!custom.isEmpty()) return custom;
            }
            return super.assemble(inv, access);
        }

        // FCMOD-INTEGRATION(1.20.1): recipes: an FC recipe matches the 2x2 grid as FC matches it (id and damage, 32767 = any),
        // as 1.5.2's ContainerPlayer did through CraftingManager; the item-level test runs first
        @Override
        public boolean matches(CraftingContainer inv, Level level) {
            return super.matches(inv, level) && fcMatches(fcRecipe, inv);
        }

        @Override
        public NonNullList<ItemStack> getRemainingItems(CraftingContainer inv) {
            return fcRemainingItems(inv, fcOutput(fcRecipe, inv));
        }
    }

    /** FC ShapelessRecipes as an MC ShapelessRecipe with FC crafting-grid remainders. */
    public static final class Shapeless extends ShapelessRecipe {
        private final btw.modern.ShapelessRecipes fcRecipe;

        public Shapeless(ResourceLocation id, ItemStack result, NonNullList<Ingredient> ingredients,
                         btw.modern.ShapelessRecipes fcRecipe) {
            super(id, "", CraftingBookCategory.MISC, result, ingredients);
            this.fcRecipe = fcRecipe;
        }

        // FCMOD-INTEGRATION(1.20.1): recipes: an FC recipe matches the 2x2 grid as FC matches it (id and damage, 32767 = any)
        @Override
        public boolean matches(CraftingContainer inv, Level level) {
            return super.matches(inv, level) && fcMatches(fcRecipe, inv);
        }

        @Override
        public NonNullList<ItemStack> getRemainingItems(CraftingContainer inv) {
            return fcRemainingItems(inv, fcOutput(fcRecipe, inv));
        }
    }

    /**
     * A code-driven FC IRecipe (not Shaped/Shapeless). The FC recipe list is built by the same
     * FCRecipes code on both sides, so a client resolves the server's entry by its list index
     * (verified against the class name; a mismatch leaves the recipe inert, never wrong).
     */
    public static final class Custom extends CustomRecipe {
        private final int fcIndex;
        private final String fcClassName;
        @Nullable private final btw.modern.IRecipe fcRecipe;

        Custom(ResourceLocation id, int fcIndex, String fcClassName, @Nullable btw.modern.IRecipe fcRecipe) {
            super(id, CraftingBookCategory.MISC);
            this.fcIndex = fcIndex;
            this.fcClassName = fcClassName;
            this.fcRecipe = fcRecipe;
        }

        /** The wrapped FC recipe, or null when it could not be resolved on this side. */
        @Nullable
        public btw.modern.IRecipe getFcRecipe() {
            return fcRecipe;
        }

        @Override
        public boolean matches(CraftingContainer inv, Level level) {
            if (fcRecipe == null) return false;
            try {
                btw.modern.InventoryCrafting fcInv = toInventoryCrafting(inv);
                // FC's IRecipes ignore the world argument (FCRecipesLogChopping.java:25,
                // FCRecipesKnitting.java:28, FCRecipesFishingRodBaiting.java:25). FCRecipesKnitting
                // keeps scratch fields, so calls on one instance are serialised.
                synchronized (fcRecipe) {
                    return fcRecipe.matches(fcInv, null);
                }
            } catch (Throwable t) {
                warnOnce("matches:" + fcClassName, "FC recipe " + fcClassName + ".matches failed", t);
                return false;
            }
        }

        @Override
        public ItemStack assemble(CraftingContainer inv, RegistryAccess access) {
            return fcRecipe == null ? ItemStack.EMPTY : fcAssemble(fcRecipe, inv);
        }

        @Override
        public boolean canCraftInDimensions(int width, int height) {
            if (fcRecipe == null) return false;
            try {
                return width * height >= fcRecipe.getRecipeSize();
            } catch (Throwable t) {
                return width * height >= 2;
            }
        }

        @Override
        public NonNullList<ItemStack> getRemainingItems(CraftingContainer inv) {
            if (fcRecipe == null) return super.getRemainingItems(inv);
            return fcRemainingItems(inv, fcOutput(fcRecipe, inv));
        }

        @Override
        public RecipeSerializer<?> getSerializer() {
            return CUSTOM_SERIALIZER;
        }
    }

    /**
     * Wraps the FC IRecipe at {@code fcIndex} of {@code btw.modern.CraftingManager}'s list.
     */
    public static Custom custom(ResourceLocation id, int fcIndex, btw.modern.IRecipe fcRecipe) {
        return new Custom(id, fcIndex, fcRecipe.getClass().getName(), fcRecipe);
    }

    /**
     * Syncs a {@link Custom} recipe as (FC list index, FC class name); the receiving side looks
     * the FC recipe up in its own CraftingManager.
     */
    public static final class CustomSerializer implements RecipeSerializer<Custom> {
        @Override
        public Custom fromJson(ResourceLocation id, JsonObject json) {
            return resolve(id, GsonHelper.getAsInt(json, "fc_index", -1),
                    GsonHelper.getAsString(json, "fc_class", ""));
        }

        @Override
        public Custom fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            int index = buf.readVarInt();
            String className = buf.readUtf();
            return resolve(id, index, className);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, Custom recipe) {
            buf.writeVarInt(recipe.fcIndex);
            buf.writeUtf(recipe.fcClassName);
        }

        private static Custom resolve(ResourceLocation id, int index, String className) {
            btw.modern.IRecipe found = null;
            try {
                List<?> list = btw.modern.CraftingManager.getInstance().getRecipeList();
                Object atIndex = index >= 0 && index < list.size() ? list.get(index) : null;
                if (atIndex instanceof btw.modern.IRecipe indexed
                        && indexed.getClass().getName().equals(className)) {
                    found = indexed;
                } else {
                    for (Object o : list) {
                        if (o instanceof btw.modern.IRecipe candidate
                                && candidate.getClass().getName().equals(className)) {
                            found = candidate;
                            break;
                        }
                    }
                }
            } catch (Throwable t) {
                warnOnce("resolve:" + className, "Could not resolve FC recipe " + className, t);
            }
            if (found == null && WARNED.add("unresolved:" + className)) {
                LOGGER.warn("FC custom recipe {} ({} #{}) not found on this side; it will not match here",
                        id, className, index);
            }
            return new Custom(id, index, className, found);
        }
    }

    /** Registers {@link #CUSTOM_SERIALIZER} (needed to sync {@link Custom} recipes to clients). */
    @Mod.EventBusSubscriber(modid = BTWForgeMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {}

        @SubscribeEvent
        public static void onRegister(RegisterEvent event) {
            event.register(ForgeRegistries.Keys.RECIPE_SERIALIZERS,
                    helper -> helper.register(CUSTOM_SERIALIZER_ID, CUSTOM_SERIALIZER));
        }
    }

    // ================================================================
    // FC <-> MC crafting grid
    // ================================================================

    /** An FC InventoryCrafting holding FC copies of the MC grid (same x + y*width layout). */
    public static btw.modern.InventoryCrafting toInventoryCrafting(CraftingContainer inv) {
        int width = Math.max(1, inv.getWidth());
        int height = Math.max(1, inv.getHeight());
        // No event handler: setInventorySlotContents then never calls back into a container.
        btw.modern.InventoryCrafting fcInv = new btw.modern.InventoryCrafting(null, width, height);
        int n = Math.min(inv.getContainerSize(), width * height);
        for (int i = 0; i < n; i++) {
            ItemStack mc = inv.getItem(i);
            if (!mc.isEmpty()) {
                fcInv.setInventorySlotContents(i, ItemStackHelper.toFcStack(mc));
            }
        }
        return fcInv;
    }

    /** FC getCraftingResult on the grid, as an MC stack (EMPTY on no result or failure). */
    private static ItemStack fcAssemble(btw.modern.IRecipe fcRecipe, CraftingContainer inv) {
        btw.modern.ItemStack out = fcResult(fcRecipe, inv);
        return out == null ? ItemStack.EMPTY : ItemStackHelper.toMcStack(out);
    }

    @Nullable
    private static btw.modern.ItemStack fcResult(btw.modern.IRecipe fcRecipe, CraftingContainer inv) {
        try {
            btw.modern.InventoryCrafting fcInv = toInventoryCrafting(inv);
            synchronized (fcRecipe) {
                return fcRecipe.getCraftingResult(fcInv);
            }
        } catch (Throwable t) {
            String name = fcRecipe.getClass().getName();
            warnOnce("result:" + name, "FC recipe " + name + ".getCraftingResult failed", t);
            return null;
        }
    }

    /**
     * FC's own match on the grid (Modern-Common ShapedRecipes.java:66-70, ShapelessRecipes.java:63-64), asked after the
     * item-level match, so every cell already holds an item of one of the recipe's own ingredients. FC's shaped matcher
     * assumes a square grid (ShapedRecipes.matches: sqrt of the size); another shape keeps the item-level answer. A throw
     * keeps the item-level answer too (the behaviour before this check), logged once per recipe class.
     */
    // FCMOD-INTEGRATION(1.20.1): recipes: the 2x2 grid (and any 1.20.1 crafting container) matches FC's recipes as the FC grids do
    private static boolean fcMatches(btw.modern.IRecipe fcRecipe, CraftingContainer inv) {
        if (inv.getWidth() != inv.getHeight()) return true;
        try {
            btw.modern.InventoryCrafting fcInv = toInventoryCrafting(inv);
            synchronized (fcRecipe) {
                return fcRecipe.matches(fcInv, null);       // FC's matchers ignore the world argument
            }
        } catch (Throwable t) {
            String name = fcRecipe.getClass().getName();
            warnOnce("fcmatch:" + name, "FC recipe " + name + ".matches failed; the grid matches it by item only", t);
            return true;
        }
    }

    /**
     * The FC output handed to the per-ingredient crafting hooks (1.5.2 passes the taken result
     * stack, SlotCrafting.java:129/159). Plain Shaped/Shapeless recipes have a fixed output;
     * code-driven ones compute it from the grid.
     */
    @Nullable
    private static btw.modern.ItemStack fcOutput(btw.modern.IRecipe fcRecipe, CraftingContainer inv) {
        if (fcRecipe.getClass() == btw.modern.ShapedRecipes.class
                || fcRecipe.getClass() == btw.modern.ShapelessRecipes.class) {
            try {
                btw.modern.ItemStack out = fcRecipe.getRecipeOutput();
                return out == null ? null : out.copy();
            } catch (Throwable t) {
                return null;
            }
        }
        return fcResult(fcRecipe, inv);
    }

    /**
     * Port of 1.5.2 SlotCrafting.onPickupFromSlot's per-ingredient rules
     * (vanilla/server/net/minecraft/src/SlotCrafting.java:118-185) expressed as MC remainders.
     *
     * <p>MC's ResultSlot.onTake removes one of every ingredient and then puts each returned
     * remainder back into its slot (merging with what is left), so:</p>
     * <ul>
     *   <li>not consumed (IsConsumedInCrafting false, :132-135) -> a 1-count copy goes back;</li>
     *   <li>damaged in crafting (:136-152) -> broken at max-1 damage (OnBrokenInCrafting), else a
     *       copy with one more point of damage goes back (OnDamagedInCrafting);</li>
     *   <li>consumed with a container item (:156-178) -> the container, unless the result
     *       consumes it (DoesConsumeContainerItemWhenCrafted); when the container "leaves the
     *       grid" it goes into the player's inventory first;</li>
     *   <li>items FC knows nothing about keep MC's own crafting remainder.</li>
     * </ul>
     *
     * <p>The FC side-effect hooks (OnUsedInCrafting: chop sounds, ejected sawdust/bark/seeds;
     * OnDamaged/OnBrokenInCrafting: sounds) and m_iTimesCraftedThisTick only run for a server
     * crafter (ForgeHooks.getCraftingPlayer, set by ResultSlot.onTake). Client prediction and
     * player-less crafters get the same item outcome without side effects.</p>
     */
    public static NonNullList<ItemStack> fcRemainingItems(CraftingContainer inv,
                                                          @Nullable btw.modern.ItemStack fcOutput) {
        NonNullList<ItemStack> result = NonNullList.withSize(inv.getContainerSize(), ItemStack.EMPTY);

        Player crafter = ForgeHooks.getCraftingPlayer();
        ServerPlayer serverCrafter = crafter instanceof ServerPlayer sp ? sp : null;
        // 1.5.2 ItemStack.damageItem skips creative players (vanilla/server ItemStack.java:325).
        // Checked on both sides so client prediction agrees with the server.
        boolean creative = crafter != null && crafter.getAbilities().instabuild;
        PlayerBridge bridge = null;
        if (serverCrafter != null) {
            try {
                bridge = PlayerBridge.getOrCreate(serverCrafter);
            } catch (Throwable t) {
                warnOnce("bridge", "Could not bridge the crafting player; FC crafting hooks skipped", t);
            }
        }

        for (int i = 0; i < result.size(); i++) {
            ItemStack mc = inv.getItem(i);
            if (!mc.isEmpty()) {
                result.set(i, remainderFor(mc, fcOutput, serverCrafter, bridge, creative));
            }
        }

        if (bridge != null) {
            bridge.m_iTimesCraftedThisTick++; // SlotCrafting.java:182-184 (reset per tick by ServerPlayerMixin)
        }
        return result;
    }

    private static ItemStack remainderFor(ItemStack mc, @Nullable btw.modern.ItemStack fcOutput,
                                          @Nullable ServerPlayer serverCrafter, @Nullable PlayerBridge bridge,
                                          boolean creative) {
        ItemStack mcRemainder = mc.hasCraftingRemainingItem() ? mc.getCraftingRemainingItem() : ItemStack.EMPTY;

        btw.modern.ItemStack fc;
        btw.modern.Item fcItem;
        try {
            fc = ItemStackHelper.toFcStack(mc);
            fcItem = fc != null ? fc.getItem() : null;
        } catch (Throwable t) {
            warnOnce("convert", "Crafting ingredient conversion failed; using MC remainders", t);
            return mcRemainder;
        }
        if (fcItem == null) return mcRemainder;

        try {
            if (bridge != null && fcOutput != null) {
                try {
                    onUsedInCrafting(fcItem, fc.getItemDamage(), bridge, fcOutput); // SlotCrafting.java:129
                } catch (Throwable t) {
                    warnOnce("used:" + fcItem.getClass().getName(), "FC OnUsedInCrafting failed", t);
                }
            }

            if (!fcItem.IsConsumedInCrafting()) {
                return mc.copyWithCount(1); // stays in the grid
            }

            if (fcItem.IsDamagedInCrafting()) {
                if (fc.getItemDamage() >= fc.getMaxDamage() - 1) {
                    if (bridge != null) {
                        try {
                            fcItem.OnBrokenInCrafting(bridge);
                        } catch (Throwable t) {
                            warnOnce("broken:" + fcItem.getClass().getName(), "FC OnBrokenInCrafting failed", t);
                        }
                    }
                    return ItemStack.EMPTY;
                }
                if (bridge != null) {
                    try {
                        fcItem.OnDamagedInCrafting(bridge);
                    } catch (Throwable t) {
                        warnOnce("damaged:" + fcItem.getClass().getName(), "FC OnDamagedInCrafting failed", t);
                    }
                }
                ItemStack damaged = mc.copyWithCount(1);
                if (damaged.isDamageableItem() && !creative) {
                    // 1.5.2 ItemStack.damageItem(1, player) -- honours Unbreaking like MC's hurt().
                    RandomSource random = serverCrafter != null ? serverCrafter.getRandom() : RandomSource.create();
                    if (damaged.hurt(1, random, serverCrafter)) {
                        return ItemStack.EMPTY;
                    }
                }
                return damaged;
            }

            if (fcItem.hasContainerItem()) {
                btw.modern.Item containerItem = fcItem.getContainerItem();
                if (fcOutput != null && fcOutput.getItem() != null
                        && fcOutput.getItem().DoesConsumeContainerItemWhenCrafted(containerItem)) {
                    return ItemStack.EMPTY;
                }
                ItemStack container = ItemStackHelper.toMcStack(new btw.modern.ItemStack(containerItem));
                if (container.isEmpty()) return mcRemainder;
                if (serverCrafter != null && fcItem.doesContainerItemLeaveCraftingGrid(fc)
                        && serverCrafter.getInventory().add(container)) {
                    return ItemStack.EMPTY;
                }
                return container; // what did not fit stays in the grid (or drops, via ResultSlot)
            }

            return mcRemainder;
        } catch (Throwable t) {
            warnOnce("rules:" + fcItem.getClass().getName(), "FC crafting rules failed; using MC remainders", t);
            return mcRemainder;
        }
    }

    /**
     * 1.5.2 SlotCrafting calls the 3-arg OnUsedInCrafting, whose base implementation forwards
     * to the 2-arg overload (vanilla/server/net/minecraft/src/Item.java:1067-1070). Most FC
     * items override only the 2-arg form (FCItemAxe, FCItemBlockBloodWood, FCItemBlockPumpkinFresh,
     * FCItemChiselIron, FCItemBow, ...); FCItemBlockLog overrides the 3-arg one. Modern-Common's
     * base 3-arg method is empty, so this calls the 3-arg form only when the item overrides it and
     * the 2-arg form otherwise: exactly one hook runs, as in 1.5.2, whether or not the base
     * forwarding is restored.
     */
    private static void onUsedInCrafting(btw.modern.Item fcItem, int itemDamage, PlayerBridge player,
                                         btw.modern.ItemStack output) {
        if (OVERRIDES_DAMAGE_HOOK.computeIfAbsent(fcItem.getClass(), FCCraftingRecipes::overridesDamageHook)) {
            fcItem.OnUsedInCrafting(itemDamage, player, output);
        } else {
            fcItem.OnUsedInCrafting(player, output);
        }
    }

    private static boolean overridesDamageHook(Class<?> itemClass) {
        try {
            return itemClass.getMethod("OnUsedInCrafting", int.class,
                            btw.modern.EntityPlayer.class, btw.modern.ItemStack.class)
                    .getDeclaringClass() != btw.modern.Item.class;
        } catch (Throwable t) {
            return true; // cannot tell: keep the 1.5.2 entry point
        }
    }

    private static void warnOnce(String key, String message, Throwable t) {
        if (WARNED.add(key)) {
            LOGGER.warn("{} (further occurrences suppressed): {}", message, t.toString());
        }
    }
}
