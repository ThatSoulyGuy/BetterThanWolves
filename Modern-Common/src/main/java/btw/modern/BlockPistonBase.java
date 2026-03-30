package btw.modern;

public class BlockPistonBase extends Block {

    public boolean isSticky;
    public Icon topIcon;
    public Icon innerTopIcon;
    public Icon bottomIcon;

    protected BlockPistonBase(int id, boolean isSticky) {
        super(id, Material.piston);
        this.isSticky = isSticky;
    }

    public static boolean isExtended(int metadata) {
        return (metadata & 8) != 0;
    }

    public void updatePistonState(World world, int i, int j, int k) {}
    public boolean canExtend(World world, int i, int j, int k, int facing) { return false; }
    public boolean tryExtend(World world, int i, int j, int k, int facing) { return false; }
    public int GetPistonShovelEjectionDirection(World world, int i, int j, int k, int facing) { return -1; }
    public void ValidatePistonState(World world, int i, int j, int k) {}

    public static int getOrientation(int metadata) {
        return metadata & 7;
    }

    public static int determineOrientation(World world, int x, int y, int z, EntityLiving placer) {
        if (MathHelper.abs((float)placer.posX - (float)x) < 2.0F
                && MathHelper.abs((float)placer.posZ - (float)z) < 2.0F) {
            double eyeY = placer.posY + 1.82 - (double)placer.yOffset;
            if (eyeY - (double)y > 2.0) return 1; // up
            if ((double)y - eyeY > 0.0) return 0; // down
        }
        int dir = MathHelper.floor_double((double)(placer.rotationYaw * 4.0F / 360.0F) + 0.5) & 3;
        return dir == 0 ? 2 : (dir == 1 ? 5 : (dir == 2 ? 3 : (dir == 3 ? 4 : 0)));
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLiving placer, ItemStack stack) {
        int facing = determineOrientation(world, x, y, z, placer);
        world.setBlockMetadataWithNotify(x, y, z, facing);
    }
}
