package btw.modern;

public class BlockTallGrass extends BlockFlower {

    protected BlockTallGrass(int id) {
        super(id, Material.vine);
    }

    // 1.5.2 BlockTallGrass.getRenderColor (vanilla/client BlockTallGrass.java:55-58): the dead shrub (meta 0) is
    // untinted, grass and fern take ColorizerFoliage.getFoliageColorBasic() (ColorizerFoliage.java:54-57, the
    // constant 4764952; Modern-Common has no colorizer). Read by RenderBlocks.renderBlockAsItemVanilla's inventory tint.
    @Override
    public int getRenderColor(int metadata) {
        return metadata == 0 ? 16777215 : 4764952;
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockTallGrass's icon members (vanilla/client
    // BlockTallGrass.java:22-23, :35-43, :126-134): the dead shrub, tall grass and fern. FCBlockTallGrass (31)
    // inherits both; a held fern drew Block's default, the unlocalized "tallgrass", which maps to grass.
    private static final String[] grassTypes = new String[] {"deadbush", "tallgrass", "fern"};
    private Icon[] iconArray;

    // 1.5.2 BlockTallGrass.getIcon (vanilla/client BlockTallGrass.java:35-43); Block's icon until registerIcons runs
    // (client only)
    @Override
    public Icon getIcon(int side, int metadata) {
        if (this.iconArray == null) {
            return super.getIcon(side, metadata);
        }
        if (metadata >= this.iconArray.length) {
            metadata = 0;
        }
        return this.iconArray[metadata];
    }

    // 1.5.2 BlockTallGrass.registerIcons (vanilla/client BlockTallGrass.java:126-134)
    @Override
    public void registerIcons(IconRegister register) {
        Icon[] icons = new Icon[grassTypes.length];
        for (int i = 0; i < icons.length; ++i) {
            icons[i] = register.registerIcon(grassTypes[i]);
        }
        this.iconArray = icons;
    }
}
