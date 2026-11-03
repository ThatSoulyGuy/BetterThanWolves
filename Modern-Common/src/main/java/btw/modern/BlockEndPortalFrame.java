package btw.modern;

public class BlockEndPortalFrame extends Block {

    protected BlockEndPortalFrame(int id) {
        super(id, Material.rock);
    }

    public static boolean isEnderEyeInserted(int metadata) {
        return (metadata & 4) != 0;
    }

    // 1.5.2 BlockEndPortalFrame.isOpaqueCube / getRenderType (vanilla/client BlockEndPortalFrame.java:58-69).
    // FCBlockEndPortalFrame (120) overrides neither.
    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public int getRenderType() {
        return 26;
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockEndPortalFrame's icon members (vanilla/client
    // BlockEndPortalFrame.java:22-23, :33-52): side, top, end stone below, and the eye. FCBlockEndPortalFrame (120)
    // inherits them; a held frame drew Block's default, the unlocalized "endPortalFrame", which names no texture.
    private Icon endFrameTopIcon;
    private Icon endFrameEyeIcon;

    // 1.5.2 BlockEndPortalFrame.getIcon (vanilla/client BlockEndPortalFrame.java:33-36); Block's icon until
    // registerIcons runs (client only)
    @Override
    public Icon getIcon(int side, int metadata) {
        if (this.endFrameTopIcon == null) {
            return super.getIcon(side, metadata);
        }
        return side == 1 ? this.endFrameTopIcon
                : (side == 0 ? Block.whiteStone.getBlockTextureFromSide(side) : this.blockIcon);
    }

    // 1.5.2 BlockEndPortalFrame.registerIcons (vanilla/client BlockEndPortalFrame.java:42-47)
    @Override
    public void registerIcons(IconRegister register) {
        this.blockIcon = register.registerIcon("endframe_side");
        this.endFrameTopIcon = register.registerIcon("endframe_top");
        this.endFrameEyeIcon = register.registerIcon("endframe_eye");
    }

    // 1.5.2 BlockEndPortalFrame.func_94398_p (vanilla/client BlockEndPortalFrame.java:49-52): the eye
    public Icon func_94398_p() {
        return this.endFrameEyeIcon;
    }
}
