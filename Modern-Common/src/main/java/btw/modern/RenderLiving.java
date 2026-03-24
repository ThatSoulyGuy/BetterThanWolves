package btw.modern;

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

    /**
     * Core living entity render pipeline matching MC 1.5.2's RenderLiving.
     * Sets up GL transforms, calls renderModel which renders the mainModel.
     * FC subclasses override specific steps (rotateCorpse, preRenderCallback, etc.)
     */
    public void doRenderLiving(EntityLiving entityLiving, double x, double y, double z, float yaw, float partialTickTime) {
        GL11.glPushMatrix();

        // Position
        renderLivingAt(entityLiving, x, y, z);

        // Body rotation
        float bodyYaw = interpolateRotation(entityLiving.prevRenderYawOffset, entityLiving.renderYawOffset, partialTickTime);
        float headYaw = interpolateRotation(entityLiving.prevRotationYawHead, entityLiving.rotationYawHead, partialTickTime);
        float headPitch = entityLiving.prevRotationPitch + (entityLiving.rotationPitch - entityLiving.prevRotationPitch) * partialTickTime;

        GL11.glRotatef(180.0F - bodyYaw, 0.0F, 1.0F, 0.0F);

        // Death rotation
        rotateCorpse(entityLiving, bodyYaw, yaw, partialTickTime);

        float scale = 0.0625F;

        // Limb animation
        float limbSwingAmount = entityLiving.prevLimbYaw + (entityLiving.limbYaw - entityLiving.prevLimbYaw) * partialTickTime;
        float limbSwing = entityLiving.limbSwing - entityLiving.limbYaw * (1.0F - partialTickTime);
        if (limbSwingAmount > 1.0F) limbSwingAmount = 1.0F;

        float ageInTicks = (float) entityLiving.ticksExisted + partialTickTime;

        // MC 1.5.2 models are upside-down. The original GL pipeline flipped them.
        // In our capture pipeline, we apply the same flip so model geometry is
        // right-side up, then offset so feet are at Y=0.
        GL11.glScalef(1.0F, -1.0F, -1.0F);
        GL11.glTranslatef(0.0F, -1.5078125F, 0.0F);

        preRenderCallback(entityLiving, partialTickTime);

        // Load texture — must happen before rendering so Tessellator records it
        String tex = entityLiving.getTexture();
        if (tex != null && !tex.isEmpty()) {
            loadTexture(tex);
        }

        // Render the model
        renderModel(entityLiving, limbSwing, limbSwingAmount, ageInTicks,
                headYaw - bodyYaw, headPitch, scale);

        GL11.glPopMatrix();
    }

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTickTime) {
        this.doRenderLiving((EntityLiving) entity, x, y, z, yaw, partialTickTime);
    }

    protected void renderModel(EntityLiving entityLiving, float limbSwing, float limbSwingAmount,
                                float ageInTicks, float headYaw, float headPitch, float scale) {
        if (mainModel != null) {
            mainModel.render(entityLiving, limbSwing, limbSwingAmount, ageInTicks,
                    headYaw, headPitch, scale);
        }
    }

    protected void renderLivingAt(EntityLiving entityLiving, double x, double y, double z) {
        GL11.glTranslatef((float) x, (float) y, (float) z);
    }

    protected void rotateCorpse(EntityLiving entityLiving, float bodyYaw, float yaw, float partialTickTime) {}

    private float interpolateRotation(float prev, float current, float partial) {
        float diff = current - prev;
        while (diff < -180.0F) diff += 360.0F;
        while (diff >= 180.0F) diff -= 360.0F;
        return prev + partial * diff;
    }

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
