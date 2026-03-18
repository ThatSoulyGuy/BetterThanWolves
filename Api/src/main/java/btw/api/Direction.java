package btw.api;

public class Direction {
    public static final int[] offsetX = {0, -1, 0, 1};
    public static final int[] offsetZ = {1, 0, -1, 0};
    public static final int[] facingToDirection = {-1, -1, 2, 0, 1, 3};
    public static final int[] directionToFacing = {3, 4, 2, 5};
    public static final int[] rotateOpposite = {2, 3, 0, 1};
    public static final int[] bedDirection = {1, 2, 3, 0};
}
