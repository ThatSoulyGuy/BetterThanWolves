package btw.forge.client.particle;

import btw.forge.FcParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 1.5.2 RenderGlobal.doSpawnParticle's limit (vanilla/client RenderGlobal.java:1841-1896) for the particles FC spawns on
 * the client, in front of 1.20.1's LevelRenderer.addParticle. {@link FcParticles#limit} names the rule per name:
 * <ul>
 *   <li>ANY: forced (the explosion names), as 1.20.1's own explosion types.</li>
 *   <li>NEAR_32: within 32 blocks of the view, forced (FC's cinders).</li>
 *   <li>NEAR_16: within 16 blocks of the view, then 1.20.1's setting check (none at Minimal, one in three dropped
 *       at Decreased, as 1.5.2), whatever the type's own override flag (so "explode" = 1.20.1's POOF is limited, as
 *       1.5.2's EntityExplodeFX was).</li>
 * </ul>
 * The distance is taken from the camera, as 1.20.1's limiter and the cinders provider (1.5.2: the view entity).
 */
// FCMOD-INTEGRATION(1.20.1): fc_particles: FC's client-side particle calls are limited as 4.B's RenderGlobal limited them
@OnlyIn(Dist.CLIENT)
public final class LegacyParticleLimiter {

    private LegacyParticleLimiter() {}

    /** Installs the limiter as FcParticles' local sink (from FcParticleProviders.register, client only). */
    public static void install() {
        FcParticles.setLocalSink(LegacyParticleLimiter::add);
    }

    static void add(Level level, String name, ParticleOptions options, double x, double y, double z,
                    double vx, double vy, double vz) {
        Minecraft mc = Minecraft.getInstance();
        if (level != mc.level) {
            level.addParticle(options, x, y, z, vx, vy, vz);   // not the rendered level: 1.20.1's own path
            return;
        }
        double distSq = mc.gameRenderer.getMainCamera().getPosition().distanceToSqr(x, y, z);
        switch (FcParticles.limit(name)) {
            case ANY -> mc.levelRenderer.addParticle(options, true, x, y, z, vx, vy, vz);
            case NEAR_32 -> {
                if (distSq < 32.0D * 32.0D) mc.levelRenderer.addParticle(options, true, x, y, z, vx, vy, vz);
            }
            case NEAR_16 -> {
                if (distSq <= 16.0D * 16.0D) mc.levelRenderer.addParticle(options, false, x, y, z, vx, vy, vz);
            }
        }
    }
}
