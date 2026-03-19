package btw.modern;

public class EntityAICreeperSwell extends EntityAIBase {

    public EntityLiving creeperAttackTarget;

    public EntityAICreeperSwell(EntityCreeper creeper) {
    }

    public boolean shouldExecute() {
        return false;
    }
}
