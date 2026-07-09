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

    public int getBlockID(int x, int y, int z) {
        return worldObj.getBlockId(xPosition * 16 + x, y, zPosition * 16 + z);
    }

    public int getBlockMetadata(int x, int y, int z) {
        return worldObj.getBlockMetadata(xPosition * 16 + x, y, zPosition * 16 + z);
    }

    public boolean setBlockIDWithMetadata(int x, int y, int z, int id, int meta) {
        return worldObj.setBlock(xPosition * 16 + x, y, zPosition * 16 + z, id, meta, 3);
    }

    public int getBlockLightValue(int x, int y, int z, int skySubtracted) {
        if (worldObj != null) {
            return worldObj.getBlockLightValue(xPosition * 16 + x, y, zPosition * 16 + z);
        }
        return 15;
    }

    public int getHeightValue(int x, int z) { return 256; }
    public boolean canBlockSeeTheSky(int x, int y, int z) { return true; }
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
    public int getSavedLightValue(EnumSkyBlock type, int x, int y, int z) {
        if (worldObj != null) {
            return worldObj.getSavedLightValue(type, xPosition * 16 + x, y, zPosition * 16 + z);
        }
        return 15;
    }
    public void setLightValue(EnumSkyBlock type, int x, int y, int z, int value) {}
    public BiomeGenBase getBiomeGenForWorldCoords(int x, int z, WorldChunkManager manager) { return null; }
    public void getEntitiesWithinAABBForEntity(Entity excluded, AxisAlignedBB bb, List list, IEntitySelector selector) {}
    public void getEntitiesOfTypeWithinAAAB(Class clazz, AxisAlignedBB bb, List list, IEntitySelector selector) {}
    /**
     * Vanilla 1.5.2 ChunkCache calls this to decide whether the chunk
     * region has any non-air content. If it returns true ("levels ARE
     * empty"), ChunkCache sets hasExtendedLevels=true which makes ALL
     * block queries return 0 (air) — PathFinder sees an empty world
     * and returns null paths. Returning false ("no, levels are NOT
     * empty") forces ChunkCache to query blocks normally.
     */
    public boolean getAreLevelsEmpty(int minY, int maxY) {
        return false;
    }

    // 1.5.2 Chunk.getTopFilledSegment — FCTileEntityBeacon.java:613,678 beacon spawn
    // Y targeting; base Y of the highest non-empty section. The shim has no section
    // storage, so derive it from the world heightmap (real via WorldBridge:getHeightValue),
    // rounded down to a section boundary like vanilla.
    public int getTopFilledSegment() {
        if (worldObj != null) {
            // getHeightValue returns first-FREE y (topmost block + 1); derive the
            // section base from the topmost block, else terrain topping exactly at
            // a section boundary reports the empty section above it.
            int h = worldObj.getHeightValue(xPosition * 16 + 8, zPosition * 16 + 8);
            return Math.max((h - 1) & ~15, 0);
        }
        return 0;
    }

    // 1.5.2 Chunk.getRandomWithSeed (vanilla Chunk.java:1106) — deterministic per-chunk RNG
    // (slime chunks); EntitySlime.getCanSpawnHere uses it.
    public java.util.Random getRandomWithSeed(long seed) {
        return new java.util.Random(worldObj.getSeed()
                + (long) (xPosition * xPosition * 4987142)
                + (long) (xPosition * 5947611)
                + (long) (zPosition * zPosition) * 4392871L
                + (long) (zPosition * 389711) ^ seed);
    }

    // 1.5.2 Chunk.GetBlockNaturalLightValue (BTW-patched vanilla/server Chunk.java:1352) —
    // called by World.GetBlockNaturalLightValue_do; modified version of getBlockLightValue
    // that only considers natural (sky) light. The shim has no section storage, so it
    // reads the saved sky light through the world (real via WorldBridge.getSavedLightValue).
    public int GetBlockNaturalLightValue(int i, int j, int k, int iSkylightSubtracted) {
        if (worldObj == null) {
            return 0;
        }

        int iLightValue = (worldObj.provider != null && worldObj.provider.hasNoSky)
                ? 0
                : worldObj.getSavedLightValue(EnumSkyBlock.Sky, xPosition * 16 + i, j, zPosition * 16 + k);

        if (iLightValue > 0) {
            isLit = true;
        }

        iLightValue -= iSkylightSubtracted;

        if (iLightValue < 0) {
            iLightValue = 0;
        }

        return iLightValue;
    }
}
