package btw.api;

/**
 * AI task for sitting behavior (tamed animals).
 * Mirrors net.minecraft.src.EntityAISit.
 */
public class EntityAISit extends EntityAIBase {

    private boolean isSitting;

    public EntityAISit(EntityLiving entity) {
    }

    public boolean shouldExecute() {
        return false;
    }

    public void setSitting(boolean sitting) {
        this.isSitting = sitting;
    }

    public boolean isSitting() {
        return this.isSitting;
    }
}
