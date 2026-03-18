package btw.api;

public class ContainerWorkbench extends Container {

    public InventoryCrafting craftMatrix;
    public InventoryCraftResult craftResult;

    public ContainerWorkbench() {}
    public ContainerWorkbench(InventoryPlayer inventory, World world, int x, int y, int z) {}

    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }
}
