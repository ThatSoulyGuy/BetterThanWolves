package btw.modern;

public class BlockCauldron extends Block {

    protected BlockCauldron(int id) {
        super(id, Material.iron);
    }

    // 1.5.2 BlockCauldron.isOpaqueCube / getRenderType / renderAsNormalBlock (vanilla/client BlockCauldron.java:88-107).
    // FCBlockVanillaCauldron (118) overrides none of them.
    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public int getRenderType() {
        return 24;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockCauldron's icon members (vanilla/client
    // BlockCauldron.java:22-24, :34-54): top, bottom and side, and the inner and bottom icons
    // RenderBlocks.renderBlockCauldron reads by name. FCBlockVanillaCauldron (118) inherits them.
    private Icon cauldronInnerIcon;
    private Icon cauldronTopIcon;
    private Icon cauldronBottomIcon;

    // 1.5.2 BlockCauldron.getIcon (vanilla/client BlockCauldron.java:34-37); Block's icon until registerIcons runs
    // (client only)
    @Override
    public Icon getIcon(int side, int metadata) {
        if (this.cauldronTopIcon == null || this.cauldronBottomIcon == null) {
            return super.getIcon(side, metadata);
        }
        return side == 1 ? this.cauldronTopIcon : (side == 0 ? this.cauldronBottomIcon : this.blockIcon);
    }

    // 1.5.2 BlockCauldron.registerIcons (vanilla/client BlockCauldron.java:43-49)
    @Override
    public void registerIcons(IconRegister register) {
        this.cauldronInnerIcon = register.registerIcon("cauldron_inner");
        this.cauldronTopIcon = register.registerIcon("cauldron_top");
        this.cauldronBottomIcon = register.registerIcon("cauldron_bottom");
        this.blockIcon = register.registerIcon("cauldron_side");
    }

    // 1.5.2 BlockCauldron.func_94375_b (vanilla/client BlockCauldron.java:51-54): Block.cauldron's inner or bottom
    // icon. Port: the cauldron is read at its id, 118 (Modern-Common keeps no Block.cauldron field), and the name is
    // compared by value (1.5.2's callers pass these literals).
    public static Icon func_94375_b(String name) {
        Block block = Block.blocksList[118];
        BlockCauldron cauldron = block instanceof BlockCauldron ? (BlockCauldron) block : null;
        if (cauldron == null) {
            return null;
        }
        return "cauldron_inner".equals(name) ? cauldron.cauldronInnerIcon
                : ("cauldron_bottom".equals(name) ? cauldron.cauldronBottomIcon : null);
    }
}
