package btw.modern;

import java.util.List;

public class BlockEndPortal extends BlockContainer {

    protected BlockEndPortal(int id, Material material) {
        super(id, material);
        // 1.5.2 BlockEndPortal ctor (vanilla/server BlockEndPortal.java:26-30): the portal emits full light
        // (Block.lightValue[119] = 15, inherited by FCBlockEndPortal; read by FCBlockLens).
        setLightValue(1.0F);
    }

    // 1.5.2 BlockEndPortal.addCollisionBoxesToList (vanilla/client BlockEndPortal.java:63): the end portal adds
    // no collision boxes (FCBlockEndPortal overrides only setBlockBoundsBasedOnState)
    @Override
    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask, List list, Entity entity) {}

    // 1.5.2 BlockEndPortal.isOpaqueCube / renderAsNormalBlock (vanilla/client BlockEndPortal.java:69-80). With the
    // inherited true, World.isBlockNormalCube reported the passable portal as solid (Entity.isEntityInsideOpaqueBlock
    // -> inWall hit).
    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    public TileEntity createNewTileEntity(World world) {
        return null;
    }

    // 1.5.2 BlockEndPortal.getRenderType (vanilla/client BlockEndPortal.java:118-121): drawn by its tile entity
    // renderer only. FCBlockEndPortal (119) does not override it.
    @Override
    public int getRenderType() {
        return -1;
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockEndPortal.registerIcons (vanilla/client
    // BlockEndPortal.java:149-152) registers "portal". FCBlockEndPortal (119) inherits it.
    @Override
    public void registerIcons(IconRegister register) {
        this.blockIcon = register.registerIcon("portal");
    }
}
