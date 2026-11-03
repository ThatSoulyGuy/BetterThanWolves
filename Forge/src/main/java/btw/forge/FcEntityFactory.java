package btw.forge;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FCMOD-INTEGRATION(1.20.1): fc_entity_construction: how an FC entity is built for its proxy (triage CF2-R1),
 * resolved once per class. 1.5.2 loaded an entity through its (World) constructor (EntityList.createEntityFromNBT,
 * vanilla/server EntityList.java:122) and built a client-spawned one through the class's own constructor with the
 * spawn packet's position and fields (vanilla/client NetClientHandler.java:290-296). Rule: a public (World)
 * constructor; else a no-argument one; else the public (World, double x, double y, double z, primitives...)
 * constructor with the fewest parameters, called with the proxy's position and zeros, whose state the caller then
 * applies (spawn payload or save). FCEntityFallingBlock and FCEntityLightningBolt have only the last kind. The
 * living proxies (ProxyMob, ProxyAnimal, ProxyPathfinderMob) accept WORLD and NO_ARG only.
 */
public final class FcEntityFactory {
    public enum Kind { WORLD, NO_ARG, POSITIONAL, NONE }

    private static final ConcurrentHashMap<String, FcEntityFactory> BY_CLASS = new ConcurrentHashMap<>();

    private final Kind kind;
    private final Constructor<?> ctor;
    private final String failure;

    private FcEntityFactory(Kind kind, Constructor<?> ctor, String failure) {
        this.kind = kind;
        this.ctor = ctor;
        this.failure = failure;
    }

    /** The cached rule for this FC class name. Never throws: a failed resolution is kind NONE. */
    public static FcEntityFactory of(String className) {
        return BY_CLASS.computeIfAbsent(className, FcEntityFactory::resolve);
    }

    public static Kind kindOf(String className) {
        return of(className).kind;
    }

    public Kind kind() { return kind; }
    public boolean usable() { return kind != Kind.NONE; }
    /** A living proxy can build it: (World) or no-argument. */
    public boolean livingUsable() { return kind == Kind.WORLD || kind == Kind.NO_ARG; }
    public String failure() { return failure; }

    private static FcEntityFactory resolve(String className) {
        // Throwable: getConstructor(s) can throw a LinkageError (a parameter class missing); it must not escape
        // computeIfAbsent (nothing would be cached and every call would throw again).
        try {
            Class<?> c = Class.forName(className);
            if (!btw.modern.Entity.class.isAssignableFrom(c) || Modifier.isAbstract(c.getModifiers())) {
                return new FcEntityFactory(Kind.NONE, null, className + " is not a concrete FC entity class");
            }
            try {
                return new FcEntityFactory(Kind.WORLD, c.getConstructor(btw.modern.World.class), null);
            } catch (NoSuchMethodException ignored) {
            }
            try {
                Constructor<?> k = c.getDeclaredConstructor();
                k.setAccessible(true);
                return new FcEntityFactory(Kind.NO_ARG, k, null);
            } catch (NoSuchMethodException | RuntimeException ignored) {
            }
            Constructor<?> best = Arrays.stream(c.getConstructors())
                    .filter(FcEntityFactory::isPositional)
                    .min(Comparator.<Constructor<?>>comparingInt(Constructor::getParameterCount)
                            .thenComparing(k -> Arrays.toString(k.getParameterTypes())))
                    .orElse(null);
            return best != null ? new FcEntityFactory(Kind.POSITIONAL, best, null)
                    : new FcEntityFactory(Kind.NONE, null, "no public (World) or no-argument constructor and no public "
                            + "(World, double, double, double, primitives...) one");
        } catch (Throwable t) {
            return new FcEntityFactory(Kind.NONE, null, t.toString());
        }
    }

    private static boolean isPositional(Constructor<?> k) {
        Class<?>[] p = k.getParameterTypes();
        if (p.length < 4 || p[0] != btw.modern.World.class
                || p[1] != double.class || p[2] != double.class || p[3] != double.class) return false;
        for (int i = 4; i < p.length; i++) {
            if (!p[i].isPrimitive()) return false;
        }
        return true;
    }

    /**
     * Builds the FC entity: (world) / () / (world, x, y, z, 0...). Throws the constructor's own failure (an
     * InvocationTargetException wraps it) or IllegalStateException for kind NONE.
     */
    public btw.modern.Entity create(btw.modern.World world, double x, double y, double z)
            throws ReflectiveOperationException {
        switch (kind) {
            case WORLD: return (btw.modern.Entity) ctor.newInstance(world);
            case NO_ARG: return (btw.modern.Entity) ctor.newInstance();
            case POSITIONAL: {
                Class<?>[] p = ctor.getParameterTypes();
                Object[] args = new Object[p.length];
                args[0] = world;
                args[1] = x;
                args[2] = y;
                args[3] = z;
                for (int i = 4; i < p.length; i++) args[i] = zero(p[i]);
                return (btw.modern.Entity) ctor.newInstance(args);
            }
            default: throw new IllegalStateException(failure);
        }
    }

    /** The root cause of a create() failure (unwraps InvocationTargetException). */
    public static Throwable rootCause(Throwable e) {
        if (e instanceof java.lang.reflect.InvocationTargetException ite && ite.getCause() != null) {
            return ite.getCause();
        }
        return e;
    }

    private static Object zero(Class<?> t) {
        if (t == boolean.class) return Boolean.FALSE;
        if (t == char.class) return '\0';
        if (t == byte.class) return (byte) 0;
        if (t == short.class) return (short) 0;
        if (t == int.class) return 0;
        if (t == long.class) return 0L;
        if (t == float.class) return 0F;
        return 0D;
    }
}
