package btw.modern;

public class BlockFence extends Block {
    public BlockFence(int id, String textureName, Material material) {
        super(id, material);
    }

    protected BlockFence(int id, Material material) {
        super(id, material);
    }
}
