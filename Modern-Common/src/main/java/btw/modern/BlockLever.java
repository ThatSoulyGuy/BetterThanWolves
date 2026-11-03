package btw.modern;

public class BlockLever extends Block {

    protected BlockLever(int id) {
        super(id, Material.circuits);
    }

    // 1.5.2 BlockLever.getCollisionBoundingBoxFromPool (vanilla/client BlockLever.java:29-32): levers have no
    // collision box (FCBlockLever overrides only the bounds methods)
    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    // 1.5.2 BlockLever.isOpaqueCube / renderAsNormalBlock (vanilla/client BlockLever.java:38-49).
    // With the inherited true, World.isBlockNormalCube reported this passable block as solid, so an FC mob's
    // eye inside the cell counted as being inside an opaque block (Entity.isEntityInsideOpaqueBlock -> inWall).
    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    // 1.5.2 BlockLever.getRenderType (vanilla/client BlockLever.java:54-57). FCBlockLever (69) does not override it.
    @Override
    public int getRenderType() {
        return 12;
    }
}
