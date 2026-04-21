package btw.modern;
public class ContainerChest extends Container {
    public ContainerChest(IInventory playerInv, IInventory chest) {}
    public boolean canInteractWith(EntityPlayer player) { return true; }
}
