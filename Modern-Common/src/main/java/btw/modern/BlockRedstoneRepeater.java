package btw.modern;

public class BlockRedstoneRepeater extends BlockRedstoneLogic {

    // The powered flag is BlockRedstoneLogic's, as in 1.5.2 (legacy_icons).
    protected BlockRedstoneRepeater(int id, boolean powered) {
        super(id, Material.circuits);
        this.isRepeaterPowered = powered;
    }

    public int func_94481_j_(int metadata) { return 0; }

    // 1.5.2 BlockRedstoneRepeater.getRenderType (vanilla/client BlockRedstoneRepeater.java:78-81).
    // FCBlockRedstoneRepeater (93, 94) does not override it.
    @Override
    public int getRenderType() {
        return 15;
    }
}
