package btw.modern;

public class RenderManager {

    /** The static instance of RenderManager. */
    public static RenderManager instance = new RenderManager();

    /** FC entity class → FC Render instance, populated by AddEntityRenderer. */
    private static final java.util.Map<Class<?>, Render> entityRenderers = new java.util.LinkedHashMap<>();

    // 1.5.2 RenderManager ctor registers RenderItem for EntityItem — the FC tile
    // renderers (tool-placed, campfire, furnace brick, wicker basket) draw their
    // embedded items through renderEntityWithPosYaw, which looks it up here.
    static {
        // AddEntityRenderer (not a raw map put) so the renderer gets setRenderManager(instance),
        // matching vanilla RenderManager's ctor loop — RenderItem.renderDroppedItem reads
        // renderManager.playerViewY.
        AddEntityRenderer(EntityItem.class, new RenderItem());
        // 1.5.2 RenderManager ctor also registered RenderXPOrb for EntityXPOrb. FC's
        // ClientAddEntityRenderers only covers FC entity subclasses, so the frozen vanilla
        // EntityXPOrb (fc_xp_orb) had no renderer and fell back to FCEntityRenderer's debug
        // box. Register it here so the capture pipeline finds an FC Render.
        AddEntityRenderer(EntityXPOrb.class, new RenderXPOrb());
    }

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

    // 1.5.2 RenderManager.getEntityClassRenderObject — walks the class hierarchy to find the
    // registered renderer. Unlike 1.5.2 we do not cache null lookups: FCEntityRenderer copies
    // the map into a ConcurrentHashMap, which rejects null values.
    public Render getEntityClassRenderObject(Class entityClass) {
        Render render = entityRenderers.get(entityClass);

        if (render == null && entityClass != null && entityClass != Entity.class && entityClass.getSuperclass() != null) {
            render = this.getEntityClassRenderObject(entityClass.getSuperclass());

            if (render != null) {
                entityRenderers.put(entityClass, render);
            }
        }

        return render;
    }

    // 1.5.2 RenderManager.getEntityRenderObject — renderer lookup by entity instance.
    public Render getEntityRenderObject(Entity entity) {
        return this.getEntityClassRenderObject(entity.getClass());
    }

    public void renderEntity(Entity entity, float partialTickTime) {}

    // 1.5.2 RenderManager.renderEntityWithPosYaw — FCTileEntityToolPlacedRenderer:163,
    // FCTileEntityCampfireRenderer:57, FCTileEntityFurnaceBrickRenderer:102 draw their embedded
    // EntityItems/tools through this. 1.5.2 also gated on renderEngine != null; the capture
    // bridge has no RenderEngine, so only the renderer lookup gates here.
    public void renderEntityWithPosYaw(Entity entity, double x, double y, double z, float yaw, float partialTickTime) {
        Render render = this.getEntityRenderObject(entity);

        if (render != null) {
            render.doRender(entity, x, y, z, yaw, partialTickTime);
            render.doRenderShadowAndFire(entity, x, y, z, yaw, partialTickTime);
        }
    }

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

    // 1.5.2 RenderManager.AddEntityRenderer (FCMOD addition) — also hands the renderer its
    // RenderManager so Render.loadTexture/getFontRendererFromRenderManager work.
    public static void AddEntityRenderer(Class entityClass, Render entityRenderer) {
        entityRenderers.put(entityClass, entityRenderer);

        entityRenderer.setRenderManager(instance);
    }

    /** Returns the FC entity renderer map (class → Render). */
    public java.util.Map<Class<?>, Render> getEntityRenderMap() {
        return entityRenderers;
    }
}
