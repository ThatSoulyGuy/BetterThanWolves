package btw.modern;

import java.util.ArrayList;
import java.util.List;

public abstract class Container {

    public List inventoryItemStacks = new ArrayList();
    public List inventorySlots = new ArrayList();
    public int windowId;
    public List crafters = new ArrayList();

    public void detectAndSendChanges() {}

    public abstract boolean canInteractWith(EntityPlayer player);

    public ItemStack slotClick(int slotId, int clickedButton, int mode, EntityPlayer player) {
        return null;
    }

    public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex) {
        return null;
    }

    public void onContainerClosed(EntityPlayer player) {}

    public void onCraftMatrixChanged(IInventory inventory) {}

    public List getInventory() {
        return new ArrayList();
    }

    protected Slot addSlotToContainer(Slot slot) {
        inventorySlots.add(slot);
        return slot;
    }

    protected boolean mergeItemStack(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection) {
        return false;
    }

    public Slot getSlot(int slotIndex) {
        return null;
    }

    public void putStackInSlot(int slotIndex, ItemStack stack) {}

    public boolean enchantItem(EntityPlayer player, int enchantmentIndex) {
        return false;
    }

    public void updateProgressBar(int id, int data) {}

    public short getNextTransactionID(InventoryPlayer playerInventory) {
        return 0;
    }

    public boolean isPlayerNotUsingContainer(EntityPlayer player) {
        return false;
    }

    public void retrySlotClick(int slotId, int clickedButton, boolean holding, EntityPlayer player) {}

    public void onCraftGuiClosed(EntityPlayer player) {}
    public void addCraftingToCrafters(ICrafting crafting) {}
    public void onCraftGuiOpened(ICrafting crafting) {}
}
