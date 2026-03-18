package btw.api;

public abstract class EntityAgeable extends EntityCreature {

    protected EntityAgeable(World world) {
        super(world);
    }

    public int getGrowingAge() {
        return 0;
    }

    public void setGrowingAge(int age) {}

    public boolean isChild() {
        return false;
    }

    public EntityAgeable createChild(EntityAgeable parent) {
        return null;
    }

    public int GetTicksForChildToGrow() { return 24000; }

    public EntityAgeable spawnBabyAnimal(EntityAgeable parent) { return null; }
    public EntityAgeable func_90012_b(EntityAgeable parent) { return null; }
}
