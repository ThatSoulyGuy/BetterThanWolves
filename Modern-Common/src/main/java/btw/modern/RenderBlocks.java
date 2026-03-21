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

    public void renderBlockUsingTexture(Block block, int x, int y, int z, Icon icon) {}

    public void renderBlockAllFaces(Block block, int x, int y, int z) {}

    public boolean renderBlockByRenderType(Block block, int x, int y, int z) {
        int renderType = block.getRenderType();
        switch (renderType) {
            case 0: return renderStandardBlock(block, x, y, z);
            case 1: return renderCrossedSquares(block, x, y, z);
            case 2: return renderBlockTorch(block, x, y, z);
            case 4: return renderBlockFluids(block, x, y, z);
            case 6: return renderBlockCrops(block, x, y, z);
            case 8: return renderBlockLadder(block, x, y, z);
            case 20: return renderBlockVine(block, x, y, z);
            default:
                // For any unhandled render type, use standard block rendering
                // with the block's own bounds (slabs, buttons, pressure plates, etc.
                // set their bounds in constructor/setBlockBoundsBasedOnState)
                return renderStandardBlock(block, x, y, z);
        }
    }

    // --- Standard block rendering ---

    public boolean renderStandardBlock(Block block, int x, int y, int z) {
        // Do NOT call setRenderBoundsFromBlock here — the caller (e.g.,
        // FCBlockAxle.RenderBlock) may have already set custom render bounds.
        // In MC 1.5.2, renderStandardBlock also did not override bounds.
        int meta = blockAccess != null ? blockAccess.getBlockMetadata(x, y, z) : 0;
        Icon icon;
        icon = getBlockIconFromSideAndMetadata(block, 0, meta);
        renderFaceYNeg(block, x, y, z, icon);
        icon = getBlockIconFromSideAndMetadata(block, 1, meta);
        renderFaceYPos(block, x, y, z, icon);
        icon = getBlockIconFromSideAndMetadata(block, 2, meta);
        renderFaceZNeg(block, x, y, z, icon);
        icon = getBlockIconFromSideAndMetadata(block, 3, meta);
        renderFaceZPos(block, x, y, z, icon);
        icon = getBlockIconFromSideAndMetadata(block, 4, meta);
        renderFaceXNeg(block, x, y, z, icon);
        icon = getBlockIconFromSideAndMetadata(block, 5, meta);
        renderFaceXPos(block, x, y, z, icon);
        return true;
    }

    public boolean renderStandardBlockWithAmbientOcclusion(Block block, int x, int y, int z, float r, float g, float b) {
        return false;
    }

    public boolean func_102027_b(Block block, int x, int y, int z, float r, float g, float b) {
        return false;
    }

    public boolean renderStandardBlockWithColorMultiplier(Block block, int x, int y, int z, float r, float g, float b) {
        return false;
    }

    // --- FC additions: standard full block rendering ---

    public boolean RenderStandardFullBlock(Block block, int x, int y, int z) {
        int meta = blockAccess != null ? blockAccess.getBlockMetadata(x, y, z) : 0;
        Icon icon;
        icon = getBlockIconFromSideAndMetadata(block, 0, meta);
        renderFaceYNeg(block, x, y, z, icon);
        icon = getBlockIconFromSideAndMetadata(block, 1, meta);
        renderFaceYPos(block, x, y, z, icon);
        icon = getBlockIconFromSideAndMetadata(block, 2, meta);
        renderFaceZNeg(block, x, y, z, icon);
        icon = getBlockIconFromSideAndMetadata(block, 3, meta);
        renderFaceZPos(block, x, y, z, icon);
        icon = getBlockIconFromSideAndMetadata(block, 4, meta);
        renderFaceXNeg(block, x, y, z, icon);
        icon = getBlockIconFromSideAndMetadata(block, 5, meta);
        renderFaceXPos(block, x, y, z, icon);
        return true;
    }

    public boolean RenderStandardFullBlockWithAmbientOcclusion(Block block, int x, int y, int z) {
        return false;
    }

    public boolean RenderStandardFullBlockWithColorMultiplier(Block block, int x, int y, int z) {
        return false;
    }

    // --- Grass block rendering ---

    public boolean renderGrassBlockWithAmbientOcclusion(Block block, int x, int y, int z, float r, float g, float b, Icon sideOverlayIcon) {
        return false;
    }

    public boolean renderGrassBlockWithColorMultiplier(Block block, int x, int y, int z, float r, float g, float b, Icon sideOverlayIcon) {
        return false;
    }

    // --- Specific block type renders ---

    public boolean renderBlockLog(Block block, int x, int y, int z) { return false; }
    public boolean renderBlockQuartz(Block block, int x, int y, int z) { return false; }
    public boolean renderBlockCactus(Block block, int x, int y, int z) { return false; }
    public boolean renderBlockCactusImpl(Block block, int x, int y, int z, float r, float g, float b) { return false; }
    public boolean renderBlockFence(BlockFence block, int x, int y, int z) { return false; }
    public boolean renderBlockWall(BlockWall block, int x, int y, int z) { return false; }
    public boolean renderBlockFenceGate(BlockFenceGate block, int x, int y, int z) { return false; }
    public boolean renderBlockStairs(BlockStairs block, int x, int y, int z) { return false; }
    public boolean renderBlockDoor(Block block, int x, int y, int z) { return false; }
    public boolean renderBlockFluids(Block block, int x, int y, int z) { return false; }
    public boolean renderBlockTorch(Block block, int x, int y, int z) {
        int meta = blockAccess != null ? blockAccess.getBlockMetadata(x, y, z) : 0;
        Tessellator t = Tessellator.instance;
        Icon icon = getBlockIconFromSideAndMetadata(block, 0, meta);
        t.setCurrentTextureName(icon != null ? icon.getIconName() : null);
        double minU = icon != null ? icon.getMinU() : 0;
        double maxU = icon != null ? icon.getMaxU() : 1;
        double minV = icon != null ? icon.getMinV() : 0;
        double maxV = icon != null ? icon.getMaxV() : 1;
        // Render as crossed squares (simplified torch — actual torch has angle)
        double w = 0.0625; // 1 pixel width
        double cx = x + 0.5, cz = z + 0.5;
        // Two crossed planes for the torch stick
        t.setNormal(0, 1, 0);
        t.addVertexWithUV(cx - w, y + 0, cz, minU, maxV);
        t.addVertexWithUV(cx - w, y + 0.625, cz, minU, minV);
        t.addVertexWithUV(cx + w, y + 0.625, cz, maxU, minV);
        t.addVertexWithUV(cx + w, y + 0, cz, maxU, maxV);
        t.addVertexWithUV(cx + w, y + 0, cz, minU, maxV);
        t.addVertexWithUV(cx + w, y + 0.625, cz, minU, minV);
        t.addVertexWithUV(cx - w, y + 0.625, cz, maxU, minV);
        t.addVertexWithUV(cx - w, y + 0, cz, maxU, maxV);
        t.addVertexWithUV(cx, y + 0, cz - w, minU, maxV);
        t.addVertexWithUV(cx, y + 0.625, cz - w, minU, minV);
        t.addVertexWithUV(cx, y + 0.625, cz + w, maxU, minV);
        t.addVertexWithUV(cx, y + 0, cz + w, maxU, maxV);
        t.addVertexWithUV(cx, y + 0, cz + w, minU, maxV);
        t.addVertexWithUV(cx, y + 0.625, cz + w, minU, minV);
        t.addVertexWithUV(cx, y + 0.625, cz - w, maxU, minV);
        t.addVertexWithUV(cx, y + 0, cz - w, maxU, maxV);
        return true;
    }
    public boolean renderBlockRepeater(BlockRedstoneRepeater block, int x, int y, int z) { return false; }
    public boolean renderBlockRedstoneWire(Block block, int x, int y, int z) { return false; }
    public boolean renderBlockMinecartTrack(BlockRailBase block, int x, int y, int z) { return false; }
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
    public boolean renderBlockVine(Block block, int x, int y, int z) { return false; }
    public boolean renderBlockPane(BlockPane block, int x, int y, int z) { return false; }
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
        t.setNormal(0, 1, 0);
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
    public boolean renderBlockStem(Block block, int x, int y, int z) { return false; }
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
        t.setNormal(0, 1, 0);
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
    public boolean renderBlockLilyPad(Block block, int x, int y, int z) { return false; }
    public boolean renderBlockFire(BlockFire block, int x, int y, int z) { return false; }
    public boolean renderBlockLever(Block block, int x, int y, int z) { return false; }
    public boolean renderBlockTripWireSource(Block block, int x, int y, int z) { return false; }
    public boolean renderBlockTripWire(Block block, int x, int y, int z) { return false; }
    public boolean renderBlockDragonEgg(BlockDragonEgg block, int x, int y, int z) { return false; }
    public boolean renderBlockCauldron(BlockCauldron block, int x, int y, int z) { return false; }
    public boolean renderBlockAnvilMetadata(BlockAnvil block, int x, int y, int z, int metadata) { return false; }

    public boolean renderPistonBase(Block block, int x, int y, int z, boolean isExtended) { return false; }
    public void renderPistonBaseAllFaces(Block block, int x, int y, int z) {}
    public boolean renderPistonExtension(Block block, int x, int y, int z, boolean isHead) { return false; }
    public void renderPistonExtensionAllFaces(Block block, int x, int y, int z, boolean isHead) {}

    // --- FC additions: specific block type renders ---

    public boolean RenderBlockRedstoneLogic(BlockRedstoneLogic block, int x, int y, int z) { return false; }
    public boolean RenderBlockBeacon(BlockBeacon block, int x, int y, int z) { return false; }
    public boolean RenderBlockBed(Block block, int x, int y, int z) { return false; }
    public boolean RenderBlockBrewingStand(BlockBrewingStand block, int x, int y, int z) { return false; }
    public boolean RenderBlockCocoa(BlockCocoa block, int x, int y, int z) { return false; }
    public boolean RenderBlockAnvil(BlockAnvil block, int x, int y, int z) { return false; }
    public boolean RenderBlockEndPortalFrame(BlockEndPortalFrame block, int x, int y, int z) { return false; }

    // --- Torch / helper rendering ---

    public void renderTorchAtAngle(Block block, double x, double y, double z, double angleX, double angleZ, int metadata) {}
    public void drawCrossedSquares(Block block, int metadata, double x, double y, double z, float brightness) {}
    public void renderBlockStemSmall(Block block, int metadata, double x, double y, double z, double height) {}
    public void renderBlockStemBig(BlockStem block, int metadata, int connectedBlockId, double x, double y, double z, double height) {}
    public void renderBlockCropsImpl(Block block, int metadata, double x, double y, double z) {}

    // --- Face rendering ---

    // Face methods interpolate UVs based on render bounds, matching MC 1.5.2.
    // For a face spanning renderMinX..renderMaxX, the U coordinate maps from
    // icon pixel (renderMin*16) to icon pixel (renderMax*16). With NamedIcon
    // (getInterpolatedU returns d/16 for 0-1 normalized), the captured UVs
    // stay in 0-1 range and convertCapturedQuad maps via sprite.getU(v.u*16).

    public void renderFaceYNeg(Block block, double x, double y, double z, Icon icon) {
        Tessellator t = Tessellator.instance;
        t.setCurrentTextureName(icon != null ? icon.getIconName() : null);
        double u0 = icon != null ? icon.getInterpolatedU(renderMinX * 16.0) : 0;
        double u1 = icon != null ? icon.getInterpolatedU(renderMaxX * 16.0) : 1;
        double v0 = icon != null ? icon.getInterpolatedV(renderMinZ * 16.0) : 0;
        double v1 = icon != null ? icon.getInterpolatedV(renderMaxZ * 16.0) : 1;
        t.setNormal(0, -1, 0);
        t.addVertexWithUV(x + renderMinX, y + renderMinY, z + renderMaxZ, u0, v1);
        t.addVertexWithUV(x + renderMinX, y + renderMinY, z + renderMinZ, u0, v0);
        t.addVertexWithUV(x + renderMaxX, y + renderMinY, z + renderMinZ, u1, v0);
        t.addVertexWithUV(x + renderMaxX, y + renderMinY, z + renderMaxZ, u1, v1);
    }

    public void renderFaceYPos(Block block, double x, double y, double z, Icon icon) {
        Tessellator t = Tessellator.instance;
        t.setCurrentTextureName(icon != null ? icon.getIconName() : null);
        double u0 = icon != null ? icon.getInterpolatedU(renderMinX * 16.0) : 0;
        double u1 = icon != null ? icon.getInterpolatedU(renderMaxX * 16.0) : 1;
        double v0 = icon != null ? icon.getInterpolatedV(renderMinZ * 16.0) : 0;
        double v1 = icon != null ? icon.getInterpolatedV(renderMaxZ * 16.0) : 1;
        t.setNormal(0, 1, 0);
        t.addVertexWithUV(x + renderMaxX, y + renderMaxY, z + renderMaxZ, u1, v1);
        t.addVertexWithUV(x + renderMaxX, y + renderMaxY, z + renderMinZ, u1, v0);
        t.addVertexWithUV(x + renderMinX, y + renderMaxY, z + renderMinZ, u0, v0);
        t.addVertexWithUV(x + renderMinX, y + renderMaxY, z + renderMaxZ, u0, v1);
    }

    public void renderFaceZNeg(Block block, double x, double y, double z, Icon icon) {
        Tessellator t = Tessellator.instance;
        t.setCurrentTextureName(icon != null ? icon.getIconName() : null);
        double u0 = icon != null ? icon.getInterpolatedU(renderMinX * 16.0) : 0;
        double u1 = icon != null ? icon.getInterpolatedU(renderMaxX * 16.0) : 1;
        double v0 = icon != null ? icon.getInterpolatedV((1.0 - renderMaxY) * 16.0) : 0;
        double v1 = icon != null ? icon.getInterpolatedV((1.0 - renderMinY) * 16.0) : 1;
        t.setNormal(0, 0, -1);
        t.addVertexWithUV(x + renderMinX, y + renderMaxY, z + renderMinZ, u1, v0);
        t.addVertexWithUV(x + renderMaxX, y + renderMaxY, z + renderMinZ, u0, v0);
        t.addVertexWithUV(x + renderMaxX, y + renderMinY, z + renderMinZ, u0, v1);
        t.addVertexWithUV(x + renderMinX, y + renderMinY, z + renderMinZ, u1, v1);
    }

    public void renderFaceZPos(Block block, double x, double y, double z, Icon icon) {
        Tessellator t = Tessellator.instance;
        t.setCurrentTextureName(icon != null ? icon.getIconName() : null);
        double u0 = icon != null ? icon.getInterpolatedU(renderMinX * 16.0) : 0;
        double u1 = icon != null ? icon.getInterpolatedU(renderMaxX * 16.0) : 1;
        double v0 = icon != null ? icon.getInterpolatedV((1.0 - renderMaxY) * 16.0) : 0;
        double v1 = icon != null ? icon.getInterpolatedV((1.0 - renderMinY) * 16.0) : 1;
        t.setNormal(0, 0, 1);
        t.addVertexWithUV(x + renderMinX, y + renderMaxY, z + renderMaxZ, u0, v0);
        t.addVertexWithUV(x + renderMinX, y + renderMinY, z + renderMaxZ, u0, v1);
        t.addVertexWithUV(x + renderMaxX, y + renderMinY, z + renderMaxZ, u1, v1);
        t.addVertexWithUV(x + renderMaxX, y + renderMaxY, z + renderMaxZ, u1, v0);
    }

    public void renderFaceXNeg(Block block, double x, double y, double z, Icon icon) {
        Tessellator t = Tessellator.instance;
        t.setCurrentTextureName(icon != null ? icon.getIconName() : null);
        double u0 = icon != null ? icon.getInterpolatedU(renderMinZ * 16.0) : 0;
        double u1 = icon != null ? icon.getInterpolatedU(renderMaxZ * 16.0) : 1;
        double v0 = icon != null ? icon.getInterpolatedV((1.0 - renderMaxY) * 16.0) : 0;
        double v1 = icon != null ? icon.getInterpolatedV((1.0 - renderMinY) * 16.0) : 1;
        t.setNormal(-1, 0, 0);
        t.addVertexWithUV(x + renderMinX, y + renderMaxY, z + renderMaxZ, u1, v0);
        t.addVertexWithUV(x + renderMinX, y + renderMaxY, z + renderMinZ, u0, v0);
        t.addVertexWithUV(x + renderMinX, y + renderMinY, z + renderMinZ, u0, v1);
        t.addVertexWithUV(x + renderMinX, y + renderMinY, z + renderMaxZ, u1, v1);
    }

    public void renderFaceXPos(Block block, double x, double y, double z, Icon icon) {
        Tessellator t = Tessellator.instance;
        t.setCurrentTextureName(icon != null ? icon.getIconName() : null);
        double u0 = icon != null ? icon.getInterpolatedU((1.0 - renderMaxZ) * 16.0) : 0;
        double u1 = icon != null ? icon.getInterpolatedU((1.0 - renderMinZ) * 16.0) : 1;
        double v0 = icon != null ? icon.getInterpolatedV((1.0 - renderMaxY) * 16.0) : 0;
        double v1 = icon != null ? icon.getInterpolatedV((1.0 - renderMinY) * 16.0) : 1;
        t.setNormal(1, 0, 0);
        t.addVertexWithUV(x + renderMaxX, y + renderMinY, z + renderMaxZ, u0, v1);
        t.addVertexWithUV(x + renderMaxX, y + renderMinY, z + renderMinZ, u1, v1);
        t.addVertexWithUV(x + renderMaxX, y + renderMaxY, z + renderMinZ, u1, v0);
        t.addVertexWithUV(x + renderMaxX, y + renderMaxY, z + renderMaxZ, u0, v0);
    }

    // --- FC additions: full face rendering ---

    public void RenderFullBottomFace(Block block, double x, double y, double z, Icon icon) {
        Tessellator t = Tessellator.instance;
        t.setCurrentTextureName(icon != null ? icon.getIconName() : null);
        double minU = icon != null ? icon.getMinU() : 0;
        double maxU = icon != null ? icon.getMaxU() : 1;
        double minV = icon != null ? icon.getMinV() : 0;
        double maxV = icon != null ? icon.getMaxV() : 1;
        t.setNormal(0, -1, 0);
        t.addVertexWithUV(x + 0, y + 0, z + 1, minU, maxV);
        t.addVertexWithUV(x + 0, y + 0, z + 0, minU, minV);
        t.addVertexWithUV(x + 1, y + 0, z + 0, maxU, minV);
        t.addVertexWithUV(x + 1, y + 0, z + 1, maxU, maxV);
    }

    public void RenderFullTopFace(Block block, double x, double y, double z, Icon icon) {
        Tessellator t = Tessellator.instance;
        t.setCurrentTextureName(icon != null ? icon.getIconName() : null);
        double minU = icon != null ? icon.getMinU() : 0;
        double maxU = icon != null ? icon.getMaxU() : 1;
        double minV = icon != null ? icon.getMinV() : 0;
        double maxV = icon != null ? icon.getMaxV() : 1;
        t.setNormal(0, 1, 0);
        t.addVertexWithUV(x + 1, y + 1, z + 1, maxU, maxV);
        t.addVertexWithUV(x + 1, y + 1, z + 0, maxU, minV);
        t.addVertexWithUV(x + 0, y + 1, z + 0, minU, minV);
        t.addVertexWithUV(x + 0, y + 1, z + 1, minU, maxV);
    }

    public void RenderFullEastFace(Block block, double x, double y, double z, Icon icon) {
        Tessellator t = Tessellator.instance;
        t.setCurrentTextureName(icon != null ? icon.getIconName() : null);
        double minU = icon != null ? icon.getMinU() : 0;
        double maxU = icon != null ? icon.getMaxU() : 1;
        double minV = icon != null ? icon.getMinV() : 0;
        double maxV = icon != null ? icon.getMaxV() : 1;
        t.setNormal(1, 0, 0);
        t.addVertexWithUV(x + 1, y + 0, z + 1, minU, maxV);
        t.addVertexWithUV(x + 1, y + 0, z + 0, maxU, maxV);
        t.addVertexWithUV(x + 1, y + 1, z + 0, maxU, minV);
        t.addVertexWithUV(x + 1, y + 1, z + 1, minU, minV);
    }

    public void RenderFullWestFace(Block block, double x, double y, double z, Icon icon) {
        Tessellator t = Tessellator.instance;
        t.setCurrentTextureName(icon != null ? icon.getIconName() : null);
        double minU = icon != null ? icon.getMinU() : 0;
        double maxU = icon != null ? icon.getMaxU() : 1;
        double minV = icon != null ? icon.getMinV() : 0;
        double maxV = icon != null ? icon.getMaxV() : 1;
        t.setNormal(-1, 0, 0);
        t.addVertexWithUV(x + 0, y + 1, z + 1, maxU, minV);
        t.addVertexWithUV(x + 0, y + 1, z + 0, minU, minV);
        t.addVertexWithUV(x + 0, y + 0, z + 0, minU, maxV);
        t.addVertexWithUV(x + 0, y + 0, z + 1, maxU, maxV);
    }

    public void RenderFullNorthFace(Block block, double x, double y, double z, Icon icon) {
        Tessellator t = Tessellator.instance;
        t.setCurrentTextureName(icon != null ? icon.getIconName() : null);
        double minU = icon != null ? icon.getMinU() : 0;
        double maxU = icon != null ? icon.getMaxU() : 1;
        double minV = icon != null ? icon.getMinV() : 0;
        double maxV = icon != null ? icon.getMaxV() : 1;
        t.setNormal(0, 0, -1);
        t.addVertexWithUV(x + 0, y + 1, z + 0, maxU, minV);
        t.addVertexWithUV(x + 1, y + 1, z + 0, minU, minV);
        t.addVertexWithUV(x + 1, y + 0, z + 0, minU, maxV);
        t.addVertexWithUV(x + 0, y + 0, z + 0, maxU, maxV);
    }

    public void RenderFullSouthFace(Block block, double x, double y, double z, Icon icon) {
        Tessellator t = Tessellator.instance;
        t.setCurrentTextureName(icon != null ? icon.getIconName() : null);
        double minU = icon != null ? icon.getMinU() : 0;
        double maxU = icon != null ? icon.getMaxU() : 1;
        double minV = icon != null ? icon.getMinV() : 0;
        double maxV = icon != null ? icon.getMaxV() : 1;
        t.setNormal(0, 0, 1);
        t.addVertexWithUV(x + 0, y + 1, z + 1, minU, minV);
        t.addVertexWithUV(x + 0, y + 0, z + 1, minU, maxV);
        t.addVertexWithUV(x + 1, y + 0, z + 1, maxU, maxV);
        t.addVertexWithUV(x + 1, y + 1, z + 1, maxU, minV);
    }

    // --- Block as item rendering ---

    public void renderBlockAsItem(Block block, int metadata, float brightness) {
        block.setBlockBoundsForItemRender();
        setRenderBoundsFromBlock(block);

        Tessellator t = Tessellator.instance;
        Icon icon;

        t.startDrawingQuads();
        t.setNormal(0, -1, 0);
        icon = getBlockIconFromSideAndMetadata(block, 0, metadata);
        renderFaceYNeg(block, 0, 0, 0, icon);

        t.setNormal(0, 1, 0);
        icon = getBlockIconFromSideAndMetadata(block, 1, metadata);
        renderFaceYPos(block, 0, 0, 0, icon);

        t.setNormal(0, 0, -1);
        icon = getBlockIconFromSideAndMetadata(block, 2, metadata);
        renderFaceZNeg(block, 0, 0, 0, icon);

        t.setNormal(0, 0, 1);
        icon = getBlockIconFromSideAndMetadata(block, 3, metadata);
        renderFaceZPos(block, 0, 0, 0, icon);

        t.setNormal(-1, 0, 0);
        icon = getBlockIconFromSideAndMetadata(block, 4, metadata);
        renderFaceXNeg(block, 0, 0, 0, icon);

        t.setNormal(1, 0, 0);
        icon = getBlockIconFromSideAndMetadata(block, 5, metadata);
        renderFaceXPos(block, 0, 0, 0, icon);
    }

    public void renderBlockAsItemVanilla(Block block, int metadata, float brightness) {
        block.setBlockBoundsForItemRender();
        setRenderBoundsFromBlock(block);

        Tessellator t = Tessellator.instance;
        Icon icon;

        t.startDrawingQuads();
        t.setNormal(0, -1, 0);
        icon = getBlockIconFromSideAndMetadata(block, 0, metadata);
        renderFaceYNeg(block, 0, 0, 0, icon);

        t.setNormal(0, 1, 0);
        icon = getBlockIconFromSideAndMetadata(block, 1, metadata);
        renderFaceYPos(block, 0, 0, 0, icon);

        t.setNormal(0, 0, -1);
        icon = getBlockIconFromSideAndMetadata(block, 2, metadata);
        renderFaceZNeg(block, 0, 0, 0, icon);

        t.setNormal(0, 0, 1);
        icon = getBlockIconFromSideAndMetadata(block, 3, metadata);
        renderFaceZPos(block, 0, 0, 0, icon);

        t.setNormal(-1, 0, 0);
        icon = getBlockIconFromSideAndMetadata(block, 4, metadata);
        renderFaceXNeg(block, 0, 0, 0, icon);

        t.setNormal(1, 0, 0);
        icon = getBlockIconFromSideAndMetadata(block, 5, metadata);
        renderFaceXPos(block, 0, 0, 0, icon);
    }

    // --- Falling block rendering ---

    public void renderBlockSandFalling(Block block, World world, int x, int y, int z, int metadata) {}

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

    public boolean ShouldSideBeRenderedBasedOnCurrentBounds(int x, int y, int z, int side) {
        return true;
    }

    // --- FC additions: falling block rendering ---

    public void RenderStandardFallingBlock(Block block, int x, int y, int z, int metadata) {}

    public void RenderStandardFullBlockMovedByPiston(Block block, int x, int y, int z) {}

    // --- FC additions: additional block renders ---

    public boolean RenderBlockFlowerpot(BlockFlowerPot block, int x, int y, int z) { return false; }
    public boolean RenderBlockHopper(Block block, int x, int y, int z) { return false; }

    // --- Static helpers ---

    public static boolean renderItemIn3d(int renderType) {
        return false;
    }

    public static boolean DoesRenderIDRenderItemIn3d(int renderType) {
        return false;
    }
}
