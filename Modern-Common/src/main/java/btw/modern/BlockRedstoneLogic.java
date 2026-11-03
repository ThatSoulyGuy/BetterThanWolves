package btw.modern;

public abstract class BlockRedstoneLogic extends BlockDirectional {

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 declares the powered flag here (vanilla/client
    // BlockRedstoneLogic.java:22, set by the constructor at :27), where getIcon and registerIcons read it; the
    // repeater and comparator constructors set it. (It lived in BlockRedstoneRepeater; FCBlockRedstoneRepeater's read
    // resolves through the superclass chain.)
    public boolean isRepeaterPowered;

    protected BlockRedstoneLogic(int id, Material material) {
        super(id, material);
        // 1.5.2 BlockRedstoneLogic constructor (FCMOD, vanilla/client BlockRedstoneLogic.java:24-32): repeaters
        // and comparators are 1/8 high. Without this they collided as full blocks (FCBlockRedstoneRepeater /
        // FCBlockComparator set no bounds of their own).
        InitBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.125F, 1.0F);
    }

    // 1.5.2 BlockRedstoneLogic.renderAsNormalBlock / isOpaqueCube (vanilla/client BlockRedstoneLogic.java:37-40,
    // 331-334). A small FC mob standing on the 1/8-high box has its eye inside the cell; with the inherited true
    // that counted as being inside an opaque block (Entity.isEntityInsideOpaqueBlock -> inWall hit).
    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockRedstoneLogic's icon members (vanilla/client
    // BlockRedstoneLogic.java:88-100): a redstone torch below, the repeater (lit or not) on top, stone slab sides.
    // FCBlockRedstoneRepeater (93, 94) inherits both.
    @Override
    public Icon getIcon(int side, int metadata) {
        return side == 0 ? (this.isRepeaterPowered ? Block.torchRedstoneActive.getBlockTextureFromSide(side)
                        : Block.torchRedstoneIdle.getBlockTextureFromSide(side))
                : (side == 1 ? this.blockIcon : Block.stoneDoubleSlab.getBlockTextureFromSide(1));
    }

    // 1.5.2 BlockRedstoneLogic.registerIcons (vanilla/client BlockRedstoneLogic.java:97-100)
    @Override
    public void registerIcons(IconRegister register) {
        this.blockIcon = register.registerIcon(this.isRepeaterPowered ? "repeater_lit" : "repeater");
    }

    public abstract int func_94481_j_(int var1);
    public int func_94480_d(IBlockAccess blockAccess, int i, int j, int k, int side) { return 0; }
    public boolean func_94490_c(int metadata) { return false; }
    public boolean func_94478_d(World world, int i, int j, int k, int side) { return false; }
    public int getInputStrength(World world, int i, int j, int k, int side) { return 0; }
    public void func_94479_f(World world, int i, int j, int k, int metadata) {}
    public boolean func_96470_c(int metadata) { return false; }
}
