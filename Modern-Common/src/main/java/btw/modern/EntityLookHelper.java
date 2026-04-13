package btw.modern;

/**
 * Helper for controlling entity head/body look direction.
 * Mirrors net.minecraft.src.EntityLookHelper.
 */
public class EntityLookHelper {

    public EntityLookHelper() {}
    /** Vanilla 1.5.2 EntityLiving constructor calls {@code new EntityLookHelper(this)}. */
    public EntityLookHelper(EntityLiving entity) {}

    public void setLookPositionWithEntity(Entity entity, float maxYawChange, float maxPitchChange) {}
    public void setLookPosition(double x, double y, double z, float maxYawChange, float maxPitchChange) {}
    public void onUpdateLook() {}
}
