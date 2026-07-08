package btw.modern;

public class EntityPotion extends EntityThrowable {

    // 1.5.2 stores the thrown potion as an ItemStack whose damage value
    // encodes the potion type; EntityWitch.attackEntityWithRangedAttack
    // calls setPotionDamage(32698/32660/32696) right after construction.
    private ItemStack potionDamage;

    public EntityPotion(World world) { super(world); }

    public EntityPotion(World world, EntityLiving thrower, int damage) {
        super(world);
        this.setPotionDamage(damage);
    }

    public EntityPotion(World world, double x, double y, double z, int damage) {
        super(world);
        this.setPotionDamage(damage);
    }

    public EntityPotion(World world, double x, double y, double z, ItemStack stack) {
        super(world);
        this.potionDamage = stack;
    }

    // 1.5.2 EntityPotion.setPotionDamage
    public void setPotionDamage(int damage) {
        if (this.potionDamage == null) {
            this.potionDamage = new ItemStack(Item.potion, 1, 0);
        }
        this.potionDamage.setItemDamage(damage);
    }

    // 1.5.2 EntityPotion.getPotionDamage
    public int getPotionDamage() {
        if (this.potionDamage == null) {
            this.potionDamage = new ItemStack(Item.potion, 1, 0);
        }
        return this.potionDamage.getItemDamage();
    }

    public void entityInit() {}
    protected void onImpact(MovingObjectPosition result) {}
}
