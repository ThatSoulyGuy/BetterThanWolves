package btw.modern;

import java.util.HashMap;
import java.util.Map;

public class GameRules {

    private final Map<String, String> rules = new HashMap<String, String>();

    public GameRules() {
        // Initialize default game rules matching vanilla 1.5.2 defaults
        rules.put("doTileDrops", "true");
        rules.put("doMobLoot", "true");
        rules.put("doFireTick", "true");
        rules.put("mobGriefing", "true");
        rules.put("keepInventory", "false");
        rules.put("naturalRegeneration", "true");
        rules.put("doMobSpawning", "true");
    }

    public void addGameRule(String name, String defaultValue) {
        if (!rules.containsKey(name)) {
            rules.put(name, defaultValue);
        }
    }

    public void setOrCreateGameRule(String name, String value) {
        rules.put(name, value);
    }

    public String getGameRuleStringValue(String name) {
        String value = rules.get(name);
        return value != null ? value : "";
    }

    public boolean getGameRuleBooleanValue(String name) {
        String value = rules.get(name);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        // Default: most game rules are true (doTileDrops, doMobLoot, etc.)
        // Subclasses (like WorldBridge's GameRules wrapper) override with real values
        return true;
    }

    public boolean hasRule(String name) {
        return rules.containsKey(name);
    }
}
