package btw.api;

public class ModelWolf extends ModelBase {

    public ModelRenderer wolfHeadMain;
    public ModelRenderer wolfBody;
    public ModelRenderer wolfLeg1;
    public ModelRenderer wolfLeg2;
    public ModelRenderer wolfLeg3;
    public ModelRenderer wolfLeg4;
    ModelRenderer wolfTail;
    ModelRenderer wolfMane;

    public ModelWolf() {
        this.wolfHeadMain = new ModelRenderer(this, 0, 0);
        this.wolfBody = new ModelRenderer(this, 18, 14);
        this.wolfMane = new ModelRenderer(this, 21, 0);
        this.wolfLeg1 = new ModelRenderer(this, 0, 18);
        this.wolfLeg2 = new ModelRenderer(this, 0, 18);
        this.wolfLeg3 = new ModelRenderer(this, 0, 18);
        this.wolfLeg4 = new ModelRenderer(this, 0, 18);
        this.wolfTail = new ModelRenderer(this, 9, 18);
    }

    public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
        super.render(entity, f, f1, f2, f3, f4, f5);
        this.setRotationAngles(f, f1, f2, f3, f4, f5, entity);
    }

    public void setLivingAnimations(EntityLiving entityLiving, float f, float f1, float f2) {}

    public void setRotationAngles(float f, float f1, float f2, float f3, float f4, float f5, Entity entity) {
        super.setRotationAngles(f, f1, f2, f3, f4, f5, entity);
    }
}
