package btw.forge.client;

import btw.forge.BTWForgeMod;
import btw.forge.ProxyBlock;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

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

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(PLACEHOLDER_BLOCK_MODEL);
        event.register(PLACEHOLDER_ITEM_MODEL);
        LOGGER.info("BTW: Registered placeholder models for baking.");
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
    }
}
