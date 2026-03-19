package btw.modern;

public class BlockFire extends Block {

    public static int[] chanceToEncourageFire = new int[4096];
    public static int[] abilityToCatchFire = new int[4096];

    protected BlockFire(int id) {
        super(id, Material.fire);
    }

    public boolean canBlockCatchFire(IBlockAccess blockAccess, int i, int j, int k) {
        return false;
    }

    public static boolean CanBlockBeDestroyedByFire(int blockID) {
        return false;
    }

    public boolean canNeighborBurn(World world, int x, int y, int z) { return false; }
}
