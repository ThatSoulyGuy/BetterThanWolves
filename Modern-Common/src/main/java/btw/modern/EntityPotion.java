package btw.modern;

public class EntityPotion extends EntityThrowable {
    public EntityPotion(World world) { super(world); }
    public EntityPotion(World world, EntityLiving thrower, int damage) { super(world); }
    public EntityPotion(World world, double x, double y, double z, int damage) { super(world); }
    public EntityPotion(World world, double x, double y, double z, ItemStack stack) { super(world); }
    public int getPotionDamage() { return 0; }
    public void entityInit() {}
    protected void onImpact(MovingObjectPosition result) {}
}
