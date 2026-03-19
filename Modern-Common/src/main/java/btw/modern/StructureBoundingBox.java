package btw.modern;

public class StructureBoundingBox {
    public int minX;
    public int minY;
    public int minZ;
    public int maxX;
    public int maxY;
    public int maxZ;

    public StructureBoundingBox() {}

    public StructureBoundingBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public StructureBoundingBox(int minX, int minZ, int maxX, int maxZ) {
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
        this.minY = 1;
        this.maxY = 512;
    }

    public boolean intersectsWith(StructureBoundingBox other) { return false; }
    public boolean intersectsWith(int minX, int minZ, int maxX, int maxZ) { return false; }
    public void expandTo(StructureBoundingBox other) {}
    public void offset(int x, int y, int z) {}
    public boolean isVecInside(int x, int y, int z) { return false; }
    public int getXSize() { return maxX - minX + 1; }
    public int getYSize() { return maxY - minY + 1; }
    public int getZSize() { return maxZ - minZ + 1; }
    public int getCenterX() { return minX + (maxX - minX + 1) / 2; }
    public int getCenterY() { return minY + (maxY - minY + 1) / 2; }
    public int getCenterZ() { return minZ + (maxZ - minZ + 1) / 2; }
    public static StructureBoundingBox getNewBoundingBox() { return new StructureBoundingBox(); }
}
