package btw.api;

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
    public boolean RenderAsBlockWithColorMultiplier(RenderBlocks renderBlocks, Block block, int i, int j, int k) { return false; }
    public boolean RenderAsBlockWithTexture(RenderBlocks renderBlocks, Block block, int i, int j, int k, Icon icon) { return false; }
    public boolean RenderAsBlockFullBrightWithTexture(RenderBlocks renderBlocks, Block block, int i, int j, int k, Icon icon) { return false; }
    public void RenderAsItemBlock(RenderBlocks renderBlocks, Block block, int iItemDamage) {}
    public void RenderAsFallingBlock(RenderBlocks renderBlocks, Block block, int i, int j, int k, int iMetadata) {}
}
