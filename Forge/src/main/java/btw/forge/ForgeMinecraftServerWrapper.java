package btw.forge;

import btw.modern.ILogAgent;
import btw.modern.ServerConfigurationManager;
import btw.modern.WorldServer;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ForgeMinecraftServerWrapper extends btw.modern.MinecraftServer {

    private static final Logger LOGGER = LogManager.getLogger("BTW-ServerWrapper");

    private final MinecraftServer delegate;
    private final ForgeLogAgent logAgent;

    public ForgeMinecraftServerWrapper(MinecraftServer delegate) {
        this.delegate = delegate;
        this.logAgent = new ForgeLogAgent();
    }

    public MinecraftServer getDelegate() {
        return delegate;
    }

    public boolean isSinglePlayer() {
        return delegate.isSingleplayer();
    }

    public ServerConfigurationManager getConfigurationManager() {
        return null;
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
}
