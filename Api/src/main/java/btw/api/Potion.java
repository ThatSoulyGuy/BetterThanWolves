package btw.api;

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
}
