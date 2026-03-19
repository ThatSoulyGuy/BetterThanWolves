package btw.modern;

public enum EnumToolMaterial {

    WOOD(0, 10, 1.01F, 0, 0),
    STONE(1, 50, 1.01F, 1, 5),
    IRON(2, 500, 6.0F, 2, 14),
    EMERALD(3, 1561, 8.0F, 3, 14),
    GOLD(0, 32, 12.0F, 0, 22),
    SOULFORGED_STEEL(4, 2250, 12.0F, 4, 0);

    private final int harvestLevel;
    private final int maxUses;
    private final float efficiencyOnProperMaterial;
    private final int damageVsEntity;
    private final int enchantability;

    EnumToolMaterial(int harvestLevel, int maxUses, float efficiency, int damage, int enchantability) {
        this.harvestLevel = harvestLevel;
        this.maxUses = maxUses;
        this.efficiencyOnProperMaterial = efficiency;
        this.damageVsEntity = damage;
        this.enchantability = enchantability;
    }

    public int getMaxUses() {
        return this.maxUses;
    }

    public float getEfficiencyOnProperMaterial() {
        return this.efficiencyOnProperMaterial;
    }

    public int getDamageVsEntity() {
        return this.damageVsEntity;
    }

    public int getHarvestLevel() {
        return this.harvestLevel;
    }

    public int getEnchantability() {
        return this.enchantability;
    }

    public int GetInfernalMaxNumEnchants() { return 3; }
    public int GetInfernalMaxEnchantmentCost() { return 30; }
}
