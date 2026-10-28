package btw.modern;

public class BlockPane extends Block {

    protected BlockPane(int id, Material material) {
        super(id, material);
        this.setCreativeTab(CreativeTabs.tabDecorations);
    }

    protected BlockPane(int id, String texture, String sideTexture, Material material, boolean canDropItself) {
        super(id, material);
        this.setCreativeTab(CreativeTabs.tabDecorations);
        this.setTextureName(texture);
    }

    @Override
    public int getRenderType() {
        return 18;
    }

    public final boolean canThisPaneConnectToThisBlockID(int blockID) {
        return blockID > 0 && Block.blocksList[blockID] != null && Block.blocksList[blockID].isOpaqueCube();
    }
}
