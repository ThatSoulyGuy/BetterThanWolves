package btw.modern;

public class PathFinder {
    private boolean canPathThroughClosedWoodDoor;
    private boolean canPathThroughOpenWoodDoor;
    private boolean canPathThroughWater;

    public PathFinder(Object... args) {}

    public PathEntity createEntityPathTo(Entity entity, Entity target, float maxDist) { return null; }
    public PathEntity createEntityPathTo(Entity entity, int x, int y, int z, float maxDist) { return null; }

    public boolean CanPathThroughClosedWoodDoor() { return canPathThroughClosedWoodDoor; }
    public boolean CanPathThroughOpenWoodDoor() { return canPathThroughOpenWoodDoor; }
    public boolean CanPathThroughWater() { return canPathThroughWater; }

    public void setCanPathThroughClosedWoodDoor(boolean value) { this.canPathThroughClosedWoodDoor = value; }
    public void setCanPathThroughOpenWoodDoor(boolean value) { this.canPathThroughOpenWoodDoor = value; }
    public void setCanPathThroughWater(boolean value) { this.canPathThroughWater = value; }
}
