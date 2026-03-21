package btw.modern;

public class BlockWeb extends Block {

    protected BlockWeb(int id) {
        super(id, Material.web);
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public int getRenderType() {
        return 1; // crossed squares
    }
}
