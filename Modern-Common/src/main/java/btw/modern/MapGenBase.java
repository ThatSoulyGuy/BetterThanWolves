package btw.modern;

import java.util.Random;

/**
 * Base class for world generation features that operate on chunk data.
 * Mirrors net.minecraft.src.MapGenBase.
 */
public class MapGenBase {

    protected Random rand = new Random();
    protected long worldSeed;

    /**
     * Called during chunk population to generate structures/features.
     * Subclasses override this to implement specific generation logic.
     */
    public void generate(IChunkProvider provider, World world, int chunkX, int chunkZ, byte[] blockArray) {
        // Override point - subclasses provide generation logic
    }
}
