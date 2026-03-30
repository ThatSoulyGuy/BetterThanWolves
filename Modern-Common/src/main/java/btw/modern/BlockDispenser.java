package btw.modern;

public class BlockDispenser extends BlockContainer {

    public static final IRegistry dispenseBehaviorRegistry = new RegistryDefaulted(null);

    protected BlockDispenser(int id) {
        super(id, Material.rock);
    }

    public TileEntity createNewTileEntity(World world) {
        return null;
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        }
        TileEntity te = world.getBlockTileEntity(x, y, z);
        if (te != null) {
            player.displayGUIDispenser(te);
        }
        return true;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLiving placer, ItemStack stack) {
        int facing = BlockPistonBase.determineOrientation(world, x, y, z, placer);
        world.setBlockMetadataWithNotify(x, y, z, facing);
    }
}
