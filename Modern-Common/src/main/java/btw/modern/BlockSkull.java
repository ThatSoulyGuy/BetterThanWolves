package btw.modern;

public class BlockSkull extends BlockContainer {

    protected BlockSkull(int id) {
        super(id, Material.circuits);
    }

    public TileEntity createNewTileEntity(World world) {
        return null;
    }

    public void makeWither(World world, int i, int j, int k, TileEntitySkull tileEntity) {}

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLiving placer, ItemStack stack) {
        int dir = MathHelper.floor_double((double)(placer.rotationYaw * 4.0F / 360.0F) + 2.5) & 3;
        world.setBlockMetadataWithNotify(x, y, z, dir);
    }

    // 1.5.2 BlockSkull.getRenderType / isOpaqueCube / renderAsNormalBlock (vanilla/client BlockSkull.java:30-50).
    // FCBlockSkull (144) overrides none of them.
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

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockSkull's icon members (vanilla/client
    // BlockSkull.java:297-305): no icon of its own, soul sand for its particles. FCBlockSkull (144) inherits them.
    @Override
    public void registerIcons(IconRegister register) {}

    @Override
    public Icon getIcon(int side, int metadata) {
        return Block.slowSand.getBlockTextureFromSide(side);
    }
}
