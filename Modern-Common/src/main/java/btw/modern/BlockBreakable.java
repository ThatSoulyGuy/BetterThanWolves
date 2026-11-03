package btw.modern;

public class BlockBreakable extends Block {

    protected BlockBreakable(int id, Material material, boolean renderAsNormalBlock) {
        super(id, material);
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2's constructor (vanilla/client BlockBreakable.java:20-27) keeps the
    // icon name its subclass passes ("glass", "ice", "portal"), and registerIcons (:52-55) registers it. Those are
    // also the blocks' unlocalized names, so they draw what they drew. A block made through the name-less constructor
    // keeps Block's default.
    private String breakableBlockIcon;

    protected BlockBreakable(int id, String iconName, Material material, boolean renderAsNormalBlock) {
        this(id, material, renderAsNormalBlock);
        this.breakableBlockIcon = iconName;
    }

    @Override
    public void registerIcons(IconRegister register) {
        if (this.breakableBlockIcon == null) {
            super.registerIcons(register);
            return;
        }
        this.blockIcon = register.registerIcon(this.breakableBlockIcon);
    }

    // 1.5.2 BlockBreakable.isOpaqueCube (vanilla/client BlockBreakable.java:33-36): ice (FCBlockIce, 79) and glass
    // (BlockGlass) inherit it.
    @Override
    public boolean isOpaqueCube() {
        return false;
    }
}
