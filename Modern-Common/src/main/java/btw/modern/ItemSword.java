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

    // 1.5.2 ItemSword.func_82803_g (vanilla ItemSword.java:33) — raw weapon damage, used by
    // EntityLiving.onLivingUpdate's canPickUpLoot better-weapon comparison.
    public int func_82803_g() {
        return this.toolMaterial.getDamageVsEntity();
    }
}
