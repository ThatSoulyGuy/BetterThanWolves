# CurseForge feedback triage (2026-10-10)
> **Later triage:** a CurseForge comment of 2026-10-19 (falling blocks, bed and door duplication, blocks that never run out, beds, flowers, zombified villages) is triaged in [curseforge-feedback-triage-2026-10-19.md](curseforge-feedback-triage-2026-10-19.md).

This is a research-only triage of player comments on the CurseForge page. It is diagnosis only: no code was changed. *(status: fixes were made afterwards, in the working tree; see [Fix status](#fix-status-2026-10-10-uncommitted-working-tree).)*

**Method.** Seven investigators each traced one group of complaints through the bridge, the FC sources, the 1.5.2 vanilla sources (`vanilla/`) and the dev logs. Six independent checkers then tried to refute every finding (the release/guidance group had no checker). Claims were checked in two places: the source, and the shipped jar via `javap` on `Forge/build/libs/BetterThanWolves-1.20.1-1.3.0-alpha.3.jar`. The corrections the checkers made are already folded in below. File:line references are against HEAD `48e9825`.

**Verdict key.** *confirmed* means a code-verified bug. *likely* means the mechanism is verified but its effect has not been seen at runtime. *possible* means it may contribute. *authentic* means it is real BTW 4.B behaviour. *ruled out* means the code does not support it.

**FC code is read-only (user directive, 2026-10-10).** Never modify FC code. FC code is FlowerChild's BTW sources (`net.minecraft.src.btw.*` under `Common/`, `Client/`, `Server/`), the 1.5.2 vanilla sources with FC's FCMOD edits under `vanilla/`, the frozen compiled classes in root `build/classes/java/{main,server}`, FlowerChild's original 4.B release files kept in the repo (`Src/`, `SrcServer/` and `SrcClient/`, holding the flat sources and the `patch.txt` files that `setupVanilla` applies to build `vanilla/`, plus `BTWMod4-B0000003.zip`), and FlowerChild's own files such as `Readme.txt`. The FC and `vanilla/` file:line references below are evidence only. Every fix direction in this triage, and every fix made for it, lands outside FC code:
- in the Forge bridge (`btw.forge`, mixins on 1.20.1 classes, data packs);
- in Modern-Common (the `btw.modern` stand-ins such as `Tessellator`, `RenderLiving`, `BlockFluid` and `EntityAITasks`, which are port code, not FC code);
- or in the planned subproject (name not chosen yet) that extends FC code and patches it at build or load time without writing back to FC files. The build-time ASM transforms in `Forge/build.gradle` (`widenFcHookDescriptors`, `openUpEntityAccess`, `extendFcPathingBuildRange`) are its precursors and are expected to move into it.

## Which build players are running

- The public CurseForge files are 0.1.0-alpha.1 (Apr 5), 0.2.0-alpha.2 (Apr 13), 0.3.0-alpha.2 (Apr 21), 0.3.0-alpha.3 (May 16) and **1.3.0-alpha.3 (Jul 9, the most downloaded)**.
- 1.3.0-alpha.3 is git HEAD `48e9825`. The local jar was built from that tree 7 seconds before the version-bump commit. It contains all the Jul-9 suffocation and "red tint" commits (`581d5a2`, `253bca0`, `85fdd2d`, `b1ab3c7`).
- **The red tint only exists in 1.3.0-alpha.3.** It was introduced by `c2e15f3` (Jul 9 02:34, the XP-orb renderer), which no earlier public build contains. A complaint about red mobs therefore comes from the current release, and the Jul-9 "fixes" did not fix it (see R4).
- The release metadata does not map onto git (CTX-2):
  - There are no tags and no CHANGELOG.
  - The version jumped from 0.4.0-alpha.3 to 1.3.0-alpha.3.
  - The CurseForge 0.3.0-alpha.3 upload (May 16) predates the git commit with that label (Jul 8).
  - Every file is marked "Release" rather than "Alpha".
  - The VERSIONING.md checklist (steps 2, 6, 7, 8) was not followed.

## Summary

| Complaint (paraphrased) | Main cause(s) | Verdict |
|---|---|---|
| Mobs are forever red; textures turn red; mobs "fully red" | **R1**: the software Tessellator never resets its vertex colour, so the dark-red colour of a "dragon" XP orb is baked into every FC model drawn after it | confirmed, high |
| XP "does the same thing" (turns red) | **R2**: the player's killing blow is never forwarded to FC, so one-hit kills drop red, uncollectable dragon orbs instead of green XP | confirmed, high |
| Vanilla mobs constantly in a dying state | The dark-red tint from R1 (D3). There is no death-pose bug: D9 rules it out | confirmed, high |
| Mobs die when they enter (water) | **D1**: the FC squid puppet is a plain `PathfinderMob`, so Minecraft drowns it in water. Also D5 (seagrass/kelp read as air) and D4 (modern blocks read as air) | confirmed / likely |
| Ink sacs and arcane scrolls appear in lakes | **D1** (drowned squids drop FC loot) plus **D2** (scroll drops roll *every tick* for live glow squids and drowned) | confirmed, high |
| Constant ghast noises in the Overworld | **GH1**: the bat takeoff sound (1.5.2 aux 1015) is passed unchanged to 1.20.1 `levelEvent`, where 1015 is the volume-10 ghast shriek. Witches (GH2) are a minor second source | confirmed, high |
| Hostile mobs don't attack | **HAI-1**: after the player's first death, every FC system keeps a bridge to the dead player until relog. Plus HAI-3, HAI-4, HAI-5, HAI-6 in specific places | confirmed, high |
| Creepers don't explode | **HAI-2**: the port's AI scheduler resets the attack task (which clears the target) before the swell task starts, so the fuse never lights | confirmed, high (code) |
| Skeletons without bows | **SW1 / U5**: replaced mobs never run FC spawn init. SW2 (equipment is never rendered) is latent behind it | confirmed, high |
| Zombies casually walking on water | **SW3**: Modern-Common `BlockFluid` lacks 1.5.2's null collision box, so FC physics sees water and lava as solid cubes | confirmed, high |
| "Weird zombies" | Probably FC zombies standing on water (SW3). Unreplaced vanilla drowned, husks and zombie villagers (SW5) are a minor alternative | possible |
| Mob animations buggy | **R7**: the stand-in `RenderLiving` drops most of the 1.5.2 render pipeline. **R8**: client FC mobs never tick and replay 2-tick snapshots | confirmed / likely |
| Can't re-enter a world after closing it | **No BTW cause found.** Candidates: RE1, RE-M2, and E1's missing jar classes. Several real state losses on reload (RE3, RE5, RE6, RE7) | possible, low |
| Wiki-dependent, little in-game info | Mostly authentic BTW (CTX-3), but the port makes it worse: the page promises a "field manual" and wiki link that don't exist, and the FC login line, JEI coverage and in-game guidance are all missing (CTX-4, CTX-5, CTX-6) | authentic + gaps |

---

## Fix status (2026-10-10, uncommitted working tree)

On 2026-10-10 the suggested fix order below was carried out in five implementation waves (W1-W5), together with the fixes from the earlier design pass (B1-B4, U1-U10, E1). Each wave was reviewed, and the review findings were then fixed, deferred or rejected. W1-W5 were committed on 2026-10-10 as `f6ec9be`, on top of 1.3.0-alpha.3 (HEAD `48e9825`); a sixth wave (W6, 2026-10-11) fixed bugs the maintainer found in mechanical power, sounds and the creative tab and is still uncommitted. All of it is listed in player terms under `[Unreleased]` in `CHANGELOG.md`. The technical write-ups are `STUB_CATALOGUE.md` entries 2026-10-10 (w) to (am) and 2026-10-11 (an) to (aq), and the older entries this triage corrects ((f), (u) and (v)) now carry 2026-10-10 correction notes. The analysis sections below are unchanged. Their file:line references are still against HEAD and have since moved.

- **W1:** render capture core and the 1.5.2 mob renderers, mob puppets (kill credit, health link, breathing, client animation), player bridge, world wiring, jar class precedence, AI scheduler and non-solid blocks, live FC block rendering, inventory write-back, recipes, JEI and the login handshake.
- **W2:** mining speed, render follow-ups, item use and the nether portal, Modern-Common collision and crafting, mob spawn init and replacement, block-id mapping and aux-sound translation.
- **W3:** logging cleanup, FC login payloads to the client, Modern-Common render fidelity, pathing below y=0, dropped items visible to FC, item use and pickup.
- **W4:** client follow-ups (live render ids, "Obstructed" name tags), held-block rendering, foods fed once, projectile damage sources.
- **W5:** entity names, eager block icons, grass item colour, 1.5.2 food values, bowls and drink containers.
- **W6 (2026-10-11):** not a triage wave. It fixes bugs the maintainer found (mechanical power rendering and logic, sounds, the BTW creative tab) after an audit-and-prove pass; ids MPR, F, S and CT. It also changes GH2 and GH6 below.

**What was verified.**
- `:Forge:jar` built green after every wave.
- LinkAudit: 0 missing classes, 0 foreign `net.minecraft` references. Missing members went from 8 to 6; the 6 left are pre-existing and sit on inert paths.
- BridgeAudit: 0 high-severity issues. The `jar.doLast` class-precedence guard passes.
- A dedicated server (dev `runServer`, fresh throwaway world, run twice) boots to "Done" with 0 ERROR lines. BTW initialisation completes: 148 blocks and 98 items replaced, 666 crafting and 23 smelting FC recipes injected. Worldgen pigs, sheep, cows and chickens and structure villagers are converted to BTW mobs, and the modern-block stand-ins are active.

**Not verified at runtime:** clean server shutdown and `session.lock` release (the test harness could not send `stop`), anything client-side, and multiplayer. No client was launched, so every rendering fix (red tint, mob renderers, held items, animations, chests) is verified by code review only.

**Status key.** *fixed* means the code fix is in the working tree and passed the build, the audits and review; it does not mean it was seen working in game. *partially fixed* means part of the finding is still open, as stated. *not a bug (authentic BTW)* and *ruled out* repeat the triage verdict.

### By complaint

| Complaint | Status |
|---|---|
| Constant ghast noises in the Overworld | fixed (GH1, GH3); witches also fixed (GH2) |
| Ink sacs and arcane scrolls in lakes | fixed (D1, D2; SW5 also replaces glow squids and drowned) |
| Skeletons without bows | fixed (SW1, HAI-6) |
| Zombies walking on water | fixed (SW3); other non-solid blocks partially fixed (SW-M1) |
| Mobs forever red, red textures, red XP, "dying state" | fixed (R1, R2, R6, D3); client not runtime-tested |
| Mobs die when they enter water | fixed (D1, D4, D5, D6, R5, RE7) |
| Hostile mobs don't attack | fixed (HAI-1, HAI-3 including the new below-y=0 pathing fix, HAI-4, HAI-5) |
| Creepers don't explode | fixed (HAI-2, HAI-7); HAI-M1 partially fixed: the cow kick works, but lightning still does not reach BTW mobs |
| Mob animations buggy | mostly fixed (R7); R8 partially fixed |
| Can't re-enter a world | still no cause found; save, stop and load paths hardened (RE1, RE-M2, RE-M3, RE3), not runtime-tested |
| Wiki-dependent, little in-game info | partly authentic (CTX-3); JEI coverage and the login line fixed (CTX-5, CTX-6); the wiki link is still a maintainer TODO (CTX-4) |

### By finding

**Red mobs, red textures, red XP, animations**

| Finding | Status | Wave: what changed | What remains / how to verify in game |
|---|---|---|---|
| R1 | fixed | W1: the Tessellator follows 1.5.2's `hasColor` rules. Colour, normal and brightness reset on every `startDrawing*` and `startCapturing`, and uncoloured vertices take the GL colour (white by default). A capture also resets the translation (review RC-1). | Let a zombie burn at dawn, or one-hit a size-1 slime. When the red orb appears, other mobs must keep their colours. Client not runtime-tested. |
| R1-M1 | fixed | W1: block captures set and then reset white colour and zero translation explicitly, and every capture starts from a clean state. | Same check as R1, near freshly placed FC blocks. |
| R2 | fixed | W1: every hit Minecraft accepts is credited to the FC entity, in `hurt()` and in `die()` before FC's death code, in all three proxies. Credit follows 1.5.2: `recentlyHit`, the attacking player, and the tamed-wolf rule. | One-hit kill any mob: it drops green, collectable XP. Fire, fall and drowning deaths still give dragon orbs, which is authentic (R3). |
| R3 | not a bug (authentic BTW) | none | none |
| R4 | ruled out | W1 restored what R4 found missing. The authentic red hurt flash and the white creeper flash come from the overlay texture, driven by the client entity's hurtTime and deathTime. Dying mobs tip over through `rotateCorpse`. Held items are not tinted (review RC-2). | The `STUB_CATALOGUE.md` (u)/(v) correction is applied (2026-10-10 notes under both entries). Check: a hit mob flashes red briefly and the flash clears; a dying mob tips over. |
| R5 | fixed | W1: `FCEnvHurtGuard` reads FC's own combat log and undoes exactly the in-wall damage, hurt timer and resistance. This also works when the update throws (W1 proxies review P2). W1/W2 drop the matching FC hurt sound (proxies review P4). | Small animals in tall grass or against chests take no damage and make no hurt sound. |
| R6 | fixed | W1: `glColor` and `glNormal` are consumed by the capture. | Dyed sheep, wolf collars, dyed windmill sails and leather armour show their colours. |
| R7 | fixed | W1: full 1.5.2 `RenderLiving` and every vanilla `Render*` subclass FC registers, plus the wither, magma cube, zombie-villager and skull models. W2: golem and silverfish renderers, centred held blocks. W3-W5: plant and block render types, held-block icons. | Chicken wings flap instead of spinning, wolf tails wag, sheep have wool, slimes have sizes, creepers swell. Client not runtime-tested. |
| R8 | partially fixed | W1: client animation tick. It covers limbs; body and head yaw (sent by a new per-tick packet); swing; hurt and death timers; sheep grazing; wolf shake and howl; slime squish; chicken flap; ghast face. Entity events are forwarded to the client FC entity. W2: FC-originated events reach replaced and reloaded mobs; wither side heads turn. W3: iron golem arm and rose timers. | Not done: squid tentacle and body angles, cow kick and squid tentacle-attack counters, and villager and witch particle events (12-15). Check: legs move smoothly, a grazing sheep lowers its head, a wet wolf shakes. |

**Mobs dying, "dying state", ink and scrolls in lakes**

| Finding | Status | Wave: what changed | What remains / how to verify in game |
|---|---|---|---|
| D1 | fixed | W1: all FC puppets can breathe underwater on the MC side, so FC's own air logic is the only one. | `/summon betterthanwolves:fc_squid` in open water: it is still alive after a minute. |
| D-M1 | fixed | W1: proxies return no MC hurt or death sound; FC plays its own. W1/W2: FC's spurious in-wall hurt sound is dropped. | Hitting an FC mob plays its BTW sound, not the player "oof". |
| D2 | fixed | W1: the scroll roll moved to `LivingDropsEvent`. It rolls once per death with FC's odds, skips babies, requires `doMobLoot` and is off in creative-mode worlds; the mob table was corrected. W2 also replaces glow squids, drowned, husks, zombie villagers, strays and wither skeletons with FC mobs (SW5). | No scrolls appear from living mobs in dark water. |
| D3 | fixed | Fixed through R1. | A reporter's screenshot would confirm. |
| D4 | fixed | W2 (U4): colliding post-1.5.2 blocks read as a solid stand-in (`ModernBlockStandIn`) instead of air. The collision scan starts one block lower (U4-b). | FC mobs stand on deepslate and tuff; penned animals no longer hop fences. |
| D5 | fixed | W2: seagrass, kelp, bubble columns and other unmapped water-holding blocks read as water. Waterlogged blocks count in FC's water queries. Metadata fixed by review BM-R1. | Caveat: an FC mob whose eye is inside a waterlogged stair or slab is not "in water" FC-side. |
| D6 | fixed | W1: a per-mob health link copies FC-side damage and heals onto MC health. An FC-side death becomes a real MC death (event 3, no vanilla loot), after FC's own 20-tick death. | A mob killed by a fall or by fire shows the damage and a death animation. |
| D7 | fixed (in 1.3.0-alpha.3) | No change needed. | Affects only players on 0.3.0-alpha.3 or older. |
| D8 | ruled out | Still changed in W2: FC's fire and lava simulation is now the only fire and lava damage on FC mobs. As a result, blazes, ghasts, pigmen, magma cubes and wither skeletons no longer take MC lava and fire damage. | Push an FC blaze into lava: it takes no damage. |
| D9 | ruled out | The death tilt now exists (R4, R7). | none |
| D10 | not a bug (authentic BTW) | none | none |

**Ghast noises, portal**

| Finding | Status | Wave: what changed | What remains / how to verify in game |
|---|---|---|---|
| GH1 | fixed | W2: the new `AuxSfxMapping` translates every 1.5.2 aux-sound and particle id at all three WorldBridge entry points. Unknown ids are dropped, never forwarded raw. | Turn subtitles on and `/summon minecraft:bat ~ ~2 ~` in open air: no "Ghast shoots". |
| GH2 | fixed | W2: the witch idle sound maps to `GHAST_SCREAM`, the file BTW actually uses. W6 (S4): every 1.5.2 sound name now resolves through `LegacySounds` to its own 1.5.2 file, here `mob/ghast/affectionate_scream`, the same file; FC witch hurt and death are now silent, as in 1.5.2 (no witch sound file existed). Biome modifiers remove natural witch spawns outside swamps and add a swamp entry; with vanilla's 5 plus 5 added, the witch share is about 1.9%, the same as 1.5.2. | Swamp huts still spawn witches, as they did in 1.5.2. |
| GH3 | fixed | W2: 1016 now maps to 1026 (zombie infects villager, plus FC's extra moan) and 1017 to 1027 (zombie villager cured). | none |
| GH4 | ruled out | `PossessionDiagnostics` and the `[GHAST-SPAWN]` warning are kept (restored after review ITEMS-R4). | Retire both once a client confirms GH1. |
| GH5 | fixed | W2: `NetherPortalBlockMixin` runs `FCBlockPortal`'s random tick: possession seeding and the nether-accessed flag. It cancels vanilla's pigman spawns, as FC does. | Possession near Overworld portals now runs for the first time. That is authentic, so real ghast moans near portals are BTW behaviour. |
| GH6 | not a bug (authentic BTW) | The verdict stands. W6 (S1): the client display world's `playSound` now plays locally through `LegacyClientSound`, so the millstone and wet-cement display-tick sounds play again. | A running millstone grinds and wet cement moans now and then (its pitch is capped at 2.0 by 1.20.1). Client not runtime-tested. |

**Hostile mobs don't attack, creepers**

| Finding | Status | Wave: what changed | What remains / how to verify in game |
|---|---|---|---|
| HAI-1 | fixed | W1: the player bridge is now a field on the `Player` object (identity, not entity id). Respawn, End exit and logout mark the old bridge dead (W1 player review P2). | Aggro a zombie, `/kill`, respawn: zombies attack again and walking speed is normal. |
| HAI-M1 | partially fixed | W1: a build pass widens FC's narrow hook overrides (cow kick, lightning, squid head-crab) so that FC actually calls them. | Not done: the widened lightning hooks link, but nothing in the port reaches them. MC's own lightning strikes are not passed to FC mobs (there is no `thunderHit` bridge), so lightning still does not charge FC creepers or turn FC pigs into pigmen. Check: a creeper kicked by an FC cow becomes determined to explode. |
| HAI-M3 | fixed (suspected cause) | W1: the scheduler runs `updateTask` on the start tick, as 1.5.2 does. W3 also fixed a separate cause of mobs not hitting: the below-y=0 path freeze (HAI-3). | Seen once in dev and never reproduced. Watch for zombies that chase without hitting. |
| HAI-2 | fixed | W1: Modern-Common `EntityAITasks` follows 1.5.2's order: the new task starts before the clashing one is dropped, and `updateTask` runs on the start tick. | A creeper walks up, hisses, swells and explodes. |
| HAI-3 | fixed | W2: post-1.5.2 blocks are solid to FC (U4). W3: FC pathfinding works below y=0. `ChunkCache` clips to the world's build range, `getSafePoint` uses the real minimum height, and y casts are floored. The old `(int)` truncation made every mob below y=0 stand still, which is another cause of "mobs don't attack" that the triage had missed. | In a deepslate cave or a superflat world at y=-60, zombies chase and attack. |
| HAI-4 | fixed | W2: ids whose block slot is empty read as 0 or as a stand-in, never as a null slot. | Mobs near hay bales, terracotta, carpet or coal blocks find paths; no `"tempBlock" is null` in the log. |
| HAI-5 | fixed | W1 (U6): `WorldBridge` now extends `WorldServer`, so the frozen equipment loop's cast no longer throws. W2 confirmed this with javap. The same fix ends the cow-kick exception. | A witch drinks a potion and keeps fighting. |
| HAI-6 | fixed | Fixed with SW1. U6 landed in W1, before U5 in W2, as required. | Naturally spawned skeletons shoot. |
| HAI-7 | fixed | W1: the creeper swell and flash renderer, and the client fuse counters. W2: explosions pass the mob as the source (kill credit). Creeper, wither and ghast-fireball explosions respect `mobGriefing`; FC dynamite and TNT ignore it, as in 1.5.2. `isFlaming` is honoured. | With `/gamerule mobGriefing false`, creeper blasts break no blocks. |
| HAI-8 | ruled out | none | none |
| HAI-9 | not a bug (authentic BTW) | none | none |
| HAI-10 | fixed | W1: blocks default to 1.5.2's stone step sound, so landing no longer NPEs. W3: per-tick failures are logged once per throw site with a stack trace, with no tick-count sampling. | none |

**Bowless skeletons, "weird zombies", walking on water**

| Finding | Status | Wave: what changed | What remains / how to verify in game |
|---|---|---|---|
| SW1 | fixed | W2 (U5): replacement is deferred to the end of the tick. Fresh spawns run FC's `PreInitCreature` and `initCreature`, or `SpawnerInitCreature` for spawners. Existing mobs get a state transfer: name, age, owner, colour, profession, equipment with drop chances, health. Spawner caps count FC mobs (W2 review finding W2-mobs-R2). | Natural skeletons carry bows; spawner skeletons have none (authentic). |
| SW2 | fixed | W1: per-texture batching and the armour and held-item passes. W2: 1.5.2 item icons map to 1.20.1 sprites. W4-W5: held blocks draw in 3D, and block icons are registered at startup. The data half was already synced. | A skeleton visibly holds its bow and armoured zombies show their armour. Client not runtime-tested. |
| SW3 | fixed | W1: `BlockFluid` has no collision box, as in 1.5.2, and the fluid-height formula is corrected (review AP-3). | `/summon betterthanwolves:fc_zombie ~ ~3 ~` over water: it sinks and swims. |
| SW-M1 | partially fixed | W1-W2: about 30 non-solid blocks lost their collision. 1.5.2 per-position bounds are back for ladders, chests, FC slabs, siding, mouldings and axles, and for fence gates, slabs and rails. The bridge now encodes gate, slab, stair and trapdoor metadata. Non-solid blocks no longer count as opaque. Plants slow FC mobs and cactus hurts them, and FC ray traces use the right bounds. | Not ported: FC mobs do not trigger pressure plates or tripwire, are not moved by nether or end portals, and are not slowed by soul sand. Moving piston blocks are still full cubes to them. *(2026-10-28: pressure plates are fixed: FC mobs press wooden and stone plates by 4.B's mob-type rule, in round 3 of the [2026-10-19 triage](curseforge-feedback-triage-2026-10-19.md) (`INTEGRATIONS.md` #056; P-FOUND-RS 80/80 (W10)). The rest of this row is unchanged.)* |
| SW4 | fixed | W1: `onGround` is forced only when the block below has a collision shape reaching the feet. | Swimming mobs do not hop. |
| SW5 | fixed | W2: drowned, husks and zombie villagers become FC zombies; strays and wither skeletons become FC skeletons (wither type); glow squids become FC squids. Natural zombie villagers become plain zombies, as BTW never spawns them (W2 review finding W2-mobs-R6). | Drowned now bring FC zombies to rivers and oceans, which BTW never had. |
| SW6 | fixed | W1: new `ModelZombieVillager`, and `RenderZombie` switches models. | none |
| SW7 | fixed | W1 deleted the flame particles; W3 deleted the `[AI-DBG]` block. | none |

**Re-entry and reload**

| Finding | Status | Wave: what changed | What remains / how to verify in game |
|---|---|---|---|
| RE1 | partially fixed | W1-W3: FC world data, player NBT, block-entity save, load and invalidate, and the container hooks catch every Throwable and log it once. No FC save-path throw can now abort `stopServer`. | No cause was ever confirmed. Clean shutdown and `session.lock` release are not runtime-tested. Ask reporters for `latest.log`. |
| RE-M1 | fixed | Fixed with HAI-1: the respawned player gets a fresh FC state (food 60, no penalties), as a new 1.5.2 `EntityPlayerMP` does. | Die at a high penalty and respawn: speed is normal. |
| RE-M2 | fixed | W1: the `ServerPlayerMixin` NBT read and write catch Throwable and log once, with the player name. | none |
| RE-M3 | fixed | W1 (E1): the jar now packs classes with the same precedence as dev, and a `jar.doLast` guard enforces it. LinkAudit reports 0 missing classes and now also checks the built jar. | With the built jar, open an anvil and an enchanting table. |
| RE2 | fixed | W1: entity, living and tile-entity bridges hold their MC object weakly. The player bridge lives on the Player, and WorldBridges are detached on server stop. | A level unloaded mid-session (only dynamic-dimension mods do this) stays referenced until the server stops. |
| RE3 | fixed | W1: FC global data is read and written on the overworld's own WorldInfo (U9-ORDER), and the statics are cleared on `ServerStoppedEvent`. The Nether, Wither and End progress flags are now saved. | Put items in a global (tier-3) ender chest, quit and reload: the items are still there. |
| RE4 | fixed | W1 (player bridge) and W2 (ProxyRegistry concurrent maps). | none |
| RE5 | fixed | W2: headless ids are saved per world in `data/btw_headless_items.dat` and restored before chunks load. The world's numbering wins on a conflict (review BM-R2). | Put a copper ingot in an FC basket, restart the game and reload: it is still a copper ingot. |
| RE6 | fixed | W1: `replaceRecipes` rebuilds both recipe indexes, and FC recipes are re-injected after `/reload`. | The recipe book keeps FC recipes after relog and after `/reload`. |
| RE7 | fixed | W1: health loaded from NBT is kept. W1 proxies review P3 also fixed slime and magma-cube max health. | Wound a mob and reload: it is still wounded. |
| RE8 | ruled out | none | none |

**Wiki dependence, release metadata**

| Finding | Status | Wave: what changed | What remains / how to verify in game |
|---|---|---|---|
| CTX-2 | partially fixed | W1: `CHANGELOG.md` was reconstructed and points to the `VERSIONING.md` checklist; this pass completed its `[Unreleased]` section. | Maintainer: git tags and marking the CurseForge files "Alpha" are not done. |
| CTX-3 | not a bug (authentic BTW) | none | none |
| CTX-4 | partially fixed | W1: `CURSEFORGE.md` lost the "field manual" line, got the issue tracker, an accurate JEI description and a Known Differences section; this pass updated Known Differences. | The wiki and original-BTW links are still maintainer TODOs, and the page has a do-not-publish guard until they are filled in. |
| CTX-5 | fixed | W1: `FCLoginHandler` runs FC's handshake: "BTW V4.B0000003" in single player, the connect and "Hardcore Modes" lines on servers. W3: FC\|VC and FC\|OP are forwarded through the new `FCPayload` message (network protocol 3), so servers print "BTW version check successful.". W3-W4: player-names levels 1 and 2 are applied. | The hardcore buoy option still has no effect (not ported). Level 2 obstructs players' name tags only. Check: join a dedicated server and look for the three lines. |
| CTX-6 | fixed | W1: JEI Soulforge and Campfire categories, FC's code-driven crafting recipes in the crafting category, and information pages for kiln, saw, turntable, hopper, fire and campfire. The 2x2 grid follows FC's crafting rules. W2: crafting-table wildcards and Block ingredients match. | Not a triage finding, but the same players will hit it: the knitting recipe JEI shows only starts the knitting. Knitting, wicker weaving and bone carving never finish, because `FCItemCraftingProgressive.UpdateUsingItem` is not bridged (pre-existing; listed under Known issues in `CHANGELOG.md`). |
| CTX-7 | partially fixed | Most of the port bugs it lists (U3, U5, U8, B1-B4) are fixed in W1-W3. | The remaining divergences are listed under Known issues in `CHANGELOG.md` and under Known Differences in `CURSEFORGE.md`. |

---

## 1. Red mobs, red textures, red XP, buggy animations

> "all mobs are forever red" · "repairing the textures that turn red and the XP that does the same thing" · "makes mobs animations buggy and turns them into fully red"

### R1. Tessellator colour leaks across draws and captures (confirmed, high)

- **Mechanism:** `btw.modern.Tessellator` keeps one sticky current colour per thread (`Tessellator.java:61`).
  - Neither `startCapturing()` (`:102-107`) nor `draw()` / `startDrawing*()` (`:130-141`) resets it.
  - 1.5.2's `Tessellator.startDrawing` sets `hasColor = false` (`vanilla/client/.../Tessellator.java:322-339`), so an uncoloured draw uses white.
- **The red source:** the ported `RenderXPOrb` (`Modern-Common/.../RenderXPOrb.java:50-58`, `:68-69`) sets the colour to (0.25–0.5, 0, 0, alpha 0.5) for unowned "dragon" orbs.
- **How it reaches mobs:** FC mob models never set a colour (frozen `TexturedQuad.draw`). So every FC entity captured afterwards inherits the colour through `addVertex` (`:224-243`).
  - `FCEntityRenderer.java:327` / `:340-343` then multiplies the texture by it, using `entityCutoutNoCull` with no blending. The result is an opaque dark-red mob.
  - The same leak affects TESR captures (`FCBlockEntityRenderer.java:104-106`, `:146-151`) and every non-living FC renderer: arrows, windmills, paintings, urns.
  - Only `FCBakedModel` resets the colour before capturing (`:176`, `:395`).
- **Why it lasts "forever":** dragon orbs can't be collected, live 6000 ticks, and are saved with the world. Nothing resets the colour except a first-time block-model bake on the render thread, which runs once per block per session (R1-M1). So the tint comes back right after loading a world that has orbs nearby.
- **Other colours:** player-owned orbs leak yellow-green. Lightning, lifted redstone and `RenderBlocks` shading leak blue-white or grey. These match "textures that turn" other colours.
- **Log evidence:** across the Jul-9 dev runs, every run that drew a dragon orb had red mobs (for example 59/60, 80/96, 104/112 tinted first-renders). Every run with no orb had 0 red mobs.
  - Today's `latest.log` shows mobs white until the first `EntityXPOrb` capture at line ~1215, and red-signature colours (alpha 0.50) after it.
  - The run before `c2e15f3` was 151/151 white.
- **1.5.2:** mobs are only red during the real hurt/death pass. Orb colours never leak.
- **Confirm:** let a zombie burn at dawn, or one-hit a size-1 slime. When the red orb appears, every FC mob on screen turns dark red.
- **Fix direction:** give the Tessellator 1.5.2 `hasColor` semantics, i.e. reset on every `startDrawing*` / `startCapturing`, and default uncoloured vertices to the tracked GL11 colour (white).

### R2. The killing blow never reaches FC, so player kills drop red dragon orbs (confirmed, high)

- **Mechanism:**
  - `ProxyMob.hurt` forwards only non-lethal hits to FC `attackEntityFrom` (`ProxyMob.java:563`, the `!isDeadOrDying()` gate). That forward is the only place FC sets `recentlyHit = 100` (`vanilla/client/.../EntityLiving.java:1102-1107`).
  - The lethal hit goes straight to `die()` → `fcEntity.onDeath` (`:589-614`).
  - FC `onDeathUpdate` then sees `recentlyHit == 0` and takes BTW's "not killed by a player" branch (`EntityLiving.java:739-748`), which spawns a dragon orb.
  - Vanilla XP is suppressed along with vanilla loot (`:617-622`, because `dropExperience` lives inside `dropAllDeathLoot`). So the uncollectable red orb is the only XP the player gets.
  - `ProxyAnimal.java:427-478` and `ProxyPathfinderMob.java:295-338` are the same.
- **Effect:** "the XP that does the same thing" — XP turns red and can't be picked up. It also feeds R1.
- **1.5.2:** a player kill, including a one-hit kill, always gives green collectable orbs.
- **Confirm:** kill a size-1 slime, or any mob, in one hit with a weapon. Note that babies drop no orbs at all.
- **Fix direction:** credit the MC killing blow to the FC entity (`recentlyHit` / `attackingPlayer`) before FC's death path runs.

### R3. Dragon orbs from non-player deaths (authentic)

Dragon orbs from burning, falling, drowning or starvation deaths are real BTW 4.B: they feed the Dragon Vessel. Only the colour leak (R1) and the over-supply (R2) are bugs.

### R4. The Jul-9 "red tint" fixes chased a mechanism that doesn't exist (ruled out)

- `b1ab3c7` blamed FC `RenderLiving`'s hurtTime damage pass, and `85fdd2d` blamed the MC hurt overlay.
- Neither exists in the port:
  - The Modern-Common `RenderLiving` has no hurtTime/deathTime pass (`RenderLiving.java:22-66`).
  - `rotateCorpse` is a no-op (`:85`).
  - `GL11.glColor4f` is never consumed by the capture (R6).
  - `FCEntityRenderer` uses `NO_OVERLAY`.
- The logged tint has alpha **0.50**, which matches `RenderXPOrb.setColorRGBA_I(color, 128)`. A damage pass would give 0.40.
- The confirmation run for `b1ab3c7` (17:04) happened to draw no orbs.
- **Consequences:**
  - STUB_CATALOGUE.md entries 2026-07-09 (u) and (v) record a wrong root cause. *(status: corrected; each entry now carries a 2026-10-10 correction note)*
  - FC mobs currently show **no** authentic red hurt flash and no death tilt at all. *(status: both restored in W1, see Fix status)*
  - Restoring the hurt flash later needs client-side hurtTime decay and forwarding of entity events 2/3; otherwise the stuck-red bug those commits imagined would become real.

### R5. Residual FC self-suffocation (possible, low)

- `FCEnvHurtGuard` reverts health only when its post-move MC probe agrees (`FCEnvHurtGuard.java:46-66`, `:90-107`). `85fdd2d` itself documents that this probe can diverge from FC's own check.
- Any remaining FC-side suffocation deaths produce more dragon orbs.
- The "0 KILLED removals" metric used to validate the Jul-9 fixes can't see FC-side deaths. Those end through `fcEntity.isDead → discard()` (`ProxyMob.java:464-466`) and are logged as DISCARDED, the same as despawns.

### R6. GL11 colour is tracked but never used (confirmed, medium)

- `GL11.glColor*` stores a colour (`GL11.java:212-224`), but nothing reads it.
- Effects:
  - Dyed windmill sails (`FCClientModelWindMill.java:95`) and `RenderItem` tints (`RenderItem.java:188/210`) are lost.
  - White resets such as `FCClientRenderCanvas.java:192` don't happen, so paintings inherit the leaked colour.
- This is the same fix as R1, seen from the other side.

### R7. The stand-in render pipeline drops most 1.5.2 animation (confirmed, high)

- Modern-Common `RenderLiving.doRenderLiving` (`:22-66`) never calls `setLivingAnimations`, `renderSwingProgress`, `handleRotationFloat`, `shouldRenderPass` or `renderEquippedItems`.
- It passes `ticksExisted + pt` where 1.5.2 passes `handleRotationFloat` (`:45`). As a result:
  - chicken wings spin (`ModelChicken.java:99-100`);
  - wolf tails spin (`ModelWolf.java:199`);
  - squid tentacles spin (`ModelSquid.java:48`).
- Squids are yaw-rotated twice (`RenderLiving.java:33-36` plus `FCClientRenderSquid.java:43-55`).
- All 21 `Render*` subclasses (Bat, Chicken, Creeper, Sheep, Skeleton, Slime, Wolf, Zombie, …) are 5–7 line stubs. So there is:
  - no attack swing and no death tilt;
  - no sheep wool, no slime size, no enderman eyes or held block;
  - no creeper swell or flash;
  - no held items or armour.
- **Fix direction:** port the full 1.5.2 `RenderLiving` and the real `Render*` subclasses onto the capture pipeline.

### R8. Client FC mobs never tick; animation replays 2-tick-old snapshots (likely, medium)

- The client branch of `ProxyMob.tick` only copies position (`ProxyMob.java:264-277`; `ProxyAnimal` and `ProxyPathfinderMob` are the same).
- State is broadcast every 2 ticks (`:486-488`), and `FCEntityStateCodec.applyState` (`:118-149`) overwrites the animation floats. `RenderLiving` interpolates the same prev→cur pair across both client ticks, so legs visibly jerk.
- Entity events (hurt 2, death 3, wolf shake 8, sheep eat 10) never reach the client FC entity, because no proxy overrides `handleEntityEvent`.

---

## 2. Mobs dying, "dying state", ink sacs and scrolls in lakes

> "the mobs die when they enter" · "some vanilla mobs constantly be in a dying state" · "ink sacs and 'arcane scrolls' are just appearing in lakes"

### D1. FC squids drown in water on the Minecraft side (confirmed, high)

- **Mechanism:**
  - `VanillaMobReplacer.java:74` replaces every vanilla squid with `fc_squid`.
  - `fc_squid` is registered as a `ProxyPathfinderMob` (`BTWEntityRegistration.java:152`), which `extends PathfinderMob` (`ProxyPathfinderMob.java:21`) and is not a `WaterAnimal`. It has no `canBreatheUnderwater` / `getMobType` override (also checked in the shipped bytecode).
  - `super.tick()` → `LivingEntity.baseTick` → `ForgeHooks.onLivingBreathe` (`ForgeHooks.java:1575-1608`) drains air underwater. At air ≤ −20 the squid takes `hurt(drown, 2)` every 20 ticks and dies about 25 s after submerging.
  - FC's death path then drops the authentic squid loot: 1–3 ink sacs, a gland 1 in 8, and a Respiration scroll 1 in 250 (`FCEntitySquid.java:139-152`, `:579-587`). That is the ink in lakes.
- **Audible too:** proxies don't override the hurt/death sounds, so each drown hit plays MC's generic player "oof" (D-M1).
- **Why it was missed:** `581d5a2` ruled out drowning "on land". `[MOB-DMG]` logging only exists in `ProxyMob.hurt`, not in `ProxyPathfinderMob`.
- **1.5.2:** `EntityWaterMob.canBreatheUnderwater()` is true. Squids drown only out of water.
- **Confirm:** `/summon betterthanwolves:fc_squid` in open water and poll its `Air`. It counts down from 300 to −20, then the squid takes drown hits and dies at about 25 s.
- **Fix direction:** give the squid puppet water-mob breathing on the MC side, and leave FC's own `EntityWaterMob` air logic as the only air logic. Consider honouring FC `getCanSpawnHere` (squids spawn only in darkness in BTW) when replacing.

### D2. Arcane scrolls roll every tick for live, unreplaced vanilla mobs (confirmed, high)

- **Mechanism:** `FCGameplayBridge.onLivingTick` (`FCGameplayBridge.java:118-180`) copies FC's per-death `CheckForScrollDrop` odds but runs them on `LivingTickEvent`, i.e. 20 rolls per second for the mob's whole life:
  - `instanceof Squid` → Respiration 1/250 (`:154-156`)
  - `instanceof Zombie` → Smite 1/1000 (`:131`)
  - `WitherSkeleton` → 1/1000 (`:133`)
- Proxies never match these vanilla classes, so it fires exactly on the **unreplaced** 1.20.1 types (`VanillaMobReplacer.java:42-75`): glow squid (a `Squid` subclass), drowned, husk and zombie villager (`Zombie` subclasses), and wither skeleton.
- **Rates:**
  - A glow squid drops a Respiration scroll about every 12 s. These appear in dark water at y ≤ 30: aquifers, cave lakes and deep ocean.
  - Drowned drop Smite scrolls about every 50 s, in surface rivers.
- **1.5.2:** a scroll is rolled once, at death, outside creative (`EntityLiving.java:3366-3369`).
- **Fix direction:** move the roll to a death event. Decide explicitly whether 1.17+ mobs should drop BTW scrolls at all.

### D3. The "dying state" is the R1 colour leak (confirmed, high)

- The "dying" look is the dark-red tint from R1/R2. Every XP orb leaks: owned orbs give yellow-green, and unowned orbs give dark red. Saved unowned orbs bring the tint back on world load.
- There is no stuck death pose. See D9.

### D4. Modern-only blocks are air to FC (likely; real bug, weak link to these complaints)

- `ProxyRegistry.getBlockId` returns 0 for blocks with no 1.5.2 counterpart (`ProxyRegistry.java:152-159`): deepslate, tuff, granite, diorite, andesite, calcite, mud and others.
- `WorldBridge.getCollidingBoundingBoxes` skips id 0 (`WorldBridge.java:2238-2260`). So FC puppets fall through them and can die of FC fall or lava damage.
- `2026-07-09-4.log.gz:329-374` shows FC-internal deaths at y ≈ −60 during load. The log can't distinguish this from deep-cave lava or residual suffocation.
- This is design_blockmap **U4**.

### D5. FC water detection ignores 1.20.1 water-bearing blocks (likely, medium)

- Only `Blocks.WATER` maps to id 8 (`ProxyRegistry.java:206`). Seagrass, kelp, bubble columns and waterlogged blocks read as air to `handleMaterialAcceleration`, so FC squids on seagrass beds think they're on land.
- The out-of-water branch freezes them (`FCEntitySquid.java:284-298`) and FC drowning starts. While D1 exists, D5 mainly immobilises squids.
- **Fix direction:** consult the `FluidState` on WorldBridge's material and fluid query paths.

### D6. Split HP pools (likely, medium)

- Damage FC applies to itself inside `onUpdate` never reaches MC HP. The only MC→FC mirror is on hurt (`ProxyMob.java:577-579`).
- So FC-side deaths are silent: there is no MC death event, removal is DISCARDED, and the death produces a dragon orb.

### D7. Pre-1.3.0 builds: MC suffocation on every puppet (fixed in 1.3.0-alpha.3)

- `581d5a2` and `253bca0` fixed this. Players on 0.3.0-alpha.3 or earlier still have it: constant damage, and death in about 10 s.

### D8 and D9. Ruled out

- **D8:** MC and FC fire damage do not actually stack. The resist window blocks the second hit.
- **D9:** no puppet can sit at ≤ 0 HP or in a death pose.
  - Both removal paths complete within 20 ticks.
  - The FC renderer has no death pose.
  - No mixin or handler puts genuine vanilla mobs into a dying state: `LivingEntityMixin` acts only on `ServerPlayer`, and `EntityBridge.attackEntityFrom` returns false.

### D10. Authentic water behaviour

Endermen take water damage. In 1.5.2, land mobs (including undead) drown. Squid loot per death is authentic.

---

## 3. Ghast noises in the Overworld

> "ghast noises are constantly playing" · "I keep hearing ghast voices in overworld"

### GH1. Bat takeoff is played as the ghast shriek (confirmed, high; the main cause)

- **Mechanism:**
  - Every vanilla bat is replaced by `fc_bat` (`VanillaMobReplacer.java:71`), which runs the **frozen 1.5.2** `EntityBat`. Every new bat starts out hanging (`EntityBat.java:25-31`).
  - When a bat has no solid block above it, or a player comes within 4 blocks, it takes off and calls `playAuxSFXAtEntity(null, 1015, …)` (`EntityBat.java:141-163`).
  - `WorldBridge.playAuxSFXAtEntity` passes the raw id to `level.levelEvent` (`WorldBridge.java:1534-1537`). `playAuxSFX` (`:1304-1312`) and `func_82739_e` (`:1576-1579`) do the same.
  - In 1.5.2, 1015 is `mob.bat.takeoff` at volume 0.05. In 1.20.1, 1015 is **GHAST_WARN**: volume 10, Hostile category, broadcast to everyone within 64 blocks (`LevelRenderer.java:2600-2601`). Bat takeoff moved to 1025.
- **Why it is constant:**
  - 1.20.1 spawns bats only where the block above is empty (`SpawnPlacements` ON_GROUND), so **every naturally spawned bat shrieks on its first AI tick**.
  - Packs of up to 4 spawn in one call, so shrieks overlap.
  - Bats despawn after about 50 s, so new ones keep spawning.
- **The same untranslated-id bug, other sounds:**
  - 1016 (zombie infects villager) → GHAST_SHOOT (GH3).
  - 1017 (zombie villager cured) → ENDER_DRAGON_SHOOT, which plays the same ghast fireball audio.
  - 1003 (door) → ender-eye launch; 1004 (fizz) → firework.
  - 1007/1008 (ghast charge/fireball) → silent.
- **Confirm:** turn subtitles on and `/summon minecraft:bat ~ ~2 ~` in open air. You get an immediate shriek with the subtitle "Ghast shoots". Muting the *Hostile Creatures* slider silences it, which separates it from GH2.
- **Fix direction:** translate 1.5.2 aux-SFX ids to their 1.20.1 equivalents, or play each 1.5.2 id's sound directly as `playFcAuxSFX` already does for FC ids. Do this at all three WorldBridge entry points.

### GH2. Witches idle with a ghast moan and spawn everywhere (likely, medium; minor)

- The idle sound is authentic: `FCEntityWitch.java:84-93` plays `mob.ghast.affectionate scream` at 0.25 volume.
- Two port differences:
  - `SoundMapping.java:91` maps it to GHAST_AMBIENT (the moans) instead of GHAST_SCREAM (the actual file).
  - 1.20.1 puts witches in most Overworld monster tables at weight 5. BTW 4.B spawns them only in swamps at weight 1.
- Impact is about 1% of monster spawns, audible within 16 blocks.

### GH4. The possessed-squid → ghast theory from `c449199` is not reachable (ruled out)

- Spontaneous possession only starts in the Nether: `EntityCreature.java:494` checks `dimensionId == -1`, and WorldBridge sets that only for `Level.NETHER` (`WorldBridge.java:85-105`).
- Zero `BTW-Possession` or `[GHAST-SPAWN]` lines appear in any instrumented log.
- `PossessionDiagnostics` and `[GHAST-SPAWN]` can be retired once GH1 is confirmed.

### GH5. FC's portal hook never runs (confirmed, high; separate bug)

- 1.20.1 `NetherPortalBlock` overrides `randomTick` without calling super. So the `BlockBehaviorMixin` randomTick route (`:145-154`) never reaches `FCBlockPortal.updateTick`.
- Lost as a result: Overworld possession seeding near portals, pigman-spawn suppression, and the nether-accessed progression flag.
- This works *against* the complaint: there are fewer ghast moans than in BTW.

### GH6. FC block and item ghast sounds (authentic)

- These are all player-driven: lighting fires with the fireball whoosh, soul mechanics, and player-placed netherrack falling with a moan.
- Some are silent in the port: millstone and wet-cement display-tick sounds go through a no-op `playSound`. *(status: playing again since W6, S1)*

---

## 4. Hostile mobs don't attack; creepers don't explode

### HAI-1. After the first death, mobs target a bridge to the dead player (confirmed, high)

- **Mechanism:**
  - `PlayerBridge.cache` is a static `WeakHashMap` keyed by MC `Player` (`PlayerBridge.java:21-22`). MC `Entity.equals` / `hashCode` compare only the entity id.
  - `PlayerList.respawn` builds a new `ServerPlayer`, calls `restoreFrom`, then `setId(oldId)` (`PlayerList.java:440-443`).
  - `getOrCreate(newPlayer)` therefore returns the **old** bridge, whose `realPlayer` is the dead, removed player with health 0 (`PlayerBridge.java:40-44`, `:93`).
- **Effects:**
  - FC target acquisition (`WorldBridge.getClosestVulnerablePlayer`, `:1710-1721`) gets the stale bridge, and `EntityAITarget.isSuitableTarget` rejects it as not alive.
  - Any FC damage goes to the removed player, which is invulnerable.
  - Mobs ignore the player until they relog.
- **Seen in logs:** the bridge reports health 0 while MC health is 20 after respawns in `2026-07-09-5` and `-15`.
- **Same bug, other symptoms:**
  - The FC health penalty stays at level 5 (25% movement speed, 25% mining speed, "dying" HUD) until relog (RE-M1). This also fits "nothing works".
  - Leaving the End through the exit portal aliases the bridge the same way.
- This is the design_mining **B3-CLIENT** issue, whose effect is much larger than "dig flicker".
- **Confirm:** survival play. Aggro a zombie, `/kill`, respawn. Zombies next to you now ignore you, and a relog fixes it.
- **Fix direction:** key the cache by object identity, or rebind on respawn, clone and logout.

### HAI-2. The creeper fuse never starts (confirmed in code and shipped bytecode, high; not runtime-observed)

- **Mechanism:**
  - The Modern-Common `EntityAITasks` runs `interruptConflicts` before the new task's `startExecuting` (`EntityAITasks.java:118-137`, `:151-160`). It calls `resetTask()` on running tasks with overlapping mutex bits.
  - FCMOD's `EntityAIAttackOnCollide.resetTask` clears the attack target (`vanilla/client/.../EntityAIAttackOnCollide.java:95-101`).
  - `EntityAICreeperSwell.startExecuting` then captures `getAttackTarget()` as null (`EntityAICreeperSwell.java:47-50`). The swell stops on the next tick, and the loop repeats every 3 ticks.
- **1.5.2 difference:** 1.5.2 starts the new task first and only drops the conflicting one on a later pass.
- **Symptom:** a creeper walks to about 3 blocks away and stands there, never hissing.
- **It's worse in the shipped jar:** the cow-kick and lightning hooks on `FCEntityCreeper` are dead overloads there.
  - The jar ships the Client copy, with `OnKickedByCow(FCEntityCow)` and no `@Override`.
  - So "determined to explode" is never set, and lightning never charges creepers (HAI-M1).
- **Possible wider impact:** the same scheduler difference (start-before-drop order; no `updateTask` in the start tick) could affect other AI tasks. It is the leading suspect for the one dev observation where a zombie held a target for about 12 s without landing a hit (HAI-M3, low).
- **Fix direction:** match the 1.5.2 `EntityAITasks` ordering.

### HAI-3. Modern blocks are air: cave mobs sink through deepslate (confirmed mechanism, medium relevance)

- This is design_blockmap **U4**, seen from the AI side. Below y=0, nearly all rock is deepslate, so FC mobs there end up inside the rock and never see the player.

### HAI-4. Legacy ids with no block crash pathfinding (possible, medium)

- Ids 95, 137, 152, 159 and 170–173 (terracotta, hay bale, carpet, coal block) have a legacy id but a null `blocksList` slot.
- Frozen `PathFinder` dereferences the slot unguarded (`PathFinder.java:446-452`). The NPE is caught in `EntityAITasks` (`:134-136`) and logged once per JVM, so the task never starts.
- Mobs near those blocks never path to you. This is design_blockmap **PATHFINDER-NPE**.

### HAI-5. Witches freeze after their first potion swap (confirmed, high)

- The equipment-change loop in frozen `EntityLiving.onUpdate` casts `(WorldServer) worldObj` (`EntityLiving.java:864`) and throws `ClassCastException` (WorldBridge is not a WorldServer).
- `previousEquipment` is only updated after the cast, so it repeats every tick and `onLivingUpdate` never runs.
- Seen in 9 Jul-9 sessions. This is design_world **U6**, the same root cause as the cow-kick crash.

### HAI-6. Bowless skeletons fight in melee (confirmed; U5)

- See SW1.
- Fixing U5 alone would make armed skeletons hit HAI-5's cast every tick and freeze. **U5 must land with or after U6.**

### HAI-7. Explosion bridging is incomplete (likely; does not stop explosions)

- `WorldBridge.java:1862-1869` calls `level.explode(null, …, ExplosionInteraction.BLOCK)`. That ignores `mobGriefing` and loses the attacker.
- The creeper swell and flash renderer is a stub, so no swelling is visible even when the creeper ignites.

### HAI-8. Ruled out

- The `EntityAIBase` and `IEntitySelector` jar/dev divergence does not affect targeting.
- These all match 1.5.2: target search, line of sight (`level.clip` COLLIDER), the `travel()` no-op, and `DamageSourceMapping`.

### HAI-9. Authentic behaviour players may read as "not attacking"

- BTW spiders only target players in darkness (brightness < 0.5).
- Sheared (neutered) creepers never swell.
- Peaceful difficulty removes hostiles.
- Undead burn in daylight.

### HAI-10. Minor issues

- A `stepSound` NPE on fall-landing aborts the mob's tick.
- `ProxyMob` samples exception logging at `tickCount % 100`, so failure rates are badly under-reported.

---

## 5. Bowless skeletons, "weird zombies", walking on water

### SW1. Replaced skeletons never run FC spawn init (confirmed, high; design_mobs U5)

- `VanillaMobReplacer.java:107-116` cancels the vanilla mob, which discards its bow, and adds a bare `fc_skeleton` with only position and rotation copied.
- Nothing in `Forge/src` calls `PreInitCreature`, `initCreature` or `SpawnerInitCreature`.
- So `FCEntitySkeleton.setCombatTask` (`:187-203`) sees an empty hand and picks the melee task. `initCreature` (`:63-79`), the only source of bows, never runs.
- Skeleton-trap horsemen and spider jockeys also lose their bows and their mounts.
- **Authentic:** spawner skeletons have no bows in BTW (`FCEntitySkeleton.java:81-89`). Any fix must keep `SpawnerInitCreature` for SPAWNER spawns.
- **Evidence caveat:** the `[AI-DUMP]` lines are taken once per class right after construction, so they cannot prove "no init". The real proof is that grep and javap find no caller.

### SW2. FC held items and armour are never rendered (confirmed, latent)

- The stand-in `RenderLiving`, `RenderBiped` and `RenderSkeleton` skip the equipment and armour passes.
- `FCEntityRenderer` binds one texture per entity (`:312-327`).
- Today nothing holds anything (SW1), so this only becomes visible once U5 is fixed.

### SW3. Water and lava are solid cubes to FC physics (confirmed, high)

- **Mechanism:**
  - 1.5.2 `BlockFluid.getCollisionBoundingBoxFromPool` returns null (`vanilla/client/.../BlockFluid.java:182-185`). The Modern-Common `BlockFluid` has no override (also checked in the shipped bytecode), so fluids inherit `Block`'s full cube (`Block.java:481-494`).
  - `WorldBridge.getCollidingBoundingBoxes` adds every id > 0 with no fluid filter (`:2238-2260`). Water and lava are installed at ids 8–11 (`BTWLifecycle.java:817-840`).
  - So FC zombies stand on the surface at block y+1 and walk at land speed. `handleWaterMovement` finds no water, and `EntityAISwimming` never activates.
- **1.5.2:** zombies bob partly submerged and swim slowly toward their target. Floating near the surface is authentic. Walking on top at full speed is not.
- **Same root cause, wider scope (SW-M1):** about 30 non-solid 1.5.2 blocks lost their collision overrides in Modern-Common. FC mobs would stand on or be blocked by fire, cobwebs, sugar cane, vines, redstone dust, levers and portals (`FCBlockFire.java:24`, `FCBlockReed.java:24`, `FCBlockWeb.java:24`, …).
- **Confirm:** `/summon betterthanwolves:fc_zombie ~ ~3 ~` over open water. It lands on the surface and `Pos` y equals the water block y + 1.0.
- **Fix direction:** restore the null collision box (or skip liquids in `getCollidingBoundingBoxes`), and audit the other non-solid blocks. Then re-check SW4 and the MC-side puppet drowning (D1).

### SW4. The `onGround` override treats water as ground (latent)

- `ProxyMob.java:361-374` and `ProxyAnimal.java:303-312` force `onGround` whenever the block below is not air.
- This is masked today by SW3. After an SW3 fix it would likely make swimming mobs hop out of the water repeatedly.

### SW5. Drowned, husks, zombie villagers, strays and wither skeletons stay vanilla (possible, low)

- These types are not in `REPLACEMENTS` (`VanillaMobReplacer.java:42-75`).
- Drowned don't dominate river spawns: they pass `checkDrownedSpawnRules` only 1 in 15 times. They swim or walk on the bed rather than on the surface, so SW3 fits "walking on water" better.
- These vanilla types also trigger D2's per-tick scroll drops.

### SW6. FC villager-zombies use the plain zombie model (fidelity gap)

There is no `ModelZombieVillager` switch in the stub `RenderZombie`. They look like ordinary zombies, not weird ones.

### SW7. A debug flame particle at hostile mobs' path waypoints (confirmed debug leftover)

`ProxyMob.java:401-415` emits one FLAME particle per second at each hostile's next path waypoint, in release builds.

---

## 6. "Close the world and enter again, it won't let you" / "nothing works"

**No BTW code path was found that reliably blocks re-entry.** Dev logs show both a same-JVM re-entry (`2026-07-09-14`) and a fresh-JVM reload (`2026-07-09-3`) succeeding. We need the reporter's `latest.log`, or a description of what they see on screen. *(status: still no cause; the candidates below were hardened in W1-W3 but clean shutdown is not runtime-tested, see Fix status RE1)*

### Candidates for the entry failure itself

- **RE1 (possible, low).** If `IntegratedServer.stopServer` throws, 1.20.1 swallows it ("Exception stopping the server") but never releases `session.lock` (`MinecraftServer.java:600-604`). The world then disappears from the list, or refuses to open, until the game restarts.
  - BTW adds unguarded serialization on that path: `FcWorldSavedData.save`, `ServerPlayerMixin` write, and Errors escaping `ProxyBlockEntity.saveAdditional`.
  - But every one of those also runs during the pause save, which happens first and would crash instead. No stop-only BTW throw was found.
- **RE-M2 (possible, low).** The FC player-NBT read (`ServerPlayerMixin.java:81-93`) runs only on re-entry, never on a world's first entry. It sits on Minecraft's unguarded single-player player-load path (`PlayerList.java:315-328`), so any throw there crashes that world on every open.
- **RE-M3 (possible; needs a runtime check).** In the shipped jar, the frozen `ContainerRepair` and `ContainerEnchantment`, created by `PlayerBridge.displayGUIAnvil` / `displayGUIEnchantment` (`:737-748`), reference classes **missing from the jar**: `InventoryRepair`, `SlotRepair`, `SlotEnchantment`, `SlotEnchantmentTable`.
  - `ProxyBlock.use` has no try/catch, so opening an anvil or enchanting table could crash the integrated server with `NoClassDefFoundError`. This matches the pattern of `crash-2026-07-08_12.50.44-server.txt`.
  - This is the E1 jar/dev class-precedence issue. It is a strong candidate for "nothing works" on the shipped jar specifically.

### Real state losses on reload (confirmed or likely; each niche)

- **RE3 (confirmed, high).**
  - The static `btw.modern.MinecraftServer` and `worldServers[]` are never cleared on stop. `BTWForgeMod.java:111-114` only logs.
  - They are assigned only at `ServerStartingEvent`, after `prepareLevels`. So during the next load, FC mobs and block entities attach `FcWorldSavedData` through the previous session's server, or through null.
  - **The tier-3 global ender chest inventory is wiped on almost every reload.** This is design_world **U9-ORDER**.
- **RE5 (likely).** Headless legacy ids (25000+) for post-1.5.2 items are allocated per JVM and never persisted, but FC NBT stores them numerically. Modern items in FC containers and ender inventories are deleted or swapped after a game restart.
- **RE6 (confirmed).** `RecipeBridge` injects FC recipes into `RecipeManager.recipes` but not `byName` (`RecipeBridge.java:43-61`). So every login removes FC recipes from the recipe book. Crafting itself works.
- **RE7 (likely).** FC puppet mobs come back at full health after every reload, because `ensureFcEntity` overwrites the loaded health (`ProxyMob.java:134-140` via `:636-642`).
- **RE2 (likely; leak).** The bridge caches (`WorldBridge`, `PlayerBridge`, `EntityBridge`, `LivingEntityBridge`, `TileEntityBridge`) are `WeakHashMap`s whose values hold their keys strongly. Every closed world's server, levels and players stay reachable for the life of the JVM. The leak grows per re-entry but can't block the first one.
- **RE4 (theoretical).** Unsynchronized cross-thread writes to `PlayerBridge.cache` and the `ProxyRegistry` headless-item map.

### Ruled out (RE8)

- Once-only init guards.
- A stale-server chunk deadlock.
- Entity and block-entity load/save hangs.
- Client-side static worlds.

---

## 7. "Wiki-dependent, lack of info"

- **CTX-3 (authentic):** BTW 4.B shipped no manual, tutorial or recipe viewer. Players were meant to use the wiki and forum.
- **CTX-4:** the port makes it worse in a few ways.
  - `CURSEFORGE.md:62` says "Read the field manual before your first night", but no field manual exists.
  - The Links section (`:112-116`) still has `*add link here*` placeholders.
  - The live page links no wiki.
- **CTX-5:** FC's login handshake (`ServerPlayerConnectionInitialized`) is never called, because the hook lived in vanilla `ServerConfigurationManager`. There is no "BTW V4.B0000003" chat line, version check or hardcore-options line.
- **CTX-6:** JEI covers only the 5 bulk-recipe categories (cauldron, crucible, mill stone…; plain Crucible is empty, which is authentic). Missing from JEI:
  - soulforge (anvil) and campfire recipes;
  - kiln, saw, turntable, hopper soul-filter and hibachi recipes;
  - FC's custom `IRecipe` classes. `RecipeBridge` silently drops log chopping, knitting and fishing-rod baiting.
- **CTX-7:** many port bugs (U3, U8, B3, U5, B1, B2, B4 and the ones above) make the game diverge from what the wiki describes, so even the wiki doesn't help.

---

## Corrections to the record

1. **`b1ab3c7` / `85fdd2d` and STUB_CATALOGUE.md 2026-07-09 (u)/(v)** name a hurtTime damage pass and the MC hurt overlay as the cause of the red tint. Neither exists in the port; the cause is R1. The hurtTime-clearing code in `FCEnvHurtGuard` (`:55-61`) has no visual effect. *(status: corrected in STUB_CATALOGUE.md under (u) and (v); `CHANGELOG.md` 1.3.0-alpha.3 now says those commits did not fix the tint)*
2. **`c449199`'s possessed-squid → ghast theory** is unreachable (GH4). The "ghast_hurt in a fresh world" event (STUB_CATALOGUE (f)) is almost certainly GH1, if it was identified by ear. GH1 is GHAST_WARN, with subtitle "Ghast shoots". *(status: corrected in STUB_CATALOGUE.md under (f))*
3. **The "0 KILLED removals" verification** used for the suffocation fixes can't see FC-side deaths, which are logged as DISCARDED (R5).
4. **design_mining B3-CLIENT** understates the PlayerBridge aliasing. It also breaks hostile AI (HAI-1) and leaves penalties stuck after a death (RE-M1).

## Overlap with the earlier bug-design pass

| Triage finding | Earlier design id |
|---|---|
| HAI-1, RE-M1 | design_mining B3-CLIENT (PlayerBridge cache aliasing) |
| HAI-3, D4 | design_blockmap U4 |
| HAI-4 | design_blockmap PATHFINDER-NPE |
| HAI-5 (and the cow-kick crash) | design_world U6 |
| SW1, HAI-6 | design_mobs U5 (must land with or after U6) |
| RE3 | design_world U9-ORDER + open decision 1 (clear statics on stop) |
| RE-M3, HAI-8 notes | design_build_logging E1 |
| SW7 | design_build_logging U10 |

New issues this triage found, not covered by the design pass:

- R1, R2, R6, R7, R8 (rendering and XP)
- D1, D2, D5, D6 (dying and drops)
- GH1, GH2, GH5 (sounds and the portal hook)
- HAI-2, the dead creeper overloads, HAI-7 (creepers)
- SW3 plus the other non-solid blocks, SW4 (collision)
- RE1, RE2, RE5, RE6, RE7, RE-M2 (reload)
- CTX-2, CTX-4, CTX-5, CTX-6 (release metadata and guidance)

## Questions for the reporters

> **Status (2026-10-10):** the fixes in [Fix status](#fix-status-2026-10-10-uncommitted-working-tree) have not shipped yet. Once a build with them is out:
> - The jar question still matters. Reports against 1.3.0-alpha.3 or older describe the old code, so only reports from the new build can point to new causes.
> - The re-entry question matters most. RE1 was only hardened: no cause was found, and clean shutdown was not runtime-tested. We still need `logs/latest.log`, any crash report, what the screen shows, and whether restarting the game helps.
> - The death, ghast-subtitle, scroll and screenshot questions matter only if those symptoms come back on the new build.

- Which jar did you run (filename, or the mod version in the mod list)? 1.3.0-alpha.3 shows R1. Older builds show D7.
- For re-entry: what does the screen show? The world missing or greyed out, a "Failed to access world" toast, a crash, or a hang? Does restarting the game fix it? Please attach `logs/latest.log`, and the crash report if there is one.
- Did you die before the mobs stopped attacking (HAI-1)? Does relogging bring them back?
- With subtitles on, what does the ghast sound say ("Ghast shoots" = GH1)? Does muting *Hostile Creatures* stop it?
- Which scroll shows up in the lakes (Respiration = glow squid or FC squid; Smite = drowned)? Is it a surface lake or an underground one?
- For "dying state": a screenshot. Dark-red mobs mean R1; mobs lying on their side would be something new.

## If and when fixing starts (suggested order; carried out in W1-W5)

> **Status (2026-10-10):** this order was carried out in waves W1-W5, together with the design-pass fixes; see [Fix status](#fix-status-2026-10-10-uncommitted-working-tree).
> - W1: steps 1 (R1), 3 (D1, D2), 4 (HAI-1), 5 (HAI-2 and the creeper overloads), 6 (SW3; the other non-solid blocks continued in W2 and are still partial, see SW-M1) and 7 (R2).
> - W2: step 2 (GH1).
> - Step 8: U6 in W1, then U5 in W2.
> - Step 9: the save/load guards, RE3, RE6, RE7 and E1/RE-M3 in W1; RE5 and the last guard in W2.
> - Step 10: the changelog, the CURSEFORGE.md fixes, the login line and JEI in W1; the server-side login payloads in W3.
> - Not done: git tags, marking the CurseForge files as Alpha, and the wiki and original-BTW links (maintainer).

1. **R1:** Tessellator colour reset. Small; fixes the red mobs, red textures and "dying state".
2. **GH1:** aux-SFX id translation at all three WorldBridge entry points. Small; fixes the ghast noises.
3. **D1 and D2:** squid breathing; move the scroll roll to death. Small; fixes ink and scrolls in lakes, and squid deaths.
4. **HAI-1:** identity-keyed PlayerBridge (with B3-CLIENT). Fixes mobs ignoring you after death and stuck penalties.
5. **HAI-2:** 1.5.2 `EntityAITasks` ordering, plus the dead creeper overloads. Fixes creepers, possibly other AI.
6. **SW3:** fluid and non-solid collision. Fixes walking on water.
7. **R2:** credit the killing blow. Gives green XP for player kills.
8. **U6, then U5:** skeleton bows, witch freeze, cow kick.
9. Reload hardening (guard save/load hooks, clear statics on stop), then RE3, RE5, RE6, RE7, and the E1 missing jar classes (RE-M3).
10. Release hygiene and onboarding:
    - tag and changelog the releases;
    - mark the CurseForge files as Alpha;
    - fix the CURSEFORGE.md links and remove the "field manual" line;
    - restore the FC login line;
    - extend JEI coverage.

Raw research output, with every claim, file:line and checker note, is in the session scratchpad as `triage_*.json`. It is not committed.
