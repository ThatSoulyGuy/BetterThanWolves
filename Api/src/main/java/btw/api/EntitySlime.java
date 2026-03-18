package btw.api;

public class EntitySlime extends EntityLiving {

    public EntitySlime(World world) {
        super(world);
    }

    public int getMaxHealth() {
        return 1;
    }

    public int getSlimeSize() {
        return 1;
    }

    public void setSlimeSize(int size) {}

    public String getJumpSound() { return "mob.slime.big"; }
    public int getJumpDelay() { return 0; }
    public EntitySlime createInstance() { return null; }

    public void entityInit() {}
}
