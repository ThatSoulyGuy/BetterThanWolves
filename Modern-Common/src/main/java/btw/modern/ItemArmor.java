package btw.modern;

public class ItemArmor extends Item {

    public final int armorType;
    public int damageReduceAmount;
    public final int renderIndex;
    protected EnumArmorMaterial material;

    public ItemArmor(int id, int renderIndex, int armorType) {
        super(id);
        this.armorType = armorType;
        this.renderIndex = renderIndex;
        this.maxStackSize = 1;
    }

    public ItemArmor(int id, EnumArmorMaterial material, int renderIndex, int armorType) {
        this(id, renderIndex, armorType);
        // Replicate vanilla 1.5.2 ItemArmor: derive protection + durability from
        // the material so FC armor subclasses produce correct values. FC variant
        // subclasses (Wool/Padded/Gimp/Refined) override these on top afterwards.
        this.material = material;
        this.damageReduceAmount = material.getDamageReductionAmount(armorType);
        setMaxDamage(material.getDurability(armorType));
    }

    public boolean hasColor(ItemStack stack) { return false; }
    public int getColor(ItemStack stack) { return 0; }
    public void removeColor(ItemStack stack) {}
    public void func_82813_b(ItemStack stack, int color) {}

    public EnumArmorMaterial getArmorMaterial() { return material; }
}
