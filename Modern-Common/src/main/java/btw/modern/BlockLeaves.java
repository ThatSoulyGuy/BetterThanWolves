package btw.modern;

public class BlockLeaves extends Block {

    public int[] adjacentTreeBlocks;
    public boolean graphicsLevel;

    protected BlockLeaves(int id) {
        super(id, Material.leaves);
    }
}
