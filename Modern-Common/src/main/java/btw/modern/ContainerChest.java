package btw.modern;
public class ContainerChest extends Container {
    private IInventory lowerChestInventory;

    public ContainerChest(IInventory playerInv, IInventory chest) {
        this.lowerChestInventory = chest;
    }

    // 1.5.2 ContainerChest.getLowerChestInventory (vanilla/server ContainerChest.java:107) —
    // TileEntityChest.updateEntity uses it to count viewers for the lid animation.
    public IInventory getLowerChestInventory() {
        return this.lowerChestInventory;
    }

    public boolean canInteractWith(EntityPlayer player) { return true; }
}
