package btw.api;

public class BlockChest extends BlockContainer {

    public int isTrapped;

    protected BlockChest(int id) {
        super(id, Material.wood);
    }

    protected BlockChest(int id, int chestType) {
        super(id, Material.wood);
        this.isTrapped = chestType;
    }

    public TileEntity createNewTileEntity(World world) {
        return null;
    }
}
