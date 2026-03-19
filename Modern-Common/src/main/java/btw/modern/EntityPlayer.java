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
    public int m_iTimesCraftedThisTick;
    public long m_lTimeOfLastDimensionSwitch;
    public long m_lRespawnAssignmentCooldownTimer;
    public ChunkCoordinates m_HardcoreSpawnChunk;
    public long m_lTimeOfLastSpawnAssignment;

    protected EntityPlayer(World world) {
        super(world);
    }

    public boolean isUsingItem() {
        return false;
    }

    public ItemStack getCurrentEquippedItem() {
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

    public void destroyCurrentEquippedItem() {}

    public boolean isInCreativeMode() {
        return false;
    }

    public void addExperience(int amount) {}

    public float getCurrentPlayerStrVsBlock(Block block, boolean flag) {
        return 0.0F;
    }

    public float getCurrentPlayerStrVsBlock(Block block, int i, int j, int k) {
        return 0.0F;
    }

    public boolean IsCurrentToolEffectiveOnBlock(Block block, int i, int j, int k) {
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
    public boolean IsWearingFullSuitSoulforgedArmor() { return false; }
    public boolean IsWearingSoulforgedBoots() { return false; }
    public int IncrementAndGetWindowID() { return 0; }
    public void AddRawChatMessage(String message) {}
    public boolean AddStackToCurrentHeldStackIfEmpty(ItemStack stack) { return false; }
    public static boolean InstallationIntegrityTestPlayer() { return true; }
    public float GetBowPullStrengthModifier() { return 1.0F; }
    public void HandleStartBlockHarvest(Object packet) {}
    public boolean IsLocalPlayerAndHittingBlock() { return false; }
    public void OnNearbyFireStartAttempt(EntityPlayer player) {}
    public int GetHungerLevel() { return 0; }
    public void AttemptToPossessNearbyCreatureOnDeath() {}
    public void AttemptToPossessNearbyCreature(double range, boolean persistentSpirit) {}
    public void AttemptToPossessCreaturesAroundBlock(World world, int x, int y, int z, int range, int dim) {}
    public void OnCantConsume() {}
    public boolean CanDrink() { return false; }

    // --- Client-side methods ---

    public int GetFatPenaltyLevel() { return 0; }
}
