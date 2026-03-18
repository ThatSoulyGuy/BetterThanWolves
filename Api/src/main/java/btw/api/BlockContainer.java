package btw.api;

public abstract class BlockContainer extends Block {

    protected BlockContainer(int id, Material material) {
        super(id, material);
    }

    public abstract TileEntity createNewTileEntity(World world);
}
