package btw.modern;

public class BlockDoor extends Block {

    protected BlockDoor(int id, Material material) {
        super(id, material);
    }

    public void onPoweredBlockChange(World world, int i, int j, int k, boolean powered) {}
    public int getFullMetadata(IBlockAccess blockAccess, int i, int j, int k) { return 0; }
    public boolean isDoorOpen(IBlockAccess blockAccess, int i, int j, int k) { return false; }
    public void setDoorRotation(int metadata) {}
    public void OnAIOpenDoor(World world, int i, int j, int k, boolean bOpen) {}

    // 1.5.2 BlockDoor.isOpaqueCube / renderAsNormalBlock / getRenderType / getMobilityFlag (vanilla/client
    // BlockDoor.java:137-140, :151-162, :440-443). FCBlockDoorWood (64, 1036) and FCBlockDoor (71) override none of them.
    // Not restored here: getBlocksMovement (:142-146) answers from getFullMetadata, a stub (0, above) until the door
    // metadata codec lands (HookPolicy "pending:door-codec"); with the stub 1.5.2's body answers closed, as the
    // material default already does, so it comes with the codec.
    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public int getRenderType() {
        return 7;
    }

    @Override
    public int getMobilityFlag() {
        return 1;
    }
}
