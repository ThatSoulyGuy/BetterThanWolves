package btw.modern;

/**
 * Vanilla 1.5.2 ContainerMerchant with FC's modified slot positions.
 * Full implementation so it works in dev (where this class loads instead
 * of the remapped FC version).
 */
public class ContainerMerchant extends Container {
    private IMerchant theMerchant;
    private InventoryMerchant merchantInventory;
    private final World theWorld;

    public int m_iAssociatedVillagerTradeLevel = 0;
    public int m_iAssociatedVillagerTradeXP = 0;
    public int m_iAssociatedVillagerTradeMaxXP = 0;

    public ContainerMerchant(InventoryPlayer playerInv, IMerchant merchant, World world) {
        this.theMerchant = merchant;
        this.theWorld = world;
        this.merchantInventory = new InventoryMerchant(playerInv.player, merchant);

        // FC's modified slot positions (y offset from vanilla)
        this.addSlotToContainer(new Slot(this.merchantInventory, 0, 36, 119));
        this.addSlotToContainer(new Slot(this.merchantInventory, 1, 62, 119));
        this.addSlotToContainer(new SlotMerchantResult(playerInv.player, merchant, this.merchantInventory, 2, 120, 119));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlotToContainer(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 157 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlotToContainer(new Slot(playerInv, col, 8 + col * 18, 215));
        }
    }

    public InventoryMerchant getMerchantInventory() { return this.merchantInventory; }

    public void setCurrentRecipeIndex(int index) {
        this.merchantInventory.setCurrentRecipeIndex(index);
    }

    public boolean canInteractWith(EntityPlayer player) {
        return this.theMerchant.getCustomer() == player;
    }

    public void onCraftMatrixChanged(IInventory inv) {
        this.merchantInventory.resetRecipeAndSlots();
        super.onCraftMatrixChanged(inv);
    }

    public void onCraftGuiClosed(EntityPlayer player) {
        super.onCraftGuiClosed(player);
        this.theMerchant.setCustomer(null);
        if (!this.theWorld.isRemote) {
            ItemStack stack = this.merchantInventory.getStackInSlotOnClosing(0);
            if (stack != null) player.dropPlayerItem(stack);
            stack = this.merchantInventory.getStackInSlotOnClosing(1);
            if (stack != null) player.dropPlayerItem(stack);
        }
    }

    public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex) {
        ItemStack result = null;
        Slot slot = (Slot) this.inventorySlots.get(slotIndex);
        if (slot != null && slot.getHasStack()) {
            ItemStack stack = slot.getStack();
            result = stack.copy();
            if (slotIndex == 2) {
                if (!this.mergeItemStack(stack, 3, 39, true)) return null;
                slot.onSlotChange(stack, result);
            } else if (slotIndex != 0 && slotIndex != 1) {
                if (slotIndex >= 3 && slotIndex < 30) {
                    if (!this.mergeItemStack(stack, 0, 2, false)) return null;
                } else if (slotIndex >= 30 && slotIndex < 39 && !this.mergeItemStack(stack, 0, 2, false)) {
                    return null;
                }
            } else if (!this.mergeItemStack(stack, 3, 39, true)) {
                return null;
            }
            if (stack.stackSize == 0) slot.putStack(null);
            else slot.onSlotChanged();
            if (stack.stackSize == result.stackSize) return null;
            slot.onPickupFromSlot(player, stack);
        }
        return result;
    }
}
