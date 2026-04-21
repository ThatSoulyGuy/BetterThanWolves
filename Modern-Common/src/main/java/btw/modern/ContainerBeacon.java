package btw.modern;
public class ContainerBeacon extends Container {
    public ContainerBeacon(InventoryPlayer playerInv, IInventory beacon) {}
    public boolean canInteractWith(EntityPlayer player) { return true; }
}
