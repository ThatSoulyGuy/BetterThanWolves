package btw.modern;

public class ItemFood extends Item {

    public final int itemUseDuration;
    private final int healAmount;
    private final float saturationModifier;
    private final boolean isWolfsFavoriteMeat;
    private boolean alwaysEdible;

    public ItemFood(int id, int healAmount, float saturation, boolean wolfFood) {
        super(id);
        this.itemUseDuration = 32;
        this.healAmount = healAmount;
        this.saturationModifier = saturation;
        this.isWolfsFavoriteMeat = wolfFood;
    }

    public ItemFood(int id, int healAmount, boolean wolfFood) {
        this(id, healAmount, 0.6F, wolfFood);
    }

    public ItemFood(int id, int healAmount, float saturation, boolean wolfFood, boolean zombiesConsume) {
        this(id, healAmount, saturation, wolfFood);
    }

    public int getHealAmount() {
        return this.healAmount;
    }

    public float getSaturationModifier() {
        return this.saturationModifier;
    }

    public boolean isWolfsFavoriteMeat() {
        return this.isWolfsFavoriteMeat;
    }

    public ItemFood setAlwaysEdible() {
        this.alwaysEdible = true;
        return this;
    }

    public ItemFood setPotionEffect(int potionId, int duration, int amplifier, float probability) {
        return this;
    }
}
