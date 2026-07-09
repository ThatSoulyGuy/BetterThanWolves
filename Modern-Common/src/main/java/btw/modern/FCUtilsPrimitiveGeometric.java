package btw.modern;

import java.util.List;

public abstract class FCUtilsPrimitiveGeometric {
    public abstract FCUtilsPrimitiveGeometric MakeTemporaryCopy();
    public abstract void RotateAroundJToFacing(int iFacing);
    public abstract void TiltToFacingAlongJ(int iFacing);
    public abstract void AddToRayTrace(Object rayTrace);
    public abstract void Translate(double dDeltaX, double dDeltaY, double dDeltaZ);
    public void AddIntersectingBoxesToCollisionList(World world, int i, int j, int k, AxisAlignedBB boxToIntersect, List collisionList) {}
    public int GetAssemblyID() { return -1; }
    public boolean RenderAsBlock(RenderBlocks renderBlocks, Block block, int i, int j, int k) { return false; }
    public boolean RenderAsBlockWithColorMultiplier(RenderBlocks renderBlocks, Block block, int i, int j, int k, float fRed, float fGreen, float fBlue) { return false; }

    // Faithful port of FC FCUtilsPrimitiveGeometric: the no-arg-color variant derives
    // the tint from the block's colorMultiplier and delegates to the 8-arg variant.
    // Was a no-op stub, which is why the wicker basket's lid/handle/interior (drawn via
    // this path) emitted zero vertices and rendered invisible.
    public boolean RenderAsBlockWithColorMultiplier(RenderBlocks renderBlocks, Block block, int i, int j, int k) {
        int iColorMultiplier = block.colorMultiplier(renderBlocks.blockAccess, i, j, k);

        float fRed = (float) (iColorMultiplier >> 16 & 255) / 255F;
        float fGreen = (float) (iColorMultiplier >> 8 & 255) / 255F;
        float fBlue = (float) (iColorMultiplier & 255) / 255F;

        return RenderAsBlockWithColorMultiplier(renderBlocks, block, i, j, k, fRed, fGreen, fBlue);
    }
    public boolean RenderAsBlockWithTexture(RenderBlocks renderBlocks, Block block, int i, int j, int k, Icon icon) { return false; }
    public boolean RenderAsBlockFullBrightWithTexture(RenderBlocks renderBlocks, Block block, int i, int j, int k, Icon icon) { return false; }
    public void RenderAsItemBlock(RenderBlocks renderBlocks, Block block, int iItemDamage) {}
    public void RenderAsFallingBlock(RenderBlocks renderBlocks, Block block, int i, int j, int k, int iMetadata) {}
}
