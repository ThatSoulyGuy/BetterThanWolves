package btw.modern;

public class BlockWorkbench extends Block {

    protected BlockWorkbench(int id) {
        super(id, Material.wood);
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockWorkbench's icon members (vanilla/client
    // BlockWorkbench.java:19-20, :31-34, :40-45): the top, the plank bottom, the front on north and west, the side
    // on south and east. FCBlockWorkbench (58, 1034) inherits both; Block's default registered the unlocalized
    // "workbench", the front on every face.
    private Icon workbenchIconTop;
    private Icon workbenchIconFront;

    // 1.5.2 BlockWorkbench.getIcon (vanilla/client BlockWorkbench.java:31-34); Block's icon until registerIcons runs
    // (client only)
    @Override
    public Icon getIcon(int side, int metadata) {
        if (this.workbenchIconTop == null || this.workbenchIconFront == null) {
            return super.getIcon(side, metadata);
        }
        return side == 1 ? this.workbenchIconTop
                : (side == 0 ? Block.planks.getBlockTextureFromSide(side)
                : (side != 2 && side != 4 ? this.blockIcon : this.workbenchIconFront));
    }

    // 1.5.2 BlockWorkbench.registerIcons (vanilla/client BlockWorkbench.java:40-45)
    @Override
    public void registerIcons(IconRegister register) {
        this.blockIcon = register.registerIcon("workbench_side");
        this.workbenchIconTop = register.registerIcon("workbench_top");
        this.workbenchIconFront = register.registerIcon("workbench_front");
    }
}
