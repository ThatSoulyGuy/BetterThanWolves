package btw.modern;

import java.util.List;

public class AxisAlignedBB extends FCUtilsPrimitiveGeometric {

    public double minX;
    public double minY;
    public double minZ;
    public double maxX;
    public double maxY;
    public double maxZ;

    private static final AABBPool thePool = new AABBPool();

    public static AABBPool getAABBPool() {
        return thePool;
    }

    public AxisAlignedBB(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public static AxisAlignedBB getBoundingBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public AxisAlignedBB setBounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        return this;
    }

    public AxisAlignedBB expand(double x, double y, double z) {
        return new AxisAlignedBB(
            this.minX - x, this.minY - y, this.minZ - z,
            this.maxX + x, this.maxY + y, this.maxZ + z
        );
    }

    /**
     * Vanilla 1.5.2 AxisAlignedBB.offset MUTATES the box in place and
     * returns `this`. This is critical for Entity.moveEntity, which calls
     * `boundingBox.offset(0, dy, 0)` and expects the box to actually move.
     * The previous "return new AxisAlignedBB" stub was a phantom op — the
     * new box was thrown away and the original never moved, so positions
     * never updated and gravity could not pull entities down.
     */
    public AxisAlignedBB offset(double x, double y, double z) {
        this.minX += x;
        this.minY += y;
        this.minZ += z;
        this.maxX += x;
        this.maxY += y;
        this.maxZ += z;
        return this;
    }

    public boolean intersectsWith(AxisAlignedBB other) {
        return other.maxX > this.minX && other.minX < this.maxX
            && other.maxY > this.minY && other.minY < this.maxY
            && other.maxZ > this.minZ && other.minZ < this.maxZ;
    }

    public AxisAlignedBB copy() {
        return new AxisAlignedBB(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
    }

    public AxisAlignedBB addCoord(double x, double y, double z) {
        double newMinX = this.minX;
        double newMinY = this.minY;
        double newMinZ = this.minZ;
        double newMaxX = this.maxX;
        double newMaxY = this.maxY;
        double newMaxZ = this.maxZ;

        if (x < 0.0D) newMinX += x; else newMaxX += x;
        if (y < 0.0D) newMinY += y; else newMaxY += y;
        if (z < 0.0D) newMinZ += z; else newMaxZ += z;

        return new AxisAlignedBB(newMinX, newMinY, newMinZ, newMaxX, newMaxY, newMaxZ);
    }

    public boolean isVecInside(Vec3 vec) {
        return vec.xCoord > this.minX && vec.xCoord < this.maxX
            && vec.yCoord > this.minY && vec.yCoord < this.maxY
            && vec.zCoord > this.minZ && vec.zCoord < this.maxZ;
    }

    public AxisAlignedBB contract(double x, double y, double z) {
        return new AxisAlignedBB(
            this.minX + x, this.minY + y, this.minZ + z,
            this.maxX - x, this.maxY - y, this.maxZ - z
        );
    }

    public void AddToListIfIntersects(AxisAlignedBB intersectingBox, List list) {
        if (this.intersectsWith(intersectingBox)) {
            list.add(this);
        }
    }

    public double calculateXOffset(AxisAlignedBB other, double offset) {
        if (other.maxY > this.minY && other.minY < this.maxY && other.maxZ > this.minZ && other.minZ < this.maxZ) {
            if (offset > 0.0D && other.maxX <= this.minX) {
                double d = this.minX - other.maxX;
                if (d < offset) {
                    offset = d;
                }
            } else if (offset < 0.0D && other.minX >= this.maxX) {
                double d = this.maxX - other.minX;
                if (d > offset) {
                    offset = d;
                }
            }
        }
        return offset;
    }

    public double calculateYOffset(AxisAlignedBB other, double offset) {
        if (other.maxX > this.minX && other.minX < this.maxX && other.maxZ > this.minZ && other.minZ < this.maxZ) {
            if (offset > 0.0D && other.maxY <= this.minY) {
                double d = this.minY - other.maxY;
                if (d < offset) {
                    offset = d;
                }
            } else if (offset < 0.0D && other.minY >= this.maxY) {
                double d = this.maxY - other.minY;
                if (d > offset) {
                    offset = d;
                }
            }
        }
        return offset;
    }

    public double calculateZOffset(AxisAlignedBB other, double offset) {
        if (other.maxX > this.minX && other.minX < this.maxX && other.maxY > this.minY && other.minY < this.maxY) {
            if (offset > 0.0D && other.maxZ <= this.minZ) {
                double d = this.minZ - other.maxZ;
                if (d < offset) {
                    offset = d;
                }
            } else if (offset < 0.0D && other.minZ >= this.maxZ) {
                double d = this.maxZ - other.minZ;
                if (d > offset) {
                    offset = d;
                }
            }
        }
        return offset;
    }

    public AxisAlignedBB getOffsetBoundingBox(double x, double y, double z) {
        return new AxisAlignedBB(
            this.minX + x, this.minY + y, this.minZ + z,
            this.maxX + x, this.maxY + y, this.maxZ + z
        );
    }

    public MovingObjectPosition calculateIntercept(Vec3 start, Vec3 end) {
        Vec3 vecMinX = start.getIntermediateWithXValue(end, this.minX);
        Vec3 vecMaxX = start.getIntermediateWithXValue(end, this.maxX);
        Vec3 vecMinY = start.getIntermediateWithYValue(end, this.minY);
        Vec3 vecMaxY = start.getIntermediateWithYValue(end, this.maxY);
        Vec3 vecMinZ = start.getIntermediateWithZValue(end, this.minZ);
        Vec3 vecMaxZ = start.getIntermediateWithZValue(end, this.maxZ);

        if (!this.isVecInYZ(vecMinX)) {
            vecMinX = null;
        }

        if (!this.isVecInYZ(vecMaxX)) {
            vecMaxX = null;
        }

        if (!this.isVecInXZ(vecMinY)) {
            vecMinY = null;
        }

        if (!this.isVecInXZ(vecMaxY)) {
            vecMaxY = null;
        }

        if (!this.isVecInXY(vecMinZ)) {
            vecMinZ = null;
        }

        if (!this.isVecInXY(vecMaxZ)) {
            vecMaxZ = null;
        }

        Vec3 closest = null;

        if (vecMinX != null && (closest == null || start.squareDistanceTo(vecMinX) < start.squareDistanceTo(closest))) {
            closest = vecMinX;
        }

        if (vecMaxX != null && (closest == null || start.squareDistanceTo(vecMaxX) < start.squareDistanceTo(closest))) {
            closest = vecMaxX;
        }

        if (vecMinY != null && (closest == null || start.squareDistanceTo(vecMinY) < start.squareDistanceTo(closest))) {
            closest = vecMinY;
        }

        if (vecMaxY != null && (closest == null || start.squareDistanceTo(vecMaxY) < start.squareDistanceTo(closest))) {
            closest = vecMaxY;
        }

        if (vecMinZ != null && (closest == null || start.squareDistanceTo(vecMinZ) < start.squareDistanceTo(closest))) {
            closest = vecMinZ;
        }

        if (vecMaxZ != null && (closest == null || start.squareDistanceTo(vecMaxZ) < start.squareDistanceTo(closest))) {
            closest = vecMaxZ;
        }

        if (closest == null) {
            return null;
        }

        byte sideHit = -1;

        if (closest == vecMinX) {
            sideHit = 4;
        }

        if (closest == vecMaxX) {
            sideHit = 5;
        }

        if (closest == vecMinY) {
            sideHit = 0;
        }

        if (closest == vecMaxY) {
            sideHit = 1;
        }

        if (closest == vecMinZ) {
            sideHit = 2;
        }

        if (closest == vecMaxZ) {
            sideHit = 3;
        }

        return new MovingObjectPosition(0, 0, 0, sideHit, closest);
    }

    private boolean isVecInYZ(Vec3 vec) {
        return vec != null && vec.yCoord >= this.minY && vec.yCoord <= this.maxY && vec.zCoord >= this.minZ && vec.zCoord <= this.maxZ;
    }

    private boolean isVecInXZ(Vec3 vec) {
        return vec != null && vec.xCoord >= this.minX && vec.xCoord <= this.maxX && vec.zCoord >= this.minZ && vec.zCoord <= this.maxZ;
    }

    private boolean isVecInXY(Vec3 vec) {
        return vec != null && vec.xCoord >= this.minX && vec.xCoord <= this.maxX && vec.yCoord >= this.minY && vec.yCoord <= this.maxY;
    }

    public void RotateAroundJToFacing(int facing) {
        if (facing > 2) {
            if (facing == 5) { // i + 1
                setBounds(1.0D - maxZ, minY, 1.0D - maxX, 1.0D - minZ, maxY, 1.0D - minX);
            } else if (facing == 4) { // i - 1
                setBounds(minZ, minY, minX, maxZ, maxY, maxX);
            } else { // facing == 3, k + 1
                setBounds(1.0D - maxX, minY, 1.0D - maxZ, 1.0D - minX, maxY, 1.0D - minZ);
            }
        }
    }

    public void TiltToFacingAlongJ(int facing) {
        if (facing == 0) { // j - 1
            setBounds(1D - maxX, 1D - maxY, minZ, 1D - minX, 1D - minY, maxZ);
        } else if (facing == 2) { // k - 1
            setBounds(minX, minZ, 1D - maxY, maxX, maxZ, 1D - minY);
        } else if (facing == 3) { // k + 1
            setBounds(minX, 1D - maxZ, minY, maxX, 1D - minZ, maxY);
        } else if (facing == 4) { // i - 1
            setBounds(1D - maxY, minX, minZ, 1D - minY, maxX, maxZ);
        } else if (facing == 5) { // i + 1
            setBounds(minY, 1D - maxX, minZ, maxY, 1D - minX, maxZ);
        }
    }

    public AxisAlignedBB MakeTemporaryCopy() {
        return this.copy();
    }

    public void AddToRayTrace(Object rayTrace) {}

    public void Translate(double dDeltaX, double dDeltaY, double dDeltaZ) {
        this.minX += dDeltaX;
        this.minY += dDeltaY;
        this.minZ += dDeltaZ;
        this.maxX += dDeltaX;
        this.maxY += dDeltaY;
        this.maxZ += dDeltaZ;
    }

    public void ExpandToInclude(AxisAlignedBB other) {
        if (other.minX < this.minX) this.minX = other.minX;
        if (other.minY < this.minY) this.minY = other.minY;
        if (other.minZ < this.minZ) this.minZ = other.minZ;
        if (other.maxX > this.maxX) this.maxX = other.maxX;
        if (other.maxY > this.maxY) this.maxY = other.maxY;
        if (other.maxZ > this.maxZ) this.maxZ = other.maxZ;
    }

    // ================================================================
    // FCUtilsPrimitiveGeometric rendering — AxisAlignedBB acts as a box
    // primitive in FC's model system. FCModelBlock.AddBox() adds AABBs
    // to the primitives list, and the render methods iterate them.
    // ================================================================

    @Override
    public void RenderAsItemBlock(RenderBlocks renderBlocks, Block block, int iItemDamage) {
        Tessellator t = Tessellator.instance;

        renderBlocks.setRenderBounds(minX, minY, minZ, maxX, maxY, maxZ);

        // Use per-face icon lookup — block.getIcon(side, meta) returns
        // the correct texture per face (e.g., log top vs bark side)
        t.startDrawingQuads();
        t.setNormal(0, -1, 0);
        renderBlocks.renderFaceYNeg(block, 0, 0, 0, renderBlocks.getBlockIconFromSideAndMetadata(block, 0, iItemDamage));
        t.setNormal(0, 1, 0);
        renderBlocks.renderFaceYPos(block, 0, 0, 0, renderBlocks.getBlockIconFromSideAndMetadata(block, 1, iItemDamage));
        t.setNormal(0, 0, -1);
        renderBlocks.renderFaceZNeg(block, 0, 0, 0, renderBlocks.getBlockIconFromSideAndMetadata(block, 2, iItemDamage));
        t.setNormal(0, 0, 1);
        renderBlocks.renderFaceZPos(block, 0, 0, 0, renderBlocks.getBlockIconFromSideAndMetadata(block, 3, iItemDamage));
        t.setNormal(-1, 0, 0);
        renderBlocks.renderFaceXNeg(block, 0, 0, 0, renderBlocks.getBlockIconFromSideAndMetadata(block, 4, iItemDamage));
        t.setNormal(1, 0, 0);
        renderBlocks.renderFaceXPos(block, 0, 0, 0, renderBlocks.getBlockIconFromSideAndMetadata(block, 5, iItemDamage));
        t.draw();
    }

    @Override
    public boolean RenderAsBlock(RenderBlocks renderBlocks, Block block, int i, int j, int k) {
        renderBlocks.setRenderBounds(minX, minY, minZ, maxX, maxY, maxZ);

        int meta = 0;
        if (renderBlocks.blockAccess != null) {
            meta = renderBlocks.blockAccess.getBlockMetadata(i, j, k);
        }

        Tessellator t = Tessellator.instance;
        t.setColorOpaque_F(1, 1, 1);

        renderBlocks.renderFaceYNeg(block, i, j, k, renderBlocks.getBlockIconFromSideAndMetadata(block, 0, meta));
        renderBlocks.renderFaceYPos(block, i, j, k, renderBlocks.getBlockIconFromSideAndMetadata(block, 1, meta));
        renderBlocks.renderFaceZNeg(block, i, j, k, renderBlocks.getBlockIconFromSideAndMetadata(block, 2, meta));
        renderBlocks.renderFaceZPos(block, i, j, k, renderBlocks.getBlockIconFromSideAndMetadata(block, 3, meta));
        renderBlocks.renderFaceXNeg(block, i, j, k, renderBlocks.getBlockIconFromSideAndMetadata(block, 4, meta));
        renderBlocks.renderFaceXPos(block, i, j, k, renderBlocks.getBlockIconFromSideAndMetadata(block, 5, meta));

        return true;
    }

    @Override
    public boolean RenderAsBlockWithColorMultiplier(RenderBlocks renderBlocks, Block block, int i, int j, int k,
            float fRed, float fGreen, float fBlue) {
        // Mirrors RenderAsBlock but tints the faces with the supplied color multiplier.
        // Was inherited as a no-op (FCUtilsPrimitiveGeometric stub), so any FC block that
        // draws boxes through the color-multiplier path (e.g. the wicker basket lid/handle
        // /interior) emitted zero geometry and rendered invisible.
        renderBlocks.setRenderBounds(minX, minY, minZ, maxX, maxY, maxZ);

        int meta = 0;
        if (renderBlocks.blockAccess != null) {
            meta = renderBlocks.blockAccess.getBlockMetadata(i, j, k);
        }

        Tessellator t = Tessellator.instance;
        t.setColorOpaque_F(fRed, fGreen, fBlue);

        renderBlocks.renderFaceYNeg(block, i, j, k, renderBlocks.getBlockIconFromSideAndMetadata(block, 0, meta));
        renderBlocks.renderFaceYPos(block, i, j, k, renderBlocks.getBlockIconFromSideAndMetadata(block, 1, meta));
        renderBlocks.renderFaceZNeg(block, i, j, k, renderBlocks.getBlockIconFromSideAndMetadata(block, 2, meta));
        renderBlocks.renderFaceZPos(block, i, j, k, renderBlocks.getBlockIconFromSideAndMetadata(block, 3, meta));
        renderBlocks.renderFaceXNeg(block, i, j, k, renderBlocks.getBlockIconFromSideAndMetadata(block, 4, meta));
        renderBlocks.renderFaceXPos(block, i, j, k, renderBlocks.getBlockIconFromSideAndMetadata(block, 5, meta));

        return true;
    }

    @Override
    public boolean RenderAsBlockWithTexture(RenderBlocks renderBlocks, Block block, int i, int j, int k, Icon icon) {
        renderBlocks.setRenderBounds(minX, minY, minZ, maxX, maxY, maxZ);

        Tessellator t = Tessellator.instance;
        t.setColorOpaque_F(1, 1, 1);

        renderBlocks.renderFaceYNeg(block, i, j, k, icon);
        renderBlocks.renderFaceYPos(block, i, j, k, icon);
        renderBlocks.renderFaceZNeg(block, i, j, k, icon);
        renderBlocks.renderFaceZPos(block, i, j, k, icon);
        renderBlocks.renderFaceXNeg(block, i, j, k, icon);
        renderBlocks.renderFaceXPos(block, i, j, k, icon);

        return true;
    }

    @Override
    public boolean RenderAsBlockFullBrightWithTexture(RenderBlocks renderBlocks, Block block, int i, int j, int k, Icon icon) {
        return RenderAsBlockWithTexture(renderBlocks, block, i, j, k, icon);
    }

    @Override
    public void RenderAsFallingBlock(RenderBlocks renderBlocks, Block block, int i, int j, int k, int iMetadata) {
        RenderAsBlock(renderBlocks, block, i, j, k);
    }

    /** Assembly ID for model primitives — identifies which icon index to use. */
    private int m_iAssemblyID = -1;

    @Override
    public int GetAssemblyID() { return m_iAssemblyID; }

    public void SetAssemblyID(int id) { m_iAssemblyID = id; }

    /**
     * Vanilla 1.5.2 AxisAlignedBB.getAverageEdgeLength — returns the
     * arithmetic mean of the box's three edge lengths. Used by
     * {@link World#func_85174_u} to detect "full cube" collision boxes.
     */
    public double getAverageEdgeLength() {
        return ((maxX - minX) + (maxY - minY) + (maxZ - minZ)) / 3.0D;
    }

    /**
     * Vanilla 1.5.2 AxisAlignedBB.setBB — copies the bounds from another
     * box into this one in place. Called by Entity.moveEntity to swap the
     * entity's bounding box with a swept/clipped one without allocating
     * a new instance.
     */
    public void setBB(AxisAlignedBB other) {
        this.minX = other.minX;
        this.minY = other.minY;
        this.minZ = other.minZ;
        this.maxX = other.maxX;
        this.maxY = other.maxY;
        this.maxZ = other.maxZ;
    }
}
