package btw.modern;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity sensing (sight) helper.
 * 1.5.2 EntitySenses — fc EntityAITarget.isSuitableTarget/continueExecuting call
 * taskOwner.getEntitySenses().canSee(target) every targeting tick for all live
 * FC mobs, so this gates aggro on real line of sight.
 */
public class EntitySenses {

    EntityLiving entityObj;

    /** Cache of entities which we can see */
    List seenEntities = new ArrayList();

    /** Cache of entities which we cannot see */
    List unseenEntities = new ArrayList();

    /**
     * Compile-only: the DEAD Modern-Common EntityLiving shim constructs
     * EntitySenses without an owner. The frozen runtime EntityLiving always
     * uses the (EntityLiving) constructor below.
     */
    public EntitySenses() {}

    /** Vanilla 1.5.2 EntityLiving constructor calls {@code new EntitySenses(this)}. */
    public EntitySenses(EntityLiving entity) {
        this.entityObj = entity;
    }

    /**
     * 1.5.2 EntitySenses.clearSensingCache — frozen EntityLiving.updateAITasks
     * calls this once per tick to invalidate the per-tick canSee() memoization.
     */
    public void clearSensingCache() {
        this.seenEntities.clear();
        this.unseenEntities.clear();
    }

    /**
     * 1.5.2 EntitySenses.canSee — checks the per-tick caches, then delegates to
     * the frozen EntityLiving.canEntityBeSeen (eye-to-eye raytrace backed by
     * WorldBridge.rayTraceBlocks), caching the result.
     */
    public boolean canSee(Entity entity) {
        if (this.seenEntities.contains(entity)) {
            return true;
        } else if (this.unseenEntities.contains(entity)) {
            return false;
        } else {
            this.entityObj.worldObj.theProfiler.startSection("canSee");
            boolean canSee = this.entityObj.canEntityBeSeen(entity);
            this.entityObj.worldObj.theProfiler.endSection();

            if (canSee) {
                this.seenEntities.add(entity);
            } else {
                this.unseenEntities.add(entity);
            }

            return canSee;
        }
    }
}
