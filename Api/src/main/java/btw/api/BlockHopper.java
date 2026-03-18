package btw.api;

public class BlockHopper extends BlockContainer {

    protected BlockHopper(int id) {
        super(id, Material.iron);
    }

    public TileEntity createNewTileEntity(World world) {
        return null;
    }

    public boolean hasComparatorInputOverride() { return false; }
    public int getComparatorInputOverride(World world, int i, int j, int k, int side) { return 0; }

    public static int getDirectionFromMetadata(int metadata) { return metadata & 7; }
}
