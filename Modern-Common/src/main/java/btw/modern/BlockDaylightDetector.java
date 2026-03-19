package btw.modern;

public class BlockDaylightDetector extends BlockContainer {

    protected BlockDaylightDetector(int id) {
        super(id, Material.wood);
    }

    public TileEntity createNewTileEntity(World world) {
        return null;
    }

    public void updateLightLevel(World world, int i, int j, int k) {}
}
