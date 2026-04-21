package btw.forge;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

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
    private final LivingEntity realEntity;
    private static final WeakHashMap<LivingEntity, LivingEntityBridge> cache = new WeakHashMap<>();

    /**
     * Holds the entity being wrapped while super(null) runs, so virtual calls
     * dispatched from EntityLiving's field initializers (notably
     * `health = this.getMaxHealth()`) can reach the real entity before our
     * subclass body has a chance to assign {@link #realEntity}.
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
     * Returns the underlying Forge LivingEntity.
     */
    public LivingEntity getRealEntity() {
        return realEntity;
    }

    // ================================================================
    // Override methods that need to reach the real entity
    // ================================================================

    @Override
    public int getMaxHealth() {
        LivingEntity target = realEntity != null ? realEntity : CONSTRUCTING.get();
        return (int) target.getMaxHealth();
    }

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
    public boolean isEntityAlive() {
        return realEntity.isAlive();
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
    public boolean canBeCollidedWith() {
        return realEntity.isPickable();
    }

    @Override
    public boolean canBePushed() {
        return realEntity.isPushable();
    }

    @Override
    public String getEntityName() {
        return realEntity.getName().getString();
    }

    @Override
    public boolean IsItemEntity() {
        return false;
    }

    @Override
    public boolean attackEntityFrom(btw.modern.DamageSource damageSource, int amount) {
        // Basic damage forwarding — full damage source mapping would need DamageSourceMapping
        realEntity.hurt(realEntity.damageSources().generic(), (float) amount);
        return true;
    }

    @Override
    public void heal(int amount) {
        realEntity.heal((float) amount);
    }

    @Override
    public boolean isOnLadder() {
        return realEntity.onClimbable();
    }

    @Override
    public void playSound(String sound, float volume, float pitch) {
        SoundMapping.playAtEntity(realEntity, sound, volume, pitch);
    }
}
