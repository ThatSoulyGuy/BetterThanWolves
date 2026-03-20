package btw.modern;

/**
 * Abstract representation of block materials.
 * Mirrors net.minecraft.src.Material with identical field/method names.
 */
public class Material {

    // --- Static material instances ---
    public static final Material air = new MaterialTransparent(MapColor.airColor);
    public static final Material grass = (new Material(MapColor.grassColor)).setRequiresTool();
    public static final Material ground = (new Material(MapColor.dirtColor)).setRequiresTool();
    public static final Material wood = (new Material(MapColor.woodColor)).setBurning().SetMobsCantSpawnOn().SetAxesEfficientOn();
    public static final Material rock = (new Material(MapColor.stoneColor)).setRequiresTool();
    public static final Material iron = (new Material(MapColor.ironColor)).setRequiresTool();
    public static final Material anvil = (new Material(MapColor.ironColor)).setRequiresTool().setImmovableMobility();
    public static final Material water = (new MaterialLiquid(MapColor.waterColor)).setNoPushMobility();
    public static final Material lava = (new MaterialLiquid(MapColor.tntColor)).setNoPushMobility();
    public static final Material leaves = (new Material(MapColor.foliageColor)).setBurning().setTranslucent().setNoPushMobility().SetAxesEfficientOn().SetAxesTreatAsVegetation();
    public static final Material plants = (new MaterialLogic(MapColor.foliageColor)).setNoPushMobility().SetAxesEfficientOn().SetAxesTreatAsVegetation();
    public static final Material vine = (new MaterialLogic(MapColor.foliageColor)).setBurning().setNoPushMobility().setReplaceable().SetAxesEfficientOn().SetAxesTreatAsVegetation();
    public static final Material sponge = new Material(MapColor.clothColor);
    public static final Material cloth = (new Material(MapColor.clothColor)).setBurning().SetAxesEfficientOn();
    public static final Material fire = (new MaterialTransparent(MapColor.airColor)).setNoPushMobility();
    public static final Material sand = (new Material(MapColor.sandColor)).setRequiresTool();
    public static final Material circuits = (new MaterialLogic(MapColor.airColor)).setNoPushMobility();
    public static final Material glass = (new Material(MapColor.airColor)).setTranslucent().setAlwaysHarvested();
    public static final Material redstoneLight = (new Material(MapColor.airColor)).setAlwaysHarvested();
    public static final Material tnt = (new Material(MapColor.tntColor)).setBurning().setTranslucent();
    public static final Material coral = (new Material(MapColor.foliageColor)).setNoPushMobility();
    public static final Material ice = (new Material(MapColor.iceColor)).setTranslucent().setAlwaysHarvested();
    public static final Material snow = (new MaterialLogic(MapColor.snowColor)).setReplaceable().setTranslucent().setRequiresTool().setNoPushMobility();
    public static final Material craftedSnow = (new Material(MapColor.snowColor)).setRequiresTool();
    public static final Material cactus = (new Material(MapColor.foliageColor)).setTranslucent().setNoPushMobility().SetMobsCantSpawnOn();
    public static final Material clay = new Material(MapColor.clayColor);
    public static final Material pumpkin = (new Material(MapColor.foliageColor)).setNoPushMobility().SetAxesEfficientOn();
    public static final Material dragonEgg = (new Material(MapColor.foliageColor)).setNoPushMobility();
    public static final Material portal = (new MaterialPortal(MapColor.airColor)).setImmovableMobility();
    public static final Material cake = (new Material(MapColor.airColor)).setNoPushMobility();
    public static final Material web = (new MaterialWeb(MapColor.clothColor)).setRequiresTool().setNoPushMobility();
    public static final Material piston = (new Material(MapColor.stoneColor)).setImmovableMobility();

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
