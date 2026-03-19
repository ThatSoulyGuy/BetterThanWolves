package btw.forge;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Post-initialization registration that creates {@link ProxyBlock} instances
 * for all FC blocks and registers them with Forge's built-in registries.
 *
 * Called after FCBetterThanWolves and all add-ons have finished their
 * Initialize() sequence (which populates btw.modern.Block.blocksList[]).
 */
public class BTWRegistration {

    private static final Logger LOGGER = LogManager.getLogger("BTW-Registration");

    /**
     * Registers all BTW content that was populated during FC initialization.
     */
    public static void registerAllBTWContent(net.minecraftforge.registries.RegisterEvent event) {
        registerBlocks(event);
        // Items and entities registered separately or via later events
    }

    /**
     * Iterates btw.modern.Block.blocksList for IDs 175+ (BTW custom blocks)
     * and creates a ProxyBlock + BlockItem for each, registering them with
     * the Forge block/item registries and updating IDMappingService.
     */
    private static void registerBlocks(net.minecraftforge.registries.RegisterEvent event) {
        int registered = 0;
        for (int id = 175; id < 4096; id++) {
            btw.modern.Block fcBlock = btw.modern.Block.blocksList[id];
            if (fcBlock != null) {
                try {
                    ProxyBlock proxy = new ProxyBlock(id, fcBlock);
                    ResourceLocation key = new ResourceLocation(
                            BTWForgeMod.MOD_ID, "block_" + id);

                    event.register(net.minecraftforge.registries.ForgeRegistries.Keys.BLOCKS,
                            helper -> helper.register(key, proxy));

                    ProxyRegistry.registerProxy(id, proxy);
                    registered++;
                } catch (Exception e) {
                    LOGGER.error("Failed to register ProxyBlock for legacy ID {}: {}",
                            id, e.getMessage());
                }
            }
        }
        LOGGER.info("Registered {} BTW ProxyBlocks with Forge registries.", registered);
    }

    /**
     * Iterates btw.modern.Item.itemsList for IDs 256..31999 (non-block item range)
     * and registers FC-created items with Forge's built-in item registry.
     *
     * Items that already have a modern delegate (via IDMappingService) are
     * vanilla items and do not need separate registration. Only FC custom
     * items (those without a modern backing item) get a proxy registered.
     */
    private static void registerItems() {
        int registered = 0;
        for (int id = 256; id < 32000; id++) {
            btw.modern.Item fcItem = btw.modern.Item.itemsList[id];
            if (fcItem == null) continue;

            // Skip items that already have a modern item registered
            // (e.g. block items registered during registerBlocks, or vanilla items)
            net.minecraft.world.item.Item existingModern = ProxyRegistry.getModernItem(id);
            if (existingModern != null) continue;

            try {
                // Build properties from the FC item
                Item.Properties props = new Item.Properties();
                props.stacksTo(fcItem.maxStackSize);
                if (fcItem.getMaxDamage() > 0) {
                    props.durability(fcItem.getMaxDamage());
                }

                // Create a simple proxy modern item
                Item proxyItem = new Item(props);

                ResourceLocation key = new ResourceLocation(
                        BTWForgeMod.MOD_ID, "item_" + id);
                Registry.register(BuiltInRegistries.ITEM, key, proxyItem);

                // Store in ProxyRegistry so legacy ID <-> modern item lookups work
                ProxyRegistry.registerItem(id, proxyItem);

                registered++;
            } catch (Exception e) {
                LOGGER.error("Failed to register proxy item for legacy ID {}: {}",
                        id, e.getMessage());
            }
        }
        LOGGER.info("Registered {} BTW proxy items with Forge registries.", registered);
    }

    /**
     * Registers BTW entity types with Forge's entity registry.
     * Delegates to {@link BTWEntityRegistration} which knows the full
     * list of FC entity classes and their proxy mappings.
     */
    private static void registerEntities() {
        try {
            BTWEntityRegistration.registerEntities();
        } catch (Exception e) {
            LOGGER.error("Failed to register BTW entities", e);
        }
    }
}
