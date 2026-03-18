package btw.api;

public class EntityArrow extends Entity implements IProjectile {

    public int xTile = -1;
    public int yTile = -1;
    public int zTile = -1;
    public boolean inGround;
    public int arrowShake;
    public Entity shootingEntity;
    public double damage = 2.0D;

    public EntityArrow(World world) {
        super(world);
    }

    public EntityArrow(World world, EntityLiving shooter, float force) {
        super(world);
        this.shootingEntity = shooter;
    }

    public EntityArrow(World world, double x, double y, double z) {
        super(world);
    }

    public EntityArrow(World world, EntityLiving shooter, EntityLiving target, float speed, float randomFactor) {
        super(world);
        this.shootingEntity = shooter;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public double getDamage() {
        return this.damage;
    }

    public void setIsCritical(boolean critical) {}

    public boolean getIsCritical() {
        return false;
    }

    // BTW methods
    public float GetDamageMultiplier() { return 1.0F; }
    public boolean AddArrowToPlayerInv(EntityPlayer player) { return false; }
    public Item GetCorrespondingItem() { return null; }
    public boolean CanHopperCollect() { return false; }
    public int GetTrackerViewDistance() { return 64; }
    public int GetTrackerUpdateFrequency() { return 20; }
    public boolean ShouldServerTreatAsOversized() { return false; }
    public boolean GetTrackMotion() { return true; }
    public Packet GetSpawnPacketForThisEntity() { return null; }

    public void entityInit() {}

    public void setKnockbackStrength(int strength) {}

    public void setThrowableHeading(double x, double y, double z, float velocity, float inaccuracy) {}

    public int canBePickedUp;
}
