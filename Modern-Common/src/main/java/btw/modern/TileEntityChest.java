package btw.modern;

public class TileEntityChest extends TileEntity implements IInventory {

    private ItemStack[] chestContents = new ItemStack[27];
    public boolean adjacentChestChecked = false;
    public TileEntityChest adjacentChestZNeg;
    public TileEntityChest adjacentChestXPos;
    public TileEntityChest adjacentChestXNeg;
    public TileEntityChest adjacentChestZPosition;
    public int numUsingPlayers;
    public float lidAngle;
    public float prevLidAngle;

    @Override
    public int getSizeInventory() { return 27; }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return (slot >= 0 && slot < chestContents.length) ? chestContents[slot] : null;
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        if (chestContents[slot] != null) {
            if (chestContents[slot].stackSize <= amount) {
                ItemStack stack = chestContents[slot];
                chestContents[slot] = null;
                onInventoryChanged();
                return stack;
            }
            ItemStack split = chestContents[slot].splitStack(amount);
            if (chestContents[slot].stackSize == 0) {
                chestContents[slot] = null;
            }
            onInventoryChanged();
            return split;
        }
        return null;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        if (chestContents[slot] != null) {
            ItemStack stack = chestContents[slot];
            chestContents[slot] = null;
            return stack;
        }
        return null;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        chestContents[slot] = stack;
        if (stack != null && stack.stackSize > getInventoryStackLimit()) {
            stack.stackSize = getInventoryStackLimit();
        }
        onInventoryChanged();
    }

    @Override
    public String getInvName() { return "container.chest"; }

    @Override
    public boolean isInvNameLocalized() { return false; }

    @Override
    public int getInventoryStackLimit() { return 64; }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        if (worldObj.getBlockTileEntity(xCoord, yCoord, zCoord) != this) return false;
        return player.getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5) <= 64.0;
    }

    public void openChest() { numUsingPlayers++; }
    public void closeChest() { numUsingPlayers--; }
    public void checkForAdjacentChests() {}
    public int func_98041_l() { return 0; }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        NBTTagList items = nbt.getTagList("Items");
        chestContents = new ItemStack[getSizeInventory()];
        if (items != null) {
            for (int i = 0; i < items.tagCount(); i++) {
                NBTTagCompound itemTag = (NBTTagCompound) items.tagAt(i);
                int slot = itemTag.getByte("Slot") & 255;
                if (slot >= 0 && slot < chestContents.length) {
                    chestContents[slot] = ItemStack.loadItemStackFromNBT(itemTag);
                }
            }
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        NBTTagList items = new NBTTagList();
        for (int i = 0; i < chestContents.length; i++) {
            if (chestContents[i] != null) {
                NBTTagCompound itemTag = new NBTTagCompound();
                itemTag.setByte("Slot", (byte) i);
                chestContents[i].writeToNBT(itemTag);
                items.appendTag(itemTag);
            }
        }
        nbt.setTag("Items", items);
    }
}
