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
    protected int air = 300;

    protected Entity(World world) {
        this.worldObj = world;
        this.rand = new Random();
        this.dataWatcher = new DataWatcher();
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
        float halfWidth = this.width / 2.0F;
        this.boundingBox.setBounds(
            x - (double) halfWidth, y - (double) this.yOffset + (double) this.ySize, z - (double) halfWidth,
            x + (double) halfWidth, y - (double) this.yOffset + (double) this.ySize + (double) this.height, z + (double) halfWidth
        );
    }

    public void onUpdate() {}

    public void onEntityUpdate() {}

    // --- Fire ---

    public void setOnFireFromLava() {}
    public void setFire(int seconds) {
        int ticks = seconds * 20;
        if (ticks > this.fire) {
            this.fire = ticks;
        }
    }

    public void extinguish() {
        this.fire = 0;
    }
    public void kill() {
        this.setDead();
    }

    // --- Movement and collision ---

    public void moveEntity(double dMoveX, double dMoveY, double dMoveZ) {
        this.posX += dMoveX;
        this.posY += dMoveY;
        this.posZ += dMoveZ;
        this.setPosition(this.posX, this.posY, this.posZ);
    }

    public boolean isOffsetPositionInLiquid(double x, double y, double z) {
        return false;
    }

    /**
     * Returns true if the entity is currently touching lava.
     * In vanilla 1.5.2, this checked if the entity's bounding box (shrunk
     * slightly) intersects any lava blocks. Bridge classes override to
     * delegate to the real MC entity's isInLava(). This base implementation
     * checks via the world's isMaterialInBB so FC entities that run their
     * own code (e.g., FCEntitySheep checking lava for grazing) get correct
     * results when the worldObj is a WorldBridge.
     */
    public boolean handleLavaMovement() {
        if (this.worldObj == null || this.boundingBox == null) return false;
        return this.worldObj.isMaterialInBB(
                this.boundingBox.contract(0.10000000149011612D, 0.4000000059604645D, 0.10000000149011612D),
                Material.lava);
    }

    public boolean handleWaterMovement() {
        return this.inWater;
    }

    public boolean isInsideOfMaterial(Material material) {
        if (this.worldObj == null) return false;
        double eyeY = this.posY + (double)this.getEyeHeight();
        int x = MathHelper.floor_double(this.posX);
        int y = MathHelper.floor_double(eyeY);
        int z = MathHelper.floor_double(this.posZ);
        int blockId = this.worldObj.getBlockId(x, y, z);
        if (blockId != 0) {
            Block block = Block.blocksList[blockId];
            if (block != null && block.blockMaterial == material) {
                float fluidHeight = BlockFluid.getFluidHeightPercent(this.worldObj.getBlockMetadata(x, y, z)) - 0.11111111F;
                float blockTop = (float)(y + 1) - fluidHeight;
                return eyeY < (double)blockTop;
            }
        }
        return false;
    }

    public boolean pushOutOfBlocks(double x, double y, double z) {
        return false;
    }

    public boolean canBePushed() {
        return !this.isDead;
    }

    // --- Riding ---

    public void mountEntity(Entity entity) {
        if (entity == null) {
            if (this.ridingEntity != null) {
                this.ridingEntity.riddenByEntity = null;
            }
            this.ridingEntity = null;
        } else {
            this.ridingEntity = entity;
            entity.riddenByEntity = this;
        }
    }
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

    private boolean sprinting;

    /**
     * Returns whether this entity is sprinting.
     * Bridge classes (PlayerBridge, EntityBridge, LivingEntityBridge) override
     * this to query the real MC entity. The base implementation stores the flag
     * locally so FC code calling setSprinting/isSprinting on Modern-Common
     * entities works correctly.
     */
    public boolean isSprinting() {
        return this.sprinting;
    }

    public void setSprinting(boolean sprinting) {
        this.sprinting = sprinting;
    }

    private boolean invisible;

    public boolean isInvisible() {
        return this.invisible;
    }

    public void setInvisible(boolean invisible) {
        this.invisible = invisible;
    }

    public void setEating(boolean eating) {}

    public boolean getFlag(int flag) {
        return false;
    }

    public void setFlag(int flag, boolean value) {}

    public int getAir() { return this.air; }
    public void setAir(int air) { this.air = air; }

    public float getBrightness(float partialTicks) {
        if (this.worldObj == null) return 0;
        int x = MathHelper.floor_double(this.posX);
        int y = MathHelper.floor_double(this.posY + (double)this.getEyeHeight());
        int z = MathHelper.floor_double(this.posZ);
        return this.worldObj.getLightBrightness(x, y, z);
    }

    public double getDistance(double x, double y, double z) {
        double dx = this.posX - x;
        double dy = this.posY - y;
        double dz = this.posZ - z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public float getEyeHeight() {
        return this.height * 0.85F;
    }

    public Vec3 getLookVec() {
        float f1 = MathHelper.cos(-rotationYaw * 0.017453292F - (float)Math.PI);
        float f2 = MathHelper.sin(-rotationYaw * 0.017453292F - (float)Math.PI);
        float f3 = -MathHelper.cos(-rotationPitch * 0.017453292F);
        float f4 = MathHelper.sin(-rotationPitch * 0.017453292F);
        return Vec3.createVectorHelper(f2 * f3, f4, f1 * f3);
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

    public void setInWeb() {
        this.isInWeb = true;
        this.fallDistance = 0.0F;
    }

    // --- Copy ---

    public void copyDataFrom(Entity entity, boolean teleport) {}

    // setThrowableHeading is on IProjectile interface, not Entity

    // --- Attack ---

    public boolean attackEntityFrom(DamageSource damageSource, int amount) {
        this.setBeenAttacked();
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
    public boolean isWet() { return this.inWater; }
    public boolean isInWater() { return this.inWater; }

    public boolean isBurning() {
        return !this.isImmuneToFire && this.fire > 0;
    }

    public void setLocationAndAngles(double x, double y, double z, float yaw, float pitch) {
        this.prevPosX = this.posX = x;
        this.prevPosY = this.posY = y;
        this.prevPosZ = this.posZ = z;
        this.prevRotationYaw = this.rotationYaw = yaw;
        this.prevRotationPitch = this.rotationPitch = pitch;
        this.lastTickPosX = this.posX;
        this.lastTickPosY = this.posY;
        this.lastTickPosZ = this.posZ;
        this.setPosition(this.posX, this.posY, this.posZ);
    }
    public void setBeenAttacked() {}
    public boolean canBeCollidedWith() { return false; }
    public EntityItem entityDropItem(ItemStack stack, float yOffset) {
        if (stack.stackSize == 0) {
            return null;
        }
        EntityItem entityItem = new EntityItem(this.worldObj, this.posX, this.posY + (double) yOffset, this.posZ, stack);
        entityItem.delayBeforeCanPickup = 10;
        if (this.worldObj != null) {
            this.worldObj.spawnEntityInWorld(entityItem);
        }
        return entityItem;
    }

    public EntityItem dropItem(int itemID, int count) {
        return this.dropItemWithOffset(itemID, count, 0.0F);
    }

    public EntityItem dropItemWithOffset(int itemID, int count, float yOffset) {
        return this.entityDropItem(new ItemStack(itemID, count, 0), yOffset);
    }
    public AxisAlignedBB getCollisionBox(Entity entity) { return null; }
    public AxisAlignedBB getBoundingBox() { return this.boundingBox; }
    public void fall(float distance) {}
    // knockBack is on EntityLiving in vanilla, not Entity
    public double getDistanceSq(double x, double y, double z) {
        double dx = this.posX - x;
        double dy = this.posY - y;
        double dz = this.posZ - z;
        return dx * dx + dy * dy + dz * dz;
    }

    public double getDistanceSqToEntity(Entity entity) {
        double dx = this.posX - entity.posX;
        double dy = this.posY - entity.posY;
        double dz = this.posZ - entity.posZ;
        return dx * dx + dy * dy + dz * dz;
    }

    public float getDistanceToEntity(Entity entity) {
        return (float) Math.sqrt(this.getDistanceSqToEntity(entity));
    }

    public void addVelocity(double x, double y, double z) {
        this.motionX += x;
        this.motionY += y;
        this.motionZ += z;
        this.isAirBorne = true;
    }
    public void applyEntityCollision(Entity entity) {}
    public void setVelocity(double x, double y, double z) {
        this.motionX = x;
        this.motionY = y;
        this.motionZ = z;
    }
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
        if (this.worldObj == null) return 0;
        int x = MathHelper.floor_double(this.posX);
        int y = MathHelper.floor_double(this.posY + (double)this.getEyeHeight());
        int z = MathHelper.floor_double(this.posZ);
        if (y < 0) y = 0;
        if (y > 255) y = 255;
        return this.worldObj.getLightBrightnessForSkyBlocks(x, y, z, 0);
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
