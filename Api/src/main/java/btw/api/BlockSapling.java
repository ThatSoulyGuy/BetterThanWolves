package btw.api;

public class BlockSapling extends BlockFlower {

    protected BlockSapling(int id) {
        super(id, Material.plants);
    }

    public boolean isSameSapling(World world, int i, int j, int k, int metadata) {
        return false;
    }

    public void growTree(World world, int i, int j, int k, java.util.Random random) {}
}
