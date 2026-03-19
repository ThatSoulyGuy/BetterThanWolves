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

    public boolean isUsingItem() {
        return false;
    }

    public ItemStack getCurrentEquippedItem() {
        if (this.inventory != null) {
            return this.inventory.getCurrentItem();
        }
        return null;
    }

    public void playSound(String name, float volume, float pitch) {}

    public void addChatMessage(String message) {}

    public boolean canEat(boolean alwaysEdible) {
        return false;
    }

    public void displayGUIChest(IInventory inventory) {}

    public void addExhaustion(float exhaustion) {}

    public void setItemInUse(ItemStack stack, int duration) {}

    public FoodStats getFoodStats() {
        return foodStats;
    }

    public boolean isPlayerSleeping() {
        return false;
    }

    public void triggerAchievement(StatBase stat) {}

    public void addStat(StatBase stat, int amount) {}

    public void destroyCurrentEquippedItem() {
        if (this.inventory != null) {
            this.inventory.setInventorySlotContents(this.inventory.currentItem, null);
        }
    }

    public boolean isInCreativeMode() {
        return false;
    }

    public void addExperience(int amount) {}

    public float getCurrentPlayerStrVsBlock(Block block, boolean flag) {
        // Delegate to position-aware version with dummy coords
        return getCurrentPlayerStrVsBlock(block, 0, 0, 0);
    }

    public float getCurrentPlayerStrVsBlock(Block block, int i, int j, int k) {
        // FC implementation from patched EntityPlayer.java
        float str = 1.0F;

        // Get tool speed from inventory
        if (this.inventory != null) {
            str = this.inventory.getStrVsBlock(block);
        }

        // Apply efficiency enchantment if tool speed > 1 and has current item
        if (str > 1.0F) {
            ItemStack currentItem = getCurrentEquippedItem();
            if (currentItem != null) {
                int efficiency = EnchantmentHelper.getEfficiencyModifier(this);
                if (efficiency > 0) {
                    float bonus = (float)(efficiency * efficiency + 1);
                    str += bonus;
                }
            }
        }

        // Apply FC mining speed modifier (health/hunger/gloom penalties)
        str *= GetMiningSpeedModifier();

        // Apply dig slowdown potion effect
        if (isPotionActive(Potion.digSlowdown)) {
            PotionEffect effect = getActivePotionEffect(Potion.digSlowdown);
            if (effect != null) {
                float slowdown = 1.0F;
                switch (effect.getAmplifier()) {
                    case 0: slowdown = 0.3F; break;
                    case 1: slowdown = 0.09F; break;
                    case 2: slowdown = 0.0027F; break;
                    default: slowdown = 8.1E-4F; break;
                }
                str *= slowdown;
            }
        }

        // Penalty for mining underwater without aqua affinity
        if (isInsideOfMaterial(Material.water)) {
            str /= 5.0F;
        }

        // Penalty for not on ground
        if (!onGround) {
            str /= 5.0F;
        }

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
    public String getCommandSenderName() { return ""; }
    public void addToPlayerScore(Entity entity, int amount) {}
    public EnumStatus sleepInBedAt(int x, int y, int z) { return null; }
    public void wakeUpPlayer(boolean immediately, boolean updateWorldFlag, boolean setSpawn) {}

    public boolean canPlayerEdit(int x, int y, int z, int side, ItemStack stack) { return true; }
    public EntityItem dropPlayerItem(ItemStack stack) { return null; }
    public InventoryEnderChest getInventoryEnderChest() { return theInventoryEnderChest; }
    public int getItemInUseCount() { return 0; }
    public void SetItemInUseCount(int count) {}
    public ItemStack getCurrentArmor(int slot) { return null; }
    public void addExperienceLevel(int levels) {}
    public void setLevelsServerSafe(int levels) {}
    public int getFoodLevel() { return 20; }
    public boolean isBlocking() { return false; }
    public void heal(int amount) {}
    public void displayGUIEditSign(TileEntity sign) {}
    public void addStats(int stat, float amount) {}
    public int IncrementAndGetWindowID() { return 0; }
    public void HandleStartBlockHarvest(Object packet) {}
    public boolean IsLocalPlayerAndHittingBlock() { return false; }
    public void OnNearbyFireStartAttempt(EntityPlayer player) {}
    public int GetHungerLevel() { return 0; }
    public void AttemptToPossessNearbyCreatureOnDeath() {}
    public void AttemptToPossessNearbyCreature(double range, boolean persistentSpirit) {}
    public void AttemptToPossessCreaturesAroundBlock(World world, int x, int y, int z, int range, int dim) {}
    public void AddRawChatMessage(String message) {}
    public boolean AddStackToCurrentHeldStackIfEmpty(ItemStack stack) { return false; }
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
     * Returns a modifier based on the maximum status penalty level.
     * penalty 0 -> 1.0, 1 -> 1.0, 2 -> 0.75, 3 -> 0.5, 4+ -> 0.25
     */
    public float GetHealthAndExhaustionModifier() {
        float fModifier = 1.0F;

        int iPenaltyLevel = GetMaximumStatusPenaltyLevel();

        if (iPenaltyLevel >= 2) {
            if (iPenaltyLevel >= 3) {
                if (iPenaltyLevel >= 4) {
                    fModifier = 0.25F;
                } else {
                    fModifier = 0.5F;
                }
            } else {
                fModifier = 0.75F;
            }
        }

        return fModifier;
    }

    /**
     * Returns GetHealthAndExhaustionModifier() halved if gloom level > 0.
     */
    public float GetHealthAndExhaustionModifierWithSightlessModifier() {
        float fModifier = GetHealthAndExhaustionModifier();

        if (GetGloomLevel() > 0) {
            fModifier *= 0.5F;
        }

        return fModifier;
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
        return false;
    }

    /**
     * Checks if the helm slot contains the soulforged plate helm.
     * TODO: Needs inventory bridge to access armorInventory[3]
     */
    public boolean IsWearingSoulforgedHelm() {
        return false;
    }

    /**
     * Checks if the boots slot contains the soulforged plate boots.
     * TODO: Needs inventory bridge to access armorInventory[0]
     */
    public boolean IsWearingSoulforgedBoots() {
        return false;
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
     * TODO: Needs world light level access; real implementation is in EntityPlayerMP override
     */
    public void UpdateGloomState() {}

    /**
     * Updates hunger penalty level based on food level thresholds.
     * Thresholds: >24 -> 0, >18 -> 1, >12 -> 2, >6 -> 3, >0 or sat>0 -> 4, else -> 5
     */
    public void UpdateHungerPenaltyLevel() {
        int iHunger = foodStats.getFoodLevel();
        int iPenaltyLevel = 5;

        if (iHunger > 24) {
            iPenaltyLevel = 0;
        } else if (iHunger > 18) {
            iPenaltyLevel = 1;
        } else if (iHunger > 12) {
            iPenaltyLevel = 2;
        } else if (iHunger > 6) {
            iPenaltyLevel = 3;
        } else if (iHunger > 0 || foodStats.getSaturationLevel() > 0F) {
            iPenaltyLevel = 4;
        }

        SetHungerPenaltyLevel(iPenaltyLevel);
    }

    /**
     * Updates fat penalty level based on saturation thresholds.
     * Thresholds: <12 -> 0, <14 -> 1, <16 -> 2, <18 -> 3, >=18 -> 4
     */
    public void UpdateFatPenaltyLevel() {
        int iFat = (int) foodStats.getSaturationLevel();
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

        SetFatPenaltyLevel(iFatLevel);
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
     * Uses isWeighted() which checks GetWornArmorWeight() >= 10.
     */
    public boolean CanSwim() {
        return !isWeighted() && health > 4;
    }

    /**
     * Returns true if worn armor weight is >= 10 (too heavy to swim).
     * Ported from EntityLiving in the FC-patched source.
     */
    public boolean isWeighted() {
        int iWeight = GetWornArmorWeight();

        if (iWeight >= 10) {
            return true;
        }

        return false;
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
     * Adds exhaustion for harvesting a block, based on the tool used.
     * TODO: Needs inventory bridge to access current item and Item.GetExhaustionOnUsedToHarvestBlock()
     */
    public void AddHarvestBlockExhaustion(int iBlockID, int iBlockI, int iBlockJ, int iBlockK, int iBlockMetadata) {
        // stub: real implementation requires access to inventory.mainInventory[inventory.currentItem]
        // and currentItemStack.getItem().GetExhaustionOnUsedToHarvestBlock()
    }

    // =========================================================================
    // FC Misc
    // =========================================================================

    /**
     * Returns true if the player is carrying blasting oil in their inventory.
     * TODO: Needs inventory scanning via inventory.hasItem()
     */
    public boolean IsCarryingBlastingOil() {
        return false;
    }

    /**
     * Detonates carried blasting oil, causing an explosion.
     * TODO: Needs inventory scanning (FCUtilsInventory.CountItemsInInventory) and world access
     */
    public void DetonateCarriedBlastingOil() {
        // stub: real implementation requires FCUtilsInventory, FCBetterThanWolves item references, and world access
    }

    /**
     * Called when the player tries to consume an item but cannot.
     * TODO: Needs world access for playAuxSFX and FCBetterThanWolves.m_iEatFailAuxFXID
     */
    public void OnCantConsume() {
        // stub: real implementation plays eat-fail sound effect via worldObj.playAuxSFX
    }

    /**
     * Returns true if the player can drink (not affected by hunger potion).
     */
    public boolean CanDrink() {
        return !isPotionActive(Potion.hunger);
    }

    /**
     * Called when the player blocks damage with an item.
     * TODO: Needs inventory bridge to damage current held item
     */
    public void OnBlockedDamage(DamageSource source, int iDamage) {
        // stub: real implementation damages the current held item stack
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
