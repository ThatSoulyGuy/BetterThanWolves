package btw.modern;

public class BlockWorkbench extends Block {

    protected BlockWorkbench(int id) {
        super(id, Material.wood);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            player.displayGUIWorkbench(x, y, z);
        }
        return true;
    }
}
