package btw.api;

public class EntityZombie extends EntityMob {

    public EntityZombie(World world) {
        super(world);
    }

    public int getMaxHealth() {
        return 20;
    }

    public boolean isVillager() {
        return false;
    }

    public void setVillager(boolean villager) {}

    public boolean isChild() {
        return false;
    }

    public void setChild(boolean child) {}

    public void entityInit() {}
}
