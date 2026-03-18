package btw.api;

public class EntityXPOrb extends Entity {





    public int xpValue;
    public boolean m_bNotPlayerOwned;
    public EntityXPOrb(World world) { super(world); }
    public EntityXPOrb(World world, double x, double y, double z, int value) {
        super(world);
        this.xpValue = value;
    }
    public EntityXPOrb(World world, double x, double y, double z, int value, boolean isFromBottle) {
        this(world, x, y, z, value);
    }
    public int getXpValue() { return xpValue; }
    public static int getPlayersXPSplit(int amount) { return amount; }
    public void entityInit() {}
}
