package btw.forge;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Factory that creates the appropriate Forge proxy entity for a given FC
 * legacy entity.  The proxy is a real MC entity that can be added to a
 * {@link ServerLevel} and rendered by the client.  All game-logic
 * callbacks are forwarded to the FC entity.
 *
 * <h3>Proxy selection logic</h3>
 * <ul>
 *   <li>{@link btw.modern.EntityAnimal} / {@link btw.modern.EntityTameable}
 *       &rarr; {@link ProxyAnimal}</li>
 *   <li>{@link btw.modern.EntityMob} / {@link btw.modern.EntityFlying} /
 *       {@link btw.modern.EntitySlime} / {@link btw.modern.EntityAmbientCreature}
 *       &rarr; {@link ProxyMob}</li>
 *   <li>{@link btw.modern.EntityCreature} / {@link btw.modern.EntityGolem} /
 *       {@link btw.modern.EntityWaterMob} / {@link btw.modern.EntityAgeable}
 *       &rarr; {@link ProxyPathfinderMob}</li>
 *   <li>Any other {@link btw.modern.Entity}
 *       &rarr; {@link ProxyEntity}</li>
 * </ul>
 *
 * If a registered {@link EntityType} exists for the FC entity's class
 * (see {@link BTWEntityRegistration}), the factory uses that type.
 * Otherwise a generic type is used as a fallback.
 */
public class EntityProxyFactory {

    private static final Logger LOGGER = LogManager.getLogger("BTW-EntityProxyFactory");

    /**
     * Creates a Forge proxy entity for the given FC entity.
     *
     * @param fcEntity the FC legacy entity to wrap
     * @param level    the Forge level to add the entity to
     * @return a Forge entity with the FC entity linked, or {@code null}
     *         if the entity type could not be determined
     */
    public static net.minecraft.world.entity.Entity createProxy(
            btw.modern.Entity fcEntity, Level level) {

        if (fcEntity == null) {
            LOGGER.warn("createProxy called with null FC entity");
            return null;
        }

        // Try to find a registered EntityType for this FC entity class
        EntityType<?> registeredType =
                BTWEntityRegistration.getEntityType(fcEntity.getClass());

        if (fcEntity instanceof btw.modern.EntityAnimal
                || fcEntity instanceof btw.modern.EntityTameable) {
            return createAnimalProxy(
                    (btw.modern.EntityLiving) fcEntity, registeredType, level);
        }

        if (fcEntity instanceof btw.modern.EntityMob
                || fcEntity instanceof btw.modern.EntityFlying
                || fcEntity instanceof btw.modern.EntitySlime
                || fcEntity instanceof btw.modern.EntityAmbientCreature) {
            return createMobProxy(
                    (btw.modern.EntityLiving) fcEntity, registeredType, level);
        }

        if (fcEntity instanceof btw.modern.EntityCreature
                || fcEntity instanceof btw.modern.EntityGolem
                || fcEntity instanceof btw.modern.EntityWaterMob
                || fcEntity instanceof btw.modern.EntityAgeable) {
            return createPathfinderMobProxy(
                    (btw.modern.EntityLiving) fcEntity, registeredType, level);
        }

        if (fcEntity instanceof btw.modern.EntityLiving) {
            // General living entity -- use PathfinderMob as safest default
            return createPathfinderMobProxy(
                    (btw.modern.EntityLiving) fcEntity, registeredType, level);
        }

        // Plain entity (items, arrows, fireballs, mech power, etc.)
        return createPlainProxy(fcEntity, registeredType, level);
    }

    // ------------------------------------------------------------------
    // Internal proxy builders
    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static net.minecraft.world.entity.Entity createAnimalProxy(
            btw.modern.EntityLiving fcEntity,
            EntityType<?> registeredType,
            Level level) {

        EntityType<ProxyAnimal> type = (EntityType<ProxyAnimal>)
                (registeredType != null ? registeredType : EntityType.PIG);
        ProxyAnimal proxy = new ProxyAnimal(type, level);
        proxy.setFcEntity(fcEntity);
        applyPositionFromFc(proxy, fcEntity);
        LOGGER.debug("Created ProxyAnimal for {}", fcEntity.getClass().getSimpleName());
        return proxy;
    }

    @SuppressWarnings("unchecked")
    private static net.minecraft.world.entity.Entity createMobProxy(
            btw.modern.EntityLiving fcEntity,
            EntityType<?> registeredType,
            Level level) {

        EntityType<ProxyMob> type = (EntityType<ProxyMob>)
                (registeredType != null ? registeredType : EntityType.ZOMBIE);
        ProxyMob proxy = new ProxyMob(type, level);
        proxy.setFcEntity(fcEntity);
        applyPositionFromFc(proxy, fcEntity);
        LOGGER.debug("Created ProxyMob for {}", fcEntity.getClass().getSimpleName());
        return proxy;
    }

    @SuppressWarnings("unchecked")
    private static net.minecraft.world.entity.Entity createPathfinderMobProxy(
            btw.modern.EntityLiving fcEntity,
            EntityType<?> registeredType,
            Level level) {

        EntityType<ProxyPathfinderMob> type = (EntityType<ProxyPathfinderMob>)
                (registeredType != null ? registeredType : EntityType.VILLAGER);
        ProxyPathfinderMob proxy = new ProxyPathfinderMob(type, level);
        proxy.setFcEntity(fcEntity);
        applyPositionFromFc(proxy, fcEntity);
        LOGGER.debug("Created ProxyPathfinderMob for {}",
                fcEntity.getClass().getSimpleName());
        return proxy;
    }

    @SuppressWarnings("unchecked")
    private static net.minecraft.world.entity.Entity createPlainProxy(
            btw.modern.Entity fcEntity,
            EntityType<?> registeredType,
            Level level) {

        EntityType<ProxyEntity> type = (EntityType<ProxyEntity>)
                (registeredType != null ? registeredType : EntityType.MARKER);
        ProxyEntity proxy = new ProxyEntity(type, level);
        proxy.setFcEntity(fcEntity);
        applyPositionFromFc(proxy, fcEntity);
        LOGGER.debug("Created ProxyEntity for {}", fcEntity.getClass().getSimpleName());
        return proxy;
    }

    /**
     * Copies the position from the FC entity to the Forge proxy so
     * that the entity spawns at the correct location.
     */
    private static void applyPositionFromFc(
            net.minecraft.world.entity.Entity proxy, btw.modern.Entity fcEntity) {
        proxy.setPos(fcEntity.posX, fcEntity.posY, fcEntity.posZ);
        proxy.setYRot(fcEntity.rotationYaw);
        proxy.setXRot(fcEntity.rotationPitch);
    }
}
