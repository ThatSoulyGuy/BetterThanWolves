package btw.modern;

public class BlockSnowBlock extends Block {

    protected BlockSnowBlock(int id) {
        super(id, Material.craftedSnow);
        this.setTickRandomly(true);
    }

    @Override
    public int idDropped(int metadata, java.util.Random random, int fortune) {
        return Item.snowball.itemID;
    }

    @Override
    public int quantityDropped(java.util.Random random) {
        return 4;
    }
}
