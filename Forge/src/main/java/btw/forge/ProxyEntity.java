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
        if (fcEntity == null && level().isClientSide && !fcClassName.isEmpty()) {
            createClientFcEntity();
        }
        return fcEntity;
    }

    public String getFcClassName() {
        return fcClassName;
    }

    /**
     * Creates a lightweight FC entity on the client for rendering.
     * The entity has position/rotation synced but no game logic.
     */
    private void createClientFcEntity() {
        String className = getFcClassName();
        if (className == null || className.isEmpty()) return;
        try {
            Class<?> fcClass = Class.forName(className);
            // Try constructor(World) — most FC entities use this
            try {
                var ctor = fcClass.getConstructor(btw.modern.World.class);
                fcEntity = (btw.modern.Entity) ctor.newInstance((btw.modern.World) null);
            } catch (NoSuchMethodException e) {
                // Try no-arg constructor
                var ctor = fcClass.getDeclaredConstructor();
                ctor.setAccessible(true);
                fcEntity = (btw.modern.Entity) ctor.newInstance();
            }
            syncToFc();
        } catch (Exception e) {
            LOGGER.debug("Could not create client FC entity {}: {}", className, e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Position / rotation synchronization
    // ------------------------------------------------------------------

    private void syncToFc() {
        if (fcEntity == null) return;
        fcEntity.posX = getX();
        fcEntity.posY = getY();
        fcEntity.posZ = getZ();
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
            try {
                fcEntity.onUpdate();
            } catch (Exception e) {
                LOGGER.debug("FC entity onUpdate() threw: {}", e.getMessage());
            }
        }
        syncFromFc();
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
    }

    @Override
    public void readSpawnData(net.minecraft.network.FriendlyByteBuf buf) {
        fcClassName = buf.readUtf();
        if (!fcClassName.isEmpty()) {
            createClientFcEntity();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (fcEntity != null && tag.contains("FCData")) {
            CompoundTag fcCompound = tag.getCompound("FCData");
            ForgeNBTCompound wrapper = new ForgeNBTCompound(fcCompound);
            try {
                fcEntity.readFromNBT(wrapper);
                fcEntity.readEntityFromNBT(wrapper);
            } catch (Exception e) {
                LOGGER.warn("Failed to read FC entity NBT data: {}", e.getMessage());
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (fcEntity != null) {
            CompoundTag fcCompound = new CompoundTag();
            ForgeNBTCompound wrapper = new ForgeNBTCompound(fcCompound);
            try {
                fcEntity.writeToNBT(wrapper);
                fcEntity.writeEntityToNBT(wrapper);
            } catch (Exception e) {
                LOGGER.warn("Failed to write FC entity NBT data: {}", e.getMessage());
            }
            tag.put("FCData", fcCompound);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);
    }
}
