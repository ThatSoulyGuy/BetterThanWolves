# Alchemy & reagents audit

Pinned FC reagent per Pattern-B gate. Every "free-win" modern MC feature
that we want to preserve (Protocol #2) gets an FC ingredient attached as
the cost. The reagent must already exist in FC's vocabulary — no new
items are invented here (Protocol #2 again — gate, don't replace).

All FC item IDs are the numeric IDs FC registers them at. Registration
site: `Client/src/main/java/net/minecraft/src/btw/core/FCBetterThanWolves.java`.

---

## Reagent vocabulary (FC inventory of gate-keepers)

Early-mid availability (stoked-cauldron / basic mill-stone chain):

| Item | FC ID | Reg line | Notes |
|---|---|---|---|
| `fcItemHellfireDust` | 257 | `FCBetterThanWolves.java:1364` | Milled from hellfire; 8 dust → 1 concentrated |
| `fcItemPotash` | 274 | `FCBetterThanWolves.java:1415` | Stoked-cauldron bleaching agent; heavily reused across FC |
| `fcItemBrimstone` | 22236 | `FCBetterThanWolves.java:1489` | FC sulfur-equivalent; gunpowder component |
| `fcItemNitre` | 22237 | `FCBetterThanWolves.java:1493` | FC saltpeter; gunpowder component |
| `fcItemCoalDust` | 267 | `FCBetterThanWolves.java:1390` | Milled coal |

Mid availability (recipe requires 8× accumulation or crucible):

| Item | FC ID | Reg line | Notes |
|---|---|---|---|
| `fcItemConcentratedHellfire` | 258 | `FCBetterThanWolves.java:1369` | Cauldron: 1 potash + 8 hellfire dust → 1 concentrated (`FCRecipes.java:3338-3339`) |
| `fcItemBlastingOil` | 22240 | `FCBetterThanWolves.java:1507` | Cauldron: hellfire dust + tallow × 2 (`FCRecipes.java:3329-3331`) |
| `fcItemSoulDust` | 283 | `FCBetterThanWolves.java:1437` | Milled soul sand output; incinerates in crucible |
| `fcItemNetherSludge` | 286 | `FCBetterThanWolves.java:1446` | Stoked cauldron output from potash chain |

Late availability (scarce, endgame):

| Item | FC ID | Reg line | Notes |
|---|---|---|---|
| `fcItemSoulFlux` | 2299 | `FCBetterThanWolves.java:1652` | Rare soul-imbued item, buoyant, has `hasEffect = true` (shines) — marker of endgame |
| `fcItemSteel` / `fcBlockSoulforgeDormant` | various | — | 60-gold-nugget crucible output |
| `fcItemArcaneScroll` | 22223 | `FCBetterThanWolves.java:1451` | Knowledge item, custom class |
| `fcItemSoulUrn` | 272 | `FCBetterThanWolves.java:1411` | Cauldron bleaching output, custom class |

**FC custom Potion IDs** (`FCBetterThanWolves.java:770-772`):
`potionFortune = 31`, `potionLooting = 30`, `potionTrueSight = 29`.
Free IDs: `26`, `27`, `28`, `32+`. (Vanilla uses 0–24 with gaps; check
`Potion.potionTypes` array before picking new IDs.)

---

## Pattern-B reagent gates

### Elytra (#051) — **blasting oil per flight-second**

| Field | Value |
|---|---|
| Reagent | `fcItemBlastingOil` (ID 22240) |
| Cost | 1 bottle per 20 ticks (1 second) of sustained flight |
| Storage | Consumed directly from player inventory each second; flight ends mid-air when none remain (vanilla glide-drop) |
| Rationale | Elytra = propelled flight → needs combustible propellant. Blasting oil is FC's liquid explosive, thematic fit; already a mid-game cauldron output so players have to *want* to fly enough to commit cauldron cycles to it |
| Implementation hook | `ElytraItemMixin` on `net.minecraft.world.item.ElytraItem#elytraFlightTick(ItemStack, LivingEntity, int)` — check inventory for blasting oil on each flight tick interval; consume + continue, or return `false` + revoke `FALL_FLYING` pose |

**Alt rejected:** burn durability on the elytra itself (no reagent). That
just makes elytra a consumable, not an FC-gated feature — players would
never craft one. Keeping elytra durable but fuel-gated preserves the
decision to "go flying today."

### Totem of Undying (#026) — **soul flux consumed on activation**

| Field | Value |
|---|---|
| Reagent | `fcItemSoulFlux` (ID 2299) |
| Cost | 1 soul flux consumed on save (in addition to the totem itself) |
| Storage | Anywhere in inventory |
| Rationale | Soul flux is FC's rarest alchemy output — requiring it makes totem-saves occasional rather than routine, preserving the weight of HC Spawn (`#002`). Also thematically clean: "soul" flux saves your soul |
| Implementation hook | `TotemOfUndyingMixin` on `LivingEntity#checkTotemDeathProtection` — detailed in `docs/audit/combat.md` §6 |

### Soul Speed (#012) — **soul dust in crafting recipe**

| Field | Value |
|---|---|
| Reagent | `fcItemSoulDust` (ID 283) |
| Cost | Soul Speed book requires 1 soul dust to craft (overrides the rare-loot-only distribution) |
| Storage | Recipe ingredient; one-shot |
| Rationale | Soul Speed boosts walk speed on soul-blocks — already a soul-themed mechanic; soul dust is the natural FC-side offering. Also routes the enchant through an FC workstation instead of loot RNG |
| Implementation | Recipe override (Phase 2 data). Add `data/minecraft/recipes/soul_speed_book.json` overriding vanilla loot-only distribution — in practice, add an FC-authored recipe since vanilla has none, and mark piglin-barter tables to drop less frequently (|#041 piglin barter work, Phase 6) |

### Respawn Anchor (#014) — **concentrated hellfire per charge**

| Field | Value |
|---|---|
| Reagent | `fcItemConcentratedHellfire` (ID 258) |
| Cost | 1 concentrated hellfire per charge (replacing 1 glowstone block) |
| Storage | Anchor charge count limited to 1 regardless |
| Rationale | Glowstone is too cheap for what amounts to an alternative HC Spawn. Concentrated hellfire is a real cauldron commitment (8 hellfire dust + 1 potash each). At 1-charge cap + expensive reagent, respawn anchor becomes a "burn an escape route" choice |
| Implementation hook | `RespawnAnchorBlockMixin` on `RespawnAnchorBlock#canBeCharged` (check for concentrated hellfire in hand) + `RespawnAnchorBlock#charge` (consume it). Charge-cap to 1 is `#014` (mixin sets `maximumCharges = 1` via `@ModifyConstant`) |

### Lodestone (#015) — **soulforged steel ingot in recipe**

| Field | Value |
|---|---|
| Reagent | `fcItemSteel` / soulforged steel ingot |
| Cost | Recipe requires 1 soulforged steel in the center slot (replacing vanilla netherite) |
| Rationale | FC has no netherite. Soulforged steel is FC's top-tier material and the correct "endgame" gate for tracking-compass utility |
| Implementation | Pure recipe override (Phase 2 data). `data/minecraft/recipes/lodestone.json` replaces netherite ingot with FC soulforged steel tag |

### Recovery Compass (#062) — **arcane scroll + echo shard**

| Field | Value |
|---|---|
| Reagent | `fcItemArcaneScroll` (ID 22223) + 1 echo shard (keep vanilla) |
| Cost | Recipe: 8 echo shard + 1 arcane scroll in 3×3 grid |
| Rationale | Recovery compass trivializes corpse retrieval after FC death penalty. Arcane scroll is FC's knowledge item — fits "scrying" theme of the compass. Keeps echo shard relevant so ancient-city dive still matters |
| Implementation | Phase 2 recipe override |

### Conduit (#060) — **prismarine + soul flux, crucible-only**

| Field | Value |
|---|---|
| Reagent | `fcItemSoulFlux` (1) + prismarine shards (8) |
| Cost | Crucible stoked cycle; heart-of-the-sea is replaced by soul flux |
| Rationale | Heart of the sea + nautilus shells is FC-illegible — the items don't participate in FC progression. Moving the craft to crucible integrates conduit into FC's workstation chain; soul flux makes it an endgame commitment |
| Implementation | Crucible recipe added to `FCCraftingManagerCrucible`. Vanilla crafting recipe for conduit blocked via Phase 2 data override (negative recipe) |

### Beacon (#060 co-entry) — **FoodStats hunger drain**

| Field | Value |
|---|---|
| Reagent | Drains `FoodStats.foodLevel` per tick while inside aura |
| Cost | Active beacon buff drains 1 food point every 40 ticks (2s) from each affected player |
| Rationale | Beacon aura is "free buffs forever" — breaks FC's hunger-is-currency model. Hunger drain folds the buff into FC's food economy (Protocol #2) without destroying beacon gameplay |
| Implementation hook | `BeaconBlockEntityMixin` on `applyEffects(Level, BlockPos, int, MobEffect, MobEffect)` — after vanilla applies MobEffect, iterate affected players and `player.causeFoodExhaustion(0.025F)` per tick (scales with beacon level) |

### Honey bottle (#035) — **stoked-cauldron alchemy input**

| Field | Value |
|---|---|
| Reagent | Honey bottle consumed as cauldron input |
| Cost | Cauldron recipe: 1 honey bottle + 1 blaze powder → 1 new potion (e.g., "Potion of Regenerative Sting") |
| Rationale | Honey bottle restores 6 hunger instantly — too good as raw food. Routing it through alchemy keeps the bees-for-honey loop alive but makes honey a resource, not a snack |
| Implementation | New stoked-cauldron recipe in `FCCraftingManagerCauldron` alongside the existing blasting-oil/concentrated-hellfire entries. New potion uses next free ID (26, 27, or 28) |

---

## Non-reagent gates (listed for completeness)

These use the alchemy chain indirectly:

| Feature | Mechanism |
|---|---|
| Grindstone disenchant (#034) | Consumes `fcItemHellfireDust` per disenchant (mixin on `GrindstoneMenu`) |
| Smithing template use (#053) | Requires `fcItemPotash` + `fcItemConcentratedHellfire` added to template recipe (see `docs/audit/tiers.md`) |
| Goat horn craft (#044) | Requires `fcItemArcaneScroll` in recipe |

---

## Recipe-file anchors for Phase 2

When Phase 2 data-gen lands, the following recipes need explicit entries:

| Reagent gate | New/overridden recipe file |
|---|---|
| Elytra fuel | *(no recipe; mixin-driven at flight tick)* |
| Totem | *(no recipe; mixin on checkTotemDeathProtection)* |
| Soul Speed book | `data/minecraft/recipes/soul_speed_book.json` |
| Respawn anchor charge | *(mixin; no recipe)* |
| Lodestone | `data/minecraft/recipes/lodestone.json` override |
| Recovery compass | `data/minecraft/recipes/recovery_compass.json` override |
| Conduit | `data/minecraft/recipes/conduit.json` → deletion + add FC crucible entry |
| Honey bottle alchemy | FC-internal: `FCCraftingManagerCauldron` add stoked recipe |
| Grindstone disenchant | *(mixin; no recipe)* |

---

## Ordering note

Phase 5 (`#035` honey alchemy) can proceed in parallel with Phase 3 and
Phase 4 since it only adds to `FCCraftingManagerCauldron`. All other
reagent gates depend on either Phase 2 data-gen being live (for
JSON-based overrides) or Phase 4 off-hand routing (for totem activation
detection through off-hand if that becomes the trigger instead of main
inventory check).
