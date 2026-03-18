package btw.api;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

/**
 * Abstract representation of a game world.
 * Mirrors net.minecraft.src.World with identical field/method names.
 */
public abstract class World implements IBlockAccess {

    // --- Instance fields ---

    public boolean scheduledUpdatesAreImmediate = false;
    public List loadedEntityList = new ArrayList();
    public List unloadedEntityList = new ArrayList();
    public List loadedTileEntityList = new ArrayList();
    public List playerEntities = new ArrayList();
    public List weatherEffects = new ArrayList();
    public int skylightSubtracted = 0;
    public int updateLCG = (new Random()).nextInt();
    public final int DIST_HASH_MAGIC = 1013904223;
    public float prevRainingStrength;
    public float rainingStrength;
    public float prevThunderingStrength;
    public float thunderingStrength;
    public int lastLightningBolt = 0;
    public int difficultySetting;

    /** RNG for World. */
    public Random rand = new Random();

    /** The WorldProvider instance that World uses. */
    public WorldProvider provider;

    public List worldAccesses = new ArrayList();
    public Object chunkProvider;
    public Object saveHandler;
    public WorldInfo worldInfo;
    public boolean findingSpawnPoint;
    public Object mapStorage;
    public Object theProfiler;
    public Object worldScoreboard;
    public boolean spawnHostileMobs = true;
    public boolean spawnPeacefulMobs = true;

    /** This is set to true for client worlds, and false for server worlds. */
    public boolean isRemote;

    // --- Abstract methods (must be implemented by backend) ---

    public abstract Object createChunkProvider();

    // --- IBlockAccess implementation / Block query methods ---

    public abstract int getBlockId(int x, int y, int z);

    public abstract int getBlockMetadata(int x, int y, int z);

    public abstract Material getBlockMaterial(int x, int y, int z);

    public abstract boolean isAirBlock(int x, int y, int z);

    public abstract TileEntity getBlockTileEntity(int x, int y, int z);

    public boolean isBlockNormalCube(int x, int y, int z) {
        return Block.isNormalCube(getBlockId(x, y, z));
    }

    // --- Block modification methods ---

    public abstract boolean setBlock(int x, int y, int z, int blockID, int metadata, int flags);

    public boolean setBlock(int x, int y, int z, int blockID) {
        return this.setBlock(x, y, z, blockID, 0, 3);
    }

    public abstract boolean setBlockMetadata(int x, int y, int z, int metadata, int flags);

    public abstract boolean setBlockToAir(int x, int y, int z);

    public abstract boolean destroyBlock(int x, int y, int z, boolean dropItems);

    // --- BTW convenience methods for block modification ---

    public boolean setBlockAndMetadataWithNotify(int i, int j, int k, int iBlockID, int iMetadata) {
        return this.setBlock(i, j, k, iBlockID, iMetadata, 3);
    }

    public boolean setBlockWithNotify(int i, int j, int k, int iBlockID) {
        return this.setBlock(i, j, k, iBlockID, 0, 3);
    }

    public boolean SetBlockMetadataWithNotify(int i, int j, int k, int iMetadata, int iNotifyBitField) {
        return this.setBlockMetadata(i, j, k, iMetadata, iNotifyBitField);
    }

    public boolean setBlockMetadata(int i, int j, int k, int iMetadata) {
        return this.setBlockMetadata(i, j, k, iMetadata, 0);
    }

    public boolean setBlockMetadataWithNotify(int i, int j, int k, int iMetadata) {
        return this.setBlockMetadata(i, j, k, iMetadata, 3);
    }

    public boolean setBlockMetadataWithClient(int i, int j, int k, int iMetadata) {
        return this.setBlockMetadata(i, j, k, iMetadata, 2);
    }

    public boolean setBlockMetadataWithNotifyNoClient(int i, int j, int k, int iMetadata) {
        return this.setBlockMetadata(i, j, k, iMetadata, 1);
    }

    public boolean setBlockAndMetadata(int i, int j, int k, int iBlockID, int iMetadata) {
        return this.setBlock(i, j, k, iBlockID, iMetadata, 0);
    }

    // --- Notification methods ---

    public abstract void notifyBlockChange(int x, int y, int z, int blockID);

    public void notifyBlocksOfNeighborChange(int x, int y, int z, int blockID) {}

    public void notifyBlocksOfNeighborChange(int x, int y, int z, int blockID, int excludedSide) {}

    public void notifyBlockOfNeighborChange(int x, int y, int z, int blockID) {}

    public void markBlockForUpdate(int x, int y, int z) {}

    public void markBlocksDirtyVertical(int x1, int z1, int y1, int y2) {}

    public abstract void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2);

    public void markBlockForRenderUpdate(int x, int y, int z) {}

    // --- Scheduled updates ---

    public void scheduleBlockUpdate(int x, int y, int z, int blockID, int delay) {}

    public void func_82740_a(int x, int y, int z, int blockID, int delay, int priority) {}

    public boolean isBlockTickScheduled(int x, int y, int z, int blockID) {
        return false;
    }

    public boolean IsUpdateScheduledForBlock(int i, int j, int k, int iBlockID) {
        return false;
    }

    public boolean IsUpdatePendingThisTickForBlock(int i, int j, int k, int iBlockID) {
        return false;
    }

    // --- Light methods ---

    public abstract boolean canBlockSeeTheSky(int x, int y, int z);

    public abstract int getFullBlockLightValue(int x, int y, int z);

    public int getBlockLightValue(int x, int y, int z) {
        return 0;
    }

    public int getBlockLightValue_do(int x, int y, int z, boolean useNeighborBrightness) {
        return 0;
    }

    public abstract int getSavedLightValue(EnumSkyBlock enumSkyBlock, int x, int y, int z);

    public void setLightValue(EnumSkyBlock enumSkyBlock, int x, int y, int z, int lightValue) {}

    public float getLightBrightness(int x, int y, int z) {
        return 0;
    }

    public int getLightBrightnessForSkyBlocks(int x, int y, int z, int lightValue) {
        return 0;
    }

    public int GetBlockNaturalLightValue(int i, int j, int k) {
        return 0;
    }

    public int GetBlockNaturalLightValueMaximum(int i, int j, int k) {
        return 0;
    }

    public float GetNaturalLightBrightness(int i, int j, int k) {
        return 0;
    }

    // --- Height methods ---

    public int getHeightValue(int x, int z) {
        return 0;
    }

    public int getChunkHeightMapMinimum(int x, int z) {
        return 0;
    }

    public int getPrecipitationHeight(int x, int z) {
        return 0;
    }

    public int getTopSolidOrLiquidBlock(int x, int z) {
        return 0;
    }

    public int getFirstUncoveredBlock(int x, int z) {
        return 0;
    }

    // --- Chunk methods ---

    public boolean blockExists(int x, int y, int z) {
        return false;
    }

    public boolean doChunksNearChunkExist(int x, int y, int z, int range) {
        return false;
    }

    public abstract boolean checkChunksExist(int x1, int y1, int z1, int x2, int y2, int z2);

    public boolean chunkExists(int chunkX, int chunkZ) {
        return false;
    }

    public Chunk getChunkFromBlockCoords(int x, int z) {
        return null;
    }

    public Chunk getChunkFromChunkCoords(int chunkX, int chunkZ) {
        return null;
    }

    // --- Biome ---

    public abstract BiomeGenBase getBiomeGenForCoords(int x, int z);

    public abstract WorldChunkManager getWorldChunkManager();

    // --- Entity methods ---

    public abstract boolean spawnEntityInWorld(Entity entity);

    public void removeEntity(Entity entity) {}

    public void removePlayerEntityDangerously(Entity entity) {}

    public boolean addWeatherEffect(Entity entity) {
        return false;
    }

    public void obtainEntitySkin(Entity entity) {}

    public void releaseEntitySkin(Entity entity) {}

    public abstract List getEntitiesWithinAABB(Class entityClass, AxisAlignedBB aabb);

    public abstract List getEntitiesWithinAABBExcludingEntity(Entity entity, AxisAlignedBB aabb);

    public List getEntitiesWithinAABBExcludingEntity(Entity entity, AxisAlignedBB aabb, Object selector) {
        return new ArrayList();
    }

    public List getCollidingBoundingBoxes(Entity entity, AxisAlignedBB aabb) {
        return new ArrayList();
    }

    public List getCollidingBlockBounds(AxisAlignedBB aabb) {
        return new ArrayList();
    }

    public abstract boolean checkNoEntityCollision(AxisAlignedBB aabb);

    public boolean checkNoEntityCollision(AxisAlignedBB aabb, Entity entity) {
        return true;
    }

    public abstract boolean canPlaceEntityOnSide(int blockID, int x, int y, int z, boolean skipEntities, int side, Entity entity, ItemStack stack);

    // --- Sound methods ---

    public abstract void playSoundEffect(double x, double y, double z, String sound, float volume, float pitch);

    public abstract void playSoundAtEntity(Entity entity, String sound, float volume, float pitch);

    public void playSoundToNearExcept(EntityPlayer player, String sound, float volume, float pitch) {}

    public void playSound(double x, double y, double z, String sound, float volume, float pitch, boolean distanceDelay) {}

    public void playSound(double x, double y, double z, String sound, float volume, float pitch) {}

    public void playRecord(String recordName, int x, int y, int z) {}

    // --- Particle ---

    public abstract void spawnParticle(String particle, double x, double y, double z, double velocityX, double velocityY, double velocityZ);

    // --- Aux SFX ---

    public abstract void playAuxSFX(int effectID, int x, int y, int z, int data);

    public void playAuxSFXAtEntity(EntityPlayer player, int effectID, int x, int y, int z, int data) {}

    public void func_82739_e(int x, int y, int z, int blockID, int data) {}

    public void func_96440_m(int x, int y, int z, int blockID) {}

    // --- Weather ---

    public abstract boolean isRaining();

    public boolean isThundering() {
        return false;
    }

    public boolean canLightningStrikeAt(int x, int y, int z) {
        return false;
    }

    public boolean isBlockHighHumidity(int x, int y, int z) {
        return false;
    }

    public float getRainStrength(float partialTicks) {
        return 0;
    }

    public float getWeightedThunderStrength(float partialTicks) {
        return 0;
    }

    public boolean IsRainingAtPos(int i, int j, int k) {
        return false;
    }

    public boolean IsSnowingAtPos(int i, int j, int k) {
        return false;
    }

    public boolean IsPrecipitatingAtPos(int i, int j, int k) {
        return false;
    }

    public boolean IsPrecipitatingAtPos(int i, int k) {
        return false;
    }

    public boolean CanLightningStrikeAtPos(int i, int j, int k) {
        return false;
    }

    public void updateWeather() {}

    // --- Time/celestial ---

    public boolean isDaytime() {
        return true;
    }

    public float getCelestialAngle(float partialTicks) {
        return 0;
    }

    public float getCelestialAngleRadians(float partialTicks) {
        return 0;
    }

    public int getMoonPhase() {
        return 0;
    }

    public int calculateSkylightSubtracted(float partialTicks) {
        return 0;
    }

    public Calendar getCurrentDate() {
        return Calendar.getInstance();
    }

    // --- Redstone ---

    public abstract boolean isBlockGettingPowered(int i, int j, int k);

    public abstract boolean isBlockIndirectlyGettingPowered(int x, int y, int z);

    // --- Ray tracing ---

    public MovingObjectPosition rayTraceBlocks(Vec3 startVec, Vec3 endVec) {
        return null;
    }

    public MovingObjectPosition rayTraceBlocks_do(Vec3 startVec, Vec3 endVec, boolean hitFluids) {
        return null;
    }

    public MovingObjectPosition rayTraceBlocks_do_do(Vec3 startVec, Vec3 endVec, boolean hitFluids, boolean ignoreNonMovement) {
        return null;
    }

    public MovingObjectPosition MouseOverRayTrace(Vec3 startVec, Vec3 endVec) {
        return null;
    }

    // --- Solid top surface ---

    public abstract boolean doesBlockHaveSolidTopSurface(int i, int j, int k);

    // --- World access ---

    public void addWorldAccess(Object worldAccess) {}

    // --- Player interaction ---

    public abstract boolean canMineBlock(EntityPlayer player, int x, int y, int z);

    // --- Dimension/map data ---

    public int getHeight() {
        return 256;
    }

    public int getActualHeight() {
        return 256;
    }

    public void setItemData(String key, Object data) {}

    public Object loadItemData(Class dataClass, String key) {
        return null;
    }

    public int getUniqueDataId(String key) {
        return 0;
    }

    // --- Explosion ---

    public Explosion NewExplosionNoFX(Entity entity, double x, double y, double z, float strength, boolean isFlaming, boolean isSmoking) {
        return null;
    }

    // --- Random seed ---

    public Random setRandomSeed(int x, int y, int z) {
        return this.rand;
    }

    // --- Structure finding ---

    public Object findClosestStructure(String structureName, int x, int y, int z) {
        return null;
    }

    // --- Vec3 pool ---

    public Vec3Pool getWorldVec3Pool() {
        return new Vec3Pool();
    }

    // --- Scoreboard ---

    public Object getScoreboard() {
        return worldScoreboard;
    }

    // --- Logging ---

    public Object getWorldLogAgent() {
        return null;
    }

    // --- Crash reports ---

    public Object addWorldInfoToCrashReport(Object crashReport) {
        return null;
    }

    // --- Block destruction effects ---

    public void destroyBlockInWorldPartially(int breakerEntityID, int x, int y, int z, int progress) {}

    // --- Minecart ---

    public Object func_82735_a(Object minecart) {
        return null;
    }

    // --- Initialization ---

    public void initialize(Object worldSettings) {}

    // --- BTW-added: Mod tick ---

    public void ModSpecificTick() {}

    // --- BTW-added: Entity methods ---

    public Entity GetClosestEntityMatchingCriteriaWithinRange(double x, double y, double z, double range, Object criteria) {
        return null;
    }

    public int CountEntitiesThatApplyToSpawnCap(Class classToCount) {
        return 0;
    }

    public int GetNumEntitiesThatApplyToSquidPossessionCap() {
        return 0;
    }

    public void NotifyNearbyAnimalsOfPlayerBlockAddOrRemove(EntityPlayer player, Block block, int i, int j, int k) {}

    // --- BTW-added: Sun/moon ---

    public float ComputeOverworldSunBrightnessWithMoonPhases() {
        return 0;
    }

    public boolean IsTheEndNigh() {
        return false;
    }

    // --- BTW-added: Magnetic points ---

    public FCMagneticPointList m_MagneticPointList;

    public FCMagneticPointList GetMagneticPointList() {
        return m_MagneticPointList;
    }

    // --- BTW-added: Ender chest ---

    public InventoryEnderChest m_localEnderChestInventory;
    public InventoryEnderChest m_localLowPowerEnderChestInventory;

    public InventoryEnderChest GetLocalEnderChestInventory() {
        return m_localEnderChestInventory;
    }

    public InventoryEnderChest GetLocalLowPowerEnderChestInventory() {
        return m_localLowPowerEnderChestInventory;
    }

    // --- BTW-added: Beacon/Looting ---

    public FCBeaconEffectLocationList m_LootingBeaconLocationList;

    public FCBeaconEffectLocationList GetLootingBeaconLocationList() {
        return m_LootingBeaconLocationList;
    }

    public int GetAmbientLootingEffectAtLocation(int iLocI, int iLocJ, int iLocK) {
        return 0;
    }

    // --- BTW-added: Spawn locations ---

    public FCSpawnLocationList m_SpawnLocationList;

    public FCSpawnLocationList GetSpawnLocationList() {
        return m_SpawnLocationList;
    }

    // --- BTW-added: View/spawn range ---

    public int GetClampedViewDistanceInChunks() {
        return 8;
    }

    public int GetMobSpawnRangeInChunks() {
        return 8;
    }

    public int GetActiveChunkRangeInChunks() {
        return 8;
    }

    // --- BTW-added: Active chunk map ---

    public Object m_activeChunksCoordsMap;
    public Object m_activeChunksCoordsList;

    public void UpdateActiveChunkMap() {}
    public void AddEntityToActiveChunkMap(Entity entity) {}
    public void AddAreaAroundChunkToActiveChunkMap(int iChunkX, int iChunkZ) {}
    public void ClearActiveChunkMap() {}
    public void AddToActiveChunkMap(int iChunkX, int iChunkZ) {}
    public boolean IsChunkActive(int iChunkX, int iChunkZ) { return false; }
    public boolean IsBlockPosActive(int i, int j, int k) { return false; }
    public Object GetActiveChunksCoordsList() { return m_activeChunksCoordsList; }

    public static final int m_iLoadedChunksUpdateRange = 32;

    // --- World time methods ---

    public long getWorldTime() { return worldInfo != null ? worldInfo.getWorldTime() : 0; }
    public void setWorldTime(long time) { if (worldInfo != null) worldInfo.setWorldTime(time); }
    public long getTotalWorldTime() { return worldInfo != null ? worldInfo.getTotalWorldTime() : 0; }

    // --- World info ---

    public WorldInfo getWorldInfo() { return worldInfo; }

    // --- Block query methods ---

    public boolean isBlockOpaqueCube(int x, int y, int z) {
        int id = getBlockId(x, y, z);
        return id > 0 && id < Block.opaqueCubeLookup.length && Block.opaqueCubeLookup[id];
    }

    public boolean blockHasTileEntity(int x, int y, int z) {
        return getBlockTileEntity(x, y, z) != null;
    }

    // --- Tile entity management ---

    public void setBlockTileEntity(int x, int y, int z, TileEntity tileEntity) {}
    public void removeBlockTileEntity(int x, int y, int z) {}

    // --- Liquid/material checks ---

    public boolean isAnyLiquid(AxisAlignedBB aabb) { return false; }
    public boolean isMaterialInBB(AxisAlignedBB aabb, Material material) { return false; }
    public boolean isAABBInMaterial(AxisAlignedBB aabb, Material material) { return false; }

    // --- Block events ---

    public void addBlockEvent(int x, int y, int z, int blockID, int eventID, int eventParam) {}

    // --- Explosion ---

    public Explosion createExplosion(Entity entity, double x, double y, double z, float strength, boolean isFlaming) {
        return null;
    }

    public Explosion createExplosion(Entity entity, int x, int y, int z, float strength, boolean isFlaming) {
        return createExplosion(entity, (double) x, (double) y, (double) z, strength, isFlaming);
    }

    // --- Player location ---

    public EntityPlayer getClosestPlayer(double x, double y, double z, double range) { return null; }
    public EntityPlayer getClosestPlayer(float x, float y, float z, double range) { return null; }
    public EntityPlayer getClosestPlayer(int x, int y, int z, float range) { return null; }
    public EntityPlayer getClosestPlayerToEntity(Entity entity, double range) { return null; }
    public EntityPlayer getClosestVulnerablePlayerToEntity(Entity entity, double range) { return null; }
    public EntityPlayer getClosestVulnerablePlayer(double x, double y, double z, double range) { return null; }

    // --- Entity state ---

    public void setEntityState(Entity entity, byte state) {}

    public Entity getEntityByID(int id) { return null; }

    public void addEntityToWorld(int entityID, Entity entity) {}

    // --- Chunk provider ---

    public IChunkProvider getChunkProvider() { return null; }

    // --- Spawn point ---

    public ChunkCoordinates getSpawnPoint() { return null; }

    // --- Game rules ---

    public GameRules getGameRules() { return null; }

    // --- Block density ---

    public float getBlockDensity(Vec3 pos, AxisAlignedBB bb) { return 0.0F; }

    // --- Tile entity collection ---

    public List getAllTileEntityInBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return new java.util.ArrayList();
    }

    // --- Secondary explosion ---

    public void AddSecondaryExplosionNoFX(double x, double y, double z, float strength, boolean isFlaming, boolean isSmoking) {}

    // --- New explosion ---

    public Explosion newExplosion(Entity entity, double x, double y, double z, float strength, boolean isFlaming, boolean isSmoking) {
        return null;
    }

    // --- Village ---

    public VillageCollection villageCollectionObj;

    public static boolean InstallationIntegrityTest() { return true; }
}
