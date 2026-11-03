package btw.modern;

public class BlockSapling extends BlockFlower {

    protected BlockSapling(int id) {
        super(id, Material.plants);
    }

    // 1.5.2 BlockSapling.isSameSapling (vanilla/server BlockSapling.java:148-151): same block and same species
    // (meta & 3; FC keeps its growth stage in bits 2-3). FCBlockSapling.growTree needs it for the 2x2 jungle tree.
    public boolean isSameSapling(World world, int i, int j, int k, int metadata) {
        return world.getBlockId(i, j, k) == blockID && (world.getBlockMetadata(i, j, k) & 3) == metadata;
    }

    public void growTree(World world, int i, int j, int k, java.util.Random random) {}

    // 1.5.2 BlockSapling.damageDropped (vanilla/server BlockSapling.java:156-159): the dropped sapling keeps its
    // species and loses FC's growth stage bits. With Block's 0 every sapling dropped by checkFlowerChange or a
    // harvest came out as an oak sapling.
    @Override
    public int damageDropped(int metadata) {
        return metadata & 3;
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockSapling's icon members (vanilla/client BlockSapling.java:23-24,
    // :53-57, :188-196): one sapling per species (meta & 3). FCBlockSapling (6, and the sapling twins) registers and
    // answers its own staged icons (Client FCBlockSapling.java:242-259) without calling these, so nothing draws them.
    private static final String[] field_94370_b = new String[] {"sapling", "sapling_spruce", "sapling_birch",
            "sapling_jungle"};
    private Icon[] saplingIcon;

    // 1.5.2 BlockSapling.getIcon (vanilla/client BlockSapling.java:53-57); Block's icon until registerIcons runs
    // (client only)
    @Override
    public Icon getIcon(int side, int metadata) {
        if (this.saplingIcon == null) {
            return super.getIcon(side, metadata);
        }
        return this.saplingIcon[metadata & 3];
    }

    // 1.5.2 BlockSapling.registerIcons (vanilla/client BlockSapling.java:188-196)
    @Override
    public void registerIcons(IconRegister register) {
        Icon[] icons = new Icon[field_94370_b.length];
        for (int i = 0; i < icons.length; ++i) {
            icons[i] = register.registerIcon(field_94370_b[i]);
        }
        this.saplingIcon = icons;
    }
}
