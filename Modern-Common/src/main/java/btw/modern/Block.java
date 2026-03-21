package btw.modern;

import java.util.List;
import java.util.Random;

/**
 * Abstract representation of a block type.
 * Mirrors net.minecraft.src.Block with identical field/method names.
 */
public abstract class Block {

    // --- Static step sounds ---
    public static final StepSound soundPowderFootstep = new StepSound("stone", 1.0F, 1.0F);
    public static final StepSound soundWoodFootstep = new StepSound("wood", 1.0F, 1.0F);
    public static final StepSound soundGravelFootstep = new StepSound("gravel", 1.0F, 1.0F);
    public static final StepSound soundGrassFootstep = new StepSound("grass", 1.0F, 1.0F);
    public static final StepSound soundStoneFootstep = new StepSound("stone", 1.0F, 1.0F);
    public static final StepSound soundMetalFootstep = new StepSound("stone", 1.0F, 1.5F);
    public static final StepSound soundGlassFootstep = new StepSound("stone", 1.0F, 1.0F);
    public static final StepSound soundClothFootstep = new StepSound("cloth", 1.0F, 1.0F);
    public static final StepSound soundSandFootstep = new StepSound("sand", 1.0F, 1.0F);
    public static final StepSound soundSnowFootstep = new StepSound("snow", 1.0F, 1.0F);
    public static final StepSound soundLadderFootstep = new StepSound("ladder", 1.0F, 1.0F);
    public static final StepSound soundAnvilFootstep = new StepSound("anvil", 0.3F, 1.0F);

    // --- Static block registry ---
    public static final Block[] blocksList = new Block[4096];
    public static final boolean[] opaqueCubeLookup = new boolean[4096];
    public static final int[] lightOpacity = new int[4096];
    public static final boolean[] canBlockGrass = new boolean[4096];
    public static final int[] lightValue = new int[4096];
    public static boolean[] useNeighborBrightness = new boolean[4096];

    // --- Static block instances (populated by backend) ---
    public static Block stone;
    public static Block dirt;
    public static Block cobblestone;
    public static Block planks;
    public static Block sand;
    public static Block gravel;
    public static Block glass;
    public static BlockFluid waterMoving;
    public static Block waterStill;
    public static BlockFluid lavaMoving;
    public static Block lavaStill;
    public static Block bedrock;
    public static Block sapling;
    public static Block wood;
    public static Block sponge;
    public static Block oreGold;
    public static Block oreIron;
    public static Block oreCoal;
    public static Block oreLapis;
    public static Block blockLapis;
    public static Block dispenser;
    public static Block sandStone;
    public static Block music;
    public static Block bed;
    public static Block railPowered;
    public static Block railDetector;
    public static Block web;
    public static Block cloth;
    public static BlockFlower plantYellow;
    public static BlockFlower plantRed;
    public static BlockFlower mushroomBrown;
    public static BlockFlower mushroomRed;
    public static Block blockGold;
    public static Block blockIron;
    public static Block brick;
    public static Block tnt;
    public static Block bookShelf;
    public static Block cobblestoneMossy;
    public static Block obsidian;
    public static Block torchWood;
    public static Block mobSpawner;
    public static Block crops;
    public static Block tilledField;
    public static Block furnaceIdle;
    public static Block furnaceBurning;
    public static Block signPost;
    public static Block doorWood;
    public static Block ladder;
    public static Block rail;
    public static Block stairsCobblestone;
    public static Block signWall;
    public static Block lever;
    public static Block pressurePlateStone;
    public static Block doorIron;
    public static Block pressurePlatePlanks;
    public static Block oreRedstone;
    public static Block oreRedstoneGlowing;
    public static Block torchRedstoneIdle;
    public static Block torchRedstoneActive;
    public static Block stoneButton;
    public static Block snow;
    public static Block ice;
    public static Block blockSnow;
    public static Block cactus;
    public static Block blockClay;
    public static Block reed;
    public static Block fence;
    public static Block netherrack;
    public static Block slowSand;
    public static Block glowStone;
    public static Block stairsWoodOak;
    public static Block oreDiamond;
    public static Block blockDiamond;
    public static Block workbench;
    public static BlockChest chest;
    public static BlockRedstoneWire redstoneWire;
    public static BlockHalfSlab stoneDoubleSlab;
    public static BlockHalfSlab stoneSingleSlab;
    public static BlockGrass grass;
    public static BlockLeaves leaves;
    public static Block tripWire;
    public static BlockPortal portal;
    public static Block vine;
    public static BlockFire fire;
    public static Block anvil;
    public static BlockBeacon beacon;
    public static Block blockEmerald;
    public static Block blockNetherQuartz;
    public static Block carrot;
    public static Block chestTrapped;
    public static Block cocoaPlant;
    public static BlockDaylightDetector daylightSensor;
    public static Block dropper;
    public static Block enchantmentTable;
    public static Block endPortal;
    public static Block endPortalFrame;
    public static Block fenceGate;
    public static Block fenceIron;
    public static BlockHopper hopperBlock;
    public static Block jukebox;
    public static Block melon;
    public static Block melonStem;
    public static BlockMycelium mycelium;
    public static Block netherBrick;
    public static Block netherFence;
    public static Block netherStalk;
    public static Block oreEmerald;
    public static Block oreNetherQuartz;
    public static BlockPistonBase pistonBase;
    public static BlockPistonExtension pistonExtension;
    public static BlockPistonMoving pistonMoving;
    public static BlockPistonBase pistonStickyBase;
    public static Block potato;
    public static Block pressurePlateGold;
    public static Block pressurePlateIron;
    public static Block pumpkin;
    public static Block pumpkinLantern;
    public static Block pumpkinStem;
    public static Block railActivator;
    public static Block redstoneLampActive;
    public static Block redstoneLampIdle;
    public static BlockRedstoneRepeater redstoneRepeaterIdle;
    public static Block silverfish;
    public static Block skull;
    public static Block stairsBrick;
    public static Block stairsNetherBrick;
    public static Block stairsNetherQuartz;
    public static Block stairsSandStone;
    public static Block stairsStoneBrick;
    public static Block stairsWoodBirch;
    public static Block stairsWoodJungle;
    public static Block stairsWoodSpruce;
    public static Block stoneBrick;
    public static BlockTallGrass tallGrass;
    public static Block thinGlass;
    public static Block trapdoor;
    public static BlockTripWireSource tripWireSource;
    public static Block waterlily;
    public static Block whiteStone;
    public static BlockHalfSlab woodDoubleSlab;
    public static BlockHalfSlab woodSingleSlab;
    public static Block woodenButton;

    // --- Instance fields ---
    public final int blockID;
    public float blockHardness;
    public float blockResistance;
    public boolean blockConstructorCalled = true;
    public boolean enableStats = true;
    public boolean needsRandomTick;
    public boolean isBlockContainer;
    public double minX = 0D;
    public double minY = 0D;
    public double minZ = 0D;
    public double maxX = 1D;
    public double maxY = 1D;
    public double maxZ = 1D;
    public StepSound stepSound;
    public float blockParticleGravity;
    public Material blockMaterial;
    public float slipperiness;
    protected CreativeTabs displayOnCreativeTab;

    // --- BTW backing fields for builder-pattern Set*() methods ---
    private boolean shovelsEffective;
    private boolean picksEffective;
    private boolean axesEffective;
    private boolean hoesEffective;
    private boolean chiselsEffective;
    private boolean chiselsCanHarvest;
    private int fireEncouragement;
    private int flammability;
    private float buoyancy = -1.0F;
    private int furnaceBurnTime;
    private int herbivoreItemFoodValue;
    private int chickenItemFoodValue;
    private int pigItemFoodValue;
    private boolean canBeCookedByKiln;
    private int kilnDropItemIndex = -1;
    private int kilnDropItemDamage;

    // --- Client-side rendering fields ---
    public Icon blockIcon;
    public RenderBlocks m_currentBlockRenderer;

    protected Block(int blockID, Material material) {
        this.blockID = blockID;
        this.blockMaterial = material;
        // Self-register in the global blocksList
        if (blockID >= 0 && blockID < blocksList.length) {
            blocksList[blockID] = this;
        }
    }

    // --- Initialization ---

    public void initializeBlock() {}

    // --- Builder/setter methods ---

    public Block setStepSound(StepSound stepSound) {
        this.stepSound = stepSound;
        return this;
    }

    public Block setLightOpacity(int opacity) {
        lightOpacity[this.blockID] = opacity;
        return this;
    }

    public Block setLightValue(float value) {
        lightValue[this.blockID] = (int)(15.0F * value);
        return this;
    }

    public Block setResistance(float resistance) {
        this.blockResistance = resistance * 3.0F;
        return this;
    }

    public Block setHardness(float hardness) {
        this.blockHardness = hardness;
        return this;
    }

    public Block setBlockUnbreakable() {
        this.setHardness(-1.0F);
        return this;
    }

    public Block setTickRandomly(boolean tickRandomly) {
        this.needsRandomTick = tickRandomly;
        return this;
    }

    private String unlocalizedName = "";
    protected String textureName;

    public Block setUnlocalizedName(String name) {
        this.unlocalizedName = name;
        return this;
    }

    /**
     * Sets the texture name used by the default registerIcons.
     * In MC 1.5.2 this was a separate field from unlocalizedName.
     * FC blocks that construct icon names like getTextureName() + "_on"
     * rely on this returning a non-null base name.
     */
    public Block setTextureName(String name) {
        this.textureName = name;
        return this;
    }

    /**
     * Returns the base texture name for this block.
     * Falls back to unlocalizedName if textureName was never set.
     */
    public String getTextureName() {
        if (this.textureName != null) return this.textureName;
        if (this.unlocalizedName != null && !this.unlocalizedName.isEmpty()) return this.unlocalizedName;
        return "MISSING_ICON_BLOCK_" + this.blockID + "_" + this.unlocalizedName;
    }

    public Block setCreativeTab(CreativeTabs tab) {
        this.displayOnCreativeTab = tab;
        return this;
    }

    public Block disableStats() {
        this.enableStats = false;
        return this;
    }

    // --- Block bounds ---

    public final void setBlockBounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public final double getBlockBoundsMinX() { return this.minX; }
    public final double getBlockBoundsMaxX() { return this.maxX; }
    public final double getBlockBoundsMinY() { return this.minY; }
    public final double getBlockBoundsMaxY() { return this.maxY; }
    public final double getBlockBoundsMinZ() { return this.minZ; }
    public final double getBlockBoundsMaxZ() { return this.maxZ; }

    // --- Static query methods ---

    public static boolean isNormalCube(int blockID) {
        Block block = blocksList[blockID];
        return block != null && block.isOpaqueCube();
    }

    // --- Instance query methods ---

    public boolean renderAsNormalBlock() { return true; }
    public boolean getBlocksMovement(IBlockAccess blockAccess, int x, int y, int z) { return true; }
    public int getRenderType() { return 0; }
    public int getRenderBlockPass() { return 0; }
    public boolean getTickRandomly() { return this.needsRandomTick; }
    public boolean hasTileEntity() { return this.isBlockContainer; }
    public boolean isBlockSolid(IBlockAccess blockAccess, int x, int y, int z, int side) { return true; }
    public boolean isOpaqueCube() { return true; }
    public boolean canCollideCheck(int metadata, boolean hitIfLiquid) { return true; }
    public boolean isCollidable() { return true; }
    public boolean canProvidePower() { return false; }
    public boolean canSilkHarvest() { return false; }
    public boolean getEnableStats() { return this.enableStats; }
    public int getMobilityFlag() { return 0; }
    public boolean hasComparatorInputOverride() { return false; }
    public int getComparatorInputOverride(World world, int i, int j, int k, int side) { return 0; }
    public boolean canDropFromExplosion(Explosion explosion) { return true; }

    public float getBlockHardness(World world, int x, int y, int z) {
        return this.blockHardness;
    }

    public float getExplosionResistance(Entity entity) {
        return this.blockResistance / 5.0F;
    }

    public float getPlayerRelativeBlockHardness(EntityPlayer player, World world, int x, int y, int z) {
        float fBlockHardness = getBlockHardness(world, x, y, z);

        if (fBlockHardness >= 0F) {
            float fRelativeHardness = player.getCurrentPlayerStrVsBlock(this, x, y, z) / fBlockHardness;

            if (player.IsCurrentToolEffectiveOnBlock(this, x, y, z)) {
                return fRelativeHardness / 30F;
            } else {
                return fRelativeHardness / 200F;
            }
        } else {
            return 0F;
        }
    }

    public int tickRate(World world) {
        return 10;
    }

    public String getLocalizedName() { return getUnlocalizedName(); }
    public String getUnlocalizedName() { return unlocalizedName; }
    public CreativeTabs getCreativeTabToDisplayOn() { return this.displayOnCreativeTab; }

    // --- Drops ---

    public int quantityDropped(Random random) {
        return 1;
    }

    public int idDropped(int metadata, Random random, int fortune) {
        return this.blockID;
    }

    public int damageDropped(int metadata) {
        return 0;
    }

    public int quantityDroppedWithBonus(int fortune, Random random) {
        return this.quantityDropped(random);
    }

    public int getDamageValue(World world, int x, int y, int z) {
        return this.damageDropped(world.getBlockMetadata(x, y, z));
    }

    public ItemStack createStackedBlock(int metadata) {
        return new ItemStack(this.blockID, 1, this.damageDropped(metadata));
    }

    // --- World interaction callbacks ---

    public void updateTick(World world, int x, int y, int z, Random random) {}
    public void onBlockAdded(World world, int x, int y, int z) {}
    public void breakBlock(World world, int x, int y, int z, int oldBlockID, int oldMetadata) {}
    public void onNeighborBlockChange(World world, int x, int y, int z, int neighborBlockID) {}
    public void onBlockDestroyedByPlayer(World world, int x, int y, int z, int metadata) {}
    public void onBlockDestroyedByExplosion(World world, int x, int y, int z, Explosion explosion) {}

    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        return false;
    }

    public void onEntityWalking(World world, int x, int y, int z, Entity entity) {}
    public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {}
    public void onBlockClicked(World world, int x, int y, int z, EntityPlayer player) {}
    public void onFallenUpon(World world, int x, int y, int z, Entity entity, float fallDistance) {}

    public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int metadata) {
        return metadata;
    }

    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLiving livingEntity, ItemStack stack) {}
    public void onPostBlockPlaced(World world, int x, int y, int z, int metadata) {}
    public void onBlockHarvested(World world, int x, int y, int z, int metadata, EntityPlayer player) {}
    public void onSetBlockIDWithMetaData(World world, int x, int y, int z, int metadata) {}
    public void fillWithRain(World world, int x, int y, int z) {}

    public boolean onBlockEventReceived(World world, int x, int y, int z, int eventID, int eventParam) {
        return false;
    }

    // --- Placement checks ---

    public boolean canPlaceBlockOnSide(World world, int x, int y, int z, int side, ItemStack stack) {
        return this.canPlaceBlockOnSide(world, x, y, z, side);
    }

    public boolean canPlaceBlockOnSide(World world, int x, int y, int z, int side) {
        return this.canPlaceBlockAt(world, x, y, z);
    }

    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        return true;
    }

    public boolean canBlockStay(World world, int x, int y, int z) {
        return true;
    }

    // --- Collision/rendering ---

    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask, List list, Entity entity) {
        AxisAlignedBB blockBB = this.getCollisionBoundingBoxFromPool(world, x, y, z);

        if (blockBB != null && mask.intersectsWith(blockBB)) {
            list.add(blockBB);
        }
    }

    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return AxisAlignedBB.getBoundingBox(
            (double)x + this.minX, (double)y + this.minY, (double)z + this.minZ,
            (double)x + this.maxX, (double)y + this.maxY, (double)z + this.maxZ);
    }

    public MovingObjectPosition collisionRayTrace(World world, int x, int y, int z, Vec3 startVec, Vec3 endVec) {
        this.setBlockBoundsBasedOnState(world, x, y, z);

        Vec3 adjustedStart = startVec.addVector((double)(-x), (double)(-y), (double)(-z));
        Vec3 adjustedEnd = endVec.addVector((double)(-x), (double)(-y), (double)(-z));

        AxisAlignedBB blockBB = AxisAlignedBB.getBoundingBox(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
        MovingObjectPosition hit = blockBB.calculateIntercept(adjustedStart, adjustedEnd);

        if (hit != null) {
            hit.blockX = x;
            hit.blockY = y;
            hit.blockZ = z;
            hit.hitVec = hit.hitVec.addVector((double)x, (double)y, (double)z);
            return hit;
        }

        return null;
    }

    public void setBlockBoundsBasedOnState(IBlockAccess blockAccess, int x, int y, int z) {
        // FC blocks override GetBlockBoundsFromPoolBasedOnState for state-dependent
        // shapes (unfired brick orientation, slab flip, etc.). Use it to set
        // the rendering bounds so renderStandardBlock uses the correct shape.
        AxisAlignedBB bounds = GetBlockBoundsFromPoolBasedOnState(blockAccess, x, y, z);
        if (bounds != null) {
            this.minX = bounds.minX;
            this.minY = bounds.minY;
            this.minZ = bounds.minZ;
            this.maxX = bounds.maxX;
            this.maxY = bounds.maxY;
            this.maxZ = bounds.maxZ;
        }
    }
    public void setBlockBoundsForItemRender() {
        this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
    }
    public void velocityToAddToEntity(World world, int x, int y, int z, Entity entity, Vec3 vec3) {}

    // --- Redstone ---

    public int isProvidingWeakPower(IBlockAccess blockAccess, int x, int y, int z, int side) {
        return 0;
    }

    public int isProvidingStrongPower(IBlockAccess blockAccess, int x, int y, int z, int side) {
        return 0;
    }

    // --- Harvesting ---

    public void harvestBlock(World world, EntityPlayer player, int x, int y, int z, int metadata) {
        // FC implementation from patched vanilla Block.java
        if (player != null) {
            player.AddHarvestBlockExhaustion(blockID, x, y, z, metadata);
        }

        if (this.canSilkHarvest(metadata) && player != null
                && EnchantmentHelper.getSilkTouchModifier(player)) {
            ItemStack silkStack = this.createStackedBlock(metadata);
            if (silkStack != null) {
                this.dropBlockAsItem_do(world, x, y, z, silkStack);
            }
        } else {
            int fortune = (player != null) ? EnchantmentHelper.getFortuneModifier(player) : 0;
            this.dropBlockAsItem(world, x, y, z, metadata, fortune);
        }
    }

    public final void dropBlockAsItem(World world, int x, int y, int z, int metadata, int fortune) {
        this.dropBlockAsItemWithChance(world, x, y, z, metadata, 1.0F, fortune);
    }

    public void dropBlockAsItemWithChance(World world, int x, int y, int z, int metadata, float chance, int fortune) {
        if (!world.isRemote) {
            int count = this.quantityDroppedWithBonus(fortune, world.rand);

            for (int i = 0; i < count; i++) {
                if (world.rand.nextFloat() <= chance) {
                    int droppedId = this.idDropped(metadata, world.rand, fortune);

                    if (droppedId > 0) {
                        this.dropBlockAsItem_do(world, x, y, z,
                                new ItemStack(droppedId, 1, this.damageDropped(metadata)));
                    }
                }
            }
        }
    }

    public void dropBlockAsItem_do(World world, int x, int y, int z, ItemStack stack) {
        if (!world.isRemote && stack != null && stack.stackSize > 0) {
            if (world.getGameRules() != null
                    && !world.getGameRules().getGameRuleBooleanValue("doTileDrops")) {
                return;
            }
            float spread = 0.7F;
            double dx = (double)(world.rand.nextFloat() * spread) + (double)(1.0F - spread) * 0.5D;
            double dy = (double)(world.rand.nextFloat() * spread) + (double)(1.0F - spread) * 0.5D;
            double dz = (double)(world.rand.nextFloat() * spread) + (double)(1.0F - spread) * 0.5D;
            EntityItem entityItem = new EntityItem(world,
                    (double) x + dx, (double) y + dy, (double) z + dz, stack);
            entityItem.delayBeforeCanPickup = 10;
            world.spawnEntityInWorld(entityItem);
        }
    }

    public void dropXpOnBlockBreak(World world, int x, int y, int z, int xpAmount) {
        // FC removes XP drops from block breaking
    }

    // --- BTW-added: Falling block methods ---

    public boolean CheckForFall(World world, int i, int j, int k) { return false; }
    public void onStartFalling(EntityFallingSand entity) {}
    public void onFinishFalling(World world, int i, int j, int k, int iMetadata) {}
    public void OnFallingUpdate(EntityFallingSand entity) {}

    public void NotifyNearbyAnimalsFinishedFalling(World world, int i, int j, int k) {}

    public boolean OnFinishedFalling(EntityFallingSand entity, float fFallDistance) { return false; }
    public boolean AttemptToCombineWithFallingEntity(World world, int i, int j, int k, EntityFallingSand entity) { return false; }
    public boolean CanBeCrushedByFallingEntity(World world, int i, int j, int k, EntityFallingSand entity) { return false; }
    public void OnCrushedByFallingEntity(World world, int i, int j, int k, EntityFallingSand entity) {}
    public boolean CanFallIntoBlockAtPos(World world, int i, int j, int k) { return false; }
    public boolean CanSupportFallingBlocks(IBlockAccess blockAccess, int i, int j, int k) { return true; }
    public void CheckForUnstableGround(World world, int i, int j, int k) {}
    public void ScheduleCheckForFall(World world, int i, int j, int k) {}
    public void OnBlockDestroyedLandingFromFall(World world, int i, int j, int k, int iMetadata) {}
    public boolean HasFallingBlockRestingOn(IBlockAccess blockAccess, int i, int j, int k) { return false; }

    // --- BTW-added: Facing methods ---

    public int GetFacing(IBlockAccess blockAccess, int i, int j, int k) { return GetFacing(blockAccess.getBlockMetadata(i, j, k)); }
    public int GetFacing(int iMetadata) { return 0; }
    public void SetFacing(World world, int i, int j, int k, int iFacing) {
        int metadata = world.getBlockMetadata(i, j, k);
        int newMetadata = SetFacing(metadata, iFacing);
        world.setBlockMetadataWithClient(i, j, k, newMetadata);
    }
    public int SetFacing(int iMetadata, int iFacing) { return iMetadata; }
    public boolean ToggleFacing(World world, int i, int j, int k, boolean bReverse) { return false; }

    // --- BTW-added: Turntable methods ---

    public boolean CanRotateOnTurntable(IBlockAccess blockAccess, int i, int j, int k) { return false; }
    public boolean CanTransmitRotationHorizontallyOnTurntable(IBlockAccess blockAccess, int i, int j, int k) { return false; }
    public boolean CanTransmitRotationVerticallyOnTurntable(IBlockAccess blockAccess, int i, int j, int k) { return false; }
    public int RotateOnTurntable(World world, int i, int j, int k, boolean bReverse, int iCraftingCounter) { return 0; }
    public int GetRotationsToCraftOnTurntable(IBlockAccess blockAccess, int i, int j, int k) { return 0; }
    public void OnCraftedOnTurntable(World world, int i, int j, int k) {}
    public int GetNewBlockIDCraftedOnTurntable(IBlockAccess blockAccess, int i, int j, int k) { return 0; }
    public int GetNewMetadataCraftedOnTurntable(IBlockAccess blockAccess, int i, int j, int k) { return 0; }
    public int GetItemIDCraftedOnTurntable(IBlockAccess blockAccess, int i, int j, int k) { return 0; }
    public int GetItemCountCraftedOnTurntable(IBlockAccess blockAccess, int i, int j, int k) { return 0; }
    public int GetItemDamageCraftedOnTurntable(IBlockAccess blockAccess, int i, int j, int k) { return 0; }
    public boolean RotateAroundJAxis(World world, int i, int j, int k, boolean bReverse) {
        int oldMeta = world.getBlockMetadata(i, j, k);
        int newMeta = RotateMetadataAroundJAxis(oldMeta, bReverse);
        if (newMeta != oldMeta) {
            world.setBlockMetadataWithClient(i, j, k, newMeta);
            return true;
        }
        return false;
    }
    public int RotateMetadataAroundJAxis(int iMetadata, boolean bReverse) { return iMetadata; }
    protected void OnRotatedOnTurntable(World world, int i, int j, int k) {}
    protected int TurntableCraftingRotation(World world, int i, int j, int k, boolean bReverse, int iCraftingCounter) { return 0; }
    public boolean CanRotateAroundBlockOnTurntableToFacing(World world, int i, int j, int k, int iFacing) { return false; }
    public boolean OnRotatedAroundBlockOnTurntableToFacing(World world, int i, int j, int k, int iFacing) { return false; }
    public int GetNewMetadataRotatedAroundBlockOnTurntableToFacing(World world, int i, int j, int k, int iInitialFacing, int iRotatedFacing) { return 0; }
    public int ConvertFacingToTopTextureRotation(int iFacing) { return 0; }
    public int ConvertFacingToBottomTextureRotation(int iFacing) { return 0; }

    // --- BTW-added: Block dispenser methods ---

    public ItemStack GetStackRetrievedByBlockDispenser(World world, int i, int j, int k) { return null; }
    public boolean IsBlockDestroyedByBlockDispenser(int iMetadata) { return false; }
    public void OnRemovedByBlockDispenser(World world, int i, int j, int k) {}

    // --- BTW-added: Lightning ---

    public void OnStruckByLightning(World world, int i, int j, int k) {}

    // --- BTW-added: Mob spawning ---

    public boolean CanMobsSpawnOn(World world, int i, int j, int k) { return true; }
    public float MobSpawnOnVerticalOffset(World world, int i, int j, int k) { return 0; }

    // --- BTW-added: Block bounds ---

    public void InitBlockBounds(double dMinX, double dMinY, double dMinZ, double dMaxX, double dMaxY, double dMaxZ) {
        this.minX = dMinX;
        this.minY = dMinY;
        this.minZ = dMinZ;
        this.maxX = dMaxX;
        this.maxY = dMaxY;
        this.maxZ = dMaxZ;
    }

    public void InitBlockBounds(AxisAlignedBB bounds) {
        this.minX = bounds.minX;
        this.minY = bounds.minY;
        this.minZ = bounds.minZ;
        this.maxX = bounds.maxX;
        this.maxY = bounds.maxY;
        this.maxZ = bounds.maxZ;
    }

    public AxisAlignedBB GetFixedBlockBoundsFromPool() {
        return AxisAlignedBB.getBoundingBox(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
    }

    public AxisAlignedBB GetBlockBoundsFromPoolBasedOnState(IBlockAccess blockAccess, int i, int j, int k) { return GetFixedBlockBoundsFromPool(); }

    public MovingObjectPosition MouseOverRayTrace(World world, int i, int j, int k, Vec3 startVec, Vec3 endVec) { return null; }

    public MovingObjectPosition CollisionRayTraceVsBlockBounds(World world, int i, int j, int k, Vec3 startVec, Vec3 endVec) { return null; }

    // --- BTW-added: Grazing methods ---

    public boolean CanBeGrazedOn(IBlockAccess blockAccess, int i, int j, int k, EntityAnimal animal) { return false; }
    public void OnGrazed(World world, int i, int j, int k, EntityAnimal animal) {}
    public void OnVegetationAboveGrazed(World world, int i, int j, int k, EntityAnimal animal) {}

    // --- BTW-added: Neighbor disruption ---

    public void NotifyNeighborsBlockDisrupted(World world, int i, int j, int k) {}
    public void OnNeighborDisrupted(World world, int i, int j, int k, int iToFacing) {}

    // --- BTW-added: Food values ---

    public int GetHerbivoreItemFoodValue(int iItemDamage) { return herbivoreItemFoodValue; }
    public void SetHerbivoreItemFoodValue(int iFoodValue) { this.herbivoreItemFoodValue = iFoodValue; }
    public int GetChickenItemFoodValue(int iItemDamage) { return chickenItemFoodValue; }
    public void SetChickenItemFoodValue(int iFoodValue) { this.chickenItemFoodValue = iFoodValue; }
    public int GetPigItemFoodValue(int iItemDamage) { return pigItemFoodValue; }
    public void SetPigItemFoodValue(int iFoodValue) { this.pigItemFoodValue = iFoodValue; }

    // --- BTW-added: Plant growth ---

    public boolean CanDomesticatedCropsGrowOnBlock(World world, int i, int j, int k) { return false; }
    public boolean CanReedsGrowOnBlock(World world, int i, int j, int k) { return false; }
    public boolean CanSaplingsGrowOnBlock(World world, int i, int j, int k) { return false; }
    public boolean CanWildVegetationGrowOnBlock(World world, int i, int j, int k) { return false; }
    public boolean CanNetherWartGrowOnBlock(World world, int i, int j, int k) { return false; }
    public boolean CanCactusGrowOnBlock(World world, int i, int j, int k) { return false; }
    public boolean IsBlockHydratedForPlantGrowthOn(World world, int i, int j, int k) { return false; }
    public boolean IsConsideredNeighbouringWaterForReedGrowthOn(World world, int i, int j, int k) { return false; }
    public float GetPlantGrowthOnMultiplier(World world, int i, int j, int k, Block plantBlock) { return 1.0F; }
    public boolean GetIsFertilizedForPlantGrowth(World world, int i, int j, int k) { return false; }
    public void NotifyOfFullStagePlantGrowthOn(World world, int i, int j, int k, Block plantBlock) {}
    public void NotifyOfPlantAboveRemoved(World world, int i, int j, int k, Block plantBlock) {}

    // --- BTW-added: Weeds ---

    public boolean CanWeedsGrowInBlock(IBlockAccess blockAccess, int i, int j, int k) { return false; }
    public int GetWeedsGrowthLevel(IBlockAccess blockAccess, int i, int j, int k) { return 0; }
    public void RemoveWeeds(World world, int i, int j, int k) {}
    public boolean AttemptToApplyFertilizerTo(World world, int i, int j, int k) { return false; }
    public boolean GetConvertsLegacySoil(IBlockAccess blockAccess, int i, int j, int k) { return false; }

    // --- BTW-added: Spiderweb replacement ---

    public boolean CanSpitWebReplaceBlock(World world, int i, int j, int k) { return false; }

    // --- BTW-added: Tool effectiveness ---

    public boolean AreAxesEffectiveOn() { return axesEffective; }
    public boolean AreChiselsEffectiveOn() { return chiselsEffective; }
    public boolean AreChiselsEffectiveOn(World world, int i, int j, int k) { return AreChiselsEffectiveOn(); }
    public boolean AreHoesEffectiveOn() { return hoesEffective; }
    public boolean ArePicksEffectiveOn() { return picksEffective; }
    public boolean AreShovelsEffectiveOn() { return shovelsEffective; }
    public boolean CanChiselsHarvest() { return chiselsCanHarvest; }
    public int GetEfficientToolLevel() { return 0; }
    public int GetEfficientToolLevel(IBlockAccess blockAccess, int i, int j, int k) { return 0; }
    public int GetHarvestToolLevel() { return 0; }
    public int GetHarvestToolLevel(IBlockAccess blockAccess, int i, int j, int k) { return GetEfficientToolLevel(blockAccess, i, j, k); }
    public int GetCurrentGrindingType() { return 0; }

    public Block SetShovelsEffectiveOn() { this.shovelsEffective = true; return this; }
    public Block SetShovelsEffectiveOn(boolean bEffective) { this.shovelsEffective = bEffective; return this; }
    public Block SetPicksEffectiveOn() { this.picksEffective = true; return this; }
    public Block SetPicksEffectiveOn(boolean bEffective) { this.picksEffective = bEffective; return this; }
    public Block SetAxesEffectiveOn() { this.axesEffective = true; return this; }
    public Block SetAxesEffectiveOn(boolean bEffective) { this.axesEffective = bEffective; return this; }
    public Block SetHoesEffectiveOn() { this.hoesEffective = true; return this; }
    public Block SetHoesEffectiveOn(boolean bEffective) { this.hoesEffective = bEffective; return this; }
    public Block SetChiselsEffectiveOn() { this.chiselsEffective = true; return this; }
    public Block SetChiselsEffectiveOn(boolean bEffective) { this.chiselsEffective = bEffective; return this; }
    public Block SetChiselsCanHarvest() { this.chiselsCanHarvest = true; return this; }
    public Block SetChiselsCanHarvest(boolean bCanHarvest) { this.chiselsCanHarvest = bCanHarvest; return this; }

    // --- BTW-added: Block material setter ---

    public void SetBlockMaterial(Material material) {
        this.blockMaterial = material;
    }

    // --- BTW-added: Misc ---

    public boolean GetIsProblemToRemove() { return false; }
    public boolean GetIsProblemToRemove(IBlockAccess blockAccess, int i, int j, int k) { return false; }
    public boolean GetDoesStumpRemoverWorkOnBlock(IBlockAccess blockAccess, int i, int j, int k) { return false; }
    public boolean GetPreventsFluidFlow() { return false; }
    public boolean GetPreventsFluidFlow(World world, int i, int j, int k, Block fluidBlock) { return false; }
    public boolean IsBreakableBarricade() { return false; }
    public boolean IsBreakableBarricade(IBlockAccess blockAccess, int i, int j, int k) { return false; }
    public boolean IsBreakableBarricadeOpen() { return false; }
    public boolean IsBreakableBarricadeOpen(IBlockAccess blockAccess, int i, int j, int k) { return false; }
    public boolean CanInputAxlePowerToFacing(World world, int i, int j, int k, int iFacing) { return false; }

    // --- BTW-added: Movement ---

    public float GetMovementModifier(World world, int i, int j, int k) { return 1.0F; }
    public void OnPlayerWalksOnBlock(World world, int i, int j, int k, EntityPlayer player) {}
    public boolean IsBlockClimbable(World world, int i, int j, int k) { return false; }

    // --- BTW-added: Normal cube and related ---

    public boolean IsNormalCube(IBlockAccess blockAccess, int i, int j, int k) { return renderAsNormalBlock(); }
    public boolean IsAirBlock() { return false; }
    public boolean IsReplaceableVegetation(World world, int i, int j, int k) { return false; }
    public boolean IsGroundCover() { return false; }
    public boolean IsStairBlock() { return false; }
    public boolean IsNaturalStone(IBlockAccess blockAccess, int i, int j, int k) { return false; }

    // --- BTW-added: Pre-placement ---

    public int PreBlockPlacedBy(World world, int i, int j, int k, int iMetadata, EntityLiving entityBy) { return iMetadata; }

    // --- BTW-added: Random update ---

    public void RandomUpdateTick(World world, int i, int j, int k, Random rand) {
        updateTick(world, i, j, k, rand);
    }

    // --- BTW-added: Client methods ---

    public void ClientNotificationOfMetadataChange(World world, int i, int j, int k, int iOldMetadata, int iNewMetadata) {}
    public void ClientBreakBlock(World world, int i, int j, int k, int iBlockID, int iMetadata) {}
    public void ClientBlockAdded(World world, int i, int j, int k) {}

    // --- Client-side rendering methods ---

    public Icon getIcon(int side, int metadata) {
        Icon icon = this.blockIcon;
        if (icon != null && Tessellator.instance.isCapturing()) {
            Tessellator.instance.setCurrentTextureName(icon.getIconName());
        }
        return icon;
    }

    public Icon getBlockTexture(IBlockAccess blockAccess, int x, int y, int z, int side) {
        return this.getIcon(side, blockAccess.getBlockMetadata(x, y, z));
    }

    public Icon getBlockTextureFromSide(int side) {
        return this.getIcon(side, 0);
    }

    public boolean shouldSideBeRendered(IBlockAccess blockAccess, int x, int y, int z, int side) {
        return true;
    }

    public int getMixedBrightnessForBlock(IBlockAccess blockAccess, int x, int y, int z) {
        // Full brightness default (sky light 15 | block light 15 packed as 0xF000F0)
        return 0xF000F0;
    }

    public int colorMultiplier(IBlockAccess blockAccess, int x, int y, int z) {
        return 0xFFFFFF;
    }

    public int getBlockColor() {
        return 0xFFFFFF;
    }

    public int getRenderColor(int metadata) {
        return 0xFFFFFF;
    }

    public void registerIcons(IconRegister register) {
        // Default implementation: register the block's texture using
        // getTextureName() (matches MC 1.5.2 behavior).
        // FC block subclasses override this to register their own textures,
        // but many call super.registerIcons() expecting blockIcon to be set.
        String name = this.getTextureName();
        if (name != null && !name.isEmpty() && !name.startsWith("MISSING_ICON_BLOCK_")) {
            this.blockIcon = register.registerIcon(name);
        }
    }

    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
    }

    public float getBlockBrightness(IBlockAccess blockAccess, int x, int y, int z) {
        return 0;
    }

    public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z) {
        return AxisAlignedBB.getBoundingBox(
            (double)x + this.minX, (double)y + this.minY, (double)z + this.minZ,
            (double)x + this.maxX, (double)y + this.maxY, (double)z + this.maxZ);
    }

    public String getUnlocalizedName2() {
        // In MC 1.5.2: getUnlocalizedName() returned "tile.<name>",
        // getUnlocalizedName2() returned just "<name>".
        // FC uses this as the base texture name in registerIcons.
        return unlocalizedName != null ? unlocalizedName : "";
    }

    public Icon func_94438_c(int index) {
        return this.blockIcon;
    }

    public boolean isPowered() {
        return false;
    }

    public AxisAlignedBB GetBlockBoundsFromPoolForItemRender(int iItemDamage) {
        return GetFixedBlockBoundsFromPool();
    }

    public boolean DoesItemRenderAsBlock(int iItemDamage) {
        return true;
    }

    public boolean ShouldRenderNeighborHalfSlabSide(IBlockAccess blockAccess, int x, int y, int z, int side, boolean isUpsideDown) {
        return true;
    }

    public boolean ShouldRenderNeighborFullFaceSide(IBlockAccess blockAccess, int x, int y, int z, int side) {
        return true;
    }

    public Icon GetIconByIndex(int index) {
        Icon icon = this.blockIcon;
        // Auto-set texture name on Tessellator so FC code that calls
        // GetIconByIndex then addVertexWithUV (without going through
        // RenderBlocks face methods) still gets the right texture.
        if (icon != null) {
            Tessellator.instance.setCurrentTextureName(icon.getIconName());
        }
        return icon;
    }

    public Icon GetHopperFilterIcon() {
        return this.blockIcon;
    }

    // --- Client-side: Block rendering methods (overridden by subclasses) ---

    public boolean RenderBlock(RenderBlocks renderBlocks, int i, int j, int k) {
        if (renderBlocks.blockAccess != null) {
            setBlockBoundsBasedOnState(renderBlocks.blockAccess, i, j, k);
        }
        renderBlocks.setRenderBoundsFromBlock(this);
        return renderBlocks.renderBlockByRenderType(this, i, j, k);
    }

    public void RenderBlockAsItem(RenderBlocks renderBlocks, int iItemDamage, float fBrightness) {
        renderBlocks.renderBlockAsItem(this, iItemDamage, fBrightness);
    }

    public void RenderFallingBlock(RenderBlocks renderBlocks, int i, int j, int k, int iMetadata) {
    }

    public void RenderBlockMovedByPiston(RenderBlocks renderBlocks, int i, int j, int k) {
    }

    public boolean RenderBlockWithTexture(RenderBlocks renderBlocks, int i, int j, int k, Icon texture) {
        // Render the block with an override texture (used for overlays like cook speckles)
        renderBlocks.setOverrideBlockTexture(texture);
        renderBlocks.setRenderBoundsFromBlock(this);
        boolean result = renderBlocks.renderStandardBlock(this, i, j, k);
        renderBlocks.clearOverrideBlockTexture();
        return result;
    }

    public void RenderCookingByKilnOverlay(RenderBlocks renderBlocks, int i, int j, int k, boolean bCooking) {
    }

    public void RenderCrossHatch(RenderBlocks renderBlocks, int i, int j, int k, Icon icon, double dYOffset, double dHeight) {
    }

    public void RenderBlockSecondPass(RenderBlocks renderBlocks, int i, int j, int k, boolean bFirstPassResult) {
    }

    public boolean ShouldSideBeRenderedOnFallingBlock(int iSide, int iMetadata) {
        return true;
    }

    public boolean ShouldRenderWhileFalling(World world, EntityFallingSand entity) {
        return true;
    }

    public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, MovingObjectPosition rayTraceHit) {
        int i = rayTraceHit.blockX;
        int j = rayTraceHit.blockY;
        int k = rayTraceHit.blockZ;
        return getSelectedBoundingBoxFromPool(world, i, j, k);
    }


    public int getState(IBlockAccess blockAccess, int i, int j, int k) {
        return blockAccess.getBlockMetadata(i, j, k);
    }

    public String getItemIconName() {
        return null;
    }

    public void getSubBlocks(int blockID, CreativeTabs creativeTabs, List list) {
        list.add(new ItemStack(blockID, 1, 0));
    }

    public int idPicked(World world, int i, int j, int k) {
        return this.blockID;
    }

    // --- BTW-added: Arrow ---

    public void OnArrowImpact(World world, int i, int j, int k, EntityArrow arrow) {}
    public void OnArrowCollide(World world, int i, int j, int k, EntityArrow arrow) {}

    // --- BTW-added: Hopper ---

    public boolean DoesBlockHopperEject(World world, int i, int j, int k) { return true; }
    public boolean DoesBlockHopperInsert(World world, int i, int j, int k) { return false; }

    // --- BTW-added: Warmth ---

    public boolean GetIsBlockWarm(IBlockAccess blockAccess, int i, int j, int k) { return false; }

    // --- BTW-added: Step sound ---

    public StepSound GetStepSound(World world, int i, int j, int k) { return stepSound; }

    // --- BTW-added: Strata ---

    public boolean HasStrata() { return false; }
    public int GetMetadataConversionForStrataLevel(int iLevel, int iMetadata) { return iMetadata; }

    // --- BTW-added: Explosion resistance overload ---

    public float getExplosionResistance(Entity entity, World world, int i, int j, int k) {
        return getExplosionResistance(entity);
    }

    // --- BTW-added: Can block stay during generate ---

    public boolean CanBlockStayDuringGenerate(World world, int i, int j, int k) {
        return canBlockStay(world, i, j, k);
    }

    // --- BTW-added: Tile entity ---

    public boolean ShouldDeleteTileEntityOnBlockChange(int iNewBlockID) { return true; }
    public TileEntity createNewTileEntity(World world) { return null; }

    // --- BTW-added: Full block bounding box ---

    public static AxisAlignedBB GetFulBlockBoundingBoxFromPool(World world, int i, int j, int k) {
        return AxisAlignedBB.getBoundingBox(i, j, k, i + 1.0, j + 1.0, k + 1.0);
    }

    // --- BTW-added: Water to sides ---

    public boolean HasWaterToSidesOrTop(World world, int i, int j, int k) { return false; }

    // --- BTW-added: Fluid flow ---

    public void OnFluidFlowIntoBlock(World world, int i, int j, int k, BlockFluid fluidBlock) {}

    // --- BTW-added: Buddy ---

    public boolean TriggersBuddy() { return true; }

    // --- BTW-added: Silk harvest overload ---

    public boolean canSilkHarvest(int iMetadata) { return canSilkHarvest(); }

    // --- BTW-added: Harvesting/improper tool ---

    public void OnBlockDestroyedWithImproperTool(World world, EntityPlayer player, int i, int j, int k, int iMetadata) {
        // FC implementation: play SFX and drop component items
        org.apache.logging.log4j.LogManager.getLogger("BTW-Harvest").info(
                "[BTW-DEBUG] OnBlockDestroyedWithImproperTool calling DropComponentItemsOnBadBreak on {}",
                this.getClass().getSimpleName());
        boolean result = DropComponentItemsOnBadBreak(world, i, j, k, iMetadata, 1F);
        org.apache.logging.log4j.LogManager.getLogger("BTW-Harvest").info(
                "[BTW-DEBUG] DropComponentItemsOnBadBreak returned {}", result);
    }
    public void DropItemsIndividualy(World world, int i, int j, int k, int iIDDropped, int iPileCount, int iDamageDropped, float fChanceOfPileDrop) {
        org.apache.logging.log4j.LogManager.getLogger("BTW-Harvest").info(
                "[BTW-DEBUG] DropItemsIndividualy itemID={} count={} isRemote={}",
                iIDDropped, iPileCount, world.isRemote);
        if (!world.isRemote) {
            for (int count = 0; count < iPileCount; count++) {
                if (world.rand.nextFloat() <= fChanceOfPileDrop) {
                    ItemStack stack = new ItemStack(iIDDropped, 1, iDamageDropped);
                    org.apache.logging.log4j.LogManager.getLogger("BTW-Harvest").info(
                            "[BTW-DEBUG] dropping stack itemID={} item={}",
                            stack.itemID, stack.getItem() != null ? stack.getItem().getClass().getSimpleName() : "null");
                    dropBlockAsItem_do(world, i, j, k, stack);
                }
            }
        }
    }
    public boolean DropComponentItemsOnBadBreak(World world, int i, int j, int k, int iMetadata, float fChanceOfDrop) { return false; }
    public void DropItemsOnDestroyedByExplosion(World world, int i, int j, int k, Explosion explosion) {}

    // --- BTW-added: Dirt dug ---

    public void OnDirtDugWithImproperTool(World world, int i, int j, int k) {}
    public void OnDirtSlabDugWithImproperTool(World world, int i, int j, int k, boolean bUpsideDown) {}
    public void NotifyNeighborDirtDugWithImproperTool(World world, int i, int j, int k, int iToFacing) {}
    public void OnNeighborDirtDugWithImproperTool(World world, int i, int j, int k, int iToFacing) {}

    // --- BTW-added: Hard points ---

    public boolean HasSmallCenterHardPointToFacing(IBlockAccess blockAccess, int i, int j, int k, int iFacing, boolean bIgnoreTransparency) {
        return HasCenterHardPointToFacing(blockAccess, i, j, k, iFacing, bIgnoreTransparency);
    }
    public boolean HasSmallCenterHardPointToFacing(IBlockAccess blockAccess, int i, int j, int k, int iFacing) {
        return HasSmallCenterHardPointToFacing(blockAccess, i, j, k, iFacing, false);
    }
    public boolean HasCenterHardPointToFacing(IBlockAccess blockAccess, int i, int j, int k, int iFacing, boolean bIgnoreTransparency) {
        return HasLargeCenterHardPointToFacing(blockAccess, i, j, k, iFacing, bIgnoreTransparency);
    }
    public boolean HasCenterHardPointToFacing(IBlockAccess blockAccess, int i, int j, int k, int iFacing) {
        return HasCenterHardPointToFacing(blockAccess, i, j, k, iFacing, false);
    }
    public boolean HasLargeCenterHardPointToFacing(IBlockAccess blockAccess, int i, int j, int k, int iFacing, boolean bIgnoreTransparency) {
        return blockAccess.isBlockNormalCube(i, j, k);
    }
    public boolean HasLargeCenterHardPointToFacing(IBlockAccess blockAccess, int i, int j, int k, int iFacing) {
        return HasLargeCenterHardPointToFacing(blockAccess, i, j, k, iFacing, false);
    }

    // --- BTW-added: Block resting/attachment ---

    public boolean IsBlockRestingOnThatBelow(IBlockAccess blockAccess, int i, int j, int k) { return false; }
    public boolean IsBlockAttachedToFacing(IBlockAccess blockAccess, int i, int j, int k, int iFacing) { return false; }
    public void AttachToFacing(World world, int i, int j, int k, int iFacing) {}

    // --- BTW-added: Contact points ---

    public boolean HasContactPointToFullFace(IBlockAccess blockAccess, int i, int j, int k, int iFacing) { return false; }
    public boolean HasContactPointToSlabSideFace(IBlockAccess blockAccess, int i, int j, int k, int iFacing, boolean bIsSlabUpsideDown) { return false; }
    public boolean HasContactPointToStairShapedFace(IBlockAccess blockAccess, int i, int j, int k, int iFacing) { return false; }
    public boolean HasContactPointToStairNarrowVerticalFace(IBlockAccess blockAccess, int i, int j, int k, int iFacing, int iStairFacing) { return false; }

    // --- BTW-added: Mortar ---

    public boolean OnMortarApplied(World world, int i, int j, int k) { return false; }
    public boolean HasMortar(IBlockAccess blockAccess, int i, int j, int k) { return false; }
    public boolean HasNeighborWithMortarInContact(World world, int i, int j, int k) { return false; }

    // --- BTW-added: Snow sticky ---

    public boolean IsStickyToSnow(IBlockAccess blockAccess, int i, int j, int k) { return false; }
    public boolean HasStickySnowNeighborInContact(World world, int i, int j, int k) { return false; }

    // --- BTW-added: Furnace burn time ---

    public int GetFurnaceBurnTime(int iItemDamage) { return furnaceBurnTime; }
    public void SetFurnaceBurnTime(int iBurnTime) { this.furnaceBurnTime = iBurnTime; }
    public void SetFurnaceBurnTime(FCEnumFurnaceBurnTime burnTimeEnum) { this.furnaceBurnTime = burnTimeEnum.m_iBurnTime; }

    // --- BTW-added: Fire ---

    public boolean DoesInfiniteBurnToFacing(IBlockAccess blockAccess, int i, int j, int k, int iFacing) { return false; }
    public boolean DoesExtinguishFireAbove(World world, int i, int j, int k) { return false; }
    public void OnDestroyedByFire(World world, int i, int j, int k, int iFireAge, boolean bForcedFireSpread) {}
    public Block SetFireProperties(int iChanceToEncourageFire, int iAbilityToCatchFire) { this.fireEncouragement = iChanceToEncourageFire; this.flammability = iAbilityToCatchFire; return this; }
    public Block SetFireProperties(FCEnumFlammability flammabilityEnum) { return SetFireProperties(flammabilityEnum.m_iChanceToEncourageFire, flammabilityEnum.m_iAbilityToCatchFire); }
    public boolean GetCanBeSetOnFireDirectly(IBlockAccess blockAccess, int i, int j, int k) { return false; }
    public boolean GetCanBeSetOnFireDirectlyByItem(IBlockAccess blockAccess, int i, int j, int k) {
        return GetCanBeSetOnFireDirectly(blockAccess, i, j, k);
    }
    public boolean SetOnFireDirectly(World world, int i, int j, int k) { return false; }
    public int GetChanceOfFireSpreadingDirectlyTo(IBlockAccess blockAccess, int i, int j, int k) { return 0; }
    public boolean GetCanBlockLightItemOnFire(IBlockAccess blockAccess, int i, int j, int k) { return false; }
    public boolean GetDoesFireDamageToEntities(World world, int i, int j, int k, Entity entity) { return false; }
    public boolean GetDoesFireDamageToEntities(World world, int i, int j, int k) { return false; }
    public boolean GetCanBlockBeIncinerated(World world, int i, int j, int k) { return false; }
    public boolean GetCanBlockBeReplacedByFire(World world, int i, int j, int k) { return false; }
    public boolean IsIncineratedInCrucible() { return false; }

    // --- BTW-added: Pathing ---

    public boolean CanPathThroughBlock(IBlockAccess blockAccess, int i, int j, int k, Entity entity, PathFinder pathFinder) { return true; }
    public boolean ShouldOffsetPositionIfPathingOutOf(IBlockAccess blockAccess, int i, int j, int k, Entity entity, PathFinder pathFinder) { return false; }
    public int GetWeightOnPathBlocked(IBlockAccess blockAccess, int i, int j, int k) { return 0; }
    public int AdjustPathWeightOnNotBlocked(int iPreviousWeight) { return iPreviousWeight; }

    // --- BTW-added: Kiln ---

    public Block SetCanBeCookedByKiln(boolean bCanBeCooked) { this.canBeCookedByKiln = bCanBeCooked; return this; }
    public boolean GetCanBeCookedByKiln(IBlockAccess blockAccess, int i, int j, int k) { return canBeCookedByKiln; }
    public int GetCookTimeMultiplierInKiln(IBlockAccess blockAccess, int i, int j, int k) { return 1; }
    public Block SetItemIndexDroppedWhenCookedByKiln(int iItemIndex) { this.kilnDropItemIndex = iItemIndex; return this; }
    public int GetItemIndexDroppedWhenCookedByKiln(IBlockAccess blockAccess, int i, int j, int k) { return kilnDropItemIndex; }
    public Block SetItemDamageDroppedWhenCookedByKiln(int iItemDamage) { this.kilnDropItemDamage = iItemDamage; return this; }
    public int GetItemDamageDroppedWhenCookedByKiln(IBlockAccess blockAccess, int i, int j, int k) { return kilnDropItemDamage; }
    public void OnCookedByKiln(World world, int i, int j, int k) {}

    // --- BTW-added: Saw ---

    public boolean DoesBlockBreakSaw(World world, int i, int j, int k) { return false; }
    public boolean OnBlockSawed(World world, int i, int j, int k, int iSawPosI, int iSawPosJ, int iSawPosK) { return OnBlockSawed(world, i, j, k); }
    public boolean OnBlockSawed(World world, int i, int j, int k) { return false; }
    public int GetItemIDDroppedOnSaw(World world, int i, int j, int k) { return -1; }
    public int GetItemCountDroppedOnSaw(World world, int i, int j, int k) { return 0; }
    public int GetItemDamageDroppedOnSaw(World world, int i, int j, int k) { return 0; }
    public boolean DoesBlockDropAsItemOnSaw(World world, int i, int j, int k) { return true; }

    // --- BTW-added: Mechanical power ---

    public int GetMechanicalPowerLevelProvidedToAxleAtFacing(World world, int i, int j, int k, int iFacing) { return 0; }
    public boolean CanOutputMechanicalPower() { return false; }
    public boolean CanInputMechanicalPower() { return false; }
    public boolean IsOutputtingMechanicalPower(World world, int i, int j, int k) { return false; }
    public boolean IsInputtingMechanicalPower(World world, int i, int j, int k) { return false; }
    public void Overpower(World world, int i, int j, int k) {}

    // --- BTW-added: Convert block ---

    public boolean CanConvertBlock(ItemStack stack, World world, int i, int j, int k) { return false; }
    public boolean ConvertBlock(ItemStack stack, World world, int i, int j, int k, int iFromSide) { return false; }

    // --- BTW-added: Tools stick in block ---

    public boolean CanToolsStickInBlock(IBlockAccess blockAccess, int i, int j, int k) { return true; }

    // --- BTW-added: Buoyancy ---

    public Block SetBuoyancy(float fBuoyancy) { this.buoyancy = fBuoyancy; return this; }
    public Block SetBuoyant() { this.buoyancy = 1.0F; return this; }
    public Block SetNonBuoyant() { this.buoyancy = -1.0F; return this; }
    public Block SetNeutralBuoyant() { this.buoyancy = 0.0F; return this; }
    public float GetBuoyancy(int iMetadata) { return buoyancy; }

    // --- BTW-added: Ground cover ---

    public boolean CanGroundCoverRestOnBlock(World world, int i, int j, int k) { return false; }
    public float GroundCoverRestingOnVisualOffset(IBlockAccess blockAccess, int i, int j, int k) { return 0F; }

    // --- BTW-added: Grass spreading ---

    public boolean AttempToSpreadGrassToBlock(World world, int i, int j, int k) { return false; }
    public boolean GetCanGrassSpreadToBlock(World world, int i, int j, int k) { return false; }
    public boolean SpreadGrassToBlock(World world, int i, int j, int k) { return false; }
    public boolean GetCanGrassGrowUnderBlock(World world, int i, int j, int k, boolean bGrassOnHalfSlab) { return true; }

    // --- BTW-added: Mycelium spreading ---

    public boolean AttempToSpreadMyceliumToBlock(World world, int i, int j, int k) { return false; }
    public boolean GetCanMyceliumSpreadToBlock(World world, int i, int j, int k) { return false; }
    public boolean SpreadMyceliumToBlock(World world, int i, int j, int k) { return false; }
    public boolean GetCanBlightSpreadToBlock(World world, int i, int j, int k, int iBlightLevel) { return false; }

    // --- BTW-added: Snow covering ---

    public boolean IsSnowCoveringTopSurface(IBlockAccess blockAccess, int i, int j, int k) { return false; }

    // --- BTW-added: Piston ---

    public int OnPreBlockPlacedByPiston(World world, int i, int j, int k, int iMetadata, int iDirectionMoved) { return iMetadata; }
    public boolean CanBlockBePulledByPiston(World world, int i, int j, int k, int iToFacing) { return false; }
    public boolean CanBlockBePushedByPiston(World world, int i, int j, int k, int iToFacing) { return true; }
    public boolean CanBePistonShoveled(World world, int i, int j, int k) { return false; }
    public int GetPistonShovelEjectDirection(World world, int i, int j, int k, int iToFacing) { return -1; }
    public AxisAlignedBB GetAsPistonMovingBoundingBox(World world, int i, int j, int k) { return null; }
    public int AdjustMetadataForPistonMove(int iMetadata) { return iMetadata; }
    public boolean CanContainPistonPackingToFacing(World world, int i, int j, int k, int iFacing) { return false; }
    public boolean IsPistonPackable(ItemStack stack) { return false; }
    public int GetRequiredItemCountToPistonPack(ItemStack stack) { return 0; }
    public int GetResultingBlockIDOnPistonPack(ItemStack stack) { return 0; }
    public int GetResultingBlockMetadataOnPistonPack(ItemStack stack) { return 0; }
    public void OnBrokenByPistonPush(World world, int i, int j, int k, int iMetadata) {}

    // --- BTW-added: Hopper filtering ---

    public boolean CanItemPassIfFilter(ItemStack filteredItem) { return true; }
    public int GetFilterableProperties(ItemStack stack) { return 0; }
    public void SetFilterableProperties(int iProperties) {}
    public boolean CanTransformItemIfFilter(ItemStack filteredItem) { return false; }

    // --- BTW-added: Falling block ---

    public boolean IsFallingBlock() { return false; }

    // --- BTW-added: Static facing helpers ---

    public static int GetOppositeFacing(int iFacing) { return iFacing ^ 1; }
    public static int RotateFacingAroundJ(int iFacing, boolean bReverse) { return iFacing; }
    public static int CycleFacing(int iFacing, boolean bReverse) { return iFacing; }

    // --- BTW-added: Conversion/combined block ---

    public int GetCombinedBlockID(int iMetadata) { return 0; }
    public int IdDroppedOnConversion(int iMetadata) { return blockID; }

    // --- BTW-added: Growth ---

    public boolean CanGrowOnBlock(World world, int i, int j, int k) { return false; }
    public float GetBaseGrowthChance(World world, int i, int j, int k) { return 0F; }
    public void IncrementGrowthLevel(World world, int i, int j, int k) {}
    public void SetFertilized(World world, int i, int j, int k) {}
    public boolean IsFertilized(IBlockAccess blockAccess, int i, int j, int k) { return false; }
    public boolean IsHydrated(int iMetadata) { return false; }
    public int SetFullyHydrated(int iMetadata) { return iMetadata; }
    public void DryIncrementally(World world, int i, int j, int k) {}
    public int GetRequiredToolLevelForOre(IBlockAccess blockAccess, int i, int j, int k) { return 0; }

    // --- BTW-added: Misc block features ---

    public boolean CanDropFromExplosion(Explosion explosion) { return true; }
    public boolean GetCanCreatureSpawnOnBlock(World world, int i, int j, int k) { return true; }
    public boolean IsValidZombieSecondaryTarget(EntityZombie zombie) { return false; }
    public boolean GetCanCreatureTypeBePossessed() { return false; }
    public void CheckForScrollDrop() {}

    public static boolean InstallationIntegrityTest() { return true; }
    public static int determineOrientation(World world, int x, int y, int z, EntityLiving entity) {
        if (entity != null) {
            if (Math.abs(entity.posX - (double)x - 0.5) < 2.0 && Math.abs(entity.posZ - (double)z - 0.5) < 2.0) {
                double eyeHeight = entity.posY + 1.82;
                if (eyeHeight - (double)y > 2.0) return 1;
                if ((double)y - eyeHeight > 0.0) return 0;
            }
            int facing = MathHelper.floor_double((double)(entity.rotationYaw * 4.0F / 360.0F) + 0.5) & 3;
            return facing == 0 ? 2 : facing == 1 ? 5 : facing == 2 ? 3 : 4;
        }
        return 0;
    }

    public boolean tryToCreatePortal(World world, int x, int y, int z) { return false; }
    public boolean graphicsLevel;

    /**
     * Populates vanilla block static fields. Called during Forge mod init
     * BEFORE FC code runs. FC code will overwrite many of these with FC subclasses.
     * Uses a concrete inner class since Block itself is abstract.
     */
    public static void initializeVanillaBlocks() {
        // Minimal placeholders so Block.stone/grass/etc. static fields are
        // non-null before FC classes load.  BTWLifecycle.replaceVanillaBlocksWithFc()
        // replaces every entry in blocksList[] with the real FC subclass which
        // sets hardness, resistance, step sounds, and all other properties.

        stone = new ConcreteBlock(1, Material.rock);
        grass = (BlockGrass) new ConcreteBlockGrass(2);
        dirt = new ConcreteBlock(3, Material.ground);
        cobblestone = new ConcreteBlock(4, Material.rock);
        planks = new ConcreteBlock(5, Material.wood);
        sapling = new ConcreteBlock(6, Material.plants);
        bedrock = new ConcreteBlock(7, Material.rock);
        waterMoving = new ConcreteBlockFluid(8, Material.water);
        waterStill = new ConcreteBlock(9, Material.water);
        lavaMoving = new ConcreteBlockFluid(10, Material.lava);
        lavaStill = new ConcreteBlock(11, Material.lava);
        sand = new ConcreteBlock(12, Material.sand);
        gravel = new ConcreteBlock(13, Material.sand);
        oreGold = new ConcreteBlock(14, Material.rock);
        oreIron = new ConcreteBlock(15, Material.rock);
        oreCoal = new ConcreteBlock(16, Material.rock);
        wood = new ConcreteBlock(17, Material.wood);
        leaves = new ConcreteBlockLeaves(18);
        sponge = new ConcreteBlock(19, Material.sponge);
        glass = new ConcreteBlock(20, Material.glass);
        oreLapis = new ConcreteBlock(21, Material.rock);
        blockLapis = new ConcreteBlock(22, Material.rock);
        dispenser = new ConcreteBlock(23, Material.rock);
        sandStone = new ConcreteBlock(24, Material.rock);
        music = new ConcreteBlock(25, Material.wood);
        bed = new ConcreteBlock(26, Material.cloth);
        railPowered = new ConcreteBlock(27, Material.circuits);
        railDetector = new ConcreteBlock(28, Material.circuits);
        pistonStickyBase = new ConcreteBlockPistonBase(29, true);
        web = new ConcreteBlock(30, Material.web);
        tallGrass = new ConcreteBlockTallGrass(31);
        pistonBase = new ConcreteBlockPistonBase(33, false);
        pistonExtension = new ConcreteBlockPistonExtension(34);
        cloth = new ConcreteBlock(35, Material.cloth);
        pistonMoving = new ConcreteBlockPistonMoving(36);
        plantYellow = new ConcreteBlockFlower(37, Material.plants);
        plantRed = new ConcreteBlockFlower(38, Material.plants);
        mushroomBrown = new ConcreteBlockFlower(39, Material.plants);
        mushroomRed = new ConcreteBlockFlower(40, Material.plants);
        blockGold = new ConcreteBlock(41, Material.iron);
        blockIron = new ConcreteBlock(42, Material.iron);
        stoneDoubleSlab = new ConcreteBlockHalfSlab(43, true);
        stoneSingleSlab = new ConcreteBlockHalfSlab(44, false);
        brick = new ConcreteBlock(45, Material.rock);
        tnt = new ConcreteBlock(46, Material.tnt);
        bookShelf = new ConcreteBlock(47, Material.wood);
        cobblestoneMossy = new ConcreteBlock(48, Material.rock);
        obsidian = new ConcreteBlock(49, Material.rock);
        torchWood = new ConcreteBlock(50, Material.circuits);
        fire = new ConcreteBlockFire(51);
        mobSpawner = new ConcreteBlock(52, Material.rock);
        stairsWoodOak = new ConcreteBlock(53, Material.wood);
        chest = new ConcreteBlockChest(54);
        redstoneWire = new ConcreteBlockRedstoneWire(55);
        oreDiamond = new ConcreteBlock(56, Material.rock);
        blockDiamond = new ConcreteBlock(57, Material.iron);
        workbench = new ConcreteBlock(58, Material.wood);
        crops = new ConcreteBlock(59, Material.plants);
        tilledField = new ConcreteBlock(60, Material.ground);
        furnaceIdle = new ConcreteBlock(61, Material.rock);
        furnaceBurning = new ConcreteBlock(62, Material.rock);
        signPost = new ConcreteBlock(63, Material.wood);
        doorWood = new ConcreteBlock(64, Material.wood);
        ladder = new ConcreteBlock(65, Material.circuits);
        rail = new ConcreteBlock(66, Material.circuits);
        stairsCobblestone = new ConcreteBlock(67, Material.rock);
        signWall = new ConcreteBlock(68, Material.wood);
        lever = new ConcreteBlock(69, Material.circuits);
        pressurePlateStone = new ConcreteBlock(70, Material.rock);
        doorIron = new ConcreteBlock(71, Material.iron);
        pressurePlatePlanks = new ConcreteBlock(72, Material.wood);
        oreRedstone = new ConcreteBlock(73, Material.rock);
        oreRedstoneGlowing = new ConcreteBlock(74, Material.rock);
        torchRedstoneIdle = new ConcreteBlock(75, Material.circuits);
        torchRedstoneActive = new ConcreteBlock(76, Material.circuits);
        stoneButton = new ConcreteBlock(77, Material.circuits);
        snow = new ConcreteBlock(78, Material.snow);
        ice = new ConcreteBlock(79, Material.ice);
        blockSnow = new ConcreteBlock(80, Material.craftedSnow);
        cactus = new ConcreteBlock(81, Material.cactus);
        blockClay = new ConcreteBlock(82, Material.clay);
        reed = new ConcreteBlock(83, Material.plants);
        jukebox = new ConcreteBlock(84, Material.wood);
        fence = new ConcreteBlock(85, Material.wood);
        pumpkin = new ConcreteBlock(86, Material.pumpkin);
        netherrack = new ConcreteBlock(87, Material.rock);
        slowSand = new ConcreteBlock(88, Material.sand);
        glowStone = new ConcreteBlock(89, Material.glass);
        portal = new ConcreteBlockPortal(90);
        pumpkinLantern = new ConcreteBlock(91, Material.pumpkin);
        redstoneRepeaterIdle = new ConcreteBlockRedstoneRepeater(93, false);
        trapdoor = new ConcreteBlock(96, Material.wood);
        silverfish = new ConcreteBlock(97, Material.clay);
        stoneBrick = new ConcreteBlock(98, Material.rock);
        fenceIron = new ConcreteBlock(101, Material.iron);
        thinGlass = new ConcreteBlock(102, Material.glass);
        melon = new ConcreteBlock(103, Material.pumpkin);
        pumpkinStem = new ConcreteBlock(104, Material.plants);
        melonStem = new ConcreteBlock(105, Material.plants);
        vine = new ConcreteBlock(106, Material.vine);
        fenceGate = new ConcreteBlock(107, Material.wood);
        stairsBrick = new ConcreteBlock(108, Material.rock);
        stairsStoneBrick = new ConcreteBlock(109, Material.rock);
        mycelium = new ConcreteBlockMycelium(110);
        waterlily = new ConcreteBlock(111, Material.plants);
        netherBrick = new ConcreteBlock(112, Material.rock);
        netherFence = new ConcreteBlock(113, Material.rock);
        stairsNetherBrick = new ConcreteBlock(114, Material.rock);
        netherStalk = new ConcreteBlock(115, Material.plants);
        enchantmentTable = new ConcreteBlock(116, Material.rock);
        endPortal = new ConcreteBlock(119, Material.portal);
        endPortalFrame = new ConcreteBlock(120, Material.rock);
        whiteStone = new ConcreteBlock(121, Material.rock);
        redstoneLampIdle = new ConcreteBlock(123, Material.redstoneLight);
        redstoneLampActive = new ConcreteBlock(124, Material.redstoneLight);
        woodDoubleSlab = new ConcreteBlockHalfSlab(125, true);
        woodSingleSlab = new ConcreteBlockHalfSlab(126, false);
        cocoaPlant = new ConcreteBlock(127, Material.plants);
        stairsSandStone = new ConcreteBlock(128, Material.rock);
        oreEmerald = new ConcreteBlock(129, Material.rock);
        tripWireSource = new ConcreteBlockTripWireSource(131);
        tripWire = new ConcreteBlock(132, Material.circuits);
        blockEmerald = new ConcreteBlock(133, Material.iron);
        stairsWoodSpruce = new ConcreteBlock(134, Material.wood);
        stairsWoodBirch = new ConcreteBlock(135, Material.wood);
        stairsWoodJungle = new ConcreteBlock(136, Material.wood);
        beacon = new ConcreteBlockBeacon(138);
        carrot = new ConcreteBlock(141, Material.plants);
        potato = new ConcreteBlock(142, Material.plants);
        woodenButton = new ConcreteBlock(143, Material.circuits);
        skull = new ConcreteBlock(144, Material.circuits);
        anvil = new ConcreteBlock(145, Material.anvil);
        chestTrapped = new ConcreteBlock(146, Material.wood);
        pressurePlateGold = new ConcreteBlock(147, Material.iron);
        pressurePlateIron = new ConcreteBlock(148, Material.iron);
        daylightSensor = new ConcreteBlockDaylightDetector(151);
        oreNetherQuartz = new ConcreteBlock(153, Material.rock);
        hopperBlock = new ConcreteBlockHopper(154);
        blockNetherQuartz = new ConcreteBlock(155, Material.rock);
        stairsNetherQuartz = new ConcreteBlock(156, Material.rock);
        railActivator = new ConcreteBlock(157, Material.circuits);
        dropper = new ConcreteBlock(158, Material.rock);

        // Create ItemBlock entries for all blocks (vanilla pattern: Item.itemsList[blockID] = ItemBlock)
        for (int id = 0; id < 256; id++) {
            if (blocksList[id] != null && Item.itemsList[id] == null) {
                Item.itemsList[id] = new ItemBlock(id - 256);
            }
        }
    }

    // Concrete subclass since Block is abstract
    private static class ConcreteBlock extends Block {
        ConcreteBlock(int id, Material mat) { super(id, mat); }
    }
    private static class ConcreteBlockGrass extends BlockGrass {
        ConcreteBlockGrass(int id) { super(id); }
    }
    private static class ConcreteBlockFluid extends BlockFluid {
        ConcreteBlockFluid(int id, Material mat) { super(id, mat); }
    }
    private static class ConcreteBlockFlower extends BlockFlower {
        ConcreteBlockFlower(int id, Material mat) { super(id, mat); }
    }
    private static class ConcreteBlockChest extends BlockChest {
        ConcreteBlockChest(int id) { super(id, 0); }
    }
    private static class ConcreteBlockRedstoneWire extends BlockRedstoneWire {
        ConcreteBlockRedstoneWire(int id) { super(id); }
    }
    private static class ConcreteBlockHalfSlab extends BlockHalfSlab {
        ConcreteBlockHalfSlab(int id, boolean isDouble) { super(id, isDouble, Material.rock); }
        public String getFullSlabName(int meta) { return ""; }
    }
    private static class ConcreteBlockLeaves extends BlockLeaves {
        ConcreteBlockLeaves(int id) { super(id); }
    }
    private static class ConcreteBlockTallGrass extends BlockTallGrass {
        ConcreteBlockTallGrass(int id) { super(id); }
    }
    private static class ConcreteBlockPortal extends BlockPortal {
        ConcreteBlockPortal(int id) { super(id); }
    }
    private static class ConcreteBlockFire extends BlockFire {
        ConcreteBlockFire(int id) { super(id); }
    }
    private static class ConcreteBlockBeacon extends BlockBeacon {
        ConcreteBlockBeacon(int id) { super(id); }
    }
    private static class ConcreteBlockPistonBase extends BlockPistonBase {
        ConcreteBlockPistonBase(int id, boolean isSticky) { super(id, isSticky); }
    }
    private static class ConcreteBlockPistonExtension extends BlockPistonExtension {
        ConcreteBlockPistonExtension(int id) { super(id); }
    }
    private static class ConcreteBlockPistonMoving extends BlockPistonMoving {
        ConcreteBlockPistonMoving(int id) { super(id); }
    }
    private static class ConcreteBlockRedstoneRepeater extends BlockRedstoneRepeater {
        ConcreteBlockRedstoneRepeater(int id, boolean powered) { super(id, powered); }
    }
    private static class ConcreteBlockMycelium extends BlockMycelium {
        ConcreteBlockMycelium(int id) { super(id); }
    }
    private static class ConcreteBlockDaylightDetector extends BlockDaylightDetector {
        ConcreteBlockDaylightDetector(int id) { super(id); }
    }
    private static class ConcreteBlockHopper extends BlockHopper {
        ConcreteBlockHopper(int id) { super(id); }
    }
    private static class ConcreteBlockTripWireSource extends BlockTripWireSource {
        ConcreteBlockTripWireSource(int id) { super(id); }
    }
}
