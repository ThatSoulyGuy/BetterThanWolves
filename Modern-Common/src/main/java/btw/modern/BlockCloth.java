package btw.modern;

public class BlockCloth extends Block {

    public BlockCloth() {
        super(35, Material.cloth);
    }

    protected BlockCloth(int id) {
        super(id, Material.cloth);
    }

    @Override
    public int damageDropped(int metadata) {
        return metadata;
    }

    public static int getBlockFromDye(int dyeColor) {
        return ~dyeColor & 15;
    }

    public static int getDyeFromBlock(int blockMeta) {
        return ~blockMeta & 15;
    }
}
