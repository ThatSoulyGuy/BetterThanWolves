package btw.modern;

public class BlockStep extends BlockHalfSlab {

    protected BlockStep(int id, boolean isDouble) {
        super(id, isDouble, Material.rock);
    }

    public String getFullSlabName(int metadata) {
        return "";
    }

    @Override
    public int idDropped(int metadata, java.util.Random random, int fortune) {
        return Block.stoneSingleSlab.blockID;
    }

    @Override
    public ItemStack createStackedBlock(int metadata) {
        return new ItemStack(Block.stoneSingleSlab.blockID, 2, metadata & 7);
    }
}
