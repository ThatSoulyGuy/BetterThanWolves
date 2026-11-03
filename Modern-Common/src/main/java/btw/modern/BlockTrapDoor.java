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

    // 1.5.2 BlockTrapDoor.isOpaqueCube / renderAsNormalBlock (vanilla/client BlockTrapDoor.java:32-43).
    // FCBlockTrapDoor (96) overrides neither.
    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }
}
