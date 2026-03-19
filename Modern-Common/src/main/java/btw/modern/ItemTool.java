package btw.modern;

public class ItemTool extends Item {
    protected float efficiencyOnProperMaterial;
    private int damageVsEntity;
    protected EnumToolMaterial toolMaterial;

    protected ItemTool(int id, int damage, EnumToolMaterial material, Block[] effectiveBlocks) {
        super(id);
        this.toolMaterial = material;
        this.damageVsEntity = damage;
        this.efficiencyOnProperMaterial = material.getEfficiencyOnProperMaterial();
    }

    public float getStrVsBlock(ItemStack stack, Block block) { return 1.0F; }
    public int getDamageVsEntity(Entity entity) { return damageVsEntity; }
    public EnumToolMaterial getToolMaterial() { return toolMaterial; }
}
