package btw.modern;

public abstract class EntityCreature extends EntityLiving {

    public Entity entityToAttack;
    public boolean hasAttacked;
    public int fleeingTick;

    protected EntityCreature(World world) {
        super(world);
    }

    public boolean isMovementCeased() {
        return false;
    }

    public Entity getEntityToAttack() {
        return entityToAttack;
    }

    public void setPathToEntity(PathEntity path) {}

    public static int AttemptToPossessCreaturesAroundBlock(World world, int x, int y, int z, int dim, int range) { return 0; }
}
