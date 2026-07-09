# Comprehensive Stub & No-Op Catalogue

Originally audited 2026-03-28. Re-audited 2026-06-05 (see below). Link-audited
2026-07-08 (see below). Every remaining stub, no-op, empty body, hardcoded
return, and TODO across the btw.modern and btw.forge layers.

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

**Now DONE (2026-07-08 follow-up — review pass + remaining ports):**
- **Village subsystem** — Village/VillageDoorInfo/VillageCollection/VillageAgressor
  ported verbatim from 1.5.2; WorldBridge.ModSpecificTick drives villageCollectionObj.tick
  (village discovery from doors, reputation, mating). All 9 Village methods +
  VillageDoorInfo now resolve in LinkAudit. Deferred: cross-session NBT persistence of
  village data (in-memory rediscovery from doors each session — 1.5.2 rebuilds within
  seconds; only reputation + the 3600-tick breeding cooldown reset on restart) and
  VillageSiege (no LinkAudit-flagged caller).
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
- ServerConfigurationManager.transferEntityToWorld, WorldServer.resetUpdateEntityTick — FC
  dimension travel; the modern engine handles proxy dimension travel (portal path dead).
- TileEntityHopper.suckItemsIntoHopper/func_96114_a — hopper minecart; modern handles it and
  FCItemMinecart never dispenses a hopper cart.
- ItemArmor.func_94602_b — empty-armor-slot GUI icon; modern GUI owns slot backgrounds.
- ILogAgent.logSevere — frozen EntityItem.onUpdate log; FC items are converted to modern
  ItemEntity (never ticks); adding to the interface only churns implementors.
- FCClientRenderSpider.setSpiderEyeBrightness — FC multi-pass render; the modern engine
  renders the proxy spider (render passes not wired).
- 68 intentional bridge no-ops (GUI display, GL state, client-only) — correct as designed.
- VillageSiege — a gameplay feature with no caller, not a gap. RenderItem enchant-glint —
  cosmetic, omitted with a comment.

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
