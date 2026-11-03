package btw.modern;

public class BlockIce extends BlockBreakable {

    // 1.5.2 BlockIce constructor sets the random-tick flag (vanilla/server BlockIce.java:24); FCBlockIce.updateTick melts on it
    // It passes its icon name "ice" (vanilla/client BlockIce.java:21-23; legacy_icons, BlockBreakable).
    protected BlockIce(int id) {
        super(id, "ice", Material.ice, false);
        this.setTickRandomly(true);
    }

    // 1.5.2 BlockIce.getRenderBlockPass (vanilla/client BlockIce.java:32-35): FCBlockIce (79) does not override it.
    @Override
    public int getRenderBlockPass() {
        return 1;
    }
}
