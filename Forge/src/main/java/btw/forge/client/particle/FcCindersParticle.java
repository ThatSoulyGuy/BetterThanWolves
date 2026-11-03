package btw.forge.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * "fccinders" (betterthanwolves:cinders): Client FCClientEntityCindersFX.java:22-107, the glowing sparks a smouldering
 * log throws when it bursts (FC aux 2277, Client FCBetterThanWolves.java:3830-3845). 1.5.2's EntityLavaFX with 1.5x
 * horizontal spread (lava: 0.8x): the lava sprite at full brightness, thrown up 0.05-0.45, falling 0.03 per tick per
 * tick, shrinking as it ages and trailing smoke. As in 1.5.2 RenderGlobal.java:1870-1878 it ignores the particle
 * setting (the type overrides the limiter) and is never spawned 32 blocks or more from the view (the provider).
 */
@OnlyIn(Dist.CLIENT)
public class FcCindersParticle extends LegacyFxParticle {

    private final float startQuadSize;

    protected FcCindersParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);           // FC: super(world, x, y, z, 0, 0, 0)
        this.xd *= 1.5D;
        this.yd *= 2.0D;
        this.zd *= 1.5D;
        this.yd = this.random.nextDouble() * 0.4D + 0.05D;
        setColor(1.0F, 1.0F, 1.0F);
        this.quadSize *= this.random.nextFloat() * 2.0F + 0.2F;
        this.startQuadSize = this.quadSize;
        this.lifetime = (int) (16.0D / (Math.random() * 0.8D + 0.2D));
        this.hasPhysics = true;                            // FC: noClip = false
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    /** FC getBrightnessForRender: full block light, the world's sky light. */
    @Override
    protected int getLightColor(float partialTick) {
        return 240 | (super.getLightColor(partialTick) >> 16 & 255) << 16;
    }

    @Override
    public float getQuadSize(float partialTick) {
        float age = ((float) this.age + partialTick) / (float) this.lifetime;
        return this.startQuadSize * (1.0F - age * age);
    }

    /** FC onUpdate (:78-107): a smoke trail while young, gravity 0.03, drag 0.999. */
    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            remove();
            return;
        }
        float ageFraction = (float) this.age / (float) this.lifetime;
        if (this.random.nextFloat() > ageFraction) {
            this.level.addParticle(ParticleTypes.SMOKE, this.x, this.y, this.z, this.xd, this.yd, this.zd);
        }
        this.yd -= 0.03D;
        move(this.xd, this.yd, this.zd);
        this.xd *= (double) 0.999F;
        this.yd *= (double) 0.999F;
        this.zd *= (double) 0.999F;
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

        /** RenderGlobal.java:1872-1877: only within 32 blocks of the view, whatever the particle setting. */
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double vx, double vy, double vz) {
            if (Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(x, y, z) >= 32.0D * 32.0D) {
                return null;
            }
            FcCindersParticle particle = new FcCindersParticle(level, x, y, z);
            particle.pickSprite(this.sprites);             // FC: setParticleTextureIndex(49), the lava spark
            return particle;
        }
    }
}
