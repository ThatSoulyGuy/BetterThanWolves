package btw.modern;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public abstract class EntityLiving extends Entity {

    /** Active potion effects, keyed by potion ID. */
    private final HashMap<Integer, PotionEffect> activePotionsMap = new HashMap<>();

    public int health;
    public float landMovementFactor;
    public boolean isLivingDead;
    public int deathTime;
    public int attackTime;
    public int hurtTime;
    public float swingProgress;
    public float prevSwingProgress;
    public EntityPlayer attackingPlayer;
    public int recentlyHit;
    public EntityLiving entityLivingToAttack;
    public int experienceValue;
    public float renderYawOffset;
    public float prevRenderYawOffset;
    public float moveStrafing;
    public float moveForward;
    public boolean isJumping;
    public float moveSpeed;
    public int attackCounter;
    public EntityAITasks tasks;
    public EntityAITasks targetTasks;
    public float prevLimbYaw;
    public float limbYaw;
    public float limbSwing;
    public int newPosRotationIncrements;
    public double newPosX;
    public double newPosY;
    public double newPosZ;
    public double newRotationYaw;
    public double newRotationPitch;
    public int revengeTimer;
    public int numTicksToChaseTarget;
    public float prevCameraPitch;
    public float cameraPitch;
    public EntityLiving lastAttackingEntity;
    public float defaultPitch;
    public String texture;
    public float rotationYawHead;
    public float prevRotationYawHead;
    public float[] equipmentDropChances = new float[5];
    public int entityAge;
    public int carryoverDamage;
    private EntityLiving attackTarget;
    public ItemStack[] equipment = new ItemStack[5];
    protected PathNavigate navigator;
    protected EntityLookHelper lookHelper;
    protected EntitySenses senses;

    protected EntityLiving(World world) {
        super(world);
        this.tasks = new EntityAITasks();
        this.targetTasks = new EntityAITasks();
    }

    public abstract int getMaxHealth();

    public void jump() {
        this.motionY = 0.41999998688697815;
        if (this.isPotionActive(Potion.jump)) {
            this.motionY += (double)((float)(this.getActivePotionEffect(Potion.jump).getAmplifier() + 1) * 0.1F);
        }
    }

    public ItemStack getHeldItem() {
        return getCurrentItemOrArmor(0);
    }

    public void swingItem() {
        this.swingProgress = 0.0F;
    }

    public EntityLiving getAttackTarget() {
        return this.attackTarget;
    }

    public EntitySenses getEntitySenses() {
        if (senses == null) {
            senses = new EntitySenses();
        }
        return senses;
    }

    public PathNavigate getNavigator() {
        if (navigator == null) {
            navigator = new PathNavigate(this, this.worldObj, 16.0F);
        }
        return navigator;
    }

    public EntityLookHelper getLookHelper() {
        if (lookHelper == null) {
            lookHelper = new EntityLookHelper();
        }
        return lookHelper;
    }

    /**
     * Called by the proxy each tick. In vanilla 1.5.2, EntityLiving.onUpdate()
     * called onLivingUpdate(). We replicate that call chain so FC entities
     * that override onLivingUpdate() have their behavior fire.
     *
     * Movement physics, collision, pathfinding, and AI goals are handled by
     * MC 1.20.1 via the proxy's super.tick() — do NOT replicate those here.
     */
    @Override
    public void onUpdate() {
        onLivingUpdate();
    }

    public void onLivingUpdate() {}

    public boolean attackEntityFrom(DamageSource source, int amount) {
        if (this.isEntityInvulnerable()) {
            return false;
        }
        if (this.health <= 0) {
            return false;
        }
        this.limbSwing = 1.5F;
        this.hurtResistantTime = 10;
        this.hurtTime = 10;
        this.health -= amount;
        if (this.health <= 0) {
            this.onDeath(source);
        }
        return true;
    }

    public void onDeath(DamageSource source) {
        this.setDead();
    }

    public void setAttackTarget(EntityLiving target) {
        this.attackTarget = target;
        this.entityLivingToAttack = target;
    }

    public float getEyeHeight() {
        return this.height * 0.85F;
    }

    public boolean canEntityBeSeen(Entity entity) {
        return true; // TODO: implement ray tracing
    }

    public int getTotalArmorValue() {
        int total = 0;
        for (int i = 1; i < 5; i++) {
            if (equipment[i] != null && equipment[i].getItem() instanceof ItemArmor) {
                total += ((ItemArmor) equipment[i].getItem()).damageReduceAmount;
            }
        }
        return total;
    }

    public boolean isPotionActive(int potionId) {
        return this.activePotionsMap.containsKey(potionId);
    }

    public boolean isPotionActive(Potion potion) {
        return potion != null && this.activePotionsMap.containsKey(potion.id);
    }

    public PotionEffect getActivePotionEffect(Potion potion) {
        return potion != null ? this.activePotionsMap.get(potion.id) : null;
    }

    public void addPotionEffect(PotionEffect effect) {
        if (effect != null) {
            this.activePotionsMap.put(effect.getPotionID(), effect);
        }
    }

    public void removePotionEffect(int potionId) {
        this.activePotionsMap.remove(potionId);
    }

    public void heal(int amount) {
        if (this.health > 0) {
            this.health += amount;
            if (this.health > this.getMaxHealth()) {
                this.health = this.getMaxHealth();
            }
            this.hurtTime = 0;
        }
    }

    public ItemStack getCurrentItemOrArmor(int slot) {
        return (slot >= 0 && slot < equipment.length) ? equipment[slot] : null;
    }

    public ItemStack getCurrentArmor(int slot) {
        return getCurrentItemOrArmor(slot + 1);
    }

    public void setCurrentItemOrArmor(int slot, ItemStack stack) {
        this.equipment[slot] = stack;
    }

    public boolean isOnLadder() {
        int x = MathHelper.floor_double(this.posX);
        int y = MathHelper.floor_double(this.boundingBox.minY);
        int z = MathHelper.floor_double(this.posZ);
        Block block = Block.blocksList[this.worldObj.getBlockId(x, y, z)];
        return block != null && block.IsBlockClimbable(this.worldObj, x, y, z);
    }

    public boolean isEntityAlive() {
        return !this.isDead && this.health > 0;
    }

    public String getTranslatedEntityName() {
        String name = getClass().getSimpleName();
        if (name.startsWith("FCEntity")) name = name.substring(8);
        else if (name.startsWith("Entity")) name = name.substring(6);
        String key = "entity." + name + ".name";
        String translated = StatCollector.translateToLocal(key);
        return translated.equals(key) ? name : translated;
    }

    public boolean isAIEnabled() {
        return false;
    }

    public void setRevengeTarget(EntityLiving target) {
        this.entityLivingToAttack = target;
        if (target != null) {
            this.revengeTimer = this.ticksExisted;
        }
    }

    public EntityLiving getLastAttackingEntity() {
        return this.lastAttackingEntity;
    }

    public boolean isPlayerSleeping() {
        return false;
    }

    public int getAge() {
        return this.entityAge;
    }

    public void clearActivePotions() {
        this.activePotionsMap.clear();
    }

    // --- Enchantment bridge methods ---
    // These return 0/false by default. PlayerBridge overrides them to query
    // the real MC 1.20.1 enchantment system via the underlying Player entity.

    /** Returns the efficiency enchantment level on the held item. */
    public int getEfficiencyEnchantLevel() { return 0; }

    /** Returns the respiration enchantment level on worn armor. */
    public int getRespirationEnchantLevel() { return 0; }

    /** Returns the knockback enchantment level on the held weapon. */
    public int getKnockbackEnchantLevel() { return 0; }

    /** Returns the fire aspect enchantment level on the held weapon. */
    public int getFireAspectEnchantLevel() { return 0; }

    /** Returns the looting enchantment level on the held weapon. */
    public int getLootingEnchantLevel() { return 0; }

    /** Returns the unbreaking enchantment level on the held item. */
    public int getUnbreakingEnchantLevel() { return 0; }

    /** Returns the fortune enchantment level on the held item. */
    public int getFortuneEnchantLevel() { return 0; }

    /** Returns true if the player has aqua affinity on worn armor. */
    public boolean getAquaAffinityEnchant() { return false; }

    /** Returns true if the player has silk touch on the held item. */
    public boolean getSilkTouchEnchant() { return false; }

    // --- BTW-added methods ---

    public void dropFewItems(boolean bKilledByPlayer, int iLootingModifier) {
        EntityLivingDropFewItems(bKilledByPlayer, iLootingModifier);
    }
    public int getDropItemId() { return 0; }
    public String getLivingSound() { return null; }
    public String getHurtSound() { return null; }
    public String getDeathSound() { return null; }
    public float getSoundVolume() { return 1.0F; }
    public float getSoundPitch() {
        return this.isChild()
            ? (this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F + 1.5F
            : (this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F + 1.0F;
    }
    public void initCreature() {}
    public void SpawnerInitCreature() {}
    public boolean interact(EntityPlayer player) { return false; }
    public boolean getCanSpawnHere() {
        return this.worldObj != null
            && this.worldObj.checkNoEntityCollision(this.boundingBox)
            && this.worldObj.getCollidingBoundingBoxes(this, this.boundingBox).isEmpty()
            && !this.worldObj.isAnyLiquid(this.boundingBox);
    }
    public void updateEntityActionState() {}
    public void fall(float fFallDistance) {}
    public void dropHead() {}
    public boolean canTriggerWalking() { return true; }
    public AxisAlignedBB getCollisionBox(Entity entity) { return null; }
    public AxisAlignedBB getBoundingBox() { return this.boundingBox; }
    public void despawnEntity() {}
    public void setCanPickUpLoot(boolean canPickUp) {}
    public boolean getCanPickUpLoot() { return false; }
    public void setEntityHealth(int health) { this.health = health; }
    public int getHealth() { return this.health; }
    public void faceEntity(Entity entity, float maxYawChange, float maxPitchChange) {
        double dx = entity.posX - this.posX;
        double dz = entity.posZ - this.posZ;
        double dy;
        if (entity instanceof EntityLiving) {
            dy = entity.posY + (double)entity.getEyeHeight() - (this.posY + (double)this.getEyeHeight());
        } else {
            dy = (entity.boundingBox.minY + entity.boundingBox.maxY) / 2.0 - (this.posY + (double)this.getEyeHeight());
        }
        double dist = MathHelper.sqrt_double(dx * dx + dz * dz);
        float targetYaw = (float)(Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
        float targetPitch = (float)(-(Math.atan2(dy, dist) * 180.0 / Math.PI));
        this.rotationPitch = updateRotation(this.rotationPitch, targetPitch, maxPitchChange);
        this.rotationYaw = updateRotation(this.rotationYaw, targetYaw, maxYawChange);
    }

    private float updateRotation(float current, float target, float maxChange) {
        float delta = MathHelper.wrapAngleTo180_float(target - current);
        if (delta > maxChange) delta = maxChange;
        if (delta < -maxChange) delta = -maxChange;
        return current + delta;
    }

    public void setHomeArea(int x, int y, int z, int radius) {}
    public boolean hasHome() { return false; }
    public ChunkCoordinates getHomePosition() { return null; }
    public float getMaximumHomeDistance() { return -1.0F; }
    public void detachHome() {}
    public PathEntity getPathEntityToEntity(Entity entity) { return null; }
    public boolean hasPath() { return false; }
    public void setPathToEntity(PathEntity path) {}
    public EntityLiving getAITarget() { return this.entityLivingToAttack; }
    public boolean isChild() { return false; }
    public void renderBrokenItemStack(ItemStack stack) {}
    public java.util.Random getRNG() { return this.rand; }

    // --- BTW-added: entity methods ---
    /**
     * "super.super()" bridge: FC entities (Zombie, Skeleton, Enderman) call
     * this to invoke EntityMob.onLivingUpdate(), which in vanilla called
     * EntityLivingOnLivingUpdate() (i.e., EntityLiving.onLivingUpdate()).
     */
    public void EntityMobOnLivingUpdate() {
        EntityLivingOnLivingUpdate();
    }

    /**
     * "super.super()" bridge: FC entities (Enderman, PigZombie) call this to
     * invoke EntityMob.attackEntityFrom(), which in vanilla just delegated to
     * EntityLiving.attackEntityFrom().
     */
    public boolean EntityMobAttackEntityFrom(DamageSource source, int amount) {
        return attackEntityFrom(source, amount);
    }
    public boolean EntityAnimalInteract(EntityPlayer player) { return false; }
    // Removed: void MeleeAttack(EntityLiving) - vanilla has boolean MeleeAttack(Entity) instead
    public void SetHungerLevel(int level) {}
    public int GetGrowthLevel() { return 0; }
    public void SetGrowthLevelNoNotify(int level) {}
    public void InitHungerWithVariance() {}
    public boolean GetIsUpsideDown() { return false; }
    public boolean GetIsPersistent() { return this.persistenceRequired; }
    public boolean persistenceRequired;
    public void SetPersistent(boolean persistent) {
        this.persistenceRequired = persistent;
    }
    public void CheckForCatchFireInSun() {}

    // BTW creature state
    public boolean IsSubjectToHunger() { return false; }
    public void UpdateHungerState() {}
    public int GetItemFoodValue(ItemStack stack) { return 0; }
    public boolean isBreedingItem(ItemStack stack) { return false; }
    public boolean GetCanCreatureTypeBePossessed() { return false; }
    public void CheckForScrollDrop() {}
    public boolean IsValidZombieSecondaryTarget(EntityZombie zombie) { return false; }

    // BTW tracker
    public int GetTrackerViewDistance() { return 80; }
    public int GetTrackerUpdateFrequency() { return 3; }
    public boolean ShouldServerTreatAsOversized() { return false; }
    public boolean GetTrackMotion() { return true; }
    public Packet GetSpawnPacketForThisEntity() { return null; }
    public double MinDistFromPlayerForDespawn() { return 128.0; }
    public Item GetCorrespondingItem() { return null; }

    // BTW misc
    public void TransferPowerStateToConnectedAxles() {}
    public void DestroyWithDrop() {}
    public int GetTicksPerFullUpdate() { return 20; }
    public int GetMaxDamage() { return 0; }
    public float GetWidth() { return 0.6F; }
    public float GetHeight() { return 1.8F; }
    public float GetDepth() { return 0.6F; }
    public float GetDamageMultiplier() { return 1.0F; }
    public float ComputeRotation() { return 0.0F; }
    public boolean canDamagePlayer() { return false; }
    public boolean ValidateConnectedAxles() { return true; }
    public boolean ValidateAreaAroundDevice() { return true; }
    public boolean CanHopperCollect() { return false; }
    public EntitySlime createInstance() { return null; }

    // BTW possession
    public boolean IsFullyFed() { return false; }
    public boolean IsStarving() { return false; }
    public boolean IsFamished() { return false; }
    public boolean IsFullyPossessed() { return false; }
    public boolean IsPossessed() { return false; }
    public int m_iHungerCountdown;

    // BTW additional entity methods needed for subclass overrides
    public boolean canDespawn() { return true; }
    public int GetMeleeAttackStrength(Entity target) { return 0; }
    public boolean attackEntityAsMob(Entity target) { return false; }
    public boolean MeleeAttack(Entity target) { return false; }
    public boolean IsReadyToEatLooseFood() { return false; }
    public boolean IsReadyToEatLooseItem(ItemStack stack) { return false; }
    public boolean AttemptToEatLooseItem(ItemStack stack) { return false; }
    public boolean IsEdibleItem(ItemStack stack) { return false; }
    public void OnNearbyPlayerBlockAddOrRemove(EntityPlayer player) {}
    public void OnNearbyPlayerStartles(EntityPlayer player) {}
    public void HandlePossession() {}
    public boolean IsEngangedInPossessionAttempt() { return false; }
    public boolean CanGrazeOnRoughVegetation() { return false; }
    public boolean GetDisruptsEarthOnGraze() { return false; }
    public boolean IsHungryEnoughToForceMoveToGraze() { return false; }
    public boolean IsReadyToEatBreedingItem() { return false; }
    public boolean IsTooHungryToGrow() { return false; }
    public boolean IsTooHungryToHeal() { return false; }
    public boolean IsWeightedByHeadCrab() { return false; }
    public boolean ShouldContinueAttacking(float fDistanceToTarget) { return false; }
    public boolean ShouldNotifyBlockOnGraze() { return false; }
    public boolean ShouldStayInPlaceToGraze() { return false; }
    public boolean CanGrazeMycelium() { return false; }
    public boolean CanSpawnOnBlock(int iBlockID) { return true; }
    public boolean CanSwim() { return false; }
    public boolean DoEyesGlow() { return false; }
    public boolean DoesLightAffectAggessiveness() { return false; }
    public boolean DropsSpiderEyes() { return false; }
    public boolean GetCanCreatureBePossessedFromDistance(boolean bPersistentSpirit) { return false; }
    public FCUtilsBlockPos GetGrazeBlockForPos() { return null; }
    public Entity findPlayerToAttack() { return null; }
    public EnumCreatureAttribute getCreatureAttribute() { return null; }
    public void playLivingSound() {}
    public void playStepSound(int iBlockI, int iBlockJ, int iBlockK, int iBlockID) {}
    public void updateAITick() {}
    public void setCombatTask() {}
    public void setTarget(Entity targetEntity) {}
    public boolean AddArrowToPlayerInv(EntityPlayer player) { return false; }
    public void onImpact(MovingObjectPosition pos) {}
    public void setPositionAndRotation2(double x, double y, double z, float yaw, float pitch, int par9) {}
    public void onKillEntity(EntityLiving entityKilled) {}

    // BTW additional methods for animal/creature subclasses
    public void OnEatBreedingItem() {}
    public void OnFullPossession() {}
    public int GetFoodValueMultiplier() { return 1; }
    public void PlayGrazeFX(int i, int j, int k, int iBlockID) {}
    public int GetGrazeDuration() { return 0; }
    public void OnBecomeFamished() {}
    public float GetGrazeHeadRotationMagnitudeDivisor() { return 1.0F; }
    public float GetGrazeHeadRotationRateMultiplier() { return 1.0F; }
    public void OnGrazeBlock(int i, int j, int k) {}
    public void GiveBirthAtTargetLocation(EntityAnimal targetMate, double dChildX, double dChildY, double dChildZ) {}
    public float KnockbackMagnitude() { return 1.0F; }
    public int func_96121_ay() { return 0; }
    public float getSpeedModifier() { return 1.0F; }
    public void dropRareDrop(int iBonusDrop) {}
    public void convertToVillager() {}
    public int getConversionTimeBoost() { return 0; }
    public void ModSpecificOnLivingUpdate() {}
    public void attackEntity(Entity attackedEntity, float fDistanceToTarget) {}
    public void OnHeadCrabbedBySquid(Entity squid) {}
    public boolean makesSoundOnLand() { return true; }
    public void onCollideWithPlayer(EntityPlayer player) {}
    public boolean canBreatheUnderwater() { return false; }
    public float GetDefaultSlipperinessOnGround() { return 0.6F; }
    public float GetSlipperinessRelativeToBlock(int iBlockID) { return 0.6F; }
    public boolean makesSoundOnJump() { return true; }
    public void useRecipe(MerchantRecipe recipe) {}
    public MerchantRecipeList getRecipes(EntityPlayer player) { return null; }
    public void setAngry(boolean angry) {}
    public void setAggressive(boolean aggressive) {}
    public void setFleeceColor(int color) {}
    public int getRandomFleeceColor() { return 0; }
    public static int getRandomFleeceColor(java.util.Random rand) { return 0; }
    public void setVillager(boolean villager) {}
    public void setTameSkin(int skin) {}
    public int getTameSkin() { return 0; }
    public boolean isTrading() { return false; }
    public boolean isConverting() { return false; }
    public void startConversion(int time) {}
    public void OnNearbyAnimalAttacked(EntityAnimal attackedAnimal, EntityLiving attackSource) {}
    public void OnStarvingCountExpired() {}
    public void ResetHungerCountdown() {}
    public float GetHungerSpeedModifier() { return 1.0F; }
    public boolean isPotionApplicable(PotionEffect effect) { return true; }

    // Additional methods needed for specific entity overrides
    public boolean isValidLightLevel() { return true; }
    public float getBlockPathWeight(int i, int j, int k) { return 0.0F; }
    public int getTalkInterval() { return 80; }
    public void PreInitCreature() {}
    public void attackEntityWithRangedAttack(EntityLiving target, float damageModifier) {}
    public void func_82162_bC() {}
    public void moveEntityWithHeading(float strafe, float forward) {}
    public void CheckForLooseFood() {}
    public void InitiatePossession() {}
    public void setHomeArea(ChunkCoordinates pos, int radius) {}

    public static boolean InstallationIntegrityTest() { return true; }

    // Missing fields
    public int attacker;

    // Missing methods
    public Vec3 getLook(float partialTicks) {
        float yaw, pitch;
        if (partialTicks == 1.0F) {
            yaw = this.rotationYaw;
            pitch = this.rotationPitch;
        } else {
            yaw = this.prevRotationYaw + (this.rotationYaw - this.prevRotationYaw) * partialTicks;
            pitch = this.prevRotationPitch + (this.rotationPitch - this.prevRotationPitch) * partialTicks;
        }
        float cosYaw = MathHelper.cos(-yaw * 0.017453292F - (float)Math.PI);
        float sinYaw = MathHelper.sin(-yaw * 0.017453292F - (float)Math.PI);
        float cosPitch = -MathHelper.cos(-pitch * 0.017453292F);
        float sinPitch = MathHelper.sin(-pitch * 0.017453292F);
        return Vec3.createVectorHelper(sinYaw * cosPitch, sinPitch, cosYaw * cosPitch);
    }
    public void knockBack(Entity entity, int damage, double motionXParam, double motionZParam) {
        this.isAirBorne = true;
        float magnitude = MathHelper.sqrt_double(motionXParam * motionXParam + motionZParam * motionZParam);
        float knockbackMag = KnockbackMagnitude();
        this.motionX /= 2.0;
        this.motionY /= 2.0;
        this.motionZ /= 2.0;
        this.motionX -= motionXParam / (double)magnitude * (double)knockbackMag;
        this.motionY += (double)knockbackMag;
        this.motionZ -= motionZParam / (double)magnitude * (double)knockbackMag;
        if (this.motionY > 0.4) {
            this.motionY = 0.4;
        }
    }
    public int getVerticalFaceSpeed() { return 40; }
    public void spawnExplosionParticle() {}
    public boolean canSee(EntityLiving entity) {
        return canEntityBeSeen(entity);
    }
    public void addOrRenewAgressor(EntityLiving entity) {
        this.lastAttackingEntity = entity;
    }
    public void dealFireDamage(int amount) {
        this.attackEntityFrom(DamageSource.onFire, amount);
    }
    public void TransmitAttackTargetToClients() {}
    public void EntityLivingSetAttackTarget(EntityLiving target) {
        this.setAttackTarget(target);
    }
    /**
     * "super.super()" bridge: FC entities call this to invoke the base
     * EntityLiving.onLivingUpdate() logic. In vanilla 1.5.2, this contained
     * position interpolation, potion ticking, AI ticking, jump logic, and
     * movement physics.
     *
     * We implement only the parts NOT handled by MC 1.20.1:
     * - Potion duration ticking (expiry and removal)
     * - Entity age increment (for despawn timing)
     *
     * Movement, physics, AI goals, and pathfinding are handled by MC 1.20.1
     * via the proxy's super.tick().
     */
    public void EntityLivingOnLivingUpdate() {
        // Tick active potion effects and remove expired ones
        Iterator<Map.Entry<Integer, PotionEffect>> it = this.activePotionsMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, PotionEffect> entry = it.next();
            PotionEffect effect = entry.getValue();
            if (!effect.onUpdate(this)) {
                it.remove();
            }
        }

        // Increment entity age (used for despawn timing)
        this.entityAge++;
    }
    public void EntityLivingOnDeath(DamageSource source) {
        this.isLivingDead = true;
        this.EntityLivingDropFewItems(this.recentlyHit > 0, 0);
        this.setDead();
    }
    public void EntityLivingFall(float distance) {
        int damage = MathHelper.ceiling_float_int(distance - 3.0F);
        if (damage > 0) {
            this.attackEntityFrom(DamageSource.fall, damage);
        }
    }
    public void EntityLivingDropFewItems(boolean recentlyHit, int lootingLevel) {
        int itemId = this.getDropItemId();
        if (itemId > 0) {
            int count = this.rand.nextInt(3);
            if (lootingLevel > 0) {
                count += this.rand.nextInt(lootingLevel + 1);
            }
            for (int i = 0; i < count; i++) {
                this.dropItem(itemId, 1);
            }
        }
    }
    public void EntityLivingAddRandomArmor() {}
    public void EntityMobAttackEntity(Entity target, float distance) {}
    public void EntityCreatureEntityInit() {}
    public boolean EntityAgeableInteract(EntityPlayer player) { return false; }
    public void addRandomArmor() {}
    public int getAttackStrength() { return 0; }
    public boolean getAggressive() { return false; }
    public void func_82206_m() {}
    public void func_82187_q() {}
    public int func_82784_g() { return 0; }
    public boolean func_98041_l() { return false; }

    public int GetHungerLevel() { return 0; }
    public int GetGrazeHungerGain() { return 0; }
    public void AttemptToPossessNearbyCreatureOnDeath() {}
    public void AttemptToPossessNearbyCreature(double range, boolean persistentSpirit) {}
    public boolean CanEntityCenterOfMassBeSeen(Entity entity) { return false; }

    // --- Client-side methods ---

    public void handleHealthUpdate(byte updateType) {}

    public String getTexture() {
        if (this.texture == null || this.texture.isEmpty()) {
            // Derive texture from class name for vanilla mobs whose
            // Modern-Common stubs don't set the texture field.
            this.texture = deriveTextureFromClass();
        }
        return this.texture;
    }

    private String deriveTextureFromClass() {
        String name = getClass().getSimpleName();
        // Strip FC prefix: FCEntityCow → Cow
        if (name.startsWith("FCEntity")) name = name.substring(8);
        else if (name.startsWith("Entity")) name = name.substring(6);
        String lower = name.toLowerCase();
        return switch (lower) {
            case "cow" -> "/mob/cow.png";
            case "pig" -> "/mob/pig.png";
            case "sheep" -> "/mob/sheep.png";
            case "chicken" -> "/mob/chicken.png";
            case "wolf" -> "/mob/wolf.png";
            case "ocelot" -> "/mob/ocelot.png";
            case "creeper" -> "/mob/creeper.png";
            case "skeleton" -> "/mob/skeleton.png";
            case "zombie" -> "/mob/zombie.png";
            case "spider", "cavespider" -> "/mob/spider.png";
            case "enderman" -> "/mob/enderman.png";
            case "blaze" -> "/mob/fire.png";
            case "ghast" -> "/mob/ghast.png";
            case "slime" -> "/mob/slime.png";
            case "magmacube" -> "/mob/magmacube.png";
            case "bat" -> "/mob/bat.png";
            case "witch" -> "/mob/witch.png";
            case "villager" -> "/mob/villager/villager.png";
            case "snowman" -> "/mob/snowman.png";
            case "pigzombie" -> "/mob/pigzombie.png";
            case "wither", "witherpersistent" -> "/mob/wither.png";
            case "wolfdire" -> "/mob/wolf.png";
            case "junglespider" -> "/mob/spider.png";
            default -> "/mob/" + lower + ".png";
        };
    }

    public void performHurtAnimation() {}

    public float spiderScaleAmount() { return 1.0F; }

    public float getTailRotation() { return 0.0F; }
}
