package btw.forge;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * An {@link ArmorMaterial} built per FC armor piece. All values are read from the
 * FC armor item (which lives in the btw.modern engine layer), so this is a thin
 * bridge — no armor logic is reimplemented here:
 * <ul>
 *   <li>defense  = the FC item's {@code damageReduceAmount} (FC's per-slot value,
 *       already adjusted by variant subclasses like Wool)</li>
 *   <li>durability = the FC item's {@code getMaxDamage()} (handles Padded/Refined/etc.)</li>
 *   <li>enchant value = the FC item's {@code getItemEnchantability()} (0 = non-enchantable)</li>
 * </ul>
 * FC's 1.5.2 defense numbers are on the same scale as 1.20.1 (iron/diamond are
 * byte-identical to vanilla), so they map directly. Toughness/knockback are 0 to
 * stay faithful to FC, which had neither.
 */
public class FCArmorMaterial implements ArmorMaterial {

    private final int defense;
    private final int durability;
    private final int enchantValue;
    private final String name;          // texture base name (no namespace)
    private final SoundEvent equipSound;

    public FCArmorMaterial(btw.modern.ItemArmor fcArmor) {
        this.defense = Math.max(0, fcArmor.damageReduceAmount);
        this.durability = Math.max(1, fcArmor.getMaxDamage());
        this.enchantValue = Math.max(0, fcArmor.getItemEnchantability());
        this.name = textureBaseFor(fcArmor);
        this.equipSound = equipSoundFor(fcArmor.getArmorMaterial());
    }

    /** Maps the concrete FC armor class to its worn-armor texture base name. */
    private static String textureBaseFor(btw.modern.ItemArmor fcArmor) {
        switch (fcArmor.getClass().getSimpleName()) {
            case "FCItemArmorChain":   return "chain";
            case "FCItemArmorIron":    return "iron";
            case "FCItemArmorGold":    return "gold";
            case "FCItemArmorDiamond": return "diamond";
            case "FCItemArmorRefined": return "plate";
            case "FCItemArmorPadded":  return "padded";
            case "FCItemArmorGimp":    return "gimp";
            case "FCItemArmorTanned":  return "tanned";
            case "FCItemArmorWool":    return "wool";
            case "FCItemArmorLeather":
            default:                   return "cloth";
        }
    }

    private static SoundEvent equipSoundFor(btw.modern.EnumArmorMaterial mat) {
        if (mat == null) return SoundEvents.ARMOR_EQUIP_LEATHER;
        switch (mat) {
            case IRON:    return SoundEvents.ARMOR_EQUIP_IRON;
            case GOLD:    return SoundEvents.ARMOR_EQUIP_GOLD;
            case DIAMOND: return SoundEvents.ARMOR_EQUIP_DIAMOND;
            case CHAIN:   return SoundEvents.ARMOR_EQUIP_CHAIN;
            case CLOTH:
            default:      return SoundEvents.ARMOR_EQUIP_LEATHER;
        }
    }

    @Override public int getDurabilityForType(ArmorItem.Type type) { return durability; }
    @Override public int getDefenseForType(ArmorItem.Type type) { return defense; }
    @Override public int getEnchantmentValue() { return enchantValue; }
    @Override public SoundEvent getEquipSound() { return equipSound; }
    @Override public Ingredient getRepairIngredient() { return Ingredient.EMPTY; }
    @Override public String getName() { return BTWForgeMod.MOD_ID + ":" + name; }
    @Override public float getToughness() { return 0.0F; }
    @Override public float getKnockbackResistance() { return 0.0F; }
}
