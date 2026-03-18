package btw.api;

public class EntitySpider extends EntityMob {

    public EntitySpider(World world) {
        super(world);
    }

    public int getMaxHealth() {
        return 16;
    }

    public boolean isBesideClimbableBlock() {
        return false;
    }

    public void setBesideClimbableBlock(boolean climbing) {}

    public void entityInit() {}
}
