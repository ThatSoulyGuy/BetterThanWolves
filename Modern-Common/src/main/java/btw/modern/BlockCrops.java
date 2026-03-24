package btw.modern;

public class BlockCrops extends BlockFlower {

    protected BlockCrops(int id) {
        super(id, Material.plants);

        setTickRandomly(true);
        float halfWidth = 0.5F;
        InitBlockBounds(0.5F - halfWidth, 0.0F, 0.5F - halfWidth, 0.5F + halfWidth, 0.25F, 0.5F + halfWidth);
        setCreativeTab(null);
        setHardness(0.0F);
        setStepSound(soundGrassFootstep);
        disableStats();
    }

    // Shadow remapping maps FCBlockCrops → BlockCrops.
    // FCBlockCrops overrides CanGrowOnBlock to check domesticated crops
    // instead of wild vegetation (the BlockFlower/FCBlockPlants default).
    @Override
    public boolean CanGrowOnBlock(World world, int i, int j, int k) {
        Block blockOn = Block.blocksList[world.getBlockId(i, j, k)];
        return blockOn != null && blockOn.CanDomesticatedCropsGrowOnBlock(world, i, j, k);
    }

    // --- Render overrides ---

    public int getRenderType() {
        return 6;
    }

    public boolean isOpaqueCube() {
        return false;
    }

    public boolean renderAsNormalBlock() {
        return false;
    }

    // --- Growth level accessors ---

    /**
     * Read the growth level from the world metadata at the given position.
     */
    public int GetGrowthLevel(IBlockAccess blockAccess, int x, int y, int z) {
        return GetGrowthLevel(blockAccess.getBlockMetadata(x, y, z));
    }

    /**
     * Extract the growth level from raw metadata. The low 3 bits (mask 0x7)
     * encode growth stages 0-7.
     */
    public int GetGrowthLevel(int metadata) {
        return metadata & 7;
    }



    // --- FC growth bridge (FCBlockCrops methods) ---

    /**
     * Bridge for FCBlockCrops.updateTick — called via random tick.
     * Checks if block can stay, then attempts growth if not fully grown.
     * FC subclasses (FCBlockHempCrop etc.) override AttemptToGrow.
     */
    @Override
    public void updateTick(World world, int i, int j, int k, java.util.Random rand) {
        if (UpdateIfBlockStays(world, i, j, k)) {
            if (world.provider == null || world.provider.dimensionId != 1) {
                // Always call AttemptToGrow — FC subclasses like FCBlockHempCrop
                // handle multi-block growth (top block placement) inside
                // AttemptToGrow even when the base is at max growth level.
                AttemptToGrow(world, i, j, k, rand);
            }
        }
    }

    /**
     * Bridge for FCBlockCrops.UpdateIfBlockStays — removes crop if it
     * can't stay (soil removed, light too low, etc.).
     */
    public boolean UpdateIfBlockStays(World world, int i, int j, int k) {
        if (!canBlockStay(world, i, j, k)) {
            dropBlockAsItem(world, i, j, k, world.getBlockMetadata(i, j, k), 0);
            world.setBlockToAir(i, j, k);
            return false;
        }
        return true;
    }

    /**
     * Bridge for FCBlockCrops.IsFullyGrown.
     */
    public boolean IsFullyGrown(IBlockAccess blockAccess, int i, int j, int k) {
        return IsFullyGrown(blockAccess.getBlockMetadata(i, j, k));
    }

    public boolean IsFullyGrown(int iMetadata) {
        return GetGrowthLevel(iMetadata) >= 7;
    }

    /**
     * FC subclasses override with custom growth logic (light, hydration, weeds).
     * Default: basic light-gated growth matching FCBlockCrops base behavior.
     */
    public void AttemptToGrow(World world, int i, int j, int k, java.util.Random rand) {
        if (world.getBlockLightValue(i, j + 1, k) >= GetLightLevelForGrowth()) {
            if (rand.nextFloat() <= GetBaseGrowthChance(world, i, j, k)) {
                IncrementGrowthLevel(world, i, j, k);
            }
        }
    }

    /** Bridge for FCBlockCrops.GetBaseGrowthChance. FC subclasses override (hemp=0.1). */
    public float GetBaseGrowthChance(World world, int i, int j, int k) {
        return 0.05F;
    }

    /** Bridge for FCBlockCrops.IncrementGrowthLevel. */
    public void IncrementGrowthLevel(World world, int i, int j, int k) {
        int growthLevel = GetGrowthLevel(world, i, j, k) + 1;
        SetGrowthLevel(world, i, j, k, growthLevel);
        if (IsFullyGrown(world, i, j, k)) {
            Block blockBelow = Block.blocksList[world.getBlockId(i, j - 1, k)];
            if (blockBelow != null) {
                blockBelow.NotifyOfFullStagePlantGrowthOn(world, i, j - 1, k, this);
            }
        }
    }

    /** Bridge for FCBlockCrops.SetGrowthLevel. */
    public void SetGrowthLevel(World world, int i, int j, int k, int level) {
        int metadata = world.getBlockMetadata(i, j, k) & (~7);
        world.setBlockMetadataWithNotify(i, j, k, metadata | level);
    }

    /** Bridge for FCBlockCrops.SetGrowthLevelNoNotify. */
    public void SetGrowthLevelNoNotify(World world, int x, int y, int z, int level) {
        int metadata = world.getBlockMetadata(x, y, z) & (~7);
        world.setBlockMetadata(x, y, z, metadata | level);
    }

    /** Bridge for FCBlockCrops.GetLightLevelForGrowth. */
    public int GetLightLevelForGrowth() {
        return 11;
    }

    /** Bridge for FCBlockCrops.fertilize. */
    public void fertilize(World world, int i, int j, int k) {
        int newLevel = world.getBlockMetadata(i, j, k)
            + MathHelper.getRandomIntegerInRange(world.rand, 2, 5);
        if (newLevel > 7) newLevel = 7;
        world.setBlockMetadataWithNotify(i, j, k, newLevel);
    }

    // --- FC drop bridge ---

    /**
     * Bridge for FCBlockCrops.dropBlockAsItemWithChance.
     * Calls idDropped to determine if something should drop — FC subclasses
     * (FCBlockHempCrop) override idDropped with their own metadata checks
     * (e.g. metadata >= 7 for hemp, which covers both base at 7 and top at 8+).
     * DropSeeds is called when the crop yields an item.
     */
    @Override
    public void dropBlockAsItemWithChance(World world, int x, int y, int z, int metadata,
            float chance, int fortune) {
        if (!world.isRemote) {
            int dropId = idDropped(metadata, world.rand, fortune);
            if (dropId > 0) {
                super.dropBlockAsItemWithChance(world, x, y, z, metadata, chance, 0);
                DropSeeds(world, x, y, z, metadata, chance, fortune);
            }
        }
    }

    // --- Seed / crop item IDs ---

    /**
     * Return the item ID for the seed dropped by this crop.
     * Override point: default returns wheat seeds (295 = Item.seeds.itemID).
     */
    public int getSeedItem() {
        return Item.seeds.itemID;
    }

    /**
     * Return the item ID for the crop product.
     * Override point: default returns wheat (296 = Item.wheat.itemID).
     */
    public int getCropItem() {
        return Item.wheat.itemID;
    }

    // --- Seed dropping ---

    /**
     * Drop seed items when a fully-grown crop is broken.
     * FC subclasses override this with fortune-aware logic.
     */
    public void DropSeeds(World world, int i, int j, int k, int iMetadata, float fChance) {
        DropSeeds(world, i, j, k, iMetadata, fChance, 0);
    }

    /**
     * Drop seed items when a fully-grown crop is broken (fortune-aware variant).
     * Default drops one seed, with a chance of a second seed.
     */
    public void DropSeeds(World world, int i, int j, int k, int iMetadata, float fChance, int iFortuneModifier) {
        dropBlockAsItem_do(world, i, j, k, new ItemStack(getSeedItem(), 1, 0));

        if (world.rand.nextInt(16) - iFortuneModifier < 4) {
            dropBlockAsItem_do(world, i, j, k, new ItemStack(getSeedItem(), 1, 0));
        }
    }

    // --- Metadata / plant mapping ---

    /**
     * Return the plant (block) icon index for a given metadata value.
     * Override point for subclasses with custom texture mappings.
     */
    public int getPlantForMeta(int meta) {
        return meta & 7;
    }

    /**
     * Return the metadata value to use when placing a crop from the given stack.
     * Override point for subclasses.
     */
    public int getMetaForPlant(ItemStack stack) {
        return 0;
    }
}
