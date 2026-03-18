package btw.api;

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

    public static void updateFurnaceBlockState(boolean isBurning, World world, int i, int j, int k) {}
    public void updateFurnaceBlockState(boolean isBurning, World world, int i, int j, int k, boolean bHasContents) {}
}
