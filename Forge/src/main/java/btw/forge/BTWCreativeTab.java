package btw.forge;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Creative tab for all Better Than Wolves blocks and items.
 */
@Mod.EventBusSubscriber(modid = BTWForgeMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BTWCreativeTab {

    private static final Logger LOGGER = LogManager.getLogger("BTW-CreativeTab");

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BTWForgeMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> BTW_TAB = CREATIVE_MODE_TABS.register("btw_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.betterthanwolves"))
                    .icon(() -> {
                        // Use gear block as icon (ID 228) or fall back to a placeholder
                        ProxyBlock gearBox = ProxyRegistry.getProxy(1065); // GearBox
                        if (gearBox != null) {
                            return new ItemStack(gearBox);
                        }
                        // Fallback to anvil (soulforge)
                        ProxyBlock anvil = ProxyRegistry.getProxy(221);
                        if (anvil != null) {
                            return new ItemStack(anvil);
                        }
                        // Last resort - any BTW block
                        for (int id = 175; id < 256; id++) {
                            ProxyBlock proxy = ProxyRegistry.getProxy(id);
                            if (proxy != null) {
                                return new ItemStack(proxy);
                            }
                        }
                        return ItemStack.EMPTY;
                    })
                    .build()
    );

    /**
     * Populates the creative tab with all BTW blocks and items.
     */
    @SubscribeEvent
    public static void buildContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == BTW_TAB.getKey()) {
            int blocksAdded = 0;
            int itemsAdded = 0;

            // Add all BTW blocks (IDs 175-4095)
            for (int id = 175; id < 4096; id++) {
                ProxyBlock proxy = ProxyRegistry.getProxy(id);
                if (proxy != null) {
                    try {
                        event.accept(proxy);
                        blocksAdded++;
                    } catch (Exception e) {
                        // Skip blocks that can't be added
                    }
                }
            }

            // Add all BTW standalone items
            // FC items start at index 478 (ParseID 222 + 256 offset).
            // Skip indices 222-477 which are either blocks or vanilla items.
            for (int id = 478; id < 32000; id++) {
                net.minecraft.world.item.Item item = ProxyRegistry.getModernItem(id);
                if (item != null && item != net.minecraft.world.item.Items.AIR) {
                    // Skip block items (already added above)
                    if (item instanceof net.minecraft.world.item.BlockItem) {
                        continue;
                    }
                    // Skip vanilla items that leaked into ProxyRegistry
                    net.minecraft.resources.ResourceLocation regName =
                            net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item);
                    if (regName != null && !BTWForgeMod.MOD_ID.equals(regName.getNamespace())) {
                        continue;
                    }
                    try {
                        // Ask FC for valid subtypes via getSubItems.
                        // FC items with subtypes (scrolls, wool, candles, etc.)
                        // populate the list with their valid variants.
                        btw.modern.Item fcItem = (id < btw.modern.Item.itemsList.length)
                                ? btw.modern.Item.itemsList[id] : null;
                        if (fcItem != null && fcItem.getHasSubtypes()) {
                            java.util.List<btw.modern.ItemStack> subItems = new java.util.ArrayList<>();
                            fcItem.getSubItems(id, null, subItems);
                            if (!subItems.isEmpty()) {
                                for (btw.modern.ItemStack fcSub : subItems) {
                                    ItemStack mcSub = ItemStackHelper.toMcStack(fcSub);
                                    if (!mcSub.isEmpty()) {
                                        event.accept(mcSub);
                                        itemsAdded++;
                                    }
                                }
                            } else {
                                // FC didn't populate subtypes — add the base item
                                event.accept(item);
                                itemsAdded++;
                            }
                        } else {
                            event.accept(item);
                            itemsAdded++;
                        }
                    } catch (Exception e) {
                        // Skip items that can't be added
                    }
                }
            }

            LOGGER.info("Added {} blocks and {} items to BTW creative tab.", blocksAdded, itemsAdded);
        }
    }
}
