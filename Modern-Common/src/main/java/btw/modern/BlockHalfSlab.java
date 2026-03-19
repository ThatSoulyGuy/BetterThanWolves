package btw.modern;

public abstract class BlockHalfSlab extends Block {

    protected final boolean isDoubleSlab;

    protected BlockHalfSlab(int id, boolean isDouble, Material material) {
        super(id, material);
        this.isDoubleSlab = isDouble;
    }

    public abstract String getFullSlabName(int metadata);

    public boolean GetIsUpsideDown(IBlockAccess blockAccess, int x, int y, int z) { return false; }
}
