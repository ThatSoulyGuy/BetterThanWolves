package btw.modern;

public class BlockLever extends Block {

    protected BlockLever(int id) {
        super(id, Material.circuits);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public boolean canPlaceBlockOnSide(World world, int x, int y, int z, int side) {
        return switch (side) {
            case 0 -> world.isBlockNormalCube(x, y + 1, z);
            case 1 -> world.doesBlockHaveSolidTopSurface(x, y - 1, z);
            case 2 -> world.isBlockNormalCube(x, y, z + 1);
            case 3 -> world.isBlockNormalCube(x, y, z - 1);
            case 4 -> world.isBlockNormalCube(x + 1, y, z);
            case 5 -> world.isBlockNormalCube(x - 1, y, z);
            default -> false;
        };
    }

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        for (int side = 0; side < 6; side++) {
            if (canPlaceBlockOnSide(world, x, y, z, side)) return true;
        }
        return false;
    }

    @Override
    public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int metadata) {
        int orientation = side >= 0 && side < 6 && canPlaceBlockOnSide(world, x, y, z, side)
                ? new int[] {0, 5, 4, 3, 2, 1}[side] : -1;
        return orientation + (metadata & 8);
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLiving placer, ItemStack stack) {
        int meta = world.getBlockMetadata(x, y, z);
        int orientation = meta & 7;
        if (placer == null || (orientation != 5 && orientation != 0)) return;
        boolean alongZ = (MathHelper.floor_double(placer.rotationYaw * 4.0F / 360.0F + 0.5D) & 1) == 0;
        int turned = orientation == 5 ? (alongZ ? 5 : 6) : (alongZ ? 7 : 0);
        world.setBlockMetadata(x, y, z, turned | (meta & 8), 2);
    }
}
