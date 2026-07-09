package btw.modern;

import java.util.Random;

/**
 * 1.5.2 RenderItem (vanilla/client RenderItem.java doRender/doRenderItem/
 * renderDroppedItem, FCMOD version) — draws the EntityItems FC tile renderers
 * embed: FCTileEntityToolPlacedRenderer:163, FCTileEntityCampfireRenderer:57,
 * FCTileEntityFurnaceBrickRenderer:102, FCTileEntityBasketWickerRenderer:75
 * all route through RenderManager.renderEntityWithPosYaw. Registered for
 * EntityItem in RenderManager's static initializer.
 *
 * <p>Adaptations from vanilla (dropped world items are MODERN entities and
 * never reach this class — only FC tile-embedded display items do):
 * fancyGraphics comes solely from m_bForceFancyItemRender (GameSettings is
 * not bridged; the FC tile renderers force it around their calls); the
 * enchantment-glint pass and GL12.GL_RESCALE_NORMAL state are omitted
 * (renderEngine texture-matrix effects, cosmetic); the missing-icon fallback
 * (renderEngine.getMissingIcon) becomes a skip.</p>
 */
public class RenderItem extends Render {

    public static boolean m_bForceFancyItemRender = false;

    /** 1.5.2 RenderItem.renderInFrame — item-frame pose flag. */
    public static boolean renderInFrame = false;

    private final RenderBlocks itemRenderBlocks = new RenderBlocks();
    private final Random random = new Random();
    public boolean renderWithColor = true;

    public RenderItem() {
        this.shadowSize = 0.15F;
    }

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTickTime) {
        this.doRenderItem((EntityItem) entity, x, y, z, yaw, partialTickTime);
    }

    public void doRenderItem(EntityItem entityItem, double x, double y, double z, float yaw, float partialTick) {
        this.random.setSeed(187L);
        ItemStack stack = entityItem.getEntityItem();

        if (stack != null && stack.getItem() != null) {
            GL11.glPushMatrix();
            float bob = MathHelper.sin(((float) entityItem.age + partialTick) / 10.0F + entityItem.hoverStart) * 0.1F + 0.1F;
            float spin = (((float) entityItem.age + partialTick) / 20.0F + entityItem.hoverStart) * (180F / (float) Math.PI);
            byte copies = 1;

            if (stack.stackSize > 1) copies = 2;
            if (stack.stackSize > 5) copies = 3;
            if (stack.stackSize > 20) copies = 4;
            if (stack.stackSize > 40) copies = 5;

            GL11.glTranslatef((float) x, (float) y + bob, (float) z);

            // FCMOD: Changed (client only) — block-form check via DoesItemRenderAsBlock
            if (stack.getItemSpriteNumber() == 0 && Block.blocksList[stack.itemID] != null
                    && Block.blocksList[stack.itemID].DoesItemRenderAsBlock(stack.getItemDamage())) {
            // END FCMOD
                Block block = Block.blocksList[stack.itemID];
                GL11.glRotatef(spin, 0.0F, 1.0F, 0.0F);

                if (renderInFrame) {
                    GL11.glScalef(1.25F, 1.25F, 1.25F);
                    GL11.glTranslatef(0.0F, 0.05F, 0.0F);
                    GL11.glRotatef(-90.0F, 0.0F, 1.0F, 0.0F);
                }

                this.loadTexture("/terrain.png");
                float scale = 0.25F;
                int renderType = block.getRenderType();

                if (renderType == 1 || renderType == 19 || renderType == 12 || renderType == 2) {
                    scale = 0.5F;
                }

                GL11.glScalef(scale, scale, scale);

                for (int copy = 0; copy < copies; ++copy) {
                    GL11.glPushMatrix();

                    if (copy > 0) {
                        float ox = (this.random.nextFloat() * 2.0F - 1.0F) * 0.2F / scale;
                        float oy = (this.random.nextFloat() * 2.0F - 1.0F) * 0.2F / scale;
                        float oz = (this.random.nextFloat() * 2.0F - 1.0F) * 0.2F / scale;
                        GL11.glTranslatef(ox, oy, oz);
                    }

                    this.itemRenderBlocks.renderBlockAsItem(block, stack.getItemDamage(), 1.0F);
                    GL11.glPopMatrix();
                }
            } else if (stack.getItem().requiresMultipleRenderPasses()) {
                for (int pass = 0; pass <= 1; ++pass) {
                    this.random.setSeed(187L);
                    Icon icon = stack.getItem().getIconFromDamageForRenderPass(stack.getItemDamage(), pass);

                    if (this.renderWithColor) {
                        int color = Item.itemsList[stack.itemID].getColorFromItemStack(stack, pass);
                        float r = (float) (color >> 16 & 255) / 255.0F;
                        float g = (float) (color >> 8 & 255) / 255.0F;
                        float b = (float) (color & 255) / 255.0F;
                        this.renderDroppedItem(entityItem, icon, copies, partialTick, r, g, b);
                    } else {
                        this.renderDroppedItem(entityItem, icon, copies, partialTick, 1.0F, 1.0F, 1.0F);
                    }
                }
            } else {
                if (renderInFrame) {
                    GL11.glScalef(0.5128205F, 0.5128205F, 0.5128205F);
                    GL11.glTranslatef(0.0F, -0.05F, 0.0F);
                } else {
                    GL11.glScalef(0.5F, 0.5F, 0.5F);
                }

                Icon icon = stack.getIconIndex();

                if (this.renderWithColor) {
                    int color = Item.itemsList[stack.itemID].getColorFromItemStack(stack, 0);
                    float r = (float) (color >> 16 & 255) / 255.0F;
                    float g = (float) (color >> 8 & 255) / 255.0F;
                    float b = (float) (color & 255) / 255.0F;
                    this.renderDroppedItem(entityItem, icon, copies, partialTick, r, g, b);
                } else {
                    this.renderDroppedItem(entityItem, icon, copies, partialTick, 1.0F, 1.0F, 1.0F);
                }
            }

            GL11.glPopMatrix();
        }
    }

    private void renderDroppedItem(EntityItem entityItem, Icon icon, int copies, float partialTick, float r, float g, float b) {
        Tessellator tess = Tessellator.instance;

        if (icon == null) {
            // Adaptation: no renderEngine.getMissingIcon in the bridge — skip.
            return;
        }

        float maxU = icon.getMinU();
        float minU = icon.getMaxU();
        float maxV = icon.getMinV();
        float minV = icon.getMaxV();
        float halfWidth = 0.5F;
        float quarterHeight = 0.25F;

        // FCMOD: Changed (client only) — fancy render forced by FC tile renderers;
        // GameSettings.fancyGraphics is not bridged (see class javadoc).
        if (m_bForceFancyItemRender) {
        // END FCMOD
            GL11.glPushMatrix();

            if (renderInFrame) {
                GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
            } else {
                GL11.glRotatef((((float) entityItem.age + partialTick) / 20.0F + entityItem.hoverStart) * (180F / (float) Math.PI), 0.0F, 1.0F, 0.0F);
            }

            float thickness = 0.0625F;
            float gap = 0.021875F;
            ItemStack stack = entityItem.getEntityItem();
            int count = stack.stackSize;
            byte cards;

            if (count < 2) {
                cards = 1;
            } else if (count < 16) {
                cards = 2;
            } else if (count < 32) {
                cards = 3;
            } else {
                cards = 4;
            }

            GL11.glTranslatef(-halfWidth, -quarterHeight, -((thickness + gap) * (float) cards / 2.0F));

            for (int card = 0; card < cards; ++card) {
                GL11.glTranslatef(0.0F, 0.0F, thickness + gap);

                if (stack.getItemSpriteNumber() == 0 && Block.blocksList[stack.itemID] != null) {
                    this.loadTexture("/terrain.png");
                } else {
                    this.loadTexture("/gui/items.png");
                }

                GL11.glColor4f(r, g, b, 1.0F);
                ItemRenderer.renderItemIn2D(tess, minU, maxV, maxU, minV, icon.getSheetWidth(), icon.getSheetHeight(), thickness);
                // Adaptation: enchantment glint pass omitted (renderEngine
                // texture-matrix effect; cosmetic).
            }

            GL11.glPopMatrix();
        } else {
            for (int copy = 0; copy < copies; ++copy) {
                GL11.glPushMatrix();

                if (copy > 0) {
                    float ox = (this.random.nextFloat() * 2.0F - 1.0F) * 0.3F;
                    float oy = (this.random.nextFloat() * 2.0F - 1.0F) * 0.3F;
                    float oz = (this.random.nextFloat() * 2.0F - 1.0F) * 0.3F;
                    GL11.glTranslatef(ox, oy, oz);
                }

                if (!renderInFrame && this.renderManager != null) {
                    GL11.glRotatef(180.0F - this.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
                }

                GL11.glColor4f(r, g, b, 1.0F);
                tess.startDrawingQuads();
                tess.setNormal(0.0F, 1.0F, 0.0F);
                tess.addVertexWithUV(0.0F - halfWidth, 0.0F - quarterHeight, 0.0D, (double) maxU, (double) minV);
                tess.addVertexWithUV((float) 1 - halfWidth, 0.0F - quarterHeight, 0.0D, (double) minU, (double) minV);
                tess.addVertexWithUV((float) 1 - halfWidth, 1.0F - quarterHeight, 0.0D, (double) minU, (double) maxV);
                tess.addVertexWithUV(0.0F - halfWidth, 1.0F - quarterHeight, 0.0D, (double) maxU, (double) maxV);
                tess.draw();
                GL11.glPopMatrix();
            }
        }
    }
}
