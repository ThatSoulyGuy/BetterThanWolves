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

    // 1.5.2 BlockCrops.CanGrowOnBlock (FCMOD, vanilla/server BlockCrops.java:267-273): crops need soil that
    // takes domesticated crops, not the wild-vegetation default of BlockFlower/FCBlockPlants.
    @Override
    public boolean CanGrowOnBlock(World world, int i, int j, int k) {
        Block blockOn = Block.blocksList[world.getBlockId(i, j, k)];
        return blockOn != null && blockOn.CanDomesticatedCropsGrowOnBlock(world, i, j, k);
    }

    // 1.5.2 BlockCrops.CanWeedsGrowInBlock (FCMOD, vanilla/server BlockCrops.java:275-279): FC farmland weeds
    // grow in among crops (FCBlockFarmland.CanWeedsShareSpaceWithBlockAt), which is what makes
    // GetWeedsGrowthLevel stop AttemptToGrow.
    @Override
    public boolean CanWeedsGrowInBlock(IBlockAccess blockAccess, int i, int j, int k) {
        return true;
    }

    // --- Render overrides ---

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockCrops' icon members (vanilla/client BlockCrops.java:21,
    // :155-163, :250-258): crops_<stage>. FCBlockWheatLegacy (59) inherits both; BlockCarrot, BlockPotato and
    // FCBlockHempCrop answer their own.
    private Icon[] iconArray;

    // 1.5.2 BlockCrops.getIcon (vanilla/client BlockCrops.java:155-163); Block's icon until registerIcons runs (client
    // only)
    @Override
    public Icon getIcon(int side, int metadata) {
        if (this.iconArray == null) {
            return super.getIcon(side, metadata);
        }
        if (metadata < 0 || metadata > 7) {
            metadata = 7;
        }
        return this.iconArray[metadata];
    }

    // 1.5.2 BlockCrops.registerIcons (vanilla/client BlockCrops.java:250-258)
    @Override
    public void registerIcons(IconRegister register) {
        Icon[] icons = new Icon[8];
        for (int i = 0; i < icons.length; ++i) {
            icons[i] = register.registerIcon("crops_" + i);
        }
        this.iconArray = icons;
    }

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

    // --- 1.5.2 BlockCrops growth (FCMOD, vanilla/server BlockCrops.java:252-360) ---

    /**
     * 1.5.2 BlockCrops.updateTick (vanilla/server BlockCrops.java:252-265): BlockFlower.updateTick drops the crop
     * if it can no longer stay (checkFlowerChange), then, outside the End and if the crop is still there,
     * AttemptToGrow. FCBlockHempCrop overrides AttemptToGrow (it also grows its top block at level 7).
     * Port guard: a world without a provider counts as not the End.
     */
    @Override
    public void updateTick(World world, int i, int j, int k, java.util.Random rand) {
        super.updateTick(world, i, j, k, rand);

        // make sure parent update didn't destroy the block, and prevent crops growing in the end
        // note that CanGrowOnBlock() has already been confirmed in parent method
        if ((world.provider == null || world.provider.dimensionId != 1) &&
            world.getBlockId(i, j, k) == blockID) {
            AttemptToGrow(world, i, j, k, rand);
        }
    }

    /**
     * 1.5.2 BlockCrops.AttemptToGrow (vanilla/server BlockCrops.java:283-303): no weeds in the soil, not fully
     * grown, light >= 9 above (with the time of day), hydrated soil, then a GetBaseGrowthChance roll scaled by
     * the soil's multiplier (fertilized soils).
     */
    public void AttemptToGrow(World world, int i, int j, int k, java.util.Random rand) {
        if (GetWeedsGrowthLevel(world, i, j, k) == 0 &&
            GetGrowthLevel(world, i, j, k) < 7 &&
            world.getBlockLightValue(i, j + 1, k) >= 9) {
            Block blockBelow = Block.blocksList[world.getBlockId(i, j - 1, k)];

            if (blockBelow != null &&
                blockBelow.IsBlockHydratedForPlantGrowthOn(world, i, j - 1, k)) {
                float fGrowthChance = GetBaseGrowthChance(world, i, j, k) *
                    blockBelow.GetPlantGrowthOnMultiplier(world, i, j - 1, k, this);

                if (rand.nextFloat() <= fGrowthChance) {
                    IncrementGrowthLevel(world, i, j, k);
                }
            }
        }
    }

    /** 1.5.2 BlockCrops.GetBaseGrowthChance (vanilla/server BlockCrops.java:316-319). Carrots and potatoes: 0.1. */
    public float GetBaseGrowthChance(World world, int i, int j, int k) {
        return 0.05F;
    }

    /**
     * 1.5.2 BlockCrops.IncrementGrowthLevel (vanilla/server BlockCrops.java:321-336): at level 7 the soil hears
     * about the full-grown plant. FCBlockCarrot / FCBlockPotato call this only for visible levels.
     */
    public void IncrementGrowthLevel(World world, int i, int j, int k) {
        int iGrowthLevel = GetGrowthLevel(world, i, j, k) + 1;

        SetGrowthLevel(world, i, j, k, iGrowthLevel);

        if (iGrowthLevel == 7) {
            Block blockBelow = Block.blocksList[world.getBlockId(i, j - 1, k)];

            if (blockBelow != null) {
                blockBelow.NotifyOfFullStagePlantGrowthOn(world, i, j - 1, k, this);
            }
        }
    }

    /** 1.5.2 BlockCrops.SetGrowthLevel (vanilla/server BlockCrops.java:348-353). */
    public void SetGrowthLevel(World world, int i, int j, int k, int iLevel) {
        int iMetadata = world.getBlockMetadata(i, j, k) & (~7); // filter out old level

        world.setBlockMetadataWithNotify(i, j, k, iMetadata | iLevel);
    }

    /** 1.5.2 BlockCrops.SetGrowthLevelNoNotify (vanilla/server BlockCrops.java:355-360). */
    public void SetGrowthLevelNoNotify(World world, int i, int j, int k, int iLevel) {
        int iMetadata = world.getBlockMetadata(i, j, k) & (~7); // filter out old level

        world.setBlockMetadata(i, j, k, iMetadata | iLevel);
    }

    /** 1.5.2 BlockCrops.fertilize (vanilla/server BlockCrops.java:79-89): flag 2, clients only. */
    public void fertilize(World world, int i, int j, int k) {
        int newLevel = world.getBlockMetadata(i, j, k)
            + MathHelper.getRandomIntegerInRange(world.rand, 2, 5);
        if (newLevel > 7) newLevel = 7;
        world.setBlockMetadata(i, j, k, newLevel, 2);
    }

    // --- 1.5.2 BlockCrops drops (vanilla/server BlockCrops.java:157-232) ---

    /**
     * 1.5.2 BlockCrops.dropBlockAsItemWithChance (vanilla/server BlockCrops.java:176-200): the produce from idDropped
     * (no fortune), then, for a fully grown crop only, DropSeeds. An unripe crop drops nothing, whether it is broken
     * or popped by checkFlowerChange. FCBlockHempCrop overrides idDropped (level 7 and its top block) and DropSeeds.
     */
    @Override
    public void dropBlockAsItemWithChance(World world, int x, int y, int z, int metadata,
            float chance, int fortune) {
        if (ModernTwins.isTwin(blockID)) {
            // FCBlockCarrot / FCBlockWheatLegacy twins stand for beetroots, torchflower and pitcher crops: the live
            // crop's 1.20.1 loot (Block.dropBlockAsItemWithChance), never carrots or wheat seeds.
            super.dropBlockAsItemWithChance(world, x, y, z, metadata, chance, fortune);
            return;
        }
        super.dropBlockAsItemWithChance(world, x, y, z, metadata, chance, 0);

        if (!world.isRemote) {
            if (metadata >= 7) {
                DropSeeds(world, x, y, z, metadata, chance, fortune);
            }
        }
    }

    /** 1.5.2 BlockCrops.idDropped (FCMOD, vanilla/server BlockCrops.java:222-232): the produce at level 7 only. */
    @Override
    public int idDropped(int metadata, java.util.Random random, int fortune) {
        if (metadata == 7) {
            return getCropItem();
        }

        return 0;
    }

    /** 1.5.2 BlockCrops.quantityDropped (vanilla/server BlockCrops.java:217-220). */
    @Override
    public int quantityDropped(java.util.Random random) {
        return 1;
    }

    // --- Seed / crop item IDs ---

    /**
     * 1.5.2 BlockCrops.getSeedItem (vanilla/server BlockCrops.java:160-163): wheat seeds. BlockCarrot / BlockPotato
     * and FCBlockHempCrop override it.
     */
    public int getSeedItem() {
        return Item.seeds.itemID;
    }

    /**
     * 1.5.2 BlockCrops.getCropItem (vanilla/server BlockCrops.java:168-171): wheat. BlockCarrot / BlockPotato and
     * FCBlockHempCrop override it.
     */
    public int getCropItem() {
        return Item.wheat.itemID;
    }

    // --- Seed dropping ---

    /** Port overload of DropSeeds without the fortune modifier. */
    public void DropSeeds(World world, int i, int j, int k, int iMetadata, float fChance) {
        DropSeeds(world, i, j, k, iMetadata, fChance, 0);
    }

    /**
     * 1.5.2 BlockCrops.DropSeeds (FCMOD, vanilla/server BlockCrops.java:305-314): one seed, and a second one with
     * chance (4 + fortune) / 16. FCBlockHempCrop overrides it.
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
