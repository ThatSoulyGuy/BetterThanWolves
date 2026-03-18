package btw.api;

public class ModelSlime extends ModelBase {

    ModelRenderer slimeBodies;
    ModelRenderer slimeRightEye;
    ModelRenderer slimeLeftEye;
    ModelRenderer slimeMouth;

    public ModelSlime(int par1) {
        this.slimeBodies = new ModelRenderer(this, 0, par1);

        if (par1 > 0) {
            this.slimeBodies = new ModelRenderer(this, 0, par1);
            this.slimeRightEye = new ModelRenderer(this, 32, 0);
            this.slimeLeftEye = new ModelRenderer(this, 32, 4);
            this.slimeMouth = new ModelRenderer(this, 32, 8);
        }
    }

    public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
        this.setRotationAngles(f, f1, f2, f3, f4, f5, entity);
    }
}
