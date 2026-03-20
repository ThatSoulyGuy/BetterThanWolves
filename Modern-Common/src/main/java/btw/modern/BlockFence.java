package btw.modern;

import java.util.List;

/**
 * Base class for fence blocks.
 * Mirrors net.minecraft.src.BlockFence with identical field/method names.
 * FC subclass (FCBlockFence) completely replaces the collision/connection logic
 * with its own model-based approach, so these methods serve as structural stubs
 * that provide correct rendering properties and vanilla-compatible defaults.
 */
public class BlockFence extends Block {

    /** The texture name used for icon registration. */
    private final String textureName;

    public BlockFence(int id, String textureName, Material material) {
        super(id, material);
        this.textureName = textureName;
    }

    protected BlockFence(int id, Material material) {
        super(id, material);
        this.textureName = null;
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
        return 11;
    }

    @Override
    public boolean getBlocksMovement(IBlockAccess blockAccess, int x, int y, int z) {
        return false;
    }

    // --- Connection logic ---

    /**
     * Checks whether a fence at the given coordinates can connect to the
     * neighbor at the specified position.
     * Returns true if the neighbor is the same block type, a fence gate,
     * or an opaque normal-rendering block (excluding pumpkin material).
     *
     * Note: FC's FCBlockFence replaces this with CanConnectToBlockAt().
     */
    public boolean canConnectFenceTo(IBlockAccess world, int x, int y, int z) {
        int blockId = world.getBlockId(x, y, z);

        if (blockId == this.blockID || blockId == Block.fenceGate.blockID) {
            return true;
        }

        Block block = Block.blocksList[blockId];

        if (block != null && block.blockMaterial.isOpaque() && block.renderAsNormalBlock()) {
            return block.blockMaterial != Material.pumpkin;
        }

        return false;
    }

    // --- Block bounds ---

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess blockAccess, int x, int y, int z) {
        boolean connectNegZ = canConnectFenceTo(blockAccess, x, y, z - 1);
        boolean connectPosZ = canConnectFenceTo(blockAccess, x, y, z + 1);
        boolean connectNegX = canConnectFenceTo(blockAccess, x - 1, y, z);
        boolean connectPosX = canConnectFenceTo(blockAccess, x + 1, y, z);

        float minX = 0.375F;
        float maxX = 0.625F;
        float minZ = 0.375F;
        float maxZ = 0.625F;

        if (connectNegZ) {
            minZ = 0.0F;
        }
        if (connectPosZ) {
            maxZ = 1.0F;
        }
        if (connectNegX) {
            minX = 0.0F;
        }
        if (connectPosX) {
            maxX = 1.0F;
        }

        this.setBlockBounds(minX, 0.0F, minZ, maxX, 1.0F, maxZ);
    }

    // --- Collision ---

    @Override
    public void addCollisionBoxesToList(World world, int x, int y, int z,
            AxisAlignedBB mask, List list, Entity entity) {
        boolean connectNegZ = canConnectFenceTo(world, x, y, z - 1);
        boolean connectPosZ = canConnectFenceTo(world, x, y, z + 1);
        boolean connectNegX = canConnectFenceTo(world, x - 1, y, z);
        boolean connectPosX = canConnectFenceTo(world, x + 1, y, z);

        float minX = 0.375F;
        float maxX = 0.625F;
        float minZ = 0.375F;
        float maxZ = 0.625F;

        if (connectNegZ) {
            minZ = 0.0F;
        }
        if (connectPosZ) {
            maxZ = 1.0F;
        }
        if (connectNegX) {
            minX = 0.0F;
        }
        if (connectPosX) {
            maxX = 1.0F;
        }

        // Fence post + connecting rails collision (1.5 blocks tall)
        this.setBlockBounds(minX, 0.0F, minZ, maxX, 1.5F, maxZ);
        super.addCollisionBoxesToList(world, x, y, z, mask, list, entity);

        // Reset bounds
        this.setBlockBounds(minX, 0.0F, minZ, maxX, 1.0F, maxZ);
    }
}
