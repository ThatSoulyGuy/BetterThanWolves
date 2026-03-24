package btw.api;

public class BlockFlower extends Block {

    protected BlockFlower(int id, Material material) {
        super(id, material);
    }

    protected BlockFlower(int id) {
        super(id, Material.plants);
    }

    public final void checkFlowerChange(World world, int i, int j, int k) {}
}
