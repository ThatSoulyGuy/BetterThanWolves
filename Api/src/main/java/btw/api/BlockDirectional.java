package btw.api;

public abstract class BlockDirectional extends Block {

    protected BlockDirectional(int id, Material material) {
        super(id, material);
    }

    public static int getDirection(int metadata) {
        return metadata & 3;
    }
}
