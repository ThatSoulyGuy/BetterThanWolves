package btw.modern;

public class BlockSilverfish extends Block {

    protected BlockSilverfish(int id) {
        super(id, Material.clay);
    }

    public static boolean getPosingIdByMetadata(int blockId) {
        return blockId == Block.stone.blockID
                || blockId == Block.cobblestone.blockID
                || blockId == Block.stoneBrick.blockID;
    }

    @Override
    public int quantityDropped(java.util.Random random) {
        return 0;
    }
}
