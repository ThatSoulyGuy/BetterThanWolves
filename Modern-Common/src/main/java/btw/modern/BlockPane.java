package btw.modern;

public class BlockPane extends Block {

    // vanilla/client BlockPane.java:41 sets tabDecorations in the constructor: FC's wicker pane, grate and slats
    // (FCBlockPane subclasses, no tab of their own) are listed under Decorations through it.
    protected BlockPane(int id, Material material) {
        super(id, material);
        this.setCreativeTab(CreativeTabs.tabDecorations);
    }

    protected BlockPane(int id, String texture, String sideTexture, Material material, boolean canDropItself) {
        super(id, material);
        this.setCreativeTab(CreativeTabs.tabDecorations);
        this.paneTextureName = texture;
        this.paneSideTextureName = sideTexture;
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockPane keeps the two texture names its constructor takes
    // (field_94402_c and sideTextureIndex, vanilla/client BlockPane.java:25, :32-40) and registers them
    // (registerIcons, :234-238; getSideTextureIndex, :199-202). FCBlockPane passes them up: the wicker pane 1084
    // registers "fcBlockWicker" (Client FCBlockWickerPane.java:26), where Block's default registered its unlocalized
    // name "fcBlockWickerPane", which names no texture (the missing texture in the world and in the hand). The grate,
    // slats and iron bars pass their unlocalized names; the glass pane 102 passes "glass" and "thinglass_top"
    // (BTWLifecycle's replay of 4.B's FCBlockPane(102, ...)), where its unlocalized "thinGlass" named no texture.
    private String paneTextureName;
    private String paneSideTextureName;
    private Icon paneSideIcon;

    // 1.5.2 BlockPane.canThisPaneConnectToThisBlockID (vanilla/client BlockPane.java:208-211): the opaque-cube table,
    // the same pane, or glass. (Asking isOpaqueCube() instead joined every pane to every other pane, door and bed.)
    public final boolean canThisPaneConnectToThisBlockID(int blockID) {
        return blockID >= 0 && blockID < Block.opaqueCubeLookup.length
                && (Block.opaqueCubeLookup[blockID] || blockID == this.blockID
                    || (Block.glass != null && blockID == Block.glass.blockID));
    }

    // 1.5.2 BlockPane.isOpaqueCube / renderAsNormalBlock / getRenderType (vanilla/client BlockPane.java:56-75).
    // FC's panes override none of them: the glass pane (FCBlockPane, 102), iron bars (FCBlockIronBars, 101), the
    // wicker pane (1084), grate (1085) and slats (1086).
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
        return 18;
    }

    // 1.5.2 BlockPane.registerIcons (vanilla/client BlockPane.java:234-238). The port-only 2-argument constructor keeps
    // no names: Block's default registers the unlocalized name, as before.
    @Override
    public void registerIcons(IconRegister register) {
        if (this.paneTextureName == null || this.paneSideTextureName == null) {
            super.registerIcons(register);
            return;
        }
        this.blockIcon = register.registerIcon(this.paneTextureName);
        this.paneSideIcon = register.registerIcon(this.paneSideTextureName);
    }

    // 1.5.2 BlockPane.getSideTextureIndex (vanilla/client BlockPane.java:199-202): the side texture (the slats'
    // fcBlockSlats_side), null until registerIcons runs on the client.
    public Icon getSideTextureIndex() {
        return this.paneSideIcon;
    }
}
