package btw.api;

import java.util.Random;

public abstract class WorldGenerator {
    private boolean doBlockNotify;

    public WorldGenerator() {}
    public WorldGenerator(boolean notify) { this.doBlockNotify = notify; }

    public abstract boolean generate(World world, Random rand, int x, int y, int z);

    public void setScale(double xScale, double yScale, double zScale) {}

    public void setBlockAndMetadata(World world, int x, int y, int z, int blockID, int metadata) {}
}
