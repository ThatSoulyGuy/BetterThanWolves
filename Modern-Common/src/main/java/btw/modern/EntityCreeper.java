package btw.modern;

public class EntityCreeper extends EntityMob {

    public EntityCreeper(World world) {
        super(world);
    }

    public int getMaxHealth() {
        return 20;
    }

    public int getCreeperState() {
        return 0;
    }

    public void setCreeperState(int state) {}

    public boolean getPowered() {
        return false;
    }

    public void entityInit() {}
}
