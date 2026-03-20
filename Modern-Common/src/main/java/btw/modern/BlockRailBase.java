package btw.modern;

/**
 * Base class for rail blocks (normal rails, powered rails, detector rails, activator rails).
 * Mirrors net.minecraft.src.BlockRailBase with identical field/method names.
 * FC subclasses override gameplay methods; this class provides correct
 * rendering/collision properties and metadata access.
 */
public abstract class BlockRailBase extends Block {

    /** Whether this rail type is a powered variant (powered/detector/activator). */
    private final boolean isPoweredRail;

    protected BlockRailBase(int id) {
        this(id, false);
    }

    protected BlockRailBase(int id, boolean isPowered) {
        super(id, Material.circuits);
        this.isPoweredRail = isPowered;

        this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.125F, 1.0F);
    }

    // --- Rendering properties ---

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
        return 9;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    // --- Rail-specific methods ---

    /**
     * Returns whether this is a powered rail variant.
     * Regular rails return false; powered, detector, and activator rails return true.
     */
    public boolean isPowered() {
        return this.isPoweredRail;
    }

    /**
     * Returns the basic rail metadata (shape/direction), stripping away
     * power/activation bits. For regular rails, uses mask 0xF (supports
     * curves), for powered variants uses mask 0x7 (no curves).
     */
    public static int getBasicRailMetadata(IBlockAccess world, int x, int y, int z) {
        int blockId = world.getBlockId(x, y, z);
        Block block = (blockId >= 0 && blockId < Block.blocksList.length) ? Block.blocksList[blockId] : null;

        if (block instanceof BlockRailBase) {
            int meta = world.getBlockMetadata(x, y, z);
            if (((BlockRailBase) block).isPowered()) {
                return meta & 7;
            } else {
                return meta;
            }
        }

        return 0;
    }

    /**
     * Returns whether this rail can form curves (flexible).
     * Regular rails return true; powered variants return false.
     * Override point for subclasses.
     */
    public boolean isFlexibleRail() {
        return false;
    }

    /**
     * Returns true if the rail can make corners.
     * Only plain rails can curve.
     */
    public boolean canMakeSlopes() {
        return true;
    }

    // --- Placement support ---

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        return world.doesBlockHaveSolidTopSurface(x, y - 1, z);
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        // Subclasses handle rail network updates
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, int neighborBlockID) {
        // Check if the block below still supports the rail
        if (!world.isRemote) {
            if (!world.doesBlockHaveSolidTopSurface(x, y - 1, z)) {
                this.dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
                world.setBlockToAir(x, y, z);
            }
        }
    }
}
