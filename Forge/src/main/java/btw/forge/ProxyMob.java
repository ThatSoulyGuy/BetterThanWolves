package btw.forge;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Forge-side proxy that extends {@link Mob} and forwards all lifecycle
 * callbacks to an FC {@link btw.modern.EntityLiving} instance.
 *
 * Used for FC entities whose legacy hierarchy roots at
 * {@link btw.modern.EntityMob} (creeper, zombie, skeleton, spider, blaze,
 * enderman, witch, wither, pig zombie, slime, etc.) as well as
 * {@link btw.modern.EntityFlying} (ghast) and other hostile mob variants.
 */
public class ProxyMob extends Mob
        implements net.minecraftforge.entity.IEntityAdditionalSpawnData {

    private static final Logger LOGGER = LogManager.getLogger("BTW-ProxyMob");

    private btw.modern.EntityLiving fcEntity;
    private String fcClassName = "";

    public ProxyMob(EntityType<? extends Mob> type, Level level) {
        super(type, level);
    }

    // ------------------------------------------------------------------
    // FC entity linkage
    // ------------------------------------------------------------------

    public void setFcEntity(btw.modern.EntityLiving fc) {
        this.fcEntity = fc;
        this.fcClassName = fc.getClass().getName();
        if (level() instanceof ServerLevel sl) {
            fc.worldObj = WorldBridge.getOrCreate(sl);
        }
        syncToFc();
    }

    public btw.modern.EntityLiving getFcEntity() {
        if (fcEntity == null && level().isClientSide && !fcClassName.isEmpty()) {
            createClientFcEntity();
        }
        if (fcEntity != null) syncToFc();
        return fcEntity;
    }

    public String getFcClassName() {
        if (fcClassName.isEmpty()) {
            fcClassName = BTWEntityRegistration.getFcClassName(getType());
        }
        return fcClassName;
    }

    private void createClientFcEntity() {
        if (fcClassName == null || fcClassName.isEmpty()) return;
        btw.modern.World dummyWorld = ProxyEntity.createDummyClientWorld();
        try {
            Class<?> fcClass = Class.forName(fcClassName);
            try {
                var ctor = fcClass.getConstructor(btw.modern.World.class);
                fcEntity = (btw.modern.EntityLiving) ctor.newInstance(dummyWorld);
            } catch (NoSuchMethodException e) {
                var ctor = fcClass.getDeclaredConstructor();
                ctor.setAccessible(true);
                fcEntity = (btw.modern.EntityLiving) ctor.newInstance();
            }
            if (fcEntity != null) fcEntity.worldObj = dummyWorld;
            syncToFc();
        } catch (Throwable e) {
            LOGGER.info("Could not create client FC entity {}: {}", fcClassName, e.getMessage(), e);
        }
    }

    @Override
    public void writeSpawnData(net.minecraft.network.FriendlyByteBuf buf) {
        buf.writeUtf(fcClassName);
    }

    @Override
    public void readSpawnData(net.minecraft.network.FriendlyByteBuf buf) {
        fcClassName = buf.readUtf();
        if (!fcClassName.isEmpty()) createClientFcEntity();
    }

    // ------------------------------------------------------------------
    // Position / rotation synchronization
    // ------------------------------------------------------------------

    /**
     * Pushes Forge entity state into the FC entity fields so that FC code
     * sees the current position, rotation, and flags.
     */
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

        // Animation fields — derived from MC entity's walk/body state
        fcEntity.renderYawOffset = yBodyRot;
        fcEntity.prevRenderYawOffset = yBodyRotO;
        fcEntity.rotationYawHead = yHeadRot;
        fcEntity.prevRotationYawHead = yHeadRotO;

        // Limb swing: compute from walk distance
        float dist = walkDist - walkDistO;
        fcEntity.limbYaw = Math.min(dist * 4.0F, 1.0F);
        fcEntity.prevLimbYaw = fcEntity.limbYaw;
        fcEntity.limbSwing = walkDist * 0.6662F;
    }

    /**
     * Pulls FC entity state back into the Forge entity so that movement
     * changes made by FC code are reflected in the modern engine.
     */
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
        // Let the Forge engine do its own processing (physics, etc.)
        super.tick();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (fcEntity != null) {
            btw.modern.DamageSource fcSource = translateDamageSource(source);
            boolean result = fcEntity.attackEntityFrom(fcSource, (int) amount);
            syncFromFc();
            return result;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        if (fcEntity != null) {
            btw.modern.DamageSource fcSource = translateDamageSource(source);
            fcEntity.onDeath(fcSource);
            syncFromFc();
        }
        super.die(source);
    }

    // ------------------------------------------------------------------
    // Attribute / health helpers
    // ------------------------------------------------------------------

    public static AttributeSupplier.Builder createProxyMobAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 2.0);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("FCClassName")) {
            fcClassName = tag.getString("FCClassName");
            if (fcEntity == null && !fcClassName.isEmpty()) {
                createClientFcEntity(); // recreate on server side too
            }
        }
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
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("FCClassName", fcClassName);
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

    // ------------------------------------------------------------------
    // DamageSource translation
    // ------------------------------------------------------------------

    static btw.modern.DamageSource translateDamageSource(DamageSource source) {
        String msgId = source.getMsgId();
        // Map common modern damage type message IDs to FC DamageSource types
        switch (msgId) {
            case "inFire":
                return btw.modern.DamageSource.inFire;
            case "onFire":
                return btw.modern.DamageSource.onFire;
            case "lava":
                return btw.modern.DamageSource.lava;
            case "inWall":
                return btw.modern.DamageSource.inWall;
            case "drown":
                return btw.modern.DamageSource.drown;
            case "starve":
                return btw.modern.DamageSource.starve;
            case "cactus":
                return btw.modern.DamageSource.cactus;
            case "fall":
                return btw.modern.DamageSource.fall;
            case "outOfWorld":
                return btw.modern.DamageSource.outOfWorld;
            case "magic":
                return btw.modern.DamageSource.magic;
            case "wither":
                return btw.modern.DamageSource.wither;
            case "anvil":
                return btw.modern.DamageSource.anvil;
            case "fallingBlock":
                return btw.modern.DamageSource.fallingBlock;
            default:
                return btw.modern.DamageSource.generic;
        }
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
        return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);
    }
}
