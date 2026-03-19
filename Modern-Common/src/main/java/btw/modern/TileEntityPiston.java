package btw.modern;

public class TileEntityPiston extends TileEntity {
    private int storedBlockID;
    private int storedMetadata;
    private int storedOrientation;
    private boolean extending;
    private boolean shouldHeadBeRendered;
    private float progress;
    private float lastProgress;

    public TileEntityPiston() {}

    public TileEntityPiston(int blockId, int metadata, int orientation, boolean extending, boolean headRendered) {
        this.storedBlockID = blockId;
        this.storedMetadata = metadata;
        this.storedOrientation = orientation;
        this.extending = extending;
        this.shouldHeadBeRendered = headRendered;
    }

    public TileEntityPiston(int blockId, int metadata, int orientation, boolean extending, boolean headRendered, boolean isStickyPiston) {
        this(blockId, metadata, orientation, extending, headRendered);
    }

    public int getStoredBlockID() { return storedBlockID; }
    public int getBlockMetadata() { return storedMetadata; }
    public boolean isExtending() { return extending; }
    public int getPistonOrientation() { return storedOrientation; }
    public float getProgress(float partialTick) { return progress; }
}
