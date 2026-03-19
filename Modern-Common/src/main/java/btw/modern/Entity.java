package btw.modern;

import java.util.Random;

/**
 * Abstract representation of a game entity.
 * Mirrors net.minecraft.src.Entity with identical field/method names.
 */
public abstract class Entity {

    // --- Instance fields ---
    public int entityId;
    public double renderDistanceWeight;
    public boolean preventEntitySpawning;
    public Entity riddenByEntity;
    public Entity ridingEntity;
    public boolean field_98038_p;

    public World worldObj;
    public double prevPosX;
    public double prevPosY;
    public double prevPosZ;
    public double posX;
    public double posY;
    public double posZ;
    public double motionX;
    public double motionY;
    public double motionZ;

    public float rotationYaw;
    public float rotationPitch;
    public float prevRotationYaw;
    public float prevRotationPitch;

    public AxisAlignedBB boundingBox;
    public boolean onGround;
    public boolean isCollidedHorizontally;
    public boolean isCollidedVertically;
    public boolean isCollided;
    public boolean velocityChanged;
    public boolean isInWeb;
    public boolean field_70135_K;
    public boolean isDead;
    public float yOffset;
    public float width;
    public float height;
    public float prevDistanceWalkedModified;
    public float distanceWalkedModified;
    public float distanceWalkedOnStepModified;
    public float fallDistance;

    public double lastTickPosX;
    public double lastTickPosY;
    public double lastTickPosZ;
    public float ySize;
    public float stepHeight;
    public boolean noClip;
    public float entityCollisionReduction;
    public Random rand;
    public int ticksExisted;
    public int fireResistance;
    public boolean inWater;
    public int hurtResistantTime;
    public boolean isImmuneToFire;
    public DataWatcher dataWatcher;
    public boolean addedToChunk;
    public int chunkCoordX;
    public int chunkCoordY;
    public int chunkCoordZ;
    public boolean ignoreFrustumCheck;
    public boolean isAirBorne;
    public int timeUntilPortal;
    public boolean inPortal;
    public int timeInPortal;
    public int dimension;
    public int teleportDirection;

    public int canBePickedUp;
    public int delayBeforeCanPickup;
    public int metadata;
    public int fire;
    public boolean forceSpawn;
    public int serverPosX;
    public int serverPosY;
    public int serverPosZ;

    protected Entity(World world) {
        this.worldObj = world;
        this.rand = new Random();
        this.boundingBox = AxisAlignedBB.getBoundingBox(0, 0, 0, 0, 0, 0);
    }

    // --- Abstract methods ---

    public void entityInit() {}

    // --- Core lifecycle ---

    public void setDead() {
        this.isDead = true;
    }

    public void setSize(float width, float height) {
        this.width = width;
        this.height = height;
    }

    public void setRotation(float yaw, float pitch) {
        this.rotationYaw = yaw;
        this.rotationPitch = pitch;
    }

    public void setPosition(double x, double y, double z) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
    }

    public void onUpdate() {}

    public void onEntityUpdate() {}

    // --- Fire ---

    public void setOnFireFromLava() {}
    public void setFire(int seconds) {}
    public void extinguish() {}
    public void kill() {}

    // --- Movement and collision ---

    public void moveEntity(double dMoveX, double dMoveY, double dMoveZ) {}

    public boolean isOffsetPositionInLiquid(double x, double y, double z) {
        return false;
    }

    public boolean handleLavaMovement() {
        return false;
    }

    public boolean handleWaterMovement() {
        return false;
    }

    public boolean isInsideOfMaterial(Material material) {
        return false;
    }

    public boolean pushOutOfBlocks(double x, double y, double z) {
        return false;
    }

    public boolean canBePushed() {
        return false;
    }

    // --- Riding ---

    public void mountEntity(Entity entity) {}
    public void unmountEntity(Entity entity) {}
    public void updateRidden() {}
    public void updateRiderPosition() {}
    public double getYOffset() { return 0; }
    public double getMountedYOffset() { return 0; }
    public boolean isRiding() { return this.ridingEntity != null; }

    // --- Query methods ---

    public boolean isEntityAlive() {
        return !this.isDead;
    }

    public boolean isSneaking() {
        return false;
    }

    public void setSneaking(boolean sneaking) {}

    public boolean isSprinting() {
        return false;
    }

    public void setSprinting(boolean sprinting) {}

    public boolean isInvisible() {
        return false;
    }

    public void setInvisible(boolean invisible) {}

    public void setEating(boolean eating) {}

    public boolean getFlag(int flag) {
        return false;
    }

    public void setFlag(int flag, boolean value) {}

    public int getAir() { return 0; }
    public void setAir(int air) {}

    public float getBrightness(float partialTicks) {
        return 0;
    }

    public double getDistance(double x, double y, double z) {
        return 0;
    }

    public float getEyeHeight() {
        return 0;
    }

    public Vec3 getLookVec() {
        return null;
    }

    // getLook is on EntityLiving in vanilla, not Entity

    public float getCollisionBorderSize() {
        return 0.1F;
    }

    public float getRotationYawHead() {
        return 0;
    }

    public boolean canAttackWithItem() {
        return true;
    }

    public boolean isEntityInvulnerable() {
        return false;
    }

    public boolean isInvulnerable() {
        return isEntityInvulnerable();
    }

    public boolean doesEntityNotTriggerPressurePlate() {
        return false;
    }

    public boolean isEntityEqual(Entity entity) {
        return this == entity;
    }

    public DataWatcher getDataWatcher() {
        return this.dataWatcher;
    }

    public int getMaxInPortalTime() {
        return 0;
    }

    public int getPortalCooldown() {
        return 300;
    }

    public int getTeleportDirection() {
        return this.teleportDirection;
    }

    public String getEntityName() {
        return "";
    }

    public String getTranslatedEntityName() {
        return "";
    }

    public Entity[] getParts() {
        return null;
    }

    public ItemStack[] getInventory() {
        return null;
    }

    public void setCurrentItemOrArmor(int slot, ItemStack stack) {}

    // --- Portal ---

    public void setInPortal() {}

    // --- NBT ---

    public void readFromNBT(NBTTagCompound tag) {}
    public void writeToNBT(NBTTagCompound tag) {}

    // --- Lightning ---

    public void onStruckByLightning(Entity lightningBolt) {}
    public void onKillEntity(EntityLiving livingEntity) {}

    // --- Web ---

    public void setInWeb() {}

    // --- Copy ---

    public void copyDataFrom(Entity entity, boolean teleport) {}

    // setThrowableHeading is on IProjectile interface, not Entity

    // --- Attack ---

    public boolean attackEntityFrom(DamageSource damageSource, int amount) {
        return false;
    }

    // --- Explosion ---

    public float func_82146_a(Explosion explosion, World world, int x, int y, int z, Block block) {
        return 0;
    }

    public boolean func_96091_a(Explosion explosion, World world, int x, int y, int z, int blockID, float chance) {
        return true;
    }

    public int func_82143_as() {
        return 0;
    }

    public boolean func_85031_j(Entity entity) {
        return false;
    }

    public void func_82149_j(Entity entity) {}

    public boolean func_96092_aw() {
        return false;
    }

    // --- Additional methods ---

    public void playSound(String sound, float volume, float pitch) {}
    public boolean canTriggerWalking() { return true; }
    public boolean isWet() { return false; }
    public boolean isInWater() { return false; }
    public boolean isBurning() { return false; }
    public void setLocationAndAngles(double x, double y, double z, float yaw, float pitch) {}
    public void setBeenAttacked() {}
    public boolean canBeCollidedWith() { return false; }
    public EntityItem entityDropItem(ItemStack stack, float yOffset) { return null; }
    public EntityItem dropItem(int itemID, int count) { return null; }
    public EntityItem dropItemWithOffset(int itemID, int count, float yOffset) { return null; }
    public AxisAlignedBB getCollisionBox(Entity entity) { return null; }
    public AxisAlignedBB getBoundingBox() { return null; }
    public void fall(float distance) {}
    // knockBack is on EntityLiving in vanilla, not Entity
    public double getDistanceSq(double x, double y, double z) { return 0; }
    public double getDistanceSqToEntity(Entity entity) { return 0; }
    public float getDistanceToEntity(Entity entity) { return 0; }
    public void addVelocity(double x, double y, double z) {}
    public void applyEntityCollision(Entity entity) {}
    public void setVelocity(double x, double y, double z) {}
    public void readEntityFromNBT(NBTTagCompound tag) {}
    public void writeEntityToNBT(NBTTagCompound tag) {}

    // --- BTW-added methods ---

    public boolean IsAffectedByMovementModifiers() { return true; }
    public void NotifyOfWolfHowl(Entity sourceEntity) {}
    public boolean ShouldSetPositionOnLoad() { return true; }
    public boolean CanCollideWithEntity(Entity entity) { return true; }
    public boolean IsItemEntity() { return false; }
    public boolean CanEntityTriggerTripwire() { return true; }
    public AxisAlignedBB GetVisualBoundingBox() { return this.boundingBox; }
    public boolean IsSecondaryTargetForSquid() { return false; }
    public boolean GetCanBeHeadCrabbed(boolean bSquidInWater) { return false; }
    public boolean IsValidOngoingAttackTargetForSquid() { return false; }
    public void OnFlungBySquidTentacle(Entity squid) {}
    public void OnHeadCrabbedBySquid(Entity squid) {}
    public boolean HasHeadCrabbedSquid() { return false; }
    public Entity GetHeadCrabSharedAttackTarget() { return null; }
    public boolean IsImmuneToHeadCrabDamage() { return false; }
    public void OnKickedByCow(Entity cow) {}
    public double GetCowKickMovementMultiplier() { return 1.0; }
    public void FlingAwayFromEntity(Entity repulsingEntity, double dForceMultiplier) {}
    public boolean DoesEntityApplyToSpawnCap() { return false; }
    public void OutOfUpdateRangeUpdate() {}
    public boolean AppliesConstantForceWhenRidingBoat() { return false; }
    public double MovementModifierWhenRidingBoat() { return 0; }
    public boolean OnPossesedRidingEntityDeath() { return false; }
    public boolean IsBeingRainedOn() { return false; }
    public boolean DoesEntityApplyToSquidPossessionCap() { return false; }
    public boolean IsValidZombieSecondaryTarget(EntityZombie zombie) { return false; }
    public boolean AttractsLightning() { return false; }
    public void OnStruckByLightning(Entity boltEntity) {}
    public void MountEntityRemote(Entity entityToMount) {}
    public void FlagAllWatchedObjectsDirty() {}

    // --- Travel ---

    public void travelToTheEnd(int dimension) {}

    public boolean interact(EntityPlayer player) { return false; }
    // spawnExplosionParticle and heal are on EntityLiving in vanilla, not Entity
    public void addOrRenewAgressor(EntityLiving entity) {}

    // --- Client-side rendering ---

    public int getBrightnessForRender(float partialTicks) {
        return 0;
    }

    public float getShadowSize() {
        return 0.0F;
    }

    public void setPositionAndRotation2(double x, double y, double z, float yaw, float pitch, int posRotationIncrements) {
        this.setPosition(x, y, z);
    }

    public boolean isInRangeToRenderVec3D(Vec3 vec) {
        return true;
    }

    public boolean isInRangeToRenderDist(double distance) {
        return true;
    }

    public void performHurtAnimation() {}
}
