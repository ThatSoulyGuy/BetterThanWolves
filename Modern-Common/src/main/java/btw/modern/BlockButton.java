package btw.modern;

import java.util.List;

public abstract class BlockButton extends Block {

    /**
     * 1.5.2 BlockButton.sensible (vanilla/client BlockButton.java:22-31): the button an arrow presses. FCBlockButtonWood
     * (143) passes true, FCBlockButtonStone (77) false (Client FCBlockButtonWood.java, FCBlockButtonStone.java).
     */
    private final boolean sensible;

    // 1.5.2 BlockButton constructor sets the random-tick flag (vanilla/server BlockButton.java:27); both port
    // constructors stand for that one 1.5.2 constructor. Flag only: the button tick body stays 1.20.1's.
    protected BlockButton(int id) {
        super(id, Material.circuits);
        this.setTickRandomly(true);
        this.sensible = false;
    }

    // FCMOD-INTEGRATION(1.20.1): fc_redstone: the sensible flag is kept (vanilla/client BlockButton.java:24-31), so FC's
    // arrows press the wooden button as in 4.B
    protected BlockButton(int id, boolean sensitive) {
        super(id, Material.circuits);
        this.setTickRandomly(true);
        this.sensible = sensitive;
    }

    // 1.5.2 BlockButton.getCollisionBoundingBoxFromPool (vanilla/client BlockButton.java:37-40): buttons have
    // no collision box (FCBlockButton overrides only the bounds methods)
    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    // 1.5.2 BlockButton.isOpaqueCube / renderAsNormalBlock (vanilla/client BlockButton.java:54-65).
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

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockButton.registerIcons (vanilla/client BlockButton.java:417)
    // registers nothing: FCBlockButton (77 stone, 143 wood) answers getIcon with the stone or planks icon itself.
    @Override
    public void registerIcons(IconRegister register) {}

    // FCMOD-INTEGRATION(1.20.1): fc_redstone: 1.5.2's arrow contact (vanilla/client BlockButton.java:45-48, :342-385, :189-221). FC's
    // arrows (ProxyEntity puppets, never AbstractArrows) reach a button only through FC's own Entity.doBlockCollisions
    // (vanilla/client EntityArrow.java:473); the press and its release stay 1.20.1's ButtonBlock (its scheduled tick, whose
    // arrow test ButtonBlockMixin widens to FC's arrows), so no updateTick is ported here

    // 1.5.2 BlockButton.tickRate (vanilla/client BlockButton.java:45-48)
    @Override
    public int tickRate(World world) {
        return this.sensible ? 30 : 20;
    }

    // 1.5.2 BlockButton.onEntityCollidedWithBlock (vanilla/client BlockButton.java:342-354)
    @Override
    public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
        if (!world.isRemote) {
            if (this.sensible) {
                if ((world.getBlockMetadata(x, y, z) & 8) == 0) {
                    this.func_82535_o(world, x, y, z);
                }
            }
        }
    }

    /**
     * 1.5.2 BlockButton.func_82535_o (vanilla/client BlockButton.java:356-385): the button is pressed while an
     * EntityArrow is inside its bounds and released when none is; a pressed button re-checks itself every tickRate.
     * The bounds are computed here rather than written into the shared bounds fields (func_82534_e); a metadata with no
     * 1.5.2 wall orientation (a 1.20.1 floor or ceiling button, which 1.5.2 does not have) is skipped.
     */
    private void func_82535_o(World world, int x, int y, int z) {
        int metadata = world.getBlockMetadata(x, y, z);
        int orientation = metadata & 7;
        boolean wasPressed = (metadata & 8) != 0;
        double[] bounds = func_82534_e(metadata);
        if (bounds == null) {
            return;
        }
        List arrows = world.getEntitiesWithinAABB(EntityArrow.class, AxisAlignedBB.getAABBPool().getAABB(
                (double) x + bounds[0], (double) y + bounds[1], (double) z + bounds[2],
                (double) x + bounds[3], (double) y + bounds[4], (double) z + bounds[5]));
        boolean isPressed = !arrows.isEmpty();

        if (isPressed && !wasPressed) {
            world.setBlockMetadata(x, y, z, orientation | 8, 3);
            this.func_82536_d(world, x, y, z, orientation);
            world.markBlockRangeForRenderUpdate(x, y, z, x, y, z);
            world.playSoundEffect((double) x + 0.5D, (double) y + 0.5D, (double) z + 0.5D, "random.click", 0.3F, 0.6F);
        }

        if (!isPressed && wasPressed) {
            world.setBlockMetadata(x, y, z, orientation, 3);
            this.func_82536_d(world, x, y, z, orientation);
            world.markBlockRangeForRenderUpdate(x, y, z, x, y, z);
            world.playSoundEffect((double) x + 0.5D, (double) y + 0.5D, (double) z + 0.5D, "random.click", 0.3F, 0.5F);
        }

        if (isPressed) {
            world.scheduleBlockUpdate(x, y, z, this.blockID, this.tickRate(world));
        }
    }

    /**
     * 1.5.2 BlockButton.func_82534_e (vanilla/client BlockButton.java:189-221): the button's bounds for a metadata, as
     * {minX, minY, minZ, maxX, maxY, maxZ}, or null for an orientation outside 1-4.
     */
    private static double[] func_82534_e(int metadata) {
        int orientation = metadata & 7;
        boolean pressed = (metadata & 8) > 0;
        float minY = 0.375F;
        float maxY = 0.625F;
        float halfWidth = 0.1875F;
        float depth = pressed ? 0.0625F : 0.125F;

        switch (orientation) {
            case 1: return new double[] {0.0F, minY, 0.5F - halfWidth, depth, maxY, 0.5F + halfWidth};
            case 2: return new double[] {1.0F - depth, minY, 0.5F - halfWidth, 1.0F, maxY, 0.5F + halfWidth};
            case 3: return new double[] {0.5F - halfWidth, minY, 0.0F, 0.5F + halfWidth, maxY, depth};
            case 4: return new double[] {0.5F - halfWidth, minY, 1.0F - depth, 0.5F + halfWidth, maxY, 1.0F};
            default: return null;
        }
    }

    /**
     * 1.5.2 BlockButton.func_82536_d (vanilla/client BlockButton.java:387-411): the button's cell and the block it is
     * attached to hear the change.
     */
    private void func_82536_d(World world, int x, int y, int z, int orientation) {
        world.notifyBlocksOfNeighborChange(x, y, z, this.blockID);

        if (orientation == 1) {
            world.notifyBlocksOfNeighborChange(x - 1, y, z, this.blockID);
        } else if (orientation == 2) {
            world.notifyBlocksOfNeighborChange(x + 1, y, z, this.blockID);
        } else if (orientation == 3) {
            world.notifyBlocksOfNeighborChange(x, y, z - 1, this.blockID);
        } else if (orientation == 4) {
            world.notifyBlocksOfNeighborChange(x, y, z + 1, this.blockID);
        } else {
            world.notifyBlocksOfNeighborChange(x, y - 1, z, this.blockID);
        }
    }
}
