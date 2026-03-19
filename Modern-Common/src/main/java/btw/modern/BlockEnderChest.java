package btw.modern;

public class BlockEnderChest extends BlockContainer {

    protected BlockEnderChest(int id) {
        super(id, Material.rock);
    }

    public TileEntity createNewTileEntity(World world) {
        return null;
    }
}
