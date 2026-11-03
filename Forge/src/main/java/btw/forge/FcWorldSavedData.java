package btw.forge;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persists the per-dimension FC communal ender-chest inventories into the
 * modern level's DimensionDataStorage. Mirrors 1.5.2 SaveHandler's
 * Load/SaveModSpecificDataToNBT (vanilla/server SaveHandler.java:435-479):
 * the same "FCEnderItems"/"FCLPEnderItems" list-of-{Slot,item} format, so the
 * data stays recognizable across the port. FC item stacks are serialized
 * through {@link ForgeNBTCompound} (write-through to the modern tag), the
 * same pattern the entity proxies use for FC NBT.
 *
 * The overworld's file also carries what 1.5.2 kept on the overworld WorldInfo in
 * level.dat: the antenna-tier-3 global ender inventory and the FC game-progress flags.
 * The overworld also keeps the headless-item-id table ({@link HeadlessItemIds}), loaded before
 * any chunk so FC NBT holding those ids resolves to the same items after a restart.
 *
 * Each dimension's file also carries what 1.5.2 SaveHandler kept per world beside the ender items
 * (vanilla/server SaveHandler.java:426-488): the magnetic points and looting beacons (fc_beacon) and the
 * hardcore spawn list "FCSpawnLocations" (hardcore_respawn; only the overworld's list ever has points).
 */
public class FcWorldSavedData extends SavedData {

    private static final Logger LOGGER = LogManager.getLogger("BTW-FcWorldData");

    private static final String STORAGE_KEY = "btw_fc_world_data";

    /**
     * Failure sites already logged. save() runs on every autosave, so each kind of failure is
     * reported once per JVM (triage RE1: this data is saved inside MinecraftServer.stopServer
     * before session.lock is released -- SavedData.save(File) calls us outside its try -- so
     * nothing here may throw).
     */
    private static final Set<String> LOGGED_FAILURES = ConcurrentHashMap.newKeySet();

    private final WorldBridge bridge;

    private FcWorldSavedData(WorldBridge bridge) {
        this.bridge = bridge;
    }

    /** Loads (or creates) the saved data for the bridge's level and fills its ender inventories. */
    public static void attach(WorldBridge bridge) {
        ServerLevel level = bridge.getServerLevel();
        // Normally already attached by Events.onLevelLoad; this is the fallback if that hook did not run.
        if (level != null && level.dimension().equals(Level.OVERWORLD)) {
            attachHeadlessItemIds(level);
        }
        bridge.getServerLevel().getDataStorage().computeIfAbsent(
                tag -> load(tag, bridge),
                () -> new FcWorldSavedData(bridge),
                STORAGE_KEY);
    }

    /**
     * Loads (or creates) the world's headless-item-id table (triage RE5) from the overworld's data storage,
     * restoring ProxyRegistry's headless items from it. Idempotent: DimensionDataStorage caches the instance,
     * so the table is restored once per world load. Never throws.
     */
    public static void attachHeadlessItemIds(ServerLevel overworld) {
        guarded("load headless item ids", () -> overworld.getDataStorage().computeIfAbsent(
                HeadlessItemIds::load, HeadlessItemIds::new, HeadlessItemIds.STORAGE_KEY));
    }

    /**
     * Per-world table of ProxyRegistry's headless item ids (id -> item registry name), file
     * data/btw_headless_items.dat of the overworld (triage RE5). FC NBT (FC containers, FC ender inventories)
     * stores item stacks by numeric id (Modern-Common ItemStack.writeToNBT) and headless ids (25000+, modern
     * items with no 1.5.2 id) are handed out in first-seen order, so without this every restart deleted or
     * swapped the post-1.5.2 items in FC containers. Restored at the overworld's LevelEvent.Load, which
     * MinecraftServer.createLevels posts before prepareLevels loads any chunk; reset at server stop so the
     * next world in this JVM restores its own numbering.
     */
    static final class HeadlessItemIds extends SavedData {
        static final String STORAGE_KEY = "btw_headless_items";

        HeadlessItemIds() {}

        static HeadlessItemIds load(CompoundTag tag) {
            Map<Integer, String> ids = new HashMap<>();
            ListTag list = tag.getList("Ids", 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                String name = entry.getString("Item");
                if (!name.isEmpty()) ids.put(entry.getInt("Id"), name);
            }
            guarded("restore headless item ids", () -> ProxyRegistry.restoreHeadlessIds(ids));
            return new HeadlessItemIds();
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            guarded("save headless item ids", () -> {
                ListTag list = new ListTag();
                for (Map.Entry<Integer, String> e : ProxyRegistry.snapshotHeadlessIds().entrySet()) {
                    CompoundTag entry = new CompoundTag();
                    entry.putInt("Id", e.getKey());
                    entry.putString("Item", e.getValue());
                    list.add(entry);
                }
                tag.put("Ids", list);
            });
            return tag;
        }

        // New ids are allocated without notifying us; the table is small, always write it.
        @Override
        public boolean isDirty() {
            return true;
        }
    }

    /** Hooks that must run outside any WorldBridge: before chunk loading, and after the server stopped. */
    @Mod.EventBusSubscriber(modid = BTWForgeMod.MOD_ID)
    public static final class Events {
        private Events() {}

        @SubscribeEvent
        public static void onLevelLoad(LevelEvent.Load event) {
            if (event.getLevel() instanceof ServerLevel level && level.dimension().equals(Level.OVERWORLD)) {
                attachHeadlessItemIds(level);
            }
        }

        @SubscribeEvent
        public static void onServerStopped(ServerStoppedEvent event) {
            // after the final save (MinecraftServer.stopServer): drop this world's headless numbering
            guarded("reset headless item ids", ProxyRegistry::resetHeadlessIds);
        }
    }

    private static FcWorldSavedData load(CompoundTag tag, WorldBridge bridge) {
        FcWorldSavedData data = new FcWorldSavedData(bridge);
        guarded("load FCEnderItems", () ->
                readInventory(tag.getList("FCEnderItems", 10), bridge.GetLocalEnderChestInventory()));
        guarded("load FCLPEnderItems", () ->
                readInventory(tag.getList("FCLPEnderItems", 10), bridge.GetLocalLowPowerEnderChestInventory()));
        // 1.5.2 WorldInfo.LoadModInfoFromNBT (vanilla/server WorldInfo.java:679-705): global ender
        // inventory + progress flags, overworld only. Absent flag keys read false, matching the
        // 1.5.2 hasKey guards; the inventory keeps its port-era key "FCGlobalEnderItems".
        guarded("load global ender inventory / progress flags", () -> {
            btw.modern.WorldInfo info = overworldInfo(bridge);
            if (info != null) {
                readInventory(tag.getList("FCGlobalEnderItems", 10), info.GetGlobalEnderChestInventory());
                if (tag.getBoolean("FCNetherAccessed")) info.SetNetherBeenAccessed();
                if (tag.getBoolean("FCWitherSummoned")) info.SetWitherHasBeenSummoned();
                if (tag.getBoolean("FCEndAccessed")) info.SetEndDimensionHasBeenAccessed();
            }
        });
        // Village data (reputation, mating cooldown, tick counter) — VillageCollection is a
        // 1.5.2 WorldSavedData; round-trip its NBT through ForgeNBTCompound like entity data.
        // Villages themselves also rediscover from doors, but this preserves reputation.
        guarded("load FCVillages", () -> {
            if (tag.contains("FCVillages", 10)) {
                bridge.villageCollectionObj.readFromNBT(new ForgeNBTCompound(tag.getCompound("FCVillages")));
            }
        });
        // FCMOD-INTEGRATION(1.20.1): fc_beacon: 1.5.2 SaveHandler kept the magnetic points and looting beacons per world (vanilla/server SaveHandler.java:426-488)
        guarded("load FCMagneticPoints", () -> {
            if (tag.contains("FCMagneticPoints", 9)) {
                bridge.GetMagneticPointList().loadFromNBT(new ForgeNBTCompound(tag).getTagList("FCMagneticPoints"));
            }
        });
        guarded("load FCLootingBeacons", () -> {
            if (tag.contains("FCLootingBeacons", 9)) {
                bridge.GetLootingBeaconLocationList().loadFromNBT(new ForgeNBTCompound(tag).getTagList("FCLootingBeacons"));
            }
        });
        // FCMOD-INTEGRATION(1.20.1): hardcore_respawn: 1.5.2 SaveHandler kept the spawn list per world (vanilla/server SaveHandler.java:456-461, :486-489); IPos/JPos(short)/KPos/SpawnTime per point (FCSpawnLocation.WriteToNBT)
        guarded("load FCSpawnLocations", () -> {
            if (tag.contains("FCSpawnLocations", 9)) {
                bridge.GetSpawnLocationList().loadFromNBT(new ForgeNBTCompound(tag).getTagList("FCSpawnLocations"));
            }
        });
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        guarded("save FCEnderItems", () ->
                tag.put("FCEnderItems", writeInventory(bridge.GetLocalEnderChestInventory())));
        guarded("save FCLPEnderItems", () ->
                tag.put("FCLPEnderItems", writeInventory(bridge.GetLocalLowPowerEnderChestInventory())));
        // 1.5.2 WorldInfo.SaveModInfoToNBT (vanilla/server WorldInfo.java:707-717), overworld only.
        guarded("save global ender inventory / progress flags", () -> {
            btw.modern.WorldInfo info = overworldInfo(bridge);
            if (info != null) {
                tag.put("FCGlobalEnderItems", writeInventory(info.GetGlobalEnderChestInventory()));
                tag.putBoolean("FCNetherAccessed", info.HasNetherBeenAccessed());
                tag.putBoolean("FCWitherSummoned", info.HasWitherBeenSummoned());
                tag.putBoolean("FCEndAccessed", info.HasEndDimensionBeenAccessed());
            }
        });
        guarded("save FCVillages", () -> {
            CompoundTag villageTag = new CompoundTag();
            bridge.villageCollectionObj.writeToNBT(new ForgeNBTCompound(villageTag));
            tag.put("FCVillages", villageTag);
        });
        // FCMOD-INTEGRATION(1.20.1): fc_beacon: the magnetic points and looting beacons (1.5.2 SaveHandler.java:426-488)
        guarded("save FCMagneticPoints", () ->
                new ForgeNBTCompound(tag).setTag("FCMagneticPoints", bridge.GetMagneticPointList().saveToNBT()));
        guarded("save FCLootingBeacons", () ->
                new ForgeNBTCompound(tag).setTag("FCLootingBeacons", bridge.GetLootingBeaconLocationList().saveToNBT()));
        // FCMOD-INTEGRATION(1.20.1): hardcore_respawn: the spawn list (1.5.2 SaveHandler.java:456-461, :486-489)
        guarded("save FCSpawnLocations", () ->
                new ForgeNBTCompound(tag).setTag("FCSpawnLocations", bridge.GetSpawnLocationList().saveToNBT()));
        return tag;
    }

    // 1.5.2 keeps the antenna-tier-3 GLOBAL ender inventory and FC game-progress flags on the
    // OVERWORLD WorldInfo, saved in level.dat (vanilla/server WorldInfo.java:665-672,
    // LoadModInfoFromNBT :679-705, SaveModInfoToNBT :707-717). worldServers[0] IS the overworld
    // WorldBridge (BTWLifecycle.wireWorlds), so use this bridge's WorldInfo directly -- NOT
    // MinecraftServer.getServer(): the overworld bridge is routinely created during
    // prepareLevels (spawn-chunk block entities, FC mobs deserialized near spawn), before
    // ServerStartingEvent wires the server, and a re-entered world used to read the previous
    // session's server here (triage RE3). Keyed on the dimension, not provider.dimensionId,
    // because modded dimensions also report dimensionId 0.
    private static btw.modern.WorldInfo overworldInfo(WorldBridge bridge) {
        ServerLevel level = bridge.getServerLevel();
        return level != null && level.dimension().equals(Level.OVERWORLD) ? bridge.worldInfo : null;
    }

    // The inventories mutate FC-side without notifying us; always write on save.
    @Override
    public boolean isDirty() {
        return true;
    }

    private static void guarded(String what, Runnable action) {
        try {
            action.run();
        } catch (Throwable t) {
            logOnce(what, t);
        }
    }

    private static void logOnce(String what, Throwable t) {
        if (LOGGED_FAILURES.add(what)) {
            LOGGER.error("FC world data: {} failed; continuing without it (logged once)", what, t);
        }
    }

    private static ListTag writeInventory(btw.modern.InventoryEnderChest inv) {
        ListTag list = new ListTag();
        if (inv == null) return list;
        for (int slot = 0; slot < inv.getSizeInventory(); slot++) {
            try {
                btw.modern.ItemStack stack = inv.getStackInSlot(slot);
                if (stack != null) {
                    CompoundTag slotTag = new CompoundTag();
                    slotTag.putByte("Slot", (byte) slot);
                    stack.writeToNBT(new ForgeNBTCompound(slotTag));
                    list.add(slotTag);
                }
            } catch (Throwable t) {
                // One bad stack must not cost the rest of the inventory.
                logOnce("write ender inventory slot", t);
            }
        }
        return list;
    }

    private static void readInventory(ListTag list, btw.modern.InventoryEnderChest inv) {
        if (inv == null) return;
        for (int i = 0; i < list.size(); i++) {
            try {
                CompoundTag slotTag = list.getCompound(i);
                int slot = slotTag.getByte("Slot") & 255;
                if (slot < inv.getSizeInventory()) {
                    inv.setInventorySlotContents(slot,
                            btw.modern.ItemStack.loadItemStackFromNBT(new ForgeNBTCompound(slotTag)));
                }
            } catch (Throwable t) {
                logOnce("read ender inventory slot", t);
            }
        }
    }
}
