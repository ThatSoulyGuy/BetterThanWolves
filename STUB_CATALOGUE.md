# Comprehensive Stub & No-Op Catalogue

Originally audited 2026-03-28. Re-audited 2026-06-05 (see below). Link-audited
2026-07-08 (see below). Every remaining stub, no-op, empty body, hardcoded
return, and TODO across the btw.modern and btw.forge layers.

**FC code is read-only. Never modify it** (user directive, 2026-10-10). FC code is FlowerChild's
BTW sources (`net.minecraft.src.btw.*` under `Common/`, `Client/`, `Server/`), the 1.5.2 vanilla
sources with FC's FCMOD edits under `vanilla/`, the frozen compiled classes in root
`build/classes/java/{main,server}`, FlowerChild's original 4.B release files kept in the repo
(`Src/`, `SrcServer/` and `SrcClient/`, holding the flat sources and the `patch.txt` files that
`setupVanilla` applies to build `vanilla/`, plus `BTWMod4-B0000003.zip`), and FlowerChild's own
files such as `Readme.txt`. No entry in this catalogue is ever closed by editing them, not even with
a small tagged fix. Fixes go to the
Forge bridge (`btw.forge`, mixins on 1.20.1 classes, data packs), to Modern-Common (the `btw.modern`
stand-ins, which are port code, not FC code), or to the planned subproject (name not chosen yet)
that extends FC code and patches it at build or load time without writing back to FC files. The
build-time ASM transforms in Forge/build.gradle (widenFcHookDescriptors, openUpEntityAccess,
extendFcPathingBuildRange) are its precursors and are expected to move into it. Older entries that
describe or suggest edits to FC code predate the rule and carry a dated note; the entries themselves
are left as written.

2026-10-10: CurseForge-feedback fix pass, entries (w)-(am) at the end of this file. They describe
changes on top of 1.3.0-alpha.3 (git 48e9825), committed on 2026-10-10 as f6ec9be ("Fixed several major
bugs"; d55db5d then set portVersion 1.4.0-alpha.5), i.e. Unreleased. Finding ids
(R1, D1, GH1, HAI-1, SW1, RE1, CTX-5, ...) come from docs/curseforge-feedback-triage.md; design ids
(B1, U1, E1, ...) come from the earlier bug-design pass the triage cross-references; review ids
(AP-1, RP-2, FD-R1, ...) come from the per-wave code reviews. Verification for every one of these
entries is limited to: `:Forge:jar` green after each of the five implementation waves; LinkAudit 0
missing classes, missing members 8 -> 6 (the 6 are pre-existing and on inert paths), 0 foreign
net.minecraft refs, jar precedence drift 0; BridgeAudit 0 high-severity; and a dedicated-server smoke
test (dev runServer, fresh throwaway world, run twice) that boots to Done with 0 ERROR lines, completes
FC init (148 blocks / 98 items replaced, 666 crafting + 23 smelting FC recipes injected), converts
worldgen pigs/sheep/cows/chickens (and, in the second run only, structure villagers), and activates
the modern-block stand-ins.
NOT run: any client (every rendering, animation, GUI and HUD fix is checked by code review only),
multiplayer, and a clean server shutdown / session.lock release (the harness could not send 'stop').
The "in game" steps listed in each entry are the intended checks, not results.

2026-10-11: bugs the maintainer found in play (mechanical power, sounds, the BTW creative tab), entries
(an)-(aq) at the end of this file. UNCOMMITTED working-tree changes on top of d55db5d. Method: an
audit-and-prove pass first (four audits, each checked by an independent verifier, and a dedicated-server
prover that ran mechanical scenarios D1-D11 twice), then one implementation wave (W6, eight workstreams,
each reviewed and its findings fixed, deferred or rejected), then orchestrator follow-ups. Audit ids:
MPR-1..6 (mechanical placement and render), F1..F4 (mechanical logic), S1..S11 (sounds), CT1..CT6 (creative
tab), MISSED-1..4 (creative-tab verifier); review ids RP-, RI-, WP-, CW-, PB-, AS-, CI-, EI-. The rule
throughout: port the 1.5.2 body of a primitive, never re-derive it from its name, and fix the shared
primitive rather than single blocks. Verification: `:Forge:jar` green (JDK 17); LinkAudit unchanged from W5
(0 missing classes, the 6 baseline missing members, 0 foreign refs, jar precedence 80 collisions / 0
drift); BridgeAudit 0 high-severity (sound coverage 72 used / 136 mapped / 0 unmapped, aux-FX 64 defined /
64 handled, textures 0 missing); a dedicated server boots a fresh world with 0 ERROR lines and registers 508
legacy sound events. Client: the maintainer's dev-client sessions (Forge/run/client/logs, nine on
2026-10-10, each joining a world) show the pre-W6 state the audit reproduced ('Added 174 blocks and 253
items to BTW creative tab'); the last session (2026-10-11 09:40-09:49) ran a mid-wave build (514 sound
events, before the review fixes) and logs the BTW tab built with 120 block and 235 item entries and the
gear box, clutch, saw, bellows and axles live-captured. None of the in-game checks in (an)-(aq) has been
run and recorded on the final code: every rendering, client sound, creative-tab and JEI change is
verified by code review and small harnesses only.

2026-10-28: round 3, the remaining items of the CurseForge report of 2026-10-19
(docs/curseforge-feedback-triage-2026-10-19.md) and the bugs found with them: entries (bg)-(bx) at the end of this
file, the RANB-PARITY paragraph in section 3, and dated notes on the older entries they correct. UNCOMMITTED
working-tree changes on top of 4558220, landed in ten waves, W1-W10 (W2 with a fix-up; W10 completed on 2026-11-03
with its solid-top fix and the support units it led to). Method: every unit was audited and proven read-only first,
with proof plans whose GAP, CONTROL and SANITY rows were run at HEAD, then designed, adversarially reviewed,
implemented and reviewed again, and landed with each file's line endings kept. Each wave was gated by the build (Mixin
config OK, 79 mixins at the end of W10; Dev class precedence OK), LinkAudit at its baseline of missing members (6
through W9; 5 from W10, where (bv)'s icon members resolve SlotArmor's `ItemArmor.func_94602_b`), BridgeAudit 0
high-severity, the wave's plans at `after` and the regression sets. The maintainer's rulings of 2026-10-21 to
2026-11-02 decided the open questions; each entry names its own. Verification is the proof record
fu_runs/BASELINE_RUNS_FU.md, sections "Round 3, W1" to "W10 guard batch". W10 (the units found in round 3) first
passed every plan except the redstone torch's rows on top of glowstone and ice (P-FOUND-RT 20/24, P-RANB-PLACE
115/117). Its gate held until FC's World answered 4.B's solid top ((bw), 2026-10-31: P-FOUND-RT 24/24, P-RANB-PLACE
117/117) and the units that followed from it landed: 1.20.1's snow, pressure plates and rails under the same support
veto ((bs)), and modern fences, walls, stained panes and plates answering as their BTW relatives, placed tools
included ((bx)). Gate W10 passed on 2026-11-03 with its guard batch (P-FOUND-RT 24/24, P-RANB-PLACE 117/117,
P-FOUND-RS 80/80, P-FOUND-CARTS 80/80, P-SUPPORT-VETO 106/106, P-SUPPORT-REL 100/100, P-TOOLSTICK-REL 38/38, the
client plans P-SUPPORT-REL-C 17/17 and P-RANB-PLACE-C 13/13, every other guard the same as its last record). Two units
fill no stub and have no entry of their own: campfire-lit Nether portals through 1.20.1's frame rule (INTEGRATIONS
#058, noted in (br)) and the block swap lift (#060, noted in (bi)).

## 2026-07-09 (e) Movement penalty vanishes while running/jumping

Symptom: the FC land-movement debuff (hunger/fat/low-health/gloom) intermittently
disappears — the player suddenly moves at vanilla speed, especially while running
and jumping.

Root cause: the local player's movement is **client-authoritative**. `LocalPlayer`
computes its own `getSpeed()`, moves itself, and reports the position to the server.
`LivingEntityMixin.btw$applyMovementPenalty` only applied the FC modifier for
`ServerPlayer`, so the client kept predicting vanilla speed; the server merely
*corrected* over-fast movement via its anti-move tolerance — which is looser for
airborne/jumping motion, so the debuff "leaked away" exactly when running and jumping.

Fix (bridged): extracted the modifier formula into a shared static
`btw.modern.EntityPlayer.computeHealthAndExhaustionModifier(maxPenaltyLevel, gloomLevel)`
(the two instance getters now delegate to it, so there is one source of truth). Added
`btw.forge.mixin.ClientPlayerSpeedMixin` — registered in the mixin config's `"client"`
array so `LocalPlayer` is never referenced on a dedicated server — which applies the
same modifier to the local player's `getSpeed()` using the penalty levels already
synced each tick by `BTWNetwork.PenaltySync` (`clientHealthPenalty`/`clientHungerPenalty`/
`clientFatPenalty`/`clientGloomLevel`). Client prediction and server correction now
agree, so the debuff is felt consistently.

2026-10-10 correction (design B4): ClientPlayerSpeedMixin and the LivingEntityMixin hook both
targeted `LivingEntity.getSpeed`, which `Player.getSpeed` overrides without calling super, so
neither ever ran for a player. The penalty players felt came only from the server's
MOVEMENT_SPEED attribute modifier, and sprint-jumping shed it because `getFlyingSpeed` never saw
the penalty. Both hooks are deleted; see 2026-10-10 (y).

## 2026-07-09 (k) Automated audit tooling (catch bugs before gameplay)

Started a comprehensive audit suite so the "compiles + links fine, silently wrong at runtime"
classes we kept finding in game get caught automatically. Three tiers:

1. `tools/LinkAudit.java` (existing) — bytecode-static: member/class resolution + shadow
   winner. Catches NoSuchMethodError / NoClassDefFoundError families.
2. `tools/BridgeAudit.java` (NEW) — source-static, runnable standalone (javac + java, exit 1
   on high-severity). Checks:
   - SOUND COVERAGE: FC sound-name literals vs SoundMapping -> unmapped = silent.
   - AUX-FX COVERAGE: FC playAuxSFX ids (>=2222) vs WorldBridge.playFcAuxSFX switch ->
     unhandled = silent.
   - TEXTURE EXISTENCE: renderer loadTexture paths + FC item icon names, resolved via the
     FCEntityRenderer rules, vs shipped resource files -> missing = placeholder/invisible.
3. `btw.forge.BridgeSelfAudit` + `FCEntityRenderer.auditRendererCoverage()` (NEW) — init-time,
   logs `[SELF-AUDIT/...]` at launch:
   - RENDERER COVERAGE (client): every registered FC entity resolves to a real renderer, not
     the debug box (the fc_xp_orb/arrow invisibility class).
   - BLOCK SPEED-FACTOR (common): no block's getSpeedFactor > 1.0 (the sprint-"flying" class —
     a >1 factor compounds into airborne momentum). Regression guard for 2026-07-09 (j).

Backlog surfaced by the first BridgeAudit run — CLEARED 2026-07-09 (l) below.

## 2026-07-09 (m) Stoked-fire (Hibachi) custom flame — authentic runtime procedural fire

The stoked fire over a stoked Hibachi already rendered its custom flame geometry (FCBakedModel
captures FCBlockFireStoked.RenderBlock for every ProxyBlock) with its authentic texture (the
block atlas force-stitches all block/ textures, so resolveTexture step 1 finds
betterthanwolves:block/fcblockfirestokedstub_0 before the soul_fire fallback). But FC's flame is
procedurally ANIMATED (FCClientAnimationFire) and only two static 16x16 frames ship, so the
bridge showed a dead, non-flickering flame.

First pass baked the two frames into a 2-frame mcmeta strip — reverted. Now done FC-authentic:
btw.forge.client.StokedFireTexture ports FCClientAnimationFire's cellular fire simulation (random
seeded bottom row drifting upward with weighted decay; white-hot core -> orange-edge color ramp)
and, each client tick, re-uploads the generated 16x16 RGBA frame straight onto the block atlas at
the stoked-fire sprite (RenderSystem.bindTexture + NativeImage.upload) — exactly what FC's 1.5.2
TextureFX did. Two independent sims drive the two checkerboard variants; the stoked variant reads
the hotter lower half of the 16x32 field. Registered via @Mod.EventBusSubscriber (forge bus,
Dist.CLIENT); self-disables on any failure. Caveat: only atlas mip 0 is re-uploaded (distant
flame falls back to the static stub mips). Untested visually (no launch here) — needs an in-game
look at a stoked Hibachi.

## 2026-07-09 (l) Clear the BridgeAudit backlog (silent effects/sounds, fish texture)

- Fish bobber texture: RenderFish now loadTexture("/gui/particles.png") — the FC particles
  atlas ships at textures/gui/particles.png (was "/particles.png" -> unshipped -> placeholder).
- 6 unmapped sounds added to SoundMapping (were silent): mob.cow.say2/say4 -> COW_AMBIENT,
  "mob.ghast.affectionate scream" -> GHAST_AMBIENT, mob.wolf.howl -> WOLF_HOWL,
  mob.zombiepig.zpigangry -> ZOMBIFIED_PIGLIN_ANGRY, random.glass -> GLASS_BREAK. Plus
  minecart.base -> MINECART_INSIDE (used by the saw aux-FX).
- 35 of 37 unhandled aux-FX ids ported into WorldBridge.playFcAuxSFX (sounds transcribed
  exactly from FC ClientPlayCustomAuxFX, incl. ghast scream/moan, soul-urn shatter, saw
  damage, mining-charge/log-smouldering explosions, hopper XP eject, animal births/milking,
  golem/wither creation, all possession transforms). Dynamic block-break sounds
  (ender-block collect/convert/place, respect-particle destroy) use a new playFcBlockSound
  helper reading block.stepSound. Sound side only.
  Remaining (BridgeAudit still flags 2): 2241 dispenser-smoke and 2282 cactus-explode are
  PARTICLE-ONLY in FC (no sound) — they need the aux-FX particle path, not a sound. That
  (client-side particle spawning for aux-FX) is the next follow-up; the cleaner long-term
  option is wiring FC's ClientPlayCustomAuxFX client-side to get sounds+particles for all 64.
  Post-fix BridgeAudit: 0 unmapped sounds, 0 missing renderer textures, 2 particle-only
  aux-FX remaining. LinkAudit unchanged.

## 2026-07-09 (j) Speed buffs / "flying" when sprinting

Symptom: random speed buffs while walking (on some blocks); sprinting → practically flying.

Root cause (verified via decompiled 1.20.1 sources + tick simulation, two adversarial
passes): BTW's `Block.GetMovementModifier` (Modern-Common Block.java:894) returns 1.2 for
every non-soil/non-grass block ("20% hard-surface bonus"), 1.0 otherwise — never < 1. The
bridge routed it through `getSpeedFactor` (BlockMixin.btw$getSpeedFactor + ProxyBlock.
getSpeedFactor, a single application — the old ×1.44 double via getBlockSpeedFactor was
already removed). But MC 1.20.1 multiplies `getSpeedFactor` into carried `deltaMovement`
EVERY tick in `Entity.move()`, so a factor > 1.0 compounds into momentum:
  - Ground: bounded (0.6·0.91·1.2 = 0.655 < 1) → ~+32% on hard blocks = the "random" walking
    buffs (random = only on non-soil blocks).
  - AIRBORNE (sprint-jump / descending stairs & slopes): retention flips to 0.91·1.2 = 1.092
    > 1 → geometric momentum GROWTH, unbounded while airborne over a hard block → "flying".
FC applied the modifier to the INPUT speed instead (EntityLiving.moveEntityWithHeading:3369
`fMoveSpeed *= GetMovementModifier`), which is bounded and never enters airborne momentum
(airborne horizontal accel uses getFlyingSpeed, not getSpeed).

Fix: apply the bonus the FC way — to `getSpeed()`, not `getSpeedFactor()`.
- Removed the `* GetMovementModifier` from BlockMixin.getSpeedFactor (hook deleted) and
  ProxyBlock.getSpeedFactor (override deleted) so getSpeedFactor stays the vanilla value.
- New `btw.forge.FCMovementBonus.getBlockBelowSpeedBonus(LivingEntity)`: on-ground only,
  keyed on the block below the feet, returns its GetMovementModifier (1.0/1.2).
- Multiply getSpeed by it in BOTH hooks — LivingEntityMixin (ServerPlayer) and
  ClientPlayerSpeedMixin (LocalPlayer), OUTSIDE the `< 1.0` penalty guard, composed with the
  FC penalty — so client-authoritative prediction matches the server (no rubber-band).
Bounded (+20% top speed), airborne-safe, player-only (FC's AI mobs ignore the boost). Soul
sand/honey keep slowing via their own getSpeedFactor/getFriction (untouched); this also
removes the prior 0.4→0.48 soul-sand inflation.

2026-10-10 correction (design B4): the getSpeed half of this fix never ran for players. Both hooks
targeted `LivingEntity.getSpeed`, which `Player.getSpeed` overrides without calling super, so the
block bonus never reached players. It is now applied by `PlayerMixin.btw$applyBlockMovementModifier`
on `Player.getSpeed`, on both sides; see 2026-10-10 (y). The getSpeedFactor removal above stands.

## 2026-07-09 (i) Soul urns render with placeholder texture

Symptom: soul urns (FCEntityUrn) render as a billboard but with the wrong/placeholder
texture.

Root cause: FCClientRenderUrn draws `Item.itemsList[m_iItemShiftedIndex].itemIcon` — an
item-icon billboard. The itemIcon is a `NamedIcon` whose UV getters record the bare FC
icon name (soul urn = "fcitemurnsoul") on the Tessellator and return 0..1 UVs. The entity
capture pipeline's `resolveEntityTexture` then mapped that bare name to
`betterthanwolves:textures/fcitemurnsoul` — no `item/` subdir, no `.png` — which doesn't
exist, so it fell back to the placeholder. (The item texture ships at
`textures/item/fcitemurnsoul.png`.)

Fix: `resolveEntityTexture` now detects a bare Icon name (no `/`, no `.png`) and maps it to
`textures/item/<name>.png` (or `textures/block/<name>.png` for `fcBlock*` icons), with the
0..1 UVs mapping the whole 16x16 sprite. Also benefits any other item-icon billboard
(thrown snowball/egg) whose item has a populated NamedIcon. Full-path textures (mob/,
item/…png, btwmodtex/) are unaffected — only previously-broken bare names change.

## 2026-07-09 (g) Frozen-vanilla entities don't render (fc_xp_orb, arrows, ...)

Symptom: some FC entities never render, e.g. `fc_xp_orb` (XP orbs invisible).

Root cause: `FCEntityRenderer` (the generic capture renderer) needs an FC `Render`
instance per entity class, looked up from `RenderManager.getEntityRenderMap()`. That map
is filled by (a) the `RenderManager` shim ctor and (b) FC's `ClientAddEntityRenderers`.
In real 1.5.2 the base `RenderManager` ctor registered ~50 vanilla renderers, but the
Modern-Common shim ctor only registered `RenderItem`; FC's `ClientAddEntityRenderers` only
covers FC entity *subclasses* (FCEntityPig→RenderPig, ...). So **frozen vanilla entities
that FC doesn't subclass have no renderer** and fall back to the debug box. Affected (all
registered via `registerPlainEntity`): `fc_xp_orb` (EntityXPOrb), `fc_arrow` (EntityArrow),
`fc_snowball` (EntitySnowball), `fc_tnt_primed` (EntityTNTPrimed), `fc_egg` (EntityEgg),
`fc_fish_hook` (EntityFishHook). The projectile/orb renderers were never ported to
Modern-Common (unlike the mob renderers) and aren't relocated, so `btw.modern.RenderXPOrb`
etc. didn't exist at runtime.

Fix (fc_xp_orb): ported `RenderXPOrb` to Modern-Common (billboard quad; drops the
lightmap/GL-state calls per the RenderFallingSand port convention; texture `/item/xporb.png`
recorded via `Render.loadTexture` for the capture pipeline, resolves to the shipped
`betterthanwolves:textures/item/xporb.png`). Added the `xpColor` field + `getTextureByXP()`
to the Modern-Common `EntityXPOrb` compile-stub (the frozen class wins at runtime and
provides the live values; the stub only needs the signatures so RenderXPOrb links).
Registered it in the `RenderManager` shim ctor next to `RenderItem`.

Remaining frozen-vanilla renderers — DONE (2026-07-09 (h) below).

## 2026-07-09 (h) Remaining frozen-vanilla entity renderers ported

Ported the rest of the missing vanilla renderers to Modern-Common and registered them via
`FCEntityRenderer.registerMissingVanillaRenderers` (reflection on the entity class, so no
Modern-Common stub is required — EntitySnowball has none):

- `RenderArrow` (fc_arrow) — 6-quad model, texture `/item/arrows.png` (shipped) -> renders
  with the correct texture.
- `RenderTNTPrimed` (fc_tnt_primed) — TNT block via `RenderBlocks.renderBlockAsItem`
  (same capture path as RenderFallingSand); swells as the fuse runs; the blended
  fuse-blink overlay is omitted.
- `RenderSnowball` (fc_snowball + fc_egg) — item-icon billboard, mirrors RenderItem's
  icon path (loadTexture "/gui/items.png" + item Icon UVs).
- `RenderFish` (fc_fish_hook) — bobber billboard from `/particles.png`; the fishing LINE
  (angler->hook) is omitted (needs client Minecraft.thePlayer + line-mode capture the
  pipeline doesn't support).

Geometry verified (compile + LinkAudit: 0 missing classes, 11 documented members). Texture
status differs by entity because the entity capture pipeline (FCEntityRenderer) binds ONE
path-resolved texture per entity (first quad's name -> betterthanwolves:textures/<name>):
arrow is confirmed correct (dedicated shipped texture); TNT/snowball/egg go through the
block/item icon-name path (like FallingSand/tile-items) and the fish bobber's
`/particles.png` is not shipped, so those may show a placeholder texture pending an in-game
check. Making those correct is a texture-layer follow-up (ship the FC atlas/particle
textures and/or teach the entity pipeline to resolve atlas icons), not a geometry issue.

Billboard note: RenderSnowball/RenderFish/RenderXPOrb use RenderManager.playerViewY/X,
which the capture pipeline leaves at 0, so they face a fixed direction rather than the
camera.

## 2026-07-09 (f) "Random ghast_hurt in a fresh world" — instrumentation

Symptom: in a fresh world (nothing built), `entity.ghast.hurt` plays at random.

Trace: `ghast_hurt` <- `mob.ghast.scream` (SoundMapping). No FC entity/mob uses it as a
sound; the block emitters need structures. The real source is BTW's possessed-squid ->
overworld-ghast mechanic: a fully-possessed squid leaps and has a 25% chance to convert
into an `FCEntityGhast` (`FCEntitySquid:686`); that overworld ghast idle-moans but
screams `mob.ghast.scream` (`GHAST_HURT`) when hurt (`EntityGhast.getHurtSound`).

Key gate: possession never self-seeds in the overworld — `EntityCreature.HandlePossession:494`
only spontaneously possesses when `provider.dimensionId == -1` (Nether). The overworld
needs a seed (a possessed creature arriving/dying, a portal). The bridge's dimension
wiring is correct (`WorldBridge:87-104`, overworld=0), so static analysis can't explain a
fresh, never-Nether world producing ghasts — it needs a runtime trace.

Instrumentation added (bridge-layer, diagnostic only — NOT a behavior change):
`PossessionDiagnostics.poll(this, fcEntity)` after `onUpdate()` in ProxyMob/ProxyAnimal/
ProxyPathfinderMob reads `GetPossessionLevel()` reflectively (shadowed frozen member) and
logs every level transition with dimension + position (`[BTW-Possession]`). `WorldBridge.
spawnEntityInWorld` now logs the dimension and flags `[GHAST-SPAWN]` for each FCEntityGhast.
A `0->1` possession or `[GHAST-SPAWN]` in `dim=minecraft:overworld` in a never-Nether world
= confirmed over-seeding bug; the log then localizes where it seeded. (Earlier guess that
the noises were the Infernal Enchanter's `GHAST_SHOOT` "whoosh" applies only to worlds that
have one placed — a different sound from the fresh-world `GHAST_HURT`.)

2026-10-10 correction (triage GH4, GH1): the possessed-squid -> ghast theory (c449199) cannot
explain a fresh world. Spontaneous possession requires `dimensionId == -1`, WorldBridge sets that
only for `Level.NETHER`, and no instrumented log ever showed a `[BTW-Possession]` or `[GHAST-SPAWN]`
line. If the sound was identified by ear, it was almost certainly GH1: the 1.5.2 bat take-off
(aux-SFX 1015) was passed raw to 1.20.1 `levelEvent`, where 1015 is GHAST_WARN (volume 10, subtitle
"Ghast shoots"), and every naturally spawned bat fired it on its first AI tick. Fixed by
AuxSfxMapping in 2026-10-10 (ag); the witch idle moan (GH2) is in (ah). Because (ag) also wires FC's
nether-portal hook (GH5), overworld possession near player-built portals is now reachable, so
`[BTW-Possession]`/`[GHAST-SPAWN]` lines there are expected. Both diagnostics stay until GH1 is
confirmed in game.

## 2026-07-09 Double-application sweep (movement "super fast" bug)

Symptom: player moves too fast. Root cause: DOUBLE application of FC's block
GetMovementModifier. Vanilla `Entity.getBlockSpeedFactor()` already calls
`Block.getSpeedFactor()` on the feet/below blocks — which BlockMixin (vanilla-
with-FC-counterpart) and ProxyBlock (FC blocks) multiply by GetMovementModifier
(×1.2 on hard surfaces). `LivingEntityMixin.btw$getBlockSpeedFactor` then
multiplied the RETURNED value by GetMovementModifier AGAIN → ×1.44. The
soul-sand review fix (replace→multiply) is what turned it from single into
double (the old replace discarded the block-level contribution). FIX: removed
the redundant LivingEntityMixin hook; the block-level getSpeedFactor is the
single, correct application point (matches vanilla's flow). Now ×1.2 single on
hard surfaces, ×0.48 on soul sand.

Swept the sibling FC property systems for the same block-vs-entity double
pattern: friction (block-level only — single), getSpeed/GetLandMovementModifier
(single, a different factor), mining getDestroyProgress (single). All clean.
FOUND a separate UNDER-application: PlayerBridge.pendingMeleeDamageModifier was
stored (PlayerMixin.attack@HEAD = GetMeleeDamageModifier()) but NEVER consumed —
FC melee damage scaling was a no-op.

## 2026-07-09 (d) — FC entity movement rubber-banding

Symptom: FC entities rubber-band when they move or turn. Root cause: generic
ProxyEntity.tick() ran the full FC simulation (syncToFc -> onUpdate -> syncFromFc)
and wrote position/rotation back via setPos/setYRot UNCONDITIONALLY — including on
the CLIENT. So the client re-simulated FC physics and overwrote the server-driven,
vanilla-interpolated (lerp) position every tick; the two fought = rubber-band. The
developer had even noticed the client re-sim (createDummyClientWorld comment about
entities "falling through the floor" client-side) and tried to fix it by making
client FC physics MATCH the server — impossible to do perfectly, hence the residual
banding. The mob proxies (ProxyMob/Animal/PathfinderMob) already gate this correctly;
only generic ProxyEntity (windmills, waterwheels, moving platforms, projectiles,
urns, mining charges — exactly the "turn or move" entities) didn't. Fix: gate the
position/rotation writeback (syncFromFc) + FC-state broadcast to server-only. onUpdate
still runs client-side for animation (windmill/waterwheel rotation counters); the
authoritative position/rotation is server-driven + lerp-interpolated. If mobs still
show subtle banding, the next suspects are the server-side NaN-sanitization snap-back
(ProxyMob) and the EntityType updateInterval.

## 2026-07-09 (c) — block orientation-on-placement wired (axle mesh bug)

Symptom: horizontal axle mesh looked scrambled. Root cause: ProxyBlock had NO
getStateForPlacement / onBlockPlaced wiring, so FC's onBlockPlaced — the 1.5.2
mechanism that encodes orientation into metadata from the clicked face — was
NEVER called. Every ProxyBlock was placed with META=0, so the axle always got
GetAxisAlignment=0 (vertical-axis geometry) regardless of how it was oriented
(log confirmed metaAfter=0 on every placement). Added ProxyBlock.getStateForPlacement
calling fc().onBlockPlaced(world, x,y,z, clickedFace(get3DDataValue == FC side),
hit, 0) and storing the result in META. FC side numbering (0/1=down/up, 2/3=N/S,
4/5=W/E) matches Direction.get3DDataValue exactly; onBlockPlaced defaults to
returning metadata unchanged, so non-orienting blocks stay META=0. This fixes
orientation for the axle AND every FC block that orients on placement (logs,
directional blocks). NOTE: the six shim renderFace methods were verified to emit
geometrically-correct vertices from the render bounds, so correct metadata =>
correct render. If any FC block's mesh still scrambles after this, the next
suspect is the GL11 software matrix not being isolated during FCBakedModel bake
(bakeForState doesn't reset/guard matrix tracking) — deferred (needs in-game repro).

## 2026-07-09 (b) — melee modifier wired + soul urn rendering

- **Melee damage modifier now applied**: PlayerMixin.btw$applyMeleeDamageModifier
  (@ModifyVariable STORE ordinal=0 on Player.attack's base ATTACK_DAMAGE float)
  multiplies base damage by pendingMeleeDamageModifier when < 0.99 — mirrors 1.5.2
  EntityPlayer.attack:1226 (`if (fModifier < 0.99F) var2 = (int)(var2 * fModifier)`,
  base damage pre-enchant). Refmap-verified attack -> m_5706_. FC weight/health/
  exhaustion now actually weakens hits.
  2026-10-10 correction (design B1): the `ordinal = 0` was the local-variable ordinal; the
  `@At("STORE")` had none, so the hook scaled all four stores of the damage float in
  `Player.attack` (m^3, m^4 on a crit), and `FCGameplayBridge.onLivingHurt` applied the modifier
  once more. Fixed in 2026-10-10 (y).
- **Soul urns render**: FCClientRenderUrn.doRender read Item.itemsList[id].itemIcon.
  getMinU() and NPE'd (0 quads) because itemIcon was null — FC registerIcons was only
  run for subtype items. BTWClientEvents now calls registerIcons(NamedIcon capturer) on
  EVERY FC item at model-register time, so itemIcon is non-null and the NamedIcon ->
  recording-Tessellator -> modern-sprite bridge draws it. (Any FC entity renderer that
  reads Item.itemIcon directly is now covered, not just the urn.)



**Remaining estimated stubs: ~400** — SUPERSEDED by the 2026-07-08 stub
elimination below: a 9-agent verified triage reduced the claim to **103 real
live stubs**, of which ~100 are now implemented. See that section for what
actually remains.

---

## 2026-07-08 Stub elimination (verified triage + implementation)

A 9-agent inventory re-triaged every claimed stub against the CURRENT runtime
winner rules, live caller chains, and the vanilla/ ground truth. Of 234
verdicts: **103 live stubs** (real gaps with named caller chains), 68
intentional bridge no-ops, 19 correct override points, 30 stale claims that
already work, 14 dead-shadowed. Headline stale claims killed: "PathFinder
returns null" (real A* has been live since the winner flip), "EntityLiving ~95
stubs" (shadowed class — dead code), "BlockFluid.getFlowDirection breaks water
wheels" (FCEntityWaterWheel has its own copy).

**Implemented (all faithful 1.5.2 ports, per-method source citations in code):**
fire spread (SetFireProperties now writes the BlockFire arrays FCBlockFire
reads; canNeighborBurn/canBlockCatchFire), water displacement
(GetPreventsFluidFlow — buckets no longer delete solid blocks), saw
(OnBlockSawed for all blocks, not just logs), grass/mycelium spread,
mortar/loose-snow stabilization, World physics/light/spawn-cap/ender-chest
methods (+ FcWorldSavedData persistence), Chunk methods, the full enchanting
chain (BlockEnchantmentTable.onBlockActivated + ContainerEnchantment +
buildEnchantmentList/calcItemStackEnchantability replacing invented logic),
thorns (func_92098_i/func_92099_a real), potions (ItemPotion.getEffects,
PotionHelper tables, EntityPotion.onImpact), EntitySenses.canSee, ItemFood
potion effects + wolf-food methods, ItemBlock dispenser/burn-time/buoyancy/
filter/piston-pack delegation, ItemStack.canHarvestBlock/splitStack,
Container.mergeItemStack (FCMOD bFavorHotbar) + slotClick + SlotCrafting
onCrafting, chest lid animation chain (TileEntityChest.updateEntity +
ContainerChest + block-event bridge WorldBridge.addBlockEvent ->
ProxyBlock.triggerEvent -> TileEntity.receiveClientEvent), furnace FCMOD
cook-time shifts, XP-orb pickup (xpCooldown + playerTouch + PlayerBridge
addExperience/onItemPickup), UpdateGloomState (+ WorldBridge.getLightBrightness
with skylightSubtracted, required for gloom to ever be FALSE),
IsCarryingBlastingOil/Detonate, proxy forwarding (interact, hurt->
attackEntityFrom, isPickable, two-way fire sync, damage-source attacker),
EntityTracker via EntityTrackerBridge, EntityList.getClassFromID + spawn-egg
383 mapping, falling-block rendering (RenderFallingSand + Block.
RenderFallingBlock), grass/rail/torch/anvil render methods, EntityPlayerMP
exhaustion/health-hunger updates + Api field-dedup (hardcore-spawn state).
*(2026-10-27: the falling-block rendering drew only the debug box until (bi): the client could not build
FCEntityFallingBlock.)*

2026-10-14 correction ((as)): "fire spread" above never applied to vanilla fire. SetFireProperties
does fill the BlockFire tables FCBlockFire reads, but FCBlockFire.updateTick never ran for
Blocks.FIRE: 1.20.1 FireBlock.tick overrides BlockBehaviour.tick without calling super, so
BlockBehaviorMixin's tick hook was never reached and 1.20.1 fire ran on its own tables. Only FC's
own fires (stoked fire, a ProxyBlock) used FC's rules. "grass/mycelium spread" was dead the same way
(SpreadingSnowyDirtBlock.randomTick, and the port's BlockGrass lacked the 1.5.2 random-tick flag).
FC fire ticks and grass and mycelium ticks are now routed by TickPolicy; see 2026-10-14 (as).

**Now DONE (2026-07-08 follow-up — review pass + remaining ports):**
- **Village subsystem** — Village/VillageDoorInfo/VillageCollection/VillageAgressor
  ported verbatim from 1.5.2; WorldBridge.ModSpecificTick drives villageCollectionObj.tick
  (village discovery from doors, reputation, mating). All 9 Village methods +
  VillageDoorInfo now resolve in LinkAudit. Deferred: cross-session NBT persistence of
  village data (in-memory rediscovery from doors each session — 1.5.2 rebuilds within
  seconds; only reputation + the 3600-tick breeding cooldown reset on restart) and
  VillageSiege (no LinkAudit-flagged caller).
  *(2026-10-28: VillageSiege: 4.B removed it (vanilla/server World.java:105-107, WorldServer.java:183-185), and
  1.20.1's siege does nothing since (bm) (`village_sieges`, VillageSiegeMixin).)*
- **RenderItem + ItemRenderer** — full port; registered for EntityItem in RenderManager.
  Placed tools / campfire food / furnace contents / wicker-basket items now render.
- **AchievementList crafting constants + SlotCrafting.onCrafting dispatch** — the 13
  constants and vanilla dispatch chain (inert: addStat is a bridge no-op, but faithful).

**Adversarial review fixes (9-agent pass, ~24 findings applied):** soul-sand speed
inversion (Forge speed hooks now COMPOSE FC's modifier with the modern factor instead of
replacing — was making soul sand faster than stone); Block.GetDoesFireDamageToEntities
5-arg delegation (fire/lava contact damage was dead); WeightedRandom dead stub (broke
enchanting); Potion heal/harm PotionHealth (isInstant); EntityPotion.onImpact splash;
getLightBrightness table lookup + skylightSubtracted ticked (gloom/Nether light);
proxy hurt/die guards (double-drop / resurrection on burn death); translateDamageSource
indirect (arrows were direct melee); Enchantment.func_92089_a → canApply; DamageSource
fcGloom → magic (armor bypass); FCContainerMenu openContainer reset; and more.

**Gap-patching pass (2026-07-09) — the last functional gaps closed:**
- **ItemFood modern-effect double-application FIXED** — LivingEntityMixin.btw$addEatEffect
  cancels the vanilla FoodProperties effect for any FC-counterpart food (refmap-verified
  addEatEffect → SRG m_21063_ for prod). No more double rotten-flesh-hunger / pufferfish-poison.
- **Spawn-egg itemDamage → entity-id FIXED** — ItemStackHelper.toFcStack sets the FC stack
  damage from the egg's EntityType via a modern-path → 1.5.2-EntityList-name table resolved
  through EntityList.stringToIDMapping, so EntityAgeable baby-spawn picks the right mob.
- **Village NBT persistence DONE** — FcWorldSavedData now round-trips villageCollectionObj
  through ForgeNBTCompound, so reputation + breeding cooldown survive restart (not just
  in-memory door rediscovery). Global (tier-3) ender chest also persisted.
- **Dead flat-FC graph + WatchableObject RESOLVED** — 17 relocate rules map the flat FC
  names the frozen artifact references to their restructured homes, plus a WatchableObject
  remap include. LinkAudit MISSING CLASSES now zero.
- **Item.record13/recordWait added** — a live fc_creeper killed by a live fc_skeleton drops
  a music disc; these were null (reachable NPE after the mob-replacement fixes). Mapped to
  legacy 2256/2267 → modern discs via ProxyRegistry.
- **REVERTED WorldBridge extends WorldServer** (the review's own recommendation): making it
  a WorldServer satisfied FCUtilsWorld's entity-tracker cast, but ALSO made
  `worldObj instanceof WorldServer` true in frozen Entity.onEntityUpdate's portal block,
  which then calls getMinecraftServer() (returns the modern net.minecraft.server type
  Modern-Common can't declare) EVERY TICK for every FC entity. Net-negative: traded a
  caught cosmetic gap for a per-tick caught crash. Back to extends World; the entity-tracker
  packet sync (cow-kick / squid-tentacle client visuals) stays a caught no-op — a KNOWN
  minor gap, the right trade. Lesson: a review recommendation can have second-order effects
  the reviewer missed; adversarially verify fixes, not just findings.
  *(2026-10-10: superseded by 2026-10-10 (x). WorldBridge extends btw.modern.WorldServer again,
  made safe by relocating net.minecraft.server.MinecraftServer to btw.modern.MinecraftServer (so
  getMinecraftServer() is declarable) and by returning false from getAllowNether(), which keeps the
  frozen portal block inert. The entity-tracker packet sync now reaches clients.)*

**Functionality pass (2026-07-09) — real bodies, not stubs, for every reachable member.**
Re-verified all 21 against ACTUAL class existence (the earlier inventory falsely claimed
several FC mobs "were never restructured" — they exist and are live). 10 had a genuine
1.5.2 body + a real (if gated) FC caller and got faithful implementations:
- BlockBed.isBlockHeadOfBed — REACHABLE: live FC ocelots (EntityAIOcelotSit) sit on beds.
- BlockSilverfish.getPosingIdByMetadata — REACHABLE: mutant silverfish from FCEntityCow.BirthMutant.
- Item.appleGold — REACHABLE: right-clicking a live FC zombie (EntityZombie.interact). id 322.
- BlockRailBase.isRailBlock/isRailBlockAt — dispensed FC minecart follows track.
- BlockSand.canFallBelow, Direction.getMovementDirection, ItemSword.func_82803_g,
  Chunk.getRandomWithSeed (+ World.getSeed / WorldBridge override), NBTTagCompound.getTags —
  real 1.5.2 logic, faithful bodies (some behind gated features; correct when the gate opens).

**Genuinely NOT patchable — 11 remaining LinkAudit members (no functionality to add):**
Each is a dead frozen-flat DUPLICATE (behavior already runs in the restructured path), or
SUPERSEDED by the modern engine, or ARCHITECTURALLY undeclarable. Adding bodies would either
change nothing (dead duplicate) or duplicate the modern engine (net-harmful — proven by the
WorldServer revert):
- Block.OnFallingUpdate(FCEntityFallingBlock), FCEntityCreeper.GetIsDeterminedToExplode/
  GetNeuteredState — flat frozen duplicates; the live restructured FCEntityFallingBlock/
  FCEntityCreeper already do this (shim has the OnFallingUpdate(EntityFallingSand) live overload).
- WorldServer.getMinecraftServer — returns the MODERN net.minecraft.server.MinecraftServer
  type, which Modern-Common structurally cannot declare; also portal-only (dead).
  *(2026-10-10: now declared and resolved via the MinecraftServer relocate, see (w)/(x).
  LinkAudit reports 6 missing members after the 2026-10-10 pass, down from 8.)*
- ServerConfigurationManager.transferEntityToWorld, WorldServer.resetUpdateEntityTick — FC
  dimension travel; the modern engine handles proxy dimension travel (portal path dead).
- TileEntityHopper.suckItemsIntoHopper/func_96114_a — hopper minecart; modern handles it and
  FCItemMinecart never dispenses a hopper cart.
- ItemArmor.func_94602_b — empty-armor-slot GUI icon; modern GUI owns slot backgrounds.
- ILogAgent.logSevere — frozen EntityItem.onUpdate log; FC items are converted to modern
  ItemEntity (never ticks); adding to the interface only churns implementors.
- FCClientRenderSpider.setSpiderEyeBrightness — FC multi-pass render; the modern engine
  renders the proxy spider (render passes not wired). *(2026-10-10: resolved by the RenderSpider
  port, see (ad).)*
- 68 intentional bridge no-ops (GUI display, GL state, client-only) — correct as designed.
- VillageSiege — a gameplay feature with no caller, not a gap. RenderItem enchant-glint —
  cosmetic, omitted with a comment.
  *(2026-10-28: VillageSiege: 4.B removed it; 1.20.1's siege is switched off the same way since (bm),
  VillageSiegeMixin.)*

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
- **Frozen artifact is now regenerable** (2026-07-08): root task `regenerateFrozenClasses`
  rebuilds `build/classes/java/{main,server}` from `vanilla/` via the MCP module's client +
  new server sourceSets (guarded: refuses to overwrite unless `-PforceRegenFrozen`). A full
  swap test passed LinkAudit with ZERO new findings and 30 fewer dead flat-FC references —
  the regenerated bytecode is post-restructure (flat FC classes gone, hooks pre-widened, the
  relocate/widen compensations become no-ops). NOT adopted yet: bytecode differs broadly from
  the frozen artifact, so switching deserves in-game verification first. Two discoveries:
  (a) `vanilla/` carried MANUAL post-setup edits (the FC-hook widening is NOT in patch.txt or
  applyFixups); (b) the server variant of that manual widening was missing an `(EntityLiving)`
  cast in `EntityAnimal` (never compiled until now) — fixed.
- **`vanilla/` is now fully reproducible** (2026-07-08): the 27 manually-edited files (FC hook
  widening, Minecraft/MinecraftServer hierarchy retargeting, AxisAlignedBB ray-trace hooks,
  ChunkProviderHell nether-fortress integration, ...) are captured in
  `patches/vanilla-manual-edits.patch` (committed; `.gitattributes` keeps it LF-exact), and
  `setupVanilla` applies it as its final step — this patch MUST apply cleanly or setup fails.
  Round-trip verified: `cleanVanilla` + `setupVanilla` reproduces the working tree
  byte-for-byte, so a fresh clone (with the MCP cache or a working download) can rebuild
  everything. Future manual edits to `vanilla/` must be folded into the patch — the
  regeneration procedure is documented next to the apply step in the root build.gradle
  (`setupVanilla -PskipManualPatch` produces the pristine baseline to diff against).
  *(2026-10-10 rule: FC code is read-only, and `vanilla/` is FC code. The 27 edits above and the
  `EntityAnimal` cast fix in the previous bullet predate the rule; the patch stays only so that
  `setupVanilla` keeps reproducing the existing tree. There are no "future manual edits to
  `vanilla/`" and nothing new is folded into the patch. A change that would have gone there goes to
  the bridge, Modern-Common, or the planned FC extension/patch subproject instead. The other legacy
  port edits that predate the rule are: the restructure of FC's flat `Src/` and `SrcServer/` sources
  into `net.minecraft.src.btw.*` (including hook-signature widening in `Server/`, e.g.
  `FCEntityCreeper.OnKickedByCow`), `applyFixups` in the root build.gradle (text edits to `vanilla/`
  during `setupVanilla`), and three vanilla classes added under `Client/` in 5257461 (`ModelChest`,
  `ModelLargeChest`, `TileEntityChestRenderer`). They stay so the tree keeps building, nothing new
  is added, and they are candidates to move into the extension/patch subproject. Full list in
  BRIDGE_SPEC.md, Key Principles.)*
- **Data-gen output now ships**: `Forge/src/generated/resources` (10 loot-modifier + 30
  stonecutter-recipe JSONs) is generated, committed, and guarded — the Forge `jar` task fails
  if the directory is missing/empty. Regenerate with `gradlew :Forge:runData`.
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
  extends World, not WorldServer; FC `setInPortal` never fires). *(2026-10-10: WorldBridge now
  extends WorldServer; the portal path stays inert because getAllowNether() returns false, see (x).)*
- `TileEntityHopper.suckItemsIntoHopper`/`func_96114_a`, `NBTTagCompound.getTags` —
  gated behind hopper/spawner minecarts (no EntityType registered; dispensed carts are
  inert-but-safe).
- `Item.appleGold`/`record13`/`recordWait`, `EntityList.getClassFromID` — gated on FC
  zombie wiring / legacy-ID mapping for spawn eggs and music discs.
- Client/Server variant drift: 6 methods in 5 dual-compiled classes (FCEntityCreeper x2,
  FCEntityPig, FCEntityZombie, FCBlockLogSmouldering, FCBlockNetherrackFalling) — Client
  kept stale narrow FC-type params, Server is widened; Client output wins the merge, so
  those overrides silently never dispatch. Fix by widening the Client sources to match
  Server twins. *(2026-10-10: the four FCEntityCreeper/FCEntityPig/FCEntityZombie hook overloads
  are widened at build time by widenFcHookDescriptors pass (c), see (w); the two block-class
  methods (FCBlockLogSmouldering, FCBlockNetherrackFalling) are not.)* *(2026-10-10 rule: the
  "widening the Client sources" fix above is withdrawn. FC code is read-only, so the Client sources
  are never edited; if the two block-class overloads are ever widened, it is done by a build-time
  transform like pass (c), in the planned FC extension/patch subproject.)*
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

**Where shadowing IS real — 21 collisions resolve to Modern-Common in dev:**

> **2026-10-10 correction:** this heading originally said "in both dev and jar", which was wrong
> for the jar. Until 2026-10-10 the Forge `jar{}` block declared `from sourceSets.fc.output` as an
> own source of the main spec, which Gradle copies before the Modern-Common child spec, and
> `DuplicatesStrategy.EXCLUDE` keeps the first copy. So in the jar the frozen fc copy won every
> collision that is not shadow-listed, while dev (securejarhandler searches mod sources in reverse
> order) let Modern-Common win. The released 1.3.0-alpha.3 jar therefore shipped the frozen copies
> of 16 classes that dev never ran: ContainerBeacon/BrewingStand/Chest/Dispenser/Enchantment/
> Furnace/Hopper/Merchant/Repair, EntityAIBase, EntityAIControlledByPlayer, IEntitySelector,
> IMerchant, InventoryMerchant, SlotCrafting and SlotMerchantResult. Jars built since the fc spec
> was added (9e9cb23, 2026-04-12) packed fc the same way, but only the alpha.3 jar was audited;
> earlier uploads were built from other trees, so their divergent set is unknown. The E1 fix in
> 2026-10-10 (w) makes the jar match dev (Modern-Common wins unless the class is on the shadow
> list), and a `jar.doLast` guard plus LinkAudit's JAR PRECEDENCE CHECK now enforce it. The bullets
> below describe dev, and the jar only from (w) on.

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
divergence sets over collisions are both empty). *(2026-10-10 correction: what this verified is
that the jar exclude list equals the dev delete list. The jar's actual winner for collisions on
neither list was still the fc copy until 2026-10-10 (w); see the correction above.)*
Residual: the FC vs stub UV difference for the model cluster is a visual change best eyeballed
once in a running dev client, but prod already used the FC versions, so dev now matches prod. See [[project_devjar_reflection_gotcha]].

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

## 1. btw.modern.Block (~81 remaining stubs)

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
- **Ground cover (P2, 2026-10-10 (ag)):** `CanGroundCoverRestOnBlock` (1.5.2 solid-top-surface rule)

### P0 — CRITICAL (14 remaining)

These are OVERRIDE POINTS — the base class default is intentionally empty/false/true. FC subclasses override them. They
only need real base implementations if vanilla Block.java in 1.5.2 had logic in the base class (e.g., breakBlock cleans
up tile entities). *(2026-10-20: the table omits `DropItemsOnDestroyedByExplosion`, which had such logic; see (bb).
2026-10-28: the rows marked (bn) are 1.5.2 base logic, now ported; 1.5.2's `setHardness` also had base logic, the
explosion-resistance floor, ported by (bn).)*

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
| `DropItemsOnDestroyedByExplosion(World, int, int, int, Explosion)` | 1.5.2 body (a throw logs once per class) | Base logic, ported (bn) |
| `OnDirtDugWithImproperTool`, `OnDirtSlabDugWithImproperTool`, `NotifyNeighborDirtDugWithImproperTool` | 1.5.2 bodies | Base logic, ported (bn) |
| `OnNeighborDirtDugWithImproperTool(World, int, int, int, int)` | Empty | Override point |

### P1 — HIGH (15 remaining)

| Signature | Current | Impact |
|-----------|---------|--------|
| `getRenderType()` | `0` | All blocks use default render type |
| `getComparatorInputOverride(World, int, int, int, int)` | `0` | Override point; BlockChest, BlockFurnace and BlockDetectorRail answer 1.5.2's readings since (bt) |
| `onBlockDestroyedByPlayer(World, int, int, int, int)` | Empty | Override point |
| `onBlockDestroyedByExplosion(World, int, int, int, Explosion)` | Empty | Override point (empty in 1.5.2 too); reached by 1.20.1 blasts since (bn) |
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

### P2 — MEDIUM (~52 remaining)

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
| Block property | 6 | `CanWeedsGrowInBlock`, `GetWeedsGrowthLevel`, `RemoveWeeds`, `AttemptToApplyFertilizerTo`, `GetConvertsLegacySoil`, `CanSpitWebReplaceBlock` |

*(2026-10-20: the work stumps of the four modern woods (META 5-8) are retrievable by the block dispenser through
2026-10-20 (ay)'s M2 patch; FC's own work stumps (META 0-3) still hit this stub.)*
*(2026-10-28: `HasStrata` and `GetMetadataConversionForStrataLevel` stay base defaults; FC's overrides of them now
drive StrataRule's classification ((bo)). Modern-Common `BlockOre` is no longer a stub: 1.5.2's drop methods ((bo)).)*

### P3 — LOW (Rendering/Client/Intentional No-ops — unchanged)

| Signature | Current | Notes |
|-----------|---------|-------|
| RenderBlock*, RenderFalling, RenderCooking, etc. (8 methods) | `false`/empty | Client rendering — vertex capture handles this |
| `randomDisplayTick(...)` | Empty | Client particles — separate system |
| Client notification methods (3 methods) | Empty | Client-side — different path in 1.20.1 |
| `dropXpOnBlockBreak(...)` | Empty | FC removes XP from most blocks |
| Falling block render methods (2 methods) | `ShouldRenderWhileFalling`: 1.5.2's body (bi); `ShouldSideBeRenderedOnFallingBlock`: `true`, the 1.5.2 base | Ported (bi) |

---

## 2. btw.modern.RenderBlocks (~49 remaining stubs)

File: `Modern-Common/src/main/java/btw/modern/RenderBlocks.java`

### Implemented since last audit

- `renderBlockByRenderType` — full switch statement with real logic
- `renderBlockTorch` — full vertex rendering
- `renderBlockLadder` — full switch-based implementation
- `renderCrossedSquares` — full 4-plane tessellator implementation
- `renderBlockCrops` — full # pattern implementation
- `renderBlockAsItem` — full face rendering implementation
- 6 face methods (renderFaceYNeg/YPos/ZNeg/ZPos/XNeg/XPos) — all fully implemented
- 2026-10-10 ((ad)): `drawCrossedSquares`, `renderBlockStemSmall`, `renderBlockCropsImpl` (verbatim
  1.5.2 ports), `renderItemIn3d` / `DoesRenderIDRenderItemIn3d` (1.5.2 table), and `renderBlockAsItem`
  dispatching to `Block.RenderBlockAsItem` with the 1.5.2 `renderBlockAsItemVanilla` branches

### P3 — LOW (all remaining — vertex capture pipeline replaces most of these)

| Category | Count | Methods |
|----------|-------|---------|
| Block rendering | 2 | `renderBlockUsingTexture`, `renderBlockAllFaces` |
| AO/color/grass | 6 | `renderStandardBlockWithAmbientOcclusion`, `func_102027_b`, `renderStandardBlockWithColorMultiplier`, `renderGrassBlockWithAmbientOcclusion`, `renderGrassBlockWithColorMultiplier`, `RenderStandardFullBlockWithAmbientOcclusion`, `RenderStandardFullBlockWithColorMultiplier` |
| Specific block renders | 28 | `renderBlockLog`, `renderBlockQuartz`, `renderBlockCactus`/Impl, `renderBlockFence`, `renderBlockWall`, `renderBlockFenceGate`, `renderBlockStairs`, `renderBlockDoor`, `renderBlockFluids`, `renderBlockRepeater`, `renderBlockRedstoneWire`, `renderBlockMinecartTrack`, `renderBlockVine`, `renderBlockPane`, `renderBlockStem`, `renderBlockLilyPad`, `renderBlockFire`, `renderBlockLever`, `renderBlockTripWireSource`/Wire, `renderBlockDragonEgg`, `renderBlockCauldron`, `renderBlockAnvilMetadata`, `renderPistonBase`/Extension, `RenderBlockRedstoneLogic`, `RenderBlockBeacon`, `RenderBlockBed`, `RenderBlockBrewingStand`, `RenderBlockCocoa`, `RenderBlockAnvil`, `RenderBlockEndPortalFrame` |
| Helpers | 2 | `renderTorchAtAngle`, `renderBlockStemBig` |
| Falling/piston | 5 | `renderBlockSandFalling`, `RenderStandardFallingBlock` (1.5.2's body since (bi)), `RenderStandardFullBlockMovedByPiston`, `renderPistonBaseAllFaces`, `renderPistonExtensionAllFaces` |
| Other | 2 | `RenderBlockFlowerpot`, `RenderBlockHopper` |

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

### 2026-10-26: the base classes carry 1.5.2's block answers (RANB-PARITY)

Many Modern-Common stand-ins of 1.5.2's block base classes lacked a 1.5.2 override, so every FC class above one took
`btw.modern.Block`'s default (opaque, a normal block, render type 0, movement and mobility from the material). An
audit of every method of the 309 FC block classes found 85 such answer gaps on 41 base classes (55 block ids). The
normal-cube query was not 1.5.2's either: `Block.isNormalCube` answered `isOpaqueCube()` and FC's World did not ask
the block's own `IsNormalCube` (20 ids answered differently from 4.B); the pane connection rule asked
`isOpaqueCube()`, so every pane joined doors, beds and other panes; the wall connection rule was a stub returning
false. Since W1 of round 3 (landed with (bg), whose crack follows `renderAsNormalBlock`):
- 40 base classes carry their 1.5.2 class's isOpaqueCube, renderAsNormalBlock, getRenderType, getRenderBlockPass,
  getBlocksMovement and getMobilityFlag wherever the port answered otherwise (81 methods);
- a normal cube is an opaque material rendered as a normal block (`Block.isNormalCube`, Block.java:350-357) unless the
  FC class answers `IsNormalCube` itself, and FC's World asks the block (`WorldBridge.isBlockNormalCube`,
  WorldBridge.java:1136-1138);
- `BlockPane`'s `canThisPaneConnectToThisBlockID`, `isOpaqueCube` and `renderAsNormalBlock` (BlockPane.java:30-49) and
  `BlockWall.canConnectWallTo` (BlockWall.java:9-12) are 1.5.2's.

Left out on purpose: `BlockDoor.getBlocksMovement` (value-equal today; it comes with the door codec),
`BlockLeaves.isOpaqueCube` (a 4.B server's answer: leaves always count as opaque, ruling of 2026-10-23), five
value-equal answers, and Block-level face culling (follow-up FU-RANB-CULL). Of the follow-ups the audit named, round 3
also fixed the soulforged plate and the BTW detector rails (FU-RANB-MECH, (bt)), comparator readings
(FU-RANB-COMPARATOR, (bt)), 1.20.1's torch and ladder placement (part of FU-RANB-PLACE, (bs)) and the wicker pane's
texture (FU-RANB-ICON, (bv)); the rest of the placement-rule family, face culling and the door codec stay open. No FC
file, mixin, data or protocol change. Verification: P-RANB 117/117 at W1 (45 of 117 passed at HEAD); P-PLACE's KNOWN
row H4 (glass counted as a normal cube) passes since W1, and P-CRACK-C's KG passes at ABC. In play: BTW grates, wicker
panes and slats collide and outline as panes and stop joining doors, beds and other kinds of pane; walls have arms for
BTW mobs; BTW mobs walk over pressure plates and past signs; a BTW chest opens under glass; BTW panes' items are flat
icons; FC attachables lose glowstone, redstone lamps, doors, beds, panes, cauldrons, hoppers and the other non-cubes
that answer through the default rule as supports, and ones already there pop at their next neighbour update. Caveats:
a corner pane collides as one bounds box, not 1.5.2's two thin boxes; the BTW door 1036 (commands only) takes FC's
bounds from the stubbed `getFullMetadata`; each pane or BTW door shape query builds a full FC World; FC entities
collide with 1.20.1 walls and panes by 4.B's connection rules while 1.20.1 draws them joined. Since W10 these bases
also carry 1.5.2's icon members ((bv)), and FC's World asks the block for its solid top too ((bw)).

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
- **DamageSourceMapping** — fcGloom mapped to magic (armour bypass). Since 2026-10-10 (ak), FC arrow/
  thrown/fireball/indirectMagic/explosion sources map to their 1.20.1 types, and `hurtFromFc` applies
  FC damage to vanilla entities through MC `hurt()`
- **EntityBridge.playSound** — delegates to SoundMapping.playAtEntity
- **ProxyAnimal/Mob/PathfinderMob NBT** — read/addAdditionalSaveData fully implemented
- **BTWNetwork** — PenaltySync packet registered and handled (2026-10-10: plus FCAnimSync and the
  FC|VC/FC|OP FCPayload; protocol "3", see (ab)/(al))

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

## 2026-07-09 (n) Worldgen: FC content inventory + phase 1 (structure loot)

Goal: keep 1.20.1 terrain, re-create FC's important worldgen CONTENT as native 1.20.1 pieces.
5-agent inventory workflow mapped every FC worldgen delta to a 1.20.1 mechanism. Key results:

- WOOD: "all wood is FC wood" is essentially DONE already — vanilla logs map to legacy 17 =
  FCBlockLog, BlockBehaviorMixin routes chop/hardness/convert, WorldBridge.deriveVanillaMetadata
  supplies orientation+wood-type. RESOLVED an agent contradiction: FC's tree-gen DELIBERATELY
  stumps the base (WorldGenTrees:234/WorldGenBigTree:528/WorldGenForest:133/WorldGenHugeTrees do
  `iTrunkMetadata | 12`), so the bridge's stump-synthesis is AUTHENTIC — keep it (one agent wrongly
  called it a bug; verified). One real wood bug remains: harvested non-oak logs drop OAK (species
  lost) — same for leaves/planks/slabs. Fix = BlockLog.damageDropped(meta)=meta&3 + a legacy->modern
  variant table in ProxyRegistry consulted by ItemStackHelper. NOT yet done.
- NETHER: FC adds almost NO nether worldgen. Blood moss + blood wood are SPREAD/GROW from
  player-placed seeds (already work via ProxyBlock.randomTick), not worldgen. FCMapGenNetherBridge
  = a mob-SPAWN change (do as add_spawns biome modifier), not blocks. Natural groves would be a
  non-FC design add.
- STRUCTURE LOOT: mostly re-weighted vanilla items (low value) + ONE net-new item (Lightning Rod
  in jungle temples) + three wicker-basket containers (witch hut/desert well: structure processors,
  high effort) + bonus basket + a mineshaft depth-rebalance (GLM). Strongholds/nether fortress: no
  FC loot.
- PLANTS/ORES: headline is the STRATA system (depth-based hardness/tool-tier on stone+ore, high
  effort, needs re-anchoring to y=-64). Plus extra jungle sugar cane + underground brown mushrooms
  (small features). Emerald/silverfish in hills = redundant with vanilla.
  *(2026-10-28: the strata line is superseded by (bo): the ruling keeps 4.B's absolute heights (mid at y <= 48, deep
  at y <= 24) rather than re-anchoring to y=-64, and deepslate is deep strata.)*
- BONUS BASKET: replace vanilla bonus chest with the FC wicker basket holding one Golden Dung.

Phase 1 shipped: Lightning Rod in jungle temples. New `add_item` GLM
(FCAddItemToLootModifier + BTWLootModifiers.ADD_ITEM), data-gen entry in FCLootOverrides, and the
generated JSON (data/betterthanwolves/loot_modifiers/jungle_temple_lightning_rod.json) targeting
minecraft:chests/jungle_temple with betterthanwolves:block_1067 at chance 0.2. Compiles clean.
Next: wood species-drop fix, bonus basket, mineshaft GLM, plants features, strata, nether spawns.

## 2026-07-09 (o) Lightning rod (FCBlockSpike) full-block collision under a 1px model

The FC Lightning Rod (fcBlockLightningRod, an FCBlockSpike) renders as a thin spike but
collided as a full block. FCBlockSpike defines its shape only via getCollisionBoundingBoxFromPool
(a thin strut/center box from FCModelBlockSpike) — it never sets minX..maxZ — but ProxyBlock's
getFcShape read minX..maxZ (default full cube). Fix: when the FC bounds are still a full cube on
a non-normal-cube block, getFcShape now calls getCollisionBoundingBoxFromPool (via a lightweight
FC world, extracted from animateTick into ProxyBlock.createFcWorld) and returns that box instead;
falls back to minX..maxZ on any failure. Targeted guard (fullCube && !renderAsNormalBlock) keeps
it off normal blocks. Fixes collision + selection outline for all FCBlockSpike variants.
(FYI the rod's function: it attracts thunderstorm lightning strikes — WorldServer:1477 /
FCEntityLightningBolt redirect bolts to it, an iron lightning rod predating vanilla's 1.17 one.)

## 2026-07-09 (p) Worldgen phase 2: wood species drops + bonus basket

- WOOD SPECIES DROPS (commit 6b3417e): the one real wood bug — chopping a natural spruce/
  birch/jungle log/leaf/plank/slab dropped OAK. Fixed via BlockLog.damageDropped(meta)=meta&3
  + ProxyRegistry.getModernBlockVariant variant table (17/18/5/126 -> 4 species blocks) +
  ItemStackHelper consulting it.
- BONUS BASKET: FC replaces the vanilla spawn bonus chest with a Wicker Basket (block 1031)
  holding one Golden Dung (item 22290) — a gag in place of free starter loot.
  BonusChestFeatureMixin @Injects BonusChestFeature.place (HEAD, cancellable): scans near the
  origin for a surface spot, places the basket ProxyBlock, and sets its single storage slot to
  golden dung via FCTileEntityBasketWicker.SetStorageStack (reflective; the FC tile entity is
  created eagerly by ProxyBlock.newBlockEntity so it's populated immediately). Falls through to
  the vanilla chest if no spot is found, so the player is never left empty-handed. Untested
  in-game (needs a bonus-chest world). Registered in the mixin config's common list.

## 2026-07-09 (q) Two systemic FC block-render fixes (workflow-diagnosed)

A 4-investigator workflow root-caused the wicker-basket scrambled mesh. Two distinct defects:

- GL11 STATIC MATRIX LEAK (systemic — could scramble ANY FC block): btw.modern.Tessellator
  is thread-local (render thread captures entity/TESR geometry; chunk-builder worker threads
  capture block-model geometry) and consults GL11's matrix per vertex. But GL11's matrix stack,
  tracking flag, color and normal were process-global `static`. A render-thread FC TESR (the
  basket's own animated lid, windmill, water wheel) enables tracking and pushes an animation
  matrix; a CONCURRENT worker-thread block capture saw matrixTrackingEnabled==true and ran the
  block's box vertices through that foreign matrix, baking scrambled positions into the chunk.
  The basket looked "fixed" only by timing luck. Fix: made GL11 state thread-local (per-thread
  State, mirroring Tessellator), so worker-thread block captures never observe the render
  thread's matrix. Belt-and-suspenders: FCBakedModel now calls GL11.disableMatrixTracking()
  before each block startCapturing(). This matches the note left after the axle fix (fe1dd22).

- INVISIBLE BASKET LID (missing color-multiplier emit): FCBlockBasketWicker draws its lid/handle
  /interior via RenderAsBlockWithColorMultiplier, but that path was a no-op stub at two levels of
  the port: FCUtilsPrimitiveGeometric's no-color-arg variant (should derive the tint from
  block.colorMultiplier and delegate to the 8-arg — restored, faithful to FC) and AxisAlignedBB's
  8-arg variant (never overridden -> inherited no-op; added, mirrors RenderAsBlock but tints via
  setColorOpaque_F). The base box rendered via RenderAsBlock, which is why only the lid vanished.
  The known-good spike/lightning rod use only RenderAsBlock, so they were unaffected.

## 2026-07-09 (r) Stoked-fire textures: fix CamelCase resource paths (Windows case trap)

The stoked-fire procedural flame was disabled at runtime: MC rejects non-lowercase resource
paths ("Invalid path in pack: betterthanwolves:textures/block/fcBlockFireStokedStub_0.png,
ignoring"), so the sprites never stitched and StokedFireTexture (which looks up lowercase
block/fcblockfirestokedstub_0/1) found nothing. Root cause: the four stoked-fire PNGs were
git-tracked as CamelCase (fcBlockFireStokedStub_0.png etc.), and a stray working-tree edit had
also pointed the model at the CamelCase name. With core.ignorecase=true (Windows) the mismatch
was invisible locally. Fixed by renaming all four textures to lowercase in git (temp-name
two-step, required on a case-insensitive FS) and reverting the model to the lowercase reference.
The atlas/blockstate/StokedFireTexture already used lowercase, so no other change was needed.

## 2026-07-09 (s) Fix all FC mobs going red / randomly dying (spurious MC suffocation)

Symptom: every FC-replaced mob rendered with the red hurt tint and died at random over ~30s
(log: repeated ProxyMob.remove(KILLED) via LivingEntity.tickDeath). Deductively traced: MC
owns puppet HP, so death means MC hurt() drove HP to 0. Ruled out by static analysis —
fire (FC Entity.fire is -1 when not burning -> flow-back sends 0), void (mobs above y=-64),
drown (on land), cramming (ProxyMob no-ops pushEntities), freeze (powder snow), fall (gated
behind the no-op travel()). Decompiling 1.20.1 LivingEntity.baseTick() (which DOES run on the
puppet via super.tick()) left exactly one land-applicable damage: inWall (suffocation), guarded
only by isInWall(). The puppet's MC position/eye box is FC-driven and doesn't match FC's world
view, so isInWall() fired spuriously on essentially every mob. Fix: override isInWall()->false
on ProxyMob/ProxyAnimal/ProxyPathfinderMob — FC already applies its own suffocation inside
fcEntity.onUpdate(), so MC's check is redundant as well as wrong. A temporary [MOB-DMG] WARN in
ProxyMob.hurt() (logs src/amount) is kept for one confirming run and will be removed after.

2026-10-10 note (triage R4, R1): the dying diagnosis stands (MC inWall damage drained the
MC-owned puppet HP; FC's own suffocation is the second source, see (t)). The red tint could not come
from it: FCEntityRenderer drew FC mobs with OverlayTexture.NO_OVERLAY, so an MC hurt never tinted
them. The red was the Tessellator colour leak (R1); see the correction under (u) and 2026-10-10
(ac).

## 2026-07-09 (t) Fix FC mobs turning red / dying: FC-side spurious suffocation

After (s) suppressed MC's suffocation (isInWall->false), mobs still flashed red and slowly
died -- the [MOB-DMG] diagnostic confirmed src=inWall (42670 hits) but showed inWall=true only
because the tested build predated (s). Once (s) actually ran, MC dealt no damage yet the red
persisted: FC applies its OWN suffocation inside fcEntity.onUpdate() (EntityLiving.onLivingUpdate
-> isEntityInsideOpaqueBlock -> attackEntityFrom(inWall,1)), which both drains fcEntity.health
AND raises worldObj.setEntityState(this,(byte)2). WorldBridge.setEntityState broadcasts that as a
client hurt event -> red flash (this is the src=generic amt=0.0 render-thread noise seen earlier).

Fix (FCEnvHurtGuard): the 3 puppet classes run fcEntity.onUpdate() through runOnUpdate(fc, this).
While the entity updates itself it's marked on the thread; WorldBridge.setEntityState drops the
hurt event (2), and any drained health is restored -- but ONLY for suffocation: the mob is still
alive (health>0, so lethal hits are never reverted -> no loot dupe / revive) AND its eye is in a
solid block, probed MC-side with FC's exact geometry (fc.getEyeHeight()=height*0.85, fc.width, the
same 8 corner points as isEntityInsideOpaqueBlock). Fall/cactus/lava self-damage, hits on other
entities, and external MC-hurt() combat are all left untouched.

First attempt reverted ALL onUpdate health loss; an adversarial review caught that it nullified
fall damage and, on a lethal fall, duped loot + revived the corpse (onDeath fires inside onUpdate).
The surgical suffocation-only version above was re-reviewed clean. Note: FC suffocation never goes
through MC hurt(), so the [MOB-DMG] log won't show it -- verify by eye (no red) after relaunch.

2026-10-10 note (triage R5): the MC-side geometry probe used here to decide "suffocation" disagrees
with FC's isBlockNormalCube. FCEnvHurtGuard now identifies the spurious hits from FC's own combat
log, reverts only inWall damage (also when onUpdate throws) and keeps the probe only as a fallback;
see 2026-10-10 (aa). Many of the spurious hits came from passable Modern-Common blocks that still
reported isOpaqueCube()==true (tall grass, flowers, repeaters, chests), fixed in (af). "Verify by eye
(no red)" could not work: the red was the Tessellator colour leak (triage R1), see (u) and (ac).

## 2026-07-09 (u) Fix stuck-on red overlay (FC self-suffocation hurt event)

After (t), FC mobs stopped dying (health restored) but the red hurt overlay stayed stuck on.
Cause: (t) gated BOTH the health-revert and the setEntityState(2) suppression on an MC-side
eyeInSuffocatingBlock re-probe. The health check runs post-onUpdate (end-of-tick position) and
worked; but FC raises the hurt event MID-onUpdate at the pre-movement position, where MC's
isSuffocating diverges from FC's isBlockNormalCube -> the re-probe missed it -> event 2
broadcast every tick -> client hurtTime pinned at 10 -> permanent red. Fix: the red suppression
now uses only `entity == UPDATING` (isSelfHurt) with no geometry re-check -- reliable, and safe
because a mob hurting another entity passes the victim (!= updater, still flashes) and real
combat runs outside onUpdate. Health is still reverted only for genuine suffocation (surgical,
in runOnUpdate), so fall/lava damage is untouched. Verified: run 16:47 shows 0 KILLED removals
(only DISCARDED despawns) and [MOB-DMG]=0.

2026-10-10 correction (triage R4, R1): the diagnosis above is wrong, and so is the one in (v).
FCEntityRenderer drew FC mobs with NO_OVERLAY and the Modern-Common RenderLiving had no hurtTime
pass, so a hurt event could not tint anything. The persistent dark red was the software
Tessellator's sticky per-thread colour: a "dragon" XP orb (RenderXPOrb, ported in (g)) set
(0.25-0.5, 0, 0, alpha 0.5), and every FC model captured afterwards inherited it (R1; the logged
tint's alpha 0.50 matches RenderXPOrb, not the 0.40 of a damage pass). Player kills kept spawning
dragon orbs because the lethal hit never reached FC (R2). The "0 KILLED removals" check also cannot
see FC-side deaths, which end as DISCARDED like despawns (R5). Real fixes: 2026-10-10 (ac)
(Tessellator colour semantics) and (aa) (kill credit, combat-log inWall revert). The isSelfHurt
event-2 suppression added here is still in WorldBridge.setEntityState.

## 2026-07-09 (v) Fix stuck dark-red tint on FC mobs (synced hurtTime -> FC RenderLiving)

The red survived (u) because it was never the MC hurt overlay -- FCEntityRenderer renders FC mobs
with OverlayTexture.NO_OVERLAY. The dark red is FC's own RenderLiving damage pass
(RenderLiving.java:184-186: if hurtTime>0||deathTime>0 -> glColor4f(brightness,0,0,0.4)), baked
into the captured vertex colors. Source: FC's spurious suffocation sets fcEntity.hurtTime=10 every
tick; (t) reverted health but NOT hurtTime, so the server value stayed high and rode the ongoing
FCEntityStateSync (BTWNetwork.broadcastFCEntityState -> FCEntityStateCodec.writeState ->
writeEntityToNBT "HurtTime") to the client fcEntity, which the FC renderer reads. Fix: in
FCEnvHurtGuard.runOnUpdate, clear fcEntity.hurtTime=0 whenever the mob is (falsely) suffocating --
runs before broadcastFCEntityState in all three proxy ticks, so the client gets HurtTime=0.
deathTime left alone (real death animations must still show). The setEntityState(2) suppression
stays for the spurious MC hurt SOUND (renderer ignores MC overlay).

2026-10-10 correction (triage R4, R1): the port had no FC RenderLiving damage pass. The
RenderLiving.java:184-186 cited above is vanilla 1.5.2; the Modern-Common stand-in had no
hurtTime/deathTime pass, and GL11.glColor4f was never read by the capture. Clearing hurtTime
therefore had no visual effect, and the 17:04 confirmation run happened to draw no XP orb. The cause
was the Tessellator colour leak (R1), fixed in 2026-10-10 (ac); see the correction under (u). Now
that (ad) ports the real 1.5.2 RenderLiving and (ac) maps hurtTime/deathTime to OverlayTexture,
hurtTime does drive the red flash. FCEnvHurtGuard now restores hurtTime only for reverted inWall
hits, identified from FC's combat log; see (aa).

## 2026-10-10 (w) Jar class precedence (E1): shipped jar ran 16 frozen classes; dead Client hook overloads

Symptom (design E1; triage RE-M3, HAI-2 addendum): the breakage was prod-only, so no dev run
reproduced it. The released 1.3.0-alpha.3 jar carried the frozen 1.5.2 copies of 16 classes that
dev never runs (only that jar was audited; earlier uploads also packed fc as an own source, but
their divergent set is unknown):
ContainerBeacon/BrewingStand/Chest/Dispenser/Enchantment/Furnace/Hopper/Merchant/Repair,
EntityAIBase, EntityAIControlledByPlayer, IEntitySelector, IMerchant, InventoryMerchant,
SlotCrafting and SlotMerchantResult. What the jar really bound was missing 8 classes
(InventoryRepair, SlotRepair, SlotEnchantment, SlotEnchantmentTable, SlotBeacon,
TileEntityBrewingStand, EntitySelectorAlive, EntitySelectorInventory) and 6 members. In prod:

- the anvil and enchanting table could throw NoClassDefFoundError from ProxyBlock.use, which has no
  catch (RE-M3, which the triage rates "possible; needs a runtime check" and calls a strong but
  unconfirmed candidate for "nothing works" on the shipped jar);
- `IEntitySelector.<clinit>` failed (hopper minecart);
- ContainerBridge.SimpleChestContainer doubled every slot, because the frozen ContainerChest
  constructor adds them too;
- the frozen SlotCrafting NPE'd on an empty take.

Separately, in BOTH dev and the jar, a creeper kicked by a cow never became determined to explode,
a zombie head-crabbed by a squid stayed silent, and FC's lightning hooks could not dispatch (a
struck creeper would have been hurt instead of charged, and a pig never became a pigman; see the
caveat below on why no lightning reaches FC mobs anyway).

Root cause:

- In Forge/build.gradle jar{}, `from sourceSets.fc.output` had no closure, so fc was an OWN source
  of the main spec while Modern-Common was a CHILD spec. Gradle visits own sources first and
  DuplicatesStrategy.EXCLUDE keeps the first copy, so fc won every collision in the jar. Dev was
  right only because securejarhandler's UnionFileSystem searches the mods{} sources in reverse
  order. The 'Modern-Common FIRST' build comment and the 2026-06-05 shadowing note above were wrong
  about the jar (corrected there).
- remapFcCode merges Client, Common and Server under EXCLUDE with Client first, so the Client
  variant always ships. Client FCEntityCreeper/Pig/Zombie still declare the 4.B narrow overloads
  (OnKickedByCow(FCEntityCow), OnStruckByLightning(FCEntityLightningBolt),
  OnHeadCrabbedBySquid(FCEntitySquid)), while every caller invokes the widened btw.modern.Entity
  hook, so they never dispatch.

Fix:

- jar{}: fc is now `from(sourceSets.fc.output) { exclude 'META-INF/MANIFEST.MF' }`, declared after
  the Modern-Common child. The shadow list (jar excludes == removeModernCommonShadowedClasses) is
  the only thing that lets fc win, in the jar and in dev alike. A new `jar.doLast` guard fails the
  build unless btw/modern/ContainerChest is Modern-Common's bytes and btw/modern/Entity is fc's.
  *(2026-10-19: removeModernCommonShadowedClasses is gone: the jar and the dev runs share one copySpec,
  modernCommonRuntime, and dev runs load a filtered copy of Modern-Common instead of deleting from its output; see
  2026-10-19 (av).)*
- widenFcHookDescriptors pass (c): narrow hook overrides in fc Entity subclasses get the wide
  `(Lbtw/modern/Entity;)V` descriptor and an `aload_1; checkcast <narrow>; astore_1` prologue;
  classes are read with EXPAND_FRAMES and every widened method is logged. Passes (a)/(b) and the
  flat-FC relocates are no-ops since the frozen-artifact regeneration; they now carry NOTEs and are
  kept pending cleanup.
- remapFcCode relocates `net.minecraft.server.MinecraftServer` to `btw.modern.MinecraftServer`. It
  is the only net.minecraft.server name in fc (frozen Entity, EntityItem, EntityMinecart) and is
  what lets WorldBridge extend WorldServer in (x).
- tools/LinkAudit.java: the header documents the shared precedence rule; ROOT comes from the working
  directory or -Dbtw.root; the reference scan now reads superclasses, interfaces, descriptors and
  checkcast/new/ldc/indy constants instead of member-ref owners only (review BUILD-R1). New
  sections: JAR PRECEDENCE CHECK (the built jar ships the modelled winner of every fc/Modern-Common
  collision), FOREIGN net.minecraft REFS (any `net/minecraft/**` name outside `net/minecraft/src`,
  or `net/minecraftforge/**` or `com/mojang/**`, linked by a winning fc or Modern-Common class; such
  a name binds to the 1.20.1 class at runtime, as MinecraftServer.getServer did before the
  relocate), and FC BYTECODE PATCHES (the pathing patch of (af)).
- tools/BridgeAudit.java (orchestrator edit): the renderer texture check strips %blur%/%clamp% and
  knows FCEntityRenderer's vanilla fallbacks (armour layers, power/witherarmor, glint, sheep fur,
  spider and enderman eyes), so the new render paths of (ac)/(ad) raise no false "missing texture"
  findings.
- Stale comments corrected: dev source order, the jar block, the Client-vs-Server duplicate rule,
  openUpEntityAccess, and the GL11 relocate comment (GL11 tracks colour, normal, lightmap,
  texture-matrix translation, blend func and the TEXTURE_2D/BLEND/ALPHA_TEST/LIGHTING caps; only
  depth/alpha-func/mask/polygon-offset, texture bind/params and immediate-mode calls are no-ops).

Verification: final LinkAudit (after wave 5): shadow-list drift [] / [], 0 missing classes, 6
missing members (8 in the pre-pass baseline; the 6 left are pre-existing and on inert paths:
ILogAgent.logSevere, ItemArmor.func_94602_b, ServerConfigurationManager.transferEntityToWorld,
TileEntityHopper.func_96114_a/suckItemsIntoHopper, WorldServer.resetUpdateEntityTick), FOREIGN REFS
count=0, and JAR PRECEDENCE CHECK over 80 collisions with drift 0 / stale 0 / absent 0. Run against
the old alpha.3 jar, the new check lists exactly the 16 drifts. Every build prints "Jar class
precedence OK (ContainerChest = Modern-Common, Entity = fc)", and reverting the fc spec makes the
guard fail. widenFcHookDescriptors logged 4 widened methods (Creeper x2, Pig, Zombie); javap shows
the (btw.modern.Entity) descriptors and `java -Xverify:all` accepts the rewritten classes.
BridgeAudit: 0 high-severity (the two particle-only aux-FX ids from (l) remain). Not run: a prod-jar
game test of chest slots, anvil, enchanting table, villager trade, hopper minecart and a cow-kicked
creeper.

Caveats: EntityAIControlledByPlayer stays the inert Modern-Common stub in both environments (pig
steering is not bridged). The guard's sentinel ContainerChest must stay off the shadow list. The two
Client-only block-class overloads (FCBlockLogSmouldering, FCBlockNetherrackFalling) are not widened;
their effect is client particles only. The widened OnStruckByLightning overloads now link and
dispatch, but no lightning reaches them in game (triage HAI-M1): FCEntityLightningBolt is created
only by the 1.5.2 WorldServer weather tick and the client's Packet71 handler (NetClientHandler),
neither of which runs in the port, and 1.20.1's own LightningBolt is not passed to FC mobs (there is
no thunderHit bridge). Creepers are not charged and pigs do not become pigmen; of the four widened
methods only the cow-kick and squid head-crab ones have an in-game effect. The released
1.3.0-alpha.3 jar keeps the old order: when triaging a report against it, check whether the code
path runs through one of the 16 classes (for an older upload the set is unknown).

## 2026-10-10 (x) One world identity: WorldBridge IS the WorldServer in worldServers[]; reload safety

Symptom (triage HAI-5, RE1, RE2, RE3, RE-M2; design U6, U9, U9-ORDER):

- FC cows that landed a kick threw `ClassCastException: btw.forge.WorldBridge cannot be cast to
  btw.modern.WorldServer` at FCEntityCow.TransmitKickAttackToClients:528, skipping the rest of that
  onUpdate; squid tentacle and attack-target syncs were lost the same way.
- Witches froze after their first potion swap: the frozen EntityLiving.java:864 equipment loop cast
  worldObj to WorldServer every tick, so onLivingUpdate never ran (seen in 9 Jul-9 sessions).
- FC chickens never laid eggs, finite torches never burned out and the fire-starter chance never
  decayed, because FCUtilsWorld.GetOverworldTimeServerOnly read a stub WorldInfo stuck at time 0.
  The nether, wither and end progress flags were never saved, and worldServerForDimension(-1)
  returned the overworld.
- The tier-3 global ender chest was emptied on almost every reload.
- Every closed world (server, levels, entities) stayed in memory for the life of the JVM.

Root cause: FC saw two worlds per level. WorldBridge (extends World) served entities and blocks,
while MinecraftServer.worldServers[] held ForgeWorldServerWrapper stubs with a default WorldInfo, in
overworld/end/nether order. The earlier `extends WorldServer` attempt was reverted (2026-07-08
section) because the frozen Entity.onEntityUpdate portal block calls
WorldServer.getMinecraftServer(), whose 1.5.2 return type resolved to the 1.20.1 class.
FcWorldSavedData reached the global inventory through getServer().worldServers[0], but the overworld
WorldBridge is created during prepareLevels (spawn-chunk block entities and mobs), before
ServerStartingEvent wires the server, so the load read nothing (fresh JVM) or the previous session's
stub (re-entry) and the next save wrote an empty list over the file. Nothing cleared the static
server or the bridge caches on stop, and each WeakHashMap cache value strongly referenced its key.

Fix:

- Modern-Common WorldServer gains getMinecraftServer() (1.5.2 WorldServer.java:1142) and
  MinecraftServer gains getAllowNether(); with the relocate of (w) they link to
  btw.modern.MinecraftServer. `WorldBridge extends btw.modern.WorldServer`; ForgeWorldServerWrapper
  is deleted.
- ForgeMinecraftServerWrapper.getAllowNether() returns false. Its only readers are the frozen
  Entity/EntityMinecart portal blocks, and frozen EntityThrowable does set inPortal (FC urns, spider
  webs); with true, 1.5.2 travelToDimension would discard the proxy and hit the missing
  transferEntityToWorld. Puppet dimension travel stays with 1.20.1.
- BTWLifecycle.wireWorlds fills worldServers[] with the real bridges in 1.5.2 order ([0] overworld,
  [1] nether, [2] end, modded after; a missing slot aliases the overworld with a WARN), and
  worldServerForDimension follows 1.5.2 (MinecraftServer.java:843-846).
- The overworld's FcWorldSavedData (data/btw_fc_world_data.dat, not level.dat) saves the global
  ender inventory under the port's key FCGlobalEnderItems (1.5.2 kept it in level.dat as
  FCEnderItems) and the 1.5.2 progress flags FCNetherAccessed/FCWitherSummoned/FCEndAccessed
  (WorldInfo.java:679-717), read from and written to the overworld bridge's own WorldInfo, keyed on
  Level.OVERWORLD with no getServer() dependency. Every load/save section catches Throwable and logs
  once (RE1).
- New BTWLifecycle.onServerStopped (ServerStoppedEvent) nulls the static legacy server and its
  worldServers[], clears the WorldBridge, EntityBridge, LivingEntityBridge and TileEntityBridge
  caches, and detaches each WorldBridge (level nulled). EntityBridge, LivingEntityBridge,
  TileEntityBridge and WorldInfoBridge hold their MC object weakly, and WorldBridge's
  FC-entity-to-proxy map is weak-valued (RE2).
- Other save/load paths made throw-proof in this pass: the ServerPlayerMixin NBT read/write and
  restoreFrom hooks ((y); RE-M2, which runs on Minecraft's unguarded single-player player-load
  path), ProxyBlockEntity save/load ((ae)) and setRemoved/clearRemoved (which run in stopServer
  before session.lock is released) all catch Throwable. Related reload fixes live elsewhere:
  headless item ids persist (RE5, (ag)), FC recipes survive login and /reload (RE6, (al)), and
  puppet health survives reload (RE7, (aa)).
- Supersedes the 2026-07-08 'REVERTED WorldBridge extends WorldServer' note and the LinkAudit
  'WorldServer.getMinecraftServer is architecturally undeclarable' classification (both annotated
  above).

Verification: javap of the frozen btw.modern.Entity shows
`WorldServer.getMinecraftServer:()Lbtw/modern/MinecraftServer;` and no net/minecraft/server/
reference; LinkAudit no longer lists getMinecraftServer or getAllowNether. Server smoke test (both
runs): "Wired world minecraft:overworld -> index 0 (FC dim 0)", "minecraft:the_nether -> index 1 (FC
dim -1)", "minecraft:the_end -> index 2 (FC dim 1)", "World wiring complete: 3 worlds registered",
no WorldServer ClassCastException and 0 ERROR lines. Not verified: the "Released BTW legacy server
state" line, the heap check after quitting and session.lock release (the harness could not send
'stop'), same-JVM re-entry, and the in-game checks: cow kicks play the client bow sound; witches
keep fighting after drinking; fed chickens lay at dawn; finite torches expire; a tier-3 ender chest
near spawn keeps its items across a fresh-JVM reload and a same-JVM re-entry.

Caveats: torch and chicken timers saved while the clock read 0 catch up once, and global ender items
already wiped cannot be recovered. FC mobs that change equipment now run onLivingUpdate, and mob
loot pickup now removes the picked-up item (onItemPickup no longer throws before setDead()).
Cow-kick, squid and attack-target FC|EV packets reach clients for the first time.
Entity.travelToDimension stays unreachable by design. WorldBridge uses detach-on-stop rather than a
weak reference to its ServerLevel (deferred), so a level that a mod unloads mid-session (dynamic
dimensions; vanilla never does) stays reachable until the server stops, and a WorldBridge must not
be used after ServerStoppedEvent. The triage found no BTW code path that reliably blocks re-entering
a world; this entry removes the candidate failure modes (RE1, RE-M2, plus RE-M3 via (w)), but the
original "can't re-enter" report is still unreproduced.

## 2026-10-10 (y) Player bridge identity; melee, slowdown, block-speed and airborne penalties

Symptom (triage HAI-1, RE-M1; design B3-CLIENT, B1, B2, B4):

- After the first death in a session, or after leaving the End, hostile mobs ignored the player
  until relog ("hostile mobs don't attack"), and the player stayed at the maximum FC health penalty:
  25% movement and mining speed and the 'dying' HUD.
- At FC health, hunger or gloom penalty levels, melee hits did about 1/8 of FC damage at modifier
  0.5 and about 0 at 0.25; arrows, thorns and sweep damage were reduced too.
- After a penalty cleared, the slowdown (and narrowed FOV) stayed until relog or death, and
  sprint-jumping shed the debuff.
- BTW's block movement modifier (+20% on hard surfaces) never applied to players.
- A logged-out player's bridge stayed "alive", so FC mobs kept chasing a ghost frozen at the logout
  spot.

Root cause:

- PlayerBridge.cache was a static WeakHashMap keyed by MC Entity.equals, which compares entity ids.
  PlayerList.respawn builds a new ServerPlayer and then calls setId(oldId)
  (PlayerList.java:440-443), so every lookup returned the dead player's bridge (health 0); FC target
  acquisition rejected it as not alive. In singleplayer the LocalPlayer shares the ServerPlayer's
  id, so the render thread got the server bridge. Values pinned their keys, and the map was not
  thread-safe.
- PlayerMixin.btw$applyMeleeDamageModifier used `@At("STORE")` with no @At ordinal (the `ordinal =
  0` was the local's), so it scaled all four stores of `f` in Player.attack: m^3, or m^4 on a crit.
  FCGameplayBridge.onLivingHurt then applied the modifier again to every player-attributed source.
- FCGameplayBridge.onPlayerTick reconciled the MOVEMENT_SPEED modifier only while a penalty was
  active, and rebuilt it every tick.
- LivingEntityMixin and ClientPlayerSpeedMixin hooked LivingEntity.getSpeed, which Player.getSpeed
  overrides without calling super, so both hooks were dead for players; getFlyingSpeed never saw the
  penalty.
- Nothing marked a removed player's bridge dead (1.5.2 calls setDead on logout and at the end of the
  death timer).

Fix:

- The bridge lives in a volatile @Unique field on Player (PlayerMixin implements the new
  PlayerBridge.Holder), and getOrCreate does double-checked creation under a lock: identity lookup,
  collected with its player, safe on the render and server threads. A respawned or cloned
  ServerPlayer keeps its own bridge, filled by ServerPlayerMixin.btw$restoreFrom. New
  PlayerMixin.btw$markBridgeDead (Player.remove TAIL) sets isDead for every removal reason except
  CHANGED_DIMENSION (logout, death timer, respawn, End exit), as 1.5.2 playerLoggedOut/onDeathUpdate
  -> setDead did, so FC AI drops the target at once. Client players get their own bridge over a
  read-only client world; PlayerBridge.createDetached serves the mining code of (z).
- Melee: `@At(value = "STORE", ordinal = 0)`, so only the base store is scaled (1.5.2
  EntityPlayer.java:1225-1242); onLivingHurt is deleted.
- Slowdown: onPlayerTick sets the FC_PENALTY_UUID modifier to the current GetLandMovementModifier()
  only when the value changes, and removes it at 1.0.
- Block modifier: PlayerMixin.btw$applyBlockMovementModifier (Player.getSpeed RETURN, both sides)
  applies only the block modifier, read from the block below Entity.getOnPos(); the penalty stays in
  the attribute. The dead LivingEntityMixin hook and ClientPlayerSpeedMixin are deleted.
- Airborne: PlayerMixin.btw$applyAirbornePenalty (Player.getFlyingSpeed RETURN) returns `0.02*m +
  (sprinting ? 0.006 : 0)`, as 1.5.2 EntityPlayer.java:513-523, except in creative/spectator flight;
  m is read on both sides from the synced attribute modifier.
- PlayerBridge.username is set from the GameProfile in the constructor and bound by FCLoginHandler
  on login and every PlayerEvent.Clone, so taming a wolf after a respawn no longer NPEs
  (FCEntityWolf.java:310) or stores a null owner.
- ServerPlayerMixin's NBT read/write and restoreFrom hooks catch Throwable and log once per player
  per hook (RE1, RE-M2); the tick hook logs once per throw site ((am)).
- Supersedes parts of 2026-07-09 (b), (e) and (j) (annotated there).

Verification: compile and audits only. The smoke test's "[SELF-AUDIT/speed] checked 1177 blocks, 0
with getSpeedFactor > 1.0" is unchanged; no player joined. In game (not yet run): /kill and respawn,
then zombies and creepers target and hit the player again, food resets and there is no 25% crawl;
after healing, `/attribute @s minecraft:generic.movement_speed modifier value get
b3e3b4a0-fc01-4e3b-8c1a-deadbeef1234` finds no modifier without a relog; sprinting on stone is about
20% faster than on grass and sprint-jumping does not accelerate; on a dedicated or LAN server a
zombie stops chasing a player who logs out; taming a wolf after a respawn records the player's name;
repeated quit-and-re-enter keeps the heap's PlayerBridge count at the number of live players.

Caveats: the FOV still narrows while penalised (the penalty is an attribute modifier). Client-side
bridges (LocalPlayer, RemotePlayer) refresh their position only when a client caller syncs them.
Metadata-dependent GetMovementModifier overrides still see a null World and give 1.0, and FC's 0.8
ground-cover factor is not bridged. As in 1.5.2, FC gloom and starvation effects now stop on a dead
player during the death screen.

## 2026-10-10 (z) FC dig speed on both sides; stump flicker; passable null-collision FC blocks

Symptom (design B3, B3-MSM, B3-CLIENT; review P1):

- Every BTW block (ids 175+) broke at the same speed whatever the tool: FC rough stone took about
  3.4 s bare-handed or with any pick.
- FC's health, hunger, fat and gloom penalties never slowed mining.
- On a dedicated server, mining stone, ore or logs with FC hardness threw an NPE on the client, a
  client crash on the first stone mined: the dig prediction
  (BlockBehaviorMixin.btw$getDestroyProgress, no try/catch) ran FC's getBlockHardness against a
  fresh PlayerBridge whose world was null (design B3-CLIENT). This comes from static analysis and was
  not reproduced at runtime. In singleplayer the LocalPlayer shared the ServerPlayer's entity id, so
  it used the server bridge and did not crash.
- Interim regression inside this pass (review P1, fixed below): after the identity change of (y),
  tree-base logs (stumps) broke, reappeared and broke again in singleplayer.
- FC blocks that have no collision in 1.5.2 (rails, plants, torches, campfires, candles, webs,
  ground cover, hand cranks, the invisible detector logic) were solid walls or steps.
- Survival-mining a modern-only block (deepslate, tuff, granite, andesite, ...) with a vanilla or FC
  tool could crash the integrated server (NPE in FCItemTool.onBlockDestroyed, rethrown by
  PacketUtils), and FC tools took no durability from modern blocks.
- In creative, blocks and seeds placed against FC terrain were used up.

Root cause:

- ProxyBlock.getDestroyProgress returned `1/(hardness*30)`, assuming Forge applies the tool
  afterwards. 1.20.1 BlockBehaviour.getDestroyProgress applies the tool inside itself
  (BlockBehaviour.java:364-372), and both game modes use the value raw.
- m_fCurrentMiningSpeedModifier (FC packet 166) was never set.
- The client predicted vanilla-id blocks through its own bridge, whose dummy world returned metadata
  0, while the server derives meta 12 for a stump (WorldBridge.deriveVanillaMetadata) and FCBlockLog
  triples a stump's hardness, so the client broke it 3x early and the server's STOP check (>= 0.7)
  rejected the break.
- ProxyBlock.getCollisionShape used the FC bounds even when FC's getCollisionBoundingBoxFromPool is
  null, which in 1.5.2 means no collision (Block.java:658-666).
- ProxyRegistry.getBlockId returns 0 for modern-only blocks, and 1.5.2 tool code dereferences
  Block.blocksList[0] (FCItemTool.java:66, FCItemClub.java:58).
- 1.20.1 restores the creative count only around ItemStack.useOn, never around Block.use.

Fix:

- New btw.forge.FCMiningSpeed.relativeHardness returns FC's own Block.getPlayerRelativeBlockHardness
  (vanilla/server Block.java:2340-2361) on both sides: on the server through the player's bridge and
  WorldBridge, on the client through PlayerBridge.createDetached(LocalPlayer) over
  ProxyEntity.createDummyClientWorld (held by a WeakReference). FC's mining penalty is applied for
  that call only, derived on each side from the same levels (the server bridge, or the PenaltySync
  mirror). ProxyBlock.getDestroyProgress and BlockBehaviorMixin.btw$getDestroyProgress both call it;
  if FC throws, a rate-limited BTW-Mining warning is logged and the old formula or vanilla is used.
  FCGameplayBridge.onBreakSpeed stays a no-op, so nothing double-applies.
- createDummyClientWorld.getBlockMetadata returns the now-static WorldBridge.deriveVanillaMetadata
  for non-META states, so the client sees the server's stump, axis and fluid metadata (mining open
  decision 2, option (b)).
- ProxyBlock.getCollisionShape returns Shapes.empty() when FC's collision box is null (probed only
  for FC classes that override it); selection keeps the FC bounds. FC webs (1037) slow entities
  through makeStuckInBlock(0.25, 0.05, 0.25).
- ProxyBlock.entityInside ports 1.5.2 contact fire for MC entities, players included (stoked fire, a
  burning ladder, lit campfires): 1 inFire damage per tick, ignition for 8 s after 20 contact ticks
  for players. Wet, fire-immune and creative players are not ignited; FC puppets are skipped because
  FC already burns them.
- Item-on-block stage 2 goes through ProxyBlock.tryPlaceHeldItem from both ProxyBlock.use and
  BlockBehaviorMixin.btw$use; it writes FC's held-stack edits back and in creative restores the
  stack's damage and size (1.5.2 ItemInWorldManager.java:394-402).
- Modern blocks: ItemStackMixin and ProxyItem.mineBlock skip FC unless the id is an FC block (the
  crash fix); ProxyItem.minedBlockIdForFc then reads ProxyRegistry.getWorldBlockId, so a modern
  block reaches FC as the stand-in of (ag) with the MC destroy speed (liquids and missing ids are
  skipped), and ProxyItem.mineBlock wears the held stack in place and broadcasts the break effect.

Verification: compile and audits only; nobody mined anything in the smoke test. Expected in game
(not yet run): FC rough stone (block_1027, hardness 2.25) takes about 22.5 s bare-handed, 3.35 s
with a stone pick and 0.6 s with an iron pick, with one destroyBlock per break; a natural oak stump
takes 3x as long as a higher log and does not flicker in singleplayer or on a dedicated server;
on a dedicated server the client mines stone, ore and logs without an NPE; famished or gloom
doubles dig time; players walk through FC campfires, candles, plants, torches and detector rails; an
FC web slows like a cobweb; stoked fire burns and ignites a survival player but
not a creative one; creative placement against FC terrain leaves the hotbar unchanged; an FC steel
pickaxe on deepslate loses 1 durability per block without a crash; `grep BTW-Mining latest.log` is
empty in normal play.

Caveats: server-only vanillaMetaOverrides (vanilla blocks whose meta FC set explicitly) are
invisible to the client prediction. 1.20.1-only blocks get no FC mining penalty. MC fluids now flow
into null-collision ProxyBlocks and break them; that is 1.5.2-like for plants and circuits, but vine
traps (leaves material) and placed buckets diverge. MC's cached per-state collision (suffocation,
isSolid, the puppet ground check) is computed at meta 0. The collision probe allocates a
btw.modern.World per query for FC classes that override getCollisionBoundingBoxFromPool. The
contact-fire counter is kept per entity in ProxyBlock and survives stepping out and back in within
20 ticks, where 1.5.2 resets it. Pre-existing: if FC re-syncs the PlayerBridge in the middle of a
call, the held FC stack is detached and a later survival decrement is not written back.
*(2026-10-15: the caveat "1.20.1-only blocks get no FC mining penalty" is fixed by compat wave C1, see 2026-10-18
(au): FCGameplayBridge.onBreakSpeed now applies BTW's health, hunger, fat and gloom modifier
(FCMiningSpeed.miningModifier) to every block with no FC block, on both sides.)*
*(2026-10-20: the "Pre-existing" re-sync caveat above (a held FC stack detached mid-call, so a later survival
decrement is lost) was the root of a survival duplication bug, CurseForge triage CF2-I1. Retired by 2026-10-20
(az): a re-sync now keeps the FC stack FC holds.)*

## 2026-10-10 (aa) FC mob puppets: kill credit, one HP pool, no MC drowning, death-only scroll drops

Symptom (triage R2, D1, D2, D6, D-M1, R5, RE7, SW4, SW7):

- One-hit player kills dropped red, uncollectable dragon orbs instead of green XP, which also fed
  the red tint of (ac).
- fc_squid drowned in rivers and lakes about 25 s after submerging, dropping ink sacs (and the
  occasional Respiration scroll) with MC's generic player "oof" on each hit. Glow squids and
  drowned, still vanilla then (see (ah)), dropped arcane scrolls while alive: Respiration about
  every 12 s, Smite about every 50 s. Together these were the "ink sacs and arcane scrolls appearing
  in lakes" reports.
- Mobs killed by FC-side causes (fall, fire, FC-mob melee) vanished silently with full MC health.
- Wounded FC mobs came back at full health after every reload; slimes could reload with the wrong
  max health.
- Spurious FC suffocation could still drain HP and play hurt sounds.
- FC blazes, ghasts, pigmen, magma cubes and wither skeletons took damage in lava and fire.
- Hostile mobs emitted one FLAME particle per second at their next path waypoint (debug leftover).

Root cause: MC owns puppet HP, but:

- ProxyMob, ProxyAnimal and ProxyPathfinderMob forwarded only non-lethal hits to FC
  attackEntityFrom, the only place FC sets recentlyHit. The lethal hit went straight to die() ->
  onDeath, so onDeathUpdate took BTW's "not killed by a player" dragon-orb branch
  (EntityLiving.java:739-748), and vanilla XP was suppressed along with vanilla loot.
- Damage FC applied inside onUpdate, or that other FC mobs dealt, never reached MC; the only
  MC-to-FC mirror ran on hurt.
- fc_squid is a ProxyPathfinderMob, not a WaterAnimal, so ForgeHooks.onLivingBreathe drowned it; the
  proxies used MC's GENERIC_HURT/DEATH sounds.
- ensureFcEntity called setHealth(max) after the NBT load.
- FCEnvHurtGuard decided "suffocation" with an MC geometry probe that disagrees with FC's
  isBlockNormalCube.
- FCGameplayBridge.onLivingTick rolled FC's per-death CheckForScrollDrop odds on LivingTickEvent, 20
  rolls a second, matching only the unreplaced vanilla classes.
- MC's Entity.baseTick on_fire/lavaHurt ran alongside FC's own burning and lava damage, and no fc_*
  type is fireImmune().

Fix:

- New FCPuppetSupport, shared by the three proxies. creditAttacker gives the 1.5.2 attackEntityFrom
  credit (EntityLiving.java:1095-1118) on every MC-accepted hit and in die(), and die() follows the
  1.5.2 order: credit, health 0, FC death sound, onDeath.
- FCPuppetSupport.HealthLink reconciles before and after FC onUpdate: FC-side HP loss goes onto MC
  (with MC's invulnerability window and an event 2), an FC death becomes MC die(generic) (event 3,
  no second onDeath, vanilla loot suppressed), and MC-side heals go onto FC. tickDeath waits for
  FC's onDeathUpdate, which spawns the XP at deathTime 20.
- All proxies return canBreatheUnderwater() true and keep full MC air, leaving FC's own air logic
  (EntityWaterMob, EntityLiving.onEntityUpdate:618-647) as the only one; getHurtSound/getDeathSound
  return null because FC plays its own.
- RE7: the loaded Health is kept; readAdditionalSaveData re-seeds MAX_HEALTH from the restored FC
  state (slime and magma cube size) and clamps Health to it.
- R5: FCEnvHurtGuard reads FC's CombatTracker entries from the update and reverts only inWall
  damage, plus the hurtTime and hurt resistance it set when those were the update's only self-hits.
  The revert runs in a finally block, so it also covers an onUpdate that throws; the old probe is
  only a fallback. FCEnvHurtGuard.deferSelfSound, called first by WorldBridge.playSoundAtEntity,
  holds back self-hurt sounds during the update and drops them when the only self-hits were reverted
  inWall hits.
- Fire: the proxies drop MC on_fire, lava and in_fire damage and skip lavaHurt; FC's own damage,
  mirrored by the health link, is the only copy.
- D2: onLivingTick is replaced by onLivingDrops (LivingDropsEvent): one roll at death, server only,
  non-baby, doMobLoot, non-creative world type (1.5.2 EntityLiving.java:3343-3369), using FC's own
  per-mob odds and enchantments (the old table had wrong entries for ghast, witch, blaze, magma cube
  and wither skeleton). It matches vanilla mobs only; FC puppets roll inside FC.
- SW4: the puppets' onGround patch counts only real collision shapes under the feet, so water is no
  longer ground. SW7: the FLAME waypoint particles are removed, and [MOB-DMG]/[ENTITY-DIED] logging
  is gone ((am)).

Verification: compile and audits only. The smoke test spawned puppets (worldgen pigs, sheep, cows
and chickens, structure villagers) with 0 ERROR lines, but no player interacted with them. In game
(not yet run): kill a size-1 slime or a chicken with a sword and get a green, collectable orb, drops
once and no dragon orb; `/summon betterthanwolves:fc_squid` in open water and poll Air (stays 300,
no hurt sounds); push an FC cow off a 25-block drop (Health falls, drops once, proxy removed after
FC's orb); damage a cow, save and reload (health kept); reload a damaged big slime (`Health <=
MAX_HEALTH = size*size`); mobs pressed against walls neither flash, lose HP nor make periodic hurt
sounds, while fall and lava still hurt; an FC blaze in lava takes no damage; a glow squid or drowned
kept alive drops no scrolls.

Caveats: land mobs now drown only through FC's own air logic, so FC's water-detection gaps apply
(see (ag): an eye inside a waterlogged stair or slab is not "in water"). A fire-resistance potion on
the MC side does not protect a puppet from FC fire damage, as before. Since (ah) replaces glow
squids, drowned, husks, zombie villagers, strays and wither skeletons, onLivingDrops now rarely
matches anything; those mobs roll FC's own scroll drop as puppets. The BTWNetwork protocol changed
(now "3", see (al)), so mismatched client and server builds are refused.

## 2026-10-10 (ab) Client FC mobs animate every tick; entity events reach every puppet

Symptom (triage R8, HAI-7 data half; reviews RF-1, proxies P1): legs, body and head stuttered at 10
Hz; sheep never grazed and wolves never visibly shook or howled; slimes never squished, chickens
never flapped and ghasts never showed their firing face; wither side heads stayed fixed; an iron
golem's raised arms and offered poppy never went back on the client; the creeper swell and flash
stayed at 0. FC-originated entity events (8, 10, 6/7, 11-16) reached clients only for proxies
spawned through WorldBridge.spawnEntityInWorld, never for replacer- or MC-spawned ones. An interim
client wolf tick briefly left wolves that touched water permanently darkened (review P1, fixed
below).

Root cause: the client branch of the proxies' tick only copied position. State came from the 2-tick
FCEntityStateSync snapshot, which overwrote the animation floats, so RenderLiving interpolated the
same pair across two client ticks. No proxy overrode handleEntityEvent. The client halves of 1.5.2
onUpdate/onLivingUpdate for slimes, chickens, ghasts, withers, iron golems, wolves and creepers
never ran, and WorldBridge.setEntityState only knew FC-spawned proxies.

Fix:

- FCPuppetSupport.clientAnimTick runs after MC interpolation and is render-only (no physics, no
  position writeback). It maintains prev and current position from MC; body and head yaw and swing
  from the new per-tick BTWNetwork.FCAnimSync packet; limb swing from interpolated motion
  (EntityLiving.java:1666-1677); hurt and death timers; sheep grazing; wolf howl, shake and head
  tilt (the client starts the shake itself once the wolf is out of water, on the ground and still,
  EntityWolf.onLivingUpdate :191-202); the creeper fuse counters lastActiveTime/timeSinceIgnited
  (EntityCreeper.java:130-150; DataWatcher 16 arrives with the state sync); slime and magma cube
  squish (EntitySlime.onUpdate :106-151); chicken wing flap (:65-95); the ghast firing face
  (DataWatcher 16); wither head angles from the synced target ids (EntityWither.java:171-207); the
  iron golem's attackTimer and holdRoseTick countdown (EntityIronGolem.java:113-121, fields reached
  through a ClassValue); and silverfish ticksExisted for the body wave.
- FC-relevant entity events and MC damage events are forwarded to the client FC entity's
  handleHealthUpdate, and the live state sync no longer overwrites client-maintained fields.
  WorldBridge.setEntityState (and explosions) fall back to finding the puppet by entity id, so
  replacer- and MC-spawned puppets get FC events too.

Verification: compile and audits only; nothing client-side was run. In game (not yet run): legs move
smoothly and zombies swing when attacking; a wolf that swims, climbs out and stops shakes once and
returns to normal brightness; slimes squash on landing; chickens flap while falling; a ghast opens
its mouth while charging; wither side heads track their targets; a golem's arms settle about 0.5 s
after a punch and its poppy is lowered after 20 s; a creeper swells and flashes white before
exploding.

Caveats: squid tentacle and body angles, and the cow-kick and squid tentacle-attack counters, are
still not maintained on the client. Particle-only events (6/7 taming hearts and smoke, 12-14
villager, 15 witch) are forwarded, but the client dummy world's spawnParticle is a no-op; enabling
it would double the taming particles the server already sends.
*(2026-10-28: still so after (bh): FC particles go through one table now, but the entity client world's spawnParticle
stays empty; `FcParticles.addLocal` is the route for the follow-up, FU-PE.)*

## 2026-10-10 (ac) Red mobs: 1.5.2 Tessellator colour rules, per-quad GL state, OverlayTexture flash

Symptom (triage R1, R6, D3, R4; the "all mobs are forever red", "textures turn red" and "vanilla
mobs constantly in a dying state" reports, which only 1.3.0-alpha.3 can produce): after any unowned
"dragon" XP orb had been drawn, every later FC model on the render thread (mobs, arrows, windmills,
urns, paintings, TESR items) was tinted opaque dark red. Owned orbs, lightning, lifted redstone and
render-thread block bakes leaked other colours. Dragon orbs cannot be collected, live 6000 ticks and
are saved, so the tint came back right after loading a world with orbs nearby. glColor-driven tints
(sheep wool, wolf collars, dyed windmill blades, RenderItem tints) never showed. Each FC entity was
drawn with one RenderType taken from its first quad, so armour, eyes and translucent layers could
not render. Mobs never flashed red when hit and creepers never flashed white. Every FC capture was
turned 180° about Y, so arrows pointed backwards.

Root cause: btw.modern.Tessellator kept one sticky colour per thread. 1.5.2 Tessellator.startDrawing
clears hasColor (Tessellator.java:322-339), so an uncoloured draw uses GL's current colour; the
port's startDrawing*/startCapturing reset nothing, and GL11.glColor was tracked but never read. FC
mob models never set a colour, so they inherited whatever the RenderXPOrb port of 2026-07-09 (g)
(commit c2e15f3) last set: (0.25-0.5, 0, 0, alpha 0.5) for dragon orbs. The logged tint's alpha 0.50
matches RenderXPOrb, not the 0.40 of a damage pass. FCEntityRenderer multiplied the texture by it
under entityCutoutNoCull with no blending. GL blend, texture and lightmap state were discarded. The
1.5.2 damage pass (RenderLiving.java:196-217) was never ported and FCEntityRenderer used NO_OVERLAY.
The 180° turn compensated the old stand-in RenderLiving's glScalef(1,-1,-1) but was applied to every
renderer. The dragon orbs that player kills spawned ((aa), R2) kept the source supplied. This is why
2026-07-09 (u) and (v) could not work (see the corrections there).

Fix:

- Tessellator follows the 1.5.2 per-draw rules: startDrawing* and startCapturing clear hasColor,
  hasNormals, hasBrightness and isColorDisabled, and uncoloured or un-normalled vertices take GL11's
  current colour and normal (white and 0,1,0 by default). startCapturing also calls
  GL11.resetCaptureState(), 1.5.2's per-object defaults (RenderManager.java:203-204,
  TileEntityRenderer.java:154-155), and clears the tessellator translation, which a renderer that
  threw between setTranslation and its undo used to leave on every later capture (review RC-1).
- GL primitives: strips become paired quads (FC's lightning bolt), triangles and fans become
  degenerate quads, and lines and points are dropped. A /terrain.png or /gui/items.png bind keeps an
  icon name already recorded.
- GL11 tracks, per thread, GL_TEXTURE_2D per texture unit (OpenGlHelper.setActiveTexture), GL_BLEND
  and glBlendFunc, GL_ALPHA_TEST, GL_LIGHTING, the lightmap coords and glMatrixMode; a GL_TEXTURE
  translation is recorded as a swirl offset. Every CapturedQuad snapshots this state.
- FCEntityRenderer batches quads per (texture, blend, light override, swirl): entityCutoutNoCull for
  opaque, entityTranslucent for SRC_ALPHA/ONE_MINUS_SRC_ALPHA (slime shell), eyes for additive
  (spider, enderman and wolf eyes), energySwirl for a texture-matrix scroll (creeper and wither
  power), armorEntityGlint for misc/glint.png, lightning() for untextured additive geometry; other
  untextured quads are skipped.
- Overlay: OverlayTexture.pack(u(RenderLiving.getOverlayWhiteness), v(hurtTime > 0 || deathTime >
  0)), applied only to batches drawn from entity textures. Icon batches (held items, carried blocks,
  the snowman's pumpkin) use NO_OVERLAY, as 1.5.2's damage passes re-render only mainModel and
  armour (RenderLiving.java:189 vs :196-234; review RC-2).
- Textures: bundled 1.5.2 originals first, with 1.20.1 fallbacks for armour layers, creeper and
  wither armour, glint, sheep fur and eyes; bare icon names use the bundled file, else the
  block-atlas sprite; unknown paths are logged once.
- The 180° flip applies only when GL11.usedLegacyEntityFlip() reports the old stand-in's flip, which
  no longer happens after (ad).

Verification: compile and audits only (BridgeAudit's texture check knows the new fallbacks, (w)).
Nothing client-side was run, so the tint fix is verified by code review only. In game (not yet run),
the triage's reproduction: kill a 1-HP slime or let a zombie burn at dawn so a dragon orb appears;
the orb renders dim red and mobs drawn afterwards look normal. Also: a hit gives about 10 ticks of
red flash; a creeper fuse pulses white and a charged creeper shows a swirl; spider and enderman eyes
glow at night; slimes show a translucent shell; sheep wool and wolf collars carry their dye; dyed
windmill blades are coloured; arrows point the way they fly; lightning is a translucent blue-white
bolt; block models and TESR items look unchanged.

Caveats: the hurt tint follows the client FC entity's hurtTime/deathTime (maintained on the client
by (ab)), so a server hurtTime that gets stuck would now really show as a persistent red tint; the
inWall revert of (aa) therefore matters visually. Untextured non-additive geometry has no MC
equivalent and is dropped (the RenderLiving colour pass, the mining-charge flash, name-tag
backgrounds, the fishing line). A light override is applied as the per-channel maximum with the host
light. Strip conversion assumes paired vertices. Non-living FC renderers lose the old 180° turn;
that matches 1.5.2, but windmill and water-wheel spin direction and fixed billboard facing change
visibly.

## 2026-10-10 (ad) 1.5.2 entity renderers ported; held items, armour, carried blocks, block icons

Symptom (triage R7, SW2, SW6; design U5b; the "mob animations buggy" reports): chicken wings, wolf
tails and squid tentacles spun continuously; zombies and skeletons never swung their arms and showed
no held item or armour; sheep had no wool; slimes were all one size with no shell; babies were
adult-sized; dying mobs stood upright and vanished; squids were yawed twice; creepers neither
swelled nor flashed; endermen had no eyes and no carried block; villager zombies used the plain
zombie head; magma cubes, withers and wither skulls drew the debug box (so did the fc_iron_golem
and fc_silverfish types that (ah) added in this pass, until their renderers landed; in alpha.3 those
mobs showed as a vanilla villager or zombie). Follow-ups found while porting (interim states of
this pass, not alpha.3 behaviour): held items drew the missing texture, carried and held blocks
sat half a block off, an invisible mob showed the orange debug box, a held item emitted about 1026
edge quads instead of 66, held blocks drew as flat icons with wrong textures, flowers carried by
endermen drew as cubes, held and worn blocks were textured with the mob's own skin until the first
FC block was meshed, a carried grass block showed the 1.20.1 tall-grass texture with a grey top, and
undyed leather armour on mobs rendered black or untinted with no trim layer.

Root cause: Modern-Common RenderLiving was a minimal stand-in, unchanged since 365f5c8. It never
called setLivingAnimations, renderSwingProgress, handleRotationFloat, shouldRenderPass or
renderEquippedItems, never set model.isChild/isRiding/onGround, and passed ticksExisted+pt where
1.5.2 passes handleRotationFloat (ModelChicken.java:99-100, ModelWolf.java:199, ModelSquid.java:48),
which made wings, tails and tentacles spin. It also rotated by 180-bodyYaw itself before calling
rotateCorpse, which squids override. The vanilla Render* subclasses were 5-7 line constructor stubs.
Downstream Modern-Common gaps: RenderBlocks.renderBlockAsItem lacked 1.5.2's 90° turn and ±0.5
centring and never dispatched to block.RenderBlockAsItem; ItemBlock inherited Item's sprite number 1
and item icon, where 1.5.2 returns 0 and the block's top texture (ItemBlock.java:42-56); the plant
stand-ins kept render type 0; block icons were registered lazily on the first FC block capture,
while 1.5.2 TextureMap registered every block before anything drew; BlockGrass had none of 1.5.2's
client methods; 1.5.2 item icon names (swordIron, hatchetIron, dyePowder_black, ...) were renamed in
1.20.1; the vanilla armour was built without an EnumArmorMaterial and ItemArmor.getColor returned 0
(reviews RP-4, RC-3).

Fix:

- RenderLiving is now 1.5.2 doRenderLiving (vanilla/client RenderLiving.java:66-257), issuing GL11
  calls as 1.5.2 does: rotateCorpse with the deathTime tip-over, glScalef(-1,-1,1),
  preRenderCallback before the -1.5078 translate, child `limbSwing*3`, the
  shouldRenderPass/renderPassModel loop with the glint, and renderEquippedItems. The
  hurt/death/creeper colour pass is left to (ac)'s OverlayTexture through the new
  RenderLiving.getOverlayWhiteness; name tags are left to 1.20.1.
- Ported every vanilla renderer FC registers (FCBetterThanWolves.java:2395-2446): RenderBat, Blaze,
  Chicken, Cow (FC udder), Creeper, Enderman, Ghast, MagmaCube, Ocelot, Pig, Sheep, Skeleton, Slime,
  SnowMan, Spider, Villager (FC priest eyes), Witch, Wither, WitherSkull (now a plain Render), Wolf,
  Zombie, and RenderBiped (armour passes, FCItemArmorMod, held items, arm pose, and worn skulls
  including FC's infused skull and its eyes pass). New models: ModelZombieVillager, ModelMagmaCube,
  ModelWither, ModelSkeletonHead, ModelSilverfish. ModelCow gains the calf head offset and the FC
  kick leg; ModelWitch resets its nose offsets each call (review RP-1). RenderIronGolem and
  RenderSilverfish are registered for the frozen classes (U5b). Members the Modern-Common stubs lack
  are reached through the new RenderEntityAccess (cached reflection, checked against the frozen
  classes with javap). This also resolves LinkAudit's FCClientRenderSpider.setSpiderEyeBrightness.
- Held items: ItemRenderer.renderItem(EntityLiving, ItemStack, int) and RenderManager.itemRenderer
  are added; the extrusion slice count comes from the icon (sliceSheetWidth/Height, also used by
  RenderItem, which now takes the 1.5.2 `fancyGraphics || m_bForceFancyItemRender` path).
  FCBakedModel.VANILLA_ITEM_TEXTURE_MAP maps 156 lower-cased 1.5.2 item icon names (144 listed
  plus the 12 music discs) to minecraft:item/ textures, and VANILLA_TEXTURE_MAP gains the plant and torch names (rose -> poppy,
  flower -> dandelion, tallgrass -> grass, saplings, mushrooms, dead bush, redstone torch).
  FCEntityRenderer accepts a bundled icon file only for "fc*" names.
- Blocks as items: RenderBlocks.renderBlockAsItem dispatches to block.RenderBlockAsItem as 1.5.2
  does (RenderBlocks.java:9453-9456); the default's re-entry is answered by
  renderBlockAsItemVanilla, so there is no recursion, and an override that throws is logged once and
  drawn plain afterwards. renderBlockAsItemVanilla follows 1.5.2: dispenser/dropper/furnace metadata
  3, render types 1, 6, 13, 16, 19 and 23, the ±0.5 centring, GetBlockBoundsFromPoolForItemRender
  instead of overwriting the shared block bounds, and the useInventoryTint colour (without 1.5.2's
  brightness factor). DoesRenderIDRenderItemIn3d is the 1.5.2 table and Block.DoesItemRenderAsBlock
  uses it; drawCrossedSquares, renderBlockStemSmall and renderBlockCropsImpl are verbatim ports.
  Render types: BlockFlower 1 (saplings, mushrooms, dead bush and tall grass inherit it), BlockStem
  19, BlockCactus 13 (with its 1.5.2 icons), BlockReed 1, BlockLilyPad 23, BlockNetherStalk 6.
  ItemBlock returns sprite number 0 unless the block names an item icon, and getIconFromDamage
  returns the block's top icon, falling back to the item icon.
- Icons and colour: BTWClientEvents.onModifyBakingResult runs
  FCBakedModel.ensureGlobalIconsRegistered() at the end of model baking (also on the
  placeholder-model-missing branch, review CL-R1), so block icons exist before anything draws; the
  lazy pass remains as a fallback. BlockGrass gains 1.5.2 getIcon/registerIcons (grass_top, the dirt
  bottom, grass_side plus its overlay), the default colour 0x7CBD6B and colorMultiplier;
  BlockTallGrass.getRenderColor is ported.
- FCEntityRenderer draws the debug box only when the FC entity or its renderer is missing, or when
  doRender threw and captured nothing; a clean empty capture draws nothing (an invisible mob, as in
  1.5.2).
- Armour: the vanilla armour is built with CLOTH/CHAIN/IRON/DIAMOND/GOLD, and
  ItemArmor.hasColor/getColor/removeColor/func_82813_b are ported (undyed leather is 10511680,
  brown).

Verification: compile and audits only; [SELF-AUDIT/renderer] is client-side and was not run. A javap
check confirmed that every renderer constructor and method FC client bytecode calls exists with the
1.5.2 descriptor (for example `RenderSpider.setSpiderEyeBrightness(Lbtw/modern/EntitySpider;IF)I`)
and that every reflectively reached frozen member exists; the wave-1 review token-diffed the ports
against vanilla/client. In game (not yet run): chicken wings rest folded and wolf tails follow
health; sheep wool is coloured and disappears when sheared; slimes scale with size; babies are
small; dying mobs tip over (spiders onto their backs); squids face their swim direction; magma
cubes, withers, wither skulls, golems and silverfish render; a skeleton's bow and a zombie's sword
show their sprites; the snowman's pumpkin sits on its head and an enderman's carried block is
centred; an enderman's flower draws as crossed planes and its grass block has a green top; an
invisible mob shows no orange box; leather armour on a mob is brown with its trim layer.

Caveats: the animations are only as good as the client FC state of (ab). A player skull with a
SkullOwner draws the default char.png (no skin download). Icon names shared by a terrain icon and an
item icon (clay, brick, reeds, netherquartz, comparator) resolve to the block texture, and skulls
and beds have no 1.20.1 item texture. The torch, chest, anvil, hopper and other special item
branches still draw as cubes. ItemBlockWithMetadata, ItemMultiTextureTile and ItemColored flat icons
use meta 0. Held block items run FC RenderBlockAsItem overrides with a null blockAccess; an override
that dereferences it is logged once and drawn plain from then on. Grass colour is a fixed default
because the port's BiomeGenBase.getBiomeGrassColor is a constant. The 90° turn now matches 1.5.2, so
blocks with a distinct face (the pumpkin) face differently from before. Vanilla iron golems and
silverfish are not replaced; only FC-spawned ones use these renderers. ModernBlockStandIn blocks get
no icons (they have no textures anyway).

## 2026-10-10 (ae) FC blocks render live (neighbours, tile entities); chest, basket, placed-tool TESRs

Symptom (design U8, U8b, U8d, U7, U7b; triage HAI-10 stepSound; reviews BR-1..BR-5, CM-R1): in-world
FC blocks always rendered from a one-time per-meta capture against a fake world. Tables and benches
kept all their legs; grates, slats and wicker panes never connected; platforms showed every rail;
cauldron and crucible contents never showed; campfire and stoked-fire flames ignored position; FC
fences and panes next to modern solids drew no arm. Blood-wood stairs (1009) were invisible
(FCBlockPlanks icon NPE) and smoothstone stairs (1003) used the missing texture. Wheat crops and
weeds (1089-1091), the BTW chest (1035) and the placed tool were invisible, and open wicker baskets
showed no stored item. FC mobs sometimes skipped a tick on landing (StepSound NPE). Interim
regression inside this pass (review MC2-R1, fixed below): once (af) let ground cover rest on slabs,
ash on a vanilla double slab drew half a block low, inside the slab; in alpha.3 ground cover could
not rest on slabs at all.

Root cause: FCBakedModel's live branch needed ModelData that only ProxyBlock.getModelData supplied,
which is not a Forge hook and was never called (Forge asks the model,
ChunkRenderDispatcher.java:631). Icons were registered only on ProxyBlocks (1.5.2
TextureMap.java:61-71 does every block), and the sprite cache was a plain map shared with mesh
threads. Modern-Common Block.RenderCrossHatch was an empty stub, and Block had no default stepSound
(1.5.2 Block.java:474). BTWClientEvents used the pre-relocation TileEntityChestRenderer name and
omitted the wicker basket. ProxyBlockEntity's client world had no provider, so frozen
`Entity.<init>` NPE'd in every FC TESR that builds an EntityItem, and RenderManager.options was
never set. LiveAccess read identity ids instead of the ids the server uses.

Fix:

- FCBakedModel overrides getModelData. Static per-meta captures go through StaticAccess, which
  records neighbour, TE and opacity reads (plus a (1,0,0) parity capture); only the dependent metas
  are re-rendered live on each chunk rebuild through LiveAccess (WorldBridge semantics, default TE
  when missing), falling back to the static quads. LiveAccess.getBlockId uses
  ProxyRegistry.getWorldBlockId (double slabs, stand-ins and water-filled plants, as the server sees
  them), and vanilla neighbours report metadata through WorldBridge.deriveVanillaMetadata.
  runFcRender mirrors RenderBlocks.java:9442-9448, resets Tessellator and GL11 state afterwards, and
  clears renderer.blockAccess so no chunk snapshot or ClientLevel stays pinned across world
  switches.
- Icons are registered on every block; spriteCache is a ConcurrentHashMap, generation-tagged and
  cleared on the block atlas's TextureStitchEvent.Post, so a resource reload cannot leave stale
  sprites. The dead ProxyBlock.getModelData is deleted.
- RenderCrossHatch is ported verbatim, and the Block constructor defaults stepSound to
  soundPowderFootstep.
- The chest TESR uses btw.modern.TileEntityChestRenderer. The wicker basket uses the lid-only
  FCTileEntityBasketRenderer with its stored item drawn natively, and the lid is hinged on the
  PoseStack (FCBlockEntityRenderer.applyBasketLidHinge, porting :94-120 and :131-139) because the
  capture applies the GL matrix at addVertex time, so FC's post-vertex glRotatef never reached the
  lid. createClientWorld sets a WorldProvider, and RenderManager.options is a GameSettings with
  fancyGraphics=true.
- FCBlockEntityRenderer: each capture starts and ends clean; the campfire and brick-oven FC TESRs
  are skipped because their cook item is drawn natively; the placed tool uses its 1.20.1 item
  sprite; quads are batched per texture and blend (cutout, translucent, eyes, glint; untextured
  skipped); bare icon names resolve to block-atlas sprites.
- ProxyBlockEntity.load builds the replacement FC tile entity completely before publishing it
  (volatile field). On the client a same-class rebuild copies the client-only animation fields
  (basket lid ratio and closing flag, chest numUsingPlayers/lidAngle/prevLidAngle), so the
  once-a-second sync no longer resets an open basket's item or snaps a chest lid shut (1.5.2
  NetClientHandler.java:1191-1219 kept the client TE). onDataPacket re-meshes through
  sendBlockUpdated only when the fcSync data changed (1.5.2 readNBTFromPacket ->
  markBlockRangeForRenderUpdate). Save and load catch Throwable and log once (RE1).

Verification: compile and audits only; nothing client-side was run, so none of the expected client
log lines ("Registered 6 FC tile entity special renderers.", "Registered icons on N blocks",
per-block "live-capture metas: 0x..") has been observed. In game (not yet run): a 3x3 of tables
keeps only the outer legs, panes connect and platform inner rails vanish; wheat is visible; the BTW
chest renders and its lid stays open while the GUI is open; a placed iron axe shows its texture; an
open wicker basket swings its lid up to 45° and raises its item once; an item put into a cauldron or
hopper updates the block within a tick or two; FC textures stay correct after a resource-pack
change; ash on a vanilla double slab sits on top of it.

Caveats: the breaking overlay, pistons and falling blocks still use the static quads. Live metas
re-run FC under a global lock on every rebuild; add a memo cache if chunk-build times regress. The
basket item is drawn once, without 1.5.2's jittered copies. If the Tessellator is ever changed to
apply the modelview at draw(), applyBasketLidHinge must go or the lid rotates twice. Client-only
tile-entity state other than the copied fields is still reset on every data packet, and re-meshing
follows fcSync only. FCTileEntityBasketRenderer.m_localRenderBlocks still holds the last client
world wrapper (one ClientLevel). Blocks that had a null stepSound now play the 1.5.2 default stone
sounds.
*(2026-10-28: the breaking crack of a block FC draws is FC's RenderBlockWithTexture over the live world since (bg),
and FC falling blocks are drawn by FC's RenderFallingSand through the entity capture, shaded like placed blocks, since
(bi); pistons still use the static quads.)*

## 2026-10-10 (af) 1.5.2 AI scheduler (creepers); passable fluids and plants; block collision; pathing below y=0

Symptom (triage HAI-2, SW3, SW-M1, HAI-3 pathing half; reviews AP-1..AP-7, MC-R2, PATH-R1):

- Creepers walked to about 3 blocks from the player and stood there, never hissing ("creepers don't
  explode").
- FC zombies, skeletons, creepers and animals walked on top of water and lava at land speed and
  never swam or sank ("zombies casually walking on water"). They also stood on tall grass, flowers,
  crops, fire, cobwebs, sugar cane, vines, redstone dust, levers, buttons, pressure plates, signs,
  tripwire and portals, and collided with single slabs, repeaters and comparators as full blocks.
  Fence gates and slabs were full cubes; FC ladders, chests, slabs, siding, mouldings and axles
  collided as full cubes or as whatever shape was last traced anywhere.
- Small FC mobs pressed into passable cells, or against a chest, took an FC inWall hit every tick:
  hurt-sound spam, reset breeding love, wolves un-sitting.
- Below y=0 (deepslate caves, a superflat at -60), FC mobs stood still: they did not chase or attack
  and barely wandered.

Root cause:

- The Modern-Common EntityAITasks reset clashing running tasks before the new task's startExecuting
  and never ran updateTask on the start tick. FCMOD's EntityAIAttackOnCollide.resetTask clears the
  attack target, so EntityAICreeperSwell.startExecuting always captured a null target and stopped
  the next tick, every 3 ticks. 1.5.2 (EntityAITasks.java:69-194) starts the new task first, ticks
  it the same tick, and drops the clashing task on a later full pass.
- Modern-Common Block subclasses lacked the 1.5.2 collision overrides (for example
  BlockFluid.getCollisionBoundingBoxFromPool returning null, BlockFluid.java:182-185) and their
  isOpaqueCube()/renderAsNormalBlock() == false pairs, so fluids and non-solid blocks inherited
  Block's full cube and Block.isNormalCube called them solid. BlockFluid.getFluidHeightPercent was
  inverted ((8-meta)/9 instead of (meta+1)/9). Block.getCollisionBoundingBoxFromPool and
  getSelectedBoundingBoxFromPool read the shared minX..maxZ fields instead of FCMOD's
  GetBlockBoundsFromPoolBasedOnState(...).offset (Block.java:3373-3376, 3769-3772); canCollideCheck
  returned true instead of isCollidable(); CollisionRayTraceVsBlockBounds was a `return null` stub.
- Frozen pathing assumed y >= 0. PathNavigate.getPathableYPos truncates with `(int)` toward zero, so
  a mob at y=-59 was reported at -58 while its path points sat at -59; pathFollow never advanced and
  the stuck check cleared the path (the main cause). getSafePoint's drop-to-ground scan is bounded
  by `par3 > 0`, and ChunkCache returned air outside [0, 256).

Fix (Modern-Common unless noted):

- EntityAITasks.onUpdateTasks, canUse and areTasksCompatible are ported 1:1 from 1.5.2, keeping the
  per-task try/catch and the once-per-JVM [AI-TASK-FAIL] log.
- Null collision for BlockFluid, BlockFire (isCollidable false), BlockReed, BlockWeb (with
  setInWeb), BlockVine, BlockRedstoneWire, BlockLever, BlockPortal, the BlockFlower family,
  BlockButton, BlockBasePressurePlate, BlockSign, BlockTripWire and BlockTripWireSource; BlockCactus
  inset boxes, the BlockSoulSand 7/8 box, the BlockLilyPad boat exclusion and an empty
  BlockEndPortal list; FCMOD constructor bounds for BlockFlower and BlockRedstoneLogic;
  isOpaqueCube/renderAsNormalBlock false for every class made passable and for BlockChest
  (BlockChest.java:39-50). getFluidHeightPercent is (meta+1)/9.
  BlockFlower.onEntityCollidedWithBlock applies the FCBlockPlants 0.8 on-ground damping, and
  BlockCactus applies attackEntityFrom(cactus, 1), since MC's CactusBlock.entityInside never runs
  for puppets.
- Block collision, selection and the FCMOD ray trace (CollisionRayTraceVsBlockBounds,
  MouseOverRayTrace, and collisionRayTrace for classes that override
  GetBlockBoundsFromPoolBasedOnState) use the per-position state bounds; a throwing or null override
  falls back to the fields with one [BLOCK-BOUNDS] warning per class. canCollideCheck returns
  isCollidable().
- BlockFenceGate gets the 1.5.2 collision (none when open, a 1.5-high box when closed) and
  getBlocksMovement; BlockHalfSlab gets the FCMOD bounds (double full, bit 8 top half), isOpaqueCube
  = isDoubleSlab, HasLargeCenterHardPointToFacing and the ground-cover and spawn hooks;
  BlockRailBase gets GetBlockBoundsFromPoolBasedOnState (ascending 0.625, else 0.125; collision
  stays null). These read the gate and slab metadata that (ag) now derives; the first pass had
  withdrawn them because the bridge could not yet supply it (AP-1, AP-4, AP-7).
- Pathing: new World.getFcMinBuildHeight/getFcMaxBuildHeight (0/256; WorldBridge returns the level's
  range). Modern-Common ChunkCache is a full port that clips to that range and is now the only copy
  (removed from the remapFcCode whitelist, both shadow lists and openUpEntityAccess). New Forge
  build task extendFcPathingBuildRange (ASM, idempotent, fails the build on an unexpected bytecode
  shape) rewrites getSafePoint's two `iload_3; ifle` checks to `par3 >
  worldObj.getFcMinBuildHeight()` and every truncating y cast in PathNavigate (9), PathFinder (1)
  and EntityAIFleeSun (1) to MathHelper.floor_double. World.getPathEntityToEntity floors
  target.posY, and GetBlockNaturalLightValue_do clamps to the build range.
  WorldBridge.getHeight/getActualHeight return the build top (320), not the span (384).

Verification: compile and audits. LinkAudit's FC BYTECODE PATCHES line reports the pathing patch
applied, and the build log shows extendFcPathingBuildRange recognising its patched classes; javap
shows invokevirtual World.getFcMinBuildHeight in getSafePoint and no d2i left in PathNavigate, and
both classes pass `java -Xverify:all`. The creeper and water cases were traced through the code: the
swell task now starts while the target is still set, sets creeperState 1 in the same tick and keeps
running after AttackOnCollide is dropped; with a null fluid box frozen Entity.handleWaterMovement
finds the water and EntityLiving takes the water branch. In game (not yet run): a creeper hisses and
explodes at a survival player; `/summon betterthanwolves:fc_zombie ~ ~3 ~` over water splashes in
and bobs; an FC mob held under source water starts drowning after about 15 s; FC mobs stand 0.125
high on a repeater and 0.5 on a bottom slab; a closed fence gate stops them and an open one lets
them through; chickens in tall grass take no damage and move slower; a mob pushed into a cactus
takes damage; on a superflat at -60, FC zombies chase and hit the player and mobs do not walk off
ledges.

Caveats: the scheduler change (start before drop, update in the start tick) applies to every FC AI
task, not just creepers. BlockPistonMoving collision is not ported (the bridge has no
TileEntityPiston). Not ported: pressure plate and tripwire triggering, nether and end portal travel,
and soul sand damping (left out on purpose). Single slabs are no longer normal cubes, as in 1.5.2:
FC falling blocks fall off a bottom slab, FC attachments hold only on a slab's solid face, and
single slabs do not rotate on turntables. GetFixedBlockBoundsFromPool still returns the shared
mutable fields, so some FC default-subtype overrides can see last-written bounds (pre-existing).
Other frozen `(int)` y casts outside pathing are exact on the ground and off by one only for
fractional negative y (EntityMob.CanSpawnOnBlockBelow, EntityAnimal.GetGrazeBlockForPos, Entity
dismount); EntityAIOcelotSit's casts are authentic 1.5.2. Frozen Entity still kills entities below
y=-64. LinkAudit confirms only the getSafePoint part of the patch; the d2i rewrites rely on the
task's own exact-count check.
*(2026-10-28: pressure plates trigger since (bt), by 4.B's mob types. One FC truncating y cast outside pathing is
floored since (bi), FCBlockNetherrackFalling.onStartFalling, whose position is always fractional; a javap scan lists
FC's other 14, left as FC wrote them.)*

## 2026-10-10 (ag) Block-id mapping: modern-block stand-ins, translated aux-SFX (ghast shrieks), portal hook

Symptom (triage GH1, GH3, GH5, HAI-3, HAI-4, HAI-7, D4, D5, RE4, RE5; design U4, U4-b,
PATHFINDER-NPE; reviews BM-R1..R4 and the wave-3 Modern-Common fixer):

- "Constant ghast noises in the Overworld": every naturally spawned bat shrieked GHAST_WARN on
  take-off (volume 10, Hostile category, audible within 64 blocks, subtitle "Ghast shoots"). A
  zombie infecting a villager played the ghast fireball; door and fizz sounds played as an ender-eye
  launch and a firework; ghast charge and fireball were silent.
- Pathfinding NPEs (`"tempBlock" is null` at PathFinder.GetPathWeightAtLocation), so mobs near hay,
  terracotta, carpet or coal blocks never pathed to the player.
- Puppets fell through deepslate, tuff, granite and other new blocks to bedrock and walked through
  walls of them (cave mobs below y=0 sat inside the rock and never saw the player); penned animals
  hopped fences and walls.
- FC squids in seagrass, kelp or waterlogged cells read inWater=false, froze and drowned FC-side; FC
  entities felt a current at coral reefs.
- Creeper blasts ignored mobGriefing=false and were not credited to the creeper.
- FC saw wrong metadata for gates, slabs, stairs and trapdoors (east MC stairs turned upside down on
  an FC turntable; closed west/east trapdoors read as open).
- After a restart, FC containers lost or swapped post-1.5.2 items, and modern-only spawn eggs were
  stuck in FC containers.
- Overworld nether portals spawned zombified piglins, never seeded possession and never set the
  nether-accessed flag.
- MC snow layers on stone, dirt or grass vanished whenever a neighbouring block changed, and burning
  leaves or wood cinders left no ash on ordinary ground.

Root cause:

- 1.5.2 aux-SFX ids were passed raw to ServerLevel.levelEvent at all three WorldBridge entry points,
  but 1.20.1 renumbered them: 1015 is GHAST_WARN, and bat take-off moved to 1025. Bats run the
  frozen 1.5.2 EntityBat, and 1.20.1 spawns bats only with open air above, so every one shrieked on
  its first AI tick, in overlapping packs.
- ProxyRegistry mapped ids 95/137/152/159/170-173 to empty blocksList slots; every 1.20.1-only state
  read as air on FC's world-read path; WorldBridge.getCollidingBoundingBoxes dropped 1.5.2's
  floor(minY)-1 start.
- No material query read FluidState. Explosions used a null source and ExplosionInteraction.BLOCK,
  and stored createExplosion's flag (isSmoking) as isFlaming.
- deriveVanillaMetadata sent gates, stairs and trapdoors through the generic HORIZONTAL_FACING
  branch and ignored SlabBlock.TYPE.
- Headless item ids (25000+) were allocated per JVM in first-seen order and never persisted, while
  FC NBT stores numeric ids; all spawn eggs mapped to 383; getItemId fell back to the raw MC
  registry id, which indexes unrelated FC items.
- 1.20.1 NetherPortalBlock overrides randomTick without calling super, so BlockBehaviorMixin's
  routing never reached FCBlockPortal.updateTick.
- Modern-Common Block.CanGroundCoverRestOnBlock was a `false` stub (1.5.2 Block.java:2590-2598
  checks for a solid top surface). MC snow layers route to FCBlockSnowCover through
  BlockBehaviorMixin, whose onNeighborBlockChange then removed every layer.

Fix:

- New AuxSfxMapping translates every 1.5.2 playAuxSFX/broadcastSound id at all three entry points:
  1015 -> 1025, 1016 -> 1026 (plus FC's extra moan), 1017 -> 1027, 1007 -> 1015, 1008 -> 1016, 1009
  -> 1018, 1004 -> 1009 with data 0; 2001 sends Block.getId(state) and 2002 the potion colour; the
  door sound (1003) is played directly. Nothing is forwarded raw any more.
- ProxyRegistry.getBlockId returns 0 unless blocksList[id] exists. New
  ProxyRegistry.getWorldBlockId, used by WorldBridge.getBlockId, ProxyBlock's shapes and
  FCBakedModel.LiveAccess: double slabs read as 43/125; colliding modern-only states read as a
  ModernBlockStandIn (solid cube = 160, shaped collider = 161, allocated lazily from the free ids
  159-174; wither-immune full blocks read as bedrock 7). A stand-in takes its collision, path rules,
  hardpoints, hardness and blast resistance from the live MC state, cannot be moved by FC pistons,
  is inert to the saw and block dispenser, drops MC loot, and is never written. Non-colliding
  water-bearing states (seagrass, kelp, bubble columns) read as water.
  *(2026-10-18: no longer inert to the saw by rule. Since compat wave C1 the stand-in's FC machine hooks
  (FCBlockSaw's 7-argument OnBlockSawed, GetDoesStumpRemoverWorkOnBlock, AttemptToApplyFertilizerTo) ask a hook
  an integration registered for the live state (ModernBlockHooks), and answer FC's inert false only when none
  applies. Since C2 they are live for Dynamic Trees (INTEGRATIONS.md #027): the saw fells full-width trunks, the
  stump remover clears dead stumps, bone meal fertilizes the rooty soil. A block whose cached collision shape is
  empty is now asked again at its live position, so a Dynamic Trees trunk shell reads as stand-in 161, not air.
  See 2026-10-18 (au).)*
- getCollidingBoundingBoxes scans from minY-1. handleMaterialAcceleration, isAnyLiquid and
  isMaterialInBB also count waterlogged water, and unmapped water-holders always report metadata 0
  (source).
- Explosions use the FC entity's proxy as the source, honour isFlaming, and use interaction NONE
  when not smoking, MOB for mob sources and BLOCK otherwise.
- `public static deriveVanillaMetadata(BlockGetter, BlockState, BlockPos)` never throws and encodes
  gates (direction | 4 when open), slabs (species | 8 for top), stairs (0=E, 1=W, 2=S, 3=N, | 4
  upside down), trapdoors (0=N, 1=S, 2=W, 3=E, | 4 open, | 8 upper half) and the flattened subtypes;
  applyFcMetadataToVanillaBlock has matching write branches, and resolveVanillaBlockState places the
  subtype variant. vanillaMetaOverrides remember which MC block FC wrote to and are dropped once MC
  replaces it; the map is concurrent.
- Headless ids persist per world (data/btw_headless_items.dat), are restored at the overworld's
  LevelEvent.Load before any chunk loads and reset at server stop; the world's saved numbering wins
  a conflict. The item maps are concurrent (RE4). Eggs of non-1.5.2 mobs are headless;
  getOrCreateLegacyId checks 1.5.2 item ids before BlockItems; getItemId returns 0 for unmapped
  items.
- New NetherPortalBlockMixin routes randomTick to FCBlockPortal.RandomUpdateTick and always cancels
  vanilla, since BTW suppressed portal pigmen (FCBlockPortal.java:42-54).
  WorldProvider.isSurfaceWorld excludes the End (WorldProviderEnd.java:55-58) and canRespawnHere is
  false there.
  *(2026-10-14: the NetherPortalBlockMixin part is superseded by 2026-10-14 (as): folded into
  TickPolicy. The mixin is deleted. The portal's random tick falls under the general rule
  (Modern-Common BlockPortal sets the 1.5.2 random-tick flag) and is dispatched at the BlockState
  level, where the class mixin would never fire; with both, FCBlockPortal.updateTick would roll
  twice per selection. The pigman rationale moved to the TickPolicy javadoc. Proof P of (as), run
  1: no zombified piglins, portal intact. The WorldProvider changes above stand.)*
- Block.CanGroundCoverRestOnBlock ports 1.5.2 (`world.doesBlockHaveSolidTopSurface`).
- setEntityState and explosions find replacer- and MC-spawned puppets by entity id ((ab));
  playSoundAtEntity defers puppets' self-hurt sounds ((aa)).

Verification: server smoke test: both runs log "[STAND-IN] modern-only blocks read by FC as legacy
160 (solid cube) / 161 (shaped collider)" followed by per-block lines (minecraft:copper_ore and
granite -> 160 in both runs; dirt_path -> 161 in the second run only), no `"tempBlock" is null`
lines and 0 ERROR lines. In game (not yet run):
`/summon minecraft:bat` in cave air gives a faint flutter with the subtitle "Bat takes off", not
"Ghast shoots"; cows and zombies stand on a floating deepslate, tuff or cherry-plank platform and
stay inside oak-fence, cobble-wall and closed-gate pens; an FC squid in a kelp pool keeps swimming;
with mobGriefing false a creeper blast leaves blocks intact and the death message says "by Creeper";
east-facing MC stairs rotate correctly on an FC turntable; closed west/east trapdoors stop FC mobs;
after a reload "[HEADLESS] restored N headless item ids" is logged and a copper ingot in an FC
hopper survives a full restart; an Overworld portal next to penned FC animals logs 'possession 0->1'
within minutes, spawns no zombified piglins and saves FCNetherAccessed; snow next to a newly placed
block stays.

Caveats: puppets inside modern solids trip FC suffocation, which (aa) reverts. Stone step sounds
play on modern wood, packed and blue ice have normal friction to FC, and no-collision climbables
stay air. An eye inside a waterlogged stair or slab is not "in water" FC-side, because frozen
Entity.isInsideOfMaterial reads getBlockId. Dynamite, vases and TNT still ignore mobGriefing, as in
1.5.2. Headless ids allocated before this fix cannot be recovered, and FC stacks created in memory
before a world loads keep an evicted id (transient). A vanillaMetaOverrides entry is still trusted
when MC changes a property of the same block (crop AGE through bone meal). Overworld possession,
with its squid -> ghast and other transformations, is reachable for the first time in the port, so
`[BTW-Possession]` and `[GHAST-SPAWN]` lines near player-built portals are now expected (see the (f)
correction). WorldBridge.doesBlockHaveSolidTopSurface uses MC isFaceSturdy(UP), which is a little
more permissive than 1.5.2 (ground cover can rest on glass). Not addressed: the particle-only aux-FX
2241 and 2282 ((l)), and triage GH6 (millstone and wet-cement display-tick sounds still go through
the client adapter's no-op playSoundEffect).
*(2026-10-31: no longer: since (bw) WorldBridge.doesBlockHaveSolidTopSurface answers 4.B's rule, the
FC block's own large centre hard point on top, and keeps 1.20.1's sturdy face only where FC reads
air; ground cover no longer rests on glass.)*

## 2026-10-10 (ah) Replaced mobs run FC spawn init and keep state; 1.20.1 variants; swamp witches; wither

Symptom (triage SW1, SW5, HAI-6, GH2; design U5, U5b; reviews W2-mobs-R1..R6):

- Skeletons had no bows and fought in melee ("skeletons without bows"); FC sheep were all white;
  every villager was a farmer; no zombie wore armour; skeleton-trap horsemen and spider jockeys lost
  their bows and mounts.
- Name tags, baby age, tame owner, collar and sitting state, sheep colour, profession, equipment,
  persistence and health from pre-mod saves were dropped.
- The FC soul-urn golem and FC silverfish appeared client-side as a vanilla villager or zombie, and
  became fc_villager or fc_zombie after a reload.
- Drowned, husks, zombie villagers, strays, wither skeletons and glow squid stayed vanilla 1.20.1
  mobs.
- Witches spawned in almost every Overworld biome, each idling with ghast moans (a minor second
  source of the ghast-noise reports).
- Wither side heads never turned, withers had no boss bar, and a summoned wither could despawn.
- Feeding an FC animal did not use the item up.
- FC villagers, snowmen and mobs holding picked-up loot vanished once the player was more than 128
  blocks away.
- (Found in review) spawners spawned replaced mobs without limit, and raids with witches stalled.

Root cause: VanillaMobReplacer swapped positions only. It never ran FC's 1.5.2 spawn initialiser
(PreInitCreature + initCreature at SpawnerAnimals.java:203/211/272, PreInitCreature +
SpawnerInitCreature at MobSpawnerBaseLogic.java:137/219, initCreature at
ItemMonsterPlacer.java:104), mapped no state, and ran inside EntityJoinLevelEvent, which fires mid
chunk promotion for worldgen entities. ForgeRegistries.ENTITY_TYPES.getValue() returns PIG for a
missing key, and EntityProxyFactory gave unregistered living FC classes the VILLAGER/ZOMBIE/PIG
types. The 1.20.1-only zombie and skeleton subtypes are separate EntityTypes and were missing from
REPLACEMENTS. 1.20.1 monsters() gives witches weight 5 everywhere, while BTW adds FCEntityWitch to
swamps only (BiomeGenSwamp.java:37, weight 1). SoundMapping sent "mob.ghast.affectionate scream" to
GHAST_AMBIENT (the moans) instead of GHAST_SCREAM, the file 1.5.2 played. FC interact edits the held
stack in place and nothing wrote it back. Mob.removeWhenFarAway ignored FC canDespawn and
persistence. BaseSpawner counts the vanilla class (BaseSpawner.java:125), and Raid.updateRaiders
only drops removed raiders.

Fix:

- VanillaMobReplacer: the join handler cancels the vanilla mob, drops JOCKEY extras (and
  spider-jockey skeletons) and queues the rest; the queue drains at ServerTickEvent END and on
  ServerStopping and discards every queued vanilla entity, so raids no longer wait on cancelled
  witches. Each mob becomes its fc_* proxy with the same position, rotation and UUID, with FC state
  set up before addFreshEntity.
- Fresh spawns run FC init: PreInitCreature for natural, worldgen and spawner spawns, then
  SpawnerInitCreature for spawners (BTW spawner skeletons get no bow) and initCreature otherwise;
  villagers keep their building profession.
  *(2026-10-28: they took FC's random profession (1.20.1's village templates place unemployed, nitwit and baby
  villagers); since (bm) a village villager takes its building's job site, as a villager zombie in a survival world.)*
- Disk loads, breeding, conversion, buckets and NBT summons carry state instead: name, persistence,
  health share, age, fleece colour and sheared, saddle, slime size, wolf
  tame/owner/sit/angry/collar, profession, wither summon charge, and equipment in 1.5.2 slot order
  with its 1.20.1 drop chances (picked-up player gear stays a guaranteed drop).
- Variants: zombie villager, husk and drowned become fc_zombie (zombie villagers keep the villager
  look only on transfer paths or SPAWN_EGG/COMMAND/DISPENSER spawns; natural, worldgen, spawner and
  structure ones become plain FC zombies, as BTW never spawned villager zombies); stray and wither
  skeleton become fc_skeleton (wither skeletons get setSkeletonType(1) before init, so they carry
  the stone sword); glow squid becomes fc_squid. MC health and the health link of (aa) are re-seeded
  after init.
  *(2026-10-28: BTW's own spawns never roll villager zombies; village ones keep their identity since (bm).)*
- New fc_iron_golem and fc_silverfish types (U5b; renderers in (ad)); EntityProxyFactory refuses
  unregistered living classes (WARN once) and ignores ItemEntityBridge views.
- Witches: new biome modifiers remove_witch_outside_swamps.json (`#minecraft:is_overworld` minus
  `#forge:is_swamp`) and add_swamp_witch.json (weight 5, group 1-1); with vanilla's own 5 the swamp
  share is 10/521, 1.5.2's 1/53. "mob.ghast.affectionate scream" maps to GHAST_SCREAM.
- Wither: FC withers get a ServerBossEvent (purple, no darkening) and never despawn (1.5.2
  EntityWither.despawnEntity override); the head angles are animated by (ab).
- ProxyMob and ProxyPathfinderMob removeWhenFarAway respect FC canDespawn/GetIsPersistent;
  mobInteract writes FC's held-stack edits back.
- Spawner cap: a new onFinalizeSpawn handler counts FC entities of the replacement type plus pending
  queue entries inside the spawner's 1.20.1 box (SpawnRange and MaxNearbyEntities from the spawner
  NBT, default 4 and 6) and cancels the spawn at the cap.

Verification: server smoke test: both runs log "Registered 54 BTW entity types with Forge
registries." and "[MobReplacer] minecraft:pig/sheep/cow/chicken -> betterthanwolves:fc_*
(CHUNK_GENERATION)" with 0 ERROR lines; only the second run also logs "[MobReplacer]
minecraft:villager -> betterthanwolves:fc_villager (STRUCTURE)". In game (not yet run): a new world has sheep in mixed colours,
skeletons that shoot arrows, villagers in varied robes and some armoured zombies; `/summon
minecraft:sheep ~ ~ ~ {Color:14,CustomName:'"Bob"',PersistenceRequired:1b}` gives a red, named FC
sheep that persists; `/summon` of wither_skeleton, husk, drowned, zombie_villager, stray and
glow_squid gives FC mobs; no witches spawn outside swamps and a swamp witch's idle sound has no
"Ghast cries" subtitle; a summoned wither keeps its boss bar beyond 128 blocks; feeding wheat to an
FC cow uses one wheat; mobs at a dungeon spawner plateau at 6; a raid with a witch wave completes.

Caveats: riding is not carried over, and vanilla villager trades, gossip and XP are dropped. 1.20.1
finalizeSpawn results (vanilla armour, baby zombies, herd babies) are discarded on purpose. A mob
whose chunk unloads within the one-tick deferral is dropped (logged once per type). Drowned spawns
give FC zombies in water, and igloo and abandoned-village zombie villagers lose their villager
identity (deliberate, per BTW). Vanilla iron golems and silverfish are still not replaced. The witch
swamp weight matches 1.5.2 by share, not by the literal weight 1. The spawner cap uses 1.20.1's box,
reads the spawner NBT on every attempt, and cancelled attempts still play the spawner's 2004
particle event. U5 (wave 2) landed after U6 ((x), wave 1), so armed skeletons no longer hit the
frozen equipment-loop cast that froze witches (the ordering the triage required for HAI-6).
*(2026-10-28: igloo zombie villagers still lose their villager identity (a separate ruling); village ones keep it, and
new villages place no iron golem, since (bm).)*

## 2026-10-10 (ai) Inventory write-back, variant mapping, FC/MC enchantments, live container slots

Symptom (design U1, U1b, U2; reviews INV-R1..R5, CM-R2):

- Right-clicking any BTW block, pressing an FC container button (Infernal Enchanter) or hitting a
  mob with an FC tool changed items elsewhere in the inventory: orange wool became a random colour,
  spruce/birch/jungle logs became oak, names and NBT were dropped, spawn eggs were deleted ("Cannot
  convert FC ItemStack with legacy ID 383"), FC tool wear was reverted, and written-back stacks
  picked up a stray {Damage:0} and stopped stacking. Variants stored in FC baskets, hoppers and
  chests, or dropped from the crafting grid, collapsed too.
- The Infernal Enchanter took the scroll and XP but did not enchant tools or armour (books worked).
- Shift-clicking into a partial FC container slot deleted the merged items, and a partial
  shift-click out duplicated them; shift-crafting in the shipped jar gave the item without consuming
  the grid (NPE in SlotCrafting.onCrafting); buckets that a recipe should return vanished; pressing
  Q over an FC slot holding an item MC cannot display deleted one item per press.
- MC enchantments (Efficiency, Unbreaking, Silk Touch, Fortune, Looting, Power, Infinity, ...) had
  no effect in FC code.

Root cause:

- InventoryBridge.writeBackAll rebuilt all 40 MC slots from the FC snapshot through a lossy mapping:
  wrapItemStack sent orange wool to (35,n,0) and a spruce log to (17,n,0); toMcStack sent (17,0) to
  OAK_LOG, had no mapping for 383, and applied damage before copying the stale tag, whose Damage
  wins in 1.20.1.
- ForgeNBTCompound kept FC's setTag lists twice (the live tagMap object and a set-time snapshot in
  the CompoundTag), and getTagList returned a detached copy for an empty list, so 1.5.2
  ItemStack.addEnchantment's append was lost. Every damageable 1.20.1 item carries tag.Damage, so it
  is always a ForgeNBTCompound.
- InventoryAdapter.getItem returned a fresh copy on every call, but 1.20.1 menus edit Slot.getItem()
  in place and only call setChanged() (AbstractContainerMenu.java:612-627), as 1.5.2 containers did.
  quickMoveStack passed the drained EMPTY stack to onTake, so FC got onPickupFromSlot(null).
  Slot.tryRemove runs remove() before it checks for an EMPTY result.
- FC reads only the 1.5.2 "ench" list, never 1.20.1 "Enchantments".

Fix:

- InventoryBridge.sync() records a per-slot baseline (FC object, MC object, id/size/damage, and a
  copy-free tag comparison that ignores Damage). writeBackAll and the
  setInventorySlotContents/decrStackSize/getStackInSlotOnClosing write-throughs write only the slots
  FC changed. A same-item change is patched onto the live MC stack (ItemStackHelper.applyFcState:
  tag, durability on damageable items only, count); only a different item or subtype is rebuilt,
  matching 1.5.2's in-place edits (InventoryPlayer.java:214,250). writeBack adopts a detached FC
  stack only while the slot still holds the stack sync() handed out. Slots that fail to convert are
  locked; an item FC puts into one is moved to a slot both sides see as empty, or dropped at the
  player, with a rate-limited [INV-WB] WARN. toMcStack resolves the item first, then copies NBT,
  then applies durability, and strips a stale Damage tag from non-damageable items.
- New LegacyVariants maps 1.5.2 subtypes both ways: wood species 5/6/17/18/126, sandstone, tall
  grass, wool, stone slab, silverfish, stone brick, wall, anvil, quartz, coal, golden apple, dye,
  skull, and spawn eggs by EntityList id (misses cached).
- ForgeNBTCompound: getTagList returns the live tagMap list whenever there is one, and registers a
  list it built from the CompoundTag as the live object, as 1.5.2 returns the stored instance
  (NBTTagCompound.java:353-363); mergeTagMapInto lets FC's live lists and compounds win; copy() goes
  through toMcTag; getCompoundTag/setCompoundTag keep tagMap consistent.
- InventoryAdapter hands out one mirror stack per slot and folds MC's in-place edits back into FC on
  setChanged; FCContainerMenu flushes the mirrors at the start of each broadcastChanges and in
  removed(). quickMoveStack moves into accepting slots on the other side (never result or output
  slots) and passes FC a copy of the original stack at the remaining size (1.5.2
  FCContainerWorkbench.java:94-108). ContainerMappedSlot wraps onPickupFromSlot and then flushes the
  player inventory; mayPlace and mayPickup refuse a slot holding an FC item MC cannot display.
- ItemStackHelper.toFcStack adds a live "ench" mirror of "Enchantments"; foldFcEnchantments reverses
  it (an unchanged mirror is dropped, FC changes replace only the entries FC can express, levels are
  not cast to byte), and InventoryBridge fingerprints compare the folded form.

Verification: compile and audits only; no inventory was exercised (no client, no player). In game
(not yet run): inventory items survive BTW-block right-clicks unchanged and the 383 warning is gone;
FC tool hits keep wearing durability; orange wool and spruce logs keep their variant through FC
containers and grid drops; the Infernal Enchanter enchants an iron pickaxe (keeping an existing
Sharpness on a sword), and the enchantment survives two save/reload cycles; chest test A (10 in the
slot plus a shift-click of 30 gives 40 after reopening) and test B (a partial shift-click out of 64
leaves 50); shift-crafting consumes one ingredient set per craft; a cake recipe returns 3 buckets;
an Efficiency/Unbreaking pickaxe digs FC stone faster and `/data get entity @s SelectedItem` shows
each enchantment once.

Caveats: modern-only variants that collapse onto one legacy id (blue orchid, wet sponge, carved
pumpkin, the bed colours, and the modern black/brown/blue/white dyes, which come back as ink sac,
cocoa, lapis and bone meal) keep their identity only while FC leaves them alone. If FC and MC both
change a slot between a sync and a write-back, FC wins. Shift-click ranges are still MC-generic
rather than FC transferStackInSlot (open decision). A slot holding an FC item with no MC mapping is
inert until the mapping exists. ForgeNBTCompound.setTag(key, ForgeNBTCompound) still stores the
child's raw CompoundTag without its tagMap, which affects FC stacks written straight into a
ForgeNBTCompound (FcWorldSavedData slots). Enchanted-book StoredEnchantments are not mirrored. FC
now sees MC enchantments: authentic, but noticeable (Silk Touch, Fortune and Looting apply to FC
drops, MC-enchanted ProxyItems glint, and the Infernal Enchanter counts existing enchantments).
*(2026-10-20: the modern dyes no longer collapse: white dye since 2026-10-20 (aw), black, brown and blue dye since
(bc), as FC 351:271, 256, 259 and 260. A broken bed or door drops its own 1.20.1 item, a bed in its colour, since
(bb), though a bed item FC holds still collapses to the red bed. Flowers still collapse when broken, popped or
placed by FC (triage CF2-DR3, CF2-P8c).)*
*(2026-10-28: the blue orchid no longer collapses: since (bk) the small flowers are subtypes 38:1-10 and FC self-drops
keep their block's identity. Wet sponge, carved pumpkin, the stone slab and smooth sandstone still collapse in FC
storage and FC placement.)*

## 2026-10-10 (aj) Item use: one arrow per bow shot, FC right-click once, entity items, 1.5.2 food values

Symptom (reviews IP-R1..R6, IU-R1..R5, FD-R1, FD-R3, FI-R1..R4):

- The vanilla bow fired two arrows per release (FC's and vanilla's); the FC composite bow could not
  be drawn; composite-bow and broadhead arrows vanished when picked up.
- Every ProxyItem right-click ran FC twice (soul urns and dynamite threw two entities for one item;
  FC buckets and the baited fishing rod acted twice), FC right-click items were never used up, and
  off-hand items ran FC against the main-hand stack.
- The breeding harness, dung and dye did nothing to FC animals.
- In creative, FC seeds, hoes and flint and steel used on blocks were consumed or damaged.
- Food: FC-replaced foods (raw meats, melon, mushroom stew, rotten flesh, and, once the 1.5.2 item
  ids were resolved first, carrots and potatoes) restored hunger twice (a carrot gave 12/60 instead
  of 3). BTW's rebalanced vanilla foods restored 1.20.1 nutrition x3 with every vanilla effect
  cancelled (bread 15 instead of 9, steak 24 instead of 15, cookie 6 instead of 3, golden apple 12
  and no effect at all instead of 3 plus Regeneration I for 5 s; spider eyes did not poison), and
  cookies and pie could not be eaten when full. FC soups never returned their bowl and mushroom stew
  could give two. A vanilla potion with a full inventory gave two bottles. FC chocolate milk lost its
  bucket. In creative, chocolate milk was used up, eating an FC-replaced food played FC's "can't
  eat" sound, and a full inventory got a free bowl.
- Interim states inside this pass, fixed below: resolving 1.5.2 item ids first let one pumpkin seed
  feed forever (review IU-R1); the wave-4 effect handshake gave golden apples 1.20.1's Regeneration
  II and Absorption (review FD-R1); and the wave-5 ItemFood registration briefly extended the
  creative "can't eat" sound to most foods (review FI-R4).

Root cause: ItemStackMixin.btw$releaseUsing ran FCItemBow without cancelling vanilla, and
ProxyItem.releaseUsing ran FC again. Modern-Common ItemBow lacked setMaxDamage(384),
getMaxItemUseDuration 72000 and EnumAction.bow (ItemBow.java:26, :106-117). ProxyEntity.playerTouch
never wrote the inventory back after EntityArrow.onCollideWithPlayer.
ServerPlayerGameModeMixin.btw$useItem dispatched FC for ProxyItems that ProxyItem.use already
dispatches, and for the off hand; FC's in-place edits stayed in the PlayerBridge snapshot. ProxyItem
called useItemOnEntity while the shipped Client FC items override itemInteractionForEntity, and
passed a LivingEntityBridge, so `instanceof FCEntitySheep` never matched; FCItemDye had no entity
hook. btw$useItemOn pre-empted 1.20.1's creative restore without doing 1.5.2's. The mixins resolved
every BlockItem by its block (carrot -> crop 141, not FC food 391). FoodDataMixin cancelled vanilla
nutrition on the deprecated 2-arg FoodData.eat, which Forge's Player.eat (Player.java:1996) never
calls; LivingEntityMixin cancelled vanilla effects for any item with an FC id, including golden
apples and spider eyes that FC never fed. Modern-Common registered BTW's rebalanced vanilla foods as
plain Item, so FC never fed them. FC soups add the bowl through the snapshot-only
addItemStackToInventory, which was never written back; ProxyItem discarded the stack FC's onEaten
returned; Modern-Common EntityPlayer.canEat refuses invulnerable players while 1.20.1 lets them eat;
Modern-Common InventoryPlayer lacked 1.5.2's creative branch.

Fix:

- Bow: ItemStackMixin.btw$releaseUsing is not cancellable. For the vanilla bow, FC is skipped
  whenever vanilla has something to fire (creative, Infinity, or a non-empty getProjectile), so the
  one arrow is vanilla's; FC runs only for a rotten-arrow draw, then writes back and wears the bow,
  sending broadcastBreakEvent before the slot empties. Modern-Common ItemBow gets the three 1.5.2
  members and the interim wear is removed. ProxyEntity.playerTouch writes the inventory back.
- Right-click: btw$useItem skips LegacyProxyItem stacks and the off hand; ProxyItem.use is main-hand
  only and applies FC's returned stack to the held slot (1.5.2
  ItemInWorldManager.tryUseItem:338-361) with a selective write-back. btw$useItemOn is main-hand
  only, restores creative damage and size, and catches FC throwables. New ProxyItem.fcItemFor
  resolves the 1.5.2 item id before the block.
- Entity items: new ProxyItem.interactWithEntityFc tries itemInteractionForEntity, then
  useItemOnEntity, on the puppet's own FC entity and applies the size change; it serves
  interactLivingEntity and a new ItemStackMixin hook for dyes (server only).
- Food: FoodDataMixin injects into the Forge 3-arg eat (remap = false). Vanilla nutrition and
  (LivingEntityMixin) vanilla effects are cancelled only when PlayerBridge.fcFedForEat(item, eater)
  is true; the bridge's FoodStats subclass records the ItemFood that 1.5.2 ItemFood.onEaten fed this
  tick, so every food restores exactly once. Item.initializeVanillaItems registers every FCMOD
  vanilla food as btw.modern.ItemFood with the exact 1.5.2 values (heal, saturation, wolf meat,
  always-edible, potion effect, buoyancy, pig food), including a nested 1.5.2 ItemAppleGold with
  subtypes and Item.poisonousPotato (394). ItemStackMixin.btw$finishUsing shrinks FC foods that
  1.20.1 does not consider edible (pumpkin seeds), writes FC's bowl back for FC-fed BowlFoodItems
  and returns the eaten stack instead of vanilla's bowl, skips FC for any PotionItem, and runs FC on
  a detached copy for an off-hand eat. ProxyItem.finishUsingItem writes the inventory back after
  onEaten, returns a different non-empty result stack (the chocolate-milk bucket) to the hand that
  was used, and skips the shrink in creative.
- Orchestrator follow-ups to the wave-5 review: ServerPlayerGameModeMixin.btw$useItem returns early
  for an FC ItemFood when the player is invulnerable, so creative eating is vanilla and silent
  (FI-R4); Modern-Common InventoryPlayer.addItemStackToInventory ports the 1.5.2 creative branch, so
  a full creative inventory swallows the stack and no free bowl drops (FI-R3a); the food comments in
  PlayerBridge, FoodDataMixin and LivingEntityMixin were refreshed.
- FC calls on player-tick and packet paths (onEaten, onUpdate, hitEntity, tryHarvestBlock) catch
  Throwable and log once (ProxyItem.logFcFailureOnce); a failed harvest still writes back and
  returns true only if the block changed.

Verification: compile and audits only. The FoodDataMixin target was checked against the binpatched
1.20.1 jar with javap (the 3-arg eat keeps its literal name), and the server smoke test showed no
Mixin apply error; no item was used. In game (not yet run): a vanilla bow fires exactly one arrow
that hits vanilla mobs, can be picked up, wears the bow and honours Power, Punch, Flame and
Infinity; the composite bow draws, fires one arrow per shot, loses 1 durability per shot (576 max)
and its arrows return when picked up; a soul urn throws one entity per click; dyeing an FC sheep
uses one dye; FC hunger: bread +9, steak +15, cooked chicken or cod +12, apple, golden carrot and
cookie +3, carrot +3, raw beef +12; a golden apple gives +3 and Regeneration I for 5 s, the
enchanted one Regeneration IV 30 s, Resistance and Fire Resistance; mushroom stew and FC soups give
exactly one bowl; a potion drunk with a full inventory leaves one bottle; chocolate milk leaves an
empty bucket; in creative food is eaten silently with no free bowl.

Caveats: the vanilla bow uses vanilla's arrow choice (any slot, tipped and spectral arrows) and pull
strength, not FCItemBow's hotbar-only rule and GetBowPullStrengthModifier; switching it to the FC
arrow is an open decision. The composite bow shows the draw pose but no pull frames (no
ItemProperties). btw$useItem still lets vanilla run after FC for vanilla items with FC replacements
and does not apply FC's returned stack there, and btw$useItemOn still runs before block activation.
MC-side stack sizes of vanilla-id foods stay at 1.20.1's 64 while FC sees 16, per the port-wide
policy (BTWRegistration.java:125-127). A potion's bottle goes to the hand, as in vanilla, instead of
1.5.2's first free slot. The client briefly predicts a bowl in hand until the server's slot update
arrives.
*(2026-10-20: btw$useItemOn is gone: the block is used first and the FC item at 1.20.1's item step, see 2026-10-20
(ba). btw$useItem still lets vanilla run after FC; FC's edits there are now dropped explicitly inside a shadow
scope ((az)).)*

## 2026-10-10 (ak) FC sees dropped items (ItemEntityBridge); FC projectiles and damage reach vanilla entities

Symptom (design U3, U3b, U3c; reviews ITEMS-R1..R4, FD-R2):

- FC code never noticed dropped items. Animals ignored loose food (FCEntityAIMoveToLooseFood never
  moved, EntityAnimal.CheckForLooseFood never ate); ocelots, spiders, dire wolves and zombies never
  ate meat; villagers never tossed milk. Bellows didn't blow items, and mining charges and cactus
  didn't destroy them. The BTW hopper, cauldron and crucible never collected items dropped on top,
  campfires didn't burn dropped fuel, bone meal did nothing on farmland or planters, and items
  tripped FC tripwires.
- FC arrows, thrown items and fireballs flew through every vanilla mob, boat and end crystal.
- FC skeleton and ghast projectiles hit players as GENERIC damage: no "shot by", no shield, no
  projectile protection. FC mining charges did generic damage too.
- FC block damage (FC cactus) did nothing to vanilla entities.

Root cause: every dropped item is a real MC ItemEntity, and WorldBridge.extractFcEntity only
unwrapped proxies and players, so getEntitiesWithinAABB(EntityItem.class, ...) was always empty. The
block-callback wrappers passed items as a generic EntityBridge, so `instanceof EntityItem` was
false. MC's checkInsideBlocks starts at minY+1e-7, while FCMOD EntityItem.doBlockCollisions starts
at minY-0.01 (EntityItem.java:483-500), so a full-height hopper or vessel under an item never got
the callback. getEntitiesWithinAABBExcludingEntity returned FC entities only, while FC projectiles
hit-test every entity in the box (EntityArrow.java:234-259). LivingEntityBridge.attackEntityFrom
used generic() and always returned true, EntityBridge.attackEntityFrom returned false, and
DamageSourceMapping mapped arrow, thrown, fireball and explosion to generic.

Fix:

- New ItemEntityBridge, an FC-side view of an MC ItemEntity (its runtime superclass is the frozen
  EntityItem; it uses only members both copies declare), stored on the entity through the new
  ItemEntityMixin field (FcItemBridgeHolder). wrap() flushes, then syncs the box, motion, onGround,
  isDead, age, pickupDelay and the FC stack; flush() writes back only FC deltas (the count as a
  delta on MC's live stack, a replaced stack as a full conversion, the pickup delay, velocity and
  position); setDead discards. Flush points are getItem() HEAD, the next wrap, isEntityAlive and
  LevelTickEvent END. onUpdate and onCollideWithPlayer are no-ops because MC owns physics and
  pickup. Both mixin hooks are guarded: a link failure logs one ERROR and disables them for the
  session.
- WorldBridge.getEntitiesWithinAABB, getEntitiesWithinAABBExcludingEntity and getEntityByID expose
  items through the bridge. The wrapEntity helpers in ProxyBlock, BlockBehaviorMixin and BlockMixin
  return the bridge for items, or null (callback skipped) for an item already consumed in the same
  pass, and stepOn skips onEntityWalking for items, as 1.5.2 EntityItem.canTriggerWalking() is
  false. The ItemEntityMixin tick visits the extra floor(minY-0.01) layer for FC blocks that
  override onEntityCollidedWithBlock (U3c). EntityProxyFactory refuses to re-spawn a live bridge.
- Mob loot pickup is live again: the frozen onItemPickup WorldServer cast holds since (x), and the
  item is setDead() through the bridge, so nothing is duplicated.
- FC projectiles (EntityArrow, EntityThrowable, EntityFireball) also get the vanilla entities a
  vanilla projectile could hit (isAlive && isPickable): living ones as the cached
  LivingEntityBridge, others as the new WorldBridge.VanillaProjectileTarget, whose attackEntityFrom
  is MC hurt() with a mapped source. FC projectiles now damage boats and minecarts, break paintings
  and item frames, detonate end crystals and hurt the dragon through its parts; rejected hits
  ricochet as in 1.5.2 (EntityArrow.java:308-347).
- Damage: WorldBridge.toMcDamageSource and DamageSourceMapping.getModern map arrow -> ARROW, thrown
  -> THROWN, fireball -> FIREBALL (UNATTRIBUTED_FIREBALL without a shooter), indirectMagic ->
  INDIRECT_MAGIC, explosion -> EXPLOSION and explosion.player -> PLAYER_EXPLOSION, with the
  projectile as the direct entity and the shooter as the causing entity. New
  DamageSourceMapping.hurtFromFc (server only, never throws) runs MC hurt() and returns its result;
  LivingEntityBridge and EntityBridge use it. PlayerBridge strips 1.20.1 difficulty scaling from
  indirect sources, because 1.5.2 never scaled projectile damage (EntityDamageSource.java:45-48),
  but keeps it for plain explosions.

Verification: compile and audits only. The `[ITEM-BRIDGE] FC view of ItemEntity#..` and `write-back
... count N -> N-1` lines are now DEBUG (orchestrator edit); only failures WARN. In game (not yet
run): hungry chickens walk to dropped seeds and eat one at a time, and when grabbing them mid-meal,
seeds eaten plus seeds picked up equals 64; items dropped on a BTW hopper, cauldron or crucible are
collected (gravel through a soul-sand filter gives sand and flint); bone meal fertilises farmland;
an FC arrow damages a cow (which flees), a zombie targets the shooter, a kill reads "shot by", an
enderman teleports and the arrow bounces; an FC bow breaks boats, item frames and end crystals and
lowers the dragon's health; an FC skeleton's arrow does the same damage on Easy, Normal and Hard,
reads "was shot by Skeleton" and is blocked by a shield; a mining charge does more damage on Hard,
less with Blast Protection and none on Peaceful.

Caveats: loot-capable zombies and skeletons pick up dropped items again (1.5.2), with no pickup
animation (Packet22Collect is ignored); whether the held item shows on the client is unverified.
Only the count, type and replacement are written back to dropped stacks, not FC in-place NBT or age
edits. The frozen 5-HP item health pool is per bridge. New FC item spawns land 0.125 above their
1.5.2 height. checkNoEntityCollision and canPlaceEntityOnSide still count items as obstructions. FC
arrows hurt a perched dragon (vanilla deflects only AbstractArrow there). FC projectiles on players
are not difficulty-scaled (1.5.2 parity, unlike 1.20.1 skeleton arrows). Vanilla mobs wrapped in FC
block callbacks now take FC block damage (1.5.2 parity).
*(2026-10-21: checkNoEntityCollision and canPlaceEntityOnSide follow 1.5.2: only entities that block building
count, so items and XP orbs no longer obstruct; see 2026-10-21 (bd).)*
*(2026-10-28: FC drops spawn with 1.5.2's box since (bj), so they no longer land 0.125 above their 1.5.2 height. FC's
queries and contact callbacks see 1.20.1 minecarts, boats, paintings, orbs and mobs as their 1.5.2 classes since (bu),
whose generic view replaces VanillaProjectileTarget.)*

## 2026-10-10 (al) Recipes (login, /reload, 2x2 grid, table rules), JEI, FC login handshake, entity names

Symptom (triage RE6, CTX-4, CTX-5, CTX-6; reviews RO-1..RO-5, CN-R1, MC2-R2, FD-R4):

- Unlocked FC recipes vanished from the recipe book on every re-entry ("Tried to load unrecognized
  recipe: betterthanwolves:fc_recipe_N removed now"), recipe-book auto-fill did nothing for them,
  and /reload removed all FC recipes until a restart.
- In the 2x2 grid, FC's code-driven recipes (axe + log chopping, knitting, fishing-rod baiting) did
  not exist, and tools used as ingredients (shears, chisels, axes) were used up instead of losing
  durability. At the crafting table, FC "any metadata" ingredients never matched (the 3x3 wool block
  could not be crafted anywhere), shaped Block ingredients accepted only their first variant (oak
  planks for the axle and hamper) in every grid and in JEI, wool blocks and wool armour lost their
  averaged colour, per-ingredient side effects (blood-wood soul dust, pumpkin seeds, iron-chisel
  gravel, bow string, crafting sounds) never happened, and a mould left in the Soulforge grid
  blocked matches.
- JEI covered only the 5 bulk categories.
- No "BTW V4.B0000003" line on joining; on dedicated servers no version check and no
  hardcore-options sync, so client-side player names always followed the default.
- Death messages and other displays of an FC entity type showed the raw key ("Steve was shot by
  entity.betterthanwolves.fc_skeleton").

Root cause: RecipeBridge wrote FC recipes into RecipeManager.recipes but not byName, which byKey()
and the recipe book read (RecipeManager.java:37-38, 145-147; ServerRecipeBook.java:97-105); it ran
only at ServerStartingEvent and dropped every IRecipe that was not ShapedRecipes/ShapelessRecipes.
The 2x2 grid is MC's InventoryMenu with plain MC recipes, without the 1.5.2
SlotCrafting.onPickupFromSlot rules (SlotCrafting.java:118-185). Modern-Common
ShapedRecipes/ShapelessRecipes tested -1 as the wildcard, which ItemStack clamps to 0 (1.5.2 uses
32767); CraftingManager made shaped Block ingredients damage 0; AddShapedRecipeWithCustomClass
ignored its class; Item.OnUsedInCrafting(int, ...) was empty instead of forwarding to the 2-arg hook
(Item.java:1067-1070), and the frozen SlotCrafting calls only that overload; the FCMOD mould skips
were never ported. The FCMOD login hook lived in vanilla ServerConfigurationManager.java:133, which
the port does not use; ForgeNetServerHandler only debug-logged FC's FC|VC and FC|OP payloads, and
their 1.5.2 consumers (FCMOD edits in RenderPlayer/RenderLiving name tags) have no 1.20.1
counterpart. en_us.json had no entity.betterthanwolves.* keys.

Fix:

- RecipeBridge rebuilds the list and calls RecipeManager.replaceRecipes, so both indexes agree, and
  re-injects on OnDatapackSyncEvent(player == null). Recipes with an unmappable ingredient are
  skipped rather than injected incomplete, and fcStackAlternatives expands 32767 to every variant.
- New FCCraftingRecipes: Shaped/Shapeless subclasses keep the vanilla serializers but override
  getRemainingItems with the SlotCrafting rules (not-consumed items stay; damaged items lose 1
  durability or break, except for creative crafters; FC container items return), calling exactly one
  OnUsedInCrafting overload per ingredient as 1.5.2 does. FC hooks and m_iTimesCraftedThisTick run
  only for a ServerPlayer crafter. A CustomRecipe delegates matches, assemble and remaining items to
  FC's IRecipe through an InventoryCrafting adapter; it syncs through the registered serializer
  betterthanwolves:fc_custom_crafting as (index, class name) and is inert on mismatch.
- Modern-Common: the wildcard is 32767 and shaped Block ingredients are 32767;
  AddShapedRecipeWithCustomClass builds the FC subclass through its (int, int, ItemStack[],
  ItemStack) constructor (one entry; a plain fallback is logged once); Item.OnUsedInCrafting(int,
  ...) forwards to the 2-arg hook, guarded so a throwing hook cannot stop SlotCrafting;
  ShapelessRecipes and ShapedRecipes skip fcItemMould stacks (vanilla/server
  ShapelessRecipes.java:63-68, ShapedRecipes.java:115-120).
- JEI: Soulforge (the 4x4 FCCraftingManagerAnvil) and Campfire categories, display recipes for the
  code-driven recipes (probed with FC's own matches/getCraftingResult), and information pages for
  the kiln, saw, turntable, hopper filters and souls, hibachi/stoked fire, cooking vessels and
  campfire, with text taken from the FC block code.
- New FCLoginHandler (PlayerLoggedInEvent) reflectively calls
  FCBetterThanWolves.ServerPlayerConnectionInitialized with the bridge's ForgeNetServerHandler (chat
  packets become system messages), falls back to the MC chat API, and shows a one-time port note.
  ForgeNetServerHandler forwards FC|VC and FC|OP raw through the new S2C message
  BTWNetwork.FCPayload (id 4, 1.5.2 Packet250 limits; PROTOCOL_VERSION is now "3"); the client-only
  FCClientPayloadHandler calls FCAddOnHandler.ClientCustomPacketReceived, so FC's own code prints
  'BTW version check successful.' and stores the options.
- Player-name levels: FCClientPayloadHandler.NameTagRules re-applies level 1 (Hardcore: no player
  name tags, no tag on a sneaking entity; RenderNameTagEvent DENY), and the new client mixin
  EntityRendererNameTagMixin reproduces level 2 (Obstructed) by depth-testing the faint first label
  pass for players (DisplayMode.SEE_THROUGH -> NORMAL, as 1.5.2 RenderLiving.java:512-520 kept
  GL_DEPTH_TEST on). The level resets to -1 on disconnect.
- en_us.json gains 55 `entity.betterthanwolves.<id>` names: vanilla en_US.lang names, FC's
  getEntityName overrides ("The Beast", "Jungle Spider"), BTW_en_US.lang item and tile names for
  FC-only entities, and 1.20.1 names where 1.5.2 had none.

Verification: server smoke test (both runs): "Injected 666 crafting (3 code-driven) + 23 smelting FC
recipes into RecipeManager (0 FC crafting recipes skipped: unconvertible)", 0 ERROR lines. The
client payload chain was type-checked with JDK 17 javac and loaded under `-Xverify:all`; no client
joined, no player logged in, and no multiplayer session was run. In game (not yet run): unlock an FC
recipe, quit and re-enter, and there is no "unrecognized recipe" line and auto-fill still works;
after /reload the log shows "Recipe reload detected; re-injecting FC recipes"; in the 2x2 grid an
iron axe plus a spruce log gives 2 spruce planks with sawdust and bark, and the axe loses 1
durability; shears plus leather keeps the shears; at the table the wool block and wool armour craft
with the averaged colour, sticks craft from any planks and the axle accepts spruce planks; a
singleplayer join shows "BTW V4.B0000003"; a dedicated-server join shows the connect line, 'BTW
version check successful.' and 'Hardcore Modes: ...'; with fcEnableHardcorePlayerNames=1 no player
tags show and with 2 they show in the open but not through walls; JEI shows the Soulforge and
Campfire tabs and the info pages; death by an FC skeleton reads "was shot by Skeleton".

Caveats: FC's RemoveVanillaRecipe/RemoveShapelessRecipe calls are still no-ops, so 1.20.1 recipes
that 4.B removed stay craftable (deferred: it needs a careful mapping from 1.5.2 patterns to 1.20.1
recipe ids). A dedicated client predicts shaped and shapeless remainders with vanilla rules for one
frame. Forge's channel and version checks refuse mismatched port builds before login, so FC's red
version-mismatch warning is practically unreachable. Other FC payload channels (FC|SE, FC|OI) are
still only debug-logged. The FC|OP buoy flag has no consumer (EntityItem.UpdateHardcoreBuoy is not
ported). Level 2 applies to players only; 1.5.2 applied it to every living label. Player name tags
are now hidden by default on servers and LAN, which is authentic but user-visible. A mod that
@Redirects the same Font.drawInBatch call would make the mixin fail at load. Some entity names
repeat (fc_wither and fc_wither_persistent are both "Wither"). The wiki and original-BTW links in
CURSEFORGE.md remain maintainer TODOs.

## 2026-10-10 (am) Log cleanup; per-throw-site failure logging; ReportedException causes

Symptom (design U10; triage HAI-10, SW7): latest.log carried diagnostics from closed investigations:
checkAndOpenContainer, 'Opening MC screen' and 'Container changed!' on every GUI open;
mirrorFcSlots; 'Enchanter levels/slots' on every tick while the Infernal Enchanter was open;
BTW-Penalties every 100 ticks with log4j-invalid '{:.1f}' placeholders (so its labels were shifted);
BTW-Fall lines on every landing; campfire save/load, [SYNC], [TURNTABLE-TICK]/[SIDE-BLOCK] and
[FURNACE-TICK] from block entities; [TRANSMIT-H]/[ROTATE]/[ATTACHED] on every turntable rotation;
about 10 [BTW-DEBUG] lines per block break; [TRADE] on every trade; [SETMETA], [ON-PLACE], use() and
spawnEntityInWorld lines; per-block-model capture dumps, per-entity render lines and 'Cook stack
found' on the client; [AI-DBG], [MOB-DMG] and [ENTITY-DIED]. At the same time real failures were
hidden: FCContainerMenu.stillValid swallowed FC exceptions silently, container failures went to
DEBUG with getMessage() only, one global 10 s limiter masked every other site, puppet onUpdate
failures were sampled at tickCount % 100, FC block tick failures were silent except for kilns, and
frozen code's btw.modern.ReportedException hid the real exception because the Modern-Common copy had
no getCause().

Root cause: instrumentation left in place after investigations closed; failure paths rate-limited by
time or tick sampling instead of by throw site; ReportedException missing the vanilla getCause()
override (ReportedException.java:34-37).

Fix:

- The listed diagnostics are deleted (ContainerBridge, FCContainerMenu, ServerPlayerMixin,
  ProxyBlockEntity, ProxyMob, ProxyBlock, WorldBridge, Modern-Common Block, ItemInWorldManager,
  SlotMerchantResult and the dead EntityLiving AI_TRACE_LOG, FCBakedModel, FCEntityRenderer,
  FCBlockEntityRenderer). The PlayerMixin and LivingEntityMixin fall-debug injectors were HEAD-only
  and non-cancellable, so deleting them changes no behaviour. The GUI-open lines and the
  block-capture summary are DEBUG, and ItemEntityBridge's view and write-back lines were demoted
  from INFO to DEBUG (orchestrator edit).
- Per-throw-site logging: FCContainerMenu.warnFcFailure, ServerPlayerMixin.btw$logTickFailure,
  FCPuppetSupport.logFailureOnce/logNbtFailure (the proxies' onUpdate, interact, attackEntityFrom,
  onDeath, NBT read/write, clientAnimTick and health link), the ProxyBlockEntity tick, ProxyBlock
  tick/randomTick, WorldBridge spawn, closest-entity, explosion and wrap failures, and
  FCEntityRenderer doRender. The key is the exception class plus the first non-JDK frame of the
  unwrapped cause (FCPuppetSupport.unwrap also reads the crash report's cause); the first occurrence
  logs WARN or ERROR with the stack, and repeats are only counted (one line at 10, 100, 1000, ...)
  or not logged again.
- Every FC call FCContainerMenu makes from a menu (stillValid, clickMenuButton, broadcastChanges,
  removed), and the ProxyBlockEntity tick, catch Throwable, so an Error from frozen/Modern-Common
  divergence no longer crashes the server as a 'Ticking entity'; stillValid keeps the menu open and
  clickMenuButton returns false.
- ReportedException.getCause() returns the crash report's cause, so 'Caused by' lines appear
  wherever such a wrapper is logged.
- Kept on purpose: PossessionDiagnostics and WorldBridge's [GHAST-SPAWN] (until (f) is closed by an
  in-game GH1 confirmation), and the one-per-block-model 'live-capture metas: 0x..' INFO for the
  render checks of (ae).

Verification: compile and audits. The server smoke-test logs contain none of the removed tags and no
BTW WARN or ERROR lines (the WARNs present are Forge config and dev-environment notices).
Client-side greps not yet run: after a client session, this should print 0 (one line; copy it
whole):

```
grep -cE "checkAndOpenContainer|mirrorFcSlots|Enchanter (levels|slots)|BTW-Penalties|causeFallDamage|checkFallDamage|\[SYNC\]|\[TURNTABLE-TICK\]|\[FURNACE-TICK\]|\[AI-DBG\]|doCaptureAndBuild|First quad|Cook stack found" latest.log
```

A forced FC exception in a container should produce exactly one WARN with a stack, then count
lines.

Caveats: the tick dedup is global per site, not per player, so a second player hitting the same site
shows only in the count lines. A failure that repeats every tick still costs one getStackTrace() per
tick to compute its key. ProxyEntity still deduplicates onUpdate failures by exception class only.
Failures that used to be silent now print one WARN or ERROR per site, which may surface latent FC
container bugs; that is intended.

## 2026-10-11 (an) Mechanical power: 1.5.2 render primitives, item models, animations, placement, turntables

Symptom (maintainer: the clutch "doesn't have proper facing" and "doesn't connect up with axles from
different directions", wrong axle textures; audits MPR-1..5, F1..F4; reviews RP-R1/R2, RI-1/R2,
PB-R2/R3):

- Clutches and gear boxes drew their input texture on the south (+Z) face whatever their facing and
  never drew their output texture, so axles went on the wrong face and the box seemed not to
  connect. The bellows, saw, screw pump, block dispenser, buddy block, detector block, companion
  cube and mining charge fronts never followed their facing. X and Y axles had end and side textures
  swapped, long faces showed the dark groove along their length, and no axle (not even an axle power
  source) showed fcblockaxle_side_on. The saw blade was mis-rotated.
- The axle item was a vertical post, the saw item the facing-0 world state, every siding, moulding
  and wool-slab subtype looked like damage 0. Millstone `_on` faces, the running saw blade, the
  powered axle and bucket water showed their frame strip squashed into one face.
- Placed against a vanilla or modern block, FC stairs always got direction 0 and the anvil, basket,
  hamper and piston shovel always faced meta 0; a lens never set up its beam.
- Turntables never rotated saws, bellows, block dispensers, screw pumps, buddy blocks, lenses and
  other default-rotation users, and gear boxes, clutches and stairs turned N->E while pumpkins,
  repeaters and pistons turned N->W (prover D8: saw stayed meta 2, gear box 2 -> 5). The clutch,
  pulley, block dispenser and turntable reacted to redstone blocks and powered conductors (prover
  D6: a clutch above a redstone block stayed disengaged). notifyBlockChange woke no neighbour, and
  the "pending this tick" guards (gear box, clutch, millstone, buddy, hopper) answered "scheduled in
  the future".

The prover (D1-D4, D9-D11, two runs) showed the server mechanics themselves were already
1.5.2-exact.

Root cause: render and world primitives re-derived from their names instead of ported from their
1.5.2 bodies.

- btw.modern.RenderBlocks.renderStandardBlock / RenderStandardFullBlock took every face icon from
  block.getIcon(side, meta), FC's item-icon API, and drew all six faces. 1.5.2
  renderStandardBlockWithColorMultiplier (vanilla/client RenderBlocks.java:6047-6187) gates each
  face on `renderAllFaces || shouldSideBeRendered` and draws it with getBlockIcon ->
  block.getBlockTexture(blockAccess, x, y, z, side), where FC keeps its facing logic; FCBakedModel
  therefore baked these blocks once ('live-capture metas: 0x0'). The six renderFace* ignored
  uvRotate* and flipTexture, had no 0..1 clamp, ignored overrideBlockTexture and paired +X UVs
  differently (:7167-7806).
- Block items used the meta-0 RenderBlock quads with ItemOverrides.EMPTY; nothing ran
  RenderBlockAsItem (RenderItem.java:374-398). 1.5.2 animated from `<name>.txt`
  (TextureStitched.java:205-245); no .png.mcmeta existed.
- ProxyBlock.getStateForPlacement called only onBlockPlaced, with metadata 0; nothing called
  PreBlockPlacedBy or onPostBlockPlaced (FCItemPlacesAsBlock.java:94-108).
- Modern-Common Block's turntable helpers had swapped tables, a (f+1)%6 CycleFacing, a no-op
  RotateMetadataAroundJAxis and ToggleFacing, and flag 2 writes (vanilla/server
  Block.java:1461-1471, 2845-2869, 2930-2948, 3059-3082). WorldBridge.notifyBlockChange called
  sendBlockUpdated, isBlockGettingPowered called hasNeighborSignal (1.5.2 getBlockPowerInput is
  strong power only, World.java:4157-4160, :3452-3510), and all three tick queries called
  hasScheduledTick.

Fix (general rule: port the 1.5.2 render and world primitives line for line and run FC's real entry
point for each context; no per-block patches):

- Modern-Common RenderBlocks (port RenderBlocks.java:211-300): renderStandardBlock draws through the
  ported renderStandardBlockWithColorMultiplier with colorMultiplier (the AO variants too);
  RenderStandardFullBlock through RenderStandardFullBlockWithColorMultiplier (1.5.2 :10103-10175)
  and the ported RenderFull*Face (FC's compass: East = z-negative, North = x-negative). The six
  renderFace* (port :2827ff) are 1.5.2's non-AO bodies (:7167-7806): override substitution,
  flipTexture, clamps, uvRotate (YNeg = Bottom, YPos = Top, ZNeg = East, ZPos = West, XNeg = North,
  XPos = South; any other value, such as the saw's 4, means none).
  ShouldSideBeRenderedBasedOnCurrentBounds (port :3702, 1.5.2 :10645-10670),
  RenderStandardFullBlockMovedByPiston, renderBlockUsingTexture and renderBlockAllFaces (1.5.2
  :392-407) are ported. Capture adaptations only: no face shades (1.20.1 applies its own), quads
  tagged with icon and normal, FC calls guarded and logged once per class and method.
- FCBakedModel items (MPR-3, MISSED-2): getOverrides() resolves Damage to a per-damage model
  (cached, up to 256 per block). With sprite number 0 and DoesItemRenderAsBlock(damage) it captures
  renderBlockAsItem(block, damage, 1F) (null blockAccess, GL matrix tracking, base
  glTranslatef(+0.5), 1.5.2's stairs item branch :7939-8010); otherwise the flat icon as
  item/generated geometry; fallback the RenderBlock quads of getMetadata(damage). RI-1: 3D item
  models carry FC_ITEM_TRANSFORMS, block.json's tilt and scale with each context's Y turn from the
  1.5.2 chain (gui RenderItem.java:379-398, first person ItemRenderer.java:421-431, third person
  RenderPlayer.java:316-338).
- RP-R1 follow-up: the gate culled static per-meta quads against StaticAccess's fake same-block
  neighbour (stairs 160 instead of 192 quads, axle 64 instead of 72). A live meta's static quads now
  come from an all-faces capture (runFcRender(..., true) -> SetRenderAllFaces,
  FCBakedModel.java:268-272, :543), 1.5.2's renderBlockAllFaces convention for piston-moved blocks
  (Block.java:3764-3767); they serve piston-moved blocks, the breaking overlay and the live-capture
  fallback.
  *(2026-10-28: no longer the breaking overlay: FC draws the crack itself since (bg).)*
- MPR-4: :Forge:generateBtwTextureAnimations (a processResources dependency) converts the zip's
  MINECRAFT-JAR/textures/{blocks,items}/*.txt to .png.mcmeta with 1.5.2's grammar ("i*t"; bare "i" =
  1 tick; the first bad token ends the list; empty or 600+ entries = every frame at 1 tick). Eight
  are checked in (axle, bucket water, millstone, saw blade).
- MPR-5 (ProxyBlock): getStateForPlacement follows FCItemPlacesAsBlock.onItemUse: metadata =
  getMetadata(damage) of the held stack, facing 1 into ground cover, hit relative to the clicked
  block (PlayerControllerMP.java:397-399), PreBlockPlacedBy with the PlayerBridge (server) or a
  same-yaw facade (client). setPlacedBy runs onBlockPlacedBy with the stack actually placed (PB-R2),
  onPostBlockPlaced and the animal notification (:103-111); canBeReplaced accepts FC ground cover
  and replaceable materials (PB-R3, :79-86).
- F2 (Modern-Common Block): the four FC tables, table-driven RotateFacingAroundJ and CycleFacing,
  RotateMetadataAroundJAxis = SetFacing(meta, rotated GetFacing), ToggleFacing = RotateAroundJAxis,
  flag 3 behind a changed check, ConvertFacingToTop/BottomTextureRotation, OnCraftedOnTurntable aux
  2252 and the 1-clay default (Block.java:1460-1471, 2833-2948, 3025-3082).
- F3, F4, ticks (WorldBridge): notifyBlockChange = level.updateNeighborsAt
  (WorldBridge.java:942-980; source air for id 0, the block at pos for an unmapped id);
  markBlockForUpdate is the only sendBlockUpdated (:984-988); isBlockGettingPowered =
  getDirectSignalTo > 0 (:2173), isBlockIndirectlyGettingPowered = hasNeighborSignal;
  isBlockTickScheduled and IsUpdatePendingThisTickForBlock = willTickThisTick,
  IsUpdateScheduledForBlock = hasScheduledTick (:1028-1060; WorldServer.java:421-425, :1258-1263).
  BRIDGE_SPEC.md 3.1 maps these by body.

Verification: build and audits. A harness gives the 1.5.2 UVs (Z-axle top U 0..16 along Z, V 6..10;
switch-like bounds clamped). The mid-wave dev client logged gear box, clutch, saw, bellows and axles
as 'live-capture metas: 0xffff' with the _output textures captured (a build before the review
fixes). To check in game (not run): a clutch or gear box placed facing each direction shows its
input there and outputs elsewhere; X, Y and Z axles show end caps on their axis, powered ones
animated; the axle item is horizontal and the saw item has an upright blade; stairs placed against
dirt follow the player's yaw; prover D8 gives saw and gear box meta 4 after one rotation and D6
clutch meta 12, outputs 11/7/7; FCBlockStairs is back to 192 static quads and FCBlockAxle to 72.

Caveats:

- RP-R2 / RI-2 (unmeasured): the gate reads neighbours, so every meta of FCBlockAestheticOpaque, the
  FC slabs, sidings, corners, mouldings, stairs, planters, panes, the mining charge and aesthetic
  vegetation is captured live on every section rebuild, serialised on CAPTURE_LOCK, for little
  culling: Block.opaqueCubeLookup is never filled (1.5.2 Block.java:484) and
  ShouldRenderNeighborFullFaceSide returns true (1.5.2: !isOpaqueCube). Measure before changing; the
  lever is lock granularity (FCBlockAestheticOpaque reads the neighbour id for blood moss, :518).
- Items: cached per damage per bake; brightness dropped; the FC chest (1035) item is a planks cube
  (ChestItemRenderHelper not ported; it was invisible); other unported vanilla item branches (11,
  21, 27, 32, 34, 35, 38) draw cubes; blocks without a RenderBlockAsItem override show 1.5.2's cube
  item; FC TESR cook items draw 3D block items turned 90 degrees.
- The BlockItem path does not run FC's canPlaceBlockOnSide / canPlaceBlockAt refusal, and client
  prediction runs on a read-only view, so neighbour-dependent blocks can mispredict briefly.
- More blocks turn on turntables and the redstone users ignore weak power: authentic, but existing
  builds change.
- Not done: opaqueCubeLookup, the ShouldRenderNeighborFullFaceSide default and
  RenderBlockMovedByPiston; AxisAlignedBB.RenderAsBlock* still use getIcon (1.5.2
  AxisAlignedBB.java:605-628; overdraw only); three Block dispenser/spawn defaults.

## 2026-10-11 (ao) One sound rule: 1.5.2 files, one resolver, each sound played by its 1.5.2 side and owner

Symptom (maintainer: wrong and missing sounds; audit S1-S11 and the verifier's missed items; reviews
WP-1, CW-1..3, PB-R1, AS-R1..R3, EI-1/2):

- Wrong files: the saw and millstone ground with the minecart-inside loop, a hurt creeper hissed the
  fuse, the bellows mooed a random cow, and chests, doors, anvil break, the "oof" and the hit ding
  played 1.20.1 replacements. Silent ('Unable to play unknown soundEvent:
  minecraft:mob_zombiepig_zpig'): zombie pigmen, enderman idle, wither idle and hurt, ghast death,
  wolf shake, ocelots, iron golem throw, zombie cure, dragon wings, firework launch, the soulforged
  helm's breath.
- Client-only sounds never played: millstone grinding, axle creak, campfire and smouldering-log
  crackle, wet-cement moan, turntable click, cow kick; the gear box, millstone and bellows
  power-on/off sounds and smoke (S2); everything FC's ClientPlayCustomAuxFX plays. FC aux ids came
  from a drifted server copy (hopper close, redstone click 0.3/0.6 instead of 0.75/2.0, XP pop, wolf
  eating, melon and pumpkin bursts, improper tool, lightning, dung whine, howl volume) with no
  particles.
- FC block sounds fell back to stone: squish blocks (FC web, unfired pottery, nether growth, rotten
  flesh) and glass-type blocks (crucible, vase, planters, light bulb); puppets on deepslate or new
  woods stepped on stone.
- Nobody else heard or saw a player break a block (S6); the placer heard a block placed against a
  legacy-id vanilla block twice; FC mobs entering water made no splash (S8); crafting FC tools,
  armour, shears or cured food was silent (S9); FC foods never burped, placement played the step
  sound at full volume and pitch, stoked fire neither crackled nor smoked (S10); no gloom ambience,
  step grunt or soulforged-boots footstep (S11).

Root cause: no single contract for names, sides or owners. SoundMapping was a hand table of 1.20.1
replacement events with a `minecraft:<a_b_c>` fallback that never exists, although 1.20.1 still
ships every 1.5.2 sound3 file (402 at the same path, 15 renamed but byte-identical). Every
client-side FC world inherited Modern-Common World.playSound as an empty method and stubbed
playAuxSFX, where 1.5.2 played through WorldClient.playSound (WorldClient.java:483-505) and
RenderGlobal, whose FCMOD default branch calls ClientPlayCustomAuxFX (RenderGlobal.java:2394/:2159);
nothing called ClientNotificationOfMetadataChange (WorldClient.java:294-315) or ClientBreakBlock /
ClientBlockAdded (Chunk.java:633/683). WorldBridge replayed FC aux ids from its own copy and ignored
the actor; tryHarvestBlock left out the 2001 emit (ItemInWorldManager.java:432/:458) while the mixin
cancels MC's. ProxyBlock.getSoundType translated stepSoundName only. Puppets copied MC's isInWater
into FC, hiding the water-entry edge (Entity.java:1079-1123). Nothing ran Item.onCreated, which FC
uses only when world.isRemote (SlotCrafting.java:72-75). ItemFood, ItemBlock and BlockFire stand-ins
dropped their 1.5.2 sounds; gloom, grunt and boots live in player classes 1.20.1 replaces
(EntityPlayer.java:2454-2492).

Fix (general rule: route every FC sound call by the 1.5.2 IWorldAccess contract of the side that
runs it, resolve every name in one place, give every sound exactly one owner):

- Names: new LegacySounds.resolve(String) (LegacySounds.java:92) is the single resolver (never
  throws, thread-safe, unknown names logged once). It registers one SoundEvent per key of the
  checked-in sounds.json: 508 events, one per 1.5.2 pool (SoundPool.addSound with FCMOD,
  SoundPool.java:46-97, so mob.cow.say4 is exactly say4.ogg), each playing 1.5.2 files only (same
  path, else the identical-SHA-1 file). Names 1.5.2 had no file for (mob.attack, step.anvil,
  damage.thorns, the villager and witch names) are SILENT_IN_1_5_2. 499 of 508 have a 1.20.1
  subtitle key (AS-R1). sounds.json comes from the manual task :Forge:generateLegacySoundsJson;
  SoundMapping.SOUND_MAP is a coverage manifest.
- Client sink: new btw.forge.client.LegacyClientSound. playLocal (:76) ports WorldClient.playSound;
  playAux (:121/:130) sends vanilla ids through AuxSfxMapping to a local levelEvent and ids >= 2222
  to FC's ClientPlayCustomAuxFX with a populated legacy Minecraft facade (particleSetting, an
  EffectRenderer bridged to ParticleEngine, an eye-level thePlayer). The ProxyEntity and
  ProxyBlockEntity client worlds, ProxyBlock's client view (createFcWorld(level, false)) and the new
  read-only ClientFcWorld use it and re-mesh through LevelRenderer.setBlocksDirty; playSoundEffect
  stays a no-op, as in RenderGlobal (:1812). CW-1: the spider-web and soul-sand spawn sounds
  (FCBetterThanWolves.java:2653/:2675); CW-2: aux digging FX use the aux metadata; CW-3: a bellows
  lifts the local player on power-off (FCBlockBellows.java:577-605).
- Client block hooks (MPR-6, S2): client mixin ClientLevelMixin calls
  ClientNotificationOfMetadataChange on a server-verified META change of the same ProxyBlock, and
  ClientBreakBlock / ClientBlockAdded on a block change.
- FC aux ids (S3): WorldBridge sends ids >= 2222 with BTWNetwork.sendFcAuxSfx
  (WorldBridge.java:2027): the FcAuxSfx message (network protocol 4) to players within 64 blocks
  except the actor, or to every player when global; the client runs FC's own handler. playFcAuxSFX
  is deleted.
- Actor rule (S6, S7, WP-1): playAuxSFX = playAuxSFXAtEntity(null); the actor is left out only where
  its 1.20.1 client really played the event: always for 2001, otherwise only off a ProxyBlock
  (ProxyBlock.use runs FC server-side only, so the FC door 1036 keeps its sound for the opener)
  (WorldBridge.java:2059-2085). tryHarvestBlock emits 2001 to the players around the breaker
  (ItemInWorldManager.java:49).
- Placement sound (follow-up 4): BlockBehaviorMixin stage 2 runs tryPlaceHeldItem inside
  WorldBridge.withPredictingActor when the held item is a BlockItem
  (BlockBehaviorMixin.java:255-266), and playSoundEffect then leaves that player out
  (PREDICTING_ACTOR, WorldBridge.java:1770-1793): the 1.20.1 client predicted BlockItem.useOn with
  its sound, while 1.5.2 clients predicted silently (vanilla/client RenderGlobal.java:1812).
  ProxyBlock.use needs no scope; its client predicts nothing.
- Block sounds (S5, PB-R1): ProxyBlock.getSoundType(state, level, pos, entity) builds a
  ForgeSoundType from FC's StepSound objects; the per-position GetStepSound feeds footsteps only
  (ProxyBlock.java:1190), break, place, hit and fall use the block's stepSound
  (RenderGlobal.java:2309, PlayerControllerMP.java:320). StepSound gains StepSoundStone/Sand/Anvil;
  ModernBlockStandIn.GetStepSound reports the live block's SoundType.
- S8: puppets seed inWater from MC on the client only; FC's handleWaterMovement owns the splash and
  its particles, MC keeps only GameEvent.SPLASH (EI-1).
- S9 and follow-up 5: ProxyItem.onCraftedBy and an ItemStackMixin onCraftedBy hook call
  ProxyItem.runFcOnCreated, which on the client counts m_iTimesCraftedThisTick and plays locally.
  For FC containers, ContainerBridge.writeSlotLayout sends a craft-result flag per slot
  (ContainerBridge.java:152-153) and the client FCContainerMenu builds ClientCraftResultSlot
  (FCContainerMenu.java:234, :595-610), whose onTake runs it (shift-click passes the pre-move stack,
  :327). The server SlotCrafting stays silent under FC's isRemote guard, so the client is the one
  owner, as in 1.5.2 (vanilla/client SlotCrafting.java:72-75, :184).
- S10: ItemFood.onEaten burps (ItemFood.java:73) and for an eat FC fed, PlayerMixin drops
  Player.eat's PLAYER_BURP and ItemStackMixin empties the closing eat sound;
  ItemBlock.PlayPlaceSound plays getPlaceSound at (v+1)/2, p*0.8; BlockFire.randomDisplayTick is
  ported (BlockFire.java:400-479).
- S11: client GloomAmbience ports UpdateGloomState (waiting up to 20 ticks for a new LocalPlayer's
  first PenaltySync, EI-2); Modern-Common EntityPlayer carries playStepSound (grunt, soulforged
  boots), run by a PlayerMixin hook.

Verification: BridgeAudit [1] 72 / 136 / 0 unmapped, [2] 64 / 64; every file sounds.json names
exists in the 1.20.1 index; the generator reproduces sounds.json byte for byte; the dedicated server
registers 508 legacy sound events. To check in game (not run): /playsound
betterthanwolves:mob.zombiepig.zpig plays; a powered gear box clicks and smokes on every client; a
second player hears a break once; placing stone on stone and crafting shears at the FC workbench
(also shift-click) each sound once; an FC cow splashes once; darkness brings cave sounds, growls and
the stare.

Caveats:

- FC villagers and witches have no idle, hurt or death sound, as in 1.5.2 (no sound file existed);
  the FC witch still idles with the ghast's affectionate scream.
- 28 same-path files are Mojang re-encodes (cat, wood steps, rain, random/click now mono). The
  1.20.1 pitch clamp (0.5-2.0) caps the wet-cement moan (2.6 +- 0.8) and the 2.5 fizz. Hitting an FC
  block is subtitled 'Footsteps'; FC's creaks read 'Chest opens/closes'.
- Global aux events (howls, dire conversion, lightning) reach every player on the server, in every
  dimension, as 1.5.2 broadcastSound did (vanilla/server WorldManager.java:108-111; first written to
  the dimension only, corrected after the review).
- ClientNotificationOfMetadataChange fires after the state is applied and only on a real META
  change; only ProxyBlocks are hooked.
- One packet per splash particle; one PlayerBridge sync per server footstep; in water the 1.20.1
  swim and muffled steps stay with soulforged boots on. Hold-to-craft PlayCraftingFX is bridged since
  2026-10-18 with UpdateUsingItem (ProxyItem.onUseTick; PlayerBridge mirrors the real use counter).
- The 508 SoundEvent registry entries and network protocol 4 refuse mismatched client and server
  builds.
  *(2026-10-15: network protocol 5 since compat wave C1: PenaltySync also carries FC exhaustion, and
  FoodTableSync sends the btw_foods table to a dedicated server's clients.)*

## 2026-10-11 (ap) Creative tab, Search and JEI from FC's creative rules; block-item names, subtypes, pick block

Symptom (maintainer: hidden technical blocks and 'Fc Opaque Generic' in the BTW tab, missing
variants; audit CT1-CT5, verifier MISSED-1..4; reviews CI-1..CI-3):

- The BTW tab, the vanilla Search tab and JEI listed every registered FC block and item ('Added 174
  blocks and 253 items to BTW creative tab'): 105 entries that 1.5.2 hides, among them burning, lit
  and crop states, Axle Power Source, Stoked Fire, the Milk and Bucket blocks, Weeds as 'Block
  1091', Lava Pillow, Shaft, Tool Placed, the 20 legacy 'Fc ... Siding/Moulding' blocks and the Old
  Slats/Grate/Wicker items.
- 33 variants 1.5.2 shows were missing: the 11 aesthetic blocks, 15 of the 16 wool slab colours, the
  sand, grass and packed-earth slabs, packed earth and dung, the blood wood sapling, the companion
  slab.
- 64 of the 120 block entries 1.5.2 shows had camel-case names from the raw block name: 'Aesthetic
  Opaque' for Block of Soap, Hellfire and the rest, 'Anvil' for Soulforge, 'Furnace Brick' for Brick
  Oven, 'Shovel' for Piston Shovel. The string 'Fc Opaque Generic' exists nowhere in the repo,
  source, lang files, classes or logs; it is most likely a paraphrase of these 'Fc ...' and
  'Aesthetic Opaque' names.
- JEI collapsed every Damage subtype into one entry (22 arcane scrolls, wool, candles, knit wool,
  bark).
- A subtype block item placed meta 0 on the BlockItem path (MISSED-1), a crafted packed-earth or
  grass slab placed the wrong subtype on the FC path, middle-click handed out the clicked block's
  own item at meta 0 (Shaft, Wheat Crop, Furnace Brick, Kiln; MISSED-3), and every subtype rendered
  as meta 0 (MISSED-2, see (an)).

Root cause: BTWCreativeTab.buildContents walked the registries (every ProxyBlock at meta 0, every
btw item from id 478) and never asked FC's getCreativeTab / getSubItems, the way 1.5.2
GuiContainerCreative did. Modern-Common lacked the plumbing: no Item.getCreativeTab(); ItemBlock did
not forward the tab, getSubBlocks or the "tile." name to its block; and the vanilla-base stand-ins
(ItemFood, ItemArmor, ItemSword, ItemBow, BlockChest, BlockPane, BlockBasePressurePlate) dropped
their constructor setCreativeTab, so a naive tab filter would have hidden 66 legitimate FC entries
(CT4). Block items had a constant literal name, JEI had no subtype interpreter, nothing read
GetBlockIDToPlace or getMetadata(damage), and ProxyBlock had no getCloneItemStack.

Fix (general rule: creative visibility, names and placement come from FC, computed as 1.5.2
CreativeTabs.displayAllReleventItems, Item.getItemDisplayName and FCItemPlacesAsBlock.onItemUse
computed them, never from the registries):

- BTWCreativeTab emulates CreativeTabs.displayAllReleventItems (vanilla/client
  CreativeTabs.java:134-147): FC tabs in creativeTabArray order (Search and Inventory skipped),
  FC-owned ids only (block items of ProxyBlocks 175-4095, ProxyItems from 478), an id listed when
  getCreativeTab() == tab and expanded through getSubItems, each stack converted with toMcStack
  (metadata on the Damage tag) and accepted with PARENT_AND_SEARCH_TABS, so the Search tab and JEI
  list exactly the BTW tab (MISSED-4).
- Modern-Common (CT4, which had to land with CT1): Item.getCreativeTab() and
  CreativeTabs.displayAllReleventItems; ItemBlock.getCreativeTab / getSubItems /
  getUnlocalizedName()/(stack) forward to the block, with "tile." + getUnlocalizedName2()
  (vanilla/client ItemBlock.java:200-227); constructor tabs restored per vanilla/client
  (ItemFood:55, ItemArmor:58, ItemSword:30, ItemBow:27, BlockChest:31, BlockPane:41,
  BlockBasePressurePlate:27).
- New ProxyBlockItem, registered by BTWRegistration: getName is FC's
  getItemDisplayName(toFcStack(stack)), with formatFcName only when FC hands back the untranslated
  key; getPlacementState places the block FC's GetBlockIDToPlace(damage, side, hit relative to the
  clicked block) names, through that block's getStateForPlacement, which takes getMetadata(damage)
  from the held stack ((an) MPR-5).
- Modern-Common ItemBlock.onItemUse is now FCItemPlacesAsBlock.onItemUse (Common
  FCItemPlacesAsBlock.java:60-123): GetBlockIDToPlace, ground-cover and replaceable targeting,
  canPlaceEntityOnSide, getMetadata(damage), onBlockPlaced, PreBlockPlacedBy, set, onBlockPlacedBy,
  onPostBlockPlaced, the animal notification, PlayPlaceSound.
  *(2026-10-21: WorldBridge.canPlaceEntityOnSide only checked for an entity in the cell until 2026-10-21 (bd): no
  replaceable-target or canPlaceBlockOnSide rule ran, so FC placement went into solid cells and flowers onto stone
  (triage CF2-P8a, correction 3).)*
- CI-1 (review blocker): getMetadata(damage) is faithful (:94), but the port registers plain
  ItemBlocks and getMetadata-less ItemMultiTextureTile / ItemBlockWithMetadata / ItemColored stubs
  at the vanilla subtype ids, so wool colours, wood species, slabs, sandstone, stone bricks, quartz,
  walls and strata stone placed against an FC or legacy-id block came out as meta 0 and snapped back
  from the client's prediction. ItemBlock.getMetadata (ItemBlock.java:112-128) restores 1.5.2's
  vanilla subclass rule: the three stubs (and FC's FCItemBlockLog, FCItemBlockSnow,
  FCItemBlockLilyPad, which extend them) return the damage; an exact-class ItemBlock at ids 5, 6,
  17, 24, 31, 35, 43, 44, 97, 98, 106, 125, 126, 139 and 155 returns the damage, and at 18 damage |
  4 (ItemLeaves.java:29); any other block item keeps FCItemPlacesAsBlock's 0. ItemPiston's 7 and
  ItemAnvilBlock's << 2 are not ported.
- CI-2: onItemUse guards its FC hooks. A throw in getMetadata, onBlockPlaced or PreBlockPlacedBy
  places nothing; a throw after the block is set is logged once per block class and hook group, and
  the place sound and stackSize-- still run, so a failing hook cannot hand out free blocks.
- JEI (CT5, CI-3): BTWJeiPlugin registers one subtype interpreter for every betterthanwolves item:
  the Damage metadata, NONE for damageable items, and NONE in UidContext.Recipe
  (BTWJeiPlugin.java:145), because JEI looks recipes up by the exact Recipe uid and RecipeBridge
  collapses FC's 32767 wildcard on a btw item to meta 0.
- Pick block (MISSED-3): ProxyBlock.getCloneItemStack (ProxyBlock.java:1227) follows 1.5.2
  Minecraft.java:2348-2380: FC's idPicked (0 picks nothing), the damage of blocksList[picked < 256
  and not a flower pot ? picked : clicked].getDamageValue, converted by
  ItemStackHelper.fcIdToMcStack (EMPTY when the id has no 1.20.1 item).

Verification: a headless harness running the real FC init (FCBetterThanWolves and
FCAddOnHandler.InitializeMods over the fc and Modern-Common classes, scratchpad only) reproduced the
maintainer's 174 + 253 and gave 355 entries under the 1.5.2 rules; the mid-wave dev client logged
'Added 120 block and 235 item entries to the BTW creative tab from FC's creative-tab membership (358
FC-owned item ids)', the same 355. No changed Modern-Common class has a frozen copy in
Forge/build/classes/java/fc/btw/modern. To check in game (not run): no 'Fc ...', burning, crop or
power-source entries; 11 named aesthetic subtypes and 16 wool slab colours; FC foods, armour, the
refined sword, the composite bow, the chest, the panes and the soulforged pressure plate present;
'axle power' finds nothing in Search or JEI; JEI shows 22 scrolls and 16 wool colours, and U on red
knit wool lists the wool armour; a creative Block of Soap places 215:5 against stone and against
deepslate; orange wool, spruce planks, a birch log, chiseled stone bricks, a jungle sapling and
spruce leaves placed against stone, dirt or an FC block keep their variant and drop it back;
middle-click on a shaft, wheat crop, lit oven and kiln gives a stick, seeds, the idle oven and the
kiln's drop.

Caveats:

- CT6 is not done (maintainer decision): the vanilla tabs still show what FC hid in 1.5.2, such as
  the vanilla-id chest, cobweb and ladder (FC's own 1035, 1037 and 1051 are in the BTW tab), the
  crafting table, records, the comparator and the TNT and hopper minecarts. If wanted, drive it from
  FC's explicit setCreativeTab(null) calls (FCMOD lines in vanilla/client Block.java and Item.java,
  BTWLifecycle.java:476-478 and :1704, the FCItemStub records), not from a null-tab test: the port's
  vanilla initialisers set no tabs at all.
  *(2026-10-15: narrowed by 2026-10-15 (at). The maintainer's stays-removed ruling hides every T1 and T2 id
  of RemovedContent from every tab, the chest, cobweb, ladder, crafting table, comparator and the TNT and
  hopper minecarts among them. The rest of CT6 stays open: records other than 13, enchanted books, wheat
  seeds and legacy wheat, glowing redstone ore, and the BlockStem / BlockNetherStalk / BlockRedstoneTorch
  stand-ins.)*
- Strict parity hides survival-obtainable untabbed FC items from the tab and from JEI's list (Witch
  Wart, Mail, Steel Nugget, Ender Slag, cut and tanned leathers, Golden Dung, Redstone Latch, ...):
  maintainer decision.
- MISSED-1 remaining gap: on the BlockItem path ProxyBlockItem delegates only GetBlockIDToPlace and
  getStateForPlacement. FC ItemBlock onItemUse overrides (FCItemBlockSlab combining, the blood wood
  sapling's soil rules, the finite torches' AttemptToLightBlock) and FC's placement refusals do not
  run there; running them in useOn needs its own creative and consumption design. The path is taken
  when the clicked block is post-1.5.2 or a vanilla block whose 1.20.1 class overrides use(), for
  off-hand placement, and for every sneaking placement: 1.20.1 ServerPlayerGameMode.useItemOn skips
  Block.use when the player is sneaking with an item, so neither ProxyBlock.use nor
  BlockBehaviorMixin stage 2 runs, while 1.5.2 skipped only onBlockActivated and still ran FC's
  onItemUse (ItemInWorldManager.activateBlockOrUseItem).
- CI-3 partial: JEI's R and U lookups merge an item's subtypes until
  RecipeBridge.fcStackAlternatives expands a 32767 wildcard to every FC subtype (getSubItems in a
  try/catch, metas 0-15 as fallback); the Recipe-context NONE can then go.
- FC block names are English Component.literal strings, as with ProxyItem. The verifier's explicit
  null tabs for the BlockStem / BlockNetherStalk / BlockRedstoneTorch stand-ins are not applied
  (vanilla ids only, CT6 territory).

## 2026-10-11 (aq) Neighbour source ids, placed leaves, placement failures, World flag 2, BridgeAudit aux check

Symptom (reviews WP-2, WP-3, WP-4, CI-1 and CI-2 follow-ups; orchestrator follow-ups 1-3, 6 and 8
after W6):

- Buddy blocks never fired when a lever beside them was flipped, a vanilla block next to them was
  mined or a post-1.5.2 block next to them changed. The F3 notifyBlockChange path ((an)) also
  reached FC ProxyBlock neighbours as "no block" whenever its source was a vanilla id.
- Leaves placed through BTW's placement path decayed like natural leaves.
- A throw from FC onItemUse, now the full FCItemPlacesAsBlock sequence, escaped
  ProxyBlock.tryPlaceHeldItem into the use-item packet handler, which has no catch (1.20.1
  PacketUtils rethrows): BlockableEventLoop.doRunTask logs an Exception as FATAL, and an Error (the
  usual failure from frozen / Modern-Common divergence) takes the server down.
- FCBlockNetherGrowth's netherrack conversion (Client FCBlockNetherGrowth.java:53/:145) and
  FCBlockSapling's restore after a failed tree (:200-207) never reached clients.
- tools/BridgeAudit check [2] reported 0 handled and 55 fired-but-unhandled FC aux ids once (ao)
  deleted WorldBridge.playFcAuxSFX: false positives that would hide a real regression.

Root cause: more primitives mapped by name.

- ProxyBlock.neighborChanged passed the source's legacy id only when the source was a ProxyBlock,
  so FC onNeighborBlockChange saw 0 for any other source, and FCBlockBuddyBlock ignores 0 (Client
  FCBlockBuddyBlock.java:78-80). 1.5.2 passes the notifying id through (World.java:694-702; setBlock
  passes the old id, :461-478; setBlockMetadata the current one, :572-575), as 1.20.1 blockUpdated
  passes the old block.
- WorldBridge.applyFcMetadataToVanillaBlock had no leaves branch: once CI-1 passed 1.5.2's
  player-placed bit (ItemLeaves.getMetadata = damage | 4, vanilla/client ItemLeaves.java:29), the
  state was still written with PERSISTENT false (before W6 the bit was not even passed, so these
  leaves decayed in 1.3.0-alpha.3 as well).
- ProxyBlock.tryPlaceHeldItem had only the creative try/finally.
- Modern-Common World.setBlockAndMetadata wrote with flag 0, while 1.5.2 uses flag 2 (vanilla/server
  World.java:4152-4155); with 1.20.1 Level.setBlock flag 0, sendBlockUpdated never runs. Its 6-arg
  playSound was empty instead of delegating to the 7-arg one (vanilla/client World.java:4994-4997),
  so a client world overriding only the 7-arg overload missed FC's 6-arg calls (FCBlockMillStone).
- BridgeAudit counted numeric `case NNNN:` labels >= 2222 in WorldBridge.

Fix:

- New ProxyRegistry.getNeighbourSourceId(Block, BlockGetter, BlockPos) (ProxyRegistry.java:280-288)
  = getWorldBlockId of the source's default state: a ProxyBlock or legacy-id vanilla block reports
  its legacy id, any other colliding block its stand-in id (160/161), air 0.
  ProxyBlock.neighborChanged (ProxyBlock.java:379) and BlockBehaviorMixin.btw$neighborChanged
  (BlockBehaviorMixin.java:178) use it, so FC neighbours see the id 1.5.2 would pass, and
  WorldBridge's notifyBlockChange(id) round-trips: id -> neighbourSource block -> the same id
  (WorldBridge.java:966-980).
- WorldBridge.applyFcMetadataToVanillaBlock maps meta bit 4 to LeavesBlock.PERSISTENT
  (WorldBridge.java:527-532); the species bits already pick the variant through
  getModernBlockVariant(18, mask 3).
- ProxyBlock.tryPlaceHeldItem (ProxyBlock.java:536-563) catches Throwable around both
  tryPlaceItemIntoWorld calls, logs once per item id through ProxyItem.logFcFailureOnce("onItemUse",
  ...) and returns false, so nothing is placed; the creative restore stays in finally. Together with
  CI-2's guards in ItemBlock.onItemUse ((ap)), a failing FC placement hook can neither crash the
  server nor hand out free blocks.
- Modern-Common World.setBlockAndMetadata uses flag 2 (World.java:295-298), and the 6-arg playSound
  delegates to the 7-arg one (:683-688). The WorldBridge setBlockAndMetadata override added during
  the wave is removed as redundant.
- BridgeAudit [2] (BridgeAudit.java:111-134): an FC aux id counts as handled when FC's own
  ClientPlayCustomAuxFX has a `case m_i...AuxFXID:` label for it (Client FCBetterThanWolves.java),
  provided the forwarding chain is intact: WorldBridge contains `BTWNetwork.sendFcAuxSfx(` and
  `FIRST_FC_AUX_SFX_ID = 2222`, BTWNetwork calls `LegacyClientSound.playAux(`, and LegacyClientSound
  names "ClientPlayCustomAuxFX". A broken link is a high-severity finding.

Verification: BridgeAudit prints '[2] AUX-FX COVERAGE: 64 aux-FX ids defined, 64 handled by FC
ClientPlayCustomAuxFX (forwarded via BTWNetwork.sendFcAuxSfx), 0 FIRED-but-UNHANDLED' (2 unhandled
before W6); `:Forge:jar` is green and the dedicated server boots with 0 ERROR lines. To check in
game (not run): a buddy block fires when a lever beside it is flipped and when the stone beside it
is mined; spruce leaves placed against stone, away from any log, never decay; nether growth
spreading into netherrack shows on the client at once; a forced FC onItemUse failure gives one WARN
and no placement.

Caveats: the source id comes from the source block's default state, so a state-dependent legacy id
is reported as the default state's (a vanilla slab notifying as a double slab reads as the
single-slab id). BridgeAudit's class comment (BridgeAudit.java:13-15) still describes the old
WorldBridge switch. RecipeBridge's per-subtype wildcard expansion (CI-3) and the opaque-cube /
AxisAlignedBB render items of (an) remain open.

## 2026-10-11 (ar) Server lag: replacement proxies count in the replaced vanilla mob's spawn category

**Symptom (maintainer, CurseForge instance).** Within about ten minutes of a fresh world the server
thread fell behind ("Can't keep up", up to 28 s, about 6 TPS). Oak logs broke 10-30 s late while dead
bushes broke at once. Reported against the published jar; the dev client showed the same.

**Evidence.**
- JFR profile of the live instance (90 s): 83.6% of server-thread samples in ProxyPathfinderMob.tick,
  69.3% in FCEntitySquid.onLivingUpdate (head-crab and attack-target entity scans through
  WorldBridge.getEntitiesWithinAABB / GetClosestEntityMatchingCriteriaWithinRange,
  collideWithNearbyEntities, moveEntity).
- The ten-minute autosave held 5745 `betterthanwolves:fc_squid`, up to 478 in one chunk, almost all at
  y -40..31, plus 403 ink sacs and 490 FC XP orbs from squids killed by entity cramming. A nine-minute
  dev world held 185.
- The only squid replacement in the log: `minecraft:glow_squid -> betterthanwolves:fc_squid (NATURAL)`.

**Root cause.** VanillaMobReplacer maps glow_squid (MobCategory UNDERGROUND_WATER_CREATURE, cap 5) to
fc_squid, registered as WATER_CREATURE. Forge 1.20.1 NaturalSpawner.createState (mapped 47.3.0
sources :67-90) counts each loaded non-persistent mob under `entity.getClassification(true)`, whose
IForgeEntity default is `getType().getCategory()`. Every converted glow squid counted as
WATER_CREATURE, the underground-water count stayed near 0, and glow squids spawned every tick without
limit, each becoming an fc_squid. FC's squid AI scans the entities around it every tick, so the cost
grew roughly with the square of the population. 1.5.2 counted each cap by the creature class it
spawns (vanilla/server SpawnerAnimals.java:107), so a mob was always counted where it spawned.
glow_squid was the only REPLACEMENTS entry whose vanilla and FC categories differ.

The late breaks follow from the lag: ServerPlayerGameMode.handleBlockBreakAction (STOP_DESTROY_BLOCK,
:179-199) measures progress in server game ticks, so at ~6 TPS the break becomes a delayed destroy;
instant blocks are destroyed at START (:93).

**Fix (general rule).** A replacement proxy counts for natural-spawn purposes in the MobCategory of
the vanilla mob it replaced.
- ProxyMob, ProxyAnimal and ProxyPathfinderMob keep a nullable replaced category
  (`setReplacedCategory`) and override `getClassification(boolean forSpawnCount)`: the replaced
  category when counting spawns and known, otherwise the registered one. NaturalSpawner.createState
  is the method's only caller.
- The category is saved next to FCClassName as the string tag `BTWSpawnCategory`
  (`MobCategory.getName()`); ProxyMob.readSpawnCategory keeps the registered category for a missing
  tag and for an unknown name (one WARN per name).
- VanillaMobReplacer.replaceWithProxy, the single place a proxy is built from a vanilla entity (every
  origin), sets it before FC init and addFreshEntity, so the next spawner count already sees it.
- At ServerAboutToStart VanillaMobReplacer logs once (INFO) every REPLACEMENTS entry that crosses
  categories (today only glow_squid).
- Unchanged: entity type registrations, despawn (both categories use 128/32), FC code, FC squid
  behaviour. Proxies not made by replacement keep their registered category.

**Verified.** `:Forge:jar` green; LinkAudit and BridgeAudit unchanged; a dedicated server boots with 0
ERROR lines and logs the category line. Still to check in game: in a fresh world near cave water,
after 10+ minutes `/execute if entity @e[type=betterthanwolves:fc_squid]` stays small, no "Can't keep
up" appears, logs break on time, and `/data get entity` on a converted glow squid shows
`BTWSpawnCategory: "underground_water_creature"`.

**Caveats.** Over-populated test worlds are not cleaned up, and their untagged fc_squid still count as
WATER_CREATURE. Biome spawn costs (the potential-charge budget) are keyed by EntityType, so proxies add
no charge after their spawn tick; in soul sand valley and warped forest, spacing comes only from the
MONSTER and local caps (not a lag source; 1.5.2 had no charge system). A BiomeModifier copying each
replaced type's spawn cost to its fc type would close it (review W7-R1).

## 2026-10-14 (as) Tick ownership: FC decides random and scheduled ticks of vanilla-id blocks

**Symptom (tickgap audit: five families, 146 findings, 124 confirmed and 22 corrected by adversarial
verification, 0 refuted; runtime proof P1).** BTW's rules for growth, spread, decay, fire and falling
blocks did not apply to blocks that keep a Minecraft id. 1.20.1's rules and rates ran instead:
- **P1 (2026-10-11, before the fix).** 25 birch saplings on superflat grass at y=-60 and 9 on a lit
  grass platform in the End, randomTickSpeed 300 (about 44 random ticks each in 30 s): 25/25 and 9/9
  were birch trees at 30 s. That is 1.20.1's rule (SaplingBlock.java:34-39: two 1/7 successes at
  light 9, any dimension). 4.B's rule (Server FCBlockSapling.java:40-64: four 1/64 successes at light
  9, never in the End) predicts about 0.14 trees and 0 in the End.
- **Saplings:** trees grew in about 16 minutes instead of hours, also in the End, as 1.20.1 trees.
- **Crops:** carrots, potatoes and wheat grew about 3x (wheat about 7x) faster, at night, on dry
  farmland and despite weeds.
- **Grass and mycelium:** loose dirt never regrew grass; grass spread under torches, at night and in
  the End; mycelium never died in darkness.
- **Leaves:** leaves decayed by 1.20.1 distance and dropped apples and sticks.
- **Other plants:** sugar cane grew 2x and cocoa 4x faster; nether wart grew in the Overworld; cactus,
  cane, vines and mushrooms grew in the End.
- **Lava:** each selected lava cell ran 1.20.1 fire spread twice and never lit BTW campfires or melted
  BTW snow.
- **Fire:** fire used 1.20.1's tables, so logs vanished instead of smouldering, BTW wicker and wood
  blocks were fireproof, and the fire on a lit hibachi kept going out.
- **Falling blocks:** sand, gravel and the dragon egg floated on torches; anvils fell; BTW's loose
  blocks never fell below y=0.
- **Stone bricks and tall grass:** stone bricks never turned mossy or cracked; tall grass never spread
  onto old farmland.
- **Plant survival:** cactus on planters, cane on loose dirt and crops on BTW farmland were destroyed
  by 1.20.1 survival checks.

**Root cause.**
- BlockBehaviorMixin routed ticks at HEAD of BlockBehaviour.randomTick / tick. A javap scan of every
  mapped class chain (scratch hook_bypass.json) shows that every mapped block 1.20.1 random-ticks
  overrides randomTick without calling super (39 rows), and 46 mapped blocks override tick without
  super, so the hook never ran for them. NetherPortalBlockMixin ((ag)) was the only per-class patch.
- 1.20.1 random-ticks only states whose cached isRandomlyTicking is true (ServerLevel.java:472-481).
  That flag disagrees with 4.B's needsRandomTick for 17 mapped ids: stone bricks, tall grass, flowers,
  mature crops, leaves below DISTANCE 7.
- The port's Modern-Common 1.5.2 bases had dropped the 1.5.2 setTickRandomly calls (only Block,
  BlockCrops, BlockDetectorRail and BlockFire set it) and many tick bodies: BlockFlower.checkFlowerChange
  (an empty stub), BlockCactus, BlockReed, BlockNetherStalk, BlockMushroom, leaf flagging, BlockFire's
  tickRate, Block.OnDestroyedByFire and CheckForFall parity.
- FC metadata for crops, stems, wart, cocoa, cactus, cane, fire, vines, farmland and saplings had no
  1.20.1 mapping, and the FC-only bits (sapling stage, leaf check flag, stem growth 0-14) lived in an
  in-memory map cleared at server stop.
- Found by the runtime proofs and the review wave once FC ran these ticks; 1.20.1's ticks had masked
  each of them:
  - FC's tree generators test the 1.5.2 build range: base y >= 1, clearance scan y >= 0, top 256
    (Common FCUtilsTrees.java:187-229, 317, 340; Server FCUtilsGenHugeTree.java:46, 69). Every sapling
    at y <= 0 failed and was restored at stage 3: run 2 left 25/25 superflat saplings untouched at 2400
    ticks, where about 7.4 trees were due (P about 2e-4).
  - The port's WorldGenBigTree was a stub and WorldGenerator.setBlockAndMetadata an empty method, so a
    big oak placed nothing and a full-grown 2x2 jungle group cleared its four saplings and placed only
    the dirt under the trunk.
  - 1.20.1 BROWN_MUSHROOM emits light 1 (Blocks.java:208-210). 4.B's emits none (vanilla/server
    Block.java:129 is FCMOD-commented; the active :205 sets no light), and FCBlockMushroomBrown spreads
    off mycelium only from a cell at full light 0 (Common FCBlockMushroomBrown.java:56-61, :67). Runs 1
    and 2: 5/5 sealed dark boxes stayed at one mushroom (about 3e-7 under 4.B).
  - Block.opaqueCubeLookup was never filled (1.5.2 fills it in the Block constructor, vanilla/server
    Block.java:484), so FC trees and huge mushrooms overwrote solid blocks beside them.
  - The port's BlockCrops, BlockCarrot and BlockPotato drops were not 1.5.2: a carrot or potato popped
    by the now-live checkFlowerChange, or harvested, dropped the crop plus 1-2 wheat seeds at any stage.
  - Stripped logs and wood read as stand-ins, so FC leaf decay did not count them as support and FC
    cocoa dropped off them; stripping a trunk ran BlockLog.breakBlock and flagged the canopy.
  - Fire on magma burned out: FCBlockFire keeps a fire alive only over netherrack, End bedrock or a
    DoesInfiniteBurnToFacing neighbour (Server FCBlockFire.java:49-62, 157-176), and magma read as the
    plain stand-in.
  - FC's light rules removed 1.20.1 content on modern soils: grass on moss in dark lush caves
    (BlockFlower.canBlockStay: light 8 or sky) and mushrooms on podzol and nylium in full light
    (FCBlockMushroom: light below 13).
  - BTWLifecycle step 4b built FCBlockStem(105) against the ConcreteBlock(103) placeholder that step 4c
    replaces, so FC melon stems never attached.
  - 1.20.1 AttachedStemBlock.updateShape (AttachedStemBlock.java:38-40) detached every BTW pumpkin stem,
    whose fruit is FC's fresh pumpkin (ProxyBlock 1006), not Blocks.PUMPKIN.

**Fix (general rule).** FC decides the ticks of every FC-mapped vanilla-id BlockState, at the
BlockState level, the single pass every server tick goes through (Forge/src/main/java/btw/forge/
TickPolicy.java). BRIDGE_SPEC.md, "Tick ownership (TickPolicy)", has the rule, the dispatch points and
the exception table.
- **TickPolicy.decide(BlockState)**, a pure function of the state and FC's static data. A state is
  managed when its block maps to an FC block at a vanilla legacy id, is not a ProxyBlock, and FC is
  ready (BTWLifecycle.isFcReady(), set as the last statement of initialize()).
  - Random ticks: gate = fc.getTickRandomly() && the runtime FC class declares updateTick or
    RandomUpdateTick below btw.modern.Block; owner FC when the gate holds, else NONE.
  - Scheduled ticks: FC, unless the 1.20.1 class overrides tick; then 1.20.1 keeps them (leaf
    DISTANCE, the cactus / cane / farmland survival executors, chests, redstone timing, ...). FC_SCHED:
    FIRE, SAND, GRAVEL, DRAGON_EGG and the three ANVILs are FC's although their classes override tick.
  - Exceptions: water NONE; lava runs FC id 11 (gate forced true; scheduled ticks keep id 10);
    redstone ore and ice keep 1.20.1 (the identical 4.B rule); detector rail NONE; dead bush NONE (D3);
    leaves gated on !PERSISTENT. The staging sets PENDING and PENDING_SCHED ship empty.
- **Dispatch mixins on BlockBehaviour$BlockStateBase:**
  - BlockStateBaseTickMixin: a @Redirect of initCache's single PUTFIELD of isRandomlyTicking
    (m_60611_, f_244227_) writes the final gate once, so LevelChunkSection never counts an interim
    1.20.1 value, and caches the tick owners per state (FcBlockStateAccess). tick HEAD (m_222963_) runs
    FC updateTick for FC-owned scheduled ticks and cancels 1.20.1.
  - BlockStateRandomTickMixin, priority 1100: randomTick HEAD (m_222972_) runs FC RandomUpdateTick and
    cancels 1.20.1, after other mods' default-priority randomTick vetoes (Serene Seasons 9.1+). For
    the plants whose 1.20.1 body fires Forge's CropGrowEvent (crops, stems, cactus, cane, wart, cocoa)
    it fires Pre before the FC body (DENY skips it) and Post when the state changed.
  - LavaFluidMixin: LavaFluid.randomTick (m_213812_) is cancelled when the lava state's random owner
    is FC, so the fluid path no longer runs 1.20.1 fire spread a second time.
  - BlockBehaviorMixin's tick and randomTick injects are removed. NetherPortalBlockMixin is deleted:
    the portal falls under the general rule.
- **D1:** TickPolicy.HONOUR_DO_FIRE_TICK = true. With doFireTick false, a scheduled FC fire tick only
  reschedules itself (tickRate + nextInt(10)) and FC fire and lava random ticks are skipped, for fire 51
  and for FC's own fires through ProxyBlock.tick / randomTick (INTEGRATIONS.md #012).
- **LightPolicy:** a state FC random-ticks emits 4.B's Block.lightValue[id], written over
  BlockStateBase.lightEmission at initCache TAIL; LIT / POWERED twin-id states and ProxyBlocks keep
  their own. Today it changes one block, brown_mushroom 1 -> 0. relightIfStale queues a block-light
  check from the FC random tick where an old save stored the 1.20.1 light. Modern-Common BlockEndPortal
  and BlockRedstoneLight (powered) regain their 1.5.2 constructor light, so nothing else goes dark.
- **Modern-Common 1.5.2 base ports** (port code; FC code untouched):
  - constructor setTickRandomly parity: BlockGrass, BlockMycelium, BlockFlower, BlockMushroom,
    BlockCactus, BlockReed, BlockNetherStalk, BlockLeaves, BlockStem, BlockVine, BlockCocoa, BlockFluid,
    BlockStationary (lava only), BlockIce, BlockSnowBlock, BlockPortal, BlockPumpkin, BlockCake,
    BlockButton, BlockBasePressurePlate, BlockTripWire, BlockTripWireSource;
  - BlockFlower: updateTick, onNeighborBlockChange and checkFlowerChange with the 1.5.2 light rule, plus
    the FCBlockPlants members (weeds relay, soil notice on removal, with a same-id guard for 1.20.1's
    attached-stem revert);
  - faithful BlockCrops (AttemptToGrow: no weeds, light 9, hydrated soil, not in the End) and the 1.5.2
    crop drops in BlockCrops, BlockCarrot and BlockPotato;
  - BlockMushroom spread and fertilizeMushroom, with a new WorldGenBigMushroom; BlockCactus; BlockReed;
    BlockNetherStalk (growth, 2-4 wart when ripe); BlockCocoa and BlockLilyPad canBlockStay;
    BlockSapling.isSameSapling and damageDropped;
  - BlockLeaves and BlockLog.breakBlock decay flagging (bit 8);
  - BlockFire tickRate 30, canPlaceBlockAt, onNeighborBlockChange, onBlockAdded; Block.OnDestroyedByFire;
  - Block.CheckForFall with FCEntityFallingBlock, the 32-block range and the world's min build height
    (World.GetMinBuildHeight is an alias of getFcMinBuildHeight); BlockDragonEgg tickRate 5;
  - WorldGenBigTree (full 1.5.2 port with the FCMOD stump) and WorldGenerator.setBlock /
    setBlockAndMetadata;
  - Block.opaqueCubeLookup filled in the constructor (BlockHalfSlab: double slabs);
  - World.GetModernSoilPlantSurvival, asked first by checkFlowerChange (base answer -1).
  *(2026-10-21: later base ports: BlockLeaves' drop methods and the interim canSilkHarvest (ax); ItemDye subtypes
  and MerchantRecipeList's slot rule (aw); Block.DropItemsOnDestroyedByExplosion's 1.5.2 body behind
  World.UsesModernBlockLoot, and the matching dropBlockAsItemWithChance branch (bb); ItemStack.isItemDamaged for
  subtyped items (bc); BlockTripWireSource's placement hooks and BlockLog.onBlockPlaced (bd).)*
- **WorldBridge and the FC-metadata overlay:**
  - derive/apply branches ahead of the generic FACING ones: crop, wart, cactus, cane and fire AGE;
    farmland MOISTURE apply; cocoa FACING | AGE << 2; vine faces; stems (AGE = growth / 2, attached =
    15, facing the fruit FC attached); leaves PERSISTENT <-> bit 4; carved pumpkin and jack o'lantern
    facing;
  - FcMetaOverlay, a SavedData per dimension (data/btw_fc_meta.dat) in place of the in-memory map. An
    entry (block, derivedAtWrite, meta) is stored only when FC's metadata differs from the derived
    value, and is valid only while that block is still there and still derives to derivedAtWrite;
  - flattened-variant switch on metadata writes (stone bricks 98 turning mossy / cracked), only from a
    block that is itself a table variant;
  - the metadata-only write guard: when setBlockMetadata switches the 1.20.1 block (a variant, stem <->
    attached stem), a thread-local scoped to that cell makes BlockBehaviorMixin skip FC onPlace /
    onRemove, since 1.5.2 Chunk.setBlockMetadata never calls onBlockAdded / breakBlock;
  - getFullBlockLightValue = level.getRawBrightness(pos, 0), without sky darkening, as in 1.5.2;
  - a 1-tick LeavesBlock tick after FC places leaves, so 1.20.1 computes their DISTANCE;
  - GetModernSoilPlantSurvival (D3): when the soil has no 1.5.2 identity, the live 1.20.1 canSurvive
    decides.
- **ProxyRegistry:** the bark and stripped forms of the oak, spruce, birch and jungle logs read as log
  17 of their species (read path only); getRemovalLegacyId lets cutting a stripped trunk run FC's log
  removal.
- **BTWLifecycle:** the melon stem's fruit is re-pointed to blocksList[103] after step 4c; Step 5c fills
  opaqueCubeLookup for every FC block (a vanilla id is opaque only when its 1.20.1 block renders solid);
  ModernBlockStandIn writes its own entries.
- **Rule bridges:**
  - FireBlockMixin: FireBlock.getBurnOdds / getIgniteOdds (m_221164_ / m_221166_) answer FC's
    abilityToCatchFire / chanceToEncourageFire for every FC-mapped block;
  - flammable stand-ins: a colliding modern-only state with 1.20.1 fire odds reads as a
    btwModernFlammable<n> stand-in carrying those odds and a burnable rock material, so FC fire burns it
    and FC lava ignites beside it; ModernBlockStandIn.DoesInfiniteBurnToFacing answers the live state's
    isFireSource for the up face (magma);
  - D3 soil hooks on ModernBlockStandIn: wild vegetation and saplings on the DIRT tag, reeds on DIRT or
    SAND, cactus on SAND; crops, wart and hydration stay FC-only;
  - PlantSoilMixin: Forge's Block.canSustainPlant (remap = false) answers FC's soil clause on FC soils;
  - FarmBlockMixin: FarmBlock.turnToDirt (m_269406_) places FC loose dirt (1011);
  - AttachedStemBlockMixin: AttachedStemBlock.updateShape (m_7417_) leaves the fruit attachment of FC
    stems to FC (FCBlockStem.ValidateFruitState).
- **Build:** new ASM task extendFcTreeBuildRange (Forge/build.gradle), wired like
  extendFcPathingBuildRange, rewrites FCUtilsTrees.GenerateTaiga2 / GenerateForest / GenerateTrees and
  FCUtilsGenHugeTree.generate against World.getFcMinBuildHeight / getFcMaxBuildHeight: the base test,
  the clearance-scan floor and the 256 tops (4 methods). FC's own 128 top for birch and spruce stays, a
  4.B rule measured from sea level. Idempotent; any other bytecode shape fails the build.
  *(2026-10-19: extendFcTreeBuildRange is no longer a task but a pass inside extractFcClasses, after
  extendFcPathingBuildRange; see (av).)*
- **Self-audits:** BridgeSelfAudit.runServerStart, called from BTWForgeMod.onServerStarting, logs
  [SELF-AUDIT/tick] and [SELF-AUDIT/light] once per JVM. It runs at ServerStartingEvent because Forge
  fills the BlockStateBase caches at the registry freeze, after FMLCommonSetupEvent (run 1 audited at
  common setup, read the default values and logged 57 warnings), and Blocks.rebuildCache refreshes them
  before the server starts.
- Unchanged: FC code; ProxyBlock tick forwarding (apart from D1); every other BlockBehaviorMixin hook.

**Verification.**
- `:Forge:jar` builds with JDK 17, and extendFcTreeBuildRange logs its 4 patched methods. The refmap
  maps m_60611_, f_244227_ and m_222963_ (BlockStateBaseTickMixin), m_222972_
  (BlockStateRandomTickMixin), m_213812_ (LavaFluidMixin), m_221164_ / m_221166_ (FireBlockMixin),
  m_269406_ (FarmBlockMixin) and m_7417_ (AttachedStemBlockMixin); PlantSoilMixin has no entry (remap =
  false). LinkAudit reports the same 6 pre-existing missing members as before.
- Self-audit: `[SELF-AUDIT/tick] 238 blocks, 778 FC-random states, 200 FC-scheduled blocks, 0 warnings
  (0 expected)` (runs 2 and 3); `[SELF-AUDIT/light] minecraft:brown_mushroom id=39 emission 1->0 (1/1
  states)` and `[SELF-AUDIT/light] 1 mapped blocks emit 4.B light that differs from 1.20.1, 0 warnings
  (0 expected)` (run 3). No BTW-Tick line (no FC tick body threw) and no BTW error in any run.
- Runtime proofs: proof_run.py drives a dev dedicated server over RCON on a fresh superflat world, with
  chunks -2..2 force-ticked in every dimension; 20 TPS throughout. Run 1: the implementation wave. Run
  2: diagnostics after three orchestrator fixes (self-audit timing, melon-stem fruit, metadata-only
  write guard). Run 3: the full plan plus diagnostics after the review wave. Every proof of the run-3
  full plan matched its 4.B expectation. Results (run 1 unless marked):

| Proof | Result | 4.B expectation |
|---|---|---|
| P1 saplings, before the fix | 25/25 overworld and 9/9 End saplings were trees at 30 s | about 0.14 trees; 0 in the End |
| P1 saplings, after | Run 2 (y=-60): 25/25 still saplings at 600, 1200 and 2400 ticks (the floor bug). Run 3, about 176 random ticks each: birch at y=-60 12/25 trees, birch at y=64 5/25, oak at y=64 9/25 (26/75); End 9/9 saplings | about 22 of 75; 0 in the End |
| S1 falling blocks | Run 2: 3 fc_falling_block entities in flight at +3; sand, gravel and the egg land at y=9 in the torch cell (torch gone, no item). ProxyBlock 1011 at y=-50 falls to -60 | FC CheckForFall and FC landing; fall to the world floor |
| S2 anvil | stays at y=10 | never falls |
| S3 survival | planter cactus, loose-dirt cane, FC-farmland carrot: 6/6 survive | 6/6 |
| S4 floating hemp | pops | pops |
| S5 1.20.1-owned ticks | leaf DISTANCE, bubble column, lamp, farmland -> loose dirt: all as expected | kept |
| R1-F1 dry farmland | 81 loose dirt, 0 dirt, 0 farmland | 81 / 0 / 0 |
| R1-C1 carrots, +1200 | 20 plants, none at age 7 (run 3: 1 at age 7) | at most 3 at age 7, 20 |
| R1-LEAVES | flagged leaves -29..-26 kept, -25 / -24 decayed; unflagged isolated leaf kept | exactly that |
| R1-CANE | 6 second segments | at most 8 (4.75 expected) |
| R1-WART | Overworld 81 at age 0; Nether 48 ripe | 81 at age 0; about 45 |
| R1-COCOA | 3 ripe + 7 unripe | at most 7 ripe, 10 |
| R1-END-CACTUS / R1-END-VINE | 0 / 1 | 0 / 1 |
| R1-G1 / R1-G2 grass | 81/81 loose dirt regrew grass / 0 in the End | at least 70 / 0 |
| R1-S1 / R1-S2 saplings | 0 trees, 16/16 saplings / End 0 trees, 4/4 | at most 1 / 0 |
| R1-MYCELIUM | 81 loose dirt turned to mycelium | more than 1 |
| R1-STEMS (run 2, R=300, 2400 ticks) | 3/3 pumpkin stems attached to block_1006, 3/3 melon stems to minecraft:melon | FC fruit, no vanilla pumpkins |
| R1-GATED | bricks mossy 3/3, cracked 3/3; tall grass 7; blood wood leaves decayed; stairs intact | at least 2 each; at least 2; decay; intact |
| R1-ENV | unlit redstone ore, snow, ice (melts at light 12, not 11 or 10) | as 1.20.1 |
| R1-LAVA | wicker 0/4 left; snow in contact melted, control kept | 0 (at most 1); melted / kept |
| R2-NIGHT carrots | 20 at age 0 | 20 / 20 |
| R3 mushrooms | dim box 1; dark boxes 1/1/1/1/1 in runs 1-2, then 6 (run 3 plan) and 5/2/5/5 (run 3 diagnostics) | 1; at least 2 in about 95% |
| F-A / F-B / F-C fire | 4/4 logs smouldering (1043), centre fire burning / wicker 0/4 / planks 0/4 | smoulder; 0; 0 |
| F-D hibachi (run 3) | lit (meta 4) with age-15 fire above, from a redstone torch below | lit by strong power only |
| P portal | no pigmen of either type; portal intact | 0 / 0 |
| X /reload | 9/9 grass, 3/3 mossy | at least 8; 3 |
| PERF, 6,400 logs under 25,600 leaves | mean overworld tick 0.282 ms at randomTickSpeed 3, 0.321 ms at 100 (run 1: 0.353 / 0.449) | small |
| Review-wave checks (run 3) | fire on magma alive at 600 and 2400 ticks; grass (31) on moss in the dark 3/3; leaves held only by a stripped oak log 3/3 after flagging; cocoa on stripped jungle logs 3/3 | kept |

- F-D failed in runs 1 and 2 by design of the proof, not the port: an adjacent redstone block gives weak
  power, and 4.B's FCBlockBBQ.IsGettingPowered counts strong power only (World.isBlockGettingPowered,
  vanilla/server World.java:4157-4160). Run 3 powers it with a redstone torch.
- Not seen at runtime: a big oak (none among the 9 run-3 oak trees, about 0.9 expected at 1 in 10; a
  differential harness ran the port and the 1.5.2 WorldGenBigTree on 300 seeds with 0 differing cells
  and the stump on 200/200 trees), a 2x2 jungle tree, doFireTick false, CropGrowEvent with Serene
  Seasons installed, client rendering after the opaque-table fill (FC face culling), and an
  existing-world upgrade (X2).

**Caveats.**
- Bone meal still grows FC-owned plants the 1.20.1 way (decision D2, deferred). StemBlock.performBonemeal
  calls BlockState.randomTick at AGE 7, which now gives one FC growth roll.
  *(2026-10-15: superseded by 2026-10-15 (at). Bone meal is FC fertilizer (BoneMealPolicy), and
  BonemealableGateMixin cancels performBonemeal on every state whose growth FC governs, StemBlock
  included, so bone meal gives no growth roll at all.)*
- Modern-only plants keep 1.20.1 rules (decision D4, deferred): acacia, dark oak, cherry and mangrove
  saplings and leaves, azalea, beetroot, sweet berries, bamboo, kelp, cave vines.
  *(2026-10-15: superseded by 2026-10-15 (at). The saplings, azaleas, beetroot, the sniffer crops and the
  Nether fungi run FC plant classes ("twins"); bamboo, kelp, the vines, sweet berries, hanging propagules and
  chorus are paced. The leaves of the new species keep 1.20.1 decay on purpose: it already has 4.B pacing.)*
- D3 remainder (review R7): dead bushes on grass blocks or dirt (1.20.1 places them in swamps, mangrove
  swamps and giant taigas) are removed on a neighbour change, because FCBlockDeadBush accepts only sand
  12, both in FC's neighbour rule and in PlantSoilMixin's answer to 1.20.1's updateShape. Dead bushes on
  red sand and terracotta stay: those soils have no 1.5.2 identity, so GetModernSoilPlantSurvival hands
  the decision to 1.20.1. Lily pads on ice can no longer be placed and existing ones pop, as in 4.B.
- Waterlogged blocks are not water to FC (review R2): sugar cane whose only water is in waterlogged
  blocks pops on a neighbour change, and FC farmland watered only by them dries out. The fix
  (WorldBridge.getBlockMaterial answering water for a waterlogged cell) changes every FC solidity read
  and needs its own audit.
- FC-grown huge mushrooms convert to FC caps 1078 / 1079 (the MUSHROOM_STEM branch was removed: 4.B
  identity, flammable, FC drops), which render as fcblockstub for every meta (blockstates/block_1078.json;
  a model gap that predates this change).
- Every lava cell counts as settled lava (id 11), so actively flowing lava also starts fires.
- More states random-tick than in 1.20.1 (every non-persistent leaf, tall grass, flowers, torches, stone
  bricks), as in 4.B; PERF above measures the cost.
- Detector rail, redstone ore and ice keep 1.20.1 ticks. Redstone torches 75 / 76 are a known flag gap
  (ConcreteTorchBlock placeholders; the self-audit logs them at INFO). The legacy snow block 80 has its
  4.B flag, but the 1.5.2 BlockSnowBlock.updateTick melt is not ported, so the real-body clause keeps it
  off FC; pumpkin 86 and cake 92 (re-validation ticks only) likewise.
- D1 gaps: the burning ladder (FCBlockLadderOnFire 1052) and the campfire tile entity still spread fire
  with doFireTick false; fire they start obeys the rule.
- Flammable stand-ins: about 13 colliding fire-odds buckets compete for about 9 free ids, so later
  buckets reuse the nearest stand-in of their shape and their FC odds are approximate; the ids are
  per session and never written. Modern blocks that 1.20.1 lava ignites only through ignitedByLava
  (barrel, loom, cartography and smithing tables) read as non-burnable, so FC lava beside them starts no
  fire (review R9).
  *(2026-10-15: the FC plant twins of (at) take five ids of the free range (174, 169, 168, 167, 166), so
  four flammable stand-ins remain (162-165) and more buckets share one.)*
  *(2026-10-20: log2 takes 165 (ay), so three flammable stand-ins remain (162-164), two when the plant stand-in
  takes 162.)*
- lightOpacity is not filled: ProxyBlock reads it for light blocking, so filling it would change
  1.20.1 lighting for every FC block. The opaque-cube fill cannot fix FC-only ProxyBlocks whose port base
  answers isOpaqueCube true (1036 wood door, 1084 wicker pane, 1085 grate, 1086 slats): Modern-Common
  BlockDoor and BlockPane need their 1.5.2 false overrides.
  *(2026-10-26: they have them since RANB-PARITY (section 3), so the opaque-cube fill no longer counts 1036 and
  1084-1086.)*
- FC machines that copy a block by id and metadata (FC pistons, the block dispenser) recreate a plain
  log from a stripped log or wood.
- The farmland double trample stays open: 1.20.1 FarmBlock.fallOn and FC onFallenUpon both run (both
  now give loose dirt).
  *(2026-10-21: closed by (be): farmland FALL is 1.20.1's (ADAPTER(D9b)), so FC's onFallenUpon no longer runs, and
  1.20.1's one roll uses 4.B's 0.75 threshold (FarmBlockMixin).)*
- The non-tick hook families remain open: pistons, liquid hardening, beds and hardcore spawn, beacon and
  ender-chest casts, the anvil workbench, buddy-block notifications, wooden doors on redstone,
  explosions (decision D6, stone's position-aware blast resistance).
  *(2026-10-21: settled since: liquid hardening (bf); beds (be; 4.B's random hardcore respawn is still open, triage
  CF2-U1-HS); the beacon and ender-chest casts (the block-entity guard of (be); the Phase-4 tile-entity bridge is
  still open); the anvil workbench (be); wooden doors on redstone (ruled 1.20.1, an explicit row of (be)). Pistons,
  buddy-block notifications and explosions remain.)*
  *(2026-10-28: explosions are settled by (bn), except TNT's chain (PENDING); the hardcore respawn by (br); the beacon
  cast by (bq), whose FC-backed block entities are the Phase-4 bridge for the beacon (the ender chest waits for its
  own unit). Pistons and buddy-block notifications remain.)*
- Supersedes the NetherPortalBlockMixin part of 2026-10-10 (ag) and corrects the 2026-07-08 "fire
  spread" and "grass/mycelium spread" claims (both annotated there).

## 2026-10-15 (at) Bone meal is FC fertilizer, post-1.5.2 plants grow by 4.B rules, removed blocks stay removed

**Symptom (maintainer rulings of 2026-10-15 on decisions D2, D4 and D5, which (as) deferred; runtime
baseline with `/btwproof use` on a dev server at 9c05019).** Three kinds of 1.20.1 content undercut 4.B's
balance:
- **A. Bone meal grew plants instantly.** At 9c05019 bone meal grew carrots from age 0 to 2 on vanilla
  farmland and from 0 to 4 on FC farmland 1087, a pumpkin stem to age 4, cocoa, beetroot, sweet berries and
  saplings, turned a brown mushroom on mycelium into a huge mushroom and sprouted grass and flowers on a
  grass block. Every use was consumed and the soil stayed unfertilized. In 4.B bone meal never grows a
  plant: FCItemDye.onItemUse applies the clicked block's AttemptToApplyFertilizerTo and uses the item
  exactly when that is true (SrcServer FCItemDye.java:16-30, 71-82); plants relay it to their soil
  (FCBlockPlants, SrcServer FCBlockPlants.java:112-126); fertilized FC farmland and planters double crop
  growth until the next full stage. Vanilla's growth code (vanilla/server ItemDye.java:121-274) is dead in
  4.B. Read from code, not run at baseline:
  - off-hand bone meal always took the 1.20.1 path;
  - netherrack beside nylium grew nylium, and an FC-mapped block under water grew seagrass or coral;
  - every success played levelEvent 1505 (particles and sound), where 4.B plays nothing;
  - on a weedy FC plant the port fertilized, where 4.B pulls the weeds and keeps the bone meal;
  - 1.20.1 white dye fertilized like bone meal (LegacyVariants.java:159 reads it as FC dye 15);
  - the vanilla dispenser grew crops (4.B ejects bone meal), and the FC block dispenser kept every item
    without its own block-dispenser use (4.B spits it out);
  - bees advanced FC-owned carrots, potatoes, legacy wheat and stems a stage per visit.
- **B. Post-1.5.2 plants grew at 1.20.1 rates (D4).** Acacia, cherry and dark oak saplings and planted
  mangrove propagules grew about 18x faster than FC saplings, also in the End; beetroot, the sniffer crops,
  sweet berries, bamboo, kelp, cave / weeping / twisting vines, hanging propagules and chorus grew 1.6x to
  20x faster than their nearest 4.B relatives. Azaleas and crimson and warped fungi never grow in 1.20.1
  without bone meal, so under ruling A they would never grow.
- **C. Blocks 4.B removed came back (D5, with CT6).** FC stubbed them, retired their ids or removed their
  recipes (FCRecipes.RemoveVanillaRecipes, Server FCRecipes.java:5112-5776: 94 crafting records and 14
  furnace inputs; the map-extending recipe and the two-tool repair, vanilla/server
  CraftingManager.java:49-51, 283-382). 1.20.1 re-exposed every one: crafting and cooking recipes; chest,
  archaeology, fishing and barter loot (mineshaft activator rails, outpost and fishing tripwire hooks,
  ruined-portal gold plates, village torches and snow blocks, piglin nether bricks); structures
  (ancient-city comparators, the mansion's trapped chest, jungle-temple tripwire and hooks); the vanilla
  creative tabs and JEI; string placing tripwire; TNT and hopper minecarts. CHANGELOG known issue "crafting
  recipes that BTW 4.B removed can still be crafted". Research: scratchpad t2/bonemeal.md (BM ids),
  t2/modern_plants.md (S ids: its lore calls, section 2.2, settled by the ruling) and t2/removed_blocks.md.

**Root cause.**
- A: Modern-Common `btw.modern.BlockFlower extends Block`, not FCBlockPlants, and the port had not ported
  FCBlockPlants' AttemptToApplyFertilizerTo relay or its onBlockActivated weed pull (its own comment said
  so). FCItemDye therefore returned false for every 1.5.2 flower-family plant (crops including hemp and
  legacy wheat, stems, saplings, mushrooms, wart, tall grass, flowers): FC said no,
  ServerPlayerGameMode.useItemOn went on, and BoneMealItem.useOn ran 1.20.1's performBonemeal. Around it:
  - ServerPlayerGameModeMixin's HEAD pre-emption ran FCItemDye for the main hand only, before block
    activation, Forge's RightClickBlock event and the spectator check; the off hand went straight to 1.20.1;
  - nothing guarded direct performBonemeal callers (mods, growCrop, GrassBlock, GrowingPlantBodyBlock) or
    Bootstrap's BONE_MEAL dispenser behaviour (DispenseItemBehavior.java:433-446, growth);
  - Modern-Common Item.OnItemUsedByBlockDispenser returned false, where 1.5.2's FCMOD default spits the item
    out (vanilla/server Item.java:1232-1240);
  - Bee$BeeGrowCropGoal.tick writes the next age straight into the world, outside the random tick.
- B: these plants have no legacy id, so TickPolicy.decide returned null and no FC rule reached them. FC's
  nearest relatives exist as classes (FCBlockSapling, FCBlockCarrot, FCBlockWheatLegacy, FCBlockMushroom),
  but nothing put an instance at the plants' cells. FC has no class whose updateTick could drive bamboo,
  kelp, vines, berries or chorus: FCBlockReed hard-codes air above and a 3-block cap, FCBlockVine works on
  side bits, FCBlockCocoa on log attachment.
- C: Modern-Common CraftingManager.RemoveRecipe / RemoveShapelessRecipe only returned true and RecipeBridge
  only added `fc_*` recipes, so no removal reached the 1.20.1 RecipeManager. No other hook knew which ids
  4.B removed: the decision (D5, with CT6) was open.

**Fix (general rules).** One rule per ruling. BRIDGE_SPEC.md has the hooks ("Tick ownership (TickPolicy)",
post-1.5.2 plants; "Bone meal (BoneMealPolicy)"; "Stays removed (RemovedContent)"); INTEGRATIONS.md #017,
#018 and #019 have the integration entries.
- **A. Bone meal is FC fertilizer and nothing else** (Forge/src/main/java/btw/forge/BoneMealPolicy.java).
  `route(state)`: FC_TARGET for every FC-mapped block (vanilla ids and ProxyBlocks run their own
  AttemptToApplyFertilizerTo); SOIL_RELAY for a post-1.5.2 plant whose growth BTW governs
  (TickPolicy.isFcGrowthGoverned), relayed to the soil below the plant's lowest cell as FCBlockPlants
  relays; VANILLA otherwise. On an owned route 1.20.1's performBonemeal and growWaterPlant never run, the
  item is used up only when FC took it, and nothing plays.
  - Modern-Common BlockFlower: FCBlockPlants.AttemptToApplyFertilizerTo (Client FCBlockPlants.java:129-143)
    and onBlockActivated, the weed pull with FX 2001 (:84-102). This alone makes FCItemDye 4.B for the whole
    flower family.
  - ServerPlayerGameModeMixin: no pre-emption for FC dye 15 (BoneMealPolicy.isFcBoneMeal). 1.20.1's own
    order runs: Forge's RightClickBlock and the spectator check, then blockstate.use, where
    BlockBehaviorMixin.btw$use / ProxyBlock.use run Stage 1 (FC onBlockActivated: the weed pull) and Stage 2
    (FCItemDye). That is 4.B's activateBlockOrUseItem order (vanilla/server
    ItemInWorldManager.java:376-407).
  - BoneMealItemMixin, BoneMealItem.useOn HEAD: sneaking, the off hand and targets without Stage 2. The
    server applies FC fertilizer through WorldBridge, shrinks by one and returns CONSUME, else PASS; the
    client (client/BoneMealPrediction, through DistExecutor) runs the same FC decision on the read-only
    ClientFcWorld for the swing (SUCCESS or PASS) and never shrinks.
  - BonemealableGateMixin, isValidBonemealTarget / performBonemeal HEAD on the 31 1.20.1 classes that
    declare them: false / cancelled for an owned state, decided per state, so a state BTW does not govern
    keeps 1.20.1.
  - BoneMealPolicy.onBonemeal (BonemealEvent, LOW, DEFAULT results only): other applyBonemeal callers get
    ALLOW when FC took the fertilizer (Forge then uses one item), else the event is cancelled.
  - Only `minecraft:bone_meal` is fertilizer (isFertilizer); BoneMealPolicy.mayUseAsFcItem in
    ProxyBlock.tryPlaceHeldItem keeps 1.20.1 white dye out of Stage 2.
    *(2026-10-20: white dye is FC 351:271 since (aw), so mayUseAsFcItem lets it into Stage 2, where FCItemDye acts
    only at damage 15 and nothing happens. Since (ba) no FC item runs at the head of useItemOn.)*
  - Dispensers: `DispenserBlock.registerBehavior(Items.BONE_MEAL, new DefaultDispenseItemBehavior())` at
    common setup (eject one, aux 1000 and 2000: 4.B's BehaviorDefaultDispenseItem); Modern-Common
    Item.OnItemUsedByBlockDispenser is the 1.5.2 default (FCBlockBlockDispenser.SpitOutItem inlined, aux
    1000, true) for every item without an override of its own.
- **B. Post-1.5.2 plants follow the 4.B rule of their nearest relative** (TwinRegistry, FcPaceAccess,
  TickPolicy, BlockStateBaseTickMixin, BlockStateRandomTickMixin, Modern-Common ModernTwins).
  - **Twins** (owner FC): a plain instance of an existing FC plant class at a reserved legacy id, built by
    TwinRegistry.createTwins in BTWLifecycle step 5d, after the opaque-table fill and before fcReady. FC's
    world reads (ProxyRegistry.getWorldBlockId) see the live 1.20.1 plant as the twin's id; identity routing
    (getBlockId, BlockBehaviorMixin, items) stays 0, so 1.20.1 keeps placement, shape-update survival, loot,
    collision and scheduled ticks. TickPolicy.decide gives every twin state random owner FC with the twin's
    id (gate true) and scheduled owner VANILLA, so FC's updateTick runs unchanged:

| Family | 1.20.1 blocks (twin states) | Twin (id) | 4.B rule |
|---|---|---|---|
| sapling | acacia, cherry, dark oak saplings; planted mangrove propagule (26) | FCBlockSapling (174) | survival (light 8 or sky, sapling soils), not in the End, light 9 above, four stages at 1/64, then the species' own 1.20.1 tree; dark oak takes FC's jungle 2x2 rule (all four at stage 3); the propagule keeps clay and mud (S3) |
| azalea | azalea, flowering azalea (2) | FCBlockSapling (169) | the sapling rule, then AzaleaTreeGrower (S1); 1.20.1 fire odds 30 / 60 |
| beetroot | beetroots (4) | FCBlockCarrot (168) | the 4.B root crop: 0.1 per tick, seven levels, light 9, hydrated FC soil, weeds stop it, fertilizer doubles it, not in the End |
| torchflower, pitcher | torchflower crop (2); pitcher crop, lower half (5) | FCBlockWheatLegacy (167) | the 4.B BlockCrops pace of legacy wheat, 0.05 (S4); the torchflower crop becomes the TORCHFLOWER block at level 7; the pitcher shows AGE 3 (upper half) at level 6 |
| fungus | crimson, warped fungus (2) | FCBlockMushroom (166) | FC's mushroom-on-mycelium rule on the species' own nylium: 1 in 50 random ticks to its planted huge fungus, not in the End, no spread (S11) |

  - Load-time mixins on the FC classes, keyed on the twin id so FC's own blocks are untouched (`@Pseudo`,
    `remap = false`, `btw.forge.mixin.fcext`): FCBlockSaplingTwinMixin (growTree HEAD: the species' 1.20.1
    tree through TwinRegistry.growSaplingTwin, after FC's own 2x2 stage check for dark oak; CanGrowOnBlock
    RETURN: the live plant's 1.20.1 soil as well) and FCBlockMushroomTwinMixin (updateTick HEAD:
    TwinRegistry.fungusTick). A tree that does not fit leaves every sapling of its square at FC stage 3,
    FC's restore rule.
    *(2026-10-18: the two twins also run other mods' plants. Compat wave C1 (maintainer decision D2 (b)): every
    other SaplingBlock with STAGE and no legacy id, for example HarvestCraft's 50 fruit-tree saplings, is a
    sapling twin of species 0 and grows into its own tree. Compat wave C2: a sapling of another mod that is not a
    SaplingBlock joins through TwinRegistry.registerModdedSapling (Dynamic Trees' DynamicSaplingBlock); its
    Nether fungi take the FCBlockMushroom twin, 1 in 50 random ticks and never in the End, as vanilla crimson and
    warped fungi do. See 2026-10-18 (au) and BRIDGE_SPEC.md, "Tick ownership (TickPolicy)", twins.)*
  - WorldBridge: the twin metadata codec first in derive / apply (TwinRegistry.deriveMeta / stateForMeta:
    species, or the crop level of AGE); setTwinMetadata (the torchflower's level 7 as a block switch under
    the metadata-only write guard; the pitcher's upper half placed as PitcherCropBlock.grow places it, or
    the write refused when there is no room); isMetaFullyDerived false for twin blocks, so FcMetaOverlay
    keeps the sapling stage and the levels between AGE steps; a waterlogged twin cell is water to FC;
    collision from the live block; DropModernBlockLoot. Modern-Common Block /
    BlockCrops.dropBlockAsItemWithChance drop a twin's live 1.20.1 loot (never carrots, seeds or an item of
    the twin's id). ProxyRegistry.getRemovalLegacyId gives the crop twins' removal to FC, so FC farmland
    under a harvested crop twin becomes loose dirt (S16).
  - **Paced plants** (new owner PACED 3): the 1.20.1 body runs on a fraction `pace` of the random ticks
    (TickPolicy.pacedRollPasses, one nextFloat), never in the End unless exempt; pace = FC relative's chance
    per tick / 1.20.1's, so the mean is FC's. The pace and End flag are cached per state (FcPaceAccess,
    BlockStateBaseTickMixin).

| Plant (paced states) | 4.B relative | Pace | End |
|---|---|---|---|
| bamboo shoot, bamboo (1 + 12) | FCBlockReed, 1/32 per segment; 1.20.1 1/3 | 3/32 | banned |
| kelp (26) | FCBlockReed, 1/32; 1.20.1 0.14 | 0.2232 | banned |
| cave, weeping, twisting vines (52 + 26 + 26) | FCBlockVine hanging pace, 1/16; 1.20.1 0.1 | 0.625 | banned |
| sweet berry bush (4) | FCBlockCocoa, 1/20 per stage; 1.20.1 1/5 | 0.25 | banned |
| mangrove propagule, hanging (20) | FCBlockCocoa, 1/20 per age; 1.20.1 every tick | 0.05 | banned |
| chorus flower (6) | FCBlockCactus counter, 16 ticks per step; 1.20.1 every tick | 1/16 | allowed (S10: the End's own plant) |

  - TickPolicy.isFcGrowthGoverned: FC-owned growing plants (FC plant, vine or cactus material), ProxyBlock
    plants, twin and paced states (pitcher upper halves and hanging propagules included); a growing-plant
    body (kelp, weeping, twisting and cave vine plants) follows its head. Bone meal (A) and bees read it.
  - BeeGrowCropGoalMixin: a @Redirect of the one Level.getBlockState in Bee$BeeGrowCropGoal.tick. A visit to
    a governed plant vanilla would grow runs that plant's own random tick once (FC's body or the paced
    roll), answers air so vanilla's growth branch sees nothing, plays 2005 when the plant changed, and
    counts toward the bee's ten crops where vanilla counts it (BeeAccessor,
    incrementNumCropsGrownSincePollination) (S17).
  - Kept on 1.20.1: the leaves of the new species and nylium decay (already 4.B pacing, S13, S14), and
    blocks that only multiply by bone meal (S15).
  - BridgeSelfAudit.auditTwins (after the tick audit): twin ids and classes, world-read ids, the codec round
    trip, the cached owners, gates and paces, and any minecraft block that grows by random tick or bone meal
    without a classification.
  - The stand-in range 159-174 is shared: the twins take the free ids from the top (174, 169, 168, 167,
    166), before any stand-in exists; the stand-ins allocate from the bottom (160 solid, 161 collider,
    flammable from 162), so four flammable stand-ins remain. ModernBlockStandIn.FIRST_FREE_ID / LAST_FREE_ID
    / isFreeId are package-visible for it.
    *(2026-10-15: four, or three when the pack has a modded crop: compat wave C1 allocates the plant stand-in
    (ModernPlantStandIn) right after the collider, only then. The 2026-10-18 production smoke with every optional
    mod installed logs `plant stand-in=162`.)*
    *(2026-10-20: three, or two with the plant stand-in: log2 holds 165, see (ay).)*
- **C. Stays removed** (Forge/src/main/java/btw/forge/removed/RemovedContent.java): one table drives every
  hook. Each row has the 1.20.1 item and blocks, the tier, the expected 1.5.2 ids, the 4.B citation, and the
  structure rule or BTW successor.
  - **T1 stubbed (12 rows):** hopper, comparator, daylight detector, dropper, trapped chest, both weighted
    plates, activator rail, TNT and hopper minecarts, tripwire hook, tripwire. No recipes, loot filtered,
    hidden from creative (and so from JEI), string no longer places tripwire, structure substitutes (trapped
    chest -> chest and dropper -> dispenser keep their block-entity data; comparator -> repeater, same
    FACING, delay 1; activator rail -> rail; weighted plates -> stone pressure plate; hopper, daylight
    detector, tripwire hook and tripwire -> air), and freshly spawned TNT and hopper minecarts become plain
    minecarts (a hopper cart's contents drop).
  - **T2 retired (8 rows):** crafting table, chest, ladder, torch (and wall torch), snow block, clay block,
    cobweb, the nether brick item. No recipes, hidden from creative, loot (and a trade) hands out the BTW
    successor at the same count (BTW workbench, chest, ladder, the unlit finite torch, solid snow, unfired
    clay, BTW web, BTW nether brick). Generated copies stay legacy blocks, which FC's legacy drop rules
    convert when broken. RetiredIngredientWidening lets non-FC recipes that take a T2 id also take FC's
    legacy conversion target for it; the torch stands for the nether-coal torch (lit 1057 or unlit 1033),
    never the finite torch.
  - **T3 recipe-only (8 rows):** the five wooden tools, the stone hoe, the snow layer, the furnace. No
    recipes, loot filtered, still in creative, as in 4.B.
  - Recipes: Modern-Common CraftingManager records every RemoveRecipe / RemoveShapelessRecipe call
    (RemovedVanillaRecipe, parsed as 1.5.2 parsed it). RecipeBridge.injectRecipes then runs, on the map it
    is about to install (so /reload re-applies it), FcRecipeRemovals: (1) the FC records against
    `minecraft:` crafting recipes by result and ingredient multiset, shape ignored, tags resolved,
    count-insensitive only when no exact count matches, results equivalent across a recipe group mapping to
    one legacy block (the 16 beds); (2) the MAP_EXTENDING and REPAIR_ITEM special recipes; (3) every
    `minecraft:` smelting, blasting, smoking and campfire recipe whose input FC took out of the 1.5.2
    furnace table (the difference between that table and FC's live list; clay keeps its brick); (4) always,
    every recipe of any type and namespace whose result is a table item, except FC's own injected recipes,
    which are reported instead. Passes 1-3 sit behind RecipeBridge.MIRROR_FC_VANILLA_REMOVALS (true). Then
    RetiredIngredientWidening (Ingredient.merge, or a Forge CompoundIngredient for other ingredient types;
    the recipe rebuilt with the same id), skipping recipes whose result FC makes its own way and reporting
    recipes stranded by a T1 or T3 ingredient.
  - Loot: FCRemovedContentLootModifier, one global loot modifier with no conditions
    (`data/betterthanwolves/loot_modifiers/removed_content.json`, emitted by FCLootOverrides), which reads
    the table at run time.
  - Structures: StructureTemplateRemovedMixin (the Forge processBlockInfos overload, RETURN, `remap =
    false`) and StructurePieceRemovedMixin (StructurePiece.placeBlock HEAD, @ModifyVariable on the state).
  - RemovedCreativeTabs (BuildCreativeModeTabContentsEvent, LOWEST) and RemovedContentEvents
    (RightClickBlock setUseItem DENY for string; EntityJoinLevelEvent cancel and end-of-tick replacement of
    fresh T1 minecarts, the VanillaMobReplacer pattern, carts loaded from disk left alone; villager and
    wanderer trade listings wrapped at LOWEST as a guard).
  - RemovedContent.audit at common setup: every FC block of an FC stub or inert class and every FCItemStub
    must be a T1 row; every row's blocks and item must map to the cited 1.5.2 ids; every T2 successor must
    resolve.
  - Left alone, by the ruling's scope: removed blocks already in a world or placed by commands (`/give` and
    `/setblock` stay op tools, as in 4.B); modern-only blocks 4.B never had; CT6's other rows.
- Unchanged: FC code; LightPolicy; PlantSoilMixin; every other tick rule of (as).

**Verification.**
- The dev-server build the proofs ran compiles with JDK 17. The refmap maps m_6225_ (BoneMealItemMixin),
  m_7370_ and m_214148_ (BonemealableGateMixin), m_7179_ (ServerPlayerGameModeMixin, existing target),
  m_8037_ and Level.getBlockState m_8055_ (BeeGrowCropGoalMixin), m_27871_ (BeeAccessor) and m_73434_
  (StructurePieceRemovedMixin); StructureTemplateRemovedMixin and the two fcext mixins have no entry (remap
  = false). LinkAudit during the wave: the 6 pre-existing missing members, jar precedence drift 0.
- Self-audits, every proof run:
  - `[SELF-AUDIT/tick] 238 blocks, 778 FC-random states, 200 FC-scheduled blocks, 0 warnings (0 expected)`
    (unchanged from (as));
  - `[SELF-AUDIT/twin] 6 twin families (11 blocks), 41 twin states, 173 paced states, 0 warnings`, with the
    twins at 174 (sapling), 169 (azalea), 168 (root crop), 167 (seed crop) and 166 (fungus);
  - `[SELF-AUDIT/light] 1 mapped blocks emit 4.B light that differs from 1.20.1, 0 warnings (0 expected)`
    (unchanged);
  - `[REMOVED-AUDIT] stays-removed table: 12 T1, 8 T2, 8 T3 rows; FC stub blocks [154, 149, 150, 151, 158,
    146, 147, 148, 157, 131, 132] and items [404, 407, 408] covered; 0 warning(s)`.
- Recipe log: `FC vanilla-recipe removals on: 93/94 records matched (3 count-insensitive, 1 unmatched), 108
  crafting + 2 special + 20 cooking recipes removed (removed furnace inputs [4, 12, 14, 15, 16, 17, 21, 56,
  73, 87, 129, 153]); stays-removed rule removed 0 more (0 outside minecraft); 130 recipes removed in
  total`. The unmatched record is FC's 6-stick fence, which has no 1.20.1 counterpart; the count-insensitive
  ones are the yellow and red dyes and the iron door. Widening: 13 recipes (the eight chest boats and the
  bamboo chest raft, lantern, shulker box, terracotta, red nether bricks); left on FC's route:
  chest_minecart, nether_bricks, nether_brick_fence; stranded: blast_furnace, smoker, furnace_minecart
  (furnace), crossbow (tripwire hook).
- Runtime proofs: dev dedicated server over RCON on fresh superflat worlds (the (as) harness), with
  `/btwproof use`, which drives ServerPlayerGameMode.useItemOn with a FakePlayer as a use packet does.

| Proof (bone meal: 88/92 checks as expected) | Result | 4.B expectation |
|---|---|---|
| P1 / P1b bare vanilla / FC farmland | becomes 204 / 1088, one used; a second use kept | same |
| P2 / P3 carrots, potatoes, wheat on vanilla farmland; carrots on 1087 | soil 204 / 1088, crops still age 0, one used; a second use kept | fertilize the soil, never grow |
| P5a / P5b BTW wheat / carrots on weedy 1087 | weeds pulled (1087 meta 0), soil not fertilized, bone meal kept | weeds first |
| P6 hemp, P7a stem, BM2 dandelion on FC farmland | soil 1088 / 204 / 1088, stem age 0 | relay to the soil |
| P8 / P12 / P13 / P11 / P14a-d oak sapling, cocoa, brown mushroom (5 uses), short grass, wart, cactus, flower on grass, mycelium | unchanged, kept | nothing |
| P9 / T11 oak / acacia sapling in planter soil | planter soil fertilized (1092 bit 2), sapling stage 0 | planter fertilized |
| grass block, P17 sand under water | no grass, flowers or seagrass; kept | nothing |
| T1-T4 beetroot, torchflower, pitcher twins on 60 / 1087 | soil 204 / 1088, crops age 0; a second use kept | relay to the soil |
| T5-T10, G1, G2 berries on grass / 1087, bamboo shoot and stalk, twisting vines (head and body), hanging propagule, cave vines (head and body) | nothing, or soil 1088 under the berries; no new segment, no berries | relay or nothing |
| T12-T14 acacia sapling, azalea, crimson fungus on soil that takes no fertilizer | unchanged, kept | nothing |
| VAN pink petals (not governed) | 1.20.1 bone meal: CONSUME, flower_amount 2 | 1.20.1 kept |
| WD1-WD4, WD6 white dye by hand on farmland, carrots, BTW wheat, 1087, beetroot | nothing, kept | not fertilizer |
| P15a / P15b dropped bone meal on farmland / 1087 | 204 / 1088, one item used | FC's item rule |
| P19 vanilla dispenser over carrots on farmland | one ejected per pulse, the farmland took it, carrots age 0 | eject |
| P20 FC block dispenser | one bone meal spat out in front, 3 left | SpitOutItem |

  The four misses: P16 (setblock netherrack became FC block 1076 on placement, so "still netherrack" failed;
  bone meal did nothing and was kept, as in 4.B) and P7b (a stem attached to a vanilla pumpkin detaches by
  FC's rule, FC's fruit being the fresh pumpkin 1006; its soil check passed) are errors in the test's
  expectation. WD5 (two checks) is the dropped white dye gap below.

| Proof (plants; W1 / W2 / W3 = 29 / 205 / 403 random ticks per block) | Result | 4.B expectation | 1.20.1 |
|---|---|---|---|
| A acacia, cherry, planted propagule present at W1 | 9/9 each | 8.99 | about 0.7 |
| A same at W2 | 8, 6 and 4 of 9 | 5.41 | 0 |
| A azalea / flowering azalea trees at W3 | 4/5, 3/4 | 4.37, 3.49 | 0 (never) |
| A2 dark oak 2x2 groups present at W1 / W2 / W3 | 8/8, 8/8, 4/8 | 8.00, 7.14, 1.77 | 0 at W1 |
| A2 FC oak controls present at W1 / W3 | 8/8, 1/8 | 7.99, 1.01 | same |
| B beetroot / torchflower / pitcher upper half ripe at W1 | 0, 0, 0 of 16 | 0.48, 0.01, 0.06 | 15.3, 15.8, 16.0 |
| B same, cumulative at W2 | 16, 13, 16 | 16.0, 14.2, 15.1 | 16 |
| B FC carrot control at W1 / W2 | 1, 15 of 16 | 0.48, 15.5 | same |
| B sweet berries at age 3, W1 | 4/16 | 2.92 | 14.9 |
| B bamboo with 3+ segments, W1 / W2 | 2/16, 13/16 | 1.05, 15.26 | about 16 |
| B kelp columns with 1+ / 3+ growths, W1 | 13, 0 of 16 | 9.6, 1.05 | 15.7, 12.4 |
| B cave / twisting vine cells grown, W1 | 27, 22 | 29.3 | 46.9 |
| B hanging propagules at age 4, W1 | 1/16 | 0.98 | 16 |
| Re-check (randomTickSpeed 300): bamboo shoots left at 1,200 / 2,400 ticks | 5/49, 1/49 | 3.1, 0.2 | 0 |
| Re-check: azaleas / FC oak controls grown at 2,400 ticks | 7/36, 13/36 | 10.7, 10.7 | azaleas never |
| Re-check: bone meal or a stick on weedy 1087 under beetroot, torchflower, carrots, BTW wheat | weeds pulled, bone meal kept (left 4) | the same | bone meal grows the crop |
| C chorus flowers not grown yet at W0 / W1 (End) | 8, 4 of 16 | 10.1, 2.56 | 0 |
| C crimson / warped huge fungi grown at W2 | 16/16 each | 15.74 | 0 (never) |
| C crimson fungus on warped nylium (own-nylium control) | 4/4 stay | 4 | 4 |
| C End: cherry saplings, bamboo, fungi | 4/4 saplings stay, 0/4 bamboo grew, 4/4 fungi stay | 4, 0, 4 | 0, 4, 4 |
| B acacia leaves with no log; crimson nylium under stone | all decayed; all gone | 1.20.1 already 4.B pacing | same |

| Proof (removed: 72/73 checks as expected) | Result |
|---|---|
| Loot: mineshaft activator rail, outpost and fishing tripwire hook, ruined-portal light plate, barter nether brick, mineshaft torch, snowy-house snow block and furnace, warm ocean ruin wooden hoe, bonus-chest wooden pickaxe | none |
| Loot successors: barter BTW nether brick, mineshaft unlit finite torch, snowy-house solid snow | 9, 4, 12 |
| Structures: mansion 1x2_s2 trapped chest / chest; ancient city comparator / repeater; jungle pyramid tripwire hooks and tripwire | chest and repeater placed; no comparator, hook or tripwire left |
| Trades: a summoned vanilla villager survives; wanderer sells any of 12 removed ids | no; none |
| String used on stone | nothing placed, string kept |
| Summoned TNT / hopper minecart | plain minecarts; the hopper cart's 5 dirt dropped |
| Recipes for the 27 table items (`/btwproof recipes`, any type and namespace) | none |
| `setblock` of nine T1 blocks | placed (left alone) |

  The miss: a `/give`'d tripwire hook used on stone placed nothing (use FAIL). The stays-removed rule leaves
  a removed item's own placement alone (RemovedContent.placesRemovedBlock), so this is a placement-routing
  issue that predates the change; parked.
  *(2026-10-21: the miss came from the plan's geometry: the target cell (4,-61,7) is the superflat grass layer,
  which 1.20.1 refused correctly, and the old helper never put its player on the ground, so FC's mid-air rule
  refused too. The real bug was wider: FC placement ignored validity, so a rerun on 2026-10-18 "placed" the hook
  inside the grass block. Fixed by (bd); P-PLACE H0 (the grass kept), H1 and H2 (hooks on a north and an east face)
  pass.)*
- Not run: any client (the bone-meal swing prediction, the hidden creative-tab entries and JEI's list),
  off-hand and creative bone meal (the helper has no flags for them), BonemealEvent callers from other mods,
  Serene Seasons, existing-world upgrades (sapling stages, generated legacy blocks), and modded templates.
- *(2026-10-18, production smoke.)* The release jar `BetterThanWolves-1.20.1-1.6.2-alpha.5.jar` (reobfuscated)
  on a clean Forge 1.20.1-47.3.0 dedicated server from the official installer, a fresh default world and 60 s of
  ticks, once with BTW alone and once with every optional mod of INTEGRATIONS.md #027-#032. `logs/debug.log`
  shows `Mixing fcext.FCBlockSaplingTwinMixin from betterthanwolves.mixins.json into
  net.minecraft.src.btw.block.FCBlockSapling`, the same line for `fcext.FCBlockMushroomTwinMixin` into
  `FCBlockMushroom`, and the same for compat wave C1's `fcext.FCBlockCropsDailyGrowthMixin` and
  `fcext.FCBlockHempCropMixin`. Both boots: 0 mixin errors, 0 crash reports, 20 TPS, and the self-audits as in
  the dev runs (`[SELF-AUDIT/twin] 6 twin families (11 blocks), 41 twin states, 173 paced states, 0 warnings`,
  `[SELF-AUDIT/tick] 238 blocks, 778 FC-random states, 200 FC-scheduled blocks, 0 warnings (0 expected)`,
  `[REMOVED-AUDIT] stays-removed table: 12 T1, 8 T2, 8 T3 rows; ... 0 warning(s)`); BTW alone logs 0 ERROR lines.
  `fcext.FCUtilsMiscColdBiomeMixin` logs no Mixing line in 60 s: it applies when FCUtilsMisc first loads.
  Run since, from the list above: Serene Seasons (proof H3, bone meal on an out-of-season tagged FC crop
  fertilizes the soil to block_1088), off-hand bone meal (on a Dynamic Trees branch, dt_p3 B2) and the
  BonemealEvent path of other mods (`BoneMealItem.applyBonemeal` on Dynamic Trees, dt_p3 B5 and B9), see (au).

**Caveats.**
- Dropped white dye (WD5): an item entity of 1.20.1 white dye on FC farmland fertilizes it and is used up.
  The item bridge hands FC the stack (351, 15), and FCBlockFarmlandBase.onEntityCollidedWithBlock checks
  only id and damage (FCBlockPlanterBase the same, by code reading). The fix is a bone-meal-only check in
  the item bridge before FC sees the stack. White dye used by hand does nothing.
  *(2026-10-20: fixed by (aw): white dye is FC 351:271, which no dye-15 rule matches; P-WD 40/40, the bone-meal
  plan 96/96.)*
- Stranded recipes: blast_furnace, smoker and furnace_minecart need the furnace (T3) and crossbow a tripwire
  hook (T1). They stay craftable from salvaged or existing items (a furnace mined with Silk Touch, as in
  4.B; by code reading); a BTW route is later compat work.
  *(2026-10-15: superseded by compat wave C1, maintainer decision D-B (a). RetiredIngredientWidening now gives
  every tier its ingredient successor: the furnace (T3) takes the brick oven that replaced it (FCRecipes.java:
  349-353), the tripwire hook (T1) one FC iron nugget, FC's own value for a hook (FCRecipes.java:4480-4485). The
  four recipes are widened and none is stranded in 1.20.1: the 2026-10-18 production smoke logs `Widened 17
  recipes` and no `Stranded` line. Every removed id stays removed as a result.)*
- A `/give`'d tripwire hook cannot be placed (above).
  *(2026-10-21: closed, see the note under the verification above and (bd).)*
- Bone meal keeps 1.20.1's behaviour on what BTW does not govern: moss, nylium, rooted dirt, pink petals,
  tall flowers, glow lichen, sea pickles, seagrass, dripleaf, mangrove leaves (a hanging propagule) and
  modded plants, and growWaterPlant on a block with no FC mapping. A later compat rule for modded plants on
  BTW soil adds its condition to BoneMealPolicy.route as a SOIL_RELAY and reuses onBonemeal; it must not add
  a second BonemealEvent listener.
  *(2026-10-18: done in compat waves C1 and C2, through that extension point and without a second listener.
  BoneMealPolicy.route(level, pos, state) makes a modded crop, sapling or #betterthanwolves:fc_soil_plants member
  on FC ground a SOIL_RELAY (INTEGRATIONS.md #026), and a modern block an integration registered FC machine
  hooks for an FC_TARGET (Dynamic Trees, #027). Modded plants on modern or modded soil keep 1.20.1 bone meal.
  BRIDGE_SPEC.md, "Bone meal (BoneMealPolicy)", has the route table.)*
- The early return for bone meal restores the 4.B order for bone meal only. Every other FC item still runs
  at HEAD of useItemOn, before block activation, RightClickBlock and the spectator check (BM8); generalising
  the early return is the 4.B rule but needs its own regression pass.
  *(2026-10-20: closed by (ba): no FC item runs at the head of useItemOn; the block is used first, then 1.20.1's
  item step runs the FC item.)*
- Paced plants reproduce FC's mean pace, not its cadence: FC's reed and cactus counters are near-regular,
  the thinning is geometric. Exact cadence would need a per-cell counter store that FC reads never see.
- Twin identity is visible to every FC world reader: FC animals graze the crop twins (4.B BlockCrops
  CanBeGrazedOn), FC weeds grow under them, FC saws and pistons harvest them. All of it is 4.B crop
  behaviour, and the drops are the live 1.20.1 loot.
- A modern sapling's FC stage lives in the overlay: an existing world's acacia sapling at 1.20.1 STAGE 1
  starts at FC stage 0 (S18), as oak saplings did in (as). A lone dark oak sapling waits at stage 3 for
  ever: 1.20.1 has no single dark oak.
- A pitcher crop with no room above stays at its FC level; FC's IncrementGrowthLevel may still tell the soil
  at level 7, using its fertilizer once.
- Generated T2 blocks stay legacy blocks: a village torch broken drops the nether-coal torch
  (FCBlockTorchLegacy.java:35-38), as in 4.B, and 1.20.1 villages have many (not checked at runtime).
- Removed blocks already in worlds are left alone and keep whatever behaviour they had; hand-harvest goes
  through FC, as in 4.B (a stub drops itself, the comparator drops nothing). Making them 4.B-inert (the
  hopper ticker, the GUIs) is not done: it would break existing contraptions.
- In the 1.20.1 data the result rule (pass 4) removed nothing beyond FC's records; it guards modded recipes
  and logs foreign removals at INFO.
- Supersedes the D2 and D4 caveats of 2026-10-14 (as) and narrows the CT6 caveat of 2026-10-11 (ap) (all
  annotated there).

## 2026-10-18 (au) Compat waves: FC machine hooks on modern blocks, modded saplings on the FC twins, FC's warm loose stone, BTW's mining penalty on modern blocks

**Symptom.** Four stubs and gaps of earlier entries, closed while BTW was opened to other mods: compat wave C1
(2026-10-15, committed as 3985147) and compat wave C2 (2026-10-17 and 2026-10-18, in the 2026-10-18 commits merged
as 0292e38, together with the hold-to-craft fix of (ao)). INTEGRATIONS.md #020-#032 has the integration entries;
BRIDGE_SPEC.md has the seams and data formats ("Recipe bridge", "Food values", "Modded plants", "Mod compatibility
(BTWCompat)", "Held use").
- **Mining penalty (C1).** A block with no FC block (deepslate, tuff, any modded block) dug at full speed while
  BTW's health, hunger, fat or gloom penalty slowed every FC block ((z) caveat).
- **FC machines on modern blocks (C2).** FC's saw, stump remover and bone meal did nothing to a Dynamic Trees
  trunk, stump or tree ((ag): the stand-in "is inert to the saw"). At 3985147 bone meal on a DT branch, rooty soil
  or trunk shell ran DT's own growth pulse instead, from either hand and at fertility 15 too (dt_p3 B1, B2, B8 fail
  at HEAD).
- **Saplings that are not SaplingBlocks (C2).** Dynamic Trees and dtphc2 saplings grew on their first random tick
  with no light, End or pace gate: in a sealed dark box a DT sapling grew at once, and DT saplings grew in the End
  (dt_p4 P1-P4 fail at HEAD). DT's Nether fungi never grew from random ticks at all (DT NetherFungusSpecies.java:63,
  gated at DynamicSaplingBlock.java:82), while vanilla crimson and warped fungi follow FC's mushroom rule ((at)).
- **FC warmth (C1 review, fixed in C2).** Loose cobblestone and loose stone brick with lava in their cracks were not
  warm to Cold Sweat or LSO: BtwThermalTable, new in C1, left out FC's own warm-block rule.

**Root cause.**
- Penalty: 1.5.2 multiplies every dig by GetMiningSpeedModifier (vanilla/server EntityPlayer.java:789-791). The
  port applies it to FC blocks inside FCMiningSpeed.compute only. Other blocks dig through 1.20.1
  Player.getDigSpeed, and FCGameplayBridge.onBreakSpeed read m_fCurrentMiningSpeedModifier, which is 1.0 outside
  compute, on the server only.
- FC machines: ModernBlockStandIn answered OnBlockSawed, GetDoesStumpRemoverWorkOnBlock and
  AttemptToApplyFertilizerTo with Modern-Common's inert false. C1 added the ModernBlockHooks seam; no integration
  registered a hook before C2. Found by the C2 after-runs (and present at 3985147): ModernBlockStandIn.idFor asked
  only MC's cached collision shape, which MC builds at BlockPos.ZERO in an empty world. A DT trunk shell
  (TrunkShellBlock, whose shape follows its core, without a dynamic shape) caches empty, so it read as air to FC and
  the saw and stump remover never reached it.
- Saplings: TwinRegistry.isFallbackSapling (C1, decision D2 (b)) accepted only an unmapped SaplingBlock with STAGE.
  DynamicSaplingBlock extends Block and has no STAGE, so TickPolicy.decide never classified it.
- Warmth: BtwThermalTable rows are contiguous metadata ranges. FC's rule is FCBlockLavaReceiver.GetIsBlockWarm =
  GetHasLavaInCracks, `(meta & 1) != 0` (Server FCBlockLavaReceiver.java:99-102, :106-109): every odd metadata,
  which no single range can hold.

**Fix (general rules).**
- **Mining penalty (C1, INTEGRATIONS.md #022).** `FCMiningSpeed.miningModifier(Player)` gives BTW's modifier on
  either side through the one formula `EntityPlayer.computeHealthAndExhaustionModifier` (server: the bridge's own
  levels; client: the PenaltySync mirror; 1.0 on any failure). `FCGameplayBridge.onBreakSpeed` multiplies
  BreakSpeed's `newSpeed` by it for every block with no FC block, on both sides, so the client predicts the
  server's progress. FC blocks keep it inside `compute`, and FC digging now posts BreakSpeed itself
  (`FCMiningSpeed.postBreakSpeed`, FC's strength as the original speed), so the penalty applies exactly once and
  other mods' factors reach FC blocks too.
- **FC machine hooks (C1 seam, live since C2; #022, #027).** `ModernBlockHooks.register(Predicate<BlockState>,
  Hook)`, called during mod construction. The stand-in's 7-argument `OnBlockSawed` (FCBlockSaw),
  `GetDoesStumpRemoverWorkOnBlock` (FCItemStumpRemover) and `AttemptToApplyFertilizerTo` (FCItemDye) ask the first
  hook whose predicate accepts the live state, on a ServerLevel; with none, on the client, or when the hook throws,
  FC's inert false stands. `BoneMealPolicy.route` answers FC_TARGET for a hooked state that no earlier row claims
  (DT's and dtphc2's pods and fruit are tagged paced plants and route SOIL_RELAY). Dynamic Trees registers
  `DTBlockHooks` for branches (roots included), rooty soil, trunk shells and DT pods and fruit (dtphc2's
  included):
  - the saw fells a trunk of radius 8 or more away from the saw; the cut block gives FC's log saw output (4 planks
    of the wood, 2 sawdust, 1 bark when unstripped; FCBlockSaw.java:618-639) and the falling payload is one log
    short; a species FC never had gives 2 sawdust and keeps its log in the payload;
  - the stump remover clears a dead DT stump, from its core or any trunk shell; a living stump is refused (air
    there would collapse the whole tree with no drops);
  - bone meal gives the rooty soil +2 fertility and nothing at 15; pods and fruit take none, as FC cocoa.
  `ModernBlockStandIn.idFor` asks the live collision shape at the position when MC's cached one is empty for a
  block without a dynamic shape (FCBakedModel's LiveAccess reads it the same way in the read-only region), so a
  trunk shell reads as stand-in 161.
- **Modded saplings (C2, #027).** `TwinRegistry.ModdedSapling` (`isSapling`, `isFungus`, `growTree`) and
  `registerModdedSapling`, called during mod construction. `isFallbackSapling` also accepts a registered
  non-SaplingBlock sapling, and a registered Nether fungus takes the FUNGUS twin. FC's rule decides (the sapling
  twin: survival, light 9 above, four stages at 1/64, never in the End; the fungus twin: 1 in 50 random ticks,
  never in the End), then the kind's `growTree` grows the mod's own tree. `DTSaplingTwin` grows DT's tree through
  `Species.canSaplingGrow` and `transitionToTree` only, honours a tree pack's `canSaplingGrowNaturally` false for
  saplings (fungi skip that gate: their natural growth is FC's roll), asks SeasonGate by the species' seed item, and
  drops the seed of a sapling that cannot stay.
- **FC warmth (C2, #030, #031).** `HeatTier.LAVA_CRACKS` and one heat row per odd metadata of
  `fcBlockCobblestoneLoose` and `fcBlockStoneBrickLoose`, so `heatSources()` has 29 rows over 14 blocks. Cold Sweat
  maps LAVA_CRACKS to its own magma block (20 F, range 3, max 48 F), LSO to 12.0.
- Unchanged: FC code.

**Verification.** Dev dedicated server over RCON with the (as) harness and `-PbtwDevMods` tokens, C2 after-runs of
2026-10-17 and 2026-10-18 (scratchpad compat/refresh2/BASELINE_RUNS_C2.md, Parts 2 and 3); every listed run passes
all its checks.
- Mining penalty: by code reading only. The C1 harness rows (harvest-tools_p0p1, section E) cannot show it: the
  proof FakePlayer never ticks, so FC's penalty levels stay 0 and every dig row printed `mod=1.0`, starving or fed.
  What the runs do show is the BreakSpeed post on FC blocks: one post per dig query (`bs=1`), and a test listener
  that halves BreakSpeed halves FC stone's progress with an iron pickaxe, 0.08888889 to 0.044444446 per tick (C1
  after-run ht_p3_cs_as).
- FC machine hooks, Dynamic Trees 1.4.10: dt_p2 (saw and stump remover) 26 of 26 (18 GAP, 8 CONTROL) after the
  live-shape fix, with `[STAND-IN] dynamictrees:trunk_shell reads as legacy id 161 to FC`. The saw (the proof
  helper calls FCBlockSaw.SawBlockToFront once, as the powered saw's tick does) fells a radius-8 oak trunk into 4
  oak planks, 2 sawdust and 1 oak bark, with the falling logs one short; a sawed trunk shell fells its core; the
  stump remover on a shell of the dead radius-12 stump left behind removes the core, the clicked shell and a
  diagonal shell, with no logs; a dark oak trunk gives 2 sawdust, no FC bark, and keeps its log. dt_p3 (bone
  meal) 14 of 14 (8 GAP, 6 CONTROL): at fertility 15 nothing is used from either hand or through
  `BoneMealItem.applyBonemeal`; at 13 one bone meal is used for +2; bone meal never grows a DT sapling.
- Modded saplings and fungi: dt_p4 19 of 19 (10 GAP, 9 CONTROL). 3 of 100 DT oak saplings grew in 64 random
  ticks (FC's pace allows at most 7) and the other 97 later; none grew at night or in the End; a sapling in a
  sealed dark box pops and drops its seed; SeasonGate DENY holds them, DESTROY removes them without a seed, and a
  provider that throws is dropped (ALLOW). 16 of 16 DT crimson fungi became DT trees by 600 random ticks (none at
  HEAD), none in the End. Logs: `[TWIN] modded sapling kind registered: Dynamic Trees saplings (#027)`,
  `[STAND-IN] FC machine hook registered: Dynamic Trees (#027)`, `[MODDED-PLANTS] ... modded-saplings-on-FC-rule=19`
  with Dynamic Trees alone. dt_p8 (Dynamic Trees, HarvestCraft, dtphc2) 12 of 12: HarvestCraft apple saplings
  keep FC's pace and DT saplings run the same rule.
- FC warmth: coldsweat_p1 55 of 55: lava-cracked loose cobblestone (meta 1) and loose stone brick (meta 3) give
  20 F (max 48, range 3), the same blocks without lava nothing; Cold Sweat logs `Loaded 19 block temperatures`.
  lso_p1 86 of 86: 12.0 for both blocks with lava, none without. `[SELF-AUDIT/thermal] 29 heat-source rows (14
  blocks), 20 clothing, 4 textiles, 3 meals, 39 fuels (nethercoal betterthanwolves:item_481 = 3200); 0 warning(s)`.
- Production smoke (2026-10-18, see (at), Verification): the release jar boots with every optional mod, 0 mixin
  errors, and logs `[MODDED-PLANTS] crops=100 saplings=0 modded-saplings-on-FC-rule=114 fc_soil_plants=6 plant
  stand-in=162 base=0.1`.

**Caveats.**
- The mining penalty on modern blocks has no runtime proof yet: a TickProof step that sets the bridge's penalty
  levels directly is needed (a C1 review note).
- A real powered saw on a DT trunk has not been run; the proof calls the saw's step directly.
- Declared Dynamic Trees deviations (INTEGRATIONS.md #027): the stump remover and the work-stump chisel act only on
  dead DT stumps; thick trunks (radius 9-24) chew one radius per hit; DT growth may thicken a chewed trunk again;
  direct `performBonemeal` callers on branches and rooty soil keep DT's effect.
- FC's sapling survival rule pops DT saplings with no sky view and light below 8, including seeds DT plants under
  dense canopies; FC behaviour, applied by lore.
- The related hold-to-craft bridge of 2026-10-18 is in (ao)'s caveats and in BRIDGE_SPEC.md, "Held use
  (hold-to-craft)".

## 2026-10-19 (av) Dev runs load a filtered copy of Modern-Common; FC bytecode passes run inside extractFcClasses

Entries (av) to (bf) (2026-10-19 to 2026-10-21) are the post-C2 follow-ups and the CurseForge fixes of
docs/curseforge-feedback-triage-2026-10-19.md, made in the working tree on top of 0292e38 and not committed. Each
gap was first recorded by a scripted in-world plan on the old code, then each stage's gate passed and every
regression plan read SAME; the proof record, with every count quoted below, is the scratchpad's
fu_runs/BASELINE_RUNS_FU.md. Every stage built with exit 0, LinkAudit kept the 6 pre-existing missing members and
BridgeAudit reported 0 high-severity findings. Not run for any of them: the release jar (no production smoke), a
launcher-installed client, multiplayer.

**Symptom.** Two build hazards of the dev runs (build tooling only: no gameplay change, no INTEGRATIONS entry):
- **A build rewrote class directories a running server loads.** On 2026-10-15 a runClient started beside a running
  runServer re-ran `:Modern-Common:compileJava` and removeModernCommonShadowedClasses, and the server died with
  `NoClassDefFoundError: btw/modern/DataWatcher`, a class that exists only in Modern-Common's output. On the fc
  side, openUpEntityAccess rewrote 71 classes on every build and extractFcClasses re-synced the whole directory
  every second build.
- **A stripped Modern-Common output was packaged and compiled against.** `:Modern-Common:jar --rerun` without
  compileJava gave a jar with 0 `btw/modern/EntityLiving.class`, and `:Forge:compileJava` then failed (exit 1, 555
  errors, the first `PlayerBridge.java:19: error: cannot access EntityLiving`).
- The proof harness worked around both with a prebuild before every server run and an 11-flag `-x` list for client
  runs.

**Root cause.** The dev-only task removeModernCommonShadowedClasses deleted the shadow-listed classes from
Modern-Common's own compile output (Modern-Common/build/classes/java/main) and declared no output. That directory
has four consumers (Modern-Common's jar, Forge's compile classpath, the dist jar and the dev runs' mods{} source),
and Gradle assumes only Modern-Common:compileJava writes it, so a later build either rewrote it under a live server
or packaged the stripped copy. The fc directory had the same flaw: four ASM tasks (widenFcHookDescriptors,
extendFcPathingBuildRange, extendFcTreeBuildRange, openUpEntityAccess) patched extractFcClasses' output in place.
The rule both broke: every class directory a dev run loads must be the declared output of exactly one task, and
nothing else may write it.

**Fix (general rules).** Forge/build.gradle, tag `dev-run`:
- New source set `devModernCommon` with no sources (Forge/build.gradle:44-65), the dev view of Modern-Common. All
  three runs list it last, after fc, in place of Modern-Common's own output (:111, :133, :159).
- One shadow list, the copySpec `modernCommonRuntime` (:775; 67 exclude lines, 65 of them naming classes
  Modern-Common has), used by the dist jar (`with modernCommonRuntime`, :1643) and by the new Sync task
  `syncDevModernCommon` (:884-897). That task fills Forge/build/classes/java/devModernCommon, fails the build
  unless ContainerChest is present and Entity absent, then logs `Dev class precedence OK (ContainerChest =
  Modern-Common, Entity = fc)`. Dev and jar bind every btw.modern collision from the same spec, so they cannot
  drift. Modern-Common's own output is never modified, and removeModernCommonShadowedClasses is removed.
- A configuration-time mods{} order guard (:907-920) fails the build if a run lacks fc, lists devModernCommon
  before fc, or lists Modern-Common's own output. Every run and prepareRun task depends on syncDevModernCommon
  (:1589-1595).
- The four ASM passes are closures with the same names, run as `extractFcClasses.doLast` steps in the order widen,
  pathing, tree, openUp (:1575-1587), so build/classes/java/fc has one writer and extractFcClasses stays
  UP-TO-DATE. The pass bodies are unchanged.
- tools/LinkAudit.java prints the one shadow list and gains a DEV FILTER CHECK (:25, :52, :206-215): the dev view's
  leaked, missing, stale and extra classes against Modern-Common's output, or "skipped: run
  :Forge:syncDevModernCommon" when the dev view is absent.
- Three comment-only Java edits keep each file's line count, so their classes compile unchanged (FCPuppetSupport,
  Modern-Common FCEntityLightningBolt and RenderEntityAccess).
- Unchanged: FC code; the jar's content.

**Verification.** Record, stages head and A (2026-10-19):
- GATE-A PASS: the jar keeps 6354 entries and only META-INF/MANIFEST.MF changed; fc 914 files identical; the dev
  view holds 512 files, each equal to Modern-Common's, with the 65 shadowed classes omitted. javap spot checks 0 2
  0 9 1, as at HEAD. The build logs 4 guard lines, `Dev class precedence OK` among them.
- LinkAudit: DEV FILTER CHECK four empty lists; 80 collisions, drift 0; stale-jar 3 and missing members 6, both as
  at HEAD.
- P-DEV-A 15/15 in both after-runs (HEAD 11/15); the second shows Modern-Common:compileJava, extractFcClasses and
  syncDevModernCommon UP-TO-DATE, 0 rewrite lines and the prebuild skipped. P-DEV-B, a client run with no `-x`
  list, 10/10 (HEAD 8/10), all six tasks UP-TO-DATE. The stripped-output symptom is gone: the Modern-Common jar
  keeps EntityLiving and `:Forge:compileJava` succeeds.
- P-BM 96/96, P-RM 73/73 and P-DT1 39/39 SAME. The record's final sweep (2026-10-21) ran P-DEV-A 15/15 again on the
  final tree, after every later build change.

**Caveats.**
- `Dev class precedence OK` prints only when syncDevModernCommon runs: a plain `:Forge:build` finds it UP-TO-DATE.
- removeModernCommonShadowedClasses is gone, and the four passes are no longer tasks (their names live on as
  passes): a static `-x` list that names any of them fails with "Task not found". Wire to extractFcClasses or
  syncDevModernCommon instead.
- Any edit to Forge/build.gradle runs syncDevModernCommon and extractFcClasses once.
- A Modern-Common output directory stripped by an older build needs a one-time `--rerun` recovery; it was needed
  once, at landing.
- LinkAudit's stale-jar 3 (ContainerEnchantment, EntityAIBase, IEntitySelector) predates this change.
- The older text that names the deletion task (:718, :774 and (w)'s jar{} bullet) stays as history.

## 2026-10-20 (aw) White dye is not bone meal; ItemDye subtypes; the 4.B merchant slot rule

**Symptom.** 1.20.1 white dye was FC bone meal (351:15) wherever FC saw it: dropped on FC farmland or a planter it
fertilized the soil and was used up ((at), caveat WD5), and it served as chicken feed, dog food and the farmer's
bone-meal trade. FC grids gave bone meal for the 1.20.1 `white_dye` and `white_dye_from_lily_of_the_valley`
recipes. Separately, an ink sac (dye 0) merged into an FC slot of any other dye and took its colour, and FC trades
accepted any subtype of a subtyped item. HEAD: P-WD 16/40 (GAP 0/24).

**Root cause.**
- LegacyVariants spelled white dye as (351, 15) in both directions, the same FC stack as bone meal, so every FC
  rule for dye 15 applied to it.
- Modern-Common ItemDye had no subtypes, where 1.5.2 ItemDye declares subtypes and no durability (vanilla/server
  ItemDye.java:23-29). FC's merge rule (FCUtilsInventory.CanStacksMerge:405-413) compares damage only for subtyped
  items.
- Modern-Common MerchantRecipeList compared item ids only, where 4.B also compares the damage of subtyped items
  (vanilla/server MerchantRecipeList.java:138-207).

**Fix (general rules).** Tag `bone_meal`, #017's tag; INTEGRATIONS.md #017 and #025 describe it:
- **A modern spelling is its own FC stack.** `LegacyVariants.MODERN_SPELLING = 0x100` (LegacyVariants.java:61) and
  `spelling(id, meta, item)` (:110) map a 1.20.1 item to (id, meta | 0x100) both ways, so the 4.B item keeps (id,
  meta) on both sides. Items only; a positive short, never 32767. White dye is FC 351:271 (:226). FC's colour code
  masks the damage (`~d & 15`), so FC sheep, wolf collars and windmills still read it as white, while no dye-15
  rule (fertilizer, feed, the trade) matches it.
- `baseSubtype` (:272) strips the bit. RecipeBridge builds its legacy aliases from the base subtype (`legacyAlias`
  :289, `buildLegacyAliases` :299-308), so the alias stays bone_meal -> white_dye, one way, and FC grids give real
  white dye.
- Modern-Common ItemDye: `setHasSubtypes(true); setMaxDamage(0)`, as in 1.5.2 (ItemDye.java:5-17).
- Modern-Common MerchantRecipeList: both slot tests go through `validFirst` and `validSecond` (:34, :41), which
  compare damage for subtyped items. 4.B's index strictness is not ported, because the port's trade screen selects
  a trade only on a click (client/FCContainerScreen.java:314-333).
- ItemDye and MerchantRecipeList are now at 1.5.2/4.B parity. Side effects: the brown wool knit (Server
  FCEntityVillager.java:642) and blood wood sapling (:781) trades are 4.B-strict; the log trades (:638-641) stay
  lenient until the setHasSubtypes audit.
  *(2026-10-28: the log trades are 4.B-strict since (bk), which gives the block items, ItemMultiTextureTile and the
  skull item 1.5.2's subtype flag.)*
- Comments only: WorkbenchFallback. Unchanged: FC code; BoneMealPolicy (white dye is not `isFcBoneMeal`, and
  FCItemDye acts only at damage 15).

**Verification.** Record, stage B (2026-10-20): P-WD 40/40, GATE after OK (HEAD 16/40); the bone-meal plan P-BM 94
-> 96/96 (WD5's two checks flip), every other row SAME.

**Caveats.**
- White dye saved in FC storage before the update was stored as damage 15 and reads back as bone meal; there is no
  migration. An older jar reads damage 271 as item 351 with {Damage:271}.
- Modern-Common ItemStack.isItemDamaged was `damage > 0`, where 1.5.2 counts damage only on an item with
  durability; known here and narrowed by (bc) to items without the subtype flag.
- The setHasSubtypes audit of the other Modern-Common items stays queued.
  *(2026-10-28: the block items (the plain ItemBlock ids and ItemMultiTextureTile) and the skull item declare subtypes
  since (bk); still queued as F-SUBTYPES: ItemBlockWithMetadata (stone, cobblestone, ores, snow), coal, spawn eggs,
  potions, filled maps, FCItemBlockDeadWeight and BTW's anvil item.)*

## 2026-10-20 (ax) Leaves drop by FC's leaf rule on every loot path

**Symptom.**
- FC's own leaves (id 18, the four 1.5.2 species) dropped an oak leaves block (18:0), never a sapling, on FC's
  harvest, decay, machine and block-dispenser paths; shears and silk touch did not give the leaves; blood wood
  leaves dropped their sapling on every break.
- The newer leaves (acacia, dark oak, mangrove, cherry, azalea, flowering azalea) and Dynamic Trees leaves have no
  FC identity and kept their 1.20.1 and DT tables: Fortune-scaled saplings plus sticks, dark oak apples, mangrove
  leaves with sticks and no propagule, DT seeds at 1/64 plus sticks. 1.20.1 explosions rolled the vanilla table for
  FC's own leaves too. #027's decision D2 (DT leaf loot) was open.
- HEAD: P-LEAF-V 12/32, P-LEAF-DT 2/10.

**Root cause.**
- Modern-Common BlockLeaves had none of 1.5.2's drop methods (vanilla/server BlockLeaves.java:184-287), so
  FCBlockLeaves and FCBlockBloodWoodLeaves inherited Block's defaults: the block itself at meta 0, one per break.
- The 4.B rule lives in FCBlockLeaves, and only legacy id 18 reaches it. Every other leaves block drops through a
  loot table, and nothing in the port rewrote leaf loot.

**Fix (general rules).** INTEGRATIONS.md #035, tag `leaves_drop_like_fc`:
- **Base ports** (Modern-Common BlockLeaves.java:43-106): quantityDropped 1 in 20, idDropped the sapling,
  damageDropped `meta & 3`, harvestBlock (shears, item 359, take the species' leaves), createStackedBlock (the
  silk-touch drop and the block dispenser's take), and an interim `canSilkHarvest()` true (decision D6(b)): 1.5.2's
  general rule (renders as a normal block and is not a container) is true for leaves, while Modern-Common Block
  answers false for every block. The Modern-Common drop-parity follow-up (D7, 66 candidates) restores the general
  rule and then removes this override. dropBlockAsItemWithChance is not ported: both FC subclasses replace it.
- **One rule for every leaves block on every loot path:** `LeafDrops` (LeafDrops.java:67; `Resolution` :87, `KEEP`
  :90, `Hook` :96, `register` :115, `apply` :121; logger `BTW-Leaves`), applied by one condition-free global loot
  modifier, `betterthanwolves:fc_leaves` shipped as `leaves_drop_like_fc` (data/FCLeavesLootModifier.java:27,
  BTWLootModifiers.java:40-42, FCLootOverrides.java:104-107). On a `#minecraft:leaves` block's own table (never
  `forge:unknown_loot_table`) a registered hook decides first; legacy id 18 takes FC's idDropped and damageDropped;
  any other leaves block takes its sapling by name (same namespace, `<stem>_sapling`, then `<stem>`, then the only
  `<stem>_*`). Shears and silk-touch loot is kept as is. Otherwise saplings, `#forge:rods/wooden`, the hook's
  byproducts and (minecraft leaves only) apples are stripped, and the sapling is rolled at 1 in 20 (1 in 40 for
  wood type 3, jungle); Fortune and the explosion radius are never read. The cache clears on the server tag load.
- FC's own paths (harvest, decay, machines, block dispenser) run FCBlockLeaves on the base ports; the table paths
  (1.20.1 breaks and decay of the newer leaves, every 1.20.1 explosion, stand-in machine drops, foreign
  destroyBlock) go through the modifier. Neither path rolls the other, so nothing drops twice.
- **Dynamic Trees** (#027's D2 settled): `DynamicTreesCompat.init` registers the DT hook first, with Minecraft
  types only (DynamicTreesCompat.java:120-124, `LeafHook` :156), then probes `DTLeafDrops`: the roll's species
  (orphan leaves take the family's common species), the seed only when `shouldDropSeeds`, the wood type from
  `FCLogRules.fcWoodMeta` of the family's primitive log, DT's seasonal seed chance as a veto, the family stick as a
  byproduct; a throw keeps DT's loot and switches #027 off with one ERROR (DTLeafDrops.java:92-94,
  `DynamicTreesCompat.fail`).
- FCImmatureCropLootModifier's ordering claim is corrected: GlobalLootModifierProvider writes the modifier list in
  hash order (FCImmatureCropLootModifier.java:41-43), and neither modifier needs a place in it.
- runData is required: without `leaves_drop_like_fc.json` and its `global_loot_modifiers.json` entry the modifier
  never loads.
- Unchanged: FC code.

**Verification.** Record, stage B (2026-10-20): P-LEAF-V 32/32 (HEAD 12/32; F6, the blood wood sapling, 100 in 2000
breaks) and P-LEAF-DT 10/10 (HEAD 2/10), both GATE after OK, and the same again in stage BDD. Logs: `[LEAVES] leaf
drop hook registered: Dynamic Trees leaves (#027)`; no `[LEAVES] minecraft:` or `[LEAVES] dynamictrees:` line, no
loot-modifier decode error. runData wrote only `leaves_drop_like_fc.json` and its list entry.
- Harness gap found by the first run (31/32): blood wood saplings spawn as FCEntityItemBloodWoodSapling, a plain
  proxy (BTWEntityRegistration.java:177), and the drop capture counted only ItemEntity, so F6 read 0; it read 0 at
  HEAD too, where the row was blind. TickProof gained `capturedStack(Entity)` (an ItemEntity's stack, or the FC
  stack of a proxy wrapping an FC EntityItem), and the rerun gave the result above.

**Caveats.**
- `canSilkHarvest()` true is interim, until the drop-parity follow-up.
- An FC identity other than 18 in `#minecraft:leaves` is left to the general getDrops bridge with one INFO line;
  none exists today.
- FC's mining charge is FC's own drop path, which stays a stub for leaves until CF2-DR5.
  *(2026-10-28: closed by (bn): the base DropItemsOnDestroyedByExplosion has 1.5.2's body, and FC's own leaves (18)
  take FCBlockLeaves' roll on every blast; the loot modifier still serves leaves without an FC blast rule.)*

## 2026-10-20 (ay) Acacia, dark oak, mangrove and cherry as FC wood types 5-8; the log rule for stems and modded logs

**Symptom (maintainer rulings of 2026-10-19: the four woods become real FC wood types through runtime patches, with
their own bark, tanning, work stump, chewed log and saw output, never mapped onto oak, spruce, birch or jungle
(ruling 1); crimson and warped stems take the log rule with no bark and no work stump (ruling 2)).**
- Acacia, dark oak, mangrove and cherry logs had no FC identity (`getBlockId` 0; FC's world reads saw a stand-in):
  a bare hand took whole logs; no bark, tanning, work stump, saw planks, chopping or FC fuel; 1.20.1's 0.005
  harvest hunger instead of FC's 0.025; and the vanilla log -> 4 planks recipe stayed (FC removes it for species
  0-3 only). Triage CF2-PR-WOOD; #022's known limitation.
- Crimson and warped stems and modded logs had no log rule at all.
- FCBlockPlanks' client `getIcon` indexes `m_iconArray[meta]`, which overflows for meta 5 and above (Client
  FCBlockPlanks.java:126-129).
- HEAD: wood_p1 14/67, mlog_p1 18/73, dt_wood 2/4, dt_p2 24/27 (the three new S4 rows fail).

**Root cause.** FC keys every wood rule on log id 17 and `meta & 3`, or on wood types 0-4: FCBlockLog's chew,
bad-break bark, stump test and fuel; the work stump's species, side icons and block-dispenser stack; FCItemBark's
five subtypes; FCBlockPlanks' fuel and icons; the saw's log case; log chopping and FCItemBlockLog.OnUsedInCrafting;
the 4.B log -> planks removals. The four woods had no place in that space, and nothing gave stems and modded logs
FC's log rule.

**Fix (general rules).** INTEGRATIONS.md #034, tags `modern_woods` and `fc_log_rules`; BRIDGE_SPEC.md has the API.
- **Log2.** `ModernWoods` builds a plain, unmodified FCBlockLog at the fixed legacy id 165 (`LOG2_ID`,
  ModernWoods.java:60) with FC's FCItemBlockLog, at BTWLifecycle step 5e (BTWLifecycle.java:164-169, after the
  twins): wood types 5 acacia, 6 dark oak, 7 mangrove, 8 cherry; log2 species = wood type - 5, in FC's own log
  layout (bits 0-1 species, 2-3 axis, 12 stump). The four logs and their wood and stripped forms read as 165, so
  they go through FC's own pipeline as oak does: chewing, bark 5-8, the work stump, the 0.025 harvest hunger, the
  kiln, FC fire and drops. `create` (:193) logs one ERROR and builds nothing if 165 is taken; `addRecipes` (:260)
  adds 8 cauldron tanning recipes and 4 RemoveVanillaRecipe records through FC's own API, so FcRecipeRemovals
  removes the four log -> 4 planks recipes; `audit` (:328) logs `[SELF-AUDIT/wood]` (BridgeSelfAudit.java:45).
  Tannin per leather by lore: acacia 2, dark oak 5, mangrove 2, cherry 8 (`BARK_PER_LEATHER` :91); planks fuel 400,
  400, 200 and 500, logs x4 (`PLANKS_BURN_TIME` :97).
- **Identity.** ProxyRegistry maps the four logs to 165, but `getBlockId` (:243-250) answers 165 only when
  `ModernWoods.isBuilt()`, so a foreign block at 165 never receives them. `LOG_FORMS` (:285; Block ->
  `LogForm(logId, species)` :271) holds the wood and stripped forms; `isLogForm` :315, `getTreeLogId` :325,
  `getTreeLogSpecies` :338 (now public), `getWoodType` :351 (0-3, 5-8 or -1). The planks variants take mask 15,
  with 5-8 for the four planks. LegacyVariants rows (LegacyVariants.java:138-144, wiring W-B1) round-trip the log
  and planks items.
- **Nine load-time patches** M1-M9 (`@Pseudo`, `remap = false`, `btw.forge.mixin.fcext`), keyed on id 165 or wood
  type 5-8, so FC's id 17 is untouched: FCBlockLogWoodMixin (M1: the work stump's wood type, bark, bad-break bark,
  the stump test, fuel, the client log icons); FCBlockWorkStumpWoodMixin (M2: the bark of a chewed work stump, the
  block dispenser's take for META 5-8, side icons); FCItemBarkWoodMixin (M3: names, icons, the creative list);
  FCItemBlockLogWoodMixin (M4: the stick branch's bark); FCRecipesLogChoppingWoodMixin (M5: log2 chops into its own
  planks); FCBlockSawWoodMixin (M6: FC's log case with the wood type); FCBlockPlanksWoodMixin (M7: fuel for 5-8;
  `getIcon` gives the 1.20.1 planks for 5-8 and icon 0 for any other meta of 5 or more, which closes FC's array
  overflow); FCBlockLogDamagedWoodMixin (M8: a chewed log connects to 165); FCItemChiselIronWoodMixin (M9: the
  chisel charge of 10 for a log2 work stump).
- **Textures and names.** The bark 5-8 icons are item models on `minecraft:block/<x>_log`, so they follow resource
  packs. The work-stump sides are derived, not drawn: `deriveModernWoodTextures`
  (Forge/gradle/modern-woods.gradle:22, input override `-PbtwClientExtraJar=<path>`) lays FC's work-stump overlay
  of the nearest FC species (acacia and mangrove: jungle; dark oak: oak; cherry: birch) on the 1.20.1 log side. The
  four PNGs are checked in, and the task rewrites one only when its pixels and bytes differ. Names are in
  `lang/BTW_modern_en_US.lang`, loaded third (BTWLifecycle.java:261).
- **The log rule.** FCLogRules: `fcWoodMeta` (:166) answers `getWoodType` (contract C1: 0-3, 5-8 or -1, never
  log2-local bits); `bark` (:186) takes 0-8; `workStumpState` (:219) is unmasked; `chargeLogHarvest` (:241) takes a
  wood type; `isLogRuleTarget` (:319); `isStump` (:331, contract C2) is the one stump rule, a vertical log on a
  solid block that is not a log, so an oak log on a stem or a DT branch is no longer a stump; `sawPlanks` (:451);
  the bark tags `betterthanwolves:bark/<wood>` and `bark/none` (:92-96, generated by data/LogBarkTagProvider.java).
  - Class F, the barked and stripped forms FC reads as 17 or 165: FC's dig rate and FC's own ConvertBlock chew; the
    harvest drop stays vanilla.
  - Class S, stems, hyphae and modded logs no earlier integration owns: the axe gate, an immediate chewed log with
    bark (decision 3(b)), the work stump, and FC's saw and stump remover through `ModernLogHooks`
    (ModernLogHooks.java:33, registered after Dynamic Trees, BTWForgeMod.java:59-60, so DT's hook wins for DT
    blocks). Stems (`bark/none`) give no bark and leave no work stump. Saw planks come from the log's
    one-ingredient planks recipe; with none, the log drops as itself.
  - `ModernLogRules`: BreakSpeed, BreakEvent and BlockToolModificationEvent at LOWEST (ModernLogRules.java:99,
    :146, :254); an axe strip's bark is settled at ServerTickEvent END (:288-289); the caches clear on TagsUpdated,
    OnDatapackSync and ServerStopped (:316-328).
  - WorldBridge's log derive uses `isStump` (WorldBridge.java:356-370, wiring W-B2).
  - Dynamic Trees (#027): DTTrees, DTBlockHooks and DTHarvestHandler use `fcWoodMeta`, so DT families of the four
    woods get their own bark, work stump and saw planks; DT's fungi are chewed, with no bark and no work stump.
- **Decisions taken at landing (WOOD-B D1-D9):** D1, a class-S log with no planks recipe saws into itself only
  (no sawdust or bark loop); D2, every log-rule chew sends break effect 2001 to the other players, as FC's
  pipeline does; D3, class-F convert wear hands FC the original 17 or 165, so M9's chisel charge applies; D4,
  `chargeLogHarvest` maps a wood type to (17, wt & 3) or (165, wt - 5), falling back to 17; D5,
  `bark/oak|spruce|birch|jungle` = `#minecraft:<wood>_logs` (FC's own wood type is checked first); D6, a DT
  family with no primitive log pays oak bark; D7, DT helper failures are logged once and swallowed instead of
  switching #027 off; D8, a class-S work stump is set with UPDATE_ALL and plays no shaft-ripped sound, as #027
  does; D9, the log hook makes `BoneMealPolicy.route` answer FC_TARGET for class-S logs, a no-op for vanilla
  logs.
- **Wiring at landing:** W-B1 the LegacyVariants rows; W-B2 the WorldBridge log derive; W-B3 the nine mixins.json
  entries; W-B4 `ModernWoods.audit()` in BridgeSelfAudit; W-B5 `apply from: 'gradle/modern-woods.gradle'`; W-B6 the
  texture task, runData and the build.
- Unchanged: FC code. FCBlockLog, FCBlockWorkStump, FCItemBark and FCBlockPlanks run unmodified on log2.

**Verification.** Record, stage B (2026-10-20):
- Build: Jar bundling OK (1840 classes), Mixin config OK (50 entries). `deriveModernWoodTextures: 4 work-stump
  textures checked (0 written)`: the checked-in PNGs are reproduced byte for byte. runData wrote the 10 bark tags
  (`none` = crimson and warped stems; oak, spruce, birch and jungle = `#minecraft:<wood>_logs`; the others empty).
- wood_p1 67/67 (HEAD 14/67), mlog_p1 73/73 (HEAD 18/73), dt_wood 4/4, dt_p2 27/27 (the three new S4 rows), dt_off
  2/2; dt_p1 39/39 and dt_p5 9/9 SAME.
- Logs: `[WOOD] log2 FCBlockLog at 165: acacia, dark oak, mangrove and cherry are FC wood types 5-8`, `[WOOD]
  tanning with bark 5-8 ([2, 5, 2, 8] bark per leather) and the 4 vanilla log -> planks removals added`,
  `[STAND-IN] FC machine hook registered: the log rule (stems and modded logs, fc_log_rules)`, `[SELF-AUDIT/wood]
  ... at 165: 4 woods (wood types 5-8), 0 warnings (0 expected)`; no `[WOOD]` ERROR and no mixin apply failure.
  Every stand-in id allocation comes after the `[WOOD]` line.
- Clients: wood_c1 shows the sprites `fcblockworkstumpacacia` and `fcblockworkstumpcherry`, bark drawn from
  `minecraft:block/acacia_log` and `cherry_log`, and dig progress 0.004 (HEAD: the spruce and oak stump sprites,
  blood wood bark, 0.016666668); mlog_c1's client dig progress equals the server's for every cell. Both read the
  same in the final sweep.
- The wave-C hook scan (W-C0) finds the four logs at 165 with OAK_LOG's hooks. Since (bd), FC-placed logs on a
  north face get axis z.

**Caveats** (#034's known limitations):
- Legacy id 165 must never change: FC stack NBT stores numeric ids. If 165 is taken at start-up, the four woods
  stay on the class S rule.
- The flammable stand-in pool shrinks by one: three ids (162-164), two with the plant stand-in.
  ModernBlockStandIn's comments (ModernBlockStandIn.java:56, :213) still count 165 as free; a code-comment
  follow-up.
- FC's own work stumps (META 0-3) still hit the block-dispenser stub (:895, the P2 "Block dispenser" row), while
  META 5-8 are retrievable through M2. Blood planks 5:4 still become oak planks.
- Old headless acacia stacks do not stack with new (165, s) stacks until they pass through a 1.20.1 inventory.
- The derived work-stump sides carry Mojang pixels and do not follow resource packs (the bark icons do).
- Kept from 4.B: a broken work stump drops an oak plank, and the chewed log is oak-textured; stems become FC's
  oak-textured, flammable chewed log.
- Class F and class S keep 1.20.1's 0.005 harvest hunger (INTEGRATIONS.md:488; follow-up FU-W5). Stand-in logs have
  no kiln and no smouldering; explosions drop whole logs; a chew can flicker for one frame; `barked()` uses a
  synthetic context with no player; chewed items are ejected upward (triage CF2-DR2).
  *(2026-10-28: chews eject toward the dug face since (bj). Class-S logs blast whole at the blast's chance, as 4.B
  drops FC's logs, and break into sawdust under a mining charge, since (bn).)*
- Follow-ups: FU-W1, the sidings, mouldings, corners and decorative blocks of the four woods (approved 2026-10-20);
  FU-W2, the lightning trunk burn; FU-W3, FC leaves support from log2; FU-W4, endermen carrying log2; FU-W5.
  *(2026-10-28: FU-W1 is done in (bp), where the four woods' planks also take FC's world identity and plank rule.)*
- No proof row covers M8, M2's dispenser take or M3's creative list. Review minors accepted at landing: the
  log-rule chew runs before a tool's onBlockStartBreak; DT's recipe-planks path takes the first `#minecraft:logs`
  stack; the W-B1 rows are unconditional even if log2 is refused.

## 2026-10-20 (az) INV-RESYNC: a re-sync never replaces or discards an FC stack FC may still hold (CF2-I1)

**Symptom (CurseForge triage CF2-I1, survival; maintainer ruling of 2026-10-20: fix it first, before the use-order
change).** Placing a block against BTW terrain could leave the held stack untouched: gravel and other slabs
combined onto a slab never ran out, and dirt, cobblestone, planks, pressure plates and loose snow were placed for
free, also from a ladder (CF2-I5). The client then showed one count while the server held another, so a shift-click
made phantom items (CF2-I2), and blocks placed before a death came back in the death pile (CF2-I3). The use-order
change of (ba) would have widened it to stakes (CF2-I6). Dyeing a windmill used no dye. (z)'s caveats had recorded
the mechanism as "Pre-existing" without its effect. HEAD: the joined-client plan placeconsume failed all 7 GAP
rows, and the client showed 3 cobblestone while the server held 4.

**Root cause.** 1.5.2 has one live `ItemStack[]` that FC edits in place (vanilla/server InventoryPlayer.java:21,
getCurrentItem :47-50), so the stack FC takes stays the inventory's stack for the whole call. The port's
InventoryBridge hands FC a snapshot and re-ran `sync()` on every `PlayerBridge.getOrCreate`, including the ones
nested inside FC's own calls: WorldBridge's entity extraction under `NotifyNearbyAnimalsOfPlayerBlockAddOrRemove`,
and the ladder gate under `canPlayerEdit`. A nested sync replaced the held FC stack with a fresh copy, so a
decrement after the notify (ItemBlock, ItemReed) hit a detached object and was lost, and one made before the notify
(FCItemBlockSlab and its kin) was overwritten by the re-read. `writeBack`'s adoption of a detached stack, meant to
rescue such edits, could also overwrite a genuine 1.20.1 change of the slot.

**Fix (general rules).** No INTEGRATIONS entry (bridge correctness); BRIDGE_SPEC.md has the sync rule.
- **The rule** (InventoryBridge class javadoc, InventoryBridge.java:21-30): while a real slot is exactly what the
  bridge last baselined (the same object, or empty then and now, with the same count, damage and NBT), `sync()`
  (:63) keeps FC's object in that slot; if FC changed it since the baseline, the change is written to the real slot
  first (server only). Only a real slot that changed is re-read, and then the real slot wins. `syncSlot` (:77) is
  `keepLive` (:93, with `mcUnchanged` :112) or `rebuildSlot` (:147); `rebaseline` (:340) records two MC-side
  baselines.
- `writeBack` (:239) no longer adopts a detached FC stack (the `syncFc` field is gone); `writeBackAll` :261.
- **Two escape hatches, explicit at each call site:** `discardPendingEdits` (:274;
  `PlayerBridge.discardInventoryEdits`, PlayerBridge.java:244) drops FC's edits; a shadow scope (`beginShadow`
  :288, `endShadow` :293) keeps a nested sync from writing them, and the scope's owner lands or drops them.
  - Shadow scopes: FC's `onItemRightClick` in ServerPlayerGameModeMixin.btw$useItem (:468-478; edits always
    dropped) and FC's `onEaten` in ItemStackMixin.btw$finishUsing (:129-167; only the bowl write-back lands).
    Review fix: the creative FC placement call is scoped too and closed after the count restore (now in
    ProxyBlock.runFcItemUse, ProxyBlock.java:799-809): a creative blood wood sapling placed in the Nether near FC
    pigmen lost the last item through two nested syncs.
  - A discard in every FC catch: ProxyEntity (:400, :450), FCContainerMenu (:417, :694), ProxyAnimal (:502),
    ProxyMob (:737), ProxyPathfinderMob (:422) and ProxyItem.onUseTick (:645).
- **Held-slot resend** (`resendHeldSlot`, :308): after FC places through Stage 2 (ProxyBlock.use :606,
  BlockBehaviorMixin.btw$use), the server resends the held slot, so the client shows the server's count.
- ProxyEntity writes back after FC's `interact` (the windmill dye), as ProxyAnimal and ProxyMob already did.
- Two rules are now explicit: a failed FC call lands nothing; shadow paths discard.
- Unchanged: FC code. `PlayerBridge.getOrCreate` and `syncFromReal` still call `sync()`; the rule lives in what it
  does per slot.

**Verification.** Record, stage INV (2026-10-20); plans and gates in the scratchpad's invresync/. The proof's fake
player is not in the level (TickProof.java:154, :385-392), so server-only plans cannot see CF2-I1: the fix was
proven with a development client joined to the dedicated server.
- placeconsume (joined client, 30 checks): HEAD GAP 0/7; after2 GATE after OK 30/30 (GAP 7/7: G1, G6, G3b, L1, G2,
  G4 x2; CONTROL 23/23) and HAND OK (slab x3, air, lever x3, cobblestone x3). Ablation, the resend alone: the
  client shows the server's count, and every GAP row still fails.
- windmill 4/4 (the dye is used); P-INV-S 3/3 (P10-1 `left=3`, CF2-P10); hookclient, bedclient and holduseclient
  SAME.
- Server regressions: 24 of 25 plans SAME with identical output values (`left=`, `ran=`/`doneAt=`); lso_p1's large
  campfire row is a pre-existing flake (86/86 on two reruns). After the creative review fix, P-INV-S, P-USE, P-BM,
  P-PLACE, P-RM and P-SMOKE1/2 SAME again.
- placeconsume passes 30/30 again after (ba) (its stake row S1 included), after (bb) with (bc), and after wave C.

**Caveats.**
- Watch items: a client that rejoined after a stalled login missed slot updates until a full resync (one invalid
  run; never on a first-attempt join); one of four client runs missed the single stake click (a timing flake; a
  server-side onGround witness before S1 would tell it from the airborne rule).
- Shadow paths stay non-4.B: for vanilla items with FC replacements (snowballs, eggs, buckets, bottles, maps,
  stews) vanilla runs after FC and FC's edits do not count ((aj) caveat; open question 1 of invresync/design.md).
- "A failed FC call lands nothing" is a port rule that 1.5.2 never had (a throw crashed the game); open question 2.
- When the real slot and FC both changed, the real slot wins at a sync, but `writeBackSlot` still lets FC win if no
  sync came between (pre-existing). A fingerprint drift falls back to the re-read: safe, but CF2-I1 would return
  for that item. Locked slots keep the old re-read.
- Retires the "Pre-existing" re-sync caveat of 2026-10-10 (z) (annotated there).

## 2026-10-20 (ba) Use order: the block before the held FC item (I-1)

**Symptom.** ServerPlayerGameModeMixin's HEAD pre-emption `btw$useItemOn` ran FC's `onItemUse` at the head of
`useItemOn` for every FC item but bone meal, before Forge's RightClickBlock event, the spectator check and the
block's own activation ((aj) caveat; (at) caveat BM8): flint and steel used on a lever did not toggle it; Stage 2
(the held FC item after the block declined) ignored a claim mod's DENY; an off-hand click ran the main-hand FC
item; a successful FC use counted in no statistic. 4.B activates the block first (vanilla/server
ItemInWorldManager.java:376-407). HEAD: P-USE 21/39.

**Root cause.** The pre-emption itself; FC's Stage 2 (ProxyBlock.tryPlaceHeldItem) never read RightClickBlock's
verdict; nothing gated the hand.

**Fix (general rules).** INTEGRATIONS.md #033, tag `fc_item_use_order`; BRIDGE_SPEC.md has the click frame.
- `btw$useItemOn` is deleted. `useItemOn` runs 1.20.1's order: RightClickBlock, the spectator check, the sneak
  rule, `blockstate.use` (Stage 1, FC's `onBlockActivated`, then Stage 2), then `ItemStack.useOn`.
- **The click frame** (contract C5, PlayerBridge.java:313-420): `UseClick` (:313) on a per-player stack,
  `beginUseClick` :355, `endUseClick` :361, `isFcUseDenied` :366, `setFcUseDenied` :372, `markFcItemUse` :382,
  `fcItemUseRanAt` :390; a frame from an earlier tick is dropped when the next click opens. In
  ServerPlayerGameModeMixin, `btw$beginClick` (HEAD of useItemOn, never cancels; :244-246) opens it and
  `btw$afterUseItemOn` (RETURN; :368-370) keeps the facing rewrite and closes it.
- **The item step** `btw$fcItemStep` (a cancellable inject at INVOKE `ItemStack.useOn`, `require = 2`; :261-265)
  skips the off hand, BlockItems, items without an FC identity, a block Stage 2 already ran FC on, and bone meal
  (`BoneMealPolicy.isFcBoneMeal`). It runs FC through ProxyBlock.tryPlaceHeldItem; on success it opens any FC
  container, writes back every changed slot, resends the held slot (:336), syncs the ProxyBlockEntity, fires
  ITEM_USED_ON_BLOCK (:342), closes the frame and returns CONSUME.
- **One FC `onItemUse` point,** ProxyBlock.tryPlaceHeldItem (ProxyBlock.java:657): a DENY recorded by
  `FcItemUseOrder` (FcItemUseOrder.java:41-45, a LOWEST server-side RightClickBlock listener) returns false with
  nothing recorded; then the click mark, then `runFcItemUse` (:778: the FC run and 1.5.2's creative count restore,
  in a shadow scope since (az)), and `Stats.ITEM_USED` on a true result (:686). ProxyBlock.use (:567) answers PASS
  for the off hand on the server; BlockBehaviorMixin.btw$use (:257) gates the main hand before any FC lookup.
- Rebased onto (az) before landing: the only conflict was `btw$useItemOn`, which this deletes; its creative
  guarantee now lives in `runFcItemUse`, and a successful item step resends the held slot.
- The ProxyBlock client comment is corrected: 1.20.1's `MultiPlayerGameMode.useItemOn` sends the use packet
  whatever the block answers (MultiPlayerGameMode.java:284-296; ProxyBlock.java:623-624).
- Comments only: BoneMealPolicy, DungSpreading, ProxyItem.useOn. Unchanged: FC code.

**Verification.** Record, stage I1 (2026-10-20):
- The refmap carries `m_7179_` (useItemOn) and `m_41661_` (useOn).
- P-USE 39/39, GATE B OK (HEAD 21/39; GAP 18/18). P-HOOK 19/34, GATE B OK: P1a and P1b flip, and its wave-C rows
  wait for (be) and (bf).
- Every other plan SAME, with intended output changes: the stump remover and a stick on grass reply SUCCESS instead
  of CONSUME (the block answers first, same `left=`); flint and steel on a lever toggles the lever (P1a); `useheld`
  under a RightClickBlock cancel fails (P1b).
- Clients: placeconsume 30/30 with HAND OK, its stake row S1 passing through the item step (CF2-I6 closed);
  windmill OK; hookclient, bedclient and holduseclient SAME.

**Caveats.**
- Residuals, accepted by the maintainer pending the activation audit: D6 (redstone ore, the empty flower pot,
  signs, a tuning fork on a note block, TNT wear 1, a fire charge priming TNT), D8 (the client's `useItem` after an
  FC success), D4 (the decline fallback), D9 (an off-hand sneak).
- `BedBlock.use` consumes every server click, so a bed click in the Nether or the End explodes whatever is held; a
  sneak still reaches the FC item, as in 4.B.
- Between this landing and wave C, a night bed click with an FC item in hand reached the old
  `PlayerMixin.btw$disableBeds` ClassCastException; (be) deleted it.
- Modern-Common ItemReed.java:8 named the deleted `btw$useItemOn`; *(2026-10-21: it now names `btw$fcItemStep`.)*
- Closes (aj)'s HEAD note and (at)'s BM8 (both annotated there).

## 2026-10-20 (bb) Beds and doors break and drop as in 1.20.1

**Symptom (CurseForge triage CF2-DR1, CF2-DR6; maintainer ruling of 2026-10-20: beds and doors take 1.20.1
drops).** Breaking the foot of a bed in survival gave two beds (a red one and one of the bed's colour) and its head
one red bed; an oak door's top half (or an iron door's, with a pickaxe) gave two doors; in creative, a bed's foot
and a door's top dropped an item; the FC saw on an oak door's top gave two doors; an FC mining charge on a door, or
on a bed facing north or west, left nothing. 4.B gives one item, and nothing in creative.

**Root cause.**
- A: FC dropped legacy 26, 64 or 71 from either half: Modern-Common BlockBed and BlockDoor have none of 1.5.2's
  drop rules, so `Block.idDropped` is the block's own id, and `toMcStack(26)` resolves to the canonical red bed.
- B: 1.20.1 pops the partner half with its own loot when one half goes (`updateShape` returns AIR, then
  `Level.destroyBlock`).
- The duplication was A plus B on the FC harvest (survival and creative) and on FC's own drop at a live cell (the
  saw). The loss was the mining charge: its drop entry point, `Block.DropItemsOnDestroyedByExplosion`, was an empty
  stub in Modern-Common, although 1.5.2's Block had a body there (vanilla/server Block.java:1730-1747). The P0
  table above did not list it (triage correction 11).

**Fix (general rules).** INTEGRATIONS.md #039, tag `vanilla_pair_drops`. The rule, "one item, two cells": a block
that is not a ProxyBlock, has an FC identity and has `BED_PART` or `DOUBLE_BLOCK_HALF` in its 1.20.1 state
(`VanillaPairDrops.isRuled`, VanillaPairDrops.java:84-111) takes 1.20.1's break, drops and partner handling. Today
that is 18 blocks: the 16 beds (26), the oak door (64) and the iron door (71).
- **S1, the player break:** ServerPlayerGameModeMixin.btw$destroyBlock returns without cancelling for a ruled state
  (ServerPlayerGameModeMixin.java:123-125), so vanilla `destroyBlock` runs (the BreakEvent, `playerWillDestroy`'s
  creative partner removal, the loot, the partner pop) for survival, creative, adventure, fake players and direct
  callers. A RETURN inject into Forge's `removeBlock` (:105-113) runs FC's animal panic after the removal and
  before the drops, 4.B's order.
- **S2, FC drops at a live cell:** the World hook `UsesModernBlockLoot` (Modern-Common World.java:893,
  WorldBridge.java:3236) at both FC drop entry points: `Block.dropBlockAsItemWithChance` extends its twin branch
  (Block.java:653-659; the saw), and `DropItemsOnDestroyedByExplosion` gets 1.5.2's base body behind the hook
  (Block.java:1534-1544; the mining charge). An FC drop of a ruled id at a live ruled cell becomes that cell's
  1.20.1 loot (`DropModernBlockLoot`).
- **B3, harvest exhaustion:** the new BlockPlayerDestroyMixin charges FC's 0.025 instead of 1.20.1's 0.005 for
  ruled blocks (`VanillaPairDrops.chargeHarvest`, :145).
- **W, FC's tool wear:** ItemStackMixin.btw$mineBlock (:353) wears the live held FC stack through FC's
  `onBlockDestroyed` when an FC item mines an FC-identity block, and skips 1.20.1's `Item.mineBlock`; one helper,
  `ProxyItem.onBlockDestroyedLive` (ProxyItem.java:739), serves it and `ProxyItem.mineBlock`. Shears take no wear
  from a bed or door, as in 4.B.
- **Audit:** `VanillaPairDrops.audit` (:174), at server start (wiring W-BD2), logs each ruled block and a summary,
  and WARNs on W1 (a ruled block that is neither a BedBlock nor a DoorBlock), W2 (a ruled block whose 1.20.1 class
  does not override `neighborChanged` while its FC class declares `onNeighborBlockChange`), W3 (a legacy id that
  maps ruled and unruled blocks) and W4 (26, 64 or 71 without a ruled block). Wiring W-BD1 adds
  BlockPlayerDestroyMixin to the mixin list.
- FC keeps the dig speed, harvest exhaustion, tool wear and animal panic. Not done, on purpose: no bed or door
  codec (drops no longer need one), no 1.5.2 drop rules in Modern-Common BlockBed and BlockDoor, and no
  `DropItemsOnDestroyedByExplosion` body for any other block (CF2-DR5). The queued Modern-Common drop-parity
  follow-up (D7) must drop its FCBlockBed, FCBlockDoor and FCBlockDoorWood rows (ruled vanilla), or keep
  FCBlockDoorWood only for the ProxyBlock door 1036 (open question Q4); audit W2 and HookPolicy's RULED(5) row
  ((be)) guard it.
  *(2026-10-28: CF2-DR5 is done in (bn): the base body is 1.5.2's for every block; a bed or door at its live cell
  still drops its own 1.20.1 loot through UsesModernBlockLoot.)*
- Unchanged: FC code; explosions through `level.explode`, pistons, fire and FC zombies, which already took 1.20.1's
  path and gave one item.

**Verification.** Record, stage BDD (2026-10-20), with the harness addendum (verbs `fcmine` and `listen expect`):
P-BEDDOOR 288/288, GATE BD OK (GAP 63/63; 225/288 at the I1 baseline, every GAP row failing), and 288/288 again
after wave C. All 25 regression plans SAME. Log: `[SELF-AUDIT/pairdrops] 18 ruled blocks (16 BedBlock, 2 DoorBlock,
0 other); FC drops of ids [26, 64, 71] at a live cell take that cell's 1.20.1 loot; 0 warnings (0 expected)`.

**Caveats.**
- As in 1.20.1 and 4.B, an iron door's top broken by hand drops the door (Q1). A door whose top burns first drops
  itself (1.20.1; 4.B dropped nothing; Q6).
- New 1.20.1 side effects on the break: the BLOCK_MINED stat, ITEM_USED for a tool FC wore, the BLOCK_DESTROY game
  event, other mods' loot modifiers on beds and doors, and PlayerDestroyItemEvent when FC breaks a tool (also from
  ProxyItem tools on modern blocks, through the shared helper).
- A mod that changes the locals of Forge's `removeBlock` fails the mixin at load (CAPTURE_FAILHARD), loudly; so
  would a Forge update that restructures it.
- A future FC identity on a `DOUBLE_BLOCK_HALF` block would switch its drops to 1.20.1 (audit W1 WARNs; Q3).
- FC's own wood door 1036, a ProxyBlock, still double-drops if it is ever placed; it is unreachable in survival
  (Q4).
- A bed codec is still wanted for FC reads (`BlockBed.isBlockHeadOfBed`, ocelots; Q8). Open: Q4, Q7 (RULED(5),
  carried by (be)) and Q9 (CF2-DR5 in its own slot); Q1, Q3, Q6 and Q8 stand at their defaults.
- The FC animal panic and PlayerDestroyItemEvent have no proof row (a manual check and code reading).

## 2026-10-20 (bc) 1.20.1 black, brown and blue dye are colours; 1.5.2 subtype stacking

**Symptom (maintainer rulings of 2026-10-20: modern black, blue and brown dye are colour only, while ink for the
book and quill, polished lapis and chocolate stay with the ink sac, lapis and cocoa, as in 4.B; restore 1.5.2's
subtype stacking).** LegacyVariants spelled 1.20.1 black, brown and blue dye as the 4.B ink sac (351:0), cocoa
(351:3) and lapis (351:4): in FC hoppers, chests, cauldrons and dispensers they turned into those items (blue dye
into lapis, brown dye into plantable cocoa beans); at FC grids cornflowers gave lapis and wither roses ink sacs,
and nine blue dyes made a lapis block. FC hoppers and chests merged no subtyped item into an existing stack (lapis,
cocoa, bone meal, white dye, BTW wool, candles, vases, slabs, sidings, bark: one slot per pickup), and moving black
dye off subtype 0 would have put it on that list too (dye3 risk R3). HEAD: P-DYE3 35/60.

**Root cause.**
- Each of the three dyes was the same FC stack as its 4.B item, as white dye was before (aw).
- Modern-Common `ItemStack.isItemDamaged` was `damage > 0`, where 1.5.2 counts damage only on an item with
  durability (vanilla/server ItemStack.java:239-242), and FC merges only an undamaged source
  (FCUtilsInventory.java:347, :538).

**Fix (general rules).** INTEGRATIONS.md #038, tag `modern_dyes`:
- **Four spellings** (the R-SPELL rule): black, brown and blue dye are FC 351:256, 259 and 260 both ways
  (LegacyVariants.java:219-221), beside white dye's 351:271 from (aw). `SPELLED` and `spelledSubtypes()` (:73,
  :277) list the spelled subtypes, `351 -> {0, 3, 4, 15}`. ItemDye's subtypes now carry four spellings
  (Modern-Common ItemDye.java:7-13). FC's colour code masks the damage, so sheep, wolf collars and windmills read
  the colour.
- **Colour roles from FC's own data** (`ModernSpellingRecipes`, new; `apply` :66): a 4.B recipe is a colour role
  when FC makes the same recipe with another dye colour, and each such recipe that takes a spelled subtype d gets a
  twin taking (351, d | 0x100). Grid twins are appended to FC's list, so the existing `fc_recipe_*` ids keep their
  place; bulk twins go right after their original, keeping FC's first-match order; a metadata-exclusive bulk recipe
  is never a member. One-colour recipes stay with the 4.B item: the book and quill, polished lapis, chocolate milk,
  chocolate, chicken feed and dog food. The librarian's ink sac trade refuses black dye (the slot rule of (aw)),
  and black dye passes wicker and slat filters like the other dye powders.
- **Changed at landing:** the draft ran `apply` from its own annotated MOD-bus `Setup` class, which Forge
  subscribed but never called (no log line, even from a diagnostic, and the common-setup queue ran without it;
  cause not found; BoneMealPolicy$Setup, the same pattern, runs). `BTWForgeMod.commonSetup` now enqueues it
  (`event.enqueueWork(ModernSpellingRecipes::apply)`, BTWForgeMod.java:116-118), the Setup class is gone, and
  `apply` WARNs when no spelling is registered (ModernSpellingRecipes.java:76) instead of returning silently.
- **1.5.2 subtype stacking** (dye3 question Q2, option A): `isItemDamaged` is false for an item that declares
  subtypes and has no durability (Modern-Common ItemStack.java:111-120), 1.5.2 parity for every subtyped item.
  Items without the subtype flag keep `damage > 0` until the setHasSubtypes audit, because FC's merge would not
  compare their damage.
  *(2026-10-28: since (bk) the plain block items, ItemMultiTextureTile and the skull item carry the flag.)*
- Comments in RecipeBridge, WorkbenchFallback, ItemDye and MerchantRecipeList. Unchanged: FC code.

**Verification.** Record, stage BDD (2026-10-20):
- P-DYE3 60/60 (HEAD 35/60). The first full stage read 50/60, the plan's "twins left out" signature, which exposed
  the Setup class that never ran.
- P-DYE3Q2 10/10; 6/10 in the ablation without the isItemDamaged hunk (the plan's "now" column).
- Log: `[MODERN-DYES] colour twins for the 1.20.1 dye spellings {351=[0, 3, 4, 15]}`, then crafting +12 (8
  one-colour recipes kept with the 4.B item), Cauldron +9 (1 kept), anvil and CauldronStoked +0 (1 kept each),
  Crucible, CrucibleStoked and MillStone +0.
- P-RV census: `sweep ns minecraft total=708 ... fail=27` (fu_runs/INV/BDD_after2/P-RV.txt and C_after/P-RV.txt; 32
  in fu_runs/INV/cur/P-RV.txt, before this change); `sweep fc total=675 ... fail=0` (663 before: the 12 new grid
  twins). All 25 regression plans SAME, P-LEAF-V 32/32 and P-LEAF-DT 10/10 included.

**Caveats.**
- Dyes saved in FC storage before the update read back as the ink sac, cocoa or lapis (R1: their identity was never
  stored). An older jar falls back to item 351 with {Damage:n} (R2).
- Pre-existing, now extended to the modern dyes by the twins: in the 2x2 grid FC colour recipes accept a candle,
  wool, vase or wool slab of any colour, because fc_recipe ingredients match by item only (R11; the general cure is
  queued as Q5).
  *(2026-10-28: fixed by (bp), Q5's server half: FC's crafting recipes also ask FC's own match on the FC reading of a
  square 1.20.1 grid; the recipe book and JEI still place by item.)*
- The entity colour paths (sheep, wolf collars, windmills) are checked by code reading only (Q3). Open: Q4, cocoa
  powder versus cocoa beans.
- The isItemDamaged divergence of (aw) now covers only items without the subtype flag; the setHasSubtypes audit
  stays queued.
  *(2026-10-28: narrowed again by (bk); the remainder is follow-up F-SUBTYPES.)*

## 2026-10-21 (bd) Placement validity: FC placement follows 4.B's rules (I-2)

**Symptom.** FC's block placement (Stage 2 and the item step) checked almost nothing:
- An FC-route block went into a solid or non-replaceable cell: on 2026-10-18 a rerun of (at)'s removed-content plan
  "placed" a tripwire hook inside the grass block it was used against, and FC placement overwrote flowers, torches,
  rails and redstone dust in the target cell.
- No 1.5.2 or FC `canPlaceBlockOnSide` rule ran: poppies and dandelions went onto stone, slabs and cobblestone
  (triage CF2-P8a), and hooks onto any face of any support.
- Items and XP orbs blocked placement ((ak) caveat), while the two-argument checkNoEntityCollision always answered
  true, so FC windmills and water wheels went up through mobs and players.
- A refused FC placement fell through to 1.20.1's BlockItem, which placed the block anyway; the oak door was used
  up with nothing placed; vanilla-route stairs, doors, beds, gates and observers came out reversed; falling sand
  deleted rails; FC-placed logs got no axis; an FC oak trapdoor or fence gate placed against a live signal stayed
  closed.
- Found by the proof gate (record, stage head): the facing rewrite read the held stack at RETURN, which is empty
  after the last item, so a block placed from the last item of a stack was never rotated: facing depended on the
  stack size.
- Before wave C: P-PLACE 8/29 (GAP 0/19).

**Root cause.**
- WorldBridge.canPlaceEntityOnSide only asked whether any entity stood in the full cell (unchanged since 520e554);
  1.5.2's World.canPlaceEntityOnSide (vanilla/server World.java:3377-3407) also needs a free or replaceable target
  and the block's `canPlaceBlockOnSide`, and counts only entities that block building. The two-argument
  checkNoEntityCollision was a stub.
- Modern-Common bases lacked 1.5.2 placement hooks their FC subclasses inherit (BlockTripWireSource's
  canPlaceBlockOnSide, canPlaceBlockAt and onBlockPlaced; BlockLog.onBlockPlaced; more are queued).
- A Stage-2 refusal was not final, and the facing rewrite was not tied to the cell FC placed.

**Fix (general rules).** INTEGRATIONS.md #040, tag `placement_validity`:
- `WorldBridge.canPlaceEntityOnSide` (WorldBridge.java:1957) is 1.5.2's rule: a free or replaceable target, the
  entity check, then `canPlaceBlockOnSide`; a throw refuses. The placing player counts in the entity check, as
  4.B's client pre-check did (review fix 1), so buckets, repeaters, urns and stakes cannot go into the player's own
  space.
- `checkNoEntityCollision` (:1877, two-argument :1896) follows 1.5.2: only entities that block building count
  (1.5.2's preventEntitySpawning) and spectators are skipped, so items and XP orbs no longer obstruct, and mobs and
  players stop windmills and water wheels.
- The tripwire hook codec (direction | 4 attached | 8 powered; :533-542, :809-818); `isMetaFullyDerived` covers the
  hook (:284-300), and a fully derived block never reads the metadata overlay, so a stale entry is dropped
  (:235-237, review fix 3).
- Modern-Common base ports: BlockTripWireSource's canPlaceBlockOnSide, canPlaceBlockAt and onBlockPlaced
  (BlockTripWireSource.java:32-60), and BlockLog.onBlockPlaced, the axis from the clicked face
  (BlockLog.java:26-35).
- ProxyBlock.tryPlaceHeldItem declines the oak door (FC item 324) and leaves it to 1.20.1's DoorItem until the door
  codec exists (:667). After FC runs, it records PLACED with the cell that changed into the held block, or REFUSED,
  but only for a player who may build (:698-701, review fix 2: an adventure-mode refusal records nothing, so
  1.20.1's CanPlaceOn decides).
- The `Stage2` record on the click frame of (ba) (PlayerBridge.java:331; `stage2` :396, `setStage2` :402 and :407,
  `stage2At` :416). In ServerPlayerGameModeMixin, a REFUSED record at the clicked block makes the item step resend
  the held slot, close the frame and return FAIL (:284-285, review fix 7), so 1.20.1 places nothing;
  `btw$afterUseItemOn` acts only on PLACED, on the placed cell: the facing rewrite never touches a hook (review
  fix 4) and runs only if the new facing survives, and a trapdoor or fence gate placed against a live signal then
  opens (G2).
- PlayerBridge: `syncFromReal` copies the swimming state into FC's `inWater` (:213-217), and `canPlayerEdit`
  answers a riding player from the real player (:828-838), so 4.B's mid-air rule does not refuse a swimming or
  riding player.
- Unchanged: FC code.

**Verification.** Record, stage C (2026-10-21), run folder fu_runs/INV/C_after:
- P-PLACE 27/29, GATE C OK (8/29 before; the two KNOWN rows fail as labelled). P8-1 to P8-3 (a poppy on stone and
  on a smooth stone slab, a dandelion on cobblestone) are refused; P8-4 (deepslate, the vanilla route) and P8-5 (a
  poppy on grass) are controls. P-PLACE-C (joined client) GATE C OK, the client hand `minecraft:tripwire_hook x1`.
- The place sweeps run to the end (1440 -> 4172 lines, 0 THREW). Of the 1436 lines both runs have, 52 changed, each
  by a 1.5.2 rule (fu_runs/C/placesweep.diff.txt): logs on a north face become `axis=z`; on stone's top, saplings,
  grass, fern, dead bush, every flower, mushrooms, ladder and cactus are refused; on a north face over grass, dead
  bush, cactus and mushrooms are refused; twisting vines' age (random) and the lily pad's reply code change with no
  block either way.
- Every other plan SAME with identical output values (29 plans); the clients placeconsume, windmill and holduse
  SAME.
- The stack-size finding is gone by construction: the rewrite acts on the cell PLACED records and never reads the
  held stack.

**Caveats.**
- KNOWN H4: glass counts as a normal cube for FC placement (Modern-Common Block.isNormalCube(20) is true; decision
  7(b) queued). KNOWN H14: FC-route stairs still face reversed (decision 6(c) queued).
  *(2026-10-26: H4 is closed by RANB-PARITY (section 3): Block.isNormalCube follows 1.5.2 and glass is no normal cube;
  P-PLACE's H4 passes since W1. H14 stays.)*
- Queued: sneak placement uses 1.20.1 rules (8(b)); FC placements post no EntityPlaceEvent (9(b)); FC-route
  trapdoors take meta 0 (look-based facing, bottom half) until decision 3 stage 2; an FC-placed hook's attached
  state waits for a neighbour update (4(b')); the other Modern-Common bases that lack 1.5.2 placement hooks
  (button, lever, vine, the piston parts, cake, chest, door, fence gate, flower pot, redstone wire, cocoa, wood
  slab, quartz, trapdoor) wait for decision 3.
  *(2026-10-28: for torches, soul torches and ladders, 1.20.1's placement, sneaking included, also asks 4.B's support
  rule since (bs). 2026-10-31: so do snow layers, pressure plates and rails, and FC's own torches, ladders, ash and
  soulforged plate (bs), and the solid top FC's own placement rules ask is 4.B's (bw); since 2026-11-02 the modern
  pressure plates too, and modern fences, walls and stained panes count as their BTW relatives as supports (bx).)*
- Other mods' fake players keep 4.B's mid-air rule (maintainer, 2026-10-20): a never-ticked fake player that calls
  `useItemOn` is refused on FC-modelled targets, and REFUSED is final. Create's deployer calls `useOn` directly and
  is not affected.
- Not run (I-2.report.md:160-169): placing while swimming or riding; the placing player in the entity check with a
  real player; adventure mode and CanPlaceOn; a world saved before this change with an FC-placed hook; other mods'
  fake players; how much the 1.5.2 entity rule now refuses for windmills, spawns and falling blocks.
- Closes (at)'s tripwire-hook caveat and corrects its verification note, and closes (ak)'s item-obstruction caveat;
  (ap)'s ItemBlock note is annotated too.

## 2026-10-21 (be) Hook ownership: beds, anvil, golems, enchanting table, fence gates, farmland (I-3)

**Symptom (maintainer rulings of 2026-10-19: beds let players sleep but never set the spawn (ruling 3); doors,
trapdoors and fence gates keep 1.20.1 redstone (ruling 4)).** The port ran FC's 1.5.2 block hooks only from the
1.20.1 base bodies (BlockBehaviorMixin, BlockMixin), so whether FC ran depended on 1.20.1's class layout: 231
override-without-super pairs bypassed FC, 108 of them hooks 4.B changed, and nothing could say that FC replaces
1.20.1 or that 1.20.1 owns a hook FC declares. In play:
- Breaking a beacon or ender chest crashed or lost the block, because FC's breakBlock cast the TileEntityBridge it
  was handed; TNT next to a beacon stopped the server (P-HOOK-CRASH: `TileEntityBridge cannot be cast to
  FCTileEntityBeacon`).
- A night bed click threw a ClassCastException in `PlayerMixin.btw$disableBeds` after the bed had already set the
  spawn, and a day click set the spawn; nobody could sleep (triage CF2-U1).
- The anvil opened 1.20.1's repair menu instead of BTW's 3x3 crafting station; iron and snow golems could be built
  from a pumpkin; FCGameplayBridge cancelled the enchanting table; fence gates did not notify their neighbours;
  farmland was trampled by two rolls (1.20.1's, then FC's).
- Before wave C: P-HOOK 20/36.

**Root cause.** No hook had an owner: the FC call sat in the base bodies, reached only where the 1.20.1 class does
not override the hook or calls super; FC's block-entity bodies ran on 1.20.1 block entities; the bed veto threw.

**Fix (general rules).** INTEGRATIONS.md #036 (tag `hook_ownership`) and #037 (tag `beds_sleep_no_spawn`):
- **One owner per hook per state** (`HookPolicy`, HookPolicy.java:113, shaped like TickPolicy): ten hooks (USE,
  ATTACK, PLACE, REMOVE, NEIGHBOR, INSIDE, SIGNAL, STEP, FALL, RESIST), two bits each: UNDECIDED (the executors
  keep their dynamic lookup), NOTIFY (FC runs where the 1.20.1 chain reaches the base body), FC (FC replaces 1.20.1
  on the server, enforced for USE and PLACE) and VANILLA (FC's block never runs for the hook). `decide` (:216) is
  cached on the state at `initCache` TAIL by the new BlockStateHookMixin (priority 1100), which also enforces FC
  rows with cancellable HEAD injects on the `use` and `onPlace` forwarders.
- **Rules, in order:** (1) FC not ready, a ProxyBlock or legacy id 0: no decision; (2) a RemovedContent T1 block:
  VANILLA; (3) the block-entity guard: USE and REMOVE of a state whose FC block is a `btw.modern.BlockContainer`
  are VANILLA (`isTeGuarded` :272), because its block entity is always 1.20.1's; (4) RULED and ADAPTER rows,
  permanent (`ruledRule` :277); (5) PENDING rows, VANILLA until their metadata codec lands; (6) EQUIVALENT (every
  FallingBlock's PLACE): NOTIFY; (7) the general rule: FC if the 1.20.1 class overrides the hook and the FC class
  declares it in FC code, else NOTIFY. RESIST stays UNDECIDED until the explosions phase.
  *(2026-10-28: RESIST is decided by (bn): FC for a block whose FC class reads its position for blast resistance
  (stone, the ores), else NOTIFY.)*
- **Rows:** RULED(3), beds USE (ruling 3); RULED(4), door, trapdoor and fence-gate NEIGHBOR (64, 71, 96, 107;
  ruling 4); RULED(5), NEIGHBOR of every `VanillaPairDrops.isRuled` block ((bb); BD's recommendation, pending its
  question Q7); ADAPTER(D16b), fence-gate USE: the new FenceGateBlockMixin adds 4.B's neighbour notify at the
  RETURN of `use` and keeps 1.20.1's sounds and game event; ADAPTER(D9b), farmland FALL: FarmBlockMixin lowers the
  fall distance passed to `ForgeHooks.onFarmlandTrample` by 0.25 (FarmBlockMixin.java:79-88), so one roll uses
  4.B's 0.75 threshold. The general rule gives FC the three anvils' USE (BTW's 3x3 crafting station; an
  empty-handed sneak-click is left to 1.20.1's AnvilMenu) and the carved pumpkin's and jack o'lantern's PLACE (no
  golems from a pumpkin; a jack o'lantern placed from now on goes out next to water).
- **Executors** (BlockBehaviorMixin, BlockMixin): each is gated on the owner, and every FC call is in a try/catch.
  An FC throw answers 1.20.1's result (the anvil then opens AnvilMenu), except on an FC-owned PLACE, which still
  cancels so golems stay blocked; a throw on a click discards FC's pending inventory edits ((az)). The USE executor
  (`HookPolicy.activate` :388) runs Stage 1 only under the owner and the block-entity guard; Stage 2, the held FC
  item, always follows where the base body is reached, so a VANILLA USE owner drops Stage 1 only (review m2: a
  BlockItem clicked against a spawner, skull, end portal or removed block places through FC's ItemBlock and the
  rules of (bd)). `runFcOwnedUse` (:463) and `runFcOwnedPlace` (:483) serve the FC rows.
- Latent hook-audit findings fixed: H3, `attack` now gets a synced PlayerBridge (it had a null player); H4, REMOVE
  passes the derived metadata (`deriveVanillaMetadata`), not 0.
- **Beds** (#037): `PlayerMixin.btw$disableBeds` is deleted. The new `BedSpawnPolicy` (BedSpawnPolicy.java:51)
  vetoes every non-forced bed spawn through Forge's PlayerSetSpawnEvent, and
  ServerPlayerMixin.btw$fcHostilesPreventRest (ServerPlayerMixin.java:343-347) answers NOT_SAFE when an FC hostile
  is in 1.20.1's monster box (FC hostiles are proxies, never Monster). 1.20.1 sleep, the night skip, its messages
  and the Nether and End explosion stay; the night skip advances world time, so FC's clocks, the moon phase, gloom
  nights and Serene Seasons' calendar move with it.
- FCGameplayBridge's enchanting-table cancel is deleted (R16): the table works. WorldGamerulesHandler: javadoc only
  (doInsomnia stays false).
- **Self-audit** `[SELF-AUDIT/hook]` (BridgeSelfAudit.auditHookPolicy, :291, run after the tick audit, :60) WARNs
  on (a) an unexpected FC pair, (b) a block-entity-guarded FC hook other than the six known gaps (those log at
  INFO), (c) cached owners that differ from `decide`, (d) a RULED or ADAPTER row whose legacy id drifted
  (`RULED_IDS` :191), (e) an expected FC row (`EXPECTED_FC` :203) that no block carries.
- Unchanged: FC code; door, trapdoor and gate redstone, 1.20.1's before and after, now by an explicit row.

**Verification.** Record, stage C (2026-10-21), run folder fu_runs/INV/C_after:
- Build: Mixin config OK (54 entries); the refmap carries BlockStateHookMixin, FenceGateBlockMixin and
  `startSleepInBed`.
- P-HOOK 34/36, GATE C OK (20/36 before wave C; the two FAILs are the INVERTED rows P0b and P0c, which fail once
  fixed); P-HOOK-CRASH 1/1 (it crashed before), smoke 4/4 after it; P-BED-C (joined client) GATE C OK: the night is
  skipped and the bed sets no spawn. No ClassCastException, "invalid for ticking" or "Failed to handle packet" in
  the P-HOOK or bed-client server logs.
- `[SELF-AUDIT/hook] 242 blocks, 5 FC-owned and 182 VANILLA hook pairs, rows {ADAPTER(D16b)=[107],
  ADAPTER(D9b)=[60], RULED(3)=[26], RULED(4)=[64, 71, 96, 107], RULED(5)=[26]}, 6 known block-entity gaps, 0
  warnings (0 expected)`.
- The place sweeps no longer die on the ender-chest ClassCastException ((bd): 0 THREW). Every other plan SAME.

**Caveats.**
- PENDING rows stay 1.20.1's until hook phase P4 brings their codec: door USE (64, 71), the cake's bites (92), the
  crafting table (58, decision D12), the flower pot (140) and the detector rail's INSIDE (28).
  *(2026-10-28: the detector rail's INSIDE is EQUIVALENT since (bt): every DetectorRailBlock, 28 and BTW's 235 and
  236, takes FC's cart rule through DetectorRailBlockMixin.)*
- The block-entity guard keys on the FC class alone and never lifts by itself; it must narrow when Phase 4 bridges
  FC tile entities. Until then the note block and furnace USE and the ender chest and beacon USE and REMOVE are
  1.20.1's (the six known gaps).
  *(2026-10-28: the guard lifts for FC-backed blocks since (bq): the beacon's USE and REMOVE are FC's, and 4 known
  gaps remain: the note block and furnace USE, the ender chest's USE and REMOVE.)*
- RULED(5) waits for the maintainer's answer to BD's Q7; dropping it is one line in `ruledRule` and one `RULED_IDS`
  entry.
- Beds: a modded bed that reports neither `#minecraft:beds` nor `isBed` still sets the spawn; a forced spawn on a
  bed (`/spawnpoint`) is kept; a bed spawn set before the update is used for one more respawn. BTW 4.B's random
  hardcore respawn is not ported (triage CF2-U1-HS): respawns go to the world spawn or a charged respawn anchor.
  *(2026-10-28: ported in (br): players respawn by 4.B's hardcore respawn unless an anchor, `/spawnpoint` or a bound
  beacon applies (in a world created as creative, as in 1.20.1); a bed spawn saved before is ignored and dropped, not
  used once more.)*
- Golems: only golems built from a pumpkin or jack o'lantern are blocked; villager-spawned iron golems are
  untouched. No row covers golems through `/setblock` or a dispenser, or review m2's BlockItem routing.
  *(2026-10-28: new villages place no iron golem since (bm) (ruling D1 a1).)*

## 2026-10-21 (bf) Lava meets water by 4.B's rules (I-4)

**Symptom.** Lava meeting water followed 1.20.1: Forge's lava+water interaction turned any touching lava into
obsidian (a source) or cobblestone, lava flowing down onto water made stone, and water flowed into lava 4/9 high or
more. 4.B hardens a source into obsidian, and flowing lava of metadata 1-4 (within two blocks of its source, four
in the Nether) and lava flowing down onto water into a drop-less lava pillow; thinner and falling lava stays lava,
and no fluid displaces lava. (as) listed liquid hardening among the open hook families. HEAD: P-HOOK P3a, P3b, P3c,
P3e and P3f fail.

**Root cause.** 4.B's rules live in 1.5.2 BlockFluid and BlockFlowing (vanilla/server BlockFluid.java:303-357,
BlockFlowing.java:167-178, :443-447). The port's lava is 1.20.1's LavaFluid with Forge's FluidInteractionRegistry,
and LavaFluidMixin covered only the random tick ((as)).

**Fix (general rules).** Part of INTEGRATIONS.md #036, tag `hook_ownership`; no rebalance.
- `LavaRules` (new, LavaRules.java:73): `contactRule` (:105) wraps Forge's own lava+water test as
  `hardensOnContact(lava) && water`; `hardensOnContact` (:130) is true for a source, or for non-falling lava with
  amount 4 or more, which is 4.B's metadata 1-4 (level = 8 - amount). A source becomes obsidian and other hardening
  lava the pillow, found through `FcRefs.block("fcBlockLavaPillow")`; if it does not resolve, the cell gets
  1.20.1's cobblestone or stone and one WARN. It never throws. Cobblestone and stone generators now make pillows.
- `FluidInteractionRegistryMixin` (new, `remap = false`): a static `@ModifyArg` on the first `addInteraction` call
  in `<clinit>` (ordinal 0, index 1; FluidInteractionRegistryMixin.java:46-52) gates Forge's built-in entry in
  place, keeping the interaction order, basalt and other mods' lava interactions; a declined contact still gets
  LiquidBlock's flow tick.
- `LavaFluidMixin`: a `@ModifyArg` in `spreadTo` on argument 3 of `ForgeEventFactory.fireFluidPlaceBlockEvent`
  (`remap = false` on the `@At`; LavaFluidMixin.java:90-91) places the pillow where 1.20.1 places stone, and a HEAD
  inject on `canBeReplacedWith` (`btw$noFluidDisplacesLava`) answers false, 1.5.2's rule that no fluid displaces
  lava. A water bucket still replaces lava (BucketItem asks `canBeReplaced`). The random-tick inject of (as) is
  unchanged.
- Refmap facts: `spreadTo` maps to `m_6364_` and `canBeReplacedWith` to `m_5486_` (both LavaFluidMixin);
  FluidInteractionRegistryMixin has no refmap entry (`remap = false`). On a Forge bump, re-check with javap that
  ordinal 0 of `<clinit>` is still the lava+water entry: the code cannot see lava's two registrations trade places.
- The `[LAVA] 4.B lava contact rule installed` INFO line is logged at the first fluid update, not at start-up:
  `LiquidBlock.onPlace` and `neighborChanged` first load FluidInteractionRegistry.
- It relies on (be) keeping the `onPlace` and `neighborChanged` hooks of lava (10) and water (8) NOTIFY, never
  FC-owned; P3b, P3e and P3g would show a change. Row 5 of (as)'s tick table is unchanged.
- The review's major finding is in: gating falling lava had opened 1.20.1's water-displaces-lava path, so a water
  stream entering a lava fall ran down the column and was capped by a pillow. The `canBeReplacedWith` rule closes
  it.

**Verification.** Record, stage C (2026-10-21), run folder fu_runs/INV/C_after: P-HOOK P3a-P3h all pass (P3a, P3b,
P3c, P3e and P3f flip; P3d stays; P3g, a lava fall beside water stays lava, flips; P3h, no pillow in the lava fall,
is a control that fails without the `canBeReplacedWith` rule). The refmap carries `m_6364_` and `m_5486_`. The log
reads `[LAVA] 4.B lava contact rule installed on Forge's lava+water interaction: source -> obsidian, flowing meta
1-4 -> FC lava pillow, meta 5-7 and falling lava keep flowing`, with no other `[LAVA]` line.

**Caveats.**
- 4.B fizzed whenever lava touched water, even when nothing hardened (vanilla/server BlockFluid.java:353); the fizz is
  not reproduced.
- Breaking a pillow, now common for the first time, runs through ProxyBlock.onRemove to FC's breakBlock (it should
  release lava when no water is beside or above it, and drop nothing); no plan exercises it.

## 2026-10-26 (bg) Block quads take their plane's normal where FC set none; per-meta digging particles; the exact 4.B breaking crack (CF2-R3)

**Symptom (triage CF2-R3; maintainer rulings of 2026-10-22: the breaking crack on BTW's web, and on every BTW block
4.B cracked differently, takes the exact 4.B look (OD2 (b)); of 2026-10-23: with 1.20.1's crack images (OD-C3 (a))).**
- The breaking crack on BTW's spider web (1037) was smeared into vertical streaks, and so was the crack on BTW ladders
  (1051, 1052), the 4.B panes (wicker 1084, grate 1085, slats 1086) and FC's crop planes.
- The same quads were smooth-lit, and some were shaded by the wrong face: BTW plants, crops, ladders, panes and the
  sides of wet cement had dark corners.
- Every BTW block shed meta 0's top-icon particles when hit or broken (a damaged web, every variant block).
- 1.20.1's crack also came out stretched and cropped on the web's diagonal planes, mirrored on the bottom, west and
  east faces of BTW cubes and on west- and east-facing ladders, and laid on overlays, fire planes and FC tile-entity
  geometry (an open basket's lid, placed tools, cooking items), none of which 4.B cracked.

**Root cause.**
- 1.5.2 meshed chunks without GL lighting and shaded faces through vertex colour, so FC's world renderers never set
  normals for chunk quads. The port's capture keeps a sticky normal: UP from `runFcRender`, an UP set on vertical
  planes by the ported `renderCrossedSquares`, `renderBlockCrops` and `Block.RenderCrossHatch`, or the last face's
  normal; FC's own `RenderLadder` and the port's `renderBlockPane` set none. FCBakedModel turned that normal into each
  quad's direction and packed normal, and 1.20.1 reads both: the face shade (`ClientLevel.getShade`), ambient
  occlusion, and the axis `SheetedDecalTextureGenerator` projects the crack on (the packed normal wins through
  `IForgeVertexConsumer.applyBakedNormals`).
- The block model had one particle sprite per block.
- 4.B drew the crack differently in kind: `RenderGlobal.drawBlockDamageTexture` re-ran each cracking block's
  `RenderBlockWithTexture` with the destroy icon every frame (vanilla/client RenderGlobal.java:1567-1624, the call at
  :1613), so the crack lay on each face with that face's own UVs, and tile-entity renderers never saw it. 1.20.1
  projects a decal over the static model quads instead. Modern-Common's `RenderBlockWithTexture` always drew a
  standard block.

**Fix (general rules).** INTEGRATIONS.md #041, tags `fc_quad_normals` (Parts A and B) and `fc_crack_4b` (Part C):
- **Part A, the quad rule:** `FCBakedModel.convertCapturedQuad(..., worldQuad)` (FCBakedModel.java:1225): a world quad
  (static or live capture) keeps its tessellator normal only if that normal is its own (`planeNormalUnlessOwn` :1300:
  the diagonals' cross product, cos >= 0.9999). Otherwise it is baked as 1.20.1 bakes its own cross, crop and ladder
  faces: `Direction.getNearest` of the plane, the normal Forge's `fillNormal` packs (the direction's axis by default,
  the exact plane normal under the client option `calculateAllNormals`), shade false and no ambient occlusion. Every
  quad whose normal is its own comes out byte for byte as before; item captures keep their normals (1.5.2 lit items by
  them).
- **Part B, particles:** the per-state `FCBakedModel.StateView` (:1568, a `BakedModelWrapper`, built by `forState` and
  registered for every state by BTWClientEvents.java:296-297) answers each meta's `getIcon(0, meta)` as its particle,
  1.5.2's rule.
- **Part C, the crack:** BlockRenderDispatcherCrackMixin (client; a cancellable HEAD inject on Forge's 6-argument
  `renderBreakingTexture`, `remap = false`) and FCBreakingCrack send a block whose model is FCBakedModel or its
  StateView (`FCBakedModel.of` :761) to `renderCrack` (:709). It runs FC's `RenderBlockWithTexture` with a NamedIcon
  "destroy" (`CRACK_ICON` :683) through `runFcRender` over the live world, under the capture lock, and writes only the
  quads drawn with that icon straight into the decal's crumbling buffer (SheetedDecalAccessor), white and full-bright.
  1.20.1's crack is drawn instead only if FC throws (one WARN per block id). `FCBlockEntityRenderer.render` hands FC
  TESRs the buffers without the crack decal (FCBlockEntityRenderer.java:55-56, `FCBreakingCrack.withoutCrumbling`,
  VertexMultiConsumerDoubleAccessor). Modern-Common `Block.RenderBlockWithTexture` is 1.5.2's body verbatim
  (Block.java:1339-1356): a block that is not `renderAsNormalBlock()` runs its own `RenderBlock`.
- Part C follows `renderAsNormalBlock()`, so it landed in the same wave as RANB-PARITY (section 3, "2026-10-26: the
  base classes carry 1.5.2's block answers"): with 1.5.2's answer a free-standing or corner pane cracks on its own
  post quads, not on a full-cell box.
- Comments corrected, no code change: Modern-Common `Block.RenderCrossHatch` and `RenderBlocks.renderCrossedSquares` /
  `renderBlockCrops` (Part A's comment edits compile to byte-identical classes).
- Unchanged: FC code; vanilla blocks keep 1.20.1's crack; the crack images are 1.20.1's `destroy_stage_0..9`, so
  resource packs change them.

**Verification.** Record, wave W1 (2026-10-25/26), run folders fu_runs/R3/W1 and compat/client `*_AB`, `*_ABC`:
- AB build (Parts A and B, R5B, RANB-PARITY and RANB-PLACE): Mixin config OK (55 entries). P-CRACK-C at stage AB:
  `tally_crack` GATE OK (K3 passed; the Part C rows and KG failed, as designed). The model dump now -> AB through
  `dumpcheck_ranb`: GATE OK, after a harness fix (quaddiff compared the dump's lower-case `dir=` with its upper-case
  table). Its INFO lists, for review: 96 PLANE quads with an anti-parallel captured normal, 636 whose face shade
  changes, 674 particle sprite changes (Part B).
- ABC build: Mixin config OK (58 entries); the refmap gains `delegate` f_85867_ and `first` / `second` f_86171_ /
  f_86172_; the crack inject is `remap = false` (Forge's 6-argument overload has no SRG name). P-CRACK-C at stage ABC:
  GATE OK, with K1-K5, KG and KB passed (KB `lit=72 crack=0`: no crack on an open basket's lid). `dumpdiff_c` AB ->
  ABC: 36943 lines, 0 differ (Part C changes no model quad).
- Regression: SET-FULL-S SAME but for P-PLACE's KNOWN row H4, which RANB-PARITY turns to PASS; SET-CRASH SAME;
  SET-CLIENT SAME, wood_c1 differing only by `model=FCBakedModel$StateView` in its blocksprite-detail lines (planned).

**Caveats.**
- The quad test is geometric: a quad drawn with no normal of its own whose plane happens to face the inherited normal
  stays smooth-lit (a flat wet-cement top beside a sloped one), and a correct normal on a reversed winding is turned
  around (none known). No provenance flag in the port's Tessellator (OD6, not now).
- The crack runs FC's render once per frame per cracking BTW block, as 4.B did, under the capture lock: a crack can
  wait while chunks rebuild.
- A block whose model another mod wraps keeps the Part A crack. A future wrapper of FC's model (the queued darker
  deep-strata textures) must be unwrapped by `FCBakedModel.of`, or its blocks fall back to the Part A crack.
- Untested with Embeddium, Oculus or shader packs.
- Found while auditing, not changed: the port's `renderBlockLadder` has no FC user and its meta-to-side mapping
  differs from 1.5.2's; the port's `renderBlockPane` draws an unconnected pane as a post, where 1.5.2 drew a full
  cross.

## 2026-10-26 (bh) FC particles: one name table for every FC world, 4.B's limits, BTW particle types for FC's own FX (CF2-R5b)

**Symptom (triage CF2-R5b).**
- Loose snow, campfires, kilns, cauldrons, crucibles, smouldering logs, mill stones and drying bricks gave off large
  grey campfire smoke, which showed even at Minimal particles; FC's smoke drawn through the other tables hung in place
  and never rose.
- Cauldrons and crucibles on a fire and lit campfires holding food poured out a second, forced stream of grey smoke.
- Candles drew no flame; a bursting smouldering log threw grey ash; a powered detector block showed bright red dust
  drifting sideways; the snow golem's falling snow puffs were snowball crumbs; the overload puff of a breaking device
  showed at Minimal.
- BTW's particles drew up to 32 blocks away; 4.B stopped at 16.
- HEAD: P-R5B-T 19/32, P-R5B-C 16/35.

**Root cause.** FC's display code reached five FC-world sinks through three name tables, plus one emitter the port
added:
- PB (`ProxyBlock.createFcWorld`, FC's `randomDisplayTick`) and PBE (`ProxyBlockEntity.createClientWorld`, FC tile
  entities' client ticks) used `ProxyBlock.mapParticle`. It mapped `fcwhitecloud` and `fcwhitesmoke` to
  `CAMPFIRE_COSY_SMOKE`, which is registered with `overrideLimiter = true`, so 1.20.1's cull and Minimal check never
  applied; it had no `fcsmallflame`, passed the detector dust colour on as motion and drew every `iconcrack_` as a
  snowball.
- CFW (ClientFcWorld: FC's aux-FX and client block notifications) and WB (WorldBridge: server-side FC calls) had their
  own tables: `fccinders` became ash, FC's smoke `CLOUD`, which never rises.
- PE (`ProxyEntity.createDummyClientWorld`, FC entities' client updates) has an empty `spawnParticle`.
- FCBlockEntityRenderer spawned forced `CAMPFIRE_COSY_SMOKE` and `LARGE_SMOKE` every rendered frame over cooking
  vessels on a fire and lit campfires with a cook stack; 4.B had no such emitter.
- 4.B's limits live in `RenderGlobal.doSpawnParticle` (vanilla/client RenderGlobal.java:1841-1896): explosions at any
  distance and setting, `fccinders` within 32 blocks at any setting, every other name within 16 blocks and only as the
  Particles setting allows. 4.B's server never drew or sent a particle.

**Fix (general rules).** INTEGRATIONS.md #042, tag `fc_particles`:
- `FcParticles` (new): one table for every FC world. A 1.5.2 name maps to the 1.20.1 particle that descends from the
  same 1.5.2 FX class; where 1.20.1 has none (FC's four FX classes and 1.5.2's `EntitySnowShovelFX`), to a BTW
  particle type. `limit` (FcParticles.java:137) gives 4.B's class per name (`Limit` :63: ANY, NEAR_32, NEAR_16);
  `addLocal` (:168) draws on a client level only (nothing on a server level, as 1.5.2's WorldManager) and logs one
  WARN per name and exception class; `send` (:196) keeps WB's forwarding to nearby players; `audit` (:210) logs
  `[SELF-AUDIT/particles]` from BridgeSelfAudit.runCommon.
- `FcParticleTypes` (new): `betterthanwolves:white_smoke`, `white_cloud`, `small_flame`, `cinders` (it overrides the
  limiter) and `snow_shovel`, each with a JSON texture list of vanilla sprites under
  assets/betterthanwolves/particles. Client package `btw.forge.client.particle`: `LegacyFxParticle` (1.5.2 EntityFX
  geometry and FC's moveEntity), five particles that port FC's four FX classes and EntitySnowShovelFX line for line
  (white smoke rises 0.004 a tick and collides with blocks), `FcParticleProviders`, and `LegacyParticleLimiter` (4.B's
  limit, then `LevelRenderer.addParticle` with 4.B's force flag) for PB, PBE and CFW.
- PB, PBE, CFW and WB call FcParticles; `ProxyBlock.mapParticle` and ClientFcWorld's `particleFor` are deleted;
  FCBlockEntityRenderer spawns no particles. Registration in BTWForgeMod, providers and the limiter in
  BTWClientEvents.
- No mixin, no FC edit, no reflection on Minecraft members.
- Unchanged: vanilla particles and 1.20.1's limiter for them; PE's empty `spawnParticle` (the (ab) caveat still
  holds).

**Verification.** Record, wave W1 (2026-10-25/26): P-R5B-T 32/32 (server: the table, the limit classes, the forwarded
BTW type over the network) and P-R5B-C 35/35 (client, with screenshots, the Minimal discriminator and 4.B's limits),
both GATE after OK. The regression sets SAME.

**Caveats.**
- FC entities' client-only particles stay dropped by PE: a falling smouldering log or burning netherrack gives no
  smoke while it falls, and villager and witch effects are not drawn. Follow-up FU-PE: decide WB's forwarding for
  entity calls first (otherwise particles double), then route the entity client world through `FcParticles.addLocal`.
- Particles the port forwards from server-side FC code (item breaks, hearts; 4.B's server sent none) follow 1.20.1's
  limiter: 32 blocks.
- The modded-crop weed cue (INTEGRATIONS #026) and FC's digging FX through `EffectRenderer.addEffect` are unchanged
  (1.5.2's addEffect had no limiter either).
- Client and server must run the same build (registry sync of the five types), as already required.

## 2026-10-27 (bi) Falling blocks: FC entities without a (World) constructor, client simulation, landing order, block shading (CF2-R1)

**Symptom (triage CF2-R1; maintainer ruling of 2026-10-22 on OD1: a block saved mid-fall is restored).**
- Every BTW falling block (sand, gravel, loose dirt and cobblestone, BTW's loose slabs and stairs, loose snow, burning
  netherrack, the dragon egg) drew as an orange outline while it fell, moved in 3-tick jumps, and the landed block
  blinked out for about a tick.
- A block saved while it was falling came back as an orange outline frozen in mid-air for good, and the block was
  lost.
- `[SELF-AUDIT/renderer]` passed.
- Found by the W2 proof: a burning netherrack falling below y = 0 fell with metadata 0 and relit no fire where it
  landed.

**Root cause.**
- The client built an FC entity only through a `(World)` or a no-argument constructor. FCEntityFallingBlock has only
  `(World, double, double, double, int, int)` (FCEntityLightningBolt only a positional one too); 4.B built it from
  Packet23 with that constructor. So the client's `fcEntity` stayed null, FCEntityRenderer drew `renderDebugBox`, and
  the lookup was retried every frame. The renderer audit checked only that each class had a renderer.
- On reload the server's lookup failed the same way: FCData was skipped and the inert proxy was saved again without
  state. 4.B also lost such a block (1.5.2 `EntityList.createEntityFromNBT` needs `(World)`: "Skipping Entity with id
  FallingSand").
- Plain FC types are tracked every 3 ticks and the base `lerpTo` snaps; the client did not simulate the fall. FC lands
  in the entity phase and `discard()` sends the removal at once, while the landed block went out with the next tick's
  chunk broadcast.
- Modern-Common `Block.ShouldRenderWhileFalling` was always true; `RenderBlocks.RenderStandardFallingBlock` reset the
  bounds to a full cube (slabs and stairs fell as cubes); `RenderFallingSand` lacked 1.5.2's dragon-egg branch; the
  capture was lit with entity light, darker on the sides than the placed block.
- The netherrack: FC's `FCBlockNetherrackFalling.onStartFalling` finds its row with `(int) entity.posY`; the entity
  starts at j + 0.5, and below y = 0 the cast rounds toward zero, so FC tested the cell two above the block for fire.
  4.B never has y < 0.

**Fix (general rules).** INTEGRATIONS.md #043, tags `fc_entity_construction` and `falling_blocks`:
- **The constructor rule** (`FcEntityFactory`, new; `Kind` WORLD, NO_ARG, POSITIONAL, NONE, FcEntityFactory.java:20):
  `(World)`, else no-argument, else positional with the proxy's position and zeros, then the state. ProxyEntity and
  the three mob proxies use it, each with a no-retry flag.
- **ProxyEntity:** `ensureFcEntity(withState)`. A positional entity is rebuilt on the server only from its saved
  FCData (`REBUILD_SAVED_POSITIONAL = true`, ProxyEntity.java:64, ruling OD1); one without state (a `/summon`, a save
  without FCData, unreadable FCData) is removed at its first tick. The client simulates the fall, as 4.B built the
  entity on the client and `FCEntityFallingBlock.onUpdate` moved it on both sides, and its `lerpTo` / `lerpMotion`
  keep the server updates that fit its own fall. `syncFromFc` sends the landed (or crushed, combined, emptied) cell
  before the removal, as 1.20.1's FallingBlockEntity does.
- **FCEntityRenderer:** the audit checks client construction; `RenderFallingSand` covers the frozen EntityFallingSand;
  a falling-block capture is shaded by block face, like the placed block.
- **Modern-Common 1.5.2 ports:** `Block.ShouldRenderWhileFalling` (Block.java:1439-1448; the entity is hidden while
  its own block still stands in its cell), `RenderBlocks.RenderStandardFallingBlock` (RenderBlocks.java:2568-2577: the
  caller's bounds, its sides, its tint) and `RenderFallingSand`'s dragon-egg branch (RenderFallingSand.java:51-54,
  tested by class).
- **BTWEntityRegistration:** `fc_lightning_bolt` is `noSave` (:183-185; 1.5.2 never saved weather effects).
- **The W2 fix-up, tag `falling_blocks`:** `fcext.FCBlockNetherrackFallingMixin` (new; `@Pseudo`, `remap = false`)
  redirects the first `posY` read of `onStartFalling` to `Math.floor` (FCBlockNetherrackFallingMixin.java:50-55). The
  rule: an FC truncating y cast means floor, its value at every 4.B height (`(int) Math.floor(y) == (int) y` for y >=
  0); `extendFcPathingBuildRange` applies the same rule to the frozen pathing classes ((af)). A javap scan of FC's
  classes found 15 `getfield posY; d2i` sites; this is the only one whose position is always fractional, and the other
  14 stay as FC wrote them.
- The same fix-up landed `block_swap_lift` (INTEGRATIONS #060; WorldBridge.setBlock, WorldBridge.java:1199-1245): an
  entity resting in the gap between FC's 4.B collision box for a cell and its lower 1.20.1 shape (an item on
  minecraft:farmland that FC fertilises into its full-height block 204) is lifted onto the block that fills the gap,
  with the body of 1.20.1 `Block.pushEntitiesUp`, instead of being pushed sideways a random distance. It fills no stub
  and has no entry of its own here.
- Unchanged: FC code; 1.20.1's FallingBlockEntity is not used for BTW falls.

**Verification.** Record, wave W2 (2026-10-26) and its fix-up W2fix (2026-10-26/27):
- P-FALL 18/18 GATE after OK (the fix-up's first run was voided by a host suspend; the rerun P-FALL-r read 18/18).
  P-FALL-RESTART A 2/2 and B 2/2: a block saved mid-fall reloads and lands. P-FALL server.log: `Could not create FC
  entity` 0, `saved while falling` 0, failed reads 0, `with no BTW state` 1, `onUpdate() threw` 0.
- P-FALL-C 16/16 with its K rows: built=7 drawn=7, a slab 0.5 high, the egg's 48 quads, smooth movement (0 stalls; 0
  of 78 server updates applied), 0 blinks, the cow drawn. At W2, K3d failed (burning netherrack `meta=0`, 6 quads);
  after the fix-up, DIAG-K3D 6/6 (the fire relit) and K3d `meta=1 quads=7 height 1.0`.
- P-BM P15a (one bone meal used from the item) had failed about 12 % of runs since the row was written; after
  block_swap_lift it passed three runs out of three with the item at distance 0.0000 (W2: 1.5848).
- Build after the fix-up: Mixin config OK (59), LinkAudit 6, BridgeAudit 0 high. SET-FULL-S, SET-CRASH and SET-CLIENT
  SAME as W1, but for P15a FAIL -> PASS.

**Caveats.**
- A block saved mid-fall is restored and lands, by ruling OD1 (4.B lost it); a block an earlier build saved mid-fall
  lands too. A summoned `betterthanwolves:fc_falling_block` carries no state and vanishes at once (one INFO line).
- Falling smouldering logs and burning netherrack give no smoke on the client (follow-up FU-PE, (bh)).
- FC's 1.5.2 void rule still turns a fall of more than 100 ticks that ends below y = 1 into an item (follow-up
  FU-VOID).
- Falling blocks can be aimed at, as in 1.5.2 and 1.20.1, but the proxy's pick box sits 0.49 above the drawn block
  (the plain-proxy yOffset convention, follow-up FU-YOFF; (bj)).
- No ambient occlusion on a falling block; a burning netherrack's embers are lit by the world light while it falls.
- The client FC world sees no entities, so a block that lands on a boat on the server falls through it on the client
  until the server's update corrects it.
- Not runtime-tested: the falling bucket, lava receiver, smouldering log and loose snow slab renderers.
- block_swap_lift covers block swaps, not metadata-only writes. The other 14 FC `(int) posY` sites are exact on a full
  block and off by one only at a fractional negative y.

## 2026-10-28 (bj) Drops leave through the dug face; FC drops spawn with 1.5.2's box (CF2-DR2)

**Symptom (triage CF2-DR2).** Conversion drops (stone and rough stone under a pickaxe that cannot harvest them, staged
ores, a bare-hand chew of a log, the work stump, grass dug with a hoe) came out of the block above the one being
mined: in a tunnel they went sideways through the rock or up through the ceiling, and on a trunk the bark left a
random side of the log above. Every FC drop also started 0.125 too high, so about one block drop in seven began inside
the block above and was pushed out through the rock. HEAD: P-DR2 9/19, every GAP row failing.

**Root cause.**
- `destroyBlock(BlockPos)` carries no face, so ServerPlayerGameModeMixin passed face 1 (UP) to FC's `tryHarvestBlock`
  for every break. 1.5.2 kept the dig packet's face (`m_iHarvestingFromFacing`), also for its delayed destroy
  (vanilla/server ItemInWorldManager.java:95-125, :229-240).
- `EntityProxyFactory.createItemEntity` used FC's `posY`, 1.5.2's box centre, as 1.20.1's feet.

**Fix (general rules).** INTEGRATIONS.md #044, tags `fc_dig_face` and `fc_drop_position`:
- **The dig face:** ServerPlayerGameModeMixin implements `DigFaceAccess`. Four injects
  (ServerPlayerGameModeMixin.java:157-204): HEAD and RETURN of `handleBlockBreakAction` (`m_214168_`) record the START
  or STOP packet's face for the cell it digs while the call runs, and keep a STOP's face when that call scheduled the
  delayed destroy; INVOKE and INVOKE-AFTER around `tick`'s (`m_7712_`) one `destroyBlock` call hand that face to the
  delayed break. `btw$digBegin` clears a stale delayed face when vanilla holds no delayed destroy. `btw$destroyBlock`
  reads `DigFace.of` (DigFace.java:41) before the BreakEvent and passes it to `tryHarvestBlock`
  (ServerPlayerGameModeMixin.java:234).
- **`DigFace`** (new): the dig packet's face, else the face the breaker's look ray hits, else UP. `FCLogRules.hitFace`
  (FCLogRules.java:529) delegates to it, so the log rule (#034) and Dynamic Trees (#027) chew toward the same face.
- **The drop box:** `createItemEntity` (EntityProxyFactory.java:229, :244-249) spawns an FC drop with feet = `posY -
  yOffset` (the ItemEntityBridge convention), except that a drop whose 1.5.2 box collides while the box at `posY` does
  not keeps `posY` (entity drops centred on a floor). `applyPositionFromFc` is unchanged: proxies sync `posY` with
  their feet both ways.
- Self-audit `[SELF-AUDIT/digface]` (`DigFace.audit` :72, from BridgeSelfAudit.runCommon, BridgeSelfAudit.java:48).
- Refmap facts: `handleBlockBreakAction` -> `m_214168_`, `tick` -> `m_7712_`; the shadows `hasDelayedDestroy` and
  `delayedDestroyPos` are reobfuscated in the class itself to `f_9253_` / `f_9254_` (shadows are never in the refmap).
  On a Forge bump, re-check with javap that `tick` still has exactly one `destroyBlock` call. A listener that calls
  `handleBlockBreakAction` from inside one overwrites the record, and the outer harvest falls back to the look ray
  (javadoc).
- Unchanged: FC code; 1.20.1's dig protocol, reach and protection checks, sequence acks and item physics.

**Verification.** Record, wave W3 (2026-10-27/28): P-DR2 19/19 and P-DR2-C 2/2 (client), both GATE after OK;
`[SELF-AUDIT/digface] ServerPlayerGameMode carries the dig face`. SET-FULL-S, SET-CRASH and SET-CLIENT: 0 checks
differ. At W6 one batch run read 18/19: row B4 counts every dropped item in every level while armed, and two stray
item records near y 100 joined its window; the rerun read 19/19 (300 harvests under a ceiling, 0 above y+1). The
plan's B4 report could bound its records to the cell's neighbourhood.

**Caveats.**
- 1.20.1 still pushes an item out of any block it overlaps, through rock, and items float as in 1.20.1 (BTW's Hardcore
  Buoy option has no effect).
- FC plain entities with a non-zero yOffset (dynamite, mining charges, floating items, XP orbs, primed TNT, falling
  blocks, platforms, anchors, windmills, the frozen boat and minecart classes) keep a 1.20.1 box yOffset above FC's
  (follow-up FU-YOFF, to be designed on (bi)'s sync code).
- Other mods' machines and fake players that break BTW stone, ores or logs get the conversion drops on the side their
  look ray hits, else on top.
- Wet sponge, the stone slab and smooth sandstone drop through `Block.popResource` with 1.20.1's offset, not FC's box
  ((bk)).
- Not runtime-tested: staged ores, grass under a hoe, the work stump, a chewed log's later stages (the same eject call
  as the rows proven).

## 2026-10-28 (bk) Flowers keep their kind; FC self-drops and BTW storage keep the block's identity (CF2-DR3, CF2-P8c)

**Symptom (triage CF2-DR3, CF2-P8c; maintainer rulings: flowers keep their kind (default of 2026-10-21); post-1.5.2
plants take their nearest 1.5.2 relative's rules (2026-10-15); subtyped items without durability stack in BTW
containers (2026-10-20)).**
- Blue orchids, alliums, azure bluets, tulips and oxeye daisies turned into poppies when broken, when they popped
  (soil dug, too dark), when FC placed them, and inside BTW storage. Their dyes and the flower banner pattern did not
  craft at BTW grids, every flower ground into red dye at the millstone, and the red tulip's dye recipe was removed as
  if the tulip were the rose.
- FC's drops lost the subtype of wool, slabs (double slabs too), sandstone, walls, quartz and mob heads; wet sponge,
  the stone slab and smooth sandstone dropped their id's base block; infested blocks dropped themselves.
- In BTW storage a coloured wool, spruce slab or spruce plank never stacked, and a white wool, oak slab or skeleton
  skull merged into the first coloured, typed or wither stack; FC's log trades took any log.
- HEAD: P-FLOWERS 41/107.

**Root cause.**
- The flowers mapped to legacy 38 with no subtype, so every trip through FC made them 38:0, the rose (the poppy).
- Modern-Common's 1.5.2 base classes lack `damageDropped`, so FC's self-drop of a block 1.20.1 split from one legacy
  id was the id's base form; FC's harvest removes the block before it drops, so nothing at the cell remembered which
  form it was.
- The port's plain `ItemBlock`, the `ItemMultiTextureTile` stub and the skull item lacked 1.5.2's subtype flag, so
  FC's merge and the log trades compared no damage ((bc)'s stacking rule needs the flag).
- Modern-Common BlockSilverfish lacked 1.5.2's `quantityDropped` 0.

**Fix (general rules).** INTEGRATIONS.md #045, tags `flower_subtypes`, `item_subtypes` and `collapsed_identity`:
- **Flower subtypes:** the ten small flowers are port-only subtypes of the rose, 38:1-10, in 1.7.2's red_flower order,
  then 9 cornflower and 10 lily of the valley (ProxyRegistry.java:227, :682; LegacyVariants.java:192;
  `ItemBlock.getMetadata` case 38, ItemBlock.java:157). The cornflower and the lily of the valley take the rose's
  rules (lore ruling); the wither rose and the torchflower stay unmapped.
- **The subtype flag:** `ItemBlock.getHasSubtypes` for the plain block items 5, 6, 17, 18, 24, 31, 35, 38, 43, 44, 97,
  98, 125, 126, 139 and 155 (ItemBlock.java:181), the `ItemMultiTextureTile` constructor flag
  (ItemMultiTextureTile.java:7) and the skull item (Item.java:924), as 1.5.2 declares them.
- **Collapsed identity:** Modern-Common `Block.dropBlockAsItem_do` asks the new World hook `ResolveModernSelfDrop`
  (Block.java:688-690; World.java:919). WorldBridge's override (WorldBridge.java:3360) hands it to `CollapsedIdentity`
  (new): an FC self-drop of a block whose 1.20.1 form shares its legacy id takes the identity of the 1.20.1 block at
  the cell, live or just harvested; the per-thread harvest record is opened in
  `ServerPlayerGameModeMixin.btw$destroyBlock` around `tryHarvestBlock` (:277), because the harvest has emptied the
  cell. FC keeps every drop decision (whether, how many, silk touch, tools); `exempt` lists the blocks whose FC drop
  wins.
- **BlockSilverfish.quantityDropped** is 0 (Modern-Common BlockSilverfish.java:9-12): infested blocks drop nothing, as
  in 1.5.2 and 1.20.1.
- Self-audit `[SELF-AUDIT/identity]` (`CollapsedIdentity.audit`, from BridgeSelfAudit.runServerStart,
  BridgeSelfAudit.java:72) WARNs on W1 (a flower not dropped as its subtype), W2 (ProxyRegistry and LegacyVariants
  disagree on a flower) and W3 (a member whose FC item lacks the subtype flag).
- Decisions: D1 (b), the red tulip makes red dye at a crafting grid (the #038 precedent); D3 (a), the rule covers
  every collapsed self-drop (about 60 blocks).
- Unchanged: FC code; the blocks, items, loot and recipes stay 1.20.1's.

**Verification.** Record, wave W3 (2026-10-27/28): P-FLOWERS 107/107 GATE after OK, and again at W5 and W7.
`[SELF-AUDIT/identity] 51 blocks keep their identity ... 0 warnings (0 expected)`, with the ten flower lines (FC
38:1..10); the block mapping reads 267 entries and the hook audit 244 blocks. SET-FULL-S: 0 checks differ in all 31
plans; the intended OUT changes: the P-RV census `sweep ns minecraft` total 708 -> 709 and fail 27 -> 19, and P-RM's
red-dye recipes list `red_dye_from_tulip`.

**Caveats.**
- Wet sponge, carved pumpkins, the stone slab, smooth sandstone and coloured beds still turn into their 1.5.2
  equivalent inside BTW storage and when BTW places them (follow-up F-COLLAPSE-ITEMS). Drops with no BTW form use
  1.20.1's drop offset.
- Still without 1.5.2's subtype flag: the `ItemBlockWithMetadata` stub (stone, cobblestone, ores, snow), coal, spawn
  eggs, potions, filled maps, FCItemBlockDeadWeight and BTW's anvil item (follow-up F-SUBTYPES).
- The Modern-Common drop-parity follow-up (D7): the `FCBlockSilverfish quantityDropped` row is done here. New row:
  1.5.2's base `Block.canSilkHarvest()` is `renderAsNormalBlock() && !isBlockContainer`, the port's answers false
  (Block.java:386), so silk touch reaches only the blocks whose BTW class allows it.
- A player head drops without its owner. An FC enderman's carried flower draws as a poppy. Data-pack millstone tag
  rules reach only items with no BTW id, so not the cornflower or the lily of the valley. The wither rose and the
  torchflower keep 1.20.1's rules.
- World-generated cornflowers and lilies of the valley now pop where neither light 8 nor the sky reaches them, as the
  other small flowers already did.
- Flowers placed before the change keep their 1.20.1 block; items already collapsed into 38:0 stay poppies; mixed
  stacks already in BTW storage stay mixed.

## 2026-10-28 (bl) FC mobs and players: creative and spectator targets, the live player list, 4.B's player lookup, squids and riders (CF2-M7, CF2-MX1-MX3)

This entry covers INTEGRATIONS.md #046 (tag `creative_targets`) and #047 (tag `head_crab`); FcRiding's javadoc and the
FCEnvHurtGuard comment cite (bl).

**Symptom (maintainer rulings of 2026-10-21: BTW mobs never take or keep a creative or spectator player as a target,
by any path; of 2026-10-22 on D1: only targeting changes, 4.B's other creative-player effects stay).**
- A BTW mob kept chasing a player who switched to creative or spectator; a creative player's hit (old-AI mobs),
  enderman and pigman group anger, a startled wild wolf or wolf meat gave mobs that player as a target; ranged AI
  tasks, the FC ghast and the wither's heads kept their own copies of the target (CF2-M7).
- Squids never hunted or head-crabbed players, and mill-stone nausea and beacon effects never reached a player
  (CF2-MX1); sneaking and invisibility changed nothing, and mobs went after a player on the death screen (CF2-MX2).
- A squid that latched onto an animal or villager hung where it latched and kept biting; the animal stayed
  head-crabbed (no milk, no eggs) after the squid died; FC spider jockeys did not ride (CF2-MX3).
- Once squids could reach players, four of 4.B's squid rules met port gaps: the bite's armour bypass, the tentacle
  fling, the knock-off from vehicles and the disarm.

**Root cause.**
- 4.B checked a player only when a mob chose a target (`EntityAITarget.isSuitableTarget`); nothing re-checked a kept
  target, and several paths set one with no filter.
- Modern-Common `World.playerEntities` was declared and never filled; its only readers are FC classes (FCEntitySquid,
  FCTileEntityBeacon, FCTileEntityMillStone).
- `WorldBridge.getClosestVulnerablePlayer` lacked 1.5.2's `disableDamage`, alive, sneak and invisibility tests
  (vanilla/client World.java:3968-4008).
- The Modern-Common `EntityPlayer` lacked 4.B's squid hooks (`GetCanBeHeadCrabbed`,
  `IsValidOngoingAttackTargetForSquid`, `IsImmuneToHeadCrabDamage`). Nothing ran an FC rider's ridden update (1.5.2
  `World.updateEntityWithOptionalForce` -> `updateRidden`) or cleared stale riding links. The bite's unblockable flag
  was lost in the damage mapping; the fling and the cow kick wrote only the FC view's motion; the knock-off read the
  FC view's riding field, which the port never sets; the disarm read FC's use state, which the port never clears.

**Fix (general rules).**
- **The target rule** (`FcTargetRule`, new): `isTargetable` is 1.20.1's `Player.canBeSeenAsEnemy`
  (FcTargetRule.java:29). `sweep` runs first in `FCEnvHurtGuard.runOnUpdate` for every FC mob each server tick
  (FCEnvHurtGuard.java:84) and clears attackTarget, entityToAttack (with the pig zombie's anger and grunt delay),
  wither heads 1 and 2, the FC ghast's private target and a head crab on such a player, then resets the AI tasks still
  holding the player (`EntityAITasks.resetExecutingTasks`, Modern-Common). Only a living protected player in the world
  (`isProtectedLivingPlayer` :47) costs a pigman its anger or a squid its head crab, so a dead or departed survival
  player keeps 4.B's reactions. Self-audit `[SELF-AUDIT/targets]` (`audit` :166).
- **The player list:** WorldBridge's constructor gives `playerEntities` a live view of the level's players
  (`PlayerEntitiesView`, WorldBridge.java:107-108, :2874).
- **The lookup:** `getClosestVulnerablePlayer(ToEntity)` is 4.B's with `isTargetable` and 1.20.1's
  `getVisibilityPercent` (sneaking x0.8, invisibility x0.7 x max(armour cover, 0.1), plus Forge's visibility hook)
  (`closestVulnerablePlayer`, :2936-2942).
- **Riders** (`FcRiding`, new; `update`, FcRiding.java:21): 1.5.2's per-entity update. A rider runs `updateRidden` so
  its mount carries it, stale links are cleared on both sides, and a head-crabbed player is knocked out of a 1.20.1
  vehicle.
- **Squids and players** (#047): Modern-Common EntityPlayer gets 4.B's three squid hooks, and `isWeighted` counts a
  squid on the head. Data: damage type `betterthanwolves:mob_attack_unblockable` and its entry in
  `#minecraft:bypasses_armor`, chosen by `DamageSourceMapping`'s "mob" case for an FC hit flagged unblockable
  (DamageSourceMapping.java:70), with `[SELF-AUDIT/damage]`. PlayerBridge: `OnFlungBySquidTentacle` and
  `OnKickedByCow` put FC's motion on the real player with `hurtMarked`, after `stopRiding()` (PlayerBridge.java:298,
  :316, :337), and never move a spectator; `isBlocking` is the real player's use of a BTW block-action item in the
  main hand (:1085); the head-crab latch asks the real player's vehicle.
- Compile-only stubs, because the frozen classes ship: `EntityPigZombie.angerLevel` / `randomSoundDelay`,
  `EntityWither.func_82211_c`.
- Unchanged: FC code; vanilla mobs keep 1.20.1's own goals.

**Verification.** Record, wave W4 (2026-10-28): P-MOBS 65/65 and P-MOBS-C 11/11, GATE after OK; again at W8. Logs:
`[SELF-AUDIT/targets] ... FCEntityGhast.m_entityTargeted ...` and `[SELF-AUDIT/damage]
betterthanwolves:mob_attack_unblockable loaded, in minecraft:bypasses_armor`; no `Target rule failed` or
`[AI-TASK-FAIL] resetTask`. SET-FULL-S (with the hunger contract `food_core`): 0 checks differ. SET-CLIENT SAME after
two batch runs were voided and rerun (probably an out-of-plan hotbar change; the reruns read 30/30 and 2/2).

**Caveats.**
- By ruling D1 (a), a creative player's other effects stay as in 4.B: a wild wolf hit or fed wolf meat turns hostile
  (toward survival players), animals panic, endermen scream, silverfish call allies.
- A head-crab squid is drawn as a free squid at its server position (no 1.20.1 mount); riders trail their mount by up
  to a tick (proxy order).
- A squid never knocks a 1.20.1 shield out of a hand: it disarms only BTW swords and tools held blocking, as 4.B did.
  The thrown copy of a disarmed item keeps FC's fields only.
- The bridge's own FC use fields are still never cleared (`isBlocking` no longer reads them; follow-up D8); the client
  half of the swim gate is unchanged (D9).
- Spectators are still in FC's entity box queries: a tentacle tip still collides with a spectator's bridge, without
  moving it.

## 2026-10-28 (bm) Zombified villages, no village golems, no zombie sieges, and the soul-urn cure (CF2-MU3)

**Symptom (maintainer rulings of 2026-10-21: 4.B zombified villages, abandonment by distance from the spawn, creative
worlds keep villagers, the cure proven end to end first; of 2026-10-22: no iron golem in any new village (D1 a1); of
2026-10-23: no zombie siege, as 4.B).**
- Every village villager was a plain BTW villager with a random profession, so trading needed no cure, and 1.20.1
  villages kept their iron golem.
- The cure was broken: a cured villager zombie made by VanillaMobReplacer or loaded from disk turned into a new
  villager every tick and never went away.
- 1.20.1's siege could still come at midnight to a village whose POI a Minecraft villager had taken (from play before
  the port, or another mod's villagers), never to a BTW village by itself.
- HEAD: P-VCURE 16/19, P-VSIEGE 14/16.

**Root cause.**
- No village rule: 4.B spawns every village villager as a persistent BTW villager zombie of its building's class and
  empties villages near the spawn (vanilla/server ComponentVillage.java:121-204,
  ComponentVillageStartPiece.java:89-120, with FC's radii in FCUtilsHardcoreSpawn); the port made every STRUCTURE
  villager an FC villager with FC's random profession.
- `WorldBridge.removeEntity` neither set FC's isDead nor found puppets.
- 4.B removed golem spawning (vanilla/server Village.java:80-97) and the siege object and its tick
  (World.java:105-107, WorldServer.java:183-185): 1.5.2's 10-door, 20-villager gate was dead code in 4.B.

**Fix (general rules).** INTEGRATIONS.md #048, tags `zombified_villages` and `village_sieges`; the creative-world
helper is #054's `creative_world`, shared with (br):
- `VillageInhabitants` (new; VillageInhabitants.java:48), called from `VanillaMobReplacer.replaceWithProxy` before the
  proxy type is picked (VanillaMobReplacer.java:380). The village is found by
  `StructureManager.getStructureWithPieceAt(pos, #minecraft:village)` or, for a live placement (`/place`), by the
  StructureStart that `StructureStartPlacementMixin` (new; two injects, `m_226850_`) publishes during
  `StructureStart.placeInChunk`. Its level comes from the start piece's centre against the world spawn, with FC's own
  radii read by name (2250 and 3000, x4 on large biomes). Building = the largest piece at the villager; class = the
  villager's own profession, else the building's nearest job-site block, else farmer. 4.B's 1-in-20 per building and
  its class filter apply. The result is a persistent villager zombie with its class and no FC init, unless the world
  is creative (`WorldInfoBridge.isCreativeWorld`: its default game type); baby villagers become baby villager zombies.
- New villages discard their 1.20.1 iron golem (VanillaMobReplacer.java:178).
- `WorldBridge.removeEntity` (WorldBridge.java:2069) is 1.5.2's: unmount, setDead, discard through `proxyFor`, so a
  cure ends with one villager.
- `WorldInfoBridge.getTerrainType` (WorldInfoBridge.java:86) answers LARGE_BIOMES for the Large Biomes preset: the
  single source of FC's terrain type, shared with (br).
- **No siege:** `VillageSiegeMixin` (new) returns 0 at the HEAD of `VillageSiege.tick(ServerLevel, boolean, boolean)`
  (`m_7995_`, VillageSiegeMixin.java:54-55): no midnight roll, no VillageSiegeEvent, no zombies, in any village, game
  mode or dimension. BTW's village bookkeeping keeps ticking in `WorldBridge.ModSpecificTick`; raids, patrols, cats
  and the wandering trader keep 1.20.1's spawners.
- Unchanged: FC code; 1.20.1's villages, buildings and job blocks.

**Verification.** Record, wave W4 (2026-10-28), all GATE after OK: P-VCURE 19/19, P-VPLACE 23/23, P-VSIEGE 16/16,
P-VNAT 11/11, P-VLB 5/5; P-VCURE, P-VPLACE and P-VLB again at W9, where P-VPLACE's block K proves the shared
creative-world helper. Refmap: StructureStartPlacementMixin `m_226850_`, VillageSiegeMixin `m_7995_`. Logs:
`[VILLAGES] new villages` with terrain multiplier 1.0 (4.0 in P-VLB), none of the forbidden WARNs, no `Failed to
create zombie` in P-VSIEGE. P-HCR-LB's world-type row passes from W4 on, through this getter. SET-FULL-S: 0 checks
differ.

**Caveats.**
- Villages generated before this version keep their villagers; a village straddling land explored before the update
  can be partly zombified; a village whose start chunk unloads mid-generation can give a level-2 building a second
  farmer.
- On Peaceful, villager zombies vanish like every BTW monster, as in 4.B. In creative worlds, 1.20.1's rare zombie
  villages still hold villager zombies (D8).
- Igloo basement zombie villagers stay plain zombies (a separate ruling, D6).
- Raids, village cats, golem strolling and fox avoidance never see BTW villages (follow-up O-S4); door facing and
  non-oak doors for BTW's villager AI stay follow-ups (O-S2, O-S3).
- A mod that relies on 1.20.1 sieges or on `VillageSiegeEvent` sees none, by the ruling.
- Proof servers run with spawn-monsters off, so only P-VSIEGE's direct `vsiege run` rows drive a siege tick.

## 2026-10-28 (bn) Explosions follow FC's per-block blast rules; mining charges drop their pieces (CF2-DR4, CF2-DR5)

**Symptom (triage CF2-DR4, CF2-DR5; maintainer ruling of 2026-10-22 on D1: (C), the 1.20.1 decay gamerules decide for
BTW blocks too, and new worlds start with `tntExplosionDropDecay` true).**
- Every blast dropped 1.20.1 loot and ran 1.20.1's reactions: stone gave cobblestone, dirt and grass vanilla dirt, BTW
  blocks lost their subtype and ignored `canDropFromExplosion`, and none of FC's 15 blast reactions ran (no loosened
  dirt, no sawdust; a blasted mining charge did not go off, a smouldering log did not flare).
- The rays read FC's position-less resistance, so strata stone and ores resisted like surface stone.
- FC's mining charge dropped nothing, and blocks with only a hardness (dirt, grass, sand, gravel, water, lava)
  resisted with 0, so a mining charge deleted water and lava.
- HEAD: P-BLAST 34/79.

**Root cause.**
- 1.20.1's `Explosion.finalizeExplosion` never reaches FC's 1.5.2 per-block body (Explosion.doExplosionB); HookPolicy
  left RESIST UNDECIDED and BlockMixin answered `getExplosionResistance(null)`.
- Modern-Common base logic was missing: `Block.DropItemsOnDestroyedByExplosion` was an empty stub (1.5.2:
  vanilla/server Block.java:1727-1748), the dirt-loosening trio was empty, and `setHardness` lacked 1.5.2's resistance
  floor (vanilla/server Block.java:575-585).

**Fix (general rules).** INTEGRATIONS.md #049, tag `fc_blast_rules`:
- `BlastRules` (new): `decide` (BlastRules.java:314) sorts each affected state by rules 0-8 into a `Rule` (:63): FC,
  NO_IDENTITY, REMOVED, PAIR, BLOCK_ENTITY, COLLAPSED, PENDING or NO_FC_RULE. ExplosionMixin's HEAD of
  `finalizeExplosion` (`m_46075_`) runs 1.5.2 doExplosionB's per-block body for every FC-owned cell in toBlow order
  and leaves the cell air for 1.20.1's own loop (`beforeFinalize` :115); FC's drops are merged as 1.20.1 merges its
  own, 16 per stack (WorldBridge holds the blast's item entities during the pass); FC's queued secondary explosions
  run at the TAIL (`afterFinalize` :153). Server thread only; a blast that keeps blocks never reaches FC. FC's drop
  chance is 1 / radius when the blast decays, else 1.
- **Positional resistance (decision O1: this unit owns it):** ExplosionDamageCalculatorMixin (HEAD of
  `getBlockExplosionResistance`, `m_6617_`, cancellable) answers 1.20.1's rays with FC's positional value for a block
  whose FC class reads its position (`positionalResistance` :265); HookPolicy decides RESIST: FC for such a class,
  else NOTIFY; ProxyBlocks go through a per-block cache.
- **Modern-Common 1.5.2 base logic:** `Block.setHardness` raises the resistance to at least hardness x 5
  (Block.java:273-274); `DropItemsOnDestroyedByExplosion` has 1.5.2's body for every block, a throw logged once per
  class (:1579-1580); `OnDirtDugWithImproperTool`, `OnDirtSlabDugWithImproperTool` and
  `NotifyNeighborDirtDugWithImproperTool` (:1604-1632); `BlockTNT.canDropFromExplosion`;
  `BlockStairs.onBlockDestroyedByExplosion`; `Explosion.doExplosionB` in 1.5.2's order. `ProxyBlock.getDrops` and
  `ModernBlockStandIn.DropItemsOnDestroyedByExplosion` give FC's rules to the 1.20.1 loot paths that roll a BTW block.
- **D1 = C:** `WorldGamerulesHandler` sets `tntExplosionDropDecay` true in new worlds only
  (WorldGamerulesHandler.java:63).
- **PENDING:** TNT (46) keeps 1.20.1's blast, because FCBlockPowderKeg primes Modern-Common `EntityTNTPrimed`, a stub
  (no position, fuse 0, no tick).
- Self-audit `[SELF-AUDIT/blast]` (`audit` :363); the hook audit gains 8 FC-owned pairs.
- Unchanged: FC code; 1.20.1's explosion, entity damage, knockback, fire, sounds and packet.

**Verification.** Record, wave W5 (2026-10-28): P-BLAST 79/79 GATE after OK on a fresh world, and again at W6, W7 and
W8. `[SELF-AUDIT/blast] 418 FC-identity blocks ... pending [46]; positional resistance on 8 blocks of ids [1, 14, 15,
16, 21, 56, 73, 129] and 0 BTW blocks; 0 warnings (0 expected)` (16 positional blocks once (bo) maps the deepslate
forms); hook audit 244 blocks, 13 FC-owned (+8); a new world logs `tntExplosionDropDecay = true`. Refmap:
ExplosionMixin `m_46075_`, ExplosionDamageCalculatorMixin `m_6617_`. The must-watch plans (P-BEDDOOR, P-HOOK,
P-LEAF-V, P-LEAF-DT, mlog_p1, wood_p1, P-RM, P-USE, P-PLACE): 0 checks differ; P-FLOWERS 107/107.

**Caveats.**
- TNT set off by another explosion goes off as 1.20.1 TNT until BTW's primed TNT is ported; a mining charge that
  reaches a powder keg stops (FC's keg primes the stub and throws).
- Blocks with a 1.20.1 block entity, beds, doors and collapsed variants keep 1.20.1's blast (D6). Other mods' loot
  modifiers do not see blasts of BTW-ruled blocks.
- Blasted infested stone still spawns silverfish, and a player's blast of nether quartz ore still gives XP, as in
  1.20.1: FCBlockSilverfish and FCBlockNetherQuartzOre declare no blast or drop method in FC code, and rule 7 of
  `decide` reads only those, so their blast stays 1.20.1's (D7). The Modern-Common `BlockSilverfish` ((bk)) and
  `BlockOre` ((bo)) ports do not change that.
- An attachment 1.20.1 owns (torch, rail, redstone, carpet, flower, crop) on a support BTW owns always pops at 100 %
  instead of rolling the blast's decay (FC's cells go first).
- Existing worlds keep `tntExplosionDropDecay` false until a player sets it.

## 2026-10-28 (bo) Stone strata at 4.B's heights, deepslate as BTW deep stone, raw ore as BTW ore chunks (CF2-PR-DEEP, CF2-PR-ORE)

**Symptom (maintainer rulings of 2026-10-21: 4.B strata at 4.B heights; deepslate, cobbled deepslate and the deepslate
ores become BTW deep-strata stone and BTW ores; raw iron and raw gold are BTW ore chunks, their ingot smelting goes,
and raw-ore blocks unpack into 9 chunks; of 2026-10-22: mined deepslate gives loose cobblestone and the cobbled
deepslate -> deepslate smelting goes (OD1 a); nether gold ore loses its two smelting recipes (OD3 b); depth decides
the hardness of every stone below y=48, placed or generated (OD5 a)).**
- Every stone and ore read strata 0 and dug like surface stone at any depth, and 1.20.1 explosions used the surface
  resistance.
- Deepslate, cobbled deepslate and the seven deepslate ores had no FC identity: they dug, dropped and resisted blasts
  as in 1.20.1.
- Raw iron and raw gold had no FC form, so a furnace or blast furnace turned them into ingots, skipping BTW's
  chunk-to-nugget step.
- Fortune never applied to BTW's ores, and nether quartz ore dropped itself.
- HEAD: 55 of 153 P-STRATA checks passed.

**Root cause.**
- 4.B wrote strata only at world generation, which the port does not run, so FC's stone and ores read metadata strata
  0 everywhere.
- The deepslate forms mapped to no legacy id; raw ore had no legacy form and no FC recipe; the minecraft: cooking
  recipes of raw ore and the deepslate ores survived FcRecipeRemovals, which removed a cooking recipe only when FC's
  furnace had none for its input.
- Modern-Common `BlockOre` lacked 1.5.2's drop methods.

**Fix (general rules).** INTEGRATIONS.md #050, tags `strata` and `raw_ore_chunks`:
- `StrataRule` (new): the strata blocks are id 1 and the FC blocks whose `HasStrata()` conversion is the level
  (`isStrataBlock`, StrataRule.java:90). The strata is deep for a deepslate form, top outside the Overworld, else
  4.B's height rule (y <= 48 mid, y <= 24 deep) with a position-hashed rand(2) edge, per column for stone and per
  block for ores (`strataAt` :52). The position owns the strata bits in WorldBridge's `getBlockMetadata`,
  `putMetaOverride` and `setBlockMetadata`; a strata block's overlay entry survives a strata change
  (`FcMetaOverlay.pruneLoaded` too); `applyFcMetadataToVanillaBlock` leaves a strata block's state alone. The client
  predicts the same strata, so a dig looks as slow as it is.
- **Deepslate:** ProxyRegistry maps the nine deepslate forms to 1, 4, 14, 15, 16, 21, 56, 73 and 129, after the stone
  forms; `getBlockFormItemId` keeps their items 1.20.1's (world-only identity; read by `getOrCreateLegacyId`,
  `hasRealLegacyId`, `ProxyItem.fcItemFor` and `ModernCookingImport.isMapped`).
- **Raw ore:** LegacyVariants counts raw iron and raw gold as `fcItemChunkIronOre` / `fcItemChunkGoldOre` (resolved by
  field name, never aliases: `RecipeBridge.buildLegacyAliases` skips them), and FC's chunk smelting also takes them.
  Data: `btw_recipe_replacements/raw_ores/` unpacks a raw-ore block into 9 chunks and
  `btw_recipe_replacements/nether_gold_ore/` removes nether gold ore's two cooking recipes. FcRecipeRemovals pass 3
  also removes a minecraft: cooking recipe when FC's furnace cooks its input into something else.
- **Modern-Common `BlockOre`:** 1.5.2's drop methods (Fortune; nether quartz ore drops quartz; no XP, as 4.B removed
  block XP). The base `HasStrata` / `GetMetadataConversionForStrataLevel` stay defaults; FC's overrides drive
  StrataRule's classification.
- The blast resistance by position is (bn)'s hook (O1); StrataRule's audit checks that it is on. Network protocol "6"
  (BTWNetwork.java:45), one bump for the release.
- Self-audit `[SELF-AUDIT/strata]`.
- Unchanged: FC code; decorative deepslate, copper and the raw-ore blocks stay 1.20.1's.

**Verification.** Record, wave W6 (2026-10-28): P-STRATA 153 checks, 152 PASS and E16[OD3a] FAIL by design (ruling OD3
(b) removes that recipe): GATE after OK; again at W7. P-STRATA-C: the client's dig progress equals the server's in all
six cells. `[SELF-AUDIT/strata] mapped strata ids [1, 14, 15, 16, 21, 56, 73, 129]; 9 deepslate forms (8 deep, cobbled
deepslate -> 4); raw_iron -> 22571, raw_gold -> 22574; blast resistance by position on; 0 warnings (0 expected)`; the
blast audit lists 16 positional blocks; `Recipe replacements: 2 replaced, 2 removed`; `112 crafting + 2 special + 39
cooking recipes removed`; no deepslate stand-in line. Lean regression (lso_p1, P-SMOKE1, P-SMOKE2, P-RM, P-RV): 0
checks differ; P-RM's OUT lists no deepslate or raw-ore cooking recipe, and P-RV counts raw iron and raw gold smelting
as FC's chunk recipes. At W9, P-USE's rows U10 and U13 needed a diamond pickaxe: the stone at y -60 is deep strata,
and FC sticks a tool only where it is efficient (4.B behaviour; the plan was fixed).

**Caveats.**
- Stone looks the same at every depth; 4.B drew deeper stone and ores darker (a follow-up, queued by ruling; it must
  wrap (bg)'s StateView and be unwrapped by `FCBakedModel.of`).
- Player-placed stone and stone FC makes (cement, lava on loose cobble) take their position's strata, by ruling OD5
  (a); in 4.B they were strata 0.
- Granite, diorite, andesite, tuff and calcite stay 1.20.1 stone (ruled): soft pockets inside the deep layers.
- Other mods' recipes can still turn raw ore and deepslate ores into ingots (only minecraft: recipes are checked). FC
  mobs' footsteps on deepslate sound like stone. Silk Touch gives BTW's normal drops (1.5.2's general
  `canSilkHarvest`, (bk)). Infested stone has no strata.
- Mining deepslate never gives cobbled deepslate, so polished deepslate, deepslate bricks and tiles come only from
  structures.

## 2026-10-28 (bp) BTW wood sub-blocks for wood types 5-8 (FU-W1), the bamboo plank rule, FC's exact match in the 2x2 grid

This entry covers INTEGRATIONS.md #051 (tag `modern_wood_subblocks`) and #052 (tag `bamboo_planks`), and the in-place
update of #025 (tag `recipes`) for the exact match.

**Symptom (maintainer rulings of 2026-10-20: FU-W1, the four newer woods get BTW's decorative wood blocks through
runtime patches; of 2026-10-21: bamboo takes BTW's plank rule, no grid recipe, no bark, no work stump; of 2026-10-22:
the bamboo yields and harvest of decision 7.1 (a), and no 6-plank BTW fence for the four woods, 7.2 (b)).**
- Acacia, dark oak, mangrove and cherry planks had FC identity at item level only ((ay)): FC's planks rule and saw
  never reached them, and every FC site that turns a wood type into a block, name or texture answered blood wood or
  oak for 5-8 (the five stub items' placement and names, the two sub-block classes' wood type, three item renderers,
  FC's sub-block recipe loop over 0-4). Their 1.20.1 stairs and slabs have no legacy id, so 4.B's stair, slab and saw
  rules missed them.
- A bamboo block made 2 planks in the 2x2 grid with no tool, a plank source outside BTW's saw-or-axe gate.
- In the 2x2 grid FC's recipes matched by item only (R11 of (bc)): two sidings of any wood made spruce planks, and
  colour recipes took any colour.

**Root cause.** FC's loops and stub items are wired to wood types 0-4 (FC code is read-only); fc_recipe ingredients
test the item only, and the 1.20.1 grids never asked FC's own match.

**Fix (general rules).**
- `ModernWoodSubBlocks` (new; BTWLifecycle step 5f): FC's own `FCBlockWoodSidingAndCornerAndDecorative` and
  `FCBlockWoodMouldingAndDecorative` with FC's items at 1093/1094 acacia, 1095/1096 dark oak, 1097/1098 mangrove and
  1099/1100 cherry; at common setup, after the modern-dye twins, FC's sub-block recipes for 5-8, 32 of them, appended
  all-or-nothing (no 6-plank fence row). `ProxyRegistry.getBlockId`: the four planks answer 5 while the sub-blocks
  exist. A stub id that resolves to 0 or less aborts the build (one ERROR).
- Six `@Pseudo` load-time patches in `btw.forge.mixin.fcext`, keyed on those ids: `FCItemBlockWoodStubWoodMixin` (the
  five stubs' placement and names), `FCBlockWoodSidingWoodMixin` and `FCBlockWoodMouldingWoodMixin` (the classes' wood
  type), and the stub item renderers `FCBlockSidingStubRenderMixin`, `FCBlockSidingDecorativeStubRenderMixin` and
  `FCBlockWoodMouldingStubRenderMixin`.
- RecipeBridge: 4.B's stair and slab rules as 1.20.1-side recipes (6 planks make 6 stairs, 3 mouldings 1 stairs, 2
  slabs 1 plank); `ModernWoodSawHooks` (new): the saw on the four woods' stairs and slabs.
- **Bamboo** (#052): `btw_recipe_replacements/bamboo_planks.json` removes the grid recipe. `ModernLogChoppingRecipe`
  (new; `betterthanwolves:modern_log_chopping`, data `recipes/bamboo_chopping.json`): an FC axe and a log of the
  recipe's tag give planks or sticks by FC's tool rules, with sawdust ejected at the player; the saw reads the same
  recipe. The saw gives 2 planks + 1 sawdust, an iron-or-better BTW axe 1 plank + 1 sawdust, a weaker one 1 stick + 2
  sawdust; bamboo blocks still break and drop as in 1.20.1. JEI displays.
- **The exact match** (Q5's server half, bundled): `FCCraftingRecipes.Shaped` / `Shapeless.matches` also ask FC's own
  recipe on the FC reading of the grid (square grids; the item-level test first).
- `:Forge:deriveModernWoodTextures` derives the four column sides at build time; 52 lang keys; `[SELF-AUDIT/wood]`,
  which calls FC's own patched methods for each stub kind and wood.
- Unchanged: FC code; the 1.20.1 stairs, slabs, fences, doors, trapdoors and signs of these woods.

**Verification.** Record, wave W7 (2026-10-28): P-W1 85/85 GATE after OK; P-W1-C: all 10 probes equal the plan's after
column. Logs: `[WOOD] sub-blocks of wood types 5-8 at 1093-1100 ...`, `[WOOD] FC recipes of wood types 5-8:
fc_recipe_678..fc_recipe_709 (32), after the modern-dye twins`, `[SELF-AUDIT/wood] ... 0 warnings (0 expected)`,
`newer woods 4 replaced, 8 added`; ProxyBlocks 174 -> 182; `deriveModernWoodTextures`: `8 textures checked (4
written)`, `(0 written)` on a rebuild. Reruns P-FLOWERS, P-BLAST and P-STRATA GATE after OK. Intended changes: the
P-RV census total 709 -> 708 and `sweep fc` 675 -> 707 with 0 fail; P-DYE3's two R11 INFO lines now read `no 2x2
crafting recipe matches` (4.B); the place sweep gains block_1093..1100.

**Caveats.**
- Ids 1093-1100 must never change; if one is taken at start-up none of this turns on (one ERROR).
- Six planks of the four woods make their 1.20.1 trapdoors, not BTW fences (their BTW fences come from three
  mouldings); spruce, birch and jungle trapdoors stay uncraftable at BTW grids, because FC's fence recipe answers that
  grid.
- The derived column sides do not follow resource packs. As in 4.B, no wood sub-block is in a creative tab.
- Bamboo, crimson and warped planks stay 1.20.1 planks (ruled). Bamboo sticks and the bamboo mosaic stay.
- The recipe book and JEI place ingredients by item; the server's result is FC's.

## 2026-10-28 (bq) The BTW beacon on the 1.20.1 block entity; magnetic compasses; FC's sky test where there is no sky light

**Symptom (maintainer ruling of 2026-10-21: FC's beacon runs, the pyramid's block decides the power, no 1.20.1 menu, a
soulforged steel beacon binds the respawn).** A beacon click opened 1.20.1's menu and gave 1.20.1's powers, and FC's
beacon never ran. Pyramids below y=0 never powered, compasses ignored FC's magnetic field, and in the End and above
the Nether roof FC's sky test was always false (flowers, fishing, beacons).

**Root cause.**
- The cell holds a 1.20.1 block entity: FC was handed a `TileEntityBridge` it would cast, HookPolicy's block-entity
  guard kept FC's USE and REMOVE off, and nothing ticked `FCTileEntityBeacon`.
- `btw.modern.TileEntityBeacon`, FCTileEntityBeacon's superclass, was a stateless stub (getters 0, setters no-ops),
  and FC keeps its whole power state there.
- FC's pyramid rule stopped at y=0; FC's Fortune, Looting and True Sight potions (29-31) had no MobEffects and FC's
  ambient flag was dropped; the magnetic-field and looting lists were neither saved nor read; ClientFcWorld's clock
  and build range were Modern-Common's 0.
- 1.20.1 has no sky light in the End or the Nether.

**Fix (general rules).** INTEGRATIONS.md #053, tags `fc_beacon`, `magnetic_compass` and `sky_column`:
- `FcBackedBlockEntities` (new): an FC tile entity attached to a 1.20.1 block entity, saved in an `fcData` sub-tag,
  with an FC world on both sides (a copied fcData restarts as a new block). `BeaconBlockEntityMixin` (new): the
  attachment; FC's `updateEntity` at `tick` HEAD; `updateBase` answers FC's level; `applyEffects` cancelled; the
  activate and deactivate sounds dropped (FC's own power-up and power-down sounds mark the same edges); `createMenu`
  null. `FcBeacons` (new): tick, mirror, sounds, `LootingLevelEvent`. `fcext.FCTileEntityBeaconPyramidFloorMixin`
  (new): pyramids below y=0.
- `HookPolicy.isTeGuarded(Block, fc)`: the block-entity guard lifts for FC-backed blocks, so the beacon's USE is FC's
  by the general rule (also on an empty-handed sneak); the known block-entity gaps drop from 6 to 4.
- Modern-Common `TileEntityBeacon`: 1.5.2's state, NBT and 4.B's FCMOD accessors replace the stub
  (TileEntityBeacon.java:4-22).
- `ClientFcWorld` answers the clock (`getTotalWorldTime`, `getWorldTime`) and the build range.
- `BTWMobEffects` and `PotionMapping`: FC potions 29-31 map to `betterthanwolves:true_sight`, `looting` and `fortune`;
  `PlayerBridge.addPotionEffect` keeps the ambient flag.
- WorldBridge and FcWorldSavedData: FCMagneticPoints and FCLootingBeacons are saved and pruned (1.5.2 SaveHandler,
  WorldServer.ModUpdateTick).
- `MagneticField` (new; 4.B's strongest magnetic point per player every 40 ticks), the packet
  `BTWNetwork.MagneticTargetSync` and the client's `MagneticFieldClient` (the compass target); a lodestone compass
  keeps its lodestone.
- `WorldBridge.fcCanSeeSky` (WorldBridge.java:1765-1766, both FC worlds): where a dimension has no sky light, FC's sky
  test reads the chunk's sky-source column, 1.5.2's column rule.
- Self-audit `[SELF-AUDIT/fcbe]`; BridgeSelfAudit's `KNOWN_TE_GAPS` (BridgeSelfAudit.java:291) no longer lists the
  beacon.
- Unchanged: FC code; the beam, its stained-glass colours, its hum and the beacon advancements stay 1.20.1's.

**Verification.** Record, wave W8 (2026-10-28): P-BEACON 66/66, P-BEACON-PERSIST-A 3/3 and -B 4/4, P-BEACON-C 14/14,
P-MOBS 65/65 and P-BLAST 79/79, all GATE after OK; the beacon plans again at W9, with P-HCR-BEACON 9/9 (a bound beacon
overrides the random respawn, (br)). `[SELF-AUDIT/fcbe] 1 rows, beacon table ok, potions 3/3, 0 warnings (0
expected)`; hook audit 257 blocks, 22 FC-owned (+1), 180 VANILLA (-2), 4 known block-entity gaps; no beacon
ClassCastException. The refmap carries all nine BeaconBlockEntityMixin names. P-HOOK, P-PLACE and SET-CRASH SAME;
dt_p4 and dt_p8 SAME as head (dt_p4 after one rerun of its documented tail of about 3 %).

**Caveats.**
- True Sight shows no mob-spawn or spawn-chunk particles (4.B's client visuals are not ported). Diamond-beacon Fortune
  does not reach blocks that 1.20.1's loot tables drop.
- SFS blight never reaches grass below y=0, and antenna spawns start at y=0 (FC's 1.5.2 ranges). An ender antenna can
  load chunks when it probes for a spawn spot, as in 4.B.
- Other players' compasses and compasses in item frames point at the world spawn.
- Ender chests keep 1.20.1's behaviour: 4.B's antenna tiers are a ruled follow-up (in full, as their own unit). The 4
  known block-entity gaps are the note block and furnace USE and the ender chest's USE and REMOVE.
- The "Modern-Common stubs win" note of the class-shadowing section keeps its ContainerBeacon and SlotBeacon part:
  there is no beacon menu.
- Found during W8, not this unit's, pre-existing at HEAD: the place sweep's sign use logs `ClassCastException:
  btw.forge.TileEntityBridge cannot be cast to btw.modern.TileEntitySign`.

## 2026-10-28 (br) 4.B's hardcore respawn; the creative-world rule (CF2-U1-HS)

**Symptom (maintainer rulings of 2026-10-21: port 4.B's hardcore respawn, a charged Nether respawn anchor still works,
a bound soulforged steel beacon overrides the random respawn, beds never set the spawn; of 2026-10-22, RESPAWN 7(a)
option 3: the random respawn applies in every game mode, except in worlds created as creative).**
- Every death respawned at the world spawn.
- The port stored FC's beacon bind in the hardcore home (`m_HardcoreSpawnChunk`), answered 1.20.1's respawn getters
  from it as a forced point, and copied the bind cooldown across deaths.
- FC's world game type answered SURVIVAL in every world, so FC's creative-world rules (no possession, no scrolls from
  BTW mobs, FC's cow, villager and wolf rules) never applied.
- HEAD: P-HCR 23/53, P-HCR-CW 20/26.

**Root cause.** FC's `FCUtilsHardcoreSpawn.HandleHardcoreSpawn` (Server FCUtilsHardcoreSpawn.java:107-188, called from
ServerConfigurationManager) never ran, and FC's code cannot run through the bridges. Recorded as stub facts:
- `WorldBridge.getTopSolidOrLiquidBlock` is MOTION_BLOCKING (water and leaves count) and never loads a chunk;
- `WorldProvider.getAverageGroundLevel` is 64;
- `FCBetterThanWolves.IsSinglePlayerNonLan` (the client flavour) dereferences `btw.modern.Minecraft.getMinecraft()`,
  which is never set (an NPE);
- `PlayerBridge`'s position fields are a shadow;
- 1.5.2 `EntityPlayer.GetValidatedRespawnCoordinates` was never in Modern-Common;
- Modern-Common `WorldInfo.getGameType` answered the stand-in's SURVIVAL (WorldInfo.java:23).

**Fix (general rules).** INTEGRATIONS.md #054, tags `hardcore_respawn` and `creative_world` (the helper (bm) uses):
- `HardcoreRespawn` (new; its javadoc cites (br), HardcoreRespawn.java:41): `HandleHardcoreSpawn`,
  `AssignNewHardcoreSpawnLocation` and `AssignPlayerToOldSpawnPos*` against 1.20.1 terrain (1.5.2's top solid
  non-leaves block, generating the chunk; the generator's sea level + 1 as ground; feet not in a fluid or powder snow;
  inside the world border; a noise pre-filter that skips oceans without generating them; a terrain-generation budget
  of 3000 ms per respawn), and 4.B's `GetValidatedRespawnCoordinates` for FC's bind. The radii (1000-2000), the
  multipliers (x1.5 after the Nether, x2 after the Wither, x4 after the End, x4 on large biomes), the soft radius
  (100), the 24000-tick window and the spawn effect (22223) come from FC's statics. Every respawn starts with 4.B's
  fresh-player home, time, bind cooldown and dimension-switch time; leaving the End takes the player to the original
  spawn and keeps the respawn point.
- `PlayerListRespawnMixin` (new; PlayerListRespawnMixin.java:47-69): HEAD and RETURN of `PlayerList.respawn`
  (`m_11236_`), a STORE `@ModifyVariable` on its respawn point (null when 4.B ignores it) and one on its resolved
  Optional (the validated beacon), and an inject before `firePlayerRespawnEvent` (`remap = false`) that keeps the
  point and sends 4.B's line. The new point is placed at `ServerPlayer.restoreFrom` TAIL, before the respawn packet.
  ServerPlayerMixin's respawn getters are removed (its refmap entry loses `m_8961_`, `m_8963_` and `m_8964_`).
- The spawn list is saved (`FCSpawnLocations`, FcWorldSavedData) and pruned every 16 ticks in
  `WorldBridge.ModSpecificTick` (`validateSpawnLocationList`, WorldBridge.java:3804, :3848).
  `PlayerBridge.setSpawnChunk` writes 1.20.1's respawn point and remembers FC's bind. BedSpawnPolicy shares its
  `isBed` predicate, and a bed point saved before (be) is ignored and dropped.
- **Creative worlds** (`HardcoreRespawn.appliesTo` = `!WorldInfoBridge.isCreativeWorld(server)`,
  HardcoreRespawn.java:578, read once per respawn): a world is creative when its default game type is. There the
  random, soft and shared respawn, the spawn list, the singleplayer morning and 4.B's End exit are off; FC's bind
  validation, the legacy-bed drop and the fresh-player reset stay. `WorldInfoBridge.getGameType`
  (WorldInfoBridge.java:128) answers FC's world game type from the same value, so FC's creative tests in FCEntityCow,
  FCEntityVillager, FCEntityWolf and FC's builds of EntityCreature (possession) and EntityLiving (the scroll roll) see
  creative worlds. VillageInhabitants and FCGameplayBridge's scroll drop call the same helper.
- **The Nether flag** (`NetherPortalProgress`, new; tag `hardcore_respawn`; no mixin): a LOWEST-priority listener of
  Forge's `PortalSpawnEvent` calls FC's `GameProgressSetNetherBeenAccessedServerOnly` when a portal is lit, the second
  place 4.B sets the flag (FCBlockPortal.tryToCreatePortal :127), which 1.20.1 fire never reaches. Since W10 FC's
  campfire lighting posts the same event (INTEGRATIONS #058, NetherPortalProgress.java:40; P-FPORTAL 38/38 at W10);
  that unit fills no stub and has no entry of its own here.
- Self-audit `[SELF-AUDIT/hcspawn]`; a failure logs at WARN once per site, never at ERROR.
- Unchanged: FC code; 1.20.1 still resolves anchors, `/spawnpoint` and modded respawn blocks.

**Verification.** Record, wave W9 (2026-10-28), GATE after OK: P-HCR 53/53, P-HCR-CW 26/26, P-HCR-LB 8/8,
P-HCR-PERSIST 1/1 + 2/2, P-HCR-C 6/6 (soft respawn health 10), P-HCR-BEACON 9/9, and P-BEACON, P-BEACON-PERSIST,
P-BEACON-C, P-VLB, P-VCURE and P-VPLACE again. `[SELF-AUDIT/hcspawn] PlayerList hooked=true, creative world false ...
-> hardcore respawn on ... FC radius 1000-2000 (x1 x1), soft radius 100, reassignment 24000 ticks, spawn effect 22223,
generation budget 3000 ms; 0 warning(s)`; in P-HCR-CW `creative world true ... -> hardcore respawn off`; in P-HCR-LB
`world type largeBiomes`. Three plan faults were found and fixed against FC's source: a Nether beacon lights only with
bedrock as the first block above it; 4.B heals on Peaceful too; P-USE's deep stone needs a diamond pickaxe since (bo).
Lean regression SAME; P-HCR-LB's large-biomes generation peaked at 2082 ms.

**Caveats.**
- The point search loads and generates terrain on the server thread, bounded by 3 s per respawn
  (`-Dbtw.hardcoreRespawn.budgetMs`); when the budget is spent the respawn falls back to the original spawn. The noise
  pre-filter also rejects spots 1.20.1 adds after the noise stage (icebergs, mangrove roots, trees on low ground),
  which 4.B's literal test would accept.
- FC's day is 1.20.1's day clock: a slept night counts, and with `doDaylightCycle false` the one-day windows never
  end.
- Whether a world is creative follows its default game mode: `/defaultgamemode`, or a dedicated server's `gamemode`
  property (applied at every start), changes it from the next death. A debug world (default spectator) gets the
  hardcore respawn and falls back to the world spawn.
- Beacon binds saved before this update count as ordinary 1.20.1 respawn points until the player rebinds. The first
  death after updating a survival world already uses the new respawn.
- Mystery meat on a player's death is not ported. The singleplayer morning skip and 4.B's beacon chat lines have no
  proof row (manual checks).
- Side effect: thrown soul sand piles have recent respawn points to fly toward again (FCItemPileSoulSand; not
  runtime-tested).

## 2026-10-28 (bs) Torches, ladders, snow, pressure plates and rails refused where 4.B refused them; soul torches; the redstone torch's 4.B placement

**Symptom (maintainer rulings of 2026-10-23: torches and ladders are refused by placement wherever 4.B refused them,
in the same wave as RANB-PARITY, with no pop-off window; soul torches take the torch's 4.B support rule; of
2026-10-25: a soul torch also drops at any neighbour change on a support 4.B refuses, as a torch does; redstone torch
side placement is fixed this round; of 2026-10-31: with 4.B's solid top ((bw)), 1.20.1's weather snow and a sneaking
player's snow layers, pressure plates and rails follow 4.B through the same veto, their survival included (a veto
only, the client gated as before), and the modern pressure plates take the oak or stone plate's rule).**
- 1.20.1's own placement paths (a sneaking click, a click on a modern-only block or on a 1.20.1 class whose `use`
  answers first, mods that call `useOn`) placed the vanilla torch and ladder on supports 4.B refused: glowstone,
  redstone lamps, TNT, pistons, beacons, the tops of glass panes and iron bars, the sides of glass and ice. With
  RANB-PARITY's 4.B answers they would then pop at the next block change beside them (P-RANB K01-K04).
- Soul torches have no FC identity and no FC route, so every click, the client and `/setblock` used 1.20.1's rule
  alone.
- The redstone torch (75/76) placed through FC's route stood beside whatever face was clicked with no support check
  (in mid-air when there was none), and its stand-in's full-cube collision box made FC's route refuse it wherever a
  living entity, the player included, overlapped the cell.
- 1.20.1's weather and placement of floor attachments never asked FC: its weather laid snow on glass, TNT, pistons,
  unlit redstone lamps, a closed upper oak trapdoor and FC machines without a 4.B solid top, and a sneaking player
  placed snow layers, pressure plates and rails there and on glowstone, beacons and an eight-layer snow (plates and
  rails also on ice, hoppers and cauldrons; plates on the oak and nether brick fences, cobblestone walls, glass panes,
  iron bars and anvils). A rail stayed when its support turned into one of these. A sneaking player put the soulforged
  plate on glass or in mid-air beside a wall and hung a BTW ladder with no side to hang from; a BTW torch with no
  support was placed and dropped at once. The modern pressure plates had no FC rule.
- HEAD: P-RANB-PLACE 59/117 (recorded before the plan made its three redstone-torch rows GAP[W10]; 56/117 by the
  current plan), P-FOUND-RT 12/24. Before the floor families (W10 with (bw)): P-SUPPORT-VETO 58/106.

**Root cause.**
- Two support rules: FC's runs only in FC's neighbour check (HookPolicy NOTIFY) and FC's own item route; 1.20.1's
  `canSurvive` (sturdy faces) is what 1.20.1's placement and pop-off ask.
- 4.B keeps 1.5.2's `BlockTorch` for the redstone torch; the port's blocksList held Modern-Common's
  `ConcreteTorchBlock` on a base without 1.5.2's placement members (canPlaceTorchOn, canPlaceBlockAt, onBlockPlaced,
  the null collision box).
- The floor attachments: 1.20.1's weather asks `SnowLayerBlock.canSurvive` (through `Biome.shouldSnow`), its BlockItem
  asks the snow layer's, plates' and rails' `canSurvive`, and a rail's survival is `BaseRailBlock.shouldBeRemoved`,
  which never asks canSurvive; FC's rail neighbour check never runs (BaseRailBlock overrides neighborChanged without
  the base body). The only test 1.20.1's BlockItem asks of FC's own blocks, `ProxyBlock.canSurvive`, answered FC's
  canBlockStay, true for every member of these families. The port's FC block 27, the powered rail, is a plain block,
  not 4.B's BlockRailPowered.

**Fix (general rules).** INTEGRATIONS.md #055, tag `attach_support_rules`:
- **The veto** (`AttachableSupportMixin`, new; RETURN of `canSurvive`, `m_7898_`, on TorchBlock, WallTorchBlock and
  LadderBlock, and since 2026-10-31 on SnowLayerBlock, BasePressurePlateBlock and BaseRailBlock): a 1.20.1 attachment
  check of a state whose rule block is in one of FC's attachment families answers false when that family's 4.B
  predicate refuses the state's orientation; it never adds a support 1.20.1 refuses. Every 1.20.1 placement path,
  1.20.1's weather snow and 1.20.1's own pop-off ask it, on the server (WorldBridge) and in the client's prediction
  (`ClientAttachSupport`, ClientFcWorld's support view). No veto in a WorldGenRegion or off-thread; a throw is logged
  once and vetoes nothing.
- `FcAttachSupport` (new): the families by FC class, resolved once: the torch (FCBlockTorchBase: `canPlaceTorchOn` for
  the floor, FCUtilsWorld's centre hard point for a wall), the ladder (FCBlockLadderBase: `CanAttachToFacing`), the
  redstone torch (Modern-Common `btw.modern.BlockTorch`) and, since 2026-10-31, three floor families that ask FC's
  `canPlaceBlockAt` of the cell, which reads only the block below: ground cover (FCBlockGroundCover: the snow layer
  78, ash 1046), pressure plate (FCBlockPressurePlate: 70, 72, 230) and rail (Modern-Common `BlockRailBase`: 66, 28,
  235, 236). A floor family vetoes only over air, an FC identity both views read alike or a support relative ((bx)),
  on the server too (`sameOnBothViews`, FcAttachSupport.java:442), so every other modern support keeps 1.20.1's
  answer. `SUPPORT_TWIN` (:145, filled at :200-209) lets a 1.20.1 block ask another FC block for the support question
  only (no FC identity, no tick owner): the soul torch and soul wall torch the torch's and wall torch's (50), the
  powered rail the rail's (66), the ten other wooden pressure plates the oak plate's (72) and the polished blackstone
  plate the stone plate's (70). BlockBehaviorMixin's neighbour inject pops a soul torch or a modern plate on a
  refused support with `destroyBlock(pos, true)`, as FC's ValidateState pops a torch and FC's plate neighbour check
  a plate.
- **FC's own blocks** of these families (ProxyBlocks: BTW's torches 1025, 1026, 1033 and 1057, ladders 1051 and 1052,
  ash 1046, the soulforged plate 230): `ProxyBlock.canSurvive` (ProxyBlock.java:933), the only test 1.20.1's BlockItem
  asks of them, asks the veto first, which for a ProxyBlock state is FC's own `canPlaceBlockAt`, the test 4.B's
  ItemBlock asks (no orientation read); the client predicts it where the four sides, and the block below for a torch,
  read alike.
- **Rail survival:** `RailSupportMixin` (new; RETURN of the private static `BaseRailBlock.shouldBeRemoved`,
  `m_49398_`, cancellable): a rail 1.20.1 keeps drops when `FcAttachSupport.railDropped` (FcAttachSupport.java:408)
  answers as 4.B's rail neighbour check: the block below refused, or the block an ascending rail climbs onto without a
  solid top in FC's World (vanilla/server BlockRailBase.java:169-208).
- **The redstone torch** (FOUND-R3 RT, W10): Modern-Common `BlockTorch` (new; BlockTorch.java:28) carries 1.5.2's
  placement (`canPlaceTorchOn` :63, whose id list reads `World.getSupportBlockId` since (bx), `canPlaceBlockAt` :74,
  `onBlockPlaced`: the clicked face, else 1.5.2's search), the null collision box (:42), and `onPostBlockPlaced`,
  which tells a wall torch's six neighbours (the flag-1 notice WorldBridge.setBlock withholds from FC's wall-torch
  writes). `ConcreteTorchBlock extends BlockTorch` (Block.java:2291).
- No FC file, no data, no packet, no HookPolicy row; the mixin list gains RailSupportMixin (78 entries at that build).

**Verification.** Record, wave W1 (2026-10-25/26): Mixin config OK (55 at the AB build); refmap
`AttachableSupportMixin` `canSurvive -> m_7898_`; P-RANB-PLACE GATE after OK with 114 of 117 (its three redstone-torch
rows C11, V25 and V26 are GAP[W10]); P-RANB-PLACE-C 13/13 and its client gate OK; P-FOUND-RT unchanged at its HEAD
signature. Record, wave W10 (2026-10-28): P-FOUND-RT 20/24 and P-RANB-PLACE 115/117. V26 (a redstone wall torch on
glowstone's side) and the side-click, mid-air and armour-stand rows passed; R4, R7, R8, R15, C11 and V25 (a redstone
torch on top of glowstone or ice) failed, and W10's gate held on them: 1.5.2's `BlockTorch.canPlaceTorchOn` asks
`World.doesBlockHaveSolidTopSurface` first, which WorldBridge answered with 1.20.1's `isFaceSturdy(UP)`. Fixed by (bw)
(record, W10 solid-top fix, 2026-10-31): P-FOUND-RT 24/24, P-RANB-PLACE 117/117. Record, W10 SUPPORT-VETO
(2026-10-31): P-SUPPORT-VETO 106/106 (48 GAP, 41 CONTROL, 17 SANITY), GATE after OK (58/106 before, every GAP
failing): 1200 ticks of rain on glass, glowstone, lamp and stone roofs in snowy plains, sneaking placements,
`canSurvive` asked directly, survival after a support change, and FC's route for the powered rail; Mixin config OK (78
entries); the refmap maps RailSupportMixin `shouldBeRemoved` to `m_49398_`; the INFO line reads `FC attachment rules
resolved: FCBlockTorchBase, FCBlockLadderBase, FCBlockGroundCover and FCBlockPressurePlate ...`. With the plate twins
((bx), 2026-11-02): P-SUPPORT-VETO amended (C12, V16, V18 flip as designed) 106/106. W10 guard batch (2026-11-03):
P-FOUND-RT 24/24, P-RANB-PLACE 117/117, P-FOUND-RS 80/80 and P-FOUND-CARTS 80/80 (GATE after OK), P-USE 39/39, P-PLACE
28/29 and P-RM SAME as their last records, SET-CRASH SAME; the clients P-RANB-PLACE-C 13/13 (client gate OK),
placeconsume 30/30 and P-FOUND-RS-C 9/9 SAME. Gate W10: PASS.

**Caveats.**
- The veto never adds a support, so a soul torch cannot stand on the few supports 4.B's torch accepts and 1.20.1
  refuses (leaves tops, the stub weighted plates 147/148, activator rails, trapped chests); the torch item reaches
  those through FC's route as BTW's torch. Snow likewise keeps 1.20.1's refusal on bottom slabs, fences and walls.
- Torches, ladders, soul torches, snow layers, plates and rails already standing on a refused support (old worlds,
  `/setblock`) drop at their next neighbour change.
- A refused support's pop often comes from 1.20.1's `updateShape`, whose loot runs the removed-content modifier,
  instead of FC's drop; the port already mixed both paths at HEAD.
- A redstone wall torch a turntable moves does not tell its new neighbours (HEAD behaviour: WorldBridge.setBlock keeps
  withholding flag 1 for turntable rotations).
- Generated snow is not vetoed (SnowAndFreezeFeature asks canSurvive on a WorldGenRegion): snow on a support 4.B
  refuses (glass, TNT or lamps open to the sky in a snowy biome; rare) goes at its first neighbour change (follow-up
  FU-WGSNOW).
- FC's own route still accepts a powered rail anywhere: the port's FC block 27 is a plain block, so the rail is placed
  and drops at once as an item, where 4.B refuses the click (follow-up FU-RAIL27).
- FC's own neighbour check of the snow layer and of plates 70/72 (BlockBehaviorMixin's NOTIFY) still asks a modern
  support's stand-in: a snow layer on a honey block goes at the next neighbour change, as before.
- An eight-layer snow is no solid top ((bw)): a ninth layer, a plate or a rail on it is refused.
- Over a support the two views do not read alike (a modern block without a support relative, a double slab) the
  client does not predict; if the server then refuses a sneaking placement, the client keeps its predicted stack
  count until the next slot sync. No client plan row covers the snow layer and rail prediction.

## 2026-10-28 (bt) Pressure plates by mob type, BTW arrows on wooden buttons, comparator readings, BTW detector rails (FOUND-R3)

**Symptom (found in round 3; maintainer rulings of 2026-10-23: the soulforged steel pressure plate, BTW detector rails
and comparators on the BTW chest and brick oven are fixed this round; BTW containers give 1.5.2 comparator readings
while the comparator itself stays removed; the vanilla detector rail 28 follows 4.B, so an empty cart does not trigger
it; mobs on pressure plates and BTW arrows on wooden buttons are fixed too).**
- The soulforged steel pressure plate (230) never pressed; stone and wooden plates did not tell players from mobs; BTW
  arrows did not press wooden buttons.
- Comparators read nothing from the BTW chest (1035) or the brick oven (1023/1024).
- BTW's wood and soulforged steel detector rails (235, 236) sensed but never powered, and minecarts did not run on
  them as rails; the vanilla detector rail took an empty cart.
- HEAD: P-FOUND-RS 61/80, P-FOUND-RS-C 6/9.

**Root cause.**
- Modern-Common `BlockBasePressurePlate` and `BlockPressurePlate` stubbed the mechanism (`getPowerSupply` 0, no
  contact, no tick) and kept no mob type; `BlockButton` had no arrow contact; 1.20.1's button re-check counts only its
  own arrows.
- `getComparatorInputOverride` is a base override point answering 0 (the P1 table); Modern-Common BlockChest and
  BlockFurnace lacked 1.5.2's comparator answers, BlockChest its full `getInventory`, and
  `TileEntity.onInventoryChanged` told no comparator.
- BTW's detector rails were ProxyBlocks on a base without 1.5.2's power answers, and HookPolicy held the detector
  rail's INSIDE as PENDING ((be)).

**Fix (general rules).** INTEGRATIONS.md #056, tag `fc_redstone`:
- **Plates:** Modern-Common `BlockBasePressurePlate` gets 1.5.2's contact mechanism (tickRate 20, updateTick,
  onEntityCollidedWithBlock, setStateIfMobInteractsWithPlate, the sensitive box, the power answers);
  `BlockPressurePlate` keeps its mob type (230 players, 70 mobs, 72 everything); WorldBridge gets plate, weighted
  plate and button metadata codecs.
- **Buttons:** `BlockButton` gets 1.5.2's arrow contact (wooden buttons, FC arrows); `ButtonBlockMixin` (new;
  `checkPressed`, `m_51120_`) makes 1.20.1's re-check count FC arrows.
- **Comparators:** `BlockChest` gets 1.5.2's `getInventory` and comparator answers, `BlockFurnace` its comparator
  answers (the brick oven), and `TileEntity.onInventoryChanged` notifies comparators (`func_96440_m` -> Forge's
  `updateNeighbourForOutputSignal`). No comparator block comes back: removed content stays removed ((at)).
- **Detector rails:** 235 and 236 are `FcDetectorRailBlock`s (new): 1.20.1 `DetectorRailBlock`s with FC identity, in
  `#minecraft:rails`, with BTW textures. `DetectorRailBlockMixin` (new; `checkPressed` `m_52432_`,
  `getInteractingMinecartOfType` `m_52436_`) filters 1.20.1's cart query on 28, 235 and 236 through FC's
  `ShouldPlateActivateBasedOnMinecart`. A rail codec; `BlockDetectorRail` power and comparator answers; TickPolicy and
  HookPolicy rules (every DetectorRailBlock's INSIDE is `equivalent:detector-rail`); `[SELF-AUDIT/rails]`.
- No FC file, frozen class or build.gradle change.

**Verification.** Record, wave W10 (2026-10-28): P-FOUND-RS 80/80 (19 GAP rows) and P-FOUND-RS-C 9/9 (3 GAP rows,
joined client), both GATE after OK. Build: Mixin config OK (77 entries); the refmap carries ButtonBlockMixin
`m_51120_` and DetectorRailBlockMixin `m_52432_` / `m_52436_`. Logs: `[SELF-AUDIT/rails] ... FC's cart rule on 3/3
ids, 0 warnings`; the tick audit gives block_235 and block_236 `random=NONE gated=0/24 sched=VANILLA`; the hook audit
gives detector_rail 28 `INSIDE=NOTIFY[equivalent:detector-rail]` (W9: `VANILLA[pending:rail-metadata]`); ProxyBlocks
182 -> 180. Checked offline before landing: javac of all of Modern-Common and Forge (rc 0), the mixin annotation
processor's refmap (ButtonBlockMixin `m_51120_`, DetectorRailBlockMixin `m_52432_` / `m_52436_`), a runtime link check
of the changed classes against the frozen ones, javap of every target; each GAP row traced to PASS through the code.
The W10 guard batch (2026-11-03) reran P-FOUND-RS 80/80 and P-FOUND-RS-C 9/9 (SAME); Gate W10: PASS.

**Caveats.**
- BTW detector rails saved before W10 (as ProxyBlock states) reload as north-south, unpowered rails.
- Re-running the manual `ModelGenerator` would point block_235/236's item models back at the ProxyBlock-era models.

## 2026-10-28 (bu) FC sees 1.20.1 minecarts, boats, paintings, XP orbs and mobs as their 1.5.2 classes (FOUND-R3)

**Symptom (found in round 3; maintainer rulings of 2026-10-23: the BTW hopper and block dispenser work with 1.20.1
minecarts; of 2026-10-25: the BTW block dispenser picks up a 1.20.1 boat as that boat's own item, wood and chest
kept).** FC never saw a 1.20.1 minecart, boat, painting, XP orb or 1.20.1-only mob as the 1.5.2 class 4.B gave it: the
BTW hopper ejected beside a chest minecart instead of filling it, the block dispenser took no cart or boat, the
detector block and BTW's detector rails ignored them, the hopper never swallowed an orb, and the mining charge,
bellows, platforms, falling blocks and FC mobs' pushes passed through them. HEAD: P-FOUND-CARTS 46/80.

**Root cause.** WorldBridge's typed, untyped and by-id queries answered BTW's own entities, players and items only
((ak) added items, and for FC projectiles a generic `VanillaProjectileTarget`); the three block-callback wrappers
handed a generic `EntityBridge`; FC-spawned minecarts and boats became undrawn `fc_entity` puppets of the frozen
classes.

**Fix (general rules).** INTEGRATIONS.md #057, tag `fc_entity_views`:
- `VanillaEntityViews` (new): every entity FC finds is the entity in the world, seen as the 1.5.2 class 4.B gave it,
  through one function (`of`), in this order: a BTW puppet's own FC object; a player's PlayerBridge; an item's
  ItemEntityBridge; a living entity's LivingEntityBridge (1.20.1-only living entities are EntityLiving to FC); a class
  view of a minecart (`btw.modern.EntityMinecart`, `getMinecartType` 0-6, a container cart also an IInventory over its
  1.20.1 container), a boat, a painting or an orb (`xpValue` = value x count); else a generic view, which replaces
  (ak)'s `VanillaProjectileTarget` (FC projectiles keep 1.20.1's `canBeHitByProjectile` filter, so they hit what they
  hit before). Each view is cached per entity with weak keys and re-synced each time FC is handed it (1.5.2's `posY =
  minY + yOffset`); FC's writes go back to the entity (setDead discards it and spills a container cart's contents;
  attackEntityFrom is its own hurt with the mapped source; velocity and position writes move it; collisions push by
  1.5.2's paired formula), flushed as deltas at the next sync and at the end of the level tick. Entry points:
  WorldBridge's three queries and the contact-callback wrappers of ProxyBlock, BlockMixin and BlockBehaviorMixin.
  `[SELF-AUDIT/views]`.
- `FcVehicles` (new): minecarts and boats FC spawns are 1.20.1 entities (through EntityProxyFactory).
- `fcext.FCBlockBlockDispenserBoatItemMixin` (new; `remap = false`): the block dispenser files a boat it takes under
  the boat's own item.
- EntityBridge, LivingEntityBridge and PlayerBridge sync the box and map FC's `mountEntity` on a view to 1.20.1
  riding. The contact path and the queries no longer create EntityBridge wrappers; the class stays.

**Verification.** Record, wave W10 (2026-10-28): P-FOUND-CARTS 80/80 (34 GAP rows) GATE after OK; `[SELF-AUDIT/views]
75 view overrides ... 0 warnings`; the W10 guard batch (2026-11-03) reran P-FOUND-CARTS 80/80, Gate W10: PASS. P-MOBS,
which reads FC's entity queries, has no W10 run recorded. Checked offline before landing: compile and annotation
processor rc 0; javap of the frozen Entity, EntityMinecart, EntityBoat, EntityPainting, EntityHanging and EntityXPOrb
(every overridden member public, every (World) constructor null-safe).

**Caveats.**
- FC's every-entity effects (the detector block, platforms and bellows, the mining charge, FC mobs' pushes) now also
  reach 1.20.1's technical entities, which 4.B never had: markers, display and interaction entities, item frames,
  leash knots. FC's wither may target an armor stand (4.B parity by ruling).
- LivingEntityBridge field writes FC makes on a 1.20.1 mob outside the synced set (a cow kick's motion) are not
  written back. FC mobs and 1.20.1 mobs each run their own paired push, so a 1.20.1 mob gets FC's share and its own.
- BTW minecart and boat puppets saved by an earlier build convert at the end of the tick their chunk loads in; one
  with no saved FC state is removed by its first tick.

## 2026-10-28 (bv) Modern-Common base classes keep 1.5.2's icon members; the wicker pane's texture; the lifted wire's cross (FOUND-R3)

**Symptom (found in round 3; maintainer rulings of 2026-10-23: the wicker pane's missing texture is fixed this round;
of 2026-10-25: restore 1.5.2's icon code in every Modern-Common base class, dead ones too; the platform-lifted
redstone wire uses a cross composed at build time from 1.20.1's dust textures, with no 1.5.2 art).**
- The BTW wicker pane (1084) drew the missing texture (RANB-PARITY's FU-RANB-ICON); the soulforged steel plate (230)
  looked like oak; the BTW workbench (1034) and the mushroom caps (1078, 1079) showed the wrong faces; a hopper's
  filter icon was wrong, and items that are not filters showed one.
- Blocks and items FC draws by icon (held or worn by mobs, carried by endermen, lifted by platforms) drew the missing
  texture or a wrong one: logs, wool, pumpkins, mycelium, slabs, leaves, walls, gates, jukeboxes, dispensers,
  furnaces, pistons, enchanting tables, lamps, dyes, potions; a curved rail drew straight, and redstone wire had no
  cross.
- HEAD: P-WICKER-C 46/64, P-WICKER-H 21/26, P-TEXTURES-C 19/56.

**Root cause.** The Modern-Common stand-ins of 1.5.2's block and item base classes were written without their icon
members (texture names, `registerIcons`, `getIcon`, `getIconFromDamage`, the named getters), so an FC subclass that
calls super or inherits them registered nothing or the wrong name; BlockPane kept no texture names. FCBakedModel's
name map lacked 1.5.2 names that have no 1.20.1 namesake. 1.20.1 has no redstone-cross texture.

**Fix (general rules).** INTEGRATIONS.md #059, tags `legacy_icons` and `legacy_icon_textures`:
- **WICKER:** `BlockPane` (texture names, `registerIcons`, `getSideTextureIndex`), `BlockBasePressurePlate` and
  `BlockPressurePlate` (the icon name), `BlockMushroomCap`, `BlockWorkbench`, `BlockChest` (`registerIcons` "wood")
  and `BlockDetectorRail`; `Block` / `Item.GetHopperFilterIcon` answer null and `ItemBlock` forwards to its block.
- **TEXTURES:** the 44 block bases and 9 item bases of the audit, and the seven bases the audit could not see
  (BlockAnvil, BlockDaylightDetector, BlockFence, BlockQuartz, BlockSapling, BlockStairs, BlockTNT), each with every
  1.5.2 icon member of its class; `BlockGlass`, `BlockIce` and `BlockPortal` pass their names to a new
  `BlockBreakable` name constructor; `isRepeaterPowered` moves up to `BlockRedstoneLogic`, and `BlockComparator(int,
  boolean)` stores it. `ItemBlock.getIconFromDamage` gives a plain ItemBlock the 1.5.2 item class's flat icon
  (ItemColored, ItemMultiTextureTile, ItemSlab, ItemLeaves, ItemCloth).
- **FCBakedModel:** 55 `VANILLA_TEXTURE_MAP` keys for 1.5.2 names with no 1.20.1 namesake.
- **The cross:** `Forge/gradle/legacy-icon-textures.gradle` (new, applied after modern-woods.gradle):
  `composeLegacyIconTextures` writes `textures/block/redstonedust_cross.png` from the client-extra jar's dust textures
  at build time.
- Left by design: `BlockDoor` (the door codec), `BlockFire.getIcon` and `BlockGrass.getBlockTexture`.
- No FC file, no mixin, no new class.

**Verification.** Record, wave W10 (2026-10-28), joined clients, GATE after OK: P-WICKER-C 64/64 (28 server rows and
36 client probes; 18 GAP), P-WICKER-H 26/26 (19 and 7; 5 GAP), P-TEXTURES-C 56/56 (4 and 52; 37 GAP); the dump gates
for the wicker and texture dumps GATE OK (0 missingno, 0 E lines). Build: `composeLegacyIconTextures: 1 texture
checked (1 written)`, `(0 written)` on a rebuild, and the composed cross has the expected pixels; the name gates read
45 map additions (0 problems) and 161 restored names (0 unresolved); LinkAudit's missing members fall from 6 to 5.
Checked offline before landing: javac rc 0, a binary link of the frozen FC classes against the new Modern-Common build
(27,349 references, 0 breaks), and a trace of every GAP row.

**Caveats.**
- The cross PNG exists only after a build that finds the client-extra jar; without it the build WARNs, and a lifted
  wire draws the missing texture.
- Draws outside the plans change to 4.B's (a mob-held glass pane 102 draws glass, a held stone plate 70 stone). Ten
  map keys beyond the design's 45 have no plan row; three are reachable (iron bars 101 held, a lifted detector rail
  28, an anvil 145 held).

## 2026-10-31 (bw) 4.B's solid top: FC's World asks the block's own hard point (W10)

**Symptom (found by W10's proof; maintainer ruling of 2026-10-31: with this fix FC's World answers 4.B's hard point,
and 1.20.1's paths that never ask FC follow 4.B through (bs)'s veto).** A standing redstone torch was placed, and
stayed, on top of glowstone and ice, where 4.B refuses it: P-FOUND-RT R4, R7, R8 and R15 (20/24) and P-RANB-PLACE
C11 and V25 (115/117) failed at W10 (2026-10-28), and W10's gate held on them ((bs)).

**Root cause.**
- `WorldBridge.doesBlockHaveSolidTopSurface` answered 1.20.1's `isFaceSturdy(UP)`, a bridge answer older than round 3.
  4.B's World answers with FC's `block.HasLargeCenterHardPointToFacing(world, i, j, k, 1)`
  (vanilla/server World.java:4786-4791, vanilla/client World.java:5190-5195; FCMOD replaced 1.5.2's version), and
  1.5.2's `BlockTorch.canPlaceTorchOn` ((bs)) asks it first. FC answers false for glowstone (FCBlockGlowStone has no
  override, and its glass material is not opaque, so it is no normal cube) and for ice (FCBlockIce answers
  `bIgnoreTransparency`, false here), whose 1.20.1 tops are sturdy.
- ClientFcWorld answered a normal-cube test, unlike both; HardcoreRespawn's beacon search, a port of 4.B's call
  (vanilla/server EntityPlayer.java:2962), asked `isFaceSturdy` itself.
- The same answer reached every FC caller of the method: the redstone torch, pressure plates, rails, ground cover
  (snow, ash), fire, mushrooms (their spread, and their survival through PlantSoilMixin), stem fruit, nether growth,
  the mining charge, the beacon's enderman and silverfish spawns, endermen placing end stone, FC mobs' dismount and
  teleport cells. All of them stood on glass, glowstone, ice, TNT, pistons, the redstone lamp, the beacon, a closed
  upper oak trapdoor, an eight-layer snow and FC blocks whose 1.20.1 shape is a full cube although FC gives them no
  hard point on top (light bulbs, the BTW hopper, mill stone, cauldron, crucible and more).

**Fix (general rules).** INTEGRATIONS.md #061, tag `solid_top_surface`:
- `WorldBridge.doesBlockHaveSolidTopSurface` (WorldBridge.java:2834) asks the FC block it reads at the cell
  (`getBlockId`) for `HasLargeCenterHardPointToFacing(this, x, y, z, 1)`, so FC's classes answer for their blocks, a
  stand-in for a modern block (the solid cube is a normal cube, the collider asks 1.20.1's sturdy face, a support
  relative its BTW relative since (bx)) and a double slab as FC's double slab. Only where FC has no block for the cell
  (a 1.20.1 block FC reads as air, such as powder snow, or any modern block when the stand-ins could not be allocated)
  does 1.20.1's sturdy face answer.
- `ClientFcWorld.doesBlockHaveSolidTopSurface` (ClientFcWorld.java:163): the same rule on the client, so (bs)'s
  prediction agrees with the server; a top slab or an upside-down stair now holds a standing redstone torch in the
  prediction too.
- `HardcoreRespawn.aroundBeacon` (HardcoreRespawn.java:325) asks the level's WorldBridge ((br)).
- No FC file; no Modern-Common, mixin or build change.

**Verification.** Record, W10 solid-top fix (2026-10-31): P-FOUND-RT 24/24 (R4, R7, R8, R15 FAIL -> PASS) and
P-RANB-PLACE 117/117 `--wave W10`, GATE after OK (C11, V25 FAIL -> PASS). Build: Mixin config OK (77 entries),
LinkAudit missing members 5, foreign 0, BridgeAudit 0 high. Checked offline before landing: javac rc 0 with the lint
set equal to the base's and the refmap byte-identical; an audit of every caller of `doesBlockHaveSolidTopSurface` in
the live classes (javap; the review's binary scan of FC, Modern-Common, Api and Forge classes found no other); a probe
of every 1.20.1 state against FC's answer (FC false where 1.20.1 is sturdy: glass, glowstone, ice, TNT, the redstone
lamp, the beacon, pistons, the piston head facing up, the closed upper oak trapdoor and eight-layer snow; FC true
where 1.20.1 is not: the four removed-content stubs); a scan of every round-3 and regression plan (no other row
changes). W10 guard batch (2026-11-03, (bs)): P-FOUND-RT 24/24 and P-RANB-PLACE 117/117 again; Gate W10: PASS.

**Caveats.**
- The removed-content stubs (trapped chest 146, weighted plates 147 and 148, activator rail 157; FCBlockStub, a rock
  cube) answer true where 1.20.1 answers false: FC's route can stand a redstone torch, plate or rail on one, and snow
  and fire can rest on one. Only `/setblock` or `/give` puts them in a world ((at)); `isBlockNormalCube` has given the
  same answer since W1.
- An eight-layer snow reads as FC's ground cover 78 and is no solid top: a redstone torch, plate or fire is refused on
  it, and FC removes a layer stacked on it (4.B has no layered snow).
- Families split by FC identity: the oak trapdoor 96 (closed in the upper half: no solid top) against the other
  trapdoors, glass 20 against stained and tinted glass, ice 79 against packed and blue ice, whose stand-ins answer
  1.20.1's face (follow-up FU-REL-FAMILIES, (bx)).
- 1.20.1's own placements that never ask FC (weather snow, a sneaking player's plate or rail) take this answer through
  (bs)'s veto since 2026-10-31; generated snow does not (FU-WGSNOW).
- ProxyBlock's display and prediction worlds, the FC entities' client world and the bonus basket's world-generation
  test keep their own answers.

## 2026-11-03 (bx) Modern fences, walls and stained panes answer as their BTW relatives; the modern plates' rule; placed tools ask the relative (W10)

**Symptom (maintainer rulings of 2026-10-31: (1) the wooden plates of spruce, birch, jungle, acacia, dark oak,
mangrove, cherry, bamboo, crimson and warped take the oak plate's 4.B rule, polished blackstone the stone plate's, and
the weighted plates stay stubs; (2) modern fences, walls and panes count as their BTW relatives for every support
question, not for drops, hooks, ticks, connections, collision, rendering or identity; of 2026-10-31 and 2026-11-02:
the oak fence answers for all ten wooden fences, not BTW's fence of the same wood; of 2026-11-02: whether a placed
tool may stick into a block is a support question; how ground cover above a related support is drawn is a
follow-up).**
- FC reads every 1.20.1 fence, wall and stained glass pane without an FC identity (10 fences, 20 walls, 16 panes)
  through a shaped-collider stand-in (a flammable one for the eight flammable wooden fences), which answers 1.20.1's
  own faces. Against their BTW relatives (the oak fence 85, the cobblestone wall 139, the glass pane 102) a modern
  fence or wall answered unlike its relative in five of FC's support questions (the centre hard point up and down:
  stakes, placed tools, lightning rods, falling blocks; the small centre hard point on its sides: iron spikes; the
  ground-cover rest and its -1 resting offset; 1.5.2's redstone torch id list), a stained pane in one (the small
  centre hard point: torches, candles, spikes and chunk ore on top). (bs)'s floor-family gate left all of them to
  1.20.1's plate rule.
- The server refused a standing redstone torch on a modern fence, wall or stained pane while the client, which reads
  them as air, predicted it and shrank the stack (46 blocks).
- Once the centre hard point was the relative's, FC's placed-tool test still read the stand-in's material: no tool
  stuck into the eight flammable wooden fences (FLAMMABLE_ROCK), while crimson and warped fences and every waterlogged
  wooden fence took every pickaxe and the iron chisel and no axe, the reverse of 4.B's oak fence.
- The modern pressure plates had no FC rule ((bs)).
- Before the units (W10 with (bw) and (bs)'s floor families): P-SUPPORT-REL 50/100, P-SUPPORT-REL-C 7/17;
  P-TOOLSTICK-REL 18/38 with SUPPORT-REL landed.

**Root cause.** The stand-in (ModernBlockStandIn) answers FC's support methods from the live 1.20.1 state: the large
hard point is `isFaceSturdy`, the small one `Block.canSupportCenter`, the centre one the large one, the ground-cover
rest its own solid top, the resting offset 0. ClientFcWorld reads every block without an FC identity as air. 1.5.2's
redstone torch id list compares the support's FC id, the stand-in's. FCItemTool's tool test (`CanToolStickInBlock`,
the tool's efficiency) reads the stuck-in block's material, the stand-in's.

**Fix (general rules).** INTEGRATIONS.md #062, tag `support_relatives` (the tables also carry
`attach_support_rules`, (bs)):
- `FcAttachSupport.SUPPORT_RELATIVE` (FcAttachSupport.java:197, filled at :211-231), exact blocks: the ten wooden
  fences -> the oak fence (85, FCBlockFenceWood), the twenty walls -> the cobblestone wall (139, FCBlockWall), the
  sixteen stained panes -> the glass pane (102, FCBlockPane). `supportRelative` (:260) answers the relative's FC block
  when the block is in the table, has FC identity 0 and FC reads the cell through a stand-in
  (`ModernBlockStandIn.isStandIn` of `getWorldBlockId`); `supportRelativeId` (:280). `SUPPORT_TWIN` (:145) gains the
  11 modern plates ((bs)); the two tables stay apart, so no fence, wall or pane has a rule block.
- Seam (a): ModernBlockStandIn hands its large, small and centre hard points, ground-cover rest and resting offset to
  the relative on WorldBridge reads (ModernBlockStandIn.java:437-485; `supportRelative` :494 answers null at once for
  the solid stand-in); `CanSupportFallingBlocks` follows the centre hard point. `IsNormalCube` stays the stand-in's
  (it is also suffocation, push-out and path finding, and answers alike).
- Seam (b): `ClientFcWorld.forSupport` (ClientFcWorld.java:101), a second cached view whose `getBlockId` reads a
  related block as its relative's id (:120); `ClientAttachSupport.worldFor` hands it to (bs)'s prediction, and every
  other client reader keeps `forLevel`'s identity reads.
- Seam (c): Modern-Common `World.getSupportBlockId` (World.java:833, `getBlockId` by default) and its WorldBridge
  override (WorldBridge.java:227); 1.5.2's `BlockTorch.canPlaceTorchOn` reads its id list through it
  (BlockTorch.java:68). `getBlockId` and `getWorldBlockId` are unchanged.
- Seam (d): (bs)'s floor-family gate counts a related support as read alike (`sameOnBothViews`,
  FcAttachSupport.java:442).
- Placed tools (STICK): `fcext.FCItemToolStickRelativeMixin` (new; `@Pseudo`, `remap = false`): `@ModifyArg` index 1
  of the one `CanToolStickInBlock` call in `FCItemTool.onItemUse` (javap offset 213; `require = 1`, `allow = 1`)
  passes `FcAttachSupport.supportBlock` (FcAttachSupport.java:293), the relative's FC block for a stand-in that stands
  for a related support on a WorldBridge. The stuck-in block, its `CanToolsStickInBlock` and the placement sound stay
  the stand-in's.
- `BridgeSelfAudit.runServerStart` calls `FcAttachSupport.audit` (BridgeSelfAudit.java:85): one INFO line, and a WARN
  for an entry without an FC target or for a `minecraft:` fence, wall or stained pane with neither FC identity nor
  relative.
- No FC file; the placed-tool mixin is the one new mixin (79 entries); no data or build change.

**Verification.** Record, W10 SUPPORT-REL (2026-11-02): P-SUPPORT-REL 100/100 (50 GAP, 36 CONTROL, 14 SANITY), GATE
after OK (50/100 at head once F03's check was tightened: FC's route placed BTW's torch on the stained pane and it
popped at once as an item, where the plan predicted it standing); P-SUPPORT-VETO amended 106/106; the INFO line
`Support relatives: 46 supports answer as {FCBlockFenceWood 85=10, FCBlockPane 102=16, FCBlockWall 139=20}; rule
twins: 14 blocks ...`, no WARN; Mixin config OK (78 entries); the harness's `ranb world` gained the support keys
(centre and small hard points, ground-cover rest and offset, falling support). Record, W10 STICK (2026-11-03):
P-TOOLSTICK-REL (20 GAP, 15 CONTROL, 3 SANITY) 18/38 at head; Mixin config OK (79 entries); refmap unchanged (48
classes). W10 guard batch (2026-11-03): P-TOOLSTICK-REL 38/38, P-SUPPORT-REL 100/100 and P-SUPPORT-VETO (amended)
106/106, GATE after OK; the six `btwproof dig` lines identical to head (mining speed and harvest unchanged);
P-SUPPORT-REL-C 17/17 and its probe gate GATE after OK; P-USE 39/39; the other guards as in (bs). Gate W10: PASS.

**Caveats.**
- Ground cover above a related support is drawn at the stand-in's offset, on top of the post (FCBakedModel's live
  access and ProxyBlock's shape world read the stand-in), while FC's rules treat it as resting on the ground under the
  fence; above an oak fence it draws on the ground. Ruled a follow-up: FU-REL-DRAW.
- Unruled families keep their stand-in's answers (follow-up FU-REL-FAMILIES; the differences found by the unit's
  census):
  - fence gates (ten modern): a closed one has a small centre hard point on top, so FC torches, soul torches and
    candles stand on it, and plates are not vetoed on it, where the oak gate 107 refuses both; a redstone torch on a
    closed modern gate still desyncs;
  - stained (16) and tinted glass: every face is sturdy on the stand-in and none on glass 20, so plates, snow, rails,
    ash and stakes stand on top and wall torches and ladders hang on the sides (the redstone torch and the FC torch on
    top agree);
  - packed and blue ice: every hard point, the solid top and the normal cube answer true where ice 79 answers false,
    so plates, rails, stakes, the redstone torch and snow stand on them (1.20.1's snow tag already refuses packed
    ice);
  - the other trapdoors (ten wooden and iron): closed in the lower half the bottom face is sturdy, closed in the upper
    half the top face (torches, redstone torches, plates, snow, rails, ash, stakes and candles), open the side (wall
    torches and ladders); the oak trapdoor 96 holds none, and a waterlogged one derives metadata 0, so ruling them
    needs that order fixed first;
  - modern leaves (acacia, dark oak, azalea, flowering azalea, mangrove, cherry): FC torches and candles stand, and
    FC's snow and ash rest, on oak leaves 18 only;
  - doors (eight wooden, crimson, warped): a closed door's flat face is sturdy, so wall torches and ladders hang on a
    modern door and not on the oak door 64.
- The 40 wall states with no side and `up=false` (two per wall, waterlogged or not; only `/setblock` makes them) have
  no collision and read as air on both views; the cobblestone wall reads 139 there.
- BTW's own wood fences (the wood sidings' fence subtype; the port's 1093, 1095, 1097 and 1099 for woods 5-8) keep
  their 4.B answers (no ground cover rests above them, offset 0, not in the redstone torch list), while the 1.20.1
  fence of the same wood answers as the oak fence, as ruled.
- After an update, attachments now refused go at their next neighbour or shape change: torches, soul torches, BTW
  torches, candles and chunk ore on stained panes; plates on modern fences, walls and panes (among them the spruce and
  acacia plates on the fence tables of taiga and savanna village houses, which survive generation); iron spikes on a
  modern fence's or wall's arm.
- A BTW torch sneaked onto a stained pane beside a modern block with neither FC identity nor relative and no centre
  hard point toward the cell: the server refuses, the client does not predict, and the held stack is out of step until
  the next slot sync.
- Modded fences, walls and panes get no relative (exact blocks); the server-start audit names any vanilla gap.
- The placed tool's placement sound is still read from the stand-in, with no audible change: axes and pickaxes play
  their own sound, and the iron chisel the stand-in's stone, as on the cobblestone wall.
