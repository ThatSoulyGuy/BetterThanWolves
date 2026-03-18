package btw.api;

public abstract class BehaviorProjectileDispense extends BehaviorDefaultDispenseItem {
    protected abstract IProjectile getProjectileEntity(World world, IPosition position);
    protected float func_82498_a() { return 6.0F; }
    protected float func_82500_b() { return 1.1F; }
}
