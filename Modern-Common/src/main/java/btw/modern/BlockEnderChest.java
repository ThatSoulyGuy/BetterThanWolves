package btw.modern;

public class BlockEnderChest extends BlockContainer {

    protected BlockEnderChest(int id) {
        super(id, Material.rock);
        this.setBlockBounds(0.0625F, 0.0F, 0.0625F, 0.9375F, 0.875F, 0.9375F);
    }

    public TileEntity createNewTileEntity(World world) {
        return null;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLiving placer, ItemStack stack) {
        int dir = MathHelper.floor_double((double)(placer.rotationYaw * 4.0F / 360.0F) + 0.5) & 3;
        int facing;
        switch (dir) {
            case 0: facing = 2; break;
            case 1: facing = 5; break;
            case 2: facing = 3; break;
            case 3: facing = 4; break;
            default: facing = 2;
        }
        world.setBlockMetadataWithNotify(x, y, z, facing);
    }

    // 1.5.2 BlockEnderChest.isOpaqueCube / renderAsNormalBlock / getRenderType (vanilla/client
    // BlockEnderChest.java:32-51). FCBlockEnderChest (130) overrides none of them.
    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public int getRenderType() {
        return 22;
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockEnderChest.registerIcons (vanilla/client
    // BlockEnderChest.java:177-180) registers "obsidian". FCBlockEnderChest (130) inherits it.
    @Override
    public void registerIcons(IconRegister register) {
        this.blockIcon = register.registerIcon("obsidian");
    }
}
