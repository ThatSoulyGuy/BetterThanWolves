package btw.api;

public class WorldSettings {
    public WorldSettings(long seed, EnumGameType gameType, boolean mapFeatures, boolean hardcore, WorldType worldType) {}

    public EnumGameType getGameType() { return null; }
    public boolean isHardcoreModeEnabled() { return false; }
    public boolean isMapFeaturesEnabled() { return false; }
    public WorldSettings enableCommands() { return this; }
    public boolean areCommandsAllowed() { return false; }
    public boolean isBonusChestEnabled() { return false; }
}
