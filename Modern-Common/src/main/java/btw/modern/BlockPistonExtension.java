package btw.modern;

public class BlockPistonExtension extends Block {

    protected BlockPistonExtension(int id) {
        super(id, Material.piston);
    }

    public static int getDirectionMeta(int metadata) {
        return metadata & 7;
    }
}
