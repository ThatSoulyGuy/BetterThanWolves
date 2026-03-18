package btw.api;

public class BlockStep extends BlockHalfSlab {

    protected BlockStep(int id, boolean isDouble) {
        super(id, isDouble, Material.rock);
    }

    public String getFullSlabName(int metadata) {
        return "";
    }
}
