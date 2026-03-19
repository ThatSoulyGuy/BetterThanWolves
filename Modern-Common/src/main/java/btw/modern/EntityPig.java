package btw.modern;

public class EntityPig extends EntityAnimal {

    public EntityPig(World world) {
        super(world);
    }

    public int getMaxHealth() {
        return 10;
    }

    public boolean getSaddled() {
        return false;
    }

    public void setSaddled(boolean saddled) {}

    public void entityInit() {}

    public EntityAIControlledByPlayer getAIControlledByPlayer() { return null; }
}
