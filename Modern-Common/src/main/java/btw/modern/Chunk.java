package btw.modern;

import java.util.List;
import java.util.Map;

public class Chunk {
    public static boolean isLit;
    public int[] precipitationHeightMap;
    public boolean[] updateSkylightColumns;
    public boolean isTerrainPopulated;
    public boolean isModified;
    public boolean hasEntities;
    public long lastSaveTime;
    public boolean isChunkLoaded;
    public int heightMapMinimum;
    public long inhabitedTime;

    public final int xPosition;
    public final int zPosition;
    public World worldObj;
    public int[] heightMap;
    public Map chunkTileEntityMap;
    public List[] entityLists;

    public Chunk(World world, int x, int z) {
        this.worldObj = world;
        this.xPosition = x;
        this.zPosition = z;
    }

    public Chunk(World world, byte[] data, int x, int z) {
        this(world, x, z);
    }

    public int getBlockID(int x, int y, int z) { return 0; }
    public int getBlockMetadata(int x, int y, int z) { return 0; }
    public boolean setBlockIDWithMetadata(int x, int y, int z, int id, int meta) { return false; }
    public int getBlockLightValue(int x, int y, int z, int skySubtracted) { return 0; }
    public int getHeightValue(int x, int z) { return 0; }
    public boolean isAtLocation(int x, int z) { return this.xPosition == x && this.zPosition == z; }
    public boolean needsSaving(boolean flag) { return this.isModified; }
    public void setChunkModified() { this.isModified = true; }
    public void addEntity(Entity entity) {}
    public void removeEntity(Entity entity) {}
    public void removeEntityAtIndex(Entity entity, int index) {}
    public TileEntity getChunkBlockTileEntity(int x, int y, int z) { return null; }
    public void setChunkBlockTileEntity(int x, int y, int z, TileEntity te) {}
    public void removeTileEntity(int x, int y, int z) {}
    public void onChunkLoad() {}
    public void onChunkUnload() {}
    public void generateHeightMap() {}
    public void generateSkylightMap() {}
    public int getSavedLightValue(EnumSkyBlock type, int x, int y, int z) { return 0; }
    public void setLightValue(EnumSkyBlock type, int x, int y, int z, int value) {}
    public BiomeGenBase getBiomeGenForWorldCoords(int x, int z, WorldChunkManager manager) { return null; }
    public void getEntitiesWithinAABBForEntity(Entity excluded, AxisAlignedBB bb, List list, IEntitySelector selector) {}
    public void getEntitiesOfTypeWithinAAAB(Class clazz, AxisAlignedBB bb, List list, IEntitySelector selector) {}
    public int getTopFilledSegment() { return 0; }
}
