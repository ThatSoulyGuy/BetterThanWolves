package btw.modern;

public class BlockBrewingStand extends BlockContainer {

    protected BlockBrewingStand(int id) {
        super(id, Material.iron);
    }

    public TileEntity createNewTileEntity(World world) {
        return null;
    }

    // 1.5.2 BlockBrewingStand.isOpaqueCube / getRenderType / renderAsNormalBlock (vanilla/client
    // BlockBrewingStand.java:34-45, :58-61). FCBlockBrewingStand (117) overrides none of them.
    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public int getRenderType() {
        return 25;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockBrewingStand's icon members (vanilla/client
    // BlockBrewingStand.java:23, :210-219): Block's own name and the base icon RenderBlocks.renderBlockBrewingStand
    // draws. FCBlockBrewingStand (117) inherits both.
    private Icon brewingStandBaseIcon;

    @Override
    public void registerIcons(IconRegister register) {
        super.registerIcons(register);
        this.brewingStandBaseIcon = register.registerIcon("brewingStand_base");
    }

    public Icon getBrewingStandIcon() {
        return this.brewingStandBaseIcon;
    }
}
