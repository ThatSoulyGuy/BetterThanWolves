package btw.modern;

public class BlockDaylightDetector extends BlockContainer {

    protected BlockDaylightDetector(int id) {
        super(id, Material.wood);
    }

    public TileEntity createNewTileEntity(World world) {
        return null;
    }

    public void updateLightLevel(World world, int i, int j, int k) {}

    // 1.5.2 BlockDaylightDetector.renderAsNormalBlock / isOpaqueCube (vanilla/client
    // BlockDaylightDetector.java:103-115). FCBlockDaylightDetector (151) overrides neither.
    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockDaylightDetector's icon members (vanilla/client
    // BlockDaylightDetector.java:21, :136-149): the top and the side. FCBlockDaylightDetector (151) registers and
    // answers its own (Client FCBlockDaylightDetector.java:59-68) without calling these, so nothing draws them.
    private Icon[] iconArray = new Icon[2];

    // 1.5.2 BlockDaylightDetector.getIcon (vanilla/client BlockDaylightDetector.java:136-139); Block's icon until
    // registerIcons runs (client only)
    @Override
    public Icon getIcon(int side, int metadata) {
        Icon top = this.iconArray[0];
        Icon sideIcon = this.iconArray[1];
        if (top == null || sideIcon == null) {
            return super.getIcon(side, metadata);
        }
        return side == 1 ? top : sideIcon;
    }

    // 1.5.2 BlockDaylightDetector.registerIcons (vanilla/client BlockDaylightDetector.java:145-149)
    @Override
    public void registerIcons(IconRegister register) {
        this.iconArray[0] = register.registerIcon("daylightDetector_top");
        this.iconArray[1] = register.registerIcon("daylightDetector_side");
    }
}
