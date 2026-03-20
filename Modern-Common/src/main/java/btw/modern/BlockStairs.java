package btw.modern;

/**
 * Base class for stair blocks.
 * Mirrors net.minecraft.src.BlockStairs with identical field/method names.
 * FC subclass (FCBlockStairs -> FCBlockStairsBase) overrides most gameplay
 * methods and delegates texture/hardness to the model block. This class
 * stores the model block reference and provides correct rendering properties.
 */
public class BlockStairs extends Block {

    /** The block that this stair variant is based on (for texture/hardness delegation). */
    private final Block modelBlock;

    /** The metadata of the model block (for texture variant selection). */
    private final int modelBlockMetadata;

    protected BlockStairs(int id, Block modelBlock, int modelMetadata) {
        super(id, modelBlock.blockMaterial);
        this.modelBlock = modelBlock;
        this.modelBlockMetadata = modelMetadata;

        // Copy properties from the model block
        this.setHardness(modelBlock.blockHardness);
        this.setResistance(modelBlock.blockResistance / 3.0F);
        this.setStepSound(modelBlock.stepSound);

        this.setLightOpacity(255);
        Block.useNeighborBrightness[id] = true;
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
        return 10;
    }

    @Override
    public boolean IsStairBlock() {
        return true;
    }

    // --- Model block access ---

    /**
     * Returns the block that this stair variant is modeled after.
     * Used by FC subclasses and rendering code.
     */
    public Block getModelBlock() {
        return this.modelBlock;
    }

    /**
     * Returns the metadata of the model block.
     */
    public int getModelBlockMetadata() {
        return this.modelBlockMetadata;
    }

    // --- Texture delegation ---

    @Override
    public Icon getIcon(int side, int metadata) {
        return this.modelBlock.getIcon(side, this.modelBlockMetadata);
    }

    // --- Gameplay delegation to model block ---

    @Override
    public float getExplosionResistance(Entity entity) {
        return this.modelBlock.getExplosionResistance(entity);
    }

    @Override
    public float getBlockHardness(World world, int x, int y, int z) {
        return this.modelBlock.getBlockHardness(world, x, y, z);
    }

    @Override
    public int tickRate(World world) {
        return this.modelBlock.tickRate(world);
    }

    @Override
    public boolean isCollidable() {
        return this.modelBlock.isCollidable();
    }

    @Override
    public boolean canCollideCheck(int metadata, boolean hitIfLiquid) {
        return this.modelBlock.canCollideCheck(metadata, hitIfLiquid);
    }
}
