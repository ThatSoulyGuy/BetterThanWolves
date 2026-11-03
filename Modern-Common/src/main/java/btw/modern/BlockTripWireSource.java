package btw.modern;

public class BlockTripWireSource extends Block {

    // 1.5.2 BlockTripWireSource constructor sets the random-tick flag (vanilla/server BlockTripWireSource.java:24). Flag
    // only: the tripwire hook tick body stays 1.20.1's.
    protected BlockTripWireSource(int id) {
        super(id, Material.circuits);
        this.setTickRandomly(true);
    }

    // 1.5.2 BlockTripWireSource.getCollisionBoundingBoxFromPool (vanilla/client BlockTripWireSource.java:32-35):
    // tripwire hooks have no collision box (FCBlockTripWireSource overrides only the bounds methods)
    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    // 1.5.2 BlockTripWireSource.isOpaqueCube / renderAsNormalBlock (vanilla/client BlockTripWireSource.java:41-52).
    // With the inherited true, World.isBlockNormalCube reported this passable block as solid, so an FC mob's
    // eye inside the cell counted as being inside an opaque block (Entity.isEntityInsideOpaqueBlock -> inWall).
    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    // 1.5.2 BlockTripWireSource.canPlaceBlockOnSide (vanilla/server BlockTripWireSource.java:72-75): a hook goes only on
    // the side (faces 2-5) of a normal cube, never on a top or bottom face. World.canPlaceEntityOnSide asks it for every
    // placement from an item or a block dispenser (WorldBridge.canPlaceEntityOnSide, vanilla/server World.java:3407);
    // with Block's inherited true the FC route hung a hook on a top face.
    // FCMOD-INTEGRATION(1.20.1): placement_validity: the 1.5.2 base hooks the FC hook (FCBlockTripWireSource) inherits
    @Override
    public boolean canPlaceBlockOnSide(World world, int x, int y, int z, int side) {
        return side == 2 && world.isBlockNormalCube(x, y, z + 1)
                || side == 3 && world.isBlockNormalCube(x, y, z - 1)
                || side == 4 && world.isBlockNormalCube(x + 1, y, z)
                || side == 5 && world.isBlockNormalCube(x - 1, y, z);
    }

    // 1.5.2 BlockTripWireSource.canPlaceBlockAt (vanilla/server BlockTripWireSource.java:80-83): a normal cube on any
    // horizontal side.
    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        return world.isBlockNormalCube(x - 1, y, z)
                || world.isBlockNormalCube(x + 1, y, z)
                || world.isBlockNormalCube(x, y, z - 1)
                || world.isBlockNormalCube(x, y, z + 1);
    }

    // 1.5.2 BlockTripWireSource.onBlockPlaced (vanilla/server BlockTripWireSource.java:88-113): the direction the hook
    // faces, away from the cube it hangs on (0 = S, 1 = W, 2 = N, 3 = E; 1.20.1 Direction.get2DDataValue), with attached
    // (4) and powered (8) clear. Port: isBlockNormalCubeDefault(..., true) is isBlockNormalCube, as the cell next to a
    // placement is loaded. WorldBridge's hook codec writes the result as TripWireHookBlock FACING.
    @Override
    public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ,
                             int metadata) {
        int direction = 0;

        if (side == 2 && world.isBlockNormalCube(x, y, z + 1)) {
            direction = 2;
        }

        if (side == 3 && world.isBlockNormalCube(x, y, z - 1)) {
            direction = 0;
        }

        if (side == 4 && world.isBlockNormalCube(x + 1, y, z)) {
            direction = 1;
        }

        if (side == 5 && world.isBlockNormalCube(x - 1, y, z)) {
            direction = 3;
        }

        return direction;
    }

    // 1.5.2 BlockTripWireSource.getRenderType (vanilla/client BlockTripWireSource.java:57-60).
    // FCBlockTripWireSource (131) does not override it.
    @Override
    public int getRenderType() {
        return 29;
    }
}
