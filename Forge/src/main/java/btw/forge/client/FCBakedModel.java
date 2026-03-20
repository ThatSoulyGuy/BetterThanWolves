package btw.forge.client;

import btw.forge.BTWForgeMod;
import btw.forge.NamedIcon;
import btw.forge.ProxyBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A BakedModel that captures FC's actual rendering output on first use.
 *
 * <p>Created during model baking (worker thread) with just a reference to
 * the FC block. On the first {@code getQuads()} call (render thread, where
 * GL context and texture atlas are available), runs FC's
 * {@code RenderBlockAsItem} through the Tessellator capture pipeline and
 * converts the captured vertices directly to BakedQuads.
 *
 * <p>This ensures FC's custom block shapes, per-face textures, and
 * per-meta variants are faithfully reproduced.
 */
public class FCBakedModel implements BakedModel {

    private static final Logger LOGGER = LogManager.getLogger("BTW-FCBakedModel");

    // --- Instance state ---

    /** The FC block to capture rendering from. Null after capture completes. */
    private btw.modern.Block fcBlock;
    private int legacyBlockId;

    /** Built quads indexed by metadata. Null until first getQuads(). */
    private volatile Map<Integer, List<BakedQuad>> allQuadsByMeta;
    private volatile TextureAtlasSprite particleSprite;

    // ================================================================
    // Factory
    // ================================================================

    /**
     * Creates a deferred-capture FCBakedModel. No vertex capture happens
     * until the first getQuads() call on the render thread.
     */
    public static FCBakedModel deferred(btw.modern.Block fcBlock, int legacyId) {
        FCBakedModel model = new FCBakedModel();
        model.fcBlock = fcBlock;
        model.legacyBlockId = legacyId;
        return model;
    }

    private FCBakedModel() {}

    // ================================================================
    // Deferred capture — runs once, guarded by a global lock
    // ================================================================

    /**
     * Global lock for Tessellator capture. btw.modern.Tessellator.instance is
     * a static singleton — concurrent captures from different chunk-builder
     * threads would corrupt each other's vertex data. This lock serialises
     * all capture operations across all FCBakedModel instances.
     */
    private static final Object CAPTURE_LOCK = new Object();

    private void ensureCaptured() {
        if (allQuadsByMeta != null) return;
        synchronized (CAPTURE_LOCK) {
            if (allQuadsByMeta != null) return;
            doCaptureAndBuild();
        }
    }

    private void doCaptureAndBuild() {
        LOGGER.info("BTW: doCaptureAndBuild starting for block {} on thread {}. " +
                        "Sprite test: fcblockanchor={}, stone={}",
                legacyBlockId, Thread.currentThread().getName(),
                lookupSprite("fcblockanchor") != null ? lookupSprite("fcblockanchor").contents().name() : "NULL",
                lookupSprite("stone") != null ? lookupSprite("stone").contents().name() : "NULL");
        btw.modern.Block block = this.fcBlock;
        if (block == null) {
            allQuadsByMeta = Collections.emptyMap();
            particleSprite = getMissingSprite();
            return;
        }

        // Step 1: Register icons (populates icon fields with NamedIcons)
        btw.modern.IconRegister capturer = new btw.modern.IconRegister() {
            @Override
            public btw.modern.Icon registerIcon(String name) {
                return new NamedIcon(name);
            }
            @Override
            public btw.modern.Icon registerIcon(String name, btw.modern.TextureStitched tex) {
                return new NamedIcon(name);
            }
        };
        try {
            block.registerIcons(capturer);
        } catch (Exception e) {
            LOGGER.debug("BTW: registerIcons failed for block {}: {}", legacyBlockId, e.getMessage());
        }

        // Step 2: For each meta, capture vertices by calling FC's RenderBlockAsItem
        btw.modern.RenderBlocks renderer = new btw.modern.RenderBlocks();
        Map<Integer, List<BakedQuad>> result = new HashMap<>();
        int totalQuads = 0;
        Set<String> allTexNames = new HashSet<>();

        for (int meta = 0; meta < 16; meta++) {
            btw.modern.Tessellator tess = btw.modern.Tessellator.instance;
            tess.setTranslation(0, 0, 0);
            tess.setNormal(0, 1, 0);
            tess.setColorOpaque_F(1, 1, 1);
            tess.setTextureUV(0, 0);
            tess.startCapturing();

            try {
                renderer.setRenderBounds(0, 0, 0, 1, 1, 1);
                renderer.unlockBlockBounds();
                // Call FC's block-level RenderBlockAsItem which handles custom
                // shapes (campfire, anvil, etc.) and per-face textures.
                // GL11 calls in FC code are remapped to btw.modern.GL11 (no-ops)
                // by the shadow plugin, so they won't crash.
                block.RenderBlockAsItem(renderer, meta, 1.0f);
            } catch (Throwable e) {
                LOGGER.warn("BTW: RenderBlockAsItem threw for block {} meta {}: {}",
                        legacyBlockId, meta, e.toString(), e);
            }

            List<btw.modern.Tessellator.CapturedQuad> captured = tess.stopCapturing();

            if (captured.isEmpty() && meta == 0) {
                // Log Tessellator state for debugging
                LOGGER.warn("BTW: Block {} ({}) RenderBlockAsItem produced 0 quads for meta 0. " +
                                "Tessellator.capturing={}, blockIcon={}",
                        legacyBlockId, block.getClass().getSimpleName(),
                        tess.isCapturing(),
                        block.blockIcon != null ? block.blockIcon.getIconName() : "NULL");
            }

            if (!captured.isEmpty()) {
                // Log raw vertex data from first quad of first successful meta
                if (totalQuads == 0 && meta == 0) {
                    btw.modern.Tessellator.CapturedQuad firstQ = captured.get(0);
                    for (int vi = 0; vi < 4; vi++) {
                        btw.modern.Tessellator.CapturedVertex v = firstQ.vertices[vi];
                        LOGGER.info("BTW: Block {} meta 0 quad 0 vert {}: pos=({},{},{}) uv=({},{}) color=({},{},{},{}) normal=({},{},{}) tex={}",
                                legacyBlockId, vi,
                                v.x, v.y, v.z, v.u, v.v,
                                v.r, v.g, v.b, v.a,
                                v.nx, v.ny, v.nz,
                                firstQ.textureName);
                    }
                }

                List<BakedQuad> bakedQuads = new ArrayList<>();
                for (btw.modern.Tessellator.CapturedQuad cq : captured) {
                    BakedQuad bq = convertCapturedQuad(cq);
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

        // Log results for ALL blocks during this diagnostic phase
        {
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
            LOGGER.info("BTW: Block {} ({}) captured {} metas, {} quads, textures: {}, getIcon: [{}]",
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

        this.allQuadsByMeta = result;
        // Release FC block reference — capture is done
        this.fcBlock = null;
    }

    // ================================================================
    // Captured quad → BakedQuad conversion
    // ================================================================

    private static BakedQuad convertCapturedQuad(btw.modern.Tessellator.CapturedQuad cq) {
        if (cq.vertices[0] == null) return null;

        btw.modern.Tessellator.CapturedVertex v0 = cq.vertices[0];
        Direction dir = directionFromNormal(v0.nx, v0.ny, v0.nz);

        TextureAtlasSprite sprite = lookupSprite(
                cq.textureName != null ? cq.textureName : "");
        if (sprite == null) return null;

        int[] vertexData = new int[32];
        for (int i = 0; i < 4; i++) {
            btw.modern.Tessellator.CapturedVertex v = cq.vertices[i];
            int base = i * 8;

            vertexData[base + 0] = Float.floatToRawIntBits((float) v.x);
            vertexData[base + 1] = Float.floatToRawIntBits((float) v.y);
            vertexData[base + 2] = Float.floatToRawIntBits((float) v.z);
            // Color (ABGR)
            int r = (int)(v.r * 255) & 0xFF;
            int g = (int)(v.g * 255) & 0xFF;
            int b = (int)(v.b * 255) & 0xFF;
            int a = (int)(v.a * 255) & 0xFF;
            vertexData[base + 3] = a << 24 | b << 16 | g << 8 | r;
            // UV: map 0-1 normalized to atlas sprite coordinates
            vertexData[base + 4] = Float.floatToRawIntBits(sprite.getU(v.u * 16));
            vertexData[base + 5] = Float.floatToRawIntBits(sprite.getV(v.v * 16));
            // Light: 0 = let MC compute from world lighting
            vertexData[base + 6] = 0;
            // Normal (packed signed bytes)
            int nx = ((byte)(v.nx * 127)) & 0xFF;
            int ny = ((byte)(v.ny * 127)) & 0xFF;
            int nz = ((byte)(v.nz * 127)) & 0xFF;
            vertexData[base + 7] = nx | (ny << 8) | (nz << 16);
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

    // ================================================================
    // BakedModel interface
    // ================================================================

    @Override
    public net.minecraftforge.client.ChunkRenderTypeSet getRenderTypes(
            BlockState state, RandomSource rand, net.minecraftforge.client.model.data.ModelData data) {
        // Use cutoutMipped so transparent pixels in FC textures render correctly
        // (alpha-tested transparency, with mipmapping). Solid blocks are unaffected.
        return net.minecraftforge.client.ChunkRenderTypeSet.of(
                net.minecraft.client.renderer.RenderType.cutoutMipped());
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
                                     net.minecraftforge.client.model.data.ModelData extraData,
                                     @Nullable net.minecraft.client.renderer.RenderType renderType) {
        return getQuads(state, side, rand);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        ensureCaptured();
        if (side != null) return Collections.emptyList(); // all quads are unculled

        int meta = 0;
        if (state != null && state.hasProperty(ProxyBlock.META)) {
            meta = state.getValue(ProxyBlock.META);
        }
        return allQuadsByMeta.getOrDefault(meta, Collections.emptyList());
    }

    @Override public boolean useAmbientOcclusion() { return true; }
    @Override public boolean isGui3d() { return true; }
    @Override public boolean usesBlockLight() { return true; }
    @Override public boolean isCustomRenderer() { return false; }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        ensureCaptured();
        return particleSprite != null ? particleSprite : getMissingSprite();
    }

    @Override
    public ItemOverrides getOverrides() { return ItemOverrides.EMPTY; }

    // ================================================================
    // Texture sprite lookup
    // ================================================================

    private static final Map<String, TextureAtlasSprite> spriteCache = new HashMap<>();

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
        VANILLA_TEXTURE_MAP.put("whiteStone", "end_stone");
        VANILLA_TEXTURE_MAP.put("tallgrass", "short_grass");
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

    static TextureAtlasSprite lookupSprite(String textureName) {
        if (textureName == null || textureName.isEmpty() || textureName.equals("unknown")) {
            return getMissingSprite();
        }
        // Partial suffix-only names from blocks with empty getTextureName()
        if (textureName.startsWith("_")) {
            return getMissingSprite();
        }

        TextureAtlasSprite cached = spriteCache.get(textureName);
        if (cached != null) return cached;

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

        // 4. Try without block/ prefix
        if (sprite == null) {
            sprite = tryLookupInNamespace(BTWForgeMod.MOD_ID, textureName);
        }
        if (sprite == null) {
            sprite = tryLookupInNamespace("minecraft", textureName);
        }

        if (sprite == null) {
            LOGGER.debug("BTW: Texture not found: '{}' (mapped: {})",
                    textureName, VANILLA_TEXTURE_MAP.get(textureName));
            sprite = getMissingSprite();
        }

        spriteCache.put(textureName, sprite);
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

    public static void clearSpriteCache() {
        spriteCache.clear();
    }
}
