package btw.modern;

/**
 * Stores information about a world (time, spawn, etc.).
 * Mirrors net.minecraft.src.WorldInfo.
 */
public class WorldInfo {

    private long worldTime;
    private long totalWorldTime;
    private int spawnX;
    private int spawnY = 64;
    private int spawnZ;
    private boolean raining;
    private boolean thundering;
    private int rainTime;
    private int thunderTime;
    private long seed;
    private String worldName = "";
    private boolean hardcore;
    private boolean commandsAllowed;
    private boolean mapFeatures;
    private EnumGameType gameType = EnumGameType.SURVIVAL;
    private WorldType terrainType;
    private GameRules gameRules;
    private NBTTagCompound playerNBTTagCompound;

    // BTW-specific fields
    private boolean netherBeenAccessed;
    private boolean witherBeenSummoned;
    private boolean endDimensionBeenAccessed;

    public long getWorldTime() { return this.worldTime; }
    public void setWorldTime(long time) { this.worldTime = time; }

    public long getTotalWorldTime() { return this.totalWorldTime; }
    public void setTotalWorldTime(long time) { this.totalWorldTime = time; }

    public int getSpawnX() { return this.spawnX; }
    public int getSpawnY() { return this.spawnY; }
    public int getSpawnZ() { return this.spawnZ; }

    public void setSpawnX(int x) { this.spawnX = x; }
    public void setSpawnY(int y) { this.spawnY = y; }
    public void setSpawnZ(int z) { this.spawnZ = z; }

    public void setSpawnPosition(int x, int y, int z) {
        this.spawnX = x;
        this.spawnY = y;
        this.spawnZ = z;
    }

    public boolean isRaining() { return this.raining; }
    public void setRaining(boolean raining) { this.raining = raining; }

    public boolean isThundering() { return this.thundering; }
    public void setThundering(boolean thundering) { this.thundering = thundering; }

    public int getRainTime() { return this.rainTime; }
    public void setRainTime(int time) { this.rainTime = time; }

    public int getThunderTime() { return this.thunderTime; }
    public void setThunderTime(int time) { this.thunderTime = time; }

    public long getSeed() { return this.seed; }
    public void setSeed(long seed) { this.seed = seed; }

    public String getWorldName() { return this.worldName; }
    public void setWorldName(String name) { this.worldName = name; }

    public WorldType getTerrainType() { return this.terrainType; }
    public void setTerrainType(WorldType type) { this.terrainType = type; }

    public EnumGameType getGameType() { return this.gameType; }
    public void setGameType(EnumGameType type) { this.gameType = type; }

    public boolean isMapFeaturesEnabled() { return this.mapFeatures; }
    public void setMapFeaturesEnabled(boolean enabled) { this.mapFeatures = enabled; }

    public boolean isHardcoreModeEnabled() { return this.hardcore; }
    public void setHardcore(boolean hardcore) { this.hardcore = hardcore; }

    public boolean areCommandsAllowed() { return this.commandsAllowed; }
    public void setCommandsAllowed(boolean allowed) { this.commandsAllowed = allowed; }

    public GameRules getGameRulesInstance() {
        if (this.gameRules == null) {
            this.gameRules = new GameRules();
        }
        return this.gameRules;
    }

    public NBTTagCompound getPlayerNBTTagCompound() { return this.playerNBTTagCompound; }
    public void setPlayerNBTTagCompound(NBTTagCompound compound) { this.playerNBTTagCompound = compound; }

    // 1.5.2 WorldInfo.m_globalEnderChestInventory (BTW-patched vanilla/server
    // WorldInfo.java:665) — FCBlockEnderChest.onBlockActivated antenna level 3
    // opens the global communal ender inventory via
    // MinecraftServer.getServer().worldServers[0].worldInfo. Vanilla persists it
    // in level.dat ("FCEnderItems" via Save/LoadModInfoToNBT); disk persistence
    // is bridged separately in the Forge layer.
    private InventoryEnderChest m_globalEnderChestInventory = new InventoryEnderChest();

    // BTW-added methods
    public InventoryEnderChest GetGlobalEnderChestInventory() { return m_globalEnderChestInventory; }
    public InventoryEnderChest GetGlobalLowPowerEnderChestInventory() { return null; }

    public boolean HasNetherBeenAccessed() { return this.netherBeenAccessed; }
    public void SetNetherBeenAccessed() { this.netherBeenAccessed = true; }

    public boolean HasWitherBeenSummoned() { return this.witherBeenSummoned; }
    public void SetWitherHasBeenSummoned() { this.witherBeenSummoned = true; }

    public boolean HasEndDimensionBeenAccessed() { return this.endDimensionBeenAccessed; }
    public void SetEndDimensionHasBeenAccessed() { this.endDimensionBeenAccessed = true; }
}
