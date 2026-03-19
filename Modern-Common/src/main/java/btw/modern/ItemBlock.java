package btw.modern;

public class ItemBlock extends Item {

    private int blockID;

    public ItemBlock(int id) {
        super(id);
        this.blockID = id + 256;
    }

    public int getBlockID() {
        return this.blockID;
    }

    public void PlayPlaceSound(World world, int x, int y, int z, Block block) {}
    public int GetTargetFacingPlacedByBlockDispenser(int dispenserFacing) { return 0; }

    public boolean canPlaceItemBlockOnSide(World world, int x, int y, int z, int side, EntityPlayer player, ItemStack stack) {
        return true;
    }
}
