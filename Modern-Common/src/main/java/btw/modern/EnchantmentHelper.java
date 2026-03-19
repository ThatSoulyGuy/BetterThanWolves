package btw.modern;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class EnchantmentHelper {
    public static int getEnchantmentLevel(int enchId, ItemStack stack) { return 0; }
    public static Map getEnchantments(ItemStack stack) { return null; }
    public static void setEnchantments(Map enchMap, ItemStack stack) {}
    public static int getMaxEnchantmentLevel(int enchId, ItemStack[] stacks) { return 0; }
    public static int getEnchantmentModifierDamage(ItemStack[] stacks, DamageSource source) { return 0; }
    public static int getEnchantmentModifierLiving(EntityLiving attacker, EntityLiving target) { return 0; }
    public static int getKnockbackModifier(EntityLiving attacker, EntityLiving target) { return 0; }
    public static int getFireAspectModifier(EntityLiving entity) { return 0; }
    public static int getRespiration(EntityLiving entity) { return 0; }
    public static int getEfficiencyModifier(EntityLiving entity) { return 0; }
    public static int getUnbreakingModifier(EntityLiving entity) { return 0; }
    public static int getFortuneModifier(EntityLiving entity) { return 0; }
    public static int getLootingModifier(EntityLiving entity) { return 0; }
    public static boolean getAquaAffinityModifier(EntityLiving entity) { return false; }
    public static boolean getSilkTouchModifier(EntityLiving entity) { return false; }
    public static int func_92098_i(EntityLiving entity) { return 0; }
    public static ItemStack addRandomEnchantment(Random rand, ItemStack stack, int level) { return stack; }
    public static List buildEnchantmentList(Random rand, ItemStack stack, int level) { return null; }
    public static int calcItemStackEnchantability(Random rand, int enchSlot, int power, ItemStack stack) { return 0; }
    public static ItemStack func_92099_a(Enchantment ench, EntityLiving entity) { return null; }
}
