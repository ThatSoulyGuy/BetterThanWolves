package btw.modern;

/**
 * 1.5.2 TileEntityBeacon with 4.B's FCMOD accessors (vanilla/server TileEntityBeacon.java:19-392). FC's beacon
 * (Common FCTileEntityBeacon) overrides updateEntity and keeps its whole state here: the power flag, the pyramid level
 * and the effect id, saved as 1.5.2 saved them ("Primary", "Secondary", "Levels"). The payment slot stays for the
 * IInventory contract (FCBlockBeacon.breakBlock ejects it); 4.B never opens the beacon menu.
 */
public class TileEntityBeacon extends TileEntity implements IInventory {
    private boolean isBeaconActive;
    private int levels = -1;                 // 1.5.2 default (:26): -1 until the first power update
    private int primaryEffect;
    private int secondaryEffect;
    private ItemStack payment;
    private String customName;

    public int getLevels() { return levels; }
    public int getPrimaryEffect() { return primaryEffect; }
    public int getSecondaryEffect() { return secondaryEffect; }
    public void setLevels(int l) { levels = l; }                     // client half, vanilla/client :200-207

    // FCMOD (vanilla/server :366-391)
    public boolean IsOn() { return isBeaconActive; }
    public void SetIsOn(boolean on) { isBeaconActive = on; }
    public void SetPrimaryEffect(int effect) { primaryEffect = effect; }
    public void setLevelsServerSafe(int l) { levels = l; }
    public boolean IsHomeBeacon() { return false; }

    @Override public Packet getDescriptionPacket() {                // :215-220
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);
        return new Packet132TileEntityData(xCoord, yCoord, zCoord, 3, tag);
    }
    @Override public void readFromNBT(NBTTagCompound tag) {         // :225-231
        super.readFromNBT(tag);
        primaryEffect = tag.getInteger("Primary");
        secondaryEffect = tag.getInteger("Secondary");
        levels = tag.getInteger("Levels");
    }
    @Override public void writeToNBT(NBTTagCompound tag) {          // :236-242
        super.writeToNBT(tag);
        tag.setInteger("Primary", primaryEffect);
        tag.setInteger("Secondary", secondaryEffect);
        tag.setInteger("Levels", levels);
    }

    // IInventory: the payment slot (:247-364), verbatim semantics
    @Override public int getSizeInventory() { return 1; }
    @Override public ItemStack getStackInSlot(int slot) { return slot == 0 ? payment : null; }
    @Override public ItemStack decrStackSize(int slot, int amount) {
        if (slot != 0 || payment == null) return null;
        if (amount >= payment.stackSize) { ItemStack s = payment; payment = null; return s; }
        payment.stackSize -= amount;
        return new ItemStack(payment.itemID, amount, payment.getItemDamage());
    }
    @Override public ItemStack getStackInSlotOnClosing(int slot) {
        if (slot != 0 || payment == null) return null;
        ItemStack s = payment; payment = null; return s;
    }
    @Override public void setInventorySlotContents(int slot, ItemStack stack) { if (slot == 0) payment = stack; }
    @Override public String getInvName() { return isInvNameLocalized() ? customName : "container.beacon"; }
    @Override public boolean isInvNameLocalized() { return customName != null && customName.length() > 0; }
    public void func_94047_a(String name) { customName = name; }
    @Override public int getInventoryStackLimit() { return 1; }
    @Override public boolean isUseableByPlayer(EntityPlayer p) {
        return worldObj != null && worldObj.getBlockTileEntity(xCoord, yCoord, zCoord) == this
                && p.getDistanceSq(xCoord + 0.5D, yCoord + 0.5D, zCoord + 0.5D) <= 64.0D;
    }
    @Override public void openChest() {}
    @Override public void closeChest() {}
    @Override public boolean isStackValidForSlot(int slot, ItemStack s) { return isItemValidForSlot(slot, s); }
    @Override public boolean isItemValidForSlot(int slot, ItemStack s) {   // :361-364
        return s != null && (s.itemID == Item.emerald.itemID || s.itemID == Item.diamond.itemID
                || s.itemID == Item.ingotGold.itemID || s.itemID == Item.ingotIron.itemID);
    }
}
