package btw.modern;

/**
 * 1.5.2 EnchantmentData — weighted (enchantment, level) pair produced by
 * EnchantmentHelper.mapEnchantmentData and consumed by buildEnchantmentList /
 * addRandomEnchantment / ItemEnchantedBook.func_92115_a.
 */
public class EnchantmentData extends WeightedRandomItem {
    /** Enchantment object associated with this EnchantmentData */
    public final Enchantment enchantmentobj;

    /** Enchantment level associated with this EnchantmentData */
    public final int enchantmentLevel;

    public EnchantmentData(Enchantment enchantment, int level) {
        super(enchantment.getWeight());
        this.enchantmentobj = enchantment;
        this.enchantmentLevel = level;
    }

    public EnchantmentData(int enchantmentId, int level) {
        this(Enchantment.enchantmentsList[enchantmentId], level);
    }
}
