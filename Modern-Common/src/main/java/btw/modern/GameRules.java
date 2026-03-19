package btw.modern;

public class GameRules {
    public GameRules() {}

    public void addGameRule(String name, String defaultValue) {}
    public void setOrCreateGameRule(String name, String value) {}
    public String getGameRuleStringValue(String name) { return ""; }
    public boolean getGameRuleBooleanValue(String name) {
        // Default: most game rules are true (doTileDrops, doMobLoot, etc.)
        // Subclasses (like WorldBridge's GameRules wrapper) override with real values
        return true;
    }
    public boolean hasRule(String name) { return false; }
}
