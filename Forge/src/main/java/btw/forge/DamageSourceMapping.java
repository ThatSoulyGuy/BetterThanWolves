package btw.forge;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;

/**
 * Maps FC legacy DamageSource constants to MC 1.20.1 DamageSource instances.
 * FC code creates damage via static fields like DamageSource.starve,
 * DamageSource.onFire, etc. This mapper translates them.
 */
public class DamageSourceMapping {

    /**
     * Maps an FC DamageSource to the modern MC equivalent.
     *
     * @param fcSource the FC DamageSource (btw.modern.DamageSource)
     * @param level    the server level (needed for 1.20.1 DamageSource registry)
     * @return the modern MC DamageSource, or generic if unmapped
     */
    public static DamageSource getModern(btw.modern.DamageSource fcSource, ServerLevel level) {
        if (fcSource == null) return level.damageSources().generic();

        DamageSources sources = level.damageSources();
        String type = fcSource.damageType;

        if (type == null) return sources.generic();

        return switch (type) {
            case "starve" -> sources.starve();
            case "inFire" -> sources.inFire();
            case "onFire" -> sources.onFire();
            case "lava" -> sources.lava();
            case "inWall" -> sources.inWall();
            case "drown" -> sources.drown();
            case "fall" -> sources.fall();
            case "outOfWorld" -> sources.fellOutOfWorld();
            case "generic" -> sources.generic();
            case "magic" -> sources.magic();
            case "wither" -> sources.wither();
            case "anvil" -> sources.anvil(null);
            case "fallingBlock" -> sources.fallingBlock(null);
            case "cactus" -> sources.cactus();
            case "lightningBolt" -> sources.lightningBolt();
            // FC custom damage types — fcGloom is BTW's "gloom" darkness damage.
            // Mapped to generic() because registering a custom MC DamageType requires
            // a datapack JSON + DamageType registry entry. Generic works for now since
            // the damage amount and bypass logic are handled on the FC side; the MC
            // DamageSource only controls the death message and armor interaction.
            // To add a proper death message, register a custom DamageType in a datapack
            // under data/btw/damage_type/gloom.json and reference it here.
            case "fcGloom" -> sources.generic();
            default -> sources.generic();
        };
    }
}
