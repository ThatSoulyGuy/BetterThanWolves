# Combat audit

Pinned numbers and decisions for Phase 4 combat integrations (`#021–#028`).
Every row in this doc is a decision that downstream mixins and event
handlers must honor exactly. Source references are to files from the FC
1.5.2 source tree (shadow-remapped to `btw.modern.*` at runtime).

---

## 1. FC melee damage — baseline

FC weapon damage is formula-driven, no attack cooldown.

```
weaponDamage = 4 + EnumToolMaterial.getDamageVsEntity()
                   ──────────────────────────────────
                     Common/src/.../FCItemBattleAxe.java:29
                     vanilla/server/.../ItemSword.java:30
```

Per tier (`vanilla/server/net/minecraft/src/EnumToolMaterial.java:29-34`):

| Tier | `damageVsEntity` | Sword dmg | Battle-axe dmg |
|---|---|---|---|
| WOOD | 0 | 4 | — |
| STONE | 1 | 5 | — |
| IRON | 2 | 6 | — |
| GOLD | 0 | 4 | — |
| EMERALD (diamond) | 3 | 7 | — |
| **SOULFORGED_STEEL** | **4** | **8** | **8** (same formula; this is FC's top-tier weapon) |

**FC melee damage modifier** (health + exhaustion + sightless penalty) is
applied multiplicatively at `vanilla/server/.../EntityPlayer.java:1226-1231`:

```java
float fModifier = GetMeleeDamageModifier();
if ( fModifier < 0.99F ) {
    var2 = (int)((float)var2 * fModifier);
}
```

Already bridged via `PlayerBridge.pendingMeleeDamageModifier` +
`FCGameplayBridge.onLivingHurt`. **No change needed.**

## 2. Attack cooldown (modern 1.9+) — **disable when holding FC weapon**

**Decision:** vanilla 1.9 attack-cooldown is **bypassed** whenever the
held main-hand item is any of:
- `btw.modern.FCItemTool` (any subclass)
- `btw.modern.ItemSword` (FC's remapped version)
- `btw.modern.FCItemBattleAxe`
- `btw.modern.FCItemClubWood`
- `btw.modern.FCItemMattock`

**Reason:** FC already rate-limits melee spam via exhaustion: every hit
calls `addExhaustion(0.3F)`; `GetMeleeDamageModifier` scales down damage
as exhaustion climbs. The 1.9 cooldown double-penalizes and produces
combat that feels broken when layered with FC's penalty curve.

**Implementation target:**
- Mixin class: `Forge/src/main/java/btw/forge/mixin/AttackCooldownMixin.java`
- Target method: `net.minecraft.world.entity.player.Player#getAttackStrengthScale(float)`
- Injection: `@Inject(method = "getAttackStrengthScale", at = @At("HEAD"), cancellable = true)`
- Logic: if main-hand item is instance of the FC classes above, return
  `1.0F` via `CallbackInfoReturnable.setReturnValue`.

**Alt rejected:** widening the cooldown window (option b). Rejected —
FC's exhaustion system already models recovery; two rate-limiters
stacked causes numeric drift nobody can reason about.

**Non-FC weapons** (wooden stick, netherite sword from other mods, etc.)
keep vanilla cooldown. This preserves Protocol #5 — vanilla weapons
still feel vanilla.

## 3. Shields — new FC item, not free

**New item** `btw.modern.FCItemShield` (registered via FC item list,
not Forge `DeferredRegister`; follows FC item-registration pattern).

| Property | Value | Rationale |
|---|---|---|
| Material tier | IRON-equivalent (500 uses baseline) | Usable mid-game, not top-tier |
| Effective durability | 80 block-uses | Each absorbed hit consumes 6.25 durability points (500 / 80) |
| Block amount | 40% of incoming damage (rounded down, minimum 1) | Half of what a full iron armor set absorbs; forces layered defense |
| Activation | Right-click hold; only if off-hand slot integration (`#021`) is live | Depends on off-hand routing |
| Block window | No vanilla 1.9 "perfect block" bonus | Simpler; matches FC's "numbers, not reflexes" combat feel |
| Recipe (stoked cauldron) | 3 tanned leather + 1 iron ingot → 1 `FCItemShield` | Uses existing cauldron crafting chain |

**Why 40%:** diamond armor (FC's top armor) absorbs 5.0 points/4 slots.
Shield at 40% flat is always worse than diamond on full-damage hits
but always useful on partial-armor setups — it becomes a gap-closer,
not a replacement.

**Why not soulforged-tier:** FC has no soulforged armor (confirmed at
`EnumArmorMaterial.java:24-29`); introducing a soulforged shield would
tacitly propose a soulforged armor tier, which is out of scope.

**Implementation files:**
- `Forge/src/main/java/btw/forge/combat/FCItemShield.java` (new)
- Registration entry point: follow `BTWRegistration` item-list pattern
- Recipe entry point: `FCCraftingManagerCauldron` stoked recipes

## 4. Sweeping edge (1.9+) — **disable (radius 0)**

**Decision:** sweeping-edge enchantment effect is suppressed; the enchant
still appears in loot/trades (Protocol #5) but does nothing.

**Reason:** FC has no area-of-effect melee concept. Sweeping edge hits
multiple entities per swing, turning mob packs into a free harvest;
incompatible with FC's per-hit exhaustion cost.

**Implementation target:**
- Mixin class: `Forge/src/main/java/btw/forge/mixin/SweepingEdgeMixin.java`
- Target: `Player#attack(Entity)` sweeping-edge block
  (search for `sweeping_edge` / `SWEEPING_EDGE` usages in
   `net.minecraft.world.entity.player.Player#attack`).
- Injection: `@Redirect` on the `EnchantmentHelper.getSweepingDamageRatio`
  call — redirect to `() -> 0.0F` so the sweep damage vector is zero.

**Verification:** with Sweeping III netherite sword, swing at sheep pack
— only directly-targeted sheep takes damage.

## 5. Crossbow piercing (1.14+) — **cap at 1**

**Decision:** any Piercing amplifier ≥ 1 is clamped to 1.

**Reason:** FC's `FCItemArrowBroadhead` is the designed-in armor bypass.
Piercing III+ fires through walls of mobs, trivializing FC's spider
swarms and zombie hordes.

**Implementation target:**
- Mixin class: `Forge/src/main/java/btw/forge/mixin/CrossbowPiercingMixin.java`
- Target: `net.minecraft.world.item.CrossbowItem#performShooting` or
  `#shootProjectile` (whichever actually reads piercing level)
- Injection: `@ModifyVariable` or `@Redirect` on
  `EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PIERCING, ...)`
  — clamp return to `Math.min(1, vanillaLevel)`.

## 6. Totem of Undying (1.11+) — **require and consume soul flux**

**Decision:** totem only activates if the player inventory contains at
least one `btw.modern.FCItemSoulFlux`; activation consumes one soul flux
in addition to the totem itself.

**Reason:** HC Spawn is FC's core death penalty. A free-save item
directly defeats it. Soul flux is a late-game alchemy output
(see `docs/audit/alchemy.md`); requiring it makes the save occasional,
not routine.

**Implementation target:**
- Mixin class: `Forge/src/main/java/btw/forge/mixin/TotemOfUndyingMixin.java`
- Target: `net.minecraft.world.entity.LivingEntity#checkTotemDeathProtection(DamageSource)`
- Injection: `@Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true)`
- Logic:
  1. If entity is not a player → pass through to vanilla.
  2. If player has no `FCItemSoulFlux` in inventory → return `false`
     (totem does nothing; player dies).
  3. Else → consume 1 soul flux from inventory via
     `inventory.clearOrCountMatchingItems`, then allow vanilla flow.

## 7. Trident (1.13+) — **damage-clamp + drowned loot nerf**

**Decision:**
- Trident melee damage clamped to `4 + EMERALD.damageVsEntity = 7` (matches diamond sword).
- Trident thrown damage clamped to 7 + velocity bonus (unchanged).
- Drowned drop rate for trident divided by 5 via loot-table override
  (Phase 2 data — `#004`).

**Reason:** vanilla trident throws for 9 damage, melees for 9 —
**higher than FC's top-tier soulforged sword (8)**. That's a strict
violation of the tier ladder. Pulling trident down to emerald-tier
keeps it as a situational ranged weapon (its actual niche), not an
overall-better soulforged alternative.

**Implementation targets:**
- Mixin: `Forge/src/main/java/btw/forge/mixin/TridentItemMixin.java`
- Target: `net.minecraft.world.item.TridentItem#getAttackDamage` (access
  transformer may be required since field is final) — clamp to 7.
- Target: `net.minecraft.world.entity.projectile.ThrownTrident#onHitEntity` — clamp `f` local to 7 + velocity bonus via
  `@ModifyVariable`.
- Loot table override file (Phase 2): `data/minecraft/loot_tables/entities/drowned.json` with trident entry weight / 5.

## 8. Tipped / lingering potions (1.9+) — **brewing-recipe restrict**

**Decision:** lingering potions craftable only at an **FC potion stand**
(not vanilla brewing stand). Vanilla brewing stand produces normal +
splash potions only.

**Reason:** lingering creates persistent AoE zones — trivializes mob
farms and escape mechanics. Gating behind FC's potion stand means the
player must opt into FC's alchemy chain.

**Implementation:**
- Pure recipe override (Phase 2 data). No mixin.
- Override the vanilla brewing-stand recipe for `dragon_breath + splash_potion → lingering_potion` to require an FC-registered ingredient
  (e.g., blasting oil) that vanilla brewing stands refuse.

Tipped arrows remain craftable — they're consistent with FC's
`FCItemArrowBroadhead` item and don't break balance at FC-normalized
damage numbers.

---

## Test matrix (Phase 4 verification)

| Scenario | Expected |
|---|---|
| Hold soulforged sword, spam-click sheep | All hits land at base 8 dmg; exhaustion penalty decays damage |
| Hold vanilla diamond sword (other mod), rapid-click | Vanilla 1.9 cooldown applies; reduced damage on fast clicks |
| Sweeping III netherite sword swing at 3-sheep clump | Only front sheep takes damage |
| Crossbow w/ Piercing V vs 4 zombies | First zombie hit, rest unaffected |
| Die w/ totem only | Player dies (no activation) |
| Die w/ totem + soul flux | Player saved, 1 soul flux consumed |
| Trident melee hit | Damage = 7 |
| Trident thrown + Riptide | Damage = 7 + velocity |
| Kill 100 drowned, count tridents | Expect ~1× the vanilla rate ÷ 5 |
| Vanilla brew stand: dragon breath + splash | No recipe match |
| FC potion stand: dragon breath + splash + blasting oil | Lingering potion produced |

## Files touched by Phase 4

| File | Change |
|---|---|
| `Forge/src/main/java/btw/forge/mixin/AttackCooldownMixin.java` | new |
| `Forge/src/main/java/btw/forge/mixin/SweepingEdgeMixin.java` | new |
| `Forge/src/main/java/btw/forge/mixin/CrossbowPiercingMixin.java` | new |
| `Forge/src/main/java/btw/forge/mixin/TotemOfUndyingMixin.java` | new |
| `Forge/src/main/java/btw/forge/mixin/TridentItemMixin.java` | new |
| `Forge/src/main/java/btw/forge/combat/FCItemShield.java` | new |
| `Forge/src/main/java/btw/forge/combat/FCOffhandAdapter.java` | new (from `#021`) |
| `Forge/src/main/resources/betterthanwolves.mixins.json` | add 5 mixin entries |
| `Forge/src/main/resources/data/minecraft/loot_tables/entities/drowned.json` | trident weight ÷ 5 (Phase 2) |
| `Forge/src/main/resources/data/minecraft/recipes/...lingering...` | block vanilla, add FC variant (Phase 2) |

## Protocol tags

Each mixin header carries `// FCMOD-INTEGRATION(1.20.1): <feature>: <one-line why>`
per Protocol #1. Each new FC-source line (none expected here — combat
touches Forge/mixin code only) would carry the same tag.
