package btw.modern;

public class EntityCow extends EntityAnimal {

    public EntityCow(World world) {
        super(world);
    }

    public int getMaxHealth() {
        return 10;
    }

    public void entityInit() {}
}
