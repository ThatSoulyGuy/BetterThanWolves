package btw.modern;

public abstract class BlockRailBase extends Block {

    protected BlockRailBase(int id) {
        super(id, Material.circuits);
    }

    public boolean isFlexibleRail() {
        return false;
    }
}
