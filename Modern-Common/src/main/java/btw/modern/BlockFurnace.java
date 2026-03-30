package btw.modern;

public class BlockFurnace extends BlockContainer {

    public final boolean isActive;
    public static boolean keepFurnaceInventory;
    public Icon furnaceIconFront;
    public Icon furnaceIconTop;

    protected BlockFurnace(int id, boolean isActive) {
        super(id, Material.rock);
        this.isActive = isActive;
    }

    public TileEntity createNewTileEntity(World world) {
        return null;
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLiving placer, ItemStack stack) {
        int direction = MathHelper.floor_double((double)(placer.rotationYaw * 4.0F / 360.0F) + 0.5) & 3;
        int facing;
        switch (direction) {
            case 0: facing = 2; break; // north
            case 1: facing = 5; break; // east
            case 2: facing = 3; break; // south
            case 3: facing = 4; break; // west
            default: facing = 2;
        }
        world.setBlockMetadataWithNotify(x, y, z, facing);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        }
        TileEntity te = world.getBlockTileEntity(x, y, z);
        if (te != null) {
            player.displayGUIFurnace(te);
        }
        return true;
    }

    public static void updateFurnaceBlockState(boolean isBurning, World world, int i, int j, int k) {}
    public void updateFurnaceBlockState(boolean isBurning, World world, int i, int j, int k, boolean bHasContents) {}
}
