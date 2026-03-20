package btw.modern;

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
        this.enableRain = true;
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
    public int getBiomeGrassColor() { return 0x7CBD6B; }
    public int getBiomeFoliageColor() { return 0x59AE30; }
    public int getSkyColorByTemp(float temp) { return 0; }

    public boolean CanLightningStrikeInBiome() { return canSpawnLightningBolt(); }
    public boolean CanRainInBiome() { return this.enableRain; }

    /**
     * Populates vanilla biome static fields. Called during Forge mod init
     * BEFORE FC code runs, but AFTER Block.initializeVanillaBlocks() since
     * the BiomeGenBase constructor accesses Block.grass.blockID and Block.dirt.blockID.
     * IDs and settings match vanilla MC 1.5.2.
     */
    public static void initializeVanillaBiomes() {
        ocean              = new ConcreteBiome(0).setColor(112).setBiomeName("Ocean").setTemperatureRainfall(0.5F, 0.5F);
        plains             = new ConcreteBiome(1).setColor(9286496).setBiomeName("Plains").setTemperatureRainfall(0.8F, 0.4F);
        desert             = new BiomeGenDesert(2).setColor(16421912).setBiomeName("Desert").setDisableRain().setTemperatureRainfall(2.0F, 0.0F);
        extremeHills       = new ConcreteBiome(3).setColor(6316128).setBiomeName("Extreme Hills").setTemperatureRainfall(0.2F, 0.3F);
        forest             = new BiomeGenForest(4).setColor(353825).setBiomeName("Forest").setTemperatureRainfall(0.7F, 0.8F);
        taiga              = new BiomeGenTaiga(5).setColor(747097).setBiomeName("Taiga").setEnableSnow().setTemperatureRainfall(0.05F, 0.8F);
        swampland          = new ConcreteBiome(6).setColor(522674).setBiomeName("Swampland").setTemperatureRainfall(0.8F, 0.9F);
        river              = new ConcreteBiome(7).setColor(255).setBiomeName("River").setTemperatureRainfall(0.5F, 0.5F);
        hell               = new BiomeGenHell(8).setColor(16711680).setBiomeName("Hell").setDisableRain().setTemperatureRainfall(2.0F, 0.0F);
        sky                = new BiomeGenEnd(9).setColor(8421631).setBiomeName("Sky").setDisableRain();
        frozenOcean        = new ConcreteBiome(10).setColor(9474208).setBiomeName("FrozenOcean").setEnableSnow().setTemperatureRainfall(0.0F, 0.5F);
        frozenRiver        = new ConcreteBiome(11).setColor(10526975).setBiomeName("FrozenRiver").setEnableSnow().setTemperatureRainfall(0.0F, 0.5F);
        icePlains          = new BiomeGenSnow(12).setColor(16777215).setBiomeName("Ice Plains").setEnableSnow().setTemperatureRainfall(0.0F, 0.5F);
        iceMountains       = new BiomeGenSnow(13).setColor(10526880).setBiomeName("Ice Mountains").setEnableSnow().setTemperatureRainfall(0.0F, 0.5F);
        mushroomIsland     = new ConcreteBiome(14).setColor(16711935).setBiomeName("MushroomIsland").setTemperatureRainfall(0.9F, 1.0F);
        mushroomIslandShore = new ConcreteBiome(15).setColor(10486015).setBiomeName("MushroomIslandShore").setTemperatureRainfall(0.9F, 1.0F);
        beach              = new ConcreteBiome(16).setColor(16440917).setBiomeName("Beach").setTemperatureRainfall(0.8F, 0.4F);
        desertHills        = new BiomeGenDesert(17).setColor(13786898).setBiomeName("DesertHills").setDisableRain().setTemperatureRainfall(2.0F, 0.0F);
        forestHills        = new BiomeGenForest(18).setColor(2250012).setBiomeName("ForestHills").setTemperatureRainfall(0.7F, 0.8F);
        taigaHills         = new BiomeGenTaiga(19).setColor(1456435).setBiomeName("TaigaHills").setEnableSnow().setTemperatureRainfall(0.05F, 0.8F);
        extremeHillsEdge   = new ConcreteBiome(20).setColor(7501978).setBiomeName("Extreme Hills Edge").setTemperatureRainfall(0.2F, 0.3F);
        jungle             = new BiomeGenJungle(21).setColor(5470985).setBiomeName("Jungle").setTemperatureRainfall(1.2F, 0.9F);
        jungleHills        = new BiomeGenJungle(22).setColor(2900485).setBiomeName("JungleHills").setTemperatureRainfall(1.2F, 0.9F);
    }

    // Concrete subclass for biomes that don't need a specialized class
    private static class ConcreteBiome extends BiomeGenBase {
        ConcreteBiome(int id) { super(id); }
    }
}
