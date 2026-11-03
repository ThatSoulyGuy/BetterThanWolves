package btw.modern;

public class BlockFlowerPot extends Block {

    protected BlockFlowerPot(int id) {
        super(id, Material.circuits);
    }

    public ItemStack getPlantForMeta(int meta) { return null; }
    public int getMetaForPlant(ItemStack stack) { return 0; }

    // 1.5.2 BlockFlowerPot.isOpaqueCube / getRenderType / renderAsNormalBlock (vanilla/client
    // BlockFlowerPot.java:41-60). FCBlockFlowerPot (140) overrides none of them.
    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public int getRenderType() {
        return 33;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }
}
