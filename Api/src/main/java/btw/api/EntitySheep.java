package btw.api;

public class EntitySheep extends EntityAnimal {

    public static final float[][] fleeceColorTable = new float[][] {
        {1.0F, 1.0F, 1.0F},    // White
        {0.85F, 0.5F, 0.2F},   // Orange
        {0.7F, 0.3F, 0.85F},   // Magenta
        {0.4F, 0.6F, 0.85F},   // Light Blue
        {0.9F, 0.9F, 0.2F},    // Yellow
        {0.5F, 0.8F, 0.1F},    // Lime
        {0.95F, 0.5F, 0.65F},  // Pink
        {0.3F, 0.3F, 0.3F},    // Gray
        {0.6F, 0.6F, 0.6F},    // Light Gray
        {0.3F, 0.5F, 0.6F},    // Cyan
        {0.5F, 0.25F, 0.7F},   // Purple
        {0.2F, 0.3F, 0.7F},    // Blue
        {0.4F, 0.3F, 0.2F},    // Brown
        {0.4F, 0.5F, 0.2F},    // Green
        {0.6F, 0.2F, 0.2F},    // Red
        {0.1F, 0.1F, 0.1F}     // Black
    };

    public EntitySheep(World world) {
        super(world);
    }

    public int getMaxHealth() {
        return 8;
    }

    public boolean getSheared() {
        return false;
    }

    public void setSheared(boolean sheared) {}

    public int getFleeceColor() {
        return 0;
    }

    public void setFleeceColor(int color) {}

    public void entityInit() {}

    // Client-side animation methods
    public float func_70894_j(float partialTick) { return 0.0F; }
    public float func_70890_k(float partialTick) { return 0.0F; }
}
