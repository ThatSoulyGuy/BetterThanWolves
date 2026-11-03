package btw.modern;

public class BlockWall extends Block {

    protected BlockWall(int id, Block modelBlock) {
        super(id, Material.rock);
    }

    // 1.5.2 BlockWall.canConnectWallTo (vanilla/client BlockWall.java:136-150): the same wall, a fence gate, or an
    // opaque material rendered as a normal block other than a pumpkin. FCBlockWall's bounds read it (Client
    // FCBlockWall.java:118-124).
    public boolean canConnectWallTo(IBlockAccess blockAccess, int i, int j, int k) {
        int id = blockAccess.getBlockId(i, j, k);
        if (id != this.blockID && (Block.fenceGate == null || id != Block.fenceGate.blockID)) {
            Block block = id > 0 && id < Block.blocksList.length ? Block.blocksList[id] : null;
            return block != null && block.blockMaterial != null && block.blockMaterial.isOpaque()
                    && block.renderAsNormalBlock() ? block.blockMaterial != Material.pumpkin : false;
        }
        return true;
    }

    // 1.5.2 BlockWall.getRenderType / renderAsNormalBlock / isOpaqueCube (vanilla/client BlockWall.java:44-55,
    // :66-69). FCBlockWall (139) overrides none of them. (getBlocksMovement false, :57-60, is the rock answer
    // Block already gives.)
    @Override
    public int getRenderType() {
        return 32;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    // FCMOD-INTEGRATION(1.20.1): legacy_icons: 1.5.2 BlockWall's icon members (vanilla/client BlockWall.java:36-39,
    // :181): cobblestone, or mossy cobblestone for meta 1, and no icon of its own. FCBlockWall (139) inherits both; a
    // held wall drew Block's default, the unlocalized "cobbleWall", which names no texture.
    @Override
    public Icon getIcon(int side, int metadata) {
        return metadata == 1 ? Block.cobblestoneMossy.getBlockTextureFromSide(side)
                : Block.cobblestone.getBlockTextureFromSide(side);
    }

    @Override
    public void registerIcons(IconRegister register) {}
}
