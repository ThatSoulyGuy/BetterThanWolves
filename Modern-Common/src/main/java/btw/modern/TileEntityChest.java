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
    // 1.5.2 TileEntityChest.ticksSinceSync — staggers the numUsingPlayers resync in updateEntity.
    private int ticksSinceSync;

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

    // 1.5.2 TileEntityChest.updateEntity — lid angle interpolation + open/close sounds; the fc
    // TileEntityChestRenderer reads lidAngle/prevLidAngle. Ticked via ProxyBlockEntity → fcTileEntity.updateEntity().
    @Override
    public void updateEntity() {
        super.updateEntity();
        this.checkForAdjacentChests();
        ++this.ticksSinceSync;
        float var1;

        if (!this.worldObj.isRemote && this.numUsingPlayers != 0
                && (this.ticksSinceSync + this.xCoord + this.yCoord + this.zCoord) % 200 == 0) {
            this.numUsingPlayers = 0;
            var1 = 5.0F;
            java.util.List var2 = this.worldObj.getEntitiesWithinAABB(EntityPlayer.class,
                    AxisAlignedBB.getAABBPool().getAABB((double) ((float) this.xCoord - var1),
                            (double) ((float) this.yCoord - var1), (double) ((float) this.zCoord - var1),
                            (double) ((float) (this.xCoord + 1) + var1), (double) ((float) (this.yCoord + 1) + var1),
                            (double) ((float) (this.zCoord + 1) + var1)));
            java.util.Iterator var3 = var2.iterator();

            while (var3.hasNext()) {
                EntityPlayer var4 = (EntityPlayer) var3.next();

                if (var4.openContainer instanceof ContainerChest) {
                    IInventory var5 = ((ContainerChest) var4.openContainer).getLowerChestInventory();

                    if (var5 == this || var5 instanceof InventoryLargeChest
                            && ((InventoryLargeChest) var5).isPartOfLargeChest(this)) {
                        ++this.numUsingPlayers;
                    }
                }
            }
        }

        this.prevLidAngle = this.lidAngle;
        var1 = 0.1F;
        double var11;

        if (this.numUsingPlayers > 0 && this.lidAngle == 0.0F
                && this.adjacentChestZNeg == null && this.adjacentChestXNeg == null) {
            double var8 = (double) this.xCoord + 0.5D;
            var11 = (double) this.zCoord + 0.5D;

            if (this.adjacentChestZPosition != null) {
                var11 += 0.5D;
            }

            if (this.adjacentChestXPos != null) {
                var8 += 0.5D;
            }

            this.worldObj.playSoundEffect(var8, (double) this.yCoord + 0.5D, var11,
                    "random.chestopen", 0.5F, this.worldObj.rand.nextFloat() * 0.1F + 0.9F);
        }

        if (this.numUsingPlayers == 0 && this.lidAngle > 0.0F || this.numUsingPlayers > 0 && this.lidAngle < 1.0F) {
            float var9 = this.lidAngle;

            if (this.numUsingPlayers > 0) {
                this.lidAngle += var1;
            } else {
                this.lidAngle -= var1;
            }

            if (this.lidAngle > 1.0F) {
                this.lidAngle = 1.0F;
            }

            float var10 = 0.5F;

            if (this.lidAngle < var10 && var9 >= var10
                    && this.adjacentChestZNeg == null && this.adjacentChestXNeg == null) {
                var11 = (double) this.xCoord + 0.5D;
                double var6 = (double) this.zCoord + 0.5D;

                if (this.adjacentChestZPosition != null) {
                    var6 += 0.5D;
                }

                if (this.adjacentChestXPos != null) {
                    var11 += 0.5D;
                }

                this.worldObj.playSoundEffect(var11, (double) this.yCoord + 0.5D, var6,
                        "random.chestclosed", 0.5F, this.worldObj.rand.nextFloat() * 0.1F + 0.9F);
            }

            if (this.lidAngle < 0.0F) {
                this.lidAngle = 0.0F;
            }
        }
    }

    // 1.5.2 TileEntityChest.receiveClientEvent — event 1 syncs numUsingPlayers to clients
    // (sent by openChest/closeChest via World.addBlockEvent).
    @Override
    public boolean receiveClientEvent(int eventId, int eventParam) {
        if (eventId == 1) {
            this.numUsingPlayers = eventParam;
            return true;
        } else {
            return super.receiveClientEvent(eventId, eventParam);
        }
    }

    // 1.5.2 TileEntityChest.openChest — called by FCTileEntityChest.openChest via super.
    public void openChest() {
        if (this.numUsingPlayers < 0) {
            this.numUsingPlayers = 0;
        }

        ++this.numUsingPlayers;
        this.worldObj.addBlockEvent(this.xCoord, this.yCoord, this.zCoord, this.getBlockType().blockID, 1, this.numUsingPlayers);
        this.worldObj.notifyBlocksOfNeighborChange(this.xCoord, this.yCoord, this.zCoord, this.getBlockType().blockID);
        this.worldObj.notifyBlocksOfNeighborChange(this.xCoord, this.yCoord - 1, this.zCoord, this.getBlockType().blockID);
    }

    // 1.5.2 TileEntityChest.closeChest — called by FCTileEntityChest.closeChest via super.
    public void closeChest() {
        if (this.getBlockType() != null && this.getBlockType() instanceof BlockChest) {
            --this.numUsingPlayers;
            this.worldObj.addBlockEvent(this.xCoord, this.yCoord, this.zCoord, this.getBlockType().blockID, 1, this.numUsingPlayers);
            this.worldObj.notifyBlocksOfNeighborChange(this.xCoord, this.yCoord, this.zCoord, this.getBlockType().blockID);
            this.worldObj.notifyBlocksOfNeighborChange(this.xCoord, this.yCoord - 1, this.zCoord, this.getBlockType().blockID);
        }
    }

    // Adjacency detection is supplied by FCTileEntityChest.checkForAdjacentChests (FCTileEntityChest.java:42),
    // which overrides this for every chest the proxy block creates.
    public void checkForAdjacentChests() {}

    public int func_98041_l() { return 0; }

    // 1.5.2 TileEntityChest.updateContainingBlockInfo — clears cached adjacency; the base
    // TileEntity part (blockType/blockMetadata cache reset) is inlined since the shim base lacks the method.
    public void updateContainingBlockInfo() {
        this.blockType = null;
        this.blockMetadata = -1;
        this.adjacentChestChecked = false;
    }

    // 1.5.2 TileEntityChest.invalidate — refreshes cached block info and adjacency on removal.
    @Override
    public void invalidate() {
        super.invalidate();
        this.updateContainingBlockInfo();
        this.checkForAdjacentChests();
    }

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
