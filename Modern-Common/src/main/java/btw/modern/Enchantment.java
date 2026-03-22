package btw.modern;

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
        if (id >= 0 && id < enchantmentsList.length) {
            enchantmentsList[id] = this;
        }
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
        return this.type.canEnchantItem(stack.getItem());
    }

    public boolean canApply(ItemStack stack) {
        return this.type.canEnchantItem(stack.getItem());
    }

    public boolean CanBeAppliedByVanillaEnchanter() {
        return true;
    }

    /**
     * Populates vanilla enchantment static fields. Called during Forge mod init
     * BEFORE FC code runs.
     * IDs, weights, and types match vanilla MC 1.5.2.
     * Uses a concrete inner class since Enchantment itself is abstract.
     */
    public static void initializeVanillaEnchantments() {
        protection           = new ConcreteEnchantment(0,  10, EnumEnchantmentType.armor, 4).setName("protect.all");
        fireProtection       = new ConcreteEnchantment(1,  5,  EnumEnchantmentType.armor, 4).setName("protect.fire");
        featherFalling       = new ConcreteEnchantment(2,  5,  EnumEnchantmentType.armor_feet, 4).setName("protect.fall");
        blastProtection      = new ConcreteEnchantment(3,  2,  EnumEnchantmentType.armor, 4).setName("protect.explosion");
        projectileProtection = new ConcreteEnchantment(4,  5,  EnumEnchantmentType.armor, 4).setName("protect.projectile");
        respiration          = new ConcreteEnchantment(5,  2,  EnumEnchantmentType.armor_head, 3).setName("oxygen");
        aquaAffinity         = new ConcreteEnchantment(6,  2,  EnumEnchantmentType.armor_head, 1).setName("waterWorker");
        thorns               = new ConcreteEnchantment(7,  1,  EnumEnchantmentType.armor_torso, 3).setName("thorns");
        sharpness            = new ConcreteEnchantment(16, 10, EnumEnchantmentType.weapon, 5).setName("damage.all");
        smite                = new ConcreteEnchantment(17, 5,  EnumEnchantmentType.weapon, 5).setName("damage.undead");
        baneOfArthropods     = new ConcreteEnchantment(18, 5,  EnumEnchantmentType.weapon, 5).setName("damage.arthropods");
        knockback            = new ConcreteEnchantment(19, 5,  EnumEnchantmentType.weapon, 2).setName("knockback");
        fireAspect           = new ConcreteEnchantment(20, 2,  EnumEnchantmentType.weapon, 2).setName("fire");
        looting              = new ConcreteEnchantment(21, 2,  EnumEnchantmentType.weapon, 3).setName("lootBonus");
        efficiency           = new ConcreteEnchantment(32, 10, EnumEnchantmentType.digger, 5).setName("digging");
        silkTouch            = new ConcreteEnchantment(33, 1,  EnumEnchantmentType.digger, 1).setName("untouching");
        unbreaking           = new ConcreteEnchantment(34, 5,  EnumEnchantmentType.digger, 3).setName("durability");
        fortune              = new ConcreteEnchantment(35, 2,  EnumEnchantmentType.digger, 3).setName("lootBonusDigger");
        power                = new ConcreteEnchantment(48, 10, EnumEnchantmentType.bow, 5).setName("arrowDamage");
        punch                = new ConcreteEnchantment(49, 2,  EnumEnchantmentType.bow, 2).setName("arrowKnockback");
        flame                = new ConcreteEnchantment(50, 2,  EnumEnchantmentType.bow, 1).setName("arrowFire");
        infinity             = new ConcreteEnchantment(51, 1,  EnumEnchantmentType.bow, 1).setName("arrowInfinite");
    }

    // Concrete subclass since Enchantment is abstract
    private static class ConcreteEnchantment extends Enchantment {
        private final int maxLevel;

        ConcreteEnchantment(int id, int weight, EnumEnchantmentType type, int maxLevel) {
            super(id, weight, type);
            this.maxLevel = maxLevel;
        }

        @Override
        public int getMaxLevel() { return maxLevel; }
    }
}
