package btw.modern;

// Compile-time stub — real implementation provided by vanilla remap at runtime.
// MUST match vanilla EntityMoveHelper signatures exactly so JIT-resolved
// method calls don't NoSuchMethodError when the runtime class is loaded.
public class EntityMoveHelper {
    protected EntityLiving entity;
    protected double posX, posY, posZ;
    protected float speed;  // vanilla uses float, not double
    protected boolean update;

    public EntityMoveHelper(EntityLiving entity) {
        this.entity = entity;
    }

    public boolean isUpdating() { return update; }
    public float getSpeed() { return speed; }  // vanilla returns float

    public void setMoveTo(double x, double y, double z, double speed) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        this.speed = (float) speed;
        this.update = true;
    }

    public void onUpdateMoveHelper() {
        if (!update) {
            entity.moveForward = 0;
            return;
        }
        update = false;
        double dx = posX - entity.posX;
        double dz = posZ - entity.posZ;
        double dy = posY - entity.posY;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 0.1) {
            entity.moveForward = 0;
            return;
        }
        float yaw = (float)(Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
        entity.rotationYaw = limitAngle(entity.rotationYaw, yaw, 30.0F);
        entity.moveForward = (float)(speed);
        if (dy > 0.0 && dist < 1.5) {
            entity.isJumping = true;
        }
    }

    private float limitAngle(float current, float target, float maxDelta) {
        float d = target - current;
        while (d < -180F) d += 360F;
        while (d >= 180F) d -= 360F;
        if (d > maxDelta) d = maxDelta;
        if (d < -maxDelta) d = -maxDelta;
        return current + d;
    }
}
