package btw.modern;

public class BlockComparator extends BlockRedstoneLogic {

    protected BlockComparator(int id) {
        super(id, Material.circuits);
    }

    // 1.5.2 BlockComparator(int, boolean) passes the powered flag to BlockRedstoneLogic (vanilla/client
    // BlockComparator.java:21-25), where getIcon and registerIcons read it (legacy_icons).
    protected BlockComparator(int id, boolean powered) {
        super(id, Material.circuits);
        this.isRepeaterPowered = powered;
    }

    public int func_94481_j_(int metadata) { return 0; }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockComparator's icon members (vanilla/client
    // BlockComparator.java:69-73, :266-269): a redstone torch below; on top the comparator, or the lit comparator's
    // top when it is powered or its metadata bit 8 is set; stone slab sides. Without them a comparator would inherit
    // the repeater's.
    // FCBlockComparator (149, 150) registers and answers its own. Block.redstoneComparatorActive is read at its id, 150
    // (Modern-Common keeps no such field).
    @Override
    public Icon getIcon(int side, int metadata) {
        boolean lit = this.isRepeaterPowered || (metadata & 8) != 0;
        Block active = Block.blocksList[150];
        return side == 0 ? (lit ? Block.torchRedstoneActive.getBlockTextureFromSide(side)
                        : Block.torchRedstoneIdle.getBlockTextureFromSide(side))
                : (side == 1 ? (lit && active != null ? active.blockIcon : this.blockIcon)
                : Block.stoneDoubleSlab.getBlockTextureFromSide(1));
    }

    // 1.5.2 BlockComparator.registerIcons (vanilla/client BlockComparator.java:266-269)
    @Override
    public void registerIcons(IconRegister register) {
        this.blockIcon = register.registerIcon(this.isRepeaterPowered ? "comparator_lit" : "comparator");
    }
}
