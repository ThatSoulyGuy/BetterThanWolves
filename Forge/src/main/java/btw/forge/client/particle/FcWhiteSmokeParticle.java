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
 * "fcwhitesmoke" (betterthanwolves:white_smoke): Client FCClientEntityWhiteSmokeFX.java:22-97, line for line. A
 * near-white puff (shade 0.7-1.0) on the eight generic smoke sprites, 0.375-0.75 blocks across, that grows in over
 * its first 1/32 of life, lives 17-65 ticks and rises 0.004 blocks per tick per tick (1.5.2's EntityCloudFX plus the
 * rise). Campfire, kiln, cauldron and crucible, smouldering log, millstone and unfired brick.
 */
@OnlyIn(Dist.CLIENT)
public class FcWhiteSmokeParticle extends LegacyFxParticle {

    private final SpriteSet sprites;
    /** FC's m_fParticleScale. */
    private final float startQuadSize;

    protected FcWhiteSmokeParticle(ClientLevel level, double x, double y, double z,
                                   double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);          // FC: super(world, x, y, z, 0, 0, 0)
        this.sprites = sprites;
        this.xd = this.xd * (double) 0.1F + vx;
        this.yd = this.yd * (double) 0.1F + vy;
        this.zd = this.zd * (double) 0.1F + vz;
        float shade = 1.0F - (float) (Math.random() * (double) 0.3F);
        setColor(shade, shade, shade);
        this.quadSize *= 0.75F;
        this.quadSize *= 2.5F;
        this.startQuadSize = this.quadSize;
        this.lifetime = (int) ((float) (int) (8.0D / (Math.random() * 0.8D + 0.3D)) * 2.5F);
        this.hasPhysics = true;                            // FC: noClip = false
        setSprite(sprites.get(7, 7));                      // 1.5.2's texture index 0 until the first update
    }

    /** False for white cloud, which does not rise (FCClientEntityWhiteCloudFX.onUpdate). */
    protected boolean rises() {
        return true;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /** FC renderParticle: the scale grows from 0 over the first 1/32 of the life. */
    @Override
    public float getQuadSize(float partialTick) {
        return this.startQuadSize * Mth.clamp(((float) this.age + partialTick) / (float) this.lifetime * 32.0F, 0.0F, 1.0F);
    }

    /** FC onUpdate (FCClientEntityWhiteSmokeFX.java:67-97; FCClientEntityWhiteCloudFX.java:32-55 without the rise). */
    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            remove();
            return;
        }
        // 1.5.2 setParticleTextureIndex(7 - age * 8 / maxAge) over generic_7..generic_0 (the JSON's order)
        setSprite(this.sprites.get(Math.min(this.age * 8 / this.lifetime, 7), 7));
        if (rises()) this.yd += 0.004D;
        move(this.xd, this.yd, this.zd);
        if (rises() && this.y == this.yo) {
            this.xd *= 1.1D;
            this.zd *= 1.1D;
        }
        this.xd *= (double) 0.96F;
        this.yd *= (double) 0.96F;
        this.zd *= (double) 0.96F;
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
            return new FcWhiteSmokeParticle(level, x, y, z, vx, vy, vz, this.sprites);
        }
    }
}
