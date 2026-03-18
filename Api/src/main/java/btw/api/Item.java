package btw.api;

import java.util.Random;

/**
 * Abstract representation of an item type.
 * Mirrors net.minecraft.src.Item with identical field/method names.
 */
public class Item {

    // --- Static item registry ---
    public static Random itemRand = new Random();
    public static Item[] itemsList = new Item[32000];

    // --- BTW filterable property constants ---
    public static final int m_iFilterable_NoProperties = 0;
    public static final int m_iFilterable_SolidBlock = 1;
    public static final int m_iFilterable_Small = 2;
    public static final int m_iFilterable_Narrow = 4;
    public static final int m_iFilterable_Fine = 8;
    public static final int m_iFilterable_Thin = 16;

    // --- Static item instances (populated by backend) ---
    public static Item shovelIron;
    public static Item pickaxeIron;
    public static Item axeIron;
    public static Item flintAndSteel;
    public static Item appleRed;
    public static Item arrow;
    public static Item coal;
    public static Item diamond;
    public static Item ingotIron;
    public static Item ingotGold;
    public static Item swordIron;
    public static Item swordWood;
    public static Item shovelWood;
    public static Item pickaxeWood;
    public static Item axeWood;
    public static Item swordStone;
    public static Item shovelStone;
    public static Item pickaxeStone;
    public static Item axeStone;
    public static Item swordDiamond;
    public static Item shovelDiamond;
    public static Item pickaxeDiamond;
    public static Item axeDiamond;
    public static Item stick;
    public static Item bowlEmpty;
    public static Item bowlSoup;
    public static Item swordGold;
    public static Item shovelGold;
    public static Item pickaxeGold;
    public static Item axeGold;
    public static Item silk;
    public static Item feather;
    public static Item gunpowder;
    public static Item hoeWood;
    public static Item hoeStone;
    public static Item hoeIron;
    public static Item hoeDiamond;
    public static Item hoeGold;
    public static Item seeds;
    public static Item wheat;
    public static Item bread;
    public static Item flint;
    public static Item bone;
    public static ItemShears shears;
    public static Item leather;
    public static Item dyePowder;
    public static Item porkRaw;
    public static Item porkCooked;
    public static Item beefRaw;
    public static Item beefCooked;
    public static Item chickenRaw;
    public static Item chickenCooked;
    public static Item rottenFlesh;
    public static Item fishRaw;
    public static Item fishCooked;
    public static Item egg;
    public static Item goldenCarrot;
    public static Item fermentedSpiderEye;
    public static Item blazePowder;
    public static Item blazeRod;
    public static Item ghastTear;
    public static Item sugar;
    public static Item magmaCream;
    public static Item enderPearl;
    public static Item netherStar;
    public static Item bucketEmpty;
    public static Item bucketWater;
    public static Item bucketLava;
    public static Item bucketMilk;
    public static Item saddle;
    public static Item snowball;
    public static Item boat;
    public static Item minecartEmpty;
    public static Item minecartPowered;
    public static Item minecartCrate;
    public static Item minecartTnt;
    public static Item minecartHopper;
    public static Item redstone;
    public static Item clay;
    public static Item paper;
    public static Item book;
    public static Item slimeBall;
    public static Item eyeOfEnder;
    public static ItemPotion potion;
    public static Item writableBook;
    public static Item writtenBook;
    public static Item emerald;
    public static ItemMap map;
    public static ItemEmptyMap emptyMap;
    public static Item compass;
    public static Item pocketSundial;
    public static Item glassBottle;
    public static ItemBow bow;
    public static ItemFishingRod fishingRod;
    public static Item carrotOnAStick;
    public static Item bed;
    public static Item sign;
    public static Item doorWood;
    public static Item doorIron;
    public static Item cake;
    public static Item brewingStand;
    public static Item cauldron;
    public static Item skull;
    public static Item netherQuartz;
    public static Item comparator;
    public static Item fireballCharge;
    public static Item painting;
    public static Item itemFrame;
    public static Item brick;
    public static Item reed;
    public static Item lightStoneDust;
    public static Item goldNugget;
    public static Item netherStalkSeeds;
    public static Item spiderEye;
    public static Item melon;
    public static Item melonSeeds;
    public static Item pumpkinSeeds;
    public static Item pumpkinPie;
    public static Item cookie;
    public static Item potato;
    public static Item bakedPotato;
    public static Item carrot;
    public static ItemEnchantedBook enchantedBook;
    public static Item redstoneRepeater;
    public static Item flowerPot;
    public static ItemArmor helmetLeather;
    public static ItemArmor helmetChain;
    public static ItemArmor helmetIron;
    public static ItemArmor helmetGold;
    public static ItemArmor helmetDiamond;
    public static ItemArmor plateLeather;
    public static ItemArmor plateChain;
    public static ItemArmor plateIron;
    public static ItemArmor plateGold;
    public static ItemArmor plateDiamond;
    public static ItemArmor legsLeather;
    public static ItemArmor legsChain;
    public static ItemArmor legsIron;
    public static ItemArmor legsGold;
    public static ItemArmor legsDiamond;
    public static ItemArmor bootsLeather;
    public static ItemArmor bootsChain;
    public static ItemArmor bootsIron;
    public static ItemArmor bootsGold;
    public static ItemArmor bootsDiamond;

    // --- Instance fields ---
    public final int itemID;
    public int maxStackSize = 64;
    public boolean bFull3D = false;
    public boolean hasSubtypes = false;
    public int m_iDefaultFurnaceBurnTime = 0;
    public boolean m_bIsInceratedInCrucible = false;
    public int m_iFilterablePropertiesBitfield = 0;

    // --- Client-side rendering fields ---
    public Icon itemIcon;

    protected int maxDamage;

    public Item(int id) {
        this.itemID = 256 + id;
    }

    // --- Builder/setter methods ---

    public Item setMaxStackSize(int maxStackSize) {
        this.maxStackSize = maxStackSize;
        return this;
    }

    public Item setHasSubtypes(boolean hasSubtypes) {
        this.hasSubtypes = hasSubtypes;
        return this;
    }

    public Item setMaxDamage(int maxDamage) {
        this.maxDamage = maxDamage;
        return this;
    }

    public Item setFull3D() {
        this.bFull3D = true;
        return this;
    }

    public Item setUnlocalizedName(String name) {
        return this;
    }

    public Item setCreativeTab(CreativeTabs tab) {
        return this;
    }

    public Item setPotionEffect(String potionEffect) {
        return this;
    }

    // --- Query methods ---

    public int getItemStackLimit() {
        return this.maxStackSize;
    }

    public int getMetadata(int damage) {
        return 0;
    }

    public boolean getHasSubtypes() {
        return this.hasSubtypes;
    }

    public int getMaxDamage() {
        return this.maxDamage;
    }

    public boolean isDamageable() {
        return this.maxDamage > 0 && !this.hasSubtypes;
    }

    public String getPotionEffect() {
        return null;
    }

    public boolean isPotionIngredient() {
        return false;
    }

    public String getItemDisplayName(ItemStack stack) {
        return "";
    }

    public boolean isItemTool(ItemStack stack) {
        return false;
    }

    public int getItemEnchantability() {
        return 0;
    }

    public boolean func_82788_x() {
        return false;
    }

    public boolean getIsRepairable(ItemStack toRepair, ItemStack repairWith) {
        return false;
    }

    public String getUnlocalizedName() {
        return "";
    }

    // --- Item use methods ---

    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        return false;
    }

    public float getStrVsBlock(ItemStack stack, Block block) {
        return 1.0F;
    }

    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        return stack;
    }

    public ItemStack onEaten(ItemStack stack, World world, EntityPlayer player) {
        return stack;
    }

    public boolean hitEntity(ItemStack stack, EntityLiving target, EntityLiving attacker) {
        return false;
    }

    public boolean onBlockDestroyed(ItemStack stack, World world, int blockID, int x, int y, int z, EntityLiving livingEntity) {
        return false;
    }

    public int getDamageVsEntity(Entity entity) {
        return 1;
    }

    public boolean canHarvestBlock(Block block) {
        return false;
    }

    public boolean useItemOnEntity(ItemStack stack, EntityLiving livingEntity) {
        return false;
    }

    public void onUpdate(ItemStack stack, World world, EntityPlayer player, int inventorySlot, boolean isHandHeld) {}

    public void onCreated(ItemStack stack, World world, EntityPlayer player) {}

    public void onPlayerStoppedUsing(ItemStack stack, World world, EntityPlayer player, int ticksRemaining) {}

    public MovingObjectPosition getMovingObjectPositionFromPlayer(World world, EntityPlayer player, boolean hitFluids) {
        return null;
    }

    // --- Client-side rendering methods ---

    public Icon getIconFromDamage(int damage) {
        return this.itemIcon;
    }

    public void registerIcons(IconRegister register) {
    }

    public int getColorFromItemStack(ItemStack stack, int renderPass) {
        return 0xFFFFFF;
    }

    public Icon GetHopperFilterIcon() {
        return this.itemIcon;
    }

    public int getItemSpriteNumber() {
        return 1;
    }

    public Icon func_94597_g() {
        return null;
    }

    public String getUnlocalizedName2() {
        return "";
    }

    // --- BTW-added methods ---

    public boolean CanItemBeUsedByPlayer(World world, int i, int j, int k, int iFacing, EntityPlayer player, ItemStack stack) {
        return true;
    }

    public boolean DoZombiesConsume() {
        return false;
    }

    public boolean IsEfficientVsBlock(ItemStack stack, World world, Block block, int i, int j, int k) {
        return false;
    }

    public boolean canHarvestBlock(ItemStack stack, World world, Block block, int i, int j, int k) {
        return false;
    }

    public float getStrVsBlock(ItemStack stack, World world, Block block, int i, int j, int k) {
        return 1.0F;
    }

    public boolean IsMultiUsePerClick() {
        return false;
    }

    public float GetExhaustionOnUsedToHarvestBlock(int iBlockID, World world, int i, int j, int k, int iBlockMetadata) {
        return 0;
    }

    public void InitializeStackOnGiveCommand(Random rand, ItemStack stack) {}

    public void UpdateUsingItem(ItemStack stack, World world, EntityPlayer player) {}

    public int GetItemUseWarmupDuration() {
        return 0;
    }

    public boolean IgnoreDamageWhenComparingDuringUse() {
        return false;
    }

    // --- BTW-added: Food values ---

    public int GetHerbivoreFoodValue(int iItemDamage) { return 0; }
    public Item SetHerbivoreFoodValue(int iFoodValue) { return this; }
    public Item SetAsBasicHerbivoreFood() { return this; }
    public int GetChickenFoodValue(int iItemDamage) { return 0; }
    public Item SetChickenFoodValue(int iFoodValue) { return this; }
    public Item SetAsBasicChickenFood() { return this; }
    public int GetPigFoodValue(int iItemDamage) { return 0; }
    public Item SetPigFoodValue(int iFoodValue) { return this; }
    public Item SetAsBasicPigFood() { return this; }
    public boolean IsWolfFood() { return false; }
    public int GetWolfHealAmount() { return 0; }

    // --- BTW-added: Buoyancy ---

    public Item SetBuoyancy(float fBuoyancy) { return this; }
    public Item SetBuoyant() { return SetBuoyancy(1F); }
    public Item SetNonBuoyant() { return SetBuoyancy(-1F); }
    public Item SetNeutralBuoyant() { return SetBuoyancy(0F); }
    public float GetBuoyancy(int iItemDamage) { return 0; }

    // --- BTW-added: Weight ---

    public int GetWeightWhenWorn() { return 0; }

    // --- BTW-added: Bellows ---

    public Item SetBellowsBlowDistance(int iDistance) { return this; }
    public int GetBellowsBlowDistance(int iItemDamage) { return 0; }

    // --- BTW-added: Enchanting ---

    public Item SetInfernalMaxNumEnchants(int iMaxNumEnchants) { return this; }
    public int GetInfernalMaxNumEnchants() { return 0; }
    public Item SetInfernalMaxEnchantmentCost(int iMaxEnchantmentCost) { return this; }
    public int GetInfernalMaxEnchantmentCost() { return 0; }
    // --- BTW-added: Crafting ---

    public boolean IsConsumedInCrafting() { return true; }
    public boolean IsDamagedInCrafting() { return false; }
    public void OnUsedInCrafting(int iItemDamage, EntityPlayer player, ItemStack outputStack) {}
    public void OnUsedInCrafting(EntityPlayer player, ItemStack outputStack) {}
    public void OnDamagedInCrafting(EntityPlayer player) {}
    public void OnBrokenInCrafting(EntityPlayer player) {}

    // --- BTW-added: Furnace ---

    public int GetFurnaceBurnTime(int iItemDamage) { return m_iDefaultFurnaceBurnTime; }
    public Item SetFurnaceBurnTime(int iBurnTime) { this.m_iDefaultFurnaceBurnTime = iBurnTime; return this; }
    public int GetCampfireBurnTime(int iItemDamage) { return 0; }

    // --- BTW-added: Fire ---

    public boolean GetCanItemStartFireOnUse(int iItemDamage) { return false; }
    public boolean GetCanItemBeSetOnFireOnUse(int iItemDamage) { return false; }
    public boolean GetCanBeFedDirectlyIntoCampfire(int iItemDamage) { return false; }
    public boolean GetCanBeFedDirectlyIntoBrickOven(int iItemDamage) { return false; }

    // --- BTW-added: Crucible ---

    public boolean IsIncineratedInCrucible() { return m_bIsInceratedInCrucible; }
    public Item SetIncineratedInCrucible() { m_bIsInceratedInCrucible = true; return this; }
    public Item SetNotIncineratedInCrucible() { m_bIsInceratedInCrucible = false; return this; }

    // --- BTW-added: Container items ---

    public boolean DoesConsumeContainerItemWhenCrafted(Item containerItem) { return false; }

    // --- BTW-added: Piston packing ---

    public boolean IsPistonPackable(ItemStack stack) { return false; }
    public int GetRequiredItemCountToPistonPack(ItemStack stack) { return 0; }
    public int GetResultingBlockIDOnPistonPack(ItemStack stack) { return 0; }
    public int GetResultingBlockMetadataOnPistonPack(ItemStack stack) { return 0; }

    // --- BTW-added: Filtering ---

    public boolean CanItemPassIfFilter(ItemStack filteredItem) { return true; }
    public int GetFilterableProperties(ItemStack stack) { return m_iFilterablePropertiesBitfield; }
    public Item SetFilterableProperties(int iProperties) { m_iFilterablePropertiesBitfield = iProperties; return this; }
    public boolean CanTransformItemIfFilter(ItemStack filteredItem) { return false; }

    // --- BTW-added: Static tool-effectiveness methods ---

    public static void SetAllPicksToBeEffectiveVsBlock(Block block) {}
    public static void SetAllAxesToBeEffectiveVsBlock(Block block) {}
    public static void SetAllShovelsToBeEffectiveVsBlock(Block block) {}

    // --- BTW-added: Block dispenser ---

    public boolean OnItemUsedByBlockDispenser(ItemStack stack, World world, int i, int j, int k, int iFacing) {
        return false;
    }

    public static boolean m_bSuppressConflictWarnings = false;

    public Item SetFurnaceBurnTime(FCEnumFurnaceBurnTime burnTimeEnum) {
        return this;
    }

    // --- BTW-added: Damage setter ---

    public Item SetDamageVsEntity(int damage) {
        return this;
    }

    // --- BTW-added: Block-related item methods ---

    public int getBlockID() { return 0; }
    public int GetBlockIDToPlace(int iItemDamage, int iFacing, float fClickX, float fClickY, float fClickZ) { return 0; }
    public boolean GetCanBePlacedAsBlock() { return false; }
    public boolean convertToFullBlock(World world, int i, int j, int k) { return false; }
    public boolean canCombineWithBlock(World world, int i, int j, int k, int iItemDamage) { return false; }
    public boolean CanToolStickInBlock(ItemStack stack, Block block, World world, int i, int j, int k) { return false; }
    public float GetBlockBoundingBoxHeight() { return 1.0F; }
    public float GetVisualVerticalOffsetAsBlock() { return 0.0F; }
    public boolean IsToolTypeEfficientVsBlockType(Block block) { return false; }

    // --- BTW-added: Placement/use sounds/effects ---

    public void PlayPlacementSound(ItemStack stack, Block blockStuckIn, World world, int i, int j, int k) {}
    public void PlayCraftingFX(ItemStack stack, World world, EntityPlayer player) {}
    public void PerformUseEffects(EntityPlayer player) {}
    public void PlayerBowSound(World world, EntityPlayer player, float fPullStrength) {}
    public void SpawnThrownEntity(ItemStack stack, World world, EntityPlayer player) {}

    // --- BTW-added: Food ---

    public int GetHungerRestored() { return 0; }

    // --- BTW-added: Texture ---

    public String GetWornTexturePrefix() { return ""; }

    // --- BTW-added: Enchantment overload ---

    public boolean IsEnchantmentApplicable(Enchantment enchantment) { return false; }

    // --- BTW-added: Misc ---

    public void func_82813_b(ItemStack stack, int par2) {}
    public void removeColor(ItemStack stack) {}
    public void updateMapData(World world, Entity entity, MapData mapData) {}
    public int getMaxItemUseDuration(ItemStack stack) { return 0; }
    public EnumAction getItemUseAction(ItemStack stack) { return null; }
    public String getUnlocalizedName(ItemStack stack) { return ""; }
    public String getLocalizedName(ItemStack stack) { return ""; }
    public Item setContainerItem(Item item) { return this; }

    // --- Client-side rendering methods ---

    public void getSubItems(int itemID, CreativeTabs creativeTabs, java.util.List list) {
    }

    public void addInformation(ItemStack itemStack, EntityPlayer player, java.util.List infoList, boolean advancedToolTips) {
    }

    public boolean isFull3D() {
        return false;
    }

    public boolean hasEffect(ItemStack itemStack) {
        return false;
    }

    public boolean itemInteractionForEntity(ItemStack itemStack, EntityLiving targetEntity) {
        return false;
    }

    public boolean requiresMultipleRenderPasses() {
        return false;
    }

    public Icon getIconFromDamageForRenderPass(int damage, int renderPass) {
        return null;
    }
}
