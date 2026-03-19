package btw.modern;

public class BlockBrewingStand extends BlockContainer {

    protected BlockBrewingStand(int id) {
        super(id, Material.iron);
    }

    public TileEntity createNewTileEntity(World world) {
        return null;
    }
}
