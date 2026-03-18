package btw.api;

public class EntityOcelot extends EntityTameable {

    public EntityOcelot(World world) {
        super(world);
    }

    public int getMaxHealth() {
        return 10;
    }

    public void entityInit() {}
}
