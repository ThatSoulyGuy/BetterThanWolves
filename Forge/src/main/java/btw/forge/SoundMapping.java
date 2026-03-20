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
        SOUND_MAP.put("random.anvil_break", SoundEvents.ANVIL_BREAK);
        SOUND_MAP.put("random.anvil_land", SoundEvents.ANVIL_LAND);
        SOUND_MAP.put("random.anvil_use", SoundEvents.ANVIL_USE);

        // ---- dig.* (block break sounds) ----
        SOUND_MAP.put("dig.stone", SoundEvents.STONE_BREAK);
        SOUND_MAP.put("dig.wood", SoundEvents.WOOD_BREAK);
        SOUND_MAP.put("dig.gravel", SoundEvents.GRAVEL_BREAK);
        SOUND_MAP.put("dig.grass", SoundEvents.GRASS_BREAK);
        SOUND_MAP.put("dig.cloth", SoundEvents.WOOL_BREAK);
        SOUND_MAP.put("dig.sand", SoundEvents.SAND_BREAK);
        SOUND_MAP.put("dig.snow", SoundEvents.SNOW_BREAK);

        // ---- step.* (footstep sounds) ----
        SOUND_MAP.put("step.stone", SoundEvents.STONE_STEP);
        SOUND_MAP.put("step.wood", SoundEvents.WOOD_STEP);
        SOUND_MAP.put("step.gravel", SoundEvents.GRAVEL_STEP);
        SOUND_MAP.put("step.grass", SoundEvents.GRASS_STEP);
        SOUND_MAP.put("step.cloth", SoundEvents.WOOL_STEP);
        SOUND_MAP.put("step.sand", SoundEvents.SAND_STEP);
        SOUND_MAP.put("step.snow", SoundEvents.SNOW_STEP);

        // ---- mob.* ----
        SOUND_MAP.put("mob.sheep.shear", SoundEvents.SHEEP_SHEAR);
        SOUND_MAP.put("mob.slime.attack", SoundEvents.SLIME_ATTACK);
        SOUND_MAP.put("mob.slime.big", SoundEvents.SLIME_SQUISH);
        SOUND_MAP.put("mob.ghast.fireball", SoundEvents.GHAST_SHOOT);
        SOUND_MAP.put("mob.ghast.moan", SoundEvents.GHAST_AMBIENT);
        SOUND_MAP.put("mob.zombie.wood", SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR);
        SOUND_MAP.put("mob.zombie.woodbreak", SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR);
        SOUND_MAP.put("mob.wolf.step", SoundEvents.WOLF_STEP);

        // ---- fire / liquid / portal / ambient ----
        SOUND_MAP.put("fire.ignite", SoundEvents.FLINTANDSTEEL_USE);
        SOUND_MAP.put("liquid.splash", SoundEvents.GENERIC_SPLASH);
        SOUND_MAP.put("liquid.swim", SoundEvents.GENERIC_SWIM);
        SOUND_MAP.put("portal.portal", SoundEvents.PORTAL_AMBIENT);
        SOUND_MAP.put("ambient.cave.cave4", SoundEvents.AMBIENT_CAVE.value());
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
