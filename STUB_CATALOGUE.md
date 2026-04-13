# Comprehensive Stub & No-Op Catalogue

Audited 2026-03-28. Every remaining stub, no-op, empty body, hardcoded return,
and TODO across the btw.modern and btw.forge layers.

**Remaining estimated stubs: ~400**
(down from ~1100+ originally — btw.forge layer fully done, many btw.modern files fully implemented)

---

## Priority Classification

### P0 — CRITICAL (blocks gameplay, causes crashes/NPEs)
These stubs are called by FC code during normal gameplay and returning wrong values breaks things.

### P1 — HIGH (affects core gameplay mechanics)
FC code calls these for game logic; wrong defaults alter gameplay significantly.

### P2 — MEDIUM (affects secondary systems)
These power less-critical FC systems (turntable, kiln, mechanical power, etc.).

### P3 — LOW (cosmetic, rarely-hit, or intentionally no-op)
Rendering stubs, client-side effects, or methods that truly should be no-ops on the Forge backend.

---

## 1. btw.modern.Block (~82 remaining stubs)

File: `Modern-Common/src/main/java/btw/modern/Block.java`

### Implemented since last audit

The following former stubs are now **done** and no longer need work:

- **Collision/bounds (P0):** `addCollisionBoxesToList`, `getCollisionBoundingBoxFromPool`,
  `collisionRayTrace`, `setBlockBoundsBasedOnState`, `setBlockBoundsForItemRender`
- **Brightness (P0):** `getMixedBrightnessForBlock` (returns 0xF000F0)
- **Mobility (P1):** `getMobilityFlag` (reads material)
- **Falling (P1):** `CheckForFall` (spawns EntityFallingSand)
- **Facing (P1):** `GetFacing(IBlockAccess,...)` (delegates), `SetFacing(World,...)`
- **Tool queries (P1):** `AreAxesEffectiveOn`, `AreChiselsEffectiveOn`, `AreHoesEffectiveOn`,
  `ArePicksEffectiveOn`, `AreShovelsEffectiveOn` (all read backing fields)
- **Creative (P1):** `getSubBlocks` (adds ItemStack)
- **Furnace/Buoyancy (P1):** `GetFurnaceBurnTime`, `GetBuoyancy` (read backing fields)
- **All Builder Pattern methods (P1):** `SetShovelsEffectiveOn`, `SetPicksEffectiveOn`,
  `SetAxesEffectiveOn`, `SetHoesEffectiveOn`, `SetChiselsEffectiveOn`, `SetChiselsCanHarvest`,
  `SetFireProperties`, `SetCanBeCookedByKiln`, `SetItemIndexDroppedWhenCookedByKiln`,
  `SetItemDamageDroppedWhenCookedByKiln`, `SetBuoyancy`/`SetBuoyant`/`SetNonBuoyant`/`SetNeutralBuoyant`,
  `SetFurnaceBurnTime`, `SetHerbivoreItemFoodValue`, `SetChickenItemFoodValue`, `SetPigItemFoodValue`
  — all store state in backing fields
- **Turntable (P2, partial):** `CanRotateOnTurntable`, `CanTransmitRotationHorizontallyOnTurntable`,
  `CanTransmitRotationVerticallyOnTurntable`, `RotateOnTurntable`
- **Hard points (P2, partial):** `HasSmallCenterHardPointToFacing`, `HasCenterHardPointToFacing`,
  `HasLargeCenterHardPointToFacing`, `HasContactPointToFullFace`, `HasContactPointToSlabSideFace`,
  `HasContactPointToStairShapedFace`
- **Hopper (P2, partial):** `DoesBlockHopperEject`, `CanItemPassIfFilter`
- **Kiln/saw (P2, partial):** `DoesBlockDropAsItemOnSaw`, `GetCookTimeMultiplierInKiln`
- **Piston (P2, partial):** `CanBlockBePushedByPiston`, `AdjustMetadataForPistonMove`
- **Vegetation (P2, partial):** `GetCanGrassGrowUnderBlock`, `GetPlantGrowthOnMultiplier`
- **Grazing (P2):** `GetHerbivoreItemFoodValue`, `GetChickenItemFoodValue`, `GetPigItemFoodValue`
- **Fire (P2, partial):** `GetCanBeSetOnFireDirectlyByItem` (delegates)

### P0 — CRITICAL (14 remaining)

These are OVERRIDE POINTS — the base class default is intentionally empty/false/true.
FC subclasses override them. They only need real base implementations if vanilla
Block.java in 1.5.2 had logic in the base class (e.g., breakBlock cleans up tile entities).

| Signature | Current | Notes |
|-----------|---------|-------|
| `isProvidingWeakPower(IBlockAccess, int, int, int, int)` | `0` | Redstone output always 0 |
| `isProvidingStrongPower(IBlockAccess, int, int, int, int)` | `0` | Redstone output always 0 |
| `onNeighborBlockChange(World, int, int, int, int)` | Empty | Override point |
| `onBlockActivated(World, int, int, int, EntityPlayer, int, float, float, float)` | `false` | Override point |
| `breakBlock(World, int, int, int, int, int)` | Empty | May need tile entity cleanup in base |
| `updateTick(World, int, int, int, Random)` | Empty | Override point |
| `onBlockAdded(World, int, int, int)` | Empty | Override point |
| `canPlaceBlockAt(World, int, int, int)` | `true` | Override point |
| `canBlockStay(World, int, int, int)` | `true` | Override point |
| `onBlockPlaced(World, int, int, int, int, float, float, float, int)` | passthrough | Override point |
| `DropComponentItemsOnBadBreak(World, int, int, int, int, float)` | `false` | Override point |
| `CanConvertBlock(ItemStack, World, int, int, int)` | `false` | Override point |
| `ConvertBlock(ItemStack, World, int, int, int, int)` | `false` | Override point |
| `createNewTileEntity(World)` | `null` | Override point |

### P1 — HIGH (15 remaining)

| Signature | Current | Impact |
|-----------|---------|--------|
| `getRenderType()` | `0` | All blocks use default render type |
| `getComparatorInputOverride(World, int, int, int, int)` | `0` | Override point |
| `onBlockDestroyedByPlayer(World, int, int, int, int)` | Empty | Override point |
| `onBlockDestroyedByExplosion(World, int, int, int, Explosion)` | Empty | Override point |
| `onEntityWalking(World, int, int, int, Entity)` | Empty | Override point |
| `onEntityCollidedWithBlock(World, int, int, int, Entity)` | Empty | Override point |
| `onBlockClicked(World, int, int, int, EntityPlayer)` | Empty | Override point |
| `onBlockPlacedBy(World, int, int, int, EntityLiving, ItemStack)` | Empty | Override point |
| `velocityToAddToEntity(World, int, int, int, Entity, Vec3)` | Empty | Override point |
| `onStartFalling(EntityFallingSand)` | Empty | Override point |
| `GetFacing(int)` | `0` | Facing from metadata — override point |
| `GetEfficientToolLevel()` | `0` | Tool level always 0 |
| `GetHarvestToolLevel()` | `0` | Harvest level always 0 |
| `IsBlockClimbable(World, int, int, int)` | `false` | Override point |
| `OnFluidFlowIntoBlock(World, int, int, int, BlockFluid)` | Empty | Override point |

**NOTE:** Most of these P0/P1 methods are intentionally empty/default in the base class.
FC subclasses override them. The defaults only need changing if vanilla 1.5.2 Block.java
had real logic in the base class for that method.

### P2 — MEDIUM (~53 remaining)

| Subsystem | Remaining Stubs | Methods |
|-----------|----------------|---------|
| Turntable | 4 | `GetRotationsToCraftOnTurntable`, `CanRotateAroundBlockOnTurntableToFacing`, `OnRotatedAroundBlockOnTurntableToFacing`, `GetNewMetadataRotatedAroundBlockOnTurntableToFacing` |
| Block dispenser | 3 | `GetStackRetrievedByBlockDispenser`, `IsBlockDestroyedByBlockDispenser`, `OnRemovedByBlockDispenser` |
| Hopper/strata | 5 | `DoesBlockHopperInsert`, `HasStrata`, `GetMetadataConversionForStrataLevel`, `GetFilterableProperties`/`SetFilterableProperties`, `CanTransformItemIfFilter` |
| Hard points | 1 | `HasContactPointToStairNarrowVerticalFace` |
| Mortar/snow/resting | 8 | `OnMortarApplied`, `HasMortar`, `HasNeighborWithMortarInContact`, `IsStickyToSnow`, `HasStickySnowNeighborInContact`, `IsBlockRestingOnThatBelow`, `IsBlockAttachedToFacing`, `AttachToFacing` |
| Fire queries | 8 | `DoesInfiniteBurnToFacing`, `DoesExtinguishFireAbove`, `GetCanBeSetOnFireDirectly`, `SetOnFireDirectly`, `GetChanceOfFireSpreadingDirectlyTo`, `GetCanBlockLightItemOnFire`, `GetDoesFireDamageToEntities`, `GetCanBlockBeReplacedByFire` |
| Kiln/saw | 5 | `DoesBlockBreakSaw`, `OnBlockSawed`, `GetItemIDDroppedOnSaw`, `GetItemCountDroppedOnSaw`, `GetItemDamageDroppedOnSaw` |
| Mechanical power | 6 | `GetMechanicalPowerLevelProvidedToAxleAtFacing`, `CanOutputMechanicalPower`, `CanInputMechanicalPower`, `IsOutputtingMechanicalPower`, `IsInputtingMechanicalPower`, `Overpower` |
| Vegetation spread | 7 | `AttempToSpreadGrassToBlock`, `GetCanGrassSpreadToBlock`, `SpreadGrassToBlock`, `AttempToSpreadMyceliumToBlock`, `GetCanMyceliumSpreadToBlock`, `SpreadMyceliumToBlock`, `GetCanBlightSpreadToBlock` |
| Piston | 5 | `CanBlockBePulledByPiston`, `CanBePistonShoveled`, `GetPistonShovelEjectDirection`, `GetAsPistonMovingBoundingBox`, `CanContainPistonPackingToFacing` |
| Growth/hydration | 11 | `CanDomesticatedCropsGrowOnBlock`, `CanReedsGrowOnBlock`, `CanSaplingsGrowOnBlock`, `CanWildVegetationGrowOnBlock`, `CanNetherWartGrowOnBlock`, `CanCactusGrowOnBlock`, `IsBlockHydratedForPlantGrowthOn`, `IsConsideredNeighbouringWaterForReedGrowthOn`, `GetIsFertilizedForPlantGrowth`, `NotifyOfFullStagePlantGrowthOn`, `NotifyOfPlantAboveRemoved` |
| Block property | 7 | `CanWeedsGrowInBlock`, `GetWeedsGrowthLevel`, `RemoveWeeds`, `AttemptToApplyFertilizerTo`, `GetConvertsLegacySoil`, `CanGroundCoverRestOnBlock`, `CanSpitWebReplaceBlock` |

### P3 — LOW (Rendering/Client/Intentional No-ops — unchanged)

| Signature | Current | Notes |
|-----------|---------|-------|
| RenderBlock*, RenderFalling, RenderCooking, etc. (8 methods) | `false`/empty | Client rendering — vertex capture handles this |
| `randomDisplayTick(...)` | Empty | Client particles — separate system |
| Client notification methods (3 methods) | Empty | Client-side — different path in 1.20.1 |
| `dropXpOnBlockBreak(...)` | Empty | FC removes XP from most blocks |
| Falling block render methods (2 methods) | `true` | Reasonable defaults |

---

## 2. btw.modern.RenderBlocks (~53 remaining stubs)

File: `Modern-Common/src/main/java/btw/modern/RenderBlocks.java`

### Implemented since last audit

- `renderBlockByRenderType` — full switch statement with real logic
- `renderBlockTorch` — full vertex rendering
- `renderBlockLadder` — full switch-based implementation
- `renderCrossedSquares` — full 4-plane tessellator implementation
- `renderBlockCrops` — full # pattern implementation
- `renderBlockAsItem` — full face rendering implementation
- 6 face methods (renderFaceYNeg/YPos/ZNeg/ZPos/XNeg/XPos) — all fully implemented

### P3 — LOW (all remaining — vertex capture pipeline replaces most of these)

| Category | Count | Methods |
|----------|-------|---------|
| Block rendering | 2 | `renderBlockUsingTexture`, `renderBlockAllFaces` |
| AO/color/grass | 6 | `renderStandardBlockWithAmbientOcclusion`, `func_102027_b`, `renderStandardBlockWithColorMultiplier`, `renderGrassBlockWithAmbientOcclusion`, `renderGrassBlockWithColorMultiplier`, `RenderStandardFullBlockWithAmbientOcclusion`, `RenderStandardFullBlockWithColorMultiplier` |
| Specific block renders | 28 | `renderBlockLog`, `renderBlockQuartz`, `renderBlockCactus`/Impl, `renderBlockFence`, `renderBlockWall`, `renderBlockFenceGate`, `renderBlockStairs`, `renderBlockDoor`, `renderBlockFluids`, `renderBlockRepeater`, `renderBlockRedstoneWire`, `renderBlockMinecartTrack`, `renderBlockVine`, `renderBlockPane`, `renderBlockStem`, `renderBlockLilyPad`, `renderBlockFire`, `renderBlockLever`, `renderBlockTripWireSource`/Wire, `renderBlockDragonEgg`, `renderBlockCauldron`, `renderBlockAnvilMetadata`, `renderPistonBase`/Extension, `RenderBlockRedstoneLogic`, `RenderBlockBeacon`, `RenderBlockBed`, `RenderBlockBrewingStand`, `RenderBlockCocoa`, `RenderBlockAnvil`, `RenderBlockEndPortalFrame` |
| Helpers | 5 | `renderTorchAtAngle`, `drawCrossedSquares`, `renderBlockStemSmall`, `renderBlockStemBig`, `renderBlockCropsImpl` |
| Falling/piston | 5 | `renderBlockSandFalling`, `RenderStandardFallingBlock`, `RenderStandardFullBlockMovedByPiston`, `renderPistonBaseAllFaces`, `renderPistonExtensionAllFaces` |
| Other | 3 | `RenderBlockFlowerpot`, `RenderBlockHopper`, `renderItemIn3d` |

**NOTE:** Only needed if FC blocks override `RenderBlockAsItem()` to call specific render methods.
Most FC blocks use the base 6-face cube which already works via the face rendering methods.

---

## 3. btw.modern.* Other Files

File: `Modern-Common/src/main/java/btw/modern/*.java` (excluding Block.java, RenderBlocks.java)

### Fully Implemented (removed from catalogue)

These files have **no remaining stubs** (or only intentional abstract methods):

- **AxisAlignedBB.java** — all collision/ray trace/rotation methods done
- **BiomeGenBase.java** — all biome property methods done
- **BlockFence.java** — all fence connection methods done
- **BlockRailBase.java** — all rail connection methods done
- **BlockStairs.java** — all stair geometry methods done
- **ChunkCache.java** — all chunk caching methods done
- **CreativeTabs.java** — all creative tab methods done
- **CraftingManager.java** — addRecipe, addShapelessRecipe, findMatchingRecipe all done
- **EntityCreature.java** — getEntityToAttack, hasPath, etc. all done
- **EntityItem.java** — onUpdate, combineItems, NBT all done
- **EnumCreatureType.java** — all getters done
- **Explosion.java** — doExplosionA/B fully implemented
- **FoodStats.java** — full 0-60 food system implemented
- **GameRules.java** — all get/set methods done
- **ItemStack.java** — all core methods done
- **Material.java** — all property methods done
- **MathHelper.java** — all math utilities done
- **NBTTagCompound.java** — all 23+ getter/setter methods done
- **StatCollector.java** — all translation methods done
- **Tessellator.java** — full vertex capture system
- **TileEntity.java** — readFromNBT, writeToNBT, updateEntity all done
- **Vec3.java** — all vector math done
- **WorldProvider.java** — getDimensionName, celestialAngle, etc. all done

### Files that do not exist (removed from catalogue)

- **AchievementList.java** — file does not exist (achievements removed in modern MC)
- **FCAddOn.java** — file does not exist
- **FCCraftingManagerBulkRecipe.java** — file does not exist
- **RenderGlobal.java** — file does not exist
- **ScaledResolution.java** — file does not exist

### P0 — CRITICAL (remaining)

| File | Remaining Stubs | Key Methods Still Stubbed |
|------|----------------|--------------------------|
| World.java | ~7 abstract | Core methods (`getBlockId`, `getBlockMetadata`, `setBlock`, `getEntitiesWithinAABB`, `scheduleBlockUpdate`, `notifyBlockChange`, `markBlockRangeForRenderUpdate`) are **abstract** — implemented by WorldBridge, not stubs per se |
| EntityPlayer.java | ~25-30 | `displayGUIChest`, `addExperience`, `triggerAchievement`, `addStat`, `displayGUIWorkbench`, `displayGUIEditSign`, `dropPlayerItem`, `wakeUpPlayer`, and various placeholder methods. ~55-60 methods ARE implemented (penalty system, food, mining speed, movement gating, armor weight) |
| Item.java | ~30-35 | `onItemUse`, `onItemRightClick`, `onEaten`, `onBlockDestroyed`, `hitEntity`, `canHarvestBlock`, `useItemOnEntity`, plus ~25 BTW-specific stubs. Builder/query methods ARE implemented |
| EntityLiving.java | ~95+ | `onLivingUpdate`, `jump`, `swingItem`, `updateEntityActionState`, `fall`, plus ~90 BTW-specific stubs (possession, grazing, hunger state). Only `attackEntityFrom`, `onDeath`, `heal`, `getHealth`, potion methods, `getTotalArmorValue` are done |

### P1 — HIGH (remaining)

| File | Remaining Stubs | Status |
|------|----------------|--------|
| Container.java | 2-3 | `transferStackInSlot` (null), `onContainerClosed` (empty). `detectAndSendChanges`, `slotClick`, `mergeItemStack` ARE done |
| Entity.java | ~15-20 | `isOffsetPositionInLiquid`, `handleLavaMovement`, `handleWaterMovement`, `isInsideOfMaterial`, `pushOutOfBlocks`, `canBePushed`, etc. Position/motion/fire/distance methods ARE done |
| Village.java | 1 | `addOrRenewAgressor` (empty). All getters ARE done |

### P2 — MEDIUM (remaining)

| File | Remaining Stubs | Key Methods |
|------|----------------|-------------|
| BlockFluid.java | 1 | `getFlowDirection` (returns -1) |
| Chunk.java | ~9 | `getBlockLightValue`, `getHeightValue`, `canBlockSeeTheSky`, `addEntity`, `removeEntity`, tile entity methods, `onChunkLoad`/`Unload`, `generateHeightMap`/`SkylightMap`. Block ID/metadata access IS done |
| Enchantment.java | 2 | `calcModifierDamage` (0), `calcModifierLiving` (0). All other methods done |
| EnchantmentHelper.java | ~5 | `setEnchantments`, `getMaxEnchantmentLevel`, `getEnchantmentModifierDamage`/`Living`, `addRandomEnchantment`, `buildEnchantmentList`. Individual enchant getters ARE done |
| MapGenCaves.java | 1 | `generate` (empty) |
| PathFinder.java | 5 | `createEntityPathTo` (2 overloads, null), `CanPathThroughClosedWoodDoor`, `CanPathThroughOpenWoodDoor`, `CanPathThroughWater` (all false) |
| Potion.java | 4 | `performEffect` (empty), `affectEntity` (empty), `isInstant` (false), `isReady` (false). Init and all getters ARE done |
| SpawnerAnimals.java | 2 | `findChunksForSpawning` (0), `performWorldGenSpawning` (empty). Spawn location checks ARE done |

### P3 — LOW (remaining)

| File | Remaining Stubs | Notes |
|------|----------------|-------|
| FontRenderer.java | 11 | All stubs: `drawStringWithShadow`, `drawString`, `getStringWidth`, `getCharWidth`, `trimStringToWidth`, `drawSplitString`, etc. Client-side only |
| GuiScreen.java | 12+ | All stubs: `drawScreen`, `keyTyped`, `mouseClicked`, etc. Client-side only |
| ModelRenderer.java | 1 | Mostly implemented (full GL rendering). Minor stubs only |
| Profiler.java | 3 | `startSection`, `endSection`, `endStartSection` — intentional no-ops |
| Render.java | 3 | `loadDownloadableImageTexture`, `doRenderShadowAndFire`, `renderOffsetAABB`/`renderAABB`. Core rendering IS done |
| RenderEngine.java | 6 | `resetBoundTexture`, `getTextureForDownloadableImage`, `getTextureContents`, `allocateAndSetupTexture`, `deleteTexture`, `refreshTextures`/`updateDynamicTextures`, `getMissingIcon`. Only `bindTexture` is done |
| RenderManager.java | 4 | `getEntityClassRenderObject`, `getEntityRenderObject`, `renderEntity`/`WithPosYaw`, `getFontRenderer`, `updateIcons`. Constructor/distance methods ARE done |
| TextureStitched.java | 2 | `updateAnimation` (empty), `IsProcedurallyAnimated` (false). All getters done |

---

## 4. btw.forge.* — FULLY IMPLEMENTED

All 28 formerly-stubbed methods in the Forge bridge layer are now implemented:

- **WorldBridge.getBlockMetadata** — full BlockState property extraction with vanilla block fallbacks
- **ForgeMinecraftServerWrapper.getConfigurationManager** — returns ForgeConfigurationManager
- **PlayerBridge.dropPlayerItem** — wraps MC drop(), returns FC EntityItem
- **PlayerBridge.displayGUIChest** — delegates to ContainerBridge.openChestGUI
- **ProxyBlock.attack/entityInside/stepOn** — all wrap entities via PlayerBridge/wrapEntity
- **ProxyItem.hitEntity/mineBlock/interactLivingEntity** — all wrap via LivingEntityBridge
- **ItemStackHelper.toMcStack/toFcStack** — full NBT + enchantment copy
- **DamageSourceMapping.fcGloom** — mapped to sources.generic()
- **EntityBridge.playSound** — delegates to SoundMapping.playAtEntity
- **ProxyAnimal/Mob/PathfinderMob NBT** — read/addAdditionalSaveData fully implemented
- **BTWNetwork** — PenaltySync packet registered and handled

---

## 5. Implementation Strategy (Updated)

### Tier 1 — DONE

These are now complete and no longer need work:

1. ~~Block builder methods with backing fields~~ — All Set*/Get*/Are* methods store and read state
2. ~~Block collision/bounds~~ — Full AABB math implemented
3. ~~WorldBridge metadata~~ — Full BlockState property extraction
4. ~~Entity parameter passing~~ — All ProxyBlock/ProxyItem methods wrap entities properly
5. ~~Entity proxy NBT~~ — read/addAdditionalSaveData implemented for all proxy types
6. ~~Sound mapping~~ — EntityBridge.playSound delegates to SoundMapping
7. ~~Container bridge~~ — displayGUIChest delegates to ContainerBridge
8. ~~ItemStack NBT~~ — Full enchantment/NBT copy between FC and MC stacks

### Tier 2 — Current Priority (enables FC subsystems)

1. **EntityLiving.java** (~95 stubs) — Largest remaining gap. Needed for mob AI, movement,
   combat, and BTW-specific creature behavior (grazing, possession, etc.)
2. **Item.java** (~30 stubs) — Item interaction callbacks (onItemUse, onItemRightClick, etc.)
   are override points but some base implementations may need logic
3. **Entity.java** (~15-20 stubs) — Water/lava detection, collision, push mechanics
4. **Block.java P2 subsystems** (~53 stubs) — Mechanical power, fire queries, piston,
   vegetation spread, mortar/snow, kiln/saw
5. **Facing/rotation** — `GetFacing(int)` still returns 0 (override point but may need base logic)
6. **Chunk.java** (~9 stubs) — Light, height, entity management, tile entities

### Tier 3 — Lower Priority (polish)

1. **RenderBlocks specific renders** (~53 stubs) — Only needed for FC blocks with custom
   rendering. Torch, ladder, crossed squares, crops already done.
2. **EntityPlayer.java** (~25-30 stubs) — GUI display methods, experience, achievements
3. **PathFinder.java** (5 stubs) — AI pathfinding
4. **Client-only files** — FontRenderer, GuiScreen, RenderEngine, RenderManager (all P3)
5. **EnchantmentHelper.java** (~5 stubs) — Modifier calculations

### Key Principle

**DO NOT manually reimplement FC logic.** Most remaining stubs in Block.java are OVERRIDE POINTS.
The base class returns a sensible default; FC subclasses override with their own logic.
The defaults are correct unless vanilla 1.5.2 Block.java had real logic in the base method.

The biggest implementation gaps are now in **EntityLiving.java** and **Item.java** — these
are the files most likely to cause gameplay issues with missing method bodies.
