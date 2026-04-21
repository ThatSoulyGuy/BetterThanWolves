package btw.modern;

public class BlockSand extends Block {
    public static boolean fallInstantly = false;

    public BlockSand(int id) {
        super(id, Material.sand);
    }

    public BlockSand(int id, Material material) {
        super(id, material);
    }
}
