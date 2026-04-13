package btw.forge;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * FCMOD-INTEGRATION(1.20.1): gamerule-defaults
 *
 * <p><b>Feature:</b> Post-1.5.2 gamerules that control phantom spawning,
 * pillager patrols, wandering trader spawning, warden spawning, and
 * inventory retention on death.</p>
 *
 * <p><b>Conflict with FC:</b> FC assumes no phantoms (FC's sleep mechanic
 * already penalises sleep avoidance), no pillager patrols or wandering
 * traders (they drop loot / sell items that shortcut FC's progression),
 * no warden (irrelevant to FC design), and no keepInventory (FC's death
 * penalty is load-bearing).</p>
 *
 * <p><b>Integration pattern:</b> C (rebalance via data, preserve feature).
 * Gamerules stay flippable by the user via {@code /gamerule}. We only set
 * them once at world creation.</p>
 *
 * <p><b>Event:</b> {@link LevelEvent.CreateSpawnPosition} fires exactly
 * once per world — during initial spawn-chunk generation. Loading an
 * existing world does NOT fire it, so user-modified gamerules on existing
 * saves are never overwritten.</p>
 *
 * <p><b>Alt rejected:</b> {@code ServerAboutToStartEvent} fires every
 * server start, which would override user preferences on restart. Violates
 * Protocol #2 (gate, don't delete).</p>
 */
@Mod.EventBusSubscriber(modid = BTWForgeMod.MOD_ID)
public final class WorldGamerulesHandler {

    private static final Logger LOGGER = LogManager.getLogger("BTW-Gamerules");

    private WorldGamerulesHandler() {}

    @SubscribeEvent
    public static void onCreateSpawnPosition(LevelEvent.CreateSpawnPosition event) {
        if (!(event.getLevel() instanceof ServerLevel sl)) return;

        MinecraftServer server = sl.getServer();
        GameRules rules = sl.getGameRules();

        LOGGER.info("BTW: Setting FC-appropriate gamerule defaults for new world...");

        setRule(rules, GameRules.RULE_DOINSOMNIA, false, server);
        setRule(rules, GameRules.RULE_DO_PATROL_SPAWNING, false, server);
        setRule(rules, GameRules.RULE_DO_TRADER_SPAWNING, false, server);
        setRule(rules, GameRules.RULE_DO_WARDEN_SPAWNING, false, server);
        setRule(rules, GameRules.RULE_KEEPINVENTORY, false, server);

        LOGGER.info("BTW: Gamerule defaults applied. Players can override via /gamerule.");
    }

    private static void setRule(GameRules rules, GameRules.Key<GameRules.BooleanValue> key,
                                boolean value, MinecraftServer server) {
        rules.getRule(key).set(value, server);
        LOGGER.info("  {} = {}", key.getId(), value);
    }
}
