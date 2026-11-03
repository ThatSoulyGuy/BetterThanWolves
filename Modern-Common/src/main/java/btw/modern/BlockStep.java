package btw.modern;

public class BlockStep extends BlockHalfSlab {

    protected BlockStep(int id, boolean isDouble) {
        super(id, isDouble, Material.rock);
    }

    public String getFullSlabName(int metadata) {
        return "";
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockStep's icon members (vanilla/client BlockStep.java:24,
    // :35-55): the stone slab's top and side, the other types' own blocks (sandstone, planks, cobblestone, brick,
    // stone brick, nether brick, quartz), the top on every face of a full double slab. FCBlockStep (43, 44) inherits
    // both; a held slab drew Block's default, the unlocalized "stoneSlab", which names no texture.
    private Icon slabSideIcon;

    // 1.5.2 BlockStep.getIcon (vanilla/client BlockStep.java:35-45); Block's icon for the stone type until
    // registerIcons runs (client only)
    @Override
    public Icon getIcon(int side, int metadata) {
        int type = metadata & 7;
        if (type == 0 && this.slabSideIcon == null) {
            return super.getIcon(side, metadata);
        }
        if (this.isDoubleSlab && (metadata & 8) != 0) {
            side = 1;
        }
        return type == 0 ? (side != 1 && side != 0 ? this.slabSideIcon : this.blockIcon)
                : (type == 1 ? Block.sandStone.getBlockTextureFromSide(side)
                : (type == 2 ? Block.planks.getBlockTextureFromSide(side)
                : (type == 3 ? Block.cobblestone.getBlockTextureFromSide(side)
                : (type == 4 ? Block.brick.getBlockTextureFromSide(side)
                : (type == 5 ? Block.stoneBrick.getIcon(side, 0)
                : (type == 6 ? Block.netherBrick.getBlockTextureFromSide(1)
                : (type == 7 ? Block.blockNetherQuartz.getBlockTextureFromSide(side) : this.blockIcon)))))));
    }

    // 1.5.2 BlockStep.registerIcons (vanilla/client BlockStep.java:51-55)
    @Override
    public void registerIcons(IconRegister register) {
        this.blockIcon = register.registerIcon("stoneslab_top");
        this.slabSideIcon = register.registerIcon("stoneslab_side");
    }
}
