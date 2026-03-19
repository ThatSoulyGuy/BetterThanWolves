package btw.modern;

public class EntitySkeleton extends EntityMob implements IRangedAttackMob {

    public EntitySkeleton(World world) {
        super(world);
    }

    public int getMaxHealth() {
        return 20;
    }

    public int getSkeletonType() {
        return 0;
    }

    public void setSkeletonType(int type) {}

    public void entityInit() {}
}
