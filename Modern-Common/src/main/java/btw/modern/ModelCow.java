package btw.modern;

public class ModelCow extends ModelQuadruped {
    private float m_fHeadRotation;

    public ModelCow() {
        super(12, 0.0F);
        this.head = new ModelRenderer(this, 0, 0);
        this.head.addBox(-4.0F, -4.0F, -6.0F, 8, 8, 6, 0.0F);
        this.head.setRotationPoint(0.0F, 4.0F, -8.0F);
        this.head.setTextureOffset(22, 0).addBox(-5.0F, -5.0F, -4.0F, 1, 3, 1, 0.0F);
        this.head.setTextureOffset(22, 0).addBox(4.0F, -5.0F, -4.0F, 1, 3, 1, 0.0F);
        this.body = new ModelRenderer(this, 18, 4);
        this.body.addBox(-6.0F, -10.0F, -7.0F, 12, 18, 10, 0.0F);
        this.body.setRotationPoint(0.0F, 5.0F, 2.0F);
        this.body.setTextureOffset(52, 0).addBox(-2.0F, 2.0F, -8.0F, 4, 6, 1);
        --this.leg1.rotationPointX;
        ++this.leg2.rotationPointX;
        --this.leg3.rotationPointX;
        ++this.leg4.rotationPointX;
        --this.leg3.rotationPointZ;
        --this.leg4.rotationPointZ;
    }

    public void setLivingAnimations(EntityLiving entity, float par2, float par3, float fPartialTick) {
        super.setLivingAnimations(entity, par2, par3, fPartialTick);
        try {
            java.lang.reflect.Method m = entity.getClass().getMethod("GetGrazeHeadVerticalOffset", float.class);
            java.lang.reflect.Method m2 = entity.getClass().getMethod("GetGrazeHeadRotation", float.class);
            head.rotationPointY = 4F + ((Float) m.invoke(entity, fPartialTick)) * 4F;
            m_fHeadRotation = ((Float) m2.invoke(entity, fPartialTick));
        } catch (Exception ignored) {}
    }

    public void setRotationAngles(float f, float f1, float f2, float f3, float f4, float f5, Entity entity) {
        super.setRotationAngles(f, f1, f2, f3, f4, f5, entity);
        head.rotateAngleX = m_fHeadRotation;
    }
}
