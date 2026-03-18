package btw.api;

public abstract class BlockRedstoneLogic extends BlockDirectional {

    protected BlockRedstoneLogic(int id, Material material) {
        super(id, material);
    }

    public abstract int func_94481_j_(int var1);
    public int func_94480_d(IBlockAccess blockAccess, int i, int j, int k, int side) { return 0; }
    public boolean func_94490_c(int metadata) { return false; }
    public boolean func_94478_d(World world, int i, int j, int k, int side) { return false; }
    public int getInputStrength(World world, int i, int j, int k, int side) { return 0; }
    public void func_94479_f(World world, int i, int j, int k, int metadata) {}
    public boolean func_96470_c(int metadata) { return false; }
}
