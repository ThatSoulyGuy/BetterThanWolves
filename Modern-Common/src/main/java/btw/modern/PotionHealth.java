package btw.modern;

/**
 * 1.5.2 PotionHealth (vanilla/server PotionHealth.java) — the instant heal/harm
 * potion type. isInstant()=true routes PotionHelper.getPotionEffects down the
 * instant-strength branch; isReady()=duration>=1 fires it once.
 */
public class PotionHealth extends Potion {

    public PotionHealth(int id, boolean isBad, int color) {
        super(id, isBad, color);
    }

    @Override
    public boolean isInstant() {
        return true;
    }

    @Override
    public boolean isReady(int duration, int amplifier) {
        return duration >= 1;
    }
}
