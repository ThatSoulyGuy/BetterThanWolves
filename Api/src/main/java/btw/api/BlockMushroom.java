package btw.api;

public class BlockMushroom extends BlockFlower {

    protected BlockMushroom(int id) {
        super(id, Material.plants);
    }

    protected BlockMushroom(int id, String iconName) {
        super(id, Material.plants);
    }

    public boolean fertilizeMushroom(World world, int i, int j, int k, java.util.Random rand) { return false; }
}
