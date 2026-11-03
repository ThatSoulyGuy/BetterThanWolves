package btw.forge;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Forge-side proxy that extends {@link net.minecraft.world.entity.Entity}
 * directly and forwards lifecycle callbacks to an FC
 * {@link btw.modern.Entity} instance.
 *
 * Used for FC entities that extend plain {@link btw.modern.Entity} rather
 * than any living-entity subclass -- for example:
 * <ul>
 *   <li>FCEntityDynamite (extends Entity)</li>
 *   <li>FCEntityFallingBlock (extends EntityFallingSand extends Entity)</li>
 *   <li>FCEntityMechPower / FCEntityWaterWheel / FCEntityWindMill</li>
 *   <li>FCEntityMovingAnchor, FCEntityMovingPlatform</li>
 *   <li>FCEntityBlockLiftedByPlatform, FCEntityCanvas, FCEntitySoulSand</li>
 *   <li>FCEntityItemFloating, FCEntityItemBloodWoodSapling (extend EntityItem)</li>
 *   <li>FCEntityBroadheadArrow, FCEntityInfiniteArrow, FCEntityRottenArrow</li>
 *   <li>FCEntityUrn, FCEntitySpiderWeb (EntityThrowable)</li>
 *   <li>FCEntityWitherSkull (EntityFireball)</li>
 * </ul>
 */
public class ProxyEntity extends net.minecraft.world.entity.Entity
        implements net.minecraftforge.entity.IEntityAdditionalSpawnData {

    private static final Logger LOGGER = LogManager.getLogger("BTW-ProxyEntity");

    private static final java.util.Set<String> LOGGED_TICK_FAILURES =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    private btw.modern.Entity fcEntity;
    private String fcClassName = "";

    /** CF2-R1: building the FC entity failed for this proxy; getFcEntity() does not retry it every frame. */
    private boolean fcBuildFailed;
    /** CF2-R1, client: built by its positional constructor from a spawn payload with no FC state (the server is
     *  removing such a proxy at its first tick); never ticked and given no state. */
    private boolean fcStateless;
    /** CF2-R1, server: why a positional FC entity was not rebuilt from its save (for the one removal log line). */
    private String fcMissingStateReason;
    /** CF2-R1, client: FC's falling-block update threw here; the proxy falls back to the server's position. */
    private boolean fcClientFallFailed;
    /** CF2-R1, client: the size of the client's last non-zero fall step (blocks per tick). */
    private double clientFallStep;
    /** CF2-R1, client: the server's last motion for a client-simulated fall (kept aside; see lerpMotion). */
    private net.minecraft.world.phys.Vec3 serverFallMotion;
    /**
     * CF2-R1, maintainer ruling OD1 (2026-10-22): true. A falling block saved mid-fall (a quit, or its area unloading)
     * is rebuilt from its save (FCData) when it loads, keeps falling and lands by FC's rules, as vanilla 1.5.2
     * (EntityFallingSand(World)) and 1.20.1 (FallingBlockEntity) keep it. BTW 4.B lost such a block: its EntityList
     * built a loaded entity only through a (World) constructor, which FCEntityFallingBlock lacks (vanilla/server
     * EntityList.java:120-136). false would bring back that loss (the first tick removes the proxy, one INFO line).
     */
    private static final boolean REBUILD_SAVED_POSITIONAL = true;

    public ProxyEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    // ------------------------------------------------------------------
    // FC entity linkage
    // ------------------------------------------------------------------

    public void setFcEntity(btw.modern.Entity fc) {
        this.fcEntity = fc;
        this.fcClassName = fc.getClass().getName();
        if (level() instanceof ServerLevel sl) {
            fc.worldObj = WorldBridge.getOrCreate(sl);
        }
        syncToFc();
    }

    public btw.modern.Entity getFcEntity() {
        if (fcEntity == null && !fcBuildFailed) {
            ensureFcEntity(false);
        }
        return fcEntity;
    }

    public String getFcClassName() {
        if (fcClassName.isEmpty()) {
            fcClassName = BTWEntityRegistration.getFcClassName(getType());
        }
        return fcClassName;
    }

    /**
     * Creates the FC entity if missing, once (CF2-R1), by the constructor FcEntityFactory picks. On the server it
     * gets the real WorldBridge so FC logic runs; on the client a read-only client FC world (rendering and the
     * client-side fall). A POSITIONAL class (no (World) or no-argument constructor) gets the proxy's position and
     * zeros; its real state comes from the spawn payload or the save. On the server such an entity exists only as
     * FC spawned it (setFcEntity) or as it was saved (its FCData, ruling OD1: REBUILD_SAVED_POSITIONAL).
     *
     * @param withState the caller applies FC state right after this call (the spawn payload carried FC state, or
     *                  the save has FCData that is to be rebuilt)
     */
    private void ensureFcEntity(boolean withState) {
        String className = getFcClassName();
        if (className == null || className.isEmpty()) return;
        try {
            FcEntityFactory factory = FcEntityFactory.of(className);
            if (!factory.usable()) {
                fcBuildFailed = true;
                if (FCPuppetSupport.firstFailure("create|" + className)) {
                    LOGGER.warn("Could not create FC entity {}: {}", className, factory.failure());
                }
                return;
            }
            boolean positional = factory.kind() == FcEntityFactory.Kind.POSITIONAL;
            btw.modern.World world;
            btw.modern.World ctorWorld;
            if (level().isClientSide) {
                world = createDummyClientWorld(level());
                ctorWorld = world;
            } else if (level() instanceof ServerLevel sl) {
                // FCMOD-INTEGRATION(1.20.1): fc_entity_construction: a positional FC entity is rebuilt on the server only from its saved state
                if (positional && !withState) return;
                world = WorldBridge.getOrCreate(sl);
                // its constructor is spawn-time code (FCEntityLightningBolt strikes when !isRemote,
                // FCEntityLightningBolt.java:45-71): a load builds it against a level-less isRemote world, then hands it
                // the real one
                ctorWorld = positional ? createDummyClientWorld(null) : world;
            } else {
                return;
            }
            btw.modern.Entity fc = factory.create(ctorWorld, getX(), getY(), getZ());
            fc.worldObj = world;
            fcEntity = fc;
            fcStateless = positional && !withState;
            syncToFc();
        } catch (Throwable e) {
            fcEntity = null;
            fcBuildFailed = true;
            Throwable root = FcEntityFactory.rootCause(e);
            if (FCPuppetSupport.firstFailure("create|" + className)) {
                LOGGER.warn("Could not create FC entity {}: {}", className, root.toString(), root);
            }
        }
    }

    /**
     * Builds a client-side {@link btw.modern.World} backed by the actual
     * MC client {@link net.minecraft.world.level.Level}. Earlier this
     * returned an "all air" stub, which made FC physics see no blocks at
     * all on the client — the visible model would fall through the floor
     * every tick (server kept the entity pinned, but the client's FC
     * tick simulated free-fall against an empty world).
     *
     * The block-query methods now delegate to the real client level via
     * the same {@link ProxyRegistry} legacy-id mapping that the server
     * {@link WorldBridge} uses, so client-side FC collision matches the
     * server within MC's tick.
     */
    public static btw.modern.World createDummyClientWorld() {
        return createDummyClientWorld(null);
    }

    public static btw.modern.World createDummyClientWorld(net.minecraft.world.level.Level mcLevel) {
        final net.minecraft.world.level.Level level = mcLevel;
        // Client-local sound / aux / render calls go to the shared client sink only for a real client
        // level (never from a server thread's world, and never on a dedicated server, where the
        // client-only sink class is not loaded); without a level they are dropped.
        final boolean clientSink = level != null && level.isClientSide();
        return new btw.modern.World() {
            { this.isRemote = true; this.rand = new java.util.Random();
              this.provider = new btw.modern.WorldProvider() {}; }

            public int getBlockId(int x, int y, int z) {
                if (level == null) return 0;
                net.minecraft.world.level.block.state.BlockState s =
                        level.getBlockState(new net.minecraft.core.BlockPos(x, y, z));
                return ProxyRegistry.getBlockId(s.getBlock());
            }

            public int getBlockMetadata(int x, int y, int z) {
                if (level == null) return 0;
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x, y, z);
                net.minecraft.world.level.block.state.BlockState s = level.getBlockState(pos);
                if (s.hasProperty(ProxyBlock.META)) return s.getValue(ProxyBlock.META);
                // Vanilla-id blocks: the SAME legacy-metadata derivation the server's WorldBridge uses
                // (log axis + stump bit, fluid level, facing, ...). Client PlayerBridge worlds predict
                // digging through this (FCMiningSpeed): returning 0 made a stump log (FCBlockLog hardness
                // x3 for meta 12) break 3x early on the client and reappear until the server finished.
                // (Server-only vanillaMetaOverrides are not visible here.)
                try {
                    return WorldBridge.deriveVanillaMetadata(level, s, pos);
                } catch (Throwable t) {
                    return 0;
                }
            }

            public btw.modern.Material getBlockMaterial(int x, int y, int z) {
                int id = getBlockId(x, y, z);
                if (id <= 0) return btw.modern.Material.air;
                btw.modern.Block block = btw.modern.Block.blocksList[id];
                return block != null ? block.blockMaterial : btw.modern.Material.air;
            }

            public boolean isAirBlock(int x, int y, int z) {
                return getBlockId(x, y, z) <= 0;
            }

            public boolean isBlockNormalCube(int x, int y, int z) {
                int id = getBlockId(x, y, z);
                if (id <= 0) return false;
                btw.modern.Block block = btw.modern.Block.blocksList[id];
                return block != null && block.blockMaterial != null && block.blockMaterial.isOpaque() && block.renderAsNormalBlock();
            }

            /**
             * Vanilla 1.5.2 Entity.moveEntity walks each block in the
             * swept AABB and asks it for collision boxes. Without this
             * the entity falls through everything because the default
             * World.getCollidingBoundingBoxes returns an empty list.
             * Mirrors {@link WorldBridge#getCollidingBoundingBoxes}.
             */
            @Override
            @SuppressWarnings("unchecked")
            public java.util.List getCollidingBoundingBoxes(btw.modern.Entity entity, btw.modern.AxisAlignedBB aabb) {
                java.util.List list = new java.util.ArrayList();
                if (level == null) return list;
                int minX = btw.modern.MathHelper.floor_double(aabb.minX);
                int maxX = btw.modern.MathHelper.floor_double(aabb.maxX + 1.0D);
                int minY = btw.modern.MathHelper.floor_double(aabb.minY);
                int maxY = btw.modern.MathHelper.floor_double(aabb.maxY + 1.0D);
                int minZ = btw.modern.MathHelper.floor_double(aabb.minZ);
                int maxZ = btw.modern.MathHelper.floor_double(aabb.maxZ + 1.0D);
                for (int x = minX; x < maxX; x++) {
                    for (int z = minZ; z < maxZ; z++) {
                        for (int y = minY; y < maxY; y++) {
                            int blockId = getBlockId(x, y, z);
                            if (blockId > 0) {
                                btw.modern.Block block = btw.modern.Block.blocksList[blockId];
                                if (block != null) {
                                    block.addCollisionBoxesToList(this, x, y, z, aabb, list, entity);
                                }
                            }
                        }
                    }
                }
                return list;
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
            // 1.5.2 WorldClient + RenderGlobal sound contract (audit sounds S1). Client FC entities run
            // onUpdate / handleHealthUpdate / client events here (tick() below, FCPuppetSupport
            // .forwardEntityEvent): e.g. the cow kick, villager restock pop and zombie-cure moan play
            // through World.playSound, which WorldClient played locally (WorldClient.java:483-505).
            // playSoundEffect / playSoundAtEntity stay no-ops as in RenderGlobal (:1812, :1817): the
            // server broadcasts those.
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
            // 1.5.2 RenderGlobal.markBlockRangeForRenderUpdate re-meshed the range on the client (MPR-6).
            public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {
                if (clientSink) btw.forge.client.ClientFcWorld.markRangeDirty(x1, y1, z1, x2, y2, z2);
            }
            public boolean canBlockSeeTheSky(int x, int y, int z) { return true; }
            public btw.modern.WorldChunkManager getWorldChunkManager() { return null; }
            public boolean setBlockToAir(int x, int y, int z) { return false; }
            public boolean setBlockMetadata(int x, int y, int z, int meta, int flags) { return false; }
            public boolean canMineBlock(btw.modern.EntityPlayer p, int x, int y, int z) { return true; }
            public boolean doesBlockHaveSolidTopSurface(int x, int y, int z) {
                return isBlockNormalCube(x, y, z);
            }
            public int getSavedLightValue(btw.modern.EnumSkyBlock type, int x, int y, int z) { return 15; }
            public int getFullBlockLightValue(int x, int y, int z) { return 15; }
            public boolean checkChunksExist(int x1, int y1, int z1, int x2, int y2, int z2) { return true; }
            public boolean checkNoEntityCollision(btw.modern.AxisAlignedBB bb) { return true; }
            public void spawnParticle(String name, double x, double y, double z, double vx, double vy, double vz) {}
        };
    }

    // ------------------------------------------------------------------
    // Position / rotation synchronization
    // ------------------------------------------------------------------

    private void syncToFc() {
        if (fcEntity == null) return;
        // Use setPosition to keep boundingBox in sync — see ProxyMob.syncToFc.
        fcEntity.setPosition(getX(), getY(), getZ());
        fcEntity.prevPosX = xOld;
        fcEntity.prevPosY = yOld;
        fcEntity.prevPosZ = zOld;
        fcEntity.rotationYaw = getYRot();
        fcEntity.rotationPitch = getXRot();
        fcEntity.prevRotationYaw = yRotO;
        fcEntity.prevRotationPitch = xRotO;
        fcEntity.onGround = onGround();
        fcEntity.entityId = getId();
        fcEntity.motionX = getDeltaMovement().x;
        fcEntity.motionY = getDeltaMovement().y;
        fcEntity.motionZ = getDeltaMovement().z;
        fcEntity.ticksExisted = tickCount;
        fcEntity.fallDistance = fallDistance;
    }

    private void syncFromFc() {
        if (fcEntity == null) return;
        pullStateFromFc();
        if (fcEntity.isDead) {
            if (fcEntity instanceof btw.modern.EntityFallingSand && level() instanceof ServerLevel sl) {
                // FCMOD-INTEGRATION(1.20.1): falling_blocks: send the cell FC just landed in (or crushed, combined, left empty) to the
                // trackers before the removal, as 1.20.1 FallingBlockEntity.tick:154-156 does; the chunk's own broadcast comes a
                // tick later, and the client would draw neither for that tick
                sl.getChunkSource().broadcast(this, new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(sl,
                        net.minecraft.core.BlockPos.containing(fcEntity.posX, fcEntity.posY, fcEntity.posZ)));
            }
            discard();
        }
    }

    /** Position, rotation, motion and fall distance from the FC entity (the server writeback and the client fall). */
    private void pullStateFromFc() {
        setPos(fcEntity.posX, fcEntity.posY, fcEntity.posZ);
        setYRot(fcEntity.rotationYaw);
        setXRot(fcEntity.rotationPitch);
        setDeltaMovement(fcEntity.motionX, fcEntity.motionY, fcEntity.motionZ);
        fallDistance = fcEntity.fallDistance;
    }

    /** Client: this proxy simulates FC's falling block itself (CF2-R1), as 1.5.2 and 1.20.1 do. */
    private boolean simulatesFallOnClient() {
        return level().isClientSide && fcEntity instanceof btw.modern.EntityFallingSand
                && !fcStateless && !fcClientFallFailed;
    }

    /**
     * Client: a server position that lies on the client's own fall, at most about a tick ahead or behind (|dx|, |dz|
     * below 1e-3; |dy| within two of the client's last fall steps plus 0.05, which also covers the 1/4096 packet
     * quantization and a client that already rests where the server is about to land).
     */
    private boolean fitsClientFall(double x, double y, double z) {
        double band = 2.0D * Math.max(clientFallStep, Math.abs(getDeltaMovement().y)) + 0.05D;
        return Math.abs(x - getX()) < 1.0E-3D && Math.abs(z - getZ()) < 1.0E-3D && Math.abs(y - getY()) <= band;
    }

    // FCMOD-INTEGRATION(1.20.1): falling_blocks: a server position update that fits the client's own fall is not applied (Entity.lerpTo
    // snaps, and a snap a tick off the client's phase is a visible hitch); anything else (a crush, a teleport, a real divergence) is applied
    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        if (simulatesFallOnClient()) {
            if (fitsClientFall(x, y, z)) return;
            // a real correction: the server's position and the motion sent just before it (ServerEntity.sendChanges sends
            // the motion packet first), so the client's fall goes on in the server's phase
            super.lerpTo(x, y, z, yRot, xRot, steps, teleport);
            if (serverFallMotion != null) setDeltaMovement(serverFallMotion);
            return;
        }
        super.lerpTo(x, y, z, yRot, xRot, steps, teleport);
    }

    // FCMOD-INTEGRATION(1.20.1): falling_blocks: the server's motion for a client-simulated fall lags it by up to a step; keep the client's
    @Override
    public void lerpMotion(double x, double y, double z) {
        if (simulatesFallOnClient() && Math.abs(x - getDeltaMovement().x) < 1.0E-3D
                && Math.abs(z - getDeltaMovement().z) < 1.0E-3D) {
            serverFallMotion = new net.minecraft.world.phys.Vec3(x, y, z);
            return;
        }
        super.lerpMotion(x, y, z);
    }

    // ------------------------------------------------------------------
    // Lifecycle overrides
    // ------------------------------------------------------------------

    @Override
    public void tick() {
        // FCMOD-INTEGRATION(1.20.1): fc_entity_construction: a positional FC entity without state has nothing to run (a /summon, a save without FCData, or unreadable FCData)
        if (fcEntity == null && !level().isClientSide && needsSavedState()) {
            removeStateless();
            return;
        }
        syncToFc();
        boolean fallFailed = false;
        if (fcEntity != null && !fcStateless) {
            try {
                fcEntity.onUpdate();
            } catch (Throwable e) {
                fallFailed = true;
                // Throwable, not Exception: a NoSuchMethodError/NoClassDefFoundError
                // from a bridge gap is an Error and would kill the server tick.
                // Logged once per (class, exception): this runs every tick on both sides.
                if (LOGGED_TICK_FAILURES.add(fcEntity.getClass().getName() + '|' + e.getClass().getName())) {
                    LOGGER.warn("FC entity onUpdate() threw: {}: {}",
                            e.getClass().getSimpleName(), e.getMessage());
                }
            }
        }
        // Position/rotation writeback is SERVER-ONLY, except for an FC falling block (below). On the client the
        // modern entity's position is driven by the server via the entity tracker + vanilla interpolation (lerp);
        // writing the client-side FC re-simulation back with setPos/setYRot every tick fought that interpolation
        // and caused rubber-banding when entities move or turn. onUpdate still runs client-side (above) so FC
        // animation state (e.g. windmill/waterwheel rotation) keeps advancing; only the authoritative
        // position/rotation stays server-driven. Mirrors the mob proxies.
        if (!level().isClientSide) {
            syncFromFc();
            // Periodically broadcast FC entity state (NBT + DataWatcher) to
            // tracking clients so dynamic state like windmill rotation speed
            // stays in sync after storms / overpower events.
            if (fcEntity != null && (tickCount % 10) == 0) {
                BTWNetwork.broadcastFCEntityState(this, fcEntity);
            }
        } else if (simulatesFallOnClient()) {
            if (fallFailed) {
                // the client cannot run FC's fall: back to the server's position updates (Entity.lerpTo), as before CF2-R1
                fcClientFallFailed = true;
            } else {
                // FCMOD-INTEGRATION(1.20.1): falling_blocks: the client simulates the fall as 1.5.2 (NetClientHandler.java:290-296, FCEntityFallingBlock.onUpdate
                // on both sides) and 1.20.1 (FallingBlockEntity.tick:115-119) do; lerpTo/lerpMotion keep it unless the server disagrees
                double step = Math.abs(fcEntity.posY - getY());
                if (step > 1.0E-4D) clientFallStep = step;
                pullStateFromFc();
                setOnGround(fcEntity.onGround);
            }
        }
        super.tick();
    }

    /** Server: this proxy's FC class can be built only from saved state (FcEntityFactory POSITIONAL). */
    private boolean needsSavedState() {
        String cn = getFcClassName();
        return cn != null && !cn.isEmpty() && FcEntityFactory.kindOf(cn) == FcEntityFactory.Kind.POSITIONAL;
    }

    /** Server: removes a positional FC entity that has no FC state to run, with one INFO line per (class, reason). */
    private void removeStateless() {
        String cn = getFcClassName();
        String simple = cn.substring(cn.lastIndexOf('.') + 1);
        String reason = fcMissingStateReason != null ? fcMissingStateReason
                : "with no BTW state (a /summon or a save from an older build)";
        if (FCPuppetSupport.firstFailure("stateless|" + reason + '|' + cn)) {
            LOGGER.info("[FC-ENTITY] Removed a {} {}; further ones are not logged", simple, reason);
        }
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (fcEntity != null) {
            btw.modern.DamageSource fcSource = ProxyMob.translateDamageSource(source);
            return fcEntity.attackEntityFrom(fcSource, (int) amount);
        }
        return false;
    }

    // 1.5.2 Entity.canBeCollidedWith — consulted by EntityRenderer.getMouseOver
    // and the world attack/use ray-traces. Without this bridge leg, MC's
    // Entity.isPickable() default (false) makes every plain-proxy FC entity
    // (FCEntityMechPower windmills/water wheels, FCEntityCanvas, moving
    // platforms) untargetable, so their attackEntityFrom overrides never run.
    @Override
    public boolean isPickable() {
        return fcEntity != null && fcEntity.canBeCollidedWith();
    }

    // 1.5.2 Entity.canBePushed — parity bridge leg for MC's push logic.
    @Override
    public boolean isPushable() {
        return fcEntity != null && fcEntity.canBePushed();
    }

    // 1.5.2 EntityPlayer.interactWith -> entity.interact(player). Live FC
    // caller: FCEntityWindMillVertical.interact (sail dyeing, Common
    // FCEntityWindMillVertical.java:133) and FCEntityWindMill likewise.
    // Mirrors ProxyMob.mobInteract.
    @Override
    public net.minecraft.world.InteractionResult interact(
            net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand) {
        if (fcEntity != null && hand == net.minecraft.world.InteractionHand.MAIN_HAND
                && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            btw.forge.PlayerBridge pb = btw.forge.PlayerBridge.getOrCreate(sp);
            try {
                boolean handled = fcEntity.interact(pb);
                // FCEntityWindMill(Vertical).interact uses one dye in place (stackSize--) and writes the slot itself
                // only when it empties; land the edit now, as ProxyAnimal/ProxyMob.mobInteract do
                pb.syncInventoryToReal();
                if (handled) {
                    return net.minecraft.world.InteractionResult.SUCCESS;
                }
            } catch (Throwable t) {
                LOGGER.warn("FC entity interact() threw: {}: {}",
                        t.getClass().getSimpleName(), t.getMessage());
                // INV-RESYNC: a failed FC call lands nothing; the next sync dropped its edits, it would now write them
                pb.discardInventoryEdits();
            }
        }
        return super.interact(player, hand);
    }

    /**
     * Cached reflective handle for the frozen 1.5.2
     * Entity.onCollideWithPlayer(EntityPlayer). The frozen btw.modern.Entity
     * declares it (verified via javap on the fc class set), but the
     * compile-time Modern-Common shim does not, so the call cannot be made
     * directly — see the deferred Modern-Common Entity.java declaration.
     */
    private java.lang.reflect.Method onCollideWithPlayerMethod;

    // 1.5.2 EntityPlayer.onUpdate -> collideWithNearbyEntities ->
    // Entity.onCollideWithPlayer. Live FC paths: frozen
    // EntityXPOrb.onCollideWithPlayer (XP collection — Dragon Orbs from
    // EntityLiving.onDeathUpdate:728, FCTileEntityArcaneVessel:342/365,
    // FCTileEntityHopper:839) and frozen EntityArrow.onCollideWithPlayer
    // (arrow pickup).
    @Override
    public void playerTouch(net.minecraft.world.entity.player.Player player) {
        if (fcEntity != null && !level().isClientSide
                && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            PlayerBridge pb = null;
            try {
                java.lang.reflect.Method m = onCollideWithPlayerMethod;
                if (m == null) {
                    m = fcEntity.getClass().getMethod(
                            "onCollideWithPlayer", btw.modern.EntityPlayer.class);
                    onCollideWithPlayerMethod = m;
                }
                pb = PlayerBridge.getOrCreate(sp);
                m.invoke(fcEntity, pb);
                // Arrow pickup puts the arrow into the FC inventory snapshot (vanilla/server
                // EntityArrow.java:516 AddArrowToPlayerInv -> :600 addItemStackToInventory, which
                // fills mainInventory[] directly) and then setDead()s the arrow (:526). Without
                // this write-back the next syncFromReal rebuilt the slots from MC: the arrow
                // vanished from the world AND the inventory (composite-bow / broadhead arrows).
                // Only after a normal return: a throw before setDead() leaves the arrow alive, so
                // writing its item back would hand it out again on the next touch.
                pb.syncInventoryToReal();
                // Orb/arrow pickup calls setDead() — discard the proxy now
                // rather than waiting for the next tick.
                syncFromFc();
            } catch (Throwable t) {
                // INV-RESYNC: the "only after a normal return" rule above. A throw before setDead() leaves the arrow
                // alive, so drop the arrow FC already put into the snapshot, as the next sync dropped it before; it
                // would now write it, one free arrow per touch tick.
                if (pb != null) pb.discardInventoryEdits();
                Throwable root = t;
                while (root.getCause() != null && root.getCause() != root) {
                    root = root.getCause();
                }
                // Once per (class, exception): this runs every tick while the player touches it.
                if (LOGGED_TICK_FAILURES.add("touch|" + fcEntity.getClass().getName() + '|' + root.getClass().getName())) {
                    LOGGER.warn("FC entity onCollideWithPlayer() threw: {}: {}",
                            root.getClass().getSimpleName(), root.getMessage(), root);
                }
            }
        }
        super.playerTouch(player);
    }

    // ------------------------------------------------------------------
    // Required abstract implementations
    // ------------------------------------------------------------------

    @Override
    protected void defineSynchedData() {
        // No synched entity data — FC class name is sent via IEntityAdditionalSpawnData
    }

    // ------------------------------------------------------------------
    // IEntityAdditionalSpawnData — sends FC class name with spawn packet
    // ------------------------------------------------------------------

    @Override
    public void writeSpawnData(net.minecraft.network.FriendlyByteBuf buf) {
        buf.writeUtf(fcClassName);
        FCEntityStateCodec.writeState(buf, fcEntity);
    }

    @Override
    public void readSpawnData(net.minecraft.network.FriendlyByteBuf buf) {
        fcClassName = buf.readUtf();
        if (!fcClassName.isEmpty()) {
            // CF2-R1: FCEntityStateCodec.writeState writes true first when the server's FC entity exists
            ensureFcEntity(buf.isReadable() && buf.getBoolean(buf.readerIndex()));
        }
        FCEntityStateCodec.applyState(buf, fcEntity);
        if (level().isClientSide) {
            playClientSpawnSound();
        }
    }

    /**
     * The sounds 1.5.2 played on the client when the spawn packet of a spider's web shot or a thrown soul-sand
     * pile arrived: Client FCBetterThanWolves.java:2653 ("mob.slime.attack") and :2675 ("dig.sand"), in FC's
     * custom spawn-packet handler. The port spawns both through Forge's spawn packet, so that handler never
     * runs, and the server plays nothing for either (FCEntitySpider.SpitWeb :261-269,
     * FCItemPileSoulSand.onItemRightClick :86-96). Like 1.5.2, they play each time a client starts tracking the
     * entity. Forge has already placed the entity (PlayMessages.SpawnEntity.handle), on the main thread.
     */
    private void playClientSpawnSound() {
        String className = getFcClassName();
        if (className == null) return;
        String simpleName = className.substring(className.lastIndexOf('.') + 1);
        try {
            net.minecraft.util.RandomSource rand = level().random;
            if (simpleName.equals("FCEntitySpiderWeb")) {
                btw.forge.client.LegacyClientSound.playLocal(getX(), getY(), getZ(), "mob.slime.attack", 1.0F,
                        (rand.nextFloat() - rand.nextFloat()) * 0.2F + 0.6F, false);
            } else if (simpleName.equals("FCEntitySoulSand")) {
                btw.forge.client.LegacyClientSound.playLocal(getX(), getY(), getZ(), "dig.sand", 1.0F,
                        1.0F + (rand.nextFloat() - rand.nextFloat()) * 0.2F, false);
            }
        } catch (Throwable t) {
            if (LOGGED_TICK_FAILURES.add("spawnsound|" + simpleName + '|' + t.getClass().getName())) {
                LOGGER.warn("FC spawn sound for {} failed: {}: {}", simpleName,
                        t.getClass().getSimpleName(), t.getMessage());
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("FCClassName")) {
            fcClassName = tag.getString("FCClassName");
            if (fcEntity == null && !fcClassName.isEmpty()) {
                // FCMOD-INTEGRATION(1.20.1): fc_entity_construction: a positional FC class is rebuilt from its saved state (FCData): a block saved mid-fall falls on (ruling OD1)
                // and lands by FC's rules; without FCData it is not built, and its first tick removes it
                boolean hasState = tag.contains("FCData");
                if (hasState && !REBUILD_SAVED_POSITIONAL && !level().isClientSide
                        && FcEntityFactory.kindOf(fcClassName) == FcEntityFactory.Kind.POSITIONAL) {
                    fcMissingStateReason = "saved while falling (BTW 4.B's rule: REBUILD_SAVED_POSITIONAL is off)";
                }
                ensureFcEntity(REBUILD_SAVED_POSITIONAL && hasState);
            }
        }
        if (fcEntity != null && tag.contains("FCData")) {
            CompoundTag fcCompound = tag.getCompound("FCData");
            ForgeNBTCompound wrapper = new ForgeNBTCompound(fcCompound);
            try {
                fcEntity.readFromNBT(wrapper);
                fcEntity.readEntityFromNBT(wrapper);
            } catch (Throwable e) {
                Throwable root = e;
                while (root.getCause() != null && root.getCause() != root) {
                    root = root.getCause();
                }
                LOGGER.warn("Failed to read FC entity NBT data for {}: {}: {}",
                        fcClassName, root.getClass().getSimpleName(), root.getMessage());
                // CF2-R1: a positional FC entity has no state but its save (FCEntityFallingBlock would run as block 0 and
                // never land); drop it, the first tick removes the proxy
                if (!level().isClientSide && FcEntityFactory.kindOf(fcClassName) == FcEntityFactory.Kind.POSITIONAL) {
                    fcEntity = null;
                    fcBuildFailed = true;
                    fcMissingStateReason = "whose saved BTW state could not be read";
                }
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("FCClassName", fcClassName);
        if (fcEntity != null) {
            CompoundTag fcCompound = new CompoundTag();
            ForgeNBTCompound wrapper = new ForgeNBTCompound(fcCompound);
            try {
                fcEntity.writeToNBT(wrapper);
                fcEntity.writeEntityToNBT(wrapper);
            } catch (Throwable e) {
                Throwable root = e;
                while (root.getCause() != null && root.getCause() != root) {
                    root = root.getCause();
                }
                LOGGER.warn("Failed to write FC entity NBT data for {}: {}: {}",
                        fcClassName, root.getClass().getSimpleName(), root.getMessage());
            }
            tag.put("FCData", fcCompound);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);
    }
}
