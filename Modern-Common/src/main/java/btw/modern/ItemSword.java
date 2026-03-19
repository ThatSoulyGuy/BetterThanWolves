package btw.modern;

public class ItemSword extends Item {

    private int weaponDamage;
    private final EnumToolMaterial toolMaterial;

    public ItemSword(int id, EnumToolMaterial material) {
        super(id);
        this.toolMaterial = material;
        this.maxStackSize = 1;
        this.weaponDamage = 4 + material.getDamageVsEntity();
    }

    public int getWeaponDamage() {
        return this.weaponDamage;
    }

    public EnumToolMaterial getToolMaterial() {
        return this.toolMaterial;
    }
}
