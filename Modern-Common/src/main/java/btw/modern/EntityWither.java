package btw.modern;

public class EntityWither extends EntityMob {

    public EntityWither(World world) {
        super(world);
    }

    public int getMaxHealth() {
        return 300;
    }

    public int getWatchedTargetId(int head) {
        return 0;
    }

    public int getInvulTime() {
        return 0;
    }

    public void setInvulTime(int time) {}

    public void entityInit() {}
}
