package btw.modern;

public class BlockNote extends BlockContainer {

    protected BlockNote(int id) {
        super(id, Material.rock);
    }

    public TileEntity createNewTileEntity(World world) {
        return null;
    }
}
