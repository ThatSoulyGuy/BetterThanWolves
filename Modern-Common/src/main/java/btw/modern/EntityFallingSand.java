package btw.modern;

public class EntityFallingSand extends Entity {

    public int blockID;
    public int metadata;
    public int fallTime;
    public boolean shouldDropItem = true;

    public EntityFallingSand(World world) {
        super(world);
    }

    public EntityFallingSand(World world, double x, double y, double z, int blockID) {
        super(world);
        this.blockID = blockID;
    }

    public EntityFallingSand(World world, double x, double y, double z, int blockID, int metadata) {
        super(world);
        this.blockID = blockID;
        this.metadata = metadata;
    }

    public void fall(float distance) {}
    public void setIsAnvil(boolean isAnvil) {}

    public void entityInit() {}
}
