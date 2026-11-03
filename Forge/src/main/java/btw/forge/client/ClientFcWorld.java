package btw.forge.client;

import btw.forge.FcAttachSupport;
import btw.forge.FcParticles;
import btw.forge.ProxyBlock;
import btw.forge.ProxyBlockEntity;
import btw.forge.ProxyRegistry;
import btw.forge.WorldBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The client-side FC world over a {@link ClientLevel}: 1.5.2 WorldClient as FC's client-only callbacks saw
 * it. Used for FC's aux-FX handler ({@link LegacyClientSound#playAux}) and for the FC block-change
 * notifications 1.5.2 fired on the client (MPR-6 / sounds S2, from btw.forge.mixin.client.ClientLevelMixin):
 * ClientNotificationOfMetadataChange (vanilla/client WorldClient.java:294-315), ClientBreakBlock and
 * ClientBlockAdded (Chunk.java:633, :683).
 *
 * <p>Read-only: block reads go to the client level through the same legacy-id mapping the other client FC
 * worlds use; every write is a no-op (FCBlockPlants.ClientBreakBlock clears ground cover through the world,
 * which the server owns). Entity queries return only the local player, as a facade that FC may move (the
 * bellows lift). Sounds and aux effects go to {@link LegacyClientSound}; particles are spawned
 * locally (FC's client callbacks and aux handler are their only source -- the server never sends them);
 * render-range marks re-mesh the affected sections like 1.5.2 RenderGlobal.markBlockRangeForRenderUpdate.</p>
 *
 * <p>Two views: {@link #forLevel} reads every block by its FC identity (FC's callbacks, the aux-FX handler, bone meal
 * and weed prediction, FC-backed block entities); {@link #forSupport} is the view FC's attachment rules predict on
 * (btw.forge.client.ClientAttachSupport), where a modern fence, wall or stained glass pane reads as its BTW relative's
 * id, as the server's stand-in answers through that relative (btw.forge.FcAttachSupport.supportRelative).</p>
 *
 * <p>Render (main) thread only.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class ClientFcWorld extends btw.modern.World {

    private static final Logger LOGGER = LogManager.getLogger("BTW-ClientFcWorld");
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    /**
     * Last world built, by identity of its level. Weak, as FCMiningSpeed's context: a level the player has
     * left is not pinned by this static; the price is an occasional rebuild of this light wrapper.
     */
    private static WeakReference<ClientFcWorld> cached = new WeakReference<>(null);
    /** As {@link #cached}, for the support view. */
    private static WeakReference<ClientFcWorld> cachedSupport = new WeakReference<>(null);

    private final ClientLevel level;
    /** The support view: a block without FC identity reads as its support relative's id (0 without one). */
    private final boolean supportReads;

    private ClientFcWorld(ClientLevel level, boolean supportReads) {
        this.level = level;
        this.supportReads = supportReads;
        this.isRemote = true;
        this.rand = new java.util.Random();
        // 1.5.2 worlds always have a provider: frozen Entity.<init> reads provider.dimensionId
        // (vanilla/client Entity.java:255-258), and FC's aux handler builds EntityDiggingFX on this world.
        this.provider = new btw.modern.WorldProvider() {};
        this.provider.worldObj = this;
        if (level.dimension() == Level.NETHER) {
            this.provider.dimensionId = -1;
            this.provider.isHellWorld = true;
            this.provider.hasNoSky = true;
        } else if (level.dimension() == Level.END) {
            this.provider.dimensionId = 1;
            this.provider.hasNoSky = true;
        }
    }

    /** The FC world for {@code level} (the current client level). Main thread only. */
    public static ClientFcWorld forLevel(ClientLevel level) {
        ClientFcWorld world = cached.get();
        if (world == null || world.level != level) {
            world = new ClientFcWorld(level, false);
            cached = new WeakReference<>(world);
        }
        return world;
    }

    /**
     * FCMOD-INTEGRATION(1.20.1): support_relatives. The support view of {@code level}, for FC's attachment rules only
     * (btw.forge.client.ClientAttachSupport): a block without FC identity reads as its support relative's FC id
     * (btw.forge.FcAttachSupport.supportRelativeId: a modern fence, wall or stained glass pane as the oak fence, the
     * cobblestone wall or the glass pane), so FC's methods run on the relative's FC block with this world, as the
     * server's stand-in runs them with WorldBridge; World.getSupportBlockId's default then gives the relative too. Every
     * other client reader keeps {@link #forLevel}'s identity reads (FC's client beacon matches block ids, so a relative
     * must not reach it). Main thread only.
     */
    public static ClientFcWorld forSupport(ClientLevel level) {
        ClientFcWorld world = cachedSupport.get();
        if (world == null || world.level != level) {
            world = new ClientFcWorld(level, true);
            cachedSupport = new WeakReference<>(world);
        }
        return world;
    }

    // ------------------------------------------------------------------
    // Block reads (as ProxyEntity.createDummyClientWorld / ProxyBlockEntity's client world)
    // ------------------------------------------------------------------

    @Override
    public int getBlockId(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);
        int id = ProxyRegistry.getBlockId(state.getBlock());
        // FCMOD-INTEGRATION(1.20.1): support_relatives: the support view reads a modern fence, wall or stained glass pane as its BTW relative
        return id != 0 || !supportReads ? id : FcAttachSupport.supportRelativeId(state, level, pos);
    }

    @Override
    public int getBlockMetadata(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(ProxyBlock.META)) return state.getValue(ProxyBlock.META);
        return WorldBridge.deriveVanillaMetadata(level, state, pos);   // never throws
    }

    @Override
    public btw.modern.Material getBlockMaterial(int x, int y, int z) {
        int id = getBlockId(x, y, z);
        if (id <= 0) return btw.modern.Material.air;
        btw.modern.Block block = btw.modern.Block.blocksList[id];
        return block != null && block.blockMaterial != null ? block.blockMaterial : btw.modern.Material.air;
    }

    @Override
    public boolean isAirBlock(int x, int y, int z) {
        return getBlockId(x, y, z) <= 0;
    }

    @Override
    public boolean isBlockNormalCube(int x, int y, int z) {
        int id = getBlockId(x, y, z);
        if (id <= 0) return false;
        btw.modern.Block block = btw.modern.Block.blocksList[id];
        return block != null && block.blockMaterial != null && block.blockMaterial.isOpaque()
                && block.renderAsNormalBlock();
    }

    /**
     * FCMOD-INTEGRATION(1.20.1): solid_top_surface. 4.B's World.doesBlockHaveSolidTopSurface (vanilla/client
     * World.java:5190-5195), the rule WorldBridge answers on the server: the block's large centre hard point on
     * top. The client then predicts FcAttachSupport's redstone torch answer as the server gives it: a top slab or an
     * upside-down stair holds a standing redstone torch (this view used to answer the normal-cube test, which
     * refused them); glowstone, ice, TNT, the redstone lamp and a closed upper trapdoor do not. A cell this view
     * reads as air although 1.20.1 holds a block there (id 0: modern blocks, read by identity here, except a support
     * relative on the support view) keeps 1.20.1's sturdy face, as the server's stand-ins answer it.
     */
    @Override
    public boolean doesBlockHaveSolidTopSurface(int x, int y, int z) {
        int id = getBlockId(x, y, z);
        btw.modern.Block block = id > 0 && id < btw.modern.Block.blocksList.length ? btw.modern.Block.blocksList[id] : null;
        if (block != null) return block.HasLargeCenterHardPointToFacing(this, x, y, z, 1);
        BlockPos pos = new BlockPos(x, y, z);
        return level.getBlockState(pos).isFaceSturdy(level, pos, net.minecraft.core.Direction.UP);
    }

    @Override
    public btw.modern.TileEntity getBlockTileEntity(int x, int y, int z) {
        net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(new BlockPos(x, y, z));
        // FCMOD-INTEGRATION(1.20.1): fc_beacon: an FC-backed block entity hands FC its client tile entity
        if (be instanceof btw.forge.FcBackedBlockEntity backed) {
            return btw.forge.FcBackedBlockEntities.isBacked(be) ? backed.btw$fcTile() : null;
        }
        return be instanceof ProxyBlockEntity pbe ? pbe.getFcTileEntity() : null;
    }

    @Override
    public boolean canBlockSeeTheSky(int x, int y, int z) {
        return WorldBridge.fcCanSeeSky(level, x, y, z);   // FCMOD-INTEGRATION(1.20.1): sky_column: the server's rule (End, Nether: the sky-source column)
    }

    // FCMOD-INTEGRATION(1.20.1): fc_beacon: 1.5.2 WorldClient answered the clock and the build range; FC's client beacon gates its power update on the clock (Common FCTileEntityBeacon.java:67) and its pyramid on the floor
    @Override public long getTotalWorldTime() { return level.getGameTime(); }
    @Override public long getWorldTime() { return level.getDayTime(); }
    @Override public int getFcMinBuildHeight() { return level.getMinBuildHeight(); }
    @Override public int getFcMaxBuildHeight() { return level.getMaxBuildHeight(); }

    @Override
    public boolean isRaining() {
        return level.isRaining();
    }

    @Override public int getSavedLightValue(btw.modern.EnumSkyBlock type, int x, int y, int z) { return 15; }
    @Override public int getFullBlockLightValue(int x, int y, int z) { return 15; }
    @Override public boolean isBlockGettingPowered(int x, int y, int z) { return false; }
    @Override public boolean isBlockIndirectlyGettingPowered(int x, int y, int z) { return false; }
    @Override public boolean checkChunksExist(int x1, int y1, int z1, int x2, int y2, int z2) { return true; }
    @Override public btw.modern.BiomeGenBase getBiomeGenForCoords(int x, int z) { return btw.modern.BiomeGenBase.plains; }
    @Override public btw.modern.WorldChunkManager getWorldChunkManager() { return null; }
    @Override public btw.modern.IChunkProvider createChunkProvider() { return null; }
    @Override public boolean canMineBlock(btw.modern.EntityPlayer p, int x, int y, int z) { return true; }

    // ------------------------------------------------------------------
    // Writes and entities: the server owns them (no-ops, like the other client FC worlds), except the
    // local player, which the client moves
    // ------------------------------------------------------------------

    @Override public boolean setBlock(int x, int y, int z, int id, int meta, int flags) { return false; }
    @Override public boolean setBlockMetadata(int x, int y, int z, int meta, int flags) { return false; }
    @Override public boolean setBlockToAir(int x, int y, int z) { return false; }
    @Override public boolean destroyBlock(int x, int y, int z, boolean drop) { return false; }
    @Override public void notifyBlockChange(int x, int y, int z, int id) {}
    @Override public boolean spawnEntityInWorld(btw.modern.Entity e) { return false; }
    @Override public boolean checkNoEntityCollision(btw.modern.AxisAlignedBB bb) { return true; }

    /**
     * Entity queries see only the local player: the one entity the client owns (1.5.2 WorldClient listed
     * every entity, vanilla/client World.java:3546). The client bellows lifts it when it expands (Client
     * FCBlockBellows.LiftCollidingEntities :577-605, from ClientNotificationOfMetadataChange :702-706). The
     * server cannot do that lift, since the client is authoritative for its player's position. Other
     * entities are the server's: the bellows' item blowing (:432) finds none here, as it should.
     */
    @Override
    @SuppressWarnings("unchecked")
    public java.util.List getEntitiesWithinAABB(Class c, btw.modern.AxisAlignedBB bb) {
        java.util.List list = new java.util.ArrayList();
        btw.modern.Entity player = localPlayerWithin(bb);
        if (player != null && c != null && c.isInstance(player)) list.add(player);
        return list;
    }

    /** As {@link #getEntitiesWithinAABB}, any class, without {@code e}. */
    @Override
    @SuppressWarnings("unchecked")
    public java.util.List getEntitiesWithinAABBExcludingEntity(btw.modern.Entity e, btw.modern.AxisAlignedBB bb) {
        java.util.List list = new java.util.ArrayList();
        btw.modern.Entity player = localPlayerWithin(bb);
        if (player != null && player != e) list.add(player);
        return list;
    }

    /**
     * The local-player facade ({@link LegacyClientSound#localPlayer}) when the player of this level touches
     * {@code bb}, as 1.5.2's AxisAlignedBB.intersectsWith tested it (strictly); else null. Spectators and
     * passengers are left out: they collide with no block, or their vehicle owns their position.
     */
    private btw.modern.Entity localPlayerWithin(btw.modern.AxisAlignedBB bb) {
        if (bb == null) return null;
        try {
            Minecraft mc = Minecraft.getInstance();
            net.minecraft.client.player.LocalPlayer player = mc.player;
            if (player == null || player.level() != level || player.isSpectator() || player.isPassenger()) {
                return null;
            }
            if (!player.getBoundingBox().intersects(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ)) {
                return null;
            }
            return LegacyClientSound.localPlayer(mc);
        } catch (Throwable t) {
            warnOnce("entities|" + t.getClass().getName(), "Client FC entity query failed", t);
            return null;
        }
    }
    @Override public boolean canPlaceEntityOnSide(int id, int x, int y, int z, boolean b, int s, btw.modern.Entity e, btw.modern.ItemStack st) { return false; }

    // ------------------------------------------------------------------
    // Sound (1.5.2 WorldClient + RenderGlobal)
    // ------------------------------------------------------------------

    @Override
    public void playSound(double x, double y, double z, String sound, float volume, float pitch, boolean distanceDelay) {
        LegacyClientSound.playLocal(x, y, z, sound, volume, pitch, distanceDelay);
    }

    /** vanilla/client World.java:4994-4997: the 6-arg overload is the 7-arg one without delay. */
    @Override
    public void playSound(double x, double y, double z, String sound, float volume, float pitch) {
        LegacyClientSound.playLocal(x, y, z, sound, volume, pitch, false);
    }

    /** RenderGlobal.playSound is empty on the client (RenderGlobal.java:1812); the server broadcasts it. */
    @Override public void playSoundEffect(double x, double y, double z, String sound, float volume, float pitch) {}
    /** As {@link #playSoundEffect} (RenderGlobal.java:1817). */
    @Override public void playSoundAtEntity(btw.modern.Entity e, String sound, float volume, float pitch) {}

    @Override
    public void playAuxSFX(int id, int x, int y, int z, int data) {
        LegacyClientSound.playAux(id, x, y, z, data);
    }

    @Override
    public void playAuxSFXAtEntity(btw.modern.EntityPlayer player, int id, int x, int y, int z, int data) {
        LegacyClientSound.playAux(id, x, y, z, data);
    }

    /** 1.5.2 World.func_82739_e -> RenderGlobal.broadcastSound on the client. */
    @Override
    public void func_82739_e(int id, int x, int y, int z, int data) {
        LegacyClientSound.playAux(id, x, y, z, data, true);
    }

    // ------------------------------------------------------------------
    // Particles (1.5.2 RenderGlobal.spawnParticle)
    // ------------------------------------------------------------------

    // FCMOD-INTEGRATION(1.20.1): fc_particles: FC's aux-FX and client-notification particles through the one shared table and 4.B's limit (FcParticles.addLocal logs a failure once)
    @Override
    public void spawnParticle(String name, double x, double y, double z, double vx, double vy, double vz) {
        FcParticles.addLocal(level, name, x, y, z, vx, vy, vz);
    }

    // ------------------------------------------------------------------
    // Render updates (1.5.2 RenderGlobal.markBlockRangeForRenderUpdate / markBlockForRenderUpdate)
    // ------------------------------------------------------------------

    @Override
    public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {
        markRangeDirty(x1, y1, z1, x2, y2, z2);
    }

    @Override
    public void markBlockForRenderUpdate(int x, int y, int z) {
        markRangeDirty(x, y, z, x, y, z);
    }

    /** 1.5.2 RenderGlobal.markBlockForUpdate re-renders the block's neighbourhood on the client too. */
    @Override
    public void markBlockForUpdate(int x, int y, int z) {
        markRangeDirty(x, y, z, x, y, z);
    }

    /**
     * Re-meshes the sections covering the block range grown by one, as 1.5.2
     * RenderGlobal.markBlockRangeForRenderUpdate -> markBlocksForUpdate(x1 - 1, ..., z2 + 1) did;
     * LevelRenderer.setBlocksDirty applies the same one-block margin. FC relies on this for state it derives
     * client-side from blocks up to 3 away (FCBlockAxle.ClientCheckIfPowered after a gear box changes, MPR-6):
     * 1.20.1 itself only re-meshes the sections within one block of a change.
     */
    public static void markRangeDirty(int x1, int y1, int z1, int x2, int y2, int z2) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return;
            if (!mc.isSameThread()) {
                mc.execute(() -> markRangeDirty(x1, y1, z1, x2, y2, z2));
                return;
            }
            if (mc.level == null || mc.levelRenderer == null) return;
            mc.levelRenderer.setBlocksDirty(x1, y1, z1, x2, y2, z2);
        } catch (Throwable t) {
            warnOnce("remesh|" + t.getClass().getName(), "Client FC render-range update failed", t);
        }
    }

    // ------------------------------------------------------------------
    // FC client block-change notifications (ClientLevelMixin)
    // ------------------------------------------------------------------

    /** The state at a position before a client block change, captured at the change's HEAD. */
    private record PendingChange(BlockPos pos, BlockState oldState) {}

    /** Pushed for changes that involve no ProxyBlock: nothing to notify, nothing allocated. */
    private static final PendingChange UNRELATED = new PendingChange(BlockPos.ZERO, null);

    /**
     * HEAD/RETURN pairs of client block changes, innermost first: a change can nest others (neighbour shape
     * updates inside Level.setBlock). Main thread only.
     */
    private static final ArrayDeque<PendingChange> PENDING = new ArrayDeque<>();

    /** Bounds the stack if a change ever threw between its HEAD and RETURN (its entry is never popped). */
    private static final int MAX_PENDING = 64;

    /**
     * At the HEAD of a client block change ({@code ClientLevel.setServerVerifiedBlockState} / {@code setBlock}):
     * remembers the old state when a ProxyBlock is involved. Must be paired with {@link #afterClientBlockChange}.
     */
    public static void beforeClientBlockChange(ClientLevel level, BlockPos pos, BlockState newState) {
        if (!Minecraft.getInstance().isSameThread()) return;   // paired call skips too (same thread)
        PendingChange change = UNRELATED;
        try {
            BlockState old = level.getBlockState(pos);
            if (old.getBlock() instanceof ProxyBlock || (newState != null && newState.getBlock() instanceof ProxyBlock)) {
                change = new PendingChange(pos.immutable(), old);
            }
        } catch (Throwable t) {
            warnOnce("before|" + t.getClass().getName(), "Capturing a client block change failed", t);
        }
        if (PENDING.size() >= MAX_PENDING) PENDING.clear();
        PENDING.push(change);
    }

    /**
     * At the RETURN of the change {@link #beforeClientBlockChange} opened: fires the FC client hooks 1.5.2 ran
     * for a client-side block change, against this world.
     * <ul>
     *   <li>Same ProxyBlock, new META, server-verified: {@code ClientNotificationOfMetadataChange(world, x, y, z,
     *       old, new)} (WorldClient.setBlockAndMetadataAndInvalidate, vanilla/client WorldClient.java:294-315,
     *       which only server packets reached). Mechanical power-on/off sounds, particles and the gear box's
     *       +-3 re-mesh live there (Client FCBlockGearBox.java:424-439, FCBlockMillStone, FCBlockBellows).</li>
     *   <li>Block changed: the old ProxyBlock's {@code ClientBreakBlock(world, x, y, z, oldId, oldMeta)}, then the
     *       new ProxyBlock's {@code ClientBlockAdded(world, x, y, z)} (Chunk.setBlockIDWithMetadata FCMODs,
     *       vanilla/client Chunk.java:633 / :683, which also ran for client-predicted changes).</li>
     * </ul>
     * A server update absorbed by a pending prediction (state unchanged here) fires nothing; it is applied
     * later through ClientLevel.syncBlockState -> setBlock, which is hooked as well.
     *
     * @param serverVerified the new state came from the server (setServerVerifiedBlockState, or setBlock
     *                       outside a client prediction, i.e. syncBlockState)
     */
    public static void afterClientBlockChange(ClientLevel level, BlockPos pos, boolean serverVerified) {
        if (!Minecraft.getInstance().isSameThread()) return;
        PendingChange change = PENDING.poll();
        if (change == null || change == UNRELATED) return;
        if (!change.pos().equals(pos)) {   // unbalanced after a throw: drop the stale entries
            PENDING.clear();
            return;
        }
        BlockState oldState = change.oldState();
        BlockState newState;
        try {
            newState = level.getBlockState(pos);
        } catch (Throwable t) {
            return;
        }
        if (newState == oldState) return;
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        if (oldState.getBlock() == newState.getBlock()) {
            if (serverVerified && oldState.getBlock() instanceof ProxyBlock pb
                    && oldState.hasProperty(ProxyBlock.META) && newState.hasProperty(ProxyBlock.META)) {
                int oldMeta = oldState.getValue(ProxyBlock.META);
                int newMeta = newState.getValue(ProxyBlock.META);
                btw.modern.Block fc = fcBlock(pb);
                if (oldMeta != newMeta && fc != null) {
                    try {
                        fc.ClientNotificationOfMetadataChange(forLevel(level), x, y, z, oldMeta, newMeta);
                    } catch (Throwable t) {
                        warnHookOnce("ClientNotificationOfMetadataChange", fc, t);
                    }
                }
            }
            return;
        }
        if (oldState.getBlock() instanceof ProxyBlock oldPb) {
            btw.modern.Block fc = fcBlock(oldPb);
            if (fc != null) {
                int oldMeta = oldState.hasProperty(ProxyBlock.META) ? oldState.getValue(ProxyBlock.META) : 0;
                try {
                    fc.ClientBreakBlock(forLevel(level), x, y, z, oldPb.getLegacyId(), oldMeta);
                } catch (Throwable t) {
                    warnHookOnce("ClientBreakBlock", fc, t);
                }
            }
        }
        if (newState.getBlock() instanceof ProxyBlock newPb) {
            btw.modern.Block fc = fcBlock(newPb);
            if (fc != null) {
                try {
                    fc.ClientBlockAdded(forLevel(level), x, y, z);
                } catch (Throwable t) {
                    warnHookOnce("ClientBlockAdded", fc, t);
                }
            }
        }
    }

    private static btw.modern.Block fcBlock(ProxyBlock pb) {
        int id = pb.getLegacyId();
        return id > 0 && id < btw.modern.Block.blocksList.length ? btw.modern.Block.blocksList[id] : null;
    }

    private static void warnHookOnce(String hook, btw.modern.Block fc, Throwable t) {
        warnOnce(hook + "|" + fc.getClass().getName() + "|" + t.getClass().getName(),
                "FC " + fc.getClass().getSimpleName() + "." + hook + " threw", t);
    }

    private static void warnOnce(String key, String message, Throwable t) {
        if (WARNED.add(key)) {
            LOGGER.warn("{} (further occurrences suppressed): {}", message, t.toString());
        }
    }
}
