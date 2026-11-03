# Changelog

Notable changes to the Better Than Wolves Forge 1.20.1 port. Versions are the port's own SemVer
(see `VERSIONING.md`); every release runs BTW **4.B0000003** (FlowerChild's 1.5.2 code). The port's
rule is that this code is read-only: the port never edits FlowerChild's code and makes its changes
around it.

> **Reconstructed 2026-10-10.** There were no tags and no changelog before this file. The history
> below is rebuilt from the CurseForge files list and the git history. Most uploads were built from
> uncommitted working trees, so for every release before 1.3.0-alpha.3 the git state named is the
> closest one, not an exact match. All five CurseForge files are marked "Release"; they are alphas.
> From now on, follow the release checklist in `VERSIONING.md` (changelog entry, `git tag`,
> CurseForge type "Alpha").

## [Unreleased]

Not released yet. These fixes were made on 2026-10-10, 2026-10-11, 2026-10-14, 2026-10-15, 2026-10-17
and 2026-10-18, in ten implementation waves on top of 1.3.0-alpha.3 (`48e9825`), plus a fix for a player
report. The five waves of 2026-10-10 address the CurseForge feedback triage and an earlier design pass;
they were committed as `f6ec9be`, and `d55db5d` set the version to 1.4.0-alpha.5. The 2026-10-11 wave
fixes bugs the maintainer found in mechanical power, sounds and the BTW creative tab, after an audit
that proved each root cause against BTW's 1.5.2 code before anything was changed. The 2026-10-14 wave
makes BTW's own rules decide how blocks that keep a Minecraft id grow, spread, decay, burn and fall; its
root cause was proven with a scripted in-world test on a dedicated server before anything was changed,
and the result was checked the same way afterwards. The first 2026-10-15 wave carries out three rulings
by the maintainer: bone meal works as in 4.B, plants added after 1.5.2 follow the 4.B rules of their
nearest BTW relative, and blocks that 4.B removed stay removed. It was checked the same way, with
scripted in-world tests before and after the change. `docs/curseforge-feedback-triage.md` has a status
table for every triage finding; the ids in parentheses refer to it, to the design pass and to the
2026-10-11 audit (`STUB_CATALOGUE.md` entries (an) to (at)). The 2026-10-10 and 2026-10-11 waves build
and pass the link and bridge audits, the 2026-10-14 wave builds and passes the link and bridge audits,
the first 2026-10-15 wave builds and passes the link audit, and a dedicated server boots a fresh world
without errors. Most of the client-side fixes have not been checked in game yet (see Known issues).

Two more waves bring the rest of Minecraft 1.20.1, and six optional mods, under BTW's rules. The second
2026-10-15 wave (`3985147`) covers hunger and food, mining, crafting, the food bar's place in the HUD
and plants from other mods. The wave of 2026-10-17 and 2026-10-18 (committed together with the
hold-to-craft fix and merged as `0292e38`) adds Dynamic Trees, Serene Seasons, AppleSkin, Cold Sweat,
Legendary Survival Overhaul (LSO) and Pam's HarvestCraft 2. Both were proven as the waves before them,
at a larger scale. Their gaps were first recorded by scripted in-world tests on a dedicated server
running the old code, and the same tests ran on the new code, with each mod alone and with all of them
together. Development clients joined those servers for what a server cannot show: the food bar's place
in the HUD, AppleSkin's tooltips and previews, Cold Sweat's gauges and LSO's frost tint. The bugs that
code review and these runs found were fixed and the runs repeated; in the final runs of 2026-10-18 every
check passes except the two that record the dropped white dye issue (fixed on 2026-10-20, see Fixed).
Last, the release jar booted a fresh world on a clean Forge 1.20.1-47.3.0 server, once alone and once
with the release jars of all six mods and the Dynamic Trees and Serene Seasons add-ons for HarvestCraft:
no crash, no mixin error and no error from BTW (the only errors came from HarvestCraft's and Dynamic
Trees' own recipe data). That boot also confirmed a fix from the review: the Dynamic Trees integration
now starts from release jars, where a method it looked up by its development name does not exist.
`INTEGRATIONS.md` describes these parts in entries #020 to #032; the numbers #020 to #032 in parentheses
below refer to them.

The work of 2026-10-19 to 2026-10-21 finishes the follow-ups of those waves and fixes the bugs that a
CurseForge comment of 2026-10-19 reported. `docs/curseforge-feedback-triage-2026-10-19.md` triages that
comment and gives the status of each finding; the CF2- ids in parentheses below refer to it. Rulings by
the maintainer on 2026-10-19 and 2026-10-20 decided the newer woods, the Nether stems, beds, bed and
door drops and the 1.20.1 dyes (see Changed), and kept 1.20.1's redstone for doors, trapdoors and fence
gates. It was proven as the waves before it: every gap was first recorded by a scripted in-world test on
the old code, and the same test ran on the new code. A development client joined the dedicated server
for what a server cannot show: the item count after placing a block, the hotbar, sleeping through the
night and the newer woods' textures. Each stage builds and passes the link and bridge audits, and on
2026-10-21 the regression tests of the earlier waves ran again on the final code with the same results
(the random growth tests within their expected spread). No release jar was booted for this work, and it
is not committed. `INTEGRATIONS.md` entries #033 to #040 describe it.

The work of 2026-10-21 to 2026-11-03 fixes the rest of that comment's findings and the bugs found with
them. Rulings by the maintainer from 2026-10-21 to 2026-10-25 decided zombified villages without golems
or sieges, 4.B's hardcore respawn (except in worlds created as creative), the beacon, the stone strata
with deepslate as deep stone and raw ore as ore chunks, explosion drops, creative players, bamboo
planks, the exact 4.B breaking crack, falling blocks saved in mid-air, and the torch, redstone, minecart
and portal fixes. Rulings of 2026-10-31 and 2026-11-02 put snow, pressure plates and rails that
Minecraft places under BTW's support rules too, and made Minecraft's newer pressure plates, fences,
walls and stained glass panes follow their BTW relatives' support rules. It was proven as the work
before it: every gap was first recorded by a scripted in-world test on the old code, and the same test
ran on the new code. Development clients joined for the breaking crack, BTW's particles, torch and
ladder placement, falling blocks, drops from a dug block, creative players and squids, the depth dig
speed, the newer woods' blocks, the beacon, the respawn, the restored textures, a player on the
soulforged steel plate and detector rail, and torches and plates placed on the newer fences, walls and
stained glass panes. The work landed in ten stages. After each of the first nine, the build passed the
link and bridge audits, and the regression tests of the earlier waves gave the same results apart from
the intended changes (from the fifth stage on, only the tests each change could reach were rerun). The
last stage (the texture, redstone, minecart and portal fixes) first failed on one rule: a redstone
torch could still stand on top of glowstone or ice. Its fix (BTW 4.B's own solid top, see Changed) and
the support rules of 2026-10-31 and 2026-11-02 joined that stage from 2026-10-31 to 2026-11-03. On
2026-11-03 it builds and passes the link and bridge audits, and all of its tests pass: that day the
redstone torch test passed its 24 checks, the torch and ladder placement test its 117, the new support
tests their 106, 100 and 38 and their client test its 17, and the regression tests these changes could
reach gave the same results; the stage's other tests, which these changes do not reach, passed on
2026-10-28. A final run on 2026-11-03 of every round-3 test and every regression test, 74 server
runs and 22 development-client runs on the finished build, matched each test's latest record; the plant
growth tests, which count random growth, stayed within their expected ranges. No release jar was
booted for this work, and it is not committed. `INTEGRATIONS.md` entries #041 to #062 describe it.

### Fixed

Reported on CurseForge:

- **Ghast noises in the Overworld.** Every bat that took off played the ghast's loud warning sound
  ("Ghast shoots"), which carries 64 blocks. The cause: 1.5.2 sound-event ids were passed to 1.20.1
  unchanged, and the bat-takeoff id means "ghast warning" in 1.20.1. All 1.5.2 sound and particle
  event ids are now translated. This also removes a ghast fireball sound when a zombie infected a
  villager or a zombie villager was cured, and a firework sound when a torch fizzled out (GH1, GH3).
  Witches now idle with the quiet ghast scream BTW gives them instead of ghast moans, and they spawn
  naturally only in swamps, as in BTW (GH2).
- **Ink sacs and arcane scrolls in lakes.** BTW squids drowned on the Minecraft side about 25 s after
  entering water, and dropped their ink. BTW mobs now breathe only by BTW's own rules (D1). Arcane
  scrolls were rolled 20 times a second for living glow squids, drowned, husks, zombie villagers and
  wither skeletons. Scrolls now drop once, on death, with BTW's odds (D2), and those mobs are now
  replaced by BTW mobs anyway (see Changed).
- **Skeletons without bows.** Mobs that replace vanilla spawns now go through BTW's own spawn setup,
  so skeletons carry bows (spawner skeletons still have none, as in BTW), zombies can wear armour and
  sheep get BTW's wool colours. Mobs converted from an existing world keep their name, age, owner,
  collar, profession, equipment and health (SW1, HAI-6). Spawners count BTW mobs toward their limit,
  as in 1.5.2, so a dungeon spawner stops at 6 nearby mobs.
- **Zombies walking on water.** Water and lava were solid ground to BTW mobs. They now sink and swim
  (SW3). The same fix lets BTW mobs pass through tall grass, flowers, crops, fire, cobwebs, sugar
  cane, vines, redstone dust, levers, buttons, pressure plates, signs, tripwire and portals. Ladders,
  chests, slabs, fence gates, rails, repeaters, and BTW's own slabs, sidings and mouldings get their
  1.5.2 shapes. Plants and cobwebs slow BTW mobs, and cactus hurts them (SW-M1, partly; see Known
  issues).
- **Mobs stuck red, red textures, red XP, vanilla mobs "in a dying state".** Once a red "dragon" XP
  orb had been drawn, every BTW mob and many BTW models stayed dark red, and other colours leaked the
  same way. The renderer now resets its colour for every model, as 1.5.2 does (R1, D3). Dye colours
  now show too: sheep wool, wolf collars, dyed windmill sails, leather armour (R6). Player kills,
  one-hit kills included, drop normal green XP instead of a red orb that could not be picked up (R2).
  Mobs flash red when hit and tip over when they die, and creepers flash white before exploding, as
  in 1.5.2 (R4).
- **Mobs dying.** Besides drowning (above), damage BTW deals by itself (falls, fire, attacks by BTW
  mobs) now lowers the mob's real health and ends in a normal death (D6). The false suffocation
  damage left in 1.3.0-alpha.3 is now undone exactly, and its hurt sound no longer plays (R5). BTW
  mobs no longer fall through deepslate, tuff, granite and other blocks added after 1.5.2 (D4).
  Squids in seagrass and kelp count as being in water (D5). Wounded mobs no longer come back at full
  health after a reload (RE7). Blazes, ghasts, pigmen, magma cubes and wither skeletons no longer
  take damage in lava and fire (D8), and hitting a BTW mob no longer plays the player's "oof" (D-M1).
- **Hostile mobs not attacking.** After a player's first death, or after leaving the End, every BTW
  mob kept targeting the dead player until relog, and BTW's health penalty stayed at its worst. Mobs
  now attack after a respawn, and the respawned player starts with BTW's fresh state (full food, no
  penalties), as in 4.B (HAI-1). BTW mobs below y=0 (deepslate caves, superflat worlds) stood still
  instead of chasing the player. BTW pathfinding now covers the full 1.20.1 world height (HAI-3).
  Mobs near hay bales, terracotta, carpet or coal blocks could not find paths (HAI-4). Witches froze
  after drinking their first potion (HAI-5).
- **Creepers not exploding.** The AI scheduler cleared a creeper's target just before its fuse could
  start, so creepers stopped about 3 blocks away and never hissed. AI tasks now run in 1.5.2's order
  (HAI-2). Creepers visibly swell and flash, and a creeper kicked by a BTW cow becomes determined to
  explode, as in 1.5.2. Creeper explosions respect `mobGriefing` and are credited to the creeper
  (HAI-7).
- **Can't re-enter a world.** No BTW cause has been found. As hardening, BTW's world, player and
  block-entity save, load and unload code can no longer throw out of the save or shutdown path, and
  BTW's server-side state is released when a world closes (RE1, RE-M2, RE3). If you still cannot
  re-enter a world, please report it with `logs/latest.log` (see Known issues).
- **Buggy animations.** The mob renderer is now a full port of 1.5.2's:
  - chicken wings, wolf tails and squid tentacles no longer spin;
  - mobs swing their arms and show held items and armour;
  - sheep have wool and lower their heads to graze;
  - slimes have their sizes, and baby mobs are small;
  - endermen have eyes and carried blocks, and zombie villagers have the villager head;
  - withers, wither skulls and magma cubes are drawn at all;
  - squids are no longer turned twice (R7, SW2, SW6).

  Clients now animate BTW mobs every tick instead of replaying two-tick-old snapshots, so legs no
  longer stutter and wolves shake dry (R8; a few animations are still missing, see Known issues).
  Withers have a boss bar and turn their side heads.
- **Little in-game information.** BTW 4.B teaches almost nothing in game, by design, but the port
  made it worse. JEI now covers the soulforge, the campfire, BTW's special crafting recipes and the
  processes that have no recipe list, and BTW's join messages are back (see Added). The CurseForge
  page no longer promises a "field manual". The wiki link is still to be added (CTX-3 to CTX-6).

Reported by a player (2026-10-18):

- **Knitting, bone carving and wicker weaving never finished.** Holding right-click on knitting, a
  carving bone or wicker weaving made no progress, so wool knit, the bone fish hook (and with it the
  4.B fishing rod) and wicker could not be made. BTW's per-tick hold-to-craft step
  (`FCItemCraftingProgressive.UpdateUsingItem`) was never called, and BTW read a copy of the use timer
  that never moved. Both are bridged now: the progress bar fills and the item finishes on 4.B's timing
  (2 minutes for knitting and carving, 1 minute for wicker, after 4.B's short warm-up). Letting go
  keeps the progress. BTW's crafting sounds and particles run through the same step again (checked by
  code review only). Eating, drinking and bows are unchanged. Checked in game with a real client
  holding right-click on knitting and a carving bone, and on a dedicated server for all three.

Found by the maintainer (2026-10-11):

Each of these was traced to a shared part of the port that did not do what BTW's own 1.5.2 code
does, and was fixed there rather than block by block.

- **Severe server lag, and blocks breaking many seconds late.** After a few minutes a world with
  underground water fell further and further behind (up to 28 seconds), and logs and other blocks
  that take time to mine broke 10-30 seconds after you finished mining them. The port turns glow
  squids into BTW squids, but counted them against the ordinary squid limit, so the game never saw
  the underground-water limit as reached and kept spawning them: a fresh world held thousands of BTW
  squids in cave water within ten minutes. Mobs that replace a vanilla mob now count against the
  spawn limit of the mob they replaced. The slow block breaking was a symptom of the lag (Minecraft
  measures mining progress in server ticks). Worlds where squids have already piled up are not
  cleaned up; `/kill @e[type=betterthanwolves:fc_squid]` clears them.
- **Clutches and gear boxes ignored their facing.** A clutch or gear box always showed its input face
  on the south side, whichever way it was placed, and never showed its output faces, so axles got
  attached to the wrong side and the block seemed not to connect. The fronts of the bellows, saw,
  screw pump, block dispenser, buddy block, detector block, companion cube and mining charge never
  turned either. BTW blocks are now drawn with BTW's own per-position textures and 1.5.2's face
  rules, so every block shows the face it really has (MPR-1, F1). The power logic itself was already
  right.
- **Axle textures.** Axles running east-west or up-down had their end and side textures swapped, axle
  sides were textured the wrong way round, and no axle ever looked powered. Axles now look as in 4.B
  on every axis, powered axles and axle power sources show their moving texture, and axles a few
  blocks from a gear box switch their look across chunk-section borders too (MPR-2, MPR-6). The saw
  blade and the turntable's switch faces are drawn the right way round.
- **Animated textures.** A grinding millstone, a running saw blade, a powered axle and the water in a
  bucket showed their whole animation strip squashed into one face. They animate again (MPR-4).
- **Block items.** BTW block items looked like the placed block's first variant: the axle item was an
  upright post, the saw item faced down, every siding, corner, moulding and wool slab colour looked
  the same, and torches and crops were 3D blocks. Items now use BTW's own item drawing for each
  variant: a horizontal axle, a saw with an upright blade, stair-shaped stairs, the gear box showing
  its input, flat torches and crops. Held BTW block items face as in 1.5.2 (MPR-3).
- **Placing blocks.** Placed against a non-BTW block, BTW stairs always faced the same way, and the
  anvil, wicker basket, hamper and piston shovel ignored which way you were facing; placed against a
  BTW block they turned correctly. BTW blocks now go through BTW's block placement steps whatever you
  click, so they face the way you place them, variant items place their own variant and a lens sets
  up its beam (MPR-5). Clicking ash replaces it, as in 1.5.2.
- **Turntables.** Saws, bellows, block dispensers, screw pumps, buddy blocks, lenses and other blocks
  never turned on a turntable, and gear boxes, clutches and stairs turned the opposite way from
  pumpkins, repeaters and pistons. They now turn, all in 1.5.2's direction. Right-clicking an
  unconnected clutch or gear box cycles its facing in 1.5.2's order, clay shaped on a turntable keeps
  its clay, and the tops of anvils, baskets and hampers turn with them (F2).
- **Redstone power.** The redstone clutch, pulley, block dispenser and turntable reacted to weak
  redstone power that 4.B ignores: a clutch next to a redstone block, or next to a block powered by a
  lever or by dust, stayed disengaged. They now react only to strong power, as in 4.B: a redstone
  torch below, a repeater or dust pointing into the block, or a lever or button on it (F4).
- **Buddy blocks** did not react when a lever next to them was flipped, a vanilla block next to them
  was mined or a block added after 1.5.2 changed: BTW blocks were told "no block" for any neighbour
  that was not a BTW block. They now get the neighbour's id, as in 1.5.2. BTW blocks that alert their
  neighbours on purpose (the gear box, clutch and saw when they change facing, the BTW chest, stake
  strings, soul urns) now actually reach them (F3).
- **Bellows.** A player standing on a powered bellows sank into it when the power went off; the
  bellows lifts the player again, as in 1.5.2.
- **Sounds.** BTW sounds now play when, where and for whom 1.5.2 played them: every BTW sound name
  plays its own 1.5.2 sound file, sounds 1.5.2 played only on the client play there again, and the
  player who caused a sound hears it once (S1 to S11).
  - Back after being silent: the millstone grinding and starting up, the gear box clicking and
    smoking when powered, the bellows, the powered axle's creak, the turntable click, campfire and
    smouldering-log crackle, the wet cement's moan, stoked fire; zombie pigmen, enderman idle, the
    wither's idle and hurt sounds, ghast deaths, wolves shaking dry, ocelots, iron golems throwing,
    zombie curing, the dragon's wings, firework launches, the soulforged helm's breathing; BTW's
    crafting sounds for tools, armour, shears, cured food and other items, in the inventory grid, at
    the BTW workbench and at the soulforge; BTW mobs splashing into water; the burp after eating;
    the gloom ambience (cave sounds, growls, the enderman stare); grunting when walking exhausted or
    badly hurt; soulforged boot steps; spider webs and thrown soul sand.
  - Fixed after playing the wrong sound: the saw and millstone used a minecart loop, a hurt creeper
    hissed its fuse, the bellows mooed at random, chests, doors, anvils, the player's "oof" and the
    arrow-hit ding used 1.20.1's replacements, and BTW's own effects (hopper closing, redstone
    clicks, XP, wolves eating, melons and pumpkins bursting, lightning) had the wrong sound, volume
    or pitch and no particles.
  - Squishy BTW blocks (unfired pottery and bricks, rotten flesh, nether growth, BTW webs) squish,
    and glassy ones (crucible, vase, planters, light bulb) sound like glass, instead of stone. BTW
    mobs walking on blocks added after 1.5.2 use that block's footstep sound.
  - Other players now hear and see you break blocks. You no longer hear a block you place twice, and
    placing plays the place sound at 1.5.2's volume and pitch. Vanilla foods that BTW feeds you end
    with one burp and no extra eating sound.
- **BTW creative tab.** The tab listed every BTW block and item, including technical ones 4.B never
  showed: burning and lit blocks, crops, the axle power source, stoked fire, the milk and bucket
  blocks, "Block 1091", 20 old "Fc ..." siding and moulding blocks, decorative blocks all named
  "Aesthetic Opaque" (most likely the reported "Fc Opaque Generic"), and the old slats, grate and
  wicker items. At the same time 33 variants were missing, among them the 11 decorative blocks (block
  of soap, padding, hellfire, rope, flint, ...), 15 wool slab colours, the grass and packed earth
  slabs and the blood wood sapling. The tab is now built from BTW's own creative-inventory rules and
  lists what 4.B listed: 355 entries (CT1, CT2, CT4).
  - Blocks have their BTW names, per variant: Block of Soap, Soulforge, Brick Oven, Piston Shovel
    (CT3).
  - Variants place the right block: a creative Block of Soap places soap, and crafted packed earth
    and grass slabs place their own slab.
  - The vanilla Search tab and JEI list exactly what the BTW tab lists, and JEI shows each variant
    on its own: 22 arcane scrolls; 16 wool, candle and knit wool colours; 4 bark types (CT5).
  - Middle-click pick block gives what 4.B gave (a stick from a shaft, seeds from a crop, the idle
    brick oven, the kiln's drop) instead of the technical block.
- **Placed leaves decayed.** Leaves placed against most blocks decayed like natural leaves. They now
  keep 1.5.2's player-placed flag and stay.
- **Blocks out of sync.** When nether growth spread over netherrack, or a sapling failed to grow
  into a tree, the client kept showing the old block (netherrack, or nothing where the sapling
  stood). Both now update at once.
- **Placement errors.** An error inside BTW's placement code could crash the game or the server. It
  is now logged once and nothing is placed.

BTW's rules for plants, fire and falling blocks (2026-10-14):

Blocks that keep a Minecraft id, such as saplings, crops, grass, leaves, fire and sand, ran Minecraft
1.20.1's rules instead of BTW's, because the port's hook never reached them. In a test world all 25
birch saplings became trees within 30 seconds, and saplings in the End grew too. BTW's own rules now
decide for every one of these blocks:

- **Trees** grow from saplings at BTW's slow rate, need light and never grow in the End. They grow into
  BTW's trees, big oaks and 2x2 jungle trees included, and they also grow below y=1 (superflat worlds,
  deep caves): BTW's tree code only knew 1.5.2's world, which started at y=0.
- **Crops and other plants** grow at BTW's rates and only under BTW's conditions. Wheat, carrots and
  potatoes need light and watered soil and stop growing while weeds grow. Pumpkin and melon stems, sugar
  cane, cactus, cocoa and vines follow BTW's rates. Nothing grows in the End, and nether wart grows only
  in the Nether. Pumpkin stems grow BTW's own pumpkins, and pumpkin and melon stems stay attached to
  their fruit.
- **Crop drops follow BTW.** Breaking or harvesting unripe wheat, carrots or potatoes drops nothing.
  Ripe wheat drops wheat and 1-2 seeds, ripe carrots and potatoes drop 2-3, and a ripe potato plant
  drops a poisonous potato 1 time in 50. Ripe nether wart drops 2-4. Before, every crop dropped its item
  plus wheat seeds at any stage.
- **Grass** grows back on loose dirt again. It spreads only in daylight and never in the End, and grass
  that gets too little sky light turns back into dirt. Tall grass spreads onto unused farmland.
  Mycelium spreads onto loose dirt and dies in the dark.
- **Brown mushrooms** spread in complete darkness (or on mycelium) and give off no light, as in BTW.
  Their faint 1.20.1 glow had kept them from ever spreading. Mushrooms on mycelium sometimes grow into
  huge mushrooms, whose blocks show their own cap, pores and stem since 2026-10-28 (see the texture fix
  below).
- **Leaves** decay the BTW way: only after a log or leaves near them are removed, and only when no log
  is within 4 blocks. They drop only saplings (1 in 20, jungle 1 in 40), never apples or sticks (in fact
  BTW's leaves dropped oak leaves until 2026-10-20; see the follow-ups below).
  Stripped logs and wood (bark blocks) of oak, spruce, birch and jungle hold up leaves and cocoa pods as
  logs do.
- **Fire** burns by BTW's rules: logs smoulder instead of burning away, BTW's wicker and wooden blocks
  burn, and the fire on a lit hibachi keeps burning. Fire on magma blocks keeps burning, as in 1.20.1.
  Blocks added after 1.5.2 that burn in 1.20.1 (bamboo wood, hay bales, ...) still burn. The logs of
  acacia, dark oak, mangrove and cherry (since 2026-10-20) and their planks (since 2026-10-28) burn by
  BTW's own fire rules, as oak does.
- **Lava** follows BTW: it lights BTW campfires and unlit torches next to it, melts BTW snow and starts
  fires at BTW's rate (1.20.1's fire spread ran twice for every lava block it picked).
- **Falling blocks.** Sand, gravel and the dragon egg no longer float on torches and other blocks they
  can fall through, and they land as BTW falling blocks. Anvils no longer fall, as in BTW. BTW's loose
  blocks (loose dirt, loose cobblestone, ...) now also fall below y=0.
- **Stone bricks** with water above them slowly turn mossy, and with lava above them cracked, when
  there is open space below them, as in BTW.
- **Plants on BTW soils.** Cactus on planters, sugar cane on loose dirt and crops on BTW farmland are no
  longer destroyed when a block next to them changes. Plants that 1.20.1 grows on its newer soils (grass
  on moss, mushrooms on podzol and nylium, cactus on red sand, ...) stay. Farmland that is trampled or
  covered turns into BTW loose dirt, as in BTW, instead of vanilla dirt.
- **`/gamerule doFireTick false` now also stops BTW's fire and lava fires.** Fire stays lit but does
  not spread or burn out, and lava sets nothing alight. With the rule on (the default), fire and lava
  behave exactly as in BTW. A burning ladder and a campfire can still spread fire.

Bone meal, newer plants and removed blocks (2026-10-15):

Three rulings by the maintainer bring more of Minecraft 1.20.1 in line with BTW 4.B. Each was tested in
a world on a dedicated server before and after the change.

- **Bone meal grew plants instantly.** On carrots, potatoes, wheat, stems, saplings, cocoa, beetroot or
  sweet berries it jumped them several stages, it grew mushrooms into huge mushrooms, and it was used
  up while the soil stayed as it was. In BTW bone meal never grows a plant: it fertilises BTW farmland
  and planters, which then grow crops twice as fast until the crop is fully grown. Bone meal now does
  exactly that, from either hand:
  - used on a plant, it fertilises the soil under the plant;
  - used on anything BTW cannot fertilise (grass blocks, netherrack, sand under water, a sapling on
    grass, ...), it does nothing and is kept: no grass, flowers, nylium or seagrass appear;
  - used on a crop whose soil has weeds, it pulls the weeds first and is kept, as in BTW;
  - it makes no particles or sound, as in BTW; the fertilised soil's look shows that it worked;
  - it no longer skips Minecraft's right-click checks, so protection mods can stop it.
- **White dye** no longer works as bone meal when you use it (dropped white dye still did until
  2026-10-20; see the follow-ups below).
- **Dispensers.** A vanilla dispenser now shoots bone meal out as an item, as in BTW, and the item
  fertilises the farmland it lands on. BTW's block dispenser now spits out items it has no use for
  (bone meal, dyes, string, ...), as in BTW; before, it clicked and kept them.
- **Weeds.** Right-clicking a crop, stem, sapling or flower whose soil has weeds pulls the weeds, as in
  BTW. Before, only BTW's own wheat did.
- **Bees** grew crops, stems and berry bushes a full stage per visit, ignoring BTW's growth rules. Each
  visit now gives the plant one extra growth chance under BTW's rules.
- **Plants added after 1.5.2 grew at 1.20.1's speed.** Each now follows the 4.B rules of its nearest
  BTW relative:
  - **Saplings.** Acacia, cherry and dark oak saplings and planted mangrove propagules grow like BTW's
    own saplings, about 18 times more slowly than in 1.20.1: they need light and never grow in the End.
    Dark oak needs four fully grown saplings in a square, like BTW's big jungle trees. Azaleas now grow
    into azalea trees by themselves, at the same pace.
  - **Crops.** Beetroot grows like BTW's carrots, and torchflower and pitcher crops like BTW's old
    wheat, at half the carrots' speed. They need light and watered soil, stop while weeds grow, grow
    twice as fast on fertilised soil and never grow in the End. Harvesting them turns BTW farmland back
    into loose dirt, as BTW's own crops do, and they still drop their own items.
  - **Nether fungi.** Crimson and warped fungi on their own nylium now grow into huge fungi by
    themselves, as BTW's mushrooms grow huge on mycelium, but never in the End.
  - **Other plants** keep their own growth but at the pace of their nearest BTW relative, and never in
    the End: bamboo and kelp grow like sugar cane, cave, weeping and twisting vines like BTW vines, and
    sweet berries and hanging mangrove propagules ripen like cocoa. Chorus flowers grow 16 times more
    slowly and still grow in the End, where they belong.
  - The leaves of the newer trees and nylium keep 1.20.1's decay, which is already as quick as BTW's.
- **Blocks and items that BTW 4.B removed came back.** The vanilla hopper, comparator, crafting table,
  chest, torch and others could be crafted again, found in chests and structures and picked from the
  creative tabs. They now stay removed (see Changed). The Minecraft recipes that 4.B removed or
  replaced with its own are gone too.

Hunger, mining, crafting and the HUD (second 2026-10-15 wave):

Parts of Minecraft 1.20.1 that BTW's code never reached still ran 1.20.1's rules. By the maintainer's
rulings, hunger and harvesting now cost what they cost in 4.B, and BTW's workbench offers every other
recipe after BTW's own. Unless marked otherwise, each gap below was first shown by a scripted in-world
test on the old code, and the test passes now; the HUD was checked with a development client.

- **Hunger costs** (#021). Walking cost no hunger, a jump was charged twice, and swimming, a melee hit
  and taking damage cost less than in 4.B. They now cost 4.B's exhaustion: 1.0 per 100 m walked (was 0),
  1.5 per 100 m swum (was 1.0), 0.2 per jump and 1.0 per sprint jump (were 0.25 and 1.2), and 0.3 per
  landed hit or hit taken (was 0.1; damage that ignores armour still costs nothing). Armour weight now
  raises every cost, BTW's own (jumping, harvesting) included, and creative and invulnerable players pay
  nothing. Hunger costs that other mods add straight to Minecraft's food data never reached BTW's hunger
  bar; they now do, under the same rule.
- **Healing and health** (#021). `/gamerule naturalRegeneration false` did not stop BTW's healing; it
  now does. When Minecraft or another mod left a player with a fraction of a health point, BTW's sync
  rounded it down (7.5 became 7), and a player left with less than 1 died. The real health is now kept,
  and BTW counts anything under 1 as 1.
- **Drinking while food-poisoned** (#021). In 4.B a food-poisoned player cannot drink, but potions,
  honey bottles and other mods' drinks could still be drunk. Every drink is now refused while you are
  food-poisoned. Splash and lingering potions are thrown, not drunk, and still work.
- **Breaking blocks** (#022).
  - Breaking a BTW block skipped Forge's block-break event, so claim and protection mods could not stop
    it and other mods never heard of the break. The event now comes first, as for every other block,
    with 1.20.1's adventure and spectator rules. Each chewing hit on a log counts as one break.
  - Creative players got drops from BTW blocks, and logs were chewed instead of removed. A creative
    break now just removes the block, as in 4.B.
  - BTW's own break effects never ran. A broken mining charge now drops itself, a vase holding blasting
    oil explodes when broken without Silk Touch, and BTW animals near a block a survival player breaks
    panic, as in 4.B (the vase and the animals are checked by code review only).
  - BTW's health, hunger, fat and gloom penalties did not slow the mining of blocks added after 1.5.2
    (deepslate, tuff, copper ore, ...). They now do, on server and client (checked by code review only:
    the test player cannot carry a penalty).
  - BTW's pickaxes, axes, shovels, hoes and mattocks were not the right tool for blocks added after
    1.5.2, such as tuff and copper ore, so those blocks dropped nothing. They now dig them at BTW's
    speed for the tool and get their drops. Deepslate and the deepslate ores are BTW's stone and ores
    since 2026-10-28 (see Changed), so a mattock on deepslate gets BTW's stone drops.
- **Crafting** (#025).
  - BTW's workbench, work stump and soulforge knew only BTW's own recipes, and 499 of 1.20.1's 822
    crafting recipes need a 3x3 grid, so those recipes, and every 3x3 recipe of other mods, could not be
    crafted anywhere. These grids now fall back to every recipe that Minecraft and other mods register,
    after BTW's own (a BTW recipe always wins), and leave each recipe's leftovers (a honey bottle leaves
    its glass bottle). The lantern (with a nether-coal torch), chains, glass panes and gold blocks can
    now be made. Recipes that 4.B removed stay removed.
  - Ink sac, cocoa beans, lapis lazuli and bone meal now dye wool and carpets at the BTW workbench, as
    in 4.B, and make 1.20.1's black, brown, blue and white stained glass, terracotta, concrete powder
    and candles. The grindstone (stone slabs) and dark prismarine (black dye) can be made too.
  - The blast furnace, smoker and furnace minecart take a brick oven in place of the furnace, and the
    crossbow takes an iron nugget in place of the tripwire hook (a stoked crucible melts a hook into one
    nugget), so all four can be made from new materials.
  - Raw mutton, rabbit and salmon could not be cooked: the furnace is gone, and BTW's brick oven and
    campfire knew only 1.5.2's foods. Any edible item that has a 1.20.1 cooking recipe and no BTW one
    now cooks in the brick oven and on the BTW campfire, foods from other mods included.
- **The food bar in the HUD** (#023). BTW drew its food row outside Minecraft's HUD stack, so air
  bubbles, a ridden animal's hearts and other mods' bars were drawn over it. The row now takes the food
  bar's place in that stack: everything that stacks above the food bar sits above BTW's row, and under
  water the hunger and gloom text moves up above the bubbles. BTW still draws the row while you ride, as
  1.5.2 did. The food pips also shook on the frame rate instead of the game clock (about 7 times too
  often at 144 fps). Checked with a development client in survival and creative, under water, riding and
  at each hunger penalty.

Found by the integration tests (2026-10-17):

- **Blocks that take their shape from a neighbour** read as air to BTW's machines. Minecraft caches each
  block's collision shape in an empty world, where such a block has none. Dynamic Trees' trunk shells
  (the outer ring of a thick trunk) were affected, so BTW's saw and stump remover never reached them.
  BTW now asks for the shape at the block's real position when the cached one is empty.

Reported on CurseForge (comment of 2026-10-19):

- **Placed blocks never ran out, phantom items, placed blocks back after death.** Most solid blocks
  (gravel and other slabs, dirt, cobblestone, planks, pressure plates, loose snow), placed without
  sneaking against a block BTW handles, stayed in the server's inventory: a nested refresh of BTW's view
  of the inventory threw the count change away. Gravel slabs placed on gravel slabs never ran out, the
  hotbar showed fewer items than the server held (a later shift-click revealed them), and a death
  dropped the "placed" blocks. Placing while hanging on a ladder lost the count for any block, rails
  included. BTW now keeps the stacks it holds across a refresh, as 1.5.2's single inventory did, and
  resends the held slot after a BTW placement (CF2-I1, CF2-I2, CF2-I3, CF2-I5; 1.3.0-alpha.3 had an
  older trigger of the same symptom, CF2-P10). Dyeing a windmill now uses up the dye. Checked with a
  development client joined to a dedicated server.
- **Beds and doors duplicated.** Breaking a bed gave two beds, one of them red, and breaking a door's
  top half gave two doors; duplicated iron doors melt in a stoked crucible into iron and gold. By the
  maintainer's ruling, beds and the oak and iron doors now break and drop as in 1.20.1: one item from
  either half, a bed keeps its colour, and creative breaks drop nothing. A saw cutting an oak door's top
  gives one door, and a mining charge that breaks a door, or a bed whose head it reaches first, now
  leaves the item (it left nothing) (CF2-DR1, CF2-DR6; #039).
- **Flowers on stone and slabs.** BTW's "cannot be placed here" check was a stub: flowers, saplings,
  mushrooms and grass could be placed on stone and slabs, and a block could be placed into a cell that
  was not free. BTW's placement now makes 4.B's checks (see the placement fix below). Flowers that
  earlier versions placed on stone or slabs pop, as in 4.B (CF2-P8a, CF2-P8b).
- **Beds: no message at night, and every click set the spawn.** A bed click at night failed with an
  error in the log and showed nothing, and every bed click, day or night, set the spawn point. Beds now
  let you sleep but never set the spawn (see Changed) (CF2-U1).
- **Falling blocks were orange outlines.** Sand, gravel, loose dirt, loose cobblestone, BTW's loose
  slabs and stairs, loose snow, falling netherrack and the dragon egg were drawn as an orange outline
  while they fell, and showed their texture only once they landed. They now fall as real blocks,
  smoothly, as in 4.B. A falling slab, stair or dragon egg keeps its shape, a falling block is shaded
  like the same block on the ground, and the landed block no longer blinks out for a moment. A block
  that was still falling when the game saved came back as an orange outline frozen in mid-air for good,
  and the block itself was lost. By the maintainer's ruling it now carries on falling and lands when the
  area loads again (4.B lost it). Stuck outlines left by earlier versions are removed, and a block that
  an earlier version saved mid-fall lands too (CF2-R1; #043).
- **The breaking crack on BTW's spider webs.** It was smeared into streaks and stretched. By the
  maintainer's ruling the crack on BTW blocks now looks exactly as in 4.B: on spider webs it covers each
  web plane edge to edge, on ladders and the faces of BTW blocks it is no longer mirrored, and it no
  longer appears on block parts 4.B never cracked, such as an open basket's lid or food cooking on a
  campfire. The crack images stay Minecraft's, so resource packs still change them. Breaking or hitting
  a damaged spider web, or any BTW block with several variants, gives off particles of that block's own
  texture, and BTW's crossed plants, crops and ladders are lit flat, as in 4.B: smooth lighting no
  longer darkens their corners (CF2-R3; #041).
- **Loose snow smoked like a campfire.** Loose snow and loose snow slabs give off faint white wisps
  again, as in 4.B, instead of large grey campfire smoke (CF2-R5b). The same fix covers BTW's other
  particles (#042):
  - campfires cooking food, kilns, cauldrons and crucibles on a fire, smouldering logs, mill stones and
    clay bricks drying in the sun give off BTW's white smoke, which rises, instead of grey campfire
    smoke or puffs that hung in place; cauldrons, crucibles and campfires no longer pour out an extra
    stream of grey smoke that even Minimal could not hide;
  - smoke, flames and other particles from BTW blocks show within 16 blocks and follow the Particles
    setting, as in 4.B; only explosions and a bursting smouldering log's cinders show at every setting.
    A gearbox or other device breaking from overload no longer shows its puff at Minimal;
  - candles show their flame again; a smouldering log that bursts throws glowing cinders instead of grey
    ash; a snow golem made with a soul urn appears in a fall of snow puffs; a powered detector block
    shows its dim red dust instead of bright red dust drifting sideways.
- **Items shot out of broken blocks in odd directions.** Stone and rough stone mined with a stone
  pickaxe, BTW's ores mined in stages, logs chewed by hand, the work stump and grass dug with a hoe
  dropped their items into the block above the one being mined. In a tunnel they shot sideways through
  the rock or rose up through the ceiling and could be lost; on a tree trunk the bark came out of a
  random side of the log above. They now come out of the side you dig, toward you, as in 4.B, also when
  the server finishes the break a moment after your client. Every BTW drop also started up to an eighth
  of a block too high, so about one block drop in seven began inside the block above and was pushed out
  through the rock. BTW's drops now start inside the broken block, as in 4.B, and mob drops still land
  on the ground (CF2-DR2; #044).
- **Mobs targeted players in creative.** BTW mobs no longer chase or attack you after you switch to
  creative or spectator mode. In creative, hitting a BTW mob, angering pigmen or endermen, startling a
  wild wolf with a fire starter or feeding a wolf wolf meat no longer makes them go after you. Minecraft
  1.20.1 works the same way; 4.B kept chasing a player who switched to creative. This follows a
  maintainer ruling, which changes only the targeting (see Known issues) (CF2-M7; #046).
- **Flowers turned into poppies.** Blue orchids, alliums, azure bluets, tulips, oxeye daisies,
  cornflowers and lilies of the valley no longer turn into poppies. They stay themselves when you break
  them, when they pop off because their soil was dug out or the light is too low, when you place them,
  and in BTW's hoppers, baskets and other storage. The blue orchid, allium, azure bluet, tulip and oxeye
  daisy dyes and the flower banner pattern can now be crafted at the BTW workbench, and the millstone
  grinds only the poppy into red dye; the other flowers no longer count as poppies there (CF2-DR3,
  CF2-P8c; #045).
- **Deepslate and raw ores worked as in plain Minecraft.** They now follow 4.B's stone strata and ore
  chunks (see Changed) (CF2-PR-DEEP, CF2-PR-ORE; #050).
- **Creepers dropped full blocks.** A 4.B creeper drops full blocks too, but explosions now drop what
  BTW drops: blasted stone gives loose cobblestone, a stone and a gravel pile; cobblestone gives loose
  cobblestone; dirt, grass and mycelium give loose dirt and loosen the dirt around the hole. Chewed
  logs, mouldings, sidings and corners break into sawdust, a mining charge caught in a blast goes off,
  snow layers drop nothing, and BTW blocks keep their colour or type (CF2-DR4). Mining charges, which
  dropped nothing from most blocks, drop the pieces of what they break: stone gives stones and gravel
  piles; dirt, gravel and sand give piles; logs give sawdust and bark; crimson and warped stems give
  sawdust, and other mods' logs give sawdust and, while barked, their bark (CF2-DR5; #049).
- **Villagers were not zombified.** Villages are now zombified, as in 4.B (see Changed) (CF2-MU3; #048).
  Curing a villager zombie with a soul urn works: a cured villager zombie that came from world
  generation, a command or an earlier session turned into a new villager every tick and never went away.
  Now it becomes one villager and the zombie is gone. A villager killed by a zombie now disappears when
  its zombie appears, as in 4.B, instead of dying beside it.

Follow-ups (2026-10-19 to 2026-10-21):

- **White dye** is never fertiliser, chicken feed, dog food or the farmer's bone-meal trade, on any
  path: dropped, from either dispenser, in a hopper, at a grid, in the cauldron or in a trade (#017).
  Lily of the valley and the 1.20.1 bone meal -> white dye recipe give real white dye at BTW's grids
  (they gave bone meal). An ink sac put into a BTW container no longer merges into another dye's stack.
  BTW villager trades check subtypes, as in 4.B (the brown wool knit and blood wood sapling trades).
- **Dyes and other BTW items with subtypes stack in BTW hoppers and chests**, as in 4.B: wool, candles,
  vases and slabs took a new slot for each item. 1.20.1's black, brown and blue dye keep their identity
  in BTW hoppers, chests, cauldrons and dispensers (they became ink sacs, plantable cocoa beans and
  lapis); cornflowers no longer give lapis and wither roses no longer give ink sacs at BTW's grids; nine
  blue dyes no longer make a lapis block (#038).
- **BTW leaves** drop their own sapling (they dropped oak leaves); shears and Silk Touch give the
  leaves; blood wood leaves drop a sapling 1 time in 20 (it was every break); BTW's block dispenser
  takes the right species of leaves (#035).
- **Right-click order** (#033). A right-click uses the clicked block before the held BTW item, as in
  4.B: flint and steel on a lever now flips the lever. Claim and protection mods that cancel a
  right-click or deny the item now stop BTW items too, an off-hand click on a block never uses the
  main-hand BTW item, and a successful use counts in the item-used statistic and its advancement
  trigger. A few blocks still answer first the 1.20.1 way (see Known issues).
- **Placement follows 4.B's rules** (#040). BTW's own placement now makes 4.B's checks: the target cell
  must be free or replaceable, the clicked side must hold the block, no mob or player may stand in the
  way (dropped items and XP orbs no longer count, nor do they block BTW's mob spawns), and you cannot
  place a block while in mid-air (on a ladder or in water you can). A refusal is final: Minecraft's
  placement no longer takes over, and your hand shows the right count. A tripwire hook from `/give`
  places as in 4.B: on the side of a full block, facing away from it, whichever way you look. Falling
  sand no longer deletes rails. Stairs, doors, beds, gates and observers placed the Minecraft way are no
  longer turned round. An oak door is no longer used up without being placed. BTW-placed logs, the newer
  woods included, lie along the clicked side. An oak trapdoor or fence gate placed against a live
  redstone signal opens, as in 1.20.1. A block placed from the last item of a stack is turned like any
  other. Windmills and water wheels are refused where a mob or player stands. Swimming or riding players
  can place through BTW's code, as in 4.B, and BTW squids see a swimming player as in water (both
  checked by code reading only) (CF2-P8a).
- **Crashes.** Breaking, burning or blowing up a beacon or ender chest no longer fails with an error (a
  broken beacon was lost), and TNT next to a beacon no longer stops the server (#036).
- **Farmland** is trampled by 4.B's rule: a chance that starts at a fall of 0.75 blocks, checked once
  per fall. Minecraft's own check, from 0.5 blocks, ran as well (#036).
- **Tools BTW breaks** while mining a modern block, a bed or a door now tell other mods they broke
  (Forge's player-destroyed-item event).

Found with that report (2026-10-21 to 2026-10-28):

- **4.B's hardcore respawn was missing.** With beds no longer setting the spawn, every death brought you
  back at the world spawn. The hardcore respawn is ported (see Changed) (CF2-U1-HS; #054).
- **BTW squids and the mill stone never reached players.** BTW's own code did not see the players in a
  world. BTW squids hunt players at night and in the dark again. They latch onto your head and ride
  along until you kill them. While one is on your head it weighs you down like heavy armour and knocks
  you out of boats and minecarts, and its bite goes through armour; only a soulforged steel helm keeps
  it off. Their tentacles fling you, and knock a BTW sword or tool out of your hand if you are blocking
  with it. Grinding netherrack in a mill stone makes the players near it nauseous again (CF2-MX1,
  CF2-MX3; #047).
- **Sneaking and invisibility** hide you from BTW mobs again, as in 4.B: sneaking cuts how far they
  notice you by a fifth, and invisibility without armour almost completely. Mobs no longer go after a
  player on the death screen (CF2-MX2; #046).
- **Cows, squids and spider jockeys.** A BTW cow's kick flings you again, as in 4.B. A squid that
  latches onto an animal or a villager now rides it instead of hanging in mid-air and biting it from
  there, and lets go when it dies; an animal no longer stays "head-crabbed" (no milk, no eggs) after its
  squid is gone. Skeletons that climb onto BTW spiders ride them as spider jockeys again (#047).
- **Broken blocks lost their kind.** Broken coloured wool, sandstone, slabs (double slabs too), mossy
  cobblestone walls, chiseled quartz, quartz pillars and mob heads drop their own kind instead of the
  plain one (white wool, oak slab, smooth stone slab, skeleton skull, ...); a player head drops without
  its owner. Wet sponges, stone slabs and smooth sandstone drop themselves, and a chipped or damaged
  anvil mined with Silk Touch stays chipped or damaged. Infested blocks drop nothing when broken, as in
  4.B and Minecraft; they dropped an infested stone block (#045).
- **Stacking.** Coloured wool, spruce, birch, jungle and the four newer woods' planks and logs, typed
  slabs, sandstone, stone bricks, walls, quartz, flowers and mob heads stack in BTW's hoppers, baskets
  and chests, and one kind no longer merges into another there (a white wool used to turn into the
  orange wool it joined) (#045).
- **Explosions** (#049), besides the creeper drops above:
  - digging dirt, grass or mycelium by hand loosens the dirt next to it, as in 4.B;
  - mining charges no longer delete water and lava, and dirt, grass, sand and gravel resist explosions
    as in 4.B, so craters in soil are no bigger than 4.B's;
  - deeper stone and ores resist explosions as in 4.B (with the stone strata, see Changed);
  - BTW blocks broken by pistons, other mods or the `/loot` command keep their colour or type.
- **Beacons** (#053). Beacons whose pyramid goes below y=0 power up (deep caves, superflat worlds), and
  a beacon moved with `/clone` or a structure block powers up at its new place. Right-clicking a
  soulforged steel beacon binds you to it ("You have been bound to this beacon"), also when sneaking
  with an empty hand. The beacon itself now works as in 4.B (see Changed).
- **Nether quartz ore** mined with a pickaxe dropped the ore block instead of quartz (#050).
- **The inventory crafting grid** now takes exactly what BTW 4.B takes for BTW's recipes: two spruce
  sidings make spruce planks (not another wood's), three spruce mouldings make spruce stairs, and the
  colour recipes need the white candle, wool, vase or wool slab (#025).

Found while fixing it (2026-10-23 to 2026-11-03):

The maintainer ruled on 2026-10-23 that bugs found during this work are proven and fixed with it.

- **BTW blocks answer as in 4.B** (#040). Many blocks counted as solid full blocks to BTW's code where
  4.B's did not:
  - BTW grates, wicker panes and slats no longer collide as full cubes: you can walk past them as in
    4.B, and they no longer join doors, beds or other kinds of pane. Their items are flat icons, as in
    4.B;
  - BTW mobs walk over pressure plates and past signs, and walls stop them properly;
  - a BTW chest opens with glass on top of it;
  - BTW torches and other BTW attachments hold only on solid blocks, as in 4.B: not on glowstone,
    redstone lamps, doors, beds or BTW panes (see Changed for existing builds);
  - glass no longer counts as a full block for BTW's placement, so a tripwire hook can no longer be hung
    on glass, as in 4.B.
- **Torches and ladders** (#055). Torches, soul torches and ladders can no longer be placed on
  glowstone, redstone lamps, TNT, pistons, beacons, the tops of glass panes and iron bars, or the sides
  of glass and ice, as in 4.B (soul torches take the torch's rule, by the maintainer's ruling). A soul
  torch already standing on one of them drops at the next block change beside it, as a torch does. A
  redstone torch clicked onto the side of a block hangs on that wall, as in 4.B, instead of standing on
  the block below it or in mid-air, and glowstone's side refuses it (for its top, see below).
- **The redstone torch on top of glowstone and ice** (#055, #061). A redstone torch could still stand on
  top of glowstone or ice, which 4.B refuses, because BTW took Minecraft's answer to whether a block's
  top is solid. BTW's own answer decides now (see Changed): a standing redstone torch is refused on
  glowstone, ice, TNT, pistons, redstone lamps, beacons, the top half of a closed oak trapdoor and BTW
  machines without a solid top, whether you place it sneaking or not, and one already standing there
  drops when a block next to it changes. Glass, fences and walls still hold it, as in 4.B.
- **Snow on glass, glowstone and BTW machines** (#055, #061). Snow could lie on glass, glowstone,
  redstone lamps, TNT, pistons, beacons, the top half of a closed oak trapdoor and BTW machines such as
  the hopper, mill stone, cauldron and crucible, where 4.B never lets snow lie. By the maintainer's
  ruling, falling snow now skips them, a snow layer placed while sneaking is refused there, and BTW's
  ash follows the same rule. Ice already refused snow.
- **Pressure plates and rails placed while sneaking** (#055, #061). Placed while sneaking, a stone or
  oak pressure plate could go on glass, glowstone, ice, a fence or a wall, the soulforged steel plate
  even in mid-air, and a rail, powered rail or detector rail (BTW's included) on glass, glowstone, ice
  or a hopper. By the maintainer's ruling they now follow 4.B's rules whichever way you place them, and
  a rail drops when the block under it, or the block an ascending rail climbs onto, stops being solid
  (Minecraft kept it on glass). A BTW ladder placed while sneaking no longer hangs with nothing to hang
  from, and a BTW torch with nothing to hold it is refused instead of dropping at once.
- **Minecraft's newer pressure plates** (#055). By the maintainer's ruling the wooden pressure plates of
  spruce, birch, jungle, acacia, dark oak, mangrove, cherry, bamboo, crimson and warped take BTW's oak
  plate rule, and the polished blackstone plate the stone plate's: like them, they can no longer be
  placed on glass, glowstone, fences, walls or the other blocks 4.B refuses, and one already on such a
  block drops when a block next to it changes. The weighted pressure plates stay removed.
- **Minecraft's newer fences, walls and stained glass panes** (#062). By the maintainer's ruling the
  wooden fences of spruce, birch, jungle, acacia, dark oak, mangrove, cherry, bamboo, crimson and warped
  count as BTW's oak fence, Minecraft's twenty walls other than the cobblestone and mossy cobblestone
  walls as the cobblestone wall, and the sixteen stained glass panes as the glass pane, wherever BTW
  asks what a block can hold:
  - a redstone torch stands on the fences and walls, as on BTW's oak fence and cobblestone wall, and
    stakes, placed tools and lightning rods now go on top of them and under them;
  - sand, gravel and other falling blocks rest on them, and BTW's ash can lie above them over solid
    ground, as with BTW's oak fence;
  - pressure plates can no longer be placed on them, and an iron spike on a fence's or wall's arm drops
    when a block next to it changes;
  - torches, soul torches, redstone torches, BTW torches, BTW candles, placed ore chunks, ash and
    pressure plates no longer stand on a stained glass pane.

  All ten fences count as the oak fence, also where BTW has a fence of that wood: a redstone torch
  stands on Minecraft's spruce fence, while BTW's own spruce fence refuses it, as in 4.B. Their drops,
  burning, mining, connections and looks stay Minecraft's.
- **Placed tools in Minecraft's newer fences, walls and panes** (#062). No tool could be stuck into
  these fences and walls. By the maintainer's ruling, which tool sticks is a support question too:
  every axe sticks into the top or bottom of the ten fences, as into BTW's oak fence, and no other tool
  does; pickaxes and the iron chisel stick into the walls, as into the cobblestone wall; and no tool
  sticks into a stained glass pane, as with the glass pane. Mining them is unchanged.
- **The wicker pane and other BTW textures** (#059). The wicker pane shows its wicker texture (it drew
  the missing texture), the soulforged steel pressure plate looks like steel, the BTW workbench shows
  its top, sides and bottom, the blocks of huge mushrooms show their cap, pores and stem (they showed
  the red cap on every face), and a hopper's filter shows the right texture (and none for items that are
  not filters). Blocks and items held or worn by mobs, carried by endermen or lifted by platforms show
  their BTW textures instead of the missing texture: logs, wool, pumpkins, mycelium, slabs, leaves,
  walls, gates, jukeboxes, dispensers, furnaces, pistons, enchanting tables, lamps, dyes, potions; a
  curved rail stays curved, and redstone wire lifted by a platform shows its cross.
- **Pressure plates, buttons, comparators and detector rails** (#056). The soulforged steel pressure
  plate presses only for players, stone plates for mobs and players, and wooden plates for everything,
  as in 4.B: BTW mobs now press plates, and the soulforged plate works (it never pressed). BTW arrows
  press wooden buttons. Comparators read BTW chests (a double chest as one) and the brick oven, as in
  4.B; the comparator block itself stays removed, by the maintainer's ruling. A double BTW chest with a
  solid block on either half stays shut, as in 4.B. BTW's wooden and soulforged steel detector rails are
  Minecraft detector rails that keep BTW's rules: minecarts run over them (they derailed), the wooden
  one triggers for every cart and the soulforged one only for a cart carrying a player. Minecraft's
  detector rail ignores an empty cart, as in 4.B.
- **Minecarts, boats and other Minecraft entities** (#057). BTW's blocks see Minecraft's minecarts,
  boats, paintings, XP orbs and mobs, as 4.B's saw their 1.5.2 forms: a BTW hopper fills a chest
  minecart under it, the block dispenser picks up minecarts and boats (a boat comes back as its own
  boat, its wood and chest kept, and a chest minecart spills its contents, as in 4.B), the detector
  block sees them, a mining charge breaks them and a soul-sand hopper swallows XP orbs. Minecarts and
  boats that BTW places are Minecraft's own.
- **Nether portals lit by a campfire** (#058). A campfire lit inside an obsidian frame opens a portal
  that faces the frame's own way and fills any frame a fire would fill (up to 21x21); north-south
  portals lit by a campfire no longer lose a column when a block is placed beside them. A fire also
  opens a frame with a campfire inside, as in 4.B, and a campfire opens a portal in any dimension, as in
  4.B. Lighting a portal either way counts toward the hardcore respawn's distance at once (#054).
- **Burning netherrack below y=0** went out as it started to fall and lit nothing where it landed. It
  now falls burning, as it does higher up (#043).
- **Items pushed off fertilised farmland.** An item or a mob resting on farmland that BTW turned into
  another block (bone meal fertilising it, for example) was pushed out sideways, up to about two blocks.
  It is now lifted onto the new block, where 4.B had it all along. The same holds for any block that is
  a full block to BTW but lower in Minecraft (#060).

Other fixes:

- **Mining speed.** Every BTW block broke at the same speed with any tool: rough stone took about
  3.4 s by hand or with an iron pick. BTW's penalties never slowed mining. Digging now uses BTW's own
  formula on both server and client, so BTW blocks are slow by hand and fast with the right tool, and
  health, hunger, fat and gloom penalties slow it (B3). On a dedicated server, the client's dig
  prediction ran FC hardness against a missing world and could crash when mining stone, ore or logs
  (B3-CLIENT; found by code review, not reproduced). Mining deepslate, tuff or another block added after 1.5.2
  with a tool could crash a single-player game. It no longer does, and BTW tools now wear on those
  blocks.
- **Penalties.** The melee damage penalty compounded up to four times on a hit, and was applied once
  more to all damage the player dealt, arrows included. It now applies once, to melee only (B1).
  The slowdown stayed after a penalty had cleared, until relog or death. It now follows the current
  penalty (B2). BTW's block speed modifiers now reach players (B4, see Changed).
- **Food.** Raw meat, raw fish, melon, mushroom stew and rotten flesh restored hunger twice: BTW's
  value plus Minecraft's. Every food now restores hunger once. For example, raw beef gives 12 points
  of BTW's 60-point hunger bar instead of 21. Carrots and potatoes restore BTW's 3. Golden apples,
  spider eyes and poisonous potatoes now apply their BTW effects. Other vanilla foods now use BTW's
  values (see Changed).
- **Bowls and drink containers.** Chicken soup, chowder and hearty stew never gave their bowl back.
  They now return one, as in 1.5.2. With a full inventory, mushroom stew gave two bowls and a potion
  two glass bottles; both now give one. Drinking BTW chocolate milk lost the bucket. In creative
  mode, chocolate milk is no longer used up, eating no longer plays BTW's "can't eat" sound, and a
  full inventory no longer produces free bowls.
- **Crafting.**
  - FC recipes no longer vanish from the recipe book on every login, and recipe-book auto-fill works
    for them. They are now injected into both of the recipe manager's indexes (RE6).
  - FC recipes are no longer lost after `/reload`; they are re-injected after every datapack reload.
  - The player's 2x2 inventory crafting grid now follows FC's crafting rules, as in 1.5.2:
    - tools used as ingredients (shears, chisels, axes) lose durability instead of being used up
      (not in creative mode);
    - moulds and other reusable items stay in the grid;
    - FC's per-ingredient effects happen: sawdust and bark from chopping logs, pumpkin seeds, gravel
      from splitting stone with the iron chisel, string from breaking a bow, the axe chop sound.
  - FC's code-driven crafting recipes (log chopping with an axe, knitting, baiting a fishing rod) now
    work in the 2x2 inventory grid. Before, they only worked at a crafting table. The knitting recipe
    starts the knitting, which you then finish by holding right-click (see the player report above).
  - Ingredients that accept any variant now accept every variant in every grid, at the soulforge, in
    the recipe book and in JEI. Before:
    - "any planks" meant jungle planks only in the 2x2 grid, and never matched at the crafting table;
    - shaped recipes that call for a block (planks in the axle and the hamper) took only its first
      variant;
    - the 3x3 wool block could not be crafted anywhere.
  - Wool blocks and wool armour get their blended colour again. BTW's per-ingredient effects now
    happen at the crafting table for every recipe, not only for log chopping.
  - A mould left in the soulforge grid no longer blocks recipes.
  - An FC recipe whose ingredient has no 1.20.1 equivalent is now skipped instead of being added
    without that ingredient.
- **Items and inventory.**
  - Right-clicking a BTW block, pressing a button in a BTW container or hitting a mob with a BTW tool
    could change other items in the inventory: wool changed colour, spruce, birch and jungle logs
    turned to oak, names were lost, spawn eggs were deleted, BTW tool wear was undone, and stacks
    picked up a stray tag and stopped stacking. Only the slots BTW actually changes are written back
    now. Wool, wood species, dyes, coal and charcoal, skulls and spawn eggs convert correctly (U1).
  - Shift-clicking into or out of BTW containers could delete or duplicate items, pressing Q on some
    BTW container slots deleted an item, and enchantments added by BTW could be lost (U1b, U2).
  - Items added after 1.5.2 that are stored in BTW containers are no longer lost or swapped after a
    game restart (RE5).
  - The global (tier-3) ender chest is no longer emptied on reload (RE3).
  - Minecraft enchantments now count in BTW code. Silk Touch, Fortune, Looting, Efficiency,
    Unbreaking and Protection affect BTW harvests, drops, digging, wear and armour, and the Infernal
    Enchanter counts them toward its limits.
- **Dropped items.** BTW could not see dropped items at all. BTW hoppers, cauldrons and crucibles now
  collect items dropped on them, animals go for loose food, campfires burn dropped fuel, bellows blow
  items around and cactus destroys them (U3).
- **Bows and projectiles.**
  - The vanilla bow fired two arrows per shot, BTW's and Minecraft's. It now fires one.
  - The BTW composite bow can be drawn, and composite-bow and broadhead arrows no longer vanish when
    picked up.
  - BTW throwables (soul urns, dynamite) threw two per click and were never used up. Now one is
    thrown and one is used.
  - BTW arrows, thrown items and fireballs flew through vanilla mobs, boats and end crystals. They
    now hit them and credit the shooter.
  - Arrows and fireballs from BTW mobs hit players as projectile damage, so shields, Projectile
    Protection and "shot by" messages work. BTW mining charges deal explosion damage, so Blast
    Protection works.
- **Using items.** BTW right-click actions from the off hand ran against the main-hand item, so an
  off-hand seed could empty the main-hand slot. BTW item actions now use the main hand only. In
  creative mode, BTW seeds, hoes and flint and steel were used up or damaged; they no longer are.
  The breeding harness, dung and dye now work on BTW animals, and feeding a BTW animal uses up the
  item.
- **Blocks and rendering.**
  - BTW blocks now render with their real neighbours and contents: table and bench legs; connecting
    grates, slats and panes; platform rails; door halves; cauldron and crucible contents.
  - Wheat crops and weeds, blood-wood stairs, the BTW chest and placed tools were invisible and now
    show. Smooth-stone stairs have their texture. The wicker basket shows its stored item and its lid
    opens (U7, U7b, U8).
  - Blocks that mobs hold, wear or carry are drawn centred, in 3D and with the right textures: a
    zombie's cobblestone, a snow golem's pumpkin, an enderman's cactus or grass block. Held torches
    and flowers are flat icons.
- **World.**
  - BTW's world clock was stuck at 0, so BTW chickens never laid eggs and torches carried in the
    inventory never burned out. BTW's Nether, Wither and End progress was not saved either (U9).
  - Snow layers on stone, dirt or grass vanished whenever a neighbouring block changed, and burning
    leaves never left ash.
  - Overworld nether portals spawned zombified piglins, which BTW prevents (GH5).
  - Penned animals no longer hop over fences and walls.
  - A BTW cow's kick threw an error that cut its update short (U6).
  - A summoned wither despawned when players moved away.
  - BTW mobs skipped a tick when landing (HAI-10).
- **Shipped jar only.** The 1.3.0-alpha.3 jar contained old copies of 16 internal classes that
  development builds never ran. As a result, the anvil and enchanting table could crash the game,
  some chest windows showed every slot twice, and shift-crafting gave items without using up the
  grid. The jar now uses the same classes as development builds, and the build fails if they drift
  apart (E1, RE-M3).
- **Entity names.** Death messages and commands showed raw keys such as
  `entity.betterthanwolves.fc_skeleton`. Every BTW entity now has a name.
- **Logging.** The `[MOB-DMG]` warning on every hit is gone, along with other leftover diagnostics
  that logged on every tick, block change or screen open (U10). Real failures are logged once per
  code location with a stack trace, instead of being hidden or sampled (HAI-10).

### Changed

Most of these restore BTW 4.B behaviour, but players will notice them.

- **Food values follow BTW 4.B.** Vanilla foods that BTW rebalanced now restore BTW's values instead
  of Minecraft's. Values are points of BTW's 60-point hunger bar (6 points = 1 shank):

  | Food | Now | Was |
  |---|---|---|
  | Bread | 9 | 15 |
  | Cooked porkchop, steak | 15 | 24 |
  | Cooked chicken | 12 | 18 |
  | Cooked fish | 12 | 15 |
  | Baked potato | 6 | 15 |
  | Pumpkin pie | 6 | 24 |
  | Cookie | 3 | 6 |
  | Apple | 3 | 12 |
  | Golden carrot | 3 | 18 |
  | Golden apple | 3 | 12 |

  Saturation follows BTW too. The golden apple gives Regeneration I for 5 s instead of Regeneration
  II and Absorption. The enchanted golden apple gives BTW's Regeneration IV, Resistance and Fire
  Resistance. Cookies and pumpkin pie can be eaten when full, BTW wolves eat cooked meat, and BTW
  pigs eat baked potatoes.
- **Foods stack to their 4.B size everywhere** (#024). Every food now stacks to its 4.B size in the
  player's inventory and in every container: 16 for most foods, BTW's own size for a BTW food that has
  one (cured meat 32), and 16 for foods from other mods. Larger stacks that already exist stay as they
  are but no longer grow. Foods that stack to 1 in 1.20.1, such as mushroom stew and the milk bucket,
  stay at 1.
- **Minecraft foods that 1.5.2 did not have now feed the value of their nearest BTW food** (#024):
  mutton as BTW's own mutton, rabbit as BTW's chicken, salmon and the other fish as BTW's fish, rabbit
  stew as the hearty stew. Before, they fed 1.20.1's value. Points of BTW's 60-point hunger bar:

  | Food | Now | Was |
  |---|---|---|
  | Raw mutton, raw salmon | 9 | 6 |
  | Tropical fish, pufferfish | 9 | 3 |
  | Cooked mutton, cooked salmon | 12 | 18 |
  | Cooked rabbit | 12 | 15 |
  | Beetroot soup, suspicious stew | 9 | 18 |
  | Honey bottle | 6 | 18 |
  | Sweet berries, glow berries | 3 | 6 |
  | Chorus fruit | 3 | 12 |

  Raw rabbit (9), rabbit stew (30), beetroot and dried kelp (3) keep their amount. Raw mutton, rabbit,
  salmon, tropical fish and pufferfish now carry BTW's food poisoning (a 30% chance, 60 s), as BTW's raw
  meat does. BTW wolves eat mutton, rabbit and rabbit stew, BTW pigs eat beetroot and BTW chickens
  beetroot seeds. Foods from other mods still feed 3 points per 1.20.1 hunger point, unless a
  `btw_foods` table gives them a BTW value (see Added). BTW's own donut, kibble, stews and chocolate
  milk now report their BTW values to other mods that read food values.
- **Movement.** BTW's block speed modifiers apply to players (B4):
  - about 20% faster on hard surfaces;
  - 80% speed on sand, farmland, loose snow and plants;
  - half speed when walking on leaves.

  BTW's penalties now also slow you in the air, as in 1.5.2.
- **Mob spawns.**
  - 1.20.1's drowned, husks and zombie villagers spawn as BTW zombies, strays and wither skeletons as
    BTW skeletons, and glow squids as BTW squids. Drowned therefore bring BTW zombies to rivers and
    oceans, which BTW never had.
  - Naturally spawned zombie villagers still become plain zombies, because BTW's own zombie spawns never
    make villager zombies. Zombie villagers in villages stay villager zombies (SW5; #048).
  - Witches spawn naturally only in swamps (GH2).
- **Blocks added after 1.5.2 are solid to BTW.** BTW mobs stand on and path around deepslate, tuff
  and the other new blocks. BTW sand rests on them, BTW torches attach to them, BTW pistons cannot
  move them and mining charges destroy them. Seagrass and kelp count as water (U4, D5).
- **BTW blocks that had no collision in 1.5.2 are walk-through again:** rails, plants, torches,
  campfires, candles, cobwebs (which slow you down), ground cover and hand cranks. Lit campfires and
  stoked fire burn players and Minecraft mobs that touch them, as in 1.5.2. Minecraft water can now
  wash some of these blocks away.
- **Slabs and chests.** Single slabs are half blocks to BTW, as in 1.5.2. BTW sand or gravel on a
  bottom slab can fall. BTW torches and other attachments on a slab's side or on top of a bottom slab
  drop on the next block update; this affects existing builds. Single slabs no longer turn on
  turntables. Chests no longer count as solid blocks for BTW attachments and turntables.
- **Mobs pick up dropped items**, as in 1.5.2. BTW villagers, snow golems, iron golems, dire
  wolves and mobs carrying picked-up loot no longer despawn when you move away.
- **Overworld nether portals** now let BTW possess nearby creatures, as in 4.B.
- **Player name tags.** In multiplayer, other players' name tags are hidden by default: this is BTW's
  "Hardcore" player-names option. Set `fcEnableHardcorePlayerNames=0` in the server's `BTWConfig.txt`
  to show them, or `2` to hide them only behind walls (CTX-5).
- **Projectile damage** from BTW mobs on players is no longer scaled by difficulty, as in 1.5.2.
- **Existing saves.** The first time an existing world loads, torches in inventories may burn out
  and fed chickens may lay at once, because BTW's world clock now runs.
- **Mechanical builds.** More blocks turn on turntables, all in 1.5.2's direction, and the redstone
  clutch, pulley, block dispenser and turntable ignore weak power, such as a redstone block or a
  powered block next to them (see Fixed). Contraptions built on the old behaviour may act
  differently.
- **Farming and forestry follow BTW 4.B** (see Fixed). Crops, saplings and other plants that keep a
  Minecraft id grow several times more slowly than in 1.20.1 (a sapling takes hours, not minutes),
  need light (crops also watered soil) and never grow in the End. Farms built around 1.20.1's rates
  produce much less. Leaves drop only saplings, and unripe crops drop nothing.
- **Bone meal is fertiliser, and newer plants grow at BTW's pace** (see Fixed). Farms that relied on
  bone meal, or on 1.20.1's speed for bamboo, kelp, sweet berries, beetroot or the newer trees, produce
  much less, and in the End only chorus grows. Bone meal still works the 1.20.1 way on blocks that
  BTW's rules do not cover, such as moss, nylium, pink petals, seagrass and sea pickles.
- **Blocks and items that BTW 4.B removed stay removed.**
  - **Removed:** the vanilla hopper, comparator, daylight detector, dropper, trapped chest, weighted
    pressure plates, activator rail, TNT and hopper minecarts and tripwire hook can no longer be
    crafted, are never found in loot and are not in the creative tabs or JEI, and string no longer
    places tripwire. BTW's own hopper, block dispenser, detector block and chest do their jobs, as in
    4.B. Structures place a 4.B block instead: a chest for the woodland mansion's trapped chest,
    repeaters for the ancient city's comparators, nothing for the jungle temple's tripwire trap. TNT
    and hopper minecarts that structures, spawn eggs, dispensers or commands create become plain
    minecarts.
  - **Replaced by BTW's own:** the vanilla crafting table, chest, ladder, torch, snow block, clay block
    and cobweb and the vanilla nether brick item can no longer be crafted and are not in the creative
    tabs. Loot gives the BTW version instead (an unlit finite torch, a block of BTW snow, BTW nether
    bricks from piglins, ...). The copies that structures place stay, and turn into the BTW item when
    broken, as in 4.B. Recipes that need one of them also accept the BTW item: the lantern takes a
    nether-coal torch, chest boats and the shulker box a BTW chest, terracotta BTW unfired clay, red
    nether bricks a BTW nether brick.
  - **Recipe removed:** wooden tools, the stone hoe, the snow layer and the furnace can no longer be
    crafted or found in loot, but stay in the creative tabs, as in 4.B.
  - Removed blocks that are already in a world, or placed with commands, are left alone. Operators can
    still `/give` and `/setblock` them, as in 4.B.
- **Minecraft recipes that BTW 4.B removed or replaced are gone.** All of 4.B's recipe removals now
  apply: 109 crafting recipes, 20 smelting, blasting, smoking and campfire recipes, extending maps and
  repairing two tools in the crafting grid. For example:
  - planks come from chopping logs with an axe or from the saw, bread from baking bread dough, stone
    axes, pickaxes and shovels from sharp stone, and diamond tools and armour from diamond ingots;
  - the enchanting table, the brewing stand and eyes of ender come only from BTW villagers;
  - oak fences come only from BTW's own recipes (six oak planks, or three oak mouldings): 1.20.1's
    planks-and-sticks recipe is removed too, because 4.B replaced the 1.5.2 fence recipe with its own;
  - ores, sand, cobblestone, logs and netherrack no longer cook in a furnace, blast furnace, smoker or
    campfire. BTW's kiln, crucible and other routes take over where 4.B has one.
- **BTW creative tab and JEI follow 4.B.** BTW items that 4.B kept out of its creative inventory,
  though they can be made in survival (Witch Wart, Mail, Steel Nugget, cut and tanned leather, Golden
  Dung and a few more), are no longer listed in the BTW tab, the Search tab or JEI's item list.
  Whether to list them anyway is the maintainer's decision.
- **Block item looks.** BTW blocks that have no item drawing of their own now show 1.5.2's plain cube
  item instead of the placed block's model, as in 4.B.
- **Hunger and healing follow BTW 4.B** (see Fixed). Walking costs hunger, armour weight raises every
  hunger cost, and the hunger costs other mods add now drain BTW's hunger bar.
  `/gamerule naturalRegeneration false` now stops BTW's healing too.
- **Breaking BTW blocks** (see Fixed). Claim and protection mods now apply to BTW blocks. In creative
  mode a BTW block is just removed, with no drops, and with a sword in hand it is not broken at all, as
  with every block in 1.20.1's creative mode.
- **The BTW workbench also makes Minecraft's and other mods' recipes** (see Fixed), after BTW's own.
  Where a 1.20.1 recipe differs from 1.5.2's, the 1.20.1 one applies (see Known issues for the golden
  apples).
- **Plants from other mods follow BTW's rules on BTW soil** (#026). Crops planted on BTW farmland or
  planters need light and watered soil, stop growing while weeds grow (particles at their base show the
  weeds, and right-clicking the crop pulls them), grow twice as fast on fertilised soil, never grow in
  the End, and turn farmland back into loose dirt when they are removed, as BTW's own crops do. Their
  growth chance is BTW's carrot chance, 0.1, spread over the crop's own number of stages (set in
  `btw_plant_rules`). Saplings from other mods grow by BTW's sapling rule (light, four stages, never in
  the End); Nether and End saplings lose their own conditions. A bee visit gives these crops one BTW
  growth chance. Unripe crops on BTW soil that are not BTW's own, beetroot and torchflower crops
  included, now drop nothing, as BTW's unripe crops; ripe crops and the pitcher crop keep their drops.
- **With the optional mods installed** (#027 to #032):
  - older versions are refused at launch, through the version ranges BTW declares to Forge: Dynamic
    Trees before 1.20.1-1.4.10, AppleSkin before 2.5.0, Cold Sweat before 2.4.1, LSO before
    1.20.1-2.4.7, HarvestCraft Crops before 1.0.3 and HarvestCraft Trees before 1.0.2;
  - bone meal on a plant from another mod that stands on BTW ground (grass, dirt, sand, farmland,
    planters) is BTW fertiliser for that ground, as on BTW's own plants: on grass or sand it does
    nothing and is kept. Before, it grew LSO's fern crops, for example;
  - HarvestCraft food, in points of BTW's 60-point bar: Crops and Trees items give 3, or 6 above 4
    nutrition; meals give 6, 9, 15, 24 or 30 by nutrition (up to 4, 8, 16 and 25, and above), BTW's
    hearty stew being the cap. Raw ground beef and pork give 12 and the other raw ground meats 9, all
    with BTW's food poisoning; cooked ground beef and pork give 15 and the others 12, as BTW's raw and
    cooked meats. Meals crafted from raw meat can poison too. HarvestCraft itself makes its seeds
    edible, and the crops band values them at 3 points, where 4.B values its only edible seed, pumpkin
    seeds, at 1. Which value they should have is an open question for the maintainer;
  - HarvestCraft recipes: dough costs 3 flour, as BTW's own dough (a mixing bowl, 3 flour, water and
    salt make 1 dough; it was 2 dough from 1 flour). The grain bread recipe now takes 3 flour and a
    water bucket, and the bakeware bread recipe takes HarvestCraft dough; both make BTW bread dough,
    which bakes in the brick oven. HarvestCraft flour is made from BTW flour, and flour plants are
    milled into BTW flour. The cattail torch makes an unlit BTW torch instead of being removed with the
    vanilla torch. String from fibre is gone: fibre crops give 2 hemp fibres each in the millstone. As a
    side effect, Food Extended's nut butters and cornmeal can now be made at the BTW workbench;
  - HarvestCraft tree fruit and the fruit and pods of Dynamic Trees and its HarvestCraft add-on grow a
    quarter as often, at the pace BTW gives sweet berries, which ripen like cocoa, and never in the End;
    bone meal does nothing on them, as on BTW's cocoa.
- **Network.** The server and every client must run the same build of this port (network protocol 6, and
  the same set of BTW sound events). 1.3.0-alpha.3 clients and servers are refused. The server sends its
  food table to every client, so tooltips and stack sizes agree.
- **Acacia, dark oak, mangrove and cherry are BTW woods** (#034). Their logs chew like BTW's, and they
  have their own bark for tanning (2, 5, 2 and 8 bark per leather), work stumps, saw planks, chopping
  and BTW fuel values; Minecraft's log -> 4 planks recipe is removed for them, as for oak. Their logs
  cost BTW's harvest hunger, and a bare hand no longer takes a whole log.
- **Crimson and warped stems, and other mods' logs, follow BTW's log rule** (#034): they come off whole
  only with an axe, chew without one, and take BTW's saw and stump remover. Stems give no bark and leave
  no work stump; other mods' logs pay oak bark unless a data pack tags them (see Added). The bark and
  stripped forms of BTW's woods (oak wood, stripped logs, ...) dig and chew like BTW logs. An axe strip
  pays bark only for a barked log of another mod; for BTW's woods the chew or the saw pays it.
- **Dynamic Trees** (#027, #034): trees of the four newer woods give their own bark, work stump and saw
  planks; Nether fungi give no bark and leave no work stump; other mods' tree families pay their bark
  tag, or oak bark, and the saw gives the planks their log makes.
- **Leaves of the newer trees and of Dynamic Trees follow BTW's leaf rule** (#035), and so do other
  mods' leaves whose sapling BTW can find: they drop only their sapling or seed, 1 time in 20 (1 in 40
  for jungle-type leaves), with no sticks, no apples from Minecraft's leaves and no Fortune bonus.
  Explosions no longer reduce leaf drops. Mangrove leaves drop propagules.
- **Beds let you sleep** (#037) through the night, with 1.20.1's rules and messages, and they still
  explode in the Nether and the End, whatever you hold; but a bed never sets the spawn point, day or
  night. A bed spawn set by an earlier version is no longer used (see the hardcore respawn below). BTW's
  hostile mobs within 8 blocks keep you awake, as Minecraft's do. The night skip moves BTW's clocks, the
  moon phase and gloom nights (and Serene Seasons' calendar) on, as in 1.20.1. Phantoms stay off. 4.B
  had no sleeping at all; this middle path is the maintainer's ruling.
- **Beds and doors break and drop as in 1.20.1** (#039; see Fixed): one item, a bed keeps its colour,
  and creative breaks drop nothing. BTW still charges its harvest hunger and tool wear, and nearby BTW
  animals still panic.
- **Iron and snow golems can no longer be built** from a pumpkin or jack o'lantern, by hand or by a
  dispenser, as in 4.B (#036). Jack o'lanterns placed from now on go out next to water.
- **The anvil** opens BTW's 3x3 crafting station when clicked, as in 4.B; an empty-handed sneak-click
  opens Minecraft's repair menu (#036).
- **The enchanting table opens** (#036). 4.B kept it and only removed its recipe; the port had blocked
  it.
- **Fence gates** update their neighbours when opened or closed, as in 4.B, and keep 1.20.1's sounds and
  the sculk vibration (#036).
- **Lava meeting water hardens as in 4.B** (#036): a source into obsidian; flowing lava within two
  blocks of its source (four in the Nether) into a drop-less lava pillow; lava flowing down onto water
  into a pillow. Thinner and falling lava stays lava, and water no longer flows into lava, so a lava
  fall beside water stays lava. Cobblestone and stone generators make pillows.
- **Black, brown and blue dye are colours only** (#038), by the maintainer's ruling: the book and quill,
  polished lapis, chocolate, chocolate milk and the librarian's ink sac trade need the ink sac, lapis
  lazuli or cocoa beans, as in 4.B. The three dyes now also dye BTW candles, wool, vases and wool slabs
  in the 2x2 grid, and black dye passes wicker and slat filters like the other dye powders. Where a
  recipe asks for one of the 1.20.1 dyes, the ink sac, lapis lazuli or cocoa beans still stand in for it
  at BTW's grids.
- **With Legendary Survival Overhaul**, LSO's sleep recovery now applies, since beds sleep (#031; not
  checked in a run).
- **Villages are zombified, as in 4.B** (#048). In a survival world, the villagers of every newly
  generated village are BTW villager zombies. They don't burn in daylight and never despawn.
  - Throw a soul urn at one to cure it. A few minutes later it becomes a villager you can trade with, of
    the same trade as its house (a farmer comes out dirty).
  - Villages near the world spawn are abandoned. Within 2250 blocks (9000 on Large Biomes) almost nobody
    lives there, now and then a lone farmer. Between 2250 and 3000 blocks (9000 and 12000), only
    farmers, blacksmiths and butchers.
  - Creative worlds keep villagers. Villages that were already generated are not changed; villages
    placed with `/place structure` follow the same rule.
  - A villager's trade now comes from the job block of its house (a lectern makes a librarian, a brewing
    stand a priest, and so on). It used to be random.
  - By the maintainer's rulings, as in 4.B, new villages have no iron golem and there are no zombie
    sieges. Before, a village from a world you played before this port, or one with another mod's
    villagers, could still be attacked by 20 zombies at midnight.
- **Respawning follows BTW 4.B's hardcore respawn** (#054). Dying no longer brings you back at the world
  spawn: you respawn at a random spot 1000 to 2000 blocks from the world's original spawn. The distance
  grows to 1.5 times once a Nether portal has been lit, 2 times after a Wither has been summoned and 4
  times once an End portal exists, and it is 4 times again on Large Biomes worlds. The spot is never in
  water or lava, and never outside the world border.
  - If you die again within a day of getting that spot, you come back near it, but with half health and
    one food pip less (never below two). Until a Nether portal is lit, players who die within a day of
    each other respawn together. A night you sleep through counts toward that day. In singleplayer, each
    new respawn spot starts the next morning and ends a thunderstorm, as in 4.B.
  - Leaving the End takes you to the original spawn, as in 4.B, and keeps your respawn point for your
    next death.
  - A charged respawn anchor in the Nether, `/spawnpoint` and a bound soulforged steel beacon (see the
    beacon below) still decide where you respawn. A beacon gives 4.B's messages when it is gone, too far
    or blocked, and you can rebind right after dying. A bed spawn set by an earlier version is no longer
    used.
  - By the maintainer's ruling this applies in every game mode, to players switched to Creative or
    Spectator too, except in a world created in Creative mode (one whose default game mode is Creative).
    There you respawn as in Minecraft: at your respawn anchor, `/spawnpoint` or bound beacon, otherwise
    at the world spawn. Changing the world's default game mode (`/defaultgamemode`, or `gamemode` in
    `server.properties`) changes this from the next death.
  - In creative worlds BTW's own creative-world rules now apply, as in 4.B: animals are never
    possessed, BTW mobs drop no arcane scrolls (Minecraft's mobs already did not), possessed cows give
    stillbirths instead of mutant calves, trading does not level villagers up, and rotten flesh no
    longer infects wolves or turns them dire.
  - On an existing survival world, the first death after updating already uses the new respawn.
- **The beacon works as in BTW 4.B** (#053). The block its pyramid is made of decides what it does: gold
  gives haste, diamond Fortune, emerald Looting (mobs that die near it drop more, too), lapis True
  Sight, glowstone night vision, hellfire blocks fire resistance and a ring of fire, iron a strong
  magnetic field, glass nothing (decoration), dung blocks a sickening cloud, and ender blocks draw
  endermen around it (silverfish in the End). A soulforged steel beacon binds your respawn (see above).
  The pyramid must be one kind of block. The beacon menu is gone, and so are its Speed, Resistance, Jump
  Boost, Strength and Regeneration powers: a beacon built before this version gives its BTW effect
  instead of the power chosen in the menu, and a pyramid of netherite or of mixed blocks gives none. A
  beacon's range is a column: it reaches players at any height inside its range. It plays BTW's sounds
  when it powers up or down (a wither's cry); a working beacon still hums and shows its beam.
  - Compasses point at the strongest magnetic field around you: the world spawn, or a powered beacon
    (iron and soulforged steel pull twice as hard). In the Nether and the End they spin unless a beacon
    is near. A lodestone compass still points at its lodestone.
  - Effects from beacons, the mill stone, starving, nearly dying and the gloom show as ambient effects
    (faint particles, a framed icon), as in 4.B.
  - In the End, and above the Nether's ceiling, BTW sees the open sky as 4.B did: beacons work there,
    flowers survive under the open sky, and fishing under the open sky can get bites.
- **Stone gets harder with depth, as in BTW 4.B** (#050). Below y=48 it takes longer to mine and a stone
  pickaxe only cracks it; below y=24, the whole deepslate layer included, only a diamond or soulforged
  steel pickaxe mines it properly, and an iron pickaxe only cracks it and wears it down. Ores in those
  layers are harder too; a pickaxe good enough for the ore but not for the rock knocks the ore loose and
  leaves rough stone. Explosions also break less rock down there. This applies in the Overworld, and to
  existing worlds at once (CF2-PR-DEEP). By the maintainer's rulings depth alone decides: stone you
  place or make deep down is as hard as the rock around it (in 4.B only generated rock was), and
  granite, diorite, andesite, tuff and calcite stay Minecraft blocks, as soft at any depth.
  - Deepslate is BTW's deep stone: it mines like the deepest stone, drops BTW's loose cobblestone,
    stones and gravel, and is a hard surface to walk on, like stone. Cobbled deepslate is BTW
    cobblestone, and it no longer smelts into deepslate. The deepslate ores are BTW ores: iron and gold
    give ore chunks, and they drop no experience. Polished deepslate, deepslate bricks and tiles stay
    decorative Minecraft blocks, found in structures such as ancient cities.
  - Raw iron and raw gold count as BTW's iron and gold ore chunks: they cook into nuggets in BTW's oven
    or a furnace, and no Minecraft recipe turns them, or a deepslate ore, into an ingot. A block of raw
    iron or gold unpacks into nine BTW chunks. Raw ore you put through a BTW container or machine comes
    back as BTW's chunk, and using raw ore on a block places a chunk on it, as BTW's chunks do. Nether
    gold ore no longer smelts into a gold ingot; its drops stay Minecraft's (CF2-PR-ORE).
  - Fortune gives more ore chunks, coal, diamonds, emeralds and lapis from BTW's ores, as in 4.B.
- **Explosion drops follow BTW's rules** (#049; see Fixed). By the maintainer's ruling Minecraft's
  explosion drop gamerules decide for BTW blocks too, and new worlds start with `tntExplosionDropDecay`
  true: TNT drops one in four of the blocks it breaks (4.B's rule: the bigger the explosion, the fewer
  blocks it drops). Use `/gamerule tntExplosionDropDecay false` for Minecraft's rule. Existing worlds
  keep their setting; use `/gamerule tntExplosionDropDecay true` there for 4.B's rule. Ores broken by an
  explosion you set off no longer drop experience (BTW gives no experience for breaking blocks), and the
  items an explosion drops lie in stacks of up to 16, as in Minecraft.
- **Acacia, dark oak, mangrove and cherry planks follow BTW's plank rule** (#051): break them with an
  iron axe or better to keep them; anything else leaves sawdust. Their stairs now make 6 from 6 planks
  (or 1 from 3 mouldings), two of their slabs make a plank again, and the saw cuts their stairs and
  slabs. By the maintainer's ruling their Minecraft trapdoors stay craftable, so six of their planks
  make trapdoors, not BTW fences; their BTW fences come from three mouldings.
- **Bamboo blocks no longer craft into planks** (#052). By the maintainer's ruling bamboo follows BTW's
  plank rule: saw a bamboo block (2 bamboo planks and a sawdust), or chop it with a BTW axe at a
  crafting grid (iron or better: 1 plank and 1 sawdust; a wooden, stone or gold axe: 1 stick and 2
  sawdust). Bamboo blocks still break by hand and give no bark.
- **Cornflowers and lilies of the valley follow BTW's flower rules** (#045), like BTW's rose: they need
  light or open sky and BTW's soils, and they pop otherwise. Wither roses and torchflowers keep
  Minecraft's rules. The red tulip makes red dye at a crafting grid, as in Minecraft 1.20.1 (BTW read it
  as a poppy, whose dye recipe 4.B removed). Villagers who want a particular kind of log take only that
  kind, as in 4.B.
- **What counts as a solid top follows BTW 4.B** (#061). Before BTW lets a redstone torch, a pressure
  plate, a rail, snow, ash or fire rest on a block, it asks whether the block's top is solid. Minecraft
  answered that; BTW's own rule answers it now, as in 4.B. Glass, glowstone, ice, TNT, pistons, redstone
  lamps, beacons, the top half of a closed oak trapdoor, a full block of snow layers and BTW blocks that
  4.B gives no solid top (light bulbs, the BTW hopper, mill stone, cauldron, crucible, bellows and
  others) no longer count, though glass still holds a redstone torch, as in 4.B; top slabs,
  upside-down stairs and solid blocks such as stone still count. Besides the attachments under Fixed:
  - fire on top of such a block goes out at its next tick unless something next to it can burn;
  - mushrooms can no longer be planted on, spread onto or stay on glass, ice, TNT, pistons, the top half
    of a closed oak trapdoor or a BTW machine, stems grow no pumpkin or melon on them, and nether growth
    does not spread onto them;
  - a mining charge on top of a block stays stuck only where the top is solid (a top slab, not glass);
  - endermen and silverfish drawn by an ender block beacon appear only on a solid top, and a bound
    beacon respawns you only on one;
  - a squid letting go of your head, and a tamed wolf or ocelot teleporting to you, land only on a solid
    top.

  The weighted pressure plates, the activator rail and the trapped chest, which 4.B removed, now count
  as solid on top (see Known issues).
- **Existing builds: attachments on blocks 4.B does not count as solid** (#040, #055, #061, #062). BTW
  torches and other BTW attachments on glowstone, redstone lamps, doors, beds or BTW panes, and torches,
  soul torches and ladders on the blocks listed under Fixed, drop at the next block change beside them.
  So do standing redstone torches, snow, pressure plates and rails on the tops 4.B does not count as
  solid (see above), pressure plates on Minecraft's newer fences, walls and stained glass panes,
  torches, BTW candles and placed ore chunks on stained glass panes, and iron spikes on the arms of the
  newer fences and walls. BTW grates, wicker panes and slats stop joining doors, beds and other kinds of
  pane.
- **You can aim at a falling block**, as in 4.B and Minecraft (#043).
- **Other mods' block breakers.** Machines and fake players from other mods that break BTW stone, ores
  or logs get BTW's conversion drops on the side facing them (they came out on top) (#044).
- CurseForge description:
  - removed the "field manual" instruction;
  - filled in the issue tracker;
  - marked the wiki and original-BTW links as maintainer TODOs;
  - described the actual JEI coverage;
  - added a "Known Differences from BTW 4.B" section, updated for this release (CTX-4);
  - updated it on 2026-10-21: the Known Differences cover beds, the hardcore respawn, door, trapdoor and
    fence-gate redstone, the newer woods, the 1.20.1 dyes and the anvil's repair menu; the Dynamic Trees
    notes follow the new log and leaf rules; "Sleep costs you" and the "Hardcore spawn, costly sleep,
    FC's death penalties" line are gone, since neither 4.B nor the port has costly sleep and the
    hardcore respawn was not ported then;
  - updated it on 2026-10-28: the Known Differences now cover the zombified villages, the hardcore
    respawn, the beacon, the stone strata with deepslate and raw ore, explosions, larger Nether portals,
    soul torches, creative players and falling blocks saved in mid-air;
  - updated it on 2026-11-03: the Known Differences now cover where torches, ladders, snow, pressure
    plates and rails can go, and Minecraft's newer fences, walls, stained glass panes and pressure
    plates following their BTW relatives, with the blocks that do not yet.

### Added
- JEI: soulforge and campfire recipe categories, FC's special crafting recipes in the crafting
  category, and information pages for the kiln, saw, turntable, hopper filters and souls,
  hibachi/stoked fire, cauldron/crucible fire rules and campfire cooking (CTX-6).
- FC's login handshake runs again (CTX-5):
  - single-player worlds show "BTW V4.B0000003" on joining;
  - servers show FC's connect line, "BTW version check successful." (or BTW's version-mismatch
    warning) and the "Hardcore Modes" line, and then apply the server's player-names option.

  A one-time note per port version points to the issue tracker.
- BTW sidings, mouldings, corners, benches, fences, columns, pedestals and tables of acacia, dark oak,
  mangrove and cherry (#051). Their planks go through the saw like oak planks, and the saw keeps cutting
  each piece down (planks, sidings, mouldings, corners, gears). As in 4.B, they are not in a creative
  tab.
- BTW's own particles as particle types: `betterthanwolves:white_smoke`, `white_cloud`, `small_flame`,
  `cinders` and `snow_shovel`, ports of 4.B's (#042).
- The beacon's 4.B effects as status effects: `betterthanwolves:fortune`, `looting` and `true_sight`
  (#053).
- The damage type `betterthanwolves:mob_attack_unblockable` (a BTW squid's bite, which ignores armour),
  in `#minecraft:bypasses_armor` (#047).
- BTW iron golem (`fc_iron_golem`) and silverfish (`fc_silverfish`) entity types, with their 1.5.2
  models. Before, they borrowed the vanilla villager and zombie types and turned into BTW villagers
  and zombies after a reload.
- Worlds save `data/btw_headless_items.dat`, the ids of items added after 1.5.2, so BTW containers
  keep those items across restarts (RE5).
- BTW's 1.5.2 sounds as 508 sound events, one per 1.5.2 sound name, playing the 1.5.2 sound files
  that Minecraft 1.20.1 still ships, with a 1.20.1 subtitle where one fits. They can be played with
  `/playsound`, for example `/playsound betterthanwolves:mob.zombiepig.zpig master @s`.
- Optional support for six mods (#027 to #032). None is bundled in the jar or required, and BTW plays
  the same without them. Each integration starts only when its mod is installed, and switches itself off
  with a log line, instead of crashing, if its mod throws an error or has changed; the one exception is
  the small fix that keeps Serene Seasons from eating bone meal, which silently does nothing if Serene
  Seasons changes that code (#028). Tested versions are in brackets.
  - **Dynamic Trees** (1.20.1-1.4.10 and 1.4.11; #027): living trees follow BTW's log rules. Without an
    axe that fells BTW logs, each hit chews the trunk (bark first, then sticks and sawdust) and a
    chewed-through tree drops no logs; an axe fell costs BTW's hunger and 1 durability per log, and
    Fortune's bonus logs are kept. The trunk base is a BTW stump: the iron chisel turns a dead one into
    a work stump, and the stump remover clears it. The BTW saw fells a full-width trunk: the cut block
    gives BTW's sawn planks, sawdust and bark, and the rest of the tree falls. Trees of oak, spruce,
    birch, jungle and, since 2026-10-20, acacia, dark oak, mangrove and cherry give their own bark, work
    stump and planks; Nether fungi give no bark and leave no work stump, and other families pay their
    bark tag, or oak bark, and give the planks their log makes (#034). Bone meal only fertilises the
    tree's soil, from either hand. Saplings grow by BTW's sapling rule, and Dynamic Trees' Nether fungi
    grow 1 time in 50 as BTW's mushrooms do, which Dynamic Trees itself never does. Since 2026-10-20 its
    leaves drop by BTW's leaf rule: the species' seed 1 time in 20 (1 in 40 for jungle-type trees), no
    sticks and no Fortune bonus, with Dynamic Trees' seasonal seed chance still applied (#035).
  - **Serene Seasons** (9.1.0.3; #028): seasons reach BTW's own crops. BTW wheat grows in summer and
    autumn, hemp in spring and summer; out of season Serene Seasons' setting applies (no growth, one
    growing day in six, or the crop is destroyed). A lit BTW light bulb counts as greenhouse glass for
    the crops below it. The season's temperature also drives BTW's snow, ice and farmland rules, and
    bone meal stays BTW fertiliser out of season instead of being used up by Serene Seasons. Its
    calendar and season sensor can be made at the BTW workbench.
  - **AppleSkin** (2.5.1, client only; #029): AppleSkin shows BTW's food model. Tooltips give a food's
    points on BTW's 60-point bar, the fat it adds, its food-poisoning chance and the penalty it changes;
    the held-food preview and the exhaustion underlay sit on BTW's food row; the health hint shows what
    a meal heals by BTW's rules; the F3 line reads BTW's food, fat and exhaustion. Foods AppleSkin
    cannot see, such as pumpkin seeds and milk, get the tooltip too.
  - **Cold Sweat** (2.4.3.2; #030): BTW's fires (from 6 °F for embers to 35 °F for a stoked fire), the
    hibachi, the brick oven, BTW's torches and candles, smouldering logs, burning ladders and
    lava-cracked loose stone warm the player, and so do Minecraft's lit candles, candle cakes and
    lantern, as BTW's candle and torch. BTW's wool, padded, tanned and gimp clothing insulates, BTW's
    cloth and leather work as Cold Sweat sewing insulation, BTW's soups and mushroom stew warm over
    time, and BTW's fuels burn in Cold Sweat's hearth and boiler at their BTW value against coal.
    Freezing costs BTW hunger (up to 0.05 exhaustion a second, and BTW burns fat before food), but never
    during Cold Sweat's Grace or in peaceful mode.
  - **Legendary Survival Overhaul** (1.20.1-2.4.7; #031): LSO data for BTW's heat sources, clothing
    (cold resistance, heat resistance for plate, limb protection for padded and plate), hot meals,
    drinks and heater fuels; burning torches warm when held, and BTW fat gives up to 2 points of cold
    resistance. LSO's Cold Hunger tints BTW's food pips frost blue instead of drawing a second food bar.
    With LSO's default settings LSO's own healing replaces BTW's 30-second heal and is paid in BTW
    hunger, but heals only above 24 of 60 food, as BTW does.
  - **Pam's HarvestCraft 2** (Crops 1.0.3, Trees 1.0.2, Food Core 1.0.5, Food Extended 1.0.1, and the
    Dynamic Trees and Serene Seasons add-ons for it; #032): HarvestCraft runs on BTW's farming and food.
    Crops and saplings follow BTW's soil, growth and sapling rules, meals feed on BTW's scale (see
    Changed), seeds, grains and vegetables feed BTW's animals, flour plants and fibre crops go through
    the millstone, gardens spread with BTW dung instead of bone meal, and tree fruit ripens at BTW's
    cocoa pace. HarvestCraft items that BTW's rules govern say so in their tooltip.
- General rules that data packs and other mods can use (#020, #022 to #028, #032, #034, #035; formats in
  `BRIDGE_SPEC.md`):
  - `btw_foods` data gives any item a BTW food value, animal-food value, poisoning chance or wolf-meat
    flag, by item or tag, with Forge load conditions, and `tiers` value a whole mod's foods by nutrition
    band;
  - `btw_recipe_replacements` replaces or removes another mod's recipe by id, and `btw_machine_recipes`
    adds millstone recipes for an item tag;
  - `btw_plant_rules` sets the growth chance of modded crops on BTW soil;
  - the block tags `#betterthanwolves:fc_soil_plants` (a modded wild plant follows BTW's wild-plant
    rules), `#betterthanwolves:dung_spreads` (BTW dung spreads it) and
    `#betterthanwolves:paced_like_sweet_berries` (it grows at BTW's sweet-berry pace, never in the End);
  - BTW's own wheat and hemp post Forge's crop-growth events, so season mods can stop or allow their
    growth;
  - code hooks for integrations: other mods' saplings and fungi can grow by BTW's sapling or mushroom
    rule (`TwinRegistry.registerModdedSapling`), modded blocks can answer BTW's saw, stump remover and
    fertiliser (`ModernBlockHooks`), season and climate mods can drive BTW's crops, snow and ice
    (`SeasonGate`, `BTWClimate`), and client mods can draw on BTW's food row (`FoodHudExtension`);
  - for temperature mods, BTW's heat sources now include lava-cracked loose cobblestone and stone brick,
    which 4.B counts as warm, and Minecraft's torch, lantern, candles and candle cakes by their nearest
    BTW relative;
  - the block tags `#betterthanwolves:bark/<wood>` (`oak`, `spruce`, `birch`, `jungle`, `blood_wood`,
    `acacia`, `dark_oak`, `mangrove`, `cherry`) and `#betterthanwolves:bark/none` decide which bark a
    log BTW has no wood type for pays, or that it pays none; an untagged log pays oak bark (#034);
  - an integration can decide what its leaves drop under BTW's leaf rule with `LeafDrops.register`,
    using Minecraft types only (#035);
  - `betterthanwolves:modern_log_chopping` recipes give a log BTW has no item for BTW's two plank
    routes, the saw and an axe at a crafting grid, with their yields; BTW ships one for bamboo blocks
    (#052).
- Development tooling:
  - `tools/LinkAudit.java` checks the class precedence of the built jar and reports Minecraft class
    references in frozen BTW code;
  - a `jar` task guard fails the build on precedence drift;
  - `tools/BridgeAudit.java`'s texture check understands the `%blur%`/`%clamp%` prefixes and the
    entity renderer's vanilla fallbacks; its aux-effect check now follows BTW effects to BTW's own
    client handler and fails if that forwarding chain breaks;
  - `:Forge:generateLegacySoundsJson` (run by hand) builds `sounds.json` from the 1.5.2 and 1.20.1
    asset indexes, and `:Forge:generateBtwTextureAnimations` (part of every build) turns BTW's 1.5.2
    texture animation files into `.png.mcmeta` files;
  - `-PbtwDevMods=<tokens>` puts optional mods on the development runtime (`dynamictrees`,
    `dynamictrees-latest`, `sereneseasons`, `appleskin`, `coldsweat`, `lso`, `harvestcraft`, `dtphc2`,
    `ss-hc2crops`); the build stops on an unknown token, on both Dynamic Trees tokens at once, and on an
    add-on without the mods it needs;
  - two more `jar` task guards: every class in the jar must be BTW's own (no bundled mod, no nested
    jar), and `betterthanwolves.mixins.json` must list exactly the mixin classes in the jar;
  - the start-up self-audit also checks BTW's table of heat sources, clothing, meals and fuels
    (`[SELF-AUDIT/thermal]`) and the BTW ids in the optional-mod data files against the server's BTW ID
    config (`[SELF-AUDIT/compat-refs]`);
  - development runs load a filtered copy of Modern-Common (`:Forge:syncDevModernCommon`, built from the
    jar's own shadow list through one copySpec) instead of deleting classes from Modern-Common's build
    output, and BTW's bytecode passes run inside `extractFcClasses` (2026-10-19). A build with unchanged
    sources rewrites no class directory that a running server loads, which ends the
    `NoClassDefFoundError` crash of a development server when a client is built beside it, and the
    "cannot access EntityLiving" compile failure. `removeModernCommonShadowedClasses`,
    `openUpEntityAccess`, `widenFcHookDescriptors`, `extendFcPathingBuildRange` and
    `extendFcTreeBuildRange` are no longer tasks, so an `-x` list that names them fails; LinkAudit gains
    a DEV FILTER CHECK;
  - `deriveModernWoodTextures` (`Forge/gradle/modern-woods.gradle`, part of every build) derives the
    four newer woods' work-stump textures from BTW's work-stump overlay and the 1.20.1 logs, and
    rewrites a file only when its pixels and bytes differ; `-PbtwClientExtraJar=<path>` names the 1.20.1
    client jar it reads, and a missing input only warns;
  - the start-up self-audit also checks the four newer woods at legacy id 165 (`[SELF-AUDIT/wood]`), the
    beds and doors that take 1.20.1 drops (`[SELF-AUDIT/pairdrops]`) and which code owns each block hook
    (`[SELF-AUDIT/hook]`);
  - the start-up self-audit also checks BTW's particle names (`[SELF-AUDIT/particles]`), the dig face
    (`[SELF-AUDIT/digface]`), the blocks that keep their own kind (`[SELF-AUDIT/identity]`), the
    creative-target rule and the squid's damage type (`[SELF-AUDIT/targets]`, `[SELF-AUDIT/damage]`),
    explosions (`[SELF-AUDIT/blast]`), the strata (`[SELF-AUDIT/strata]`), the beacon
    (`[SELF-AUDIT/fcbe]`), the hardcore respawn (`[SELF-AUDIT/hcspawn]`), BTW's view of Minecraft's
    entities (`[SELF-AUDIT/views]`) and BTW's detector rails (`[SELF-AUDIT/rails]`);
  - the start-up self-audit also counts the Minecraft fences, walls and stained glass panes that answer
    as BTW's oak fence, cobblestone wall and glass pane, and the blocks that take another block's
    attachment rule (`Support relatives: ...`), and warns about a Minecraft fence, wall or stained glass
    pane that is neither a BTW block nor given a relative, and about an entry whose BTW block is missing
    (#062);
  - `composeLegacyIconTextures` (`Forge/gradle/legacy-icon-textures.gradle`, part of every build)
    composes the cross texture of platform-lifted redstone wire from 1.20.1's own dust textures (no
    1.5.2 art), and `deriveModernWoodTextures` now also derives the four newer woods' column sides
    (`8 textures checked`).

### Known issues
- **Mostly not checked on a client yet.** A dedicated server has been run on a fresh world three times
  (twice on 2026-10-10, once after the 2026-10-11 wave): it boots with no errors, BTW initialises and
  vanilla mobs are replaced. For the 2026-10-14 wave it ran three more times with scripted in-world
  tests of BTW's growth, fire and falling-block rules, and for the first 2026-10-15 wave with scripted
  tests of bone meal, the newer plants and the removed blocks. The maintainer has played development
  builds, which is how the 2026-10-11 bugs were found, and the last session ran a build from the middle
  of that wave (its log shows the BTW tab built with 355 entries), but none of the in-game checks has
  been run on the final code. Every rendering fix (red tint, mob renderers and animations, held items,
  chests and baskets, block and item models, animated textures), every client-side sound, the creative
  tab and JEI are checked by code review only, and so is how BTW blocks draw their faces next to other
  blocks now that BTW's table of opaque blocks is filled (2026-10-14). So are the arm swing when bone
  meal is used, bone meal from the off hand, and the removed items' absence from the vanilla creative
  tabs and JEI (2026-10-15). A development client has checked only the food bar's place in the HUD
  (2026-10-15), the AppleSkin, Cold Sweat and LSO displays (2026-10-17), hold-to-craft (2026-10-18), and
  from 2026-10-19 to 2026-10-21 the item count after placing a block (also from a ladder and after
  death), windmill dyeing, sleeping through the night, the hand after a refused placement, and the newer
  woods' textures and dig speed. From 2026-10-25 to 2026-10-28 development clients also checked the
  breaking crack, BTW's particles, torch and ladder placement, falling blocks, drops from a dug block,
  creative players and squid head crabs, the depth dig speed, the newer woods' decorative blocks, the
  beacon, the hardcore respawn, the restored textures (the wicker pane, huge mushrooms, hopper filters,
  blocks held by mobs) and a player on the soulforged steel plate and detector rail, and on 2026-11-03
  torches, redstone torches and pressure plates placed while sneaking on the newer fences, walls and
  stained glass panes. No client has been run from a launcher install with the release jar, and no
  release jar has been booted at all for the work of 2026-10-19 to 2026-11-03. Multiplayer is untested
  too.
- **Re-entering a world.** No cause was found. It has not been tested whether a world now always
  shuts down cleanly and releases its `session.lock`. If a world will not open again, please report
  it with `logs/latest.log` and any crash report.
- **BTW villagers and witches make no idle, hurt or death sounds** (witches still idle with BTW's
  quiet ghast scream). This is expected: it is how 4.B sounds, because Minecraft 1.5.2 had no
  villager or witch sound files. 1.3.0-alpha.3 played 1.20.1's witch hurt and death sounds.
- **The vanilla creative tabs still list a few Minecraft items that 4.B hid**, such as music discs
  other than "13", enchanted books and wheat seeds. Hiding them is waiting for the maintainer's
  decision (CT6). The blocks and items that 4.B removed or replaced (the vanilla hopper, comparator,
  chest, crafting table, cobweb, ladder, ...) are now hidden from the tabs (see Changed).
- **BTW's own placement code is skipped in some cases.** When you place a block item while sneaking,
  from the off hand, or against a block added after 1.5.2 (tuff, copper ore, ...) or a vanilla block
  with its own right-click action, Minecraft's placement runs with BTW's facing and variant, but without
  BTW's item-specific placement code: slabs do not join into a double slab, saplings ignore BTW's soil
  rules, a burning torch does not light the block you click, and BTW's "cannot be placed here" checks
  are not made (MISSED-1). The support rules of torches, ladders, snow, ash, pressure plates and rails
  apply either way (#055). Placing against any other block uses BTW's code, with 4.B's placement checks
  since 2026-10-21.
- **The enchanted golden apple cannot be crafted, and the golden apple costs 8 gold ingots.** In 4.B, 8
  gold nuggets make a golden apple and 8 gold blocks an enchanted one. The BTW workbench makes these
  Minecraft recipes in their 1.20.1 form, which has no enchanted golden apple. A 4.B recipe for it is a
  planned follow-up.
- Dead bushes on grass blocks or dirt (1.20.1 places some in swamps and giant taigas) are removed when
  a block next to them changes, because BTW lets dead bushes grow only on sand. Dead bushes on sand,
  red sand and terracotta stay.
- Waterlogged blocks (stairs, slabs, fences, ...) do not count as water for BTW: sugar cane whose only
  water is in waterlogged blocks breaks when a block next to it changes, and BTW farmland watered only
  by them dries out.
- JEI's recipe and usage lookups (R and U) treat all variants of an item as one: looking up red knit
  wool shows the recipes of every knit wool colour (CI-3). The item list itself shows each variant.
- The BTW chest's item shows as a planks-textured cube, and a few other BTW block items whose 1.5.2
  item drawing is not ported yet show a plain cube.
- Chunk rebuilds in large BTW builds may be slower: many BTW building blocks (decorative blocks,
  slabs, sidings, mouldings, stairs) are now drawn from their live neighbours every time their chunk
  section is rebuilt. This has not been measured (RP-R2).
- Some mob animations and effects are still missing: squid tentacle movement, the animations of the
  cow's kick and the squid's tentacle attack (both fling you since 2026-10-28), and villager and witch
  particles (R8). Falling smouldering logs and burning netherrack give off no smoke while they fall:
  particles of BTW's own moving objects are not drawn yet.
- BTW's Hardcore Buoy option (`fcEnableHardcoreBuoy`) has no effect yet: dropped items float, and are
  pushed out of blocks they are stuck in, as in Minecraft 1.20.1. The "Obstructed" player-names option
  hides only players' name tags behind walls, not named mobs'.
- BTW mobs press pressure plates (since 2026-10-28), but they do not trigger tripwire, do not travel
  through nether or end portals, and are not slowed by soul sand. Moving piston blocks are solid cubes
  to them (SW-M1).
- The maintainer's rulings give Minecraft's newer pressure plates, fences, walls and stained glass panes
  their BTW relative's support rules (#055, #062). The blocks below are not ruled yet and still answer
  by their Minecraft shape, unlike their nearest BTW block (FU-REL-FAMILIES):
  - fence gates other than oak: torches, soul torches and BTW candles stand on a closed one and pressure
    plates go on it, where the oak fence gate refuses them; a redstone torch placed on a closed one
    leaves your hand one short while the server refuses it;
  - stained and tinted glass: pressure plates, snow, rails, ash and stakes go on top, and wall torches
    and ladders hang on the sides, where glass refuses them all;
  - packed and blue ice: pressure plates, rails, stakes and a standing redstone torch go on top (and
    snow on blue ice), where ice refuses them;
  - trapdoors other than oak, the iron one included: a closed one in the top half holds torches,
    redstone torches, pressure plates, snow, rails, ash, stakes and BTW candles, and an open one holds
    wall torches and ladders, where the oak trapdoor holds none of them;
  - acacia, dark oak, azalea, flowering azalea, mangrove and cherry leaves: BTW torches and candles
    cannot stand on them and BTW's snow and ash do not rest on them, as they do on oak leaves;
  - wooden doors other than oak: wall torches and ladders hang on a closed one, not on the oak door.
- BTW's ash above Minecraft's newer fences and walls is drawn on top of the post; above BTW's oak fence
  and cobblestone wall it is drawn on the ground below, as in 4.B. By the maintainer's ruling this waits
  for a follow-up (FU-REL-DRAW; #062).
- Clicked without sneaking onto a block whose top 4.B does not count as solid, a powered rail is placed
  and drops at once as an item, where 4.B refuses the click (FU-RAIL27; #055).
- Snow that world generation lays on glass, TNT or a redstone lamp open to the sky in a snowy biome
  (rare) is not refused; it goes when a block next to it changes (FU-WGSNOW; #055).
- The tables in taiga and savanna village houses, a pressure plate on a fence, lose their plate when a
  block next to it changes, as the oak tables of plains villages do: BTW's rule lets no plate stand on
  a fence (#055, #062). A village placed with `/place structure` loses them at once.
- A full block of snow layers (eight) is not a solid top: 4.B has no eight-layer snow, and its snow
  cover holds nothing. A redstone torch, a pressure plate, a rail or another snow layer cannot be placed
  on it (#055, #061).
- The weighted pressure plates and the activator rail, which 4.B removed, have no BTW support rule:
  placed with commands, they keep Minecraft's. BTW counts them and the trapped chest as solid blocks,
  so BTW's own placement can stand a redstone torch, a pressure plate or a rail on one where Minecraft
  would refuse it, and snow and fire can rest on one (#055, #061).
- An attachment the server refuses on or next to some of Minecraft's newer blocks that have no BTW
  relative can leave your hand one short until the next inventory update: for example a redstone torch
  on a bell, a chain, a lightning rod or pointed dripstone, or a BTW torch or ladder placed while
  sneaking beside such a block, such as a BTW torch on a stained glass pane next to one (#055, #062).
- A squid on your head is drawn as a free squid, a little behind you when you move fast (#047).
- In creative, a wild wolf you hit or feed wolf meat still turns hostile (to survival players), animals
  still panic and endermen still scream, as in 4.B: by the maintainer's ruling only the targeting
  changes (#046).
- A block that falls for more than about 5 seconds and ends its fall below y=1 drops as an item. This is
  4.B's rule for blocks falling into the void, which 1.20.1's deeper world can reach (#043).
- Lightning does not charge BTW creepers or turn BTW pigs into zombie pigmen: Minecraft's lightning
  strikes are not passed to BTW mobs (HAI-M1).
- The vanilla bow fires Minecraft's arrows and draws with arrows from anywhere in the inventory. 4.B
  fired BTW arrows, from the hotbar only. BTW's arrow code runs only for rotten arrows and for the
  composite bow.
- When BTW stores or places items that 1.20.1 split into separate items, some still come back as their
  1.5.2 equivalent: smooth sandstone becomes cut sandstone; stone slabs become smooth stone slabs; wet
  sponges become dry sponges; carved pumpkins become pumpkins; coloured beds held by BTW come back red
  (a broken bed keeps its colour). At the BTW workbench, smooth sandstone slabs and stairs and stone
  slabs come out as cut sandstone slabs, sandstone stairs and smooth stone slabs, and a dyed bed comes
  out red. Other split items, such as flowers, wool colours and wooden slabs, keep their kind since
  2026-10-28 (#045).
- BTW's own items can only be used from the main hand.
- Minecraft water can wash away BTW vine traps and placed buckets, which 1.5.2 protected.
- Right-click order (#033): a few blocks that have their own 1.20.1 right-click act before the held BTW
  item: redstone ore, an empty flower pot, signs, a note block clicked with a tuning fork, and TNT
  (flint and steel takes 1 durability when it primes TNT, and a fire charge primes it at once). This is
  accepted for now; a review of such blocks is next. After a BTW item is used on a block, the same click
  can also run the item's use-in-the-air action, which 4.B never did. When a BTW item declines a click,
  Minecraft's own use of the item can still run: flint and steel lights a fire while you are in mid-air,
  where 4.B refuses. With an empty main hand, a sneak-click with an item in the off hand does not
  activate the block (1.20.1's rule; 4.B activates it).
- Placement (#040): stairs placed without sneaking against a block BTW handles (oak stairs on stone, for
  example) face the wrong way round, and oak trapdoors placed that way always sit in the lower half and
  face by your look, not by the side you clicked. A tripwire hook placed through BTW's code shows as
  attached only after a block next to it changes. Placements through BTW's code post no block-place
  event, so other mods that watch placements do not see them. Other mods' block placers that act as a
  fake player can be refused against blocks BTW handles, because 4.B's mid-air rule sees them as
  airborne (Create's deployer is not affected); the maintainer ruled on 2026-10-20 to keep the rule for
  them.
- A door whose top half burns first may drop itself (1.20.1; 4.B dropped nothing).
- Dyes stored in BTW containers before this update read back as the 4.B items: white dye as bone meal,
  and black, brown and blue dye as the ink sac, cocoa beans and lapis lazuli. A world opened again in an
  older build does not read the dyes that this build stored correctly.
- Spruce, birch and jungle trapdoors cannot be crafted: BTW's fence recipe takes the same grid (BTW's
  own trapdoor recipe works with any planks). Crimson and warped stems chew into BTW's chewed log, which
  looks like oak, as every chewed log does in 4.B. Acacia logs stored by BTW before this update do not
  stack with new ones until they pass through a Minecraft inventory. The newer woods' work stumps and
  column sides use textures made from Minecraft's at build time, so resource packs do not change them
  (their bark items do follow resource packs).
- Right-clicking a note block, furnace or ender chest runs Minecraft's own action, and breaking an ender
  chest runs Minecraft's own break, until BTW's block entities are bridged. So ender chests open your
  personal ender inventory, as in Minecraft. By the maintainer's ruling, 4.B's ender chest tiers, which
  depend on an ender block beacon, are queued as their own update.
- The beacon (#053): True Sight shows no spawn particles yet. Fortune from a beacon does not reach
  blocks BTW does not know, such as copper ore. Other players' compasses, and compasses in item frames,
  point at the world spawn.
- The hardcore respawn (#054): BTW's day is Minecraft's day clock, so with the daylight cycle turned off
  the one-day windows never end. On very slow world generation a respawn can fall back to the world
  spawn (servers can raise `-Dbtw.hardcoreRespawn.budgetMs`, 3000 by default). 4.B's mystery-meat drop
  on a player's death is not ported. The singleplayer morning skip is not covered by the scripted tests,
  which run on a dedicated server.
- Explosions (#049): TNT set off by another explosion goes off as Minecraft TNT, because BTW's own lit
  TNT is not ported yet. Chests, furnaces and other blocks that hold contents, beds and doors, and the
  1.20.1 flower and stone variants keep Minecraft's explosion rule. Blasted infested stone still
  releases silverfish, and blasted nether quartz ore still gives experience.
- The strata (#050): deeper stone and ores look the same as surface stone; 4.B drew them darker, and a
  texture update is queued. Other mods' recipes can still turn raw iron and raw gold into ingots: only
  Minecraft's are removed.
- BTW's own code learns of a `/difficulty` change only at the next player tick, and not at all while no
  player is online; until then BTW's mobs go by the old difficulty.
- Hunger costs that still differ from 4.B:
  - food poisoning (the hunger effect) drains a fifth of 4.B's hunger: 1.20.1 charges 0.005 exhaustion
    per tick and level, 1.5.2 charged 0.025;
  - breaking a block added after 1.5.2 costs 0.005, a fifth of 4.B's 0.025. That includes the bark and
    stripped forms of BTW's woods, crimson and warped stems and other mods' logs. BTW's own blocks, the
    four newer woods' logs (since 2026-10-20), deepslate and the deepslate ores (since 2026-10-28) and
    beds and doors (which now break as in 1.20.1) cost 4.B's amount;
  - a jump onto a ladder costs a jump's hunger, which 4.B did not charge;
  - with only your feet in water, BTW blocks already mine at the under-water speed (4.B slowed mining
    only with your head under water).
- With the optional mods:
  - Dynamic Trees: a thick trunk (radius 9 to 24, wider than one block) loses one radius per chewing
    hit, so a 3x3 trunk takes about 9 hits where BTW's logs would take 45, and a chewed trunk can grow
    thick again. The stump remover and the chisel work only on a dead stump: on a living tree the
    remover would delete the whole tree without drops. Tree families from other mods pay oak bark unless
    a data pack puts their log in a `#betterthanwolves:bark/<wood>` tag. Dynamic Trees' growth potions,
    and fertilisers other than bone meal, keep their own effects. Its `replaceVanillaSapling` option is
    not supported: BTW grows oak, spruce, birch and jungle saplings with its own tree code, which never
    tells Dynamic Trees that a sapling grew.
  - Serene Seasons: with 9.3.0.27, Cold Sweat switches its own season temperatures off (it takes Serene
    Seasons 9.1.0.0 to 9.3.0.26 only), so use 9.1.0.3 with Cold Sweat. With the older 9.0.0.46 line,
    BTW's season tags are not used: that version's own rule gates every crop by its block name. In
    spring Serene Seasons melts ice into water sources, ice a player placed included. Blocks that BTW
    draws itself with a grass tint stay summer green.
  - AppleSkin: other mods that listen to AppleSkin see BTW food in AppleSkin's units (a third of BTW's
    points, rounded up) and a saturation made from BTW's fat value. The health hint assumes BTW's
    healing is on, because the client is not told the `naturalRegeneration` rule. Numbers use the
    system's decimal separator, as AppleSkin's own do.
  - Cold Sweat: its cold and heat damage sets off blasting oil you carry, as any damage does in BTW. 4.B
    has no recipe for wool boots, so only three of the four wool pieces can be made in survival.
    Waterskins do not warm on BTW's campfire spit; Cold Sweat's boiler, which burns BTW fuels, heats
    them.
  - Cold Sweat and LSO together both simulate body temperature from the same BTW sources, so their
    effects stack, and BTW logs one warning. Recommended: let Cold Sweat own temperature and set LSO's
    "Temperature Enabled" to false (LSO's thirst, limbs and health keep working).
  - LSO: drinking water by hand still works while you are food-poisoned; only drink items are refused.
  - HarvestCraft and Dynamic Trees log a few errors about their own data at start-up (for HarvestCraft:
    the melon pie, honey muffin and caramel cupcake recipes, and its fern, grass and tall-grass drops).
    They come from those mods, not from BTW.
- Minor visual and sound gaps:
  - the composite bow shows no draw animation;
  - placed tools show their 1.20.1 texture;
  - the mining charge's flash is not drawn;
  - mobs picking up items show no animation;
  - block-shaped items cooking on a campfire or in a brick oven are drawn turned a quarter turn;
  - BTW sounds pitched above 2.0 (the wet cement's moan, fizzing) are capped at 2.0 by 1.20.1;
  - some BTW sounds have a generic subtitle or none: hitting a BTW block reads "Footsteps", BTW's
    creaking machinery reads "Chest opens";
  - in water, soulforged boots do not change 1.20.1's swimming sounds;
  - BTW mobs walking on deepslate sound like stone;
  - the target outline of a falling block sits about half a block above the block.

## [1.3.0-alpha.3] - 2026-07-09

CurseForge: `BetterThanWolves-1.20.1-1.3.0-alpha.3.jar`, uploaded 2026-07-09. Git: `48e9825`
(version bump only); the jar was built from the tree at `b1ab3c7`, seven seconds before the bump
was committed. The version jumped from 0.4.0-alpha.3 (`2c806c8`, never uploaded) straight to
1.3.0-alpha.3; there are no 0.4-1.2 releases.

Everything since the 0.3.0-alpha.3 upload, including the 2026-06-05 re-audit:

### Fixed
- Bridge link crashes (including the soulforge crash), FC armor registering as real armor, and
  FC foods that could not be eaten or gave nutrition twice (`04274c9`).
- Entity registry crash on start-up (missing Hopper class) (`66b6fa0`).
- Movement modifiers applied twice ("super fast" walking) (`d4a6a98`) and sprint "flying"
  (`80d9a7c`); the movement penalty is now also applied on the client (`2c806c8`).
- FC container clicks needing two clicks, and dragging stacks across slots (`0db375a`).
- FC mobs rubber-banding (`5030245`).
- Blocks ignoring their orientation when placed, e.g. axles (`fe1dd22`).
- Spruce, birch and jungle logs, leaves, planks and slabs dropping as oak (`6b3417e`).
- Lightning rod collision box (`6451796`).
- Two systemic FC block render bugs: a GL11 matrix leak and invisible colour-multiplied
  geometry (`974c644`); stoked-fire textures (`a121a88`); soul urn textures (`532c521`,
  `ca0a5ed`).
- FC mobs dying at random from spurious suffocation (`581d5a2`, `253bca0`, `85fdd2d`, `b1ab3c7`).
  The last two also targeted the red tint, but they did not fix it (see Known issues and triage R4).

### Added
- FC melee damage modifier wired up (`532c521`).
- Renderers for XP orbs, arrows, snowballs, eggs, TNT and the fishing hook (`c2e15f3`, `c32cbf5`).
- Animated stoked-fire flame using FC's own procedural fire (`35b7885`, `0c55137`).
- Worldgen: FC lightning rod in jungle temples (`fc83664`); FC bonus basket replaces the
  bonus chest (`edf0e1b`, `0975006`, `d3072d8`).
- Village subsystem and many verified stub implementations (`a40ba58`, `f36f2d5`, `edd3e73`).
- Build/launch audit tooling (`8c8aea9`, `31ec424`).

### Known issues (found after release)
- Mobs and some textures turn dark red once an unowned XP orb has been drawn; the XP-orb
  renderer added in this release leaks its colour (triage R1).
- Every real hit on an FC mob is logged at WARN (`[MOB-DMG]` diagnostic left in).
- See `docs/curseforge-feedback-triage.md` for the full list.

## [0.3.0-alpha.3] - 2026-05-16

CurseForge upload of 2026-05-16. No commit matches it: the 0.3.0-alpha.3 label was only
committed on 2026-07-08 (`04274c9`), together with later work. The jar was built from an
uncommitted tree after `be8140d`; its exact contents are unknown. It does **not** contain the
2026-06-05 bridge fixes, the June armor registration, or any 2026-07-08/09 fix.

## [0.3.0-alpha.2] - 2026-04-21

CurseForge upload of 2026-04-21. Closest commit: `be8140d` (2026-04-20, "Patched some UI
elements"), which set this version.

## [0.2.0-alpha.2] - 2026-04-13

CurseForge upload of 2026-04-13. No commit ever carried this version; the jar was built from an
uncommitted tree between `9e9cb23` (2026-04-12, "Added entity movement") and `be8140d`.

## [0.1.0-alpha.1] - 2026-04-05

First CurseForge upload. The version label was only committed on 2026-04-12 (`9e9cb23`); the
jar was built from an uncommitted tree whose closest commit is `5257461` (2026-03-30). It is the
first public build of BTW 4.B0000003 running on the Forge 1.20.1 bridge (server and client), with
item textures and models, partial block models and UI, hunger and exhaustion, entity rendering
and the furnace brick (git, 2026-03-18 to 2026-03-30).
