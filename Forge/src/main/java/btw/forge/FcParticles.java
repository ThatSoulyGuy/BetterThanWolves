package btw.forge;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FC's 1.5.2 particle names as 1.20.1 particles: the one table every FC world uses (INTEGRATIONS.md, fc_particles).
 *
 * <p>1.5.2's {@code World.spawnParticle} handed a name to {@code RenderGlobal.doSpawnParticle} (vanilla/client
 * RenderGlobal.java:1841-2072), which built one FX class per name. The rule here: a name maps to the 1.20.1 particle
 * that descends from the same 1.5.2 FX class; where 1.20.1 has none (FC's own four FX classes, and 1.5.2's
 * EntitySnowShovelFX) it maps to the BTW particle type that ports that class ({@link FcParticleTypes}).</p>
 *
 * <p>Client-side FC calls ({@link #addLocal}) are also limited as RenderGlobal.doSpawnParticle limited them
 * ({@link #limit}): the client installs a {@link LocalSink} that applies that rule in front of 1.20.1's
 * LevelRenderer (btw.forge.client.particle.LegacyParticleLimiter).</p>
 *
 * <p>Sinks: the client display world of {@code ProxyBlock} (randomDisplayTick), the client world of
 * {@code ProxyBlockEntity} (FC tile entities' client ticks), {@code ClientFcWorld} (FC's aux-FX handler and client
 * block notifications) through {@link #addLocal}; {@code WorldBridge} (server-side FC calls, forwarded to nearby
 * clients) through {@link #send}. Stateless apart from the sink and the once-per-failure log set; any thread.</p>
 */
// FCMOD-INTEGRATION(1.20.1): fc_particles: one table maps FC's 1.5.2 particle names for every FC world, so FC blocks show 4.B's particles under 4.B's particle limits
public final class FcParticles {

    private static final Logger LOGGER = LogManager.getLogger("BTW-Particles");

    /**
     * Every particle name FC 4.B passes to World.spawnParticle: the literal names in Common/, Client/ and Server/, the
     * ones FC passes through a variable (FCBlockMillStone.java:479-490 "smoke", "largesmoke", "fcwhitesmoke";
     * FCBlockCompanionCube.java:327 and FCEntityVillager.java:1651 "heart"), and the Modern-Common stand-ins
     * (Explosion, BlockFire). "iconcrack_&lt;item id&gt;" is checked separately.
     */
    static final List<String> FC_NAMES = List.of(
            "smoke", "largesmoke", "largeexplode", "hugeexplosion", "explode", "reddust", "snowballpoof", "snowshovel",
            "slime", "fcwhitesmoke", "fcwhitecloud", "fcsmallflame", "fccinders", "mobSpell", "note", "townaura",
            "portal", "lava", "flame", "dripLava", "dripWater", "bubble", "splash", "witchMagic", "enchantmenttable",
            "heart");

    /**
     * 1.5.2 RenderGlobal.doSpawnParticle's limit for a name (vanilla/client RenderGlobal.java:1841-1896).
     * ANY: added at any distance and particle setting (hugeexplosion, largeexplode, fireworksSpark, :1857-1868).
     * NEAR_32: added within 32 blocks of the view at any particle setting (FC's fccinders, :1869-1878).
     * NEAR_16: nothing beyond 16 blocks (:1887-1892), nothing at Minimal (:1893-1896), one in three dropped at
     * Decreased (:1845-1850): every other name.
     */
    public enum Limit { ANY, NEAR_32, NEAR_16 }

    /** The client's display sink (btw.forge.client.particle.LegacyParticleLimiter); null on a dedicated server. */
    @FunctionalInterface
    public interface LocalSink {
        void add(Level level, String name, ParticleOptions options, double x, double y, double z,
                 double vx, double vy, double vz);
    }

    private static volatile LocalSink localSink;

    /** Failures already logged, one WARN per name and exception class. */
    private static final Set<String> FAILURES_LOGGED = ConcurrentHashMap.newKeySet();

    private FcParticles() {}

    /** Installed once by the client (FcParticleProviders.register). */
    public static void setLocalSink(LocalSink sink) {
        localSink = sink;
    }

    /**
     * The 1.20.1 particle for a 1.5.2 particle name, or null for a name 1.5.2 drew nothing for. The velocity arguments
     * are FC's own: "reddust" reads its colour from them (1.5.2 EntityReddustFX, red 0 meaning 1).
     */
    @Nullable
    public static ParticleOptions options(String name, double vx, double vy, double vz) {
        if (name == null || name.isEmpty()) return null;
        if (name.startsWith("iconcrack_")) return itemCrack(name.substring("iconcrack_".length()));
        return switch (name) {
            // 1.5.2 vanilla FX -> their 1.20.1 descendants (RenderGlobal.java:1857-2045)
            case "hugeexplosion" -> ParticleTypes.EXPLOSION_EMITTER;     // EntityHugeExplodeFX
            case "largeexplode" -> ParticleTypes.EXPLOSION;              // EntityLargeExplodeFX
            case "explode" -> ParticleTypes.POOF;                        // EntityExplodeFX
            case "fireworksSpark" -> ParticleTypes.FIREWORK;
            case "bubble" -> ParticleTypes.BUBBLE;
            case "suspended" -> ParticleTypes.UNDERWATER;
            case "townaura" -> ParticleTypes.MYCELIUM;
            case "crit" -> ParticleTypes.CRIT;
            case "magicCrit" -> ParticleTypes.ENCHANTED_HIT;
            case "smoke" -> ParticleTypes.SMOKE;
            case "largesmoke" -> ParticleTypes.LARGE_SMOKE;
            case "mobSpell" -> ParticleTypes.ENTITY_EFFECT;              // colour in the velocity, as 1.20.1's
            case "mobSpellAmbient" -> ParticleTypes.AMBIENT_ENTITY_EFFECT;
            case "spell" -> ParticleTypes.EFFECT;
            case "instantSpell" -> ParticleTypes.INSTANT_EFFECT;
            case "witchMagic" -> ParticleTypes.WITCH;
            case "note" -> ParticleTypes.NOTE;                           // colour in vx, as 1.20.1's
            case "portal" -> ParticleTypes.PORTAL;
            case "enchantmenttable" -> ParticleTypes.ENCHANT;
            case "flame" -> ParticleTypes.FLAME;
            case "lava" -> ParticleTypes.LAVA;
            case "splash" -> ParticleTypes.SPLASH;
            case "cloud" -> ParticleTypes.CLOUD;                         // EntityCloudFX
            case "reddust" -> new DustParticleOptions(
                    new Vector3f(vx == 0.0D ? 1.0F : (float) vx, (float) vy, (float) vz), 1.0F);
            case "snowballpoof" -> ParticleTypes.ITEM_SNOWBALL;          // EntityBreakingFX(snowball)
            case "slime" -> ParticleTypes.ITEM_SLIME;                    // EntityBreakingFX(slimeBall)
            case "dripWater" -> ParticleTypes.DRIPPING_WATER;
            case "dripLava" -> ParticleTypes.DRIPPING_LAVA;
            case "heart" -> ParticleTypes.HEART;
            case "angryVillager" -> ParticleTypes.ANGRY_VILLAGER;
            case "happyVillager" -> ParticleTypes.HAPPY_VILLAGER;
            // no 1.20.1 descendant: BTW ports of the 1.5.2 classes (FcParticleTypes)
            case "snowshovel" -> FcParticleTypes.SNOW_SHOVEL.get();     // EntitySnowShovelFX
            case "fcwhitesmoke" -> FcParticleTypes.WHITE_SMOKE.get();   // FCClientEntityWhiteSmokeFX
            case "fcwhitecloud" -> FcParticleTypes.WHITE_CLOUD.get();   // FCClientEntityWhiteCloudFX
            case "fcsmallflame" -> FcParticleTypes.SMALL_FLAME.get();   // FCClientEntitySmallFlameFX
            case "fccinders" -> FcParticleTypes.CINDERS.get();          // FCClientEntityCindersFX
            default -> null;
        };
    }

    /** 1.5.2 RenderGlobal.doSpawnParticle's limit for a name (see {@link Limit}). */
    public static Limit limit(String name) {
        if (name == null) return Limit.NEAR_16;
        return switch (name) {
            case "hugeexplosion", "largeexplode", "fireworksSpark" -> Limit.ANY;
            case "fccinders" -> Limit.NEAR_32;
            default -> Limit.NEAR_16;
        };
    }

    /** 1.5.2 RenderGlobal.java:2036-2040: "iconcrack_&lt;item id&gt;" breaks the legacy item. */
    @Nullable
    private static ParticleOptions itemCrack(String id) {
        try {
            Item item = ProxyRegistry.getModernItem(Integer.parseInt(id));
            return item != null ? new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(item)) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** True when the 1.5.2 particle read its velocity arguments as data, not motion (EntityReddustFX's colour). */
    private static boolean velocityIsColour(String name) {
        return "reddust".equals(name);
    }

    /**
     * Spawns the particle on the client, as 1.5.2's client RenderGlobal did, through the installed {@link LocalSink}
     * (4.B's limit, then 1.20.1's LevelRenderer). A server level draws nothing, as 1.5.2's
     * WorldManager.spawnParticle (vanilla/server WorldManager.java:36). Unknown names draw nothing. Never throws: a
     * failure (for example a provider whose sprite set never bound) is logged once per name and exception class.
     */
    public static void addLocal(Level level, String name, double x, double y, double z, double vx, double vy, double vz) {
        if (level == null || !level.isClientSide()) return;
        try {
            ParticleOptions options = options(name, vx, vy, vz);
            if (options == null) return;
            if (velocityIsColour(name)) {
                vx = 0.0D;
                vy = 0.0D;
                vz = 0.0D;
            }
            LocalSink sink = localSink;
            if (sink != null) {
                sink.add(level, name, options, x, y, z, vx, vy, vz);
            } else {
                level.addParticle(options, x, y, z, vx, vy, vz);
            }
        } catch (Throwable t) {
            if (FAILURES_LOGGED.add(name + "|" + t.getClass().getName())) {
                LOGGER.warn("FC particle '{}' failed (further failures of this kind are not logged)", name, t);
            }
        }
    }

    /**
     * Sends the particle to the players within 32 blocks (ServerLevel.sendParticles): one particle with FC's velocity
     * (count 0, speed 1), or for "reddust" one still particle of FC's colour. 1.5.2's server drew nothing; this is the
     * port's forwarding, limited by 1.20.1's client limiter (the packet path).
     */
    public static void send(ServerLevel level, String name, double x, double y, double z, double vx, double vy, double vz) {
        ParticleOptions options = options(name, vx, vy, vz);
        if (options == null) return;
        if (velocityIsColour(name)) {
            level.sendParticles(options, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        } else {
            level.sendParticles(options, x, y, z, 0, vx, vy, vz, 1.0D);
        }
    }

    /**
     * Startup self-audit (BridgeSelfAudit.runCommon): every name FC spawns resolves, and the BTW types are registered.
     * One {@code [SELF-AUDIT/particles]} line; a WARN lists any name that would draw nothing.
     */
    public static void audit() {
        try {
            List<String> unresolved = new ArrayList<>();
            for (String name : FC_NAMES) {
                if (options(name, 0.0D, 0.0D, 0.0D) == null) unresolved.add(name);
            }
            if (options("iconcrack_280", 0.0D, 0.0D, 0.0D) == null) unresolved.add("iconcrack_280"); // stick
            List<String> types = new ArrayList<>();
            for (var entry : FcParticleTypes.PARTICLE_TYPES.getEntries()) {
                types.add(String.valueOf(BuiltInRegistries.PARTICLE_TYPE.getKey(entry.get())));
            }
            if (unresolved.isEmpty()) {
                LOGGER.info("[SELF-AUDIT/particles] all {} particle names FC spawns resolve (and iconcrack_<id>); BTW types {}",
                        FC_NAMES.size(), types);
            } else {
                LOGGER.warn("[SELF-AUDIT/particles] {} particle names FC spawns draw nothing: {}; BTW types {}",
                        unresolved.size(), unresolved, types);
            }
        } catch (Throwable t) {
            LOGGER.warn("[SELF-AUDIT/particles] the particle audit failed", t);
        }
    }
}
