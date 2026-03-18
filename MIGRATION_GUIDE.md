# BTW Migration Guide — IntelliJ Manual Steps

## Prerequisites
1. Open the project in IntelliJ IDEA (it auto-detects the Gradle modules)
2. Wait for indexing to complete
3. Verify the build works: ./gradlew buildAll

## STEP 1: Change `extends VanillaClass` → `extends AbstractVanillaClass`

Use IntelliJ's **Find and Replace in Path** (Ctrl+Shift+R).
Scope: Common/src, Client/src, Server/src
Check **Regex** mode.

Run these replacements IN ORDER (longest class names first to avoid partial matches):

| Find (regex) | Replace |
|---|---|
| `extends AbstractEntityAINearestAttackableTarget` | `extends AbstractEntityAINearestAttackableTarget` |
| `extends AbstractBehaviorProjectileDispense` | `extends AbstractBehaviorProjectileDispense` |
| `extends AbstractTileEntitySpecialRenderer` | `extends AbstractTileEntitySpecialRenderer` |
| `extends AbstractRandomPositionGenerator` | `extends AbstractRandomPositionGenerator` |
| `extends AbstractEntityAIAttackOnCollide` | `extends AbstractEntityAIAttackOnCollide` |
| `extends AbstractItemBlockWithMetadata` | `extends AbstractItemBlockWithMetadata` |
| `extends AbstractBlockRedstoneRepeater` | `extends AbstractBlockRedstoneRepeater` |
| `extends AbstractBlockEnchantmentTable` | `extends AbstractBlockEnchantmentTable` |
| `extends AbstractBlockDaylightDetector` | `extends AbstractBlockDaylightDetector` |
| `extends AbstractTileEntityMobSpawner` | `extends AbstractTileEntityMobSpawner` |
| `extends AbstractTileEntityEnderChest` | `extends AbstractTileEntityEnderChest` |
| `extends AbstractItemMultiTextureTile` | `extends AbstractItemMultiTextureTile` |
| `extends AbstractEntityAICreeperSwell` | `extends AbstractEntityAICreeperSwell` |
| `extends AbstractBlockPistonExtension` | `extends AbstractBlockPistonExtension` |
| `extends AbstractEntityWeatherEffect` | `extends AbstractEntityWeatherEffect` |
| `extends AbstractBlockTripWireSource` | `extends AbstractBlockTripWireSource` |
| `extends AbstractBlockEndPortalFrame` | `extends AbstractBlockEndPortalFrame` |
| `extends AbstractWeightedRandomItem` | `extends AbstractWeightedRandomItem` |
| `extends AbstractMapGenNetherBridge` | `extends AbstractMapGenNetherBridge` |
| `extends AbstractItemCarrotOnAStick` | `extends AbstractItemCarrotOnAStick` |
| `extends AbstractContainerWorkbench` | `extends AbstractContainerWorkbench` |
| `extends AbstractBlockRedstoneLight` | `extends AbstractBlockRedstoneLight` |
| `extends AbstractBlockPressurePlate` | `extends AbstractBlockPressurePlate` |
| `extends AbstractTileEntityFurnace` | `extends AbstractTileEntityFurnace` |
| `extends AbstractItemEnchantedBook` | `extends AbstractItemEnchantedBook` |
| `extends AbstractEntityWitherSkull` | `extends AbstractEntityWitherSkull` |
| `extends AbstractEntityFallingSand` | `extends AbstractEntityFallingSand` |
| `extends AbstractBlockRedstoneWire` | `extends AbstractBlockRedstoneWire` |
| `extends AbstractBlockPistonMoving` | `extends AbstractBlockPistonMoving` |
| `extends AbstractBlockDetectorRail` | `extends AbstractBlockDetectorRail` |
| `extends BlockBrewingStand` | `extends AbstractBlockBrewingStand` |
| `extends TileEntityBeacon` | `extends AbstractTileEntityBeacon` |
| `extends ItemSimpleFoiled` | `extends AbstractItemSimpleFoiled` |
| `extends ItemEditableBook` | `extends AbstractItemEditableBook` |
| `extends BlockNetherStalk` | `extends AbstractBlockNetherStalk` |
| `extends BlockMushroomCap` | `extends AbstractBlockMushroomCap` |
| `extends WorldGenBigTree` | `extends AbstractWorldGenBigTree` |
| `extends TileEntityChest` | `extends AbstractTileEntityChest` |
| `extends TextureStitched` | `extends AbstractTextureStitched` |
| `extends ItemGlassBottle` | `extends AbstractItemGlassBottle` |
| `extends EntityThrowable` | `extends AbstractEntityThrowable` |
| `extends EntityPigZombie` | `extends AbstractEntityPigZombie` |
| `extends EntityMagmaCube` | `extends AbstractEntityMagmaCube` |
| `extends ContainerPlayer` | `extends AbstractContainerPlayer` |
| `extends BlockStoneBrick` | `extends AbstractBlockStoneBrick` |
| `extends BlockStationary` | `extends AbstractBlockStationary` |
| `extends BlockSilverfish` | `extends AbstractBlockSilverfish` |
| `extends BlockPistonBase` | `extends AbstractBlockPistonBase` |
| `extends BlockMobSpawner` | `extends AbstractBlockMobSpawner` |
| `extends BlockEnderChest` | `extends AbstractBlockEnderChest` |
| `extends BlockComparator` | `extends AbstractBlockComparator` |
| `extends WorldGenerator` | `extends AbstractWorldGenerator` |
| `extends ModelQuadruped` | `extends AbstractModelQuadruped` |
| `extends ItemFishingRod` | `extends AbstractItemFishingRod` |
| `extends InventoryBasic` | `extends AbstractInventoryBasic` |
| `extends EntityWaterMob` | `extends AbstractEntityWaterMob` |
| `extends EntityVillager` | `extends AbstractEntityVillager` |
| `extends EntitySkeleton` | `extends AbstractEntitySkeleton` |
| `extends EntityEnderman` | `extends AbstractEntityEnderman` |
| `extends EntityCreature` | `extends AbstractEntityCreature` |
| `extends BlockWorkbench` | `extends AbstractBlockWorkbench` |
| `extends BlockTallGrass` | `extends AbstractBlockTallGrass` |
| `extends BlockSnowBlock` | `extends AbstractBlockSnowBlock` |
| `extends BlockSandStone` | `extends AbstractBlockSandStone` |
| `extends BlockGlowStone` | `extends AbstractBlockGlowStone` |
| `extends BlockFlowerPot` | `extends AbstractBlockFlowerPot` |
| `extends BlockFenceGate` | `extends AbstractBlockFenceGate` |
| `extends BlockEndPortal` | `extends AbstractBlockEndPortal` |
| `extends BlockDragonEgg` | `extends AbstractBlockDragonEgg` |
| `extends BlockDispenser` | `extends AbstractBlockDispenser` |
| `extends BlockContainer` | `extends AbstractBlockContainer` |
| `extends BiomeGenJungle` | `extends AbstractBiomeGenJungle` |
| `extends BiomeGenForest` | `extends AbstractBiomeGenForest` |
| `extends BiomeGenDesert` | `extends AbstractBiomeGenDesert` |
| `extends ShapedRecipes` | `extends AbstractShapedRecipes` |
| `extends EntitySnowman` | `extends AbstractEntitySnowman` |
| `extends EntityCreeper` | `extends AbstractEntityCreeper` |
| `extends EntityChicken` | `extends AbstractEntityChicken` |
| `extends BlockTripWire` | `extends AbstractBlockTripWire` |
| `extends BlockTrapDoor` | `extends AbstractBlockTrapDoor` |
| `extends BlockSoulSand` | `extends AbstractBlockSoulSand` |
| `extends BlockObsidian` | `extends AbstractBlockObsidian` |
| `extends BlockMycelium` | `extends AbstractBlockMycelium` |
| `extends BlockMushroom` | `extends AbstractBlockMushroom` |
| `extends BlockHalfSlab` | `extends AbstractBlockHalfSlab` |
| `extends BlockDeadBush` | `extends AbstractBlockDeadBush` |
| `extends BlockCauldron` | `extends AbstractBlockCauldron` |
| `extends BiomeGenTaiga` | `extends AbstractBiomeGenTaiga` |
| `extends AxisAlignedBB` | `extends AbstractAxisAlignedBB` |
| `extends RenderSpider` | `extends AbstractRenderSpider` |
| `extends RenderLiving` | `extends AbstractRenderLiving` |
| `extends ModelChicken` | `extends AbstractModelChicken` |
| `extends ItemSnowball` | `extends AbstractItemSnowball` |
| `extends ItemMinecart` | `extends AbstractItemMinecart` |
| `extends ItemFireball` | `extends AbstractItemFireball` |
| `extends ItemEmptyMap` | `extends AbstractItemEmptyMap` |
| `extends GuiContainer` | `extends AbstractGuiContainer` |
| `extends EntityZombie` | `extends AbstractEntityZombie` |
| `extends EntityWither` | `extends AbstractEntityWither` |
| `extends EntitySpider` | `extends AbstractEntitySpider` |
| `extends EntityOcelot` | `extends AbstractEntityOcelot` |
| `extends EntityAIBase` | `extends AbstractEntityAIBase` |
| `extends DamageSource` | `extends AbstractDamageSource` |
| `extends BlockSapling` | `extends AbstractBlockSapling` |
| `extends BlockPumpkin` | `extends AbstractBlockPumpkin` |
| `extends BlockLilyPad` | `extends AbstractBlockLilyPad` |
| `extends BlockJukeBox` | `extends AbstractBlockJukeBox` |
| `extends BlockFurnace` | `extends AbstractBlockFurnace` |
| `extends BlockFlowing` | `extends AbstractBlockFlowing` |
| `extends BiomeGenSnow` | `extends AbstractBiomeGenSnow` |
| `extends BiomeGenHell` | `extends AbstractBiomeGenHell` |
| `extends BiomeGenBase` | `extends AbstractBiomeGenBase` |
| `extends ItemLilyPad` | `extends AbstractItemLilyPad` |
| `extends EntityWitch` | `extends AbstractEntityWitch` |
| `extends EntitySlime` | `extends AbstractEntitySlime` |
| `extends EntitySheep` | `extends AbstractEntitySheep` |
| `extends EntityGhast` | `extends AbstractEntityGhast` |
| `extends EntityBlaze` | `extends AbstractEntityBlaze` |
| `extends EntityArrow` | `extends AbstractEntityArrow` |
| `extends CommandBase` | `extends AbstractCommandBase` |
| `extends BlockQuartz` | `extends AbstractBlockQuartz` |
| `extends BlockPotato` | `extends AbstractBlockPotato` |
| `extends BlockPortal` | `extends AbstractBlockPortal` |
| `extends BlockLeaves` | `extends AbstractBlockLeaves` |
| `extends BlockHopper` | `extends AbstractBlockHopper` |
| `extends BlockFlower` | `extends AbstractBlockFlower` |
| `extends BlockCarrot` | `extends AbstractBlockCarrot` |
| `extends BlockCactus` | `extends AbstractBlockCactus` |
| `extends BlockButton` | `extends AbstractBlockButton` |
| `extends BlockBeacon` | `extends AbstractBlockBeacon` |
| `extends BiomeGenEnd` | `extends AbstractBiomeGenEnd` |
| `extends TileEntity` | `extends AbstractTileEntity` |
| `extends RenderWolf` | `extends AbstractRenderWolf` |
| `extends ModelSquid` | `extends AbstractModelSquid` |
| `extends ModelBiped` | `extends AbstractModelBiped` |
| `extends ItemShears` | `extends AbstractItemShears` |
| `extends ItemPotion` | `extends AbstractItemPotion` |
| `extends EntityWolf` | `extends AbstractEntityWolf` |
| `extends EntityItem` | `extends AbstractEntityItem` |
| `extends BlockSkull` | `extends AbstractBlockSkull` |
| `extends BlockLever` | `extends AbstractBlockLever` |
| `extends BlockGrass` | `extends AbstractBlockGrass` |
| `extends BlockGlass` | `extends AbstractBlockGlass` |
| `extends BlockCrops` | `extends AbstractBlockCrops` |
| `extends BlockCocoa` | `extends AbstractBlockCocoa` |
| `extends BlockCloth` | `extends AbstractBlockCloth` |
| `extends BlockChest` | `extends AbstractBlockChest` |
| `extends StepSound` | `extends AbstractStepSound` |
| `extends ModelBase` | `extends AbstractModelBase` |
| `extends ItemSword` | `extends AbstractItemSword` |
| `extends ItemBlock` | `extends AbstractItemBlock` |
| `extends ItemArmor` | `extends AbstractItemArmor` |
| `extends EntityPig` | `extends AbstractEntityPig` |
| `extends EntityCow` | `extends AbstractEntityCow` |
| `extends EntityBat` | `extends AbstractEntityBat` |
| `extends Container` | `extends AbstractContainer` |
| `extends BlockWall` | `extends AbstractBlockWall` |
| `extends BlockVine` | `extends AbstractBlockVine` |
| `extends BlockStep` | `extends AbstractBlockStep` |
| `extends BlockStem` | `extends AbstractBlockStem` |
| `extends BlockSign` | `extends AbstractBlockSign` |
| `extends BlockReed` | `extends AbstractBlockReed` |
| `extends BlockRail` | `extends AbstractBlockRail` |
| `extends BlockPane` | `extends AbstractBlockPane` |
| `extends BlockNote` | `extends AbstractBlockNote` |
| `extends BlockFire` | `extends AbstractBlockFire` |
| `extends BlockDoor` | `extends AbstractBlockDoor` |
| `extends BlockCake` | `extends AbstractBlockCake` |
| `extends ModelPig` | `extends AbstractModelPig` |
| `extends Material` | `extends AbstractMaterial` |
| `extends ItemSign` | `extends AbstractItemSign` |
| `extends ItemReed` | `extends AbstractItemReed` |
| `extends ItemFood` | `extends AbstractItemFood` |
| `extends ItemBook` | `extends AbstractItemBook` |
| `extends ItemBoat` | `extends AbstractItemBoat` |
| `extends EntityFX` | `extends AbstractEntityFX` |
| `extends BlockWeb` | `extends AbstractBlockWeb` |
| `extends BlockTNT` | `extends AbstractBlockTNT` |
| `extends BlockOre` | `extends AbstractBlockOre` |
| `extends BlockLog` | `extends AbstractBlockLog` |
| `extends BlockIce` | `extends AbstractBlockIce` |
| `extends BlockBed` | `extends AbstractBlockBed` |
| `extends ItemMap` | `extends AbstractItemMap` |
| `extends ItemDye` | `extends AbstractItemDye` |
| `extends ItemBow` | `extends AbstractItemBow` |
| `extends Render` | `extends AbstractRender` |
| `extends Packet` | `extends AbstractPacket` |
| `extends Entity` | `extends AbstractEntity` |
| `extends Block` | `extends AbstractBlock` |
| `extends Item` | `extends AbstractItem` |

**IMPORTANT:** Only replace in FC class declarations (class FC... extends).
Do NOT replace FC-to-FC extends (like FCBlockDirt extends FCBlockFullBlock).
The regex `class FC\w+ extends ` ensures only FC classes are matched.

After each replacement, add the import at the top of changed files:
`import btw.adapter.block.*;` (or .entity.*, .item.*, etc.)

## STEP 2: Rename override methods to _btw suffix

For each bridge method, find all FC overrides and rename them.

Use IntelliJ's **Structural Search & Replace** (Edit → Find → Replace Structurally):
Or use **Find and Replace in Path** with regex.

### Block bridge methods (most common):

- `getBlockHardness(...)` → `getBlockHardness_btw(...)` — 12 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `addCollisionBoxesToList(...)` → `addCollisionBoxesToList_btw(...)` — 28 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `updateTick(...)` → `updateTick_btw(...)` — 149 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `onBlockDestroyedByPlayer(...)` → `onBlockDestroyedByPlayer_btw(...)` — 4 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `onNeighborBlockChange(...)` → `onNeighborBlockChange_btw(...)` — 125 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `tickRate(...)` → `tickRate_btw(...)` — 61 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `onBlockAdded(...)` → `onBlockAdded_btw(...)` — 88 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `breakBlock(...)` → `breakBlock_btw(...)` — 55 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `dropBlockAsItemWithChance(...)` → `dropBlockAsItemWithChance_btw(...)` — 33 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `dropBlockAsItem_do(...)` → `dropBlockAsItem_do_btw(...)` — 4 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `onBlockDestroyedByExplosion(...)` → `onBlockDestroyedByExplosion_btw(...)` — 29 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `canPlaceBlockOnSide(...)` → `canPlaceBlockOnSide_btw(...)` — 9 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `canPlaceBlockOnSide(...)` → `canPlaceBlockOnSide_btw(...)` — 9 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `canPlaceBlockAt(...)` → `canPlaceBlockAt_btw(...)` — 49 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `onBlockActivated(...)` → `onBlockActivated_btw(...)` — 71 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `onBlockPlaced(...)` → `onBlockPlaced_btw(...)` — 91 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `onEntityCollidedWithBlock(...)` → `onEntityCollidedWithBlock_btw(...)` — 31 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `harvestBlock(...)` → `harvestBlock_btw(...)` — 15 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `canBlockStay(...)` → `canBlockStay_btw(...)` — 11 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `onBlockPlacedBy(...)` → `onBlockPlacedBy_btw(...)` — 38 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `onPostBlockPlaced(...)` → `onPostBlockPlaced_btw(...)` — 6 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `onFallenUpon(...)` → `onFallenUpon_btw(...)` — 2 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `getDamageValue(...)` → `getDamageValue_btw(...)` — 6 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `PreBlockPlacedBy(...)` → `PreBlockPlacedBy_btw(...)` — 10 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `RandomUpdateTick(...)` → `RandomUpdateTick_btw(...)` — 45 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `ClientNotificationOfMetadataChange(...)` → `ClientNotificationOfMetadataChange_btw(...)` — 3 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `OnArrowCollide(...)` → `OnArrowCollide_btw(...)` — 4 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `GetMovementModifier(...)` → `GetMovementModifier_btw(...)` — 33 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `DoesBlockHopperEject(...)` → `DoesBlockHopperEject_btw(...)` — 4 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `GetStepSound(...)` → `GetStepSound_btw(...)` — 8 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `ClientBreakBlock(...)` → `ClientBreakBlock_btw(...)` — 5 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `ClientBlockAdded(...)` → `ClientBlockAdded_btw(...)` — 2 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `IsBlockClimbable(...)` → `IsBlockClimbable_btw(...)` — 6 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `OnBlockDestroyedWithImproperTool(...)` → `OnBlockDestroyedWithImproperTool_btw(...)` — 52 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `DropComponentItemsOnBadBreak(...)` → `DropComponentItemsOnBadBreak_btw(...)` — 77 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `HasNeighborWithMortarInContact(...)` → `HasNeighborWithMortarInContact_btw(...)` — 4 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `HasStickySnowNeighborInContact(...)` → `HasStickySnowNeighborInContact_btw(...)` — 2 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `DoesExtinguishFireAbove(...)` → `DoesExtinguishFireAbove_btw(...)` — 2 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `OnDestroyedByFire(...)` → `OnDestroyedByFire_btw(...)` — 16 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `SetOnFireDirectly(...)` → `SetOnFireDirectly_btw(...)` — 17 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `GetCanBlockBeIncinerated(...)` → `GetCanBlockBeIncinerated_btw(...)` — 12 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `OnCookedByKiln(...)` → `OnCookedByKiln_btw(...)` — 4 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `DoesBlockBreakSaw(...)` → `DoesBlockBreakSaw_btw(...)` — 27 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `OnBlockSawed(...)` → `OnBlockSawed_btw(...)` — 12 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `OnBlockSawed(...)` → `OnBlockSawed_btw(...)` — 12 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `GetItemIDDroppedOnSaw(...)` → `GetItemIDDroppedOnSaw_btw(...)` — 31 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `GetItemCountDroppedOnSaw(...)` → `GetItemCountDroppedOnSaw_btw(...)` — 31 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `GetItemDamageDroppedOnSaw(...)` → `GetItemDamageDroppedOnSaw_btw(...)` — 8 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `GetMechanicalPowerLevelProvidedToAxleAtFacing(...)` → `GetMechanicalPowerLevelProvidedToAxleAtFacing_btw(...)` — 6 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `CanConvertBlock(...)` → `CanConvertBlock_btw(...)` — 26 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `ConvertBlock(...)` → `ConvertBlock_btw(...)` — 26 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `CanGroundCoverRestOnBlock(...)` → `CanGroundCoverRestOnBlock_btw(...)` — 53 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `GetCanBlightSpreadToBlock(...)` → `GetCanBlightSpreadToBlock_btw(...)` — 8 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `OnPreBlockPlacedByPiston(...)` → `OnPreBlockPlacedByPiston_btw(...)` — 4 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `CanBePistonShoveled(...)` → `CanBePistonShoveled_btw(...)` — 42 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `GetPistonShovelEjectDirection(...)` → `GetPistonShovelEjectDirection_btw(...)` — 2 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `CanItemPassIfFilter(...)` → `CanItemPassIfFilter_btw(...)` — 20 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `SetFacing(...)` → `SetFacing_btw(...)` — 72 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `ToggleFacing(...)` → `ToggleFacing_btw(...)` — 34 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `RotateOnTurntable(...)` → `RotateOnTurntable_btw(...)` — 68 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `RotateAroundJAxis(...)` → `RotateAroundJAxis_btw(...)` — 20 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `CanRotateAroundBlockOnTurntableToFacing(...)` → `CanRotateAroundBlockOnTurntableToFacing_btw(...)` — 15 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `GetNewMetadataRotatedAroundBlockOnTurntableToFacing(...)` → `GetNewMetadataRotatedAroundBlockOnTurntableToFacing_btw(...)` — 8 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `GetStackRetrievedByBlockDispenser(...)` → `GetStackRetrievedByBlockDispenser_btw(...)` — 32 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `CanMobsSpawnOn(...)` → `CanMobsSpawnOn_btw(...)` — 16 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `MobSpawnOnVerticalOffset(...)` → `MobSpawnOnVerticalOffset_btw(...)` — 14 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `getCollisionBoundingBoxFromPool(...)` → `getCollisionBoundingBoxFromPool_btw(...)` — 58 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `collisionRayTrace(...)` → `collisionRayTrace_btw(...)` — 58 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `CanBeGrazedOn(...)` → `CanBeGrazedOn_btw(...)` — 23 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `OnVegetationAboveGrazed(...)` → `OnVegetationAboveGrazed_btw(...)` — 14 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `OnNeighborDisrupted(...)` → `OnNeighborDisrupted_btw(...)` — 8 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `CanDomesticatedCropsGrowOnBlock(...)` → `CanDomesticatedCropsGrowOnBlock_btw(...)` — 6 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `CanSaplingsGrowOnBlock(...)` → `CanSaplingsGrowOnBlock_btw(...)` — 10 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `CanWildVegetationGrowOnBlock(...)` → `CanWildVegetationGrowOnBlock_btw(...)` — 10 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `IsBlockHydratedForPlantGrowthOn(...)` → `IsBlockHydratedForPlantGrowthOn_btw(...)` — 8 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `GetPlantGrowthOnMultiplier(...)` → `GetPlantGrowthOnMultiplier_btw(...)` — 6 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `RemoveWeeds(...)` → `RemoveWeeds_btw(...)` — 5 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack
- `AttemptToApplyFertilizerTo(...)` → `AttemptToApplyFertilizerTo_btw(...)` — 8 FC files
  Change param types: World→IWorld, Entity→IEntity, EntityPlayer→IEntityPlayer, ItemStack→IItemStack

### Entity bridge methods:
- `getCollisionBox(...)` → `getCollisionBox_btw(...)`

### Item bridge methods:
- `onItemUse(...)` → `onItemUse_btw(...)`
- `onItemRightClick(...)` → `onItemRightClick_btw(...)`
- `hitEntity(...)` → `hitEntity_btw(...)`
- `onBlockDestroyed(...)` → `onBlockDestroyed_btw(...)`
- `getDamageVsEntity(...)` → `getDamageVsEntity_btw(...)`
- `getUnlocalizedName(...)` → `getUnlocalizedName_btw(...)`
- `onCreated(...)` → `onCreated_btw(...)`
- `getItemUseAction(...)` → `getItemUseAction_btw(...)`
- `getMaxItemUseDuration(...)` → `getMaxItemUseDuration_btw(...)`
- `getItemDisplayName(...)` → `getItemDisplayName_btw(...)`
- `CanItemBeUsedByPlayer(...)` → `CanItemBeUsedByPlayer_btw(...)`
- `IsEfficientVsBlock(...)` → `IsEfficientVsBlock_btw(...)`
- `getStrVsBlock(...)` → `getStrVsBlock_btw(...)`
- `IsPistonPackable(...)` → `IsPistonPackable_btw(...)`
- `GetRequiredItemCountToPistonPack(...)` → `GetRequiredItemCountToPistonPack_btw(...)`
- `GetResultingBlockIDOnPistonPack(...)` → `GetResultingBlockIDOnPistonPack_btw(...)`
- `GetResultingBlockMetadataOnPistonPack(...)` → `GetResultingBlockMetadataOnPistonPack_btw(...)`
- `OnItemUsedByBlockDispenser(...)` → `OnItemUsedByBlockDispenser_btw(...)`

### Also change super.method() → super.method_btw():
When FC code calls `super.updateTick(world, ...)`, change to `super.updateTick_btw(world, ...)`

## STEP 3: Change parameter types in FC method bodies

After renaming methods, change the parameter types:

| Vanilla Type | Interface Type | Import |
|---|---|---|
| `World` | `IWorld` | `btw.api.world.IWorld` |
| `Entity` | `IEntity` | `btw.api.entity.IEntity` |
| `EntityLiving` | `IEntityLiving` | `btw.api.entity.IEntityLiving` |
| `EntityPlayer` | `IEntityPlayer` | `btw.api.entity.IEntityPlayer` |
| `EntityAnimal` | `IEntityAnimal` | `btw.api.entity.IEntityAnimal` |
| `ItemStack` | `IItemStack` | `btw.api.item.IItemStack` |
| `Block` | `IBlock` | `btw.api.block.IBlock` |
| `EntityFallingSand` | `IEntityFallingSand` | `btw.api.entity.IEntityFallingSand` |

Use IntelliJ's **Refactor → Migrate Type** or **Change Signature** for safety.

## STEP 4: Convert field access to getters

| Direct Access | Getter Call |
|---|---|
| `entity.posX` | `entity.getPosX()` |
| `entity.posY` | `entity.getPosY()` |
| `entity.posZ` | `entity.getPosZ()` |
| `entity.motionX` | `entity.getMotionX()` |
| `entity.motionY` | `entity.getMotionY()` |
| `entity.motionZ` | `entity.getMotionZ()` |
| `entity.rand` | `entity.getRandom()` |
| `entity.worldObj` | `entity.getWorldObj()` |
| `entity.boundingBox` | `entity.getBoundingBox()` |
| `world.rand` | `world.getRandom()` |
| `world.isRemote` | `world.getIsRemote()` |
| `block.blockID` | `block.getBlockID()` |
| `block.blockHardness` | `block.getHardness()` |
| `stack.itemID` | `stack.getItemID()` |
| `stack.stackSize` | `stack.getStackSize()` |
| `tile.worldObj` | `tile.getWorldObj()` |
| `tile.xCoord` | `tile.getXCoord()` |
| `tile.yCoord` | `tile.getYCoord()` |
| `tile.zCoord` | `tile.getZCoord()` |

**Skip** `this.fieldName` — FC classes inherit these fields from Abstract* classes.

## STEP 5: Convert static access to registries

| Static Access | Registry Call |
|---|---|
| `Block.blocksList[id]` | `BlockRegistry.get(id)` |
| `Block.stone` | `Blocks.stone` |
| `Block.dirt` | `Blocks.dirt` |
| (all 130 block constants) | `Blocks.xxx` |
| `Item.itemsList[id]` | `ItemRegistry.get(id)` |

Import: `btw.api.registry.*`

## STEP 6: Verify

After all changes:
1. `./gradlew buildAll` — should compile
2. `./gradlew :Client:runClient` — should launch
3. `grep -r 'import net.minecraft.src' Common/src Client/src Server/src | grep -v 'extends'` — should be empty

## Tips

- Do one module at a time (Common first, then Server, then Client)
- Build after each step to catch errors early
- IntelliJ's **Analyze → Inspect Code** can find remaining raw type references
- If a method doesn't have a _btw bridge, override it directly (no rename needed)
