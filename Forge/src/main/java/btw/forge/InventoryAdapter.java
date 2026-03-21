package btw.forge;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Adapts an FC {@link btw.modern.IInventory} to MC 1.20.1
 * {@link net.minecraft.world.Container} so that it can be used
 * with the modern inventory/menu system.
 *
 * <p>Item stacks are converted on-the-fly between FC and MC formats
 * using {@link ItemStackHelper}. The FC inventory remains the
 * authoritative data store; this adapter is a live view.</p>
 */
public class InventoryAdapter implements net.minecraft.world.Container {

    private static final Logger LOGGER = LogManager.getLogger("BTW-InventoryAdapter");

    private final btw.modern.IInventory fcInventory;

    public InventoryAdapter(btw.modern.IInventory fcInventory) {
        this.fcInventory = fcInventory;
    }

    /**
     * Returns the underlying FC inventory.
     */
    public btw.modern.IInventory getFcInventory() {
        return fcInventory;
    }

    @Override
    public int getContainerSize() {
        return fcInventory.getSizeInventory();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < fcInventory.getSizeInventory(); i++) {
            btw.modern.ItemStack fcStack = fcInventory.getStackInSlot(i);
            if (fcStack != null && fcStack.stackSize > 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        btw.modern.ItemStack fcStack = fcInventory.getStackInSlot(slot);
        return ItemStackHelper.toMcStack(fcStack);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        btw.modern.ItemStack fcStack = fcInventory.decrStackSize(slot, amount);
        return ItemStackHelper.toMcStack(fcStack);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        btw.modern.ItemStack fcStack = fcInventory.getStackInSlotOnClosing(slot);
        return ItemStackHelper.toMcStack(fcStack);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        btw.modern.ItemStack fcStack = ItemStackHelper.toFcStack(stack);
        fcInventory.setInventorySlotContents(slot, fcStack);
    }

    @Override
    public void setChanged() {
        fcInventory.onInventoryChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        // FC validity checks are done at the Container level, not inventory level.
        // Always return true here; FCContainerMenu handles stillValid.
        return true;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < fcInventory.getSizeInventory(); i++) {
            fcInventory.setInventorySlotContents(i, null);
        }
    }

    @Override
    public int getMaxStackSize() {
        return fcInventory.getInventoryStackLimit();
    }
}
