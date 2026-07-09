package btw.modern;

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
    /** Vanilla 1.5.2 Item.expBottle — referenced by EntityVillager static init. */
    public static Item expBottle;
    /** Vanilla 1.5.2 Item.monsterPlacer — spawn egg, referenced by EntityAgeable.interact. */
    public static Item monsterPlacer;
    public static Item redstoneRepeater;
    public static Item flowerPot;
    // Music-disc endpoints — frozen EntityCreeper.onDeath (live fc_creeper killed by a
    // live fc_skeleton) drops a random disc id in [record13.itemID, recordWait.itemID].
    public static Item record13;
    public static Item recordWait;
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
    protected CreativeTabs creativeTab;

    // --- BTW backing fields ---
    private float m_fBuoyancy = -1.0F;
    private int m_iBellowsBlowDistance = 0;
    private int m_iInfernalMaxNumEnchants = 0;
    private int m_iInfernalMaxEnchantmentCost = 0;
    private int m_iHerbivoreFoodValue = 0;
    private int m_iBirdFoodValue = 0;
    private int m_iPigFoodValue = 0;

    // --- BTW food value constants ---
    public static final int m_iBaseHerbivoreItemFoodValue = ( EntityAnimal.m_iBaseGrazeFoodValue * 4 );
    public static final int m_iBasePigItemFoodValue = ( EntityAnimal.m_iBaseGrazeFoodValue * 4 );
    public static final int m_iBaseChickenItemFoodValue = ( EntityAnimal.m_iBaseGrazeFoodValue * 8 );

    public Item(int id) {
        this.itemID = 256 + id;
        // Self-register in the global itemsList
        if (this.itemID >= 0 && this.itemID < itemsList.length) {
            itemsList[this.itemID] = this;
        }
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

    private String unlocalizedName = "";

    public Item setUnlocalizedName(String name) {
        this.unlocalizedName = name;
        return this;
    }

    public Item setCreativeTab(CreativeTabs tab) {
        this.creativeTab = tab;
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
        String key = this.getUnlocalizedName(stack) + ".name";
        String translated = StatCollector.translateToLocal(key);
        // If translation exists (different from key), use it
        if (!translated.equals(key)) {
            return translated;
        }
        // Fallback: try base name without subtype
        key = this.getUnlocalizedName() + ".name";
        translated = StatCollector.translateToLocal(key);
        if (!translated.equals(key)) {
            return translated;
        }
        return this.getUnlocalizedName(stack);
    }

    public boolean isItemTool(ItemStack stack) {
        return getItemStackLimit() == 1 && isDamageable();
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
        return "item." + unlocalizedName;
    }

    // --- Item use methods ---

    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        return false;
    }

    public float getStrVsBlock(ItemStack stack, Block block) {
        return 1.0F;
    }

    public float getStrVsBlock(ItemStack stack, Block block, int metadata) {
        return getStrVsBlock(stack, block);
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
        float pitch = player.rotationPitch;
        float yaw = player.rotationYaw;
        double x = player.posX;
        double y = player.posY + 1.62D - (double) player.yOffset;
        double z = player.posZ;

        Vec3 startVec = Vec3.createVectorHelper(x, y, z);

        float cosYaw = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
        float sinYaw = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
        float cosPitch = -MathHelper.cos(-pitch * 0.017453292F);
        float sinPitch = MathHelper.sin(-pitch * 0.017453292F);

        float dx = sinYaw * cosPitch;
        float dy = sinPitch;
        float dz = cosYaw * cosPitch;

        double reach = 5.0D;
        Vec3 endVec = startVec.addVector((double) dx * reach, (double) dy * reach, (double) dz * reach);

        return world.rayTraceBlocks_do(startVec, endVec, hitFluids);
    }

    // --- Client-side rendering methods ---

    public Icon getIconFromDamage(int damage) {
        return this.itemIcon;
    }

    // 1.5.2 Item.getIconIndex(ItemStack) — ItemStack.getIconIndex delegates here;
    // FC items override for damage/state-dependent icons.
    public Icon getIconIndex(ItemStack stack) {
        return this.getIconFromDamage(stack.getItemDamage());
    }

    public void registerIcons(IconRegister register) {
        this.itemIcon = register.registerIcon(this.unlocalizedName);
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
        return this.itemIcon;
    }

    public String getUnlocalizedName2() {
        return unlocalizedName;
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

    // 1.5.2 Item.GetExhaustionOnUsedToHarvestBlock (vanilla/server/.../Item.java:875) —
    // EntityPlayer harvest exhaustion path; FCItemAxe overrides and super-calls this default.
    public float GetExhaustionOnUsedToHarvestBlock(int iBlockID, World world, int i, int j, int k, int iBlockMetadata) {
        return 0.025F; // standard default exhaustion amount
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

    public int GetHerbivoreFoodValue(int iItemDamage) { return m_iHerbivoreFoodValue; }
    public Item SetHerbivoreFoodValue(int iFoodValue) { this.m_iHerbivoreFoodValue = iFoodValue; return this; }
    public Item SetAsBasicHerbivoreFood() { return SetHerbivoreFoodValue(m_iBaseHerbivoreItemFoodValue); }
    public int GetChickenFoodValue(int iItemDamage) { return m_iBirdFoodValue; }
    public Item SetChickenFoodValue(int iFoodValue) { this.m_iBirdFoodValue = iFoodValue; return this; }
    public Item SetAsBasicChickenFood() { return SetChickenFoodValue(m_iBaseChickenItemFoodValue); }
    public int GetPigFoodValue(int iItemDamage) { return m_iPigFoodValue; }
    public Item SetPigFoodValue(int iFoodValue) { this.m_iPigFoodValue = iFoodValue; return this; }
    public Item SetAsBasicPigFood() { return SetPigFoodValue(m_iBasePigItemFoodValue); }
    public boolean IsWolfFood() { return false; }
    public int GetWolfHealAmount() { return 0; }

    // --- BTW-added: Buoyancy ---

    public Item SetBuoyancy(float fBuoyancy) { this.m_fBuoyancy = fBuoyancy; return this; }
    public Item SetBuoyant() { return SetBuoyancy(1F); }
    public Item SetNonBuoyant() { return SetBuoyancy(-1F); }
    public Item SetNeutralBuoyant() { return SetBuoyancy(0F); }
    public float GetBuoyancy(int iItemDamage) { return m_fBuoyancy; }

    // --- BTW-added: Weight ---

    public int GetWeightWhenWorn() { return 0; }

    // --- BTW-added: Bellows ---

    public Item SetBellowsBlowDistance(int iDistance) { this.m_iBellowsBlowDistance = iDistance; return this; }
    public int GetBellowsBlowDistance(int iItemDamage) { return m_iBellowsBlowDistance; }

    // --- BTW-added: Enchanting ---

    public Item SetInfernalMaxNumEnchants(int iMaxNumEnchants) { this.m_iInfernalMaxNumEnchants = iMaxNumEnchants; return this; }
    public int GetInfernalMaxNumEnchants() { return m_iInfernalMaxNumEnchants; }
    public Item SetInfernalMaxEnchantmentCost(int iMaxEnchantmentCost) { this.m_iInfernalMaxEnchantmentCost = iMaxEnchantmentCost; return this; }
    public int GetInfernalMaxEnchantmentCost() { return m_iInfernalMaxEnchantmentCost; }
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
    public int GetCampfireBurnTime(int iItemDamage) { return GetFurnaceBurnTime(iItemDamage); }

    // --- BTW-added: Fire ---

    public boolean GetCanItemStartFireOnUse(int iItemDamage) { return false; }
    public boolean GetCanItemBeSetOnFireOnUse(int iItemDamage) { return false; }
    public boolean GetCanBeFedDirectlyIntoCampfire(int iItemDamage) {
        return !GetCanItemBeSetOnFireOnUse(iItemDamage) && !GetCanItemStartFireOnUse(iItemDamage) &&
                GetCampfireBurnTime(iItemDamage) > 0;
    }
    public boolean GetCanBeFedDirectlyIntoBrickOven(int iItemDamage) {
        return !GetCanItemBeSetOnFireOnUse(iItemDamage) && !GetCanItemStartFireOnUse(iItemDamage) &&
                GetFurnaceBurnTime(iItemDamage) > 0;
    }

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
        return SetFurnaceBurnTime(burnTimeEnum.m_iBurnTime);
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
    public String getUnlocalizedName(ItemStack stack) { return getUnlocalizedName(); }
    public String getLocalizedName(ItemStack stack) {
        String key = this.getUnlocalizedName(stack) + ".name";
        String translated = StatCollector.translateToLocal(key);
        if (!translated.equals(key)) return translated;
        // Fallback: try base name
        key = this.getUnlocalizedName() + ".name";
        translated = StatCollector.translateToLocal(key);
        if (!translated.equals(key)) return translated;
        return "";
    }
    private Item containerItem;
    public Item setContainerItem(Item item) { this.containerItem = item; return this; }
    public Item getContainerItem() { return containerItem; }
    public boolean hasContainerItem() { return containerItem != null; }
    // 1.5.2 Item.doesContainerItemLeaveCraftingGrid (vanilla/server/.../Item.java:666) —
    // SlotCrafting.onPickupFromSlot container-item return path.
    public boolean doesContainerItemLeaveCraftingGrid(ItemStack stack) { return true; }

    // --- Client-side rendering methods ---

    @SuppressWarnings("unchecked")
    public void getSubItems(int itemID, CreativeTabs creativeTabs, java.util.List list) {
        list.add(new ItemStack(itemID, 1, 0));
    }

    public void addInformation(ItemStack itemStack, EntityPlayer player, java.util.List infoList, boolean advancedToolTips) {
    }

    public boolean isFull3D() {
        return this.bFull3D;
    }

    public boolean hasEffect(ItemStack itemStack) {
        return itemStack != null && itemStack.isItemEnchanted();
    }

    public boolean itemInteractionForEntity(ItemStack itemStack, EntityLiving targetEntity) {
        return false;
    }

    public boolean requiresMultipleRenderPasses() {
        return false;
    }

    public Icon getIconFromDamageForRenderPass(int damage, int renderPass) {
        return this.getIconFromDamage(damage);
    }

    /**
     * Populates vanilla item static fields. Called during Forge mod init
     * BEFORE FC code runs. FC code will overwrite many of these with FC subclasses.
     *
     * Raw ID = itemID - 256.  Constructor: new Item(rawId) sets itemID = 256 + rawId.
     */
    public static void initializeVanillaItems() {
        // Tools & weapons
        shovelIron = new Item(0);          // 256
        pickaxeIron = new Item(1);         // 257
        axeIron = new Item(2);             // 258
        flintAndSteel = new Item(3);       // 259
        appleRed = new Item(4);            // 260
        bow = new ItemBow(5);              // 261
        arrow = new Item(6);               // 262
        coal = new Item(7);                // 263
        diamond = new Item(8);             // 264
        ingotIron = new Item(9);           // 265
        ingotGold = new Item(10);          // 266
        swordIron = new Item(11);          // 267
        swordWood = new Item(12);          // 268
        shovelWood = new Item(13);         // 269
        pickaxeWood = new Item(14);        // 270
        axeWood = new Item(15);            // 271
        swordStone = new Item(16);         // 272
        shovelStone = new Item(17);        // 273
        pickaxeStone = new Item(18);       // 274
        axeStone = new Item(19);           // 275
        swordDiamond = new Item(20);       // 276
        shovelDiamond = new Item(21);      // 277
        pickaxeDiamond = new Item(22);     // 278
        axeDiamond = new Item(23);         // 279
        stick = new Item(24);              // 280
        bowlEmpty = new Item(25);          // 281
        bowlSoup = new Item(26);           // 282
        swordGold = new Item(27);          // 283
        shovelGold = new Item(28);         // 284
        pickaxeGold = new Item(29);        // 285
        axeGold = new Item(30);            // 286
        silk = new Item(31);               // 287
        feather = new Item(32);            // 288
        gunpowder = new Item(33);          // 289
        hoeWood = new Item(34);            // 290
        hoeStone = new Item(35);           // 291
        hoeIron = new Item(36);            // 292
        hoeDiamond = new Item(37);         // 293
        hoeGold = new Item(38);            // 294
        seeds = new Item(39);              // 295
        wheat = new Item(40);              // 296
        bread = new Item(41);              // 297

        // Armor — ItemArmor(rawId, renderIndex, armorType)
        // armorType: 0=helmet, 1=plate, 2=legs, 3=boots
        helmetLeather = new ItemArmor(42, 0, 0);   // 298
        plateLeather = new ItemArmor(43, 0, 1);     // 299
        legsLeather = new ItemArmor(44, 0, 2);      // 300
        bootsLeather = new ItemArmor(45, 0, 3);      // 301
        helmetChain = new ItemArmor(46, 1, 0);       // 302
        plateChain = new ItemArmor(47, 1, 1);        // 303
        legsChain = new ItemArmor(48, 1, 2);         // 304
        bootsChain = new ItemArmor(49, 1, 3);        // 305
        helmetIron = new ItemArmor(50, 2, 0);        // 306
        plateIron = new ItemArmor(51, 2, 1);         // 307
        legsIron = new ItemArmor(52, 2, 2);          // 308
        bootsIron = new ItemArmor(53, 2, 3);         // 309
        helmetDiamond = new ItemArmor(54, 3, 0);     // 310
        plateDiamond = new ItemArmor(55, 3, 1);      // 311
        legsDiamond = new ItemArmor(56, 3, 2);       // 312
        bootsDiamond = new ItemArmor(57, 3, 3);      // 313
        helmetGold = new ItemArmor(58, 4, 0);        // 314
        plateGold = new ItemArmor(59, 4, 1);         // 315
        legsGold = new ItemArmor(60, 4, 2);          // 316
        bootsGold = new ItemArmor(61, 4, 3);         // 317

        // Materials & food
        flint = new Item(62);              // 318
        porkRaw = new Item(63);            // 319
        porkCooked = new Item(64);         // 320
        painting = new Item(65);           // 321
        sign = new Item(67);               // 323
        doorWood = new Item(68);           // 324
        bucketEmpty = new Item(69);        // 325
        bucketWater = new Item(70);        // 326
        bucketLava = new Item(71);         // 327
        minecartEmpty = new Item(72);      // 328
        saddle = new Item(73);             // 329
        doorIron = new Item(74);           // 330
        redstone = new Item(75);           // 331
        snowball = new Item(76);           // 332
        boat = new Item(77);              // 333
        leather = new Item(78);            // 334
        bucketMilk = new Item(79);         // 335
        brick = new Item(80);              // 336
        clay = new Item(81);               // 337
        reed = new Item(82);               // 338
        paper = new Item(83);              // 339
        book = new Item(84);               // 340
        slimeBall = new Item(85);          // 341
        minecartCrate = new Item(86);      // 342
        minecartPowered = new Item(87);    // 343
        egg = new Item(88);                // 344
        compass = new Item(89);            // 345
        fishingRod = new ItemFishingRod(90); // 346
        pocketSundial = new Item(91);      // 347
        lightStoneDust = new Item(92);     // 348
        fishRaw = new Item(93);            // 349
        fishCooked = new Item(94);         // 350
        dyePowder = new Item(95);          // 351
        bone = new Item(96);               // 352
        sugar = new Item(97);              // 353
        cake = new Item(98);               // 354
        bed = new Item(99);                // 355
        redstoneRepeater = new Item(100);  // 356
        cookie = new Item(101);            // 357
        map = new ConcreteItemMap(102);    // 358
        shears = new ItemShears(103);      // 359
        melon = new Item(104);             // 360
        pumpkinSeeds = new Item(105);      // 361
        melonSeeds = new Item(106);        // 362
        beefRaw = new Item(107);           // 363
        beefCooked = new Item(108);        // 364
        chickenRaw = new Item(109);        // 365
        chickenCooked = new Item(110);     // 366
        rottenFlesh = new Item(111);       // 367
        enderPearl = new Item(112);        // 368
        blazeRod = new Item(113);          // 369
        ghastTear = new Item(114);         // 370
        goldNugget = new Item(115);        // 371
        netherStalkSeeds = new Item(116);  // 372
        potion = new ItemPotion(117);      // 373
        glassBottle = new Item(118);       // 374
        spiderEye = new Item(119);         // 375
        fermentedSpiderEye = new Item(120); // 376
        blazePowder = new Item(121);       // 377
        magmaCream = new Item(122);        // 378
        brewingStand = new Item(123);      // 379
        cauldron = new Item(124);          // 380
        eyeOfEnder = new Item(125);        // 381
        fireballCharge = new Item(129);    // 385
        writableBook = new Item(130);      // 386
        writtenBook = new Item(131);       // 387
        emerald = new Item(132);           // 388
        itemFrame = new Item(133);         // 389
        flowerPot = new Item(134);         // 390
        carrot = new Item(135);            // 391
        potato = new Item(136);            // 392
        bakedPotato = new Item(137);       // 393
        emptyMap = new ConcreteItemEmptyMap(139); // 395
        goldenCarrot = new Item(140);      // 396
        skull = new Item(141);             // 397
        carrotOnAStick = new Item(142);    // 398
        netherStar = new Item(143);        // 399
        pumpkinPie = new Item(144);        // 400
        enchantedBook = new ItemEnchantedBook(147); // 403
        expBottle = new Item(128);                  // 384 (vanilla XP bottle id)
        // 1.5.2 Item.monsterPlacer = new ItemMonsterPlacer(127) → id 383 (vanilla/server/.../Item.java:332);
        // EntityAgeable.interact (jar-excluded fc class) compares held itemID against it.
        monsterPlacer = new Item(127);              // 383 (vanilla spawn egg id)
        comparator = new Item(148);        // 404
        netherQuartz = new Item(150);      // 406
        minecartTnt = new Item(151);       // 407
        minecartHopper = new Item(152);    // 408
        // Music discs: id 2000+256=2256 (13) .. 2011+256=2267 (wait); ProxyRegistry maps
        // 2256-2267 to the modern discs, so EntityCreeper's random-in-range drop yields a
        // real disc. Only the two range endpoints are referenced by FC code.
        record13 = new Item(2000);         // 2256 (music_disc_13)
        recordWait = new Item(2011);       // 2267 (music_disc_wait)
    }

    // Concrete subclass for ItemMap (protected constructor)
    private static class ConcreteItemMap extends ItemMap {
        ConcreteItemMap(int id) { super(id); }
    }
    // Concrete subclass for ItemEmptyMap (protected constructor)
    private static class ConcreteItemEmptyMap extends ItemEmptyMap {
        ConcreteItemEmptyMap(int id) { super(id); }
    }
}
