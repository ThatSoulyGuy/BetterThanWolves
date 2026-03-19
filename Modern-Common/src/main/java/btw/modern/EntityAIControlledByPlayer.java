package btw.modern;

public class EntityAIControlledByPlayer extends EntityAIBase {
    public EntityAIControlledByPlayer(EntityLiving entity, float speed) {}
    public boolean shouldExecute() { return false; }
    public boolean isPlayerSteerBoosted() { return false; }
}
