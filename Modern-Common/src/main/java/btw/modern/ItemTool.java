package btw.modern;

/**
 * FC's replacement for vanilla ItemTool.
 * Mirrors the FC-patched FCItemTool with tool-type effectiveness checking.
 *
 * The mining speed chain:
 *   getStrVsBlock(stack, world, block, i, j, k)
 *     -> IsEfficientVsBlock(stack, world, block, i, j, k)
 *       -> IsToolTypeEfficientVsBlockType(block)  [abstract, implemented by subclasses]
 *     -> efficiencyOnProperMaterial  (the tool material speed)
 */
public abstract class ItemTool extends Item {
    public float efficiencyOnProperMaterial;
    public int damageVsEntity;
    public EnumToolMaterial toolMaterial;

    protected ItemTool(int id, int baseEntityDamage, EnumToolMaterial material, Block[] effectiveBlocks) {
        super(id);
        this.toolMaterial = material;
        this.maxStackSize = 1;
        this.efficiencyOnProperMaterial = material.getEfficiencyOnProperMaterial();
        this.damageVsEntity = baseEntityDamage + material.getDamageVsEntity();
        setMaxDamage(material.getMaxUses());
    }

    // Convenience constructor without effective blocks (FC style)
    protected ItemTool(int id, int baseEntityDamage, EnumToolMaterial material) {
        this(id, baseEntityDamage, material, new Block[0]);
    }

    /**
     * Non-positional getStrVsBlock. Checks the block's FC effectiveness flags
     * via IsToolTypeEfficientVsBlockType. Returns efficiencyOnProperMaterial
     * if the tool type is effective, 1.0F otherwise.
     */
    @Override
    public float getStrVsBlock(ItemStack stack, Block block) {
        if (block != null && IsToolTypeEfficientVsBlockType(block)) {
            return this.efficiencyOnProperMaterial;
        }
        return 1.0F;
    }

    /**
     * Position-aware getStrVsBlock (FC's primary mining speed path).
     * Checks IsEfficientVsBlock first; if efficient, returns efficiencyOnProperMaterial.
     */
    @Override
    public float getStrVsBlock(ItemStack stack, World world, Block block, int i, int j, int k) {
        if (IsEfficientVsBlock(stack, world, block, i, j, k)) {
            return this.efficiencyOnProperMaterial;
        }
        return super.getStrVsBlock(stack, world, block, i, j, k);
    }

    /**
     * FC's efficiency check. A tool is efficient vs a block if:
     * 1. The block's material requires a tool AND this tool can harvest it, OR
     * 2. The tool type is efficient vs the block type (via IsToolTypeEfficientVsBlockType).
     *
     * Mirrors FCItemTool.IsEfficientVsBlock from the patched vanilla source.
     */
    @Override
    public boolean IsEfficientVsBlock(ItemStack stack, World world, Block block, int i, int j, int k) {
        if (!block.blockMaterial.isToolNotRequired()) {
            if (canHarvestBlock(stack, world, block, i, j, k)) {
                return true;
            }
        }
        return IsToolTypeEfficientVsBlockType(block);
    }

    /**
     * Abstract method implemented by each tool subclass to check the block's
     * FC effectiveness flag (ArePicksEffectiveOn, AreAxesEffectiveOn, etc.).
     */
    public abstract boolean IsToolTypeEfficientVsBlockType(Block block);

    @Override
    public boolean onBlockDestroyed(ItemStack stack, World world, int blockID, int x, int y, int z, EntityLiving entity) {
        Block block = Block.blocksList[blockID];
        if (block != null && (double) block.getBlockHardness(world, x, y, z) != 0.0D) {
            stack.damageItem(1, entity);
        }
        return true;
    }

    @Override
    public boolean hitEntity(ItemStack stack, EntityLiving target, EntityLiving attacker) {
        stack.damageItem(2, attacker);
        return true;
    }

    @Override
    public int getDamageVsEntity(Entity entity) {
        return damageVsEntity;
    }

    public EnumToolMaterial getToolMaterial() {
        return toolMaterial;
    }
}
