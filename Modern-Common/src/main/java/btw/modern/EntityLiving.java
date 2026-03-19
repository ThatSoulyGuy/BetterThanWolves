package btw.modern;

public abstract class EntityLiving extends Entity {

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

    protected EntityLiving(World world) {
        super(world);
    }

    public abstract int getMaxHealth();

    public void jump() {}

    public ItemStack getHeldItem() {
        return null;
    }

    public void swingItem() {}

    public EntityLiving getAttackTarget() {
        return null;
    }

    public EntitySenses getEntitySenses() {
        return null;
    }

    public PathNavigate getNavigator() {
        return null;
    }

    public EntityLookHelper getLookHelper() {
        return null;
    }

    public void onLivingUpdate() {}

    public boolean attackEntityFrom(DamageSource source, int amount) {
        return false;
    }

    public void onDeath(DamageSource source) {}

    public void setAttackTarget(EntityLiving target) {}

    public float getEyeHeight() {
        return 0.0F;
    }

    public boolean canEntityBeSeen(Entity entity) {
        return false;
    }

    public int getTotalArmorValue() {
        return 0;
    }

    public boolean isPotionActive(int potionId) {
        return false;
    }

    public boolean isPotionActive(Potion potion) {
        return false;
    }

    public PotionEffect getActivePotionEffect(Potion potion) {
        return null;
    }

    public void addPotionEffect(PotionEffect effect) {}
    public void removePotionEffect(int potionId) {}

    public void heal(int amount) {}

    public ItemStack getCurrentItemOrArmor(int slot) {
        return null;
    }

    public ItemStack getCurrentArmor(int slot) {
        return getCurrentItemOrArmor(slot + 1);
    }

    public void setCurrentItemOrArmor(int slot, ItemStack stack) {}

    public boolean isOnLadder() {
        return false;
    }

    public boolean isEntityAlive() {
        return false;
    }

    public String getTranslatedEntityName() {
        return "";
    }

    public boolean isAIEnabled() {
        return false;
    }

    public void setRevengeTarget(EntityLiving target) {}

    public EntityLiving getLastAttackingEntity() {
        return null;
    }

    public boolean isPlayerSleeping() {
        return false;
    }

    public int getAge() {
        return 0;
    }

    public void clearActivePotions() {}

    // --- BTW-added methods ---

    public void dropFewItems(boolean bKilledByPlayer, int iLootingModifier) {}
    public int getDropItemId() { return 0; }
    public String getLivingSound() { return null; }
    public String getHurtSound() { return null; }
    public String getDeathSound() { return null; }
    public float getSoundVolume() { return 1.0F; }
    public float getSoundPitch() { return 1.0F; }
    public void initCreature() {}
    public void SpawnerInitCreature() {}
    public boolean interact(EntityPlayer player) { return false; }
    public boolean getCanSpawnHere() { return false; }
    public void updateEntityActionState() {}
    public void fall(float fFallDistance) {}
    public void dropHead() {}
    public boolean canTriggerWalking() { return true; }
    public AxisAlignedBB getCollisionBox(Entity entity) { return null; }
    public AxisAlignedBB getBoundingBox() { return null; }
    public void despawnEntity() {}
    public void setCanPickUpLoot(boolean canPickUp) {}
    public boolean getCanPickUpLoot() { return false; }
    public void setEntityHealth(int health) { this.health = health; }
    public int getHealth() { return this.health; }
    public void faceEntity(Entity entity, float maxYawChange, float maxPitchChange) {}
    public void setHomeArea(int x, int y, int z, int radius) {}
    public boolean hasHome() { return false; }
    public ChunkCoordinates getHomePosition() { return null; }
    public float getMaximumHomeDistance() { return -1.0F; }
    public void detachHome() {}
    public PathEntity getPathEntityToEntity(Entity entity) { return null; }
    public boolean hasPath() { return false; }
    public void setPathToEntity(PathEntity path) {}
    public EntityLiving getAITarget() { return null; }
    public boolean isChild() { return false; }
    public void renderBrokenItemStack(ItemStack stack) {}
    public java.util.Random getRNG() { return this.rand; }

    // --- BTW-added: entity methods ---
    public void EntityMobOnLivingUpdate() {}
    public boolean EntityMobAttackEntityFrom(DamageSource source, int amount) { return false; }
    public boolean EntityAnimalInteract(EntityPlayer player) { return false; }
    // Removed: void MeleeAttack(EntityLiving) - vanilla has boolean MeleeAttack(Entity) instead
    public void SetHungerLevel(int level) {}
    public int GetGrowthLevel() { return 0; }
    public void SetGrowthLevelNoNotify(int level) {}
    public void InitHungerWithVariance() {}
    public boolean GetIsUpsideDown() { return false; }
    public boolean GetIsPersistent() { return false; }
    public void SetPersistent(boolean persistent) {}
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
    public Vec3 getLook(float partialTicks) { return null; }
    public void knockBack(Entity entity, int damage, double motionX, double motionY) {}
    public int getVerticalFaceSpeed() { return 10; }
    public void spawnExplosionParticle() {}
    public boolean canSee(EntityLiving entity) { return false; }
    public void addOrRenewAgressor(EntityLiving entity) {}
    public void dealFireDamage(int amount) {}
    public void TransmitAttackTargetToClients() {}
    public void EntityLivingSetAttackTarget(EntityLiving target) {}
    public void EntityLivingOnLivingUpdate() {}
    public void EntityLivingOnDeath(DamageSource source) {}
    public void EntityLivingFall(float distance) {}
    public void EntityLivingDropFewItems(boolean recentlyHit, int lootingLevel) {}
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
        return this.texture;
    }

    public void performHurtAnimation() {}

    public float spiderScaleAmount() { return 1.0F; }

    public float getTailRotation() { return 0.0F; }
}
