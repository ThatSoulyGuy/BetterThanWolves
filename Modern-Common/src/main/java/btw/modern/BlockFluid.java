package btw.modern;

/**
 * Base class for fluid (liquid) blocks.
 * Mirrors net.minecraft.src.BlockFluid with identical field/method names.
 * FC subclasses (BlockFlowing, BlockStationary) override gameplay methods;
 * this class provides structural/rendering/collision properties.
 */
public abstract class BlockFluid extends Block {

    protected BlockFluid(int id, Material material) {
        super(id, material);
    }

    /**
     * Returns the percentage of the block that is filled with fluid for rendering.
     * Meta 0 = full source block, meta 1-7 = decreasing levels, meta >= 8 = falling.
     */
    public static float getFluidHeightPercent(int meta) {
        if (meta >= 8) {
            meta = 0;
        }
        return (float)(8 - meta) / 9.0F;
    }

    /**
     * Returns the effective flow decay value at the given position.
     * A value of -1 means the material at that position does not match this fluid.
     * Source blocks (meta 0) return 0; flowing blocks return their meta level.
     */
    protected int getEffectiveFlowDecay(IBlockAccess world, int x, int y, int z) {
        if (world.getBlockMaterial(x, y, z) != this.blockMaterial) {
            return -1;
        }

        int meta = world.getBlockMetadata(x, y, z);

        if (meta >= 8) {
            meta = 0;
        }

        return meta;
    }

    /**
     * Returns the flow direction as a Vec3 based on neighboring fluid levels.
     * Simplified stub: returns a zero vector. FC subclasses do not typically
     * override this, but it is called by entity water wheels and similar code.
     */
    public Vec3 getFlowVector(IBlockAccess world, int x, int y, int z) {
        Vec3 vec = world.getWorldVec3Pool().getVecFromPool(0.0D, 0.0D, 0.0D);

        int decay = this.getEffectiveFlowDecay(world, x, y, z);

        for (int side = 0; side < 4; side++) {
            int nx = x;
            int nz = z;

            switch (side) {
                case 0: nx--; break;
                case 1: nz--; break;
                case 2: nx++; break;
                case 3: nz++; break;
            }

            int neighborDecay = this.getEffectiveFlowDecay(world, nx, y, nz);

            if (neighborDecay < 0) {
                if (!world.getBlockMaterial(nx, y, nz).blocksMovement()) {
                    neighborDecay = this.getEffectiveFlowDecay(world, nx, y - 1, nz);

                    if (neighborDecay >= 0) {
                        int diff = neighborDecay - (decay - 8);
                        vec = vec.setComponents(
                            vec.xCoord + (double)(nx - x) * (double)diff,
                            vec.yCoord,
                            vec.zCoord + (double)(nz - z) * (double)diff
                        );
                    }
                }
            } else {
                int diff = neighborDecay - decay;
                vec = vec.setComponents(
                    vec.xCoord + (double)(nx - x) * (double)diff,
                    vec.yCoord,
                    vec.zCoord + (double)(nz - z) * (double)diff
                );
            }
        }

        if (world.getBlockMetadata(x, y, z) >= 8) {
            // Falling fluid: check if flow should be biased downward along slopes
            boolean flowDown = false;

            if (flowDown || this.isBlockSolid(world, x, y + 1, z - 1, 2)
                || this.isBlockSolid(world, x, y + 1, z + 1, 3)
                || this.isBlockSolid(world, x - 1, y + 1, z, 4)
                || this.isBlockSolid(world, x + 1, y + 1, z, 5)) {
                vec = vec.normalize().addVector(0.0D, -6.0D, 0.0D);
            }
        }

        vec = vec.normalize();
        return vec;
    }

    /**
     * Returns the flow direction as an integer.
     * Simplified stub: returns -1 (no dominant direction).
     */
    public static int getFlowDirection(IBlockAccess world, int x, int y, int z, Material material) {
        return -1;
    }

    // --- Rendering/collision overrides ---

    @Override
    public boolean isBlockSolid(IBlockAccess world, int x, int y, int z, int side) {
        return false;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean canCollideCheck(int meta, boolean hitIfLiquid) {
        return hitIfLiquid && meta == 0;
    }

    @Override
    public boolean isCollidable() {
        return false;
    }

    @Override
    public int tickRate(World world) {
        if (this.blockMaterial == Material.water) {
            return 5;
        } else if (this.blockMaterial == Material.lava) {
            return 30;
        }
        return 5;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public int getRenderType() {
        return 4;
    }

    @Override
    public int quantityDropped(java.util.Random random) {
        return 0;
    }

    @Override
    public int getRenderBlockPass() {
        return this.blockMaterial == Material.water ? 1 : 0;
    }

    @Override
    public boolean getBlocksMovement(IBlockAccess blockAccess, int x, int y, int z) {
        return this.blockMaterial != Material.lava;
    }

    @Override
    public void velocityToAddToEntity(World world, int x, int y, int z, Entity entity, Vec3 velocity) {
        Vec3 flow = this.getFlowVector(world, x, y, z);
        velocity.xCoord += flow.xCoord;
        velocity.yCoord += flow.yCoord;
        velocity.zCoord += flow.zCoord;
    }
}
