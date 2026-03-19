package btw.modern;

public class EntityItem extends Entity {

    public int age;
    public int delayBeforeCanPickup;
    public float hoverStart;
    private ItemStack entityItem;

    public EntityItem(World world) {
        super(world);
    }

    public EntityItem(World world, double x, double y, double z, ItemStack stack) {
        super(world);
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        this.entityItem = stack;
    }

    public ItemStack getEntityItem() {
        return this.entityItem;
    }

    public void setEntityItemStack(ItemStack stack) {
        this.entityItem = stack;
    }

    public void entityInit() {}

    public static boolean InstallationIntegrityTestEntityItem() { return true; }
}
