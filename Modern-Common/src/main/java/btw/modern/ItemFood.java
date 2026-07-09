package btw.modern;

public class ItemFood extends Item {

    public final int itemUseDuration;
    private final int healAmount;
    private final float saturationModifier;
    private final boolean isWolfsFavoriteMeat;
    private boolean alwaysEdible;

    // 1.5.2 ItemFood potion-effect fields — set by setPotionEffect (FCItemCreeperOysters,
    // FCItemFood.SetStandardFoodPoisoningEffect), applied by onFoodEaten.
    private int potionId;
    private int potionDuration;
    private int potionAmplifier;
    private float potionEffectProbability;

    public ItemFood(int id, int healAmount, float saturation, boolean wolfFood) {
        super(id);
        this.itemUseDuration = 32;
        this.healAmount = healAmount;
        this.saturationModifier = saturation;
        this.isWolfsFavoriteMeat = wolfFood;
        // 1.5.2 (FCMOD) ItemFood ctor additions — BTWRegistration reads maxStackSize,
        // EntityItem.UpdateHardcoreBuoy reads buoyancy, FCTileEntityCrucible reads incineration.
        // FCMOD: Added
        maxStackSize = 16;
        SetBuoyant();
        SetIncineratedInCrucible();
        // END FCMOD
    }

    public ItemFood(int id, int healAmount, boolean wolfFood) {
        this(id, healAmount, 0.6F, wolfFood);
    }

    // 1.5.2 (FCMOD) ItemFood 5-arg ctor — stores m_bDoZombiesConsume, read by
    // FCEntityWolfDire.CheckForLooseFood (item.DoZombiesConsume()).
    // FCMOD: Added New
    private boolean m_bDoZombiesConsume = false;

    public ItemFood(int id, int healAmount, float saturation, boolean wolfFood, boolean zombiesConsume) {
        this(id, healAmount, saturation, wolfFood);

        m_bDoZombiesConsume = zombiesConsume;
    }

    @Override
    public boolean DoZombiesConsume() {
        return m_bDoZombiesConsume;
    }
    // END FCMOD

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

    // 1.5.2 ItemFood.setPotionEffect — stores the effect applied on eating; live via
    // FCItemCreeperOysters ctor and FCItemFood.Set*FoodPoisoningEffect on raw foods.
    public ItemFood setPotionEffect(int potionId, int duration, int amplifier, float probability) {
        this.potionId = potionId;
        this.potionDuration = duration;
        this.potionAmplifier = amplifier;
        this.potionEffectProbability = probability;
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
        this.onFoodEaten(stack, world, player);
        return stack;
    }

    // 1.5.2 ItemFood.onFoodEaten — applies the stored potion effect (food poisoning etc.);
    // PlayerBridge.addPotionEffect forwards to the modern ServerPlayer.addEffect.
    public void onFoodEaten(ItemStack stack, World world, EntityPlayer player) {
        if (!world.isRemote && this.potionId > 0 && world.rand.nextFloat() < this.potionEffectProbability) {
            player.addPotionEffect(new PotionEffect(this.potionId, this.potionDuration * 20, this.potionAmplifier));
        }
    }

    // 1.5.2 (FCMOD) ItemFood.IsWolfFood/GetWolfHealAmount — FCEntityWolf.java:346/386/415
    // (feeding/taming/loose-food checks) and :726 heal(food.GetWolfHealAmount()).
    // FCMOD: Added New
    @Override
    public boolean IsWolfFood() {
        return isWolfsFavoriteMeat();
    }

    @Override
    public int GetWolfHealAmount() {
        return getHealAmount();
    }
    // END FCMOD
}
