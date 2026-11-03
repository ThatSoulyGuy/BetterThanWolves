package btw.forge;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * The particle types BTW 4.B spawned that 1.20.1 has no counterpart for (INTEGRATIONS.md, fc_particles): FC's own four client FX
 * classes, and 1.5.2's snow-shovel puff, which 1.20.1 dropped. {@link FcParticles} maps FC's 1.5.2 names to them; the
 * client particles that draw them live in {@code btw.forge.client.particle} (registered from BTWClientEvents).
 *
 * <p>Registered on both sides (the server sends them through WorldBridge, and Forge syncs the particle registry at
 * login). Particle types are never saved, so worlds are unaffected.</p>
 */
// FCMOD-INTEGRATION(1.20.1): fc_particles: FC's own particles (white smoke, white cloud, small flame, cinders) and 1.5.2's snow-shovel puff as BTW particle types
public final class FcParticleTypes {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, BTWForgeMod.MOD_ID);

    /** "fcwhitesmoke": Client FCClientEntityWhiteSmokeFX. Limited like every 1.5.2 particle (vanilla/client RenderGlobal.java:1845-1850, :1887-1896). */
    public static final RegistryObject<SimpleParticleType> WHITE_SMOKE =
            PARTICLE_TYPES.register("white_smoke", () -> new SimpleParticleType(false));

    /** "fcwhitecloud": Client FCClientEntityWhiteCloudFX (white smoke that does not rise). Limited. */
    public static final RegistryObject<SimpleParticleType> WHITE_CLOUD =
            PARTICLE_TYPES.register("white_cloud", () -> new SimpleParticleType(false));

    /** "fcsmallflame": Client FCClientEntitySmallFlameFX (the candle flame). Limited. */
    public static final RegistryObject<SimpleParticleType> SMALL_FLAME =
            PARTICLE_TYPES.register("small_flame", () -> new SimpleParticleType(false));

    /**
     * "fccinders": Client FCClientEntityCindersFX. 1.5.2's RenderGlobal spawned cinders ahead of the particle setting,
     * within 32 blocks of the view (vanilla/client RenderGlobal.java:1870-1878), so the type overrides the limiter and
     * the client provider keeps the 32-block range.
     */
    public static final RegistryObject<SimpleParticleType> CINDERS =
            PARTICLE_TYPES.register("cinders", () -> new SimpleParticleType(true));

    /** "snowshovel": 1.5.2 EntitySnowShovelFX (FC's snow golem effect, aux 2263); 1.20.1 has no such particle. Limited. */
    public static final RegistryObject<SimpleParticleType> SNOW_SHOVEL =
            PARTICLE_TYPES.register("snow_shovel", () -> new SimpleParticleType(false));

    private FcParticleTypes() {}

    /** Hooks the types into the mod event bus. Called once, from the mod constructor. */
    public static void register(IEventBus modEventBus) {
        PARTICLE_TYPES.register(modEventBus);
    }
}
