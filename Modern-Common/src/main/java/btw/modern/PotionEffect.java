package btw.modern;

public class PotionEffect {
    private int potionID;
    private int duration;
    private int amplifier;
    private boolean isSplashPotion;
    private boolean isAmbient;

    public PotionEffect(int id, int duration) {
        this(id, duration, 0);
    }

    public PotionEffect(int id, int duration, int amplifier) {
        this(id, duration, amplifier, false);
    }

    public PotionEffect(int id, int duration, int amplifier, boolean isAmbient) {
        this.potionID = id;
        this.duration = duration;
        this.amplifier = amplifier;
        this.isAmbient = isAmbient;
    }

    public PotionEffect(PotionEffect other) {
        this.potionID = other.potionID;
        this.duration = other.duration;
        this.amplifier = other.amplifier;
    }

    public void combine(PotionEffect other) {}
    public int getPotionID() { return this.potionID; }
    public int getDuration() { return this.duration; }
    public int getAmplifier() { return this.amplifier; }
    public boolean isSplashPotionEffect() { return this.isSplashPotion; }
    public void setSplashPotion(boolean splash) { this.isSplashPotion = splash; }
    public boolean getIsAmbient() { return this.isAmbient; }

    /**
     * Ticks this effect. Returns true if the effect is still active.
     * Mirrors vanilla 1.5.2 PotionEffect.onUpdate().
     */
    public boolean onUpdate(EntityLiving entity) {
        if (this.duration > 0) {
            this.duration--;
        }
        return this.duration > 0;
    }
    public void performEffect(EntityLiving entity) {}
    public String getEffectName() { return ""; }

    public NBTTagCompound writeCustomPotionEffectToNBT(NBTTagCompound tag) { return tag; }
    public static PotionEffect readCustomPotionEffectFromNBT(NBTTagCompound tag) {
        return new PotionEffect(0, 0);
    }
}
