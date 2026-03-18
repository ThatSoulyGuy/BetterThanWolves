package btw.api;

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
        return false;
    }

    // --- Standard block rendering ---

    public boolean renderStandardBlock(Block block, int x, int y, int z) {
        return false;
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
        return false;
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
    public boolean renderBlockTorch(Block block, int x, int y, int z) { return false; }
    public boolean renderBlockRepeater(BlockRedstoneRepeater block, int x, int y, int z) { return false; }
    public boolean renderBlockRedstoneWire(Block block, int x, int y, int z) { return false; }
    public boolean renderBlockMinecartTrack(BlockRailBase block, int x, int y, int z) { return false; }
    public boolean renderBlockLadder(Block block, int x, int y, int z) { return false; }
    public boolean renderBlockVine(Block block, int x, int y, int z) { return false; }
    public boolean renderBlockPane(BlockPane block, int x, int y, int z) { return false; }
    public boolean renderCrossedSquares(Block block, int x, int y, int z) { return false; }
    public boolean renderBlockStem(Block block, int x, int y, int z) { return false; }
    public boolean renderBlockCrops(Block block, int x, int y, int z) { return false; }
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
    public boolean RenderBlockBrewingStand(Object block, int x, int y, int z) { return false; }
    public boolean RenderBlockCocoa(Object block, int x, int y, int z) { return false; }
    public boolean RenderBlockAnvil(Object block, int x, int y, int z) { return false; }
    public boolean RenderBlockEndPortalFrame(Object block, int x, int y, int z) { return false; }

    // --- Torch / helper rendering ---

    public void renderTorchAtAngle(Block block, double x, double y, double z, double angleX, double angleZ, int metadata) {}
    public void drawCrossedSquares(Block block, int metadata, double x, double y, double z, float brightness) {}
    public void renderBlockStemSmall(Block block, int metadata, double x, double y, double z, double height) {}
    public void renderBlockStemBig(Object block, int metadata, int connectedBlockId, double x, double y, double z, double height) {}
    public void renderBlockCropsImpl(Block block, int metadata, double x, double y, double z) {}

    // --- Face rendering ---

    public void renderFaceYNeg(Block block, double x, double y, double z, Icon icon) {}
    public void renderFaceYPos(Block block, double x, double y, double z, Icon icon) {}
    public void renderFaceZNeg(Block block, double x, double y, double z, Icon icon) {}
    public void renderFaceZPos(Block block, double x, double y, double z, Icon icon) {}
    public void renderFaceXNeg(Block block, double x, double y, double z, Icon icon) {}
    public void renderFaceXPos(Block block, double x, double y, double z, Icon icon) {}

    // --- FC additions: full face rendering ---

    public void RenderFullBottomFace(Block block, double x, double y, double z, Icon icon) {}
    public void RenderFullTopFace(Block block, double x, double y, double z, Icon icon) {}
    public void RenderFullEastFace(Block block, double x, double y, double z, Icon icon) {}
    public void RenderFullWestFace(Block block, double x, double y, double z, Icon icon) {}
    public void RenderFullNorthFace(Block block, double x, double y, double z, Icon icon) {}
    public void RenderFullSouthFace(Block block, double x, double y, double z, Icon icon) {}

    // --- Block as item rendering ---

    public void renderBlockAsItem(Block block, int metadata, float brightness) {}
    public void renderBlockAsItemVanilla(Block block, int metadata, float brightness) {}

    // --- Falling block rendering ---

    public void renderBlockSandFalling(Block block, World world, int x, int y, int z, int metadata) {}

    // --- Icon helpers ---

    public Icon getBlockIcon(Block block, IBlockAccess blockAccess, int x, int y, int z, int side) {
        return null;
    }

    public Icon getBlockIconFromSideAndMetadata(Block block, int side, int metadata) {
        return null;
    }

    public Icon getBlockIconFromSide(Block block, int side) {
        return null;
    }

    public Icon getBlockIcon(Block block) {
        return null;
    }

    public Icon getIconSafe(Icon icon) {
        return icon;
    }

    // --- FC additions: ShouldSideBeRendered ---

    public boolean ShouldSideBeRenderedBasedOnCurrentBounds(int x, int y, int z, int side) {
        return true;
    }

    // --- FC additions: falling block rendering ---

    public boolean RenderStandardFallingBlock(Block block, int x, int y, int z, int metadata) {
        return false;
    }

    public boolean RenderStandardFullBlockMovedByPiston(Block block, int x, int y, int z) {
        return false;
    }

    // --- FC additions: additional block renders ---

    public boolean RenderBlockFlowerpot(Block block, int x, int y, int z) { return false; }
    public boolean RenderBlockHopper(Block block, int x, int y, int z) { return false; }

    // --- Static helpers ---

    public static boolean renderItemIn3d(int renderType) {
        return false;
    }

    public static boolean DoesRenderIDRenderItemIn3d(int renderType) {
        return false;
    }
}
