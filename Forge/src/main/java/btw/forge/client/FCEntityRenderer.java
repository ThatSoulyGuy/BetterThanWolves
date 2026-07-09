package btw.forge.client;

import btw.forge.BTWForgeMod;
import btw.forge.NamedIcon;
import btw.forge.ProxyAnimal;
import btw.forge.ProxyEntity;
import btw.forge.ProxyMob;
import btw.forge.ProxyPathfinderMob;
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

        // Register the vanilla renderers for frozen-vanilla entities that FC's
        // ClientAddEntityRenderers doesn't cover (it only registers FC subclasses).
        // Without these, fc_arrow/fc_snowball/fc_egg/fc_tnt_primed/fc_fish_hook have no
        // FC Render and fall back to the debug box.
        registerMissingVanillaRenderers();

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

    /**
     * Registers the ported vanilla renderers for the frozen-vanilla entities FC does not
     * subclass (real 1.5.2 registered these in the base RenderManager ctor; our shim only
     * registered RenderItem + RenderXPOrb). Uses reflection for the entity class so we
     * don't need a Modern-Common stub for every one (EntitySnowball has none), and skips
     * any that already have a renderer.
     */
    private static void registerMissingVanillaRenderers() {
        reg("btw.modern.EntityArrow", new btw.modern.RenderArrow());
        reg("btw.modern.EntityTNTPrimed", new btw.modern.RenderTNTPrimed());
        reg("btw.modern.EntityFishHook", new btw.modern.RenderFish());
        reg("btw.modern.EntitySnowball", new btw.modern.RenderSnowball(btw.modern.Item.snowball));
        reg("btw.modern.EntityEgg", new btw.modern.RenderSnowball(btw.modern.Item.egg));
    }

    private static void reg(String entityClassName, btw.modern.Render renderer) {
        try {
            Class<?> entityClass = Class.forName(entityClassName);
            if (btw.modern.RenderManager.instance.getEntityClassRenderObject(entityClass) == null) {
                btw.modern.RenderManager.AddEntityRenderer(entityClass, renderer);
                LOGGER.info("Registered vanilla renderer {} for {}",
                        renderer.getClass().getSimpleName(), entityClass.getSimpleName());
            }
        } catch (Throwable t) {
            LOGGER.warn("Could not register {} for {}: {}",
                    renderer.getClass().getSimpleName(), entityClassName, t.toString());
        }
    }

    @Override
    public void render(Entity entity, float yaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        // Extract FC entity from any proxy type.
        // IMPORTANT: call getFcClassName() FIRST — it populates fcClassName
        // via EntityType fallback, which getFcEntity() needs to create the
        // client-side FC entity.
        btw.modern.Entity fcEntity = null;
        String fcClassName = "";
        if (entity instanceof ProxyEntity pe) {
            fcClassName = pe.getFcClassName();
            fcEntity = pe.getFcEntity();
        } else if (entity instanceof ProxyMob pm) {
            fcClassName = pm.getFcClassName();
            fcEntity = pm.getFcEntity();
        } else if (entity instanceof ProxyAnimal pa) {
            fcClassName = pa.getFcClassName();
            fcEntity = pa.getFcEntity();
        } else if (entity instanceof ProxyPathfinderMob pp) {
            fcClassName = pp.getFcClassName();
            fcEntity = pp.getFcEntity();
        } else {
            return;
        }

        // Find the FC renderer — try entity class first, fall back to className lookup
        btw.modern.Render fcRenderer = null;
        if (fcEntity != null) {
            fcRenderer = findRenderer(fcEntity.getClass());
        }
        if (fcRenderer == null && !fcClassName.isEmpty()) {
            try {
                fcRenderer = findRenderer(Class.forName(fcClassName));
            } catch (ClassNotFoundException ignored) {}
        }

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
        } catch (Throwable e) {
            if (logOnce(entity.getId() + 100000)) {
                LOGGER.warn("FC doRender failed for {} (renderer={}): {}",
                        fcEntity.getClass().getSimpleName(), fcRenderer.getClass().getSimpleName(),
                        e.toString(), e);
            }
        }

        List<btw.modern.Tessellator.CapturedQuad> quads = tess.stopCapturing();
        btw.modern.GL11.disableMatrixTracking();

        if (logOnce(entity.getId() + 200000)) {
            LOGGER.info("FC render result: {} quads for {} (renderer={})",
                    quads.size(), fcEntity.getClass().getSimpleName(),
                    fcRenderer.getClass().getSimpleName());
            // Log first quad's vertex positions for debugging
            for (btw.modern.Tessellator.CapturedQuad q : quads) {
                if (q != null && q.vertices != null && q.vertices[0] != null) {
                    LOGGER.info("  First quad v0: pos=({},{},{}) uv=({},{}) color=({},{},{},{})",
                            String.format("%.2f", q.vertices[0].x), String.format("%.2f", q.vertices[0].y),
                            String.format("%.2f", q.vertices[0].z),
                            String.format("%.2f", q.vertices[0].u), String.format("%.2f", q.vertices[0].v),
                            String.format("%.2f", q.vertices[0].r), String.format("%.2f", q.vertices[0].g),
                            String.format("%.2f", q.vertices[0].b), String.format("%.2f", q.vertices[0].a));
                    break;
                }
            }
        }

        if (quads.isEmpty()) {
            renderDebugBox(poseStack, bufferSource, packedLight, entity);
            return;
        }

        // Render captured quads through MC's pipeline
        poseStack.pushPose();

        // FC models are authored facing south (+Z in 1.5.2's GL coordinate
        // system). MC 1.20.1's entity render pipeline faces north. Rotate
        // 180° around Y to flip the model to face the correct direction.
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));

        // Determine texture from captured quads — FC renderers bind textures
        // via loadTexture() which records the name on the Tessellator.
        ResourceLocation texLoc = FALLBACK_TEXTURE;
        String foundTex = null;
        for (btw.modern.Tessellator.CapturedQuad q : quads) {
            if (q != null && q.textureName != null && !q.textureName.isEmpty()) {
                foundTex = q.textureName;
                texLoc = resolveEntityTexture(foundTex);
                break;
            }
        }
        if (logOnce(entity.getId() + 300000)) {
            LOGGER.info("FC entity texture: {} → {} for {}", foundTex, texLoc, fcEntity.getClass().getSimpleName());
        }

        RenderType renderType = RenderType.entityCutoutNoCull(texLoc);
        VertexConsumer consumer = bufferSource.getBuffer(renderType);
        Matrix4f mat = poseStack.last().pose();

        for (btw.modern.Tessellator.CapturedQuad quad : quads) {
            if (quad == null || quad.vertices == null) continue;
            boolean valid = true;
            for (int i = 0; i < 4; i++) {
                if (quad.vertices[i] == null) { valid = false; break; }
            }
            if (!valid) continue;
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
        return FALLBACK_TEXTURE;
    }

    /**
     * Resolves an FC texture path (e.g. "/mob/cow.png", "/btwmodtex/fcwindmillent.png")
     * to an MC 1.20.1 ResourceLocation.
     */
    private static final Map<String, ResourceLocation> textureCache = new ConcurrentHashMap<>();

    private static ResourceLocation resolveEntityTexture(String fcPath) {
        return textureCache.computeIfAbsent(fcPath, path -> {
            // Strip leading slash
            String clean = path.startsWith("/") ? path.substring(1) : path;

            // MC 1.20.1 requires all-lowercase ResourceLocation paths
            String lower = clean.toLowerCase(java.util.Locale.ROOT);

            // FC BTW textures: btwmodtex/foo.png → betterthanwolves:textures/entity/foo.png
            if (lower.startsWith("btwmodtex/")) {
                String name = lower.substring("btwmodtex/".length());
                return new ResourceLocation(BTWForgeMod.MOD_ID, "textures/entity/" + name);
            }

            // Bare FC Icon name (no path separator, no extension) — comes from NamedIcon,
            // not a loadTexture() path. Used by item-icon billboards (the soul urn draws
            // Item.itemsList[m_iItemShiftedIndex].itemIcon; likewise thrown snowball/egg)
            // and RenderBlocks block icons. FC ships these as individual 16x16 files under
            // textures/item/ (or textures/block/ for fcBlock* icons), and the icon's 0..1
            // UVs map the whole file. Without this they resolved to "textures/<name>" (no
            // subdir, no .png) and fell back to the placeholder — the "soul urns don't
            // render properly" bug.
            if (!lower.contains("/") && !lower.endsWith(".png")) {
                String subdir = lower.startsWith("fcblock") ? "block/" : "item/";
                return new ResourceLocation(BTWForgeMod.MOD_ID, "textures/" + subdir + lower + ".png");
            }

            // All FC texture paths (mob/, armor/, item/, etc.) use bundled
            // 1.5.2 originals. MC 1.20.1 textures have different dimensions
            // (64x64 vs 64x32) and reorganized paths that break FC's UV maps.
            return new ResourceLocation(BTWForgeMod.MOD_ID, "textures/" + lower);
        });
    }
}
