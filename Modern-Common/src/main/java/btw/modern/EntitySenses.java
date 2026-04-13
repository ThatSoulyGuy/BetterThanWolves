package btw.modern;

/**
 * Entity sensing (sight) helper.
 * Mirrors net.minecraft.src.EntitySenses.
 */
public class EntitySenses {

    public EntitySenses() {}
    /** Vanilla 1.5.2 EntityLiving constructor calls {@code new EntitySenses(this)}. */
    public EntitySenses(EntityLiving entity) {}

    /**
     * Vanilla 1.5.2 EntityAITarget.isSuitableTarget checks
     * {@code shouldCheckSight && !canSee(target)} — if canSee returns false,
     * the AI rejects EVERY target and the mob never attacks/follows anything.
     * A true default is the safe no-op: "can always see" means the AI task
     * delegates targeting to distance/path checks instead of LOS.
     */
    public boolean canSee(Entity entity) { return true; }

    /**
     * Vanilla 1.5.2 EntityLiving.updateAITasks() calls this once per tick to
     * invalidate the per-tick canSee() memoization. We don't memoize, so
     * the no-op is correct — but the method must exist or the call site NPEs.
     */
    public void clearSensingCache() {}
}
