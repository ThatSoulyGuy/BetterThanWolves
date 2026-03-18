package btw.api;

public class BlockDoor extends Block {

    protected BlockDoor(int id, Material material) {
        super(id, material);
    }

    public void onPoweredBlockChange(World world, int i, int j, int k, boolean powered) {}
    public int getFullMetadata(IBlockAccess blockAccess, int i, int j, int k) { return 0; }
    public boolean isDoorOpen(IBlockAccess blockAccess, int i, int j, int k) { return false; }
    public void setDoorRotation(int metadata) {}
    public void OnAIOpenDoor(World world, int i, int j, int k, boolean bOpen) {}
}
