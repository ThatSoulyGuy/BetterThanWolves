package btw.modern;

public class BlockBed extends BlockDirectional {

    protected BlockBed(int id) {
        super(id, Material.cloth);
    }

    // 1.5.2 BlockBed.isBlockHeadOfBed (vanilla BlockBed.java:212) — EntityAIOcelotSit
    // checks this when a (live, replaced) FC ocelot looks for a bed head to sit on.
    public static boolean isBlockHeadOfBed(int metadata) {
        return (metadata & 8) != 0;
    }

    // 1.5.2 BlockBed.getRenderType / renderAsNormalBlock / isOpaqueCube / getMobilityFlag (vanilla/client
    // BlockBed.java:169-189, :327-330). FCBlockBed (26) overrides none of them.
    @Override
    public int getRenderType() {
        return 14;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public int getMobilityFlag() {
        return 1;
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockBed's icon members (vanilla/client BlockBed.java:24-26,
    // :140-164): planks below, then the top, side and end of the foot or head half by direction. FCBlockBed (26)
    // inherits both. Nothing FC draws asks for them today (the bed item is ItemBed; beds in the world are 1.20.1's),
    // and 1.20.1 draws beds from entity textures, so the six bed_* names have no block-atlas sprite.
    private Icon[] bedEndIcons;
    private Icon[] bedSideIcons;
    private Icon[] bedTopIcons;

    // 1.5.2 BlockBed.getIcon (vanilla/client BlockBed.java:140-153); Block's icon until registerIcons runs (client
    // only)
    @Override
    public Icon getIcon(int side, int metadata) {
        if (side == 0) {
            return Block.planks.getBlockTextureFromSide(side);
        }
        if (this.bedTopIcons == null || this.bedSideIcons == null || this.bedEndIcons == null) {
            return super.getIcon(side, metadata);
        }
        int direction = getDirection(metadata);
        int face = Direction.bedDirection[direction][side];
        int half = isBlockHeadOfBed(metadata) ? 1 : 0;
        return (half != 1 || face != 2) && (half != 0 || face != 3)
                ? (face != 5 && face != 4 ? this.bedTopIcons[half] : this.bedSideIcons[half])
                : this.bedEndIcons[half];
    }

    // 1.5.2 BlockBed.registerIcons (vanilla/client BlockBed.java:159-164)
    @Override
    public void registerIcons(IconRegister register) {
        this.bedTopIcons = new Icon[] {register.registerIcon("bed_feet_top"), register.registerIcon("bed_head_top")};
        this.bedEndIcons = new Icon[] {register.registerIcon("bed_feet_end"), register.registerIcon("bed_head_end")};
        this.bedSideIcons = new Icon[] {register.registerIcon("bed_feet_side"), register.registerIcon("bed_head_side")};
    }
}
