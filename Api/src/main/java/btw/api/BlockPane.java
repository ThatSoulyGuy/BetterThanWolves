package btw.api;

public class BlockPane extends Block {

    protected BlockPane(int id, Material material) {
        super(id, material);
    }

    protected BlockPane(int id, String texture, String sideTexture, Material material, boolean canDropItself) {
        super(id, material);
    }

    public final boolean canThisPaneConnectToThisBlockID(int blockID) {
        return blockID > 0 && Block.blocksList[blockID] != null && Block.blocksList[blockID].isOpaqueCube();
    }
}
