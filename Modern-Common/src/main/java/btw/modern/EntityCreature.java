package btw.modern;

public abstract class EntityCreature extends EntityLiving {

    public Entity entityToAttack;
    public boolean hasAttacked;
    public PathEntity pathToEntity;
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

    public void setTarget(Entity target) {
        this.entityToAttack = target;
    }

    public void setPathToEntity(PathEntity path) {
        this.pathToEntity = path;
    }

    public boolean hasPath() {
        return pathToEntity != null && !pathToEntity.isFinished();
    }

    public PathEntity getPathToEntity() {
        return pathToEntity;
    }

    public float getBlockPathWeight(int x, int y, int z) {
        return 0.0F;
    }

    public static int AttemptToPossessCreaturesAroundBlock(World world, int x, int y, int z, int dim, int range) { return 0; }
}
