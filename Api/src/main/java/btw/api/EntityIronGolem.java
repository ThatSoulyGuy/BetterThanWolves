package btw.api;

public class EntityIronGolem extends EntityGolem {
    public EntityIronGolem(World world) { super(world); }
    public int getMaxHealth() { return 100; }
    public void entityInit() {}
    public void setPlayerCreated(boolean playerCreated) {}
}
