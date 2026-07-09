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

    // 1.5.2 FCItemPlacesAsBlock.GetTargetFacingPlacedByBlockDispenser (Common/.../FCItemPlacesAsBlock.java:231)
    // — real ItemBlock inherits it; used by OnItemUsedByBlockDispenser below.
    public int GetTargetFacingPlacedByBlockDispenser(int dispenserFacing) {
        return Block.GetOppositeFacing(dispenserFacing);
    }

    // 1.5.2 FCItemPlacesAsBlock.OnItemUsedByBlockDispenser (Common/.../FCItemPlacesAsBlock.java:138)
    // — real ItemBlock inherits it; caller FCBlockBlockDispenser.DispenseCurrentItem (live ProxyBlock).
    @Override
    public boolean OnItemUsedByBlockDispenser(ItemStack stack, World world,
            int i, int j, int k, int iFacing) {
        FCUtilsBlockPos targetPos = new FCUtilsBlockPos(i, j, k, iFacing);
        int iTargetDirection = GetTargetFacingPlacedByBlockDispenser(iFacing);

        int iBlockID = GetBlockIDToPlace(stack.getItemDamage(), iTargetDirection,
                0.5F, 0.25F, 0.5F); // equivalent to lower half of the middle of the block

        Block newBlock = Block.blocksList[iBlockID];

        if (newBlock != null && world.canPlaceEntityOnSide(iBlockID,
                targetPos.i, targetPos.j, targetPos.k, true, iTargetDirection, null, stack)) {
            int iBlockMetadata = getMetadata(stack.getItemDamage());

            iBlockMetadata = newBlock.onBlockPlaced(
                    world, targetPos.i, targetPos.j, targetPos.k, iTargetDirection,
                    0.5F, 0.25F, 0.5F, // equivalent to lower half of the middle of the block
                    iBlockMetadata);

            world.setBlockAndMetadataWithNotify(targetPos.i, targetPos.j, targetPos.k, iBlockID,
                    iBlockMetadata);

            newBlock.onPostBlockPlaced(world, targetPos.i, targetPos.j, targetPos.k,
                    iBlockMetadata);

            // FCBetterThanWolves.m_iBlockPlaceAuxFXID (2236) — FC class not visible from Modern-Common
            world.playAuxSFX(2236, i, j, k, iBlockID);

            return true;
        }

        return false;
    }

    public boolean canPlaceItemBlockOnSide(World world, int x, int y, int z, int side, EntityPlayer player, ItemStack stack) {
        return true;
    }

    // --- 1.5.2 (FCMOD) ItemBlock delegating overrides (vanilla/server/.../ItemBlock.java:142-269)
    // — FC sets these properties on the Block, so the item form must read/write through it.
    // Null-guarded like onItemUse above (shim blocksList can be sparser than real BTW's). ---

    // 1.5.2 ItemBlock.GetBuoyancy/SetBuoyancy — EntityItem.UpdateHardcoreBuoy (jar-excluded fc class)
    @Override
    public float GetBuoyancy(int iItemDamage) {
        Block block = Block.blocksList[blockID];
        return block != null ? block.GetBuoyancy(iItemDamage) : super.GetBuoyancy(iItemDamage);
    }

    @Override
    public Item SetBuoyancy(float fBuoyancy) {
        Block block = Block.blocksList[blockID];
        if (block != null) {
            block.SetBuoyancy(fBuoyancy);
        }

        return super.SetBuoyancy(fBuoyancy);
    }

    // 1.5.2 ItemBlock piston-packing delegation — FC piston packing reads these off item stacks
    @Override
    public boolean IsPistonPackable(ItemStack stack) {
        Block block = Block.blocksList[blockID];
        return block != null ? block.IsPistonPackable(stack) : super.IsPistonPackable(stack);
    }

    @Override
    public int GetRequiredItemCountToPistonPack(ItemStack stack) {
        Block block = Block.blocksList[blockID];
        return block != null ? block.GetRequiredItemCountToPistonPack(stack) : super.GetRequiredItemCountToPistonPack(stack);
    }

    @Override
    public int GetResultingBlockIDOnPistonPack(ItemStack stack) {
        Block block = Block.blocksList[blockID];
        return block != null ? block.GetResultingBlockIDOnPistonPack(stack) : super.GetResultingBlockIDOnPistonPack(stack);
    }

    @Override
    public int GetResultingBlockMetadataOnPistonPack(ItemStack stack) {
        Block block = Block.blocksList[blockID];
        return block != null ? block.GetResultingBlockMetadataOnPistonPack(stack) : super.GetResultingBlockMetadataOnPistonPack(stack);
    }

    // 1.5.2 ItemBlock.GetFurnaceBurnTime/SetFurnaceBurnTime — TileEntityFurnace.java:155/162 and
    // FCBlockCampfire fuel checks; FC sets burn times on the Block (wood, siding, etc.)
    @Override
    public int GetFurnaceBurnTime(int iItemDamage) {
        Block block = Block.blocksList[blockID];
        return block != null ? block.GetFurnaceBurnTime(iItemDamage) : super.GetFurnaceBurnTime(iItemDamage);
    }

    @Override
    public Item SetFurnaceBurnTime(int iBurnTime) {
        Block block = Block.blocksList[blockID];
        if (block != null) {
            block.SetFurnaceBurnTime(iBurnTime);
        }

        return super.SetFurnaceBurnTime(iBurnTime);
    }

    // 1.5.2 ItemBlock hopper-filter delegation — FCTileEntityHopper.java:447 and
    // FCBlockGrate/Slats/WickerPane/IronBars/TrapDoor/Ladder GetFilterableProperties callers
    @Override
    public boolean CanItemPassIfFilter(ItemStack filteredItem) {
        Block block = Block.blocksList[blockID];
        return block != null ? block.CanItemPassIfFilter(filteredItem) : super.CanItemPassIfFilter(filteredItem);
    }

    @Override
    public int GetFilterableProperties(ItemStack stack) {
        Block block = Block.blocksList[blockID];
        return block != null ? block.GetFilterableProperties(stack) : super.GetFilterableProperties(stack);
    }

    @Override
    public boolean CanTransformItemIfFilter(ItemStack filteredItem) {
        Block block = Block.blocksList[blockID];
        return block != null ? block.CanTransformItemIfFilter(filteredItem) : super.CanTransformItemIfFilter(filteredItem);
    }

    // 1.5.2 ItemBlock animal-food-value delegation (vanilla/server ItemBlock.java:197-269) —
    // FC sets herbivore/chicken/pig food values on the Block (hay, seeds, planter soil...);
    // the item form reads/writes through it so FCEntityCow/Chicken/Pig feeding works.
    @Override
    public int GetHerbivoreFoodValue(int iItemDamage) {
        Block block = Block.blocksList[blockID];
        return block != null ? block.GetHerbivoreItemFoodValue(iItemDamage) : super.GetHerbivoreFoodValue(iItemDamage);
    }

    @Override
    public Item SetHerbivoreFoodValue(int iFoodValue) {
        Block block = Block.blocksList[blockID];
        if (block != null) {
            block.SetHerbivoreItemFoodValue(iFoodValue);
        }
        return super.SetHerbivoreFoodValue(iFoodValue);
    }

    @Override
    public int GetChickenFoodValue(int iItemDamage) {
        Block block = Block.blocksList[blockID];
        return block != null ? block.GetChickenItemFoodValue(iItemDamage) : super.GetChickenFoodValue(iItemDamage);
    }

    @Override
    public Item SetChickenFoodValue(int iFoodValue) {
        Block block = Block.blocksList[blockID];
        if (block != null) {
            block.SetChickenItemFoodValue(iFoodValue);
        }
        return super.SetChickenFoodValue(iFoodValue);
    }

    @Override
    public int GetPigFoodValue(int iItemDamage) {
        Block block = Block.blocksList[blockID];
        return block != null ? block.GetPigItemFoodValue(iItemDamage) : super.GetPigFoodValue(iItemDamage);
    }

    @Override
    public Item SetPigFoodValue(int iFoodValue) {
        Block block = Block.blocksList[blockID];
        if (block != null) {
            block.SetPigItemFoodValue(iFoodValue);
        }
        return super.SetPigFoodValue(iFoodValue);
    }
}
