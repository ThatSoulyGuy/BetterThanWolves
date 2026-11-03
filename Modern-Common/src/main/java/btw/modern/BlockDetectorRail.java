package btw.modern;

import java.util.List;

public class BlockDetectorRail extends BlockRailBase {

    // 1.5.2 BlockDetectorRail (vanilla/client BlockDetectorRail.java:24) passes
    // isPowered=true — detector rails ARE powered-type rails. RenderBlocks.
    // renderBlockMinecartTrack does `if (block.isPowered()) meta &= 7;`, so a
    // false flag renders the wrong rail metadata.
    protected BlockDetectorRail(int id) {
        super(id, true);
        this.setTickRandomly(true);
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockDetectorRail's icon members (vanilla/client
    // BlockDetectorRail.java:22, :178-191): the off and on rail. FCBlockDetectorRail (28, 235, 236) calls this
    // registerIcons first (Client FCBlockDetectorRail.java:180-182), then sets blockIcon and its own on icon and
    // answers getIcon itself, so what it draws does not change.
    private Icon[] iconArray;

    // 1.5.2 BlockDetectorRail.registerIcons (vanilla/client BlockDetectorRail.java:178-183)
    @Override
    public void registerIcons(IconRegister register) {
        Icon[] icons = new Icon[2];
        icons[0] = register.registerIcon("detectorRail");
        icons[1] = register.registerIcon("detectorRail_on");
        this.iconArray = icons;
    }

    // 1.5.2 BlockDetectorRail.getIcon (vanilla/client BlockDetectorRail.java:188-191); Block's icon until
    // registerIcons runs (client only)
    @Override
    public Icon getIcon(int side, int metadata) {
        if (this.iconArray == null) {
            return super.getIcon(side, metadata);
        }
        return (metadata & 8) != 0 ? this.iconArray[1] : this.iconArray[0];
    }

    // FCMOD-INTEGRATION(1.20.1): fc_redstone: 1.5.2's power and comparator answers (vanilla/client
    // BlockDetectorRail.java:33-98, :149-170) for FC's own reads of a detector rail (28, 235, 236). In the world these
    // rails are 1.20.1 detector rails (FcDetectorRailBlock for 235 and 236), whose signal and comparator output are
    // 1.20.1's and agree with these answers; FC's trigger rule (FCBlockDetectorRail.ShouldPlateActivateBasedOnMinecart)
    // filters the carts that press them (DetectorRailBlockMixin)

    // 1.5.2 BlockDetectorRail.tickRate (vanilla/client BlockDetectorRail.java:33-36): FCBlockDetectorRail re-checks a
    // pressed rail at this rate
    @Override
    public int tickRate(World world) {
        return 20;
    }

    // 1.5.2 BlockDetectorRail.canProvidePower (vanilla/client BlockDetectorRail.java:41-44)
    @Override
    public boolean canProvidePower() {
        return true;
    }

    // 1.5.2 BlockDetectorRail.isProvidingWeakPower (vanilla/client BlockDetectorRail.java:82-85)
    @Override
    public int isProvidingWeakPower(IBlockAccess blockAccess, int x, int y, int z, int side) {
        return (blockAccess.getBlockMetadata(x, y, z) & 8) != 0 ? 15 : 0;
    }

    // 1.5.2 BlockDetectorRail.isProvidingStrongPower (vanilla/client BlockDetectorRail.java:91-94): the block under the
    // rail (side 1)
    @Override
    public int isProvidingStrongPower(IBlockAccess blockAccess, int x, int y, int z, int side) {
        return (blockAccess.getBlockMetadata(x, y, z) & 8) == 0 ? 0 : (side == 1 ? 15 : 0);
    }

    // 1.5.2 BlockDetectorRail.hasComparatorInputOverride (vanilla/client BlockDetectorRail.java:149-152)
    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    // 1.5.2 BlockDetectorRail.getComparatorInputOverride (vanilla/client BlockDetectorRail.java:158-170): a pressed
    // rail reads the first cart with an inventory over it
    @Override
    public int getComparatorInputOverride(World world, int x, int y, int z, int side) {
        if ((world.getBlockMetadata(x, y, z) & 8) > 0) {
            float border = 0.125F;
            List carts = world.selectEntitiesWithinAABB(EntityMinecart.class, AxisAlignedBB.getAABBPool().getAABB(
                    (double) ((float) x + border), (double) y, (double) ((float) z + border),
                    (double) ((float) (x + 1) - border), (double) ((float) (y + 1) - border),
                    (double) ((float) (z + 1) - border)), IEntitySelector.selectInventories);

            if (carts.size() > 0) {
                return Container.calcRedstoneFromInventory((IInventory) carts.get(0));
            }
        }

        return 0;
    }
}
