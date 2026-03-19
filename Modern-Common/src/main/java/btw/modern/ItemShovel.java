package btw.modern;

/**
 * FC's shovel tool. Checks AreShovelsEffectiveOn() for block effectiveness.
 */
public class ItemShovel extends ItemTool {
    public ItemShovel(int id, EnumToolMaterial material) {
        super(id, 1, material);
    }

    @Override
    public boolean IsToolTypeEfficientVsBlockType(Block block) {
        return block.AreShovelsEffectiveOn();
    }
}
