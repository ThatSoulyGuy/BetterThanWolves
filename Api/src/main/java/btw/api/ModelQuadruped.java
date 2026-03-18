package btw.api;

public class ModelQuadruped extends ModelBase {

    public ModelRenderer head = new ModelRenderer(this, 0, 0);
    public ModelRenderer body;
    public ModelRenderer leg1;
    public ModelRenderer leg2;
    public ModelRenderer leg3;
    public ModelRenderer leg4;
    public float field_78145_g = 8.0F;
    public float field_78151_h = 4.0F;

    public ModelQuadruped(int legHeight, float scale) {
        this.head.setRotationPoint(0.0F, (float)(18 - legHeight), -6.0F);
        this.body = new ModelRenderer(this, 28, 8);
        this.body.setRotationPoint(0.0F, (float)(17 - legHeight), 2.0F);
        this.leg1 = new ModelRenderer(this, 0, 16);
        this.leg1.setRotationPoint(-3.0F, (float)(24 - legHeight), 7.0F);
        this.leg2 = new ModelRenderer(this, 0, 16);
        this.leg2.setRotationPoint(3.0F, (float)(24 - legHeight), 7.0F);
        this.leg3 = new ModelRenderer(this, 0, 16);
        this.leg3.setRotationPoint(-3.0F, (float)(24 - legHeight), -5.0F);
        this.leg4 = new ModelRenderer(this, 0, 16);
        this.leg4.setRotationPoint(3.0F, (float)(24 - legHeight), -5.0F);
    }

    public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
        this.setRotationAngles(f, f1, f2, f3, f4, f5, entity);
    }

    public void setRotationAngles(float f, float f1, float f2, float f3, float f4, float f5, Entity entity) {}
}
