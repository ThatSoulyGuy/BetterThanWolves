package btw.modern;

public class EntityEgg extends EntityThrowable {
    public EntityEgg(World world) { super(world); }
    public EntityEgg(World world, EntityLiving thrower) { super(world); }
    public EntityEgg(World world, double x, double y, double z) { super(world); }
    public void entityInit() {}
    protected void onImpact(MovingObjectPosition result) {}
}
