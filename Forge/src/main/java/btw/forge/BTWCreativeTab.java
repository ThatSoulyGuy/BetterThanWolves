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

    /** First Item.itemsList index of FC's standalone items (ParseID 222 + the 256 item offset). */
    private static final int FIRST_FC_ITEM_ID = 478;

    /** Item ids whose FC creative-tab query failed: logged once per id, the build goes on without them. */
    private static final java.util.Set<Integer> FC_FAILURES_LOGGED =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * Fills the BTW tab exactly as 1.5.2 filled its creative tabs: CreativeTabs.displayAllReleventItems
     * (vanilla/client CreativeTabs.java:134-147) lists every item whose getCreativeTab() is the tab, expanded
     * through its getSubItems (an ItemBlock: its block's tab and getSubBlocks, ItemBlock.java:216-227). The FC
     * tabs are walked in creativeTabArray order (Search and Inventory hold nothing of their own), over the
     * FC-owned item ids only: the block items of FC blocks (ProxyBlocks, ids 175-4095) and FC's standalone
     * items (ProxyItems, ids 478 and up).
     *
     * <p>Visibility, subtypes and names all come from FC. Walking the registries instead showed every
     * technical block FC hides with a null tab (burning/lit/crop states, axle power source, legacy siding and
     * moulding blocks, ...) and FC's untabbed items, added each block only at metadata 0 (missing the aesthetic
     * blocks, wool slab colours, ...), and hid nothing from the vanilla Search tab or JEI, which both read this
     * tab's search contents. Hidden blocks and items stay registered (world data, drops, /give); only their
     * creative listing follows 1.5.2.</p>
     */
    @SubscribeEvent
    public static void buildContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != BTW_TAB.getKey()) return;

        // FC-owned Item.itemsList indices, in index order (the 1.5.2 order within a tab)
        java.util.List<Integer> fcOwned = new java.util.ArrayList<>();
        for (int id = 175; id < btw.modern.Item.itemsList.length; id++) {
            if (btw.modern.Item.itemsList[id] != null && isFcOwned(id)) fcOwned.add(id);
        }

        int blockEntries = 0;
        int itemEntries = 0;
        for (btw.modern.CreativeTabs fcTab : btw.modern.CreativeTabs.creativeTabArray) {
            if (fcTab == null || fcTab == btw.modern.CreativeTabs.tabAllSearch
                    || fcTab == btw.modern.CreativeTabs.tabInventory) {
                continue;
            }
            for (int id : fcOwned) {
                btw.modern.Item fcItem = btw.modern.Item.itemsList[id];
                try {
                    if (fcItem.getCreativeTab() != fcTab) continue;
                    java.util.List<Object> subItems = new java.util.ArrayList<>();
                    fcItem.getSubItems(fcItem.itemID, fcTab, subItems);
                    for (Object sub : subItems) {
                        if (!(sub instanceof btw.modern.ItemStack fcSub)) continue;
                        ItemStack mcSub = ItemStackHelper.toMcStack(fcSub);   // the subtype rides the Damage tag
                        if (mcSub.isEmpty()) continue;
                        mcSub.setCount(1);   // creative tab contents must be single stacks (CreativeModeTab.java:370)
                        // listed in the Search tab (and so JEI) exactly as in this tab; hidden entries in neither
                        event.accept(mcSub, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                        if (isFcBlockId(id)) blockEntries++; else itemEntries++;
                    }
                } catch (Throwable t) {
                    if (FC_FAILURES_LOGGED.add(id)) {
                        LOGGER.warn("FC item {} creative-tab listing failed; it is left out of the BTW tab: {}",
                                id, t.toString(), t);
                    }
                }
            }
        }

        LOGGER.info("Added {} block and {} item entries to the BTW creative tab from FC's creative-tab membership"
                + " ({} FC-owned item ids).", blockEntries, itemEntries, fcOwned.size());
    }

    /**
     * True for an FC block id with a block of this mod: a ProxyBlock, or FCMOD-INTEGRATION(1.20.1): fc_redstone: an FC
     * block registered as a 1.20.1 class (BTW's detector rails, FcDetectorRailBlock).
     */
    private static boolean isFcBlockId(int id) {
        return id < 4096 && (ProxyRegistry.getProxy(id) != null || ProxyRegistry.getFcOwnBlock(id) != null);
    }

    /**
     * True for an Item.itemsList index whose item this mod registered for FC: the block item of an FC block
     * (a ProxyBlock id, 175-4095) or an FC standalone item (a ProxyItem / ProxyArmorItem, id 478 and up).
     * Vanilla-id items FC replaces (256-477, the records) keep their 1.20.1 items and tabs.
     */
    private static boolean isFcOwned(int id) {
        if (isFcBlockId(id)) return true;
        return id >= FIRST_FC_ITEM_ID && ProxyRegistry.getModernItem(id) instanceof LegacyProxyItem;
    }
}
