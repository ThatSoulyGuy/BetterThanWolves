package btw.modern;

/**
 * Stores information about a world (time, spawn, etc.).
 * Mirrors net.minecraft.src.WorldInfo.
 */
public class WorldInfo {

    public long getWorldTime() { return 0; }
    public void setWorldTime(long time) {}

    public long getTotalWorldTime() { return 0; }

    public int getSpawnX() { return 0; }
    public int getSpawnY() { return 64; }
    public int getSpawnZ() { return 0; }

    public void setSpawnX(int x) {}
    public void setSpawnY(int y) {}
    public void setSpawnZ(int z) {}
    public void setSpawnPosition(int x, int y, int z) {}

    public boolean isRaining() { return false; }
    public void setRaining(boolean raining) {}

    public boolean isThundering() { return false; }
    public void setThundering(boolean thundering) {}

    public int getRainTime() { return 0; }
    public void setRainTime(int time) {}

    public int getThunderTime() { return 0; }
    public void setThunderTime(int time) {}

    public long getSeed() { return 0; }

    public String getWorldName() { return ""; }

    public WorldType getTerrainType() { return null; }

    public EnumGameType getGameType() { return EnumGameType.SURVIVAL; }

    public boolean isMapFeaturesEnabled() { return false; }

    public boolean isHardcoreModeEnabled() { return false; }

    public boolean areCommandsAllowed() { return false; }

    public GameRules getGameRulesInstance() { return null; }

    public NBTTagCompound getPlayerNBTTagCompound() { return null; }

    // BTW-added methods
    public InventoryEnderChest GetGlobalEnderChestInventory() { return null; }
    public InventoryEnderChest GetGlobalLowPowerEnderChestInventory() { return null; }
    public boolean HasNetherBeenAccessed() { return false; }
    public void SetNetherBeenAccessed() {}
    public boolean HasWitherBeenSummoned() { return false; }
    public void SetWitherHasBeenSummoned() {}
    public boolean HasEndDimensionBeenAccessed() { return false; }
    public void SetEndDimensionHasBeenAccessed() {}
}
