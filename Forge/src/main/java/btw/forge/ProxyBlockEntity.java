package btw.forge;

import btw.modern.TileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A Forge 1.20.1 {@link BlockEntity} that wraps an FC {@link TileEntity}.
 *
 * <p>FC blocks that extend {@code BlockContainer} (e.g. campfires, hoppers,
 * furnaces, cauldrons) create their own FC tile entity via
 * {@code createNewTileEntity(World)}.  This class provides the MC BlockEntity
 * shell so that the FC tile entity is properly persisted, ticked, and
 * accessible via {@code world.getBlockTileEntity(x, y, z)}.</p>
 *
 * <p>A single {@link BlockEntityType} is shared by all ProxyBlockEntities
 * regardless of which FC block created them.  The FC tile entity class is
 * determined at runtime by the FC block at the position.</p>
 */
public class ProxyBlockEntity extends BlockEntity {

    private static final Logger LOGGER = LogManager.getLogger("BTW-ProxyBlockEntity");

    /**
     * The shared BlockEntityType for all ProxyBlockEntities.
     * Registered during the BLOCK_ENTITY_TYPES registry event.
     */
    public static BlockEntityType<ProxyBlockEntity> TYPE;

    /**
     * The wrapped FC tile entity. May be null briefly during load
     * (before {@link #load(CompoundTag)} is called and the FC tile entity
     * is recreated from NBT).
     */
    private TileEntity fcTileEntity;

    /**
     * The legacy FC block ID, saved to NBT so we can recreate the correct
     * FC tile entity type on load.
     */
    private int fcBlockId = -1;

    public ProxyBlockEntity(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
        if (TYPE != null) {
            // Verify the type in the registry matches
            var regKey = net.minecraftforge.registries.ForgeRegistries.BLOCK_ENTITY_TYPES.getKey(TYPE);
            var regType = regKey != null ? net.minecraftforge.registries.ForgeRegistries.BLOCK_ENTITY_TYPES.getValue(regKey) : null;
            if (regType != TYPE) {
                LOGGER.warn("ProxyBlockEntity TYPE mismatch! TYPE={} regType={} key={}",
                        System.identityHashCode(TYPE),
                        regType != null ? System.identityHashCode(regType) : "null",
                        regKey);
            }
        }
    }

    /**
     * Creates a ProxyBlockEntity with the given FC tile entity pre-set.
     */
    public ProxyBlockEntity(BlockPos pos, BlockState state, TileEntity fcTe, int legacyBlockId) {
        super(TYPE, pos, state);
        this.fcTileEntity = fcTe;
        this.fcBlockId = legacyBlockId;
        initFcTileEntity();
        LOGGER.info("ProxyBlockEntity created at {} fcTe={} blockId={}", pos, fcTe != null ? fcTe.getClass().getSimpleName() : "null", legacyBlockId);
    }

    /**
     * Returns the wrapped FC tile entity, or null if not yet initialised.
     */
    public TileEntity getFcTileEntity() {
        return fcTileEntity;
    }

    /**
     * Sets the wrapped FC tile entity. Called when FC code uses
     * {@code world.setBlockTileEntity(x, y, z, te)} to replace the
     * tile entity at this position.
     */
    public void setFcTileEntity(TileEntity te) {
        this.fcTileEntity = te;
        if (te != null) {
            initFcTileEntity();
        }
        setChanged();
    }

    /**
     * Initialises the FC tile entity's position and world references
     * to match this BlockEntity's position.
     */
    private void initFcTileEntity() {
        if (fcTileEntity == null) return;
        fcTileEntity.xCoord = getBlockPos().getX();
        fcTileEntity.yCoord = getBlockPos().getY();
        fcTileEntity.zCoord = getBlockPos().getZ();
        if (level != null) {
            if (level instanceof net.minecraft.server.level.ServerLevel sl) {
                fcTileEntity.setWorldObj(WorldBridge.getOrCreate(sl));
            } else {
                // Client level — create lightweight world wrapper for particles
                fcTileEntity.setWorldObj(createClientWorld(level));
            }
        }
        fcTileEntity.validate();
    }

    // ------------------------------------------------------------------
    // Level set override — update FC tile entity's world reference
    // ------------------------------------------------------------------

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (fcTileEntity != null) {
            if (level instanceof net.minecraft.server.level.ServerLevel sl) {
                fcTileEntity.setWorldObj(WorldBridge.getOrCreate(sl));
            } else if (level != null) {
                // Client level — create a lightweight World wrapper for particle spawning
                fcTileEntity.setWorldObj(createClientWorld(level));
            }
        }
    }

    /**
     * Copies all entries from an FC NBTTagCompound to a ForgeNBTCompound.
     * Uses reflection to access the FC tag's internal Map.
     */
    @SuppressWarnings("unchecked")
    private static void copyFcTagToForge(btw.modern.NBTTagCompound fcTag, ForgeNBTCompound target) {
        try {
            java.lang.reflect.Field mapField = btw.modern.NBTTagCompound.class.getDeclaredField("tagMap");
            mapField.setAccessible(true);
            java.util.Map<String, Object> map = (java.util.Map<String, Object>) mapField.get(fcTag);
            if (map != null) {
                for (java.util.Map.Entry<String, Object> entry : map.entrySet()) {
                    String key = entry.getKey();
                    Object val = entry.getValue();
                    if (val instanceof Boolean b) target.setBoolean(key, b);
                    else if (val instanceof Integer i) target.setInteger(key, i);
                    else if (val instanceof Long l) target.setLong(key, l);
                    else if (val instanceof Float f) target.setFloat(key, f);
                    else if (val instanceof Double d) target.setDouble(key, d);
                    else if (val instanceof String s) target.setString(key, s);
                    else if (val instanceof Short sh) target.setShort(key, sh);
                    else if (val instanceof Byte by) target.setByte(key, by);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to copy FC tag: {}", e.getMessage());
        }
    }

    /** Creates a minimal client-side World for FC tile entity particle spawning. */
    private static btw.modern.World createClientWorld(Level level) {
        return new btw.modern.World() {
            { this.isRemote = true; this.rand = new java.util.Random(); }
            public int getBlockId(int x, int y, int z) { return ProxyRegistry.getBlockId(level.getBlockState(new net.minecraft.core.BlockPos(x,y,z)).getBlock()); }
            public int getBlockMetadata(int x, int y, int z) {
                net.minecraft.world.level.block.state.BlockState s = level.getBlockState(new net.minecraft.core.BlockPos(x,y,z));
                return s.hasProperty(ProxyBlock.META) ? s.getValue(ProxyBlock.META) : 0;
            }
            public btw.modern.Material getBlockMaterial(int x, int y, int z) { return btw.modern.Material.air; }
            public boolean isAirBlock(int x, int y, int z) { return level.getBlockState(new net.minecraft.core.BlockPos(x,y,z)).isAir(); }
            public boolean isBlockNormalCube(int x, int y, int z) { return false; }
            public void spawnParticle(String name, double x, double y, double z, double vx, double vy, double vz) {
                net.minecraft.core.particles.ParticleOptions p = ProxyBlock.mapParticle(name);
                if (p != null) level.addParticle(p, x, y, z, vx, vy, vz);
            }
            public boolean setBlock(int x, int y, int z, int id, int meta, int flags) { return false; }
            public btw.modern.TileEntity getBlockTileEntity(int x, int y, int z) { return null; }
            public boolean canPlaceEntityOnSide(int id, int x, int y, int z, boolean b, int s, btw.modern.Entity e, btw.modern.ItemStack st) { return false; }
            public java.util.List getEntitiesWithinAABB(Class c, btw.modern.AxisAlignedBB bb) { return java.util.Collections.emptyList(); }
            public java.util.List getEntitiesWithinAABBExcludingEntity(btw.modern.Entity e, btw.modern.AxisAlignedBB bb) { return java.util.Collections.emptyList(); }
            public void scheduleBlockUpdate(int x, int y, int z, int id, int delay) {}
            public void notifyBlockChange(int x, int y, int z, int id) {}
            public boolean spawnEntityInWorld(btw.modern.Entity e) { return false; }
            public btw.modern.BiomeGenBase getBiomeGenForCoords(int x, int z) { return btw.modern.BiomeGenBase.plains; }
            public void playSoundEffect(double x, double y, double z, String s, float v, float p) {}
            public void playSoundAtEntity(btw.modern.Entity e, String s, float v, float p) {}
            public void playAuxSFX(int id, int x, int y, int z, int data) {}
            public void playAuxSFXAtEntity(btw.modern.EntityPlayer p, int id, int x, int y, int z, int data) {}
            public boolean isRaining() { return false; }
            public boolean isBlockGettingPowered(int x, int y, int z) { return false; }
            public boolean isBlockIndirectlyGettingPowered(int x, int y, int z) { return false; }
            public btw.modern.IChunkProvider createChunkProvider() { return null; }
            public boolean destroyBlock(int x, int y, int z, boolean drop) { return false; }
            public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {}
            public boolean canBlockSeeTheSky(int x, int y, int z) { return level.canSeeSky(new net.minecraft.core.BlockPos(x,y,z)); }
            public btw.modern.WorldChunkManager getWorldChunkManager() { return null; }
            public boolean setBlockToAir(int x, int y, int z) { return false; }
            public boolean setBlockMetadata(int x, int y, int z, int meta, int flags) { return false; }
            public boolean canMineBlock(btw.modern.EntityPlayer p, int x, int y, int z) { return true; }
            public boolean doesBlockHaveSolidTopSurface(int x, int y, int z) { return true; }
            public int getSavedLightValue(btw.modern.EnumSkyBlock type, int x, int y, int z) { return 15; }
            public int getFullBlockLightValue(int x, int y, int z) { return 15; }
            public boolean checkChunksExist(int x1, int y1, int z1, int x2, int y2, int z2) { return true; }
            public boolean checkNoEntityCollision(btw.modern.AxisAlignedBB bb) { return true; }
        };
    }

    // ------------------------------------------------------------------
    // NBT persistence
    // ------------------------------------------------------------------

    private static final String TAG_FC_BLOCK_ID = "fcBlockId";
    private static final String TAG_FC_DATA = "fcData";

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(TAG_FC_BLOCK_ID, fcBlockId);
        if (fcTileEntity != null) {
            CompoundTag fcData = new CompoundTag();
            ForgeNBTCompound wrapper = new ForgeNBTCompound(fcData);
            try {
                fcTileEntity.writeToNBT(wrapper);
            } catch (Exception e) {
                LOGGER.debug("Failed to save FC tile entity data for block {}: {}",
                        fcBlockId, e.getMessage());
            }
            if (fcBlockId >= 1013 && fcBlockId <= 1016) {
                LOGGER.info("Saving campfire FC data: keys={}", fcData.getAllKeys());
            }
            tag.put(TAG_FC_DATA, fcData);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        fcBlockId = tag.getInt(TAG_FC_BLOCK_ID);

        // Recreate the FC tile entity from the FC block
        if (fcBlockId > 0 && fcBlockId < btw.modern.Block.blocksList.length) {
            btw.modern.Block fcBlock = btw.modern.Block.blocksList[fcBlockId];
            if (fcBlock != null) {
                try {
                    fcTileEntity = fcBlock.createNewTileEntity(null);
                } catch (Exception e) {
                    LOGGER.debug("Failed to create FC tile entity for block {}: {}",
                            fcBlockId, e.getMessage());
                }
            }
        }

        // Load saved FC data into the tile entity
        if (fcTileEntity != null && tag.contains(TAG_FC_DATA)) {
            CompoundTag fcData = tag.getCompound(TAG_FC_DATA);
            if (fcBlockId >= 1013 && fcBlockId <= 1016) {
                LOGGER.info("Loading campfire FC data: keys={} hasCookStack={}",
                        fcData.getAllKeys(), fcData.contains("fcCookStack"));
                if (fcData.contains("fcCookStack")) {
                    CompoundTag cookTag = fcData.getCompound("fcCookStack");
                    LOGGER.info("  fcCookStack contents: id={} Count={} Damage={} keys={}",
                            cookTag.getShort("id"), cookTag.getByte("Count"),
                            cookTag.getShort("Damage"), cookTag.getAllKeys());
                }
            }
            ForgeNBTCompound wrapper = new ForgeNBTCompound(fcData);
            try {
                fcTileEntity.readFromNBT(wrapper);
            } catch (Exception e) {
                LOGGER.debug("Failed to load FC tile entity data for block {}: {}",
                        fcBlockId, e.getMessage());
            }
        }

        initFcTileEntity();
    }

    // ------------------------------------------------------------------
    // Client sync — sends FC tile entity data to the client
    // ------------------------------------------------------------------

    /**
     * Called when a chunk is sent to the client. Returns the NBT tag that
     * will be sent alongside the chunk data. The client receives this in
     * {@link #handleUpdateTag}.
     */
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        // Also include FC's sync-specific data (m_bIsCooking, etc.)
        // FC tile entities override getDescriptionPacket() to create a
        // Packet132TileEntityData with SYNC data (e.g., cooking state).
        // This is separate from writeToNBT which handles persistence.
        if (fcTileEntity != null) {
            try {
                btw.modern.Packet pkt = fcTileEntity.getDescriptionPacket();
                if (pkt instanceof btw.modern.Packet132TileEntityData teData
                        && teData.customParam1 != null) {
                    // Write FC sync data into a ForgeNBTCompound so it becomes
                    // an MC CompoundTag that we can include in the update tag.
                    CompoundTag syncTag = new CompoundTag();
                    ForgeNBTCompound syncWrapper = new ForgeNBTCompound(syncTag);
                    // Copy from FC tag to ForgeNBTCompound using FC's own API
                    btw.modern.NBTTagCompound fcTag = teData.customParam1;
                    copyFcTagToForge(fcTag, syncWrapper);
                    tag.put("fcSync", syncTag);
                }
            } catch (Exception ignored) {}
        }
        return tag;
    }

    /**
     * Called when the block entity needs to sync to watching clients
     * (e.g., after {@link #setChanged()} + {@code level.sendBlockUpdated()}).
     * Returns a packet containing the FC tile entity's full NBT state.
     */
    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * Called on the CLIENT when a block entity update packet arrives.
     * Loads the FC tile entity data from the received NBT.
     */
    @Override
    public void onDataPacket(net.minecraft.network.Connection net,
                              net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            load(tag);
            applySyncData(tag);
        }
    }

    /**
     * Called on the CLIENT when chunk data arrives with the update tag.
     */
    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
        applySyncData(tag);
    }

    /**
     * Applies FC's sync-specific data (from getDescriptionPacket/readNBTFromPacket).
     * This handles fields like m_bIsCooking that are NOT in writeToNBT but ARE
     * synced via FC's custom packet system.
     */
    private void applySyncData(CompoundTag tag) {
        if (fcTileEntity != null && tag.contains("fcSync")) {
            try {
                CompoundTag syncTag = tag.getCompound("fcSync");
                ForgeNBTCompound wrapper = new ForgeNBTCompound(syncTag);
                // FC tile entities that implement FCITileEntityDataPacketHandler
                // have a readNBTFromPacket method for sync-specific data.
                // Use reflection since the interface is in the FC runtime jar.
                java.lang.reflect.Method readMethod = null;
                for (java.lang.reflect.Method m : fcTileEntity.getClass().getMethods()) {
                    if (m.getName().equals("readNBTFromPacket")
                            && m.getParameterCount() == 1) {
                        readMethod = m;
                        break;
                    }
                }
                if (readMethod != null) {
                    readMethod.invoke(fcTileEntity, wrapper);
                }
            } catch (Exception ignored) {}
        }
    }

    /**
     * Marks the tile entity as needing a sync to clients. Call this when
     * FC tile entity state changes that clients need to see (cook level,
     * cooking state, etc.).
     */
    public void syncToClients() {
        if (level != null && !level.isClientSide()) {
            setChanged();
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    // ------------------------------------------------------------------
    // Ticking
    // ------------------------------------------------------------------

    /**
     * Static tick method used by the block entity ticker.
     * Called every server tick for ProxyBlockEntities whose FC block
     * has a tile entity.
     */
    private int syncCounter = 0;
    private boolean initialTickScheduled = false;

    private int turntableLogCounter = 0;

    public static void tick(Level level, BlockPos pos, BlockState state, ProxyBlockEntity be) {
        if (be.fcTileEntity != null) {
            // Always sync coordinates and world — they may have been wrong
            // during initial construction (e.g., load() before setLevel())
            be.fcTileEntity.xCoord = pos.getX();
            be.fcTileEntity.yCoord = pos.getY();
            be.fcTileEntity.zCoord = pos.getZ();
            if (be.fcTileEntity.worldObj == null && level != null) {
                if (level instanceof net.minecraft.server.level.ServerLevel sl) {
                    be.fcTileEntity.setWorldObj(WorldBridge.getOrCreate(sl));
                } else {
                    be.fcTileEntity.setWorldObj(createClientWorld(level));
                }
            }

            // On first tick, schedule a block update so the FC block's updateTick
            // fires. This is critical for blocks loaded from saved worlds — their
            // onBlockAdded never fires, so mechanical power detection, fire checks,
            // etc. never initialize without this kick.
            if (!be.initialTickScheduled && level instanceof net.minecraft.server.level.ServerLevel sl) {
                be.initialTickScheduled = true;
                if (state.getBlock() instanceof ProxyBlock pb) {
                    int legacyId = pb.getLegacyId();
                    btw.modern.Block fcBlock = btw.modern.Block.blocksList[legacyId];
                    if (fcBlock != null) {
                        int tickRate = fcBlock.tickRate(null);
                        if (tickRate <= 0) tickRate = 1;
                        sl.scheduleTick(pos, pb, tickRate);
                    }
                }
            }

            // Turntable diagnostics: log once per second with internal state
            boolean isTurntable = be.fcTileEntity.getClass().getSimpleName().contains("Turntable");
            if (isTurntable && !level.isClientSide()) {
                be.turntableLogCounter++;
                if (be.turntableLogCounter >= 20) {
                    be.turntableLogCounter = 0;
                    int meta = state.hasProperty(ProxyBlock.META) ? state.getValue(ProxyBlock.META) : -1;
                    // Read internal fields via reflection
                    int rotCount = -1;
                    try {
                        var f = be.fcTileEntity.getClass().getDeclaredField("m_iRotationTickCount");
                        f.setAccessible(true);
                        rotCount = f.getInt(be.fcTileEntity);
                    } catch (Exception ignored) {}
                    // Check what IsBlockMechanicalOn returns via worldObj
                    boolean mechOn = (meta & 1) > 0;
                    // Log all 4 side blocks with their MC identity
                    if (be.turntableLogCounter == 0) { // only log first time after counter reset
                        for (int side = 2; side <= 5; side++) {
                            int dx = btw.modern.Facing.offsetsXForSide[side];
                            int dz = btw.modern.Facing.offsetsZForSide[side];
                            net.minecraft.core.BlockPos sidePos = pos.above().offset(dx, 0, dz);
                            net.minecraft.world.level.block.state.BlockState sideState = level.getBlockState(sidePos);
                            int sideId = ProxyRegistry.getBlockId(sideState.getBlock());
                            String mcName = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(sideState.getBlock()) + "";
                            btw.modern.Block fcSide = sideId > 0 && sideId < btw.modern.Block.blocksList.length ? btw.modern.Block.blocksList[sideId] : null;
                            LOGGER.info("[SIDE-BLOCK] side={} pos={} mcBlock={} fcId={} fcClass={}",
                                    side, sidePos, mcName, sideId,
                                    fcSide != null ? fcSide.getClass().getSimpleName() : "null");
                        }
                    }
                    // Check block above — show BOTH FC id and MC block name
                    int aboveId = 0;
                    String aboveBlock = "none";
                    String mcBlockName = "?";
                    if (be.fcTileEntity.worldObj != null) {
                        aboveId = be.fcTileEntity.worldObj.getBlockId(pos.getX(), pos.getY() + 1, pos.getZ());
                        btw.modern.Block aboveFc = aboveId > 0 && aboveId < btw.modern.Block.blocksList.length ? btw.modern.Block.blocksList[aboveId] : null;
                        aboveBlock = aboveFc != null ? aboveFc.getClass().getSimpleName() : "null(id=" + aboveId + ")";
                    }
                    // Read MC block directly
                    net.minecraft.core.BlockPos abovePos = pos.above();
                    net.minecraft.world.level.block.state.BlockState aboveState = level.getBlockState(abovePos);
                    mcBlockName = aboveState.getBlock().getClass().getSimpleName() + "=" + net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(aboveState.getBlock());
                    LOGGER.info("[TURNTABLE-TICK] pos={} meta={} mechOn={} rotCount={} fcId={} fcBlock={} mcBlock={}",
                            pos, meta, mechOn, rotCount, aboveId, aboveBlock, mcBlockName);
                }
            }

            try {
                be.fcTileEntity.updateEntity();
            } catch (Exception e) {
                // Log with stack trace so we can diagnose turntable/mechanical failures
                if (isTurntable) {
                    LOGGER.error("Turntable tile entity tick FAILED at {}: {}", pos, e.getMessage(), e);
                } else {
                    LOGGER.debug("FC tile entity tick failed at {}: {}",
                            pos, e.getMessage());
                }
            }

            // Sync tile entity data to clients every 20 ticks (1 second)
            // so cook levels, burning state, etc. are visible on the client
            if (!level.isClientSide()) {
                be.syncCounter++;
                if (be.syncCounter >= 20) {
                    be.syncCounter = 0;
                    be.syncToClients();
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (fcTileEntity != null) {
            fcTileEntity.invalidate();
        }
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (fcTileEntity != null) {
            fcTileEntity.validate();
        }
    }
}
