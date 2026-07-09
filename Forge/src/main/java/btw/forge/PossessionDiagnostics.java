package btw.forge;

import net.minecraft.world.entity.Entity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DIAGNOSTIC (temporary): traces BTW's creature-possession pipeline — the chain that
 * turns possessed squids into overworld ghasts, which is the source of the "random
 * ghast_hurt" sounds heard in a fresh world.
 *
 * <p>Per the FC logic, possession never starts spontaneously in the overworld
 * ({@code EntityCreature.HandlePossession} only self-seeds when
 * {@code provider.dimensionId == -1}, i.e. the Nether); it has to be seeded by an
 * already-possessed creature. So a possession-level {@code 0 -> 1} transition logged in
 * the overworld, in a world that has never been to the Nether and has no portal, is the
 * bug we're hunting.</p>
 *
 * <p>The possession level is read reflectively: {@code GetPossessionLevel()} is declared
 * on the frozen (shadowed) {@code EntityCreature}, which we must not reference at compile
 * time, and non-creature FC entities don't have it at all. This lives entirely in the
 * bridge layer — no FC source is touched.</p>
 */
public final class PossessionDiagnostics {

    private static final Logger LOGGER = LogManager.getLogger("BTW-Possession");

    /** Last observed possession level per FC entity (weak so dead entities clear out). */
    private static final Map<Object, Integer> LAST_LEVEL =
            Collections.synchronizedMap(new WeakHashMap<>());

    /** Cached GetPossessionLevel() lookup per FC class; {@link #MISSING} marks non-creatures. */
    private static final Map<Class<?>, Method> METHODS = new ConcurrentHashMap<>();
    private static final Method MISSING;
    static {
        Method m;
        try { m = Object.class.getMethod("hashCode"); } catch (NoSuchMethodException e) { m = null; }
        MISSING = m;
    }

    private PossessionDiagnostics() {}

    /**
     * Poll one FC creature's possession level and log any transition. Call once per
     * server tick per proxy, after {@code fcEntity.onUpdate()}.
     */
    public static void poll(Entity mcEntity, Object fcEntity) {
        if (mcEntity == null || fcEntity == null) return;
        if (mcEntity.level().isClientSide()) return;

        Method m = METHODS.computeIfAbsent(fcEntity.getClass(), c -> {
            try { return c.getMethod("GetPossessionLevel"); }
            catch (NoSuchMethodException e) { return MISSING; }
        });
        if (m == MISSING) return;

        int level;
        try {
            Object r = m.invoke(fcEntity);
            if (!(r instanceof Integer)) return;
            level = (Integer) r;
        } catch (Throwable t) {
            return;
        }

        Integer prev = LAST_LEVEL.get(fcEntity);
        int prevLevel = prev == null ? 0 : prev;
        if (level == prevLevel) return;
        LAST_LEVEL.put(fcEntity, level);
        if (level == 0 && prevLevel == 0) return;

        LOGGER.info("possession {}->{} {} id={} dim={} pos=({}, {}, {})",
                prevLevel, level,
                fcEntity.getClass().getSimpleName(), mcEntity.getId(),
                mcEntity.level().dimension().location(),
                fmt(mcEntity.getX()), fmt(mcEntity.getY()), fmt(mcEntity.getZ()));
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }
}
