package btw.forge;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.lang.ref.WeakReference;
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
    // Weak: this bridge is the value of a WeakHashMap keyed by the same entity, so a strong
    // reference pinned the key and the cache never evicted -- every wrapped entity (and through
    // it its level and server) stayed reachable for the life of the JVM (triage RE2). While the
    // entity is loaded its level keeps it alive; a bridge that outlives it reads as dead.
    private final WeakReference<Entity> realEntityRef;
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

    /** Drops every cached bridge; called once the server has stopped (BTWLifecycle.onServerStopped). */
    static void clearCache() {
        cache.clear();
    }

    private EntityBridge(Entity entity) {
        super(null); // btw.modern.Entity(World) constructor
        this.realEntityRef = new WeakReference<>(entity);
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
        Entity realEntity = real();
        if (realEntity == null) {
            this.isDead = true;
            return;
        }
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
        // FCMOD-INTEGRATION(1.20.1): fc_entity_views: the box from the entity (the frozen constructor's is empty)
        net.minecraft.world.phys.AABB bb = realEntity.getBoundingBox();
        this.boundingBox.setBounds(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
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
        Entity realEntity = real();
        if (realEntity == null) return;
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
     * Returns the underlying Forge Entity, or null once it has been unloaded and collected.
     */
    public Entity getRealEntity() {
        return real();
    }

    // Null while super(null) runs (field not yet assigned) or after the entity was collected.
    private Entity real() {
        return realEntityRef != null ? realEntityRef.get() : null;
    }

    // ================================================================
    // Override methods that need to reach the real entity
    // ================================================================

    @Override
    public boolean isSneaking() {
        Entity realEntity = real();
        return realEntity != null && realEntity.isShiftKeyDown();
    }

    @Override
    public boolean isSprinting() {
        Entity realEntity = real();
        return realEntity != null && realEntity.isSprinting();
    }

    @Override
    public boolean isInWater() {
        Entity realEntity = real();
        return realEntity != null && realEntity.isInWater();
    }

    @Override
    public boolean isBurning() {
        Entity realEntity = real();
        return realEntity != null && realEntity.isOnFire();
    }

    @Override
    public boolean handleLavaMovement() {
        Entity realEntity = real();
        return realEntity != null && realEntity.isInLava();
    }

    @Override
    public void setFire(int seconds) {
        Entity realEntity = real();
        if (realEntity != null) {
            realEntity.setRemainingFireTicks(seconds * 20);
        }
        this.fire = seconds * 20;
    }

    @Override
    public void extinguish() {
        Entity realEntity = real();
        if (realEntity != null) {
            realEntity.clearFire();
        }
        this.fire = 0;
    }

    @Override
    public void setDead() {
        super.setDead();
        Entity realEntity = real();
        if (realEntity != null) {
            realEntity.discard();
        }
    }

    @Override
    public void addVelocity(double x, double y, double z) {
        this.motionX += x;
        this.motionY += y;
        this.motionZ += z;
        Entity realEntity = real();
        if (realEntity != null) {
            realEntity.setDeltaMovement(
                realEntity.getDeltaMovement().add(x, y, z));
        }
    }

    @Override
    public void setVelocity(double x, double y, double z) {
        this.motionX = x;
        this.motionY = y;
        this.motionZ = z;
        Entity realEntity = real();
        if (realEntity != null) {
            realEntity.setDeltaMovement(x, y, z);
        }
    }

    @Override
    public double getDistanceSq(double x, double y, double z) {
        Entity realEntity = real();
        return realEntity != null ? realEntity.distanceToSqr(x, y, z) : super.getDistanceSq(x, y, z);
    }

    @Override
    public boolean isEntityAlive() {
        Entity realEntity = real();
        return realEntity != null && realEntity.isAlive();
    }

    @Override
    public boolean canBeCollidedWith() {
        Entity realEntity = real();
        return realEntity != null && realEntity.isPickable();
    }

    @Override
    public boolean canBePushed() {
        Entity realEntity = real();
        return realEntity != null && realEntity.isPushable();
    }

    @Override
    public void playSound(String sound, float volume, float pitch) {
        Entity realEntity = real();
        if (realEntity != null) {
            SoundMapping.playAtEntity(realEntity, sound, volume, pitch);
        }
    }

    /**
     * FC damage on the wrapped vanilla entity (an FC block callback hurting a boat, minecart or a mob wrapped
     * here, e.g. FC cactus damage): MC's own hurt() with the mapped source (WorldBridge.toMcDamageSource), as
     * 1.5.2 damaged the entity through its own attackEntityFrom (EntityBoat, EntityMinecart, EntityLiving).
     * Returns hurt()'s result; server only, false on the client; never throws (DamageSourceMapping.hurtFromFc).
     * FC's queries and contact callbacks hand out VanillaEntityViews' views of non-living 1.20.1 entities instead.
     */
    @Override
    public boolean attackEntityFrom(btw.modern.DamageSource damageSource, int amount) {
        return DamageSourceMapping.hurtFromFc(real(), damageSource, amount);
    }

    /**
     * FCMOD-INTEGRATION(1.20.1): fc_entity_views: FC mounting this entity on a 1.20.1 vehicle it found (a view of
     * VanillaEntityViews: the block dispenser's {@code riddenByEntity.mountEntity(cart)} before it takes a cart, Client
     * FCBlockBlockDispenser.java:292-297) rides or leaves the vehicle itself: leaving it when already on it (4.B
     * EntityPlayer.mountEntity's toggle), else mounting it. The view is never stored in ridingEntity (the vehicle is
     * 1.20.1's; see PlayerBridge.canPlayerEdit). Any other mount is FC's own, unchanged.
     */
    @Override
    public void mountEntity(btw.modern.Entity target) {
        net.minecraft.world.entity.Entity vehicle = VanillaEntityViews.realOf(target);
        if (vehicle == null) {
            super.mountEntity(target);
            return;
        }
        net.minecraft.world.entity.Entity self = real();
        if (self == null) return;
        if (self.getVehicle() == vehicle) {
            self.stopRiding();
        } else {
            self.startRiding(vehicle);
        }
    }

    @Override
    public String getEntityName() {
        Entity realEntity = real();
        return realEntity != null ? realEntity.getName().getString() : super.getEntityName();
    }

    @Override
    public boolean IsItemEntity() {
        return real() instanceof net.minecraft.world.entity.item.ItemEntity;
    }
}
