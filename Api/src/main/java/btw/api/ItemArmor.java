package btw.api;

public class ItemArmor extends Item {

    public final int armorType;
    public int damageReduceAmount;
    public final int renderIndex;

    public ItemArmor(int id, int renderIndex, int armorType) {
        super(id);
        this.armorType = armorType;
        this.renderIndex = renderIndex;
        this.maxStackSize = 1;
    }

    public ItemArmor(int id, EnumArmorMaterial material, int renderIndex, int armorType) {
        this(id, renderIndex, armorType);
    }

    public boolean hasColor(ItemStack stack) { return false; }
    public int getColor(ItemStack stack) { return 0; }
    public void removeColor(ItemStack stack) {}
    public void func_82813_b(ItemStack stack, int color) {}

    public EnumArmorMaterial getArmorMaterial() { return null; }
}
