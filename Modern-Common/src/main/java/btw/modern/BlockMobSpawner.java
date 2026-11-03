package btw.modern;

public class BlockMobSpawner extends BlockContainer {

    protected BlockMobSpawner(int id) {
        super(id, Material.rock);
    }

    public TileEntity createNewTileEntity(World world) {
        return null;
    }

    // 1.5.2 BlockMobSpawner.isOpaqueCube (vanilla/client BlockMobSpawner.java:64-67). FCBlockMobSpawner (52) does
    // not override it.
    @Override
    public boolean isOpaqueCube() {
        return false;
    }
}
