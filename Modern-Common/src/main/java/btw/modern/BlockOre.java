package btw.modern;

public class BlockOre extends Block {

    protected BlockOre(int id) {
        super(id, Material.rock);
    }

    @Override
    public int idDropped(int metadata, java.util.Random random, int fortune) {
        return switch (blockID) {
            case 16 -> Item.coal.itemID;
            case 56 -> Item.diamond.itemID;
            case 21 -> Item.dyePowder.itemID;
            case 129 -> Item.emerald.itemID;
            case 153 -> Item.netherQuartz.itemID;
            default -> blockID;
        };
    }

    @Override
    public int quantityDropped(java.util.Random random) {
        return blockID == 21 ? 4 + random.nextInt(5) : 1;
    }

    @Override
    public int quantityDroppedWithBonus(int fortune, java.util.Random random) {
        if (fortune > 0 && blockID != idDropped(0, random, fortune)) {
            return quantityDropped(random) * (Math.max(random.nextInt(fortune + 2) - 1, 0) + 1);
        }
        return quantityDropped(random);
    }

    @Override
    public int damageDropped(int metadata) {
        return blockID == 21 ? 4 : 0;
    }
}
