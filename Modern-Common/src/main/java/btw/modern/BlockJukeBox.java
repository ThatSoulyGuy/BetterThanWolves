package btw.modern;

public class BlockJukeBox extends BlockContainer {

    protected BlockJukeBox(int id) {
        super(id, Material.wood);
    }

    public TileEntity createNewTileEntity(World world) {
        return null;
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockJukeBox's icon members (vanilla/client BlockJukeBox.java:19,
    // :30-33, :132-136): the note-block sides and the jukebox top. FCBlockJukebox (84) inherits both; a held jukebox
    // drew Block's default, the unlocalized "jukebox", which names no texture.
    private Icon jukeboxTopIcon;

    // 1.5.2 BlockJukeBox.getIcon (vanilla/client BlockJukeBox.java:30-33); Block's icon until registerIcons runs
    // (client only)
    @Override
    public Icon getIcon(int side, int metadata) {
        if (this.jukeboxTopIcon == null) {
            return super.getIcon(side, metadata);
        }
        return side == 1 ? this.jukeboxTopIcon : this.blockIcon;
    }

    // 1.5.2 BlockJukeBox.registerIcons (vanilla/client BlockJukeBox.java:132-136)
    @Override
    public void registerIcons(IconRegister register) {
        this.blockIcon = register.registerIcon("musicBlock");
        this.jukeboxTopIcon = register.registerIcon("jukebox_top");
    }
}
