package btw.forge;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps FC legacy potion IDs to modern MC 1.20.1 MobEffects.
 * FC code references potions by integer ID (e.g., Potion.hunger.id = 17).
 * This mapping allows PlayerBridge to translate FC potion checks
 * to real MC effect queries.
 */
public class PotionMapping {

    private static final Map<Integer, MobEffect> ID_TO_EFFECT = new HashMap<>();

    static {
        // MC 1.5.2 potion IDs → MC 1.20.1 MobEffects
        ID_TO_EFFECT.put(1, MobEffects.MOVEMENT_SPEED);
        ID_TO_EFFECT.put(2, MobEffects.MOVEMENT_SLOWDOWN);
        ID_TO_EFFECT.put(3, MobEffects.DIG_SPEED);
        ID_TO_EFFECT.put(4, MobEffects.DIG_SLOWDOWN);
        ID_TO_EFFECT.put(5, MobEffects.DAMAGE_BOOST);
        ID_TO_EFFECT.put(6, MobEffects.HEAL);
        ID_TO_EFFECT.put(7, MobEffects.HARM);
        ID_TO_EFFECT.put(8, MobEffects.JUMP);
        ID_TO_EFFECT.put(9, MobEffects.CONFUSION);
        ID_TO_EFFECT.put(10, MobEffects.REGENERATION);
        ID_TO_EFFECT.put(11, MobEffects.DAMAGE_RESISTANCE);
        ID_TO_EFFECT.put(12, MobEffects.FIRE_RESISTANCE);
        ID_TO_EFFECT.put(13, MobEffects.WATER_BREATHING);
        ID_TO_EFFECT.put(14, MobEffects.INVISIBILITY);
        ID_TO_EFFECT.put(15, MobEffects.BLINDNESS);
        ID_TO_EFFECT.put(16, MobEffects.NIGHT_VISION);
        ID_TO_EFFECT.put(17, MobEffects.HUNGER);
        ID_TO_EFFECT.put(18, MobEffects.WEAKNESS);
        ID_TO_EFFECT.put(19, MobEffects.POISON);
        ID_TO_EFFECT.put(20, MobEffects.WITHER);
    }

    /**
     * Returns the modern MobEffect for a legacy FC potion ID.
     */
    public static MobEffect getEffect(int legacyId) {
        return ID_TO_EFFECT.get(legacyId);
    }

    /**
     * Returns the modern MobEffect for an FC Potion object.
     */
    public static MobEffect getEffect(btw.modern.Potion fcPotion) {
        if (fcPotion == null) return null;
        return ID_TO_EFFECT.get(fcPotion.id);
    }
}
