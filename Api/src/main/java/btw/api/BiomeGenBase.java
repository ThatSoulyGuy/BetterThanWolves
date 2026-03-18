package btw.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BiomeGenBase {
    public static final BiomeGenBase[] biomeList = new BiomeGenBase[256];
    public static BiomeGenBase ocean;
    public static BiomeGenBase plains;
    public static BiomeGenBase desert;
    public static BiomeGenBase extremeHills;
    public static BiomeGenBase forest;
    public static BiomeGenBase taiga;
    public static BiomeGenBase swampland;
    public static BiomeGenBase river;
    public static BiomeGenBase hell;
    public static BiomeGenBase sky;
    public static BiomeGenBase frozenOcean;
    public static BiomeGenBase frozenRiver;
    public static BiomeGenBase icePlains;
    public static BiomeGenBase iceMountains;
    public static BiomeGenBase mushroomIsland;
    public static BiomeGenBase mushroomIslandShore;
    public static BiomeGenBase beach;
    public static BiomeGenBase desertHills;
    public static BiomeGenBase forestHills;
    public static BiomeGenBase taigaHills;
    public static BiomeGenBase extremeHillsEdge;
    public static BiomeGenBase jungle;
    public static BiomeGenBase jungleHills;

    public String biomeName;
    public int color;
    public byte topBlock;
    public byte fillerBlock;
    public int field_76754_C;
    public float temperature;
    public float rainfall;
    public int waterColorMultiplier;
    public BiomeDecorator theBiomeDecorator;
    public int biomeID;
    public boolean enableSnow;
    public boolean enableRain;
    public List spawnableMonsterList = new ArrayList();
    public List spawnableCreatureList = new ArrayList();
    public List spawnableWaterCreatureList = new ArrayList();
    public List spawnableCaveCreatureList = new ArrayList();

    protected BiomeGenBase(int id) {
        this.biomeID = id;
        this.topBlock = (byte) Block.grass.blockID;
        this.fillerBlock = (byte) Block.dirt.blockID;
        this.waterColorMultiplier = 16777215;
        this.temperature = 0.5F;
        this.rainfall = 0.5F;
        if (id >= 0 && id < biomeList.length) biomeList[id] = this;
    }

    public BiomeGenBase setTemperatureRainfall(float temp, float rain) {
        this.temperature = temp;
        this.rainfall = rain;
        return this;
    }

    public BiomeGenBase setBiomeName(String name) {
        this.biomeName = name;
        return this;
    }

    public BiomeGenBase setColor(int color) {
        this.color = color;
        return this;
    }

    public BiomeGenBase setDisableRain() {
        this.enableRain = false;
        return this;
    }

    public BiomeGenBase setEnableSnow() {
        this.enableSnow = true;
        return this;
    }

    public int getIntTemperature() { return (int)(this.temperature * 65536.0F); }
    public int getIntRainfall() { return (int)(this.rainfall * 65536.0F); }
    public float getFloatTemperature() { return this.temperature; }
    public float getFloatRainfall() { return this.rainfall; }

    public boolean canSpawnLightningBolt() { return !this.enableSnow && this.enableRain; }
    public boolean getEnableSnow() { return this.enableSnow; }
    public boolean isHighHumidity() { return this.rainfall > 0.85F; }
    public float getSpawningChance() { return 0.1F; }

    public void decorate(World world, Random rand, int x, int z) {}
    public int getBiomeGrassColor() { return 0; }
    public int getBiomeFoliageColor() { return 0; }
    public int getSkyColorByTemp(float temp) { return 0; }

    public boolean CanLightningStrikeInBiome() { return canSpawnLightningBolt(); }
    public boolean CanRainInBiome() { return this.enableRain; }
}
