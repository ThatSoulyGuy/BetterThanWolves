package btw.api;

public class MovingObjectPosition {

    public EnumMovingObjectType typeOfHit;
    public int blockX;
    public int blockY;
    public int blockZ;
    public int sideHit;
    public Vec3 hitVec;
    public Entity entityHit;

    public static final int TYPE_BLOCK = 0;
    public static final int TYPE_ENTITY = 1;

    public MovingObjectPosition(int blockX, int blockY, int blockZ, int sideHit, Vec3 hitVec) {
        this.typeOfHit = EnumMovingObjectType.TILE;
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
        this.sideHit = sideHit;
        this.hitVec = hitVec;
    }

    public MovingObjectPosition(Entity entity) {
        this.typeOfHit = EnumMovingObjectType.ENTITY;
        this.entityHit = entity;
    }
}
