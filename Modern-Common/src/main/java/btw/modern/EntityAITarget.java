package btw.modern;

public abstract class EntityAITarget extends EntityAIBase {

    protected EntityLiving taskOwner;

    public EntityAITarget(EntityLiving creature, boolean checkSight) {
        this.taskOwner = creature;
    }

    public EntityAITarget(EntityLiving creature, boolean checkSight, boolean nearbyOnly) {
        this.taskOwner = creature;
    }
}
