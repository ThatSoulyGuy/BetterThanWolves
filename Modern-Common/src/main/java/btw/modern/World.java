package btw.modern;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Abstract representation of a game world.
 * Mirrors net.minecraft.src.World with identical field/method names.
 */
public abstract class World implements IBlockAccess {

    /** Debug counter: increments every time getEntityPathToXYZ is called. */
    public static final AtomicInteger pathfindCallCount = new AtomicInteger(0);

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
    public IChunkProvider chunkProvider;
    public ISaveHandler saveHandler;
    /**
     * Vanilla 1.5.2 EntityCreature.HandlePossession (and other FCMOD code)
     * dereferences {@code worldObj.getWorldInfo().getGameType()} during
     * tick. The server-side WorldBridge overwrites this with the real
     * MC WorldInfo, but the client default needed a non-null sentinel
     * to avoid per-tick NPEs across every entity.
     */
    public WorldInfo worldInfo = new WorldInfo();
    public boolean findingSpawnPoint;
    public MapStorage mapStorage;
    /**
     * Vanilla 1.5.2 Entity.onEntityUpdate dereferences {@code worldObj.theProfiler}
     * unconditionally on every tick. Field-initialised here so every World
     * subclass (server WorldBridge, client world stub, etc.) starts with a
     * working no-op profiler instead of NPEing on the first entity tick.
     * Subclasses are free to overwrite this with the real MC profiler.
     */
    public Profiler theProfiler = new Profiler();
    public Scoreboard worldScoreboard;
    public boolean spawnHostileMobs = true;
    public boolean spawnPeacefulMobs = true;

    /** This is set to true for client worlds, and false for server worlds. */
    public boolean isRemote;

    // --- Abstract methods (must be implemented by backend) ---

    public abstract IChunkProvider createChunkProvider();

    // --- IBlockAccess implementation / Block query methods ---

    public abstract int getBlockId(int x, int y, int z);

    public abstract int getBlockMetadata(int x, int y, int z);

    public abstract Material getBlockMaterial(int x, int y, int z);

    public abstract boolean isAirBlock(int x, int y, int z);

    public abstract TileEntity getBlockTileEntity(int x, int y, int z);

    public boolean isBlockNormalCube(int x, int y, int z) {
        return Block.isNormalCube(getBlockId(x, y, z));
    }

    /**
     * Vanilla 1.5.2 World.isBoundingBoxBurning — returns true if the
     * entity's bounding box intersects fire or lava. Called by vanilla
     * Entity.moveEntity. Default stub returns false; WorldBridge
     * overrides to check the live MC level.
     */
    public boolean isBoundingBoxBurning(Entity entity) {
        return false;
    }

    /**
     * Overload used by some FC code paths that want to check a specific
     * AABB (e.g., looking ahead for movement). Same behavior as above.
     */
    public boolean isBoundingBoxBurning(AxisAlignedBB aabb) {
        return false;
    }

    /**
     * Vanilla 1.5.2 World.handleMaterialAcceleration — scans every block
     * intersecting {@code aabb}, and for each one whose block material
     * equals {@code material}, nudges {@code entity}'s motion in the
     * direction of the liquid's flow.  Called from {@code Entity.handleWaterMovement}
     * and {@code Entity.handleLavaMovement}.  Returns true if any matching
     * block was found.
     *
     * <p>This is the exact algorithm from vanilla 1.5.2, needed so FC's
     * canonical Entity.moveEntity recognizes entities as being in water/
     * lava for drag, buoyancy, and flow-push physics.</p>
     */
    /**
     * Vanilla 1.5.2 World.func_85174_u — returns true if the block at
     * (x,y,z) has a full-cube collision box (average AABB edge length
     * >= 1.0). Used by entity collision logic to decide whether the
     * block is something a mob can stand on or be pushed against.
     * Mirrors the vanilla MCP-named method exactly.
     */
    public boolean func_85174_u(int x, int y, int z) {
        int id = getBlockId(x, y, z);
        if (id != 0 && Block.blocksList[id] != null) {
            AxisAlignedBB aabb = Block.blocksList[id].getCollisionBoundingBoxFromPool(this, x, y, z);
            return aabb != null && aabb.getAverageEdgeLength() >= 1.0D;
        }
        return false;
    }

    public boolean handleMaterialAcceleration(AxisAlignedBB aabb, Material material, Entity entity) {
        int minX = (int) Math.floor(aabb.minX);
        int maxX = (int) Math.floor(aabb.maxX + 1.0D);
        int minY = (int) Math.floor(aabb.minY);
        int maxY = (int) Math.floor(aabb.maxY + 1.0D);
        int minZ = (int) Math.floor(aabb.minZ);
        int maxZ = (int) Math.floor(aabb.maxZ + 1.0D);
        for (int x = minX; x < maxX; ++x) {
            for (int y = minY; y < maxY; ++y) {
                for (int z = minZ; z < maxZ; ++z) {
                    int id = getBlockId(x, y, z);
                    if (id <= 0) continue;
                    Block block = Block.blocksList[id];
                    if (block == null) continue;
                    if (block.blockMaterial == material) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Vanilla 1.5.2 World.blockGetRenderType — returns the vanilla
     * render type ID of the block at the given coordinates (used by
     * Entity.moveEntity to decide footstep sounds for fence/slab/
     * non-full blocks). Default returns -1 (unknown); WorldBridge
     * can override if a more accurate value is needed.
     */
    public int blockGetRenderType(int x, int y, int z) {
        int id = getBlockId(x, y, z);
        if (id <= 0) return -1;
        Block block = Block.blocksList[id];
        return block == null ? -1 : block.getRenderType();
    }

    /**
     * Returns the Block instance at the given position, or null if the
     * block ID is out of range or the slot is empty.
     */
    public Block getBlock(int x, int y, int z) {
        int id = getBlockId(x, y, z);
        if (id >= 0 && id < Block.blocksList.length) {
            return Block.blocksList[id];
        }
        return null;
    }

    /**
     * Returns true if the block at the given position has a solid side
     * on the specified face.  Checks the block's material solidity.
     */
    public boolean isBlockSolidOnSide(int x, int y, int z, int side) {
        Block block = getBlock(x, y, z);
        if (block != null) {
            return block.blockMaterial.isSolid();
        }
        return false;
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
        return 15;
    }

    public int getBlockLightValue_do(int x, int y, int z, boolean useNeighborBrightness) {
        return 15;
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
        // Returns the maximum natural (sky) light the block can receive.
        // If the block can see the sky, it gets full sunlight (15).
        return canBlockSeeTheSky(i, j, k) ? 15 : 0;
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
        return true;
    }

    public boolean doChunksNearChunkExist(int x, int y, int z, int range) {
        return false;
    }

    public abstract boolean checkChunksExist(int x1, int y1, int z1, int x2, int y2, int z2);

    public boolean chunkExists(int chunkX, int chunkZ) {
        return false;
    }

    /**
     * Lazily-allocated, shared "always loaded" sentinel chunk. Vanilla 1.5.2
     * Entity / EntityLiving code dereferences {@code getChunkFromBlockCoords(...).isChunkLoaded}
     * unconditionally during movement. The server-side WorldBridge overrides
     * these methods with real chunk lookups, but the client side reaches the
     * default impl. Returning null caused per-tick NPEs across every entity;
     * a sentinel chunk with {@code isChunkLoaded=true} matches the bridge's
     * "if the entity is being ticked, its chunk must be loaded" invariant.
     */
    private Chunk loadedSentinelChunk;

    public Chunk getChunkFromBlockCoords(int x, int z) {
        return getLoadedSentinelChunk();
    }

    public Chunk getChunkFromChunkCoords(int chunkX, int chunkZ) {
        return getLoadedSentinelChunk();
    }

    private Chunk getLoadedSentinelChunk() {
        if (loadedSentinelChunk == null) {
            loadedSentinelChunk = new Chunk(this, 0, 0);
            loadedSentinelChunk.isChunkLoaded = true;
        }
        return loadedSentinelChunk;
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

    public List selectEntitiesWithinAABB(Class entityClass, AxisAlignedBB aabb, IEntitySelector selector) {
        List all = getEntitiesWithinAABB(entityClass, aabb);
        if (selector == null || all == null) return all;
        List filtered = new ArrayList();
        for (Object e : all) {
            if (e instanceof Entity && selector.isEntityApplicable((Entity) e)) {
                filtered.add(e);
            }
        }
        return filtered;
    }

    /**
     * Vanilla 1.5.2 World.getEntityPathToXYZ — creates a ChunkCache around
     * the entity and delegates to PathFinder for real A* pathfinding.
     * Copied verbatim from vanilla so FC's PathNavigate calls this directly.
     */
    public PathEntity getEntityPathToXYZ(Entity entity, int x, int y, int z, float range,
                                         boolean canBreakDoors, boolean canEnterDoors,
                                         boolean avoidsWater, boolean canSwim) {
        pathfindCallCount.incrementAndGet();
        this.theProfiler.startSection("pathfind");
        int ex = MathHelper.floor_double(entity.posX);
        int ey = MathHelper.floor_double(entity.posY);
        int ez = MathHelper.floor_double(entity.posZ);
        int pad = (int)(range + 8.0F);
        ChunkCache cache = new ChunkCache(this, ex - pad, ey - pad, ez - pad,
                ex + pad, ey + pad, ez + pad, 0);
        PathEntity result = (new PathFinder(cache, canBreakDoors, canEnterDoors,
                avoidsWater, canSwim)).createEntityPathTo(entity, x, y, z, range);
        this.theProfiler.endSection();
        return result;
    }

    /**
     * Vanilla 1.5.2 World.getPathEntityToEntity — delegates to getEntityPathToXYZ.
     */
    public PathEntity getPathEntityToEntity(Entity source, Entity target, float range,
                                             boolean canBreakDoors, boolean canEnterDoors,
                                             boolean avoidsWater, boolean canSwim) {
        if (target == null) return null;
        return getEntityPathToXYZ(source,
                MathHelper.floor_double(target.posX),
                (int)target.posY,
                MathHelper.floor_double(target.posZ),
                range, canBreakDoors, canEnterDoors, avoidsWater, canSwim);
    }

    public abstract List getEntitiesWithinAABBExcludingEntity(Entity entity, AxisAlignedBB aabb);

    public List getEntitiesWithinAABBExcludingEntity(Entity entity, AxisAlignedBB aabb, IEntitySelector selector) {
        return new ArrayList();
    }

    public Entity findNearestEntityWithinAABB(Class entityClass, AxisAlignedBB aabb, Entity exclude) {
        List list = this.getEntitiesWithinAABB(entityClass, aabb);
        Entity nearest = null;
        double nearestDistSq = Double.MAX_VALUE;
        for (int i = 0; i < list.size(); i++) {
            Entity candidate = (Entity) list.get(i);
            if (candidate != exclude) {
                double distSq = exclude.getDistanceSqToEntity(candidate);
                if (distSq <= nearestDistSq) {
                    nearest = candidate;
                    nearestDistSq = distSq;
                }
            }
        }
        return nearest;
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
        return worldInfo != null && worldInfo.isThundering();
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

    public void addWorldAccess(IWorldAccess worldAccess) {}

    // --- Player interaction ---

    public abstract boolean canMineBlock(EntityPlayer player, int x, int y, int z);

    // --- Dimension/map data ---

    public int getHeight() {
        return 256;
    }

    public int getActualHeight() {
        return 256;
    }

    public void setItemData(String key, WorldSavedData data) {}

    public WorldSavedData loadItemData(Class dataClass, String key) {
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

    public ChunkPosition findClosestStructure(String structureName, int x, int y, int z) {
        return null;
    }

    // --- Vec3 pool ---

    public Vec3Pool getWorldVec3Pool() {
        return new Vec3Pool();
    }

    // --- Scoreboard ---

    public Scoreboard getScoreboard() {
        return worldScoreboard;
    }

    // --- Logging ---

    public ILogAgent getWorldLogAgent() {
        return null;
    }

    // --- Crash reports ---

    public CrashReportCategory addWorldInfoToCrashReport(CrashReport crashReport) {
        return null;
    }

    // --- Block destruction effects ---

    public void destroyBlockInWorldPartially(int breakerEntityID, int x, int y, int z, int progress) {}

    // --- Minecart ---

    public IUpdatePlayerListBox func_82735_a(EntityMinecart minecart) {
        return null;
    }

    // --- Initialization ---

    public void initialize(WorldSettings worldSettings) {}

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

    public LongHashMap m_activeChunksCoordsMap;
    public LinkedList<ChunkCoordIntPair> m_activeChunksCoordsList;

    public void UpdateActiveChunkMap() {}
    public void AddEntityToActiveChunkMap(Entity entity) {}
    public void AddAreaAroundChunkToActiveChunkMap(int iChunkX, int iChunkZ) {}
    public void ClearActiveChunkMap() {}
    public void AddToActiveChunkMap(int iChunkX, int iChunkZ) {}

    /**
     * FC's "active chunk map" is its own caching layer separate from MC's
     * chunk loading. Vanilla 1.5.2 EntityLiving.despawnEntity() calls
     * {@code IsChunkActive} and immediately {@link Entity#setDead() setDead()}'s
     * the entity if it returns false. Returning false on every call meant
     * EVERY despawnable hostile mob died on its first tick.
     *
     * Bridge invariant: if MC is ticking the entity through ProxyMob/
     * ProxyAnimal, by definition its chunk is loaded by MC, so we can
     * report it as active. MC owns chunk loading and despawn lifecycle;
     * FC's chunk-activity tracking is simply not relevant in the bridge.
     */
    public boolean IsChunkActive(int iChunkX, int iChunkZ) { return true; }
    public boolean IsBlockPosActive(int i, int j, int k) { return true; }
    public LinkedList<ChunkCoordIntPair> GetActiveChunksCoordsList() { return m_activeChunksCoordsList; }

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

    // --- Player location ---

    // 1.5.2 World.getPlayerEntityByName — frozen EntityTameable.getOwner calls
    // it every AI tick for tamed wolves/cats; EntityThrowable.getThrower uses
    // it after NBT reload. WorldBridge overrides with the live player list.
    public EntityPlayer getPlayerEntityByName(String name) {
        for (int i = 0; i < this.playerEntities.size(); i++) {
            if (name.equals(((EntityPlayer) this.playerEntities.get(i)).username)) {
                return (EntityPlayer) this.playerEntities.get(i);
            }
        }
        return null;
    }

    public EntityPlayer getClosestPlayer(double x, double y, double z, double range) { return null; }
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

    private static final GameRules DEFAULT_GAME_RULES = new GameRules();

    public GameRules getGameRules() { return DEFAULT_GAME_RULES; }

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

    public VillageCollection villageCollectionObj = new VillageCollection();

    public static boolean InstallationIntegrityTest() { return true; }
}
