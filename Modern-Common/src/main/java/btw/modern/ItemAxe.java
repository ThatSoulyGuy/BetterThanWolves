package btw.modern;

/**
 * FC's axe tool. Checks AreAxesEffectiveOn() and material.GetAxesEfficientOn()
 * for block effectiveness. Also respects GetEfficientToolLevel and
 * GetIsProblemToRemove (stumps).
 */
public class ItemAxe extends ItemTool {
    public ItemAxe(int id, EnumToolMaterial material) {
        super(id, 3, material);
    }

    @Override
    public float getStrVsBlock(ItemStack stack, World world, Block block, int i, int j, int k) {
        int toolLevel = toolMaterial.getHarvestLevel();
        int blockToolLevel = block.GetEfficientToolLevel(world, i, j, k);

        if (blockToolLevel > toolLevel) {
            return 1.0F;
        }

        if (block.GetIsProblemToRemove(world, i, j, k)) {
            // stumps and such
            return 1.0F;
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

        if (block.GetIsProblemToRemove(world, i, j, k)) {
            // stumps and such
            return false;
        }

        return super.IsEfficientVsBlock(stack, world, block, i, j, k);
    }

    @Override
    public boolean IsToolTypeEfficientVsBlockType(Block block) {
        return block.blockMaterial.GetAxesEfficientOn() || block.AreAxesEffectiveOn();
    }
}
