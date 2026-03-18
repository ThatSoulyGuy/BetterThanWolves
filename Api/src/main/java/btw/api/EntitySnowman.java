package btw.api;

public class EntitySnowman extends EntityGolem {

    public EntitySnowman(World world) {
        super(world);
    }

    public int getMaxHealth() {
        return 4;
    }

    public void entityInit() {}
}
