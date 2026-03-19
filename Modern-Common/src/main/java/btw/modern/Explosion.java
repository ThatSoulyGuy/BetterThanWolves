package btw.modern;

import java.util.ArrayList;
import java.util.List;

public class Explosion {

    public boolean isFlaming;
    public boolean isSmoking = true;
    public double explosionX;
    public double explosionY;
    public double explosionZ;
    public Entity exploder;
    public float explosionSize;
    public List affectedBlockPositions = new ArrayList();
    public World worldObj;

    public Explosion(World world, Entity entity, double x, double y, double z, float size) {
        this.worldObj = world;
        this.exploder = entity;
        this.explosionSize = size;
        this.explosionX = x;
        this.explosionY = y;
        this.explosionZ = z;
    }

    public EntityLiving func_94613_c() {
        return (EntityLiving) this.exploder;
    }

    public void AddSecondaryExplosionNoFX(double x, double y, double z, float strength, boolean isFlaming, boolean isSmoking) {}
}
