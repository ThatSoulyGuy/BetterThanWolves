package btw.forge;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Persists the per-dimension FC communal ender-chest inventories into the
 * modern level's DimensionDataStorage. Mirrors 1.5.2 SaveHandler's
 * Load/SaveModSpecificDataToNBT (vanilla/server SaveHandler.java:435-479):
 * the same "FCEnderItems"/"FCLPEnderItems" list-of-{Slot,item} format, so the
 * data stays recognizable across the port. FC item stacks are serialized
 * through {@link ForgeNBTCompound} (write-through to the modern tag), the
 * same pattern the entity proxies use for FC NBT.
 */
public class FcWorldSavedData extends SavedData {

    private static final String STORAGE_KEY = "btw_fc_world_data";

    private final WorldBridge bridge;

    private FcWorldSavedData(WorldBridge bridge) {
        this.bridge = bridge;
    }

    /** Loads (or creates) the saved data for the bridge's level and fills its ender inventories. */
    public static void attach(WorldBridge bridge) {
        bridge.getServerLevel().getDataStorage().computeIfAbsent(
                tag -> load(tag, bridge),
                () -> new FcWorldSavedData(bridge),
                STORAGE_KEY);
    }

    private static FcWorldSavedData load(CompoundTag tag, WorldBridge bridge) {
        FcWorldSavedData data = new FcWorldSavedData(bridge);
        readInventory(tag.getList("FCEnderItems", 10), bridge.GetLocalEnderChestInventory());
        readInventory(tag.getList("FCLPEnderItems", 10), bridge.GetLocalLowPowerEnderChestInventory());
        // The antenna-tier-3 GLOBAL ender inventory is server-wide; persist it once, in the
        // overworld's saved data, under a distinct key (matches FC's WorldInfo storage).
        btw.modern.InventoryEnderChest global = globalInventory(bridge);
        if (global != null) {
            readInventory(tag.getList("FCGlobalEnderItems", 10), global);
        }
        // Village data (reputation, mating cooldown, tick counter) — VillageCollection is a
        // 1.5.2 WorldSavedData; round-trip its NBT through ForgeNBTCompound like entity data.
        // Villages themselves also rediscover from doors, but this preserves reputation.
        if (tag.contains("FCVillages", 10)) {
            bridge.villageCollectionObj.readFromNBT(new ForgeNBTCompound(tag.getCompound("FCVillages")));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.put("FCEnderItems", writeInventory(bridge.GetLocalEnderChestInventory()));
        tag.put("FCLPEnderItems", writeInventory(bridge.GetLocalLowPowerEnderChestInventory()));
        btw.modern.InventoryEnderChest global = globalInventory(bridge);
        if (global != null) {
            tag.put("FCGlobalEnderItems", writeInventory(global));
        }
        CompoundTag villageTag = new CompoundTag();
        bridge.villageCollectionObj.writeToNBT(new ForgeNBTCompound(villageTag));
        tag.put("FCVillages", villageTag);
        return tag;
    }

    // The global inventory lives on the overworld WorldInfo; return null off the overworld
    // or before the server has wired its worlds (so it is saved/loaded exactly once).
    private static btw.modern.InventoryEnderChest globalInventory(WorldBridge bridge) {
        if (bridge.provider == null || bridge.provider.dimensionId != 0) return null;
        btw.modern.MinecraftServer server = btw.modern.MinecraftServer.getServer();
        if (server == null || server.worldServers == null || server.worldServers.length == 0
                || server.worldServers[0] == null || server.worldServers[0].worldInfo == null) {
            return null;
        }
        return server.worldServers[0].worldInfo.GetGlobalEnderChestInventory();
    }

    // The inventories mutate FC-side without notifying us; always write on save.
    @Override
    public boolean isDirty() {
        return true;
    }

    private static ListTag writeInventory(btw.modern.InventoryEnderChest inv) {
        ListTag list = new ListTag();
        if (inv == null) return list;
        for (int slot = 0; slot < inv.getSizeInventory(); slot++) {
            btw.modern.ItemStack stack = inv.getStackInSlot(slot);
            if (stack != null) {
                CompoundTag slotTag = new CompoundTag();
                slotTag.putByte("Slot", (byte) slot);
                stack.writeToNBT(new ForgeNBTCompound(slotTag));
                list.add(slotTag);
            }
        }
        return list;
    }

    private static void readInventory(ListTag list, btw.modern.InventoryEnderChest inv) {
        if (inv == null) return;
        for (int i = 0; i < list.size(); i++) {
            CompoundTag slotTag = list.getCompound(i);
            int slot = slotTag.getByte("Slot") & 255;
            if (slot < inv.getSizeInventory()) {
                inv.setInventorySlotContents(slot,
                        btw.modern.ItemStack.loadItemStackFromNBT(new ForgeNBTCompound(slotTag)));
            }
        }
    }
}
