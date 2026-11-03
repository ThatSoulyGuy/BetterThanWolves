package btw.forge.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/**
 * A 1.5.2 EntityFX's body and movement on a 1.20.1 particle, for the BTW particle types that port 1.5.2 FX classes
 * (btw.forge.FcParticleTypes; INTEGRATIONS.md, fc_particles).
 * <ul>
 *   <li>Position: 1.5.2's EntityFX is 0.2 x 0.2 with {@code yOffset = height / 2} (vanilla/client EntityFX.java:57-59),
 *       so its drawn point sits 0.1 above the floor of its box; a 1.20.1 particle's point is the floor itself.</li>
 *   <li>Movement: FC's {@code Entity.moveEntity} (vanilla/client Entity.java:2674-2945) with {@code noClip} false: the
 *       box collides with blocks, and each blocked axis zeroes that motion. 1.20.1's {@code Particle.move} instead
 *       freezes a particle for good once a vertical move is blocked ({@code stoppedByCollision}), and keeps the
 *       vertical motion.</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
abstract class LegacyFxParticle extends TextureSheetParticle {

    /** 1.5.2 EntityFX.yOffset: half of its 0.2 height. */
    private static final double Y_OFFSET = 0.1D;

    protected LegacyFxParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
        super(level, x, y, z, vx, vy, vz);
    }

    /** 1.5.2 Entity.setPosition: the box's floor is yOffset below the point. Runs from Particle's constructor. */
    @Override
    public void setPos(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        float half = this.bbWidth / 2.0F;
        setBoundingBox(new AABB(x - half, y - Y_OFFSET, z - half, x + half, y - Y_OFFSET + this.bbHeight, z + half));
    }

    @Override
    protected void setLocationFromBoundingbox() {
        AABB box = getBoundingBox();
        this.x = (box.minX + box.maxX) / 2.0D;
        this.y = box.minY + Y_OFFSET;
        this.z = (box.minZ + box.maxZ) / 2.0D;
    }

    /** 1.5.2 moveEntity for an FX (FC's replacement, vanilla/client Entity.java:2674-2945): see the class comment. */
    @Override
    public void move(double dx, double dy, double dz) {
        double wantX = dx, wantY = dy, wantZ = dz;
        if (this.hasPhysics && (dx != 0.0D || dy != 0.0D || dz != 0.0D) && dx * dx + dy * dy + dz * dz < 10000.0D) {
            Vec3 allowed = Entity.collideBoundingBox(null, new Vec3(dx, dy, dz), getBoundingBox(), this.level, List.of());
            dx = allowed.x;
            dy = allowed.y;
            dz = allowed.z;
        }
        if (dx != 0.0D || dy != 0.0D || dz != 0.0D) {
            setBoundingBox(getBoundingBox().move(dx, dy, dz));
            setLocationFromBoundingbox();
        }
        this.onGround = wantY != dy && wantY < 0.0D;
        if (wantX != dx) this.xd = 0.0D;
        if (wantY != dy) this.yd = 0.0D;
        if (wantZ != dz) this.zd = 0.0D;
    }
}
