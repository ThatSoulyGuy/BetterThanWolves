package btw.modern;

public class BlockMycelium extends Block {

    protected BlockMycelium(int id) {
        super(id, Material.grass);
        // 1.5.2 BlockMycelium constructor (vanilla/server BlockMycelium.java:23): FCBlockMycelium.updateTick (death
        // in the dark, spread) runs on the random tick
        setTickRandomly(true);
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockMycelium's icon members (vanilla/client
    // BlockMycelium.java:21-22, :34-68): top, dirt below, side (snowy side under snow in the world). FCBlockMycelium
    // (110) calls registerIcons first and inherits getIcon (its getBlockTexture is its own); a carried mycelium drew
    // Block's default, the unlocalized "mycel", which names no texture.
    private Icon myceliumTopIcon;
    private Icon myceliumSnowSideIcon;

    // 1.5.2 BlockMycelium.getIcon (vanilla/client BlockMycelium.java:34-37); Block's icon until registerIcons runs
    // (client only)
    @Override
    public Icon getIcon(int side, int metadata) {
        if (this.myceliumTopIcon == null) {
            return super.getIcon(side, metadata);
        }
        return side == 1 ? this.myceliumTopIcon
                : (side == 0 ? Block.dirt.getBlockTextureFromSide(side) : this.blockIcon);
    }

    // 1.5.2 BlockMycelium.getBlockTexture (vanilla/client BlockMycelium.java:42-57); getIcon's answer until
    // registerIcons runs (client only)
    @Override
    public Icon getBlockTexture(IBlockAccess blockAccess, int x, int y, int z, int side) {
        if (this.myceliumTopIcon == null || this.myceliumSnowSideIcon == null) {
            return super.getBlockTexture(blockAccess, x, y, z, side);
        }
        if (side == 1) {
            return this.myceliumTopIcon;
        } else if (side == 0) {
            return Block.dirt.getBlockTextureFromSide(side);
        } else {
            Material above = blockAccess.getBlockMaterial(x, y + 1, z);
            return above != Material.snow && above != Material.craftedSnow ? this.blockIcon : this.myceliumSnowSideIcon;
        }
    }

    // 1.5.2 BlockMycelium.registerIcons (vanilla/client BlockMycelium.java:63-68)
    @Override
    public void registerIcons(IconRegister register) {
        this.blockIcon = register.registerIcon("mycel_side");
        this.myceliumTopIcon = register.registerIcon("mycel_top");
        this.myceliumSnowSideIcon = register.registerIcon("snow_side");
    }
}
