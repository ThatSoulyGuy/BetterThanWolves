package btw.modern;

public class BlockGlass extends BlockBreakable {

    // 1.5.2 BlockGlass passes its icon name "glass" (vanilla/client BlockGlass.java:21-25; legacy_icons,
    // BlockBreakable)
    protected BlockGlass(int id, Material material, boolean renderAsNormalBlock) {
        super(id, "glass", material, renderAsNormalBlock);
    }

    // 1.5.2 BlockGlass.isOpaqueCube / renderAsNormalBlock (vanilla/client BlockGlass.java:47-58). FCBlockGlass (20)
    // overrides neither. (getRenderBlockPass 0, :38-41, is Block's own answer.)
    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }
}
