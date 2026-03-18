package btw.api;

public class EntityAIAttackOnCollide extends EntityAIBase {

    public EntityLiving attacker;

    public EntityAIAttackOnCollide(EntityLiving entity, Class targetClass, float speed, boolean longMemory) {
        this.attacker = entity;
    }

    public EntityAIAttackOnCollide(EntityLiving entity, float speed, boolean longMemory) {
        this.attacker = entity;
    }

    public boolean shouldExecute() {
        return false;
    }
}
