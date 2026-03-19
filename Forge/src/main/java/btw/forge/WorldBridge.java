package btw.forge;

import btw.modern.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

/**
 * Full {@link btw.modern.World} implementation backed by a Forge 1.20.1
 * {@link ServerLevel}.  This replaces {@link ForgeWorldServerWrapper} as the
 * canonical bridge between FC legacy code (int IDs, block coords) and the
 * modern block-state system.
 *
 * Instances are cached per-ServerLevel via {@link #getOrCreate(ServerLevel)}.
 */
public class WorldBridge extends btw.modern.World {

    private static final Logger LOGGER = LogManager.getLogger("BTW-WorldBridge");

    private static final Map<ServerLevel, WorldBridge> cache = new WeakHashMap<>();

    /**
     * Returns (or creates) the WorldBridge for the given ServerLevel.
     */
    public static WorldBridge getOrCreate(ServerLevel level) {
        return cache.computeIfAbsent(level, WorldBridge::new);
    }

    private final ServerLevel level;

    public WorldBridge(ServerLevel level) {
        this.level = level;
        this.rand = new Random();
        this.isRemote = false;
    }

    /**
     * Returns the underlying modern ServerLevel.
     */
    public ServerLevel getServerLevel() {
        return level;
    }

    // ================================================================
    // IChunkProvider (required abstract)
    // ================================================================

    
    public IChunkProvider createChunkProvider() {
        // FC code should not call this on the Forge backend
        return null;
    }

    // ================================================================
    // Block access
    // ================================================================

    
    public int getBlockId(int x, int y, int z) {
        BlockState state = level.getBlockState(new BlockPos(x, y, z));
        if (state.getBlock() instanceof ProxyBlock pb) {
            return pb.getLegacyId();
        }
        return 0;
    }

    
    public int getBlockMetadata(int x, int y, int z) {
        BlockState state = level.getBlockState(new BlockPos(x, y, z));
        if (state.hasProperty(ProxyBlock.META)) {
            return state.getValue(ProxyBlock.META);
        }
        return 0;
    }

    
    public Material getBlockMaterial(int x, int y, int z) {
        int id = getBlockId(x, y, z);
        if (id > 0 && id < btw.modern.Block.blocksList.length) {
            btw.modern.Block fcBlock = btw.modern.Block.blocksList[id];
            if (fcBlock != null) {
                return fcBlock.blockMaterial;
            }
        }
        return Material.air;
    }

    
    public boolean isAirBlock(int x, int y, int z) {
        return level.isEmptyBlock(new BlockPos(x, y, z));
    }

    
    public boolean isBlockNormalCube(int x, int y, int z) {
        int id = getBlockId(x, y, z);
        return btw.modern.Block.isNormalCube(id);
    }

    
    public TileEntity getBlockTileEntity(int x, int y, int z) {
        // TODO: bridge modern BlockEntity to btw.modern.TileEntity
        return null;
    }

    // ================================================================
    // Block modification
    // ================================================================

    
    public boolean setBlock(int x, int y, int z, int blockID, int metadata, int flags) {
        ProxyBlock proxy = ProxyRegistry.getProxy(blockID);
        if (proxy != null) {
            BlockState state = proxy.defaultBlockState()
                    .setValue(ProxyBlock.META, Math.min(Math.max(metadata, 0), 15));
            return level.setBlock(new BlockPos(x, y, z), state, flags);
        }
        return false;
    }

    
    public boolean setBlockToAir(int x, int y, int z) {
        return level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
    }

    
    public boolean setBlockMetadata(int x, int y, int z, int metadata, int flags) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState current = level.getBlockState(pos);
        if (current.hasProperty(ProxyBlock.META)) {
            BlockState newState = current.setValue(ProxyBlock.META, Math.min(Math.max(metadata, 0), 15));
            return level.setBlock(pos, newState, flags);
        }
        return false;
    }

    
    public boolean destroyBlock(int x, int y, int z, boolean dropItems) {
        return level.destroyBlock(new BlockPos(x, y, z), dropItems);
    }

    // ================================================================
    // Tile entity management
    // ================================================================

    
    public void setBlockTileEntity(int x, int y, int z, TileEntity tileEntity) {
        // TODO: bridge btw.modern.TileEntity to modern BlockEntity
    }

    
    public void removeBlockTileEntity(int x, int y, int z) {
        // TODO: implement
    }

    // ================================================================
    // Notifications
    // ================================================================

    
    public void notifyBlockChange(int x, int y, int z, int blockID) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, 3);
    }

    
    public void notifyBlocksOfNeighborChange(int x, int y, int z, int blockID) {
        net.minecraft.world.level.block.Block modernBlock = ProxyRegistry.getModernBlock(blockID);
        if (modernBlock != null) {
            level.updateNeighborsAt(new BlockPos(x, y, z), modernBlock);
        }
    }

    
    public void notifyBlockOfNeighborChange(int x, int y, int z, int blockID) {
        BlockPos pos = new BlockPos(x, y, z);
        net.minecraft.world.level.block.Block sourceBlock = ProxyRegistry.getModernBlock(blockID);
        if (sourceBlock != null) {
            level.getBlockState(pos).neighborChanged(level, pos, sourceBlock, pos, false);
        }
    }

    
    public void markBlockForUpdate(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, 3);
    }

    
    public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {
        // Render updates are client-side; on server this is a no-op
    }

    // ================================================================
    // Scheduled updates
    // ================================================================

    
    public void scheduleBlockUpdate(int x, int y, int z, int blockID, int delay) {
        BlockPos pos = new BlockPos(x, y, z);
        net.minecraft.world.level.block.Block modernBlock = ProxyRegistry.getModernBlock(blockID);
        if (modernBlock != null) {
            level.scheduleTick(pos, modernBlock, delay);
        }
    }

    
    public void func_82740_a(int x, int y, int z, int blockID, int delay, int priority) {
        // Same as scheduleBlockUpdate with priority (ignored for simplicity)
        scheduleBlockUpdate(x, y, z, blockID, delay);
    }

    // ================================================================
    // Light methods
    // ================================================================

    
    public boolean canBlockSeeTheSky(int x, int y, int z) {
        return level.canSeeSky(new BlockPos(x, y, z));
    }

    
    public int getFullBlockLightValue(int x, int y, int z) {
        return level.getMaxLocalRawBrightness(new BlockPos(x, y, z));
    }

    
    public int getBlockLightValue(int x, int y, int z) {
        return level.getMaxLocalRawBrightness(new BlockPos(x, y, z));
    }

    
    public int getSavedLightValue(EnumSkyBlock enumSkyBlock, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        if (enumSkyBlock == EnumSkyBlock.Sky) {
            return level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos);
        } else {
            return level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos);
        }
    }

    
    public void setLightValue(EnumSkyBlock enumSkyBlock, int x, int y, int z, int lightValue) {
        // Modern MC handles lighting internally; this is largely a no-op on the server.
    }

    // ================================================================
    // Height methods
    // ================================================================

    
    public int getHeightValue(int x, int z) {
        return level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
    }

    
    public int getTopSolidOrLiquidBlock(int x, int z) {
        return level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
    }

    // ================================================================
    // Chunk methods
    // ================================================================

    
    public boolean blockExists(int x, int y, int z) {
        return level.isLoaded(new BlockPos(x, y, z));
    }

    
    public boolean doChunksNearChunkExist(int x, int y, int z, int range) {
        return checkChunksExist(x - range, y - range, z - range,
                x + range, y + range, z + range);
    }

    
    public boolean checkChunksExist(int x1, int y1, int z1, int x2, int y2, int z2) {
        // Check if chunks covering the region are loaded
        int cx1 = x1 >> 4;
        int cz1 = z1 >> 4;
        int cx2 = x2 >> 4;
        int cz2 = z2 >> 4;
        for (int cx = cx1; cx <= cx2; cx++) {
            for (int cz = cz1; cz <= cz2; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    return false;
                }
            }
        }
        return true;
    }

    
    public boolean chunkExists(int chunkX, int chunkZ) {
        return level.hasChunk(chunkX, chunkZ);
    }

    // ================================================================
    // Biome
    // ================================================================

    
    public BiomeGenBase getBiomeGenForCoords(int x, int z) {
        // TODO: bridge modern biome to btw.modern.BiomeGenBase
        return null;
    }

    
    public WorldChunkManager getWorldChunkManager() {
        // TODO: bridge
        return null;
    }

    // ================================================================
    // Entity methods
    // ================================================================

    /**
     * Maps FC entity instances to their Forge proxy counterpart so we
     * can look up the proxy for removal, position sync, etc.
     */
    private final Map<btw.modern.Entity, net.minecraft.world.entity.Entity> fcToForgeEntity =
            new WeakHashMap<>();

    
    public boolean spawnEntityInWorld(btw.modern.Entity entity) {
        if (entity == null) return false;
        try {
            net.minecraft.world.entity.Entity proxy =
                    EntityProxyFactory.createProxy(entity, level);
            if (proxy == null) {
                LOGGER.debug("EntityProxyFactory returned null for {}",
                        entity.getClass().getSimpleName());
                return false;
            }
            fcToForgeEntity.put(entity, proxy);
            return level.addFreshEntity(proxy);
        } catch (Exception e) {
            LOGGER.debug("Failed to spawn proxy for {}: {}",
                    entity.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }

    
    public void removeEntity(btw.modern.Entity entity) {
        if (entity == null) return;
        net.minecraft.world.entity.Entity proxy = fcToForgeEntity.remove(entity);
        if (proxy != null) {
            proxy.discard();
        }
    }

    
    @SuppressWarnings("unchecked")
    public List getEntitiesWithinAABB(Class entityClass, AxisAlignedBB aabb) {
        net.minecraft.world.phys.AABB forgeAABB = toForgeAABB(aabb);
        List<net.minecraft.world.entity.Entity> forgeEntities =
                level.getEntities((net.minecraft.world.entity.Entity) null, forgeAABB,
                        e -> matchesFcClass(e, entityClass));
        // Return the FC entity instances rather than the proxies so FC
        // code can cast them to the expected FC types.
        List result = new ArrayList();
        for (net.minecraft.world.entity.Entity forgeEntity : forgeEntities) {
            btw.modern.Entity fc = extractFcEntity(forgeEntity);
            if (fc != null && entityClass.isInstance(fc)) {
                result.add(fc);
            }
        }
        return result;
    }

    
    public List getEntitiesWithinAABBExcludingEntity(btw.modern.Entity entity, AxisAlignedBB aabb) {
        net.minecraft.world.phys.AABB forgeAABB = toForgeAABB(aabb);
        net.minecraft.world.entity.Entity excludeProxy = fcToForgeEntity.get(entity);
        List<net.minecraft.world.entity.Entity> forgeEntities =
                level.getEntities(excludeProxy, forgeAABB, e -> true);
        List result = new ArrayList();
        for (net.minecraft.world.entity.Entity forgeEntity : forgeEntities) {
            btw.modern.Entity fc = extractFcEntity(forgeEntity);
            if (fc != null) {
                result.add(fc);
            }
        }
        return result;
    }

    
    public boolean checkNoEntityCollision(AxisAlignedBB aabb) {
        net.minecraft.world.phys.AABB forgeAABB = toForgeAABB(aabb);
        return level.getEntities((net.minecraft.world.entity.Entity) null,
                forgeAABB, e -> true).isEmpty();
    }

    
    public boolean canPlaceEntityOnSide(int blockID, int x, int y, int z,
                                        boolean skipEntities, int side,
                                        btw.modern.Entity entity, ItemStack stack) {
        if (skipEntities) return true;
        net.minecraft.world.phys.AABB checkBox = new net.minecraft.world.phys.AABB(
                x, y, z, x + 1, y + 1, z + 1);
        return level.getEntities((net.minecraft.world.entity.Entity) null,
                checkBox, e -> true).isEmpty();
    }

    // ------------------------------------------------------------------
    // Entity bridge helpers
    // ------------------------------------------------------------------

    /**
     * Converts an FC AxisAlignedBB to a Forge AABB.
     */
    private static net.minecraft.world.phys.AABB toForgeAABB(AxisAlignedBB aabb) {
        return new net.minecraft.world.phys.AABB(
                aabb.minX, aabb.minY, aabb.minZ,
                aabb.maxX, aabb.maxY, aabb.maxZ);
    }

    /**
     * Extracts the FC entity from a Forge proxy, or null if the entity
     * is not a BTW proxy.
     */
    private static btw.modern.Entity extractFcEntity(net.minecraft.world.entity.Entity forgeEntity) {
        if (forgeEntity instanceof ProxyMob pm) return pm.getFcEntity();
        if (forgeEntity instanceof ProxyAnimal pa) return pa.getFcEntity();
        if (forgeEntity instanceof ProxyPathfinderMob pp) return pp.getFcEntity();
        if (forgeEntity instanceof ProxyEntity pe) return pe.getFcEntity();
        return null;
    }

    /**
     * Checks whether a Forge entity wraps an FC entity that is an
     * instance of the given FC class.
     */
    private static boolean matchesFcClass(
            net.minecraft.world.entity.Entity forgeEntity, Class<?> fcClass) {
        btw.modern.Entity fc = extractFcEntity(forgeEntity);
        return fc != null && fcClass.isInstance(fc);
    }

    // ================================================================
    // Sound methods
    // ================================================================

    
    public void playSoundEffect(double x, double y, double z,
                                String sound, float volume, float pitch) {
        // Map legacy sound names to modern SoundEvents (simplified)
        try {
            level.playSound(null, x, y, z,
                    net.minecraft.sounds.SoundEvents.STONE_BREAK,
                    net.minecraft.sounds.SoundSource.BLOCKS,
                    volume, pitch);
        } catch (Exception e) {
            LOGGER.debug("Could not play sound '{}': {}", sound, e.getMessage());
        }
    }

    
    public void playSoundAtEntity(btw.modern.Entity entity, String sound,
                                  float volume, float pitch) {
        // TODO: bridge entity position
    }

    
    public void spawnParticle(String particle, double x, double y, double z,
                              double velocityX, double velocityY, double velocityZ) {
        // Particles are client-side; server can send packets but we skip for now
    }

    
    public void playAuxSFX(int effectID, int x, int y, int z, int data) {
        level.levelEvent(effectID, new BlockPos(x, y, z), data);
    }

    
    public void playAuxSFXAtEntity(EntityPlayer player, int effectID,
                                   int x, int y, int z, int data) {
        level.levelEvent(null, effectID, new BlockPos(x, y, z), data);
    }

    // ================================================================
    // Weather
    // ================================================================

    
    public boolean isRaining() {
        return level.isRaining();
    }

    
    public boolean isThundering() {
        return level.isThundering();
    }

    // ================================================================
    // Redstone
    // ================================================================

    
    public boolean isBlockGettingPowered(int x, int y, int z) {
        return level.hasNeighborSignal(new BlockPos(x, y, z));
    }

    
    public boolean isBlockIndirectlyGettingPowered(int x, int y, int z) {
        return level.getBestNeighborSignal(new BlockPos(x, y, z)) > 0;
    }

    // ================================================================
    // Solid top surface
    // ================================================================

    
    public boolean doesBlockHaveSolidTopSurface(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);
        return state.isFaceSturdy(level, pos, net.minecraft.core.Direction.UP);
    }

    // ================================================================
    // Player
    // ================================================================

    
    public boolean canMineBlock(EntityPlayer player, int x, int y, int z) {
        // TODO: bridge player permissions
        return true;
    }

    
    public EntityPlayer getClosestPlayer(double x, double y, double z, double range) {
        // TODO: bridge player entity
        return null;
    }

    // ================================================================
    // Explosion
    // ================================================================

    
    public Explosion createExplosion(btw.modern.Entity entity, double x, double y, double z,
                                     float strength, boolean isFlaming) {
        level.explode(null, x, y, z, strength,
                net.minecraft.world.level.Level.ExplosionInteraction.BLOCK);
        // Return a stub Explosion; FC code mostly ignores the return value
        return null;
    }

    
    public Explosion newExplosion(btw.modern.Entity entity, double x, double y, double z,
                                  float strength, boolean isFlaming, boolean isSmoking) {
        level.explode(null, x, y, z, strength,
                net.minecraft.world.level.Level.ExplosionInteraction.BLOCK);
        return null;
    }

    // ================================================================
    // Time
    // ================================================================

    
    public long getWorldTime() {
        return level.getDayTime();
    }

    
    public void setWorldTime(long time) {
        level.setDayTime(time);
    }

    
    public long getTotalWorldTime() {
        return level.getGameTime();
    }

    
    public boolean isDaytime() {
        return level.isDay();
    }

    // ================================================================
    // Spawn point
    // ================================================================

    
    public ChunkCoordinates getSpawnPoint() {
        BlockPos sp = level.getSharedSpawnPos();
        return new ChunkCoordinates(sp.getX(), sp.getY(), sp.getZ());
    }

    // ================================================================
    // World height
    // ================================================================

    
    public int getHeight() {
        return level.getHeight();
    }

    
    public int getActualHeight() {
        return level.getHeight();
    }

    // ================================================================
    // Block events
    // ================================================================

    
    public void addBlockEvent(int x, int y, int z, int blockID, int eventID, int eventParam) {
        BlockPos pos = new BlockPos(x, y, z);
        net.minecraft.world.level.block.Block modernBlock = ProxyRegistry.getModernBlock(blockID);
        if (modernBlock != null) {
            level.blockEvent(pos, modernBlock, eventID, eventParam);
        }
    }

    // ================================================================
    // Game rules
    // ================================================================

    
    public GameRules getGameRules() {
        // TODO: bridge modern GameRules to btw.modern.GameRules
        return null;
    }
}
