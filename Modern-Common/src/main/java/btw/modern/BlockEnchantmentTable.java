package btw.modern;

public class BlockEnchantmentTable extends BlockContainer {

    protected BlockEnchantmentTable(int id) {
        super(id, Material.rock);
        // 1.5.2 BlockEnchantmentTable constructor — 3/4-height bounds and no
        // light blocking (FCBlockEnchantmentTable re-applies the same bounds
        // via InitBlockBounds).
        this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.75F, 1.0F);
        this.setLightOpacity(0);
    }

    // 1.5.2 BlockEnchantmentTable.renderAsNormalBlock
    public boolean renderAsNormalBlock() {
        return false;
    }

    // 1.5.2 BlockEnchantmentTable.isOpaqueCube
    public boolean isOpaqueCube() {
        return false;
    }

    public TileEntity createNewTileEntity(World world) {
        return null;
    }

    // 1.5.2 BlockEnchantmentTable.onBlockActivated — inherited by
    // FCBlockEnchantmentTable (registered ProxyBlock, id 116); opens the FC
    // enchanting container via PlayerBridge.displayGUIEnchantment. The 1.5.2
    // custom-name lookup through TileEntityEnchantmentTable is dropped (the
    // shim has no such tile entity); a null name falls back to the bridge's
    // default "Enchant" title.
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        } else {
            player.displayGUIEnchantment(x, y, z, null);
            return true;
        }
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockEnchantmentTable's icon members (vanilla/client
    // BlockEnchantmentTable.java:21-22, :87-90, :134-139): side, top and bottom. FCBlockEnchantmentTable (116)
    // inherits both; a held table drew Block's default, the unlocalized "enchantmentTable", which names no texture.
    private Icon enchantmentTopIcon;
    private Icon enchantmentBottomIcon;

    // 1.5.2 BlockEnchantmentTable.getIcon (vanilla/client BlockEnchantmentTable.java:87-90); Block's icon until
    // registerIcons runs (client only)
    @Override
    public Icon getIcon(int side, int metadata) {
        if (this.enchantmentTopIcon == null || this.enchantmentBottomIcon == null) {
            return super.getIcon(side, metadata);
        }
        return side == 0 ? this.enchantmentBottomIcon : (side == 1 ? this.enchantmentTopIcon : this.blockIcon);
    }

    // 1.5.2 BlockEnchantmentTable.registerIcons (vanilla/client BlockEnchantmentTable.java:134-139)
    @Override
    public void registerIcons(IconRegister register) {
        this.blockIcon = register.registerIcon("enchantment_side");
        this.enchantmentTopIcon = register.registerIcon("enchantment_top");
        this.enchantmentBottomIcon = register.registerIcon("enchantment_bottom");
    }
}
