package btw.api;

public class PathFinder {
    public PathFinder(Object... args) {}

    public PathEntity createEntityPathTo(Entity entity, Entity target, float maxDist) { return null; }
    public PathEntity createEntityPathTo(Entity entity, int x, int y, int z, float maxDist) { return null; }

    public boolean CanPathThroughClosedWoodDoor() { return false; }
    public boolean CanPathThroughOpenWoodDoor() { return false; }
    public boolean CanPathThroughWater() { return false; }
}
