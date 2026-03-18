package btw.api;

public class BlockWall extends Block {

    protected BlockWall(int id, Block modelBlock) {
        super(id, Material.rock);
    }

    public boolean canConnectWallTo(IBlockAccess blockAccess, int i, int j, int k) {
        return false;
    }
}
