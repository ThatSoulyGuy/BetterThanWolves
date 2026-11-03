package btw.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Replaces vanilla mobs with their FC proxy equivalents, so naturally spawning, spawner, structure,
 * spawn-egg and disk-loaded mobs all get FC's AI, drops and behaviour (1.5.2
 * EntityList.ReplaceExistingMapping, FCBetterThanWolves.java:1979-2003).
 *
 * <p>The join event only cancels the vanilla entity and queues it. The replacement itself runs at
 * the end of the server tick (and once more on server stop, before the final chunk save): worldgen
 * entities join from LevelChunk.runPostLoad inside ChunkMap.protoChunkToFullChunk, before the chunk
 * is FULL, and FC's spawn init / state transfer read the world.</p>
 *
 * <p>Each queued mob becomes its {@code betterthanwolves:fc_*} proxy at the same position, rotation
 * and UUID, with FC state set up before it is added (so the spawn packet already carries it):</p>
 * <ul>
 *   <li>Fresh spawns that 1.5.2 initialised (natural, worldgen, spawner, structure, egg, command...)
 *       run FC's own spawn init: PreInitCreature + initCreature at SpawnerAnimals.java:203/211/272,
 *       PreInitCreature + SpawnerInitCreature at MobSpawnerBaseLogic.java:137/219, initCreature at
 *       ItemMonsterPlacer.java:104. That is what gives FC sheep their fleece colours, skeletons their
 *       bows, villagers their professions and zombies their armour.</li>
 *   <li>Disk loads, breeding, conversions, buckets and spawns without finalizeSpawn carry the vanilla
 *       mob's state over instead (name, persistence, health, age, colours, saddle, size, tame state,
 *       profession, equipment), as 1.5.2 loading a vanilla save through FC readEntityFromNBT did.</li>
 * </ul>
 *
 * <p>Every proxy made here counts for natural-spawn caps in the replaced vanilla mob's MobCategory
 * (W7, see ProxyMob.getClassification): a glow squid stays UNDERGROUND_WATER_CREATURE as an fc_squid.</p>
 *
 * <p>Proxy entities ({@link ProxyMob}, {@link ProxyAnimal}, {@link ProxyPathfinderMob},
 * {@link ProxyEntity}) are never replaced.</p>
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

        // --- 1.20.1-only variants of 1.5.2 mobs (SW5). 1.5.2 has only EntityZombie (villager flag),
        // EntitySkeleton (type 0 / 1 = wither) and EntitySquid; see applyVariant. ---
        REPLACEMENTS.put(EntityType.ZOMBIE_VILLAGER, new ResourceLocation(ns, "fc_zombie"));
        REPLACEMENTS.put(EntityType.HUSK,            new ResourceLocation(ns, "fc_zombie"));
        REPLACEMENTS.put(EntityType.DROWNED,         new ResourceLocation(ns, "fc_zombie"));
        REPLACEMENTS.put(EntityType.STRAY,           new ResourceLocation(ns, "fc_skeleton"));
        REPLACEMENTS.put(EntityType.WITHER_SKELETON, new ResourceLocation(ns, "fc_skeleton"));
        REPLACEMENTS.put(EntityType.GLOW_SQUID,      new ResourceLocation(ns, "fc_squid"));

        // --- Ambient / passive ---
        REPLACEMENTS.put(EntityType.BAT,        new ResourceLocation(ns, "fc_bat"));
        REPLACEMENTS.put(EntityType.SNOW_GOLEM, new ResourceLocation(ns, "fc_snowman"));
        REPLACEMENTS.put(EntityType.VILLAGER,   new ResourceLocation(ns, "fc_villager"));
        REPLACEMENTS.put(EntityType.SQUID,      new ResourceLocation(ns, "fc_squid"));
    }

    /** A cancelled vanilla mob waiting for its FC proxy; {@code fcId == null}: dropped, only discarded. */
    private record Pending(ServerLevel level, Entity vanilla, ResourceLocation fcId, boolean fromDisk,
                           @Nullable StructureStart livePlacement) {}

    /** Filled by the join handler, drained on the server thread at the end of each server tick. */
    private static final Queue<Pending> PENDING = new ConcurrentLinkedQueue<>();

    /** Once-per-key log gate. */
    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();

    private static final Class<?>[] NONE = {};
    private static final Class<?>[] INT = {int.class};
    private static final Class<?>[] BOOL = {boolean.class};
    private static final Class<?>[] STR = {String.class};

    /** 1.5.2 equipment[] order: 0 held, 1 boots, 2 legs, 3 chest, 4 helmet (EntityLiving.java:2730). */
    private static final EquipmentSlot[] FC_SLOTS = {
            EquipmentSlot.MAINHAND, EquipmentSlot.FEET, EquipmentSlot.LEGS,
            EquipmentSlot.CHEST, EquipmentSlot.HEAD};

    /** FCEntityVillager.m_iNumProfessionTypes (Server FCEntityVillager.java:27). */
    private static final int FC_PROFESSION_COUNT = 5;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        Entity entity = event.getEntity();

        // Never replace entities that are already FC proxies (also the ones drain() adds)
        if (entity instanceof ProxyMob
                || entity instanceof ProxyAnimal
                || entity instanceof ProxyPathfinderMob
                || entity instanceof ProxyEntity) {
            return;
        }

        // FCMOD-INTEGRATION(1.20.1): zombified_villages: a new village gets no 1.20.1 iron golem (4.B removed village golems, Village.java:80-97); decision D1 (a1)
        if (entity.getType() == EntityType.IRON_GOLEM && !event.loadedFromDisk() && entity instanceof Mob golem
                && !golem.isSpawnCancelled() && golem.getSpawnType() == MobSpawnType.STRUCTURE) {
            VillageInhabitants.queueGolem(level, golem, VillageInhabitants.livePlacement());
            return;   // joins as usual; checked at the end of the tick
        }

        ResourceLocation fcId = REPLACEMENTS.get(entity.getType());
        if (fcId == null) return;

        // Spawn already refused (e.g. onFinalizeSpawn's spawner cap): Forge's builtinMobSpawnBlocker
        // (same priority) cancels the join; it must not be replaced either.
        if (entity instanceof Mob sm && sm.isSpawnCancelled()) return;

        // ENTITY_TYPES is a defaulted registry: getValue(missing) returns PIG
        // (ForgeRegistry.java:205-213), so a failed fc_* registration must be caught here.
        if (!ForgeRegistries.ENTITY_TYPES.containsKey(fcId)) {
            if (LOGGED.add("missing|" + fcId)) {
                LOGGER.warn("FC entity type {} not registered; leaving vanilla {}", fcId, entity.getType());
            }
            return;
        }

        // Cancel the vanilla entity (it never reaches addEntityUuid, so its UUID is reused below)
        event.setCanceled(true);

        // 1.20.1 finalizeSpawn jockey extras (spider-jockey skeletons, chicken-jockey chickens) are
        // dropped: FC decides its own jockeys (FCEntitySpider.initCreature, Server FCEntitySpider.java:56-68).
        // Still queued, so drain() discards them (R5) once the chunk is FULL.
        if (!event.loadedFromDisk() && entity instanceof Mob m && isFinalizeSpawnExtra(m)) {
            PENDING.add(new Pending(level, entity, null, false, null));
            return;
        }

        // Deferred: worldgen entities join from LevelChunk.runPostLoad inside
        // ChunkMap.protoChunkToFullChunk (loadedFromDisk=false, chunk not yet FULL), and the
        // FC init / state transfer below touch the world.
        PENDING.add(new Pending(level, entity, fcId, event.loadedFromDisk(),
                VillageInhabitants.candidate(entity, event.loadedFromDisk()) ? VillageInhabitants.livePlacement() : null));
    }

    /**
     * Riders and mounts that a 1.20.1 finalizeSpawn added: chicken-jockey chickens (MobSpawnType.JOCKEY,
     * Zombie.java:445) and spider-jockey skeletons, which Spider.finalizeSpawn (Spider.java:149-155)
     * finalizes with the spider's own spawn type, not JOCKEY. A skeleton riding a spider from NBT
     * (/summon Passengers, spawner data) skipped finalizeSpawn (spawn type null) and is kept.
     */
    private static boolean isFinalizeSpawnExtra(Mob m) {
        MobSpawnType type = m.getSpawnType();
        if (type == MobSpawnType.JOCKEY) return true;
        return type != null && m.getType() == EntityType.SKELETON
                && m.getVehicle() instanceof Spider spider && spider.getSpawnType() == type;
    }

    /**
     * R2: BaseSpawner caps its mobs by counting the spawned entity's class (BaseSpawner.java:125,
     * maxNearbyEntities 6 within spawnRange 4), but every replaced mob becomes a proxy, so the count of
     * e.g. Zombie.class stays 0 and the spawner never stops. 1.5.2 counted the FC class through
     * ReplaceExistingMapping (MobSpawnerBaseLogic.java:122-128). Count the FC type, plus mobs still
     * queued for replacement, and refuse the spawn at the cap; Forge's builtinMobSpawnBlocker then
     * cancels the join (Mob.setSpawnCancelled).
     */
    @SubscribeEvent
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        BaseSpawner spawner = event.getSpawner();
        if (spawner == null || !(event.getLevel() instanceof ServerLevel level)) return;
        Mob mob = event.getEntity();
        ResourceLocation fcId = REPLACEMENTS.get(mob.getType());
        if (fcId == null || !ForgeRegistries.ENTITY_TYPES.containsKey(fcId)) return;
        try {
            int range = 4;
            int max = 6;
            try {
                CompoundTag cfg = spawner.save(new CompoundTag()); // the fields are private
                if (cfg.contains("SpawnRange")) range = cfg.getShort("SpawnRange");
                if (cfg.contains("MaxNearbyEntities")) max = cfg.getShort("MaxNearbyEntities");
            } catch (Throwable ignored) {
                // keep BaseSpawner's defaults
            }
            BlockPos pos = spawner.getSpawnerBlockEntity() != null ? spawner.getSpawnerBlockEntity().getBlockPos()
                    : spawner.getSpawnerEntity() != null ? spawner.getSpawnerEntity().blockPosition()
                    : mob.blockPosition();
            AABB box = new AABB(pos).inflate(range);
            int nearby = countOf(level, ForgeRegistries.ENTITY_TYPES.getValue(fcId), box);
            for (Pending q : PENDING) {
                if (q.level() == level && fcId.equals(q.fcId()) && box.intersects(q.vanilla().getBoundingBox())) {
                    nearby++;
                }
            }
            if (nearby >= max) {
                event.setSpawnCancelled(true); // blocks the join
                event.setCanceled(true);       // and skips finalizeSpawn (no jockey extras)
                if (LOGGED.add("spawnercap|" + fcId)) {
                    LOGGER.info("[MobReplacer] spawner at {} reached its cap ({} {} nearby); logged once per type",
                            pos, nearby, fcId);
                }
            }
        } catch (Throwable t) {
            if (LOGGED.add("spawnercapfail|" + t.getClass().getName())) {
                LOGGER.warn("Spawner cap check for {} failed: {}", fcId, t.toString());
            }
        }
    }

    private static <T extends Entity> int countOf(ServerLevel level, EntityType<T> type, AABB box) {
        return type == null ? 0 : level.getEntities(type, box, EntitySelector.NO_SPECTATORS).size();
    }

    /**
     * W7: logs once which REPLACEMENTS cross MobCategories (today glow_squid UNDERGROUND_WATER_CREATURE ->
     * fc_squid WATER_CREATURE). Such a replacement keeps counting in its vanilla category for natural-spawn
     * caps (setReplacedCategory below), so the line is information, not an error. Registries are frozen
     * by server start.
     */
    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        if (!LOGGED.add("categories")) return;
        try {
            for (Map.Entry<EntityType<?>, ResourceLocation> e : REPLACEMENTS.entrySet()) {
                if (!ForgeRegistries.ENTITY_TYPES.containsKey(e.getValue())) continue;
                EntityType<?> fcType = ForgeRegistries.ENTITY_TYPES.getValue(e.getValue());
                MobCategory vanillaCategory = e.getKey().getCategory();
                if (fcType != null && fcType.getCategory() != vanillaCategory) {
                    LOGGER.info("[MobReplacer] {} ({}) -> {} ({}): replacements count as {} for natural-spawn caps",
                            EntityType.getKey(e.getKey()), vanillaCategory.getName(), e.getValue(),
                            fcType.getCategory().getName(), vanillaCategory.getName());
                }
            }
        } catch (Throwable t) {
            LOGGER.warn("Replacement spawn-category check failed: {}", t.toString());
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) drain();
    }

    /** Fired after the tick loop and before stopServer()'s final saveAllChunks (MinecraftServer.java:671 vs :584). */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        drain();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PENDING.clear();
    }

    /** Replaces everything queued so far; mobs queued while this runs wait for the next tick. */
    private static void drain() {
        VillageInhabitants.clearStalePlacement();
        VillageInhabitants.drainGolems();
        if (PENDING.isEmpty()) return;
        List<Pending> batch = new ArrayList<>();
        for (Pending p; (p = PENDING.poll()) != null; ) batch.add(p);
        for (Pending p : batch) {
            try {
                replace(p);
            } catch (Throwable t) {
                if (LOGGED.add("fail|" + p.fcId() + "|" + t.getClass().getName())) {
                    LOGGER.error("Failed to replace {} with {}: {}", p.vanilla().getType(), p.fcId(), t.toString(), t);
                }
            }
        }
    }

    private static void replace(Pending p) {
        try {
            if (p.fcId() != null) replaceWithProxy(p);
        } finally {
            discardCancelled(p.vanilla());
        }
    }

    /**
     * R5: the cancelled vanilla entity was never added, but code that kept a reference to it still
     * sees a live entity: Raid.joinRaid registers a raider before addFreshEntityWithPassengers, and
     * Raid.updateRaiders only drops removed raiders (or ones with tickCount > 600), so a cancelled
     * witch stalled the wave. Its levelCallback is EntityInLevelCallback.NULL, and removal sets the
     * removal reason before dismounting, so LivingEntity.dismountVehicle reads no blocks.
     */
    private static void discardCancelled(Entity vanilla) {
        try {
            if (!vanilla.isRemoved()) vanilla.discard();
        } catch (Throwable t) {
            if (LOGGED.add("discard|" + vanilla.getType() + "|" + t.getClass().getName())) {
                LOGGER.warn("Discarding the cancelled {} failed: {}", vanilla.getType(), t.toString());
            }
        }
    }

    private static void replaceWithProxy(Pending p) {
        ServerLevel level = p.level();
        Entity vanilla = p.vanilla();
        if (!level.isLoaded(vanilla.blockPosition())) {
            if (LOGGED.add("unloaded|" + p.fcId())) {
                LOGGER.warn("Chunk at {} unloaded before {} became {}; dropped (logged once per type)",
                        vanilla.blockPosition(), vanilla.getType(), p.fcId());
            }
            return;
        }
        // FCMOD-INTEGRATION(1.20.1): zombified_villages: a village's fresh villagers follow ComponentVillage.spawnVillagers (4.B)
        VillageInhabitants.Decision village = null;
        if (VillageInhabitants.candidate(vanilla, p.fromDisk())) {
            try {
                village = VillageInhabitants.decide(level, (Mob) vanilla, p.livePlacement());
            } catch (Throwable t) {
                if (LOGGED.add("village|" + t.getClass().getName())) {
                    LOGGER.warn("Village rule failed for {} at {}: {}", vanilla.getType(), vanilla.blockPosition(), t.toString());
                }
            }
        }
        if (village != null && village.kind() == VillageInhabitants.Kind.DROP) {
            if (LOGGED.add("villagedrop|" + village.abandonment())) {
                LOGGER.info("[VILLAGES] {} at {} not spawned: village level {}, class {} (logged once per level)",
                        EntityType.getKey(vanilla.getType()), vanilla.blockPosition(), village.abandonment(), village.fcClass());
            }
            return;   // replace()'s finally discards the cancelled vanilla mob (R5)
        }
        ResourceLocation fcId = village == null ? p.fcId() : new ResourceLocation(BTWForgeMod.MOD_ID,
                village.kind() == VillageInhabitants.Kind.VILLAGER ? "fc_villager" : "fc_zombie");

        // Riding is not carried over: detach the cancelled entity from vehicles/passengers that are
        // in the world (e.g. a skeleton-trap horse) so they don't keep a reference to it.
        try {
            vanilla.ejectPassengers();
            vanilla.stopRiding();
        } catch (Throwable ignored) {
            // cosmetic only
        }

        EntityType<?> fcType = ForgeRegistries.ENTITY_TYPES.getValue(fcId);
        Entity proxy = fcType == null ? null : fcType.create(level);
        if (proxy == null) return;
        proxy.moveTo(vanilla.getX(), vanilla.getY(), vanilla.getZ(), vanilla.getYRot(), vanilla.getXRot());
        proxy.setUUID(vanilla.getUUID()); // the cancelled copy never reached addEntityUuid
        // W7: every origin (natural, worldgen, disk, spawner, egg...) counts in the vanilla mob's category,
        // set before addFreshEntity so the next NaturalSpawner.createState already sees it.
        setReplacedCategory(proxy, vanilla.getType().getCategory());

        // getFcEntity() -> ensureFcEntity seeds the FC entity's position/rotation from moveTo above.
        btw.modern.EntityLiving fc = fcEntityOf(proxy);
        if (fc != null) {
            if (vanilla instanceof LivingEntity vl) {
                fc.rotationYawHead = vl.getYHeadRot();
                fc.renderYawOffset = vl.yBodyRot;
            }
            transferIdentity(vanilla, proxy, fc);
            MobSpawnType origin = vanilla instanceof Mob m ? m.getSpawnType() : null;
            if (village != null) {
                applyVillageDecision(vanilla, proxy, fc, village);
            } else if (!p.fromDisk() && usesFcSpawnInit(origin)) {
                runFcSpawnInit(level, fc, vanilla, origin);
            } else {
                applyVariant(level, vanilla, fc, true);
                transferState(level, vanilla, proxy, fc, p.fromDisk());
            }
            // FC init / transfer may have changed FC's max and current health (slime size, wither
            // summon charge, loaded health): re-seed MC MAX_HEALTH/Health and the D6 health link.
            reseedFromFc(proxy, fc);
        }

        // init-before-add (as ItemMonsterPlacer.java:104 / MobSpawnerBaseLogic.java:219) so the spawn
        // packet (writeSpawnData -> FCEntityStateCodec) already carries FC's colour/profession/equipment.
        if (!level.addFreshEntity(proxy)) {
            LOGGER.warn("addFreshEntity rejected {} for {}", fcId, vanilla.getType());
            return;
        }
        if (village != null && LOGGED.add("village|" + village.kind() + "|" + village.abandonment())) {
            LOGGER.info("[VILLAGES] {} -> {} (village level {}, class {}{})", EntityType.getKey(vanilla.getType()), fcId,
                    village.abandonment(), village.fcClass(), village.dirtyPeasant() ? ", dirty" : "");
        }
        if (LOGGED.add("ok|" + vanilla.getType() + "|" + p.fromDisk())) {
            LOGGER.info("[MobReplacer] {} -> {} ({})", EntityType.getKey(vanilla.getType()), fcId,
                    p.fromDisk() ? "from disk: state transferred"
                            : String.valueOf(vanilla instanceof Mob m ? m.getSpawnType() : null));
        }
    }

    private static btw.modern.EntityLiving fcEntityOf(Entity proxy) {
        if (proxy instanceof ProxyAnimal pa) return pa.getFcEntity();
        if (proxy instanceof ProxyMob pm) return pm.getFcEntity();
        if (proxy instanceof ProxyPathfinderMob pp) return pp.getFcEntity();
        return null;
    }

    /** W7: the replaced category the proxy reports for spawn counting — see ProxyMob.getClassification. */
    private static void setReplacedCategory(Entity proxy, MobCategory category) {
        if (proxy instanceof ProxyAnimal pa) pa.setReplacedCategory(category);
        else if (proxy instanceof ProxyMob pm) pm.setReplacedCategory(category);
        else if (proxy instanceof ProxyPathfinderMob pp) pp.setReplacedCategory(category);
    }

    /** setFcEntity re-seeds MC MAX_HEALTH/Health from FC and marks the health link (same FC entity). */
    private static void reseedFromFc(Entity proxy, btw.modern.EntityLiving fc) {
        if (proxy instanceof ProxyAnimal pa) pa.setFcEntity(fc);
        else if (proxy instanceof ProxyMob pm) pm.setFcEntity(fc);
        else if (proxy instanceof ProxyPathfinderMob pp) pp.setFcEntity(fc);
    }

    /**
     * Origins for which 1.5.2 ran FC's spawn initialiser (SpawnerAnimals.java:203/211/272,
     * MobSpawnerBaseLogic.java:137/219, ItemMonsterPlacer.java:104,
     * ComponentScatteredFeatureSwampHut.java:172-175). Breeding, conversion, bucket and spawns that
     * skipped finalizeSpawn (spawnType null: /summon with NBT, WitherSkullBlock, pumpkin golems) carry
     * the vanilla state instead; JOCKEY never gets here.
     */
    private static boolean usesFcSpawnInit(MobSpawnType origin) {
        if (origin == null) return false;
        return switch (origin) {
            case BREEDING, CONVERSION, BUCKET, JOCKEY -> false;
            default -> true;
        };
    }

    private static void runFcSpawnInit(ServerLevel level, btw.modern.EntityLiving fc, Entity vanilla, MobSpawnType origin) {
        try {
            // PreInitCreature only where 1.5.2 called it: natural and worldgen spawning
            // (SpawnerAnimals.java:203/323) and spawners (MobSpawnerBaseLogic.java:137) -- e.g. the
            // nether wither-skeleton roll (Server FCEntitySkeleton.java:54-61). Eggs only ran initCreature.
            if (origin == MobSpawnType.NATURAL || origin == MobSpawnType.CHUNK_GENERATION
                    || origin == MobSpawnType.SPAWNER) {
                fc.PreInitCreature();
            }
            // Before initCreature: a wither skeleton's type selects its stone sword there.
            // R6: BTW never spawned villager zombies (FCEntityZombie.initCreature skips EntityZombie's
            // villager roll, Server FCEntityZombie.java:183-191; they only come from killed villagers,
            // onKillEntity :154-180), so only an explicitly requested zombie villager (egg, command,
            // dispenser) keeps the villager flag; natural/worldgen/spawner/structure ones are plain
            // FC zombies.
            applyVariant(level, vanilla, fc, origin == MobSpawnType.SPAWN_EGG
                    || origin == MobSpawnType.COMMAND || origin == MobSpawnType.DISPENSER);
            if (origin == MobSpawnType.SPAWNER) {
                fc.SpawnerInitCreature(); // MobSpawnerBaseLogic.java:216-219 (no spawner armour/bows)
            } else {
                fc.initCreature();
            }
        } catch (Throwable t) {
            if (LOGGED.add("init|" + fc.getClass().getName())) {
                LOGGER.warn("FC initCreature threw for {}: {}", fc.getClass().getSimpleName(), t.toString());
            }
        }
        // 1.5.2 villages pick the profession by building (ComponentVillage.java:164-170), not at random.
        if (vanilla instanceof Villager v) {
            int prof = fcProfession(v.getVillagerData().getProfession());
            if (prof >= 0) fcCall(fc, "setProfession", INT, prof);
        }
    }

    /**
     * SW5: the 1.20.1-only variants as their 1.5.2 representation on the FC base mob. Husk, drowned,
     * stray and glow squid are plain FC zombies / skeletons / squid. A zombie villager is an FC
     * villager zombie only when {@code villagerIdentity} (existing mob, or an explicit request);
     * otherwise it stays a plain FC zombie (R6).
     */
    private static void applyVariant(ServerLevel level, Entity vanilla, btw.modern.EntityLiving fc,
                                     boolean villagerIdentity) {
        EntityType<?> type = vanilla.getType();
        if (type == EntityType.WITHER_SKELETON) {
            // EntitySkeleton.setSkeletonType (vanilla/server EntitySkeleton.java:361-375): type 1 is the
            // 1.5.2 wither skeleton (fire immune, 0.72x2.34, stone sword from FCEntitySkeleton.initCreature).
            fcCall(fc, "setSkeletonType", INT, 1);
        } else if (type == EntityType.ZOMBIE_VILLAGER && villagerIdentity) {
            fcCall(fc, "setVillager", BOOL, true); // EntityZombie.java:124 / FCEntityZombie.java:79
            // FCEntityZombie.ModSpecificOnLivingUpdate (Server FCEntityZombie.java:250-256) turns a
            // villager zombie without a villager class back into a plain zombie; the class is also the
            // profession a cure restores (convertToVillager :211-214).
            int prof = vanilla instanceof ZombieVillager zv ? fcProfession(zv.getVillagerData().getProfession()) : -1;
            if (prof < 0) prof = level.getRandom().nextInt(FC_PROFESSION_COUNT);
            setIntField(fc, "m_iVillagerClass", prof);
        }
    }

    /**
     * ComponentVillage.spawnVillagers (vanilla/server ComponentVillage.java:178-198): no FC spawn init, as 4.B built them
     * with new FCEntityZombie / new FCEntityVillager(world, type). FCMOD-INTEGRATION(1.20.1): zombified_villages
     */
    private static void applyVillageDecision(Entity vanilla, Entity proxy, btw.modern.EntityLiving fc,
                                             VillageInhabitants.Decision d) {
        boolean baby = vanilla instanceof AgeableMob am ? am.isBaby() : vanilla instanceof Zombie z && z.isBaby();
        if (d.kind() == VillageInhabitants.Kind.VILLAGER_ZOMBIE) {
            setIntField(fc, "m_iVillagerClass", d.fcClass());
            fc.SetPersistent(true);
            if (proxy instanceof Mob pm) pm.setPersistenceRequired();
            fcCall(fc, "setVillager", BOOL, true);
            if (baby) fcCall(fc, "setChild", BOOL, true);
        } else {
            fcCall(fc, "setProfession", INT, d.fcClass());
            if (d.dirtyPeasant()) fcCall(fc, "SetDirtyPeasant", INT, 1);
            if (baby && vanilla instanceof AgeableMob va && fc instanceof btw.modern.EntityAgeable fa) {
                fa.setGrowingAge(va.getAge());
            }
        }
    }

    private static void transferIdentity(Entity vanilla, Entity proxy, btw.modern.EntityLiving fc) {
        if (vanilla.hasCustomName()) {
            Component name = vanilla.getCustomName();
            proxy.setCustomName(name);
            proxy.setCustomNameVisible(vanilla.isCustomNameVisible());
            if (name != null) {
                fcCall(fc, "func_94058_c", STR, name.getString()); // EntityLiving.java:3104 (not on the stub)
            }
        }
        if (vanilla instanceof Mob vm && vm.isPersistenceRequired()) {
            if (proxy instanceof Mob pm) pm.setPersistenceRequired();
            fc.SetPersistent(true); // EntityLiving.java:3545, honoured by FC despawnEntity
        }
    }

    /** Emulates 1.5.2 ReplaceExistingMapping loading a vanilla save through FC readEntityFromNBT. */
    private static void transferState(ServerLevel level, Entity vanilla, Entity proxy,
                                      btw.modern.EntityLiving fc, boolean fromDisk) {
        if (vanilla instanceof AgeableMob va && fc instanceof btw.modern.EntityAgeable fa) {
            fa.setGrowingAge(va.getAge()); // EntityAgeable.java:107; both use -24000 for a fresh baby
        }
        if (vanilla instanceof Zombie vz && vz.isBaby()) fcCall(fc, "setChild", BOOL, true); // EntityZombie.java:108
        if (vanilla instanceof Sheep s) {
            fcCall(fc, "setFleeceColor", INT, s.getColor().getId()); // EntitySheep.java:205; wool index == DyeColor id
            fcCall(fc, "setSheared", BOOL, s.isSheared());           // EntitySheep.java:222
        }
        if (vanilla instanceof Pig pig && pig.isSaddled()) fcCall(fc, "setSaddled", BOOL, true); // EntityPig.java:193
        // Slime and magma cube: setSlimeSize also resets FC health to the new max (EntitySlime.java:45-52).
        if (vanilla instanceof Slime sl) fcCall(fc, "setSlimeSize", INT, sl.getSize());
        if (vanilla instanceof Wolf w) {
            if (w.isTame()) {
                fcCall(fc, "setTamed", BOOL, true);                         // EntityTameable.java:95
                String owner = ownerName(level, w.getOwnerUUID());
                if (owner != null) fcCall(fc, "setOwner", STR, owner);     // EntityTameable.java:133 (owners are names)
                if (w.isOrderedToSit()) {
                    Object aiSit = fcCall(fc, "func_70907_r", NONE);        // EntityTameable.java:143
                    if (aiSit != null) fcCall(aiSit, "setSitting", BOOL, true);
                    fcCall(fc, "setSitting", BOOL, true);                   // as readEntityFromNBT, EntityTameable.java:65-66
                }
            } else if (w.isAngry()) {
                fcCall(fc, "setAngry", BOOL, true);                         // EntityWolf.java:423
            }
            fcCall(fc, "setCollarColor", INT, w.getCollarColor().getId()); // EntityWolf.java:448
        }
        if (vanilla instanceof Villager v) {
            int prof = fcProfession(v.getVillagerData().getProfession());
            if (prof >= 0) {
                fcCall(fc, "setProfession", INT, prof);                     // EntityVillager.java:265
            } else {
                try {
                    fc.initCreature();                                      // FCEntityVillager.java:326-329 random pick
                } catch (Throwable t) {
                    if (LOGGED.add("init|" + fc.getClass().getName())) {
                        LOGGER.warn("FC initCreature threw for {}: {}", fc.getClass().getSimpleName(), t.toString());
                    }
                }
            }
        }
        if (vanilla instanceof WitherBoss wb && wb.getInvulnerableTicks() > 0) {
            fcCall(fc, "func_82206_m", NONE); // summon charge: BlockSkull.java:210/249 -> EntityWither.java:394
        }
        // Equipment in 1.5.2 slot order (/summon NBT, conversions and old saves; fresh spawns use FC init).
        // Setting slot 0 re-picks the skeleton's ranged/melee task (EntitySkeleton.java:404-410).
        // R4: with its drop chance, as readEntityFromNBT's DropChances (EntityLiving.java:1761-1768);
        // picked-up gear is 2.0 (guaranteed, undamaged) in both versions (Mob.setGuaranteedDrop :604-611,
        // EntityLiving.java:2009 / dropEquipment :2751-2753), and ProxyMob leaves the drop to FC.
        if (vanilla instanceof Mob vm) {
            CompoundTag saved = null;
            for (int i = 0; i < FC_SLOTS.length; i++) {
                ItemStack st = vm.getItemBySlot(FC_SLOTS[i]);
                if (st.isEmpty()) continue;
                try {
                    btw.modern.ItemStack fcSt = ItemStackHelper.toFcStack(st);
                    if (fcSt != null) {
                        fc.setCurrentItemOrArmor(i, fcSt);
                        if (saved == null) saved = dropChanceData(vm);
                        float chance = dropChance(saved, FC_SLOTS[i]);
                        if (chance >= 0.0F && i < fc.equipmentDropChances.length) {
                            fc.equipmentDropChances[i] = chance;
                        }
                    }
                } catch (Throwable t) {
                    if (LOGGED.add("equip|" + fc.getClass().getName() + "|" + t.getClass().getName())) {
                        LOGGER.warn("FC equipment transfer for {} failed: {}", fc.getClass().getSimpleName(), t.toString());
                    }
                }
            }
        }
        // Health last (slime size and the wither charge above reset FC health): the loaded mob keeps
        // its share of its max health.
        if (fromDisk && vanilla instanceof LivingEntity vl && proxy instanceof LivingEntity pl) {
            int fcMax = fc.getMaxHealth();
            if (fcMax > 0 && vl.getMaxHealth() > 0) {
                int hp = Math.max(1, Math.min(fcMax, Math.round(vl.getHealth() / vl.getMaxHealth() * fcMax)));
                fc.health = hp;
                pl.setHealth(hp);
            }
        }
    }

    /** 1.20.1 profession -> FC 0 farmer, 1 librarian, 2 priest, 3 blacksmith, 4 butcher; -1 = let FC pick. */
    static int fcProfession(VillagerProfession p) {
        if (p == VillagerProfession.LIBRARIAN || p == VillagerProfession.CARTOGRAPHER) return 1;
        if (p == VillagerProfession.CLERIC) return 2;
        if (p == VillagerProfession.ARMORER || p == VillagerProfession.WEAPONSMITH
                || p == VillagerProfession.TOOLSMITH || p == VillagerProfession.MASON) return 3;
        if (p == VillagerProfession.BUTCHER || p == VillagerProfession.LEATHERWORKER) return 4;
        if (p == VillagerProfession.FARMER || p == VillagerProfession.FISHERMAN
                || p == VillagerProfession.SHEPHERD || p == VillagerProfession.FLETCHER) return 0;
        return -1;
    }

    /** The mob's saved HandDropChances / ArmorDropChances (Mob.java:393-406); Mob's getter is protected. */
    private static CompoundTag dropChanceData(Mob vm) {
        CompoundTag tag = new CompoundTag();
        try {
            vm.addAdditionalSaveData(tag);
        } catch (Throwable t) {
            if (LOGGED.add("dropchances|" + vm.getType() + "|" + t.getClass().getName())) {
                LOGGER.warn("Reading {} drop chances failed: {}", vm.getType(), t.toString());
            }
        }
        return tag;
    }

    /** 1.20.1 drop chance for {@code slot} (hand index 0-1, armour feet..head 0-3), or -1 if absent. */
    private static float dropChance(CompoundTag saved, EquipmentSlot slot) {
        ListTag list = saved.getList(slot.getType() == EquipmentSlot.Type.HAND
                ? "HandDropChances" : "ArmorDropChances", Tag.TAG_FLOAT);
        int idx = slot.getIndex();
        return idx < list.size() ? list.getFloat(idx) : -1.0F;
    }

    private static String ownerName(ServerLevel level, UUID id) {
        if (id == null) return null;
        var online = level.getServer().getPlayerList().getPlayer(id);
        if (online != null) return online.getGameProfile().getName();
        var cache = level.getServer().getProfileCache();
        return cache == null ? null : cache.get(id).map(com.mojang.authlib.GameProfile::getName).orElse(null);
    }

    /** Calls a public FC method the Modern-Common stubs don't declare (the frozen/FC class at runtime does). */
    private static Object fcCall(Object target, String name, Class<?>[] sig, Object... args) {
        try {
            return target.getClass().getMethod(name, sig).invoke(target, args);
        } catch (Throwable t) {
            Throwable c = t instanceof java.lang.reflect.InvocationTargetException ite && ite.getCause() != null
                    ? ite.getCause() : t;
            if (LOGGED.add("call|" + target.getClass().getSimpleName() + "#" + name)) {
                LOGGER.warn("FC {}.{} failed during vanilla->FC transfer: {}",
                        target.getClass().getSimpleName(), name, c.toString());
            }
            return null;
        }
    }

    private static void setIntField(Object target, String name, int value) {
        try {
            target.getClass().getField(name).setInt(target, value);
        } catch (Throwable t) {
            if (LOGGED.add("field|" + target.getClass().getSimpleName() + "#" + name)) {
                LOGGER.warn("FC {}.{} not settable during vanilla->FC transfer: {}",
                        target.getClass().getSimpleName(), name, t.toString());
            }
        }
    }

    private VanillaMobReplacer() {}
}
