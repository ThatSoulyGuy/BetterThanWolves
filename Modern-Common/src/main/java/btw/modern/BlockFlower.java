package btw.modern;

public class BlockFlower extends Block {

    protected BlockFlower(int id, Material material) {
        super(id, material);
    }

    protected BlockFlower(int id) {
        super(id, Material.plants);
    }

    // Shadow remapping maps FCBlockPlants → BlockFlower.
    // These methods replicate FCBlockPlants's overrides so FC subclasses
    // (FCBlockCrops, FCBlockHempCrop, etc.) dispatch correctly.

    @Override
    public boolean canPlaceBlockAt(World world, int i, int j, int k) {
        return super.canPlaceBlockAt(world, i, j, k) && CanGrowOnBlock(world, i, j - 1, k);
    }

    @Override
    public boolean canBlockStay(World world, int i, int j, int k) {
        return CanGrowOnBlock(world, i, j - 1, k);
    }

    /** FCBlockPlants default: checks CanWildVegetationGrowOnBlock. */
    public boolean CanGrowOnBlock(World world, int i, int j, int k) {
        Block blockOn = Block.blocksList[world.getBlockId(i, j, k)];
        return blockOn != null && blockOn.CanWildVegetationGrowOnBlock(world, i, j, k);
    }

    public final void checkFlowerChange(World world, int i, int j, int k) {}
}
