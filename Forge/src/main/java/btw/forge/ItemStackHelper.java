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

        return mcStack;
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

        int legacyId;

        // BlockItems should map via the block's legacy ID
        if (mcStack.getItem() instanceof net.minecraft.world.item.BlockItem bi) {
            legacyId = ProxyRegistry.getBlockId(bi.getBlock());
        } else {
            legacyId = ProxyRegistry.getItemId(mcStack.getItem());
        }

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
    static CompoundTag toMcTag(btw.modern.NBTTagCompound fcTag) {
        if (fcTag == null) return null;

        if (fcTag instanceof ForgeNBTCompound fnbt) {
            // Already backed by a real CompoundTag — return a copy to avoid shared state
            return fnbt.getTag().copy();
        }

        // Plain NBTTagCompound with private tagMap — cannot iterate keys
        LOGGER.warn("Cannot convert plain NBTTagCompound to CompoundTag: " +
                "tag is not a ForgeNBTCompound. NBT data will be lost.");
        return null;
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
