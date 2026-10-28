package btw.modern;

public abstract class BlockButton extends Block {

    protected BlockButton(int id) {
        super(id, Material.circuits);
        this.setTickRandomly(true);
    }

    protected BlockButton(int id, boolean sensitive) {
        super(id, Material.circuits);
        this.setTickRandomly(true);
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
            case 2 -> world.isBlockNormalCube(x, y, z + 1);
            case 3 -> world.isBlockNormalCube(x, y, z - 1);
            case 4 -> world.isBlockNormalCube(x + 1, y, z);
            case 5 -> world.isBlockNormalCube(x - 1, y, z);
            default -> false;
        };
    }

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        return world.isBlockNormalCube(x - 1, y, z) || world.isBlockNormalCube(x + 1, y, z)
                || world.isBlockNormalCube(x, y, z - 1) || world.isBlockNormalCube(x, y, z + 1);
    }

    @Override
    public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int metadata) {
        int orientation;
        if (canPlaceBlockOnSide(world, x, y, z, side)) {
            orientation = 6 - side;
        } else if (world.isBlockNormalCube(x + 1, y, z)) {
            orientation = 2;
        } else if (world.isBlockNormalCube(x, y, z - 1)) {
            orientation = 3;
        } else if (world.isBlockNormalCube(x, y, z + 1)) {
            orientation = 4;
        } else {
            orientation = 1;
        }
        return orientation + (metadata & 8);
    }
}
