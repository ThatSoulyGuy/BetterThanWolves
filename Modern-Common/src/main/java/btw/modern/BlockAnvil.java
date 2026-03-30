package btw.modern;

public class BlockAnvil extends BlockSandStone {
    protected BlockAnvil(int id) {
        super(id);
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLiving placer, ItemStack stack) {
        int dir = MathHelper.floor_double((double)(placer.rotationYaw * 4.0F / 360.0F) + 0.5) & 3;
        int damageState = world.getBlockMetadata(x, y, z) >> 2;
        dir = (dir + 1) % 4;
        int facing;
        switch (dir) {
            case 0: facing = 2; break;
            case 1: facing = 3; break;
            case 2: facing = 0; break;
            case 3: facing = 1; break;
            default: facing = 0;
        }
        world.setBlockMetadataWithNotify(x, y, z, facing | (damageState << 2));
    }
}
