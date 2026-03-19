package btw.modern;

public class EntityBat extends EntityAmbientCreature {

    public EntityBat(World world) {
        super(world);
    }

    public int getMaxHealth() {
        return 6;
    }

    public void entityInit() {}
}
