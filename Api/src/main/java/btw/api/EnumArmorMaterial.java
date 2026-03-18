package btw.api;

public enum EnumArmorMaterial {
    CLOTH(10, new int[]{1, 3, 2, 1}, 15),
    CHAIN(20, new int[]{2, 5, 4, 1}, 12),
    IRON(20, new int[]{2, 6, 5, 2}, 10),
    GOLD(7, new int[]{2, 5, 3, 1}, 25),
    DIAMOND(33, new int[]{3, 8, 6, 3}, 10);

    private int maxDamageFactor;
    private int[] damageReductionAmountArray;
    private int enchantability;

    private EnumArmorMaterial(int maxDamage, int[] reduction, int enchant) {
        this.maxDamageFactor = maxDamage;
        this.damageReductionAmountArray = reduction;
        this.enchantability = enchant;
    }

    public int getDurability(int slot) { return 0; }
    public int getDamageReductionAmount(int slot) { return damageReductionAmountArray[slot]; }
    public int getEnchantability() { return enchantability; }
    public int getArmorCraftingMaterial() { return 0; }
}
