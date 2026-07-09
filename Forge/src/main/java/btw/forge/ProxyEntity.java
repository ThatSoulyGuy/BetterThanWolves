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

    private btw.modern.Entity fcEntity;
    private String fcClassName = "";

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
        if (fcEntity == null) {
            ensureFcEntity();
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
     * Creates the FC entity if missing — works on both client and server.
     * On server: uses a real WorldBridge so FC logic functions.
     * On client: uses a dummy world for rendering only.
     */
    private void ensureFcEntity() {
        String className = getFcClassName();
        if (className == null || className.isEmpty()) return;
        btw.modern.World world;
        if (level().isClientSide) {
            world = createDummyClientWorld(level());
        } else if (level() instanceof ServerLevel sl) {
            world = WorldBridge.getOrCreate(sl);
        } else {
            return;
        }
        try {
            Class<?> fcClass = Class.forName(className);
            try {
                var ctor = fcClass.getConstructor(btw.modern.World.class);
                fcEntity = (btw.modern.Entity) ctor.newInstance(world);
            } catch (NoSuchMethodException e) {
                var ctor = fcClass.getDeclaredConstructor();
                ctor.setAccessible(true);
                fcEntity = (btw.modern.Entity) ctor.newInstance();
            }
            if (fcEntity != null) {
                fcEntity.worldObj = world;
                syncToFc();
            }
        } catch (Throwable e) {
            LOGGER.info("Could not create FC entity {}: {}", className, e.getMessage(), e);
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
                net.minecraft.world.level.block.state.BlockState s =
                        level.getBlockState(new net.minecraft.core.BlockPos(x, y, z));
                if (s.hasProperty(ProxyBlock.META)) return s.getValue(ProxyBlock.META);
                return 0;
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
        setPos(fcEntity.posX, fcEntity.posY, fcEntity.posZ);
        setYRot(fcEntity.rotationYaw);
        setXRot(fcEntity.rotationPitch);
        setDeltaMovement(fcEntity.motionX, fcEntity.motionY, fcEntity.motionZ);
        fallDistance = fcEntity.fallDistance;
        if (fcEntity.isDead) {
            discard();
        }
    }

    // ------------------------------------------------------------------
    // Lifecycle overrides
    // ------------------------------------------------------------------

    @Override
    public void tick() {
        syncToFc();
        if (fcEntity != null) {
            boolean wasDead = fcEntity.isDead;
            try {
                fcEntity.onUpdate();
            } catch (Throwable e) {
                // Throwable, not Exception: a NoSuchMethodError/NoClassDefFoundError
                // from a bridge gap is an Error and would kill the server tick.
                LOGGER.warn("FC entity onUpdate() threw: {}: {}",
                        e.getClass().getSimpleName(), e.getMessage());
            }
            if (!wasDead && fcEntity.isDead) {
                LOGGER.info("[ENTITY-DIED] {} at ({},{},{}) on tick after onUpdate. worldObj={} worldRemote={}",
                        fcEntity.getClass().getSimpleName(),
                        String.format("%.1f", fcEntity.posX), String.format("%.1f", fcEntity.posY), String.format("%.1f", fcEntity.posZ),
                        fcEntity.worldObj != null ? fcEntity.worldObj.getClass().getSimpleName() : "null",
                        fcEntity.worldObj != null ? fcEntity.worldObj.isRemote : "N/A");
                // Log what block is at the center for mech power entities
                if (fcEntity.worldObj != null) {
                    int cx = btw.modern.MathHelper.floor_double(fcEntity.posX);
                    int cy = btw.modern.MathHelper.floor_double(fcEntity.posY);
                    int cz = btw.modern.MathHelper.floor_double(fcEntity.posZ);
                    int centerId = fcEntity.worldObj.getBlockId(cx, cy, cz);
                    LOGGER.info("[ENTITY-DIED] center block at ({},{},{}) = id {} ({})",
                            cx, cy, cz, centerId,
                            centerId > 0 && centerId < btw.modern.Block.blocksList.length && btw.modern.Block.blocksList[centerId] != null
                                    ? btw.modern.Block.blocksList[centerId].getClass().getSimpleName() : "null");
                }
            }
        }
        // Position/rotation writeback is SERVER-ONLY. On the client the modern
        // entity's position is driven by the server via the entity tracker + vanilla
        // interpolation (lerp); writing the client-side FC re-simulation back with
        // setPos/setYRot every tick fought that interpolation and caused rubber-banding
        // when entities move or turn. onUpdate still runs client-side (above) so FC
        // animation state (e.g. windmill/waterwheel rotation) keeps advancing; only the
        // authoritative position/rotation stays server-driven. Mirrors the mob proxies.
        if (!level().isClientSide) {
            syncFromFc();
            // Periodically broadcast FC entity state (NBT + DataWatcher) to
            // tracking clients so dynamic state like windmill rotation speed
            // stays in sync after storms / overpower events.
            if (fcEntity != null && (tickCount % 10) == 0) {
                BTWNetwork.broadcastFCEntityState(this, fcEntity);
            }
        }
        super.tick();
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
                if (fcEntity.interact(pb)) {
                    return net.minecraft.world.InteractionResult.SUCCESS;
                }
            } catch (Throwable t) {
                LOGGER.warn("FC entity interact() threw: {}: {}",
                        t.getClass().getSimpleName(), t.getMessage());
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
            try {
                java.lang.reflect.Method m = onCollideWithPlayerMethod;
                if (m == null) {
                    m = fcEntity.getClass().getMethod(
                            "onCollideWithPlayer", btw.modern.EntityPlayer.class);
                    onCollideWithPlayerMethod = m;
                }
                m.invoke(fcEntity, btw.forge.PlayerBridge.getOrCreate(sp));
                // Orb/arrow pickup calls setDead() — discard the proxy now
                // rather than waiting for the next tick.
                syncFromFc();
            } catch (Throwable t) {
                Throwable root = t;
                while (root.getCause() != null && root.getCause() != root) {
                    root = root.getCause();
                }
                LOGGER.warn("FC entity onCollideWithPlayer() threw: {}: {}",
                        root.getClass().getSimpleName(), root.getMessage());
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
            ensureFcEntity();
        }
        FCEntityStateCodec.applyState(buf, fcEntity);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("FCClassName")) {
            fcClassName = tag.getString("FCClassName");
            if (fcEntity == null && !fcClassName.isEmpty()) {
                ensureFcEntity();
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
