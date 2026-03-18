package btw.api;

public class ModelChicken extends ModelBase {

    public ModelRenderer head;
    public ModelRenderer body;
    public ModelRenderer rightLeg;
    public ModelRenderer leftLeg;
    public ModelRenderer rightWing;
    public ModelRenderer leftWing;
    public ModelRenderer bill;
    public ModelRenderer chin;

    public ModelChicken() {
        this.head = new ModelRenderer(this, 0, 0);
        this.bill = new ModelRenderer(this, 14, 0);
        this.chin = new ModelRenderer(this, 14, 4);
        this.body = new ModelRenderer(this, 0, 9);
        this.rightLeg = new ModelRenderer(this, 26, 0);
        this.leftLeg = new ModelRenderer(this, 26, 0);
        this.rightWing = new ModelRenderer(this, 24, 13);
        this.leftWing = new ModelRenderer(this, 24, 13);
    }

    public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
        this.setRotationAngles(f, f1, f2, f3, f4, f5, entity);
    }

    public void setRotationAngles(float f, float f1, float f2, float f3, float f4, float f5, Entity entity) {}
}
