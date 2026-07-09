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

    // Wood species lives in metadata bits 0-1 (orientation in 2-3). Without this the drop
    // stack carries damage 0, so a chopped spruce/birch/jungle log dropped as OAK. The bridge
    // (ItemStackHelper + ProxyRegistry variant table) turns this damage back into the right
    // 1.20.1 log block.
    @Override
    public int damageDropped(int metadata) {
        return metadata & 3;
    }
}
