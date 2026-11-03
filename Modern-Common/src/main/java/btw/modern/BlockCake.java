package btw.modern;

public class BlockCake extends Block {

    // 1.5.2 BlockCake constructor sets the random-tick flag (vanilla/server BlockCake.java:23)
    protected BlockCake(int id) {
        super(id, Material.cake);
        this.setTickRandomly(true);
    }

    // 1.5.2 BlockCake.renderAsNormalBlock / isOpaqueCube (vanilla/client BlockCake.java:101-113). FCBlockCake (92)
    // overrides neither.
    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockCake's icon members (vanilla/client BlockCake.java:21-23,
    // :81-96): top, bottom, side and the cut inner side. FCBlockCake (92) reads getIcon through super.
    private Icon cakeTopIcon;
    private Icon cakeBottomIcon;
    private Icon cakeInnerIcon;

    // 1.5.2 BlockCake.getIcon (vanilla/client BlockCake.java:81-84); Block's icon until registerIcons runs (client
    // only)
    @Override
    public Icon getIcon(int side, int metadata) {
        if (this.cakeTopIcon == null || this.cakeBottomIcon == null || this.cakeInnerIcon == null) {
            return super.getIcon(side, metadata);
        }
        return side == 1 ? this.cakeTopIcon
                : (side == 0 ? this.cakeBottomIcon : (metadata > 0 && side == 4 ? this.cakeInnerIcon : this.blockIcon));
    }

    // 1.5.2 BlockCake.registerIcons (vanilla/client BlockCake.java:90-96)
    @Override
    public void registerIcons(IconRegister register) {
        this.blockIcon = register.registerIcon("cake_side");
        this.cakeInnerIcon = register.registerIcon("cake_inner");
        this.cakeTopIcon = register.registerIcon("cake_top");
        this.cakeBottomIcon = register.registerIcon("cake_bottom");
    }
}
