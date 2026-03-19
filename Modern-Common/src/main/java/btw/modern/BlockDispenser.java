package btw.modern;

public class BlockDispenser extends BlockContainer {

    public static final IRegistry dispenseBehaviorRegistry = new RegistryDefaulted(null);

    protected BlockDispenser(int id) {
        super(id, Material.rock);
    }

    public TileEntity createNewTileEntity(World world) {
        return null;
    }
}
