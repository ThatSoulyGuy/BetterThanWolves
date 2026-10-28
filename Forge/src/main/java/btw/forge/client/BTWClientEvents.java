package btw.forge.client;

import btw.forge.BTWForgeMod;
import btw.forge.BTWMenuTypes;
import btw.forge.FCContainerMenu;
import btw.forge.ProxyBlock;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Client-side event handler that injects deferred-capture FCBakedModels
 * for all BTW ProxyBlocks at runtime.
 *
 * <p>During model baking (worker thread), we create lightweight FCBakedModel
 * shells that store only a reference to the FC block. On first use each model
 * captures FC's actual vertex output via the Tessellator and converts it to
 * BakedQuads: FC's RenderBlock for the block states, and FC's RenderBlockAsItem
 * per item damage for the block's item (served through the model's item overrides).
 */
@Mod.EventBusSubscriber(modid = BTWForgeMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class BTWClientEvents {

    private static final Logger LOGGER = LogManager.getLogger("BTW-Client");

    private static final ResourceLocation PLACEHOLDER_BLOCK_MODEL =
            new ResourceLocation(BTWForgeMod.MOD_ID, "block/btw_placeholder_cube");

    private static final ResourceLocation PLACEHOLDER_ITEM_MODEL =
            new ResourceLocation(BTWForgeMod.MOD_ID, "item/btw_placeholder_item");

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(BTWMenuTypes.FC_CONTAINER.get(), FCContainerScreen::new);
            LOGGER.info("BTW: Registered FCContainerScreen for FC container menus.");

            FCEntityRenderer.initFcRenderers();

            registerFcTileEntityRenderers();

            registerBlockEntityRendererSafe();

            Item compositeBow = btw.forge.ProxyRegistry.getModernItem(520);
            if (compositeBow != null) {
                net.minecraft.client.renderer.item.ItemProperties.register(compositeBow, new ResourceLocation("pull"),
                        (stack, level, entity, seed) -> entity == null || entity.getUseItem() != stack ? 0.0F
                                : (stack.getUseDuration() - entity.getUseItemRemainingTicks()) / 20.0F);
                net.minecraft.client.renderer.item.ItemProperties.register(compositeBow, new ResourceLocation("pulling"),
                        (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F);
            }
        });
    }

    @Mod.EventBusSubscriber(modid = BTWForgeMod.MOD_ID, value = Dist.CLIENT)
    public static final class BowFov {

        private BowFov() {}

        @SubscribeEvent
        public static void onComputeFov(net.minecraftforge.client.event.ComputeFovModifierEvent event) {
            var player = event.getPlayer();
            if (!player.isUsingItem() || !(player.getUseItem().getItem() instanceof btw.forge.ProxyItem)
                    || !(btw.forge.ProxyItem.fcItemFor(player.getUseItem().getItem()) instanceof btw.modern.ItemBow)) {
                return;
            }
            float draw = player.getTicksUsingItem() / 20.0F;
            draw = draw > 1.0F ? 1.0F : draw * draw;
            event.setNewFovModifier(event.getNewFovModifier() * (1.0F - draw * 0.15F));
        }
    }

    private static boolean beRendererRegistered = false;
    private static void registerBlockEntityRendererSafe() {
        if (beRendererRegistered) return;
        if (btw.forge.ProxyBlockEntity.TYPE == null) {
            LOGGER.warn("BTW: ProxyBlockEntity.TYPE is null — cannot register BlockEntityRenderer yet.");
            return;
        }
        beRendererRegistered = true;
        try {
            net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
                    btw.forge.ProxyBlockEntity.TYPE, FCBlockEntityRenderer::new);
            LOGGER.info("BTW: Registered FCBlockEntityRenderer for TYPE={}.",
                    btw.forge.ProxyBlockEntity.TYPE);
        } catch (Exception e) {
            LOGGER.warn("BTW: Failed to register BlockEntityRenderer: {}", e.getMessage());
        }
    }

    /**
     * Registers FC's TileEntitySpecialRenderers into btw.modern.TileEntityRenderer.
     * These were originally registered in the vanilla TileEntityRenderer constructor
     * patch (vanilla/client TileEntityRenderer.java:67, :74-78) but lost due to the
     * Modern-Common stub replacement.
     */
    private static void registerFcTileEntityRenderers() {
        var map = btw.modern.TileEntityRenderer.instance.GetSpecialRendererMap();
        // FC TESR classes live at net.minecraft.src.btw.tileentity.* (not remapped)
        // Tile entity classes may be at same package or btw.modern.*
        String fcPkg = "net.minecraft.src.btw.tileentity.";
        String modernPkg = "btw.modern.";
        String[][] registrations = {
            // {tile entity class, renderer class}
            // Try FC package first for both, fall back to modern package for TE class
            {fcPkg + "FCTileEntityCampfire",      fcPkg + "FCTileEntityCampfireRenderer"},
            {fcPkg + "FCTileEntityFurnaceBrick",  fcPkg + "FCTileEntityFurnaceBrickRenderer"},
            {fcPkg + "FCTileEntityToolPlaced",    fcPkg + "FCTileEntityToolPlacedRenderer"},
            // 1.5.2 TileEntityRenderer.java:77 maps this to FCTileEntityBasketWickerRenderer, whose
            // RenderStorageStack (:40-79) draws the stored item through RenderItem, which the capture
            // pipeline cannot texture for 1.20.1 items. Register its superclass instead: the lid pass
            // the wicker renderer runs via super.renderTileEntityAt (:31). FCBlockEntityRenderer
            // draws the storage stack natively (renderBasketStorageStack) and swings the captured
            // lid about its hinge (applyBasketLidHinge), for the hamper too.
            {fcPkg + "FCTileEntityBasketWicker",  fcPkg + "FCTileEntityBasketRenderer"},
            {fcPkg + "FCTileEntityHamper",        fcPkg + "FCTileEntityBasketRenderer"},
            // 1.5.2 TileEntityRenderer.java:67. Client's net.minecraft.src.TileEntityChestRenderer is
            // relocated to btw.modern by remapFcCode; string literals here are not. Renders the BTW
            // chest (block 1035, render type 22 = no block quads), whose FCTileEntityChest extends
            // btw.modern.TileEntityChest (found via the superclass walk).
            {modernPkg + "TileEntityChest",       modernPkg + "TileEntityChestRenderer"},
        };
        int count = 0;
        for (String[] reg : registrations) {
            try {
                Class<?> teClass = Class.forName(reg[0]);
                Class<?> rendererClass = Class.forName(reg[1]);
                btw.modern.TileEntitySpecialRenderer renderer =
                        (btw.modern.TileEntitySpecialRenderer) rendererClass.getConstructor().newInstance();
                renderer.setTileEntityRenderer(btw.modern.TileEntityRenderer.instance);
                map.put(teClass, renderer);
                count++;
            } catch (Throwable e) {
                LOGGER.warn("BTW: Could not register FC TESR {} → {}: {}", reg[0], reg[1], e.toString());
            }
        }
        LOGGER.info("BTW: Registered {} FC tile entity special renderers.", count);
    }

    /**
     * Registers FCEntityRenderer for all FC proxy entity types.
     * Must happen during RegisterRenderers (not later) because MC
     * bakes the renderer map and ignores later registrations.
     */
    @SubscribeEvent
    public static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        var entityTypes = btw.forge.BTWEntityRegistration.getAllEntityTypes();
        int count = 0;
        for (var type : entityTypes) {
            try {
                @SuppressWarnings("unchecked")
                net.minecraft.world.entity.EntityType<net.minecraft.world.entity.Entity> castType =
                        (net.minecraft.world.entity.EntityType<net.minecraft.world.entity.Entity>) (Object) type;
                // Use BOTH registration paths to ensure the renderer is found
                event.registerEntityRenderer(castType, FCEntityRenderer::new);
                net.minecraft.client.renderer.entity.EntityRenderers.register(castType, FCEntityRenderer::new);
                count++;
            } catch (Exception e) {
                LOGGER.warn("Could not register entity renderer for {}: {}", type, e.getMessage(), e);
            }
        }
        LOGGER.info("BTW: Registered FCEntityRenderer for {} FC entity types.", count);

        // Register BlockEntityRenderer for FC tile entities.
        // Done in RegisterRenderers event AND also deferred to FMLClientSetupEvent
        // to cover all timing scenarios.
        registerBlockEntityRendererSafe();
    }

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(PLACEHOLDER_BLOCK_MODEL);
        event.register(PLACEHOLDER_ITEM_MODEL);

        // Register ALL BTW item variant models so MC stitches their textures.
        // Without this, per-subtype models (bark_oak, bark_spruce, etc.) are
        // never loaded and their textures are missing from the atlas.
        int variantModels = 0;
        try {
            // Scan for item model files that aren't the main item_XXXX models
            var resourceManager = net.minecraft.server.packs.resources.ResourceManager.class;
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc != null && mc.getResourceManager() != null) {
                var resources = mc.getResourceManager().listResources(
                        "models/item",
                        loc -> loc.getNamespace().equals(btw.forge.BTWForgeMod.MOD_ID)
                                && loc.getPath().endsWith(".json"));
                for (var entry : resources.entrySet()) {
                    ResourceLocation loc = entry.getKey();
                    // Convert models/item/foo.json → betterthanwolves:item/foo
                    String path = loc.getPath();
                    path = path.substring("models/".length()); // item/foo.json
                    path = path.substring(0, path.length() - 5); // item/foo
                    ResourceLocation modelLoc = new ResourceLocation(loc.getNamespace(), path);
                    event.register(modelLoc);
                    variantModels++;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Could not scan for BTW item variant models: {}", e.getMessage());
        }

        LOGGER.info("BTW: Registered {} placeholder + {} variant item models for baking.",
                2, variantModels);
    }

    /**
     * ModifyBakingResult (below) runs in the background stage of a resource reload, before the new
     * block atlas is uploaded on the game thread (ModelManager.apply). Live FC captures on mesh
     * threads keep resolving sprites in between, from the old atlas; clear again once the new one
     * is in place so none of those pre-reload sprites outlives the reload.
     */
    @SubscribeEvent
    public static void onTextureStitchPost(TextureStitchEvent.Post event) {
        if (InventoryMenu.BLOCK_ATLAS.equals(event.getAtlas().location())) {
            FCBakedModel.clearSpriteCache();
        }
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ResourceLocation, BakedModel> models = event.getModels();

        FCBakedModel.clearSpriteCache();

        BakedModel blockPlaceholder = models.get(PLACEHOLDER_BLOCK_MODEL);
        BakedModel itemPlaceholder = models.get(PLACEHOLDER_ITEM_MODEL);

        if (blockPlaceholder == null) {
            LOGGER.error("BTW: Block placeholder model not found!");
            // No FCBakedModels are created on this path, so the lazy fallback in
            // FCBakedModel.ensureIconsRegistered never runs; the icon pass needs no placeholder.
            // (FCItemSubtypeModel leaves FCBakedModel block items alone, so no FC ItemBlock is
            // affected by later reloads injecting subtype models after this pass.)
            registerBlockIconsEagerly();
            return;
        }
        // item/generated: the display transforms of the block-item damages 1.5.2 drew as flat icons.
        BakedModel flatItemTemplate = itemPlaceholder;
        if (itemPlaceholder == null) {
            itemPlaceholder = blockPlaceholder;
        }

        int injectedBlockStates = 0;
        int injectedBlockItems = 0;
        int injectedStandaloneItems = 0;
        int skippedRealModels = 0;
        int generatedFCModels = 0;

        BakedModel missingModel = models.get(
                new ModelResourceLocation(new ResourceLocation("minecraft", "builtin/missing"), "missing"));

        // For each ProxyBlock, create a deferred-capture FCBakedModel.
        // No vertex capture happens here — it's deferred to the render thread.
        for (Block block : ForgeRegistries.BLOCKS) {
            if (!(block instanceof ProxyBlock proxyBlock)) continue;

            ResourceLocation blockRegName = ForgeRegistries.BLOCKS.getKey(block);
            if (blockRegName == null) continue;

            btw.modern.Block fcBlock = proxyBlock.getFcBlock();
            BakedModel modelToUse;
            if (fcBlock != null) {
                // Create a deferred-capture model — capture happens on render thread
                modelToUse = FCBakedModel.deferred(fcBlock, proxyBlock.getLegacyId(), flatItemTemplate);
                generatedFCModels++;
            } else {
                modelToUse = blockPlaceholder;
            }

            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                ModelResourceLocation mrl = BlockModelShaper.stateToModelLocation(state);
                models.put(mrl, modelToUse);
                injectedBlockStates++;
            }

            // The same model: FCBakedModel.getOverrides resolves the stack's damage to FC's
            // RenderBlockAsItem capture (or flat icon) for that damage (1.5.2 RenderItem.java:374-398).
            ModelResourceLocation itemMrl =
                    new ModelResourceLocation(blockRegName, "inventory");
            models.put(itemMrl, modelToUse);
            injectedBlockItems++;
        }

        // Standalone BTW items
        for (Item item : ForgeRegistries.ITEMS) {
            ResourceLocation itemRegName = ForgeRegistries.ITEMS.getKey(item);
            if (itemRegName == null) continue;
            if (!BTWForgeMod.MOD_ID.equals(itemRegName.getNamespace())) continue;
            if (!itemRegName.getPath().startsWith("item_")) continue;

            ModelResourceLocation mrl =
                    new ModelResourceLocation(itemRegName, "inventory");
            BakedModel existing = models.get(mrl);
            if (existing == null || existing == missingModel) {
                models.put(mrl, itemPlaceholder);
                injectedStandaloneItems++;
            } else {
                skippedRealModels++;
            }
        }

        LOGGER.info("BTW: Injected {} block states, {} block items, {} standalone items. " +
                        "{} deferred FCBakedModels created. Kept {} real models.",
                injectedBlockStates, injectedBlockItems, injectedStandaloneItems,
                generatedFCModels, skippedRealModels);

        // Inject per-damage-value models for FC items with different textures
        // per subtype (tree bark, tuning forks, ancient prophecies, etc.)
        btw.modern.IconRegister iconCapturer = new btw.modern.IconRegister() {
            @Override
            public btw.modern.Icon registerIcon(String name) {
                return new btw.forge.NamedIcon(name);
            }
            @Override
            public btw.modern.Icon registerIcon(String name, btw.modern.TextureStitched texture) {
                return new btw.forge.NamedIcon(name);
            }
        };
        // Populate itemIcon on EVERY FC item, not just subtype items. FC entity renderers
        // read Item.itemsList[id].itemIcon.getMinU() directly (e.g. FCClientRenderUrn draws
        // its contained item's icon) and NPE if it's null — which is why soul urns didn't
        // render. NamedIcon captures the texture name so the recording renderer maps it to a
        // modern sprite; registering is cheap (no atlas lookup) and idempotent with the
        // subtype pass below.
        int itemIconsPopulated = 0;
        for (int id = 0; id < btw.modern.Item.itemsList.length; id++) {
            btw.modern.Item fcItem = btw.modern.Item.itemsList[id];
            if (fcItem == null) continue;
            try {
                fcItem.registerIcons(iconCapturer);
                itemIconsPopulated++;
            } catch (Exception ignored) {}
        }
        LOGGER.info("BTW: Populated itemIcon on {} FC items.", itemIconsPopulated);
        int subtypeModels = FCItemSubtypeModel.injectSubtypeModels(models, iconCapturer);
        LOGGER.info("BTW: Injected subtype models for {} items with per-damage textures.", subtypeModels);

        registerBlockIconsEagerly();
    }

    /**
     * Block icons, eagerly (triage MC3-R1): 1.5.2 registered every block's icons before anything drew
     * (TextureMap.refreshTextures). Until this pass, held/worn block items (ItemBlock sprite number 0,
     * ItemRenderer's 3D branch) drew with null face icons, textured with the mob's own skin, and flat
     * ones (torches, flowers) fell back to the item icon. Called AFTER injectSubtypeModels, which
     * also skips FCBakedModel block items: an FC ItemBlock whose getIconFromDamage returns per-damage
     * block icons must keep its FCBakedModel, whose item overrides already draw each damage as 1.5.2
     * did. Runs once per session (NamedIcons hold names only, so a resource reload needs no
     * re-registration). Never throws.
     */
    private static void registerBlockIconsEagerly() {
        try {
            FCBakedModel.ensureGlobalIconsRegistered();
        } catch (Throwable t) {
            LOGGER.warn("BTW: Eager block icon registration failed: {}", t.toString());
        }
    }

    /**
     * Registers item color handlers for FC items that use
     * {@code getColorFromItemStack()} to tint per damage value
     * (wool colors, candle colors, dyed items, etc.).
     *
     * <p>FC items return 0xFFFFFF (white / no tint) by default.
     * Subclasses like wool and candles override to return the
     * appropriate color for each metadata value. We bridge this
     * directly — no reimplementation needed.</p>
     */
    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        List<Item> tintableItems = new ArrayList<>();

        for (int id = 175; id < btw.modern.Item.itemsList.length; id++) {
            btw.modern.Item fcItem = (id < btw.modern.Item.itemsList.length)
                    ? btw.modern.Item.itemsList[id] : null;
            if (fcItem == null) continue;
            if (!fcItem.getHasSubtypes()) continue;

            // Check if this FC item actually returns non-white colors
            // by testing a few damage values
            boolean hasColors = false;
            for (int dmg = 0; dmg <= 15; dmg++) {
                btw.modern.ItemStack testStack = new btw.modern.ItemStack(id, 1, dmg);
                int color = fcItem.getColorFromItemStack(testStack, 0);
                if (color != 0xFFFFFF) {
                    hasColors = true;
                    break;
                }
            }
            if (!hasColors) continue;

            Item modernItem = btw.forge.ProxyRegistry.getModernItem(id);
            if (modernItem != null) {
                tintableItems.add(modernItem);
            }
        }

        if (!tintableItems.isEmpty()) {
            event.register((stack, tintIndex) -> {
                btw.modern.ItemStack fcStack = btw.forge.ItemStackHelper.toFcStack(stack);
                if (fcStack != null) {
                    btw.modern.Item fcItem = fcStack.getItem();
                    if (fcItem != null) {
                        return fcItem.getColorFromItemStack(fcStack, tintIndex);
                    }
                }
                return 0xFFFFFF;
            }, tintableItems.toArray(new Item[0]));

            LOGGER.info("BTW: Registered item color tinting for {} subtype items.", tintableItems.size());
        }
    }
}
