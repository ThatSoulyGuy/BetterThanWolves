package btw.modern;

public class BlockTNT extends Block {

    protected BlockTNT(int id) {
        super(id, Material.tnt);
    }

    // FCMOD-INTEGRATION(1.20.1): fc_blast_rules: 1.5.2 BlockTNT.canDropFromExplosion (vanilla/server BlockTNT.java:129-135): a blasted keg never drops itself (FCBlockPowderKeg inherits it; a mining charge reaches it through DropItemsOnDestroyedByExplosion)
    @Override
    public boolean canDropFromExplosion(Explosion explosion) {
        return false;
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockTNT's icon members (vanilla/client BlockTNT.java:21-22,
    // :33-36, :153-158): side, top and bottom. FCBlockPowderKeg (46) registers and answers its own (Client
    // FCBlockPowderKeg.java:73-94) without calling these, so nothing draws them.
    private Icon tntTopIcon;
    private Icon tntBottomIcon;

    // 1.5.2 BlockTNT.getIcon (vanilla/client BlockTNT.java:33-36); Block's icon until registerIcons runs (client only)
    @Override
    public Icon getIcon(int side, int metadata) {
        if (this.tntTopIcon == null || this.tntBottomIcon == null) {
            return super.getIcon(side, metadata);
        }
        return side == 0 ? this.tntBottomIcon : (side == 1 ? this.tntTopIcon : this.blockIcon);
    }

    // 1.5.2 BlockTNT.registerIcons (vanilla/client BlockTNT.java:153-158)
    @Override
    public void registerIcons(IconRegister register) {
        this.blockIcon = register.registerIcon("tnt_side");
        this.tntTopIcon = register.registerIcon("tnt_top");
        this.tntBottomIcon = register.registerIcon("tnt_bottom");
    }
}
