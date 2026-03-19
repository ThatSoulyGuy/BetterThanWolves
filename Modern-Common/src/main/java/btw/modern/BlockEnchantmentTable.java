package btw.modern;

public class BlockEnchantmentTable extends BlockContainer {

    protected BlockEnchantmentTable(int id) {
        super(id, Material.rock);
    }

    public TileEntity createNewTileEntity(World world) {
        return null;
    }
}
