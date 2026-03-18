package btw.api;

public class EntityWitherSkull extends EntityFireball {

    public EntityWitherSkull(World world) {
        super(world);
    }

    public EntityWitherSkull(World world, EntityLiving shooter, double accelX, double accelY, double accelZ) {
        super(world, shooter, accelX, accelY, accelZ);
    }

    public EntityWitherSkull(World world, double x, double y, double z, double accelX, double accelY, double accelZ) {
        super(world);
        this.setPosition(x, y, z);
    }

    protected void onImpact(MovingObjectPosition result) {}

    public void entityInit() {}
}
