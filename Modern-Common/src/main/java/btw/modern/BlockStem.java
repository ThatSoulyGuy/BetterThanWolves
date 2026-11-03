package btw.modern;

public class BlockStem extends BlockFlower {

    public Block fruitType;

    protected BlockStem(int id) {
        super(id, Material.plants);
    }

    protected BlockStem(int id, Block fruitBlock) {
        super(id, Material.plants);
        this.fruitType = fruitBlock;
        // 1.5.2 BlockStem constructor (vanilla/server BlockStem.java:32)
        setTickRandomly(true);
    }

    // 1.5.2 BlockStem.getRenderType (vanilla/client BlockStem.java:229-232). FCBlockStem draws itself in-world
    // through its RenderBlock override (renderBlockStem); the id selects RenderBlocks.renderBlockAsItemVanilla's
    // stem branch and makes DoesItemRenderAsBlock false.
    @Override
    public int getRenderType() {
        return 19;
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockStem's icon members (vanilla/client BlockStem.java:28,
    // :303-312): the straight stem (blockIcon) and the bent one RenderBlocks.renderBlockStem reads. FCBlockStem
    // (104, 105) inherits them.
    private Icon stemBentIcon;

    @Override
    public void registerIcons(IconRegister register) {
        this.blockIcon = register.registerIcon("stem_straight");
        this.stemBentIcon = register.registerIcon("stem_bent");
    }

    public Icon func_94368_p() {
        return this.stemBentIcon;
    }
}
