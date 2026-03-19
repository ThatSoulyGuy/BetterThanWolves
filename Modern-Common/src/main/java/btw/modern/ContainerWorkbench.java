package btw.modern;

public class ContainerWorkbench extends Container {

    public InventoryCrafting craftMatrix;
    public IInventory craftResult;

    public ContainerWorkbench() {}
    public ContainerWorkbench(InventoryPlayer inventory, World world, int x, int y, int z) {}

    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }
}
