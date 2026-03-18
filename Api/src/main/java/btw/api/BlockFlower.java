package btw.api;

public class BlockFlower extends Block {

    protected BlockFlower(int id, Material material) {
        super(id, material);
    }

    protected BlockFlower(int id) {
        super(id, Material.plants);
    }

    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        return false;
    }

    public boolean canBlockStay(World world, int x, int y, int z) {
        return false;
    }

    public final void checkFlowerChange(World world, int i, int j, int k) {}
}
