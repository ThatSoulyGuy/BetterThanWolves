package btw.modern;

import java.util.HashMap;
import java.util.Map;

public class TileEntityRenderer {

    private Map specialRendererMap = new HashMap();
    public static TileEntityRenderer instance = new TileEntityRenderer();

    public static double staticPlayerX;
    public static double staticPlayerY;
    public static double staticPlayerZ;

    public RenderEngine renderEngine;
    public World worldObj;
    public EntityLiving entityLivingPlayer;
    public float playerYaw;
    public float playerPitch;
    public double playerX;
    public double playerY;
    public double playerZ;

    private FontRenderer fontRenderer;

    public TileEntityRenderer() {}

    public TileEntitySpecialRenderer getSpecialRendererForClass(Class clazz) {
        TileEntitySpecialRenderer renderer = (TileEntitySpecialRenderer) this.specialRendererMap.get(clazz);
        if (renderer == null && clazz != TileEntity.class) {
            renderer = this.getSpecialRendererForClass(clazz.getSuperclass());
            this.specialRendererMap.put(clazz, renderer);
        }
        return renderer;
    }

    public boolean hasSpecialRenderer(TileEntity tileEntity) {
        return this.getSpecialRendererForEntity(tileEntity) != null;
    }

    public TileEntitySpecialRenderer getSpecialRendererForEntity(TileEntity tileEntity) {
        return tileEntity == null ? null : this.getSpecialRendererForClass(tileEntity.getClass());
    }

    public void cacheActiveRenderInfo(World world, RenderEngine renderEngine, FontRenderer fontRenderer, EntityLiving entityLiving, float partialTickTime) {
        this.worldObj = world;
        this.renderEngine = renderEngine;
        this.fontRenderer = fontRenderer;
        this.entityLivingPlayer = entityLiving;
    }

    public void renderTileEntity(TileEntity tileEntity, float partialTickTime) {}

    public void renderTileEntityAt(TileEntity tileEntity, double x, double y, double z, float partialTickTime) {
        TileEntitySpecialRenderer renderer = this.getSpecialRendererForEntity(tileEntity);
        if (renderer != null) {
            renderer.renderTileEntityAt(tileEntity, x, y, z, partialTickTime);
        }
    }

    public void setWorld(World world) {
        this.worldObj = world;
    }

    public FontRenderer getFontRenderer() {
        return this.fontRenderer;
    }

    public Map GetSpecialRendererMap() {
        return specialRendererMap;
    }
}
