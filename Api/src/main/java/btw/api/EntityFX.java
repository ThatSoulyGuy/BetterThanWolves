package btw.api;

public class EntityFX extends Entity {

    protected int particleTextureIndexX;
    protected int particleTextureIndexY;
    protected float particleTextureJitterX;
    protected float particleTextureJitterY;
    public int particleAge;
    public int particleMaxAge;
    public float particleScale;
    public float particleGravity;

    /** The red amount of color. Used as a percentage, 1.0 = 255 and 0.0 = 0. */
    protected float particleRed;

    /** The green amount of color. */
    protected float particleGreen;

    /** The blue amount of color. */
    protected float particleBlue;

    /** Particle alpha */
    protected float particleAlpha;

    /** The icon field from which the given particle pulls its texture. */
    protected Icon particleIcon;

    public static double interpPosX;
    public static double interpPosY;
    public static double interpPosZ;

    protected EntityFX(World world, double x, double y, double z) {
        super(world);
        this.particleAlpha = 1.0F;
    }

    public EntityFX(World world, double x, double y, double z, double motionX, double motionY, double motionZ) {
        this(world, x, y, z);
    }

    public EntityFX multiplyVelocity(float factor) {
        return this;
    }

    public EntityFX multipleParticleScaleBy(float factor) {
        return this;
    }

    public void setRBGColorF(float r, float g, float b) {
        this.particleRed = r;
        this.particleGreen = g;
        this.particleBlue = b;
    }

    public void setAlphaF(float alpha) {
        this.particleAlpha = alpha;
    }

    public float getRedColorF() {
        return this.particleRed;
    }

    public float getGreenColorF() {
        return this.particleGreen;
    }

    public float getBlueColorF() {
        return this.particleBlue;
    }

    public void renderParticle(Tessellator tessellator, float partialTickTime, float rotX, float rotXZ, float rotZ, float rotYZ, float rotXY) {}

    public int getFXLayer() {
        return 0;
    }

    public void setParticleIcon(RenderEngine renderEngine, Icon icon) {
        this.particleIcon = icon;
    }

    public void setParticleTextureIndex(int index) {
        this.particleTextureIndexX = index % 16;
        this.particleTextureIndexY = index / 16;
    }

    public void nextTextureIndexX() {
        ++this.particleTextureIndexX;
    }

}
