package btw.api;

public class SlotCrafting extends Slot {
    private final IInventory craftMatrix;
    private EntityPlayer thePlayer;
    private int amountCrafted;

    // Second parameter MUST be IInventory to match the real 1.5.2 vanilla
    // signature — FC bytecode compiled against this stub emits an invokespecial
    // with exactly this descriptor, and the btw.modern class that wins the
    // shadow-merge at runtime only declares (EntityPlayer, IInventory, ...).
    public SlotCrafting(EntityPlayer player, IInventory craftMatrix, IInventory craftResult, int index, int x, int y) {
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
        if (resultStack != null) {
            resultStack.onCrafting(player.worldObj, player, this.amountCrafted);
        }
        this.amountCrafted = 0;

        for (int i = 0; i < this.craftMatrix.getSizeInventory(); i++) {
            ItemStack ingredient = this.craftMatrix.getStackInSlot(i);
            if (ingredient == null) continue;

            Item item = ingredient.getItem();
            if (item == null) {
                this.craftMatrix.decrStackSize(i, 1);
                continue;
            }

            item.OnUsedInCrafting(ingredient.getItemDamage(), player, resultStack);

            if (!item.IsConsumedInCrafting()) continue;

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

            this.craftMatrix.decrStackSize(i, 1);
        }
    }
}
