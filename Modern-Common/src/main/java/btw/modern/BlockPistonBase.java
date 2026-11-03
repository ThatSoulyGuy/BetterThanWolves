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

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockPistonBase's icon members (vanilla/client
    // BlockPistonBase.java:24-42, :59-93): registerIcons fills the side icon and the public top, inner-top and bottom
    // fields that FCBlockPistonBase.getIcon reads (Client FCBlockPistonBase.java:500-524); they were never assigned,
    // so a held piston drew two faces with no texture and the others with the unlocalized "pistonBase", which names
    // no texture. FCBlockPistonBase (29, 33) answers getIcon itself.
    public Icon getPistonExtensionTexture() {
        return this.topIcon;
    }

    // 1.5.2 BlockPistonBase.getIcon (vanilla/client BlockPistonBase.java:72-76); Block's icon until registerIcons runs
    // (client only)
    @Override
    public Icon getIcon(int side, int metadata) {
        if (this.topIcon == null || this.innerTopIcon == null || this.bottomIcon == null) {
            return super.getIcon(side, metadata);
        }
        int facing = getOrientation(metadata);
        return facing > 5 ? this.topIcon
                : (side == facing ? (!isExtended(metadata) && this.minX <= 0.0D && this.minY <= 0.0D
                        && this.minZ <= 0.0D && this.maxX >= 1.0D && this.maxY >= 1.0D && this.maxZ >= 1.0D
                        ? this.topIcon : this.innerTopIcon)
                : (side == Facing.oppositeSide[facing] ? this.bottomIcon : this.blockIcon));
    }

    // 1.5.2 BlockPistonBase.func_94496_b (vanilla/client BlockPistonBase.java:78-81): Block.pistonBase's or
    // Block.pistonStickyBase's icons by name (compared by value; 1.5.2's callers pass these literals)
    public static Icon func_94496_b(String name) {
        BlockPistonBase piston = Block.pistonBase;
        BlockPistonBase sticky = Block.pistonStickyBase;
        return "piston_side".equals(name) ? (piston != null ? piston.blockIcon : null)
                : ("piston_top".equals(name) ? (piston != null ? piston.topIcon : null)
                : ("piston_top_sticky".equals(name) ? (sticky != null ? sticky.topIcon : null)
                : ("piston_inner_top".equals(name) ? (piston != null ? piston.innerTopIcon : null) : null)));
    }

    // 1.5.2 BlockPistonBase.registerIcons (vanilla/client BlockPistonBase.java:87-93)
    @Override
    public void registerIcons(IconRegister register) {
        this.blockIcon = register.registerIcon("piston_side");
        this.topIcon = register.registerIcon(this.isSticky ? "piston_top_sticky" : "piston_top");
        this.innerTopIcon = register.registerIcon("piston_inner_top");
        this.bottomIcon = register.registerIcon("piston_bottom");
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

    // 1.5.2 BlockPistonBase.getRenderType / isOpaqueCube / renderAsNormalBlock (vanilla/client
    // BlockPistonBase.java:98-110, :373-376). FCBlockPistonBase (29, 33) overrides none of them.
    @Override
    public int getRenderType() {
        return 16;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }
}
