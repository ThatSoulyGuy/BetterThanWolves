package btw.api;

public class PathEntity {
    private PathPoint[] points;
    private int pathLength;
    private int pathIndex;

    public PathEntity(PathPoint[] points) {
        this.points = points;
        this.pathLength = points.length;
    }

    public void incrementPathIndex() { this.pathIndex++; }
    public boolean isFinished() { return this.pathIndex >= this.pathLength; }
    public PathPoint getFinalPathPoint() {
        return pathLength > 0 ? points[pathLength - 1] : null;
    }
    public PathPoint getPathPointFromIndex(int index) { return points[index]; }
    public int getCurrentPathLength() { return pathLength; }
    public void setCurrentPathLength(int length) { this.pathLength = length; }
    public int getCurrentPathIndex() { return pathIndex; }
    public void setCurrentPathIndex(int index) { this.pathIndex = index; }
    public Vec3 getVectorFromIndex(Entity entity, int index) { return null; }
    public Vec3 getPosition(Entity entity) { return null; }
    public boolean isSamePath(PathEntity other) { return false; }
    public boolean isDestinationSame(Vec3 vec) { return false; }
}
