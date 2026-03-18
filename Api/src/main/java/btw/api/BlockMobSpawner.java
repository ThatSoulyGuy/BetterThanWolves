package btw.api;

public class BlockMobSpawner extends BlockContainer {

    protected BlockMobSpawner(int id) {
        super(id, Material.rock);
    }

    public TileEntity createNewTileEntity(World world) {
        return null;
    }
}
