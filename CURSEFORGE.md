<!-- MAINTAINER: do not publish this page while a "TODO (maintainer)" under Links is still open.
     Once the BTW wiki link is filled in, the second Installation tip may point players to it. -->

# Better Than Wolves — Forge 1.20.1 Port

A faithful port of **Better Than Wolves** (originally by FlowerChild for Minecraft 1.5.2) to **Forge 1.20.1**.

This is not a rewrite. The port runs the original FC game code verbatim on top of a compatibility bridge that translates modern Minecraft's APIs back into the APIs FC was written against. Every block, item, entity, recipe, and tile-entity behaviour comes straight from the original BTW 4.B0000003 sources — the same progression, the same difficulty curve, the same unforgiving hunger/penalty system FlowerChild designed.

---

## What Better Than Wolves Is

BTW is a total-conversion survival mod that throws out Minecraft's ease-of-life shortcuts and rebuilds the game around a multi-stage **mechanical-power tech tree**. Early game is hard. Resources are scarce. Tools break. Food matters. The payoff is one of the deepest hand-built tech progressions ever shipped in a Minecraft mod — windmills, waterwheels, axles, gearboxes, mill stones, saws, anvils, cauldrons, crucibles, kilns, soulforges, and the soul-steel endgame.

If you've never played BTW before, **do not expect vanilla Minecraft + extras**. Expect a different, harsher, more rewarding game.

---

## Core Content Included

**Progression & Tech**
- Full mechanical-power network: windmills, waterwheels, horizontal/vertical axles, gearboxes, hand cranks
- Mill stone, saw, anvil, chopping block, turntable, pottery wheel
- Hibachi + stoked fire, kiln, cauldron, stoked cauldron, crucible, stoked crucible
- Soulforge and the steel progression
- Bellows, detector blocks, pulleys, rope, cords

**Farming & Food**
- Hemp cultivation, flax-style fiber processing
- Animal husbandry loop with FC's health/nutrition rules
- Cooking, baking, knitting, dying
- Stoking, roasting, canning

**Combat & Survival**
- FC's pre-1.9 combat model retained
- Composite bow, battleaxe, warhammer, mattock, steel tools
- Gloom, hunger, fat, and health penalty system (four separate debuff tracks)
- FC's food scale (0–60) driving penalty gates

**World & Mobs**
- All FC blocks, plants, ores, and worldgen-facing content
- FC's custom animal behaviours and drop tables

**Integration**
- **JEI** support (optional). BTW 4.B itself had no recipe viewer; with JEI installed you can browse:
  - recipe categories for the cauldron, stoked cauldron, crucible, stoked crucible, mill stone, soulforge (4x4) and campfire spit (the plain crucible list is empty, as in 4.B);
  - FC's special crafting recipes (log chopping with an axe, knitting, baiting a fishing rod) in the crafting category;
  - information pages for the processes that have no recipe list: kiln, saw, turntable, hopper filters and souls, hibachi and stoked fire, cauldron/crucible fire rules, and campfire cooking.
- **Optional mod support.** With Dynamic Trees, Serene Seasons, AppleSkin, Cold Sweat, Legendary Survival Overhaul or Pam's HarvestCraft 2 installed, BTW makes their content follow BTW's own rules. None of them is required or included in the BTW jar. See **Compatibility Notes**.

---

## Requirements

- **Minecraft 1.20.1**
- **Forge 47.3.0** or newer
- **Java 17**
- **JEI 15.3.0.8+** (optional, recommended)

---

## Installation

1. Install Forge 1.20.1.
2. Drop the BTW jar into your `mods/` folder.
3. (Optional) Drop JEI 15.x into the same folder.
4. (Optional) Add any of the supported mods listed under **Compatibility Notes**.
5. Launch and create a new world.

> **Tip:** BTW assumes a fresh world. Do not apply it to an existing save.

> **Tip:** BTW 4.B teaches almost nothing in game, by design. Install JEI for the recipes and for notes on the processes that have no recipe list (kiln, saw, turntable, hopper, fire).

---

## Compatibility Notes

This port runs FlowerChild's original code through a runtime bridge. The port's rule is never to edit his code: it adapts modern Minecraft around it instead. Other Forge mods generally work alongside it, but a few caveats apply:

- **World generation mods** that replace Minecraft biomes wholesale may hide FC ores or shift FC worldgen.
- **Food/hunger mods** will conflict — BTW replaces the vanilla food system with its own penalty model. AppleSkin, Cold Sweat, Legendary Survival Overhaul and Pam's HarvestCraft 2 are the exceptions: BTW supports them (see **Supported optional mods** below).
- **Combat overhauls** (that add attack cooldown tuning, dodge mechanics, etc.) may not mesh with FC's pre-1.9 combat.
- **Tool-durability overhauls** (mending changes, unbreakable tool mods) break FC's repair economy on purpose.
- **Early-progression mods** (copper/brass tech trees, quest mods, mining mods) typically don't need to conflict but were not explicitly tested.
- **Multiplayer:** run the same version of this port on the server and on every client. A client whose BTW network protocol differs from the server's is refused when it joins.

If you are bundling BTW in a modpack, keep its progression in mind — many "quality of life" mods trivialise FC's intended difficulty curve.

**Supported optional mods**

BTW supports the mods below. None of them is included in the BTW jar, and BTW runs unchanged without them. Install each one on the server and on every client; AppleSkin is needed on clients only. When one is present, BTW and that mod work together as described below, and `latest.log` says so, for example `[BTW-Compat] dynamictrees 1.20.1-1.4.10 present: integration #027 enabled`. Forge refuses to start with a version older than the minimum given below (except HarvestCraft Food Extended, whose jar reports no version). Newer versions are accepted but were not tested. If BTW cannot work with an installed version, it logs `[BTW-Compat] <mod> integration failed to start; BTW continues without it`, and that mod keeps its own rules.

The tested versions below were run together on a Forge 47.3.0 dedicated server with their release jars (Dynamic Trees 1.20.1-1.4.10; 1.20.1-1.4.11 only in development runs), and one by one and in combinations in development runs of the server and the client.

- **Dynamic Trees** — tested with 1.20.1-1.4.10 and 1.20.1-1.4.11; needs 1.20.1-1.4.10 or newer. Living Dynamic Trees follow BTW's log rules:
  - only an axe that can harvest BTW logs fells a tree and gets its logs, at BTW's hunger cost and 1 durability per log. Dynamic Trees' own Fortune bonus logs are kept and cost nothing extra;
  - any other hit chews the trunk as BTW chews a log: bark first, then sticks and sawdust. A tree that is chewed through drops no logs;
  - the bottom block of a trunk is a BTW stump, which an axe only chews. Once the tree above it is felled, the stump remover removes the stump, and an iron or better chisel turns it into a work stump;
  - a powered BTW saw cuts trunks that are at least one block thick, and the rest of the tree falls with its logs;
  - bone meal on a tree fertilises its rooty soil, as BTW fertiliser does, instead of making the tree grow. Fruit and pods take no bone meal and ripen at the pace of BTW's cocoa;
  - saplings grow at BTW's sapling pace: they need light and never grow in the End. Dynamic Trees' crimson and warped fungi grow on their own at BTW's 1-in-50 fungus rate;
  - leaves drop as BTW's leaves do: only the species' seed, 1 time in 20 (1 in 40 for jungle trees), with no sticks and no Fortune bonus. Leaves of fruit trees drop no seed, and Dynamic Trees' seasonal seed chance still applies;
  - Dynamic Trees' world generation, falling trees, seeds and its own season support are unchanged. See **Dynamic Trees settings** below.
- **Serene Seasons** (9.1 and later need GlitchCore) — tested with 9.1.0.3 and GlitchCore 0.0.1.1, the recommended pair; needs 9.0.0.0 or newer.
  - BTW's crops follow Serene Seasons' crop seasons: BTW wheat grows in summer and autumn, hemp in spring and summer, and crops Serene Seasons already knows, such as carrots, keep their Serene Seasons seasons. Serene Seasons' out-of-season setting and its greenhouse glass apply to them, and a lit BTW Light Block above a crop also counts as greenhouse glass.
  - Serene Seasons' seasonal temperature decides where BTW's snow and ice form, so temperate biomes get snow and ice in winter.
  - 9.3.0.27: the Serene Seasons code BTW uses is the same as in 9.1.0.3 (checked against its jar, not played), but Cold Sweat applies seasons only with Serene Seasons 9.1.0.0 to 9.3.0.26. With Cold Sweat installed, use 9.1.0.3.
  - Older 9.0.0.x builds are accepted but were not tested. With them, BTW leaves crop seasons to that Serene Seasons build's own rule.
  - Serene Seasons' own behaviour that shows in BTW: in spring it melts ice into water source blocks, including ice that players placed, and some blocks that BTW draws itself keep their summer colours.
- **AppleSkin** (clients only) — tested with 2.5.1; needs 2.5.0 or newer (2.5.0 has the same API, not played). AppleSkin's tooltips, held-food preview, exhaustion underlay, health hint and F3 line show BTW's food model on BTW's own food bar: BTW's 60-point food values, fat, food-poisoning chance and exhaustion. AppleSkin's own on/off settings still apply. A server may have AppleSkin installed too; BTW's AppleSkin support runs only on the client.
- **Cold Sweat** — tested with 2.4.3.2; needs 2.4.1 or newer (2.4.1 has the same API, not played).
  - BTW's campfire (more with each size, from embers up to a large fire), stoked fire, hibachi, burning brick oven, torches, candle, smouldering log, burning ladder and loose stone with lava in its cracks warm you, and so do the vanilla torch, candles, candle cakes and the lantern;
  - knit wool, padded, tanned leather and gimp armour insulate, and BTW textiles can be sewn onto armour at Cold Sweat's sewing table;
  - BTW's cauldron soups and stews, and mushroom stew, warm you over time;
  - BTW fuels burn in Cold Sweat's hearth and boiler, at BTW's ratio to coal;
  - freezing makes you shiver, which drains BTW's food bar like other exertion, fat before hunger as in 4.B. You do not shiver during Cold Sweat's Grace period for new players or on peaceful;
  - careful: to BTW, Cold Sweat's cold and heat damage is damage like any other, so it can set off blasting oil you carry.
- **Legendary Survival Overhaul (LSO)** — tested with 1.20.1-2.4.7, which is also the oldest version BTW accepts.
  - BTW's heat sources, clothing and armour, hot meals and fuels count in LSO's temperature, padded and plate armour also protect LSO's limbs, and BTW's drinks and stews quench thirst;
  - BTW fat gives cold resistance;
  - LSO's Cold Hunger tints BTW's food bar frost blue instead of drawing a second food bar;
  - LSO's own healing follows BTW's rule: no healing at or below 24 of 60 food. See **Healing with Legendary Survival Overhaul** below;
  - bone meal does not grow LSO's ferns and water plants on BTW ground and is not used up, as 4.B bone meal on wild plants.
- **Pam's HarvestCraft 2** — any of Crops, Trees, Food Core and Food Extended; tested with Crops 1.0.3, Trees 1.0.2, Food Core 1.0.5 and Food Extended 1.0.1; needs Crops 1.0.3, Trees 1.0.2, Food Core 1.0.0 and Food Extended 1.0.0 or newer. Food Extended needs Food Core, although its own mod information does not say so. What BTW changes is listed in the **Pam's HarvestCraft 2: licence and interoperability notice** below.
- **Dynamic Trees for PHC2** — tested with 1.20.1-1.3.2; needs Dynamic Trees and HarvestCraft Trees. Its trees follow the Dynamic Trees rules above, and its fruit takes no bone meal and ripens at BTW's cocoa pace.
- **Serene Seasons - Pam's HarvestCraft 2 - Crops Compat** — tested with 1.20.1-1.0.0; needs Serene Seasons and HarvestCraft Crops. HarvestCraft crops then follow Serene Seasons' seasons on top of BTW's growth rules.

**Cold Sweat and Legendary Survival Overhaul together**

Both mods simulate body temperature, and BTW feeds its heat sources, clothing, meals and fuels to both, so with both installed their effects stack. BTW logs one warning about it at start-up. Recommended: let Cold Sweat own temperature. In `config/legendarysurvivaloverhaul/legendarysurvivaloverhaul-common.toml`, under `[core]`, set `"Temperature Enabled" = false`. LSO's thirst, limbs and health overhaul keep working; BTW's heat sources, clothing, meal warmth, fuels and fat then have no effect in LSO, and Cold Sweat handles them.

**Healing with Legendary Survival Overhaul**

By default (`"Natural Regeneration Enabled" = false` under `[core.misc]` in the same file), LSO switches Minecraft's natural regeneration off and heals through its own regeneration instead. BTW lets it heal only above 24 of 60 food, as in 4.B, and each heal costs BTW food. For BTW's own 4.B healing instead, set `"Natural Regeneration Enabled" = true`. Change this setting rather than `/gamerule naturalRegeneration`: LSO sets the game rule from it whenever a world loads, and until then a game rule set by hand can let both heal.

**Dynamic Trees settings**

- Leave `replaceVanillaSapling` (in `config/dynamictrees-common.toml`, section `[vanilla]`) at its default, `false`. With it on, Dynamic Trees turns vanilla saplings into its own trees by its own rule, which BTW does not gate. That setup was not tested and is not supported.
- BTW follows Dynamic Trees' stripping settings in `dynamictrees-server.toml` (the world's `serverconfig` folder, or `defaultconfigs` for new worlds), section `[interaction]`. A chew gives bark only where Dynamic Trees allows stripping (`minRadiusForStrip`, default 6; `0` turns stripping, and with it the bark, off). With `enableStripRadiusReduction = false`, a full trunk takes one more chew hit.
- Dynamic Trees' `axeDamageMode` makes no difference: an axe fell always costs 1 durability per log.
- Known differences from BTW's own trees: trunks thicker than one block lose one ring per chew hit, so chewing through a 3x3 trunk takes about 9 hits where BTW's rule would need 45; Dynamic Trees' crimson and warped fungi give no bark and leave no work stump, like the stems; a tree from another mod whose log BTW does not know gives oak bark and makes an oak work stump, unless a data pack adds that log to one of BTW's bark tags (`betterthanwolves:bark/<wood>` for that wood's bark and work stump, `betterthanwolves:bark/none` for neither); Dynamic Trees' Dendro potions and fertilisers other than bone meal keep their Dynamic Trees effects.

**Pam's HarvestCraft 2: licence and interoperability notice**

Pam's HarvestCraft 2 is made by Pamela Collins (MatrexsVigil, PamHarvestCraft) and is All Rights Reserved. BTW does not include, redistribute or rename any part of it: get HarvestCraft from its own CurseForge or Modrinth page. BTW contains no HarvestCraft code and names none of its classes; it recognises HarvestCraft content only by its item and block ids, tags and data.

Pam's licence allows changing HarvestCraft through add-ons, JSON files or config changes, as long as players are told about the changes and that they are not hers. **The changes below are made by this BTW port, not by Pam.** Please report problems with HarvestCraft in a BTW world to the BTW issue tracker (see **Links**), not to Pam. In game, every HarvestCraft item that BTW's rules govern carries the tooltip line "Better Than Wolves: grown, eaten and cooked by BTW rules".

With HarvestCraft installed, BTW changes this:

- **Crops** are planted on BTW farmland and in BTW planters and grow by BTW's crop rules; weeds can grow in their farmland as under BTW crops. Breaking a crop untills the farmland, and an unripe crop drops nothing.
- **Bone meal** never grows HarvestCraft crops, saplings, gardens or fruit, as in 4.B. On a crop it fertilises the farmland below; elsewhere it does nothing and is not used up.
- **Gardens** spread when you use BTW dung on them.
- **Saplings** grow at BTW's sapling pace.
- **Tree fruit** ripens at BTW's cocoa pace, a quarter of HarvestCraft's own rate, and never in the End.
- **Food values:** HarvestCraft food is worth BTW food points, by how filling HarvestCraft makes it: Crops and Trees items 3 or 6 of BTW's 60 points, Food Core and Food Extended items 6 to 30, capped at BTW's hearty stew. Raw ground meat is worth the BTW raw meat of the same animal and can give food poisoning like it; cooked ground meat is worth BTW cooked meat. A meal crafted from raw meat can poison you too. HarvestCraft food stacks to 16, like BTW food.
- **Seeds:** HarvestCraft makes its seeds edible. BTW currently values each at 3 of 60 points, while 4.B's only edible seed, pumpkin seeds, is worth 1. Whether HarvestCraft seeds should follow 4.B here is still an open question.
- **Animal feed:** HarvestCraft seeds feed BTW chickens, vegetables feed BTW pigs, and grains feed BTW's plant-eating animals.
- **Mill stone:** HarvestCraft flour plants grind into BTW flour (1 each), and fibre crops into 2 hemp fibres.
- **Recipes:**
  - HarvestCraft's grain-to-bread recipe becomes BTW's dough recipe: 3 flour and a water bucket make BTW bread dough, which you bake in BTW's brick oven;
  - HarvestCraft dough with bakeware makes BTW bread dough instead of bread;
  - HarvestCraft dough costs 3 flour (with the mixing bowl, water and salt) and makes one dough instead of two, like BTW's own dough;
  - HarvestCraft flour is made from BTW flour with the grinder, not ground from flour plants;
  - cooking oil and a cattail make an unlit BTW finite torch instead of a vanilla torch;
  - the string-from-fibre recipe is removed: mill fibre crops instead.
- Every other HarvestCraft recipe, block, item and world generation feature stays as HarvestCraft made it. Food Core 1.0.5 logs parse errors for three of its own recipes (melon pie, honey muffin, caramel cupcakes); they come from HarvestCraft's data, not from BTW.

**For developers: running these mods from source**

`-PbtwDevMods=<tokens>` (or the `ORG_GRADLE_PROJECT_btwDevMods` environment variable) puts the listed mods on the classpath of every development run (`runServer`, `runClient` and `runData`), for example `gradlew :Forge:runServer -PbtwDevMods=dynamictrees,sereneseasons` or `gradlew :Forge:runClient -PbtwDevMods=appleskin,lso`. They never reach the BTW jar: the build fails if another mod's class ends up in it. The tokens:

- `dynamictrees`: Dynamic Trees 1.20.1-1.4.10
- `dynamictrees-latest`: Dynamic Trees 1.20.1-1.4.11 (not together with `dynamictrees`)
- `sereneseasons`: Serene Seasons 9.1.0.3 and GlitchCore 0.0.1.1
- `appleskin`: AppleSkin 2.5.1
- `coldsweat`: Cold Sweat 2.4.3.2
- `lso`: Legendary Survival Overhaul 1.20.1-2.4.7
- `harvestcraft`: Pam's HarvestCraft 2 Crops 1.0.3, Trees 1.0.2, Food Core 1.0.5 and Food Extended 1.0.1
- `dtphc2`: Dynamic Trees for PHC2 1.20.1-1.3.2 (needs `dynamictrees` or `dynamictrees-latest`, and `harvestcraft`)
- `ss-hc2crops`: Serene Seasons - Pam's HarvestCraft 2 - Crops Compat 1.20.1-1.0.0 (needs `sereneseasons` and `harvestcraft`)

An unknown token, both Dynamic Trees tokens at once, or an add-on token without the mods it needs stops the build with an error that says why. The first run with a new set of tokens downloads the jars; after that, `--offline` works. Without the property, JEI is the only other mod on the development runtime. BTW's generated data for these mods (the Legendary Survival Overhaul, Serene Seasons and HarvestCraft files) comes from a plain `gradlew :Forge:runData`, with no token and no `BTWConfig.txt`.

---

## Known Differences from BTW 4.B

The port aims to play exactly like BTW 4.B0000003, but it is not there yet. If something works differently from BTW 4.B, it is most likely a port bug: please report it (see **Links**) with your `latest.log`.

- Minecraft 1.20.1 blocks, items, mobs and biomes that did not exist in 1.5.2 are still in the game:
  - BTW's mobs and machines treat the newer blocks as plain solid blocks; seagrass and kelp count as water. Deepslate, cobbled deepslate and the deepslate ores other than copper are the exception: they are BTW stone, cobblestone and ores (see the stone strata below).
  - Some 1.20.1 mob types are replaced by BTW mobs: drowned, husks and zombie villagers spawn as BTW zombies, strays and wither skeletons as BTW skeletons, and glow squids as BTW squids. Because drowned spawn in water, BTW zombies also appear in rivers and oceans. A zombie villager that comes with a village becomes a BTW villager zombie instead (see the villages below).
  - Acacia, dark oak, mangrove and cherry are BTW woods: their logs chew down when hit without an axe, giving bark for tanning, and they make work stumps, go through the saw and the axe chopping recipe and burn as BTW fuel. Their log -> 4 planks recipe is removed, as for BTW's own woods. They also have BTW's sidings, mouldings, corners and other wood blocks, and their planks follow BTW's plank rule. Crimson and warped stems chew and saw like logs but give no bark and leave no work stump, and a mining charge breaks them into sawdust. Bamboo blocks no longer craft into planks: the saw and the axe chopping recipe make bamboo planks from them, with no bark and no work stump. Crimson, warped and bamboo planks stay Minecraft planks.
  - Leaves of the newer trees drop only their sapling, at BTW's rate, as BTW's leaves do: no sticks and no apples.
  - 1.20.1's black, brown, blue and white dye are colours only: bone meal stays the fertiliser, and the book and quill, polished lapis and chocolate need the ink sac, lapis lazuli and cocoa beans, as in 4.B.
  - Cornflowers and lilies of the valley follow BTW's flower rules, like the other small flowers; wither roses and torchflowers keep Minecraft 1.20.1's.
  - Soul torches follow BTW's torch rule: they can only be placed where a BTW torch can, and they drop like a torch when their support no longer holds them.
  - The other wooden fences, the other walls and the stained glass panes hold things as BTW's oak fence, cobblestone wall and glass pane do: a torch or a redstone torch stands on a fence or a wall, an axe (and no other tool) can be stuck into a wooden fence, and no pressure plate goes on any of them, nor a torch on a stained glass pane. They still look, connect and drop as in Minecraft 1.20.1. The newer pressure plates follow BTW's plate rule too: the wooden ones the oak plate's, the polished blackstone one the stone plate's.
  - Fence gates, trapdoors and wooden doors other than oak, stained and tinted glass, packed and blue ice, and the newer leaves do not follow their BTW relative yet and hold things as in Minecraft 1.20.1: for example, snow settles on a stained glass roof and a pressure plate goes on stained glass, unlike plain glass. BTW's ash resting above one of the other fences or walls is drawn on top of the post, where above an oak fence or a cobblestone wall it is drawn on the ground, as in 4.B.
  - Raw iron and raw gold count as BTW's iron and gold ore chunks: they cook into nuggets, not ingots, a raw iron or raw gold block unpacks into nine chunks, and raw ore that BTW stores or changes comes back as BTW's chunk.
  - Copper ore, deepslate copper ore, raw copper and copper blocks work as in Minecraft 1.20.1; 4.B had no copper. Granite, diorite, andesite, tuff and calcite are not BTW stone: they stay as soft as in Minecraft at any depth.
  - BTW's block dispenser picks up a boat as that boat's own item, keeping its wood and its chest.
- Torches, ladders, snow, pressure plates and rails follow 4.B's support rules however they are placed, sneaking included: falling snow does not settle on glass roofs, redstone lamps or BTW's hopper, mill stone, cauldron or crucible, and a pressure plate or rail cannot be placed on glass, glowstone or ice. Two exceptions remain: a powered rail clicked without sneaking onto a block whose top 4.B does not count as solid is placed and drops at once as an item, where 4.B refuses the click; and snow that world generation lays on glass, TNT or a redstone lamp open to the sky stays until a block next to it changes.
- Villages are zombified, as in 4.B: their villagers are villager zombies that a thrown soul urn cures, and villages near the spawn are partly or fully abandoned. Minecraft 1.20.1's villages have different buildings, so a villager's trade is taken from the job block of its house. Baby villagers become baby villager zombies, new villages have no iron golem, and villages generated before this version keep their villagers. In worlds created in Creative mode villages keep their villagers, and 1.20.1's rare zombie villages still hold villager zombies.
- Stone gets harder with depth at 4.B's heights: harder from about y=48 down and hardest from about y=24 down, although Minecraft 1.20.1 worlds reach down to y=-64, and all deepslate is the hardest layer. Stone you place down there is as hard as the rock around it; in 4.B placed stone stayed as soft as surface stone. Stone also looks the same at every depth, where 4.B gave its deeper layers darker textures.
- BTW mobs do not trigger tripwire, do not travel through nether or end portals, and are not slowed by soul sand. Blocks being moved by a piston are solid to them.
- BTW's Hardcore Buoy option (`fcEnableHardcoreBuoy`) has no effect yet: dropped items float, and are pushed out of blocks they are stuck in, as in Minecraft 1.20.1.
- The Obstructed player-names option (`fcEnableHardcorePlayerNames=2`) hides only players' name tags behind walls, not named mobs' tags. The default Hardcore option (no player name tags) works.
- The vanilla bow fires Minecraft's own arrows, from anywhere in your inventory; 4.B fired BTW arrows from the hotbar only. BTW's arrow handling applies to rotten arrows and the composite bow.
- BTW mobs never target players in creative or spectator mode, as in Minecraft 1.20.1; in 4.B a mob kept chasing a player who switched to creative. This is a maintainer decision. A squid never knocks a shield out of your hand: it disarms only the BTW swords and tools you block with, as in 4.B.
- Beds let you sleep through the night, with 1.20.1's rules and messages, and they still explode in the Nether and the End, but a bed never sets your spawn point. In 4.B beds could not be slept in at all; this middle path is a maintainer decision. Beds and doors break and drop as in 1.20.1: one item, and a bed keeps its colour.
- Respawning works as in BTW 4.B: you come back at a random spot 1000 to 2000 blocks from the original spawn, farther as the game progresses, with a soft respawn if you die again soon and shared respawns early in the game, and a bound soulforged steel beacon still brings you back to it. A charged respawn anchor in the Nether and `/spawnpoint` also set where you respawn (1.20.1 additions), and a respawn spot is never outside the world border. In worlds created in Creative mode you respawn as in Minecraft (at your respawn point, else the world spawn). BTW counts the one-day windows of the soft and shared respawns by Minecraft's day clock, so a night you sleep through counts, and with the daylight cycle turned off the windows never end. 4.B's mystery meat drop on a player's death is not ported.
- The beacon is BTW's: the block its pyramid is made of decides what it does, and it has no menu. Minecraft's beacon powers are gone, so a beacon built before this version gives its BTW effect instead of the power chosen in its menu, and a pyramid of netherite or of mixed blocks gives none. The beam and its hum are Minecraft's, and the beam takes the colour of stained glass above it. True Sight does not show its particles yet. Compasses follow BTW's magnetic field (the spawn, or a powered beacon other than a glass one); a lodestone compass works as in Minecraft. Ender chests always open your personal ender inventory, as in Minecraft; 4.B's ender chest tiers, which depend on an ender block beacon, are not ported yet.
- Nether portals can be larger than 4.B's 2 by 3 opening: fire and BTW's campfire both light any obsidian frame Minecraft 1.20.1 accepts, up to 21 by 21.
- Explosions follow BTW's block rules, with these exceptions: TNT that another explosion sets off goes off as Minecraft TNT (BTW's own lit TNT is not ported yet); chests, furnaces and other blocks that hold contents, beds, doors and a few blocks that 1.20.1 split into several kinds drop by Minecraft's explosion rule; blasted infested stone releases silverfish, and blasted nether quartz ore can give experience, as in Minecraft. In worlds created before this version, TNT drops every block it breaks unless you run `/gamerule tntExplosionDropDecay true`.
- A block that is still falling when the game saves, for example when you quit or leave the area, carries on falling when you come back. In 4.B it was lost. This is a maintainer decision.
- Doors, trapdoors and fence gates open and close with redstone as in 1.20.1, the wooden ones included; 4.B's wooden doors, trapdoors and gates ignore redstone. This is a maintainer decision.
- Most items that 1.20.1 split into separate items, such as flowers, wool colours and wooden slabs, keep their kind in BTW's hands, but a few come back as their 1.5.2 equivalent when BTW stores or changes them:
  - smooth sandstone becomes cut sandstone;
  - stone slabs become smooth stone slabs;
  - wet sponges become dry sponges, carved pumpkins become pumpkins, and coloured beds become red beds.
- BTW's own items can only be used from the main hand; 1.5.2 had no off hand.
- JEI support, the optional mod support (see **Compatibility Notes**) and the port's one-time join note are additions; 4.B had none of them. So is Minecraft's anvil repair menu: an empty-handed sneak-click on an anvil opens it, while a plain click opens BTW's 3x3 anvil crafting, as in 4.B.
- Port bugs and their fixes are listed per release in `CHANGELOG.md` in the source repository: https://github.com/ThatSoulyGuy/BetterThanWolves

---

## Known Limitations (Alpha)

This is an ongoing port. The following are in progress:

- Some visual polish (cracked pottery overlay, specific animated textures)
- A handful of FC tile-entity renderers
- A few mob animations and effects: squid tentacles, the cow's kick, villager and witch particles, the composite bow's draw animation; a squid on your head is drawn as a free squid, a little behind you when you move fast
- Network sync timing on some multi-player edge cases
- Full compatibility with every modded dimension is not guaranteed

Bug reports with log files are welcome. "I tried to use X and it didn't drop Y" reports **with a `latest.log`** are the most useful.

---

## Credits

- **FlowerChild** — original author of Better Than Wolves for Minecraft 1.5.2. This port contains his code verbatim. All gameplay design credit belongs to him.
- **Mojang** — Minecraft.
- **MinecraftForge team** — the Forge 1.20.1 modloader.
- **mezz** — JEI.
- **SpongePowered** — Mixin.
- **Ferreusveritas** — Dynamic Trees (optional, not included).
- **Adubbz and Forstride** — Serene Seasons; **Adubbz** — GlitchCore (optional, not included).
- **squeek** — AppleSkin (optional, not included).
- **iMikul, chubbymomo and skinnymomo** — Cold Sweat (optional, not included).
- **Sfiomn** — Legendary Survival Overhaul (optional, not included).
- **Pamela Collins (MatrexsVigil, PamHarvestCraft)** — Pam's HarvestCraft 2 (optional, not included).
- **Max Hyper** — Dynamic Trees for PHC2 (optional, not included).
- **Darkorg** — Serene Seasons - Pam's HarvestCraft 2 - Crops Compat (optional, not included).

---

## License

The port bridge code (the `btw.forge.*` and `btw.modern.*` compatibility layer) is released under its own terms. The BTW content itself remains the property of its original author and is redistributed only to the extent permitted by the original BTW distribution license. If you are the original author and wish redistribution terms to change, please contact the port maintainer.

---

## Links

- **Issue tracker:** https://github.com/ThatSoulyGuy/BetterThanWolves/issues
- **Original BTW:** *TODO (maintainer): add the link to FlowerChild's original Better Than Wolves release. Left blank on purpose rather than guessing a URL.*
- **BTW wiki:** *TODO (maintainer): add the link to the BTW 4.x wiki players should use. Left blank on purpose rather than guessing a URL.*
