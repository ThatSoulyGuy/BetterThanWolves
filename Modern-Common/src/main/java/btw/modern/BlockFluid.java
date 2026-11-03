package btw.modern;

/**
 * Base class for fluid (liquid) blocks.
 * Mirrors net.minecraft.src.BlockFluid with identical field/method names.
 * FC subclasses (BlockFlowing, BlockStationary) override gameplay methods;
 * this class provides structural/rendering/collision properties.
 */
public abstract class BlockFluid extends Block {

    // 1.5.2 BlockFluid constructor (vanilla/server BlockFluid.java:29): every fluid random-ticks unless a subclass
    // turns it off (BlockStationary does, for water). Only the flag is ported; the 1.20.1 fluid engine owns flow.
    protected BlockFluid(int id, Material material) {
        super(id, material);
        this.setTickRandomly(true);
    }

    /**
     * 1.5.2 BlockFluid.getFluidHeightPercent (vanilla/client BlockFluid.java:79-87): the fraction of the
     * block that is AIR above the fluid surface, (meta + 1) / 9, so a source block (meta 0) is 1/9 air.
     * Meta 1-7 = decreasing levels, meta >= 8 = falling (treated as full). All callers expect this
     * formula: frozen Entity.isInsideOfMaterial (vanilla Entity.java:1198, the drowning check),
     * World.handleMaterialAcceleration and RenderBlocks.getFluidHeight. It used to return the filled
     * fraction (8 - meta) / 9, which put a source block's surface at y + 2/9 for drowning.
     */
    public static float getFluidHeightPercent(int meta) {
        if (meta >= 8) {
            meta = 0;
        }
        return (float)(meta + 1) / 9.0F;
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

    // 1.5.2 BlockFluid.getCollisionBoundingBoxFromPool (vanilla/client BlockFluid.java:182-185): fluids have
    // no collision box. Without it water and lava inherited Block's full cube, so FC mobs walked on the
    // surface and Entity.handleWaterMovement never set inWater.
    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
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

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockFluid's icon members (vanilla/client BlockFluid.java:21,
    // :92-95, :637-652): still and flowing water or lava, and the static lookup RenderBlocks.renderBlockFluids reads.
    // BlockFlowing and BlockStationary (8-11) inherit them.
    private Icon[] fluidIcons;

    // 1.5.2 BlockFluid.getIcon (vanilla/client BlockFluid.java:92-95); Block's icon until registerIcons runs (client
    // only)
    @Override
    public Icon getIcon(int side, int metadata) {
        if (this.fluidIcons == null) {
            return super.getIcon(side, metadata);
        }
        return side != 0 && side != 1 ? this.fluidIcons[1] : this.fluidIcons[0];
    }

    // 1.5.2 BlockFluid.registerIcons (vanilla/client BlockFluid.java:637-647)
    @Override
    public void registerIcons(IconRegister register) {
        if (this.blockMaterial == Material.lava) {
            this.fluidIcons = new Icon[] {register.registerIcon("lava"), register.registerIcon("lava_flow")};
        } else {
            this.fluidIcons = new Icon[] {register.registerIcon("water"), register.registerIcon("water_flow")};
        }
    }

    // 1.5.2 BlockFluid.func_94424_b (vanilla/client BlockFluid.java:649-652): Block.waterMoving's or
    // Block.lavaMoving's still or flowing icon by name (compared by value; 1.5.2's callers pass these literals).
    public static Icon func_94424_b(String name) {
        return "water".equals(name) ? fluidIcon(Block.waterMoving, 0)
                : ("water_flow".equals(name) ? fluidIcon(Block.waterMoving, 1)
                : ("lava".equals(name) ? fluidIcon(Block.lavaMoving, 0)
                : ("lava_flow".equals(name) ? fluidIcon(Block.lavaMoving, 1) : null)));
    }

    private static Icon fluidIcon(BlockFluid fluid, int index) {
        return fluid != null && fluid.fluidIcons != null ? fluid.fluidIcons[index] : null;
    }
}
