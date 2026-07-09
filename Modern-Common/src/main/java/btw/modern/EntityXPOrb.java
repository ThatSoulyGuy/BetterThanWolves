package btw.modern;

public class EntityXPOrb extends Entity {





    public int xpValue;
    public boolean m_bNotPlayerOwned;
    /** Color-pulse animation counter, read by RenderXPOrb. Live value comes from the
     *  frozen EntityXPOrb (this class is shadow-excluded at runtime); declared here so
     *  RenderXPOrb compiles against the shim. */
    public int xpColor;
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

    /** 1.5.2 EntityXPOrb.getTextureByXP — frame index (0-10) of the /item/xporb.png atlas
     *  by orb value. Pure function of xpValue; the frozen runtime class provides the live
     *  one, this mirrors it so RenderXPOrb links. */
    public int getTextureByXP() {
        return this.xpValue >= 2477 ? 10 : (this.xpValue >= 1237 ? 9 : (this.xpValue >= 617 ? 8
                : (this.xpValue >= 307 ? 7 : (this.xpValue >= 149 ? 6 : (this.xpValue >= 73 ? 5
                : (this.xpValue >= 37 ? 4 : (this.xpValue >= 17 ? 3 : (this.xpValue >= 7 ? 2
                : (this.xpValue >= 3 ? 1 : 0)))))))));
    }
}
