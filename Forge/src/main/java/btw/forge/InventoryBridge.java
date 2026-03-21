package btw.forge;

import net.minecraft.world.entity.player.Inventory;

/**
 * Bridges {@link Inventory} to {@link btw.modern.InventoryPlayer}.
 * FC code accesses inventory slots, current item, and armor through
 * btw.modern.InventoryPlayer fields and methods.
 */
public class InventoryBridge extends btw.modern.InventoryPlayer {
    private final Inventory inventory;

    public InventoryBridge(Inventory inventory) {
        super(null); // btw.modern.InventoryPlayer(EntityPlayer) constructor
        this.inventory = inventory;
        sync();
    }

    /** Copy current inventory state to FC fields */
    public void sync() {
        this.currentItem = inventory.selected;

        // Sync main inventory (36 slots)
        for (int i = 0; i < mainInventory.length && i < inventory.items.size(); i++) {
            net.minecraft.world.item.ItemStack modern = inventory.items.get(i);
            mainInventory[i] = wrapItemStack(modern);
        }

        // Sync armor inventory (4 slots)
        for (int i = 0; i < armorInventory.length && i < inventory.armor.size(); i++) {
            net.minecraft.world.item.ItemStack modern = inventory.armor.get(i);
            armorInventory[i] = wrapItemStack(modern);
        }
    }

    @Override
    public btw.modern.ItemStack getCurrentItem() {
        int selected = inventory.selected;
        if (selected >= 0 && selected < 9 && selected < inventory.items.size()) {
            return wrapItemStack(inventory.items.get(selected));
        }
        return null;
    }

    @Override
    public btw.modern.ItemStack getStackInSlot(int slot) {
        if (slot >= 0 && slot < mainInventory.length && slot < inventory.items.size()) {
            return wrapItemStack(inventory.items.get(slot));
        }
        // Armor slots come after main inventory in FC layout
        int armorSlot = slot - mainInventory.length;
        if (armorSlot >= 0 && armorSlot < armorInventory.length
                && armorSlot < inventory.armor.size()) {
            return wrapItemStack(inventory.armor.get(armorSlot));
        }
        return null;
    }

    @Override
    public void setInventorySlotContents(int slot, btw.modern.ItemStack stack) {
        // Write through to the REAL MC inventory so changes persist
        net.minecraft.world.item.ItemStack mcStack = ItemStackHelper.toMcStack(stack);
        if (slot >= 0 && slot < inventory.items.size()) {
            inventory.items.set(slot, mcStack);
        } else {
            int armorSlot = slot - mainInventory.length;
            if (armorSlot >= 0 && armorSlot < inventory.armor.size()) {
                inventory.armor.set(armorSlot, mcStack);
            }
        }
        // Also update FC snapshot
        super.setInventorySlotContents(slot, stack);
    }

    @Override
    public btw.modern.ItemStack decrStackSize(int slot, int count) {
        // Decrement in the REAL MC inventory
        net.minecraft.world.item.ItemStack mcStack;
        if (slot >= 0 && slot < inventory.items.size()) {
            mcStack = inventory.items.get(slot);
        } else {
            int armorSlot = slot - mainInventory.length;
            if (armorSlot >= 0 && armorSlot < inventory.armor.size()) {
                mcStack = inventory.armor.get(armorSlot);
            } else {
                return null;
            }
        }
        if (mcStack.isEmpty()) return null;

        net.minecraft.world.item.ItemStack removed;
        if (mcStack.getCount() <= count) {
            removed = mcStack.copy();
            if (slot >= 0 && slot < inventory.items.size()) {
                inventory.items.set(slot, net.minecraft.world.item.ItemStack.EMPTY);
            } else {
                inventory.armor.set(slot - mainInventory.length, net.minecraft.world.item.ItemStack.EMPTY);
            }
        } else {
            removed = mcStack.split(count);
        }
        // Sync FC snapshot
        sync();
        return ItemStackHelper.toFcStack(removed);
    }

    @Override
    public int getSizeInventory() {
        return mainInventory.length + armorInventory.length;
    }

    /**
     * Wraps a modern Forge ItemStack into a btw.modern.ItemStack.
     * Returns null for empty stacks.
     */
    private static btw.modern.ItemStack wrapItemStack(net.minecraft.world.item.ItemStack modern) {
        if (modern == null || modern.isEmpty()) return null;

        int legacyId;
        // Check if this is a block item with a proxy
        if (modern.getItem() instanceof net.minecraft.world.item.BlockItem bi) {
            legacyId = ProxyRegistry.getBlockId(bi.getBlock());
        } else {
            legacyId = ProxyRegistry.getItemId(modern.getItem());
        }

        // If the legacy ID doesn't map to any FC item or block, return null.
        // FC code does Item.itemsList[stack.itemID] without null checks, so
        // an unmapped item would NPE (e.g., FCBlockCampfire.onBlockActivated).
        if (legacyId <= 0) return null;
        boolean hasItem = legacyId < btw.modern.Item.itemsList.length
                && btw.modern.Item.itemsList[legacyId] != null;
        boolean hasBlock = legacyId < btw.modern.Block.blocksList.length
                && btw.modern.Block.blocksList[legacyId] != null;
        if (!hasItem && !hasBlock) return null;

        int damage = modern.getDamageValue();
        int count = modern.getCount();
        btw.modern.ItemStack fcStack = new btw.modern.ItemStack(legacyId, count, damage);

        // Copy NBT from MC stack to FC stack so accumulated data (fire chance,
        // last use time, custom names, etc.) persists across interactions.
        net.minecraft.nbt.CompoundTag mcTag = modern.getTag();
        if (mcTag != null && !mcTag.isEmpty()) {
            fcStack.setTagCompound(new btw.forge.ForgeNBTCompound(mcTag.copy()));
        }

        return fcStack;
    }

    /**
     * Writes FC ItemStack state back to the real MC inventory slot.
     * Call after FC code that may modify items (damageItem, stackSize changes).
     *
     * @param fcStack the FC ItemStack that was modified (from getCurrentItem etc.)
     * @param slot the inventory slot index, or -1 for current held item
     */
    public void writeBack(btw.modern.ItemStack fcStack, int slot) {
        if (slot < 0) slot = inventory.selected;
        if (slot < 0 || slot >= inventory.items.size()) return;

        net.minecraft.world.item.ItemStack mcStack = inventory.items.get(slot);
        if (mcStack == null || mcStack.isEmpty()) return;

        if (fcStack == null || fcStack.stackSize <= 0) {
            // Item consumed or broken — remove from MC inventory
            inventory.items.set(slot, net.minecraft.world.item.ItemStack.EMPTY);
            return;
        }

        // Sync NBT tag data FIRST (FC stores accumulated chance, time of last use,
        // custom names, enchantments, etc. in the tag compound)
        if (fcStack.hasTagCompound()) {
            btw.modern.NBTTagCompound fcTag = fcStack.getTagCompound();
            if (fcTag instanceof btw.forge.ForgeNBTCompound forgeTag) {
                mcStack.setTag(forgeTag.getTag().copy());
            } else {
                try {
                    net.minecraft.nbt.CompoundTag mcTag = ItemStackHelper.toMcTag(fcTag);
                    if (mcTag != null) mcStack.setTag(mcTag);
                } catch (Exception ignored) {}
            }
        }

        // Sync damage and count AFTER NBT (so setDamageValue wins over any
        // stale "Damage" key that was in the copied NBT)
        mcStack.setDamageValue(fcStack.getItemDamage());
        mcStack.setCount(fcStack.stackSize);
    }

    /**
     * Writes the current held item's FC state back to MC inventory.
     */
    public void writeBackCurrentItem(btw.modern.ItemStack fcStack) {
        writeBack(fcStack, inventory.selected);
    }

    /**
     * Flushes the entire FC inventory snapshot back to the real MC inventory.
     *
     * <p>FC code may mutate ItemStack objects in mainInventory[] directly
     * (e.g., {@code slotStack.stackSize += placeCount} during merge), which
     * bypasses the write-through in setInventorySlotContents/decrStackSize.
     * Call this after any FC operation that may have modified player inventory.</p>
     */
    public void writeBackAll() {
        for (int i = 0; i < mainInventory.length && i < inventory.items.size(); i++) {
            net.minecraft.world.item.ItemStack mcStack = ItemStackHelper.toMcStack(mainInventory[i]);
            inventory.items.set(i, mcStack);
        }
        for (int i = 0; i < armorInventory.length && i < inventory.armor.size(); i++) {
            net.minecraft.world.item.ItemStack mcStack = ItemStackHelper.toMcStack(armorInventory[i]);
            inventory.armor.set(i, mcStack);
        }
    }

    /** Returns the underlying Forge inventory. */
    public Inventory getRealInventory() {
        return inventory;
    }
}
