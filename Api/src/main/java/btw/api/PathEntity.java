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
    public Vec3 getVectorFromIndex(Entity entity, int index) {
        PathPoint point = this.points[index];
        return Vec3.createVectorHelper(
            (double) point.xCoord + (double) ((int) (entity.width + 1.0F)) * 0.5D,
            (double) point.yCoord,
            (double) point.zCoord + (double) ((int) (entity.width + 1.0F)) * 0.5D
        );
    }

    public Vec3 getPosition(Entity entity) {
        return this.getVectorFromIndex(entity, this.pathIndex);
    }

    public boolean isSamePath(PathEntity other) {
        if (other == null) {
            return false;
        }
        if (other.points.length != this.points.length) {
            return false;
        }
        for (int i = 0; i < this.points.length; i++) {
            if (!this.points[i].equals(other.points[i])) {
                return false;
            }
        }
        return true;
    }

    public boolean isDestinationSame(Vec3 vec) {
        PathPoint finalPoint = this.getFinalPathPoint();
        if (finalPoint == null) {
            return false;
        }
        return finalPoint.xCoord == (int) vec.xCoord
            && finalPoint.zCoord == (int) vec.zCoord;
    }
}
