package btw.forge.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * "snowshovel" (betterthanwolves:snow_shovel): 1.5.2 EntitySnowShovelFX (vanilla/client EntitySnowShovelFX.java:17-90),
 * which 1.20.1 dropped. A near-white puff on the generic smoke sprites that falls (0.03 per tick per tick). FC spawns
 * 120 of them when a snow golem is built (aux 2263, Client FCBetterThanWolves.java:3618-3623).
 */
@OnlyIn(Dist.CLIENT)
public class LegacySnowShovelParticle extends LegacyFxParticle {

    private final SpriteSet sprites;
    private final float startQuadSize;

    protected LegacySnowShovelParticle(ClientLevel level, double x, double y, double z,
                                       double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz);                 // 1.5.2: super(world, x, y, z, vx, vy, vz)
        this.sprites = sprites;
        this.xd = this.xd * (double) 0.1F + vx;
        this.yd = this.yd * (double) 0.1F + vy;
        this.zd = this.zd * (double) 0.1F + vz;
        float shade = 1.0F - (float) (Math.random() * (double) 0.3F);
        setColor(shade, shade, shade);
        this.quadSize *= 0.75F;                            // scale argument 1.0
        this.startQuadSize = this.quadSize;
        this.lifetime = (int) ((float) (int) (8.0D / (Math.random() * 0.8D + 0.2D)) * 1.0F);
        this.hasPhysics = true;
        setSprite(sprites.get(7, 7));
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public float getQuadSize(float partialTick) {
        return this.startQuadSize * Mth.clamp(((float) this.age + partialTick) / (float) this.lifetime * 32.0F, 0.0F, 1.0F);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            remove();
            return;
        }
        setSprite(this.sprites.get(Math.min(this.age * 8 / this.lifetime, 7), 7));
        this.yd -= 0.03D;
        move(this.xd, this.yd, this.zd);
        this.xd *= (double) 0.99F;
        this.yd *= (double) 0.99F;
        this.zd *= (double) 0.99F;
        if (this.onGround) {
            this.xd *= (double) 0.7F;
            this.zd *= (double) 0.7F;
        }
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
            return new LegacySnowShovelParticle(level, x, y, z, vx, vy, vz, this.sprites);
        }
    }
}
