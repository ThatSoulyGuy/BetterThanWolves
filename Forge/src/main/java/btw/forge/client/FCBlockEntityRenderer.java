package btw.forge.client;

import btw.forge.ItemStackHelper;
import btw.forge.ProxyBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;

/**
 * Renders FC tile entity visuals: cooking items on campfires/furnaces,
 * placed tools, basket lids, etc.
 *
 * <p>Instead of running FC's TileEntitySpecialRenderer (which requires
 * a full RenderManager bridge), this reads FC tile entity state directly
 * and renders items using MC 1.20.1's native ItemRenderer.</p>
 */
public class FCBlockEntityRenderer implements BlockEntityRenderer<ProxyBlockEntity> {

    @Override
    public boolean shouldRenderOffScreen(ProxyBlockEntity blockEntity) {
        // Always render — FC tile entities may have items/effects that extend beyond the block
        return true;
    }

    @Override
    public int getViewDistance() {
        return 64;
    }

    private static final Logger LOGGER = LogManager.getLogger("BTW-BERenderer");
    private final ItemRenderer itemRenderer;

    public FCBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
        this.itemRenderer = ctx.getItemRenderer();
    }

    @Override
    public void render(ProxyBlockEntity blockEntity, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        btw.modern.TileEntity fcTe = blockEntity.getFcTileEntity();
        if (fcTe == null) return;
        // FCMOD-INTEGRATION(1.20.1): fc_crack_4b: 4.B laid the breaking crack on the block's own RenderBlockWithTexture only, never on tile-entity geometry
        bufferSource = FCBreakingCrack.withoutCrumbling(bufferSource);

        String className = fcTe.getClass().getSimpleName();

        // === Generalized FC TESR rendering via vertex capture ===
        // Look up FC's TileEntitySpecialRenderer for this tile entity class.
        // If one exists, run it with GL11 matrix tracking enabled, capture
        // vertices from Tessellator, and render them as MC 1.20.1 quads.
        btw.modern.TileEntitySpecialRenderer fcTesr =
                btw.modern.TileEntityRenderer.instance.getSpecialRendererForClass(fcTe.getClass());
        if (fcTesr != null) {
            // FCTileEntityCampfireRenderer / FCTileEntityFurnaceBrickRenderer draw nothing but the cook
            // stack, which renderCampfireCookItem / renderFurnaceBrickCookItem below draw natively with
            // the 1.20.1 item model. Now that their EntityItem can be built (client-world provider),
            // running them too would draw the item twice, the captured copy without a 1.20.1 texture.
            if (!drawsCookStackNatively(className)) {
                // The placed tool is drawn by FC's own TESR (geometry), textured with the tool's
                // 1.20.1 item sprite: see toolPlacedItemSprite.
                TextureAtlasSprite itemSprite = "FCTileEntityToolPlaced".equals(className)
                        ? toolPlacedItemSprite(fcTe) : null;
                renderFcTESR(fcTesr, fcTe, blockEntity, partialTick, poseStack, bufferSource, packedLight, itemSprite);
            }
        } else if (loggedPositions.add(blockEntity.getBlockPos().hashCode() + 77777)) {
            LOGGER.debug("No FC TESR for {} (class hierarchy: {} → {})",
                    fcTe.getClass().getSimpleName(),
                    fcTe.getClass().getSuperclass() != null ? fcTe.getClass().getSuperclass().getSimpleName() : "none",
                    fcTe.getClass().getSuperclass() != null && fcTe.getClass().getSuperclass().getSuperclass() != null
                            ? fcTe.getClass().getSuperclass().getSuperclass().getSimpleName() : "none");
        }

        // Try rendering cook stack for any tile entity that has one
        btw.modern.ItemStack cookStack = getCookStack(fcTe);
        if (cookStack != null) {
            if (className.contains("Campfire")) {
                renderCampfireCookItem(cookStack, poseStack, bufferSource, packedLight, packedOverlay, blockEntity);
            } else if (className.contains("FurnaceBrick")) {
                renderFurnaceBrickCookItem(cookStack, poseStack, bufferSource, packedLight, packedOverlay, blockEntity);
            } else {
                renderGenericCookItem(cookStack, poseStack, bufferSource, packedLight, packedOverlay);
            }
        }

        // The wicker basket is registered with the lid-only FCTileEntityBasketRenderer (see
        // BTWClientEvents); its stored item is drawn natively here.
        if ("FCTileEntityBasketWicker".equals(className)) {
            renderBasketStorageStack(fcTe, blockEntity, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        }

        // FCMOD-INTEGRATION(1.20.1): fc_particles: no particles from the renderer; FC's own display ticks give a cooking vessel's and a campfire's 4.B smoke (FCBlockVessel / FCBlockCampfire.randomDisplayTick through FcParticles)
    }

    /** True for TEs whose FC TESR draws only the cook stack that render() draws natively. */
    private static boolean drawsCookStackNatively(String className) {
        return className.contains("Campfire") || className.contains("FurnaceBrick");
    }

    /**
     * Runs an FC TileEntitySpecialRenderer with GL11 matrix tracking and
     * Tessellator vertex capture, then renders the captured quads using
     * MC 1.20.1's rendering system.
     *
     * @param atlasSprite if non-null, every captured quad is drawn from this block-atlas sprite
     *                    (its 0..1 icon UVs remapped into the sprite) instead of a standalone file
     */
    private void renderFcTESR(btw.modern.TileEntitySpecialRenderer fcTesr,
                               btw.modern.TileEntity fcTe,
                               ProxyBlockEntity blockEntity,
                               float partialTick,
                               PoseStack poseStack,
                               MultiBufferSource bufferSource,
                               int packedLight,
                               @Nullable TextureAtlasSprite atlasSprite) {
        btw.modern.Tessellator tess = btw.modern.Tessellator.instance;
        // Start every capture from a clean state (triage R1): enableMatrixTracking resets the matrix
        // stack to identity; GL colour white as 1.5.2 TileEntityRenderer.renderTileEntity set before
        // each TESR (TileEntityRenderer.java:155); no tessellator translation/colour left over from an
        // earlier capture (block bake, entity, TESR) on this thread.
        btw.modern.GL11.enableMatrixTracking();
        btw.modern.GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        tess.setTranslation(0, 0, 0);
        tess.setColorOpaque_F(1, 1, 1);
        tess.setNormal(0, 1, 0);
        tess.setTextureUV(0, 0);
        tess.startCapturing();

        try {
            // FC TESR expects coordinates relative to the camera.
            // In MC 1.20.1 BlockEntityRenderer, the PoseStack is already translated
            // to the block position. FC TESRs get (x, y, z) as double offsets.
            // We pass (0, 0, 0) since PoseStack handles positioning.
            fcTesr.renderTileEntityAt(fcTe, 0, 0, 0, partialTick);
        } catch (Throwable e) {
            if (loggedPositions.add(blockEntity.getBlockPos().hashCode() + 99999)) {
                LOGGER.warn("FC TESR render failed for {}: {}", fcTe.getClass().getSimpleName(), e.toString());
            }
        }

        java.util.List<btw.modern.Tessellator.CapturedQuad> captured;
        try {
            captured = tess.stopCapturing();
        } catch (Throwable e) {
            captured = java.util.Collections.emptyList();
        }
        btw.modern.GL11.disableMatrixTracking();
        // Leave this thread clean for the next capture: FC TESRs set tessellator translation
        // (FCTileEntityBasketRenderer) and GL/tessellator colours (RenderItem.renderDroppedItem).
        tess.setTranslation(0, 0, 0);
        tess.setColorOpaque_F(1, 1, 1);
        btw.modern.GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        if (captured.isEmpty()) return;

        // One MC batch per (texture, 1.5.2 blend mode), with the RenderType chosen from the GL state
        // each CapturedQuad carries, exactly as FCEntityRenderer batches entity captures. Untextured
        // quads (GL_TEXTURE_2D off) have no textured entity RenderType equivalent and are skipped.
        java.util.Map<String, TesrTexture> frameTextures = new java.util.HashMap<>();
        java.util.Map<TesrBatch, java.util.List<FCEntityRenderer.Resolved>> batches = new java.util.LinkedHashMap<>();
        for (btw.modern.Tessellator.CapturedQuad cq : captured) {
            if (!FCEntityRenderer.isComplete(cq) || !cq.textured) continue;
            String texName = cq.textureName != null ? cq.textureName : "missing";
            FCEntityRenderer.Blend blend = FCEntityRenderer.blendOf(cq, texName);
            if (blend == FCEntityRenderer.Blend.GLINT) {
                // MC's glint type supplies its own texture and scroll.
                batches.computeIfAbsent(new TesrBatch(null, blend), k -> new java.util.ArrayList<>())
                        .add(new FCEntityRenderer.Resolved(cq, null));
                continue;
            }
            TesrTexture tex = atlasSprite != null
                    ? new TesrTexture(atlasSprite.atlasLocation(), atlasSprite)
                    : frameTextures.computeIfAbsent(texName, FCBlockEntityRenderer::resolveTesrTexture);
            batches.computeIfAbsent(new TesrBatch(tex.location(), blend), k -> new java.util.ArrayList<>())
                    .add(new FCEntityRenderer.Resolved(cq, tex.sprite()));
        }
        if (batches.isEmpty()) return;

        poseStack.pushPose();
        try {
            applyBasketLidHinge(fcTesr, fcTe, blockEntity, partialTick, poseStack);
            var matrix = poseStack.last().pose();
            var normal = poseStack.last().normal();
            int noOverlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;

            for (var entry : batches.entrySet()) {
                TesrBatch batch = entry.getKey();
                net.minecraft.resources.ResourceLocation texLoc =
                        batch.texture() != null ? batch.texture() : UNKNOWN_TEXTURE;
                switch (batch.blend()) {
                    case GLINT -> FCEntityRenderer.emitPositionTex(
                            bufferSource.getBuffer(net.minecraft.client.renderer.RenderType.armorEntityGlint()),
                            matrix, entry.getValue());
                    // SRC_ALPHA/ONE_MINUS_SRC_ALPHA
                    case TRANSLUCENT -> FCEntityRenderer.emitEntity(
                            bufferSource.getBuffer(net.minecraft.client.renderer.RenderType.entityTranslucent(texLoc)),
                            matrix, normal, entry.getValue(), false, packedLight, noOverlay);
                    // ONE / SRC_ALPHA-ONE additive
                    case ADDITIVE -> FCEntityRenderer.emitEntity(
                            bufferSource.getBuffer(net.minecraft.client.renderer.RenderType.eyes(texLoc)),
                            matrix, normal, entry.getValue(), true, packedLight, noOverlay);
                    default -> FCEntityRenderer.emitEntity(
                            bufferSource.getBuffer(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(texLoc)),
                            matrix, normal, entry.getValue(), false, packedLight, noOverlay);
                }
            }
        } catch (Throwable e) {
            if (loggedPositions.add(blockEntity.getBlockPos().hashCode() + 88888)) {
                LOGGER.warn("Emitting FC TESR quads failed for {}: {}", fcTe.getClass().getSimpleName(), e.toString());
            }
        } finally {
            poseStack.popPose();   // an unbalanced PoseStack would crash the frame
        }
    }

    /** One MC batch of a TESR capture. texture is null for the glint (MC supplies it). */
    private record TesrBatch(net.minecraft.resources.ResourceLocation texture, FCEntityRenderer.Blend blend) {}

    /** A TESR texture: a standalone file, or (sprite != null) an Icon in the block atlas. */
    private record TesrTexture(net.minecraft.resources.ResourceLocation location, TextureAtlasSprite sprite) {}

    /**
     * Resolves the texture recorded on a captured TESR quad. A bare Icon name (a NamedIcon drawn
     * through RenderBlocks or RenderItem: basket/hamper lid faces, items in a TESR) is a block-atlas
     * sprite in 1.20.1, with the quad's 0..1 icon UVs remapped into it (FCBakedModel.lookupSprite: FC
     * block/item icons, 1.5.2 terrain and item names); anything else is a texture path. Not cached
     * across frames: atlas sprites change on a resource reload.
     */
    private static TesrTexture resolveTesrTexture(String texName) {
        String lower = texName.toLowerCase(java.util.Locale.ROOT);
        if (lower.indexOf('/') < 0 && !lower.endsWith(".png") && !"missing".equals(lower)) {
            try {
                TextureAtlasSprite sprite = FCBakedModel.lookupSprite(lower);
                if (sprite != null && !MissingTextureAtlasSprite.getLocation().equals(sprite.contents().name())) {
                    return new TesrTexture(sprite.atlasLocation(), sprite);
                }
            } catch (Throwable ignored) {
                // fall back to the standalone-file mapping below
            }
        }
        return new TesrTexture(fcTextureToResourceLocation(texName), null);
    }

    private static final String BASKET_LID_RENDERER = "net.minecraft.src.btw.tileentity.FCTileEntityBasketRenderer";

    /** True for FCTileEntityBasketRenderer and its subclasses (wicker basket, hamper lids). */
    private static final ClassValue<Boolean> IS_BASKET_LID_RENDERER = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            for (Class<?> c = type; c != null; c = c.getSuperclass()) {
                if (BASKET_LID_RENDERER.equals(c.getName())) return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
    };

    /** FCBlockBasket.GetLidRotationPoint() (Client FCBlockBasket.java:199), looked up per block class. */
    private static final ClassValue<java.util.Optional<java.lang.reflect.Method>> LID_ROTATION_POINT =
            new ClassValue<>() {
        @Override
        protected java.util.Optional<java.lang.reflect.Method> computeValue(Class<?> type) {
            try {
                return java.util.Optional.of(type.getMethod("GetLidRotationPoint"));
            } catch (Throwable e) {
                return java.util.Optional.empty();
            }
        }
    };

    /**
     * FCTileEntityBasketRenderer.RenderBasketLidAsBlock (Client :94-120) swings the open lid about its
     * hinge with GL calls issued AFTER the lid vertices, just before Tessellator.draw(): 1.5.2 applied
     * the modelview when the batch was drawn. The capture Tessellator transforms each vertex when it is
     * added (Tessellator.addVertex), so those calls never reached the captured lid and an open wicker
     * basket or hamper showed its lid shut. Apply the same hinge to the PoseStack the captured quads
     * are emitted with. FCTileEntityBasketRenderer draws only the lid (and only while open), so the
     * transform touches nothing else. If the Tessellator ever applies the GL modelview at draw() time,
     * this becomes a double rotation and must go.
     */
    private void applyBasketLidHinge(btw.modern.TileEntitySpecialRenderer fcTesr, btw.modern.TileEntity fcTe,
                                     ProxyBlockEntity be, float partialTick, PoseStack poseStack) {
        try {
            if (!IS_BASKET_LID_RENDERER.get(fcTesr.getClass())) return;
            if (!(be.getBlockState().getBlock() instanceof btw.forge.ProxyBlock pb)) return;
            btw.modern.Block block = pb.getFcBlock();
            if (block == null) return;
            java.lang.reflect.Method lidPoint = LID_ROTATION_POINT.get(block.getClass()).orElse(null);
            if (lidPoint == null) return;
            float open = basketOpenRatio(fcTe, partialTick);
            if (Float.isNaN(open)) return;

            int meta = be.getBlockState().hasProperty(btw.forge.ProxyBlock.META)
                    ? be.getBlockState().getValue(btw.forge.ProxyBlock.META) : 0;
            int facing = block.GetFacing(meta);
            float lidAngle = open * 45F;                                                       // :95

            // :97-99
            if (!(lidPoint.invoke(block) instanceof btw.modern.Vec3 point)) return;
            btw.modern.Vec3 pivot = btw.modern.Vec3.createVectorHelper(point);
            pivot.RotateAsBlockPosAroundJToFacing(facing);

            // :101-120 -- translate to the pivot, rotate, translate back
            org.joml.Quaternionf rotation;
            if (facing == 2) rotation = Axis.XP.rotationDegrees(lidAngle);
            else if (facing == 3) rotation = Axis.XP.rotationDegrees(-lidAngle);
            else if (facing == 4) rotation = Axis.ZP.rotationDegrees(-lidAngle);
            else if (facing == 5) rotation = Axis.ZP.rotationDegrees(lidAngle);
            else return;
            poseStack.rotateAround(rotation, (float) pivot.xCoord, (float) pivot.yCoord, (float) pivot.zCoord);
        } catch (Throwable e) {
            if (loggedPositions.add(be.getBlockPos().hashCode() + 44444)) {
                LOGGER.warn("Basket lid hinge failed for {}: {}", fcTe.getClass().getSimpleName(), e.toString());
            }
        }
    }

    /**
     * FCTileEntityBasketRenderer.GetCurrentOpenRatio (Client :131-139) over the public FCTileEntityBasket
     * fields m_fLidOpenRatio / m_fPrevLidOpenRatio (FCTileEntityBasket.java:29-30). NaN if the TE is
     * not a basket.
     */
    private static float basketOpenRatio(btw.modern.TileEntity fcTe, float partialTick) {
        try {
            float ratio = fcTe.getClass().getField("m_fLidOpenRatio").getFloat(fcTe);
            float prevRatio = fcTe.getClass().getField("m_fPrevLidOpenRatio").getFloat(fcTe);
            float open = prevRatio + (ratio - prevRatio) * partialTick;
            open = 1.0F - open;
            return 1.0F - open * open * open;
        } catch (Throwable e) {
            return Float.NaN;
        }
    }

    private void renderCampfireCookItem(btw.modern.ItemStack fcCookStack,
                                         PoseStack poseStack, MultiBufferSource bufferSource,
                                         int packedLight, int packedOverlay,
                                         ProxyBlockEntity blockEntity) {
        ItemStack mcStack = ItemStackHelper.toMcStack(fcCookStack);
        if (mcStack.isEmpty()) return;

        // Check spit alignment from block metadata
        boolean iAligned = false;
        try {
            int meta = 0;
            if (blockEntity.getBlockState().hasProperty(btw.forge.ProxyBlock.META)) {
                meta = blockEntity.getBlockState().getValue(btw.forge.ProxyBlock.META);
            }
            // GetIsIAligned checks (metadata & 4) != 0
            iAligned = (meta & 4) != 0;
        } catch (Exception ignored) {}

        poseStack.pushPose();
        poseStack.translate(0.5, 14.0 / 16.0, 0.5);

        // Rotate based on spit alignment (matching FC's renderer)
        if (!iAligned) {
            poseStack.mulPose(Axis.YP.rotationDegrees(90));
        }

        poseStack.scale(0.5f, 0.5f, 0.5f);

        itemRenderer.renderStatic(mcStack, ItemDisplayContext.FIXED,
                packedLight, packedOverlay, poseStack, bufferSource,
                Minecraft.getInstance().level, 0);
        poseStack.popPose();
    }

    private void renderFurnaceBrickCookItem(btw.modern.ItemStack fcCookStack,
                                              PoseStack poseStack, MultiBufferSource bufferSource,
                                              int packedLight, int packedOverlay,
                                              ProxyBlockEntity blockEntity) {
        ItemStack mcStack = ItemStackHelper.toMcStack(fcCookStack);
        if (mcStack.isEmpty()) return;

        // Get facing from block metadata
        int meta = 0;
        if (blockEntity.getBlockState().hasProperty(btw.forge.ProxyBlock.META)) {
            meta = blockEntity.getBlockState().getValue(btw.forge.ProxyBlock.META);
        }
        int facing = meta & 7;

        // Position at center, offset toward front face
        double offsetX = 0, offsetZ = 0;
        float yaw = 0;
        switch (facing) {
            case 2: offsetZ = -0.25; yaw = 0; break;   // north
            case 3: offsetZ = 0.25; yaw = 180; break;   // south
            case 4: offsetX = -0.25; yaw = 90; break;   // west
            case 5: offsetX = 0.25; yaw = 270; break;   // east
        }

        // Get light from block in front of furnace for better item visibility
        var pos = blockEntity.getBlockPos();
        var level = Minecraft.getInstance().level;
        int itemLight = packedLight;
        if (level != null) {
            net.minecraft.core.BlockPos frontPos = pos.relative(net.minecraft.core.Direction.from3DDataValue(facing));
            itemLight = net.minecraft.client.renderer.LightTexture.pack(
                    level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, frontPos),
                    level.getBrightness(net.minecraft.world.level.LightLayer.SKY, frontPos));
        }

        poseStack.pushPose();
        poseStack.translate(0.5 + offsetX, 6.5 / 16.0, 0.5 + offsetZ);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yaw));
        poseStack.scale(0.5f, 0.5f, 0.5f);

        itemRenderer.renderStatic(mcStack, ItemDisplayContext.FIXED,
                itemLight, packedOverlay, poseStack, bufferSource,
                Minecraft.getInstance().level, 0);

        // Render cooking progress as breaking/cracking overlay on the item model.
        // Re-renders the same item with a destroy stage texture mapped onto it.
        btw.modern.TileEntity fcTe = blockEntity.getFcTileEntity();
        if (fcTe != null) {
            try {
                var cookTimeField = fcTe.getClass().getField("furnaceCookTime");
                int cookTime = cookTimeField.getInt(fcTe);
                var getCookTimeMethod = fcTe.getClass().getMethod("GetCookTimeForCurrentItem");
                int maxCookTime = (int) getCookTimeMethod.invoke(fcTe);
                if (maxCookTime > 0 && cookTime > 0) {
                    float progress = (float) cookTime / (float) maxCookTime;
                    int stage = Math.min(9, (int) (progress * 10));

                    // Get the destroy stage sprite from the block atlas
                    var destroySprite = Minecraft.getInstance().getTextureAtlas(
                            net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)
                            .apply(new net.minecraft.resources.ResourceLocation("block/destroy_stage_" + stage));

                    // Re-render the item model with UVs replaced by the destroy sprite.
                    // Track vertex positions to derive UVs from world-space coordinates.
                    var baseConsumer = bufferSource.getBuffer(
                            net.minecraft.client.renderer.RenderType.entityCutoutNoCull(
                                    net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS));

                    final var sprite = destroySprite;
                    var crumbleConsumer = new com.mojang.blaze3d.vertex.VertexConsumer() {
                        float lastX, lastY, lastZ;
                        public com.mojang.blaze3d.vertex.VertexConsumer vertex(double x, double y, double z) {
                            lastX = (float)x; lastY = (float)y; lastZ = (float)z;
                            baseConsumer.vertex(x, y, z);
                            return this;
                        }
                        public com.mojang.blaze3d.vertex.VertexConsumer color(int r, int g, int b, int a) {
                            baseConsumer.color(255, 255, 255, 255);
                            return this;
                        }
                        public com.mojang.blaze3d.vertex.VertexConsumer uv(float u, float v) {
                            // Derive UVs from vertex position (maps world coords to texture)
                            float pu = (lastX + 0.5f) % 1.0f;
                            float pv = (lastY + 0.5f) % 1.0f;
                            if (pu < 0) pu += 1.0f;
                            if (pv < 0) pv += 1.0f;
                            baseConsumer.uv(
                                    sprite.getU(pu * 16),
                                    sprite.getV(pv * 16));
                            return this;
                        }
                        public com.mojang.blaze3d.vertex.VertexConsumer overlayCoords(int u, int v) { baseConsumer.overlayCoords(u, v); return this; }
                        public com.mojang.blaze3d.vertex.VertexConsumer uv2(int u, int v) { baseConsumer.uv2(u, v); return this; }
                        public com.mojang.blaze3d.vertex.VertexConsumer normal(float x, float y, float z) { baseConsumer.normal(x, y, z); return this; }
                        public void endVertex() { baseConsumer.endVertex(); }
                        public void defaultColor(int r, int g, int b, int a) { baseConsumer.defaultColor(r, g, b, a); }
                        public void unsetDefaultColor() { baseConsumer.unsetDefaultColor(); }
                    };

                    itemRenderer.renderStatic(mcStack, ItemDisplayContext.FIXED,
                            itemLight, packedOverlay, poseStack, type -> crumbleConsumer,
                            Minecraft.getInstance().level, 0);
                }
            } catch (Exception ignored) {}
        }

        poseStack.popPose();
    }

    private void renderGenericCookItem(btw.modern.ItemStack fcCookStack,
                                        PoseStack poseStack, MultiBufferSource bufferSource,
                                        int packedLight, int packedOverlay) {
        ItemStack mcStack = ItemStackHelper.toMcStack(fcCookStack);
        if (mcStack.isEmpty()) return;

        poseStack.pushPose();
        poseStack.translate(0.5, 6.5 / 16.0, 0.5);
        poseStack.scale(0.4f, 0.4f, 0.4f);

        itemRenderer.renderStatic(mcStack, ItemDisplayContext.FIXED,
                packedLight, packedOverlay, poseStack, bufferSource,
                Minecraft.getInstance().level, 0);
        poseStack.popPose();
    }

    /**
     * FCTileEntityToolPlacedRenderer (:163) draws the tool through RenderItem.renderDroppedItem, so the
     * captured quads carry the tool's 1.5.2 icon name (or the "/gui/items.png" sheet bound after the
     * icon UV reads) with 0..1 icon UVs. Vanilla-id tools ("hatchetiron", ...) have no standalone file
     * under that name and the 1.5.2 item sheet does not exist in 1.20.1, so texture the quads with the
     * tool's own 1.20.1 item sprite (block atlas). Returns null (standalone-file mapping) if the stack
     * or its sprite cannot be resolved.
     */
    @Nullable
    private TextureAtlasSprite toolPlacedItemSprite(btw.modern.TileEntity fcTe) {
        try {
            Object stack = fcTe.getClass().getMethod("GetToolStack").invoke(fcTe);
            if (!(stack instanceof btw.modern.ItemStack fcStack) || fcStack.itemID <= 0) return null;
            ItemStack mcStack = ItemStackHelper.toMcStack(fcStack);
            if (mcStack.isEmpty()) return null;
            TextureAtlasSprite sprite = itemRenderer.getModel(mcStack, Minecraft.getInstance().level, null, 0)
                    .getParticleIcon();
            if (sprite == null || MissingTextureAtlasSprite.getLocation().equals(sprite.contents().name())) {
                return null;
            }
            return sprite;
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * 1.5.2 FCTileEntityBasketWickerRenderer.RenderStorageStack (Client :40-79), drawn natively like
     * the cook stacks: the stored item rises out of the open basket and faces the basket front.
     * One copy with the GROUND transform (1.5.2 RenderItem drew 2-5 jittered copies for big stacks).
     */
    private void renderBasketStorageStack(btw.modern.TileEntity fcTe, ProxyBlockEntity be, float partialTick,
                                          PoseStack poseStack, MultiBufferSource buffers,
                                          int packedLight, int packedOverlay) {
        try {
            btw.modern.ItemStack fcStack =
                    (btw.modern.ItemStack) fcTe.getClass().getMethod("GetStorageStack").invoke(fcTe);
            float ratio = fcTe.getClass().getField("m_fLidOpenRatio").getFloat(fcTe);
            float prevRatio = fcTe.getClass().getField("m_fPrevLidOpenRatio").getFloat(fcTe);
            // :44 -- only while the lid is (partly) open
            if (fcStack == null || fcStack.itemID <= 0 || fcStack.stackSize <= 0 || ratio <= 0.01F) return;
            ItemStack mcStack = ItemStackHelper.toMcStack(fcStack);
            if (mcStack.isEmpty()) return;

            // FCTileEntityBasketRenderer.GetCurrentOpenRatio (:129-136)
            float open = prevRatio + (ratio - prevRatio) * partialTick;
            open = 1.0F - open;
            open = 1.0F - open * open * open;

            int meta = be.getBlockState().hasProperty(btw.forge.ProxyBlock.META)
                    ? be.getBlockState().getValue(btw.forge.ProxyBlock.META) : 0;
            int facing = (meta & 3) + 2;                                                   // FCBlockBasket.GetFacing :106-109
            float yaw = facing == 3 ? 180F : facing == 4 ? 90F : facing == 5 ? 270F : 0F;  // ConvertFacingToYaw :81-99
            var level = Minecraft.getInstance().level;
            boolean gui3d = itemRenderer.getModel(mcStack, level, null, 0).isGui3d();

            poseStack.pushPose();
            try {
                // :54-56 (max height 0.25 * open ratio) + RenderItem's bob at age 0 (0.1)
                poseStack.translate(0.5, 0.25F * open + 0.1F, 0.5);
                // :60-65 -- fancyGraphics (RenderManager.options default) rotates to the basket front
                poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
                if (fcStack.stackSize > 4 && gui3d) {
                    poseStack.translate(0.0, 0.0, -0.15);                                  // :68-73
                }
                itemRenderer.renderStatic(mcStack, ItemDisplayContext.GROUND, packedLight, packedOverlay,
                        poseStack, buffers, level, 0);
            } finally {
                poseStack.popPose();   // an unbalanced PoseStack would crash the frame
            }
        } catch (Throwable e) {
            if (loggedPositions.add(be.getBlockPos().hashCode() + 33333)) {
                LOGGER.warn("Wicker basket storage-stack render failed: {}", e.toString());
            }
        }
    }

    private static final java.util.Set<Integer> loggedPositions = java.util.Collections.synchronizedSet(new java.util.HashSet<>());
    private static boolean logRendererOnce(int posHash, String className, Object cookStack) {
        int key = posHash ^ (cookStack != null ? 1 : 0);
        return loggedPositions.add(key);
    }

    /** Reads the cook/display stack from an FC tile entity via reflection. */
    private static btw.modern.ItemStack getCookStack(btw.modern.TileEntity fcTe) {
        btw.modern.ItemStack result = null;

        // Try GetCookStack() method (campfire, furnace)
        try {
            var method = fcTe.getClass().getMethod("GetCookStack");
            result = (btw.modern.ItemStack) method.invoke(fcTe);
        } catch (Exception ignored) {}

        // Try GetDisplayStack() or similar
        if (result == null) {
            try {
                var method = fcTe.getClass().getMethod("GetDisplayStack");
                result = (btw.modern.ItemStack) method.invoke(fcTe);
            } catch (Exception ignored) {}
        }

        // Treat itemID 0 (air) or stackSize 0 as empty
        if (result != null && (result.itemID <= 0 || result.stackSize <= 0)) {
            return null;
        }

        return result;
    }

    private static final net.minecraft.resources.ResourceLocation UNKNOWN_TEXTURE =
            new net.minecraft.resources.ResourceLocation("minecraft", "textures/misc/unknown_pack.png");
    private static final java.util.Map<String, net.minecraft.resources.ResourceLocation> TEXTURE_CACHE =
            new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Converts an FC texture path (e.g. "/item/chest.png", "/mob/cow.png")
     * to an MC 1.20.1 ResourceLocation.
     *
     * FC paths map to vanilla texture locations:
     * - "/item/chest.png" → "minecraft:textures/entity/chest/normal.png"
     * - "/mob/cow.png" → "minecraft:textures/entity/cow/cow.png"
     * - BTW-specific textures → "betterthanwolves:textures/..."
     */
    private static net.minecraft.resources.ResourceLocation fcTextureToResourceLocation(String fcPath) {
        if (fcPath == null || fcPath.isEmpty()) {
            return UNKNOWN_TEXTURE;
        }
        return TEXTURE_CACHE.computeIfAbsent(fcPath, p -> {
            // Strip leading slash; ResourceLocation paths must be lowercase
            String path = p.startsWith("/") ? p.substring(1) : p;
            String lower = path.toLowerCase(java.util.Locale.ROOT);
            try {
                // Bare NamedIcon name that has no block-atlas sprite (resolveTesrTexture tries the
                // atlas first), with 0..1 UVs over the standalone 16x16 file. Same mapping as
                // FCEntityRenderer.computeEntityTexture: fcBlock* icons live in textures/block/,
                // everything else in textures/item/.
                if (!lower.contains("/") && !lower.endsWith(".png")) {
                    String subdir = lower.startsWith("fcblock") ? "block/" : "item/";
                    return new net.minecraft.resources.ResourceLocation("betterthanwolves",
                            "textures/" + subdir + lower + ".png");
                }

                // Use bundled 1.5.2 textures — MC 1.20.1 changed the chest texture UV layout,
                // so the 1.5.2 ModelChest UVs don't match 1.20.1 textures.
                // The 1.5.2 textures are extracted to assets/betterthanwolves/textures/item/
                // Generic mapping: as-is under betterthanwolves namespace ("/item/chest.png").
                String noExt = lower.endsWith(".png") ? lower.substring(0, lower.length() - 4) : lower;
                return new net.minecraft.resources.ResourceLocation("betterthanwolves", "textures/" + noExt + ".png");
            } catch (RuntimeException e) {
                // ResourceLocationException (illegal path character) must not escape the BER.
                return UNKNOWN_TEXTURE;
            }
        });
    }
}
