package btw.api;

public abstract class EntityAITarget extends EntityAIBase {

    protected EntityCreature taskOwner;
    protected EntityLiving taskOwnerLiving;

    public EntityAITarget(EntityCreature creature, boolean checkSight) {
        this.taskOwner = creature;
        this.taskOwnerLiving = creature;
    }

    public EntityAITarget(EntityCreature creature, boolean checkSight, boolean nearbyOnly) {
        this.taskOwner = creature;
        this.taskOwnerLiving = creature;
    }

    public EntityAITarget(EntityLiving creature, boolean checkSight) {
        this.taskOwnerLiving = creature;
    }
}
