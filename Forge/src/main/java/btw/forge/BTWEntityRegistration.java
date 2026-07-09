package btw.forge;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Registers Forge {@link EntityType}s for all FC entity classes that
 * appear in {@code FCBetterThanWolves.CreateModEntityMappings()} and
 * {@code ReplaceExistingMapping()} calls.
 *
 * Each FC entity class is mapped to an EntityType that uses one of the
 * proxy shells ({@link ProxyMob}, {@link ProxyAnimal},
 * {@link ProxyPathfinderMob}, or {@link ProxyEntity}).  The Forge
 * engine treats these as first-class entity types and handles
 * spawning, tracking, persistence, etc.
 *
 * Registration happens after FCBetterThanWolves has finished its
 * initialization (so the EntityList mappings are populated).  This is
 * called from {@link BTWRegistration#registerAllBTWContent()}.
 */
public class BTWEntityRegistration {

    private static final Logger LOGGER = LogManager.getLogger("BTW-EntityRegistration");

    /**
     * Maps FC entity class -> registered Forge EntityType.
     * Populated during {@link #registerEntities()}.
     */
    private static final Map<Class<?>, EntityType<?>> fcClassToEntityType = new HashMap<>();
    private static final Map<EntityType<?>, String> entityTypeToFcClassName = new HashMap<>();

    /** Returns the FC class name for a given EntityType, or empty string if unknown. */
    public static String getFcClassName(EntityType<?> type) {
        String name = entityTypeToFcClassName.get(type);
        return name != null ? name : "";
    }

    /** Registered EntityType -> FC class name, for the startup self-audit (renderer coverage). */
    public static Map<EntityType<?>, String> getRegisteredFcEntities() {
        return java.util.Collections.unmodifiableMap(entityTypeToFcClassName);
    }

    /**
     * Maps FC entity class -> EntityType for external lookup
     * (used by {@link EntityProxyFactory}).
     */
    /** Returns all registered FC EntityTypes from the Forge registry. */
    public static java.util.Collection<EntityType<?>> getAllEntityTypes() {
        java.util.List<EntityType<?>> result = new java.util.ArrayList<>();
        for (Map.Entry<Class<?>, EntityType<?>> entry : fcClassToEntityType.entrySet()) {
            // Look up the type from the actual registry to ensure instance identity
            ResourceLocation key = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(entry.getValue());
            if (key != null) {
                EntityType<?> registryType = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(key);
                if (registryType != null) {
                    result.add(registryType);
                    continue;
                }
            }
            result.add(entry.getValue());
        }
        // The generic fallback type is not keyed by an FC class (see
        // getGenericPlainType) but still needs its FCEntityRenderer.
        EntityType<?> generic = getGenericPlainType();
        if (generic != null) {
            result.add(generic);
        }
        return result;
    }

    public static EntityType<?> getEntityType(Class<?> fcClass) {
        // Walk the hierarchy to find a registered type
        Class<?> c = fcClass;
        while (c != null && c != Object.class) {
            EntityType<?> type = fcClassToEntityType.get(c);
            if (type != null) {
                // Return the instance from the Forge registry (not our local copy)
                // so MC's identity-based renderer lookup matches
                ResourceLocation key = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(type);
                if (key != null) {
                    EntityType<?> registryType = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(key);
                    if (registryType != null) return registryType;
                }
                return type;
            }
            c = c.getSuperclass();
        }
        return null;
    }

    /**
     * Registers all BTW entity types with the Forge entity registry.
     * Should be called during post-init after FC has populated its
     * EntityList mappings.
     */
    private static net.minecraftforge.registries.RegisterEvent currentEvent;

    public static void registerEntities(net.minecraftforge.registries.RegisterEvent event) {
        currentEvent = event;
        registerEntities();
        currentEvent = null;
    }

    public static void registerEntities() {
        int registered = 0;

        // ============================================================
        // Vanilla replacement entities (ReplaceExistingMapping calls)
        // These override vanilla mobs with FC versions.
        // ============================================================

        // --- Hostile mobs (EntityMob hierarchy) -> ProxyMob ---
        registered += registerMob("fc_creeper", "FCEntityCreeper", MobCategory.MONSTER, 0.6F, 1.7F);
        registered += registerMob("fc_skeleton", "FCEntitySkeleton", MobCategory.MONSTER, 0.6F, 1.99F);
        registered += registerMob("fc_spider", "FCEntitySpider", MobCategory.MONSTER, 1.4F, 0.9F);
        registered += registerMob("fc_zombie", "FCEntityZombie", MobCategory.MONSTER, 0.6F, 1.95F);
        registered += registerMob("fc_enderman", "FCEntityEnderman", MobCategory.MONSTER, 0.6F, 2.9F);
        registered += registerMob("fc_cave_spider", "FCEntityCaveSpider", MobCategory.MONSTER, 0.7F, 0.5F);
        registered += registerMob("fc_blaze", "FCEntityBlaze", MobCategory.MONSTER, 0.6F, 1.8F);
        registered += registerMob("fc_witch", "FCEntityWitch", MobCategory.MONSTER, 0.6F, 1.95F);
        registered += registerMob("fc_wither", "FCEntityWither", MobCategory.MONSTER, 0.9F, 3.5F);
        registered += registerMob("fc_pig_zombie", "FCEntityPigZombie", MobCategory.MONSTER, 0.6F, 1.95F);
        registered += registerMob("fc_slime", "FCEntitySlime", MobCategory.MONSTER, 2.04F, 2.04F);
        registered += registerMob("fc_magma_cube", "FCEntityMagmaCube", MobCategory.MONSTER, 2.04F, 2.04F);

        // --- Flying mobs -> ProxyMob ---
        registered += registerMob("fc_ghast", "FCEntityGhast", MobCategory.MONSTER, 4.0F, 4.0F);

        // --- Ambient (bat) -> ProxyMob ---
        registered += registerMob("fc_bat", "FCEntityBat", MobCategory.AMBIENT, 0.5F, 0.9F);

        // --- Animals (EntityAnimal / EntityTameable hierarchy) -> ProxyAnimal ---
        registered += registerAnimal("fc_pig", "FCEntityPig", 0.9F, 0.9F);
        registered += registerAnimal("fc_sheep", "FCEntitySheep", 0.9F, 1.3F);
        registered += registerAnimal("fc_cow", "FCEntityCow", 0.9F, 1.4F);
        registered += registerAnimal("fc_chicken", "FCEntityChicken", 0.4F, 0.7F);
        registered += registerAnimal("fc_wolf", "FCEntityWolf", 0.6F, 0.85F);
        registered += registerAnimal("fc_ocelot", "FCEntityOcelot", 0.6F, 0.7F);

        // --- Passive mobs (PathfinderMob category) ---
        registered += registerPathfinderMob("fc_snowman", "FCEntitySnowman", MobCategory.MISC, 0.7F, 1.9F);
        registered += registerPathfinderMob("fc_villager", "FCEntityVillager", MobCategory.MISC, 0.6F, 1.95F);
        registered += registerPathfinderMob("fc_squid", "FCEntitySquid", MobCategory.WATER_CREATURE, 0.8F, 0.8F);

        // ============================================================
        // BTW custom entities (AddMapping calls in CreateModEntityMappings)
        // ============================================================

        // --- Custom creatures ---
        registered += registerPathfinderMob("fc_dire_wolf", "FCEntityWolfDire", MobCategory.MONSTER, 0.6F, 0.85F);
        registered += registerMob("fc_jungle_spider", "FCEntityJungleSpider", MobCategory.MONSTER, 1.4F, 0.9F);
        registered += registerMob("fc_wither_persistent", "FCEntityWitherPersistent", MobCategory.MONSTER, 0.9F, 3.5F);

        // --- Non-living custom entities ---
        registered += registerPlainEntity("fc_water_wheel", "FCEntityWaterWheel", MobCategory.MISC, 4.0F, 4.0F);
        registered += registerPlainEntity("fc_wind_mill", "FCEntityWindMill", MobCategory.MISC, 8.0F, 8.0F);
        registered += registerPlainEntity("fc_wind_mill_vertical", "FCEntityWindMillVertical", MobCategory.MISC, 8.0F, 8.0F);
        registered += registerPlainEntity("fc_moving_anchor", "FCEntityMovingAnchor", MobCategory.MISC, 1.0F, 1.0F);
        registered += registerPlainEntity("fc_moving_platform", "FCEntityMovingPlatform", MobCategory.MISC, 1.0F, 1.0F);
        registered += registerPlainEntity("fc_block_lifted", "FCEntityBlockLiftedByPlatform", MobCategory.MISC, 1.0F, 1.0F);
        registered += registerPlainEntity("fc_dynamite", "FCEntityDynamite", MobCategory.MISC, 0.98F, 0.98F);
        registered += registerPlainEntity("fc_mining_charge", "FCEntityMiningCharge", MobCategory.MISC, 0.98F, 0.98F);
        registered += registerPlainEntity("fc_urn", "FCEntityUrn", MobCategory.MISC, 0.25F, 0.25F);
        registered += registerPlainEntity("fc_broadhead_arrow", "FCEntityBroadheadArrow", MobCategory.MISC, 0.5F, 0.5F);
        registered += registerPlainEntity("fc_infinite_arrow", "FCEntityInfiniteArrow", MobCategory.MISC, 0.5F, 0.5F);
        registered += registerPlainEntity("fc_rotten_arrow", "FCEntityRottenArrow", MobCategory.MISC, 0.5F, 0.5F);
        registered += registerPlainEntity("fc_item_floating", "FCEntityItemFloating", MobCategory.MISC, 0.25F, 0.25F);
        registered += registerPlainEntity("fc_item_blood_wood_sapling", "FCEntityItemBloodWoodSapling", MobCategory.MISC, 0.25F, 0.25F);
        registered += registerPlainEntity("fc_canvas", "FCEntityCanvas", MobCategory.MISC, 0.5F, 0.5F);
        registered += registerPlainEntity("fc_spider_web", "FCEntitySpiderWeb", MobCategory.MISC, 0.25F, 0.25F);
        registered += registerPlainEntity("fc_soul_sand", "FCEntitySoulSand", MobCategory.MISC, 0.98F, 0.98F);
        registered += registerPlainEntity("fc_falling_block", "FCEntityFallingBlock", MobCategory.MISC, 0.98F, 0.98F);
        registered += registerPlainEntity("fc_wither_skull", "FCEntityWitherSkull", MobCategory.MISC, 0.3125F, 0.3125F);
        registered += registerPlainEntity("fc_lightning_bolt", "FCEntityLightningBolt", MobCategory.MISC, 0.0F, 0.0F);

        // ============================================================
        // Frozen vanilla 1.5.2 entities spawned live by FC code through
        // WorldBridge.spawnEntityInWorld: EntityXPOrb (mob-death Dragon
        // Orbs, FCTileEntityArcaneVessel, FCTileEntityHopper), EntityArrow
        // (FCItemBow / FCItemArrow dispenser), EntitySnowball (snowman AI),
        // EntityTNTPrimed (FC-lit TNT), EntityEgg / EntityFishHook.
        // Previously these fell through to EntityProxyFactory's
        // EntityType.MARKER fallback (clientTrackingRange 0, 0x0 size), so
        // they were invisible and untouchable client-side. Sizes match the
        // 1.5.2 constructors' setSize calls.
        // ============================================================
        registered += registerPlainEntity("fc_xp_orb", "EntityXPOrb", MobCategory.MISC, 0.5F, 0.5F);
        registered += registerPlainEntity("fc_arrow", "EntityArrow", MobCategory.MISC, 0.5F, 0.5F);
        registered += registerPlainEntity("fc_snowball", "EntitySnowball", MobCategory.MISC, 0.25F, 0.25F);
        registered += registerPlainEntity("fc_tnt_primed", "EntityTNTPrimed", MobCategory.MISC, 0.98F, 0.98F);
        registered += registerPlainEntity("fc_egg", "EntityEgg", MobCategory.MISC, 0.25F, 0.25F);
        registered += registerPlainEntity("fc_fish_hook", "EntityFishHook", MobCategory.MISC, 0.25F, 0.25F);

        // Generic fallback for FC entity classes without a dedicated
        // registration — replaces the MARKER fallback in EntityProxyFactory.
        registerGenericPlainType();

        // Bridge for FC's EntityTracker packet sends (cow kick / squid
        // tentacle custom entity events). Installed here because this runs
        // exactly once per side (ENTITY_TYPES RegisterEvent) and after
        // BTWNetwork.register() (mod constructor) created the channel.
        EntityTrackerBridge.install();

        LOGGER.info("Registered {} BTW entity types with Forge registries.", registered);
    }

    /**
     * Called during the EntityAttributeCreationEvent to register
     * attributes for all living-entity proxy types.
     */
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        for (Map.Entry<Class<?>, EntityType<?>> entry : fcClassToEntityType.entrySet()) {
            EntityType<?> type = entry.getValue();

            // Only living entities need attribute registration.
            // We determine which proxy class was used based on FC class hierarchy.
            Class<?> fcClass = entry.getKey();
            try {
                if (isAnimalClass(fcClass)) {
                    @SuppressWarnings("unchecked")
                    EntityType<ProxyAnimal> animalType = (EntityType<ProxyAnimal>) type;
                    event.put(animalType, ProxyAnimal.createProxyAnimalAttributes().build());
                } else if (isMobClass(fcClass)) {
                    @SuppressWarnings("unchecked")
                    EntityType<ProxyMob> mobType = (EntityType<ProxyMob>) type;
                    event.put(mobType, ProxyMob.createProxyMobAttributes().build());
                } else if (isPathfinderMobClass(fcClass)) {
                    @SuppressWarnings("unchecked")
                    EntityType<ProxyPathfinderMob> pathType = (EntityType<ProxyPathfinderMob>) type;
                    event.put(pathType, ProxyPathfinderMob.createProxyPathfinderMobAttributes().build());
                }
                // Plain entities (ProxyEntity) do not need attributes
            } catch (Exception e) {
                LOGGER.error("Failed to register attributes for {}: {}",
                        fcClass.getSimpleName(), e.getMessage());
            }
        }
        LOGGER.info("Registered attributes for {} BTW living entity types.",
                fcClassToEntityType.size());
    }

    // ------------------------------------------------------------------
    // Internal registration helpers
    // ------------------------------------------------------------------

    private static int registerMob(String id, String fcClassName,
                                    MobCategory category, float width, float height) {
        try {
            Class<?> fcClass = findFcClass(fcClassName);
            if (fcClass == null) return 0;

            EntityType<ProxyMob> type = EntityType.Builder
                    .<ProxyMob>of(ProxyMob::new, category)
                    .sized(width, height)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build(BTWForgeMod.MOD_ID + ":" + id);

            ResourceLocation key = new ResourceLocation(BTWForgeMod.MOD_ID, id);
            if (currentEvent != null) {
                currentEvent.register(net.minecraftforge.registries.ForgeRegistries.Keys.ENTITY_TYPES,
                        key, () -> type);
            } else {
                Registry.register(BuiltInRegistries.ENTITY_TYPE, key, type);
            }
            fcClassToEntityType.put(fcClass, type);
            entityTypeToFcClassName.put(type, fcClass.getName());
            return 1;
        } catch (Exception e) {
            LOGGER.error("Failed to register mob entity type '{}': {}", id, e.getMessage());
            return 0;
        }
    }

    private static int registerAnimal(String id, String fcClassName,
                                       float width, float height) {
        try {
            Class<?> fcClass = findFcClass(fcClassName);
            if (fcClass == null) return 0;

            EntityType<ProxyAnimal> type = EntityType.Builder
                    .<ProxyAnimal>of(ProxyAnimal::new, MobCategory.CREATURE)
                    .sized(width, height)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .build(BTWForgeMod.MOD_ID + ":" + id);

            ResourceLocation key = new ResourceLocation(BTWForgeMod.MOD_ID, id);
            if (currentEvent != null) {
                currentEvent.register(net.minecraftforge.registries.ForgeRegistries.Keys.ENTITY_TYPES,
                        key, () -> type);
            } else {
                Registry.register(BuiltInRegistries.ENTITY_TYPE, key, type);
            }
            fcClassToEntityType.put(fcClass, type);
            entityTypeToFcClassName.put(type, fcClass.getName());
            return 1;
        } catch (Exception e) {
            LOGGER.error("Failed to register animal entity type '{}': {}", id, e.getMessage());
            return 0;
        }
    }

    private static int registerPathfinderMob(String id, String fcClassName,
                                              MobCategory category,
                                              float width, float height) {
        try {
            Class<?> fcClass = findFcClass(fcClassName);
            if (fcClass == null) return 0;

            EntityType<ProxyPathfinderMob> type = EntityType.Builder
                    .<ProxyPathfinderMob>of(ProxyPathfinderMob::new, category)
                    .sized(width, height)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .build(BTWForgeMod.MOD_ID + ":" + id);

            ResourceLocation key = new ResourceLocation(BTWForgeMod.MOD_ID, id);
            if (currentEvent != null) {
                currentEvent.register(net.minecraftforge.registries.ForgeRegistries.Keys.ENTITY_TYPES,
                        key, () -> type);
            } else {
                Registry.register(BuiltInRegistries.ENTITY_TYPE, key, type);
            }
            fcClassToEntityType.put(fcClass, type);
            entityTypeToFcClassName.put(type, fcClass.getName());
            return 1;
        } catch (Exception e) {
            LOGGER.error("Failed to register pathfinder mob entity type '{}': {}",
                    id, e.getMessage());
            return 0;
        }
    }

    // ------------------------------------------------------------------
    // Generic fallback type
    // ------------------------------------------------------------------

    private static EntityType<ProxyEntity> genericPlainType;

    /**
     * Generic plain-proxy fallback type for FC entity classes with no
     * dedicated registration. Replaces EntityProxyFactory's old
     * EntityType.MARKER fallback (MARKER has clientTrackingRange 0 and 0x0
     * size, so such proxies were never sent to any client and had no
     * hitbox). Deliberately NOT put into fcClassToEntityType: it must not
     * shadow the class-hierarchy walk in {@link #getEntityType} (living
     * entities need types with registered attributes).
     */
    public static EntityType<?> getGenericPlainType() {
        if (genericPlainType == null) return null;
        ResourceLocation key = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(genericPlainType);
        if (key != null) {
            EntityType<?> registryType = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(key);
            if (registryType != null) return registryType;
        }
        return genericPlainType;
    }

    private static void registerGenericPlainType() {
        try {
            EntityType<ProxyEntity> type = EntityType.Builder
                    .<ProxyEntity>of(ProxyEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .build(BTWForgeMod.MOD_ID + ":fc_entity");

            ResourceLocation key = new ResourceLocation(BTWForgeMod.MOD_ID, "fc_entity");
            if (currentEvent != null) {
                currentEvent.register(net.minecraftforge.registries.ForgeRegistries.Keys.ENTITY_TYPES,
                        key, () -> type);
            } else {
                Registry.register(BuiltInRegistries.ENTITY_TYPE, key, type);
            }
            genericPlainType = type;
        } catch (Exception e) {
            LOGGER.error("Failed to register generic fallback entity type 'fc_entity': {}",
                    e.getMessage());
        }
    }

    private static int registerPlainEntity(String id, String fcClassName,
                                            MobCategory category,
                                            float width, float height) {
        try {
            Class<?> fcClass = findFcClass(fcClassName);
            if (fcClass == null) return 0;

            EntityType<ProxyEntity> type = EntityType.Builder
                    .<ProxyEntity>of(ProxyEntity::new, category)
                    .sized(width, height)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .build(BTWForgeMod.MOD_ID + ":" + id);

            ResourceLocation key = new ResourceLocation(BTWForgeMod.MOD_ID, id);
            if (currentEvent != null) {
                currentEvent.register(net.minecraftforge.registries.ForgeRegistries.Keys.ENTITY_TYPES,
                        key, () -> type);
            } else {
                Registry.register(BuiltInRegistries.ENTITY_TYPE, key, type);
            }
            fcClassToEntityType.put(fcClass, type);
            entityTypeToFcClassName.put(type, fcClass.getName());
            return 1;
        } catch (Exception e) {
            LOGGER.error("Failed to register plain entity type '{}': {}",
                    id, e.getMessage());
            return 0;
        }
    }

    // ------------------------------------------------------------------
    // FC class resolution
    // ------------------------------------------------------------------

    /**
     * Attempts to find the FC entity class by short name.  Tries the
     * known package paths used by the FC mod.
     */
    private static Class<?> findFcClass(String simpleName) {
        String[] packages = {
            "net.minecraft.src.btw.entity.",
            "net.minecraft.src.btw.core.",
            "net.minecraft.src.",
            // Frozen vanilla 1.5.2 classes (EntityXPOrb, EntityArrow, ...)
            // live under btw.modern at runtime (fc source set).
            "btw.modern."
        };
        for (String pkg : packages) {
            try {
                return Class.forName(pkg + simpleName);
            } catch (ClassNotFoundException ignored) {
                // Try next package
            }
        }
        // FC entity classes are loaded at runtime via the FC classloader.
        // If not found now, they may appear later when the server starts.
        LOGGER.debug("FC entity class '{}' not found at registration time "
                + "(may be loaded later via FC classloader)", simpleName);
        return null;
    }

    // ------------------------------------------------------------------
    // FC class hierarchy checks
    // ------------------------------------------------------------------

    private static boolean isAnimalClass(Class<?> fcClass) {
        return btw.modern.EntityAnimal.class.isAssignableFrom(fcClass)
                || btw.modern.EntityTameable.class.isAssignableFrom(fcClass);
    }

    private static boolean isMobClass(Class<?> fcClass) {
        return btw.modern.EntityMob.class.isAssignableFrom(fcClass)
                || btw.modern.EntityFlying.class.isAssignableFrom(fcClass)
                || btw.modern.EntitySlime.class.isAssignableFrom(fcClass)
                || btw.modern.EntityAmbientCreature.class.isAssignableFrom(fcClass);
    }

    private static boolean isPathfinderMobClass(Class<?> fcClass) {
        return btw.modern.EntityCreature.class.isAssignableFrom(fcClass)
                || btw.modern.EntityGolem.class.isAssignableFrom(fcClass)
                || btw.modern.EntityWaterMob.class.isAssignableFrom(fcClass)
                || btw.modern.EntityAgeable.class.isAssignableFrom(fcClass);
    }
}
