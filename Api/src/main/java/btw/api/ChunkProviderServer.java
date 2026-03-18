package btw.api;

import java.util.List;
import java.util.Set;

public class ChunkProviderServer implements IChunkProvider {
    public Set droppedChunksSet;
    public IChunkProvider currentChunkProvider;
    public Object currentChunkLoader;
    public Object worldObj;
    public List loadedChunks;
    public Object id2ChunkMap;

    public boolean chunkExists(int x, int z) { return false; }
    public Chunk provideChunk(int x, int z) { return null; }
    public Chunk loadChunk(int x, int z) { return null; }
    public void populate(IChunkProvider provider, int x, int z) {}
    public boolean saveChunks(boolean flag, Object progress) { return false; }
    public boolean unloadQueuedChunks() { return false; }
    public boolean canSave() { return false; }
    public String makeString() { return ""; }
    public int getLoadedChunkCount() { return 0; }

    public void unloadChunksIfNotNearSpawn(int x, int z) {}
    public void unloadAllChunks() {}
    public IChunkProvider GetCurrentProvider() { return currentChunkProvider; }
}
