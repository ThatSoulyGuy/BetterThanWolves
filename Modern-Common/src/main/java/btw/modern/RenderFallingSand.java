package btw.modern;

/**
 * 1.5.2 (FCMOD) RenderFallingSand (vanilla/client RenderFallingSand.java:31-133) —
 * registered by FCBetterThanWolves.ClientAddEntityRenderers for FCEntityFallingBlock;
 * FCEntityRenderer invokes {@link #doRender}. Records the falling block's geometry
 * through the recording Tessellator via Block.RenderFallingBlock.
 *
 * <p>Adaptations from vanilla: the BlockAnvil/BlockDragonEgg special branches are
 * omitted — vanilla falling blocks are handled by the modern engine, only FC falling
 * entities route here — and the loadTexture/GL lighting calls are dropped (textures
 * come from the modern pipeline; GL11 shim calls are no-ops).</p>
 */
public class RenderFallingSand extends Render {

    private final RenderBlocks sandRenderBlocks = new RenderBlocks();

    public RenderFallingSand() {
        this.shadowSize = 0.5F;
    }

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTickTime) {
        this.doRenderFallingSand((EntityFallingSand) entity, x, y, z, yaw, partialTickTime);
    }

    public void doRenderFallingSand(EntityFallingSand entity, double x, double y, double z, float yaw, float partialTickTime) {
        World world = entity.worldObj;
        Block block = Block.blocksList[entity.blockID];

        // FCMOD: Changed (client only) — render gated on ShouldRenderWhileFalling
        if (ShouldRender(world, entity)) {
            shadowSize = 0.5F;

            GL11.glPushMatrix();
            GL11.glTranslatef((float) x, (float) y, (float) z);

            if (block != null) {
                sandRenderBlocks.blockAccess = world;

                Tessellator.instance.startDrawingQuads();
                Tessellator.instance.setTranslation(
                        -MathHelper.floor_double(entity.posX) - 0.5D,
                        -MathHelper.floor_double(entity.posY) - 0.5D,
                        -MathHelper.floor_double(entity.posZ) - 0.5D);

                block.m_currentBlockRenderer = sandRenderBlocks;

                block.RenderFallingBlock(sandRenderBlocks,
                        MathHelper.floor_double(entity.posX),
                        MathHelper.floor_double(entity.posY),
                        MathHelper.floor_double(entity.posZ),
                        entity.metadata);

                Tessellator.instance.setTranslation(0D, 0D, 0D);
                Tessellator.instance.draw();
            }

            GL11.glPopMatrix();
        } else {
            // FCMOD: Added (client only)
            shadowSize = 0F;
        }
    }

    // FCMOD: Added (client only) — 1.5.2 RenderFallingSand.ShouldRender
    private boolean ShouldRender(World world, EntityFallingSand entity) {
        Block fallingBlock = Block.blocksList[entity.blockID];
        if (fallingBlock != null) {
            return fallingBlock.ShouldRenderWhileFalling(world, entity);
        }
        return false;
    }
}
