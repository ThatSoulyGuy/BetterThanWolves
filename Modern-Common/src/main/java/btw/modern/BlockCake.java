package btw.modern;

public class BlockCake extends Block {

    protected BlockCake(int id) {
        super(id, Material.cake);
        this.setTickRandomly(true);
    }

    @Override
    public int quantityDropped(java.util.Random random) {
        return 0;
    }
}
