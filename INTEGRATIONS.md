# FC ↔ Modern MC Integration Protocols

This document defines the contract for reconciling FC gameplay with
post-1.5.2 Minecraft features. Every integration lives here as a
numbered, audited entry. Nothing gets silently modified.

---

## Protocols (strict)

1. **Every FC-side change carries an integration tag.**
   Java: `// FCMOD-INTEGRATION(1.20.1): <feature>: <one-line why>`
   Mixins: full Javadoc block on the class (Conflict / Pattern / Rebalance / Rejected alternatives).

2. **Gate modern affordances through FC infrastructure — don't delete them.**
   If vanilla gives you X for free, attach an FC cost to the channel,
   don't remove the channel.

3. **Prefer data-driven overrides over code changes** (loot tables,
   recipes, tags, gamerules, biome modifiers). Code changes only when
   data can't express the integration.

4. **Prefer Forge events over mixins** when you only need to observe/cancel.
   Mixin only for surgical bytecode changes where no event exists.

5. **Keep both items/blocks dual-sourced.** Never silently replace vanilla X
   with FC Y — let vanilla X still exist and rebalance it so FC's version
   is the natural choice. Preserves compat with other mods.

6. **Version-lock the assumption.** Every integration names `(1.20.1)` in
   its tag. A future port knows exactly what to re-examine.

7. **Test with "what if another mod depends on this"** before shipping.

---

## Integration patterns

Choose one per feature. Integrations that combine patterns must justify
the combination.

| Pattern | Name | When to use |
|---------|------|-------------|
| **A** | Tier-align | Modern item is fine in concept, just belongs at FC's equivalent tier. |
| **B** | Gate | Require FC infrastructure / material to unlock or use. |
| **C** | Rebalance | Keep the feature; adjust numbers so it slots into FC's economy. |
| **D** | FC-extend | FC adds its own variants using the modern feature's framework. |
| **E** | Naturalize | Leave vanilla intact, buff FC's version so players prefer FC. |

---

## Integration log

### #001 — Mending enchantment

| Field | Value |
|-------|-------|
| **Modern feature** | Mending enchantment (MC 1.9+) |
| **Added** | 2016-02 / MC 1.9 |
| **Conflict** | Infinite durability trivialises FC's anvil / steel / soulforge repair loop |
| **Pattern** | **C** (rebalance, preserve feature) |
| **Implementation** | [`MendingCapMixin`](Forge/src/main/java/btw/forge/mixin/MendingCapMixin.java) |
| **Effect** | Each mending XP orb restores **½** the vanilla durability amount. XP cost is unchanged. |
| **Dual-sourced?** | Yes — the enchantment still appears in loot, trades, and enchanting table RNG. |
| **Data or code?** | Code (mixin). Data-driven override was not possible — Mending's repair amount is hard-coded in `ExperienceOrb.repairPlayerItems`. |
| **Alt rejected** | Gate mending-books behind soulforge. Rejected under Protocol #5 (would remove mending from vanilla enchanting RNG, breaks other mods). |
| **Verified by** | Refmap emits `m_147092_` (repairPlayerItems) + `m_41721_` (setDamageValue) mappings → production build applies correctly. |
| **Net effect on FC loop** | A mending pickaxe now takes 2× as many XP orbs to fully repair, so the anvil / soulforge chain is still the fastest maintenance route for heavy use. Mending remains a quality-of-life boon, not a tool-immortality switch. |

### #002 — Gamerule defaults

| Field | Value |
|-------|-------|
| **Modern feature** | Post-1.5.2 gamerules: `doInsomnia` (1.13), `doPatrolSpawning` (1.14), `doTraderSpawning` (1.14), `doWardenSpawning` (1.19), `keepInventory` (1.4) |
| **Added** | MC 1.4 – 1.19 |
| **Conflict** | Phantoms double-punish FC's sleep cost; pillager patrols and wandering traders drop loot / sell items that shortcut FC progression; warden is irrelevant to FC design; keepInventory removes FC's load-bearing death penalty |
| **Pattern** | **C** (rebalance via data — set FC-appropriate defaults, preserve user override) |
| **Implementation** | [`WorldGamerulesHandler`](Forge/src/main/java/btw/forge/WorldGamerulesHandler.java) |
| **Effect** | New worlds get `doInsomnia=false`, `doPatrolSpawning=false`, `doTraderSpawning=false`, `doWardenSpawning=false`, `keepInventory=false`. Existing worlds are untouched. Users can override via `/gamerule`. |
| **Dual-sourced?** | Yes — gamerules are vanilla-native and fully reversible |
| **Data or code?** | Event handler (Forge `LevelEvent.CreateSpawnPosition`). No mixin, no bytecode. |
| **Alt rejected** | `ServerAboutToStartEvent` (fires every server start — would override user preferences on restart, violates Protocol #2). |
| **Verified by** | New world → `/gamerule doInsomnia` reports `false`. Existing world → user-set values unchanged. |
| **Net effect on FC loop** | Removes phantom/patrol/trader/warden distractions from FC's early game. Death penalty stays intact by default. |

### #003 — Pattern-E batch (naturalized, no code)

| Field | Value |
|-------|-------|
| **Modern feature** | 21 post-1.5.2 additive features with no FC-design friction. |
| **Added** | MC 1.7 – 1.20 |
| **Conflict** | None that degrade the FC core loop. Each row is either purely cosmetic, a compatible mob variant, or a niche effect FC already tolerates. |
| **Pattern** | **E** (naturalize — allow feature, FC doesn't compete) |
| **Implementation** | None. Features ship vanilla-intact. |
| **Effect** | These rows are reviewed and batch-approved for unchanged behavior. They remain fully present in the game with vanilla mechanics. |
| **Dual-sourced?** | Yes — all items/mechanics remain vanilla-reversible via datapack removal. |
| **Data or code?** | Neither. Documentation entry only. |
| **Alt rejected** | Writing one integration entry per cosmetic row. Rejected: inflates the log with 21 rows that document "no change", drowning real integrations; batching preserves auditability (each row is listed below) while keeping the log scannable. |
| **Verified by** | Static review against Protocol #5 (don't silently replace vanilla). No runtime test — Pattern-E rows are definitionally no-op. |
| **Net effect on FC loop** | Zero. |
| **Batched rows** | Boss bars (1.10); Pufferfish poison teach (1.7); Curse of Vanishing (1.11); Curse of Binding (1.11); Loom (1.14); Cartography table (1.14); Fletching table (1.14); Polar bear (1.10); Husk/stray/drowned (1.10/1.13); Frog/tadpole (1.19); Cat (1.14); Fox (1.14); Panda (1.14); Glowing effect (1.9); Luck / Bad Luck effects (1.9); Levitation effect (1.9); Music discs (1.10+); Banner patterns (1.8); Armor trims (1.20); Hanging signs (1.20); Decorated pots (1.20). |

### #004 — Loot-table nerfs (data-driven)

| Field | Value |
|-------|-------|
| **Modern feature** | Post-1.9 loot tables that drop progression-skipping items — bastion chests, ancient city chests, pillager outpost chests, woodland mansion chests, trail-ruins archaeology |
| **Added** | MC 1.9 – 1.20 |
| **Conflict** | Free high-tier loot (ancient debris, diamond gear, enchanted books, iron pickaxes, music discs) bypasses FC's mining/smithing/enchant chains. Players who find a bastion can skip entire tiers of progression |
| **Pattern** | **C** (rebalance via data — keep feature, subtract the progression-skipping items) |
| **Implementation** | Global Loot Modifiers registered via [`BTWLootModifiers`](Forge/src/main/java/btw/forge/data/BTWLootModifiers.java) + [`FCRemoveItemFromLootModifier`](Forge/src/main/java/btw/forge/data/FCRemoveItemFromLootModifier.java). JSON instances emitted by [`FCLootOverrides`](Forge/src/main/java/btw/forge/data/FCLootOverrides.java) during `runData` |
| **Effect** | The following items are stripped from their generating loot tables: ancient debris (all 4 bastion chest tiers); diamond leggings + diamond horse armor (ancient city); iron pickaxe + enchanted books (pillager outpost); diamond chestplate (woodland mansion); Relic music disc (trail-ruins brushing). **10 Global Loot Modifiers total.** |
| **Dual-sourced?** | Yes — vanilla loot tables are untouched; GLMs are an additive layer that server ops can disable via pack priority |
| **Data or code?** | Both — one custom `FCRemoveItemFromLootModifier` codec class (~60 lines) + JSON GLM instances generated via data-gen |
| **Alt rejected** | Full loot-table replacement JSON. Rejected because (a) brittle — vanilla loot tables change between MC patches, we'd have to re-sync; (b) breaks compat with other mods that also modify those tables via GLMs; (c) more JSON per table |
| **Verified by** | `gradlew :Forge:runData` emits 10 JSON files under `src/generated/resources/data/betterthanwolves/loot_modifiers/` plus `global_loot_modifiers.json` index. In-world: open a bastion treasure chest, confirm ancient debris absent from drops |
| **Net effect on FC loop** | Structure exploration stays valuable (players still find emeralds, books, food, situational items), but no structure is a "skip to tier N" shortcut. Netherite progression requires netherite-tier mining (still possible; just not chest loot) |

### #005 — Stonecutter slab-output rebalance (data-driven)

| Field | Value |
|-------|-------|
| **Modern feature** | Stonecutter (MC 1.14) as a 2× efficiency multiplier over crafting table for slab recipes |
| **Added** | MC 1.14 |
| **Conflict** | Stonecutter turns 1 stone → 2 stone slabs, while the crafting table gives 6 slabs from 3 stone (2 per stone). Net: stonecutter matches crafting-table efficiency *and* lets the player skip all shape-swap intermediate crafts. In FC's economy, where stone is a foundational tier the player earns through mining with a wood/stone pickaxe, the shortcut erodes the early-game mining loop |
| **Pattern** | **C** (rebalance via data — halve stonecutter slab output to 1, matching its output pattern for stairs/walls/etc.) |
| **Implementation** | [`FCRecipeOverrides`](Forge/src/main/java/btw/forge/data/FCRecipeOverrides.java) — 33 stonecutter recipes regenerated at their vanilla resource locations with `count=1` |
| **Effect** | Stonecutter-produced slabs across stone, cobblestone, mossy, smooth, bricks, granite, diorite, andesite, sandstone (regular/smooth/cut/red), prismarine family, nether bricks, end stone bricks, blackstone family, deepslate family, and mud bricks: output count halved from 2 → 1 |
| **Dual-sourced?** | Yes — players can still craft slabs via the 3-stone → 6-slab crafting-table recipe (vanilla, untouched). Stonecutter remains functional, just no longer a strict improvement |
| **Data or code?** | Pure data (generated JSON at `data/minecraft/recipes/*_stonecutting.json` override paths) |
| **Alt rejected** | Remove stonecutter slab recipes entirely. Rejected under Protocol #5 — removes a vanilla affordance rather than rebalancing it; would also break compat with mods that expect stonecutter-to-slab paths |
| **Verified by** | `gradlew :Forge:runData` emits 33 JSON files under `src/generated/resources/data/minecraft/recipes/`. In-world: load recipe book → stonecutter with 1 stone shows `1 stone_slab` output (was `2`) |
| **Net effect on FC loop** | Early-game stone economy retains its weight. Stonecutter is still the "right tool" for non-slab shapes (stairs, walls, cut variants) since those were already 1:1 |

### #006 — Frost Walker radius cap

| Field | Value |
|-------|-------|
| **Modern feature** | Frost Walker enchantment (MC 1.9+) — freezes water blocks in a radius around the wearer while walking |
| **Added** | MC 1.9 |
| **Conflict** | Vanilla level-1 Frost Walker freezes a 5×5 area (radius 2) under the player, turning any body of water into walkable terrain. FC worldgen tunes water crossings as deliberate traversal obstacles (swim exhaustion, water slowdown, limited ice-breaking). Free water crossings defeat that design |
| **Pattern** | **C** (rebalance, preserve feature) |
| **Implementation** | [`FrostWalkerRadiusMixin`](Forge/src/main/java/btw/forge/mixin/FrostWalkerRadiusMixin.java) — `@ModifyConstant` on the `2` literal inside `onEntityMoved`, reducing base radius from `2 + level` to `0 + level` |
| **Effect** | Level 1 → 1×1 freeze (block under player only); Level 2 → 3×3 freeze (small puddle). Enchantment still rolls in loot and enchanting table |
| **Dual-sourced?** | Yes — enchantment remains in vanilla loot pools and enchanting RNG |
| **Data or code?** | Code (mixin) |
| **Alt rejected** | Remove Frost Walker from treasure-enchant pool. Rejected under Protocol #5 — removes rather than rebalances |
| **Verified by** | Refmap at `Forge/build/tmp/compileJava/betterthanwolves.refmap.json` emits `m_45018_` mapping for `onEntityMoved` → production build applies correctly |
| **Net effect on FC loop** | Water crossings remain deliberate traversal choices. Frost Walker is now a modest slip-protection perk rather than a free-bridge enchant |

### #007 — Respawn anchor single-use

| Field | Value |
|-------|-------|
| **Modern feature** | Respawn Anchor (MC 1.16+) — Nether block that stores up to 4 glowstone charges, each allowing one respawn at the anchor |
| **Added** | MC 1.16 |
| **Conflict** | FC's HC Spawn (integration `#002`) makes initial respawn location load-bearing for early-game progression. The respawn anchor is a portable Nether-side second spawn, stacking 4 deep — removes death consequence entirely for long Nether stretches |
| **Pattern** | **C** (rebalance, preserve feature) |
| **Implementation** | [`RespawnAnchorChargeMixin`](Forge/src/main/java/btw/forge/mixin/RespawnAnchorChargeMixin.java) — `@ModifyConstant` on the `4` literal inside `canBeCharged(BlockState)`, capping at 1 |
| **Effect** | Respawn anchor accepts exactly one charge. Using it to respawn consumes the charge and requires re-charging. Per `docs/audit/alchemy.md`, each charge costs `fcItemConcentratedHellfire` (gating follow-up, deferred) |
| **Dual-sourced?** | Yes — anchor block and recipe remain vanilla-reachable |
| **Data or code?** | Code (mixin) |
| **Alt rejected** | Delete respawn anchor recipe. Rejected under Protocol #5 — breaks player autonomy; also leaves dangling block in world if players find one in structure loot |
| **Verified by** | Refmap emits `m_55894_` mapping for `canBeCharged` → production build applies correctly |
| **Net effect on FC loop** | Anchor becomes a "one escape route" commitment rather than a routine safety net. HC Spawn stays meaningful |

### #008 — Swift Sneak level cap

| Field | Value |
|-------|-------|
| **Modern feature** | Swift Sneak enchantment (MC 1.19+) — legging-slot enchant that reduces the sneak-speed penalty; at level 3 sneak speed approaches full walking speed |
| **Added** | MC 1.19 |
| **Conflict** | FC's cave mining loop treats crouch as a deliberate speed trade-off for ledge safety. Swift Sneak III eliminates the trade entirely, making crouched mining strictly better than upright — collapses a moment-to-moment design decision FC expects the player to make |
| **Pattern** | **C** (rebalance, preserve feature) |
| **Implementation** | [`SwiftSneakLevelMixin`](Forge/src/main/java/btw/forge/mixin/SwiftSneakLevelMixin.java) — `@Inject(HEAD, cancellable)` on `getMaxLevel`, forces return value to `1` |
| **Effect** | Swift Sneak rolls only at level 1 via enchanting table and Forge enchantment RNG. Level 1 still provides a partial mitigation (useful for long mining) without eliminating the trade-off |
| **Dual-sourced?** | Yes — enchant remains available via all vanilla pathways |
| **Data or code?** | Code (mixin) |
| **Alt rejected** | Remove Swift Sneak from ancient-city loot. Rejected under Protocol #5 |
| **Known limitation** | Pre-generated ancient-city loot rolling Swift Sneak III books still functions at level 3 (level is NBT-stored and consulted at consumption). A consumption-time clamp via `EnchantmentHelper#getEnchantmentLevel` remains possible if playtest shows it matters |
| **Verified by** | Refmap emits `m_6586_` mapping for `getMaxLevel` → production build applies correctly |
| **Net effect on FC loop** | Crouch-vs-walk is a meaningful decision in FC mining again. Swift Sneak becomes "modest comfort boost" rather than "crouch-mode upgrade path" |

### #009 — Totem of Undying soul-flux gate

| Field | Value |
|-------|-------|
| **Modern feature** | Totem of Undying (MC 1.11+) — held totem activates on lethal damage, restoring 1 HP + Regen + Fire Resist + Absorption, consuming the totem |
| **Added** | MC 1.11 |
| **Conflict** | Free death-save short-circuits FC's HC Spawn penalty (integration `#002`). Evokers drop totems at 100% and can be farmed, which turns late-game FC into "accumulate totems, become immortal" |
| **Pattern** | **B** (gate via FC reagent — preserve feature, route through FC's alchemy chain) |
| **Implementation** | [`TotemOfUndyingMixin`](Forge/src/main/java/btw/forge/mixin/TotemOfUndyingMixin.java) — `@Inject(HEAD, cancellable)` on `LivingEntity#checkTotemDeathProtection(DamageSource)` |
| **Effect** | Totem still required in a hand (vanilla behavior), but also requires at least one `fcItemSoulFlux` (item ID 2555) in the player's inventory. On save, one soul flux is consumed alongside the totem. Without soul flux, totem doesn't save. Non-player entities unaffected. Damage sources that bypass invulnerability are passed through to vanilla unchanged (no soul flux consumed on unsavable deaths) |
| **Dual-sourced?** | Yes — totems remain in vanilla loot (evoker drops, woodland-mansion chests), and the vanilla activation mechanic is untouched when soul flux is present |
| **Data or code?** | Code (mixin) |
| **Alt rejected** | Remove totem from evoker drops. Rejected under Protocol #5 — loot-source removal breaks other mods and is heavier than an activation cost |
| **Alt rejected (2)** | `@Redirect` on `shrink(1)` inside vanilla's totem block. Rejected because if we skip the shrink, vanilla still returns `true` and applies save effects, causing the totem to save without being consumed — bug in the wrong direction |
| **Verified by** | Refmap emits `m_21262_` for `checkTotemDeathProtection`. In-game test: lethal damage with totem but no soul flux → player dies; with totem + soul flux → player saved, totem consumed, soul flux decremented |
| **Net effect on FC loop** | Totem saves become endgame commitments rather than routine escape hatches. HC Spawn retains its weight through late game |

### #010 — Crossbow piercing cap

| Field | Value |
|-------|-------|
| **Modern feature** | Crossbow PIERCING enchantment (MC 1.14+) — each level allows projectiles to penetrate one additional entity |
| **Added** | MC 1.14 |
| **Conflict** | FC's ranged-combat armor-bypass mechanism is the `FCItemArrowBroadhead` item (designed-in). Piercing III+ fires through walls of mobs, turning FC spider swarms and zombie hordes into one-shot cleanups, collapsing FC's number-based encounter tuning |
| **Pattern** | **C** (rebalance, preserve feature) |
| **Implementation** | [`CrossbowPiercingMixin`](Forge/src/main/java/btw/forge/mixin/CrossbowPiercingMixin.java) — `@Redirect` on `AbstractArrow#setPierceLevel(byte)` inside `CrossbowItem#shootProjectile` |
| **Effect** | When a crossbow projectile is fired, the piercing level applied to the outgoing arrow is clamped to `min(1, enchantLevel)`. Enchantment still appears in loot and on NBT at any level; only the fire-time effect is capped |
| **Dual-sourced?** | Yes — Piercing stays in enchant pools; crossbows with level-4 piercing still exist in loot |
| **Data or code?** | Code (mixin) |
| **Alt rejected** | Overwrite `PiercingEnchantment.getMaxLevel()` to 1. Rejected because pre-generated loot already has level-3/4 crossbows; level is stored in NBT and consulted at fire time regardless of max-level API. Fire-time clamp is more robust |
| **Verified by** | Refmap emits `m_40894_` for `shootProjectile` (10-arg method matching CrossbowItem's static shoot). In-game: Piercing V crossbow fires → hit first zombie in a line, second zombie unaffected |
| **Net effect on FC loop** | Crossbow remains viable ranged weapon, but mobs-in-a-line no longer cascade-die. Spider/zombie swarms retain their intended threat |

### #011 — Sweeping Edge suppression

| Field | Value |
|-------|-------|
| **Modern feature** | Sweeping Edge enchantment (MC 1.11+) — sword enchant that inflicts reduced AoE damage on entities near the primary target |
| **Added** | MC 1.11 |
| **Conflict** | FC combat is per-target-hit — each swing costs exhaustion, and damage is reconciled individually. AoE melee breaks that accounting: a single swing can kill multiple mobs without paying the per-mob exhaustion cost. FC's spider swarms and husk clusters are tuned as numerically-threatening encounters, not as loot-bundles |
| **Pattern** | **C** (rebalance, preserve feature) |
| **Implementation** | [`SweepingEdgeMixin`](Forge/src/main/java/btw/forge/mixin/SweepingEdgeMixin.java) — `@Redirect` on `Level#getEntitiesOfClass(Class, AABB)` inside `Player#attack`, returning empty list |
| **Effect** | Sweeping animation, sound, and particle still fire — visible feedback preserved. But the peripheral-entity list used for sweep damage is empty, so no secondary entity takes damage. Sweeping Edge is effectively cosmetic |
| **Dual-sourced?** | Yes — enchant still rolls at all levels, still applies to swords, still displays in item tooltip |
| **Data or code?** | Code (mixin) |
| **Alt rejected** | `@Redirect` on `EnchantmentHelper.getSweepingDamageRatio` returning 0. Rejected because vanilla formula is `sweepMul = 1.0f + ratio × damage`; ratio=0 still yields sweepMul=1.0, so peripheral entities take 1 damage per sweep. Zero-target is cleaner |
| **Alt rejected (2)** | Overwrite `Player#attack`'s sweep block. Rejected — high mixin-conflict risk with mods injecting into attack for stat/damage reasons |
| **Verified by** | Refmap emits `m_5706_` for `attack` + `m_45976_` for `getEntitiesOfClass`. In-game: swing soulforged-steel sword at a sheep inside a 3-sheep clump → only the directly-targeted sheep takes damage |
| **Net effect on FC loop** | Per-target exhaustion cost is enforced. Crowd control via AoE is no longer a free bypass of FC's swing economy |

### Follow-up from #005 implementation

The stonecutter pass reinforced that several planned recipe overrides —
lodestone (needs FC soulforged steel), recovery compass (needs FC
arcane scroll), conduit disable (needs FC crucible replacement in the
same sprint to avoid regression) — are **blocked on stable resource
locations for FC items**. FC's legacy ParseID→index mapping renders
each item as `betterthanwolves:item_<runtime_index>`, which shifts if
the FC config changes ParseIDs. Promoted as a blocker to the deferred
`#006` tag-remediation entry.

---

## Adding a new integration — checklist

When you pilot a new feature:

- [ ] Pick an integration pattern (A–E) and justify it in one sentence.
- [ ] Write an alternative you considered and rejected (forces you to
      explore the design space).
- [ ] Prefer Forge event → loot-table override → recipe override →
      mixin, in that order.
- [ ] If mixin, include a full Javadoc block on the class with:
      Feature / Conflict / Pattern / Rebalance / Injection strategy /
      Alt rejected.
- [ ] Add `// FCMOD-INTEGRATION(1.20.1): <feature>` to any touched
      FC source line.
- [ ] Add a new row to the Integration log above.
- [ ] Rebuild and verify refmap (`grep <MixinName>
      Forge/build/tmp/compileJava/betterthanwolves.refmap.json`).
- [ ] Smoke-test in dev run.
- [ ] Smoke-test in built jar (reobfuscation catches things dev misses).

---

## Tracking deferred integrations

Catalogue of post-1.5.2 MC features that touch FC's design surface and are
not yet integrated. Grouped by domain. Each row carries a tentative
pattern (A–E). Items already implemented live in the integration log
above and do **not** appear here. Pattern **E** ("naturalize") rows are
listed for completeness but are batch-approvable with no FC code change.

Promote a row out of this section by writing it up in the integration
log above with the next sequential `#NNN`.

### Combat & weapons (1.9 onward)

| # | Feature | Since | Pattern | Notes |
|---|---------|-------|---------|-------|
| — | Off-hand slot | 1.9 | D | FC inventory has no slot 40; pair with FC torch/shield use |
| — | Attack cooldown bar | 1.9 | C | FC combat assumes instant repeat; cooldown halves DPS vs FC penalty model |
| — | Shields | 1.9 | D | FC variant: requires hide + iron, blocks % based on stamina |
| — | Tipped arrows / lingering potions | 1.9 | C | Lingering only craftable if FC potion stand |
| — | Trident + Riptide/Loyalty/Channeling | 1.13 | B | Gate behind drowned drop in FC nether/wild loot |
| — | Smithing-template netherite upgrade | 1.20 | A or B | Re-tier netherite under Soulforged, or gate behind soulforge |
| — | Mace + Wind Charge | 1.21 | — | Future port only |

### Inventory, UI & player UX

| # | Feature | Since | Pattern | Notes |
|---|---------|-------|---------|-------|
| — | Recipe book | 1.12 | C | Reveals all FC recipes; only books FC explicitly grants |
| — | Advancements (replaces achievements) | 1.12 | D | FC adds its own advancement tree as datapack |
| — | Spectator mode | 1.8 | C | Server gamerule disable in default |
| — | Auto-jump | 1.10 | C | Default off — bypasses FC penalty-jump tuning |
| — | Held-item swap (F key) | 1.9 | C | Rebind to FC torch toggle |

### Movement & survival

| # | Feature | Since | Pattern | Notes |
|---|---------|-------|---------|-------|
| — | Sprint mechanics (revised) | 1.9 | — | Partially bridged in `PlayerBridge`; needs penalty-gate audit |
| — | Elytra | 1.9 | B | Gate behind soulforge fuel + cooldown; bypasses ground travel & HC Spawn |
| — | Powder snow + freezing damage | 1.17 | D | Route through FC `DamageSource`, allow leather-boots immunity |
| — | Scaffolding | 1.14 | C | Limit FC variant to N blocks of vertical traversal |
| — | Dolphin's grace effect | 1.13 | C | Cap duration |
| — | Soul speed enchant | 1.16 | B | Require FC reagent |
| — | Bubble columns (magma/soul sand) | 1.13 | C | Rebalance flow — trivializes underwater mob farms |

### Food & hunger

| # | Feature | Since | Pattern | Notes |
|---|---------|-------|---------|-------|
| — | Honey bottle | 1.15 | C | Low restore, FC alchemy reagent |
| — | Sweet berries / glow berries | 1.14 / 1.17 | C | Low restore, hostile bush damage |
| — | Suspicious stew | 1.14 | C | Lower buff tier |
| — | Composter | 1.14 | D | FC compost-bin variant; bypasses FC's hemp/dung composting chain |

### Tools, tiers & enchantments

| # | Feature | Since | Pattern | Notes |
|---|---------|-------|---------|-------|
| — | Netherite tier | 1.16 | A | Re-tier under Soulforged; conflicts with FC's tier ceiling |
| — | Smithing templates | 1.20 | B | Require FC ingredient in template |
| — | Anvil rename / repair audit | pre-FC | — | Audit FC anvil overrides for parity with `MendingCapMixin` siblings |
| — | Grindstone disenchanting | 1.14 | C | Tax via FC reagent |
| — | Anvil XP cost cap (40) | 1.9 | C | Raise cap, FC charges differently |

### Blocks & materials

| # | Feature | Since | Pattern | Notes |
|---|---------|-------|---------|-------|
| — | Copper (block + oxidation) | 1.17 | D | FC repurposes for piping/wiring |
| — | Amethyst geodes | 1.17 | C | Slow regen rate to avoid free renewable |
| — | Lightning rod | 1.17 | C | Reduce range — negates FC lightning ignition |
| — | Sculk catalyst/shrieker/sensor | 1.19 | C | Sensor wireless redstone shortcuts FC redstone |
| — | Lodestone | 1.16 | B | Require FC nether material — compass-tracking trivializes navigation |
| — | Conduit | 1.13 | C | Smaller radius, drains hunger — free Strength III + water breathing |
| — | Beacon (post-1.4 + soul beacon) | 1.4 / 1.16 | C | FC food cost, smaller pyramid base |
| — | Mangrove propagules / azalea | 1.19 / 1.17 | D | FC growth tick rules |
| — | Chiseled bookshelves | 1.20 | C | Comparator output signal nerf |
| — | Bamboo wood family | 1.20 | A | Tier-align with FC wood progression |

### Crafting & workstations

| # | Feature | Since | Pattern | Notes |
|---|---------|-------|---------|-------|
| — | Blast furnace | 1.14 | B or D | Recipe set restricted, or FC variant — bypasses bloomery progression |
| — | Smoker | 1.14 | C | Slower than vanilla, costs FC fuel |
| — | Campfire | 1.14 | C | Limited recipes |
| — | Datapack recipe / FC `CraftingManager` priority | 1.13 | — | Audit needed — could shadow FC recipes |

### Mobs

| # | Feature | Since | Pattern | Notes |
|---|---------|-------|---------|-------|
| — | Horse / donkey / mule | 1.6 | D | FC saddle + breeding rebalance |
| — | Llama / trader llama | 1.11 / 1.14 | D | Caravan mount + spit |
| — | Rabbit | 1.8 | C | Food value low |
| — | Aquatic life (turtle, dolphin, fish) | 1.13 | C | Drops slotted into FC food tiers |
| — | Pillager / vindicator / ravager / evoker | 1.14 / 1.11 | C | Raid mob drops (totem, emerald, raid-specific items) — `#004` covered the outpost chest loot; mob-drop loot still open |
| — | Bee | 1.15 | D | FC honey integrates with alchemy |
| — | Piglin / hoglin / zoglin / strider / piglin brute | 1.16 | D | Barter table FC-flavored |
| — | Goat / axolotl / glow squid | 1.17 | C | Axolotl drops nerf, goat horn locked |
| — | Allay | 1.19 | C | Limit count — auto item-collect trivializes FC sorting |
| — | Camel | 1.20 | D | Mount |
| — | Sniffer + torchflower / pitcher plant | 1.20 | C | Low yield |

### World generation

| # | Feature | Since | Pattern | Notes |
|---|---------|-------|---------|-------|
| — | 1.7 biome rewrite | 1.7 | D | FC biome modifier datapack — `BiomeDecorator` only knows old biomes |
| — | Ocean monument / elder guardian | 1.8 | C | Mining fatigue + prismarine balance |
| — | Datapack worldgen (noise router) | 1.18 | D | FC datapack overrides — `WorldType` ignored |
| — | World height extension (-64 → 320) | 1.18 | — | Audit FC pathfinding & block IDs assuming 0–255 |
| — | Cave biomes (lush/dripstone/deep dark) | 1.17 / 1.19 | D | FC ore tweaks per cave biome |
| — | Cherry biome | 1.20 | A | Biome + wood tier-align |
| — | Mangrove biome | 1.19 | A | Biome + wood + mud + propagules |

### Trading & villages

| # | Feature | Since | Pattern | Notes |
|---|---------|-------|---------|-------|
| — | Profession workstation system | 1.14 | D | FC professions on workstations (in progress: `ContainerMerchant`) |
| — | Trade level XP / progression | 1.14 | C | Slow leveling |
| — | Hero of the Village | 1.14 | C | Disable or nerf — free discount + items |
| — | Cured zombie villager discount | 1.4 / 1.14 | C | Cap discount — trivial near-infinite trades |
| — | Iron golem auto-spawn from villagers | 1.14 | C | Require FC trigger — free defenses |

### Death, respawn & sleep

| # | Feature | Since | Pattern | Notes |
|---|---------|-------|---------|-------|
| — | Recovery compass | 1.19 | C | Require FC compass + reagent — trivializes lost-corpse retrieval |
| — | Hardcore-mode spectator-on-death | 1.3 | — | Audit interaction with FC death override |

### Status effects & potions

| # | Feature | Since | Pattern | Notes |
|---|---------|-------|---------|-------|
| — | Slow Falling | 1.13 | C | Low brewing yield, expensive |

### Audio / visual / cosmetic

| # | Feature | Since | Pattern | Notes |
|---|---------|-------|---------|-------|
| — | New particle types | 1.13+ | — | Partial: numeric ID translation — finish coverage in client bridge |

### Networking / data / commands

| # | Feature | Since | Pattern | Notes |
|---|---------|-------|---------|-------|
| — | Brigadier commands | 1.13 | D | Re-register FC commands as Brigadier nodes — `CommandBase` is old API |
| — | Datapack tag system | 1.13 | — | Audit — FC ID-based checks ignore tags |
| — | Resource pack v2/v3/v4 textures | various | — | Partial via `BlockModelBridge` / `FCBakedModel`; finish coverage |
| — | World height in heightmap APIs | 1.18 | — | Audit FC code assuming 0–255 |
| — | IFluidHandler | Forge | D | Partial — wire FC fluid containers (cauldron, bucket, etc.) |
