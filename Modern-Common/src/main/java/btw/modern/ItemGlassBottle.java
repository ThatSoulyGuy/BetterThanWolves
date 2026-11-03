package btw.modern;

public class ItemGlassBottle extends Item {

    public ItemGlassBottle(int id) {
        super(id);
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 ItemGlassBottle's icon members (vanilla/client
    // ItemGlassBottle.java:28-31, :82): the drinkable potion's bottle, and no icon of its own. FCItemGlassBottle (374)
    // inherits both. (1.20.1's item/potion.png and item/glass_bottle.png are the same picture.)
    @Override
    public Icon getIconFromDamage(int damage) {
        Icon icon = Item.potion != null ? Item.potion.getIconFromDamage(0) : null;
        return icon != null ? icon : super.getIconFromDamage(damage);
    }

    @Override
    public void registerIcons(IconRegister register) {}
}
