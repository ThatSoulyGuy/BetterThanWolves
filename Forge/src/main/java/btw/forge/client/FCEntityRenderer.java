package btw.forge.client;

import btw.forge.BTWForgeMod;
import btw.forge.NamedIcon;
import btw.forge.ProxyEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic entity renderer that captures FC's doRender() vertex output
 * via the Tessellator and renders it through MC 1.20.1's pipeline.
 *
 * <p>FC entity renderers use GL11 transforms (translate/rotate/scale) and
 * ModelBase/Tessellator drawing. Our GL11 tracks a real matrix stack, and
 * the Tessellator applies it to vertex positions. This renderer captures
 * the transformed vertices and pipes them to MC's VertexConsumer.</p>
 */
public class FCEntityRenderer extends EntityRenderer<Entity> {

    private static final Logger LOGGER = LogManager.getLogger("BTW-EntityRenderer");
    private static final ResourceLocation FALLBACK_TEXTURE =
            new ResourceLocation(BTWForgeMod.MOD_ID, "textures/entity/fcplaceholder.png");

    /** Cache of FC renderer instances by FC entity class name. */
    private static final Map<String, btw.modern.Render> fcRenderers = new ConcurrentHashMap<>();
    private static boolean renderersInitialized = false;

    public FCEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    /**
     * Initializes the FC renderer map by calling FC's ClientAddEntityRenderers.
     * Must be called after FC is loaded.
     */
    public static void initFcRenderers() {
        if (renderersInitialized) return;
        renderersInitialized = true;

        // Call FC's ClientAddEntityRenderers to populate RenderManager
        try {
            Class<?> btwClass = Class.forName("net.minecraft.src.btw.core.FCBetterThanWolves");
            java.lang.reflect.Field instanceField = btwClass.getField("m_instance");
            Object btwInstance = instanceField.get(null);
            LOGGER.info("FCBetterThanWolves.m_instance = {}", btwInstance != null ? btwInstance.getClass().getName() : "null");

            if (btwInstance != null) {
                java.lang.reflect.Method m = btwClass.getMethod("ClientAddEntityRenderers");
                m.invoke(btwInstance);
                LOGGER.info("Called FC ClientAddEntityRenderers successfully.");
            } else {
                LOGGER.warn("m_instance is null, trying direct registration fallback.");
                tryDirectRegistration();
            }
        } catch (Exception e) {
            LOGGER.warn("Could not call ClientAddEntityRenderers: {} — trying direct registration", e.getMessage());
            tryDirectRegistration();
        }

        // Populate from FC's RenderManager renderer map
        try {
            Map<?, ?> rendererMap = btw.modern.RenderManager.instance.getEntityRenderMap();
            if (rendererMap != null) {
                for (Map.Entry<?, ?> entry : rendererMap.entrySet()) {
                    Class<?> entityClass = (Class<?>) entry.getKey();
                    btw.modern.Render renderer = (btw.modern.Render) entry.getValue();
                    fcRenderers.put(entityClass.getName(), renderer);
                    LOGGER.info("FC renderer: {} -> {}",
                            entityClass.getSimpleName(), renderer.getClass().getSimpleName());
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to read FC renderer map: {}", e.getMessage());
        }
        LOGGER.info("Initialized {} FC entity renderers.", fcRenderers.size());
    }

    /** Fallback: try to instantiate known FC renderer classes directly. */
    private static void tryDirectRegistration() {
        String[] rendererClasses = {
                "net.minecraft.src.btw.client.FCClientRenderWindMill",
                "net.minecraft.src.btw.client.FCClientRenderWaterWheel",
                "net.minecraft.src.btw.client.FCClientRenderEntityWindMillVertical",
                "net.minecraft.src.btw.client.FCClientRenderCanvas",
                "net.minecraft.src.btw.client.FCClientRenderDynamite",
                "net.minecraft.src.btw.client.FCClientRenderUrn",
                "net.minecraft.src.btw.client.FCClientRenderMovingPlatform",
                "net.minecraft.src.btw.client.FCClientRenderBlockLiftedByPlatform",
                "net.minecraft.src.btw.client.FCClientRenderLightningBolt",
                "net.minecraft.src.btw.client.FCClientRenderBroadheadArrow",
                "net.minecraft.src.btw.client.FCClientRenderMiningCharge",
        };
        for (String className : rendererClasses) {
            try {
                Class<?> renderClass = Class.forName(className);
                btw.modern.Render renderer = (btw.modern.Render) renderClass.getDeclaredConstructor().newInstance();
                // Find the entity class this renderer is for by checking the class name pattern
                // FCClientRenderWindMill -> FCEntityWindMill
                String entityClassName = className
                        .replace("FCClientRender", "FCEntity")
                        .replace("net.minecraft.src.btw.client.", "net.minecraft.src.btw.entity.");
                try {
                    Class<?> entityClass = Class.forName(entityClassName);
                    btw.modern.RenderManager.AddEntityRenderer(entityClass, renderer);
                    LOGGER.info("Direct-registered FC renderer: {} for {}",
                            renderClass.getSimpleName(), entityClass.getSimpleName());
                } catch (ClassNotFoundException ignored) {
                    // Entity class name pattern doesn't match, skip
                }
            } catch (Exception e) {
                LOGGER.debug("Could not direct-register {}: {}", className, e.getMessage());
            }
        }
    }

    @Override
    public void render(Entity entity, float yaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (!(entity instanceof ProxyEntity proxy)) {
            LOGGER.warn("FCEntityRenderer.render called with non-ProxyEntity: {}", entity.getClass().getName());
            return;
        }
        btw.modern.Entity fcEntity = proxy.getFcEntity();
        String fcClassName = proxy.getFcClassName();

        // Find the FC renderer
        btw.modern.Render fcRenderer = fcEntity != null ? findRenderer(fcEntity.getClass()) : null;

        if (logOnce(entity.getId())) {
            LOGGER.info("FCEntityRenderer.render: id={} fcEntity={} fcClassName='{}' fcRenderer={}",
                    entity.getId(),
                    fcEntity != null ? fcEntity.getClass().getSimpleName() : "null",
                    fcClassName,
                    fcRenderer != null ? fcRenderer.getClass().getSimpleName() : "null");
        }

        if (fcEntity == null || fcRenderer == null) {
            renderDebugBox(poseStack, bufferSource, packedLight, entity);
            return;
        }

        // Enable GL11 matrix tracking and capture Tessellator output
        btw.modern.GL11.enableMatrixTracking();
        btw.modern.Tessellator tess = btw.modern.Tessellator.instance;
        tess.startCapturing();

        try {
            fcRenderer.doRender(fcEntity, 0, 0, 0, yaw, partialTick);
        } catch (Exception e) {
            LOGGER.debug("FC doRender failed for {}: {}", fcEntity.getClass().getSimpleName(), e.getMessage());
        }

        List<btw.modern.Tessellator.CapturedQuad> quads = tess.stopCapturing();
        btw.modern.GL11.disableMatrixTracking();

        if (quads.isEmpty()) {
            // FC renderer produced no quads — show debug box
            renderDebugBox(poseStack, bufferSource, packedLight, entity);
            return;
        }

        // Render captured quads through MC's pipeline
        poseStack.pushPose();

        RenderType renderType = RenderType.entityCutoutNoCull(getTextureLocation(entity));
        VertexConsumer consumer = bufferSource.getBuffer(renderType);
        Matrix4f mat = poseStack.last().pose();

        for (btw.modern.Tessellator.CapturedQuad quad : quads) {
            for (int i = 0; i < 4; i++) {
                btw.modern.Tessellator.CapturedVertex v = quad.vertices[i];
                consumer.vertex(mat, (float) v.x, (float) v.y, (float) v.z)
                        .color((float) v.r, (float) v.g, (float) v.b, (float) v.a)
                        .uv((float) v.u, (float) v.v)
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(packedLight)
                        .normal(poseStack.last().normal(), (float) v.nx, (float) v.ny, (float) v.nz)
                        .endVertex();
            }
        }

        poseStack.popPose();
    }

    /** Renders a simple colored wireframe box so invisible entities are at least locatable. */
    private void renderDebugBox(PoseStack poseStack, MultiBufferSource bufferSource,
                                 int packedLight, Entity entity) {
        poseStack.pushPose();
        float w = entity.getBbWidth() / 2f;
        float h = entity.getBbHeight();
        net.minecraft.client.renderer.LevelRenderer.renderLineBox(
                poseStack,
                bufferSource.getBuffer(RenderType.lines()),
                -w, 0, -w, w, h, w,
                1.0f, 0.5f, 0.0f, 1.0f); // Orange wireframe
        poseStack.popPose();
    }

    private static final java.util.Set<Integer> loggedEntityIds = java.util.Collections.synchronizedSet(new java.util.HashSet<>());
    private static boolean logOnce(int id) {
        return loggedEntityIds.add(id);
    }

    private btw.modern.Render findRenderer(Class<?> fcClass) {
        for (Class<?> c = fcClass; c != null; c = c.getSuperclass()) {
            btw.modern.Render r = fcRenderers.get(c.getName());
            if (r != null) return r;
        }
        return null;
    }

    @Override
    public ResourceLocation getTextureLocation(Entity entity) {
        // FC renderers bind their own textures — we use a fallback
        // The actual texture comes from the captured quad UV mapping
        return FALLBACK_TEXTURE;
    }
}
