package btw.modern;

/**
 * FC's hoe tool. Checks AreHoesEffectiveOn() for block effectiveness.
 * FC hoes extend FCItemTool (not vanilla ItemHoe) and have reduced efficiency:
 * iron or worse = efficiencyOnProperMaterial / 8, diamond+ = / 4.
 */
public class ItemHoe extends ItemTool {
    protected EnumToolMaterial theToolMaterial;

    public ItemHoe(int id, EnumToolMaterial material) {
        super(id, 1, material);
        this.theToolMaterial = material;

        if (material.getHarvestLevel() <= 2) {
            // iron or worse: very slow efficiency
            this.efficiencyOnProperMaterial /= 8;
        } else {
            // diamond+: slightly less slow
            this.efficiencyOnProperMaterial /= 4;
        }
    }

    @Override
    public boolean IsToolTypeEfficientVsBlockType(Block block) {
        return block.AreHoesEffectiveOn();
    }
}
