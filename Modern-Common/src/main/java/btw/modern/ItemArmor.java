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

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 ItemArmor's icon members (vanilla/client ItemArmor.java:21-22,
    // :46-47, :142-145, :203-251): the cloth overlay drawn in the second pass, and the empty armour-slot icons. FC's
    // armour (FCItemArmor and its subclasses) inherits them; picked-up armour is drawn with armour textures, so
    // nothing FC draws asks for them today.
    private static final String[] CLOTH_OVERLAY_ICON_NAMES = new String[] {"helmetCloth_overlay",
            "chestplateCloth_overlay", "leggingsCloth_overlay", "bootsCloth_overlay"};
    public static final String[] field_94603_a = new String[] {"slot_empty_helmet", "slot_empty_chestplate",
            "slot_empty_leggings", "slot_empty_boots"};
    private Icon clothOverlayIcon;
    private Icon emptySlotIcon;

    // 1.5.2 ItemArmor.getIconFromDamageForRenderPass (vanilla/client ItemArmor.java:142-145); Item's answer while the
    // overlay is not registered (client only; never for non-cloth armour)
    @Override
    public Icon getIconFromDamageForRenderPass(int damage, int renderPass) {
        return renderPass == 1 && this.clothOverlayIcon != null ? this.clothOverlayIcon
                : super.getIconFromDamageForRenderPass(damage, renderPass);
    }

    // 1.5.2 ItemArmor.registerIcons (vanilla/client ItemArmor.java:203-213)
    @Override
    public void registerIcons(IconRegister register) {
        super.registerIcons(register);
        if (this.material == EnumArmorMaterial.CLOTH) {
            this.clothOverlayIcon = register.registerIcon(CLOTH_OVERLAY_ICON_NAMES[this.armorType]);
        }
        this.emptySlotIcon = register.registerIcon(field_94603_a[this.armorType]);
    }

    // 1.5.2 ItemArmor.func_94602_b (vanilla/client ItemArmor.java:232-251): the empty-slot icon for an armour slot,
    // read from the diamond pieces
    public static Icon func_94602_b(int armorType) {
        ItemArmor piece;
        switch (armorType) {
            case 0:
                piece = Item.helmetDiamond;
                break;
            case 1:
                piece = Item.plateDiamond;
                break;
            case 2:
                piece = Item.legsDiamond;
                break;
            case 3:
                piece = Item.bootsDiamond;
                break;
            default:
                return null;
        }
        return piece != null ? piece.emptySlotIcon : null;
    }
}
