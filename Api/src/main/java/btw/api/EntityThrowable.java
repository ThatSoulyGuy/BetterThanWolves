package btw.api;

public abstract class EntityThrowable extends Entity implements IProjectile {

    public EntityThrowable(World world) {
        super(world);
    }

    public EntityThrowable(World world, EntityLiving thrower) {
        super(world);
    }

    public EntityThrowable(World world, double x, double y, double z) {
        super(world);
    }

    public EntityLiving getThrower() {
        return null;
    }

    protected abstract void onImpact(MovingObjectPosition result);

    public void SetThrower(EntityLiving thrower) {}
    public void setThrowableHeading(double x, double y, double z, float velocity, float inaccuracy) {}
}
