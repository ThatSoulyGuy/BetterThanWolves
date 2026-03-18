package btw.api;

public class BlockFlowerPot extends Block {

    protected BlockFlowerPot(int id) {
        super(id, Material.circuits);
    }

    public ItemStack getPlantForMeta(int meta) { return null; }
    public int getMetaForPlant(ItemStack stack) { return 0; }
}
