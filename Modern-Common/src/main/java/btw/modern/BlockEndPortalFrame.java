package btw.modern;

public class BlockEndPortalFrame extends Block {

    protected BlockEndPortalFrame(int id) {
        super(id, Material.rock);
    }

    public static boolean isEnderEyeInserted(int metadata) {
        return (metadata & 4) != 0;
    }
}
