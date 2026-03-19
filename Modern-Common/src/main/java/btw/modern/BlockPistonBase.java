package btw.modern;

public class BlockPistonBase extends Block {

    public boolean isSticky;
    public Icon topIcon;
    public Icon innerTopIcon;
    public Icon bottomIcon;

    protected BlockPistonBase(int id, boolean isSticky) {
        super(id, Material.piston);
        this.isSticky = isSticky;
    }

    public static boolean isExtended(int metadata) {
        return (metadata & 8) != 0;
    }

    public void updatePistonState(World world, int i, int j, int k) {}
    public boolean canExtend(World world, int i, int j, int k, int facing) { return false; }
    public boolean tryExtend(World world, int i, int j, int k, int facing) { return false; }
    public int GetPistonShovelEjectionDirection(World world, int i, int j, int k, int facing) { return -1; }
    public void ValidatePistonState(World world, int i, int j, int k) {}

    public static int getOrientation(int metadata) {
        return metadata & 7;
    }
}
