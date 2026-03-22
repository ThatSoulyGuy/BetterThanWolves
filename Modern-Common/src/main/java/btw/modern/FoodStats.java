package btw.modern;

public class FoodStats {
    // FC: 3x resolution — 60 = full (not 20)
    private int foodLevel = 60;

    // FC: player spawns with zero fat (not 5.0F)
    private float foodSaturationLevel = 0F;

    private float foodExhaustionLevel;

    private int foodTimer = 0;

    // FC: 3x resolution
    private int prevFoodLevel = 60;

    /**
     * FC food gain logic.
     * iFoodGain is one third regular hunger gained, with 6 units being a full pip.
     * Only excess food (above the 60 cap) converts to saturation (fat), capped at 20F.
     */
    public void addStats(int iFoodGain, float fFatMultiplier) {
        int iPreviousFoodLevel = foodLevel;

        foodLevel = Math.min(iFoodGain + foodLevel, 60);

        int iExcessFood = iFoodGain - (foodLevel - iPreviousFoodLevel);

        if (iExcessFood > 0) {
            // divide by 3 due to increased resolution
            foodSaturationLevel = Math.min(foodSaturationLevel + (float) iExcessFood * fFatMultiplier / 3F, 20F);
        }
    }

    /**
     * Eat some food.
     */
    public void addStats(ItemFood par1ItemFood) {
        this.addStats(par1ItemFood.GetHungerRestored(), par1ItemFood.getSaturationModifier());
    }

    /**
     * Handles the food game logic (FC version).
     * Called on server only.
     */
    public void onUpdate(EntityPlayer player) {
        int iDifficulty = player.worldObj != null ? player.worldObj.difficultySetting : 2;

        prevFoodLevel = foodLevel;

        if (iDifficulty > 0) {
            // burn hunger
            while (foodLevel > 0 && foodExhaustionLevel >= 1.33F && !shouldBurnFatBeforeHunger()) {
                foodExhaustionLevel -= 1.33F;

                foodLevel = Math.max(foodLevel - 1, 0);
            }

            // burn fat
            while (foodExhaustionLevel >= 0.5F && shouldBurnFatBeforeHunger()) {
                foodExhaustionLevel -= 0.5F;

                foodSaturationLevel = Math.max(foodSaturationLevel - 0.125F, 0F);
            }
        } else {
            foodExhaustionLevel = 0F;
        }

        if (foodLevel > 24 && player.getHealth() > 0 && player.getHealth() < player.getMaxHealth()) {
            ++foodTimer;

            if (foodTimer >= 600) // once every 30 seconds
            {
                player.heal(1);
                foodTimer = 0;
            }
        } else if (foodLevel <= 0 && foodSaturationLevel <= 0.01F) {
            ++foodTimer;

            if (foodTimer >= 80) {
                if (iDifficulty > 0) {
                    player.attackEntityFrom(DamageSource.starve, 1);
                }

                foodTimer = 0;
            }

            // reset the exhaustion level so that it doesn't stack up while the player is starving
            foodExhaustionLevel = 0F;
        } else {
            foodTimer = 0;
        }
    }

    /**
     * Reads the food data for the player.
     */
    public void readNBT(NBTTagCompound par1NBTTagCompound) {
        if (par1NBTTagCompound.hasKey("foodLevel")) {
            this.foodLevel = par1NBTTagCompound.getInteger("foodLevel");
            this.foodTimer = par1NBTTagCompound.getInteger("foodTickTimer");
            this.foodSaturationLevel = par1NBTTagCompound.getFloat("foodSaturationLevel");
            this.foodExhaustionLevel = par1NBTTagCompound.getFloat("foodExhaustionLevel");

            // upgrade vanilla food level x3 if not already adjusted
            if (!par1NBTTagCompound.hasKey("fcFoodLevelAdjusted")) {
                foodLevel = foodLevel * 3;
                foodSaturationLevel = 0F;
            }

            // sanity check the values as apparently they can get fucked up when importing from vanilla
            if (foodLevel > 60 || foodLevel < 0) {
                foodLevel = 60;
            }

            if (foodSaturationLevel > 20F || foodSaturationLevel < 0F) {
                foodSaturationLevel = 20F;
            }
        }
    }

    /**
     * Writes the food data for the player.
     */
    public void writeNBT(NBTTagCompound par1NBTTagCompound) {
        par1NBTTagCompound.setInteger("foodLevel", this.foodLevel);
        par1NBTTagCompound.setInteger("foodTickTimer", this.foodTimer);
        par1NBTTagCompound.setFloat("foodSaturationLevel", this.foodSaturationLevel);
        par1NBTTagCompound.setFloat("foodExhaustionLevel", this.foodExhaustionLevel);

        par1NBTTagCompound.setBoolean("fcFoodLevelAdjusted", true);
    }

    /**
     * Get the player's food level.
     */
    public int getFoodLevel() {
        return this.foodLevel;
    }

    public int getPrevFoodLevel() {
        return this.prevFoodLevel;
    }

    /**
     * Get whether the player must eat food.
     * FC: 60 max instead of 20.
     */
    public boolean needFood() {
        return this.foodLevel < 60;
    }

    /**
     * Adds input to foodExhaustionLevel to a max of 40.
     */
    public void addExhaustion(float par1) {
        this.foodExhaustionLevel = Math.min(this.foodExhaustionLevel + par1, 40.0F);
    }

    /**
     * Get the player's food saturation level.
     */
    public float getSaturationLevel() {
        return this.foodSaturationLevel;
    }

    public void setFoodLevel(int par1) {
        this.foodLevel = par1;
    }

    public void setFoodSaturationLevel(float par1) {
        this.foodSaturationLevel = par1;
    }

    /**
     * FC: Only burn fat when the corresponding hunger pip is completely depleted.
     */
    private boolean shouldBurnFatBeforeHunger() {
        return foodSaturationLevel > (float) ((foodLevel + 5) / 6) * 2F;
    }
}
