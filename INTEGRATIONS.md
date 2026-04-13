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

Features identified as conflicting but not yet integrated. Keep the
list short — add here, then promote to the integration log once done.

| # | Feature | Pattern (tentative) | Blocker |
|---|---------|---------------------|---------|
| — | Villager trading | D (FC-extend, add FC trades) | No pilot yet |
| — | Elytra | B (gate behind soulforge fuel) | Needs FC alchemy design |
| — | Grindstone disenchanting | C (tax via FC reagent) | |
| — | Bastion loot tables | C (loot override) | Data-driven, easy |
| — | Totem of Undying | B (require FC component) | |
| — | Shields | D (FC bashable + tier) | Needs combat audit |
| — | Off-hand slot | D (FC torch/shield pairing) | |
