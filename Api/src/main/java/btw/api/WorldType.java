package btw.api;

public class WorldType {
    public static final WorldType[] worldTypes = new WorldType[16];
    public static final WorldType DEFAULT = new WorldType(0, "default", 1);
    public static final WorldType FLAT = new WorldType(1, "flat");
    public static final WorldType LARGE_BIOMES = new WorldType(2, "largeBiomes");
    public static final WorldType DEFAULT_1_1 = new WorldType(8, "default_1_1", 0);

    private final int worldTypeId;
    private final String worldType;
    private final int generatorVersion;
    private boolean canBeCreated;
    private boolean isWorldTypeVersioned;

    private WorldType(int id, String name) {
        this(id, name, 0);
    }

    private WorldType(int id, String name, int version) {
        this.worldTypeId = id;
        this.worldType = name;
        this.generatorVersion = version;
        this.canBeCreated = true;
        worldTypes[id] = this;
    }

    public String getWorldTypeName() { return worldType; }
    public int getWorldTypeID() { return worldTypeId; }
    public int getGeneratorVersion() { return generatorVersion; }

    public static WorldType parseWorldType(String type) {
        for (WorldType wt : worldTypes) {
            if (wt != null && wt.worldType.equalsIgnoreCase(type)) return wt;
        }
        return null;
    }
}
