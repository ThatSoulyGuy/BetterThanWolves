package btw.modern;

public abstract class EntityFireball extends Entity {

    public EntityLiving shootingEntity;
    public double accelerationX;
    public double accelerationY;
    public double accelerationZ;

    public EntityFireball(World world) {
        super(world);
    }

    public EntityFireball(World world, double x, double y, double z, double accelX, double accelY, double accelZ) {
        super(world);
        this.accelerationX = accelX;
        this.accelerationY = accelY;
        this.accelerationZ = accelZ;
    }

    public EntityFireball(World world, EntityLiving shooter, double accelX, double accelY, double accelZ) {
        super(world);
        this.shootingEntity = shooter;
        this.accelerationX = accelX;
        this.accelerationY = accelY;
        this.accelerationZ = accelZ;
    }

    protected abstract void onImpact(MovingObjectPosition result);
}
