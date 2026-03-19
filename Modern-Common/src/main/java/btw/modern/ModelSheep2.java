package btw.modern;

public class ModelSheep2 extends ModelQuadruped {

    public ModelSheep2() {
        super(12, 0.0F);
    }

    public void setLivingAnimations(EntityLiving entityLiving, float f, float f1, float f2) {
        super.setLivingAnimations(entityLiving, f, f1, f2);
    }

    public void setRotationAngles(float f, float f1, float f2, float f3, float f4, float f5, Entity entity) {
        super.setRotationAngles(f, f1, f2, f3, f4, f5, entity);
    }
}
