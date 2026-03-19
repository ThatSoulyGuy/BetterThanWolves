package btw.modern;

public class EntityClientPlayerMP extends EntityPlayerMP {

    public EntityClientPlayerMP(World world) {
        super(world);
    }

    public boolean attackEntityFrom(DamageSource source, int amount) {
        return false;
    }

    public void heal(int amount) {}
}
