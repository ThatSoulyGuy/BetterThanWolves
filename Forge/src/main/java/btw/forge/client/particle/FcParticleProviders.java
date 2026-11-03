package btw.forge.client.particle;

import btw.forge.FcParticleTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;

/**
 * Client providers for the BTW particle types (btw.forge.FcParticleTypes). Each type has a texture list in
 * assets/betterthanwolves/particles/&lt;name&gt;.json: ParticleEngine.reload binds a sprite set only from that file and
 * reports nothing when it is missing (the provider then throws on its first particle, which FcParticles.addLocal logs
 * once per name).
 */
// FCMOD-INTEGRATION(1.20.1): fc_particles: the client particles that draw FC's white smoke, white cloud, small flame and cinders and 1.5.2's snow-shovel puff
@OnlyIn(Dist.CLIENT)
public final class FcParticleProviders {

    private FcParticleProviders() {}

    /**
     * From BTWClientEvents (RegisterParticleProvidersEvent, mod bus, client only, once, before any level loads):
     * the five providers, and 4.B's limit for the particles FC spawns on the client (LegacyParticleLimiter).
     */
    public static void register(RegisterParticleProvidersEvent event) {
        LegacyParticleLimiter.install();
        event.registerSpriteSet(FcParticleTypes.WHITE_SMOKE.get(), FcWhiteSmokeParticle.Provider::new);
        event.registerSpriteSet(FcParticleTypes.WHITE_CLOUD.get(), FcWhiteCloudParticle.Provider::new);
        event.registerSpriteSet(FcParticleTypes.SMALL_FLAME.get(), FcSmallFlameParticle.Provider::new);
        event.registerSpriteSet(FcParticleTypes.CINDERS.get(), FcCindersParticle.Provider::new);
        event.registerSpriteSet(FcParticleTypes.SNOW_SHOVEL.get(), LegacySnowShovelParticle.Provider::new);
    }
}
