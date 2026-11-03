package btw.modern;

public class BlockCloth extends Block {

    public BlockCloth() {
        super(35, Material.cloth);
    }

    protected BlockCloth(int id) {
        super(id, Material.cloth);
    }

    public static int getBlockFromDye(int dyeColor) {
        return ~dyeColor & 15;
    }

    public static int getDyeFromBlock(int blockMeta) {
        return ~blockMeta & 15;
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockCloth's icon members (vanilla/client BlockCloth.java:21,
    // :32-35, :76-84): cloth_<meta>, one per colour. FCBlockCloth (35) inherits both; a held or worn wool block
    // drew Block's default, the unlocalized "cloth", which names no texture.
    private Icon[] iconArray;

    // 1.5.2 BlockCloth.getIcon (vanilla/client BlockCloth.java:32-35); Block's icon until registerIcons runs (client
    // only)
    @Override
    public Icon getIcon(int side, int metadata) {
        if (this.iconArray == null) {
            return super.getIcon(side, metadata);
        }
        return this.iconArray[metadata % this.iconArray.length];
    }

    // 1.5.2 BlockCloth.registerIcons (vanilla/client BlockCloth.java:76-84)
    @Override
    public void registerIcons(IconRegister register) {
        Icon[] icons = new Icon[16];
        for (int i = 0; i < icons.length; ++i) {
            icons[i] = register.registerIcon("cloth_" + i);
        }
        this.iconArray = icons;
    }
}
