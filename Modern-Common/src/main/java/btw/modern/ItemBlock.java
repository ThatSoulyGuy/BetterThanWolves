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

    /** 1.5.2 ItemBlock.field_94588_b: the item icon registered from Block.getItemIconName (null for nearly all). */
    private Icon itemBlockIcon;

    /** Block classes whose getIcon threw for the item icon; only the log is once per class (see getIconFromDamage). */
    private static final java.util.Set<Class<?>> LOGGED_ICON_FAILURES =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    private Block getBlockForItem() {
        return this.blockID >= 0 && this.blockID < Block.blocksList.length ? Block.blocksList[this.blockID] : null;
    }

    // 1.5.2 ItemBlock.getSpriteNumber (vanilla/client ItemBlock.java:42-48): 0 (/terrain.png) unless the block names
    // an item icon. ItemStack.getItemSpriteNumber reads it for ItemRenderer.renderItem (mob-held and head-slot
    // items), RenderItem.doRenderItem (items FC tile renderers embed) and FCTileEntityBasketWickerRenderer, which
    // take their 3D-block branch only for 0; with Item's inherited 1 every block item was drawn as a flat icon.
    // The capture resolves sprites by icon name, so the number only picks the branch and the sheet bind.
    @Override
    public int getItemSpriteNumber() {
        Block block = this.getBlockForItem();
        if (block == null) {
            return super.getItemSpriteNumber();
        }
        return block.getItemIconName() != null ? 1 : 0;
    }

    // 1.5.2 ItemBlock.getIconFromDamage (vanilla/client ItemBlock.java:50-56): the registered item icon, else the
    // block's top texture at metadata 0 (the flat icon of a held torch, flower or rail). Item's inherited body
    // returned the NamedIcon of the unlocalized name, which names no texture for most blocks. Subclasses with
    // per-damage icons (FCItemBlockLeaves, FCItemBlockLegacySiding, ...) still override this.
    // Port: block icons are registered lazily (FCBakedModel's global registerIcons pass runs on the first FC block
    // capture), so the top icon can still be null here; 1.5.2 registers every block icon before anything draws.
    // Rather than return null (ItemRenderer.renderItem would then draw nothing), fall back to Item's registered
    // itemIcon, the icon these items drew before this override.
    @Override
    public Icon getIconFromDamage(int damage) {
        if (this.itemBlockIcon != null) {
            return this.itemBlockIcon;
        }
        Block block = this.getBlockForItem();
        if (block == null) {
            return super.getIconFromDamage(damage);
        }
        Icon icon;
        try {
            icon = block.getBlockTextureFromSide(1);
        } catch (Throwable t) {
            // An FC getIcon override that throws (e.g. before its icons are registered) must not abort the
            // entity / tile-entity capture drawing this item. The failure may be transient, so every call
            // retries; only the log is once per class.
            if (LOGGED_ICON_FAILURES.add(block.getClass())) {
                org.apache.logging.log4j.LogManager.getLogger("BTW-ItemBlock").warn(
                        "{}.getIcon failed for its item icon; using the item's own icon while it fails: {}",
                        block.getClass().getSimpleName(), t.toString());
            }
            icon = null;
        }
        return icon != null ? icon : super.getIconFromDamage(damage);
    }

    @Override
    public Icon GetHopperFilterIcon() {
        Block block = Block.blocksList[blockID];
        return block != null ? block.GetHopperFilterIcon() : null;
    }

    @Override
    public boolean IsIncineratedInCrucible() {
        Block block = Block.blocksList[blockID];
        return block != null && block.IsIncineratedInCrucible();
    }

    @Override
    public void registerIcons(IconRegister register) {
        super.registerIcons(register);
        Block block = this.getBlockForItem();
        String iconName = block != null ? block.getItemIconName() : null;
        if (iconName != null) {
            this.itemBlockIcon = register.registerIcon(iconName);
        }
    }

    /** Block class + hook group pairs that threw in onItemUse; only the log is once per pair. */
    private static final java.util.Set<String> LOGGED_PLACE_FAILURES =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static void logPlaceFailureOnce(Block block, String hooks, Throwable t) {
        if (LOGGED_PLACE_FAILURES.add(block.getClass().getName() + '/' + hooks)) {
            org.apache.logging.log4j.LogManager.getLogger("BTW-ItemBlock").warn(
                    "{} {} failed while placing it from an item: {}", block.getClass().getSimpleName(), hooks, t.toString());
        }
    }

    // The metadata onItemUse / OnItemUsedByBlockDispenser place. An FC ItemBlock is FCItemPlacesAsBlock.getMetadata
    // (Common FCItemPlacesAsBlock.java:126, m_iBlockMetadata = 0) unless its FC subclass overrides it (FCItemBlockDirtSlab:
    // damage << 1). The 1.5.2 vanilla ItemBlock subclasses return the damage (vanilla/client ItemCloth.java:37,
    // ItemMultiTextureTile.java:42, ItemBlockWithMetadata.java:40, ItemSlab.java:48, ItemColored.java:50) or, for leaves,
    // damage | 4 (ItemLeaves.java:29). Port: the ItemMultiTextureTile / ItemBlockWithMetadata / ItemColored stubs
    // (FCBetterThanWolves.java:1880-1896 registers them at planks, stone, cobble and the ores; FCItemBlockLog,
    // FCItemBlockSnow and FCItemBlockLilyPad extend them) have no override, and the Block constructor registers a plain
    // ItemBlock at the ids where vanilla/client Block.java:1546-1565 registered ItemCloth, ItemSlab, ItemLeaves,
    // ItemColored and ItemMultiTextureTile. Both get the 1.5.2 rule here; with Item's 0 every wool colour, wood species,
    // slab, sandstone, stone brick and strata stone placed through onItemUse came out as its meta-0 variant.
    // Not ported: ItemPiston's 7 (BlockPistonBase.onBlockPlacedBy sets the facing; the port placed 0 before) and
    // ItemAnvilBlock's damage << 2 (FC registers FCItemBlockDeadWeight at the anvil, FCBetterThanWolves.java:1899).
    @Override
    public int getMetadata(int damage) {
        if (this instanceof ItemMultiTextureTile || this instanceof ItemBlockWithMetadata || this instanceof ItemColored) {
            return damage;
        }
        if (this.getClass() == ItemBlock.class) {
            switch (this.blockID) {
                case 5: case 6: case 17: case 24: case 31: case 35: case 43: case 44: case 97: case 98:
                case 106: case 125: case 126: case 139: case 155:
                    return damage;
                case 18:
                    return damage | 4;
                default:
                    break;
            }
        }
        return super.getMetadata(damage);
    }

    /**
     * Returns the block ID to place. Subclasses like FCItemBlockLegacySubstitution
     * override this to place a different block than the one this item represents.
     */
    public int GetBlockIDToPlace(int iItemDamage, int iFacing, float fClickX, float fClickY, float fClickZ) {
        return this.blockID;
    }

    // 1.5.2 ItemBlock inherits FCItemPlacesAsBlock.onItemUse (Common FCItemPlacesAsBlock.java:60-123; FC removed vanilla's
    // own, vanilla/client ItemBlock.java:62-143). ProxyBlock.tryPlaceHeldItem runs it for every block item placed against
    // an FC block or an FC-replaced vanilla block, i.e. most placement. The old stand-in passed the raw item damage as the
    // metadata instead of getMetadata(damage) (a packed-earth or grass slab, FCItemBlockDirtSlab.getMetadata = damage << 1,
    // placed the wrong subtype), always placed beside the clicked block (never into replaceable ground cover), and
    // skipped PreBlockPlacedBy, onPostBlockPlaced and the animal-spooking notification.
    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world,
                              int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        int newBlockID = GetBlockIDToPlace(stack.getItemDamage(), side, hitX, hitY, hitZ);
        // port guard: 1.5.2 dereferences Block.blocksList[newBlockID] unguarded
        Block newBlock = newBlockID > 0 && newBlockID < Block.blocksList.length ? Block.blocksList[newBlockID] : null;
        if (newBlock == null) return false;

        // 1.5.2 refuses a solid block clicked against the top layer (j == 255, World.getHeight() - 1); the port world's
        // top is WorldBridge.getHeight() (the level's max build height).
        // (material null-guards: this runs in the use-item packet handler, which rethrows)
        if (stack.stackSize == 0
                || (player != null && !player.canPlayerEdit(x, y, z, side, stack))
                || (y == world.getHeight() - 1 && newBlock.blockMaterial != null && newBlock.blockMaterial.isSolid())) {
            return false;
        }

        FCUtilsBlockPos targetPos = new FCUtilsBlockPos(x, y, z);

        int oldBlockID = world.getBlockId(x, y, z);
        Block oldBlock = oldBlockID > 0 && oldBlockID < Block.blocksList.length ? Block.blocksList[oldBlockID] : null;

        if (oldBlock != null) {
            if (oldBlock.IsGroundCover()) {
                side = 1;
            } else if (oldBlock.blockMaterial == null || !oldBlock.blockMaterial.isReplaceable()) {
                targetPos.AddFacingAsOffset(side);
            }
        } else if (!world.isAirBlock(x, y, z)) {
            // port: id 0 is also a block with no legacy id (1.5.2 id 0 was only air, which cannot be clicked)
            targetPos.AddFacingAsOffset(side);
        }

        // 1.5.2 passes the player here, which World.canPlaceEntityOnSide excludes from the entity check, but the 1.5.2
        // client never sent the request when its own pre-check failed (PlayerControllerMP.java:419-426 ->
        // FCItemPlacesAsBlock.canPlaceItemBlockOnSide, which passes a null entity). The 1.20.1 client sends it anyway,
        // so the server applies the client's null-entity check: no block placed into the player's own space.
        if (world.canPlaceEntityOnSide(newBlockID, targetPos.i, targetPos.j, targetPos.k, false, side, null, stack)) {
            // port guards (this runs in the use-item packet handler, which rethrows, and its callers do not catch):
            // an FC hook that throws before the block is set places nothing and uses nothing; one that throws after
            // it still leaves the placement and the stackSize-- below paired, so a failing hook gives no free blocks.
            int newMetadata;
            try {
                newMetadata = getMetadata(stack.getItemDamage());

                newMetadata = newBlock.onBlockPlaced(world, targetPos.i, targetPos.j, targetPos.k, side, hitX, hitY, hitZ, newMetadata);

                newMetadata = newBlock.PreBlockPlacedBy(world, targetPos.i, targetPos.j, targetPos.k, newMetadata, player);
            } catch (Throwable t) {
                logPlaceFailureOnce(newBlock, "onBlockPlaced / PreBlockPlacedBy", t);
                return false;
            }

            if (world.setBlockAndMetadataWithNotify(targetPos.i, targetPos.j, targetPos.k, newBlockID, newMetadata)) {
                try {
                    if (world.getBlockId(targetPos.i, targetPos.j, targetPos.k) == newBlockID) {
                        newBlock.onBlockPlacedBy(world, targetPos.i, targetPos.j, targetPos.k, player, stack);

                        newBlock.onPostBlockPlaced(world, targetPos.i, targetPos.j, targetPos.k, newMetadata);

                        // Panick animals when blocks are placed near them (guarded: it reads player.capabilities and the
                        // block's material)
                        if (player != null && newBlock.blockMaterial != null) {
                            world.NotifyNearbyAnimalsOfPlayerBlockAddOrRemove(player, newBlock, targetPos.i, targetPos.j, targetPos.k);
                        }
                    }
                } catch (Throwable t) {
                    logPlaceFailureOnce(newBlock, "onBlockPlacedBy / onPostBlockPlaced", t);
                }

                PlayPlaceSound(world, targetPos.i, targetPos.j, targetPos.k, newBlock);

                stack.stackSize--;
            }

            return true;
        }

        return false;
    }

    // 1.5.2 FCItemPlacesAsBlock.PlayPlaceSound (Common FCItemPlacesAsBlock.java:225-229; vanilla/client ItemBlock.java:131):
    // the place sound at (volume + 1) / 2 and pitch * 0.8. The stand-in played the step sound at full volume and pitch.
    public void PlayPlaceSound(World world, int x, int y, int z, Block block) {
        if (block.stepSound != null) {
            world.playSoundEffect((double) x + 0.5D, (double) y + 0.5D, (double) z + 0.5D, block.stepSound.getPlaceSound(),
                    (block.stepSound.getVolume() + 1F) / 2F, block.stepSound.getPitch() * 0.8F);
        }
    }

    // 1.5.2 ItemBlock.getUnlocalizedName(ItemStack) / getUnlocalizedName() (vanilla/client ItemBlock.java:200-211): the
    // block's name, which 1.5.2 Block.getUnlocalizedName returns as "tile." + name (vanilla/client Block.java:1352-1355).
    // Modern-Common Block.getUnlocalizedName returns the bare name (other callers rely on that), so the prefix is added
    // here to getUnlocalizedName2, the bare name. With Item's inherited "item." + the item's own name, FC's per-subtype
    // names (FCItemBlockAestheticOpaque.getUnlocalizedName: super.getUnlocalizedName() + ".soap") missed every lang key
    // (tile.fcBlockAestheticOpaque.soap.name, BTW_en_US.lang).
    @Override
    public String getUnlocalizedName(ItemStack stack) {
        Block block = this.getBlockForItem();
        return block != null ? "tile." + block.getUnlocalizedName2() : super.getUnlocalizedName(stack);
    }

    @Override
    public String getUnlocalizedName() {
        Block block = this.getBlockForItem();
        return block != null ? "tile." + block.getUnlocalizedName2() : super.getUnlocalizedName();
    }

    // 1.5.2 ItemBlock.getCreativeTab (vanilla/client ItemBlock.java:216-219): the block's tab. FC hides its technical
    // blocks by leaving it null (FCBlockAxlePowerSource.java:32, FCBlockTorchFiniteBurning.java:39, ...).
    @Override
    public CreativeTabs getCreativeTab() {
        Block block = this.getBlockForItem();
        return block != null ? block.getCreativeTabToDisplayOn() : super.getCreativeTab();
    }

    // 1.5.2 ItemBlock.getSubItems (vanilla/client ItemBlock.java:224-227): the block's getSubBlocks lists the creative
    // subtypes (11 FCBlockAestheticOpaque subtypes without meta 0, 16 wool slab colours, none for FCBlockAestheticNonOpaque).
    @SuppressWarnings("rawtypes")
    @Override
    public void getSubItems(int itemID, CreativeTabs creativeTabs, java.util.List list) {
        Block block = this.getBlockForItem();
        if (block != null) {
            block.getSubBlocks(itemID, creativeTabs, list);
        } else {
            super.getSubItems(itemID, creativeTabs, list);
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
