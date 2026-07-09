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

    // 1.5.2 BlockFire.canBlockCatchFire — called by FCBlockFire.updateTick (super.canBlockCatchFire)
    // and Block.GetCanBlockBeIncinerated; reads the array populated by Block.SetFireProperties
    public boolean canBlockCatchFire(IBlockAccess blockAccess, int i, int j, int k) {
        return chanceToEncourageFire[blockAccess.getBlockId(i, j, k)] > 0;
    }

    // 1.5.2 FCBlockFire.CanBlockBeDestroyedByFire — mirrors the FC static so shim-side callers
    // (Block.IsIncineratedInCrucible in vanilla) see real flammability
    public static boolean CanBlockBeDestroyedByFire(int blockID) {
        return abilityToCatchFire[blockID] > 0;
    }

    // 1.5.2 BlockFire.canNeighborBurn — called by FCBlockFire.updateTick (not overridden there);
    // fire extinguishes immediately without it
    public boolean canNeighborBurn(World world, int x, int y, int z) {
        return this.canBlockCatchFire(world, x + 1, y, z)
            || this.canBlockCatchFire(world, x - 1, y, z)
            || this.canBlockCatchFire(world, x, y - 1, z)
            || this.canBlockCatchFire(world, x, y + 1, z)
            || this.canBlockCatchFire(world, x, y, z - 1)
            || this.canBlockCatchFire(world, x, y, z + 1);
    }
}
