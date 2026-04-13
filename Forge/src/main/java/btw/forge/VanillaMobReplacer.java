package btw.forge;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Intercepts vanilla mob spawns and replaces them with their FC proxy
 * equivalents. This ensures that naturally spawning mobs, spawner-block
 * mobs, structure mobs, and spawn-egg mobs all get FC's AI, drops,
 * and behavior.
 *
 * <p>Fires at {@link EventPriority#HIGHEST} so it runs before other
 * mods that might query the entity. The vanilla entity is cancelled and
 * a {@code betterthanwolves:fc_*} proxy is spawned at the same position.</p>
 *
 * <p>Proxy entities ({@link ProxyMob}, {@link ProxyAnimal},
 * {@link ProxyPathfinderMob}) are excluded from replacement to prevent
 * infinite recursion.</p>
 */
@Mod.EventBusSubscriber(modid = BTWForgeMod.MOD_ID)
public final class VanillaMobReplacer {

    private static final Logger LOGGER = LogManager.getLogger("BTW-MobReplacer");

    /**
     * Vanilla EntityType → FC entity type registry name.
     * Every entry means "when MC tries to spawn this vanilla type,
     * cancel it and spawn the FC version instead."
     */
    private static final Map<EntityType<?>, ResourceLocation> REPLACEMENTS = new IdentityHashMap<>();

    static {
        String ns = BTWForgeMod.MOD_ID;

        // --- Animals ---
        REPLACEMENTS.put(EntityType.COW,          new ResourceLocation(ns, "fc_cow"));
        REPLACEMENTS.put(EntityType.PIG,          new ResourceLocation(ns, "fc_pig"));
        REPLACEMENTS.put(EntityType.SHEEP,        new ResourceLocation(ns, "fc_sheep"));
        REPLACEMENTS.put(EntityType.CHICKEN,      new ResourceLocation(ns, "fc_chicken"));
        REPLACEMENTS.put(EntityType.WOLF,         new ResourceLocation(ns, "fc_wolf"));
        REPLACEMENTS.put(EntityType.OCELOT,       new ResourceLocation(ns, "fc_ocelot"));

        // --- Hostile ---
        REPLACEMENTS.put(EntityType.ZOMBIE,           new ResourceLocation(ns, "fc_zombie"));
        REPLACEMENTS.put(EntityType.SKELETON,         new ResourceLocation(ns, "fc_skeleton"));
        REPLACEMENTS.put(EntityType.SPIDER,           new ResourceLocation(ns, "fc_spider"));
        REPLACEMENTS.put(EntityType.CREEPER,          new ResourceLocation(ns, "fc_creeper"));
        REPLACEMENTS.put(EntityType.ENDERMAN,         new ResourceLocation(ns, "fc_enderman"));
        REPLACEMENTS.put(EntityType.CAVE_SPIDER,      new ResourceLocation(ns, "fc_cave_spider"));
        REPLACEMENTS.put(EntityType.BLAZE,            new ResourceLocation(ns, "fc_blaze"));
        REPLACEMENTS.put(EntityType.WITCH,            new ResourceLocation(ns, "fc_witch"));
        REPLACEMENTS.put(EntityType.WITHER,           new ResourceLocation(ns, "fc_wither"));
        REPLACEMENTS.put(EntityType.ZOMBIFIED_PIGLIN, new ResourceLocation(ns, "fc_pig_zombie"));
        REPLACEMENTS.put(EntityType.SLIME,            new ResourceLocation(ns, "fc_slime"));
        REPLACEMENTS.put(EntityType.MAGMA_CUBE,       new ResourceLocation(ns, "fc_magma_cube"));
        REPLACEMENTS.put(EntityType.GHAST,            new ResourceLocation(ns, "fc_ghast"));

        // --- Ambient / passive ---
        REPLACEMENTS.put(EntityType.BAT,        new ResourceLocation(ns, "fc_bat"));
        REPLACEMENTS.put(EntityType.SNOW_GOLEM, new ResourceLocation(ns, "fc_snowman"));
        REPLACEMENTS.put(EntityType.VILLAGER,   new ResourceLocation(ns, "fc_villager"));
        REPLACEMENTS.put(EntityType.SQUID,      new ResourceLocation(ns, "fc_squid"));
    }

    /** Re-entrancy guard — prevents the FC spawn from triggering itself. */
    private static boolean replacing = false;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (replacing) return;
        if (event.getLevel().isClientSide()) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        Entity entity = event.getEntity();

        // Never replace entities that are already FC proxies
        if (entity instanceof ProxyMob
                || entity instanceof ProxyAnimal
                || entity instanceof ProxyPathfinderMob
                || entity instanceof ProxyEntity) {
            return;
        }

        ResourceLocation fcId = REPLACEMENTS.get(entity.getType());
        if (fcId == null) return;

        EntityType<?> fcType = ForgeRegistries.ENTITY_TYPES.getValue(fcId);
        if (fcType == null) {
            LOGGER.debug("FC entity type {} not found in registry", fcId);
            return;
        }

        // Cancel the vanilla entity
        event.setCanceled(true);

        // Spawn FC replacement at the same position / rotation
        replacing = true;
        try {
            Entity fcEntity = fcType.create(level);
            if (fcEntity != null) {
                fcEntity.setPos(entity.getX(), entity.getY(), entity.getZ());
                fcEntity.setYRot(entity.getYRot());
                fcEntity.setXRot(entity.getXRot());
                level.addFreshEntity(fcEntity);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to spawn FC replacement {} for {}: {}",
                    fcId, entity.getType(), e.getMessage());
        } finally {
            replacing = false;
        }
    }

    private VanillaMobReplacer() {}
}
