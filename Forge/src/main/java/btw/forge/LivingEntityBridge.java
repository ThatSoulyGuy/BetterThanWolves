package btw.forge;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/**
 * Bridges vanilla Minecraft {@link LivingEntity} to {@link btw.modern.EntityLiving}.
 * FC code interacts with living entities through btw.modern.EntityLiving fields
 * and methods; this bridge synchronizes state in both directions.
 *
 * Used for non-player living entities that need to interact with FC item
 * callbacks like hitEntity, onBlockDestroyed, and useItemOnEntity.
 *
 * For Player entities, use {@link PlayerBridge} instead (which also extends
 * EntityLiving via EntityPlayer).
 *
 * Instances are cached per-LivingEntity via {@link #getOrCreate(LivingEntity)}.
 *
 * Use {@link #wrapLiving(LivingEntity)} as a convenience that returns a
 * PlayerBridge when the entity is a Player, or a LivingEntityBridge otherwise.
 */
public class LivingEntityBridge extends btw.modern.EntityLiving {
    // Weak: this bridge is the value of a WeakHashMap keyed by the same entity, so a strong
    // reference pinned the key and the cache never evicted -- every wrapped entity (and through
    // it its level and server) stayed reachable for the life of the JVM (triage RE2). While the
    // entity is loaded its level keeps it alive; a bridge that outlives it reads as dead.
    private final WeakReference<LivingEntity> realEntityRef;
    private static final WeakHashMap<LivingEntity, LivingEntityBridge> cache = new WeakHashMap<>();

    /**
     * Holds the entity being wrapped while super(null) runs, so virtual calls
     * dispatched from EntityLiving's field initializers (notably
     * `health = this.getMaxHealth()`) can reach the real entity before our
     * subclass body has a chance to assign {@link #realEntityRef}.
     */
    private static final ThreadLocal<LivingEntity> CONSTRUCTING = new ThreadLocal<>();

    /**
     * Returns (or creates) the LivingEntityBridge for the given LivingEntity.
     * For Player entities, prefer {@link #wrapLiving(LivingEntity)} which
     * returns the correct PlayerBridge subtype.
     */
    public static LivingEntityBridge getOrCreate(LivingEntity entity) {
        return cache.computeIfAbsent(entity, e -> {
            CONSTRUCTING.set(e);
            try {
                return new LivingEntityBridge(e);
            } finally {
                CONSTRUCTING.remove();
            }
        });
    }

    /** Drops every cached bridge; called once the server has stopped (BTWLifecycle.onServerStopped). */
    static void clearCache() {
        cache.clear();
    }

    /**
     * Wraps any LivingEntity as a btw.modern.EntityLiving.
     * Returns a {@link PlayerBridge} if the entity is a Player,
     * otherwise returns a {@link LivingEntityBridge}.
     */
    public static btw.modern.EntityLiving wrapLiving(LivingEntity entity) {
        if (entity instanceof Player player) {
            return PlayerBridge.getOrCreate(player);
        }
        return getOrCreate(entity);
    }

    private LivingEntityBridge(LivingEntity entity) {
        super(null); // btw.modern.EntityLiving(World) constructor
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
        LivingEntity realEntity = real();
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
        // FCMOD-INTEGRATION(1.20.1): fc_entity_views: the box from the entity, as every view's: FC's queries hand this bridge
        // out (FCExplosionMining's getBlockDensity, FC projectiles' intercept, FCEntityCanvas' overlap test read it), and
        // the frozen constructor's box is empty at the origin
        net.minecraft.world.phys.AABB bb = realEntity.getBoundingBox();
        this.boundingBox.setBounds(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
        this.isInWeb = false; // MC 1.20.1 handles this differently
        this.fire = realEntity.getRemainingFireTicks();
        this.health = (int) realEntity.getHealth();

        // Update world reference in case entity changed dimensions
        if (realEntity.level() instanceof ServerLevel sl) {
            this.worldObj = WorldBridge.getOrCreate(sl);
        }
    }

    /**
     * Sync state FROM FC entity TO real MC entity (call after FC callbacks).
     */
    public void syncToReal() {
        LivingEntity realEntity = real();
        if (realEntity == null) return;
        // Sync velocity if FC modified it (knockback, fling)
        if (this.motionX != realEntity.getDeltaMovement().x ||
            this.motionY != realEntity.getDeltaMovement().y ||
            this.motionZ != realEntity.getDeltaMovement().z) {
            realEntity.setDeltaMovement(this.motionX, this.motionY, this.motionZ);
        }
        // Sync health
        if (this.health != (int) realEntity.getHealth()) {
            realEntity.setHealth((float) this.health);
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
     * Returns the underlying Forge LivingEntity, or null once it has been unloaded and collected.
     */
    public LivingEntity getRealEntity() {
        return real();
    }

    // Null while super(null) runs (field not yet assigned) or after the entity was collected.
    private LivingEntity real() {
        return realEntityRef != null ? realEntityRef.get() : null;
    }

    // ================================================================
    // Override methods that need to reach the real entity
    // ================================================================

    @Override
    public int getMaxHealth() {
        LivingEntity target = realEntityRef != null ? realEntityRef.get() : CONSTRUCTING.get();
        // Orphaned bridge (entity collected): keep the last synced health as the cap.
        return target != null ? (int) target.getMaxHealth() : Math.max(this.health, 1);
    }

    @Override
    public boolean isSneaking() {
        LivingEntity realEntity = real();
        return realEntity != null && realEntity.isShiftKeyDown();
    }

    @Override
    public boolean isSprinting() {
        LivingEntity realEntity = real();
        return realEntity != null && realEntity.isSprinting();
    }

    @Override
    public boolean isInWater() {
        LivingEntity realEntity = real();
        return realEntity != null && realEntity.isInWater();
    }

    @Override
    public boolean isBurning() {
        LivingEntity realEntity = real();
        return realEntity != null && realEntity.isOnFire();
    }

    @Override
    public boolean handleLavaMovement() {
        LivingEntity realEntity = real();
        return realEntity != null && realEntity.isInLava();
    }

    @Override
    public boolean isEntityAlive() {
        LivingEntity realEntity = real();
        return realEntity != null && realEntity.isAlive();
    }

    @Override
    public void setFire(int seconds) {
        LivingEntity realEntity = real();
        if (realEntity != null) {
            realEntity.setRemainingFireTicks(seconds * 20);
        }
        this.fire = seconds * 20;
    }

    @Override
    public void extinguish() {
        LivingEntity realEntity = real();
        if (realEntity != null) {
            realEntity.clearFire();
        }
        this.fire = 0;
    }

    @Override
    public void setDead() {
        super.setDead();
        LivingEntity realEntity = real();
        if (realEntity != null) {
            realEntity.discard();
        }
    }

    @Override
    public void addVelocity(double x, double y, double z) {
        this.motionX += x;
        this.motionY += y;
        this.motionZ += z;
        LivingEntity realEntity = real();
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
        LivingEntity realEntity = real();
        if (realEntity != null) {
            realEntity.setDeltaMovement(x, y, z);
        }
    }

    @Override
    public double getDistanceSq(double x, double y, double z) {
        LivingEntity realEntity = real();
        return realEntity != null ? realEntity.distanceToSqr(x, y, z) : super.getDistanceSq(x, y, z);
    }

    @Override
    public boolean canBeCollidedWith() {
        LivingEntity realEntity = real();
        return realEntity != null && realEntity.isPickable();
    }

    @Override
    public boolean canBePushed() {
        LivingEntity realEntity = real();
        return realEntity != null && realEntity.isPushable();
    }

    @Override
    public String getEntityName() {
        LivingEntity realEntity = real();
        return realEntity != null ? realEntity.getName().getString() : super.getEntityName();
    }

    @Override
    public boolean IsItemEntity() {
        return false;
    }

    /**
     * FC damage on a vanilla living entity -- an FC arrow / broadhead / thrown item / fireball hit
     * (WorldBridge's untyped query, VanillaEntityViews.of), an FC block or item. MC's hurt() with the mapped source
     * (WorldBridge.toMcDamageSource): a projectile credits its shooter, so the mob retaliates, the kill counts
     * for the player and the death message reads "shot by"; and hurt()'s own result is returned, so a hit
     * rejected during the invulnerability window, or an enderman dodging a projectile, ricochets the frozen
     * EntityArrow as in 1.5.2 (vanilla/server EntityArrow.java:308-347, EntityLiving.attackEntityFrom) instead
     * of being consumed. Server only; never throws (DamageSourceMapping.hurtFromFc).
     */
    @Override
    public boolean attackEntityFrom(btw.modern.DamageSource damageSource, int amount) {
        LivingEntity realEntity = real();
        if (realEntity == null) return false;
        boolean hurt = DamageSourceMapping.hurtFromFc(realEntity, damageSource, amount);
        // Keep the FC view current, so a later syncToReal() does not write the pre-hit health back.
        this.health = (int) realEntity.getHealth();
        return hurt;
    }

    @Override
    public void heal(int amount) {
        LivingEntity realEntity = real();
        if (realEntity != null) {
            realEntity.heal((float) amount);
        }
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
    public boolean isOnLadder() {
        LivingEntity realEntity = real();
        return realEntity != null && realEntity.onClimbable();
    }

    @Override
    public void playSound(String sound, float volume, float pitch) {
        LivingEntity realEntity = real();
        if (realEntity != null) {
            SoundMapping.playAtEntity(realEntity, sound, volume, pitch);
        }
    }
}
