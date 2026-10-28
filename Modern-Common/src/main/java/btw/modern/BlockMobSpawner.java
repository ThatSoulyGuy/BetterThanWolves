package btw.modern;

public class BlockMobSpawner extends BlockContainer {

    protected BlockMobSpawner(int id) {
        super(id, Material.rock);
    }

    public TileEntity createNewTileEntity(World world) {
        return null;
    }

    @Override
    public int quantityDropped(java.util.Random random) {
        return 0;
    }
}
