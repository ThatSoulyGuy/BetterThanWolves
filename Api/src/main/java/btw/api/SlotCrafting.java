package btw.api;

public class SlotCrafting extends Slot {
    private final IInventory craftMatrix;
    private EntityPlayer thePlayer;
    private int amountCrafted;

    public SlotCrafting(EntityPlayer player, InventoryCrafting craftMatrix, IInventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
        this.thePlayer = player;
        this.craftMatrix = craftMatrix;
    }

    public boolean isItemValid(ItemStack stack) { return false; }
}
