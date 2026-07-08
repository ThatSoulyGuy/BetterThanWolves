package btw.modern;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class EnchantmentHelper {

    private static final Random enchantmentRand = new Random();

    public static int getEnchantmentLevel(int enchId, ItemStack stack) {
        if (stack == null) return 0;
        NBTTagList enchList = stack.getEnchantmentTagList();
        if (enchList == null) return 0;

        for (int i = 0; i < enchList.tagCount(); i++) {
            NBTTagCompound tag = (NBTTagCompound) enchList.tagAt(i);
            short id = tag.getShort("id");
            short lvl = tag.getShort("lvl");
            if (id == enchId) {
                return lvl;
            }
        }
        return 0;
    }

    public static Map getEnchantments(ItemStack stack) {
        Map<Integer, Integer> map = new HashMap<>();
        if (stack == null) return map;
        NBTTagList enchList = stack.getEnchantmentTagList();
        if (enchList == null) return map;

        for (int i = 0; i < enchList.tagCount(); i++) {
            NBTTagCompound tag = (NBTTagCompound) enchList.tagAt(i);
            short id = tag.getShort("id");
            short lvl = tag.getShort("lvl");
            map.put((int) id, (int) lvl);
        }
        return map;
    }

    public static void setEnchantments(Map enchMap, ItemStack stack) {}

    /**
     * Walks every stack in {@code stacks} and returns the highest level of
     * enchantment {@code enchId} found across all of them. Mirrors vanilla
     * 1.5.2 EnchantmentHelper.getMaxEnchantmentLevel.
     */
    public static int getMaxEnchantmentLevel(int enchId, ItemStack[] stacks) {
        if (stacks == null) return 0;
        int max = 0;
        for (ItemStack stack : stacks) {
            int lvl = getEnchantmentLevel(enchId, stack);
            if (lvl > max) max = lvl;
        }
        return max;
    }

    /**
     * Returns the modifier of protection enchantments on armors.
     * Reimplements vanilla 1.5.2 EnchantmentHelper + EnchantmentProtection logic.
     */
    public static int getEnchantmentModifierDamage(ItemStack[] stacks, DamageSource source) {
        if (stacks == null) return 0;

        int totalModifier = 0;

        for (ItemStack stack : stacks) {
            if (stack == null) continue;
            NBTTagList enchList = stack.getEnchantmentTagList();
            if (enchList == null) continue;

            for (int i = 0; i < enchList.tagCount(); i++) {
                NBTTagCompound tag = (NBTTagCompound) enchList.tagAt(i);
                short id = tag.getShort("id");
                short lvl = tag.getShort("lvl");
                totalModifier += calcProtectionModifier(id, lvl, source);
            }
        }

        if (totalModifier > 25) {
            totalModifier = 25;
        }

        return (totalModifier + 1 >> 1) + enchantmentRand.nextInt((totalModifier >> 1) + 1);
    }

    /**
     * Calculates protection modifier for a single enchantment.
     * Mirrors EnchantmentProtection.calcModifierDamage from vanilla 1.5.2.
     */
    private static int calcProtectionModifier(int enchId, int level, DamageSource source) {
        if (source.canHarmInCreative()) return 0;

        // Determine protection type from enchantment ID
        // 0 = protection (all), 1 = fire protection, 2 = feather falling, 3 = blast protection, 4 = projectile protection
        int protectionType;
        if (enchId == 0) protectionType = 0;      // protection
        else if (enchId == 1) protectionType = 1;  // fire protection
        else if (enchId == 2) protectionType = 2;  // feather falling
        else if (enchId == 3) protectionType = 3;  // blast protection
        else if (enchId == 4) protectionType = 4;  // projectile protection
        else return 0; // not a protection enchantment

        float base = (float)(6 + level * level) / 3.0F;
        if (protectionType == 0) return MathHelper.floor_float(base * 0.75F);
        if (protectionType == 1 && source.isFireDamage()) return MathHelper.floor_float(base * 1.25F);
        if (protectionType == 2 && source == DamageSource.fall) return MathHelper.floor_float(base * 2.5F);
        if (protectionType == 3 && source.isExplosion()) return MathHelper.floor_float(base * 1.5F);
        if (protectionType == 4 && source.isProjectile()) return MathHelper.floor_float(base * 1.5F);
        return 0;
    }

    /**
     * Returns the (magic) extra damage of enchantments on the attacker's held item.
     * Reimplements vanilla 1.5.2 EnchantmentHelper + EnchantmentDamage logic.
     */
    public static int getEnchantmentModifierLiving(EntityLiving attacker, EntityLiving target) {
        ItemStack held = attacker.getHeldItem();
        if (held == null) return 0;

        NBTTagList enchList = held.getEnchantmentTagList();
        if (enchList == null) return 0;

        int totalModifier = 0;

        for (int i = 0; i < enchList.tagCount(); i++) {
            NBTTagCompound tag = (NBTTagCompound) enchList.tagAt(i);
            short id = tag.getShort("id");
            short lvl = tag.getShort("lvl");
            totalModifier += calcDamageModifier(id, lvl, target);
        }

        return totalModifier > 0 ? 1 + enchantmentRand.nextInt(totalModifier) : 0;
    }

    /**
     * Calculates damage modifier for a single enchantment.
     * Mirrors EnchantmentDamage.calcModifierLiving from vanilla 1.5.2.
     */
    private static int calcDamageModifier(int enchId, int level, EntityLiving target) {
        // 16 = sharpness (all), 17 = smite (undead), 18 = bane of arthropods
        if (enchId == 16) return MathHelper.floor_float((float) level * 2.75F);
        if (enchId == 17 && target.getCreatureAttribute() == EnumCreatureAttribute.UNDEAD)
            return MathHelper.floor_float((float) level * 4.5F);
        if (enchId == 18 && target.getCreatureAttribute() == EnumCreatureAttribute.ARTHROPOD)
            return MathHelper.floor_float((float) level * 4.5F);
        return 0;
    }

    // The methods below mirror vanilla 1.5.2 EnchantmentHelper.
    // Earlier versions delegated to per-entity helper methods like
    // entity.getKnockbackEnchantLevel(), but those only existed on
    // Modern-Common's stub EntityLiving — at runtime the real vanilla
    // 1.5.2 EntityLiving is loaded, and it doesn't expose them, so the
    // delegating versions threw NoSuchMethodError every tick.
    // Instead we walk the entity's items directly via getHeldItem() /
    // getLastActiveItems(), which DO exist on real EntityLiving.

    public static int getKnockbackModifier(EntityLiving attacker, EntityLiving target) {
        return getEnchantmentLevel(Enchantment.knockback.effectId, attacker.getHeldItem());
    }

    public static int getFireAspectModifier(EntityLiving entity) {
        return getEnchantmentLevel(Enchantment.fireAspect.effectId, entity.getHeldItem());
    }

    public static int getRespiration(EntityLiving entity) {
        return getMaxEnchantmentLevel(Enchantment.respiration.effectId, entity.getLastActiveItems());
    }

    public static int getEfficiencyModifier(EntityLiving entity) {
        return getEnchantmentLevel(Enchantment.efficiency.effectId, entity.getHeldItem());
    }

    public static int getUnbreakingModifier(EntityLiving entity) {
        return getEnchantmentLevel(Enchantment.unbreaking.effectId, entity.getHeldItem());
    }

    public static int getFortuneModifier(EntityLiving entity) {
        return getEnchantmentLevel(Enchantment.fortune.effectId, entity.getHeldItem());
    }

    public static int getLootingModifier(EntityLiving entity) {
        return getEnchantmentLevel(Enchantment.looting.effectId, entity.getHeldItem());
    }

    public static boolean getAquaAffinityModifier(EntityLiving entity) {
        return getMaxEnchantmentLevel(Enchantment.aquaAffinity.effectId, entity.getLastActiveItems()) > 0;
    }

    public static boolean getSilkTouchModifier(EntityLiving entity) {
        // getSilkTouchEnchant is a bridge method and must live on the
        // NON-shadowed EntityPlayer (the runtime EntityLiving is FC's real
        // 1.5.2 class, which doesn't have it — calling it there is a
        // NoSuchMethodError). PlayerBridge overrides it with the modern
        // player's held item. Non-player mobs take the real 1.5.2 path.
        if (entity instanceof EntityPlayer) {
            return ((EntityPlayer) entity).getSilkTouchEnchant();
        }
        return getEnchantmentLevel(Enchantment.silkTouch.effectId, entity.getHeldItem()) > 0;
    }

    public static int func_92098_i(EntityLiving entity) { return 0; }
    public static ItemStack addRandomEnchantment(Random rand, ItemStack stack, int level) {
        // Simplified enchantment: pick a random valid enchantment and apply it
        List<Enchantment> valid = new java.util.ArrayList<>();
        for (int i = 0; i < Enchantment.enchantmentsList.length; i++) {
            Enchantment ench = Enchantment.enchantmentsList[i];
            if (ench != null && ench.canApply(stack)) {
                valid.add(ench);
            }
        }
        if (!valid.isEmpty()) {
            Enchantment chosen = valid.get(rand.nextInt(valid.size()));
            int enchLevel = 1 + rand.nextInt(chosen.getMaxLevel());
            stack.addEnchantment(chosen, enchLevel);
        }
        return stack;
    }
    public static List buildEnchantmentList(Random rand, ItemStack stack, int level) { return null; }
    public static int calcItemStackEnchantability(Random rand, int enchSlot, int power, ItemStack stack) { return 0; }
    public static ItemStack func_92099_a(Enchantment ench, EntityLiving entity) { return null; }
}
