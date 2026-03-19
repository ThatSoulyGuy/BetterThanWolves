package btw.modern;

public class BlockLog extends Block {

    protected BlockLog(int id) {
        super(id, Material.wood);
    }

    protected BlockLog(int id, Material material) {
        super(id, material);
    }

    public static final String[] woodType = new String[] {"oak", "spruce", "birch", "jungle"};
    public static int limitToValidMetadata(int metadata) { return metadata & 3; }
}
