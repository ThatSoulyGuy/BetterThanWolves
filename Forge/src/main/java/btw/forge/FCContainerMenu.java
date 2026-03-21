package btw.forge;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * MC 1.20.1 {@link AbstractContainerMenu} that wraps an FC
 * {@link btw.modern.Container}.
 *
 * <h3>Architecture: FC is sole authority</h3>
 * <p>ALL slot interaction logic runs through FC's {@code Container.slotClick()}.
 * The client does NOT predict clicks — it sends the click to the server, FC
 * handles it, and MC's {@code broadcastChanges()} pushes the resulting state
 * back to the client. This eliminates all prediction mismatches between MC
 * and FC click logic.</p>
 *
 * <h3>Slot bridging</h3>
 * <p>Container (non-player) slots are backed by {@link InventoryAdapter},
 * a live view of the FC inventory — reads go directly to FC, no copying.
 * Player inventory slots are backed by the real MC {@link Inventory}; after
 * each FC click, {@link InventoryBridge#writeBackAll()} flushes FC's
 * snapshot back to the real MC inventory.</p>
 */
public class FCContainerMenu extends AbstractContainerMenu {

    private static final Logger LOGGER = LogManager.getLogger("BTW-FCContainerMenu");

    private final btw.modern.Container fcContainer;
    private final PlayerBridge fcPlayer;

    /** FC container type name (e.g., "FCContainerSoulforge") for texture selection. */
    private String containerType = "";

    public String getContainerType() { return containerType; }

    /**
     * Server-side constructor: wraps an existing FC container.
     */
    public FCContainerMenu(int containerId, Inventory playerInv,
                           btw.modern.Container fcContainer, PlayerBridge fcPlayer) {
        super(BTWMenuTypes.FC_CONTAINER.get(), containerId);
        this.fcContainer = fcContainer;
        this.fcPlayer = fcPlayer;
        mirrorFcSlots();
    }

    /**
     * Client-side constructor: called by the IContainerFactory when the
     * server opens a menu via NetworkHooks.openScreen.
     *
     * <p>On the client we do not have the real FC container. We create
     * a dummy container with enough slots to match the server's layout.
     * The slot count is transmitted in the network buffer.</p>
     */
    public FCContainerMenu(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        super(BTWMenuTypes.FC_CONTAINER.get(), containerId);
        this.fcContainer = null;
        this.fcPlayer = null;
        this.containerType = buf.readUtf();

        int slotCount = buf.readVarInt();
        int containerSlots = buf.readVarInt();

        net.minecraft.world.SimpleContainer dummyContainer =
                new net.minecraft.world.SimpleContainer(containerSlots > 0 ? containerSlots : 1);

        for (int i = 0; i < slotCount; i++) {
            int x = buf.readShort();
            int y = buf.readShort();
            boolean isPlayerSlot = buf.readBoolean();

            if (isPlayerSlot) {
                int playerSlotIndex = buf.readVarInt();
                this.addSlot(new Slot(playerInv, playerSlotIndex, x, y));
            } else {
                int containerSlotIndex = buf.readVarInt();
                int safeIndex = Math.min(containerSlotIndex, dummyContainer.getContainerSize() - 1);
                this.addSlot(new Slot(dummyContainer, safeIndex, x, y));
            }
        }
    }

    /**
     * Reads the FC container's slot list and creates MC Slot wrappers.
     * Container slots get an {@link InventoryAdapter} (live view of FC inventory).
     * Player slots get the real MC {@link Inventory}.
     */
    private void mirrorFcSlots() {
        java.util.Map<btw.modern.IInventory, InventoryAdapter> adapters = new java.util.IdentityHashMap<>();

        for (int i = 0; i < fcContainer.inventorySlots.size(); i++) {
            btw.modern.Slot fcSlot = fcContainer.inventorySlots.get(i);
            btw.modern.IInventory fcInv = fcSlot.inventory;

            if (fcInv instanceof btw.modern.InventoryPlayer) {
                Inventory realInv = fcPlayer.getRealPlayer().getInventory();
                this.addSlot(new PlayerMappedSlot(realInv, fcSlot));
            } else {
                InventoryAdapter adapter = adapters.computeIfAbsent(fcInv, InventoryAdapter::new);
                this.addSlot(new ContainerMappedSlot(adapter, fcSlot));
            }
        }
    }

    // ================================================================
    // AbstractContainerMenu overrides
    // ================================================================

    @Override
    public boolean stillValid(Player player) {
        if (fcContainer != null && fcPlayer != null) {
            try {
                return fcContainer.canInteractWith(fcPlayer);
            } catch (Exception e) {
                return true;
            }
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Shift-click is handled by FC's Container.slotClick mode 1,
        // which calls transferStackInSlot internally. This method exists
        // as a fallback but should not be reached in normal flow.
        if (fcContainer != null && fcPlayer != null) {
            try {
                btw.modern.ItemStack fcResult = fcContainer.transferStackInSlot(fcPlayer, index);
                return ItemStackHelper.toMcStack(fcResult);
            } catch (Exception e) {
                LOGGER.debug("FC transferStackInSlot failed for slot {}: {}", index, e.getMessage());
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * All slot clicks are delegated to FC's {@code Container.slotClick()}.
     *
     * <p><b>Server:</b> syncs MC cursor → FC, calls FC slotClick, reads back
     * all state from FC, flushes player inventory to MC. MC's
     * {@code broadcastChanges()} then pushes the resulting state to the client.</p>
     *
     * <p><b>Client:</b> does nothing (no prediction). The server pushes the
     * correct state. Since the client "predicted" no change, every real change
     * is detected as a diff and sent as a correction.</p>
     */
    @Override
    public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType type, Player player) {
        if (fcContainer == null || fcPlayer == null) {
            // CLIENT: no prediction. Server will push correct state.
            return;
        }

        // SERVER: FC is the sole authority for all click logic.
        try {
            fcPlayer.syncFromReal();

            // Sync MC cursor → FC cursor
            fcPlayer.inventory.setItemStack(
                    ItemStackHelper.toFcStack(getCarried()));

            // Delegate entirely to FC
            fcContainer.slotClick(slotId, button, type.ordinal(), fcPlayer);

            // Read FC cursor → MC cursor
            setCarried(ItemStackHelper.toMcStack(
                    fcPlayer.inventory.getItemStack()));

            // Flush player inventory: FC may have mutated ItemStack objects
            // in mainInventory[] directly (e.g., stackSize changes during
            // merge/split), which bypasses InventoryBridge's write-through.
            ((InventoryBridge) fcPlayer.inventory).writeBackAll();

        } catch (Exception e) {
            LOGGER.warn("FC slotClick failed (slot={}, button={}, mode={}): {}",
                    slotId, button, type, e.toString(), e);
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (fcContainer != null && fcPlayer != null) {
            try {
                // FC containers override onCraftGuiClosed (NOT onContainerClosed)
                // to drop temporary items back to the player on close.
                fcContainer.onCraftGuiClosed(fcPlayer);
            } catch (Exception e) {
                LOGGER.debug("FC onCraftGuiClosed failed: {}", e.getMessage());
            }
        }
    }

    /**
     * Returns the wrapped FC container, or null on the client side.
     */
    public btw.modern.Container getFcContainer() {
        return fcContainer;
    }

    // ================================================================
    // Custom Slot implementations
    // ================================================================

    /**
     * A slot backed by the real MC player {@link Inventory}, positioned
     * according to the FC slot's x/y coordinates.
     */
    private static class PlayerMappedSlot extends Slot {
        private final btw.modern.Slot fcSlot;

        PlayerMappedSlot(Inventory inventory, btw.modern.Slot fcSlot) {
            super(inventory, fcSlot.slotIndex, fcSlot.xDisplayPosition, fcSlot.yDisplayPosition);
            this.fcSlot = fcSlot;
        }
    }

    /**
     * A slot backed by an {@link InventoryAdapter} (live view of FC inventory),
     * positioned according to the FC slot's x/y coordinates.
     */
    private static class ContainerMappedSlot extends Slot {
        private final btw.modern.Slot fcSlot;

        ContainerMappedSlot(InventoryAdapter adapter, btw.modern.Slot fcSlot) {
            super(adapter, fcSlot.slotIndex, fcSlot.xDisplayPosition, fcSlot.yDisplayPosition);
            this.fcSlot = fcSlot;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            btw.modern.ItemStack fcStack = ItemStackHelper.toFcStack(stack);
            return fcSlot.isItemValid(fcStack);
        }

        @Override
        public int getMaxStackSize() {
            return fcSlot.getSlotStackLimit();
        }
    }
}
