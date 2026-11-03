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

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockFurnace's icon members (vanilla/client
    // BlockFurnace.java:46-50, :116-130): the top above and below, the front (lit or not) on the facing side, the side
    // elsewhere; the public front and top fields are FCMOD's. FCBlockFurnace (61, 62) reads both through super; a held
    // furnace drew Block's default, the unlocalized "furnace", which names no texture.
    @Override
    public Icon getIcon(int side, int metadata) {
        if (this.furnaceIconTop == null || this.furnaceIconFront == null) {
            return super.getIcon(side, metadata);
        }
        return side == 1 ? this.furnaceIconTop
                : (side == 0 ? this.furnaceIconTop : (side != metadata ? this.blockIcon : this.furnaceIconFront));
    }

    // 1.5.2 BlockFurnace.registerIcons (vanilla/client BlockFurnace.java:125-130)
    @Override
    public void registerIcons(IconRegister register) {
        this.blockIcon = register.registerIcon("furnace_side");
        this.furnaceIconFront = register.registerIcon(this.isActive ? "furnace_front_lit" : "furnace_front");
        this.furnaceIconTop = register.registerIcon("furnace_top");
    }

    // FCMOD-INTEGRATION(1.20.1): fc_redstone: 1.5.2's comparator answers (vanilla/client BlockFurnace.java:336-348): a
    // comparator reads FC's brick oven (1023, 1024: FCBlockFurnaceBrick < FCBlockFurnace, ProxyBlocks) as a vanilla furnace
    // (ruling Q-RS3 (a)); the vanilla-id furnaces 61 and 62 keep 1.20.1's FurnaceBlock answers
    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(World world, int x, int y, int z, int side) {
        TileEntity te = world.getBlockTileEntity(x, y, z);
        return Container.calcRedstoneFromInventory(te instanceof IInventory inventory ? inventory : null);
    }

    public static void updateFurnaceBlockState(boolean isBurning, World world, int i, int j, int k) {}
    public void updateFurnaceBlockState(boolean isBurning, World world, int i, int j, int k, boolean bHasContents) {}
}
