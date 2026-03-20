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
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * Client-side event handler that injects baked models for all BTW ProxyBlocks
 * and proxy items at runtime. This eliminates the need for hundreds of
 * individual blockstate, model, and item model JSON files.
 *
 * <p>The approach:
 * <ol>
 *   <li>During {@link ModelEvent.RegisterAdditional}, register a placeholder
 *       cube model and placeholder item model so their textures get stitched
 *       into the block/item atlas.</li>
 *   <li>During {@link ModelEvent.ModifyBakingResult}, copy the baked
 *       placeholder model into every {@link ModelResourceLocation} that
 *       corresponds to a BTW ProxyBlock or proxy item.</li>
 * </ol>
 *
 * <p>The result: all BTW blocks render as simple cubes with a distinctive
 * brown "BTW" placeholder texture instead of the purple/black missing texture.
 * Standalone items render with a brown "?" placeholder texture.
 */
@Mod.EventBusSubscriber(modid = BTWForgeMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class BTWClientEvents {

    private static final Logger LOGGER = LogManager.getLogger("BTW-Client");

    /** Placeholder cube model for blocks (defined in models/block/btw_placeholder_cube.json). */
    private static final ResourceLocation PLACEHOLDER_BLOCK_MODEL =
            new ResourceLocation(BTWForgeMod.MOD_ID, "block/btw_placeholder_cube");

    /** Placeholder flat item model (defined in models/item/btw_placeholder_item.json). */
    private static final ResourceLocation PLACEHOLDER_ITEM_MODEL =
            new ResourceLocation(BTWForgeMod.MOD_ID, "item/btw_placeholder_item");

    /**
     * Register additional models that need to be baked. This ensures the
     * placeholder textures get stitched into the texture atlas even though
     * no blockstate JSON references them.
     */
    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(PLACEHOLDER_BLOCK_MODEL);
        event.register(PLACEHOLDER_ITEM_MODEL);
        LOGGER.info("BTW: Registered placeholder models for baking.");
    }

    /**
     * After all models have been baked, inject the placeholder model into
     * the model map for every BTW block state and item that lacks a proper
     * model.
     */
    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ResourceLocation, BakedModel> models = event.getModels();

        // Retrieve the baked placeholder models
        BakedModel blockPlaceholder = models.get(PLACEHOLDER_BLOCK_MODEL);
        BakedModel itemPlaceholder = models.get(PLACEHOLDER_ITEM_MODEL);

        if (blockPlaceholder == null) {
            LOGGER.error("BTW: Block placeholder model not found after baking! " +
                    "BTW blocks will show missing texture.");
            return;
        }
        if (itemPlaceholder == null) {
            LOGGER.warn("BTW: Item placeholder model not found after baking. " +
                    "Using block placeholder for items too.");
            itemPlaceholder = blockPlaceholder;
        }

        int injectedBlockStates = 0;
        int injectedBlockItems = 0;
        int injectedStandaloneItems = 0;
        int skippedRealModels = 0;

        // The Forge "missing" model - used to detect slots with no real model
        // Must use ModelResourceLocation (not plain ResourceLocation) to find it in the map
        BakedModel missingModel = models.get(
                new ModelResourceLocation(new ResourceLocation("minecraft", "builtin/missing"), "missing"));

        // --- Inject models for all ProxyBlocks ---
        for (Block block : ForgeRegistries.BLOCKS) {
            if (!(block instanceof ProxyBlock)) continue;

            ResourceLocation blockRegName = ForgeRegistries.BLOCKS.getKey(block);
            if (blockRegName == null) continue;

            // Inject model for every block state variant (meta=0 through meta=15)
            // Only inject placeholder if no real model was baked from JSON
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                ModelResourceLocation mrl = BlockModelShaper.stateToModelLocation(state);
                BakedModel existing = models.get(mrl);
                if (existing == null || existing == missingModel) {
                    models.put(mrl, blockPlaceholder);
                    injectedBlockStates++;
                } else {
                    skippedRealModels++;
                }
            }

            // Inject the inventory item model for this block
            // Only if no real model exists
            ModelResourceLocation itemMrl =
                    new ModelResourceLocation(blockRegName, "inventory");
            BakedModel existingItem = models.get(itemMrl);
            if (existingItem == null || existingItem == missingModel) {
                models.put(itemMrl, blockPlaceholder);
                injectedBlockItems++;
            } else {
                skippedRealModels++;
            }
        }

        // --- Inject models for standalone BTW items (item_<id>) ---
        // Only inject placeholder if no real model was baked from JSON
        for (Item item : ForgeRegistries.ITEMS) {
            ResourceLocation itemRegName = ForgeRegistries.ITEMS.getKey(item);
            if (itemRegName == null) continue;
            if (!BTWForgeMod.MOD_ID.equals(itemRegName.getNamespace())) continue;

            String path = itemRegName.getPath();
            // Only handle standalone items (item_NNN), not block items (block_NNN)
            // Block items are already handled above
            if (!path.startsWith("item_")) continue;

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

        LOGGER.info("BTW: Injected placeholder models - {} block states, {} block items, {} standalone items. Kept {} real models.",
                injectedBlockStates, injectedBlockItems, injectedStandaloneItems, skippedRealModels);
    }
}
