package btw.modern;

public class BlockSkull extends BlockContainer {

    protected BlockSkull(int id) {
        super(id, Material.circuits);
    }

    public TileEntity createNewTileEntity(World world) {
        return null;
    }

    public void makeWither(World world, int i, int j, int k, TileEntitySkull tileEntity) {}

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLiving placer, ItemStack stack) {
        int dir = MathHelper.floor_double((double)(placer.rotationYaw * 4.0F / 360.0F) + 2.5) & 3;
        world.setBlockMetadataWithNotify(x, y, z, dir);
    }
}
