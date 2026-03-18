package btw.api;

public class BehaviorDefaultDispenseItem implements IBehaviorDispenseItem {
    public final ItemStack dispense(IBlockSource source, ItemStack stack) { return dispenseStack(source, stack); }
    public ItemStack dispenseStack(IBlockSource source, ItemStack stack) { return stack; }
    public void playDispenseSound(IBlockSource source) {}
    public int func_82488_a(IBlockSource source) { return 0; }
}
