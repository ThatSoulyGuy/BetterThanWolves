package btw.forge;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps FC legacy sound names (e.g., "random.pop", "step.grass") to MC 1.20.1
 * {@link SoundEvent} instances. Used by {@link EntityBridge}, {@link LivingEntityBridge},
 * and {@link PlayerBridge} to play sounds when FC code calls playSound(String, float, float).
 *
 * <p>FC used a flat string-based sound system where names like "random.pop" or
 * "dig.stone" mapped directly to sound resources. MC 1.20.1 uses registry-based
 * SoundEvent objects. This class provides the translation layer.</p>
 */
public class SoundMapping {

    private static final Logger LOGGER = LogManager.getLogger("BTW-SoundMapping");

    private static final Map<String, SoundEvent> SOUND_MAP = new HashMap<>();

    static {
        // ---- random.* ----
        SOUND_MAP.put("random.pop", SoundEvents.ITEM_PICKUP);
        SOUND_MAP.put("random.break", SoundEvents.ITEM_BREAK);
        SOUND_MAP.put("random.click", SoundEvents.UI_BUTTON_CLICK.value());
        SOUND_MAP.put("random.fizz", SoundEvents.FIRE_EXTINGUISH);
        SOUND_MAP.put("random.explode", SoundEvents.GENERIC_EXPLODE);
        SOUND_MAP.put("random.splash", SoundEvents.GENERIC_SPLASH);
        SOUND_MAP.put("random.bow", SoundEvents.ARROW_SHOOT);
        SOUND_MAP.put("random.eat", SoundEvents.GENERIC_EAT);
        SOUND_MAP.put("random.drink", SoundEvents.GENERIC_DRINK);
        SOUND_MAP.put("random.anvil_break", SoundEvents.ANVIL_BREAK);
        SOUND_MAP.put("random.anvil_land", SoundEvents.ANVIL_LAND);
        SOUND_MAP.put("random.anvil_use", SoundEvents.ANVIL_USE);
        SOUND_MAP.put("random.chestclosed", SoundEvents.CHEST_CLOSE);
        SOUND_MAP.put("random.chestopen", SoundEvents.CHEST_OPEN);
        SOUND_MAP.put("random.door_open", SoundEvents.WOODEN_DOOR_OPEN);
        SOUND_MAP.put("random.door_close", SoundEvents.WOODEN_DOOR_CLOSE);
        SOUND_MAP.put("random.fuse", SoundEvents.TNT_PRIMED);
        SOUND_MAP.put("random.levelup", SoundEvents.PLAYER_LEVELUP);
        SOUND_MAP.put("random.orb", SoundEvents.EXPERIENCE_ORB_PICKUP);
        SOUND_MAP.put("random.burp", SoundEvents.PLAYER_BURP);
        SOUND_MAP.put("random.hurt", SoundEvents.PLAYER_HURT);
        SOUND_MAP.put("random.classic_hurt", SoundEvents.PLAYER_HURT);
        SOUND_MAP.put("random.splash", SoundEvents.GENERIC_SPLASH);
        SOUND_MAP.put("random.successful_hit", SoundEvents.PLAYER_ATTACK_STRONG);
        SOUND_MAP.put("random.wood_click", SoundEvents.WOODEN_BUTTON_CLICK_ON);
        SOUND_MAP.put("random.bowhit", SoundEvents.ARROW_HIT);
        SOUND_MAP.put("random.glass", SoundEvents.GLASS_BREAK);

        // ---- dig.* (block break sounds) ----
        SOUND_MAP.put("dig.stone", SoundEvents.STONE_BREAK);
        SOUND_MAP.put("dig.wood", SoundEvents.WOOD_BREAK);
        SOUND_MAP.put("dig.gravel", SoundEvents.GRAVEL_BREAK);
        SOUND_MAP.put("dig.grass", SoundEvents.GRASS_BREAK);
        SOUND_MAP.put("dig.cloth", SoundEvents.WOOL_BREAK);
        SOUND_MAP.put("dig.sand", SoundEvents.SAND_BREAK);
        SOUND_MAP.put("dig.snow", SoundEvents.SNOW_BREAK);
        SOUND_MAP.put("dig.glass", SoundEvents.GLASS_BREAK);

        // ---- step.* (footstep sounds) ----
        SOUND_MAP.put("step.stone", SoundEvents.STONE_STEP);
        SOUND_MAP.put("step.wood", SoundEvents.WOOD_STEP);
        SOUND_MAP.put("step.gravel", SoundEvents.GRAVEL_STEP);
        SOUND_MAP.put("step.grass", SoundEvents.GRASS_STEP);
        SOUND_MAP.put("step.cloth", SoundEvents.WOOL_STEP);
        SOUND_MAP.put("step.sand", SoundEvents.SAND_STEP);
        SOUND_MAP.put("step.snow", SoundEvents.SNOW_STEP);
        SOUND_MAP.put("step.ladder", SoundEvents.LADDER_STEP);

        // ---- mob.* ----
        SOUND_MAP.put("mob.sheep.shear", SoundEvents.SHEEP_SHEAR);
        SOUND_MAP.put("mob.sheep.say", SoundEvents.SHEEP_AMBIENT);
        SOUND_MAP.put("mob.sheep.step", SoundEvents.SHEEP_STEP);
        SOUND_MAP.put("mob.slime.attack", SoundEvents.SLIME_ATTACK);
        // FC saw grinding uses the minecart rumble as its "base" grind sound.
        SOUND_MAP.put("minecart.base", SoundEvents.MINECART_INSIDE);
        SOUND_MAP.put("mob.slime.big", SoundEvents.SLIME_SQUISH);
        SOUND_MAP.put("mob.slime.small", SoundEvents.SLIME_SQUISH_SMALL);
        SOUND_MAP.put("mob.ghast.fireball", SoundEvents.GHAST_SHOOT);
        SOUND_MAP.put("mob.ghast.moan", SoundEvents.GHAST_AMBIENT);
        SOUND_MAP.put("mob.ghast.scream", SoundEvents.GHAST_HURT);
        // BTW "affectionate scream" (witch idle / possession transforms) — reuse ghast ambient.
        SOUND_MAP.put("mob.ghast.affectionate scream", SoundEvents.GHAST_AMBIENT);
        SOUND_MAP.put("mob.zombie.wood", SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR);
        SOUND_MAP.put("mob.zombie.woodbreak", SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR);
        SOUND_MAP.put("mob.zombie.say", SoundEvents.ZOMBIE_AMBIENT);
        SOUND_MAP.put("mob.zombie.hurt", SoundEvents.ZOMBIE_HURT);
        SOUND_MAP.put("mob.zombie.death", SoundEvents.ZOMBIE_DEATH);
        SOUND_MAP.put("mob.zombie.step", SoundEvents.ZOMBIE_STEP);
        SOUND_MAP.put("mob.zombiepig.zpigangry", SoundEvents.ZOMBIFIED_PIGLIN_ANGRY);
        SOUND_MAP.put("mob.wolf.bark", SoundEvents.WOLF_AMBIENT);
        SOUND_MAP.put("mob.wolf.step", SoundEvents.WOLF_STEP);
        SOUND_MAP.put("mob.wolf.hurt", SoundEvents.WOLF_HURT);
        SOUND_MAP.put("mob.wolf.death", SoundEvents.WOLF_DEATH);
        SOUND_MAP.put("mob.wolf.growl", SoundEvents.WOLF_GROWL);
        SOUND_MAP.put("mob.wolf.panting", SoundEvents.WOLF_PANT);
        SOUND_MAP.put("mob.wolf.whine", SoundEvents.WOLF_WHINE);
        SOUND_MAP.put("mob.wolf.howl", SoundEvents.WOLF_HOWL);
        SOUND_MAP.put("mob.cow.say", SoundEvents.COW_AMBIENT);
        // BTW-added cow variant moos (FCBlockBellows / milking) — no distinct vanilla event.
        SOUND_MAP.put("mob.cow.say2", SoundEvents.COW_AMBIENT);
        SOUND_MAP.put("mob.cow.say4", SoundEvents.COW_AMBIENT);
        SOUND_MAP.put("mob.cow.hurt", SoundEvents.COW_HURT);
        SOUND_MAP.put("mob.cow.step", SoundEvents.COW_STEP);
        SOUND_MAP.put("mob.cow.death", SoundEvents.COW_DEATH);
        SOUND_MAP.put("mob.pig.say", SoundEvents.PIG_AMBIENT);
        SOUND_MAP.put("mob.pig.death", SoundEvents.PIG_DEATH);
        SOUND_MAP.put("mob.pig.step", SoundEvents.PIG_STEP);
        SOUND_MAP.put("mob.chicken.say", SoundEvents.CHICKEN_AMBIENT);
        SOUND_MAP.put("mob.chicken.hurt", SoundEvents.CHICKEN_HURT);
        SOUND_MAP.put("mob.chicken.step", SoundEvents.CHICKEN_STEP);
        SOUND_MAP.put("mob.chicken.plop", SoundEvents.CHICKEN_EGG);
        SOUND_MAP.put("mob.creeper.say", SoundEvents.CREEPER_PRIMED);
        SOUND_MAP.put("mob.creeper.death", SoundEvents.CREEPER_DEATH);
        SOUND_MAP.put("mob.skeleton.say", SoundEvents.SKELETON_AMBIENT);
        SOUND_MAP.put("mob.skeleton.hurt", SoundEvents.SKELETON_HURT);
        SOUND_MAP.put("mob.skeleton.death", SoundEvents.SKELETON_DEATH);
        SOUND_MAP.put("mob.skeleton.step", SoundEvents.SKELETON_STEP);
        SOUND_MAP.put("mob.spider.say", SoundEvents.SPIDER_AMBIENT);
        SOUND_MAP.put("mob.spider.death", SoundEvents.SPIDER_DEATH);
        SOUND_MAP.put("mob.spider.step", SoundEvents.SPIDER_STEP);
        SOUND_MAP.put("mob.endermen.stare", SoundEvents.ENDERMAN_STARE);
        SOUND_MAP.put("mob.endermen.hit", SoundEvents.ENDERMAN_HURT);
        SOUND_MAP.put("mob.endermen.death", SoundEvents.ENDERMAN_DEATH);
        SOUND_MAP.put("mob.endermen.portal", SoundEvents.ENDERMAN_TELEPORT);
        SOUND_MAP.put("mob.endermen.scream", SoundEvents.ENDERMAN_SCREAM);
        SOUND_MAP.put("mob.enderdragon.growl", SoundEvents.ENDER_DRAGON_GROWL);
        SOUND_MAP.put("mob.wither.spawn", SoundEvents.WITHER_SPAWN);
        SOUND_MAP.put("mob.wither.shoot", SoundEvents.WITHER_SHOOT);
        SOUND_MAP.put("mob.wither.death", SoundEvents.WITHER_DEATH);
        SOUND_MAP.put("mob.irongolem.hit", SoundEvents.IRON_GOLEM_HURT);
        SOUND_MAP.put("mob.irongolem.death", SoundEvents.IRON_GOLEM_DEATH);
        SOUND_MAP.put("mob.irongolem.walk", SoundEvents.IRON_GOLEM_STEP);
        SOUND_MAP.put("mob.cat.meow", SoundEvents.CAT_AMBIENT);
        SOUND_MAP.put("mob.cat.hiss", SoundEvents.CAT_HISS);
        SOUND_MAP.put("mob.cat.purr", SoundEvents.CAT_PURR);
        SOUND_MAP.put("mob.villager.idle", SoundEvents.VILLAGER_AMBIENT);
        SOUND_MAP.put("mob.villager.haggle", SoundEvents.VILLAGER_TRADE);
        SOUND_MAP.put("mob.villager.hit", SoundEvents.VILLAGER_HURT);
        SOUND_MAP.put("mob.villager.death", SoundEvents.VILLAGER_DEATH);
        SOUND_MAP.put("mob.villager.yes", SoundEvents.VILLAGER_YES);
        SOUND_MAP.put("mob.villager.no", SoundEvents.VILLAGER_NO);
        SOUND_MAP.put("mob.blaze.breathe", SoundEvents.BLAZE_AMBIENT);
        SOUND_MAP.put("mob.blaze.hit", SoundEvents.BLAZE_HURT);
        SOUND_MAP.put("mob.blaze.death", SoundEvents.BLAZE_DEATH);
        SOUND_MAP.put("mob.bat.idle", SoundEvents.BAT_AMBIENT);
        SOUND_MAP.put("mob.bat.hurt", SoundEvents.BAT_HURT);
        SOUND_MAP.put("mob.bat.death", SoundEvents.BAT_DEATH);
        SOUND_MAP.put("mob.bat.takeoff", SoundEvents.BAT_TAKEOFF);
        SOUND_MAP.put("mob.magmacube.big", SoundEvents.MAGMA_CUBE_SQUISH);
        SOUND_MAP.put("mob.magmacube.small", SoundEvents.MAGMA_CUBE_SQUISH_SMALL);
        SOUND_MAP.put("mob.magmacube.jump", SoundEvents.MAGMA_CUBE_JUMP);
        SOUND_MAP.put("mob.silverfish.say", SoundEvents.SILVERFISH_AMBIENT);
        SOUND_MAP.put("mob.silverfish.hit", SoundEvents.SILVERFISH_HURT);
        SOUND_MAP.put("mob.silverfish.kill", SoundEvents.SILVERFISH_DEATH);
        SOUND_MAP.put("mob.silverfish.step", SoundEvents.SILVERFISH_STEP);
        SOUND_MAP.put("mob.witch.idle", SoundEvents.WITCH_AMBIENT);
        SOUND_MAP.put("mob.witch.hurt", SoundEvents.WITCH_HURT);
        SOUND_MAP.put("mob.witch.death", SoundEvents.WITCH_DEATH);

        // ---- fire / liquid / portal / ambient ----
        SOUND_MAP.put("fire.fire", SoundEvents.FIRE_AMBIENT);
        SOUND_MAP.put("fire.ignite", SoundEvents.FLINTANDSTEEL_USE);
        SOUND_MAP.put("liquid.splash", SoundEvents.GENERIC_SPLASH);
        SOUND_MAP.put("liquid.swim", SoundEvents.GENERIC_SWIM);
        SOUND_MAP.put("liquid.water", SoundEvents.WATER_AMBIENT);
        SOUND_MAP.put("liquid.lava", SoundEvents.LAVA_AMBIENT);
        SOUND_MAP.put("liquid.lavapop", SoundEvents.LAVA_POP);
        SOUND_MAP.put("portal.portal", SoundEvents.PORTAL_AMBIENT);
        SOUND_MAP.put("portal.trigger", SoundEvents.PORTAL_TRIGGER);
        SOUND_MAP.put("portal.travel", SoundEvents.PORTAL_TRAVEL);
        SOUND_MAP.put("ambient.cave.cave", SoundEvents.AMBIENT_CAVE.value());
        SOUND_MAP.put("ambient.cave.cave4", SoundEvents.AMBIENT_CAVE.value());
        SOUND_MAP.put("ambient.weather.rain", SoundEvents.WEATHER_RAIN);
        SOUND_MAP.put("ambient.weather.thunder", SoundEvents.LIGHTNING_BOLT_THUNDER);

        // ---- note.* (note block) ----
        SOUND_MAP.put("note.harp", SoundEvents.NOTE_BLOCK_HARP.value());
        SOUND_MAP.put("note.bass", SoundEvents.NOTE_BLOCK_BASS.value());
        SOUND_MAP.put("note.bassattack", SoundEvents.NOTE_BLOCK_BASEDRUM.value());
        SOUND_MAP.put("note.bd", SoundEvents.NOTE_BLOCK_BASEDRUM.value());
        SOUND_MAP.put("note.hat", SoundEvents.NOTE_BLOCK_HAT.value());
        SOUND_MAP.put("note.snare", SoundEvents.NOTE_BLOCK_SNARE.value());
        SOUND_MAP.put("note.pling", SoundEvents.NOTE_BLOCK_PLING.value());

        // ---- damage.* ----
        SOUND_MAP.put("damage.hit", SoundEvents.PLAYER_HURT);
        SOUND_MAP.put("damage.fallbig", SoundEvents.PLAYER_BIG_FALL);
        SOUND_MAP.put("damage.fallsmall", SoundEvents.PLAYER_SMALL_FALL);

        // ---- tile.* (block place/break) ----
        SOUND_MAP.put("tile.piston.out", SoundEvents.PISTON_EXTEND);
        SOUND_MAP.put("tile.piston.in", SoundEvents.PISTON_CONTRACT);

        // ---- FC custom — map to closest vanilla equivalent ----
        SOUND_MAP.put("fcwhitesmoke", SoundEvents.FIRE_EXTINGUISH);
    }

    /**
     * Returns the MC SoundEvent for the given FC sound name, or null if unmapped.
     */
    public static SoundEvent get(String fcSoundName) {
        return SOUND_MAP.get(fcSoundName);
    }

    /**
     * Plays an FC-named sound at the given entity's position.
     * If the sound name is not in the map, logs a debug message and does nothing.
     *
     * @param entity the MC entity at whose position the sound should play
     * @param fcSoundName the FC legacy sound name (e.g., "random.pop")
     * @param volume sound volume
     * @param pitch sound pitch
     */
    public static void playAtEntity(Entity entity, String fcSoundName, float volume, float pitch) {
        if (fcSoundName == null || entity == null) return;

        SoundEvent event = SOUND_MAP.get(fcSoundName);
        if (event == null) {
            LOGGER.debug("Unmapped FC sound: '{}' — skipping playback", fcSoundName);
            return;
        }

        entity.level().playSound(
                null, // null = play for all nearby players
                entity.getX(), entity.getY(), entity.getZ(),
                event,
                SoundSource.PLAYERS,
                volume,
                pitch);
    }
}
