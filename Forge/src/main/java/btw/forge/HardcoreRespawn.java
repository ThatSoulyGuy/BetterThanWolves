package btw.forge;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FCMOD-INTEGRATION(1.20.1): hardcore_respawn, creative_world (INTEGRATIONS #054, STUB_CATALOGUE (br))
 *
 * <p><b>Feature:</b> 1.20.1 respawn. PlayerList.respawn (PlayerList.java:425-494) resolves the player's respawn point
 * through Player.findRespawnPositionAndUseSpawnBlock (a charged respawn anchor, a bed, Forge's getRespawnPosition, a
 * forced /spawnpoint, Player.java:1326-1345), else the ServerPlayer constructor places the player at the world spawn
 * within the spawnRadius gamerule (ServerPlayer.java:259-298). The world border, the integrated server's LAN state, the
 * doDaylightCycle gamerule and sleeping (which move or stop the day clock) are 1.20.1 additions.</p>
 *
 * <p><b>Conflict with FC:</b> 4.B's respawn (Server FCUtilsHardcoreSpawn.HandleHardcoreSpawn, :107-188, called from
 * vanilla/server ServerConfigurationManager.recreatePlayerEntity :1082-1219) never ran: every death respawned at the world
 * spawn. FC's own code cannot run through the bridges: WorldBridge.getTopSolidOrLiquidBlock is MOTION_BLOCKING (water
 * and leaves count) and never loads a chunk, WorldProvider.getAverageGroundLevel is 64 everywhere, the client flavour's
 * FCBetterThanWolves.IsSinglePlayerNonLan dereferences a btw.modern.Minecraft that nothing sets, and PlayerBridge's
 * position fields are a shadow of the real player. 4.B's beacon validation (vanilla/server
 * EntityPlayer.GetValidatedRespawnCoordinates, :2806-2969) was never ported either.</p>
 *
 * <p><b>Integration pattern:</b> D (FC-extend): FC's numbers, progress flags and spawn list; 1.20.1's world. The radii,
 * the progress and world-type multipliers, the soft radius, the 24000-tick window, the Nether gate and the beacon's spawn
 * effect are read from FC's own statics ({@link Fc}); only the spatial search (the surface column, chunk generation,
 * placement) is ported, against 1.20.1. Rebalance: none (4.B parity), plus the maintainer rulings below.</p>
 *
 * <p><b>The general rule.</b> At every PlayerList.respawn a respawn point 1.20.1 owns (an anchor, /spawnpoint, a modded
 * respawn block) is resolved by 1.20.1. The point FC itself set, the beacon bind (Server FCBlockBeacon.java:93, kept as
 * {@link PlayerBridge#fcBindPos}), is resolved by 4.B's GetValidatedRespawnCoordinates. A non-forced point on a bed is
 * never a respawn point (beds ruling). When no point is used, 4.B's HandleHardcoreSpawn places the player in the
 * Overworld: a random point 1000-2000 blocks from the original spawn (x1.5 after the Nether, x2 after the Wither, x4
 * after the End, x4 again on large-biomes worlds), a soft respawn for a death within a day of the assignment (near the
 * old point, health 10, food -6 with a floor of 12), the early-game shared respawn, and the spawn list. Leaving the End
 * returns the player to the original spawn and keeps the respawn point (SCM:1144-1149). Every respawn ends with 4.B's
 * fresh-player state (EntityPlayer.clonePlayer copies none of it, :2092-2115): no hardcore home, time 0, bind cooldown
 * 0 (a dead player may rebind at once, FCBlockBeacon.java:86-87), dimension-switch time 0.</p>
 *
 * <p><b>Maintainer rulings:</b> 2026-10-21, port 4.B's hardcore respawn; a charged respawn anchor in the Nether still
 * works; a bound soulforged-steel beacon overrides the random respawn (BEACON); beds never set the spawn. 2026-10-22
 * (RESPAWN 7(a), option 3): every game mode, except in worlds created as creative ({@link #appliesTo}).</p>
 *
 * <p><b>Injection strategy:</b> PlayerListRespawnMixin (HEAD, the respawn point's STORE, the resolved Optional's STORE,
 * the INVOKE of ForgeEventFactory.firePlayerRespawnEvent, RETURN of PlayerList.respawn) and ServerPlayerMixin's
 * restoreFrom TAIL ({@link #onRestore}), the first moment the new ServerPlayer exists: the placement is made before
 * 1.20.1's own collision bump (PlayerList.java:470-472) and before the respawn packet and teleport (:476-477), so there is
 * no second teleport. The decision context is a per-thread chain keyed on the dying player, so a respawn started by a
 * listener inside another respawn gets its own. Never throws: every entry catches Throwable, logs once per site and
 * leaves 1.20.1's own behaviour, which is 4.B's original-spawn fallback.</p>
 *
 * <p><b>Known limitation:</b> the point search loads and generates terrain on the server thread, as 4.B did, bounded by
 * a wall-clock budget per respawn ({@link #generationBudgetNanos}, 3000 ms, -Dbtw.hardcoreRespawn.budgetMs); once it is
 * spent, candidates in unloaded chunks fail and the respawn falls back to the original spawn. A noise pre-filter skips
 * candidates below the ground level without generating them, so spots 1.20.1 adds after the noise stage (icebergs,
 * mangrove roots, eroded-badlands pillars, trees on low ground) are not used. FC's day is 1.20.1's day clock: a slept
 * night counts, and with doDaylightCycle false the one-day windows never end. Mystery meat on a player's death is not
 * ported.</p>
 */
// FCMOD-INTEGRATION(1.20.1): hardcore_respawn: 4.B's random, soft and shared respawn and its beacon validation run at PlayerList.respawn
public final class HardcoreRespawn {

    /** Implemented on PlayerList by PlayerListRespawnMixin, so the server-start self-audit can prove the mixin applied. */
    public interface Hooked {}

    /** surfaceY's answer for a column FC's test rejects. */
    public static final int NO_SURFACE = Integer.MIN_VALUE;

    private static final Logger LOGGER = LogManager.getLogger("BTW-HardcoreRespawn");

    /**
     * Wall-clock budget for chunk loads and generation during ONE respawn's point search. Non-final: the proof harness
     * lowers it by reflection; servers may set -Dbtw.hardcoreRespawn.budgetMs.
     */
    static volatile long generationBudgetNanos =
            Math.max(0L, Long.getLong("btw.hardcoreRespawn.budgetMs", 3000L)) * 1_000_000L;

    /** Nanoseconds the last hardcore respawn spent loading or generating chunks (-1 before the first); read by the proof verb. */
    static volatile long lastGenerationNanos = -1L;

    private static final ThreadLocal<Ctx> CTX = new ThreadLocal<>();

    /** Sites whose failure was logged (once per site per JVM). */
    private static final Set<String> FAILURES_LOGGED = ConcurrentHashMap.newKeySet();

    private static volatile boolean budgetLogged;

    private HardcoreRespawn() {}

    /** One respawn's decision (SCM:1097-1149), from the HEAD of PlayerList.respawn to its RETURN. */
    static final class Ctx {
        final ServerPlayer old;
        final boolean endExit;
        final Ctx outer;
        final int tick;
        BlockPos pos;                // the old point
        ResourceKey<Level> dim;
        float angle;
        boolean forced;
        boolean hardcore;            // 4.B's hardcore respawn applies in this world (appliesTo; RESPAWN 7(a), ruling 2026-10-22)
        boolean suppress;            // PlayerList.respawn sees no point (End exit, failed FC bind, bed point)
        boolean retain;              // re-set the old point on the new player (4.B bRetainPreviousSpawn, SCM:1187-1190)
        boolean dropBind;            // forget FC's bind (code 2)
        Vec3 beaconSpawn;            // code 0
        boolean pointUsed;           // PlayerList's Optional was present
        String message;              // SCM:1119-1139
        ServerPlayer np;             // the new player, from onRestore
        long genNanos;               // chunk load/generation time spent by this respawn

        Ctx(ServerPlayer old, boolean endExit, Ctx outer, int tick) {
            this.old = old;
            this.endExit = endExit;
            this.outer = outer;
            this.tick = tick;
        }

        /** Plain 1.20.1 point handling (after a failure); the hardcore flag stays as it was read. */
        void plain() {
            suppress = retain = dropBind = false;
            beaconSpawn = null;
            message = null;
            pos = null;
        }
    }

    private static Ctx find(ServerPlayer old) {
        for (Ctx c = CTX.get(); c != null; c = c.outer) {
            if (c.old == old) return c;
        }
        return null;
    }

    // =====================================================================================================
    // PlayerList.respawn hooks (PlayerListRespawnMixin) and the restoreFrom hook (ServerPlayerMixin)
    // =====================================================================================================

    /** HEAD of PlayerList.respawn: 4.B's decision on the old point (SCM:1104-1149), plus the beds ruling. */
    public static void begin(ServerPlayer old, boolean endExit) {
        Ctx c;
        try {
            int tick = old.server.getTickCount();
            Ctx outer = CTX.get();
            while (outer != null && outer.tick != tick) outer = outer.outer;  // left by a respawn that threw in an earlier tick
            c = new Ctx(old, endExit, outer, tick);
            CTX.set(c);
        } catch (Throwable t) {
            logOnce("begin", t);
            return;
        }
        try {
            c.hardcore = appliesTo(old.server);                                 // read once per respawn
            BlockPos pos = old.getRespawnPosition();
            if (pos == null) return;                                            // HasRespawnCoordinates false
            c.pos = pos;
            c.dim = old.getRespawnDimension();
            c.angle = old.getRespawnAngle();
            c.forced = old.isRespawnForced();
            ServerLevel at = old.server.getLevel(c.dim);
            if (!c.forced && at != null) {
                // Beds ruling, every world: a bed is never a respawn point. BedSpawnPolicy stops new bed points; this drops
                // one saved before it, with no 1.20.1 respawn or message. PlayerList.java:434 would read this cell anyway.
                BlockState s = at.getBlockState(pos);
                if (BedSpawnPolicy.isBed(s, at, pos, old)) {
                    c.suppress = true;
                    c.pos = null;
                    return;
                }
            }
            if (endExit && c.hardcore) {                                        // SCM:1144-1149; a creative world keeps 1.20.1's End exit
                c.suppress = true;
                c.retain = true;
                return;
            }
            PlayerBridge ob = PlayerBridge.getOrCreate(old);
            if (c.forced || !pos.equals(ob.fcBindPos) || !c.dim.equals(ob.fcBindDim)) return;   // 1.20.1's point
            if (!Fc.resolve()) return;                                          // FC link broken: leave the point to 1.20.1
            int code = at == null ? 2 : validateBeacon(old, at, pos, c);        // EntityPlayer.java:2806-2917, every world
            if (code == 0) return;                                              // c.beaconSpawn set
            c.suppress = true;
            c.retain = code == 3 || code == 4;                                  // SCM:1128-1136
            c.dropBind = !c.retain;
            c.message = code == 2 ? "The beacon to which you were bound is no longer present"
                    : code == 3 ? "The beacon to which you are bound was too far away"
                    : "The beacon to which you are bound was obstructed";
        } catch (Throwable t) {
            logOnce("begin", t);
            c.plain();
        }
    }

    /** The STORE of PlayerList.respawn's respawn point: null when 4.B ignores the point (End exit, failed bind, bed). */
    public static BlockPos respawnPosition(BlockPos vanilla, ServerPlayer old) {
        Ctx c = find(old);
        return c != null && c.suppress ? null : vanilla;
    }

    /** The STOREs of PlayerList.respawn's resolved Optional: a validated beacon position (code 0) replaces it. */
    public static Optional<Vec3> respawnResult(Optional<Vec3> vanilla, ServerPlayer old) {
        Ctx c = find(old);
        if (c == null) return vanilla;
        // beaconSpawn is set only when server.getLevel(dim) != null, so a present result never meets a null serverlevel
        Optional<Vec3> r = c.beaconSpawn != null ? Optional.of(c.beaconSpawn) : vanilla;
        c.pointUsed = r.isPresent();
        return r;
    }

    /**
     * ServerPlayer.restoreFrom TAIL, inside PlayerList.respawn (PlayerList.java:440-442): 4.B's fresh EntityPlayerMP, then
     * HandleHardcoreSpawn when no point was used (SCM:1163-1185).
     */
    public static void onRestore(ServerPlayer np, ServerPlayer old, boolean keepEverything) {
        Ctx c = find(old);
        if (c == null) return;                                                  // restoreFrom outside PlayerList.respawn
        c.np = np;
        try {
            PlayerBridge nb = PlayerBridge.getOrCreate(np);
            PlayerBridge ob = PlayerBridge.getOrCreate(old);
            // A fresh 4.B EntityPlayerMP: clonePlayer copies none of these (EntityPlayer.java:2092-2115, defaults :2224-2225)
            nb.m_HardcoreSpawnChunk = null;
            nb.m_lTimeOfLastSpawnAssignment = 0L;
            nb.m_lRespawnAssignmentCooldownTimer = 0L;                          // FCBlockBeacon.java:86-87: a dead player may rebind at once
            nb.m_lTimeOfLastDimensionSwitch = 0L;
            if (c.dropBind) {
                nb.fcBindPos = null;
                nb.fcBindDim = null;
            }
            if (keepEverything || c.pointUsed) return;                          // End exit / a point was used (SCM:1175-1181)
            ServerLevel ow = np.server.overworld();
            if (np.serverLevel() != ow) return;                                 // 4.B respawns into dimension 0
            if (!c.hardcore) return;                                            // creative world: 1.20.1's placement stays (ruling 2026-10-22)
            handleHardcoreSpawn(c, ow, ob, np, nb);
        } catch (Throwable t) {
            logOnce("restore", t);
        } finally {
            lastGenerationNanos = c.genNanos;
        }
    }

    /** The INVOKE of ForgeEventFactory.firePlayerRespawnEvent: 4.B's retain and line (SCM:1187-1195), before listeners run. */
    public static void beforeRespawnEvent(ServerPlayer old) {
        Ctx c = find(old);
        if (c == null || c.np == null) return;
        try {
            if (c.retain && c.pos != null) c.np.setRespawnPosition(c.dim, c.pos, c.angle, c.forced, false);
            if (c.message != null) PlayerBridge.getOrCreate(c.np).AddRawChatMessage(c.message);   // FC's literal, like FCBlockBeacon's bind line
        } catch (Throwable t) {
            logOnce("retain", t);
        }
    }

    /** RETURN of PlayerList.respawn: context cleanup only. */
    public static void end(ServerPlayer old) {
        try {
            Ctx c = find(old);
            if (c == null) return;
            if (c.outer == null) CTX.remove();
            else CTX.set(c.outer);                                              // pops c and anything a swallowed nested throw left above it
        } catch (Throwable t) {
            logOnce("end", t);
        }
    }

    // =====================================================================================================
    // 4.B: GetValidatedRespawnCoordinates (vanilla/server EntityPlayer.java:2806-2969), non-forced branch
    // =====================================================================================================

    /** 0 valid (c.beaconSpawn set), 2 no beacon, 3 out of range or another dimension, 4 obstructed. */
    static int validateBeacon(ServerPlayer old, ServerLevel at, BlockPos b, Ctx c) throws Throwable {
        WorldBridge w = WorldBridge.getOrCreate(at);                            // reads load the chunk on demand (EP:2817-2820)
        if (w.getBlockId(b.getX(), b.getY(), b.getZ()) != FcBeacons.FC_BEACON_ID) return 2;   // Block.beacon.blockID, as BEACON's prune
        Object te = w.getBlockTileEntity(b.getX(), b.getY(), b.getZ());
        if (!(te instanceof btw.modern.TileEntityBeacon beacon) || !Fc.beaconClass.isInstance(te)) return 2;
        if (beacon.getPrimaryEffect() != Fc.spawnEffect) return 2;             // FCTileEntityBeacon.m_iEffectIDSpawnPoint
        int level = beacon.getLevels();
        if (level <= 0) return 2;
        if (level < 4 && !old.level().dimension().equals(at.dimension())) return 3;
        if (level < 3) {
            int max = level == 2 ? 2000 : 160;
            if (Math.abs((int) old.getX() - b.getX()) > max || Math.abs((int) old.getZ() - b.getZ()) > max) return 3;
        }
        BlockPos p = aroundBeacon(at, b, level, old.getRandom());
        if (p == null) return 4;
        c.beaconSpawn = new Vec3(p.getX() + 0.5D, p.getY() + 0.1D, p.getZ() + 0.5D);   // SCM:1177
        Fc.respawnedAtBeacon.setBoolean(te, true);                              // EP:2896: the beacon's respawn sounds
        return 0;
    }

    /**
     * GetRandomValidSpawnAroundBeaconLocation (EP:2931-2969): 20 tries on the pyramid's sides. The support test is 4.B's
     * World.doesBlockHaveSolidTopSurface (EP:2962), asked of the level's WorldBridge (solid_top_surface).
     */
    static BlockPos aroundBeacon(ServerLevel w, BlockPos b, int level, RandomSource rand) {
        WorldBridge fw = WorldBridge.getOrCreate(w);
        for (int a = 0; a < 20; a++) {
            int dist = rand.nextInt(level) + 1;
            int primary = rand.nextInt(2) * dist;                               // 4.B's own expression, kept
            if (rand.nextInt(2) == 0) primary = -primary;
            int secondary = rand.nextInt(level * 2 + 1) - level;
            int dx = primary, dy = -(dist - 1), dz = secondary;
            if (rand.nextInt(2) == 0) {
                dx = secondary;
                dz = primary;
            }
            BlockPos p = b.offset(dx, dy, dz), below = p.below();
            if (fw.doesBlockHaveSolidTopSurface(below.getX(), below.getY(), below.getZ())
                    && respawnable(w, p) && respawnable(w, p.above())) {        // IsValidRespawnLocation
                return p;
            }
        }
        return null;
    }

    /** IsValidRespawnLocation (EP:2920-2929): neither solid nor liquid; 1.20.1's Block.isPossibleToRespawnInThis. */
    static boolean respawnable(ServerLevel w, BlockPos p) {
        BlockState s = w.getBlockState(p);
        return s.getBlock().isPossibleToRespawnInThis(s);
    }

    // =====================================================================================================
    // 4.B: HandleHardcoreSpawn and its helpers (Server FCUtilsHardcoreSpawn.java:107-406), against 1.20.1
    // =====================================================================================================

    /** HS:107-188. The original-spawn fallback (HS:363-385) leaves the player where the ServerPlayer constructor put them. */
    static void handleHardcoreSpawn(Ctx c, ServerLevel ow, PlayerBridge ob, ServerPlayer np, PlayerBridge nb) throws Throwable {
        if (!Fc.resolve()) return;                                              // FC link broken: 1.20.1's spawn = original spawn
        long now = ow.getDayTime();                                             // FCUtilsWorld.GetOverworldTimeServerOnly
        long last = ob.m_lTimeOfLastSpawnAssignment;
        long delta = now - last;
        boolean soft = last > 0 && delta >= 0 && delta < Fc.reassign;          // HS:124-125
        if (soft) {
            np.setHealth(10F);                                                  // HS:131
            nb.foodStats.setFoodLevel(Math.max(ob.foodStats.getFoodLevel() - 6, 12));   // HS:133-142 (FC's 60-unit scale)
        }
        btw.modern.FCSpawnLocationList list = WorldBridge.getOrCreate(ow).GetSpawnLocationList();
        if (!Fc.netherAccessed()) {                                             // HS:145-165: early game, players respawn together
            btw.modern.FCSpawnLocation r = list.GetMostRecentSpawnLocation();
            if (r != null) {
                long d = now - r.m_lSpawnTime;
                if (d > 0 && d < Fc.reassign && nearOld(c, ow, np, nb,
                        new btw.modern.ChunkCoordinates(r.m_iIPos, r.m_iJPos, r.m_iKPos), r.m_lSpawnTime)) {
                    return;                                                     // the point is not re-added
                }
            }
        }
        btw.modern.ChunkCoordinates home = ob.m_HardcoreSpawnChunk;            // HS:167-180
        if (home == null || !soft || !nearOld(c, ow, np, nb, home, last)) {
            if (!assignNew(c, ow, np, nb)) {                                    // HS:176: ReturnPlayerToOriginalSpawn
                nb.m_HardcoreSpawnChunk = null;
                nb.m_lTimeOfLastSpawnAssignment = 0L;
                return;
            }
        }
        btw.modern.ChunkCoordinates h = nb.m_HardcoreSpawnChunk;               // HS:182-187
        if (h != null) list.AddPointIfNotAlreadyPresent(h.posX, h.posY, h.posZ, nb.m_lTimeOfLastSpawnAssignment);
    }

    /** AssignNewHardcoreSpawnLocation (HS:190-268): 20 tries in the annulus around the original spawn. */
    static boolean assignNew(Ctx c, ServerLevel ow, ServerPlayer np, PlayerBridge nb) throws Throwable {
        double r2 = Fc.radius(), r1 = Fc.exclusion();                           // FC: base x world type x progress
        double r1sq = r1 * r1, dsq = r2 * r2 - r1sq;
        RandomSource rand = ow.getRandom();
        BlockPos spawn = ow.getSharedSpawnPos();                                // worldInfo.getSpawnX/Z
        int ground = groundLevel(ow);
        for (int a = 0; a < 20; a++) {
            double dist = Math.sqrt(rand.nextDouble() * dsq + r1sq);
            double yaw = rand.nextDouble() * Math.PI * 2D;
            int i = Mth.floor(-Math.sin(yaw) * dist) + spawn.getX();
            int k = Mth.floor(Math.cos(yaw) * dist) + spawn.getZ();
            int j = candidate(c, ow, i, k, ground);
            if (j == NO_SURFACE) continue;
            np.moveTo(i + 0.5D, j + 1.5D, k + 0.5D, rand.nextFloat() * 360F, 0F);   // HS:224, 4.B's 1.5-block drop
            bumpUp(ow, np);                                                     // HS:387-406
            long t = ow.getDayTime();
            if (np.server.isSingleplayer() && !np.server.isPublished()) t = advanceToNextMorning(np.server);   // HS:230-251
            nb.m_lTimeOfLastSpawnAssignment = t;
            nb.m_HardcoreSpawnChunk = new btw.modern.ChunkCoordinates(Mth.floor(np.getX()), Mth.floor(np.getY()), Mth.floor(np.getZ()));
            return true;
        }
        return false;
    }

    /** AssignPlayerToOldSpawnPosWithVariance / AssignPlayerToOldSpawnPos / OffsetPlayerPositionUntilValidSpawn (HS:270-361). */
    static boolean nearOld(Ctx c, ServerLevel ow, ServerPlayer np, PlayerBridge nb, btw.modern.ChunkCoordinates centre, long time)
            throws Throwable {
        RandomSource rand = ow.getRandom();
        int ground = groundLevel(ow);
        for (int a = 0; a < 20; a++) {                                          // AssignPlayerToOldSpawnPosWithVariance
            double dist = Math.sqrt(rand.nextDouble()) * Fc.multiple();
            double yaw = rand.nextDouble() * Math.PI * 2D;
            int i = Mth.floor(-Math.sin(yaw) * dist) + centre.posX;
            int k = Mth.floor(Math.cos(yaw) * dist) + centre.posZ;
            int j = candidate(c, ow, i, k, ground);
            if (j == NO_SURFACE) continue;
            np.moveTo(i + 0.5D, j + 1.5D, k + 0.5D, rand.nextFloat() * 360F, 0F);
            bumpUp(ow, np);
            nb.m_lTimeOfLastSpawnAssignment = time;
            nb.m_HardcoreSpawnChunk = new btw.modern.ChunkCoordinates(centre.posX, centre.posY, centre.posZ);
            return true;
        }
        int ci = centre.posX, ck = centre.posZ;                                 // AssignPlayerToOldSpawnPos (HS:311-336)
        int cj = topSolidBudgeted(c, ow, ci, ck);
        if (cj == NO_SURFACE) return false;
        np.moveTo(ci + 0.5D, cj + 1.5D, ck + 0.5D, rand.nextFloat() * 360F, 0F);
        int i = ci, k = ck;                                                     // OffsetPlayerPositionUntilValidSpawn (HS:338-361)
        for (int a = 0; a < 20; a++) {
            if (ow.getWorldBorder().isWithinBounds(new BlockPos(i, 0, k))) {
                int j = topSolidBudgeted(c, ow, i, k);
                if (j != NO_SURFACE && !liquidLike(ow.getBlockState(new BlockPos(i, j, k)))) {
                    np.moveTo(i + 0.5D, np.getY(), k + 0.5D, np.getYRot(), np.getXRot());   // keeps the centre's height (4.B)
                    bumpUp(ow, np);
                    nb.m_lTimeOfLastSpawnAssignment = time;
                    nb.m_HardcoreSpawnChunk = new btw.modern.ChunkCoordinates(Mth.floor(np.getX()), Mth.floor(np.getY()), Mth.floor(np.getZ()));
                    return true;
                }
            }
            i += rand.nextInt(11) - 5;
            k += rand.nextInt(11) - 5;
        }
        return false;
    }

    // =====================================================================================================
    // The column test, the budget, and the 1.20.1 integration
    // =====================================================================================================

    /** FC's column test (HS:216-222): the first free y above the top solid non-leaves block, or NO_SURFACE. Generates the chunk. */
    public static int surfaceY(ServerLevel w, int x, int z) {
        return surfaceAt(w, x, z, topSolid(w, x, z));
    }

    static int surfaceAt(ServerLevel w, int x, int z, int j) {
        if (j == NO_SURFACE || j < groundLevel(w)) return NO_SURFACE;          // HS:218 getAverageGroundLevel
        return liquidLike(w.getBlockState(new BlockPos(x, j, z))) ? NO_SURFACE : j;   // HS:220-222 not liquid
    }

    /** A liquid, a waterlogged block, or 1.20.1's sinking powder snow: never a place to stand. */
    static boolean liquidLike(BlockState s) {
        return !s.getFluidState().isEmpty() || s.is(Blocks.POWDER_SNOW);
    }

    /**
     * 1.5.2 World.getTopSolidOrLiquidBlock (vanilla/server World.java:1602-1619): one above the highest block that blocks
     * movement and is not leaves (liquids do not block movement). Loads, and if needed generates, the chunk, as 1.5.2's
     * getChunkFromBlockCoords did. OCEAN_FLOOR is the highest blocksMotion() block, leaves included; the walk skips leaves.
     */
    static int topSolid(ServerLevel w, int x, int z) {
        LevelChunk ch = w.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));   // FULL, server thread
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, 0, z);
        for (int y = ch.getHeight(Heightmap.Types.OCEAN_FLOOR, x & 15, z & 15); y >= w.getMinBuildHeight(); --y) {
            BlockState s = ch.getBlockState(p.setY(y));
            if (s.blocksMotion() && !s.is(BlockTags.LEAVES)) return y + 1;
        }
        return w.getMinBuildHeight() - 1;
    }

    /** topSolid where a not-yet-loaded chunk costs budget; once this respawn's budget is spent, such a column fails. */
    static int topSolidBudgeted(Ctx c, ServerLevel w, int x, int z) {
        if (w.hasChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z))) return topSolid(w, x, z);
        if (c.genNanos >= generationBudgetNanos) {
            logBudgetOnce();
            return NO_SURFACE;
        }
        long t0 = Util.getNanos();
        try {
            return topSolid(w, x, z);
        } finally {
            c.genNanos += Util.getNanos() - t0;
        }
    }

    /**
     * 1.5.2's average ground level 64 = sea level 63 + 1 (flat 4); 1.20.1: the generator's sea level + 1 (63 + 1 on normal
     * and large biomes; flat -63 + 1 = -62, ground at -60). Level.getSeaLevel() is a constant 63, so the generator is asked.
     */
    static int groundLevel(ServerLevel w) {
        return w.getChunkSource().getGenerator().getSeaLevel() + 1;
    }

    /** surfaceY behind three 1.20.1 gates (the world border, the budget, the noise pre-filter); each failure is one of FC's 20 tries. */
    static int candidate(Ctx c, ServerLevel w, int x, int z, int ground) {
        if (!w.getWorldBorder().isWithinBounds(new BlockPos(x, 0, z))) return NO_SURFACE;   // no respawn outside the border
        if (!w.hasChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z))) {
            if (c.genNanos >= generationBudgetNanos) {                          // budget spent
                logBudgetOnce();
                return NO_SURFACE;
            }
            try {                                                               // noise pre-filter, no chunk generated
                int h = w.getChunkSource().getGenerator().getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG,
                        w, w.getChunkSource().randomState());
                if (h < ground) return NO_SURFACE;                              // ocean, river, deep valley
            } catch (Throwable t) {
                logOnce("prefilter", t);
            }
        }
        return surfaceAt(w, x, z, topSolidBudgeted(c, w, x, z));
    }

    /** HS:387-406 (BumpPlayerPosUpwardsUntilValidSpawnReached), as 1.20.1's own bump at PlayerList.java:470-472. */
    static void bumpUp(ServerLevel w, ServerPlayer p) {
        while (!w.noCollision(p) && p.getY() < w.getMaxBuildHeight()) p.setPos(p.getX(), p.getY() + 1.0D, p.getZ());
    }

    private static void logBudgetOnce() {
        if (budgetLogged) return;
        budgetLogged = true;
        LOGGER.warn("[hardcore_respawn] terrain generation budget ({} ms per respawn) spent; candidates in unloaded chunks "
                + "are skipped and the respawn may fall back to the original spawn (logged once; "
                + "-Dbtw.hardcoreRespawn.budgetMs raises it)", generationBudgetNanos / 1_000_000L);
    }

    /**
     * The singleplayer morning (HS:230-251), run by a new assignment on an integrated server not opened to LAN: every
     * level's day time goes to the next morning, and a thunderstorm ends. Returns the new day time. Public for the proof
     * verb.
     */
    public static long advanceToNextMorning(MinecraftServer server) {
        long t = (server.overworld().getDayTime() / 24000L + 1L) * 24000L;
        boolean cleared = false;
        for (ServerLevel l : server.getAllLevels()) {
            l.setDayTime(t);                                                    // derived levels ignore it, as /time set
            if (l.getLevelData().isThundering() && l.getLevelData() instanceof ServerLevelData d) {
                d.setThundering(false);
                l.setThunderLevel(0F);
                cleared = true;
            }
        }
        if (cleared) {                                                          // 1.5.2 Packet70GameEvent(8, 0); 1.20.1 type 8 is the same event
            server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, 0F));
        }
        return t;
    }

    /** 1.5.2 ValidateSpawnLocationList's window for WorldBridge's prune: FC's m_iHardcoreSpawnTimeBetweenReassignments once
     *  resolved, else 24000 (never resolves on the tick path). */
    public static int reassignTicks() {
        return Fc.reassign;
    }

    /**
     * RESPAWN 7(a), maintainer ruling 2026-10-22 (option 3): 4.B's hardcore respawn applies to every game mode (4.B has no
     * game-mode test, FCUtilsHardcoreSpawn.java:107-188, ServerConfigurationManager.java:1082-1219) except in a creative
     * world (WorldInfoBridge.isCreativeWorld, the rule VILLAGES uses), where the respawn is 1.20.1's: the player's own
     * respawn point, else the world spawn. Read once per respawn, at the HEAD of PlayerList.respawn (Ctx.hardcore).
     */
    static boolean appliesTo(MinecraftServer server) {
        return !WorldInfoBridge.isCreativeWorld(server);
    }

    // =====================================================================================================
    // FC's beacon bind: the legacy dimension, and the bind marker's NBT
    // =====================================================================================================

    /**
     * The level FC means by a legacy dimension id (FCBlockBeacon passes world.provider.dimensionId of the beacon's world,
     * and the binding player stands in it): the player's current level when it has that id (modded dimensions report 0,
     * FcWorldSavedData.overworldInfo), else -1 the Nether, 1 the End, any other the Overworld.
     */
    public static ResourceKey<Level> levelForLegacyDim(ServerPlayer sp, int id) {
        try {
            if (sp.level() instanceof ServerLevel l && WorldBridge.getOrCreate(l).provider.dimensionId == id) return l.dimension();
        } catch (Throwable t) {
            logOnce("legacy dimension", t);
        }
        return id == -1 ? Level.NETHER : id == 1 ? Level.END : Level.OVERWORLD;
    }

    static final String BIND_KEY = "btwFcSpawnBind";

    /** Reads PlayerBridge's FC bind marker (key btwFcSpawnBind, {X, Y, Z, Dim}); a missing or malformed tag is no bind. Never throws. */
    public static void readBind(CompoundTag tag, PlayerBridge pb) {
        try {
            pb.fcBindPos = null;
            pb.fcBindDim = null;
            if (!tag.contains(BIND_KEY, 10)) return;
            CompoundTag b = tag.getCompound(BIND_KEY);
            ResourceLocation dim = ResourceLocation.tryParse(b.getString("Dim"));
            if (dim == null || !b.contains("X", 99) || !b.contains("Y", 99) || !b.contains("Z", 99)) return;
            pb.fcBindPos = new BlockPos(b.getInt("X"), b.getInt("Y"), b.getInt("Z"));
            pb.fcBindDim = ResourceKey.create(Registries.DIMENSION, dim);
        } catch (Throwable t) {
            pb.fcBindPos = null;
            pb.fcBindDim = null;
            logOnce("read bind", t);
        }
    }

    /** Writes PlayerBridge's FC bind marker, only when there is one. Never throws. */
    public static void writeBind(CompoundTag tag, PlayerBridge pb) {
        try {
            BlockPos p = pb.fcBindPos;
            ResourceKey<Level> d = pb.fcBindDim;
            if (p == null || d == null) return;
            CompoundTag b = new CompoundTag();
            b.putInt("X", p.getX());
            b.putInt("Y", p.getY());
            b.putInt("Z", p.getZ());
            b.putString("Dim", d.location().toString());
            tag.put(BIND_KEY, b);
        } catch (Throwable t) {
            logOnce("write bind", t);
        }
    }

    // =====================================================================================================
    // Server start: FC links and the self-audit
    // =====================================================================================================

    /** Once per world start, after BTWLifecycle.wireWorlds (FC's statics read worldServers[0]). Sets nothing. Never throws. */
    public static void onServerStarting(MinecraftServer server) {
        try {
            List<String> warnings = new ArrayList<>();
            boolean hooked = server.getPlayerList() instanceof Hooked;
            if (!hooked) warnings.add("PlayerListRespawnMixin is not applied: no hardcore respawn, beacon validation or End exit rule");
            boolean creative = WorldInfoBridge.isCreativeWorld(server);
            ServerLevel ow = server.overworld();
            btw.modern.WorldInfo info = WorldBridge.getOrCreate(ow).worldInfo;
            btw.modern.EnumGameType want = WorldInfoBridge.worldGameType(server);
            btw.modern.EnumGameType fcType = info.getGameType();
            if (fcType != want) {
                warnings.add("FC world game type " + name(fcType) + " differs from the default game type " + name(want));
            }
            btw.modern.WorldType wt = info.getTerrainType();
            boolean genLarge = ow.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator g
                    && g.stable(NoiseGeneratorSettings.LARGE_BIOMES);
            if (genLarge != (wt == btw.modern.WorldType.LARGE_BIOMES)) {
                warnings.add("FC world type " + (wt == null ? "null" : wt.getWorldTypeName()) + " disagrees with the overworld generator"
                        + " (large biomes " + genLarge + "): WorldInfoBridge.getTerrainType is missing or broken");
            }
            String fc;
            if (Fc.resolve()) {
                double world = Fc.worldMultiplier(), progress = Fc.progressMultiplier();
                double r2 = Fc.radius(), r1 = Fc.exclusion(), soft = Fc.multiple();
                if (Math.abs(r2 / (world * progress) - 2000D) > 1e-6) warnings.add("FC spawn radius " + num(r2) + " is not 2000 x multipliers");
                if (Math.abs(r1 / (world * progress) - 1000D) > 1e-6) warnings.add("FC exclusion radius " + num(r1) + " is not 1000 x multipliers");
                if (soft != 100D) warnings.add("FC soft radius " + num(soft) + " is not 100");
                if (Fc.reassign != 24000) warnings.add("FC reassignment window " + Fc.reassign + " is not 24000");
                if (Fc.spawnEffect != 22223) warnings.add("FC spawn effect id " + Fc.spawnEffect + " is not 22223");
                fc = "FC radius " + num(r1) + "-" + num(r2) + " (x" + num(world) + " x" + num(progress) + "), soft radius " + num(soft)
                        + ", reassignment " + Fc.reassign + " ticks, spawn effect " + Fc.spawnEffect;
            } else {
                warnings.add("FC links unavailable: every respawn stays 1.20.1's (" + Fc.failure + ")");
                fc = "FC links unavailable";
            }
            String line = "[SELF-AUDIT/hcspawn] PlayerList hooked=" + hooked + ", creative world " + creative
                    + " (default game type " + server.getDefaultGameType().getName() + ", FC world game type " + name(fcType)
                    + ", hardcore " + server.isHardcore() + ") -> hardcore respawn " + (appliesTo(server) ? "on" : "off")
                    + ", world type " + (wt == null ? "null" : wt.getWorldTypeName()) + " (generator large biomes " + genLarge + "), "
                    + fc + ", generation budget " + generationBudgetNanos / 1_000_000L + " ms; " + warnings.size() + " warning(s)";
            for (String w : warnings) LOGGER.warn("[SELF-AUDIT/hcspawn] WARN: {}", w);
            if (warnings.isEmpty()) LOGGER.info(line);
            else LOGGER.warn(line);
        } catch (Throwable t) {
            LOGGER.warn("[SELF-AUDIT/hcspawn] the hardcore respawn self-audit failed: {}", t.toString(), t);
        }
    }

    private static String name(btw.modern.EnumGameType t) {
        return t == null ? "null" : t.name().toLowerCase(Locale.ROOT);
    }

    private static String num(double v) {
        return v == Math.rint(v) ? Long.toString((long) v) : Double.toString(v);
    }

    private static void logOnce(String site, Throwable t) {
        if (FAILURES_LOGGED.add(site)) {
            LOGGER.warn("[hardcore_respawn] {} failed (logged once per site); this respawn keeps 1.20.1's behaviour: {}",
                    site, t.toString(), t);
        }
    }

    // =====================================================================================================
    // FC links. FC classes are not on Forge's compile classpath (the FcRefs / FCLoginHandler pattern).
    // =====================================================================================================

    /**
     * FC's statics, resolved lazily and cached; a miss is not cached, a failure is logged once. javap of the frozen fc
     * classes: FCUtilsHardcoreSpawn GetPlayerSpawnRadius/GetPlayerSpawnExclusionRadius/GetPlayerMultipleRespawnRadius/
     * GetWorldTypeRadiusMultiplier/GetGameProgressRadiusMultiplier ()D and m_iHardcoreSpawnTimeBetweenReassignments I;
     * FCUtilsWorld.GameProgressHasNetherBeenAccessedServerOnly()Z; FCTileEntityBeacon (extends btw.modern.TileEntityBeacon)
     * m_iEffectIDSpawnPoint I and m_bPlayerRespawnedAtBeacon Z. A failed link turns the hardcore respawn off for that
     * respawn (the player stays at 1.20.1's original spawn); it never stops a respawn.
     */
    private static final class Fc {
        static final String HS = "net.minecraft.src.btw.util.FCUtilsHardcoreSpawn";
        static final String UW = "net.minecraft.src.btw.util.FCUtilsWorld";
        static final String BEACON = "net.minecraft.src.btw.tileentity.FCTileEntityBeacon";

        static volatile boolean ok;
        static volatile String failure = "not resolved yet";
        static MethodHandle radius, exclusion, multiple, worldMultiplier, progressMultiplier, netherAccessed;
        static volatile int reassign = 24000;
        static int spawnEffect;
        static Class<?> beaconClass;
        static Field respawnedAtBeacon;

        static boolean resolve() {
            if (ok) return true;
            synchronized (Fc.class) {
                if (ok) return true;
                try {
                    MethodHandles.Lookup l = MethodHandles.lookup();
                    MethodType d = MethodType.methodType(double.class);
                    Class<?> hs = Class.forName(HS);
                    Class<?> uw = Class.forName(UW);
                    Class<?> bc = Class.forName(BEACON);
                    radius = l.findStatic(hs, "GetPlayerSpawnRadius", d);
                    exclusion = l.findStatic(hs, "GetPlayerSpawnExclusionRadius", d);
                    multiple = l.findStatic(hs, "GetPlayerMultipleRespawnRadius", d);
                    worldMultiplier = l.findStatic(hs, "GetWorldTypeRadiusMultiplier", d);
                    progressMultiplier = l.findStatic(hs, "GetGameProgressRadiusMultiplier", d);
                    netherAccessed = l.findStatic(uw, "GameProgressHasNetherBeenAccessedServerOnly", MethodType.methodType(boolean.class));
                    int window = hs.getField("m_iHardcoreSpawnTimeBetweenReassignments").getInt(null);
                    spawnEffect = bc.getField("m_iEffectIDSpawnPoint").getInt(null);
                    respawnedAtBeacon = bc.getField("m_bPlayerRespawnedAtBeacon");
                    if (!btw.modern.TileEntityBeacon.class.isAssignableFrom(bc)) {
                        throw new IllegalStateException(BEACON + " does not extend btw.modern.TileEntityBeacon");
                    }
                    beaconClass = bc;
                    reassign = window;
                    ok = true;
                    return true;
                } catch (Throwable t) {
                    failure = t.toString();
                    logOnce("FC links", t);
                    return false;
                }
            }
        }

        static double radius() throws Throwable { return (double) radius.invokeExact(); }
        static double exclusion() throws Throwable { return (double) exclusion.invokeExact(); }
        static double multiple() throws Throwable { return (double) multiple.invokeExact(); }
        static double worldMultiplier() throws Throwable { return (double) worldMultiplier.invokeExact(); }
        static double progressMultiplier() throws Throwable { return (double) progressMultiplier.invokeExact(); }
        static boolean netherAccessed() throws Throwable { return (boolean) netherAccessed.invokeExact(); }
    }
}
