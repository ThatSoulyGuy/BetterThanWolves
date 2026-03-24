package btw.modern;

public class BlockFire extends Block {

    public static int[] chanceToEncourageFire = new int[4096];
    public static int[] abilityToCatchFire = new int[4096];

    private Icon[] fireIcons = new Icon[2];

    protected BlockFire(int id) {
        super(id, Material.fire);
        setTickRandomly(true);
    }

    @Override
    public void registerIcons(IconRegister register) {
        fireIcons[0] = register.registerIcon("fire_layer_0");
        fireIcons[1] = register.registerIcon("fire_layer_1");
        this.blockIcon = fireIcons[0];
    }

    @Override
    public Icon func_94438_c(int index) {
        return fireIcons[index & 1];
    }

    public boolean canBlockCatchFire(IBlockAccess blockAccess, int i, int j, int k) {
        return false;
    }

    public static boolean CanBlockBeDestroyedByFire(int blockID) {
        return false;
    }

    public boolean canNeighborBurn(World world, int x, int y, int z) { return false; }
}
