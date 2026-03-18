package btw.api;

public class EntitySmokeFX extends EntityFX {

    float smokeParticleScale;

    public EntitySmokeFX(World world, double x, double y, double z, double motionX, double motionY, double motionZ) {
        this(world, x, y, z, motionX, motionY, motionZ, 1.0F);
    }

    public EntitySmokeFX(World world, double x, double y, double z, double motionX, double motionY, double motionZ, float scale) {
        super(world, x, y, z, 0.0D, 0.0D, 0.0D);
        this.smokeParticleScale = this.particleScale;
    }

    public void renderParticle(Tessellator tessellator, float partialTickTime, float rotX, float rotXZ, float rotZ, float rotYZ, float rotXY) {}

    public void onUpdate() {}
}
