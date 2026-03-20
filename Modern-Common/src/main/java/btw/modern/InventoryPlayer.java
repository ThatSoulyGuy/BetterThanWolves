package btw.modern;

public class InventoryPlayer implements IInventory {
    public ItemStack[] mainInventory = new ItemStack[36];
    public ItemStack[] armorInventory = new ItemStack[4];
    public int currentItem = 0;
    public EntityPlayer player;
    private ItemStack itemStack;
    public boolean inventoryChanged = false;

    public InventoryPlayer(EntityPlayer player) {
        this.player = player;
    }

    public ItemStack getCurrentItem() {
        return this.currentItem < 9 && this.currentItem >= 0 ? this.mainInventory[this.currentItem] : null;
    }

    public static int getHotbarSize() { return 9; }
    public int getSizeInventory() { return mainInventory.length + 4; }
    public ItemStack getStackInSlot(int slot) {
        if (slot < mainInventory.length) {
            return mainInventory[slot];
        }
        return armorInventory[slot - mainInventory.length];
    }
    public ItemStack decrStackSize(int slot, int amount) {
        ItemStack[] inv = (slot < mainInventory.length) ? mainInventory : armorInventory;
        int idx = (slot < mainInventory.length) ? slot : slot - mainInventory.length;

        if (inv[idx] != null) {
            ItemStack result;
            if (inv[idx].stackSize <= amount) {
                result = inv[idx];
                inv[idx] = null;
                return result;
            } else {
                result = inv[idx].splitStack(amount);
                if (inv[idx].stackSize == 0) {
                    inv[idx] = null;
                }
                return result;
            }
        }
        return null;
    }
    public ItemStack getStackInSlotOnClosing(int slot) {
        ItemStack[] inv = (slot < mainInventory.length) ? mainInventory : armorInventory;
        int idx = (slot < mainInventory.length) ? slot : slot - mainInventory.length;

        if (inv[idx] != null) {
            ItemStack stack = inv[idx];
            inv[idx] = null;
            return stack;
        }
        return null;
    }
    public void setInventorySlotContents(int slot, ItemStack stack) {
        if (slot < mainInventory.length) {
            mainInventory[slot] = stack;
        } else {
            armorInventory[slot - mainInventory.length] = stack;
        }
    }
    public String getInvName() { return "container.inventory"; }
    public boolean isInvNameLocalized() { return false; }
    public int getInventoryStackLimit() { return 64; }
    public void onInventoryChanged() {}
    public boolean isUseableByPlayer(EntityPlayer player) { return true; }
    public void openChest() {}
    public void closeChest() {}
    public boolean isItemValidForSlot(int slot, ItemStack stack) { return true; }
    public boolean addItemStackToInventory(ItemStack stack) {
        if (stack == null || stack.stackSize == 0) {
            return false;
        }

        // First pass: try to merge with existing stacks
        if (stack.isStackable()) {
            for (int i = 0; i < mainInventory.length; i++) {
                if (mainInventory[i] != null
                        && mainInventory[i].itemID == stack.itemID
                        && mainInventory[i].getItemDamage() == stack.getItemDamage()
                        && mainInventory[i].stackSize < mainInventory[i].getMaxStackSize()
                        && ItemStack.areItemStackTagsEqual(mainInventory[i], stack)) {
                    int space = mainInventory[i].getMaxStackSize() - mainInventory[i].stackSize;
                    int transfer = Math.min(stack.stackSize, space);
                    mainInventory[i].stackSize += transfer;
                    stack.stackSize -= transfer;
                    if (stack.stackSize <= 0) {
                        return true;
                    }
                }
            }
        }

        // Second pass: find empty slot
        for (int i = 0; i < mainInventory.length; i++) {
            if (mainInventory[i] == null) {
                mainInventory[i] = stack.copy();
                mainInventory[i].stackSize = stack.stackSize;
                stack.stackSize = 0;
                return true;
            }
        }

        return false;
    }
    public boolean consumeInventoryItem(int itemId) {
        for (int i = 0; i < mainInventory.length; i++) {
            if (mainInventory[i] != null && mainInventory[i].itemID == itemId) {
                --mainInventory[i].stackSize;
                if (mainInventory[i].stackSize <= 0) {
                    mainInventory[i] = null;
                }
                return true;
            }
        }
        return false;
    }
    public boolean hasItem(int itemId) {
        for (int i = 0; i < mainInventory.length; i++) {
            if (mainInventory[i] != null && mainInventory[i].itemID == itemId) {
                return true;
            }
        }
        for (int i = 0; i < armorInventory.length; i++) {
            if (armorInventory[i] != null && armorInventory[i].itemID == itemId) {
                return true;
            }
        }
        return false;
    }
    public ItemStack getItemStack() { return itemStack; }
    public void setItemStack(ItemStack stack) { this.itemStack = stack; }
    public int clearInventory(int itemId, int metadata) {
        int count = 0;
        for (int i = 0; i < mainInventory.length; i++) {
            if (mainInventory[i] != null
                    && (itemId <= -1 || mainInventory[i].itemID == itemId)
                    && (metadata <= -1 || mainInventory[i].getItemDamage() == metadata)) {
                count += mainInventory[i].stackSize;
                mainInventory[i] = null;
            }
        }
        for (int i = 0; i < armorInventory.length; i++) {
            if (armorInventory[i] != null
                    && (itemId <= -1 || armorInventory[i].itemID == itemId)
                    && (metadata <= -1 || armorInventory[i].getItemDamage() == metadata)) {
                count += armorInventory[i].stackSize;
                armorInventory[i] = null;
            }
        }
        return count;
    }
    public void dropAllItems() {
        for (int i = 0; i < mainInventory.length; i++) {
            if (mainInventory[i] != null) {
                player.dropPlayerItem(mainInventory[i]);
                mainInventory[i] = null;
            }
        }
        for (int i = 0; i < armorInventory.length; i++) {
            if (armorInventory[i] != null) {
                player.dropPlayerItem(armorInventory[i]);
                armorInventory[i] = null;
            }
        }
    }
    public int getTotalArmorValue() {
        int total = 0;
        for (int i = 0; i < armorInventory.length; i++) {
            if (armorInventory[i] != null && armorInventory[i].getItem() instanceof ItemArmor) {
                total += ((ItemArmor) armorInventory[i].getItem()).damageReduceAmount;
            }
        }
        return total;
    }
    public void damageArmor(int damage) {
        damage /= 4;
        if (damage < 1) {
            damage = 1;
        }
        for (int i = 0; i < armorInventory.length; i++) {
            if (armorInventory[i] != null && armorInventory[i].getItem() instanceof ItemArmor) {
                armorInventory[i].damageItem(damage, player);
                if (armorInventory[i].stackSize == 0) {
                    armorInventory[i] = null;
                }
            }
        }
    }

    public ItemStack armorItemInSlot(int slot) {
        return armorInventory[slot];
    }

    /**
     * FC's tool speed calculation. Gets the held item's strength vs the block.
     */
    public float getStrVsBlock(Block block) {
        float str = 1.0F;
        if (this.currentItem >= 0 && this.currentItem < this.mainInventory.length
                && this.mainInventory[this.currentItem] != null) {
            str = this.mainInventory[this.currentItem].getStrVsBlock(block);
        }
        return str;
    }

    /**
     * FC's position-aware tool speed calculation.
     */
    public float getStrVsBlock(World world, Block block, int i, int j, int k) {
        float str = 1.0F;
        if (this.currentItem >= 0 && this.currentItem < this.mainInventory.length
                && this.mainInventory[this.currentItem] != null) {
            str = this.mainInventory[this.currentItem].getStrVsBlock(world, block, i, j, k);
        }
        return str;
    }

    /**
     * FC's harvest check. Can the current tool harvest this block?
     */
    public boolean canHarvestBlock(World world, Block block, int i, int j, int k) {
        if (this.currentItem >= 0 && this.currentItem < this.mainInventory.length
                && this.mainInventory[this.currentItem] != null) {
            return this.mainInventory[this.currentItem].canHarvestBlock(world, block, i, j, k);
        }
        return false;
    }
}
