# CurseForge feedback triage (2026-10-19)

This is a research-only triage of one comment left on the CurseForge page by a CurseForge commenter (2026-10-19). It is diagnosis only: no code was changed, and nothing below has been fixed. *(status: fixes were made afterwards: those of 2026-10-19 to 2026-10-21 are committed as `4558220`, and round 3 (2026-10-21 to 2026-11-03) is in the working tree; see [Fix status](#fix-status-2026-11-03-uncommitted-working-tree).)* The earlier triage of 2026-10-10 is [curseforge-feedback-triage.md](curseforge-feedback-triage.md). Its finding ids (R1, HAI-2, SW5 and so on) are used here unchanged; this triage's ids carry a `CF2-` prefix.

**The comment.** It has eleven numbered observations. The commenter numbered two items "5"; here they are (5a), placed blocks returning after death, and (5b), smoke from loose snow. It also has a remark that deepslate, deepslate ores and the newer wood types work as in plain 1.20.1 ("PR" below), and three questions marked "unknown if this is just BTW": U1 (can't sleep), U2 (creeper drops) and U3 (zombified villagers). The commenter played early-game survival in and around a village, and does not plan to continue.

**Method.** Six investigators each traced one group (rendering, drops, inventory, placement, mobs, sleep and progression). They followed it through:
- the port (`Forge/src/main/java/btw/forge`, `Modern-Common/src/main/java/btw/modern`);
- the FC sources (`Common/`, `Client/`, `Server/`) and the 1.5.2 sources with FC's FCMOD edits (`vanilla/`);
- the decompiled Forge 1.20.1 sources and the vanilla 1.20.1 data (loot tables, recipes, worldgen);
- the dev logs under `Forge/run/`, the git history, and the jars in `Forge/build/libs/` (`javap`, JDK 17).

A skeptic pass then re-derived every root cause from the source. Its corrections are folded in below.

**References.** Code file:line references are against HEAD `0292e38` unless a commit is named. References into `CHANGELOG.md`, `STUB_CATALOGUE.md`, `BRIDGE_SPEC.md`, `INTEGRATIONS.md` and `CURSEFORGE.md` are against the working tree, which has uncommitted C1/C2 doc edits. A 1.20.1 reference names the 1.20.1 class ("1.20.1 `ServerPlayer.java:853`"). `followups_recon.md`, `hooks_delta.md`, `woodtypes_design.md`, the read-only audit scripts and the proof harness `TickProof.java` are in the session scratchpad and are not committed.

**FC code is read-only.** As in the earlier triage, every fix direction below lands outside FC code:
- in the Forge bridge;
- in the Modern-Common stand-ins;
- or in the planned patching subproject.

FC and `vanilla/` file:line references are evidence only.

**Verdict key.**
- *confirmed*: a code-verified bug.
- *likely*: the mechanism is verified, but its effect has not been seen at runtime.
- *possible*: it may contribute.
- *authentic*: real BTW 4.B behaviour.
- *fixed-since*: fixed after the build the commenter probably used.
- *ruled out*: the code does not support it.

Unless a finding says otherwise, nothing here has been reproduced in a running game. Every finding ends with a repro idea: `proof_run` plan lines, or a manual client step.

## Which build the commenter ran

- `CHANGELOG.md` records 1.3.0-alpha.3 (`48e9825`, 2026-07-09) as the last release; everything since is under `[Unreleased]`.
- Git has later version bumps (`git log -p gradle.properties`):
  - 1.4.0-alpha.5 (`d55db5d`, 2026-10-10);
  - 1.4.1-alpha.5 (`ba91232`, 2026-10-11);
  - 1.6.1-alpha.5 (`3985147`, 2026-10-16);
  - 1.6.3-alpha.5 (`af433f8`, 2026-10-18).
  
  The working tree carries 1.7.3-alpha.5 (uncommitted). `Forge/build/libs/` holds jars labelled 1.4.1-alpha.5, 1.4.1-alpha.6, 1.5.1-alpha.5, 1.6.1-alpha.5, 1.6.2-alpha.5 and 1.6.3-alpha.5. The 1.4.1-alpha.5 jar was rebuilt on 2026-10-15, after `9c05019`, so a version label does not pin the code.
- **Not 1.3.0-alpha.3.** Creepers exploded for the commenter (5a, U2). In 1.3.0-alpha.3 the creeper fuse never starts (earlier triage HAI-2). `f6ec9be` fixed that, so the build is 1.4.0-alpha.5 or later.
- Two drops agree. The stray bed was red and the wrong flower was a poppy. Since `f6ec9be` the legacy-id table is a `LinkedHashMap`, so the pick is always the red bed and the poppy. In 1.3.0-alpha.3 it came from an `IdentityHashMap` and varied per launch: 1 in 16 for red and 1 in 9 for the poppy.
- **Probably 1.4.1-alpha.5 or later.** Two items need the general ItemBlock trigger of CF2-I1, which `ba91232` (1.4.1-alpha.5) added:
  - full blocks from a creeper blast coming back after death (5a);
  - a phantom wooden pressure plate (4).
  
  Gravel and loose snow slabs placed onto slabs of the same kind are lost in every build.
- Nothing else in the report narrows it further. Every finding below that is still at HEAD was also in the commenter's build.
- So a build newer than 1.3.0-alpha.3 seems to have been public with no CHANGELOG release entry, no tag and no VERSIONING checklist (the CTX-2 gap of the earlier triage). The maintainer should confirm which files are on CurseForge, and record them.

## Summary

| # | Complaint (paraphrased) | Main cause | Verdict | At HEAD |
|---|---|---|---|---|
| 1 | A falling block is an orange outline until it lands | **CF2-R1**: the client can't construct `FCEntityFallingBlock` (no `(World)` constructor), so the FC entity renderer draws its debug box | confirmed, medium | yes |
| 2 | A yellow village bed also dropped a red bed | **CF2-DR1**: the FC harvest drops legacy bed 26 (canonical red bed). Then 1.20.1's shape update pops the other half, with its own loot. Doors duplicate the same way (**CF2-DR6**) | confirmed, high | yes |
| 3 | The cobweb's breaking texture looks stretched | **CF2-R3**: BTW's crossed quads carry an UP normal, so 1.20.1's crack projection smears along the diagonal planes | confirmed, low | yes |
| 4 | A pressure plate, then a loose snow slab, appeared when shift-clicking into the hotbar | **CF2-I2**: items the server never removed on placement (CF2-I1) are revealed by a slot resync. Crafting is ruled out (**CF2-I4**) | likely, high | yes |
| 5a | Placed blocks came back after dying | **CF2-I3**: the server still held them (CF2-I1); death drops the real inventory | likely, high | yes |
| 5b | Loose snow slabs give off smoke | **CF2-R5b**: 4.B emits faint white wisps; the port draws long-lived campfire smoke that ignores the particle setting | authentic, plus a confirmed fidelity bug (low) | yes |
| 6 | Items shoot out in any direction after a break | **CF2-DR2**: the FC harvest always uses face UP, and FC drops spawn 0.125 too high. 1.20.1 then pushes items out of the solid block above | confirmed (the link to the symptom is likely), medium | yes |
| 7 | Mobs still target players in creative | **CF2-M7**: 4.B AI keeps a target across a switch to creative, and some event paths target creative players | authentic | yes (4.B) |
| 8 | A flower could be placed on stone and slabs; it broke on the slabs | **CF2-P8a**: `WorldBridge.canPlaceEntityOnSide` is a stub. **CF2-P8b**: the pop is 4.B's removal of an invalid flower | confirmed, medium / ruled out | yes / n/a |
| 9 | An oxeye daisy dropped a poppy | **CF2-DR3**: nine flowers share legacy id 38, and its FC drop resolves to the poppy. **CF2-P8c**: placing them through FC also turns them into poppies | confirmed, low / medium | yes |
| 10 | Gravel slabs never run out | **CF2-I1**: a nested PlayerBridge re-sync inside FC's placement detaches the held FC stack before FC decrements it. **CF2-P10** is a different, older trigger in 1.3.0-alpha.3 | confirmed, high / fixed-since | yes / no |
| PR | Deepslate, deepslate ores and the newer woods work as vanilla | **CF2-PR-DEEP** (no strata, no deepslate identity), **CF2-PR-ORE** (raw ore smelts to ingots), **CF2-PR-WOOD** (no log rules for the new species) | confirmed / likely / confirmed | yes |
| U1 | Can't sleep, no message, only the spawn is set | **CF2-U1**: not sleeping is 4.B. The silence is a `ClassCastException` in `PlayerMixin.btw$disableBeds`, and vanilla code sets the spawn. **CF2-U1-HS**: 4.B's hardcore respawn is absent | authentic + confirmed, high | yes |
| U2 | Creepers drop full blocks, not piles | **CF2-DR4**: full blocks are 4.B, but the block identities and the extras differ. **CF2-DR5**: mining charges drop nothing | authentic + confirmed | yes |
| U3 | Are villagers meant to be zombified? | **CF2-MU3**: yes. In 4.B every village villager is a villager zombie until cured with a soul urn. The port's 1.20.1 villages have normal villagers; the zombies the commenter saw are FC zombies converting villagers | authentic (the port's gap is confirmed) | gap at HEAD |

Side findings not in the report: CF2-DR5 and CF2-DR6 (drops); CF2-I5 and CF2-I6 (inventory); CF2-MX1, CF2-MX2 and CF2-MX3 (mobs); CF2-U1-HS (respawn); CF2-P8c (placement).

---

## Fix status (2026-11-03, uncommitted working tree)

Between 2026-10-19 and 2026-10-21 the fixes below were made in the working tree, on top of HEAD `0292e38` with the uncommitted C1/C2 docs and the 1.7.3-alpha.5 version bump, together with the post-C2 follow-ups (the dev-run fix, white dye, leaves, the four newer woods and the log rule). None of it is committed. *(2026-10-28: all of it was committed on 2026-10-21 as `4558220`.)* On 2026-10-20 the maintainer ruled that the CF2-I1 duplication is fixed first (INV-RESYNC), before the use-order change (I-1) and wave C (I-2 placement, I-3 hook ownership and beds, I-4 lava), and that beds and doors take 1.20.1 drops (BD). Beds had been ruled on 2026-10-19 (ruling 3: sleeping is allowed, and a bed never sets the spawn). The player-facing changes are listed under `[Unreleased]` in `CHANGELOG.md`, the technical write-ups are `STUB_CATALOGUE.md` entries 2026-10-19 (av) to 2026-10-21 (bf), and the integrations are `INTEGRATIONS.md` #033 to #040. The analysis sections below are unchanged. Their code file:line references are against HEAD and their doc references against the docs of 2026-10-19; both have since moved.

- **2026-10-19:** WS-DEV, the dev-run fix (not a triage item).
- **2026-10-20, stage B:** white dye (WD), leaves (LEAF), the four newer woods (WOOD-A) and the log rule (WOOD-B).
- **2026-10-20, INV-RESYNC:** the CF2-I1 fix, plus a creative last-item fix from its review.
- **2026-10-20, I-1:** the use order (the block before the held BTW item), rebased onto INV-RESYNC.
- **2026-10-20, BD and dye3:** bed and door drops, colour-only 1.20.1 dyes and 1.5.2 subtype stacking.
- **2026-10-21, wave C:** I-2 placement validity, I-3 hook ownership and beds, I-4 lava (landed 2026-10-20 and 2026-10-21, proven 2026-10-21), then a final sweep on the final tree.

**What was verified.**
- Every stage built (exit 0). LinkAudit kept the 6 pre-existing missing members throughout, BridgeAudit found 0 high-severity issues, and the jar guards pass (`Mixin config OK (54 entries)` after wave C).
- Proof first: every gap was first recorded by a scripted in-world test (`proof_run` plans) on the old code. Each stage's gate then passed, and every regression plan read SAME (same verdicts and values) against the stage before, apart from one pre-existing flaky row (a Legendary Survival Overhaul campfire reading) that passed on rerun. The final sweep of 2026-10-21 ran the remaining regression plans on the final tree. The record is the session scratchpad's `fu_runs/BASELINE_RUNS_FU.md` (not committed).
- The proof's fake player is not in the level, so it cannot show CF2-I1. The inventory fixes were therefore proven with a development client joined to the dedicated server (`placeconsume`, `windmill`). Beds and placement also ran with a joined client (`P-BED-C`, `P-PLACE-C`), and so did the newer woods' textures and dig speed (`wood_c1`, `mlog_c1`).

**Not verified at runtime:** the release jar (no production smoke was run for this work), a launcher-installed client, multiplayer, other mods' fake players, placing while swimming or riding, a real player's own collision box, adventure-mode CanPlaceOn, and worlds saved before these changes (bed spawns set earlier, dyes already in BTW storage, old acacia stacks, a tripwire hook saved at HEAD). Watch items: once, a client that joined after a stalled login missed slot updates until a full resync (never on a first-attempt join); one of four client runs missed a single stake click (a timing flake).

**Round 3 (2026-10-21 to 2026-11-03).** On 2026-10-21 the maintainer ruled that the rest of this report is fixed too, together with the bugs found while fixing it; the older 2026-10-10 leftovers stay out of scope. Round 3 is built on `4558220` and is in the working tree, uncommitted. The player-facing changes are under `[Unreleased]` in `CHANGELOG.md`, the integrations are `INTEGRATIONS.md` #041 to #062, and the technical write-ups start at `STUB_CATALOGUE.md` (bg). It landed in ten waves:

- **W1 (2026-10-25/26):** the breaking crack (R3-CRACK Parts A to C), with RANB-PARITY (1.5.2's opaque and normal-cube answers in the Modern-Common base classes; leaves count as opaque) and RANB-PLACE (torches and ladders are refused where 4.B refused them); BTW's particles (R5B-PARTICLES).
- **W2 (2026-10-26/27):** falling blocks (R1-FALL), then R1-FIX: burning netherrack below y=0 falls burning, and an item or mob resting where a block swap raises the floor is lifted onto the new block instead of being pushed sideways (#060).
- **W3 (2026-10-27/28):** the dug face and the drop position (DR2-EJECT), then flowers and the collapsed-identity rule (FLOWERS).
- **W4 (2026-10-28):** mob targets, the player list and squids (MOBS-PLAYER); zombified villages, with no iron golem and no zombie siege (VILLAGES).
- **W5 to W9 (2026-10-28):** explosions (BLAST); strata, deepslate and raw ore (STRATA-ORE); the newer woods' sub-blocks and bamboo (WOOD-W1-BAMBOO); the BTW beacon and the magnetic compass (BEACON); 4.B's hardcore respawn, with FC's Nether flag set when a portal is lit (RESPAWN).
- **W10 (2026-10-28 to 2026-11-03):** the bugs found during the round (FOUND-R3): the wicker pane and the other lost 1.5.2 icons (#059); pressure plates by mob type, BTW arrows on wooden buttons, comparator readings of BTW containers and BTW's detector rails (#056); BTW's view of 1.20.1 minecarts, boats, paintings, experience orbs and mobs (#057); the redstone torch's 4.B placement (#055); campfire-lit Nether portals, in any frame 1.20.1 accepts (#058). Four fixes followed:
  - SOLIDTOP-FIX (2026-10-31): the bridge's solid top is 4.B's, the large centre hard point of FC's block at the cell, so a standing redstone torch is refused on glowstone and ice, and fire, snow and ash, plates, rails, mushrooms and FC's other callers read 4.B's answer (#061).
  - SUPPORT-VETO (2026-10-31, ruling of that day): weather snow and sneak-placed snow layers, pressure plates and rails take FC's support veto, at placement and for their survival, as torches and ladders do; the powered rail takes the rail's rule, and BTW's own torches, ladders, ash and soulforged steel plate, placed through 1.20.1's path, take FC's `canPlaceBlockAt` (#055).
  - SUPPORT-REL (2026-11-02, rulings 2026-10-31 and 2026-11-02): the newer pressure plates take the oak or stone plate's rule, and the 10 newer wooden fences, the 20 newer walls and the 16 stained glass panes answer every support question as the oak fence, the cobblestone wall and the glass pane, all ten fences as the oak fence even where BTW has a fence of that wood (#055, #062).
  - STICK (2026-11-03, ruling 2026-11-02): a placed tool sticks into them as into their relative, so an axe sticks into a newer wooden fence (#062).
  - Follow-ups: BTW's ash above such a fence or wall is drawn at the stand-in's offset, on top of the post, where above the oak fence it is drawn on the ground (FU-REL-DRAW); fence gates, stained and tinted glass, packed and blue ice, the other trapdoors, the newer leaves and the other wooden doors have no relative yet (FU-REL-FAMILIES); FC's own route accepts a powered rail anywhere, and the rail then drops at once (FU-RAIL27); generated snow is not vetoed, and goes at its first neighbour change (FU-WGSNOW).

**Round 3 rulings** (2026-10-21 to 2026-10-25), as far as they decide the rows below: flowers keep their kind; BTW mobs never take or keep a creative or spectator player as a target (1.20.1's rule), and everything else a creative player causes stays as in 4.B; villages are zombified as in 4.B, with no iron golem and no zombie siege; 4.B's hardcore respawn applies in every game mode except in worlds created as creative, a charged Nether respawn anchor still works, and a bound beacon overrides the random respawn; stone strata at 4.B's heights, with deepslate as the deep strata and raw iron and gold as ore chunks; bamboo planks by BTW's plank rule; the 4.B beacon; a block saved mid-fall is kept (4.B lost it); the exact 4.B web crack, with 1.20.1's crack images; the 1.20.1 explosion-decay gamerules decide for BTW blocks too, and new worlds start with `tntExplosionDropDecay` true.

**What round 3 verified.**
- Every wave built (exit 0). BridgeAudit found 0 high-severity issues, LinkAudit no new missing member, the Mixin config check passed (79 entries after W10's fixes), and `git ls-files --eol` was unchanged after every wave.
- Proof first: the round's plans first ran on `4558220` and recorded every gap. After its wave, each plan's gate passed (W10's once four fixes had landed, below), and the regression plans read SAME apart from the intended changes (one is P-PLACE's glass row, under CF2-P8a). From W5 on, each wave reran a smaller regression set; the full sweep ran once at the end (below).
- Final sweep (2026-11-03): every round-3 plan and every regression set (SET-FULL-S, SET-FINAL with P-DEV-A, SET-NETHER, the client plans, then SET-CRASH), 74 server runs and 22 client runs on the finished build, matched each plan's latest record; the statistical plant plans P-PL and P-PA stayed within their expected ranges.
- Joined development clients ran the client plans: the crack, the particles, falling blocks, drops, mobs, strata, the woods, the beacon, the respawn, and W10's redstone, wicker pane, textures and support relatives.
- The record is the session scratchpad's `fu_runs/BASELINE_RUNS_FU.md`, sections "Round 3, W1" to "Round 3, W10" and W10's "W10 solid-top fix" to "W10 guard batch (2026-11-03)", and "Round 3 final sweep (2026-11-03)" (not committed). W10's gate passed on 2026-11-03. It was first held on 2026-10-28: P-FOUND-RS 80/80, P-FOUND-CARTS 80/80, P-FPORTAL 38/38 and its client plans passed, but a standing redstone torch was still allowed on the top of glowstone and ice (P-FOUND-RT 20/24, rows R4, R7, R8 and R15; P-RANB-PLACE 115/117, rows C11 and V25). The cause was older than W10: `WorldBridge.doesBlockHaveSolidTopSurface` answered 1.20.1's sturdy-face test, where 4.B's `World` asks the block's `HasLargeCenterHardPointToFacing` (`vanilla/server/.../World.java:4786-4791`; correction 22). After the solid-top fix (#061): P-FOUND-RT 24/24 and P-RANB-PLACE 117/117. SUPPORT-VETO: P-SUPPORT-VETO 106/106 (58/106 before it landed, every GAP row failing). SUPPORT-REL: P-SUPPORT-REL 100/100 (50/100 before), and P-SUPPORT-VETO, with the three rows the relatives change amended, 106/106. STICK: P-TOOLSTICK-REL 38/38 (18/38 before), with mining speed and harvest unchanged. The guard batch, on the build with all four fixes: P-FOUND-RT 24/24, P-RANB-PLACE 117/117, P-FOUND-RS 80/80, P-FOUND-CARTS 80/80 and P-USE 39/39, and P-PLACE, P-RM, P-HOOK-CRASH and P-SMOKE1-post the same as their last records; joined clients P-SUPPORT-REL-C 17/17 (7/17 before), P-RANB-PLACE-C 13/13, placeconsume 30/30 and P-FOUND-RS-C 9/9, with no player input.

**Round 3, not verified at runtime:** the release jar, a launcher-installed client, multiplayer, worlds saved before round 3 (villages generated before it keep their villagers), and Embeddium, Oculus or shader packs.

**Status key.** *fixed*: the change is in the code (committed as `4558220`, or uncommitted in the working tree for round 3) and its proof rows pass; it does not mean it was seen in a normal game. *partly fixed*: part of the finding is still open, as stated. *planned*: queued work, not done. *not addressed*: no change, and the triage verdict stands (authentic, ruled out, or needs a ruling).

### By complaint

| # | Complaint | Status |
|---|---|---|
| 1 | Falling blocks are an orange outline | fixed in round 3 (CF2-R1, #043) |
| 2 | A yellow bed also dropped a red bed | fixed (CF2-DR1; doors too, CF2-DR6) |
| 3 | The cobweb crack looks stretched | fixed in round 3 (CF2-R3, #041): the exact 4.B crack, by ruling |
| 4 | Phantom items on shift-click | fixed (CF2-I2, through CF2-I1) |
| 5a | Placed blocks came back after dying | fixed (CF2-I3, through CF2-I1) |
| 5b | Loose snow slabs smoke | fixed in round 3 (CF2-R5b, #042): 4.B's faint white wisps, under 4.B's particle limits |
| 6 | Items shoot out after a break | fixed in round 3 (CF2-DR2, #044) |
| 7 | Mobs target players in creative | fixed in round 3 by ruling (CF2-M7, #046): BTW mobs never target creative or spectator players, 1.20.1's rule |
| 8 | Flowers on stone and slabs | fixed (CF2-P8a; CF2-P8b needed nothing separate) |
| 9 | An oxeye daisy dropped a poppy | fixed in round 3 (CF2-DR3, CF2-P8c, #045) |
| 10 | Gravel slabs never run out | fixed (CF2-I1) |
| PR | Deepslate, deepslate ores and newer woods are plain 1.20.1 | fixed: the woods (CF2-PR-WOOD, #034; their sub-blocks and bamboo in round 3, #051, #052); deepslate, the strata and raw ore in round 3 (CF2-PR-DEEP, CF2-PR-ORE, #050) |
| U1 | Can't sleep, no message, only the spawn is set | fixed by ruling 3: beds sleep and never set the spawn (CF2-U1); 4.B's hardcore respawn in round 3 (CF2-U1-HS, #054) |
| U2 | Creepers drop full blocks | fixed in round 3 (CF2-DR4, CF2-DR5, #049): a blast still drops full blocks, as in 4.B, but BTW's (loose dirt, loose cobblestone), with FC's blast reactions; mining charges drop their pieces |
| U3 | Zombified villagers | fixed in round 3 (CF2-MU3, #048): villages are zombified, as in 4.B |

### By finding

**Rendering**

| Finding | Status | What changed (workstream, date) | Proof | What remains / how to verify in game |
|---|---|---|---|---|
| CF2-R1 | fixed (round 3) | R1-FALL, W2 (2026-10-26), then R1-FIX (2026-10-27). An FC entity class with neither a `(World)` nor a no-argument constructor is built through its positional `(World, x, y, z, ...)` constructor, resolved once per class (`FcEntityFactory`), and the client simulates the fall as 1.5.2 did. `[SELF-AUDIT/renderer]` now also counts FC entities the client cannot build (`client/FCEntityRenderer.java:142`). By ruling (2026-10-22) a block saved mid-fall reloads and lands (`REBUILD_SAVED_POSITIONAL`, `ProxyEntity.java:64`), where 4.B lost it (correction 15). R1-FIX: burning netherrack below y=0 falls burning (`fcext.FCBlockNetherrackFallingMixin`). (`INTEGRATIONS.md` #043; `STUB_CATALOGUE.md` (bi).) | P-FALL 18/18; P-FALL-RESTART 2/2 + 2/2; joined client P-FALL-C 16/16 (7 falling blocks built and 7 drawn, 0 blinks on landing); DIAG-K3D 6/6 after R1-FIX; no `Could not create FC entity` in the server log | Dig out the block under loose dirt: it falls drawn as loose dirt and lands. Not done: smoke from a falling smouldering log or burning netherrack (FU-PE), and moving 1.5.2's void rule (a long fall that ends below y=1 drops as an item) to 1.20.1's floor (FU-VOID). |
| CF2-R3 | fixed (round 3) | R3-CRACK Parts A to C, W1 (2026-10-25/26), with RANB-PARITY. Part A gives a BTW quad with no normal of its own its plane's normal (`fc_quad_normals`); Part B gives the digging particles the icon of the block's own metadata; Part C draws the breaking crack of a block FC draws with FC's own `RenderBlockWithTexture`, as 4.B did (`fc_crack_4b`, `client/FCBakedModel.java:688`; ruling 2026-10-22: the exact 4.B look, with 1.20.1's crack images). RANB-PARITY's 1.5.2 answers let BTW's panes crack on the pane. (`INTEGRATIONS.md` #041; `STUB_CATALOGUE.md` (bg).) | Joined client P-CRACK-C, gate OK at stage ABC (K1-K5, KG and KB pass); the quad dump is identical between stages AB and ABC (36943 lines); P-RANB 117/117 | Hold left click on a web a spider has spat: the crack covers each diagonal plane, unstretched. The crack images are 1.20.1's, so resource packs change them. Not tested with Embeddium, Oculus or shader packs. |
| CF2-R5b | fixed (round 3) | R5B-PARTICLES, W1 (2026-10-25/26). One table maps FC's particle names for every FC world; FC's own effects (white smoke, white cloud, small flame, cinders) are BTW particle types that port 4.B's; and FC's client particles keep 4.B's limits, so the snow's wisps vanish beyond 16 blocks and at the Minimal setting (`FcParticles.addLocal`, `FcParticles.java:168`; `send`, `:196`). (`INTEGRATIONS.md` #042; `STUB_CATALOGUE.md` (bh).) | P-R5B-T 32/32 (19/32 before); joined client P-R5B-C 35/35 (16/35 before) | Next to loose snow, set Particles to Minimal: the wisps go; at All they are faint white puffs. Not done: FC entities' client-only particles, such as a falling smouldering log's smoke and villager and witch effects (FU-PE). |

**Drops**

| Finding | Status | What changed (workstream, date) | Proof | What remains / how to verify in game |
|---|---|---|---|---|
| CF2-DR1 | fixed | BD, 2026-10-20 (ruling: beds and doors take 1.20.1 drops). For the 16 beds and the oak and iron doors the FC harvest is skipped and vanilla `destroyBlock` runs; an FC drop at a live bed or door cell is that cell's 1.20.1 loot. FC keeps its dig speed, harvest exhaustion, tool wear and animal panic (`VanillaPairDrops.isRuled`, `VanillaPairDrops.java:95`; `INTEGRATIONS.md` #039; `STUB_CATALOGUE.md` (bb)). | P-BEDDOOR 288/288 (GAP 63/63; 225/288 on the tree before); `[SELF-AUDIT/pairdrops] 18 ruled blocks (16 BedBlock, 2 DoorBlock, 0 other); ... 0 warnings (0 expected)` | Break the foot of a yellow bed in survival: one yellow bed; in creative: nothing. The bed codec of the fix direction is no longer needed for drops (it stays wanted for FC reads such as ocelots sitting on beds). |
| CF2-DR6 | fixed | As CF2-DR1. | The door rows of P-BEDDOOR: oak and iron doors, either half, by hand, by tool and in creative, the BTW saw on either half, a mining charge, a piston | Break an oak door's top half: one door. A door whose top half burns first may drop itself (1.20.1; 4.B dropped nothing). |
| CF2-DR2 | fixed (round 3) | DR2-EJECT, W3 (2026-10-27/28). FC's harvest gets the face the player dug, from the dig packet and kept for a delayed break as 1.5.2 kept it, else the look ray, else up (`DigFace.of`, `DigFace.java:41`; `fc_dig_face`), so every FC conversion ejects through that face; and an FC drop spawns with 1.5.2's box, not 0.125 higher (`EntityProxyFactory.java:244`; `fc_drop_position`). This closes #034's upward chew-eject. (`INTEGRATIONS.md` #044; `STUB_CATALOGUE.md` (bj).) | P-DR2 19/19 (9/19 before); joined client P-DR2-C 2/2 | With a stone pickaxe, mine the lower block of a 1x2 tunnel face: the stones and the gravel pile come out toward you. 1.20.1 still pushes an item out of any block it overlaps (`CURSEFORGE.md`, Hardcore Buoy). Not done: the yOffset of FC's other plain entities (FU-YOFF). |
| CF2-DR3 | fixed (round 3) | FLOWERS, W3 (2026-10-27/28; the maintainer's default: every flower keeps its own kind). Beds had left its scope with BD. The rose's ten small relatives (blue orchid, allium, azure bluet, the four tulips, oxeye daisy, and now cornflower and lily of the valley) are subtypes 38:1-10 (`LegacyVariants.java:197`; `flower_subtypes`); an FC self-drop takes the identity of the block it came from (`CollapsedIdentity.resolve`, `CollapsedIdentity.java:131`; `collapsed_identity`); the port's vanilla block items carry 1.5.2's subtype flag again (`item_subtypes`). Wool, slabs, sandstone, walls, quartz and mob heads now drop their own kind, and wet sponge, the stone slab and smooth sandstone drop themselves. (`INTEGRATIONS.md` #045; `STUB_CATALOGUE.md` (bk).) | P-FLOWERS 107/107 (41/107 before); `[SELF-AUDIT/identity] 51 blocks keep their identity ... 0 warnings (0 expected)` | Punch an oxeye daisy, or dig out the grass under a tulip: the flower drops as itself. Not done: wet sponges, carved pumpkins, stone slabs, smooth sandstone and coloured beds still turn into their 1.5.2 form in BTW storage and when BTW places them (F-COLLAPSE-ITEMS); stone, cobblestone, ore and snow items and a few others still lack 1.5.2's subtype flag (F-SUBTYPES). The wither rose and the torchflower keep 1.20.1's rules (lore ruling). |
| CF2-DR4 | fixed (round 3) | BLAST, W5 (2026-10-28; ruling 2026-10-22, D1 = C: the 1.20.1 decay gamerules decide, and new worlds start with `tntExplosionDropDecay` true, 4.B's 1 in 4 for TNT). A blasted cell whose block FC has a blast or drop rule for runs 1.5.2's `doExplosionB` body: `canDropFromExplosion`, the drop at the blast's chance, the removal, then FC's `onBlockDestroyedByExplosion` (`BlastRules.decide`, `BlastRules.java:314`; `destroy`, `:181`; `fc_blast_rules`). Blast resistance reads FC's positional value, so deep strata resist more, and 1.5.2's resistance floor and dirt loosening are ported (Modern-Common `Block.java:273`, `:1604`). (`INTEGRATIONS.md` #049; `STUB_CATALOGUE.md` (bn).) | P-BLAST 79/79 (fresh world); `[SELF-AUDIT/blast] ... 0 warnings (0 expected)` | Blow up TNT on grass, dirt and stone: loose dirt, loose cobblestone, stones and gravel piles, no vanilla dirt or cobblestone, and the dirt around the crater loosens. Full blocks from a creeper blast are 4.B. Not done: TNT that another blast sets off is 1.20.1's TNT (BTW's primed TNT is a stub); blocks with a 1.20.1 block entity, beds, doors and the other collapsed variants keep 1.20.1's blast; blasted infested stone releases silverfish and nether quartz ore gives experience, as in 1.20.1. Worlds created before round 3 keep `tntExplosionDropDecay` false. |
| CF2-DR5 | fixed (round 3) | BD ported 1.5.2's `DropItemsOnDestroyedByExplosion` body behind `World.UsesModernBlockLoot`, for beds and doors only (2026-10-20). Round 3's BLAST (W5, 2026-10-28) runs that body for every block (Modern-Common `Block.java:1580`) and ports the dirt-loosening bodies. (`INTEGRATIONS.md` #049; `STUB_CATALOGUE.md` (bn).) | The mining-charge rows of P-BEDDOOR (MC-OAK, MC-IRON and MC-BEDN: 0 -> 1); P-BLAST's mining-charge rows MC1 to MC10, within P-BLAST 79/79 | Light a mining charge against stone: 5 stones and 3 gravel piles; against dirt, gravel or sand: 6 piles; against a log: sawdust and bark. A charge no longer deletes water or lava. |

**Inventory**

| Finding | Status | What changed (workstream, date) | Proof | What remains / how to verify in game |
|---|---|---|---|---|
| CF2-I1 | fixed | INV-RESYNC, 2026-10-20 (ruling: fix it first), fix direction 1 plus fix 4. A sync keeps FC's live stack while the real slot is unchanged, writing FC's pending edit through first (`InventoryBridge.sync`, `InventoryBridge.java:63`; `keepLive`, `:93`); the write-back no longer adopts a detached stack; an FC call whose edits must not land drops them explicitly (a discard after a throw, a shadow scope); the held slot is resent after a BTW placement (`resendHeldSlot`, `:308`). A review fix keeps a creative player's last item through nested syncs (`STUB_CATALOGUE.md` (az)). | Joined client `placeconsume` 30/30 (GAP 7/7; 0/7 at HEAD), the client's hand matching the server; 30/30 again after I-1, BD and dye3, and wave C | In survival, without sneaking, place gravel slabs on a gravel slab: the count drops. |
| CF2-I2 | fixed | Through CF2-I1, plus the held-slot resend. | G2: the server used one, and the client shows the server's count (`hand minecraft:cobblestone x3`) | Place cobblestone on dirt without sneaking, then shift-click in the inventory: nothing appears. |
| CF2-I3 | fixed | Through CF2-I1. | G4 x2: the death pile holds the 3 left, and no fourth | Place 3 of 4 dirt on grass without sneaking, then `/kill`: the pile holds 1 dirt. |
| CF2-I4 | not addressed (ruled out: no bug) | none | none | none |
| CF2-I5 | fixed | Through CF2-I1. | L1: a lever placed while airborne on a ladder uses one, with `OnGround:0b` witnessed before and after the click | From a ladder, without sneaking, place a rail on dirt: the count drops. |
| CF2-I6 | closed: it did not happen | INV-RESYNC landed before I-1, and I-1 was rebased onto it (2026-10-20). | S1: a stake placed through the I-1 item step uses one (joined client, after I-1) | The repro's stake id is `betterthanwolves:item_22491` (dated note under the repro). |
| CF2-P10 | fixed-since 1.4.0-alpha.5; its symptom ended with CF2-I1 | none | P10-1 (`use SUCCESS left=3`, P-INV-S 3/3) | `CHANGELOG.md` folds it into the CF2-I1 fix. |

**Placement**

| Finding | Status | What changed (workstream, date) | Proof | What remains / how to verify in game |
|---|---|---|---|---|
| CF2-P8a | fixed | I-2, 2026-10-21. `WorldBridge.canPlaceEntityOnSide` (`WorldBridge.java:1957`) is 1.5.2's rule: a free or replaceable target, the entity check with the placing player counted, and `canPlaceBlockOnSide`. A REFUSED Stage 2 is final (FAIL and a held-slot resend), so 1.20.1's BlockItem no longer places instead (`INTEGRATIONS.md` #040; `STUB_CATALOGUE.md` (bd)). | P-PLACE 27/29 (8/29 before): P8-1 to P8-3 GAP pass, P8-4 and P8-5 CONTROL pass, the 2 FAILs are the KNOWN rows H4 (glass) and H14 (BTW-placed stairs), and H4 passes since round 3's RANB-PARITY (W1) restored 1.5.2's answer that glass is not a normal cube; the place sweeps now refuse saplings, grass, fern, dead bush, flowers, mushrooms, ladder and cactus on stone's top; joined client `P-PLACE-C` | Without sneaking, right-click the top of stone with a poppy: nothing is placed. Other mods' fake players keep 4.B's mid-air rule (maintainer, 2026-10-20). |
| CF2-P8b | ruled out; closed with CF2-P8a | none | none | Flowers that earlier versions placed on stone or slabs pop, as in 4.B. |
| CF2-P8c | fixed (round 3) | FLOWERS, W3, with CF2-DR3: a flower placed through BTW's code keeps its subtype (the 38 case of Modern-Common `ItemBlock.getMetadata`, `ItemBlock.java:157-158`; `LegacyVariants.java:197`). (`INTEGRATIONS.md` #045; `STUB_CATALOGUE.md` (bk).) | P-FLOWERS 107/107 | Without sneaking, place a tulip on grass: it stays a tulip. |

**Mobs**

| Finding | Status | What changed (workstream, date) | Proof | What remains / how to verify in game |
|---|---|---|---|---|
| CF2-M7 | fixed (round 3, by ruling) | MOBS-PLAYER, W4 (2026-10-28; ruling 2026-10-21: 1.20.1's rule; 2026-10-22, D1 = (a): only targeting changes). Before every FC mob update, a player 1.20.1 refuses as an enemy (creative or spectator, among others) is dropped from every target slot and from the AI tasks that hold it, and a hit, anger, a startled wolf or wolf meat never makes such a player a target (`FcTargetRule.isTargetable`, `FcTargetRule.java:29`; `sweep`, `:53`; `creative_targets`). This supersedes the section's "Fix direction: none needed" (correction 18). (`INTEGRATIONS.md` #046; `STUB_CATALOGUE.md` (bl).) | P-MOBS 65/65; joined client P-MOBS-C 11/11 | Get chased by a BTW zombie in survival, then switch to creative: it stops. Kept as in 4.B (D1 = (a)): in creative, a wild wolf you hit or fed wolf meat still turns hostile to survival players, animals still panic and endermen still scream. Spectators are still inside FC's entity box queries (a general follow-up). |
| CF2-MU3 | fixed (round 3) | VILLAGES, W4 (2026-10-28; rulings 2026-10-21: 4.B's zombified villages; 2026-10-22: no iron golem in any new village; 2026-10-23: no zombie siege, as in 4.B). A fresh villager or zombie villager that a village places becomes a persistent BTW villager zombie with its building's class; the village's distance to the world spawn decides, with FC's radii, how abandoned it is (nearly empty within 2250 blocks, farmers, blacksmiths and butchers out to 3000, every class beyond; x4 on large biomes); a world created as creative keeps villagers (`VillageInhabitants.decide`, `VillageInhabitants.java:167`; `zombified_villages`). New villages get no iron golem, and 1.20.1's zombie siege does nothing (`VillageSiegeMixin`; `village_sieges`; correction 16). The soul-urn cure works: `WorldBridge.removeEntity` now kills the FC entity and finds puppets (`WorldBridge.java:2069-2081`; correction 17). (`INTEGRATIONS.md` #048; `STUB_CATALOGUE.md` (bm).) | P-VCURE 19/19, P-VPLACE 23/23, P-VSIEGE 16/16, P-VNAT 11/11, P-VLB 5/5 | Find a village more than 3000 blocks from spawn: it holds villager zombies; throw a soul urn at one and it is cured into a villager. Villages generated before round 3 keep their villagers. Igloo basement zombie villagers stay plain BTW zombies (a separate ruling). |
| CF2-MX1 | fixed (round 3) | MOBS-PLAYER, W4 (2026-10-28). `World.playerEntities` is a live view of the level's players, spectators left out as 1.20.1's entity queries leave them out (`PlayerEntitiesView`, `WorldBridge.java:2874`), so BTW squids hunt players again, the mill stone's netherrack nausea reaches them, and so do beacon effects since BEACON (W8, `INTEGRATIONS.md` #053; `STUB_CATALOGUE.md` (bq)). (`INTEGRATIONS.md` #047; `STUB_CATALOGUE.md` (bl).) | P-MOBS 65/65; P-BEACON 66/66 for beacon effects | Stand within 10 blocks of a mill stone grinding netherrack: you get nausea. |
| CF2-MX2 | fixed (round 3) | MOBS-PLAYER, W4. `WorldBridge.getClosestVulnerablePlayer` (`WorldBridge.java:2931`) is 4.B's lookup on 1.20.1's terms: it skips players 1.20.1 refuses as enemies and scales the range by 1.20.1's `getVisibilityPercent` (sneaking x0.8; invisibility x0.7 times the armour cover). (`INTEGRATIONS.md` #046; `STUB_CATALOGUE.md` (bl).) | P-MOBS 65/65 | Pen a BTW zombie and stand 14 blocks away, sneaking: it ignores you. |
| CF2-MX3 | fixed (round 3) | MOBS-PLAYER, W4. Modern-Common `EntityPlayer` has 4.B's three squid hooks. A squid on a player's head rides along (`FcRiding`), weighs the player down and knocks them out of a boat, minecart or saddle; its bite goes through armour, except a soulforged steel helm (damage type `betterthanwolves:mob_attack_unblockable`); tentacles and a BTW cow's kick fling the player; a squid disarms a player who blocks with a BTW sword or tool (`head_crab`). (`INTEGRATIONS.md` #047; `STUB_CATALOGUE.md` (bl).) | P-MOBS rows C10-C17; joined client P-MOBS-C, row CL9 | At night in water, hit a BTW squid: it attacks and can latch onto your head. Not done: a squid on your head is drawn as a free squid; a 1.20.1 shield is never knocked out (4.B had none). |

**Beds and respawn**

| Finding | Status | What changed (workstream, date) | Proof | What remains / how to verify in game |
|---|---|---|---|---|
| CF2-U1 | fixed (by ruling 3) | I-3, 2026-10-21. `PlayerMixin.btw$disableBeds` is deleted; `BedSpawnPolicy.onSetSpawn` (`BedSpawnPolicy.java:60`) vetoes non-forced bed spawns; an FC hostile within 8 blocks gives NOT_SAFE (`ServerPlayerMixin.btw$fcHostilesPreventRest`, `ServerPlayerMixin.java:347`); beds sleep with 1.20.1's messages (`INTEGRATIONS.md` #037; `STUB_CATALOGUE.md` (be)). Part (a) is now a deliberate difference from 4.B. | P-HOOK P2g, P2n, P2h (was the ClassCastException) and P2m pass, with the controls P2o (anchor) and P2a (a Nether bed explodes); P-HOOK GATE C OK 34/36; joined client `P-BED-C`: night skipped, no bed spawn; no ClassCastException in the logs | At night, right-click a bed: you sleep and the night skips; `/kill` returns you to the world spawn, not the bed. Since round 3, `/kill` puts you at 4.B's random respawn spot instead (CF2-U1-HS), still never at the bed. |
| CF2-U1-HS | fixed (round 3) | RESPAWN, W9 (2026-10-28), on BEACON (W8) (rulings 2026-10-21: port 4.B's hardcore respawn, a charged Nether respawn anchor still works, a bound soulforged steel beacon overrides the random respawn; 2026-10-22: every game mode, except in worlds created as creative). At `PlayerList.respawn`, a point 1.20.1 owns (an anchor, `/spawnpoint`) stays 1.20.1's; FC's beacon bind is checked by 4.B's `GetValidatedRespawnCoordinates`; otherwise 4.B's `HandleHardcoreSpawn` runs against 1.20.1 terrain: a random spot 1000-2000 blocks from the original spawn (x1.5 after the Nether, x2 after the Wither, x4 after the End, x4 on large biomes), the soft respawn, the early shared respawn and the End exit to the original spawn (`HardcoreRespawn.onRestore`, `HardcoreRespawn.java:244`; `appliesTo`, `:578`; `hardcore_respawn`, `creative_world`). Lighting a Nether portal sets FC's Nether flag at once (`NetherPortalProgress`). The beacon bind works since BEACON (`INTEGRATIONS.md` #053; `STUB_CATALOGUE.md` (bq)). `CURSEFORGE.md` Known Differences now describes the respawn. (`INTEGRATIONS.md` #054; `STUB_CATALOGUE.md` (br).) | P-HCR 53/53, P-HCR-CW 26/26, P-HCR-LB 8/8, P-HCR-PERSIST 1/1 + 2/2, P-HCR-BEACON 9/9, P-BEACON 66/66; joined client P-HCR-C 6/6; `[SELF-AUDIT/hcspawn] ... 0 warnings` | Note the world spawn and `/kill`: you respawn 1000 to 2000 blocks away; `/kill` again at once: you come back near that spot with half health. With the daylight cycle off the one-day windows never end; slow terrain generation can fall back to the world spawn (a 3-second budget per respawn); 4.B's mystery meat drop is not ported. |

**Progression**

| Finding | Status | What changed (workstream, date) | Proof | What remains / how to verify in game |
|---|---|---|---|---|
| CF2-PR-WOOD | fixed | WOOD-A and WOOD-B, 2026-10-20 (rulings 1 and 2 of 2026-10-19). Acacia, dark oak, mangrove and cherry are FC wood types 5-8 on log2 at legacy id 165 (`ModernWoods.LOG2_ID`, `ModernWoods.java:60`): chewing, bark 5-8 and tanning, work stump, saw, chopping and fuel, with their log -> 4 planks recipe removed. Stems and modded logs follow the log rule (`FCLogRules.isLogRuleTarget`, `FCLogRules.java:319`); stems give no bark and leave no work stump (`INTEGRATIONS.md` #034; `STUB_CATALOGUE.md` (ay)). The new species' leaves drop by BTW's leaf rule (LEAF, #035). Round 3 (WOOD-W1-BAMBOO, W7, 2026-10-28; bamboo ruled 2026-10-21 and 2026-10-22): the four woods' planks are FC planks, with BTW's sidings, mouldings, corners and other wood blocks at ids 1093-1100 (`ModernWoodSubBlocks`; `modern_wood_subblocks`, `INTEGRATIONS.md` #051), and bamboo blocks give planks through the saw (2 planks and sawdust) and the axe chopping recipe, with no bark or work stump and no crafting recipe (`bamboo_planks`, #052; `STUB_CATALOGUE.md` (bp)). | wood_p1 67/67 (14/67 at HEAD), mlog_p1 73/73 (18/73), dt_wood 4/4, dt_p2 27/27; clients `wood_c1` and `mlog_c1`; round 3: P-W1 85/85, joined client P-W1-C (10 probes as expected) | Check: punch an acacia log by hand: it chews and gives bark, and no whole log drops. Saw a bamboo block: 2 bamboo planks and sawdust. Crimson, warped and bamboo planks stay Minecraft planks (ruling 2026-10-22). |
| CF2-PR-DEEP | fixed (round 3) | STRATA-ORE, W6 (2026-10-28; rulings 2026-10-21: 4.B's heights, deepslate as the deep strata; 2026-10-22: mined deepslate gives loose cobblestone, and depth decides the hardness of all stone below y=48). Stone and ores read their strata from their position at 4.B's heights (deep at y=24 and below, mid at y=48 and below, with 4.B's random one-block edge), on both sides, so the client's dig prediction agrees (`StrataRule.strataAt`, `StrataRule.java:52`; `strata`). Deepslate, cobbled deepslate and the deepslate ores are BTW deep stone, cobblestone and ores; blasts read the positional resistance (#049). (`INTEGRATIONS.md` #050; `STUB_CATALOGUE.md` (bo).) | P-STRATA, 153 checks: 152 pass, and E16[OD3a] fails by design (the ruling took the other option; gate OK); joined client P-STRATA-C: the client's dig progress equals the server's in all six cells | With the same iron pickaxe, dig stone at y=60 and at y=10: the deep one takes longer. Mined deepslate gives loose cobblestone, never cobbled deepslate. Not done: darker deep-strata textures (a queued follow-up). Granite, diorite, andesite, tuff and calcite stay Minecraft stone (ruling 2026-10-22). |
| CF2-PR-ORE | fixed (round 3) | STRATA-ORE, W6 (rulings 2026-10-21: raw ore is ore chunks; 2026-10-22: nether gold ore loses its furnace and blast-furnace recipes). Raw iron and raw gold count as BTW's iron and gold ore chunks (`raw_ore_chunks`): FC's chunk cooking applies, the `minecraft:` ingot recipes of raw ore and the deepslate ores are removed, and a raw iron or raw gold block unpacks into nine chunks. (`INTEGRATIONS.md` #050; `STUB_CATALOGUE.md` (bo).) | P-STRATA, as above; the P-RM recipe dump lists no raw-ore or deepslate cooking recipe | Smelt raw iron: it cooks into an iron nugget, as BTW's iron ore chunk does. Other mods' machines can still turn raw ore into ingots (only `minecraft:` recipes are checked). |

### Corrections to the record: status

| # | Correction | Status |
|---|---|---|
| 1 | R6 premise | partly closed (round 3): village villagers and zombie villagers become villager zombies since #048, where the R6 rule no longer applies; the R6 comment (`VanillaMobReplacer.java:504-508`) still states the premise, which now holds only for zombie villagers outside villages, such as igloo basements (a separate ruling) |
| 2 | Falling-block rendering listed as done | closed (round 3): falling blocks draw (CF2-R1, #043), and `[SELF-AUDIT/renderer]` now also counts FC entities the client cannot build (`client/FCEntityRenderer.java:142`) |
| 3 | "Placing against any other block uses BTW's code"; the ported-steps list in `STUB_CATALOGUE.md` (ap) | closed: true since I-2; the CHANGELOG sentence now names 4.B's placement checks, and (ap) has a dated note pointing to (bd) |
| 4 | `BRIDGE_SPEC.md` calls the bed hook an `@Overwrite` | closed: the sleep section (2.6) is rewritten, and the hook is deleted |
| 5 | `CURSEFORGE.md` "Sleep costs you." and "Hardcore spawn, costly sleep" | closed: both are removed, and Known Differences said that 4.B's hardcore respawn is not ported; since round 3 it describes the ported respawn (CF2-U1-HS, #054) |
| 6 | `INTEGRATIONS.md` #007 cites "FC's HC Spawn (integration `#002`)" | closed: the Conflict rows of #007 and #009 (which made the same citation) now name FC's hardcore spawn, `FCUtilsHardcoreSpawn`, with a dated correction, and say that it is not ported (CF2-U1-HS); round 3 ports it (#054) |
| 7 | The follow-up plan's "FC beacon (forced)" comments | closed in the code: the `BedSpawnPolicy` javadoc and its `onSetSpawn` comment now say the beacon bind is not forced (`Server/.../FCBlockBeacon.java:93`) and is left alone because a beacon is not a bed. The plan files are outside the repo and keep the old wording |
| 8 | The queued "upward chew-eject" names only chewing | closed (round 3): DR2-EJECT gives every FC conversion the dug face (CF2-DR2, #044) |
| 9 | The collapse bullets | partly closed: the dated note under `STUB_CATALOGUE.md` (ai) records that the dyes no longer collapse, that beds and doors keep 1.20.1's item and colour, and that the flowers still collapse; round 3 closed the flowers (CF2-DR3, CF2-P8c, #045). Still open: wet sponge, carved pumpkin, the stone slab, smooth sandstone and coloured beds in BTW storage and BTW placement (F-COLLAPSE-ITEMS) |
| 10 | `STUB_CATALOGUE.md` (z) records the nested re-sync as "Pre-existing" | closed: retired by INV-RESYNC ((az); dated note under (z)) |
| 11 | The P0 override-point table omits `DropItemsOnDestroyedByExplosion` | closed: a dated note in the P0 introduction points to (bb), which ports its body for beds and doors |
| 12 | The prior hook audit's "rethrows" | outside the repo; open |
| 13 | CF2-P10 "None at 1.4.0-alpha.5 and later" | closed: the symptom ended with CF2-I1 |
| 14 | (new, 2026-10-20) The CF2-I6 repro names the stake by its FC ParseID (22235), not by its registered id | recorded in a dated note under the repro |
| 15 | (new, round 3) CF2-R1's reload note: 4.B also lost a block saved mid-fall, because 1.5.2 `EntityList.createEntityFromNBT` builds a loaded entity only through a `(World)` constructor and logs `Skipping Entity with id FallingSand` (`vanilla/server/.../EntityList.java:122`, `:136`); and its `/summon` repro cannot show the loss, because a summoned `fc_falling_block` carries no `FCClassName` | recorded here; by the ruling of 2026-10-22 the port keeps such a block (#043) |
| 16 | (new, round 3) CF2-MU3: "Sieges exist in 1.5.2 too". FC removed the siege from 4.B (`vanilla/server/.../World.java:105-107`, `WorldServer.java:183-185`) | recorded here; since #048 the port has no zombie siege either |
| 17 | (new, round 3) CF2-MU3's prerequisite, "prove the soul-urn cure": the cure was broken in the port, because `WorldBridge.removeEntity` neither killed the FC entity nor found puppets. Its repro cannot pass either: `/place structure` never registers the village's start, so `getStructureWithPieceAt` finds no village | closed (round 3): `removeEntity` is fixed (`WorldBridge.java:2069-2081`), and `StructureStartPlacementMixin` names the start while a structure is placed (#048; P-VCURE 19/19, P-VPLACE 23/23) |
| 18 | (new, round 3) CF2-M7's "Fix direction: none needed" | superseded by the ruling of 2026-10-21 (#046) |
| 19 | (new, round 3) CF2-U1-HS's mechanism: 4.B's beacon bind is the player's spawn point (`setSpawnChunk`, `Server/.../FCBlockBeacon.java:93`), and `m_HardcoreSpawnChunk` is the centre of the soft respawn, which `FCUtilsHardcoreSpawn` assigns (`Server/.../FCUtilsHardcoreSpawn.java:258`, `:301`, `:330`); the port had merged the two. And the End exit it cites (`:111-116`) is dead code: 4.B never calls `HandleHardcoreSpawn` for a player leaving the End, and keeps that player's respawn point (`vanilla/server/.../ServerConfigurationManager.java:1144-1148`, `:1182-1184`) | recorded here; #054 follows 4.B's reading |
| 20 | (new, round 3) CF2-MX1's beacon notes: the `ClassCastException` ("invalid for ticking") was `0292e38`'s; at `4558220` the beacon raised no error and still never ticked. Its fix was not a `ProxyBlockEntity`, which would have replaced 1.20.1's beacon block entity, its beam and every saved beacon: #053 keeps that block entity and attaches FC's tile entity to it | recorded here |
| 21 | (new, round 3) CF2-DR3 and rank 10 of the Mapping: "the same rule covers wet sponge and carved pumpkin". No drop rule covers the carved pumpkin: its collapse is the item, which BTW storage and BTW placement read as a pumpkin | open (F-COLLAPSE-ITEMS, with wet sponge) |
| 22 | (new, round 3) `STUB_CATALOGUE.md` (ag) records as a caveat that `WorldBridge.doesBlockHaveSolidTopSurface` uses 1.20.1's `isFaceSturdy(UP)`, "a little more permissive than 1.5.2 (ground cover can rest on glass)". It was a 4.B parity bug: 4.B's `World` asks FC's block at the cell for its large centre hard point on top (`HasLargeCenterHardPointToFacing(..., 1)`, `vanilla/server/.../World.java:4786-4791`), and FC's callers read that answer (the redstone torch, fire, snow and ash, plates, rails, mushrooms and others), so the port let a standing redstone torch stand on glowstone and ice (W10's held rows: P-FOUND-RT R4, R7, R8 and R15; P-RANB-PLACE C11 and V25) | closed (round 3, #061): `WorldBridge.doesBlockHaveSolidTopSurface` (`WorldBridge.java:2834`) and `ClientFcWorld` (`client/ClientFcWorld.java:163`) answer 4.B's hard point, with 1.20.1's face only where FC reads air, and HardcoreRespawn's beacon search asks it (`HardcoreRespawn.java:338`); P-FOUND-RT 24/24, P-RANB-PLACE 117/117 |

---

## 1. Rendering: falling blocks, the cobweb crack, loose-snow smoke

> "Falling blocks texture is missing when actively falling (only an orange outline remains until it is no longer in a falling state)" · "Cobweb breaking texture seems oddly stretched?" · "Are loose snow slabs suppose to emit smoke particles??"

The skeptic re-derived all three from source, from the shipped jar and from the dev logs. All three are still at HEAD and in every public build. No CHANGELOG `[Unreleased]`, STUB_CATALOGUE, earlier-triage or `followups_recon.md` entry covers any of them.

### CF2-R1. Falling blocks draw as an orange debug box (confirmed, medium)

- **Effect:** every falling block is affected: loose dirt, loose cobble, BTW slabs and loose snow, and since `9c05019` also sand, gravel and the dragon egg. It is cosmetic but constant.
- **Mechanism:**
  - Forge's spawn handler calls `ProxyEntity.readSpawnData` (1.20.1 `PlayMessages.java:165-171`), which calls `ensureFcEntity` (`ProxyEntity.java:474-482`).
  - `ensureFcEntity` tries only `getConstructor(World)`, then `getDeclaredConstructor()` (`ProxyEntity.java:80-109`; `:94`, `:97`). `FCEntityFallingBlock` declares only `(World, double, double, double, int, int)` (`Common/.../entity/FCEntityFallingBlock.java:33-36`; `javap` of the 1.6.3 jar agrees).
  - Both lookups throw. The failure is logged once per class (`FCPuppetSupport.firstFailure`, `:106`) and `fcEntity` stays null. `FCEntityStateCodec.applyState(buf, null)` then drains the payload and applies nothing (`FCEntityStateCodec.java:116-131`).
  - Every frame, `FCEntityRenderer.render` calls `getFcEntity()`, which retries `ensureFcEntity` (`ProxyEntity.java:61-66`). That is a new dummy client World, a `Class.forName` and two reflective exceptions per falling block per frame. The renderer then calls `renderDebugBox` (`FCEntityRenderer.java:283-285`): an orange `RenderType.lines()` wireframe of the 0.98 box, colour (1.0, 0.5, 0.0) (`:581-591`).
  - The server runs FC's fall, places the block on landing and discards the proxy. That is why the textured block appears only on landing.
- **Why nobody noticed:** the start-up check passes falsely. `latest.log` (2026-10-18 10:38) says `[SELF-AUDIT/renderer] 54 registered FC entities, 0 missing a renderer`. The audit (`FCEntityRenderer.java:125-143`) checks only the renderer map, and the complete port of `RenderFallingSand` (`Modern-Common/.../RenderFallingSand.java:27-73`) is registered. The dev logs record "Could not create FC entity" only for early-build mobs (bat, chicken, creeper, enderman, pig, skeleton, spider, squid, zombie), never for `FCEntityFallingBlock`. So no recorded dev session had a falling block on screen.
- **Two routes, split by build date (not version label):**
  - **Before `9c05019` (2026-10-15 10:00).** FC-id falling blocks spawned the frozen `btw.modern.EntityFallingSand` (`48e9825` `Modern-Common/.../Block.java:615`; `ba91232` `:693`). That class is unregistered, so it maps to the generic `fc_entity` type (`EntityProxyFactory.java:185-191`). The client can build it, but nothing renders it:
    - FC registers `RenderFallingSand` only for `FCEntityFallingBlock` (`Client/.../core/FCBetterThanWolves.java:2440`);
    - the `RenderManager` stand-in registers only `RenderItem` and `RenderXPOrb` (`Modern-Common/.../RenderManager.java:18`, `:23`);
    - `registerMissingVanillaRenderers` has no entry for it (`ba91232` `FCEntityRenderer.java:209-218`).
    
    So the debug box was drawn. Vanilla sand and gravel still used MC's `FallingBlockEntity` and looked right.
  - **From `9c05019` on.** `CheckForFall` spawns `FCEntityFallingBlock` for every falling block (`Modern-Common/.../Block.java:710-744`, `:756-777`; `CHANGELOG.md:276-278`). `javap` finds `CreateFallingBlockEntity` in the 1.4.1-alpha.5 (rebuilt), 1.5.1, 1.6.1, 1.6.2 and 1.6.3 jars. Only 1.4.1-alpha.6 (2026-10-11) lacks it.
  
  Whichever jar is on CurseForge, the orange box shows.
- **Rare side effect: a block saved mid-fall is lost.** Chunk load goes through the same `ensureFcEntity` (`readAdditionalSaveData`, `ProxyEntity.java:515-520`) and gets `fcEntity == null`. Its tick then does nothing (`:317-347`), and `syncFromFc` returns early, so it is never discarded (`:302-309`). It re-saves only `FCClassName` (`:540-542`). The block is lost, and an inert orange box stays for ever.
- **Not a visible case:** `FCEntityLightningBolt` also lacks a `(World)` constructor (`FCEntityLightningBolt.java:35`), but nothing in the port spawns it. The only spawn site is the unused `vanilla/server/.../WorldServer.java:327`. The stand-in `World.addWeatherEffect` returns false (`Modern-Common/.../World.java:549-551`), and `fc_lightning_bolt` is sized 0x0 (`BTWEntityRegistration.java:183`). It is latent.
- **4.B:**
  - The server sends Packet23 type 70 with `blockID | meta << 16` (`vanilla/server/.../EntityTrackerEntry.java:554-557`).
  - The client builds `new FCEntityFallingBlock(world, x, y, z, id & 65535, id >> 16)` (`vanilla/client/.../NetClientHandler.java:290-296`), simulates the fall itself and draws it through `RenderFallingSand` (`FCBetterThanWolves.java:2440`).
  - The entity is hidden only once its block id sits at its own cell (`vanilla/client/.../Block.java:3735-3744`).
  - 1.20.1's own `FallingBlockRenderer` (`:29-44`) draws a falling block the same way.
- **Docs:** `STUB_CATALOGUE.md:423-424` lists "falling-block rendering (RenderFallingSand + Block.RenderFallingBlock)" as done, and the caveat at `:2023-2024` assumes falling blocks draw.
- **Fix direction (port only):**
  - Add one general rule to `ProxyEntity.ensureFcEntity`. When an FC class has neither a `(World)` nor a no-argument constructor, use its public constructor whose first parameter is `World`. Pass the proxy's position for the doubles and 0/false for the rest, and let the spawn state fill the fields: `writeState` already sends `writeEntityToNBT`, and the frozen `EntityFallingSand` restores `TileID`/`Data` from it.
  - This covers the client spawn, the reload path and the latent lightning case.
  - It is safe on the client: `FCEntityFallingBlock.onUpdate` guards every landing and world write with `!worldObj.isRemote` (`:56-72`), and the dummy client world sets `isRemote = true` (`ProxyEntity.java:128-136`).
  - Cache a construction failure per class, so reflection is not retried every frame.
  - Make `[SELF-AUDIT/renderer]` also check that each FC entity class can be constructed on the client, so this class of bug fails at start-up.
  - Once it draws, two follow-ups:
    - port 1.5.2 `ShouldRenderWhileFalling` into the stand-in (`Modern-Common/.../Block.java:1399-1401` returns true), so the entity does not draw over the placed block in the landing frame;
    - smooth the movement. The proxy is tracked every 3 ticks (`BTWEntityRegistration.java:409-414`), the client writes no position back, and the base `lerpTo` snaps. 1.5.2 and 1.20.1 both simulate the fall on the client.
  - Do not switch to MC's `FallingBlockEntity`: FC's landing rules would be lost.
- **Overlap:** none. I-2 changes landing and placement, not client construction or rendering.
- **Repro (client plan; ClientProbe on a superflat world):**
  ```text
  @world fall_render_client
  gamerule doDaylightCycle false
  time set 6000
  gamerule randomTickSpeed 0
  gamerule doMobSpawning false
  difficulty peaceful
  @waitfor 300 :: execute if entity @a
  @ticks 200
  gamemode creative @a
  tp @a 4 -58 0 facing 0 -50 0
  setblock 0 -40 0 betterthanwolves:block_1011
  setblock 2 -40 0 minecraft:sand
  @ticks 6
  @check server spawned the FC falling entity :: execute if entity @e[type=betterthanwolves:fc_falling_block]
  say PROBE shot falling_blocks
  @ticks 80
  @check loose dirt landed :: execute if block 0 -60 0 betterthanwolves:block_1011
  ```
  - Expected at HEAD: two orange 0.98 wireframes, no texture, and one client WARN `Could not create FC entity net.minecraft.src.btw.entity.FCEntityFallingBlock: ...FCEntityFallingBlock.<init>()`.
  - After the fix: two textured blocks and no WARN.
  - Reload loss: summon a falling block high up, run `save-all`, and restart before it lands. `execute if entity @e[type=betterthanwolves:fc_falling_block]` then stays true for ever, and no block lands.
  - Manual: dig out the block under sand or loose dirt in any client.

### CF2-R3. The crack on BTW's web is smeared (confirmed, low)

- **Scope:**
  - It applies to BTW's own web, `betterthanwolves:block_1037`, the web BTW spiders spit (`Common/.../FCEntitySpiderWeb.java:191`). FC also substitutes that block for the cobweb item (`Client/.../FCBetterThanWolves.java:1168`, `:1912`).
  - Vanilla `minecraft:cobweb` (mineshafts) is not a ProxyBlock. It keeps 1.20.1's cross model and crack, which vanilla itself stretches mildly (by sqrt 2, horizontally). That part is ruled out as a port bug.
- **Mechanism:**
  - 1.20.1 draws the crack by re-tessellating the block model into a `SheetedDecalTextureGenerator` (1.20.1 `LevelRenderer.java:1314-1316`, `BlockRenderDispatcher.java:49-57`).
  - Its `endVertex` (`SheetedDecalTextureGenerator.java:47-58`) picks the axis nearest the vertex normal (`ForgeHooksClient.getNearestStable`) and projects the vertex onto it. For UP, the crack UV becomes u = x, v = z, and the height is dropped.
  - The port gives the web's two vertical diagonal planes the normal UP: `renderCrossedSquares` calls `setNormal(0, 1, 0)` (`Modern-Common/.../RenderBlocks.java:2499-2531`, `:2510`; `javap` of the jar shows the same), and `runFcRender` also defaults to (0, 1, 0) (`FCBakedModel.java:283`).
  - `FCBakedModel.convertCapturedQuad` (`:1085-1124`) turns that normal into `Direction.UP` (`directionFromNormal`, `:1126-1134`) and packs (0, 127, 0) into every vertex (`:1117-1121`). Nothing computes a normal from the geometry.
  - Forge's `putBulkData` takes the quad direction (1.20.1 `VertexConsumer.java:74`), but the baked normal wins (`applyBakedNormals`, `:110`; `IForgeVertexConsumer.java:58-67`).
  - On a vertical diagonal plane, u and v therefore run along one diagonal of the crack texture and stay constant going up the plane, which gives vertical streaks.
- **Same smear elsewhere:** every BTW quad drawn by these renderers:
  - `renderBlockCrops` (`:2534-2545`, UP);
  - `renderBlockStem` (`:1741-1766`, UP);
  - `renderBlockVine` (`:789` on, which inherits the UP default);
  - `drawCrossedSquares` (`:2665-2690`, inherits);
  - `renderBlockLadder` (`:2176-2189`, always +Z, so east- and west-facing ladders smear horizontally).
  
  Players mostly see it on webs (hardness 4) and ladders; crops, saplings, flowers and stems break instantly and show no crack. `FCBlockWeb` draws through `renderCrossedSquares` (`Client/.../FCBlockWeb.java:157-168`).
- **Minor side issue:** there is one `FCBakedModel` per block for all states (`BTWClientEvents.java:257-278`), and its particle sprite is `getIcon(1, 0)` (`FCBakedModel.java:590-598`, `:1325-1327`). 1.5.2 used `getIcon(0, meta)` (`vanilla/client/.../EntityDiggingFX.java:25`), so damaged webs (meta 1-3) shed the particles of an undamaged web.
- **4.B:** the crack is drawn by re-rendering the block with the destroy icon as an override texture: `vanilla/client/.../RenderGlobal.java:1609-1615`, then `RenderBlockWithTexture`, then `FCBlockWeb.RenderBlock`, then `renderCrossedSquares` (`vanilla/client/.../RenderBlocks.java:3511-3550`, `:3712-3745`). Each diagonal plane carries the whole crack texture, unstretched.
- **At HEAD:** yes, in every build. `git blame` puts `RenderBlocks.java:2510` in `183b36c` (2026-03-20), and FCBakedModel's normal packing is unchanged since `48e9825`. `STUB_CATALOGUE.md:2023` notes only that the breaking overlay uses the static quads.
- **Fix direction (port only):**
  - In `FCBakedModel.convertCapturedQuad`, compute each quad's normal from its geometry (the cross product of its diagonals) instead of trusting the tessellator's current normal. Use it for both the `BakedQuad` direction (`Direction.getNearest`) and the packed vertex normal. FC face renderers that set exact axis normals come out unchanged.
  - Bake non-axis-aligned quads with shade=false, as vanilla `cross.json` does, so webs and plants are not darkened by side-face shading. Re-check ambient occlusion.
  - The crack then projects onto the nearest axis, as vanilla's cobweb crack does. For an exact 4.B crack, the alternative is a crumbling pass that captures FC's `RenderBlockWithTexture` with the destroy-stage sprite; that is a larger change.
  - Optionally pick the particle icon per meta with side 0. This needs a per-state particle lookup or a `TerrainParticle` hook.
- **Overlap:** none.
- **Repro:** needs one harness addition, a TickProof verb `btwproof crack <x> <y> <z> <stage>` that calls `ServerLevel.destroyBlockProgress(<fake id>, pos, stage)`. Client plan:
  ```text
  @world web_crack_client
  gamerule doDaylightCycle false
  time set 6000
  gamerule doMobSpawning false
  difficulty peaceful
  @waitfor 300 :: execute if entity @a
  @ticks 200
  gamemode creative @a
  tp @a 0 -60 -3 facing 0 -59.5 0
  setblock -1 -60 0 betterthanwolves:block_1037
  setblock 1 -60 0 minecraft:cobweb
  btwproof crack -1 -60 0 7
  btwproof crack 1 -60 0 7
  @ticks 5
  say PROBE shot web_crack
  ```
  - Expected at HEAD: vertical streaks on the BTW web's crack, and a normal crack on the vanilla cobweb.
  - Manual: in survival, hold left click on a web a spider has spat, and compare it with a mineshaft cobweb. An east-facing BTW ladder shows the horizontal variant.

### CF2-R5b. Loose snow gives off campfire smoke instead of 4.B's white wisps (authentic, plus a confirmed fidelity bug, low)

- **What 4.B does:** loose snow and loose snow slabs spawn one `fcwhitecloud` per display tick, unconditionally (`Client/.../FCBlockSnowLoose.java:271-288`, `FCBlockSnowLooseSlab.java:305-322`).
  - The particle is `FCClientEntityWhiteCloudFX` (`Client/.../FCClientEntityWhiteCloudFX.java:25-50`).
  - It is a near-white puff (brightness 0.7-1.0) using the generic 8-frame smoke sprites, 0.375-0.75 blocks across.
  - It grows in over its first frames, lives 18-66 ticks, and does not drift upward.
  - `RenderGlobal` drops it beyond 16 blocks and at the Minimal particle setting (`vanilla/client/.../RenderGlobal.java:1845-1896`, `:2049-2055`).
  
  So faint white wisps around loose snow are BTW behaviour.
- **What the port does:**
  - `ProxyBlock.animateTick` (`ProxyBlock.java:330-340`) runs FC's `randomDisplayTick` against `createFcWorld` (`:1073`).
  - Its `spawnParticle` goes through `ProxyBlock.mapParticle` (`:1089-1096`, `:369-407`), which maps `fcwhitecloud` to `ParticleTypes.CAMPFIRE_COSY_SMOKE` (`:405`). `fcwhitesmoke` is mapped the same way, twice (`:396`, `:403`). `ProxyBlockEntity.java:218-220` uses the same table.
  - The cosy campfire particle (1.20.1 `CampfireSmokeParticle.java:10-24`, `:26-42`, `:56-61`) is a random `big_smoke` sprite at alpha 0.9, 0.6-1.2 blocks across. It lives 80-129 ticks and wanders slowly upward.
  - Its type has `overrideLimiter = true` (1.20.1 `ParticleTypes.java:82`). `ClientLevel.addParticle` passes that as `force` (`ClientLevel.java:588-593`), so `LevelRenderer.addParticleInternal` skips both the 32-block cull and the Minimal check (`LevelRenderer.java:2500-2530`).
  - Spawn rates are comparable: about 0.18 display ticks per game tick in 1.20.1 against 0.24 in 1.5.2. The visible difference is the particle itself, which lives 2-5 times longer, is about 1.6 times wider and is grey.
- **Inconsistency in the port:** two other paths already map `fcwhitecloud` and `fcwhitesmoke` to `CLOUD`: `ClientFcWorld.java:316` and `WorldBridge.java:2344`. The latter also carries the server-side `fcwhitesmoke` of `Common/.../FCTileEntityUnfiredBrick.java:76`.
- **At HEAD:** yes, in every public build. The mapping dates from `5257461` (2026-03-30) and is at `ProxyBlock.java:292` in `48e9825`.
- **Fix direction:**
  - Map `fcwhitecloud` and `fcwhitesmoke` to `ParticleTypes.CLOUD` in `ProxyBlock.mapParticle`, as `ClientFcWorld` and `WorldBridge` already do, and delete the duplicate line.
  - 1.20.1's `PlayerCloudParticle` (`:14-58`) is the 1.5.2 `EntityCloudFX` that FC's white smoke copies: the same colour, size, lifetime formula and grow-in, and it respects the limiter.
  - Remaining gaps: `fcwhitesmoke` drifts up 0.004 per tick and CLOUD does not; CLOUD has no physics; CLOUD is pulled toward a player within 2 blocks.
  - Better: one shared FC-particle table used by `ProxyBlock`, `ProxyBlockEntity`, `ClientFcWorld` and `WorldBridge`. Optionally register two BTW particle types that reproduce `FCClientEntityWhiteSmokeFX` and `FCClientEntityWhiteCloudFX` exactly.
  - The same change corrects the brick oven, campfire, kiln, smouldering log, millstone and vessel particles (`FCBlockFurnaceBrick.java:531`, `FCBlockCampfire.java:1076`, `FCBlockKiln.java:316` and `:351`, `FCBlockLogSmouldering.java:367`, `FCBlockMillStone.java:490`, `FCBlockVessel.java:373`).
- **Overlap:** none.
- **Repro (client plan):**
  ```text
  @world snow_particles_client
  gamerule doDaylightCycle false
  time set 6000
  gamerule randomTickSpeed 0
  gamerule doMobSpawning false
  difficulty peaceful
  @waitfor 300 :: execute if entity @a
  @ticks 200
  gamemode creative @a
  tp @a 0 -60 -3 facing 0 -60 0
  setblock 0 -60 0 betterthanwolves:block_1048
  setblock 2 -60 0 betterthanwolves:block_1047
  @ticks 100
  say PROBE shot loose_snow_particles
  ```
  - `randomTickSpeed 0` keeps FC from melting the snow outside cold biomes.
  - Expected at HEAD: large grey puffs lingering around both blocks.
  - Manual discriminator: set Particles to Minimal, or stand more than 32 blocks away. The puffs stay at HEAD. In 4.B, or with CLOUD, they disappear.

---

## 2. Drops: beds, doors, scattering items, flowers, creepers

> "Breaking a village bed that was yellow caused a red bed to drop, alongside the yellow one, giving me 2 beds." · "Items tend to shoot out in any direction after breaking a block, not always though." · "Breaking an oxide daisy dropped a poppy instead" · "Creepers drop full blocks when blowing up (I thought maybe they would drop the block's dust forms like pile of dirt/gravel, but I am not sure)"

All four reported root causes hold at HEAD `0292e38`, in 1.3.0-alpha.3 (`48e9825`) and in 1.4.0-alpha.5 (`d55db5d`); each file was checked in each build. The working tree's `BoneMealPolicy` and `FCImmatureCropLootModifier` diffs change only comments.

### CF2-DR1. Beds duplicate: the FC harvest and 1.20.1's shape update both drop one (confirmed, high)

- **Effect:**
  - Any bed broken at its foot gives 2 beds: one red, plus one of the bed's own colour. A red bed gives 2 red beds, so beds can be duplicated without limit.
  - A head break gives 1 red bed, which loses the colour.
  - A creative foot break drops a bed.
- **Mechanism:**
  - **(a) FC harvests vanilla beds.**
    - All 16 beds map to legacy 26 (`FCBlockBed`), red first (`ProxyRegistry.java:462-478`). So `ServerPlayerGameModeMixin` cancels `destroyBlock` (`:89-92`) and runs FC `tryHarvestBlock` (`:136`).
    - Cloth needs no tool (`InventoryBridge.java:312-319`), so `harvestBlock` runs.
    - Modern-Common `BlockBed` (`BlockBed.java:1-14`) has none of 1.5.2 `BlockBed`'s drop rules: no `idDropped`, `dropBlockAsItemWithChance`, `onNeighborBlockChange` or `onBlockHarvested`. So `Block.idDropped` returns 26 from either half (`Modern-Common/.../Block.java:423-425`, `:630-675`).
    - `toMcStack(26)` has no variant entry and resolves to the canonical `RED_BED` (`ProxyRegistry.java:825-829`, `:79-87`; `ItemStackHelper.java:46-57`).
  - **(b) 1.20.1 pops the partner with its own loot.**
    - FC removes the broken half with `setBlockToAir`, flags 3 (`WorldBridge.java:1117-1121`).
    - 1.20.1 `Level.markAndNotifyBlock` first runs the neighbour pass (`Level.java:251`). `BlockBehaviorMixin.neighborChanged` reaches FC for beds (`:156-170`), but Modern-Common `BlockBed` has no `onNeighborBlockChange`.
    - The shape pass (`:257-261`) then runs `BedBlock.updateShape` on the partner (`BedBlock.java:148-154`), which returns AIR. `Block.updateOrDestroy` calls `destroyBlock(pos, true)` (`Block.java:155-166`, `Level.java:279-299`).
    - The bed's loot table drops only from the head (`loot_tables/blocks/yellow_bed.json`).
  - **Creative:** FC's creative branch only calls `removeBlock` (`Modern-Common/.../ItemInWorldManager.java:40-52`). 1.20.1's creative partner removal (`BedBlock.playerWillDestroy`, `:160-174`) never runs because `destroyBlock` was cancelled. So the head's loot drops.
- **Missing prerequisite for any fix: beds have no metadata codec.** Beds fall into the generic `HORIZONTAL_FACING` branch of `WorldBridge.deriveVanillaMetadataUnchecked` (`:574-586`), which returns 2-5 with no head bit (8). `meta & 3` matches the 1.5.2 bed direction only for north-facing beds. A straight 1.5.2 port without a codec is worse than HEAD:
  - both halves would drop a bed;
  - `onNeighborBlockChange` would look for the partner in the wrong cell for 3 of the 4 facings, so any neighbour update would pop south-, west- and east-facing beds.
- **4.B:** breaking either half gives exactly one bed (`vanilla/server/.../BlockBed.java:170-191`, `:196-199`; `Server/.../FCBlockBed.java:21-39`):
  - the foot drops `Item.bed`;
  - the head drops nothing;
  - an orphaned head is removed without a drop;
  - creative breaks drop nothing (`:302-314`).
  
  4.B has one bed colour, and its beds can't be slept in.
- **At HEAD:** yes.
  - In `48e9825` the table was an `IdentityHashMap` (`:35`) with `WHITE_BED` listed first, so the extra bed was a random colour per launch. Since `f6ec9be` it is always red.
  - Not recorded anywhere. `CHANGELOG.md:868-876`, `STUB_CATALOGUE.md:2419-2421` and `BRIDGE_SPEC.md:944`, `:1007` cover only the collapse of items at FC grids and workbenches. The follow-ups R11 (HookPolicy bed row) and R12 (`BedSpawnPolicy`) cover USE only.
  - The read-only audit `leaves_drop_audit2.py` reports `FCBlockBed idDropped: 1.5.2=BlockBed port=Block`.
- **Fix direction:** FC code untouched.
  - **(0) Prerequisite:** a bed codec in `WorldBridge.deriveVanillaMetadataUnchecked`:
    - direction from `FACING` (0 = S, 1 = W, 2 = N, 3 = E, as in `BlockBed.footBlockToHeadBlockMap`, `BlockBed.java:22`);
    - bit 8 from `PART=HEAD`;
    - bit 4 from `OCCUPIED`.
  - **(1)** Port 1.5.2 `BlockBed`'s `idDropped`, `dropBlockAsItemWithChance`, `onNeighborBlockChange` and `onBlockHarvested` into Modern-Common `BlockBed`. With (0) in place, FC's `onNeighborBlockChange` runs in the neighbour pass (`Level.java:251`), before the shape pass, and clears the partner without loot.
  
    D7 (`idDropped` parity) alone is not enough: a foot break still gives 2 beds, and a head break would give 0.
  - **(2)** Keep the colour with the *collapsed-identity rule* shared with CF2-DR3. The FC drop (`Item.bed` 355, which resolves to `RED_BED`) becomes the broken bed's own colour, read from a harvest context that holds the original `BlockState`. For a head break, 1.5.2 drops from the orphaned foot's `onNeighborBlockChange` after the head is gone. So the state has to be captured in `BlockBehaviorMixin.neighborChanged` before FC runs.
  - **Alternative (needs a maintainer ruling):** beds are already RULED VANILLA for USE. Ruling bed drops VANILLA too means skipping the FC harvest for the 16 beds and letting 1.20.1 `destroyBlock` run. That gives the right count and colour with no codec, and nothing in creative. FC's harvest exhaustion and the panic of nearby FC animals would then need separate calls.
- **Overlap:** partly D7, "Modern-Common drop parity (66 classes)" (`followups_recon.md` section 9), which lists `FCBlockBed idDropped`. D7 needs the codec in (0). Not recorded anywhere: the double drop, the creative drop and the missing bed codec.
- **Repro (`proof_run`, survival proof player, empty hand):**
  ```text
  fill -2 -62 4 6 -62 10 minecraft:stone
  fill -2 -61 4 6 -58 10 minecraft:air
  setblock 0 -61 8 minecraft:yellow_bed[facing=north,part=foot]
  setblock 0 -61 7 minecraft:yellow_bed[facing=north,part=head]
  btwproof hand main none
  btwproof hit 0 -61 8 north survival
  @check BED-F1 GAP foot break drops no red bed (HEAD FAIL: red_bed x1 + yellow_bed x1) :: btwproof items 0 -61 7 3 expect minecraft:red_bed == 0
  @check BED-F2 CONTROL foot break drops the yellow bed :: btwproof items 0 -61 7 3 expect minecraft:yellow_bed == 1
  btwproof items 0 -61 7 3 clear
  setblock 0 -61 8 minecraft:yellow_bed[facing=north,part=foot]
  setblock 0 -61 7 minecraft:yellow_bed[facing=north,part=head]
  btwproof hit 0 -61 7 north survival
  @check BED-H1 GAP head break drops the yellow bed (HEAD FAIL: red_bed x1 only) :: btwproof items 0 -61 7 3 expect minecraft:yellow_bed == 1
  btwproof items 0 -61 7 3 clear
  setblock 0 -61 8 minecraft:red_bed[facing=north,part=foot]
  setblock 0 -61 7 minecraft:red_bed[facing=north,part=head]
  btwproof hit 0 -61 8 north survival
  @check BED-DUP GAP one red bed from one red bed (HEAD FAIL: 2) :: btwproof items 0 -61 7 3 expect minecraft:red_bed == 1
  btwproof items 0 -61 7 3 clear
  setblock 0 -61 8 minecraft:yellow_bed[facing=north,part=foot]
  setblock 0 -61 7 minecraft:yellow_bed[facing=north,part=head]
  btwproof hit 0 -61 8 north creative
  @check BED-C1 GAP creative foot break drops nothing (HEAD FAIL: yellow_bed x1) :: btwproof items 0 -61 7 3 expect minecraft:yellow_bed == 0
  setblock 4 -61 8 minecraft:yellow_bed[facing=south,part=foot]
  setblock 4 -61 9 minecraft:yellow_bed[facing=south,part=head]
  setblock 5 -61 8 minecraft:stone
  setblock 5 -61 8 minecraft:air
  @check BED-R1 REGRESSION a south-facing bed survives a neighbour update (HEAD PASS; FAILS after a 1.5.2 port without the codec) :: execute if block 4 -61 8 minecraft:yellow_bed
  ```
  Manual: in survival, punch out the foot of any non-red bed. HEAD drops a red bed and one of the bed's colour.

### CF2-DR6. Doors duplicate the same way (confirmed, high; not in the report)

- **Effect:**
  - Breaking the upper half of an oak door, or of an iron door with a pickaxe, drops 2 doors. A lower break drops 1.
  - A creative upper break drops a door.
  - An iron door melts in a stoked crucible into 4 iron ingots and 4 gold nuggets (`Server/.../FCRecipes.java:4314-4323`), so this is an iron and gold generator.
- **Mechanism:** the same as CF2-DR1.
  - `OAK_DOOR` maps to legacy 64 (`FCBlockDoorWood`) and `IRON_DOOR` to 71 (`FCBlockDoor`) (`ProxyRegistry.java:587`, `:601`; `BTWLifecycle.java:425`, `:965-975`).
  - Modern-Common `BlockDoor` (`:1-14`) lacks 1.5.2's "upper half drops 0" and its creative `onBlockHarvested`. So FC drops a door from either half (audit: `FCBlockDoor`/`FCBlockDoorWood idDropped: 1.5.2=BlockDoor port=Block`).
  - 1.20.1 `DoorBlock.neighborChanged` never calls super (`DoorBlock.java:180-191`), so FC's `onNeighborBlockChange` never runs for doors.
  - `DoorBlock.updateShape` returns AIR for the orphaned half (`:76-83`), whose loot drops only from `half=lower` (`oak_door.json`, `iron_door.json`).
  - In creative, `preventCreativeDropFromBottomPart` (`:85-88`) is skipped along with the cancelled `destroyBlock`.
- **No codec:** doors also fall into the generic facing branch (`WorldBridge.java:574-586`) and never set the upper bit 8. A 1.5.2 `idDropped` port on its own would still drop a door from the upper half.
- **4.B:** either half gives exactly one door. The upper half drops nothing, and an orphaned lower half drops the door (`vanilla/server/.../BlockDoor.java:314-317`, `:374-380`; `Server/.../FCBlockDoor.java:68-125`). Creative drops nothing.
- **At HEAD:** yes, in every build (`48e9825` `ProxyRegistry.java:370`, `:384`). Not recorded anywhere. The R11 door rows (NEIGHBOR RULED VANILLA, USE PENDING the P4 door codec) do not touch the harvest.
- **Fix direction:**
  - (0) the queued P4 door codec, with `DoorBlock` joining `isMetaFullyDerived`;
  - (1) port 1.5.2 `BlockDoor.idDropped` and `onBlockHarvested` into Modern-Common `BlockDoor` (part of D7).
  
  Alternative, consistent with doors' NEIGHBOR VANILLA ruling: skip the FC harvest for doors and let 1.20.1 `destroyBlock` run.
- **Overlap:** D7 lists `FCBlockDoor` and `FCBlockDoorWood` `idDropped`, and P4 queues the door codec. Neither is framed as a duplication fix, and D7 does nothing without the codec.
- **Repro (`proof_run`):**
  ```text
  fill 0 -62 18 2 -62 22 minecraft:stone
  fill 0 -61 18 2 -58 22 minecraft:air
  setblock 1 -61 20 minecraft:oak_door[facing=north,half=lower]
  setblock 1 -60 20 minecraft:oak_door[facing=north,half=upper]
  btwproof hand main none
  btwproof hit 1 -60 20 north survival
  @check DOOR-1 GAP upper break gives one door (HEAD FAIL: oak_door x2) :: btwproof items 1 -61 20 3 expect minecraft:oak_door == 1
  btwproof items 1 -61 20 3 clear
  setblock 1 -61 20 minecraft:oak_door[facing=north,half=lower]
  setblock 1 -60 20 minecraft:oak_door[facing=north,half=upper]
  btwproof hit 1 -61 20 north survival
  @check DOOR-2 CONTROL lower break gives one door :: btwproof items 1 -61 20 3 expect minecraft:oak_door == 1
  btwproof items 1 -61 20 3 clear
  setblock 1 -61 20 minecraft:iron_door[facing=north,half=lower]
  setblock 1 -60 20 minecraft:iron_door[facing=north,half=upper]
  btwproof hand main minecraft:iron_pickaxe
  btwproof hit 1 -60 20 north survival
  @check DOOR-3 GAP iron door upper break with a pickaxe gives one door (HEAD FAIL: 2) :: btwproof items 1 -61 20 3 expect minecraft:iron_door == 1
  ```
  Manual: place an oak door in survival and punch its top half. HEAD drops 2 oak doors.

### CF2-DR2. Drops eject out of the block above (confirmed; the link to the symptom is likely; medium)

- **Effect:** in early-game mining, stone and rough stone with a stone pickaxe, staged ores and bare-hand log chewing eject their drops into the block above. In tunnels those drops are pushed sideways through the rock toward a random open side. A drop from a ceiling-height block can rise through rock and be lost. With air above the block, the drop just pops on top, which explains "not always".
- **Mechanism:**
  - **(A) The main cause: the FC harvest always runs with face 1 (UP).**
    - The mixin calls `tryHarvestBlock(x, y, z, 1)` (`ServerPlayerGameModeMixin.java:136`). 1.5.2 passed the clicked face from the dig packet and kept it for the delayed break (`m_iHarvestingFromFacing`; `vanilla/server/.../NetServerHandler.java:449`, `:460`; `ItemInWorldManager.java:44`, `:120`, `:155-165`, `:188`, `:208-239`).
    - Every FC conversion ejects through `FCUtilsItem.EjectStackFromBlockTowardsFacing(..., iFromSide)` (`Common/.../FCUtilsItem.java:118-158`). That covers:
      - stone with a stone pickaxe, giving 3 stones and a gravel pile (`Server/.../FCBlockStone.java:128-197`, `:204-215`);
      - rough stone (`FCBlockStoneRough.java:99`, `:111`, `:119`);
      - staged ores (`FCBlockOreStaged.java:134-150`);
      - grass with a hoe (hemp seeds 1 in 25, `FCBlockGrass.java:172-195`);
      - bare-hand log chewing and the work stump (bark; `FCBlockLog.java:74-111`, `FCBlockWorkStump.java:92`);
      - damaged logs (`FCBlockLogDamaged.java:179-190`).
    - With face 1 there is no tilt, so the item's centre spawns at y+1.2 to y+1.3, inside the block above. In a tunnel, a trunk or a wall, that block is solid.
    - 1.20.1 `ItemEntity.tick` sets `noPhysics` on any overlap. `moveTowardsClosestSpace` (`Entity.java:2343-2373`) then gives it 0.1-0.3 velocity toward the nearest non-full neighbour (N, S, W, E or UP), chosen by its position in the cell, and moves it without collision.
      - A 1x1 trunk: the item leaves a random side of the log above.
      - The lower block of a 1x2 tunnel face: toward the tunnel.
      - The upper block (all neighbours full): up through the rock until it finds air.
  - **(B) Every plain FC dropped item spawns 0.125 too high.**
    - `EntityProxyFactory.createItemEntity` (`:116-122`, `:219-226`) uses FC `posY`, which is 1.5.2's box centre (`minY + yOffset`, `yOffset = 0.125`; `Modern-Common/.../EntityItem.java:11-26`, `Entity.java:143-152`), as the 1.20.1 feet position. `ItemEntityBridge` already converts correctly (`:153-156`, `:249-250`).
    - `dropBlockAsItem_do` drops (`Modern-Common/.../Block.java:677-694`) then reach up to y+1.1, so about 14% of them overlap a solid block above and get pushed. In 1.5.2 their top is at most y+0.975.
  - **(C)** 1.20.1's push rule applies (`ItemEntity.java:128-134`). In 1.5.2, FC's FCMOD reverted Mojang's fix, and an item was pushed only when its centre cell was a normal cube (`vanilla/server/.../EntityItem.java:98-106`, `:624-640`; `Entity.java:358-366`).
- **Partly authentic:** several separate piles scattering with random velocity from an improper-tool break is 4.B (for example, 6 dirt piles from a bare-hand dirt break).
- **4.B:** conversion drops eject out of the face that was hit, toward the player: 0.2-0.3 outside that face, with velocity along it. Ordinary drops spawn inside the broken cell.
- **At HEAD:** yes. The face was 1 in `48e9825` (`:65`) and `d55db5d` (`:64`). `createItemEntity` is unchanged since `64016ac`. Not in CHANGELOG, STUB_CATALOGUE or INTEGRATIONS.
- **Fix direction (port only):**
  1. Capture the dig face. Inject at HEAD of `ServerPlayerGameMode.handleBlockBreakAction` (1.20.1 `ServerPlayerGameMode.java:124-126`, where the face is known) and store `Direction.get3DDataValue()` per position for `START_DESTROY_BLOCK` and `STOP_DESTROY_BLOCK`. Keep it for the delayed destroy in `tick()` (`:87-97`), as `m_iHarvestingFromFacing` did, and pass it to `tryHarvestBlock`. The face ids match 1.5.2. Fall back to 1 only for direct `destroyBlock` callers.
  2. In `createItemEntity`, spawn at `feet = posY - yOffset`, the convention `ItemEntityBridge` already uses. Audit `applyPositionFromFc` for other FC entities whose `yOffset` is not 0.
  3. Leave 1.20.1 item physics alone (a maintainer decision, as with the Hardcore Buoy known issue).
- **Overlap:** (1) is queued as "the upward chew-eject (`ServerPlayerGameModeMixin.java:136`)" (`followups_recon.md` section 9, Woods and logs; #034 pre-existing gaps). That item names only chewing, but the same line governs every FC conversion. (2) is not recorded anywhere.
- **Repro (`proof_run`; deterministic case, a bare-hand chew of a 1x1 trunk):**
  ```text
  fill 20 -62 0 24 -62 4 minecraft:stone
  fill 20 -61 0 24 -57 4 minecraft:air
  setblock 22 -61 2 minecraft:oak_log
  setblock 22 -60 2 minecraft:oak_log
  setblock 22 -59 2 minecraft:oak_log
  btwproof hand main none
  btwproof hit 22 -61 2 north survival
  @check EJ-1 GAP bark not spawned inside the log above (HEAD FAIL) :: execute positioned 22 -60 2 unless entity @e[type=minecraft:item,dx=0,dy=0,dz=0]
  @ticks 40
  @check EJ-2 GAP bark lands on the clicked (north) side (HEAD FAILS about 3 runs in 4) :: btwproof items 22 -61 1 0.5 expect fc:fcItemBark >= 1
  ```
  - Run EJ-1 in the same tick as the hit.
  - `TickProof.hit` calls `destroyBlock` directly (`TickProof.java:2031-2060`) and carries no face. The after-fix run needs `hit` to call `handleBlockBreakAction(pos, START_DESTROY_BLOCK, face, ...)` first.
  - Offset (B): harvest FC dirt with a shovel under a stone ceiling 1000 times and log each item's box `maxY - y`. HEAD: a maximum near 1.10, with about 14% above 1.0. After the fix: at most 0.975.
  - Manual: in a 1x2 tunnel, mine into stone with a stone pickaxe. Drops from the lower block squirt toward you out of the block above; drops from the upper block rise into the rock. Punch the base of a 1x1 oak trunk: the bark leaves a random side of the log above.

### CF2-DR3. Eight 1.20.1 flowers drop a poppy (confirmed, low)

- **Effect:** blue orchid, allium, azure bluet, the four tulips and the oxeye daisy drop a poppy when broken. They also drop a poppy when FC pops them, after their soil is dug out or when they fail FC's light rule on a random tick. Crafting then gives red dye.
- **Mechanism:**
  - All nine flowers map to legacy 38 (`FCBlockFlowerBlossom`), POPPY first (`ProxyRegistry.java:516-526`; `BTWLifecycle.java:531-532`).
  - 38 has no `VARIANT_BLOCKS` or `LegacyVariants` entry (`ProxyRegistry.java:162-186`), and no ProxyItem is registered there (ids start at 175; `BTWRegistration.java:84-85`, `:115-116`).
  - A player break goes through the FC harvest (`ServerPlayerGameModeMixin.java:89-92`, `:136`). Neither Modern-Common `BlockFlower` nor 1.5.2 `BlockFlower`/`FCBlockPlants` overrides `idDropped`, so FC drops `ItemStack(38, 1, 0)` (`vanilla/server/.../Block.java:756-759`).
  - `toMcStack` falls back to `getModernBlock(38)`, which is POPPY (`ItemStackHelper.java:46-57`, `:113-128`; `ProxyRegistry.java:825-829`).
  - FC pops go the same way:
    - The FC neighbour pass runs before 1.20.1's shape pass (`Level.java:251` against `:257-261`). When the soil is removed, `BlockBehaviorMixin.neighborChanged` runs FC `checkFlowerChange`, which drops a poppy and removes the flower (`Modern-Common/.../BlockFlower.java:195-216`). That happens before `BushBlock.updateShape` could drop the flower's own loot.
    - `TickPolicy` routes FC random ticks to FC-mapped flowers (`:34-58`), so the light rule can pop a flower as a poppy too.
  - Cornflower, lily of the valley and wither rose are not mapped, so they keep the vanilla path and their identity.
- **4.B:** 4.B has only the rose (38:0) and the dandelion, and a broken rose drops a rose (`Common/.../FCBlockFlowerBlossom.java:21`; `vanilla/server/.../BlockFlower.java:20`, `:87-94`). The port should keep 1.20.1's identity: a broken daisy drops a daisy (`loot_tables/blocks/oxeye_daisy.json`).
- **At HEAD:** yes. The drop is always a poppy since `f6ec9be`; in `48e9825` it was one of the nine at random per launch. The docs explain the collapse for items only (`CHANGELOG.md:868-876`, `STUB_CATALOGUE.md:2419-2421`, `BRIDGE_SPEC.md:944`, `:1007`), not for block drops.
- **Fix direction:** a general *collapsed-identity rule*, shared with CF2-DR1. When an FC self-drop (`idDropped` equals the broken block's own legacy id) comes from a cell whose 1.20.1 block is a non-canonical member of a collapsed id, it becomes that block's own item.
  - (a) Harvest: `ItemInWorldManager` removes the block before `harvestBlock`, so take the original `BlockState` from a context set in `ServerPlayerGameModeMixin` around `tryHarvestBlock`.
  - (b) Pops: the live block is still there, so reuse the existing `DropModernBlockLoot` mechanism (`WorldBridge.java:3025-3038`; precedent `ModernTwins.java:3-15`).
  
  A variant table for 38 alone does not help, because `damageDropped` returns 0. The same rule covers wet sponge (19) and carved pumpkin (86) from `STUB_CATALOGUE.md:2419-2421`. Narrower option: skip the FC harvest only for non-canonical members of a collapsed id, as twins already do. See also CF2-P8c, the placement side of the same collapse.
- **Overlap:** nothing planned. White dye Q1 and the flower-dye notes (`INTEGRATIONS.md:564`, `BRIDGE_SPEC.md:1007`) cover crafting only.
- **Repro (`proof_run`, `randomTickSpeed 0`):**
  ```text
  setblock 0 -62 12 minecraft:grass_block
  setblock 0 -61 12 minecraft:oxeye_daisy
  @check FL-1 GAP oxeye daisy drops itself (HEAD FAIL: poppy x1) :: btwproof break 0 -61 12 none 1 survival minecraft:oxeye_daisy
  setblock 0 -61 12 minecraft:blue_orchid
  @check FL-2 GAP blue orchid drops itself (HEAD FAIL: poppy) :: btwproof break 0 -61 12 none 1 survival minecraft:blue_orchid
  setblock 0 -61 12 minecraft:poppy
  @check FL-3 CONTROL poppy :: btwproof break 0 -61 12 none 1 survival minecraft:poppy
  setblock 0 -61 12 minecraft:oxeye_daisy
  @check FL-4 GAP digging the soil pops the daisy as itself (HEAD FAIL: poppy) :: btwproof break 0 -62 12 minecraft:iron_shovel 1 survival minecraft:oxeye_daisy
  ```
  Manual: punch any tulip, allium, bluet, orchid or daisy in survival, or dig out the grass under one. It drops a poppy.

### CF2-DR4. Creeper drops: full blocks are 4.B, but the identities and FC's blast rules are not (authentic in substance; divergences confirmed; low)

- **Answer to the commenter:** a 4.B creeper drops full blocks, not piles. In 4.B, piles come from mining charges and improper-tool breaks (CF2-DR5).
- **Mechanism:**
  - `WorldBridge.newExplosion` runs 1.20.1 `level.explode` (`WorldBridge.java:2851-2863`, `:2873-2879`). For a creeper that is MOB interaction, so `mobExplosionDropDecay` gives a 1/radius chance. The FC `Explosion` object it builds is never run.
  - So every blasted block drops through 1.20.1 `BlockState.getDrops` (1.20.1 `Explosion.java:240-275`, `:292-308`; `Level.java:525-540`). 1.5.2 `doExplosionB` never runs: `canDropFromExplosion`, then `dropBlockAsItemWithChance(1/size)`, then `setBlock`, then `onBlockDestroyedByExplosion` (`vanilla/server/.../Explosion.java:237-247`).
  - Vanilla-identity blocks use vanilla loot:
    - grass and dirt drop `minecraft:dirt` (4.B: loose dirt; `FCBlockDirt.java:41-44`, `FCBlockGrass.java:68-95`);
    - stone drops cobblestone (4.B: loose cobblestone, plus a stone item from every blasted stone block, plus a gravel pile when uncracked; `FCBlockStone.java:88-107`);
    - cobblestone drops cobblestone (4.B: loose cobblestone; `FCBlockCobblestone.java:33`);
    - gravel and sand match 4.B (gravel gives flint 1 in 10, `FCBlockGravel.java:40-49`).
  - The port does not hook Forge `onBlockExploded` or `wasExploded`, so FC's `onBlockDestroyedByExplosion` overrides never run on 1.20.1 blasts. There are 15 such overrides, among them:
    - dirt, grass, mycelium and their slabs (dirt loosening, `FCBlockDirt.java:63-75`);
    - rough stone;
    - damaged, smouldering and spike logs;
    - the mining charge (chain priming, `FCBlockMiningCharge.java:170-182`).
  - FC-only ProxyBlocks use `ProxyBlock.getDrops` (`:833-854`). It skips the `survives_explosion` roll and FC's `dropBlockAsItemWithChance` overrides, so they always drop (4.B: 1/size). It also computes `damageDropped` without using it (`:839`), so blasted FC blocks lose their subtype.
  - TNT drop decay is off by default (1.20.1 `GameRules.java:82-84`).
- **At HEAD:** yes. CHANGELOG HAI-2 and HAI-7 only made creepers explode and credited the kill. `STUB_CATALOGUE.md:3579-3581` lists the open "explosions" hook family (decision D6).
- **Fix direction (port only, integrated rather than disabled):**
  - Bridge 1.20.1 blasts to FC's per-block rules. This is the queued "P5 explosions" and "D1(c), the general getDrops bridge on blasts".
  - Hook the explosion itself, not a loot modifier. A modifier keyed on `EXPLOSION_RADIUS` misses blasts without decay: TNT by default, and every blast when the decay gamerules are off.
  - Hook per block through Forge `ExplosionEvent.Detonate`'s affected list, or inject in `Explosion.finalizeExplosion` around `getDrops` and `onBlockExploded` (`Explosion.java:244-268`).
  - For an FC-identity block, run `doExplosionB`'s body: `canDropFromExplosion`, then `dropBlockAsItemWithChance(chance)`, then remove the block, then `onBlockDestroyedByExplosion`. Use chance = 1/radius when that interaction's decay rule is on, and 1 otherwise.
  - Also:
    - give `ProxyBlock.getDrops` the explosion chance (queued) and apply `damageDropped`;
    - port the 1.5.2 base logic of `OnDirtDugWithImproperTool`, `OnDirtSlabDugWithImproperTool` and `NotifyNeighborDirtDugWithImproperTool`. They are empty stubs in Modern-Common (`Block.java:1536-1539`), so dirt loosening is dead for bare-hand digging too (`vanilla/server/.../Block.java:1753-1789`).
- **Overlap:** queued in `followups_recon.md` section 9:
  - "P5: pistons, explosions, RESIST";
  - "D1(c)";
  - "the ProxyBlock.getDrops explosion chance";
  - "explosion drops for stand-in logs" (#034, "explosions give whole logs").
  
  Not recorded: the dead dirt-loosening stubs, the unused `damageDropped`, and the missing FC blast reactions.
- **Repro (`proof_run`; vanilla TNT, because a summoned creeper may be replaced; the drop identity is the same for every blast):**
  ```text
  fill 30 -62 0 40 -61 10 minecraft:stone
  fill 30 -60 0 40 -60 10 minecraft:dirt
  fill 30 -59 0 40 -59 10 minecraft:grass_block
  summon minecraft:tnt 35 -60 5 {fuse:0s}
  @ticks 10
  @check U2-1 GAP no vanilla dirt from blasted dirt or grass (HEAD FAIL) :: btwproof items 35 -60 5 7 expect minecraft:dirt == 0
  @check U2-2 GAP blasted dirt gives loose dirt (HEAD FAIL: 0) :: btwproof items 35 -60 5 7 expect fc:fcBlockDirtLoose >= 1
  @check U2-3 GAP blasted stone gives stone items (HEAD FAIL: 0) :: btwproof items 35 -60 5 7 expect fc:fcItemStone >= 1
  @check U2-4 GAP no vanilla cobblestone (HEAD FAIL) :: btwproof items 35 -60 5 7 expect minecraft:cobblestone == 0
  ```
  Manual: let a creeper explode on grass, dirt and stone. HEAD gives vanilla dirt and cobblestone, no stone items or gravel piles, and no loosened dirt around the crater.

### CF2-DR5. Mining charges drop nothing from stone, dirt, gravel or sand (confirmed, medium; not in the report)

- **Mechanism:**
  - `FCExplosionMining.DestroyBlock` calls `DropItemsOnDestroyedByExplosion(world, i, j, k, null)` for every block except cobblestone, which drops gravel through `dropBlockAsItem_do` (`Common/.../FCExplosionMining.java:216-237`; `FCEntityMiningCharge.java:261-265`). It then calls `onBlockDestroyedByExplosion` and clears the block, which drops nothing.
  - In 1.5.2 the FC-patched base `Block.DropItemsOnDestroyedByExplosion` has a body (`vanilla/server/.../Block.java:1722-1748`): chance 1 (or 1/size with an explosion), then `DropComponentItemsOnBadBreak`, else `dropBlockAsItemWithChance`. No FC block overrides it.
  - Modern-Common's base is an empty stub (`Block.java:1531-1532`), so stone, dirt, gravel and sand drop nothing. Only `ModernBlockStandIn` overrides it (`ModernBlockStandIn.java:545-549`), so a charge drops loot from deepslate but nothing from stone.
  - The empty base `onBlockDestroyedByExplosion` is correct; 1.5.2's is empty too (`Block.java:1002`). FC's overrides of it do run on this path, so chain priming works. Only their dirt loosening is dead (the stubs of CF2-DR4).
- **4.B:** a mining charge breaks each block into its component piles at 100%:
  - stone: 5 stones and 3 gravel piles, 2 if cracked (`FCBlockStone.java:110-121`);
  - dirt: 6 dirt piles (`FCBlockDirt.java:47-52`);
  - gravel: 6 gravel piles (`FCBlockGravel.java:66-71`);
  - sand: 6 sand piles (`FCBlockSand.java:47-52`);
  - cobblestone: a gravel block.
  
  The dirt around the blast is loosened.
- **At HEAD:** yes; the stub was the same in `48e9825` (`Block.java:1226`). Not recorded. The STUB_CATALOGUE P0 "override point" table (`:845-863`) does not list this method, although 1.5.2 had base logic there.
- **Fix direction:** port the 1.5.2 base bodies of `DropItemsOnDestroyedByExplosion`, `OnDirtDugWithImproperTool`, `OnDirtSlabDugWithImproperTool` and `NotifyNeighborDirtDugWithImproperTool` into Modern-Common, and update the STUB_CATALOGUE P0 table. These methods carry base logic; they are not override points.
- **Overlap:** next to D7, but that audit checks only `idDropped`, `damageDropped`, `quantityDropped`, `quantityDroppedWithBonus`, `harvestBlock`, `createStackedBlock` and `canSilkHarvest`. Also next to the queued P5 explosions. Not planned as such.
- **Repro:**
  - Manual: place a BTW mining charge against stone, dirt or gravel and light it. At HEAD the cleared blocks leave no items (cobblestone still leaves gravel).
  - `proof_run` idea: a helper verb that runs `new FCExplosionMining(world, x, y, z, facing).doExplosion()`. Fill a 3x3x3 of stone, detonate facing north, then check `@check MC-1 GAP (HEAD FAIL: 0) :: btwproof items <centre> 3 expect fc:fcItemStone >= 5`.

---

## 3. Inventory: blocks that never run out, phantom items, items back after death

> "I somehow got a wooden pressure plate when in a village, then when shift clicking bark into my hotbar I got another? ... Later on I crafted a sharp stone and when shift clicking it into my hotbar it gave me a loose snow slab I shouldn't have." · "After dying blocks I got from a creeper explosion were put back in my inventory despite placing them" · "Gravel slabs don't run out and can continue to be placed?"

One root cause explains all three items: CF2-I1, with its consequences CF2-I2 and CF2-I3, and a second trigger, CF2-I5. CF2-P10 is a different, older trigger of the same symptom; it existed only in 1.3.0-alpha.3, which the commenter did not run (see [Which build](#which-build-the-commenter-ran)).

### CF2-I1. A nested PlayerBridge re-sync loses the placement decrement (confirmed, high)

- **Effect:** survival block duplication on most placements.
  - A block placed through BTW's Stage-2 placement code (not sneaking, main hand, against an FC-handled block) is not removed from the server inventory. That holds whenever its material blocks movement: gravel slabs, dirt, cobblestone, planks, pressure plates, loose snow.
  - On a ProxyBlock target (a gravel slab onto a gravel slab), neither side shrinks the count, so the stack never runs out.
  - On a vanilla-id FC target (dirt, grass, stone, planks, cobble), the client predicts the shrink and the server does not apply it. The hotbar drops while the server keeps the items, and the client still sends use packets with a hand it shows as empty.
  - Sneaking, or clicking a block with no FC identity, uses 1.20.1's `BlockItem.place`, which shrinks correctly (`CHANGELOG.md:827-832`, MISSED-1). That explains "not always".
- **Mechanism (re-derived at HEAD):**
  1. The HEAD item step skips BlockItems (`ServerPlayerGameModeMixin.java:180`). A non-sneaking click reaches `blockstate.use` (1.20.1 `ServerPlayerGameMode.java:343`), which is `ProxyBlock.use` (`fcHeld` captured at `:579`; Stage 2 at `:590`) or `BlockBehaviorMixin.btw$use` (`fcHeld` at `:253`; Stage 2 at `:260`/`:263`). Both call `ProxyBlock.tryPlaceHeldItem` (`:629-658`), which calls `fcHeld.tryPlaceItemIntoWorld` (`Modern-Common/.../ItemStack.java:265-271`).
  2. Modern-Common `ItemBlock.onItemUse` sets the block, then calls `world.NotifyNearbyAnimalsOfPlayerBlockAddOrRemove` (`ItemBlock.java:207`) **before** `stack.stackSize--` (`:216`).
  3. For a survival player, the notify runs when `blockMaterial.blocksMovement()` is true (`Modern-Common/.../World.java:996`). It then queries `getEntitiesWithinAABB(EntityAnimal.class, ±8/±4/±8)` (`:1001-1003`). The materials involved all block movement (`Material.java:25`, `:33`, `:75-77`; only `MaterialLogic` and `MaterialTransparent` don't, `MaterialLogic.java:20-22`):
     - the gravel slab is `Material.sand` (`Server/.../FCBlockSlabSandAndGravel.java:30`);
     - the loose snow slab is `Material.craftedSnow` (`FCBlockSnowLooseSlab.java:27`);
     - the wooden pressure plate is `fcMaterialPlanks` (`Common/.../FCBlockPressurePlatePlanks.java:25`; `FCBetterThanWolves.java:52`).
  4. `WorldBridge.getEntitiesWithinAABB` passes every non-item entity in the box to `matchesFcClass` (`WorldBridge.java:1800-1802`). That predicate calls `extractFcEntity` **before** the class test (`:2172-2176`), so the query class does not matter. For a Player, `extractFcEntity` returns `PlayerBridge.getOrCreate(p)` (`:1888-1890`). Within survival reach, the placing player is in the box. There is no reentrancy guard.
  5. `getOrCreate` always calls `syncFromReal` (`PlayerBridge.java:125`), which runs `invBridge.sync()` (`:218`). `syncSlot` builds a **new** FC stack with `ItemStackHelper.toFcStack` (`InventoryBridge.java:71`), installs it and rebaselines it (`:85-87`).
  6. The decrement at `ItemBlock.java:216` hits the detached `fcHeld`. The write-back that follows (`ProxyBlock.java:598` `writeBackAll`, or `BlockBehaviorMixin.java:275` `syncInventoryToReal`) compares the fresh snapshot with the fresh baseline (`fcChanged`, `InventoryBridge.java:215-221`) and writes nothing. The click returns SUCCESS.
  7. The slab-combine path decrements first (`Server/.../FCItemBlockSlab.java:86`) and notifies after (`:93`), so the nested sync discards the pending edit. `FCItemBlockAestheticNonOpaque.java:253`/`:260` and `FCItemBlockCompanionCube.java:196`/`:203` follow the same pattern. `checkNoEntityCollision` (`:79`) does not trigger it, because it uses `level.getEntities` with no extraction (`WorldBridge.java:1845-1849`).
  8. A second trigger, the airborne-on-ladder rule, is CF2-I5.
  9. Today the item step is immune. `ServerPlayerGameModeMixin.java:248` uses `writeBackCurrentItem`, which adopts a detached stack (`InventoryBridge.java:155-170`), so FC non-block items (seeds, stakes) are consumed. The planned I-1 removes that path (CF2-I6).
- **Why the client never corrects:** `handleUseItemOn` sends only two block updates (1.20.1 `ServerGamePacketListenerImpl.java:1057`, `:1070-1071`). `broadcastChanges` sees the server stack equal to `remoteSlots` and sends nothing (`AbstractContainerMenu.java:225-239`). The client predicted the placement (`MultiPlayerGameMode.java:290-292`, `:323`, `:334-339`).
- **Discriminator:** non-solid blocks (rails, `Material.circuits`, `BlockRailBase.java:19`) are consumed even through Stage 2, because no notify runs.
- **Builds:**
  - The general ItemBlock trigger (notify before decrement) was added by `ba91232` (1.4.1-alpha.5). `git log -S` finds no earlier notify in `ItemBlock.java`. `javap` shows the call in every jar from 1.4.1-alpha.5 on; in 1.6.3 it sits at offset 472, before the decrement at 514-519.
  - The slab-combine trigger and the ladder trigger (CF2-I5) lose the decrement in **every** build, 1.3.0-alpha.3 included. There, `InventoryBridge.sync` also rebuilt every FC object (`48e9825` `InventoryBridge.java:20-34`).
  - In 1.4.0-alpha.5 (`d55db5d`) only the combine and ladder triggers existed.
- **Not visible to the proof harness:** `ProofPlayer` extends `FakePlayer` (`TickProof.java:154`) and is never added to the level (`:385-392`), so `Level.getEntities` never yields it. The P10 regression row below therefore passes at HEAD and cannot catch this.
- **4.B:** every placement consumes one item from the live inventory stack (`Common/.../FCItemPlacesAsBlock.java:111` notifies and `:116` decrements, both on the live stack; `vanilla/server/.../InventoryPlayer.java:47-50`). After every place packet, 1.5.2 also clears an emptied slot and resends the held slot when it differs (`vanilla/server/.../NetServerHandler.java:551-569`).
- **At HEAD:** yes. Nothing in `[Unreleased]` fixes it, and Known issues does not list it. `STUB_CATALOGUE.md:1642-1643` records the underlying caveat as "Pre-existing" but not as a duplication bug.
- **Fix direction (one general rule: a re-sync must never replace or discard an FC stack that FC may still hold):**
  1. **Preferred.** In `InventoryBridge.syncSlot` (`:67-88`), keep the existing FC object while the MC slot is still `baseMc[i]` (same object, non-empty) with the baseline id, count, damage and tag. If `fcChanged(i)`, write it through first with `writeBackSlot(i)`, then keep the object. Keep `syncFc[i]` consistent, so the adoption check in `writeBack` (`:165`) stays valid. This matches 1.5.2's live `ItemStack[]`, covers every caller and every nested trigger, and retires `STUB_CATALOGUE.md:1642-1643`.
     - Risk to test: pending FC edits that a later sync silently discards today would now be committed mid-call. That is 4.B-faithful, but it may surface latent paths. The creative restore in `tryPlaceHeldItem` (`:646-657`) must still run after FC returns, and it does.
  2. **Smaller alternative (case-specific, not preferred).** At `ProxyBlock.java:598` and `BlockBehaviorMixin.java:275`, call `writeBackCurrentItem(fcHeld)` before `writeBackAll`.
  3. **Defence in depth.** For entity queries, `extractFcEntity` needs only position and AABB. A position-only refresh also saves a 40-slot rebuild per FC entity query near a player. The per-tick sync (`ServerPlayerMixin.java:117-151`) keeps the inventory at most one tick stale.
  4. **Client reconciliation.** After a Stage-2 placement with a held BlockItem, resend the main-hand slot (or `inventoryMenu.sendAllDataToRemote`, as 1.20.1 `useItem` does at `ServerPlayerGameMode.java:309`), mirroring 1.5.2 `NetServerHandler.java:551-569`.
  
  Keep the animal notification: it is 4.B behaviour.
- **Overlap:** no planned follow-up fixes this.
  - I-2 makes `canPlaceEntityOnSide` and `checkNoEntityCollision` follow 1.5.2 (`followups_recon.md:541-543`). That adds a nested sync only when extraction hits entities in the target cell's box, which is low risk.
  - I-2's resync covers REFUSED only (`:550`). Extending it to PLACED is the natural home for fix 4.
  - **I-1 deletes the adopting path** (CF2-I6). The CF2-I1 fix must land before or with I-1 (wave B).
  - The C1 leftover, the FCContainerMenu live result stack, is unrelated.
- **Repro (needs a joined dev client with `btwclientprobe`; follow the dev-run concurrency rule).** On the default flat world the floor is y=-61 and the player stands at y=-60. Targets go at floor level, and the checks look at y=-60.
  ```text
  @world placeconsume
  gamerule doDaylightCycle false
  gamerule doMobSpawning false
  difficulty peaceful
  kill @e[type=!minecraft:player]
  @waitfor 300 :: execute if entity @a
  @ticks 100
  gamemode survival @a
  tp @a 0.5 -60 0.5 0 60
  # G1 gravel slab onto a gravel slab (ProxyBlock target, combine path; lossy in every build)
  setblock 0 -60 1 minecraft:air
  setblock 0 -61 1 betterthanwolves:block_175
  clear @a
  item replace entity @a hotbar.0 with betterthanwolves:block_175 4
  @ticks 20
  say PROBE holduse 1
  @ticks 20
  @check G1 combined to full gravel :: execute if block 0 -61 1 minecraft:gravel
  @check G1 server used one (HEAD FAIL, Count 4b) :: execute if entity @a[nbt={SelectedItem:{Count:3b}}]
  # G3 control: deepslate has no FC identity, so 1.20.1 BlockItem runs (HEAD PASS)
  setblock 0 -60 1 minecraft:air
  setblock 0 -61 1 minecraft:deepslate
  clear @a
  item replace entity @a hotbar.0 with minecraft:cobblestone 4
  @ticks 20
  say PROBE holduse 1
  @ticks 20
  @check G3 placed :: execute if block 0 -60 1 minecraft:cobblestone
  @check G3 control used one (HEAD PASS) :: execute if entity @a[nbt={SelectedItem:{Count:3b}}]
  # G5 discriminator: rail (no notify) through Stage 2 on dirt (HEAD PASS proves the notify is the trigger)
  setblock 0 -60 1 minecraft:air
  setblock 0 -61 1 minecraft:dirt
  clear @a
  item replace entity @a hotbar.0 with minecraft:rail 4
  @ticks 20
  say PROBE holduse 1
  @ticks 20
  @check G5 rail placed :: execute if block 0 -60 1 minecraft:rail
  @check G5 rail used one (HEAD PASS) :: execute if entity @a[nbt={SelectedItem:{Count:3b}}]
  # G2 cobblestone on dirt (vanilla-id FC target; HEAD: placed PASS, used-one FAIL)
  setblock 0 -60 1 minecraft:air
  setblock 0 -61 1 minecraft:dirt
  clear @a
  item replace entity @a hotbar.0 with minecraft:cobblestone 4
  @ticks 20
  say PROBE holduse 1
  @ticks 20
  @check G2 placed :: execute if block 0 -60 1 minecraft:cobblestone
  @check G2 server used one (HEAD FAIL) :: execute if entity @a[nbt={SelectedItem:{Count:3b}}]
  say PROBE hand
  ```
  - At HEAD the client log shows `hand minecraft:cobblestone x3` while the server holds 4: the desync.
  - Manual: in survival, without sneaking, place gravel slabs on top of a gravel slab. The count never drops. Sneak-place the same slab, and it is used.

### CF2-I2. Phantom items on shift-click are server-held placements (likely, high)

- **Mechanism:** these are items the server never removed (CF2-I1), revealed when a click resyncs the slot.
  1. On vanilla-id FC targets (village oak planks, grass, dirt), the client predicted the shrink and the server did not apply it. Neither side then speaks: `handleUseItemOn` has no slot resync, and `remoteSlots` still equal the unchanged server stack. The client shows the slot empty while the server holds the item.
  2. Shift-click in the 2x2 inventory screen goes through vanilla `InventoryMenu`; the port does not replace it (`FCCraftingRecipes.java:42`).
     - The client predicts the move into the slot it believes empty: main inventory to the hotbar, first empty from the left (1.20.1 `InventoryMenu.java:174`); the crafting result into slots 9-45 in reverse, hotbar rightmost first (`:151`).
     - The packet's changed-slot map becomes the server's remote view (`ServerGamePacketListenerImpl.java:1552-1553`).
     - The server puts the item in the next truly empty slot. `broadcastChanges` (`:1561`) then sends the real content of the slot the client predicted into, so the hidden pressure plate or loose snow slab appears "from nowhere".
  3. The first village pressure plate was most likely a normal pickup (1.20.1 plains houses have fence-and-pressure-plate tables). It could also have been an earlier reveal.
  4. Other reveals: picking up more of the same item, opening any container (the full state is sent on open), relogging, and death (CF2-I3).
- **Evidence:** the CF2-I1 chain. No port code resends slots (`grep` for `sendAllDataToRemote`, `setRemoteSlot` and `ClientboundContainerSetSlotPacket` in `Forge/src` finds none). Ids 72, 1048, 22547 and 22567 are unrelated, so id aliasing is ruled out.
- **Builds:** the pressure plate (non-slab ItemBlock path) needs 1.4.1-alpha.5 or later. A loose snow slab placed onto a loose snow slab is the combine path (`Common/.../FCItemBlockSnowLooseSlab.java:21` extends `FCItemBlockSlab`), which is lossy in every build.
- **4.B:** not authentic. 4.B consumes placed blocks and resyncs the held slot after every place packet.
- **Fix direction:** CF2-I1, plus CF2-I1 fix 4 as defence in depth. Crafting and quick-move need no change (CF2-I4).
- **Repro:** after CF2-I1 G2 (the server holds 4 cobblestone, the client shows 3):
  ```text
  @check reveal: server still holds 4 (HEAD PASS) :: execute if entity @a[nbt={SelectedItem:{Count:4b}}]
  ```
  Then, by hand on the joined client, open the inventory and shift-click any main-inventory item toward the hotbar, so the client targets slot 0. The "used" cobblestone reappears.

### CF2-I3. Placed blocks return after death (likely, high)

- **Mechanism:**
  - The "placed" blocks never left the server inventory (CF2-I1). Explosion drops are BlockItems. Placed without sneaking on BTW terrain, they go through Stage 2 and are not consumed.
  - Death drops the server's real inventory (1.20.1 `Player.java:607-611`, `inventory.dropAll` when `keepInventory` is off; the port turns `keepInventory` off for new worlds, `WorldGamerulesHandler.java:58`). So the blocks are in the death pile and come back on pickup.
  - With `keepInventory` on, `restoreFrom` copies the whole inventory (1.20.1 `ServerPlayer.java:1133-1134`, as well as `:1124` for keepEverything). Then the server-held blocks are literally "put back in my inventory" on respawn.
- **Ruled out as causes:**
  - `ServerPlayerMixin.btw$restoreFrom` (`:206-249`) copies only FC spawn, penalty and food fields;
  - `FCLoginHandler.onPlayerClone` (`:87-94`) only binds the username;
  - `PlayerBridge.onDeath` (`:411-417`) delegates to `ServerPlayer.die`;
  - the new player gets a new bridge (`PlayerBridge.java:144-148`);
  - the per-tick sync writes no inventory (`:248-260`).
- **Builds:** 1.4.1-alpha.5 or later (creeper drops are full blocks, not slabs).
- **4.B:** not authentic. A placed block has left the inventory.
- **Fix direction:** CF2-I1. The death and respawn code is correct.
- **Repro:** last in the CF2-I1 plan, after G2:
  ```text
  kill @a
  @ticks 10
  @check G4 no phantom (HEAD FAIL) :: execute unless entity @e[type=minecraft:item,nbt={Item:{id:"minecraft:cobblestone",Count:4b}}]
  @check G4 after-fix drop is 3 :: execute if entity @e[type=minecraft:item,nbt={Item:{id:"minecraft:cobblestone",Count:3b}}]
  ```
  Manual: place 3 of 4 dirt on grass without sneaking, then `/kill` and pick up the pile. You get 4 dirt.

### CF2-I4. Crafting and moving the result do not create items (ruled out)

- No crafting or quick-move path creates items:
  - Sharp stone is one loose stone, shapeless (`Server/.../FCRecipes.java:296-298`).
  - `FCCraftingRecipes.fcRemainingItems` (`:339-368`) computes remainders and adds nothing to the inventory.
  - Bark from chopping logs on the grid is ejected as item entities (`Common/.../FCItemBlockLog.java:28-49`), not inserted.
  - The 2x2 grid is MC's own `InventoryMenu`, whose `quickMoveStack` only moves stacks (`InventoryMenu.java:143-196`).
  - In the FC workbench, player slots are `PlayerMappedSlots` over the real Inventory (`FCContainerMenu.java:255-271`), and adapter mirrors fold back only FC container slots (`:296-360`). Opening the FC workbench sends the full container state, which itself reveals hidden items.
  - The C1 leftover, the FCContainerMenu live result stack, concerns what modded `onCraftedBy` listeners see. It creates nothing.
- **Repro (control):** `/clear`, craft sharp stone in the 2x2 grid and shift-click it, with no earlier non-sneaking placements in the session. Nothing extra appears. After CF2-I1 G2, a phantom appears only in the slot the client believed empty.

### CF2-I5. A placement while airborne on a ladder loses its decrement for any material (confirmed, medium; not in the report)

- **Mechanism:**
  - Modern-Common `EntityPlayer.canPlayerEdit` evaluates `!onGround && !inWater && !isOnLadder() ...` (`EntityPlayer.java:292-299`). It runs at the start of `ItemBlock.onItemUse` (`:156`) and of `FCItemBlockSlab.onItemUse` (`:38`).
  - `PlayerBridge.isOnLadder` (`:663-664`) calls `realPlayer.onClimbable()`, which `LivingEntityMixin.btw$gateLadder` intercepts (`:95-104`) with `PlayerBridge.getOrCreate` and `syncFromReal`. That replaces the held FC stack before the decrement, exactly as in CF2-I1.
  - The trigger needs `onGround` false and does not depend on the material, so rails placed from a ladder are free as well.
- **Builds:** every build. `btw$gateLadder` dates from `64016ac`, the airborne rule from `bd4e535`.
- **Side note for I-2 (placement validity, not inventory):** `PlayerBridge` never syncs the `inWater` and `ridingEntity` fields that the airborne rule reads (`Modern-Common/.../Entity.java:16`, `:62`; `PlayerBridge.java:194-224`). So FC refuses a swimming or riding player, and 1.20.1 then places with vanilla rules. 4.B allows placing in water and while riding.
- **4.B:** the mid-air rule is authentic (`FCItemPlacesAsBlock.java:66`). The free block is not.
- **Fix direction:**
  - CF2-I1 fix 1 covers it.
  - Optionally, `btw$gateLadder` can read the bridge without a full sync, since the per-tick sync keeps `GetHealthPenaltyLevel` current.
  - Hand the `inWater`/`ridingEntity` sync to I-2.
- **Repro (manual, joined client):**
  1. Climb a ladder column holding forward, without sneaking.
  2. While airborne on the ladder, place a rail on an adjacent dirt block, then a cobblestone.
  3. After each first use from a stack of 4, check `execute if entity @a[nbt={SelectedItem:{Count:3b}}]`.
  
  HEAD fails for both. The same rail placed from the ground passes (G5).

### CF2-I6. The planned I-1 would widen CF2-I1 to FC non-block items (likely, high if I-1 lands first; not in the report)

- **Mechanism:**
  - I-1 deletes the HEAD pre-emption `btw$useItemOn` (`ServerPlayerGameModeMixin.java:165-254`) and its adopting write-back (`:248`) (`followups_recon.md:511-520`).
  - Its replacement, `btw$fcItemStep`, uses `syncInventoryToReal`, which is `writeBackAll` with no adoption. Under R1, "Stage 2 stays for every item" (`followups_recon.md:77-83`).
  - So for non-block items, Stage 2 runs FC `onItemUse` first and writes back with `writeBackAll`.
  - Example: the FC stake is `new ItemReed(..., fcBlockStake)` (`Server/.../FCBetterThanWolves.java:1486`). Modern-Common `ItemReed.onItemUse` notifies (`:76`) before it decrements (`:81`), and `FCBlockStake` is `Material.wood` (`FCBlockStake.java:31`). After I-1, stakes placed on FC blocks would not be consumed.
- **At HEAD:** not present; the item step adopts the detached stack.
- **Fix direction:** land CF2-I1 fix 1 before or with I-1. Failing that, `btw$fcItemStep` and the Stage-2 sites should call `writeBackCurrentItem(fcHeld)` before `writeBackAll`.
- **Overlap:** directly I-1 (wave B) and R4. Add a P-USE regression row for item consumption with a joined client.
- **Repro:** after I-1, with a joined client, give 4 stakes (FC item 22235) and right-click the top of a dirt block without sneaking. Then check `execute if entity @a[nbt={SelectedItem:{Count:3b}}]`. It passes at HEAD and is expected to fail after I-1 unless CF2-I1 is fixed.

  *(2026-10-20: the stake's registered id is `betterthanwolves:item_22491`: item id 256 + ParseID 22235 (Modern-Common `Item.java:211`), registered as `item_<id>` (`BTWRegistration.java:153`); `item_22235` names no item. INV-RESYNC landed before I-1, and this row, S1 of the joined-client plan, passes after I-1; see [Fix status](#fix-status-2026-11-03-uncommitted-working-tree).)*

### CF2-P10. 1.3.0-alpha.3: Stage-2 placements against vanilla-id blocks were never charged (fixed-since)

- **Mechanism (1.3.0-alpha.3, `48e9825`):**
  - `ServerPlayerGameModeMixin.btw$useItemOn` returned at once for every BlockItem (`:85-92`).
  - `BlockBehaviorMixin.btw$use` ran FC Stage 2 (`:243-250`) and returned SUCCESS with **no** write-back (`:254-258`). `ProxyBlock.use` did write back (`:418`).
  - The next `syncFromReal` rebuilt every FC slot (`PlayerBridge.java:42`, `:74-98`; `InventoryBridge.java:20-33`), discarding the decrement (`ItemBlock.java:53`).
  - So every block placed against stone, dirt, grass, cobble, planks, logs, sand, gravel or the 1.5.2 slabs was free. The client desync and the reveals were the same as in CF2-I1 and CF2-I2.
- **At HEAD:** this mechanism is gone. `f6ec9be` (1.4.0-alpha.5 and later) added the write-back at `BlockBehaviorMixin.java:271-275` (`syncInventoryToReal`, `PlayerBridge.java:230`, selective `InventoryBridge.writeBackSlot` at `:228-262`).
- **Reconciliation with CF2-I1:**
  - The commenter did not run 1.3.0-alpha.3 (creepers exploded), so CF2-I1, not CF2-P10, explains items 4, 5a and 10.
  - The write-back that fixed P10 is itself defeated by CF2-I1's nested re-sync: from 1.4.1-alpha.5 for movement-blocking blocks, and in every build for slab combines and ladder placements.
  - So **the symptom is not fixed**. The CHANGELOG "Fixed" bullet proposed for P10 must wait until CF2-I1 is fixed, or players will read it as a fix they don't see.
- **Fix direction:** none for P10 itself. Keep a regression row, so that I-1 and I-2, which restructure Stage 2, cannot drop the `btw$use` write-back at `BlockBehaviorMixin.java:275`.
- **Repro:**
  ```text
  btwproof use 0 -61 12 minecraft:cobblestone 4 up
  # expect 'use SUCCESS left=3 ...'; a 48e9825 build would read left=4
  @check P10-1-placed-cobble-placed=PASS :: execute if block 0 -60 12 minecraft:cobblestone
  ```
  This row passes at HEAD only because the proof FakePlayer is not in the level, so it cannot catch CF2-I1 (see CF2-I1).

---

## 4. Placement: flowers on stone and slabs

> "Was able to place a flower on stone + slabs but they broke on the slabs"

### CF2-P8a. BTW's "cannot be placed here" check is a stub (confirmed, medium)

- **Effect:**
  - Every placement by a 4.B ItemBlock-family item against an FC-mapped block (a mapped vanilla id or an FC ProxyBlock) skips 4.B's validity rules. That covers flowers, saplings, mushrooms, tall grass, torches, rails and hooks.
  - Because the stub also drops 1.5.2's free-or-replaceable target test, such a placement can overwrite a solid block. This was runtime-proven for the tripwire hook: placed into the solid superflat `grass_block` cell (`compat/refresh2/out_after3_a_t2_removed.txt:39-40`; FU-removed-tripwire-hook-placement).
- **Mechanism.** The player is not sneaking, holds the flower in the main hand and right-clicks an FC-mapped block (stone 1, cobble 4, slabs 44/126, any FC ProxyBlock slab):
  - 1.20.1 calls `blockstate.use` (`ServerPlayerGameMode.java:342-343`).
  - `BlockBehaviorMixin.btw$use` (`:215-280`), or `ProxyBlock.use` (`:588-591`), runs FC Stage 1 (stone and slabs return false), then Stage 2 `tryPlaceHeldItem` (`:629-658`). That calls FC `ItemStack.tryPlaceItemIntoWorld`, then Modern-Common `ItemBlock.onItemUse` (`:145-223`); items 37 and 38 are plain ItemBlocks (`Modern-Common/.../Block.java:2070-2071`, `:2178-2183`).
  - `onItemUse` offsets the target to the cell above (`:161-175`) and calls `world.canPlaceEntityOnSide(38, ..., false, side, null, stack)` (`:181`).
  - **`WorldBridge.canPlaceEntityOnSide` (`:1852-1860`) is a stub.** It returns true whenever the target cell holds no entity. It never tests that the target is free or replaceable, and it never calls `newBlock.canPlaceBlockOnSide`.
  - So the runtime block `FCBlockFlowerBlossom` (`BTWLifecycle.java:526-532`) is never consulted. It inherits 4.B's soil rule from Modern-Common `BlockFlower.canPlaceBlockAt` (`:73-75`).
  - The flower is set (`:197`), and `btw$use` returns SUCCESS (`:277-279`). `ServerPlayerGameMode` therefore returns before `ItemStack.useOn`, so 1.20.1's own refusal never runs.
  - On the client, `btw$use` does nothing (not a ServerLevel), and `BlockItem.getPlacementState` predicts FAIL, because `canSurvive` is false on stone. The flower appears from the server's block update. Because of `withPredictingActor` (`WorldBridge.java:2199-2206`), the placing player hears no place sound.
- **Condition:** the player must stand on the ground. BTW's airborne rule refuses a jumping player, and the 1.20.1 route then refuses too.
- **4.B:** a flower is refused on stone and on every slab:
  - `World.canPlaceEntityOnSide` (`vanilla/server/.../World.java:3377-3407`) runs the replaceable check (`:3399-3405`), then `par1 > 0 && var10 == null && var11.canPlaceBlockOnSide(...)` (`:3407`). That reaches `FCBlockPlants.canPlaceBlockAt` (`Server/.../FCBlockPlants.java:71-75`), which needs `CanWildVegetationGrowOnBlock` from the block below.
  - Only dirt, loose dirt, grass, farmland, planters and planter soil answer true. No slab class does.
  - The 4.B client also refuses before sending (`vanilla/client/.../PlayerControllerMP.java:419`, `:426`; `FCItemPlacesAsBlock.java:131-135`, `:180-202`).
  - 1.20.1 refuses too (`BlockItem.java:118-120`, `:152-155`; `BushBlock.java:18-31`).
- **At HEAD:** yes, in every public build. The stub body is unchanged since `520e554` (`48e9825` `WorldBridge.java:1112-1120`). Two docs mislead:
  - `CHANGELOG.md:827-832` (Known issues) ends with "Placing against any other block uses BTW's code", which implies BTW's checks run.
  - `STUB_CATALOGUE.md:3090-3093` lists `canPlaceEntityOnSide` among the ported `onItemUse` steps without saying it is a stub.
- **Fix direction:** land I-2 design A. `WorldBridge.canPlaceEntityOnSide` becomes 1.5.2 `World.java:3377-3407`, in this order:
  1. the entity check;
  2. the free-or-replaceable target;
  3. the anvil-on-circuits case;
  4. `id > 0 && free && newBlock.canPlaceBlockOnSide(...)`.
  
  Keep corrections i-iv and add A2. Flowers need no Modern-Common change. With design G (decision 2(a), a REFUSED Stage 2 gives `sendAllDataToRemote` plus FAIL), the fallback never runs. Without G, the 1.20.1 fallback also refuses through `PlantSoilMixin` (`:107-145`, `:173-176`, `:210-222`). In the I-2 place-sweep diff, expect flowers, saplings, mushrooms and tall grass on stone or slab tops to flip from placed to refused.
- **Overlap:** fully covered by the planned I-2 (`followups_recon.md:536-558`, design A/A2/G), which is not in code yet. It shares its root cause with the parked tripwire result. The dispenser and falling-block callers (`ItemBlock.java:292`, `skipEntities=true`) share the stub; I-2 decision 1(a) covers them.
- **Repro (`proof_run`, existing verbs).** The `use` verb stands its FakePlayer on the clicked block with `onGround` set, so the FC route runs. Expectations are HEAD / after I-2:
  ```text
  @world flowerplace
  gamerule doDaylightCycle false
  gamerule doMobSpawning false
  gamerule randomTickSpeed 0
  forceload add 0 0 31 15
  kill @e[type=!minecraft:player]
  @ticks 20
  # P8-1 poppy on stone top (FC Stage 2). HEAD FAIL / after PASS
  setblock 4 -60 4 minecraft:stone
  btwproof use 4 -60 4 minecraft:poppy 1 up
  @check P8-1-poppy-refused-on-stone=PASS :: execute unless block 4 -59 4 minecraft:poppy
  # P8-2 poppy on a bottom smooth stone slab (FC 44). HEAD FAIL / after PASS
  setblock 8 -60 4 minecraft:smooth_stone_slab[type=bottom]
  btwproof use 8 -60 4 minecraft:poppy 1 up
  @check P8-2-poppy-refused-on-slab=PASS :: execute unless block 8 -59 4 minecraft:poppy
  # P8-3 dandelion on cobblestone. HEAD FAIL / after PASS
  setblock 12 -60 4 minecraft:cobblestone
  btwproof use 12 -60 4 minecraft:dandelion 1 up
  @check P8-3-dandelion-refused-on-cobble=PASS :: execute unless block 12 -59 4 minecraft:dandelion
  # P8-4 control: deepslate has no FC identity, so the 1.20.1 route refuses. PASS / PASS
  setblock 16 -60 4 minecraft:deepslate
  btwproof use 16 -60 4 minecraft:poppy 1 up
  @check P8-4-vanilla-route-refuses=PASS :: execute unless block 16 -59 4 minecraft:poppy
  # P8-5 control: superflat grass accepts. PASS / PASS
  btwproof use 20 -61 4 minecraft:poppy 1 up
  @check P8-5-poppy-on-grass=PASS :: execute if block 20 -60 4 minecraft:poppy
  ```
  Manual: standing on the ground, hold a poppy and right-click the top of stone or a smooth stone slab without sneaking. The poppy appears with no place sound. Sneaking, or jumping while clicking, places nothing.

### CF2-P8b. The flowers that broke on the slabs (ruled out as a separate bug)

- **Mechanism:** no code path treats a slab differently from stone. At HEAD, FC answers false for both: `GetModernSoilPlantSurvival` returns -1 for mapped soils, and `CanGrowOnBlock` is false on `FCBlockStep` (44) and stone (1). Every flower placed through CF2-P8a is invalid, and whichever trigger comes first removes it.
  1. **All builds.** `Level.setBlock` runs `updateNeighbourShapes` on the six neighbours of any cell that is set (1.20.1 `Level.java:250-261`; `BlockBehaviour.java:731-737`). The flower's `BushBlock.updateShape` returns AIR because `canSurvive` is false (`BushBlock.java:22-24`), and `updateOrDestroy` destroys it with a drop (`Block.java:155-160`).
     - `destroyBlock`'s own `setBlock` (`Level.java:291-294`) runs the same shape update on its neighbours. So placing a second invalid flower next to the first pops the first, and that removal pops the second.
     - A row of flowers along a slab row destroys itself as soon as the second one is placed, while a lone flower on stone stays (before 1.6.1). That fits "they broke on the slabs".
  2. **From `9c05019`** (first shipped in 1.6.1-alpha.5):
     - FC random ticks for 37/38 (`TickPolicy.java:273-274`, `:456-474`; `BlockStateBaseTickMixin.java:23-29`; `BlockStateRandomTickMixin.java:55-62`) run `BlockFlower.updateTick`, then `checkFlowerChange`, then `canBlockStay`, which is false.
     - FC neighbour changes reach `checkFlowerChange` too, through `BlockBehaviorMixin.btw$neighborChanged`.
     - Every such flower is removed after about 68 s on average at `randomTickSpeed` 3. Up to 1.4.1, Modern-Common `checkFlowerChange` was empty (`ba91232` `BlockFlower.java:83`), so only (1) existed.
  3. **Possible:** if "slabs" meant BTW's loose slabs, a slab that falls or is removed takes the flower's support away, and the flower pops by (1). Their hooks act only on the slab (`Server/.../FCBlockSlabFalling.java:50-74`; `Common/.../FCBlockSlabAttached.java:29`, `:76`).
- **4.B:** removing a flower whose soil does not accept it, on a neighbour change or a random tick, is authentic (`vanilla/server/.../BlockFlower.java:73-94`). In 4.B the flower could not have been placed there in the first place.
- **Fix direction:** nothing separate; I-2 (CF2-P8a) removes the cause. Flowers already placed on stone or slabs by earlier versions pop on 1.6.1 and later and drop their item, which is acceptable 4.B parity. Optionally mention it in the I-2 CHANGELOG bullet.
- **Repro (append to the P8a plan; `setblock` keeps an unsupported flower, so these rows do not depend on I-2):**
  ```text
  # P8-6 chain shape update: two adjacent slab poppies destroy each other (all builds). PASS / PASS
  setblock 4 -60 8 minecraft:smooth_stone_slab[type=bottom]
  setblock 5 -60 8 minecraft:smooth_stone_slab[type=bottom]
  setblock 4 -59 8 minecraft:poppy
  setblock 5 -59 8 minecraft:poppy
  @ticks 2
  @check P8-6-first-slab-poppy-popped=PASS :: execute unless block 4 -59 8 minecraft:poppy
  @check P8-6b-second-slab-poppy-popped=PASS :: execute unless block 5 -59 8 minecraft:poppy
  @check P8-6c-poppies-dropped-as-items=PASS :: execute if entity @e[type=minecraft:item,x=3,y=-60,z=7,dx=3,dy=3,dz=3]
  kill @e[type=minecraft:item]
  # P8-7 FC random tick removes a lone stone poppy. >=1.6.1/HEAD PASS; <=1.4.1 FAIL. Unchanged by I-2
  setblock 8 -60 8 minecraft:stone
  setblock 8 -59 8 minecraft:poppy
  @check P8-7-stone-poppy-random-tick=PASS :: btwproof randomtickarea minecraft:poppy 8 -59 8 1 1 1 1 expect changed == 1
  kill @e[type=minecraft:item]
  ```

### CF2-P8c. Placing an id-38 flower through FC turns it into a poppy (confirmed, medium; not in the report)

- **Effect:** every normal placement (main hand, not sneaking) of 8 of the 9 small flowers against grass, dirt or any other BTW-handled block places a poppy. The identity is lost for good: breaking it gives a poppy. A player who did not notice the flip would then report item 9.
- **Mechanism:**
  - The held 1.20.1 item has no `LegacyVariants` row (`LegacyVariants.java:192-195`). `ItemStackHelper.toFcStack` (`:447-460`) falls back to `getOrCreateLegacyId`, whose BlockItem branch resolves the block to 38 with damage 0 (`ProxyRegistry.java:934-938`, `:234-240`).
  - `Item.itemsList[38]` is a plain Modern-Common `ItemBlock`, whose `getMetadata` returns 0 (`ItemBlock.java:112-128`).
  - FC sets (38, 0). `WorldBridge.setBlock` reaches `resolveVanillaBlockState(38, 0)` (`:1028-1100`, `:944-947`), which falls back to the canonical POPPY (`ProxyRegistry.java:194-200`, `:826-829`).
  - On valid soil, the client predicts the daisy, and the server's block update replaces it with a poppy. The held stack keeps its identity, because `writeBackSlot` patches the count in place (`InventoryBridge.java:252-255`).
  - `btw$fixBlockFacing` (`ServerPlayerGameModeMixin.java:263-291`) never corrects the variant.
- **Builds:** at HEAD and from 1.4.0-alpha.5. In 1.3.0-alpha.3 the pick was random per launch (`IdentityHashMap`), so even a poppy could come out as a tulip. No CHANGELOG bullet records that change.
- **Docs:** the general collapse is documented (`CHANGELOG.md:868-873`, `STUB_CATALOGUE.md:2420-2422`), but only as an item-storage issue, not as an everyday placement issue.
- **4.B:** n/a; 4.B had only the rose at 38 (`vanilla/server/.../Block.java:204`). In 1.20.1 every flower is its own block (`Blocks.java:194-204`).
- **Fix direction (needs a maintainer decision; tied to CF2-DR3):**
  - (a) Preferred: give the eight non-poppy flowers port-only subtypes 38:1-8 (1.7.2 numbering: 1 orchid, 2 allium, 3 bluet, 4-7 tulips, 8 oxeye). That means `variants(38, ...)`, `LegacyVariants both(38, m, ...)` and `case 38` in `ItemBlock.getMetadata`. It follows the pattern of the port-only planks 5:5-8 in the wood design (`woodtypes_design.md:8`, `:224`, `:228`). FC's drop (`damageDropped` returns 0) still needs CF2-DR3's identity rule.
  - (b) Narrower: in Stage 2, when FC placed exactly the legacy id of the held BlockItem's block, set the held block's own state at that cell. This fixes placement only.
- **Overlap:** not in I-2. Its nearest sibling is Q1, the dye shared-subtype defect (`followups_recon.md:1475`, `:1519`).
- **Repro (append to the P8a plan):**
  ```text
  # P8-8 oxeye daisy on superflat grass via FC Stage 2. HEAD FAIL (poppy) / PASS after the variant fix
  btwproof use 24 -61 4 minecraft:oxeye_daisy 1 up
  btwproof state 24 -60 4
  @check P8-8-daisy-stays-daisy=PASS :: execute if block 24 -60 4 minecraft:oxeye_daisy
  # P8-9 control: dandelion (37, a 1:1 mapping). PASS / PASS
  btwproof use 28 -61 4 minecraft:dandelion 1 up
  @check P8-9-dandelion-stays=PASS :: execute if block 28 -60 4 minecraft:dandelion
  ```
  Manual: right-click grass with an oxeye daisy or a tulip, without sneaking. It flashes as the daisy, then becomes a poppy. Sneaking places the daisy.

---

## 5. Mobs: creative targeting, zombified villagers

> "Mobs still target players in creative?" · "I was wondering if villagers are suppose to be zombified instead of normal?"

### CF2-M7. Mobs targeting creative players is 4.B behaviour (authentic, low)

- **Mechanism:**
  - Proximity scans exclude creative players in both 4.B and the port. `WorldBridge.getClosestVulnerablePlayer` uses 1.20.1's `NO_CREATIVE_OR_SPECTATOR` filter (`WorldBridge.java:2667-2677`; 1.20.1 `EntityGetter.java:97-100`, `EntitySelector.java:22-24`).
  - The new-AI player task is followed by `EntityAITarget.isSuitableTarget` on `disableDamage`. `CapabilitiesBridge.java:19-21` maps `disableDamage` to `abilities.invulnerable`, re-synced on every `getOrCreate` (`PlayerBridge.java:113-125`, `:217`).
  - `ProxyMob` runs no 1.20.1 goals (`ProxyMob.java:24`), and the frozen `EntityLiving`/`EntityCreature`/`EntityMob` ship (`Forge/build.gradle:762-776`).
  - So FC mobs never pick a creative player through a proximity scan.
- **The 4.B paths that still target a creative player:**
  - (a) A target acquired in survival survives a switch to creative. `EntityAITarget.continueExecuting` (`vanilla/server/.../EntityAITarget.java:50-82`), `EntityAIAttackOnCollide.continueExecuting` (`:76-80`) and the old-AI `EntityCreature` loop (`:61-97`) never re-check `disableDamage` (it is checked only on selection, `:137-140`). The mob keeps chasing and swinging, and the damage is refused (`EntityPlayer.java:923`; `PlayerBridge.java:358-370`, then `realPlayer.hurt`).
  - (b) Old-AI mobs hit by a creative player target that player: spider, cave spider, enderman, pig zombie, silverfish and blaze (`EntityMob.java:91-99`). Hitting an enderman also angers nearby endermen (`FCEntityEnderman.java:283`, `:575-593`). The port forwards every landed hit to FC (`ProxyMob.java:771-818`, `setRevengeTarget` at `:816`).
  - (c) Pig zombies hit by an explosion or a ghast anger the closest player in any mode. Hitting a BTW pig angers nearby pig zombies (`FCEntityPigZombie.java:49-58`; `FCEntityPig.java:94-108`).
  - (d) Wild wolves target a player who startles them, for example with a fire-starter attempt within 6 blocks (`FCItemFireStarter.java:49`, `:132-147`; `FCEntityWolf.java:437-446`).
  - (e) Feeding a wolf wolf meat makes it hostile to the feeder (`FCEntityWolf.java:274-286`).
  
  FC's `EntityAIHurtByTarget` edit explicitly ignores creative attackers (`EntityAIHurtByTarget.java:44-49`).
- **1.20.1 differs:** `TargetGoal.canContinueToUse` calls `canAttack`, and `Player.canBeSeenAsEnemy` is false once the player is invulnerable (`TargetGoal.java:37-45`; `LivingEntity.java:854-856`; `Player.java:853-855`). Vanilla mobs drop the target at once, which is what the commenter expected.
- **At HEAD:** yes, in every release (`48e9825` `WorldBridge.java:1711-1720`). `javap` of the shipped `EntityAITarget` shows the `disableDamage` read.
- **Fix direction:** none needed for 4.B parity. Record it as a known difference in `CURSEFORGE.md` and CHANGELOG Known issues.
  - Optional, by maintainer ruling: on `PlayerEvent.PlayerChangeGameModeEvent` to CREATIVE or SPECTATOR, clear every FC mob's `getAttackTarget()`, `entityToAttack` and `getAITarget()` that points at that player's bridge. This mirrors 1.20.1's `canContinueToUse` without touching FC code.
  - The spectator case has no 1.5.2 equivalent (today a mob follows an invisible, non-colliding spectator) and can be done without a ruling.
- **Repro (manual client; the proof FakePlayer is not in `level.players()`):**
  1. In survival at night, `/summon betterthanwolves:fc_zombie ~8 ~ ~`. Once it chases you, `/gamemode creative`. It keeps following and swinging, with no damage. A control `/summon minecraft:vindicator ~8 ~ ~` drops its target on the switch.
  2. Start in creative. An `fc_zombie` and an `fc_creeper` 8 blocks away never approach, and no fuse lights. If either approaches, the lookup has regressed.
  3. In creative at night, punch an `fc_spider`: it attacks (old AI, 4.B). Punch an `fc_zombie`: it does not retaliate.
  4. Use flint and steel within 6 blocks of an untamed `fc_wolf`: it targets you.

### CF2-MU3. Zombified villages are 4.B; the port's villages are not zombified (authentic; the port's gap is confirmed; medium)

- **Answer to the commenter:** yes. In 4.B survival worlds every villager in a generated village is a persistent villager zombie, and villages near the world spawn are partly or fully abandoned. A thrown soul urn cures a villager zombie into a trader. 4.B has no "nightmare" or "normal" mode: this is BTW 4.B0000003 (`gradle.properties` `modVersion`), with its hardcore rules always on.
- **The 4.B rule** (`vanilla/server/.../ComponentVillage.java:121-204`, `ComponentVillageStartPiece.java:89-120`):
  - Each village villager spawns as a persistent `FCEntityZombie` with `setVillager(true)` and its class set, unless the world's game type is creative (`:178-187`; creative worlds get villagers, `:188-198`).
  - Abandonment depends on the distance from the village to the world spawn (`Server/.../FCUtilsHardcoreSpawn.java:30-31`, `:35`, `:61-69`, `:76-86`):
    - under 2250 blocks (x4 on large biomes), level 2: each building has a 1-in-20 chance of a single farmer (`:128-144`);
    - 2250 to 3000 blocks, level 1: farmers, blacksmiths and butchers only (`:170-176`);
    - beyond 3000 blocks: every profession.
  - The cure is a thrown soul urn (`Common/.../FCEntityUrn.java:88-126`; `FCEntityZombie.java:393-403`). The golden-apple cure is removed (`:195-200`). Villager zombies do not burn in sunlight.
- **The port:**
  - Villages are 1.20.1 jigsaw structures, so `ComponentVillage` never runs.
  - `VanillaMobReplacer` turns their STRUCTURE villagers into normal `fc_villager`s, with the profession taken from the building (`VanillaMobReplacer.java:134`, `:386-392`, `:445-484`, `:479-483`). There is no zombify or abandonment step.
  - R6 makes STRUCTURE zombie villagers plain FC zombies (`:124`, comment at `:462-466`; identity is kept only for SPAWN_EGG, COMMAND and DISPENSER, `:467-468`).
  - R6's premise, "BTW never spawned villager zombies", is wrong for villages. It holds for natural zombie spawns (`FCEntityZombie.initCreature` skips the villager roll, `:183-192`) but not for villages (`ComponentVillage.java:178-187`). The premise is repeated in `CHANGELOG.md:575-576`, `STUB_CATALOGUE.md:2307-2309` and `:2340-2341`.
- **What the commenter saw:** on 1.4.0-alpha.5 and later, FC zombies hunt FC villagers (`FCEntityZombie.java:45-53` secondary target task; `FCEntityVillager.java:409-412`). Three kills in four become persistent villager zombies (`onKillEntity`, `:154-180`), drawn with the villager head (`CHANGELOG.md:116`). `ProxyPathfinderMob.die` passes the attacker to FC `onDeath` (`ProxyMob.java:961-985`), so `onKillEntity` fires.
- **Possible contributors:**
  - Village iron golems are vanilla and ignore FC mobs, because `ProxyMob` is not an `Enemy` (1.20.1 `IronGolem.java:78-80`).
  - 1.20.1 village sieges spawn EVENT zombies, which become `fc_zombie`s (`VillageSiege.java:35`, `:103-104`). Sieges exist in 1.5.2 too.
  - (In 1.3.0-alpha.3, vanilla zombie villagers were not replaced at all: `48e9825` `VanillaMobReplacer.java:48-74`.)
- **Severity:** medium (progression). At HEAD every 1.20.1 village hands out tradeable BTW villagers at once. The enchanting table, brewing stand and eyes of ender come only from BTW villagers (`CHANGELOG.md:635`). In 4.B the player must first cure a villager zombie with a soul urn.
- **Fix direction (needs a maintainer ruling: it changes gameplay, but restores the 4.B progression gate):** implement it in `VanillaMobReplacer`, never in FC code.
  - Trigger: a fresh STRUCTURE `Villager` or `ZombieVillager` inside a village (`level.structureManager().getStructureWithPieceAt(pos, StructureTags.VILLAGE)`).
  - Compute the abandonment level once per `StructureStart`, from the bounding-box centre's distance to `level.getSharedSpawnPos()` with FC's radii, and cache it.
  - Unless `server.getDefaultGameType()` is CREATIVE, spawn a persistent `fc_zombie` with `setVillager(true)` and the FC profession class. Otherwise spawn an `fc_villager`, with `SetDirtyPeasant(1)` at level 2.
  - Narrow R6 so village STRUCTURE zombie villagers keep their identity; igloo basements are a separate ruling.
  - Correct the three wrong-premise docs and the R6 comment, and mention the soul-urn cure in `CURSEFORGE.md`.
  - **Prerequisite:** prove the soul-urn cure end to end before shipping: urn impact, then `AttemptToStartCure`, then the conversion timer, then `convertToVillager`. If any step is broken, zombified villages become a hard block on the enchanting table and the brewing stand.
  - Out of scope: abandoned-village block decay.
- **Overlap:** none with the planned follow-ups. It corrects the shipped SW5/R6 rule (earlier triage SW5, SW6). It sits next to I-3 golems, but village iron golems ignoring FC mobs is a separate question.
- **Repro (`proof_run`):**
  ```text
  @world u3_village
  @prop spawn-npcs=true
  @prop spawn-monsters=false
  @prop gamemode=survival
  @prop difficulty=normal
  gamerule doMobSpawning false
  forceload add 3900 3900 4100 4100
  place structure minecraft:village_plains 4000 -60 4000
  @ticks 200
  @check U3a 4.B: no plain BTW villagers >3000 blocks out :: execute unless entity @e[type=betterthanwolves:fc_villager,x=3880,y=-64,z=3880,dx=240,dy=80,dz=240]
  @check U3b 4.B: villager zombies present :: execute if entity @e[type=betterthanwolves:fc_zombie,x=3880,y=-64,z=3880,dx=240,dy=80,dz=240]
  forceload add 200 200 400 400
  place structure minecraft:village_plains 300 -60 300
  @ticks 200
  @check U3c 4.B: fully abandoned village near spawn has no BTW villagers :: execute unless entity @e[type=betterthanwolves:fc_villager,x=180,y=-64,z=180,dx=240,dy=80,dz=240]
  ```
  - Difficulty must not be peaceful: frozen `EntityMob.java:55-58` kills FC mobs on peaceful.
  - U3b can pass by accident on a zombie-village variant; a stricter check needs a verb that prints FC `isVillager()`.
  - Expected at HEAD: U3a, U3b and U3c all FAIL.
  - Manual check of the commenter's mechanism: at night in a village, watch an `fc_zombie` kill an `fc_villager`. In 3 kills of 4, a persistent villager zombie with the villager head appears.

### CF2-MX1. FC code that scans `World.playerEntities` never sees a player (confirmed, medium; not in the report)

- **Mechanism:**
  - The class chain is `WorldBridge`, then `btw.modern.WorldServer`, then Modern-Common `World`. That `World` declares `public List playerEntities = new ArrayList()` (`World.java:21`), and nothing ever adds to it (1.5.2 did, in `vanilla/server/.../World.java:1362`, `:1414`, `:1428`).
  - `WorldBridge` overrides the `getClosest*Player` lookups and `getPlayerEntityByName` through `level.players()` (`WorldBridge.java:2631-2677`). But FC code that reads the field directly iterates an empty list.
  - In the 1.6.3 jar only `btw/modern/World`, `FCEntitySquid`, `FCTileEntityBeacon` and `FCTileEntityMillStone` reference it. `World` is not in the frozen-replacement list (`Forge/build.gradle:762-776`).
- **Effect:**
  - FC squids never choose a player as their night or darkness target (`Common/.../FCEntitySquid.java:819-853`).
  - BTW beacon effects and the dung cloud never reach players (`FCTileEntityBeacon.java:130-190`). This is currently masked: at HEAD the BTW beacon does not tick at all (a `ClassCastException`, "invalid for ticking"; `followups_recon.md:1187-1190`).
  - The mill stone's netherrack nausea never applies (`FCTileEntityMillStone.java:410-436`).
- **4.B:** squids hunt players in darkness or at night. The beacon applies its effects to non-creative players in range. Grinding netherrack gives nausea every 40 ticks within 10 blocks.
- **Fix direction:** in `WorldBridge`, make `playerEntities` a live `AbstractList` view of `level.players()`, mapped through `PlayerBridge.getOrCreate`. It is assigned in the constructor and is never stale (the HAI-1 lesson). FC's own `disableDamage` and `isCreativeMode` checks keep creative players excluded. Land it with or before CF2-MX3, and re-test the beacon after the planned beacon bridge.
- **Overlap:** partly the planned I-3 P0/P4 beacon `ProxyBlockEntity` bridge (`followups_recon.md:1558`), which fixes the beacon ticking but not this list; both are needed.
- **Repro (manual):** stand within 10 blocks of a BTW mill stone grinding netherrack. 4.B gives nausea; HEAD gives none. A verb that prints `WorldBridge.getOrCreate(level).playerEntities.size()` with a client connected prints 0 at HEAD.

### CF2-MX2. The "vulnerable player" lookup drops 4.B's sneak and invisibility ranges and its alive check (confirmed, low; not in the report)

- **Mechanism:**
  - 1.5.2 `World.getClosestVulnerablePlayer` (`vanilla/server/.../World.java:3629-3666`) does three things:
    - skips `disableDamage` and dead players (`:3637`);
    - scales the range by 0.8 when the player is sneaking (`:3641-3644`);
    - scales it by 0.7 x max(armour cover, 0.1) when the player is invisible (`:3646-3656`).
  - `WorldBridge.getClosestVulnerablePlayer` (`:2667-2677`) calls `level.getNearestPlayer(..., true)`, which applies none of these (1.20.1 `EntityGetter.java:74-89`).
  - So FC mobs detect a sneaking player at the full range: 16 blocks instead of 12.8 for zombies, and 64 instead of 51.2 for endermen. They detect an invisible, unarmoured player at 16 instead of about 1.1. A dead player still on the death screen can be returned; the new AI rejects it, but old-AI mobs and ghasts re-acquire it every tick.
- **At HEAD:** yes, in every release.
- **Fix direction:** loop over `level.players()` and skip spectators, creative or invulnerable players, and players where `!isAlive()`. Compare the distance with `range * sp.getVisibilityPercent(null)` (1.20.1 `LivingEntity.java:828-850`, which is 1.20.1's own form of the 1.5.2 formula). Keep `range < 0` as unlimited.
- **Repro (manual):** pen an `fc_zombie` and stand 14 blocks away. Sneaking on a fresh approach, 4.B ignores you and HEAD targets you. With invisibility and no armour at 8 blocks, 4.B ignores you and HEAD targets you.

### CF2-MX3. BTW squids can't keep a player as a target or head-crab one (confirmed, medium; not in the report)

- **Mechanism:**
  - The server player's FC view is `PlayerBridge`, then `btw.modern.EntityPlayerMP`, then the Modern-Common `EntityPlayer` stand-in (`EntityPlayer.java:3`). It lacks FC's `EntityPlayer` overrides of three Entity hooks (`vanilla/server/.../EntityPlayer.java:3227-3245`): `GetCanBeHeadCrabbed`, `IsValidOngoingAttackTargetForSquid` and `IsImmuneToHeadCrabDamage`.
  - The frozen Entity defaults (`Entity.java:2335-2349`, `:2369-2372`) are false for players.
  - So a squid that retaliates against a player drops the target on its next update (`FCEntitySquid.java:346-370`, `:550-559`), and squids never head-crab players (`:914`, `:942`, `:975`).
  - The same diff finds other missing `EntityPlayer` overrides outside this scope (boat riding, `RecoverAirSupply`, `dropHead`, mounting). They are not audited here.
- **Fix direction:** add the three overrides to Modern-Common `EntityPlayer` with FC's semantics:
  - `GetCanBeHeadCrabbed`: alive, not `disableDamage`, not riding or ridden;
  - `IsValidOngoingAttackTargetForSquid`: alive;
  - `IsImmuneToHeadCrabDamage`: `IsWearingSoulforgedHelm()`, which exists at `:617`.
  
  Check at runtime that a head-crab mount of a `PlayerBridge` is mirrored on the MC side. Pair it with CF2-MX1.
- **Repro:** at night in water, hit an `fc_squid` once. 4.B: it attacks with its tentacle. HEAD: it never attacks. A verb that prints these two hooks for the FakePlayer works without `level.players()`; it FAILs at HEAD and PASSes after the fix.

---

## 6. Beds and respawn

> "Unable to sleep in beds (not even a message shows up, only setting spawn works)"

### CF2-U1. Not sleeping is 4.B; the silence and the spawn point are port bugs (authentic + confirmed, high)

- **Three parts:**
  - (a) Not being able to sleep is authentic 4.B.
  - (b) The silent failure at night is a port bug.
  - (c) Setting the spawn is a port bug. It is plain 1.20.1 code, and the commenter saw it.
- **Mechanism:**
  1. **The port never routes a bed click to FC.** Beds map to FC id 26 (`ProxyRegistry.java:462-478`), but `BlockBehaviorMixin.btw$use` is a HEAD inject in `BlockBehaviour.use` (`:54`, `:215-216`). `BedBlock.use` overrides `use()` without calling super (1.20.1 `BedBlock.java:72-110`), so `FCBlockBed.onBlockActivated` never runs. `FCGameplayBridge.java:54-60` handles only the enchanting table.
  2. **1.20.1 sets the spawn before the time check.** `ServerPlayer.startSleepInBed` (`:840-884`) calls `setRespawnPosition(..., forced=false, message=true)` at `:853`, after the range and obstruction checks (`:846-851`) and **before** the time check (`:854`). Not every click sets it: only an in-range, unobstructed overworld click does, and `PlayerSleepInBedEvent` can veto at `:842`.
     - A day click therefore sets the spawn ("Respawn point set", `:1481-1483`, only when the position changed), then shows "You can sleep only at night" (`BedBlock.java:101-106`, `Player.java:2083`).
     - Nothing in the port subscribes to `PlayerSetSpawnEvent`, `SleepingTimeCheckEvent` or `PlayerSleepInBedEvent`.
  3. **At night it throws.** With no vanilla Monster in the ±8/±5/±8 box (`:857-867`), `:869` calls `super.startSleepInBed`. FC hostiles are `ProxyMob`, which extends `Mob` (`ProxyMob.java:24`), so they never count.
     - `PlayerMixin.btw$disableBeds` (`:106-111`) declares `CallbackInfoReturnable<Player.BedSleepingProblem>` and stores `OTHER_PROBLEM`, but `Player.startSleepInBed` returns `Either<BedSleepingProblem, Unit>` (`Player.java:1306-1310`).
     - Mixin 0.8.5's cancel path inserts `CHECKCAST Either` (`CallbackInjector.java:771-786`, `:783`; it applies because the return sort is ARRAY or wider). `setReturnValue` erases to `Object`, so javac adds no cast at the call site.
     - The resulting `ClassCastException` aborts `BedBlock.use` before `ifLeft` (`:101-106`), so no action-bar line appears. The "Respawn point set" line from `:853` has already gone out, which matches "only setting spawn works".
     - `ServerPacketListener.shouldPropagateHandlingExceptions()` is false (`ServerPacketListener.java:5-8`), so `PacketUtils.java:21-28` logs ERROR `Failed to handle packet ..., suppressing error` and carries on. (The prior hook audit's "rethrows to BlockableEventLoop" is wrong.)
     - In creative the monster check is skipped. With a vanilla Monster nearby, the player gets the "not_safe" line instead.
  4. **Respawn:** `PlayerList.respawn` resolves the bed (`:425-434`; `Player.java:1326-1345`). `ServerPlayerMixin`'s getters (`:269-317`) override that only when `m_HardcoreSpawnChunk` is not null, which never happens at HEAD (CF2-U1-HS).
- **Runtime:** the `ClassCastException` has never been seen at runtime. "BedSleepingProblem cannot be cast" appears only in plan text, not in any dev log.
- **4.B:** clicking a bed only prints the client chat line "You are too troubled to rest." (`Client/.../FCBlockBed.java:31-40`). There is no sleep, no night skip and no spawn point (`vanilla/server/.../EntityPlayer.java:1379-1383`; in 1.5.2 the spawn is set in `wakeUpPlayer`, `:1496`, `:1534`).
- **At HEAD:** yes, and byte-identical in every build since `64016ac` (`48e9825` `:48-53` through HEAD `:106-111`). There is no sleep line in `[Unreleased]`. `STUB_CATALOGUE.md:3579-3580` lists "beds and hardcore spawn" as open. `BRIDGE_SPEC.md:1652` wrongly calls the hook `@Overwrite`. `CURSEFORGE.md:14` ("Sleep costs you.") and `:44` ("Hardcore spawn, costly sleep") advertise mechanics that neither 4.B nor the port has as worded.
- **Fix direction:** maintainer ruling 3 (`followups_recon.md:18`): sleeping is allowed, and a bed never sets the spawn. The plan is R12/R13 in I-3 (wave C):
  1. Delete `PlayerMixin.btw$disableBeds` (`:102-111`) and its javadoc bullet (`:33`).
  2. Add a `BedSpawnPolicy` listener on `PlayerSetSpawnEvent` that vetoes bed spawns.
     - The FC beacon bind is **not** forced at the event: `FCBlockBeacon.java:93` passes false, and `PlayerBridge.setSpawnChunk` forwards it (`:560-589`). The beacon spawn is spared only because the block at the bound position is a beacon, not a bed.
     - The plan comments that call it "forced" (`followups_recon.md:172`, `:185`; `hooks_delta.md:22`, `:161`) should say so. The logic is still correct.
  3. R13: at the INVOKE of `Player.startSleepInBed` inside `ServerPlayer.startSleepInBed` (`:869`), return `Either.left(NOT_SAFE)` for a `ProxyMob` whose FC entity is an `EntityMob` in the box. This is the 1.5.2 rule (`vanilla/server/.../EntityPlayer.java:1410`).
  4. Keep `BedBlock`'s Nether/End explosion and its messages.
  5. Docs: `BRIDGE_SPEC.md:58`, `:1527`, `:1647-1652`, `:2030`; `INTEGRATIONS.md:128`, `:688`; the drafted CHANGELOG line (`followups_recon.md:1461`); `CURSEFORGE.md:14` and `:44` (not in the plan).
- **Overlap:** fully covered by I-3 (R12 at `followups_recon.md:159-197`, R13 at `:199-224`, file list `:582`, P-HOOK rows `:1183-1224`; `hooks_delta.md` B7 row at `:118`). Not covered: the `CURSEFORGE.md` wording and the hardcore spawn. `recon_summary.md:32` still says `BedRules` instead of `BedSpawnPolicy`.
- **Repro:** the `respawn` and `sleep` verbs exist (`TickProof.java:350-351`, `:3979`), so the P2 rows run at HEAD now. The `sleep` verb catches Throwable, so P2h reports the `ClassCastException` and the run continues.
  ```text
  btwproof respawn clear
  setblock 0 -60 8 minecraft:red_bed[facing=north,part=foot]
  setblock 0 -60 7 minecraft:red_bed[facing=north,part=head]
  btwproof usetok 0 -60 7 _ 1 east
  @check P2g-day-bed-click-sets-no-spawn (HEAD FAIL | after PASS) :: btwproof respawn expect none
  @check P2n-day-refused-no-spawn (HEAD FAIL | after PASS) :: btwproof sleep 0 -60 7 expect NOT_POSSIBLE_NOW
  time set 18000
  @check P2h-night-sleeps-without-spawn (HEAD FAIL: ClassCastException | after PASS) :: btwproof sleep 0 -60 7 expect slept
  ```
  Then the penned `fc_zombie` row P2m (expect `NOT_SAFE`), and the controls P2o (anchor) and P2a (a Nether bed explodes).
  
  Manual at HEAD:
  1. In survival, `/time set 18000`, with no vanilla monster within 8 blocks.
  2. Right-click a bed. Expect "Respawn point set" on the first click only, no action-bar text, and the player still standing. `latest.log` shows `Failed to handle packet ...ServerboundUseItemOnPacket..., suppressing error` with `class net.minecraft.world.entity.player.Player$BedSleepingProblem cannot be cast to class com.mojang.datafixers.util.Either`.
  3. `/kill`: you respawn beside the bed.

### CF2-U1-HS. 4.B's hardcore respawn never runs (confirmed, high; not in the report)

- **Mechanism:**
  - `FCUtilsHardcoreSpawn.HandleHardcoreSpawn` (`Server/.../FCUtilsHardcoreSpawn.java:107-188`) is called only from 1.5.2 `ServerConfigurationManager.respawnPlayer` (`vanilla/server/...:1184`). The port neither uses that method nor hooks `PlayerList.respawn` (no `HandleHardcoreSpawn` or `PlayerRespawnEvent` in `Forge/` at `48e9825` or HEAD).
  - `m_HardcoreSpawnChunk` has two writers:
    - the NBT load (`Modern-Common/.../EntityPlayer.java:1155-1160`), which only round-trips an existing value;
    - `PlayerBridge.setSpawnChunk` (`:560-589`), whose only FC caller is `FCBlockBeacon.onBlockActivated` (`:69-112`, bind at `:93`).
    
    That caller is unreachable: `BeaconBlock.use` overrides `use()` without super and opens the vanilla menu (1.20.1 `BeaconBlock.java:39-51`; `ProxyRegistry.java:745`; hup-43).
  - So every player has a null FC spawn, and respawns go to the bed or the world spawn.
- **Likely second gap:** even once hup-43 lands, the bound spawn is the beacon's own coordinates. `isRespawnForced` is hooked to true (`ServerPlayerMixin.java:308-317`), so 1.20.1 takes the forced branch (`Player.java:1340-1344`). That branch tests `isPossibleToRespawnInThis` on the beacon block, which is a full cube (`Block.java:362-363`). The result is an empty Optional and `NO_RESPAWN_BLOCK_AVAILABLE` (`PlayerList.java:466-467`). 1.5.2 validated it through `GetValidatedRespawnCoordinates` (`ServerConfigurationManager.java:1103-1140`), which was never ported.
- **4.B:**
  - Each death respawns the player at a random point 1000-2000 blocks from the original spawn (`FCUtilsHardcoreSpawn.java:26-27`, `:40-51`). The radius is x1.5 after the Nether, x2 after the Wither and x4 after the End (`:88-105`).
  - Before the Nether, players who die within a day of the latest spawn assignment respawn together (`:145-165`).
  - A repeat death within 24000 ticks is a soft respawn near the old point, with health 10 and reduced food (`:124-143`).
  - Leaving the End returns the player to the original spawn (`:111-116`).
  - A validated beacon binding overrides all of this. Beds play no part.
- **Effect after R12:** every player respawns at world spawn, because no player can bind a beacon at HEAD.
- **Fix direction:** a separate design; do not fold it into I-3.
  - Run `HandleHardcoreSpawn`'s logic through the bridges at the end of `PlayerList.respawn`, using the FC world's progress flags, and skip it when a respawn anchor applies.
  - Port `GetValidatedRespawnCoordinates` for FC spawns.
  - The beacon bind needs hup-43's beacon routing first (P4, `followups_recon.md:1558`).
  - Keep the 1.20.1 respawn anchor (`INTEGRATIONS.md:223`).
  - Fix `INTEGRATIONS.md:214`, which cites "FC's HC Spawn (integration `#002`)" as if it existed (#002 is the gamerules entry), and `CURSEFORGE.md:44`.
- **Overlap:** none of the planned follow-ups cover the random respawn.
- **Repro (manual, HEAD):**
  1. Note the world spawn and `/kill`. You respawn near spawn, not 1000-2000 blocks away.
  2. `/kill` again within a day: there is no soft-respawn penalty.
  3. Right-click a beacon: the vanilla GUI opens, and "You have been bound to this beacon" never appears.
  
  Proof idea: a respawn-position probe that calls `PlayerList.respawn` on a fake player and expects a distance of at least 1000 as the 4.B row. Also call `findRespawnPositionAndUseSpawnBlock(level, <beacon pos>, 0, true, false)` and expect an empty result at HEAD.

---

## 7. Progression remark: deepslate, deepslate ores, newer woods

> "Deepslate/deepslate ores/newer wood types still function as normal vanilla and whatnot, not a fault of the port or anything"

The commenter is right that this is a progression problem, and it is the port's to solve: "integrate, don't disable".

### CF2-PR-WOOD. The newer wood species have no BTW log rules (confirmed, medium; mostly planned)

- **Mechanism:**
  - `getBlockId` returns 0 for blocks with no 1.5.2 counterpart (`ProxyRegistry.java:234-241`). The species variants (`:163`, `:165`, `:407-410`, `:435-438`) and `LOG_FORM_SPECIES` (`:272-277`) cover only oak, spruce, birch and jungle.
  - So acacia, dark oak, mangrove, cherry, crimson, warped and bamboo are headless to FC (`ModernBlockStandIn.java:20-30`). Their harvest stays on 1.20.1's path. FC's log rules never apply: chewing by hand and bark (`Client/.../FCBlockLog.java:74-116`, `:112`).
  - FC removes log-to-4-planks only for species 0-3 (`FCRecipes.java:5418-5436`). `FcRecipeRemovals` can only match ingredients that have a legacy key (`:503-546`), so `acacia_planks.json` and its siblings stay.
- **4.B:** oak, spruce, birch, jungle and blood wood only. A log can't be taken whole by hand: it chews down and drops bark. Log-to-4-planks is removed. There is a work stump, saw planks with sawdust and bark, bark tanning and per-species fuel.
- **At HEAD:** yes for harvest and recipes. `[Unreleased]` gives the newer saplings BTW growth (`CHANGELOG.md:316-319`) and notes the newer leaves keep their drops (`:897-899`); neither touches log harvest.
- **Fix direction (planned):**
  - WOOD-A: acacia, dark oak, mangrove and cherry become FC wood types 5-8 at legacy id 165, with chew, bark 5-8, work stump, saw, chopping, fuel and tannin, and with the vanilla 4-planks recipe removed.
  - WOOD-B: the log rule. Class F covers wood and stripped forms; class S covers crimson and warped stems (barkless, no work stump) and modded `#minecraft:logs`.
  - Not addressed: FC sub-blocks and planks identity for the new species (FU-W1, `woodtypes_design.md:121-135`), and bamboo blocks and planks, which are excluded on purpose ("not a tree") and need a maintainer call if they matter for progression.
- **Overlap:** `followups_recon.md` R6 (`:110-116`), WS-WOOD-A (`:413-461`), WS-WOOD-B (`:462-507`), INTEGRATIONS queue #034 (`:1409`), the CHANGELOG draft (`:1457-1459`, `:1476`); `woodtypes_design.md`.
- **Repro:** the planned plan `compat/refresh2/wood_p1.txt` (every GAP row fails at HEAD):
  ```text
  btwproof hit 6 -59 6 north
  @check B1 GAP log not taken (HEAD 1) :: btwproof items 6 -59 6 2 expect minecraft:acacia_log == 0
  ```
  Also `btwproof hasrecipe minecraft:acacia_planks` must give 0 (HEAD 1), with the control `crimson_planks` staying 1. Manual: punch an acacia or cherry log in survival and you get the whole log; craft it into 4 planks.

### CF2-PR-DEEP. No deepslate identity and no stone strata (confirmed, medium; not planned)

- **Mechanism:**
  1. Deepslate, cobbled deepslate and the deepslate ores have no FC identity. There is no DEEPSLATE entry (only a comment at `ProxyRegistry.java:328`), so `getBlockId` returns 0. They dig and drop by 1.20.1 rules (`deepslate.json:36`, `deepslate_iron_ore.json:41`, `deepslate_gold_ore.json:41`). To FC they are just a solid stand-in (`ModernBlockStandIn.java:27`, `:107`).
  2. Plain stone (`ProxyRegistry.java:399`) and the stone ores (`:429-433`, `:572`) never get strata.
     - FC keeps strata in the metadata (`Client/.../FCBlockStone.java:273-281`).
     - 4.B writes it at generation: meta 2 at y ≤ 24 + rand(2), and meta 1 at y ≤ 48 + rand(2) (`vanilla/server/.../ChunkProviderGenerate.java:687-722`; ores in `WorldGenMinable.java:86-103`).
     - `WorldBridge.getBlockMetadata` (`:219-241`) falls back to `deriveVanillaMetadataUnchecked` (`:307-345`), which has no Y-based branch. Modern-Common keeps `HasStrata` false (`Block.java:1447-1450`).
     
     So every stone and ore block reads as strata 0 at any depth.
- **Related:** 1.20.1 charges 0.005 exhaustion per break (`Block.java:352-354`), while FC charges `AddHarvestBlockExhaustion` (`vanilla/server/.../Block.java:1150-1160`). That is deferred to the logs design's hunger decision.
- **4.B:** there is no deepslate. Overworld stone hardens with depth (`FCBlockStone.java:44-62`, `:203-227`):
  - the mid strata (y ≤ 48) has hardness 3.0 and needs a better tool to mine efficiently;
  - the deep strata (y ≤ 24) has hardness 4.5 and needs tool level 3 to harvest.
  
  Ores carry the same strata (`FCBlockOre.java:34-37`, `:101-110`).
- **At HEAD:** yes. `[Unreleased]` changed only BTW tool drops, wear and the mining penalty on deepslate (`CHANGELOG.md:366-372`, `:408-413`). In 1.3.0-alpha.3 a BTW pickaxe on deepslate diamond ore dropped nothing (`:369-370`). `STUB_CATALOGUE.md:1130` records STRATA as not done, and `INTEGRATIONS.md:815-819`, `:882` and `:1053` are directions only.
- **Fix direction (needs a maintainer design):**
  - (a) Re-anchor 4.B strata to 1.20.1 heights. Derive stone and ore meta from Y in `deriveVanillaMetadataUnchecked`, by a Y band or by per-chunk SavedData noise that matches 4.B's `+rand(2)` edge.
  - (b) Give deepslate and cobbled deepslate FC identity as deep-strata stone, and the deepslate ores their FC ore with meta 2 (the `INTEGRATIONS.md:815`, `:882` direction). This also closes most of CF2-PR-ORE.
  
  Either changes mining balance in existing worlds.
- **Repro:**
  ```text
  setblock 0 10 0 minecraft:stone
  setblock 2 60 0 minecraft:stone
  btwproof dig 0 10 0 minecraft:iron_pickaxe
  btwproof dig 2 60 0 minecraft:iron_pickaxe
  # HEAD: identical progress; 4.B strata would give y=10 about half the progress of y=60
  setblock 4 -60 0 minecraft:deepslate
  btwproof break 4 -60 0 minecraft:wooden_pickaxe 1 survival minecraft:cobbled_deepslate
  # HEAD PASS: plain 1.20.1 rule
  ```

### CF2-PR-ORE. Raw iron and gold smelt straight into ingots (likely, high)

- **Mechanism:**
  - Cooking pass 3 (`FcRecipeRemovals.java:418-445`) removes a `minecraft:` cooking recipe only when an ingredient's legacy key is in `removedFurnaceInputs`: the 1.5.2 furnace table minus FC's live list (`:56-66`).
  - `raw_iron`, `raw_gold`, `raw_iron_block` and the deepslate ores have no legacy key (`legacyKey` returns -1, `:503-546`). Pass 4 (`:461-481`) keys on `RemovedContent` results, and the ingots are not among them.
  - So `iron_ingot_from_smelting/blasting_raw_iron`, the `_deepslate_iron_ore` variants and their gold equivalents stay. Only `*_iron_ore` and `*_gold_ore` go, matching FC's removals (`Client/.../FCRecipes.java:5764-5765`).
  - A village furnace (the recipe is removed, but generated copies stay) or a blast furnace (craftable with the brick oven, `RemovedContent.java:258-263`) smelts them. The brick oven is not this path (`RecipeBridge.java:60`, `:158`).
  - 1.20.1 iron ore veins at y -60 to -8 also place `raw_iron_block` (`OreVeinifier.java:59`). It crafts into 9 raw iron (`recipes/raw_iron.json`), and each raw iron smelts to one ingot.
- **4.B:** iron ore drops an iron ore chunk, which smelts into one iron nugget, and gold works the same way (`FCRecipes.java:3031`, `:3033`; chunks 22315 and 22318, `FCBetterThanWolves.java:1701`, `:1709`). This is a key early-game bottleneck. 4.B has no raw-iron blocks or ore veins.
- **At HEAD:** yes. Nothing maps raw ore or deepslate ores, and no port code touches the vanilla furnace or blast furnace block entities.
- **Fix direction (the maintainer picks):**
  - (a) Fold it into CF2-PR-DEEP option (b), so FC's harvest of deepslate ores drops chunks. Veins stay open.
  - (b) Generalise pass 3: remove any `minecraft:` cooking recipe whose ingredient maps to an FC id that FC's live list cooks into a different result. Mapping `raw_iron` onto the chunk identity does not by itself remove the recipe, because the chunk ids are FC additions.
  - (c) A ruling on `raw_iron_block` from veins: map it, remove its 9-raw recipe, or treat veins as intended 1.20.1 bounty.
  
  Check Fortune and Silk Touch, and keep JEI consistent.
- **Overlap:** none planned; it belongs with the CF2-PR-DEEP decision.
- **Repro:**
  ```text
  @check ORE-0 CONTROL stone-ore smelt removed (HEAD PASS) :: btwproof recipeis minecraft:iron_ingot_from_smelting_iron_ore none
  @check ORE-1 GAP raw iron smelts (HEAD FAIL) :: btwproof recipeis minecraft:iron_ingot_from_smelting_raw_iron none
  @check ORE-2 GAP raw iron blasts (HEAD FAIL) :: btwproof recipeis minecraft:iron_ingot_from_blasting_raw_iron none
  @check ORE-3 GAP deepslate iron ore smelts (HEAD FAIL) :: btwproof recipeis minecraft:iron_ingot_from_smelting_deepslate_iron_ore none
  @check ORE-4 GAP raw iron block unpacks (HEAD FAIL) :: btwproof hasrecipe minecraft:raw_iron
  setblock 0 -60 2 minecraft:deepslate_iron_ore
  setblock 2 -60 2 minecraft:iron_ore
  btwproof break 0 -60 2 minecraft:stone_pickaxe 1 survival minecraft:raw_iron
  btwproof break 2 -60 2 minecraft:stone_pickaxe 1 survival betterthanwolves:item_22315
  ```
  ORE-4 expects 0. Add the same rows for gold. The first break PASSes at HEAD, which is the gap; the second is the control (the FC chunk).

---

## Corrections to the record

These are recorded here only; no doc was changed. *(2026-10-21: several have been acted on since, and the rest are still open; see [Corrections to the record: status](#corrections-to-the-record-status). 2026-10-28: round 3 closed 2 and 8, the flower half of 9 and part of 1; the status table adds corrections 15 to 21. 2026-11-03: it adds correction 22, closed by W10's solid-top fix.)*

1. **R6 premise.** "BTW never spawned villager zombies" is wrong for villages (`ComponentVillage.java:178-187`): `CHANGELOG.md:575-576`, `STUB_CATALOGUE.md:2307-2309`, `:2340-2341` and the comment at `VanillaMobReplacer.java:462-466` (CF2-MU3).
2. **Falling-block rendering is listed as done** (`STUB_CATALOGUE.md:423-424`; caveat at `:2023-2024`), and `[SELF-AUDIT/renderer]` passes falsely (CF2-R1).
3. **"Placing against any other block uses BTW's code"** (`CHANGELOG.md:827-832`) implies BTW's placement checks run, but `canPlaceEntityOnSide` is a stub. The ported-steps list in `STUB_CATALOGUE.md:3090-3093` omits that (CF2-P8a).
4. **`BRIDGE_SPEC.md:1652`** calls the bed hook an `@Overwrite`. It is a HEAD inject that throws a `ClassCastException` (CF2-U1).
5. **`CURSEFORGE.md:14` ("Sleep costs you.") and `:44` ("Hardcore spawn, costly sleep")** advertise mechanics that neither 4.B nor the port has as worded (CF2-U1, CF2-U1-HS).
6. **`INTEGRATIONS.md:214` (#007)** cites "FC's HC Spawn (integration `#002`)" as if it existed; #002 is the gamerules entry (CF2-U1-HS).
7. **The follow-up plan's "FC beacon (forced)" comments** (`followups_recon.md:172`, `:185`; `hooks_delta.md:22`, `:161`): the beacon bind passes `forced=false`. `recon_summary.md:32` still says `BedRules`.
8. **The queued "upward chew-eject"** (`followups_recon.md` section 9) names only chewing, but the same line governs every FC conversion (CF2-DR2).
9. **The collapse bullets** (`CHANGELOG.md:868-876`, `STUB_CATALOGUE.md:2419-2422`) frame the collapse as an item-storage issue. They miss bed and door duplication, flower drops and flower placement (CF2-DR1, CF2-DR3, CF2-P8c).
10. **`STUB_CATALOGUE.md:1642-1643`** records the nested re-sync as a "Pre-existing" caveat. It is the root of a survival duplication bug (CF2-I1).
11. **The STUB_CATALOGUE P0 override-point table (`:845-863`)** omits `DropItemsOnDestroyedByExplosion`, which had base logic in 1.5.2 (CF2-DR5).
12. **The prior hook audit** says a handler exception "rethrows to BlockableEventLoop". `PacketUtils` suppresses and logs it (CF2-U1).
13. **The CF2-P10 investigator wrote "None at 1.4.0-alpha.5 and later".** That is true only of its mechanism; the symptom continues through CF2-I1.

## Mapping to planned work and proposed follow-ups (by player impact)

"Planned" means in `followups_recon.md` or the queued C1 leftovers; none of it is in code yet. "New" is a proposal from this triage.

*(2026-10-21: ranks 1, 2, 3 and 5 and the woods of rank 8 have landed since, rank 2 by the maintainer's ruling that beds and doors take 1.20.1 drops. 2026-10-28: every other rank has landed in round 3, rank 13 by the maintainer's ruling that BTW mobs never target creative or spectator players; see [Fix status](#fix-status-2026-11-03-uncommitted-working-tree).)*

| Rank | Finding(s) | Player impact | Home | Notes |
|---|---|---|---|---|
| 1 | **CF2-I1**, with CF2-I2, CF2-I3, CF2-I5; guard CF2-I6 | Survival duplication of most placed blocks; phantom items; items back after death | **New: INV-RESYNC** (non-destructive `InventoryBridge.syncSlot`, plus a main-hand resend after Stage 2) | Must land before or with **I-1** (wave B), or I-1 extends the loss to stakes and other FC items. Fix 4 can live in I-2's REFUSED resync, extended to PLACED. Keep the CF2-P10 regression row. The joined-client G1-G5 plan is the proof. |
| 2 | **CF2-DR1** beds, **CF2-DR6** doors | Unlimited bed and door duplication; iron doors turn into iron and gold | Planned **D7** (drop parity) plus **P4** (door codec), plus **new: a bed codec** and the **collapsed-identity rule** | D7 alone does nothing, or makes beds worse. Either the codec and the 1.5.2 drop rules, or a maintainer ruling to make bed and door drops VANILLA (skip the FC harvest). Add the BED-R1 regression row. |
| 3 | **CF2-U1** | Bed click sets the spawn and fails silently at night (logged ERROR) | Planned **I-3** R12 (`BedSpawnPolicy`), R13 (wave C) | Already ruled (ruling 3). Add the `CURSEFORGE.md:14`/`:44` wording and the `BRIDGE_SPEC` fix to the I-3 doc step. |
| 4 | **CF2-R1** | Every falling block is an orange box; mid-fall saves lose blocks | **New: PROXY-CTOR** (`ensureFcEntity` constructor rule, failure cache, constructability self-audit) | Small and self-contained. Then `ShouldRenderWhileFalling` and movement smoothing. |
| 5 | **CF2-P8a** (and CF2-P8b) | Plants, torches, rails and hooks are placeable anywhere, even into solid blocks | Planned **I-2** design A/A2/G | Add the P8 rows to P-PLACE. CF2-P8b needs nothing separate. |
| 6 | **CF2-DR2** | Drops eject through rock or into the block above | Planned "upward chew-eject" (section 9), widened to **all FC conversions**, plus **new:** the item `yOffset` fix | Needs a TickProof `hit` that sends the face. |
| 7 | **CF2-MU3**, **CF2-U1-HS** | 4.B's progression gate (zombified villages and the soul-urn cure) and its death mechanic (hardcore respawn) are missing | **New designs**, both needing maintainer rulings | Prove the soul-urn cure end to end first. U1-HS also needs hup-43 (P4 beacon bridge) and `GetValidatedRespawnCoordinates`. |
| 8 | **CF2-PR-ORE**, **CF2-PR-DEEP**, **CF2-PR-WOOD** | Progression shortcuts: raw ore to ingot, no strata, whole new-species logs and a 4-planks recipe | WOOD: planned **WOOD-A**/**WOOD-B**. ORE and DEEP: **new**, need a ruling | ORE (b) needs a generalised cooking pass 3. DEEP (b) closes most of ORE. Bamboo needs a call. |
| 9 | **CF2-DR5**, **CF2-DR4** | Mining charges yield nothing; blasts use vanilla loot and skip FC reactions | Planned **P5** explosions, **D1(c)**, the `ProxyBlock.getDrops` chance; **new:** port the base `DropItemsOnDestroyedByExplosion` and the dirt-loosening stubs | DR5 is a small Modern-Common change with high value for mining. |
| 10 | **CF2-DR3**, **CF2-P8c** | Eight flowers become poppies on break, pop or placement | **New: the collapsed-identity rule** (shared with DR1), plus the 38:1-8 variant table (ruling) | Same rule covers wet sponge and carved pumpkin. |
| 11 | **CF2-MX1**, **CF2-MX3**, **CF2-MX2** | Squids never hunt players; mill-stone nausea and beacon effects never reach players; sneaking and invisibility don't shorten detection | **New** (WorldBridge live `playerEntities`; three `EntityPlayer` overrides; a visibility-aware lookup) | MX1 next to the planned beacon bridge (P4). Land MX1 with MX3. |
| 12 | **CF2-R3**, **CF2-R5b** | Smeared crack on webs and ladders; campfire smoke around loose snow and BTW machines | **New** (geometry normals in `FCBakedModel`; one shared FC-particle table mapped to CLOUD) | Cosmetic and cheap. R5b is a one-line change plus the table merge. |
| 13 | **CF2-M7** | Mobs keep chasing after a switch to creative | Docs only (CURSEFORGE and CHANGELOG known differences); the optional game-mode-change handler needs a ruling | Authentic 4.B. |
| - | CF2-I4, CF2-P10 | - | none | Ruled out / fixed-since. |

## Questions for the commenter

- Which jar did you run (the filename, or the version in the mod list)? The clues point to 1.4.1-alpha.5 or later, which the CHANGELOG does not list as a release.
- When placed blocks came back: were you sneaking when you placed them, and did you place them on dirt, grass, planks or other BTW-handled blocks? Was `keepInventory` on?
- Were the slabs the flowers broke on stone or wooden slabs, or BTW's loose slabs (gravel, sand, dirt, snow)? Were several flowers placed next to each other?
- Were the zombified villagers in a village where you saw zombies at night? Did any of them have villager heads?
- Was the "smoke" around loose snow grey and lingering, or faint white wisps?
