package btw.api;

public interface IChunkProvider {
    boolean chunkExists(int x, int z);
    Chunk provideChunk(int x, int z);
    void populate(IChunkProvider provider, int x, int z);
    boolean saveChunks(boolean flag, Object progress);
    boolean unloadQueuedChunks();
    boolean canSave();
    String makeString();
}
