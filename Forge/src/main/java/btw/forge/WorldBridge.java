package btw.forge;

import btw.modern.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.GameRules.BooleanValue;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
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
        // Eagerly initialise FC-only data holders so FC code never
        // encounters null when accessing these lists.
        this.m_MagneticPointList = new FCMagneticPointList();
        this.m_SpawnLocationList = new FCSpawnLocationList();
        this.m_LootingBeaconLocationList = new FCBeaconEffectLocationList();
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
        return ProxyRegistry.getBlockId(state.getBlock());
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
        BlockPos pos = new BlockPos(x, y, z);
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return null;
        return TileEntityBridge.getOrCreate(be, this);
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
        BlockPos pos = new BlockPos(x, y, z);
        if (tileEntity instanceof TileEntityBridge bridge) {
            // Re-use the real BlockEntity wrapped by the bridge
            level.setBlockEntity(bridge.getBlockEntity());
        } else if (tileEntity != null) {
            // FC code created a pure FC TileEntity (not backed by a real
            // BlockEntity).  We cannot place it directly into the modern
            // world since MC requires a real BlockEntity.  Log and skip.
            LOGGER.debug("setBlockTileEntity: cannot place pure FC TileEntity {} at {}",
                    tileEntity.getClass().getSimpleName(), pos);
        }
    }

    
    public void removeBlockTileEntity(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) {
            TileEntityBridge.uncache(be);
            level.removeBlockEntity(pos);
        }
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

    @Override
    public boolean isBlockTickScheduled(int x, int y, int z, int blockID) {
        BlockPos pos = new BlockPos(x, y, z);
        net.minecraft.world.level.block.Block modernBlock = ProxyRegistry.getModernBlock(blockID);
        if (modernBlock != null) {
            return level.getBlockTicks().hasScheduledTick(pos, modernBlock);
        }
        return false;
    }

    @Override
    public boolean IsUpdateScheduledForBlock(int i, int j, int k, int iBlockID) {
        return isBlockTickScheduled(i, j, k, iBlockID);
    }

    @Override
    public boolean IsUpdatePendingThisTickForBlock(int i, int j, int k, int iBlockID) {
        // MC 1.20.1 does not expose "pending this tick" easily; approximate
        // by checking if any tick is scheduled for the block.
        return isBlockTickScheduled(i, j, k, iBlockID);
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
        Holder<Biome> holder = level.getBiome(new BlockPos(x, 64, z));
        return mapBiomeToFc(holder);
    }

    /**
     * Maps a modern Biome holder to the closest FC BiomeGenBase static
     * instance.  Falls back to {@link BiomeGenBase#plains} if the biome
     * is unknown or FC biomes have not been initialised yet.
     */
    private static BiomeGenBase mapBiomeToFc(Holder<Biome> holder) {
        // Safety: if FC biomes were never initialised, return a non-null default
        if (BiomeGenBase.plains == null) {
            return new BiomeGenBase(1) {};
        }

        ResourceKey<Biome> key = holder.unwrapKey().orElse(null);
        if (key == null) return BiomeGenBase.plains;

        ResourceLocation loc = key.location();
        String path = loc.getPath(); // e.g. "plains", "desert", "frozen_ocean"

        return switch (path) {
            case "ocean", "deep_ocean",
                 "deep_lukewarm_ocean", "deep_cold_ocean",
                 "deep_frozen_ocean", "warm_ocean",
                 "lukewarm_ocean", "cold_ocean" -> BiomeGenBase.ocean;
            case "plains", "sunflower_plains",
                 "meadow" -> BiomeGenBase.plains;
            case "desert" -> BiomeGenBase.desert;
            case "windswept_hills", "windswept_gravelly_hills",
                 "windswept_forest", "stony_peaks" -> BiomeGenBase.extremeHills;
            case "forest", "flower_forest",
                 "birch_forest", "old_growth_birch_forest",
                 "dark_forest" -> BiomeGenBase.forest;
            case "taiga", "old_growth_pine_taiga",
                 "old_growth_spruce_taiga" -> BiomeGenBase.taiga;
            case "swamp", "mangrove_swamp" -> BiomeGenBase.swampland;
            case "river" -> BiomeGenBase.river;
            case "nether_wastes", "soul_sand_valley",
                 "crimson_forest", "warped_forest",
                 "basalt_deltas" -> BiomeGenBase.hell;
            case "the_end", "end_highlands",
                 "end_midlands", "end_barrens",
                 "small_end_islands", "the_void" -> BiomeGenBase.sky;
            case "frozen_ocean" -> BiomeGenBase.frozenOcean;
            case "frozen_river" -> BiomeGenBase.frozenRiver;
            case "snowy_plains", "snowy_slopes",
                 "frozen_peaks", "ice_spikes" -> BiomeGenBase.icePlains;
            case "snowy_taiga", "grove",
                 "jagged_peaks" -> BiomeGenBase.iceMountains;
            case "mushroom_fields" -> BiomeGenBase.mushroomIsland;
            case "beach", "stony_shore" -> BiomeGenBase.beach;
            case "jungle", "sparse_jungle",
                 "bamboo_jungle" -> BiomeGenBase.jungle;
            case "snowy_beach" -> BiomeGenBase.frozenOcean;
            case "savanna", "savanna_plateau",
                 "windswept_savanna" -> BiomeGenBase.plains;
            case "badlands", "eroded_badlands",
                 "wooded_badlands" -> BiomeGenBase.desert;
            case "cherry_grove" -> BiomeGenBase.forest;
            case "dripstone_caves", "lush_caves",
                 "deep_dark" -> BiomeGenBase.plains;
            default -> BiomeGenBase.plains;
        };
    }

    
    /**
     * Lazy-initialised WorldChunkManager bridge that delegates biome
     * lookups to the real MC level.
     */
    private WorldChunkManager chunkManagerBridge;

    public WorldChunkManager getWorldChunkManager() {
        if (chunkManagerBridge == null) {
            chunkManagerBridge = new WorldChunkManager() {
                @Override
                public BiomeGenBase getBiomeGenAt(int x, int z) {
                    return getBiomeGenForCoords(x, z);
                }
            };
        }
        return chunkManagerBridge;
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
        try {
            SoundEvent event = resolveSoundEvent(sound);
            if (event != null) {
                level.playSound(null, x, y, z, event,
                        SoundSource.BLOCKS, volume, pitch);
            }
        } catch (Exception e) {
            LOGGER.debug("Could not play sound '{}': {}", sound, e.getMessage());
        }
    }


    public void playSoundAtEntity(btw.modern.Entity entity, String sound,
                                  float volume, float pitch) {
        try {
            SoundEvent event = resolveSoundEvent(sound);
            if (event != null && entity != null) {
                // Use entity's FC position to play the sound at its location
                level.playSound(null, entity.posX, entity.posY, entity.posZ,
                        event, SoundSource.NEUTRAL, volume, pitch);
            }
        } catch (Exception e) {
            LOGGER.debug("Could not play sound '{}' at entity: {}",
                    sound, e.getMessage());
        }
    }

    @Override
    public void playSoundToNearExcept(EntityPlayer player, String sound,
                                      float volume, float pitch) {
        try {
            SoundEvent event = resolveSoundEvent(sound);
            if (event != null && player != null) {
                // Play sound to all nearby players EXCEPT the given player.
                // If the player is a PlayerBridge, use the real ServerPlayer
                // so level.playSound excludes them from hearing it.
                ServerPlayer excluded = null;
                if (player instanceof PlayerBridge pb) {
                    excluded = pb.getServerPlayer();
                }
                level.playSound(excluded, player.posX, player.posY, player.posZ,
                        event, SoundSource.PLAYERS, volume, pitch);
            }
        } catch (Exception e) {
            LOGGER.debug("Could not play sound '{}' near-except: {}",
                    sound, e.getMessage());
        }
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

    @Override
    public boolean IsRainingAtPos(int i, int j, int k) {
        return level.isRainingAt(new BlockPos(i, j, k));
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
        // Check player permissions via the real ServerPlayer if available
        if (player instanceof PlayerBridge pb) {
            ServerPlayer sp = pb.getServerPlayer();
            if (sp != null) {
                BlockPos pos = new BlockPos(x, y, z);
                if (sp.isSpectator()) return false;
                if (!sp.mayBuild()) return false;
                if (sp.server.isUnderSpawnProtection(level, pos, sp)) return false;
            }
            return true;
        }
        return true;
    }


    public EntityPlayer getClosestPlayer(double x, double y, double z, double range) {
        // Delegate to modern level -- range<0 means unlimited
        net.minecraft.world.entity.player.Player nearest =
                level.getNearestPlayer(x, y, z, range, false);
        if (nearest instanceof ServerPlayer sp) {
            return PlayerBridge.getOrCreate(sp);
        }
        return null;
    }

    @Override
    public EntityPlayer getClosestPlayerToEntity(btw.modern.Entity entity, double range) {
        if (entity == null) return null;
        return getClosestPlayer(entity.posX, entity.posY, entity.posZ, range);
    }

    @Override
    public EntityPlayer getClosestVulnerablePlayerToEntity(btw.modern.Entity entity, double range) {
        if (entity == null) return null;
        return getClosestVulnerablePlayer(entity.posX, entity.posY, entity.posZ, range);
    }

    @Override
    public EntityPlayer getClosestVulnerablePlayer(double x, double y, double z, double range) {
        // "Vulnerable" in FC means not in creative/spectator mode
        net.minecraft.world.entity.player.Player nearest =
                level.getNearestPlayer(x, y, z, range, true); // true = ignore creative
        if (nearest instanceof ServerPlayer sp) {
            // Also exclude spectators
            if (sp.isSpectator()) return null;
            return PlayerBridge.getOrCreate(sp);
        }
        return null;
    }

    // ================================================================
    // Entity state
    // ================================================================

    @Override
    public void setEntityState(btw.modern.Entity entity, byte state) {
        if (entity == null) return;
        net.minecraft.world.entity.Entity proxy = fcToForgeEntity.get(entity);
        if (proxy != null) {
            level.broadcastEntityEvent(proxy, state);
        }
    }

    // ================================================================
    // Explosion
    // ================================================================

    
    public Explosion createExplosion(btw.modern.Entity entity, double x, double y, double z,
                                     float strength, boolean isFlaming) {
        level.explode(null, x, y, z, strength,
                net.minecraft.world.level.Level.ExplosionInteraction.BLOCK);
        Explosion fcExplosion = new Explosion(this, entity, x, y, z, strength);
        fcExplosion.isFlaming = isFlaming;
        return fcExplosion;
    }


    public Explosion newExplosion(btw.modern.Entity entity, double x, double y, double z,
                                  float strength, boolean isFlaming, boolean isSmoking) {
        level.explode(null, x, y, z, strength,
                net.minecraft.world.level.Level.ExplosionInteraction.BLOCK);
        Explosion fcExplosion = new Explosion(this, entity, x, y, z, strength);
        fcExplosion.isFlaming = isFlaming;
        fcExplosion.isSmoking = isSmoking;
        return fcExplosion;
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

    /**
     * Lazy-initialised wrapper that delegates btw.modern.GameRules methods
     * to the real MC 1.20.1 GameRules stored in the ServerLevel.
     */
    private GameRules gameRulesBridge;


    public GameRules getGameRules() {
        if (gameRulesBridge == null) {
            gameRulesBridge = new GameRules() {
                @Override
                public String getGameRuleStringValue(String name) {
                    net.minecraft.world.level.GameRules.Key<? extends net.minecraft.world.level.GameRules.Value<?>> key =
                            findRuleKey(name);
                    if (key != null) {
                        return level.getGameRules().getRule(key).toString();
                    }
                    return "";
                }

                @Override
                public boolean getGameRuleBooleanValue(String name) {
                    net.minecraft.world.level.GameRules.Key<BooleanValue> key =
                            findBooleanRuleKey(name);
                    if (key != null) {
                        return level.getGameRules().getBoolean(key);
                    }
                    return false;
                }

                @Override
                public boolean hasRule(String name) {
                    return findRuleKey(name) != null;
                }

                /**
                 * Finds a GameRules key by name. MC 1.20.1 uses typed keys
                 * so we check the well-known set.
                 */
                private net.minecraft.world.level.GameRules.Key<? extends net.minecraft.world.level.GameRules.Value<?>> findRuleKey(String name) {
                    return switch (name) {
                        case "doFireTick" -> net.minecraft.world.level.GameRules.RULE_DOFIRETICK;
                        case "mobGriefing" -> net.minecraft.world.level.GameRules.RULE_MOBGRIEFING;
                        case "keepInventory" -> net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY;
                        case "doMobSpawning" -> net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING;
                        case "doMobLoot" -> net.minecraft.world.level.GameRules.RULE_DOMOBLOOT;
                        case "doTileDrops" -> net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS;
                        case "commandBlockOutput" -> net.minecraft.world.level.GameRules.RULE_COMMANDBLOCKOUTPUT;
                        case "naturalRegeneration" -> net.minecraft.world.level.GameRules.RULE_NATURAL_REGENERATION;
                        case "doDaylightCycle" -> net.minecraft.world.level.GameRules.RULE_DAYLIGHT;
                        // doWeatherCycle not in MC 1.5.2 FC, skip
                        case "showDeathMessages" -> net.minecraft.world.level.GameRules.RULE_SHOWDEATHMESSAGES;
                        default -> null;
                    };
                }

                @SuppressWarnings("unchecked")
                private net.minecraft.world.level.GameRules.Key<BooleanValue> findBooleanRuleKey(String name) {
                    var key = findRuleKey(name);
                    if (key != null) {
                        // All the rules mapped above are BooleanValue rules
                        return (net.minecraft.world.level.GameRules.Key<BooleanValue>) key;
                    }
                    return null;
                }
            };
        }
        return gameRulesBridge;
    }

    // ================================================================
    // FC-only data holders (magnetic points, spawn locations, beacons)
    // ================================================================

    /**
     * Initialises the FC-only list fields that FC code expects to be
     * non-null on a World instance.  Called lazily / from the constructor.
     */
    private void initFcDataHolders() {
        if (m_MagneticPointList == null) {
            m_MagneticPointList = new FCMagneticPointList();
        }
        if (m_SpawnLocationList == null) {
            m_SpawnLocationList = new FCSpawnLocationList();
        }
        if (m_LootingBeaconLocationList == null) {
            m_LootingBeaconLocationList = new FCBeaconEffectLocationList();
        }
    }

    @Override
    public FCMagneticPointList GetMagneticPointList() {
        initFcDataHolders();
        return m_MagneticPointList;
    }

    @Override
    public FCSpawnLocationList GetSpawnLocationList() {
        initFcDataHolders();
        return m_SpawnLocationList;
    }

    @Override
    public FCBeaconEffectLocationList GetLootingBeaconLocationList() {
        initFcDataHolders();
        return m_LootingBeaconLocationList;
    }

    // ================================================================
    // Tile entity collection
    // ================================================================

    @Override
    @SuppressWarnings("unchecked")
    public List getAllTileEntityInBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        List result = new ArrayList();
        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    BlockEntity be = level.getBlockEntity(new BlockPos(x, y, z));
                    if (be != null) {
                        result.add(TileEntityBridge.getOrCreate(be, this));
                    }
                }
            }
        }
        return result;
    }

    // ================================================================
    // Celestial angle / sun brightness
    // ================================================================

    @Override
    public float ComputeOverworldSunBrightnessWithMoonPhases() {
        // Compute sun angle from time-of-day
        float celestialAngle = level.getTimeOfDay(0);

        // Sun brightness curve: 1.0 at noon, 0.0 at midnight, with
        // smooth transitions at dawn/dusk.
        float sunBrightness;
        if (celestialAngle < 0.25F) {
            // Night to sunrise (0.0 = midnight, 0.25 = sunrise)
            sunBrightness = 0.0F;
        } else if (celestialAngle < 0.27F) {
            // Dawn transition
            sunBrightness = (celestialAngle - 0.25F) / 0.02F;
        } else if (celestialAngle < 0.73F) {
            // Daytime
            sunBrightness = 1.0F;
        } else if (celestialAngle < 0.75F) {
            // Dusk transition
            sunBrightness = 1.0F - (celestialAngle - 0.73F) / 0.02F;
        } else {
            // Night
            sunBrightness = 0.0F;
        }

        // Modulate by moon phase (0=full, 4=new). Full moon adds some
        // night brightness; new moon means total darkness.
        int moonPhase = level.getMoonPhase();
        float moonBrightness = switch (moonPhase) {
            case 0 -> 1.0F;   // full moon
            case 1, 7 -> 0.75F;
            case 2, 6 -> 0.5F;
            case 3, 5 -> 0.25F;
            case 4 -> 0.0F;   // new moon
            default -> 0.5F;
        };

        // When the sun is up, moon has no effect. At night, use moon
        // brightness scaled down.
        if (sunBrightness > 0.0F) {
            return sunBrightness;
        }
        return moonBrightness * 0.25F;
    }

    // ================================================================
    // Sound resolution helper
    // ================================================================

    /**
     * Resolves a legacy FC sound name (e.g. "random.drink") to a modern
     * SoundEvent.  Uses {@link SoundEvent#createVariableRangeEvent} with
     * the FC name as a ResourceLocation under the "minecraft" namespace,
     * converting dots to underscores so "random.drink" becomes
     * "minecraft:random/drink" (matching vanilla's path convention where
     * applicable).  Returns null if the name is blank.
     */
    private static SoundEvent resolveSoundEvent(String fcSoundName) {
        if (fcSoundName == null || fcSoundName.isEmpty()) return null;
        try {
            // FC uses dot-separated names like "random.drink", "mob.zombie.say".
            // Modern MC uses slash-separated ResourceLocation paths.
            String path = fcSoundName.replace('.', '/');
            ResourceLocation loc = new ResourceLocation("minecraft", path);
            return SoundEvent.createVariableRangeEvent(loc);
        } catch (Exception e) {
            // Invalid resource location or other issue — silently ignore
            return null;
        }
    }
}
