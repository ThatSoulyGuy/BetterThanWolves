package btw.forge.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * "fcwhitecloud" (betterthanwolves:white_cloud): Client FCClientEntityWhiteCloudFX.java:25-55, white smoke that does
 * not rise. Loose snow and loose snow slabs (FCBlockSnowLoose.java:273-288, FCBlockSnowLooseSlab.java:307-322) and
 * the cooking brick oven (FCBlockFurnaceBrick.java:531).
 */
@OnlyIn(Dist.CLIENT)
public class FcWhiteCloudParticle extends FcWhiteSmokeParticle {

    protected FcWhiteCloudParticle(ClientLevel level, double x, double y, double z,
                                   double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz, sprites);
    }

    @Override
    protected boolean rises() {
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new FcWhiteCloudParticle(level, x, y, z, vx, vy, vz, this.sprites);
        }
    }
}
