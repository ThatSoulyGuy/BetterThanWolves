package btw.modern;

/**
 * FCMOD-INTEGRATION(1.20.1): attach_support_rules (INTEGRATIONS #055)
 *
 * <p>1.5.2 BlockTorch's placement rule (vanilla/client BlockTorch.java:30-119, :136-162) on FC's World, for the torch ids
 * FC keeps as 1.5.2 blocks: 4.B's redstone torches 75 and 76 are 1.5.2 BlockRedstoneTorch (vanilla/client
 * Block.java:244-245), a BlockTorch, while the port's blocksList holds Modern-Common placeholders there. Without this
 * rule FC's item route stood the redstone torch in the cell beside any clicked face whatever was below it (in mid-air
 * over a drop), and the placeholder's full-cube collision box refused it in any cell a living entity overlapped, the
 * placing player's own included.</p>
 * <ul>
 *   <li>{@link #canPlaceTorchOn}: a solid top surface, or a fence, nether fence, glass or cobblestone wall top
 *       (vanilla/client BlockTorch.java:65-76);</li>
 *   <li>{@link #canPlaceBlockAt}: a normal cube on one of the four sides, or a torch support below (:81-84), FC's
 *       isBlockNormalCubeDefault being World.isBlockNormalCube (FCMOD, vanilla/server World.java:2600-2614);</li>
 *   <li>{@link #onBlockPlaced}: the clicked face (:89-119); when it is not a valid support, 1.5.2's onBlockAdded
 *       orientation search (:136-162), here because the 1.20.1 block's onPlace never reaches FC;</li>
 *   <li>no collision box (:31-34), and neither opaque nor a normal-block render (:40-51);</li>
 *   <li>{@link #onPostBlockPlaced}: the six neighbours of a wall torch FC's route placed hear it, as 1.5.2's
 *       placement told them (World.setBlock flag 1).</li>
 * </ul>
 * <p>The same predicates answer 1.20.1's own placement and survival of the redstone torch and redstone wall torch:
 * btw.forge.FcAttachSupport takes this class as its third attachment family. No tick body and no random-tick flag: the
 * redstone torch's ticks stay 1.20.1's (TickPolicy; BridgeSelfAudit's known flag gaps 75 and 76).</p>
 */
// FCMOD-INTEGRATION(1.20.1): attach_support_rules: 1.5.2 BlockTorch's placement rule for the redstone torch ids FC keeps as 1.5.2 blocks (75, 76)
public class BlockTorch extends Block {

    /** 1.5.2 Block.fence, Block.netherFence, Block.glass and Block.cobblestoneWall (vanilla/client Block.java). */
    private static final int FENCE_ID = 85;
    private static final int NETHER_FENCE_ID = 113;
    private static final int GLASS_ID = 20;
    private static final int COBBLESTONE_WALL_ID = 139;

    protected BlockTorch(int id, Material material) {
        super(id, material);
    }

    // 1.5.2 BlockTorch.getCollisionBoundingBoxFromPool (vanilla/client BlockTorch.java:30-34)
    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    // 1.5.2 BlockTorch.isOpaqueCube / renderAsNormalBlock (vanilla/client BlockTorch.java:40-51)
    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    /**
     * 1.5.2 BlockTorch.canPlaceTorchOn (vanilla/client BlockTorch.java:65-76, private there): a solid top surface, or
     * the top of a fence, nether fence, glass or cobblestone wall. The id is the support's
     * ({@link World#getSupportBlockId}): a modern fence or wall reads as its BTW relative, the oak fence or the
     * cobblestone wall (btw.forge.FcAttachSupport's support relatives).
     */
    public boolean canPlaceTorchOn(World world, int x, int y, int z) {
        if (world.doesBlockHaveSolidTopSurface(x, y, z)) {
            return true;
        }
        // FCMOD-INTEGRATION(1.20.1): support_relatives: 1.5.2's id list reads the support's id, a support relative's BTW id
        int id = world.getSupportBlockId(x, y, z);
        return id == FENCE_ID || id == NETHER_FENCE_ID || id == GLASS_ID || id == COBBLESTONE_WALL_ID;
    }

    // 1.5.2 BlockTorch.canPlaceBlockAt (vanilla/client BlockTorch.java:81-84)
    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        return world.isBlockNormalCube(x - 1, y, z)
                || world.isBlockNormalCube(x + 1, y, z)
                || world.isBlockNormalCube(x, y, z - 1)
                || world.isBlockNormalCube(x, y, z + 1)
                || this.canPlaceTorchOn(world, x, y - 1, z);
    }

    // 1.5.2 BlockTorch.onBlockPlaced (vanilla/client BlockTorch.java:89-119), then onBlockAdded's search
    // (vanilla/client BlockTorch.java:136-162) for a clicked face that holds no torch
    @Override
    public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int metadata) {
        int orientation = metadata;

        if (side == 1 && this.canPlaceTorchOn(world, x, y - 1, z)) {
            orientation = 5;
        }

        if (side == 2 && world.isBlockNormalCube(x, y, z + 1)) {
            orientation = 4;
        }

        if (side == 3 && world.isBlockNormalCube(x, y, z - 1)) {
            orientation = 3;
        }

        if (side == 4 && world.isBlockNormalCube(x + 1, y, z)) {
            orientation = 2;
        }

        if (side == 5 && world.isBlockNormalCube(x - 1, y, z)) {
            orientation = 1;
        }

        if (orientation == 0) {
            orientation = this.findOrientation(world, x, y, z);
        }

        return orientation;
    }

    /**
     * 1.5.2 BlockTorch.onBlockAdded's search (vanilla/client BlockTorch.java:136-162): the first wall among x-1, x+1, z-1
     * and z+1 that is a normal cube, else standing on a torch support below; 0 when there is none (canPlaceBlockAt has
     * already refused such a cell).
     */
    private int findOrientation(World world, int x, int y, int z) {
        if (world.isBlockNormalCube(x - 1, y, z)) {
            return 1;
        } else if (world.isBlockNormalCube(x + 1, y, z)) {
            return 2;
        } else if (world.isBlockNormalCube(x, y, z - 1)) {
            return 3;
        } else if (world.isBlockNormalCube(x, y, z + 1)) {
            return 4;
        } else if (this.canPlaceTorchOn(world, x, y - 1, z)) {
            return 5;
        }
        return 0;
    }

    /**
     * After FC's item route has set the torch (Modern-Common ItemBlock.onItemUse, as Common FCItemPlacesAsBlock.onItemUse):
     * a wall torch's six neighbours hear it. 1.5.2's placement told them through World.setBlock's flag 1
     * (vanilla/server World.java:477-479 -> notifyBlockChange :649-652), but the bridge sets FC's wall torch states
     * without that flag (btw.forge.WorldBridge.setBlock, kept for turntable rotations), and 1.20.1's
     * RedstoneTorchBlock.onPlace only updates the cells around those neighbours. Without this, the dust, lamp or piston
     * beside a redstone wall torch FC's route hung up kept its old power until some other update reached it. A standing
     * torch (orientation 5) is set with the flag and needs nothing here.
     */
    @Override
    public void onPostBlockPlaced(World world, int x, int y, int z, int metadata) {
        super.onPostBlockPlaced(world, x, y, z, metadata);
        int orientation = metadata & 7;
        if (orientation >= 1 && orientation <= 4) {
            world.notifyBlocksOfNeighborChange(x, y, z, this.blockID);
        }
    }
}
