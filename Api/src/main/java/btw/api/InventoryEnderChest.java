package btw.api;

public class InventoryEnderChest extends InventoryBasic {
    public InventoryEnderChest() {
        super("container.enderchest", false, 27);
    }

    public void setAssociatedChest(TileEntityEnderChest chest) {}
    public NBTTagList saveInventoryToNBT() { return null; }
    public void loadInventoryFromNBT(NBTTagList tagList) {}
}
