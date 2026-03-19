package btw.modern;

public class EntityEnderman extends EntityMob {

    public EntityEnderman(World world) {
        super(world);
    }

    public int getMaxHealth() {
        return 40;
    }

    public int getCarried() {
        return 0;
    }

    public int getCarryingData() {
        return 0;
    }

    public void setCarried(int blockId) {}

    public void setCarryingData(int metadata) {}

    public boolean teleportRandomly() { return false; }
    public boolean teleportToEntity(Entity entity) { return false; }
    public boolean isScreaming() { return false; }
    public void setScreaming(boolean screaming) {}

    public void entityInit() {}
}
