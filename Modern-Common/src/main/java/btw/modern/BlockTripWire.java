package btw.modern;

public class BlockTripWire extends Block {

    // 1.5.2 BlockTripWire constructor sets the random-tick flag (vanilla/server BlockTripWire.java:26). Flag only: the
    // tripwire tick body stays 1.20.1's.
    protected BlockTripWire(int id) {
        super(id, Material.circuits);
        this.setTickRandomly(true);
    }

    // 1.5.2 BlockTripWire.getCollisionBoundingBoxFromPool (vanilla/client BlockTripWire.java:42-45): tripwire has
    // no collision box (FCBlockTripWire overrides only the bounds methods)
    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    // 1.5.2 BlockTripWire.isOpaqueCube / renderAsNormalBlock (vanilla/client BlockTripWire.java:51-62).
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

    // 1.5.2 BlockTripWire.getRenderBlockPass / getRenderType (vanilla/client BlockTripWire.java:67-78).
    // FCBlockTripWire (132) overrides neither.
    @Override
    public int getRenderBlockPass() {
        return 1;
    }

    @Override
    public int getRenderType() {
        return 30;
    }
}
