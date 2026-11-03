package btw.forge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * FCMOD-INTEGRATION(1.20.1): zombified_villages (INTEGRATIONS #048)
 *
 * <p><b>Feature:</b> 1.20.1 jigsaw villages (MC 1.14; every biome style and its zombie variant), and the villagers and
 * zombie villagers their templates place with spawn type STRUCTURE, by world generation or {@code /place structure}.</p>
 *
 * <p><b>Conflict with FC:</b> 4.B spawns every village villager as a persistent BTW villager zombie with its building's
 * class, and empties the villages near the world spawn (vanilla/server ComponentVillage.java:121-204,
 * ComponentVillageStartPiece.java:89-120, FCUtilsHardcoreSpawn.java:30-86). Without this rule every village villager
 * became a plain BTW villager with a random profession, so trades came without the soul urn.</p>
 *
 * <p><b>Integration pattern:</b> C (rebalance, keep the feature), by ruling (2026-10-21): 1.20.1's villages stay, and
 * their inhabitants follow 4.B's rule, with 1.20.1 buildings standing in for 4.B's components.</p>
 *
 * <p><b>Rule:</b> the village is the {@code #minecraft:village} structure with a piece at the villager
 * ({@code StructureManager.getStructureWithPieceAt}), or the StructureStart that StructureStartPlacementMixin publishes
 * while a structure is placed into a live level. Its abandonment level comes from the start piece's distance to the
 * world spawn and FC's own radii (FCUtilsHardcoreSpawn, reached by name; x4 on large biomes): nearly empty inside the
 * inner radius (one building in 20 gets one dirty farmer), farmers, blacksmiths and butchers out to the outer one, every
 * class beyond. The building is the largest piece at the villager; the class is the villager's own profession, else the
 * building's nearest job-site block, else farmer. The result is a persistent villager zombie, or a villager in a world
 * created creative (WorldInfoBridge.isCreativeWorld). VanillaMobReplacer asks {@link #decide} before it picks the proxy
 * type. Server thread only.</p>
 *
 * <p><b>Disables (justification):</b> the 1.20.1 village iron golem, in every new village (ruling D1 (a1)): 4.B removed
 * village golems in every game mode (vanilla/server Village.java:80-97), and a vanilla golem ignores BTW zombies, leaving
 * a free iron source beside them. Outposts keep theirs.</p>
 *
 * <p><b>Alternatives considered and rejected:</b> zombifying by spawn type and position alone (no village, so no
 * per-village level and no buildings; igloo basements would be caught); professions from template names (fails for
 * modded houses); FC's random profession (loses 4.B's building classes, which the abandonment filter depends on).</p>
 */
@Mod.EventBusSubscriber(modid = BTWForgeMod.MOD_ID)
public final class VillageInhabitants {

    private static final Logger LOGGER = LogManager.getLogger("BTW-Villages");

    public enum Kind { VILLAGER_ZOMBIE, VILLAGER, DROP }

    /** What a fresh village villager becomes; {@code fcClass} is FC's villager class (0 farmer .. 4 butcher). */
    public record Decision(Kind kind, int fcClass, boolean dirtyPeasant, int abandonment) {}

    /** 4.B's base radii (FCUtilsHardcoreSpawn.java:30-31), used only when FC's own getters cannot be reached. */
    private static final double FALLBACK_ABANDONED = 2250D, FALLBACK_PARTIAL = 3000D;

    /** The StructureStart being placed straight into a live ServerLevel on this thread (/place structure, mods). */
    private static final ThreadLocal<StructureStart> LIVE_PLACEMENT = new ThreadLocal<>();

    /** Per village (StructureStart identity; weak: dies with its start chunk). Server thread only. */
    private static final Map<StructureStart, VillageState> VILLAGES = new WeakHashMap<>();

    private record GolemCheck(ServerLevel level, Mob golem, @Nullable StructureStart live) {}
    private static final Queue<GolemCheck> GOLEMS = new ConcurrentLinkedQueue<>();
    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();

    private static final class VillageState {
        final int abandonment;
        /** Level 2: the building's single slot was taken (4.B caps the count to 1, ComponentVillage.java:136-140). */
        final Map<StructurePiece, Boolean> slotTaken = new IdentityHashMap<>();
        VillageState(int abandonment) { this.abandonment = abandonment; }
    }

    /**
     * FC's FCUtilsHardcoreSpawn, by name: FC classes are not on Forge's compile classpath (Forge/build.gradle:1905-1908
     * compiles against :Modern-Common and :Api only; the fc source set is a runtime classes directory). Same pattern as
     * FCLoginHandler.java:121 and ProxyItem.FcToolClasses. A null method means the lookup failed (logged once).
     */
    private static final class FcRadii {
        static final Method ABANDONED, PARTIAL, MULTIPLIER;
        static {
            Method a = null, p = null, m = null;
            try {
                Class<?> c = Class.forName("net.minecraft.src.btw.util.FCUtilsHardcoreSpawn", true,
                        VillageInhabitants.class.getClassLoader());
                a = c.getMethod("GetAbandonedVillageRadius");
                p = c.getMethod("GetPartiallyAbandonedVillageRadius");
                m = c.getMethod("GetWorldTypeRadiusMultiplier");
            } catch (Throwable t) {
                LOGGER.warn("[VILLAGES] FCUtilsHardcoreSpawn not found ({}); villages use 2250/3000 blocks, no large-biomes x4",
                        t.toString());
            }
            ABANDONED = a;
            PARTIAL = p;
            MULTIPLIER = m;
        }

        /** Throws when the method is missing (null) or FC's getter throws (no MinecraftServer wired yet). */
        static double get(Method m) throws Exception {
            return ((Number) m.invoke(null)).doubleValue();
        }

        private FcRadii() {}
    }

    // ---- live placement (StructureStartPlacementMixin) ----

    public static void enterLivePlacement(WorldGenLevel level, StructureStart start) {
        if (level instanceof ServerLevel) LIVE_PLACEMENT.set(start);   // worldgen threads pass a WorldGenRegion: no-op
    }

    public static void exitLivePlacement(WorldGenLevel level) {
        if (level instanceof ServerLevel) LIVE_PLACEMENT.remove();
    }

    @Nullable
    public static StructureStart livePlacement() { return LIVE_PLACEMENT.get(); }

    /** End of every server tick: a placement that threw never reached RETURN. */
    static void clearStalePlacement() { LIVE_PLACEMENT.remove(); }

    // ---- the rule ----

    /** The fresh STRUCTURE villagers and zombie villagers this rule owns. */
    static boolean candidate(Entity e, boolean fromDisk) {
        return !fromDisk && e instanceof Mob m && m.getSpawnType() == MobSpawnType.STRUCTURE
                && (e.getType() == EntityType.VILLAGER || e.getType() == EntityType.ZOMBIE_VILLAGER);
    }

    /** null: not inside a village (igloo basements, structure blocks, /place template): VanillaMobReplacer's own rules. */
    @Nullable
    static Decision decide(ServerLevel level, Mob vanilla, @Nullable StructureStart live) {
        BlockPos pos = vanilla.blockPosition();
        StructureStart start = villageAt(level, pos, live);
        if (start == null) return null;
        VillageState v = VILLAGES.computeIfAbsent(start, s -> new VillageState(abandonmentLevel(level, startCentre(s))));
        StructurePiece building = buildingAt(start, pos);
        int fcClass = villagerClass(level, vanilla, building, pos);
        boolean dirty = false;
        if (v.abandonment > 1) {
            // ComponentVillage.java:128-144: 19 buildings in 20 stay empty; the 20th gets its first villager only, dirty
            boolean first = building != null && v.slotTaken.put(building, Boolean.TRUE) == null;
            if (!first || !buildingRoll(level, building)) return new Decision(Kind.DROP, fcClass, false, v.abandonment);
            dirty = true;
        }
        // ComponentVillage.java:170-176: level 1 keeps farmers, blacksmiths and butchers; level 2 farmers only
        if (fcClass != 0 && v.abandonment > 0 && (v.abandonment > 1 || (fcClass != 3 && fcClass != 4))) {
            return new Decision(Kind.DROP, fcClass, false, v.abandonment);
        }
        // ComponentVillage.java:178-198: villager zombies unless the world is creative (WorldInfoBridge.isCreativeWorld)
        boolean zombie = vanilla.getType() == EntityType.ZOMBIE_VILLAGER
                || !WorldInfoBridge.isCreativeWorld(level.getServer());
        return new Decision(zombie ? Kind.VILLAGER_ZOMBIE : Kind.VILLAGER, fcClass, dirty, v.abandonment);
    }

    /** ComponentVillageStartPiece.java:89-120 with FC's radii (FCUtilsHardcoreSpawn.java:61-69, x4 on large biomes). */
    public static int abandonmentLevel(ServerLevel level, BlockPos centre) {
        BlockPos spawn = level.getSharedSpawnPos();             // 4.B: world.getWorldInfo().getSpawnX/Z
        double dx = spawn.getX() - centre.getX(), dz = spawn.getZ() - centre.getZ();
        double d2 = dx * dx + dz * dz;
        double full, partial;
        try {
            full = FcRadii.get(FcRadii.ABANDONED);
            partial = FcRadii.get(FcRadii.PARTIAL);
        } catch (Throwable t) {                                  // FC not linked or not wired: 4.B's base radii
            if (LOGGED.add("radii")) {
                LOGGER.warn("[VILLAGES] FC radii unavailable ({}); using 2250/3000", t.toString());
            }
            full = FALLBACK_ABANDONED;
            partial = FALLBACK_PARTIAL;
        }
        if (d2 < full * full) return 2;
        return d2 < partial * partial ? 1 : 0;
    }

    private static BlockPos startCentre(StructureStart s) {
        return s.getPieces().get(0).getBoundingBox().getCenter();   // the jigsaw start piece = 4.B's start piece
    }

    @Nullable
    static StructureStart villageAt(ServerLevel level, BlockPos pos, @Nullable StructureStart live) {
        if (live != null && isVillage(level, live) && level.structureManager().structureHasPieceAt(pos, live)) return live;
        StructureStart s = level.structureManager().getStructureWithPieceAt(pos, StructureTags.VILLAGE);
        return s.isValid() ? s : null;
    }

    private static boolean isVillage(ServerLevel level, StructureStart s) {
        Registry<Structure> reg = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        return reg.getResourceKey(s.getStructure()).flatMap(reg::getHolder)
                .map(h -> h.is(StructureTags.VILLAGE)).orElse(false);
    }

    @Nullable
    private static StructurePiece buildingAt(StructureStart start, BlockPos pos) {
        StructurePiece best = null;
        long bestVolume = -1;
        for (StructurePiece p : start.getPieces()) {
            BoundingBox b = p.getBoundingBox();
            if (!b.isInside(pos)) continue;
            long volume = (long) b.getXSpan() * b.getYSpan() * b.getZSpan();
            if (volume > bestVolume) { best = p; bestVolume = volume; }
        }
        return best;   // the house (the villagers feature piece inside it is smaller)
    }

    /** ComponentVillage.java:129: rand.nextInt(20) != 0 -> nobody. Deterministic per building, so a reload re-rolls the same. */
    private static boolean buildingRoll(ServerLevel level, StructurePiece building) {
        BoundingBox b = building.getBoundingBox();
        long seed = level.getSeed() ^ (b.minX() * 341873128712L + b.minZ() * 132897987541L + b.minY());
        return new Random(seed).nextInt(20) == 0;
    }

    private static int villagerClass(ServerLevel level, Mob vanilla, @Nullable StructurePiece building, BlockPos pos) {
        VillagerProfession own = vanilla instanceof Villager v ? v.getVillagerData().getProfession()
                : vanilla instanceof ZombieVillager z ? z.getVillagerData().getProfession() : VillagerProfession.NONE;
        int c = VanillaMobReplacer.fcProfession(own);
        if (c >= 0) return c;
        VillagerProfession site = building == null ? null : jobSiteProfession(level, building.getBoundingBox(), pos);
        c = site == null ? -1 : VanillaMobReplacer.fcProfession(site);
        return c >= 0 ? c : 0;   // ComponentVillage.getVillagerType default (:209-212): farmer
    }

    @Nullable
    private static VillagerProfession jobSiteProfession(ServerLevel level, BoundingBox building, BlockPos pos) {
        int x0 = Math.max(building.minX(), pos.getX() - 16), x1 = Math.min(building.maxX(), pos.getX() + 16);
        int y0 = Math.max(building.minY(), pos.getY() - 8),  y1 = Math.min(building.maxY(), pos.getY() + 8);
        int z0 = Math.max(building.minZ(), pos.getZ() - 16), z1 = Math.min(building.maxZ(), pos.getZ() + 16);
        VillagerProfession best = null;
        double bestD = Double.MAX_VALUE;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int cx = x0 >> 4; cx <= x1 >> 4; cx++) {
            for (int cz = z0 >> 4; cz <= z1 >> 4; cz++) {
                ChunkAccess chunk = level.getChunkSource().getChunk(cx, cz, ChunkStatus.FEATURES, false);
                if (chunk == null) continue;
                for (int x = Math.max(x0, cx << 4); x <= Math.min(x1, (cx << 4) + 15); x++)
                    for (int z = Math.max(z0, cz << 4); z <= Math.min(z1, (cz << 4) + 15); z++)
                        for (int y = y0; y <= y1; y++) {
                            BlockState st = chunk.getBlockState(m.set(x, y, z));
                            if (st.isAir()) continue;
                            Optional<Holder<PoiType>> poi = PoiTypes.forState(st);
                            if (poi.isEmpty()) continue;
                            VillagerProfession p = professionFor(poi.get());
                            if (p == null) continue;
                            double d = m.distSqr(pos);
                            if (d < bestD) { bestD = d; best = p; }
                        }
            }
        }
        return best;
    }

    @Nullable
    private static VillagerProfession professionFor(Holder<PoiType> poi) {
        for (VillagerProfession p : ForgeRegistries.VILLAGER_PROFESSIONS.getValues()) {
            if (p.heldJobSite().test(poi)) return p;   // NONE and NITWIT accept nothing (PoiType.NONE)
        }
        return null;
    }

    // ---- iron golems (decision D1 (a1)) ----

    static void queueGolem(ServerLevel level, Mob golem, @Nullable StructureStart live) {
        GOLEMS.add(new GolemCheck(level, golem, live));
    }

    /** Vanilla/server Village.java:80-97: 4.B removed village golems in every game mode. */
    static void drainGolems() {
        for (GolemCheck g; (g = GOLEMS.poll()) != null; ) {
            try {
                if (g.golem().isRemoved()) continue;
                if (villageAt(g.level(), g.golem().blockPosition(), g.live()) == null) continue;   // outposts keep theirs
                g.golem().discard();
                if (LOGGED.add("golem")) {
                    LOGGER.info("[VILLAGES] a new village's iron golem at {} was not spawned (4.B; logged once)",
                            g.golem().blockPosition());
                }
            } catch (Throwable t) {
                if (LOGGED.add("golemfail|" + t.getClass().getName())) LOGGER.warn("[VILLAGES] golem check failed: {}", t.toString());
            }
        }
    }

    // ---- lifecycle ----

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        boolean creative = WorldInfoBridge.isCreativeWorld(server);
        double full = -1, partial = -1, mult = -1;
        try {
            full = FcRadii.get(FcRadii.ABANDONED);
            partial = FcRadii.get(FcRadii.PARTIAL);
            mult = FcRadii.get(FcRadii.MULTIPLIER);
        } catch (Throwable t) {
            LOGGER.warn("[VILLAGES] FC radii unavailable at server start ({})", t.toString());
        }
        LOGGER.info("[VILLAGES] new villages: {} (default game type {}); abandoned within {} blocks, partially within {} "
                + "of the spawn {} (terrain multiplier {})", creative ? "villagers" : "villager zombies",
                server.getDefaultGameType(), full, partial, server.overworld().getSharedSpawnPos(), mult);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        VILLAGES.clear();
        GOLEMS.clear();
        LIVE_PLACEMENT.remove();
        LOGGED.clear();
    }

    private VillageInhabitants() {}
}
