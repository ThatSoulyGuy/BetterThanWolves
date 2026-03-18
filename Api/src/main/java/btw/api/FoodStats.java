package btw.api;

public class FoodStats {
    private int foodLevel = 20;
    private float foodSaturationLevel = 5.0F;
    private float foodExhaustionLevel;
    private int foodTimer;

    public void addStats(int food, float saturation) {}
    public void addStats(ItemFood food) {}
    public void onUpdate(EntityPlayer player) {}
    public int getFoodLevel() { return foodLevel; }
    public boolean needFood() { return foodLevel < 20; }
    public void addExhaustion(float exhaustion) {}
    public float getSaturationLevel() { return foodSaturationLevel; }
    public void setFoodLevel(int level) { this.foodLevel = level; }
    public void setFoodSaturationLevel(float level) { this.foodSaturationLevel = level; }

    public void readNBT(NBTTagCompound tag) {}
    public void writeNBT(NBTTagCompound tag) {}
}
