package btw.modern;

public abstract class EntityPlayer extends EntityLiving {

    public FoodStats foodStats;
    public InventoryPlayer inventory;
    public PlayerCapabilities capabilities;
    public InventoryEnderChest theInventoryEnderChest;
    public Container openContainer;
    public Container inventoryContainer;
    public float cameraYaw;
    public float prevCameraYaw;

    /** Boolean value indicating whether a player is sleeping or not */
    public boolean sleeping;
    public double chasingPosX;
    public double chasingPosY;
    public double chasingPosZ;
    public double prevChasingPosX;
    public double prevChasingPosY;
    public double prevChasingPosZ;
    public String username;
    public boolean isCreativeMode;
    public int experienceLevel;
    public EntityFishHook fishEntity;
    public boolean disableDamage;

    /** Used by EntityPlayer to prevent too many xp orbs from getting absorbed at once. */
    // 1.5.2 EntityPlayer.xpCooldown (vanilla/server EntityPlayer.java:50) — read/written by the
    // frozen EntityXPOrb.onCollideWithPlayer (getfield btw/modern/EntityPlayer.xpCooldown) when a
    // player touches an XP orb; decremented per tick by ServerPlayerMixin.btw$tick.
    public int xpCooldown = 0;

    // --- FC fields ---
    public int m_iHungerPenaltyLevel;
    public int m_iFatPenaltyLevel;
    public int m_iHealthPenaltyLevel;
    public int m_iGloomLevel;
    public int m_iTimesCraftedThisTick;
    public int m_iInGloomCounter;
    public int m_iTicksSinceEmoteSound;
    public float m_fCurrentMiningSpeedModifier = 1F;
    public int m_iAirRecoveryCountdown;
    public ChunkCoordinates m_HardcoreSpawnChunk;
    public int m_iSpawnDimension;
    public long m_lTimeOfLastSpawnAssignment;
    public long m_lTimeOfLastDimensionSwitch;
    public long m_lRespawnAssignmentCooldownTimer;

    public static final int m_iGloomCounterBetweenStateChanges = 1200; // 1 minute

    private static final int m_iTicksBetweenEmoteSounds = 10;

    public static final float m_fExhaustionJumping = 0.2F;
    public static final float m_fExhaustionJumpingSprinting = 1.0F;

    protected EntityPlayer(World world) {
        super(world);
    }

    private ItemStack itemInUse;
    private int itemInUseCount;

    public boolean isUsingItem() {
        return itemInUse != null && itemInUseCount > 0;
    }

    public ItemStack getCurrentEquippedItem() {
        if (this.inventory != null) {
            return this.inventory.getCurrentItem();
        }
        return null;
    }

    public void playSound(String name, float volume, float pitch) {
        if (this.worldObj != null) {
            this.worldObj.playSoundAtEntity(this, name, volume, pitch);
        }
    }

    public void addChatMessage(String message) {}

    public boolean canEat(boolean alwaysEdible) {
        // FC: Prevent eating while having the hunger potion effect
        if (isPotionActive(Potion.hunger)) {
            return false;
        }
        return (alwaysEdible || this.foodStats.needFood()) && !this.capabilities.disableDamage;
    }

    /**
     * Whether FoodStats.onUpdate may heal this player. 1.5.2 had no naturalRegeneration gamerule (vanilla/server
     * GameRules.java:26-32), so FC always healed; the server bridge (btw.forge.PlayerBridge) answers the 1.20.1 rule.
     * FCMOD-INTEGRATION(1.20.1): hunger_contract: the gamerule and regen-gate mods reach FC's heal (INTEGRATIONS #021).
     */
    public boolean IsNaturalRegenerationEnabled() {
        return true;
    }

    public void displayGUIChest(IInventory inventory) {}
    public void displayGUIFurnace(TileEntity tileEntity) {}
    public void displayGUIDispenser(TileEntity tileEntity) {}
    public void displayGUIHopper(TileEntity tileEntity) {}
    public void displayGUIHopperMinecart(EntityMinecartHopper minecart) {}

    // 1.5.2 EntityPlayer.func_71066_bF — server impl returns false (client-side
    // it reports sleeping-in-bed); frozen Entity.updateRiderPosition calls it
    // whenever a player rides an FC entity. Beds are disabled in BTW anyway.
    public boolean func_71066_bF() { return false; }

    // 1.5.2 EntityPlayer.func_96122_a — PvP friendly-fire check via scoreboard
    // teams; frozen EntityArrow calls it when a player-shot FC arrow hits a
    // player. The shim has no scoreboard teams, and 1.5.2's no-team semantics
    // are "attack allowed", so true is the faithful default.
    public boolean func_96122_a(EntityPlayer other) { return true; }
    public void displayGUIBrewingStand(TileEntity tileEntity) {}
    public void displayGUIBeacon(TileEntity tileEntity) {}
    public void displayGUIAnvil(int x, int y, int z) {}
    public void displayGUIMerchant(IMerchant merchant, String title) {}
    public void displayGUIEnchantment(int x, int y, int z, String name) {}

    /** Set to true when exhaustion is added; read by HUD rendering to trigger food pip shake. */
    public boolean m_bExhaustionAddedSinceLastGuiUpdate = false;

    public void addExhaustion(float exhaustion) {
        if (!this.capabilities.disableDamage) {
            if (!this.worldObj.isRemote) {
                // FC: Apply armor weight exhaustion modifier
                exhaustion *= GetArmorExhaustionModifier();
                this.foodStats.addExhaustion(exhaustion);
                m_bExhaustionAddedSinceLastGuiUpdate = true;
            }
        }
    }

    public void setItemInUse(ItemStack stack, int duration) {
        this.itemInUse = stack;
        this.itemInUseCount = duration;
    }

    public ItemStack getItemInUse() {
        return this.itemInUse;
    }

    public int getItemInUseCount() {
        return this.itemInUseCount;
    }

    public void clearItemInUse() {
        this.itemInUse = null;
        this.itemInUseCount = 0;
    }

    public FoodStats getFoodStats() {
        return foodStats;
    }

    public boolean isPlayerSleeping() {
        return this.sleeping;
    }

    public void triggerAchievement(StatBase stat) {}

    public void addStat(StatBase stat, int amount) {}

    public void destroyCurrentEquippedItem() {
        if (this.inventory != null) {
            this.inventory.setInventorySlotContents(this.inventory.currentItem, null);
        }
    }

    public boolean isInCreativeMode() {
        return this.capabilities != null && this.capabilities.isCreativeMode;
    }

    public void addExperience(int amount) {}

    /**
     * 1.5.2 EntityLiving.onItemPickup — broadcasts the collect-item packet so nearby clients
     * play the pickup fly-to-player animation. Declared HERE because the frozen
     * EntityXPOrb.onCollideWithPlayer resolves it against btw.modern.EntityPlayer, and the
     * frozen EntityLiving version casts worldObj to the frozen WorldServer (which would
     * ClassCastException on a WorldBridge). PlayerBridge overrides this to delegate to
     * ServerPlayer.take(); packet broadcast is engine-side, so the base is a safe no-op.
     */
    public void onItemPickup(Entity entity, int count) {}

    public float getCurrentPlayerStrVsBlock(Block block, boolean flag) {
        // Delegate to position-aware version with dummy coords
        return getCurrentPlayerStrVsBlock(block, 0, 0, 0);
    }

    public float getCurrentPlayerStrVsBlock(Block block, int i, int j, int k) {
        // FC position-aware tool speed calculation
        float str = 1.0F;

        if (this.inventory != null) {
            str = this.inventory.getStrVsBlock(worldObj, block, i, j, k);
        }

        // Apply efficiency enchantment
        if (str > 1.0F) {
            int efficiency = EnchantmentHelper.getEfficiencyModifier(this);
            ItemStack currentItem = getCurrentEquippedItem();

            if (efficiency > 0 && currentItem != null) {
                float bonus = (float)(efficiency * efficiency + 1);

                if (!currentItem.canHarvestBlock(worldObj, block, i, j, k) && str <= 1.0F) {
                    str += bonus * 0.08F;
                } else {
                    str += bonus;
                }
            }
        }

        // Apply dig speed (haste) potion buff
        if (isPotionActive(Potion.digSpeed)) {
            PotionEffect hasteEffect = getActivePotionEffect(Potion.digSpeed);
            if (hasteEffect != null) {
                str *= 1.0F + (float)(hasteEffect.getAmplifier() + 1) * 0.2F;
            }
        }

        // Apply dig slowdown (mining fatigue) potion debuff
        if (isPotionActive(Potion.digSlowdown)) {
            PotionEffect fatigueEffect = getActivePotionEffect(Potion.digSlowdown);
            if (fatigueEffect != null) {
                str *= 1.0F - (float)(fatigueEffect.getAmplifier() + 1) * 0.2F;
            }
        }

        // Penalty for mining underwater without aqua affinity
        if (isInsideOfMaterial(Material.water) && !EnchantmentHelper.getAquaAffinityModifier(this)) {
            str /= 5.0F;
        }

        // Penalty for not on ground
        if (!onGround) {
            str /= 5.0F;
        }

        // FC: Apply mining speed modifier (health/hunger/gloom penalties)
        str *= GetMiningSpeedModifier();

        return str;
    }

    public boolean IsCurrentToolEffectiveOnBlock(Block targetBlock, int i, int j, int k) {
        // FC implementation: check if held item is efficient vs block
        if (this.inventory == null) return false;
        ItemStack currentItemStack = getCurrentEquippedItem();
        if (currentItemStack != null && currentItemStack.getItem() != null) {
            return currentItemStack.getItem().IsEfficientVsBlock(currentItemStack, worldObj, targetBlock, i, j, k);
        }
        return false;
    }

    /**
     * FC's harvest check: can this player harvest the given block at the given position?
     * Delegates to the held item's canHarvestBlock via inventory.
     * This determines whether harvestBlock (proper) or OnBlockDestroyedWithImproperTool
     * (improper) is called.
     */
    public boolean canHarvestBlock(Block block, int i, int j, int k) {
        // Blocks that don't require a tool can always be harvested (bare hand works)
        if (block.blockMaterial != null && block.blockMaterial.isToolNotRequired()) {
            return true;
        }
        if (this.inventory != null) {
            return this.inventory.canHarvestBlock(worldObj, block, i, j, k);
        }
        return false;
    }

    public void displayGUIWorkbench(int x, int y, int z) {}
    public void openGui(Object mod, int guiId, World world, int x, int y, int z) {}
    public void setSpawnChunk(ChunkCoordinates pos, boolean forced) {}
    public void setSpawnChunk(ChunkCoordinates pos, boolean forced, int dimensionId) {}
    public ChunkCoordinates getBedLocation() { return null; }
    public boolean isSpawnForced() { return false; }
    public void func_71012_a(ItemStack stack) {}
    public Object getGameProfile() { return null; }
    public String getCommandSenderName() { return this.username; }
    public void addToPlayerScore(Entity entity, int amount) {}
    public EnumStatus sleepInBedAt(int x, int y, int z) {
        // FC: Sleeping is removed
        return EnumStatus.OTHER_PROBLEM;
    }
    public void wakeUpPlayer(boolean immediately, boolean updateWorldFlag, boolean setSpawn) {}

    public boolean canPlayerEdit(int x, int y, int z, int side, ItemStack stack) {
        // FC: Prevent placing blocks while in mid air (unless creative, on ground, in water, on ladder, riding, or in lava)
        if (this.capabilities != null && !this.capabilities.isCreativeMode
                && !onGround && !inWater && !isOnLadder() && ridingEntity == null && !handleLavaMovement()) {
            return false;
        }
        return this.capabilities != null && this.capabilities.allowEdit;
    }
    /**
     * Drops an item from the player's inventory at their position.
     * In vanilla 1.5.2, this called dropPlayerItemWithRandomChoice(stack, false)
     * which spawned an EntityItem with a slight random velocity.
     *
     * PlayerBridge overrides this to delegate to the real MC player's drop()
     * method. This base implementation provides a fallback using entityDropItem
     * so that items are not silently lost if called on a non-bridged instance.
     */
    public EntityItem dropPlayerItem(ItemStack stack) {
        if (stack == null) return null;
        return this.entityDropItem(stack, 0.0F);
    }
    public InventoryEnderChest getInventoryEnderChest() { return theInventoryEnderChest; }
    public void SetItemInUseCount(int count) { this.itemInUseCount = count; }
    public ItemStack getCurrentArmor(int slot) {
        if (this.inventory != null && slot >= 0 && slot < this.inventory.armorInventory.length) {
            return this.inventory.armorInventory[slot];
        }
        return null;
    }

    /**
     * Returns the held item (slot 0) or armor piece (slots 1-4).
     */
    public ItemStack getEquipmentInSlot(int slot) {
        if (this.inventory == null) return null;
        return slot == 0 ? this.inventory.getCurrentItem() : this.inventory.armorInventory[slot - 1];
    }

    /**
     * Returns the item the player is currently holding.
     */
    public ItemStack getHeldItem() {
        return this.inventory != null ? this.inventory.getCurrentItem() : null;
    }

    /**
     * Sets armor in the given slot. Slot 0-3 maps to armorInventory.
     */
    public void setCurrentItemOrArmor(int slot, ItemStack stack) {
        if (this.inventory != null && slot >= 0 && slot < this.inventory.armorInventory.length) {
            this.inventory.armorInventory[slot] = stack;
        }
    }

    public void addExperienceLevel(int levels) {
        this.experienceLevel += levels;
        if (this.experienceLevel < 0) {
            this.experienceLevel = 0;
        }
    }
    public void setLevelsServerSafe(int levels) {}
    public int getFoodLevel() {
        return this.foodStats != null ? this.foodStats.getFoodLevel() : 0;
    }
    public boolean isBlocking() {
        return this.isUsingItem() && this.getItemInUse() != null
            && this.getItemInUse().getItem() != null
            && this.getItemInUse().getItem().getItemUseAction(this.getItemInUse()) == EnumAction.block;
    }
    public void heal(int amount) {
        super.heal(amount);
    }
    public void displayGUIEditSign(TileEntity sign) {}
    public void addStats(int stat, float amount) {}
    private int currentWindowId;
    public int IncrementAndGetWindowID() {
        this.currentWindowId = this.currentWindowId % 100 + 1;
        return this.currentWindowId;
    }
    public void HandleStartBlockHarvest(Object packet) {}
    public boolean IsLocalPlayerAndHittingBlock() { return false; }
    public void OnNearbyFireStartAttempt(EntityPlayer player) {}
    public int GetHungerLevel() { return 0; }
    public void AttemptToPossessNearbyCreatureOnDeath() {}
    // boolean to match real 1.5.2 EntityCreature and the EntityLiving shim decl
    public boolean AttemptToPossessNearbyCreature(double range, boolean persistentSpirit) { return false; }
    public void AttemptToPossessCreaturesAroundBlock(World world, int x, int y, int z, int range, int dim) {}

    /**
     * Returns true if the player has silk touch on the held item.
     * Declared HERE (not on EntityLiving) because the runtime EntityLiving is
     * FC's real 1.5.2 class — methods invented on shadowed classes throw
     * NoSuchMethodError. PlayerBridge overrides this with the modern player's
     * live held item; this default is the real 1.5.2 logic over FC state.
     */
    public boolean getSilkTouchEnchant() {
        return EnchantmentHelper.getEnchantmentLevel(Enchantment.silkTouch.effectId, getHeldItem()) > 0;
    }
    public void AddRawChatMessage(String message) {}
    public boolean AddStackToCurrentHeldStackIfEmpty(ItemStack stack) {
        if (getCurrentEquippedItem() == null && this.inventory != null) {
            this.inventory.setInventorySlotContents(this.inventory.currentItem, stack.copy());
            return true;
        }
        return false;
    }
    public static boolean InstallationIntegrityTestPlayer() { return true; }

    // =========================================================================
    // FC Penalty System - Getters and Setters
    // =========================================================================

    public int GetFatPenaltyLevel() {
        return m_iFatPenaltyLevel;
    }

    public void SetFatPenaltyLevel(int iPenaltyLevel) {
        m_iFatPenaltyLevel = iPenaltyLevel;
    }

    public int GetHungerPenaltyLevel() {
        return m_iHungerPenaltyLevel;
    }

    public void SetHungerPenaltyLevel(int iPenaltyLevel) {
        m_iHungerPenaltyLevel = iPenaltyLevel;
    }

    public int GetHealthPenaltyLevel() {
        return m_iHealthPenaltyLevel;
    }

    public void SetHealthPenaltyLevel(int iPenaltyLevel) {
        m_iHealthPenaltyLevel = iPenaltyLevel;
    }

    public int GetGloomLevel() {
        return m_iGloomLevel;
    }

    public void SetGloomLevel(int iGloomLevel) {
        m_iGloomLevel = iGloomLevel;
    }

    /**
     * Returns the maximum of all three penalty levels (health, hunger, fat).
     */
    public int GetMaximumStatusPenaltyLevel() {
        int iMaximumPenaltyLevel = GetHealthPenaltyLevel();
        int iHungerPenaltyLevel = GetHungerPenaltyLevel();

        if (iHungerPenaltyLevel > iMaximumPenaltyLevel) {
            iMaximumPenaltyLevel = iHungerPenaltyLevel;
        }

        int iFatPenaltyLevel = GetFatPenaltyLevel();

        if (iFatPenaltyLevel > iMaximumPenaltyLevel) {
            iMaximumPenaltyLevel = iFatPenaltyLevel;
        }

        return iMaximumPenaltyLevel;
    }

    /**
     * Returns true if any status penalty is active (health low, hungry, or overfed).
     */
    public boolean HasStatusPenalty() {
        return getHealth() <= 10 || foodStats.getFoodLevel() <= 24 || (int) foodStats.getSaturationLevel() >= 12;
    }

    // =========================================================================
    // FC Modifiers
    // =========================================================================

    /**
     * Pure computation of the FC health/exhaustion modifier from its raw inputs.
     * Extracted so the CLIENT — which only has the synced penalty levels (not a full
     * EntityPlayer) — can reproduce the exact server-side value for the local player's
     * movement penalty. Keeping ONE formula here prevents client/server divergence.
     * See the client-side movement-penalty mixin.
     *
     * penalty 0/1 -> 1.0, 2 -> 0.75, 3 -> 0.5, 4+ -> 0.25; halved again if gloom > 0.
     */
    public static float computeHealthAndExhaustionModifier(int iMaxPenaltyLevel, int iGloomLevel) {
        float fModifier = 1.0F;

        if (iMaxPenaltyLevel >= 2) {
            if (iMaxPenaltyLevel >= 3) {
                if (iMaxPenaltyLevel >= 4) {
                    fModifier = 0.25F;
                } else {
                    fModifier = 0.5F;
                }
            } else {
                fModifier = 0.75F;
            }
        }

        if (iGloomLevel > 0) {
            fModifier *= 0.5F;
        }

        return fModifier;
    }

    /**
     * Returns a modifier based on the maximum status penalty level.
     * penalty 0 -> 1.0, 1 -> 1.0, 2 -> 0.75, 3 -> 0.5, 4+ -> 0.25
     */
    public float GetHealthAndExhaustionModifier() {
        return computeHealthAndExhaustionModifier(GetMaximumStatusPenaltyLevel(), 0);
    }

    /**
     * Returns GetHealthAndExhaustionModifier() halved if gloom level > 0.
     */
    public float GetHealthAndExhaustionModifierWithSightlessModifier() {
        return computeHealthAndExhaustionModifier(GetMaximumStatusPenaltyLevel(), GetGloomLevel());
    }

    public float GetMiningSpeedModifier() {
        return m_fCurrentMiningSpeedModifier;
    }

    public void SetMiningSpeedModifier(float fModifier) {
        if (fModifier > 1F) {
            // cap it just in case the client sends an invalid speed to the server
            fModifier = 1F;
        }

        m_fCurrentMiningSpeedModifier = fModifier;
    }

    public float UpdateMiningSpeedModifier() {
        m_fCurrentMiningSpeedModifier = GetHealthAndExhaustionModifierWithSightlessModifier();

        return m_fCurrentMiningSpeedModifier;
    }

    public float GetMeleeDamageModifier() {
        return GetHealthAndExhaustionModifierWithSightlessModifier();
    }

    /**
     * Bow pull strength uses base modifier WITHOUT gloom penalty.
     */
    public float GetBowPullStrengthModifier() {
        return GetHealthAndExhaustionModifier();
    }

    public float GetSwimmingHorizontalModifier() {
        return GetHealthAndExhaustionModifierWithSightlessModifier();
    }

    public float GetLandMovementModifier() {
        return GetHealthAndExhaustionModifierWithSightlessModifier();
    }

    public float GetLadderVerticalMovementModifier() {
        return GetHealthAndExhaustionModifierWithSightlessModifier();
    }

    public float GetJumpingHorizontalMovementModifier() {
        return GetHealthAndExhaustionModifierWithSightlessModifier();
    }

    // =========================================================================
    // FC Armor
    // =========================================================================

    /**
     * Returns the exhaustion modifier based on worn armor weight.
     * 1.0 + (weight / 44), capping at ~2x with full plate armor.
     */
    public float GetArmorExhaustionModifier() {
        float fModifier = 1.0F;

        int iWeight = GetWornArmorWeight();

        if (iWeight > 0) {
            fModifier += (float) iWeight / 44F;
        }

        return fModifier;
    }

    /**
     * Returns total weight of worn armor.
     * Sums Item.GetWeightWhenWorn() for all equipped armor pieces.
     */
    public int GetWornArmorWeight() {
        int weight = 0;
        if (inventory != null) {
            for (int i = 0; i < inventory.armorInventory.length; i++) {
                ItemStack armorStack = inventory.armorInventory[i];
                if (armorStack != null && armorStack.getItem() != null) {
                    weight += armorStack.getItem().GetWeightWhenWorn();
                }
            }
        }
        return weight;
    }

    /**
     * Checks if all 4 armor slots contain soulforged (refined) armor.
     * TODO: Needs inventory bridge to access armorInventory and FCItemArmorRefined
     */
    public boolean IsWearingFullSuitSoulforgedArmor() {
        if (inventory == null) return false;
        for (int i = 0; i < inventory.armorInventory.length; i++) {
            ItemStack armorStack = inventory.armorInventory[i];
            if (armorStack == null || armorStack.getItem() == null) return false;
            // FCItemArmorRefined is the soulforged plate armor base class
            // After shadow remapping, it lives in btw.modern package
            String className = armorStack.getItem().getClass().getSimpleName();
            if (!className.contains("ArmorRefined") && !className.contains("ArmorSoulforged")) return false;
        }
        return true;
    }

    /**
     * Checks if the helm slot contains the soulforged plate helm.
     * TODO: Needs inventory bridge to access armorInventory[3]
     */
    public boolean IsWearingSoulforgedHelm() {
        if (inventory == null) return false;
        ItemStack helm = inventory.armorInventory[3];
        if (helm == null || helm.getItem() == null) return false;
        String className = helm.getItem().getClass().getSimpleName();
        return className.contains("ArmorRefined") || className.contains("ArmorSoulforged");
    }

    // 4.B EntityPlayer (vanilla/client EntityPlayer.java:3391-3407): an FC squid may head-crab a living player who can take
    // damage and is neither riding nor ridden, keeps a living player as its target, and the soulforged plate helm stops its bite.
    // Entity's defaults (vanilla/client Entity.java:2524-2556) refused all three for every player (triage CF2-MX3).
    @Override
    public boolean GetCanBeHeadCrabbed(boolean bSquidInWater) {
        return isEntityAlive() && capabilities != null && !capabilities.disableDamage
                && riddenByEntity == null && ridingEntity == null;
    }

    @Override
    public boolean IsValidOngoingAttackTargetForSquid() {
        return isEntityAlive();
    }

    @Override
    public boolean IsImmuneToHeadCrabDamage() {
        return IsWearingSoulforgedHelm();
    }

    /**
     * Checks if the boots slot contains the soulforged plate boots.
     * TODO: Needs inventory bridge to access armorInventory[0]
     */
    public boolean IsWearingSoulforgedBoots() {
        if (inventory == null) return false;
        ItemStack boots = inventory.armorInventory[0];
        return boots != null && IsSoulforgedBootsItem(boots.getItem());
    }

    /**
     * The boots-slot test of {@link #IsWearingSoulforgedBoots()}, shared with the bridge's client-side
     * step-sound hook, which must agree with the server on when the boots replace the regular step.
     */
    public static boolean IsSoulforgedBootsItem(Item item) {
        if (item == null) return false;
        String className = item.getClass().getSimpleName();
        return className.contains("ArmorRefined") || className.contains("ArmorSoulforged");
    }

    /**
     * Respiration enchant level. Declared here (not only on EntityLiving) because
     * the runtime {@code btw.modern.EntityLiving} is FC's vanilla 1.5.2 class — it
     * is shadowed out of Modern-Common and lacks this bridge method, so calling it
     * via the inherited reference NoSuchMethodErrors at runtime. EntityPlayer is NOT
     * shadowed, so declaring it here makes the call in {@link #decreaseAirSupply(int)}
     * resolve. PlayerBridge overrides this with the real value from the live player.
     */
    public int getRespirationEnchantLevel() { return 0; }

    /**
     * FC's air-supply decrease (replaces vanilla). A soulforged helm makes the
     * Respiration enchantment quadratically effective — the per-tick chance to
     * keep air is {@code 1 - 1/(level^2 + 1)} vs vanilla's linear
     * {@code 1 - 1/(level + 1)}. Mirrors FC EntityPlayer.decreaseAirSupply.
     * Real enchant/armor data arrives through the bridge-overridden
     * {@link #getRespirationEnchantLevel()} and {@link #IsWearingSoulforgedHelm()}.
     */
    public int decreaseAirSupply(int iAirSupply) {
        m_iAirRecoveryCountdown = 20;

        int iEnchantmentLevel = getRespirationEnchantLevel();

        if (iEnchantmentLevel > 0 && IsWearingSoulforgedHelm()) {
            if (worldObj != null && worldObj.getWorldTime() % 100 == 0) {
                worldObj.playSoundAtEntity(this, "random.breath",
                        0.75F + rand.nextFloat() * 0.5F,
                        0.5F + rand.nextFloat() * 0.025F);
            }

            return rand.nextInt(iEnchantmentLevel * iEnchantmentLevel + 1) > 0
                    ? iAirSupply : iAirSupply - 1;
        }

        // No soulforged helm: vanilla linear Respiration roll (1.5.2 base).
        return iEnchantmentLevel > 0 && rand.nextInt(iEnchantmentLevel + 1) > 0
                ? iAirSupply : iAirSupply - 1;
    }

    // =========================================================================
    // FC Step Sounds
    // =========================================================================

    /**
     * FC's EntityPlayer.playStepSound override (vanilla/server EntityPlayer.java:2454-2492): the grunt, then
     * the soulforged-boots step in place of the regular one. The bridge's players step through 1.20.1, so
     * PlayerMixin calls the two parts below directly and keeps 1.20.1's own step as the regular one.
     */
    @Override
    public void playStepSound(int i, int j, int k, int iBlockID) {
        PlayStepGrunt();

        if (!PlaySoulforgedBootsStep(i, j, k, iBlockID)) {
            super.playStepSound(i, j, k, iBlockID);
        }
    }

    /**
     * The low health/exhaustion grunt played along with every step (vanilla/server EntityPlayer.java:2457-2468).
     * 1.5.2 computes a grunt volume there but plays a fixed 0.5F.
     */
    public void PlayStepGrunt() {
        if (worldObj != null && GetHealthAndExhaustionModifier() < 0.26F) {
            worldObj.playSoundAtEntity(this, "random.classic_hurt", 0.5F, 1F + rand.nextFloat() * 0.1F);
        }
    }

    /**
     * The soulforged-boots branch (vanilla/server EntityPlayer.java:2470-2487): the step of the ground cover
     * resting above the block stepped on, else of that block unless it is a liquid, at 0.3x volume and lowered
     * pitch. Returns true when the boots are worn, i.e. when this replaces the regular step sound.
     */
    public boolean PlaySoulforgedBootsStep(int i, int j, int k, int iBlockID) {
        if (!IsWearingSoulforgedBoots()) {
            return false;
        }

        if (worldObj == null) {
            return true;
        }

        int iBlockAboveID = worldObj.getBlockId(i, j + 1, k);
        Block blockAbove = iBlockAboveID > 0 && iBlockAboveID < Block.blocksList.length
                ? Block.blocksList[iBlockAboveID] : null;

        if (blockAbove != null && blockAbove.IsGroundCover()) {
            StepSound stepSound = blockAbove.stepSound;

            if (stepSound != null) {
                worldObj.playSoundAtEntity(this, stepSound.getStepSound(),
                        stepSound.getVolume() * 0.3F, stepSound.getPitch() * 0.75F);
            }
        } else {
            // 1.5.2 dereferences blocksList[iBlockID] unguarded; a block FC does not know plays nothing here
            Block block = iBlockID > 0 && iBlockID < Block.blocksList.length ? Block.blocksList[iBlockID] : null;

            if (block != null && (block.blockMaterial == null || !block.blockMaterial.isLiquid())) {
                StepSound stepSound = block.GetStepSound(worldObj, i, j, k);

                if (stepSound != null) {
                    worldObj.playSoundAtEntity(this, stepSound.getStepSound(),
                            stepSound.getVolume() * 0.3F, stepSound.getPitch() * 0.5F);
                }
            }
        }

        return true;
    }

    // =========================================================================
    // FC Status Updates
    // =========================================================================

    /**
     * Called each tick to update all mod status variables.
     */
    public void UpdateModStatusVariables() {
        UpdateGloomState();
        UpdateHungerPenaltyLevel();
        UpdateFatPenaltyLevel();
        UpdateHealthPenaltyLevel();
    }

    /**
     * Updates gloom state based on light level.
     * Empty in the base class like 1.5.2 EntityPlayer; the real implementation is the
     * EntityPlayerMP override (vanilla/server EntityPlayerMP.java:1205), ported to the
     * EntityPlayerMP shim.
     */
    public void UpdateGloomState() {}

    /**
     * Updates hunger penalty level based on food level thresholds.
     * Thresholds: >24 -> 0, >18 -> 1, >12 -> 2, >6 -> 3, >0 or sat>0 -> 4, else -> 5
     */
    public void UpdateHungerPenaltyLevel() {
        SetHungerPenaltyLevel(hungerPenaltyLevelFor(foodStats.getFoodLevel(), foodStats.getSaturationLevel()));
    }

    /**
     * The hunger penalty level (0-5) for an FC food level (0-60) and fat, as {@link #UpdateHungerPenaltyLevel} sets it.
     * FCMOD-INTEGRATION(1.20.1): hunger_contract: static, so the eat preview and food-bar mods compute FC's penalty
     * levels without a player (INTEGRATIONS #021).
     */
    public static int hungerPenaltyLevelFor(int foodLevel, float fat) {
        int iPenaltyLevel = 5;

        if (foodLevel > 24) {
            iPenaltyLevel = 0;
        } else if (foodLevel > 18) {
            iPenaltyLevel = 1;
        } else if (foodLevel > 12) {
            iPenaltyLevel = 2;
        } else if (foodLevel > 6) {
            iPenaltyLevel = 3;
        } else if (foodLevel > 0 || fat > 0F) {
            iPenaltyLevel = 4;
        }

        return iPenaltyLevel;
    }

    /**
     * Updates fat penalty level based on saturation thresholds.
     * Thresholds: <12 -> 0, <14 -> 1, <16 -> 2, <18 -> 3, >=18 -> 4
     */
    public void UpdateFatPenaltyLevel() {
        SetFatPenaltyLevel(fatPenaltyLevelFor(foodStats.getSaturationLevel()));
    }

    /**
     * The fat penalty level (0-4) for an FC fat (saturation) value, as {@link #UpdateFatPenaltyLevel} sets it.
     * FCMOD-INTEGRATION(1.20.1): hunger_contract: static, for the eat preview and food-bar mods (INTEGRATIONS #021).
     */
    public static int fatPenaltyLevelFor(float fat) {
        int iFat = (int) fat;
        int iFatLevel = 4;

        if (iFat < 12) {
            iFatLevel = 0;
        } else if (iFat < 14) {
            iFatLevel = 1;
        } else if (iFat < 16) {
            iFatLevel = 2;
        } else if (iFat < 18) {
            iFatLevel = 3;
        }

        return iFatLevel;
    }

    /**
     * Updates health penalty level based on health thresholds.
     * Thresholds: >10 -> 0, >8 -> 1, >6 -> 2, >4 -> 3, >2 -> 4, <=2 -> 5
     */
    public void UpdateHealthPenaltyLevel() {
        int iHealth = getHealth();
        int iPenaltyLevel = 5;

        if (iHealth > 10) {
            iPenaltyLevel = 0;
        } else if (iHealth > 8) {
            iPenaltyLevel = 1;
        } else if (iHealth > 6) {
            iPenaltyLevel = 2;
        } else if (iHealth > 4) {
            iPenaltyLevel = 3;
        } else if (iHealth > 2) {
            iPenaltyLevel = 4;
        }

        SetHealthPenaltyLevel(iPenaltyLevel);
    }

    // =========================================================================
    // FC Movement Gating
    // =========================================================================

    /**
     * Player can jump if health > 4, food level > 12, and saturation < 18.
     */
    public boolean CanJump() {
        return health > 4 && foodStats.getFoodLevel() > 12 && (int) foodStats.getSaturationLevel() < 18;
    }

    /**
     * Player can swim if not weighted by armor and health > 4.
     * Uses isWeighted(): GetWornArmorWeight() >= 10, or a squid on the head.
     */
    public boolean CanSwim() {
        return !isWeighted() && health > 4;
    }

    /**
     * 4.B EntityLiving.isWeighted (vanilla/client EntityLiving.java:3577-3592): worn armour weight 10 or more, or a squid on
     * the head (IsWeightedByHeadCrab -> Entity.HasHeadCrabbedSquid, Entity.java:2543-2546). CanSwim reads it, so a
     * head-crabbed player cannot swim up (LivingEntityMixin.btw$gateSwimming). The head-crab clause was missing (CF2-MX3).
     */
    public boolean isWeighted() {
        return GetWornArmorWeight() >= 10 || IsWeightedByHeadCrab();
    }

    // =========================================================================
    // FC Exhaustion
    // =========================================================================

    /**
     * Adds exhaustion for a jump: 0.2F normally, 1.0F if sprinting.
     * Multiplied by armor exhaustion modifier.
     */
    public void AddExhaustionForJump() {
        if (isSprinting()) {
            addExhaustion(m_fExhaustionJumpingSprinting);
        } else {
            addExhaustion(m_fExhaustionJumping);
        }
    }

    /**
     * Adds exhaustion without any visual feedback (no hunger bar shake).
     */
    public void AddExhaustionWithoutVisualFeedback(float fAmount) {
        addExhaustion(fAmount);
    }

    /**
     * 1.5.2 EntityPlayer.addMovementStat's exhaustion (vanilla/server EntityPlayer.java:1707-1780) for one move of
     * (dx, dy, dz): under water 0.015 per metre of the 3D distance, swimming at the surface 0.015 per horizontal metre,
     * nothing on a ladder, on the ground 0.1 per metre sprinting and 0.01 per metre walking (sneaking included, as in
     * 1.5.2). The water and walking costs carry no visual feedback, as FC changed them. Without the stats and without
     * the 0.025 swim-up charge at its head, which the swim gate charges per tick (LivingEntityMixin.btw$gateSwimming).
     * FCMOD-INTEGRATION(1.20.1): hunger_contract: 1.20.1 Player.checkMovementStatistics charges walking 0 and water
     * 0.01 per metre; PlayerMixin drops those amounts and charges this 1.5.2 rule instead (decision D-A).
     */
    public void AddMovementExhaustion(double dx, double dy, double dz) {
        if (this.ridingEntity != null) {
            return;
        }

        int iDistance;

        if (this.isInsideOfMaterial(Material.water)) {
            iDistance = Math.round((float) Math.sqrt(dx * dx + dy * dy + dz * dz) * 100.0F);

            if (iDistance > 0) {
                AddExhaustionWithoutVisualFeedback(0.015F * (float) iDistance * 0.01F);
            }
        } else if (this.isInWater()) {
            iDistance = Math.round((float) Math.sqrt(dx * dx + dz * dz) * 100.0F);

            if (iDistance > 0) {
                AddExhaustionWithoutVisualFeedback(0.015F * (float) iDistance * 0.01F);
            }
        } else if (this.isOnLadder()) {
            // climbing costs nothing beyond the jump and the harvest charges
        } else if (this.onGround) {
            iDistance = Math.round((float) Math.sqrt(dx * dx + dz * dz) * 100.0F);

            if (iDistance > 0) {
                if (this.isSprinting()) {
                    addExhaustion(0.099999994F * (float) iDistance * 0.01F);
                } else {
                    AddExhaustionWithoutVisualFeedback(0.01F * (float) iDistance * 0.01F);
                }
            }
        }
    }

    /**
     * Adds exhaustion for harvesting a block, based on the tool used.
     * FC: Queries Item.GetExhaustionOnUsedToHarvestBlock() for the held item, with a default of 0.025F.
     */
    public void AddHarvestBlockExhaustion(int iBlockID, int iBlockI, int iBlockJ, int iBlockK, int iBlockMetadata) {
        float fExhaustionConsumed = 0.025F; // default exhaustion amount

        if (this.inventory != null) {
            ItemStack currentItemStack = this.inventory.mainInventory[this.inventory.currentItem];

            if (currentItemStack != null) {
                fExhaustionConsumed = currentItemStack.getItem().GetExhaustionOnUsedToHarvestBlock(
                    iBlockID, worldObj, iBlockI, iBlockJ, iBlockK, iBlockMetadata);
            }
        }

        if (fExhaustionConsumed > 0F) {
            addExhaustion(fExhaustionConsumed);
        }
    }

    // =========================================================================
    // FC Misc
    // =========================================================================

    // Legacy item IDs resolved reflectively from FC's FCBetterThanWolves registry —
    // Modern-Common cannot compile against the FC sources (same Class.forName pattern
    // as PlayerBridge.displayGUIWorkbench). -1 = unresolved; re-tried until FC init
    // has registered the items (long before any player can take damage).
    private static int fcItemBlastingOilID = -1;
    private static int fcItemHellfireDustID = -1;

    private static int ResolveFCItemID(String itemFieldName) {
        try {
            Class<?> fcClass = Class.forName("net.minecraft.src.btw.core.FCBetterThanWolves");
            Item item = (Item) fcClass.getField(itemFieldName).get(null);

            if (item != null) {
                return item.itemID;
            }
        } catch (Exception e) {
            // FC not present/initialized — leave unresolved
        }

        return -1;
    }

    private static int GetBlastingOilItemID() {
        if (fcItemBlastingOilID < 0) {
            fcItemBlastingOilID = ResolveFCItemID("fcItemBlastingOil");
        }

        return fcItemBlastingOilID;
    }

    private static int GetHellfireDustItemID() {
        if (fcItemHellfireDustID < 0) {
            fcItemHellfireDustID = ResolveFCItemID("fcItemHellfireDust");
        }

        return fcItemHellfireDustID;
    }

    /** m_iIgnoreMetadata to disregard metadata — FCUtilsInventory.m_iIgnoreMetadata (Common FCUtilsInventory.java:25). */
    private static final int m_iIgnoreMetadata = 32767;

    // 1.5.2 FCUtilsInventory.CountItemsInInventory (Common FCUtilsInventory.java:244, bMetaDataExclusive=false) —
    // local port for DetonateCarriedBlastingOil. Compares ItemStack.itemID directly instead of
    // getItem().itemID so stacks holding modern-only items (no legacy Item entry) don't NPE.
    private static int CountItemsInInventory(IInventory inventory, int iItemID, int iItemDamage) {
        int itemCount = 0;

        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack tempStack = inventory.getStackInSlot(i);

            if (tempStack != null) {
                if (tempStack.itemID == iItemID) {
                    if (iItemDamage == m_iIgnoreMetadata || tempStack.getItemDamage() == iItemDamage) {
                        itemCount += tempStack.stackSize;
                    }
                }
            }
        }

        return itemCount;
    }

    // 1.5.2 FCUtilsInventory.ClearInventoryContents (Common FCUtilsInventory.java:27) —
    // clears via setInventorySlotContents so the InventoryBridge write-through persists
    // the wipe to the real MC inventory.
    private static void ClearInventoryContents(IInventory inventory) {
        for (int iSlot = 0; iSlot < inventory.getSizeInventory(); iSlot++) {
            ItemStack itemstack = inventory.getStackInSlot(iSlot);

            if (itemstack != null) {
                inventory.setInventorySlotContents(iSlot, null);
            }
        }
    }

    /**
     * Returns true if the player is carrying blasting oil in their inventory.
     */
    // 1.5.2 EntityPlayer.IsCarryingBlastingOil (vanilla/server EntityPlayer.java:2617) —
    // called from PlayerMixin.btw$checkBlastingOilOnDamage on every server-side player hit.
    public boolean IsCarryingBlastingOil() {
        return inventory.hasItem(GetBlastingOilItemID());
    }

    /**
     * Detonates carried blasting oil, killing the player and causing an explosion.
     */
    // 1.5.2 EntityPlayer.DetonateCarriedBlastingOil (vanilla/server EntityPlayer.java:2622) —
    // called from PlayerMixin.btw$checkBlastingOilOnDamage right after IsCarryingBlastingOil().
    // onDeath is bridged by PlayerBridge to the real ServerPlayer death path.
    public void DetonateCarriedBlastingOil() {
        if (!worldObj.isRemote) {
            int iHellfireCount = CountItemsInInventory(inventory, GetHellfireDustItemID(), -1);

            float fExplosionSize = (iHellfireCount * 10.0F) / 64.0F;

            fExplosionSize += (CountItemsInInventory(inventory, Item.gunpowder.itemID, -1) * 10.0F) / 64.0F;

            fExplosionSize += (CountItemsInInventory(inventory, GetBlastingOilItemID(), -1) * 10.0F) / 64.0F;

            int iTNTCount = CountItemsInInventory(inventory, Block.tnt.blockID, -1);

            if (iTNTCount > 0) {
                if (fExplosionSize < 4.0F) {
                    fExplosionSize = 4.0F;
                }

                fExplosionSize += CountItemsInInventory(inventory, Block.tnt.blockID, -1);
            }

            if (fExplosionSize < 1.5F) {
                fExplosionSize = 1.5F;
            } else if (fExplosionSize > 10.0F) {
                fExplosionSize = 10.0F;
            }

            ClearInventoryContents(inventory);

            health = 0;

            onDeath(DamageSource.generic);

            worldObj.createExplosion(null, posX, posY, posZ, fExplosionSize, true);
        }
    }

    /**
     * Called when the player tries to consume an item but cannot.
     * TODO: Needs world access for playAuxSFX and FCBetterThanWolves.m_iEatFailAuxFXID
     */
    public void OnCantConsume() {
        if (this.worldObj != null && m_iTicksSinceEmoteSound >= m_iTicksBetweenEmoteSounds) {
            this.worldObj.playAuxSFX(2285,
                MathHelper.floor_double(posX),
                MathHelper.floor_double(posY),
                MathHelper.floor_double(posZ), 0);
            m_iTicksSinceEmoteSound = 0;
        }
    }

    /**
     * Returns true if the player can drink (not affected by hunger potion).
     */
    public boolean CanDrink() {
        return !isPotionActive(Potion.hunger);
    }

    /**
     * Called when the player blocks damage with an item.
     * FC: Damages the currently held item by 1 durability point.
     */
    public void OnBlockedDamage(DamageSource source, int iDamage) {
        if (this.inventory != null) {
            ItemStack currentItemStack = this.inventory.mainInventory[this.inventory.currentItem];
            if (currentItemStack != null) {
                currentItemStack.damageItem(1, this);
            }
        }
    }

    /**
     * Reads FC-specific mod data from an NBT tag compound.
     */
    public void ReadModDataFromNBT(NBTTagCompound tag) {
        if (tag.hasKey("fcTimeOfLastSpawnAssignment")) {
            m_lTimeOfLastSpawnAssignment = tag.getLong("fcTimeOfLastSpawnAssignment");
        }

        if (tag.hasKey("fcTimeOfLastDimensionSwitch")) {
            m_lTimeOfLastDimensionSwitch = tag.getLong("fcTimeOfLastDimensionSwitch");
        }

        if (tag.hasKey("fcHCSpawnX") && tag.hasKey("fcHCSpawnY") && tag.hasKey("fcHCSpawnZ")) {
            m_HardcoreSpawnChunk = new ChunkCoordinates(
                tag.getInteger("fcHCSpawnX"),
                tag.getInteger("fcHCSpawnY"),
                tag.getInteger("fcHCSpawnZ")
            );
        }

        if (tag.hasKey("fcSpawnDimension")) {
            m_iSpawnDimension = tag.getInteger("fcSpawnDimension");
        }

        if (tag.hasKey("fcGloomLevel")) {
            SetGloomLevel(tag.getInteger("fcGloomLevel"));
        }

        if (tag.hasKey("fcGloomCounter")) {
            m_iInGloomCounter = tag.getInteger("fcGloomCounter");
        }
    }

    /**
     * Writes FC-specific mod data to an NBT tag compound.
     */
    public void WriteModDataToNBT(NBTTagCompound tag) {
        tag.setLong("fcTimeOfLastSpawnAssignment", m_lTimeOfLastSpawnAssignment);
        tag.setLong("fcTimeOfLastDimensionSwitch", m_lTimeOfLastDimensionSwitch);

        if (m_HardcoreSpawnChunk != null) {
            tag.setInteger("fcHCSpawnX", m_HardcoreSpawnChunk.posX);
            tag.setInteger("fcHCSpawnY", m_HardcoreSpawnChunk.posY);
            tag.setInteger("fcHCSpawnZ", m_HardcoreSpawnChunk.posZ);
        }

        tag.setInteger("fcSpawnDimension", m_iSpawnDimension);
        tag.setInteger("fcGloomLevel", GetGloomLevel());
        tag.setInteger("fcGloomCounter", m_iInGloomCounter);
    }
}
