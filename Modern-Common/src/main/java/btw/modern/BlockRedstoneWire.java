package btw.modern;

public class BlockRedstoneWire extends Block {

    protected BlockRedstoneWire(int id) {
        super(id, Material.circuits);
    }

    // 1.5.2 BlockRedstoneWire.getCollisionBoundingBoxFromPool (vanilla/client BlockRedstoneWire.java:45-48):
    // redstone dust has no collision box (FCBlockRedstoneWire does not override it)
    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    // 1.5.2 BlockRedstoneWire.isOpaqueCube / renderAsNormalBlock (vanilla/client BlockRedstoneWire.java:54-65).
    // With the inherited true, World.isBlockNormalCube reported this passable block as solid, so an FC mob's
    // eye inside the cell counted as being inside an opaque block (Entity.isEntityInsideOpaqueBlock -> inWall).
    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    // 1.5.2 BlockRedstoneWire.getRenderType (vanilla/client BlockRedstoneWire.java:70-73). FCBlockRedstoneWire (55)
    // does not override it.
    @Override
    public int getRenderType() {
        return 5;
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockRedstoneWire's icon members (vanilla/client
    // BlockRedstoneWire.java:30-33, :557-569): the cross (also blockIcon), line and their overlays, and the static
    // lookup RenderBlocks.renderBlockRedstoneWire reads. FCBlockRedstoneWire (55) inherits them; a wire lifted by a
    // platform draws getIcon(0, meta) (Client FCClientRenderBlockLiftedByPlatform.java:94): "redstoneDust_cross",
    // the sprite the build composes from 1.20.1's own dust textures (Forge/gradle/legacy-icon-textures.gradle).
    // Block's default registered the unlocalized "redstoneDust", which names no texture.
    private Icon crossIcon;
    private Icon lineIcon;
    private Icon crossOverlayIcon;
    private Icon lineOverlayIcon;

    @Override
    public void registerIcons(IconRegister register) {
        this.crossIcon = register.registerIcon("redstoneDust_cross");
        this.lineIcon = register.registerIcon("redstoneDust_line");
        this.crossOverlayIcon = register.registerIcon("redstoneDust_cross_overlay");
        this.lineOverlayIcon = register.registerIcon("redstoneDust_line_overlay");
        this.blockIcon = this.crossIcon;
    }

    // 1.5.2 BlockRedstoneWire.func_94409_b (vanilla/client BlockRedstoneWire.java:566-569): Block.redstoneWire's icons
    // by name (compared by value; 1.5.2's callers pass these literals)
    public static Icon func_94409_b(String name) {
        BlockRedstoneWire wire = Block.redstoneWire;
        if (wire == null) {
            return null;
        }
        return "redstoneDust_cross".equals(name) ? wire.crossIcon
                : ("redstoneDust_line".equals(name) ? wire.lineIcon
                : ("redstoneDust_cross_overlay".equals(name) ? wire.crossOverlayIcon
                : ("redstoneDust_line_overlay".equals(name) ? wire.lineOverlayIcon : null)));
    }
}
