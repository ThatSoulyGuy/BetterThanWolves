package btw.forge;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility class for converting between FC ({@link btw.modern.ItemStack}) and
 * MC 1.20.1 ({@link net.minecraft.world.item.ItemStack}) item stacks.
 *
 * <p>Conversion relies on {@link ProxyRegistry} to map legacy numeric IDs
 * to modern Item/Block instances and vice versa.</p>
 */
public class ItemStackHelper {

    private static final Logger LOGGER = LogManager.getLogger("BTW-ItemStackHelper");

    /**
     * Convert an FC ItemStack to an MC ItemStack.
     *
     * @param fcStack the FC item stack (may be null)
     * @return the equivalent MC item stack, or {@link net.minecraft.world.item.ItemStack#EMPTY}
     *         if the input is null or the legacy ID cannot be resolved
     */
    public static net.minecraft.world.item.ItemStack toMcStack(btw.modern.ItemStack fcStack) {
        if (fcStack == null) return net.minecraft.world.item.ItemStack.EMPTY;

        int legacyId = fcStack.itemID;
        if (legacyId <= 0) return net.minecraft.world.item.ItemStack.EMPTY;

        // Try as an item first
        Item modernItem = ProxyRegistry.getModernItem(legacyId);

        // If no item mapping, try as a block and get its item form
        if (modernItem == null) {
            net.minecraft.world.level.block.Block block = ProxyRegistry.getModernBlock(legacyId);
            if (block != null) {
                modernItem = block.asItem();
            }
        }

        if (modernItem == null) {
            LOGGER.warn("Cannot convert FC ItemStack with legacy ID {} to MC ItemStack: no mapping found.", legacyId);
            return net.minecraft.world.item.ItemStack.EMPTY;
        }

        net.minecraft.world.item.ItemStack mcStack = new net.minecraft.world.item.ItemStack(
                modernItem, fcStack.stackSize);

        // Carry over damage value (used for durability and metadata subtypes)
        if (fcStack.getItemDamage() > 0) {
            mcStack.setDamageValue(fcStack.getItemDamage());
        }

        // Copy NBT/enchantments if the FC stack has tag data
        if (fcStack.hasTagCompound()) {
            CompoundTag mcTag = toMcTag(fcStack.getTagCompound());
            if (mcTag != null) {
                mcStack.setTag(mcTag);
            }
        }

        // Convert FC enchantments (ench tag with numeric IDs) to
        // MC 1.20.1 format (Enchantments tag with string registry names).
        // FC stores: {ench: [{id: 16s, lvl: 1s}]}
        // MC wants:  applied via ItemStack.enchant() which handles format
        convertFcEnchantments(mcStack);

        return mcStack;
    }

    /**
     * Converts FC-format enchantments ({@code ench} tag with numeric IDs)
     * to MC 1.20.1 format by calling {@code mcStack.enchant()}.
     * FC and MC 1.5.2 use the same numeric enchantment IDs.
     */
    private static void convertFcEnchantments(net.minecraft.world.item.ItemStack mcStack) {
        CompoundTag tag = mcStack.getTag();
        if (tag == null || !tag.contains("ench", 9)) return; // 9 = list tag

        net.minecraft.nbt.ListTag enchList = tag.getList("ench", 10); // 10 = compound
        tag.remove("ench"); // Remove FC format

        for (int i = 0; i < enchList.size(); i++) {
            CompoundTag enchTag = enchList.getCompound(i);
            int enchId = enchTag.getShort("id");
            int enchLvl = enchTag.getShort("lvl");

            net.minecraft.world.item.enchantment.Enchantment mcEnch = fcEnchIdToMc(enchId);
            if (mcEnch != null && enchLvl > 0) {
                mcStack.enchant(mcEnch, enchLvl);
            }
        }
    }

    /** Maps FC/MC 1.5.2 numeric enchantment IDs to MC 1.20.1 Enchantment objects. */
    private static net.minecraft.world.item.enchantment.Enchantment fcEnchIdToMc(int fcId) {
        var E = net.minecraft.world.item.enchantment.Enchantments.class;
        return switch (fcId) {
            case 0  -> net.minecraft.world.item.enchantment.Enchantments.ALL_DAMAGE_PROTECTION;
            case 1  -> net.minecraft.world.item.enchantment.Enchantments.FIRE_PROTECTION;
            case 2  -> net.minecraft.world.item.enchantment.Enchantments.FALL_PROTECTION;
            case 3  -> net.minecraft.world.item.enchantment.Enchantments.BLAST_PROTECTION;
            case 4  -> net.minecraft.world.item.enchantment.Enchantments.PROJECTILE_PROTECTION;
            case 5  -> net.minecraft.world.item.enchantment.Enchantments.RESPIRATION;
            case 6  -> net.minecraft.world.item.enchantment.Enchantments.AQUA_AFFINITY;
            case 7  -> net.minecraft.world.item.enchantment.Enchantments.THORNS;
            case 16 -> net.minecraft.world.item.enchantment.Enchantments.SHARPNESS;
            case 17 -> net.minecraft.world.item.enchantment.Enchantments.SMITE;
            case 18 -> net.minecraft.world.item.enchantment.Enchantments.BANE_OF_ARTHROPODS;
            case 19 -> net.minecraft.world.item.enchantment.Enchantments.KNOCKBACK;
            case 20 -> net.minecraft.world.item.enchantment.Enchantments.FIRE_ASPECT;
            case 21 -> net.minecraft.world.item.enchantment.Enchantments.MOB_LOOTING;
            case 32 -> net.minecraft.world.item.enchantment.Enchantments.BLOCK_EFFICIENCY;
            case 33 -> net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH;
            case 34 -> net.minecraft.world.item.enchantment.Enchantments.UNBREAKING;
            case 35 -> net.minecraft.world.item.enchantment.Enchantments.BLOCK_FORTUNE;
            case 48 -> net.minecraft.world.item.enchantment.Enchantments.POWER_ARROWS;
            case 49 -> net.minecraft.world.item.enchantment.Enchantments.PUNCH_ARROWS;
            case 50 -> net.minecraft.world.item.enchantment.Enchantments.FLAMING_ARROWS;
            case 51 -> net.minecraft.world.item.enchantment.Enchantments.INFINITY_ARROWS;
            default -> null;
        };
    }

    /**
     * Convert an MC ItemStack to an FC ItemStack.
     *
     * @param mcStack the MC item stack (may be null or empty)
     * @return the equivalent FC item stack, or null if the input is null/empty
     *         or the item cannot be mapped to a legacy ID
     */
    public static btw.modern.ItemStack toFcStack(net.minecraft.world.item.ItemStack mcStack) {
        if (mcStack == null || mcStack.isEmpty()) return null;

        // getOrCreateLegacyId always succeeds: if the item has no FC
        // equivalent, a headless passthrough entry is created so the
        // item roundtrips losslessly through FC container logic.
        int legacyId = ProxyRegistry.getOrCreateLegacyId(mcStack.getItem());

        if (legacyId <= 0) {
            LOGGER.debug("Cannot convert MC ItemStack {} to FC ItemStack: no legacy ID mapping.", mcStack);
            return null;
        }

        btw.modern.ItemStack fcStack = new btw.modern.ItemStack(
                legacyId, mcStack.getCount(), mcStack.getDamageValue());

        // Copy NBT/enchantments from MC CompoundTag to FC NBTTagCompound
        CompoundTag mcTag = mcStack.getTag();
        if (mcTag != null && !mcTag.isEmpty()) {
            fcStack.setTagCompound(new ForgeNBTCompound(mcTag.copy()));
        }

        return fcStack;
    }

    /**
     * Convert an FC {@link btw.modern.NBTTagCompound} to an MC {@link CompoundTag}.
     *
     * <p>If the FC tag is a {@link ForgeNBTCompound} (backed by a real CompoundTag),
     * the inner tag is extracted and copied. Otherwise, conversion is not possible
     * because the plain {@link btw.modern.NBTTagCompound} does not expose its keys
     * for iteration.</p>
     *
     * @param fcTag the FC NBT compound (may be null)
     * @return a copy of the underlying MC CompoundTag, or null if conversion is not possible
     */
    @SuppressWarnings("unchecked")
    static CompoundTag toMcTag(btw.modern.NBTTagCompound fcTag) {
        if (fcTag == null) return null;

        if (fcTag instanceof ForgeNBTCompound fnbt) {
            CompoundTag result = fnbt.getTag().copy();
            // FC code uses generic setTag(String, NBTBase) which stores in
            // the inherited tagMap, NOT in the CompoundTag. Merge any tagMap
            // data that the CompoundTag doesn't have (e.g., enchantments
            // added by ItemStack.addEnchantment).
            mergeTagMapInto(fcTag, result);
            return result;
        }

        // Plain NBTTagCompound — iterate its tagMap and build a real CompoundTag.
        // FC creates these when adding enchantments, custom data, etc.
        CompoundTag result = new CompoundTag();
        try {
            java.lang.reflect.Field mapField =
                    btw.modern.NBTTagCompound.class.getDeclaredField("tagMap");
            mapField.setAccessible(true);
            java.util.Map<String, Object> tagMap =
                    (java.util.Map<String, Object>) mapField.get(fcTag);

            for (var entry : tagMap.entrySet()) {
                String key = entry.getKey();
                Object val = entry.getValue();
                if (val instanceof Integer i) result.putInt(key, i);
                else if (val instanceof String s) result.putString(key, s);
                else if (val instanceof Boolean b) result.putBoolean(key, b);
                else if (val instanceof Byte b) result.putByte(key, b);
                else if (val instanceof Short s) result.putShort(key, s);
                else if (val instanceof Long l) result.putLong(key, l);
                else if (val instanceof Float f) result.putFloat(key, f);
                else if (val instanceof Double d) result.putDouble(key, d);
                else if (val instanceof byte[] ba) result.putByteArray(key, ba);
                else if (val instanceof int[] ia) result.putIntArray(key, ia);
                else if (val instanceof btw.modern.NBTTagCompound sub) {
                    CompoundTag mcSub = toMcTag(sub);
                    if (mcSub != null) result.put(key, mcSub);
                } else if (val instanceof btw.modern.NBTTagList list) {
                    net.minecraft.nbt.ListTag mcList = toMcList(list);
                    if (mcList != null) result.put(key, mcList);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to convert plain NBTTagCompound: {}", e.getMessage());
        }
        return result;
    }

    /**
     * Converts an FC NBTTagList to an MC ListTag.
     */
    static net.minecraft.nbt.ListTag toMcList(btw.modern.NBTTagList fcList) {
        if (fcList == null) return null;
        net.minecraft.nbt.ListTag result = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < fcList.tagCount(); i++) {
            btw.modern.NBTBase tag = fcList.tagAt(i);
            if (tag instanceof btw.modern.NBTTagCompound sub) {
                CompoundTag mcSub = toMcTag(sub);
                if (mcSub != null) result.add(mcSub);
            }
            // Extend for other tag types as needed
        }
        return result;
    }

    /**
     * Merges data from NBTTagCompound's inherited tagMap into a CompoundTag.
     * ForgeNBTCompound stores typed data in the real CompoundTag, but FC's
     * generic setTag(String, NBTBase) stores in the inherited tagMap. This
     * method picks up anything in tagMap that the CompoundTag is missing.
     */
    @SuppressWarnings("unchecked")
    private static void mergeTagMapInto(btw.modern.NBTTagCompound fcTag, CompoundTag target) {
        try {
            java.lang.reflect.Field mapField =
                    btw.modern.NBTTagCompound.class.getDeclaredField("tagMap");
            mapField.setAccessible(true);
            java.util.Map<String, Object> tagMap =
                    (java.util.Map<String, Object>) mapField.get(fcTag);

            for (var entry : tagMap.entrySet()) {
                String key = entry.getKey();
                if (target.contains(key)) continue; // CompoundTag data wins
                Object val = entry.getValue();
                if (val instanceof Integer i) target.putInt(key, i);
                else if (val instanceof String s) target.putString(key, s);
                else if (val instanceof Boolean b) target.putBoolean(key, b);
                else if (val instanceof Byte b) target.putByte(key, b);
                else if (val instanceof Short s) target.putShort(key, s);
                else if (val instanceof Long l) target.putLong(key, l);
                else if (val instanceof Float f) target.putFloat(key, f);
                else if (val instanceof Double d) target.putDouble(key, d);
                else if (val instanceof byte[] ba) target.putByteArray(key, ba);
                else if (val instanceof int[] ia) target.putIntArray(key, ia);
                else if (val instanceof btw.modern.NBTTagCompound sub) {
                    CompoundTag mcSub = toMcTag(sub);
                    if (mcSub != null) target.put(key, mcSub);
                } else if (val instanceof btw.modern.NBTTagList list) {
                    net.minecraft.nbt.ListTag mcList = toMcList(list);
                    if (mcList != null) target.put(key, mcList);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("mergeTagMapInto failed: {}", e.getMessage());
        }
    }

    /**
     * Check whether a given FC ItemStack can be mapped to a valid MC item.
     *
     * @param fcStack the FC item stack to check
     * @return true if the stack is non-null and its legacy ID resolves to a modern item or block
     */
    public static boolean canConvert(btw.modern.ItemStack fcStack) {
        if (fcStack == null) return false;
        int legacyId = fcStack.itemID;
        if (ProxyRegistry.getModernItem(legacyId) != null) return true;
        return ProxyRegistry.getModernBlock(legacyId) != null;
    }
}
