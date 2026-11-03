package btw.forge.recipes;

import btw.forge.BTWForgeMod;
import btw.forge.FCCraftingRecipes;
import btw.forge.ItemStackHelper;
import btw.forge.PlayerBridge;
import btw.forge.ProxyItem;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FCMOD-INTEGRATION(1.20.1): bamboo_planks
 *
 * <p><b>Feature:</b> FC's log chopping for a log FC has no item for: a {@code betterthanwolves:modern_log_chopping}
 * recipe ({@code data/<ns>/recipes/*.json}) names the logs (an ingredient), their planks and the yields, and BTW's two
 * plank routes follow it: an FC axe and one such log at any crafting grid, and FC's saw ({@code ModernWoodSawHooks},
 * through {@link #sawRecipeFor}). Shipped for the bamboo blocks ({@code betterthanwolves:bamboo_chopping}).</p>
 *
 * <p><b>Conflict with FC:</b> FC's FCRecipesLogChopping accepts only log 17 and blood wood (Common :178-189), and its
 * sawdust and bark come from the log item's own OnUsedInCrafting (Common FCItemBlockLog.java:28-50). A bamboo block is
 * a headless item to FC, with no such hook, so neither FC route makes its planks; the 1.20.1 recipe made them from the
 * block alone, in the 2x2 grid, with no tool (removed by {@code btw_recipe_replacements/bamboo_planks.json},
 * maintainer ruling 2026-10-21).</p>
 *
 * <p><b>Integration pattern:</b> C (rebalance through BTW's own routes), data-driven: FC's rules on 1.20.1 data.</p>
 *
 * <p><b>Rebalance:</b> FC's saw : chop ratio (4 : 2 for a log, FCBlockSaw.java:619-639, FCRecipesLogChopping.java:114-132)
 * on bamboo's own 1.20.1 yield (2 planks, half a log's): the saw 2 planks and 1 sawdust; an FC axe of harvest level 2
 * or more 1 plank and 1 sawdust; a wooden, stone or gold axe 1 stick and 2 sawdust (ruling 7.1 (a)); no bark
 * (maintainer ruling 2026-10-21). The numbers are the recipe's data.</p>
 *
 * <p><b>Injection strategy:</b> a special crafting recipe (no recipe-book entry) with its own serializer, so the 2x2
 * grid (RecipeManager) and the FC work stump, workbench and soulforge (WorkbenchFallback, after FC's own list) all
 * answer it. Matching identifies the axe with {@link ProxyItem#fcItemFor} and never allocates a headless id. The axe
 * keeps FC's crafting rules ({@link FCCraftingRecipes#fcRemainingItems}: damaged, FC's chop sound); the sawdust is
 * ejected at a server crafter through FC's FCUtilsItem.EjectStackWithRandomVelocity, where FCItemBlockLog ejects a
 * log's. JEI shows two displays per recipe ({@link #jeiDisplays}).</p>
 *
 * <p><b>Alternative considered and rejected:</b> a patch on FCRecipesLogChopping (FC's IsLog and result are keyed on
 * FC ids, and the sawdust lives in the log item's hook, which a headless item lacks); the log rule for bamboo blocks
 * (they are crafted, never world-generated, so a chew gate would gate nothing; ruling 7.1 (a)).</p>
 */
// FCMOD-INTEGRATION(1.20.1): bamboo_planks: bamboo planks come from FC's saw and an FC axe at the BTW grids, as a log's planks do
public final class ModernLogChoppingRecipe extends CustomRecipe {

    private static final Logger LOGGER = LogManager.getLogger("BTW-RecipeBridge");

    /** Registry name of the serializer ({@code "type": "betterthanwolves:modern_log_chopping"}). */
    public static final ResourceLocation SERIALIZER_ID = new ResourceLocation(BTWForgeMod.MOD_ID, "modern_log_chopping");
    public static final Serializer SERIALIZER = new Serializer();

    /** FC's FCItemAxe (refined and battle axes extend it) and FCItemTool.toolMaterial; null when missing. */
    private static final Class<?> FC_AXE = load("net.minecraft.src.btw.item.FCItemAxe");
    private static final Field TOOL_MATERIAL = toolMaterial();
    /** FCUtilsItem.EjectStackWithRandomVelocity(World, double, double, double, ItemStack); resolved on first use. */
    private static volatile MethodHandle eject;
    private static final Set<String> FAILURES_LOGGED = ConcurrentHashMap.newKeySet();

    /** Block -> the first recipe (by id) whose log ingredient holds its item; the saw's index, rebuilt per injection. */
    private static volatile Map<Block, ModernLogChoppingRecipe> sawIndex = Map.of();

    private final Ingredient log;
    private final Item planks;
    private final int chopPlanks, chopSticks, chopSawdust, chopSticksSawdust, sawPlanks, sawSawdust;

    ModernLogChoppingRecipe(ResourceLocation id, Ingredient log, Item planks, int[] n) {
        super(id, CraftingBookCategory.MISC);
        this.log = log;
        this.planks = planks;
        chopPlanks = n[0];
        chopSticks = n[1];
        chopSawdust = n[2];
        chopSticksSawdust = n[3];
        sawPlanks = n[4];
        sawSawdust = n[5];
    }

    // ================================================================
    // Crafting (FCRecipesLogChopping + FCItemBlockLog.OnUsedInCrafting with this recipe's numbers)
    // ================================================================

    /** Exactly two stacks: one FC axe and one log of this recipe, nothing else (FC :25-66). */
    @Override
    public boolean matches(CraftingContainer inv, Level level) {
        boolean axe = false, found = false;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            if (log.test(stack)) {
                if (found) return false;
                found = true;
            } else if (axeTier(stack) >= 0) {
                if (axe) return false;
                axe = true;
            } else {
                return false;
            }
        }
        return axe && found;
    }

    /** A weak axe (harvest level 1 or less: wood, stone, gold) makes sticks, any other planks (FC :114-132). */
    @Override
    public ItemStack assemble(CraftingContainer inv, RegistryAccess access) {
        int tier = tierIn(inv);
        if (tier < 0) return ItemStack.EMPTY;
        return weakAxe(tier) ? new ItemStack(Items.STICK, chopSticks) : new ItemStack(planks, chopPlanks);
    }

    /** FC's weak-axe test (Common FCRecipesLogChopping.java:116): harvest level 1 or less makes sticks. */
    private static boolean weakAxe(int tier) {
        return tier <= 1;
    }

    /**
     * FC's crafting rules for the grid (the axe is damaged or kept and plays FC's chop sound; the log is consumed),
     * then, for a server crafter only (set by ResultSlot.onTake and WorkbenchFallback), this recipe's sawdust ejected
     * at the player as FCItemBlockLog ejects a log's (Common :35-46). No bark. The FC conversions here may give the log
     * item its headless id, as any BTW container does when it holds one; {@link #matches} never does.
     */
    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer inv) {
        int tier = tierIn(inv);
        ItemStack out = assemble(inv, null);
        NonNullList<ItemStack> remaining = FCCraftingRecipes.fcRemainingItems(inv,
                out.isEmpty() ? null : ItemStackHelper.toFcStack(out));
        // the branch assemble took, by the axe (a recipe's planks item may itself be a stick)
        if (!out.isEmpty() && ForgeHooks.getCraftingPlayer() instanceof ServerPlayer player) {
            ejectSawdust(player, weakAxe(tier) ? chopSticksSawdust : chopSawdust);
        }
        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    /** FC harvest level of an FC axe in {@code stack} (FCItemAxe and subclasses), else -1. Never creates a headless id. */
    static int axeTier(ItemStack stack) {
        if (stack.isEmpty()) return -1;
        return harvestLevel(ProxyItem.fcItemFor(stack.getItem()));
    }

    private static int harvestLevel(btw.modern.Item fc) {
        if (fc == null || FC_AXE == null || TOOL_MATERIAL == null || !FC_AXE.isInstance(fc)) return -1;
        try {
            return ((btw.modern.EnumToolMaterial) TOOL_MATERIAL.get(fc)).getHarvestLevel();
        } catch (Throwable t) {
            failOnce("tool material of " + fc.getClass().getName(), t);
            return -1;
        }
    }

    private static int tierIn(CraftingContainer inv) {
        for (int i = 0; i < inv.getContainerSize(); i++) {
            int tier = axeTier(inv.getItem(i));
            if (tier >= 0) return tier;
        }
        return -1;
    }

    /** {@code count} FC sawdust at the player, through FC's FCUtilsItem.EjectStackWithRandomVelocity. */
    private static void ejectSawdust(ServerPlayer player, int count) {
        if (count <= 0 || !player.isAlive()) return;
        try {
            MethodHandle handle = eject;
            if (handle == null) {
                handle = MethodHandles.lookup().unreflect(Class.forName("net.minecraft.src.btw.util.FCUtilsItem")
                        .getMethod("EjectStackWithRandomVelocity", btw.modern.World.class, double.class, double.class,
                                double.class, btw.modern.ItemStack.class));
                eject = handle;
            }
            Object sawDust = Class.forName("net.minecraft.src.btw.core.FCBetterThanWolves").getField("fcItemSawDust")
                    .get(null);
            if (!(sawDust instanceof btw.modern.Item item)) throw new IllegalStateException("fcItemSawDust is " + sawDust);
            PlayerBridge bridge = PlayerBridge.getOrCreate(player);
            handle.invoke(bridge.worldObj, bridge.posX, bridge.posY, bridge.posZ, new btw.modern.ItemStack(item, count, 0));
        } catch (Throwable t) {
            failOnce("sawdust", t);
        }
    }

    // ================================================================
    // The saw (ModernWoodSawHooks)
    // ================================================================

    /** The saw's planks: {@code saw_planks} of the planks item. */
    public ItemStack sawPlanks() {
        return new ItemStack(planks, sawPlanks);
    }

    /** The saw's sawdust count. */
    public int sawSawdust() {
        return sawSawdust;
    }

    /** The recipe FC's saw follows for {@code block} (a BlockItem of a recipe's log ingredient), or null. */
    public static ModernLogChoppingRecipe sawRecipeFor(Block block) {
        return block == null ? null : sawIndex.get(block);
    }

    /**
     * Rebuilds the saw's index from the installed recipes (RecipeBridge.injectRecipes, after the map is installed, and
     * on every re-injection): each recipe's log blocks, recipes in id order, the first one wins. Returns the number of
     * blocks indexed.
     */
    // FCMOD-INTEGRATION(1.20.1): bamboo_planks: one data file drives both plank routes; the saw index follows /reload
    public static int rebuildSawIndex(Collection<Recipe<?>> recipes) {
        Map<Block, ModernLogChoppingRecipe> index = new IdentityHashMap<>();
        recipes.stream()
                .filter(r -> r instanceof ModernLogChoppingRecipe)
                .map(r -> (ModernLogChoppingRecipe) r)
                .sorted(Comparator.comparing(r -> r.getId().toString()))
                .forEach(r -> {
                    for (ItemStack stack : r.log.getItems()) {
                        if (stack.getItem() instanceof BlockItem blockItem) index.putIfAbsent(blockItem.getBlock(), r);
                    }
                });
        sawIndex = index;
        return index.size();
    }

    /** The world is gone (RecipeBridge.onServerStopped): the saw follows no recipe until the next injection. */
    public static void clearSawIndex() {
        sawIndex = Map.of();
    }

    // ================================================================
    // JEI (btw.forge.jei.BTWJeiPlugin; display only)
    // ================================================================

    /**
     * Display-only crafting recipes for JEI, which does not list special recipes: per recipe, the FC axes of harvest
     * level 2 or more with a log -&gt; its chop planks, and the weaker FC axes with a log -&gt; its chop sticks. The
     * axes are FC's FCItemAxe instances in FC's item table, as 1.20.1 items.
     */
    public static List<CraftingRecipe> jeiDisplays(RecipeManager manager) {
        List<CraftingRecipe> out = new ArrayList<>();
        if (manager == null || FC_AXE == null) return out;
        Set<Item> strongAxes = new LinkedHashSet<>(), weakAxes = new LinkedHashSet<>();
        for (btw.modern.Item fc : btw.modern.Item.itemsList) {
            int tier = harvestLevel(fc);
            if (tier < 0) continue;
            ItemStack mc = ItemStackHelper.fcIdToMcStack(fc.itemID, 1, 0);
            if (mc.isEmpty()) continue;
            (weakAxe(tier) ? weakAxes : strongAxes).add(mc.getItem());
        }
        manager.getAllRecipesFor(RecipeType.CRAFTING).stream()
                .filter(r -> r instanceof ModernLogChoppingRecipe)
                .map(r -> (ModernLogChoppingRecipe) r)
                .sorted(Comparator.comparing(r -> r.getId().toString()))
                .forEach(r -> {
                    if (!strongAxes.isEmpty() && r.chopPlanks > 0) {
                        out.add(display(r, "planks", new ItemStack(r.planks, r.chopPlanks), strongAxes));
                    }
                    if (!weakAxes.isEmpty() && r.chopSticks > 0) {
                        out.add(display(r, "sticks", new ItemStack(Items.STICK, r.chopSticks), weakAxes));
                    }
                });
        return out;
    }

    private static CraftingRecipe display(ModernLogChoppingRecipe r, String kind, ItemStack result, Set<Item> axes) {
        ResourceLocation id = new ResourceLocation(BTWForgeMod.MOD_ID, "jei/modern_log_chopping/"
                + r.getId().getNamespace() + "/" + r.getId().getPath() + "/" + kind);
        return new ShapelessRecipe(id, "", CraftingBookCategory.MISC, result,
                NonNullList.of(Ingredient.EMPTY, Ingredient.of(axes.toArray(new Item[0])), r.log));
    }

    // ================================================================
    // Serializer and registration
    // ================================================================

    /**
     * {@code {"type": "betterthanwolves:modern_log_chopping", "log": <ingredient>, "planks": "<item>",
     * "chop_planks": 1, "chop_sticks": 1, "chop_sawdust": 1, "chop_sticks_sawdust": 2, "saw_planks": 2,
     * "saw_sawdust": 1}}: the numbers default to these and are clamped to 0..64. Synced to clients by value.
     */
    public static final class Serializer implements RecipeSerializer<ModernLogChoppingRecipe> {
        private static final String[] KEYS =
                {"chop_planks", "chop_sticks", "chop_sawdust", "chop_sticks_sawdust", "saw_planks", "saw_sawdust"};
        private static final int[] DEFAULTS = {1, 1, 1, 2, 2, 1};

        @Override
        public ModernLogChoppingRecipe fromJson(ResourceLocation id, JsonObject json) {
            if (!json.has("log")) throw new JsonSyntaxException("Missing log in " + id);
            Ingredient log = Ingredient.fromJson(GsonHelper.isArrayNode(json, "log")
                    ? GsonHelper.getAsJsonArray(json, "log") : GsonHelper.getAsJsonObject(json, "log"), false);
            String planksName = GsonHelper.getAsString(json, "planks");
            Item planks = ForgeRegistries.ITEMS.getValue(new ResourceLocation(planksName));
            if (planks == null || planks == Items.AIR) {
                throw new JsonSyntaxException("Unknown planks item '" + planksName + "' in " + id);
            }
            int[] n = new int[KEYS.length];
            for (int i = 0; i < KEYS.length; i++) {
                n[i] = Math.max(0, Math.min(64, GsonHelper.getAsInt(json, KEYS[i], DEFAULTS[i])));
            }
            return new ModernLogChoppingRecipe(id, log, planks, n);
        }

        @Override
        public ModernLogChoppingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Ingredient log = Ingredient.fromNetwork(buf);
            Item planks = buf.readItem().getItem();
            int[] n = new int[KEYS.length];
            for (int i = 0; i < KEYS.length; i++) n[i] = buf.readVarInt();
            return new ModernLogChoppingRecipe(id, log, planks, n);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, ModernLogChoppingRecipe r) {
            r.log.toNetwork(buf);
            buf.writeItem(new ItemStack(r.planks));
            for (int v : new int[] {r.chopPlanks, r.chopSticks, r.chopSawdust, r.chopSticksSawdust, r.sawPlanks,
                    r.sawSawdust}) {
                buf.writeVarInt(v);
            }
        }
    }

    /** Registers {@link #SERIALIZER} (the data files name it; clients need it to read the synced recipes). */
    @Mod.EventBusSubscriber(modid = BTWForgeMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {}

        @SubscribeEvent
        public static void onRegister(RegisterEvent event) {
            event.register(ForgeRegistries.Keys.RECIPE_SERIALIZERS,
                    helper -> helper.register(SERIALIZER_ID, SERIALIZER));
        }
    }

    // ================================================================
    // FC lookups
    // ================================================================

    private static Class<?> load(String name) {
        try {
            return Class.forName(name, false, ModernLogChoppingRecipe.class.getClassLoader());
        } catch (Throwable t) {
            LOGGER.warn("[BTW-Recipes] FC class {} not found; modern log chopping matches no axe: {}", name, t.toString());
            return null;
        }
    }

    private static Field toolMaterial() {
        try {
            return Class.forName("net.minecraft.src.btw.item.FCItemTool", false,
                    ModernLogChoppingRecipe.class.getClassLoader()).getField("toolMaterial");
        } catch (Throwable t) {
            LOGGER.warn("[BTW-Recipes] FCItemTool.toolMaterial not found; modern log chopping matches no axe: {}",
                    t.toString());
            return null;
        }
    }

    private static void failOnce(String what, Throwable t) {
        if (FAILURES_LOGGED.add(what)) {
            LOGGER.warn("[BTW-Recipes] modern log chopping: {} failed (further failures are not logged): {}", what,
                    t.toString(), t);
        }
    }
}
