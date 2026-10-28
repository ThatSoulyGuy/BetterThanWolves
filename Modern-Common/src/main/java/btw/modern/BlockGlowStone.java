package btw.modern;

public class BlockGlowStone extends Block {

    protected BlockGlowStone(int id, Material material) {
        super(id, material);
    }

    @Override
    public int quantityDroppedWithBonus(int fortune, java.util.Random random) {
        return MathHelper.clamp_int(quantityDropped(random) + random.nextInt(fortune + 1), 1, 4);
    }

    @Override
    public int quantityDropped(java.util.Random random) {
        return 2 + random.nextInt(3);
    }

    @Override
    public int idDropped(int metadata, java.util.Random random, int fortune) {
        return Item.lightStoneDust.itemID;
    }
}
