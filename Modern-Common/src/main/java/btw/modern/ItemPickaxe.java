package btw.modern;

/**
 * FC's pickaxe tool. Checks ArePicksEffectiveOn() for block effectiveness.
 * Also has special handling for rock/iron/anvil materials and tool level checks.
 */
public class ItemPickaxe extends ItemTool {
    public ItemPickaxe(int id, EnumToolMaterial material) {
        super(id, 2, material);
    }

    @Override
    public float getStrVsBlock(ItemStack stack, World world, Block block, int i, int j, int k) {
        int toolLevel = toolMaterial.getHarvestLevel();
        int blockToolLevel = block.GetEfficientToolLevel(world, i, j, k);

        if (blockToolLevel > toolLevel) {
            return 1.0F;
        }

        Material material = block.blockMaterial;

        if (material == Material.iron || material == Material.rock || material == Material.anvil) {
            return this.efficiencyOnProperMaterial;
        }

        return super.getStrVsBlock(stack, world, block, i, j, k);
    }

    @Override
    public boolean IsEfficientVsBlock(ItemStack stack, World world, Block block, int i, int j, int k) {
        int toolLevel = toolMaterial.getHarvestLevel();
        int blockToolLevel = block.GetEfficientToolLevel(world, i, j, k);

        if (blockToolLevel > toolLevel) {
            return false;
        }

        return super.IsEfficientVsBlock(stack, world, block, i, j, k);
    }

    @Override
    public boolean IsToolTypeEfficientVsBlockType(Block block) {
        return block.ArePicksEffectiveOn();
    }
}
