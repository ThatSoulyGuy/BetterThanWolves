package btw.api;

public abstract class BlockRailBase extends Block {

    protected BlockRailBase(int id) {
        super(id, Material.circuits);
    }

    public boolean isFlexibleRail() {
        return false;
    }
}
