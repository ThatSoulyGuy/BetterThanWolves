package btw.modern;

public class BlockPumpkin extends BlockDirectional {

    // 1.5.2 BlockPumpkin constructor sets the random-tick flag (vanilla/server BlockPumpkin.java:24); both port
    // constructors stand for that one 1.5.2 constructor
    protected BlockPumpkin(int id) {
        super(id, Material.pumpkin);
        this.setTickRandomly(true);
    }

    protected BlockPumpkin(int id, boolean isLantern) {
        super(id, Material.pumpkin);
        this.setTickRandomly(true);
        this.blockType = isLantern;
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockPumpkin's icon members (vanilla/client
    // BlockPumpkin.java:20-22, :35-38, :139-144): the top above and below, the carved face (or the lit jack-o'-lantern
    // face, by the constructor's flag) on the facing side, the side elsewhere. FCBlockPumpkinCarved (86) inherits both;
    // a carried or worn pumpkin drew Block's default, the unlocalized "pumpkin", which names no texture.
    private boolean blockType;
    private Icon pumpkinTopIcon;
    private Icon pumpkinFaceIcon;

    // 1.5.2 BlockPumpkin.getIcon (vanilla/client BlockPumpkin.java:35-38); Block's icon until registerIcons runs
    // (client only)
    @Override
    public Icon getIcon(int side, int metadata) {
        if (this.pumpkinTopIcon == null || this.pumpkinFaceIcon == null) {
            return super.getIcon(side, metadata);
        }
        return side == 1 ? this.pumpkinTopIcon
                : (side == 0 ? this.pumpkinTopIcon
                : (metadata == 2 && side == 2 ? this.pumpkinFaceIcon
                : (metadata == 3 && side == 5 ? this.pumpkinFaceIcon
                : (metadata == 0 && side == 3 ? this.pumpkinFaceIcon
                : (metadata == 1 && side == 4 ? this.pumpkinFaceIcon : this.blockIcon)))));
    }

    // 1.5.2 BlockPumpkin.registerIcons (vanilla/client BlockPumpkin.java:139-144)
    @Override
    public void registerIcons(IconRegister register) {
        this.pumpkinFaceIcon = register.registerIcon(this.blockType ? "pumpkin_jack" : "pumpkin_face");
        this.pumpkinTopIcon = register.registerIcon("pumpkin_top");
        this.blockIcon = register.registerIcon("pumpkin_side");
    }
}
