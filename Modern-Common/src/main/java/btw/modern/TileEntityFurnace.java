package btw.modern;

public class TileEntityFurnace extends TileEntity implements IInventory {

    public ItemStack[] furnaceItemStacks = new ItemStack[3];
    public int furnaceBurnTime = 0;
    public int currentItemBurnTime = 0;
    public int furnaceCookTime = 0;
    public boolean keepFurnaceInventory = false;
    public static int m_iBaseBurnTimeMultiplier = 2;
    public static int m_iDefaultCookTime = 400; // FC doubles vanilla's 200

    private String customName;

    public boolean isBurning() {
        return this.furnaceBurnTime > 0;
    }

    // --- IInventory ---

    public int getSizeInventory() { return this.furnaceItemStacks.length; }

    public ItemStack getStackInSlot(int slot) {
        return this.furnaceItemStacks[slot];
    }

    public ItemStack decrStackSize(int slot, int amount) {
        if (this.furnaceItemStacks[slot] != null) {
            if (this.furnaceItemStacks[slot].stackSize <= amount) {
                ItemStack stack = this.furnaceItemStacks[slot];
                this.furnaceItemStacks[slot] = null;
                return stack;
            }
            ItemStack split = this.furnaceItemStacks[slot].splitStack(amount);
            if (this.furnaceItemStacks[slot].stackSize == 0) {
                this.furnaceItemStacks[slot] = null;
            }
            return split;
        }
        return null;
    }

    public ItemStack getStackInSlotOnClosing(int slot) {
        if (this.furnaceItemStacks[slot] != null) {
            ItemStack stack = this.furnaceItemStacks[slot];
            this.furnaceItemStacks[slot] = null;
            return stack;
        }
        return null;
    }

    public void setInventorySlotContents(int slot, ItemStack stack) {
        this.furnaceItemStacks[slot] = stack;
        if (stack != null && stack.stackSize > getInventoryStackLimit()) {
            stack.stackSize = getInventoryStackLimit();
        }
    }

    public String getInvName() {
        return customName != null ? customName : "container.furnace";
    }

    public boolean isInvNameLocalized() { return customName != null; }
    public int getInventoryStackLimit() { return 64; }

    public boolean isUseableByPlayer(EntityPlayer player) {
        if (worldObj.getBlockTileEntity(xCoord, yCoord, zCoord) != this) return false;
        return player.getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5) <= 64.0;
    }

    public void openChest() {}
    public void closeChest() {}

    // --- Smelting logic ---

    @Override
    public void updateEntity() {
        boolean wasBurning = this.furnaceBurnTime > 0;
        boolean changed = false;

        if (this.furnaceBurnTime > 0) {
            --this.furnaceBurnTime;
        }

        if (!this.worldObj.isRemote) {
            if (this.furnaceBurnTime == 0 && this.canSmelt()) {
                this.currentItemBurnTime = this.furnaceBurnTime = getItemBurnTime(this.furnaceItemStacks[1]);

                if (this.furnaceBurnTime > 0) {
                    changed = true;
                    if (this.furnaceItemStacks[1] != null) {
                        --this.furnaceItemStacks[1].stackSize;
                        if (this.furnaceItemStacks[1].stackSize == 0) {
                            Item containerItem = this.furnaceItemStacks[1].getItem().getContainerItem();
                            this.furnaceItemStacks[1] = containerItem != null ? new ItemStack(containerItem) : null;
                        }
                    }
                }
            }

            if (this.isBurning() && this.canSmelt()) {
                ++this.furnaceCookTime;
                if (this.furnaceCookTime >= GetCookTimeForCurrentItem()) {
                    this.furnaceCookTime = 0;
                    this.smeltItem();
                    changed = true;
                }
            } else {
                this.furnaceCookTime = 0;
            }

            if (wasBurning != this.furnaceBurnTime > 0) {
                changed = true;
                boolean hasVisibleContents = furnaceItemStacks[0] != null || furnaceItemStacks[2] != null;
                Block block = Block.blocksList[worldObj.getBlockId(xCoord, yCoord, zCoord)];
                if (block instanceof BlockFurnace) {
                    ((BlockFurnace) block).updateFurnaceBlockState(
                            furnaceBurnTime > 0, worldObj, xCoord, yCoord, zCoord, hasVisibleContents);
                }
            }
        }

        if (changed) {
            this.onInventoryChanged();
        }
    }

    public boolean canSmelt() {
        if (this.furnaceItemStacks[0] == null) return false;
        ItemStack result = FurnaceRecipes.smelting().getSmeltingResult(this.furnaceItemStacks[0].getItem().itemID);
        if (result == null) return false;
        if (this.furnaceItemStacks[2] == null) return true;
        if (!this.furnaceItemStacks[2].isItemEqual(result)) return false;
        int totalSize = this.furnaceItemStacks[2].stackSize + result.stackSize;
        return totalSize <= getInventoryStackLimit() && totalSize <= this.furnaceItemStacks[2].getMaxStackSize();
    }

    public void smeltItem() {
        if (this.canSmelt()) {
            ItemStack result = FurnaceRecipes.smelting().getSmeltingResult(this.furnaceItemStacks[0].getItem().itemID);
            if (this.furnaceItemStacks[2] == null) {
                this.furnaceItemStacks[2] = result.copy();
            } else if (this.furnaceItemStacks[2].itemID == result.itemID) {
                this.furnaceItemStacks[2].stackSize += result.stackSize;
            }
            --this.furnaceItemStacks[0].stackSize;
            if (this.furnaceItemStacks[0].stackSize <= 0) {
                this.furnaceItemStacks[0] = null;
            }
        }
    }

    public int getItemBurnTime(ItemStack stack) {
        if (stack != null && stack.getItem() != null) {
            return stack.getItem().GetFurnaceBurnTime(stack.getItemDamage()) * m_iBaseBurnTimeMultiplier;
        }
        return 0;
    }

    public static boolean isItemFuel(ItemStack stack) {
        if (stack != null && stack.getItem() != null) {
            return stack.getItem().GetFurnaceBurnTime(stack.getItemDamage()) > 0;
        }
        return false;
    }

    // 1.5.2 FCMOD TileEntityFurnace.GetCookTimeForCurrentItem (vanilla/client TileEntityFurnace.java:555-566) —
    // per-item cook-time binary shift (iron/gold ore chunks shift 3, clay/nether sludge shift 2, FCRecipes:3027-3033).
    // Live via FCTileEntityFurnaceBrick.updateEntity:69 → its override multiplies this by m_iCookTimeMultiplier.
    public int GetCookTimeForCurrentItem() {
        int iCookTimeShift = 0;

        if (furnaceItemStacks[0] != null) {
            iCookTimeShift = FurnaceRecipes.smelting().GetCookTimeBinaryShift(
                furnaceItemStacks[0].getItem().itemID);
        }

        return m_iDefaultCookTime << iCookTimeShift;
    }

    // --- NBT ---

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        NBTTagList items = tag.getTagList("Items");
        furnaceItemStacks = new ItemStack[3];
        if (items != null) {
            for (int i = 0; i < items.tagCount(); i++) {
                NBTTagCompound itemTag = (NBTTagCompound) items.tagAt(i);
                int slot = itemTag.getByte("Slot") & 255;
                if (slot >= 0 && slot < furnaceItemStacks.length) {
                    furnaceItemStacks[slot] = ItemStack.loadItemStackFromNBT(itemTag);
                }
            }
        }
        furnaceBurnTime = tag.getShort("BurnTime");
        furnaceCookTime = tag.getShort("CookTime");
        currentItemBurnTime = getItemBurnTime(furnaceItemStacks[1]);

        // FC extended burn times
        if (tag.hasKey("fcBurnTimeEx")) {
            furnaceBurnTime = tag.getInteger("fcBurnTimeEx");
            furnaceCookTime = tag.getInteger("fcCookTimeEx");
            if (tag.hasKey("fcItemBurnTimeEx")) {
                currentItemBurnTime = tag.getInteger("fcItemBurnTimeEx");
            }
        }

        if (tag.hasKey("CustomName")) {
            customName = tag.getString("CustomName");
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setShort("BurnTime", (short) furnaceBurnTime);
        tag.setShort("CookTime", (short) furnaceCookTime);
        NBTTagList items = new NBTTagList();
        for (int i = 0; i < furnaceItemStacks.length; i++) {
            if (furnaceItemStacks[i] != null) {
                NBTTagCompound itemTag = new NBTTagCompound();
                itemTag.setByte("Slot", (byte) i);
                furnaceItemStacks[i].writeToNBT(itemTag);
                items.appendTag(itemTag);
            }
        }
        tag.setTag("Items", items);

        // FC extended burn times
        tag.setInteger("fcBurnTimeEx", furnaceBurnTime);
        tag.setInteger("fcCookTimeEx", furnaceCookTime);
        tag.setInteger("fcItemBurnTimeEx", currentItemBurnTime);

        if (customName != null) {
            tag.setString("CustomName", customName);
        }
    }
}
