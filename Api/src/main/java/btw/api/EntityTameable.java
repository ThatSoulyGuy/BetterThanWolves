package btw.api;

public abstract class EntityTameable extends EntityAnimal {

    public EntityAISit aiSit;

    protected EntityTameable(World world) {
        super(world);
    }

    public boolean isTamed() {
        return false;
    }

    public void setTamed(boolean tamed) {}

    public boolean isSitting() {
        return false;
    }

    public void setSitting(boolean sitting) {}

    public EntityLiving getOwner() {
        return null;
    }

    public String getOwnerName() { return null; }
    public void setOwner(String name) {}
    public void playTameEffect(boolean happy) {}
    public boolean func_70918_i() { return false; }
}
