package btw.modern;

public class BlockSign extends BlockContainer {

    protected BlockSign(int id, Class tileEntityClass, boolean isStanding) {
        super(id, Material.wood);
    }

    // 1.5.2 BlockSign.getCollisionBoundingBoxFromPool (vanilla/client BlockSign.java:48-51): signs have no
    // collision box (FCBlockSign / FCBlockSignWall override only the bounds methods)
    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    // 1.5.2 BlockSign.isOpaqueCube / renderAsNormalBlock (vanilla/client BlockSign.java:110-113, 124-127).
    // With the inherited true, World.isBlockNormalCube reported this passable block as solid, so an FC mob's
    // eye inside the cell counted as being inside an opaque block (Entity.isEntityInsideOpaqueBlock -> inWall).
    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    public TileEntity createNewTileEntity(World world) {
        return null;
    }

    // 1.5.2 BlockSign.getRenderType / getBlocksMovement (vanilla/client BlockSign.java:102-105, :115-118; misnamed,
    // true = passable): a sign's cell is open to FC's path finder. FCBlockSign (63) and FCBlockSignWall (68) override
    // neither.
    @Override
    public int getRenderType() {
        return -1;
    }

    @Override
    public boolean getBlocksMovement(IBlockAccess blockAccess, int x, int y, int z) {
        return true;
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockSign's icon members (vanilla/client BlockSign.java:39-42,
    // :214): the planks on every face, and no icon of its own. FCBlockSign (63) and FCBlockSignWall (68) inherit them.
    @Override
    public Icon getIcon(int side, int metadata) {
        return Block.planks.getBlockTextureFromSide(side);
    }

    @Override
    public void registerIcons(IconRegister register) {}
}
