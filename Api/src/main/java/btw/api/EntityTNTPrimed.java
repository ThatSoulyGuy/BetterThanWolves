package btw.api;

public class EntityTNTPrimed extends Entity {




    public int fuse;
    public EntityLiving tntPlacedBy;
    public EntityTNTPrimed(World world) { super(world); }
    public EntityTNTPrimed(World world, double x, double y, double z, EntityLiving placer) { super(world); }

    public void entityInit() {}
}
