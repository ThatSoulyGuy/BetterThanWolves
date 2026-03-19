package btw.modern;

public class EntityMinecart extends Entity {


    public int minecartType;
    public EntityMinecart(World world) { super(world); }
    public EntityMinecart(World world, double x, double y, double z, int type) {
        super(world);
        this.minecartType = type;
    }



    public void entityInit() {}
    public boolean interact(EntityPlayer player) { return false; }
    public int getMinecartType() { return this.minecartType; }

    public static EntityMinecart createMinecart(World world, double x, double y, double z, int type) {
        return new EntityMinecart(world, x, y, z, type);
    }

    public Object getAIControlledByPlayer() { return null; }
}
