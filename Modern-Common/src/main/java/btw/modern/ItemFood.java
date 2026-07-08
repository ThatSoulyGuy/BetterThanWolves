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

    // --- 1.5.2 eat-start sequence (verbatim vanilla ItemFood + FCMOD hook) ---
    // FC food classes (FCItemFood and subclasses) do NOT override these; they
    // rely on this base class. ProxyItem.getUseAnimation/getUseDuration and
    // PlayerBridge.setItemInUse -> ServerPlayer.startUsingItem bridge them to
    // the modern engine. Without all three, right-clicking FC food does
    // nothing: use action null, duration 0, and no startUsingItem call.

    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return this.itemUseDuration;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.eat;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (player.canEat(this.alwaysEdible)) {
            player.setItemInUse(stack, this.getMaxItemUseDuration(stack));
        } else {
            // FCMOD: Added
            player.OnCantConsume();
        }
        return stack;
    }

    /**
     * Base ItemFood stores its heal amount in vanilla (0-20) units, but FC's
     * food bar is high-res (0-60), so the restored value is tripled. FC's
     * {@code FCItemFoodHighRes} overrides this to return the raw amount (its
     * heal value is already expressed in high-res units).
     */
    @Override
    public int GetHungerRestored() {
        return getHealAmount() * 3;
    }

    /**
     * Restores hunger/saturation through FC's high-res {@link FoodStats}.
     * Mirrors the original FC {@code ItemFood.onEaten -> FoodStats.addStats(this)}.
     * The MC-side stack decrement and HUD sync are performed by the bridge layer
     * (ProxyItem / ItemStackMixin), so this only applies nutrition.
     */
    @Override
    public ItemStack onEaten(ItemStack stack, World world, EntityPlayer player) {
        player.getFoodStats().addStats(this);
        return stack;
    }
}
