package btw.api;

public class ModelOcelot extends ModelBase {

    ModelRenderer ocelotBackLeftLeg;
    ModelRenderer ocelotBackRightLeg;
    ModelRenderer ocelotFrontLeftLeg;
    ModelRenderer ocelotFrontRightLeg;
    ModelRenderer ocelotTail;
    ModelRenderer ocelotTail2;
    ModelRenderer ocelotHead;
    ModelRenderer ocelotBody;
    int field_78163_i = 1;

    public ModelOcelot() {
        this.ocelotHead = new ModelRenderer(this, "head");
        this.ocelotBody = new ModelRenderer(this, 20, 0);
        this.ocelotTail = new ModelRenderer(this, 0, 15);
        this.ocelotTail2 = new ModelRenderer(this, 4, 15);
        this.ocelotBackLeftLeg = new ModelRenderer(this, 8, 13);
        this.ocelotBackRightLeg = new ModelRenderer(this, 8, 13);
        this.ocelotFrontLeftLeg = new ModelRenderer(this, 40, 0);
        this.ocelotFrontRightLeg = new ModelRenderer(this, 40, 0);
    }

    public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
        this.setRotationAngles(f, f1, f2, f3, f4, f5, entity);
    }

    public void setRotationAngles(float f, float f1, float f2, float f3, float f4, float f5, Entity entity) {}

    public void setLivingAnimations(EntityLiving entityLiving, float f, float f1, float f2) {}
}
