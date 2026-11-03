package btw.modern;

public class ItemDye extends Item {

    public ItemDye(int id) {
        super(id);
        // FCMOD-INTEGRATION(1.20.1): bone_meal: 1.5.2 ItemDye (vanilla/server ItemDye.java:23-29) has subtypes and no
        // durability; FCItemDye (BTWLifecycle.java:1500) inherits them. FC's merge rule (FCUtilsInventory
        // .CanStacksMerge:405-413) and the merchant slot rule (MerchantRecipeList) compare damage only for subtyped
        // items: without them an ink sac (dye 0) merged into any dye slot and took its colour, and the 1.20.1 dyes,
        // FC (351, 256 / 259 / 260 / 271) (Forge LegacyVariants.MODERN_SPELLING), would merge into an ink sac, cocoa,
        // lapis or bone-meal slot, now that ItemStack.isItemDamaged is at 1.5.2 parity for subtyped items
        // (vanilla/server ItemStack.java:239-242).
        setHasSubtypes(true);
        setMaxDamage(0);
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 ItemDye's icon members (vanilla/client ItemDye.java:23-25,
    // :38-42, :349-357): dyePowder_<colour> by damage. FCItemDye (351) reads both through super (cocoa beans, damage
    // 3, are its own); a held dye drew Item's default, the unlocalized "dyePowder", which names no texture.
    public static final String[] field_94595_b = new String[] {"dyePowder_black", "dyePowder_red", "dyePowder_green",
            "dyePowder_brown", "dyePowder_blue", "dyePowder_purple", "dyePowder_cyan", "dyePowder_silver",
            "dyePowder_gray", "dyePowder_pink", "dyePowder_lime", "dyePowder_yellow", "dyePowder_lightBlue",
            "dyePowder_magenta", "dyePowder_orange", "dyePowder_white"};
    private Icon[] dyeIcons;

    // 1.5.2 ItemDye.getIconFromDamage (vanilla/client ItemDye.java:38-42); Item's icon until registerIcons runs
    // (client only)
    @Override
    public Icon getIconFromDamage(int damage) {
        if (this.dyeIcons == null) {
            return super.getIconFromDamage(damage);
        }
        return this.dyeIcons[MathHelper.clamp_int(damage, 0, 15)];
    }

    // 1.5.2 ItemDye.registerIcons (vanilla/client ItemDye.java:349-357)
    @Override
    public void registerIcons(IconRegister register) {
        Icon[] icons = new Icon[field_94595_b.length];
        for (int i = 0; i < icons.length; ++i) {
            icons[i] = register.registerIcon(field_94595_b[i]);
        }
        this.dyeIcons = icons;
    }
}
