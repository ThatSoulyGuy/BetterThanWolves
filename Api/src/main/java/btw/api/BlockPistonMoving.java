package btw.api;

public class BlockPistonMoving extends BlockContainer {

    protected BlockPistonMoving(int id) {
        super(id, Material.piston);
    }

    public TileEntity createNewTileEntity(World world) {
        return null;
    }

    public AxisAlignedBB getAxisAlignedBB(World world, int i, int j, int k, int blockID, float progress, int orientation) {
        return null;
    }

    public static TileEntity getTileEntity(int x, int y, int z, boolean allowCreate, boolean forceBlock) {
        return null;
    }
}
