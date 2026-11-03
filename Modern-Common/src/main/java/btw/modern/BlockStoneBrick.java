package btw.modern;

public class BlockStoneBrick extends Block {

    protected BlockStoneBrick(int id) {
        super(id, Material.rock);
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockStoneBrick's icon members (vanilla/client
    // BlockStoneBrick.java:22-23, :34-42, :67-75): plain, mossy, cracked and chiseled. FCBlockStoneBrick (98)
    // inherits both; a held stone brick drew Block's default, the unlocalized "stonebricksmooth" (plain), for every
    // type.
    public static final String[] field_94407_b = new String[] {"stonebricksmooth", "stonebricksmooth_mossy",
            "stonebricksmooth_cracked", "stonebricksmooth_carved"};
    private Icon[] brickIcons;

    // 1.5.2 BlockStoneBrick.getIcon (vanilla/client BlockStoneBrick.java:34-42); Block's icon until registerIcons
    // runs (client only)
    @Override
    public Icon getIcon(int side, int metadata) {
        if (this.brickIcons == null) {
            return super.getIcon(side, metadata);
        }
        if (metadata < 0 || metadata >= field_94407_b.length) {
            metadata = 0;
        }
        return this.brickIcons[metadata];
    }

    // 1.5.2 BlockStoneBrick.registerIcons (vanilla/client BlockStoneBrick.java:67-75)
    @Override
    public void registerIcons(IconRegister register) {
        Icon[] icons = new Icon[field_94407_b.length];
        for (int i = 0; i < icons.length; ++i) {
            icons[i] = register.registerIcon(field_94407_b[i]);
        }
        this.brickIcons = icons;
    }
}
