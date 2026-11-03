package btw.modern;

public class BlockPistonExtension extends Block {

    protected BlockPistonExtension(int id) {
        super(id, Material.piston);
    }

    public static int getDirectionMeta(int metadata) {
        return metadata & 7;
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockPistonExtension's icon members (vanilla/client
    // BlockPistonExtension.java:23, :32-40, :69-79): the head face (a set head texture, else the piston base's plain or
    // sticky top), the back and the sides from the piston base, and no icon of its own. FCBlockPistonExtension (34)
    // inherits them.
    private Icon headTexture = null;

    public void setHeadTexture(Icon icon) {
        this.headTexture = icon;
    }

    public void clearHeadTexture() {
        this.headTexture = null;
    }

    // 1.5.2 BlockPistonExtension.getIcon (vanilla/client BlockPistonExtension.java:69-73)
    @Override
    public Icon getIcon(int side, int metadata) {
        int facing = getDirectionMeta(metadata);
        return side == facing ? (this.headTexture != null ? this.headTexture
                        : ((metadata & 8) != 0 ? BlockPistonBase.func_94496_b("piston_top_sticky")
                        : BlockPistonBase.func_94496_b("piston_top")))
                : (facing < 6 && side == Facing.oppositeSide[facing] ? BlockPistonBase.func_94496_b("piston_top")
                : BlockPistonBase.func_94496_b("piston_side"));
    }

    // 1.5.2 BlockPistonExtension.registerIcons (vanilla/client BlockPistonExtension.java:79)
    @Override
    public void registerIcons(IconRegister register) {}

    // 1.5.2 BlockPistonExtension.getRenderType / isOpaqueCube / renderAsNormalBlock (vanilla/client
    // BlockPistonExtension.java:84-104). FCBlockPistonExtension (34) overrides none of them.
    @Override
    public int getRenderType() {
        return 17;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }
}
