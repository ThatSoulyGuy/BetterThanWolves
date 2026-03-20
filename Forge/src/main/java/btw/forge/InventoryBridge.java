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
        return new btw.modern.ItemStack(legacyId, count, damage);
    }

    /** Returns the underlying Forge inventory. */
    public Inventory getRealInventory() {
        return inventory;
    }
}
