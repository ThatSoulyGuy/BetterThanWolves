package btw.modern;

public class BlockBeacon extends BlockContainer {

    protected BlockBeacon(int id) {
        super(id, Material.glass);
    }

    public TileEntity createNewTileEntity(World world) {
        return null;
    }
}
