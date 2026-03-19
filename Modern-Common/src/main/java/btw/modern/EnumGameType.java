package btw.modern;

public enum EnumGameType {
    NOT_SET(-1, ""),
    SURVIVAL(0, "survival"),
    CREATIVE(1, "creative"),
    ADVENTURE(2, "adventure");

    int id;
    String name;

    EnumGameType(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getID() { return id; }
    public String getName() { return name; }
    public boolean isCreative() { return this == CREATIVE; }
    public boolean isSurvivalOrAdventure() { return this == SURVIVAL || this == ADVENTURE; }
    public boolean isAdventure() { return this == ADVENTURE; }

    public static EnumGameType getByID(int id) {
        for (EnumGameType type : values()) {
            if (type.id == id) return type;
        }
        return SURVIVAL;
    }

    public static EnumGameType getByName(String name) {
        for (EnumGameType type : values()) {
            if (type.name.equals(name)) return type;
        }
        return SURVIVAL;
    }
}
