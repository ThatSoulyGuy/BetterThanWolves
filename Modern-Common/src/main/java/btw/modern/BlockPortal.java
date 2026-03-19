package btw.modern;

public class BlockPortal extends BlockBreakable {

    protected BlockPortal(int id) {
        super(id, Material.portal, false);
    }

    public boolean tryToCreatePortal(World world, int x, int y, int z) { return false; }
}
