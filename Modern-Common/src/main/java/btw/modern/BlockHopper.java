package btw.modern;

public class BlockHopper extends BlockContainer {

    protected BlockHopper(int id) {
        super(id, Material.iron);
    }

    public TileEntity createNewTileEntity(World world) {
        return null;
    }

    public boolean hasComparatorInputOverride() { return false; }
    public int getComparatorInputOverride(World world, int i, int j, int k, int side) { return 0; }

    public static int getDirectionFromMetadata(int metadata) { return metadata & 7; }

    // 1.5.2 BlockHopper.renderAsNormalBlock / isOpaqueCube (vanilla/client BlockHopper.java:217-229).
    // FCBlockVanillaHopper (154) overrides neither.
    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockHopper's icon members (vanilla/client BlockHopper.java:23-25,
    // :243-246, :280-290): outside, top and inside, and the static lookup RenderBlocks.renderBlockHopper reads.
    // FCBlockVanillaHopper (154) registers and answers its own icons without calling these
    // (Client FCBlockVanillaHopper.java:114-120).
    private Icon hopperIcon;
    private Icon hopperTopIcon;
    private Icon hopperInsideIcon;

    // 1.5.2 BlockHopper.getIcon (vanilla/client BlockHopper.java:243-246); Block's icon until registerIcons runs
    // (client only)
    @Override
    public Icon getIcon(int side, int metadata) {
        if (this.hopperIcon == null || this.hopperTopIcon == null) {
            return super.getIcon(side, metadata);
        }
        return side == 1 ? this.hopperTopIcon : this.hopperIcon;
    }

    // 1.5.2 BlockHopper.registerIcons (vanilla/client BlockHopper.java:280-285)
    @Override
    public void registerIcons(IconRegister register) {
        this.hopperIcon = register.registerIcon("hopper");
        this.hopperTopIcon = register.registerIcon("hopper_top");
        this.hopperInsideIcon = register.registerIcon("hopper_inside");
    }

    // 1.5.2 BlockHopper.getHopperIcon (vanilla/client BlockHopper.java:287-290): Block.hopperBlock's outside or inside
    // icon by name (compared by value; 1.5.2's callers pass these literals)
    public static Icon getHopperIcon(String name) {
        BlockHopper hopper = Block.hopperBlock;
        if (hopper == null) {
            return null;
        }
        return "hopper".equals(name) ? hopper.hopperIcon
                : ("hopper_inside".equals(name) ? hopper.hopperInsideIcon : null);
    }
}
