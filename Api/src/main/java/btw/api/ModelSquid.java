package btw.api;

public class ModelSquid extends ModelBase {

    public ModelRenderer squidBody;
    public ModelRenderer[] squidTentacles = new ModelRenderer[8];

    public ModelSquid() {
        this.squidBody = new ModelRenderer(this, 0, 0);

        for (int i = 0; i < this.squidTentacles.length; ++i) {
            this.squidTentacles[i] = new ModelRenderer(this, 48, 0);
        }
    }

    public void setRotationAngles(float f, float f1, float f2, float f3, float f4, float f5, Entity entity) {}

    public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
        this.setRotationAngles(f, f1, f2, f3, f4, f5, entity);
    }
}
