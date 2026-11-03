package btw.modern;

public class BlockVine extends Block {

    protected BlockVine(int id) {
        super(id, Material.vine);
        // 1.5.2 BlockVine constructor (vanilla/server BlockVine.java:23): FCBlockVine.updateTick spreads the vine
        setTickRandomly(true);
    }

    // 1.5.2 BlockVine.getCollisionBoundingBoxFromPool (vanilla/client BlockVine.java:136-139): vines have no
    // collision box (FCBlockVine overrides only the bounds methods)
    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    // 1.5.2 BlockVine.isOpaqueCube / renderAsNormalBlock (vanilla/client BlockVine.java:48-59).
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

    // 1.5.2 BlockVine.getRenderType (vanilla/client BlockVine.java:39-42). FCBlockVine (106) does not override it.
    @Override
    public int getRenderType() {
        return 20;
    }
}
