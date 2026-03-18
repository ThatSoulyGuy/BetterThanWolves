package btw.api;

import java.util.Random;

public class WorldGenMinable extends WorldGenerator {
    private int minableBlockId;
    private int numberOfBlocks;
    private int blockTarget;

    public WorldGenMinable(int blockId, int count) {
        this.minableBlockId = blockId;
        this.numberOfBlocks = count;
    }

    public WorldGenMinable(int blockId, int count, int target) {
        this(blockId, count);
        this.blockTarget = target;
    }

    public boolean generate(World world, Random rand, int x, int y, int z) { return false; }
}
