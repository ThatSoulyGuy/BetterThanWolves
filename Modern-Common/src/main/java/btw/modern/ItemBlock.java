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

    /**
     * Returns the block ID to place. Subclasses like FCItemBlockLegacySubstitution
     * override this to place a different block than the one this item represents.
     */
    public int GetBlockIDToPlace(int iItemDamage, int iFacing, float fClickX, float fClickY, float fClickZ) {
        return this.blockID;
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world,
                              int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        // Offset the target position based on the clicked face
        int targetX = x, targetY = y, targetZ = z;
        switch (side) {
            case 0: targetY--; break; // bottom
            case 1: targetY++; break; // top
            case 2: targetZ--; break; // north
            case 3: targetZ++; break; // south
            case 4: targetX--; break; // west
            case 5: targetX++; break; // east
        }

        if (stack.stackSize <= 0) return false;
        if (!player.canPlayerEdit(targetX, targetY, targetZ, side, stack)) return false;

        int placeBlockID = GetBlockIDToPlace(stack.getItemDamage(), side, hitX, hitY, hitZ);
        Block placeBlock = Block.blocksList[placeBlockID];
        if (placeBlock == null) return false;

        if (!world.canPlaceEntityOnSide(placeBlockID, targetX, targetY, targetZ, false, side, null, stack)) {
            return false;
        }

        if (world.setBlockAndMetadataWithNotify(targetX, targetY, targetZ, placeBlockID,
                placeBlock.onBlockPlaced(world, targetX, targetY, targetZ, side, hitX, hitY, hitZ, stack.getItemDamage()))) {
            placeBlock.onBlockPlacedBy(world, targetX, targetY, targetZ, player, stack);
            PlayPlaceSound(world, targetX, targetY, targetZ, placeBlock);
            stack.stackSize--;
            return true;
        }

        return false;
    }

    public void PlayPlaceSound(World world, int x, int y, int z, Block block) {
        if (block.stepSound != null) {
            world.playSoundEffect(x + 0.5, y + 0.5, z + 0.5,
                    block.stepSound.getStepSound(), 1.0F, block.stepSound.getPitch());
        }
    }

    public int GetTargetFacingPlacedByBlockDispenser(int dispenserFacing) { return 0; }

    public boolean canPlaceItemBlockOnSide(World world, int x, int y, int z, int side, EntityPlayer player, ItemStack stack) {
        return true;
    }
}
