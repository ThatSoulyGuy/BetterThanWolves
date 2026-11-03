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

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockDispenser's icon members (vanilla/client
    // BlockDispenser.java:24-26, :92-108): the furnace side and top, and the front (or the vertical front facing up
    // or down) on the facing side. FCBlockDispenserVanilla (23) inherits both; a held dispenser drew Block's default,
    // the unlocalized "dispenser", which names no texture.
    public Icon furnaceTopIcon;
    public Icon furnaceFrontIcon;
    public Icon field_96473_e;

    // 1.5.2 BlockDispenser.getIcon (vanilla/client BlockDispenser.java:92-96); Block's icon until registerIcons runs
    // (client only)
    @Override
    public Icon getIcon(int side, int metadata) {
        if (this.furnaceTopIcon == null || this.furnaceFrontIcon == null || this.field_96473_e == null) {
            return super.getIcon(side, metadata);
        }
        int facing = metadata & 7;
        return side == facing ? (facing != 1 && facing != 0 ? this.furnaceFrontIcon : this.field_96473_e)
                : (facing != 1 && facing != 0 ? (side != 1 && side != 0 ? this.blockIcon : this.furnaceTopIcon)
                : this.furnaceTopIcon);
    }

    // 1.5.2 BlockDispenser.registerIcons (vanilla/client BlockDispenser.java:102-108)
    @Override
    public void registerIcons(IconRegister register) {
        this.blockIcon = register.registerIcon("furnace_side");
        this.furnaceTopIcon = register.registerIcon("furnace_top");
        this.furnaceFrontIcon = register.registerIcon("dispenser_front");
        this.field_96473_e = register.registerIcon("dispenser_front_vertical");
    }
}
