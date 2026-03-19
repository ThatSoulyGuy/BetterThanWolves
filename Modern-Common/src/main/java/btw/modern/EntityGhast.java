package btw.modern;

public class EntityGhast extends EntityFlying {

    public int courseChangeCooldown = 0;
    public double waypointX;
    public double waypointY;
    public double waypointZ;
    public int attackCounter = 0;
    public int prevAttackCounter = 0;

    public EntityGhast(World world) {
        super(world);
    }

    public int getMaxHealth() {
        return 10;
    }

    public void entityInit() {}
}
