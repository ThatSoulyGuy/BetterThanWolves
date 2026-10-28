package btw.modern;

public class ItemSword extends Item {

    private int weaponDamage;
    private final EnumToolMaterial toolMaterial;

    public ItemSword(int id, EnumToolMaterial material) {
        super(id);
        this.toolMaterial = material;
        this.maxStackSize = 1;
        this.setMaxDamage(material.getMaxUses());
        this.setCreativeTab(CreativeTabs.tabCombat);
        this.weaponDamage = 4 + material.getDamageVsEntity();
    }

    @Override
    public boolean hitEntity(ItemStack stack, EntityLiving target, EntityLiving attacker) {
        stack.damageItem(1, attacker);
        return true;
    }

    @Override
    public boolean onBlockDestroyed(ItemStack stack, World world, int blockID, int x, int y, int z, EntityLiving entity) {
        Block block = Block.blocksList[blockID];
        if (block != null && block.getBlockHardness(world, x, y, z) != 0.0F) {
            stack.damageItem(2, entity);
        }
        return true;
    }

    @Override
    public int getItemEnchantability() {
        return this.toolMaterial.getEnchantability();
    }

    public int getWeaponDamage() {
        return this.weaponDamage;
    }

    @Override
    public int getDamageVsEntity(Entity entity) {
        return this.weaponDamage;
    }

    @Override
    public boolean isFull3D() {
        return true;
    }

    public EnumToolMaterial getToolMaterial() {
        return this.toolMaterial;
    }

    public int func_82803_g() {
        return this.toolMaterial.getDamageVsEntity();
    }
}
