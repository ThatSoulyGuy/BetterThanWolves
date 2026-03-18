package btw.api;

public class BlockStem extends BlockFlower {

    public Block fruitType;

    protected BlockStem(int id) {
        super(id, Material.plants);
    }

    protected BlockStem(int id, Block fruitBlock) {
        super(id, Material.plants);
        this.fruitType = fruitBlock;
    }
}
