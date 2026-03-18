package btw.api;

public class BlockCrops extends BlockFlower {

    protected BlockCrops(int id) {
        super(id, Material.plants);
    }

    public int getRenderType() {
        return 6;
    }

    public void AttemptToGrow(World world, int i, int j, int k, java.util.Random rand) {}
    public void DropSeeds(World world, int i, int j, int k, int iMetadata, float fChance) {}
    public void DropSeeds(World world, int i, int j, int k, int iMetadata, float fChance, int iFortuneModifier) {}
    public int getSeedItem() { return 0; }
    public int getCropItem() { return 0; }
    public void fertilize(World world, int i, int j, int k) {}

    public int GetGrowthLevel(IBlockAccess blockAccess, int x, int y, int z) { return 0; }
    public void SetGrowthLevelNoNotify(World world, int x, int y, int z, int level) {}
    public int getPlantForMeta(int meta) { return 0; }
    public int getMetaForPlant(ItemStack stack) { return 0; }
}
