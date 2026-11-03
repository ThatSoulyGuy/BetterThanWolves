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
 * "fcsmallflame" (betterthanwolves:small_flame): Client FCClientEntitySmallFlameFX.java:22-94, the candle flame
 * (FCBlockCandle.java:197-208 spawns three at one point "so that it's a little more steady"). Half a flame, no position
 * jitter (1.20.1's small_flame jitters 0.05), 1.5.2 EntityFX's 0.98 drag, no collision, 12-44 ticks, brightening
 * and shrinking to half as it ages.
 */
@OnlyIn(Dist.CLIENT)
public class FcSmallFlameParticle extends LegacyFxParticle {

    /** FC's m_fStartScale. */
    private final float startQuadSize;

    protected FcSmallFlameParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
        super(level, x, y, z, vx, vy, vz);                 // FC: super(world, x, y, z, vx, vy, vz)
        this.xd = this.xd * (double) 0.01F + vx;
        this.yd = this.yd * (double) 0.01F + vy;
        this.zd = this.zd * (double) 0.01F + vz;
        this.quadSize *= 0.5F;
        this.startQuadSize = this.quadSize;
        this.lifetime = (int) (8.0D / (Math.random() * 0.8D + 0.2D)) + 4;
        this.hasPhysics = false;                           // FC: noClip = true
    }

    private float decay(float partialTick) {
        return Mth.clamp(((float) this.age + partialTick) / (float) this.lifetime, 0.0F, 1.0F);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    public float getQuadSize(float partialTick) {
        float decay = decay(partialTick);
        return this.startQuadSize * (1.0F - decay * decay * 0.5F);
    }

    /** FC getBrightnessForRender: block light rises to full over the life. */
    @Override
    protected int getLightColor(float partialTick) {
        int packed = super.getLightColor(partialTick);
        int block = (packed & 255) + (int) (decay(partialTick) * 15.0F * 16.0F);
        if (block > 240) block = 240;
        return block | (packed >> 16 & 255) << 16;
    }

    /** 1.5.2 EntityFX.onUpdate (vanilla/client EntityFX.java:143-165; FC does not override it): no gravity, 0.98 drag. */
    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            remove();
            return;
        }
        move(this.xd, this.yd, this.zd);
        this.xd *= (double) 0.98F;
        this.yd *= (double) 0.98F;
        this.zd *= (double) 0.98F;
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
            FcSmallFlameParticle particle = new FcSmallFlameParticle(level, x, y, z, vx, vy, vz);
            particle.pickSprite(this.sprites);             // FC: setParticleTextureIndex(48), the flame
            return particle;
        }
    }
}
