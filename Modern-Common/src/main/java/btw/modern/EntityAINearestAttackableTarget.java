package btw.modern;

public class EntityAINearestAttackableTarget extends EntityAITarget {

    public EntityAINearestAttackableTarget(EntityLiving creature, Class targetClass, float distance, int chance, boolean checkSight) {
        super(creature, checkSight);
    }

    public EntityAINearestAttackableTarget(EntityLiving creature, Class targetClass, float distance, int chance, boolean checkSight, boolean nearbyOnly) {
        super(creature, checkSight);
    }

    public EntityAINearestAttackableTarget(EntityLiving creature, Class targetClass, float distance, int chance, boolean checkSight, boolean nearbyOnly, IEntitySelector selector) {
        super(creature, checkSight);
    }

    public boolean shouldExecute() {
        return false;
    }
}
