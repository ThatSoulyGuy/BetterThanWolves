package btw.forge;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
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
 * The client predicts via vanilla logic for instant feedback; the server (FC)
 * is authoritative and {@code broadcastChanges()} corrects any mismatches.</p>
 *
 * <h3>Data sync</h3>
 * <p>FC containers sync UI state (cook progress, enchantment levels, etc.)
 * via the {@link btw.modern.ICrafting} interface. The PlayerBridge implements
 * ICrafting and writes received values into this menu's {@link #fcData} array,
 * which is backed by MC {@link DataSlot}s — MC automatically detects changes
 * and pushes them to the client.</p>
 *
 * <p>For FC containers that compute state client-side (e.g., enchantment
 * levels), {@link #pushFcComputedData()} reads those fields via reflection
 * and pushes them as additional data slots.</p>
 */
public class FCContainerMenu extends AbstractContainerMenu {

    private static final Logger LOGGER = LogManager.getLogger("BTW-FCContainerMenu");

    /** Number of data slots reserved for FC progress bars + computed data. */
    private static final int FC_DATA_SLOT_COUNT = 16;

    private final btw.modern.Container fcContainer;
    private final PlayerBridge fcPlayer;

    /**
     * FC container data array. Slots 0+ are used by FC's progress bar system
     * (sendProgressBarUpdate) and by pushFcComputedData() for container-specific
     * computed fields like enchantment levels.
     *
     * <p>Backed by MC DataSlots — changes are auto-synced to the client.</p>
     */
    private final int[] fcData = new int[FC_DATA_SLOT_COUNT];

    /** FC container type name (e.g., "FCContainerSoulforge") for texture selection. */
    private String containerType = "";

    public String getContainerType() { return containerType; }

    /** Read a synced FC data value (available on both server and client). */
    public int getFcData(int id) {
        return id >= 0 && id < fcData.length ? fcData[id] : 0;
    }

    /** Write an FC data value (called by PlayerBridge.sendProgressBarUpdate). */
    public void setFcData(int id, int value) {
        if (id >= 0 && id < fcData.length) {
            fcData[id] = value;
        }
    }

    /**
     * Server-side constructor: wraps an existing FC container.
     */
    public FCContainerMenu(int containerId, Inventory playerInv,
                           btw.modern.Container fcContainer, PlayerBridge fcPlayer) {
        super(BTWMenuTypes.FC_CONTAINER.get(), containerId);
        this.fcContainer = fcContainer;
        this.fcPlayer = fcPlayer;
        mirrorFcSlots();
        addFcDataSlots();
    }

    /**
     * Client-side constructor: called by the IContainerFactory when the
     * server opens a menu via NetworkHooks.openScreen.
     */
    public FCContainerMenu(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        super(BTWMenuTypes.FC_CONTAINER.get(), containerId);
        this.fcContainer = null;
        this.fcPlayer = null;
        this.containerType = buf.readUtf();

        int slotCount = buf.readVarInt();
        int containerSlots = buf.readVarInt();

        // Use sequential indices for ALL container slots to avoid collisions.
        // The result slot (slotIndex=0 in craftResult) and first grid slot
        // (slotIndex=0 in craftMatrix) must NOT share the same index in the
        // same SimpleContainer — that causes them to mirror each other.
        net.minecraft.world.SimpleContainer dummyContainer =
                new net.minecraft.world.SimpleContainer(Math.max(slotCount, containerSlots + 1));
        int nextContainerSlot = 0;

        for (int i = 0; i < slotCount; i++) {
            int x = buf.readShort();
            int y = buf.readShort();
            boolean isPlayerSlot = buf.readBoolean();
            int origSlotIndex = buf.readVarInt();

            if (isPlayerSlot) {
                this.addSlot(new Slot(playerInv, origSlotIndex, x, y));
            } else {
                // Assign sequential index to avoid collisions between
                // different FC inventories (result vs grid vs others)
                this.addSlot(new Slot(dummyContainer, nextContainerSlot++, x, y));
            }
        }

        addFcDataSlots();
    }

    /** Registers MC DataSlots backed by the fcData array. */
    private void addFcDataSlots() {
        for (int i = 0; i < FC_DATA_SLOT_COUNT; i++) {
            final int idx = i;
            addDataSlot(new DataSlot() {
                @Override public int get() { return fcData[idx]; }
                @Override public void set(int value) { fcData[idx] = value; }
            });
        }
    }

    /**
     * Reads the FC container's slot list and creates MC Slot wrappers.
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
                if (i < 3) {
                    LOGGER.info("mirrorFcSlots: slot {} fcInv={} adapter={} slotIndex={}",
                            i, System.identityHashCode(fcInv), System.identityHashCode(adapter), fcSlot.slotIndex);
                }
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
        // Use MC's native moveItemStackTo instead of FC's mergeItemStack.
        // FC's version operates on FC slots disconnected from MC's InventoryAdapter.
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack slotStack = slot.getItem();
        ItemStack originalStack = slotStack.copy();

        // Determine slot ranges: container slots vs player inventory slots
        int containerSlotCount = fcContainer != null ? 0 : 0;
        for (int i = 0; i < this.slots.size(); i++) {
            if (this.slots.get(i) instanceof ContainerMappedSlot) {
                containerSlotCount++;
            }
        }
        // Container slots are first, player slots follow
        int playerStart = containerSlotCount;
        int playerEnd = this.slots.size();

        if (index < containerSlotCount) {
            // Shift-click from container → player inventory
            if (!this.moveItemStackTo(slotStack, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Shift-click from player → container
            if (!this.moveItemStackTo(slotStack, 0, containerSlotCount, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (slotStack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (slotStack.getCount() == originalStack.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, slotStack);
        return originalStack;
    }

    /**
     * Slot clicks are handled by MC 1.20.1's native AbstractContainerMenu logic.
     *
     * <p>All MC slots are backed by {@link InventoryAdapter} (live proxy to FC
     * inventory) or the real player {@link net.minecraft.world.entity.player.Inventory}.
     * MC's native click/drag/shift-click logic operates directly on these proxies,
     * which read/write to FC's inventories. No FC Container.slotClick reimplementation
     * needed — MC's own implementation handles all interaction modes correctly.</p>
     *
     * <p>FC-specific slot behavior (validation, stack limits) is bridged via
     * {@link ContainerMappedSlot#mayPlace} and {@link ContainerMappedSlot#getMaxStackSize}.</p>
     */
    @Override
    public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType type, Player player) {
        // Let MC 1.20.1's native AbstractContainerMenu handle ALL slot interactions.
        // The InventoryAdapter proxy ensures MC reads/writes directly to FC's inventories.
        super.clicked(slotId, button, type, player);
    }

    /**
     * Forwards MC's menu button clicks to FC's {@code enchantItem()}.
     * MC sends this when the client clicks an enchantment button or
     * similar UI element (via ServerboundContainerButtonClickPacket).
     */
    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (fcContainer != null && fcPlayer != null) {
            try {
                fcPlayer.syncFromReal();
                boolean result = fcContainer.enchantItem(fcPlayer, buttonId);
                // Flush inventory changes (XP cost, scroll consumed, etc.)
                ((InventoryBridge) fcPlayer.inventory).writeBackAll();
                return result;
            } catch (Exception e) {
                LOGGER.debug("FC enchantItem failed for button {}: {}", buttonId, e.getMessage());
            }
        }
        return false;
    }

    /**
     * Runs FC's sync loop and pushes computed data to MC DataSlots.
     *
     * <p>Called every server tick while the container is open. FC's
     * {@code detectAndSendChanges()} iterates registered crafters and
     * calls {@code sendProgressBarUpdate()} which writes into {@link #fcData}.
     * MC then detects the DataSlot changes and sends them to the client.</p>
     */
    @Override
    public void broadcastChanges() {
        if (fcContainer != null && fcPlayer != null) {
            try {
                // Run FC's sync loop — triggers sendProgressBarUpdate on crafters
                fcContainer.detectAndSendChanges();

                // Push FC-computed data that FC normally computes client-side
                // (e.g., enchantment levels). We send these explicitly because
                // the client doesn't have the FC container.
                pushFcComputedData();
            } catch (Exception e) {
                LOGGER.debug("FC detectAndSendChanges failed: {}", e.getMessage());
            }
        }
        // MC's broadcastChanges syncs slots + DataSlots to client
        super.broadcastChanges();
    }

    /**
     * Reads FC container fields via reflection and pushes them into
     * the fcData array. MC DataSlots detect changes and sync to client.
     *
     * <p>FC containers like the Infernal Enchanter compute enchantment
     * levels on the client from slot contents. Since the Forge client
     * doesn't have the FC container, we send these values explicitly.</p>
     *
     * <p>Layout:</p>
     * <ul>
     *   <li>0: reserved for FC's native progress bar (e.g., bookshelf level)</li>
     *   <li>1-5: enchantment levels (m_CurrentEnchantmentLevels[0..4])</li>
     *   <li>6-7: enchantment name seed (m_lNameSeed split into two ints)</li>
     * </ul>
     */
    private void pushFcComputedData() {
        // Only check enchanter-specific fields on enchanter containers
        if (!fcContainer.getClass().getSimpleName().contains("InfernalEnchanter")) return;

        // Enchanter: m_CurrentEnchantmentLevels[] and m_lNameSeed
        try {
            java.lang.reflect.Field levelsField = findField(fcContainer.getClass(), "m_CurrentEnchantmentLevels");
            if (levelsField != null) {
                int[] levels = (int[]) levelsField.get(fcContainer);
                boolean anyNonZero = false;
                for (int i = 0; i < Math.min(levels.length, 5); i++) {
                    fcData[i + 1] = levels[i];
                    if (levels[i] != 0) anyNonZero = true;
                }
                if (anyNonZero) {
                    LOGGER.info("Enchanter levels: [{},{},{},{},{}]",
                            levels[0], levels[1], levels[2], levels[3], levels[4]);
                }
            } else {
                LOGGER.warn("m_CurrentEnchantmentLevels field not found on {}",
                        fcContainer.getClass().getName());
            }

            java.lang.reflect.Field seedField = findField(fcContainer.getClass(), "m_lNameSeed");
            if (seedField != null) {
                long seed = seedField.getLong(fcContainer);
                fcData[6] = (int) (seed & 0xFFFFFFFFL);
                fcData[7] = (int) ((seed >>> 32) & 0xFFFFFFFFL);
            }

            // Log slot contents for diagnostics
            java.lang.reflect.Field tableField = findField(fcContainer.getClass(), "m_tableInventory");
            if (tableField != null) {
                btw.modern.IInventory table = (btw.modern.IInventory) tableField.get(fcContainer);
                if (table != null) {
                    btw.modern.ItemStack s0 = table.getStackInSlot(0);
                    btw.modern.ItemStack s1 = table.getStackInSlot(1);
                    if (s0 != null || s1 != null) {
                        LOGGER.info("Enchanter slots: [0]={} [1]={}",
                                s0 != null ? "id=" + s0.itemID + " dmg=" + s0.getItemDamage() : "empty",
                                s1 != null ? "id=" + s1.itemID + " dmg=" + s1.getItemDamage() : "empty");
                    }
                }
            }

            // Log bookshelf level for diagnostics
            java.lang.reflect.Field bsField = findField(fcContainer.getClass(), "m_iMaxSurroundingBookshelfLevel");
            if (bsField != null) {
                int bsLevel = bsField.getInt(fcContainer);
                if (bsLevel != fcData[0]) {
                    LOGGER.info("Enchanter bookshelf level: {}", bsLevel);
                    fcData[0] = bsLevel; // Also push via data slot (in case crafter registration didn't fire)
                }
            }
        } catch (Exception e) {
            LOGGER.debug("pushFcComputedData failed: {}", e.getMessage());
        }
    }

    /** Finds a field by name, searching the class hierarchy. Sets accessible. */
    private static java.lang.reflect.Field findField(Class<?> clazz, String name) {
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            try {
                java.lang.reflect.Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {}
        }
        return null;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (fcContainer != null && fcPlayer != null) {
            try {
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

    private static class PlayerMappedSlot extends Slot {
        private final btw.modern.Slot fcSlot;

        PlayerMappedSlot(Inventory inventory, btw.modern.Slot fcSlot) {
            super(inventory, fcSlot.slotIndex, fcSlot.xDisplayPosition, fcSlot.yDisplayPosition);
            this.fcSlot = fcSlot;
        }
    }

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

        @Override
        public boolean mayPickup(Player player) {
            PlayerBridge pb = player instanceof net.minecraft.server.level.ServerPlayer sp
                    ? PlayerBridge.getOrCreate(sp) : null;
            return fcSlot.canTakeStack(pb);
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            // Bridge FC's onPickupFromSlot — critical for crafting
            // (consumes ingredients when result is taken)
            PlayerBridge pb = player instanceof net.minecraft.server.level.ServerPlayer sp
                    ? PlayerBridge.getOrCreate(sp) : null;
            btw.modern.ItemStack fcStack = ItemStackHelper.toFcStack(stack);
            fcSlot.onPickupFromSlot(pb, fcStack);
            super.onTake(player, stack);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            fcSlot.onSlotChanged();
        }
    }
}
