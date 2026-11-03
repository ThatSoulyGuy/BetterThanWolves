package btw.modern;

public class RenderBlocks {

    /** The IBlockAccess used by this instance of RenderBlocks */
    public IBlockAccess blockAccess;

    /** If set to >=0, all block faces will be rendered using this texture index */
    private Icon overrideBlockTexture = null;

    private boolean flipTexture = false;

    /** If true, renders all faces on all blocks rather than using shouldSideBeRendered logic. */
    private boolean renderAllFaces = false;

    /** Fancy grass side matching biome */
    public static boolean fancyGrass = true;
    public boolean useInventoryTint = true;

    private double renderMinX;
    private double renderMaxX;
    private double renderMinY;
    private double renderMaxY;
    private double renderMinZ;
    private double renderMaxZ;

    private boolean lockBlockBounds = false;
    private boolean partialRenderBounds = false;

    private int uvRotateEast = 0;
    private int uvRotateWest = 0;
    private int uvRotateSouth = 0;
    private int uvRotateNorth = 0;
    private int uvRotateTop = 0;
    private int uvRotateBottom = 0;

    private boolean enableAO;

    public RenderBlocks(IBlockAccess blockAccess) {
        this.blockAccess = blockAccess;
    }

    public RenderBlocks() {}

    // --- Override block texture ---

    public void setOverrideBlockTexture(Icon icon) {
        this.overrideBlockTexture = icon;
    }

    public void clearOverrideBlockTexture() {
        this.overrideBlockTexture = null;
    }

    public boolean hasOverrideBlockTexture() {
        return this.overrideBlockTexture != null;
    }

    public Icon GetOverrideTexture() {
        return this.overrideBlockTexture;
    }

    // --- Render bounds ---

    public void setRenderBounds(AxisAlignedBB bounds) {
        if (bounds != null) {
            setRenderBounds(bounds.minX, bounds.minY, bounds.minZ, bounds.maxX, bounds.maxY, bounds.maxZ);
        }
    }

    public void setRenderBounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (!this.lockBlockBounds) {
            this.renderMinX = minX;
            this.renderMinY = minY;
            this.renderMinZ = minZ;
            this.renderMaxX = maxX;
            this.renderMaxY = maxY;
            this.renderMaxZ = maxZ;
            this.partialRenderBounds = this.renderMinX > 0.0D || this.renderMinY > 0.0D || this.renderMinZ > 0.0D
                    || this.renderMaxX < 1.0D || this.renderMaxY < 1.0D || this.renderMaxZ < 1.0D;
        }
    }

    public void setRenderBoundsFromBlock(Block block) {
        this.setRenderBounds(
            block.getBlockBoundsMinX(), block.getBlockBoundsMinY(), block.getBlockBoundsMinZ(),
            block.getBlockBoundsMaxX(), block.getBlockBoundsMaxY(), block.getBlockBoundsMaxZ()
        );
    }

    public void overrideBlockBounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.renderMinX = minX;
        this.renderMinY = minY;
        this.renderMinZ = minZ;
        this.renderMaxX = maxX;
        this.renderMaxY = maxY;
        this.renderMaxZ = maxZ;
        this.lockBlockBounds = true;
        this.partialRenderBounds = this.renderMinX > 0.0D || this.renderMinY > 0.0D || this.renderMinZ > 0.0D
                || this.renderMaxX < 1.0D || this.renderMaxY < 1.0D || this.renderMaxZ < 1.0D;
    }

    public void unlockBlockBounds() {
        this.lockBlockBounds = false;
    }

    // --- UV rotation ---

    public void SetUvRotateEast(int value) { this.uvRotateEast = value; }
    public void SetUvRotateWest(int value) { this.uvRotateWest = value; }
    public void SetUvRotateSouth(int value) { this.uvRotateSouth = value; }
    public void SetUvRotateNorth(int value) { this.uvRotateNorth = value; }
    public void SetUvRotateTop(int value) { this.uvRotateTop = value; }
    public void SetUvRotateBottom(int value) { this.uvRotateBottom = value; }

    public void ClearUvRotation() {
        this.uvRotateEast = 0;
        this.uvRotateWest = 0;
        this.uvRotateSouth = 0;
        this.uvRotateNorth = 0;
        this.uvRotateTop = 0;
        this.uvRotateBottom = 0;
    }

    // --- RenderAllFaces ---

    public boolean GetRenderAllFaces() { return this.renderAllFaces; }
    public void SetRenderAllFaces(boolean value) { this.renderAllFaces = value; }

    // --- Block rendering by render type ---

    /**
     * 1.5.2 renderBlockUsingTexture (RenderBlocks.java:392-397): the block's own render with every face
     * drawn in the given icon. Port: the override is cleared in a finally block, and an exception from
     * FC still reaches the caller (as in 1.5.2), whose capture records the failure.
     */
    public void renderBlockUsingTexture(Block block, int x, int y, int z, Icon icon) {
        this.setOverrideBlockTexture(icon);
        try {
            this.renderBlockThroughFC(block, x, y, z);
        } finally {
            this.clearOverrideBlockTexture();
        }
    }

    /**
     * 1.5.2 renderBlockAllFaces (RenderBlocks.java:402-407), which the default
     * Block.RenderBlockMovedByPiston draws a piston-moved block with (vanilla/client Block.java:3764-3767):
     * the block's own render with the shouldSideBeRendered culling switched off. The flag is reset in
     * a finally block, as the override is in renderBlockUsingTexture.
     */
    public void renderBlockAllFaces(Block block, int x, int y, int z) {
        this.renderAllFaces = true;
        try {
            this.renderBlockThroughFC(block, x, y, z);
        } finally {
            this.renderAllFaces = false;
        }
    }

    /**
     * 1.5.2 (FCMOD) renderBlockByRenderType (RenderBlocks.java:9442-9451), the dispatch both methods
     * above go through. The port's renderBlockByRenderType below is the vanilla render-type switch that
     * btw.modern.Block.RenderBlock falls back to, so calling it from them would skip FC's RenderBlock
     * overrides and the second pass.
     */
    private boolean renderBlockThroughFC(Block block, int x, int y, int z) {
        block.m_currentBlockRenderer = this;
        boolean firstPass = block.RenderBlock(this, x, y, z);
        block.RenderBlockSecondPass(this, x, y, z, firstPass);
        return firstPass;
    }

    public boolean renderBlockByRenderType(Block block, int x, int y, int z) {
        int renderType = block.getRenderType();
        switch (renderType) {
            case 0: return renderStandardBlock(block, x, y, z);
            case 1: return renderCrossedSquares(block, x, y, z);
            case 2: return renderBlockTorch(block, x, y, z);
            case 4: return renderBlockFluids(block, x, y, z);
            case 6: return renderBlockCrops(block, x, y, z);
            case 8: return renderBlockLadder(block, x, y, z);
            // 1.5.2 render type 9 — btw.modern.BlockRailBase.getRenderType(); live blocks are
            // FCBlockDetectorRail (fcDetectorRailWood 235 / fcBlockDetectorRailSoulforgedSteel 236)
            case 9: return renderBlockMinecartTrack((BlockRailBase) block, x, y, z);
            // 1.5.2 render types 13 / 23 (RenderBlocks.java:426) — btw.modern.BlockCactus / BlockLilyPad
            // .getRenderType(); FCBlockCactus and FCBlockLilyPad override RenderBlock and call these themselves,
            // so this only serves a subclass without that override
            case 13: return renderBlockCactus(block, x, y, z);
            // 1.5.2 render type 19 — btw.modern.BlockStem.getRenderType(); FCBlockStem overrides RenderBlock and
            // calls renderBlockStem itself, so this only serves a stem without that override
            case 19: return renderBlockStem(block, x, y, z);
            case 20: return renderBlockVine(block, x, y, z);
            case 23: return renderBlockLilyPad(block, x, y, z);
            default:
                // For any unhandled render type, use standard block rendering
                // with the block's own bounds (slabs, buttons, pressure plates, etc.
                // set their bounds in constructor/setBlockBoundsBasedOnState)
                return renderStandardBlock(block, x, y, z);
        }
    }

    // --- Standard block rendering ---

    /**
     * 1.5.2 RenderBlocks.renderStandardBlock (RenderBlocks.java:4270-4292): the block's colorMultiplier
     * tint, then the AO or colour-multiplier variant. The capture has no per-vertex AO (MC relights
     * the baked quads), so it always takes renderStandardBlockWithColorMultiplier. Like 1.5.2 it does
     * not set the render bounds: the caller (Block.RenderBlock, FCBlockAxle.RenderBlock, ...) has.
     */
    public boolean renderStandardBlock(Block block, int x, int y, int z) {
        int color = this.colorMultiplierForFaces(block, x, y, z);
        float r = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;
        return this.renderStandardBlockWithColorMultiplier(block, x, y, z, r, g, b);
    }

    /**
     * 1.5.2 renderStandardBlockWithAmbientOcclusion (RenderBlocks.java:4352-5161): the same
     * renderAllFaces / shouldSideBeRendered gate and getBlockIcon faces as the colour-multiplier
     * variant; the capture discards per-vertex AO, so it draws through that variant. 1.5.2 drops the
     * tint while an override texture is set (:4373-4377).
     */
    public boolean renderStandardBlockWithAmbientOcclusion(Block block, int x, int y, int z, float r, float g, float b) {
        boolean tint = !this.hasOverrideBlockTexture();
        return this.renderStandardBlockWithColorMultiplier(block, x, y, z, tint ? r : 1.0F, tint ? g : 1.0F, tint ? b : 1.0F);
    }

    /** 1.5.2 func_102027_b (RenderBlocks.java:5163-6012), the partial-bounds AO variant; see renderStandardBlockWithAmbientOcclusion. */
    public boolean func_102027_b(Block block, int x, int y, int z, float r, float g, float b) {
        return this.renderStandardBlockWithAmbientOcclusion(block, x, y, z, r, g, b);
    }

    /**
     * 1.5.2 renderStandardBlockWithColorMultiplier (RenderBlocks.java:6047-6187): each face is drawn
     * when renderAllFaces or block.shouldSideBeRendered(neighbour, side) allows it, with the
     * world-aware icon block.getBlockTexture(blockAccess, x, y, z, side) (getBlockIcon, :8377-8380).
     * That is where FC puts facing- and neighbour-dependent faces (gear box / clutch input and output,
     * axle axis and powered textures, bellows / saw / screw pump fronts), and FCBakedModel's capture
     * sees those neighbour reads and captures such metas live.
     *
     * <p>Capture adaptations: the colour is the tint alone; 1.5.2's face shades (0.5 bottom, 1.0 top,
     * 0.8 z sides, 0.6 x sides) and setBrightness are left out because MC shades and lights the baked
     * quads itself (ModelBlockRenderer multiplies the vertex colour by its directional shade). With
     * no blockAccess (a draw outside the world) every face is drawn with getIcon(side, 0) in the
     * caller's colour.
     */
    public boolean renderStandardBlockWithColorMultiplier(Block block, int x, int y, int z, float r, float g, float b) {
        this.enableAO = false;
        Tessellator t = Tessellator.instance;
        boolean rendered = false;

        if (this.renderAllFaces || this.sideVisible(block, x, y - 1, z, 0)) {
            this.setFaceColor(t, r, g, b);
            this.renderFaceYNeg(block, (double)x, (double)y, (double)z, this.standardFaceIcon(block, x, y, z, 0));
            rendered = true;
        }

        if (this.renderAllFaces || this.sideVisible(block, x, y + 1, z, 1)) {
            this.setFaceColor(t, r, g, b);
            this.renderFaceYPos(block, (double)x, (double)y, (double)z, this.standardFaceIcon(block, x, y, z, 1));
            rendered = true;
        }

        if (this.renderAllFaces || this.sideVisible(block, x, y, z - 1, 2)) {
            this.setFaceColor(t, r, g, b);
            this.renderFaceZNeg(block, (double)x, (double)y, (double)z, this.standardFaceIcon(block, x, y, z, 2));
            rendered = true;
        }

        if (this.renderAllFaces || this.sideVisible(block, x, y, z + 1, 3)) {
            this.setFaceColor(t, r, g, b);
            this.renderFaceZPos(block, (double)x, (double)y, (double)z, this.standardFaceIcon(block, x, y, z, 3));
            rendered = true;
        }

        if (this.renderAllFaces || this.sideVisible(block, x - 1, y, z, 4)) {
            this.setFaceColor(t, r, g, b);
            this.renderFaceXNeg(block, (double)x, (double)y, (double)z, this.standardFaceIcon(block, x, y, z, 4));
            rendered = true;
        }

        if (this.renderAllFaces || this.sideVisible(block, x + 1, y, z, 5)) {
            this.setFaceColor(t, r, g, b);
            this.renderFaceXPos(block, (double)x, (double)y, (double)z, this.standardFaceIcon(block, x, y, z, 5));
            rendered = true;
        }

        return rendered;
    }

    // --- FC additions: standard full block rendering ---

    /**
     * 1.5.2 (FCMOD) RenderStandardFullBlock (RenderBlocks.java:9461-9478): full-cube bounds written
     * straight to the fields (the bounds lock is not consulted), then the AO or colour-multiplier
     * variant; the capture has no AO, so always the latter.
     */
    public boolean RenderStandardFullBlock(Block block, int x, int y, int z) {
        this.renderMinX = this.renderMinY = this.renderMinZ = 0.0D;
        this.renderMaxX = this.renderMaxY = this.renderMaxZ = 1.0D;
        this.partialRenderBounds = false;
        return this.RenderStandardFullBlockWithColorMultiplier(block, x, y, z);
    }

    /** 1.5.2 RenderStandardFullBlockWithAmbientOcclusion (RenderBlocks.java:9480-10101): same gate and icons; drawn without AO. */
    public boolean RenderStandardFullBlockWithAmbientOcclusion(Block block, int x, int y, int z) {
        return this.RenderStandardFullBlockWithColorMultiplier(block, x, y, z);
    }

    /**
     * 1.5.2 RenderStandardFullBlockWithColorMultiplier (RenderBlocks.java:10103-10175): faces gated by
     * block.shouldSideBeRendered alone (FC strips the renderAllFaces test here, see
     * RenderStandardFullBlockMovedByPiston), world-aware getBlockIcon faces, drawn through the
     * RenderFull*Face methods. The face shades are left out as in renderStandardBlockWithColorMultiplier.
     */
    public boolean RenderStandardFullBlockWithColorMultiplier(Block block, int x, int y, int z) {
        this.enableAO = false;
        Tessellator t = Tessellator.instance;
        boolean rendered = false;

        if (this.sideVisible(block, x, y - 1, z, 0)) {
            this.setFaceColor(t, 1.0F, 1.0F, 1.0F);
            this.RenderFullBottomFace(block, (double)x, (double)y, (double)z, this.standardFaceIcon(block, x, y, z, 0));
            rendered = true;
        }

        if (this.sideVisible(block, x, y + 1, z, 1)) {
            this.setFaceColor(t, 1.0F, 1.0F, 1.0F);
            this.RenderFullTopFace(block, (double)x, (double)y, (double)z, this.standardFaceIcon(block, x, y, z, 1));
            rendered = true;
        }

        if (this.sideVisible(block, x, y, z - 1, 2)) {
            this.setFaceColor(t, 1.0F, 1.0F, 1.0F);
            this.RenderFullEastFace(block, (double)x, (double)y, (double)z, this.standardFaceIcon(block, x, y, z, 2));
            rendered = true;
        }

        if (this.sideVisible(block, x, y, z + 1, 3)) {
            this.setFaceColor(t, 1.0F, 1.0F, 1.0F);
            this.RenderFullWestFace(block, (double)x, (double)y, (double)z, this.standardFaceIcon(block, x, y, z, 3));
            rendered = true;
        }

        if (this.sideVisible(block, x - 1, y, z, 4)) {
            this.setFaceColor(t, 1.0F, 1.0F, 1.0F);
            this.RenderFullNorthFace(block, (double)x, (double)y, (double)z, this.standardFaceIcon(block, x, y, z, 4));
            rendered = true;
        }

        if (this.sideVisible(block, x + 1, y, z, 5)) {
            this.setFaceColor(t, 1.0F, 1.0F, 1.0F);
            this.RenderFullSouthFace(block, (double)x, (double)y, (double)z, this.standardFaceIcon(block, x, y, z, 5));
            rendered = true;
        }

        return rendered;
    }

    // --- Standard-block helpers (FC calls guarded so a throwing override cannot drop the capture) ---

    /** "Class#method" for each FC override (colorMultiplier, getBlockTexture, shouldSideBeRendered, ...) that threw here; logged once. */
    private static final java.util.Set<String> FAILED_FACE_CALLS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static void logFaceCallFailure(Block block, String method, Throwable t) {
        if (FAILED_FACE_CALLS.add(block.getClass().getName() + "#" + method)) {
            LOGGER.warn("{}.{} threw while drawing a standard block; using the fallback for it: {}",
                    block.getClass().getSimpleName(), method, t.toString());
        }
    }

    /**
     * The colour 1.5.2 sets before each standard-block face (Tessellator.setColorOpaque_F). Without a
     * blockAccess it is left alone, so such a draw keeps the colour its caller set, as the port drew
     * these faces before.
     */
    private void setFaceColor(Tessellator t, float r, float g, float b) {
        if (this.blockAccess != null) {
            t.setColorOpaque_F(r, g, b);
        }
    }

    /** block.colorMultiplier(blockAccess, x, y, z) (1.5.2 :4272); white without a blockAccess or if it throws. */
    private int colorMultiplierForFaces(Block block, int x, int y, int z) {
        if (this.blockAccess == null) {
            return 0xFFFFFF;
        }
        try {
            return block.colorMultiplier(this.blockAccess, x, y, z);
        } catch (Throwable t) {
            logFaceCallFailure(block, "colorMultiplier", t);
            return 0xFFFFFF;
        }
    }

    /**
     * block.shouldSideBeRendered(blockAccess, neighbour, side) for the standard-block gates. Without a
     * blockAccess, or if the override throws (FC overrides dereference m_currentBlockRenderer), the
     * face is drawn, as the port did before the gate existed.
     */
    private boolean sideVisible(Block block, int nx, int ny, int nz, int side) {
        if (this.blockAccess == null) {
            return true;
        }
        try {
            return block.shouldSideBeRendered(this.blockAccess, nx, ny, nz, side);
        } catch (Throwable t) {
            logFaceCallFailure(block, "shouldSideBeRendered", t);
            return true;
        }
    }

    /**
     * The icon 1.5.2's standard-block routines draw a face with: getBlockIcon(block, blockAccess, x, y,
     * z, side), i.e. the world-aware block.getBlockTexture. getIcon(side, meta) is FC's item-render API
     * and is used only without a blockAccess (meta 0) or when getBlockTexture throws.
     */
    private Icon standardFaceIcon(Block block, int x, int y, int z, int side) {
        if (this.blockAccess == null) {
            return this.getBlockIconFromSideAndMetadata(block, side, 0);
        }
        try {
            return this.getBlockIcon(block, this.blockAccess, x, y, z, side);
        } catch (Throwable t) {
            logFaceCallFailure(block, "getBlockTexture", t);
            return this.getBlockIconFromSideAndMetadata(block, side, this.blockAccess.getBlockMetadata(x, y, z));
        }
    }

    // --- Grass block rendering ---

    // 1.5.2 RenderBlocks.renderGrassBlockWithAmbientOcclusion — FCBlockDirtSlab.RenderBlock:507 /
    // FCBlockGrass.RenderBlock take this branch when Minecraft.isAmbientOcclusionEnabled(); the
    // capture pipeline discards per-vertex AO (the modern engine relights baked quads), so the AO
    // variant delegates to the ColorMultiplier port to emit identical geometry.
    public boolean renderGrassBlockWithAmbientOcclusion(Block block, int x, int y, int z, float r, float g, float b, Icon sideOverlayIcon) {
        return renderGrassBlockWithColorMultiplier(block, x, y, z, r, g, b, sideOverlayIcon);
    }

    // 1.5.2 RenderBlocks.renderGrassBlockWithColorMultiplier — FCBlockDirtSlab.RenderBlock:511
    // (grass subtype of dirt slab, legacy ID 206): tinted top face, untinted base side faces,
    // then a tinted second pass over the sides with the grass side-overlay icon.
    public boolean renderGrassBlockWithColorMultiplier(Block block, int x, int y, int z, float r, float g, float b, Icon sideOverlayIcon) {
        this.enableAO = false;
        Tessellator t = Tessellator.instance;
        boolean rendered = false;
        float brightnessBottom = 0.5F;
        float brightnessTop = 1.0F;
        float brightnessZSides = 0.8F;
        float brightnessXSides = 0.6F;
        float topR = brightnessTop * r;
        float topG = brightnessTop * g;
        float topB = brightnessTop * b;

        int brightness = block.getMixedBrightnessForBlock(this.blockAccess, x, y, z);

        if (block.shouldSideBeRendered(this.blockAccess, x, y - 1, z, 0)) {
            t.setBrightness(this.renderMinY > 0.0D ? brightness : block.getMixedBrightnessForBlock(this.blockAccess, x, y - 1, z));
            t.setColorOpaque_F(brightnessBottom, brightnessBottom, brightnessBottom);
            this.renderFaceYNeg(block, (double)x, (double)y, (double)z, this.getBlockIcon(block, this.blockAccess, x, y, z, 0));
            rendered = true;
        }

        if (block.shouldSideBeRendered(this.blockAccess, x, y + 1, z, 1)) {
            t.setBrightness(this.renderMaxY < 1.0D ? brightness : block.getMixedBrightnessForBlock(this.blockAccess, x, y + 1, z));
            t.setColorOpaque_F(topR, topG, topB);
            this.renderFaceYPos(block, (double)x, (double)y, (double)z, this.getBlockIcon(block, this.blockAccess, x, y, z, 1));
            rendered = true;
        }

        Icon sideIcon;

        if (block.shouldSideBeRendered(this.blockAccess, x, y, z - 1, 2)) {
            t.setBrightness(this.renderMinZ > 0.0D ? brightness : block.getMixedBrightnessForBlock(this.blockAccess, x, y, z - 1));
            t.setColorOpaque_F(brightnessZSides, brightnessZSides, brightnessZSides);
            sideIcon = this.getBlockIcon(block, this.blockAccess, x, y, z, 2);
            this.renderFaceZNeg(block, (double)x, (double)y, (double)z, sideIcon);

            t.setColorOpaque_F(brightnessZSides * r, brightnessZSides * g, brightnessZSides * b);
            this.renderFaceZNeg(block, (double)x, (double)y, (double)z, sideOverlayIcon);

            rendered = true;
        }

        if (block.shouldSideBeRendered(this.blockAccess, x, y, z + 1, 3)) {
            t.setBrightness(this.renderMaxZ < 1.0D ? brightness : block.getMixedBrightnessForBlock(this.blockAccess, x, y, z + 1));
            t.setColorOpaque_F(brightnessZSides, brightnessZSides, brightnessZSides);
            sideIcon = this.getBlockIcon(block, this.blockAccess, x, y, z, 3);
            this.renderFaceZPos(block, (double)x, (double)y, (double)z, sideIcon);

            t.setColorOpaque_F(brightnessZSides * r, brightnessZSides * g, brightnessZSides * b);
            this.renderFaceZPos(block, (double)x, (double)y, (double)z, sideOverlayIcon);

            rendered = true;
        }

        if (block.shouldSideBeRendered(this.blockAccess, x - 1, y, z, 4)) {
            t.setBrightness(this.renderMinX > 0.0D ? brightness : block.getMixedBrightnessForBlock(this.blockAccess, x - 1, y, z));
            t.setColorOpaque_F(brightnessXSides, brightnessXSides, brightnessXSides);
            sideIcon = this.getBlockIcon(block, this.blockAccess, x, y, z, 4);
            this.renderFaceXNeg(block, (double)x, (double)y, (double)z, sideIcon);

            t.setColorOpaque_F(brightnessXSides * r, brightnessXSides * g, brightnessXSides * b);
            this.renderFaceXNeg(block, (double)x, (double)y, (double)z, sideOverlayIcon);

            rendered = true;
        }

        if (block.shouldSideBeRendered(this.blockAccess, x + 1, y, z, 5)) {
            t.setBrightness(this.renderMaxX < 1.0D ? brightness : block.getMixedBrightnessForBlock(this.blockAccess, x + 1, y, z));
            t.setColorOpaque_F(brightnessXSides, brightnessXSides, brightnessXSides);
            sideIcon = this.getBlockIcon(block, this.blockAccess, x, y, z, 5);
            this.renderFaceXPos(block, (double)x, (double)y, (double)z, sideIcon);

            t.setColorOpaque_F(brightnessXSides * r, brightnessXSides * g, brightnessXSides * b);
            this.renderFaceXPos(block, (double)x, (double)y, (double)z, sideOverlayIcon);

            rendered = true;
        }

        return rendered;
    }

    // ================================================================
    // Specific block type renders - ported from vanilla 1.5.2
    // ================================================================

    // --- renderBlockDoor ---
    // Doors render as a thin slab oriented based on metadata.
    // The block sets its bounds in setBlockBoundsBasedOnState,
    // and the rendering uses face methods with those bounds.
    public boolean renderBlockDoor(Block block, int x, int y, int z) {
        Tessellator t = Tessellator.instance;
        int meta = this.blockAccess.getBlockMetadata(x, y, z);

        if ((meta & 8) != 0) {
            if (this.blockAccess.getBlockId(x, y - 1, z) != block.blockID) {
                return false;
            }
        } else if (this.blockAccess.getBlockId(x, y + 1, z) != block.blockID) {
            return false;
        }

        boolean rendered = false;
        float brightnessBottom = 0.5F;
        float brightnessTop = 1.0F;
        float brightnessZSides = 0.8F;
        float brightnessXSides = 0.6F;
        int brightness = block.getMixedBrightnessForBlock(this.blockAccess, x, y, z);

        t.setBrightness(this.renderMinY > 0.0D ? brightness : block.getMixedBrightnessForBlock(this.blockAccess, x, y - 1, z));
        t.setColorOpaque_F(brightnessBottom, brightnessBottom, brightnessBottom);
        this.renderFaceYNeg(block, (double)x, (double)y, (double)z, this.getBlockIcon(block, this.blockAccess, x, y, z, 0));
        rendered = true;

        t.setBrightness(this.renderMaxY < 1.0D ? brightness : block.getMixedBrightnessForBlock(this.blockAccess, x, y + 1, z));
        t.setColorOpaque_F(brightnessTop, brightnessTop, brightnessTop);
        this.renderFaceYPos(block, (double)x, (double)y, (double)z, this.getBlockIcon(block, this.blockAccess, x, y, z, 1));
        rendered = true;

        t.setBrightness(this.renderMinZ > 0.0D ? brightness : block.getMixedBrightnessForBlock(this.blockAccess, x, y, z - 1));
        t.setColorOpaque_F(brightnessZSides, brightnessZSides, brightnessZSides);
        Icon doorIcon = this.getBlockIcon(block, this.blockAccess, x, y, z, 2);
        this.renderFaceZNeg(block, (double)x, (double)y, (double)z, doorIcon);
        rendered = true;
        this.flipTexture = false;

        t.setBrightness(this.renderMaxZ < 1.0D ? brightness : block.getMixedBrightnessForBlock(this.blockAccess, x, y, z + 1));
        t.setColorOpaque_F(brightnessZSides, brightnessZSides, brightnessZSides);
        doorIcon = this.getBlockIcon(block, this.blockAccess, x, y, z, 3);
        this.renderFaceZPos(block, (double)x, (double)y, (double)z, doorIcon);
        rendered = true;
        this.flipTexture = false;

        t.setBrightness(this.renderMinX > 0.0D ? brightness : block.getMixedBrightnessForBlock(this.blockAccess, x - 1, y, z));
        t.setColorOpaque_F(brightnessXSides, brightnessXSides, brightnessXSides);
        doorIcon = this.getBlockIcon(block, this.blockAccess, x, y, z, 4);
        this.renderFaceXNeg(block, (double)x, (double)y, (double)z, doorIcon);
        rendered = true;
        this.flipTexture = false;

        t.setBrightness(this.renderMaxX < 1.0D ? brightness : block.getMixedBrightnessForBlock(this.blockAccess, x + 1, y, z));
        t.setColorOpaque_F(brightnessXSides, brightnessXSides, brightnessXSides);
        doorIcon = this.getBlockIcon(block, this.blockAccess, x, y, z, 5);
        this.renderFaceXPos(block, (double)x, (double)y, (double)z, doorIcon);
        rendered = true;
        this.flipTexture = false;

        return rendered;
    }

    // --- renderBlockFluids ---
    // Water/lava rendering with corner height interpolation and side faces.
    private float getFluidHeight(int x, int y, int z, Material material) {
        int count = 0;
        float totalHeight = 0.0F;

        for (int corner = 0; corner < 4; ++corner) {
            int bx = x - (corner & 1);
            int bz = z - (corner >> 1 & 1);

            if (this.blockAccess.getBlockMaterial(bx, y + 1, bz) == material) {
                return 1.0F;
            }

            Material blockMaterial = this.blockAccess.getBlockMaterial(bx, y, bz);

            if (blockMaterial == material) {
                int meta = this.blockAccess.getBlockMetadata(bx, y, bz);

                if (meta >= 8 || meta == 0) {
                    totalHeight += BlockFluid.getFluidHeightPercent(meta) * 10.0F;
                    count += 10;
                }

                totalHeight += BlockFluid.getFluidHeightPercent(meta);
                ++count;
            } else if (!blockMaterial.isSolid()) {
                ++totalHeight;
                ++count;
            }
        }

        return 1.0F - totalHeight / (float)count;
    }

    public boolean renderBlockFluids(Block block, int x, int y, int z) {
        Tessellator t = Tessellator.instance;
        int colorMult = block.colorMultiplier(this.blockAccess, x, y, z);
        float cr = (float)(colorMult >> 16 & 255) / 255.0F;
        float cg = (float)(colorMult >> 8 & 255) / 255.0F;
        float cb = (float)(colorMult & 255) / 255.0F;

        boolean renderTop = block.shouldSideBeRendered(this.blockAccess, x, y + 1, z, 1);
        boolean renderBottom = block.shouldSideBeRendered(this.blockAccess, x, y - 1, z, 0);
        boolean[] renderSide = new boolean[] {
            block.shouldSideBeRendered(this.blockAccess, x, y, z - 1, 2),
            block.shouldSideBeRendered(this.blockAccess, x, y, z + 1, 3),
            block.shouldSideBeRendered(this.blockAccess, x - 1, y, z, 4),
            block.shouldSideBeRendered(this.blockAccess, x + 1, y, z, 5)
        };

        if (!renderTop && !renderBottom && !renderSide[0] && !renderSide[1] && !renderSide[2] && !renderSide[3]) {
            return false;
        }

        boolean rendered = false;
        float brightnessBottom = 0.5F;
        float brightnessTop = 1.0F;
        float brightnessZ = 0.8F;
        float brightnessX = 0.6F;
        Material mat = block.blockMaterial;
        int meta = this.blockAccess.getBlockMetadata(x, y, z);
        double heightSW = (double)this.getFluidHeight(x, y, z, mat);
        double heightNW = (double)this.getFluidHeight(x, y, z + 1, mat);
        double heightNE = (double)this.getFluidHeight(x + 1, y, z + 1, mat);
        double heightSE = (double)this.getFluidHeight(x + 1, y, z, mat);
        double eps = 0.0010000000474974513D;

        if (this.renderAllFaces || renderTop) {
            rendered = true;
            Icon topIcon = this.getBlockIconFromSideAndMetadata(block, 1, meta);
            t.setCurrentTextureName(topIcon != null ? topIcon.getIconName() : null);

            float flowDir = (float)BlockFluid.getFlowDirection(this.blockAccess, x, y, z, mat);

            double topH_SW = heightSW - eps;
            double topH_NW = heightNW - eps;
            double topH_NE = heightNE - eps;
            double topH_SE = heightSE - eps;

            double u0, u1, u2, u3, v0, v1, v2, v3;

            if (flowDir < -999.0F) {
                // Still water - no flow rotation
                u0 = (double)topIcon.getInterpolatedU(0.0D);
                v0 = (double)topIcon.getInterpolatedV(0.0D);
                u1 = u0;
                v1 = (double)topIcon.getInterpolatedV(16.0D);
                u2 = (double)topIcon.getInterpolatedU(16.0D);
                v2 = v1;
                u3 = u2;
                v3 = v0;
            } else {
                float sinFlow = MathHelper.sin(flowDir) * 0.25F;
                float cosFlow = MathHelper.cos(flowDir) * 0.25F;
                u0 = (double)topIcon.getInterpolatedU((double)(8.0F + (-cosFlow - sinFlow) * 16.0F));
                v0 = (double)topIcon.getInterpolatedV((double)(8.0F + (-cosFlow + sinFlow) * 16.0F));
                u1 = (double)topIcon.getInterpolatedU((double)(8.0F + (-cosFlow + sinFlow) * 16.0F));
                v1 = (double)topIcon.getInterpolatedV((double)(8.0F + (cosFlow + sinFlow) * 16.0F));
                u2 = (double)topIcon.getInterpolatedU((double)(8.0F + (cosFlow + sinFlow) * 16.0F));
                v2 = (double)topIcon.getInterpolatedV((double)(8.0F + (cosFlow - sinFlow) * 16.0F));
                u3 = (double)topIcon.getInterpolatedU((double)(8.0F + (cosFlow - sinFlow) * 16.0F));
                v3 = (double)topIcon.getInterpolatedV((double)(8.0F + (-cosFlow - sinFlow) * 16.0F));
            }

            t.setBrightness(block.getMixedBrightnessForBlock(this.blockAccess, x, y, z));
            t.setColorOpaque_F(brightnessTop * cr, brightnessTop * cg, brightnessTop * cb);
            t.addVertexWithUV((double)(x + 0), (double)y + topH_SW, (double)(z + 0), u0, v0);
            t.addVertexWithUV((double)(x + 0), (double)y + topH_NW, (double)(z + 1), u1, v1);
            t.addVertexWithUV((double)(x + 1), (double)y + topH_NE, (double)(z + 1), u2, v2);
            t.addVertexWithUV((double)(x + 1), (double)y + topH_SE, (double)(z + 0), u3, v3);
        }

        if (this.renderAllFaces || renderBottom) {
            t.setBrightness(block.getMixedBrightnessForBlock(this.blockAccess, x, y - 1, z));
            float bMult = 1.0F;
            t.setColorOpaque_F(brightnessBottom * bMult, brightnessBottom * bMult, brightnessBottom * bMult);
            this.renderFaceYNeg(block, (double)x, (double)y + eps, (double)z, this.getBlockIconFromSide(block, 0));
            rendered = true;
        }

        for (int side = 0; side < 4; ++side) {
            int nx = x;
            int nz = z;

            if (side == 0) { nz = z - 1; }
            if (side == 1) { ++nz; }
            if (side == 2) { nx = x - 1; }
            if (side == 3) { ++nx; }

            Icon sideIcon = this.getBlockIconFromSideAndMetadata(block, side + 2, meta);

            if (this.renderAllFaces || renderSide[side]) {
                double sideH1, sideH2, sx1, sx2, sz1, sz2;

                if (side == 0) {
                    sideH1 = heightSW;
                    sideH2 = heightSE;
                    sx1 = (double)x;
                    sx2 = (double)(x + 1);
                    sz1 = (double)z + eps;
                    sz2 = (double)z + eps;
                } else if (side == 1) {
                    sideH1 = heightNE;
                    sideH2 = heightNW;
                    sx1 = (double)(x + 1);
                    sx2 = (double)x;
                    sz1 = (double)(z + 1) - eps;
                    sz2 = (double)(z + 1) - eps;
                } else if (side == 2) {
                    sideH1 = heightNW;
                    sideH2 = heightSW;
                    sx1 = (double)x + eps;
                    sx2 = (double)x + eps;
                    sz1 = (double)(z + 1);
                    sz2 = (double)z;
                } else {
                    sideH1 = heightSE;
                    sideH2 = heightNE;
                    sx1 = (double)(x + 1) - eps;
                    sx2 = (double)(x + 1) - eps;
                    sz1 = (double)z;
                    sz2 = (double)(z + 1);
                }

                rendered = true;
                float su0 = sideIcon.getInterpolatedU(0.0D);
                float su1 = sideIcon.getInterpolatedU(8.0D);
                float sv0 = sideIcon.getInterpolatedV((1.0D - sideH1) * 16.0D * 0.5D);
                float sv1 = sideIcon.getInterpolatedV((1.0D - sideH2) * 16.0D * 0.5D);
                float sv2 = sideIcon.getInterpolatedV(8.0D);
                t.setBrightness(block.getMixedBrightnessForBlock(this.blockAccess, nx, y, nz));
                float sideBrightness = 1.0F;

                if (side < 2) {
                    sideBrightness *= brightnessZ;
                } else {
                    sideBrightness *= brightnessX;
                }

                t.setColorOpaque_F(brightnessTop * sideBrightness * cr, brightnessTop * sideBrightness * cg, brightnessTop * sideBrightness * cb);
                t.addVertexWithUV(sx1, (double)y + sideH1, sz1, (double)su0, (double)sv0);
                t.addVertexWithUV(sx2, (double)y + sideH2, sz2, (double)su1, (double)sv1);
                t.addVertexWithUV(sx2, (double)(y + 0), sz2, (double)su1, (double)sv2);
                t.addVertexWithUV(sx1, (double)(y + 0), sz1, (double)su0, (double)sv2);
            }
        }

        this.renderMinY = 0.0D;
        this.renderMaxY = 1.0D;
        return rendered;
    }

    // --- renderBlockVine ---
    // Flat planes on block faces where vine attaches, based on metadata bit flags.
    public boolean renderBlockVine(Block block, int x, int y, int z) {
        Tessellator t = Tessellator.instance;
        Icon icon = this.getBlockIconFromSide(block, 0);

        if (this.hasOverrideBlockTexture()) {
            icon = this.overrideBlockTexture;
        }

        t.setCurrentTextureName(icon != null ? icon.getIconName() : null);
        t.setBrightness(block.getMixedBrightnessForBlock(this.blockAccess, x, y, z));
        int colorMult = block.colorMultiplier(this.blockAccess, x, y, z);
        float cr = (float)(colorMult >> 16 & 255) / 255.0F;
        float cg = (float)(colorMult >> 8 & 255) / 255.0F;
        float cb = (float)(colorMult & 255) / 255.0F;
        t.setColorOpaque_F(cr, cg, cb);

        double minU = icon != null ? (double)icon.getMinU() : 0;
        double minV = icon != null ? (double)icon.getMinV() : 0;
        double maxU = icon != null ? (double)icon.getMaxU() : 1;
        double maxV = icon != null ? (double)icon.getMaxV() : 1;
        double offset = 0.05000000074505806D;
        int meta = this.blockAccess.getBlockMetadata(x, y, z);

        // bit 2 = west face (x+)
        if ((meta & 2) != 0) {
            t.addVertexWithUV((double)x + offset, (double)(y + 1), (double)(z + 1), minU, minV);
            t.addVertexWithUV((double)x + offset, (double)(y + 0), (double)(z + 1), minU, maxV);
            t.addVertexWithUV((double)x + offset, (double)(y + 0), (double)(z + 0), maxU, maxV);
            t.addVertexWithUV((double)x + offset, (double)(y + 1), (double)(z + 0), maxU, minV);
            t.addVertexWithUV((double)x + offset, (double)(y + 1), (double)(z + 0), maxU, minV);
            t.addVertexWithUV((double)x + offset, (double)(y + 0), (double)(z + 0), maxU, maxV);
            t.addVertexWithUV((double)x + offset, (double)(y + 0), (double)(z + 1), minU, maxV);
            t.addVertexWithUV((double)x + offset, (double)(y + 1), (double)(z + 1), minU, minV);
        }

        // bit 8 = east face (x+1-)
        if ((meta & 8) != 0) {
            t.addVertexWithUV((double)(x + 1) - offset, (double)(y + 0), (double)(z + 1), maxU, maxV);
            t.addVertexWithUV((double)(x + 1) - offset, (double)(y + 1), (double)(z + 1), maxU, minV);
            t.addVertexWithUV((double)(x + 1) - offset, (double)(y + 1), (double)(z + 0), minU, minV);
            t.addVertexWithUV((double)(x + 1) - offset, (double)(y + 0), (double)(z + 0), minU, maxV);
            t.addVertexWithUV((double)(x + 1) - offset, (double)(y + 0), (double)(z + 0), minU, maxV);
            t.addVertexWithUV((double)(x + 1) - offset, (double)(y + 1), (double)(z + 0), minU, minV);
            t.addVertexWithUV((double)(x + 1) - offset, (double)(y + 1), (double)(z + 1), maxU, minV);
            t.addVertexWithUV((double)(x + 1) - offset, (double)(y + 0), (double)(z + 1), maxU, maxV);
        }

        // bit 4 = north face (z+)
        if ((meta & 4) != 0) {
            t.addVertexWithUV((double)(x + 1), (double)(y + 0), (double)z + offset, maxU, maxV);
            t.addVertexWithUV((double)(x + 1), (double)(y + 1), (double)z + offset, maxU, minV);
            t.addVertexWithUV((double)(x + 0), (double)(y + 1), (double)z + offset, minU, minV);
            t.addVertexWithUV((double)(x + 0), (double)(y + 0), (double)z + offset, minU, maxV);
            t.addVertexWithUV((double)(x + 0), (double)(y + 0), (double)z + offset, minU, maxV);
            t.addVertexWithUV((double)(x + 0), (double)(y + 1), (double)z + offset, minU, minV);
            t.addVertexWithUV((double)(x + 1), (double)(y + 1), (double)z + offset, maxU, minV);
            t.addVertexWithUV((double)(x + 1), (double)(y + 0), (double)z + offset, maxU, maxV);
        }

        // bit 1 = south face (z+1-)
        if ((meta & 1) != 0) {
            t.addVertexWithUV((double)(x + 1), (double)(y + 1), (double)(z + 1) - offset, minU, minV);
            t.addVertexWithUV((double)(x + 1), (double)(y + 0), (double)(z + 1) - offset, minU, maxV);
            t.addVertexWithUV((double)(x + 0), (double)(y + 0), (double)(z + 1) - offset, maxU, maxV);
            t.addVertexWithUV((double)(x + 0), (double)(y + 1), (double)(z + 1) - offset, maxU, minV);
            t.addVertexWithUV((double)(x + 0), (double)(y + 1), (double)(z + 1) - offset, maxU, minV);
            t.addVertexWithUV((double)(x + 0), (double)(y + 0), (double)(z + 1) - offset, maxU, maxV);
            t.addVertexWithUV((double)(x + 1), (double)(y + 0), (double)(z + 1) - offset, minU, maxV);
            t.addVertexWithUV((double)(x + 1), (double)(y + 1), (double)(z + 1) - offset, minU, minV);
        }

        // Top face if block above is solid
        if (this.blockAccess.isBlockNormalCube(x, y + 1, z)) {
            t.addVertexWithUV((double)(x + 1), (double)(y + 1) - offset, (double)(z + 0), minU, minV);
            t.addVertexWithUV((double)(x + 1), (double)(y + 1) - offset, (double)(z + 1), minU, maxV);
            t.addVertexWithUV((double)(x + 0), (double)(y + 1) - offset, (double)(z + 1), maxU, maxV);
            t.addVertexWithUV((double)(x + 0), (double)(y + 1) - offset, (double)(z + 0), maxU, minV);
        }

        return true;
    }

    // --- renderBlockPane ---
    // Thin center pane with connections to adjacent blocks.
    public boolean renderBlockPane(BlockPane block, int x, int y, int z) {
        Tessellator t = Tessellator.instance;
        t.setBrightness(block.getMixedBrightnessForBlock(this.blockAccess, x, y, z));
        t.setColorOpaque_F(1.0F, 1.0F, 1.0F);

        Icon sideIcon = this.getBlockIconFromSideAndMetadata(block, 0, 0);
        Icon edgeIcon = block.getIcon(2, 0); // edge texture
        t.setCurrentTextureName(sideIcon != null ? sideIcon.getIconName() : null);

        double minU = sideIcon != null ? (double)sideIcon.getMinU() : 0;
        double maxU = sideIcon != null ? (double)sideIcon.getMaxU() : 1;
        double minV = sideIcon != null ? (double)sideIcon.getMinV() : 0;
        double maxV = sideIcon != null ? (double)sideIcon.getMaxV() : 1;
        double midU = sideIcon != null ? (double)sideIcon.getInterpolatedU(7.0D) : 0.4375;
        double mid1U = sideIcon != null ? (double)sideIcon.getInterpolatedU(9.0D) : 0.5625;

        // Connection checks
        boolean connectNegZ = block.canThisPaneConnectToThisBlockID(this.blockAccess.getBlockId(x, y, z - 1));
        boolean connectPosZ = block.canThisPaneConnectToThisBlockID(this.blockAccess.getBlockId(x, y, z + 1));
        boolean connectNegX = block.canThisPaneConnectToThisBlockID(this.blockAccess.getBlockId(x - 1, y, z));
        boolean connectPosX = block.canThisPaneConnectToThisBlockID(this.blockAccess.getBlockId(x + 1, y, z));

        // If no connections, render a center post
        if (!connectNegZ && !connectPosZ && !connectNegX && !connectPosX) {
            // Small center cross-section
            double cx = x + 0.4375D;
            double cx2 = x + 0.5625D;
            double cz = z + 0.4375D;
            double cz2 = z + 0.5625D;
            // Z-facing face
            t.addVertexWithUV(cx, y + 1, cz, midU, minV);
            t.addVertexWithUV(cx, y + 0, cz, midU, maxV);
            t.addVertexWithUV(cx, y + 0, cz2, mid1U, maxV);
            t.addVertexWithUV(cx, y + 1, cz2, mid1U, minV);
            t.addVertexWithUV(cx, y + 1, cz2, midU, minV);
            t.addVertexWithUV(cx, y + 0, cz2, midU, maxV);
            t.addVertexWithUV(cx, y + 0, cz, mid1U, maxV);
            t.addVertexWithUV(cx, y + 1, cz, mid1U, minV);

            t.addVertexWithUV(cx2, y + 1, cz2, midU, minV);
            t.addVertexWithUV(cx2, y + 0, cz2, midU, maxV);
            t.addVertexWithUV(cx2, y + 0, cz, mid1U, maxV);
            t.addVertexWithUV(cx2, y + 1, cz, mid1U, minV);
            t.addVertexWithUV(cx2, y + 1, cz, midU, minV);
            t.addVertexWithUV(cx2, y + 0, cz, midU, maxV);
            t.addVertexWithUV(cx2, y + 0, cz2, mid1U, maxV);
            t.addVertexWithUV(cx2, y + 1, cz2, mid1U, minV);
            return true;
        }

        // Pane runs along X or Z axis with thin geometry
        double paneCenterX = x + 0.5D;
        double paneCenterZ = z + 0.5D;
        double halfThick = 0.0625D; // 1 pixel / 16

        // X-axis pane (connect negX to posX)
        if (connectNegX || connectPosX) {
            double startX = connectNegX ? (double)x : paneCenterX - halfThick;
            double endX = connectPosX ? (double)(x + 1) : paneCenterX + halfThick;
            // Two face quads (front and back of thin pane along X)
            t.addVertexWithUV(startX, y + 1, paneCenterZ - halfThick, minU, minV);
            t.addVertexWithUV(startX, y + 0, paneCenterZ - halfThick, minU, maxV);
            t.addVertexWithUV(endX, y + 0, paneCenterZ - halfThick, maxU, maxV);
            t.addVertexWithUV(endX, y + 1, paneCenterZ - halfThick, maxU, minV);

            t.addVertexWithUV(endX, y + 1, paneCenterZ + halfThick, maxU, minV);
            t.addVertexWithUV(endX, y + 0, paneCenterZ + halfThick, maxU, maxV);
            t.addVertexWithUV(startX, y + 0, paneCenterZ + halfThick, minU, maxV);
            t.addVertexWithUV(startX, y + 1, paneCenterZ + halfThick, minU, minV);
        }

        // Z-axis pane (connect negZ to posZ)
        if (connectNegZ || connectPosZ) {
            double startZ = connectNegZ ? (double)z : paneCenterZ - halfThick;
            double endZ = connectPosZ ? (double)(z + 1) : paneCenterZ + halfThick;

            t.addVertexWithUV(paneCenterX + halfThick, y + 1, startZ, minU, minV);
            t.addVertexWithUV(paneCenterX + halfThick, y + 0, startZ, minU, maxV);
            t.addVertexWithUV(paneCenterX + halfThick, y + 0, endZ, maxU, maxV);
            t.addVertexWithUV(paneCenterX + halfThick, y + 1, endZ, maxU, minV);

            t.addVertexWithUV(paneCenterX - halfThick, y + 1, endZ, maxU, minV);
            t.addVertexWithUV(paneCenterX - halfThick, y + 0, endZ, maxU, maxV);
            t.addVertexWithUV(paneCenterX - halfThick, y + 0, startZ, minU, maxV);
            t.addVertexWithUV(paneCenterX - halfThick, y + 1, startZ, minU, minV);
        }

        return true;
    }

    // --- renderBlockFire ---
    // Fire renders as angled planes on adjacent flammable surfaces,
    // or as crossed planes on top of a solid/flammable block.
    public boolean renderBlockFire(BlockFire block, int x, int y, int z) {
        Tessellator t = Tessellator.instance;
        Icon icon0 = block.func_94438_c(0);
        Icon icon1 = block.func_94438_c(1);
        Icon icon = icon0;

        if (this.hasOverrideBlockTexture()) {
            icon = this.overrideBlockTexture;
        }

        t.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        t.setBrightness(block.getMixedBrightnessForBlock(this.blockAccess, x, y, z));
        t.setCurrentTextureName(icon != null ? icon.getIconName() : null);

        double minU = icon != null ? (double)icon.getMinU() : 0;
        double minV = icon != null ? (double)icon.getMinV() : 0;
        double maxU = icon != null ? (double)icon.getMaxU() : 1;
        double maxV = icon != null ? (double)icon.getMaxV() : 1;
        float height = 1.4F;

        boolean fireRendered = false;
        // Check if fire should render on sides of adjacent blocks (not upward)
        boolean renderUpward = this.blockAccess.isBlockNormalCube(x, y - 1, z)
                || block.canBlockCatchFire(this.blockAccess, x, y - 1, z);

        if (!renderUpward) {
            float inset = 0.2F;
            float yOff = 0.0625F;

            if ((x + y + z & 1) == 1) {
                minU = icon1 != null ? (double)icon1.getMinU() : 0;
                minV = icon1 != null ? (double)icon1.getMinV() : 0;
                maxU = icon1 != null ? (double)icon1.getMaxU() : 1;
                maxV = icon1 != null ? (double)icon1.getMaxV() : 1;
                t.setCurrentTextureName(icon1 != null ? icon1.getIconName() : null);
            }

            if ((x / 2 + y / 2 + z / 2 & 1) == 1) {
                double tmpU = maxU;
                maxU = minU;
                minU = tmpU;
            }

            // Render fire on adjacent -X face
            if (block.canBlockCatchFire(this.blockAccess, x - 1, y, z)) {
                t.addVertexWithUV((double)((float)x + inset), (double)((float)y + height + yOff), (double)(z + 1), maxU, minV);
                t.addVertexWithUV((double)(x + 0), (double)((float)(y + 0) + yOff), (double)(z + 1), maxU, maxV);
                t.addVertexWithUV((double)(x + 0), (double)((float)(y + 0) + yOff), (double)(z + 0), minU, maxV);
                t.addVertexWithUV((double)((float)x + inset), (double)((float)y + height + yOff), (double)(z + 0), minU, minV);
                t.addVertexWithUV((double)((float)x + inset), (double)((float)y + height + yOff), (double)(z + 0), minU, minV);
                t.addVertexWithUV((double)(x + 0), (double)((float)(y + 0) + yOff), (double)(z + 0), minU, maxV);
                t.addVertexWithUV((double)(x + 0), (double)((float)(y + 0) + yOff), (double)(z + 1), maxU, maxV);
                t.addVertexWithUV((double)((float)x + inset), (double)((float)y + height + yOff), (double)(z + 1), maxU, minV);
                fireRendered = true;
            }

            // Render fire on adjacent +X face
            if (block.canBlockCatchFire(this.blockAccess, x + 1, y, z)) {
                t.addVertexWithUV((double)((float)(x + 1) - inset), (double)((float)y + height + yOff), (double)(z + 0), minU, minV);
                t.addVertexWithUV((double)(x + 1), (double)((float)(y + 0) + yOff), (double)(z + 0), minU, maxV);
                t.addVertexWithUV((double)(x + 1), (double)((float)(y + 0) + yOff), (double)(z + 1), maxU, maxV);
                t.addVertexWithUV((double)((float)(x + 1) - inset), (double)((float)y + height + yOff), (double)(z + 1), maxU, minV);
                t.addVertexWithUV((double)((float)(x + 1) - inset), (double)((float)y + height + yOff), (double)(z + 1), maxU, minV);
                t.addVertexWithUV((double)(x + 1), (double)((float)(y + 0) + yOff), (double)(z + 1), maxU, maxV);
                t.addVertexWithUV((double)(x + 1), (double)((float)(y + 0) + yOff), (double)(z + 0), minU, maxV);
                t.addVertexWithUV((double)((float)(x + 1) - inset), (double)((float)y + height + yOff), (double)(z + 0), minU, minV);
                fireRendered = true;
            }

            // Render fire on adjacent -Z face
            if (block.canBlockCatchFire(this.blockAccess, x, y, z - 1)) {
                t.addVertexWithUV((double)(x + 0), (double)((float)y + height + yOff), (double)((float)z + inset), maxU, minV);
                t.addVertexWithUV((double)(x + 0), (double)((float)(y + 0) + yOff), (double)(z + 0), maxU, maxV);
                t.addVertexWithUV((double)(x + 1), (double)((float)(y + 0) + yOff), (double)(z + 0), minU, maxV);
                t.addVertexWithUV((double)(x + 1), (double)((float)y + height + yOff), (double)((float)z + inset), minU, minV);
                t.addVertexWithUV((double)(x + 1), (double)((float)y + height + yOff), (double)((float)z + inset), minU, minV);
                t.addVertexWithUV((double)(x + 1), (double)((float)(y + 0) + yOff), (double)(z + 0), minU, maxV);
                t.addVertexWithUV((double)(x + 0), (double)((float)(y + 0) + yOff), (double)(z + 0), maxU, maxV);
                t.addVertexWithUV((double)(x + 0), (double)((float)y + height + yOff), (double)((float)z + inset), maxU, minV);
                fireRendered = true;
            }

            // Render fire on adjacent +Z face
            if (block.canBlockCatchFire(this.blockAccess, x, y, z + 1)) {
                t.addVertexWithUV((double)(x + 1), (double)((float)y + height + yOff), (double)((float)(z + 1) - inset), minU, minV);
                t.addVertexWithUV((double)(x + 1), (double)((float)(y + 0) + yOff), (double)(z + 1), minU, maxV);
                t.addVertexWithUV((double)(x + 0), (double)((float)(y + 0) + yOff), (double)(z + 1), maxU, maxV);
                t.addVertexWithUV((double)(x + 0), (double)((float)y + height + yOff), (double)((float)(z + 1) - inset), maxU, minV);
                t.addVertexWithUV((double)(x + 0), (double)((float)y + height + yOff), (double)((float)(z + 1) - inset), maxU, minV);
                t.addVertexWithUV((double)(x + 0), (double)((float)(y + 0) + yOff), (double)(z + 1), maxU, maxV);
                t.addVertexWithUV((double)(x + 1), (double)((float)(y + 0) + yOff), (double)(z + 1), minU, maxV);
                t.addVertexWithUV((double)(x + 1), (double)((float)y + height + yOff), (double)((float)(z + 1) - inset), minU, minV);
                fireRendered = true;
            }

            // Render fire on top of adjacent block above
            if (block.canBlockCatchFire(this.blockAccess, x, y + 1, z)) {
                double x1 = (double)x + 0.5D + 0.5D;
                double x2 = (double)x + 0.5D - 0.5D;
                double z1 = (double)z + 0.5D + 0.5D;
                double z2 = (double)z + 0.5D - 0.5D;
                double x3 = (double)x + 0.5D - 0.5D;
                double x4 = (double)x + 0.5D + 0.5D;
                double z3 = (double)z + 0.5D - 0.5D;
                double z4 = (double)z + 0.5D + 0.5D;
                minU = (double)icon0.getMinU();
                minV = (double)icon0.getMinV();
                maxU = (double)icon0.getMaxU();
                maxV = (double)icon0.getMaxV();
                t.setCurrentTextureName(icon0 != null ? icon0.getIconName() : null);
                ++y;
                height = -0.2F;

                if ((x + y + z & 1) == 0) {
                    t.addVertexWithUV(x3, (double)((float)y + height), (double)(z + 0), maxU, minV);
                    t.addVertexWithUV(x1, (double)(y + 0), (double)(z + 0), maxU, maxV);
                    t.addVertexWithUV(x1, (double)(y + 0), (double)(z + 1), minU, maxV);
                    t.addVertexWithUV(x3, (double)((float)y + height), (double)(z + 1), minU, minV);
                    minU = (double)icon1.getMinU();
                    minV = (double)icon1.getMinV();
                    maxU = (double)icon1.getMaxU();
                    maxV = (double)icon1.getMaxV();
                    t.setCurrentTextureName(icon1 != null ? icon1.getIconName() : null);
                    t.addVertexWithUV(x4, (double)((float)y + height), (double)(z + 1), maxU, minV);
                    t.addVertexWithUV(x2, (double)(y + 0), (double)(z + 1), maxU, maxV);
                    t.addVertexWithUV(x2, (double)(y + 0), (double)(z + 0), minU, maxV);
                    t.addVertexWithUV(x4, (double)((float)y + height), (double)(z + 0), minU, minV);
                } else {
                    t.addVertexWithUV((double)(x + 0), (double)((float)y + height), z4, maxU, minV);
                    t.addVertexWithUV((double)(x + 0), (double)(y + 0), z2, maxU, maxV);
                    t.addVertexWithUV((double)(x + 1), (double)(y + 0), z2, minU, maxV);
                    t.addVertexWithUV((double)(x + 1), (double)((float)y + height), z4, minU, minV);
                    minU = (double)icon1.getMinU();
                    minV = (double)icon1.getMinV();
                    maxU = (double)icon1.getMaxU();
                    maxV = (double)icon1.getMaxV();
                    t.setCurrentTextureName(icon1 != null ? icon1.getIconName() : null);
                    t.addVertexWithUV((double)(x + 1), (double)((float)y + height), z3, maxU, minV);
                    t.addVertexWithUV((double)(x + 1), (double)(y + 0), z1, maxU, maxV);
                    t.addVertexWithUV((double)(x + 0), (double)(y + 0), z1, minU, maxV);
                    t.addVertexWithUV((double)(x + 0), (double)((float)y + height), z3, minU, minV);
                }
                fireRendered = true;
            }
        }

        // Render upward fire (on top of solid or flammable block below)
        if (!fireRendered && renderUpward) {
            double cx1 = (double)x + 0.5D + 0.2D;
            double cx2 = (double)x + 0.5D - 0.2D;
            double cz1 = (double)z + 0.5D + 0.2D;
            double cz2 = (double)z + 0.5D - 0.2D;
            double cx3 = (double)x + 0.5D - 0.3D;
            double cx4 = (double)x + 0.5D + 0.3D;
            double cz3 = (double)z + 0.5D - 0.3D;
            double cz4 = (double)z + 0.5D + 0.3D;

            t.addVertexWithUV(cx3, (double)((float)y + height), (double)(z + 1), maxU, minV);
            t.addVertexWithUV(cx1, (double)(y + 0), (double)(z + 1), maxU, maxV);
            t.addVertexWithUV(cx1, (double)(y + 0), (double)(z + 0), minU, maxV);
            t.addVertexWithUV(cx3, (double)((float)y + height), (double)(z + 0), minU, minV);
            t.addVertexWithUV(cx4, (double)((float)y + height), (double)(z + 0), maxU, minV);
            t.addVertexWithUV(cx2, (double)(y + 0), (double)(z + 0), maxU, maxV);
            t.addVertexWithUV(cx2, (double)(y + 0), (double)(z + 1), minU, maxV);
            t.addVertexWithUV(cx4, (double)((float)y + height), (double)(z + 1), minU, minV);

            minU = (double)icon1.getMinU();
            minV = (double)icon1.getMinV();
            maxU = (double)icon1.getMaxU();
            maxV = (double)icon1.getMaxV();
            t.setCurrentTextureName(icon1 != null ? icon1.getIconName() : null);

            t.addVertexWithUV((double)(x + 1), (double)((float)y + height), cz4, maxU, minV);
            t.addVertexWithUV((double)(x + 1), (double)(y + 0), cz2, maxU, maxV);
            t.addVertexWithUV((double)(x + 0), (double)(y + 0), cz2, minU, maxV);
            t.addVertexWithUV((double)(x + 0), (double)((float)y + height), cz4, minU, minV);
            t.addVertexWithUV((double)(x + 0), (double)((float)y + height), cz3, maxU, minV);
            t.addVertexWithUV((double)(x + 0), (double)(y + 0), cz1, maxU, maxV);
            t.addVertexWithUV((double)(x + 1), (double)(y + 0), cz1, minU, maxV);
            t.addVertexWithUV((double)(x + 1), (double)((float)y + height), cz3, minU, minV);

            cx1 = (double)x + 0.5D - 0.5D;
            cx2 = (double)x + 0.5D + 0.5D;
            cz1 = (double)z + 0.5D - 0.5D;
            cz2 = (double)z + 0.5D + 0.5D;
            cx3 = (double)x + 0.5D - 0.4D;
            cx4 = (double)x + 0.5D + 0.4D;
            cz3 = (double)z + 0.5D - 0.4D;
            cz4 = (double)z + 0.5D + 0.4D;

            minU = (double)icon0.getMinU();
            minV = (double)icon0.getMinV();
            maxU = (double)icon0.getMaxU();
            maxV = (double)icon0.getMaxV();
            t.setCurrentTextureName(icon0 != null ? icon0.getIconName() : null);

            t.addVertexWithUV(cx3, (double)((float)y + height), (double)(z + 0), minU, minV);
            t.addVertexWithUV(cx1, (double)(y + 0), (double)(z + 0), minU, maxV);
            t.addVertexWithUV(cx1, (double)(y + 0), (double)(z + 1), maxU, maxV);
            t.addVertexWithUV(cx3, (double)((float)y + height), (double)(z + 1), maxU, minV);
            t.addVertexWithUV(cx4, (double)((float)y + height), (double)(z + 1), minU, minV);
            t.addVertexWithUV(cx2, (double)(y + 0), (double)(z + 1), minU, maxV);
            t.addVertexWithUV(cx2, (double)(y + 0), (double)(z + 0), maxU, maxV);
            t.addVertexWithUV(cx4, (double)((float)y + height), (double)(z + 0), maxU, minV);

            minU = (double)icon0.getMinU();
            minV = (double)icon0.getMinV();
            maxU = (double)icon0.getMaxU();
            maxV = (double)icon0.getMaxV();

            t.addVertexWithUV((double)(x + 0), (double)((float)y + height), cz4, minU, minV);
            t.addVertexWithUV((double)(x + 0), (double)(y + 0), cz2, minU, maxV);
            t.addVertexWithUV((double)(x + 1), (double)(y + 0), cz2, maxU, maxV);
            t.addVertexWithUV((double)(x + 1), (double)((float)y + height), cz4, maxU, minV);
            t.addVertexWithUV((double)(x + 1), (double)((float)y + height), cz3, minU, minV);
            t.addVertexWithUV((double)(x + 1), (double)(y + 0), cz1, minU, maxV);
            t.addVertexWithUV((double)(x + 0), (double)(y + 0), cz1, maxU, maxV);
            t.addVertexWithUV((double)(x + 0), (double)((float)y + height), cz3, maxU, minV);
        }

        return true;
    }

    // --- renderBlockLever ---
    // Lever base plate (cobblestone) + angled handle stick.
    public boolean renderBlockLever(Block block, int x, int y, int z) {
        int meta = this.blockAccess.getBlockMetadata(x, y, z);
        int orientation = meta & 7;
        boolean isOn = (meta & 8) > 0;
        Tessellator t = Tessellator.instance;
        boolean hadOverride = this.hasOverrideBlockTexture();

        // Render base plate using cobblestone texture
        if (!hadOverride) {
            this.setOverrideBlockTexture(this.getBlockIcon(Block.cobblestone));
        }

        float baseW = 0.25F;
        float baseH = 0.1875F;
        float baseD = 0.1875F;

        if (orientation == 5) {
            this.setRenderBounds((double)(0.5F - baseH), 0.0D, (double)(0.5F - baseW), (double)(0.5F + baseH), (double)baseD, (double)(0.5F + baseW));
        } else if (orientation == 6) {
            this.setRenderBounds((double)(0.5F - baseW), 0.0D, (double)(0.5F - baseH), (double)(0.5F + baseW), (double)baseD, (double)(0.5F + baseH));
        } else if (orientation == 4) {
            this.setRenderBounds((double)(0.5F - baseH), (double)(0.5F - baseW), (double)(1.0F - baseD), (double)(0.5F + baseH), (double)(0.5F + baseW), 1.0D);
        } else if (orientation == 3) {
            this.setRenderBounds((double)(0.5F - baseH), (double)(0.5F - baseW), 0.0D, (double)(0.5F + baseH), (double)(0.5F + baseW), (double)baseD);
        } else if (orientation == 2) {
            this.setRenderBounds((double)(1.0F - baseD), (double)(0.5F - baseW), (double)(0.5F - baseH), 1.0D, (double)(0.5F + baseW), (double)(0.5F + baseH));
        } else if (orientation == 1) {
            this.setRenderBounds(0.0D, (double)(0.5F - baseW), (double)(0.5F - baseH), (double)baseD, (double)(0.5F + baseW), (double)(0.5F + baseH));
        } else if (orientation == 0) {
            this.setRenderBounds((double)(0.5F - baseW), (double)(1.0F - baseD), (double)(0.5F - baseH), (double)(0.5F + baseW), 1.0D, (double)(0.5F + baseH));
        } else if (orientation == 7) {
            this.setRenderBounds((double)(0.5F - baseH), (double)(1.0F - baseD), (double)(0.5F - baseW), (double)(0.5F + baseH), 1.0D, (double)(0.5F + baseW));
        }

        this.renderStandardBlock(block, x, y, z);

        if (!hadOverride) {
            this.clearOverrideBlockTexture();
        }

        // Render lever handle as a simple thin stick
        t.setBrightness(block.getMixedBrightnessForBlock(this.blockAccess, x, y, z));
        t.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        Icon leverIcon = this.getBlockIconFromSide(block, 0);
        if (this.hasOverrideBlockTexture()) {
            leverIcon = this.overrideBlockTexture;
        }
        t.setCurrentTextureName(leverIcon != null ? leverIcon.getIconName() : null);

        // Build lever stick geometry with rotation
        double stickW = 0.0625D;
        double stickH = 0.625D;

        Vec3[] verts = new Vec3[8];
        verts[0] = new Vec3(-stickW, 0.0D, -stickW);
        verts[1] = new Vec3(stickW, 0.0D, -stickW);
        verts[2] = new Vec3(stickW, 0.0D, stickW);
        verts[3] = new Vec3(-stickW, 0.0D, stickW);
        verts[4] = new Vec3(-stickW, stickH, -stickW);
        verts[5] = new Vec3(stickW, stickH, -stickW);
        verts[6] = new Vec3(stickW, stickH, stickW);
        verts[7] = new Vec3(-stickW, stickH, stickW);

        for (int i = 0; i < 8; ++i) {
            // Tilt based on on/off state
            if (isOn) {
                verts[i].zCoord -= 0.0625D;
                verts[i].rotateAroundX((float)(Math.PI * 2.0 / 9.0));
            } else {
                verts[i].zCoord += 0.0625D;
                verts[i].rotateAroundX((float)(-Math.PI * 2.0 / 9.0));
            }

            // Rotation based on orientation
            if (orientation == 0 || orientation == 7) {
                // Ceiling - flip upside down
                verts[i].yCoord = -verts[i].yCoord;
                verts[i].xCoord = -verts[i].xCoord;
            }

            if (orientation == 6 || orientation == 0) {
                // Rotate 90 degrees
                double tmpX = verts[i].xCoord;
                verts[i].xCoord = -verts[i].zCoord;
                verts[i].zCoord = tmpX;
            }

            if (orientation > 0 && orientation < 5) {
                // Wall-mounted: rotate from floor to wall
                verts[i].yCoord -= 0.375D;
                // Rotate around X by 90 degrees
                double tmpY = verts[i].yCoord;
                verts[i].yCoord = -verts[i].zCoord;
                verts[i].zCoord = tmpY;

                // Rotate around Y based on which wall
                if (orientation == 3) {
                    double tx = verts[i].xCoord;
                    verts[i].xCoord = -verts[i].zCoord;
                    verts[i].zCoord = tx;
                    tx = verts[i].xCoord;
                    verts[i].xCoord = -verts[i].zCoord;
                    verts[i].zCoord = tx;
                } else if (orientation == 2) {
                    double tx = verts[i].xCoord;
                    verts[i].xCoord = -verts[i].zCoord;
                    verts[i].zCoord = tx;
                } else if (orientation == 1) {
                    double tx = verts[i].xCoord;
                    verts[i].xCoord = verts[i].zCoord;
                    verts[i].zCoord = -tx;
                }

                verts[i].xCoord += (double)x + 0.5D;
                verts[i].yCoord += (double)((float)y + 0.5F);
                verts[i].zCoord += (double)z + 0.5D;
            } else if (orientation != 0 && orientation != 7) {
                verts[i].xCoord += (double)x + 0.5D;
                verts[i].yCoord += (double)((float)y + 0.125F);
                verts[i].zCoord += (double)z + 0.5D;
            } else {
                verts[i].xCoord += (double)x + 0.5D;
                verts[i].yCoord += (double)((float)y + 0.875F);
                verts[i].zCoord += (double)z + 0.5D;
            }
        }

        // Render 6 faces of the lever stick
        double leverMinU = leverIcon != null ? (double)leverIcon.getInterpolatedU(7.0D) : 0;
        double leverMinV = leverIcon != null ? (double)leverIcon.getInterpolatedV(6.0D) : 0;
        double leverMaxU = leverIcon != null ? (double)leverIcon.getInterpolatedU(9.0D) : 1;
        double leverMaxV = leverIcon != null ? (double)leverIcon.getInterpolatedV(8.0D) : 1;

        Vec3 a = null, b = null, c = null, d = null;

        for (int face = 0; face < 6; ++face) {
            if (face == 0) {
                leverMinU = leverIcon != null ? (double)leverIcon.getInterpolatedU(7.0D) : 0;
                leverMinV = leverIcon != null ? (double)leverIcon.getInterpolatedV(6.0D) : 0;
                leverMaxU = leverIcon != null ? (double)leverIcon.getInterpolatedU(9.0D) : 1;
                leverMaxV = leverIcon != null ? (double)leverIcon.getInterpolatedV(8.0D) : 1;
            } else if (face == 2) {
                leverMinU = leverIcon != null ? (double)leverIcon.getInterpolatedU(7.0D) : 0;
                leverMinV = leverIcon != null ? (double)leverIcon.getInterpolatedV(6.0D) : 0;
                leverMaxU = leverIcon != null ? (double)leverIcon.getInterpolatedU(9.0D) : 1;
                leverMaxV = leverIcon != null ? (double)leverIcon.getMaxV() : 1;
            }

            if (face == 0) { a = verts[0]; b = verts[1]; c = verts[2]; d = verts[3]; }
            else if (face == 1) { a = verts[7]; b = verts[6]; c = verts[5]; d = verts[4]; }
            else if (face == 2) { a = verts[1]; b = verts[0]; c = verts[4]; d = verts[5]; }
            else if (face == 3) { a = verts[2]; b = verts[1]; c = verts[5]; d = verts[6]; }
            else if (face == 4) { a = verts[3]; b = verts[2]; c = verts[6]; d = verts[7]; }
            else if (face == 5) { a = verts[0]; b = verts[3]; c = verts[7]; d = verts[4]; }

            t.addVertexWithUV(a.xCoord, a.yCoord, a.zCoord, leverMinU, leverMaxV);
            t.addVertexWithUV(b.xCoord, b.yCoord, b.zCoord, leverMaxU, leverMaxV);
            t.addVertexWithUV(c.xCoord, c.yCoord, c.zCoord, leverMaxU, leverMinV);
            t.addVertexWithUV(d.xCoord, d.yCoord, d.zCoord, leverMinU, leverMinV);
        }

        return true;
    }

    // --- renderBlockStairs ---
    // Stairs are rendered as multiple sub-boxes using the stair block's
    // func_82541_d / func_82542_g / func_82544_h methods which set bounds.
    // Since those methods may not exist in the modern stubs, we render a
    // simplified two-box stair shape based on metadata.
    public boolean renderBlockStairs(BlockStairs block, int x, int y, int z) {
        int meta = this.blockAccess.getBlockMetadata(x, y, z);
        int facing = meta & 3;    // 0=east, 1=west, 2=south, 3=north
        boolean upsideDown = (meta & 4) != 0;

        this.renderAllFaces = true;

        // Bottom/top slab (half block)
        if (upsideDown) {
            this.setRenderBounds(0.0D, 0.5D, 0.0D, 1.0D, 1.0D, 1.0D);
        } else {
            this.setRenderBounds(0.0D, 0.0D, 0.0D, 1.0D, 0.5D, 1.0D);
        }
        this.renderStandardBlock(block, x, y, z);

        // Step part (other half, partial)
        if (upsideDown) {
            switch (facing) {
                case 0: this.setRenderBounds(0.5D, 0.0D, 0.0D, 1.0D, 0.5D, 1.0D); break;
                case 1: this.setRenderBounds(0.0D, 0.0D, 0.0D, 0.5D, 0.5D, 1.0D); break;
                case 2: this.setRenderBounds(0.0D, 0.0D, 0.5D, 1.0D, 0.5D, 1.0D); break;
                case 3: this.setRenderBounds(0.0D, 0.0D, 0.0D, 1.0D, 0.5D, 0.5D); break;
            }
        } else {
            switch (facing) {
                case 0: this.setRenderBounds(0.5D, 0.5D, 0.0D, 1.0D, 1.0D, 1.0D); break;
                case 1: this.setRenderBounds(0.0D, 0.5D, 0.0D, 0.5D, 1.0D, 1.0D); break;
                case 2: this.setRenderBounds(0.0D, 0.5D, 0.5D, 1.0D, 1.0D, 1.0D); break;
                case 3: this.setRenderBounds(0.0D, 0.5D, 0.0D, 1.0D, 1.0D, 0.5D); break;
            }
        }
        this.renderStandardBlock(block, x, y, z);

        this.renderAllFaces = false;
        this.setRenderBounds(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
        return true;
    }

    // --- renderBlockFenceGate ---
    // Gate posts and crossbars, open/closed states from vanilla.
    public boolean renderBlockFenceGate(BlockFenceGate block, int x, int y, int z) {
        int meta = this.blockAccess.getBlockMetadata(x, y, z);
        boolean isOpen = BlockFenceGate.isFenceGateOpen(meta);
        int direction = BlockDirectional.getDirection(meta);
        float yBase = 0.3125F;
        float yTop = 1.0F;
        float barBottom = 0.375F;
        float barMid = 0.5625F;
        float barTop2 = 0.75F;
        float barTop = 0.9375F;

        this.renderAllFaces = true;

        // Posts
        if (direction != 3 && direction != 1) {
            // NS orientation - posts on X ends
            this.setRenderBounds(0.0D, (double)yBase, 0.4375D, 0.125D, (double)yTop, 0.5625D);
            this.renderStandardBlock(block, x, y, z);
            this.setRenderBounds(0.875D, (double)yBase, 0.4375D, 1.0D, (double)yTop, 0.5625D);
            this.renderStandardBlock(block, x, y, z);
        } else {
            // EW orientation - posts on Z ends
            this.uvRotateTop = 1;
            this.setRenderBounds(0.4375D, (double)yBase, 0.0D, 0.5625D, (double)yTop, 0.125D);
            this.renderStandardBlock(block, x, y, z);
            this.setRenderBounds(0.4375D, (double)yBase, 0.875D, 0.5625D, (double)yTop, 1.0D);
            this.renderStandardBlock(block, x, y, z);
            this.uvRotateTop = 0;
        }

        if (isOpen) {
            if (direction == 2 || direction == 0) {
                this.uvRotateTop = 1;
            }

            if (direction == 3) {
                this.setRenderBounds(0.8125D, (double)barBottom, 0.0D, 0.9375D, (double)barTop, 0.125D);
                this.renderStandardBlock(block, x, y, z);
                this.setRenderBounds(0.8125D, (double)barBottom, 0.875D, 0.9375D, (double)barTop, 1.0D);
                this.renderStandardBlock(block, x, y, z);
                this.setRenderBounds(0.5625D, (double)barBottom, 0.0D, 0.8125D, (double)barMid, 0.125D);
                this.renderStandardBlock(block, x, y, z);
                this.setRenderBounds(0.5625D, (double)barBottom, 0.875D, 0.8125D, (double)barMid, 1.0D);
                this.renderStandardBlock(block, x, y, z);
                this.setRenderBounds(0.5625D, (double)barTop2, 0.0D, 0.8125D, (double)barTop, 0.125D);
                this.renderStandardBlock(block, x, y, z);
                this.setRenderBounds(0.5625D, (double)barTop2, 0.875D, 0.8125D, (double)barTop, 1.0D);
                this.renderStandardBlock(block, x, y, z);
            } else if (direction == 1) {
                this.setRenderBounds(0.0625D, (double)barBottom, 0.0D, 0.1875D, (double)barTop, 0.125D);
                this.renderStandardBlock(block, x, y, z);
                this.setRenderBounds(0.0625D, (double)barBottom, 0.875D, 0.1875D, (double)barTop, 1.0D);
                this.renderStandardBlock(block, x, y, z);
                this.setRenderBounds(0.1875D, (double)barBottom, 0.0D, 0.4375D, (double)barMid, 0.125D);
                this.renderStandardBlock(block, x, y, z);
                this.setRenderBounds(0.1875D, (double)barBottom, 0.875D, 0.4375D, (double)barMid, 1.0D);
                this.renderStandardBlock(block, x, y, z);
                this.setRenderBounds(0.1875D, (double)barTop2, 0.0D, 0.4375D, (double)barTop, 0.125D);
                this.renderStandardBlock(block, x, y, z);
                this.setRenderBounds(0.1875D, (double)barTop2, 0.875D, 0.4375D, (double)barTop, 1.0D);
                this.renderStandardBlock(block, x, y, z);
            } else if (direction == 0) {
                this.setRenderBounds(0.0D, (double)barBottom, 0.8125D, 0.125D, (double)barTop, 0.9375D);
                this.renderStandardBlock(block, x, y, z);
                this.setRenderBounds(0.875D, (double)barBottom, 0.8125D, 1.0D, (double)barTop, 0.9375D);
                this.renderStandardBlock(block, x, y, z);
                this.setRenderBounds(0.0D, (double)barBottom, 0.5625D, 0.125D, (double)barMid, 0.8125D);
                this.renderStandardBlock(block, x, y, z);
                this.setRenderBounds(0.875D, (double)barBottom, 0.5625D, 1.0D, (double)barMid, 0.8125D);
                this.renderStandardBlock(block, x, y, z);
                this.setRenderBounds(0.0D, (double)barTop2, 0.5625D, 0.125D, (double)barTop, 0.8125D);
                this.renderStandardBlock(block, x, y, z);
                this.setRenderBounds(0.875D, (double)barTop2, 0.5625D, 1.0D, (double)barTop, 0.8125D);
                this.renderStandardBlock(block, x, y, z);
            } else if (direction == 2) {
                this.setRenderBounds(0.0D, (double)barBottom, 0.0625D, 0.125D, (double)barTop, 0.1875D);
                this.renderStandardBlock(block, x, y, z);
                this.setRenderBounds(0.875D, (double)barBottom, 0.0625D, 1.0D, (double)barTop, 0.1875D);
                this.renderStandardBlock(block, x, y, z);
                this.setRenderBounds(0.0D, (double)barBottom, 0.1875D, 0.125D, (double)barMid, 0.4375D);
                this.renderStandardBlock(block, x, y, z);
                this.setRenderBounds(0.875D, (double)barBottom, 0.1875D, 1.0D, (double)barMid, 0.4375D);
                this.renderStandardBlock(block, x, y, z);
                this.setRenderBounds(0.0D, (double)barTop2, 0.1875D, 0.125D, (double)barTop, 0.4375D);
                this.renderStandardBlock(block, x, y, z);
                this.setRenderBounds(0.875D, (double)barTop2, 0.1875D, 1.0D, (double)barTop, 0.4375D);
                this.renderStandardBlock(block, x, y, z);
            }
        } else if (direction != 3 && direction != 1) {
            // Closed, NS orientation
            this.setRenderBounds(0.375D, (double)barBottom, 0.4375D, 0.5D, (double)barTop, 0.5625D);
            this.renderStandardBlock(block, x, y, z);
            this.setRenderBounds(0.5D, (double)barBottom, 0.4375D, 0.625D, (double)barTop, 0.5625D);
            this.renderStandardBlock(block, x, y, z);
            this.setRenderBounds(0.625D, (double)barBottom, 0.4375D, 0.875D, (double)barMid, 0.5625D);
            this.renderStandardBlock(block, x, y, z);
            this.setRenderBounds(0.625D, (double)barTop2, 0.4375D, 0.875D, (double)barTop, 0.5625D);
            this.renderStandardBlock(block, x, y, z);
            this.setRenderBounds(0.125D, (double)barBottom, 0.4375D, 0.375D, (double)barMid, 0.5625D);
            this.renderStandardBlock(block, x, y, z);
            this.setRenderBounds(0.125D, (double)barTop2, 0.4375D, 0.375D, (double)barTop, 0.5625D);
            this.renderStandardBlock(block, x, y, z);
        } else {
            // Closed, EW orientation
            this.uvRotateTop = 1;
            this.setRenderBounds(0.4375D, (double)barBottom, 0.375D, 0.5625D, (double)barTop, 0.5D);
            this.renderStandardBlock(block, x, y, z);
            this.setRenderBounds(0.4375D, (double)barBottom, 0.5D, 0.5625D, (double)barTop, 0.625D);
            this.renderStandardBlock(block, x, y, z);
            this.setRenderBounds(0.4375D, (double)barBottom, 0.625D, 0.5625D, (double)barMid, 0.875D);
            this.renderStandardBlock(block, x, y, z);
            this.setRenderBounds(0.4375D, (double)barTop2, 0.625D, 0.5625D, (double)barTop, 0.875D);
            this.renderStandardBlock(block, x, y, z);
            this.setRenderBounds(0.4375D, (double)barBottom, 0.125D, 0.5625D, (double)barMid, 0.375D);
            this.renderStandardBlock(block, x, y, z);
            this.setRenderBounds(0.4375D, (double)barTop2, 0.125D, 0.5625D, (double)barTop, 0.375D);
            this.renderStandardBlock(block, x, y, z);
        }

        this.renderAllFaces = false;
        this.uvRotateTop = 0;
        this.setRenderBounds(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
        return true;
    }

    // --- renderBlockLog ---
    // Log rendering with UV rotation based on axis metadata.
    public boolean renderBlockLog(Block block, int x, int y, int z) {
        int meta = this.blockAccess.getBlockMetadata(x, y, z);
        int axis = meta & 12;

        if (axis == 4) {
            // X-axis
            this.uvRotateEast = 1;
            this.uvRotateWest = 1;
            this.uvRotateTop = 1;
            this.uvRotateBottom = 1;
        } else if (axis == 8) {
            // Z-axis
            this.uvRotateSouth = 1;
            this.uvRotateNorth = 1;
        }

        boolean result = this.renderStandardBlock(block, x, y, z);
        this.uvRotateSouth = 0;
        this.uvRotateEast = 0;
        this.uvRotateWest = 0;
        this.uvRotateNorth = 0;
        this.uvRotateTop = 0;
        this.uvRotateBottom = 0;
        return result;
    }

    public boolean renderBlockQuartz(Block block, int x, int y, int z) {
        int meta = this.blockAccess.getBlockMetadata(x, y, z);

        if (meta == 3) {
            this.uvRotateEast = 1;
            this.uvRotateWest = 1;
            this.uvRotateTop = 1;
            this.uvRotateBottom = 1;
        } else if (meta == 4) {
            this.uvRotateSouth = 1;
            this.uvRotateNorth = 1;
        }

        boolean result = this.renderStandardBlock(block, x, y, z);
        this.uvRotateSouth = 0;
        this.uvRotateEast = 0;
        this.uvRotateWest = 0;
        this.uvRotateNorth = 0;
        this.uvRotateTop = 0;
        this.uvRotateBottom = 0;
        return result;
    }

    // --- renderBlockCactus ---
    // Cactus renders with inset side faces (1 pixel from block edges).
    public boolean renderBlockCactus(Block block, int x, int y, int z) {
        int colorMult = block.colorMultiplier(this.blockAccess, x, y, z);
        float cr = (float)(colorMult >> 16 & 255) / 255.0F;
        float cg = (float)(colorMult >> 8 & 255) / 255.0F;
        float cb = (float)(colorMult & 255) / 255.0F;
        return this.renderBlockCactusImpl(block, x, y, z, cr, cg, cb);
    }

    public boolean renderBlockCactusImpl(Block block, int x, int y, int z, float cr, float cg, float cb) {
        Tessellator t = Tessellator.instance;
        boolean rendered = false;
        float inset = 0.0625F;
        int brightness = block.getMixedBrightnessForBlock(this.blockAccess, x, y, z);

        // Bottom face
        if (this.renderAllFaces || block.shouldSideBeRendered(this.blockAccess, x, y - 1, z, 0)) {
            t.setBrightness(this.renderMinY > 0.0D ? brightness : block.getMixedBrightnessForBlock(this.blockAccess, x, y - 1, z));
            t.setColorOpaque_F(0.5F * cr, 0.5F * cg, 0.5F * cb);
            this.renderFaceYNeg(block, (double)x, (double)y, (double)z, this.getBlockIcon(block, this.blockAccess, x, y, z, 0));
            rendered = true;
        }

        // Top face
        if (this.renderAllFaces || block.shouldSideBeRendered(this.blockAccess, x, y + 1, z, 1)) {
            t.setBrightness(this.renderMaxY < 1.0D ? brightness : block.getMixedBrightnessForBlock(this.blockAccess, x, y + 1, z));
            t.setColorOpaque_F(1.0F * cr, 1.0F * cg, 1.0F * cb);
            this.renderFaceYPos(block, (double)x, (double)y, (double)z, this.getBlockIcon(block, this.blockAccess, x, y, z, 1));
            rendered = true;
        }

        // Z-neg face (inset +Z by 1 pixel)
        if (this.renderAllFaces || block.shouldSideBeRendered(this.blockAccess, x, y, z - 1, 2)) {
            t.setBrightness(this.renderMinZ > 0.0D ? brightness : block.getMixedBrightnessForBlock(this.blockAccess, x, y, z - 1));
            t.setColorOpaque_F(0.8F * cr, 0.8F * cg, 0.8F * cb);
            t.addTranslation(0.0F, 0.0F, inset);
            this.renderFaceZNeg(block, (double)x, (double)y, (double)z, this.getBlockIcon(block, this.blockAccess, x, y, z, 2));
            t.addTranslation(0.0F, 0.0F, -inset);
            rendered = true;
        }

        // Z-pos face (inset -Z by 1 pixel)
        if (this.renderAllFaces || block.shouldSideBeRendered(this.blockAccess, x, y, z + 1, 3)) {
            t.setBrightness(this.renderMaxZ < 1.0D ? brightness : block.getMixedBrightnessForBlock(this.blockAccess, x, y, z + 1));
            t.setColorOpaque_F(0.8F * cr, 0.8F * cg, 0.8F * cb);
            t.addTranslation(0.0F, 0.0F, -inset);
            this.renderFaceZPos(block, (double)x, (double)y, (double)z, this.getBlockIcon(block, this.blockAccess, x, y, z, 3));
            t.addTranslation(0.0F, 0.0F, inset);
            rendered = true;
        }

        // X-neg face (inset +X by 1 pixel)
        if (this.renderAllFaces || block.shouldSideBeRendered(this.blockAccess, x - 1, y, z, 4)) {
            t.setBrightness(this.renderMinX > 0.0D ? brightness : block.getMixedBrightnessForBlock(this.blockAccess, x - 1, y, z));
            t.setColorOpaque_F(0.6F * cr, 0.6F * cg, 0.6F * cb);
            t.addTranslation(inset, 0.0F, 0.0F);
            this.renderFaceXNeg(block, (double)x, (double)y, (double)z, this.getBlockIcon(block, this.blockAccess, x, y, z, 4));
            t.addTranslation(-inset, 0.0F, 0.0F);
            rendered = true;
        }

        // X-pos face (inset -X by 1 pixel)
        if (this.renderAllFaces || block.shouldSideBeRendered(this.blockAccess, x + 1, y, z, 5)) {
            t.setBrightness(this.renderMaxX < 1.0D ? brightness : block.getMixedBrightnessForBlock(this.blockAccess, x + 1, y, z));
            t.setColorOpaque_F(0.6F * cr, 0.6F * cg, 0.6F * cb);
            t.addTranslation(-inset, 0.0F, 0.0F);
            this.renderFaceXPos(block, (double)x, (double)y, (double)z, this.getBlockIcon(block, this.blockAccess, x, y, z, 5));
            t.addTranslation(inset, 0.0F, 0.0F);
            rendered = true;
        }

        return rendered;
    }

    // --- renderBlockFence ---
    // Fence post with connecting bars based on neighbors.
    public boolean renderBlockFence(BlockFence block, int x, int y, int z) {
        // Center post
        this.setRenderBounds(0.375D, 0.0D, 0.375D, 0.625D, 1.0D, 0.625D);
        this.renderStandardBlock(block, x, y, z);

        boolean connectNegX = block.canConnectFenceTo(this.blockAccess, x - 1, y, z);
        boolean connectPosX = block.canConnectFenceTo(this.blockAccess, x + 1, y, z);
        boolean connectNegZ = block.canConnectFenceTo(this.blockAccess, x, y, z - 1);
        boolean connectPosZ = block.canConnectFenceTo(this.blockAccess, x, y, z + 1);

        // Horizontal bars (top and bottom rail)
        if (connectNegX) {
            this.setRenderBounds(0.0D, 0.75D, 0.4375D, 0.375D, 0.9375D, 0.5625D);
            this.renderStandardBlock(block, x, y, z);
            this.setRenderBounds(0.0D, 0.375D, 0.4375D, 0.375D, 0.5625D, 0.5625D);
            this.renderStandardBlock(block, x, y, z);
        }
        if (connectPosX) {
            this.setRenderBounds(0.625D, 0.75D, 0.4375D, 1.0D, 0.9375D, 0.5625D);
            this.renderStandardBlock(block, x, y, z);
            this.setRenderBounds(0.625D, 0.375D, 0.4375D, 1.0D, 0.5625D, 0.5625D);
            this.renderStandardBlock(block, x, y, z);
        }
        if (connectNegZ) {
            this.setRenderBounds(0.4375D, 0.75D, 0.0D, 0.5625D, 0.9375D, 0.375D);
            this.renderStandardBlock(block, x, y, z);
            this.setRenderBounds(0.4375D, 0.375D, 0.0D, 0.5625D, 0.5625D, 0.375D);
            this.renderStandardBlock(block, x, y, z);
        }
        if (connectPosZ) {
            this.setRenderBounds(0.4375D, 0.75D, 0.625D, 0.5625D, 0.9375D, 1.0D);
            this.renderStandardBlock(block, x, y, z);
            this.setRenderBounds(0.4375D, 0.375D, 0.625D, 0.5625D, 0.5625D, 1.0D);
            this.renderStandardBlock(block, x, y, z);
        }

        this.setRenderBoundsFromBlock(block);
        return true;
    }

    // --- renderBlockWall ---
    // Wall post with connections based on neighbors.
    public boolean renderBlockWall(BlockWall block, int x, int y, int z) {
        boolean connectWest = block.canConnectWallTo(this.blockAccess, x - 1, y, z);
        boolean connectEast = block.canConnectWallTo(this.blockAccess, x + 1, y, z);
        boolean connectNorth = block.canConnectWallTo(this.blockAccess, x, y, z - 1);
        boolean connectSouth = block.canConnectWallTo(this.blockAccess, x, y, z + 1);
        boolean straightNS = connectNorth && connectSouth && !connectWest && !connectEast;
        boolean straightEW = !connectNorth && !connectSouth && connectWest && connectEast;
        boolean airAbove = this.blockAccess.isAirBlock(x, y + 1, z);

        if ((straightNS || straightEW) && airAbove) {
            if (straightNS) {
                this.setRenderBounds(0.3125D, 0.0D, 0.0D, 0.6875D, 0.8125D, 1.0D);
                this.renderStandardBlock(block, x, y, z);
            } else {
                this.setRenderBounds(0.0D, 0.0D, 0.3125D, 1.0D, 0.8125D, 0.6875D);
                this.renderStandardBlock(block, x, y, z);
            }
        } else {
            // Center post
            this.setRenderBounds(0.25D, 0.0D, 0.25D, 0.75D, 1.0D, 0.75D);
            this.renderStandardBlock(block, x, y, z);

            if (connectWest) {
                this.setRenderBounds(0.0D, 0.0D, 0.3125D, 0.25D, 0.8125D, 0.6875D);
                this.renderStandardBlock(block, x, y, z);
            }
            if (connectEast) {
                this.setRenderBounds(0.75D, 0.0D, 0.3125D, 1.0D, 0.8125D, 0.6875D);
                this.renderStandardBlock(block, x, y, z);
            }
            if (connectNorth) {
                this.setRenderBounds(0.3125D, 0.0D, 0.0D, 0.6875D, 0.8125D, 0.25D);
                this.renderStandardBlock(block, x, y, z);
            }
            if (connectSouth) {
                this.setRenderBounds(0.3125D, 0.0D, 0.75D, 0.6875D, 0.8125D, 1.0D);
                this.renderStandardBlock(block, x, y, z);
            }
        }

        block.setBlockBoundsBasedOnState(this.blockAccess, x, y, z);
        return true;
    }

    // --- renderBlockStem ---
    // Pumpkin/melon stem with growth stages using crossed squares.
    public boolean renderBlockStem(Block block, int x, int y, int z) {
        Tessellator t = Tessellator.instance;
        t.setBrightness(block.getMixedBrightnessForBlock(this.blockAccess, x, y, z));
        int colorMult = block.colorMultiplier(this.blockAccess, x, y, z);
        float cr = (float)(colorMult >> 16 & 255) / 255.0F;
        float cg = (float)(colorMult >> 8 & 255) / 255.0F;
        float cb = (float)(colorMult & 255) / 255.0F;
        t.setColorOpaque_F(cr, cg, cb);

        // Use the block's bounds for height (set by setBlockBoundsBasedOnState)
        block.setBlockBoundsBasedOnState(this.blockAccess, x, y, z);
        double height = block.getBlockBoundsMaxY();

        Icon icon = this.getBlockIconFromSideAndMetadata(block, 0, this.blockAccess.getBlockMetadata(x, y, z));
        t.setCurrentTextureName(icon != null ? icon.getIconName() : null);
        double minU = icon != null ? (double)icon.getMinU() : 0;
        double maxU = icon != null ? (double)icon.getMaxU() : 1;
        double minV = icon != null ? (double)icon.getMinV() : 0;
        double maxV = icon != null ? (double)icon.getMaxV() : 1;

        // Render as crossed squares at the stem height
        double cx = x + 0.5D;
        double cz = z + 0.5D;
        double d = 0.25D;

        t.setNormal(0, 1, 0);
        t.addVertexWithUV(cx - d, y, cz - d, minU, maxV);
        t.addVertexWithUV(cx - d, y + height, cz - d, minU, minV);
        t.addVertexWithUV(cx + d, y + height, cz + d, maxU, minV);
        t.addVertexWithUV(cx + d, y, cz + d, maxU, maxV);
        t.addVertexWithUV(cx + d, y, cz + d, minU, maxV);
        t.addVertexWithUV(cx + d, y + height, cz + d, minU, minV);
        t.addVertexWithUV(cx - d, y + height, cz - d, maxU, minV);
        t.addVertexWithUV(cx - d, y, cz - d, maxU, maxV);
        t.addVertexWithUV(cx - d, y, cz + d, minU, maxV);
        t.addVertexWithUV(cx - d, y + height, cz + d, minU, minV);
        t.addVertexWithUV(cx + d, y + height, cz - d, maxU, minV);
        t.addVertexWithUV(cx + d, y, cz - d, maxU, maxV);
        t.addVertexWithUV(cx + d, y, cz - d, minU, maxV);
        t.addVertexWithUV(cx + d, y + height, cz - d, minU, minV);
        t.addVertexWithUV(cx - d, y + height, cz + d, maxU, minV);
        t.addVertexWithUV(cx - d, y, cz + d, maxU, maxV);

        return true;
    }

    // --- renderPistonBase ---
    // Piston base with correct bounds when extended (leaving room for arm).
    public boolean renderPistonBase(Block block, int x, int y, int z, boolean isExtended) {
        int meta = this.blockAccess.getBlockMetadata(x, y, z);
        boolean extended = isExtended || (meta & 8) != 0;
        int facing = BlockPistonBase.getOrientation(meta);

        if (extended) {
            switch (facing) {
                case 0:
                    this.uvRotateEast = 3;
                    this.uvRotateWest = 3;
                    this.uvRotateSouth = 3;
                    this.uvRotateNorth = 3;
                    this.setRenderBounds(0.0D, 0.25D, 0.0D, 1.0D, 1.0D, 1.0D);
                    break;
                case 1:
                    this.setRenderBounds(0.0D, 0.0D, 0.0D, 1.0D, 0.75D, 1.0D);
                    break;
                case 2:
                    this.uvRotateSouth = 1;
                    this.uvRotateNorth = 2;
                    this.setRenderBounds(0.0D, 0.0D, 0.25D, 1.0D, 1.0D, 1.0D);
                    break;
                case 3:
                    this.uvRotateSouth = 2;
                    this.uvRotateNorth = 1;
                    this.uvRotateTop = 3;
                    this.uvRotateBottom = 3;
                    this.setRenderBounds(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 0.75D);
                    break;
                case 4:
                    this.uvRotateEast = 1;
                    this.uvRotateWest = 2;
                    this.uvRotateTop = 2;
                    this.uvRotateBottom = 1;
                    this.setRenderBounds(0.25D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
                    break;
                case 5:
                    this.uvRotateEast = 2;
                    this.uvRotateWest = 1;
                    this.uvRotateTop = 1;
                    this.uvRotateBottom = 2;
                    this.setRenderBounds(0.0D, 0.0D, 0.0D, 0.75D, 1.0D, 1.0D);
                    break;
            }

            this.renderStandardBlock(block, x, y, z);
            this.uvRotateEast = 0;
            this.uvRotateWest = 0;
            this.uvRotateSouth = 0;
            this.uvRotateNorth = 0;
            this.uvRotateTop = 0;
            this.uvRotateBottom = 0;
            this.setRenderBounds(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
        } else {
            switch (facing) {
                case 0:
                    this.uvRotateEast = 3;
                    this.uvRotateWest = 3;
                    this.uvRotateSouth = 3;
                    this.uvRotateNorth = 3;
                    break;
                case 2:
                    this.uvRotateSouth = 1;
                    this.uvRotateNorth = 2;
                    break;
                case 3:
                    this.uvRotateSouth = 2;
                    this.uvRotateNorth = 1;
                    this.uvRotateTop = 3;
                    this.uvRotateBottom = 3;
                    break;
                case 4:
                    this.uvRotateEast = 1;
                    this.uvRotateWest = 2;
                    this.uvRotateTop = 2;
                    this.uvRotateBottom = 1;
                    break;
                case 5:
                    this.uvRotateEast = 2;
                    this.uvRotateWest = 1;
                    this.uvRotateTop = 1;
                    this.uvRotateBottom = 2;
                    break;
            }

            this.renderStandardBlock(block, x, y, z);
            this.uvRotateEast = 0;
            this.uvRotateWest = 0;
            this.uvRotateSouth = 0;
            this.uvRotateNorth = 0;
            this.uvRotateTop = 0;
            this.uvRotateBottom = 0;
        }

        return true;
    }

    public void renderPistonBaseAllFaces(Block block, int x, int y, int z) {}

    // --- renderPistonExtension ---
    // Piston arm/head rendered as a flat plate + rod segments.
    public boolean renderPistonExtension(Block block, int x, int y, int z, boolean isHead) {
        int meta = this.blockAccess.getBlockMetadata(x, y, z);
        int facing = BlockPistonExtension.getDirectionMeta(meta);

        // Render the head plate
        switch (facing) {
            case 0:
                this.uvRotateEast = 3;
                this.uvRotateWest = 3;
                this.uvRotateSouth = 3;
                this.uvRotateNorth = 3;
                this.setRenderBounds(0.0D, 0.0D, 0.0D, 1.0D, 0.25D, 1.0D);
                break;
            case 1:
                this.setRenderBounds(0.0D, 0.75D, 0.0D, 1.0D, 1.0D, 1.0D);
                break;
            case 2:
                this.uvRotateSouth = 1;
                this.uvRotateNorth = 2;
                this.setRenderBounds(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 0.25D);
                break;
            case 3:
                this.uvRotateSouth = 2;
                this.uvRotateNorth = 1;
                this.uvRotateTop = 3;
                this.uvRotateBottom = 3;
                this.setRenderBounds(0.0D, 0.0D, 0.75D, 1.0D, 1.0D, 1.0D);
                break;
            case 4:
                this.uvRotateEast = 1;
                this.uvRotateWest = 2;
                this.uvRotateTop = 2;
                this.uvRotateBottom = 1;
                this.setRenderBounds(0.0D, 0.0D, 0.0D, 0.25D, 1.0D, 1.0D);
                break;
            case 5:
                this.uvRotateEast = 2;
                this.uvRotateWest = 1;
                this.uvRotateTop = 1;
                this.uvRotateBottom = 2;
                this.setRenderBounds(0.75D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
                break;
        }

        this.renderStandardBlock(block, x, y, z);

        // Render the rod as a thin box
        float rodMin = 0.375F;
        float rodMax = 0.625F;
        float rodLen = isHead ? 1.0F : 0.5F;

        switch (facing) {
            case 0:
                this.setRenderBounds(rodMin, 0.25D, rodMin, rodMax, 0.25D + rodLen, rodMax);
                break;
            case 1:
                this.setRenderBounds(rodMin, 0.75D - rodLen, rodMin, rodMax, 0.75D, rodMax);
                break;
            case 2:
                this.setRenderBounds(rodMin, rodMin, 0.25D, rodMax, rodMax, 0.25D + rodLen);
                break;
            case 3:
                this.setRenderBounds(rodMin, rodMin, 0.75D - rodLen, rodMax, rodMax, 0.75D);
                break;
            case 4:
                this.setRenderBounds(0.25D, rodMin, rodMin, 0.25D + rodLen, rodMax, rodMax);
                break;
            case 5:
                this.setRenderBounds(0.75D - rodLen, rodMin, rodMin, 0.75D, rodMax, rodMax);
                break;
        }

        this.renderStandardBlock(block, x, y, z);

        this.uvRotateEast = 0;
        this.uvRotateWest = 0;
        this.uvRotateSouth = 0;
        this.uvRotateNorth = 0;
        this.uvRotateTop = 0;
        this.uvRotateBottom = 0;
        this.setRenderBounds(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
        return true;
    }

    public void renderPistonExtensionAllFaces(Block block, int x, int y, int z, boolean isHead) {
        this.renderAllFaces = true;
        this.renderPistonExtension(block, x, y, z, isHead);
        this.renderAllFaces = false;
    }

    // --- renderBlockLilyPad ---
    // Flat pad floating on water with random rotation.
    public boolean renderBlockLilyPad(Block block, int x, int y, int z) {
        Tessellator t = Tessellator.instance;
        Icon icon = this.getBlockIconFromSide(block, 1);

        if (this.hasOverrideBlockTexture()) {
            icon = this.overrideBlockTexture;
        }

        t.setCurrentTextureName(icon != null ? icon.getIconName() : null);

        float yOff = 0.015625F;
        double minU = icon != null ? (double)icon.getMinU() : 0;
        double minV = icon != null ? (double)icon.getMinV() : 0;
        double maxU = icon != null ? (double)icon.getMaxU() : 1;
        double maxV = icon != null ? (double)icon.getMaxV() : 1;

        // Random rotation based on position hash
        long hash = (long)(x * 3129871) ^ (long)z * 116129781L ^ (long)y;
        hash = hash * hash * 42317861L + hash * 11L;
        int rotation = (int)(hash >> 16 & 3L);

        t.setBrightness(block.getMixedBrightnessForBlock(this.blockAccess, x, y, z));

        float cx = (float)x + 0.5F;
        float cz = (float)z + 0.5F;
        float r1 = (float)(rotation & 1) * 0.5F * (float)(1 - rotation / 2 % 2 * 2);
        float r2 = (float)(rotation + 1 & 1) * 0.5F * (float)(1 - (rotation + 1) / 2 % 2 * 2);

        t.setColorOpaque_I(block.getBlockColor());
        t.addVertexWithUV((double)(cx + r1 - r2), (double)((float)y + yOff), (double)(cz + r1 + r2), minU, minV);
        t.addVertexWithUV((double)(cx + r1 + r2), (double)((float)y + yOff), (double)(cz - r1 + r2), maxU, minV);
        t.addVertexWithUV((double)(cx - r1 + r2), (double)((float)y + yOff), (double)(cz - r1 - r2), maxU, maxV);
        t.addVertexWithUV((double)(cx - r1 - r2), (double)((float)y + yOff), (double)(cz + r1 - r2), minU, maxV);

        // Back face (darker)
        t.setColorOpaque_I((block.getBlockColor() & 16711422) >> 1);
        t.addVertexWithUV((double)(cx - r1 - r2), (double)((float)y + yOff), (double)(cz + r1 - r2), minU, maxV);
        t.addVertexWithUV((double)(cx - r1 + r2), (double)((float)y + yOff), (double)(cz - r1 - r2), maxU, maxV);
        t.addVertexWithUV((double)(cx + r1 + r2), (double)((float)y + yOff), (double)(cz - r1 + r2), maxU, minV);
        t.addVertexWithUV((double)(cx + r1 - r2), (double)((float)y + yOff), (double)(cz + r1 + r2), minU, minV);

        return true;
    }

    // --- renderBlockCauldron ---
    // Cauldron outer shell + inner walls + water level.
    public boolean renderBlockCauldron(BlockCauldron block, int x, int y, int z) {
        this.renderStandardBlock(block, x, y, z);
        Tessellator t = Tessellator.instance;
        t.setBrightness(block.getMixedBrightnessForBlock(this.blockAccess, x, y, z));
        t.setColorOpaque_F(1.0F, 1.0F, 1.0F);

        Icon sideIcon = block.getBlockTextureFromSide(2);
        float inset = 0.124F;
        // Inner walls
        this.renderFaceXPos(block, (double)((float)x - 1.0F + inset), (double)y, (double)z, sideIcon);
        this.renderFaceXNeg(block, (double)((float)x + 1.0F - inset), (double)y, (double)z, sideIcon);
        this.renderFaceZPos(block, (double)x, (double)y, (double)((float)z - 1.0F + inset), sideIcon);
        this.renderFaceZNeg(block, (double)x, (double)y, (double)((float)z + 1.0F - inset), sideIcon);

        // Inner bottom
        Icon innerIcon = block.getIcon(1, 0); // Use top icon for inner bottom
        this.renderFaceYPos(block, (double)x, (double)((float)y - 1.0F + 0.25F), (double)z, innerIcon);
        this.renderFaceYNeg(block, (double)x, (double)((float)y + 1.0F - 0.75F), (double)z, innerIcon);

        // Water level based on metadata
        int meta = this.blockAccess.getBlockMetadata(x, y, z);
        if (meta > 0) {
            Icon waterIcon = this.getBlockIconFromSide(block, 1); // Use block's own top icon as fallback
            if (meta > 3) { meta = 3; }
            this.renderFaceYPos(block, (double)x, (double)((float)y - 1.0F + (6.0F + (float)meta * 3.0F) / 16.0F), (double)z, waterIcon);
        }

        return true;
    }

    // --- renderBlockRepeater ---
    // Flat base slab + torch models on top.
    public boolean renderBlockRepeater(BlockRedstoneRepeater block, int x, int y, int z) {
        // Render as a flat slab on the bottom
        this.setRenderBounds(0.0D, 0.0D, 0.0D, 1.0D, 0.125D, 1.0D);
        this.renderStandardBlock(block, x, y, z);
        this.setRenderBounds(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
        return true;
    }

    // --- renderBlockRedstoneWire ---
    // Wire flat on ground with cross/line patterns.
    public boolean renderBlockRedstoneWire(Block block, int x, int y, int z) {
        // Render as a flat cross pattern on the ground
        Tessellator t = Tessellator.instance;
        t.setBrightness(block.getMixedBrightnessForBlock(this.blockAccess, x, y, z));
        int meta = this.blockAccess.getBlockMetadata(x, y, z);
        float power = (float)meta / 15.0F;
        float r = power * 0.6F + 0.4F;
        if (meta == 0) { r = 0.3F; }
        float g = Math.max(0.0F, power * power * 0.7F - 0.5F);
        float b = Math.max(0.0F, power * power * 0.6F - 0.7F);
        t.setColorOpaque_F(r, g, b);

        Icon icon = this.getBlockIconFromSideAndMetadata(block, 1, meta);
        t.setCurrentTextureName(icon != null ? icon.getIconName() : null);
        double minU = icon != null ? (double)icon.getMinU() : 0;
        double maxU = icon != null ? (double)icon.getMaxU() : 1;
        double minV = icon != null ? (double)icon.getMinV() : 0;
        double maxV = icon != null ? (double)icon.getMaxV() : 1;

        double yPos = (double)y + 0.015625D;

        t.addVertexWithUV((double)(x + 1), yPos, (double)(z + 1), maxU, maxV);
        t.addVertexWithUV((double)(x + 1), yPos, (double)(z + 0), maxU, minV);
        t.addVertexWithUV((double)(x + 0), yPos, (double)(z + 0), minU, minV);
        t.addVertexWithUV((double)(x + 0), yPos, (double)(z + 1), minU, maxV);

        return true;
    }

    // 1.5.2 RenderBlocks.renderBlockMinecartTrack — flat rail plane at y+1/16 (raised ends on
    // slopes) with metadata-based UV rotation, double-sided; dispatched from renderBlockByRenderType
    // case 9 for FCBlockDetectorRail (wood/soulforged-steel detector rails).
    public boolean renderBlockMinecartTrack(BlockRailBase block, int x, int y, int z) {
        Tessellator t = Tessellator.instance;
        int meta = this.blockAccess.getBlockMetadata(x, y, z);
        Icon icon = this.getBlockIconFromSideAndMetadata(block, 0, meta);

        if (this.hasOverrideBlockTexture()) {
            icon = this.overrideBlockTexture;
        }

        if (block.isPowered()) {
            meta &= 7;
        }

        t.setCurrentTextureName(icon != null ? icon.getIconName() : null);
        t.setBrightness(block.getMixedBrightnessForBlock(this.blockAccess, x, y, z));
        t.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        double minU = icon != null ? (double)icon.getMinU() : 0;
        double minV = icon != null ? (double)icon.getMinV() : 0;
        double maxU = icon != null ? (double)icon.getMaxU() : 1;
        double maxV = icon != null ? (double)icon.getMaxV() : 1;
        double railHeight = 0.0625D;
        double x1 = (double)(x + 1);
        double x2 = (double)(x + 1);
        double x3 = (double)(x + 0);
        double x4 = (double)(x + 0);
        double z1 = (double)(z + 0);
        double z2 = (double)(z + 1);
        double z3 = (double)(z + 1);
        double z4 = (double)(z + 0);
        double y1 = (double)y + railHeight;
        double y2 = (double)y + railHeight;
        double y3 = (double)y + railHeight;
        double y4 = (double)y + railHeight;

        if (meta != 1 && meta != 2 && meta != 3 && meta != 7) {
            if (meta == 8) {
                x1 = x2 = (double)(x + 0);
                x3 = x4 = (double)(x + 1);
                z1 = z4 = (double)(z + 1);
                z2 = z3 = (double)(z + 0);
            } else if (meta == 9) {
                x1 = x4 = (double)(x + 0);
                x2 = x3 = (double)(x + 1);
                z1 = z2 = (double)(z + 0);
                z3 = z4 = (double)(z + 1);
            }
        } else {
            x1 = x4 = (double)(x + 1);
            x2 = x3 = (double)(x + 0);
            z1 = z2 = (double)(z + 1);
            z3 = z4 = (double)(z + 0);
        }

        if (meta != 2 && meta != 4) {
            if (meta == 3 || meta == 5) {
                ++y2;
                ++y3;
            }
        } else {
            ++y1;
            ++y4;
        }

        t.addVertexWithUV(x1, y1, z1, maxU, minV);
        t.addVertexWithUV(x2, y2, z2, maxU, maxV);
        t.addVertexWithUV(x3, y3, z3, minU, maxV);
        t.addVertexWithUV(x4, y4, z4, minU, minV);
        t.addVertexWithUV(x4, y4, z4, minU, minV);
        t.addVertexWithUV(x3, y3, z3, minU, maxV);
        t.addVertexWithUV(x2, y2, z2, maxU, maxV);
        t.addVertexWithUV(x1, y1, z1, maxU, minV);
        return true;
    }

    public boolean renderBlockLadder(Block block, int x, int y, int z) {
        // Ladder renders as a flat plane on one face based on metadata
        int meta = blockAccess != null ? blockAccess.getBlockMetadata(x, y, z) : 0;
        Tessellator t = Tessellator.instance;
        Icon icon = getBlockIconFromSideAndMetadata(block, 0, meta);
        t.setCurrentTextureName(icon != null ? icon.getIconName() : null);
        double minU = icon != null ? icon.getMinU() : 0;
        double maxU = icon != null ? icon.getMaxU() : 1;
        double minV = icon != null ? icon.getMinV() : 0;
        double maxV = icon != null ? icon.getMaxV() : 1;
        float d = 0.0625F; // 1 pixel offset from wall

        // meta: 2=north, 3=south, 4=west, 5=east
        t.setNormal(0, 0, 1);
        switch (meta) {
            case 2: // north face
                t.addVertexWithUV(x + 1, y + 1, z + d, minU, minV);
                t.addVertexWithUV(x + 1, y + 0, z + d, minU, maxV);
                t.addVertexWithUV(x + 0, y + 0, z + d, maxU, maxV);
                t.addVertexWithUV(x + 0, y + 1, z + d, maxU, minV);
                break;
            case 3: // south face
                t.addVertexWithUV(x + 0, y + 1, z + 1 - d, minU, minV);
                t.addVertexWithUV(x + 0, y + 0, z + 1 - d, minU, maxV);
                t.addVertexWithUV(x + 1, y + 0, z + 1 - d, maxU, maxV);
                t.addVertexWithUV(x + 1, y + 1, z + 1 - d, maxU, minV);
                break;
            case 4: // west face
                t.addVertexWithUV(x + d, y + 1, z + 0, minU, minV);
                t.addVertexWithUV(x + d, y + 0, z + 0, minU, maxV);
                t.addVertexWithUV(x + d, y + 0, z + 1, maxU, maxV);
                t.addVertexWithUV(x + d, y + 1, z + 1, maxU, minV);
                break;
            case 5: // east face
                t.addVertexWithUV(x + 1 - d, y + 1, z + 1, minU, minV);
                t.addVertexWithUV(x + 1 - d, y + 0, z + 1, minU, maxV);
                t.addVertexWithUV(x + 1 - d, y + 0, z + 0, maxU, maxV);
                t.addVertexWithUV(x + 1 - d, y + 1, z + 0, maxU, minV);
                break;
            default:
                return renderStandardBlock(block, x, y, z);
        }
        return true;
    }

    // --- renderBlockTripWireSource / renderBlockTripWire ---
    // Simplified rendering as small boxes.
    public boolean renderBlockTripWireSource(Block block, int x, int y, int z) {
        // Render tripwire hook as a small box on the wall
        this.setRenderBounds(0.25D, 0.25D, 0.25D, 0.75D, 0.75D, 0.75D);
        this.renderStandardBlock(block, x, y, z);
        this.setRenderBounds(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
        return true;
    }

    public boolean renderBlockTripWire(Block block, int x, int y, int z) {
        // Render tripwire string as a thin flat line
        Tessellator t = Tessellator.instance;
        Icon icon = this.getBlockIconFromSide(block, 0);
        t.setCurrentTextureName(icon != null ? icon.getIconName() : null);
        t.setBrightness(block.getMixedBrightnessForBlock(this.blockAccess, x, y, z));
        t.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        double minU = icon != null ? (double)icon.getMinU() : 0;
        double maxU = icon != null ? (double)icon.getMaxU() : 1;
        double minV = icon != null ? (double)icon.getMinV() : 0;
        double maxV = icon != null ? (double)icon.getMaxV() : 1;

        double yPos = y + 0.0625D;
        t.addVertexWithUV((double)x, yPos, (double)(z + 1), minU, maxV);
        t.addVertexWithUV((double)(x + 1), yPos, (double)(z + 1), maxU, maxV);
        t.addVertexWithUV((double)(x + 1), yPos, (double)z, maxU, minV);
        t.addVertexWithUV((double)x, yPos, (double)z, minU, minV);

        return true;
    }

    // --- renderBlockDragonEgg ---
    // Dragon egg rendered as a series of inset horizontal slices.
    public boolean renderBlockDragonEgg(BlockDragonEgg block, int x, int y, int z) {
        boolean rendered = false;
        this.renderAllFaces = true;

        for (int slice = 0; slice < 8; ++slice) {
            double offset;
            double height;

            // Each slice has a different inset
            switch (slice) {
                case 0: offset = 0.3125D; height = 0.0D; break;
                case 1: offset = 0.1875D; height = 0.125D; break;
                case 2: offset = 0.125D; height = 0.25D; break;
                case 3: offset = 0.0625D; height = 0.375D; break;
                case 4: offset = 0.0625D; height = 0.5D; break;
                case 5: offset = 0.125D; height = 0.625D; break;
                case 6: offset = 0.1875D; height = 0.75D; break;
                case 7: offset = 0.25D; height = 0.875D; break;
                default: offset = 0.3125D; height = 0.0D;
            }

            this.setRenderBounds(offset, height, offset, 1.0D - offset, height + 0.125D, 1.0D - offset);
            this.renderStandardBlock(block, x, y, z);
            rendered = true;
        }

        this.renderAllFaces = false;
        this.setRenderBounds(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
        return rendered;
    }

    // --- RenderBlockCocoa ---
    // Cocoa pod at growth stages rendered as small boxes.
    public boolean RenderBlockCocoa(BlockCocoa block, int x, int y, int z) {
        int meta = this.blockAccess.getBlockMetadata(x, y, z);
        int stage = (meta >> 2) & 3; // growth stage 0-2
        int facing = meta & 3; // direction

        // Size based on growth stage
        double halfW, halfH;
        double yOff;
        if (stage == 0) { halfW = 0.125D; halfH = 0.1875D; yOff = 0.5D; }
        else if (stage == 1) { halfW = 0.1875D; halfH = 0.25D; yOff = 0.4375D; }
        else { halfW = 0.25D; halfH = 0.3125D; yOff = 0.375D; }

        double cx = x + 0.5D;
        double cy = y + yOff;
        double cz = z + 0.5D;

        // Offset from trunk based on facing
        double offsetX = 0, offsetZ = 0;
        if (facing == 0) { offsetZ = -0.25D; }
        else if (facing == 1) { offsetX = 0.25D; }
        else if (facing == 2) { offsetZ = 0.25D; }
        else if (facing == 3) { offsetX = -0.25D; }

        this.renderAllFaces = true;
        this.setRenderBounds(cx + offsetX - halfW - x, cy - halfH - y, cz + offsetZ - halfW - z,
                             cx + offsetX + halfW - x, cy + halfH - y, cz + offsetZ + halfW - z);
        this.renderStandardBlock(block, x, y, z);
        this.renderAllFaces = false;
        this.setRenderBounds(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
        return true;
    }

    // --- RenderBlockEndPortalFrame ---
    // End portal frame with eye slot.
    public boolean RenderBlockEndPortalFrame(BlockEndPortalFrame block, int x, int y, int z) {
        // Base slab
        this.setRenderBounds(0.0D, 0.0D, 0.0D, 1.0D, 0.8125D, 1.0D);
        this.renderStandardBlock(block, x, y, z);

        // Eye on top if present
        int meta = this.blockAccess.getBlockMetadata(x, y, z);
        if ((meta & 4) != 0) {
            this.setRenderBounds(0.25D, 0.8125D, 0.25D, 0.75D, 1.0D, 0.75D);
            this.renderStandardBlock(block, x, y, z);
        }

        this.setRenderBounds(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
        return true;
    }

    // --- RenderBlockBeacon ---
    // Beacon base - glass-like block, render as standard cube.
    public boolean RenderBlockBeacon(BlockBeacon block, int x, int y, int z) {
        this.renderStandardBlock(block, x, y, z);
        return true;
    }

    // --- RenderBlockBed ---
    // Bed with head/foot halves.
    public boolean RenderBlockBed(Block block, int x, int y, int z) {
        int meta = this.blockAccess.getBlockMetadata(x, y, z);
        boolean isHead = (meta & 8) != 0;

        // Bed is 9/16 tall
        this.setRenderBounds(0.0D, 0.0D, 0.0D, 1.0D, 0.5625D, 1.0D);
        this.renderStandardBlock(block, x, y, z);
        this.setRenderBounds(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
        return true;
    }

    // --- RenderBlockBrewingStand ---
    // Brewing stand with rods - simplified as a thin center post + base.
    public boolean RenderBlockBrewingStand(BlockBrewingStand block, int x, int y, int z) {
        this.renderAllFaces = true;

        // Base
        this.setRenderBounds(0.0D, 0.0D, 0.0D, 1.0D, 0.125D, 1.0D);
        this.renderStandardBlock(block, x, y, z);

        // Center rod
        this.setRenderBounds(0.4375D, 0.0D, 0.4375D, 0.5625D, 0.875D, 0.5625D);
        this.renderStandardBlock(block, x, y, z);

        this.renderAllFaces = false;
        this.setRenderBounds(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
        return true;
    }

    // --- RenderBlockFlowerpot ---
    // Flower pot outer shell + inner walls + dirt + contained plant.
    public boolean RenderBlockFlowerpot(BlockFlowerPot block, int x, int y, int z) {
        this.renderStandardBlock(block, x, y, z);
        Tessellator t = Tessellator.instance;
        t.setBrightness(block.getMixedBrightnessForBlock(this.blockAccess, x, y, z));
        t.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        Icon potIcon = this.getBlockIconFromSide(block, 0);
        float inset = 0.1865F;
        this.renderFaceXPos(block, (double)((float)x - 0.5F + inset), (double)y, (double)z, potIcon);
        this.renderFaceXNeg(block, (double)((float)x + 0.5F - inset), (double)y, (double)z, potIcon);
        this.renderFaceZPos(block, (double)x, (double)y, (double)((float)z - 0.5F + inset), potIcon);
        this.renderFaceZNeg(block, (double)x, (double)y, (double)((float)z + 0.5F - inset), potIcon);
        this.renderFaceYPos(block, (double)x, (double)((float)y - 0.5F + inset + 0.1875F), (double)z, this.getBlockIcon(Block.dirt));
        return true;
    }

    // --- renderBlockAnvilMetadata ---
    // Anvil with 4 stacked sub-boxes and rotation.
    public boolean renderBlockAnvilMetadata(BlockAnvil block, int x, int y, int z, int metadata) {
        Tessellator t = Tessellator.instance;
        t.setBrightness(block.getMixedBrightnessForBlock(this.blockAccess, x, y, z));
        t.setColorOpaque_F(1.0F, 1.0F, 1.0F);

        int dir = metadata & 3;
        boolean rotated = (dir == 1 || dir == 3);

        // Set UV rotations based on direction
        switch (dir) {
            case 0:
                this.uvRotateSouth = 2;
                this.uvRotateNorth = 1;
                this.uvRotateTop = 3;
                this.uvRotateBottom = 3;
                break;
            case 1:
                this.uvRotateEast = 1;
                this.uvRotateWest = 2;
                this.uvRotateTop = 2;
                this.uvRotateBottom = 1;
                break;
            case 2:
                this.uvRotateSouth = 1;
                this.uvRotateNorth = 2;
                break;
            case 3:
                this.uvRotateEast = 2;
                this.uvRotateWest = 1;
                this.uvRotateTop = 1;
                this.uvRotateBottom = 2;
                break;
        }

        // 4 sections of the anvil (bottom to top)
        renderAnvilSection(block, x, y, z, 0, 0.0F, 0.75F, 0.25F, 0.75F, rotated, metadata);
        renderAnvilSection(block, x, y, z, 1, 0.25F, 0.5F, 0.0625F, 0.625F, rotated, metadata);
        renderAnvilSection(block, x, y, z, 2, 0.3125F, 0.25F, 0.3125F, 0.5F, rotated, metadata);
        renderAnvilSection(block, x, y, z, 3, 0.625F, 0.625F, 0.375F, 1.0F, rotated, metadata);

        this.setRenderBounds(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
        this.uvRotateEast = 0;
        this.uvRotateWest = 0;
        this.uvRotateSouth = 0;
        this.uvRotateNorth = 0;
        this.uvRotateTop = 0;
        this.uvRotateBottom = 0;
        return true;
    }

    private void renderAnvilSection(Block block, int x, int y, int z,
                                    int section, float yBase, float width, float height, float depth,
                                    boolean rotated, int metadata) {
        if (rotated) {
            float tmp = width;
            width = depth;
            depth = tmp;
        }
        width /= 2.0F;
        depth /= 2.0F;
        this.setRenderBounds((double)(0.5F - width), (double)yBase, (double)(0.5F - depth),
                             (double)(0.5F + width), (double)(yBase + height), (double)(0.5F + depth));
        this.renderStandardBlock(block, x, y, z);
    }

    // --- RenderBlockAnvil (FC variant that delegates) ---
    public boolean RenderBlockAnvil(BlockAnvil block, int x, int y, int z) {
        return renderBlockAnvilMetadata(block, x, y, z, this.blockAccess.getBlockMetadata(x, y, z));
    }

    // --- FC additions: specific block type renders ---

    public boolean RenderBlockRedstoneLogic(BlockRedstoneLogic block, int x, int y, int z) { return false; }
    public boolean RenderBlockHopper(Block block, int x, int y, int z) { return false; }

    // --- renderBlockTorch ---
    // 1.5.2 RenderBlocks.renderBlockTorch (FCMOD-patched) — render type 2, FCBlockTorchBase
    // subclasses; wall orientations 1-4 lean out of the wall via renderTorchAtAngle.
    public boolean renderBlockTorch(Block block, int x, int y, int z) {
        int meta = blockAccess != null ? blockAccess.getBlockMetadata(x, y, z) : 0;
        // FCMOD: orientation from FCBlockTorchBase.GetOrientation(meta) == meta & 7
        int orientation = meta & 7;
        Tessellator t = Tessellator.instance;
        t.setBrightness(block.getMixedBrightnessForBlock(this.blockAccess, x, y, z));
        t.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        double torchLean = 0.4000000059604645D;
        double wallOffset = 0.5D - torchLean;
        double wallHeight = 0.20000000298023224D;

        if (orientation == 1) {
            this.renderTorchAtAngle(block, (double)x - wallOffset, (double)y + wallHeight, (double)z, -torchLean, 0.0D, meta);
        } else if (orientation == 2) {
            this.renderTorchAtAngle(block, (double)x + wallOffset, (double)y + wallHeight, (double)z, torchLean, 0.0D, meta);
        } else if (orientation == 3) {
            this.renderTorchAtAngle(block, (double)x, (double)y + wallHeight, (double)z - wallOffset, 0.0D, -torchLean, meta);
        } else if (orientation == 4) {
            this.renderTorchAtAngle(block, (double)x, (double)y + wallHeight, (double)z + wallOffset, 0.0D, torchLean, meta);
        } else {
            this.renderTorchAtAngle(block, (double)x, (double)y, (double)z, 0.0D, 0.0D, meta);
        }

        return true;
    }

    // --- renderCrossedSquares ---
    public boolean renderCrossedSquares(Block block, int x, int y, int z) {
        int meta = blockAccess != null ? blockAccess.getBlockMetadata(x, y, z) : 0;
        Tessellator t = Tessellator.instance;
        Icon icon = getBlockIconFromSideAndMetadata(block, 0, meta);
        t.setCurrentTextureName(icon != null ? icon.getIconName() : null);
        double minU = icon != null ? icon.getMinU() : 0;
        double maxU = icon != null ? icon.getMaxU() : 1;
        double minV = icon != null ? icon.getMinV() : 0;
        double maxV = icon != null ? icon.getMaxV() : 1;
        double d = 0.45D;
        // Diagonal plane 1: SW to NE
        t.setNormal(0, 1, 0);   // not the planes' own normal: fc_quad_normals (FCBakedModel) bakes them unshaded, flat-lit
        t.addVertexWithUV(x + 0.5 - d, y + 0, z + 0.5 - d, minU, maxV);
        t.addVertexWithUV(x + 0.5 - d, y + 1, z + 0.5 - d, minU, minV);
        t.addVertexWithUV(x + 0.5 + d, y + 1, z + 0.5 + d, maxU, minV);
        t.addVertexWithUV(x + 0.5 + d, y + 0, z + 0.5 + d, maxU, maxV);
        // Back face
        t.addVertexWithUV(x + 0.5 + d, y + 0, z + 0.5 + d, minU, maxV);
        t.addVertexWithUV(x + 0.5 + d, y + 1, z + 0.5 + d, minU, minV);
        t.addVertexWithUV(x + 0.5 - d, y + 1, z + 0.5 - d, maxU, minV);
        t.addVertexWithUV(x + 0.5 - d, y + 0, z + 0.5 - d, maxU, maxV);
        // Diagonal plane 2: NW to SE
        t.addVertexWithUV(x + 0.5 - d, y + 0, z + 0.5 + d, minU, maxV);
        t.addVertexWithUV(x + 0.5 - d, y + 1, z + 0.5 + d, minU, minV);
        t.addVertexWithUV(x + 0.5 + d, y + 1, z + 0.5 - d, maxU, minV);
        t.addVertexWithUV(x + 0.5 + d, y + 0, z + 0.5 - d, maxU, maxV);
        // Back face
        t.addVertexWithUV(x + 0.5 + d, y + 0, z + 0.5 - d, minU, maxV);
        t.addVertexWithUV(x + 0.5 + d, y + 1, z + 0.5 - d, minU, minV);
        t.addVertexWithUV(x + 0.5 - d, y + 1, z + 0.5 + d, maxU, minV);
        t.addVertexWithUV(x + 0.5 - d, y + 0, z + 0.5 + d, maxU, maxV);
        return true;
    }

    // --- renderBlockCrops ---
    public boolean renderBlockCrops(Block block, int x, int y, int z) {
        // Crops use 4 planes arranged in a # pattern (not crossed)
        int meta = blockAccess != null ? blockAccess.getBlockMetadata(x, y, z) : 0;
        Tessellator t = Tessellator.instance;
        Icon icon = getBlockIconFromSideAndMetadata(block, 0, meta);
        t.setCurrentTextureName(icon != null ? icon.getIconName() : null);
        double minU = icon != null ? icon.getMinU() : 0;
        double maxU = icon != null ? icon.getMaxU() : 1;
        double minV = icon != null ? icon.getMinV() : 0;
        double maxV = icon != null ? icon.getMaxV() : 1;
        double o = 0.25; // offset from center
        t.setNormal(0, 1, 0);   // not the planes' own normal: fc_quad_normals (FCBakedModel) bakes them unshaded, flat-lit
        // 4 planes in # pattern
        for (int dir = 0; dir < 2; dir++) {
            double px = dir == 0 ? x + o : x + 0.5;
            double pz = dir == 0 ? z + 0.5 : z + o;
            double dx = dir == 0 ? 0 : 1;
            double dz = dir == 0 ? 1 : 0;
            // Front
            t.addVertexWithUV(px, y + 0, pz, minU, maxV);
            t.addVertexWithUV(px, y + 1, pz, minU, minV);
            t.addVertexWithUV(px + dx * 0.5, y + 1, pz + dz * 0.5, maxU, minV);
            t.addVertexWithUV(px + dx * 0.5, y + 0, pz + dz * 0.5, maxU, maxV);
            // Back
            t.addVertexWithUV(px + dx * 0.5, y + 0, pz + dz * 0.5, minU, maxV);
            t.addVertexWithUV(px + dx * 0.5, y + 1, pz + dz * 0.5, minU, minV);
            t.addVertexWithUV(px, y + 1, pz, maxU, minV);
            t.addVertexWithUV(px, y + 0, pz, maxU, maxV);
        }
        return true;
    }

    // --- FC additions: standard full block rendering for falling/piston-moved blocks ---

    // FCMOD-INTEGRATION(1.20.1): falling_blocks: 1.5.2's RenderStandardFallingBlock: the caller's bounds, its sides, its tint
    /**
     * 1.5.2 (FCMOD) RenderStandardFallingBlock (vanilla/client RenderBlocks.java:10563-10628): the six faces of the
     * current render bounds (Block.RenderFallingBlock sets the fixed block bounds; FCBlockSlabFalling and
     * FCBlockStairsFalling set their own), each only if ShouldSideBeRenderedOnFallingBlock allows it, tinted by
     * colorMultiplier. Capture adaptations, as in renderStandardBlockWithColorMultiplier: no per-face shade in the
     * colour (FCEntityRenderer shades a falling block's faces by their normals, the same 0.5/1/0.8/0.6) and no
     * setBrightness (the entity emitter lights by the capture's GL lightmap or MC's packed light, not per vertex).
     */
    public void RenderStandardFallingBlock(Block block, int x, int y, int z, int metadata) {
        this.enableAO = false;
        Tessellator t = Tessellator.instance;
        int mul = block.colorMultiplier(this.blockAccess, x, y, z);
        t.setColorOpaque_F((mul >> 16 & 255) / 255.0F, (mul >> 8 & 255) / 255.0F, (mul & 255) / 255.0F);
        if (block.ShouldSideBeRenderedOnFallingBlock(0, metadata)) renderFaceYNeg(block, x, y, z, getBlockIconFromSideAndMetadata(block, 0, metadata));
        if (block.ShouldSideBeRenderedOnFallingBlock(1, metadata)) renderFaceYPos(block, x, y, z, getBlockIconFromSideAndMetadata(block, 1, metadata));
        if (block.ShouldSideBeRenderedOnFallingBlock(2, metadata)) renderFaceZNeg(block, x, y, z, getBlockIconFromSideAndMetadata(block, 2, metadata));
        if (block.ShouldSideBeRenderedOnFallingBlock(3, metadata)) renderFaceZPos(block, x, y, z, getBlockIconFromSideAndMetadata(block, 3, metadata));
        if (block.ShouldSideBeRenderedOnFallingBlock(4, metadata)) renderFaceXNeg(block, x, y, z, getBlockIconFromSideAndMetadata(block, 4, metadata));
        if (block.ShouldSideBeRenderedOnFallingBlock(5, metadata)) renderFaceXPos(block, x, y, z, getBlockIconFromSideAndMetadata(block, 5, metadata));
    }

    /**
     * 1.5.2 (FCMOD) RenderStandardFullBlockMovedByPiston (RenderBlocks.java:10631-10643): a full block
     * pushed by a piston is drawn through renderStandardBlock with renderAllFaces set, because
     * RenderStandardFullBlock's faces skip the renderAllFaces test and would be culled by neighbours.
     */
    public void RenderStandardFullBlockMovedByPiston(Block block, int x, int y, int z) {
        this.SetRenderAllFaces(true);
        this.setRenderBoundsFromBlock(block);
        this.renderStandardBlock(block, x, y, z);
        this.SetRenderAllFaces(false);
    }

    // --- Torch / helper rendering ---

    // 1.5.2 RenderBlocks.renderTorchAtAngle — tilted, wall-offset torch geometry; called from
    // renderBlockTorch for FCBlockTorchBase wall orientations 1-4 (upright with 0 angles otherwise).
    public void renderTorchAtAngle(Block block, double x, double y, double z, double angleX, double angleZ, int metadata) {
        Tessellator t = Tessellator.instance;
        Icon icon = this.getBlockIconFromSideAndMetadata(block, 0, metadata);

        if (this.hasOverrideBlockTexture()) {
            icon = this.overrideBlockTexture;
        }

        t.setCurrentTextureName(icon != null ? icon.getIconName() : null);
        double minU = icon != null ? (double)icon.getMinU() : 0;
        double minV = icon != null ? (double)icon.getMinV() : 0;
        double maxU = icon != null ? (double)icon.getMaxU() : 1;
        double maxV = icon != null ? (double)icon.getMaxV() : 1;
        double topMinU = icon != null ? (double)icon.getInterpolatedU(7.0D) : 0.4375;
        double topMinV = icon != null ? (double)icon.getInterpolatedV(6.0D) : 0.375;
        double topMaxU = icon != null ? (double)icon.getInterpolatedU(9.0D) : 0.5625;
        double topMaxV = icon != null ? (double)icon.getInterpolatedV(8.0D) : 0.5;
        double bottomMinU = icon != null ? (double)icon.getInterpolatedU(7.0D) : 0.4375;
        double bottomMinV = icon != null ? (double)icon.getInterpolatedV(13.0D) : 0.8125;
        double bottomMaxU = icon != null ? (double)icon.getInterpolatedU(9.0D) : 0.5625;
        double bottomMaxV = icon != null ? (double)icon.getInterpolatedV(15.0D) : 0.9375;
        x += 0.5D;
        z += 0.5D;
        double xNeg = x - 0.5D;
        double xPos = x + 0.5D;
        double zNeg = z - 0.5D;
        double zPos = z + 0.5D;
        double halfWidth = 0.0625D;
        double topHeight = 0.625D;
        // Top face of the torch head
        t.addVertexWithUV(x + angleX * (1.0D - topHeight) - halfWidth, y + topHeight, z + angleZ * (1.0D - topHeight) - halfWidth, topMinU, topMinV);
        t.addVertexWithUV(x + angleX * (1.0D - topHeight) - halfWidth, y + topHeight, z + angleZ * (1.0D - topHeight) + halfWidth, topMinU, topMaxV);
        t.addVertexWithUV(x + angleX * (1.0D - topHeight) + halfWidth, y + topHeight, z + angleZ * (1.0D - topHeight) + halfWidth, topMaxU, topMaxV);
        t.addVertexWithUV(x + angleX * (1.0D - topHeight) + halfWidth, y + topHeight, z + angleZ * (1.0D - topHeight) - halfWidth, topMaxU, topMinV);
        // Bottom face of the torch stick
        t.addVertexWithUV(x + halfWidth + angleX, y, z - halfWidth + angleZ, bottomMaxU, bottomMinV);
        t.addVertexWithUV(x + halfWidth + angleX, y, z + halfWidth + angleZ, bottomMaxU, bottomMaxV);
        t.addVertexWithUV(x - halfWidth + angleX, y, z + halfWidth + angleZ, bottomMinU, bottomMaxV);
        t.addVertexWithUV(x - halfWidth + angleX, y, z - halfWidth + angleZ, bottomMinU, bottomMinV);
        // Four full-height side planes, sheared by the lean angles at the base
        t.addVertexWithUV(x - halfWidth, y + 1.0D, zNeg, minU, minV);
        t.addVertexWithUV(x - halfWidth + angleX, y + 0.0D, zNeg + angleZ, minU, maxV);
        t.addVertexWithUV(x - halfWidth + angleX, y + 0.0D, zPos + angleZ, maxU, maxV);
        t.addVertexWithUV(x - halfWidth, y + 1.0D, zPos, maxU, minV);
        t.addVertexWithUV(x + halfWidth, y + 1.0D, zPos, minU, minV);
        t.addVertexWithUV(x + angleX + halfWidth, y + 0.0D, zPos + angleZ, minU, maxV);
        t.addVertexWithUV(x + angleX + halfWidth, y + 0.0D, zNeg + angleZ, maxU, maxV);
        t.addVertexWithUV(x + halfWidth, y + 1.0D, zNeg, maxU, minV);
        t.addVertexWithUV(xNeg, y + 1.0D, z + halfWidth, minU, minV);
        t.addVertexWithUV(xNeg + angleX, y + 0.0D, z + halfWidth + angleZ, minU, maxV);
        t.addVertexWithUV(xPos + angleX, y + 0.0D, z + halfWidth + angleZ, maxU, maxV);
        t.addVertexWithUV(xPos, y + 1.0D, z + halfWidth, maxU, minV);
        t.addVertexWithUV(xPos, y + 1.0D, z - halfWidth, minU, minV);
        t.addVertexWithUV(xPos + angleX, y + 0.0D, z - halfWidth + angleZ, minU, maxV);
        t.addVertexWithUV(xNeg + angleX, y + 0.0D, z - halfWidth + angleZ, maxU, maxV);
        t.addVertexWithUV(xNeg, y + 1.0D, z - halfWidth, maxU, minV);
    }

    /**
     * 1.5.2 RenderBlocks.drawCrossedSquares (RenderBlocks.java:3712-3747): the two crossed planes of a
     * plant, each drawn in both windings, 0.45 * scale out from the cell centre and scale high. The
     * last parameter is that scale (1.0F for items). Called by renderBlockAsItemVanilla's
     * render-type-1 branch; world plants go through renderCrossedSquares.
     */
    public void drawCrossedSquares(Block block, int metadata, double x, double y, double z, float scale) {
        Tessellator t = Tessellator.instance;
        Icon icon = this.getBlockIconFromSideAndMetadata(block, 0, metadata);
        t.setCurrentTextureName(icon != null ? icon.getIconName() : null);
        double minU = icon != null ? icon.getMinU() : 0;
        double minV = icon != null ? icon.getMinV() : 0;
        double maxU = icon != null ? icon.getMaxU() : 1;
        double maxV = icon != null ? icon.getMaxV() : 1;
        double halfWidth = 0.45D * (double) scale;
        double minX = x + 0.5D - halfWidth;
        double maxX = x + 0.5D + halfWidth;
        double minZ = z + 0.5D - halfWidth;
        double maxZ = z + 0.5D + halfWidth;
        t.addVertexWithUV(minX, y + (double) scale, minZ, minU, minV);
        t.addVertexWithUV(minX, y + 0.0D, minZ, minU, maxV);
        t.addVertexWithUV(maxX, y + 0.0D, maxZ, maxU, maxV);
        t.addVertexWithUV(maxX, y + (double) scale, maxZ, maxU, minV);
        t.addVertexWithUV(maxX, y + (double) scale, maxZ, minU, minV);
        t.addVertexWithUV(maxX, y + 0.0D, maxZ, minU, maxV);
        t.addVertexWithUV(minX, y + 0.0D, minZ, maxU, maxV);
        t.addVertexWithUV(minX, y + (double) scale, minZ, maxU, minV);
        t.addVertexWithUV(minX, y + (double) scale, maxZ, minU, minV);
        t.addVertexWithUV(minX, y + 0.0D, maxZ, minU, maxV);
        t.addVertexWithUV(maxX, y + 0.0D, minZ, maxU, maxV);
        t.addVertexWithUV(maxX, y + (double) scale, minZ, maxU, minV);
        t.addVertexWithUV(maxX, y + (double) scale, minZ, minU, minV);
        t.addVertexWithUV(maxX, y + 0.0D, minZ, minU, maxV);
        t.addVertexWithUV(minX, y + 0.0D, maxZ, maxU, maxV);
        t.addVertexWithUV(minX, y + (double) scale, maxZ, maxU, minV);
    }

    /**
     * 1.5.2 RenderBlocks.renderBlockStemSmall (RenderBlocks.java:3752-3784): a stem's crossed planes,
     * 0.45 out from the cell centre and {@code height} high, the icon cut off at that height. The
     * height comes FIRST, as in 1.5.2 (the old stub and the btw.api placeholder name the parameters
     * x, y, z, height, but only the positions matter in the descriptor). Called by
     * renderBlockAsItemVanilla's render-type-19 branch.
     */
    public void renderBlockStemSmall(Block block, int metadata, double height, double x, double y, double z) {
        Tessellator t = Tessellator.instance;
        Icon icon = this.getBlockIconFromSideAndMetadata(block, 0, metadata);
        t.setCurrentTextureName(icon != null ? icon.getIconName() : null);
        double minU = icon != null ? icon.getMinU() : 0;
        double minV = icon != null ? icon.getMinV() : 0;
        double maxU = icon != null ? icon.getMaxU() : 1;
        double cutV = icon != null ? icon.getInterpolatedV(height * 16.0D) : height;
        double minX = x + 0.5D - 0.44999998807907104D;
        double maxX = x + 0.5D + 0.44999998807907104D;
        double minZ = z + 0.5D - 0.44999998807907104D;
        double maxZ = z + 0.5D + 0.44999998807907104D;
        t.addVertexWithUV(minX, y + height, minZ, minU, minV);
        t.addVertexWithUV(minX, y + 0.0D, minZ, minU, cutV);
        t.addVertexWithUV(maxX, y + 0.0D, maxZ, maxU, cutV);
        t.addVertexWithUV(maxX, y + height, maxZ, maxU, minV);
        t.addVertexWithUV(maxX, y + height, maxZ, minU, minV);
        t.addVertexWithUV(maxX, y + 0.0D, maxZ, minU, cutV);
        t.addVertexWithUV(minX, y + 0.0D, minZ, maxU, cutV);
        t.addVertexWithUV(minX, y + height, minZ, maxU, minV);
        t.addVertexWithUV(minX, y + height, maxZ, minU, minV);
        t.addVertexWithUV(minX, y + 0.0D, maxZ, minU, cutV);
        t.addVertexWithUV(maxX, y + 0.0D, minZ, maxU, cutV);
        t.addVertexWithUV(maxX, y + height, minZ, maxU, minV);
        t.addVertexWithUV(maxX, y + height, minZ, minU, minV);
        t.addVertexWithUV(maxX, y + 0.0D, minZ, minU, cutV);
        t.addVertexWithUV(minX, y + 0.0D, maxZ, maxU, cutV);
        t.addVertexWithUV(minX, y + height, maxZ, maxU, minV);
    }

    public void renderBlockStemBig(BlockStem block, int metadata, int connectedBlockId, double x, double y, double z, double height) {}

    /**
     * 1.5.2 RenderBlocks.renderBlockCropsImpl (RenderBlocks.java:3885-3936): the crop "#", two
     * full-height planes along Z at x+0.25/x+0.75 and two along X at z+0.25/z+0.75, each drawn in
     * both windings. Called by renderBlockAsItemVanilla's render-type-6 branch.
     */
    public void renderBlockCropsImpl(Block block, int metadata, double x, double y, double z) {
        Tessellator t = Tessellator.instance;
        Icon icon = this.getBlockIconFromSideAndMetadata(block, 0, metadata);
        t.setCurrentTextureName(icon != null ? icon.getIconName() : null);
        double minU = icon != null ? icon.getMinU() : 0;
        double minV = icon != null ? icon.getMinV() : 0;
        double maxU = icon != null ? icon.getMaxU() : 1;
        double maxV = icon != null ? icon.getMaxV() : 1;
        double minX = x + 0.5D - 0.25D;
        double maxX = x + 0.5D + 0.25D;
        double minZ = z + 0.5D - 0.5D;
        double maxZ = z + 0.5D + 0.5D;
        t.addVertexWithUV(minX, y + 1.0D, minZ, minU, minV);
        t.addVertexWithUV(minX, y + 0.0D, minZ, minU, maxV);
        t.addVertexWithUV(minX, y + 0.0D, maxZ, maxU, maxV);
        t.addVertexWithUV(minX, y + 1.0D, maxZ, maxU, minV);
        t.addVertexWithUV(minX, y + 1.0D, maxZ, minU, minV);
        t.addVertexWithUV(minX, y + 0.0D, maxZ, minU, maxV);
        t.addVertexWithUV(minX, y + 0.0D, minZ, maxU, maxV);
        t.addVertexWithUV(minX, y + 1.0D, minZ, maxU, minV);
        t.addVertexWithUV(maxX, y + 1.0D, maxZ, minU, minV);
        t.addVertexWithUV(maxX, y + 0.0D, maxZ, minU, maxV);
        t.addVertexWithUV(maxX, y + 0.0D, minZ, maxU, maxV);
        t.addVertexWithUV(maxX, y + 1.0D, minZ, maxU, minV);
        t.addVertexWithUV(maxX, y + 1.0D, minZ, minU, minV);
        t.addVertexWithUV(maxX, y + 0.0D, minZ, minU, maxV);
        t.addVertexWithUV(maxX, y + 0.0D, maxZ, maxU, maxV);
        t.addVertexWithUV(maxX, y + 1.0D, maxZ, maxU, minV);
        minX = x + 0.5D - 0.5D;
        maxX = x + 0.5D + 0.5D;
        minZ = z + 0.5D - 0.25D;
        maxZ = z + 0.5D + 0.25D;
        t.addVertexWithUV(minX, y + 1.0D, minZ, minU, minV);
        t.addVertexWithUV(minX, y + 0.0D, minZ, minU, maxV);
        t.addVertexWithUV(maxX, y + 0.0D, minZ, maxU, maxV);
        t.addVertexWithUV(maxX, y + 1.0D, minZ, maxU, minV);
        t.addVertexWithUV(maxX, y + 1.0D, minZ, minU, minV);
        t.addVertexWithUV(maxX, y + 0.0D, minZ, minU, maxV);
        t.addVertexWithUV(minX, y + 0.0D, minZ, maxU, maxV);
        t.addVertexWithUV(minX, y + 1.0D, minZ, maxU, minV);
        t.addVertexWithUV(maxX, y + 1.0D, maxZ, minU, minV);
        t.addVertexWithUV(maxX, y + 0.0D, maxZ, minU, maxV);
        t.addVertexWithUV(minX, y + 0.0D, maxZ, maxU, maxV);
        t.addVertexWithUV(minX, y + 1.0D, maxZ, maxU, minV);
        t.addVertexWithUV(minX, y + 1.0D, maxZ, minU, minV);
        t.addVertexWithUV(minX, y + 0.0D, maxZ, minU, maxV);
        t.addVertexWithUV(maxX, y + 0.0D, maxZ, maxU, maxV);
        t.addVertexWithUV(maxX, y + 1.0D, maxZ, maxU, minV);
    }

    // --- Face rendering ---

    // The six renderFace* methods are 1.5.2 RenderBlocks.java:7167-7806 (non-AO branch): the override
    // texture replaces the passed icon, UVs interpolate over the render bounds, bounds outside 0..1
    // take the whole icon, flipTexture mirrors the side faces, and the per-face uvRotate value (0-3;
    // any other value, e.g. the saw's 4, is no rotation) turns the UVs. 1.5.2's field-to-face mapping
    // is kept: YNeg uvRotateBottom, YPos uvRotateTop, ZNeg uvRotateEast, ZPos uvRotateWest, XNeg
    // uvRotateNorth, XPos uvRotateSouth. Capture adaptations: the icon's name tags the quad and the
    // face normal is set; a null icon samples d/16 (NamedIcon's mapping) instead of throwing. With
    // NamedIcon the captured UVs stay in 0..1 icon space and FCBakedModel.convertCapturedQuad maps
    // them onto the sprite with sprite.getU(u * 16).

    private static double iconU(Icon icon, double d) {
        return icon != null ? (double)icon.getInterpolatedU(d) : d / 16.0D;
    }

    private static double iconV(Icon icon, double d) {
        return icon != null ? (double)icon.getInterpolatedV(d) : d / 16.0D;
    }

    private static double iconMinU(Icon icon) {
        return icon != null ? (double)icon.getMinU() : 0.0D;
    }

    private static double iconMaxU(Icon icon) {
        return icon != null ? (double)icon.getMaxU() : 1.0D;
    }

    private static double iconMinV(Icon icon) {
        return icon != null ? (double)icon.getMinV() : 0.0D;
    }

    private static double iconMaxV(Icon icon) {
        return icon != null ? (double)icon.getMaxV() : 1.0D;
    }

    /** 1.5.2 RenderBlocks.renderFaceYNeg (RenderBlocks.java:7167-7264). */
    public void renderFaceYNeg(Block block, double x, double y, double z, Icon icon) {
        Tessellator t = Tessellator.instance;

        if (this.hasOverrideBlockTexture()) {
            icon = this.overrideBlockTexture;
        }

        t.setCurrentTextureName(icon != null ? icon.getIconName() : null);
        double u1 = iconU(icon, this.renderMinX * 16.0D);
        double u2 = iconU(icon, this.renderMaxX * 16.0D);
        double v1 = iconV(icon, this.renderMinZ * 16.0D);
        double v2 = iconV(icon, this.renderMaxZ * 16.0D);

        if (this.renderMinX < 0.0D || this.renderMaxX > 1.0D) {
            u1 = iconMinU(icon);
            u2 = iconMaxU(icon);
        }

        if (this.renderMinZ < 0.0D || this.renderMaxZ > 1.0D) {
            v1 = iconMinV(icon);
            v2 = iconMaxV(icon);
        }

        double u3 = u2;
        double u4 = u1;
        double v3 = v1;
        double v4 = v2;

        if (this.uvRotateBottom == 2) {
            u1 = iconU(icon, this.renderMinZ * 16.0D);
            v1 = iconV(icon, 16.0D - this.renderMaxX * 16.0D);
            u2 = iconU(icon, this.renderMaxZ * 16.0D);
            v2 = iconV(icon, 16.0D - this.renderMinX * 16.0D);
            v3 = v1;
            v4 = v2;
            u3 = u1;
            u4 = u2;
            v1 = v2;
            v2 = v3;
        } else if (this.uvRotateBottom == 1) {
            u1 = iconU(icon, 16.0D - this.renderMaxZ * 16.0D);
            v1 = iconV(icon, this.renderMinX * 16.0D);
            u2 = iconU(icon, 16.0D - this.renderMinZ * 16.0D);
            v2 = iconV(icon, this.renderMaxX * 16.0D);
            u3 = u2;
            u4 = u1;
            u1 = u2;
            u2 = u4;
            v3 = v2;
            v4 = v1;
        } else if (this.uvRotateBottom == 3) {
            u1 = iconU(icon, 16.0D - this.renderMinX * 16.0D);
            u2 = iconU(icon, 16.0D - this.renderMaxX * 16.0D);
            v1 = iconV(icon, 16.0D - this.renderMinZ * 16.0D);
            v2 = iconV(icon, 16.0D - this.renderMaxZ * 16.0D);
            u3 = u2;
            u4 = u1;
            v3 = v1;
            v4 = v2;
        }

        double x0 = x + this.renderMinX;
        double x1 = x + this.renderMaxX;
        double y0 = y + this.renderMinY;
        double z0 = z + this.renderMinZ;
        double z1 = z + this.renderMaxZ;

        t.setNormal(0, -1, 0);
        t.addVertexWithUV(x0, y0, z1, u4, v4);
        t.addVertexWithUV(x0, y0, z0, u1, v1);
        t.addVertexWithUV(x1, y0, z0, u3, v3);
        t.addVertexWithUV(x1, y0, z1, u2, v2);
    }

    /** 1.5.2 RenderBlocks.renderFaceYPos (RenderBlocks.java:7269-7366). */
    public void renderFaceYPos(Block block, double x, double y, double z, Icon icon) {
        Tessellator t = Tessellator.instance;

        if (this.hasOverrideBlockTexture()) {
            icon = this.overrideBlockTexture;
        }

        t.setCurrentTextureName(icon != null ? icon.getIconName() : null);
        double u1 = iconU(icon, this.renderMinX * 16.0D);
        double u2 = iconU(icon, this.renderMaxX * 16.0D);
        double v1 = iconV(icon, this.renderMinZ * 16.0D);
        double v2 = iconV(icon, this.renderMaxZ * 16.0D);

        if (this.renderMinX < 0.0D || this.renderMaxX > 1.0D) {
            u1 = iconMinU(icon);
            u2 = iconMaxU(icon);
        }

        if (this.renderMinZ < 0.0D || this.renderMaxZ > 1.0D) {
            v1 = iconMinV(icon);
            v2 = iconMaxV(icon);
        }

        double u3 = u2;
        double u4 = u1;
        double v3 = v1;
        double v4 = v2;

        if (this.uvRotateTop == 1) {
            u1 = iconU(icon, this.renderMinZ * 16.0D);
            v1 = iconV(icon, 16.0D - this.renderMaxX * 16.0D);
            u2 = iconU(icon, this.renderMaxZ * 16.0D);
            v2 = iconV(icon, 16.0D - this.renderMinX * 16.0D);
            v3 = v1;
            v4 = v2;
            u3 = u1;
            u4 = u2;
            v1 = v2;
            v2 = v3;
        } else if (this.uvRotateTop == 2) {
            u1 = iconU(icon, 16.0D - this.renderMaxZ * 16.0D);
            v1 = iconV(icon, this.renderMinX * 16.0D);
            u2 = iconU(icon, 16.0D - this.renderMinZ * 16.0D);
            v2 = iconV(icon, this.renderMaxX * 16.0D);
            u3 = u2;
            u4 = u1;
            u1 = u2;
            u2 = u4;
            v3 = v2;
            v4 = v1;
        } else if (this.uvRotateTop == 3) {
            u1 = iconU(icon, 16.0D - this.renderMinX * 16.0D);
            u2 = iconU(icon, 16.0D - this.renderMaxX * 16.0D);
            v1 = iconV(icon, 16.0D - this.renderMinZ * 16.0D);
            v2 = iconV(icon, 16.0D - this.renderMaxZ * 16.0D);
            u3 = u2;
            u4 = u1;
            v3 = v1;
            v4 = v2;
        }

        double x0 = x + this.renderMinX;
        double x1 = x + this.renderMaxX;
        double y1 = y + this.renderMaxY;
        double z0 = z + this.renderMinZ;
        double z1 = z + this.renderMaxZ;

        t.setNormal(0, 1, 0);
        t.addVertexWithUV(x1, y1, z1, u2, v2);
        t.addVertexWithUV(x1, y1, z0, u3, v3);
        t.addVertexWithUV(x0, y1, z0, u1, v1);
        t.addVertexWithUV(x0, y1, z1, u4, v4);
    }

    /** 1.5.2 RenderBlocks.renderFaceZNeg (RenderBlocks.java:7371-7476). */
    public void renderFaceZNeg(Block block, double x, double y, double z, Icon icon) {
        Tessellator t = Tessellator.instance;

        if (this.hasOverrideBlockTexture()) {
            icon = this.overrideBlockTexture;
        }

        t.setCurrentTextureName(icon != null ? icon.getIconName() : null);
        double u1 = iconU(icon, this.renderMinX * 16.0D);
        double u2 = iconU(icon, this.renderMaxX * 16.0D);
        double v1 = iconV(icon, 16.0D - this.renderMaxY * 16.0D);
        double v2 = iconV(icon, 16.0D - this.renderMinY * 16.0D);

        if (this.flipTexture) {
            double swap = u1;
            u1 = u2;
            u2 = swap;
        }

        if (this.renderMinX < 0.0D || this.renderMaxX > 1.0D) {
            u1 = iconMinU(icon);
            u2 = iconMaxU(icon);
        }

        if (this.renderMinY < 0.0D || this.renderMaxY > 1.0D) {
            v1 = iconMinV(icon);
            v2 = iconMaxV(icon);
        }

        double u3 = u2;
        double u4 = u1;
        double v3 = v1;
        double v4 = v2;

        if (this.uvRotateEast == 2) {
            u1 = iconU(icon, this.renderMinY * 16.0D);
            v1 = iconV(icon, 16.0D - this.renderMinX * 16.0D);
            u2 = iconU(icon, this.renderMaxY * 16.0D);
            v2 = iconV(icon, 16.0D - this.renderMaxX * 16.0D);
            v3 = v1;
            v4 = v2;
            u3 = u1;
            u4 = u2;
            v1 = v2;
            v2 = v3;
        } else if (this.uvRotateEast == 1) {
            u1 = iconU(icon, 16.0D - this.renderMaxY * 16.0D);
            v1 = iconV(icon, this.renderMaxX * 16.0D);
            u2 = iconU(icon, 16.0D - this.renderMinY * 16.0D);
            v2 = iconV(icon, this.renderMinX * 16.0D);
            u3 = u2;
            u4 = u1;
            u1 = u2;
            u2 = u4;
            v3 = v2;
            v4 = v1;
        } else if (this.uvRotateEast == 3) {
            u1 = iconU(icon, 16.0D - this.renderMinX * 16.0D);
            u2 = iconU(icon, 16.0D - this.renderMaxX * 16.0D);
            v1 = iconV(icon, this.renderMaxY * 16.0D);
            v2 = iconV(icon, this.renderMinY * 16.0D);
            u3 = u2;
            u4 = u1;
            v3 = v1;
            v4 = v2;
        }

        double x0 = x + this.renderMinX;
        double x1 = x + this.renderMaxX;
        double y0 = y + this.renderMinY;
        double y1 = y + this.renderMaxY;
        double z0 = z + this.renderMinZ;

        t.setNormal(0, 0, -1);
        t.addVertexWithUV(x0, y1, z0, u3, v3);
        t.addVertexWithUV(x1, y1, z0, u1, v1);
        t.addVertexWithUV(x1, y0, z0, u4, v4);
        t.addVertexWithUV(x0, y0, z0, u2, v2);
    }

    /** 1.5.2 RenderBlocks.renderFaceZPos (RenderBlocks.java:7481-7586). */
    public void renderFaceZPos(Block block, double x, double y, double z, Icon icon) {
        Tessellator t = Tessellator.instance;

        if (this.hasOverrideBlockTexture()) {
            icon = this.overrideBlockTexture;
        }

        t.setCurrentTextureName(icon != null ? icon.getIconName() : null);
        double u1 = iconU(icon, this.renderMinX * 16.0D);
        double u2 = iconU(icon, this.renderMaxX * 16.0D);
        double v1 = iconV(icon, 16.0D - this.renderMaxY * 16.0D);
        double v2 = iconV(icon, 16.0D - this.renderMinY * 16.0D);

        if (this.flipTexture) {
            double swap = u1;
            u1 = u2;
            u2 = swap;
        }

        if (this.renderMinX < 0.0D || this.renderMaxX > 1.0D) {
            u1 = iconMinU(icon);
            u2 = iconMaxU(icon);
        }

        if (this.renderMinY < 0.0D || this.renderMaxY > 1.0D) {
            v1 = iconMinV(icon);
            v2 = iconMaxV(icon);
        }

        double u3 = u2;
        double u4 = u1;
        double v3 = v1;
        double v4 = v2;

        if (this.uvRotateWest == 1) {
            u1 = iconU(icon, this.renderMinY * 16.0D);
            v2 = iconV(icon, 16.0D - this.renderMinX * 16.0D);
            u2 = iconU(icon, this.renderMaxY * 16.0D);
            v1 = iconV(icon, 16.0D - this.renderMaxX * 16.0D);
            v3 = v1;
            v4 = v2;
            u3 = u1;
            u4 = u2;
            v1 = v2;
            v2 = v3;
        } else if (this.uvRotateWest == 2) {
            u1 = iconU(icon, 16.0D - this.renderMaxY * 16.0D);
            v1 = iconV(icon, this.renderMinX * 16.0D);
            u2 = iconU(icon, 16.0D - this.renderMinY * 16.0D);
            v2 = iconV(icon, this.renderMaxX * 16.0D);
            u3 = u2;
            u4 = u1;
            u1 = u2;
            u2 = u4;
            v3 = v2;
            v4 = v1;
        } else if (this.uvRotateWest == 3) {
            u1 = iconU(icon, 16.0D - this.renderMinX * 16.0D);
            u2 = iconU(icon, 16.0D - this.renderMaxX * 16.0D);
            v1 = iconV(icon, this.renderMaxY * 16.0D);
            v2 = iconV(icon, this.renderMinY * 16.0D);
            u3 = u2;
            u4 = u1;
            v3 = v1;
            v4 = v2;
        }

        double x0 = x + this.renderMinX;
        double x1 = x + this.renderMaxX;
        double y0 = y + this.renderMinY;
        double y1 = y + this.renderMaxY;
        double z1 = z + this.renderMaxZ;

        t.setNormal(0, 0, 1);
        t.addVertexWithUV(x0, y1, z1, u1, v1);
        t.addVertexWithUV(x0, y0, z1, u4, v4);
        t.addVertexWithUV(x1, y0, z1, u2, v2);
        t.addVertexWithUV(x1, y1, z1, u3, v3);
    }

    /** 1.5.2 RenderBlocks.renderFaceXNeg (RenderBlocks.java:7591-7696). */
    public void renderFaceXNeg(Block block, double x, double y, double z, Icon icon) {
        Tessellator t = Tessellator.instance;

        if (this.hasOverrideBlockTexture()) {
            icon = this.overrideBlockTexture;
        }

        t.setCurrentTextureName(icon != null ? icon.getIconName() : null);
        double u1 = iconU(icon, this.renderMinZ * 16.0D);
        double u2 = iconU(icon, this.renderMaxZ * 16.0D);
        double v1 = iconV(icon, 16.0D - this.renderMaxY * 16.0D);
        double v2 = iconV(icon, 16.0D - this.renderMinY * 16.0D);

        if (this.flipTexture) {
            double swap = u1;
            u1 = u2;
            u2 = swap;
        }

        if (this.renderMinZ < 0.0D || this.renderMaxZ > 1.0D) {
            u1 = iconMinU(icon);
            u2 = iconMaxU(icon);
        }

        if (this.renderMinY < 0.0D || this.renderMaxY > 1.0D) {
            v1 = iconMinV(icon);
            v2 = iconMaxV(icon);
        }

        double u3 = u2;
        double u4 = u1;
        double v3 = v1;
        double v4 = v2;

        if (this.uvRotateNorth == 1) {
            u1 = iconU(icon, this.renderMinY * 16.0D);
            v1 = iconV(icon, 16.0D - this.renderMaxZ * 16.0D);
            u2 = iconU(icon, this.renderMaxY * 16.0D);
            v2 = iconV(icon, 16.0D - this.renderMinZ * 16.0D);
            v3 = v1;
            v4 = v2;
            u3 = u1;
            u4 = u2;
            v1 = v2;
            v2 = v3;
        } else if (this.uvRotateNorth == 2) {
            u1 = iconU(icon, 16.0D - this.renderMaxY * 16.0D);
            v1 = iconV(icon, this.renderMinZ * 16.0D);
            u2 = iconU(icon, 16.0D - this.renderMinY * 16.0D);
            v2 = iconV(icon, this.renderMaxZ * 16.0D);
            u3 = u2;
            u4 = u1;
            u1 = u2;
            u2 = u4;
            v3 = v2;
            v4 = v1;
        } else if (this.uvRotateNorth == 3) {
            u1 = iconU(icon, 16.0D - this.renderMinZ * 16.0D);
            u2 = iconU(icon, 16.0D - this.renderMaxZ * 16.0D);
            v1 = iconV(icon, this.renderMaxY * 16.0D);
            v2 = iconV(icon, this.renderMinY * 16.0D);
            u3 = u2;
            u4 = u1;
            v3 = v1;
            v4 = v2;
        }

        double x0 = x + this.renderMinX;
        double y0 = y + this.renderMinY;
        double y1 = y + this.renderMaxY;
        double z0 = z + this.renderMinZ;
        double z1 = z + this.renderMaxZ;

        t.setNormal(-1, 0, 0);
        t.addVertexWithUV(x0, y1, z1, u3, v3);
        t.addVertexWithUV(x0, y1, z0, u1, v1);
        t.addVertexWithUV(x0, y0, z0, u4, v4);
        t.addVertexWithUV(x0, y0, z1, u2, v2);
    }

    /**
     * 1.5.2 RenderBlocks.renderFaceXPos (RenderBlocks.java:7701-7806). U runs from u(maxZ) at minZ to
     * u(minZ) at maxZ, i.e. u(z) = (minZ + maxZ - z) * 16, which equals (1 - z) * 16 only for bounds
     * centred on the block.
     */
    public void renderFaceXPos(Block block, double x, double y, double z, Icon icon) {
        Tessellator t = Tessellator.instance;

        if (this.hasOverrideBlockTexture()) {
            icon = this.overrideBlockTexture;
        }

        t.setCurrentTextureName(icon != null ? icon.getIconName() : null);
        double u1 = iconU(icon, this.renderMinZ * 16.0D);
        double u2 = iconU(icon, this.renderMaxZ * 16.0D);
        double v1 = iconV(icon, 16.0D - this.renderMaxY * 16.0D);
        double v2 = iconV(icon, 16.0D - this.renderMinY * 16.0D);

        if (this.flipTexture) {
            double swap = u1;
            u1 = u2;
            u2 = swap;
        }

        if (this.renderMinZ < 0.0D || this.renderMaxZ > 1.0D) {
            u1 = iconMinU(icon);
            u2 = iconMaxU(icon);
        }

        if (this.renderMinY < 0.0D || this.renderMaxY > 1.0D) {
            v1 = iconMinV(icon);
            v2 = iconMaxV(icon);
        }

        double u3 = u2;
        double u4 = u1;
        double v3 = v1;
        double v4 = v2;

        if (this.uvRotateSouth == 2) {
            u1 = iconU(icon, this.renderMinY * 16.0D);
            v1 = iconV(icon, 16.0D - this.renderMinZ * 16.0D);
            u2 = iconU(icon, this.renderMaxY * 16.0D);
            v2 = iconV(icon, 16.0D - this.renderMaxZ * 16.0D);
            v3 = v1;
            v4 = v2;
            u3 = u1;
            u4 = u2;
            v1 = v2;
            v2 = v3;
        } else if (this.uvRotateSouth == 1) {
            u1 = iconU(icon, 16.0D - this.renderMaxY * 16.0D);
            v1 = iconV(icon, this.renderMaxZ * 16.0D);
            u2 = iconU(icon, 16.0D - this.renderMinY * 16.0D);
            v2 = iconV(icon, this.renderMinZ * 16.0D);
            u3 = u2;
            u4 = u1;
            u1 = u2;
            u2 = u4;
            v3 = v2;
            v4 = v1;
        } else if (this.uvRotateSouth == 3) {
            u1 = iconU(icon, 16.0D - this.renderMinZ * 16.0D);
            u2 = iconU(icon, 16.0D - this.renderMaxZ * 16.0D);
            v1 = iconV(icon, this.renderMaxY * 16.0D);
            v2 = iconV(icon, this.renderMinY * 16.0D);
            u3 = u2;
            u4 = u1;
            v3 = v1;
            v4 = v2;
        }

        double x1 = x + this.renderMaxX;
        double y0 = y + this.renderMinY;
        double y1 = y + this.renderMaxY;
        double z0 = z + this.renderMinZ;
        double z1 = z + this.renderMaxZ;

        t.setNormal(1, 0, 0);
        t.addVertexWithUV(x1, y0, z1, u4, v4);
        t.addVertexWithUV(x1, y0, z0, u2, v2);
        t.addVertexWithUV(x1, y1, z0, u3, v3);
        t.addVertexWithUV(x1, y1, z1, u1, v1);
    }

    // --- FC additions: full face rendering ---

    // 1.5.2 (FCMOD) RenderFull*Face (RenderBlocks.java:10177-10449), RenderStandardFullBlock's faces:
    // renderFace* without the override texture, flip, clamps or uvRotate. FC names them after its own
    // compass, not the face normal: East is the z-negative face, West z-positive, North x-negative,
    // South x-positive. Same capture adaptations as renderFace*.

    /** 1.5.2 RenderFullBottomFace (RenderBlocks.java:10177-10219). */
    public void RenderFullBottomFace(Block block, double x, double y, double z, Icon icon) {
        Tessellator t = Tessellator.instance;
        t.setCurrentTextureName(icon != null ? icon.getIconName() : null);
        double u1 = iconU(icon, this.renderMinX * 16.0D);
        double u2 = iconU(icon, this.renderMaxX * 16.0D);
        double v1 = iconV(icon, this.renderMinZ * 16.0D);
        double v2 = iconV(icon, this.renderMaxZ * 16.0D);
        double x0 = x + this.renderMinX;
        double x1 = x + this.renderMaxX;
        double y0 = y + this.renderMinY;
        double z0 = z + this.renderMinZ;
        double z1 = z + this.renderMaxZ;
        t.setNormal(0, -1, 0);
        t.addVertexWithUV(x0, y0, z1, u1, v2);
        t.addVertexWithUV(x0, y0, z0, u1, v1);
        t.addVertexWithUV(x1, y0, z0, u2, v1);
        t.addVertexWithUV(x1, y0, z1, u2, v2);
    }

    /** 1.5.2 RenderFullTopFace (RenderBlocks.java:10221-10263). */
    public void RenderFullTopFace(Block block, double x, double y, double z, Icon icon) {
        Tessellator t = Tessellator.instance;
        t.setCurrentTextureName(icon != null ? icon.getIconName() : null);
        double u1 = iconU(icon, this.renderMinX * 16.0D);
        double u2 = iconU(icon, this.renderMaxX * 16.0D);
        double v1 = iconV(icon, this.renderMinZ * 16.0D);
        double v2 = iconV(icon, this.renderMaxZ * 16.0D);
        double x0 = x + this.renderMinX;
        double x1 = x + this.renderMaxX;
        double y1 = y + this.renderMaxY;
        double z0 = z + this.renderMinZ;
        double z1 = z + this.renderMaxZ;
        t.setNormal(0, 1, 0);
        t.addVertexWithUV(x1, y1, z1, u2, v2);
        t.addVertexWithUV(x1, y1, z0, u2, v1);
        t.addVertexWithUV(x0, y1, z0, u1, v1);
        t.addVertexWithUV(x0, y1, z1, u1, v2);
    }

    /** 1.5.2 RenderFullEastFace (RenderBlocks.java:10265-10308): the z-negative face. */
    public void RenderFullEastFace(Block block, double x, double y, double z, Icon icon) {
        Tessellator t = Tessellator.instance;
        t.setCurrentTextureName(icon != null ? icon.getIconName() : null);
        double u1 = iconU(icon, this.renderMinX * 16.0D);
        double u2 = iconU(icon, this.renderMaxX * 16.0D);
        double v1 = iconV(icon, 16.0D - this.renderMaxY * 16.0D);
        double v2 = iconV(icon, 16.0D - this.renderMinY * 16.0D);
        double x0 = x + this.renderMinX;
        double x1 = x + this.renderMaxX;
        double y0 = y + this.renderMinY;
        double y1 = y + this.renderMaxY;
        double z0 = z + this.renderMinZ;
        t.setNormal(0, 0, -1);
        t.addVertexWithUV(x0, y1, z0, u2, v1);
        t.addVertexWithUV(x1, y1, z0, u1, v1);
        t.addVertexWithUV(x1, y0, z0, u1, v2);
        t.addVertexWithUV(x0, y0, z0, u2, v2);
    }

    /** 1.5.2 RenderFullWestFace (RenderBlocks.java:10310-10353): the z-positive face. */
    public void RenderFullWestFace(Block block, double x, double y, double z, Icon icon) {
        Tessellator t = Tessellator.instance;
        t.setCurrentTextureName(icon != null ? icon.getIconName() : null);
        double u1 = iconU(icon, this.renderMinX * 16.0D);
        double u2 = iconU(icon, this.renderMaxX * 16.0D);
        double v1 = iconV(icon, 16.0D - this.renderMaxY * 16.0D);
        double v2 = iconV(icon, 16.0D - this.renderMinY * 16.0D);
        double x0 = x + this.renderMinX;
        double x1 = x + this.renderMaxX;
        double y0 = y + this.renderMinY;
        double y1 = y + this.renderMaxY;
        double z1 = z + this.renderMaxZ;
        t.setNormal(0, 0, 1);
        t.addVertexWithUV(x0, y1, z1, u1, v1);
        t.addVertexWithUV(x0, y0, z1, u1, v2);
        t.addVertexWithUV(x1, y0, z1, u2, v2);
        t.addVertexWithUV(x1, y1, z1, u2, v1);
    }

    /** 1.5.2 RenderFullNorthFace (RenderBlocks.java:10358-10401): the x-negative face. */
    public void RenderFullNorthFace(Block block, double x, double y, double z, Icon icon) {
        Tessellator t = Tessellator.instance;
        t.setCurrentTextureName(icon != null ? icon.getIconName() : null);
        double u1 = iconU(icon, this.renderMinZ * 16.0D);
        double u2 = iconU(icon, this.renderMaxZ * 16.0D);
        double v1 = iconV(icon, 16.0D - this.renderMaxY * 16.0D);
        double v2 = iconV(icon, 16.0D - this.renderMinY * 16.0D);
        double x0 = x + this.renderMinX;
        double y0 = y + this.renderMinY;
        double y1 = y + this.renderMaxY;
        double z0 = z + this.renderMinZ;
        double z1 = z + this.renderMaxZ;
        t.setNormal(-1, 0, 0);
        t.addVertexWithUV(x0, y1, z1, u2, v1);
        t.addVertexWithUV(x0, y1, z0, u1, v1);
        t.addVertexWithUV(x0, y0, z0, u1, v2);
        t.addVertexWithUV(x0, y0, z1, u2, v2);
    }

    /** 1.5.2 RenderFullSouthFace (RenderBlocks.java:10406-10449): the x-positive face. */
    public void RenderFullSouthFace(Block block, double x, double y, double z, Icon icon) {
        Tessellator t = Tessellator.instance;
        t.setCurrentTextureName(icon != null ? icon.getIconName() : null);
        double u1 = iconU(icon, this.renderMinZ * 16.0D);
        double u2 = iconU(icon, this.renderMaxZ * 16.0D);
        double v1 = iconV(icon, 16.0D - this.renderMaxY * 16.0D);
        double v2 = iconV(icon, 16.0D - this.renderMinY * 16.0D);
        double x1 = x + this.renderMaxX;
        double y0 = y + this.renderMinY;
        double y1 = y + this.renderMaxY;
        double z0 = z + this.renderMinZ;
        double z1 = z + this.renderMaxZ;
        t.setNormal(1, 0, 0);
        t.addVertexWithUV(x1, y0, z1, u1, v2);
        t.addVertexWithUV(x1, y0, z0, u2, v2);
        t.addVertexWithUV(x1, y1, z0, u2, v1);
        t.addVertexWithUV(x1, y1, z1, u1, v1);
    }

    // --- Block as item rendering ---

    private static final org.apache.logging.log4j.Logger LOGGER =
            org.apache.logging.log4j.LogManager.getLogger("BTW-RenderBlocks");

    /** FC RenderBlockAsItem overrides that threw once; drawn through renderBlockAsItemVanilla after. */
    private static final java.util.Set<Class<?>> FAILED_ITEM_RENDER_OVERRIDES =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** The block whose RenderBlockAsItem this instance is inside; see renderBlockAsItem. */
    private Block blockAsItemDispatch;

    /**
     * 1.5.2 RenderBlocks.renderBlockAsItem (FCMOD, RenderBlocks.java:9453-9456): hands the draw to
     * block.RenderBlockAsItem, so FC's overrides (mouldings, sidings, loose snow, cauldron, ...) draw
     * their own item shape. Its 1.5.2 default calls renderBlockAsItemVanilla (Block.java:3701-3704),
     * but the Modern-Common Block.RenderBlockAsItem default calls back into this method; that
     * re-entry for the block being dispatched is answered with renderBlockAsItemVanilla, which is
     * what the 1.5.2 default draws. An override that throws is logged once and its class is drawn
     * through renderBlockAsItemVanilla from then on. Callers: ItemRenderer.renderItem (held/worn
     * blocks, the snowman pumpkin), RenderEnderman.renderCarrying, RenderIronGolem, RenderItem
     * (embedded dropped blocks) and RenderTNTPrimed. Forge RecordingRenderBlocks overrides this.
     */
    public void renderBlockAsItem(Block block, int metadata, float brightness) {
        if (block == null) {
            return;
        }
        if (this.blockAsItemDispatch == block || FAILED_ITEM_RENDER_OVERRIDES.contains(block.getClass())) {
            this.renderBlockAsItemVanilla(block, metadata, brightness);
            return;
        }
        Block outer = this.blockAsItemDispatch;
        this.blockAsItemDispatch = block;
        try {
            block.RenderBlockAsItem(this, metadata, brightness);
        } catch (Throwable t) {
            if (FAILED_ITEM_RENDER_OVERRIDES.add(block.getClass())) {
                LOGGER.warn("{}.RenderBlockAsItem failed; drawing it as a plain block from now on: {}",
                        block.getClass().getSimpleName(), t.toString());
            }
        } finally {
            this.blockAsItemDispatch = outer;
        }
    }

    /**
     * 1.5.2 renderBlockAsItemVanilla (RenderBlocks.java:7815-8362): the useInventoryTint colour
     * (:7830-7843), the render-type-1 (crossed squares, :7851-7857), 19 (stem, :7858-7867), 23 (lily
     * pad: nothing, :7868-7876), 13 (inset cactus, :7877-7917) and 6 (crops, :7925-7931) branches,
     * and the standard-cube branch (:8306-8361): a 90-degree turn
     * about Y and the cube centred on the origin, translated by -0.5 before the faces (:8319) and
     * back by +0.5 after them (:8360), in FCMOD's item bounds (GetBlockBoundsFromPoolForItemRender,
     * :8313-8317), with grass tinting only its top face (:8325-8344). The turn and the GL colour stay
     * applied on return, as in 1.5.2, so callers push/pop around the call. Nothing here writes the
     * shared Block's bounds. Adaptations: every other special render type takes the cube branch
     * (the torch/anvil/chest/... branches are not ported); and the tint is the block's render colour
     * alone, without 1.5.2's {@code * brightness} factor, because the capture lights quads with MC's
     * packed light already and the client FC world's getBrightness can read 0 (RenderTNTPrimed
     * passes it), which would draw the block black.
     */
    public void renderBlockAsItemVanilla(Block block, int metadata, float brightness) {
        Tessellator t = Tessellator.instance;
        boolean isGrass = Block.grass != null && block.blockID == Block.grass.blockID;

        // :7820-7823 the dispenser, dropper and furnace show their front face.
        if (block == Block.dispenser || block == Block.dropper || block == Block.furnaceIdle) {
            metadata = 3;
        }

        // :7830-7843 the item takes the block's render colour (a stem's growth colour, blood wood
        // leaves' red); grass draws white here and tints only its top face in the cube branch.
        if (this.useInventoryTint) {
            this.setInventoryTint(isGrass ? 16777215 : block.getRenderColor(metadata));
        }

        int renderType = block.getRenderType();
        this.setRenderBoundsFromBlock(block);

        if (renderType == 1) {
            t.startDrawingQuads();
            t.setNormal(0.0F, -1.0F, 0.0F);
            this.drawCrossedSquares(block, metadata, -0.5D, -0.5D, -0.5D, 1.0F);
            t.draw();
            return;
        }

        if (renderType == 19) {
            // :7858-7867 (FCMOD dropped the setBlockBoundsForItemRender call): the stem's own height.
            t.startDrawingQuads();
            t.setNormal(0.0F, -1.0F, 0.0F);
            this.renderBlockStemSmall(block, metadata, this.renderMaxY, -0.5D, -0.5D, -0.5D);
            t.draw();
            return;
        }

        if (renderType == 23) {
            // :7868-7876 the lily pad draws nothing here (FCMOD dropped the setBlockBoundsForItemRender call).
            t.startDrawingQuads();
            t.setNormal(0.0F, -1.0F, 0.0F);
            t.draw();
            return;
        }

        if (renderType == 13) {
            // :7877-7917 the cactus: the block-bounds cube with its four sides inset 1/16, centred on the origin,
            // with no 90-degree turn and side icons at metadata 0 (FCMOD dropped setBlockBoundsForItemRender).
            GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
            float inset = 0.0625F;
            t.startDrawingQuads();
            t.setNormal(0.0F, -1.0F, 0.0F);
            this.renderFaceYNeg(block, 0.0D, 0.0D, 0.0D, this.getBlockIconFromSide(block, 0));
            t.draw();
            t.startDrawingQuads();
            t.setNormal(0.0F, 1.0F, 0.0F);
            this.renderFaceYPos(block, 0.0D, 0.0D, 0.0D, this.getBlockIconFromSide(block, 1));
            t.draw();
            t.startDrawingQuads();
            t.setNormal(0.0F, 0.0F, -1.0F);
            t.addTranslation(0.0F, 0.0F, inset);
            this.renderFaceZNeg(block, 0.0D, 0.0D, 0.0D, this.getBlockIconFromSide(block, 2));
            t.addTranslation(0.0F, 0.0F, -inset);
            t.draw();
            t.startDrawingQuads();
            t.setNormal(0.0F, 0.0F, 1.0F);
            t.addTranslation(0.0F, 0.0F, -inset);
            this.renderFaceZPos(block, 0.0D, 0.0D, 0.0D, this.getBlockIconFromSide(block, 3));
            t.addTranslation(0.0F, 0.0F, inset);
            t.draw();
            t.startDrawingQuads();
            t.setNormal(-1.0F, 0.0F, 0.0F);
            t.addTranslation(inset, 0.0F, 0.0F);
            this.renderFaceXNeg(block, 0.0D, 0.0D, 0.0D, this.getBlockIconFromSide(block, 4));
            t.addTranslation(-inset, 0.0F, 0.0F);
            t.draw();
            t.startDrawingQuads();
            t.setNormal(1.0F, 0.0F, 0.0F);
            t.addTranslation(-inset, 0.0F, 0.0F);
            this.renderFaceXPos(block, 0.0D, 0.0D, 0.0D, this.getBlockIconFromSide(block, 5));
            t.addTranslation(inset, 0.0F, 0.0F);
            t.draw();
            GL11.glTranslatef(0.5F, 0.5F, 0.5F);
            return;
        }

        if (renderType == 6) {
            t.startDrawingQuads();
            t.setNormal(0.0F, -1.0F, 0.0F);
            this.renderBlockCropsImpl(block, metadata, -0.5D, -0.5D, -0.5D);
            t.draw();
            return;
        }

        if (renderType == 16) {
            metadata = 1;
        }

        // FCMOD (:8313-8317) in place of setBlockBoundsForItemRender + setRenderBoundsFromBlock; a
        // null result keeps the block bounds set above.
        this.setRenderBounds(block.GetBlockBoundsFromPoolForItemRender(metadata));
        GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        t.startDrawingQuads();
        t.setNormal(0.0F, -1.0F, 0.0F);
        this.renderFaceYNeg(block, 0.0D, 0.0D, 0.0D, this.getBlockIconFromSideAndMetadata(block, 0, metadata));
        t.draw();

        if (isGrass && this.useInventoryTint) {
            this.setInventoryTint(block.getRenderColor(metadata));
        }

        t.startDrawingQuads();
        t.setNormal(0.0F, 1.0F, 0.0F);
        this.renderFaceYPos(block, 0.0D, 0.0D, 0.0D, this.getBlockIconFromSideAndMetadata(block, 1, metadata));
        t.draw();

        if (isGrass && this.useInventoryTint) {
            this.setInventoryTint(16777215);
        }

        t.startDrawingQuads();
        t.setNormal(0.0F, 0.0F, -1.0F);
        this.renderFaceZNeg(block, 0.0D, 0.0D, 0.0D, this.getBlockIconFromSideAndMetadata(block, 2, metadata));
        t.draw();
        t.startDrawingQuads();
        t.setNormal(0.0F, 0.0F, 1.0F);
        this.renderFaceZPos(block, 0.0D, 0.0D, 0.0D, this.getBlockIconFromSideAndMetadata(block, 3, metadata));
        t.draw();
        t.startDrawingQuads();
        t.setNormal(-1.0F, 0.0F, 0.0F);
        this.renderFaceXNeg(block, 0.0D, 0.0D, 0.0D, this.getBlockIconFromSideAndMetadata(block, 4, metadata));
        t.draw();
        t.startDrawingQuads();
        t.setNormal(1.0F, 0.0F, 0.0F);
        this.renderFaceXPos(block, 0.0D, 0.0D, 0.0D, this.getBlockIconFromSideAndMetadata(block, 5, metadata));
        t.draw();
        GL11.glTranslatef(0.5F, 0.5F, 0.5F);
    }

    /**
     * renderBlockAsItemVanilla's GL colour (1.5.2 :7842, :8331, :8341): an 0xRRGGBB render colour
     * as glColor4f with full alpha. 1.5.2 multiplied each channel by the brightness argument; see
     * the adaptation note on renderBlockAsItemVanilla.
     */
    private void setInventoryTint(int color) {
        float r = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;
        GL11.glColor4f(r, g, b, 1.0F);
    }

    // --- Falling block rendering ---

    // 1.5.2 RenderBlocks.renderBlockSandFalling — RenderFallingSand.doRender draws airborne
    // falling blocks (FCEntityFallingBlock: loose blocks, gravel, falling slabs) through this,
    // centered on -0.5 offsets at the entity position with world brightness. The vanilla
    // var12/var13 anaglyph remnants always resolve to 1.0 and are folded in.
    public void renderBlockSandFalling(Block block, World world, int x, int y, int z, int metadata) {
        float brightnessBottom = 0.5F;
        float brightnessTop = 1.0F;
        float brightnessZSides = 0.8F;
        float brightnessXSides = 0.6F;
        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
        t.setColorOpaque_F(brightnessBottom, brightnessBottom, brightnessBottom);
        this.renderFaceYNeg(block, -0.5D, -0.5D, -0.5D, this.getBlockIconFromSideAndMetadata(block, 0, metadata));
        t.setColorOpaque_F(brightnessTop, brightnessTop, brightnessTop);
        this.renderFaceYPos(block, -0.5D, -0.5D, -0.5D, this.getBlockIconFromSideAndMetadata(block, 1, metadata));
        t.setColorOpaque_F(brightnessZSides, brightnessZSides, brightnessZSides);
        this.renderFaceZNeg(block, -0.5D, -0.5D, -0.5D, this.getBlockIconFromSideAndMetadata(block, 2, metadata));
        t.setColorOpaque_F(brightnessZSides, brightnessZSides, brightnessZSides);
        this.renderFaceZPos(block, -0.5D, -0.5D, -0.5D, this.getBlockIconFromSideAndMetadata(block, 3, metadata));
        t.setColorOpaque_F(brightnessXSides, brightnessXSides, brightnessXSides);
        this.renderFaceXNeg(block, -0.5D, -0.5D, -0.5D, this.getBlockIconFromSideAndMetadata(block, 4, metadata));
        t.setColorOpaque_F(brightnessXSides, brightnessXSides, brightnessXSides);
        this.renderFaceXPos(block, -0.5D, -0.5D, -0.5D, this.getBlockIconFromSideAndMetadata(block, 5, metadata));
        t.draw();
    }

    // --- Icon helpers ---

    public Icon getBlockIcon(Block block, IBlockAccess blockAccess, int x, int y, int z, int side) {
        return this.overrideBlockTexture != null ? this.overrideBlockTexture : block.getBlockTexture(blockAccess, x, y, z, side);
    }

    public Icon getBlockIconFromSideAndMetadata(Block block, int side, int metadata) {
        return this.overrideBlockTexture != null ? this.overrideBlockTexture : block.getIcon(side, metadata);
    }

    public Icon getBlockIconFromSide(Block block, int side) {
        return this.overrideBlockTexture != null ? this.overrideBlockTexture : block.getIcon(side, 0);
    }

    public Icon getBlockIcon(Block block) {
        return this.overrideBlockTexture != null ? this.overrideBlockTexture : block.getIcon(0, 0);
    }

    public Icon getIconSafe(Icon icon) {
        return icon;
    }

    // --- FC additions: ShouldSideBeRendered ---

    /**
     * 1.5.2 (FCMOD) RenderBlocks.ShouldSideBeRenderedBasedOnCurrentBounds (RenderBlocks.java:10645-10670),
     * which FC's shouldSideBeRendered overrides (saw, sidings, mouldings, stairs, bellows, ...) delegate
     * to through m_currentBlockRenderer: a face inset from its block edge by the current render bounds
     * is always drawn; a face on the edge is drawn unless the neighbour's ShouldRenderNeighborFullFaceSide
     * hides it. Port guards: without a blockAccess (a renderer FCBakedModel has detached, or a draw
     * outside the world) the face is drawn, and a throwing neighbour override draws it too (logged once).
     */
    public boolean ShouldSideBeRenderedBasedOnCurrentBounds(int iNeighborI, int iNeighborJ, int iNeighborK, int iSide) {
        if ((iSide == 0 && this.renderMinY > 0.0D)
                || (iSide == 1 && this.renderMaxY < 1.0D)
                || (iSide == 2 && this.renderMinZ > 0.0D)
                || (iSide == 3 && this.renderMaxZ < 1.0D)
                || (iSide == 4 && this.renderMinX > 0.0D)
                || (iSide == 5 && this.renderMaxX < 1.0D)) {
            return true;
        }

        if (this.blockAccess == null) {
            return true;
        }

        int neighborId = this.blockAccess.getBlockId(iNeighborI, iNeighborJ, iNeighborK);
        Block neighborBlock = neighborId > 0 && neighborId < Block.blocksList.length ? Block.blocksList[neighborId] : null;

        if (neighborBlock != null) {
            try {
                return neighborBlock.ShouldRenderNeighborFullFaceSide(this.blockAccess, iNeighborI, iNeighborJ, iNeighborK, iSide);
            } catch (Throwable t) {
                logFaceCallFailure(neighborBlock, "ShouldRenderNeighborFullFaceSide", t);
                return true;
            }
        }

        return true;
    }

    // --- Static helpers ---

    /** Vanilla 1.5.2's name for DoesRenderIDRenderItemIn3d (FCMOD renamed it, RenderBlocks.java:8367-8371). */
    public static boolean renderItemIn3d(int renderType) {
        return DoesRenderIDRenderItemIn3d(renderType);
    }

    /**
     * 1.5.2 RenderBlocks.DoesRenderIDRenderItemIn3d (RenderBlocks.java:8371-8375): the render types
     * whose item is drawn as a 3D block rather than a flat icon. 1.5.2 Block.DoesItemRenderAsBlock
     * returns this for the block's render type (Block.java:3706-3709).
     */
    public static boolean DoesRenderIDRenderItemIn3d(int renderType) {
        return renderType == 0 ? true : (renderType == 31 ? true : (renderType == 39 ? true : (renderType == 13 ? true
                : (renderType == 10 ? true : (renderType == 11 ? true : (renderType == 27 ? true : (renderType == 22 ? true
                : (renderType == 21 ? true : (renderType == 16 ? true : (renderType == 26 ? true : (renderType == 32 ? true
                : (renderType == 34 ? true : renderType == 35))))))))))));
    }
}
