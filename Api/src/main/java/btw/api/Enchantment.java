package btw.api;

public abstract class Enchantment {
    public static final Enchantment[] enchantmentsList = new Enchantment[256];
    public static final Enchantment[] field_92090_c = new Enchantment[0];

    public static Enchantment protection;
    public static Enchantment fireProtection;
    public static Enchantment featherFalling;
    public static Enchantment blastProtection;
    public static Enchantment projectileProtection;
    public static Enchantment respiration;
    public static Enchantment aquaAffinity;
    public static Enchantment thorns;
    public static Enchantment sharpness;
    public static Enchantment smite;
    public static Enchantment baneOfArthropods;
    public static Enchantment knockback;
    public static Enchantment fireAspect;
    public static Enchantment looting;
    public static Enchantment efficiency;
    public static Enchantment silkTouch;
    public static Enchantment unbreaking;
    public static Enchantment fortune;
    public static Enchantment power;
    public static Enchantment punch;
    public static Enchantment flame;
    public static Enchantment infinity;

    public final int effectId;
    private final int weight;
    public EnumEnchantmentType type;
    public String name;

    public Enchantment(int id, int weight, EnumEnchantmentType type) {
        this.effectId = id;
        this.weight = weight;
        this.type = type;
    }

    public int getWeight() { return this.weight; }
    public int getMinLevel() { return 1; }
    public int getMaxLevel() { return 1; }
    public int getMinEnchantability(int level) { return 1 + level * 10; }
    public int getMaxEnchantability(int level) { return getMinEnchantability(level) + 5; }
    public int calcModifierDamage(int level, DamageSource source) { return 0; }
    public int calcModifierLiving(int level, EntityLiving target) { return 0; }

    public boolean canApplyTogether(Enchantment other) {
        return this != other;
    }

    public Enchantment setName(String name) {
        this.name = name;
        return this;
    }

    public String getName() {
        return "enchantment." + this.name;
    }

    public String getTranslatedName(int level) {
        return getName();
    }

    public boolean func_92089_a(ItemStack stack) {
        return false;
    }

    public boolean canApply(ItemStack stack) {
        return false;
    }

    public boolean CanBeAppliedByVanillaEnchanter() {
        return true;
    }
}
