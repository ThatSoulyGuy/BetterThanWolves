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

    /**
     * Set the growth level in the world without sending a block-change notification.
     */
    public void SetGrowthLevelNoNotify(World world, int x, int y, int z, int level) {
        int metadata = world.getBlockMetadata(x, y, z) & (~7); // filter out old level
        world.setBlockMetadata(x, y, z, metadata | level);
    }

    // --- Growth logic ---

    /**
     * Attempt to advance the crop one growth stage. FC subclasses override this
     * with custom daily-growth / hydration / weed checks.
     *
     * Default behaviour: if growth level is below 7 and there is enough light,
     * increment growth level with a random chance scaled by getGrowthRate-style
     * logic matching the vanilla crop tick.
     */
    public void AttemptToGrow(World world, int i, int j, int k, java.util.Random rand) {
        if (world.getBlockLightValue(i, j + 1, k) >= 9) {
            int metadata = world.getBlockMetadata(i, j, k);
            int growthLevel = GetGrowthLevel(metadata);

            if (growthLevel < 7) {
                if (rand.nextInt(26) == 0) {
                    int newMetadata = (metadata & (~7)) | (growthLevel + 1);
                    world.setBlockMetadataWithNotify(i, j, k, newMetadata);
                }
            }
        }
    }

    /**
     * Apply bonemeal-style fertilisation: boost growth by 2-5 stages, capped at 7.
     */
    public void fertilize(World world, int i, int j, int k) {
        int newLevel = world.getBlockMetadata(i, j, k)
            + MathHelper.getRandomIntegerInRange(world.rand, 2, 5);

        if (newLevel > 7) {
            newLevel = 7;
        }

        world.setBlockMetadataWithNotify(i, j, k, newLevel);
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
