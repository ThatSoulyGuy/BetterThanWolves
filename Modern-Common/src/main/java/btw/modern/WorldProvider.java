package btw.modern;

public abstract class WorldProvider {
    public World worldObj;
    public WorldType terrainType;
    public WorldChunkManager worldChunkMgr;
    public boolean isHellWorld = false;
    public boolean hasNoSky = false;
    public float[] lightBrightnessTable = new float[16];
    public int dimensionId = 0;

    public static WorldProvider getProviderForDimension(int dim) { return null; }

    public void registerWorldChunkManager() {}
    public IChunkProvider createChunkGenerator() { return null; }
    public boolean canCoordinateBeSpawn(int x, int z) { return false; }
    public float calculateCelestialAngle(long time, float partialTicks) { return 0.0F; }
    public int getMoonPhase(long time) { return 0; }
    public boolean isSurfaceWorld() { return true; }
    public boolean canRespawnHere() { return true; }
    public boolean isSkyColored() { return true; }
    public String getSaveFolder() { return null; }
    public String getWelcomeMessage() { return null; }
    public String getDepartMessage() { return null; }
    public int getAverageGroundLevel() { return 64; }
    public double getMovementFactor() { return 1.0D; }
    public boolean doesXZShowFog(int x, int z) { return false; }
    public int getHeight() { return 256; }
}
