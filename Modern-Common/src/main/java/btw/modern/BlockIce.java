package btw.modern;

public class BlockIce extends BlockBreakable {

    protected BlockIce(int id) {
        super(id, Material.ice, false);
        this.setTickRandomly(true);
    }

    @Override
    public int quantityDropped(java.util.Random random) {
        return 0;
    }

    @Override
    public void harvestBlock(World world, EntityPlayer player, int x, int y, int z, int metadata) {
        super.harvestBlock(world, player, x, y, z, metadata);
        if (world.provider.isHellWorld) return;
        Material below = world.getBlockMaterial(x, y - 1, z);
        if (below.blocksMovement() || below.isLiquid()) {
            world.setBlock(x, y, z, Block.waterMoving.blockID);
        }
    }
}
