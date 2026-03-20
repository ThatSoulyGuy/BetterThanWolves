package btw.forge;

import btw.modern.ILogAgent;
import btw.modern.Packet;
import btw.modern.Packet3Chat;
import btw.modern.Packet70GameEvent;
import btw.modern.ServerConfigurationManager;
import btw.modern.WorldServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ForgeMinecraftServerWrapper extends btw.modern.MinecraftServer {

    private static final Logger LOGGER = LogManager.getLogger("BTW-ServerWrapper");

    private final MinecraftServer delegate;
    private final ForgeLogAgent logAgent;
    private final ForgeConfigurationManager configManager;

    public ForgeMinecraftServerWrapper(MinecraftServer delegate) {
        this.delegate = delegate;
        this.logAgent = new ForgeLogAgent();
        this.configManager = new ForgeConfigurationManager(delegate);
    }

    public MinecraftServer getDelegate() {
        return delegate;
    }

    public boolean isSinglePlayer() {
        return delegate.isSingleplayer();
    }

    public ServerConfigurationManager getConfigurationManager() {
        return configManager;
    }

    public WorldServer worldServerForDimension(int dimension) {
        if (worldServers != null && dimension >= 0 && dimension < worldServers.length) {
            return worldServers[dimension];
        }
        if (worldServers != null && worldServers.length > 0) {
            return worldServers[0];
        }
        return null;
    }

    public ILogAgent getLogAgent() {
        return logAgent;
    }

    public boolean getPublic() {
        return !delegate.isSingleplayer();
    }

    private static class ForgeLogAgent implements ILogAgent {
        private static final Logger LOG = LogManager.getLogger("BTW");

        public void func_98233_a(String message) { LOG.info(message); }
        public void func_98236_b(String message) { LOG.warn(message); }
        public void logInfo(String message) { LOG.info(message); }
        public void logWarning(String message) { LOG.warn(message); }
    }

    /**
     * Wraps MC 1.20.1's PlayerList as an FC ServerConfigurationManager.
     * FC code uses getConfigurationManager().sendPacketToAllPlayers() to
     * broadcast chat and game-event packets. This bridge translates those
     * legacy Packet objects into modern MC network calls.
     */
    private static class ForgeConfigurationManager extends ServerConfigurationManager {
        private final MinecraftServer server;

        ForgeConfigurationManager(MinecraftServer server) {
            this.server = server;
        }

        @Override
        public void sendPacketToAllPlayers(Packet packet) {
            if (packet instanceof Packet3Chat chatPacket) {
                // Broadcast a chat/system message to all online players
                Component message = Component.literal(chatPacket.message != null ? chatPacket.message : "");
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    player.sendSystemMessage(message);
                }
            } else if (packet instanceof Packet70GameEvent gameEvent) {
                // Game events (rain start/stop, thunder start/stop, etc.)
                // In MC 1.20.1 these are handled by the server level automatically,
                // so this is largely a no-op. Log for debugging.
                LOGGER.debug("FC Packet70GameEvent type={} mode={} — weather sync handled by MC natively",
                        gameEvent.eventType, gameEvent.gameMode);
            } else {
                LOGGER.debug("Unhandled FC packet type sent to all players: {}",
                        packet.getClass().getSimpleName());
            }
        }
    }
}
