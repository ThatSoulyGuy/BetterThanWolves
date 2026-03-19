package btw.modern;

public class EntityDiggingFX extends EntityFX {

    private Block blockInstance;

    public EntityDiggingFX(World world, double x, double y, double z, double motionX, double motionY, double motionZ, Block block, int side, int metadata, RenderEngine renderEngine) {
        super(world, x, y, z, motionX, motionY, motionZ);
        this.blockInstance = block;
    }

    public EntityDiggingFX func_70596_a(int x, int y, int z) {
        return this;
    }

    public EntityDiggingFX applyRenderColor(int metadata) {
        return this;
    }

    public int getFXLayer() {
        return 1;
    }

    public void renderParticle(Tessellator tessellator, float partialTickTime, float rotX, float rotXZ, float rotZ, float rotYZ, float rotXY) {}
}
