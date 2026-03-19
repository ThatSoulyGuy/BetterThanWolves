package btw.modern;

public class EntityWolf extends EntityTameable {

    public EntityWolf(World world) {
        super(world);
    }

    public int getMaxHealth() {
        return 8;
    }

    public boolean isAngry() {
        return false;
    }

    public void setAngry(boolean angry) {}

    public int getCollarColor() {
        return 0;
    }

    public void setCollarColor(int color) {}

    public void entityInit() {}

    public void becomeAngryAt(Entity entity) {}

    public float getTailRotation() { return 0.0F; }
}
