package btw.modern;

public class BlockRail extends BlockRailBase {

    protected BlockRail(int id) {
        super(id);
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockRail's icon members (vanilla/client BlockRail.java:19,
    // :29-42): the straight rail (Block's name, "rail") and the curve for metas 6-9. FCBlockRailRegular (66) inherits
    // both; a curved rail lifted by a platform drew straight.
    private Icon railTurnIcon;

    // 1.5.2 BlockRail.getIcon (vanilla/client BlockRail.java:29-32); Block's icon until registerIcons runs (client
    // only)
    @Override
    public Icon getIcon(int side, int metadata) {
        if (this.railTurnIcon == null) {
            return super.getIcon(side, metadata);
        }
        return metadata >= 6 ? this.railTurnIcon : this.blockIcon;
    }

    // 1.5.2 BlockRail.registerIcons (vanilla/client BlockRail.java:38-42)
    @Override
    public void registerIcons(IconRegister register) {
        super.registerIcons(register);
        this.railTurnIcon = register.registerIcon("rail_turn");
    }
}
