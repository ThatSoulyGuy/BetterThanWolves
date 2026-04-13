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

        // For entity-sourced damage, resolve the MC proxy entity so MC's
        // knockback, death messages, and combat tracker work correctly.
        net.minecraft.world.entity.Entity mcAttacker = null;
        if (fcSource instanceof btw.modern.EntityDamageSource eds) {
            btw.modern.Entity fcAttacker = eds.getEntity();
            if (fcAttacker != null) {
                mcAttacker = resolveProxy(fcAttacker, level);
            }
        }

        return switch (type) {
            case "mob" -> sources.mobAttack(mcAttacker instanceof net.minecraft.world.entity.LivingEntity le
                    ? le : null);
            case "player" -> sources.playerAttack(mcAttacker instanceof net.minecraft.world.entity.player.Player p
                    ? p : null);
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
            case "fcGloom" -> sources.generic();
            default -> sources.generic();
        };
    }

    /**
     * Finds the MC proxy entity for an FC entity.
     */
    private static net.minecraft.world.entity.Entity resolveProxy(btw.modern.Entity fcEntity, ServerLevel level) {
        // Search by entity ID — the proxy and FC entity share the same ID.
        return level.getEntity(fcEntity.entityId);
    }
}
