package btw.api;

public interface IBehaviorDispenseItem {
    IBehaviorDispenseItem itemDispenseBehaviorProvider = null;
    ItemStack dispense(IBlockSource source, ItemStack stack);
}
