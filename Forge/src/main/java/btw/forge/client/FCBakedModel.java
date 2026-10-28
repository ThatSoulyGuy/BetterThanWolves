package btw.forge.client;

import btw.forge.BTWForgeMod;
import btw.forge.NamedIcon;
import btw.forge.ProxyBlock;
import btw.forge.ProxyBlockEntity;
import btw.forge.ProxyRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.client.model.geometry.UnbakedGeometryHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A BakedModel that renders an FC block through FC's own {@code RenderBlock}.
 *
 * <p>Created during model baking with just a reference to the FC block. On first
 * use it runs FC's {@code RenderBlock} once per metadata value through the
 * Tessellator capture pipeline against a fake world ({@link StaticAccess}) and
 * converts the captured vertices to BakedQuads. Those static quads serve every
 * meta whose output cannot vary with position.
 *
 * <p>The block's item ({@code <block>#inventory}) is served through {@link #getOverrides}: per
 * item damage, FC's {@code RenderBlockAsItem} captured the same way, or the flat icon where
 * 1.5.2 drew one (see {@link #itemModelFor}). World quads come from RenderBlock, item quads from
 * RenderBlockAsItem, as in 1.5.2.
 *
 * <p>While capturing, {@link StaticAccess} records whether a meta's render read
 * a neighbour, a tile entity or opacity, and a second capture at (1,0,0)
 * detects coordinate parity. Such position-dependent metas are re-captured on
 * every chunk rebuild against the real chunk snapshot ({@link LiveAccess}),
 * through the Forge {@link #getModelData} hook -- 1.5.2's WorldRenderer ran
 * RenderBlock for every block on every rebuild (WorldRenderer.java:162-222).
 *
 * <p>The live set is not limited to the gear boxes, clutches and axles whose getBlockTexture reads
 * neighbours. RenderBlocks.renderStandardBlock / RenderStandardFullBlock gate every face on
 * block.shouldSideBeRendered (1.5.2 RenderBlocks.java:6047-6187, 10103-10175), so every meta of a
 * block whose override reads a neighbour is live too: FCBlockAestheticOpaque and FCBlockSlab
 * (getBlockId / isBlockOpaqueCube), and the sidings, corners, mouldings, stairs, planters and panes
 * (grates, slats) that go through ShouldSideBeRenderedBasedOnCurrentBounds (getBlockId). Each such block
 * costs one FC RenderBlock per chunk-section rebuild, serialised on {@link #CAPTURE_LOCK}; that buys
 * 1.5.2's face culling, which the static quads (air neighbours, every face drawn) lack. The log line
 * "live-capture metas" lists the live metas per block.
 */
public class FCBakedModel implements BakedModel {

    private static final Logger LOGGER = LogManager.getLogger("BTW-FCBakedModel");

    // --- ModelData properties for live rendering ---

    /** ModelData: the chunk-mesh snapshot (RenderChunkRegion) for real neighbour access. */
    public static final ModelProperty<BlockGetter> BLOCK_GETTER = new ModelProperty<>();
    /** ModelData: the block's world position (immutable copy). */
    public static final ModelProperty<BlockPos> BLOCK_POS = new ModelProperty<>();

    // --- Instance state ---

    /** The FC block to capture rendering from. */
    private btw.modern.Block fcBlock;
    private int legacyBlockId;

    /** Static per-meta RenderBlock quads (no world context). Null until first use. */
    private volatile Map<Integer, List<BakedQuad>> staticQuadsByMeta;
    /** Bit m set => meta m's RenderBlock output depends on world context (neighbour/TE/opacity read
     *  or coordinate parity) and is captured live per position. Written before staticQuadsByMeta is
     *  published, so any reader that saw staticQuadsByMeta != null sees the final mask. */
    private volatile int liveMetaMask;
    private volatile TextureAtlasSprite particleSprite;
    /** Whether icons have been registered for this block. */
    private volatile boolean iconsRegistered = false;

    /** One WARN per (block, meta) for live-capture failures (static quads are used instead). */
    private static final Set<Integer> LIVE_FAIL_LOGGED = ConcurrentHashMap.newKeySet();

    /** Item damage -> item model, built on first use (see {@link #itemModelFor}). */
    private final Map<Integer, BakedModel> itemModelsByDamage = new ConcurrentHashMap<>();
    private final ItemOverrides itemOverrides = new DamageOverrides();
    /** item/generated model whose transforms the flat (icon) item models use; null = block transforms. */
    private BakedModel flatItemTemplate;

    // ================================================================
    // Factory
    // ================================================================

    /**
     * Creates a deferred-capture FCBakedModel. No vertex capture happens
     * until the first getQuads() call on the render thread.
     */
    public static FCBakedModel deferred(btw.modern.Block fcBlock, int legacyId) {
        return deferred(fcBlock, legacyId, null);
    }

    /**
     * As {@link #deferred(btw.modern.Block, int)}; {@code flatItemTemplate} (an item/generated model)
     * supplies the display transforms of the item damages 1.5.2 drew as a flat icon.
     */
    public static FCBakedModel deferred(btw.modern.Block fcBlock, int legacyId, @Nullable BakedModel flatItemTemplate) {
        FCBakedModel model = new FCBakedModel();
        model.fcBlock = fcBlock;
        model.legacyBlockId = legacyId;
        model.flatItemTemplate = flatItemTemplate;
        return model;
    }

    private FCBakedModel() {}

    // ================================================================
    // Deferred capture — runs once, guarded by a global lock
    // ================================================================

    /**
     * Global lock for FC block-render execution. Tessellator and GL11 state are
     * thread-local (Tessellator.java THREAD_LOCAL), but FC's RenderBlock code
     * writes per-Block-instance scratch fields (m_currentBlockRenderer,
     * FCBlockGrass.m_bTempHasSnowOnTop, FCBlockFurnaceBrick.m_iInteriorBrightness,
     * ...) because 1.5.2 meshed chunks on a single thread. This lock serialises
     * every FC render call across all FCBakedModel instances and mesh threads.
     */
    private static final Object CAPTURE_LOCK = new Object();

    /** Set once the global block-icon pass ({@link #ensureGlobalIconsRegistered}) has run. */
    private static volatile boolean globalIconsRegistered = false;
    /** True while the global pass runs; stops a re-entrant call on the same thread. Guarded by CAPTURE_LOCK. */
    private static boolean globalIconsRunning = false;

    /** IconRegister that records each icon by name; captured quads resolve the name to an atlas sprite. */
    private static btw.modern.IconRegister namedIconRegister() {
        return new btw.modern.IconRegister() {
            @Override public btw.modern.Icon registerIcon(String name) { return new NamedIcon(name); }
            @Override public btw.modern.Icon registerIcon(String name, btw.modern.TextureStitched tex) { return new NamedIcon(name); }
        };
    }

    /** Ensure icons are registered (once, thread-safe). */
    private void ensureIconsRegistered() {
        if (iconsRegistered) return;
        synchronized (CAPTURE_LOCK) {
            if (iconsRegistered) return;
            if (fcBlock != null) {
                try { fcBlock.registerIcons(namedIconRegister()); } catch (Throwable ignored) {}
            }
            ensureGlobalIconsRegistered();
            iconsRegistered = true;
        }
    }

    /**
     * 1.5.2 TextureMap.refreshTextures (TextureMap.java:61-71) registers icons on EVERY block.
     * Vanilla ids 1-174 are real 1.20.1 blocks here, yet FC blocks render with their icons by
     * reference (FCBlockStairs m_referenceBlock: 1003 -> Block.stone, 1009 -> Block.planks meta 4),
     * FC fire/campfire geometry uses Block.fire's icons, and held/worn block items (ItemBlock sprite
     * number 0: ItemRenderer.renderItem's 3D branch, block.getBlockTextureFromSide) read block icons.
     * Subsumes the old Block.fire special case; NamedIcon registration is idempotent.
     *
     * <p>Runs once. BTWClientEvents calls it at the end of model baking, so block icons exist before
     * anything draws, as in 1.5.2 (triage MC3-R1); the first FC block capture
     * ({@link #ensureIconsRegistered}) still runs it if model baking never got that far. Holds
     * CAPTURE_LOCK, so it never overlaps an FC render on a mesh thread. Never throws.
     */
    static void ensureGlobalIconsRegistered() {
        if (globalIconsRegistered) return;
        synchronized (CAPTURE_LOCK) {
            if (globalIconsRegistered || globalIconsRunning) return;
            globalIconsRunning = true;
            try {
                btw.modern.IconRegister capturer = namedIconRegister();
                int registered = 0;
                for (btw.modern.Block b : btw.modern.Block.blocksList) {
                    if (b == null) continue;
                    try {
                        b.registerIcons(capturer);
                        registered++;
                    } catch (Throwable ignored) {}
                }
                LOGGER.info("BTW: Registered icons on {} blocks (1.5.2 TextureMap.refreshTextures pass).", registered);
            } catch (Throwable t) {
                LOGGER.error("BTW: block icon registration pass failed", t);
            } finally {
                globalIconsRunning = false;
                // Set last: a reader that sees true on the fast path sees every icon written above.
                globalIconsRegistered = true;
            }
        }
    }

    /** Ensure the static per-meta RenderBlock quads are built (position-independent metas, fallbacks). */
    private void ensureStaticQuadsBuilt() {
        if (staticQuadsByMeta != null) return;
        synchronized (CAPTURE_LOCK) {
            if (staticQuadsByMeta != null) return;
            try {
                ensureIconsRegistered();
                doCaptureAndBuild();
            } catch (Throwable t) {
                // Never let a capture failure escape into chunk meshing / item rendering (it would
                // retry and throw on every call). Per-meta FC failures are already caught inside.
                LOGGER.error("BTW: static capture failed for block {}; it renders empty", legacyBlockId, t);
                this.liveMetaMask = 0;
                if (this.particleSprite == null) this.particleSprite = getMissingSprite();
                this.staticQuadsByMeta = Collections.emptyMap();
            }
        }
    }

    // ================================================================
    // FC render runner (shared by the static and live captures)
    // ================================================================

    /** Result of one FC RenderBlock capture. */
    private static final class Capture {
        final List<btw.modern.Tessellator.CapturedQuad> quads;
        final Throwable failure;
        Capture(List<btw.modern.Tessellator.CapturedQuad> quads, Throwable failure) {
            this.quads = quads;
            this.failure = failure;
        }
    }

    /**
     * 1.5.2 RenderBlocks.renderBlockByRenderType (RenderBlocks.java:9442-9448) plus WorldRenderer's
     * tessellator offset (WorldRenderer.java:197), offset to block-local instead of chunk-local so the
     * captured geometry is in 0..1 block space. The caller must hold {@link #CAPTURE_LOCK}.
     *
     * <p>Leaves the thread's Tessellator/GL11 state clean afterwards (zero translation, white
     * colour): a lazy bake can run on the render thread between FC entity/TESR captures, and a
     * leftover face shade or colour multiplier would tint the next capture (triage R1-M1).
     */
    private Capture runFcRender(btw.modern.IBlockAccess access, int x, int y, int z) {
        return runFcRender(access, x, y, z, false);
    }

    /**
     * {@code renderAllFaces}: 1.5.2 RenderBlocks.renderAllFaces, which skips the shouldSideBeRendered face gate
     * (renderBlockAllFaces, vanilla/client RenderBlocks.java:402-407, what 1.5.2 drew a piston-moved block with).
     */
    private Capture runFcRender(btw.modern.IBlockAccess access, int x, int y, int z, boolean renderAllFaces) {
        btw.modern.Block block = this.fcBlock;
        btw.modern.RenderBlocks renderer = new btw.modern.RenderBlocks();
        renderer.blockAccess = access;
        renderer.SetRenderAllFaces(renderAllFaces);
        btw.modern.Tessellator tess = btw.modern.Tessellator.instance;   // thread-local
        // Block geometry is emitted in block coords via render bounds; no GL matrix may apply. A
        // same-thread FC TESR could have left tracking enabled (see GL11 javadoc).
        btw.modern.GL11.disableMatrixTracking();
        // 1.5.2 chunk meshing drew with the GL colour at white; vertices FC leaves uncoloured must
        // not inherit a renderer's tint.
        btw.modern.GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        tess.startCapturing();
        // Set explicitly after startCapturing so the capture never depends on sticky state.
        tess.setTranslation(-x, -y, -z);
        tess.setNormal(0, 1, 0);
        tess.setColorOpaque_F(1, 1, 1);
        tess.setTextureUV(0, 0);
        Throwable failure = null;
        try {
            renderer.setRenderBounds(0, 0, 0, 1, 1, 1);
            renderer.unlockBlockBounds();
            // FC shouldSideBeRendered overrides dereference it (e.g. FCBlockAestheticNonOpaque.java:742).
            block.m_currentBlockRenderer = renderer;
            boolean firstPass = block.RenderBlock(renderer, x, y, z);
            // Second pass: overlays (cook speckles, etc.), given the real first-pass result.
            block.RenderBlockSecondPass(renderer, x, y, z, firstPass);
        } catch (Throwable t) {
            failure = t;
        }
        List<btw.modern.Tessellator.CapturedQuad> quads;
        try {
            quads = tess.stopCapturing();
        } catch (Throwable t) {
            quads = Collections.emptyList();
            if (failure == null) failure = t;
        }
        tess.setTranslation(0, 0, 0);
        tess.setColorOpaque_F(1, 1, 1);
        btw.modern.GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        // Detach the access: blocksList is JVM-static and keeps block.m_currentBlockRenderer, so a
        // LiveAccess left here would pin the chunk snapshot (and through it the ClientLevel) after
        // the player leaves the world. The renderer itself stays assigned, non-null as 1.5.2 left it
        // after chunk meshing (RenderBlocks.java:9444); FC calls ShouldSideBeRenderedBasedOnCurrentBounds
        // through it, which draws the face (returns true) while blockAccess is null.
        renderer.blockAccess = null;
        return new Capture(quads, failure);
    }

    private static String fallbackTexture(btw.modern.Block b) {
        try {
            if (b.blockIcon != null && b.blockIcon.getIconName() != null) {
                return b.blockIcon.getIconName().toLowerCase();
            }
        } catch (Throwable ignored) {}
        return "";
    }

    /** True if both captures emitted the same quads (texture, position, UV, colour). */
    private static boolean sameGeometry(List<btw.modern.Tessellator.CapturedQuad> a,
                                        List<btw.modern.Tessellator.CapturedQuad> b) {
        if (a.size() != b.size()) return false;
        for (int q = 0; q < a.size(); q++) {
            btw.modern.Tessellator.CapturedQuad qa = a.get(q), qb = b.get(q);
            if (qa == null || qb == null) {
                if (qa != qb) return false;
                continue;
            }
            if (!Objects.equals(qa.textureName, qb.textureName)) return false;
            for (int v = 0; v < 4; v++) {
                btw.modern.Tessellator.CapturedVertex va = qa.vertices[v], vb = qb.vertices[v];
                if (va == null || vb == null) {
                    if (va != vb) return false;
                    continue;
                }
                if (Math.abs(va.x - vb.x) > 1e-5 || Math.abs(va.y - vb.y) > 1e-5 || Math.abs(va.z - vb.z) > 1e-5
                        || Math.abs(va.u - vb.u) > 1e-6 || Math.abs(va.v - vb.v) > 1e-6
                        || va.r != vb.r || va.g != vb.g || va.b != vb.b || va.a != vb.a) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Fake world for the per-meta static capture: the block at the origin, the same block id at
     * y+-1 (two-high blocks such as doors detect their other half), air elsewhere, the capture meta
     * everywhere and a default tile entity. Returns exactly what the old anonymous fake did, so the
     * static quads are unchanged, but records whether the render read anything that varies in-world.
     */
    private static final class StaticAccess implements btw.modern.IBlockAccess {
        private final btw.modern.Block block;
        private final int meta, ox, oy, oz;
        boolean dependent;

        StaticAccess(btw.modern.Block block, int meta, int ox, int oy, int oz) {
            this.block = block;
            this.meta = meta;
            this.ox = ox;
            this.oy = oy;
            this.oz = oz;
        }

        private boolean self(int x, int y, int z) { return x == ox && y == oy && z == oz; }

        @Override public int getBlockId(int x, int y, int z) {
            if (self(x, y, z)) return block.blockID;
            dependent = true;
            if (x == ox && z == oz && (y == oy + 1 || y == oy - 1)) return block.blockID;
            return 0;
        }
        @Override public int getBlockMetadata(int x, int y, int z) {
            if (!self(x, y, z)) dependent = true;
            return meta;
        }
        @Override public btw.modern.TileEntity getBlockTileEntity(int x, int y, int z) {
            dependent = true;
            try {
                return block.createNewTileEntity(null);
            } catch (Throwable t) {
                return null;
            }
        }
        @Override public btw.modern.Material getBlockMaterial(int x, int y, int z) {
            if (self(x, y, z)) return block.blockMaterial;
            dependent = true;
            return btw.modern.Material.air;
        }
        @Override public boolean isAirBlock(int x, int y, int z) {
            if (self(x, y, z)) return false;
            dependent = true;
            return true;
        }
        @Override public boolean isBlockOpaqueCube(int x, int y, int z) { dependent = true; return false; }
        @Override public boolean isBlockNormalCube(int x, int y, int z) { dependent = true; return false; }
        // Same constants as LiveAccess: baked quads are relit by MC (vertex light 0), so these never
        // differ in-world and are not recorded as dependencies.
        @Override public int getLightBrightnessForSkyBlocks(int x, int y, int z, int light) { return 0xF000F0; }
        @Override public float getLightBrightness(int x, int y, int z) { return 1.0f; }
        @Override public btw.modern.BiomeGenBase getBiomeGenForCoords(int x, int z) { return btw.modern.BiomeGenBase.plains; }
    }

    /**
     * IBlockAccess over the chunk-mesh snapshot (RenderChunkRegion: PalettedContainer copies plus an
     * ImmutableMap of BlockEntities taken on the main thread, so safe to read off-thread). Translation
     * as WorldBridge.getBlockId / getBlockMetadata (ProxyBlock META; vanilla neighbours through
     * WorldBridge.deriveVanillaMetadata) / getBlockMaterial / isBlockNormalCube, and
     * btw.modern.World.isBlockOpaqueCube (the inherited IBlockAccess default, opaqueCubeLookup[id]).
     */
    private static final class LiveAccess implements btw.modern.IBlockAccess {
        private final BlockGetter region;
        private final BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();

        LiveAccess(BlockGetter region) { this.region = region; }

        private BlockState state(int x, int y, int z) {
            try {
                return region.getBlockState(probe.set(x, y, z));
            } catch (RuntimeException e) {
                // Outside the snapshot's 3x3 chunks (RenderChunkRegion throws AIOOBE): treat as air.
                return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
            }
        }

        /**
         * The id FC sees for this cell in the live world (WorldBridge.getBlockId), so FC geometry that
         * reads neighbours (fence/pane/wall arms, ground-cover rest offsets, side culling) draws what the
         * server-side shape and collision use: a 1.20.1 double slab reads as 43/125, a colliding
         * modern-only state (deepslate, tuff, ...) as a ModernBlockStandIn and a water-filled unmapped
         * plant as water, not air. Safe on the mesh thread: getWorldBlockId never throws, takes any
         * BlockGetter and only reads it (collision/occlusion shape of the snapshot state, and for a
         * non-dynamic block whose cached collision shape is empty its live shape at that position in the
         * read-only region, e.g. a Dynamic Trees trunk shell asking for its core); the stand-in
         * ids are allocated once, under ModernBlockStandIn's class lock and published through a volatile
         * flag; the stand-in's live-level overrides need a WorldBridge and take their no-level fallback
         * through this access (no ServerLevel is touched). state() has set probe; outside the snapshot
         * it is air (0).
         */
        @Override public int getBlockId(int x, int y, int z) {
            BlockState s = state(x, y, z);
            return ProxyRegistry.getWorldBlockId(s, region, probe.immutable());
        }
        @Override public int getBlockMetadata(int x, int y, int z) {
            BlockState s = state(x, y, z);
            if (s.hasProperty(ProxyBlock.META)) return s.getValue(ProxyBlock.META);
            if (s.isAir()) return 0;
            // Vanilla neighbour: the same 1.5.2 metadata the server-side WorldBridge reports (log
            // axis, stairs/slab halves, fluid level, ...), so connecting FC geometry matches in-world.
            try {
                return btw.forge.WorldBridge.deriveVanillaMetadata(region, s, probe.immutable());
            } catch (RuntimeException e) {
                // A neighbour read outside the 3x3-chunk snapshot (RenderChunkRegion AIOOBE).
                return 0;
            }
        }
        @Override public btw.modern.TileEntity getBlockTileEntity(int x, int y, int z) {
            BlockState s = state(x, y, z);
            if (!(s.getBlock() instanceof ProxyBlock pb)) return null;
            try {
                BlockEntity be = region.getBlockEntity(probe.set(x, y, z));
                if (be instanceof ProxyBlockEntity pbe && pbe.getFcTileEntity() != null) {
                    return pbe.getFcTileEntity();
                }
            } catch (RuntimeException ignored) {}
            // 1.5.2 Chunk.getChunkBlockTileEntity (Chunk.java:906-919) creates a missing TE on demand.
            try {
                btw.modern.Block fc = pb.getFcBlock();
                return fc != null ? fc.createNewTileEntity(null) : null;
            } catch (Throwable t) {
                return null;
            }
        }
        @Override public btw.modern.Material getBlockMaterial(int x, int y, int z) {
            int id = getBlockId(x, y, z);
            if (id > 0 && id < btw.modern.Block.blocksList.length && btw.modern.Block.blocksList[id] != null) {
                return btw.modern.Block.blocksList[id].blockMaterial;
            }
            return btw.modern.Material.air;
        }
        @Override public boolean isBlockNormalCube(int x, int y, int z) {
            return btw.modern.Block.isNormalCube(getBlockId(x, y, z));
        }
        @Override public boolean isAirBlock(int x, int y, int z) { return state(x, y, z).isAir(); }
        @Override public int getLightBrightnessForSkyBlocks(int x, int y, int z, int light) { return 0xF000F0; }
        @Override public float getLightBrightness(int x, int y, int z) { return 1.0f; }
        @Override public btw.modern.BiomeGenBase getBiomeGenForCoords(int x, int z) { return btw.modern.BiomeGenBase.plains; }
    }

    private void doCaptureAndBuild() {
        btw.modern.Block block = this.fcBlock;
        if (block == null) {
            staticQuadsByMeta = Collections.emptyMap();
            particleSprite = getMissingSprite();
            return;
        }

        // Step 2: For each meta, capture vertices from FC's in-world RenderBlock, which uses FC's
        // FCModelBlock system for correct shapes (items run RenderBlockAsItem instead, see
        // itemModelFor). StaticAccess provides the metadata to FC and records
        // whether the meta's output depends on world context (live capture).
        // RenderBlock returns false for TESR blocks (chest, ender chest, end portal)
        // — those are rendered by FCBlockEntityRenderer via the TESR capture pipeline.
        Map<Integer, List<BakedQuad>> result = new HashMap<>();
        int totalQuads = 0;
        int liveMask = 0;
        Set<String> allTexNames = new HashSet<>();

        // Fallback texture for quads that don't set a texture name
        String fallbackTex = fallbackTexture(block);

        for (int meta = 0; meta < 16; meta++) {
            StaticAccess access = new StaticAccess(block, meta, 0, 0, 0);
            Capture cap = runFcRender(access, 0, 0, 0);
            if (cap.failure != null) {
                LOGGER.warn("BTW: RenderBlock threw for block {} meta {}: {}",
                        legacyBlockId, meta, cap.failure.toString());
            }

            boolean live = access.dependent || cap.failure != null;
            if (!live) {
                // Coordinate parity with no blockAccess read: FCBlockCampfire.java:943 /
                // FCBlockFireStoked.java:195 ((i+k)&1), FCBlockLadderOnFire.java:183 ((i+j+k)&1).
                // Origin (1,0,0) flips both.
                StaticAccess shiftedAccess = new StaticAccess(block, meta, 1, 0, 0);
                Capture shifted = runFcRender(shiftedAccess, 1, 0, 0);
                live = shiftedAccess.dependent || shifted.failure != null
                        || !sameGeometry(cap.quads, shifted.quads);
            }
            if (live) liveMask |= 1 << meta;

            List<btw.modern.Tessellator.CapturedQuad> captured = cap.quads;
            if (live && cap.failure == null) {
                // The static quads of a live meta are drawn where no live capture runs (piston-moved block,
                // breaking overlay, live-capture fallback). The first pass culled faces against StaticAccess's
                // fake neighbours (the same block at y+-1), so take them from an all-faces pass, as 1.5.2 drew
                // a moving block. A non-live meta read no neighbour: its first pass is its in-world geometry.
                Capture all = runFcRender(new StaticAccess(block, meta, 0, 0, 0), 0, 0, 0, true);
                if (all.failure == null && !all.quads.isEmpty()) captured = all.quads;
            }

            if (captured.isEmpty() && meta == 0) {
                LOGGER.warn("BTW: Block {} ({}) produced 0 quads for meta 0. blockIcon={}",
                        legacyBlockId, block.getClass().getSimpleName(),
                        block.blockIcon != null ? block.blockIcon.getIconName() : "NULL");
            }

            if (!captured.isEmpty()) {
                List<BakedQuad> bakedQuads = new ArrayList<>();
                for (btw.modern.Tessellator.CapturedQuad cq : captured) {
                    if (cq == null || cq.vertices == null) continue;
                    BakedQuad bq = convertCapturedQuad(cq, fallbackTex);
                    if (bq != null) {
                        bakedQuads.add(bq);
                    }
                    if (cq.textureName != null) allTexNames.add(cq.textureName);
                }
                if (!bakedQuads.isEmpty()) {
                    result.put(meta, Collections.unmodifiableList(bakedQuads));
                    totalQuads += bakedQuads.size();
                }
            }
        }

        // Per-model capture summary (debug only: getIcon x6 per block)
        if (LOGGER.isDebugEnabled()) {
            // Show what getIcon returns per face for meta 0
            StringBuilder iconInfo = new StringBuilder();
            for (int side = 0; side < 6; side++) {
                try {
                    btw.modern.Icon icon = block.getIcon(side, 0);
                    String name = icon != null ? icon.getIconName() : "NULL";
                    iconInfo.append(side).append("=").append(name).append(" ");
                } catch (Exception e) {
                    iconInfo.append(side).append("=ERR ");
                }
            }
            LOGGER.debug("BTW: Block {} ({}) captured {} metas, {} quads, textures: {}, getIcon: [{}]",
                    legacyBlockId, block.getClass().getSimpleName(),
                    result.size(), totalQuads, allTexNames, iconInfo.toString().trim());
        }

        // Resolve particle sprite
        String particleTexName = "";
        try {
            btw.modern.Icon topIcon = block.getIcon(1, 0);
            if (topIcon != null && topIcon.getIconName() != null) {
                particleTexName = topIcon.getIconName().toLowerCase();
            }
        } catch (Exception ignored) {}
        this.particleSprite = lookupSprite(particleTexName);

        LOGGER.info("BTW: Block {} ({}) live-capture metas: 0x{}",
                legacyBlockId, block.getClass().getSimpleName(), Integer.toHexString(liveMask));
        this.liveMetaMask = liveMask;      // publish before the map: readers gate on staticQuadsByMeta != null
        this.staticQuadsByMeta = result;
    }

    // ================================================================
    // Live capture — runs FC's RenderBlock with real world data
    // ================================================================

    /**
     * 1.5.2 WorldRenderer.updateRenderer (WorldRenderer.java:162-222): FC RenderBlock over the real
     * neighbours at absolute coordinates, on every chunk rebuild. Runs on the mesh thread with the
     * RenderChunkRegion snapshot. Returns null if FC threw (the caller falls back to static quads).
     */
    @Nullable
    private List<BakedQuad> captureLive(BlockGetter region, BlockPos pos, int meta) {
        Capture cap;
        synchronized (CAPTURE_LOCK) {
            cap = runFcRender(new LiveAccess(region), pos.getX(), pos.getY(), pos.getZ());
        }
        if (cap.failure != null) {
            if (LIVE_FAIL_LOGGED.add(legacyBlockId * 16 + meta)) {
                LOGGER.warn("BTW: live RenderBlock threw for block {} meta {} at {} -- using static quads: {}",
                        legacyBlockId, meta, pos, cap.failure.toString());
            }
            return null;
        }
        if (cap.quads.isEmpty()) return Collections.emptyList();
        // Conversion outside the lock: lookupSprite's cache is a ConcurrentHashMap.
        try {
            String fallbackTex = fallbackTexture(fcBlock);
            List<BakedQuad> out = new ArrayList<>(cap.quads.size());
            for (btw.modern.Tessellator.CapturedQuad cq : cap.quads) {
                if (cq == null || cq.vertices == null) continue;
                BakedQuad bq = convertCapturedQuad(cq, fallbackTex);
                if (bq != null) out.add(bq);
            }
            return out;
        } catch (Throwable t) {
            // Never throw out of chunk meshing; fall back to the static quads.
            if (LIVE_FAIL_LOGGED.add(legacyBlockId * 16 + meta)) {
                LOGGER.warn("BTW: live quad conversion failed for block {} meta {} at {} -- using static quads: {}",
                        legacyBlockId, meta, pos, t.toString());
            }
            return null;
        }
    }

    // ================================================================
    // Item models — FC's RenderBlockAsItem per item damage
    // ================================================================

    /** Distinct item damages cached per block; further damages show damage 0 instead of re-capturing each frame. */
    private static final int MAX_ITEM_DAMAGE_MODELS = 256;

    /** One WARN per block whose item render threw or drew nothing (its RenderBlock quads are drawn instead). */
    private static final Set<Integer> ITEM_FAIL_LOGGED = ConcurrentHashMap.newKeySet();

    /**
     * The model of this block's item stack with the given damage, built on first use and cached.
     * 1.5.2 drew a block item from the stack's damage: RenderItem.renderItemIntoGUI (RenderItem.java:374)
     * and ItemRenderer.renderItem (ItemRenderer.java:57) take the 3D branch when
     * {@code stack.getItemSpriteNumber() == 0 && block.DoesItemRenderAsBlock(damage)} and call
     * renderBlockAsItem(block, damage, 1F) (RenderItem.java:398, ItemRenderer.java:61), which hands the
     * draw to FC's Block.RenderBlockAsItem (RenderBlocks.java:9453-9456); otherwise they draw the stack's
     * icon flat (RenderItem.java:402-460). So every subtype stack (sidings, corners, mouldings, wool
     * slabs, ...) gets its own model, and torches, plants and other flat-icon blocks are flat. Never throws.
     */
    private BakedModel itemModelFor(int damage) {
        BakedModel cached = itemModelsByDamage.get(damage);
        if (cached != null) return cached;
        if (damage != 0 && itemModelsByDamage.size() >= MAX_ITEM_DAMAGE_MODELS) return itemModelFor(0);
        if (btw.modern.Tessellator.instance.isCapturing()) {
            // Asked from inside an FC capture on this thread: the Tessellator holds one capture per
            // thread, so capturing now would wipe that capture's quads. Serve the meta-0 RenderBlock
            // quads this once (uncached); the item model is built on a later call.
            return staticItemModel(0);
        }
        BakedModel built;
        try {
            built = buildItemModel(damage);
        } catch (Throwable t) {
            if (ITEM_FAIL_LOGGED.add(legacyBlockId)) {
                LOGGER.warn("BTW: item model of block {} damage {} failed -- drawing its RenderBlock quads: {}",
                        legacyBlockId, damage, t.toString());
            }
            built = staticItemModel(0);
        }
        BakedModel raced = itemModelsByDamage.putIfAbsent(damage, built);
        return raced != null ? raced : built;
    }

    /** What FC decides for one item damage (read under CAPTURE_LOCK; FC Block instances keep scratch fields). */
    private static final class ItemPlan {
        boolean asBlock;
        /** getColorFromItemStack per drawn layer (3D: one, the GL colour of the block draw). */
        int[] colors = {0xFFFFFF};
        /** Flat: icon name per render pass (null = no icon for that pass). */
        String[] iconNames = new String[0];
        /** The item's placed metadata (FC Item.getMetadata), for the RenderBlock fallback. */
        int fallbackMeta;
    }

    private BakedModel buildItemModel(int damage) {
        ensureStaticQuadsBuilt();
        btw.modern.Block block = this.fcBlock;
        if (block == null) return staticItemModel(0);

        ItemPlan plan;
        Capture cap = null;
        synchronized (CAPTURE_LOCK) {
            plan = planItem(block, damage);
            if (plan.asBlock) cap = runFcItemRender(damage, plan.colors[0]);
        }

        if (plan.asBlock) {
            // Conversion outside the lock: lookupSprite's cache is a ConcurrentHashMap.
            List<BakedQuad> quads = cap.failure == null ? toBakedQuads(cap.quads) : Collections.emptyList();
            if (!quads.isEmpty()) {
                return new ItemModel(quads, true, particleOrMissing(), FC_ITEM_TRANSFORMS);
            }
            if (ITEM_FAIL_LOGGED.add(legacyBlockId)) {
                LOGGER.warn("BTW: RenderBlockAsItem of block {} ({}) damage {} {} -- drawing its RenderBlock quads"
                                + " (meta {}) for its item instead",
                        legacyBlockId, block.getClass().getSimpleName(), damage,
                        cap.failure != null ? "threw " + cap.failure : "drew nothing", plan.fallbackMeta);
            }
            return staticItemModel(plan.fallbackMeta);
        }

        List<BakedQuad> quads = new ArrayList<>();
        TextureAtlasSprite particle = null;
        for (int layer = 0; layer < plan.iconNames.length; layer++) {
            String name = plan.iconNames[layer];
            TextureAtlasSprite sprite = name != null ? lookupSprite(name) : null;
            if (sprite == null || sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())) continue;
            if (particle == null) particle = sprite;
            quads.addAll(flatLayerQuads(sprite, layer, plan.colors[layer]));
        }
        if (quads.isEmpty()) {
            // 1.5.2 drew the missing icon here (RenderItem.java:442-445); the RenderBlock quads at least
            // show the block.
            if (ITEM_FAIL_LOGGED.add(legacyBlockId)) {
                LOGGER.warn("BTW: no item icon for block {} ({}) damage {} -- drawing its RenderBlock quads (meta {})",
                        legacyBlockId, block.getClass().getSimpleName(), damage, plan.fallbackMeta);
            }
            return staticItemModel(plan.fallbackMeta);
        }
        BakedModel template = this.flatItemTemplate;
        return new ItemModel(quads, false, particle,
                template != null ? template.getTransforms() : ItemTransforms.NO_TRANSFORMS);
    }

    /**
     * 1.5.2 RenderItem.renderItemIntoGUI's branch choice and inputs (RenderItem.java:362-460) for a stack
     * of this block with the given damage. An FC block without an FC item behaves as a plain ItemBlock
     * (vanilla/client ItemBlock.java:42-56: sprite 0 unless the block names an item icon, the block's top
     * icon). The caller holds CAPTURE_LOCK.
     */
    private ItemPlan planItem(btw.modern.Block block, int damage) {
        ItemPlan plan = new ItemPlan();
        int id = this.legacyBlockId;
        btw.modern.Item item = id > 0 && id < btw.modern.Item.itemsList.length ? btw.modern.Item.itemsList[id] : null;
        btw.modern.ItemStack stack = new btw.modern.ItemStack(id, 1, damage);
        if (item != null) {
            try {
                int meta = item.getMetadata(damage);
                plan.fallbackMeta = meta >= 0 && meta < 16 ? meta : 0;
            } catch (Throwable ignored) {}
        }
        int spriteNumber = item != null ? item.getItemSpriteNumber() : (block.getItemIconName() != null ? 1 : 0);
        plan.asBlock = spriteNumber == 0 && block.DoesItemRenderAsBlock(damage);
        if (plan.asBlock) {
            // :386-394 the stack's colour is the GL colour of the block draw; renderBlockAsItemVanilla
            // replaces it with the block's render colour (useInventoryTint), FC item renders keep it.
            plan.colors = new int[] {itemColor(item, stack, 0)};
            return plan;
        }
        if (item != null && item.requiresMultipleRenderPasses()) {
            // :406-428 two passes, each with its own icon and colour.
            plan.iconNames = new String[] {
                    iconName(item.getIconFromDamageForRenderPass(damage, 0)),
                    iconName(item.getIconFromDamageForRenderPass(damage, 1))};
            plan.colors = new int[] {itemColor(item, stack, 0), itemColor(item, stack, 1)};
        } else {
            // :429-460 the stack's icon in the stack's colour.
            plan.iconNames = new String[] {
                    iconName(item != null ? stack.getIconIndex() : block.getBlockTextureFromSide(1))};
            plan.colors = new int[] {itemColor(item, stack, 0)};
        }
        return plan;
    }

    private static int itemColor(@Nullable btw.modern.Item item, btw.modern.ItemStack stack, int pass) {
        if (item == null) return 0xFFFFFF;
        try {
            return item.getColorFromItemStack(stack, pass) & 0xFFFFFF;
        } catch (Throwable t) {
            return 0xFFFFFF;
        }
    }

    @Nullable
    private static String iconName(@Nullable btw.modern.Icon icon) {
        return icon != null ? icon.getIconName() : null;
    }

    /**
     * The RenderBlocks of an item capture, like 1.5.2's RenderItem.itemRenderBlocks and
     * ItemRenderer.renderBlocksInstance (blockAccess null). It dispatches the captured block as
     * btw.modern.RenderBlocks.renderBlockAsItem does -- FC's RenderBlockAsItem, whose default (1.5.2
     * Block.java:3701-3704) and super calls re-enter here and get renderBlockAsItemVanilla -- but records
     * a throw instead of swallowing it, so the capture can fall back to the RenderBlock quads. Other
     * blocks an FC item render draws take the inherited dispatch.
     */
    private static final class ItemCaptureRenderBlocks extends btw.modern.RenderBlocks {
        private btw.modern.Block dispatching;
        Throwable failure;

        @Override
        public void renderBlockAsItem(btw.modern.Block block, int damage, float brightness) {
            if (block == null) return;
            if (block == dispatching) {
                renderBlockAsItemVanilla(block, damage, brightness);
                return;
            }
            if (dispatching != null) {
                super.renderBlockAsItem(block, damage, brightness);
                return;
            }
            dispatching = block;
            try {
                block.RenderBlockAsItem(this, damage, brightness);
            } catch (Throwable t) {
                failure = t;
            } finally {
                dispatching = null;
            }
        }

        /**
         * 1.5.2 renderBlockAsItemVanilla's stairs branch (RenderBlocks.java:7939-8010), which the
         * Modern-Common port does not have (it draws an unported render type as its cube): FC's stairs
         * (FCBlockStairsBase render type 10, no RenderBlockAsItem override; ids 190, 1003, 1009, ...)
         * are a full-height back half and a half-height front step, each face's icon at the item damage
         * (FCMOD), after the useInventoryTint colour (:7830-7843, without the brightness factor, as the
         * port). Every other render type takes the inherited port.
         */
        @Override
        public void renderBlockAsItemVanilla(btw.modern.Block block, int damage, float brightness) {
            if (block == null || block.getRenderType() != 10) {
                super.renderBlockAsItemVanilla(block, damage, brightness);
                return;
            }
            btw.modern.Tessellator t = btw.modern.Tessellator.instance;
            if (this.useInventoryTint) {
                int color = block.getRenderColor(damage);
                btw.modern.GL11.glColor4f((float) (color >> 16 & 255) / 255.0F, (float) (color >> 8 & 255) / 255.0F,
                        (float) (color & 255) / 255.0F, 1.0F);
            }
            for (int box = 0; box < 2; box++) {
                if (box == 0) {
                    this.setRenderBounds(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 0.5D);
                } else {
                    this.setRenderBounds(0.0D, 0.0D, 0.5D, 1.0D, 0.5D, 1.0D);
                }
                btw.modern.GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
                t.startDrawingQuads();
                t.setNormal(0.0F, -1.0F, 0.0F);
                this.renderFaceYNeg(block, 0.0D, 0.0D, 0.0D, this.getBlockIconFromSideAndMetadata(block, 0, damage));
                t.draw();
                t.startDrawingQuads();
                t.setNormal(0.0F, 1.0F, 0.0F);
                this.renderFaceYPos(block, 0.0D, 0.0D, 0.0D, this.getBlockIconFromSideAndMetadata(block, 1, damage));
                t.draw();
                t.startDrawingQuads();
                t.setNormal(0.0F, 0.0F, -1.0F);
                this.renderFaceZNeg(block, 0.0D, 0.0D, 0.0D, this.getBlockIconFromSideAndMetadata(block, 2, damage));
                t.draw();
                t.startDrawingQuads();
                t.setNormal(0.0F, 0.0F, 1.0F);
                this.renderFaceZPos(block, 0.0D, 0.0D, 0.0D, this.getBlockIconFromSideAndMetadata(block, 3, damage));
                t.draw();
                t.startDrawingQuads();
                t.setNormal(-1.0F, 0.0F, 0.0F);
                this.renderFaceXNeg(block, 0.0D, 0.0D, 0.0D, this.getBlockIconFromSideAndMetadata(block, 4, damage));
                t.draw();
                t.startDrawingQuads();
                t.setNormal(1.0F, 0.0F, 0.0F);
                this.renderFaceXPos(block, 0.0D, 0.0D, 0.0D, this.getBlockIconFromSideAndMetadata(block, 5, damage));
                t.draw();
                btw.modern.GL11.glTranslatef(0.5F, 0.5F, 0.5F);
            }
        }
    }

    /**
     * 1.5.2 RenderItem.renderItemIntoGUI's 3D branch (RenderItem.java:374-399): the stack's colour as the
     * GL colour (:386-394), then renderBlockAsItem(block, damage, 1F) (:398). FC item renders draw centred
     * on the origin, either through GL11.glTranslatef(-0.5) (FCClientUtilsRender.RenderInvBlockWithMetadata,
     * FCUtilsPrimitiveQuad.RenderAsItemBlock) or with -0.5 vertex offsets (renderBlockAsItemVanilla's plant
     * branches), and the vanilla cube branch turns 90 degrees about Y first (RenderBlocks.java:8318). So
     * GL11 matrix tracking is on and a base glTranslatef(+0.5) puts both styles in the model's 0..1 space
     * (a tessellator offset would miss the glTranslatef style); 1.20.1 ItemRenderer.render's
     * translate(-0.5) (ItemRenderer.java:113) undoes it, so every display context draws FC's item
     * geometry exactly as FC drew it. The
     * per-context turns 1.5.2 applied around that geometry (RenderItem's glRotatef(-90) about Y for the
     * GUI, :396; none in first person) live in {@link #FC_ITEM_TRANSFORMS}, not in model space. The caller
     * must hold {@link #CAPTURE_LOCK}. Leaves this thread's Tessellator/GL11 state clean, as
     * {@link #runFcRender}.
     */
    private Capture runFcItemRender(int damage, int itemColor) {
        btw.modern.Block block = this.fcBlock;
        ItemCaptureRenderBlocks renderer = new ItemCaptureRenderBlocks();
        btw.modern.Tessellator tess = btw.modern.Tessellator.instance;   // thread-local
        tess.startCapturing();                       // also resets the GL colour and render state
        btw.modern.GL11.enableMatrixTracking();      // identity modelview stack
        btw.modern.GL11.glTranslatef(0.5F, 0.5F, 0.5F);
        btw.modern.GL11.glColor4f((float) (itemColor >> 16 & 255) / 255.0F, (float) (itemColor >> 8 & 255) / 255.0F,
                (float) (itemColor & 255) / 255.0F, 1.0F);
        Throwable failure = null;
        try {
            renderer.renderBlockAsItem(block, damage, 1.0F);
            failure = renderer.failure;
        } catch (Throwable t) {
            failure = t;
        }
        List<btw.modern.Tessellator.CapturedQuad> quads;
        try {
            quads = tess.stopCapturing();
        } catch (Throwable t) {
            quads = Collections.emptyList();
            if (failure == null) failure = t;
        }
        // Block captures on this thread run untransformed (runFcRender disables tracking too).
        btw.modern.GL11.disableMatrixTracking();
        tess.setTranslation(0, 0, 0);
        tess.setColorOpaque_F(1, 1, 1);
        btw.modern.GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        return new Capture(quads, failure);
    }

    private List<BakedQuad> toBakedQuads(List<btw.modern.Tessellator.CapturedQuad> captured) {
        String fallbackTex = fallbackTexture(fcBlock);
        List<BakedQuad> out = new ArrayList<>(captured.size());
        for (btw.modern.Tessellator.CapturedQuad cq : captured) {
            if (cq == null || cq.vertices == null) continue;
            BakedQuad bq = convertCapturedQuad(cq, fallbackTex);
            if (bq != null) out.add(bq);
        }
        return out;
    }

    /**
     * The static RenderBlock quads of one meta as a 3D item model (the item fallback). World geometry in
     * block space, so block.json's transforms, as 1.20.1 draws a block model's item.
     */
    private BakedModel staticItemModel(int meta) {
        Map<Integer, List<BakedQuad>> statics = staticQuadsByMeta;
        List<BakedQuad> quads = statics == null ? Collections.emptyList()
                : statics.getOrDefault(meta, statics.getOrDefault(0, Collections.emptyList()));
        return new ItemModel(quads, true, particleOrMissing(), BLOCK_TRANSFORMS);
    }

    private TextureAtlasSprite particleOrMissing() {
        TextureAtlasSprite sprite = particleSprite;
        return sprite != null ? sprite : getMissingSprite();
    }

    /**
     * One flat item layer (1.5.2 RenderItem.java:406-460: the icon in getColorFromItemStack's colour) as
     * item/generated geometry, baked the way Forge's ItemLayerModel bakes a layer
     * (UnbakedGeometryHelper.createUnbakedItemElements + bakeElements). The colour is baked into the
     * vertices with tint index -1, so a registered item colour handler cannot tint it a second time.
     */
    private List<BakedQuad> flatLayerQuads(TextureAtlasSprite sprite, int layer, int color) {
        List<BakedQuad> baked;
        try {
            List<BlockElement> elements = UnbakedGeometryHelper.createUnbakedItemElements(layer, sprite.contents());
            baked = UnbakedGeometryHelper.bakeElements(elements, material -> sprite, BlockModelRotation.X0_Y0,
                    new ResourceLocation(BTWForgeMod.MOD_ID, "item/block_" + legacyBlockId));
        } catch (Throwable t) {
            // The sprite's pixels could not be read: front and back faces without the edge strips.
            baked = flatFrontAndBack(sprite);
        }
        List<BakedQuad> out = new ArrayList<>(baked.size());
        for (BakedQuad quad : baked) out.add(withColor(quad, color));
        return out;
    }

    /** The quad with each vertex colour multiplied by an 0xRRGGBB colour, untinted (tint index -1). */
    private static BakedQuad withColor(BakedQuad quad, int color) {
        int[] data = quad.getVertices().clone();
        int stride = data.length / 4;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;
        for (int i = 0; i < 4; i++) {
            int c = data[i * stride + 3];   // ABGR
            data[i * stride + 3] = (c & 0xFF000000)
                    | (int) ((c >> 16 & 255) * b) << 16
                    | (int) ((c >> 8 & 255) * g) << 8
                    | (int) ((c & 255) * r);
        }
        return new BakedQuad(data, -1, quad.getDirection(), quad.getSprite(), quad.isShade());
    }

    /** item/generated's front (south) and back (north) faces at z 8.5/16 and 7.5/16, white, normals packed. */
    private static List<BakedQuad> flatFrontAndBack(TextureAtlasSprite sprite) {
        float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
        float front = 8.5F / 16.0F, back = 7.5F / 16.0F;
        int south = 127 << 16;
        int north = (((byte) -127) & 0xFF) << 16;
        List<BakedQuad> quads = new ArrayList<>(2);
        quads.add(new BakedQuad(flatQuadData(new float[] {
                0, 1, front, u0, v0,   0, 0, front, u0, v1,   1, 0, front, u1, v1,   1, 1, front, u1, v0}, south),
                -1, Direction.SOUTH, sprite, false));
        quads.add(new BakedQuad(flatQuadData(new float[] {
                1, 1, back, u1, v0,   1, 0, back, u1, v1,   0, 0, back, u0, v1,   0, 1, back, u0, v0}, north),
                -1, Direction.NORTH, sprite, false));
        return quads;
    }

    private static int[] flatQuadData(float[] xyzuv, int packedNormal) {
        int[] data = new int[32];
        for (int i = 0; i < 4; i++) {
            int base = i * 8, src = i * 5;
            data[base] = Float.floatToRawIntBits(xyzuv[src]);
            data[base + 1] = Float.floatToRawIntBits(xyzuv[src + 1]);
            data[base + 2] = Float.floatToRawIntBits(xyzuv[src + 2]);
            data[base + 3] = -1;
            data[base + 4] = Float.floatToRawIntBits(xyzuv[src + 3]);
            data[base + 5] = Float.floatToRawIntBits(xyzuv[src + 4]);
            data[base + 6] = 0;
            data[base + 7] = packedNormal;
        }
        return data;
    }

    /** FCBakedModel's item overrides: the stack's damage picks its model. */
    private final class DamageOverrides extends ItemOverrides {
        @Override
        public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level,
                                  @Nullable LivingEntity entity, int seed) {
            try {
                return itemModelFor(Math.max(0, stack.getDamageValue()));
            } catch (Throwable t) {
                return model;
            }
        }
    }

    /** One item damage's model: its quads plus the 3D-block or flat-icon display settings. */
    private static final class ItemModel implements BakedModel {
        private final List<BakedQuad> quads;
        private final boolean block3d;
        private final TextureAtlasSprite particle;
        private final ItemTransforms transforms;

        ItemModel(List<BakedQuad> quads, boolean block3d, TextureAtlasSprite particle, ItemTransforms transforms) {
            this.quads = quads;
            this.block3d = block3d;
            this.particle = particle;
            this.transforms = transforms;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
            return side == null ? quads : Collections.emptyList();
        }

        @Override public boolean useAmbientOcclusion() { return block3d; }
        @Override public boolean isGui3d() { return block3d; }
        @Override public boolean usesBlockLight() { return block3d; }
        @Override public boolean isCustomRenderer() { return false; }
        @Override public TextureAtlasSprite getParticleIcon() { return particle; }
        @Override public ItemTransforms getTransforms() { return transforms; }
        @Override public ItemOverrides getOverrides() { return ItemOverrides.EMPTY; }
    }

    private static BakedQuad convertCapturedQuad(btw.modern.Tessellator.CapturedQuad cq,
                                                  String fallbackTexture) {
        if (cq.vertices[0] == null) return null;

        btw.modern.Tessellator.CapturedVertex v0 = cq.vertices[0];
        Direction dir = directionFromNormal(v0.nx, v0.ny, v0.nz);

        String texName = cq.textureName;
        if (texName == null || texName.isEmpty()) texName = fallbackTexture;
        TextureAtlasSprite sprite = lookupSprite(texName != null ? texName : "");
        if (sprite == null) return null;

        btw.modern.Tessellator.CapturedVertex p0 = cq.vertices[0], p1 = cq.vertices[1], p2 = cq.vertices[2], p3 = cq.vertices[3];
        org.joml.Vector3f normal = new org.joml.Vector3f((float) (p2.x - p0.x), (float) (p2.y - p0.y), (float) (p2.z - p0.z))
                .cross((float) (p3.x - p1.x), (float) (p3.y - p1.y), (float) (p3.z - p1.z));
        if (normal.lengthSquared() > 1.0E-8F) {
            normal.normalize();
        } else {
            normal.set(p0.nx, p0.ny, p0.nz);
        }
        int packedNormal = (((byte) (normal.x * 127)) & 0xFF) | ((((byte) (normal.y * 127)) & 0xFF) << 8)
                | ((((byte) (normal.z * 127)) & 0xFF) << 16);

        int[] vertexData = new int[32];
        for (int i = 0; i < 4; i++) {
            btw.modern.Tessellator.CapturedVertex v = cq.vertices[i];
            int base = i * 8;

            vertexData[base + 0] = Float.floatToRawIntBits((float) v.x);
            vertexData[base + 1] = Float.floatToRawIntBits((float) v.y);
            vertexData[base + 2] = Float.floatToRawIntBits((float) v.z);
            int r = (int)(v.r * 255) & 0xFF;
            int g = (int)(v.g * 255) & 0xFF;
            int b = (int)(v.b * 255) & 0xFF;
            int a = (int)(v.a * 255) & 0xFF;
            vertexData[base + 3] = a << 24 | b << 16 | g << 8 | r;
            vertexData[base + 4] = Float.floatToRawIntBits(sprite.getU(v.u * 16));
            vertexData[base + 5] = Float.floatToRawIntBits(sprite.getV(v.v * 16));
            vertexData[base + 6] = 0;
            vertexData[base + 7] = packedNormal;
        }

        return new BakedQuad(vertexData, -1, dir, sprite, true);
    }

    private static Direction directionFromNormal(float nx, float ny, float nz) {
        if (ny > 0.5f) return Direction.UP;
        if (ny < -0.5f) return Direction.DOWN;
        if (nz < -0.5f) return Direction.NORTH;
        if (nz > 0.5f) return Direction.SOUTH;
        if (nx < -0.5f) return Direction.WEST;
        if (nx > 0.5f) return Direction.EAST;
        return Direction.UP;
    }

    @Override
    public net.minecraftforge.client.ChunkRenderTypeSet getRenderTypes(
            BlockState state, RandomSource rand, net.minecraftforge.client.model.data.ModelData data) {
        return net.minecraftforge.client.ChunkRenderTypeSet.of(fcBlock != null && fcBlock.getRenderBlockPass() == 1
                ? net.minecraft.client.renderer.RenderType.translucent()
                : net.minecraft.client.renderer.RenderType.cutoutMipped());
    }

    /**
     * IForgeBakedModel hook, called from ChunkRenderDispatcher.RebuildTask.compile (:631) on the mesh
     * thread with the RenderChunkRegion, just before getQuads for the same block. Only
     * position-dependent metas get the region and position; every other meta stays on the static path.
     */
    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        if (fcBlock == null) return modelData;
        ensureStaticQuadsBuilt();
        if ((liveMetaMask & (1 << metaOf(state))) == 0) return modelData;
        return modelData.derive()
                .with(BLOCK_GETTER, level)
                .with(BLOCK_POS, pos.immutable())   // compile() iterates a MutableBlockPos
                .build();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
                                     ModelData extraData,
                                     @Nullable net.minecraft.client.renderer.RenderType renderType) {
        if (side != null) return Collections.emptyList();
        // No block state: an item draw (ItemRenderer.renderModelLists). The stack's damage is chosen by
        // getOverrides; a caller that skips the overrides gets damage 0's item quads (under this model's
        // block transforms, not FC_ITEM_TRANSFORMS).
        if (state == null) return itemModelFor(0).getQuads(null, null, rand);
        ensureStaticQuadsBuilt();
        int meta = metaOf(state);

        // Position-dependent meta with real world data: run FC's RenderBlock LIVE with real
        // neighbours, exactly what MC 1.5.2 did during chunk rebuilds.
        if (extraData != null && fcBlock != null && (liveMetaMask & (1 << meta)) != 0) {
            BlockGetter region = extraData.get(BLOCK_GETTER);
            BlockPos pos = extraData.get(BLOCK_POS);
            if (region != null && pos != null) {
                List<BakedQuad> live = captureLive(region, pos, meta);
                if (live != null) return live;
            }
        }

        // The breaking overlay and position-independent metas: static per-meta cache.
        return staticQuadsByMeta.getOrDefault(meta, Collections.emptyList());
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        if (side != null) return Collections.emptyList();
        if (state == null) return itemModelFor(0).getQuads(null, null, rand);
        ensureStaticQuadsBuilt();
        return staticQuadsByMeta.getOrDefault(metaOf(state), Collections.emptyList());
    }

    private static int metaOf(@Nullable BlockState state) {
        return state != null && state.hasProperty(ProxyBlock.META) ? state.getValue(ProxyBlock.META) : 0;
    }

    @Override public boolean useAmbientOcclusion() { return true; }
    @Override public boolean isGui3d() { return true; }
    @Override public boolean usesBlockLight() { return true; }
    @Override public boolean isCustomRenderer() { return false; }

    /** Standard block display transforms from minecraft:block/block.json (this model, RenderBlock item fallbacks). */
    private static final net.minecraft.client.renderer.block.model.ItemTransforms BLOCK_TRANSFORMS =
            new net.minecraft.client.renderer.block.model.ItemTransforms(
                    // thirdperson_lefthand
                    new net.minecraft.client.renderer.block.model.ItemTransform(
                            new org.joml.Vector3f(75, 45, 0),
                            new org.joml.Vector3f(0, 2.5f / 16f, 0),
                            new org.joml.Vector3f(0.375f, 0.375f, 0.375f)),
                    // thirdperson_righthand
                    new net.minecraft.client.renderer.block.model.ItemTransform(
                            new org.joml.Vector3f(75, 45, 0),
                            new org.joml.Vector3f(0, 2.5f / 16f, 0),
                            new org.joml.Vector3f(0.375f, 0.375f, 0.375f)),
                    // firstperson_lefthand
                    new net.minecraft.client.renderer.block.model.ItemTransform(
                            new org.joml.Vector3f(0, 225, 0),
                            new org.joml.Vector3f(0, 0, 0),
                            new org.joml.Vector3f(0.4f, 0.4f, 0.4f)),
                    // firstperson_righthand
                    new net.minecraft.client.renderer.block.model.ItemTransform(
                            new org.joml.Vector3f(0, 45, 0),
                            new org.joml.Vector3f(0, 0, 0),
                            new org.joml.Vector3f(0.4f, 0.4f, 0.4f)),
                    // head
                    net.minecraft.client.renderer.block.model.ItemTransform.NO_TRANSFORM,
                    // gui
                    new net.minecraft.client.renderer.block.model.ItemTransform(
                            new org.joml.Vector3f(30, 225, 0),
                            new org.joml.Vector3f(0, 0, 0),
                            new org.joml.Vector3f(0.625f, 0.625f, 0.625f)),
                    // ground
                    new net.minecraft.client.renderer.block.model.ItemTransform(
                            new org.joml.Vector3f(0, 0, 0),
                            new org.joml.Vector3f(0, 3f / 16f, 0),
                            new org.joml.Vector3f(0.25f, 0.25f, 0.25f)),
                    // fixed (item frame)
                    new net.minecraft.client.renderer.block.model.ItemTransform(
                            new org.joml.Vector3f(0, 0, 0),
                            new org.joml.Vector3f(0, 0, 0),
                            new org.joml.Vector3f(0.5f, 0.5f, 0.5f))
            );

    /**
     * Display transforms of the item models captured from FC's RenderBlockAsItem ({@link #runFcItemRender}:
     * FC's item geometry G, centred on the origin, in model space unturned). block.json's tilt, offset and
     * scale, with each context's turn about Y chosen so G faces the way 1.5.2 drew it:
     * <ul>
     * <li>gui: RenderItem.renderItemIntoGUI (RenderItem.java:381-398) draws S(1,1,-1) Rx(210) Ry(45) Ry(-90) G
     *     = S(1,-1,1) Rx(30) Ry(-45) G; GuiGraphics.renderItem already scales (1,-1,1), so (30, 315, 0) --
     *     block.json's 225 plus 90 (a gear box's input on the right, as in a 1.5.2 inventory).</li>
     * <li>firstperson_righthand: ItemRenderer.renderItemInFirstPerson (ItemRenderer.java:421-431, then
     *     renderItem :57-61) draws Ry(45) S(0.4) G after the arm translate and swing turns, which
     *     ItemInHandRenderer.applyItemArmTransform / applyItemArmAttackTransform reproduce: block.json's (0, 45, 0).</li>
     * <li>thirdperson: RenderPlayer.renderSpecials (RenderPlayer.java:316-338, RenderBiped the same) draws
     *     Rx(20) Ry(45) S(-0.375, -0.375, 0.375) G in the arm frame; behind ItemInHandLayer's Rx(-90) Ry(180)
     *     that is rotation (70, -45, 0). Kept at block.json's 75-degree tilt: (75, -45, 0).</li>
     * <li>head: RenderPlayer.java:214-218 draws Ry(90) S(0.625, -0.625, -0.625) G; CustomHeadLayer.translateToHead
     *     turns Ry(180) before the same scale, so (0, 90, 0).</li>
     * <li>fixed: RenderItemFrame.java:96 turns Ry(180 + yaw) and RenderItem.doRenderItem's renderInFrame branch
     *     Ry(-90) (RenderItem.java:96-101); ItemFrameRenderer's Ry(180 - yRot) is 180 degrees from 1.5.2's
     *     turn for the same wall face, so (0, 90, 0).</li>
     * <li>ground and firstperson_lefthand: block.json (dropped items spin; 1.5.2 had no left hand). The wicker
     *     basket's GROUND draw matches FCTileEntityBasketWickerRenderer's unturned EntityItem (:48-75).</li>
     * </ul>
     */
    private static final ItemTransforms FC_ITEM_TRANSFORMS =
            new ItemTransforms(
                    // thirdperson_lefthand (apply() mirrors the Y/Z turns for the left arm, as block.json)
                    new net.minecraft.client.renderer.block.model.ItemTransform(
                            new org.joml.Vector3f(75, -45, 0),
                            new org.joml.Vector3f(0, 2.5f / 16f, 0),
                            new org.joml.Vector3f(0.375f, 0.375f, 0.375f)),
                    // thirdperson_righthand
                    new net.minecraft.client.renderer.block.model.ItemTransform(
                            new org.joml.Vector3f(75, -45, 0),
                            new org.joml.Vector3f(0, 2.5f / 16f, 0),
                            new org.joml.Vector3f(0.375f, 0.375f, 0.375f)),
                    // firstperson_lefthand
                    new net.minecraft.client.renderer.block.model.ItemTransform(
                            new org.joml.Vector3f(0, 225, 0),
                            new org.joml.Vector3f(0, 0, 0),
                            new org.joml.Vector3f(0.4f, 0.4f, 0.4f)),
                    // firstperson_righthand
                    new net.minecraft.client.renderer.block.model.ItemTransform(
                            new org.joml.Vector3f(0, 45, 0),
                            new org.joml.Vector3f(0, 0, 0),
                            new org.joml.Vector3f(0.4f, 0.4f, 0.4f)),
                    // head
                    new net.minecraft.client.renderer.block.model.ItemTransform(
                            new org.joml.Vector3f(0, 90, 0),
                            new org.joml.Vector3f(0, 0, 0),
                            new org.joml.Vector3f(1, 1, 1)),
                    // gui
                    new net.minecraft.client.renderer.block.model.ItemTransform(
                            new org.joml.Vector3f(30, 315, 0),
                            new org.joml.Vector3f(0, 0, 0),
                            new org.joml.Vector3f(0.625f, 0.625f, 0.625f)),
                    // ground
                    new net.minecraft.client.renderer.block.model.ItemTransform(
                            new org.joml.Vector3f(0, 0, 0),
                            new org.joml.Vector3f(0, 3f / 16f, 0),
                            new org.joml.Vector3f(0.25f, 0.25f, 0.25f)),
                    // fixed (item frame)
                    new net.minecraft.client.renderer.block.model.ItemTransform(
                            new org.joml.Vector3f(0, 90, 0),
                            new org.joml.Vector3f(0, 0, 0),
                            new org.joml.Vector3f(0.5f, 0.5f, 0.5f))
            );

    @Override
    public net.minecraft.client.renderer.block.model.ItemTransforms getTransforms() {
        return BLOCK_TRANSFORMS;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        ensureStaticQuadsBuilt();
        return particleSprite != null ? particleSprite : getMissingSprite();
    }

    /** The {@code <block>#inventory} model is this one; its overrides pick the stack damage's item model. */
    @Override
    public ItemOverrides getOverrides() { return itemOverrides; }

    // ================================================================
    // Texture sprite lookup
    // ================================================================

    /** Shared by mesh threads (static/live capture) and the render thread (FCItemSubtypeModel item
     *  quads, clearSpriteCache at ModifyBakingResult and after the block atlas upload), hence
     *  concurrent. */
    private static final Map<String, CachedSprite> spriteCache = new ConcurrentHashMap<>();

    /**
     * Bumped by {@link #clearSpriteCache}. A resource reload clears the cache at ModifyBakingResult
     * (background) but uploads the new atlas later on the game thread; a live capture on a mesh
     * thread in between resolves pre-reload sprites. Entries are tagged with the generation read
     * before their lookup started, so one resolved before the atlas upload but stored after the
     * post-upload clear is rejected instead of serving stale atlas UVs until the next reload.
     */
    private static final java.util.concurrent.atomic.AtomicInteger spriteCacheGeneration =
            new java.util.concurrent.atomic.AtomicInteger();

    private record CachedSprite(TextureAtlasSprite sprite, int generation) {}

    private static final Map<String, String> VANILLA_TEXTURE_MAP = new HashMap<>();
    static {
        // Planks
        VANILLA_TEXTURE_MAP.put("wood", "oak_planks");
        VANILLA_TEXTURE_MAP.put("wood_spruce", "spruce_planks");
        VANILLA_TEXTURE_MAP.put("wood_birch", "birch_planks");
        VANILLA_TEXTURE_MAP.put("wood_jungle", "jungle_planks");
        // Logs
        VANILLA_TEXTURE_MAP.put("tree_side", "oak_log");
        VANILLA_TEXTURE_MAP.put("tree_top", "oak_log_top");
        VANILLA_TEXTURE_MAP.put("tree_spruce", "spruce_log");
        VANILLA_TEXTURE_MAP.put("tree_birch", "birch_log");
        VANILLA_TEXTURE_MAP.put("tree_jungle", "jungle_log");
        // Ores
        VANILLA_TEXTURE_MAP.put("oregold", "gold_ore");
        VANILLA_TEXTURE_MAP.put("oreiron", "iron_ore");
        VANILLA_TEXTURE_MAP.put("orecoal", "coal_ore");
        VANILLA_TEXTURE_MAP.put("orediamond", "diamond_ore");
        VANILLA_TEXTURE_MAP.put("orelapis", "lapis_ore");
        VANILLA_TEXTURE_MAP.put("oreredstone", "redstone_ore");
        VANILLA_TEXTURE_MAP.put("oreemerald", "emerald_ore");
        // Metal/gem blocks
        VANILLA_TEXTURE_MAP.put("blockgold", "gold_block");
        VANILLA_TEXTURE_MAP.put("blockiron", "iron_block");
        VANILLA_TEXTURE_MAP.put("blockdiamond", "diamond_block");
        VANILLA_TEXTURE_MAP.put("blocklapis", "lapis_block");
        VANILLA_TEXTURE_MAP.put("blockemerald", "emerald_block");
        // Wool
        VANILLA_TEXTURE_MAP.put("cloth_0", "white_wool");
        VANILLA_TEXTURE_MAP.put("cloth_1", "orange_wool");
        VANILLA_TEXTURE_MAP.put("cloth_2", "magenta_wool");
        VANILLA_TEXTURE_MAP.put("cloth_3", "light_blue_wool");
        VANILLA_TEXTURE_MAP.put("cloth_4", "yellow_wool");
        VANILLA_TEXTURE_MAP.put("cloth_5", "lime_wool");
        VANILLA_TEXTURE_MAP.put("cloth_6", "pink_wool");
        VANILLA_TEXTURE_MAP.put("cloth_7", "gray_wool");
        VANILLA_TEXTURE_MAP.put("cloth_8", "light_gray_wool");
        VANILLA_TEXTURE_MAP.put("cloth_9", "cyan_wool");
        VANILLA_TEXTURE_MAP.put("cloth_10", "purple_wool");
        VANILLA_TEXTURE_MAP.put("cloth_11", "blue_wool");
        VANILLA_TEXTURE_MAP.put("cloth_12", "brown_wool");
        VANILLA_TEXTURE_MAP.put("cloth_13", "green_wool");
        VANILLA_TEXTURE_MAP.put("cloth_14", "red_wool");
        VANILLA_TEXTURE_MAP.put("cloth_15", "black_wool");
        // Stone bricks
        VANILLA_TEXTURE_MAP.put("stonebricksmooth", "stone_bricks");
        VANILLA_TEXTURE_MAP.put("stonebricksmooth_mossy", "mossy_stone_bricks");
        VANILLA_TEXTURE_MAP.put("stonebricksmooth_cracked", "cracked_stone_bricks");
        VANILLA_TEXTURE_MAP.put("stonebricksmooth_carved", "chiseled_stone_bricks");
        VANILLA_TEXTURE_MAP.put("stonebrick", "cobblestone");
        // Grass
        VANILLA_TEXTURE_MAP.put("grass_top", "grass_block_top");
        VANILLA_TEXTURE_MAP.put("grass_side", "grass_block_side");
        VANILLA_TEXTURE_MAP.put("grass_side_overlay", "grass_block_side_overlay");
        VANILLA_TEXTURE_MAP.put("snow_side", "grass_block_snow");
        // Crafting table
        VANILLA_TEXTURE_MAP.put("workbench_top", "crafting_table_top");
        VANILLA_TEXTURE_MAP.put("workbench_side", "crafting_table_side");
        VANILLA_TEXTURE_MAP.put("workbench_front", "crafting_table_front");
        VANILLA_TEXTURE_MAP.put("workbench", "crafting_table_front");
        // Furnace
        VANILLA_TEXTURE_MAP.put("furnace_front_off", "furnace_front");
        VANILLA_TEXTURE_MAP.put("furnace_front_on", "furnace_front_on");
        // Sandstone
        VANILLA_TEXTURE_MAP.put("sandstone_side", "sandstone");
        VANILLA_TEXTURE_MAP.put("sandstone_carved", "chiseled_sandstone");
        VANILLA_TEXTURE_MAP.put("sandstone_smooth", "cut_sandstone");
        // Misc
        VANILLA_TEXTURE_MAP.put("web", "cobweb");
        VANILLA_TEXTURE_MAP.put("brick", "bricks");
        VANILLA_TEXTURE_MAP.put("cobblestonemossy", "mossy_cobblestone");
        VANILLA_TEXTURE_MAP.put("nether_brick", "nether_bricks");
        VANILLA_TEXTURE_MAP.put("endstone", "end_stone");
        VANILLA_TEXTURE_MAP.put("reeds", "sugar_cane");
        VANILLA_TEXTURE_MAP.put("waterlily", "lily_pad");
        VANILLA_TEXTURE_MAP.put("mycel_side", "mycelium_side");
        VANILLA_TEXTURE_MAP.put("mycel_top", "mycelium_top");
        VANILLA_TEXTURE_MAP.put("pumpkin_face_off", "carved_pumpkin");
        VANILLA_TEXTURE_MAP.put("pumpkin_face_on", "jack_o_lantern");
        VANILLA_TEXTURE_MAP.put("hellrock", "netherrack");
        VANILLA_TEXTURE_MAP.put("hellsand", "soul_sand");
        VANILLA_TEXTURE_MAP.put("netherquartz", "nether_quartz_ore");
        // Keys are looked up lower-cased (NamedIcon lower-cases every icon name).
        VANILLA_TEXTURE_MAP.put("whitestone", "end_stone");
        // Fire
        VANILLA_TEXTURE_MAP.put("fire_layer_0", "fire_0");
        VANILLA_TEXTURE_MAP.put("fire_layer_1", "fire_1");
        // Stoked fire (Hibachi): texture-resolution step 1 finds the stitched
        // betterthanwolves:block/fcblockfirestokedstub_0/1 sprites, over which the block's
        // custom RenderBlock flame geometry is baked. StokedFireTexture then drives those
        // sprites at runtime with FC's actual procedural fire simulation (re-uploaded to the
        // atlas each tick), reproducing FC's FCClientAnimationFire. These soul-fire entries
        // are only a fallback if that sprite is ever missing.
        VANILLA_TEXTURE_MAP.put("fcblockfirestokedstub_0", "soul_fire_0");
        VANILLA_TEXTURE_MAP.put("fcblockfirestokedstub_1", "soul_fire_1");
        // 1.20.1 still calls the tall-grass texture block/grass (short_grass is 1.20.3+).
        VANILLA_TEXTURE_MAP.put("tallgrass", "grass");
        VANILLA_TEXTURE_MAP.put("musicblock", "note_block");
        VANILLA_TEXTURE_MAP.put("blockcoal", "coal_block");
        VANILLA_TEXTURE_MAP.put("blockredstone", "redstone_block");
        // Leaves
        VANILLA_TEXTURE_MAP.put("leaves", "oak_leaves");
        VANILLA_TEXTURE_MAP.put("leaves_spruce", "spruce_leaves");
        VANILLA_TEXTURE_MAP.put("leaves_birch", "birch_leaves");
        VANILLA_TEXTURE_MAP.put("leaves_jungle", "jungle_leaves");
        VANILLA_TEXTURE_MAP.put("leaves_opaque", "oak_leaves");
        VANILLA_TEXTURE_MAP.put("leaves_spruce_opaque", "spruce_leaves");
        VANILLA_TEXTURE_MAP.put("leaves_birch_opaque", "birch_leaves");
        VANILLA_TEXTURE_MAP.put("leaves_jungle_opaque", "jungle_leaves");
        // Plants (vanilla/client Block.java:206-209 plantYellow "flower", plantRed "rose",
        // BlockMushroom "mushroom_brown"/"mushroom_red"; BlockSapling.java:23; BlockTallGrass.java:22;
        // BlockRedstoneTorch.java:308-312). Reached by FC captures, not world plants (vanilla blocks):
        // the iron golem's poppy (RenderIronGolem), blocks endermen carry, mob-held and floating items.
        VANILLA_TEXTURE_MAP.put("rose", "poppy");
        VANILLA_TEXTURE_MAP.put("flower", "dandelion");
        VANILLA_TEXTURE_MAP.put("mushroom_brown", "brown_mushroom");
        VANILLA_TEXTURE_MAP.put("mushroom_red", "red_mushroom");
        VANILLA_TEXTURE_MAP.put("sapling", "oak_sapling");
        VANILLA_TEXTURE_MAP.put("sapling_spruce", "spruce_sapling");
        VANILLA_TEXTURE_MAP.put("sapling_birch", "birch_sapling");
        VANILLA_TEXTURE_MAP.put("sapling_jungle", "jungle_sapling");
        VANILLA_TEXTURE_MAP.put("deadbush", "dead_bush");
        VANILLA_TEXTURE_MAP.put("redtorch", "redstone_torch_off");
        VANILLA_TEXTURE_MAP.put("redtorch_lit", "redstone_torch");
        // Doors
        VANILLA_TEXTURE_MAP.put("doorwood", "oak_door_top");
        VANILLA_TEXTURE_MAP.put("doorwood_lower", "oak_door_bottom");
        VANILLA_TEXTURE_MAP.put("doorwood_upper", "oak_door_top");
        VANILLA_TEXTURE_MAP.put("dooriron", "iron_door_top");
        VANILLA_TEXTURE_MAP.put("dooriron_lower", "iron_door_bottom");
        VANILLA_TEXTURE_MAP.put("dooriron_upper", "iron_door_top");
        // Farmland
        VANILLA_TEXTURE_MAP.put("farmland_dry", "farmland");
        VANILLA_TEXTURE_MAP.put("farmland_wet", "farmland_moist");
        // Mushroom
        VANILLA_TEXTURE_MAP.put("mushroom", "red_mushroom_block");
        VANILLA_TEXTURE_MAP.put("mushroom_skin_brown", "brown_mushroom_block");
        VANILLA_TEXTURE_MAP.put("mushroom_skin_red", "red_mushroom_block");
        VANILLA_TEXTURE_MAP.put("mushroom_skin_stem", "mushroom_stem");
        VANILLA_TEXTURE_MAP.put("mushroom_inside", "mushroom_block_inside");
        VANILLA_TEXTURE_MAP.put("pressureplate", "oak_planks");
        VANILLA_TEXTURE_MAP.put("chest", "oak_planks");
        // Piston
        VANILLA_TEXTURE_MAP.put("piston_top_normal", "piston_top");
        VANILLA_TEXTURE_MAP.put("piston_top_sticky", "piston_top_sticky");
        // Dispenser
        VANILLA_TEXTURE_MAP.put("dispenser_front_horizontal", "dispenser_front");
        VANILLA_TEXTURE_MAP.put("dispenser_front_vertical", "dispenser_front_vertical");
        VANILLA_TEXTURE_MAP.put("dropper_front_horizontal", "dropper_front");
        VANILLA_TEXTURE_MAP.put("dropper_front_vertical", "dropper_front_vertical");
        // Quartz
        VANILLA_TEXTURE_MAP.put("quartzblock_side", "quartz_block_side");
        VANILLA_TEXTURE_MAP.put("quartzblock_top", "quartz_block_top");
        VANILLA_TEXTURE_MAP.put("quartzblock_bottom", "quartz_block_bottom");
        VANILLA_TEXTURE_MAP.put("quartzblock_chiseled", "chiseled_quartz_block");
        VANILLA_TEXTURE_MAP.put("quartzblock_chiseled_top", "chiseled_quartz_block_top");
        VANILLA_TEXTURE_MAP.put("quartzblock_lines", "quartz_pillar");
        VANILLA_TEXTURE_MAP.put("quartzblock_lines_top", "quartz_pillar_top");
        // Redstone lamp
        VANILLA_TEXTURE_MAP.put("redstone_lamp_off", "redstone_lamp");
        VANILLA_TEXTURE_MAP.put("redstone_lamp_on", "redstone_lamp_on");
        // End portal frame
        VANILLA_TEXTURE_MAP.put("endframe_side", "end_portal_frame_side");
        VANILLA_TEXTURE_MAP.put("endframe_top", "end_portal_frame_top");
        VANILLA_TEXTURE_MAP.put("endframe_eye", "end_portal_frame_eye");
        // Terracotta
        VANILLA_TEXTURE_MAP.put("hardened_clay", "terracotta");
        VANILLA_TEXTURE_MAP.put("hardened_clay_stained_white", "white_terracotta");
        VANILLA_TEXTURE_MAP.put("hardened_clay_stained_orange", "orange_terracotta");
        VANILLA_TEXTURE_MAP.put("hardened_clay_stained_magenta", "magenta_terracotta");
        VANILLA_TEXTURE_MAP.put("hardened_clay_stained_light_blue", "light_blue_terracotta");
        VANILLA_TEXTURE_MAP.put("hardened_clay_stained_yellow", "yellow_terracotta");
        VANILLA_TEXTURE_MAP.put("hardened_clay_stained_lime", "lime_terracotta");
        VANILLA_TEXTURE_MAP.put("hardened_clay_stained_pink", "pink_terracotta");
        VANILLA_TEXTURE_MAP.put("hardened_clay_stained_gray", "gray_terracotta");
        VANILLA_TEXTURE_MAP.put("hardened_clay_stained_silver", "light_gray_terracotta");
        VANILLA_TEXTURE_MAP.put("hardened_clay_stained_cyan", "cyan_terracotta");
        VANILLA_TEXTURE_MAP.put("hardened_clay_stained_purple", "purple_terracotta");
        VANILLA_TEXTURE_MAP.put("hardened_clay_stained_blue", "blue_terracotta");
        VANILLA_TEXTURE_MAP.put("hardened_clay_stained_brown", "brown_terracotta");
        VANILLA_TEXTURE_MAP.put("hardened_clay_stained_green", "green_terracotta");
        VANILLA_TEXTURE_MAP.put("hardened_clay_stained_red", "red_terracotta");
        VANILLA_TEXTURE_MAP.put("hardened_clay_stained_black", "black_terracotta");
        // Hay/hopper
        VANILLA_TEXTURE_MAP.put("hayblock_side", "hay_block_side");
        VANILLA_TEXTURE_MAP.put("hayblock_top", "hay_block_top");
        VANILLA_TEXTURE_MAP.put("hopper_inside", "hopper_inside");
        VANILLA_TEXTURE_MAP.put("hopper_outside", "hopper_outside");
        VANILLA_TEXTURE_MAP.put("hopper_top", "hopper_top");
    }

    /**
     * 1.5.2 vanilla ITEM icon name (lower-cased) -> 1.20.1 minecraft:item/ texture, for the names
     * 1.20.1 changed. Item icons are the unlocalized names (vanilla/client Item.java:41-438 and the
     * FCMOD replacements, registered by Item.registerIcons :927-930) plus the subclass icon sets:
     * ItemBow.java:19, ItemArmor.java:21-22, ItemDye.java:23, ItemPotion.java:393-395,
     * ItemFishingRod.java:75, ItemMonsterPlacer.java:156, ItemFireworkCharge.java:233 and
     * ItemRecord.java:106. Mob held items (RenderBiped/RenderWitch through ItemRenderer.renderItem)
     * and FC TESR/entity item draws reach lookupSprite with these names. Names 1.20.1 kept (bow,
     * stick, bone, apple, ...) already resolve as minecraft:item/name and are not listed. 1.5.2 drew
     * items and blocks from separate sheets; a name that is also a terrain icon (clay, brick, reeds,
     * netherquartz, comparator, doorWood, doorIron) cannot be told apart here and keeps resolving to
     * the block texture, so it is not listed. The door blocks register the bare names too (the
     * BlockDoor stub keeps Block.registerIcons), so the block mapping must win for them.
     * Filled once in the static initializer and never written again, so lock-free reads are safe.
     */
    private static final Map<String, String> VANILLA_ITEM_TEXTURE_MAP = new HashMap<>();

    private static void item(String iconName152, String itemTexture120) {
        VANILLA_ITEM_TEXTURE_MAP.put(iconName152.toLowerCase(Locale.ROOT), itemTexture120);
    }

    static {
        // Tools and weapons (Item.java:41-105, 112-128; ItemBow.java:19; ItemFishingRod.java:75)
        item("shovelIron", "iron_shovel");
        item("pickaxeIron", "iron_pickaxe");
        item("hatchetIron", "iron_axe");
        item("flintAndSteel", "flint_and_steel");
        item("swordIron", "iron_sword");
        item("swordWood", "wooden_sword");
        item("shovelWood", "wooden_shovel");
        item("pickaxeWood", "wooden_pickaxe");
        item("hatchetWood", "wooden_axe");
        item("swordStone", "stone_sword");
        item("shovelStone", "stone_shovel");
        item("pickaxeStone", "stone_pickaxe");
        item("hatchetStone", "stone_axe");
        item("swordDiamond", "diamond_sword");
        item("shovelDiamond", "diamond_shovel");
        item("pickaxeDiamond", "diamond_pickaxe");
        item("hatchetDiamond", "diamond_axe");
        item("swordGold", "golden_sword");
        item("shovelGold", "golden_shovel");
        item("pickaxeGold", "golden_pickaxe");
        item("hatchetGold", "golden_axe");
        item("hoeWood", "wooden_hoe");
        item("hoeStone", "stone_hoe");
        item("hoeIron", "iron_hoe");
        item("hoeDiamond", "diamond_hoe");
        item("hoeGold", "golden_hoe");
        item("bow_pull_0", "bow_pulling_0");
        item("bow_pull_1", "bow_pulling_1");
        item("bow_pull_2", "bow_pulling_2");
        item("fishingRod", "fishing_rod");
        item("fishingRod_empty", "fishing_rod_cast");
        item("carrotOnAStick", "carrot_on_a_stick");
        // Armour (Item.java:135-175; ItemArmor.java:21-22)
        item("helmetCloth", "leather_helmet");
        item("chestplateCloth", "leather_chestplate");
        item("leggingsCloth", "leather_leggings");
        item("bootsCloth", "leather_boots");
        item("helmetCloth_overlay", "leather_helmet_overlay");
        item("chestplateCloth_overlay", "leather_chestplate_overlay");
        item("leggingsCloth_overlay", "leather_leggings_overlay");
        item("bootsCloth_overlay", "leather_boots_overlay");
        item("helmetChain", "chainmail_helmet");
        item("chestplateChain", "chainmail_chestplate");
        item("leggingsChain", "chainmail_leggings");
        item("bootsChain", "chainmail_boots");
        item("helmetIron", "iron_helmet");
        item("chestplateIron", "iron_chestplate");
        item("leggingsIron", "iron_leggings");
        item("bootsIron", "iron_boots");
        item("helmetDiamond", "diamond_helmet");
        item("chestplateDiamond", "diamond_chestplate");
        item("leggingsDiamond", "diamond_leggings");
        item("bootsDiamond", "diamond_boots");
        item("helmetGold", "golden_helmet");
        item("chestplateGold", "golden_chestplate");
        item("leggingsGold", "golden_leggings");
        item("bootsGold", "golden_boots");
        item("slot_empty_helmet", "empty_armor_slot_helmet");
        item("slot_empty_chestplate", "empty_armor_slot_chestplate");
        item("slot_empty_leggings", "empty_armor_slot_leggings");
        item("slot_empty_boots", "empty_armor_slot_boots");
        // Materials, food and misc (Item.java:45-438; ItemDye.java:23; ItemPotion.java:393-395)
        item("appleGold", "golden_apple");
        item("ingotIron", "iron_ingot");
        item("ingotGold", "gold_ingot");
        item("mushroomStew", "mushroom_stew");
        item("sulphur", "gunpowder");
        item("seeds", "wheat_seeds");
        item("porkchopRaw", "porkchop");
        item("porkchopCooked", "cooked_porkchop");
        item("sign", "oak_sign");
        item("bucketWater", "water_bucket");
        item("bucketLava", "lava_bucket");
        item("minecartChest", "chest_minecart");
        item("minecartFurnace", "furnace_minecart");
        item("minecartHopper", "hopper_minecart");
        item("minecartTnt", "tnt_minecart");
        item("boat", "oak_boat");
        item("milk", "milk_bucket");
        item("slimeball", "slime_ball");
        item("compass", "compass_00");
        item("clock", "clock_00");
        item("yellowDust", "glowstone_dust");
        item("fishRaw", "cod");
        item("fishCooked", "cooked_cod");
        item("dyePowder_black", "ink_sac");
        item("dyePowder_red", "red_dye");
        item("dyePowder_green", "green_dye");
        item("dyePowder_brown", "cocoa_beans");
        item("dyePowder_blue", "lapis_lazuli");
        item("dyePowder_purple", "purple_dye");
        item("dyePowder_cyan", "cyan_dye");
        item("dyePowder_silver", "light_gray_dye");
        item("dyePowder_gray", "gray_dye");
        item("dyePowder_pink", "pink_dye");
        item("dyePowder_lime", "lime_dye");
        item("dyePowder_yellow", "yellow_dye");
        item("dyePowder_lightBlue", "light_blue_dye");
        item("dyePowder_magenta", "magenta_dye");
        item("dyePowder_orange", "orange_dye");
        item("dyePowder_white", "bone_meal");
        item("diode", "repeater");
        item("map", "filled_map");
        item("emptyMap", "map");
        item("melon", "melon_slice");
        item("seeds_pumpkin", "pumpkin_seeds");
        item("seeds_melon", "melon_seeds");
        item("beefRaw", "beef");
        item("beefCooked", "cooked_beef");
        item("chickenRaw", "chicken");
        item("chickenCooked", "cooked_chicken");
        item("rottenFlesh", "rotten_flesh");
        item("enderPearl", "ender_pearl");
        item("blazeRod", "blaze_rod");
        item("ghastTear", "ghast_tear");
        item("goldNugget", "gold_nugget");
        item("netherStalkSeeds", "nether_wart");
        item("potion_splash", "splash_potion");
        item("potion_contents", "potion_overlay");
        item("glassBottle", "glass_bottle");
        item("spiderEye", "spider_eye");
        item("fermentedSpiderEye", "fermented_spider_eye");
        item("blazePowder", "blaze_powder");
        item("magmaCream", "magma_cream");
        item("brewingStand", "brewing_stand");
        item("eyeOfEnder", "ender_eye");
        item("speckledMelon", "glistering_melon_slice");
        item("monsterPlacer", "spawn_egg");
        item("monsterPlacer_overlay", "spawn_egg_overlay");
        item("expBottle", "experience_bottle");
        item("fireball", "fire_charge");
        item("writingBook", "writable_book");
        item("writtenBook", "written_book");
        item("frame", "item_frame");
        item("flowerPot", "flower_pot");
        item("carrots", "carrot");
        item("potatoBaked", "baked_potato");
        item("potatoPoisonous", "poisonous_potato");
        item("carrotGolden", "golden_carrot");
        item("netherStar", "nether_star");
        item("pumpkinPie", "pumpkin_pie");
        item("fireworks", "firework_rocket");
        item("fireworksCharge", "firework_star");
        item("fireworksCharge_overlay", "firework_star_overlay");
        item("enchantedBook", "enchanted_book");
        item("netherbrick", "nether_brick");
        // Music discs (ItemRecord.java:106, "record_" + recordName)
        for (String disc : new String[] {"13", "cat", "blocks", "chirp", "far", "mall", "mellohi", "stal",
                "strad", "ward", "11", "wait"}) {
            item("record_" + disc, "music_disc_" + disc);
        }
    }

    static TextureAtlasSprite lookupSprite(String textureName) {
        if (textureName == null || textureName.isEmpty() || textureName.equals("unknown")) {
            return getMissingSprite();
        }
        // Partial suffix-only names from blocks with empty getTextureName()
        if (textureName.startsWith("_")) {
            return getMissingSprite();
        }

        int generation = spriteCacheGeneration.get();
        CachedSprite cached = spriteCache.get(textureName);
        if (cached != null && cached.generation() == generation) return cached.sprite();

        // 1. Try BTW namespace: betterthanwolves:block/<name>
        TextureAtlasSprite sprite = tryLookupInNamespace(BTWForgeMod.MOD_ID, "block/" + textureName);

        // 2. Try vanilla 1.5.2 → 1.20.1 name mapping
        if (sprite == null) {
            String mapped = VANILLA_TEXTURE_MAP.get(textureName);
            if (mapped != null) {
                sprite = tryLookupInNamespace("minecraft", "block/" + mapped);
            }
        }

        // 3. Try minecraft namespace with original name
        if (sprite == null) {
            sprite = tryLookupInNamespace("minecraft", "block/" + textureName);
        }

        // 4. Try item/ prefix (for FC items with subtype textures)
        if (sprite == null) {
            sprite = tryLookupInNamespace(BTWForgeMod.MOD_ID, "item/" + textureName);
        }
        // 1.5.2 vanilla item icon -> 1.20.1 item texture (swordIron -> iron_sword). Looked up
        // lower-cased: FCEntityRenderer passes lower-cased names, block captures pass them as
        // registered. Before minecraft:item/<name>, which is a different item for "map".
        if (sprite == null) {
            String mappedItem = VANILLA_ITEM_TEXTURE_MAP.get(textureName.toLowerCase(Locale.ROOT));
            if (mappedItem != null) {
                sprite = tryLookupInNamespace("minecraft", "item/" + mappedItem);
            }
        }
        if (sprite == null) {
            String mapped = VANILLA_TEXTURE_MAP.get(textureName);
            if (mapped != null) {
                sprite = tryLookupInNamespace("minecraft", "item/" + mapped);
            }
        }
        if (sprite == null) {
            sprite = tryLookupInNamespace("minecraft", "item/" + textureName);
        }

        // 5. Try without prefix
        if (sprite == null) {
            sprite = tryLookupInNamespace(BTWForgeMod.MOD_ID, textureName);
        }
        if (sprite == null) {
            sprite = tryLookupInNamespace("minecraft", textureName);
        }

        if (sprite == null) {
            LOGGER.warn("BTW: Texture not found: '{}' (mapped: {}, item: {})",
                    textureName, VANILLA_TEXTURE_MAP.get(textureName),
                    VANILLA_ITEM_TEXTURE_MAP.get(textureName.toLowerCase(Locale.ROOT)));
            sprite = getMissingSprite();
        }

        // ConcurrentHashMap rejects null values; getMissingSprite() returns null if the atlas is gone.
        if (sprite != null) spriteCache.put(textureName, new CachedSprite(sprite, generation));
        return sprite;
    }

    private static TextureAtlasSprite tryLookupInNamespace(String namespace, String path) {
        try {
            ResourceLocation loc = new ResourceLocation(namespace, path);
            TextureAtlasSprite sprite = Minecraft.getInstance()
                    .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(loc);
            if (sprite != null && !sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
                return sprite;
            }
        } catch (Exception e) {
            // texture not found
        }
        return null;
    }

    private static TextureAtlasSprite getMissingSprite() {
        try {
            return Minecraft.getInstance()
                    .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(MissingTextureAtlasSprite.getLocation());
        } catch (Exception e) {
            return null;
        }
    }

    /** Called at ModifyBakingResult and after the block atlas upload (BTWClientEvents). */
    public static void clearSpriteCache() {
        spriteCacheGeneration.incrementAndGet();   // before clear: in-flight lookups store stale-tagged
        spriteCache.clear();
    }
}
