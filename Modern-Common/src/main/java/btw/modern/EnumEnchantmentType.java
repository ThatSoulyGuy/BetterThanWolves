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
        return true;
    }
}
