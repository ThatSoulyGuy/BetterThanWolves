# Tiers & materials audit

Pinned slot for each post-1.5.2 MC material in FC's progression. Every
modern material either (a) slots into an existing FC tier, (b) gets a
new utility-only tier that doesn't compete with tools, or (c) is
re-tiered under Soulforged Steel. **No modern material is allowed above
Soulforged Steel** — FC's ladder is hard-capped and the port is
committed to not shifting that ceiling (Protocol #5).

Source of FC tiers: `vanilla/server/net/minecraft/src/EnumToolMaterial.java:29-34`
and `vanilla/server/net/minecraft/src/EnumArmorMaterial.java:24-29`.

---

## FC tier ladder (reference)

```
EnumToolMaterial (tools):
  WOOD              (0, 10,   1.01F, 0, 0)
  STONE             (1, 50,   1.01F, 1, 5)
  IRON              (2, 500,  6F,    2, 14)
  EMERALD (diamond) (3, 1561, 8F,    3, 14)
  GOLD              (0, 32,   12F,   0, 22)
  SOULFORGED_STEEL  (4, 2250, 12F,   4, 0)   ← hard cap
                                             ^ no vanilla enchant
```

Fields: `(harvestLevel, maxUses, efficiency, damageVsEntity, enchantability)`

```
EnumArmorMaterial (armor):
  CLOTH    (10, [1,3,2,1], 15)
  CHAIN    (20, [2,5,4,1], 12)
  IRON     (20, [2,6,5,2], 10)
  GOLD     (7,  [2,5,3,1], 25)
  DIAMOND  (33, [3,8,6,3], 10)   ← hard cap for armor
```

**Note:** FC armor has no Soulforged tier. Diamond is the armor ceiling.
This matters for netherite integration (below).

---

## Material slotting

### Netherite (1.16) → slot at EMERALD / DIAMOND

| Property | FC decision | Rationale |
|---|---|---|
| `Tiers.NETHERITE` harvest level | 3 (same as EMERALD) | FC caps at 4 = Soulforged. Netherite at 3 puts it level with diamond — where the modding community already treats it |
| `Tiers.NETHERITE` max uses | 1561 (same as EMERALD) | No durability advantage over diamond |
| `Tiers.NETHERITE` efficiency | 8F (same as EMERALD) | No mining-speed advantage |
| `Tiers.NETHERITE` damage | 3 (same as EMERALD) | No damage advantage |
| `Tiers.NETHERITE` enchantability | 14 (same as EMERALD) | No enchant advantage |
| Lava immunity (item-on-ground) | **KEEP** | Thematic preservation — netherite items floating in lava is netherite's only real identity at this tier |
| Fire resistance on wearer | **KEEP** | Netherite armor's wearer-fire-resist is modest and doesn't break FC's fire-danger loop |
| Knockback resistance | **KEEP** | Numerically fine; FC combat doesn't collapse if a netherite-armored player eats slightly less knockback |

**Net effect:** netherite becomes a *sidegrade* to diamond with an
anti-loss safety feature, not an upgrade. Players who currently pursue
netherite for strict tool/weapon gain will find the pursuit neutral;
players who want the lava-immunity safety will still find it worthwhile.

**Implementation target:**
- Access transformer entry on `net.minecraft.world.item.Tiers.NETHERITE`
  to make fields mutable, OR
- Mixin `Forge/src/main/java/btw/forge/mixin/NetheriteTierMixin.java`
  targeting the enum constructor (more invasive; use AT if possible).
- `#052` is the integration entry.

### Smithing templates (1.20) → FC-reagent cost per upgrade

| Template | Current recipe | New recipe |
|---|---|---|
| Netherite upgrade | 1 diamond item + 1 netherite ingot + 1 template | 1 diamond item + 1 netherite ingot + 1 template + **`fcItemPotash` + `fcItemConcentratedHellfire`** |
| Armor trim (any) | trim template + 1 ingot (varies) + 1 armor piece | unchanged (trims are cosmetic per `#003` E-batch) |

**Why:** netherite re-tier above removes the power gradient, but smithing
templates themselves still function as a one-use gate — requiring two
FC reagents makes the upgrade an FC-workstation commitment. Potash is
mid-game / ubiquitous; concentrated hellfire is mid-late. Combined, the
cost is "you have been playing FC."

**Implementation target:**
- Phase 2 recipe override. Vanilla smithing recipe file:
  `data/minecraft/recipes/netherite_smithing_upgrade.json` (or equivalent
  paths per tool type).
- Recipe type `minecraft:smithing_transform` supports additional
  ingredients in 1.20 — confirm via recipe-JSON schema during
  implementation.
- `#053` is the integration entry.

### Copper (1.17) → NEW utility tier between STONE and IRON; no tools

| Property | Decision | Rationale |
|---|---|---|
| Tool recipes | **NONE** — no copper pick, axe, sword, shovel | FC's mining progression is tightly tuned; a half-tier tool breaks the stone→iron gating |
| Utility role | Piping, wiring, lightning attractor, decorative oxidation | All existing vanilla copper blocks kept; thematic "industrial" vibe fits FC's gears-and-rope aesthetic |
| Lightning rod (#047 covered separately) | Reduced range, see `#047` | Addressed in `INTEGRATIONS.md` deferred |
| Oxidation | **KEEP** | Purely cosmetic, already Pattern-E-acceptable |

**Net effect:** copper exists as a Forge mod registered in the world,
but doesn't create a parallel tool progression. FC code doesn't need to
know copper exists because no FC block accepts copper as a harvest level.

**Implementation target:**
- Phase 2 recipe removal: block vanilla recipes that produce copper
  tools (if any exist in 1.20.1 — they don't by default, this is
  defensive against a future patch).
- No FC-code change.

### Bamboo wood family (1.20) → OAK tier-align

| Item | Slot |
|---|---|
| Bamboo planks | Equivalent to oak planks |
| Bamboo slabs / stairs / fence / etc. | Equivalent to oak variants |
| Bamboo chest raft | Equivalent to oak boat with chest |

No FC code change; all slotting is vanilla-parity. Phase 1 (`#003`)
already approved armor trims as cosmetic; bamboo is analogous.

**Exception:** FC tool recipes that currently accept only oak planks
need a tag-based recipe (Phase 2, `#006` tag remediation) to also
accept bamboo planks. Otherwise players harvesting a bamboo-only jungle
can't progress.

### Cherry wood family (1.20) → OAK tier-align

Identical treatment to bamboo. Same `#006` tag remediation applies.

### Mangrove wood family (1.19) → OAK tier-align

Identical. `#006` tag remediation. Mud and propagules are additive and
E-pattern (no tool/tier conflict); they're already absent from the
deferred table after `#003`.

---

## FC sides that need tag-driven consumption (Phase 2 `#006`)

The audit exposes which FC-side recipes currently ID-check wood/stone/iron
ingredients and need to accept modern equivalents:

| FC recipe | Ingredient ID check | Target tag |
|---|---|---|
| Sawmill planks | `Block.planks` (ID 5) with metadata | `#minecraft:planks` |
| Crafting table craft | Hardcoded oak planks | `#minecraft:planks` |
| Axe handle recipes | Various `Item.stick` / plank combos | `#minecraft:planks`, `#forge:rods/wooden` |
| Pickaxe head recipes | `Block.cobblestone.blockID` | `#forge:cobblestone` |
| Iron-tier tool recipes | `Item.ingotIron.itemID` | `#forge:ingots/iron` |

**Note:** each entry above becomes a separate row in the Phase 2 `#006`
tag-remediation entry's scope. Concrete file:line references must be
gathered during Phase 2 implementation by greppin
`Common/src/main/java/net/minecraft/src/btw/crafting/FCRecipes.java` for
`Block.planks`, `Item.ingotIron`, etc.

---

## Test matrix (Phase 8 `#052`, `#053` verification)

| Scenario | Expected |
|---|---|
| Mine obsidian with diamond pick | Breaks at vanilla speed |
| Mine obsidian with netherite pick | Breaks at same speed as diamond (no efficiency bump) |
| Hit zombie with netherite sword | Damage = 7 (same as diamond) |
| Hit zombie with soulforged-steel sword | Damage = 8 (hard cap holds) |
| Throw netherite tool into lava (dropped on ground) | Item survives, floats (preserved) |
| Attempt netherite-smithing-upgrade without potash | Recipe fails to match |
| With potash + concentrated hellfire + diamond tool + netherite + template | Produces netherite tool |
| Craft lodestone with vanilla netherite | Recipe fails |
| Craft lodestone with soulforged steel | Produces lodestone |
| Stand in bamboo jungle, craft wooden pickaxe | Succeeds (bamboo planks pass tag) |
| Mine cherry tree, craft crafting table | Succeeds (cherry planks pass tag) |

## Files touched by this audit's downstream integrations

| Phase | File | Note |
|---|---|---|
| Phase 2 `#005` | `data/minecraft/recipes/netherite_smithing_upgrade.json` | Add FC reagent ingredients |
| Phase 2 `#005` | `data/minecraft/recipes/lodestone.json` | Swap netherite → soulforged steel |
| Phase 2 `#006` | FC side: `FCRecipes.java` | Swap hardcoded ID checks for tag-driven where practical; may require new mixin or new FC method `IsPlankMaterial(ItemStack)` that checks `#minecraft:planks` tag |
| Phase 8 `#052` | `Forge/src/main/java/btw/forge/mixin/NetheriteTierMixin.java` or access transformer | Re-tier NETHERITE enum constants |
| Phase 8 `#052` | `Forge/src/main/resources/accesstransformer.cfg` (may need creation) | Expose Tiers.NETHERITE fields for mutation |
| Phase 8 `#053` | Phase 2 recipe overrides + no additional code | Pure data |

## Out of scope for this audit

- **Soulforged armor tier**: FC has no Soulforged armor (confirmed in
  `EnumArmorMaterial.java:24-29`). Adding one would shift the armor
  ceiling and create a new design problem. This audit does not propose
  it. Netherite armor is committed to slot at DIAMOND.
- **Bamboo/cherry/mangrove wood as fuel tier**: modern MC has no fuel
  tier system; burn times are per-item and already covered by FC's
  `SetFurnaceBurnTime` calls. Any gaps here are data-driven
  (`#006` tag remediation) and covered above.
- **Copper tool reintroduction via separate mod**: outside port scope.
  If a tool-mod adds copper tools, it's on that mod to integrate with
  FC tiers.
