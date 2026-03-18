package btw.api;

public class EntityFishHook extends Entity {

    public EntityPlayer angler;
    public EntityFishHook(World world) { super(world); }
    public EntityFishHook(World world, EntityPlayer player) { super(world); this.angler = player; }
    public EntityFishHook(World world, EntityPlayer player, boolean baited) { this(world, player); }
    public int catchFish() { return 0; }




    public void entityInit() {}
}
