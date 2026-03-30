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
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

        String className = fcTe.getClass().getSimpleName();

        // === Generalized FC TESR rendering via vertex capture ===
        // Look up FC's TileEntitySpecialRenderer for this tile entity class.
        // If one exists, run it with GL11 matrix tracking enabled, capture
        // vertices from Tessellator, and render them as MC 1.20.1 quads.
        btw.modern.TileEntitySpecialRenderer fcTesr =
                btw.modern.TileEntityRenderer.instance.getSpecialRendererForClass(fcTe.getClass());
        if (fcTesr != null) {
            renderFcTESR(fcTesr, fcTe, blockEntity, partialTick, poseStack, bufferSource, packedLight);
        } else if (loggedPositions.add(blockEntity.getBlockPos().hashCode() + 77777)) {
            LOGGER.info("No FC TESR for {} (class hierarchy: {} → {})",
                    fcTe.getClass().getSimpleName(),
                    fcTe.getClass().getSuperclass() != null ? fcTe.getClass().getSuperclass().getSimpleName() : "none",
                    fcTe.getClass().getSuperclass() != null && fcTe.getClass().getSuperclass().getSuperclass() != null
                            ? fcTe.getClass().getSuperclass().getSuperclass().getSimpleName() : "none");
        }

        // Try rendering cook stack for any tile entity that has one
        btw.modern.ItemStack cookStack = getCookStack(fcTe);
        if (cookStack != null && loggedPositions.add(blockEntity.getBlockPos().hashCode() + 55555)) {
            LOGGER.info("Cook stack found: {} x{} for {} at {}",
                    cookStack.itemID, cookStack.stackSize, className, blockEntity.getBlockPos());
        }
        if (cookStack != null) {
            if (className.contains("Campfire")) {
                renderCampfireCookItem(cookStack, poseStack, bufferSource, packedLight, packedOverlay, blockEntity);
            } else if (className.contains("FurnaceBrick")) {
                renderFurnaceBrickCookItem(cookStack, poseStack, bufferSource, packedLight, packedOverlay, blockEntity);
            } else {
                renderGenericCookItem(cookStack, poseStack, bufferSource, packedLight, packedOverlay);
            }
        }

        // Emit cooking particles for crucible/cauldron when fire is below
        if (className.contains("CookingVessel") || className.contains("Crucible") || className.contains("Cauldron")) {
            emitCookingParticles(fcTe, blockEntity);
        }
    }

    /**
     * Runs an FC TileEntitySpecialRenderer with GL11 matrix tracking and
     * Tessellator vertex capture, then renders the captured quads using
     * MC 1.20.1's rendering system.
     */
    private void renderFcTESR(btw.modern.TileEntitySpecialRenderer fcTesr,
                               btw.modern.TileEntity fcTe,
                               ProxyBlockEntity blockEntity,
                               float partialTick,
                               PoseStack poseStack,
                               MultiBufferSource bufferSource,
                               int packedLight) {
        btw.modern.Tessellator tess = btw.modern.Tessellator.instance;
        btw.modern.GL11.enableMatrixTracking();
        tess.startCapturing();

        try {
            // FC TESR expects coordinates relative to the camera.
            // In MC 1.20.1 BlockEntityRenderer, the PoseStack is already translated
            // to the block position. FC TESRs get (x, y, z) as double offsets.
            // We pass (0, 0, 0) since PoseStack handles positioning.
            fcTesr.renderTileEntityAt(fcTe, 0, 0, 0, partialTick);
        } catch (Throwable e) {
            if (loggedPositions.add(blockEntity.getBlockPos().hashCode() + 99999)) {
                LOGGER.warn("FC TESR render failed for {}: {}", fcTe.getClass().getSimpleName(), e.getMessage());
            }
        }

        java.util.List<btw.modern.Tessellator.CapturedQuad> captured = tess.stopCapturing();
        btw.modern.GL11.disableMatrixTracking();

        if (captured.isEmpty()) return;

        // Group quads by texture name — each unique texture needs its own RenderType
        // because TESR textures are standalone files, not atlas sprites.
        java.util.Map<String, java.util.List<btw.modern.Tessellator.CapturedQuad>> byTexture = new java.util.LinkedHashMap<>();
        for (btw.modern.Tessellator.CapturedQuad cq : captured) {
            if (cq == null || cq.vertices == null) continue;
            String tex = cq.textureName != null ? cq.textureName : "missing";
            byTexture.computeIfAbsent(tex, k -> new java.util.ArrayList<>()).add(cq);
        }

        poseStack.pushPose();
        var matrix = poseStack.last().pose();
        var normal = poseStack.last().normal();

        for (var entry : byTexture.entrySet()) {
            // Convert FC texture path (e.g. "/item/chest.png") to MC ResourceLocation
            net.minecraft.resources.ResourceLocation texLoc = fcTextureToResourceLocation(entry.getKey());
            var consumer = bufferSource.getBuffer(
                    net.minecraft.client.renderer.RenderType.entityCutoutNoCull(texLoc));

            for (btw.modern.Tessellator.CapturedQuad cq : entry.getValue()) {
                for (int vi = 0; vi < 4; vi++) {
                    btw.modern.Tessellator.CapturedVertex v = cq.vertices[vi];
                    // UV coordinates are already in 0-1 range from ModelRenderer
                    consumer.vertex(matrix, (float) v.x, (float) v.y, (float) v.z)
                            .color(v.r, v.g, v.b, v.a)
                            .uv((float) v.u, (float) v.v)
                            .overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                            .uv2(packedLight)
                            .normal(normal, v.nx, v.ny, v.nz)
                            .endVertex();
                }
            }
        }

        poseStack.popPose();
    }

    /** Emits steam/smoke particles from cooking vessels when they have fire below. */
    private void emitCookingParticles(btw.modern.TileEntity fcTe, ProxyBlockEntity blockEntity) {
        // Check m_iFireUnderType via reflection (0=none, 1=normal fire, 2=stoked)
        int fireType = 0;
        try {
            var f = fcTe.getClass().getField("m_iFireUnderType");
            fireType = f.getInt(fcTe);
        } catch (Exception e) {
            try {
                var f = fcTe.getClass().getDeclaredField("m_iFireUnderType");
                f.setAccessible(true);
                fireType = f.getInt(fcTe);
            } catch (Exception ignored) {}
        }

        if (fireType <= 0) return;

        var level = Minecraft.getInstance().level;
        if (level == null) return;

        var pos = blockEntity.getBlockPos();
        var rand = level.getRandom();

        // Normal fire: gentle steam. Stoked fire: more intense
        int chance = fireType == 2 ? 2 : 4;
        if (rand.nextInt(chance) == 0) {
            double px = pos.getX() + 0.2 + rand.nextFloat() * 0.6;
            double py = pos.getY() + 0.9 + rand.nextFloat() * 0.2;
            double pz = pos.getZ() + 0.2 + rand.nextFloat() * 0.6;

            if (fireType == 2) {
                // Stoked: larger smoke
                level.addParticle(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                        px, py, pz, 0, 0.05, 0);
            } else {
                // Normal: gentle steam
                level.addParticle(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        px, py, pz, 0, 0.03, 0);
            }
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

        // Spawn cooking particles only when the campfire is lit (fire level > 0).
        // Unlit campfire = block 1013 (m_iFireLevel=0), lit = 1014/1015/1016.
        boolean isLit = false;
        if (blockEntity.getBlockState().getBlock() instanceof btw.forge.ProxyBlock pb) {
            isLit = pb.getLegacyId() >= 1014 && pb.getLegacyId() <= 1016;
        }
        if (isLit && Minecraft.getInstance().level != null) {
            var level = Minecraft.getInstance().level;
            var pos = blockEntity.getBlockPos();
            var rand = level.getRandom();
            if (rand.nextInt(3) == 0) {
                double px = pos.getX() + 0.375 + rand.nextFloat() * 0.25;
                double py = pos.getY() + 0.75 + rand.nextFloat() * 0.1;
                double pz = pos.getZ() + 0.375 + rand.nextFloat() * 0.25;
                level.addParticle(
                        net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        px, py, pz, 0, 0.02, 0);
            }
        }
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
            return new net.minecraft.resources.ResourceLocation("minecraft", "textures/misc/unknown_pack.png");
        }

        // Strip leading slash
        String path = fcPath.startsWith("/") ? fcPath.substring(1) : fcPath;

        // Use bundled 1.5.2 textures — MC 1.20.1 changed the chest texture UV layout,
        // so the 1.5.2 ModelChest UVs don't match 1.20.1 textures.
        // The 1.5.2 textures are extracted to assets/betterthanwolves/textures/item/

        // Generic mapping: try as-is under betterthanwolves namespace
        // Strip .png extension for ResourceLocation
        String noExt = path.endsWith(".png") ? path.substring(0, path.length() - 4) : path;
        return new net.minecraft.resources.ResourceLocation("betterthanwolves", "textures/" + noExt + ".png");
    }
}
