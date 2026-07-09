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

    // 1.5.2 World.isBoundingBoxBurning (FCMOD Entity overload, vanilla/server World.java:2112) —
    // frozen Entity.moveEntity (vanilla Entity.java:2849) calls it every tick for contact fire/lava damage.
    // FCMOD: contracts the entity's box and asks each block GetDoesFireDamageToEntities.
    public boolean isBoundingBoxBurning(Entity entity) {
        // FCMOD: Added
        AxisAlignedBB aabb = entity.boundingBox.contract(0.001D, 0.001D, 0.001D);
        // END FCMOD

        int minX = MathHelper.floor_double(aabb.minX);
        int maxX = MathHelper.floor_double(aabb.maxX + 1.0D);
        int minY = MathHelper.floor_double(aabb.minY);
        int maxY = MathHelper.floor_double(aabb.maxY + 1.0D);
        int minZ = MathHelper.floor_double(aabb.minZ);
        int maxZ = MathHelper.floor_double(aabb.maxZ + 1.0D);

        if (this.checkChunksExist(minX, minY, minZ, maxX, maxY, maxZ)) {
            for (int x = minX; x < maxX; ++x) {
                for (int y = minY; y < maxY; ++y) {
                    for (int z = minZ; z < maxZ; ++z) {
                        // FCMOD: Changed — per-block fire damage check
                        Block block = Block.blocksList[getBlockId(x, y, z)];

                        if (block != null && block.GetDoesFireDamageToEntities(this, x, y, z, entity)) {
                            // END FCMOD
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    // 1.5.2 World.isBoundingBoxBurning (pre-FCMOD AABB overload) — frozen
    // Entity.java:890 fire-check path; scans the box for fire/lava block IDs.
    public boolean isBoundingBoxBurning(AxisAlignedBB aabb) {
        int minX = MathHelper.floor_double(aabb.minX);
        int maxX = MathHelper.floor_double(aabb.maxX + 1.0D);
        int minY = MathHelper.floor_double(aabb.minY);
        int maxY = MathHelper.floor_double(aabb.maxY + 1.0D);
        int minZ = MathHelper.floor_double(aabb.minZ);
        int maxZ = MathHelper.floor_double(aabb.maxZ + 1.0D);

        if (this.checkChunksExist(minX, minY, minZ, maxX, maxY, maxZ)) {
            for (int x = minX; x < maxX; ++x) {
                for (int y = minY; y < maxY; ++y) {
                    for (int z = minZ; z < maxZ; ++z) {
                        int id = this.getBlockId(x, y, z);

                        if ((Block.fire != null && id == Block.fire.blockID)
                                || (Block.lavaMoving != null && id == Block.lavaMoving.blockID)
                                || (Block.lavaStill != null && id == Block.lavaStill.blockID)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

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

    // 1.5.2 World.handleMaterialAcceleration (vanilla/server World.java:2158) —
    // frozen Entity.handleWaterMovement (Entity.java:1081) calls it every tick;
    // accumulates the fluid flow vector below the fluid surface and pushes the
    // entity's motion by 0.014 per normalized component (water-stream transport).
    public boolean handleMaterialAcceleration(AxisAlignedBB aabb, Material material, Entity entity) {
        int minX = MathHelper.floor_double(aabb.minX);
        int maxX = MathHelper.floor_double(aabb.maxX + 1.0D);
        int minY = MathHelper.floor_double(aabb.minY);
        int maxY = MathHelper.floor_double(aabb.maxY + 1.0D);
        int minZ = MathHelper.floor_double(aabb.minZ);
        int maxZ = MathHelper.floor_double(aabb.maxZ + 1.0D);

        if (!this.checkChunksExist(minX, minY, minZ, maxX, maxY, maxZ)) {
            return false;
        } else {
            boolean foundMatch = false;
            Vec3 flowVec = this.getWorldVec3Pool().getVecFromPool(0.0D, 0.0D, 0.0D);

            for (int x = minX; x < maxX; ++x) {
                for (int y = minY; y < maxY; ++y) {
                    for (int z = minZ; z < maxZ; ++z) {
                        Block block = Block.blocksList[this.getBlockId(x, y, z)];

                        if (block != null && block.blockMaterial == material) {
                            double fluidTop = (double)((float)(y + 1) - BlockFluid.getFluidHeightPercent(this.getBlockMetadata(x, y, z)));

                            if ((double)maxY >= fluidTop) {
                                foundMatch = true;
                                block.velocityToAddToEntity(this, x, y, z, entity, flowVec);
                            }
                        }
                    }
                }
            }

            if (flowVec.lengthVector() > 0.0D && entity.func_96092_aw()) {
                flowVec = flowVec.normalize();
                double push = 0.014D;
                entity.motionX += flowVec.xCoord * push;
                entity.motionY += flowVec.yCoord * push;
                entity.motionZ += flowVec.zCoord * push;
            }

            return foundMatch;
        }
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

    // 1.5.2 World.notifyBlocksOfNeighborChange(x,y,z,id,side) (vanilla/server World.java:708) —
    // FCBlockRedstoneRepeater.java:69-84 post-rotation redstone cleanup; notifies the
    // 6 neighbors except the excluded side (notifyBlockOfNeighborChange is real via WorldBridge).
    public void notifyBlocksOfNeighborChange(int x, int y, int z, int blockID, int excludedSide) {
        if (excludedSide != 4) {
            this.notifyBlockOfNeighborChange(x - 1, y, z, blockID);
        }

        if (excludedSide != 5) {
            this.notifyBlockOfNeighborChange(x + 1, y, z, blockID);
        }

        if (excludedSide != 0) {
            this.notifyBlockOfNeighborChange(x, y - 1, z, blockID);
        }

        if (excludedSide != 1) {
            this.notifyBlockOfNeighborChange(x, y + 1, z, blockID);
        }

        if (excludedSide != 2) {
            this.notifyBlockOfNeighborChange(x, y, z - 1, blockID);
        }

        if (excludedSide != 3) {
            this.notifyBlockOfNeighborChange(x, y, z + 1, blockID);
        }
    }

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

    // 1.5.2 World.getLightBrightness (vanilla/server World.java:1018) — frozen
    // Entity.getBrightness → EntityMob/EntityAnimal.getBlockPathWeight wander/flee
    // scoring; light value mapped through the provider's brightness curve.
    public float getLightBrightness(int x, int y, int z) {
        if (provider == null) return 0;
        return provider.lightBrightnessTable[this.getBlockLightValue(x, y, z)];
    }

    public int getLightBrightnessForSkyBlocks(int x, int y, int z, int lightValue) {
        return 0;
    }

    // 1.5.2 World.GetBlockNaturalLightValue (BTW-patched vanilla/server World.java:4704) —
    // saved sky light with the current skylight subtraction applied.
    public int GetBlockNaturalLightValue(int i, int j, int k) {
        return GetBlockNaturalLightValue_do(i, j, k, true, skylightSubtracted);
    }

    // 1.5.2 World.GetBlockNaturalLightValueMaximum (vanilla/server World.java:4709) —
    // FCBlockFarmland.java:228 crop growth gate, FCBlockGrass/FCBlockDirtSlab/
    // FCBlockTallGrass grass spread, FCTileEntityUnfiredBrick.java:109 brick drying.
    // Saved sky light with zero skylight subtraction (works under glass/leaves/overhangs).
    public int GetBlockNaturalLightValueMaximum(int i, int j, int k) {
        return GetBlockNaturalLightValue_do(i, j, k, true, 0);
    }

    // 1.5.2 World.GetNaturalLightBrightness (vanilla/server World.java:4714)
    public float GetNaturalLightBrightness(int i, int j, int k) {
        if (provider == null) return 0;
        return provider.lightBrightnessTable[GetBlockNaturalLightValue(i, j, k)];
    }

    // 1.5.2 World.GetBlockNaturalLightValue_do (vanilla/server World.java:4719) —
    // version of getBlockLightValue_do modified to only consider natural light,
    // propagating through neighbors for non-opaque blocks.
    private int GetBlockNaturalLightValue_do(int i, int j, int k, boolean bConsiderNeighbors, int iSkylightToSubtract) {
        if (i >= -30000000 && k >= -30000000 && i < 30000000 && k < 30000000) {
            if (bConsiderNeighbors) {
                int iBlockID = getBlockId(i, j, k);

                if (iBlockID >= 0 && iBlockID < Block.useNeighborBrightness.length
                        && Block.useNeighborBrightness[iBlockID]) {
                    int iNeighbor1 = GetBlockNaturalLightValue_do(i, j + 1, k, false, iSkylightToSubtract);
                    int iNeighbor2 = GetBlockNaturalLightValue_do(i + 1, j, k, false, iSkylightToSubtract);
                    int iNeighbor3 = GetBlockNaturalLightValue_do(i - 1, j, k, false, iSkylightToSubtract);
                    int iNeighbor4 = GetBlockNaturalLightValue_do(i, j, k + 1, false, iSkylightToSubtract);
                    int iNeighbor5 = GetBlockNaturalLightValue_do(i, j, k - 1, false, iSkylightToSubtract);

                    if (iNeighbor2 > iNeighbor1) {
                        iNeighbor1 = iNeighbor2;
                    }

                    if (iNeighbor3 > iNeighbor1) {
                        iNeighbor1 = iNeighbor3;
                    }

                    if (iNeighbor4 > iNeighbor1) {
                        iNeighbor1 = iNeighbor4;
                    }

                    if (iNeighbor5 > iNeighbor1) {
                        iNeighbor1 = iNeighbor5;
                    }

                    return iNeighbor1;
                }
            }

            if (j < 0) {
                return 0;
            } else {
                if (j >= 256) {
                    j = 255;
                }

                Chunk chunk = this.getChunkFromChunkCoords(i >> 4, k >> 4);

                i &= 15;
                k &= 15;

                return chunk.GetBlockNaturalLightValue(i, j, k, iSkylightToSubtract);
            }
        } else {
            return 15;
        }
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

    // 1.5.2 World.getCollidingBlockBounds (vanilla/server World.java:1511) — frozen
    // Entity.pushOutOfBlocks (Entity.java:2040) and move-to-free-space (Entity.java:1831);
    // block collision boxes only, no entity boxes (uses a local list instead of
    // vanilla's shared collidingBoundingBoxes field, matching the bridge style).
    public List getCollidingBlockBounds(AxisAlignedBB aabb) {
        List list = new ArrayList();
        int minX = MathHelper.floor_double(aabb.minX);
        int maxX = MathHelper.floor_double(aabb.maxX + 1.0D);
        int minY = MathHelper.floor_double(aabb.minY);
        int maxY = MathHelper.floor_double(aabb.maxY + 1.0D);
        int minZ = MathHelper.floor_double(aabb.minZ);
        int maxZ = MathHelper.floor_double(aabb.maxZ + 1.0D);

        for (int x = minX; x < maxX; ++x) {
            for (int z = minZ; z < maxZ; ++z) {
                if (this.blockExists(x, 64, z)) {
                    for (int y = minY - 1; y < maxY; ++y) {
                        Block block = Block.blocksList[this.getBlockId(x, y, z)];

                        if (block != null) {
                            block.addCollisionBoxesToList(this, x, y, z, aabb, list, (Entity) null);
                        }
                    }
                }
            }
        }

        return list;
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

    // 1.5.2 World.isBlockHighHumidity (vanilla/server World.java:3863) —
    // FCBlockFire.java:94,216,245,363 fire spread/burn-rate humidity resistance.
    public boolean isBlockHighHumidity(int x, int y, int z) {
        BiomeGenBase biome = this.getBiomeGenForCoords(x, z);
        return biome != null && biome.isHighHumidity();
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

    // 1.5.2 World.IsPrecipitatingAtPos(i,k) (BTW-patched vanilla/server World.java:5191) —
    // FCEntityWindMillVertical.java:634 rain-over-rotor scan and FCEntityWindMill.java:283
    // storm damage; column check without sky visibility (isRaining real via WorldBridge).
    public boolean IsPrecipitatingAtPos(int i, int k) {
        if (isRaining()) {
            BiomeGenBase biome = getBiomeGenForCoords(i, k);

            return biome != null && (biome.getEnableSnow() || biome.CanRainInBiome());
        }

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

    // 1.5.2 World.getMoonPhase (vanilla/server World.java:1576) — FCEntityWolf.
    // IsWildAndHostile (phase 0 = full moon) and FCEntityAIWolfHowl howl trigger;
    // delegates to the provider's (time/24000 % 8) formula. WorldBridge overrides
    // with the live level's moon phase (same numbering).
    public int getMoonPhase() {
        if (provider == null) return 0;
        return provider.getMoonPhase(this.getWorldTime());
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

    // 1.5.2 World.setItemData/loadItemData/getUniqueDataId (vanilla/server World.java:3873-3894) —
    // FCItemEmptyMap.java:41,45 map crafting; delegates to mapStorage exactly like
    // vanilla (WorldBridge installs a live MapStorage backed by the MC level).
    public void setItemData(String key, WorldSavedData data) {
        if (this.mapStorage != null) {
            this.mapStorage.setData(key, data);
        }
    }

    public WorldSavedData loadItemData(Class dataClass, String key) {
        return this.mapStorage != null ? this.mapStorage.loadData(dataClass, key) : null;
    }

    public int getUniqueDataId(String key) {
        return this.mapStorage != null ? this.mapStorage.getUniqueDataId(key) : 0;
    }

    // --- Explosion ---

    // 1.5.2 World.NewExplosionNoFX (BTW-patched vanilla/server World.java:4902) —
    // FCBlockLogSmouldering.Explode (FCBlockLogSmouldering.java:389); copy of
    // newExplosion() that suppresses the audio/visual effects. WorldBridge
    // overrides with a modern-engine explosion without client FX.
    public Explosion NewExplosionNoFX(Entity entity, double x, double y, double z, float strength, boolean isFlaming, boolean isSmoking) {
        Explosion explosion = new Explosion(this, entity, x, y, z, strength);

        explosion.isFlaming = isFlaming;
        explosion.isSmoking = isSmoking;
        explosion.m_bSuppressFX = true;

        explosion.doExplosionA();
        explosion.doExplosionB(false); // false tells individual block destruction effects not to play

        return explosion;
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

    // 1.5.2 World.NotifyNearbyAnimalsOfPlayerBlockAddOrRemove (vanilla/server World.java:4678) —
    // FCItemPlacesAsBlock.java:111, FCItemBlockAestheticNonOpaque.java:260,
    // FCItemBlockCompanionCube.java:203, FCItemBlockSlab.java:93; hardcore-animal
    // spooking on player block placement (entity query real via WorldBridge).
    public void NotifyNearbyAnimalsOfPlayerBlockAddOrRemove(EntityPlayer player, Block block, int i, int j, int k) {
        if (!isRemote && block.blockMaterial.blocksMovement() && !player.capabilities.isCreativeMode) {
            double dXBlock = (double)i + 0.5D;
            double dYBlock = (double)j + 0.5D;
            double dZBlock = (double)k + 0.5D;

            AxisAlignedBB targetBox = AxisAlignedBB.getAABBPool().getAABB(dXBlock - 8D, dYBlock - 4D, dZBlock - 8D, dXBlock + 8D, dYBlock + 4D, dZBlock + 8D);

            List animalList = this.getEntitiesWithinAABB(EntityAnimal.class, targetBox);

            java.util.Iterator animalIterator = animalList.iterator();

            while (animalIterator.hasNext()) {
                EntityAnimal tempAnimal = (EntityAnimal)animalIterator.next();

                if (!tempAnimal.isLivingDead) {
                    tempAnimal.OnNearbyPlayerBlockAddOrRemove(player);
                }
            }
        }
    }

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

    // 1.5.2 World.GetAmbientLootingEffectAtLocation (BTW-patched vanilla/server World.java:4883) —
    // frozen EntityLiving.GetAmbientLootingModifier (EntityLiving.java:3423) on every mob death;
    // the beacon location list is real via WorldBridge.initFcDataHolders.
    public int GetAmbientLootingEffectAtLocation(int iLocI, int iLocJ, int iLocK) {
        FCBeaconEffectLocationList lootingList = GetLootingBeaconLocationList();

        if (lootingList != null) {
            return lootingList.GetMostPowerfulBeaconEffectForLocation(iLocI, iLocK);
        }

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
    // World seed — Chunk.getRandomWithSeed (slime-chunk RNG); WorldBridge overrides with the live seed.
    public long getSeed() { return worldInfo != null ? worldInfo.getSeed() : 0L; }

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

    // 1.5.2 World.getBlockDensity (vanilla/server World.java:2308) — FCExplosionMining.java:173
    // exposure fraction for mining charge entity damage; samples rays from AABB points
    // to the explosion center via rayTraceBlocks (real via WorldBridge).
    public float getBlockDensity(Vec3 pos, AxisAlignedBB bb) {
        double stepX = 1.0D / ((bb.maxX - bb.minX) * 2.0D + 1.0D);
        double stepY = 1.0D / ((bb.maxY - bb.minY) * 2.0D + 1.0D);
        double stepZ = 1.0D / ((bb.maxZ - bb.minZ) * 2.0D + 1.0D);
        int unblocked = 0;
        int total = 0;

        for (float fx = 0.0F; fx <= 1.0F; fx = (float)((double)fx + stepX)) {
            for (float fy = 0.0F; fy <= 1.0F; fy = (float)((double)fy + stepY)) {
                for (float fz = 0.0F; fz <= 1.0F; fz = (float)((double)fz + stepZ)) {
                    double sampleX = bb.minX + (bb.maxX - bb.minX) * (double)fx;
                    double sampleY = bb.minY + (bb.maxY - bb.minY) * (double)fy;
                    double sampleZ = bb.minZ + (bb.maxZ - bb.minZ) * (double)fz;

                    if (this.rayTraceBlocks(this.getWorldVec3Pool().getVecFromPool(sampleX, sampleY, sampleZ), pos) == null) {
                        ++unblocked;
                    }

                    ++total;
                }
            }
        }

        return (float)unblocked / (float)total;
    }

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
