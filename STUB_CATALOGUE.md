# Comprehensive Stub & No-Op Catalogue

Originally audited 2026-03-28. Re-audited 2026-06-05 (see below). Link-audited
2026-07-08 (see below). Every remaining stub, no-op, empty body, hardcoded
return, and TODO across the btw.modern and btw.forge layers.

**Remaining estimated stubs: ~400**
(down from ~1100+ originally — many btw.modern files fully implemented)

---

## 2026-07-08 Link audit (bytecode-level; fixes applied)

Triggered by a soulforge crash: `NoSuchMethodError: SlotCrafting.<init>(EntityPlayer,
InventoryCrafting, IInventory, III)` — the Api stub had drifted from the real 1.5.2
signature (`IInventory` 2nd param), so FC bytecode emitted a descriptor no runtime
class declares. `tools/LinkAudit.java` (run: `javac tools/LinkAudit.java -d /tmp &&
java -cp /tmp LinkAudit`) now checks EVERY method/field/ctor reference in the
effective runtime set (fc output + Modern-Common winners per the jar shadow rules +
Forge main) and reports unresolvable ones — each is a latent NoSuchMethodError /
NoClassDefFoundError. Re-run it after touching Api signatures, Modern-Common shims,
the remapFcCode include list, or the shadow/exclude lists.

### Build bugs fixed (dev classpath was running STALE bytecode)

- `remapFcCode` wrote version-stamped jars that accumulated in `Forge/build/remapped/`;
  `extractFcClasses` extracted ALL of them first-in-wins, so the OLDEST jar's classes
  won on the dev classpath. Fixed: `archiveVersion = ''` + `Sync` from exactly the
  current archive (also purges stale classes). Delete old `fc-remapped-*.jar` if any
  reappear.
- **Purge fallout pattern**: a stale class can be LOAD-BEARING — a real 1.5.2 class
  left in the fc dir by an old jar can mask a Modern-Common shim gap, and purging it
  flips the winner to the incomplete shim. First confirmed case: `btw.modern.ItemFood`
  lost the real eat-start trio (`getMaxItemUseDuration`/`getItemUseAction`/
  `onItemRightClick`) → right-clicking any FC food did nothing. Fixed 2026-07-08 by
  porting the verbatim 1.5.2+FCMOD trio into the shim (PlayerBridge.setItemInUse →
  startUsingItem was already bridged). If some other feature "suddenly stopped
  working" after 2026-07-08, suspect this same pattern: a shim missing real 1.5.2
  base-class logic that a stale class used to provide. LinkAudit does NOT catch these
  (the member resolves via the base class — it's a behavior gap, not a link gap).

### Crash-chain fixes applied 2026-07-08 (all adversarially verified reachable)

| Fix | Crash it prevented |
|---|---|
| Api `SlotCrafting` ctor 2nd param → `IInventory` | Opening soulforge |
| `EnchantmentProtection`/`EnchantmentThorns` remap includes | First burning zombie at dawn; first mob melee hit on a player |
| `DamageSource.causeArrowDamage`/`causeFireballDamage`/`causeThornsDamage` shims | FC skeleton arrow hit; blaze fireball hit |
| `widenFcHookDescriptors` build task (ASM): widens frozen `OnKickedByCow`/`OnFlungBySquidTentacle`/`OnHeadCrabbedBySquid`/`OnStruckByLightning` to `(Entity)` + rewrites narrow call sites; EntityAnimal gets injected checkcasts | Cow kick / squid attack connecting (frozen artifact predates the hook-widening in vanilla/) |
| `EnchantmentHelper.getSilkTouchModifier` real impl; `getSilkTouchEnchant` moved EntityLiving→EntityPlayer (shadowed-class rule) | Breaking an FC furnace with a pickaxe |
| `EntityAISit` winner flipped to frozen (jar exclude + dev delete) | Every wolf was an inert husk (ctor NoSuchMethodError swallowed) |
| `World.getPlayerEntityByName` (+ WorldBridge override) | Tamed-wolf owner lookup every AI tick |
| `NBTTagCompound(String)` ctor | World save while a squid head-crab is mounted |
| `EntityPotion.setPotionDamage`, `PotionHelper.calcPotionLiquidColor`/`func_82817_b`, `MathHelper.getRandomDoubleInRange` | Witch attack/drink bricked witches; wither AI bricked mid-fight |
| `EntityHanging` + 6 minecart-class + `MobSpawnerBaseLogic`/`WeightedRandomMinecart` + `CallableEffect*` includes | Canvas placement; dispensing a minecart from a Block Dispenser; masked crash reports |
| Flat→restructured relocate rules (`FCBetterThanWolves`, `FCUtilsItem`, `FCUtilsRandomPositionGenerator`, `FCBlockSilverfish`, `FCBlockMycelium`, `FCBlockGroundCover`) | Breeding-harness animal death; FC egg impact; pig-zombie wander AI, mutant-silverfish/mooshroom behavior (frozen artifact references pre-restructure flat FC names) |
| `FCEntityCow` stub: `GotMilk`/`SetGotMilk` via DataWatcher 26 | Milking a converted mooshroom |
| Api/Modern-Common `AttemptToPossessNearbyCreature` void→boolean | Wolf possession (descriptor (DZ)V existed nowhere) |
| `IEntitySelector.selectAnything`, `Container.calcRedstoneFromInventory`, `EntityPlayer.displayGUIHopperMinecart`/`func_96122_a`/`func_71066_bF`, type-only `EntityMinecartHopper` stub | Second-layer refs unmasked by the new includes |
| Proxy* catch hardening: tick/interact/die/save now catch `Throwable` | Any remaining bridge-gap Error degrades to a warn log instead of killing the server / corrupting saves |

### Remaining KNOWN-UNRESOLVED references (all verified dead-code or feature-gated)

- Flat `btw.modern.FCEntity*` shotgun-include copies (mech-power, urn, canvas, flat
  villager/lightning) reference flat `FCBlock*`/`FCUtils*`/`FCTileEntityPulley` names —
  dead graph, nothing loadable instantiates them.
- `Village.*` members + `VillageDoorInfo` + `ChunkCoordinates` homing — gated behind
  Modern-Common `VillageCollection.findNearestVillage` returning null.
- `WorldServer`/`ServerConfigurationManager`/`Direction` portal trio — dead (WorldBridge
  extends World, not WorldServer; FC `setInPortal` never fires).
- `TileEntityHopper.suckItemsIntoHopper`/`func_96114_a`, `NBTTagCompound.getTags` —
  gated behind hopper/spawner minecarts (no EntityType registered; dispensed carts are
  inert-but-safe).
- `Item.appleGold`/`record13`/`recordWait`, `EntityList.getClassFromID` — gated on FC
  zombie wiring / legacy-ID mapping for spawn eggs and music discs.
- Client/Server variant drift: 6 methods in 5 dual-compiled classes (FCEntityCreeper x2,
  FCEntityPig, FCEntityZombie, FCBlockLogSmouldering, FCBlockNetherrackFalling) — Client
  kept stale narrow FC-type params, Server is widened; Client output wins the merge, so
  those overrides silently never dispatch. Fix by widening the Client sources to match
  Server twins.
- `fc_zombie`/`fc_spider`/`fc_ocelot`/`fc_ghast`/`fc_cave_spider` never register —
  those FC classes were never restructured into `net.minecraft.src.btw.entity`.

---

## 2026-06-05 Re-audit (supersedes stale claims below)

Verified against current code. The headline change: **the btw.forge layer is NOT
"fully implemented"** as Section 4 claims — several FC behaviors were ported into
btw.modern but never wired through the bridge. Findings split into three tiers.

### Tier 1 — Forge-layer bridge gaps — FIXED 2026-06-05

| Gap | Was | Now |
|-----|-----|-----|
| `ItemStackMixin.btw$hurtEnemy` | `hitEntity(stack, null, null)` — NPE/wrong durability on FC weapons (e.g. `FCItemBattleaxe` dereferences attacker) | Wraps target+attacker via `LivingEntityBridge.wrapLiving`; guards ProxyItems (handled by `ProxyItem.hurtEnemy`) |
| `BlockMixin.btw$stepOn` / `btw$fallOn` | `onEntityWalking/onFallenUpon(..., null)` | Added `btw$wrapEntity` helper (mirrors `ProxyBlock.wrapEntity`); passes real entity |
| `btw.modern.ItemFood.onEaten` | stub returning stack; nutrition faked in `ProxyItem.finishUsingItem` (and missing entirely on the vanilla-item path) | `onEaten` calls `getFoodStats().addStats(this)`; `GetHungerRestored()` overridden to `getHealAmount()*3` (HighRes overrides to raw). Bridge workaround removed; `ItemStackMixin.btw$finishUsing` guards ProxyItems + syncs |
| `decreaseAirSupply` | no-op placeholder; vanilla respiration always used | FC logic added to `btw.modern.EntityPlayer.decreaseAirSupply` (quadratic respiration `1-1/(level²+1)` with soulforged helm, pulling real data via overridden `getRespirationEnchantLevel()`/`IsWearingSoulforgedHelm()`); the mixin is now a thin call-through. **Bridged, not ported** — corrected after initial mixin-side reimplementation |
| `btw.modern.ItemFood.GetHungerRestored` | base `Item` returned 0 | `healAmount*3` — verified against canonical FC (`patch.txt:22558`, `vanilla/server/ItemFood.java:200`), not inferred |

Both Modern-Common and Forge compile clean after these.

### Tier 1 — evaluated, NOT real gaps (left as-is on purpose)

These pass `null` world/coords but the FC methods ignore those params (or the
vanilla signature supplies no position), so forcing context would be churn:
`ProxyBlockEntity.createNewTileEntity(null)`, `tickRate(null)`,
`ProxyBlock.getExplosionResistance(null)`, `getSpeedFactor → GetMovementModifier(null,0,0,0)`,
and the matching `BlockMixin` hooks. (FC `getExplosionResistance(Entity)`/`tickRate(World)`/
`createNewTileEntity(World)` are constant-returning; `getSpeedFactor()` has no pos in 1.20.1.)

### Tier 2 — genuine btw.modern engine gaps (NOT override points), still open

- `Entity.isOffsetPositionInLiquid` → false; `Entity.pushOutOfBlocks` → false
- `EntityLiving.canEntityBeSeen` → true (`// TODO ray tracing`) — mobs see through walls
- `EntityLiving` enchant-level getters (efficiency/respiration/knockback/fireAspect/looting/unbreaking) → 0
- `PathFinder.createEntityPathTo` (both overloads) → null — mob navigation dead
- `EnchantmentHelper.setEnchantments` / `buildEnchantmentList` / `calcItemStackEnchantability`
- `Chunk` height/sky/tile-entity/heightmap methods hardcoded
- `BlockFluid.getFlowDirection` → -1 (water-wheel direction); `MapGenCaves.generate` no-op (likely intentional)

### Tier 3 — open investigations (tracked as tasks)

- **Damage/armor pipeline** — INVESTIGATED 2026-06-05 (task #2). **No damage mixin is needed**
  — the earlier "armor protection broken, add a hurt mixin" finding was wrong. BTW does NOT
  override the vanilla armor formula: `applyArmorCalculations`, `getTotalArmorValue`, and
  `damageArmor` are unpatched in FC source (only the blocking hook `OnBlockedDamage` and the
  already-bridged melee `GetMeleeDamageModifier` sit near the damage path). `FCItemArmor` extends
  vanilla `ItemArmor` with standard `EnumArmorMaterial`; its only FC addition is
  `m_iArmorWeight`/`GetWeightWhenWorn`. FC's real armor mechanic is **weight → exhaustion**
  (heavy armor drains hunger faster), and that IS bridged: `EntityPlayer.GetArmorExhaustionModifier`
  (reads `GetWornArmorWeight`, `+weight/44`) → `PlayerMixin:102`. Vanilla 1.20.1 applies armor
  protection natively, so it would just work — **except** the real gap below.

- **Armor equipment registration** — DONE 2026-06-06, runtime-confirmed. FC armor now registers
  as `ProxyArmorItem extends ArmorItem` with a per-item `FCArmorMaterial` (defense/durability read
  off the FC item), `armorType→ArmorItem.Type` mapping, faithful `btw.modern.ItemArmor`/`EnumArmorMaterial`
  value population, and worn-layer textures (standard tiers + extracted FC variants incl. soulforged
  plate). Vanilla protection + worn rendering work natively; the weight→exhaustion penalty and
  soulforged-helm Respiration bonus reactivate. (A shadowing regression — `getRespirationEnchantLevel`
  on the shadowed `EntityLiving` — was fixed by declaring it on the non-shadowed `EntityPlayer`; see
  the shadowed-class-method-calls note.) Original gap, now historical:

- ~~**Armor equipment registration (the actual gap)**~~: FC armor items were registered as plain
  `ProxyItem extends Item`, never as `ArmorItem`/`Equipable`. There was zero armor-equipment wiring
  in the Forge layer (no `Equipable`, `getEquipmentSlot`, `ArmorMaterial`, ARMOR attribute). So FC
  armor can't be equipped into 1.20.1 armor slots → no protection attribute applies AND
  `InventoryBridge` (which reads MC armor slots into FC `armorInventory`) finds them empty, so even
  the weight→exhaustion penalty never fires. Fix = a `ProxyArmorItem` (or make `ProxyItem`
  conditionally `Equipable`) carrying an `ArmorMaterial`/slot/defense derived from FC's
  `EnumArmorMaterial` + `armorType`. Then both vanilla protection and the already-bridged weight
  penalty come alive. Tracked as a task; needs runtime verification (equip, take damage, watch
  hunger) and a 1.5.2→1.20.1 armor-value mapping decision (see docs/audit/tiers.md).
- **Class shadowing** — INVESTIGATED 2026-06-05, see below.

### Class-shadowing investigation (2026-06-05) — task #3

Empirical method: compared `btw.modern.*` class names in Modern-Common's build output
vs the relocated FC output (`Forge/build/classes/java/fc`), then cross-referenced the
85 collisions against the two resolution mechanisms (dev: `removeModernCommonShadowedClasses`;
jar: the `from(Modern-Common) { exclude ... }` block).

**Block subsystems are NOT shadowed (hypothesis disproved — good).** FC's subsystem
classes (`FCBlockAxle`, `FCBlockGearBox`, `FCBlockMillStone`, `FCBlockSaw`, …) live in the
nested `net.minecraft.src.btw.block.*` package, which the relocate rule (Forge/build.gradle:577)
**excludes** from `→ btw.modern`. They keep unique FQNs, collide with nothing, and load with
their overrides intact (e.g. `FCBlockAxle.GetMechanicalPowerLevelProvidedToAxleAtFacing`,
line 127), and are registered via `FCBetterThanWolves`. The subsystem "stubs" the 2026-03-28
audit flagged in `btw.modern.Block` are base-class override-point defaults; the real logic is
in these FC subclasses. So mechanical power / mill / saw are present and load — runtime wiring
(tick scheduling) is the open question, not shadowing. (The historically-shadowed
`BlockCrops/Flower/Cloth` are no longer in the collision set — already resolved.)

**Where shadowing IS real — 21 collisions resolve to Modern-Common in both dev and jar:**
- Mostly **deliberate type-only deferral stubs**: `FCEntitySquid` (7-line stub vs **1429-line**
  real FC squid), `FCEntityCow/Creeper/Ghast` (6–14 lines; comment: "type-only stub … referenced
  by vanilla Entity/EntityLiving"). The real FC behavior is shadowed by design until those mobs
  are wired — not an accident.
- **Vanilla containers** (`ContainerChest/Furnace/Hopper/BrewingStand/Dispenser/Enchantment/
  Beacon/Repair`): 5-line Modern-Common stubs win over the BTW-patched vanilla versions. GUI
  logic depends on the bridge (FCContainerMenu/ContainerBridge) — confirm per-container whether
  the stub or the bridge supplies behavior.
- **Merchant/trading + some AI** (`ContainerMerchant`, `InventoryMerchant`, `SlotCrafting`,
  `SlotMerchantResult`, `EntityAIBase`): Modern-Common has substantial (80–99 line)
  reimplementations — present, not stubs.

**Dev/jar divergence — FIXED 2026-06-05 (task #4).** 6 classes were deleted in dev (FC wins)
but NOT excluded from the jar (Modern-Common stub shipped → **prod-only shadowing**):
`ChunkCache, EntityEgg, EntityLargeFireball, EntityWitherSkull, RandomPositionGenerator,
EntityLookHelper`. Added all 6 to the jar `exclude` block so FC wins in both. Verified by
building `:Forge:jar` and confirming each in-jar class size now matches the FC class, not the
Modern-Common stub (e.g. `ChunkCache` 6843 vs 2913; `EntityLookHelper` 2555 vs 858). The
`build.gradle:820` note claiming `EntityLookHelper` was an intentional jar keep was **stale** —
it reasoned about Modern-Common's `EntityLiving`, which never wins at runtime (FC's does, and it
calls `new EntityLookHelper(this)`; Modern-Common's look helper is all empty stubs). Comment
corrected.

**Reverse case — FIXED 2026-06-05 (task #5).** `ModelBox, TexturedQuad, PositionTextureVertex`
were excluded from the jar (FC wins, correct UVs) but not deleted in dev (Modern-Common stub won).
Added all three to `removeModernCommonShadowedClasses` so dev matches the jar. Safe because they
form a self-contained cluster (only reference each other's fields) and the only boundary,
`ModelRenderer` (single-sourced), uses the 10-arg `ModelBox` ctor present in both versions — the
jar already binds MC `ModelRenderer` to FC `ModelBox`, so dev now just matches the shipping
artifact. Verified the deletion task removes them and the FC versions are present in `sourceSets.fc`.

**Result: dev and jar resolution are now fully reconciled** — every one of the 85 `btw.modern`
collisions resolves to the same version in both environments (verified: the dev-only and jar-only
divergence sets over collisions are both empty). Residual: the FC vs stub UV difference for the
model cluster is a visual change best eyeballed once in a running dev client, but prod already used
the FC versions, so dev now matches prod. See [[project_devjar_reflection_gotcha]].

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

## 4. btw.forge.* — mostly implemented (see 2026-06-05 re-audit)

> **Correction (2026-06-05):** this section's "fully implemented" claim was wrong.
> The 28 methods below are done, but the bridge layer still had FC behaviors that
> were ported into btw.modern and never wired (null entity/world args, no-op
> placeholders). Those Tier-1 gaps were fixed 2026-06-05 — see the re-audit at the
> top of this file.

The 28 formerly-stubbed methods in the Forge bridge layer are implemented:

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
