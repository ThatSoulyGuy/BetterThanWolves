package btw.modern;

public class BlockCocoa extends BlockDirectional {

    protected BlockCocoa(int id) {
        super(id, Material.plants);
        // 1.5.2 BlockCocoa constructor (vanilla/server BlockCocoa.java:25): FCBlockCocoa.updateTick (survival,
        // ripening) runs on the random tick
        setTickRandomly(true);
    }

    // 1.5.2 BlockCocoa.canBlockStay (vanilla/server BlockCocoa.java:54-61): the block the pod faces (direction
    // 0 = S, 1 = W, 2 = N, 3 = E, toward the log) is a jungle log. FCBlockCocoa.updateTick drops the pod when this
    // fails; with Block's true it never could.
    @Override
    public boolean canBlockStay(World world, int i, int j, int k) {
        int iDirection = getDirection(world.getBlockMetadata(i, j, k));
        i += Direction.offsetX[iDirection];
        k += Direction.offsetZ[iDirection];
        int iBlockID = world.getBlockId(i, j, k);
        return iBlockID == Block.wood.blockID && BlockLog.limitToValidMetadata(world.getBlockMetadata(i, j, k)) == 3;
    }

    // 1.5.2 BlockCocoa.onNeighborBlockChange (vanilla/server BlockCocoa.java:155-162). Reached through
    // BlockBehaviorMixin.btw$neighborChanged.
    @Override
    public void onNeighborBlockChange(World world, int i, int j, int k, int iNeighborBlockID) {
        if (!canBlockStay(world, i, j, k)) {
            dropBlockAsItem(world, i, j, k, world.getBlockMetadata(i, j, k), 0);
            world.setBlockToAir(i, j, k);
        }
    }

    public static int func_72219_c(int metadata) {
        return (metadata >> 2) & 3;
    }

    // 1.5.2 BlockCocoa.getRenderType / renderAsNormalBlock / isOpaqueCube (vanilla/client BlockCocoa.java:86-106).
    // FCBlockCocoa (127) overrides none of them.
    @Override
    public int getRenderType() {
        return 28;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockCocoa's icon members (vanilla/client BlockCocoa.java:21-22,
    // :33-46, :237-245): the three pod stages; getIcon answers the ripe one, func_94468_i_ a stage
    // (RenderBlocks.renderBlockCocoa). FCBlockCocoa (127) inherits them.
    public static final String[] cocoaIcons = new String[] {"cocoa_0", "cocoa_1", "cocoa_2"};
    private Icon[] iconArray;

    // 1.5.2 BlockCocoa.getIcon (vanilla/client BlockCocoa.java:33-36); Block's icon until registerIcons runs (client
    // only)
    @Override
    public Icon getIcon(int side, int metadata) {
        if (this.iconArray == null) {
            return super.getIcon(side, metadata);
        }
        return this.iconArray[2];
    }

    // 1.5.2 BlockCocoa.func_94468_i_ (vanilla/client BlockCocoa.java:38-46); null until registerIcons runs
    public Icon func_94468_i_(int stage) {
        if (this.iconArray == null) {
            return null;
        }
        if (stage < 0 || stage >= this.iconArray.length) {
            stage = this.iconArray.length - 1;
        }
        return this.iconArray[stage];
    }

    // 1.5.2 BlockCocoa.registerIcons (vanilla/client BlockCocoa.java:237-245)
    @Override
    public void registerIcons(IconRegister register) {
        Icon[] icons = new Icon[cocoaIcons.length];
        for (int i = 0; i < icons.length; ++i) {
            icons[i] = register.registerIcon(cocoaIcons[i]);
        }
        this.iconArray = icons;
    }
}
