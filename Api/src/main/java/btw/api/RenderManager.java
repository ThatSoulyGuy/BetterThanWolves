package btw.api;

public class RenderManager {

    /** The static instance of RenderManager. */
    public static RenderManager instance = new RenderManager();

    public static double renderPosX;
    public static double renderPosY;
    public static double renderPosZ;
    public RenderEngine renderEngine;
    public World worldObj;
    public EntityLiving livingPlayer;
    public EntityLiving field_96451_i;
    public float playerViewY;
    public float playerViewX;
    public GameSettings options;
    public double viewerPosX;
    public double viewerPosY;
    public double viewerPosZ;
    public static boolean field_85095_o = false;

    protected RenderManager() {}

    public Render getEntityClassRenderObject(Class entityClass) {
        return null;
    }

    public Render getEntityRenderObject(Entity entity) {
        return null;
    }

    public void renderEntity(Entity entity, float partialTickTime) {}

    public void renderEntityWithPosYaw(Entity entity, double x, double y, double z, float yaw, float partialTickTime) {}

    public void set(World world) {
        this.worldObj = world;
    }

    public double getDistanceToCamera(double x, double y, double z) {
        double dx = x - this.viewerPosX;
        double dy = y - this.viewerPosY;
        double dz = z - this.viewerPosZ;
        return dx * dx + dy * dy + dz * dz;
    }

    public FontRenderer getFontRenderer() {
        return null;
    }

    public void updateIcons(IconRegister iconRegister) {}

    public static void AddEntityRenderer(Class entityClass, Render entityRenderer) {}
}
