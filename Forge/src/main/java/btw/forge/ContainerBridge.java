package btw.forge;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Bridges FC's container/GUI system to MC 1.20.1's menu system.
 *
 * <p>FC opens container GUIs through two paths:</p>
 * <ol>
 *   <li>{@code FCBetterThanWolves.ServerOpenCustomInterface(player, container, containerID)}
 *       — used by FC blocks like hopper, soulforge, pulley, etc.</li>
 *   <li>{@code EntityPlayer.displayGUIChest(IInventory)} — used for simple
 *       chest-like inventories (ender chest, etc.)</li>
 * </ol>
 *
 * <p>Both paths converge here: the FC container is wrapped in an
 * {@link FCContainerMenu} and opened via
 * {@link NetworkHooks#openScreen(ServerPlayer, MenuProvider, java.util.function.Consumer)}.</p>
 */
public class ContainerBridge {

    private static final Logger LOGGER = LogManager.getLogger("BTW-ContainerBridge");

    private static final java.util.Map<String, String> FC_GUI_TITLES = java.util.Map.of(
            "FCContainerSoulforge", "Soulforge",
            "FCContainerInfernalEnchanter", "Infernal Enchanter",
            "FCContainerBlockDispenser", "Block Dispenser",
            "FCContainerWorkbench", "Crafting");

    private static String invTitle(btw.modern.IInventory inv) {
        return inv.isInvNameLocalized() ? inv.getInvName() : btw.modern.StatCollector.translateToLocal(inv.getInvName());
    }

    public static void openFCContainer(PlayerBridge player,
                                        btw.modern.Container fcContainer,
                                        String title) {
        ServerPlayer serverPlayer = player.getServerPlayer();
        if (serverPlayer == null) {
            LOGGER.warn("Cannot open FC container: player is not a ServerPlayer");
            return;
        }

        String containerType = fcContainer.getClass().getSimpleName();
        String shownTitle = FC_GUI_TITLES.getOrDefault(containerType, title != null ? title : "Container");
        MenuProvider menuProvider = new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal(shownTitle);
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player p) {
                return new FCContainerMenu(containerId, playerInventory, fcContainer, player);
            }
        };

        try {
            LOGGER.debug("Opening MC screen with {} slots", fcContainer.inventorySlots.size());
            NetworkHooks.openScreen(serverPlayer, menuProvider, buf -> {
                buf.writeUtf(containerType);
                writeSlotLayout(buf, fcContainer);
            });
            LOGGER.debug("NetworkHooks.openScreen completed successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to open FC container screen: {}", e.toString(), e);
        }

        try {
            if (!fcContainer.crafters.contains(player)) {
                fcContainer.addCraftingToCrafters(player);
            }
        } catch (Exception e) {
            LOGGER.debug("Could not register crafter: {}", e.getMessage());
        }
    }

    public static void openChestGUI(PlayerBridge player, btw.modern.IInventory inventory) {
        ServerPlayer serverPlayer = player.getServerPlayer();
        if (serverPlayer == null) return;

        btw.modern.Container chestContainer = new SimpleChestContainer(inventory, player);

        player.openContainer = chestContainer;

        String name = invTitle(inventory);
        openFCContainer(player, chestContainer, name);
    }

    /**
     * Writes the FC container's slot layout to a network buffer so the
     * client can reconstruct the menu without the FC container.
     */
    private static void writeSlotLayout(FriendlyByteBuf buf, btw.modern.Container fcContainer) {
        int totalSlots = fcContainer.inventorySlots.size();

        // Count non-player (container) slots
        int containerSlots = 0;
        for (int i = 0; i < totalSlots; i++) {
            btw.modern.Slot fcSlot = fcContainer.inventorySlots.get(i);
            if (!(fcSlot.inventory instanceof btw.modern.InventoryPlayer)) {
                containerSlots++;
            }
        }

        buf.writeVarInt(totalSlots);
        buf.writeVarInt(containerSlots);

        for (int i = 0; i < totalSlots; i++) {
            btw.modern.Slot fcSlot = fcContainer.inventorySlots.get(i);
            buf.writeShort(fcSlot.xDisplayPosition);
            buf.writeShort(fcSlot.yDisplayPosition);

            boolean isPlayer = fcSlot.inventory instanceof btw.modern.InventoryPlayer;
            buf.writeBoolean(isPlayer);

            if (isPlayer) {
                // Write the player inventory slot index
                int slotIndex = getSlotIndex(fcSlot);
                buf.writeVarInt(slotIndex);
            } else {
                // Write the container slot index
                int slotIndex = getSlotIndex(fcSlot);
                buf.writeVarInt(slotIndex);
                // Crafting result (1.5.2 SlotCrafting): the client slot runs FC's client-side onCreated on take
                buf.writeBoolean(fcSlot instanceof btw.modern.SlotCrafting);
            }
        }
    }

    /**
     * Gets the internal slot index from an FC Slot.
     */
    private static int getSlotIndex(btw.modern.Slot fcSlot) {
        return fcSlot.slotIndex;
    }

    @SuppressWarnings("unused")
    private static int getSlotIndex_old(btw.modern.Slot fcSlot) {
        try {
            java.lang.reflect.Field f = btw.modern.Slot.class.getDeclaredField("slotIndex");
            f.setAccessible(true);
            return f.getInt(fcSlot);
        } catch (Exception e) {
            return 0;
        }
    }

    public static boolean checkAndOpenContainer(PlayerBridge player,
                                              btw.modern.Container previousContainer) {
        btw.modern.Container currentContainer = player.openContainer;

        ServerPlayer sp = player.getServerPlayer();
        if (sp != null && sp.containerMenu instanceof FCContainerMenu) {
            return false;
        }

        if (currentContainer != null
                && currentContainer != previousContainer
                && currentContainer != player.inventoryContainer) {

            LOGGER.debug("Container changed! Opening MC menu for {}", currentContainer.getClass().getSimpleName());

            ServerPlayer serverPlayer = player.getServerPlayer();
            if (serverPlayer == null) {
                LOGGER.warn("Cannot open container — serverPlayer is null");
                return false;
            }

            String title = "Container";
            for (int i = 0; i < currentContainer.inventorySlots.size(); i++) {
                btw.modern.Slot slot = currentContainer.inventorySlots.get(i);
                if (!(slot.inventory instanceof btw.modern.InventoryPlayer)) {
                    title = invTitle(slot.inventory);
                    break;
                }
            }

            openFCContainer(player, currentContainer, title);
            return true;
        }
        return false;
    }

    // ================================================================
    // Simple chest container for displayGUIChest
    // ================================================================

    /**
     * A minimal FC Container for displaying a simple chest-like inventory.
     * Maps the IInventory's slots on top, player inventory below.
     */
    // Extends ContainerChest (not bare Container) so the ported
    // TileEntityChest.updateEntity 200-tick viewer recount finds this container
    // via `openContainer instanceof ContainerChest && getLowerChestInventory() ==
    // this` — otherwise the recount zeroes numUsingPlayers and the chest lid
    // snaps shut ~10s into viewing.
    private static class SimpleChestContainer extends btw.modern.ContainerChest {
        private final btw.modern.IInventory chestInventory;
        private final int numRows;

        SimpleChestContainer(btw.modern.IInventory inventory, PlayerBridge player) {
            super(player.inventory, inventory);
            this.chestInventory = inventory;
            int invSize = inventory.getSizeInventory();
            this.numRows = Math.max(1, (invSize + 8) / 9); // ceil(invSize / 9)

            // Add chest slots
            for (int row = 0; row < numRows; row++) {
                for (int col = 0; col < 9; col++) {
                    int slotIndex = col + row * 9;
                    if (slotIndex < invSize) {
                        addSlotToContainer(new btw.modern.Slot(inventory, slotIndex,
                                8 + col * 18, 18 + row * 18));
                    }
                }
            }

            // Add player inventory slots (3 rows of 9)
            int playerInvY = 31 + numRows * 18;
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    addSlotToContainer(new btw.modern.Slot(player.inventory,
                            col + row * 9 + 9,
                            8 + col * 18, playerInvY + row * 18));
                }
            }

            // Add hotbar slots
            int hotbarY = playerInvY + 58;
            for (int col = 0; col < 9; col++) {
                addSlotToContainer(new btw.modern.Slot(player.inventory,
                        col, 8 + col * 18, hotbarY));
            }

            inventory.openChest();
        }

        @Override
        public boolean canInteractWith(btw.modern.EntityPlayer player) {
            return true;
        }

        @Override
        public btw.modern.ItemStack transferStackInSlot(btw.modern.EntityPlayer player, int slotIndex) {
            btw.modern.ItemStack result = null;
            btw.modern.Slot slot = this.inventorySlots.get(slotIndex);

            if (slot != null && slot.getHasStack()) {
                btw.modern.ItemStack slotStack = slot.getStack();
                result = slotStack.copy();

                int chestSlotCount = numRows * 9;
                if (chestSlotCount > chestInventory.getSizeInventory()) {
                    chestSlotCount = chestInventory.getSizeInventory();
                }

                if (slotIndex < chestSlotCount) {
                    // Move from chest to player inventory
                    if (!mergeItemStack(slotStack, chestSlotCount, inventorySlots.size(), true)) {
                        return null;
                    }
                } else {
                    // Move from player inventory to chest
                    if (!mergeItemStack(slotStack, 0, chestSlotCount, false)) {
                        return null;
                    }
                }

                if (slotStack.stackSize == 0) {
                    slot.putStack(null);
                } else {
                    slot.onSlotChanged();
                }
            }
            return result;
        }

        @Override
        public void onContainerClosed(btw.modern.EntityPlayer player) {
            super.onContainerClosed(player);
            chestInventory.closeChest();
        }
    }
}
