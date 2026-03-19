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
            // FC custom damage types
            case "fcGloom" -> sources.generic(); // TODO: register custom FC damage type
            default -> sources.generic();
        };
    }
}
