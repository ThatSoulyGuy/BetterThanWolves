package btw.modern;

public class BlockFenceGate extends BlockDirectional {

    protected BlockFenceGate(int id) {
        super(id, Material.wood);
    }

    public static boolean isFenceGateOpen(int metadata) {
        return (metadata & 4) != 0;
    }
}
