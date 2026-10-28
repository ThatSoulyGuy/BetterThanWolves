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
        // vanilla/client ItemArmor.java:58 (1.5.2 has only the material constructor, which chains through here). FC
        // armour (FCItemArmor and its subclasses) sets no tab of its own and is listed under Combat through this one.
        this.setCreativeTab(CreativeTabs.tabCombat);
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

    // 1.5.2 ItemArmor.hasColor (vanilla/client ItemArmor.java:109-112): only cloth armour carries a dye colour, in
    // the display.color NBT tag. FCItemArmorMod overrides all four colour methods for FC's own armour.
    public boolean hasColor(ItemStack stack) {
        return this.material != EnumArmorMaterial.CLOTH ? false
            : (!stack.hasTagCompound() ? false
                : (!stack.getTagCompound().hasKey("display") ? false
                    : stack.getTagCompound().getCompoundTag("display").hasKey("color")));
    }

    // 1.5.2 ItemArmor.getColor (vanilla/client ItemArmor.java:117-137): -1 (untinted) unless cloth; undyed cloth is
    // the default leather brown 10511680. RenderBiped's CLOTH pass feeds this into glColor3f, so the old stub's 0
    // rendered undyed leather armour black.
    public int getColor(ItemStack stack) {
        if (this.material != EnumArmorMaterial.CLOTH) {
            return -1;
        } else {
            NBTTagCompound tag = stack.getTagCompound();

            if (tag == null) {
                return 10511680;
            } else {
                NBTTagCompound display = tag.getCompoundTag("display");
                return display == null ? 10511680 : (display.hasKey("color") ? display.getInteger("color") : 10511680);
            }
        }
    }

    // 1.5.2 ItemArmor.removeColor (vanilla/client ItemArmor.java:150-166)
    public void removeColor(ItemStack stack) {
        if (this.material == EnumArmorMaterial.CLOTH) {
            NBTTagCompound tag = stack.getTagCompound();

            if (tag != null) {
                NBTTagCompound display = tag.getCompoundTag("display");

                if (display.hasKey("color")) {
                    display.removeTag("color");
                }
            }
        }
    }

    // 1.5.2 ItemArmor.func_82813_b (vanilla/client ItemArmor.java:168-193): dyes cloth armour; any other material
    // throws, as in 1.5.2 (its only caller, FCRecipesArmorWool, dyes FCItemArmorWool, whose FCItemArmorMod override
    // runs instead).
    public void func_82813_b(ItemStack stack, int color) {
        if (this.material != EnumArmorMaterial.CLOTH) {
            throw new UnsupportedOperationException("Can't dye non-leather!");
        } else {
            NBTTagCompound tag = stack.getTagCompound();

            if (tag == null) {
                tag = new NBTTagCompound();
                stack.setTagCompound(tag);
            }

            NBTTagCompound display = tag.getCompoundTag("display");

            if (!tag.hasKey("display")) {
                tag.setCompoundTag("display", display);
            }

            display.setInteger("color", color);
        }
    }

    public EnumArmorMaterial getArmorMaterial() { return material; }

    @Override
    public int getItemEnchantability() {
        return material != null ? material.getEnchantability() : 0;
    }

    @Override
    public boolean IsEnchantmentApplicable(Enchantment enchantment) {
        if (enchantment.type == EnumEnchantmentType.armor) return true;
        if (enchantment.type == EnumEnchantmentType.armor_head) return armorType == 0;
        if (enchantment.type == EnumEnchantmentType.armor_torso) return armorType == 1;
        if (enchantment.type == EnumEnchantmentType.armor_legs) return armorType == 2;
        if (enchantment.type == EnumEnchantmentType.armor_feet) return armorType == 3;
        return super.IsEnchantmentApplicable(enchantment);
    }
}
