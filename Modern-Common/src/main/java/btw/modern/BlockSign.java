package btw.modern;

public class BlockSign extends BlockContainer {

    protected BlockSign(int id, Class tileEntityClass, boolean isStanding) {
        super(id, Material.wood);
    }

    public TileEntity createNewTileEntity(World world) {
        return null;
    }
}
