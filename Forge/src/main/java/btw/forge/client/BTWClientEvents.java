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
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
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
 * shells that store only a reference to the FC block. On the first
 * {@code getQuads()} call (render thread, with GL context), each model
 * captures FC's actual vertex output via the Tessellator and converts
 * it to BakedQuads. This ensures FC's RenderBlockAsItem runs with full
 * GL support and produces correct geometry for non-cube blocks.
 */
@Mod.EventBusSubscriber(modid = BTWForgeMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class BTWClientEvents {

    private static final Logger LOGGER = LogManager.getLogger("BTW-Client");

    private static final ResourceLocation PLACEHOLDER_BLOCK_MODEL =
            new ResourceLocation(BTWForgeMod.MOD_ID, "block/btw_placeholder_cube");

    private static final ResourceLocation PLACEHOLDER_ITEM_MODEL =
            new ResourceLocation(BTWForgeMod.MOD_ID, "item/btw_placeholder_item");

    /**
     * Registers the FCContainerScreen for the FC_CONTAINER menu type.
     * This links the server-side FCContainerMenu to the client-side screen
     * so that when a container is opened, the correct GUI appears.
     */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(BTWMenuTypes.FC_CONTAINER.get(), FCContainerScreen::new);
            LOGGER.info("BTW: Registered FCContainerScreen for FC container menus.");

            // Initialize FC entity renderers (populates the renderer map
            // from FC's ClientAddEntityRenderers registration)
            FCEntityRenderer.initFcRenderers();

            // Also register BlockEntityRenderer here (belt-and-suspenders with RegisterRenderers)
            registerBlockEntityRendererSafe();
        });
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

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ResourceLocation, BakedModel> models = event.getModels();

        FCBakedModel.clearSpriteCache();

        BakedModel blockPlaceholder = models.get(PLACEHOLDER_BLOCK_MODEL);
        BakedModel itemPlaceholder = models.get(PLACEHOLDER_ITEM_MODEL);

        if (blockPlaceholder == null) {
            LOGGER.error("BTW: Block placeholder model not found!");
            return;
        }
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
                modelToUse = FCBakedModel.deferred(fcBlock, proxyBlock.getLegacyId());
                generatedFCModels++;
            } else {
                modelToUse = blockPlaceholder;
            }

            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                ModelResourceLocation mrl = BlockModelShaper.stateToModelLocation(state);
                models.put(mrl, modelToUse);
                injectedBlockStates++;
            }

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
        int subtypeModels = FCItemSubtypeModel.injectSubtypeModels(models, iconCapturer);
        LOGGER.info("BTW: Injected subtype models for {} items with per-damage textures.", subtypeModels);
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
