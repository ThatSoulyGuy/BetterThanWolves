package btw.modern;

public class BlockSkull extends BlockContainer {

    protected BlockSkull(int id) {
        super(id, Material.circuits);
    }

    public TileEntity createNewTileEntity(World world) {
        return null;
    }

    public void makeWither(World world, int i, int j, int k, TileEntitySkull tileEntity) {}
}
