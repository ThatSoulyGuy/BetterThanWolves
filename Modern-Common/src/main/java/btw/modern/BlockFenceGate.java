package btw.modern;

public class BlockFenceGate extends BlockDirectional {

    protected BlockFenceGate(int id) {
        super(id, Material.wood);
    }

    public static boolean isFenceGateOpen(int metadata) {
        return (metadata & 4) != 0;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLiving placer, ItemStack stack) {
        int dir = (MathHelper.floor_double((double)(placer.rotationYaw * 4.0F / 360.0F) + 0.5) & 3) % 4;
        world.setBlockMetadataWithNotify(x, y, z, dir);
    }
}
