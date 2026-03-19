package btw.modern;

public class EntityChicken extends EntityAnimal {

    public int timeUntilNextEgg;

    public EntityChicken(World world) {
        super(world);
    }

    public int getMaxHealth() {
        return 4;
    }

    public void entityInit() {}
}
