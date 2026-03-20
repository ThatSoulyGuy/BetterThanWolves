package btw.forge.client;

import btw.forge.NamedIcon;
import btw.modern.Block;
import btw.modern.Icon;

import java.util.ArrayList;
import java.util.List;

/**
 * A RenderBlocks subclass that captures geometry instead of rendering it.
 *
 * <p>When FC block code calls {@code setRenderBounds}, {@code renderStandardBlock},
 * {@code RenderStandardFullBlock}, {@code renderFaceYNeg}, etc., this class
 * records the box coordinates and per-face texture names. The captured data
 * is used by {@link FCBakedModel} to generate BakedQuad objects for MC's
 * chunk renderer.
 */
public class RecordingRenderBlocks extends btw.modern.RenderBlocks {

    // Current render bounds (captured from setRenderBounds calls)
    private double curMinX = 0, curMinY = 0, curMinZ = 0;
    private double curMaxX = 1, curMaxY = 1, curMaxZ = 1;

    /** Metadata value currently being rendered. Set by the caller before invoking FC render code. */
    private int currentMeta;

    /** Accumulated recorded boxes for the current render pass. */
    private final List<RecordedBox> boxes = new ArrayList<>();

    /**
     * Tracks individual face renders (renderFaceYNeg, renderFaceYPos, etc.).
     * Some blocks render faces one-by-one instead of calling renderStandardBlock.
     * We accumulate faces keyed by the current bounds and flush them into a box
     * when the bounds change or when results are collected.
     */
    private final boolean[] pendingFaces = new boolean[6];
    private final String[] pendingFaceTextures = new String[6];
    private boolean hasPendingFaces = false;
    private double pendingMinX, pendingMinY, pendingMinZ;
    private double pendingMaxX, pendingMaxY, pendingMaxZ;

    public RecordingRenderBlocks() {
        super();
        // Ensure all faces are rendered during recording
        SetRenderAllFaces(true);
    }

    // ================================================================
    // Metadata control
    // ================================================================

    public void setCurrentMeta(int meta) {
        this.currentMeta = meta;
    }

    public int getCurrentMeta() {
        return currentMeta;
    }

    // ================================================================
    // Results
    // ================================================================

    /** Returns all recorded boxes and clears the internal list. */
    public List<RecordedBox> getAndClearBoxes() {
        flushPendingFaces();
        List<RecordedBox> result = new ArrayList<>(boxes);
        boxes.clear();
        return result;
    }

    /** Clears recorded boxes without returning them. */
    public void clear() {
        boxes.clear();
        hasPendingFaces = false;
    }

    // ================================================================
    // setRenderBounds overrides — capture current bounds
    // ================================================================

    @Override
    public void setRenderBounds(double minX, double minY, double minZ,
                                double maxX, double maxY, double maxZ) {
        super.setRenderBounds(minX, minY, minZ, maxX, maxY, maxZ);
        // If pending faces exist with different bounds, flush them first
        if (hasPendingFaces && !sameBounds(minX, minY, minZ, maxX, maxY, maxZ)) {
            flushPendingFaces();
        }
        this.curMinX = minX;
        this.curMinY = minY;
        this.curMinZ = minZ;
        this.curMaxX = maxX;
        this.curMaxY = maxY;
        this.curMaxZ = maxZ;
    }

    @Override
    public void setRenderBounds(btw.modern.AxisAlignedBB bounds) {
        if (bounds != null) {
            setRenderBounds(bounds.minX, bounds.minY, bounds.minZ,
                            bounds.maxX, bounds.maxY, bounds.maxZ);
        }
    }

    @Override
    public void setRenderBoundsFromBlock(Block block) {
        setRenderBounds(
            block.getBlockBoundsMinX(), block.getBlockBoundsMinY(), block.getBlockBoundsMinZ(),
            block.getBlockBoundsMaxX(), block.getBlockBoundsMaxY(), block.getBlockBoundsMaxZ()
        );
    }

    @Override
    public void overrideBlockBounds(double minX, double minY, double minZ,
                                    double maxX, double maxY, double maxZ) {
        super.overrideBlockBounds(minX, minY, minZ, maxX, maxY, maxZ);
        if (hasPendingFaces && !sameBounds(minX, minY, minZ, maxX, maxY, maxZ)) {
            flushPendingFaces();
        }
        this.curMinX = minX;
        this.curMinY = minY;
        this.curMinZ = minZ;
        this.curMaxX = maxX;
        this.curMaxY = maxY;
        this.curMaxZ = maxZ;
    }

    // ================================================================
    // Standard block rendering — captures a full box with per-face textures
    // ================================================================

    @Override
    public boolean renderStandardBlock(Block block, int x, int y, int z) {
        flushPendingFaces();
        recordFullBox(block);
        return true;
    }

    @Override
    public boolean RenderStandardFullBlock(Block block, int x, int y, int z) {
        flushPendingFaces();
        recordFullBox(block);
        return true;
    }

    @Override
    public boolean RenderStandardFullBlockWithAmbientOcclusion(Block block, int x, int y, int z) {
        flushPendingFaces();
        recordFullBox(block);
        return true;
    }

    @Override
    public boolean RenderStandardFullBlockWithColorMultiplier(Block block, int x, int y, int z) {
        flushPendingFaces();
        recordFullBox(block);
        return true;
    }

    @Override
    public boolean renderStandardBlockWithAmbientOcclusion(Block block, int x, int y, int z,
                                                           float r, float g, float b) {
        flushPendingFaces();
        recordFullBox(block);
        return true;
    }

    @Override
    public boolean renderStandardBlockWithColorMultiplier(Block block, int x, int y, int z,
                                                          float r, float g, float b) {
        flushPendingFaces();
        recordFullBox(block);
        return true;
    }

    @Override
    public boolean func_102027_b(Block block, int x, int y, int z,
                                  float r, float g, float b) {
        flushPendingFaces();
        recordFullBox(block);
        return true;
    }

    // ================================================================
    // Specific block type renders — treated as standard blocks
    // ================================================================

    @Override
    public boolean renderBlockLog(Block block, int x, int y, int z) {
        flushPendingFaces();
        recordFullBox(block);
        return true;
    }

    @Override
    public boolean renderBlockQuartz(Block block, int x, int y, int z) {
        flushPendingFaces();
        recordFullBox(block);
        return true;
    }

    @Override
    public boolean renderBlockCactus(Block block, int x, int y, int z) {
        flushPendingFaces();
        recordFullBox(block);
        return true;
    }

    @Override
    public boolean renderBlockCactusImpl(Block block, int x, int y, int z,
                                          float r, float g, float b) {
        flushPendingFaces();
        recordFullBox(block);
        return true;
    }

    @Override
    public boolean renderBlockByRenderType(Block block, int x, int y, int z) {
        flushPendingFaces();
        recordFullBox(block);
        return true;
    }

    @Override
    public void renderBlockAsItem(Block block, int metadata, float brightness) {
        recordFullBox(block);
    }

    @Override
    public void renderBlockAsItemVanilla(Block block, int metadata, float brightness) {
        recordFullBox(block);
    }

    // Grass block rendering — record as standard boxes
    @Override
    public boolean renderGrassBlockWithAmbientOcclusion(Block block, int x, int y, int z,
                                                         float r, float g, float b, Icon sideOverlayIcon) {
        flushPendingFaces();
        recordFullBox(block);
        return true;
    }

    @Override
    public boolean renderGrassBlockWithColorMultiplier(Block block, int x, int y, int z,
                                                        float r, float g, float b, Icon sideOverlayIcon) {
        flushPendingFaces();
        recordFullBox(block);
        return true;
    }

    // ================================================================
    // Individual face rendering — accumulate faces, flush as a box
    // ================================================================

    @Override
    public void renderFaceYNeg(Block block, double x, double y, double z, Icon icon) {
        recordFace(0, icon, block);
    }

    @Override
    public void renderFaceYPos(Block block, double x, double y, double z, Icon icon) {
        recordFace(1, icon, block);
    }

    @Override
    public void renderFaceZNeg(Block block, double x, double y, double z, Icon icon) {
        recordFace(2, icon, block);
    }

    @Override
    public void renderFaceZPos(Block block, double x, double y, double z, Icon icon) {
        recordFace(3, icon, block);
    }

    @Override
    public void renderFaceXNeg(Block block, double x, double y, double z, Icon icon) {
        recordFace(4, icon, block);
    }

    @Override
    public void renderFaceXPos(Block block, double x, double y, double z, Icon icon) {
        recordFace(5, icon, block);
    }

    // FC full-face renders
    @Override
    public void RenderFullBottomFace(Block block, double x, double y, double z, Icon icon) {
        recordFace(0, icon, block);
    }

    @Override
    public void RenderFullTopFace(Block block, double x, double y, double z, Icon icon) {
        recordFace(1, icon, block);
    }

    @Override
    public void RenderFullEastFace(Block block, double x, double y, double z, Icon icon) {
        // FC "East" = MC side 5 (positive X)
        recordFace(5, icon, block);
    }

    @Override
    public void RenderFullWestFace(Block block, double x, double y, double z, Icon icon) {
        // FC "West" = MC side 4 (negative X)
        recordFace(4, icon, block);
    }

    @Override
    public void RenderFullNorthFace(Block block, double x, double y, double z, Icon icon) {
        // FC "North" = MC side 2 (negative Z)
        recordFace(2, icon, block);
    }

    @Override
    public void RenderFullSouthFace(Block block, double x, double y, double z, Icon icon) {
        // FC "South" = MC side 3 (positive Z)
        recordFace(3, icon, block);
    }

    // ================================================================
    // Texture/icon rendering (renderBlockUsingTexture, renderBlockAllFaces)
    // ================================================================

    @Override
    public void renderBlockUsingTexture(Block block, int x, int y, int z, Icon icon) {
        flushPendingFaces();
        // Record a full box using the override texture for all faces
        String texName = resolveIconName(icon);
        String[] faceTextures = new String[6];
        for (int i = 0; i < 6; i++) {
            faceTextures[i] = texName;
        }
        boxes.add(new RecordedBox(curMinX, curMinY, curMinZ, curMaxX, curMaxY, curMaxZ, faceTextures));
    }

    @Override
    public void renderBlockAllFaces(Block block, int x, int y, int z) {
        flushPendingFaces();
        recordFullBox(block);
    }

    // ================================================================
    // Icon helpers — return NamedIcon-based results
    // ================================================================

    @Override
    public Icon getBlockIcon(Block block, btw.modern.IBlockAccess blockAccess, int x, int y, int z, int side) {
        if (hasOverrideBlockTexture()) {
            return GetOverrideTexture();
        }
        return block.getIcon(side, currentMeta);
    }

    @Override
    public Icon getBlockIconFromSideAndMetadata(Block block, int side, int metadata) {
        if (hasOverrideBlockTexture()) {
            return GetOverrideTexture();
        }
        return block.getIcon(side, metadata);
    }

    @Override
    public Icon getBlockIconFromSide(Block block, int side) {
        if (hasOverrideBlockTexture()) {
            return GetOverrideTexture();
        }
        return block.getIcon(side, currentMeta);
    }

    @Override
    public Icon getBlockIcon(Block block) {
        if (hasOverrideBlockTexture()) {
            return GetOverrideTexture();
        }
        return block.getIcon(0, currentMeta);
    }

    @Override
    public Icon getIconSafe(Icon icon) {
        return icon != null ? icon : new NamedIcon("unknown");
    }

    @Override
    public boolean ShouldSideBeRenderedBasedOnCurrentBounds(int x, int y, int z, int side) {
        return true; // Always render all sides during recording
    }

    // FC additions: falling/piston rendering — treat as standard blocks
    @Override
    public void RenderStandardFallingBlock(Block block, int x, int y, int z, int metadata) {
        recordFullBox(block);
    }

    @Override
    public void RenderStandardFullBlockMovedByPiston(Block block, int x, int y, int z) {
        recordFullBox(block);
    }

    // ================================================================
    // Internal helpers
    // ================================================================

    /** Records a full box with per-face textures from block.getIcon(). */
    private void recordFullBox(Block block) {
        String[] faceTextures = new String[6];
        for (int side = 0; side < 6; side++) {
            Icon icon;
            if (hasOverrideBlockTexture()) {
                icon = GetOverrideTexture();
            } else {
                try {
                    icon = block.getIcon(side, currentMeta);
                } catch (Exception e) {
                    icon = null;
                }
            }
            faceTextures[side] = resolveIconName(icon);
        }
        boxes.add(new RecordedBox(curMinX, curMinY, curMinZ, curMaxX, curMaxY, curMaxZ, faceTextures));
    }

    /** Records a single face for later aggregation into a box. */
    private void recordFace(int side, Icon icon, Block block) {
        // If we're starting a new face set or bounds changed, flush pending
        if (hasPendingFaces && !sameBounds(curMinX, curMinY, curMinZ, curMaxX, curMaxY, curMaxZ)) {
            flushPendingFaces();
        }

        if (!hasPendingFaces) {
            hasPendingFaces = true;
            pendingMinX = curMinX;
            pendingMinY = curMinY;
            pendingMinZ = curMinZ;
            pendingMaxX = curMaxX;
            pendingMaxY = curMaxY;
            pendingMaxZ = curMaxZ;
            for (int i = 0; i < 6; i++) {
                pendingFaces[i] = false;
                pendingFaceTextures[i] = null;
            }
        }

        pendingFaces[side] = true;
        pendingFaceTextures[side] = resolveIconName(icon);
    }

    /** Flushes accumulated individual face renders into a RecordedBox. */
    private void flushPendingFaces() {
        if (!hasPendingFaces) return;

        // Fill in any missing faces with "unknown"
        String[] faceTextures = new String[6];
        for (int i = 0; i < 6; i++) {
            faceTextures[i] = pendingFaceTextures[i] != null ? pendingFaceTextures[i] : "unknown";
        }

        boxes.add(new RecordedBox(pendingMinX, pendingMinY, pendingMinZ,
                                  pendingMaxX, pendingMaxY, pendingMaxZ, faceTextures));
        hasPendingFaces = false;
    }

    /** Checks if the given bounds match the pending face bounds. */
    private boolean sameBounds(double minX, double minY, double minZ,
                               double maxX, double maxY, double maxZ) {
        return Double.compare(minX, pendingMinX) == 0
            && Double.compare(minY, pendingMinY) == 0
            && Double.compare(minZ, pendingMinZ) == 0
            && Double.compare(maxX, pendingMaxX) == 0
            && Double.compare(maxY, pendingMaxY) == 0
            && Double.compare(maxZ, pendingMaxZ) == 0;
    }

    /** Resolves an Icon to its texture name string. */
    private static String resolveIconName(Icon icon) {
        if (icon instanceof NamedIcon) {
            return ((NamedIcon) icon).getIconName();
        }
        if (icon != null && icon.getIconName() != null) {
            return icon.getIconName().toLowerCase();
        }
        return "unknown";
    }

    // ================================================================
    // RecordedBox — one box element with per-face textures
    // ================================================================

    /**
     * Represents a single box element captured during recording.
     * Stores from/to coordinates (0-1 range) and a texture name for each of the 6 faces.
     *
     * <p>Face indices follow MC convention:
     * 0=down (Y-), 1=up (Y+), 2=north (Z-), 3=south (Z+), 4=west (X-), 5=east (X+)
     */
    public static class RecordedBox {
        public final double minX, minY, minZ;
        public final double maxX, maxY, maxZ;
        /** Texture names per face: [down, up, north, south, west, east] */
        public final String[] faceTextures;

        public RecordedBox(double minX, double minY, double minZ,
                           double maxX, double maxY, double maxZ,
                           String[] faceTextures) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
            this.faceTextures = faceTextures.clone();
        }
    }
}
