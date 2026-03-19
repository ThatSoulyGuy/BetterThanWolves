package btw.modern;

import java.util.Random;

public class WorldGenBigTree extends WorldGenerator {

    public World worldObj;
    public static final byte[] otherCoordPairs = new byte[] {(byte)2, (byte)0, (byte)0, (byte)1, (byte)2, (byte)1};
    public int[] basePos = new int[] {0, 0, 0};
    public int heightLimit = 0;
    public int heightLimitLimit = 12;

    public WorldGenBigTree(boolean notify) { super(notify); }
    public boolean generate(World world, Random rand, int x, int y, int z) { return false; }
    public void genTreeLayer(int x, int y, int z, float radius, byte axis, int blockID) {}
    public int checkBlockLine(int[] start, int[] end) { return 0; }
    public boolean validTreeLocation() { return false; }
}
