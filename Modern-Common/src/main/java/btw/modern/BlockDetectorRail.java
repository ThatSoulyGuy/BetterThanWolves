package btw.modern;

public class BlockDetectorRail extends BlockRailBase {

    // 1.5.2 BlockDetectorRail (vanilla/client BlockDetectorRail.java:24) passes
    // isPowered=true — detector rails ARE powered-type rails. RenderBlocks.
    // renderBlockMinecartTrack does `if (block.isPowered()) meta &= 7;`, so a
    // false flag renders the wrong rail metadata.
    protected BlockDetectorRail(int id) {
        super(id, true);
        this.setTickRandomly(true);
    }
}
