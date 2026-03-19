package btw.modern;

public class Potion {
    public static final Potion[] potionTypes = new Potion[32];
    public static final Potion field_76423_b = null;
    public static Potion moveSpeed;
    public static Potion moveSlowdown;
    public static Potion digSpeed;
    public static Potion digSlowdown;
    public static Potion damageBoost;
    public static Potion heal;
    public static Potion harm;
    public static Potion jump;
    public static Potion confusion;
    public static Potion regeneration;
    public static Potion resistance;
    public static Potion fireResistance;
    public static Potion waterBreathing;
    public static Potion invisibility;
    public static Potion blindness;
    public static Potion nightVision;
    public static Potion hunger;
    public static Potion weakness;
    public static Potion poison;
    public static Potion wither;

    public final int id;
    private String name = "";
    private int statusIconIndex = -1;
    private final boolean isBadEffect;
    private double effectiveness;
    private boolean usable;
    private final int liquidColor;

    public Potion(int id, boolean isBad, int color) {
        this.id = id;
        this.isBadEffect = isBad;
        this.effectiveness = isBad ? 0.5D : 1.0D;
        this.liquidColor = color;
        if (id >= 0 && id < potionTypes.length) {
            potionTypes[id] = this;
        }
    }

    public Potion setIconIndex(int x, int y) {
        this.statusIconIndex = x + y * 8;
        return this;
    }

    public int getId() { return this.id; }

    public void performEffect(EntityLiving entity, int amplifier) {}

    public void affectEntity(EntityLiving source, EntityLiving target, int amplifier, double effectiveness) {}

    public boolean isInstant() { return false; }

    public boolean isReady(int duration, int amplifier) { return false; }

    public Potion setPotionName(String name) {
        this.name = name;
        return this;
    }

    public String getName() { return this.name; }

    public Potion setEffectiveness(double effectiveness) {
        this.effectiveness = effectiveness;
        return this;
    }

    public double getEffectiveness() { return this.effectiveness; }
    public boolean isUsable() { return this.usable; }
    public int getLiquidColor() { return this.liquidColor; }
    public boolean isBadEffect() { return this.isBadEffect; }

    /**
     * Populates vanilla potion static fields. Called during Forge mod init
     * BEFORE FC code runs.
     * IDs, isBad flags, and colors match vanilla MC 1.5.2.
     */
    public static void initializeVanillaPotions() {
        moveSpeed       = new Potion(1,  false, 8171462).setPotionName("potion.moveSpeed").setIconIndex(0, 0);
        moveSlowdown    = new Potion(2,  true,  5926017).setPotionName("potion.moveSlowdown").setIconIndex(1, 0);
        digSpeed        = new Potion(3,  false, 14270531).setPotionName("potion.digSpeed").setIconIndex(2, 0);
        digSlowdown     = new Potion(4,  true,  4866583).setPotionName("potion.digSlowdown").setIconIndex(3, 0);
        damageBoost     = new Potion(5,  false, 9643043).setPotionName("potion.damageBoost").setIconIndex(4, 0);
        heal            = new Potion(6,  false, 16262179).setPotionName("potion.heal").setIconIndex(5, 0);
        harm            = new Potion(7,  true,  4393481).setPotionName("potion.harm").setIconIndex(6, 0);
        jump            = new Potion(8,  false, 2293580).setPotionName("potion.jump").setIconIndex(2, 1);
        confusion       = new Potion(9,  true,  5578058).setPotionName("potion.confusion").setIconIndex(3, 1);
        regeneration    = new Potion(10, false, 13458603).setPotionName("potion.regeneration").setIconIndex(7, 0);
        resistance      = new Potion(11, false, 10044730).setPotionName("potion.resistance").setIconIndex(6, 1);
        fireResistance  = new Potion(12, false, 14981690).setPotionName("potion.fireResistance").setIconIndex(7, 1);
        waterBreathing  = new Potion(13, false, 3035801).setPotionName("potion.waterBreathing").setIconIndex(0, 2);
        invisibility    = new Potion(14, false, 8356754).setPotionName("potion.invisibility").setIconIndex(0, 1);
        blindness       = new Potion(15, true,  2039587).setPotionName("potion.blindness").setIconIndex(5, 1);
        nightVision     = new Potion(16, false, 2039713).setPotionName("potion.nightVision").setIconIndex(4, 1);
        hunger          = new Potion(17, true,  5797459).setPotionName("potion.hunger").setIconIndex(1, 1);
        weakness        = new Potion(18, true,  4738376).setPotionName("potion.weakness").setIconIndex(5, 1);
        poison          = new Potion(19, true,  5149489).setPotionName("potion.poison").setIconIndex(6, 0);
        wither          = new Potion(20, true,  3484199).setPotionName("potion.wither").setIconIndex(1, 2);
    }
}
