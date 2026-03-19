package btw.modern;

public class BlockTrapDoor extends Block {

    protected BlockTrapDoor(int id, Material material) {
        super(id, material);
    }

    public static boolean isTrapdoorOpen(int metadata) {
        return (metadata & 4) != 0;
    }

    public static int getDirection(int metadata) {
        return metadata & 3;
    }

    public void onPoweredBlockChange(World world, int i, int j, int k, boolean powered) {}
}
