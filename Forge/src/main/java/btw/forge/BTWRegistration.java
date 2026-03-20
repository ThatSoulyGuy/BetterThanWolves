package btw.forge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.datafixers.types.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

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
     * Called once for each registry type (blocks, items, entities, etc.).
     */
    public static void registerAllBTWContent(net.minecraftforge.registries.RegisterEvent event) {
        if (event.getRegistryKey().equals(net.minecraftforge.registries.ForgeRegistries.Keys.BLOCKS)) {
            registerBlocks(event);
        } else if (event.getRegistryKey().equals(net.minecraftforge.registries.ForgeRegistries.Keys.ITEMS)) {
            registerItems(event);
        } else if (event.getRegistryKey().equals(net.minecraftforge.registries.ForgeRegistries.Keys.BLOCK_ENTITY_TYPES)) {
            registerBlockEntityType(event);
        }
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
     * Registers items for all BTW content:
     * 1. BlockItems for every ProxyBlock (so block drops and /give work)
     * 2. Standalone items from Item.itemsList[256..32000]
     */
    private static void registerItems(net.minecraftforge.registries.RegisterEvent event) {
        int registered = 0;

        // --- Register BlockItems for ALL ProxyBlocks ---
        // Without this, ProxyBlock.asItem() returns Items.AIR and drops are empty.
        for (int id = 175; id < btw.modern.Block.blocksList.length; id++) {
            ProxyBlock proxy = ProxyRegistry.getProxy(id);
            if (proxy == null) continue;

            try {
                // Get FC display name from the FC block
                btw.modern.Block fcBlock = proxy.getFcBlock();
                String fcName = fcBlock != null ? fcBlock.getUnlocalizedName() : "";
                String displayName = formatFcName(fcName, "Block " + id);

                net.minecraft.world.item.BlockItem blockItem =
                        new net.minecraft.world.item.BlockItem(proxy, new Item.Properties()) {
                            @Override
                            public net.minecraft.network.chat.Component getName(
                                    net.minecraft.world.item.ItemStack stack) {
                                return net.minecraft.network.chat.Component.literal(displayName);
                            }
                        };

                ResourceLocation key = new ResourceLocation(
                        BTWForgeMod.MOD_ID, "block_" + id);

                event.register(net.minecraftforge.registries.ForgeRegistries.Keys.ITEMS,
                        helper -> helper.register(key, blockItem));

                ProxyRegistry.registerItem(id, blockItem);
                registered++;
            } catch (Exception e) {
                LOGGER.error("Failed to register BlockItem for ProxyBlock ID {}: {}",
                        id, e.getMessage());
            }
        }

        // --- Register standalone FC items (non-block items) ---
        // FC items start at ParseID 222 (stored at index 478 due to +256 offset).
        // Indices 256-477 are vanilla MC 1.5.2 items (replaced by FC subclasses) - skip those.
        for (int id = 175; id < btw.modern.Item.itemsList.length; id++) {
            btw.modern.Item fcItem = btw.modern.Item.itemsList[id];
            if (fcItem == null) continue;

            // Skip vanilla item range: indices 256-477 are vanilla items with FC subclasses.
            // FC's own items start at index 478 (ParseID 222 + 256 offset).
            if (id >= 256 && id < 478) continue;

            // Skip items that already have a modern item registered
            // (block items registered above, or vanilla items)
            net.minecraft.world.item.Item existingModern = ProxyRegistry.getModernItem(id);
            if (existingModern != null) continue;

            try {
                Item.Properties props = new Item.Properties();
                props.stacksTo(fcItem.maxStackSize);
                if (fcItem.getMaxDamage() > 0) {
                    props.durability(fcItem.getMaxDamage());
                }

                String fcName = fcItem.getUnlocalizedName();
                String displayName = formatFcName(fcName, "Item " + id);

                // ProxyItem delegates ALL item interactions to the FC item class
                Item proxyItem = new ProxyItem(props, id, displayName);

                ResourceLocation key = new ResourceLocation(
                        BTWForgeMod.MOD_ID, "item_" + id);

                event.register(net.minecraftforge.registries.ForgeRegistries.Keys.ITEMS,
                        helper -> helper.register(key, proxyItem));

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
     * Formats an FC unlocalized name into a human-readable display name.
     * E.g. "fcBlockGear" → "Gear", "tile.dirt" → "Dirt", "item.helmetPlate" → "Helmet Plate"
     */
    private static String formatFcName(String fcName, String fallback) {
        if (fcName == null || fcName.isEmpty()) return fallback;

        // Strip common prefixes
        String name = fcName;
        for (String prefix : new String[]{
                "tile.", "item.", "fcBlock", "fcItem", "fc_", "FC"}) {
            if (name.startsWith(prefix)) {
                name = name.substring(prefix.length());
                break;
            }
        }
        if (name.isEmpty()) return fallback;

        // Insert spaces before capitals: "GearBox" → "Gear Box"
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (i > 0 && Character.isUpperCase(c) && !Character.isUpperCase(name.charAt(i - 1))) {
                sb.append(' ');
            }
            sb.append(c);
        }

        // Capitalize first letter
        String result = sb.toString().trim();
        if (!result.isEmpty()) {
            result = Character.toUpperCase(result.charAt(0)) + result.substring(1);
        }
        return result.isEmpty() ? fallback : result;
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

    /**
     * Registers a single generic {@link BlockEntityType} for all ProxyBlocks
     * that have FC tile entities.  The type is valid for every ProxyBlock whose
     * FC block returns non-null from {@code createNewTileEntity(null)}.
     *
     * <p>Forge 1.20.1 requires that a BlockEntityType lists all valid blocks
     * in its builder.  We collect every ProxyBlock that has a tile entity and
     * pass them all to the builder.</p>
     */
    private static void registerBlockEntityType(net.minecraftforge.registries.RegisterEvent event) {
        // Collect all ProxyBlocks whose FC block creates a tile entity
        Set<Block> validBlocks = new HashSet<>();
        for (int id = 175; id < 4096; id++) {
            ProxyBlock proxy = ProxyRegistry.getProxy(id);
            if (proxy != null && proxy.hasFcTileEntity()) {
                validBlocks.add(proxy);
            }
        }

        if (validBlocks.isEmpty()) {
            LOGGER.info("No FC blocks with tile entities found; skipping BlockEntityType registration.");
            return;
        }

        // Build the BlockEntityType with all valid blocks
        Block[] blocks = validBlocks.toArray(new Block[0]);
        BlockEntityType<ProxyBlockEntity> type = BlockEntityType.Builder.of(
                ProxyBlockEntity::new, blocks
        ).build((Type<?>) null);

        ProxyBlockEntity.TYPE = type;

        ResourceLocation key = new ResourceLocation(BTWForgeMod.MOD_ID, "proxy_block_entity");
        event.register(net.minecraftforge.registries.ForgeRegistries.Keys.BLOCK_ENTITY_TYPES,
                helper -> helper.register(key, type));

        LOGGER.info("Registered ProxyBlockEntity type for {} FC blocks with tile entities.",
                validBlocks.size());
    }
}
