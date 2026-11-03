package btw.modern;

public class BlockSandStone extends Block {

    protected BlockSandStone(int id) {
        super(id, Material.rock);
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockSandStone's icon members (vanilla/client
    // BlockSandStone.java:22-25, :36-58, :82-93): the top, the bottom (the top for chiseled and smooth), and the
    // plain, chiseled or smooth side. FCBlockSandStone (24) inherits both; a held sandstone drew Block's default,
    // the unlocalized "sandStone" (the plain side), on every face of every type.
    private static final String[] SIDE_ICON_NAMES = new String[] {"sandstone_side", "sandstone_carved",
            "sandstone_smooth"};
    private Icon[] sideIcons;
    private Icon topIcon;
    private Icon bottomIcon;

    // 1.5.2 BlockSandStone.getIcon (vanilla/client BlockSandStone.java:36-58); Block's icon until registerIcons runs
    // (client only)
    @Override
    public Icon getIcon(int side, int metadata) {
        if (this.sideIcons == null || this.topIcon == null || this.bottomIcon == null) {
            return super.getIcon(side, metadata);
        }
        if (side != 1 && (side != 0 || metadata != 1 && metadata != 2)) {
            if (side == 0) {
                return this.bottomIcon;
            }
            if (metadata < 0 || metadata >= this.sideIcons.length) {
                metadata = 0;
            }
            return this.sideIcons[metadata];
        }
        return this.topIcon;
    }

    // 1.5.2 BlockSandStone.registerIcons (vanilla/client BlockSandStone.java:82-93)
    @Override
    public void registerIcons(IconRegister register) {
        Icon[] icons = new Icon[SIDE_ICON_NAMES.length];
        for (int i = 0; i < icons.length; ++i) {
            icons[i] = register.registerIcon(SIDE_ICON_NAMES[i]);
        }
        this.topIcon = register.registerIcon("sandstone_top");
        this.bottomIcon = register.registerIcon("sandstone_bottom");
        this.sideIcons = icons;
    }
}
