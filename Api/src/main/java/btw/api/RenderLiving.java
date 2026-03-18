package btw.api;

public class RenderLiving extends Render {

    protected ModelBase mainModel;
    protected ModelBase renderPassModel;

    public RenderLiving(ModelBase model, float shadowSize) {
        this.mainModel = model;
        this.shadowSize = shadowSize;
    }

    public void setRenderPassModel(ModelBase model) {
        this.renderPassModel = model;
    }

    public void doRenderLiving(EntityLiving entityLiving, double x, double y, double z, float yaw, float partialTickTime) {}

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTickTime) {
        this.doRenderLiving((EntityLiving) entity, x, y, z, yaw, partialTickTime);
    }

    protected void renderModel(EntityLiving entityLiving, float f, float f1, float f2, float f3, float f4, float f5) {}

    protected void renderLivingAt(EntityLiving entityLiving, double x, double y, double z) {}

    protected void rotateCorpse(EntityLiving entityLiving, float f, float f1, float f2) {}

    protected float renderSwingProgress(EntityLiving entityLiving, float partialTickTime) {
        return 0.0F;
    }

    protected float handleRotationFloat(EntityLiving entityLiving, float partialTickTime) {
        return 0.0F;
    }

    protected void renderEquippedItems(EntityLiving entityLiving, float partialTickTime) {}

    protected void renderArrowsStuckInEntity(EntityLiving entityLiving, float partialTickTime) {}

    protected int inheritRenderPass(EntityLiving entityLiving, int pass, float partialTickTime) {
        return -1;
    }

    protected int shouldRenderPass(EntityLiving entityLiving, int pass, float partialTickTime) {
        return -1;
    }

    protected void func_82408_c(EntityLiving entityLiving, int pass, float partialTickTime) {}

    protected float getDeathMaxRotation(EntityLiving entityLiving) {
        return 90.0F;
    }

    protected int getColorMultiplier(EntityLiving entityLiving, float lightBrightness, float partialTickTime) {
        return 0;
    }

    protected void preRenderCallback(EntityLiving entityLiving, float partialTickTime) {}

    protected void passSpecialRender(EntityLiving entityLiving, double x, double y, double z) {}

    protected void renderLivingLabel(EntityLiving entityLiving, String label, double x, double y, double z, int maxDistance) {}

    protected void func_98190_a(EntityLiving entityLiving) {}
}
