package btw.modern;

/**
 * Player capabilities (creative mode, flying, etc.).
 * Mirrors net.minecraft.src.PlayerCapabilities.
 */
public class PlayerCapabilities {

    public boolean disableDamage;
    public boolean isFlying;
    public boolean allowFlying;
    public boolean isCreativeMode;
    public boolean allowEdit = true;
    public float flySpeed = 0.05F;
    public float walkSpeed = 0.1F;

    public void writeCapabilitiesToNBT(NBTTagCompound tag) {}
    public void readCapabilitiesFromNBT(NBTTagCompound tag) {}

    public float getFlySpeed() { return flySpeed; }
    public float getWalkSpeed() { return walkSpeed; }
}
