package btw.modern;

// Compile-time stub — real implementation provided by vanilla remap at runtime
public class EntityAISit extends EntityAIBase {
    private boolean sitting;
    public EntityAISit(EntityLiving entity) { setMutexBits(5); }
    public boolean shouldExecute() { return this.sitting; }
    public void setSitting(boolean sitting) { this.sitting = sitting; }
    public boolean isSitting() { return this.sitting; }
}
