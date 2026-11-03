package btw.modern;

public class BlockPistonMoving extends BlockContainer {

    protected BlockPistonMoving(int id) {
        super(id, Material.piston);
    }

    public TileEntity createNewTileEntity(World world) {
        return null;
    }

    public AxisAlignedBB getAxisAlignedBB(World world, int i, int j, int k, int blockID, float progress, int orientation) {
        return null;
    }

    public static TileEntity getTileEntity(int x, int y, int z, boolean allowCreate, boolean forceBlock) {
        return null;
    }

    // 1.5.2 BlockPistonMoving.getRenderType / isOpaqueCube / renderAsNormalBlock (vanilla/client
    // BlockPistonMoving.java:76-96). FCBlockPistonMoving (36) overrides none of them.
    @Override
    public int getRenderType() {
        return -1;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockPistonMoving.registerIcons (vanilla/client
    // BlockPistonMoving.java:286-289) registers "piston_top". FCBlockPistonMoving (36) inherits it.
    @Override
    public void registerIcons(IconRegister register) {
        this.blockIcon = register.registerIcon("piston_top");
    }
}
