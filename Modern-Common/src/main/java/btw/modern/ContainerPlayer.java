package btw.modern;

public class ContainerPlayer extends Container {

    public ContainerPlayer() {}
    public ContainerPlayer(InventoryPlayer inventory, boolean notRemote, EntityPlayer player) {}

    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }
}
