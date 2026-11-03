package btw.forge.client;

import btw.forge.BTWForgeMod;
import btw.forge.NamedIcon;
import btw.forge.ProxyAnimal;
import btw.forge.ProxyEntity;
import btw.forge.ProxyMob;
import btw.forge.ProxyPathfinderMob;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generic entity renderer that captures FC's doRender() vertex output
 * via the Tessellator and renders it through MC 1.20.1's pipeline.
 *
 * <p>FC entity renderers use GL11 transforms (translate/rotate/scale) and
 * ModelBase/Tessellator drawing. Our GL11 tracks a real matrix stack, and
 * the Tessellator applies it to vertex positions. This renderer captures
 * the transformed vertices and pipes them to MC's VertexConsumer.</p>
 *
 * <p>Each captured quad also carries the GL state it was drawn with (texture,
 * blending, lightmap override), so the quads are batched per texture and blend
 * mode into the matching RenderType, and the 1.5.2 hurt/death red and white
 * flash become MC overlay coords.</p>
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

        auditRendererCoverage();
    }

    /**
     * Startup self-audit: every registered FC entity type must resolve to a real FC renderer,
     * else it renders as the debug box (invisible-looking). This is exactly the class of bug
     * that made fc_xp_orb / fc_arrow / etc. invisible — now caught at launch instead of in
     * game. Runs right after the renderer map is populated.
     */
    private static void auditRendererCoverage() {
        try {
            Map<net.minecraft.world.entity.EntityType<?>, String> registered =
                    btw.forge.BTWEntityRegistration.getRegisteredFcEntities();
            int missing = 0;
            int notBuilt = 0;
            java.util.TreeSet<String> positional = new java.util.TreeSet<>();
            for (Map.Entry<net.minecraft.world.entity.EntityType<?>, String> e : registered.entrySet()) {
                if (!hasRendererForFcClass(e.getValue())) {
                    LOGGER.warn("[SELF-AUDIT/renderer] {} ({}) has NO FC renderer -> renders as debug box",
                            net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(e.getKey()),
                            e.getValue());
                    missing++;
                }
                // FCMOD-INTEGRATION(1.20.1): fc_entity_construction: a renderer draws nothing the client cannot build (CF2-R1 passed this audit)
                String why = clientBuildProblem(e.getValue());
                if (why != null) {
                    LOGGER.warn("[SELF-AUDIT/renderer] {} ({}) cannot be built on the client ({}) -> renders as debug box",
                            net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(e.getKey()),
                            e.getValue(), why);
                    notBuilt++;
                } else if (btw.forge.FcEntityFactory.kindOf(e.getValue()) == btw.forge.FcEntityFactory.Kind.POSITIONAL) {
                    positional.add(e.getValue().substring(e.getValue().lastIndexOf('.') + 1));
                }
            }
            LOGGER.info("[SELF-AUDIT/renderer] {} registered FC entities, {} missing a renderer (0 expected), {} not "
                    + "constructible on the client (0 expected); built from their spawn state by a positional "
                    + "constructor: {}", registered.size(), missing, notBuilt, positional);
        } catch (Throwable t) {
            LOGGER.warn("[SELF-AUDIT/renderer] audit failed: {}", t.toString());
        }
    }

    /**
     * Null when this FC class's proxy can build it on the client (FcEntityFactory; living proxies need WORLD or
     * NO_ARG). A POSITIONAL class is also built once here, against a level-less client FC world, because that is
     * the one construction path no earlier code exercised: its constructor must not need the world or real fields.
     * WORLD and NO_ARG classes are only resolved (FC mob constructors build AI and DataWatcher state).
     */
    private static String clientBuildProblem(String fcClassName) {
        btw.forge.FcEntityFactory f = btw.forge.FcEntityFactory.of(fcClassName);
        if (!f.usable()) return f.failure();
        if (f.kind() != btw.forge.FcEntityFactory.Kind.POSITIONAL) return null;
        try {
            if (btw.modern.EntityLiving.class.isAssignableFrom(Class.forName(fcClassName))) {
                return "a living FC entity needs a (World) or no-argument constructor";
            }
            f.create(btw.forge.ProxyEntity.createDummyClientWorld(null), 0.0D, 0.0D, 0.0D);
            return null;
        } catch (Throwable t) {
            return btw.forge.FcEntityFactory.rootCause(t).toString();
        }
    }

    /**
     * Does a registered FC entity class resolve to a renderer? Mirrors the render path's
     * class-hierarchy walk (an entity inherits its superclass's renderer). Used by the audit.
     */
    public static boolean hasRendererForFcClass(String fcClassName) {
        if (fcClassName == null || fcClassName.isEmpty()) return false;
        try {
            Class<?> c = Class.forName(fcClassName);
            while (c != null && c != Object.class) {
                if (fcRenderers.containsKey(c.getName())) return true;
                c = c.getSuperclass();
            }
        } catch (Throwable t) {
            return fcRenderers.containsKey(fcClassName);
        }
        return false;
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
        // 1.5.2 RenderManager.java:103. The frozen EntityFallingSand is what Block.CreateFallingBlockEntity spawns
        // (on the generic fc_entity type) when FCEntityFallingBlock cannot be built (Block.java:771-778).
        reg("btw.modern.EntityFallingSand", new btw.modern.RenderFallingSand());
        // 1.5.2 RenderManager.java:65/:80. FC spawns these frozen vanilla mobs directly (the soul-urn
        // golem, FCEntityUrn.java:370; silverfish from FCBlockSilverfish/FCEntityCow/FCTileEntityBeacon)
        // as betterthanwolves:fc_iron_golem / fc_silverfish.
        reg("btw.modern.EntityIronGolem", btw.modern.RenderIronGolem::new);
        reg("btw.modern.EntitySilverfish", btw.modern.RenderSilverfish::new);
    }

    /** As {@link #reg(String, btw.modern.Render)}, but a failing renderer constructor is logged, not thrown. */
    private static void reg(String entityClassName, java.util.function.Supplier<? extends btw.modern.Render> factory) {
        btw.modern.Render renderer;
        try {
            renderer = factory.get();
        } catch (Throwable t) {
            LOGGER.warn("Could not create the vanilla renderer for {}: {}", entityClassName, t.toString());
            return;
        }
        reg(entityClassName, renderer);
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

        if (fcEntity == null || fcRenderer == null) {
            renderDebugBox(poseStack, bufferSource, packedLight, entity);
            return;
        }

        // Enable GL11 matrix tracking and capture Tessellator output. startCapturing also resets
        // the thread's GL11 colour/blend/texture/lightmap state, as 1.5.2 RenderManager.renderEntity
        // did per entity (glColor4f(1,1,1,1) + lightmap, RenderManager.java:203-204), so a colour
        // left by an earlier capture (a dragon orb, a block-face shade) can't tint this one.
        btw.modern.GL11.enableMatrixTracking();
        btw.modern.Tessellator tess = btw.modern.Tessellator.instance;
        tess.startCapturing();

        boolean renderFailed = false;
        try {
            fcRenderer.doRender(fcEntity, 0, 0, 0, yaw, partialTick);
        } catch (Throwable e) {
            renderFailed = true;
            // Once per (FC class, exception type): per-entity-id keys grew without bound.
            if (loggedKeys.add("doRender:" + fcEntity.getClass().getName() + '|' + e.getClass().getName())) {
                LOGGER.warn("FC doRender failed for {} (renderer={}): {}",
                        fcEntity.getClass().getSimpleName(), fcRenderer.getClass().getSimpleName(),
                        e.toString(), e);
            }
        }

        List<btw.modern.Tessellator.CapturedQuad> quads = tess.stopCapturing();
        boolean legacyFlip = btw.modern.GL11.usedLegacyEntityFlip();
        btw.modern.GL11.disableMatrixTracking();

        if (quads.isEmpty()) {
            // A renderer that ran cleanly may draw nothing: 1.5.2 RenderLiving.renderModel draws no
            // geometry for an invisible mob (RenderLiving.java:284-287). Only a failed render gets the
            // locator box (a missing renderer is handled above).
            if (renderFailed) {
                renderDebugBox(poseStack, bufferSource, packedLight, entity);
            }
            return;
        }

        // Render captured quads through MC's pipeline
        poseStack.pushPose();
        try {
            emitCaptured(entity, fcEntity, fcRenderer, quads, legacyFlip, partialTick,
                    poseStack, bufferSource, packedLight);
        } catch (Throwable t) {
            if (loggedKeys.add("emit:" + fcRenderer.getClass().getName())) {
                LOGGER.warn("Emitting captured quads failed for {} (renderer={}): {}",
                        fcEntity.getClass().getSimpleName(), fcRenderer.getClass().getSimpleName(), t.toString(), t);
            }
        } finally {
            poseStack.popPose();
        }
    }

    private void emitCaptured(Entity entity, btw.modern.Entity fcEntity, btw.modern.Render fcRenderer,
                              List<btw.modern.Tessellator.CapturedQuad> quads, boolean legacyFlip, float partialTick,
                              PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        // 1.5.2's GL frame at doRender and 1.20.1's entity PoseStack are the same (world axes,
        // origin at the entity; LivingEntityRenderer.setupRotations = 1.5.2 rotateCorpse +
        // glScalef(-1,-1,1)), so captures need no extra turn. Only the pre-port RenderLiving
        // stand-in drew models with glScalef(1,-1,-1), 1.5.2's flip turned 180 degrees about Y;
        // undo that turn for captures that used it. (This used to be applied to every capture,
        // which turned arrows, orbs, windmills etc. around.)
        if (legacyFlip) {
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
        }

        // Group the captured quads into one MC batch per (texture, 1.5.2 blend mode, light
        // override): a mob plus its armour, held item, wool or glowing eyes binds several
        // textures with different GL state (1.5.2 RenderLiving.java:131-189 render passes and
        // renderEquippedItems).
        int overlay = computeOverlay(fcEntity, fcRenderer, partialTick);
        String fallbackTexName = null;
        for (btw.modern.Tessellator.CapturedQuad q : quads) {
            if (q != null && q.textureName != null && !q.textureName.isEmpty()) {
                fallbackTexName = q.textureName;
                break;
            }
        }
        Map<String, TexRef> frameTextures = new HashMap<>();
        Map<GroupKey, List<Resolved>> groups = new LinkedHashMap<>();
        int skippedUntextured = 0;
        for (btw.modern.Tessellator.CapturedQuad q : quads) {
            if (!isComplete(q)) continue;
            if (!q.textured) {
                // GL_TEXTURE_2D off: only additive untextured geometry (FCClientRenderLightningBolt,
                // SRC_ALPHA/ONE) has an MC equivalent, RenderType.lightning().
                if (q.blend && q.blendDst == btw.modern.GL11.GL_ONE) {
                    groups.computeIfAbsent(new GroupKey(null, Blend.UNTEXTURED_ADDITIVE, -1, 0, 0, false),
                            k -> new ArrayList<>()).add(new Resolved(q, null));
                } else {
                    skippedUntextured++;
                }
                continue;
            }
            String texName = q.textureName != null && !q.textureName.isEmpty() ? q.textureName : fallbackTexName;
            TexRef ref = frameTextures.computeIfAbsent(texName != null ? texName : "", FCEntityRenderer::resolveTexRef);
            Blend blend = blendOf(q, texName);
            boolean iconSource = isIconTexture(texName);
            GroupKey key;
            if (blend == Blend.GLINT) {
                key = new GroupKey(null, Blend.GLINT, -1, 0, 0, false);
            } else if (blend == Blend.ADDITIVE) {
                key = new GroupKey(ref.location(), blend, q.lightmap, q.texOffsetU, q.texOffsetV, iconSource);
            } else {
                key = new GroupKey(ref.location(), blend, q.lightmap, 0, 0, iconSource);
            }
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(new Resolved(q, ref.sprite()));
        }
        if (skippedUntextured > 0 && loggedKeys.add("untextured:" + fcRenderer.getClass().getName())) {
            LOGGER.debug("Skipped {} untextured quads from {} (GL_TEXTURE_2D off, not additive; no MC equivalent)",
                    skippedUntextured, fcRenderer.getClass().getSimpleName());
        }
        // Once per (FC class, batch count) rather than per entity id, so the key set stays bounded.
        if (loggedKeys.add("batches:" + fcEntity.getClass().getName() + '#' + groups.size())) {
            LOGGER.info("FC entity batches for {}: {}", fcEntity.getClass().getSimpleName(), groups.keySet());
        }

        Matrix4f mat = poseStack.last().pose();
        Matrix3f normalMat = poseStack.last().normal();
        // FCMOD-INTEGRATION(1.20.1): falling_blocks: a falling block is shaded as a placed block, as 1.5.2 (RenderFallingSand with
        // GL_LIGHTING off, RenderStandardFallingBlock's 0.5/1/0.8/0.6 faces) and 1.20.1 (FallingBlockRenderer through the block
        // model renderer) both draw it, not by the entity render type's diffuse light
        boolean blockShaded = fcRenderer instanceof btw.modern.RenderFallingSand;
        for (Map.Entry<GroupKey, List<Resolved>> group : groups.entrySet()) {
            GroupKey key = group.getKey();
            switch (key.blend()) {
                case UNTEXTURED_ADDITIVE -> emitPositionColor(bufferSource.getBuffer(RenderType.lightning()), mat, group.getValue());
                case GLINT -> emitPositionTex(bufferSource.getBuffer(RenderType.armorEntityGlint()), mat, group.getValue());
                // 1.5.2's red/flash passes re-render only mainModel and the inheritRenderPass models,
                // never renderEquippedItems (RenderLiving.java:189 vs :196-234): icon-drawn quads
                // (held item, carried block, snowman pumpkin) get no overlay.
                default -> emitEntity(bufferSource.getBuffer(renderTypeFor(key)), mat, normalMat, group.getValue(),
                        key.blend() == Blend.ADDITIVE, lightFor(key.lightmap(), packedLight),
                        key.iconSource() ? OverlayTexture.NO_OVERLAY : overlay, blockShaded);
            }
        }
    }

    /**
     * True for quads drawn from an Icon rather than an entity texture: a bare Icon name (NamedIcon,
     * RenderBlocks faces) or a bind of 1.5.2's icon sheets. In a living renderer only
     * renderEquippedItems draws those.
     */
    private static boolean isIconTexture(String texName) {
        if (texName == null || texName.isEmpty()) return false;
        String lower = texName.toLowerCase(Locale.ROOT);
        if (lower.equals("/terrain.png") || lower.equals("/gui/items.png")) return true;
        return lower.indexOf('/') < 0 && !lower.endsWith(".png");
    }

    /**
     * Light for a batch. A setLightmapTextureCoords override from the capture (eyes passes:
     * block light 15) is honoured per channel but never made darker than the host light: the
     * other 1.5.2 callers pass getBrightnessForRender of the client-side FC entity, whose world
     * lookup can come back 0 where MC's own packed light is right.
     */
    private static int lightFor(int captured, int packedLight) {
        if (captured < 0) return packedLight;
        int block = Math.max(captured & 0xFFFF, packedLight & 0xFFFF);
        int sky = Math.max(captured >>> 16 & 0xFFFF, packedLight >>> 16 & 0xFFFF);
        return block | sky << 16;
    }

    // ================================================================
    // Captured-quad batching (1.5.2 GL state -> 1.20.1 RenderType)
    // (package-private: FCBlockEntityRenderer batches TESR captures the same way)
    // ================================================================

    /** How a quad was blended in 1.5.2, from GL_BLEND + glBlendFunc at draw time. */
    enum Blend { OPAQUE, TRANSLUCENT, ADDITIVE, GLINT, UNTEXTURED_ADDITIVE }

    /**
     * One MC batch. texture is null for the texture-less types (lightning, glint). iconSource:
     * drawn from an Icon (equipped item / carried block), which takes no hurt or flash overlay.
     */
    private record GroupKey(ResourceLocation texture, Blend blend, int lightmap, float swirlU, float swirlV,
                            boolean iconSource) {}

    /** A resolved FC texture: a standalone texture, or (sprite != null) an Icon in the block atlas. */
    private record TexRef(ResourceLocation location, TextureAtlasSprite sprite) {}

    /** A captured quad plus the atlas sprite its 0..1 icon UVs map into (null: standalone texture). */
    record Resolved(btw.modern.Tessellator.CapturedQuad quad, TextureAtlasSprite sprite) {}

    static boolean isComplete(btw.modern.Tessellator.CapturedQuad q) {
        if (q == null || q.vertices == null) return false;
        for (int i = 0; i < 4; i++) {
            if (q.vertices[i] == null) return false;
        }
        return true;
    }

    static Blend blendOf(btw.modern.Tessellator.CapturedQuad q, String texName) {
        if (!q.blend) return Blend.OPAQUE;
        // 1.5.2 RenderLiving.java:146-178 enchantment glint ("%blur%/misc/glint.png", SRC_COLOR/ONE)
        if (texName != null && texName.endsWith("misc/glint.png")) return Blend.GLINT;
        // ONE/ONE: spider/enderman/wolf eyes (RenderSpider.java:49), creeper/wither swirl (RenderCreeper.java:117)
        if (q.blendDst == btw.modern.GL11.GL_ONE) return Blend.ADDITIVE;
        if (q.blendSrc == btw.modern.GL11.GL_ONE && q.blendDst == btw.modern.GL11.GL_ZERO) return Blend.OPAQUE;
        // SRC_ALPHA/ONE_MINUS_SRC_ALPHA: slime outer layer (RenderSlime.java:42-43)
        return Blend.TRANSLUCENT;
    }

    private static RenderType renderTypeFor(GroupKey key) {
        ResourceLocation tex = key.texture() != null ? key.texture() : FALLBACK_TEXTURE;
        switch (key.blend()) {
            case TRANSLUCENT:
                return RenderType.entityTranslucent(tex);
            case ADDITIVE:
                // A GL_TEXTURE-matrix scroll (power/wither-armour swirl) maps to MC's energy swirl.
                if (key.swirlU() != 0.0F || key.swirlV() != 0.0F) {
                    return RenderType.energySwirl(tex, key.swirlU() % 1.0F, key.swirlV() % 1.0F);
                }
                return RenderType.eyes(tex);
            case OPAQUE:
            default:
                return RenderType.entityCutoutNoCull(tex);
        }
    }

    /** NEW_ENTITY-format batches (cutout, translucent, eyes, energy swirl). */
    static void emitEntity(VertexConsumer consumer, Matrix4f mat, Matrix3f normalMat, List<Resolved> quads,
                           boolean additive, int light, int overlay) {
        emitEntity(consumer, mat, normalMat, quads, additive, light, overlay, false);
    }

    /**
     * As above. blockShaded (falling blocks, CF2-R1): each vertex colour takes the block face shade of its normal
     * (1.5.2 RenderBlocks and 1.20.1 Level.getShade: 0.5 down, 1.0 up, 0.8 north/south, 0.6 east/west) and is
     * emitted with an up normal, whose entity diffuse factor is 1, so the quad is lit like a placed block.
     */
    static void emitEntity(VertexConsumer consumer, Matrix4f mat, Matrix3f normalMat, List<Resolved> quads,
                           boolean additive, int light, int overlay, boolean blockShaded) {
        for (Resolved r : quads) {
            btw.modern.Tessellator.CapturedQuad q = r.quad();
            // eyes()/energySwirl() blend ONE/ONE; 1.5.2 SRC_ALPHA/ONE weighs the source by alpha.
            boolean premultiply = additive && q.blendSrc == btw.modern.GL11.GL_SRC_ALPHA;
            for (int i = 0; i < 4; i++) {
                btw.modern.Tessellator.CapturedVertex v = q.vertices[i];
                float red = v.r, green = v.g, blue = v.b, alpha = v.a;
                if (premultiply) {
                    red *= alpha; green *= alpha; blue *= alpha;
                }
                float nx = v.nx, ny = v.ny, nz = v.nz;
                if (blockShaded) {
                    float shade = blockFaceShade(nx, ny, nz);
                    red *= shade; green *= shade; blue *= shade;
                    nx = 0.0F; ny = 1.0F; nz = 0.0F;
                }
                float u = (float) v.u, tv = (float) v.v;
                if (r.sprite() != null) {
                    u = r.sprite().getU(v.u * 16.0);
                    tv = r.sprite().getV(v.v * 16.0);
                }
                consumer.vertex(mat, (float) v.x, (float) v.y, (float) v.z)
                        .color(red, green, blue, alpha)
                        .uv(u, tv)
                        .overlayCoords(overlay)
                        .uv2(light)
                        .normal(normalMat, nx, ny, nz)
                        .endVertex();
            }
        }
    }

    /** 1.5.2's block face shade (RenderBlocks.RenderStandardFallingBlock, vanilla/client RenderBlocks.java:10570-10573) by normal. */
    static float blockFaceShade(float nx, float ny, float nz) {
        float ax = Math.abs(nx), ay = Math.abs(ny), az = Math.abs(nz);
        if (ay >= ax && ay >= az) return ny < 0.0F ? 0.5F : 1.0F;
        return az >= ax ? 0.8F : 0.6F;
    }

    /** RenderType.lightning() (POSITION_COLOR, SRC_ALPHA/ONE). */
    private static void emitPositionColor(VertexConsumer consumer, Matrix4f mat, List<Resolved> quads) {
        for (Resolved r : quads) {
            btw.modern.Tessellator.CapturedQuad q = r.quad();
            boolean sourceOne = q.blendSrc == btw.modern.GL11.GL_ONE;
            for (int i = 0; i < 4; i++) {
                btw.modern.Tessellator.CapturedVertex v = q.vertices[i];
                consumer.vertex(mat, (float) v.x, (float) v.y, (float) v.z)
                        .color(v.r, v.g, v.b, sourceOne ? 1.0F : v.a)
                        .endVertex();
            }
        }
    }

    /** RenderType.armorEntityGlint() (POSITION_TEX; MC supplies the glint texture and scroll). */
    static void emitPositionTex(VertexConsumer consumer, Matrix4f mat, List<Resolved> quads) {
        for (Resolved r : quads) {
            for (int i = 0; i < 4; i++) {
                btw.modern.Tessellator.CapturedVertex v = r.quad().vertices[i];
                consumer.vertex(mat, (float) v.x, (float) v.y, (float) v.z)
                        .uv((float) v.u, (float) v.v)
                        .endVertex();
            }
        }
    }

    /**
     * MC overlay coords for a living FC entity. 1.5.2 drew a red pass while hurtTime > 0 or
     * deathTime > 0 (RenderLiving.java:196-217) and a getColorMultiplier white pass (creeper
     * fuse flash); the capture does not draw those as geometry, MC's OverlayTexture does the
     * same job (LivingEntityRenderer.getOverlayCoords).
     */
    private static int computeOverlay(btw.modern.Entity fcEntity, btw.modern.Render fcRenderer, float partialTick) {
        if (!(fcEntity instanceof btw.modern.EntityLiving living)
                || !(fcRenderer instanceof btw.modern.RenderLiving renderLiving)) {
            return OverlayTexture.NO_OVERLAY;
        }
        float whiteness = 0.0F;
        try {
            whiteness = renderLiving.getOverlayWhiteness(living, partialTick);
        } catch (Throwable t) {
            if (loggedKeys.add("whiteness:" + fcRenderer.getClass().getName())) {
                LOGGER.warn("getOverlayWhiteness failed for {}: {}", fcRenderer.getClass().getSimpleName(), t.toString());
            }
        }
        whiteness = Math.max(0.0F, Math.min(1.0F, whiteness));
        boolean hurt = living.hurtTime > 0 || living.deathTime > 0;
        return OverlayTexture.pack(OverlayTexture.u(whiteness), OverlayTexture.v(hurt));
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

    /** Once-only log keys (render/emit/overlay failures, unknown textures, skipped geometry, batch layouts). */
    private static final java.util.Set<String> loggedKeys = ConcurrentHashMap.newKeySet();

    /**
     * Resolves an FC texture name recorded on a captured quad. A bare Icon name (NamedIcon:
     * held items, item billboards, block-as-item faces) uses FC's own 16x16 file when one is
     * bundled, else the 1.20.1 block-atlas sprite (vanilla item/terrain icons such as a
     * skeleton's "bow"); anything else is a texture path.
     */
    private static TexRef resolveTexRef(String name) {
        if (name == null || name.isEmpty()) return new TexRef(FALLBACK_TEXTURE, null);
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.indexOf('/') < 0 && !lower.endsWith(".png")) {
            Optional<ResourceLocation> file = iconFileCache.computeIfAbsent(lower, FCEntityRenderer::findIconFile);
            if (file.isPresent()) return new TexRef(file.get(), null);
            try {
                // Block and item textures share the block atlas in 1.20.1. lookupSprite maps
                // 1.5.2 terrain and item icon names (VANILLA_TEXTURE_MAP, VANILLA_ITEM_TEXTURE_MAP:
                // swordIron -> iron_sword) and logs unknown ones once.
                TextureAtlasSprite sprite = FCBakedModel.lookupSprite(lower);
                if (sprite != null) return new TexRef(TextureAtlas.LOCATION_BLOCKS, sprite);
            } catch (Throwable t) {
                if (loggedKeys.add("icon:" + lower)) {
                    LOGGER.warn("FC icon '{}' could not be resolved in the block atlas: {}", lower, t.toString());
                }
            }
        }
        return new TexRef(resolveEntityTexture(name), null);
    }

    /** Bare Icon name -> its bundled 16x16 file, if FC ships one. */
    private static final Map<String, Optional<ResourceLocation>> iconFileCache = new ConcurrentHashMap<>();

    private static Optional<ResourceLocation> findIconFile(String lowerIconName) {
        // FC ships its icons as individual files under textures/item/ (textures/block/ for
        // fcBlock* icons) and the icon's 0..1 UVs map the whole file (the soul urn draws
        // Item.itemsList[m_iItemShiftedIndex].itemIcon this way; likewise thrown snowball/egg).
        // FC's icon names all start with "fc"; the other files there are bundled 1.5.2 /item/*.png
        // entity sheets (boat.png, book.png, sign.png, ...) that must not stand in for the vanilla
        // "boat"/"book"/"sign" item icons, which resolve through the block atlas instead.
        if (!lowerIconName.startsWith("fc")) return Optional.empty();
        try {
            String subdir = lowerIconName.startsWith("fcblock") ? "block/" : "item/";
            ResourceLocation loc = new ResourceLocation(BTWForgeMod.MOD_ID, "textures/" + subdir + lowerIconName + ".png");
            return resourceExists(loc) ? Optional.of(loc) : Optional.empty();
        } catch (Throwable t) {
            return Optional.empty();
        }
    }

    /**
     * Resolves an FC texture path (e.g. "/mob/cow.png", "/btwmodtex/fcwindmillent.png")
     * to an MC 1.20.1 ResourceLocation. Unknown paths are logged once.
     */
    private static final Map<String, ResourceLocation> textureCache = new ConcurrentHashMap<>();

    private static ResourceLocation resolveEntityTexture(String fcPath) {
        return textureCache.computeIfAbsent(fcPath, FCEntityRenderer::computeEntityTexture);
    }

    private static ResourceLocation computeEntityTexture(String path) {
        try {
            // 1.5.2 RenderEngine name prefixes only select filtering ("%blur%/misc/glint.png", "%clamp%...")
            String clean = path.replace("%blur%", "").replace("%clamp%", "");
            // Strip leading slash
            if (clean.startsWith("/")) clean = clean.substring(1);

            // MC 1.20.1 requires all-lowercase ResourceLocation paths
            String lower = clean.toLowerCase(Locale.ROOT);

            ResourceLocation bundled;
            if (lower.startsWith("btwmodtex/")) {
                // FC BTW textures: btwmodtex/foo.png → betterthanwolves:textures/entity/foo.png
                bundled = new ResourceLocation(BTWForgeMod.MOD_ID, "textures/entity/" + lower.substring("btwmodtex/".length()));
            } else if (!lower.contains("/") && !lower.endsWith(".png")) {
                // Bare icon name with no bundled file and no atlas sprite (see resolveTexRef)
                String subdir = lower.startsWith("fcblock") ? "block/" : "item/";
                bundled = new ResourceLocation(BTWForgeMod.MOD_ID, "textures/" + subdir + lower + ".png");
            } else {
                // All FC texture paths (mob/, armor/, item/, etc.) use bundled
                // 1.5.2 originals. MC 1.20.1 textures have different dimensions
                // (64x64 vs 64x32) and reorganized paths that break FC's UV maps.
                bundled = new ResourceLocation(BTWForgeMod.MOD_ID, "textures/" + lower);
            }
            if (resourceExists(bundled)) return bundled;

            ResourceLocation vanilla = vanillaFallback(lower);
            if (vanilla != null && resourceExists(vanilla)) return vanilla;

            LOGGER.warn("FC texture '{}' not found (tried {}{}); it renders as the missing texture",
                    path, bundled, vanilla != null ? " and " + vanilla : "");
            return bundled;
        } catch (Throwable t) {
            LOGGER.warn("FC texture '{}' is not a usable resource path ({}); using the placeholder", path, t.toString());
            return FALLBACK_TEXTURE;
        }
    }

    /** 1.5.2 armour layer path: /armor/&lt;material&gt;_&lt;1|2&gt;.png, "_b" = the leather overlay layer. */
    private static final Pattern ARMOR_LAYER = Pattern.compile("armor/([a-z]+)_([12])(_b)?\\.png");

    /** 1.20.1 equivalents for 1.5.2 paths, used only if the bundled 1.5.2 original is missing. */
    private static ResourceLocation vanillaFallback(String lowerPath) {
        Matcher armor = ARMOR_LAYER.matcher(lowerPath);
        if (armor.matches()) {
            // RenderBiped.java:68/:145 bipedArmorFilenamePrefix {cloth, chain, iron, diamond, gold}
            String material = switch (armor.group(1)) {
                case "cloth" -> "leather";
                case "chain" -> "chainmail";
                default -> armor.group(1);
            };
            return new ResourceLocation("minecraft", "textures/models/armor/" + material + "_layer_" + armor.group(2)
                    + (armor.group(3) != null ? "_overlay" : "") + ".png");
        }
        switch (lowerPath) {
            case "armor/power.png": return new ResourceLocation("minecraft", "textures/entity/creeper/creeper_armor.png");
            case "armor/witherarmor.png": return new ResourceLocation("minecraft", "textures/entity/wither/wither_armor.png");
            case "misc/glint.png": return new ResourceLocation("minecraft", "textures/misc/enchanted_glint_entity.png");
            case "mob/sheep_fur.png": return new ResourceLocation("minecraft", "textures/entity/sheep/sheep_fur.png");
            case "mob/spider_eyes.png": return new ResourceLocation("minecraft", "textures/entity/spider_eyes.png");
            case "mob/enderman_eyes.png": return new ResourceLocation("minecraft", "textures/entity/enderman/enderman_eyes.png");
            default: return null;
        }
    }

    private static boolean resourceExists(ResourceLocation loc) {
        try {
            return Minecraft.getInstance().getResourceManager().getResource(loc).isPresent();
        } catch (Throwable t) {
            return true; // can't tell: keep the bundled path, as before
        }
    }
}
