package btw.modern;

public class BlockGrass extends Block {

    protected BlockGrass(int id) {
        super(id, Material.grass);
    }

    private static Icon sideOverlayIcon;

    public static Icon getIconSideOverlay() {
        return sideOverlayIcon;
    }

    public static void setIconSideOverlay(Icon icon) {
        sideOverlayIcon = icon;
    }
}
