package btw.modern;

public class PathNavigate {
    private EntityLiving theEntity;
    private World worldObj;
    private PathEntity currentPath;
    private float speed;
    private float pathSearchRange;
    private boolean noSunPathfind;
    private boolean canPassOpenWoodenDoors;
    private boolean canPassClosedWoodenDoors;
    private boolean avoidsWater;
    private boolean canSwim;

    public PathNavigate(EntityLiving entity, World world, float range) {
        this.theEntity = entity;
        this.worldObj = world;
        this.pathSearchRange = range;
    }

    public void setAvoidsWater(boolean flag) { this.avoidsWater = flag; }
    public boolean getAvoidsWater() { return this.avoidsWater; }
    public void setCanSwim(boolean flag) { this.canSwim = flag; }
    public void setBreakDoors(boolean flag) { this.canPassClosedWoodenDoors = flag; }
    public void setEnterDoors(boolean flag) { this.canPassOpenWoodenDoors = flag; }
    public boolean getCanBreakDoors() { return this.canPassClosedWoodenDoors; }
    public void setAvoidSun(boolean flag) { this.noSunPathfind = flag; }
    public void setSpeed(float speed) { this.speed = speed; }
    public float func_111269_d() { return pathSearchRange; }
    public boolean noPath() { return currentPath == null || currentPath.isFinished(); }
    public void clearPathEntity() { this.currentPath = null; }
    public PathEntity getPath() { return currentPath; }
    public PathEntity getPathToXYZ(double x, double y, double z) { return null; }
    public PathEntity getPathToXYZ(int x, int y, int z) { return null; }
    public PathEntity getPathToEntityLiving(EntityLiving entity) { return null; }
    public boolean tryMoveToXYZ(double x, double y, double z, float speed) { return false; }
    public boolean tryMoveToXYZ(int x, int y, int z, float speed) { return false; }
    public boolean tryMoveToEntityLiving(EntityLiving entity, float speed) { return false; }
    public boolean setPath(PathEntity path, float speed) { return false; }
    public boolean TryMoveToEntity(Entity entity, float speed) { return tryMoveToEntityLiving((EntityLiving) entity, speed); }
    public void onUpdateNavigation() {}

    // BTW-added methods
    public boolean CanPathThroughClosedWoodDoor() { return canPassClosedWoodenDoors; }
    public boolean CanPathThroughOpenWoodDoor() { return canPassOpenWoodenDoors; }
    public boolean CanPathThroughWater() { return canSwim; }
}
