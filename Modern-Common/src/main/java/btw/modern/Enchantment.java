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

    // Both enchant-applicability entry points must share the FCMOD canApply body:
    // client-named FC bytecode calls canApply, server-named calls func_92089_a, and
    // the per-type subclass overrides below are on canApply — so func_92089_a must
    // delegate or the FCMOD item-controlled applicability is lost on the server path.
    public boolean func_92089_a(ItemStack stack) {
        return this.canApply(stack);
    }

    // 1.5.2 Enchantment.canApply (FCMOD: Changed) — delegates to the item's
    // IsEnchantmentApplicable so FC items (FCItemTool/FCItemBow/armor) control
    // which enchants they accept.
    public boolean canApply(ItemStack stack) {
        return stack.getItem().IsEnchantmentApplicable(this);
    }

    // FCMOD: Added New — FC removes the more powerful enchants from the
    // vanilla enchanter (see the bans applied in initializeVanillaEnchantments).
    private boolean m_bCanBeAppliedByVanillaEnchanter = true;

    public boolean CanBeAppliedByVanillaEnchanter() {
        return m_bCanBeAppliedByVanillaEnchanter;
    }
    // END FCMOD

    /**
     * Populates vanilla enchantment static fields. Called during Forge mod init
     * BEFORE FC code runs.
     * IDs, weights, types, and per-type subclasses match vanilla MC 1.5.2
     * (Enchantment.java statics); the subclasses below are faithful ports of the
     * vanilla Enchantment* classes, nested so they don't shadow the fc-compiled
     * originals (fc EnchantmentThorns.func_92096_a etc. stay live).
     */
    public static void initializeVanillaEnchantments() {
        protection           = new EnchantmentProtection(0,  10, 0);
        fireProtection       = new EnchantmentProtection(1,  5,  1);
        featherFalling       = new EnchantmentProtection(2,  5,  2);
        blastProtection      = new EnchantmentProtection(3,  2,  3);
        projectileProtection = new EnchantmentProtection(4,  5,  4);
        respiration          = new EnchantmentOxygen(5, 2);
        aquaAffinity         = new EnchantmentWaterWorker(6, 2);
        thorns               = new EnchantmentThorns(7, 1);
        sharpness            = new EnchantmentDamage(16, 10, 0);
        smite                = new EnchantmentDamage(17, 5,  1);
        baneOfArthropods     = new EnchantmentDamage(18, 5,  2);
        knockback            = new EnchantmentKnockback(19, 5);
        fireAspect           = new EnchantmentFireAspect(20, 2);
        looting              = new EnchantmentLootBonus(21, 2, EnumEnchantmentType.weapon);
        efficiency           = new EnchantmentDigging(32, 10);
        silkTouch            = new EnchantmentUntouching(33, 1);
        unbreaking           = new EnchantmentDurability(34, 5);
        fortune              = new EnchantmentLootBonus(35, 2, EnumEnchantmentType.digger);
        power                = new EnchantmentArrowDamage(48, 10);
        punch                = new EnchantmentArrowKnockback(49, 2);
        flame                = new EnchantmentArrowFire(50, 2);
        infinity             = new EnchantmentArrowInfinite(51, 1);

        // FCMOD: Added New — remove the more powerful enchants from the
        // vanilla enchanter (1.5.2 Enchantment static block, FC-added).
        protection.m_bCanBeAppliedByVanillaEnchanter = false;
        silkTouch.m_bCanBeAppliedByVanillaEnchanter = false;
        fortune.m_bCanBeAppliedByVanillaEnchanter = false;
        sharpness.m_bCanBeAppliedByVanillaEnchanter = false;
        featherFalling.m_bCanBeAppliedByVanillaEnchanter = false;
        // END FCMOD
    }

    // ================================================================
    // 1.5.2 per-type Enchantment subclasses, ported verbatim (nested so
    // they don't collide with the fc-compiled classes of the same name).
    // Consumed by EnchantmentHelper.mapEnchantmentData/buildEnchantmentList
    // via the live addRandomEnchantment chain (FCEntitySkeleton spawn gear,
    // FCEntityVillager trades) and the enchanting table container.
    // ================================================================

    // 1.5.2 EnchantmentProtection
    private static class EnchantmentProtection extends Enchantment {
        private static final String[] protectionName = new String[] {"all", "fire", "fall", "explosion", "projectile"};
        private static final int[] baseEnchantability = new int[] {1, 10, 5, 5, 3};
        private static final int[] levelEnchantability = new int[] {11, 8, 6, 8, 6};
        private static final int[] thresholdEnchantability = new int[] {20, 12, 10, 12, 15};

        /** 0 = all, 1 = fire, 2 = fall (feather fall), 3 = explosion, 4 = projectile */
        public final int protectionType;

        EnchantmentProtection(int id, int weight, int protType) {
            super(id, weight, EnumEnchantmentType.armor);
            this.protectionType = protType;

            if (protType == 2) {
                this.type = EnumEnchantmentType.armor_feet;
            }
        }

        @Override
        public int getMinEnchantability(int level) {
            return baseEnchantability[this.protectionType] + (level - 1) * levelEnchantability[this.protectionType];
        }

        @Override
        public int getMaxEnchantability(int level) {
            return this.getMinEnchantability(level) + thresholdEnchantability[this.protectionType];
        }

        @Override
        public int getMaxLevel() { return 4; }

        @Override
        public int calcModifierDamage(int level, DamageSource source) {
            if (source.canHarmInCreative()) {
                return 0;
            } else {
                float base = (float)(6 + level * level) / 3.0F;
                return this.protectionType == 0 ? MathHelper.floor_float(base * 0.75F)
                    : (this.protectionType == 1 && source.isFireDamage() ? MathHelper.floor_float(base * 1.25F)
                    : (this.protectionType == 2 && source == DamageSource.fall ? MathHelper.floor_float(base * 2.5F)
                    : (this.protectionType == 3 && source.isExplosion() ? MathHelper.floor_float(base * 1.5F)
                    : (this.protectionType == 4 && source.isProjectile() ? MathHelper.floor_float(base * 1.5F) : 0))));
            }
        }

        @Override
        public String getName() {
            return "enchantment.protect." + protectionName[this.protectionType];
        }

        @Override
        public boolean canApplyTogether(Enchantment other) {
            if (other instanceof EnchantmentProtection) {
                EnchantmentProtection otherProt = (EnchantmentProtection) other;
                return otherProt.protectionType == this.protectionType ? false
                    : this.protectionType == 2 || otherProt.protectionType == 2;
            } else {
                return super.canApplyTogether(other);
            }
        }
    }

    // 1.5.2 EnchantmentDamage
    private static class EnchantmentDamage extends Enchantment {
        private static final String[] protectionName = new String[] {"all", "undead", "arthropods"};
        private static final int[] baseEnchantability = new int[] {1, 5, 5};
        private static final int[] levelEnchantability = new int[] {11, 8, 8};
        private static final int[] thresholdEnchantability = new int[] {20, 20, 20};

        /** 0 = all, 1 = undead, 2 = arthropods */
        public final int damageType;

        EnchantmentDamage(int id, int weight, int dmgType) {
            super(id, weight, EnumEnchantmentType.weapon);
            this.damageType = dmgType;
        }

        @Override
        public int getMinEnchantability(int level) {
            return baseEnchantability[this.damageType] + (level - 1) * levelEnchantability[this.damageType];
        }

        @Override
        public int getMaxEnchantability(int level) {
            return this.getMinEnchantability(level) + thresholdEnchantability[this.damageType];
        }

        @Override
        public int getMaxLevel() { return 5; }

        @Override
        public int calcModifierLiving(int level, EntityLiving target) {
            return this.damageType == 0 ? MathHelper.floor_float((float)level * 2.75F)
                : (this.damageType == 1 && target.getCreatureAttribute() == EnumCreatureAttribute.UNDEAD ? MathHelper.floor_float((float)level * 4.5F)
                : (this.damageType == 2 && target.getCreatureAttribute() == EnumCreatureAttribute.ARTHROPOD ? MathHelper.floor_float((float)level * 4.5F) : 0));
        }

        @Override
        public String getName() {
            return "enchantment.damage." + protectionName[this.damageType];
        }

        @Override
        public boolean canApplyTogether(Enchantment other) {
            return !(other instanceof EnchantmentDamage);
        }

        @Override
        public boolean canApply(ItemStack stack) {
            return stack.getItem() instanceof ItemAxe ? true : super.canApply(stack);
        }
    }

    // 1.5.2 EnchantmentOxygen
    private static class EnchantmentOxygen extends Enchantment {
        EnchantmentOxygen(int id, int weight) {
            super(id, weight, EnumEnchantmentType.armor_head);
            this.setName("oxygen");
        }

        @Override
        public int getMinEnchantability(int level) { return 10 * level; }

        @Override
        public int getMaxEnchantability(int level) { return this.getMinEnchantability(level) + 30; }

        @Override
        public int getMaxLevel() { return 3; }
    }

    // 1.5.2 EnchantmentWaterWorker
    private static class EnchantmentWaterWorker extends Enchantment {
        EnchantmentWaterWorker(int id, int weight) {
            super(id, weight, EnumEnchantmentType.armor_head);
            this.setName("waterWorker");
        }

        @Override
        public int getMinEnchantability(int level) { return 1; }

        @Override
        public int getMaxEnchantability(int level) { return this.getMinEnchantability(level) + 40; }

        @Override
        public int getMaxLevel() { return 1; }
    }

    // 1.5.2 EnchantmentThorns (instance side only — the live static retaliation
    // helpers func_92094_a/func_92095_b/func_92096_a stay on the fc-compiled
    // EnchantmentThorns class).
    private static class EnchantmentThorns extends Enchantment {
        EnchantmentThorns(int id, int weight) {
            super(id, weight, EnumEnchantmentType.armor_torso);
            this.setName("thorns");
        }

        @Override
        public int getMinEnchantability(int level) { return 10 + 20 * (level - 1); }

        @Override
        public int getMaxEnchantability(int level) { return super.getMinEnchantability(level) + 50; }

        @Override
        public int getMaxLevel() { return 3; }

        @Override
        public boolean canApply(ItemStack stack) {
            return stack.getItem() instanceof ItemArmor ? true : super.canApply(stack);
        }
    }

    // 1.5.2 EnchantmentKnockback
    private static class EnchantmentKnockback extends Enchantment {
        EnchantmentKnockback(int id, int weight) {
            super(id, weight, EnumEnchantmentType.weapon);
            this.setName("knockback");
        }

        @Override
        public int getMinEnchantability(int level) { return 5 + 20 * (level - 1); }

        @Override
        public int getMaxEnchantability(int level) { return super.getMinEnchantability(level) + 50; }

        @Override
        public int getMaxLevel() { return 2; }
    }

    // 1.5.2 EnchantmentFireAspect
    private static class EnchantmentFireAspect extends Enchantment {
        EnchantmentFireAspect(int id, int weight) {
            super(id, weight, EnumEnchantmentType.weapon);
            this.setName("fire");
        }

        @Override
        public int getMinEnchantability(int level) { return 10 + 20 * (level - 1); }

        @Override
        public int getMaxEnchantability(int level) { return super.getMinEnchantability(level) + 50; }

        @Override
        public int getMaxLevel() { return 2; }
    }

    // 1.5.2 EnchantmentLootBonus (looting / fortune)
    private static class EnchantmentLootBonus extends Enchantment {
        EnchantmentLootBonus(int id, int weight, EnumEnchantmentType type) {
            super(id, weight, type);
            this.setName("lootBonus");

            if (type == EnumEnchantmentType.digger) {
                this.setName("lootBonusDigger");
            }
        }

        @Override
        public int getMinEnchantability(int level) { return 15 + (level - 1) * 9; }

        @Override
        public int getMaxEnchantability(int level) { return super.getMinEnchantability(level) + 50; }

        @Override
        public int getMaxLevel() { return 3; }

        @Override
        public boolean canApplyTogether(Enchantment other) {
            return super.canApplyTogether(other) && other.effectId != silkTouch.effectId;
        }
    }

    // 1.5.2 EnchantmentDigging (efficiency)
    private static class EnchantmentDigging extends Enchantment {
        EnchantmentDigging(int id, int weight) {
            super(id, weight, EnumEnchantmentType.digger);
            this.setName("digging");
        }

        @Override
        public int getMinEnchantability(int level) { return 1 + 10 * (level - 1); }

        @Override
        public int getMaxEnchantability(int level) { return super.getMinEnchantability(level) + 50; }

        @Override
        public int getMaxLevel() { return 5; }

        @Override
        public boolean canApply(ItemStack stack) {
            return stack.getItem().itemID == Item.shears.itemID ? true : super.canApply(stack);
        }
    }

    // 1.5.2 EnchantmentUntouching (silk touch)
    private static class EnchantmentUntouching extends Enchantment {
        EnchantmentUntouching(int id, int weight) {
            super(id, weight, EnumEnchantmentType.digger);
            this.setName("untouching");
        }

        @Override
        public int getMinEnchantability(int level) { return 15; }

        @Override
        public int getMaxEnchantability(int level) { return super.getMinEnchantability(level) + 50; }

        @Override
        public int getMaxLevel() { return 1; }

        @Override
        public boolean canApplyTogether(Enchantment other) {
            return super.canApplyTogether(other) && other.effectId != fortune.effectId;
        }

        @Override
        public boolean canApply(ItemStack stack) {
            return stack.getItem().itemID == Item.shears.itemID ? true : super.canApply(stack);
        }
    }

    // 1.5.2 EnchantmentDurability (unbreaking)
    private static class EnchantmentDurability extends Enchantment {
        EnchantmentDurability(int id, int weight) {
            super(id, weight, EnumEnchantmentType.digger);
            this.setName("durability");
        }

        @Override
        public int getMinEnchantability(int level) { return 5 + (level - 1) * 8; }

        @Override
        public int getMaxEnchantability(int level) { return super.getMinEnchantability(level) + 50; }

        @Override
        public int getMaxLevel() { return 3; }

        @Override
        public boolean canApply(ItemStack stack) {
            return stack.isItemStackDamageable() ? true : super.canApply(stack);
        }
    }

    // 1.5.2 EnchantmentArrowDamage (power)
    private static class EnchantmentArrowDamage extends Enchantment {
        EnchantmentArrowDamage(int id, int weight) {
            super(id, weight, EnumEnchantmentType.bow);
            this.setName("arrowDamage");
        }

        @Override
        public int getMinEnchantability(int level) { return 1 + (level - 1) * 10; }

        @Override
        public int getMaxEnchantability(int level) { return this.getMinEnchantability(level) + 15; }

        @Override
        public int getMaxLevel() { return 5; }
    }

    // 1.5.2 EnchantmentArrowKnockback (punch)
    private static class EnchantmentArrowKnockback extends Enchantment {
        EnchantmentArrowKnockback(int id, int weight) {
            super(id, weight, EnumEnchantmentType.bow);
            this.setName("arrowKnockback");
        }

        @Override
        public int getMinEnchantability(int level) { return 12 + (level - 1) * 20; }

        @Override
        public int getMaxEnchantability(int level) { return this.getMinEnchantability(level) + 25; }

        @Override
        public int getMaxLevel() { return 2; }
    }

    // 1.5.2 EnchantmentArrowFire (flame)
    private static class EnchantmentArrowFire extends Enchantment {
        EnchantmentArrowFire(int id, int weight) {
            super(id, weight, EnumEnchantmentType.bow);
            this.setName("arrowFire");
        }

        @Override
        public int getMinEnchantability(int level) { return 20; }

        @Override
        public int getMaxEnchantability(int level) { return 50; }

        @Override
        public int getMaxLevel() { return 1; }
    }

    // 1.5.2 EnchantmentArrowInfinite (infinity)
    private static class EnchantmentArrowInfinite extends Enchantment {
        EnchantmentArrowInfinite(int id, int weight) {
            super(id, weight, EnumEnchantmentType.bow);
            this.setName("arrowInfinite");
        }

        @Override
        public int getMinEnchantability(int level) { return 20; }

        @Override
        public int getMaxEnchantability(int level) { return 50; }

        @Override
        public int getMaxLevel() { return 1; }
    }
}
