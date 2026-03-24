package btw.modern;

/**
 * Crafting result slot. When the result is picked up, ingredients
 * in the crafting matrix are consumed.
 *
 * <p>FC's patched version adds tool damage-in-crafting and
 * non-consumed-item logic. These are delegated to the FC Item
 * methods (IsConsumedInCrafting, IsDamagedInCrafting, etc.)
 * which FC subclasses override.</p>
 */
public class SlotCrafting extends Slot {
    private final IInventory craftMatrix;
    private EntityPlayer thePlayer;
    private int amountCrafted;

    public SlotCrafting(EntityPlayer player, InventoryCrafting craftMatrix, IInventory craftResult, int index, int x, int y) {
        super(craftResult, index, x, y);
        this.thePlayer = player;
        this.craftMatrix = craftMatrix;
    }

    @Override
    public boolean isItemValid(ItemStack stack) { return false; }

    @Override
    public ItemStack decrStackSize(int amount) {
        if (this.getHasStack()) {
            this.amountCrafted += Math.min(amount, this.getStack().stackSize);
        }
        return super.decrStackSize(amount);
    }

    @Override
    public void onPickupFromSlot(EntityPlayer player, ItemStack resultStack) {
        // Fire crafting callbacks
        if (resultStack != null) {
            resultStack.onCrafting(player.worldObj, player, this.amountCrafted);
        }
        this.amountCrafted = 0;

        // Consume ingredients from the crafting matrix
        for (int i = 0; i < this.craftMatrix.getSizeInventory(); i++) {
            ItemStack ingredient = this.craftMatrix.getStackInSlot(i);
            if (ingredient == null) continue;

            Item item = ingredient.getItem();
            if (item == null) {
                // No FC Item entry — just consume
                this.craftMatrix.decrStackSize(i, 1);
                continue;
            }

            // FC hooks: notify item it was used in crafting
            item.OnUsedInCrafting(ingredient.getItemDamage(), player, resultStack);

            // FC: some items are not consumed (e.g., tools used as crafting aids)
            if (!item.IsConsumedInCrafting()) {
                continue;
            }

            // FC: some items are damaged instead of consumed
            if (item.IsDamagedInCrafting()) {
                if (ingredient.getItemDamage() >= ingredient.getMaxDamage() - 1) {
                    item.OnBrokenInCrafting(player);
                    this.craftMatrix.decrStackSize(i, 1);
                } else {
                    item.OnDamagedInCrafting(player);
                    ingredient.damageItem(1, player);
                }
                continue;
            }

            // Default: consume 1 from the stack
            this.craftMatrix.decrStackSize(i, 1);
        }
    }
}
