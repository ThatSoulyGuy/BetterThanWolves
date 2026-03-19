package btw.modern;

public class EntityItem extends Entity {

    public int age;
    public int delayBeforeCanPickup;
    public float hoverStart;

    public EntityItem(World world) {
        super(world);
    }

    public EntityItem(World world, double x, double y, double z, ItemStack stack) {
        super(world);
    }

    public ItemStack getEntityItem() {
        return null;
    }

    public void setEntityItemStack(ItemStack stack) {}

    public void entityInit() {}

    public static boolean InstallationIntegrityTestEntityItem() { return true; }
}
