# Better Than Wolves — Forge 1.20.1 Port

A faithful port of **Better Than Wolves** (originally by FlowerChild for Minecraft 1.5.2) to **Forge 1.20.1**.

This is not a rewrite. The port runs the original FC game code verbatim on top of a compatibility bridge that translates modern Minecraft's APIs back into the APIs FC was written against. Every block, item, entity, recipe, and tile-entity behaviour comes straight from the original BTW 4.B0000003 sources — the same progression, the same difficulty curve, the same unforgiving hunger/penalty system FlowerChild designed.

---

## What Better Than Wolves Is

BTW is a total-conversion survival mod that throws out Minecraft's ease-of-life shortcuts and rebuilds the game around a multi-stage **mechanical-power tech tree**. Early game is hard. Resources are scarce. Tools break. Food matters. Sleep costs you. The payoff is one of the deepest hand-built tech progressions ever shipped in a Minecraft mod — windmills, waterwheels, axles, gearboxes, mill stones, saws, anvils, cauldrons, crucibles, kilns, soulforges, and the soul-steel endgame.

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
- Hardcore spawn, costly sleep, FC's death penalties

**Integration**
- Full **JEI** support (optional). Cauldron, stoked cauldron, crucible, stoked crucible, and mill stone recipe categories are browsable in-game.

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
4. Launch. Create a new world. Read the field manual before your first night.

> **Tip:** BTW assumes a fresh world. Do not apply it to an existing save.

---

## Compatibility Notes

This port runs the original FC code through a runtime bridge. Other Forge mods generally work alongside it, but a few caveats apply:

- **World generation mods** that replace Minecraft biomes wholesale may hide FC ores or shift FC worldgen.
- **Food/hunger mods** will conflict — BTW replaces the vanilla food system with its own penalty model.
- **Combat overhauls** (that add attack cooldown tuning, dodge mechanics, etc.) may not mesh with FC's pre-1.9 combat.
- **Tool-durability overhauls** (mending changes, unbreakable tool mods) break FC's repair economy on purpose.
- **Early-progression mods** (copper/brass tech trees, quest mods, mining mods) typically don't need to conflict but were not explicitly tested.

If you are bundling BTW in a modpack, keep its progression in mind — many "quality of life" mods trivialise FC's intended difficulty curve.

---

## Known Limitations (Beta)

This is an ongoing port. The following are in progress:

- Some visual polish (cracked pottery overlay, specific animated textures)
- A handful of FC tile-entity renderers
- Certain late-game FC entities still use placeholder models
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

---

## License

The port bridge code (the `btw.forge.*` and `btw.modern.*` compatibility layer) is released under its own terms. The BTW content itself remains the property of its original author and is redistributed only to the extent permitted by the original BTW distribution license. If you are the original author and wish redistribution terms to change, please contact the port maintainer.

---

## Links

- **Issue tracker:** *add your GitHub/forum link here*
- **Original BTW:** *add link here*
- **Wiki / field manual:** *add link here*
