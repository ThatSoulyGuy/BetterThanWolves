package btw.modern;

public abstract class BlockHalfSlab extends Block {

    protected final boolean isDoubleSlab;

    protected BlockHalfSlab(int id, boolean isDouble, Material material) {
        super(id, material);
        this.isDoubleSlab = isDouble;

        // 1.5.2 (FCMOD) BlockHalfSlab constructor bounds (vanilla/client BlockHalfSlab.java:28-45): a double slab is a
        // full cube, a single slab the bottom half. WorldBridge reports MC slabs in 1.5.2 form (TYPE=DOUBLE as id
        // 43/125, TYPE=TOP as species | 8), which GetBlockBoundsFromPoolBasedOnState below reads per position.
        if (isDouble) {
            // 1.5.2 BlockHalfSlab ctor (vanilla/server BlockHalfSlab.java:28-30): isOpaqueCube reads isDoubleSlab,
            // which was still false when the Block constructor filled the table.
            opaqueCubeLookup[id] = true;
            InitBlockBounds(0D, 0D, 0D, 1D, 1D, 1D);
        } else {
            InitBlockBounds(0D, 0D, 0D, 1D, 0.5D, 1D);
        }
    }

    @Override
    public boolean isOpaqueCube() {
        return this.isDoubleSlab;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return this.isDoubleSlab;
    }

    @Override
    public int quantityDropped(java.util.Random random) {
        return this.isDoubleSlab ? 2 : 1;
    }

    @Override
    public int damageDropped(int metadata) {
        return metadata & 7;
    }

    public abstract String getFullSlabName(int metadata);

    @Override
    public AxisAlignedBB GetBlockBoundsFromPoolBasedOnState(IBlockAccess blockAccess, int i, int j, int k) {
        AxisAlignedBB bounds;

        if (isDoubleSlab) {
            bounds = AxisAlignedBB.getAABBPool().getAABB(0F, 0F, 0F, 1F, 1F, 1F);
        } else {
            if (GetIsUpsideDown(blockAccess, i, j, k)) {
                bounds = AxisAlignedBB.getAABBPool().getAABB(0F, 0.5F, 0F, 1F, 1F, 1F);
            } else {
                bounds = AxisAlignedBB.getAABBPool().getAABB(0F, 0F, 0F, 1F, 0.5F, 1F);
            }
        }

        return bounds;
    }

    // 1.5.2 (FCMOD) BlockHalfSlab.HasLargeCenterHardPointToFacing (vanilla/client BlockHalfSlab.java:217-241): a single
    // slab's full face (bottom of a bottom slab, top of an upside-down one) stays a hard point now that the slab is
    // no longer a normal cube; every other face falls through to the normal-cube test.
    @Override
    public boolean HasLargeCenterHardPointToFacing(IBlockAccess blockAccess, int i, int j, int k, int iFacing, boolean bIgnoreTransparency) {
        if (!isDoubleSlab) {
            boolean bIsUpsideDown = GetIsUpsideDown(blockAccess, i, j, k);

            if (iFacing == 0) {
                if (!bIsUpsideDown) {
                    return true;
                }
            } else if (iFacing == 1) {
                if (bIsUpsideDown) {
                    return true;
                }
            }
        }

        return super.HasLargeCenterHardPointToFacing(blockAccess, i, j, k, iFacing, bIgnoreTransparency);
    }

    // 1.5.2 (FCMOD) BlockHalfSlab.GroundCoverRestingOnVisualOffset (vanilla/client BlockHalfSlab.java:243-255): ground
    // cover (ash, snow) on a bottom slab is drawn half a block lower.
    @Override
    public float GroundCoverRestingOnVisualOffset(IBlockAccess blockAccess, int i, int j, int k) {
        if (!isDoubleSlab) {
            if ((blockAccess.getBlockMetadata(i, j, k) & 8) == 0) {
                return -0.5F;
            }
        }

        return 0F;
    }

    // 1.5.2 (FCMOD) BlockHalfSlab.CanGroundCoverRestOnBlock (vanilla/client BlockHalfSlab.java:257-261). Consumers:
    // FCBlockGroundCover.canPlaceBlockAt:62, FCBlockAshGroundCover.AttemptToPlaceAshAt:80, FCBlockLeaves:158.
    @Override
    public boolean CanGroundCoverRestOnBlock(World world, int i, int j, int k) {
        return true;
    }

    // 1.5.2 (FCMOD) BlockHalfSlab.CanMobsSpawnOn (vanilla/client BlockHalfSlab.java:263-267): the material decides,
    // without the base class's collision-box test. The provider guard is the port's (BlockCrops.updateTick does the
    // same); FCBlockStep refines this per slab type.
    @Override
    public boolean CanMobsSpawnOn(World world, int i, int j, int k) {
        return blockMaterial.GetMobsCanSpawnOn(world.provider != null ? world.provider.dimensionId : 0);
    }

    // 1.5.2 (FCMOD) BlockHalfSlab.MobSpawnOnVerticalOffset (vanilla/client BlockHalfSlab.java:269-278).
    @Override
    public float MobSpawnOnVerticalOffset(World world, int i, int j, int k) {
        if (!isDoubleSlab && !GetIsUpsideDown(world, i, j, k)) {
            return -0.5F;
        }

        return 0F;
    }

    // 1.5.2 BlockHalfSlab.GetIsUpsideDown (FCMOD, vanilla/client BlockHalfSlab.java:282-290): metadata bit 8.
    // This stub used to always return false.
    public boolean GetIsUpsideDown(IBlockAccess blockAccess, int x, int y, int z) {
        return GetIsUpsideDown(blockAccess.getBlockMetadata(x, y, z));
    }

    public boolean GetIsUpsideDown(int iMetadata) {
        return (iMetadata & 8) > 0;
    }
}
