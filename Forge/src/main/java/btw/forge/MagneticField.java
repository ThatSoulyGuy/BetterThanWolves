package btw.forge;

import btw.modern.FCMagneticPoint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 4.B's magnetic field per player (vanilla/server EntityPlayerMP.java:1094-1152), sent to the client compass. */
// FCMOD-INTEGRATION(1.20.1): magnetic_compass: 4.B's magnetic field (the world spawn and every powered BTW beacon but a glass one) steers the 1.20.1 compass; a lodestone compass keeps its lodestone
@Mod.EventBusSubscriber(modid = BTWForgeMod.MOD_ID)
public final class MagneticField {
    private MagneticField() {}

    private static final Logger LOGGER = LogManager.getLogger("BTW-Magnetic");
    private static final Map<UUID, Optional<GlobalPos>> LAST = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer sp) || sp instanceof FakePlayer) return;
        try {
            update(sp);
        } catch (Throwable t) {
            FCPuppetSupport.logFailureOnce(LOGGER, "magnetic field", sp, t);
        }
    }

    /** 1.5.2 EntityPlayerMP.UpdateMagneticInfluences, every 40 ticks staggered by entity id; the result goes to the
     *  player only when it changes (1.5.2: DataWatcher 22-24). */
    static void update(ServerPlayer sp) {
        ServerLevel sl = sp.serverLevel();
        if ((sl.getGameTime() + sp.getId()) % 40L != 0L || !BTWLifecycle.isFcReady()) return;
        Optional<GlobalPos> now = Optional.ofNullable(strongest(sl, sp.getX(), sp.getZ()));
        if (!now.equals(LAST.put(sp.getUUID(), now))) BTWNetwork.sendMagneticTarget(sp, now.orElse(null));
    }

    /** The strongest point at x/z: in a natural dimension (1.5.2 isSurfaceWorld; 1.20.1 CompassItem.getSpawnPosition's
     *  test) the world spawn at field level 2 competes with the beacons; elsewhere only beacons, with background noise.
     *  The spawn point is 1.5.2's FCMagneticPoint(spawnX, 0, spawnZ, 2): its y is 0 (the compass reads only x/z). */
    @Nullable
    public static GlobalPos strongest(ServerLevel sl, double x, double z) {
        List<?> points = WorldBridge.getOrCreate(sl).GetMagneticPointList().m_MagneticPoints;
        FCMagneticPoint best = null;
        double bestStrength = 0D;
        boolean surface = sl.dimensionType().natural();
        if (surface) {
            BlockPos spawn = sl.getSharedSpawnPos();
            best = new FCMagneticPoint(spawn.getX(), 0, spawn.getZ(), 2);
            bestStrength = best.GetFieldStrengthRelativeToPosition(x, z);
        }
        for (Object o : points) {
            FCMagneticPoint p = (FCMagneticPoint) o;
            double s = surface ? p.GetFieldStrengthRelativeToPosition(x, z)
                               : p.GetFieldStrengthRelativeToPositionWithBackgroundNoise(x, z);
            if (s > bestStrength) {
                best = p;
                bestStrength = s;
            }
        }
        return best == null ? null : GlobalPos.of(sl.dimension(), new BlockPos(best.m_iIPos, best.m_iJPos, best.m_iKPos));
    }

    /** The last target sent to this player (proof verb 'magnetic player'). */
    @Nullable
    public static GlobalPos lastTarget(ServerPlayer sp) {
        Optional<GlobalPos> o = LAST.get(sp.getUUID());
        return o == null ? null : o.orElse(null);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { LAST.remove(e.getEntity().getUUID()); }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent e) { LAST.clear(); }
}
