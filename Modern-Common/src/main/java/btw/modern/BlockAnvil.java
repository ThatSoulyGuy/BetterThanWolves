package btw.modern;

public class BlockAnvil extends BlockSandStone {
    protected BlockAnvil(int id) {
        super(id);
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLiving placer, ItemStack stack) {
        int dir = MathHelper.floor_double((double)(placer.rotationYaw * 4.0F / 360.0F) + 0.5) & 3;
        int damageState = world.getBlockMetadata(x, y, z) >> 2;
        dir = (dir + 1) % 4;
        int facing;
        switch (dir) {
            case 0: facing = 2; break;
            case 1: facing = 3; break;
            case 2: facing = 0; break;
            case 3: facing = 1; break;
            default: facing = 0;
        }
        world.setBlockMetadataWithNotify(x, y, z, facing | (damageState << 2));
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockAnvil's icon members (vanilla/client BlockAnvil.java:23-25,
    // :54-80): the anvil base, and the top by damage while RenderBlocks.renderBlockAnvil draws the top
    // (field_82521_b 3). No block is a BlockAnvil (FCBlockAnvil, 145, extends Block and registers its own
    // "anvil_base"), so nothing draws these; they also keep this class from answering BlockSandStone's icons.
    private static final String[] anvilIconNames = new String[] {"anvil_top", "anvil_top_damaged_1",
            "anvil_top_damaged_2"};
    public int field_82521_b = 0;
    private Icon[] iconArray;

    // 1.5.2 BlockAnvil.getIcon (vanilla/client BlockAnvil.java:54-65); the base icon while the tops are not
    // registered (client only)
    @Override
    public Icon getIcon(int side, int metadata) {
        if (this.field_82521_b == 3 && side == 1 && this.iconArray != null) {
            return this.iconArray[(metadata >> 2) % this.iconArray.length];
        }
        return this.blockIcon;
    }

    // 1.5.2 BlockAnvil.registerIcons (vanilla/client BlockAnvil.java:71-80)
    @Override
    public void registerIcons(IconRegister register) {
        this.blockIcon = register.registerIcon("anvil_base");
        Icon[] icons = new Icon[anvilIconNames.length];
        for (int i = 0; i < icons.length; ++i) {
            icons[i] = register.registerIcon(anvilIconNames[i]);
        }
        this.iconArray = icons;
    }
}
