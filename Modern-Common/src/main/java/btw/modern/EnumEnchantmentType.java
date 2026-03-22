package btw.modern;

public enum EnumEnchantmentType {
    all,
    armor,
    armor_feet,
    armor_legs,
    armor_torso,
    armor_head,
    weapon,
    digger,
    fishing_rod,
    breakable,
    bow;

    public boolean canEnchantItem(Item item) {
        if (item == null) return false;
        return switch (this) {
            case all -> true;
            case breakable -> item.isDamageable();
            case armor -> item instanceof ItemArmor;
            case armor_head -> item instanceof ItemArmor a && a.armorType == 0;
            case armor_torso -> item instanceof ItemArmor a && a.armorType == 1;
            case armor_legs -> item instanceof ItemArmor a && a.armorType == 2;
            case armor_feet -> item instanceof ItemArmor a && a.armorType == 3;
            case weapon -> item instanceof ItemSword;
            case digger -> item instanceof ItemTool;
            case bow -> item instanceof ItemBow;
            case fishing_rod -> item instanceof ItemFishingRod;
        };
    }
}
