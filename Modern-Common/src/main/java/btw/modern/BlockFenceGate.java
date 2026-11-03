package btw.modern;

public class BlockFenceGate extends BlockDirectional {

    protected BlockFenceGate(int id) {
        super(id, Material.wood);
    }

    public static boolean isFenceGateOpen(int metadata) {
        return (metadata & 4) != 0;
    }

    // 1.5.2 BlockFenceGate.getCollisionBoundingBoxFromPool (vanilla/client BlockFenceGate.java:45-49): no collision
    // when open, else a 1.5-high, 0.25-thick box across the gate's axis (directions 0/2 = south/north span x).
    // Reads 1.5.2 gate metadata, direction (S=0, W=1, N=2, E=3) | 4 when open, which WorldBridge derives from the
    // MC FenceGateBlock FACING/OPEN state. FCBlockFenceGate does not override it; its 1.0-high
    // GetBlockBoundsFromPoolBasedOnState stays the selection/render box.
    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        int meta = world.getBlockMetadata(x, y, z);

        if (isFenceGateOpen(meta)) {
            return null;
        }

        if (meta != 2 && meta != 0) {
            return AxisAlignedBB.getAABBPool().getAABB((double)((float)x + 0.375F), (double)y, (double)z,
                (double)((float)x + 0.625F), (double)((float)y + 1.5F), (double)(z + 1));
        }

        return AxisAlignedBB.getAABBPool().getAABB((double)x, (double)y, (double)((float)z + 0.375F),
            (double)(x + 1), (double)((float)y + 1.5F), (double)((float)z + 0.625F));
    }

    // 1.5.2 BlockFenceGate.isOpaqueCube / renderAsNormalBlock (vanilla/client BlockFenceGate.java:72-83)
    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    // 1.5.2 BlockFenceGate.getBlocksMovement (vanilla/client BlockFenceGate.java:85-88; misnamed, true = passable):
    // open gates are passable. FCBlockFenceGate.CanPathThroughBlock reads it, so FC path finding now agrees with
    // the null collision of an open gate instead of treating every gate as a closed wooden block.
    @Override
    public boolean getBlocksMovement(IBlockAccess blockAccess, int x, int y, int z) {
        return isFenceGateOpen(blockAccess.getBlockMetadata(x, y, z));
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLiving placer, ItemStack stack) {
        int dir = (MathHelper.floor_double((double)(placer.rotationYaw * 4.0F / 360.0F) + 0.5) & 3) % 4;
        world.setBlockMetadataWithNotify(x, y, z, dir);
    }

    // 1.5.2 BlockFenceGate.getRenderType (vanilla/client BlockFenceGate.java:93-96). FCBlockFenceGate (107) does not
    // override it.
    @Override
    public int getRenderType() {
        return 21;
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockFenceGate's icon members (vanilla/client
    // BlockFenceGate.java:28-31, :183): the planks on every face, and no icon of its own. FCBlockFenceGate (107)
    // inherits both; a held gate drew Block's default, the unlocalized "fenceGate", which names no texture.
    @Override
    public Icon getIcon(int side, int metadata) {
        return Block.planks.getBlockTextureFromSide(side);
    }

    @Override
    public void registerIcons(IconRegister register) {}
}
