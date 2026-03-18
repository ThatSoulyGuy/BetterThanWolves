package btw.api;

public class BlockRedstoneRepeater extends BlockRedstoneLogic {

    public boolean isRepeaterPowered;

    protected BlockRedstoneRepeater(int id, boolean powered) {
        super(id, Material.circuits);
        this.isRepeaterPowered = powered;
    }

    public int func_94481_j_(int metadata) { return 0; }
}
