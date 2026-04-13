package btw.modern;

public class PathFinder {
    private boolean canPathThroughClosedWoodDoor;
    private boolean canPathThroughOpenWoodDoor;
    private boolean canPathThroughWater;

    /**
     * Must match vanilla 1.5.2's exact constructor signature so the
     * compiler generates bytecode that resolves against the real class
     * at runtime (the stub is deleted post-compile).
     */
    public PathFinder(IBlockAccess blockAccess, boolean canBreakDoors,
                      boolean canEnterDoors, boolean avoidsWater, boolean canSwim) {}

    public PathEntity createEntityPathTo(Entity entity, Entity target, float maxDist) { return null; }
    public PathEntity createEntityPathTo(Entity entity, int x, int y, int z, float maxDist) { return null; }

    public boolean CanPathThroughClosedWoodDoor() { return canPathThroughClosedWoodDoor; }
    public boolean CanPathThroughOpenWoodDoor() { return canPathThroughOpenWoodDoor; }
    public boolean CanPathThroughWater() { return canPathThroughWater; }

    public void setCanPathThroughClosedWoodDoor(boolean value) { this.canPathThroughClosedWoodDoor = value; }
    public void setCanPathThroughOpenWoodDoor(boolean value) { this.canPathThroughOpenWoodDoor = value; }
    public void setCanPathThroughWater(boolean value) { this.canPathThroughWater = value; }
}
