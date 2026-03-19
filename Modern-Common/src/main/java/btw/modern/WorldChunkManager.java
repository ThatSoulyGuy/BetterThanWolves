package btw.modern;

import java.util.List;
import java.util.Random;

public class WorldChunkManager {
    public WorldChunkManager() {}
    public WorldChunkManager(long seed, Object worldType) {}
    public WorldChunkManager(World world) {}

    public BiomeGenBase getBiomeGenAt(int x, int z) { return null; }
    public float[] getRainfall(float[] arr, int x, int z, int width, int height) { return arr; }
    public float[] getTemperatures(float[] arr, int x, int z, int width, int height) { return arr; }
    public BiomeGenBase[] getBiomesForGeneration(BiomeGenBase[] biomes, int x, int z, int width, int height) { return biomes; }
    public BiomeGenBase[] loadBlockGeneratorData(BiomeGenBase[] biomes, int x, int z, int width, int height) { return biomes; }
    public BiomeGenBase[] getBiomeGenAt(BiomeGenBase[] biomes, int x, int z, int width, int height, boolean cache) { return biomes; }
    public boolean areBiomesViable(int x, int z, int radius, List biomes) { return false; }
    public ChunkPosition findBiomePosition(int x, int z, int radius, List biomes, Random rand) { return null; }
    public void cleanupCache() {}
}
