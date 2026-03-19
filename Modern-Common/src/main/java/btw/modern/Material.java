package btw.modern;

/**
 * Abstract representation of block materials.
 * Mirrors net.minecraft.src.Material with identical field/method names.
 */
public class Material {

    // --- Static material instances ---
    public static final Material air = new Material(MapColor.airColor);
    public static final Material grass = new Material(MapColor.grassColor);
    public static final Material ground = new Material(MapColor.dirtColor);
    public static final Material wood = new Material(MapColor.woodColor);
    public static final Material rock = new Material(MapColor.stoneColor);
    public static final Material iron = new Material(MapColor.ironColor);
    public static final Material anvil = new Material(MapColor.ironColor);
    public static final Material water = new Material(MapColor.waterColor);
    public static final Material lava = new Material(MapColor.tntColor);
    public static final Material leaves = new Material(MapColor.foliageColor);
    public static final Material plants = new Material(MapColor.foliageColor);
    public static final Material vine = new Material(MapColor.foliageColor);
    public static final Material sponge = new Material(MapColor.clothColor);
    public static final Material cloth = new Material(MapColor.clothColor);
    public static final Material fire = new Material(MapColor.airColor);
    public static final Material sand = new Material(MapColor.sandColor);
    public static final Material circuits = new Material(MapColor.airColor);
    public static final Material glass = new Material(MapColor.airColor);
    public static final Material redstoneLight = new Material(MapColor.airColor);
    public static final Material tnt = new Material(MapColor.tntColor);
    public static final Material coral = new Material(MapColor.foliageColor);
    public static final Material ice = new Material(MapColor.iceColor);
    public static final Material snow = new Material(MapColor.snowColor);
    public static final Material craftedSnow = new Material(MapColor.snowColor);
    public static final Material cactus = new Material(MapColor.foliageColor);
    public static final Material clay = new Material(MapColor.clayColor);
    public static final Material pumpkin = new Material(MapColor.foliageColor);
    public static final Material dragonEgg = new Material(MapColor.foliageColor);
    public static final Material portal = new Material(MapColor.airColor);
    public static final Material cake = new Material(MapColor.airColor);
    public static final Material web = new Material(MapColor.clothColor);
    public static final Material piston = new Material(MapColor.stoneColor);

    // --- Instance fields ---
    private boolean canBurn;
    private boolean replaceable;
    private boolean isTranslucent;
    private boolean requiresNoTool = true;
    private int mobilityFlag;
    private boolean field_85159_M;
    private boolean m_bMobsCanSpawnOn = true;
    private boolean m_bNetherMobsCanSpawnOn = false;
    private boolean m_bAxesEfficientOn = false;
    private boolean m_bAxesTreatAsVegetation = false;

    public final MapColor materialMapColor;

    public Material(MapColor mapColor) {
        this.materialMapColor = mapColor;
    }

    // --- Query methods ---

    public boolean isLiquid() {
        return false;
    }

    public boolean isSolid() {
        return true;
    }

    public boolean getCanBlockGrass() {
        return true;
    }

    public boolean blocksMovement() {
        return true;
    }

    public boolean getCanBurn() {
        return this.canBurn;
    }

    public boolean isReplaceable() {
        return this.replaceable;
    }

    public boolean isOpaque() {
        return this.isTranslucent ? false : this.blocksMovement();
    }

    public boolean isToolNotRequired() {
        return this.requiresNoTool;
    }

    public int getMaterialMobility() {
        return this.mobilityFlag;
    }

    public boolean isAlwaysHarvested() {
        return this.field_85159_M;
    }

    // --- Builder methods ---

    public Material setTranslucent() {
        this.isTranslucent = true;
        return this;
    }

    public Material setRequiresTool() {
        this.requiresNoTool = false;
        return this;
    }

    public Material setBurning() {
        this.canBurn = true;
        return this;
    }

    public Material setReplaceable() {
        this.replaceable = true;
        return this;
    }

    public Material setNoPushMobility() {
        this.mobilityFlag = 1;
        return this;
    }

    public Material setImmovableMobility() {
        this.mobilityFlag = 2;
        return this;
    }

    public Material setAlwaysHarvested() {
        this.field_85159_M = true;
        return this;
    }

    // --- BTW-added methods ---

    public boolean GetMobsCanSpawnOn(int iDimension) {
        if (iDimension == -1) {
            return m_bNetherMobsCanSpawnOn;
        }
        return m_bMobsCanSpawnOn;
    }

    public Material SetMobsCantSpawnOn() {
        m_bMobsCanSpawnOn = false;
        return this;
    }

    public Material SetNetherMobsCanSpawnOn() {
        m_bNetherMobsCanSpawnOn = true;
        return this;
    }

    public boolean GetAxesEfficientOn() {
        return m_bAxesEfficientOn;
    }

    public Material SetAxesEfficientOn() {
        m_bAxesEfficientOn = true;
        return this;
    }

    public boolean GetAxesTreatAsVegetation() {
        return m_bAxesTreatAsVegetation;
    }

    public Material SetAxesTreatAsVegetation() {
        m_bAxesTreatAsVegetation = true;
        return this;
    }
}
