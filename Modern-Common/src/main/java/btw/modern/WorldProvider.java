package btw.modern;

public abstract class WorldProvider {
    public World worldObj;
    public WorldType terrainType;
    public WorldChunkManager worldChunkMgr;
    public boolean isHellWorld = false;
    public boolean hasNoSky = false;
    public float[] lightBrightnessTable = new float[16];
    public int dimensionId = 0;

    public static WorldProvider getProviderForDimension(int dim) {
        // Factory method - returns null in this stub; Forge bridge provides real providers
        return null;
    }

    public void registerWorldChunkManager() {}
    public IChunkProvider createChunkGenerator() { return null; }

    public boolean canCoordinateBeSpawn(int x, int z) {
        return true;
    }

    public float calculateCelestialAngle(long time, float partialTicks) {
        int j = (int)(time % 24000L);
        float f1 = ((float)j + partialTicks) / 24000.0F - 0.25F;

        if (f1 < 0.0F) {
            f1 += 1.0F;
        }
        if (f1 > 1.0F) {
            f1 -= 1.0F;
        }

        float f2 = f1;
        f1 = 1.0F - (float)((Math.cos((double)f1 * Math.PI) + 1.0D) / 2.0D);
        f1 = f2 + (f1 - f2) / 3.0F;
        return f1;
    }

    public int getMoonPhase(long time) {
        return (int)(time / 24000L % 8L + 8L) % 8;
    }

    public boolean isSurfaceWorld() {
        return !isHellWorld;
    }

    public boolean canRespawnHere() {
        return !isHellWorld;
    }

    public boolean isDaytime() {
        float celestialAngle = this.calculateCelestialAngle(
                worldObj != null ? worldObj.getWorldTime() : 0L, 0.0F);
        return celestialAngle >= 0.0F && celestialAngle < 0.5F;
    }

    public String getDimensionName() {
        switch (dimensionId) {
            case -1: return "Nether";
            case 0:  return "Overworld";
            case 1:  return "The End";
            default: return "DIM" + dimensionId;
        }
    }

    public boolean isSkyColored() { return true; }
    public String getSaveFolder() { return null; }
    public String getWelcomeMessage() { return null; }
    public String getDepartMessage() { return null; }
    public int getAverageGroundLevel() { return 64; }
    public double getMovementFactor() { return 1.0D; }
    public boolean doesXZShowFog(int x, int z) { return false; }
    public int getHeight() { return 256; }
}
