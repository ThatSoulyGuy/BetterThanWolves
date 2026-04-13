package btw.forge;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.WeakHashMap;

/**
 * Bridges vanilla Minecraft {@link Entity} to {@link btw.modern.Entity}.
 * FC code interacts with entities through btw.modern.Entity fields
 * and methods; this bridge synchronizes state in both directions.
 *
 * Used for vanilla entities that don't have a dedicated FC proxy
 * (ProxyMob, ProxyAnimal, etc.) but still need to interact with
 * FC block callbacks like onEntityCollidedWithBlock.
 *
 * Instances are cached per-Entity via {@link #getOrCreate(Entity)}.
 */
public class EntityBridge extends btw.modern.Entity {
    private final Entity realEntity;
    private static final WeakHashMap<Entity, EntityBridge> cache = new WeakHashMap<>();

    /** Concrete override of abstract Entity.entityInit() — see EntityLiving for rationale. */
    @Override
    public void entityInit() {}

    /**
     * Returns (or creates) the EntityBridge for the given Entity.
     */
    public static EntityBridge getOrCreate(Entity entity) {
        return cache.computeIfAbsent(entity, EntityBridge::new);
    }

    private EntityBridge(Entity entity) {
        super(null); // btw.modern.Entity(World) constructor
        this.realEntity = entity;
        // World bridge — use ServerLevel if available
        if (entity.level() instanceof ServerLevel sl) {
            this.worldObj = WorldBridge.getOrCreate(sl);
        }
        syncFromReal();
    }

    /**
     * Sync state FROM real MC entity TO FC entity (call before FC callbacks).
     */
    public void syncFromReal() {
        this.posX = realEntity.getX();
        this.posY = realEntity.getY();
        this.posZ = realEntity.getZ();
        this.prevPosX = realEntity.xOld;
        this.prevPosY = realEntity.yOld;
        this.prevPosZ = realEntity.zOld;
        this.motionX = realEntity.getDeltaMovement().x;
        this.motionY = realEntity.getDeltaMovement().y;
        this.motionZ = realEntity.getDeltaMovement().z;
        this.rotationYaw = realEntity.getYRot();
        this.rotationPitch = realEntity.getXRot();
        this.prevRotationYaw = realEntity.yRotO;
        this.prevRotationPitch = realEntity.xRotO;
        this.onGround = realEntity.onGround();
        this.entityId = realEntity.getId();
        this.ticksExisted = realEntity.tickCount;
        this.fallDistance = realEntity.fallDistance;
        this.isDead = realEntity.isRemoved();
        this.width = realEntity.getBbWidth();
        this.height = realEntity.getBbHeight();
        this.isInWeb = false; // MC 1.20.1 handles this differently
        this.fire = realEntity.getRemainingFireTicks();

        // Update world reference in case entity changed dimensions
        if (realEntity.level() instanceof ServerLevel sl) {
            this.worldObj = WorldBridge.getOrCreate(sl);
        }
    }

    /**
     * Sync state FROM FC entity TO real MC entity (call after FC callbacks).
     */
    public void syncToReal() {
        // Sync velocity if FC modified it (knockback, fling)
        if (this.motionX != realEntity.getDeltaMovement().x ||
            this.motionY != realEntity.getDeltaMovement().y ||
            this.motionZ != realEntity.getDeltaMovement().z) {
            realEntity.setDeltaMovement(this.motionX, this.motionY, this.motionZ);
        }
        // Sync fire state
        if (this.fire > 0 && realEntity.getRemainingFireTicks() <= 0) {
            realEntity.setRemainingFireTicks(this.fire);
        }
        // If FC code marked entity as dead
        if (this.isDead && !realEntity.isRemoved()) {
            realEntity.discard();
        }
    }

    /**
     * Returns the underlying Forge Entity.
     */
    public Entity getRealEntity() {
        return realEntity;
    }

    // ================================================================
    // Override methods that need to reach the real entity
    // ================================================================

    @Override
    public boolean isSneaking() {
        return realEntity.isShiftKeyDown();
    }

    @Override
    public boolean isSprinting() {
        return realEntity.isSprinting();
    }

    @Override
    public boolean isInWater() {
        return realEntity.isInWater();
    }

    @Override
    public boolean isBurning() {
        return realEntity.isOnFire();
    }

    @Override
    public boolean handleLavaMovement() {
        return realEntity.isInLava();
    }

    @Override
    public void setFire(int seconds) {
        realEntity.setRemainingFireTicks(seconds * 20);
        this.fire = seconds * 20;
    }

    @Override
    public void extinguish() {
        realEntity.clearFire();
        this.fire = 0;
    }

    @Override
    public void setDead() {
        super.setDead();
        realEntity.discard();
    }

    @Override
    public void addVelocity(double x, double y, double z) {
        this.motionX += x;
        this.motionY += y;
        this.motionZ += z;
        realEntity.setDeltaMovement(
            realEntity.getDeltaMovement().add(x, y, z));
    }

    @Override
    public void setVelocity(double x, double y, double z) {
        this.motionX = x;
        this.motionY = y;
        this.motionZ = z;
        realEntity.setDeltaMovement(x, y, z);
    }

    @Override
    public double getDistanceSq(double x, double y, double z) {
        return realEntity.distanceToSqr(x, y, z);
    }

    @Override
    public boolean isEntityAlive() {
        return realEntity.isAlive();
    }

    @Override
    public boolean canBeCollidedWith() {
        return realEntity.isPickable();
    }

    @Override
    public boolean canBePushed() {
        return realEntity.isPushable();
    }

    @Override
    public void playSound(String sound, float volume, float pitch) {
        SoundMapping.playAtEntity(realEntity, sound, volume, pitch);
    }

    @Override
    public boolean attackEntityFrom(btw.modern.DamageSource damageSource, int amount) {
        // For non-living entities, damage is handled differently
        // Living entities should use LivingEntityBridge instead
        return false;
    }

    @Override
    public String getEntityName() {
        return realEntity.getName().getString();
    }

    @Override
    public boolean IsItemEntity() {
        return realEntity instanceof net.minecraft.world.entity.item.ItemEntity;
    }
}
