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
     * is recreated from NBT). Volatile: on the client, chunk-mesh threads read it
     * (FCBakedModel live capture) while {@link #load} replaces it on the main thread.
     */
    private volatile TileEntity fcTileEntity;

    /** Client-side FC world wrapper for {@link #clientFcWorldLevel}, built once instead of on every
     *  data packet (see {@link #clientWorldFor}). Main thread only. */
    private btw.modern.World clientFcWorld;
    private Level clientFcWorldLevel;

    /** Client: copy of the fcSync tag last applied, to re-mesh only when FC's description data
     *  changes (see {@link #onDataPacket}). Main thread only. */
    private CompoundTag lastClientSync;

    /**
     * The legacy FC block ID, saved to NBT so we can recreate the correct
     * FC tile entity type on load.
     */
    private int fcBlockId = -1;

    public ProxyBlockEntity(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }

    /**
     * Creates a ProxyBlockEntity with the given FC tile entity pre-set.
     */
    public ProxyBlockEntity(BlockPos pos, BlockState state, TileEntity fcTe, int legacyBlockId) {
        super(TYPE, pos, state);
        this.fcTileEntity = fcTe;
        this.fcBlockId = legacyBlockId;
        initFcTileEntity();
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
        initFcTileEntity(fcTileEntity);
    }

    private void initFcTileEntity(TileEntity te) {
        if (te == null) return;
        te.xCoord = getBlockPos().getX();
        te.yCoord = getBlockPos().getY();
        te.zCoord = getBlockPos().getZ();
        if (level != null) {
            if (level instanceof net.minecraft.server.level.ServerLevel sl) {
                te.setWorldObj(WorldBridge.getOrCreate(sl));
            } else {
                // Client level — lightweight world wrapper for particles
                te.setWorldObj(clientWorldFor(level));
            }
        }
        te.validate();
    }

    /** The client-side FC world wrapper for {@code level}, created on first use per level. */
    private btw.modern.World clientWorldFor(Level level) {
        btw.modern.World world = clientFcWorld;
        if (world == null || clientFcWorldLevel != level) {
            world = createClientWorld(level);
            clientFcWorld = world;
            clientFcWorldLevel = level;
        }
        return world;
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
                // Client level — a lightweight World wrapper for particle spawning
                fcTileEntity.setWorldObj(clientWorldFor(level));
            }
        }
    }

    /**
     * Copies all entries from an FC NBTTagCompound to a ForgeNBTCompound.
     * Uses reflection to access the FC tag's internal Map.
     */
    /**
     * Deep-copies an FC NBTTagCompound into a ForgeNBTCompound, including
     * nested compounds and lists. Uses FC's own writeToNBT/readFromNBT
     * round-trip to ensure all data types are preserved.
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
                    else if (val instanceof btw.modern.NBTTagCompound compound) {
                        // Nested compound tag — recursively copy
                        CompoundTag nested = new CompoundTag();
                        ForgeNBTCompound nestedWrapper = new ForgeNBTCompound(nested);
                        copyFcTagToForge(compound, nestedWrapper);
                        target.setCompoundTag(key, nestedWrapper);
                    }
                    else if (val instanceof btw.modern.NBTTagList list) {
                        target.setTag(key, list);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to copy FC tag: {}", e.getMessage());
        }
    }

    /** Creates a minimal client-side World for FC tile entity particle spawning. */
    private static btw.modern.World createClientWorld(Level level) {
        // Client-local sounds and aux effects go to the shared client sink only for a real client level
        // (a dedicated server never builds this world, nor loads the client-only sink class).
        final boolean clientSink = level.isClientSide();
        return new btw.modern.World() {
            { this.isRemote = true; this.rand = new java.util.Random();
              // 1.5.2 worlds always have a provider: frozen Entity.<init> reads
              // par1World.provider.dimensionId (vanilla/client Entity.java:255-258), so FC TESRs that
              // build an EntityItem on this world (FCTileEntityToolPlacedRenderer:42) NPE'd every
              // frame. Same setup as WorldBridge's provider and ProxyEntity.createDummyClientWorld.
              this.provider = new btw.modern.WorldProvider() {};
              this.provider.worldObj = this;
              if (level.dimension() == Level.NETHER) {
                  this.provider.dimensionId = -1;
                  this.provider.isHellWorld = true;
                  this.provider.hasNoSky = true;
              } else if (level.dimension() == Level.END) {
                  this.provider.dimensionId = 1;
                  this.provider.hasNoSky = true;
              } }
            public int getBlockId(int x, int y, int z) { return ProxyRegistry.getBlockId(level.getBlockState(new net.minecraft.core.BlockPos(x,y,z)).getBlock()); }
            public int getBlockMetadata(int x, int y, int z) {
                net.minecraft.world.level.block.state.BlockState s = level.getBlockState(new net.minecraft.core.BlockPos(x,y,z));
                return s.hasProperty(ProxyBlock.META) ? s.getValue(ProxyBlock.META) : 0;
            }
            public btw.modern.Material getBlockMaterial(int x, int y, int z) { return btw.modern.Material.air; }
            public boolean isAirBlock(int x, int y, int z) { return level.getBlockState(new net.minecraft.core.BlockPos(x,y,z)).isAir(); }
            public boolean isBlockNormalCube(int x, int y, int z) { return false; }
            // FCMOD-INTEGRATION(1.20.1): fc_particles: FC tile entities' client-tick particles through the one shared table and 4.B's limit (FcParticles.addLocal logs a failure once)
            public void spawnParticle(String name, double x, double y, double z, double vx, double vy, double vz) {
                FcParticles.addLocal(level, name, x, y, z, vx, vy, vz);
            }
            public boolean setBlock(int x, int y, int z, int id, int meta, int flags) { return false; }
            public btw.modern.TileEntity getBlockTileEntity(int x, int y, int z) {
                net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(new net.minecraft.core.BlockPos(x, y, z));
                if (be instanceof ProxyBlockEntity pbe) return pbe.getFcTileEntity();
                return null;
            }
            public boolean canPlaceEntityOnSide(int id, int x, int y, int z, boolean b, int s, btw.modern.Entity e, btw.modern.ItemStack st) { return false; }
            public java.util.List getEntitiesWithinAABB(Class c, btw.modern.AxisAlignedBB bb) { return java.util.Collections.emptyList(); }
            public java.util.List getEntitiesWithinAABBExcludingEntity(btw.modern.Entity e, btw.modern.AxisAlignedBB bb) { return java.util.Collections.emptyList(); }
            public void scheduleBlockUpdate(int x, int y, int z, int id, int delay) {}
            public void notifyBlockChange(int x, int y, int z, int id) {}
            public boolean spawnEntityInWorld(btw.modern.Entity e) { return false; }
            public btw.modern.BiomeGenBase getBiomeGenForCoords(int x, int z) { return btw.modern.BiomeGenBase.plains; }
            // 1.5.2 WorldClient + RenderGlobal sound contract (audit sounds S1): the client ticker runs FC
            // te.updateEntity() here (tick() below), and 1.5.2 TEs played their client-only sounds through
            // World.playSound (e.g. the turntable click, FCTileEntityTurntable.java:88-108 `if
            // (worldObj.isRemote)`), which WorldClient played locally (WorldClient.java:483-505).
            // playSoundEffect / playSoundAtEntity stay no-ops as in RenderGlobal (:1812, :1817).
            public void playSoundEffect(double x, double y, double z, String s, float v, float p) {}
            public void playSoundAtEntity(btw.modern.Entity e, String s, float v, float p) {}
            @Override
            public void playSound(double x, double y, double z, String s, float v, float p, boolean distanceDelay) {
                if (clientSink) btw.forge.client.LegacyClientSound.playLocal(x, y, z, s, v, p, distanceDelay);
            }
            // vanilla/client World.java:4994-4997: the 6-arg overload is the 7-arg one without delay.
            @Override
            public void playSound(double x, double y, double z, String s, float v, float p) {
                playSound(x, y, z, s, v, p, false);
            }
            // RenderGlobal.playAuxSFX / broadcastSound, FC ids through FC's own client handler.
            public void playAuxSFX(int id, int x, int y, int z, int data) {
                if (clientSink) btw.forge.client.LegacyClientSound.playAux(id, x, y, z, data);
            }
            public void playAuxSFXAtEntity(btw.modern.EntityPlayer p, int id, int x, int y, int z, int data) {
                playAuxSFX(id, x, y, z, data);
            }
            @Override
            public void func_82739_e(int id, int x, int y, int z, int data) {
                if (clientSink) btw.forge.client.LegacyClientSound.playAux(id, x, y, z, data, true);
            }
            public boolean isRaining() { return false; }
            public boolean isBlockGettingPowered(int x, int y, int z) { return false; }
            public boolean isBlockIndirectlyGettingPowered(int x, int y, int z) { return false; }
            public btw.modern.IChunkProvider createChunkProvider() { return null; }
            public boolean destroyBlock(int x, int y, int z, boolean drop) { return false; }
            // Deliberately not a re-mesh (unlike ClientFcWorld, MPR-6): FC calls this only from
            // readNBTFromPacket (FCTileEntityCookingVessel/Hopper/Campfire/...), which runs for every data
            // packet (load -> applySyncData; the server resends each FC TE every 20 ticks). onDataPacket
            // re-meshes once, and only when FC's description data changed.
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
    private static final String TAG_FC_SYNC = "fcSync";

    /** One WARN (with stack) per FC block id and site for save/load failures. Errors from FC code
     *  must never escape: ChunkMap.save catches Exception only, and a throw during the stop save
     *  aborts MinecraftServer.stopServer before session.lock is released (triage RE1). */
    private static final java.util.Set<String> SAVE_LOAD_FAIL_LOGGED =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static void logSaveLoadFailureOnce(String site, int blockId, Throwable t) {
        if (SAVE_LOAD_FAIL_LOGGED.add(site + ":" + blockId)) {
            LOGGER.warn("FC tile entity {} failed for block {} (further failures for this block are not logged)",
                    site, blockId, t);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(TAG_FC_BLOCK_ID, fcBlockId);
        if (fcTileEntity != null) {
            CompoundTag fcData = new CompoundTag();
            ForgeNBTCompound wrapper = new ForgeNBTCompound(fcData);
            try {
                fcTileEntity.writeToNBT(wrapper);
            } catch (Throwable e) {
                logSaveLoadFailureOnce("save", fcBlockId, e);
            }
            tag.put(TAG_FC_DATA, fcData);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        int prevBlockId = fcBlockId;
        TileEntity prevTe = fcTileEntity;
        fcBlockId = tag.getInt(TAG_FC_BLOCK_ID);

        // Recreate the FC tile entity from the FC block. It is built completely (data, position,
        // world, sync data) before being published: on the client a chunk-mesh thread can read
        // fcTileEntity at any moment (FCBakedModel live capture).
        TileEntity te = null;
        if (fcBlockId > 0 && fcBlockId < btw.modern.Block.blocksList.length) {
            btw.modern.Block fcBlock = btw.modern.Block.blocksList[fcBlockId];
            if (fcBlock != null) {
                try {
                    te = fcBlock.createNewTileEntity(null);
                } catch (Throwable e) {
                    logSaveLoadFailureOnce("create", fcBlockId, e);
                }
            }
        }
        boolean recreated = te != null;
        if (!recreated) te = prevTe;   // as before: no new instance, so reload into the current one

        // Load saved FC data into the tile entity
        if (te != null && tag.contains(TAG_FC_DATA)) {
            CompoundTag fcData = tag.getCompound(TAG_FC_DATA);
            ForgeNBTCompound wrapper = new ForgeNBTCompound(fcData);
            try {
                te.readFromNBT(wrapper);
            } catch (Throwable e) {
                logSaveLoadFailureOnce("load", fcBlockId, e);
            }
        }

        if (recreated && prevTe != null && prevBlockId == fcBlockId && prevTe.getClass() == te.getClass()
                && level != null && level.isClientSide()) {
            copyClientOnlyState(prevTe, te);
        }

        try {
            initFcTileEntity(te);
        } catch (Throwable e) {
            logSaveLoadFailureOnce("init", fcBlockId, e);
        }
        // FC description data (data packet / chunk data only), once the TE has its world:
        // readNBTFromPacket implementations call worldObj.markBlockRangeForRenderUpdate.
        applySyncData(te, tag);
        fcTileEntity = te;
    }

    /**
     * Client-only FC tile entity state that no NBT carries: animation advanced by client ticks and
     * set by block events (receiveClientEvent). 1.5.2 kept the client TE across data packets
     * (NetClientHandler.handleTileEntityData, NetClientHandler.java:1191-1219, read the packet into
     * the existing TE), but {@link #load} builds a new FC TE from every data packet, and the server
     * sends one per FC TE every 20 ticks. Without carrying these over, an open wicker basket's
     * stored item dropped back and rose again every second and an open chest's lid snapped shut.
     */
    private static final String[] CLIENT_ONLY_FIELDS = {
            "m_fLidOpenRatio", "m_fPrevLidOpenRatio", "m_bClosing",   // FCTileEntityBasket.java:29-32
            "numUsingPlayers", "lidAngle", "prevLidAngle",            // TileEntityChest (TileEntityChestRenderer)
    };

    /** Public, non-static, non-final {@link #CLIENT_ONLY_FIELDS} present on a TE class. */
    private static final ClassValue<java.lang.reflect.Field[]> CLIENT_ONLY_FIELD_CACHE = new ClassValue<>() {
        @Override
        protected java.lang.reflect.Field[] computeValue(Class<?> type) {
            java.util.List<java.lang.reflect.Field> fields = new java.util.ArrayList<>();
            for (String name : CLIENT_ONLY_FIELDS) {
                try {
                    java.lang.reflect.Field f = type.getField(name);
                    int mod = f.getModifiers();
                    if (!java.lang.reflect.Modifier.isStatic(mod) && !java.lang.reflect.Modifier.isFinal(mod)) {
                        fields.add(f);
                    }
                } catch (Throwable ignored) {}   // not declared on this TE class
            }
            return fields.toArray(new java.lang.reflect.Field[0]);
        }
    };

    /** Copies {@link #CLIENT_ONLY_FIELDS} from the replaced FC TE to its same-class replacement. */
    private static void copyClientOnlyState(TileEntity from, TileEntity to) {
        for (java.lang.reflect.Field f : CLIENT_ONLY_FIELD_CACHE.get(to.getClass())) {
            try {
                f.set(to, f.get(from));
            } catch (Throwable ignored) {}   // client load path: never escape
        }
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
                    tag.put(TAG_FC_SYNC, syncTag);
                }
            } catch (Throwable ignored) {}   // chunk-send path: an FC Error must not escape
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
        // Use getUpdateTag() which includes both saveAdditional data AND fcSync data
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this,
                be -> ((ProxyBlockEntity) be).getUpdateTag());
    }

    /**
     * Called on the CLIENT when a block entity update packet arrives.
     * Loads the FC tile entity data (and FC sync data) from the received NBT.
     */
    @Override
    public void onDataPacket(net.minecraft.network.Connection net,
                              net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            CompoundTag sync = syncSnapshot(tag);
            load(tag);
            // 1.5.2 FC readNBTFromPacket re-meshed the block (worldObj.markBlockRangeForRenderUpdate,
            // e.g. FCTileEntityCookingVessel / FCTileEntityHopper / FCTileEntityCampfire), so geometry
            // FCBakedModel captures live from TE state (vessel contents, hopper filter, spit, ...)
            // followed it. The client world wrapper cannot reach the LevelRenderer, and the server's
            // same-state sendBlockUpdated in syncToClients triggers no client rebuild. Re-mesh here,
            // only when FC's description data actually changed: the server resends it every 20 ticks.
            if (!java.util.Objects.equals(sync, lastClientSync) && level != null && level.isClientSide()) {
                try {
                    level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 0);
                } catch (Throwable ignored) {}   // client packet path: never escape
            }
            lastClientSync = sync;
        }
    }

    /**
     * Called on the CLIENT when chunk data arrives with the update tag. The section is meshed after
     * the chunk loads, so only the sync baseline is recorded.
     */
    @Override
    public void handleUpdateTag(CompoundTag tag) {
        CompoundTag sync = syncSnapshot(tag);
        load(tag);
        lastClientSync = sync;
    }

    /** Copy of the tag's fcSync (taken before FC reads it), or null if it has none. */
    @javax.annotation.Nullable
    private static CompoundTag syncSnapshot(CompoundTag tag) {
        return tag.contains(TAG_FC_SYNC) ? tag.getCompound(TAG_FC_SYNC).copy() : null;
    }

    /**
     * Applies FC's sync-specific data (from getDescriptionPacket/readNBTFromPacket).
     * This handles fields like m_bIsCooking that are NOT in writeToNBT but ARE
     * synced via FC's custom packet system.
     */
    private static void applySyncData(TileEntity te, CompoundTag tag) {
        if (te != null && tag.contains(TAG_FC_SYNC)) {
            try {
                CompoundTag syncTag = tag.getCompound(TAG_FC_SYNC);
                ForgeNBTCompound wrapper = new ForgeNBTCompound(syncTag);
                // FC tile entities that implement FCITileEntityDataPacketHandler
                // have a readNBTFromPacket method for sync-specific data.
                // Use reflection since the interface is in the FC runtime jar.
                java.lang.reflect.Method readMethod = null;
                for (java.lang.reflect.Method m : te.getClass().getMethods()) {
                    if (m.getName().equals("readNBTFromPacket")
                            && m.getParameterCount() == 1) {
                        readMethod = m;
                        break;
                    }
                }
                if (readMethod != null) {
                    readMethod.invoke(te, wrapper);
                }
            } catch (Throwable ignored) {}   // client load path: an FC Error must not escape
        }
    }

    /**
     * Marks the tile entity as needing a sync to clients. Call this when
     * FC tile entity state changes that clients need to see (cook level,
     * cooking state, etc.).
     */
    public void syncToClients() {
        if (level != null && !level.isClientSide() && level instanceof net.minecraft.server.level.ServerLevel sl) {
            setChanged();
            // Send tile entity data packet to all tracking players (getUpdatePacket builds the tag)
            var packet = getUpdatePacket();
            if (packet != null) {
                for (net.minecraft.server.level.ServerPlayer player : sl.players()) {
                    player.connection.send(packet);
                }
            }
            sl.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
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

    public static void tick(Level level, BlockPos pos, BlockState state, ProxyBlockEntity be) {
        TileEntity te = be.fcTileEntity;
        if (te != null) {
            // Always sync coordinates and world — they may have been wrong
            // during initial construction (e.g., load() before setLevel())
            te.xCoord = pos.getX();
            te.yCoord = pos.getY();
            te.zCoord = pos.getZ();
            if (te.worldObj == null && level != null) {
                if (level instanceof net.minecraft.server.level.ServerLevel sl) {
                    te.setWorldObj(WorldBridge.getOrCreate(sl));
                } else {
                    te.setWorldObj(be.clientWorldFor(level));
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

            try {
                te.updateEntity();
            } catch (Throwable e) {
                // U10 / HAI-10: one WARN with stack per FC tile entity class and throw site, on
                // either side (the ticker runs on the client and the server thread). Was: an
                // unthrottled per-tick INFO without a stack for everything but turntables and
                // furnace bricks, and Errors escaped into a server crash. ML-1: the logged stack
                // is the real failure's, even inside an FC ReportedException.
                FCPuppetSupport.logFailureOnce(LOGGER, "FC tile entity tick", te, pos, e);
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

    // setRemoved runs on the stop path: MinecraftServer.stopServer -> saveAllChunks(flush) ->
    // ChunkMap.processUnloads -> LevelChunk.clearAllBlockEntities -> setRemoved, before
    // session.lock is released; clearRemoved runs on the chunk-load path (LevelChunk.setBlockEntity).
    // An FC throw from either must not escape (triage RE1, as save/load above).

    @Override
    public void setRemoved() {
        super.setRemoved();
        TileEntity te = fcTileEntity;
        if (te != null) {
            try {
                te.invalidate();
            } catch (Throwable e) {
                logSaveLoadFailureOnce("invalidate", fcBlockId, e);
            }
        }
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        TileEntity te = fcTileEntity;
        if (te != null) {
            try {
                te.validate();
            } catch (Throwable e) {
                logSaveLoadFailureOnce("validate", fcBlockId, e);
            }
        }
    }
}
