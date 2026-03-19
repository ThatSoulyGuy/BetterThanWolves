package btw.modern;

public class BlockJukeBox extends BlockContainer {

    protected BlockJukeBox(int id) {
        super(id, Material.wood);
    }

    public TileEntity createNewTileEntity(World world) {
        return null;
    }
}
