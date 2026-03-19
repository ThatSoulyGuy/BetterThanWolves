package btw.modern;

public abstract class EntityAnimal extends EntityAgeable {

    public int breeding;
    public int m_iGrazeProgressCounter;
    public static int m_iBaseGrazeFoodValue;
    public boolean m_bNotPlayerOwned;
    public static final int m_iFullHungerCount = 24000;
    public int m_iHungerCountdown;
    public int m_iHealingCountdown;

    protected EntityAnimal(World world) {
        super(world);
    }

    public boolean getWearingBreedingHarness() {
        return false;
    }

    public boolean CanGrazeMycelium() {
        return false;
    }

    public boolean isInLove() {
        return false;
    }

    public void resetInLove() {}

    public void setWearingBreedingHarness(boolean wearing) {}
    public boolean IsTemptingItem(ItemStack stack) { return false; }
    public boolean IsHungryEnoughToGraze() { return false; }
    public void OnNearbyFireStartAttempt(EntityPlayer player) {}

    // --- Client-side grazing animation ---

    public float GetGrazeHeadVerticalOffset(float partialTick) { return 0.0F; }
    public float GetGrazeHeadRotation(float partialTick) { return 0.0F; }
}
