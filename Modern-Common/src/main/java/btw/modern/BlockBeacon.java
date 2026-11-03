package btw.modern;

public class BlockBeacon extends BlockContainer {

    protected BlockBeacon(int id) {
        super(id, Material.glass);
    }

    public TileEntity createNewTileEntity(World world) {
        return null;
    }

    // 1.5.2 BlockBeacon.isOpaqueCube / renderAsNormalBlock / getRenderType (vanilla/client BlockBeacon.java:62-81).
    // FCBlockBeacon (138) overrides none of them.
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
        return 34;
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockBeacon's icon members (vanilla/client BlockBeacon.java:19,
    // :87-96): Block's own name (the unlocalized "beacon", as before) and the beacon core icon that
    // RenderBlocks.renderBlockBeacon draws. FCBlockBeacon (138) inherits both.
    private Icon beaconIcon;

    @Override
    public void registerIcons(IconRegister register) {
        super.registerIcons(register);
        this.beaconIcon = register.registerIcon("beacon");
    }

    public Icon getBeaconIcon() {
        return this.beaconIcon;
    }
}
