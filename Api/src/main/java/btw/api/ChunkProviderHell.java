package btw.api;

public class ChunkProviderHell implements IChunkProvider {
    public MapGenNetherBridge genNetherBridge;

    public ChunkProviderHell(World world, long seed) {}

    public boolean chunkExists(int x, int z) { return false; }
    public Chunk provideChunk(int x, int z) { return null; }
    public void populate(IChunkProvider provider, int x, int z) {}
    public boolean saveChunks(boolean flag, IProgressUpdate progress) { return false; }
    public boolean unloadQueuedChunks() { return false; }
    public boolean canSave() { return false; }
    public String makeString() { return ""; }
}
